package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.SafeComposeTextToolbar
import com.example.data.FriendItemUi
import com.example.data.GameModeType
import com.example.data.SupabaseSchemaSql
import com.example.data.TimeControlOption
import com.example.data.UserProfileDto
import com.example.engine.AiDifficulty
import com.example.ui.theme.DangerRose
import com.example.ui.theme.GoldAccentRestrained
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianSlate
import com.example.ui.theme.PlatinumLight
import com.example.ui.theme.PlayfairDisplayFontFamily
import com.example.ui.theme.PlusJakartaSansFontFamily
import com.example.ui.theme.StateSuccess
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.SurfaceGround
import com.example.ui.theme.TextMuted
import com.example.ui.theme.StateInteractiveText
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.abs

/**
 * In-window bottom sheet overlay that renders inside the main Activity window (no separate Android
 * Dialog window), preventing Material3 ModalBottomSheet scrim/focus deadlocks, nested scroll locks,
 * and DecorView FloatingActionMode crashes.
 */
@Composable
fun GrandmasterSheetOverlay(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    BackHandler(onBack = onDismiss)
    val sheetFocusManager = LocalFocusManager.current
    val safeSheetToolbar = remember { SafeComposeTextToolbar() }
    DisposableEffect(Unit) {
        onDispose { sheetFocusManager.clearFocus(force = true) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
            .statusBarsPadding()
            .padding(top = 36.dp)
            .imePadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            color = SurfaceCard,
            contentColor = TextPrimary,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .border(
                    width = 1.dp,
                    color = SurfaceBorder,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
        ) {
            CompositionLocalProvider(LocalTextToolbar provides safeSheetToolbar) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 2.dp, start = 16.dp, end = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .width(38.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(SurfaceBorder)
                        )
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(36.dp)
                                .testTag("sheet_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

/**
 * 1. NEW MATCH & PLAYER SELECTION MODAL SHEET
 * Supports AI Opponent (4 Difficulty Levels), Online Match (Friend selection, @username invite,
 * Share Link, or QR Code), and Local Pass & Play.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewMatchSetupSheet(
    currentUser: UserProfileDto,
    preselectedOpponent: UserProfileDto?,
    initialMode: GameModeType,
    initialAiDifficulty: AiDifficulty,
    friends: List<FriendItemUi>,
    onStartMatch: (
        opponent: UserProfileDto?,
        modeType: GameModeType,
        aiDifficulty: AiDifficulty,
        timeControl: TimeControlOption,
        playAsWhite: Boolean
    ) -> Unit,
    onInviteByUsername: (
        username: String,
        timeControl: TimeControlOption,
        playAsWhite: Boolean
    ) -> Unit,
    onStartMatchFromLinkOrQr: (
        linkOrCode: String,
        timeControl: TimeControlOption
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var selectedMode by remember { mutableStateOf(initialMode) }
    var selectedAiDifficulty by remember { mutableStateOf(initialAiDifficulty) }
    var selectedFriend by remember {
        mutableStateOf(
            preselectedOpponent
                ?: friends.firstOrNull { !it.isPendingIncoming }?.profile
                ?: friends.firstOrNull()?.profile
        )
    }
    var selectedTimeControl by remember { mutableStateOf(TimeControlOption.RAPID_10_0) }
    var playAsWhite by remember { mutableStateOf(true) }

    // Sub-tab when Online mode is selected: 0 = Friends / @Username, 1 = Share Link, 2 = QR Code
    var onlineInviteTab by remember { mutableIntStateOf(0) }
    var customUsernameInput by remember { mutableStateOf("") }
    var joinLinkOrCodeInput by remember { mutableStateOf("") }
    var linkCopiedToast by remember { mutableStateOf(false) }

    val personalInviteCode = remember(currentUser.username) {
        "CWF-${currentUser.username.uppercase().take(4)}-842"
    }
    val personalShareLink = remember(currentUser.username, selectedTimeControl) {
        "https://chesswithfriends.app/join/$personalInviteCode?host=${currentUser.username}&tc=${selectedTimeControl.shortBadge}"
    }

    GrandmasterSheetOverlay(onDismiss = onDismiss) {
        Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Start New Chess Match",
                fontFamily = PlayfairDisplayFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = TextPrimary
            )

            // Step 1: Select Opponent Mode
            Text(
                text = "1. SELECT OPPONENT MODE",
                fontFamily = PlusJakartaSansFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.2.sp,
                color = GoldAccentRestrained
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (mode in GameModeType.entries) {
                    val isSelected = selectedMode == mode
                    Surface(
                        color = if (isSelected) PlatinumLight else SurfaceElevated,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                1.dp,
                                if (isSelected) PlatinumLight else SurfaceBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedMode = mode }
                            .testTag("mode_select_${mode.name}")
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = when (mode) {
                                    GameModeType.AI_OPPONENT -> Icons.Default.SmartToy
                                    GameModeType.ONLINE_SUPABASE -> Icons.Default.Group
                                    GameModeType.PASS_AND_PLAY -> Icons.Default.SportsEsports
                                },
                                contentDescription = mode.label,
                                tint = if (isSelected) StateInteractiveText else GoldAccentRestrained,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) StateInteractiveText else TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Step 2A: AI Opponent -> Choose AI Difficulty Level
            if (selectedMode == GameModeType.AI_OPPONENT) {
                Text(
                    text = "2. SELECT AI DIFFICULTY LEVEL",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.2.sp,
                    color = GoldAccentRestrained
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (diff in AiDifficulty.entries) {
                        val isSelected = selectedAiDifficulty == diff
                        Surface(
                            color = if (isSelected) SurfaceElevated else SurfaceGround,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.5.dp,
                                    if (isSelected) GoldAccentRestrained else SurfaceBorder,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedAiDifficulty = diff }
                                .testTag("sheet_ai_difficulty_${diff.dbKey}")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = diff.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Surface(
                                            color = SurfaceCard,
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.border(
                                                1.dp,
                                                SurfaceBorder,
                                                RoundedCornerShape(6.dp)
                                            )
                                        ) {
                                            Text(
                                                text = "${diff.eloRating} ELO • Depth ${diff.searchDepth}",
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontSize = 11.sp,
                                                color = GoldAccentRestrained,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = diff.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = GoldAccentRestrained
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Step 2B: Online Match -> Invite by Username, Share Link, or QR Code
            if (selectedMode == GameModeType.ONLINE_SUPABASE) {
                Text(
                    text = "2. INVITE OR SELECT OPPONENT",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.2.sp,
                    color = GoldAccentRestrained
                )

                // Sub-tabs: Username / Friends | Share Link | QR Code
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tabs = listOf(
                        Triple(0, "@Username", Icons.Default.AlternateEmail),
                        Triple(1, "Share Link", Icons.Default.Link),
                        Triple(2, "QR Code", Icons.Default.QrCode2)
                    )
                    for ((index, label, icon) in tabs) {
                        val selected = onlineInviteTab == index
                        Surface(
                            color = if (selected) SurfaceElevated else SurfaceGround,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(
                                    1.dp,
                                    if (selected) GoldAccentRestrained else SurfaceBorder,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { onlineInviteTab = index }
                                .testTag("new_match_invite_tab_$index")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (selected) GoldAccentRestrained else TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) TextPrimary else TextSecondary,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                when (onlineInviteTab) {
                    0 -> {
                        // Direct @username invite input
                        Surface(
                            color = SurfaceGround,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Invite Player by @Username",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = customUsernameInput,
                                        onValueChange = { customUsernameInput = it },
                                        placeholder = {
                                            Text("e.g. @magnus_carlsen", color = TextMuted)
                                        },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = GoldAccentRestrained,
                                            unfocusedBorderColor = SurfaceBorder,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("sheet_username_invite_input")
                                    )
                                    Button(
                                        onClick = {
                                            val target = customUsernameInput.trim()
                                            if (target.isNotEmpty()) {
                                                onInviteByUsername(
                                                    target,
                                                    selectedTimeControl,
                                                    playAsWhite
                                                )
                                            }
                                        },
                                        enabled = customUsernameInput.isNotBlank(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = PlatinumLight,
                                            contentColor = StateInteractiveText
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .height(52.dp)
                                            .testTag("sheet_btn_invite_username")
                                    ) {
                                        Text("Invite", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Friend list selection
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (friendItem in friends) {
                                val prof = friendItem.profile
                                val isSelected = selectedFriend?.id == prof.id
                                Surface(
                                    color = if (isSelected) SurfaceElevated else SurfaceGround,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            1.dp,
                                            if (isSelected) GoldAccentRestrained else SurfaceBorder,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { selectedFriend = prof }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = prof.displayName,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = TextPrimary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "@${prof.username} • ${prof.eloRating} ELO",
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontSize = 11.sp,
                                                color = TextSecondary
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = GoldAccentRestrained
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        ShareLinkInvitePanel(
                            shareLink = personalShareLink,
                            inviteCode = personalInviteCode,
                            joinInput = joinLinkOrCodeInput,
                            onJoinInputChanged = { joinLinkOrCodeInput = it },
                            copiedToast = linkCopiedToast,
                            onCopyLink = {
                                clipboard.setText(AnnotatedString(personalShareLink))
                                linkCopiedToast = true
                            },
                            onNativeShare = {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Play me in Chess With Friends! Join my match: $personalShareLink"
                                    )
                                }
                                context.startActivity(
                                    Intent.createChooser(sendIntent, "Share Chess Match Invite")
                                )
                            },
                            onStartMatchFromLink = { link ->
                                onStartMatchFromLinkOrQr(link, selectedTimeControl)
                            }
                        )
                    }

                    2 -> {
                        QrCodeInvitePanel(
                            shareLink = personalShareLink,
                            inviteCode = personalInviteCode,
                            hostUsername = currentUser.username,
                            onStartQrMatch = { code ->
                                onStartMatchFromLinkOrQr(code, selectedTimeControl)
                            }
                        )
                    }
                }
            }

            // Step 3: Select Time Control (2 rows of 3 options including No Clock / Off)
            Text(
                text = "3. TIME CONTROL FORMAT",
                fontFamily = PlusJakartaSansFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.2.sp,
                color = GoldAccentRestrained
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TimeControlOption.entries.chunked(3).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (tc in rowItems) {
                            val isSelected = selectedTimeControl == tc
                            Surface(
                                color = if (isSelected) PlatinumLight else SurfaceGround,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        1.dp,
                                        if (isSelected) PlatinumLight else SurfaceBorder,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedTimeControl = tc }
                                    .testTag("new_match_tc_${tc.dbKey}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = tc.shortBadge,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 12.sp,
                                        color = if (isSelected) StateInteractiveText else TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = tc.label.substringBefore(" "),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) StateInteractiveText.copy(alpha = 0.75f) else TextSecondary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Step 4: Piece Color Selection
            Text(
                text = "4. YOUR PIECE COLOR",
                fontFamily = PlusJakartaSansFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.2.sp,
                color = GoldAccentRestrained
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(true to "Play as White (♔)", false to "Play as Black (♚)").forEach { (isWhite, label) ->
                    val selected = playAsWhite == isWhite
                    Surface(
                        color = if (selected) SurfaceElevated else SurfaceGround,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                1.dp,
                                if (selected) GoldAccentRestrained else SurfaceBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { playAsWhite = isWhite }
                            .padding(vertical = 12.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) GoldAccentRestrained else TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    if (selectedMode == GameModeType.ONLINE_SUPABASE && customUsernameInput.isNotBlank()) {
                        onInviteByUsername(customUsernameInput.trim(), selectedTimeControl, playAsWhite)
                    } else {
                        onStartMatch(
                            selectedFriend,
                            selectedMode,
                            selectedAiDifficulty,
                            selectedTimeControl,
                            playAsWhite
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PlatinumLight,
                    contentColor = StateInteractiveText
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_confirm_start_match")
            ) {
                Text(
                    text = when (selectedMode) {
                        GameModeType.AI_OPPONENT ->
                            "Start Match vs ${selectedAiDifficulty.title}"
                        GameModeType.PASS_AND_PLAY ->
                            "Start Local Pass & Play Match"
                        GameModeType.ONLINE_SUPABASE ->
                            if (customUsernameInput.isNotBlank()) {
                                "Invite @${customUsernameInput.trim().removePrefix("@")} & Start Match"
                            } else {
                                "Send Challenge & Start Match"
                            }
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(16.dp))
        }
        }
    }
}

/**
 * 2. NOTIFICATIONS, MATCH INVITES, SHARE LINKS, QR CODES & USERNAME SEARCH SHEET
 * Opened from the Bell icon in the header.
 * - Accepting an incoming match invite immediately creates a new match and adds it to the Matches screen.
 * - Users can also start a new match via Share Links, QR Codes, or inviting by @username.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendSearchDrawerSheet(
    currentUser: UserProfileDto,
    friends: List<FriendItemUi>,
    searchQuery: String,
    searchResults: List<UserProfileDto>,
    onQueryChanged: (String) -> Unit,
    onSendFriendInvite: (UserProfileDto) -> Unit,
    onAcceptFriendInvite: (String) -> Unit,
    onInviteByUsername: (String) -> Unit,
    onStartMatchFromLinkOrQr: (String) -> Unit,
    onChallengeFriend: (UserProfileDto) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var selectedInviteMethodTab by remember { mutableIntStateOf(0) } // 0 = @Username, 1 = Share Link, 2 = QR Code
    var directUsernameInput by remember { mutableStateOf("") }
    var joinLinkOrCodeInput by remember { mutableStateOf("") }
    var copiedShareLink by remember { mutableStateOf(false) }

    val pendingInvites = remember(friends) { friends.filter { it.isPendingIncoming } }
    val acceptedFriends = remember(friends) { friends.filter { !it.isPendingIncoming } }

    val personalInviteCode = remember(currentUser.username) {
        "CWF-${currentUser.username.uppercase().take(4)}-842"
    }
    val personalShareLink = remember(currentUser.username) {
        "https://chesswithfriends.app/join/$personalInviteCode?host=${currentUser.username}"
    }

    GrandmasterSheetOverlay(onDismiss = onDismiss) {
        Box {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .testTag("notifications_and_invites_sheet"),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 6.dp, bottom = 32.dp)
        ) {
            item {
                Text(
                    text = "Invitations & Match Lobby",
                    fontFamily = PlayfairDisplayFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = TextPrimary
                )
                Text(
                    text = "Accept incoming match invites, or start a new match using a username, share link, or QR code.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            // ---------------- SECTION A: INCOMING MATCH INVITATIONS (NOTIFICATIONS) ----------------
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = GoldAccentRestrained,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "INCOMING MATCH INVITES (${pendingInvites.size})",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.2.sp,
                        color = GoldAccentRestrained
                    )
                }
            }

            if (pendingInvites.isEmpty()) {
                item {
                    Surface(
                        color = SurfaceGround,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                    ) {
                        Text(
                            text = "All incoming match invitations have been accepted and added to your Matches screen.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            } else {
                items(pendingInvites, key = { it.friendshipId }) { item ->
                    Surface(
                        color = SurfaceElevated,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, GoldAccentRestrained.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                            .testTag("pending_invite_card_${item.friendshipId}")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = item.profile.displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Surface(
                                        color = SurfaceCard,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "10m Rapid",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 10.sp,
                                            color = GoldAccentRestrained,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "@${item.profile.username} • ${item.profile.eloRating} ELO • Invited you to a match",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }

                            Spacer(Modifier.width(10.dp))

                            Button(
                                onClick = { onAcceptFriendInvite(item.friendshipId) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PlatinumLight,
                                    contentColor = StateInteractiveText
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("btn_accept_invite_${item.friendshipId}")
                            ) {
                                Text(
                                    text = "Accept Invite",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // ---------------- SECTION B: START MATCH VIA USERNAME, SHARE LINK, OR QR CODE ----------------
            item {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "START NEW MATCH WITH OTHERS",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.2.sp,
                    color = GoldAccentRestrained
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val methods = listOf(
                        Triple(0, "@Username", Icons.Default.AlternateEmail),
                        Triple(1, "Share Link", Icons.Default.Link),
                        Triple(2, "QR Code", Icons.Default.QrCode2)
                    )
                    for ((idx, label, icon) in methods) {
                        val isSelected = selectedInviteMethodTab == idx
                        Surface(
                            color = if (isSelected) PlatinumLight else SurfaceGround,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(
                                    1.dp,
                                    if (isSelected) PlatinumLight else SurfaceBorder,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedInviteMethodTab = idx }
                                .testTag("drawer_invite_tab_$idx")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (isSelected) StateInteractiveText else GoldAccentRestrained,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) StateInteractiveText else TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            when (selectedInviteMethodTab) {
                0 -> {
                    // Direct @username invite + search directory
                    item {
                        Surface(
                            color = SurfaceGround,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Invite by Username to Start a Match",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = directUsernameInput,
                                        onValueChange = {
                                            directUsernameInput = it
                                            onQueryChanged(it.removePrefix("@"))
                                        },
                                        placeholder = {
                                            Text("Enter @username (e.g. hikaru)", color = TextMuted)
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Search,
                                                contentDescription = null,
                                                tint = GoldAccentRestrained
                                            )
                                        },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = GoldAccentRestrained,
                                            unfocusedBorderColor = SurfaceBorder,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("friend_search_input")
                                    )

                                    Button(
                                        onClick = {
                                            val target = directUsernameInput.trim()
                                            if (target.isNotEmpty()) {
                                                onInviteByUsername(target)
                                                directUsernameInput = ""
                                            }
                                        },
                                        enabled = directUsernameInput.isNotBlank(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = PlatinumLight,
                                            contentColor = StateInteractiveText
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .height(54.dp)
                                            .testTag("btn_invite_by_username")
                                    ) {
                                        Text("Start Match", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    if (acceptedFriends.isNotEmpty()) {
                        item {
                            Text(
                                text = "YOUR FRIENDS",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 1.2.sp,
                                color = TextSecondary
                            )
                        }

                        items(acceptedFriends, key = { it.friendshipId }) { item ->
                            Surface(
                                color = SurfaceGround,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.profile.displayName,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "@${item.profile.username} • ${item.profile.eloRating} ELO",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilledTonalButton(
                                            onClick = { onInviteByUsername(item.profile.username) },
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = SurfaceElevated,
                                                contentColor = PlatinumLight
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.testTag("btn_quick_match_${item.profile.username}")
                                        ) {
                                            Text("Quick Match")
                                        }
                                        OutlinedButton(
                                            onClick = { onChallengeFriend(item.profile) },
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("Setup", color = GoldAccentRestrained)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "PLAYER DIRECTORY",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.2.sp,
                            color = TextSecondary
                        )
                    }

                    items(searchResults, key = { it.id }) { prof ->
                        Surface(
                            color = SurfaceGround,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = prof.displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "@${prof.username} • ${prof.eloRating} ELO",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }

                                Button(
                                    onClick = { onSendFriendInvite(prof) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PlatinumLight,
                                        contentColor = StateInteractiveText
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("btn_invite_player_${prof.username}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PersonAdd,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("Invite & Play", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                1 -> {
                    item {
                        ShareLinkInvitePanel(
                            shareLink = personalShareLink,
                            inviteCode = personalInviteCode,
                            joinInput = joinLinkOrCodeInput,
                            onJoinInputChanged = { joinLinkOrCodeInput = it },
                            copiedToast = copiedShareLink,
                            onCopyLink = {
                                clipboard.setText(AnnotatedString(personalShareLink))
                                copiedShareLink = true
                            },
                            onNativeShare = {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Play me in Chess With Friends! Join my match: $personalShareLink"
                                    )
                                }
                                context.startActivity(
                                    Intent.createChooser(sendIntent, "Share Chess Match Invite")
                                )
                            },
                            onStartMatchFromLink = { link ->
                                onStartMatchFromLinkOrQr(link)
                            }
                        )
                    }
                }

                2 -> {
                    item {
                        QrCodeInvitePanel(
                            shareLink = personalShareLink,
                            inviteCode = personalInviteCode,
                            hostUsername = currentUser.username,
                            onStartQrMatch = { code ->
                                onStartMatchFromLinkOrQr(code)
                            }
                        )
                    }
                }
            }
        }
        }
    }
}

/**
 * Reusable Share Link Invite & Join Panel
 */
@Composable
private fun ShareLinkInvitePanel(
    shareLink: String,
    inviteCode: String,
    joinInput: String,
    onJoinInputChanged: (String) -> Unit,
    copiedToast: Boolean,
    onCopyLink: () -> Unit,
    onNativeShare: () -> Unit,
    onStartMatchFromLink: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Card 1: Your Personal Share Link
        Surface(
            color = SurfaceGround,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Your Match Invite Link",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = SurfaceElevated,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = inviteCode,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = GoldAccentRestrained,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Surface(
                    color = SurfaceCard,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(10.dp))
                ) {
                    Text(
                        text = shareLink,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onCopyLink,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_copy_share_link")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = GoldAccentRestrained,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (copiedToast) "Copied!" else "Copy Link",
                            color = TextPrimary
                        )
                    }

                    Button(
                        onClick = onNativeShare,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PlatinumLight,
                            contentColor = StateInteractiveText
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_share_link_intent")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Share Link", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Card 2: Join or Start a Match from a Friend's Share Link
        Surface(
            color = SurfaceGround,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Open or Paste a Share Link to Start Match",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Paste a friend's invite URL or invite code below to add the match to your Matches screen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                val clipboardManager = LocalClipboardManager.current
                val focusManager = LocalFocusManager.current
                OutlinedTextField(
                    value = joinInput,
                    onValueChange = onJoinInputChanged,
                    placeholder = {
                        Text(
                            "https://chesswithfriends.app/join/CWF-904?host=nadia_k",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                val clipText = clipboardManager.getText()?.text?.trim().orEmpty()
                                if (clipText.isNotEmpty()) {
                                    onJoinInputChanged(clipText)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = "Paste invite link",
                                tint = GoldAccentRestrained,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldAccentRestrained,
                        unfocusedBorderColor = SurfaceBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("join_link_input")
                )

                Button(
                    onClick = {
                        focusManager.clearFocus(force = true)
                        val linkToUse = joinInput.ifBlank {
                            "https://chesswithfriends.app/join/CWF-904?host=nadia_k"
                        }
                        onStartMatchFromLink(linkToUse)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PlatinumLight,
                        contentColor = StateInteractiveText
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_join_by_link")
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (joinInput.isBlank()) {
                            "Accept Shared Link & Start Match"
                        } else {
                            "Join Match from Link"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Reusable QR Code Match Invite & Scan Panel with 2D Jetpack Compose Canvas QR Matrix
 */
@Composable
private fun QrCodeInvitePanel(
    shareLink: String,
    inviteCode: String,
    hostUsername: String,
    onStartQrMatch: (String) -> Unit
) {
    var customQrToken by remember { mutableStateOf("") }

    Surface(
        color = SurfaceGround,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Scan QR Code to Start Match",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Have your opponent scan this QR code, or scan theirs to immediately start a match.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            // High-contrast 2D Compose Canvas QR Code
            Box(
                modifier = Modifier
                    .size(188.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(PlatinumLight)
                    .border(2.dp, GoldAccentRestrained, RoundedCornerShape(16.dp))
                    .padding(14.dp)
                    .testTag("qr_code_canvas_box"),
                contentAlignment = Alignment.Center
            ) {
                InviteQrCodeCanvas(
                    payload = shareLink,
                    modifier = Modifier.size(160.dp)
                )
            }

            Text(
                text = "INVITE CODE: $inviteCode • HOST: @$hostUsername",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                color = GoldAccentRestrained,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = customQrToken,
                onValueChange = { customQrToken = it },
                placeholder = {
                    Text("Optional: Enter scanned QR code or @username", color = TextMuted, fontSize = 12.sp)
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldAccentRestrained,
                    unfocusedBorderColor = SurfaceBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("qr_code_input")
            )

            Button(
                onClick = {
                    val code = customQrToken.ifBlank {
                        "https://chesswithfriends.app/join/QR-772?host=viktor_korchnoi"
                    }
                    onStartQrMatch(code)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PlatinumLight,
                    contentColor = StateInteractiveText
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_start_qr_match")
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Scan / Accept QR Code & Start Match",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Custom 2D Jetpack Compose Canvas QR Code renderer.
 * Renders standard 21x21 Version-1 QR finder patterns, timing strips, and deterministic payload modules.
 */
@Composable
fun InviteQrCodeCanvas(
    payload: String,
    modifier: Modifier = Modifier
) {
    val gridSize = 21
    val seed = remember(payload) { payload.hashCode() }

    Canvas(modifier = modifier) {
        val cellSize = size.width / gridSize

        fun isFinderPattern(r: Int, c: Int, topR: Int, leftC: Int): Boolean {
            val dr = r - topR
            val dc = c - leftC
            if (dr !in 0..6 || dc !in 0..6) return false
            if (dr == 0 || dr == 6 || dc == 0 || dc == 6) return true
            if (dr in 2..4 && dc in 2..4) return true
            return false
        }

        fun isReservedQuietAroundFinder(r: Int, c: Int): Boolean {
            if (r in 0..7 && c in 0..7) return true
            if (r in 0..7 && c in (gridSize - 8) until gridSize) return true
            if (r in (gridSize - 8) until gridSize && c in 0..7) return true
            return false
        }

        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                val isDark = when {
                    isFinderPattern(r, c, 0, 0) -> true
                    isFinderPattern(r, c, 0, gridSize - 7) -> true
                    isFinderPattern(r, c, gridSize - 7, 0) -> true
                    isReservedQuietAroundFinder(r, c) -> false
                    r == 6 -> c % 2 == 0
                    c == 6 -> r % 2 == 0
                    else -> {
                        val charCode = payload[(r * gridSize + c) % payload.length].code
                        val hashMix = abs((r * 37 + c * 73 + seed xor (charCode * 19)))
                        hashMix % 10 < 5
                    }
                }

                if (isDark) {
                    drawRoundRect(
                        color = ObsidianSlate,
                        topLeft = Offset(c * cellSize, r * cellSize),
                        size = Size(cellSize * 0.92f, cellSize * 0.92f),
                        cornerRadius = CornerRadius(cellSize * 0.18f, cellSize * 0.18f)
                    )
                }
            }
        }
    }
}

/**
 * 3. SUPABASE AUTHENTICATION & PROFILE SHEET
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupabaseAuthSheet(
    currentUser: UserProfileDto,
    isSupabaseConfigured: Boolean,
    activeSupabaseUrl: String = "",
    activeSupabaseAnonKey: String = "",
    supabaseConnectionStatus: String? = null,
    isTestingConnection: Boolean = false,
    isAuthenticatedOnline: Boolean,
    authError: String?,
    onConfigureAndTestSupabase: (String, String) -> Unit = { _, _ -> },
    onSignIn: (String, String) -> Unit,
    onSignUp: (String, String, String, String) -> Unit,
    onSignOut: () -> Unit,
    onOpenSqlSchema: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var copiedDirectSql by remember { mutableStateOf(false) }
    var isRegisterMode by remember { mutableStateOf(false) }
    var showCredentialsConfig by remember(isSupabaseConfigured) { mutableStateOf(!isSupabaseConfigured) }
    var supabaseUrlInput by remember(activeSupabaseUrl) {
        mutableStateOf(
            if (activeSupabaseUrl.contains("your-project-id")) "" else activeSupabaseUrl
        )
    }
    var supabaseKeyInput by remember(activeSupabaseAnonKey) {
        mutableStateOf(
            if (activeSupabaseAnonKey.contains("your-supabase-anon")) "" else activeSupabaseAnonKey
        )
    }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }

    GrandmasterSheetOverlay(onDismiss = onDismiss) {
        Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (isRegisterMode) "Create Grandmaster Account" else "Profile & Supabase Connection",
                fontFamily = PlayfairDisplayFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = TextPrimary
            )

            // 1. SUPABASE CONNECTION STATUS, CREDENTIALS & SQL SCHEMA CARD
            Surface(
                color = SurfaceGround,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (isSupabaseConfigured) GoldAccentRestrained.copy(alpha = 0.6f) else SurfaceBorder,
                        RoundedCornerShape(12.dp)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Active Profile: ${currentUser.displayName} (@${currentUser.username})",
                                style = MaterialTheme.typography.titleSmall,
                                color = GoldAccentRestrained,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isSupabaseConfigured) {
                                    "Project: $activeSupabaseUrl"
                                } else {
                                    "Status: Not Connected to Supabase (Using Local Mode)"
                                },
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = if (isSupabaseConfigured) StateSuccess else TextSecondary
                            )
                        }
                        TextButton(
                            onClick = { showCredentialsConfig = !showCredentialsConfig }
                        ) {
                            Text(
                                text = if (showCredentialsConfig) "Hide Setup" else "Configure Keys",
                                fontSize = 11.5.sp,
                                color = GoldAccentRestrained,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (showCredentialsConfig) {
                        Text(
                            text = "Paste your Supabase Project URL and Anon Public Key below (from Supabase Dashboard → Project Settings → API), or set SUPABASE_URL and SUPABASE_ANON_KEY in the AI Studio Secrets panel.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        OutlinedTextField(
                            value = supabaseUrlInput,
                            onValueChange = { supabaseUrlInput = it },
                            label = { Text("Supabase Project URL (https://xyz.supabase.co)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_supabase_url")
                        )

                        OutlinedTextField(
                            value = supabaseKeyInput,
                            onValueChange = { supabaseKeyInput = it },
                            label = { Text("Supabase Anon / Public Key") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_supabase_anon_key")
                        )

                        Button(
                            onClick = {
                                onConfigureAndTestSupabase(supabaseUrlInput.trim(), supabaseKeyInput.trim())
                            },
                            enabled = !isTestingConnection,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PlatinumLight,
                                contentColor = StateInteractiveText
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_test_supabase_connection")
                        ) {
                            Text(
                                text = if (isTestingConnection) {
                                    "Testing Supabase Connection..."
                                } else {
                                    "Connect & Test Supabase"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                onConfigureAndTestSupabase(
                                    supabaseUrlInput.ifBlank { activeSupabaseUrl },
                                    supabaseKeyInput.ifBlank { activeSupabaseAnonKey }
                                )
                            },
                            enabled = !isTestingConnection,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_test_supabase_connection")
                        ) {
                            Text(
                                text = if (isTestingConnection) "Testing Connection..." else "Test Supabase Connection",
                                color = GoldAccentRestrained,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (supabaseConnectionStatus != null) {
                        Surface(
                            color = SurfaceElevated,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(0.5.dp, StateSuccess.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        ) {
                            Text(
                                text = supabaseConnectionStatus,
                                style = MaterialTheme.typography.bodySmall,
                                color = StateSuccess,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(SupabaseSchemaSql.FULL_POSTGRES_SCHEMA))
                                copiedDirectSql = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("auth_sheet_copy_sql_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = GoldAccentRestrained,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (copiedDirectSql) "SQL Copied!" else "Copy SQL Commands",
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        OutlinedButton(
                            onClick = onOpenSqlSchema,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("auth_sheet_view_sql_button")
                        ) {
                            Text(
                                text = "View SQL",
                                color = GoldAccentRestrained,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (authError != null) {
                Surface(
                    color = SurfaceGround,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, DangerRose.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                ) {
                    Text(
                        text = authError,
                        style = MaterialTheme.typography.bodySmall,
                        color = DangerRose,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_email_input")
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_password_input")
            )

            if (isRegisterMode) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username (e.g. magnus_c)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Button(
                onClick = {
                    if (isRegisterMode) {
                        onSignUp(email, password, username, displayName)
                    } else {
                        onSignIn(email, password)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PlatinumLight,
                    contentColor = StateInteractiveText
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("auth_submit_button")
            ) {
                Text(
                    text = if (isRegisterMode) "Register & Sync Profile" else "Sign In",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = { isRegisterMode = !isRegisterMode }) {
                    Text(
                        text = if (isRegisterMode) "Already have an account? Sign In" else "Need an account? Sign Up",
                        color = GoldAccentRestrained
                    )
                }

                if (isAuthenticatedOnline) {
                    TextButton(onClick = onSignOut) {
                        Text("Sign Out", color = DangerRose)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
        }
    }
}

/**
 * 4. SUPABASE POSTGRESQL SCHEMA & RLS VIEWER SHEET
 */
@Composable
fun SupabaseSqlSchemaSheet(
    onDismiss: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    GrandmasterSheetOverlay(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PostgreSQL Schema & RLS",
                        fontFamily = PlayfairDisplayFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "Paste into your Supabase SQL Editor to provision profiles, friendships, games, moves, and Realtime publications.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        clipboard.setText(AnnotatedString(SupabaseSchemaSql.FULL_POSTGRES_SCHEMA))
                        copied = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PlatinumLight,
                        contentColor = StateInteractiveText
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("btn_copy_sql_schema")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (copied) "Copied!" else "Copy SQL", fontWeight = FontWeight.Bold)
                }
            }

            Surface(
                color = SurfaceGround,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp))
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp)
                ) {
                    Text(
                        text = SupabaseSchemaSql.FULL_POSTGRES_SCHEMA,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFontFamily,
                        color = GoldAccentRestrained
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * 5. TIME CLOCK & HOROLOGY SETTINGS MENU SHEET
 * Allows configuring the chess time clock:
 * - Turning the time clock completely OFF (Unlimited / untimed mode) or ON
 * - Selecting standard FIDE/online presets (No Clock, Blitz 3+2, Blitz 5+0, Rapid 10+0, Classical 15+10, Daily 24h)
 * - Configuring custom base minutes (1–180 min) and Fischer increment seconds (0–60 sec)
 * - Applying changes immediately to the active match as well as future matches
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockSettingsSheet(
    isClockEnabled: Boolean,
    currentTimeControl: TimeControlOption,
    useCustomClockDuration: Boolean,
    customClockMinutes: Int,
    customIncrementSeconds: Int,
    hasActiveMatch: Boolean,
    onToggleClockEnabled: (Boolean) -> Unit,
    onApplyClockSettings: (
        enabled: Boolean,
        timeControl: TimeControlOption,
        useCustom: Boolean,
        customMinutes: Int,
        customIncrementSec: Int,
        applyToActiveMatch: Boolean
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var clockEnabled by remember(isClockEnabled) { mutableStateOf(isClockEnabled) }
    var selectedPreset by remember(currentTimeControl) {
        mutableStateOf(
            if (currentTimeControl == TimeControlOption.UNLIMITED && isClockEnabled) {
                TimeControlOption.RAPID_10_0
            } else {
                currentTimeControl
            }
        )
    }
    var useCustom by remember(useCustomClockDuration) { mutableStateOf(useCustomClockDuration) }
    var customMin by remember(customClockMinutes) { mutableIntStateOf(customClockMinutes.coerceIn(1, 180)) }
    var customInc by remember(customIncrementSeconds) { mutableIntStateOf(customIncrementSeconds.coerceIn(0, 60)) }
    var applyToActive by remember(hasActiveMatch) { mutableStateOf(hasActiveMatch) }

    GrandmasterSheetOverlay(onDismiss = onDismiss) {
        Box {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .testTag("clock_settings_sheet"),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Time Clock Settings",
                            fontFamily = PlayfairDisplayFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Configure match timers, Fischer increments, or turn the time clock off for untimed play.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Icon(
                        imageVector = if (clockEnabled) Icons.Default.Timer else Icons.Default.TimerOff,
                        contentDescription = null,
                        tint = if (clockEnabled) GoldAccentRestrained else TextMuted,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // 1. Master Time Clock On/Off Switch Card
                Surface(
                    color = SurfaceElevated,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (clockEnabled) GoldAccentRestrained.copy(alpha = 0.55f) else SurfaceBorder,
                            RoundedCornerShape(14.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (clockEnabled) "Time Clock Active" else "Time Clock Turned Off",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (clockEnabled) {
                                        "Digital countdown clock ticks during each player's turn."
                                    } else {
                                        "Untimed mode — clocks are disabled and matches never flag on time."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Switch(
                                checked = clockEnabled,
                                onCheckedChange = { enabled ->
                                    clockEnabled = enabled
                                    if (!enabled) {
                                        selectedPreset = TimeControlOption.UNLIMITED
                                        useCustom = false
                                    } else if (selectedPreset == TimeControlOption.UNLIMITED) {
                                        selectedPreset = TimeControlOption.RAPID_10_0
                                    }
                                    onToggleClockEnabled(enabled)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = StateInteractiveText,
                                    checkedTrackColor = PlatinumLight,
                                    uncheckedThumbColor = TextSecondary,
                                    uncheckedTrackColor = SurfaceGround
                                ),
                                modifier = Modifier.testTag("switch_clock_enabled")
                            )
                        }

                        // Quick One-Tap Turn Clock Off / Restore Timed Clock button
                        if (clockEnabled) {
                            OutlinedButton(
                                onClick = {
                                    clockEnabled = false
                                    selectedPreset = TimeControlOption.UNLIMITED
                                    useCustom = false
                                    onApplyClockSettings(
                                        false,
                                        TimeControlOption.UNLIMITED,
                                        false,
                                        customMin,
                                        customInc,
                                        applyToActive
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_turn_clock_off")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TimerOff,
                                    contentDescription = null,
                                    tint = GoldAccentRestrained,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Turn Time Clock Off (Unlimited Time)",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // 2. Preset Time Control Formats (including "No Clock (Off)")
                Text(
                    text = "PRESET TIME CONTROLS",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.2.sp,
                    color = GoldAccentRestrained
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimeControlOption.entries.chunked(2).forEach { rowPresets ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (preset in rowPresets) {
                                val isSelected = (!useCustom) && (
                                    (!clockEnabled && preset == TimeControlOption.UNLIMITED) ||
                                        (clockEnabled && selectedPreset == preset)
                                )
                                Surface(
                                    color = if (isSelected) PlatinumLight else SurfaceGround,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(
                                            1.dp,
                                            if (isSelected) PlatinumLight else SurfaceBorder,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            useCustom = false
                                            selectedPreset = preset
                                            clockEnabled = preset != TimeControlOption.UNLIMITED
                                        }
                                        .testTag("clock_preset_${preset.dbKey}")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = preset.shortBadge,
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (isSelected) StateInteractiveText else TextPrimary
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = StateInteractiveText,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = preset.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) StateInteractiveText.copy(alpha = 0.8f) else TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Custom Clock Duration & Fischer Increment Configurator
                Surface(
                    color = SurfaceGround,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (clockEnabled && useCustom) GoldAccentRestrained else SurfaceBorder,
                            RoundedCornerShape(14.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Custom Time Clock & Increment",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${customMin}m base time + ${customInc}s per move",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.5.sp,
                                    color = GoldAccentRestrained
                                )
                            }
                            Switch(
                                checked = clockEnabled && useCustom,
                                onCheckedChange = { checked ->
                                    useCustom = checked
                                    if (checked) {
                                        clockEnabled = true
                                        if (selectedPreset == TimeControlOption.UNLIMITED) {
                                            selectedPreset = TimeControlOption.RAPID_10_0
                                        }
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = StateInteractiveText,
                                    checkedTrackColor = GoldAccentRestrained,
                                    uncheckedThumbColor = TextSecondary,
                                    uncheckedTrackColor = SurfaceElevated
                                ),
                                modifier = Modifier.testTag("switch_custom_clock")
                            )
                        }

                        // Base Minutes Selector Chips
                        Text(
                            text = "BASE MINUTES PER PLAYER",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(1, 3, 5, 10, 15, 30, 60).forEach { mins ->
                                val selected = clockEnabled && useCustom && customMin == mins
                                Surface(
                                    color = if (selected) PlatinumLight else SurfaceElevated,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(
                                            1.dp,
                                            if (selected) PlatinumLight else SurfaceBorder,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            clockEnabled = true
                                            useCustom = true
                                            customMin = mins
                                        }
                                        .testTag("custom_min_chip_$mins")
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${mins}m",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = if (selected) StateInteractiveText else TextPrimary
                                        )
                                    }
                                }
                            }
                        }

                        // Increment Seconds Selector Chips
                        Text(
                            text = "FISCHER INCREMENT (SECONDS ADDED PER MOVE)",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(0, 2, 3, 5, 10, 15, 30).forEach { secs ->
                                val selected = clockEnabled && useCustom && customInc == secs
                                Surface(
                                    color = if (selected) PlatinumLight else SurfaceElevated,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(
                                            1.dp,
                                            if (selected) PlatinumLight else SurfaceBorder,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            clockEnabled = true
                                            useCustom = true
                                            customInc = secs
                                        }
                                        .testTag("custom_inc_chip_$secs")
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "+${secs}s",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = if (selected) StateInteractiveText else TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Apply to Current Active Match Toggle (if a match is open or active)
                if (hasActiveMatch) {
                    Surface(
                        color = SurfaceGround,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                            .clickable { applyToActive = !applyToActive }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Update Active Match Clock",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.5.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Immediately apply these time clock settings to the current match.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Switch(
                                checked = applyToActive,
                                onCheckedChange = { applyToActive = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = StateInteractiveText,
                                    checkedTrackColor = PlatinumLight
                                ),
                                modifier = Modifier.testTag("switch_apply_to_active_match")
                            )
                        }
                    }
                }

                // 5. Save & Apply CTA Button
                Button(
                    onClick = {
                        val effectivePreset = if (!clockEnabled) {
                            TimeControlOption.UNLIMITED
                        } else {
                            selectedPreset
                        }
                        onApplyClockSettings(
                            clockEnabled,
                            effectivePreset,
                            clockEnabled && useCustom,
                            customMin,
                            customInc,
                            applyToActive
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PlatinumLight,
                        contentColor = StateInteractiveText
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_save_clock_settings")
                ) {
                    Text(
                        text = when {
                            !clockEnabled -> "Save Settings (Time Clock Off)"
                            useCustom -> "Apply Custom Clock (${customMin}m + ${customInc}s)"
                            else -> "Apply ${selectedPreset.label}"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

