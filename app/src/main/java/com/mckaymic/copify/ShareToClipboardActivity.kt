package com.mckaymic.copify

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

/**
 * Receives shared images or text and puts them on the system clipboard, then closes.
 *
 * Shared image URIs only grant us temporary read access, so each image is copied into our
 * own cache and re-exposed through a FileProvider. The clipboard service grants the pasting
 * app read access to that URI when it reads the clip.
 */
class ShareToClipboardActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            handle(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy shared content", e)
            toast(getString(R.string.copy_failed))
        }
        finish()
    }

    private fun handle(intent: Intent) {
        val type = intent.type.orEmpty()
        when {
            intent.action == Intent.ACTION_SEND && type.startsWith("image/") -> {
                val uri = intent.streamUri() ?: return toast(getString(R.string.copy_failed))
                copyImages(listOf(uri))
            }
            intent.action == Intent.ACTION_SEND_MULTIPLE && type.startsWith("image/") -> {
                val uris = intent.streamUris()
                if (uris.isEmpty()) return toast(getString(R.string.copy_failed))
                copyImages(uris)
            }
            intent.action == Intent.ACTION_SEND -> {
                val text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)
                    ?: return toast(getString(R.string.copy_failed))
                setClip(ClipData.newPlainText(getString(R.string.app_name), text))
                confirm(getString(R.string.copied_text))
            }
            else -> toast(getString(R.string.copy_failed))
        }
    }

    private fun copyImages(sources: List<Uri>) {
        val dir = File(cacheDir, "clips")
        // Only the latest clip needs to stay readable; drop older copies.
        dir.deleteRecursively()
        dir.mkdirs()

        val uris = sources.mapIndexed { i, src -> cacheCopy(src, dir, i) }
        val clip = ClipData.newUri(contentResolver, getString(R.string.app_name), uris.first())
        uris.drop(1).forEach { clip.addItem(ClipData.Item(it)) }
        setClip(clip)

        confirm(
            if (uris.size == 1) getString(R.string.copied_image)
            else getString(R.string.copied_images, uris.size)
        )
    }

    private fun cacheCopy(source: Uri, dir: File, index: Int): Uri {
        val mime = contentResolver.getType(source) ?: intent.type ?: "image/png"
        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "png"
        val file = File(dir, "clip_${System.currentTimeMillis()}_$index.$ext")
        contentResolver.openInputStream(source).use { input ->
            requireNotNull(input) { "Could not open $source" }
            file.outputStream().use { input.copyTo(it) }
        }
        return FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
    }

    private fun setClip(clip: ClipData) {
        getSystemService(ClipboardManager::class.java).setPrimaryClip(clip)
    }

    /** Android 13+ shows its own "Copied" confirmation, so only toast on older versions. */
    private fun confirm(message: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) toast(message)
    }

    private fun toast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    @Suppress("DEPRECATION")
    private fun Intent.streamUri(): Uri? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        else getParcelableExtra(Intent.EXTRA_STREAM)

    @Suppress("DEPRECATION")
    private fun Intent.streamUris(): List<Uri> =
        (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
        else getParcelableArrayListExtra(Intent.EXTRA_STREAM)).orEmpty()

    private companion object {
        const val TAG = "Copify"
    }
}
