package com.radiometry.toolbox

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun IfovGsdVisualization(settingsManager: SettingsManager) {
    var modeIndex by remember {
        mutableStateOf(settingsManager.getInt("key_ifov_calc_mode", 0).coerceIn(0, 1))
    }

    var focalLengthText by remember {
        mutableStateOf(settingsManager.getString("key_ifov_focal_length", "50"))
    }
    var pixelPitchText by remember {
        mutableStateOf(settingsManager.getString("key_ifov_pixel_pitch", "10"))
    }
    var ifovUnit by remember {
        val savedName = settingsManager.getString("key_ifov_unit", IfovUnit.MILLIRADIANS.name)
        mutableStateOf(runCatching { IfovUnit.valueOf(savedName) }.getOrDefault(IfovUnit.MILLIRADIANS))
    }

    val initialIfovText = remember {
        val saved = settingsManager.getString("key_ifov_text", "")
        saved.ifEmpty {
            val f = "50".toDoubleOrNull()
            val p = "10".toDoubleOrNull()
            if (f != null && p != null) {
                val rad = calculateIfovInRadians(f, p)
                val valInUnit = rad?.let { convertIfovFromRadians(it, IfovUnit.MILLIRADIANS) }
                valInUnit?.let { formatNumbers(it) } ?: ""
            } else ""
        }
    }

    var ifovText by remember { mutableStateOf(initialIfovText) }
    var distanceText by remember {
        mutableStateOf(settingsManager.getString("key_ifov_distance", "100"))
    }
    var pixelCountText by remember {
        mutableStateOf(settingsManager.getString("key_ifov_pixel_count", "1920"))
    }
    var objectSizeText by remember {
        mutableStateOf(settingsManager.getString("key_ifov_object_size", "2.0"))
    }
    var objectPixelsText by remember {
        mutableStateOf(settingsManager.getString("key_ifov_object_pixels", "50"))
    }

    var isIfovUnitDropdownExpanded by remember { mutableStateOf(false) }

    val onFocalLengthChange: (String) -> Unit = { newFocalStr ->
        focalLengthText = newFocalStr
        settingsManager.putString("key_ifov_focal_length", newFocalStr)

        val fMm = newFocalStr.toDoubleOrNull()
        val pUm = pixelPitchText.toDoubleOrNull()
        if (fMm != null && fMm > 0 && pUm != null && pUm > 0) {
            val rad = calculateIfovInRadians(fMm, pUm)
            val valInUnit = rad?.let { convertIfovFromRadians(it, ifovUnit) }
            if (valInUnit != null) {
                val formatted = formatNumbers(valInUnit)
                ifovText = formatted
                settingsManager.putString("key_ifov_text", formatted)
            }
        }
    }

    val onPixelPitchChange: (String) -> Unit = { newPitchStr ->
        pixelPitchText = newPitchStr
        settingsManager.putString("key_ifov_pixel_pitch", newPitchStr)

        val fMm = focalLengthText.toDoubleOrNull()
        val pUm = newPitchStr.toDoubleOrNull()
        if (fMm != null && fMm > 0 && pUm != null && pUm > 0) {
            val rad = calculateIfovInRadians(fMm, pUm)
            val valInUnit = rad?.let { convertIfovFromRadians(it, ifovUnit) }
            if (valInUnit != null) {
                val formatted = formatNumbers(valInUnit)
                ifovText = formatted
                settingsManager.putString("key_ifov_text", formatted)
            }
        }
    }

    val onIfovChange: (String) -> Unit = { newIfovStr ->
        ifovText = newIfovStr
        settingsManager.putString("key_ifov_text", newIfovStr)

        val ifovVal = newIfovStr.toDoubleOrNull()
        val fMm = focalLengthText.toDoubleOrNull()
        if (ifovVal != null && ifovVal > 0 && fMm != null && fMm > 0 && pixelPitchText.isNotEmpty()) {
            val ifovRad = convertIfovToRadians(ifovVal, ifovUnit)
            if (ifovRad != null) {
                val calcPitchUm = calculatePixelPitchUm(ifovRad, fMm)
                if (calcPitchUm != null) {
                    val formattedPitch = formatNumbers(calcPitchUm)
                    pixelPitchText = formattedPitch
                    settingsManager.putString("key_ifov_pixel_pitch", formattedPitch)
                }
            }
        }
    }

    val onIfovUnitChange: (IfovUnit) -> Unit = { newUnit ->
        val oldUnit = ifovUnit
        ifovUnit = newUnit
        settingsManager.putString("key_ifov_unit", newUnit.name)

        val fMm = focalLengthText.toDoubleOrNull()
        val pUm = pixelPitchText.toDoubleOrNull()
        if (fMm != null && fMm > 0 && pUm != null && pUm > 0) {
            val rad = calculateIfovInRadians(fMm, pUm)
            val valInUnit = rad?.let { convertIfovFromRadians(it, newUnit) }
            if (valInUnit != null) {
                val formatted = formatNumbers(valInUnit)
                ifovText = formatted
                settingsManager.putString("key_ifov_text", formatted)
            }
        } else {
            val currentVal = ifovText.toDoubleOrNull()
            if (currentVal != null) {
                val rad = convertIfovToRadians(currentVal, oldUnit)
                val valInNewUnit = rad?.let { convertIfovFromRadians(it, newUnit) }
                if (valInNewUnit != null) {
                    val formatted = formatNumbers(valInNewUnit)
                    ifovText = formatted
                    settingsManager.putString("key_ifov_text", formatted)
                }
            }
        }
    }

    val focalLengthMm = focalLengthText.toDoubleOrNull()
    val pixelPitchUm = pixelPitchText.toDoubleOrNull()
    val userIfovVal = ifovText.toDoubleOrNull()
    val distanceMeters = distanceText.toDoubleOrNull()
    val pixelCountVal = pixelCountText.toDoubleOrNull()
    val objectSizeVal = objectSizeText.toDoubleOrNull()
    val objectPixelsVal = objectPixelsText.toDoubleOrNull()

    val effectiveIfovRad: Double? = if (focalLengthMm != null && focalLengthMm > 0 && pixelPitchUm != null && pixelPitchUm > 0) {
        calculateIfovInRadians(focalLengthMm, pixelPitchUm)
    } else if (userIfovVal != null && userIfovVal > 0) {
        convertIfovToRadians(userIfovVal, ifovUnit)
    } else {
        null
    }

    // Mode 0 calculations: GSD & Total FOV
    val gsdMeters: Double? = if (effectiveIfovRad != null && distanceMeters != null && distanceMeters >= 0) {
        calculateGsdInMeters(effectiveIfovRad, distanceMeters)
    } else {
        null
    }
    val footprintAreaSqM: Double? = gsdMeters?.let { calculatePixelFootprintArea(it) }
    val totalFovRad: Double? = if (focalLengthMm != null && focalLengthMm > 0 && pixelPitchUm != null && pixelPitchUm > 0 && pixelCountVal != null && pixelCountVal > 0) {
        calculateTotalFovRadians(focalLengthMm, pixelPitchUm, pixelCountVal)
    } else null

    // Mode 1 calculations: Range to Object
    val objectRangeMeters: Double? = if (objectSizeVal != null && objectSizeVal > 0 && objectPixelsVal != null && objectPixelsVal > 0) {
        if (effectiveIfovRad != null && effectiveIfovRad > 0) {
            calculateObjectRangeMeters(objectSizeVal, objectPixelsVal, effectiveIfovRad)
        } else if (focalLengthMm != null && focalLengthMm > 0 && pixelPitchUm != null && pixelPitchUm > 0) {
            calculateObjectRangeFromFocalPitch(objectSizeVal, focalLengthMm, pixelPitchUm, objectPixelsVal)
        } else {
            null
        }
    } else {
        null
    }

    val gsdOutputStr = if (gsdMeters != null) {
        formatGsd(gsdMeters)
    } else if (effectiveIfovRad != null) {
        "Enter Range / Distance"
    } else {
        "Invalid Input"
    }

    val ifovOutputStr = if (effectiveIfovRad != null) {
        formatIfov(effectiveIfovRad, ifovUnit)
    } else {
        "Invalid Input"
    }

    val areaOutputStr = if (footprintAreaSqM != null) {
        formatFootprintArea(footprintAreaSqM)
    } else {
        "N/A"
    }

    val totalFovOutputStr = if (totalFovRad != null) {
        formatTotalFov(totalFovRad)
    } else {
        "N/A"
    }

    val rangeOutputStr = if (objectRangeMeters != null) {
        formatRange(objectRangeMeters)
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
        // Mode Selector Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val modes = listOf(
                0 to "IFOV, GSD &\nTotal FOV",
                1 to "Range to\nObject"
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
                            settingsManager.putInt("key_ifov_calc_mode", idx)
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

        // Output Summary Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (modeIndex == 0) {
                    Text(
                        text = "Ground Sampling Distance (GSD)",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                    Text(
                        text = gsdOutputStr,
                        fontSize = 20.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "IFOV",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.7f),
                            )
                            Text(
                                text = ifovOutputStr,
                                fontSize = 14.sp,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Total FOV",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.7f),
                            )
                            Text(
                                text = totalFovOutputStr,
                                fontSize = 14.sp,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Footprint Area",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.7f),
                            )
                            Text(
                                text = areaOutputStr,
                                fontSize = 14.sp,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Calculated Range to Object",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                    Text(
                        text = rangeOutputStr,
                        fontSize = 20.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Sensor IFOV",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.7f),
                            )
                            Text(
                                text = ifovOutputStr,
                                fontSize = 14.sp,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Object Size",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.7f),
                            )
                            Text(
                                text = objectSizeVal?.let { "${formatNumbers(it)} m" } ?: "N/A",
                                fontSize = 14.sp,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }

        // Common Inputs: Focal Length & Pixel Pitch & IFOV
        OutlinedTextField(
            value = focalLengthText,
            onValueChange = onFocalLengthChange,
            label = { Text("Optical Focal Length (mm)", color = Color.White.copy(alpha = 0.8f)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = textFieldColors,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = pixelPitchText,
            onValueChange = onPixelPitchChange,
            label = { Text("Pixel Pitch (µm)", color = Color.White.copy(alpha = 0.8f)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = textFieldColors,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = ifovText,
                onValueChange = onIfovChange,
                label = { Text("IFOV", color = Color.White.copy(alpha = 0.8f)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = textFieldColors,
                modifier = Modifier.weight(1f),
            )

            Box {
                OutlinedButton(
                    onClick = { isIfovUnitDropdownExpanded = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .height(56.dp)
                        .padding(top = 8.dp),
                ) {
                    Text(ifovUnit.symbol, fontWeight = FontWeight.Bold)
                }
                DropdownMenu(
                    expanded = isIfovUnitDropdownExpanded,
                    onDismissRequest = { isIfovUnitDropdownExpanded = false },
                ) {
                    IfovUnit.entries.forEach { unit ->
                        DropdownMenuItem(
                            text = { Text("${unit.label} (${unit.symbol})") },
                            onClick = {
                                onIfovUnitChange(unit)
                                isIfovUnitDropdownExpanded = false
                            },
                        )
                    }
                }
            }
        }

        // Mode-specific inputs
        if (modeIndex == 0) {
            OutlinedTextField(
                value = distanceText,
                onValueChange = { newDistStr ->
                    distanceText = newDistStr
                    settingsManager.putString("key_ifov_distance", newDistStr)
                },
                label = { Text("Distance / Range (m)", color = Color.White.copy(alpha = 0.8f)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = textFieldColors,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = pixelCountText,
                onValueChange = { newCountStr ->
                    pixelCountText = newCountStr
                    settingsManager.putString("key_ifov_pixel_count", newCountStr)
                },
                label = { Text("Sensor Pixel Count (e.g. 1920)", color = Color.White.copy(alpha = 0.8f)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = textFieldColors,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            OutlinedTextField(
                value = objectSizeText,
                onValueChange = { newSizeStr ->
                    objectSizeText = newSizeStr
                    settingsManager.putString("key_ifov_object_size", newSizeStr)
                },
                label = { Text("Object Physical Size (m)", color = Color.White.copy(alpha = 0.8f)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = textFieldColors,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = objectPixelsText,
                onValueChange = { newPixelsStr ->
                    objectPixelsText = newPixelsStr
                    settingsManager.putString("key_ifov_object_pixels", newPixelsStr)
                },
                label = { Text("Object Pixel Span (pixel count)", color = Color.White.copy(alpha = 0.8f)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = textFieldColors,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
