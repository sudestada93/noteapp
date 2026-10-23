package com.libreta.app.util

import android.text.Html
import com.libreta.app.data.Repository
import com.libreta.app.model.Block
import com.libreta.app.model.BlockType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/** "Recortador web": guarda una página compartida desde el navegador como nota legible. */
object Clipper {
    data class Clip(val title: String, val blocks: List<Block>, val url: String)

    private val urlRe = Regex("https?://[^\\s]+")

    fun findUrl(text: String): String? = urlRe.find(text)?.value

    private fun fetch(url: String, maxBytes: Int = 2_000_000): ByteArray? {
        val c = URL(url).openConnection() as HttpURLConnection
        return try {
            c.connectTimeout = 10_000
            c.readTimeout = 15_000
            c.instanceFollowRedirects = true
            c.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124 Mobile Safari/537.36")
            c.setRequestProperty("Accept-Language", "es,en;q=0.8")
            if (c.responseCode !in 200..299) return null
            c.inputStream.use { input ->
                val out = java.io.ByteArrayOutputStream()
                val buf = ByteArray(8192)
                var total = 0
                while (true) {
                    val r = input.read(buf)
                    if (r < 0 || total > maxBytes) break
                    out.write(buf, 0, r)
                    total += r
                }
                out.toByteArray()
            }
        } catch (e: Exception) {
            null
        } finally {
            c.disconnect()
        }
    }

    private fun clean(html: String): String =
        Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString().replace('￼', ' ').replace(Regex("\\s+"), " ").trim()

    private fun meta(html: String, prop: String): String? {
        val re1 = Regex("<meta[^>]+(?:property|name)=[\"']${Regex.escape(prop)}[\"'][^>]*content=[\"']([^\"']*)[\"']", RegexOption.IGNORE_CASE)
        val re2 = Regex("<meta[^>]+content=[\"']([^\"']*)[\"'][^>]*(?:property|name)=[\"']${Regex.escape(prop)}[\"']", RegexOption.IGNORE_CASE)
        return (re1.find(html) ?: re2.find(html))?.groupValues?.get(1)?.let { clean(it) }?.ifBlank { null }
    }

    suspend fun clip(repo: Repository, sharedText: String, subject: String?): Clip = withContext(Dispatchers.IO) {
        val url = findUrl(sharedText) ?: return@withContext Clip(subject ?: "Nota compartida", listOf(Block(text = sharedText)), "")
        val bytes = fetch(url)
        if (bytes == null) {
            return@withContext Clip(subject ?: url, listOf(Block(type = BlockType.CALLOUT, text = "Enlace guardado: [$url]($url)"), Block(text = sharedText.replace(url, "").trim())), url)
        }
        val html = String(bytes, Charsets.UTF_8)
        val title = meta(html, "og:title")
            ?: Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)).find(html)?.groupValues?.get(1)?.let { clean(it) }
            ?: subject ?: url
        val desc = meta(html, "og:description") ?: meta(html, "description")
        val image = meta(html, "og:image")
        val body = html
            .replace(Regex("<script.*?</script>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
            .replace(Regex("<style.*?</style>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
        val blocks = mutableListOf<Block>()
        blocks += Block(type = BlockType.CALLOUT, text = "Recortado de [${URL(url).host}]($url) · ${Fmt.full(System.currentTimeMillis())}", color = 6)
        if (image != null && image.startsWith("http")) {
            fetch(image, 6_000_000)?.let { img ->
                val ext = when { image.contains(".png", true) -> "png"; image.contains(".webp", true) -> "webp"; else -> "jpg" }
                val (path, file) = repo.newAttachmentFile(ext)
                file.writeBytes(img)
                blocks += Block(type = BlockType.IMAGE, path = path, name = "portada.$ext", mime = "image/$ext", size = img.size.toLong())
            }
        }
        if (desc != null) blocks += Block(type = BlockType.QUOTE, text = desc)
        val tagRe = Regex("<(h[1-3]|p|li)[^>]*>(.*?)</\\1>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        var count = 0
        for (m in tagRe.findAll(body)) {
            val tag = m.groupValues[1].lowercase()
            val text = clean(m.groupValues[2])
            if (text.length < (if (tag == "p") 40 else if (tag == "li") 25 else 3)) continue
            blocks += when (tag) {
                "h1", "h2" -> Block(type = BlockType.H2, text = text)
                "h3" -> Block(type = BlockType.H3, text = text)
                "li" -> Block(type = BlockType.BULLET, text = text)
                else -> Block(text = text)
            }
            if (++count > 80) break
        }
        Clip(title, blocks, url)
    }
}
