package com.example.jeecommandcenter

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
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
        enableEdgeToEdge()
        handleSupabaseAuthIntent(intent)
        setContent {
            JeePrepTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = BgAppBase) {
                    Box(Modifier.fillMaxSize().background(BgAppBase)) {
                        JeeBackground()
                        AppRoot()
                    }
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
