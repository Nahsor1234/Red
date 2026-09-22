package com.example.jeecommandcenter.data

import android.content.Context

class ChapterTopicRepository(context: Context) {
    private val prefs = context.getSharedPreferences("jee_chapter_topics", Context.MODE_PRIVATE)

    fun topicsFor(chapter: JeeChapter, learning: LearningRepository): List<String> {
        val fromQuestions = learning.getQuestions().filter { it.chapterId == chapter.id }
            .map { it.topic.trim() }.filter { it.isNotBlank() }.distinct()
        return if (fromQuestions.isNotEmpty()) fromQuestions else fallbackTopics()
    }

    fun isComplete(chapterId: String, topic: String): Boolean = prefs.getBoolean(key(chapterId, topic), false)
    fun setComplete(chapterId: String, topic: String, complete: Boolean) = prefs.edit().putBoolean(key(chapterId, topic), complete).apply()
    fun completedCount(chapterId: String, topics: List<String>): Int = topics.count { isComplete(chapterId, it) }
    private fun key(chapterId: String, topic: String): String = "topic_${chapterId}_${topic.hashCode()}"
    private fun fallbackTopics(): List<String> = listOf("Core concepts", "Key formulas and definitions", "Standard JEE problems", "Previous-year questions", "Mixed practice")
}
