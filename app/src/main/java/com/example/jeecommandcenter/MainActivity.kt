package com.example.jeecommandcenter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.jeecommandcenter.ui.components.JeeBackground
import com.example.jeecommandcenter.ui.navigation.AppRoot
import com.example.jeecommandcenter.ui.theme.BgApp
import com.example.jeecommandcenter.ui.theme.JeePrepTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JeePrepTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = BgApp) {
                    Box(Modifier.fillMaxSize().background(com.example.jeecommandcenter.ui.theme.ColorBase)) {
                        JeeBackground()
                        AppRoot()
                    }
                }
            }
        }
    }
}
