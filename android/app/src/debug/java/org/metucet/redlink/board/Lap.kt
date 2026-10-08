package org.metucet.redlink.board

import org.metucet.redlink.protocol.DecodedAdc
import org.metucet.redlink.protocol.ValuesSetup

// The fixed lap the simulated board plays, over and over: accelerate, cruise,
// coast to a stop, stand. Every value is a function of the time since the run
// started and nothing else, so the same run always gives the same replies.
//
// The values agree with each other. Distance is speed added up, charge is
// battery current added up, and energy is battery voltage x battery current
// added up. Each is worked out exactly from the phase's formulas, not by
// stepping, so a reply does not depend on the replies before it.

// A stretch of the lap over which the speed changes evenly and the motor
// current is steady.
private class Phase(val seconds: Double, val speedFrom: Double, val speedTo: Double, val motorCurrentA: Double) {
    private val acceleration = (speedTo - speedFrom) / seconds

    // The battery current also changes evenly: it is the motor current scaled
    // by the duty cycle, which follows the speed.
    private val batteryCurrentFrom = motorCurrentA * speedFrom / NO_LOAD_SPEED_M_S
    private val batteryCurrentRise = motorCurrentA * acceleration / NO_LOAD_SPEED_M_S

    fun speed(t: Double) = speedFrom + acceleration * t

    fun batteryCurrent(t: Double) = batteryCurrentFrom + batteryCurrentRise * t

    // Metres covered, t seconds into the phase.
    fun distance(t: Double) = speedFrom * t + acceleration * t * t / 2

    // Ampere-seconds drawn.
    fun charge(t: Double) = batteryCurrentFrom * t + batteryCurrentRise * t * t / 2

    // Watt-seconds drawn: the voltage is REST_VOLTAGE_V less the sag, so the
    // power is REST_VOLTAGE_V x current - RESISTANCE_OHM x current squared.
    fun energy(t: Double): Double {
        val currentSquared = batteryCurrentFrom * batteryCurrentFrom * t +
            batteryCurrentFrom * batteryCurrentRise * t * t +
            batteryCurrentRise * batteryCurrentRise * t * t * t / 3
        return REST_VOLTAGE_V * charge(t) - RESISTANCE_OHM * currentSquared
    }
}

private const val CRUISE_M_S = 8.0
private const val FULL_THROTTLE_A = 15.0

// The speed at which the duty cycle would be 1.
private const val NO_LOAD_SPEED_M_S = 10.0
private const val ERPM_PER_M_S = 1000.0

private const val REST_VOLTAGE_V = 48.0
private const val RESISTANCE_OHM = 0.1
private const val CAPACITY_AH = 20.0

// What the odometer read when the run started.
private const val ODOMETER_START_M = 12_000L

private val phases = listOf(
    Phase(seconds = 20.0, speedFrom = 0.0, speedTo = CRUISE_M_S, motorCurrentA = FULL_THROTTLE_A),
    Phase(seconds = 60.0, speedFrom = CRUISE_M_S, speedTo = CRUISE_M_S, motorCurrentA = 4.0),
    Phase(seconds = 30.0, speedFrom = CRUISE_M_S, speedTo = 0.0, motorCurrentA = 0.0),
    Phase(seconds = 10.0, speedFrom = 0.0, speedTo = 0.0, motorCurrentA = 0.0),
)

private val lapSeconds = phases.sumOf { it.seconds }

// OVER_TEMP_FET, raised over the last 5 seconds of the acceleration.
private const val FAULT_CODE = 5
private val faultSeconds = 15.0..<20.0

// Where the lap is at a time: the phase, the seconds into it, and what the
// laps and phases before it have added up to.
private class Moment(elapsedMs: Long) {
    val secondsIntoLap: Double
    val phase: Phase
    val t: Double
    val distanceM: Double
    val chargeAs: Double
    val energyWs: Double

    init {
        val seconds = elapsedMs / 1000.0
        val laps = Math.floor(seconds / lapSeconds)
        secondsIntoLap = seconds - laps * lapSeconds

        var index = 0
        var phaseStart = 0.0
        while (index < phases.lastIndex && secondsIntoLap >= phaseStart + phases[index].seconds) {
            phaseStart += phases[index].seconds
            index += 1
        }
        phase = phases[index]
        t = secondsIntoLap - phaseStart

        val before = phases.take(index)
        fun total(amount: Phase.(Double) -> Double) =
            laps * phases.sumOf { it.amount(it.seconds) } + before.sumOf { it.amount(it.seconds) } + phase.amount(t)
        distanceM = total(Phase::distance)
        chargeAs = total(Phase::charge)
        energyWs = total(Phase::energy)
    }
}

fun lapValues(elapsedMs: Long): ValuesSetup {
    val moment = Moment(elapsedMs)
    val phase = moment.phase
    val speed = phase.speed(moment.t)
    val batteryCurrent = phase.batteryCurrent(moment.t)
    val chargeAh = moment.chargeAs / 3600
    return ValuesSetup(
        mosfetTempC = 30 + 0.6 * phase.motorCurrentA,
        motorTempC = 35 + phase.motorCurrentA,
        motorCurrentA = phase.motorCurrentA,
        batteryCurrentA = batteryCurrent,
        dutyCycle = speed / NO_LOAD_SPEED_M_S,
        erpm = Math.round(speed * ERPM_PER_M_S).toInt(),
        speedMS = speed,
        batteryVoltageV = REST_VOLTAGE_V - RESISTANCE_OHM * batteryCurrent,
        batteryLevel = (1 - chargeAh / CAPACITY_AH).coerceAtLeast(0.0),
        chargeUsedAh = chargeAh,
        chargeChargedAh = 0.0,
        energyUsedWh = moment.energyWs / 3600,
        energyChargedWh = 0.0,
        distanceM = moment.distanceM,
        distanceAbsM = moment.distanceM,
        position = 0.0,
        faultCode = if (moment.secondsIntoLap in faultSeconds) FAULT_CODE else 0,
        boardId = 0,
        boardCount = 1,
        batteryCapacityWh = CAPACITY_AH * REST_VOLTAGE_V,
        odometerM = ODOMETER_START_M + moment.distanceM.toLong(),
        boardUptimeMs = elapsedMs,
    )
}

// The throttle is on ADC 1 and follows the motor current. ADC 2, the brake,
// is never pressed: the car coasts to its stop.
fun lapAdc(elapsedMs: Long): DecodedAdc {
    val throttle = Moment(elapsedMs).phase.motorCurrentA / FULL_THROTTLE_A
    return DecodedAdc(adcLevel1 = throttle, adcVoltage1 = 0.8 + 2.4 * throttle, adcLevel2 = 0.0, adcVoltage2 = 0.8)
}
