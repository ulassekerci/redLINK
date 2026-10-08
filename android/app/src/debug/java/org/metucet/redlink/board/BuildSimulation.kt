package org.metucet.redlink.board

import kotlinx.coroutines.flow.MutableStateFlow

val simulation: Simulation? = object : Simulation {
    override val stopAnswering = MutableStateFlow(false)

    override fun board(elapsedMs: () -> Long) = SimulatedBoard(elapsedMs) { !stopAnswering.value }
}
