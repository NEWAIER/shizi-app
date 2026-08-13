package com.family.shizi.ui.audio

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.family.shizi.data.content.ContentRepository
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * 本地教学音频播放器。
 *
 * Mate 30 的系统 MediaPlayer 在连续播放短 MP3 时会进入原生不可中断等待，
 * 因此这里使用单实例 Media3 ExoPlayer 播放 asset 播放列表。新播放会原子替换旧列表，
 * 播放令牌阻止旧回调继续序列；页面停止时释放底层播放器，下次播放再按需创建。
 */
class AssetAudioPlayer(
    context: Context,
    private val onError: (AudioPlayerError) -> Unit = {},
    private val assetRootProvider: () -> String = { ContentRepository.get(context).active().descriptor.assetRoot },
) {
    private val appContext = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private val tokenCounter = AtomicLong(0L)
    private val permanentlyReleased = AtomicBoolean(false)
    @Volatile private var activeToken = 0L
    @Volatile private var activeAssets: List<String> = emptyList()
    @Volatile private var completion: (() -> Unit)? = null
    @Volatile private var player: ExoPlayer? = null
    @Volatile private var attached: Lifecycle? = null

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState != Player.STATE_ENDED) return
            val token = activeToken
            val callback = completion ?: return
            completion = null
            handler.post {
                if (tokenMatches(token)) callback()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            val token = activeToken
            if (!tokenMatches(token)) return
            val current = player
            val index = current?.currentMediaItemIndex ?: C.INDEX_UNSET
            val asset = activeAssets.getOrNull(index) ?: activeAssets.firstOrNull().orEmpty()
            completion = null
            tokenCounter.incrementAndGet()
            activeToken = 0L
            runOnMain {
                runCatching { current?.stop() }
                runCatching { current?.clearMediaItems() }
            }
            onError(
                AudioPlayerError(
                    assetPath = asset,
                    message = "EXOPLAYER_${error.errorCodeName}",
                    throwableClass = error.javaClass.name,
                ),
            )
        }
    }

    private val lifecycleObserver = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_STOP -> stop()
            Lifecycle.Event.ON_DESTROY -> release()
            else -> Unit
        }
    }

    fun attachLifecycle(owner: LifecycleOwner) {
        val lifecycle = owner.lifecycle
        if (attached === lifecycle) return
        runCatching { attached?.removeObserver(lifecycleObserver) }
        attached = lifecycle
        lifecycle.addObserver(lifecycleObserver)
    }

    fun play(assetPath: String, onComplete: () -> Unit = {}): Boolean =
        playSequence(listOf(assetPath), onComplete)

    fun playSequence(assetPaths: List<String>, onComplete: () -> Unit = {}): Boolean {
        if (permanentlyReleased.get()) return false
        if (assetPaths.isEmpty()) {
            handler.post(onComplete)
            return true
        }

        val invalidType = assetPaths.firstOrNull { !it.endsWith(".mp3", ignoreCase = true) }
        if (invalidType != null) {
            onError(
                AudioPlayerError(
                    assetPath = invalidType,
                    message = "AUDIO_PATH_INVALID",
                    throwableClass = IllegalArgumentException::class.java.name,
                ),
            )
            return false
        }

        val failedAsset = assetPaths.firstOrNull { !assetCanBeOpened(it) }
        if (failedAsset != null) {
            onError(
                AudioPlayerError(
                    assetPath = failedAsset,
                    message = "OPENFD_OR_CONSTRUCT_EXCEPTION",
                    throwableClass = java.io.FileNotFoundException::class.java.name,
                ),
            )
            return false
        }

        val token = tokenCounter.incrementAndGet()
        activeToken = token
        activeAssets = assetPaths.toList()
        completion = onComplete
        runOnMain {
            if (!tokenMatches(token) || permanentlyReleased.get()) return@runOnMain
            val exoPlayer = ensurePlayer()
            runCatching {
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                exoPlayer.setMediaItems(
                    assetPaths.map { MediaItem.fromUri("asset:///${assetRootProvider().trimEnd('/')}/$it") },
                )
                exoPlayer.prepare()
                exoPlayer.play()
            }.onFailure { throwable ->
                handleSynchronousError(token, assetPaths.first(), throwable)
            }
        }
        return true
    }

    /** 停止、取消回调并释放本次底层播放器；之后仍可再次播放。 */
    fun stop() {
        tokenCounter.incrementAndGet()
        activeToken = 0L
        activeAssets = emptyList()
        completion = null
        handler.removeCallbacksAndMessages(null)
        releasePlayer()
    }

    /** 永久释放；重复调用安全。 */
    fun release() {
        if (!permanentlyReleased.compareAndSet(false, true)) return
        stop()
        runCatching { attached?.removeObserver(lifecycleObserver) }
        attached = null
    }

    private fun ensurePlayer(): ExoPlayer {
        player?.let { return it }
        return ExoPlayer.Builder(appContext).build().also { created ->
            created.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .build(),
                true,
            )
            created.setHandleAudioBecomingNoisy(true)
            created.addListener(listener)
            player = created
        }
    }

    private fun releasePlayer() {
        val current = player
        player = null
        if (current != null) {
            runOnMain {
                runCatching { current.removeListener(listener) }
                runCatching { current.stop() }
                runCatching { current.clearMediaItems() }
                runCatching { current.release() }
            }
        }
    }

    private fun assetCanBeOpened(assetPath: String): Boolean =
        runCatching {
            appContext.assets.openFd("${assetRootProvider().trimEnd('/')}/$assetPath").use { descriptor ->
                descriptor.length >= 0L
            }
        }.getOrDefault(false)

    private fun handleSynchronousError(token: Long, asset: String, throwable: Throwable) {
        if (!tokenMatches(token)) return
        completion = null
        tokenCounter.incrementAndGet()
        activeToken = 0L
        onError(
            AudioPlayerError(
                assetPath = asset,
                message = "EXOPLAYER_SETUP_EXCEPTION",
                throwableClass = throwable.javaClass.name,
            ),
        )
    }

    private fun tokenMatches(token: Long): Boolean =
        token > 0L && token == activeToken && token == tokenCounter.get()

    private fun runOnMain(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) action() else handler.post(action)
    }
}

data class AudioPlayerError(
    val assetPath: String,
    val message: String,
    val throwableClass: String? = null,
)
