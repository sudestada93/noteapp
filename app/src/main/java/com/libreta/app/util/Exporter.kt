package com.libreta.app.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.libreta.app.data.Repository
import com.libreta.app.model.Block
import com.libreta.app.model.BlockCodec
import com.libreta.app.model.BlockType
import com.libreta.app.model.Inline

object Exporter {
    private var keepAlive: WebView? = null

    fun html(title: String, blocks: List<Block>, repo: Repository): String = buildString {
        append(
            """<!doctype html><html><head><meta charset="utf-8"><style>
            body{font-family:'IBM Plex Sans',sans-serif;color:#383838;background:#fff;margin:32px;line-height:1.55;font-size:13pt}
            h1.t{font-family:monospace;font-weight:600;font-size:24pt;border-bottom:3px solid #383838;padding-bottom:8px;letter-spacing:.02em}
            h1,h2,h3{font-family:monospace}
            blockquote{border-left:5px solid #6fc2ff;margin:8px 0;padding:2px 14px;font-style:italic}
            pre{background:#2a2927;color:#f4efea;padding:12px;border:2px solid #383838;white-space:pre-wrap}
            code{background:#ebf9ff;padding:1px 4px;font-family:monospace}
            mark{background:#ffef7a}
            .callout{background:#fff8c4;border:2px solid #383838;padding:10px 14px;margin:8px 0}
            .task{border:1.5px solid #383838;padding:6px 10px;margin:4px 0}
            table{border-collapse:collapse;margin:10px 0}td{border:1.5px solid #383838;padding:6px 10px}tr:first-child td{background:#ebf9ff;font-weight:600}
            img{max-width:100%;border:2px solid #383838;margin:8px 0}
            hr{border:none;border-top:2px dashed #383838;margin:16px 0}
            .file{border:2px solid #383838;padding:8px 12px;margin:6px 0;font-family:monospace}
            </style></head><body>""",
        )
        append("<h1 class=\"t\">").append(esc(title.ifBlank { "Sin título" })).append("</h1>")
        var inList: String? = null
        fun closeList() { inList?.let { append("</$it>") }; inList = null }
        blocks.forEach { b ->
            val listTag = when (b.type) { BlockType.BULLET -> "ul"; BlockType.NUMBERED -> "ol"; else -> null }
            if (listTag != inList) { closeList(); if (listTag != null) { append("<$listTag>"); inList = listTag } }
            val t = Inline.toHtml(b.text)
            when (b.type) {
                BlockType.PARAGRAPH -> append("<p>").append(t.ifBlank { "&nbsp;" }).append("</p>")
                BlockType.H1 -> append("<h1>$t</h1>")
                BlockType.H2 -> append("<h2>$t</h2>")
                BlockType.H3 -> append("<h3>$t</h3>")
                BlockType.BULLET, BlockType.NUMBERED -> append("<li style=\"margin-left:${b.indent * 20}px\">$t</li>")
                BlockType.CHECK -> append("<div>${if (b.checked) "☑" else "☐"} ${if (b.checked) "<s>$t</s>" else t}</div>")
                BlockType.TASK -> append("<div class=\"task\">${if (b.checked) "●" else "○"} $t ${b.dueAt?.let { "<small>— " + Fmt.due(it) + "</small>" } ?: ""}${if (b.flagged) " 🚩" else ""}</div>")
                BlockType.QUOTE -> append("<blockquote>$t</blockquote>")
                BlockType.CODE -> append("<pre>").append(esc(b.text)).append("</pre>")
                BlockType.CALLOUT -> append("<div class=\"callout\">💡 $t</div>")
                BlockType.DIVIDER -> append("<hr>")
                BlockType.IMAGE -> b.path?.let { append("<img src=\"file://${repo.fileFor(it).absolutePath}\">") }
                BlockType.FILE -> append("<div class=\"file\">📎 ${esc(b.name ?: "")} (${Fmt.size(b.size)})</div>")
                BlockType.AUDIO -> append("<div class=\"file\">🎙 ${esc(b.name ?: "")} (${Fmt.duration(b.durationMs)})</div>")
                BlockType.NOTE_LINK -> append("<div class=\"file\">🔗 ${esc(b.name ?: "Nota")}</div>")
                BlockType.TABLE -> {
                    append("<table>")
                    b.rows.forEach { r -> append("<tr>"); r.forEach { append("<td>").append(esc(it)).append("</td>") }; append("</tr>") }
                    append("</table>")
                }
            }
        }
        closeList()
        append("</body></html>")
    }

    private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    @SuppressLint("SetJavaScriptEnabled")
    fun print(context: Context, title: String, blocks: List<Block>, repo: Repository) {
        val wv = WebView(context)
        wv.settings.allowFileAccess = true
        wv.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) {
                val pm = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                val name = title.ifBlank { "Nota" }
                pm.print(name, view.createPrintDocumentAdapter(name), PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build())
                keepAlive = null
            }
        }
        keepAlive = wv
        wv.loadDataWithBaseURL("file:///", html(title, blocks, repo), "text/html", "utf-8", null)
    }

    fun shareText(context: Context, title: String, blocks: List<Block>) {
        val i = Intent(Intent.ACTION_SEND).setType("text/plain")
            .putExtra(Intent.EXTRA_SUBJECT, title)
            .putExtra(Intent.EXTRA_TEXT, BlockCodec.toMarkdown(title, blocks))
        context.startActivity(Intent.createChooser(i, "Compartir nota"))
    }
}
