const polePairs = 15 // 30 poles
const wheelDiameter = 0.5 // meters
const wheelCircumference = Math.PI * wheelDiameter // meters

export const calculateSpeed = (erpm: number) => {
  const rpm = erpm / polePairs
  const metersPerMinute = rpm * wheelCircumference
  const kilometersPerHour = (metersPerMinute * 60) / 1000
  return kilometersPerHour
}

export const calculateDistance = (tachometer: number) => {
  // vesc tachometer counts every hall sensor update,
  // as 3 sensors update twice for each revolution
  // we need to divide it by 6 to get electrical revolutions
  const electricalRevolutions = tachometer / 6
  const totalMechanicalRevolutions = electricalRevolutions / polePairs
  const distance = totalMechanicalRevolutions * wheelCircumference // meters
  return distance
}
