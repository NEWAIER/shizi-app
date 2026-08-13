package com.family.shizi.domain.core

import java.time.ZoneId
import java.util.UUID
import kotlin.random.Random

fun interface ZoneIdProvider {
    fun zoneId(): ZoneId
}

interface RandomProvider {
    fun <T> shuffled(values: List<T>): List<T>
}

fun interface IdProvider {
    fun newId(): String
}

object SystemZoneIdProvider : ZoneIdProvider {
    override fun zoneId(): ZoneId = ZoneId.systemDefault()
}

class KotlinRandomProvider(private val random: Random = Random.Default) : RandomProvider {
    override fun <T> shuffled(values: List<T>): List<T> = values.shuffled(random)
}

object UuidProvider : IdProvider {
    override fun newId(): String = UUID.randomUUID().toString()
}
