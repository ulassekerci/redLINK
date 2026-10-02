# 45: Update line in both apps

**What to build:** each app tells its user when a newer release exists, and does nothing more. It asks GitHub for the repo's latest release, which leaves out pre-releases, and when that version is newer than its own it shows one line that opens the Release page in the browser. Neither app downloads or installs anything.

Spec: [Android app spec](../spec-android.md) 2.13 and 3.3; [desktop app spec](../spec-desktop.md) 2.11, 3.2 and 3.4; [front page](../spec.md) section 4.

**Blocked by:** 30 (Viewer joins the hub and goes live), 34 (Main and Setup screens with grants).

**Status:** ready-for-agent

- [ ] Both apps ask the repo's latest-release endpoint named in the specs and compare its version with their own
- [ ] A `-dev` or `-rc` build compares by its major, minor and patch in a way that is written down in the code, and a pre-release is never offered
- [ ] A failed check says nothing in either app
- [ ] **Phone:** the check is made when the app is opened with no run active, and never during a run
- [ ] **Phone:** Setup shows `Update available: 1.3.0`, which opens the Release page; it is never a notification and never on the main screen
- [ ] **Desktop:** the check is made once per launch, in main, and the result is readable through the preload API
- [ ] **Desktop:** `Güncelleme var: 1.3.0` is shown on the waiting screen and in settings, and clicking it opens the Release page in the browser
- [ ] The version comparison has a unit test in each app
