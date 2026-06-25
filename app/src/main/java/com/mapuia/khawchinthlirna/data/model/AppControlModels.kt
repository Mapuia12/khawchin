package com.mapuia.khawchinthlirna.data.model

data class AppAnnouncement(
    val enabled: Boolean = false,
    val id: String = "",
    val severity: String = "info",
    val titleMz: String? = null,
    val bodyMz: String? = null,
    val titleEn: String? = null,
    val bodyEn: String? = null,
    val startAt: String? = null,
    val endAt: String? = null,
    val minVersionCode: Int? = null,
    val maxVersionCode: Int? = null,
    val dismissible: Boolean = true,
    val actionLabelMz: String? = null,
    val actionLabelEn: String? = null,
    val actionUrl: String? = null,
) {
    fun title(isMizo: Boolean): String =
        if (isMizo) titleMz.orEmpty().ifBlank { titleEn.orEmpty() }
        else titleEn.orEmpty().ifBlank { titleMz.orEmpty() }

    fun body(isMizo: Boolean): String =
        if (isMizo) bodyMz.orEmpty().ifBlank { bodyEn.orEmpty() }
        else bodyEn.orEmpty().ifBlank { bodyMz.orEmpty() }

    fun actionLabel(isMizo: Boolean): String =
        if (isMizo) actionLabelMz.orEmpty().ifBlank { actionLabelEn.orEmpty() }
        else actionLabelEn.orEmpty().ifBlank { actionLabelMz.orEmpty() }
}

data class AppStatus(
    val serviceOk: Boolean = true,
    val maintenance: Boolean = false,
    val messageMz: String? = null,
    val messageEn: String? = null,
    val forecastSource: String? = null,
    val latestVersionCode: Int? = null,
    val minSupportedVersionCode: Int? = null,
    val forecastUrl: String? = null,
    val currentUrl: String? = null,
)
