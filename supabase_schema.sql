-- ============================================================================
-- CHESS WITH FRIENDS - SUPABASE POSTGRESQL SCHEMA & ROW LEVEL SECURITY (RLS)
-- ============================================================================

-- Enable UUID generation extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. USER PROFILES TABLE
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    username TEXT UNIQUE NOT NULL CHECK (char_length(username) >= 3 AND char_length(username) <= 24),
    display_name TEXT NOT NULL,
    avatar_color TEXT NOT NULL DEFAULT '#10B981',
    elo_rating INTEGER NOT NULL DEFAULT 1200,
    games_played INTEGER NOT NULL DEFAULT 0,
    games_won INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_profiles_username ON public.profiles USING btree (lower(username));

-- 2. FRIENDSHIPS & INVITATIONS TABLE
CREATE TABLE IF NOT EXISTS public.friendships (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    requester_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    addressee_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'accepted', 'declined')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (requester_id, addressee_id)
);

CREATE INDEX IF NOT EXISTS idx_friendships_participants ON public.friendships(requester_id, addressee_id);

-- 3. ACTIVE & ARCHIVED GAMES TABLE
CREATE TABLE IF NOT EXISTS public.games (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    white_player_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    black_player_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    white_username TEXT NOT NULL,
    black_username TEXT NOT NULL,
    current_fen TEXT NOT NULL DEFAULT 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1',
    turn_color TEXT NOT NULL DEFAULT 'w' CHECK (turn_color IN ('w', 'b')),
    status TEXT NOT NULL DEFAULT 'active' CHECK (
        status IN ('active', 'checkmate', 'stalemate', 'draw_agreement', 'draw_fifty_move', 'resigned', 'timeout')
    ),
    winner_id UUID REFERENCES public.profiles(id),
    draw_offered_by UUID REFERENCES public.profiles(id),
    time_control TEXT NOT NULL DEFAULT 'daily' CHECK (
        time_control IN ('daily', 'blitz_3_2', 'blitz_5_0', 'rapid_10_0')
    ),
    white_time_remaining_ms BIGINT NOT NULL DEFAULT 86400000,
    black_time_remaining_ms BIGINT NOT NULL DEFAULT 86400000,
    last_move_from TEXT,
    last_move_to TEXT,
    last_move_san TEXT,
    move_list_san TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_games_white_player ON public.games(white_player_id, status);
CREATE INDEX IF NOT EXISTS idx_games_black_player ON public.games(black_player_id, status);

-- 4. MOVE HISTORY TABLE
CREATE TABLE IF NOT EXISTS public.moves (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    game_id UUID NOT NULL REFERENCES public.games(id) ON DELETE CASCADE,
    move_number INTEGER NOT NULL,
    player_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    piece TEXT NOT NULL,
    from_square TEXT NOT NULL,
    to_square TEXT NOT NULL,
    promotion TEXT,
    san TEXT NOT NULL,
    fen_after TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_moves_game_id ON public.moves(game_id, move_number);

-- ============================================================================
-- ROW LEVEL SECURITY (RLS) POLICIES
-- ============================================================================

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.friendships ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.games ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.moves ENABLE ROW LEVEL SECURITY;

-- Profiles Policies:
-- Any authenticated user can read profiles to search for friends and view opponents.
CREATE POLICY "Profiles are viewable by authenticated users"
    ON public.profiles FOR SELECT
    TO authenticated
    USING (true);

-- Users can insert and update only their own profile.
CREATE POLICY "Users can insert own profile"
    ON public.profiles FOR INSERT
    TO authenticated
    WITH CHECK (auth.uid() = id);

CREATE POLICY "Users can update own profile"
    ON public.profiles FOR UPDATE
    TO authenticated
    USING (auth.uid() = id)
    WITH CHECK (auth.uid() = id);

-- Friendships Policies:
-- Users can view friendships where they are either the requester or addressee.
CREATE POLICY "Users can view their own friendships"
    ON public.friendships FOR SELECT
    TO authenticated
    USING (auth.uid() = requester_id OR auth.uid() = addressee_id);

-- Users can send friend requests where they are the requester.
CREATE POLICY "Users can create friend requests"
    ON public.friendships FOR INSERT
    TO authenticated
    WITH CHECK (auth.uid() = requester_id AND requester_id <> addressee_id);

-- Addressee or requester can update friendship status (accept/decline).
CREATE POLICY "Participants can update friendship status"
    ON public.friendships FOR UPDATE
    TO authenticated
    USING (auth.uid() = addressee_id OR auth.uid() = requester_id)
    WITH CHECK (auth.uid() = addressee_id OR auth.uid() = requester_id);

-- Games Policies:
-- Only the white or black participant can view or subscribe to the game.
CREATE POLICY "Players can view their own games"
    ON public.games FOR SELECT
    TO authenticated
    USING (auth.uid() = white_player_id OR auth.uid() = black_player_id);

-- Any authenticated player can create a game in which they are one of the participants.
CREATE POLICY "Players can create games they participate in"
    ON public.games FOR INSERT
    TO authenticated
    WITH CHECK (auth.uid() = white_player_id OR auth.uid() = black_player_id);

-- Only the white or black participant can update game state (moves, clocks, resign, draw offers).
CREATE POLICY "Players can update their active games"
    ON public.games FOR UPDATE
    TO authenticated
    USING (auth.uid() = white_player_id OR auth.uid() = black_player_id)
    WITH CHECK (auth.uid() = white_player_id OR auth.uid() = black_player_id);

-- Moves Policies:
-- Players can read move history for games they participate in.
CREATE POLICY "Players can read moves of their games"
    ON public.moves FOR SELECT
    TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM public.games g
            WHERE g.id = moves.game_id
              AND (g.white_player_id = auth.uid() OR g.black_player_id = auth.uid())
        )
    );

-- Players can insert a move only if they belong to the game and are the moving player.
CREATE POLICY "Players can insert their own moves"
    ON public.moves FOR INSERT
    TO authenticated
    WITH CHECK (
        auth.uid() = player_id AND
        EXISTS (
            SELECT 1 FROM public.games g
            WHERE g.id = moves.game_id
              AND g.status = 'active'
              AND (
                  (g.turn_color = 'w' AND g.white_player_id = auth.uid()) OR
                  (g.turn_color = 'b' AND g.black_player_id = auth.uid())
              )
        )
    );

-- ============================================================================
-- AUTOMATIC PROFILE TRIGGER & SUPABASE REALTIME PUBLICATION
-- ============================================================================

CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.profiles (id, username, display_name, avatar_color, elo_rating)
    VALUES (
        NEW.id,
        COALESCE(NEW.raw_user_meta_data->>'username', 'player_' || substr(NEW.id::text, 1, 6)),
        COALESCE(NEW.raw_user_meta_data->>'display_name', 'Chess Player'),
        '#10B981',
        1200
    )
    ON CONFLICT (id) DO NOTHING;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- Enable Supabase Realtime change streams for live multiplayer updates without polling
ALTER PUBLICATION supabase_realtime ADD TABLE public.games;
ALTER PUBLICATION supabase_realtime ADD TABLE public.moves;
ALTER PUBLICATION supabase_realtime ADD TABLE public.friendships;
