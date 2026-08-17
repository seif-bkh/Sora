package com.sora.app.navigation

/**
 * Route constants.
 *
 * NOTE: there is deliberately no `TopLevelDestination` enum any more. That
 * enum existed to populate a bottom navigation bar, which DESIGN.md §3
 * removes — it was the strongest visual signature of the Tachiyomi lineage.
 *
 * Home and Discover are now pages of a [androidx.compose.foundation.pager]
 * pager inside a single route, not sibling destinations. Everything else is
 * pushed onto the back stack.
 */
object SoraRoutes {

    /** Hosts the Home ⇄ Discover pager. The app's start destination. */
    const val SHELL = "shell"

    const val AUTH = "auth"
    const val SETTINGS = "settings"
    const val SEARCH = "search"

    // Parameterised destinations, wired up in later phases.
    const val DETAILS = "details/{entryId}"
    const val PLAYER = "player/{unitId}"
    const val READER = "reader/{unitId}"

    fun details(entryId: String) = "details/$entryId"
    fun player(unitId: String) = "player/$unitId"
    fun reader(unitId: String) = "reader/$unitId"
}

/**
 * The two pages of the shell pager.
 *
 * Ordered: Home is index 0 and the landing page; Discover sits to its right,
 * reached by swiping left→right (or the compass glyph — DESIGN.md §6 requires
 * every gesture to have a non-gesture equivalent).
 */
enum class ShellPage(val index: Int) {
    HOME(0),
    DISCOVER(1),
    ;

    companion object {
        const val COUNT = 2

        fun fromIndex(index: Int): ShellPage =
            entries.firstOrNull { it.index == index } ?: HOME
    }
}
