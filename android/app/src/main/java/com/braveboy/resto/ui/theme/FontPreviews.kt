package com.braveboy.resto.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import com.braveboy.resto.ui.dashboard.components.DashboardRedesignView

@Preview(name = "Shabnam Font Main Screen Preview", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun ShabnamFontPreview() {
    RestoTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            DashboardRedesignView()
        }
    }
}
