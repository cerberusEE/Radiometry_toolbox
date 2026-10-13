package com.radiometry.toolbox

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.pow

@Composable
fun TemperatureComparison(
    settingsManager: SettingsManager,
    temperatureText: String,
    onTemperatureTextChange: (String) -> Unit,
    temperatureUnit: TemperatureUnit,
    onTemperatureUnitChange: (TemperatureUnit) -> Unit,
) {
    // Initial wavelength limits setup sharing state with Radiance Calculator
    val initialStartUm = remember {
        val savedTempCompStart = settingsManager.getString("key_temp_comp_start_wavelength", "")
        savedTempCompStart.ifEmpty {
            val radStart = settingsManager.getString("key_radiance_start_wavelength", "380")
            val radUnitName = settingsManager.getString("key_radiance_wavelength_unit", WavelengthUnit.NANOMETERS.name)
            val radUnit = runCatching { WavelengthUnit.valueOf(radUnitName) }.getOrDefault(WavelengthUnit.NANOMETERS)
            val valDouble = radStart.toDoubleOrNull()
            if (valDouble != null) {
                val converted = convertWavelength(valDouble, radUnit, WavelengthUnit.MICROMETERS)
                converted?.let { formatNumbers(it) } ?: "0.38"
            } else {
                "0.38"
            }
        }
    }

    val initialStopUm = remember {
        val savedTempCompStop = settingsManager.getString("key_temp_comp_stop_wavelength", "")
        savedTempCompStop.ifEmpty {
            val radStop = settingsManager.getString("key_radiance_stop_wavelength", "780")
            val radUnitName = settingsManager.getString("key_radiance_wavelength_unit", WavelengthUnit.NANOMETERS.name)
            val radUnit = runCatching { WavelengthUnit.valueOf(radUnitName) }.getOrDefault(WavelengthUnit.NANOMETERS)
            val valDouble = radStop.toDoubleOrNull()
            if (valDouble != null) {
                val converted = convertWavelength(valDouble, radUnit, WavelengthUnit.MICROMETERS)
                converted?.let { formatNumbers(it) } ?: "0.78"
            } else {
                "0.78"
            }
        }
    }

    var startWavelengthText by remember { mutableStateOf(initialStartUm) }
    var stopWavelengthText by remember { mutableStateOf(initialStopUm) }

    var temperature2Text by remember {
        mutableStateOf(settingsManager.getString("key_temp_comp_temperature_2", "300"))
    }
    var tempBgText by remember {
        mutableStateOf(settingsManager.getString("key_temp_comp_temperature_bg", "300"))
    }
    var emissivity1Text by remember {
        mutableStateOf(settingsManager.getString("key_temp_comp_emissivity_1", "1.0"))
    }
    var emissivity2Text by remember {
        mutableStateOf(settingsManager.getString("key_temp_comp_emissivity_2", "1.0"))
    }

    // Function to update start wavelength in both local state and shared Radiance Calculator state
    val onStartWavelengthChange: (String) -> Unit = { newStartStr ->
        startWavelengthText = newStartStr
        settingsManager.putString("key_temp_comp_start_wavelength", newStartStr)

        val newStartVal = newStartStr.toDoubleOrNull()
        if (newStartVal != null) {
            val radUnitName = settingsManager.getString("key_radiance_wavelength_unit", WavelengthUnit.NANOMETERS.name)
            val radUnit = runCatching { WavelengthUnit.valueOf(radUnitName) }.getOrDefault(WavelengthUnit.NANOMETERS)
            val converted = convertWavelength(newStartVal, WavelengthUnit.MICROMETERS, radUnit)
            if (converted != null) {
                settingsManager.putString("key_radiance_start_wavelength", formatNumbers(converted))
            }
        }
    }

    // Function to update stop wavelength in both local state and shared Radiance Calculator state
    val onStopWavelengthChange: (String) -> Unit = { newStopStr ->
        stopWavelengthText = newStopStr
        settingsManager.putString("key_temp_comp_stop_wavelength", newStopStr)

        val newStopVal = newStopStr.toDoubleOrNull()
        if (newStopVal != null) {
            val radUnitName = settingsManager.getString("key_radiance_wavelength_unit", WavelengthUnit.NANOMETERS.name)
            val radUnit = runCatching { WavelengthUnit.valueOf(radUnitName) }.getOrDefault(WavelengthUnit.NANOMETERS)
            val converted = convertWavelength(newStopVal, WavelengthUnit.MICROMETERS, radUnit)
            if (converted != null) {
                settingsManager.putString("key_radiance_stop_wavelength", formatNumbers(converted))
            }
        }
    }

    val startVal = startWavelengthText.toDoubleOrNull()
    val stopVal = stopWavelengthText.toDoubleOrNull()
    val temp1Val = temperatureText.toDoubleOrNull()
    val temp2Val = temperature2Text.toDoubleOrNull()
    val tempBgVal = tempBgText.toDoubleOrNull()
    val e1Val = emissivity1Text.toDoubleOrNull() ?: 1.0
    val e2Val = emissivity2Text.toDoubleOrNull() ?: 1.0

    val startMeters = startVal?.let { convertToWavelengthInMeters(it, WavelengthUnit.MICROMETERS) }
    val stopMeters = stopVal?.let { convertToWavelengthInMeters(it, WavelengthUnit.MICROMETERS) }
    val temp1Kelvin = temp1Val?.let { convertTemperatureToKelvin(it, temperatureUnit) }
    val temp2Kelvin = temp2Val?.let { convertTemperatureToKelvin(it, temperatureUnit) }
    val tempBgKelvin = tempBgVal?.let { convertTemperatureToKelvin(it, temperatureUnit) }

    val comparisonResult = if (startMeters != null && stopMeters != null && temp1Kelvin != null && temp2Kelvin != null && tempBgKelvin != null) {
        calculateRadianceComparison(
            startWavelengthMeters = startMeters,
            stopWavelengthMeters = stopMeters,
            temp1Kelvin = temp1Kelvin,
            emissivity1 = e1Val,
            temp2Kelvin = temp2Kelvin,
            emissivity2 = e2Val,
            tempBgKelvin = tempBgKelvin,
        )
    } else {
        null
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
        // Output result card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Radiance Contrast",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (comparisonResult != null) {
                        "Contrast: ${formatRadiance(comparisonResult.contrast)}"
                    } else {
                        "Contrast: Invalid Input"
                    },
                    fontSize = 20.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
                if (comparisonResult != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (e1Val < 1.0) {
                            "L₁ = ${formatRadiance(comparisonResult.radiance1)} (Emitted: ${formatNumbers(comparisonResult.emitted1)}, Reflected: ${formatNumbers(comparisonResult.reflected1)})"
                        } else {
                            "L₁ = ${formatRadiance(comparisonResult.radiance1)} (ε₁ = $e1Val)"
                        },
                        fontSize = 12.sp,
                        color = Color(0xFF64B5F6),
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = if (e2Val < 1.0) {
                            "L₂ = ${formatRadiance(comparisonResult.radiance2)} (Emitted: ${formatNumbers(comparisonResult.emitted2)}, Reflected: ${formatNumbers(comparisonResult.reflected2)})"
                        } else {
                            "L₂ = ${formatRadiance(comparisonResult.radiance2)} (ε₂ = $e2Val)"
                        },
                        fontSize = 12.sp,
                        color = Color(0xFFFFB74D),
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        // Spectral band wavelength inputs in micrometers (µm)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = startWavelengthText,
                onValueChange = onStartWavelengthChange,
                label = {
                    Text(
                        "Start Wavelength (µm)",
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = textFieldColors,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = stopWavelengthText,
                onValueChange = onStopWavelengthChange,
                label = {
                    Text(
                        "Stop Wavelength (µm)",
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = textFieldColors,
                modifier = Modifier.weight(1f),
            )
        }

        // Temperatures row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = temperatureText,
                onValueChange = { onTemperatureTextChange(it) },
                label = {
                    Text(
                        "Temperature 1",
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = textFieldColors,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = temperature2Text,
                onValueChange = {
                    temperature2Text = it
                    settingsManager.putString("key_temp_comp_temperature_2", it)
                },
                label = {
                    Text(
                        "Temperature 2",
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = textFieldColors,
                modifier = Modifier.weight(1f),
            )
        }

        // Background Temperature Text Field
        OutlinedTextField(
            value = tempBgText,
            onValueChange = {
                tempBgText = it
                settingsManager.putString("key_temp_comp_temperature_bg", it)
            },
            label = {
                Text(
                    "Background Temperature (${temperatureUnit.symbol})",
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = textFieldColors,
            modifier = Modifier.fillMaxWidth(),
        )

        // Emissivities row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = emissivity1Text,
                onValueChange = {
                    emissivity1Text = it
                    settingsManager.putString("key_temp_comp_emissivity_1", it)
                },
                label = {
                    Text(
                        "Emissivity 1",
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = textFieldColors,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = emissivity2Text,
                onValueChange = {
                    emissivity2Text = it
                    settingsManager.putString("key_temp_comp_emissivity_2", it)
                },
                label = {
                    Text(
                        "Emissivity 2",
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = textFieldColors,
                modifier = Modifier.weight(1f),
            )
        }

        // Temperature unit selector dropdown
        UnitDropdown(
            label = "Temperature Unit",
            selectedUnit = temperatureUnit,
            onUnitSelected = { newUnit ->
                if (newUnit != temperatureUnit) {
                    val currentTemp1 = temperatureText.toDoubleOrNull()
                    if (currentTemp1 != null) {
                        val converted = convertTemperature(currentTemp1, temperatureUnit, newUnit)
                        if (!converted.isNaN()) {
                            onTemperatureTextChange(formatNumbers(converted))
                        }
                    }
                    val currentTemp2 = temperature2Text.toDoubleOrNull()
                    if (currentTemp2 != null) {
                        val converted2 = convertTemperature(currentTemp2, temperatureUnit, newUnit)
                        if (!converted2.isNaN()) {
                            val formatted2 = formatNumbers(converted2)
                            temperature2Text = formatted2
                            settingsManager.putString("key_temp_comp_temperature_2", formatted2)
                        }
                    }
                    val currentTempBg = tempBgText.toDoubleOrNull()
                    if (currentTempBg != null) {
                        val convertedBg = convertTemperature(currentTempBg, temperatureUnit, newUnit)
                        if (!convertedBg.isNaN()) {
                            val formattedBg = formatNumbers(convertedBg)
                            tempBgText = formattedBg
                            settingsManager.putString("key_temp_comp_temperature_bg", formattedBg)
                        }
                    }
                    onTemperatureUnitChange(newUnit)
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        // Comparison Planck curve plot
        ComparisonPlanckPlot(
            startValUm = startVal,
            stopValUm = stopVal,
            temp1Kelvin = temp1Kelvin,
            emissivity1 = e1Val,
            temp2Kelvin = temp2Kelvin,
            emissivity2 = e2Val,
            tempBgKelvin = tempBgKelvin,
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
        )
    }
}

private fun formatTickVal(value: Double): String {
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
fun ComparisonPlanckPlot(
    startValUm: Double?,
    stopValUm: Double?,
    temp1Kelvin: Double?,
    emissivity1: Double,
    temp2Kelvin: Double?,
    emissivity2: Double,
    tempBgKelvin: Double?,
    modifier: Modifier = Modifier,
) {
    val isValid = startValUm != null && startValUm > 0.0 &&
            stopValUm != null && stopValUm > 0.0 &&
            temp1Kelvin != null && temp1Kelvin > 0.0 &&
            temp2Kelvin != null && temp2Kelvin > 0.0 &&
            tempBgKelvin != null && tempBgKelvin > 0.0 &&
            emissivity1 >= 0.0 && emissivity2 >= 0.0

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (!isValid) {
            Text(
                text = "Enter valid inputs to view spectral comparison plot",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f),
            )
            return@Box
        }

        val vMin = minOf(startValUm, stopValUm)
        val vMax = maxOf(startValUm, stopValUm)
        val xMin = 0.9 * vMin
        val xMax = 1.1 * vMax

        if (xMin >= xMax || xMin <= 0.0) {
            Text(
                text = "Invalid wavelength range for comparison plot",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f),
            )
            return@Box
        }

        val numPoints = 200
        val points1 = List(numPoints) { i ->
            val x = xMin + i * (xMax - xMin) / (numPoints - 1)
            val emitted = emissivity1 * calculateSpectralRadianceForUnit(x, WavelengthUnit.MICROMETERS, temp1Kelvin)
            val reflected = (1.0 - emissivity1).coerceAtLeast(0.0) * calculateSpectralRadianceForUnit(x, WavelengthUnit.MICROMETERS, tempBgKelvin)
            Pair(x, emitted + reflected)
        }
        val points2 = List(numPoints) { i ->
            val x = xMin + i * (xMax - xMin) / (numPoints - 1)
            val emitted = emissivity2 * calculateSpectralRadianceForUnit(x, WavelengthUnit.MICROMETERS, temp2Kelvin)
            val reflected = (1.0 - emissivity2).coerceAtLeast(0.0) * calculateSpectralRadianceForUnit(x, WavelengthUnit.MICROMETERS, tempBgKelvin)
            Pair(x, emitted + reflected)
        }

        val maxY1 = points1.maxOfOrNull { it.second } ?: 0.0
        val maxY2 = points2.maxOfOrNull { it.second } ?: 0.0
        val yMax = maxOf(maxY1, maxY2).takeIf { it > 0.0 } ?: 1.0
        val yMin = 0.0

        val xLabel = "[µm]"
        val yLabel = "Spectral Radiance (W/(sr·m²·µm))"

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

            // Horizontal Grid Lines & Y Tick Labels
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

                val tickStr = formatTickVal(yValue)
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
                    color = Color.White.copy(alpha = 0.3f),
                    start = Offset(px, paddingTop),
                    end = Offset(px, paddingTop + plotHeight),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Shaded region between Curve 1 and Curve 2 in spectral band
            val subSteps = 100
            val bandPath = Path().apply {
                val startEmitted1 = emissivity1 * calculateSpectralRadianceForUnit(vMin, WavelengthUnit.MICROMETERS, temp1Kelvin)
                val startReflected1 = (1.0 - emissivity1).coerceAtLeast(0.0) * calculateSpectralRadianceForUnit(vMin, WavelengthUnit.MICROMETERS, tempBgKelvin)
                moveTo(toPxX(vMin), toPxY(startEmitted1 + startReflected1))

                for (j in 1..subSteps) {
                    val x = vMin + j * (vMax - vMin) / subSteps
                    val e = emissivity1 * calculateSpectralRadianceForUnit(x, WavelengthUnit.MICROMETERS, temp1Kelvin)
                    val r = (1.0 - emissivity1).coerceAtLeast(0.0) * calculateSpectralRadianceForUnit(x, WavelengthUnit.MICROMETERS, tempBgKelvin)
                    lineTo(toPxX(x), toPxY(e + r))
                }

                for (j in subSteps downTo 0) {
                    val x = vMin + j * (vMax - vMin) / subSteps
                    val e = emissivity2 * calculateSpectralRadianceForUnit(x, WavelengthUnit.MICROMETERS, temp2Kelvin)
                    val r = (1.0 - emissivity2).coerceAtLeast(0.0) * calculateSpectralRadianceForUnit(x, WavelengthUnit.MICROMETERS, tempBgKelvin)
                    lineTo(toPxX(x), toPxY(e + r))
                }
                close()
            }
            drawPath(
                path = bandPath,
                color = Color(0xFFE91E63).copy(alpha = 0.3f)
            )

            // Curve 1 (Greybody 1 total radiance)
            val curvePath1 = Path().apply {
                val first = points1.first()
                moveTo(toPxX(first.first), toPxY(first.second))
                for (k in 1 until points1.size) {
                    val p = points1[k]
                    lineTo(toPxX(p.first), toPxY(p.second))
                }
            }
            drawPath(
                path = curvePath1,
                color = Color(0xFF64B5F6),
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // Curve 2 (Greybody 2 total radiance)
            val curvePath2 = Path().apply {
                val first = points2.first()
                moveTo(toPxX(first.first), toPxY(first.second))
                for (k in 1 until points2.size) {
                    val p = points2[k]
                    lineTo(toPxX(p.first), toPxY(p.second))
                }
            }
            drawPath(
                path = curvePath2,
                color = Color(0xFFFFB74D),
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
                val tickStr = formatTickVal(xVal)
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
