# 47: Release keystore and first release candidate

**What to build:** the release workflow is proven on a real tag. The maintainer makes the Android release keystore once, stores it as Actions secrets and keeps a copy offline, then pushes a release-candidate tag and checks that the three packages it produces install and run. Fixes to the workflow are made with further candidates, which use up no version number.

This needs the maintainer: the keystore is a secret, and if it is lost a newer APK can no longer install over the old app.

Spec: [front page](../spec.md) sections 3 (the old app is uninstalled first), 4 and 5.

**Blocked by:** 46 (Release workflow).

**Status:** ready-for-human

- [ ] The release keystore is created and stored in the Actions secrets the workflow expects
- [ ] A copy of the keystore and its passwords is held offline by the maintainer
- [ ] A `-rc1` tag is pushed and the workflow passes, publishing a pre-release with the APK, the `dmg` and the zip
- [ ] The old React Native app is uninstalled from the phone, and the APK is sideloaded and opens
- [ ] The `dmg` opens on an Apple-silicon Mac after "Open Anyway", and the zip runs on Windows after "Run anyway"
- [ ] Each app shows the tag's version on its settings screen
- [ ] A second candidate's APK installs over the first
- [ ] Neither app's update line offers the pre-release
