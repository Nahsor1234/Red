package com.example.jeecommandcenter.ui.screens
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import com.example.jeecommandcenter.data.JeeRepository
import com.example.jeecommandcenter.ui.components.*
import com.example.jeecommandcenter.ui.theme.*
import kotlinx.coroutines.delay

private enum class TimerPreset(val label:String,val sublabel:String,val minutes:Int){POMODORO("Pomodoro","25 min",25),SHORT("Short","15 min",15),LONG("Long","50 min",50)}
@Composable fun StudyTimerScreen(repo:JeeRepository,selectedTab:AppTab,onTabSelected:(AppTab)->Unit,onFabClick:()->Unit={}){
 var selectedPreset by remember{mutableStateOf(TimerPreset.POMODORO)};var totalSeconds by remember{mutableIntStateOf(1500)};var remaining by remember{mutableIntStateOf(1500)};var running by remember{mutableStateOf(false)};var sessionsTab by remember{mutableStateOf(false)};var refresh by remember{mutableIntStateOf(0)}
 LaunchedEffect(running){while(running&&remaining>0){delay(1000);remaining--;if(remaining==0){running=false;repo.addSession(totalSeconds/60);refresh++}}}
 fun preset(p:TimerPreset){selectedPreset=p;totalSeconds=p.minutes*60;remaining=totalSeconds;running=false}
 Scaffold(containerColor=BgApp,bottomBar={BottomNavBar(selectedTab,onTabSelected,onFabClick)}){padding->
  Column(Modifier.fillMaxSize().padding(padding).padding(horizontal=16.dp)){Spacer(Modifier.height(12.dp));Row(Modifier.fillMaxWidth(),Arrangement.SpaceBetween,Alignment.CenterVertically){Text("Study session",style=MaterialTheme.typography.headlineMedium.copy(fontSize=22.sp));Text(repo.getTodayMinutes().toString()+" min today",color=TextSecondary,fontSize=12.sp)}
   Spacer(Modifier.height(14.dp));Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(BgCard).padding(4.dp)){SegmentButton("Timer",!sessionsTab,Modifier.weight(1f)){sessionsTab=false};SegmentButton("Sessions",sessionsTab,Modifier.weight(1f)){sessionsTab=true}}
   if(sessionsTab){Spacer(Modifier.height(16.dp));val sessions=repo.getSessions();LazyColumn(Modifier.weight(1f)){items(sessions.take(30)){s->Row(Modifier.fillMaxWidth().padding(vertical=10.dp),Arrangement.SpaceBetween){Column{Text(s.subject,color=TextOnCard,fontSize=13.sp);Text(s.chapter,color=TextMuted,fontSize=11.sp)};Text(s.minutes.toString()+" min",color=AccentGreen,fontSize=12.sp)}};item{Spacer(Modifier.height(20.dp))}}}
   else{Spacer(Modifier.height(28.dp));Box(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center){val progress=if(totalSeconds>0)remaining.toFloat()/totalSeconds else 0f;Canvas(Modifier.size(220.dp)){val stroke=Stroke(width=14.dp.toPx(),cap=StrokeCap.Round);drawArc(BgDivider,-90f,360f,false,style=stroke);drawArc(AccentBlue,-90f,360f*progress,false,style=stroke)};Column(horizontalAlignment=Alignment.CenterHorizontally){Text(String.format("%02d:%02d",remaining/60,remaining%60),fontSize=44.sp,fontWeight=FontWeight.Medium,color=TextPrimary);Text(if(running)"Focus mode" else "Ready",color=TextMuted,fontSize=12.sp)}}
    Spacer(Modifier.height(28.dp));Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(BgCard).padding(14.dp),Arrangement.SpaceBetween,Alignment.CenterVertically){Column{Text("Session subject",color=TextMuted,fontSize=11.sp);Text("General study",style=MaterialTheme.typography.titleMedium);Text("Saved automatically when the timer completes",color=TextMuted,fontSize=11.sp)}}
    Spacer(Modifier.height(14.dp));Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){TimerPreset.values().forEach{p->PresetCard(p,selectedPreset==p,Modifier.weight(1f)){preset(p)}}}
    Spacer(Modifier.height(18.dp));Button(onClick={if(remaining==0){remaining=totalSeconds};running=!running},modifier=Modifier.fillMaxWidth().height(52.dp),shape=RoundedCornerShape(14.dp),colors=ButtonDefaults.buttonColors(containerColor=AccentBlue)){Icon(if(running)Icons.Filled.Pause else Icons.Filled.PlayArrow,null,tint=Color.White);Spacer(Modifier.width(8.dp));Text(if(running)"Pause" else "Start",color=Color.White)}
    Spacer(Modifier.height(14.dp));Text("Completed sessions are added to your dashboard automatically.",color=TextMuted,fontSize=12.sp,modifier=Modifier.fillMaxWidth())}
  }
 }
}
@Composable private fun SegmentButton(label:String,selected:Boolean,modifier:Modifier=Modifier,onClick:()->Unit){Box(modifier.clip(RoundedCornerShape(8.dp)).background(if(selected)AccentBlue else Color.Transparent).premiumClick(onClick).padding(vertical=8.dp),contentAlignment=Alignment.Center){Text(label,color=if(selected)Color.White else TextSecondary,fontSize=13.sp,fontWeight=FontWeight.Medium)}}
@Composable private fun PresetCard(p:TimerPreset,selected:Boolean,modifier:Modifier=Modifier,onClick:()->Unit){Column(modifier.clip(RoundedCornerShape(12.dp)).background(BgCard).border(1.dp,if(selected)AccentBlue else Color.Transparent,RoundedCornerShape(12.dp)).premiumClick(onClick).padding(vertical=12.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(p.label,color=if(selected)AccentBlue else TextOnCard,fontSize=13.sp,fontWeight=FontWeight.Medium);Text(p.sublabel,color=TextMuted,fontSize=11.sp)}}
