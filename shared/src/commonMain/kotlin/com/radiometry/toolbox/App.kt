package com.radiometry.toolbox

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/*
TODO more tools!
TODO: Utilize solar calculator tool to make a spectral solar irradiance tool with band selection.
TODO: Dynamic range calculator tool. Input is full well capacity in electrons (e-), read noise in ADC counts, ADC-bits. Assume a black level of 0 and full well saturation at 2^ADC-bits. Output is dynamic range in dB. Also calculate maximum SNR in dB assuming photon noise limited system.
----  Dynamic Range Calculator -----
Inputs:

Full well capacity
Read noise
ADC bits

Outputs:

Dynamic range (dB)
Effective dynamic range
ENOB estimate

 */

// Define the available tools in the toolbox with ultra-high contrast background colors (> 15:1 WCAG contrast ratio with white text)
val tools = listOf(
    Tool("Blackbody Radiance", "Calculate Planck spectral and band radiance", Color(0xFF0A2246)),
    Tool("Thermal Contrast", "Compare radiance contrast between target temperatures", Color(0xFF500A1D)),
    Tool("Temperature Units", "Convert temperature units and view blackbody color", Color(0xFF002C27)),
    Tool("Source Irradiance", "Calculate irradiance at distance from a beam source", Color(0xFF3B0B4C)),
    Tool("IFOV & GSD", "Calculate optics IFOV and Ground Sampling Distance", Color(0xFF421C00)),
    Tool("Solar Position", "Calculate sun elevation, azimuth, and compass orientation", Color(0xFF481400)),
    Tool("Sun Events", "Sunrise, sunset, twilights, and golden/blue hours", Color(0xFF3E1A00)),
    Tool("Lux Meter", "Measure real-time illuminance using the ambient light sensor", Color(0xFF082C0C)),
)


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
            "Blackbody Radiance" -> RadianceVisualization(
                settingsManager = settingsManager,
                temperatureText = temperatureText,
                onTemperatureTextChange = onTemperatureTextChange,
                temperatureUnit = temperatureUnit,
                onTemperatureUnitChange = onTemperatureUnitChange,
            )

            "Temperature Units" -> TemperatureConverter(
                settingsManager = settingsManager,
                temperatureText = temperatureText,
                onTemperatureTextChange = onTemperatureTextChange,
                temperatureUnit = temperatureUnit,
                onTemperatureUnitChange = onTemperatureUnitChange,
            )

            "Source Irradiance" -> LightSourceVisualization(settingsManager)

            "Thermal Contrast" -> TemperatureComparison(
                settingsManager = settingsManager,
                temperatureText = temperatureText,
                onTemperatureTextChange = onTemperatureTextChange,
                temperatureUnit = temperatureUnit,
                onTemperatureUnitChange = onTemperatureUnitChange,
            )

            "IFOV & GSD" -> IfovGsdVisualization(settingsManager)

            "Solar Position" -> SolarAngleVisualization(settingsManager)

            "Sun Events" -> SolarEventsVisualization(settingsManager)

            "Lux Meter" -> AmbientLightVisualization(settingsManager)
        }
    }
}
