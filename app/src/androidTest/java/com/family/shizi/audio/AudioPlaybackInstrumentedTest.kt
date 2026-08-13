package com.family.shizi.audio

import android.content.Context
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.family.shizi.data.content.ContentLoader
import com.family.shizi.data.content.ContentPackage
import com.family.shizi.data.content.OptionKind
import com.family.shizi.ui.audio.AssetAudioPlayer
import com.family.shizi.ui.audio.AudioPlayerError
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AudioPlaybackInstrumentedTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = instrumentation.targetContext
    private lateinit var content: ContentPackage

    @Before fun loadContent() {
        content = ContentLoader.load(context)
    }

    /** 1. 38 个 MP3 逐个自然播放完成。 */
    @Test
    fun allThirtyEightMp3AssetsReachNaturalCompletion() {
        val assets = allMp3Assets(content)
        assertEquals("content/manifest should expose exactly 38 MP3 assets", 38, assets.size)

        assets.forEachIndexed { index, asset ->
            val errors = CopyOnWriteArrayList<AudioPlayerError>()
            val completed = CountDownLatch(1)
            var player: AssetAudioPlayer? = null
            instrumentation.runOnMainSync {
                player = AssetAudioPlayer(context) {
                    errors += it
                    completed.countDown()
                }.also { p ->
                    assertTrue("play() should start for #${index + 1}: $asset", p.play(asset) {
                        Log.i(TAG, "PASS natural-complete ${index + 1}/${assets.size} $asset")
                        completed.countDown()
                    })
                }
            }

            assertTrue("$asset timed out before natural completion", completed.await(15, TimeUnit.SECONDS))
            instrumentation.runOnMainSync { player?.stop() }
            assertTrue("$asset reported audio errors: $errors", errors.isEmpty())
        }
    }

    /** 2. A/B/C 连续播放：字音+字义+词语x3+句子 连续6段。 */
    @Test
    fun abcTeachingSequenceCompletes() {
        val first = content.characters.minBy { it.order }
        val sequence = listOf(first.audio.character, first.audio.meaning) +
            first.words.map { it.audioAsset } +
            listOf(first.sentence.audioAsset)
        val errors = CopyOnWriteArrayList<AudioPlayerError>()
        val completed = CountDownLatch(1)
        var player: AssetAudioPlayer? = null

        instrumentation.runOnMainSync {
            player = AssetAudioPlayer(context) { errors += it }.also { p ->
                assertTrue("A/B/C teaching sequence should start", p.playSequence(sequence) {
                    Log.i(TAG, "PASS abc-sequence ${first.id} ${sequence.joinToString(",")}")
                    completed.countDown()
                })
            }
        }

        assertTrue("A/B/C sequence timed out", completed.await(25, TimeUnit.SECONDS))
        instrumentation.runOnMainSync { player?.stop() }
        assertTrue("A/B/C sequence reported audio errors: $errors", errors.isEmpty())
    }

    /** 3. 快速重复播放8次：仅最新触发完成回调，其余7次被令牌取消。 */
    @Test
    fun rapidRepeatedPlayOnlyLatestCompletionRuns() {
        val asset = content.characters.first().audio.character
        val completions = AtomicInteger(0)
        val completed = CountDownLatch(1)
        val errors = CopyOnWriteArrayList<AudioPlayerError>()
        var player: AssetAudioPlayer? = null

        instrumentation.runOnMainSync {
            player = AssetAudioPlayer(context) { errors += it }.also { p ->
                repeat(8) { index ->
                    assertTrue("rapid play #$index should start", p.play(asset) {
                        val count = completions.incrementAndGet()
                        Log.i(TAG, "PASS rapid-complete count=$count asset=$asset")
                        completed.countDown()
                    })
                }
            }
        }

        assertTrue("latest rapid play timed out", completed.await(15, TimeUnit.SECONDS))
        Thread.sleep(500)
        instrumentation.runOnMainSync { player?.stop() }
        assertEquals("stale completion callbacks must be ignored", 1, completions.get())
        assertTrue("rapid play reported audio errors: $errors", errors.isEmpty())
    }

    /** 4. 播放中停止：无崩溃、无完成回调。 */
    @Test
    fun stopDuringPlaybackCancelsWithoutCrashOrCompletion() {
        val asset = content.characters.maxBy { it.meaningForChild.length }.audio.meaning
        val completions = AtomicInteger(0)
        val errors = CopyOnWriteArrayList<AudioPlayerError>()
        var player: AssetAudioPlayer? = null

        instrumentation.runOnMainSync {
            player = AssetAudioPlayer(context) { errors += it }.also { p ->
                assertTrue("play should start before stop", p.play(asset) {
                    completions.incrementAndGet()
                })
            }
        }
        Thread.sleep(120)
        instrumentation.runOnMainSync { player?.stop() }
        Thread.sleep(500)

        assertEquals("stop should cancel the active completion callback", 0, completions.get())
        assertTrue("stop path reported audio errors: $errors", errors.isEmpty())
        Log.i(TAG, "PASS stop-during-playback $asset")
    }

    /** 5. 缺失资源故障注入：onError报告细分错误码，不抛异常。 */
    @Test
    fun missingAssetFailsWithoutThrowingAndReportsError() {
        val errors = CopyOnWriteArrayList<AudioPlayerError>()
        var player: AssetAudioPlayer? = null

        instrumentation.runOnMainSync {
            player = AssetAudioPlayer(context) { errors += it }.also { p ->
                assertFalse(
                    "missing asset should fail gracefully",
                    p.play("audio/fault_injection/missing.mp3"),
                )
            }
        }
        Thread.sleep(500)
        instrumentation.runOnMainSync { player?.stop() }

        assertTrue("missing asset should report a diagnostic error, got=$errors", errors.isNotEmpty())
        val code = errors.first().message
        assertTrue("error code should be OPENFD_OR_CONSTRUCT_EXCEPTION, got=$code",
            code == "OPENFD_OR_CONSTRUCT_EXCEPTION")
        Log.i(TAG, "PASS fault-injection ${errors.first()}")
    }

    /** 6. 连续完成多字教学：人→口→大三字完整A/B/C序列。 */
    @Test
    fun multiCharacterAbcSequenceCompletes() {
        val chars = content.characters.sortedBy { it.order }.take(3)
        val combined = chars.flatMap { c ->
            listOf(c.audio.character, c.audio.meaning) +
                c.words.map { it.audioAsset } +
                listOf(c.sentence.audioAsset)
        }
        val errors = CopyOnWriteArrayList<AudioPlayerError>()
        val completed = CountDownLatch(1)
        var player: AssetAudioPlayer? = null

        instrumentation.runOnMainSync {
            player = AssetAudioPlayer(context) { errors += it }.also { p ->
                assertTrue("multi-char sequence should start", p.playSequence(combined) {
                    Log.i(TAG, "PASS multi-char-abc size=${combined.size}")
                    completed.countDown()
                })
            }
        }

        assertTrue("multi-char sequence timed out", completed.await(60, TimeUnit.SECONDS))
        instrumentation.runOnMainSync { player?.stop() }
        assertTrue("multi-char sequence reported audio errors: $errors", errors.isEmpty())
    }

    /** 7. 错误MP3 setDataSource失败（构造一个非MP3字节流注入）：MediaPlayer回调OnErrorListener。 */
    @Test
    fun corruptedAssetTriggersOnErrorListener() {
        val errors = CopyOnWriteArrayList<AudioPlayerError>()
        val completed = CountDownLatch(1)
        var player: AssetAudioPlayer? = null

        val webpAsset = content.characters.first().imageAsset
        instrumentation.runOnMainSync {
            player = AssetAudioPlayer(context) { err ->
                errors += err
                completed.countDown()
            }.also { p ->
                val started = p.play(webpAsset) {
                    Log.e(TAG, "UNEXPECTED completion for non-audio asset")
                    completed.countDown()
                }
                if (!started) completed.countDown()
            }
        }

        assertTrue("non-audio asset should fail or report error within timeout",
            completed.await(10, TimeUnit.SECONDS))
        instrumentation.runOnMainSync { player?.stop() }
        assertTrue("non-audio should either fail start or report MediaPlayer error; errors=$errors",
            errors.isNotEmpty())
        Log.i(TAG, "PASS corrupted-asset error=${errors.firstOrNull()?.message}")
    }

    /** 8. 两次 stop + 两次 release 不崩溃（幂等）。 */
    @Test
    fun repeatedStopAndReleaseAreIdempotent() {
        val errors = CopyOnWriteArrayList<AudioPlayerError>()
        val latch = CountDownLatch(1)
        var player: AssetAudioPlayer? = null
        instrumentation.runOnMainSync {
            player = AssetAudioPlayer(context) { errors += it }.also { p ->
                p.play(content.characters.first().audio.character) {
                    latch.countDown()
                }
            }
        }
        Thread.sleep(100)
        repeat(2) {
            instrumentation.runOnMainSync { player?.stop() }
            instrumentation.runOnMainSync { player?.release() }
        }
        Thread.sleep(200)
        assertTrue("repeated stop/release reported errors: $errors", errors.isEmpty())
        Log.i(TAG, "PASS idempotent-stop-release")
    }

    private fun allMp3Assets(content: ContentPackage): List<String> =
        buildSet {
            content.characters.forEach { character ->
                add(character.audio.character)
                add(character.audio.meaning)
                character.words.forEach { add(it.audioAsset) }
                add(character.sentence.audioAsset)
                character.questionSeeds.forEach { add(it.promptAudio) }
            }
            content.optionCatalog.forEach { option ->
                if (option.kind == OptionKind.AUDIO) option.asset?.let(::add)
            }
        }
            .filter { it.endsWith(".mp3") }
            .sorted()

    private companion object {
        const val TAG = "ShiziAudioTest"
    }
}
