export const getAspilsanPercentage = (cellVoltage: number) => {
  // voltage-discharge chart from https://www.aspilsan.com/wp-content/uploads/2025/05/A28_Public_Datasheet_.pdf
  const chart = [
    [4.25, 0],
    [4.18, 10],
    [4, 47],
    [3.98, 200],
    [3.94, 339],
    [3.88, 501],
    [3.82, 679],
    [3.72, 1001],
    [3.65, 1191],
    [3.55, 1502],
    [3.52, 1640],
    [3.49, 1798],
    [3.45, 2003],
    [3.41, 2148],
    [3.36, 2311],
    [3.32, 2418],
    [3.28, 2507],
    [3.23, 2576],
    [3.18, 2617],
    [3.08, 2663],
    [3, 2688],
    [2.88, 2715],
    [2.8, 2721],
    [2.75, 2727],
    [2.5, 2761],
  ]

  if (cellVoltage > 4.2) return 100
  if (cellVoltage < 2.8) return 0

  for (let i = 0; i < chart.length - 1; i++) {
    const voltage1 = chart[i][0]
    const percentage1 = 100 - 100 * (chart[i][1] / 2721)
    const voltage2 = chart[i + 1][0]
    const percentage2 = 100 - 100 * (chart[i + 1][1] / 2721)

    if ((cellVoltage <= voltage1 && cellVoltage >= voltage2) || (cellVoltage >= voltage1 && cellVoltage <= voltage2)) {
      // Linear interpolation
      const slope = (percentage2 - percentage1) / (voltage2 - voltage1)
      const percentage = percentage1 + slope * (cellVoltage - voltage1)
      return Math.round(percentage)
    }
  }

  return 0
}
