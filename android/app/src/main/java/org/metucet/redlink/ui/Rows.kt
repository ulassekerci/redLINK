package org.metucet.redlink.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.metucet.redlink.R

// A rounded block of rows.
@Composable
fun Group(content: @Composable ColumnScope.() -> Unit) = Surface(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 12.dp),
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceContainer,
) {
    Column(content = content)
}

@Composable
fun ListRow(
    label: String,
    modifier: Modifier = Modifier,
    dot: Dot? = null,
    trailing: @Composable (() -> Unit)? = null,
) = ListItem(
    headlineContent = { Text(label) },
    modifier = modifier,
    leadingContent = dot?.let { { Box(Modifier.size(10.dp).background(it.color, CircleShape)) } },
    trailingContent = trailing,
    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
)

// The bar of a screen reached from the main screen.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackBar(title: String, onBack: () -> Unit) = TopAppBar(
    title = { Text(title) },
    navigationIcon = {
        IconButton(onClick = onBack) { Icon(painterResource(R.drawable.arrow_back), contentDescription = "Back") }
    },
)
