package com.jworks.kanjisage.data.auth

import com.jworks.kanjisage.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SupabaseConfigTest {
    @Test
    fun authAndFunctionsUseTheSameJCoinEndpoint() {
        assertEquals("https://j-coin.jworks-ai.com", SupabaseConfig.SUPABASE_URL)
        assertEquals(SupabaseConfig.SUPABASE_URL, SupabaseConfig.JCOIN_SUPABASE_URL)
    }

    @Test
    fun bothClientsUseTheProvisionedBuildConfiguration() {
        // Boolean assertions deliberately keep key values out of failed-test reports.
        assertTrue("Auth key must be provisioned", SupabaseConfig.SUPABASE_ANON_KEY.isNotBlank())
        assertTrue("Auth key must use BuildConfig", SupabaseConfig.SUPABASE_ANON_KEY == BuildConfig.SUPABASE_ANON_KEY)
        assertTrue("Functions key must use BuildConfig", SupabaseConfig.JCOIN_SUPABASE_ANON_KEY == BuildConfig.JCOIN_SUPABASE_ANON_KEY)
        assertTrue("Both clients must use the same project key", SupabaseConfig.SUPABASE_ANON_KEY == SupabaseConfig.JCOIN_SUPABASE_ANON_KEY)
    }
}
