# Repo layout and cutover

Type: grilling
Status: open

Map: [Hub migration](../map.md)

## Question

The new apps live in this repo and replace `mobile/`, `server/` and `web/`, with the old stack kept until parity. What is the layout and when does the old stack go?

Decide: directory names for the Android and Electron apps; whether the Electron app is built in place from `web/` or copied; where the protocol description shared by Kotlin and TypeScript lives; what "parity" means concretely as the trigger for deleting the old stack.
