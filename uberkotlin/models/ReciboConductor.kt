package com.carlosvicente.uberkotlin.models

import com.beust.klaxon.*
import java.util.*

private val klaxon = Klaxon()

data class ReciboConductor (

    var id: String? = null,
    var nro: String? = null,
    val idClient: String? = null,
    val idDriver: String? = null,
    val montoBs: Double? = null,
    val montoDollar: Double? = null,
    val fechaPago: String? = null,
    val tazaCambiaria: Double? = null,
    val timestamp: Long? = null,
    val date: Date?= null,
    val tipoPago: String?= null,
    val destino: String?= null,
    val origen: String?= null,
    val verificado: Boolean? = false
) {
    public fun toJson() = klaxon.toJsonString(this)

    companion object {
        public fun fromJson(json: String) = klaxon.parse<PagoMovil>(json)
    }
}
