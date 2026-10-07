package com.noise.applens.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Minimal typed back stack driven by Compose state.
 *
 * The project has no navigation artifact and this app has four screens with simple arguments, so a
 * dependency is not justified (spec §19: reuse what is sufficient). See
 * `docs/decisions/004-minimal-navigation-state-holder.md`.
 */
class AppNavigator(initial: Screen = Screen.Dashboard) {

    var backStack: List<Screen> by mutableStateOf(listOf(initial))
        private set

    /** Top of the back stack — the screen currently rendered. */
    val current: Screen get() = backStack.last()

    val canGoBack: Boolean get() = backStack.size > 1

    fun navigateTo(screen: Screen) {
        if (screen == current) return
        backStack = backStack + screen
    }

    /** Returns `true` when a screen was popped. */
    fun pop(): Boolean {
        if (!canGoBack) return false
        backStack = backStack.dropLast(1)
        return true
    }

    /** Returns to the dashboard, used when an entry point no longer exists. */
    fun popToRoot() {
        backStack = backStack.take(1)
    }
}
