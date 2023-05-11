package com.carlosvicente.uberkotlin.models

import com.beust.klaxon.*
import java.util.*

private val klaxon = Klaxon()

data class PagoMovil (

    var id: String? = null,
    var nro: String? = null,
    val idClient: String? = null,
    val montoBs: Double? = null,
    val montoDollar: Double? = null,
    val fechaPago: String? = null,
    val tazaCambiaria: Double? = null,
    val timestamp: Long? = null,
    val date: Date?= null,
    val verificado: Boolean? = false
) {
    public fun toJson() = klaxon.toJsonString(this)

    companion object {
        public fun fromJson(json: String) = klaxon.parse<PagoMovil>(json)
    }
}
