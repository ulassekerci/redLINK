import { mkdir, readFile, rename, writeFile } from 'node:fs/promises'
import { dirname } from 'node:path'
import type { Settings, TypedSettings } from '../preload/api'
import { normaliseTeamCode } from '../protocol'

// The settings file (desktop spec 2.7): one plain-text JSON file that main
// alone reads and writes, holding the team code without its dash, the hub host
// and the hub port.

export const PUBLIC_HUB = { host: 'veschub.vedder.se', port: 65101 }

const isPort = (value: unknown): value is number =>
  typeof value === 'number' && Number.isInteger(value) && value >= 1 && value <= 65535

// A missing or unreadable file is no settings: no team code, and the public
// hub's host and port. So is any one value in it that is not valid.
export async function readSettings(file: string): Promise<Settings> {
  let stored: Partial<Record<keyof Settings, unknown>> = {}
  try {
    stored = JSON.parse(await readFile(file, 'utf8')) ?? {}
  } catch {
    // Missing or unreadable.
  }
  const { teamCode, hubHost, hubPort } = stored
  return {
    teamCode: typeof teamCode === 'string' ? normaliseTeamCode(teamCode) : null,
    hubHost: typeof hubHost === 'string' && hubHost.trim() ? hubHost.trim() : PUBLIC_HUB.host,
    hubPort: isPort(hubPort) ? hubPort : PUBLIC_HUB.port,
  }
}

// Stores the three values together and returns them as stored, or stores
// nothing and returns null when the typed team code is not a valid one. An
// empty code field is no team code. An empty host, or a port that is not one,
// goes back to the public hub's.
export async function saveSettings(file: string, typed: TypedSettings): Promise<Settings | null> {
  const empty = !typed.teamCode.trim()
  const teamCode = empty ? null : normaliseTeamCode(typed.teamCode)
  if (!empty && !teamCode) return null

  const port = /^\d+$/.test(typed.hubPort.trim()) ? Number(typed.hubPort) : null
  const settings: Settings = {
    teamCode,
    hubHost: typed.hubHost.trim() || PUBLIC_HUB.host,
    hubPort: isPort(port) ? port : PUBLIC_HUB.port,
  }
  // Written beside the file and moved over it, so a crash part-way through a
  // write leaves the old settings and not half a file.
  const partial = `${file}.${process.pid}.tmp`
  await mkdir(dirname(file), { recursive: true })
  await writeFile(partial, JSON.stringify(settings, null, 2) + '\n')
  await rename(partial, file)
  return settings
}
