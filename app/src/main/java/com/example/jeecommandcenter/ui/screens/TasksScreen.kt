package com.example.jeecommandcenter.ui.screens
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeecommandcenter.data.*
import com.example.jeecommandcenter.ui.components.*
import com.example.jeecommandcenter.ui.theme.*

@Composable fun TasksScreen(repo:JeeRepository,selectedTab:AppTab,onTabSelected:(AppTab)->Unit,onFabClick:()->Unit={}){
 var selectedFilter by remember{mutableStateOf("All")};var refresh by remember{mutableIntStateOf(0)};var showAdd by remember{mutableStateOf(false)}
 val filters=listOf("All","Today","Upcoming","Completed");val all=repo.getTasks()
 val visible=all.filter{when(selectedFilter){"Today"->it.dueDay=="Today";"Upcoming"->it.dueDay!="Today"&&!it.done;"Completed"->it.done;else->true}}
 Scaffold(containerColor=BgApp,bottomBar={BottomNavBar(selectedTab,onTabSelected){showAdd=true}},floatingActionButton={ExtendedFloatingActionButton(onClick={showAdd=true},containerColor=AccentBlue,contentColor=Color.White){Icon(Icons.Filled.Add,null,modifier=Modifier.size(18.dp));Spacer(Modifier.width(6.dp));Text("Add task")}}){padding->
  Column(Modifier.fillMaxSize().padding(padding).padding(horizontal=16.dp)){Spacer(Modifier.height(12.dp));Row(Modifier.fillMaxWidth(),Arrangement.SpaceBetween,Alignment.CenterVertically){Text("Tasks",style=MaterialTheme.typography.headlineMedium.copy(fontSize=22.sp));Icon(Icons.Filled.Refresh,"Refresh",tint=TextSecondary,modifier=Modifier.premiumClick{refresh++})}
   Spacer(Modifier.height(14.dp));LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){items(filters){f->FilterChip(f,f==selectedFilter){selectedFilter=f}}};Spacer(Modifier.height(8.dp))
   LazyColumn(Modifier.weight(1f)){items(visible,key={it.id}){task->TaskCard(task,{repo.toggleTask(task.id);refresh++},{repo.deleteTask(task.id);refresh++})};item{Spacer(Modifier.height(80.dp))}}
  }
 }
 if(showAdd)AddTaskDialog(onDismiss={showAdd=false}){title,subject,duration,due->repo.addTask(title,subject,duration,due);showAdd=false;refresh++}
}
@Composable private fun TaskCard(task:AppTask,onToggle:()->Unit,onDelete:()->Unit){Row(Modifier.fillMaxWidth().padding(bottom=10.dp).clip(RoundedCornerShape(14.dp)).background(BgCard).padding(12.dp),Arrangement.SpaceBetween,Alignment.CenterVertically){
 Row(Modifier.weight(1f).premiumClick(onToggle),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(if(task.done)AccentGreen.copy(alpha=.2f) else AccentBlueSoft),contentAlignment=Alignment.Center){Icon(if(task.done)Icons.Filled.Check else Icons.Filled.MenuBook,null,tint=if(task.done)AccentGreen else AccentBlueLight,modifier=Modifier.size(16.dp))};Spacer(Modifier.width(10.dp));Column{Text(task.title,color=if(task.done)TextMuted else TextOnCard,fontSize=13.sp);Text(task.subject+" · "+task.durationMin+"m · "+task.dueDay,color=TextMuted,fontSize=11.sp)}}
 IconButton(onClick=onDelete){Icon(Icons.Filled.DeleteOutline,"Delete",tint=TextMuted,modifier=Modifier.size(18.dp))}
}}
@Composable private fun AddTaskDialog(onDismiss:()->Unit,onAdd:(String,String,Int,String)->Unit){
 var title by remember{mutableStateOf("")};var subject by remember{mutableStateOf("Physics")};var duration by remember{mutableStateOf("30")};var due by remember{mutableStateOf("Today")}
 AlertDialog(onDismissRequest=onDismiss,title={Text("Add task")},text={Column{OutlinedTextField(title,{title=it},label={Text("Task")},singleLine=true);Spacer(Modifier.height(8.dp));OutlinedTextField(subject,{subject=it},label={Text("Subject")},singleLine=true);Spacer(Modifier.height(8.dp));OutlinedTextField(duration,{duration=it.filter{c->c.isDigit()}},label={Text("Minutes")},singleLine=true);Spacer(Modifier.height(8.dp));Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip("Today",due=="Today"){due="Today"};FilterChip("Upcoming",due=="Upcoming"){due="Upcoming"}}}},confirmButton={TextButton(enabled=title.isNotBlank()&&duration.toIntOrNull()!=null,onClick={onAdd(title.trim(),subject.trim().ifBlank{"General"},duration.toInt(),due)}){Text("Add")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}})
}
