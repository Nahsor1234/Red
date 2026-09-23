package com.example.jeecommandcenter.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
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
            val h = mixHash(row,col,17)
            val h2 = mixHash(row,col,43)
            val scale = .72f + (h % 36) / 100f
            val alpha = .028f + (h2 % 24) / 1000f
            val ox = ((mixHash(row,col,71)%35)-17).dp.toPx()
            val oy = ((mixHash(row,col,97)%31)-15).dp.toPx()
            val o = androidx.compose.ui.geometry.Offset(col*step+ox,row*step+oy)
            val s = scale*11.dp.toPx()
            val st = (.75f+(h2%40)/100f).dp.toPx()
            val color = Color.White.copy(alpha=alpha)
            when(h%12){
                0->{ drawLine(color,o,o.copy(x=o.x+s*1.5f),strokeWidth=st); drawLine(color,o,o.copy(y=o.y-s*1.25f),strokeWidth=st); drawCircle(color,s*.18f,o.copy(x=o.x+s*.9f,y=o.y-s*.55f)) }
                1->{ val a=o.copy(x=o.x-s*.8f,y=o.y+s*.55f); val b=o.copy(x=o.x+s*.9f,y=o.y+s*.55f); val d=o.copy(x=o.x+s*.15f,y=o.y-s*.9f); drawLine(color,a,b,strokeWidth=st); drawLine(color,b,d,strokeWidth=st); drawLine(color,d,a,strokeWidth=st); drawCircle(color,s*.12f,a) }
                2->{ drawArc(color,210f,140f,false,o.copy(x=o.x-s,y=o.y-s*.2f),androidx.compose.ui.geometry.Size(s*1.9f,s*1.4f),strokeWidth=st); drawLine(color,o.copy(x=o.x-s*.15f),o.copy(x=o.x-s*.55f,y=o.y-s*.8f),strokeWidth=st) }
                3->{ var last=o.copy(x=o.x-s); for(i in 1..8){val next=o.copy(x=o.x-s+i*(s*.25f),y=o.y+kotlin.math.sin(i*.75f)*s*.42f); drawLine(color,last,next,strokeWidth=st); last=next} }
                4->{ val a=o.copy(x=o.x-s*.65f); val b=o.copy(x=o.x+s*.65f,y=o.y-s*.3f); val d=o.copy(y=o.y+s*.7f); drawLine(color,a,b,strokeWidth=st); drawLine(color,a,d,strokeWidth=st); drawLine(color,b,d,strokeWidth=st); drawCircle(color,s*.2f,a); drawCircle(color,s*.16f,b); drawCircle(color,s*.18f,d) }
                5->{ drawCircle(color,s*.55f,o,androidx.compose.ui.graphics.drawscope.Stroke(st)); drawCircle(color,s*.28f,o,androidx.compose.ui.graphics.drawscope.Stroke(st)); drawCircle(color,s*.08f,o); drawLine(color,o.copy(x=o.x-s*.85f),o.copy(x=o.x+s*.85f),strokeWidth=st) }
                6->{ val pts=(0 until 6).map{i->val a=Math.toRadians((60*i-30).toDouble()); o.copy(x=o.x+kotlin.math.cos(a).toFloat()*s*.75f,y=o.y+kotlin.math.sin(a).toFloat()*s*.75f)}; pts.forEachIndexed{i,p0->drawLine(color,p0,pts[(i+1)%pts.size],strokeWidth=st)}; drawCircle(color,s*.28f,o,androidx.compose.ui.graphics.drawscope.Stroke(st)) }
                7->{ val a=o.copy(x=o.x-s,y=o.y+s*.45f); val b=o.copy(x=o.x+s*.95f,y=o.y-s*.35f); drawLine(color,a,b,strokeWidth=st); drawLine(color,b,b.copy(x=b.x-s*.4f),strokeWidth=st); drawLine(color,b,b.copy(x=b.x-s*.12f,y=b.y+s*.28f),strokeWidth=st) }
                8->{ val a=o.copy(x=o.x-s); val b=o.copy(x=o.x-s*.35f); val d=o.copy(x=o.x+s*.35f); val q=o.copy(x=o.x+s); drawLine(color,a,b,strokeWidth=st); drawLine(color,d,q,strokeWidth=st); drawRoundRect(color,b.copy(y=b.y-s*.28f),androidx.compose.ui.geometry.Size(s*.7f,s*.56f),androidx.compose.ui.geometry.CornerRadius(s*.08f),style=androidx.compose.ui.graphics.drawscope.Stroke(st)) }
                9->{ drawLine(color,o.copy(x=o.x-s,y=o.y+s*.75f),o.copy(x=o.x+s,y=o.y+s*.75f),strokeWidth=st); drawLine(color,o.copy(x=o.x-s,y=o.y+s*.75f),o.copy(x=o.x-s,y=o.y-s),strokeWidth=st); var last=o.copy(x=o.x-s,y=o.y+s*.35f); for(i in 1..8){val next=o.copy(x=o.x-s+i*(s*.25f),y=o.y+kotlin.math.cos(i*.6f)*s*.45f); drawLine(color,last,next,strokeWidth=st); last=next} }
                10->{ drawArc(color,195f,150f,false,o.copy(x=o.x-s,y=o.y-s*.35f),androidx.compose.ui.geometry.Size(s*2f,s*1.3f),strokeWidth=st); drawArc(color,195f,150f,false,o.copy(x=o.x-s*.7f,y=o.y-s*.15f),androidx.compose.ui.geometry.Size(s*1.4f,s*.9f),strokeWidth=st) }
                else->{ drawLine(color,o.copy(x=o.x-s),o.copy(x=o.x+s),strokeWidth=st); drawLine(color,o.copy(x=o.x-s*.35f,y=o.y-s*.55f),o.copy(x=o.x+s*.35f,y=o.y-s*.55f),strokeWidth=st); drawCircle(color,s*.22f,o.copy(y=o.y+s*.55f),androidx.compose.ui.graphics.drawscope.Stroke(st)) }
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
    Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal=12.dp,vertical=8.dp),contentAlignment=Alignment.Center){
        Box(Modifier.wrapContentWidth().height(56.dp).clip(RoundedCornerShape(22.dp)).background(BgCard).border(1.dp,BgCardBorder.copy(alpha=.88f),RoundedCornerShape(22.dp))){
            val targetPos=positions[selected]?:0.dp
            val targetWidth=widths[selected]?:0.dp
            val pillX by animateDpAsState(targetPos,animationSpec=spring(dampingRatio=Spring.DampingRatioNoBouncy,stiffness=Spring.StiffnessMediumLow),label="nav-pill-x")
            val pillW by animateDpAsState(targetWidth,animationSpec=spring(dampingRatio=Spring.DampingRatioNoBouncy,stiffness=Spring.StiffnessMediumLow),label="nav-pill-w")
            Box(Modifier.matchParentSize().padding(4.dp)){if(targetWidth>0.dp)Box(Modifier.offset(x=pillX).width(pillW).fillMaxHeight().clip(RoundedCornerShape(18.dp)).background(BgCardAlt).border(1.dp,BgCardBorder.copy(alpha=.82f),RoundedCornerShape(18.dp)))}
            Row(Modifier.padding(4.dp),verticalAlignment=Alignment.CenterVertically){
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
    val transition=updateTransition(selected,label="nav-$label")
    val color by transition.animateColor({spring(stiffness=Spring.StiffnessMedium)},"color"){if(it)AccentBlueLight else TextSecondary}
    val scale by transition.animateFloat({spring(dampingRatio=Spring.DampingRatioMediumBouncy,stiffness=Spring.StiffnessMediumLow)},"scale"){if(it)1.08f else 1f}
    val pad by transition.animateDp({spring(dampingRatio=Spring.DampingRatioNoBouncy,stiffness=Spring.StiffnessMediumLow)},"pad"){if(it)11.dp else 12.dp}
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

