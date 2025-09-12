import { StyleSheet, View } from 'react-native'
import { Text } from './Text'
import colors from 'tailwindcss/colors'

export const DataRow = ({
  name,
  data,
  unit,
  precision,
  last,
}: {
  name: string
  data: number
  unit?: string
  precision?: number
  last?: boolean
}) => {
  return (
    <View style={{ ...styles.row, borderBottomWidth: last ? 0 : 0.5 }}>
      <Text>{name}</Text>
      <Text>
        {data.toFixed(precision ?? 0)} {unit}
      </Text>
    </View>
  )
}

const styles = StyleSheet.create({
  row: {
    padding: 12,
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    borderColor: colors.neutral[900],
  },
})
