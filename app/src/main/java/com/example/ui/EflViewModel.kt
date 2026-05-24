package com.example.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.RetrofitClient
import com.example.data.model.Fixture
import com.example.data.model.Player
import com.example.data.model.Result
import com.example.data.model.StandingRow
import com.example.data.model.Supporter
import com.example.data.model.Team
import com.example.data.model.SystemSetting
import com.example.data.model.FplUser
import com.example.data.model.FplMatchData
import com.example.data.model.FplAuthResponse
import com.squareup.moshi.Moshi
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EflUiState(
    val isLoading: Boolean = false,
    val teams: List<Team> = emptyList(),
    val players: List<Player> = emptyList(),
    val fixtures: List<Fixture> = emptyList(),
    val results: List<Result> = emptyList(),
    val supporters: List<Supporter> = emptyList(),
    val standings: List<StandingRow> = emptyList(),
    val errorMessage: String? = null,
    
    // FPL specific fields
    val systemSettings: List<SystemSetting> = emptyList(),
    val fplUsers: List<FplUser> = emptyList(),
    val fplMatchData: List<FplMatchData> = emptyList(),
    val currentFplUser: FplUser? = null,
    val activeMatchday: String = "MD 1",
    val deadlineTime: Long = 0L,
    val snapshotTaken: Boolean = false,
    val enableCountdown: Boolean = true,
    val forceLock: Boolean = false
)

class EflViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("efl_portal_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(EflUiState())
    val uiState: StateFlow<EflUiState> = _uiState.asStateFlow()

    private val _currentSeason = MutableStateFlow(sharedPrefs.getString("activeSeason", "5") ?: "5")
    val currentSeason: StateFlow<String> = _currentSeason.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(sharedPrefs.getBoolean("isDarkTheme", false))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    // Search and filter for players
    private val _playerSearchQuery = MutableStateFlow("")
    val playerSearchQuery: StateFlow<String> = _playerSearchQuery.asStateFlow()

    private val _selectedPlayerCategory = MutableStateFlow("All")
    val selectedPlayerCategory: StateFlow<String> = _selectedPlayerCategory.asStateFlow()

    val baseUrl: String
        get() = if (_currentSeason.value == "4") "https://pbdb2.duckdns.org" else "https://efljudb.duckdns.org"

    init {
        loadData()
    }

    fun toggleTheme() {
        val newTheme = !_isDarkTheme.value
        _isDarkTheme.update { newTheme }
        sharedPrefs.edit().putBoolean("isDarkTheme", newTheme).apply()
    }

    fun switchSeason(season: String) {
        if (_currentSeason.value == season) return
        _currentSeason.value = season
        sharedPrefs.edit().putString("activeSeason", season).apply()
        // Reset state and load
        _playerSearchQuery.value = ""
        _selectedPlayerCategory.value = "All"
        loadData()
    }

    fun updateSearchQuery(query: String) {
        _playerSearchQuery.value = query
    }

    fun updateSelectedCategory(category: String) {
        _selectedPlayerCategory.value = category
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val currentBase = baseUrl
                Log.d("EflViewModel", "Fetching data from base url: $currentBase")
                
                // Construct URLs
                val teamsUrl = "$currentBase/api/collections/teams/records?perPage=100"
                val playersUrl = "$currentBase/api/collections/players/records?perPage=500"
                val fixturesUrl = "$currentBase/api/collections/fixtures/records?sort=match_date&perPage=100"
                val resultsUrl = "$currentBase/api/collections/results/records?perPage=100"
                val supportersUrl = "$currentBase/api/collections/supporters/records?sort=-created&perPage=100"
                
                // FPL URLs
                val systemSettingsUrl = "https://pbdb2.duckdns.org/api/collections/system_settings/records"
                val fplUsersUrl = "https://pbdb2.duckdns.org/api/collections/fpl_users/records?perPage=200"
                val fplMatchDataUrl = "https://efljudb.duckdns.org/api/collections/fpl_match_data/records?perPage=100"

                // Launch downloads in parallel or sequencially
                val api = RetrofitClient.apiService
                
                val teamsList = try {
                    api.getTeams(teamsUrl).items
                } catch (e: Exception) {
                    Log.e("EflViewModel", "Error fetching teams", e)
                    emptyList()
                }

                val playersList = try {
                    api.getPlayers(playersUrl).items
                } catch (e: Exception) {
                    Log.e("EflViewModel", "Error fetching players", e)
                    emptyList()
                }

                val fixturesList = try {
                    api.getFixtures(fixturesUrl).items
                } catch (e: Exception) {
                    Log.e("EflViewModel", "Error fetching fixtures", e)
                    emptyList()
                }

                val resultsList = try {
                    api.getResults(resultsUrl).items
                } catch (e: Exception) {
                    Log.e("EflViewModel", "Error fetching results", e)
                    emptyList()
                }

                val supportersList = try {
                    api.getSupporters(supportersUrl).items
                } catch (e: Exception) {
                    Log.e("EflViewModel", "Error fetching supporters", e)
                    emptyList()
                }

                val systemSettingsList = try {
                    api.getSystemSettings(systemSettingsUrl).items
                } catch (e: Exception) {
                    Log.e("EflViewModel", "Error fetching FPL system settings", e)
                    emptyList()
                }

                val fplUsersList = try {
                    api.getFplUsers(fplUsersUrl).items
                } catch (e: Exception) {
                    Log.e("EflViewModel", "Error fetching FPL users", e)
                    emptyList()
                }

                val fplMatchDataList = try {
                    api.getFplMatchData(fplMatchDataUrl).items
                } catch (e: Exception) {
                    Log.e("EflViewModel", "Error fetching FPL match data", e)
                    emptyList()
                }

                var activeMd = "MD 1"
                var deadTime = 0L
                var snapTaken = false
                var enCountdown = true
                var fLock = false

                if (systemSettingsList.isNotEmpty()) {
                    val setting = systemSettingsList.firstOrNull { !it.deadline.isNullOrEmpty() }
                    if (setting != null) {
                        activeMd = setting.active_matchday ?: "MD 1"
                        snapTaken = setting.snapshot_taken == true

                        setting.deadline?.let { dStr ->
                            try {
                                val normalizedDate = dStr.replace(" ", "T")
                                val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
                                format.timeZone = java.util.TimeZone.getTimeZone("UTC")
                                val date = format.parse(normalizedDate)
                                if (date != null) {
                                    deadTime = date.time
                                }
                            } catch (e: Exception) {
                                Log.e("EflViewModel", "Error parsing deadline time: $dStr", e)
                            }
                        }

                        setting.enable_countdown?.let { ec ->
                            val ecString = ec.toString().lowercase().trim()
                            enCountdown = (ecString == "true" || ecString == "yes" || ecString == "1")
                        }

                        setting.force_lock?.let { fl ->
                            val flString = fl.toString().lowercase().trim()
                            fLock = (flString == "true" || flString == "yes" || flString == "1")
                        }
                    }
                }

                // Compute FPL player points
                calculateGlobalFplPoints(playersList, resultsList, fixturesList, fplMatchDataList)

                val savedUserId = sharedPrefs.getString("fpl_user_id", null)
                val currentFplUser = if (savedUserId != null) {
                    fplUsersList.firstOrNull { it.id == savedUserId }
                } else null

                // Calculate Standings Table Row
                val computedStandings = calculateStandings(teamsList, resultsList, fixturesList)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        teams = teamsList,
                        players = playersList,
                        fixtures = fixturesList,
                        results = resultsList,
                        supporters = supportersList,
                        standings = computedStandings,
                        systemSettings = systemSettingsList,
                        fplUsers = fplUsersList,
                        fplMatchData = fplMatchDataList,
                        currentFplUser = currentFplUser,
                        activeMatchday = activeMd,
                        deadlineTime = deadTime,
                        snapshotTaken = snapTaken,
                        enableCountdown = enCountdown,
                        forceLock = fLock,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                Log.e("EflViewModel", "Error aggregating EFL resources", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Unable to fetch league data. Please swipe down or check your internet connection."
                    )
                }
            }
        }
    }

    private fun calculateStandings(
        teams: List<Team>,
        results: List<Result>,
        fixtures: List<Fixture>
    ): List<StandingRow> {
        val tableMap = teams.associate { it.id to StandingRow(team = it) }.toMutableMap()

        for (res in results) {
            val fixId = res.fixture.firstOrNull() ?: continue
            val fix = fixtures.firstOrNull { f -> f.id == fixId } ?: continue

            val matchType = (fix.match_type ?: "").lowercase()
            // Ignore playoff phases in cumulative standings
            if (matchType.contains("qualifier") ||
                matchType.contains("eliminator") ||
                matchType.contains("semi") ||
                matchType.contains("final")
            ) {
                continue
            }

            val t1Id = fix.team1.firstOrNull() ?: continue
            val t2Id = fix.team2.firstOrNull() ?: continue

            val standing1 = tableMap[t1Id] ?: continue
            val standing2 = tableMap[t2Id] ?: continue

            // Dart constraint: if any team playedGroup count already reached 5, stop adding
            // (Wait, only group matches contribute up to maximum 5 per team)
            if (standing1.playedGroupQuantity >= 5 || standing2.playedGroupQuantity >= 5) {
                continue
            }

            val s1 = res.score1
            val s2 = res.score2

            standing1.played += 1
            standing1.playedGroupQuantity += 1
            standing2.played += 1
            standing2.playedGroupQuantity += 1

            standing1.gf += s1
            standing1.ga += s2
            standing2.gf += s2
            standing2.ga += s1

            if (s1 > s2) {
                standing1.won += 1
                standing1.pts += 3
                standing2.lost += 1
            } else if (s2 > s1) {
                standing2.won += 1
                standing2.pts += 3
                standing1.lost += 1
            } else {
                standing1.drawn += 1
                standing1.pts += 1
                standing2.drawn += 1
                standing2.pts += 1
            }
        }

        val rows = tableMap.values.toList()
        for (r in rows) {
            r.gd = r.gf - r.ga
        }

        // Sort rows by Pts desc, then GD desc, then GF desc
        return rows.sortedWith(
            compareByDescending<StandingRow> { it.pts }
                .thenByDescending { it.gd }
                .thenByDescending { it.gf }
        )
    }

    // FPL POINT RULES & SYSTEM MATH
    fun getPlayerMins(id: String, under15: List<String>, dnp: List<String>): Int {
        return if (dnp.contains(id)) 0
        else if (under15.contains(id)) 14
        else 90
    }

    fun countEvents(id: String, res: Result, fieldPrefix: String, maxCount: Int = 7): Int {
        var count = 0
        for (i in 1..maxCount) {
            val valList = res.getEventField("$fieldPrefix$i")
            if (valList.contains(id)) {
                count++
            }
        }
        return count
    }

    private fun checkPenMatch(valList: List<String>, id: String): Boolean {
        return valList.contains(id)
    }

    private fun isPenMissed(takerVal: List<String>, scoredVal: Boolean?): Boolean {
        return takerVal.isNotEmpty() && scoredVal == false
    }

    fun calculateGlobalFplPoints(
        players: List<Player>,
        results: List<Result>,
        fixtures: List<Fixture>,
        matchData: List<FplMatchData>
    ) {
        for (p in players) {
            p.computedPoints = 0
            val pTeamId = p.team.firstOrNull() ?: continue

            for (fix in fixtures) {
                val log = matchData.firstOrNull { l -> l.fixture.contains(fix.id) } ?: continue
                val res = results.firstOrNull { r -> r.fixture.contains(fix.id) } ?: continue

                val t1Id = fix.team1.firstOrNull() ?: continue
                val t2Id = fix.team2.firstOrNull() ?: continue
                if (pTeamId != t1Id && pTeamId != t2Id) continue

                val isT1 = pTeamId == t1Id
                val teamPrefix = if (isT1) "t1" else "t2"
                val oppPrefix = if (isT1) "t2" else "t1"
                val oppScore = if (isT1) res.score2 else res.score1

                val under15 = log.players_under_15_mins
                val dnp = log.did_not_play

                val mins = getPlayerMins(p.id, under15, dnp)
                val yellow = countEvents(p.id, res, "${teamPrefix}_yellow_", 4)
                val red = countEvents(p.id, res, "${teamPrefix}_red_", 2)

                if (mins == 0 && yellow == 0 && red == 0) continue

                var pts = 0
                val pos = p.fplPosition

                if (mins >= 15) pts += 2
                else if (mins > 0) pts += 1

                val regularGoals = countEvents(p.id, res, "${teamPrefix}_goal_")
                var penGoals = 0
                val pen1Taker = if (isT1) log.t1_pen_taker_1 else log.t2_pen_taker_1
                val pen1Scored = if (isT1) log.t1_pen_scored_1 else log.t2_pen_scored_1
                val pen2Taker = if (isT1) log.t1_pen_taker_2 else log.t2_pen_taker_2
                val pen2Scored = if (isT1) log.t1_pen_scored_2 else log.t2_pen_scored_2

                if (checkPenMatch(pen1Taker, p.id) && pen1Scored == true) penGoals++
                if (checkPenMatch(pen2Taker, p.id) && pen2Scored == true) penGoals++
                val goals = regularGoals + penGoals

                if (goals > 0) {
                    when (pos) {
                        "FWD" -> pts += goals * 4
                        "MID" -> pts += goals * 5
                        else -> pts += goals * 6
                    }
                }

                val assists = countEvents(p.id, res, "${teamPrefix}_assist_")
                if (assists > 0) pts += assists * 3

                val ownGoals = countEvents(p.id, res, "${oppPrefix}_goal_")
                if (ownGoals > 0) pts -= ownGoals * 2

                if (yellow > 0) pts -= yellow * 1
                if (red > 0) pts -= red * 3

                if (mins >= 15 && oppScore == 0) {
                    when (pos) {
                        "GK" -> pts += 4
                        "DEF" -> pts += 3
                        "MID" -> pts += 1
                    }
                }

                if (mins >= 15 && (pos == "DEF" || pos == "GK") && oppScore >= 2) {
                    pts -= oppScore / 2
                }

                var pensEarned = 0
                val pen1Earned = if (isT1) log.t1_pen_earned_1 else log.t2_pen_earned_1
                val pen2Earned = if (isT1) log.t1_pen_earned_2 else log.t2_pen_earned_2
                if (checkPenMatch(pen1Earned, p.id)) pensEarned++
                if (checkPenMatch(pen2Earned, p.id)) pensEarned++
                if (pensEarned > 0) pts += pensEarned * 3

                var pensMissed = 0
                if (checkPenMatch(pen1Taker, p.id) && pen1Scored == false) pensMissed++
                if (checkPenMatch(pen2Taker, p.id) && pen2Scored == false) pensMissed++
                if (pensMissed > 0) pts -= pensMissed * 2

                if (pos == "GK") {
                    var pensSaved = 0
                    val oppPen1Taker = if (isT1) log.t2_pen_taker_1 else log.t1_pen_taker_1
                    val oppPen1Scored = if (isT1) log.t2_pen_scored_1 else log.t1_pen_scored_1
                    val oppPen2Taker = if (isT1) log.t2_pen_taker_2 else log.t1_pen_taker_2
                    val oppPen2Scored = if (isT1) log.t2_pen_scored_2 else log.t1_pen_scored_2

                    if (isPenMissed(oppPen1Taker, oppPen1Scored)) pensSaved++
                    if (isPenMissed(oppPen2Taker, oppPen2Scored)) pensSaved++
                    if (pensSaved > 0) pts += pensSaved * 5

                    val saves = if (isT1) (log.t1_saves ?: 0) else (log.t2_saves ?: 0)
                    if (saves >= 3) pts += saves / 3
                }

                val motm = res.motm
                val potm = res.potm
                if (motm.contains(p.id) || potm.contains(p.id)) pts += 3

                val secondBest = log.second_best
                if (secondBest.contains(p.id)) pts += 2

                val thirdBest = log.third_best
                if (thirdBest.contains(p.id)) pts += 1

                p.computedPoints += pts
            }
        }
    }

    data class MatchdayPointsResult(
        val mdTotal: Int = 0,
        val mdPlayerPts: Map<String, Int> = emptyMap(),
        val mdBreakdown: Map<String, List<String>> = emptyMap()
    )

    fun getMDPoints(user: FplUser, mdName: String): MatchdayPointsResult {
        val normalizedReq = mdName.uppercase().replace(" ", "")
        val localActive = _uiState.value.activeMatchday.uppercase().replace(" ", "")
        val isDeadlinePassed = _uiState.value.forceLock || (System.currentTimeMillis() >= _uiState.value.deadlineTime)

        var squadFieldStr: String? = when(normalizedReq) {
            "MD1" -> user.squad_md1
            "MD2" -> user.squad_md2
            "MD3" -> user.squad_md3
            "MD4" -> user.squad_md4
            "MD5" -> user.squad_md5
            else -> null
        }

        if (squadFieldStr.isNullOrEmpty() || squadFieldStr == "{}") {
            if (normalizedReq == localActive && isDeadlinePassed && !_uiState.value.snapshotTaken) {
                squadFieldStr = user.squad
            }
        }

        if (squadFieldStr.isNullOrEmpty() || squadFieldStr == "{}") return MatchdayPointsResult()

        var vsFormation = "4-4-2"
        var vsCapId: String? = null
        var vsVCId: String? = null
        val vsSquadPlayers = mutableMapOf<String, String>()

        try {
            val json = org.json.JSONObject(squadFieldStr)
            vsFormation = json.optString("formation", "4-4-2")
            vsCapId = json.optString("captain", null)
            vsVCId = json.optString("viceCaptain", null)
            if (json.has("players")) {
                val pJson = json.getJSONObject("players")
                val keys = pJson.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    if (!pJson.isNull(k)) {
                        vsSquadPlayers[k] = pJson.getString(k)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("EflViewModel", "Squad json parse error", e)
            return MatchdayPointsResult()
        }

        var mdTotal = 0
        val mdPlayerPts = mutableMapOf<String, Int>()
        val mdBreakdown = mutableMapOf<String, MutableList<String>>()

        vsSquadPlayers.forEach { (k, _) ->
            mdPlayerPts[k] = 0
            mdBreakdown[k] = mutableListOf()
        }

        val mdFixtures = _uiState.value.fixtures.filter { fix ->
            val d = fix.match_date ?: ""
            when(normalizedReq) {
                "MD1" -> d.contains("-01-")
                "MD2" -> d.contains("-02-")
                "MD3" -> d.contains("-03-")
                "MD4" -> d.contains("-04-")
                "MD5" -> d.contains("-05-")
                else -> false
            }
        }

        var captainTotalMins = 0
        mdFixtures.forEach { fix ->
            val log = _uiState.value.fplMatchData.firstOrNull { l -> l.fixture.contains(fix.id) } ?: return@forEach
            val t1Id = fix.team1.firstOrNull() ?: return@forEach
            val t2Id = fix.team2.firstOrNull() ?: return@forEach
            val capPlayer = _uiState.value.players.firstOrNull { it.id == vsCapId }
            if (capPlayer != null && (capPlayer.team.contains(t1Id) || capPlayer.team.contains(t2Id))) {
                captainTotalMins += getPlayerMins(capPlayer.id, log.players_under_15_mins, log.did_not_play)
            }
        }

        val doubleId = if (captainTotalMins > 0) vsCapId else vsVCId

        mdFixtures.forEach { fix ->
            val log = _uiState.value.fplMatchData.firstOrNull { l -> l.fixture.contains(fix.id) } ?: return@forEach
            val res = _uiState.value.results.firstOrNull { r -> r.fixture.contains(fix.id) } ?: return@forEach

            val t1Id = fix.team1.firstOrNull() ?: return@forEach
            val t2Id = fix.team2.firstOrNull() ?: return@forEach
            val score1 = res.score1
            val score2 = res.score2

            vsSquadPlayers.forEach { (slotKey, pId) ->
                val player = _uiState.value.players.firstOrNull { it.id == pId } ?: return@forEach
                val pTeamId = player.team.firstOrNull() ?: return@forEach
                if (pTeamId != t1Id && pTeamId != t2Id) return@forEach

                val isT1 = pTeamId == t1Id
                val teamPrefix = if (isT1) "t1" else "t2"
                val oppPrefix = if (isT1) "t2" else "t1"
                val oppScore = if (isT1) score2 else score1

                val mins = getPlayerMins(player.id, log.players_under_15_mins, log.did_not_play)
                val yellow = countEvents(player.id, res, "${teamPrefix}_yellow_", 4)
                val red = countEvents(player.id, res, "${teamPrefix}_red_", 2)

                if (mins == 0 && yellow == 0 && red == 0) return@forEach

                var pts = 0
                val pos = slotKey.split("-")[0].uppercase()
                val pBreak = mutableListOf<String>()

                if (mins >= 15) {
                    pts += 2
                    pBreak.add("Played 15+ mins (+2)")
                } else if (mins > 0) {
                    pts += 1
                    pBreak.add("Played <15 mins (+1)")
                }

                val regularGoals = countEvents(player.id, res, "${teamPrefix}_goal_")
                var penGoals = 0
                val pen1Taker = if (isT1) log.t1_pen_taker_1 else log.t2_pen_taker_1
                val pen1Scored = if (isT1) log.t1_pen_scored_1 else log.t2_pen_scored_1
                val pen2Taker = if (isT1) log.t1_pen_taker_2 else log.t2_pen_taker_2
                val pen2Scored = if (isT1) log.t1_pen_scored_2 else log.t2_pen_scored_2

                if (checkPenMatch(pen1Taker, player.id) && pen1Scored == true) penGoals++
                if (checkPenMatch(pen2Taker, player.id) && pen2Scored == true) penGoals++
                val goals = regularGoals + penGoals

                if (goals > 0) {
                    val gPts = when (pos) {
                        "FWD" -> goals * 4
                        "MID" -> goals * 5
                        else -> goals * 6
                    }
                    pts += gPts
                    pBreak.add("$goals Goal(s) (+$gPts)")
                }

                val assists = countEvents(player.id, res, "${teamPrefix}_assist_")
                if (assists > 0) {
                    pts += assists * 3
                    pBreak.add("$assists Assist(s) (+${assists * 3})")
                }

                val ownGoals = countEvents(player.id, res, "${oppPrefix}_goal_")
                if (ownGoals > 0) {
                    pts -= ownGoals * 2
                    pBreak.add("$ownGoals Own Goal(s) (-${ownGoals * 2})")
                }

                if (yellow > 0) {
                    pts -= yellow * 1
                    pBreak.add("$yellow Yellow (-${yellow * 1})")
                }
                if (red > 0) {
                    pts -= red * 3
                    pBreak.add("$red Red (-${red * 3})")
                }

                if (mins >= 15 && oppScore == 0) {
                    val csPts = when (pos) {
                        "GK" -> 4
                        "DEF" -> 3
                        "MID" -> 1
                        else -> 0
                    }
                    if (csPts > 0) {
                        pts += csPts
                        pBreak.add("Clean Sheet (+$csPts)")
                    }
                }

                if (mins >= 15 && (pos == "DEF" || pos == "GK") && oppScore >= 2) {
                    val minusPts = oppScore / 2
                    pts -= minusPts
                    pBreak.add("Conceded $oppScore (-$minusPts)")
                }

                var pensEarned = 0
                val pen1Earned = if (isT1) log.t1_pen_earned_1 else log.t2_pen_earned_1
                val pen2Earned = if (isT1) log.t1_pen_earned_2 else log.t2_pen_earned_2
                if (checkPenMatch(pen1Earned, player.id)) pensEarned++
                if (checkPenMatch(pen2Earned, player.id)) pensEarned++
                if (pensEarned > 0) {
                    pts += pensEarned * 3
                    pBreak.add("$pensEarned Pen Earned (+${pensEarned * 3})")
                }

                var pensMissed = 0
                if (checkPenMatch(pen1Taker, player.id) && pen1Scored == false) pensMissed++
                if (checkPenMatch(pen2Taker, player.id) && pen2Scored == false) pensMissed++
                if (pensMissed > 0) {
                    pts -= pensMissed * 2
                    pBreak.add("$pensMissed Pen Missed (-${pensMissed * 2})")
                }

                if (pos == "GK") {
                    var pensSaved = 0
                    val oppPen1Taker = if (isT1) log.t2_pen_taker_1 else log.t1_pen_taker_1
                    val oppPen1Scored = if (isT1) log.t2_pen_scored_1 else log.t1_pen_scored_1
                    val oppPen2Taker = if (isT1) log.t2_pen_taker_2 else log.t1_pen_taker_2
                    val oppPen2Scored = if (isT1) log.t2_pen_scored_2 else log.t1_pen_scored_2

                    if (isPenMissed(oppPen1Taker, oppPen1Scored)) pensSaved++
                    if (isPenMissed(oppPen2Taker, oppPen2Scored)) pensSaved++
                    if (pensSaved > 0) {
                        pts += pensSaved * 5
                        pBreak.add("$pensSaved Pen Saved (+${pensSaved * 5})")
                    }

                    val saves = if (isT1) (log.t1_saves ?: 0) else (log.t2_saves ?: 0)
                    if (saves >= 3) {
                        val savePts = saves / 3
                        pts += savePts
                        pBreak.add("$saves Saves (+$savePts)")
                    }
                }

                val motm = res.motm
                val potm = res.potm
                if (motm.contains(player.id) || potm.contains(player.id)) {
                    pts += 3
                    pBreak.add("POTM (+3)")
                }

                val secondBest = log.second_best
                if (secondBest.contains(player.id)) {
                    pts += 2
                    pBreak.add("2nd Best (+2)")
                }

                val thirdBest = log.third_best
                if (thirdBest.contains(player.id)) {
                    pts += 1
                    pBreak.add("3rd Best (+1)")
                }

                if (player.id == doubleId && pts != 0) {
                    pts *= 2
                    pBreak.add("<b>Captain (x2)</b>")
                }

                mdPlayerPts[slotKey] = (mdPlayerPts[slotKey] ?: 0) + pts
                mdTotal += pts
                mdBreakdown[slotKey]?.addAll(pBreak)
            }
        }

        return MatchdayPointsResult(mdTotal, mdPlayerPts, mdBreakdown)
    }

    fun getUserPointsForTab(user: FplUser, tabName: String): Int {
        return if (tabName != "Overall") {
            getMDPoints(user, tabName).mdTotal
        } else {
            var sum = 0
            listOf("MD 1", "MD 2", "MD 3", "MD 4", "MD 5").forEach { md ->
                sum += getMDPoints(user, md).mdTotal
            }
            sum
        }
    }

    // Dynamic request padding
    fun padPassword(pin: String): String {
        return pin + "_EFLSEC"
    }

    private fun parseRetrofitError(e: Exception, defaultMsg: String): String {
        if (e is retrofit2.HttpException) {
            try {
                val errBody = e.response()?.errorBody()?.string()
                if (!errBody.isNullOrEmpty()) {
                    val json = org.json.JSONObject(errBody)
                    val data = json.optJSONObject("data")
                    val errMap = mutableListOf<String>()
                    if (data != null) {
                        val keys = data.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val fieldObj = data.opt(key)
                            val fieldErr = if (fieldObj is org.json.JSONObject) {
                                fieldObj.optString("message")
                            } else if (fieldObj is org.json.JSONArray) {
                                fieldObj.optString(0)
                            } else {
                                fieldObj?.toString() ?: ""
                            }
                            if (!fieldErr.isNullOrEmpty()) {
                                errMap.add("$key: $fieldErr")
                            }
                        }
                    }
                    val baseMsg = json.optString("message", "")
                    return if (errMap.isNotEmpty()) {
                        "$baseMsg (${errMap.joinToString(", ")})"
                    } else {
                        baseMsg.ifEmpty { "Error code ${e.code()}" }
                    }
                }
            } catch (ex: Exception) {
                Log.e("EflViewModel", "Error parsing HTTP details", ex)
            }
        }
        return e.message ?: defaultMsg
    }

    fun loginFplUser(email: String, pin: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val json = "{\"identity\":\"$email\",\"password\":\"${padPassword(pin)}\"}"
                val body = json.toRequestBody("application/json".toMediaTypeOrNull())
                val response = RetrofitClient.apiService.executePost("https://pbdb2.duckdns.org/api/collections/fpl_users/auth-with-password", body)
                
                val moshiObj = com.example.data.api.RetrofitClient.moshi
                val adapter = moshiObj.adapter(FplAuthResponse::class.java)
                val authObj = adapter.fromJson(response.string()) as? FplAuthResponse
                if (authObj != null) {
                    sharedPrefs.edit().putString("fpl_user_id", authObj.record.id).apply()
                    _uiState.update { it.copy(currentFplUser = authObj.record) }
                    loadData()
                    onSuccess()
                } else {
                    onError("Failed parsing server response")
                }
            } catch (e: Exception) {
                Log.e("EflViewModel", "FPL login error", e)
                onError(parseRetrofitError(e, "Invalid credentials or server down"))
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun logoutFplUser() {
        sharedPrefs.edit().remove("fpl_user_id").apply()
        _uiState.update { it.copy(currentFplUser = null) }
    }

    fun registerFplUser(
        name: String,
        batch: String,
        email: String,
        pin: String,
        avatarBytes: ByteArray?,
        avatarFileName: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val padded = padPassword(pin)
                val builder = okhttp3.MultipartBody.Builder().setType(okhttp3.MultipartBody.FORM)
                    .addFormDataPart("email", email)
                    .addFormDataPart("password", padded)
                    .addFormDataPart("passwordConfirm", padded)
                    .addFormDataPart("name", name)
                    .addFormDataPart("batch", batch)
                    .addFormDataPart("squad", "{}")

                if (avatarBytes != null && avatarFileName != null) {
                    val fileBody = avatarBytes.toRequestBody("image/*".toMediaTypeOrNull())
                    builder.addFormDataPart("avatar", avatarFileName, fileBody)
                }

                RetrofitClient.apiService.executePost("https://pbdb2.duckdns.org/api/collections/fpl_users/records", builder.build())
                
                loginFplUser(email, pin, onSuccess, onError)
            } catch (e: Exception) {
                Log.e("EflViewModel", "FPL registration error", e)
                onError(parseRetrofitError(e, "Error creating account. Ensure valid email & unique username."))
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun updateFplProfile(
        email: String,
        pin: String?,
        avatarBytes: ByteArray?,
        avatarFileName: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val user = _uiState.value.currentFplUser ?: return@launch
            _uiState.update { it.copy(isLoading = true) }
            try {
                val builder = okhttp3.MultipartBody.Builder().setType(okhttp3.MultipartBody.FORM)
                if (email != user.email) {
                    builder.addFormDataPart("email", email)
                }
                if (!pin.isNullOrEmpty()) {
                    val padded = padPassword(pin)
                    builder.addFormDataPart("password", padded)
                    builder.addFormDataPart("passwordConfirm", padded)
                }
                if (avatarBytes != null && avatarFileName != null) {
                    val fileBody = avatarBytes.toRequestBody("image/*".toMediaTypeOrNull())
                    builder.addFormDataPart("avatar", avatarFileName, fileBody)
                }

                val patchUrl = "https://pbdb2.duckdns.org/api/collections/fpl_users/records/${user.id}"
                val response = RetrofitClient.apiService.executePatch(patchUrl, builder.build())
                
                val moshiObj = com.example.data.api.RetrofitClient.moshi
                val adapter = moshiObj.adapter(FplUser::class.java)
                val updatedUser = adapter.fromJson(response.string()) as? FplUser
                if (updatedUser != null) {
                    _uiState.update { it.copy(currentFplUser = updatedUser) }
                    loadData()
                    onSuccess()
                } else {
                    onError("Failed parsing updated model")
                }
            } catch (e: Exception) {
                Log.e("EflViewModel", "FPL update profile error", e)
                onError(parseRetrofitError(e, "Failed to update profile details. Email/username conflict."))
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun requestFplReset(email: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val json = "{\"email\":\"$email\"}"
                val body = json.toRequestBody("application/json".toMediaTypeOrNull())
                RetrofitClient.apiService.executePost("https://pbdb2.duckdns.org/api/collections/fpl_users/request-password-reset", body)
                onSuccess()
            } catch (e: Exception) {
                onError("Failed requesting password reset.")
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun confirmFplReset(token: String, pin: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val padded = padPassword(pin)
                val json = "{\"token\":\"$token\",\"password\":\"$padded\",\"passwordConfirm\":\"$padded\"}"
                val body = json.toRequestBody("application/json".toMediaTypeOrNull())
                RetrofitClient.apiService.executePost("https://pbdb2.duckdns.org/api/collections/fpl_users/confirm-password-reset", body)
                onSuccess()
            } catch (e: Exception) {
                onError("Failed resetting password. Token might be expired.")
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun searchFplUserToReset(
        batch: String,
        email: String,
        onSuccess: (com.example.data.model.FplUser) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val filter = "(batch='${batch.trim()}' && email='${email.trim()}')"
                val url = "https://pbdb2.duckdns.org/api/collections/fpl_users/records?filter=${java.net.URLEncoder.encode(filter, "UTF-8")}"
                val response = RetrofitClient.apiService.getFplUsers(url)
                if (response.items.isNotEmpty()) {
                    onSuccess(response.items[0])
                } else {
                    onError("No manager found matching Batch '$batch' and Email '$email'.")
                }
            } catch (e: Exception) {
                Log.e("EflViewModel", "Search reset user error", e)
                onError("Failed searching for manager: ${e.localizedMessage}")
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun resetSecurityPin(
        userId: String,
        newPin: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val padded = padPassword(newPin)
                val json = "{\"password\":\"$padded\",\"passwordConfirm\":\"$padded\"}"
                val body = json.toRequestBody("application/json".toMediaTypeOrNull())
                val patchUrl = "https://pbdb2.duckdns.org/api/collections/fpl_users/records/$userId"
                RetrofitClient.apiService.executePatch(patchUrl, body)
                onSuccess()
            } catch (e: Exception) {
                Log.e("EflViewModel", "Reset custom PIN error", e)
                onError("Failed to update PIN: ${e.localizedMessage}")
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun saveFplSquad(
        formation: String,
        players: Map<String, String?>,
        captainKey: String?,
        viceCaptainKey: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val user = _uiState.value.currentFplUser ?: return@launch
            _uiState.update { it.copy(isLoading = true) }
            try {
                val pEntries = players.entries.joinToString(",") { (k, v) ->
                    if (v != null) "\"$k\":\"$v\"" else "\"$k\":null"
                }
                val capVal = if (captainKey != null) "\"${players[captainKey]}\"" else "null"
                val vcVal = if (viceCaptainKey != null) "\"${players[viceCaptainKey]}\"" else "null"
                
                val squadJson = "{\"formation\":\"$formation\",\"players\":{$pEntries},\"captain\":$capVal,\"viceCaptain\":$vcVal}"
                val escapedSquadJson = squadJson.replace("\"", "\\\"")
                
                val patchJson = "{\"squad\":\"$escapedSquadJson\"}"
                val body = patchJson.toRequestBody("application/json".toMediaTypeOrNull())
                
                val patchUrl = "https://pbdb2.duckdns.org/api/collections/fpl_users/records/${user.id}"
                val response = RetrofitClient.apiService.executePatch(patchUrl, body)
                
                val moshiObj = com.example.data.api.RetrofitClient.moshi
                val adapter = moshiObj.adapter(FplUser::class.java)
                val updatedUser = adapter.fromJson(response.string()) as? FplUser
                if (updatedUser != null) {
                    _uiState.update { it.copy(currentFplUser = updatedUser) }
                    loadData()
                    onSuccess()
                } else {
                    onError("Failed saving squad response")
                }
            } catch (e: Exception) {
                Log.e("EflViewModel", "FPL squad save error", e)
                onError("Error saving squad")
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
