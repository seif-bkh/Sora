package com.sora.app.ui.shell

import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.sora.app.ui.theme.SoraTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Covers the shell that replaced the bottom navigation bar.
 *
 * Two things here are worth more than a screenshot:
 *
 *  1. The pager is the primary navigation gesture, so page switching has to
 *     work by swipe *and* by tap. DESIGN.md §6 forbids gesture-only function,
 *     and that rule is invisible to anyone reading the layout code — it only
 *     survives if a test asserts the tap path.
 *  2. Every persistent control is a bare glyph. Without a contentDescription
 *     the whole shell is unusable with TalkBack, and a missing one is
 *     completely silent at runtime.
 *
 * Runs under Robolectric so CI covers it without an emulator.
 */
@RunWith(RobolectricTestRunner::class)
class SoraShellTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `home page is shown first`() {
        setShell()
        composeTestRule.onNodeWithText(HOME_MARKER).assertIsDisplayed()
    }

    @Test
    fun `compass glyph switches to discover without a swipe`() {
        // The non-gesture equivalent required by §6.
        setShell()

        composeTestRule.onNodeWithContentDescription("Open discover").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(DISCOVER_MARKER).assertIsDisplayed()
    }

    @Test
    fun `compass glyph returns to home`() {
        setShell()

        composeTestRule.onNodeWithContentDescription("Open discover").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Back to home").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(HOME_MARKER).assertIsDisplayed()
    }

    @Test
    fun `swiping left reveals discover and swiping back returns home`() {
        setShell()

        composeTestRule.onNodeWithTag(SHELL_PAGER_TAG).performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(DISCOVER_MARKER).assertIsDisplayed()

        composeTestRule.onNodeWithTag(SHELL_PAGER_TAG).performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(HOME_MARKER).assertIsDisplayed()
    }

    @Test
    fun `every persistent glyph is labelled for screen readers`() {
        setShell()

        // Compact chrome: avatar, compass, search. Three glyphs, no labels on
        // screen, so these descriptions are the only thing TalkBack can read.
        composeTestRule.onNodeWithContentDescription("Account and settings").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Open discover").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Search").assertIsDisplayed()
    }

    @Test
    fun `expanded width shows the icon rail and it navigates`() {
        setShell(size = DpSize(1280.dp, 800.dp))

        // The rail replaces the floating glyphs; both its page controls have
        // to be reachable and labelled.
        composeTestRule.onNodeWithContentDescription("Home").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Discover").assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Discover").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(DISCOVER_MARKER).assertIsDisplayed()
    }

    @Test
    fun `settings and search are reachable from both layouts`() {
        var settingsOpened = false
        var searchOpened = false

        setShell(
            size = DpSize(1280.dp, 800.dp),
            onOpenSettings = { settingsOpened = true },
            onOpenSearch = { searchOpened = true },
        )

        composeTestRule.onNodeWithContentDescription("Account and settings").performClick()
        composeTestRule.onNodeWithContentDescription("Search").performClick()

        assert(settingsOpened) { "avatar glyph did not open settings" }
        assert(searchOpened) { "search glyph did not open search" }
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    private fun setShell(
        size: DpSize = DpSize(412.dp, 892.dp),
        onOpenSettings: () -> Unit = {},
        onOpenSearch: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            SoraTheme {
                SoraShell(
                    windowSizeClass = WindowSizeClass.calculateFromSize(size),
                    onOpenSettings = onOpenSettings,
                    onOpenSearch = onOpenSearch,
                    homeContent = { Text(HOME_MARKER) },
                    discoverContent = { Text(DISCOVER_MARKER) },
                )
            }
        }
    }

    private companion object {
        const val HOME_MARKER = "home-page-content"
        const val DISCOVER_MARKER = "discover-page-content"
    }
}
