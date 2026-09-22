package com.example.jeecommandcenter.data

data class ChapterProgress(
    val subject: String,
    val number: Int,
    val name: String,
    val progress: Float,
    val confidence: Int = 0,
    val difficulty: Int = 3,
    val estimatedMinutes: Int = 240
)

data class JeeChapter(
    val id: String,
    val subject: String,
    val number: Int,
    val name: String,
    val difficulty: Int = 3,
    val estimatedMinutes: Int = 240
)

data class ChapterState(
    val chapterId: String,
    val progress: Float,
    val confidence: Int = 0,
    val lastStudiedAt: Long = 0L
) {
    val mastered: Boolean
        get() = progress >= 1f && confidence >= 3
}

enum class RevisionRating {
    AGAIN,
    HARD,
    GOOD,
    EASY
}

data class RevisionItem(
    val id: String,
    val chapterId: String,
    val dueDate: String,
    val intervalDays: Int = 1,
    val ease: Float = 2.5f,
    val repetitions: Int = 0,
    val lapses: Int = 0,
    val lastReviewedAt: Long = 0L
)

enum class PlannerItemType {
    REVISION,
    STUDY,
    PRACTICE
}

data class PlannerItem(
    val id: String,
    val date: String,
    val type: PlannerItemType,
    val title: String,
    val subject: String,
    val chapterId: String? = null,
    val durationMin: Int,
    val priority: Int,
    val completed: Boolean = false
)

data class DailyPlan(
    val date: String,
    val goalMinutes: Int,
    val items: List<PlannerItem>
)

data class RevisionQueueItem(
    val chapter: JeeChapter,
    val revision: RevisionItem,
    val progress: Float,
    val confidence: Int
)

object JeeCatalog {
    val chapters: List<JeeChapter> = listOf(
        chapter("Physics", 1, "Units and measurements", 2, 90),
        chapter("Physics", 2, "Motion in a straight line", 2, 180),
        chapter("Physics", 3, "Motion in a plane", 3, 210),
        chapter("Physics", 4, "Laws of motion", 3, 240),
        chapter("Physics", 5, "Work, energy and power", 3, 210),
        chapter("Physics", 6, "System of particles", 3, 240),
        chapter("Physics", 7, "Rotational motion", 5, 360),
        chapter("Physics", 8, "Gravitation", 3, 210),
        chapter("Physics", 9, "Mechanical properties of solids", 2, 150),
        chapter("Physics", 10, "Mechanical properties of fluids", 3, 180),
        chapter("Physics", 11, "Thermal properties of matter", 2, 180),
        chapter("Physics", 12, "Thermodynamics", 4, 270),
        chapter("Physics", 13, "Kinetic theory", 2, 120),
        chapter("Physics", 14, "Oscillations", 3, 210),
        chapter("Physics", 15, "Waves", 3, 240),
        chapter("Physics", 16, "Electric charges and fields", 4, 270),
        chapter("Physics", 17, "Electrostatic potential and capacitance", 4, 270),
        chapter("Physics", 18, "Current electricity", 4, 270),
        chapter("Physics", 19, "Moving charges and magnetism", 4, 300),
        chapter("Physics", 20, "Magnetism and matter", 3, 180),
        chapter("Physics", 21, "Electromagnetic induction", 4, 270),
        chapter("Physics", 22, "Alternating current", 4, 240),
        chapter("Physics", 23, "Electromagnetic waves", 2, 90),
        chapter("Physics", 24, "Ray optics", 4, 300),
        chapter("Physics", 25, "Wave optics", 4, 240),
        chapter("Physics", 26, "Dual nature of matter", 2, 120),
        chapter("Physics", 27, "Atoms", 2, 120),
        chapter("Physics", 28, "Nuclei", 2, 120),
        chapter("Physics", 29, "Semiconductor electronics", 3, 180),

        chapter("Chemistry", 1, "Some basic concepts of chemistry", 2, 150),
        chapter("Chemistry", 2, "Structure of atom", 2, 150),
        chapter("Chemistry", 3, "Classification of elements", 2, 150),
        chapter("Chemistry", 4, "Chemical bonding", 4, 270),
        chapter("Chemistry", 5, "Thermodynamics", 4, 270),
        chapter("Chemistry", 6, "Equilibrium", 4, 270),
        chapter("Chemistry", 7, "Redox reactions", 3, 180),
        chapter("Chemistry", 8, "Organic chemistry basics", 4, 270),
        chapter("Chemistry", 9, "Hydrocarbons", 3, 210),
        chapter("Chemistry", 10, "Solutions", 3, 180),
        chapter("Chemistry", 11, "Electrochemistry", 4, 240),
        chapter("Chemistry", 12, "Chemical kinetics", 3, 210),
        chapter("Chemistry", 13, "Surface chemistry", 2, 120),
        chapter("Chemistry", 14, "p-Block elements", 4, 300),
        chapter("Chemistry", 15, "d- and f-Block elements", 3, 210),
        chapter("Chemistry", 16, "Coordination compounds", 4, 270),
        chapter("Chemistry", 17, "Haloalkanes and haloarenes", 3, 210),
        chapter("Chemistry", 18, "Alcohols, phenols and ethers", 3, 210),
        chapter("Chemistry", 19, "Aldehydes, ketones and carboxylic acids", 4, 270),
        chapter("Chemistry", 20, "Amines", 3, 180),
        chapter("Chemistry", 21, "Biomolecules and polymers", 2, 150),
        chapter("Chemistry", 22, "Practical chemistry", 2, 120),

        chapter("Mathematics", 1, "Sets", 2, 90),
        chapter("Mathematics", 2, "Relations and functions", 3, 180),
        chapter("Mathematics", 3, "Trigonometric functions", 4, 240),
        chapter("Mathematics", 4, "Complex numbers", 4, 240),
        chapter("Mathematics", 5, "Quadratic equations", 3, 180),
        chapter("Mathematics", 6, "Sequences and series", 3, 180),
        chapter("Mathematics", 7, "Permutations and combinations", 4, 240),
        chapter("Mathematics", 8, "Binomial theorem", 3, 150),
        chapter("Mathematics", 9, "Straight lines", 3, 180),
        chapter("Mathematics", 10, "Circles", 3, 180),
        chapter("Mathematics", 11, "Conic sections", 4, 270),
        chapter("Mathematics", 12, "Limits", 3, 180),
        chapter("Mathematics", 13, "Continuity and differentiability", 4, 270),
        chapter("Mathematics", 14, "Application of derivatives", 4, 240),
        chapter("Mathematics", 15, "Integrals", 5, 330),
        chapter("Mathematics", 16, "Application of integrals", 3, 180),
        chapter("Mathematics", 17, "Differential equations", 3, 180),
        chapter("Mathematics", 18, "Matrices", 3, 180),
        chapter("Mathematics", 19, "Determinants", 3, 180),
        chapter("Mathematics", 20, "Vector algebra", 4, 210),
        chapter("Mathematics", 21, "Three dimensional geometry", 4, 240),
        chapter("Mathematics", 22, "Statistics", 2, 120),
        chapter("Mathematics", 23, "Probability", 4, 240),
        chapter("Mathematics", 24, "Mathematical reasoning", 2, 120)
    )

    private fun chapter(
        subject: String,
        number: Int,
        name: String,
        difficulty: Int,
        estimatedMinutes: Int
    ) = JeeChapter(
        id = subject.lowercase() + "_" + number,
        subject = subject,
        number = number,
        name = name,
        difficulty = difficulty,
        estimatedMinutes = estimatedMinutes
    )

    fun forSubject(subject: String): List<JeeChapter> =
        chapters.filter { it.subject == subject }

    fun find(chapterId: String): JeeChapter? =
        chapters.firstOrNull { it.id == chapterId }
}


enum class ActivityType {
    LEARNING,
    PRACTICE,
    REVISION,
    TEST,
    OTHER
}

data class RevisionRecord(
    val id: String,
    val chapterId: String,
    val reviewedAt: Long,
    val durationMin: Int,
    val confidence: Int,
    val result: RevisionRating,
    val nextReviewAt: Long
)
