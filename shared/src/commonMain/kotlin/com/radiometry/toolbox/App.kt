package com.radiometry.toolbox

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

// Define the available tools in the toolbox automatically from ToolType enum
val tools = ToolType.entries.map { Tool(it) }

fun getRecentToolNames(settingsManager: SettingsManager): List<String> {
    val raw = settingsManager.getString("key_recent_tools", "")
    if (raw.isBlank()) return emptyList()
    val validNames = tools.map { it.name }.toSet()
    return raw.split(",")
        .map { it.trim() }
        .filter { it in validNames }
        .distinct()
        .take(MAX_RECENT_TOOLS_COUNT)
}

fun recordToolUsed(toolName: String, settingsManager: SettingsManager) {
    val currentRecent = getRecentToolNames(settingsManager).toMutableList()
    currentRecent.remove(toolName)
    currentRecent.add(0, toolName)
    val updated = currentRecent.take(MAX_RECENT_TOOLS_COUNT).joinToString(",")
    settingsManager.putString("key_recent_tools", updated)
}

@Composable
fun RadiometryToolboxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme()
    } else {
        lightColorScheme()
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}

@Composable
fun App() {
    RadiometryToolboxTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            ToolboxApp()
        }
    }
}

@Composable
fun ToolboxApp(settingsManager: SettingsManager = rememberSettingsManager()) {
    var currentToolIndex by remember {
        mutableIntStateOf(
            settingsManager.getInt("key_current_tool_index", 0).coerceIn(0, tools.size - 1),
        )
    }
    var isZoomedOut by remember { mutableStateOf(false) }
    var expandedSettingIndex by remember { mutableIntStateOf(-1) }

    var sharedTemperatureText by remember {
        val saved = settingsManager.getString("key_shared_temperature", "")
        val initial = saved.ifEmpty {
            settingsManager.getString(
                "key_temp_input",
                settingsManager.getString("key_radiance_temperature", "5800"),
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
                        TemperatureUnit.KELVIN.name,
                    ),
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
    val alphabeticalTools = remember { tools.sortedBy { it.name } }

    var recentToolNames by remember { mutableStateOf(getRecentToolNames(settingsManager)) }

    // Record tool usage whenever active tool changes
    LaunchedEffect(currentToolIndex) {
        recordToolUsed(tools[currentToolIndex].name, settingsManager)
        recentToolNames = getRecentToolNames(settingsManager)
    }

    val scrollState = rememberScrollState()

    // Reset scroll position to top when switching tools or opening tools list
    LaunchedEffect(currentToolIndex, isZoomedOut) {
        scrollState.scrollTo(0)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .pointerInput(currentToolIndex, isZoomedOut, expandedSettingIndex) {
                    awaitEachGesture {
                        var initialDist = -1f
                        var dragAccumulatorX = 0f
                        var dragAccumulatorY = 0f

                        awaitFirstDown(requireUnconsumed = false)

                        do {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val pressedPointers = event.changes.filter { it.pressed }

                            if (pressedPointers.size >= 2) {
                                // Multitouch pinch gesture
                                val p1 = pressedPointers[0].position
                                val p2 = pressedPointers[1].position
                                val currentDist = (p1 - p2).getDistance()

                                if (initialDist < 0f) {
                                    initialDist = currentDist
                                } else if (initialDist > 0f) {
                                    val scale = currentDist / initialDist

                                    if (!isZoomedOut) {
                                        if (scale < 0.8f) {
                                            isZoomedOut = true
                                            event.changes.forEach { it.consume() }
                                            break
                                        }
                                    } else {
                                        if (scale > 1.25f) {
                                            isZoomedOut = false
                                            event.changes.forEach { it.consume() }
                                            break
                                        }
                                    }
                                }
                            } else if (pressedPointers.size == 1) {
                                initialDist = -1f
                                val change = pressedPointers[0]
                                if (!change.isConsumed) {
                                    val dragAmount = change.position - change.previousPosition
                                    dragAccumulatorX += dragAmount.x
                                    dragAccumulatorY += dragAmount.y

                                    if (!isZoomedOut) {
                                        if (abs(dragAccumulatorX) > 80f && abs(dragAccumulatorX) > abs(dragAccumulatorY) * 1.5f) {
                                            // Horizontal swipe -> switch tool
                                            change.consume()
                                            if (dragAccumulatorX < 0f && currentToolIndex < tools.size - 1) {
                                                currentToolIndex++
                                                settingsManager.putInt("key_current_tool_index", currentToolIndex)
                                                expandedSettingIndex = -1
                                            } else if (dragAccumulatorX > 0f && currentToolIndex > 0) {
                                                currentToolIndex--
                                                settingsManager.putInt("key_current_tool_index", currentToolIndex)
                                                expandedSettingIndex = -1
                                            }
                                            break
                                        } else if (abs(dragAccumulatorY) > 80f && abs(dragAccumulatorY) > abs(dragAccumulatorX)) {
                                            dragAccumulatorX = 0f
                                            dragAccumulatorY = 0f
                                        }
                                    }
                                }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                },
        ) {
            // Header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = if (isZoomedOut) "All Tools" else currentTool.name,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (!isZoomedOut) {
                        Text(
                            text = "${currentToolIndex + 1}/${tools.size}",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        IconButton(
                            onClick = { isZoomedOut = true },
                            modifier = Modifier.size(36.dp),
                        ) {
                            GridIcon(
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    } else {
                        TextButton(onClick = { isZoomedOut = false }) {
                            Text(
                                text = "Close",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }

            // Main content area with smooth animation between zoomed out overview and active tool
            AnimatedContent(
                targetState = isZoomedOut,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) { zoomedOut ->
                if (zoomedOut) {
                    val recentToolsList = recentToolNames.mapNotNull { name -> tools.find { it.name == name } }
                    ToolSelectionList(
                        allToolsSorted = alphabeticalTools,
                        recentTools = recentToolsList,
                        currentTool = currentTool,
                        onToolSelected = { selectedTool ->
                            val index = tools.indexOf(selectedTool)
                            if (index >= 0) {
                                currentToolIndex = index
                                settingsManager.putInt("key_current_tool_index", index)
                                expandedSettingIndex = -1
                                isZoomedOut = false
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(scrollState)
                            .padding(16.dp),
                    ) {
                        VisualizationPanel(
                            tool = currentTool,
                            settingsManager = settingsManager,
                            temperatureText = sharedTemperatureText,
                            onTemperatureTextChange = onTemperatureTextChange,
                            temperatureUnit = sharedTemperatureUnit,
                            onTemperatureUnitChange = onTemperatureUnitChange,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GridIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
) {
    val iconColor = if (color == Color.Unspecified) MaterialTheme.colorScheme.onPrimaryContainer else color
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val strokeWidth = 2.dp.toPx()
        val cellSize = width * 0.38f
        val gap = width * 0.12f
        val startX = (width - (cellSize * 2 + gap)) / 2f
        val startY = (height - (cellSize * 2 + gap)) / 2f

        drawRoundRect(
            color = iconColor,
            topLeft = Offset(startX, startY),
            size = Size(cellSize, cellSize),
            cornerRadius = CornerRadius(2.dp.toPx()),
            style = Stroke(strokeWidth),
        )
        drawRoundRect(
            color = iconColor,
            topLeft = Offset(startX + cellSize + gap, startY),
            size = Size(cellSize, cellSize),
            cornerRadius = CornerRadius(2.dp.toPx()),
            style = Stroke(strokeWidth),
        )
        drawRoundRect(
            color = iconColor,
            topLeft = Offset(startX, startY + cellSize + gap),
            size = Size(cellSize, cellSize),
            cornerRadius = CornerRadius(2.dp.toPx()),
            style = Stroke(strokeWidth),
        )
        drawRoundRect(
            color = iconColor,
            topLeft = Offset(startX + cellSize + gap, startY + cellSize + gap),
            size = Size(cellSize, cellSize),
            cornerRadius = CornerRadius(2.dp.toPx()),
            style = Stroke(strokeWidth),
        )
    }
}

@Composable
fun ToolCard(
    tool: Tool,
    isSelected: Boolean,
    onClick: () -> Unit,
    indexPrefix: String? = null,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = tool.color,
            contentColor = contentColorFor(tool.color),
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 6.dp else 2.dp,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (indexPrefix != null) {
                        Text(
                            text = indexPrefix,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = contentColorFor(tool.color).copy(alpha = 0.8f),
                        )
                    }
                    Text(
                        text = tool.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = contentColorFor(tool.color),
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tool.description,
                    fontSize = 14.sp,
                    color = contentColorFor(tool.color).copy(alpha = 0.9f),
                )
            }

            if (isSelected) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Text(
                        text = "Active",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun ToolSelectionList(
    allToolsSorted: List<Tool>,
    recentTools: List<Tool>,
    currentTool: Tool,
    onToolSelected: (Tool) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Section 1: Recently Used (Top 5)
        if (recentTools.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Recently Used",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                )

                recentTools.forEach { tool ->
                    ToolCard(
                        tool = tool,
                        isSelected = (tool.name == currentTool.name),
                        onClick = { onToolSelected(tool) },
                    )
                }
            }
        }

        // Section 2: All Tools (Alphabetical)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "All Tools (A–Z)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
            )

            allToolsSorted.forEachIndexed { index, tool ->
                ToolCard(
                    tool = tool,
                    isSelected = (tool.name == currentTool.name),
                    onClick = { onToolSelected(tool) },
                    indexPrefix = "${index + 1}.",
                )
            }
        }
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
        // Tool-specific visualization / output automatically mapped from ToolType enum
        when (tool.type) {
            ToolType.BLACKBODY_RADIANCE -> RadianceVisualization(
                settingsManager = settingsManager,
                temperatureText = temperatureText,
                onTemperatureTextChange = onTemperatureTextChange,
                temperatureUnit = temperatureUnit,
                onTemperatureUnitChange = onTemperatureUnitChange,
            )

            ToolType.TEMPERATURE_UNITS -> TemperatureConverter(
                settingsManager = settingsManager,
                temperatureText = temperatureText,
                onTemperatureTextChange = onTemperatureTextChange,
                temperatureUnit = temperatureUnit,
                onTemperatureUnitChange = onTemperatureUnitChange,
            )

            ToolType.SOURCE_IRRADIANCE -> LightSourceVisualization(settingsManager)

            ToolType.BLACKBODY_CONTRAST -> TemperatureComparison(
                settingsManager = settingsManager,
                temperatureText = temperatureText,
                onTemperatureTextChange = onTemperatureTextChange,
                temperatureUnit = temperatureUnit,
                onTemperatureUnitChange = onTemperatureUnitChange,
            )

            ToolType.IFOV_GSD -> IfovGsdVisualization(settingsManager)

            ToolType.SOLAR_POSITION -> SolarAngleVisualization(settingsManager)

            ToolType.SUN_EVENTS -> SolarEventsVisualization(settingsManager)

            ToolType.LUX_METER -> AmbientLightVisualization(settingsManager)
        }
    }
}
