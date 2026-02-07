package com.mapuia.khawchinthlirna.data.model

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

@IgnoreExtraProperties
data class ImergDoc(
    @get:PropertyName("grid_id")
    @set:PropertyName("grid_id")
    var gridId: String? = null,
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    @get:PropertyName("imerg_time")
    @set:PropertyName("imerg_time")
    var imergTime: String? = null,
    @get:PropertyName("precip_rate_mm_hr")
    @set:PropertyName("precip_rate_mm_hr")
    var precipRateMmHr: Double? = null,
    @get:PropertyName("precip_30min_mm")
    @set:PropertyName("precip_30min_mm")
    var precip30MinMm: Double? = null,
    val source: String? = null,
    val generated: String? = null,
)

@IgnoreExtraProperties
data class ForecastSnapshot(
    @get:PropertyName("grid_id")
    @set:PropertyName("grid_id")
    var gridId: String? = null,
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val generated: String? = null,
    @get:PropertyName("run_id")
    @set:PropertyName("run_id")
    var runId: String? = null,
    @get:PropertyName("run_time")
    @set:PropertyName("run_time")
    var runTime: Any? = null,
    val times: List<String> = emptyList(),
    @get:PropertyName("precip_mm")
    @set:PropertyName("precip_mm")
    var precipMm: List<Double?> = emptyList(),
    @get:PropertyName("models_used")
    @set:PropertyName("models_used")
    var modelsUsed: List<String> = emptyList(),
)
