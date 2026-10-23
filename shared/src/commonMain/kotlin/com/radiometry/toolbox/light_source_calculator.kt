package com.radiometry.toolbox

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.pow

enum class LightSourceCalculationMode {
    POWER_AND_DIVERGENCE,      // Mode 1: Optical Power & Divergence -> Irradiance & Intensity
    INTENSITY_AND_RANGE,       // Mode 2: Radiant Intensity -> Irradiance
    INTENSITY_FROM_IRRADIANCE  // Mode 3: Target Irradiance -> Required Radiant Intensity
}

@Composable
fun LightSourceVisualization(settingsManager: SettingsManager) {
    var modeIndex by remember {
        mutableIntStateOf(
            settingsManager.getInt("key_light_mode", 0).coerceIn(0, 2)
        )
    }

    val selectedMode = when (modeIndex) {
        0 -> LightSourceCalculationMode.POWER_AND_DIVERGENCE
        1 -> LightSourceCalculationMode.INTENSITY_AND_RANGE
        else -> LightSourceCalculationMode.INTENSITY_FROM_IRRADIANCE
    }

    var opticalPowerText by remember {
        mutableStateOf(settingsManager.getString("key_light_optical_power", "1.0"))
    }
    var divergenceText by remember {
        mutableStateOf(settingsManager.getString("key_light_divergence", "0.1"))
    }
    var intensityText by remember {
        mutableStateOf(settingsManager.getString("key_light_intensity", "10.0"))
    }
    var targetIrradianceText by remember {
        mutableStateOf(settingsManager.getString("key_light_target_irradiance", "0.01"))
    }
    var distanceText by remember {
        mutableStateOf(settingsManager.getString("key_light_distance", "1.0"))
    }
    var useAtmosphericCompensation by remember {
        mutableStateOf(settingsManager.getBoolean("key_light_use_atm_comp", false))
    }
    var transmissionText by remember {
        mutableStateOf(
            settingsManager.getString(
                "key_light_transmission",
                settingsManager.getString("key_light_extinction", "0.9")
            )
        )
    }

    val opticalPower = opticalPowerText.toDoubleOrNull()
    val divergenceSr = divergenceText.toDoubleOrNull()
    val radiantIntensityInput = intensityText.toDoubleOrNull()
    val targetIrradianceInput = targetIrradianceText.toDoubleOrNull()
    val distance = distanceText.toDoubleOrNull()
    val transmissionAt1000m = transmissionText.toDoubleOrNull() ?: 0.9

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedBorderColor = Color.White,
        unfocusedBorderColor = Color.White.copy(alpha = 0.6f),
        cursorColor = Color.White,
        focusedLabelColor = Color.White,
        unfocusedLabelColor = Color.White.copy(alpha = 0.8f),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Mode Selector Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val modes = listOf(
                0 to "Power &\nDivergence",
                1 to "Intensity &\nRange",
                2 to "Intensity from\nIrradiance"
            )

            modes.forEach { (idx, label) ->
                val isSelected = (idx == modeIndex)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .background(
                            color = if (isSelected) Color.White.copy(alpha = 0.25f) else Color.Transparent,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable {
                            modeIndex = idx
                            settingsManager.putInt("key_light_mode", idx)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        lineHeight = 13.sp
                    )
                }
            }
        }

        // Output Result Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                when (selectedMode) {
                    LightSourceCalculationMode.POWER_AND_DIVERGENCE -> {
                        Text(
                            text = "CALCULATED IRRADIANCE (E)",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (opticalPower != null && divergenceSr != null && distance != null) {
                            val intensity = calculateRadiantIntensity(opticalPower, divergenceSr)
                            val irradiance = calculateIrradianceFromIntensity(
                                radiantIntensity = intensity,
                                distanceMeters = distance,
                                useAtmosphericCompensation = useAtmosphericCompensation,
                                transmissionAt1000m = transmissionAt1000m
                            )
                            Text(
                                text = formatIrradiance(irradiance),
                                fontSize = 20.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                            )
                        } else {
                            Text("Invalid Input", fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    LightSourceCalculationMode.INTENSITY_AND_RANGE -> {
                        Text(
                            text = "CALCULATED IRRADIANCE (E)",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (radiantIntensityInput != null && distance != null) {
                            val irradiance = calculateIrradianceFromIntensity(
                                radiantIntensity = radiantIntensityInput,
                                distanceMeters = distance,
                                useAtmosphericCompensation = useAtmosphericCompensation,
                                transmissionAt1000m = transmissionAt1000m
                            )
                            Text(
                                text = formatIrradiance(irradiance),
                                fontSize = 20.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                            )
                        } else {
                            Text("Invalid Input", fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    LightSourceCalculationMode.INTENSITY_FROM_IRRADIANCE -> {
                        Text(
                            text = "REQUIRED RADIANT INTENSITY (I)",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (targetIrradianceInput != null && distance != null) {
                            val requiredIntensity = calculateIntensityFromIrradiance(
                                irradianceWPerM2 = targetIrradianceInput,
                                distanceMeters = distance,
                                useAtmosphericCompensation = useAtmosphericCompensation,
                                transmissionAt1000m = transmissionAt1000m
                            )
                            Text(
                                text = formatRadiantIntensity(requiredIntensity),
                                fontSize = 20.sp,
                                color = Color(0xFFFFD54F),
                                fontWeight = FontWeight.Bold,
                            )
                        } else {
                            Text("Invalid Input", fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (useAtmosphericCompensation && distance != null && distance > 0.0) {
                    val tRef = when {
                        transmissionAt1000m <= 0.0 -> 0.0
                        transmissionAt1000m > 1.0 && transmissionAt1000m <= 100.0 -> transmissionAt1000m / 100.0
                        transmissionAt1000m > 100.0 -> 1.0
                        else -> transmissionAt1000m
                    }
                    val pathTransmittancePct = (tRef.pow(distance / 1000.0) * 100.0).coerceIn(0.0, 100.0)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Path Transmittance: ${formatNumbers(pathTransmittancePct)}%",
                        fontSize = 11.sp,
                        color = Color(0xFF81C784)
                    )
                }
            }
        }

        // Input Fields (conditioned on selected mode)
        when (selectedMode) {
            LightSourceCalculationMode.POWER_AND_DIVERGENCE -> {
                OutlinedTextField(
                    value = opticalPowerText,
                    onValueChange = {
                        opticalPowerText = it
                        settingsManager.putString("key_light_optical_power", it)
                    },
                    label = { Text("Optical Power (W)", color = Color.White.copy(alpha = 0.8f)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = divergenceText,
                    onValueChange = {
                        divergenceText = it
                        settingsManager.putString("key_light_divergence", it)
                    },
                    label = { Text("Beam Divergence (sr)", color = Color.White.copy(alpha = 0.8f)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            LightSourceCalculationMode.INTENSITY_AND_RANGE -> {
                OutlinedTextField(
                    value = intensityText,
                    onValueChange = {
                        intensityText = it
                        settingsManager.putString("key_light_intensity", it)
                    },
                    label = { Text("Radiant Intensity (W/sr)", color = Color.White.copy(alpha = 0.8f)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            LightSourceCalculationMode.INTENSITY_FROM_IRRADIANCE -> {
                OutlinedTextField(
                    value = targetIrradianceText,
                    onValueChange = {
                        targetIrradianceText = it
                        settingsManager.putString("key_light_target_irradiance", it)
                    },
                    label = { Text("Target Irradiance (W/m²)", color = Color.White.copy(alpha = 0.8f)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        OutlinedTextField(
            value = distanceText,
            onValueChange = {
                distanceText = it
                settingsManager.putString("key_light_distance", it)
            },
            label = { Text("Range / Distance (m)", color = Color.White.copy(alpha = 0.8f)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = textFieldColors,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val newValue = !useAtmosphericCompensation
                    useAtmosphericCompensation = newValue
                    settingsManager.putBoolean("key_light_use_atm_comp", newValue)
                }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.material3.Checkbox(
                checked = useAtmosphericCompensation,
                onCheckedChange = {
                    useAtmosphericCompensation = it
                    settingsManager.putBoolean("key_light_use_atm_comp", it)
                },
            )
            Spacer(modifier = Modifier.padding(4.dp))
            Text(
                text = "Atmospheric Compensation",
                color = Color.White,
            )
        }

        if (useAtmosphericCompensation) {
            OutlinedTextField(
                value = transmissionText,
                onValueChange = {
                    transmissionText = it
                    settingsManager.putString("key_light_transmission", it)
                },
                label = { Text("Transmission at 1000 m (0 to 1)", color = Color.White.copy(alpha = 0.8f)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = textFieldColors,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
