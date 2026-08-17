package com.sora.app.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sora.app.navigation.ShellPage
import com.sora.app.ui.theme.LocalAmbientColors
import com.sora.app.ui.theme.MicroLabel
import kotlinx.coroutines.launch

/**
 * The app shell: Home ⇄ Discover, with no bottom navigation bar.
 *
 * DESIGN.md §3. The bottom bar is gone; the two primary surfaces are pages of
 * a horizontal pager, so the main navigation action is a thumb swipe. Only two
 * glyphs persist — avatar and search — plus a compass that mirrors the swipe.
 *
 * ADAPTIVE (§3)
 *   Compact           -> pager, glyphs floating over content
 *   Medium / Expanded -> icon-only rail on the left, pager beside it
 *
 * The rail is deliberately unlabelled: a labelled rail is a bottom bar rotated
 * 90° and would reintroduce exactly the look being removed.
 */
@Composable
fun SoraShell(
    windowSizeClass: WindowSizeClass,
    onOpenSettings: () -> Unit,
    onOpenSearch: () -> Unit,
    modifier: Modifier = Modifier,
    homeContent: @Composable () -> Unit,
    discoverContent: @Composable () -> Unit,
) {
    val pagerState = rememberPagerState(
        initialPage = ShellPage.HOME.index,
        pageCount = { ShellPage.COUNT },
    )
    val scope = rememberCoroutineScope()
    val ambient = LocalAmbientColors.current

    val isCompact = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact

    // Ambient wash. In Compose this is a radial gradient rather than a real
    // blur: the showcase's CSS `filter: blur(120px)` has no cheap equivalent
    // (RenderEffect is API 31+ and costs GPU time on every scrolled frame).
    // Same visual result, a fraction of the cost.
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        AmbientWash(color = ambient.glow)

        Row(modifier = Modifier.fillMaxSize()) {
            if (!isCompact) {
                ShellRail(
                    currentPage = ShellPage.fromIndex(pagerState.currentPage),
                    onSelectPage = { page ->
                        scope.launch { pagerState.animateScrollToPage(page.index) }
                    },
                    onOpenSearch = onOpenSearch,
                    onOpenSettings = onOpenSettings,
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(SHELL_PAGER_TAG),
                    // Keeps both pages composed so swiping does not re-fetch
                    // and re-lay-out the neighbour every time.
                    beyondViewportPageCount = 1,
                ) { page ->
                    when (ShellPage.fromIndex(page)) {
                        ShellPage.HOME -> homeContent()
                        ShellPage.DISCOVER -> discoverContent()
                    }
                }

                // Compact keeps the glyphs floating over the content, so the
                // artwork still runs edge to edge.
                if (isCompact) {
                    CompactGlyphs(
                        currentPage = ShellPage.fromIndex(pagerState.currentPage),
                        onOpenSettings = onOpenSettings,
                        onOpenSearch = onOpenSearch,
                        onToggleDiscover = {
                            val target = if (pagerState.currentPage == ShellPage.HOME.index) {
                                ShellPage.DISCOVER
                            } else {
                                ShellPage.HOME
                            }
                            scope.launch { pagerState.animateScrollToPage(target.index) }
                        },
                    )
                }
            }
        }
    }
}

/**
 * Soft radial wash behind everything, tinted by the current artwork.
 */
@Composable
private fun AmbientWash(color: Color) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(color, Color.Transparent),
                    radius = AMBIENT_RADIUS_PX,
                ),
            )
            .alpha(AMBIENT_WASH_ALPHA),
    )
}

private const val AMBIENT_RADIUS_PX = 1200f
private const val AMBIENT_WASH_ALPHA = 0.5f

/**
 * Floating controls for compact widths.
 *
 * Every one of these is the non-gesture equivalent of a gesture, which
 * DESIGN.md §6 makes mandatory: the compass mirrors the swipe, search mirrors
 * pull-down. No function is reachable by gesture alone.
 */
@Composable
private fun CompactGlyphs(
    currentPage: ShellPage,
    onOpenSettings: () -> Unit,
    onOpenSearch: () -> Unit,
    onToggleDiscover: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(WindowInsets.statusBars.asPaddingValues())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlyphButton(
            onClick = onOpenSettings,
            description = "Account and settings",
        ) {
            Icon(
                imageVector = Icons.Outlined.AccountCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }

        // Names the current page. Without this the pager has no visible
        // affordance at all, and a user cannot tell the second page exists.
        Text(
            text = when (currentPage) {
                ShellPage.HOME -> "SORA"
                ShellPage.DISCOVER -> "DISCOVER"
            },
            style = MicroLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row {
            GlyphButton(
                onClick = onToggleDiscover,
                description = when (currentPage) {
                    ShellPage.HOME -> "Open discover"
                    ShellPage.DISCOVER -> "Back to home"
                },
            ) {
                Icon(
                    imageVector = Icons.Outlined.Explore,
                    contentDescription = null,
                    tint = if (currentPage == ShellPage.DISCOVER) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            GlyphButton(onClick = onOpenSearch, description = "Search") {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

/**
 * Icon-only rail for medium and expanded widths.
 */
@Composable
private fun ShellRail(
    currentPage: ShellPage,
    onSelectPage: (ShellPage) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(RAIL_WIDTH)
            .padding(WindowInsets.statusBars.asPaddingValues())
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        GlyphButton(
            onClick = { onSelectPage(ShellPage.HOME) },
            description = "Home",
        ) {
            // The 空 mark stands in for "home" — it is the app's own glyph,
            // and avoids the house icon every other library app uses.
            Text(
                text = "空",
                style = MaterialTheme.typography.titleLarge,
                color = if (currentPage == ShellPage.HOME) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }

        GlyphButton(
            onClick = { onSelectPage(ShellPage.DISCOVER) },
            description = "Discover",
        ) {
            Icon(
                imageVector = Icons.Outlined.Explore,
                contentDescription = null,
                tint = if (currentPage == ShellPage.DISCOVER) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }

        GlyphButton(onClick = onOpenSearch, description = "Search") {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Box(modifier = Modifier.weight(1f))

        GlyphButton(onClick = onOpenSettings, description = "Account and settings") {
            Icon(
                imageVector = Icons.Outlined.AccountCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val RAIL_WIDTH = 72.dp

/** Lets UI tests drive the pager directly rather than guessing at coordinates. */
const val SHELL_PAGER_TAG = "shell_pager"

/**
 * A glyph-only control.
 *
 * [description] is mandatory rather than optional: DESIGN.md §6 requires every
 * glyph-only control to be labelled for screen readers, and making the
 * parameter non-null is how that stays true as controls are added. The 48dp
 * minimum touch target is enforced here too, even where the icon is smaller.
 */
@Composable
private fun GlyphButton(
    onClick: () -> Unit,
    description: String,
    content: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(TOUCH_TARGET)
            .semantics { contentDescription = description },
    ) {
        content()
    }
}

private val TOUCH_TARGET = 48.dp
