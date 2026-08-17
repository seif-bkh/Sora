package com.sora.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sora.app.ui.theme.MicroLabel
import com.sora.app.ui.theme.SoraTheme

/**
 * Temporary screen used by every route until its feature module is built.
 *
 * Exists so each phase produces a genuinely navigable app rather than a blank
 * shell - each placeholder names the phase that will replace it.
 *
 * The surface is transparent so the shell's ambient wash shows through;
 * an opaque surface here would flatten the whole background treatment.
 */
@Composable
fun PlaceholderScreen(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    microLabel: String? = null,
    onPrimaryAction: (() -> Unit)? = null,
    primaryActionLabel: String? = null,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.Transparent,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (microLabel != null) {
                Text(
                    text = microLabel,
                    style = MicroLabel,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 12.dp)
                    // Keeps line length readable on tablets rather than
                    // stretching across the full width.
                    .widthIn(max = 420.dp),
            )
            if (onPrimaryAction != null && primaryActionLabel != null) {
                Button(
                    onClick = onPrimaryAction,
                    modifier = Modifier.padding(top = 24.dp),
                ) {
                    Text(primaryActionLabel)
                }
            }
        }
    }
}

@Preview(name = "Placeholder - dark", showBackground = true)
@Composable
private fun PlaceholderScreenPreview() {
    SoraTheme {
        PlaceholderScreen(
            title = "Continue",
            description = "Local and server content will appear here.",
            microLabel = "HOME",
            onPrimaryAction = {},
            primaryActionLabel = "Add a folder",
        )
    }
}
