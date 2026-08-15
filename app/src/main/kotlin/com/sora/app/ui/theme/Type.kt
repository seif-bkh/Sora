package com.sora.app.ui.theme

import androidx.compose.material3.Typography

/**
 * Typography.
 *
 * Uses the Material 3 defaults for now. The platform default font already
 * covers the CJK glyphs Sora needs (the 空 in the wordmark renders from the
 * system font stack), so no font is bundled in the APK. Custom type scales, if
 * any, land in Phase 8 polish.
 */
val SoraTypography = Typography()
