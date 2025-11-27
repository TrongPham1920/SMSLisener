package com.aquq.smslisener.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme

private val LightColors = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF6200EE),
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFBB86FC),
    secondary = androidx.compose.ui.graphics.Color(0xFF03DAC6),
    onSecondary = androidx.compose.ui.graphics.Color.Black,
    background = androidx.compose.ui.graphics.Color(0xFFFFFBFE),
    surface = androidx.compose.ui.graphics.Color(0xFFFFFBFE)
)

private val DarkColors = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFFBB86FC),
    onPrimary = androidx.compose.ui.graphics.Color.Black,
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF3700B3),
    secondary = androidx.compose.ui.graphics.Color(0xFF03DAC6),
    onSecondary = androidx.compose.ui.graphics.Color.Black,
    background = androidx.compose.ui.graphics.Color(0xFF121212),
    surface = androidx.compose.ui.graphics.Color(0xFF121212)
)

@Composable
fun SMSLisenerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

