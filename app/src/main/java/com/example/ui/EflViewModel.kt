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
    val errorMessage: String? = null
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
}
