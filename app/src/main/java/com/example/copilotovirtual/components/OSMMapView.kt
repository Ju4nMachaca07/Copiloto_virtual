package com.example.copilotovirtual.Components

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.util.ArrayList

@Composable
fun OSMMapView(
    modifier: Modifier = Modifier,
    context: Context,
    showPoints: Boolean = true
) {
    var mapView by remember { mutableStateOf<MapView?>(null) }

    AndroidView(
        factory = { ctx ->
            // Configurar OSMDroid
            Configuration.getInstance().load(
                ctx,
                ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
            )

            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)

                // Centrar en el área de Marcona-MinaJusta-Nazca
                controller.setCenter(LocationManager.AREA_CENTER)
                controller.setZoom(11.0)  // Zoom para ver los 3 puntos

                mapView = this
            }
        },
        modifier = modifier,
        update = { view ->
            if (showPoints) {
                // Limpiar overlays anteriores
                view.overlays.clear()

                // Añadir marcadores para cada punto
                LocationManager.ALL_POINTS.forEach { point ->
                    val marker = Marker(view).apply {
                        position = org.osmdroid.util.GeoPoint(
                            point.latitude,
                            point.longitude
                        )
                        title = point.name
                        snippet = point.description
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    }
                    view.overlays.add(marker)
                }

                // Opcional: Añadir línea conectando los puntos
                val polyline = Polyline().apply {
                    color = 0xFF2196F3.toInt()  // Azul
                    width = 5.0f
                    title = "Ruta Marcona - Mina Justa - Nazca"
                }
                view.overlays.add(polyline)

                // Forzar actualización del mapa
                view.invalidate()
            }
        }
    )
}