package com.example.ui.components

import android.content.Context
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.model.TripState
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.util.Locale

enum class OsmMapLayerMode {
    OSM_STREET,
    OSM_DARK,
    GPS_RADAR
}

/**
 * High-performance, 100% Free OpenStreetMap component powered by OSMDroid.
 * Operates purely without Google Maps API keys or Google Play Services.
 * Connects directly to free public OSM tile servers (https://tile.openstreetmap.org/{z}/{x}/{y}.png).
 */
@Composable
fun TripMapView(
    tripState: TripState,
    modifier: Modifier = Modifier
) {
    var selectedLayer by remember { mutableStateOf(OsmMapLayerMode.OSM_STREET) }
    var showLayerMenu by remember { mutableStateOf(false) }

    // Reference to MapView for programmatic zoom and recentering
    var osMapViewRef by remember { mutableStateOf<MapView?>(null) }

    val lat = tripState.latitude ?: 11.0168
    val lon = tripState.longitude ?: 76.9558

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(24.dp))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedLayer) {
                OsmMapLayerMode.OSM_STREET -> {
                    OsmDroidMapView(
                        tripState = tripState,
                        useDarkStyle = false,
                        onMapViewReady = { osMapViewRef = it },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                OsmMapLayerMode.OSM_DARK -> {
                    OsmDroidMapView(
                        tripState = tripState,
                        useDarkStyle = true,
                        onMapViewReady = { osMapViewRef = it },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                OsmMapLayerMode.GPS_RADAR -> {
                    GpsVectorRouteCanvas(
                        tripState = tripState,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Top Status Overlay (OSM attribution badge & Live Coordinates)
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xCC0F172A))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (tripState.latitude != null && tripState.longitude != null) Color(0xFF22C55E) else Color(0xFFEF4444))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (selectedLayer == OsmMapLayerMode.GPS_RADAR) {
                        "GPS Radar • %.4f, %.4f".format(Locale.US, lat, lon)
                    } else {
                        "OpenStreetMap • %.4f, %.4f".format(Locale.US, lat, lon)
                    },
                    color = Color(0xFFF1F5F9),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Top Right Layer Switcher
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
            ) {
                IconButton(
                    onClick = { showLayerMenu = true },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xCC0F172A))
                        .testTag("map_layer_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = "Map Layers",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = showLayerMenu,
                    onDismissRequest = { showLayerMenu = false },
                    modifier = Modifier.background(Color(0xFF1E293B))
                ) {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LightMode, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("OSM Street Map", color = Color.White, fontSize = 12.sp)
                            }
                        },
                        onClick = {
                            selectedLayer = OsmMapLayerMode.OSM_STREET
                            showLayerMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DarkMode, contentDescription = null, tint = Color(0xFF818CF8), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("OSM Dark Mode", color = Color.White, fontSize = 12.sp)
                            }
                        },
                        onClick = {
                            selectedLayer = OsmMapLayerMode.OSM_DARK
                            showLayerMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Radar, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("GPS Radar Canvas", color = Color.White, fontSize = 12.sp)
                            }
                        },
                        onClick = {
                            selectedLayer = OsmMapLayerMode.GPS_RADAR
                            showLayerMenu = false
                        }
                    )
                }
            }

            // Bottom Right Map Controls (Recenter + Zoom In + Zoom Out)
            if (selectedLayer != OsmMapLayerMode.GPS_RADAR) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Recenter button
                    IconButton(
                        onClick = {
                            osMapViewRef?.let { map ->
                                val targetPoint = GeoPoint(lat, lon)
                                map.controller.animateTo(targetPoint)
                                map.controller.setZoom(17.5)
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xEE0F172A))
                            .testTag("recenter_gps_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "Center GPS",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Zoom In button
                    IconButton(
                        onClick = {
                            osMapViewRef?.controller?.zoomIn()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xEE0F172A))
                            .testTag("map_zoom_in_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Zoom In",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Zoom Out button
                    IconButton(
                        onClick = {
                            osMapViewRef?.controller?.zoomOut()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xEE0F172A))
                            .testTag("map_zoom_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Zoom Out",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Native OSMDroid implementation utilizing OpenStreetMap's Mapnik public tile servers.
 * Completely free, does not use Google Maps API keys or Google Play Services.
 */
@Composable
private fun OsmDroidMapView(
    tripState: TripState,
    useDarkStyle: Boolean,
    onMapViewReady: (MapView) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lat = tripState.latitude ?: 11.0168
    val lon = tripState.longitude ?: 76.9558
    val routePoints = tripState.routePoints

    // Configure user agent for OSM tile server compliance
    DisposableEffect(Unit) {
        try {
            Configuration.getInstance().userAgentValue = "${context.packageName}/GetTaxiMeter"
        } catch (_: Exception) {}
        onDispose {}
    }

    AndroidView(
        factory = { ctx ->
            Configuration.getInstance().userAgentValue = "${ctx.packageName}/GetTaxiMeter"
            MapView(ctx).apply {
                // Free, public OpenStreetMap tile source (https://tile.openstreetmap.org/{z}/{x}/{y}.png)
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                isTilesScaledToDpi = true
                maxZoomLevel = 20.0
                minZoomLevel = 4.0
                controller.setZoom(17.0)
                controller.setCenter(GeoPoint(lat, lon))
                onMapViewReady(this)
            }
        },
        update = { mapView ->
            mapView.overlays.clear()

            // Dark Mode color filter for night driving if enabled
            if (useDarkStyle) {
                val matrix = ColorMatrix(
                    floatArrayOf(
                        -0.8f, 0f, 0f, 0f, 240f,
                        0f, -0.8f, 0f, 0f, 240f,
                        0f, 0f, -0.8f, 0f, 240f,
                        0f, 0f, 0f, 1.0f, 0f
                    )
                )
                mapView.overlayManager.tilesOverlay.setColorFilter(ColorMatrixColorFilter(matrix))
            } else {
                mapView.overlayManager.tilesOverlay.setColorFilter(null)
            }

            val currentGeoPoint = GeoPoint(lat, lon)

            // Animate to current position smoothly
            if (tripState.latitude != null && tripState.longitude != null) {
                mapView.controller.animateTo(currentGeoPoint)
            }

            // Draw route polyline
            if (routePoints.size >= 2) {
                val polyline = Polyline(mapView).apply {
                    outlinePaint.color = android.graphics.Color.parseColor("#DC2626")
                    outlinePaint.strokeWidth = 12f
                    outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                    outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
                    setPoints(routePoints.map { GeoPoint(it.first, it.second) })
                }
                mapView.overlays.add(polyline)

                // Trip start flag marker
                val startPoint = GeoPoint(routePoints.first().first, routePoints.first().second)
                val startMarker = Marker(mapView).apply {
                    position = startPoint
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    icon = ContextCompat.getDrawable(mapView.context, R.drawable.ic_start_flag)
                    title = "Trip Start Location"
                }
                mapView.overlays.add(startMarker)
            }

            // Taxi Car Marker at live GPS coordinates
            val taxiMarker = Marker(mapView).apply {
                position = currentGeoPoint
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = ContextCompat.getDrawable(mapView.context, R.drawable.ic_taxi_marker)
                title = "Taxi (%.4f, %.4f)".format(Locale.US, lat, lon)
            }
            mapView.overlays.add(taxiMarker)

            mapView.invalidate()
        },
        modifier = modifier.testTag("osm_trip_map_view")
    )
}

/**
 * Fallback / Ultra-Lightweight GPS Vector Route Canvas
 * Renders purely in Compose Canvas without network tile downloads.
 */
@Composable
private fun GpsVectorRouteCanvas(
    tripState: TripState,
    modifier: Modifier = Modifier
) {
    val route = tripState.routePoints

    Canvas(modifier = modifier.background(Color(0xFF0F172A))) {
        val width = size.width
        val height = size.height

        if (width <= 0f || width.isInfinite() || height <= 0f || height.isInfinite()) return@Canvas

        val gridStep = kotlin.math.max(40.dp.toPx(), 10f)
        var x = 0f
        var maxIterationsX = 2000
        while (x < width && maxIterationsX > 0) {
            drawLine(
                color = Color(0xFF1E293B),
                start = Offset(x, 0f),
                end = Offset(x, height),
                strokeWidth = 1f
            )
            x += gridStep
            maxIterationsX--
        }
        var y = 0f
        var maxIterationsY = 2000
        while (y < height && maxIterationsY > 0) {
            drawLine(
                color = Color(0xFF1E293B),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
            y += gridStep
            maxIterationsY--
        }

        if (route.isEmpty()) {
            val centerX = width / 2f
            val centerY = height / 2f
            drawCircle(
                color = Color(0xFF38BDF8).copy(alpha = 0.2f),
                radius = 36.dp.toPx(),
                center = Offset(centerX, centerY)
            )
            drawCircle(
                color = Color(0xFF38BDF8),
                radius = 8.dp.toPx(),
                center = Offset(centerX, centerY)
            )
            return@Canvas
        }

        val lats = route.map { it.first }
        val lons = route.map { it.second }
        val minLat = lats.minOrNull() ?: 0.0
        val maxLat = lats.maxOrNull() ?: 0.0
        val minLon = lons.minOrNull() ?: 0.0
        val maxLon = lons.maxOrNull() ?: 0.0

        val latRange = (maxLat - minLat).coerceAtLeast(0.0001)
        val lonRange = (maxLon - minLon).coerceAtLeast(0.0001)

        val padding = 48.dp.toPx()
        val usableW = width - (padding * 2)
        val usableH = height - (padding * 2)

        fun mapToScreen(lat: Double, lon: Double): Offset {
            val normX = ((lon - minLon) / lonRange).toFloat()
            val normY = (1.0f - ((lat - minLat) / latRange)).toFloat()
            return Offset(
                x = padding + (normX * usableW),
                y = padding + (normY * usableH)
            )
        }

        val path = Path()
        val firstPoint = mapToScreen(route.first().first, route.first().second)
        path.moveTo(firstPoint.x, firstPoint.y)

        for (i in 1 until route.size) {
            val pt = mapToScreen(route[i].first, route[i].second)
            path.lineTo(pt.x, pt.y)
        }

        drawPath(
            path = path,
            color = Color(0xFFEF4444),
            style = Stroke(
                width = 8f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        val startScreen = mapToScreen(route.first().first, route.first().second)
        drawCircle(
            color = Color(0xFF22C55E),
            radius = 10.dp.toPx(),
            center = startScreen
        )
        drawCircle(
            color = Color.White,
            radius = 4.dp.toPx(),
            center = startScreen
        )

        val currentScreen = mapToScreen(route.last().first, route.last().second)
        drawCircle(
            color = Color(0xFFEF4444).copy(alpha = 0.3f),
            radius = 18.dp.toPx(),
            center = currentScreen
        )
        drawCircle(
            color = Color(0xFFEF4444),
            radius = 10.dp.toPx(),
            center = currentScreen
        )
        drawCircle(
            color = Color.White,
            radius = 4.dp.toPx(),
            center = currentScreen
        )
    }
}
