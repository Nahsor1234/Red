package com.example.jeecommandcenter.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class ChapterTopic(val id: String, val chapterId: String, val title: String, val completed: Boolean)

class TopicRepository(context: Context) {
    private val prefs = context.getSharedPreferences("jee_topics", Context.MODE_PRIVATE)

    fun topicsFor(chapter: JeeChapter): List<ChapterTopic> {
        val saved = read(chapter.id).associateBy { it.id }
        return defaultTopics(chapter).map { topic -> saved[topic.id] ?: topic }
    }

    fun setCompleted(chapterId: String, topicId: String, completed: Boolean) {
        val current = read(chapterId).toMutableList()
        val index = current.indexOfFirst { it.id == topicId }
        if (index >= 0) current[index] = current[index].copy(completed = completed)
        else current.add(ChapterTopic(topicId, chapterId, topicId.substringAfterLast('_').replace('_', ' '), completed))
        write(chapterId, current)
    }

    private fun read(chapterId: String): List<ChapterTopic> {
        val raw = prefs.getString("chapter_$chapterId", null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { i ->
                val o = array.getJSONObject(i)
                ChapterTopic(o.getString("id"), chapterId, o.getString("title"), o.optBoolean("completed"))
            }
        }.getOrDefault(emptyList())
    }

    private fun write(chapterId: String, topics: List<ChapterTopic>) {
        val array = JSONArray()
        topics.forEach { array.put(JSONObject().put("id", it.id).put("title", it.title).put("completed", it.completed)) }
        prefs.edit().putString("chapter_$chapterId", array.toString()).apply()
    }

    private fun defaultTopics(chapter: JeeChapter): List<ChapterTopic> {
        val key = chapter.name.lowercase()
        val specific = when {
            "motion in a straight" in key -> listOf("Position, distance and displacement", "Speed and velocity", "Acceleration", "Graphs of motion", "Equations of uniformly accelerated motion", "Relative motion")
            "laws of motion" in key -> listOf("Newton's laws", "Free-body diagrams", "Friction", "Tension and connected bodies", "Circular motion applications", "Pseudo force")
            "work, energy" in key -> listOf("Work by constant force", "Work-energy theorem", "Kinetic and potential energy", "Conservation of mechanical energy", "Power", "Collisions")
            "electric charges" in key -> listOf("Charge and Coulomb's law", "Electric field", "Electric flux", "Gauss's law", "Electric dipole", "Applications of Gauss's law")
            "electrostatic potential" in key -> listOf("Electric potential", "Potential due to charges", "Equipotential surfaces", "Capacitance", "Dielectrics", "Energy stored in a capacitor")
            "current electricity" in key -> listOf("Electric current", "Drift velocity", "Ohm's law", "Resistance and resistivity", "Kirchhoff's laws", "Cells and combinations")
            "chemical bonding" in key -> listOf("Lewis structures", "Formal charge", "VSEPR theory", "Hybridisation", "Molecular orbital theory", "Hydrogen bonding")
            "thermodynamics" in key -> listOf("System and surroundings", "First law", "Enthalpy", "Entropy", "Gibbs energy", "Thermochemical calculations")
            "quadratic equations" in key -> listOf("Roots and coefficients", "Nature of roots", "Relations between roots", "Quadratic inequalities", "Parameter-based equations", "Graphical interpretation")
            "complex numbers" in key -> listOf("Algebra of complex numbers", "Argand plane", "Modulus and argument", "Polar form", "Conjugate", "Roots of complex equations")
            "limits" == key -> listOf("Limit laws", "Standard limits", "Algebraic simplification", "Trigonometric limits", "Exponential and logarithmic limits", "One-sided limits")
            "continuity" in key -> listOf("Continuity at a point", "Continuity on intervals", "Differentiability", "Derivative rules", "Implicit differentiation", "Higher derivatives")
            "probability" in key -> listOf("Sample space and events", "Addition theorem", "Conditional probability", "Bayes theorem", "Independent events", "Random variables")
            else -> listOf("Core concepts and definitions", "Key formulas and principles", "Standard solved examples", "Common problem types", "Advanced applications", "Previous-year question practice")
        }
        return specific.mapIndexed { index, title -> ChapterTopic("${chapter.id}_$index", chapter.id, title, false) }
    }
}
