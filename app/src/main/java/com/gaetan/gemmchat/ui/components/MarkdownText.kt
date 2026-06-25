package com.gaetan.gemmchat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaetan.gemmchat.ui.theme.GemmaColors
import com.gaetan.gemmchat.ui.theme.JetBrainsMonoFamily
import com.gaetan.gemmchat.ui.theme.ManropeFamily

/**
 * Rendu Markdown léger et robuste au streaming (les marqueurs non fermés sont
 * traités comme du texte littéral, jamais de crash). Gère : blocs de code,
 * code inline, **gras**, *italique*, titres #, listes à puces et numérotées.
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    color: Color = GemmaColors.TextSecondary,
    fontSize: androidx.compose.ui.unit.TextUnit = 14.5.sp,
    lineHeight: androidx.compose.ui.unit.TextUnit = 23.sp,
) {
    val blocks = remember(markdown) { parseBlocks(markdown) }
    Column(modifier = modifier) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.Code -> CodeBlock(block.code, block.language)
                is MdBlock.Lines -> block.lines.forEach { line ->
                    MarkdownLine(line, color, fontSize, lineHeight)
                }
            }
        }
    }
}

@Composable
private fun MarkdownLine(
    line: String,
    color: Color,
    fontSize: androidx.compose.ui.unit.TextUnit,
    lineHeight: androidx.compose.ui.unit.TextUnit,
) {
    val trimmed = line.trimStart()
    when {
        trimmed.isEmpty() -> Spacer(modifier = Modifier.height(6.dp))

        HEADING.matches(trimmed) -> {
            val m = HEADING.find(trimmed)!!
            val level = m.groupValues[1].length
            Text(
                text = parseInline(m.groupValues[2]),
                color = GemmaColors.TextPrimary,
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.Bold,
                fontSize = when (level) {
                    1 -> 19.sp
                    2 -> 17.sp
                    else -> 15.5.sp
                },
                lineHeight = lineHeight,
                modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
            )
        }

        BULLET.matches(trimmed) -> {
            val content = BULLET.find(trimmed)!!.groupValues[1]
            ListRow(marker = "•", content = content, color = color, fontSize = fontSize, lineHeight = lineHeight)
        }

        NUMBERED.matches(trimmed) -> {
            val m = NUMBERED.find(trimmed)!!
            ListRow(marker = "${m.groupValues[1]}.", content = m.groupValues[2], color = color, fontSize = fontSize, lineHeight = lineHeight)
        }

        else -> Text(
            text = parseInline(line),
            color = color,
            fontFamily = ManropeFamily,
            fontSize = fontSize,
            lineHeight = lineHeight,
        )
    }
}

@Composable
private fun ListRow(
    marker: String,
    content: String,
    color: Color,
    fontSize: androidx.compose.ui.unit.TextUnit,
    lineHeight: androidx.compose.ui.unit.TextUnit,
) {
    Row(modifier = Modifier.padding(start = 2.dp, top = 1.dp, bottom = 1.dp)) {
        Text(
            text = marker,
            color = GemmaColors.AccentPurpleSoft,
            fontFamily = ManropeFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = fontSize,
            lineHeight = lineHeight,
            modifier = Modifier.width(if (marker.length > 1) 26.dp else 18.dp),
        )
        Text(
            text = parseInline(content),
            color = color,
            fontFamily = ManropeFamily,
            fontSize = fontSize,
            lineHeight = lineHeight,
        )
    }
}

@Composable
private fun CodeBlock(code: String, language: String?) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(GemmaColors.SurfacePill)
            .border(1.dp, GemmaColors.BorderSubtle, RoundedCornerShape(12.dp)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = language?.takeIf { it.isNotBlank() } ?: "code",
                color = GemmaColors.TextDim,
                fontFamily = JetBrainsMonoFamily,
                fontSize = 10.5.sp,
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(
                        onClickLabel = "Copier le code",
                    ) {
                        clipboard.setText(AnnotatedString(code))
                        copied = true
                    }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                    contentDescription = if (copied) "Copié" else "Copier le code",
                    tint = if (copied) GemmaColors.Success else GemmaColors.TextMuted,
                    modifier = Modifier.size(15.dp),
                )
                Text(
                    text = if (copied) "Copié" else "Copier",
                    color = if (copied) GemmaColors.Success else GemmaColors.TextMuted,
                    fontFamily = ManropeFamily,
                    fontSize = 11.sp,
                )
            }
        }
        Text(
            text = code,
            color = GemmaColors.TextSecondary,
            fontFamily = JetBrainsMonoFamily,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            modifier = Modifier
                .horizontalScroll(scroll)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}

private fun parseInline(s: String): AnnotatedString = buildAnnotatedString {
    // Nettoyage LaTeX/maths d'abord (n'affecte que le texte de prose / titres /
    // listes ; les blocs de code passent par CodeBlock sans parseInline).
    appendInline(cleanupMath(s))
}

private fun androidx.compose.ui.text.AnnotatedString.Builder.appendInline(s: String) {
    var i = 0
    while (i < s.length) {
        when {
            s.startsWith("**", i) -> {
                val end = s.indexOf("**", i + 2)
                if (end == -1) {
                    append(s[i]); i++
                } else {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        appendInline(s.substring(i + 2, end))
                    }
                    i = end + 2
                }
            }
            s[i] == '`' -> {
                val end = s.indexOf('`', i + 1)
                if (end == -1) {
                    append(s[i]); i++
                } else {
                    withStyle(
                        SpanStyle(
                            fontFamily = JetBrainsMonoFamily,
                            background = GemmaColors.SurfaceInput,
                            color = GemmaColors.AccentPurplePale,
                        ),
                    ) {
                        append(s.substring(i + 1, end))
                    }
                    i = end + 1
                }
            }
            s[i] == '*' -> {
                val end = s.indexOf('*', i + 1)
                if (end == -1 || end == i + 1) {
                    append(s[i]); i++
                } else {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        appendInline(s.substring(i + 1, end))
                    }
                    i = end + 1
                }
            }
            else -> {
                append(s[i]); i++
            }
        }
    }
}

private sealed interface MdBlock {
    data class Lines(val lines: List<String>) : MdBlock
    data class Code(val code: String, val language: String?) : MdBlock
}

private fun parseBlocks(markdown: String): List<MdBlock> {
    val blocks = mutableListOf<MdBlock>()
    val lines = markdown.split("\n")
    var i = 0
    val pending = mutableListOf<String>()

    fun flush() {
        if (pending.isNotEmpty()) {
            blocks += MdBlock.Lines(pending.toList())
            pending.clear()
        }
    }

    while (i < lines.size) {
        val line = lines[i]
        if (line.trimStart().startsWith("```")) {
            flush()
            val language = line.trimStart().removePrefix("```").trim().ifBlank { null }
            val code = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                code += lines[i]
                i++
            }
            // saute la clôture si présente (sinon : bloc non fermé en streaming)
            if (i < lines.size) i++
            blocks += MdBlock.Code(code.joinToString("\n"), language)
        } else {
            pending += line
            i++
        }
    }
    flush()
    return blocks
}

private val HEADING = Regex("^(#{1,6})\\s+(.*)$")
private val BULLET = Regex("^[-*•]\\s+(.*)$")
private val NUMBERED = Regex("^(\\d+)\\.\\s+(.*)$")
