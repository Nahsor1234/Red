package com.example.jeecommandcenter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.jeecommandcenter.ui.theme.BgApp
import com.example.jeecommandcenter.ui.theme.BgCard
import com.example.jeecommandcenter.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JeeTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    TopAppBar(
        title = {
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1)
                subtitle?.let {
                    Text(it, style = MaterialTheme.typography.labelMedium, color = TextSecondary, maxLines = 1)
                }
            }
        },
        navigationIcon = {
            IconButton(
                onClick = onBack,
                modifier = Modifier.padding(start = 8.dp).size(48.dp).clip(CircleShape).background(BgCard)
            ) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = { trailing?.invoke() },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = BgApp,
            scrolledContainerColor = BgApp
        ),
        modifier = modifier
    )
}
