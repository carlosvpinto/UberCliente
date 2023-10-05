package com.carlosvicente.uberkotlin.models

import com.beust.klaxon.Klaxon
import com.google.android.gms.maps.model.LatLng

private val klaxon = Klaxon()

data class Client(
    val id: String? = null,
    val name: String? = null,
    val lastname: String? = null,
    val email: String? = null,
    val phone: String? = null,
    var image: String? = null,
    var token: String? = null,
    var direcionFrecu1: String? = null,
    var latFrecuente1: Double? = null,
    var lngFrecuente1: Double? = null,
    var direcionFrecu2: String? = null,
    var latFrecuente2: Double? = null,
    var lngFrecuente2: Double? = null,
    var billetera: Double? = null
) {

    fun toJson(): String = klaxon.toJsonString(this)

    companion object {
        fun fromJson(json: String): Client? = klaxon.parse<Client>(json)
    }
}
