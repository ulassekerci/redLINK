import { Button as RNButton, ButtonProps as RNButtonProps } from 'react-native'
import colors from 'tailwindcss/colors'

// Same as react native Button but this one is rose-600 by default

export const Button: React.FC<RNButtonProps> = ({ color, ...props }) => {
  return <RNButton color={color ?? colors.rose[600]} {...props} />
}
