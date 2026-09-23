package com.example.jeecommandcenter.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeecommandcenter.ui.theme.*
import kotlin.math.abs

private fun mixHash(row: Int, col: Int, seed: Int): Int {
    var value = row * 1103515245 + col * 12345 + seed * 265443576
    value = value xor (value ushr 16)
    value *= 224682251
    value = value xor (value ushr 13)
    return value and Int.MAX_VALUE
}

@Composable
fun JeeBackground(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize()) {
        val step = 62.dp.toPx()
        val rows = (size.height / step).toInt() + 3
        val cols = (size.width / step).toInt() + 3
        for (row in 0 until rows) for (col in 0 until cols) {
            val h = mixHash(row, col, 17)
            val h2 = mixHash(row, col, 43)
            val scale = 0.7f + (h % 40) / 100f
            val alpha = 0.028f + (h2 % 26) / 1000f
            val ox = ((mixHash(row, col, 71) % 35) - 17).dp.toPx()
            val oy = ((mixHash(row, col, 97) % 31) - 15).dp.toPx()
            val center = androidx.compose.ui.geometry.Offset(col * step + ox, row * step + oy)
            val s = scale * 11.dp.toPx()
            val stroke = (0.8f + (h2 % 40) / 100f).dp.toPx()
            val color = TextSecondary.copy(alpha = alpha)
            fun line(a: androidx.compose.ui.geometry.Offset, b: androidx.compose.ui.geometry.Offset) = drawLine(color, a, b, strokeWidth = stroke)
            fun ring(radius: Float, point: androidx.compose.ui.geometry.Offset = center) = drawCircle(color, radius = radius, center = point, style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke))
            when (h % 12) {
                0 -> { line(center.copy(x = center.x - s), center.copy(x = center.x + s)); line(center.copy(y = center.y + s), center.copy(y = center.y - s)); drawCircle(color, s * 0.16f, center.copy(x = center.x + s * 0.55f, y = center.y - s * 0.42f)) }
                1 -> { val a = center.copy(x = center.x - s * .8f, y = center.y + s * .55f); val b = center.copy(x = center.x + s * .85f, y = center.y + s * .55f); val d = center.copy(x = center.x + s * .1f, y = center.y - s * .85f); line(a, b); line(b, d); line(d, a); line(d, center.copy(x = d.x, y = a.y)) }
                2 -> { var last = center.copy(x = center.x - s); for (i in 1..10) { val next = center.copy(x = center.x - s + i * (s * .2f), y = center.y + kotlin.math.sin(i * .8f) * s * .45f); line(last, next); last = next } }
                3 -> { val a = center.copy(x = center.x - s * .72f, y = center.y); val b = center.copy(x = center.x + s * .7f, y = center.y - s * .25f); val d = center.copy(x = center.x, y = center.y + s * .75f); line(a, b); line(a, d); line(b, d); drawCircle(color, s * .18f, a); drawCircle(color, s * .15f, b); drawCircle(color, s * .17f, d) }
                4 -> { ring(s * .62f); ring(s * .35f); drawCircle(color, s * .09f, center); line(center.copy(x = center.x - s), center.copy(x = center.x + s)) }
                5 -> { val pts = (0 until 6).map { i -> val angle = Math.toRadians((60 * i - 30).toDouble()); center.copy(x = center.x + kotlin.math.cos(angle).toFloat() * s * .72f, y = center.y + kotlin.math.sin(angle).toFloat() * s * .72f) }; pts.forEachIndexed { i, p -> line(p, pts[(i + 1) % pts.size]) }; ring(s * .22f) }
                6 -> { val start = center.copy(x = center.x - s, y = center.y + s * .45f); val tip = center.copy(x = center.x + s, y = center.y - s * .35f); line(start, tip); line(tip, tip.copy(x = tip.x - s * .38f)); line(tip, tip.copy(x = tip.x - s * .12f, y = tip.y + s * .28f)) }
                7 -> { val axisX = center.x - s * .8f; val axisY = center.y + s * .65f; line(center.copy(x = axisX, y = center.y - s), center.copy(x = axisX, y = axisY)); line(center.copy(x = axisX, y = axisY), center.copy(x = center.x + s, y = axisY)); var last = center.copy(x = axisX, y = center.y + s * .15f); for (i in 1..7) { val next = center.copy(x = axisX + i * (s * .25f), y = center.y + kotlin.math.cos(i * .55f) * s * .42f); line(last, next); last = next } }
                8 -> { val left = center.copy(x = center.x - s); val right = center.copy(x = center.x + s); line(left, center.copy(x = center.x - s * .35f)); line(center.copy(x = center.x + s * .35f), right); line(center.copy(x = center.x - s * .35f, y = center.y - s * .3f), center.copy(x = center.x + s * .35f, y = center.y - s * .3f)); line(center.copy(x = center.x, y = center.y - s * .3f), center.copy(x = center.x, y = center.y + s * .3f)) }
                9 -> { ring(s * .72f); ring(s * .36f); line(center.copy(x = center.x - s), center.copy(x = center.x + s)); line(center.copy(y = center.y - s), center.copy(y = center.y + s)) }
                10 -> { val a = center.copy(x = center.x - s, y = center.y - s * .2f); val b = center.copy(x = center.x + s, y = center.y - s * .2f); line(a, b); line(a, a.copy(x = a.x + s * .32f, y = a.y - s * .28f)); line(a, a.copy(x = a.x + s * .32f, y = a.y + s * .28f)); line(b, b.copy(x = b.x - s * .32f, y = b.y - s * .28f)); line(b, b.copy(x = b.x - s * .32f, y = b.y + s * .28f)) }
                else -> { val tl = center.copy(x = center.x - s * .7f, y = center.y - s * .45f); val tr = center.copy(x = center.x + s * .7f, y = center.y - s * .45f); val br = center.copy(x = center.x + s * .7f, y = center.y + s * .45f); val bl = center.copy(x = center.x - s * .7f, y = center.y + s * .45f); line(tl, tr); line(tr, br); line(br, bl); line(bl, tl); drawCircle(color, s * .12f, center) }
            }
        }
    }
}

enum class AppTab { HOME, SYLLABUS, TASKS, STATS }

@Composable
fun JeeCard(modifier: Modifier = Modifier, featured: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    val shape = if (featured) JeeShapes.large else JeeShapes.medium
    Column(modifier.fillMaxWidth().clip(shape).background(if (featured) BgCardAlt else BgCard).border(JeeSurfaceTokens.borderWidth, BgCardBorder.copy(alpha = if (featured) JeeSurfaceTokens.featuredBorderAlpha else JeeSurfaceTokens.cardBorderAlpha), shape).padding(16.dp), content = content)
}

@Composable
fun LinearStatBar(progress: Float, modifier: Modifier = Modifier, trackColor: Color = BgDivider, fillColor: Color = AccentBlue, height: androidx.compose.ui.unit.Dp = 5.dp) {
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), animationSpec = tween(320), label = "progress")
    Box(modifier.fillMaxWidth().height(height).clip(JeeShapes.pill).background(trackColor)) { Box(Modifier.fillMaxHeight().fillMaxWidth(p).clip(JeeShapes.pill).background(fillColor)) }
}

@Composable
fun SectionHeader(title: String, actionLabel: String? = null, onActionClick: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) { Text(title, style = MaterialTheme.typography.titleMedium); if (actionLabel != null) Text(actionLabel, color = AccentBlueLight, fontSize = 12.sp, modifier = Modifier.premiumClick { onActionClick() }) }
}

data class TaskItem(val id: String, val title: String, val subtitle: String, val duration: String, val done: Boolean = false, val inProgress: Boolean = false)

@Composable
fun TaskRow(task: TaskItem, onToggle: (String) -> Unit, onClick: () -> Unit = {}) {
    val alpha by animateFloatAsState(if (task.done) .62f else 1f, label = "task-alpha")
    val check by animateColorAsState(if (task.done) AccentGreen else Color.Transparent, label = "task-check")
    val scale by animateFloatAsState(if (task.done) .985f else 1f, animationSpec = tween(180), label = "task-scale")
    Row(Modifier.fillMaxWidth().graphicsLayer(alpha = alpha, scaleX = scale, scaleY = scale).premiumClick(onClick = onClick).padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(22.dp).clip(RoundedCornerShape(7.dp)).background(check).border(1.dp, if (task.done) AccentGreen else BgCardBorder, RoundedCornerShape(7.dp)).premiumClick(haptic = if (!task.done) HapticFeedbackConstants.KEYBOARD_TAP else HapticFeedbackConstants.VIRTUAL_KEY) { onToggle(task.id) }, Alignment.Center) { if (task.done) Icon(Icons.Filled.Check, null, tint = AccentGreenDark, modifier = Modifier.size(14.dp)) }
        Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(task.title, color = TextOnCard, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), textDecoration = if (task.done) TextDecoration.LineThrough else TextDecoration.None); if (task.subtitle.isNotEmpty()) Text(task.subtitle, color = TextSecondary, style = MaterialTheme.typography.bodySmall) }
        Text(task.duration, color = TextMuted, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun JeeFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal) }, shape = JeeShapes.pill, colors = FilterChipDefaults.filterChipColors(containerColor = BgCard, labelColor = TextSecondary, selectedContainerColor = AccentBlue, selectedLabelColor = Color(0xFF17120A)))
}

@Composable
fun BottomNavBar(selected: AppTab, onTabSelected: (AppTab) -> Unit, onAiClick: () -> Unit) {
    val view=LocalView.current
    val density=LocalDensity.current
    val positions=remember{mutableStateMapOf<AppTab,androidx.compose.ui.unit.Dp>()}
    val widths=remember{mutableStateMapOf<AppTab,androidx.compose.ui.unit.Dp>()}
    val items=listOf(
        AppTab.HOME to (Icons.Filled.Home to "Home"),
        AppTab.SYLLABUS to (Icons.Filled.MenuBook to "Syllabus"),
        AppTab.TASKS to (Icons.Filled.CheckCircle to "Tasks"),
        AppTab.STATS to (Icons.Filled.Timer to "Timer")
    )
    Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(start=16.dp,end=16.dp,bottom=17.dp),contentAlignment=Alignment.Center){
        Box(Modifier.fillMaxWidth().height(66.dp).clip(RoundedCornerShape(33.dp)).background(BgCard).border(1.dp,BgCardBorder.copy(alpha=.88f),RoundedCornerShape(22.dp))){
            val targetPos=positions[selected]?:0.dp
            val targetWidth=widths[selected]?:0.dp
            val pillX by animateDpAsState(targetPos,animationSpec=spring(dampingRatio=Spring.DampingRatioNoBouncy,stiffness=Spring.StiffnessMediumLow),label="nav-pill-x")
            val pillW by animateDpAsState(targetWidth,animationSpec=spring(dampingRatio=Spring.DampingRatioNoBouncy,stiffness=Spring.StiffnessMediumLow),label="nav-pill-w")
            Box(Modifier.matchParentSize().padding(6.dp)){if(targetWidth>0.dp)Box(Modifier.offset(x=pillX).width(pillW).height(52.dp).align(Alignment.Center).clip(RoundedCornerShape(18.dp)).background(BgCardAlt).border(1.dp,BgCardBorder.copy(alpha=.82f),RoundedCornerShape(18.dp)))}
            Row(Modifier.fillMaxSize().padding(6.dp),verticalAlignment=Alignment.CenterVertically){
                NavDestination(items[0].second.first,items[0].second.second,selected==items[0].first,Modifier.onGloballyPositioned{co->positions[items[0].first]=with(density){co.positionInParent().x.toDp()};widths[items[0].first]=with(density){co.size.width.toDp()}}){if(selected!=items[0].first)view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);onTabSelected(items[0].first)}
                NavDestination(items[1].second.first,items[1].second.second,selected==items[1].first,Modifier.onGloballyPositioned{co->positions[items[1].first]=with(density){co.positionInParent().x.toDp()};widths[items[1].first]=with(density){co.size.width.toDp()}}){if(selected!=items[1].first)view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);onTabSelected(items[1].first)}
                AiDestination(Modifier.widthIn(min=48.dp),view,onAiClick)
                NavDestination(items[2].second.first,items[2].second.second,selected==items[2].first,Modifier.onGloballyPositioned{co->positions[items[2].first]=with(density){co.positionInParent().x.toDp()};widths[items[2].first]=with(density){co.size.width.toDp()}}){if(selected!=items[2].first)view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);onTabSelected(items[2].first)}
                NavDestination(items[3].second.first,items[3].second.second,selected==items[3].first,Modifier.onGloballyPositioned{co->positions[items[3].first]=with(density){co.positionInParent().x.toDp()};widths[items[3].first]=with(density){co.size.width.toDp()}}){if(selected!=items[3].first)view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);onTabSelected(items[3].first)}
            }
        }
    }
}

@Composable
private fun RowScope.NavDestination(icon:androidx.compose.ui.graphics.vector.ImageVector,label:String,selected:Boolean,modifier:Modifier,onClick:()->Unit){
    val color by animateColorAsState(if (selected) AccentBlueLight else TextSecondary, animationSpec = spring(stiffness = Spring.StiffnessMedium), label = "color")
    val scale by animateFloatAsState(if (selected) 1.08f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow), label = "scale")
    val pad by animateDpAsState(if (selected) 11.dp else 12.dp, animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow), label = "pad")
    val interaction=remember{androidx.compose.foundation.interaction.MutableInteractionSource()}
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(if(pressed).94f else 1f,animationSpec=spring(dampingRatio=Spring.DampingRatioMediumBouncy,stiffness=Spring.StiffnessMedium),label="nav-press")
    Row(modifier.widthIn(min=48.dp).graphicsLayer(scaleX=pressScale,scaleY=pressScale).clip(RoundedCornerShape(18.dp)).clickable(interactionSource=interaction,indication=androidx.compose.foundation.LocalIndication.current,onClick=onClick).padding(horizontal=pad,vertical=10.dp),horizontalArrangement=Arrangement.Center,verticalAlignment=Alignment.CenterVertically){
        Icon(icon,label,tint=color,modifier=Modifier.size(22.dp).graphicsLayer(scaleX=scale,scaleY=scale))
        AnimatedVisibility(selected,enter=fadeIn(spring(stiffness=Spring.StiffnessMediumLow))+expandHorizontally(spring(stiffness=Spring.StiffnessMediumLow)),exit=fadeOut(spring(stiffness=Spring.StiffnessMediumLow))+shrinkHorizontally(spring(stiffness=Spring.StiffnessMediumLow))){
            Row(verticalAlignment=Alignment.CenterVertically){Spacer(Modifier.width(6.dp));Text(label,color=color,fontSize=11.sp,fontWeight=FontWeight.Medium,maxLines=1)}
        }
    }
}

@Composable
private fun AiDestination(modifier:Modifier,view:android.view.View,onClick:()->Unit){
    Box(modifier.widthIn(min=48.dp).fillMaxHeight().clip(RoundedCornerShape(18.dp)).clickable(interactionSource=remember{androidx.compose.foundation.interaction.MutableInteractionSource()},indication=LocalIndication.current){view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);onClick()},contentAlignment=Alignment.Center){
        Icon(Icons.Filled.AutoAwesome,"AI",tint=TextSecondary,modifier=Modifier.size(22.dp))
    }
}
