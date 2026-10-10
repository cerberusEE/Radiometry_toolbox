package com.radiometry.toolbox

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.roundToInt

// Define the available tools in the toolbox
val tools = listOf(
    Tool("Radiance Calculator", "Calculate radiance values", Color(0xFF6200EE)),
    Tool("Spectrum Viewer", "Visualize spectral distributions", Color(0xFF3700B3)),
    Tool("Temperature Converter", "Convert between temperature units", Color(0xFF018786)),
    Tool("Light source calculator", "Calculate irradiance from a light source", Color(0xFFFF6D00)),
)

data class Tool(
    val name: String,
    val description: String,
    val color: Color,
)

@Composable
fun App() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            ToolboxApp()
        }
    }
}

@Composable
fun ToolboxApp() {
    var currentToolIndex by remember { mutableIntStateOf(0) }
    var expandedSettingIndex by remember { mutableIntStateOf(-1) }
    
    val currentTool = tools[currentToolIndex]

    var dragAccumulatorX by remember { mutableFloatStateOf(0f) }
    var dragAccumulatorY by remember { mutableFloatStateOf(0f) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(currentToolIndex, expandedSettingIndex) {
                detectDragGestures(
                    onDragEnd = {
                        dragAccumulatorX = 0f
                        dragAccumulatorY = 0f
                    },
                    onDragCancel = {
                        dragAccumulatorX = 0f
                        dragAccumulatorY = 0f
                    },
                ) { change, dragAmount ->
                    change.consume()
                    dragAccumulatorX += dragAmount.x
                    dragAccumulatorY += dragAmount.y
                    
                    // Horizontal swipe detection for tool navigation
                    if ((abs(dragAccumulatorX) > 80f) && (abs(dragAccumulatorX) > abs(dragAccumulatorY))) {
                        if ((dragAccumulatorX < 0f) && (currentToolIndex < (tools.size - 1))) {
                            // Swipe LEFT -> next tool
                            currentToolIndex++
                            expandedSettingIndex = -1
                        } else if ((dragAccumulatorX > 0f) && (currentToolIndex > 0)) {
                            // Swipe RIGHT -> previous tool
                            currentToolIndex--
                            expandedSettingIndex = -1
                        }
                        dragAccumulatorX = 0f
                        dragAccumulatorY = 0f
                    } 
                    // Vertical swipe detection for settings expansion
                    else if ((abs(dragAccumulatorY) > 80f) && (abs(dragAccumulatorY) > abs(dragAccumulatorX))) {
                        dragAccumulatorX = 0f
                        dragAccumulatorY = 0f
                    }
                }
            },
    ) {
        // Tool selector header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = currentTool.name,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp),
            )
            Text(
                text = "${currentToolIndex + 1}/${tools.size}",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontSize = 16.sp,
                modifier = Modifier.padding(end = 16.dp),
            )
        }
        
        // Visualization panel showing tool output
        VisualizationPanel(
            tool = currentTool,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )
    }
}

@Composable
fun VisualizationPanel(tool: Tool, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = tool.color.copy(alpha = 0.85f),
            contentColor = contentColorFor(tool.color),
        ),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            // Tool-specific visualization / output
            when (tool.name) {
                "Radiance Calculator" -> RadianceVisualization()
                "Spectrum Viewer" -> SpectrumVisualization()
                "Temperature Converter" -> TemperatureVisualization()
                "Light source calculator" -> LightSourceVisualization()
            }
        }
    }
}

@Composable
fun RadianceVisualization() {
    var startWavelengthText by remember { mutableStateOf("380") }
    var stopWavelengthText by remember { mutableStateOf("780") }
    var wavelengthUnit by remember { mutableStateOf(WavelengthUnit.NANOMETERS) }
    var temperatureText by remember { mutableStateOf("5800") }
    var temperatureUnit by remember { mutableStateOf(TemperatureUnit.KELVIN) }

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
                onValueChange = { startWavelengthText = it },
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
                onValueChange = { stopWavelengthText = it },
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
            onValueChange = { temperatureText = it },
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
                onUnitSelected = { wavelengthUnit = it },
                modifier = Modifier.weight(1f),
            )
            UnitDropdown(
                label = "Temperature Unit",
                selectedUnit = temperatureUnit,
                onUnitSelected = { temperatureUnit = it },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun SpectrumVisualization() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
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
                text = "Spectrum: 400-700 nm",
                fontSize = 20.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

enum class WavelengthUnit(val symbol: String, val label: String) {
    NANOMETERS("nm", "Nanometers"),
    MICROMETERS("µm", "Micrometers"),
    INVERSE_CENTIMETERS("cm⁻¹", "Inverse Centimeters"),
}

fun convertToWavelengthInMeters(value: Double, unit: WavelengthUnit): Double? {
    if (value <= 0.0 || value.isNaN() || value.isInfinite()) return null
    return when (unit) {
        WavelengthUnit.NANOMETERS -> value * 1e-9
        WavelengthUnit.MICROMETERS -> value * 1e-6
        WavelengthUnit.INVERSE_CENTIMETERS -> 1.0 / (100.0 * value)
    }
}

fun convertTemperatureToKelvin(value: Double, unit: TemperatureUnit): Double? {
    if (value.isNaN() || value.isInfinite()) return null
    val kelvin = when (unit) {
        TemperatureUnit.CELSIUS -> value + 273.15
        TemperatureUnit.KELVIN -> value
        TemperatureUnit.FAHRENHEIT -> ((value - 32.0) * 5.0 / 9.0) + 273.15
    }
    return if (kelvin > 0.0) kelvin else null
}

fun calculateRadiance(
    startWavelengthMeters: Double,
    stopWavelengthMeters: Double,
    temperatureKelvin: Double,
    steps: Int = 1000,
): Double {
    if (temperatureKelvin <= 0.0) return 0.0
    val lambdaMin = minOf(startWavelengthMeters, stopWavelengthMeters)
    val lambdaMax = maxOf(startWavelengthMeters, stopWavelengthMeters)
    if (lambdaMin <= 0.0 || lambdaMax <= 0.0 || lambdaMin == lambdaMax) return 0.0

    val c1 = 1.1910429723971884e-16
    val c2 = 0.014387773538277204

    fun spectralRadiance(lambda: Double): Double {
        val x = c2 / (lambda * temperatureKelvin)
        if (x > 700.0) return 0.0
        val expTerm = kotlin.math.exp(x) - 1.0
        if (expTerm <= 0.0) return 0.0
        return c1 / (lambda.pow(5.0) * expTerm)
    }

    val n = if (steps % 2 != 0) steps + 1 else steps
    val hStep = (lambdaMax - lambdaMin) / n
    var sum = spectralRadiance(lambdaMin) + spectralRadiance(lambdaMax)

    for (i in 1 until n) {
        val lambda = lambdaMin + i * hStep
        val factor = if (i % 2 == 0) 2.0 else 4.0
        sum += factor * spectralRadiance(lambda)
    }

    return (hStep / 3.0) * sum
}

fun formatRadiance(value: Double): String {
    if (value.isNaN() || value.isInfinite() || value < 0.0) return "Invalid Input"
    if (value == 0.0 || value < 1e-30) return "0 W/sr/m²"
    val absVal = abs(value)
    return if (absVal >= 1e5 || absVal < 1e-3) {
        val exp = kotlin.math.floor(kotlin.math.log10(absVal)).toInt()
        val mantissa = absVal / 10.0.pow(exp.toDouble())
        val roundedMantissa = round(mantissa * 100.0) / 100.0
        "${roundedMantissa}e$exp W/sr/m²"
    } else {
        val rounded = round(value * 100.0) / 100.0
        if (abs(rounded - value) < 1e-6 && abs(rounded % 1.0) < 1e-6) {
            "${rounded.toLong()} W/sr/m²"
        } else {
            val str = rounded.toString()
            if (str.contains('.')) {
                val parts = str.split('.')
                val decimals = parts[1].take(2).padEnd(2, '0')
                "${parts[0]}.$decimals W/sr/m²"
            } else {
                "$str W/sr/m²"
            }
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

enum class TemperatureUnit(val symbol: String, val label: String) {
    CELSIUS("°C", "Celsius"),
    KELVIN("K", "Kelvin"),
    FAHRENHEIT("°F", "Fahrenheit"),
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
fun TemperatureVisualization() {
    var inputTempText by remember { mutableStateOf("5800") }
    var inputUnit by remember { mutableStateOf(TemperatureUnit.KELVIN) }
    var outputUnit by remember { mutableStateOf(TemperatureUnit.CELSIUS) }

    val inputValue = inputTempText.toDoubleOrNull()
    val convertedValue = inputValue?.let { convertTemperature(it, inputUnit, outputUnit) }
    val convertedKelvin = inputValue?.let { value ->
        when (inputUnit) {
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
            value = inputTempText,
            onValueChange = { inputTempText = it },
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
                selectedUnit = inputUnit,
                onUnitSelected = { inputUnit = it },
                modifier = Modifier.weight(1f),
            )
            UnitDropdown(
                label = "To Unit",
                selectedUnit = outputUnit,
                onUnitSelected = { outputUnit = it },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

fun calculateIrradiance(opticalPower: Double, divergenceSr: Double, distance: Double, useAtmosphericCompensation: Boolean, extinctionCoefficient: Double): Double {
    if (opticalPower <= 0.0 || divergenceSr <= 0.0 || distance < 0.0) return 0.0
    val solidAngle = divergenceSr
    val area = solidAngle * distance * distance
    val irradiance = opticalPower / area
    if (!useAtmosphericCompensation) return irradiance
    val atmosphericLoss = kotlin.math.exp(-extinctionCoefficient * distance)
    return irradiance * atmosphericLoss
}

fun formatIrradiance(value: Double): String {
    if (value.isNaN() || value.isInfinite() || value < 0.0) return "Invalid Input"
    if (value == 0.0 || value < 1e-30) return "0 W/m\u00b2"
    val absVal = abs(value)
    return if (absVal >= 1e5 || absVal < 1e-3) {
        val exp = kotlin.math.floor(kotlin.math.log10(absVal)).toInt()
        val mantissa = absVal / 10.0.pow(exp.toDouble())
        val roundedMantissa = round(mantissa * 100.0) / 100.0
        "${roundedMantissa}e$exp W/m\u00b2"
    } else {
        val rounded = round(value * 100.0) / 100.0
        if (abs(rounded - value) < 1e-6 && abs(rounded % 1.0) < 1e-6) {
            "${rounded.toLong()} W/m\u00b2"
        } else {
            val str = rounded.toString()
            if (str.contains('.')) {
                val parts = str.split('.')
                val decimals = parts[1].take(2).padEnd(2, '0')
                "${parts[0]}.$decimals W/m\u00b2"
            } else {
                "$str W/m\u00b2"
            }
        }
    }
}

@Composable
fun LightSourceVisualization() {
    var opticalPowerText by remember { mutableStateOf("1.0") }
    var divergenceText by remember { mutableStateOf("0.1") }
    var distanceText by remember { mutableStateOf("1.0") }
    var useAtmosphericCompensation by remember { mutableStateOf(false) }
    var extinctionText by remember { mutableStateOf("0.01") }

    val opticalPower = opticalPowerText.toDoubleOrNull()
    val divergenceSr = divergenceText.toDoubleOrNull()
    val distance = distanceText.toDoubleOrNull()
    val extinctionCoefficient = extinctionText.toDoubleOrNull() ?: 0.0

    val irradianceResult = if (opticalPower != null && divergenceSr != null && distance != null) {
        calculateIrradiance(opticalPower, divergenceSr, distance, useAtmosphericCompensation, extinctionCoefficient)
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
            onValueChange = { opticalPowerText = it },
            label = { Text("Optical Power (W)", color = Color.White.copy(alpha = 0.8f)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = textFieldColors,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = divergenceText,
            onValueChange = { divergenceText = it },
            label = { Text("Divergence (sr)", color = Color.White.copy(alpha = 0.8f)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = textFieldColors,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = distanceText,
            onValueChange = { distanceText = it },
            label = { Text("Distance (m)", color = Color.White.copy(alpha = 0.8f)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = textFieldColors,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { useAtmosphericCompensation = !useAtmosphericCompensation }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.material3.Checkbox(
                checked = useAtmosphericCompensation,
                onCheckedChange = { useAtmosphericCompensation = it },
            )
            Spacer(modifier = Modifier.padding(4.dp))
            Text(
                text = "Atmospheric Compensation",
                color = Color.White,
            )
        }

        if (useAtmosphericCompensation) {
            OutlinedTextField(
                value = extinctionText,
                onValueChange = { extinctionText = it },
                label = { Text("Extinction Coefficient (1/m)", color = Color.White.copy(alpha = 0.8f)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = textFieldColors,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
