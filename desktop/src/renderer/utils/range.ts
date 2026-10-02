export const range = (startInput: number, endInput: number) => {
  const start = Math.round(startInput)
  const end = Math.round(endInput)
  return Array.from({ length: end - start + 1 }, (_, i) => start + i)
}
