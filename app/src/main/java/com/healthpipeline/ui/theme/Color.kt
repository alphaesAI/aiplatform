package com.healthpipeline.ui.theme

import androidx.compose.ui.graphics.Color

// NOTHING-INSPIRED MONOCHROMATIC THEME

// BASE COLORS - Pure Black & White
val PureBlack = Color(0xFF000000)
val PureWhite = Color(0xFFFFFFFF)

// GRAY SCALE
val Gray50 = Color(0xFFFAFAFA)
val Gray100 = Color(0xFFF5F5F5)
val Gray200 = Color(0xFFEEEEEE)
val Gray300 = Color(0xFFE0E0E0)
val Gray400 = Color(0xFFBDBDBD)
val Gray500 = Color(0xFF9E9E9E)
val Gray600 = Color(0xFF757575)
val Gray700 = Color(0xFF616161)
val Gray800 = Color(0xFF424242)
val Gray900 = Color(0xFF212121)
val Gray950 = Color(0xFF0A0A0A)

// ACCENT COLOR - Nothing Red
val NothingRed = Color(0xFFFF0000)
val NothingRedDark = Color(0xFFCC0000)
val NothingRedLight = Color(0xFFFF3333)

// SURFACE COLORS
val SurfaceLight = PureWhite
val SurfaceDark = Gray950
val SurfaceVariantLight = Gray100
val SurfaceVariantDark = Gray900

// BACKGROUND COLORS
val BackgroundLight = Gray50
val BackgroundDark = PureBlack

// TEXT COLORS
val TextPrimaryLight = PureBlack
val TextSecondaryLight = Gray700
val TextTertiaryLight = Gray500

val TextPrimaryDark = PureWhite
val TextSecondaryDark = Gray300
val TextTertiaryDark = Gray500

// BORDER COLORS
val BorderLight = Gray300
val BorderDark = Gray800

// STATUS COLORS (Minimal)
val StatusSuccess = Gray600  // Subtle gray-green
val StatusWarning = Gray500
val StatusError = NothingRed
val StatusInfo = Gray700
