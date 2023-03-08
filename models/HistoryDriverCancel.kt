package com.carlosvicente.uberkotlin.models

import com.beust.klaxon.*
import java.util.*

private val klaxon = Klaxon()

data class HistoryDriverCancel (
    var id: String? = null,
    val idClient: String? = null,
    val idDriver: String? = null,
    val origin: String? = null,
    val destination: String? = null,
    val km: Double? = null,
    val originLat: Double? = null,
    val originLng: Double? = null,
    val destinationLat: Double? = null,
    val destinationLng: Double? = null,
    val price: Double? = null,
    val timestamp: Long? = null,
    val causa: String? = null,
    val fecha: Date? = null,
) {
    public fun toJson() = klaxon.toJsonString(this)

    companion object {
        public fun fromJson(json: String) = klaxon.parse<Booking>(json)
    }
}
