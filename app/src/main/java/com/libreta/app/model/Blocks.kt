package com.libreta.app.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
enum class BlockType {
    PARAGRAPH, H1, H2, H3, BULLET, NUMBERED, CHECK, TASK, QUOTE, CODE, CALLOUT,
    DIVIDER, IMAGE, FILE, AUDIO, TABLE, NOTE_LINK;

    val isText: Boolean
        get() = this in setOf(PARAGRAPH, H1, H2, H3, BULLET, NUMBERED, CHECK, TASK, QUOTE, CODE, CALLOUT)

    val isListLike: Boolean
        get() = this in setOf(BULLET, NUMBERED, CHECK, TASK)
}

@Serializable
data class Block(
    val id: String = newId(),
    val type: BlockType = BlockType.PARAGRAPH,
    val text: String = "",
    val checked: Boolean = false,
    val indent: Int = 0,
    val path: String? = null,
    val name: String? = null,
    val mime: String? = null,
    val size: Long = 0,
    val durationMs: Long = 0,
    val waveform: List<Int> = emptyList(),
    val rows: List<List<String>> = emptyList(),
    val linkId: String? = null,
    val color: Int = 0,
    val dueAt: Long? = null,
    val reminderAt: Long? = null,
    val flagged: Boolean = false,
    val sketch: Boolean = false,
)

fun newId(): String = UUID.randomUUID().toString()

object BlockCodec {
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        explicitNulls = false
    }

    fun encode(blocks: List<Block>): String = json.encodeToString(blocks)

    fun decode(content: String): List<Block> {
        if (content.isBlank()) return emptyList()
        return try {
            json.decodeFromString<List<Block>>(content)
        } catch (e: Exception) {
            content.split('\n').map { Block(text = it) }
        }
    }

    /** Texto plano para búsqueda, vista previa y exportación. */
    fun plainText(blocks: List<Block>): String = buildString {
        blocks.forEach { b ->
            when (b.type) {
                BlockType.TABLE -> b.rows.forEach { appendLine(it.joinToString("  ")) }
                BlockType.FILE, BlockType.AUDIO -> b.name?.let { appendLine(it) }
                BlockType.IMAGE -> if (b.text.isNotBlank()) appendLine(b.text)
                BlockType.NOTE_LINK -> b.name?.let { appendLine(it) }
                BlockType.DIVIDER -> {}
                else -> appendLine(Inline.strip(b.text))
            }
        }
    }.trim()

    fun preview(blocks: List<Block>, max: Int = 220): String {
        val sb = StringBuilder()
        for (b in blocks) {
            if (!b.type.isText && b.type != BlockType.TABLE) continue
            val t = if (b.type == BlockType.TABLE) b.rows.flatten().joinToString(" ") else Inline.strip(b.text)
            if (t.isBlank()) continue
            val prefix = when (b.type) {
                BlockType.CHECK, BlockType.TASK -> if (b.checked) "☑ " else "☐ "
                BlockType.BULLET -> "• "
                else -> ""
            }
            if (sb.isNotEmpty()) sb.append("  ")
            sb.append(prefix).append(t)
            if (sb.length > max) break
        }
        return sb.toString().take(max)
    }

    fun wordCount(text: String): Int = text.split(Regex("\\s+")).count { it.isNotBlank() }

    fun toMarkdown(title: String, blocks: List<Block>): String = buildString {
        if (title.isNotBlank()) appendLine("# $title").appendLine()
        var number = 0
        blocks.forEach { b ->
            val pad = "  ".repeat(b.indent)
            if (b.type != BlockType.NUMBERED) number = 0
            when (b.type) {
                BlockType.PARAGRAPH -> appendLine(b.text)
                BlockType.H1 -> appendLine("# ${b.text}")
                BlockType.H2 -> appendLine("## ${b.text}")
                BlockType.H3 -> appendLine("### ${b.text}")
                BlockType.BULLET -> appendLine("$pad- ${b.text}")
                BlockType.NUMBERED -> { number++; appendLine("$pad$number. ${b.text}") }
                BlockType.CHECK, BlockType.TASK -> appendLine("$pad- [${if (b.checked) "x" else " "}] ${b.text}")
                BlockType.QUOTE -> appendLine("> ${b.text}")
                BlockType.CALLOUT -> appendLine("> 💡 ${b.text}")
                BlockType.CODE -> { appendLine("```"); appendLine(b.text); appendLine("```") }
                BlockType.DIVIDER -> appendLine("---")
                BlockType.IMAGE -> appendLine("![${b.name ?: "imagen"}](${b.name ?: "imagen"})")
                BlockType.FILE -> appendLine("📎 ${b.name}")
                BlockType.AUDIO -> appendLine("🎙 ${b.name}")
                BlockType.NOTE_LINK -> appendLine("🔗 ${b.name}")
                BlockType.TABLE -> {
                    b.rows.forEachIndexed { i, row ->
                        appendLine("| " + row.joinToString(" | ") + " |")
                        if (i == 0) appendLine("|" + row.joinToString("|") { " --- " } + "|")
                    }
                }
            }
        }
    }
}

/** Formato en línea estilo markdown: **negrita**, *cursiva*, ++subrayado++, ~~tachado~~, ==resaltado==, `código`, [texto](url). */
object Inline {
    enum class Kind { BOLD, ITALIC, UNDERLINE, STRIKE, HIGHLIGHT, CODE, LINK, URL }

    data class Span(val kind: Kind, val start: Int, val end: Int, val contentStart: Int, val contentEnd: Int, val url: String? = null)

    private val patterns = listOf(
        Kind.CODE to Regex("`([^`\\n]+)`"),
        Kind.BOLD to Regex("\\*\\*(.+?)\\*\\*"),
        Kind.ITALIC to Regex("(?<![*\\\\])\\*(?![*\\s])(.+?)(?<![*\\s\\\\])\\*(?!\\*)"),
        Kind.UNDERLINE to Regex("\\+\\+(.+?)\\+\\+"),
        Kind.STRIKE to Regex("~~(.+?)~~"),
        Kind.HIGHLIGHT to Regex("==(.+?)=="),
        Kind.LINK to Regex("\\[([^\\]\\n]+)\\]\\(([^)\\s]+)\\)"),
    )
    private val urlRegex = Regex("(?<![(\\w])(https?://[^\\s)\\]]+)")

    fun parse(text: String): List<Span> {
        val spans = mutableListOf<Span>()
        for ((kind, re) in patterns) {
            re.findAll(text).forEach { m ->
                val g = m.groups[1]!!
                val url = if (kind == Kind.LINK) m.groups[2]?.value else null
                spans += Span(kind, m.range.first, m.range.last + 1, g.range.first, g.range.last + 1, url)
            }
        }
        val linkRanges = spans.filter { it.kind == Kind.LINK }
        urlRegex.findAll(text).forEach { m ->
            val s = m.range.first
            val e = m.range.last + 1
            if (linkRanges.none { s >= it.start && e <= it.end }) {
                spans += Span(Kind.URL, s, e, s, e, m.value)
            }
        }
        return spans
    }

    fun strip(text: String): String {
        var t = text
        t = t.replace(Regex("\\[([^\\]\\n]+)\\]\\(([^)\\s]+)\\)"), "$1")
        t = t.replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
        t = t.replace(Regex("\\+\\+(.+?)\\+\\+"), "$1")
        t = t.replace(Regex("~~(.+?)~~"), "$1")
        t = t.replace(Regex("==(.+?)=="), "$1")
        t = t.replace(Regex("`([^`\\n]+)`"), "$1")
        t = t.replace(Regex("(?<![*\\\\])\\*(?![*\\s])(.+?)(?<![*\\s\\\\])\\*(?!\\*)"), "$1")
        return t
    }

    fun toHtml(text: String): String {
        var t = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        t = t.replace(Regex("\\[([^\\]\\n]+)\\]\\(([^)\\s]+)\\)"), "<a href=\"$2\">$1</a>")
        t = t.replace(Regex("\\*\\*(.+?)\\*\\*"), "<b>$1</b>")
        t = t.replace(Regex("\\+\\+(.+?)\\+\\+"), "<u>$1</u>")
        t = t.replace(Regex("~~(.+?)~~"), "<s>$1</s>")
        t = t.replace(Regex("==(.+?)=="), "<mark>$1</mark>")
        t = t.replace(Regex("`([^`\\n]+)`"), "<code>$1</code>")
        t = t.replace(Regex("(?<![*\\\\])\\*(?![*\\s])(.+?)(?<![*\\s\\\\])\\*(?!\\*)"), "<i>$1</i>")
        return t
    }
}
