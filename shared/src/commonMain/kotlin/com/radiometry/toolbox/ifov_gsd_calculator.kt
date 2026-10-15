package com.radiometry.toolbox

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun IfovGsdVisualization(settingsManager: SettingsManager) {
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

    val effectiveIfovRad: Double? = if (focalLengthMm != null && focalLengthMm > 0 && pixelPitchUm != null && pixelPitchUm > 0) {
        calculateIfovInRadians(focalLengthMm, pixelPitchUm)
    } else if (userIfovVal != null && userIfovVal > 0) {
        convertIfovToRadians(userIfovVal, ifovUnit)
    } else {
        null
    }

    val gsdMeters: Double? = if (effectiveIfovRad != null && distanceMeters != null && distanceMeters >= 0) {
        calculateGsdInMeters(effectiveIfovRad, distanceMeters)
    } else {
        null
    }

    val footprintAreaSqM: Double? = gsdMeters?.let { calculatePixelFootprintArea(it) }

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
                            text = "Pixel Footprint Area",
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
            }
        }

        // Ray Diagram Canvas
        OpticsGeometryDiagram(
            focalLengthMm = focalLengthMm,
            pixelPitchUm = pixelPitchUm,
            ifovRad = effectiveIfovRad,
            distanceMeters = distanceMeters,
            gsdMeters = gsdMeters,
            ifovUnit = ifovUnit,
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                .padding(8.dp),
        )

        // Inputs
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
    }
}

@Composable
fun OpticsGeometryDiagram(
    focalLengthMm: Double?,
    pixelPitchUm: Double?,
    ifovRad: Double?,
    distanceMeters: Double?,
    gsdMeters: Double?,
    ifovUnit: IfovUnit,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f

        // X-coordinates for optical elements
        val detectorX = width * 0.08f
        val lensX = width * 0.28f
        val targetX = width * 0.90f

        // Optical axis line
        drawLine(
            color = Color.White.copy(alpha = 0.3f),
            start = Offset(10f, centerY),
            end = Offset(width - 10f, centerY),
            strokeWidth = 1.5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f),
        )

        // Draw Lens (convex arc line)
        val lensHeight = height * 0.6f
        val lensTop = centerY - lensHeight / 2f
        val lensBottom = centerY + lensHeight / 2f

        val lensPath = Path().apply {
            moveTo(lensX, lensTop)
            quadraticTo(lensX + 8f, centerY, lensX, lensBottom)
            quadraticTo(lensX - 8f, centerY, lensX, lensTop)
        }
        drawPath(
            path = lensPath,
            color = Color(0xFF64B5F6),
            style = Stroke(width = 2.5f),
        )

        // Draw Detector Pixel at detectorX
        val pixelDisplayHeight = height * 0.25f
        val pixelTop = centerY - pixelDisplayHeight / 2f
        val pixelBottom = centerY + pixelDisplayHeight / 2f

        drawLine(
            color = Color(0xFFFFD54F),
            start = Offset(detectorX, pixelTop),
            end = Offset(detectorX, pixelBottom),
            strokeWidth = 4f,
            cap = StrokeCap.Round,
        )

        // Draw Target Plane / Footprint GSD at targetX
        val gsdDisplayHeight = height * 0.65f
        val targetTop = centerY - gsdDisplayHeight / 2f
        val targetBottom = centerY + gsdDisplayHeight / 2f

        drawLine(
            color = Color(0xFF81C784),
            start = Offset(targetX, targetTop),
            end = Offset(targetX, targetBottom),
            strokeWidth = 4f,
            cap = StrokeCap.Round,
        )

        // Cone / Ray lines: Pixel -> Lens center -> Target GSD
        val topRay = Path().apply {
            moveTo(detectorX, pixelTop)
            lineTo(lensX, centerY)
            lineTo(targetX, targetBottom)
        }
        val bottomRay = Path().apply {
            moveTo(detectorX, pixelBottom)
            lineTo(lensX, centerY)
            lineTo(targetX, targetTop)
        }

        drawPath(
            path = topRay,
            color = Color(0xFFFFB74D).copy(alpha = 0.7f),
            style = Stroke(width = 1.5f),
        )
        drawPath(
            path = bottomRay,
            color = Color(0xFFFFB74D).copy(alpha = 0.7f),
            style = Stroke(width = 1.5f),
        )

        // Text Labels on Canvas
        val smallTextStyle = TextStyle(
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
        )

        // Label for focal length f & pitch p
        val focalText = if (focalLengthMm != null) "f=${formatNumbers(focalLengthMm)}mm" else "f"
        val pitchText = if (pixelPitchUm != null) "p=${formatNumbers(pixelPitchUm)}µm" else ""
        val specText = if (pitchText.isNotEmpty()) "$focalText, $pitchText" else focalText
        drawText(
            textMeasurer = textMeasurer,
            text = specText,
            style = smallTextStyle,
            topLeft = Offset(detectorX, pixelTop - 14f),
        )

        // Label for Lens
        drawText(
            textMeasurer = textMeasurer,
            text = "Lens",
            style = smallTextStyle,
            topLeft = Offset(lensX - 10f, lensTop - 14f),
        )

        // Label for Range / Distance D
        drawText(
            textMeasurer = textMeasurer,
            text = if (distanceMeters != null) "D = ${formatNumbers(distanceMeters)}m" else "D",
            style = smallTextStyle,
            topLeft = Offset((lensX + targetX) / 2f - 20f, centerY + 6f),
        )

        // Label for IFOV angle
        if (ifovRad != null) {
            drawText(
                textMeasurer = textMeasurer,
                text = "IFOV: ${formatIfov(ifovRad, ifovUnit)}",
                style = TextStyle(
                    color = Color(0xFFFFB74D),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                ),
                topLeft = Offset(lensX + 10f, centerY - 20f),
            )
        }

        // Label for GSD on Target
        val gsdText = if (gsdMeters != null) "GSD: ${formatGsd(gsdMeters)}" else "GSD"
        drawText(
            textMeasurer = textMeasurer,
            text = gsdText,
            style = TextStyle(
                color = Color(0xFF81C784),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            ),
            topLeft = Offset(targetX - 70f, targetBottom + 2f),
        )
    }
}
