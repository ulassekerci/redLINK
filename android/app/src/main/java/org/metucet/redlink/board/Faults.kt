package org.metucet.redlink.board

// The firmware 6.06 fault codes (mc_fault_code in the firmware's datatypes.h),
// named without their FAULT_CODE_ prefix. The desktop app holds the same list.
private val faultNames = listOf(
    "NONE",
    "OVER_VOLTAGE",
    "UNDER_VOLTAGE",
    "DRV",
    "ABS_OVER_CURRENT",
    "OVER_TEMP_FET",
    "OVER_TEMP_MOTOR",
    "GATE_DRIVER_OVER_VOLTAGE",
    "GATE_DRIVER_UNDER_VOLTAGE",
    "MCU_UNDER_VOLTAGE",
    "BOOTING_FROM_WATCHDOG_RESET",
    "ENCODER_SPI",
    "ENCODER_SINCOS_BELOW_MIN_AMPLITUDE",
    "ENCODER_SINCOS_ABOVE_MAX_AMPLITUDE",
    "FLASH_CORRUPTION",
    "HIGH_OFFSET_CURRENT_SENSOR_1",
    "HIGH_OFFSET_CURRENT_SENSOR_2",
    "HIGH_OFFSET_CURRENT_SENSOR_3",
    "UNBALANCED_CURRENTS",
    "BRK",
    "RESOLVER_LOT",
    "RESOLVER_DOS",
    "RESOLVER_LOS",
    "FLASH_CORRUPTION_APP_CFG",
    "FLASH_CORRUPTION_MC_CFG",
    "ENCODER_NO_MAGNET",
    "ENCODER_MAGNET_TOO_STRONG",
    "PHASE_FILTER",
    "ENCODER_FAULT",
    "LV_OUTPUT_FAULT",
)

// A code with no name is shown as its number.
fun faultName(code: Int) = faultNames.getOrNull(code) ?: code.toString()
