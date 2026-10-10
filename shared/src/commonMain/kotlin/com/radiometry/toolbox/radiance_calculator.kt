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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun RadianceVisualization(
    settingsManager: SettingsManager,
    temperatureText: String,
    onTemperatureTextChange: (String) -> Unit,
    temperatureUnit: TemperatureUnit,
    onTemperatureUnitChange: (TemperatureUnit) -> Unit,
) {
    var startWavelengthText by remember {
        mutableStateOf(settingsManager.getString("key_radiance_start_wavelength", "380"))
    }
    var stopWavelengthText by remember {
        mutableStateOf(settingsManager.getString("key_radiance_stop_wavelength", "780"))
    }
    var wavelengthUnit by remember {
        val savedName = settingsManager.getString("key_radiance_wavelength_unit", WavelengthUnit.NANOMETERS.name)
        mutableStateOf(runCatching { WavelengthUnit.valueOf(savedName) }.getOrDefault(WavelengthUnit.NANOMETERS))
    }

    val startVal = startWavelengthText.toDoubleOrNull()
    val stopVal = stopWavelengthText.toDoubleOrNull()
    val tempVal = temperatureText.toDoubleOrNull()

    val startMeters = startVal?.let { convertToWavelengthInMeters(it, wavelengthUnit) }
    val stopMeters = stopVal?.let { convertToWavelengthInMeters(it, wavelengthUnit) }
    val tempKelvin = tempVal?.let { convertTemperatureToKelvin(it, temperatureUnit) }

    val radianceResult = if (startMeters != null && stopMeters != null && tempKelvin != null) {
        calculateRadiance(startMeters, stopMeters, tempKelvin)
    } else {
        null
    }

    val radianceOutputStr = if (radianceResult != null) {
        formatRadiance(radianceResult)
    } else {
        "Invalid Input"
    }

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
        // Output display box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Calculated Output",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (radianceResult != null) "Radiance: $radianceOutputStr" else "Radiance: Invalid Input",
                    fontSize = 20.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        // Numerical textboxes for start and stop wavelength
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = startWavelengthText,
                onValueChange = {
                    startWavelengthText = it
                    settingsManager.putString("key_radiance_start_wavelength", it)
                },
                label = {
                    Text(
                        if (wavelengthUnit == WavelengthUnit.INVERSE_CENTIMETERS) "Start Wavenumber" else "Start Wavelength",
                        color = Color.White.copy(alpha = 0.8f),
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = textFieldColors,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = stopWavelengthText,
                onValueChange = {
                    stopWavelengthText = it
                    settingsManager.putString("key_radiance_stop_wavelength", it)
                },
                label = {
                    Text(
                        if (wavelengthUnit == WavelengthUnit.INVERSE_CENTIMETERS) "Stop Wavenumber" else "Stop Wavelength",
                        color = Color.White.copy(alpha = 0.8f),
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = textFieldColors,
                modifier = Modifier.weight(1f),
            )
        }

        // Numerical textbox for temperature
        OutlinedTextField(
            value = temperatureText,
            onValueChange = { onTemperatureTextChange(it) },
            label = { Text("Temperature", color = Color.White.copy(alpha = 0.8f)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = textFieldColors,
            modifier = Modifier.fillMaxWidth(),
        )

        // Dropdowns for wavelength unit and temperature unit
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            WavelengthUnitDropdown(
                label = "Wavelength Unit",
                selectedUnit = wavelengthUnit,
                onUnitSelected = {
                    wavelengthUnit = it
                    settingsManager.putString("key_radiance_wavelength_unit", it.name)
                },
                modifier = Modifier.weight(1f),
            )
            UnitDropdown(
                label = "Temperature Unit",
                selectedUnit = temperatureUnit,
                onUnitSelected = { onTemperatureUnitChange(it) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun WavelengthUnitDropdown(
    label: String,
    selectedUnit: WavelengthUnit,
    onUnitSelected: (WavelengthUnit) -> Unit,
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
            WavelengthUnit.entries.forEach { unit ->
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