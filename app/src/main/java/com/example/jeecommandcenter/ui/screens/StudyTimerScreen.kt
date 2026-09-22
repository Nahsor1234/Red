package com.example.jeecommandcenter.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeecommandcenter.data.*
import com.example.jeecommandcenter.ui.components.*
import com.example.jeecommandcenter.ui.theme.*
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.ceil

private enum class TimerPreset(val label:String,val sublabel:String,val minutes:Int){POMODORO("Pomodoro","25 min",25),SHORT("Short","15 min",15),LONG("Long","50 min",50)}

@Composable fun StudyTimerScreen(repo:JeeRepository,selectedTab:AppTab,onTabSelected:(AppTab)->Unit,onAiClick:()->Unit={}){
 val initial=remember{repo.restoreTimerState()};var selected by remember{mutableStateOf(TimerPreset.values().firstOrNull{it.minutes*60==initial.totalSeconds})};var custom by remember{mutableStateOf((initial.totalSeconds/60).coerceAtLeast(1).toString())};var showCustom by remember{mutableStateOf(false)};var total by remember{mutableIntStateOf(initial.totalSeconds)};var remaining by remember{mutableIntStateOf(initial.remainingSeconds)};var running by remember{mutableStateOf(initial.running)};var end by remember{mutableLongStateOf(initial.endAtMillis)};var sessions by remember{mutableStateOf(false)};var refresh by remember{mutableIntStateOf(0)};var subject by remember{mutableStateOf("General")};var chapter by remember{mutableStateOf<String?>(null)};var activity by remember{mutableStateOf(ActivityType.LEARNING)};val view=LocalView.current
 LaunchedEffect(running){if(!running)return@LaunchedEffect;while(running){val r=ceil((end-System.currentTimeMillis()).coerceAtLeast(0)/1000.0).toInt().coerceIn(0,total);remaining=r;if(r==0){running=false;end=0;repo.completeTimer(total,subject,chapter,activity);refresh++;view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);break};delay(250)}}
 fun preset(p:TimerPreset){selected=p;total=p.minutes*60;remaining=total;running=false;end=0;repo.resetTimer(total);view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)}
 Scaffold(containerColor=BgApp,bottomBar={BottomNavBar(selectedTab,onTabSelected,onAiClick)}){p->Column(Modifier.fillMaxSize().padding(p).padding(horizontal=18.dp)){Spacer(Modifier.height(10.dp));Row(Modifier.fillMaxWidth(),Arrangement.SpaceBetween,Alignment.CenterVertically){Text("Study session",style=MaterialTheme.typography.headlineSmall);Text("${remember(refresh){repo.getTodayMinutes()}} min today",color=TextMuted,fontSize=11.sp)};Spacer(Modifier.height(12.dp));Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(BgCard).padding(4.dp)){Segment("Timer",!sessions){sessions=false};Segment("Sessions",sessions){sessions=true}}
 if(sessions){Spacer(Modifier.height(12.dp));val list=remember(refresh){repo.getSessions()};LazyColumn(Modifier.weight(1f)){items(list,key={it.id}){s->Row(Modifier.fillMaxWidth().padding(vertical=12.dp),Arrangement.SpaceBetween){Column{Text(s.subject,fontSize=14.sp);Text(s.chapter,color=TextMuted,fontSize=10.sp)};Column(horizontalAlignment=Alignment.End){Text("${s.minutes} min",color=AccentGreen,fontSize=12.sp);Text(s.date,color=TextMuted,fontSize=10.sp)}};HorizontalDivider(color=BgDivider,thickness=.5.dp)};item{Spacer(Modifier.height(20.dp))}}}
 else{Spacer(Modifier.height(18.dp));Box(Modifier.fillMaxWidth().height(250.dp),Alignment.Center){val progress=if(total>0)remaining.toFloat()/total else 0f;Canvas(Modifier.size(224.dp)){val st=Stroke(12.dp.toPx(),cap=StrokeCap.Round);drawArc(BgDivider,-90f,360f,false,style=st);drawArc(AccentBlue,-90f,360f*progress,false,style=st)};Column(horizontalAlignment=Alignment.CenterHorizontally){Text(String.format(Locale.US,"%02d:%02d",remaining/60,remaining%60),fontSize=46.sp,fontWeight=FontWeight.Normal);Text(if(running)"Focus mode"else"Ready",color=TextMuted,fontSize=12.sp)}}
 Spacer(Modifier.height(12.dp));Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(BgCardAlt).padding(15.dp)){Text("SESSION CONTEXT",color=TextSecondary,fontSize=11.sp);Text(subject,style=MaterialTheme.typography.titleMedium);Text(chapter?.let{JeeCatalog.find(it)?.name}?:"No chapter linked",color=TextMuted,fontSize=11.sp);Text("Activity: ${activity.name.lowercase().replace('_',' ')}",color=TextMuted,fontSize=10.sp)}
 Spacer(Modifier.height(12.dp));Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){TimerPreset.values().forEach{p->Preset(p.label,p.sublabel,selected==p,Modifier.weight(1f)){preset(p)}};Preset("Custom","${total/60} min",selected==null,Modifier.weight(1f)){custom=(total/60).coerceAtLeast(1).toString();showCustom=true}}
 Spacer(Modifier.height(14.dp));Button(onClick={if(running){remaining=ceil((end-System.currentTimeMillis()).coerceAtLeast(0)/1000.0).toInt().coerceIn(0,total);running=false;end=0;repo.pauseTimer(total,remaining)}else{if(remaining<=0)remaining=total;repo.startTimer(total,remaining);end=System.currentTimeMillis()+remaining*1000;running=true};view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)},modifier=Modifier.fillMaxWidth().height(52.dp),shape=RoundedCornerShape(17.dp),colors=ButtonDefaults.buttonColors(containerColor=AccentBlue)){Icon(if(running)Icons.Filled.Pause else Icons.Filled.PlayArrow,null,tint=Color(0xFF17120A));Spacer(Modifier.width(8.dp));Text(if(running)"Pause"else"Start session",color=Color(0xFF17120A))};Spacer(Modifier.height(8.dp));Text("Completed sessions are added to your dashboard automatically.",color=TextMuted,fontSize=11.sp)}}}
 if(showCustom)AlertDialog(onDismissRequest={showCustom=false},title={Text("Custom timer")},text={OutlinedTextField(custom,{custom=it.filter(Char::isDigit).take(4)},label={Text("Minutes")},singleLine=true)},confirmButton={val m=custom.toIntOrNull();TextButton(enabled=m!=null&&m in 1..1440,onClick={m?.let{selected=null;total=it*60;remaining=total;running=false;end=0;repo.resetTimer(total);showCustom=false}}){Text("Use")}},dismissButton={TextButton(onClick={showCustom=false}){Text("Cancel")}})
}
@Composable private fun RowScope.Segment(label:String,selected:Boolean,onClick:()->Unit){Box(Modifier.weight(1f).clip(RoundedCornerShape(9.dp)).background(if(selected)AccentBlue else Color.Transparent).premiumClick(onClick).padding(vertical=8.dp),Alignment.Center){Text(label,color=if(selected)Color(0xFF17120A)else TextSecondary,fontSize=12.sp,fontWeight=FontWeight.Medium)}}
@Composable private fun Preset(label:String,sub:String,selected:Boolean,modifier:Modifier,onClick:()->Unit){Column(modifier.clip(RoundedCornerShape(15.dp)).background(BgCard).border(1.dp,if(selected)AccentBlue else Color.Transparent,RoundedCornerShape(15.dp)).premiumClick(onClick).padding(vertical=11.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(label,color=if(selected)AccentBlue else TextOnCard,fontSize=12.sp,fontWeight=FontWeight.Medium);Text(sub,color=TextMuted,fontSize=10.sp)}}
