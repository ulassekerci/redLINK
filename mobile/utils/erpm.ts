const polePairs = 11 // 22 poles
const gearRatio = 5.88
const wheelDiameter = 0.5 // meters
const wheelCircumference = Math.PI * wheelDiameter // meters

export const calculateSpeed = (erpm: number) => {
  const motorRPM = erpm / polePairs
  const wheelRPM = motorRPM / gearRatio
  const metersPerMinute = wheelRPM * wheelCircumference
  const kilometersPerHour = (metersPerMinute * 60) / 1000
  return kilometersPerHour
}

export const calculateDistance = (tachometer: number) => {
  // vesc tachometer counts hall transitions, so divide by 6 to get electrical revolutions.
  const electricalRevolutions = tachometer / 6
  const motorRevolutions = electricalRevolutions / polePairs
  const wheelRevolutions = motorRevolutions / gearRatio
  const distance = wheelRevolutions * wheelCircumference // meters
  return distance
}
