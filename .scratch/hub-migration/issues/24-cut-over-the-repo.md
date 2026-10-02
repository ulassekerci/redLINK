# 24: Cut over the repo

**What to build:** the repo takes the layout of the front page's "Repo layout" by following its "Cutover" steps 1 to 4. The old stack is gone behind a tag, the dashboard lives in `desktop/` on pnpm and still builds and runs in a browser exactly as before, and the wire format has its permanent home in `protocol/`. Step 5 (the Android project) and `vectors.json` are separate tickets.

Spec: [front page](../spec.md) sections 2 and 3; [hub usage spec](../spec-hub.md) for the sections marked **(moves)**.

**Blocked by:** None (can start immediately).

**Status:** ready-for-agent

- [x] The last commit before any implementation change is tagged `legacy-stack`
- [x] `mobile/` and `server/` are deleted
- [x] `web/` is renamed to `desktop/` with `git mv`, so file history carries over
- [x] `desktop/` is on pnpm: `pnpm-lock.yaml` replaces `package-lock.json` and `packageManager` pins the pnpm version
- [x] `pnpm install`, `pnpm build` and `pnpm dev` work in `desktop/`, and the dashboard looks as it did
- [x] `protocol/` exists with `tcp-hub.md` moved in from the root
- [x] `protocol/README.md` holds the hub spec's sections marked **(moves)**: framing, board commands, our messages and the vector list; the hub spec keeps a link in the place of each
- [x] Every reference to `tcp-hub.md` at its old path is updated
- [x] The root `README.md` describes the new stack, with no mention of the relay or the React Native app as current
- [x] There is no root `package.json` and no workspace
