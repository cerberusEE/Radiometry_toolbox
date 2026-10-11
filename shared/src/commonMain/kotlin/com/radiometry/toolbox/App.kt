package com.radiometry.toolbox

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

// Define the available tools in the toolbox
val tools = listOf(
    Tool("Temperature Converter", "Convert between temperature units", Color(0xFF018786)),
    Tool("Radiance Calculator", "Calculate radiance values", Color(0xFF6200EE)),
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
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            ToolboxApp()
        }
    }
}

@Composable
fun ToolboxApp(settingsManager: SettingsManager = rememberSettingsManager()) {
    var currentToolIndex by remember {
        mutableIntStateOf(
            settingsManager.getInt("key_current_tool_index", 0).coerceIn(0, tools.size - 1)
        )
    }
    var expandedSettingIndex by remember { mutableIntStateOf(-1) }

    var sharedTemperatureText by remember {
        val saved = settingsManager.getString("key_shared_temperature", "")
        val initial = saved.ifEmpty {
            settingsManager.getString(
                "key_temp_input",
                settingsManager.getString("key_radiance_temperature", "5800")
            )
        }
        mutableStateOf(initial)
    }

    var sharedTemperatureUnit by remember {
        val savedName = settingsManager.getString("key_shared_temperature_unit", "")
        val initialName =
            savedName.ifEmpty {
                settingsManager.getString(
                    "key_temp_input_unit",
                    settingsManager.getString(
                        "key_radiance_temperature_unit",
                        TemperatureUnit.KELVIN.name
                    )
                )
            }
        val unit =
            runCatching { TemperatureUnit.valueOf(initialName) }.getOrDefault(TemperatureUnit.KELVIN)
        mutableStateOf(unit)
    }

    val onTemperatureTextChange: (String) -> Unit = { newText ->
        sharedTemperatureText = newText
        settingsManager.putString("key_shared_temperature", newText)
        settingsManager.putString("key_temp_input", newText)
        settingsManager.putString("key_radiance_temperature", newText)
    }

    val onTemperatureUnitChange: (TemperatureUnit) -> Unit = { newUnit ->
        sharedTemperatureUnit = newUnit
        settingsManager.putString("key_shared_temperature_unit", newUnit.name)
        settingsManager.putString("key_temp_input_unit", newUnit.name)
        settingsManager.putString("key_radiance_temperature_unit", newUnit.name)
    }

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
                    if ((abs(dragAccumulatorX) > 80f) && (abs(dragAccumulatorX) > abs(
                            dragAccumulatorY
                        ))
                    ) {
                        if ((dragAccumulatorX < 0f) && (currentToolIndex < (tools.size - 1))) {
                            // Swipe LEFT -> next tool
                            currentToolIndex++
                            settingsManager.putInt("key_current_tool_index", currentToolIndex)
                            expandedSettingIndex = -1
                        } else if ((dragAccumulatorX > 0f) && (currentToolIndex > 0)) {
                            // Swipe RIGHT -> previous tool
                            currentToolIndex--
                            settingsManager.putInt("key_current_tool_index", currentToolIndex)
                            expandedSettingIndex = -1
                        }
                        dragAccumulatorX = 0f
                        dragAccumulatorY = 0f
                    }
                    // Vertical swipe detection for settings expansion
                    else if ((abs(dragAccumulatorY) > 80f) && (abs(dragAccumulatorY) > abs(
                            dragAccumulatorX
                        ))
                    ) {
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
            settingsManager = settingsManager,
            temperatureText = sharedTemperatureText,
            onTemperatureTextChange = onTemperatureTextChange,
            temperatureUnit = sharedTemperatureUnit,
            onTemperatureUnitChange = onTemperatureUnitChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )
    }
}

@Composable
fun VisualizationPanel(
    tool: Tool,
    settingsManager: SettingsManager,
    temperatureText: String,
    onTemperatureTextChange: (String) -> Unit,
    temperatureUnit: TemperatureUnit,
    onTemperatureUnitChange: (TemperatureUnit) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = tool.color.copy(alpha = 0.85f),
            contentColor = contentColorFor(tool.color),
        ),
    ) {
        // Tool-specific visualization / output
        when (tool.name) {
            "Radiance Calculator" -> RadianceVisualization(
                settingsManager = settingsManager,
                temperatureText = temperatureText,
                onTemperatureTextChange = onTemperatureTextChange,
                temperatureUnit = temperatureUnit,
                onTemperatureUnitChange = onTemperatureUnitChange,
            )

            "Temperature Converter" -> TemperatureConverter(
                settingsManager = settingsManager,
                temperatureText = temperatureText,
                onTemperatureTextChange = onTemperatureTextChange,
                temperatureUnit = temperatureUnit,
                onTemperatureUnitChange = onTemperatureUnitChange,
            )

            "Light source calculator" -> LightSourceVisualization(settingsManager)
        }
    }
}
