package com.example.jeecommandcenter.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.unit.dp
import com.example.jeecommandcenter.R
import com.example.jeecommandcenter.ui.theme.BgAppBase

@Composable
fun OpeningScreen() {
    val transition = rememberInfiniteTransition(label = "opening-artwork")
    val glow by transition.animateFloat(
        initialValue = .98f,
        targetValue = 1.012f,
        animationSpec = infiniteRepeatable(
            tween(1700, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "opening-scale"
    )
    val alpha by transition.animateFloat(
        initialValue = .96f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(1700, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "opening-alpha"
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(BgAppBase),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.sigma_je_icon),
            contentDescription = "Sigma JE",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .padding(22.dp)
                .size(340.dp)
                .scale(glow)
                .alpha(alpha)
        )
    }
}
