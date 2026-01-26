package com.jayala.vexapp
import com.google.gson.annotations.SerializedName

// Models for fetching seasons
data class SeasonsResponse(
    @SerializedName("data") val data: List<Season>
)

data class Season(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("program") val program: ProgramRef
)

data class ProgramRef(
    @SerializedName("id") val id: Int
)

// Models for the event dropdown
data class CompEventResponse(
    @SerializedName("data") val data: List<CompEventDetail>
)

// The model for each event in the dropdown
data class CompEventDetail(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("start") val start: String? = null,
    @SerializedName("program") val program: ProgramRef,
    @SerializedName("season") val season: SeasonRef? = null
)

data class SeasonRef(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String? = null
)

// Models for fetching event details (skills, rank, awards)
data class CompSkillsResponse(
    @SerializedName("data") val data: List<CompSkillData>
)
data class CompSkillData(
    @SerializedName("type") val type: String,
    @SerializedName("score") val score: Int,
    @SerializedName("rank") val rank: Int,
    @SerializedName("event") val event: CompEventRef?
)

data class CompRankingsResponse(
    @SerializedName("data") val data: List<CompRankingData>
)
data class CompRankingData(
    @SerializedName("rank") val rank: Int,
    @SerializedName("wins") val wins: Int,
    @SerializedName("losses") val losses: Int,
    @SerializedName("ties") val ties: Int,
    @SerializedName("event") val event: CompEventRef?
)

data class CompAwardResponse(
    @SerializedName("data") val data: List<CompAwardData>
)
data class CompAwardData(
    @SerializedName("title") val title: String,
    @SerializedName("event") val event: CompEventRef?
)


data class CompEventRef(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String? = null,
    @SerializedName("program") val program: ProgramRef? = null
)

