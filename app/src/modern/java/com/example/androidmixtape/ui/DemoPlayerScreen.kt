package com.example.androidmixtape.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.androidmixtape.data.Track
import com.example.androidmixtape.playback.AudioLevelMonitor
import com.example.androidmixtape.playback.mixtapeCounterValue
import com.example.androidmixtape.playback.mixtapePlaybackProgress
import com.example.androidmixtape.viewmodel.*

/** Native app composition of independently themed player, cassette, sleeve and case surfaces. */
@Composable
internal fun DemoPlayerScreen(
    state:MixtapeUiState,name:String,showTapes:Boolean,ejected:Boolean,listState:LazyListState,
    onSelectTape:(Int)->Unit,onToggleList:()->Unit,onSettings:()->Unit,onCustomize:()->Unit,onCustomizeTape:(Int)->Unit,
    onEject:()->Unit,onPlayPause:()->Unit,onPrevious:()->Unit,onNext:()->Unit,onStop:()->Unit,
    onSelectTrack:(Int,Track)->Unit,onDelete:(Track)->Unit,onRemove:(Track)->Unit,onInfo:(Track)->Unit,
    modifier:Modifier=Modifier,
) {
    val audio by AudioLevelMonitor.levels.collectAsState()
    val progress=mixtapePlaybackProgress(state.queueTracks,state.currentIndex,state.positionMs)
    val geometry=demoDeckGeometry(state.mixtapeThemeSettings.deckTheme)
    val deck:@Composable (Modifier)->Unit={deckModifier->
        DemoDeck(theme=state.mixtapeThemeSettings.deckTheme,name=name,properties=state.currentMixtapeVisualProperties,
            progress=progress,counter=mixtapeCounterValue(progress),counterRevision=state.counterRevision,
            playing=state.isPlaying&&!ejected,transportCue=state.transportCueDirection,leftLevel=audio.left,rightLevel=audio.right,
            ejected=ejected,showTapes=showTapes,hasTrack=state.currentTrack!=null,
            canPrevious=state.canGoPrevious,canNext=state.canGoNext,
            onEject=onEject,onPlayPause=onPlayPause,onPrevious=onPrevious,onNext=onNext,onStop=onStop,
            onToggleList=onToggleList,onSettings=onSettings,onCustomize=onCustomize,modifier=deckModifier)
    }
    val list:@Composable (Modifier)->Unit={listModifier->
        if(showTapes) DemoTapeShelf(state.mixTapeGroups,state.currentMixtapeIndex,listState,onSelectTape,listModifier,
            centerCurrent=state.currentMixtapeIndex>=0,onCustomizeTape=onCustomizeTape)
        else Column(listModifier,verticalArrangement=Arrangement.spacedBy(8.dp)) {
            DemoSpine(name,state.currentMixtapeVisualProperties,Modifier.fillMaxWidth(),onClick=onCustomize,onLongClick=onCustomize)
            key(state.currentMixtapeStableKey) {
                DemoTrackList(state.queueTracks,state.currentIndex,state.currentMixtapeVisualProperties,
                    onSelectTrack,onDelete,onRemove,onInfo,Modifier.weight(1f).fillMaxWidth())
            }
        }
    }
    BoxWithConstraints(modifier.fillMaxSize()) {
        val wide=maxWidth>maxHeight||maxWidth>=840.dp
        if(wide) {
            Row(Modifier.fillMaxSize(),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                BoxWithConstraints(Modifier.weight(1.05f).fillMaxHeight(),contentAlignment=Alignment.Center) {
                    val width=minOf(maxWidth,(maxHeight-12.dp).coerceAtLeast(0.dp)*geometry.aspectRatio)
                    deck(Modifier.width(width))
                }
                list(Modifier.weight(1f).fillMaxHeight())
            }
        } else {
            val width=minOf(maxWidth,maxHeight*.55f*geometry.aspectRatio)
            Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Box(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center){deck(Modifier.width(width))}
                list(Modifier.weight(1f).fillMaxWidth())
            }
        }
    }
}
