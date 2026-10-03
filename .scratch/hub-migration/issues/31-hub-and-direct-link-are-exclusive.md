# 31: Hub and direct link are exclusive

**What to build:** the app is either a viewer or on a direct link, never both, and it moves between the two without a restart. Picking a board for a direct link takes the laptop off the hub; disconnecting puts it back. Saving a different team code or host in settings makes it leave and join again with the new values.

Spec: [desktop app spec](../spec-desktop.md) 2.5 ("Hub and direct link are exclusive", "Ending it"), 2.6 ("Changing it"), 2.8, 3.1 and 3.2 (the device list rows), and the last two `hub-client` cases in section 4.

**Blocked by:** 30 (Viewer joins the hub and goes live).

**Status:** ready-for-agent

- [ ] While the device list is open the app stays on the hub: the status line shows the viewer's state and the gauges keep showing the stream
- [ ] When a board is picked the renderer tells main, which closes its socket and stops visiting the lobby
- [ ] From then on the store is written only by the direct link
- [ ] A direct link that loses its board does not fall back to the hub
- [ ] `Bağlantıyı kes` closes the Bluetooth link and tells main, which starts visiting the lobby again
- [ ] After `Bluetooth kullanılamıyor` has shown for 5 s the status line returns to the viewer's state
- [ ] `Doğrudan bağlan` is on the settings screen only, and `Bağlantıyı kes` is there too; pressing the first returns to the gauges, where the list is
- [ ] Saving a different team code makes main leave the hub and join with the new one
- [ ] Saving a different host or port makes main leave the hub and join on the new host
- [ ] The trip is kept across the switch in both directions
- [ ] The `hub-client` suite gains the two cases (leaving for a direct link and visiting again when it ends; a changed code or host) and passes
