package com.appsonair.appsync.services

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.view.View
import android.widget.ImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Loads a remote image into [bmImage], downsampled to the view's size.
 *
 * Call [cancel] when the host screen is destroyed so the view isn't touched afterwards.
 */
class DownloadImageTask(private val bmImage: ImageView) {

    private var job: Job? = null

    fun execute(url: String) {
        val (reqWidth, reqHeight) = targetSize()
        val client = httpClient(bmImage.context)
        job = CoroutineScope(Dispatchers.IO).launch {
            val bitmap = downloadImage(client, url, reqWidth, reqHeight)
            withContext(Dispatchers.Main) {
                // On failure keep the default drawable instead of clearing it.
                if (bitmap != null) bmImage.setImageBitmap(bitmap)
                bmImage.visibility = View.VISIBLE
            }
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
    }

    /** Target decode size in px, taken from the view's fixed layout size. */
    private fun targetSize(): Pair<Int, Int> {
        val params = bmImage.layoutParams
        val width = params?.width?.takeIf { it > 0 } ?: DEFAULT_TARGET_PX
        val height = params?.height?.takeIf { it > 0 } ?: DEFAULT_TARGET_PX
        return width to height
    }

    private fun downloadImage(
        client: OkHttpClient,
        url: String,
        reqWidth: Int,
        reqHeight: Int,
    ): Bitmap? {
        return try {
            val bytes = fetchBytes(client, url) ?: return null

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            val options = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(
                    bounds.outWidth, bounds.outHeight, reqWidth, reqHeight
                )
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        } catch (e: OutOfMemoryError) {
            Log.w(TAG, "Not enough memory to decode maintenance image", e)
            null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load maintenance image", e)
            null
        }
    }

    /** Downloads the body, returning null on HTTP errors or if it exceeds [MAX_IMAGE_BYTES]. */
    private fun fetchBytes(client: OkHttpClient, url: String): ByteArray? {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body ?: return null
            if (body.contentLength() > MAX_IMAGE_BYTES) return null

            body.byteStream().use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8 * 1024)
                var total = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    total += read
                    if (total > MAX_IMAGE_BYTES) return null
                    output.write(buffer, 0, read)
                }
                return output.toByteArray()
            }
        }
    }

    companion object {
        private const val TAG = "DownloadImageTask"
        private const val DEFAULT_TARGET_PX = 512
        private const val MAX_IMAGE_BYTES = 10L * 1024 * 1024
        private const val CACHE_BYTES = 5L * 1024 * 1024

        @Volatile
        private var client: OkHttpClient? = null

        private fun httpClient(context: Context): OkHttpClient =
            client ?: synchronized(this) {
                client ?: OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .cache(
                        Cache(
                            File(context.applicationContext.cacheDir, "appsync_images"),
                            CACHE_BYTES
                        )
                    )
                    .build()
                    .also { client = it }
            }

        internal fun calculateInSampleSize(
            width: Int,
            height: Int,
            reqWidth: Int,
            reqHeight: Int,
        ): Int {
            var inSampleSize = 1
            if (height > reqHeight || width > reqWidth) {
                val halfHeight = height / 2
                val halfWidth = width / 2
                while (halfHeight / inSampleSize >= reqHeight &&
                    halfWidth / inSampleSize >= reqWidth
                ) {
                    inSampleSize *= 2
                }
            }
            return inSampleSize
        }
    }
}
