package com.example.jeecommandcenter

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.example.jeecommandcenter.data.SupabaseClientProvider
import com.example.jeecommandcenter.ui.components.JeeBackground
import com.example.jeecommandcenter.ui.navigation.AppRoot
import com.example.jeecommandcenter.ui.theme.BgAppBase
import com.example.jeecommandcenter.ui.theme.JeePrepTheme
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.parseFragmentAndImportSession
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleSupabaseAuthIntent(intent)
        setContent {
            JeePrepTheme {
                JeeBackground(baseColor = BgAppBase) {
                    AppRoot()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleSupabaseAuthIntent(intent)
    }

    private fun handleSupabaseAuthIntent(intent: Intent?) {
        val fragment = intent?.data?.fragment?.takeIf { it.isNotBlank() } ?: return
        val client = SupabaseClientProvider.client

        lifecycleScope.launch {
            runCatching { client.auth.parseFragmentAndImportSession(fragment) }
        }
    }
}
