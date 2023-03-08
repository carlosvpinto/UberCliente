package com.carlosvicente.uberkotlin.models


import com.beust.klaxon.*
import java.time.Instant
import java.util.*

private val klaxon = Klaxon()

data class Booking (
    val id: String? = null,
    val activo: Boolean = true,
    val idClient: String? = null,
    val idDriver: String? = null,
    val origin: String? = null,
    val destination: String? = null,
    val status: String? = null,
    val time: Double? = null,
    val km: Double? = null,
    val originLat: Double? = null,
    val originLng: Double? = null,
    val destinationLat: Double? = null,
    val destinationLng: Double? = null,
    val price: Double? = null,
    val date: Date? = null,

) {
    public fun toJson() = klaxon.toJsonString(this)

    companion object {
        public fun fromJson(json: String) = klaxon.parse<Booking>(json)
    }
}
