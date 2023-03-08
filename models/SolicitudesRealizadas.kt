package com.carlosvicente.uberkotlin.models

import com.beust.klaxon.Klaxon
import java.util.*

private val klaxon = Klaxon()

data class SolicitudesRealizadas (
    val id: String? = null,
    val idClient: String? = null,
    val idDriver: String? = null,
    val destination: String? = null,
    val origin: String? = null,
    val time: Double? = null,
    val km: Double? = null,
    val name: String? = null,
    val lastname: String? = null,
    val email: String? = null,
    val phone: String? = null,
    var image: String? = null,
    var token: String? = null,
    var fecha: Date? = null,
) {


    public fun toJson() = klaxon.toJsonString(this)

    companion object {
        public fun fromJson(json: String) = klaxon.parse<Client>(json)
    }
}