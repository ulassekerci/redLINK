// What the preload script exposes to the renderer as `window.redlink`, and the
// only way the renderer reaches main. Later tickets extend it to the full list
// in the desktop spec, 2.3.
export interface RedlinkApi {
  version: () => Promise<string>
}
