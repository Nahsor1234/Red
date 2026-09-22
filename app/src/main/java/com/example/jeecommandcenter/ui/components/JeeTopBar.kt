package com.example.jeecommandcenter.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.jeecommandcenter.ui.theme.JeeSpacing

@Composable
fun JeeTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = JeeSpacing.lg, vertical = JeeSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(72.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            TextButton(onClick = onBack) { Text("Back") }
        }

        Column(
            Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            subtitle?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
        }

        Box(
            Modifier.size(72.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            trailing?.invoke()
        }
    }
}
