package com.example

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.EflViewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: EflViewModel = viewModel()
            val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()

            MyApplicationTheme(darkTheme = isDarkTheme) {
                MainAppContainer(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContainer(viewModel: EflViewModel) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentSeason by viewModel.currentSeason.collectAsStateWithLifecycle()
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()

    val searchQuery by viewModel.playerSearchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedPlayerCategory.collectAsStateWithLifecycle()

    // Keep track of current primary navigation screen route
    var activeTabRoute by remember { mutableStateOf("home") }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("main_scaffold"),
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "EFL PORTAL",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = "JU Dept. of English · Season $currentSeason",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        )
                    }
                },
                actions = {
                    // Season tab switch button
                    Row(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(2.dp)
                            .testTag("season_toggle_row"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SeasonBadgeButton(
                            label = "S5",
                            active = currentSeason == "5",
                            onClick = { viewModel.switchSeason("5") }
                        )
                        SeasonBadgeButton(
                            label = "S4",
                            active = currentSeason == "4",
                            onClick = { viewModel.switchSeason("4") }
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Dark mode toggler
                    IconButton(
                        onClick = { viewModel.toggleTheme() },
                        modifier = Modifier.testTag("theme_toggle")
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Theme Toggle"
                        )
                    }
                }
            )
        },
        bottomBar = {
            // Display Bottom Bar if on one of primary screens: home, teams, players, table, more
            val primaryTabs = listOf("home", "teams", "players", "table", "more")
            if (activeTabRoute in primaryTabs) {
                NavigationBar(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .testTag("bottom_navigation_bar"),
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = activeTabRoute == "home",
                        onClick = {
                            activeTabRoute = "home"
                            navController.navigate("home") {
                                popUpTo("home") { inclusive = true }
                            }
                        },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        modifier = Modifier.testTag("tab_home")
                    )
                    NavigationBarItem(
                        selected = activeTabRoute == "teams",
                        onClick = {
                            activeTabRoute = "teams"
                            navController.navigate("teams") {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Shield, contentDescription = "Teams") },
                        label = { Text("Teams") },
                        modifier = Modifier.testTag("tab_teams")
                    )
                    NavigationBarItem(
                        selected = activeTabRoute == "players",
                        onClick = {
                            activeTabRoute = "players"
                            navController.navigate("players") {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.DirectionsRun, contentDescription = "Players") },
                        label = { Text("Players") },
                        modifier = Modifier.testTag("tab_players")
                    )
                    NavigationBarItem(
                        selected = activeTabRoute == "table",
                        onClick = {
                            activeTabRoute = "table"
                            navController.navigate("table") {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Leaderboard, contentDescription = "Table") },
                        label = { Text("Table") },
                        modifier = Modifier.testTag("tab_table")
                    )
                    NavigationBarItem(
                        selected = activeTabRoute == "more",
                        onClick = {
                            activeTabRoute = "more"
                            navController.navigate("more") {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.MoreHoriz, contentDescription = "More") },
                        label = { Text("More") },
                        modifier = Modifier.testTag("tab_more")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main views navigation Graph
            NavHost(
                navController = navController,
                startDestination = "home"
            ) {
                composable("home") {
                    HomeScreen(
                        season = currentSeason,
                        players = uiState.players,
                        onViewTeamsClick = {
                            activeTabRoute = "teams"
                            navController.navigate("teams")
                        }
                    )
                }
                composable("teams") {
                    TeamsScreen(
                        teams = uiState.teams,
                        players = uiState.players,
                        baseUrl = viewModel.baseUrl
                    )
                }
                composable("players") {
                    PlayersScreen(
                        players = uiState.players,
                        teams = uiState.teams,
                        searchQuery = searchQuery,
                        selectedCategory = selectedCategory,
                        onSearchChanged = { viewModel.updateSearchQuery(it) },
                        onCategoryChanged = { viewModel.updateSelectedCategory(it) },
                        baseUrl = viewModel.baseUrl
                    )
                }
                composable("table") {
                    StandingsScreen(
                        standings = uiState.standings,
                        baseUrl = viewModel.baseUrl
                    )
                }
                composable("more") {
                    MoreScreen(
                        onNavigateTo = { route ->
                            activeTabRoute = route
                            navController.navigate(route)
                        }
                    )
                }

                // Sub views inside the directory
                composable("fixtures") {
                    FixturesSubPage(
                        fixtures = uiState.fixtures,
                        teams = uiState.teams,
                        baseUrl = viewModel.baseUrl,
                        onBack = {
                            activeTabRoute = "more"
                            navController.popBackStack()
                        }
                    )
                }

                composable("results") {
                    ResultsSubPage(
                        results = uiState.results,
                        fixtures = uiState.fixtures,
                        teams = uiState.teams,
                        players = uiState.players,
                        onBack = {
                            activeTabRoute = "more"
                            navController.popBackStack()
                        }
                    )
                }

                composable("supporters") {
                    SupportersSubPage(
                        supporters = uiState.supporters,
                        teams = uiState.teams,
                        baseUrl = viewModel.baseUrl,
                        onBack = {
                            activeTabRoute = "more"
                            navController.popBackStack()
                        }
                    )
                }

                composable("about") {
                    AboutSubPage(
                        onBack = {
                            activeTabRoute = "more"
                            navController.popBackStack()
                        }
                    )
                }
            }

            // Central progress loading HUD indicators
            if (uiState.isLoading) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.5f)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Error message HUD banner
            uiState.errorMessage?.let { errorMsg ->
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                        .padding(horizontal = 24.dp)
                        .testTag("error_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = errorMsg,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SeasonBadgeButton(
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (active) MaterialTheme.colorScheme.error else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (active) Color.White else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
    }
}
