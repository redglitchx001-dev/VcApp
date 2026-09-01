package com.vcapp.voicechanger.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.animateFloatAsState

/** Small reusable card with a title. */
@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

/** Horizontal VU meter, level is 0..1. */
@Composable
fun LevelMeter(level: Float, label: String, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(level.coerceIn(0f, 1f), label = "level")
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(animated)
                    .height(12.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF22D3EE), Color(0xFF7C3AED), Color(0xFFFF6B6B))
                        )
                    )
            )
        }
    }
}

@Composable
fun StatusDot(active: Boolean) {
    val color by animateColorAsState(
        if (active) Color(0xFF4ADE80) else Color(0xFF6B6880), label = "dot"
    )
    Box(
        Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
fun ScrollColumn(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) { content() }
}

@Composable
fun LabeledRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}
