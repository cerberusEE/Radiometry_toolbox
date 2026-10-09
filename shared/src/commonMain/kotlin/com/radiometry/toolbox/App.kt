package com.radiometry.toolbox

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

// Define the available tools in the toolbox
val tools = listOf(
    Tool("Radiance Calculator", "Calculate radiance values", Color(0xFF6200EE)),
    Tool("Irradiance Analyzer", "Analyze irradiance measurements", Color(0xFF03DAC5)),
    Tool("Spectrum Viewer", "Visualize spectral distributions", Color(0xFF3700B3)),
    Tool("Temperature Converter", "Convert between temperature units", Color(0xFF018786)),
    Tool("Flux Calculator", "Calculate luminous flux", Color(0xFFBB86FC)),
)

// Define settings for each tool
val toolSettings = mapOf(
    "Radiance Calculator" to listOf(
        Setting("Wavelength Range", "nm", "400-700"),
        Setting("Measurement Unit", "W/sr/m²", "W/sr/m²"),
        Setting("Precision", "Decimal places", "4")
    ),
    "Irradiance Analyzer" to listOf(
        Setting("Sensor Type", "Silicon, InGaAs", "Silicon"),
        Setting("Integration Time", "ms", "100"),
        Setting("Calibration Factor", "", "1.0")
    ),
    "Spectrum Viewer" to listOf(
        Setting("Wavelength Range", "nm", "380-780"),
        Setting("Resolution", "nm", "1"),
        Setting("Normalization", "Method", "Peak")
    ),
    "Temperature Converter" to listOf(
        Setting("Input Unit", "K, °C, °F", "K"),
        Setting("Output Unit", "K, °C, °F", "°C"),
        Setting("Precision", "Decimal places", "2")
    ),
    "Flux Calculator" to listOf(
        Setting("Luminous Efficacy", "lm/W", "683"),
        Setting("Radiant Power", "W", "1.0"),
        Setting("Correction Factor", "", "1.0")
    )
)

data class Tool(
    val name: String,
    val description: String,
    val color: Color
)

data class Setting(
    val name: String,
    val unit: String,
    val defaultValue: String
)

@Composable
fun App() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            ToolboxApp()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ToolboxApp() {
    var currentToolIndex by remember { mutableIntStateOf(0) }
    var expandedSettingIndex by remember { mutableIntStateOf(-1) }
    
    val currentTool = tools[currentToolIndex]
    val currentSettings = toolSettings[currentTool.name] ?: emptyList()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Tool selector header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = currentTool.name,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp)
            )
            Text(
                text = "${currentToolIndex + 1}/${tools.size}",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontSize = 16.sp,
                modifier = Modifier.padding(end = 16.dp)
            )
        }
        
        // Main content area with visualization and settings
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            // Visualization panel at the top
            VisualizationPanel(
                tool = currentTool,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .align(Alignment.TopCenter)
            )
            
            // Settings panels below visualization
            SettingsPanel(
                settings = currentSettings,
                expandedIndex = expandedSettingIndex,
                onSettingClick = { index -> 
                    expandedSettingIndex = if (expandedSettingIndex == index) -1 else index
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(top = 320.dp)
            )
        }
        
        // Swipe gesture handlers
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, _ ->
                        val (_, x) = change.position
                        val delta = change.positionChange().x
                        
                        if (abs(delta) > 50) {
                            if (delta > 0 && currentToolIndex > 0) {
                                currentToolIndex--
                            } else if (delta < 0 && currentToolIndex < tools.size - 1) {
                                currentToolIndex++
                            }
                            expandedSettingIndex = -1
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectVerticalDragGestures { change, _ ->
                        val (_, y) = change.position
                        val delta = change.positionChange().y
                        
                        if (abs(delta) > 50) {
                            if (delta < 0) {
                                // Swipe up - collapse settings
                                if (expandedSettingIndex != -1) {
                                    expandedSettingIndex = -1
                                }
                            } else {
                                // Swipe down - expand next setting
                                if (expandedSettingIndex == -1 && currentSettings.isNotEmpty()) {
                                    expandedSettingIndex = 0
                                } else if (expandedSettingIndex < currentSettings.size - 1) {
                                    expandedSettingIndex++
                                }
                            }
                        }
                    }
                }
        )
    }
}

@Composable
fun VisualizationPanel(tool: Tool, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = tool.color.copy(alpha = 0.8f),
            contentColor = contentColorFor(tool.color)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = tool.name,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = tool.description,
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Tool-specific visualization
                when (tool.name) {
                    "Radiance Calculator" -> RadianceVisualization()
                    "Irradiance Analyzer" -> IrradianceVisualization()
                    "Spectrum Viewer" -> SpectrumVisualization()
                    "Temperature Converter" -> TemperatureVisualization()
                    "Flux Calculator" -> FluxVisualization()
                }
            }
        }
    }
}

@Composable
fun RadianceVisualization() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Radiance: 1.23 W/sr/m²",
            fontSize = 20.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun IrradianceVisualization() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Irradiance: 456 mW/cm²",
            fontSize = 20.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SpectrumVisualization() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Spectrum: 400-700 nm",
            fontSize = 20.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun TemperatureVisualization() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Temperature: 5800 K",
            fontSize = 20.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun FluxVisualization() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Luminous Flux: 1500 lm",
            fontSize = 20.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingsPanel(
    settings: List<Setting>,
    expandedIndex: Int,
    onSettingClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        state = rememberLazyListState(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(settings) { index, setting ->
            SettingCard(
                setting = setting,
                isExpanded = index == expandedIndex,
                onClick = { onSettingClick(index) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun SettingCard(
    setting: Setting,
    isExpanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isExpanded) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    
    val contentColor = if (isExpanded) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = setting.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                    if (setting.unit.isNotEmpty()) {
                        Text(
                            text = setting.unit,
                            fontSize = 14.sp,
                            color = contentColor.copy(alpha = 0.7f)
                        )
                    }
                }
                Text(
                    text = setting.defaultValue,
                    fontSize = 16.sp,
                    color = contentColor
                )
            }
            
            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                ExpandedSettingContent(setting = setting)
            }
        }
    }
}

@Composable
fun ExpandedSettingContent(setting: Setting) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, start = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Current Value: ${setting.defaultValue}",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
        
        when (setting.name) {
            "Wavelength Range" -> {
                Text("Range: 380-780 nm", fontSize = 14.sp)
                Text("Step: 1 nm", fontSize = 14.sp)
            }
            "Measurement Unit" -> {
                Text("Available: W/sr/m², µW/sr/cm²", fontSize = 14.sp)
            }
            "Precision" -> {
                Text("Range: 0-8 decimal places", fontSize = 14.sp)
            }
            "Sensor Type" -> {
                Text("Available: Silicon, InGaAs, PbS", fontSize = 14.sp)
            }
            "Integration Time" -> {
                Text("Range: 1-1000 ms", fontSize = 14.sp)
            }
            "Calibration Factor" -> {
                Text("Range: 0.1-10.0", fontSize = 14.sp)
            }
            "Resolution" -> {
                Text("Range: 0.1-10 nm", fontSize = 14.sp)
            }
            "Normalization" -> {
                Text("Methods: Peak, Area, Integral", fontSize = 14.sp)
            }
            "Input Unit", "Output Unit" -> {
                Text("Available: K, °C, °F", fontSize = 14.sp)
            }
            "Luminous Efficacy" -> {
                Text("Range: 1-1000 lm/W", fontSize = 14.sp)
            }
            "Radiant Power" -> {
                Text("Range: 0.001-1000 W", fontSize = 14.sp)
            }
            "Correction Factor" -> {
                Text("Range: 0.01-10.0", fontSize = 14.sp)
            }
        }
    }
}

// Preview function for testing
@Composable
fun AppPreview() {
    App()
}
