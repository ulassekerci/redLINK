package org.metucet.redlink.board

import kotlinx.coroutines.flow.MutableStateFlow

// The simulated board as the rest of the app reaches it. Only a debug build
// has one: `simulation` is declared once in the debug source set and once in
// the release one, where it is null, so a release build holds none of the
// simulated board's code.
interface Simulation {
    // The "Stop answering" switch. While on, the simulated board answers
    // nothing.
    val stopAnswering: MutableStateFlow<Boolean>

    // A simulated board whose values follow this clock, the milliseconds
    // since the run started.
    fun board(elapsedMs: () -> Long): BoardLink
}
