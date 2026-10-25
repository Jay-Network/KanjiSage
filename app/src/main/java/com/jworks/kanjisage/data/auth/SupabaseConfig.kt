package com.jworks.kanjisage.data.auth

import com.jworks.kanjisage.BuildConfig

object SupabaseConfig {
    // Auth, roles, J Coin and feedback share the J Coin backend.
    const val SUPABASE_URL = "https://j-coin.jworks-ai.com"
    const val SUPABASE_ANON_KEY = BuildConfig.SUPABASE_ANON_KEY

    // Shared J Coin backend (cross-business)
    const val JCOIN_SUPABASE_URL = SUPABASE_URL
    const val JCOIN_SUPABASE_ANON_KEY = BuildConfig.JCOIN_SUPABASE_ANON_KEY

    // Google OAuth Web Client ID (from Google Cloud Console)
    const val GOOGLE_WEB_CLIENT_ID = "946989395001-r98m76mf2a52kbbnqskvug15htue2smb.apps.googleusercontent.com"
}
