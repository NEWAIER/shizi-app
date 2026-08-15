package com.family.shizi.ui.audio

/**
 * 界面反馈音频（audio/ui/）固定文件名。
 * 文件名必须与 tools/audio-generator/ui_feedback.csv 及内容包 uiAudio 列表一致，
 * 由 G2 资源校验保证存在性与格式。
 */
object UiFeedbackAudio {
    const val FOUND_IT = "audio/ui/found_it.mp3"
    const val TRY_AGAIN = "audio/ui/try_again.mp3"
    const val LETS_LOOK_AGAIN = "audio/ui/lets_look_again.mp3"
    const val GREAT = "audio/ui/great.mp3"
    const val CHALLENGE_COMPLETE = "audio/ui/challenge_complete.mp3"
    const val BADGE_UNLOCK = "audio/ui/badge_unlock.mp3"
    const val LEVEL_UP = "audio/ui/level_up.mp3"
}
