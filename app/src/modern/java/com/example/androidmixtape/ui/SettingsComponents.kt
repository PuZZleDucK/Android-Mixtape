package com.example.androidmixtape.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Settings use their own paper/ink palette without changing cassette artwork. */
@Composable
internal fun SettingsPage(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    backLabel: String = "Settings",
    controls: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = if (isSystemInDarkTheme()) {
        darkColorScheme(
            primary = Color(0xFFFFB68E), onPrimary = Color(0xFF552000),
            primaryContainer = Color(0xFF533326), onPrimaryContainer = Color(0xFFFFDCC8),
            background = Color(0xFF191C21), surface = Color(0xFF24282E),
            onSurface = Color(0xFFF1EAE0), onSurfaceVariant = Color(0xFFC7C0B8),
            surfaceVariant = Color(0xFF30343B), outlineVariant = Color(0xFF4A4D51),
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF96471F), onPrimary = Color.White,
            primaryContainer = Color(0xFFFFE4CF), onPrimaryContainer = Color(0xFF542A15),
            background = Color(0xFFF2EFE8), surface = Color(0xFFFFFCF6),
            onSurface = Color(0xFF282D32), onSurfaceVariant = Color(0xFF65615B),
            surfaceVariant = Color(0xFFE9E4DA), outlineVariant = Color(0xFFD7D0C5),
        )
    }
    MaterialTheme(colorScheme = colors) {
        Surface(modifier.fillMaxSize().imePadding(), color = colors.background) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 1040.dp).fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(
                        onClick = onBack,
                        modifier = Modifier.semantics { contentDescription = if (backLabel == "Back") "Back" else "Back to $backLabel" },
                    ) { Text("‹ $backLabel") }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                            modifier = Modifier.semantics { heading() })
                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                }
                controls()
                HorizontalDivider(color = colors.outlineVariant)
                Column(Modifier.weight(1f).padding(horizontal = 12.dp), content = content)
            }
        }
        }
    }
}

@Composable
internal fun SettingsTwoColumns(first: @Composable () -> Unit, second: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth >= 700.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) { first() }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) { second() }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { first(); second() }
        }
    }
}

@Composable
internal fun SettingsSection(title: String, description: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() })
            description?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            content()
        }
    }
}

@Composable
internal fun SettingsLink(title: String, summary: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent,
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            Modifier.heightIn(min = 64.dp).padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("›", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
internal fun SettingsChoice(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.heightIn(min = 48.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(12.dp),
    ) {
        Box(Modifier.padding(horizontal = 12.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.labelLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
internal fun <T> SettingsOptionGrid(options: List<T>, minSize: Dp = 280.dp, itemContent: @Composable (T) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize),
        modifier = Modifier.fillMaxSize().semantics { contentDescription = "Settings options" },
        contentPadding = PaddingValues(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(options, key = { it.toString() }) { itemContent(it) }
    }
}

@Composable
internal fun SettingsPreviewCard(
    title: String,
    selected: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit,
    singleChoice: Boolean = false,
    preview: @Composable () -> Unit,
) {
    val selection = if (singleChoice) {
        Modifier.selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onSelect)
    } else {
        Modifier.toggleable(value = selected, enabled = enabled, role = Role.Checkbox, onValueChange = { onSelect() })
    }
    Card(
        modifier = Modifier.fillMaxWidth().then(selection),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { preview() }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        when { !enabled -> "At least one must stay enabled"; singleChoice && selected -> "Current deck"; selected -> "Enabled"; else -> "Not selected" },
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // The entire card is one accessible control, not a second competing click target.
                if (singleChoice) RadioButton(selected, onClick = null)
                else Checkbox(selected, onCheckedChange = null, enabled = enabled)
            }
        }
    }
}
