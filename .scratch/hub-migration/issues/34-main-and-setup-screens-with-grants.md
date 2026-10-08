# 34: Main and Setup screens with grants

**What to build:** the phone app has its main screen and its Setup screen, and Setup walks a person through the six grants a run needs. The main screen shows the status rows in their idle state and a Start button that stays disabled, with a notice saying why, until every grant is in place. Start does nothing yet.

The chosen look is variant D of the phone screen prototype in this effort's assets.

Spec: [Android app spec](../spec-android.md) 2.4, 2.11, 3.1 (the "no run" conditions, the setup-incomplete notice, the button) and 3.3 (title, grants, version).

**Blocked by:** 33 (Android project and Kotlin protocol module).

**Status:** resolved

- [x] The main screen has the redLINK wordmark, rows for Vehicle, Hub, Viewers and Log with a coloured dot, label and state, then plain Logs and Setup rows, and one wide Start button pinned to the bottom
- [x] The rows show their "no run" text and dot from 3.1; the Vehicle row reads `No vehicle picked` in amber
- [x] Dark only, pure black background, redLINK red `#E11D48` for the wordmark and Start, stock Material 3 components, every string English
- [x] No telemetry value appears anywhere
- [x] Setup lists the six grants under `Grants (n of 6)`, each showing `Granted` or a `Grant` button that asks for it or opens the right system screen
- [x] The manifest declares the permissions 2.2 lists
- [x] All six are re-read whenever the Setup or main screen is shown
- [x] Start is disabled while any grant is missing or no board is picked, and the notice reads as 3.1 says and opens Setup when tapped
- [x] Setup shows `Version` with the app's version
- [x] The Logs row opens a screen that may be empty for now

## Comments

Implemented as three Compose screens in the package `org.metucet.redlink.ui`, with the grants in `Grants.kt`. Run on the Android 16 emulator image: each of the six grants was given through its own dialog or system screen, taken away again, and the rows and the notice followed.

- **No automated test.** The Android spec's section 4 lists the grants and the screens as not automated, so this ticket adds no suite.
- **Start is never enabled yet.** Nothing picks a board until ticket 35, so the Vehicle row always reads `No vehicle picked` and, with all six grants in place, the notice gives that as the reason.
- **One grant missing** reads `1 grant missing`. The spec had only the plural and says so now.
- **A refused grant.** Android shows its dialog twice and then answers without showing it. The `Grant` button then opens the app's page in system settings, or its notification settings for Notifications. Android does not report that it skipped the dialog; the sign used is that it would explain the permission neither before the request nor after it. A first dialog dismissed without an answer looks the same and opens settings too.
- **Location "all the time"** cannot be asked for before the app has location at all. Its `Grant` button asks for location first when it is missing and goes straight on to Android's "Allow all the time" page.
- **Re-reading.** The six are read when the activity resumes, which covers coming back from a dialog or from system settings, when a screen is opened, and when the window gets focus back, which covers the location switch in the quick settings panel.
- **Notifications** are read as "notifications are enabled for the app" on every Android version. On Android 13 and later that is the `POST_NOTIFICATIONS` permission.
- **Version** is read from the installed package, which the build stamps.
- **Icons.** Material 3 no longer ships icons, so the back arrow and the chevron are two vector drawables in the app.

Not run:

- **Android 12.** There the Notifications `Grant` button opens the app's notification settings instead of a dialog. The only emulator image is Android 16.

Known and left:

- **A dialog answered across a rotation.** If Android recreates the activity while a grant's dialog is up, the answer is not followed up: "all the time" stops after its first step and a refusal does not open settings. Pressing `Grant` again does.

Handed on, not decided here:

- **`neverForLocation`.** `BLUETOOTH_SCAN` is declared without it. Ticket 42, which scans, should decide.
- **Lint** warns that `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` is against Play Store policy. The app is sideloaded (spec section 5), so the warning is left as it is.
