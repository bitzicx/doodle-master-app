package com.bitzicx.doodlemaster.update

import com.bitzicx.doodlemaster.ReleaseResponse
import com.bitzicx.doodlemaster.UpdateInfo
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

class UpdateRepository @Inject constructor(
    private val client: OkHttpClient
) {
    suspend fun checkForUpdate(currentVersionCode: Int): UpdateInfo? =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("https://api.github.com/repos/bitzicx/doodle-master-app/releases/latest")
                    .header("Accept", "application/vnd.github+json")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    val json = response.body.string()
                    val release = Gson().fromJson(json, ReleaseResponse::class.java)

                    val latestCode = tagToCode(release.tagName)
                    val apk = release.assets.firstOrNull { it.name.endsWith(".apk") }

                    if (latestCode > currentVersionCode && apk != null) {
                        UpdateInfo(
                            versionName = release.tagName.removePrefix("v"),
                            notes = release.body ?: "",
                            downloadUrl = apk.downloadUrl
                        )
                    } else null
                }
            } catch (e: Exception) {
                null   //  skip
            }
        }

    private fun tagToCode(tag: String): Int {
        val parts = tag.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
        val major = parts.getOrElse(0) { 0 }
        val minor = parts.getOrElse(1) { 0 }
        val patch = parts.getOrElse(2) { 0 }
        return major * 10000 + minor * 100 + patch
    }
}