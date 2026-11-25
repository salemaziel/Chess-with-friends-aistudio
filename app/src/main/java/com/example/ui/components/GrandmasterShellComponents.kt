package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.GoldAccentRestrained
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PlatinumDark
import com.example.ui.theme.PlatinumLight
import com.example.ui.theme.PlayfairDisplayFontFamily
import com.example.ui.theme.PlusJakartaSansFontFamily
import com.example.ui.theme.StateInteractiveText
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.SurfaceGround
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarmBronzeText

enum class GrandmasterNavTab(val label: String) {
    MATCHES("Matches"),
    LIVE("Live"),
    TACTICS("Tactics"),
    PROFILE("Profile")
}

/**
 * Persistent Shell Header from DESIGN.md Section 4.1:
 * Left: Crown emblem button + "CHESS WITH FRIENDS" serif wordmark & "GRANDMASTER SUITE" subtitle
 * Right: Notification bell with gold beacon + circular avatar with status badge
 */
@Composable
fun GrandmasterTopAppBar(
    isMatchScreen: Boolean,
    hasPendingInvites: Boolean,
    onCrownOrBackClick: () -> Unit,
    onBellClick: () -> Unit,
    onAvatarClick: () -> Unit,
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Brand Cluster
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable { onCrownOrBackClick() }
                .testTag(if (isMatchScreen) "btn_back_to_dashboard" else "brand_crown_button")
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(SurfaceCard)
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) {
                CrownVectorIcon(modifier = Modifier.size(20.dp))
            }

            Spacer(Modifier.width(12.dp))

            if (isMatchScreen) {
                Column {
                    Text(
                        text = "CHESS",
                        fontFamily = PlayfairDisplayFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 22.sp,
                        lineHeight = 23.sp,
                        letterSpacing = 2.8.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "WITH FRIENDS",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 9.5.sp,
                        lineHeight = 12.sp,
                        letterSpacing = 2.6.sp,
                        color = WarmBronzeText
                    )
                }
            } else {
                Column {
                    Text(
                        text = "CHESS WITH\nFRIENDS",
                        fontFamily = PlayfairDisplayFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 19.sp,
                        lineHeight = 21.sp,
                        letterSpacing = 2.2.sp,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "GRANDMASTER SUITE",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 9.5.sp,
                        lineHeight = 12.sp,
                        letterSpacing = 2.4.sp,
                        color = WarmBronzeText
                    )
                }
            }
        }

        // Right Utility Cluster (Settings Gear + Bell with Gold Beacon + Circular Avatar)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceCard)
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                    .clickable { onSettingsClick() }
                    .testTag("settings_menu_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Time Clock & Match Settings",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceCard)
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                    .clickable { onBellClick() }
                    .testTag("friends_drawer_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsNone,
                    contentDescription = "Notifications & Friend Invitations",
                    tint = TextPrimary,
                    modifier = Modifier.size(21.dp)
                )
                if (hasPendingInvites) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 8.dp, end = 9.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(GoldAccentRestrained)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(SurfaceCard)
                    .border(1.5.dp, SurfaceBorder, CircleShape)
                    .clickable { onAvatarClick() }
                    .testTag("auth_sheet_button"),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_chess_hero_1793508283788),
                    contentDescription = "Player Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                )
            }
        }
    }
}

@Composable
fun CrownVectorIcon(
    modifier: Modifier = Modifier,
    tint: Color = TextPrimary
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.16f, h * 0.76f)
            lineTo(w * 0.84f, h * 0.76f)
            lineTo(w * 0.90f, h * 0.32f)
            lineTo(w * 0.68f, h * 0.52f)
            lineTo(w * 0.50f, h * 0.22f)
            lineTo(w * 0.32f, h * 0.52f)
            lineTo(w * 0.10f, h * 0.32f)
            close()
        }
        drawPath(
            path = path,
            color = tint,
            style = Stroke(
                width = w * 0.09f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.18f, h * 0.88f),
            end = Offset(w * 0.82f, h * 0.88f),
            strokeWidth = w * 0.09f,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Tactile Platinum Action Button from DESIGN.md Section 5:
 * `background: linear-gradient(180deg, #f5f6f8 0%, #d8dade 100%)`
 * `color: #0d0e11; font-weight: 700`
 */
@Composable
fun PlatinumActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingArrow: Boolean = true
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(PlatinumLight, PlatinumDark)
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.92f), RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                fontFamily = PlusJakartaSansFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = StateInteractiveText
            )
            if (trailingArrow) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "→",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = StateInteractiveText
                )
            }
        }
    }
}

/**
 * Persistent Bottom Navigation Bar from DESIGN.md Section 4.2:
 * 4 primary routes: Matches [Active], Live, Tactics, Profile.
 */
@Composable
fun GrandmasterBottomNavBar(
    activeTab: GrandmasterNavTab,
    onSelectTab: (GrandmasterNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SurfaceGround.copy(alpha = 0.96f),
        modifier = modifier
            .fillMaxWidth()
            .border(width = 0.5.dp, color = SurfaceBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .height(66.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val items = listOf(
                GrandmasterNavTab.MATCHES to Icons.Default.GridView,
                GrandmasterNavTab.LIVE to Icons.Default.Bolt,
                GrandmasterNavTab.TACTICS to Icons.Default.Psychology,
                GrandmasterNavTab.PROFILE to Icons.Default.PersonOutline
            )
            for ((tab, icon) in items) {
                val isSelected = tab == activeTab
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelectTab(tab) }
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("bottom_nav_${tab.name.lowercase()}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = tab.label,
                        tint = if (isSelected) TextPrimary else TextMuted,
                        modifier = Modifier.size(21.dp)
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = tab.label.uppercase(),
                        fontFamily = PlusJakartaSansFontFamily,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 10.sp,
                        letterSpacing = 0.8.sp,
                        color = if (isSelected) TextPrimary else TextMuted
                    )
                }
            }
        }
    }
}
