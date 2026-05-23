package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.graphics.drawscope.Stroke
import android.util.Log
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.api.RetrofitClient
import com.example.data.model.FplUser
import com.example.data.model.Player
import com.example.data.model.Team
import com.example.ui.EflViewModel
import kotlinx.coroutines.delay
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FplScreen(
    viewModel: EflViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    var currentTab by remember { mutableStateOf("SQUAD") } // SQUAD, LEADERBOARD, BEST11, RULES
    
    // Countdown Timer logic
    var timeRemainingStr by remember { mutableStateOf("🔒 Locked") }
    val isDeadlinePassed = uiState.forceLock || (System.currentTimeMillis() >= uiState.deadlineTime)
    
    LaunchedEffect(uiState.deadlineTime, uiState.forceLock) {
        while (true) {
            val now = System.currentTimeMillis()
            val diff = uiState.deadlineTime - now
            if (uiState.forceLock || diff <= 0) {
                timeRemainingStr = "🔒 Matchday Locked"
            } else {
                val sec = (diff / 1000) % 60
                val min = (diff / (1000 * 60)) % 60
                val hr = (diff / (1000 * 60 * 60)) % 24
                val day = diff / (1000 * 60 * 60 * 24)
                timeRemainingStr = "⏳ Deadline: ${day}d ${hr}h ${min}m ${sec}s"
            }
            delay(1000)
        }
    }

    Scaffold(
        topBar = {
            // Highly compact custom header row to save maximal vertical space
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isDeadlinePassed) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = (if (isDeadlinePassed) "Locked" else "Active") + " (${uiState.activeMatchday})",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                            color = if (isDeadlinePassed) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = timeRemainingStr,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                        color = if (isDeadlinePassed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = { viewModel.loadData() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh FPL Data",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tabs header
            TabRow(
                selectedTabIndex = when (currentTab) {
                    "SQUAD" -> 0
                    "LEADERBOARD" -> 1
                    "BEST11" -> 2
                    else -> 3
                }
            ) {
                Tab(
                    selected = currentTab == "SQUAD",
                    onClick = { currentTab = "SQUAD" },
                    text = { Text("My Squad", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )
                Tab(
                    selected = currentTab == "LEADERBOARD",
                    onClick = { currentTab = "LEADERBOARD" },
                    text = { Text("Standings", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )
                Tab(
                    selected = currentTab == "BEST11",
                    onClick = { currentTab = "BEST11" },
                    text = { Text("Best XI", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )
                Tab(
                    selected = currentTab == "RULES",
                    onClick = { currentTab = "RULES" },
                    text = { Text("Rules", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                when (currentTab) {
                    "SQUAD" -> SquadTab(viewModel, isDeadlinePassed)
                    "LEADERBOARD" -> LeaderboardTab(viewModel)
                    "BEST11" -> BestElevenTab(viewModel)
                    "RULES" -> RulesTab()
                }
            }
        }
    }
}

// SQUAD MANAGEMENT TAB
// ──────────────────────────────────────────────
@Composable
fun SquadTab(viewModel: EflViewModel, isDeadlineLocked: Boolean) {
    val uiState by viewModel.uiState.collectAsState()
    val user = uiState.currentFplUser

    if (user == null) {
        FplAuthScreen(viewModel)
    } else {
        FplSquadManager(viewModel, user, isDeadlineLocked)
    }
}

// AUTH SUB SCREEN
// ──────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FplAuthScreen(viewModel: EflViewModel) {
    var mode by remember { mutableStateOf("LOGIN") } // LOGIN, REGISTER, RESET
    var email by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var batch by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    
    val avatarEmojis = listOf("🦁", "🐯", "🐝", "🦅", "🦉", "🎸", "⚽", "🏆", "🕶️", "🎩", "🦊", "👑")
    var selectedEmoji by remember { mutableStateOf("🦁") }

    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when(mode) {
                        "LOGIN" -> "Manager Sign In"
                        "REGISTER" -> "Create Manager Account"
                        else -> "Reset Dashboard Password"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                if (errorMsg != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMsg ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                if (mode == "REGISTER") {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Manager Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("auth_name_field"),
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                    )

                    OutlinedTextField(
                        value = batch,
                        onValueChange = { batch = it },
                        label = { Text("Batch Code (e.g. 19)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("auth_batch_field"),
                        leadingIcon = { Icon(Icons.Default.Group, contentDescription = null) }
                    )

                    // Avatar Selector
                    Text("Choose Manager Crest Emoji", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        avatarEmojis.forEach { emoji ->
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (selectedEmoji == emoji) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surface
                                    )
                                    .clickable { selectedEmoji = emoji },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 22.sp)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth().testTag("auth_email_field"),
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) }
                )

                if (mode != "RESET") {
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = it },
                        label = { Text("Security passcode (PIN)") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth().testTag("auth_pin_field"),
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) }
                    )
                }

                Button(
                    onClick = {
                        errorMsg = null
                        if (email.trim().isEmpty()) {
                            errorMsg = "Please enter email address"
                            return@Button
                        }
                        if (mode != "RESET" && pin.trim().isEmpty()) {
                            errorMsg = "Please enter passcode (PIN)"
                            return@Button
                        }

                        if (mode == "LOGIN") {
                            viewModel.loginFplUser(
                                email = email.trim(),
                                pin = pin.trim(),
                                onSuccess = {
                                    Toast.makeText(context, "Welcome back, Manager!", Toast.LENGTH_SHORT).show()
                                },
                                onError = { errorMsg = it }
                            )
                        } else if (mode == "REGISTER") {
                            if (name.trim().isEmpty()) {
                                errorMsg = "Please fill in Manager Name"
                                return@Button
                            }
                            viewModel.registerFplUser(
                                name = name.trim(),
                                batch = batch.trim(),
                                email = email.trim(),
                                pin = pin.trim(),
                                avatarBytes = selectedEmoji.toByteArray(Charsets.UTF_8),
                                avatarFileName = "emoji_${selectedEmoji}.txt",
                                onSuccess = {
                                    Toast.makeText(context, "Manager registration successful!", Toast.LENGTH_SHORT).show()
                                },
                                onError = { errorMsg = it }
                            )
                        } else {
                            viewModel.requestFplReset(
                                email = email.trim(),
                                onSuccess = {
                                    Toast.makeText(context, "Password reset request sent!", Toast.LENGTH_LONG).show()
                                    mode = "LOGIN"
                                },
                                onError = { errorMsg = it }
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("auth_submit_btn")
                ) {
                    Text(
                        if (mode == "LOGIN") "Sign In"
                        else if (mode == "REGISTER") "Register Account"
                        else "Send Reset Link"
                    )
                }

                // Footers toggling
                Surface(
                    onClick = {
                        errorMsg = null
                        mode = if (mode == "LOGIN") "REGISTER" else "LOGIN"
                    },
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.Transparent
                ) {
                    Text(
                        text = if (mode == "LOGIN") "Don't have an account? Sign Up" else "Already registered? Sign In",
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                if (mode == "LOGIN") {
                    Surface(
                        onClick = { mode = "RESET" },
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.Transparent
                    ) {
                        Text(
                            text = "Forgot PIN?",
                            color = MaterialTheme.colorScheme.tertiary,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else if (mode == "RESET") {
                    Surface(
                        onClick = { mode = "LOGIN" },
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.Transparent
                    ) {
                        Text(
                            text = "Back to Sign In",
                            color = MaterialTheme.colorScheme.tertiary,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

// PITCH OR SQUAD LAYOUT FOR INDIVIDUAL USER
// ──────────────────────────────────────────────
@Composable
fun FplSquadManager(
    viewModel: EflViewModel,
    user: FplUser,
    isDeadlineLocked: Boolean
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Initialize state from existing squad JSON
    var selectedFormation by remember { mutableStateOf("4-4-2") }
    var squadPlayers = remember { mutableStateMapOf<String, String?>() } // Map Slot -> Player ID
    var captainKey by remember { mutableStateOf<String?>(null) }
    var viceCaptainKey by remember { mutableStateOf<String?>(null) }

    var isPlayerSelectionVisible by remember { mutableStateOf(false) }
    var selectingSlotKey by remember { mutableStateOf<String?>(null) }

    var isProfileEditingVisible by remember { mutableStateOf(false) }

    // Parse squad when changed or loaded
    LaunchedEffect(user.squad) {
        val squadStr = user.squad
        if (!squadStr.isNullOrEmpty() && squadStr != "{}") {
            try {
                val json = JSONObject(squadStr)
                selectedFormation = json.optString("formation", "4-4-2")
                captainKey = json.optString("captain", null)
                viceCaptainKey = json.optString("viceCaptain", null)
                
                squadPlayers.clear()
                if (json.has("players")) {
                    val pObj = json.getJSONObject("players")
                    val keys = pObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        squadPlayers[k] = if (pObj.isNull(k)) null else pObj.getString(k)
                    }
                }
            } catch (e: Exception) {
                Log.e("FplScreen", "Error loading squad", e)
            }
        } else {
            // Fill default empty slots based on 4-4-2
            selectedFormation = "4-4-2"
            squadPlayers.clear()
            fillDefaultEmptySlots(squadPlayers, selectedFormation)
            captainKey = null
            viceCaptainKey = null
        }
    }

    // Budget math
    val squadDraftCount = squadPlayers.values.count { it != null }
    val spentBudget = squadPlayers.values.filterNotNull().sumOf { pId ->
        uiState.players.firstOrNull { it.id == pId }?.fplPrice ?: 7.0
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Manager Info Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val avatarStr = user.avatar ?: ""
                    val displayAvatar = if (avatarStr.startsWith("emoji_") && avatarStr.endsWith(".txt")) {
                        avatarStr.removePrefix("emoji_").removeSuffix(".txt")
                    } else "⚽"

                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(displayAvatar, fontSize = 26.sp)
                    }

                    Column {
                        Text(user.name ?: "Unknown Manager", fontWeight = FontWeight.Black, style = MaterialTheme.typography.bodyLarge)
                        Text("Batch: ${user.batch ?: "N/A"}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = { isProfileEditingVisible = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Profile")
                    }
                    Button(
                        onClick = { viewModel.logoutFplUser() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Logout")
                    }
                }
            }
        }

        // Sidebar status (counts + budget)
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Players Selected", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$squadDraftCount / 11", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Box(modifier = Modifier.width(1.dp).height(40.dp).background(MaterialTheme.colorScheme.outlineVariant))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Budget", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = String.format("%.1fM / 100.0M", spentBudget),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (spentBudget > 100.0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Carefulness Reminders banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Reminders",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Crucial Reminders:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                Text(
                    text = "1. You must click on 'Save Squad' below to update/save your squad. Unsaved drafts will be lost.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = "2. Last saved squad before a match day countdown ends will be Auto saved for that match day.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }

        // Formation Selection Controls
        if (!isDeadlineLocked) {
            Text("Select Formation", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            val formations = listOf("5-4-1", "4-5-1", "5-3-2", "4-3-3", "4-4-2", "3-5-2", "3-4-3", "4-2-4", "5-2-3")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                formations.forEach { form ->
                    FilterChip(
                        selected = selectedFormation == form,
                        onClick = {
                            if (selectedFormation != form) {
                                adjustSquadSlotsForFormation(squadPlayers, form)
                                selectedFormation = form
                            }
                        },
                        label = { Text(form, fontWeight = FontWeight.Bold) }
                    )
                }
            }
        }

        // Visual Soccer FIELD Pitch Layout
        // ──────────────────────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF234F26))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        // Drawing pitch grass patterns and lines
                        val brush = Brush.verticalGradient(listOf(Color(0xFF1D4520), Color(0xFF2E6332)))
                        drawRect(brush)

                        val white = Color(0x35FFFFFF)
                        val stroke = 3.dp.toPx()

                        // Border
                        drawRect(color = white, style = Stroke(stroke))
                        // Center line
                        drawLine(color = white, start = Offset(0f, size.height / 2), end = Offset(size.width, size.height / 2), strokeWidth = stroke)
                        // Center circle
                        drawCircle(color = white, radius = size.width * 0.18f, center = Offset(size.width / 2, size.height / 2), style = Stroke(stroke))
                        // Penalty box primary (top)
                        drawRect(color = white, topLeft = Offset(size.width * 0.15f, 0f), size = androidx.compose.ui.geometry.Size(size.width * 0.7f, size.height * 0.15f), style = Stroke(stroke))
                        // Penalty box bottom
                        drawRect(color = white, topLeft = Offset(size.width * 0.15f, size.height * 0.85f), size = androidx.compose.ui.geometry.Size(size.width * 0.7f, size.height * 0.15f), style = Stroke(stroke))
                    }
                    .padding(8.dp)
            ) {
                // Placing tactical players vertical layout
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val split = selectedFormation.split("-").map { it.toInt() }
                    val dCount = split[0]
                    val mCount = split[1]
                    val fCount = split[2]

                    // Goalkeeper row (At the very bottom/GK role)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        PitchSlot(
                            slotKey = "GK-1",
                            pId = squadPlayers["GK-1"],
                            uiState = uiState,
                            isDeadlineLocked = isDeadlineLocked,
                            isCaptain = captainKey == "GK-1",
                            isViceCaptain = viceCaptainKey == "GK-1",
                            onSlotClick = {
                                selectingSlotKey = "GK-1"
                                isPlayerSelectionVisible = true
                            },
                            onRemove = { squadPlayers["GK-1"] = null },
                            onMarkCaptain = { isCap ->
                                if (isCap) {
                                    captainKey = "GK-1"
                                    if (viceCaptainKey == "GK-1") viceCaptainKey = null
                                } else {
                                    captainKey = null
                                }
                            },
                            onMarkViceCaptain = { isVC ->
                                if (isVC) {
                                    viceCaptainKey = "GK-1"
                                    if (captainKey == "GK-1") captainKey = null
                                } else {
                                    viceCaptainKey = null
                                }
                            }
                        )
                    }

                    // Defenders row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (i in 1..dCount) {
                            val slot = "DEF-$i"
                            PitchSlot(
                                slotKey = slot,
                                pId = squadPlayers[slot],
                                uiState = uiState,
                                isDeadlineLocked = isDeadlineLocked,
                                isCaptain = captainKey == slot,
                                isViceCaptain = viceCaptainKey == slot,
                                onSlotClick = {
                                    selectingSlotKey = slot
                                    isPlayerSelectionVisible = true
                                },
                                onRemove = { squadPlayers[slot] = null },
                                onMarkCaptain = { isCap ->
                                    if (isCap) {
                                        captainKey = slot
                                        if (viceCaptainKey == slot) viceCaptainKey = null
                                    } else {
                                        captainKey = null
                                    }
                                },
                                onMarkViceCaptain = { isVC ->
                                    if (isVC) {
                                        viceCaptainKey = slot
                                        if (captainKey == slot) captainKey = null
                                    } else {
                                        viceCaptainKey = null
                                    }
                                }
                            )
                        }
                    }

                    // Midfielders row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (i in 1..mCount) {
                            val slot = "MID-$i"
                            PitchSlot(
                                slotKey = slot,
                                pId = squadPlayers[slot],
                                uiState = uiState,
                                isDeadlineLocked = isDeadlineLocked,
                                isCaptain = captainKey == slot,
                                isViceCaptain = viceCaptainKey == slot,
                                onSlotClick = {
                                    selectingSlotKey = slot
                                    isPlayerSelectionVisible = true
                                },
                                onRemove = { squadPlayers[slot] = null },
                                onMarkCaptain = { isCap ->
                                    if (isCap) {
                                        captainKey = slot
                                        if (viceCaptainKey == slot) viceCaptainKey = null
                                    } else {
                                        captainKey = null
                                    }
                                },
                                onMarkViceCaptain = { isVC ->
                                    if (isVC) {
                                        viceCaptainKey = slot
                                        if (captainKey == slot) captainKey = null
                                    } else {
                                        viceCaptainKey = null
                                    }
                                }
                            )
                        }
                    }

                    // Forwards row (At the very top)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (i in 1..fCount) {
                            val slot = "FWD-$i"
                            PitchSlot(
                                slotKey = slot,
                                pId = squadPlayers[slot],
                                uiState = uiState,
                                isDeadlineLocked = isDeadlineLocked,
                                isCaptain = captainKey == slot,
                                isViceCaptain = viceCaptainKey == slot,
                                onSlotClick = {
                                    selectingSlotKey = slot
                                    isPlayerSelectionVisible = true
                                },
                                onRemove = { squadPlayers[slot] = null },
                                onMarkCaptain = { isCap ->
                                    if (isCap) {
                                        captainKey = slot
                                        if (viceCaptainKey == slot) viceCaptainKey = null
                                    } else {
                                        captainKey = null
                                    }
                                },
                                onMarkViceCaptain = { isVC ->
                                    if (isVC) {
                                        viceCaptainKey = slot
                                        if (captainKey == slot) captainKey = null
                                    } else {
                                        viceCaptainKey = null
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Squad Actions Save or Clear button
        if (!isDeadlineLocked) {
            val errors = evaluateSquadErrors(squadPlayers, captainKey, viceCaptainKey, spentBudget, uiState.players, uiState.teams)

            if (errors.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Draft validation requirements:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                        errors.forEach { err ->
                            Text("• $err", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        squadPlayers.keys.forEach { squadPlayers[it] = null }
                        captainKey = null
                        viceCaptainKey = null
                    },
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Clear Squad")
                }

                Button(
                    onClick = {
                        viewModel.saveFplSquad(
                            formation = selectedFormation,
                            players = squadPlayers.toMap(),
                            captainKey = captainKey,
                            viceCaptainKey = viceCaptainKey,
                            onSuccess = {
                                Toast.makeText(context, "Squad draft compiled and saved!", Toast.LENGTH_SHORT).show()
                            },
                            onError = {
                                Toast.makeText(context, "Error saving squad: $it", Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    enabled = errors.isEmpty(),
                    modifier = Modifier.weight(1f).height(48.dp).testTag("save_squad_btn")
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Save Squad")
                }
            }
        }
    }

    // PROFILE EDIT MODAL
    if (isProfileEditingVisible) {
        FplProfileModal(
            viewModel = viewModel,
            user = user,
            onDismiss = { isProfileEditingVisible = false }
        )
    }

    // DRAWER SELECTION DIALOG FOR DRAFTING PLAYERS
    if (isPlayerSelectionVisible && selectingSlotKey != null) {
        PlayerDraftDialog(
            viewModel = viewModel,
            slotKey = selectingSlotKey!!,
            squadPlayers = squadPlayers,
            onSelect = { draftedPlayerId ->
                squadPlayers[selectingSlotKey!!] = draftedPlayerId
                isPlayerSelectionVisible = false
            },
            onDismiss = { isPlayerSelectionVisible = false }
        )
    }
}

// Tactical Slot inside soccer green pitch
// ──────────────────────────────────────────────
@Composable
fun PitchSlot(
    slotKey: String,
    pId: String?,
    uiState: com.example.ui.EflUiState,
    isDeadlineLocked: Boolean,
    isCaptain: Boolean,
    isViceCaptain: Boolean,
    onSlotClick: () -> Unit,
    onRemove: () -> Unit,
    onMarkCaptain: (Boolean) -> Unit,
    onMarkViceCaptain: (Boolean) -> Unit
) {
    val player = uiState.players.firstOrNull { it.id == pId }
    val team = uiState.teams.firstOrNull { it.id == player?.team?.firstOrNull() }

    Box(
        modifier = Modifier
            .width(82.dp)
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !isDeadlineLocked) { onSlotClick() }
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(
                        if (player != null) Color.White.copy(alpha = 0.2f)
                        else Color.White.copy(alpha = 0.08f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (player != null) {
                    // Photo loading
                    val picUrl = "${uiState.systemSettings.firstOrNull()?.deadline?.let { "https://efljudb.duckdns.org" } ?: "https://pbdb2.duckdns.org"}/api/files/${player.collectionId}/${player.id}/${player.photo}"
                    AsyncImage(
                        model = picUrl,
                        contentDescription = player.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = if (isDeadlineLocked) Icons.Default.Lock else Icons.Default.Add,
                        contentDescription = "Draft Player",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // C / VC badging tags in upper left/right corner of visual circular frame
                if (player != null) {
                    if (isCaptain) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFC107))
                                .align(Alignment.TopStart),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("C", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (isViceCaptain) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE0E0E0))
                                .align(Alignment.TopStart),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("V", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // Last Name or display name tag
            val rawName = player?.name ?: ""
            val computedLastName = if (rawName.contains(" ")) rawName.substringAfterLast(" ") else rawName
            
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (player != null) Color(0xD0132B15)
                        else Color(0x7000000)
                    )
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (player != null) computedLastName else slotKey.split("-")[0],
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }

            if (player != null) {
                // Team text and points details underneath name tags
                val crestText = team?.crest_text ?: ""
                Text(
                    text = "${player.fplPrice}M | ${player.computedPoints} pts",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 9.sp),
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Quick overlay bubble selectors for Captain options or Delete
        if (player != null && !isDeadlineLocked) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 10.dp)
                    .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 2.dp, vertical = 1.dp)
            ) {
                // Remove player
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Remove player", tint = Color.Red, modifier = Modifier.size(12.dp))
                }
                // Mark Captain
                IconButton(
                    onClick = { onMarkCaptain(!isCaptain) },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = if (isCaptain) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = "Mark Captain",
                        tint = Color(0xFFFFD54F),
                        modifier = Modifier.size(12.dp)
                    )
                }
                // Mark Vice Captain
                IconButton(
                    onClick = { onMarkViceCaptain(!isViceCaptain) },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = if (isViceCaptain) Icons.Filled.VerifiedUser else Icons.Outlined.VerifiedUser,
                        contentDescription = "Mark Vice Captain",
                        tint = Color(0xFF90CAF9),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

// PROFILE EDITING MODAL BACKEND COMPOSABLE
// ──────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FplProfileModal(
    viewModel: EflViewModel,
    user: FplUser,
    onDismiss: () -> Unit
) {
    var email by remember { mutableStateOf(user.email ?: "") }
    var pin by remember { mutableStateOf("") }
    val avatarEmojis = listOf("🦁", "🐯", "🐝", "🦅", "🦉", "🎸", "⚽", "🏆", "🕶️", "🎩", "🦊", "👑")
    var selectedEmoji by remember {
        val avatarStr = user.avatar ?: ""
        var em = "🦁"
        if (avatarStr.startsWith("emoji_") && avatarStr.endsWith(".txt")) {
            em = avatarStr.removePrefix("emoji_").removeSuffix(".txt")
        }
        mutableStateOf(em)
    }

    var progressMode by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Edit Manager Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                if (errorMsg != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(errorMsg ?: "", color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.bodySmall)
                    }
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it },
                    label = { Text("New PIN (Leave blank to keep current)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Profile Avatar Crest Emojis:", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    avatarEmojis.forEach { emoji ->
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selectedEmoji == emoji) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { selectedEmoji = emoji },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 18.sp)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }

                    Button(
                        modifier = Modifier.weight(1f),
                        enabled = !progressMode && email.isNotEmpty(),
                        onClick = {
                            progressMode = true
                            errorMsg = null
                            viewModel.updateFplProfile(
                                email = email.trim(),
                                pin = if (pin.trim().isEmpty()) null else pin.trim(),
                                avatarBytes = selectedEmoji.toByteArray(Charsets.UTF_8),
                                avatarFileName = "emoji_${selectedEmoji}.txt",
                                onSuccess = {
                                    Toast.makeText(context, "Manager profile updated!", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                },
                                onError = {
                                    errorMsg = it
                                    progressMode = false
                                }
                            )
                        }
                    ) {
                        if (progressMode) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}

// PLAYER DRAFT SELECTOR DIALOG DRAWER
// ──────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerDraftDialog(
    viewModel: EflViewModel,
    slotKey: String,
    squadPlayers: Map<String, String?>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    
    val targetPosition = slotKey.split("-")[0].uppercase()

    // Filter list of players by position and search keyword and duplicate drafting
    val draftedIds = squadPlayers.values.filterNotNull().toSet()
    val availablePlayers = uiState.players.filter { player ->
        val mappedPos = (player.position ?: "DEF").uppercase()
        mappedPos == targetPosition && !draftedIds.contains(player.id) &&
        (player.name ?: "").contains(searchQuery, ignoreCase = true)
    }.sortedByDescending { it.computedPoints }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.82f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Draft player for $targetPosition slot",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search Players...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                )

                if (availablePlayers.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No eligible players found", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availablePlayers, key = { it.id }) { p ->
                            val teamObj = uiState.teams.firstOrNull { it.id == p.team.firstOrNull() }
                            val teamDraftsCount = squadPlayers.values.filterNotNull().count { draftedId ->
                                val dp = uiState.players.firstOrNull { it.id == draftedId }
                                dp?.team?.firstOrNull() == teamObj?.id
                            }
                            val isLimitExceeded = teamObj != null && teamDraftsCount >= 5

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isLimitExceeded) { onSelect(p.id) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isLimitExceeded) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Player photo
                                        val picUrl = "${uiState.systemSettings.firstOrNull()?.deadline?.let { "https://efljudb.duckdns.org" } ?: "https://pbdb2.duckdns.org"}/api/files/${p.collectionId}/${p.id}/${p.photo}"
                                        AsyncImage(
                                            model = picUrl,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )

                                        Column {
                                            Text(p.name ?: "Unknown Player", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            // The user's request: "in the player section show team logos with full team name if or as they ar drafted in the relational team field in players table..."
                                            Text(
                                                text = teamObj?.crest_text ?: "Unassigned Team",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("${p.fplPrice}M", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Text("${p.computedPoints} pts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                        if (isLimitExceeded) {
                                            Text("Team Limit (5/5)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

// STANDINGS LEADERBOARD TAB
// ──────────────────────────────────────────────
@Composable
fun LeaderboardTab(viewModel: EflViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    
    val tabs = listOf("Overall", "MD 1", "MD 2", "MD 3", "MD 4", "MD 5")
    var selectedLeaderboardTab by remember { mutableStateOf("Overall") }

    // Sort users by points score under the selected matchday or overall
    val sortedManagers = uiState.fplUsers.map { user ->
        val pts = viewModel.getUserPointsForTab(user, selectedLeaderboardTab)
        user to pts
    }.sortedByDescending { it.second }

    var selectedManagerSquadToShow by remember { mutableStateOf<FplUser?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Tab header selection Scroll Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabs.forEach { tb ->
                FilterChip(
                    selected = selectedLeaderboardTab == tb,
                    onClick = { selectedLeaderboardTab = tb },
                    label = { Text(tb, fontWeight = FontWeight.Bold) }
                )
            }
        }

        // Small compact instruction text
        Text(
            text = "* Click on any manager to view their squad and points breakdown",
            style = MaterialTheme.typography.bodySmall.copy(
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        // Leaderboard Lists Table header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                .padding(vertical = 10.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("#", fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
                Text("Manager Name", fontWeight = FontWeight.Bold)
            }
            Text("Score", fontWeight = FontWeight.Bold)
        }

        if (sortedManagers.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No Managers registered yet", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(sortedManagers) { index, pair ->
                    val mgr = pair.first
                    val pts = pair.second
                    
                    val rank = index + 1

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedManagerSquadToShow = mgr }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (rank) {
                                                1 -> Color(0xFFFFD54F)
                                                2 -> Color(0xFFB0BEC5)
                                                3 -> Color(0xFFFFAB91)
                                                else -> MaterialTheme.colorScheme.secondaryContainer
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$rank",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (rank <= 3) Color.Black else MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val avatarStr = mgr.avatar ?: ""
                                    val iconDisplay = if (avatarStr.startsWith("emoji_") && avatarStr.endsWith(".txt")) {
                                        avatarStr.removePrefix("emoji_").removeSuffix(".txt")
                                    } else "🦊"
                                    Text(iconDisplay, fontSize = 20.sp)

                                    Column {
                                        Text(mgr.name ?: "Unknown", fontWeight = FontWeight.Bold)
                                        Text("Batch ${mgr.batch ?: "N/A"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            Text("$pts pts", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }

    // Modal view details of squad
    if (selectedManagerSquadToShow != null) {
        ManagerSquadDetailsModal(
            viewModel = viewModel,
            user = selectedManagerSquadToShow!!,
            activeTab = selectedLeaderboardTab,
            onDismiss = { selectedManagerSquadToShow = null }
        )
    }
}

// READ-ONLY SQUAD AND POINTS BREAKDOWN MODAL
// ──────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerSquadDetailsModal(
    viewModel: EflViewModel,
    user: FplUser,
    activeTab: String,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Choose matchday: default to MD1-5 matching the modal's activeTab
    val defaultMd = if (activeTab == "Overall") "MD 1" else activeTab
    var viewMdName by remember { mutableStateOf(defaultMd) }

    val mdOptions = listOf("MD 1", "MD 2", "MD 3", "MD 4", "MD 5")

    // Compile Matchday results and breakdown mapping
    val computedRes = viewModel.getMDPoints(user, viewMdName)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Modal Profile Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val avatarStr = user.avatar ?: ""
                    val iconDisplay = if (avatarStr.startsWith("emoji_") && avatarStr.endsWith(".txt")) {
                        avatarStr.removePrefix("emoji_").removeSuffix(".txt")
                    } else "🎩"

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(iconDisplay, fontSize = 22.sp)
                    }

                    Column {
                        Text("${user.name}'s Squad", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Weekly Points compiler table breakdown", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Matchday selector switches Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    mdOptions.forEach { md ->
                        FilterChip(
                            selected = viewMdName == md,
                            onClick = { viewMdName = md },
                            label = { Text(md, fontWeight = FontWeight.SemiBold) }
                        )
                    }
                }

                // Matchday points scoreboard header
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Matchday Subtotal Points:", fontWeight = FontWeight.Bold)
                        Text("${computedRes.mdTotal} pts", fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }

                // Players compiled table row list
                Text("Players Points Breakdown", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                if (computedRes.mdPlayerPts.isEmpty()) {
                    Box(modifier = Modifier.height(100.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No active squad drafting logged for $viewMdName", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    computedRes.mdPlayerPts.forEach { (slotKey, pts) ->
                        // Parse player name
                        val parsedSlotVal = mutableMapOf<String, String>()
                        try {
                            val normalizedReq = viewMdName.uppercase().replace(" ", "")
                            val localActive = uiState.activeMatchday.uppercase().replace(" ", "")
                            val isDeadlinePassed = uiState.forceLock || (System.currentTimeMillis() >= uiState.deadlineTime)

                            var squadFieldStr: String? = when(normalizedReq) {
                                "MD1" -> user.squad_md1
                                "MD2" -> user.squad_md2
                                "MD3" -> user.squad_md3
                                "MD4" -> user.squad_md4
                                "MD5" -> user.squad_md5
                                else -> null
                            }

                            if (squadFieldStr.isNullOrEmpty() || squadFieldStr == "{}") {
                                if (normalizedReq == localActive && isDeadlinePassed && !uiState.snapshotTaken) {
                                    squadFieldStr = user.squad
                                }
                            }

                            if (!squadFieldStr.isNullOrEmpty()) {
                                val js = JSONObject(squadFieldStr)
                                if (js.has("players")) {
                                    val pl = js.getJSONObject("players")
                                    val keys = pl.keys()
                                    while (keys.hasNext()) {
                                        val k = keys.next()
                                        parsedSlotVal[k] = pl.getString(k)
                                    }
                                }
                            }
                        } catch (e: Exception) {}

                        val pId = parsedSlotVal[slotKey]
                        val player = uiState.players.firstOrNull { it.id == pId }

                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(player?.name ?: "Vacant slot", fontWeight = FontWeight.Bold)
                                        Text(slotKey.split("-")[0], style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                    }
                                    Text("$pts pts", fontWeight = FontWeight.Black, style = MaterialTheme.typography.bodyLarge)
                                }

                                val details = computedRes.mdBreakdown[slotKey] ?: emptyList()
                                if (details.isNotEmpty()) {
                                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                                    details.forEach { dl ->
                                        // Simple bold stripping for caption indicators
                                        val cleanText = dl.replace("<b>", "").replace("</b>", "")
                                        Text(cleanText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }

                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Close details")
                }
            }
        }
    }
}

// BEST ELEVEN (TOURNAMENT BEST XI IN 4-3-3 FORMATION)
// ──────────────────────────────────────────────
@Composable
fun BestElevenTab(viewModel: EflViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    // Calculate Tournament Best XI automatically:
    // Sort players by total FPL points in 4-3-3 formation template:
    // GK: Highest 1
    // DEF: Highest 4
    // MID: Highest 3
    // FWD: Highest 3
    val gks = uiState.players.filter { (it.position ?: "DEF").uppercase() == "GK" }.sortedByDescending { it.computedPoints }
    val defs = uiState.players.filter { (it.position ?: "DEF").uppercase() == "DEF" }.sortedByDescending { it.computedPoints }
    val mids = uiState.players.filter { (it.position ?: "DEF").uppercase() == "MID" }.sortedByDescending { it.computedPoints }
    val fwds = uiState.players.filter { (it.position ?: "DEF").uppercase() == "FWD" }.sortedByDescending { it.computedPoints }

    val bestGk = gks.firstOrNull()
    val bestDefs = defs.take(4)
    val bestMids = mids.take(3)
    val bestFwds = fwds.take(3)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Tournament Best XI", fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            Text("Automated highest-scoring 4-3-3 squad", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Field representation
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF132B15))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val brush = Brush.verticalGradient(listOf(Color(0xFF1D4520), Color(0xFF2E6332)))
                        drawRect(brush)
                        val white = Color(0x30FFFFFF)
                        val stroke = 3.dp.toPx()
                        drawRect(color = white, style = Stroke(stroke))
                        drawLine(color = white, start = Offset(0f, size.height / 2), end = Offset(size.width, size.height / 2), strokeWidth = stroke)
                        drawCircle(color = white, radius = size.width * 0.18f, center = Offset(size.width / 2, size.height / 2), style = Stroke(stroke))
                    }
                    .padding(10.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Goalkeeper row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        bestGk?.let { BestSlot(it, uiState) }
                    }

                    // Defenders row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        bestDefs.forEach { BestSlot(it, uiState) }
                    }

                    // Midfielders row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        bestMids.forEach { BestSlot(it, uiState) }
                    }

                    // Forwards row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        bestFwds.forEach { BestSlot(it, uiState) }
                    }
                }
            }
        }
    }
}

@Composable
fun BestSlot(player: Player, uiState: com.example.ui.EflUiState) {
    val team = uiState.teams.firstOrNull { it.id == player.team.firstOrNull() }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(82.dp)
    ) {
        val picUrl = "${uiState.systemSettings.firstOrNull()?.deadline?.let { "https://efljudb.duckdns.org" } ?: "https://pbdb2.duckdns.org"}/api/files/${player.collectionId}/${player.id}/${player.photo}"
        AsyncImage(
            model = picUrl,
            contentDescription = null,
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f)),
            contentScale = ContentScale.Crop
        )

        Spacer(Modifier.height(3.dp))

        val rawName = player.name ?: ""
        val lastName = if (rawName.contains(" ")) rawName.substringAfterLast(" ") else rawName
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xD0132B15))
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(lastName, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = Color.White, maxLines = 1)
        }

        Text("${player.computedPoints} pts", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f))
    }
}

// RULES TAB CONTROLLERS
// ──────────────────────────────────────────────
@Composable
fun RulesTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Rules & Scoring guide", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Draft constraints:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("• Active budget max limit: 100.0M")
                Text("• Selected managers squad size: exactly 11 players")
                Text("• Max 5 players can be drafted from the same EFL relational team")
                Text("• Both a Captain (x2 multiplier) and Vice-Captain must be marked")
            }
        }

        // Carefulness rules
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f),
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Carefulness Rules",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Carefulness Rules & Reminders",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Text(
                    text = "1. You must click on 'Save Squad' to update/save your squad. Unsaved changes will be lost.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "2. Last saved squad before a match day countdown ends will be Auto saved for that match day.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Point Compiler Rules:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Played 15+ mins")
                    Text("+2 pts", fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Played <15 mins")
                    Text("+1 pt", fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Goal Scored (FWD)")
                    Text("+4 pts", fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Goal Scored (MID)")
                    Text("+5 pts", fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Goal Scored (DEF/GK)")
                    Text("+6 pts", fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Goal Assist")
                    Text("+3 pts", fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Clean Sheet (GK)")
                    Text("+4 pts", fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Clean Sheet (DEF)")
                    Text("+3 pts", fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Clean Sheet (MID)")
                    Text("+1 pt", fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Penalty Saved (GK)")
                    Text("+5 pts", fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Penalty Missed")
                    Text("-2 pts", fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Penalty Earned")
                    Text("+3 pts", fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Saves (GK)")
                    Text("+1 pt / 3 saves", fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Yellow Card")
                    Text("-1 pt", fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Red Card")
                    Text("-3 pts", fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Own Goal conceded")
                    Text("-2 pts", fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("MOTM / POTM")
                    Text("+3 pts", fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("2nd Best Player")
                    Text("+2 pts", fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("3rd Best Player")
                    Text("+1 pt", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// AUXILIARY HELPERS SQUAD COMPILERS
// ──────────────────────────────────────────────
private fun fillDefaultEmptySlots(squadMap: MutableMap<String, String?>, formation: String) {
    squadMap["GK-1"] = null
    val split = formation.split("-").map { it.toInt() }
    for (i in 1..split[0]) squadMap["DEF-$i"] = null
    for (i in 1..split[1]) squadMap["MID-$i"] = null
    for (i in 1..split[2]) squadMap["FWD-$i"] = null
}

private fun adjustSquadSlotsForFormation(squadMap: MutableMap<String, String?>, newFormation: String) {
    val backup = squadMap.toMap()
    squadMap.clear()
    
    // Fill empty keys first
    fillDefaultEmptySlots(squadMap, newFormation)
    
    // Re-draft matches where possible
    val backupGks = backup.filterKeys { it.startsWith("GK") }.values.filterNotNull().toList()
    val backupDefs = backup.filterKeys { it.startsWith("DEF") }.values.filterNotNull().toList()
    val backupMids = backup.filterKeys { it.startsWith("MID") }.values.filterNotNull().toList()
    val backupFwds = backup.filterKeys { it.startsWith("FWD") }.values.filterNotNull().toList()

    var gkIdx = 0
    var defIdx = 0
    var midIdx = 0
    var fwdIdx = 0

    squadMap.keys.sorted().forEach { key ->
        if (key.startsWith("GK")) {
            if (gkIdx < backupGks.size) squadMap[key] = backupGks[gkIdx++]
        } else if (key.startsWith("DEF")) {
            if (defIdx < backupDefs.size) squadMap[key] = backupDefs[defIdx++]
        } else if (key.startsWith("MID")) {
            if (midIdx < backupMids.size) squadMap[key] = backupMids[midIdx++]
        } else if (key.startsWith("FWD")) {
            if (fwdIdx < backupFwds.size) squadMap[key] = backupFwds[fwdIdx++]
        }
    }
}

private fun evaluateSquadErrors(
    squadPlayers: Map<String, String?>,
    captainKey: String?,
    viceCaptainKey: String?,
    spentBudget: Double,
    players: List<Player>,
    teams: List<Team>
): List<String> {
    val errors = mutableListOf<String>()

    if (squadPlayers.values.count { it != null } < 11) {
        errors.add("Draft size incomplete (Need exactly 11 players)")
    }

    if (spentBudget > 100.0) {
        errors.add("Budget exceeded (Maximum limit: 100.0M)")
    }

    if (captainKey == null) {
        errors.add("Select a Captain (C)")
    } else if (squadPlayers[captainKey] == null) {
        errors.add("Captain slot must contain a selected player")
    }

    if (viceCaptainKey == null) {
        errors.add("Select a Vice-Captain (V)")
    } else if (squadPlayers[viceCaptainKey] == null) {
        errors.add("Vice-Captain slot must contain a selected player")
    }

    // Max 5 players check from same team
    val teamCounts = mutableMapOf<String, Int>()
    squadPlayers.values.filterNotNull().forEach { pId ->
        val p = players.firstOrNull { it.id == pId }
        val tId = p?.team?.firstOrNull()
        if (tId != null) {
            teamCounts[tId] = (teamCounts[tId] ?: 0) + 1
        }
    }

    teamCounts.forEach { (tId, count) ->
        if (count > 5) {
            val team = teams.firstOrNull { it.id == tId }
            val tName = team?.displayName ?: "same team"
            errors.add("Too many players from $tName (Limit max 5)")
        }
    }

    return errors
}
