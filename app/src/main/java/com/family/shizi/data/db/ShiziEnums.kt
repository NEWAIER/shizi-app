package com.family.shizi.data.db

enum class LearningState { UNLEARNED, FIRST_LEARNING, REVIEWING, TEMP_MASTERED, STABLE_MASTERED }

enum class InitialTeachingStep {
    NOT_STARTED,
    A_CONTEXT,
    B_SOUND_MEANING,
    C_WORD_SENTENCE,
    PRACTICE,
    DONE,
}

enum class ReviewStage { NONE, D1, D3, D7, D14, D30, D60 }

enum class DelayedStatus { NOT_DUE, PENDING, PASS, FAIL }

enum class OralStatus { NOT_TESTED, INDEPENDENT_PASS, PROMPTED, FAIL }

enum class SessionStatus { CREATED, ACTIVE, PAUSED, COMPLETED, ENDED_EARLY, ERROR }

enum class PauseReason { USER_REST, COLD_START_CLEANUP }

enum class EarlyEndReason { FATIGUE, TIME_LIMIT, DAY_ROLLOVER }

enum class ItemKind { NEW, REVIEW, TEST }

enum class ItemStatus { PENDING, ACTIVE, COMPLETED, SKIPPED }

enum class QuestionStatus { PENDING, ACTIVE, COMPLETED }

enum class QuestionPurpose { INITIAL, REVIEW, EVIDENCE }

enum class FinalOutcome { CORRECT, TAUGHT_AFTER_ERROR, ABANDONED }

enum class HintLevel { NONE, LIGHT, STRONG }
