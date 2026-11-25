package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// DESIGN SYSTEM: CHESS WITH FRIENDS (GRANDMASTER SUITE)
// "Physical Grandmaster Luxury Meets Digital Precision"
// ============================================================================

// Surface & Canvas Tokens
val SurfaceGround = Color(0xFF0D0E11)        // --surface-ground: Deep void obsidian
val SurfaceCard = Color(0xFF15161A)          // --surface-card: Primary container elevation
val SurfaceElevated = Color(0xFF1C1D22)      // --surface-elevated: Secondary elevation / pills
val SurfaceGlass = Color(0xBF15161A)         // --surface-glass: Frosted glass backdrop
val SurfaceBorder = Color(0x14FFFFFF)        // --surface-border: rgba(255, 255, 255, 0.08)
val SurfaceBorderSubtle = Color(0x0AFFFFFF)  // --surface-border-subtle: rgba(255, 255, 255, 0.04)

// Walnut & Timber Accent Tokens
val WoodGrainDark = Color(0xFF140B07)        // --wood-grain-dark: Deep espresso timber undertone
val WoodGrainMid = Color(0xFF23140D)         // --wood-grain-mid: Warm cured walnut heartwood
val WoodGrainHighlight = Color(0xFF3A2016)   // --wood-grain-highlight: Directional specular wood grain
val WoodPerimeterBorder = Color(0xFF3E2216)  // --wood-perimeter-border: Physical board beveled rim
val WoodRadialGlow = Color(0x4078442A)       // Warm burled walnut top-right highlight

// Typography & Metallic Shimmer Tokens
val TextPrimary = Color(0xFFF8F9FA)          // --text-primary: Crisp eggshell white
val TextSecondary = Color(0xFFB0B4BD)        // --text-secondary: Brushed platinum silver
val TextMuted = Color(0xFF686D77)            // --text-muted: Muted tournament slate
val PlatinumLight = Color(0xFFF5F6F8)        // Top of platinum CTA gradient
val PlatinumMid = Color(0xFFE2E4E8)          // Mid metallic gleam
val PlatinumDark = Color(0xFFD8DADE)         // Bottom of platinum CTA gradient
val PlatinumShade = Color(0xFF9FA4AF)        // Brushed chrome shadow
val GoldAccentRestrained = Color(0xFFD4AF37) // --gold-accent-restrained: Master title badges (WGM/IM/GM)
val WarmBronzeText = Color(0xFFA38A68)       // Subtle bronze-gold subtitle accent

// Functional & Interactive States
val StateActiveTurn = Color(0xFFFFFFFF)      // --state-active-turn: Luminous pulse beacon
val StateInteractiveText = Color(0xFF0D0E11) // --state-interactive-text: Dark contrast on platinum CTA
val StateSuccess = Color(0xFF34D399)         // --state-success: Subtle tournament victory green
val StateDanger = Color(0xFFF87171)          // --state-danger: Resign / forfeiture state
val ColdIceBlue = Color(0xFF93C5FD)          // Subtle cold ice blue for tactical trajectories

// Heirloom Chessboard & Shadowed Pieces Tokens
val BoardRimOuter = Color(0xFF160B06)        // Outer beveled walnut edge
val BoardRimMid = Color(0xFF2A160D)          // Mid walnut frame
val BoardRimChannel = Color(0xFF0B0604)      // Fine inner routed channel
val BoardLightSquare = Color(0xFFEDE4D4)     // Warm alabaster eggshell (#ede4d4)
val BoardDarkSquare = Color(0xFF3D281D)      // Deep espresso walnut (#3d281d)
val BoardSelectedSquare = Color(0x59D4AF37)  // Warm amber-ochre translucent highlight
val BoardLastMoveGlow = Color(0x4DD4AF37)    // rgba(212, 175, 55, 0.30) previous move glow
val BoardLastMoveBorder = Color(0xFF9A7B3E)  // Subtle inset border on last move square

// Tactical Assist (Beginner Mode) Engine Colors
val BeginnerPathLine = Color(0xFFE2E4E8)     // Luminous platinum trajectory ray
val BeginnerQuietBadge = Color(0xFF34D399)   // Subtle tournament emerald reachable dot
val BeginnerCaptureRing = Color(0xFFF87171)  // Tactical capture ring
val KingCheckGlow = Color(0xFFEF4444)        // Check alert glow

// Chess Piece Colors on Heirloom Canvas
val WhitePieceFill = Color(0xFFF4F1EA)           // Alabaster ivory fill
val WhitePieceStroke = Color(0xFF23252A)         // Crisp dark charcoal engraving
val WhitePieceInnerHighlight = Color(0xFFFFFFFF) // Specular ivory rim
val BlackPieceFill = Color(0xFF15161A)           // Deep obsidian piece body
val BlackPieceStroke = Color(0xFF8E939E)         // Brushed platinum silver engraving
val BlackPieceInnerHighlight = Color(0xFF383B44) // Machined dark chrome inner bevel

// Backwards-compatible aliases used across sheets/components
val ObsidianSlate = SurfaceGround
val DeepSlate = SurfaceCard
val ElevatedSlate = SurfaceElevated
val SurfaceSlate = Color(0xFF24262D)
val BorderSlate = Color(0xFF2A2C34)
val MutedSlateText = TextSecondary
val CrispWhiteText = TextPrimary
val TournamentEmerald = PlatinumLight
val BrightEmerald = GoldAccentRestrained
val DeepEmeraldContainer = WoodGrainMid
val EmeraldTint = StateSuccess
val WarmAmber = GoldAccentRestrained
val SoftAmberContainer = WoodGrainDark
val DangerRose = StateDanger
val CyanAccent = ColdIceBlue
val BoardFrameDark = BoardRimMid
val BoardFrameBorder = WoodPerimeterBorder
val BoardLastMoveLight = Color(0xFFE5D3A8)
val BoardLastMoveDark = Color(0xFF563B25)
