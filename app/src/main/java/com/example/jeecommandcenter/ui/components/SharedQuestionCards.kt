package com.example.jeecommandcenter.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.jeecommandcenter.ui.theme.*

/** Shared question prompt card used by chapter practice and Tests & Mocks. */
@Composable
fun SharedQuestionPromptCard(prompt: String) {
    androidx.compose.material3.Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = BgCardAlt,
        border = BorderStroke(1.dp, BgCardBorder.copy(alpha = .85f))
    ) {
        MathMarkdownText(
            text = prompt,
            modifier = Modifier.padding(18.dp),
            color = TextPrimary,
            fontSize = MaterialTheme.typography.titleLarge.fontSize,
            lineHeight = MaterialTheme.typography.titleLarge.lineHeight
        )
    }
}

/** Shared option row with matching selected, correct, and incorrect answer states. */
@Composable
fun SharedQuestionOptionRow(
    optionText: String,
    selected: Boolean,
    submitted: Boolean,
    isCorrectOption: Boolean,
    onClick: () -> Unit
) {
    val correctVisible = submitted && isCorrectOption
    val wrongSelection = submitted && selected && !isCorrectOption
    val borderColor = when {
        correctVisible -> Primary
        wrongSelection -> Danger
        selected -> Primary
        else -> BgCardBorder.copy(alpha = .8f)
    }
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected || correctVisible) PrimarySoft else BgCard)
            .border(
                BorderStroke(if (selected || correctVisible) 1.5.dp else 1.dp, borderColor),
                RoundedCornerShape(16.dp)
            )
            .clickable(enabled = !submitted, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            enabled = !submitted,
            colors = RadioButtonDefaults.colors(
                selectedColor = Primary,
                unselectedColor = TextMuted,
                disabledSelectedColor = Primary
            )
        )
        Spacer(Modifier.width(8.dp))
        MathMarkdownText(
            text = optionText,
            color = TextPrimary,
            modifier = Modifier.weight(1f),
            fontSize = MaterialTheme.typography.bodyLarge.fontSize,
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
        )
        if (correctVisible) {
            androidx.compose.material3.Icon(Icons.Filled.CheckCircle, null, tint = PrimaryLight, modifier = Modifier.size(19.dp))
        } else if (wrongSelection) {
            androidx.compose.material3.Icon(Icons.Filled.ErrorOutline, null, tint = Danger, modifier = Modifier.size(19.dp))
        }
    }
}
