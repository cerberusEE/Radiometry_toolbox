package com.radiometry.toolbox

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.pow

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
                onUnitSelected = { newUnit ->
                    if (newUnit != wavelengthUnit) {
                        val oldUnit = wavelengthUnit

                        val currentStartVal = startWavelengthText.toDoubleOrNull()
                        if (currentStartVal != null) {
                            val convertedStart = convertWavelength(currentStartVal, oldUnit, newUnit)
                            if (convertedStart != null) {
                                val formatted = formatNumbers(convertedStart)
                                startWavelengthText = formatted
                                settingsManager.putString("key_radiance_start_wavelength", formatted)
                            }
                        }

                        val currentStopVal = stopWavelengthText.toDoubleOrNull()
                        if (currentStopVal != null) {
                            val convertedStop = convertWavelength(currentStopVal, oldUnit, newUnit)
                            if (convertedStop != null) {
                                val formatted = formatNumbers(convertedStop)
                                stopWavelengthText = formatted
                                settingsManager.putString("key_radiance_stop_wavelength", formatted)
                            }
                        }

                        wavelengthUnit = newUnit
                        settingsManager.putString("key_radiance_wavelength_unit", newUnit.name)
                    }
                },
                modifier = Modifier.weight(1f),
            )
            UnitDropdown(
                label = "Temperature Unit",
                selectedUnit = temperatureUnit,
                onUnitSelected = { newUnit ->
                    if (newUnit != temperatureUnit) {
                        val currentTemp = temperatureText.toDoubleOrNull()
                        if (currentTemp != null) {
                            val converted = convertTemperature(currentTemp, temperatureUnit, newUnit)
                            if (!converted.isNaN()) {
                                onTemperatureTextChange(formatNumbers(converted))
                            }
                        }
                        onTemperatureUnitChange(newUnit)
                    }
                },
                modifier = Modifier.weight(1f),
            )
        }

        // Planck curve figure
        PlanckCurvePlot(
            startVal = startVal,
            stopVal = stopVal,
            tempVal = tempVal,
            wavelengthUnit = wavelengthUnit,
            temperatureUnit = temperatureUnit,
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
        )
    }
}


private fun formatTickValue(value: Double): String {
    if (value == 0.0) return "0"
    val absVal = kotlin.math.abs(value)
    return if (absVal !in 0.01..<10000.0) {
        val exp = kotlin.math.floor(kotlin.math.log10(absVal)).toInt()
        val mantissa = absVal / 10.0.pow(exp.toDouble())
        val rounded = kotlin.math.round(mantissa * 100.0) / 100.0
        "${rounded}e$exp"
    } else {
        val rounded = kotlin.math.round(value * 100.0) / 100.0
        if (kotlin.math.abs(rounded - kotlin.math.round(rounded)) < 1e-6) {
            rounded.toLong().toString()
        } else {
            rounded.toString()
        }
    }
}

@Composable
fun PlanckCurvePlot(
    startVal: Double?,
    stopVal: Double?,
    tempVal: Double?,
    wavelengthUnit: WavelengthUnit,
    temperatureUnit: TemperatureUnit,
    modifier: Modifier = Modifier,
) {
    val tempKelvin = tempVal?.let { convertTemperatureToKelvin(it, temperatureUnit) }

    val isValid = startVal != null && startVal > 0.0 &&
            stopVal != null && stopVal > 0.0 &&
            tempKelvin != null && tempKelvin > 0.0

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (!isValid) {
            Text(
                text = "Enter valid positive inputs to view Planck curve",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f),
            )
            return@Box
        }

        val vMin = minOf(startVal, stopVal)
        val vMax = maxOf(startVal, stopVal)
        val xMin = 0.9 * vMin
        val xMax = 1.1 * vMax

        if (xMin >= xMax || xMin <= 0.0) {
            Text(
                text = "Invalid wavelength range for Planck curve",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f),
            )
            return@Box
        }

        val numPoints = 200
        val points = List(numPoints) { i ->
            val x = xMin + i * (xMax - xMin) / (numPoints - 1)
            val y = calculateSpectralRadianceForUnit(x, wavelengthUnit, tempKelvin)
            Pair(x, y)
        }

        val yMax = points.maxOfOrNull { it.second }?.takeIf { it > 0.0 } ?: 1.0
        val yMin = 0.0

        val xLabel = "[" + wavelengthUnit.symbol + "]"
        val yLabel = "Spectral Radiance (W/(sr·m²·" + wavelengthUnit.symbol + "))"

        val textMeasurer = rememberTextMeasurer()

        Canvas(modifier = Modifier.fillMaxSize()) {
            val paddingLeft = 55.dp.toPx()
            val paddingRight = 16.dp.toPx()
            val paddingTop = 24.dp.toPx()
            val paddingBottom = 36.dp.toPx()

            val plotWidth = size.width - paddingLeft - paddingRight
            val plotHeight = size.height - paddingTop - paddingBottom

            if (plotWidth <= 0 || plotHeight <= 0) return@Canvas

            fun toPxX(x: Double): Float {
                return (paddingLeft + ((x - xMin) / (xMax - xMin)) * plotWidth).toFloat()
            }

            fun toPxY(y: Double): Float {
                return (paddingTop + plotHeight - ((y - yMin) / (yMax - yMin)) * plotHeight).toFloat()
            }

            // Draw Y-axis title above chart
            val yTitleResult = textMeasurer.measure(
                text = yLabel,
                style = TextStyle(color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            )
            drawText(yTitleResult, topLeft = Offset(paddingLeft, 2.dp.toPx()))

            // Horizontal Grid Lines & Y Tick Labels (4 steps)
            val ySteps = 4
            for (i in 0..ySteps) {
                val yValue = yMin + i * (yMax - yMin) / ySteps
                val py = toPxY(yValue)

                drawLine(
                    color = Color.White.copy(alpha = 0.12f),
                    start = Offset(paddingLeft, py),
                    end = Offset(paddingLeft + plotWidth, py),
                    strokeWidth = 1.dp.toPx()
                )

                val tickStr = formatTickValue(yValue)
                val tickResult = textMeasurer.measure(
                    text = tickStr,
                    style = TextStyle(color = Color.White.copy(alpha = 0.7f), fontSize = 9.sp)
                )
                val textX = paddingLeft - tickResult.size.width - 4.dp.toPx()
                val textY = py - tickResult.size.height / 2f
                if (textX >= 0) {
                    drawText(tickResult, topLeft = Offset(textX, textY))
                }
            }

            // Vertical Grid Lines for vMin and vMax
            listOf(vMin, vMax).forEach { xVal ->
                val px = toPxX(xVal)
                drawLine(
                    color = Color(0xFFFFB74D).copy(alpha = 0.4f),
                    start = Offset(px, paddingTop),
                    end = Offset(px, paddingTop + plotHeight),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Shaded Area under the curve between vMin and vMax
            val subSteps = 100
            val bandPath = Path().apply {
                moveTo(toPxX(vMin), toPxY(0.0))
                for (j in 0..subSteps) {
                    val x = vMin + j * (vMax - vMin) / subSteps
                    val y = calculateSpectralRadianceForUnit(x, wavelengthUnit, tempKelvin)
                    lineTo(toPxX(x), toPxY(y))
                }
                lineTo(toPxX(vMax), toPxY(0.0))
                close()
            }
            drawPath(
                path = bandPath,
                color = Color(0xFFFFB74D).copy(alpha = 0.25f)
            )

            // Planck Curve
            val curvePath = Path().apply {
                val first = points.first()
                moveTo(toPxX(first.first), toPxY(first.second))
                for (k in 1 until points.size) {
                    val p = points[k]
                    lineTo(toPxX(p.first), toPxY(p.second))
                }
            }
            drawPath(
                path = curvePath,
                color = Color(0xFF64B5F6),
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // Axes
            drawLine(
                color = Color.White.copy(alpha = 0.7f),
                start = Offset(paddingLeft, paddingTop + plotHeight),
                end = Offset(paddingLeft + plotWidth, paddingTop + plotHeight),
                strokeWidth = 1.5.dp.toPx()
            )
            drawLine(
                color = Color.White.copy(alpha = 0.7f),
                start = Offset(paddingLeft, paddingTop),
                end = Offset(paddingLeft, paddingTop + plotHeight),
                strokeWidth = 1.5.dp.toPx()
            )

            // X-Axis Tick Labels at xMin, vMin, vMax, xMax
            val xTicks = listOf(xMin, vMin, vMax, xMax).distinct()
            xTicks.forEach { xVal ->
                val px = toPxX(xVal)
                val tickStr = formatTickValue(xVal)
                val tickResult = textMeasurer.measure(
                    text = tickStr,
                    style = TextStyle(
                        color = if (xVal == vMin || xVal == vMax) Color(0xFFFFB74D) else Color.White.copy(alpha = 0.7f),
                        fontSize = 9.sp,
                        fontWeight = if (xVal == vMin || xVal == vMax) FontWeight.Bold else FontWeight.Normal
                    )
                )
                val textX = px - tickResult.size.width / 2f
                val textY = paddingTop + plotHeight + 4.dp.toPx()
                drawText(tickResult, topLeft = Offset(textX, textY))
            }

            // X-Axis Title
            val xTitleResult = textMeasurer.measure(
                text = xLabel,
                style = TextStyle(color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            )
            val xTitleX = paddingLeft + (plotWidth - xTitleResult.size.width) / 2f
            val xTitleY = paddingTop + plotHeight + 18.dp.toPx()
            drawText(xTitleResult, topLeft = Offset(xTitleX, xTitleY))
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