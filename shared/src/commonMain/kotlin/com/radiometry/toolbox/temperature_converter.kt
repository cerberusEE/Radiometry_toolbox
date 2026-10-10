package com.radiometry.toolbox

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.roundToInt


@Composable
fun BlackbodyColorBox(kelvin: Double, modifier: Modifier = Modifier) {
    val color = kelvinToBlackbodyColor(kelvin)
    val description = blackbodyColorDescription(kelvin)

    val luminance = (0.299f * color.red + 0.587f * color.green + 0.114f * color.blue)
    val textColor = if (luminance > 0.5f) Color.Black else Color.White

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(color)
            .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Blackbody Color (${formatTemperature(kelvin)} K)",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor.copy(alpha = 0.85f),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
            )
        }
    }
}

@Composable
fun TemperatureVisualization(
    settingsManager: SettingsManager,
    temperatureText: String,
    onTemperatureTextChange: (String) -> Unit,
    temperatureUnit: TemperatureUnit,
    onTemperatureUnitChange: (TemperatureUnit) -> Unit,
) {
    var outputUnit by remember {
        val savedName = settingsManager.getString("key_temp_output_unit", TemperatureUnit.CELSIUS.name)
        mutableStateOf(runCatching { TemperatureUnit.valueOf(savedName) }.getOrDefault(TemperatureUnit.CELSIUS))
    }

    val inputValue = temperatureText.toDoubleOrNull()
    val convertedValue = inputValue?.let { convertTemperature(it, temperatureUnit, outputUnit) }
    val convertedKelvin = inputValue?.let { value ->
        when (temperatureUnit) {
            TemperatureUnit.CELSIUS -> value + 273.15
            TemperatureUnit.KELVIN -> value
            TemperatureUnit.FAHRENHEIT -> ((value - 32.0) * 5.0 / 9.0) + 273.15
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
        // Converted Results Display (above inputs, below heading)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Converted Result",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f),
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (convertedValue != null) {
                    Text(
                        text = "${formatTemperature(convertedValue)} ${outputUnit.symbol}",
                        fontSize = 22.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                } else {
                    Text(
                        text = "Invalid Input",
                        fontSize = 18.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        // Blackbody Color Box (below conversion results)
        if (convertedKelvin != null) {
            BlackbodyColorBox(kelvin = convertedKelvin)
        }

        // Input temperature textfield
        OutlinedTextField(
            value = temperatureText,
            onValueChange = { onTemperatureTextChange(it) },
            label = { Text("Input Temperature", color = Color.White.copy(alpha = 0.8f)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Color.White.copy(alpha = 0.6f),
                cursorColor = Color.White,
                focusedLabelColor = Color.White,
                unfocusedLabelColor = Color.White.copy(alpha = 0.8f),
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        // Dropdown menus for input and output units
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            UnitDropdown(
                label = "From Unit",
                selectedUnit = temperatureUnit,
                onUnitSelected = { onTemperatureUnitChange(it) },
                modifier = Modifier.weight(1f),
            )
            UnitDropdown(
                label = "To Unit",
                selectedUnit = outputUnit,
                onUnitSelected = {
                    outputUnit = it
                    settingsManager.putString("key_temp_output_unit", it.name)
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun UnitDropdown(
    label: String,
    selectedUnit: TemperatureUnit,
    onUnitSelected: (TemperatureUnit) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color.White,
            ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                    Text(
                        text = selectedUnit.label,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
                Text(
                    text = "▼",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.8f),
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            TemperatureUnit.entries.forEach { unit ->
                DropdownMenuItem(
                    text = { Text(unit.label) },
                    onClick = {
                        onUnitSelected(unit)
                        expanded = false
                    },
                )
            }
        }
    }
}

fun kelvinToBlackbodyColor(kelvin: Double): Color {
    // Below 700 K (~427°C), blackbody radiation is invisible to the human eye (infrared)
    if (kelvin < 700.0) {
        return Color.Black
    }

    // Between 700 K and 1000 K (includes 500°C = ~773 K), human perception sees a faint dull red glow
    if (kelvin < 1000.0) {
        val factor = (kelvin - 700.0) / (1000.0 - 700.0)
        val redInt = (factor * 200.0).roundToInt().coerceIn(0, 255)
        val greenInt = (factor * 30.0).roundToInt().coerceIn(0, 255)
        return Color(redInt, greenInt, 0)
    }

    val temp = kelvin.coerceIn(1000.0, 40000.0) / 100.0

    val r = if (temp <= 66.0) {
        255.0
    } else {
        329.698727446 * (temp - 60.0).pow(-0.1332047592)
    }

    val g = if (temp <= 66.0) {
        99.4708025861 * ln(temp) - 161.1195681661
    } else {
        288.1221695283 * (temp - 60.0).pow(-0.0755148492)
    }

    val b = when {
        temp >= 66.0 -> 255.0
        temp <= 19.0 -> 0.0
        else -> 138.5177312231 * ln(temp - 10.0) - 305.0447927307
    }

    val redInt = r.coerceIn(0.0, 255.0).roundToInt()
    val greenInt = g.coerceIn(0.0, 255.0).roundToInt()
    val blueInt = b.coerceIn(0.0, 255.0).roundToInt()

    return Color(redInt, greenInt, blueInt)
}

fun blackbodyColorDescription(kelvin: Double): String {
    return when {
        kelvin < 0.0 -> "Below Absolute Zero"
        kelvin < 700.0 -> "Infrared (Invisible / Black)"
        kelvin < 850.0 -> "Faint Dull Red Glow"
        kelvin < 1000.0 -> "Dark Cherry Red Glow"
        kelvin < 1500.0 -> "Bright Red-Orange Glow"
        kelvin < 2500.0 -> "Warm Orange Glow"
        kelvin < 3500.0 -> "Warm Soft White"
        kelvin < 5000.0 -> "Neutral White"
        kelvin < 6500.0 -> "Daylight White"
        kelvin < 9000.0 -> "Cool White"
        kelvin < 15000.0 -> "Sky Blue"
        else -> "Deep Stellar Blue"
    }
}

fun convertTemperature(value: Double, fromUnit: TemperatureUnit, toUnit: TemperatureUnit): Double {
    val tempInKelvin = when (fromUnit) {
        TemperatureUnit.CELSIUS -> value + 273.15
        TemperatureUnit.KELVIN -> value
        TemperatureUnit.FAHRENHEIT -> ((value - 32.0) * 5.0 / 9.0) + 273.15
    }
    return when (toUnit) {
        TemperatureUnit.CELSIUS -> tempInKelvin - 273.15
        TemperatureUnit.KELVIN -> tempInKelvin
        TemperatureUnit.FAHRENHEIT -> ((tempInKelvin - 273.15) * 9.0 / 5.0) + 32.0
    }
}

fun formatTemperature(value: Double): String {
    if (value.isNaN() || value.isInfinite()) return "N/A"
    val rounded = round(value * 100.0) / 100.0
    return if (abs(rounded % 1.0) < 0.0001) {
        rounded.toLong().toString()
    } else {
        val str = rounded.toString()
        if (str.contains('.')) {
            val parts = str.split('.')
            val decimals = parts[1].take(2)
            "${parts[0]}.$decimals"
        } else {
            str
        }
    }
}