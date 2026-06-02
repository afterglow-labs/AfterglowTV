package com.afterglowtv.app.ui.design

import androidx.compose.ui.unit.dp

object FocusSpec {
    // Existing constants kept compiling — value tuned closer to AfterglowTV's flatter look.
    const val FocusedScale = 1.05f
    const val PressedScale = 0.98f
    val BorderWidth = 5.dp
    val CardBorderWidth = 5.dp

    // Afterglow-specific roles for the new afterglowFocus() modifier.
    const val RowFocusedScale = 1.00f       // TV rows are flat — drawable swap only.
    const val CardFocusedScale = 1.04f      // gentle lift on posters/cards.
    val CornerRadius = 4.dp                 // AfterglowTV canonical corner radius.
}
