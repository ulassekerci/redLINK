# 34: Main and Setup screens with grants

**What to build:** the phone app has its main screen and its Setup screen, and Setup walks a person through the six grants a run needs. The main screen shows the status rows in their idle state and a Start button that stays disabled, with a notice saying why, until every grant is in place. Start does nothing yet.

The chosen look is variant D of the phone screen prototype in this effort's assets.

Spec: [Android app spec](../spec-android.md) 2.4, 2.11, 3.1 (the "no run" conditions, the setup-incomplete notice, the button) and 3.3 (title, grants, version).

**Blocked by:** 33 (Android project and Kotlin protocol module).

**Status:** ready-for-agent

- [ ] The main screen has the redLINK wordmark, rows for Vehicle, Hub, Viewers and Log with a coloured dot, label and state, then plain Logs and Setup rows, and one wide Start button pinned to the bottom
- [ ] The rows show their "no run" text and dot from 3.1; the Vehicle row reads `No vehicle picked` in amber
- [ ] Dark only, pure black background, redLINK red `#E11D48` for the wordmark and Start, stock Material 3 components, every string English
- [ ] No telemetry value appears anywhere
- [ ] Setup lists the six grants under `Grants (n of 6)`, each showing `Granted` or a `Grant` button that asks for it or opens the right system screen
- [ ] The manifest declares the permissions 2.2 lists
- [ ] All six are re-read whenever the Setup or main screen is shown
- [ ] Start is disabled while any grant is missing or no board is picked, and the notice reads as 3.1 says and opens Setup when tapped
- [ ] Setup shows `Version` with the app's version
- [ ] The Logs row opens a screen that may be empty for now
