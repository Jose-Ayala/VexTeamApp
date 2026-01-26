package com.jayala.vexapp

import com.google.gson.annotations.SerializedName

data class SeasonResponse(
    @SerializedName("data") val data: List<SeasonDetail>
)

data class SeasonDetail(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("program") val program: ProgramData
)