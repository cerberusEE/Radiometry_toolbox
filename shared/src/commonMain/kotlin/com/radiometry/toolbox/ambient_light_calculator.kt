package com.radiometry.toolbox

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.log10

fun getLightEnvironmentCategory(lux: Float): String {
    return when {
        lux < 1.0f -> "Night / Starlight (< 1 lx)"
        lux < 10.0f -> "Moonlight / Twilight (1 - 10 lx)"
        lux < 100.0f -> "Dim Indoor / Hallway (10 - 100 lx)"
        lux < 500.0f -> "Normal Indoor / Office (100 - 500 lx)"
        lux < 1000.0f -> "Bright Indoor / Workspace (500 - 1000 lx)"
        lux < 10000.0f -> "Overcast Daylight / Shade (1000 - 10000 lx)"
        else -> "Direct Sunlight (> 10000 lx)"
    }
}

@Composable
fun AmbientLightVisualization(settingsManager: SettingsManager) {
    val lightSensor = rememberAmbientLightSensor()
    val isAvailable = remember { lightSensor.isSensorAvailable() }

    var currentLux by remember { mutableFloatStateOf(0f) }
    var minLux by remember { mutableStateOf<Float?>(null) }
    var maxLux by remember { mutableStateOf<Float?>(null) }
    var totalLuxSum by remember { mutableFloatStateOf(0f) }
    var sampleCount by remember { mutableIntStateOf(0) }

    DisposableEffect(isAvailable) {
        if (isAvailable) {
            lightSensor.startListening { lux ->
                currentLux = lux
                minLux = minOf(minLux ?: lux, lux)
                maxLux = maxOf(maxLux ?: lux, lux)
                totalLuxSum += lux
                sampleCount++
            }
        }
        onDispose {
            if (isAvailable) {
                lightSensor.stopListening()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (!isAvailable) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "AMBIENT LIGHT SENSOR",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Unavailable",
                        color = Color(0xFFFF8A80),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "No ambient light sensor detected on this device.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                    )
                }
            }
        } else {
            // Live Lux Reading Display Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .padding(16.dp),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(
                        text = "AMBIENT ILLUMINANCE",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "${formatNumbers(currentLux.toDouble())} lx",
                            color = Color(0xFFFFD54F),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Text(
                        text = getLightEnvironmentCategory(currentLux),
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            // Visual Light Intensity Gauge Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "LIGHT INTENSITY SCALE",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )

                // Logarithmic progress bar calculation: 0 lx -> 0%, 100,000 lx -> 100%
                val logFraction = if (currentLux <= 0.1f) 0f else {
                    (log10(currentLux.coerceIn(0.1f, 100000f)) / 5.0f).coerceIn(0f, 1f)
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                ) {
                    // Track background
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.2f),
                        size = size,
                        cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                    )

                    // Fill bar
                    if (logFraction > 0f) {
                        drawRoundRect(
                            color = Color(0xFFFFB300),
                            size = Size(size.width * logFraction, size.height),
                            cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("0.1 lx", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                    Text("100 lx", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                    Text("10,000 lx", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                    Text("100,000 lx", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                }
            }

            // Session Statistics Display Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(12.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "SESSION STATISTICS",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Min: ${formatNumbers((minLux ?: 0f).toDouble())} lx",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Max: ${formatNumbers((maxLux ?: 0f).toDouble())} lx",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp
                        )
                        val avg = if (sampleCount > 0) totalLuxSum / sampleCount else 0f
                        Text(
                            text = "Avg: ${formatNumbers(avg.toDouble())} lx",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    minLux = currentLux
                    maxLux = currentLux
                    totalLuxSum = currentLux
                    sampleCount = 1
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            ) {
                Text("Reset Statistics")
            }
        }
    }
}
