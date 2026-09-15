# resfreshed-addon — контекст проекта

> Этот файл автоматически загружается Codex в начале каждой сессии в этой папке.
> Он даёт ИИ «конект» с исходным проектом без отдельных указаний. Держи его в актуальном состоянии.

## Что это за проект

Аддон к моду **BBS** (`bbs-fs`), который **выносит все наши UI-правки темы интерфейса в отдельный мод через миксины**, чтобы базовый `bbs-fs` оставался чистым (= легко тянуть апстрим-обновления BBS).

Сейчас все правки лежат **напрямую в коде** форка bbs-fs на ветке `master-refreshed`. Цель — переложить их в этот аддон **поэтапно**, по фичам.

## Источники (абсолютные пути)

| Роль | Путь | Примечание |
|---|---|---|
| **Эталон-источник правок** | `C:\Users\Qualet\Documents\Project\Minecraft\BBS\bbs-fs` | git-репо. Ветка `master` = чистый upstream (`bbs 2.2-dev1`); ветка `master-refreshed` = все наши UI-правки (`bbs 2.2-dev2`) |
| **Шаблон аддона** | `C:\Users\Qualet\Documents\Project\Minecraft\BBS\IRLEngine` | рабочий Fabric-аддон к BBS. Берём структуру (build.gradle, fabric.mod.json, mixin-конфиги, accesswidener, entry points, ISourcePack) как основу — не изобретаем с нуля |
| **Этот проект** | `C:\Users\Qualet\Documents\Project\Minecraft\BBS\resfreshed-addon` | новый аддон |

### Как смотреть, что портировать
Весь набор правок = дифф между чистым и нашим:
```
cd C:\Users\Qualet\Documents\Project\Minecraft\BBS\bbs-fs
git diff master master-refreshed                 # весь дифф (79 файлов, +1701/-220)
git diff master master-refreshed -- <файл>       # по конкретному файлу
git log master..master-refreshed --oneline       # 24 коммита (19 фич темы + 5 не-тема)
git show <hash>                                   # отдельная фича-коммит
```
> Для удобства можно добавить `bbs-fs` как additional working directory в новой сессии, либо просто читать по абсолютным путям.

## Источник истины для BBS API (актуально с 2026-08-08)

Ветка MC **1.20.1** собирается против форка **`C:\Users\Qualet\Documents\Project\Minecraft\BBS\npc-mod`, ветка `1.20.1`**
(remote `fs` = upstream `Wemppy4/bbs-fs`, remote `origin` = `quaIett/bbs-fbs-npc`). Именно этот билд
стоит в проде (Prism-инстанс `Reff`). Dep в аддоне: `libs/bbs-2.4-npc-1.20.1.jar` (+ `-sources.jar`),
собирается там же через `./gradlew build` → `npc-mod/build/libs/`.

> Осторожно: npc-mod и старый bbs-fs дают jar с ОДИНАКОВЫМ именем `bbs-2.4-1.20.1.jar` и одинаковой
> версией `2.4-1.20.1`, но это разные билды. Поэтому наш dep переименован в `bbs-2.4-npc-*`.
> Профили `-Pmc=1.20.4` и `-Pmc=universal` всё ещё смотрят на старый bbs-fs-jar и после
> перехода на `surfaceBox` **не собираются** — им нужен свой билд из npc-mod (веток под них пока нет).

Диагностика после апстрим-бампа: `./gradlew runClient -Pprobe` — `MixinProbe` форс-грузит все
целевые классы миксинов и печатает все провалившиеся инъекции за один запуск (отчёт `run/refreshedui-probe.txt`).

## Окружение сборки (из шаблона IRLEngine — совпадает с bbs-fs)

- Fabric Loom `1.15-SNAPSHOT`, MC `1.20.4`, yarn `1.20.4+build.1`, loader `0.16.14`, fabric-api `0.91.1+1.20.4`, Java 17.
- **Сборка требует JDK 21** (Loom 1.15 не стартует на 17): `JAVA_HOME=".../jdk-21..." ./gradlew build`.
- **Зависимость на BBS:** jar, собранный из `bbs-fs` ветки **`master`** (чистый). Уже лежит в `libs/`: `bbs-2.2-dev1-1.20.4.jar` (+ `-sources.jar`). Подключается как `modImplementation files("libs/bbs-2.2-dev1-1.20.4.jar")`.
  - Пересборка: `cd bbs-fs && git checkout master && ./gradlew build -x test` → `build/libs/bbs-2.2-dev1-1.20.4.jar`, затем вернуть `git checkout master-refreshed`.
  - **Важно:** dep — это ЧИСТЫЙ master. Поэтому новых классов/методов из наших правок (UIConstants, UICornerRadii, Batcher2D.roundedBox и т.п.) в нём НЕТ — их добавляет сам аддон (см. архитектуру ниже).

## Архитектура переноса (решение принято — см. анализ)

Правки делятся на 4 типа по способу выноса:

1. **Новый самодостаточный код** → **обычные классы аддона** (не миксины).
   `UIConstants`, `UICornerRadii`, добавки логики `Area`/`Scroll`.
2. **Новые примитивы в `Batcher2D`** (+763 стр: `roundedBox`, `roundedFrame`, `roundedBoxSides`, `roundedBoxHorizontalAlpha`, `roundedIconArea`, `filledCircle`) → **accessor-миксин + `@Unique`**.
   Узкое место: они лезут в приватный `this.context` (DrawContext) и в НОВУЮ machinery масок (`getRoundedRectMask()`/`getFilledCircleMask()` + кэш-поля Texture). Внешним хелпером не вынести. Решение: миксин в `Batcher2D` добавляет всё как `@Unique` + интерфейс-аксессор `IRoundedBatcher` с публичными методами; вызовы в коде → каст `((IRoundedBatcher) context.batcher).roundedBox(...)`.
3. **Точечные правки** (цвет/тень/одна ветка) → **`@ModifyExpressionValue` / `@Redirect` / `@Inject`**. Устойчивы к апстрим-апдейтам.
   Напр. `UIButton`: `textColor WHITE→A100`, `textShadow→false`, `bevelBox→roundedBox`; off-fill тогла.
4. **Целиком переписанные render-методы** → **`@Overwrite`**. ЭТО зона хрупкости (привязка к версии BBS). Держать список коротким и задокументированным.
   Главный пример: `UIToggle.renderSkin` (полностью переписан + новые поля анимации).

**Ресурсы** (`icons.png` 16KB→69KB, `bg.png`, строки `*_*.json`) → через `ISourcePack` (см. `IrlightsAssetsSourcePack` в шаблоне). Нюанс: наши ассеты переопределяют СВОЙ namespace BBS (`assets/bbs/assets/...`), а не чужой — надо проверить порядок резолва в `AssetProvider` (флаг-исследование в плане). Код ссылается на индексы новых иконок, которых нет в чистом атласе → атлас обязателен в аддоне.

## Рабочие договорённости

- **Поэтапно.** Не валить все фичи сразу. Один под-этап = одна фича = отдельная проверка в игре + коммит. Порядок в `PLAN.md`.
- **Foundation первым.** Этап 3.1 (Batcher2D-примитивы + UICornerRadii + UIConstants + настройка интенсивности) — база, на неё опираются все остальные фичи. Без неё ничего не скруглится.
- **Лог правок** — терсово на английском (как в основном проекте). **Коммит — только по явному «ОК» от пользователя.**
- **Не угадывать API BBS.** Сверяться с исходником в `bbs-fs` и с примерами в `IRLEngine` (события `@Subscribe`, `BBSAddonMod`, `ISourcePack`, реестр форм/клипов).

## Полный план — в `PLAN.md`

Этапы 0→3 (git → база аддона → проверка запуска без инъекций → поэтапный перенос фич). Это пошаговый промт для исполнения в новой сессии.
