import { describe, expect, test } from 'vitest'
import { offeredUpdate } from './update'

describe('update line', () => {
  test('a newer release is offered by its version', () => {
    expect(offeredUpdate('v1.3.0', '1.2.0')).toBe('1.3.0')
    expect(offeredUpdate('v1.2.1', '1.2.0')).toBe('1.2.1')
    expect(offeredUpdate('v2.0.0', '1.9.9')).toBe('2.0.0')
    expect(offeredUpdate('v1.10.0', '1.9.0')).toBe('1.10.0')
  })

  test('the same or an older release is not offered', () => {
    expect(offeredUpdate('v1.2.0', '1.2.0')).toBeNull()
    expect(offeredUpdate('v1.1.9', '1.2.0')).toBeNull()
    expect(offeredUpdate('v0.9.0', '1.0.0')).toBeNull()
  })

  test('a dev or candidate build compares by its major, minor and patch alone', () => {
    // A local build is the latest tag with -dev added: it is that release or later.
    expect(offeredUpdate('v1.2.0', '1.2.0-dev')).toBeNull()
    expect(offeredUpdate('v1.3.0', '1.2.0-dev')).toBe('1.3.0')
    expect(offeredUpdate('v0.1.0', '0.0.0-dev')).toBe('0.1.0')
    // A candidate and its final release are the same build.
    expect(offeredUpdate('v1.2.0', '1.2.0-rc1')).toBeNull()
    expect(offeredUpdate('v1.3.0', '1.2.0-rc1')).toBe('1.3.0')
  })

  test('a pre-release is never offered', () => {
    expect(offeredUpdate('v1.3.0-rc1', '1.2.0')).toBeNull()
    expect(offeredUpdate('v2.0.0-beta', '1.2.0-dev')).toBeNull()
  })

  test('a tag that is not a version is not offered', () => {
    expect(offeredUpdate('nightly', '1.2.0')).toBeNull()
    expect(offeredUpdate('1.3.0', '1.2.0')).toBeNull()
    expect(offeredUpdate('', '1.2.0')).toBeNull()
  })
})
