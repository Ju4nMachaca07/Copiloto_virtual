// data/datasources/Converters.kt
package com.example.copilotovirtual.data.datasources

import androidx.room.TypeConverter
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromLatLngList(value: List<LatLng>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toLatLngList(value: String): List<LatLng> {
        val listType = object : TypeToken<List<LatLng>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromRouteInstructionList(value: List<RouteInstruction>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toRouteInstructionList(value: String): List<RouteInstruction> {
        val listType = object : TypeToken<List<RouteInstruction>>() {}.type
        return gson.fromJson(value, listType)
    }
}

// Modelo para instrucciones
data class RouteInstruction(
    val text: String,
    val spokenText: String,
    val distance: Double,
    val duration: Long,
    val maneuver: String,
    val latitude: Double,
    val longitude: Double,
    val streetName: String? = null
)