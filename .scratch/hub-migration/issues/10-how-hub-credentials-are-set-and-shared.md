# How hub credentials are set and shared

Type: grilling
Status: resolved
Blocked by: 04, 13

Map: [Hub migration](../map.md)

## Question

The hub ID and password are the only access control and travel in clear text. How are they chosen, stored and handed to the pit crew?

Decide: who generates them and where they are entered; the length and alphabet of the viewer's token (carried as ASCII in the lobby request, see [What travels on the stream](05-what-travels-on-the-stream.md)); how the lobby ID and each viewer's token-bearing ID are derived from them (the hub upper-cases IDs, strips spaces and rejects `:`); how a laptop is configured (typed, file, QR); where the hub host and port setting lives so a self-run stock hub can be swapped in.

## Answer

The pit crew is handed one secret, the team code, and every hub ID and password is derived from it in plain form. Decided by grilling on 2026-10-02, on top of [How several pit laptops watch at once](04-how-several-pit-laptops-watch-at-once.md) and [Stale re-registration on the public hub](13-stale-re-registration-on-the-public-hub.md).

- **Team code:** the phone generates it on first launch and keeps it across runs. The Setup screen shows it with one action, "New code", which asks for confirmation because it locks out every laptop, and which is unavailable during a run.
- **No typing a code into the phone:** a replacement phone gets its own code and the laptops are updated. Entering an existing code was rejected: an old phone started by accident would take the lobby from the new one. Reinstalling the app also produces a new code.
- **Shape:** 8 characters shown as `XXXX-XXXX`, from the 31-character alphabet `23456789ABCDEFGHJKMNPQRSTUVWXYZ` (no `0`, `1`, `I`, `L` or `O`). Seven are random, about 35 bits; the eighth is a check character.
- **Check character:** give each character its position in the alphabet (0 to 30), multiply the first seven by the weights 1 to 7, add them up and take the result modulo 31; the check character is the alphabet entry at that position. Because 31 is prime, this catches every single wrong character and every swap of two neighbours.
- **Hub identities:** the lobby ID is `REDLINK` followed by the code without its dash (`REDLINKK7QM3XPD`). A viewer's ID is the lobby ID followed by its token. The password is the code without its dash, for every registration. Hashing was rejected: an ID is only seen on the wire, where the password travels beside it in clear text, and plain IDs can be reproduced by hand when debugging.
- **Token:** 8 random characters from the same alphabet, fresh for each lobby visit. The phone ignores a lobby request whose token is anything else.
- **Laptop setup:** the desktop app asks for the code on first launch and keeps it in its settings, where it can be changed. It accepts lower case, ignores dashes and spaces, and rejects a code whose check character is wrong. A QR code and a config file were rejected.
- **Waiting screen:** while a viewer has not joined, it shows the code in use. A valid code from the wrong phone looks to the hub like an unknown ID, which the viewer cannot tell from the phone not running.
- **Host and port:** on the laptop they sit beside the code in the settings, pre-filled with `veschub.vedder.se:65101`. The phone has them on its Setup screen already. Switching to a self-run hub means changing the two fields on the phone and on each laptop.
- **Storage:** plain app settings on both sides (private app storage on Android, the Electron settings on the laptop). The keystore and keychain were rejected: the code crosses the internet in clear text on every connection.
- **What it does not protect:** the code keeps out strangers who are guessing. Anyone who can see the traffic has it.

Handed on: the desktop's first-launch prompt, settings and waiting screen go to [Desktop app architecture](11-desktop-app-architecture.md).

Nothing here was tested against the hub; the ID forms rest on the hub rules in `protocol/tcp-hub.md` (IDs upper-cased, spaces stripped, `:` rejected), which name no length limit.
