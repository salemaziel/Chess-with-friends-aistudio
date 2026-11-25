package com.example

import android.os.Bundle
import android.view.ActionMode
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.GameModeType
import com.example.data.TimeControlOption
import com.example.engine.PieceColor
import com.example.ui.ChessViewModel
import com.example.ui.screens.ClockSettingsSheet
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FriendSearchDrawerSheet
import com.example.ui.screens.MatchScreen
import com.example.ui.screens.NewMatchSetupSheet
import com.example.ui.screens.SupabaseAuthSheet
import com.example.ui.screens.SupabaseSqlSchemaSheet
import com.example.ui.theme.ChessWithFriendsTheme

/**
 * Custom Compose [TextToolbar] that handles copy/paste/cut/selectAll cleanly in Compose
 * without spawning native `DecorView` `TYPE_FLOATING` [ActionMode] instances that can trigger
 * `E/DecorView: Destroying unexpected ActionMode instance of TYPE_FLOATING` when a
 * [androidx.compose.material3.ModalBottomSheet] window is dismissed or re-composed.
 */
class SafeComposeTextToolbar : TextToolbar {
    override var status: TextToolbarStatus = TextToolbarStatus.Hidden
        private set

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?
    ) {
        status = TextToolbarStatus.Shown
        when {
            onPasteRequested != null && onCopyRequested == null -> {
                onPasteRequested.invoke()
                status = TextToolbarStatus.Hidden
            }
            onCopyRequested != null -> {
                onCopyRequested.invoke()
                status = TextToolbarStatus.Hidden
            }
            onSelectAllRequested != null -> {
                onSelectAllRequested.invoke()
                status = TextToolbarStatus.Hidden
            }
        }
    }

    override fun hide() {
        status = TextToolbarStatus.Hidden
    }
}

class MainActivity : ComponentActivity() {
    private var activeActionMode: ActionMode? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val deepLinkUri = intent?.data?.toString()
        setContent {
            ChessWithFriendsTheme(darkTheme = true) {
                val safeToolbar = remember { SafeComposeTextToolbar() }
                CompositionLocalProvider(LocalTextToolbar provides safeToolbar) {
                    ChessWithFriendsApp(initialInviteDeepLink = deepLinkUri)
                }
            }
        }
    }

    override fun onActionModeStarted(mode: ActionMode?) {
        super.onActionModeStarted(mode)
        activeActionMode = mode
    }

    override fun onActionModeFinished(mode: ActionMode?) {
        if (activeActionMode == mode) {
            activeActionMode = null
        }
        super.onActionModeFinished(mode)
    }

    override fun onPause() {
        activeActionMode?.finish()
        activeActionMode = null
        super.onPause()
    }
}

@Composable
fun ChessWithFriendsApp(
    initialInviteDeepLink: String? = null,
    chessViewModel: ChessViewModel = viewModel()
) {
    val uiState by chessViewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(initialInviteDeepLink) {
        if (!initialInviteDeepLink.isNullOrBlank()) {
            chessViewModel.startMatchFromShareLinkOrQr(initialInviteDeepLink)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Crossfade(
        targetState = uiState.activeMatch != null,
        label = "screen_navigation"
    ) { isInMatch ->
        if (isInMatch && uiState.activeMatch != null) {
            val active = uiState.activeMatch!!
            MatchScreen(
                uiState = uiState,
                onBackToDashboard = {
                    focusManager.clearFocus(force = true)
                    chessViewModel.closeActiveMatch()
                },
                onToggleBeginnerMode = { chessViewModel.toggleBeginnerMode() },
                onToggleFlipBoard = { chessViewModel.toggleFlipBoard() },
                onSquareTapped = { sq -> chessViewModel.onSquareTapped(sq) },
                onPieceDragged = { from, to -> chessViewModel.onPieceDragged(from, to) },
                onSelectPromotionPiece = { type -> chessViewModel.onPromotionPieceSelected(type) },
                onDismissPromotion = { chessViewModel.dismissPromotionDialog() },
                onChangeAiDifficulty = { diff -> chessViewModel.changeActiveAiDifficulty(diff) },
                onRequestHint = { chessViewModel.requestTacticalHint() },
                onSimulateOpponentReply = { chessViewModel.simulateOpponentResponseInOnlineDemo() },
                onOfferOrAcceptDraw = { chessViewModel.offerOrAcceptDrawInActiveMatch() },
                onResignMatch = { chessViewModel.resignActiveMatch() },
                onRematch = {
                    chessViewModel.startNewMatch(
                        opponent = null,
                        modeType = active.modeType,
                        aiDifficulty = active.aiDifficulty ?: uiState.selectedAiDifficulty,
                        timeControl = active.timeControlOption,
                        playAsWhite = active.playerColor == PieceColor.BLACK
                    )
                },
                onOpenFriendDrawer = {
                    focusManager.clearFocus(force = true)
                    chessViewModel.setShowFriendDrawer(true)
                },
                onOpenAuthSheet = {
                    focusManager.clearFocus(force = true)
                    chessViewModel.setShowAuthSheet(true)
                },
                onOpenClockSettingsSheet = {
                    focusManager.clearFocus(force = true)
                    chessViewModel.setShowClockSettingsSheet(true)
                }
            )
        } else {
            DashboardScreen(
                uiState = uiState,
                onOpenMatch = { match ->
                    focusManager.clearFocus(force = true)
                    chessViewModel.openMatch(match)
                },
                onOpenNewMatchSheet = { mode ->
                    focusManager.clearFocus(force = true)
                    chessViewModel.openNewMatchSheet(preselectedOpponent = null, preselectedMode = mode)
                },
                onQuickStartPassAndPlay = {
                    focusManager.clearFocus(force = true)
                    chessViewModel.startNewMatch(
                        opponent = null,
                        modeType = GameModeType.PASS_AND_PLAY,
                        timeControl = if (uiState.isClockEnabled) uiState.defaultTimeControl else TimeControlOption.UNLIMITED,
                        playAsWhite = true
                    )
                },
                onOpenFriendDrawer = {
                    focusManager.clearFocus(force = true)
                    chessViewModel.setShowFriendDrawer(true)
                },
                onOpenAuthSheet = {
                    focusManager.clearFocus(force = true)
                    chessViewModel.setShowAuthSheet(true)
                },
                onOpenSqlSchemaSheet = {
                    focusManager.clearFocus(force = true)
                    chessViewModel.setShowSqlSchemaSheet(true)
                },
                onOpenClockSettingsSheet = {
                    focusManager.clearFocus(force = true)
                    chessViewModel.setShowClockSettingsSheet(true)
                }
            )
        }
    }

    // Modal Bottom Sheets
    if (uiState.showNewMatchSheet) {
        NewMatchSetupSheet(
            currentUser = uiState.currentUser,
            preselectedOpponent = uiState.preselectedOpponentForChallenge,
            initialMode = uiState.preselectedModeForNewMatch,
            initialAiDifficulty = uiState.selectedAiDifficulty,
            friends = uiState.friends,
            onStartMatch = { opp, mode, aiDiff, tc, playWhite ->
                focusManager.clearFocus(force = true)
                chessViewModel.startNewMatch(
                    opponent = opp,
                    modeType = mode,
                    aiDifficulty = aiDiff,
                    timeControl = tc,
                    playAsWhite = playWhite
                )
            },
            onInviteByUsername = { username, tc, playWhite ->
                focusManager.clearFocus(force = true)
                chessViewModel.inviteByUsernameAndStartMatch(
                    username = username,
                    timeControl = tc,
                    playAsWhite = playWhite
                )
            },
            onStartMatchFromLinkOrQr = { linkOrCode, tc ->
                focusManager.clearFocus(force = true)
                chessViewModel.startMatchFromShareLinkOrQr(
                    linkOrCode = linkOrCode,
                    timeControl = tc
                )
            },
            onDismiss = {
                focusManager.clearFocus(force = true)
                chessViewModel.closeNewMatchSheet()
            }
        )
    }

    if (uiState.showFriendDrawer) {
        FriendSearchDrawerSheet(
            currentUser = uiState.currentUser,
            friends = uiState.friends,
            searchQuery = uiState.friendSearchQuery,
            searchResults = uiState.friendSearchResults,
            onQueryChanged = { chessViewModel.updateFriendSearchQuery(it) },
            onSendFriendInvite = {
                focusManager.clearFocus(force = true)
                chessViewModel.sendFriendInvite(it)
            },
            onAcceptFriendInvite = {
                focusManager.clearFocus(force = true)
                chessViewModel.acceptFriendInvite(it)
            },
            onInviteByUsername = { username ->
                focusManager.clearFocus(force = true)
                chessViewModel.inviteByUsernameAndStartMatch(username)
            },
            onStartMatchFromLinkOrQr = { linkOrCode ->
                focusManager.clearFocus(force = true)
                chessViewModel.startMatchFromShareLinkOrQr(linkOrCode)
            },
            onChallengeFriend = { friendProfile ->
                focusManager.clearFocus(force = true)
                chessViewModel.setShowFriendDrawer(false)
                chessViewModel.openNewMatchSheet(
                    preselectedOpponent = friendProfile,
                    preselectedMode = GameModeType.ONLINE_SUPABASE
                )
            },
            onDismiss = {
                focusManager.clearFocus(force = true)
                chessViewModel.setShowFriendDrawer(false)
            }
        )
    }

    if (uiState.showAuthSheet) {
        SupabaseAuthSheet(
            currentUser = uiState.currentUser,
            isSupabaseConfigured = uiState.isSupabaseConfigured,
            activeSupabaseUrl = uiState.activeSupabaseUrl,
            activeSupabaseAnonKey = uiState.activeSupabaseAnonKey,
            supabaseConnectionStatus = uiState.supabaseConnectionStatus,
            isTestingConnection = uiState.isTestingSupabaseConnection,
            isAuthenticatedOnline = uiState.isAuthenticatedOnline,
            authError = uiState.authErrorMessage,
            onConfigureAndTestSupabase = { url, anonKey ->
                focusManager.clearFocus(force = true)
                chessViewModel.configureAndTestSupabase(url, anonKey)
            },
            onSignIn = { email, pass ->
                focusManager.clearFocus(force = true)
                chessViewModel.signIn(email, pass)
            },
            onSignUp = { email, pass, user, disp ->
                focusManager.clearFocus(force = true)
                chessViewModel.signUp(email, pass, user, disp)
            },
            onSignOut = {
                focusManager.clearFocus(force = true)
                chessViewModel.signOut()
            },
            onOpenSqlSchema = {
                focusManager.clearFocus(force = true)
                chessViewModel.setShowAuthSheet(false)
                chessViewModel.setShowSqlSchemaSheet(true)
            },
            onDismiss = {
                focusManager.clearFocus(force = true)
                chessViewModel.setShowAuthSheet(false)
            }
        )
    }

    if (uiState.showSqlSchemaSheet) {
        SupabaseSqlSchemaSheet(
            onDismiss = {
                focusManager.clearFocus(force = true)
                chessViewModel.setShowSqlSchemaSheet(false)
            }
        )
    }

    if (uiState.showClockSettingsSheet) {
        ClockSettingsSheet(
            isClockEnabled = uiState.isClockEnabled,
            currentTimeControl = uiState.activeMatch?.timeControlOption ?: uiState.defaultTimeControl,
            useCustomClockDuration = uiState.useCustomClockDuration,
            customClockMinutes = uiState.customClockMinutes,
            customIncrementSeconds = uiState.customIncrementSeconds,
            hasActiveMatch = uiState.activeMatch != null,
            onToggleClockEnabled = { enabled ->
                chessViewModel.setClockEnabled(enabled)
            },
            onApplyClockSettings = { enabled, tc, useCustom, customMin, customInc, applyToActive ->
                focusManager.clearFocus(force = true)
                chessViewModel.applyClockSettings(
                    enabled = enabled,
                    timeControl = tc,
                    useCustom = useCustom,
                    customMinutes = customMin,
                    customIncrementSec = customInc,
                    applyToActiveMatch = applyToActive
                )
            },
            onDismiss = {
                focusManager.clearFocus(force = true)
                chessViewModel.setShowClockSettingsSheet(false)
            }
        )
    }
    }
}
