package com.example.ui.screens

import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Fixture
import com.example.data.model.FplMatchData
import com.example.data.model.Player
import com.example.data.model.Result
import com.example.data.model.Supporter
import com.example.data.model.Team
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MoreScreen(
    onNavigateTo: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            MenuTile(
                icon = Icons.Default.CalendarToday,
                title = "Fixtures",
                onClick = { onNavigateTo("fixtures") },
                modifier = Modifier.testTag("menu_fixtures")
            )
        }
        item {
            MenuTile(
                icon = Icons.Default.EmojiEvents,
                title = "Results & Stats",
                onClick = { onNavigateTo("results") },
                modifier = Modifier.testTag("menu_results")
            )
        }
        item {
            MenuTile(
                icon = Icons.Default.Handshake,
                title = "Supporters & Donors",
                onClick = { onNavigateTo("supporters") },
                modifier = Modifier.testTag("menu_supporters")
            )
        }
        item {
            MenuTile(
                icon = Icons.Default.Info,
                title = "About EFL",
                onClick = { onNavigateTo("about") },
                modifier = Modifier.testTag("menu_about")
            )
        }
        item {
            MenuTile(
                icon = Icons.Default.Help,
                title = "Help & Support",
                onClick = { onNavigateTo("support") },
                modifier = Modifier.testTag("menu_support")
            )
        }
    }
}

@Composable
fun MenuTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

// ──────────────────────────────────────────────
// FIXTURES SUB PAGE
// ──────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FixturesSubPage(
    fixtures: List<Fixture>,
    teams: List<Team>,
    results: List<Result> = emptyList(),
    players: List<Player> = emptyList(),
    fplMatchData: List<FplMatchData> = emptyList(),
    currentSeason: String = "",
    baseUrl: String,
    onBack: () -> Unit
) {
    var showOnlyUpcoming by remember { mutableStateOf(false) }

    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    val sdfAlternative = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    val now = Date()

    val filteredFixtures = if (showOnlyUpcoming) {
        fixtures.filter { f ->
            if (f.match_date.isNullOrEmpty()) return@filter true
            try {
                val date = sdf.parse(f.match_date) ?: sdfAlternative.parse(f.match_date)
                date != null && date.after(now)
            } catch (e: Exception) {
                true
            }
        }
    } else {
        fixtures
    }

    // Group fixtures by match_type / Matchday
    val groupedFixtures = filteredFixtures.groupBy { it.match_type ?: "OTHER MATCHES" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Fixtures Schedule", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (currentSeason.isNotEmpty()) {
                            Text("JU Dept. of English · Season $currentSeason", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    FilterChip(
                        selected = showOnlyUpcoming,
                        onClick = { showOnlyUpcoming = !showOnlyUpcoming },
                        label = { Text("Upcoming Only", fontSize = 11.sp) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            )
        }
    ) { paddingValues ->
        if (fixtures.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No match fixtures found.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                groupedFixtures.forEach { entry ->
                    val matchday = entry.key
                    val list = entry.value
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            Text(
                                text = matchday.uppercase(),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    items(list, key = { it.id }) { fix ->
                        val t1 = teams.firstOrNull { it.id == fix.team1.firstOrNull() }
                        val t2 = teams.firstOrNull { it.id == fix.team2.firstOrNull() }
                        val associatedResult = results.firstOrNull { r -> r.fixture.contains(fix.id) }
                        var isExpanded by remember { mutableStateOf(false) }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("fixture_card_${fix.id}")
                                .then(
                                    if (associatedResult != null) {
                                        Modifier.clickable { isExpanded = !isExpanded }
                                    } else Modifier
                                ),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = formatMatchDate(fix.match_date),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    if (associatedResult != null) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                        ) {
                                            Text(
                                                text = "Completed (FT)",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    } else {
                                        val isUpcoming = try {
                                            val date = sdf.parse(fix.match_date ?: "") ?: sdfAlternative.parse(fix.match_date ?: "")
                                            date != null && date.after(now)
                                        } catch (e: Exception) {
                                            true
                                        }
                                        if (isUpcoming) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                                            ) {
                                                Text(
                                                    text = "Scheduled",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = t1?.displayName ?: "TBD",
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                            textAlign = TextAlign.End,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        TeamLogo(team = t1, baseUrl = baseUrl, size = 28.dp)
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    if (associatedResult != null) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                        ) {
                                            val showPens = associatedResult.t1_pen_score != null && associatedResult.t2_pen_score != null && (associatedResult.t1_pen_score > 0.0 || associatedResult.t2_pen_score > 0.0)
                                            val scoreText = if (showPens) {
                                                "${associatedResult.score1} (${associatedResult.t1_pen_score!!.toInt()}) - ${associatedResult.score2} (${associatedResult.t2_pen_score!!.toInt()})"
                                            } else {
                                                "${associatedResult.score1} - ${associatedResult.score2}"
                                            }
                                            Text(
                                                text = scoreText,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = "VS",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.error),
                                            modifier = Modifier.padding(horizontal = 10.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Row(
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.Start,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TeamLogo(team = t2, baseUrl = baseUrl, size = 28.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = t2?.displayName ?: "TBD",
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                            textAlign = TextAlign.Start,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                    }
                                }

                                if (associatedResult != null) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Toggle Details",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isExpanded) "Tap to collapse" else "Tap for Match Center (Scorers, Cards, MOTM)",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    AnimatedVisibility(visible = isExpanded) {
                                        val goalsT1 = remember(associatedResult, players) {
                                            val list = mutableListOf<Pair<Player, Player?>>()
                                            for (g in 1..7) {
                                                val scorerId = associatedResult.getEventField("t1_goal_$g").firstOrNull()
                                                if (scorerId != null) {
                                                    val scorer = players.firstOrNull { it.id == scorerId }
                                                    if (scorer != null) {
                                                        val assistId = associatedResult.getEventField("t1_assist_$g").firstOrNull()
                                                        val assister = if (assistId != null) players.firstOrNull { it.id == assistId } else null
                                                        list.add(scorer to assister)
                                                    }
                                                }
                                            }
                                            list
                                        }

                                        val goalsT2 = remember(associatedResult, players) {
                                            val list = mutableListOf<Pair<Player, Player?>>()
                                            for (g in 1..7) {
                                                val scorerId = associatedResult.getEventField("t2_goal_$g").firstOrNull()
                                                if (scorerId != null) {
                                                    val scorer = players.firstOrNull { it.id == scorerId }
                                                    if (scorer != null) {
                                                        val assistId = associatedResult.getEventField("t2_assist_$g").firstOrNull()
                                                        val assister = if (assistId != null) players.firstOrNull { it.id == assistId } else null
                                                        list.add(scorer to assister)
                                                    }
                                                }
                                            }
                                            list
                                        }

                                        val yellowT1 = remember(associatedResult, players) {
                                            val list = mutableListOf<Player>()
                                            for (y in 1..4) {
                                                val pId = associatedResult.getEventField("t1_yellow_$y").firstOrNull()
                                                if (pId != null) {
                                                    players.firstOrNull { it.id == pId }?.let { list.add(it) }
                                                }
                                            }
                                            list
                                        }

                                        val redT1 = remember(associatedResult, players) {
                                            val list = mutableListOf<Player>()
                                            for (r in 1..2) {
                                                val pId = associatedResult.getEventField("t1_red_$r").firstOrNull()
                                                if (pId != null) {
                                                    players.firstOrNull { it.id == pId }?.let { list.add(it) }
                                                }
                                            }
                                            list
                                        }

                                        val yellowT2 = remember(associatedResult, players) {
                                            val list = mutableListOf<Player>()
                                            for (y in 1..4) {
                                                val pId = associatedResult.getEventField("t2_yellow_$y").firstOrNull()
                                                if (pId != null) {
                                                    players.firstOrNull { it.id == pId }?.let { list.add(it) }
                                                }
                                            }
                                            list
                                        }

                                        val redT2 = remember(associatedResult, players) {
                                            val list = mutableListOf<Player>()
                                            for (r in 1..2) {
                                                val pId = associatedResult.getEventField("t2_red_$r").firstOrNull()
                                                if (pId != null) {
                                                    players.firstOrNull { it.id == pId }?.let { list.add(it) }
                                                }
                                            }
                                            list
                                        }

                                        val motmPlayer = players.firstOrNull { it.id == associatedResult.motm.firstOrNull() }
                                        val matchData = fplMatchData.firstOrNull { it.fixture.contains(fix.id) }

                                        val penaltiesT1 = remember(matchData, players) {
                                            val list = mutableListOf<String>()
                                            if (matchData != null) {
                                                val taker1Id = matchData.t1_pen_taker_1.firstOrNull()
                                                val earner1Id = matchData.t1_pen_earned_1.firstOrNull()
                                                if (taker1Id != null) {
                                                    val taker = players.firstOrNull { it.id == taker1Id }?.name ?: "Unknown"
                                                    val earner = if (earner1Id != null) " (Earned by: ${players.firstOrNull { it.id == earner1Id }?.name ?: "Unknown"})" else ""
                                                    val scored = matchData.t1_pen_scored_1 == true
                                                    list.add("${if (scored) "⚽" else "❌"} Pen: $taker$earner")
                                                }
                                                val taker2Id = matchData.t1_pen_taker_2.firstOrNull()
                                                val earner2Id = matchData.t1_pen_earned_2.firstOrNull()
                                                if (taker2Id != null) {
                                                    val taker = players.firstOrNull { it.id == taker2Id }?.name ?: "Unknown"
                                                    val earner = if (earner2Id != null) " (Earned by: ${players.firstOrNull { it.id == earner2Id }?.name ?: "Unknown"})" else ""
                                                    val scored = matchData.t1_pen_scored_2 == true
                                                    list.add("${if (scored) "⚽" else "❌"} Pen: $taker$earner")
                                                }
                                            }
                                            list
                                        }

                                        val penaltiesT2 = remember(matchData, players) {
                                            val list = mutableListOf<String>()
                                            if (matchData != null) {
                                                val taker1Id = matchData.t2_pen_taker_1.firstOrNull()
                                                val earner1Id = matchData.t2_pen_earned_1.firstOrNull()
                                                if (taker1Id != null) {
                                                    val taker = players.firstOrNull { it.id == taker1Id }?.name ?: "Unknown"
                                                    val earner = if (earner1Id != null) " (Earned by: ${players.firstOrNull { it.id == earner1Id }?.name ?: "Unknown"})" else ""
                                                    val scored = matchData.t2_pen_scored_1 == true
                                                    list.add("${if (scored) "⚽" else "❌"} Pen: $taker$earner")
                                                }
                                                val taker2Id = matchData.t2_pen_taker_2.firstOrNull()
                                                val earner2Id = matchData.t2_pen_earned_2.firstOrNull()
                                                if (taker2Id != null) {
                                                    val taker = players.firstOrNull { it.id == taker2Id }?.name ?: "Unknown"
                                                    val earner = if (earner2Id != null) " (Earned by: ${players.firstOrNull { it.id == earner2Id }?.name ?: "Unknown"})" else ""
                                                    val scored = matchData.t2_pen_scored_2 == true
                                                    list.add("${if (scored) "⚽" else "❌"} Pen: $taker$earner")
                                                }
                                            }
                                            list
                                        }

                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                                            ) {
                                                // T1 Events
                                                Column(
                                                    modifier = Modifier.weight(1f),
                                                    horizontalAlignment = Alignment.End,
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    goalsT1.forEach { gPair ->
                                                        val scorer = gPair.first
                                                        val prevCount = goalsT1.take(goalsT1.indexOf(gPair)).count { it.first.id == scorer.id }
                                                        val penSuffix = getGoalSuffix(associatedResult, scorer.id, true, prevCount)
                                                        val assistStr = if (gPair.second != null) " (a: ${gPair.second?.name})" else ""
                                                        Text("⚽ ${scorer.name ?: ""}$penSuffix$assistStr", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End)
                                                    }
                                                    penaltiesT1.forEach { pen ->
                                                        Text(pen, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.End)
                                                    }
                                                    yellowT1.forEach { p ->
                                                        Text("🟨 ${p.name ?: ""}", style = MaterialTheme.typography.bodySmall, color = Color(0xFFD97706), textAlign = TextAlign.End)
                                                    }
                                                    redT1.forEach { p ->
                                                        Text("🟥 ${p.name ?: ""}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.End)
                                                    }
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .width(1.dp)
                                                        .align(Alignment.CenterVertically)
                                                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                                        .height(40.dp)
                                                )

                                                // T2 Events
                                                Column(
                                                    modifier = Modifier.weight(1f),
                                                    horizontalAlignment = Alignment.Start,
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    goalsT2.forEach { gPair ->
                                                        val scorer = gPair.first
                                                        val prevCount = goalsT2.take(goalsT2.indexOf(gPair)).count { it.first.id == scorer.id }
                                                        val penSuffix = getGoalSuffix(associatedResult, scorer.id, false, prevCount)
                                                        val assistStr = if (gPair.second != null) " (a: ${gPair.second?.name})" else ""
                                                        Text("⚽ ${gPair.first.name ?: ""}$penSuffix$assistStr", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Start)
                                                    }
                                                    penaltiesT2.forEach { pen ->
                                                        Text(pen, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Start)
                                                    }
                                                    yellowT2.forEach { p ->
                                                        Text("🟨 ${p.name ?: ""}", style = MaterialTheme.typography.bodySmall, color = Color(0xFFD97706), textAlign = TextAlign.Start)
                                                    }
                                                    redT2.forEach { p ->
                                                        Text("🟥 ${p.name ?: ""}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Start)
                                                    }
                                                }
                                            }

                                            if (motmPlayer != null) {
                                                Text(
                                                    text = "🏅 Match Best (MOTM): ${motmPlayer.name}",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.primary,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helper function to format match dates beautifully
fun formatMatchDate(dateStr: String?): String {
    if (dateStr.isNullOrEmpty()) return "TBA"
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val outputFormat = SimpleDateFormat("EEEE, d MMM yyyy · HH:mm", Locale.getDefault())
        val date = inputFormat.parse(dateStr) ?: return dateStr.split(".").first().replace("T", " ")
        outputFormat.format(date)
    } catch (e: Exception) {
        try {
            val inputFormat2 = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = inputFormat2.parse(dateStr) ?: return dateStr
            val outputFormat2 = SimpleDateFormat("EEEE, d MMM yyyy · HH:mm", Locale.getDefault())
            outputFormat2.format(date)
        } catch (e2: Exception) {
            dateStr.replace("T", " ").replace("Z", "")
        }
    }
}

fun getGoalSuffix(res: Result, scorerId: String, isT1: Boolean, previousScoredCount: Int): String {
    val penGoals = if (isT1) {
        listOf(res.t1_pen_goal_1, res.t1_pen_goal_2, res.t1_pen_goal_3, res.t1_pen_goal_4)
    } else {
        listOf(res.t2_pen_goal_1, res.t2_pen_goal_2, res.t2_pen_goal_3, res.t2_pen_goal_4)
    }
    val penCount = penGoals.count { it.firstOrNull() == scorerId }
    if (previousScoredCount < penCount) {
        return " (p)"
    }
    return ""
}

// ──────────────────────────────────────────────
// RESULTS SUB PAGE
// ──────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsSubPage(
    results: List<Result>,
    fixtures: List<Fixture>,
    teams: List<Team>,
    players: List<Player>,
    fplMatchData: List<FplMatchData> = emptyList(),
    currentSeason: String = "",
    baseUrl: String,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Match Results", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (currentSeason.isNotEmpty()) {
                            Text("JU Dept. of English · Season $currentSeason", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (results.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No match results logged yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(results, key = { it.id }) { res ->
                    val fixId = res.fixture.firstOrNull()
                    val associatedFixture = fixtures.firstOrNull { f -> f.id == fixId }

                    if (associatedFixture != null) {
                        val t1 = teams.firstOrNull { it.id == associatedFixture.team1.firstOrNull() }
                        val t2 = teams.firstOrNull { it.id == associatedFixture.team2.firstOrNull() }
                        
                        var isExpanded by remember { mutableStateOf(false) }

                        // Parse game events from result fields
                        val goalsT1 = remember(res, players) {
                            val list = mutableListOf<Pair<Player, Player?>>()
                            for (g in 1..7) {
                                val scorerId = res.getEventField("t1_goal_$g").firstOrNull()
                                if (scorerId != null) {
                                    val scorer = players.firstOrNull { it.id == scorerId }
                                    if (scorer != null) {
                                        val assistId = res.getEventField("t1_assist_$g").firstOrNull()
                                        val assister = if (assistId != null) players.firstOrNull { it.id == assistId } else null
                                        list.add(scorer to assister)
                                    }
                                }
                            }
                            list
                        }

                        val goalsT2 = remember(res, players) {
                            val list = mutableListOf<Pair<Player, Player?>>()
                            for (g in 1..7) {
                                val scorerId = res.getEventField("t2_goal_$g").firstOrNull()
                                if (scorerId != null) {
                                    val scorer = players.firstOrNull { it.id == scorerId }
                                    if (scorer != null) {
                                        val assistId = res.getEventField("t2_assist_$g").firstOrNull()
                                        val assister = if (assistId != null) players.firstOrNull { it.id == assistId } else null
                                        list.add(scorer to assister)
                                    }
                                }
                            }
                            list
                        }

                        val yellowT1 = remember(res, players) {
                            val list = mutableListOf<Player>()
                            for (y in 1..4) {
                                val pId = res.getEventField("t1_yellow_$y").firstOrNull()
                                if (pId != null) {
                                    players.firstOrNull { it.id == pId }?.let { list.add(it) }
                                }
                            }
                            list
                        }

                        val redT1 = remember(res, players) {
                            val list = mutableListOf<Player>()
                            for (r in 1..2) {
                                val pId = res.getEventField("t1_red_$r").firstOrNull()
                                if (pId != null) {
                                    players.firstOrNull { it.id == pId }?.let { list.add(it) }
                                }
                            }
                            list
                        }

                        val yellowT2 = remember(res, players) {
                            val list = mutableListOf<Player>()
                            for (y in 1..4) {
                                val pId = res.getEventField("t2_yellow_$y").firstOrNull()
                                if (pId != null) {
                                    players.firstOrNull { it.id == pId }?.let { list.add(it) }
                                }
                            }
                            list
                        }

                        val redT2 = remember(res, players) {
                            val list = mutableListOf<Player>()
                            for (r in 1..2) {
                                val pId = res.getEventField("t2_red_$r").firstOrNull()
                                if (pId != null) {
                                    players.firstOrNull { it.id == pId }?.let { list.add(it) }
                                }
                            }
                            list
                        }

                        val motmPlayer = players.firstOrNull { it.id == res.motm.firstOrNull() }
                        val potmPlayer = players.firstOrNull { it.id == res.potm.firstOrNull() }
                        val matchData = fplMatchData.firstOrNull { it.fixture.contains(fixId) }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("result_card_${res.id}")
                                .clickable { isExpanded = !isExpanded },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Match header: Match day and formatted date
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = (associatedFixture.match_type ?: "Match").uppercase(),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    )
                                    Text(
                                        text = formatMatchDate(associatedFixture.match_date),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Interactive scoreboard
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = t1?.displayName ?: "TBD",
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                            textAlign = TextAlign.End,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        TeamLogo(team = t1, baseUrl = baseUrl, size = 28.dp)
                                    }

                                    Surface(
                                        modifier = Modifier.padding(horizontal = 10.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                    ) {
                                        val showPens = res.t1_pen_score != null && res.t2_pen_score != null && (res.t1_pen_score > 0.0 || res.t2_pen_score > 0.0)
                                        val scoreText = if (showPens) {
                                            "${res.score1} (${res.t1_pen_score!!.toInt()}) - ${res.score2} (${res.t2_pen_score!!.toInt()})"
                                        } else {
                                            "${res.score1} - ${res.score2}"
                                        }
                                        Text(
                                            text = scoreText,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.Start,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TeamLogo(team = t2, baseUrl = baseUrl, size = 28.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = t2?.displayName ?: "TBD",
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                            textAlign = TextAlign.Start,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                    }
                                }

                                // Interactive Prompt
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Toggle Match Details",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isExpanded) "Tap to collapse details" else "Tap to view Match Center details",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                // Expanded Match Details panel
                                AnimatedVisibility(visible = isExpanded) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 10.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                                        // Side-by-Side Match Events Row (Goals & bookings)
                                        if (goalsT1.isNotEmpty() || goalsT2.isNotEmpty() || yellowT1.isNotEmpty() || redT1.isNotEmpty() || yellowT2.isNotEmpty() || redT2.isNotEmpty()) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                                            ) {
                                                // Team 1 events
                                                Column(
                                                    modifier = Modifier.weight(1f),
                                                    horizontalAlignment = Alignment.End,
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    goalsT1.forEach { gPair ->
                                                        val scorer = gPair.first
                                                        val assist = gPair.second
                                                        val prevCount = goalsT1.take(goalsT1.indexOf(gPair)).count { it.first.id == scorer.id }
                                                        val penSuffix = getGoalSuffix(res, scorer.id, true, prevCount)
                                                        val assistStr = if (assist != null) " (a: ${assist.name})" else ""
                                                        Text(
                                                            text = "⚽ ${scorer.name ?: "Player"}$penSuffix$assistStr",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            textAlign = TextAlign.End,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                    }
                                                    yellowT1.forEach { p ->
                                                        Text(
                                                            text = "🟨 ${p.name ?: ""}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            textAlign = TextAlign.End,
                                                            color = Color(0xFFD97706)
                                                        )
                                                    }
                                                    redT1.forEach { p ->
                                                        Text(
                                                            text = "🟥 ${p.name ?: ""}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            textAlign = TextAlign.End,
                                                            color = MaterialTheme.colorScheme.error
                                                        )
                                                    }
                                                }

                                                // Vertical Separator
                                                Box(
                                                    modifier = Modifier
                                                        .width(1.dp)
                                                        .align(Alignment.CenterVertically)
                                                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                                        .padding(vertical = 12.dp)
                                                )

                                                // Team 2 events
                                                Column(
                                                    modifier = Modifier.weight(1f),
                                                    horizontalAlignment = Alignment.Start,
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    goalsT2.forEach { gPair ->
                                                        val scorer = gPair.first
                                                        val assist = gPair.second
                                                        val prevCount = goalsT2.take(goalsT2.indexOf(gPair)).count { it.first.id == scorer.id }
                                                        val penSuffix = getGoalSuffix(res, scorer.id, false, prevCount)
                                                        val assistStr = if (assist != null) " (a: ${assist.name})" else ""
                                                        Text(
                                                            text = "⚽ ${scorer.name ?: "Player"}$penSuffix$assistStr",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            textAlign = TextAlign.Start,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                    }
                                                    yellowT2.forEach { p ->
                                                        Text(
                                                            text = "🟨 ${p.name ?: ""}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            textAlign = TextAlign.Start,
                                                            color = Color(0xFFD97706)
                                                        )
                                                    }
                                                    redT2.forEach { p ->
                                                        Text(
                                                            text = "🟥 ${p.name ?: ""}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            textAlign = TextAlign.Start,
                                                            color = MaterialTheme.colorScheme.error
                                                        )
                                                    }
                                                }
                                            }
                                        } else {
                                            Text(
                                                text = "No goal or card events recorded.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                            )
                                        }

                                        // Detailed Performance and Goalkeeping parameters
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            if (motmPlayer != null || potmPlayer != null) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    if (motmPlayer != null) {
                                                        Text(
                                                            text = "🏅 MOTM: ${motmPlayer.name}",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                            color = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                    }
                                                    if (potmPlayer != null) {
                                                        Text(
                                                            text = "🧤 POTM: ${potmPlayer.name}",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                            color = Color(0xFF10B981),
                                                            modifier = Modifier.weight(1f),
                                                            textAlign = TextAlign.End
                                                        )
                                                    }
                                                }
                                                Divider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                                            }

                                            if (matchData != null) {
                                                val sbPlayers = matchData.second_best.mapNotNull { sId -> players.firstOrNull { it.id == sId } }
                                                if (sbPlayers.isNotEmpty()) {
                                                    Text(
                                                        text = "🥈 Second Best (3 pts): ${sbPlayers.joinToString { it.name ?: "" }}",
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                }

                                                val tbPlayers = matchData.third_best.mapNotNull { tId -> players.firstOrNull { it.id == tId } }
                                                if (tbPlayers.isNotEmpty()) {
                                                    Text(
                                                        text = "🥉 Third Best (2 pts): ${tbPlayers.joinToString { it.name ?: "" }}",
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                }

                                                val u15Players = matchData.players_under_15_mins.mapNotNull { uId -> players.firstOrNull { it.id == uId } }
                                                if (u15Players.isNotEmpty()) {
                                                    Text(
                                                        text = "⏱️ Played < 15m (1 pt): ${u15Players.joinToString { it.name ?: "" }}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }

                                                val dnpPlayers = matchData.did_not_play.mapNotNull { dId -> players.firstOrNull { it.id == dId } }
                                                if (dnpPlayers.isNotEmpty()) {
                                                    Text(
                                                        text = "❌ DNP (0 pts): ${dnpPlayers.joinToString { it.name ?: "" }}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────
// SUPPORTERS SUB PAGE
// ──────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportersSubPage(
    supporters: List<Supporter>,
    teams: List<Team>,
    baseUrl: String,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Supporters & Donors") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (supporters.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No supporter logs available.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(supporters, key = { it.id }) { sup ->
                    val supportedTeam = teams.firstOrNull { it.id == sup.supported_team.firstOrNull() }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("supporter_card_${sup.id}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Circular photo avatar
                            Surface(
                                modifier = Modifier.size(50.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                AsyncImage(
                                    model = "$baseUrl/api/files/${sup.collectionId}/${sup.id}/${sup.photo}",
                                    contentDescription = sup.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            // Details
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = sup.name ?: "Anonymous Supporter",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Batch: " + (sup.batch ?: "--"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (!sup.message.isNullOrEmpty()) {
                                    Text(
                                        text = "\"${sup.message}\"",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontStyle = FontStyle.Italic,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            // Side supported team tag label
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFFEF3C7),
                                contentColor = Color(0xFFD97706)
                            ) {
                                Text(
                                    text = supportedTeam?.crest_text ?: "Supporter",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────
// ABOUT SUB PAGE
// ──────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSubPage(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About EFL") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Centered EFL Brand Logo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = com.example.R.drawable.efl_logo,
                    contentDescription = "EFL Logo",
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(4.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "THE TOURNAMENT",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                )

                Text(
                    text = "The English Football League (EFL) is the annual intra-departmental football tournament organized by students of the Department of English, Jahangirnagar University. Now entering its 5th season, EFL has grown into a celebrated tradition that brings together football enthusiasts from all batches.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "ORGANIZING COMMITTEE",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                )

                val committee = listOf(
                    "• Tournament Director: UTAU -(The Watcher)",
                    "• Registration Lead & Logistics: Sayem",
                    "• Media & Comms: Madhu, Indrolal",
                    "• Alumni Operations: Akon & Sayem"
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    committee.forEach { item ->
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────
// TEAM LOGO COMPONENT
// ──────────────────────────────────────────────
@Composable
fun TeamLogo(
    team: Team?,
    baseUrl: String,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 28.dp
) {
    val teamColor = remember(team) { hexToColor(team?.teamBgColorHex) }
    Surface(
        modifier = modifier.size(size),
        shape = CircleShape,
        color = teamColor,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        if (team != null && !team.banner.isNullOrEmpty()) {
            AsyncImage(
                model = "$baseUrl/api/files/${team.collectionId}/${team.id}/${team.banner}",
                contentDescription = team.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = team?.crest_text ?: "FC",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.45f).sp,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ──────────────────────────────────────────────
// HELP & SUPPORT SUB PAGE
// ──────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportSubPage(onBack: () -> Unit) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Help & Support") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Elegant Icon / Illustration Header
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Help,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(40.dp)
                )
            }

            Text(
                text = "How can we help?",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
            )

            Text(
                text = "In case you need any support regarding your EFL FPL account, tournament stats, data corrections, or other issues, please get in touch with our team via email or WhatsApp below. We're here to assist you!",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Email Card
            val email = "bumblebee9171@gmail.com"
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        try {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:$email")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("EFL Support Email", email)
                            clipboard.setPrimaryClip(clip)
                            android.widget.Toast.makeText(context, "Email copied: $email", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.secondaryContainer, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email Support",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Email Support",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "Open",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // WhatsApp Card
            val whatsappNumber = "+8801300890530"
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        try {
                            val cleanNumber = whatsappNumber.replace("+", "").replace(" ", "")
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("https://wa.me/$cleanNumber")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("EFL Support WhatsApp Phone", whatsappNumber)
                            clipboard.setPrimaryClip(clip)
                            android.widget.Toast.makeText(context, "Number copied: $whatsappNumber", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFF25D366).copy(alpha = 0.15f), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = "WhatsApp Chat",
                            tint = Color(0xFF128C7E)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "WhatsApp Chat",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = whatsappNumber,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "Open",
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFF128C7E)
                    )
                }
            }
        }
    }
}
