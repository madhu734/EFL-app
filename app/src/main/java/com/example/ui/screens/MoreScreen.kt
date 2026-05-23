package com.example.ui.screens

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

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("fixture_card_${fix.id}"),
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
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = t1?.displayName ?: "TBD",
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                        textAlign = TextAlign.End,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))

                                    if (associatedResult != null) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                        ) {
                                            Text(
                                                text = "${associatedResult.score1} - ${associatedResult.score2}",
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

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Text(
                                        text = t2?.displayName ?: "TBD",
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                        textAlign = TextAlign.Start,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
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
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = t1?.displayName ?: "TBD",
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                        textAlign = TextAlign.End,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Surface(
                                        modifier = Modifier.padding(horizontal = 14.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                    ) {
                                        Text(
                                            text = "${res.score1} - ${res.score2}",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }

                                    Text(
                                        text = t2?.displayName ?: "TBD",
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                        textAlign = TextAlign.Start,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
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
                                                        val assistStr = if (assist != null) " (a: ${assist.name})" else ""
                                                        Text(
                                                            text = "⚽ ${scorer.name ?: "Player"}$assistStr",
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
                                                        val assistStr = if (assist != null) " (a: ${assist.name})" else ""
                                                        Text(
                                                            text = "⚽ ${scorer.name ?: "Player"}$assistStr",
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
                                            Text(
                                                text = "Match Center Performance Metrics",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(bottom = 2.dp)
                                            )

                                            if (motmPlayer != null) {
                                                Text(
                                                    text = "🏅 MOTM (1st Best): ${motmPlayer.name} · ${t1?.displayName ?: ""}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            if (potmPlayer != null && potmPlayer.id != motmPlayer?.id) {
                                                Text(
                                                    text = "🏅 POTM: ${potmPlayer.name}",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }

                                            if (matchData != null) {
                                                if (matchData.t1_saves != null || matchData.t2_saves != null) {
                                                    val s1 = matchData.t1_saves ?: 0
                                                    val s2 = matchData.t2_saves ?: 0
                                                    Text(
                                                        text = "🧤 Goalkeeper Saves: $s1 (Home) vs $s2 (Away)",
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                }

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
