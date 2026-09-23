package com.example.jeecommandcenter.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeecommandcenter.ui.theme.BgAppBase
import com.example.jeecommandcenter.ui.theme.PrimaryLight
import com.example.jeecommandcenter.ui.theme.TextMuted
import com.example.jeecommandcenter.ui.theme.TextPrimary

@Composable
fun OpeningScreen() {
    val transition = rememberInfiniteTransition(label = "opening-glow")
    val glow by transition.animateFloat(
        initialValue = .22f,
        targetValue = .5f,
        animationSpec = infiniteRepeatable(
            tween(1500, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "opening-glow-alpha"
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(BgAppBase),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Box(
                Modifier
                    .size(210.dp)
                    .shadow(24.dp, RoundedCornerShape(48.dp))
                    .background(
                        Brush.radialGradient(
                            listOf(
                                PrimaryLight.copy(alpha = glow),
                                Color.Transparent
                            )
                        ),
                        RoundedCornerShape(48.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.fillMaxSize().padding(26.dp)) {
                    val w = size.width
                    val h = size.height
                    val top = Path().apply {
                        moveTo(w * .16f, h * .14f)
                        lineTo(w * .80f, h * .14f)
                        lineTo(w * .80f, h * .34f)
                        lineTo(w * .46f, h * .34f)
                        lineTo(w * .60f, h * .50f)
                        lineTo(w * .46f, h * .66f)
                        lineTo(w * .80f, h * .66f)
                        lineTo(w * .80f, h * .86f)
                        lineTo(w * .16f, h * .86f)
                        lineTo(w * .16f, h * .66f)
                        lineTo(w * .40f, h * .66f)
                        lineTo(w * .54f, h * .50f)
                        lineTo(w * .40f, h * .34f)
                        lineTo(w * .16f, h * .34f)
                        close()
                    }
                    drawPath(top, brush = Brush.linearGradient(listOf(Color(0xFFE8FF9C), Color(0xFF77D96A), Color(0xFF4A9B43))))
                    drawOval(
                        color = PrimaryLight.copy(alpha = .58f),
                        topLeft = Offset(w * .02f, h * .20f),
                        size = androidx.compose.ui.geometry.Size(w * .96f, h * .60f),
                        style = Stroke(2.5.dp.toPx())
                    )
                    drawCircle(PrimaryLight, radius = 6.dp.toPx(), center = Offset(w * .10f, h * .57f))
                    val cx = w * .78f
                    val cy = h * .50f
                    val star = Path().apply {
                        moveTo(cx, cy - 13); lineTo(cx + 5, cy - 5); lineTo(cx + 13, cy)
                        lineTo(cx + 5, cy + 5); lineTo(cx, cy + 13); lineTo(cx - 5, cy + 5)
                        lineTo(cx - 13, cy); lineTo(cx - 5, cy - 5); close()
                    }
                    drawPath(star, color = Color(0xFFF1FF9F))
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("SIGMA JEE", color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp)
            Spacer(Modifier.height(8.dp))
            Text("HIGHER. EVERYDAY.", color = PrimaryLight, fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 3.2.sp)
            Spacer(Modifier.height(30.dp))
            Text("Your JEE command center.", color = TextMuted, fontSize = 10.sp)
        }
    }
}
