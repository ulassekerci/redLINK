# 29: Settings screen and team code

**What to build:** the pit crew can give a laptop its team code and, if needed, another hub. On first launch the app opens on the settings screen. A typed code is accepted in any case with or without its dash, and rejected with a message when its check character is wrong. The code, host and port are saved together in one file that main owns, and survive a restart.

Nothing joins the hub yet; ticket 30 uses what is stored here.

Spec: [desktop app spec](../spec-desktop.md) 2.6, 2.7, 2.8 and 3.4; [hub usage spec](../spec-hub.md) 2.7 for the code's rules.

**Blocked by:** 25 (Protocol vectors and the TypeScript protocol module), 26 (Electron shell around the dashboard).

**Status:** ready-for-agent

- [ ] `settings.json` in Electron's user-data folder holds the team code without its dash, the hub host and the hub port, as plain text, read and written by main only
- [ ] A missing or unreadable file means no team code and the public hub's host and port
- [ ] The preload API lets the renderer read and write the settings
- [ ] With no code stored the app opens on the settings screen
- [ ] The settings screen has the Turkish strings of 3.4: `Ayarlar`, `Geri`, `Takım kodu`, `Hub adresi`, `Port`, `Kaydet`, and `Sürüm` with the app's version
- [ ] `Kaydet` saves the three values together
- [ ] Lower case is accepted, dashes and spaces are ignored; a code with a wrong check character shows `Takım kodu hatalı. Telefondaki kodu kontrol edin.` and is not stored
- [ ] Host and port are pre-filled with the public hub's
- [ ] The settings entry in the bottom section opens the screen at any time
- [ ] The gauge style toggle stays in the renderer's local storage
- [ ] The app never generates a team code
