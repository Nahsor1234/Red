package com.example.jeecommandcenter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeecommandcenter.data.*
import com.example.jeecommandcenter.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AiHubScreen(context:android.content.Context,onBack:()->Unit,onOpenTutor:()->Unit,onOpenSettings:()->Unit){
 val settings=remember{AiSettingsRepository(context)};val orchestrator=remember{AiOrchestrator(context)};val scope=rememberCoroutineScope();val view=LocalView.current;var busy by remember{mutableStateOf(false)};var title by remember{mutableStateOf<String?>(null)};var result by remember{mutableStateOf("")};val config=settings.getConfig();val hasKey=settings.hasApiKey()
 fun run(t:String,a:suspend()->AiResult){if(!hasKey||busy)return;view.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK);busy=true;title=t;scope.launch{val r=a();result=if(r.success)r.text else(r.error?:"AI request failed.");busy=false}}
 Scaffold(containerColor=BgApp){p->LazyColumn(Modifier.fillMaxSize().padding(p).padding(horizontal=18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){item{Spacer(Modifier.height(8.dp));Row(Modifier.fillMaxWidth(),Arrangement.SpaceBetween,Alignment.CenterVertically){Column{Text("AI study coach",style=MaterialTheme.typography.headlineSmall);Text("Analysis and actions from your real study data",color=TextMuted,fontSize=11.sp)};TextButton(onClick={view.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK);onOpenSettings()}){Text("Settings",fontSize=12.sp)}}}
 item{Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(BgCardAlt).border(JeeSurfaceTokens.borderWidth,BgCardBorder.copy(alpha=JeeSurfaceTokens.cardBorderAlpha),JeeShapes.medium).padding(17.dp)){Text("BASED ON YOUR STUDY DATA",color=TextSecondary,fontSize=11.sp);Spacer(Modifier.height(6.dp));Text(if(hasKey)"AI is ready" else "AI is optional",style=MaterialTheme.typography.titleLarge);Text(if(hasKey)"${config.provider.label} · ${config.model}" else "Configure a provider in Settings when you need AI.",color=TextMuted,fontSize=11.sp)}}
 item{Text("What do you want to understand?",style=MaterialTheme.typography.titleMedium)}
 item{AiAction("Analyze my preparation","Find weaknesses, gaps and recent patterns.",Icons.Filled.Insights,hasKey&&!busy){run("Performance analysis"){orchestrator.analyzePerformance()}}}
 item{AiAction("What should I study now?","Build a focused next-step plan from your data.",Icons.Filled.Today,hasKey&&!busy){run("Today's study plan"){orchestrator.buildDailyStudyPlan()}}}
 item{AiAction("Analyze my mistakes","Find recurring mistake patterns and chapters.",Icons.Filled.ErrorOutline,hasKey&&!busy){run("Mistake analysis"){orchestrator.explainMistakes()}}}
 item{AiAction("Ask the study coach","Chat about your preparation and context.",Icons.Filled.Chat,hasKey){view.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK);onOpenTutor()}}
 if(title!=null)item{Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(BgCard).border(JeeSurfaceTokens.borderWidth,BgCardBorder.copy(alpha=JeeSurfaceTokens.cardBorderAlpha),JeeShapes.medium).padding(15.dp)){Text(title!!,style=MaterialTheme.typography.titleMedium);Spacer(Modifier.height(8.dp));if(busy)LinearProgressIndicator(Modifier.fillMaxWidth(),color=AccentBlue)else Text(result,color=TextOnCard,fontSize=13.sp)}}
 if(!hasKey)item{Text("Provider, model and API key are managed in Settings. AI does not replace your stored study data.",color=TextMuted,fontSize=10.sp)};item{Spacer(Modifier.height(20.dp))}}}
}
@Composable private fun AiAction(title:String,subtitle:String,icon:androidx.compose.ui.graphics.vector.ImageVector,enabled:Boolean,onClick:()->Unit){ElevatedCard(onClick=onClick,enabled=enabled,modifier=Modifier.fillMaxWidth(),colors=CardDefaults.elevatedCardColors(containerColor=BgCard)){ListItem(headlineContent={Text(title)},supportingContent={Text(subtitle)},leadingContent={Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(AccentBlueSoft),Alignment.Center){Icon(icon,null,tint=if(enabled)AccentBlue else TextMuted,modifier=Modifier.size(21.dp))}},trailingContent={Icon(Icons.Filled.ChevronRight,null,tint=TextMuted)},colors=ListItemDefaults.colors(containerColor=Color.Transparent))}}
