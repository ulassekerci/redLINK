# Testing without the car

Type: grilling
Status: open

Map: [Hub migration](../map.md)

## Question

The acceptance session is a real track session with the car ([Repo layout and cutover](12-repo-layout-and-cutover.md)), but both apps must be exercisable on a desk before that. What stands in for the board and for the hub?

Decide: what plays the board for the Android app and for the desktop's direct link (a spare VESC on a bench supply, a BLE peripheral that replays recorded replies, or a fake behind the app's Bluetooth seam); whether desk tests use the public hub or a locally run stock hub; how a viewer is exercised with no phone at all; which of these stand-ins the specs require to be built, and where they live in the repo beside `protocol/vectors.json`.
