package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.GameModeType
import com.example.data.MatchSummaryUi
import com.example.data.TimeControlOption
import com.example.ui.ChessUiState
import com.example.ui.components.GrandmasterBottomNavBar
import com.example.ui.components.GrandmasterNavTab
import com.example.ui.components.GrandmasterTopAppBar
import com.example.ui.components.PlatinumActionButton
import com.example.ui.theme.GoldAccentRestrained
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PlayfairDisplayFontFamily
import com.example.ui.theme.PlusJakartaSansFontFamily
import com.example.ui.theme.StateActiveTurn
import com.example.ui.theme.StateSuccess
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.SurfaceGround
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WoodGrainDark
import com.example.ui.theme.WoodGrainHighlight
import com.example.ui.theme.WoodGrainMid
import com.example.ui.theme.WoodPerimeterBorder
import java.text.NumberFormat
import java.util.Locale

@Composable
fun DashboardScreen(
    uiState: ChessUiState,
    onOpenMatch: (MatchSummaryUi) -> Unit,
    onOpenNewMatchSheet: (GameModeType) -> Unit,
    onQuickStartPassAndPlay: () -> Unit,
    onOpenFriendDrawer: () -> Unit,
    onOpenAuthSheet: () -> Unit,
    onOpenSqlSchemaSheet: () -> Unit,
    onOpenClockSettingsSheet: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val pendingInvitesCount = uiState.friends.count { it.isPendingIncoming }

    val pulseTransition = rememberInfiniteTransition(label = "turn_beacon")
    val beaconAlpha by pulseTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beacon_alpha"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = SurfaceGround,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            GrandmasterBottomNavBar(
                activeTab = GrandmasterNavTab.MATCHES,
                onSelectTab = { tab ->
                    when (tab) {
                        GrandmasterNavTab.MATCHES -> {}
                        GrandmasterNavTab.LIVE -> {
                            val firstActive = uiState.yourTurnMatches.firstOrNull()
                                ?: uiState.awaitingOpponentMatches.firstOrNull()
                            if (firstActive != null) {
                                onOpenMatch(firstActive)
                            } else {
                                onOpenNewMatchSheet(GameModeType.ONLINE_SUPABASE)
                            }
                        }
                        GrandmasterNavTab.TACTICS -> onOpenNewMatchSheet(GameModeType.AI_OPPONENT)
                        GrandmasterNavTab.PROFILE -> onOpenAuthSheet()
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .testTag("dashboard_lazy_column"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Persistent Shell Header
                item {
                    GrandmasterTopAppBar(
                        isMatchScreen = false,
                        hasPendingInvites = pendingInvitesCount > 0,
                        onCrownOrBackClick = { onOpenNewMatchSheet(GameModeType.AI_OPPONENT) },
                        onSettingsClick = onOpenClockSettingsSheet,
                        onBellClick = onOpenFriendDrawer,
                        onAvatarClick = onOpenAuthSheet
                    )
                }

                // 2. Player Rating & Tier Bar (Alex Mercer | 1,420 ELO | TIER IV SCHOLAR)
                item {
                    GrandmasterPlayerRatingCard(
                        displayName = uiState.currentUser.displayName,
                        eloRating = uiState.currentUser.eloRating,
                        onProfileClick = onOpenAuthSheet,
                        onSqlSchemaClick = onOpenSqlSchemaSheet
                    )
                }

                // 3. Hero Banner Card ("NOCTURNE INVITATIONAL — Master Every Square")
                item {
                    NocturneInvitationalHeroCard(
                        onVsEngine = { onOpenNewMatchSheet(GameModeType.AI_OPPONENT) },
                        onPlayOnline = { onOpenNewMatchSheet(GameModeType.ONLINE_SUPABASE) },
                        onPassAndPlay = onQuickStartPassAndPlay
                    )
                }

                // 4. Status Banner Toast (if active)
                if (uiState.statusBannerMessage != null) {
                    item {
                        Surface(
                            color = SurfaceElevated,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = uiState.statusBannerMessage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "SQL SCHEMA",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldAccentRestrained,
                                    modifier = Modifier
                                        .clickable { onOpenSqlSchemaSheet() }
                                        .padding(start = 12.dp)
                                )
                            }
                        }
                    }
                }

                // 5. SECTION 1: YOUR TURN
                item {
                    GrandmasterSectionHeader(
                        title = "Your Turn",
                        badgeText = "${uiState.yourTurnMatches.size} Active",
                        rightCaption = "CLOCKS TICKING",
                        beaconAlpha = beaconAlpha
                    )
                }

                if (uiState.yourTurnMatches.isEmpty()) {
                    item {
                        EmptyMatchSlotCard(
                            label = "No active clocks awaiting your move.",
                            ctaText = "+ NEW MATCH",
                            onCtaClick = { onOpenNewMatchSheet(GameModeType.AI_OPPONENT) }
                        )
                    }
                } else {
                    itemsIndexed(
                        items = uiState.yourTurnMatches,
                        key = { _, m -> m.record.id }
                    ) { index, match ->
                        DirectionalWalnutMatchCard(
                            match = match,
                            cardIndex = index,
                            isUrgentYourTurn = true,
                            isClockEnabled = uiState.isClockEnabled,
                            onMakeMoveClick = { onOpenMatch(match) },
                            onInspectClick = { onOpenMatch(match) }
                        )
                    }
                }

                // 6. SECTION 2: AWAITING OPPONENT
                if (uiState.awaitingOpponentMatches.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(4.dp))
                        GrandmasterSectionHeader(
                            title = "Awaiting Opponent",
                            badgeText = "${uiState.awaitingOpponentMatches.size} Active",
                            rightCaption = "CORRESPONDENCE",
                            beaconAlpha = 0.35f
                        )
                    }

                    itemsIndexed(
                        items = uiState.awaitingOpponentMatches,
                        key = { _, m -> m.record.id }
                    ) { index, match ->
                        DirectionalWalnutMatchCard(
                            match = match,
                            cardIndex = index + 2,
                            isUrgentYourTurn = false,
                            isClockEnabled = uiState.isClockEnabled,
                            onMakeMoveClick = { onOpenMatch(match) },
                            onInspectClick = { onOpenMatch(match) }
                        )
                    }
                }

                // 7. SECTION 3: ARCHIVED & PAST GAMES
                if (uiState.archivedMatches.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(4.dp))
                        GrandmasterSectionHeader(
                            title = "Completed Archives",
                            badgeText = "${uiState.archivedMatches.size} Finished",
                            rightCaption = "PGN & FEN LOG",
                            beaconAlpha = 0.25f
                        )
                    }

                    itemsIndexed(
                        items = uiState.archivedMatches,
                        key = { _, m -> m.record.id }
                    ) { index, match ->
                        DirectionalWalnutMatchCard(
                            match = match,
                            cardIndex = index + 4,
                            isUrgentYourTurn = false,
                            isClockEnabled = uiState.isClockEnabled,
                            onMakeMoveClick = { onOpenMatch(match) },
                            onInspectClick = { onOpenMatch(match) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Matches the top player rating pill from Screenshot 1:
 * `[ • Alex Mercer | 1,420 ELO | 🎖 TIER IV SCHOLAR ]`
 */
@Composable
private fun GrandmasterPlayerRatingCard(
    displayName: String,
    eloRating: Int,
    onProfileClick: () -> Unit,
    onSqlSchemaClick: () -> Unit
) {
    val formattedElo = NumberFormat.getIntegerInstance(Locale.US).format(eloRating)
    val nameParts = displayName.trim().split(" ")
    val firstName = nameParts.firstOrNull() ?: displayName
    val lastName = nameParts.drop(1).joinToString(" ").ifEmpty { "Grandmaster" }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        SurfaceElevated,
                        SurfaceCard,
                        SurfaceGround
                    ),
                    start = Offset(1000f, 0f),
                    end = Offset(0f, 300f)
                )
            )
            .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
            .clickable { onProfileClick() }
            .padding(horizontal = 18.dp, vertical = 14.dp)
            .testTag("profile_header_button")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Luminous white dot + Two-line Name
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(StateActiveTurn)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = firstName,
                        fontFamily = PlusJakartaSansFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        lineHeight = 19.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = lastName,
                        fontFamily = PlusJakartaSansFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        lineHeight = 19.sp,
                        color = TextPrimary
                    )
                }
            }

            // Center: 1,420 ELO in JetBrains Mono
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(TextMuted)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = formattedElo,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        lineHeight = 20.sp,
                        letterSpacing = 0.8.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "ELO",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        lineHeight = 18.sp,
                        letterSpacing = 1.2.sp,
                        color = TextPrimary
                    )
                }
            }

            // Right: Medal Icon + TIER IV SCHOLAR + subtle SQL Schema trigger
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.MilitaryTech,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Column {
                    Text(
                        text = "TIER IV",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        letterSpacing = 1.2.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "SCHOLAR",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        letterSpacing = 1.2.sp,
                        color = TextPrimary
                    )
                }
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceElevated)
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp))
                        .clickable { onSqlSchemaClick() }
                        .testTag("sql_schema_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = "Supabase SQL & RLS Schema",
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * Matches the Hero Card from Screenshot 1:
 * `• NOCTURNE INVITATIONAL`
 * `Master Every Square`
 * `Asynchronous grandmaster correspondence, tactical mastery, and adaptive engine play.`
 * `[ VS ENGINE ] [ PLAY ONLINE ] [ PASS & PLAY ]`
 */
@Composable
private fun NocturneInvitationalHeroCard(
    onVsEngine: () -> Unit,
    onPlayOnline: () -> Unit,
    onPassAndPlay: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceCard)
            .border(1.dp, WoodPerimeterBorder.copy(alpha = 0.85f), RoundedCornerShape(18.dp))
    ) {
        // Background atmospheric chessboard photo
        Image(
            painter = painterResource(id = R.drawable.img_chess_hero_1793508283788),
            contentDescription = "Nocturne Invitational Chessboard",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(256.dp)
                .alpha(0.32f)
        )

        // 45° Directional Walnut & Obsidian Gradient Overlay per DESIGN.md Section 5
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(256.dp)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            WoodGrainHighlight.copy(alpha = 0.72f),
                            WoodGrainMid.copy(alpha = 0.65f),
                            SurfaceGround.copy(alpha = 0.96f)
                        ),
                        start = Offset(1000f, 0f),
                        end = Offset(0f, 750f)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Subtle top micro-label
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "MATCHES & LOBBY",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 8.5.sp,
                    letterSpacing = 1.8.sp,
                    color = TextMuted.copy(alpha = 0.75f)
                )
            }

            Spacer(Modifier.height(8.dp))

            // Eyebrow: • NOCTURNE INVITATIONAL
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(StateActiveTurn)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "NOCTURNE INVITATIONAL",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 2.2.sp,
                    color = TextPrimary
                )
            }

            Spacer(Modifier.height(10.dp))

            // Serif Hero Title: Master Every Square
            Text(
                text = "Master Every Square",
                fontFamily = PlayfairDisplayFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 28.sp,
                lineHeight = 32.sp,
                letterSpacing = (-0.2).sp,
                color = TextPrimary
            )

            Spacer(Modifier.height(6.dp))

            // Subtitle
            Text(
                text = "Asynchronous grandmaster correspondence,\ntactical mastery, and adaptive engine play.",
                fontFamily = PlusJakartaSansFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = TextSecondary
            )

            Spacer(Modifier.height(18.dp))

            // 3 Equal Tactical Mode Tiles: VS ENGINE | PLAY ONLINE | PASS & PLAY
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HeroActionTile(
                    icon = Icons.Default.Memory,
                    label = "VS ENGINE",
                    onClick = onVsEngine,
                    testTag = "btn_play_ai_opponent",
                    modifier = Modifier.weight(1f)
                )
                HeroActionTile(
                    icon = Icons.Default.Language,
                    label = "PLAY ONLINE",
                    onClick = onPlayOnline,
                    testTag = "btn_challenge_friend",
                    modifier = Modifier.weight(1f)
                )
                HeroActionTile(
                    icon = Icons.Default.Handshake,
                    label = "PASS & PLAY",
                    onClick = onPassAndPlay,
                    testTag = "btn_pass_and_play",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HeroActionTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceElevated.copy(alpha = 0.88f))
            .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = TextPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = label,
                fontFamily = PlusJakartaSansFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
                letterSpacing = 0.8.sp,
                color = TextPrimary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun GrandmasterSectionHeader(
    title: String,
    badgeText: String,
    rightCaption: String,
    beaconAlpha: Float
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(StateActiveTurn.copy(alpha = beaconAlpha))
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = title,
                fontFamily = PlusJakartaSansFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                color = TextPrimary
            )
            Spacer(Modifier.width(10.dp))
            Surface(
                color = SurfaceElevated,
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier.border(1.dp, SurfaceBorder, RoundedCornerShape(999.dp))
            ) {
                Text(
                    text = badgeText,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Text(
            text = rightCaption,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.5.sp,
            letterSpacing = 1.5.sp,
            color = TextSecondary
        )
    }
}

/**
 * The 45° Directional Walnut Match Card from DESIGN.md Section 5 & Screenshot 1:
 * - Rich walnut timber texture in top-right fading at 45° (225deg) to deep obsidian at bottom-left.
 * - Top Row: Avatar + Opponent Name + Master Title Badge (`IM` / `1700`) + `Playing Black • 10m Rapid` | Digital Clock (`04:18`) + `Your Clock`
 * - Middle Inset Bar: `LAST MOVE` + `[ 24... Bc5 ]` + `Eval +0.8` + horizontal progress bar
 * - Bottom Row: Two tactical icon buttons + progress dots + Platinum `Make Move ->` CTA button
 */
@Composable
private fun DirectionalWalnutMatchCard(
    match: MatchSummaryUi,
    cardIndex: Int,
    isUrgentYourTurn: Boolean,
    isClockEnabled: Boolean = true,
    onMakeMoveClick: () -> Unit,
    onInspectClick: () -> Unit
) {
    val opponentDisplayName = when (match.modeType) {
        GameModeType.AI_OPPONENT -> {
            val diff = match.aiDifficulty
            if (diff != null) "Deep Nocturne VII" else match.opponentUsername.substringBefore(" (")
        }
        else -> match.opponentUsername
    }

    val titleBadge = when (match.modeType) {
        GameModeType.AI_OPPONENT -> (match.aiDifficulty?.eloRating ?: 1700).toString()
        GameModeType.PASS_AND_PLAY -> "OTB"
        GameModeType.ONLINE_SUPABASE -> if (cardIndex == 0) "IM" else "WGM"
    }

    val isClockOffForMatch = !isClockEnabled || match.timeControlOption == TimeControlOption.UNLIMITED

    val timeLabel = if (isClockOffForMatch) {
        "No Clock"
    } else {
        when (match.timeControlOption) {
            TimeControlOption.UNLIMITED -> "No Clock"
            TimeControlOption.RAPID_10_0 -> "10m Rapid"
            TimeControlOption.BLITZ_5_0 -> "5m Blitz"
            TimeControlOption.BLITZ_3_2 -> "3+2 Blitz"
            TimeControlOption.CLASSICAL_15_10 -> "15+10 Classical"
            TimeControlOption.DAILY_ASYNC -> "24h Daily"
        }
    }

    val clockFormatted = if (isClockOffForMatch) {
        "OFF"
    } else {
        formatMatchCardClock(
            if (match.playerColor.fenChar == 'w') match.record.whiteTimeRemainingMs else match.record.blackTimeRemainingMs,
            match.timeControlOption.isLiveClock
        )
    }

    val moveCount = (match.sanHistoryList.size / 2).coerceAtLeast(1)
    val lastMoveChip = if (!match.record.lastMoveSan.isNullOrBlank()) {
        "${moveCount + 20}... ${match.record.lastMoveSan}"
    } else {
        "1. e4 e5"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        WoodGrainHighlight,
                        WoodGrainMid,
                        WoodGrainDark,
                        SurfaceGround
                    ),
                    start = Offset(1100f, 0f),
                    end = Offset(0f, 800f)
                )
            )
            .border(
                width = 1.dp,
                color = if (isUrgentYourTurn) WoodPerimeterBorder else SurfaceBorder,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { onMakeMoveClick() }
            .padding(16.dp)
            .testTag("match_card_${match.record.id}")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // 1. Top Row: Opponent Info & Digital Horology Clock
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Rounded-Square Avatar with bottom-right status beacon
                    Box(modifier = Modifier.size(52.dp)) {
                        if (match.modeType == GameModeType.AI_OPPONENT) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceGround)
                                    .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SmartToy,
                                    contentDescription = "AI Engine",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.img_chess_hero_1793508283788),
                                contentDescription = opponentDisplayName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                            )
                        }

                        // Status Dot
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(3.dp)
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(StateActiveTurn)
                                .border(1.5.dp, SurfaceGround, CircleShape)
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = opponentDisplayName,
                                fontFamily = PlusJakartaSansFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = WoodGrainDark,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.border(0.5.dp, SurfaceBorder, RoundedCornerShape(6.dp))
                            ) {
                                Text(
                                    text = titleBadge,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(3.dp))

                        Text(
                            text = "Playing ${match.playerColor.opposite.displayName} • $timeLabel",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.5.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Right Digital Clock Column
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = clockFormatted,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        letterSpacing = 0.8.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = if (match.gameStatus.isTerminal) {
                            match.gameStatus.displayLabel
                        } else if (isClockOffForMatch) {
                            "Clock Off"
                        } else if (isUrgentYourTurn) {
                            "Your Clock"
                        } else {
                            "Their Clock"
                        },
                        fontFamily = PlusJakartaSansFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            // 2. Middle Inset Last-Move & Evaluation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceGround.copy(alpha = 0.78f))
                    .border(0.5.dp, SurfaceBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "LAST MOVE",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp,
                        letterSpacing = 1.0.sp,
                        color = TextSecondary
                    )
                    Spacer(Modifier.width(10.dp))
                    Surface(
                        color = SurfaceElevated,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = lastMoveChip,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.5.sp,
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Eval +0.8",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Spacer(Modifier.width(8.dp))
                    // Mini Evaluation Bar
                    Box(
                        modifier = Modifier
                            .width(44.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(SurfaceElevated)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.62f)
                                .fillMaxHeight()
                                .background(TextPrimary)
                        )
                    }
                }
            }

            // 3. Bottom Action Row: Tactical Mini-Buttons + Dots + Platinum CTA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceElevated.copy(alpha = 0.9f))
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(10.dp))
                            .clickable { onInspectClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QueryStats,
                            contentDescription = "Inspect Match",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceElevated.copy(alpha = 0.9f))
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(10.dp))
                            .clickable { onInspectClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Balance,
                            contentDescription = "Material Balance",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(Modifier.width(2.dp))

                    // Subtle pagination dots matching Screenshot 1
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        repeat(7) { dotIdx ->
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (dotIdx == 6) TextSecondary else TextMuted.copy(alpha = 0.45f)
                                    )
                            )
                        }
                    }
                }

                PlatinumActionButton(
                    text = when {
                        match.gameStatus.isTerminal -> "Review"
                        isUrgentYourTurn -> "Make Move"
                        else -> "Open Board"
                    },
                    onClick = onMakeMoveClick
                )
            }
        }
    }
}

@Composable
private fun EmptyMatchSlotCard(
    label: String,
    ctaText: String,
    onCtaClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceCard)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = PlusJakartaSansFontFamily,
            fontSize = 13.sp,
            color = TextSecondary,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(12.dp))
        PlatinumActionButton(
            text = ctaText,
            onClick = onCtaClick,
            trailingArrow = false
        )
    }
}

private fun formatMatchCardClock(ms: Long, isLive: Boolean): String {
    if (!isLive) return "24:00"
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val m = totalSec / 60L
    val s = totalSec % 60L
    return String.format(Locale.US, "%02d:%02d", m, s)
}
