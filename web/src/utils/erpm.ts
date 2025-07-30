const polePairs = 15 // 30 poles
const wheelDiameter = 0.5 // meters

export const calculateSpeed = (erpm: number) => {
  const wheelCircumference = Math.PI * wheelDiameter
  const rpm = erpm / polePairs

  const metersPerMinute = rpm * wheelCircumference
  const kilometersPerHour = (metersPerMinute * 60) / 1000
  return Math.round(kilometersPerHour)
}

export const calculateDistance = (eRevs: number) => {
  const totalMechanicalRevolutions = eRevs / polePairs
  const distance = totalMechanicalRevolutions * wheelDiameter // meters
  return Math.round(distance)
}
