import { LucideTriangleAlert } from 'lucide-react'
import { AnimatePresence, motion } from 'motion/react'

export const Warning = ({ code }: { code: number }) => {
  return (
    <div>
      <AnimatePresence>
        {code !== 0 && (
          <motion.div
            initial={{ y: -200 }}
            animate={{ y: 0 }}
            exit={{ y: -200 }}
            className='absolute left-1/2 transform -translate-x-1/2 flex gap-3 items-center p-3 pr-5 min-w-[360px] h-24 bg-rose-600/20 rounded-2xl'
          >
            <LucideTriangleAlert className='text-rose-800' size={48} />
            <span className='w-full text-center'>{FaultCode[code]}</span>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  )
}

enum FaultCode {
  FAULT_CODE_NONE = 0,
  FAULT_CODE_OVER_VOLTAGE,
  FAULT_CODE_UNDER_VOLTAGE,
  FAULT_CODE_DRV,
  FAULT_CODE_ABS_OVER_CURRENT,
  FAULT_CODE_OVER_TEMP_FET,
  FAULT_CODE_OVER_TEMP_MOTOR,
  FAULT_CODE_GATE_DRIVER_OVER_VOLTAGE,
  FAULT_CODE_GATE_DRIVER_UNDER_VOLTAGE,
  FAULT_CODE_MCU_UNDER_VOLTAGE,
  FAULT_CODE_BOOTING_FROM_WATCHDOG_RESET,
  FAULT_CODE_ENCODER_SPI,
  FAULT_CODE_ENCODER_SINCOS_BELOW_MIN_AMPLITUDE,
  FAULT_CODE_ENCODER_SINCOS_ABOVE_MAX_AMPLITUDE,
  FAULT_CODE_FLASH_CORRUPTION,
  FAULT_CODE_HIGH_OFFSET_CURRENT_SENSOR_1,
  FAULT_CODE_HIGH_OFFSET_CURRENT_SENSOR_2,
  FAULT_CODE_HIGH_OFFSET_CURRENT_SENSOR_3,
  FAULT_CODE_UNBALANCED_CURRENTS,
  FAULT_CODE_BRK,
  FAULT_CODE_RESOLVER_LOT,
  FAULT_CODE_RESOLVER_DOS,
  FAULT_CODE_RESOLVER_LOS,
  FAULT_CODE_FLASH_CORRUPTION_APP_CFG,
  FAULT_CODE_FLASH_CORRUPTION_MC_CFG,
  FAULT_CODE_ENCODER_NO_MAGNET,
  FAULT_CODE_ENCODER_MAGNET_TOO_STRONG,
  FAULT_CODE_PHASE_FILTER,
}
