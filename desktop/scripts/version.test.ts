import { describe, expect, test } from 'vitest'
import { localVersion } from './version'

describe('localVersion', () => {
  test('is the latest version tag with -dev added', () => {
    expect(localVersion('v1.2.0')).toBe('1.2.0-dev')
  })

  test('drops a release candidate suffix before adding -dev', () => {
    expect(localVersion('v1.3.0-rc2')).toBe('1.3.0-dev')
  })

  test('is 0.0.0-dev with no tag', () => {
    expect(localVersion(null)).toBe('0.0.0-dev')
  })

  test('is 0.0.0-dev when the tag is not a version', () => {
    expect(localVersion('legacy-stack')).toBe('0.0.0-dev')
  })
})
