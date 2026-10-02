# Where the specs live and how they are split

Type: grilling
Status: open

Map: [Hub migration](../map.md)

## Question

Every decision ticket is resolved; what remains is writing the three specs the destination names (Android app, hub usage, Electron app). Where do they live, and what goes in which?

Decide: the file paths (the tracker convention is one `.scratch/<feature-slug>/spec.md` per feature, and there are three parts); how the hub usage spec relates to `protocol/README.md` and `protocol/vectors.json` from [Repo layout and cutover](12-repo-layout-and-cutover.md), so the wire format is described in one place only; where cross-cutting requirements go (the named test suites and desk checklist from [Testing without the car](16-testing-without-the-car.md), the release workflow from [Distribution](15-distribution.md), the cutover steps); whether a spec restates decisions or links to the tickets; in what order the three are written and whether each is its own session.
