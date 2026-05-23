package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import com.squareup.moshi.JsonReader

@JsonClass(generateAdapter = true)
data class PocketBaseResponse<T>(
    @Json(name = "items") val items: List<T> = emptyList()
)

@JsonClass(generateAdapter = true)
data class Team(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String? = null,
    @Json(name = "crest_bg") val crest_bg: String? = null,
    @Json(name = "bg") val bg: String? = null,
    @Json(name = "crest_text") val crest_text: String? = null,
    @Json(name = "banner") val banner: String? = null,
    @Json(name = "cover_pic") val cover_pic: String? = null,
    @Json(name = "manager_name") val manager_name: String? = null,
    @Json(name = "owner_name") val owner_name: String? = null,
    @Json(name = "collectionId") val collectionId: String,
    @Json(name = "short_name") val short_name: String? = null
) {
    val displayName: String
        get() {
            val ct = crest_text?.trim()
            if (!ct.isNullOrEmpty()) return ct
            val sn = short_name?.trim()
            if (!sn.isNullOrEmpty()) return sn
            return "Unnamed Team"
        }

    val tableDisplayName: String
        get() {
            val sn = short_name?.trim()
            if (!sn.isNullOrEmpty()) return sn
            val ct = crest_text?.trim()
            if (!ct.isNullOrEmpty()) return ct
            return "Unnamed Team"
        }

    val teamBgColorHex: String?
        get() = crest_bg ?: bg
}

@JsonClass(generateAdapter = true)
data class Player(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String?,
    @Json(name = "photo") val photo: String?,
    @Json(name = "team") val team: List<String> = emptyList(),
    @Json(name = "category") val category: String?,
    @Json(name = "Catagory") val catagory: String?,
    @Json(name = "position") val position: String?,
    @Json(name = "batch") val batch: String?,
    @Json(name = "experience") val experience: String?,
    @Json(name = "collectionId") val collectionId: String
) {
    val mergedCategory: String
        get() = (category ?: catagory ?: "All").trim()
}

@JsonClass(generateAdapter = true)
data class Fixture(
    @Json(name = "id") val id: String,
    @Json(name = "team1") val team1: List<String> = emptyList(),
    @Json(name = "team2") val team2: List<String> = emptyList(),
    @Json(name = "match_type") val match_type: String?,
    @Json(name = "match_date") val match_date: String?,
    @Json(name = "collectionId") val collectionId: String = ""
)

@JsonClass(generateAdapter = true)
data class Result(
    @Json(name = "id") val id: String,
    @Json(name = "fixture") val fixture: List<String> = emptyList(),
    @Json(name = "team1_score") val team1_score: Double?,
    @Json(name = "t1_score") val t1_score: Double?,
    @Json(name = "team2_score") val team2_score: Double?,
    @Json(name = "t2_score") val t2_score: Double?,
    @Json(name = "motm") val motm: List<String> = emptyList(),
    @Json(name = "collectionId") val collectionId: String = ""
) {
    val score1: Int
        get() = (team1_score ?: t1_score ?: 0.0).toInt()

    val score2: Int
        get() = (team2_score ?: t2_score ?: 0.0).toInt()
}

@JsonClass(generateAdapter = true)
data class Supporter(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String?,
    @Json(name = "photo") val photo: String?,
    @Json(name = "batch") val batch: String?,
    @Json(name = "message") val message: String?,
    @Json(name = "supported_team") val supported_team: List<String> = emptyList(),
    @Json(name = "collectionId") val collectionId: String = ""
)

data class StandingRow(
    val team: Team,
    var played: Int = 0,
    var won: Int = 0,
    var drawn: Int = 0,
    var lost: Int = 0,
    var gf: Int = 0,
    var ga: Int = 0,
    var gd: Int = 0,
    var pts: Int = 0,
    var playedGroupQuantity: Int = 0
)

class PocketBaseRelationAdapter {
    @FromJson
    fun fromJson(reader: JsonReader): List<String> {
        val result = mutableListOf<String>()
        when (reader.peek()) {
            JsonReader.Token.BEGIN_ARRAY -> {
                reader.beginArray()
                while (reader.hasNext()) {
                    if (reader.peek() == JsonReader.Token.STRING) {
                        result.add(reader.nextString())
                    } else {
                        reader.skipValue()
                    }
                }
                reader.endArray()
            }
            JsonReader.Token.STRING -> {
                result.add(reader.nextString())
            }
            JsonReader.Token.NULL -> {
                reader.nextNull<Unit>()
            }
            else -> reader.skipValue()
        }
        return result
    }

    @ToJson
    fun toJson(value: List<String>?): String? {
        return value?.joinToString(",")
    }
}
