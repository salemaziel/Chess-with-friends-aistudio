# Design System: Chess With Friends (Grandmaster Suite)

## 1. Visual Philosophy & Core Aesthetic

### The Concept: "Physical Grandmaster Luxury Meets Digital Precision"
The interface elevates the traditional casual mobile chess game into an artisanal, heirloom-grade experience. Inspired by bespoke walnut and obsidian chess sets, luxury horology, and high-stakes tournament halls, the design system pairs:
- **Obsidian Deep Surfaces**: Grounding UI with rich, layered dark tones (`#0d0e11` to `#1b1b1f`) that provide infinite depth without harsh pure-black eye fatigue.
- **Translucent Walnut Timber & Wood Grain**: Rich, warm burled walnut gradients (`#2b1810` to `#1a0f0a`) layered at 45° directional angles (`225deg`, top-right to bottom-left falloff) to introduce organic materiality.
- **Platinum & Metallic Gleam**: High-contrast, tactile highlights (`#f5f5f7` to `#c8cbd0`) echoing machined platinum and brushed chrome rather than synthetic brass or neon.
- **Zero Neon / Strict Zero-Yellow Discipline**: Complete avoidance of distracting yellows and neons; critical state signifiers rely on luminous eggshell white, cold ice blue, and emerald accents.

---

## 2. Color Palette & Tokens

### Surface & Canvas Tokens
- `--surface-ground`: `#0d0e11` (Deep void obsidian, global backdrop)
- `--surface-card`: `#15161a` (Primary container elevation)
- `--surface-elevated`: `#1c1d22` (Secondary elevation / pill containers)
- `--surface-glass`: `rgba(21, 22, 26, 0.75)` (Frosted glass backdrops with `backdrop-filter: blur(16px)`)
- `--surface-border`: `rgba(255, 255, 255, 0.08)` (Subtle hairline perimeter border)
- `--surface-border-subtle`: `rgba(255, 255, 255, 0.04)`

### Walnut & Timber Accent Tokens
- `--wood-grain-dark`: `#140b07` (Deep espresso timber undertone)
- `--wood-grain-mid`: `#23140d` (Warm cured walnut heartwood)
- `--wood-grain-highlight`: `#3a2016` (Directional specular wood grain)
- `--wood-perimeter-border`: `#3e2216` (Physical board beveled rim)

### Typography & Metallic Shimmer
- `--text-primary`: `#f8f9fa` (Crisp eggshell white, 96% contrast)
- `--text-secondary`: `#b0b4bd` (Brushed platinum silver)
- `--text-muted`: `#686d77` (Muted tournament slate)
- `--metallic-linear-gleam`: `linear-gradient(135deg, #ffffff 0%, #e2e4e8 45%, #9fa4af 100%)`
- `--gold-accent-restrained`: `#d4af37` (Reserved strictly for master title badges e.g., WGM/IM/GM)

### Functional & Interactive States
- `--state-active-turn`: `#ffffff` (Luminous pulse beacon)
- `--state-interactive-cta`: `linear-gradient(180deg, #f2f3f5 0%, #d8dade 100%)` (Solid platinum button fill)
- `--state-interactive-text`: `#0d0e11` (Dark contrast text on platinum CTA)
- `--state-success`: `#34d399` (Subtle tournament victory green)
- `--state-danger`: `#f87171` (Resign / match forfeiture state)

---

## 3. Typography Hierarchy

### Font Families
- **Display & Brand Headings**: `Bodoni Moda`, `Playfair Display`, or serif fallback.
  - Used for: Brand logo wordmarks (`CHESS WITH FRIENDS`), card titles (`Master Every Square`), and section anchors.
  - Characteristics: High stroke contrast, elegant vertical emphasis, tracking `+0.05em` to `+0.12em`.
- **Interface & Metrics Body**: `Plus Jakarta Sans`, `Inter`, or `SF Pro Display`.
  - Used for: Player names, ratings, clocks, move notations, and button controls.
  - Characteristics: Clean geometric sans-serif, high legibility at micro scales (8px–12px).
- **Notation & Clocks (Monospace)**: `JetBrains Mono` or tabular figures (`font-variant-numeric: tabular-nums`).
  - Used for: Digital game timers (`08:57`, `04:18`), FEN moves (`1. e4 e5`, `2. Nf3 Nc6`).

### Scale & Weight
| Element | Size | Weight | Tracking | Case / Style |
| :--- | :--- | :--- | :--- | :--- |
| **Brand Wordmark** | 20px / 1.25rem | 700 SemiBold | `0.14em` | All-Caps Serif |
| **Brand Subtitle** | 10px / 0.625rem | 600 Medium | `0.22em` | Uppercase Monospace |
| **Hero Title** | 24px / 1.5rem | 600 SemiBold | `-0.01em` | Serif Title Case |
| **Player / Heading** | 16px / 1.0rem | 600 SemiBold | `0.01em` | Sans Title Case |
| **Clock Digits** | 18px / 1.125rem | 700 Bold | `0.05em` | Tabular Monospace |
| **Body / Meta** | 12px / 0.75rem | 400 Regular | `0.02em` | Sentence Case |
| **Controls / Pills** | 11px / 0.6875rem | 600 SemiBold | `0.06em` | Uppercase Sans |

---

## 4. Layout Architecture & Spatial Grid

### Mobile Viewport Model
- **Device Width**: Standard 390px–412px portrait mobile frame.
- **Margins & Gutters**: Fixed `16px` outer horizontal margins (`px-4`).
- **Vertical Rhythm**: `12px` to `16px` consistent component gap (`space-y-3` / `space-y-4`).
- **Border Radii**:
  - Cards & Hero Banners: `16px` (`rounded-2xl`)
  - Sub-cards & Player Bars: `12px` (`rounded-xl`)
  - Interactive Action Pills: `8px` to `10px` (`rounded-lg`)
  - Badges & Chips: `9999px` (`rounded-full`)

### Structure Archetypes

#### 1. Top Application Bar (Persistent Shell Header)
- Height: `64px`
- Layout: Left brand mark (Crown SVG + "CHESS WITH FRIENDS" serif wordmark) | Right utility cluster (Bell notification badge with alert beacon + Alex Mercer's circular avatar `36x36px`).
- Style: Translucent dark obsidian glass (`backdrop-blur-md`, subtle bottom hairline border `rgba(255,255,255,0.06)`).

#### 2. Persistent Bottom Navigation Bar
- Height: `68px` fixed dock.
- Items: 4 primary routes (`Matches` [Active], `Live`, `Tactics`, `Profile`).
- Active State: Platinum-lit SVG icon with subtle gold/white accent glow and label.
- Inactive State: Muted tournament slate (`#686d77`) transitioning smoothly on hover/tap.

---

## 5. Component Patterns & Visual Treatments

### The 45° Directional Walnut Match Card
- **Background Multi-Layering**:
  ```css
  background: 
    linear-gradient(225deg, rgba(62, 34, 22, 0.45) 0%, rgba(20, 11, 7, 0.2) 45%, rgba(13, 14, 17, 0.95) 100%),
    radial-gradient(circle at 90% 10%, rgba(120, 68, 42, 0.25) 0%, transparent 60%),
    #131418;
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 4px 20px -2px rgba(0, 0, 0, 0.5), inset 0 1px 0 rgba(255, 255, 255, 0.08);
  ```
- **Top-Right Focus**: Rich walnut texture concentrates in the top-right corner, fading down at 45° towards deep dark obsidian in the bottom-left corner to ensure text readability remains absolute.

### The Tactile Platinum Action Button
- **Light Contrast Action** (`Make Move`, `Play`, `+ NEW MATCH`):
  ```css
  background: linear-gradient(180deg, #f5f6f8 0%, #d8dade 100%);
  color: #0d0e11;
  font-weight: 700;
  border: 1px solid rgba(255, 255, 255, 0.9);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.4), inset 0 1px 0 #ffffff;
  ```
- **Dark Tactical Action** (`Flip`, `Hint`, `Draw`, `Resign`):
  - Inset obsidian container with hairline rim and subtle icon + caption stacked layout.

### The Heirloom Chessboard & Shadowed Pieces
- **Perimeter Rim**: Multi-beveled dark walnut border (`#2a160d` to `#160b06`) with fine inner routed channel.
- **Squares**:
  - Light Squares: Warm alabaster eggshell (`#ede4d4` / `#f0ebe1`).
  - Dark Squares: Deep espresso walnut (`#3d281d` / `#362118`).
- **Piece Drop Shadows & Spatial Depth**:
  - To prevent dark pieces from vanishing into dark squares (and light pieces on light squares), every piece asset utilizes a tailored drop shadow filter:
  ```css
  filter: drop-shadow(0 4px 6px rgba(0, 0, 0, 0.65)) drop-shadow(0 1px 2px rgba(0, 0, 0, 0.8));
  ```
  - Highlighted previous moves utilize warm amber-ochre translucent glows (`rgba(212, 175, 55, 0.3)`) rather than loud fluorescent markers.

### Digital Horology (Clocks & Turn Trackers)
- Encapsulated in dark matte chamfered pill boxes (`bg-surface-elevated`).
- Active turn features a breathing white pulse dot (`animate-pulse`).
- Timer digits displayed in bold, high-contrast monospace tabular numerals.

---

## 6. Motion & Interactive Principles
- **Tactile Weight**: Subtle downward press transform (`active:scale-[0.98]`) on all cards and action buttons.
- **Glass Transitions**: State changes on modals and toggles smoothly cross-fade blur and opacity over `150ms–200ms` ease-out.
- **Tournament Focus**: No whimsical bouncy physics; all animations are disciplined, responsive, and purposeful.
