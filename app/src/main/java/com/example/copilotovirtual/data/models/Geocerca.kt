package com.example.copilotovirtual.data.models

import com.google.android.gms.maps.model.LatLng

data class Geocerca(
    val id: String,
    val name: String,
    val polygon: List<LatLng>,
    val speedLimits: SpeedLimits? = null
)

data class SpeedLimits(
    val liviano: Int,
    val personal: Int,
    val vacio: Int,
    val cargado: Int,
    val matpel: Int
)