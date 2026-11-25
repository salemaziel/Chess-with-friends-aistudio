package com.example.data

object SupabaseSchemaSql {
    const val FULL_POSTGRES_SCHEMA = """
-- ============================================================================
-- CHESS WITH FRIENDS - SUPABASE POSTGRESQL SCHEMA & ROW LEVEL SECURITY (RLS)
-- Idempotent script: safe to run multiple times in Supabase SQL Editor.
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. USER PROFILES TABLE
CREATE TABLE IF NOT EXISTS public.profiles (
    id TEXT PRIMARY KEY,
    username TEXT UNIQUE NOT NULL CHECK (char_length(username) >= 2 AND char_length(username) <= 32),
    display_name TEXT NOT NULL,
    avatar_color TEXT NOT NULL DEFAULT '#10B981',
    elo_rating INTEGER NOT NULL DEFAULT 1200,
    games_played INTEGER NOT NULL DEFAULT 0,
    games_won INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_profiles_username ON public.profiles USING btree (lower(username));

-- Auto-create profile row whenever a user signs up via Supabase Auth
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.profiles (id, username, display_name, avatar_color, elo_rating)
    VALUES (
        NEW.id::text,
        COALESCE(NEW.raw_user_meta_data->>'username', split_part(NEW.email, '@', 1) || '_' || substr(NEW.id::text, 1, 4)),
        COALESCE(NEW.raw_user_meta_data->>'display_name', split_part(NEW.email, '@', 1)),
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

-- 2. FRIENDSHIPS & INVITATIONS TABLE
CREATE TABLE IF NOT EXISTS public.friendships (
    id TEXT PRIMARY KEY DEFAULT uuid_generate_v4()::text,
    requester_id TEXT NOT NULL,
    addressee_id TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'accepted', 'declined')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (requester_id, addressee_id)
);

-- 3. ACTIVE & ARCHIVED GAMES TABLE
CREATE TABLE IF NOT EXISTS public.games (
    id TEXT PRIMARY KEY DEFAULT uuid_generate_v4()::text,
    white_player_id TEXT NOT NULL,
    black_player_id TEXT NOT NULL,
    white_username TEXT NOT NULL,
    black_username TEXT NOT NULL,
    current_fen TEXT NOT NULL DEFAULT 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1',
    turn_color TEXT NOT NULL DEFAULT 'w' CHECK (turn_color IN ('w', 'b')),
    status TEXT NOT NULL DEFAULT 'active' CHECK (
        status IN ('active', 'checkmate', 'stalemate', 'draw_agreement', 'draw_fifty_move', 'resigned', 'timeout')
    ),
    winner_id TEXT,
    draw_offered_by TEXT,
    time_control TEXT NOT NULL DEFAULT 'rapid_10_0' CHECK (
        time_control IN ('unlimited', 'daily', 'blitz_3_2', 'blitz_5_0', 'rapid_10_0', 'classical_15_10')
    ),
    white_time_remaining_ms BIGINT NOT NULL DEFAULT 600000,
    black_time_remaining_ms BIGINT NOT NULL DEFAULT 600000,
    last_move_from TEXT,
    last_move_to TEXT,
    last_move_san TEXT,
    move_list_san TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 4. MOVE HISTORY TABLE
CREATE TABLE IF NOT EXISTS public.moves (
    id TEXT PRIMARY KEY DEFAULT uuid_generate_v4()::text,
    game_id TEXT NOT NULL REFERENCES public.games(id) ON DELETE CASCADE,
    move_number INTEGER NOT NULL,
    player_id TEXT NOT NULL,
    piece TEXT NOT NULL,
    from_square TEXT NOT NULL,
    to_square TEXT NOT NULL,
    promotion TEXT,
    san TEXT NOT NULL,
    fen_after TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ROW LEVEL SECURITY (RLS) POLICIES
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.friendships ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.games ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.moves ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Profiles are viewable by everyone" ON public.profiles;
CREATE POLICY "Profiles are viewable by everyone"
    ON public.profiles FOR SELECT USING (true);

DROP POLICY IF EXISTS "Users can insert own profile" ON public.profiles;
CREATE POLICY "Users can insert own profile"
    ON public.profiles FOR INSERT WITH CHECK (true);

DROP POLICY IF EXISTS "Users can update own profile" ON public.profiles;
CREATE POLICY "Users can update own profile"
    ON public.profiles FOR UPDATE USING (auth.uid()::text = id);

DROP POLICY IF EXISTS "Users can view friendships" ON public.friendships;
CREATE POLICY "Users can view friendships"
    ON public.friendships FOR SELECT USING (true);

DROP POLICY IF EXISTS "Users can create friend requests" ON public.friendships;
CREATE POLICY "Users can create friend requests"
    ON public.friendships FOR INSERT WITH CHECK (true);

DROP POLICY IF EXISTS "Participants can update friendship status" ON public.friendships;
CREATE POLICY "Participants can update friendship status"
    ON public.friendships FOR UPDATE USING (true);

DROP POLICY IF EXISTS "Players can view games" ON public.games;
CREATE POLICY "Players can view games"
    ON public.games FOR SELECT USING (true);

DROP POLICY IF EXISTS "Players can create games" ON public.games;
CREATE POLICY "Players can create games"
    ON public.games FOR INSERT WITH CHECK (true);

DROP POLICY IF EXISTS "Players can update games" ON public.games;
CREATE POLICY "Players can update games"
    ON public.games FOR UPDATE USING (true);

DROP POLICY IF EXISTS "Players can read moves" ON public.moves;
CREATE POLICY "Players can read moves"
    ON public.moves FOR SELECT USING (true);

DROP POLICY IF EXISTS "Players can insert moves" ON public.moves;
CREATE POLICY "Players can insert moves"
    ON public.moves FOR INSERT WITH CHECK (true);
"""
}
