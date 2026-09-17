package com.securenotes.app.data.link

import com.securenotes.app.data.local.dao.LinkPreviewDao
import com.securenotes.app.data.local.entity.LinkPreviewEntity
import com.securenotes.app.domain.model.LinkPreview
import com.securenotes.app.domain.repository.LinkPreviewRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.URI
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fetches OpenGraph/Twitter-card metadata for a URL and caches it in the
 * encrypted database so a note can render its previews entirely offline.
 */
@Singleton
class LinkPreviewRepositoryImpl @Inject constructor(
    private val dao: LinkPreviewDao,
    private val ioDispatcher: CoroutineDispatcher,
) : LinkPreviewRepository {

    override suspend fun fetch(url: String, forceRefresh: Boolean): Result<LinkPreview> =
        withContext(ioDispatcher) {
            val normalized = normalize(url) ?: return@withContext Result.failure(
                IllegalArgumentException("Not a valid http(s) URL"),
            )

            if (!forceRefresh) {
                dao.get(normalized)?.let { cached ->
                    val fresh = System.currentTimeMillis() - cached.fetchedAt < CACHE_TTL_MS
                    if (fresh) return@withContext Result.success(cached.toDomain())
                }
            }

            runCatching {
                val document = Jsoup.connect(normalized)
                    .userAgent(USER_AGENT)
                    .timeout(TimeUnit.SECONDS.toMillis(12).toInt())
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .get()

                val host = URI(normalized).host.orEmpty().removePrefix("www.")

                val title = document.metaContent("og:title")
                    ?: document.metaContent("twitter:title")
                    ?: document.title().takeIf { it.isNotBlank() }
                    ?: host

                val description = document.metaContent("og:description")
                    ?: document.metaContent("twitter:description")
                    ?: document.metaContent("description")

                val image = document.metaContent("og:image")
                    ?: document.metaContent("twitter:image")

                LinkPreview(
                    url = normalized,
                    title = title.trim().take(160),
                    description = description?.trim()?.take(240),
                    faviconUrl = "https://www.google.com/s2/favicons?sz=64&domain=$host",
                    imageUrl = image?.let { absolute(it, normalized) },
                    domain = host,
                    fetchedAt = System.currentTimeMillis(),
                )
            }.onSuccess { preview ->
                dao.upsert(LinkPreviewEntity.fromDomain(preview))
            }.recover { _ ->
                // Offline or blocked: fall back to a domain-only card so the user
                // still gets a tappable, well-formed preview.
                val host = runCatching { URI(normalized).host.orEmpty().removePrefix("www.") }
                    .getOrDefault("")
                dao.get(normalized)?.toDomain() ?: LinkPreview(
                    url = normalized,
                    title = host.ifBlank { normalized },
                    domain = host,
                    fetchedAt = System.currentTimeMillis(),
                )
            }
        }

    private fun org.jsoup.nodes.Document.metaContent(key: String): String? =
        selectFirst("meta[property=$key]")?.attr("content")?.takeIf { it.isNotBlank() }
            ?: selectFirst("meta[name=$key]")?.attr("content")?.takeIf { it.isNotBlank() }

    private fun absolute(candidate: String, base: String): String = runCatching {
        URI(base).resolve(candidate).toString()
    }.getOrDefault(candidate)

    private fun normalize(raw: String): String? {
        val trimmed = raw.trim()
        val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }
        return runCatching {
            val uri = URI(withScheme)
            if (uri.host.isNullOrBlank()) null else withScheme
        }.getOrNull()
    }

    private companion object {
        const val CACHE_TTL_MS = 7L * 24 * 60 * 60 * 1000
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124 Mobile Safari/537.36"
    }
}

/** Utility for finding URLs inside note text. */
object LinkDetector {
    private val REGEX = Regex("""https?://[^\s<>"')\]]+""", RegexOption.IGNORE_CASE)

    fun findUrls(text: String): List<String> =
        REGEX.findAll(text)
            .map { it.value.trimEnd('.', ',', ';', ':', '!', '?') }
            .distinct()
            .toList()
}
