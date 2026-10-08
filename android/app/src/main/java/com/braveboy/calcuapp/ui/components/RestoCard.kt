package com.braveboy.calcuapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.braveboy.calcuapp.ui.theme.RestoShapes
import com.braveboy.calcuapp.ui.theme.RestoSpacing

enum class RestoCardVariant {
    Filled,
    Elevated,
    Outlined
}

@Composable
fun RestoCard(
    modifier: Modifier = Modifier,
    variant: RestoCardVariant = RestoCardVariant.Filled,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentPadding: Dp = RestoSpacing.md,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardColors = CardDefaults.cardColors(containerColor = containerColor)
    val shape = RestoShapes.large

    if (onClick != null) {
        when (variant) {
            RestoCardVariant.Filled -> {
                Card(
                    onClick = onClick,
                    shape = shape,
                    colors = cardColors,
                    modifier = modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(contentPadding), content = content)
                }
            }
            RestoCardVariant.Elevated -> {
                Card(
                    onClick = onClick,
                    shape = shape,
                    colors = cardColors,
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(contentPadding), content = content)
                }
            }
            RestoCardVariant.Outlined -> {
                OutlinedCard(
                    onClick = onClick,
                    shape = shape,
                    colors = cardColors,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(contentPadding), content = content)
                }
            }
        }
    } else {
        when (variant) {
            RestoCardVariant.Filled -> {
                Card(
                    shape = shape,
                    colors = cardColors,
                    modifier = modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(contentPadding), content = content)
                }
            }
            RestoCardVariant.Elevated -> {
                Card(
                    shape = shape,
                    colors = cardColors,
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(contentPadding), content = content)
                }
            }
            RestoCardVariant.Outlined -> {
                OutlinedCard(
                    shape = shape,
                    colors = cardColors,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(contentPadding), content = content)
                }
            }
        }
    }
}
