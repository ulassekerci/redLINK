# 49: Acceptance session

**What to build:** nothing new. One real track session with the car decides whether the migration is done. A test on a desk does not count, however complete: the old app's failures only showed up in the car. The session is booked only after the desk checklist has passed, and passing it triggers no deletion.

Spec: [front page](../spec.md) section 7.

**Blocked by:** 48 (Desk checklist).

**Status:** ready-for-human

- [ ] The phone runs a release build, with the release app's team code typed into each laptop
- [ ] The phone completes a run with the screen off, and its log of record has no gaps
- [ ] At least two laptops, one macOS and one Windows, watch through the hub for the whole run
- [ ] Every dashboard feature works on a viewer: gauges, ADC, map, trip meter, CSV export
- [ ] The direct link works on one laptop
