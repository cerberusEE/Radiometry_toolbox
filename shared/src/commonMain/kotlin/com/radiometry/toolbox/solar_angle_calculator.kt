package com.radiometry.toolbox

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun getCardinalDirection(azimuthDeg: Double): String {
    val directions = arrayOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
    val index = (((azimuthDeg % 360.0) + 11.25) / 22.5).toInt() % 16
    return directions[index]
}

@Composable
fun LocationPinIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height * 0.38f
        val r = size.minDimension * 0.3f

        // Pin head circle
        drawCircle(color = color, radius = r, center = Offset(cx, cy))
        // Inner hole
        drawCircle(color = Color(0xFF303030), radius = r * 0.45f, center = Offset(cx, cy))
        // Pin point triangle
        val path = Path().apply {
            moveTo(cx - r * 0.85f, cy + r * 0.3f)
            lineTo(cx, size.height * 0.92f)
            lineTo(cx + r * 0.85f, cy + r * 0.3f)
            close()
        }
        drawPath(path, color = color)
    }
}

@Composable
fun CompassIcon(enabled: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f - 2.dp.toPx()

        drawCircle(
            color = if (enabled) Color.White else Color.White.copy(alpha = 0.7f),
            radius = radius,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )

        val needlePathNorth = Path().apply {
            moveTo(center.x, center.y - radius + 3.dp.toPx())
            lineTo(center.x - 3.5.dp.toPx(), center.y)
            lineTo(center.x + 3.5.dp.toPx(), center.y)
            close()
        }
        val needlePathSouth = Path().apply {
            moveTo(center.x, center.y + radius - 3.dp.toPx())
            lineTo(center.x - 3.5.dp.toPx(), center.y)
            lineTo(center.x + 3.5.dp.toPx(), center.y)
            close()
        }
        drawPath(needlePathNorth, color = if (enabled) Color(0xFFFF5252) else Color.Red.copy(alpha = 0.7f))
        drawPath(needlePathSouth, color = Color.White.copy(alpha = 0.8f))
    }
}

@Composable
fun SolarAngleVisualization(settingsManager: SettingsManager) {
    val locationProvider = rememberLocationProvider()
    val compassSensor = rememberCompassSensor()

    val systemNow = remember { getSystemDateTime() }

    var latitudeText by remember {
        mutableStateOf(settingsManager.getString("key_solar_latitude", "39.742476"))
    }
    var longitudeText by remember {
        mutableStateOf(settingsManager.getString("key_solar_longitude", "-105.1786"))
    }

    var yearText by remember {
        mutableStateOf(settingsManager.getString("key_solar_year", systemNow.year.toString()))
    }
    var monthText by remember {
        mutableStateOf(settingsManager.getString("key_solar_month", systemNow.month.toString()))
    }
    var dayText by remember {
        mutableStateOf(settingsManager.getString("key_solar_day", systemNow.day.toString()))
    }
    var hourText by remember {
        mutableStateOf(settingsManager.getString("key_solar_hour", systemNow.hour.toString()))
    }
    var minuteText by remember {
        mutableStateOf(settingsManager.getString("key_solar_minute", systemNow.minute.toString()))
    }
    var tzOffsetText by remember {
        val initialTz = if (systemNow.timeZoneOffsetHours >= 0) "+${systemNow.timeZoneOffsetHours}" else systemNow.timeZoneOffsetHours.toString()
        mutableStateOf(settingsManager.getString("key_solar_tz_offset", initialTz))
    }

    var compassEnabled by remember { mutableStateOf(false) }
    var currentHeading by remember { mutableStateOf<Float?>(null) }
    var locationStatusMessage by remember { mutableStateOf<String?>(null) }

    val compassFilter = remember { CompassLowPassFilter(timeConstantSeconds = 0.5) }

    DisposableEffect(compassEnabled) {
        if (compassEnabled) {
            compassFilter.reset()
            compassSensor.startListening { rawHeading ->
                currentHeading = compassFilter.filter(rawHeading)
            }
        } else {
            compassSensor.stopListening()
            currentHeading = null
        }
        onDispose {
            compassSensor.stopListening()
        }
    }

    val lat = latitudeText.toDoubleOrNull()
    val lon = longitudeText.toDoubleOrNull()
    val yr = yearText.toIntOrNull()
    val mo = monthText.toIntOrNull()
    val dy = dayText.toIntOrNull()
    val hr = hourText.toIntOrNull()
    val mn = minuteText.toIntOrNull()
    val tz = tzOffsetText.replace("+", "").toDoubleOrNull()

    val spaResult: SpaResult? = if (lat != null && lon != null && yr != null && mo != null && dy != null && hr != null && mn != null && tz != null) {
        runCatching {
            SpaCalculator.calculateSolarPosition(
                latitudeDeg = lat,
                longitudeDeg = lon,
                year = yr,
                month = mo,
                day = dy,
                hour = hr,
                minute = mn,
                second = 0.0,
                timeZoneOffsetHours = tz
            )
        }.getOrNull()
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
        // Output Results Display Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                .padding(12.dp),
        ) {
            if (spaResult != null) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(
                        text = "SOLAR POSITION OUTPUT",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text = "Azimuth Angle",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 13.sp,
                            )
                            Text(
                                text = "${formatNumbers(spaResult.azimuthAngleDeg)}° (${getCardinalDirection(spaResult.azimuthAngleDeg)})",
                                color = Color(0xFFFFD54F),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Elevation Angle",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 13.sp,
                            )
                            Text(
                                text = "${formatNumbers(spaResult.elevationAngleDeg)}°",
                                color = Color(0xFF81C784),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "Invalid Position or Date/Time Input",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        // Compass Comparison Display Dial
        if (spaResult != null) {
            SolarCompassDial(
                solarAzimuthDeg = spaResult.azimuthAngleDeg,
                phoneHeadingDeg = currentHeading,
                compassEnabled = compassEnabled,
                onToggleCompass = {
                    compassEnabled = !compassEnabled
                }
            )
        }

        // Location Input Section
        Text(
            text = "Observer Position (WGS84)",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = latitudeText,
                onValueChange = {
                    latitudeText = it
                    settingsManager.putString("key_solar_latitude", it)
                },
                label = { Text("Latitude (°)") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = textFieldColors,
            )

            OutlinedTextField(
                value = longitudeText,
                onValueChange = {
                    longitudeText = it
                    settingsManager.putString("key_solar_longitude", it)
                },
                label = { Text("Longitude (°)") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = textFieldColors,
            )

            OutlinedButton(
                onClick = {
                    locationStatusMessage = "Fetching GPS location..."
                    locationProvider.getCurrentLocation { coords ->
                        if (coords != null) {
                            val latStr = formatNumbers(coords.latitude)
                            val lonStr = formatNumbers(coords.longitude)
                            latitudeText = latStr
                            longitudeText = lonStr
                            settingsManager.putString("key_solar_latitude", latStr)
                            settingsManager.putString("key_solar_longitude", lonStr)
                            locationStatusMessage = "Location updated!"
                        } else {
                            locationStatusMessage = "Could not fetch GPS. Check permissions."
                        }
                    }
                },
                modifier = Modifier
                    .padding(top = 6.dp)
                    .height(56.dp)
                    .width(52.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            ) {
                LocationPinIcon(modifier = Modifier.size(24.dp))
            }
        }

        if (locationStatusMessage != null) {
            Text(
                text = locationStatusMessage!!,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp,
            )
        }

        // Date and Time Picker Section
        Text(
            text = "Date & Time Input",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            OutlinedTextField(
                value = yearText,
                onValueChange = {
                    yearText = it
                    settingsManager.putString("key_solar_year", it)
                },
                label = { Text("Year") },
                modifier = Modifier.weight(1.2f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = textFieldColors,
            )
            OutlinedTextField(
                value = monthText,
                onValueChange = {
                    monthText = it
                    settingsManager.putString("key_solar_month", it)
                },
                label = { Text("Month") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = textFieldColors,
            )
            OutlinedTextField(
                value = dayText,
                onValueChange = {
                    dayText = it
                    settingsManager.putString("key_solar_day", it)
                },
                label = { Text("Day") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = textFieldColors,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            OutlinedTextField(
                value = hourText,
                onValueChange = {
                    hourText = it
                    settingsManager.putString("key_solar_hour", it)
                },
                label = { Text("Hour (0-23)") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = textFieldColors,
            )
            OutlinedTextField(
                value = minuteText,
                onValueChange = {
                    minuteText = it
                    settingsManager.putString("key_solar_minute", it)
                },
                label = { Text("Min (0-59)") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = textFieldColors,
            )
            OutlinedTextField(
                value = tzOffsetText,
                onValueChange = {
                    tzOffsetText = it
                    settingsManager.putString("key_solar_tz_offset", it)
                },
                label = { Text("UTC Offset") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = textFieldColors,
            )
        }

        OutlinedButton(
            onClick = {
                val now = getSystemDateTime()
                yearText = now.year.toString()
                monthText = now.month.toString()
                dayText = now.day.toString()
                hourText = now.hour.toString()
                minuteText = now.minute.toString()
                tzOffsetText = if (now.timeZoneOffsetHours >= 0) "+${now.timeZoneOffsetHours}" else now.timeZoneOffsetHours.toString()

                settingsManager.putString("key_solar_year", yearText)
                settingsManager.putString("key_solar_month", monthText)
                settingsManager.putString("key_solar_day", dayText)
                settingsManager.putString("key_solar_hour", hourText)
                settingsManager.putString("key_solar_minute", minuteText)
                settingsManager.putString("key_solar_tz_offset", tzOffsetText)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
        ) {
            Text("Set to Current Device Time")
        }
    }
}

@Composable
fun SolarCompassDial(
    solarAzimuthDeg: Double,
    phoneHeadingDeg: Float?,
    compassEnabled: Boolean,
    onToggleCompass: () -> Unit
) {
    val textMeasurer = rememberTextMeasurer()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SOLAR COMPASS COMPARISON",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
            IconButton(
                onClick = onToggleCompass,
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = if (compassEnabled) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    )
            ) {
                CompassIcon(enabled = compassEnabled, modifier = Modifier.size(20.dp))
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = minOf(size.width, size.height) / 2f - 24.dp.toPx()

            // Draw outer compass circle
            drawCircle(
                color = Color.White.copy(alpha = 0.3f),
                radius = radius,
                center = center,
                style = Stroke(width = 3.dp.toPx())
            )

            // Calculate rotation offsets based on live compass heading
            val compassHeading = phoneHeadingDeg?.toDouble() ?: 0.0
            val rotationOffset = if (compassEnabled) compassHeading else 0.0

            // Draw cardinal points (N, E, S, W)
            val cardinals = listOf("N" to 0.0, "E" to 90.0, "S" to 180.0, "W" to 270.0)
            for ((label, angle) in cardinals) {
                val cardinalAngle = angle - rotationOffset
                val rad = (cardinalAngle - 90.0) * (PI / 180.0)
                val labelX = center.x + (radius + 14.dp.toPx()) * cos(rad).toFloat()
                val labelY = center.y + (radius + 14.dp.toPx()) * sin(rad).toFloat()

                val textLayoutResult = textMeasurer.measure(
                    text = label,
                    style = TextStyle(color = if (label == "N") Color(0xFFFF5252) else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                )
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(labelX - textLayoutResult.size.width / 2f, labelY - textLayoutResult.size.height / 2f)
                )
            }

            // Draw Sun direction vector (Yellow pointer)
            val sunAngleOnDial = solarAzimuthDeg - rotationOffset
            val sunRad = (sunAngleOnDial - 90.0) * (PI / 180.0)
            val sunX = center.x + radius * cos(sunRad).toFloat()
            val sunY = center.y + radius * sin(sunRad).toFloat()

            // Draw line from center to Sun
            drawLine(
                color = Color(0xFFFFD54F),
                start = center,
                end = Offset(sunX, sunY),
                strokeWidth = 4.dp.toPx()
            )

            // Draw Sun icon circle at edge
            drawCircle(
                color = Color(0xFFFFC107),
                radius = 10.dp.toPx(),
                center = Offset(sunX, sunY)
            )

            // Draw Phone Heading Pointer (Red arrow pointing up if compass active)
            if (compassEnabled) {
                val phoneArrowPath = Path().apply {
                    moveTo(center.x, center.y - radius + 10.dp.toPx())
                    lineTo(center.x - 8.dp.toPx(), center.y - radius + 25.dp.toPx())
                    lineTo(center.x + 8.dp.toPx(), center.y - radius + 25.dp.toPx())
                    close()
                }
                drawPath(path = phoneArrowPath, color = Color(0xFFFF5252))
            }
        }

        // Relative Angle Comparison Readout
        if (compassEnabled && phoneHeadingDeg != null) {
            val phoneHeading = phoneHeadingDeg.toDouble()
            var relativeBearing = (solarAzimuthDeg - phoneHeading) % 360.0
            if (relativeBearing < 0) relativeBearing += 360.0

            val sideString = if (relativeBearing <= 180.0) {
                "${formatNumbers(relativeBearing)}° to your Right"
            } else {
                "${formatNumbers(360.0 - relativeBearing)}° to your Left"
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Phone Heading: ${formatNumbers(phoneHeading)}°",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp
                )
                Text(
                    text = "Sun Relative Position: $sideString",
                    color = Color(0xFFFFD54F),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else if (!compassEnabled) {
            Text(
                text = "Sun Azimuth: ${formatNumbers(solarAzimuthDeg)}° (Relative to True North)",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp
            )
        }
    }
}
