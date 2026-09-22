package com.example.jeecommandcenter.data

enum class TestMode {
    PRACTICE,
    MOCK
}

data class Question(
    val id: String,
    val subject: String,
    val chapterId: String,
    val chapter: String,
    val topic: String,
    val prompt: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val difficulty: Int = 3,
    val source: String = "JeE starter bank",
    val year: Int? = null,
    val marks: Int = 4,
    val negativeMarks: Float = 1f
)

data class QuestionAttemptRecord(
    val id: Long,
    val testId: String,
    val questionId: String,
    val selectedIndex: Int,
    val correct: Boolean,
    val responseTimeSec: Int,
    val subject: String,
    val chapterId: String,
    val mistakeType: MistakeType? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class MistakeType {
    CONCEPT,
    FORMULA,
    CALCULATION,
    SILLY,
    MISREAD,
    TIME_PRESSURE,
    GUESS,
    METHOD,
    UNCLASSIFIED
}

data class MistakeRecord(
    val id: String,
    val questionId: String,
    val subject: String,
    val chapterId: String,
    val questionPrompt: String,
    val correctAnswer: String,
    val lastSelectedAnswer: String,
    val count: Int,
    val mistakeType: MistakeType,
    val source: String = "assessment",
    val correction: String = "",
    val resolved: Boolean = false,
    val lastSeenAt: Long = System.currentTimeMillis()
)

data class TestAttemptRecord(
    val id: String,
    val mode: TestMode,
    val startedAt: Long,
    val completedAt: Long,
    val questionCount: Int,
    val correctCount: Int,
    val score: Int,
    val totalMarks: Int,
    val durationSec: Int,
    val subjectBreakdown: Map<String, Pair<Int, Int>>,
    val name: String = "",
    val attemptedCount: Int = questionCount,
    val incorrectCount: Int = (questionCount - correctCount).coerceAtLeast(0),
    val skippedCount: Int = 0
)

data class SubjectAnalytics(
    val subject: String,
    val attempted: Int,
    val correct: Int,
    val accuracy: Float
)

data class AnalyticsSnapshot(
    val studyMinutes7d: Int,
    val totalQuestionsAttempted: Int,
    val correctAnswers: Int,
    val accuracy: Float,
    val testsCompleted: Int,
    val averageTestScore: Float,
    val unresolvedMistakes: Int,
    val repeatedMistakes: Int,
    val masteredChapters: Int,
    val activeRevisionItems: Int,
    val subjectAnalytics: List<SubjectAnalytics>
)

data class MistakeStats(
    val type: MistakeType,
    val count: Int
)

object StarterQuestionBank {
    val fallback: List<Question> = listOf(
        Question(
            "phy_units_001", "Physics", "Physics_1", "Units and measurements",
            "SI units", "Which SI unit is used for force?",
            listOf("Joule", "Newton", "Pascal", "Watt"), 1,
            "Force is measured in newtons (N), which is kg·m/s².", 1
        ),
        Question(
            "phy_motion_001", "Physics", "Physics_2", "Motion in a straight line",
            "Equations of motion", "A body starts from rest and accelerates uniformly at 2 m/s². What is its speed after 5 s?",
            listOf("5 m/s", "7 m/s", "10 m/s", "12 m/s"), 2,
            "Use v = u + at = 0 + 2×5 = 10 m/s.", 2
        ),
        Question(
            "phy_current_001", "Physics", "Physics_18", "Current electricity",
            "Resistance", "Two resistors of 2 Ω and 3 Ω are connected in series. What is their equivalent resistance?",
            listOf("1 Ω", "5 Ω", "6 Ω", "2.5 Ω"), 1,
            "Series resistances add: R = 2 + 3 = 5 Ω.", 2
        ),
        Question(
            "chem_atom_001", "Chemistry", "Chemistry_2", "Structure of atom",
            "Atomic number", "The atomic number of an element equals the number of:",
            listOf("Neutrons", "Protons", "Nucleons", "Protons + neutrons"), 1,
            "Atomic number Z is defined as the number of protons in the nucleus.", 1
        ),
        Question(
            "chem_basic_001", "Chemistry", "Chemistry_1", "Some basic concepts of chemistry",
            "Moles", "How many particles are present in one mole of a substance?",
            listOf("6.022×10^23", "3.011×10^23", "9.81×10^2", "1.602×10^-19"), 0,
            "One mole contains Avogadro's number, approximately 6.022×10^23 entities.", 1
        ),
        Question(
            "chem_redox_001", "Chemistry", "Chemistry_7", "Redox reactions",
            "Oxidation", "Oxidation is best described as:",
            listOf("Gain of electrons", "Loss of electrons", "Gain of neutrons", "Loss of protons"), 1,
            "Oxidation corresponds to loss of electrons; reduction is gain of electrons.", 2
        ),
        Question(
            "math_sets_001", "Mathematics", "Mathematics_1", "Sets",
            "Set cardinality", "If A = {1, 2, 3, 4}, what is n(A)?",
            listOf("2", "3", "4", "5"), 2,
            "The set contains four distinct elements, so its cardinality is 4.", 1
        ),
        Question(
            "math_quad_001", "Mathematics", "Mathematics_5", "Quadratic equations",
            "Roots", "For x² - 5x + 6 = 0, the roots are:",
            listOf("1 and 6", "2 and 3", "-2 and -3", "0 and 6"), 1,
            "Factor x² - 5x + 6 = (x - 2)(x - 3).", 2
        ),
        Question(
            "math_derivative_001", "Mathematics", "Mathematics_13", "Continuity and differentiability",
            "Differentiation", "What is d(x²)/dx?",
            listOf("x", "2x", "x²/2", "2"), 1,
            "By the power rule, d(x²)/dx = 2x.", 1
        ),
        Question(
            "math_det_001", "Mathematics", "Mathematics_19", "Determinants",
            "2×2 determinant", "What is the determinant of [[1, 2], [3, 4]]?",
            listOf("-2", "2", "10", "-10"), 0,
            "For [[a,b],[c,d]], determinant is ad - bc = 1×4 - 2×3 = -2.", 2
        ),
        Question(
            "phy_energy_001", "Physics", "Physics_5", "Work, energy and power",
            "Kinetic energy", "The kinetic energy of a body of mass m moving with speed v is:",
            listOf("mv", "mv²", "1/2 mv²", "m/v²"), 2,
            "Kinetic energy is K = ½mv².", 1
        ),
        Question(
            "chem_bond_001", "Chemistry", "Chemistry_4", "Chemical bonding",
            "Bonding", "A covalent bond is formed primarily by:",
            listOf("Transfer of protons", "Sharing of electrons", "Transfer of neutrons", "Sharing of nuclei"), 1,
            "Covalent bonding results from sharing electron pairs between atoms.", 2
        )
    )
}
