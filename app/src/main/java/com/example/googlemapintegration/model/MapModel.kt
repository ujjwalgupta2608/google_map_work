package com.example.googlemapintegration.model

import com.google.gson.annotations.SerializedName

data class DirectionsResponse(
    @SerializedName("routes") val routes: List<Route>
)

data class Route(
    @SerializedName("overview_polyline") val overview_polyline: OverviewPolyline,
    @SerializedName("legs") val legs: List<Leg>
)

data class OverviewPolyline(
    @SerializedName("points") val points: String
)

data class Leg(
    @SerializedName("distance") val distance: Distance,
    @SerializedName("duration") val duration: Duration
)

data class Distance(
    @SerializedName("value") val value: Int // Distance in meters
)

data class Duration(
    @SerializedName("value") val value: Int // Duration in seconds
)


