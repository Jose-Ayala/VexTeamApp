package com.jayala.vexapp

data class TeamResponse(
    val data: List<TeamData>
)

data class TeamData(
    val id: Int,
    val number: String,
    val team_name: String,
    val organization: String,
    val location: LocationData,
    val program: ProgramData,
    val grade: String?
)

data class LocationData(
    val city: String,
    val region: String,
    val country: String
)

data class ProgramData(
    val id: Int,
    val name: String,
    val code: String
)