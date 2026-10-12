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
fun LightSourceVisualization(settingsManager: SettingsManager) {
    var opticalPowerText by remember {
        mutableStateOf(settingsManager.getString("key_light_optical_power", "1.0"))
    }
    var divergenceText by remember {
        mutableStateOf(settingsManager.getString("key_light_divergence", "0.1"))
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
                settingsManager.getString("key_light_extinction", "0.9"),
            ),
        )
    }

    val opticalPower = opticalPowerText.toDoubleOrNull()
    val divergenceSr = divergenceText.toDoubleOrNull()
    val distance = distanceText.toDoubleOrNull()
    val transmissionAt1000m = transmissionText.toDoubleOrNull() ?: 0.9

    val irradianceResult = if (opticalPower != null && divergenceSr != null && distance != null) {
        calculateIrradiance(
            opticalPower = opticalPower,
            divergenceSr = divergenceSr,
            distance = distance,
            useAtmosphericCompensation = useAtmosphericCompensation,
            transmissionAt1000m = transmissionAt1000m,
        )
    } else {
        null
    }

    val irradianceOutputStr = if (irradianceResult != null) {
        formatIrradiance(irradianceResult)
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Calculated Irradiance",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = irradianceOutputStr,
                    fontSize = 20.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

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
            label = { Text("Divergence (sr)", color = Color.White.copy(alpha = 0.8f)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = textFieldColors,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = distanceText,
            onValueChange = {
                distanceText = it
                settingsManager.putString("key_light_distance", it)
            },
            label = { Text("Distance (m)", color = Color.White.copy(alpha = 0.8f)) },
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
