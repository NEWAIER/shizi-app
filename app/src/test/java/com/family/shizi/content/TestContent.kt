package com.family.shizi.content

import com.family.shizi.data.content.ContentLoader
import com.family.shizi.data.content.ContentPackage
import java.io.File

internal object TestContent {
    val contentFile = File("src/main/assets/content/v1/content.json")
    val schemaFile = File("src/main/assets/content/v1/content.schema.json")
    fun json(): String = contentFile.readText(Charsets.UTF_8)
    fun packageData(): ContentPackage = ContentLoader.decode(json())

    fun ContentPackage.replaceQuestion(
        characterIndex: Int = 0,
        questionIndex: Int = 0,
        transform: (com.family.shizi.data.content.QuestionSeed) -> com.family.shizi.data.content.QuestionSeed,
    ): ContentPackage {
        val updatedCharacters = characters.toMutableList()
        val character = updatedCharacters[characterIndex]
        val questions = character.questionSeeds.toMutableList()
        questions[questionIndex] = transform(questions[questionIndex])
        updatedCharacters[characterIndex] = character.copy(questionSeeds = questions)
        return copy(characters = updatedCharacters)
    }
}
