package com.example.jeecommandcenter.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

data class AppTask(val id: Long,val title: String,val subject: String,val durationMin: Int,val dueDay: String,val done: Boolean=false)
data class StudySession(val id: Long,val date: String,val minutes: Int,val subject: String,val chapter: String)
data class ChapterProgress(val subject: String,val number: Int,val name: String,val progress: Float)

class JeeRepository(context: Context) {
 private val prefs=context.getSharedPreferences("jee_command_center",Context.MODE_PRIVATE)
 fun getTasks():List<AppTask>{ val raw=prefs.getString("tasks",null)?:return defaultTasks(); return runCatching{val a=JSONArray(raw);List(a.length()){i->val o=a.getJSONObject(i);AppTask(o.getLong("id"),o.getString("title"),o.getString("subject"),o.getInt("duration"),o.getString("dueDay"),o.optBoolean("done"))}}.getOrElse{defaultTasks()} }
 fun addTask(title:String,subject:String,durationMin:Int,dueDay:String="Today"){val t=getTasks().toMutableList();t.add(AppTask(System.currentTimeMillis(),title,subject,durationMin,dueDay));saveTasks(t)}
 fun toggleTask(id:Long){saveTasks(getTasks().map{if(it.id==id)it.copy(done=!it.done)else it})}
 fun deleteTask(id:Long){saveTasks(getTasks().filterNot{it.id==id})}
 private fun saveTasks(tasks:List<AppTask>){val a=JSONArray();tasks.forEach{a.put(JSONObject().apply{put("id",it.id);put("title",it.title);put("subject",it.subject);put("duration",it.durationMin);put("dueDay",it.dueDay);put("done",it.done)})};prefs.edit().putString("tasks",a.toString()).apply()}
 fun getSessions():List<StudySession>{val raw=prefs.getString("sessions",null)?:return emptyList();return runCatching{val a=JSONArray(raw);List(a.length()){i->val o=a.getJSONObject(i);StudySession(o.getLong("id"),o.getString("date"),o.getInt("minutes"),o.getString("subject"),o.getString("chapter"))}.sortedByDescending{it.id}}.getOrDefault(emptyList())}
 fun addSession(minutes:Int,subject:String="General",chapter:String="Self study"){if(minutes<=0)return;val s=getSessions().toMutableList();s.add(StudySession(System.currentTimeMillis(),LocalDate.now().toString(),minutes,subject,chapter));val a=JSONArray();s.take(500).forEach{a.put(JSONObject().apply{put("id",it.id);put("date",it.date);put("minutes",it.minutes);put("subject",it.subject);put("chapter",it.chapter)})};prefs.edit().putString("sessions",a.toString()).apply()}
 fun getTodayMinutes()=getSessions().filter{it.date==LocalDate.now().toString()}.sumOf{it.minutes}
 fun getDailyGoalMinutes()=prefs.getInt("daily_goal",360)
 fun setDailyGoalMinutes(v:Int)=prefs.edit().putInt("daily_goal",v.coerceIn(15,1440)).apply()
 fun getChapterProgress(subject:String,number:Int)=prefs.getFloat("chapter_"+subject+"_"+number,0f)
 fun setChapterProgress(subject:String,number:Int,p:Float)=prefs.edit().putFloat("chapter_"+subject+"_"+number,p.coerceIn(0f,1f)).apply()
 fun chapters(subject:String)= (syllabus[subject]?:emptyList()).mapIndexed{i,n->ChapterProgress(subject,i+1,n,getChapterProgress(subject,i+1))}
 private fun defaultTasks()=listOf(AppTask(1,"Study kinematics theory","Physics",60,"Today",true),AppTask(2,"Solve 30 questions","Physics",45,"Today"),AppTask(3,"Revise formula sheet","Physics",30,"Today"),AppTask(4,"Chemical bonding notes","Chemistry",60,"Tomorrow"),AppTask(5,"Quadratic equations practice","Mathematics",60,"Tomorrow"))
 companion object{val syllabus=mapOf("Physics" to listOf("Units and measurements","Motion in a straight line","Motion in a plane","Laws of motion","Work, energy and power","System of particles","Rotational motion","Gravitation","Mechanical properties of solids","Mechanical properties of fluids","Thermal properties of matter","Thermodynamics","Kinetic theory","Oscillations","Waves","Electric charges and fields","Electrostatic potential and capacitance","Current electricity","Moving charges and magnetism","Magnetism and matter","Electromagnetic induction","Alternating current","Electromagnetic waves","Ray optics","Wave optics","Dual nature of matter","Atoms","Nuclei","Semiconductor electronics"),"Chemistry" to listOf("Some basic concepts of chemistry","Structure of atom","Classification of elements","Chemical bonding","Thermodynamics","Equilibrium","Redox reactions","Organic chemistry basics","Hydrocarbons","Solutions","Electrochemistry","Chemical kinetics","Surface chemistry","p-Block elements","d- and f-Block elements","Coordination compounds","Haloalkanes and haloarenes","Alcohols, phenols and ethers","Aldehydes, ketones and carboxylic acids","Amines","Biomolecules and polymers","Practical chemistry"),"Mathematics" to listOf("Sets","Relations and functions","Trigonometric functions","Complex numbers","Quadratic equations","Sequences and series","Permutations and combinations","Binomial theorem","Straight lines","Circles","Conic sections","Limits","Continuity and differentiability","Application of derivatives","Integrals","Application of integrals","Differential equations","Matrices","Determinants","Vector algebra","Three dimensional geometry","Statistics","Probability","Mathematical reasoning"))}
}
