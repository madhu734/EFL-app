package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonQualifier
import com.squareup.moshi.Moshi

@JsonQualifier
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
annotation class FlexibleSquad

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
    @Json(name = "collectionId") val collectionId: String,
    @Json(name = "efl_price") val efl_price: Double? = null,
    @Json(name = "eflPrice") val eflPrice: Double? = null,
    @Json(name = "price") val price: Double? = null
) {
    val mergedCategory: String
        get() = (category ?: catagory ?: "All").trim()

    val fplPrice: Double
        get() = efl_price ?: eflPrice ?: price ?: 7.0

    var computedPoints: Int = 0
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
    @Json(name = "potm") val potm: List<String> = emptyList(),
    @Json(name = "collectionId") val collectionId: String = "",

    @Json(name = "t1_goal_1") val t1_goal_1: List<String> = emptyList(),
    @Json(name = "t1_goal_2") val t1_goal_2: List<String> = emptyList(),
    @Json(name = "t1_goal_3") val t1_goal_3: List<String> = emptyList(),
    @Json(name = "t1_goal_4") val t1_goal_4: List<String> = emptyList(),
    @Json(name = "t1_goal_5") val t1_goal_5: List<String> = emptyList(),
    @Json(name = "t1_goal_6") val t1_goal_6: List<String> = emptyList(),
    @Json(name = "t1_goal_7") val t1_goal_7: List<String> = emptyList(),

    @Json(name = "t2_goal_1") val t2_goal_1: List<String> = emptyList(),
    @Json(name = "t2_goal_2") val t2_goal_2: List<String> = emptyList(),
    @Json(name = "t2_goal_3") val t2_goal_3: List<String> = emptyList(),
    @Json(name = "t2_goal_4") val t2_goal_4: List<String> = emptyList(),
    @Json(name = "t2_goal_5") val t2_goal_5: List<String> = emptyList(),
    @Json(name = "t2_goal_6") val t2_goal_6: List<String> = emptyList(),
    @Json(name = "t2_goal_7") val t2_goal_7: List<String> = emptyList(),

    @Json(name = "t1_assist_1") val t1_assist_1: List<String> = emptyList(),
    @Json(name = "t1_assist_2") val t1_assist_2: List<String> = emptyList(),
    @Json(name = "t1_assist_3") val t1_assist_3: List<String> = emptyList(),
    @Json(name = "t1_assist_4") val t1_assist_4: List<String> = emptyList(),
    @Json(name = "t1_assist_5") val t1_assist_5: List<String> = emptyList(),
    @Json(name = "t1_assist_6") val t1_assist_6: List<String> = emptyList(),
    @Json(name = "t1_assist_7") val t1_assist_7: List<String> = emptyList(),

    @Json(name = "t2_assist_1") val t2_assist_1: List<String> = emptyList(),
    @Json(name = "t2_assist_2") val t2_assist_2: List<String> = emptyList(),
    @Json(name = "t2_assist_3") val t2_assist_3: List<String> = emptyList(),
    @Json(name = "t2_assist_4") val t2_assist_4: List<String> = emptyList(),
    @Json(name = "t2_assist_5") val t2_assist_5: List<String> = emptyList(),
    @Json(name = "t2_assist_6") val t2_assist_6: List<String> = emptyList(),
    @Json(name = "t2_assist_7") val t2_assist_7: List<String> = emptyList(),

    @Json(name = "t1_yellow_1") val t1_yellow_1: List<String> = emptyList(),
    @Json(name = "t1_yellow_2") val t1_yellow_2: List<String> = emptyList(),
    @Json(name = "t1_yellow_3") val t1_yellow_3: List<String> = emptyList(),
    @Json(name = "t1_yellow_4") val t1_yellow_4: List<String> = emptyList(),

    @Json(name = "t2_yellow_1") val t2_yellow_1: List<String> = emptyList(),
    @Json(name = "t2_yellow_2") val t2_yellow_2: List<String> = emptyList(),
    @Json(name = "t2_yellow_3") val t2_yellow_3: List<String> = emptyList(),
    @Json(name = "t2_yellow_4") val t2_yellow_4: List<String> = emptyList(),

    @Json(name = "t1_red_1") val t1_red_1: List<String> = emptyList(),
    @Json(name = "t1_red_2") val t1_red_2: List<String> = emptyList(),

    @Json(name = "t2_red_1") val t2_red_1: List<String> = emptyList(),
    @Json(name = "t2_red_2") val t2_red_2: List<String> = emptyList()
) {
    val score1: Int
        get() = (team1_score ?: t1_score ?: 0.0).toInt()

    val score2: Int
        get() = (team2_score ?: t2_score ?: 0.0).toInt()

    fun getEventField(field: String): List<String> {
        return when (field) {
            "t1_goal_1" -> t1_goal_1
            "t1_goal_2" -> t1_goal_2
            "t1_goal_3" -> t1_goal_3
            "t1_goal_4" -> t1_goal_4
            "t1_goal_5" -> t1_goal_5
            "t1_goal_6" -> t1_goal_6
            "t1_goal_7" -> t1_goal_7
            "t2_goal_1" -> t2_goal_1
            "t2_goal_2" -> t2_goal_2
            "t2_goal_3" -> t2_goal_3
            "t2_goal_4" -> t2_goal_4
            "t2_goal_5" -> t2_goal_5
            "t2_goal_6" -> t2_goal_6
            "t2_goal_7" -> t2_goal_7
            "t1_assist_1" -> t1_assist_1
            "t1_assist_2" -> t1_assist_2
            "t1_assist_3" -> t1_assist_3
            "t1_assist_4" -> t1_assist_4
            "t1_assist_5" -> t1_assist_5
            "t1_assist_6" -> t1_assist_6
            "t1_assist_7" -> t1_assist_7
            "t2_assist_1" -> t2_assist_1
            "t2_assist_2" -> t2_assist_2
            "t2_assist_3" -> t2_assist_3
            "t2_assist_4" -> t2_assist_4
            "t2_assist_5" -> t2_assist_5
            "t2_assist_6" -> t2_assist_6
            "t2_assist_7" -> t2_assist_7
            "t1_yellow_1" -> t1_yellow_1
            "t1_yellow_2" -> t1_yellow_2
            "t1_yellow_3" -> t1_yellow_3
            "t1_yellow_4" -> t1_yellow_4
            "t2_yellow_1" -> t2_yellow_1
            "t2_yellow_2" -> t2_yellow_2
            "t2_yellow_3" -> t2_yellow_3
            "t2_yellow_4" -> t2_yellow_4
            "t1_red_1" -> t1_red_1
            "t1_red_2" -> t1_red_2
            "t2_red_1" -> t2_red_1
            "t2_red_2" -> t2_red_2
            else -> emptyList()
        }
    }
}

@JsonClass(generateAdapter = true)
data class SystemSetting(
    @Json(name = "id") val id: String,
    @Json(name = "deadline") val deadline: String? = null,
    @Json(name = "active_matchday") val active_matchday: String? = null,
    @Json(name = "snapshot_taken") val snapshot_taken: Boolean? = null,
    @Json(name = "enable_countdown") val enable_countdown: Any? = null,
    @Json(name = "force_lock") val force_lock: Any? = null
)

data class FplUser(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String? = null,
    @Json(name = "batch") val batch: String? = null,
    @Json(name = "email") val email: String? = null,
    @Json(name = "avatar") val avatar: String? = null,
    @Json(name = "squad") val squad: String? = null,
    @Json(name = "squad_md1") val squad_md1: String? = null,
    @Json(name = "squad_md2") val squad_md2: String? = null,
    @Json(name = "squad_md3") val squad_md3: String? = null,
    @Json(name = "squad_md4") val squad_md4: String? = null,
    @Json(name = "squad_md5") val squad_md5: String? = null,
    @Json(name = "collectionId") val collectionId: String = ""
)

@JsonClass(generateAdapter = true)
data class FplMatchData(
    @Json(name = "id") val id: String,
    @Json(name = "fixture") val fixture: List<String> = emptyList(),
    @Json(name = "players_under_15_mins") val players_under_15_mins: List<String> = emptyList(),
    @Json(name = "did_not_play") val did_not_play: List<String> = emptyList(),
    @Json(name = "t1_saves") val t1_saves: Int? = null,
    @Json(name = "t2_saves") val t2_saves: Int? = null,
    @Json(name = "second_best") val second_best: List<String> = emptyList(),
    @Json(name = "third_best") val third_best: List<String> = emptyList(),
    @Json(name = "collectionId") val collectionId: String = "",
    
    @Json(name = "t1_pen_earned_1") val t1_pen_earned_1: List<String> = emptyList(),
    @Json(name = "t1_pen_earned_2") val t1_pen_earned_2: List<String> = emptyList(),
    @Json(name = "t2_pen_earned_1") val t2_pen_earned_1: List<String> = emptyList(),
    @Json(name = "t2_pen_earned_2") val t2_pen_earned_2: List<String> = emptyList(),

    @Json(name = "t1_pen_taker_1") val t1_pen_taker_1: List<String> = emptyList(),
    @Json(name = "t1_pen_taker_2") val t1_pen_taker_2: List<String> = emptyList(),
    @Json(name = "t2_pen_taker_1") val t2_pen_taker_1: List<String> = emptyList(),
    @Json(name = "t2_pen_taker_2") val t2_pen_taker_2: List<String> = emptyList(),

    @Json(name = "t1_pen_scored_1") val t1_pen_scored_1: Boolean? = null,
    @Json(name = "t1_pen_scored_2") val t1_pen_scored_2: Boolean? = null,
    @Json(name = "t2_pen_scored_1") val t2_pen_scored_1: Boolean? = null,
    @Json(name = "t2_pen_scored_2") val t2_pen_scored_2: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class FplAuthResponse(
    @Json(name = "token") val token: String,
    @Json(name = "record") val record: FplUser
)

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

class FplUserAdapter {
    @FromJson
    fun fromJson(reader: JsonReader): FplUser {
        var id = ""
        var name: String? = null
        var batch: String? = null
        var email: String? = null
        var avatar: String? = null
        var squad: String? = null
        var squad_md1: String? = null
        var squad_md2: String? = null
        var squad_md3: String? = null
        var squad_md4: String? = null
        var squad_md5: String? = null
        var collectionId = ""

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "id" -> id = reader.nextString()
                "name" -> name = if (reader.peek() == JsonReader.Token.NULL) { reader.nextNull<java.lang.Void>(); null } else reader.nextString()
                "batch" -> batch = if (reader.peek() == JsonReader.Token.NULL) { reader.nextNull<java.lang.Void>(); null } else reader.nextString()
                "email" -> email = if (reader.peek() == JsonReader.Token.NULL) { reader.nextNull<java.lang.Void>(); null } else reader.nextString()
                "avatar" -> avatar = if (reader.peek() == JsonReader.Token.NULL) { reader.nextNull<java.lang.Void>(); null } else reader.nextString()
                "squad" -> squad = parseFlexibleSquadValue(reader)
                "squad_md1" -> squad_md1 = parseFlexibleSquadValue(reader)
                "squad_md2" -> squad_md2 = parseFlexibleSquadValue(reader)
                "squad_md3" -> squad_md3 = parseFlexibleSquadValue(reader)
                "squad_md4" -> squad_md4 = parseFlexibleSquadValue(reader)
                "squad_md5" -> squad_md5 = parseFlexibleSquadValue(reader)
                "collectionId" -> collectionId = reader.nextString()
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        return FplUser(
            id = id,
            name = name,
            batch = batch,
            email = email,
            avatar = avatar,
            squad = squad,
            squad_md1 = squad_md1,
            squad_md2 = squad_md2,
            squad_md3 = squad_md3,
            squad_md4 = squad_md4,
            squad_md5 = squad_md5,
            collectionId = collectionId
        )
    }

    private fun parseFlexibleSquadValue(reader: JsonReader): String? {
        val peek = reader.peek()
        if (peek == JsonReader.Token.NULL) {
            reader.nextNull<java.lang.Void>()
            return null
        }
        if (peek == JsonReader.Token.BEGIN_OBJECT || peek == JsonReader.Token.BEGIN_ARRAY) {
            val value = reader.readJsonValue()
            return try {
                val subMoshi = Moshi.Builder().addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build()
                subMoshi.adapter(Any::class.java).toJson(value)
            } catch (e: Exception) {
                value?.toString()
            }
        }
        if (peek == JsonReader.Token.STRING) {
            return reader.nextString()
        }
        val value = reader.readJsonValue()
        return value?.toString()
    }

    @ToJson
    fun toJson(writer: com.squareup.moshi.JsonWriter, value: FplUser?) {
        if (value == null) {
            writer.nullValue()
            return
        }
        writer.beginObject()
        writer.name("id").value(value.id)
        writer.name("name").value(value.name)
        writer.name("batch").value(value.batch)
        writer.name("email").value(value.email)
        writer.name("avatar").value(value.avatar)

        writeFlexibleSquad(writer, "squad", value.squad)
        writeFlexibleSquad(writer, "squad_md1", value.squad_md1)
        writeFlexibleSquad(writer, "squad_md2", value.squad_md2)
        writeFlexibleSquad(writer, "squad_md3", value.squad_md3)
        writeFlexibleSquad(writer, "squad_md4", value.squad_md4)
        writeFlexibleSquad(writer, "squad_md5", value.squad_md5)

        writer.name("collectionId").value(value.collectionId)
        writer.endObject()
    }

    private fun writeFlexibleSquad(writer: com.squareup.moshi.JsonWriter, name: String, value: String?) {
        writer.name(name)
        if (value == null) {
            writer.nullValue()
            return
        }
        val trimmed = value.trim()
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            try {
                val subMoshi = Moshi.Builder().addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build()
                val parsedObj = subMoshi.adapter(Any::class.java).fromJson(value)
                val str = subMoshi.adapter(Any::class.java).toJson(parsedObj)
                writer.value(str)
                return
            } catch (e: Exception) {
                // fall through to string value
            }
        }
        writer.value(value)
    }
}
