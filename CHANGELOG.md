# Changelog

## Unreleased

Requires BBS FS 2.7.

### Changed

- The shader options grid of the curve clip (curve fixer, now built into BBS) has rounded cells, frames and value boxes again.

## 1.3.2

Requires BBS FS 2.7.

### Changed

- Grey clips have softly rounded corners.
- Grey clips: the envelope graph is visible again and takes the clip's colour.

## 1.3.1

Requires BBS FS 2.7.

### Changed

- Row highlights fade in and out twice as fast.
- Grey clips: a selected clip keeps the standard BBS look with the white frame; only hovering lightens the grey.

## 1.3.0

This release requires BBS FS 2.7.

### Changes

- Refreshed Blur is no longer a setting. BBS 2.7 blurs the background behind panels with the same dual
  Kawase algorithm, so the addon only keeps the part BBS has no place for: the blur fades out together
  with a closing overlay panel instead of vanishing at once.
- Notification toasts are redesigned and now slide down from the top of the screen.
- The replay search field now has the same height as other controls.
- Section fold animations now play only for sections that are on screen.
- The custom UI font works with Caxton 0.9 on Minecraft 1.21.11.
- Docked panels are rounded off at the corners again: a panel that paints its own opaque background over
  the whole slot — the clips and actions timelines, the keyframe editor, the replay editor's category bar —
  no longer squares them off.

### Additions

- Tooltips wait a moment, then fade and slide in.
- Context menus pop out of the click point and fade out when closed.
- Row hover highlights fade in and out smoothly.
- List rows move: the picked row slides, folders unfold, and dragged rows make room.
- Mode strips slide their mark to the picked cell.
- The taskbar mark slides to the opened panel.
- Text buttons sink and darken while pressed.
- The timeline cursor glides to the spot you click.
- The auto-hidden preview icon bar slides down and up instead of popping.
- Switching between the camera and replay editors fades, and the preview glides to its new place instead of jumping.
- A new "Animation duration" slider in the refreshed settings (50–200%) speeds up or slows down all interface animations together.
