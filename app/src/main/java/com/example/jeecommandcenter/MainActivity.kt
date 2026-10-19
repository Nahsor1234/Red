package com.example.jeecommandcenter

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.jeecommandcenter.data.SupabaseClientProvider
import com.example.jeecommandcenter.data.ReminderScheduler
import com.example.jeecommandcenter.ui.components.JeeBackground
import com.example.jeecommandcenter.ui.navigation.AppRoot
import com.example.jeecommandcenter.ui.theme.BgAppBase
import com.example.jeecommandcenter.ui.theme.JeePrepTheme
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.parseFragmentAndImportSession
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var launchTarget by mutableStateOf<String?>(null)
    private var launchRequestId by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Reconcile stored reminders with AlarmManager whenever the app starts.
        ReminderScheduler(applicationContext).scheduleAll()
        enableEdgeToEdge()
        handleSupabaseAuthIntent(intent)
        launchTarget = intent?.getStringExtra(EXTRA_OPEN_TARGET)
        if (launchTarget != null) launchRequestId++
        setContent {
            JeePrepTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = BgAppBase) {
                    Box(Modifier.fillMaxSize().background(BgAppBase)) {
                        JeeBackground()
                        AppRoot(initialTarget = launchTarget, launchRequestId = launchRequestId)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleSupabaseAuthIntent(intent)
        intent.getStringExtra(EXTRA_OPEN_TARGET)?.let { target -> launchTarget = target; launchRequestId++ }
    }

    companion object {
        const val EXTRA_OPEN_TARGET = "open_target"
    }

    @OptIn(SupabaseInternal::class)
    private fun handleSupabaseAuthIntent(intent: Intent?) {
        val fragment = intent?.data?.fragment?.takeIf { it.isNotBlank() } ?: return
        val client = SupabaseClientProvider.client
        lifecycleScope.launch {
            runCatching { client.auth.parseFragmentAndImportSession(fragment) }
        }
    }
}
