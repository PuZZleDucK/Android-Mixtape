package com.example.androidmixtape.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp

/** Direct selection replaces cycling through dozens of artist theme options. */
@Composable
internal fun <T> DemoThemeChoice(label:String,current:T,options:List<T>,name:(T)->String,onSelect:(T)->Unit) {
    var open by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(label,Modifier.weight(.34f))
        Box(Modifier.weight(.66f)) {
            OutlinedButton(onClick={open=true},modifier=Modifier.fillMaxWidth().semantics {contentDescription="$label: ${name(current)}"}) {
                Text(name(current))
            }
            DropdownMenu(expanded=open,onDismissRequest={open=false},modifier=Modifier.heightIn(max=320.dp)) {
                options.forEach { option ->
                    DropdownMenuItem(text={Text(name(option))},onClick={onSelect(option);open=false},modifier=Modifier.semantics {selected=option==current})
                }
            }
        }
    }
}
