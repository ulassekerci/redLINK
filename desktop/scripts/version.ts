import { execFileSync } from 'node:child_process'

// A local build's version: the latest version tag in git with -dev added, so
// its protocol version is the major of the latest release. A release
// candidate's suffix is dropped first. With no version tag it is 0.0.0-dev.
export const localVersion = (latestTag: string | null) => {
  const match = latestTag?.match(/^v(\d+\.\d+\.\d+)(-.*)?$/)
  return `${match ? match[1] : '0.0.0'}-dev`
}

export const latestVersionTag = (cwd: string) => {
  try {
    const tag = execFileSync('git', ['describe', '--tags', '--abbrev=0', '--match', 'v[0-9]*'], {
      cwd,
      encoding: 'utf8',
      stdio: ['ignore', 'pipe', 'ignore'],
    })
    return tag.trim()
  } catch {
    return null
  }
}
