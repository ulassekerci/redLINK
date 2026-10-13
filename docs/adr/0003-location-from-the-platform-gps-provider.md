# The phone takes its location from the platform's GPS provider, not Google Play services

During a run the phone app needs precise location once a second, with the screen off, for hours, and it must work on a stock Android Studio emulator image playing a route as well as on the reference phone. We decided to ask Android's own `LocationManager` for the GPS provider's fixes, at a 1 s interval, and not to use Google Play services' fused location provider.

## Considered options

- **Google Play services' fused location provider.** Rejected: it adds a dependency on Play services and its library for no gain on a car outdoors. It blends in Wi-Fi and cell positions when satellites are weak, which gives fixes the log and the viewers would take as GPS. It would also tie location to Play services being present and up to date on the phone and on the emulator image.
- **The platform's fused provider (`LocationManager.FUSED_PROVIDER`).** Rejected: what stands behind it differs between manufacturers, and it blends positions as the option above does.

## Consequences

- Every fix is from the satellites. Where the sky is hidden (a garage, a tunnel) there is no fix, the GPS row reads `No fix`, and nothing is sent or logged, as the Android spec's 2.7 says for no fix.
- The emulator's route playback and `geo fix` feed the GPS provider directly, so a run on the emulator gets fixes with no Play services on the image.
- The location services switch turns the GPS provider off. The app's request stays registered while it is off, and fixes come again once it is turned back on, with no action by the app.
