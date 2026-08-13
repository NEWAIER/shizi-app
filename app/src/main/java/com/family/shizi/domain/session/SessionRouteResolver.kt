package com.family.shizi.domain.session

import com.family.shizi.data.db.ItemKind
import com.family.shizi.data.db.ItemStatus
import com.family.shizi.data.db.QuestionStatus
import com.family.shizi.data.db.ShiziDatabase
import com.family.shizi.navigation.ShiziRoute

class SessionRouteResolver(private val database: ShiziDatabase) {
    suspend fun nextRoute(sessionId: String): ShiziRoute {
        val session = database.learningSessionDao().getById(sessionId) ?: return ShiziRoute.Home
        val items = database.sessionItemDao().getForSession(session.id)
        val item = items.firstOrNull { it.status != ItemStatus.COMPLETED } ?: return ShiziRoute.Result
        val progress = database.characterProgressDao().getById(item.characterId)
        return if (item.kind == ItemKind.NEW && progress?.initialLessonCompleted != true) {
            ShiziRoute.Learn
        } else {
            val hasPendingQuestion = database.questionInstanceDao().getForItem(item.id)
                .any { it.status != QuestionStatus.COMPLETED }
            if (hasPendingQuestion) ShiziRoute.Practice else ShiziRoute.Result
        }
    }
}
