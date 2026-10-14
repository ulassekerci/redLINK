// The update line (desktop spec 2.11): one check of the repo's latest release
// per launch, and nothing downloaded or installed.

const REPO = 'ulassekerci/redLINK'

// A newer release, as the update line shows it and the page it opens.
export interface Update {
  version: string
  releasePage: string
}

const releaseTag = /^v(\d+)\.(\d+)\.(\d+)$/
const ownVersion = /^(\d+)\.(\d+)\.(\d+)(-.*)?$/

// The version to offer for the latest release's tag, or null for none.
//
// Only major, minor and patch are compared; a suffix on the app's own version
// is ignored. A local build is the latest tag with -dev added, so it is that
// release or later, and a candidate is the same build as its final release:
// 1.2.0-dev and 1.2.0-rc1 are offered 1.3.0 but not 1.2.0. A tag with a suffix
// is a pre-release, which the endpoint leaves out, and is never offered.
export const offeredUpdate = (latestTag: string, version: string): string | null => {
  const latest = latestTag.match(releaseTag)
  const own = version.match(ownVersion)
  if (!latest || !own) return null
  for (let part = 1; part <= 3; part++) {
    const difference = Number(latest[part]) - Number(own[part])
    if (difference !== 0) return difference > 0 ? latestTag.slice(1) : null
  }
  return null
}

// Asks GitHub for the latest release, and resolves with it when it is newer
// than this app. A failed check resolves with null and says nothing.
export const checkForUpdate = async (version: string): Promise<Update | null> => {
  try {
    const response = await fetch(`https://api.github.com/repos/${REPO}/releases/latest`, {
      headers: { Accept: 'application/vnd.github+json' },
      signal: AbortSignal.timeout(15_000),
    })
    if (!response.ok) return null
    const release: unknown = await response.json()
    const tag = (release as { tag_name?: unknown } | null)?.tag_name
    if (typeof tag !== 'string') return null
    const offered = offeredUpdate(tag, version)
    if (!offered) return null
    return { version: offered, releasePage: `https://github.com/${REPO}/releases/tag/${tag}` }
  } catch {
    return null
  }
}
