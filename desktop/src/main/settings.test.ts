import { mkdtemp, readdir, readFile, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { afterEach, beforeEach, describe, expect, test } from 'vitest'
import { PUBLIC_HUB } from '../protocol'
import { readSettings, saveSettings } from './settings'

const noSettings = { teamCode: null, hubHost: PUBLIC_HUB.host, hubPort: PUBLIC_HUB.port }

let dir: string
let file: string

beforeEach(async () => {
  dir = await mkdtemp(join(tmpdir(), 'redlink-settings-'))
  file = join(dir, 'settings.json')
})

afterEach(() => rm(dir, { recursive: true }))

describe('settings', () => {
  test('a missing file is no team code and the public hub', async () => {
    expect(await readSettings(file)).toEqual(noSettings)
  })

  test('an unreadable file is no team code and the public hub', async () => {
    await writeFile(file, '{"teamCode": "K7QM')
    expect(await readSettings(file)).toEqual(noSettings)

    await writeFile(file, 'null')
    expect(await readSettings(file)).toEqual(noSettings)
  })

  test('a typed code is stored without its dash, with the host and the port', async () => {
    const typed = { teamCode: ' k7qm-3xpc', hubHost: ' hub.example.org ', hubPort: '65102' }
    const saved = { teamCode: 'K7QM3XPC', hubHost: 'hub.example.org', hubPort: 65102 }

    expect(await saveSettings(file, typed)).toEqual(saved)
    expect(JSON.parse(await readFile(file, 'utf8'))).toEqual(saved)
    expect(await readSettings(file)).toEqual(saved)
    expect(await readdir(dir)).toEqual(['settings.json'])
  })

  test('the folder is made when it does not exist yet', async () => {
    file = join(dir, 'redLINK', 'settings.json')
    await saveSettings(file, { teamCode: 'K7QM-3XPC', hubHost: PUBLIC_HUB.host, hubPort: '65101' })
    expect((await readSettings(file)).teamCode).toBe('K7QM3XPC')
  })

  test('a code with a wrong check character is rejected and nothing is stored', async () => {
    const stored = await saveSettings(file, { teamCode: 'K7QM-3XPC', hubHost: 'hub.example.org', hubPort: '65102' })

    expect(await saveSettings(file, { teamCode: 'K7QM-3XPD', hubHost: 'other.example.org', hubPort: '1' })).toBeNull()
    expect(await readSettings(file)).toEqual(stored)
  })

  test('an empty code field stores no team code', async () => {
    await saveSettings(file, { teamCode: 'K7QM-3XPC', hubHost: 'hub.example.org', hubPort: '65102' })

    expect(await saveSettings(file, { teamCode: '  ', hubHost: 'hub.example.org', hubPort: '65102' })).toEqual({
      teamCode: null,
      hubHost: 'hub.example.org',
      hubPort: 65102,
    })
  })

  test('an empty host or a port that is not one goes back to the public hub', async () => {
    for (const hubPort of ['', '0', '65536', '80a', '8.5']) {
      expect(await saveSettings(file, { teamCode: '', hubHost: '', hubPort })).toEqual(noSettings)
    }
  })

  test('a file edited by hand keeps only what is valid', async () => {
    await writeFile(file, JSON.stringify({ teamCode: 'K7QM3XPD', hubHost: 'hub.example.org', hubPort: '65102' }))
    expect(await readSettings(file)).toEqual({ teamCode: null, hubHost: 'hub.example.org', hubPort: PUBLIC_HUB.port })
  })
})
