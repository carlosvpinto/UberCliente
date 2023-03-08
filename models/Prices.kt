package com.carlosvicente.uberkotlin.models

import com.beust.klaxon.*

private val klaxon = Klaxon()

data class Prices (
    val CMediaCarro: Double? = null,
    val CcortaCarro: Double? = null,
    val CcortaMoto: Double? = null,
    val CmediaMoto: Double? = null,
    val km: Double? = null,
    val kmCarro: Double? = null,
    val kmMoto: Double? = null,
    val min: Double? = null,
    val minValue: Double? = null,
    val difference: Double? = null
) {
    public fun toJson() = klaxon.toJsonString(this)

    companion object {
        public fun fromJson(json: String) = klaxon.parse<Prices>(json)
    }
}
