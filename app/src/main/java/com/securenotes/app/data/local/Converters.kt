package com.securenotes.app.data.local

import androidx.room.TypeConverter
import com.securenotes.app.domain.model.LinkPreview
import org.json.JSONArray
import org.json.JSONObject

/**
 * Small hand-rolled JSON converters. Avoiding a reflection-based serializer keeps
 * the APK lean and dodges any R8 keep-rule surprises.
 */
class Converters {

    @TypeConverter
    fun stringListToJson(value: List<String>): String {
        val array = JSONArray()
        value.forEach { array.put(it) }
        return array.toString()
    }

    @TypeConverter
    fun jsonToStringList(value: String): List<String> = runCatching {
        val array = JSONArray(value)
        (0 until array.length()).map { array.getString(it) }
    }.getOrDefault(emptyList())

    @TypeConverter
    fun linkPreviewsToJson(value: List<LinkPreview>): String {
        val array = JSONArray()
        value.forEach { preview ->
            array.put(
                JSONObject().apply {
                    put("url", preview.url)
                    put("title", preview.title ?: JSONObject.NULL)
                    put("description", preview.description ?: JSONObject.NULL)
                    put("faviconUrl", preview.faviconUrl ?: JSONObject.NULL)
                    put("imageUrl", preview.imageUrl ?: JSONObject.NULL)
                    put("domain", preview.domain)
                    put("fetchedAt", preview.fetchedAt)
                    put("hidden", preview.hidden)
                },
            )
        }
        return array.toString()
    }

    @TypeConverter
    fun jsonToLinkPreviews(value: String): List<LinkPreview> = runCatching {
        val array = JSONArray(value)
        (0 until array.length()).map { index ->
            val obj = array.getJSONObject(index)
            LinkPreview(
                url = obj.getString("url"),
                title = obj.optStringOrNull("title"),
                description = obj.optStringOrNull("description"),
                faviconUrl = obj.optStringOrNull("faviconUrl"),
                imageUrl = obj.optStringOrNull("imageUrl"),
                domain = obj.optString("domain", ""),
                fetchedAt = obj.optLong("fetchedAt", 0L),
                hidden = obj.optBoolean("hidden", false),
            )
        }
    }.getOrDefault(emptyList())

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotEmpty() }
}
