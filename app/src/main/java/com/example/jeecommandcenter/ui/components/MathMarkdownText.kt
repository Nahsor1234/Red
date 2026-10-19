package com.example.jeecommandcenter.ui.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.agog.mathdisplay.MTMathView
import com.agog.mathdisplay.parse.MTParseErrors

/**
 * Compact Markdown and native LaTeX renderer for educational content.
 * Uses AndroidMath's native view: no WebView, remote scripts, or JavaScript.
 * Accepts inline $...$, \( ... \), and display $$...$$ or \[...\] math.
 */
@Composable
fun MathMarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    fontSize: TextUnit = MaterialTheme.typography.bodyMedium.fontSize,
    lineHeight: TextUnit = fontSize * 1.45f
) {
    val blocks = remember(text) { parseMathBlocks(text) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MathBlock.Paragraph -> InlineMathText(
                    block.text,
                    Modifier.fillMaxWidth(),
                    color,
                    block.fontSize ?: fontSize,
                    lineHeight,
                    block.weight
                )
                is MathBlock.Bullet -> Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("•", color = color, fontSize = fontSize, lineHeight = lineHeight)
                    InlineMathText(block.text, Modifier.weight(1f), color, fontSize, lineHeight)
                }
                is MathBlock.Numbered -> Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(block.number, color = color, fontSize = fontSize, lineHeight = lineHeight)
                    InlineMathText(block.text, Modifier.weight(1f), color, fontSize, lineHeight)
                }
                is MathBlock.Display -> Box(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NativeMathFormula(block.latex, color, fontSize * 1.12f, true)
                    }
                }
                is MathBlock.Code -> Text(
                    block.text,
                    color = color.copy(alpha = .88f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = fontSize * .92f,
                    lineHeight = lineHeight
                )
                MathBlock.Rule -> HorizontalDivider(color = color.copy(alpha = .28f))
                MathBlock.Gap -> Spacer(Modifier.height(3.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InlineMathText(
    text: String,
    modifier: Modifier,
    color: Color,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    weight: FontWeight = FontWeight.Normal
) {
    val segments = remember(text) { parseInlineSegments(text) }
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        segments.forEach { segment ->
            if (segment.latex != null) {
                NativeMathFormula(
                    segment.latex,
                    color,
                    fontSize,
                    false,
                    Modifier.align(Alignment.CenterVertically)
                )
            } else if (!segment.text.isNullOrEmpty()) {
                val tokens = tokenizeText(segment.text)
                tokens.forEach { token ->
                    if (token.isNotEmpty()) {
                        val bold = token.startsWith("**") && token.endsWith("**") && token.length > 4
                        Text(
                            text = if (bold) token.substring(2, token.length - 2) else token.replace("**", ""),
                            color = color,
                            fontSize = fontSize,
                            lineHeight = lineHeight,
                            fontWeight = if (bold) FontWeight.SemiBold else weight
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NativeMathFormula(
    formula: String,
    color: Color,
    fontSize: TextUnit,
    display: Boolean,
    modifier: Modifier = Modifier
) {
    var parseFailed by remember(formula) { mutableStateOf(false) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val fontPx = with(density) { fontSize.toPx() }

    if (parseFailed) {
        Text(
            text = formula,
            modifier = modifier,
            color = color,
            fontSize = fontSize,
            lineHeight = fontSize * 1.3f,
            fontFamily = FontFamily.Monospace
        )
    } else {
        AndroidView(
            modifier = modifier,
            factory = { context ->
                MTMathView(context).apply {
                    setBackgroundColor(AndroidColor.TRANSPARENT)
                    displayErrorInline = false
                    textAlignment = MTMathView.MTTextAlignment.KMTTextAlignmentLeft
                }
            },
            update = { view ->
                if (view.textColor != color.toArgb()) view.textColor = color.toArgb()
                if (view.fontSize != fontPx) view.fontSize = fontPx
                val mode = if (display) {
                    MTMathView.MTMathViewMode.KMTMathViewModeDisplay
                } else {
                    MTMathView.MTMathViewMode.KMTMathViewModeText
                }
                if (view.labelMode != mode) view.labelMode = mode
                if (view.latex != formula.trim()) view.latex = formula.trim()
                val failed = view.lastError.errorcode != MTParseErrors.ErrorNone
                if (parseFailed != failed) parseFailed = failed
            }
        )
    }
}

private sealed interface MathBlock {
    data class Paragraph(
        val text: String,
        val fontSize: TextUnit? = null,
        val weight: FontWeight = FontWeight.Normal
    ) : MathBlock
    data class Bullet(val text: String) : MathBlock
    data class Numbered(val number: String, val text: String) : MathBlock
    data class Display(val latex: String) : MathBlock
    data class Code(val text: String) : MathBlock
    data object Rule : MathBlock
    data object Gap : MathBlock
}

private data class InlineSegment(val text: String? = null, val latex: String? = null)

private const val CODE_FENCE = "\u0060\u0060\u0060"

private fun parseMathBlocks(source: String): List<MathBlock> {
    val lines = source.replace("\r\n", "\n").replace('\r', '\n').split('\n')
    val out = mutableListOf<MathBlock>()
    var inCode = false
    var inDisplay = false
    var displayDelimiter = "$$"
    val displayLines = mutableListOf<String>()
    val codeLines = mutableListOf<String>()

    fun flushDisplay() {
        val latex = displayLines.joinToString("\n").trim()
        if (latex.isNotEmpty()) out += MathBlock.Display(latex)
        displayLines.clear()
        inDisplay = false
    }

    for (raw in lines) {
        val line = raw.trimEnd()
        val trimmed = line.trim()
        if (trimmed.startsWith(CODE_FENCE)) {
            if (inCode) {
                out += MathBlock.Code(codeLines.joinToString("\n"))
                codeLines.clear()
                inCode = false
            } else {
                inCode = true
            }
            continue
        }
        if (inCode) {
            codeLines += line
            continue
        }
        if (inDisplay) {
            if (trimmed.contains(displayDelimiter)) {
                val closingIndex = trimmed.indexOf(displayDelimiter)
                val before = trimmed.substring(0, closingIndex)
                if (before.isNotBlank()) displayLines += before
                flushDisplay()
                val after = trimmed.substring(closingIndex + displayDelimiter.length).trim()
                if (after.isNotBlank()) out += parseSingleLine(after)
            } else {
                displayLines += line
            }
            continue
        }
        if (trimmed == "$$" || trimmed == "\\[") {
            displayDelimiter = if (trimmed == "$$") "$$" else "\\]"
            inDisplay = true
            displayLines.clear()
            continue
        }
        if (trimmed.startsWith("$$") && trimmed.endsWith("$$") && trimmed.length >= 4) {
            out += MathBlock.Display(trimmed.substring(2, trimmed.length - 2).trim())
            continue
        }
        if (trimmed.startsWith("\\[") && trimmed.endsWith("\\]")) {
            out += MathBlock.Display(trimmed.removePrefix("\\[").removeSuffix("\\]").trim())
            continue
        }
        out += parseSingleLine(line)
    }
    if (inCode && codeLines.isNotEmpty()) out += MathBlock.Code(codeLines.joinToString("\n"))
    if (inDisplay) flushDisplay()
    return out
}

private fun parseSingleLine(line: String): MathBlock {
    val trimmed = line.trim()
    if (trimmed.isEmpty()) return MathBlock.Gap
    if (trimmed == "---" || trimmed == "***" || trimmed == "___") return MathBlock.Rule
    if (trimmed.startsWith("### ")) return MathBlock.Paragraph(trimmed.removePrefix("### "), 18.sp, FontWeight.SemiBold)
    if (trimmed.startsWith("## ")) return MathBlock.Paragraph(trimmed.removePrefix("## "), 20.sp, FontWeight.SemiBold)
    if (trimmed.startsWith("# ")) return MathBlock.Paragraph(trimmed.removePrefix("# "), 23.sp, FontWeight.Bold)
    if (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ")) {
        return MathBlock.Bullet(trimmed.drop(2))
    }
    val numbered = Regex("^(\\d+\\.)\\s+(.*)$").matchEntire(trimmed)
    if (numbered != null) return MathBlock.Numbered(numbered.groupValues[1], numbered.groupValues[2])
    return MathBlock.Paragraph(line)
}

private fun parseInlineSegments(source: String): List<InlineSegment> {
    val segments = mutableListOf<InlineSegment>()
    var cursor = 0
    while (cursor < source.length) {
        val parenStart = source.indexOf("\\(", cursor)
        val dollarStart = source.indexOf('$', cursor)
        val start = listOf(parenStart, dollarStart).filter { it >= 0 }.minOrNull()
        if (start == null) {
            segments += InlineSegment(text = source.substring(cursor))
            break
        }
        if (start > cursor) segments += InlineSegment(text = source.substring(cursor, start))
        val delimiter = when {
            source.startsWith("\\(", start) -> "\\("
            source.startsWith("$$", start) -> "$$"
            else -> "$"
        }
        val closing = when (delimiter) {
            "\\(" -> "\\)"
            "$$" -> "$$"
            else -> "$"
        }
        val contentStart = start + delimiter.length
        val end = findUnescaped(source, closing, contentStart)
        if (end < 0) {
            segments += InlineSegment(text = source.substring(start))
            break
        }
        val latex = source.substring(contentStart, end).trim()
        if (latex.isEmpty()) {
            segments += InlineSegment(text = source.substring(start, end + closing.length))
        } else {
            segments += InlineSegment(latex = latex)
        }
        cursor = end + closing.length
    }
    return if (segments.isEmpty()) listOf(InlineSegment(text = source)) else segments
}

private fun findUnescaped(source: String, delimiter: String, from: Int): Int {
    var index = source.indexOf(delimiter, from)
    while (index >= 0) {
        var backslashes = 0
        var i = index - 1
        while (i >= 0 && source[i] == '\\') {
            backslashes++
            i--
        }
        if (backslashes % 2 == 0) return index
        index = source.indexOf(delimiter, index + delimiter.length)
    }
    return -1
}

private fun tokenizeText(source: String): List<String> {
    val pattern = Regex("\\*\\*.*?\\*\\*|[^\\s]+\\s*|\\s+")
    return pattern.findAll(source).map { it.value }.toList()
}
