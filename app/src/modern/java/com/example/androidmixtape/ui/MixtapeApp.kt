package com.example.androidmixtape.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.draggable2D
import androidx.compose.foundation.gestures.rememberDraggable2DState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidmixtape.data.Track
import com.example.androidmixtape.playback.AudioLevelMonitor
import com.example.androidmixtape.playback.mixtapeCounterValue
import com.example.androidmixtape.playback.mixtapePlaybackProgress
import com.example.androidmixtape.viewmodel.ArtistGrouping
import com.example.androidmixtape.viewmodel.HandwritingMessiness
import com.example.androidmixtape.viewmodel.CaseTheme
import com.example.androidmixtape.viewmodel.CassetteTheme
import com.example.androidmixtape.viewmodel.DeckTheme
import com.example.androidmixtape.viewmodel.LibraryStatus
import com.example.androidmixtape.viewmodel.LP_SONGS_PER_MIXTAPE
import com.example.androidmixtape.viewmodel.MEGA_SONGS_PER_MIXTAPE
import com.example.androidmixtape.viewmodel.MIXTAPE_TRACK_COUNT_OPTIONS
import com.example.androidmixtape.viewmodel.MixTapeGroup
import com.example.androidmixtape.viewmodel.NORMAL_SONGS_PER_MIXTAPE
import com.example.androidmixtape.viewmodel.MixtapeEmbellishment
import com.example.androidmixtape.viewmodel.MixtapeHandwritingFont
import com.example.androidmixtape.viewmodel.MixtapeNameInfo
import com.example.androidmixtape.viewmodel.MixtapeScreen
import com.example.androidmixtape.viewmodel.MixtapeSpineSkin
import com.example.androidmixtape.viewmodel.MixtapeSymbolColor
import com.example.androidmixtape.viewmodel.MixtapeTapeSkin
import com.example.androidmixtape.viewmodel.MixtapeUiState
import com.example.androidmixtape.viewmodel.MixtapeCustomization
import com.example.androidmixtape.viewmodel.MixtapeThemeSettings
import com.example.androidmixtape.viewmodel.MixtapeVisualProperties
import com.example.androidmixtape.viewmodel.ScrewTheme
import com.example.androidmixtape.viewmodel.SleeveTheme
import com.example.androidmixtape.viewmodel.StickerTheme
import com.example.androidmixtape.viewmodel.XLP_SONGS_PER_MIXTAPE
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

// Physical cassette/J-card ratios keep shell, cover, and spine surfaces from
// borrowing one another's geometry and looking stretched or squashed.
private const val CASSETTE_SHELL_ASPECT_RATIO = 400f / 254f
// NoProductionCassetteCaseFrontSurface: current production UI shows shell art,
// filed spines, and track-list paper, but no front-facing case/J-card cover.
private const val CASSETTE_CASE_FRONT_ASPECT_RATIO = 4f / 2.5625f
private const val CASSETTE_SPINE_LABEL_ASPECT_RATIO = 4f / 0.5f
private const val CASSETTE_CASE_SPINE_ASPECT_RATIO = 70f / 17f
private const val CASSETTE_J_CARD_BACK_ASPECT_RATIO = 4f / 1.0625f
private const val DEMO_DECK_PLAYER_ASPECT_RATIO = 560f / 400f
private const val CASE_PLASTIC_TINT_STRENGTH = 0.62f

private enum class NowPlayingBodyMode {
    Tracks,
    MixTapes,
}

@Composable
fun AndroidMixtapeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(),
        content = content,
    )
}

@Composable
fun MixtapeApp(
    state: MixtapeUiState,
    onRequestPermission: () -> Unit,
    onRefresh: () -> Unit,
    onShowSettings: () -> Unit = {},
    onShowHelp: () -> Unit = {},
    onShowMixtapeNames: () -> Unit = {},
    onShowMixtapeSymbolSettings: () -> Unit = {},
    onShowTapeSkinSettings: () -> Unit = {},
    onShowSpineSkinSettings: () -> Unit = {},
    onShowHandwritingFontSettings: () -> Unit = {},
    onShowDeckThemeSettings: () -> Unit = {},
    onShowCassetteThemeSettings: () -> Unit = {},
    onShowScrewThemeSettings: () -> Unit = {},
    onShowStickerThemeSettings: () -> Unit = {},
    onShowCaseThemeSettings: () -> Unit = {},
    onShowSleeveThemeSettings: () -> Unit = {},
    onShowExclusionSettings: () -> Unit = {},
    onMixTapeGroupClick: (Int) -> Unit = {},
    onBackToMixTapes: () -> Unit = {},
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onStop: () -> Unit = {},
    onEject: () -> Unit = {},
    onSeekTo: (Long) -> Unit,
    onDeleteTrackFromDevice: (Track) -> Unit = {},
    onRemoveTrackFromMixtape: (Track) -> Unit = {},
    onShowTrackInfo: (Track) -> Unit = {},
    onTrackDoubleClick: (Int, Track) -> Unit = { _, _ -> },
    onBackFromTrackInfo: () -> Unit = {},
    onResetAllMixTapes: () -> Unit = {},
    onSongsPerMixTapeChange: (Int) -> Unit = {},
    onArtistGroupingChange: (ArtistGrouping) -> Unit = {},
    onHandwritingMessinessChange: (HandwritingMessiness) -> Unit = {},
    onEditMixtapeName: (String, String) -> Unit = { _, _ -> },
    onRegenerateMixtapeName: (String) -> Unit = {},
    onAddFilenameExclusionPattern: (String) -> Unit = {},
    onRemoveFilenameExclusionPattern: (String) -> Unit = {},
    onMixtapeEmbellishmentEnabledChange: (MixtapeEmbellishment, Boolean) -> Unit = { _, _ -> },
    onMixtapeTapeSkinEnabledChange: (MixtapeTapeSkin, Boolean) -> Unit = { _, _ -> },
    onMixtapeSpineSkinEnabledChange: (MixtapeSpineSkin, Boolean) -> Unit = { _, _ -> },
    onMixtapeHandwritingFontEnabledChange: (MixtapeHandwritingFont, Boolean) -> Unit = { _, _ -> },
    onDeckThemeChange: (DeckTheme) -> Unit = {},
    onCassetteThemeEnabledChange: (CassetteTheme, Boolean) -> Unit = { _, _ -> },
    onScrewThemeEnabledChange: (ScrewTheme, Boolean) -> Unit = { _, _ -> },
    onStickerThemeEnabledChange: (StickerTheme, Boolean) -> Unit = { _, _ -> },
    onCaseThemeEnabledChange: (CaseTheme, Boolean) -> Unit = { _, _ -> },
    onSleeveThemeEnabledChange: (SleeveTheme, Boolean) -> Unit = { _, _ -> },
    onUpdateCurrentMixtapeCustomization: (MixtapeCustomization) -> Unit = {},
) {
    val mixTapeListState = rememberLazyListState()
    val mixTapeGridState = rememberLazyGridState()
    CompositionLocalProvider(LocalHandwritingMessiness provides state.mixtapeSettings.handwritingMessiness) {
        Scaffold(containerColor = state.mixtapeThemeSettings.deckTheme.backgroundColor()) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (state.status) {
                LibraryStatus.PermissionRequired -> PermissionRequired(onRequestPermission)
                LibraryStatus.Loading -> Text("Scanning for songs…")
                LibraryStatus.Empty -> EmptyLibrary(onRefresh)
                LibraryStatus.Error -> ErrorState(message = state.message, onRefresh = onRefresh)
                LibraryStatus.Ready -> when (state.screen) {
                    MixtapeScreen.MixTapes -> MixTapeLibrary(
                        groups = state.mixTapeGroups,
                        mixTapeListState = mixTapeListState,
                        mixTapeGridState = mixTapeGridState,
                        onShowSettings = onShowSettings,
                        onMixTapeGroupClick = onMixTapeGroupClick,
                        modifier = Modifier.weight(1f),
                    )
                    MixtapeScreen.NowPlaying -> NowPlaying(
                        state = state,
                        mixTapeListState = mixTapeListState,
                        mixTapeGridState = mixTapeGridState,
                        onMixTapeGroupClick = onMixTapeGroupClick,
                        onTogglePlayPause = onTogglePlayPause,
                        onPrevious = onPrevious,
                        onNext = onNext,
                        onStop = onStop,
                        onEject = onEject,
                        onDeleteTrackFromDevice = onDeleteTrackFromDevice,
                        onRemoveTrackFromMixtape = onRemoveTrackFromMixtape,
                        onShowTrackInfo = onShowTrackInfo,
                        onTrackDoubleClick = onTrackDoubleClick,
                        onUpdateCurrentMixtapeCustomization = onUpdateCurrentMixtapeCustomization,
                        modifier = Modifier.weight(1f),
                    )
                    MixtapeScreen.TrackInfo -> TrackInfoScreen(
                        track = state.selectedTrackInfo,
                        mixtapeName = state.currentMixtapeName,
                        onBack = onBackFromTrackInfo,
                        modifier = Modifier.weight(1f),
                    )
                    MixtapeScreen.Settings -> SettingsScreen(
                        mixTapeCount = state.mixTapeGroups.size,
                        songsPerMixTape = state.mixtapeSettings.songsPerMixTape,
                        artistGrouping = state.mixtapeSettings.artistGrouping,
                        handwritingMessiness = state.mixtapeSettings.handwritingMessiness,
                        onBackToMixTapes = onBackToMixTapes,
                        onShowHelp = onShowHelp,
                        onShowMixtapeNames = onShowMixtapeNames,
                        onShowMixtapeSymbolSettings = onShowMixtapeSymbolSettings,
                        onShowTapeSkinSettings = onShowTapeSkinSettings,
                        onShowSpineSkinSettings = onShowSpineSkinSettings,
                        onShowHandwritingFontSettings = onShowHandwritingFontSettings,
                        onShowDeckThemeSettings = onShowDeckThemeSettings,
                        onShowCassetteThemeSettings = onShowCassetteThemeSettings,
                        onShowScrewThemeSettings = onShowScrewThemeSettings,
                        onShowStickerThemeSettings = onShowStickerThemeSettings,
                        onShowCaseThemeSettings = onShowCaseThemeSettings,
                        onShowSleeveThemeSettings = onShowSleeveThemeSettings,
                        onShowExclusionSettings = onShowExclusionSettings,
                        onResetAllMixTapes = onResetAllMixTapes,
                        onSongsPerMixTapeChange = onSongsPerMixTapeChange,
                        onArtistGroupingChange = onArtistGroupingChange,
                        onHandwritingMessinessChange = onHandwritingMessinessChange,
                        modifier = Modifier.weight(1f),
                    )
                    MixtapeScreen.Help -> HelpScreen(
                        onBackToSettings = onShowSettings,
                        modifier = Modifier.weight(1f),
                    )
                    MixtapeScreen.MixtapeNames -> MixtapeNamesScreen(
                        mixtapeNameInfos = state.mixtapeNameInfos,
                        onBackToSettings = onShowSettings,
                        onEditMixtapeName = onEditMixtapeName,
                        onRegenerateMixtapeName = onRegenerateMixtapeName,
                        modifier = Modifier.weight(1f),
                    )
                    MixtapeScreen.MixtapeSymbolSettings -> MixtapeSymbolSettingsScreen(
                        enabledEmbellishments = state.enabledMixtapeEmbellishments,
                        onBackToSettings = onShowSettings,
                        onMixtapeEmbellishmentEnabledChange = onMixtapeEmbellishmentEnabledChange,
                        modifier = Modifier.weight(1f),
                    )
                    MixtapeScreen.TapeSkinSettings -> TapeSkinSettingsScreen(
                        enabledTapeSkins = state.enabledMixtapeTapeSkins,
                        onBackToSettings = onShowSettings,
                        onMixtapeTapeSkinEnabledChange = onMixtapeTapeSkinEnabledChange,
                        modifier = Modifier.weight(1f),
                    )
                    MixtapeScreen.SpineSkinSettings -> SpineSkinSettingsScreen(
                        enabledSpineSkins = state.enabledMixtapeSpineSkins,
                        onBackToSettings = onShowSettings,
                        onMixtapeSpineSkinEnabledChange = onMixtapeSpineSkinEnabledChange,
                        modifier = Modifier.weight(1f),
                    )
                    MixtapeScreen.HandwritingFontSettings -> HandwritingFontSettingsScreen(
                        enabledHandwritingFonts = state.enabledMixtapeHandwritingFonts,
                        onBackToSettings = onShowSettings,
                        onMixtapeHandwritingFontEnabledChange = onMixtapeHandwritingFontEnabledChange,
                        modifier = Modifier.weight(1f),
                    )
                    MixtapeScreen.DeckThemeSettings -> DeckThemeSettingsScreen(
                        selected = state.mixtapeThemeSettings.deckTheme,
                        onBack = onShowSettings,
                        onSelect = onDeckThemeChange,
                        modifier = Modifier.weight(1f),
                    )
                    MixtapeScreen.CassetteThemeSettings -> ThemeToggleSettingsScreen(
                        title = "Cassette themes", options = CassetteTheme.entries,
                        enabled = state.mixtapeThemeSettings.enabledCassetteThemes,
                        label = { it.readableName() }, onBack = onShowSettings,
                        preview = { theme, previewModifier -> CassetteComponentPreview(cassetteTheme = theme, modifier = previewModifier) },
                        onToggle = onCassetteThemeEnabledChange, modifier = Modifier.weight(1f),
                    )
                    MixtapeScreen.ScrewThemeSettings -> ThemeToggleSettingsScreen(
                        title = "Screw themes", options = ScrewTheme.entries,
                        enabled = state.mixtapeThemeSettings.enabledScrewThemes,
                        label = { it.readableName() }, onBack = onShowSettings,
                        preview = { theme, previewModifier -> CassetteComponentPreview(screwTheme = theme, modifier = previewModifier) },
                        onToggle = onScrewThemeEnabledChange, modifier = Modifier.weight(1f),
                    )
                    MixtapeScreen.StickerThemeSettings -> ThemeToggleSettingsScreen(
                        title = "Sticker themes", options = StickerTheme.entries,
                        enabled = state.mixtapeThemeSettings.enabledStickerThemes,
                        label = { it.readableName() }, onBack = onShowSettings,
                        preview = { theme, previewModifier -> CassetteComponentPreview(stickerTheme = theme, modifier = previewModifier) },
                        onToggle = onStickerThemeEnabledChange, modifier = Modifier.weight(1f),
                    )
                    MixtapeScreen.CaseThemeSettings -> ThemeToggleSettingsScreen(
                        title = "Case themes", options = CaseTheme.entries,
                        enabled = state.mixtapeThemeSettings.enabledCaseThemes,
                        label = { it.readableName() }, onBack = onShowSettings,
                        preview = { theme, previewModifier -> CaseComponentPreview(caseTheme = theme, modifier = previewModifier) },
                        onToggle = onCaseThemeEnabledChange, modifier = Modifier.weight(1f),
                    )
                    MixtapeScreen.SleeveThemeSettings -> ThemeToggleSettingsScreen(
                        title = "Sleeve themes", options = SleeveTheme.entries,
                        enabled = state.mixtapeThemeSettings.enabledSleeveThemes,
                        label = { it.readableName() }, onBack = onShowSettings,
                        preview = { theme, previewModifier -> SleeveComponentPreview(sleeveTheme = theme, modifier = previewModifier) },
                        onToggle = onSleeveThemeEnabledChange, modifier = Modifier.weight(1f),
                    )
                    MixtapeScreen.ExclusionSettings -> ExclusionSettingsScreen(
                        state = state,
                        onBackToSettings = onShowSettings,
                        onAddFilenameExclusionPattern = onAddFilenameExclusionPattern,
                        onRemoveFilenameExclusionPattern = onRemoveFilenameExclusionPattern,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
}

@Composable
private fun PermissionRequired(onRequestPermission: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Mixtape needs permission to read audio files stored on this device.")
        Button(onClick = onRequestPermission) { Text("Allow audio access") }
    }
}

@Composable
private fun EmptyLibrary(onRefresh: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("No audio files found. Add MP3, M4A, OGG, or WAV files to Music and scan again.")
        Button(onClick = onRefresh) { Text("Refresh library") }
    }
}

@Composable
private fun ErrorState(message: String, onRefresh: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Audio scan failed: $message")
        Button(onClick = onRefresh) { Text("Try again") }
    }
}

@Composable
private fun MixTapeLibrary(
    groups: List<MixTapeGroup>,
    mixTapeListState: LazyListState,
    mixTapeGridState: LazyGridState,
    onShowSettings: () -> Unit,
    onMixTapeGroupClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onShowSettings) { Text("Settings") }
            Text(
                "${groups.size} mix tapes",
                color = Color(0xFF3E4654),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        MixTapeBriefcaseList(
            groups = groups,
            mixTapeListState = mixTapeListState,
            mixTapeGridState = mixTapeGridState,
            onMixTapeGroupClick = onMixTapeGroupClick,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        )
    }
}

@Composable
private fun MixTapeBriefcaseList(
    groups: List<MixTapeGroup>,
    mixTapeListState: LazyListState,
    mixTapeGridState: LazyGridState,
    onMixTapeGroupClick: (Int) -> Unit,
    currentMixtapeIndex: Int = -1,
    forceSingleColumn: Boolean = false,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isLandscape = maxWidth > maxHeight
        val useTwoColumnLandscape = isLandscape && !forceSingleColumn
        val briefcaseModifier = Modifier
            .fillMaxSize()
            .cassetteBriefcaseFrame()
            .padding(horizontal = 10.dp, vertical = 12.dp)

        if (useTwoColumnLandscape) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = briefcaseModifier,
                state = mixTapeGridState,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                gridItemsIndexed(groups, key = { index, _ -> index }) { index, group ->
                    CassetteSpineRow(
                        group = group,
                        sideNumber = index + 1,
                        isCurrentMixtape = index == currentMixtapeIndex,
                        onClick = { onMixTapeGroupClick(index) },
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = briefcaseModifier,
                state = mixTapeListState,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                itemsIndexed(groups, key = { index, _ -> index }) { index, group ->
                    CassetteSpineRow(
                        group = group,
                        sideNumber = index + 1,
                        isCurrentMixtape = index == currentMixtapeIndex,
                        onClick = { onMixTapeGroupClick(index) },
                    )
                }
            }
        }
    }
}

private fun Modifier.cassetteBriefcaseFrame(): Modifier = drawBehind {
    val outerCase = Color(0xFF201A17)
    val sideRail = Color(0xFF4B3528)
    val brass = Color(0xFFA77A35)
    val smokyPlastic = Color(0xFF2C3038)
    drawRoundRect(
        color = outerCase,
        size = size,
        cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),
    )
    drawRoundRect(
        color = smokyPlastic,
        topLeft = Offset(8.dp.toPx(), 10.dp.toPx()),
        size = Size(size.width - 16.dp.toPx(), size.height - 20.dp.toPx()),
        cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx()),
    )
    drawRoundRect(
        color = sideRail,
        topLeft = Offset(0f, 0f),
        size = Size(size.width, 18.dp.toPx()),
        cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),
    )
    drawRoundRect(
        color = sideRail,
        topLeft = Offset(0f, size.height - 20.dp.toPx()),
        size = Size(size.width, 20.dp.toPx()),
        cornerRadius = CornerRadius(22.dp.toPx(), 22.dp.toPx()),
    )
    drawLine(
        color = brass,
        start = Offset(24.dp.toPx(), 12.dp.toPx()),
        end = Offset(size.width - 24.dp.toPx(), 12.dp.toPx()),
        strokeWidth = 2.dp.toPx(),
    )
    drawLine(
        color = Color(0x66201A17),
        start = Offset(14.dp.toPx(), 30.dp.toPx()),
        end = Offset(14.dp.toPx(), size.height - 30.dp.toPx()),
        strokeWidth = 5.dp.toPx(),
    )
    drawLine(
        color = Color(0x66201A17),
        start = Offset(size.width - 14.dp.toPx(), 30.dp.toPx()),
        end = Offset(size.width - 14.dp.toPx(), size.height - 30.dp.toPx()),
        strokeWidth = 5.dp.toPx(),
    )
}

@Composable
private fun SettingsScreen(
    mixTapeCount: Int,
    songsPerMixTape: Int,
    artistGrouping: ArtistGrouping,
    handwritingMessiness: HandwritingMessiness,
    onBackToMixTapes: () -> Unit,
    onShowHelp: () -> Unit,
    onShowMixtapeNames: () -> Unit,
    onShowMixtapeSymbolSettings: () -> Unit,
    onShowTapeSkinSettings: () -> Unit,
    onShowSpineSkinSettings: () -> Unit,
    onShowHandwritingFontSettings: () -> Unit,
    onShowDeckThemeSettings: () -> Unit,
    onShowCassetteThemeSettings: () -> Unit,
    onShowScrewThemeSettings: () -> Unit,
    onShowStickerThemeSettings: () -> Unit,
    onShowCaseThemeSettings: () -> Unit,
    onShowSleeveThemeSettings: () -> Unit,
    onShowExclusionSettings: () -> Unit,
    onResetAllMixTapes: () -> Unit,
    onSongsPerMixTapeChange: (Int) -> Unit,
    onArtistGroupingChange: (ArtistGrouping) -> Unit,
    onHandwritingMessinessChange: (HandwritingMessiness) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onBackToMixTapes, enabled = mixTapeCount > 0) { Text("Mix Tapes") }
        }
        Text("Settings", style = MaterialTheme.typography.titleLarge)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Help", style = MaterialTheme.typography.titleMedium)
                Text("Setup instructions and troubleshooting for Android Auto.")
                Button(onClick = onShowHelp) { Text("Android Auto help") }
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Mixtape names", style = MaterialTheme.typography.titleMedium)
                Text("Review names, edit them, or pick another random name from the bundled list.")
                Button(onClick = onShowMixtapeNames) { Text("Mixtape names") }
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Mixtape symbol settings", style = MaterialTheme.typography.titleMedium)
                Text("Choose which hand-drawn mixtape images can be used on cassettes.")
                Button(onClick = onShowMixtapeSymbolSettings) { Text("Mixtape symbol settings") }
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Component themes", style = MaterialTheme.typography.titleMedium)
                Text("Choose the global deck and which components can be assigned to new mixtapes.")
                Button(onClick = onShowDeckThemeSettings) { Text("Deck theme") }
                Button(onClick = onShowCassetteThemeSettings) { Text("Cassette themes") }
                Button(onClick = onShowScrewThemeSettings) { Text("Screw themes") }
                Button(onClick = onShowStickerThemeSettings) { Text("Sticker themes") }
                Button(onClick = onShowCaseThemeSettings) { Text("Case themes") }
                Button(onClick = onShowSleeveThemeSettings) { Text("Sleeve themes") }
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Handwriting font settings", style = MaterialTheme.typography.titleMedium)
                Text("Preview and choose which handwritten label fonts can be assigned to mixtapes.")
                Button(onClick = onShowHandwritingFontSettings) { Text("Handwriting font settings") }
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Exclusion settings", style = MaterialTheme.typography.titleMedium)
                Text("Hide files from mixtapes when their filenames match your patterns.")
                Button(onClick = onShowExclusionSettings) { Text("Exclusion settings") }
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Messiness", style = MaterialTheme.typography.titleMedium)
                Text("Choose how strongly handwritten labels are shifted and rotated.")
                SongCountOption(label = "High", selected = handwritingMessiness == HandwritingMessiness.High, onClick = { onHandwritingMessinessChange(HandwritingMessiness.High) })
                SongCountOption(label = "Low", selected = handwritingMessiness == HandwritingMessiness.Low, onClick = { onHandwritingMessinessChange(HandwritingMessiness.Low) })
                SongCountOption(label = "Off", selected = handwritingMessiness == HandwritingMessiness.Off, onClick = { onHandwritingMessinessChange(HandwritingMessiness.Off) })
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Songs per mix tape", style = MaterialTheme.typography.titleMedium)
                Text("$songsPerMixTape songs per mix tape")
                MIXTAPE_TRACK_COUNT_OPTIONS.forEach { option ->
                    SongCountOption(
                        label = mixtapeTrackCountLabel(option),
                        selected = songsPerMixTape == option,
                        onClick = { onSongsPerMixTapeChange(option) },
                    )
                }
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Artist grouping", style = MaterialTheme.typography.titleMedium)
                ArtistGroupingOption(
                    label = "No artist grouping",
                    selected = artistGrouping == ArtistGrouping.NoGrouping,
                    onClick = { onArtistGroupingChange(ArtistGrouping.NoGrouping) },
                )
                ArtistGroupingOption(
                    label = "Keep artist together",
                    selected = artistGrouping == ArtistGrouping.KeepArtistTogether,
                    onClick = { onArtistGroupingChange(ArtistGrouping.KeepArtistTogether) },
                )
                ArtistGroupingOption(
                    label = "Artist triplets on different tapes",
                    selected = artistGrouping == ArtistGrouping.ArtistTripletsAcrossTapes,
                    onClick = { onArtistGroupingChange(ArtistGrouping.ArtistTripletsAcrossTapes) },
                )
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Reset", style = MaterialTheme.typography.titleMedium)
                Text("Recreate every mix tape from your current library with a new random song order.")
                Button(onClick = onResetAllMixTapes, enabled = mixTapeCount > 0) {
                    Text("Reset all mixtapes")
                }
            }
        }
    }
}

@Composable
private fun DeckThemeSettingsScreen(
    selected: DeckTheme,
    onBack: () -> Unit,
    onSelect: (DeckTheme) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TextButton(onClick = onBack) { Text("Settings") }
        Text("Deck theme", style = MaterialTheme.typography.titleLarge)
        DeckTheme.entries.forEach { theme ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(theme) },
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    DeckComponentPreview(
                        deckTheme = theme,
                        modifier = Modifier
                            .width(112.dp)
                            .height(64.dp),
                    )
                    RadioButton(selected = selected == theme, onClick = { onSelect(theme) })
                    Text(theme.readableName(), modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun <T> ThemeToggleSettingsScreen(
    title: String,
    options: List<T>,
    enabled: Set<T>,
    label: (T) -> String,
    onBack: () -> Unit,
    preview: @Composable (T, Modifier) -> Unit,
    onToggle: (T, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TextButton(onClick = onBack) { Text("Settings") }
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text("Enabled options are used for newly created mixtapes. Existing tapes stay unchanged.")
        options.forEach { option ->
            val checked = option in enabled
            val canToggle = !checked || enabled.size > 1
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = canToggle) { onToggle(option, !checked) },
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    preview(
                        option,
                        Modifier
                            .width(112.dp)
                            .height(64.dp),
                    )
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { onToggle(option, it) },
                        enabled = canToggle,
                    )
                    Text(label(option), modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DeckComponentPreview(
    deckTheme: DeckTheme,
    modifier: Modifier = Modifier,
) {
    val palette = deckTheme.deckPalette()
    Canvas(modifier = modifier.semantics { contentDescription = "${deckTheme.readableName()} deck preview" }) {
        val radius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
        drawRoundRect(palette.body, size = size, cornerRadius = radius)
        drawRoundRect(
            palette.panel,
            topLeft = Offset(size.width * 0.06f, size.height * 0.10f),
            size = Size(size.width * 0.88f, size.height * 0.78f),
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
        )
        drawRoundRect(
            palette.well,
            topLeft = Offset(size.width * 0.12f, size.height * 0.18f),
            size = Size(size.width * 0.56f, size.height * 0.43f),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
        )
        drawRoundRect(
            palette.wellFrame,
            topLeft = Offset(size.width * 0.12f, size.height * 0.18f),
            size = Size(size.width * 0.56f, size.height * 0.43f),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
            style = Stroke(width = 1.5.dp.toPx()),
        )
        repeat(4) { index ->
            drawRoundRect(
                if (index == 1) palette.accent else palette.button,
                topLeft = Offset(size.width * (0.13f + index * 0.14f), size.height * 0.69f),
                size = Size(size.width * 0.11f, size.height * 0.12f),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
            )
        }
        drawRoundRect(
            palette.counterFace,
            topLeft = Offset(size.width * 0.73f, size.height * 0.23f),
            size = Size(size.width * 0.17f, size.height * 0.22f),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
        )
        repeat(2) { index ->
            drawRect(
                palette.accent,
                topLeft = Offset(size.width * (0.75f + index * 0.09f), size.height * 0.55f),
                size = Size(size.width * 0.035f, size.height * (0.16f + index * 0.08f)),
            )
        }
    }
}

@Composable
private fun CassetteComponentPreview(
    cassetteTheme: CassetteTheme = CassetteTheme.GhostClear,
    screwTheme: ScrewTheme = ScrewTheme.Light,
    stickerTheme: StickerTheme = StickerTheme.None,
    modifier: Modifier = Modifier,
) {
    val palette = cassetteTheme.palette()
    val screw = if (screwTheme == ScrewTheme.Light) Color(0xFFD7DCE2) else Color(0xFF24282F)
    val screwSlot = if (screwTheme == ScrewTheme.Light) Color(0xFF343A43) else Color(0xFFC8D0DB)
    Canvas(modifier = modifier.semantics { contentDescription = "Cassette component preview" }) {
        val shell = Size(size.width, size.height)
        drawRoundRect(palette.shell, size = shell, cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()))
        if (stickerTheme != StickerTheme.None) drawCassetteSticker(stickerTheme, palette, shell)
        drawRoundRect(
            palette.reelWell,
            topLeft = Offset(size.width * 0.19f, size.height * 0.29f),
            size = Size(size.width * 0.62f, size.height * 0.38f),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
        )
        listOf(0.31f, 0.69f).forEach { xFraction ->
            val center = Offset(size.width * xFraction, size.height * 0.48f)
            drawCircle(palette.reel, size.height * 0.18f, center)
            drawCircle(palette.tapePath, size.height * 0.075f, center)
            drawCircle(palette.accentSecondary, size.height * 0.18f, center, style = Stroke(width = 1.dp.toPx()))
        }
        listOf(Offset(0.08f, 0.14f), Offset(0.92f, 0.14f), Offset(0.08f, 0.86f), Offset(0.92f, 0.86f)).forEach { point ->
            val center = Offset(size.width * point.x, size.height * point.y)
            drawCircle(screw, 3.5.dp.toPx(), center)
            drawLine(screwSlot, center - Offset(2.dp.toPx(), 0f), center + Offset(2.dp.toPx(), 0f), 1.dp.toPx())
        }
        drawRoundRect(
            palette.accentSecondary.copy(alpha = 0.72f),
            size = shell,
            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
            style = Stroke(width = 1.5.dp.toPx()),
        )
    }
}

@Composable
private fun CaseComponentPreview(
    caseTheme: CaseTheme,
    modifier: Modifier = Modifier,
) {
    val plastic = caseTheme.plasticPalette()
    Canvas(modifier = modifier.semantics { contentDescription = "${caseTheme.readableName()} case preview" }) {
        drawRoundRect(Color(0xFF171A20), size = size, cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx()))
        drawRoundRect(
            Color(0xFFF1E8C8),
            topLeft = Offset(size.width * 0.08f, size.height * 0.14f),
            size = Size(size.width * 0.84f, size.height * 0.72f),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
        )
        drawLine(Color(0xFFB8422F), Offset(size.width * 0.19f, size.height * 0.18f), Offset(size.width * 0.19f, size.height * 0.82f), 1.dp.toPx())
        drawRoundRect(plastic.tint, size = size, cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx()))
        if (plastic.haze != Color.Transparent) drawRoundRect(plastic.haze, size = size, cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx()))
        drawRoundRect(plastic.edge, size = size, cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx()), style = Stroke(width = 2.dp.toPx()))
        drawLine(Color.White.copy(alpha = 0.35f), Offset(size.width * 0.12f, size.height * 0.12f), Offset(size.width * 0.78f, size.height * 0.12f), 1.5.dp.toPx())
    }
}

@Composable
private fun SleeveComponentPreview(
    sleeveTheme: SleeveTheme,
    modifier: Modifier = Modifier,
) {
    val paper = sleeveTheme.paperPalette()
    Canvas(modifier = modifier.semantics { contentDescription = "${sleeveTheme.readableName()} sleeve preview" }) {
        drawRoundRect(paper.base, size = size, cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx()))
        when (paper.pattern) {
            SleevePaperPattern.Lined -> {
                var y = size.height * 0.28f
                while (y < size.height) { drawLine(paper.line, Offset(0f, y), Offset(size.width, y), 1.dp.toPx()); y += size.height * 0.22f }
                paper.margin?.let { drawLine(it, Offset(size.width * 0.20f, 0f), Offset(size.width * 0.20f, size.height), 1.dp.toPx()) }
            }
            SleevePaperPattern.Grid -> {
                val step = size.height * 0.25f
                var x = step
                while (x < size.width) { drawLine(paper.line, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx()); x += step }
                var y = step
                while (y < size.height) { drawLine(paper.line, Offset(0f, y), Offset(size.width, y), 1.dp.toPx()); y += step }
            }
            SleevePaperPattern.Album -> {
                drawRect(paper.accent, Offset(0f, 0f), Size(size.width, size.height * 0.24f))
                drawRect(paper.ink.copy(alpha = 0.18f), Offset(size.width * 0.10f, size.height * 0.40f), Size(size.width * 0.80f, size.height * 0.10f))
            }
            SleevePaperPattern.Blank -> Unit
        }
        paper.speckle?.let { fleck ->
            repeat(16) { index ->
                drawCircle(fleck.copy(alpha = 0.55f), 1.dp.toPx(), Offset(size.width * ((index * 37) % 97) / 97f, size.height * ((index * 23) % 89) / 89f))
            }
        }
        drawRoundRect(paper.edge, size = size, cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx()), style = Stroke(width = 1.5.dp.toPx()))
    }
}

@Composable
private fun HelpScreen(
    onBackToSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TextButton(onClick = onBackToSettings) { Text("Back to Settings") }
        Text("Help", style = MaterialTheme.typography.titleLarge)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Show Mixtape in Android Auto", style = MaterialTheme.typography.titleMedium)
                Text("Complete these steps on your phone while parked and disconnected from the vehicle:")
                Text("1. Open Mixtape once and allow audio access.")
                Text("2. Open phone Settings → Connected devices → Connection preferences → Android Auto. You can also search Settings for Android Auto.")
                Text("3. Scroll to About and tap Version repeatedly until Android Auto developer mode is enabled.")
                Text("4. Open the three-dot menu, choose Developer settings, and turn on Enable CAL beta features.")
                Text("5. If Mixtape was sideloaded rather than installed from Google Play, also turn on Unknown sources.")
                Text("6. Return to Android Auto settings, open Customise Launcher (or Customize launcher), and make sure Mixtape is checked.")
                Text("7. Disconnect and reconnect Android Auto so the vehicle refreshes its app list.")
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Still not visible?", style = MaterialTheme.typography.titleMedium)
                Text("Confirm Mixtape is installed on the phone connected to the vehicle, then restart Android Auto or the phone and reconnect. Some Android Auto versions hide sideloaded apps until Unknown sources is enabled.")
            }
        }
    }
}

@Composable
private fun MixtapeNamesScreen(
    mixtapeNameInfos: List<MixtapeNameInfo>,
    onBackToSettings: () -> Unit,
    onEditMixtapeName: (String, String) -> Unit,
    onRegenerateMixtapeName: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editingInfo by remember { mutableStateOf<MixtapeNameInfo?>(null) }
    var editedName by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TextButton(onClick = onBackToSettings) { Text("Back to Settings") }
        Text("Mixtape names", style = MaterialTheme.typography.titleLarge)
        if (mixtapeNameInfos.isEmpty()) {
            Text("No mixtapes yet.")
        }
        mixtapeNameInfos.forEach { info ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(info.name, style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            editingInfo = info
                            editedName = info.name
                        }) { Text("Edit") }
                        OutlinedButton(onClick = { onRegenerateMixtapeName(info.stableKey) }) { Text("Randomize") }
                    }
                }
            }
        }
    }

    editingInfo?.let { info ->
        AlertDialog(
            onDismissRequest = { editingInfo = null },
            title = { Text("Edit mixtape name") },
            text = {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    label = { Text("Mixtape name") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onEditMixtapeName(info.stableKey, editedName)
                    editingInfo = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editingInfo = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun ExclusionSettingsScreen(
    state: MixtapeUiState,
    onBackToSettings: () -> Unit,
    onAddFilenameExclusionPattern: (String) -> Unit,
    onRemoveFilenameExclusionPattern: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var patternText by remember { mutableStateOf("") }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onBackToSettings) { Text("Settings") }
        }
        Text("Exclusion settings", style = MaterialTheme.typography.titleLarge)
        Text("Add filename patterns for audio files that should not be included in mixtapes.")
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = patternText,
                    onValueChange = { patternText = it },
                    label = { Text("Filename pattern") },
                    placeholder = { Text("*.tmp.mp3") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Examples: *.tmp.mp3, *voice memo*, WhatsApp*.opus")
                Button(
                    onClick = {
                        onAddFilenameExclusionPattern(patternText)
                        patternText = ""
                    },
                    enabled = patternText.isNotBlank(),
                ) { Text("Add") }
            }
        }
        Text("Current patterns", style = MaterialTheme.typography.titleMedium)
        if (state.mixtapeExclusionSettings.filenamePatterns.isEmpty()) {
            Text("No filename exclusion patterns yet.")
        } else {
            state.mixtapeExclusionSettings.filenamePatterns.forEach { pattern ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(pattern, modifier = Modifier.weight(1f))
                        OutlinedButton(onClick = { onRemoveFilenameExclusionPattern(pattern) }) {
                            Text("Remove")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MixtapeSymbolSettingsScreen(
    enabledEmbellishments: Set<MixtapeEmbellishment>,
    onBackToSettings: () -> Unit,
    onMixtapeEmbellishmentEnabledChange: (MixtapeEmbellishment, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onBackToSettings) { Text("Settings") }
        }
        Text("Mixtape symbol settings", style = MaterialTheme.typography.titleLarge)
        Text("Unselect a symbol to keep that mixtape image off cassette spines and now-playing labels.")
        MixtapeEmbellishment.entries.forEach { embellishment ->
            val selected = embellishment in enabledEmbellishments
            val toggleEnabled = !selected || enabledEmbellishments.size > 1
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = toggleEnabled) {
                            onMixtapeEmbellishmentEnabledChange(embellishment, !selected)
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    HandDrawnEmbellishment(
                        embellishment = embellishment,
                        modifier = Modifier.size(36.dp),
                        color = Color(0xFF233C6E),
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(embellishment.readableName(), fontWeight = FontWeight.SemiBold)
                        Text(if (selected) "Used for mixtapes" else "Hidden from mixtapes", style = MaterialTheme.typography.bodySmall)
                    }
                    Checkbox(
                        checked = selected,
                        onCheckedChange = { checked -> onMixtapeEmbellishmentEnabledChange(embellishment, checked) },
                        enabled = !selected || enabledEmbellishments.size > 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun TapeSkinSettingsScreen(
    enabledTapeSkins: Set<MixtapeTapeSkin>,
    onBackToSettings: () -> Unit,
    onMixtapeTapeSkinEnabledChange: (MixtapeTapeSkin, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onBackToSettings) { Text("Settings") }
        }
        Text("Tape skin settings", style = MaterialTheme.typography.titleLarge)
        Text("Unselect a skin to keep that cassette look out of newly assigned mixtapes.")
        MixtapeTapeSkin.entries.forEach { tapeSkin ->
            val selected = tapeSkin in enabledTapeSkins
            val toggleEnabled = !selected || enabledTapeSkins.size > 1
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = toggleEnabled) {
                            onMixtapeTapeSkinEnabledChange(tapeSkin, !selected)
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TapeSkinPreview(
                        tapeSkin = tapeSkin,
                        modifier = Modifier.width(160.dp),
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(tapeSkin.readableName(), fontWeight = FontWeight.SemiBold)
                        Text(if (selected) "Used for mixtapes" else "Hidden from mixtapes", style = MaterialTheme.typography.bodySmall)
                    }
                    Checkbox(
                        checked = selected,
                        onCheckedChange = { checked -> onMixtapeTapeSkinEnabledChange(tapeSkin, checked) },
                        enabled = !selected || enabledTapeSkins.size > 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun TapeSkinPreview(
    tapeSkin: MixtapeTapeSkin,
    modifier: Modifier = Modifier,
) {
    CassetteTape(
        tapeName = tapeSkin.readableName(),
        currentTrack = Track(
            id = -1,
            title = "Preview track",
            artist = "Preview artist",
            durationMs = 180_000,
            uri = "preview://tape-skin",
        ),
        isPlaying = false,
        handwritingFont = MixtapeHandwritingFont.Kalam,
        mixtapeJitterStartIndex = handwritingPerturbationIndex(tapeSkin.name.hashCode()),
        embellishment = MixtapeEmbellishment.Star,
        symbolColor = MixtapeSymbolColor.Navy,
        nameColor = MixtapeSymbolColor.Navy,
        tapeSkin = tapeSkin,
        modifier = modifier,
    )
}

@Composable
private fun SpineSkinSettingsScreen(
    enabledSpineSkins: Set<MixtapeSpineSkin>,
    onBackToSettings: () -> Unit,
    onMixtapeSpineSkinEnabledChange: (MixtapeSpineSkin, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onBackToSettings) { Text("Settings") }
        }
        Text("Spine skin settings", style = MaterialTheme.typography.titleLarge)
        Text("Unselect a spine skin to keep that cassette-spine look out of newly assigned mixtapes.")
        MixtapeSpineSkin.entries.forEach { spineSkin ->
            val selected = spineSkin in enabledSpineSkins
            val toggleEnabled = !selected || enabledSpineSkins.size > 1
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = toggleEnabled) {
                            onMixtapeSpineSkinEnabledChange(spineSkin, !selected)
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SpineSkinPreview(
                        spineSkin = spineSkin,
                        modifier = Modifier.weight(1f),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(spineSkin.readableName(), fontWeight = FontWeight.SemiBold)
                        Text(if (selected) "Used for mixtapes" else "Hidden from mixtapes", style = MaterialTheme.typography.bodySmall)
                    }
                    Checkbox(
                        checked = selected,
                        onCheckedChange = { checked -> onMixtapeSpineSkinEnabledChange(spineSkin, checked) },
                        enabled = !selected || enabledSpineSkins.size > 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun SpineSkinPreview(
    spineSkin: MixtapeSpineSkin,
    modifier: Modifier = Modifier,
) {
    CassetteSpineRow(
        group = MixTapeGroup(
            name = spineSkin.readableName(),
            tracks = listOf(Track(-2, "Preview", "Preview", 180_000, "preview://spine-skin")),
            startIndex = 0,
            visualProperties = MixtapeVisualProperties(
                jitterStartIndex = handwritingPerturbationIndex(spineSkin.name.hashCode()),
                spineSkin = spineSkin,
            ),
        ),
        sideNumber = 1,
        onClick = {},
    )
}

@Composable
private fun HandwritingFontSettingsScreen(
    enabledHandwritingFonts: Set<MixtapeHandwritingFont>,
    onBackToSettings: () -> Unit,
    onMixtapeHandwritingFontEnabledChange: (MixtapeHandwritingFont, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onBackToSettings) { Text("Settings") }
        }
        Text("Handwriting font settings", style = MaterialTheme.typography.titleLarge)
        Text("Unselect a font to keep that handwriting style out of newly assigned mixtapes.")
        MixtapeHandwritingFont.entries.forEach { handwritingFont ->
            val selected = handwritingFont in enabledHandwritingFonts
            val toggleEnabled = !selected || enabledHandwritingFonts.size > 1
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = toggleEnabled) {
                            onMixtapeHandwritingFontEnabledChange(handwritingFont, !selected)
                        }
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(handwritingFont.readableName(), fontWeight = FontWeight.SemiBold)
                            Text(if (selected) "Used for mixtapes" else "Hidden from mixtapes", style = MaterialTheme.typography.bodySmall)
                        }
                        Checkbox(
                            checked = selected,
                            onCheckedChange = { checked -> onMixtapeHandwritingFontEnabledChange(handwritingFont, checked) },
                            enabled = !selected || enabledHandwritingFonts.size > 1,
                        )
                    }
                    HandwritingFontContextPreview(
                        handwritingFont = handwritingFont,
                        onClick = {
                            if (toggleEnabled) onMixtapeHandwritingFontEnabledChange(handwritingFont, !selected)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun HandwritingFontContextPreview(
    handwritingFont: MixtapeHandwritingFont,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fontFamily = handwritingFont.cassetteHandwritingFontFamily()
    val sleevePaper = SleeveTheme.RuledNotebook.paperPalette()
    val casePlastic = CaseTheme.CrystalClear.plasticPalette()
    val previewTracks = listOf(
        Track(-101, "Neon Moon", "Night Drive", 201_000, "preview://handwriting/neon-moon"),
        Track(-102, "Tape Hiss", "Bedroom Pop", 178_000, "preview://handwriting/tape-hiss"),
    )
    val previewGroup = MixTapeGroup(
        name = "Night Drive",
        tracks = previewTracks,
        startIndex = 0,
        visualProperties = MixtapeVisualProperties(
            handwritingFont = handwritingFont,
            jitterStartIndex = handwritingPerturbationIndex(handwritingFont.name.hashCode()),
            decorativeId = "A7",
            sleeveTheme = SleeveTheme.RuledNotebook,
            caseTheme = CaseTheme.CrystalClear,
        ),
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CassetteSpineRow(
            group = previewGroup,
            sideNumber = 1,
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = sleevePaper.base),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(2.dp, casePlastic.edge),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        val lineSpacing = 49.dp.toPx()
                        var y = 50.dp.toPx()
                        while (y < size.height) {
                            drawLine(sleevePaper.line, Offset(0f, y), Offset(size.width, y), 1.5f)
                            y += lineSpacing
                        }
                    }
                    .drawWithContent {
                        drawContent()
                        drawRoundRect(
                            casePlastic.tint.copy(alpha = casePlastic.tint.alpha * CASE_PLASTIC_TINT_STRENGTH),
                            size = size,
                            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                        )
                        drawRoundRect(
                            casePlastic.edge.copy(alpha = 0.7f),
                            size = size,
                            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                            style = Stroke(width = 1.5.dp.toPx()),
                        )
                    }
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                previewTracks.forEachIndexed { index, track ->
                    val rowText = "${track.title} — ${track.artist}  ${formatDuration(track.durationMs)}"
                    JitteredHandwritingText(
                        text = rowText,
                        startIndex = handwritingTrackStartIndex(previewGroup.visualProperties.jitterStartIndex, index),
                        color = sleevePaper.ink,
                        fontFamily = fontFamily,
                        fontSize = 40.sp,
                        fontWeight = handwritingFont.effectiveCassetteWeight(FontWeight.Bold),
                        lineHeightScale = 0.8f,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        tokenization = HandwritingJitterTokenization.Character,
                        strength = HandwritingJitterStrength(maxDxEm = 0.036f, maxDyEm = 0.065f, maxRotationDegrees = 1.8f, maxTrackingEm = 0.008f),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

private fun MixtapeEmbellishment.readableName(): String = name
    .replace(Regex("(?<=.)(?=\\p{Upper})"), " ")

private fun MixtapeTapeSkin.readableName(): String = name
    .replace(Regex("(?<=.)(?=\\p{Upper})"), " ")

private fun MixtapeSpineSkin.readableName(): String = name
    .replace(Regex("(?<=.)(?=\\p{Upper})"), " ")

private enum class CassetteSpineSkinMotif {
    Plain,
    DoubleRail,
    NotebookRules,
    StickerTab,
    CollageBlocks,
    DarkFrame,
}

private data class CassetteSpineSkinPalette(
    val paper: Color,
    val label: Color,
    val strip: Color,
    val border: Color,
    val endCap: Color,
    val rule: Color,
    val accent: Color,
    val motif: CassetteSpineSkinMotif = CassetteSpineSkinMotif.Plain,
)

private fun SleeveTheme.legacySpineSkin(): MixtapeSpineSkin = when (this) {
    SleeveTheme.BlankWhite -> MixtapeSpineSkin.CreamRed
    SleeveTheme.BlueprintGrid -> MixtapeSpineSkin.PeachGridSticker
    SleeveTheme.CoralAlbum -> MixtapeSpineSkin.CoralStickerRail
    SleeveTheme.ForestFleck -> MixtapeSpineSkin.EmeraldNotebookLines
    SleeveTheme.GraphPaper -> MixtapeSpineSkin.PowderBlueMarker
    SleeveTheme.KraftBrown -> MixtapeSpineSkin.SepiaNewsprintFrame
    SleeveTheme.MidnightGrid -> MixtapeSpineSkin.MidnightRainbowFrame
    SleeveTheme.RuledNotebook -> MixtapeSpineSkin.TealNotebookRail
    SleeveTheme.AlbumPrint -> MixtapeSpineSkin.RainbowCutout
}

private fun MixtapeSpineSkin.palette(): CassetteSpineSkinPalette = when (this) {
    MixtapeSpineSkin.CreamRed -> CassetteSpineSkinPalette(
        paper = Color(0xFFF6EACF),
        label = Color(0xFFFFF6DD),
        strip = Color(0xFFB84C65),
        border = Color(0xFF9B6F55),
        endCap = Color(0xFFE8E0D5),
        rule = Color(0xFF9BAFC0),
        accent = Color(0xFFD96B7D),
    )
    MixtapeSpineSkin.BlushBlue -> CassetteSpineSkinPalette(
        paper = Color(0xFFFFE0E8),
        label = Color(0xFFFFF4F7),
        strip = Color(0xFF436B9C),
        border = Color(0xFF7C8BA7),
        endCap = Color(0xFFE8EDF6),
        rule = Color(0xFF8FA7C2),
        accent = Color(0xFFCB6686),
        motif = CassetteSpineSkinMotif.DoubleRail,
    )
    MixtapeSpineSkin.SkyGreen -> CassetteSpineSkinPalette(
        paper = Color(0xFFDCEBFA),
        label = Color(0xFFF3FAFF),
        strip = Color(0xFF4E8A61),
        border = Color(0xFF6B8B9F),
        endCap = Color(0xFFE4EFF4),
        rule = Color(0xFF7BA67F),
        accent = Color(0xFF83C6D8),
        motif = CassetteSpineSkinMotif.NotebookRules,
    )
    MixtapeSpineSkin.MintAmber -> CassetteSpineSkinPalette(
        paper = Color(0xFFDDF1E5),
        label = Color(0xFFF6FFF6),
        strip = Color(0xFFB27735),
        border = Color(0xFF879A77),
        endCap = Color(0xFFE9F2DF),
        rule = Color(0xFF95B8A6),
        accent = Color(0xFFE2A84E),
        motif = CassetteSpineSkinMotif.StickerTab,
    )
    MixtapeSpineSkin.LemonNavy -> CassetteSpineSkinPalette(
        paper = Color(0xFFF7E9B5),
        label = Color(0xFFFFF6CA),
        strip = Color(0xFF233C6E),
        border = Color(0xFF8A7335),
        endCap = Color(0xFFEDE6C8),
        rule = Color(0xFF9C9EB8),
        accent = Color(0xFFE0BD42),
        motif = CassetteSpineSkinMotif.DoubleRail,
    )
    MixtapeSpineSkin.PinkZineBorder -> CassetteSpineSkinPalette(
        paper = Color(0xFFFFE2EC),
        label = Color(0xFFFFF8F1),
        strip = Color(0xFFE63D83),
        border = Color(0xFF2C2930),
        endCap = Color(0xFFF4C2D2),
        rule = Color(0xFF8E818B),
        accent = Color(0xFFFF8CB5),
        motif = CassetteSpineSkinMotif.DoubleRail,
    )
    MixtapeSpineSkin.VioletLibraryStripe -> CassetteSpineSkinPalette(
        paper = Color(0xFFE8DDF8),
        label = Color(0xFFF8F2DA),
        strip = Color(0xFF4F4E95),
        border = Color(0xFF6E639A),
        endCap = Color(0xFFD8DAE4),
        rule = Color(0xFF9C91BD),
        accent = Color(0xFF7B78C7),
        motif = CassetteSpineSkinMotif.StickerTab,
    )
    MixtapeSpineSkin.MintCollageTab -> CassetteSpineSkinPalette(
        paper = Color(0xFFD9F4EC),
        label = Color(0xFFFFFBEB),
        strip = Color(0xFF50A891),
        border = Color(0xFF6CA78D),
        endCap = Color(0xFFE8F5EA),
        rule = Color(0xFF8DC9B2),
        accent = Color(0xFFF47F75),
        motif = CassetteSpineSkinMotif.CollageBlocks,
    )
    MixtapeSpineSkin.AmberIndexBlock -> CassetteSpineSkinPalette(
        paper = Color(0xFFF4E4BF),
        label = Color(0xFFFFF5DC),
        strip = Color(0xFFC08332),
        border = Color(0xFF8A5F33),
        endCap = Color(0xFFE2CFAB),
        rule = Color(0xFFB48758),
        accent = Color(0xFFE6B866),
        motif = CassetteSpineSkinMotif.StickerTab,
    )
    MixtapeSpineSkin.PowderBlueMarker -> CassetteSpineSkinPalette(
        paper = Color(0xFFE0EDF5),
        label = Color(0xFFD5ECFA),
        strip = Color(0xFF248899),
        border = Color(0xFF6F90A5),
        endCap = Color(0xFFE7ECEF),
        rule = Color(0xFF588CB8),
        accent = Color(0xFF77B7C6),
        motif = CassetteSpineSkinMotif.NotebookRules,
    )
    MixtapeSpineSkin.CoralStickerRail -> CassetteSpineSkinPalette(
        paper = Color(0xFFFFF0DF),
        label = Color(0xFFFFE2D8),
        strip = Color(0xFFD84B43),
        border = Color(0xFFB76A53),
        endCap = Color(0xFFF0D7C7),
        rule = Color(0xFFD79386),
        accent = Color(0xFFFFA089),
        motif = CassetteSpineSkinMotif.StickerTab,
    )
    MixtapeSpineSkin.EmeraldNotebookLines -> CassetteSpineSkinPalette(
        paper = Color(0xFFEAF4D8),
        label = Color(0xFFF9FFE9),
        strip = Color(0xFF16865E),
        border = Color(0xFF5E8F65),
        endCap = Color(0xFFDAE8C7),
        rule = Color(0xFF6AB78A),
        accent = Color(0xFF98CE6C),
        motif = CassetteSpineSkinMotif.NotebookRules,
    )
    MixtapeSpineSkin.SepiaNewsprintFrame -> CassetteSpineSkinPalette(
        paper = Color(0xFFE7D0A7),
        label = Color(0xFFF4E5C5),
        strip = Color(0xFF8B6F4F),
        border = Color(0xFF6D563F),
        endCap = Color(0xFFD1BE9A),
        rule = Color(0xFFB59A78),
        accent = Color(0xFF9D8566),
        motif = CassetteSpineSkinMotif.DarkFrame,
    )
    MixtapeSpineSkin.BlackPhotoNegative -> CassetteSpineSkinPalette(
        paper = Color(0xFF22252D),
        label = Color(0xFFECE5D3),
        strip = Color(0xFF2E7AD1),
        border = Color(0xFF15181F),
        endCap = Color(0xFF2E333F),
        rule = Color(0xFF8FB6DD),
        accent = Color(0xFFE552A3),
        motif = CassetteSpineSkinMotif.DarkFrame,
    )
    MixtapeSpineSkin.RainbowCutout -> CassetteSpineSkinPalette(
        paper = Color(0xFFFFF2D6),
        label = Color(0xFFFFFBEC),
        strip = Color(0xFF21A8A3),
        border = Color(0xFF85775E),
        endCap = Color(0xFFEAE2CF),
        rule = Color(0xFF8BA6BE),
        accent = Color(0xFFE95A91),
        motif = CassetteSpineSkinMotif.CollageBlocks,
    )
    MixtapeSpineSkin.SunburstCreamRail -> CassetteSpineSkinPalette(
        paper = Color(0xFFFFE7B0),
        label = Color(0xFFFFF7D8),
        strip = Color(0xFFE14D2A),
        border = Color(0xFFB36D21),
        endCap = Color(0xFFF3C96A),
        rule = Color(0xFFD8993B),
        accent = Color(0xFFFFB22C),
        motif = CassetteSpineSkinMotif.DoubleRail,
    )
    MixtapeSpineSkin.AquaLibraryTab -> CassetteSpineSkinPalette(
        paper = Color(0xFFD7F3F0),
        label = Color(0xFFF7F1D2),
        strip = Color(0xFF188E9D),
        border = Color(0xFF4B8A91),
        endCap = Color(0xFFEAF4E6),
        rule = Color(0xFF62AEB4),
        accent = Color(0xFFFF8A57),
        motif = CassetteSpineSkinMotif.StickerTab,
    )
    MixtapeSpineSkin.CandyStripePink -> CassetteSpineSkinPalette(
        paper = Color(0xFFFFDCEB),
        label = Color(0xFFFFF6F0),
        strip = Color(0xFFE13E86),
        border = Color(0xFFA74368),
        endCap = Color(0xFFFFB8CF),
        rule = Color(0xFFC7769B),
        accent = Color(0xFF25A7B8),
        motif = CassetteSpineSkinMotif.DoubleRail,
    )
    MixtapeSpineSkin.GoldenMemoBlock -> CassetteSpineSkinPalette(
        paper = Color(0xFFF4D67A),
        label = Color(0xFFFFF2BF),
        strip = Color(0xFF8D6C1F),
        border = Color(0xFF5F7C63),
        endCap = Color(0xFFE8BE53),
        rule = Color(0xFFD39D2F),
        accent = Color(0xFF5BA979),
        motif = CassetteSpineSkinMotif.StickerTab,
    )
    MixtapeSpineSkin.TomatoBorderLabel -> CassetteSpineSkinPalette(
        paper = Color(0xFFFFE1D0),
        label = Color(0xFFFFF7E8),
        strip = Color(0xFFD6422F),
        border = Color(0xFFB4322F),
        endCap = Color(0xFFFFC3A5),
        rule = Color(0xFFC96C58),
        accent = Color(0xFFFFD05B),
        motif = CassetteSpineSkinMotif.DarkFrame,
    )
    MixtapeSpineSkin.TealNotebookRail -> CassetteSpineSkinPalette(
        paper = Color(0xFFCBEDE6),
        label = Color(0xFFF3FFE8),
        strip = Color(0xFF0D7F7A),
        border = Color(0xFF2E6969),
        endCap = Color(0xFFB8DCD4),
        rule = Color(0xFF65B7AD),
        accent = Color(0xFFFF9F45),
        motif = CassetteSpineSkinMotif.NotebookRules,
    )
    MixtapeSpineSkin.VioletIndexPanel -> CassetteSpineSkinPalette(
        paper = Color(0xFFE5D8F6),
        label = Color(0xFFFFF3CF),
        strip = Color(0xFF6D48A6),
        border = Color(0xFF544276),
        endCap = Color(0xFFD2C0E8),
        rule = Color(0xFF8D75B6),
        accent = Color(0xFFE86889),
        motif = CassetteSpineSkinMotif.StickerTab,
    )
    MixtapeSpineSkin.LimeCutoutStripe -> CassetteSpineSkinPalette(
        paper = Color(0xFFE5F0A6),
        label = Color(0xFFFFF8DB),
        strip = Color(0xFF8FB52D),
        border = Color(0xFF6E8A35),
        endCap = Color(0xFFD7E67B),
        rule = Color(0xFFA1BD53),
        accent = Color(0xFF18AFC0),
        motif = CassetteSpineSkinMotif.CollageBlocks,
    )
    MixtapeSpineSkin.PeachGridSticker -> CassetteSpineSkinPalette(
        paper = Color(0xFFFFD9BC),
        label = Color(0xFFFFF4D9),
        strip = Color(0xFFE98745),
        border = Color(0xFFB97655),
        endCap = Color(0xFFF5C6A6),
        rule = Color(0xFFD79A76),
        accent = Color(0xFF42A5A7),
        motif = CassetteSpineSkinMotif.NotebookRules,
    )
    MixtapeSpineSkin.MidnightRainbowFrame -> CassetteSpineSkinPalette(
        paper = Color(0xFF1E2636),
        label = Color(0xFFF1E8C8),
        strip = Color(0xFF24A8B8),
        border = Color(0xFF111827),
        endCap = Color(0xFF2A3347),
        rule = Color(0xFF8EC9CE),
        accent = Color(0xFFFF4E93),
        motif = CassetteSpineSkinMotif.DarkFrame,
    )
}

private fun MixtapeHandwritingFont.readableName(): String = name
    .replace(Regex("(?<=.)(?=\\p{Upper})"), " ")

private fun MixtapeSymbolColor.readableName(): String = name
    .replace(Regex("(?<=.)(?=\\p{Upper})"), " ")

private fun MixtapeSymbolColor.toComposeColor(): Color = when (this) {
    MixtapeSymbolColor.Navy -> Color(0xFF263864)
    MixtapeSymbolColor.Red -> Color(0xFFD32F2F)
    MixtapeSymbolColor.Green -> Color(0xFF2E7D32)
    MixtapeSymbolColor.Purple -> Color(0xFF6A3D9A)
    MixtapeSymbolColor.Amber -> Color(0xFF8A5A14)
}

@Composable
private fun SongCountOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label)
    }
}

private fun mixtapeTrackCountLabel(songsPerMixTape: Int): String = when (songsPerMixTape) {
    NORMAL_SONGS_PER_MIXTAPE -> "Normal ($songsPerMixTape tracks)"
    LP_SONGS_PER_MIXTAPE -> "LP ($songsPerMixTape tracks)"
    XLP_SONGS_PER_MIXTAPE -> "XLP ($songsPerMixTape tracks)"
    MEGA_SONGS_PER_MIXTAPE -> "Mega ($songsPerMixTape tracks)"
    else -> "$songsPerMixTape tracks"
}

@Composable
private fun ArtistGroupingOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label)
    }
}

@Composable
private fun HandDrawnEmbellishment(
    embellishment: MixtapeEmbellishment,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF263864),
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 2.8.dp.toPx()
        val style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        val w = size.width
        val h = size.height
        if (w <= 1f || h <= 1f) return@Canvas

        fun point(x: Float, y: Float): Offset = Offset(w * x, h * y)
        fun line(startX: Float, startY: Float, endX: Float, endY: Float) {
            drawLine(
                color = color,
                start = point(startX, startY),
                end = point(endX, endY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }
        fun path(vararg points: Pair<Float, Float>) {
            if (points.isEmpty()) return
            val drawnPath = Path().apply {
                moveTo(w * points.first().first, h * points.first().second)
                points.drop(1).forEach { (x, y) -> lineTo(w * x, h * y) }
            }
            drawPath(drawnPath, color = color, style = style)
        }
        fun filledNoteHead(centerX: Float, centerY: Float, radius: Float) {
            drawCircle(
                color = color,
                radius = minOf(w, h) * radius,
                center = point(centerX, centerY),
            )
        }

        when (embellishment) {
            MixtapeEmbellishment.Star -> {
                path(0.50f to 0.12f, 0.60f to 0.40f, 0.90f to 0.40f, 0.66f to 0.56f, 0.76f to 0.86f, 0.50f to 0.68f, 0.24f to 0.86f, 0.34f to 0.56f, 0.10f to 0.40f, 0.40f to 0.40f, 0.50f to 0.12f)
            }
            MixtapeEmbellishment.Heart -> {
                val heart = Path().apply {
                    moveTo(w * 0.50f, h * 0.82f)
                    cubicTo(w * 0.12f, h * 0.54f, w * 0.18f, h * 0.16f, w * 0.42f, h * 0.30f)
                    cubicTo(w * 0.50f, h * 0.14f, w * 0.82f, h * 0.18f, w * 0.76f, h * 0.48f)
                    cubicTo(w * 0.72f, h * 0.64f, w * 0.58f, h * 0.72f, w * 0.50f, h * 0.82f)
                }
                drawPath(heart, color = color, style = style)
            }
            MixtapeEmbellishment.LightningBolt -> {
                path(0.60f to 0.10f, 0.30f to 0.52f, 0.52f to 0.50f, 0.40f to 0.90f, 0.76f to 0.42f, 0.54f to 0.44f, 0.60f to 0.10f)
            }
            MixtapeEmbellishment.Sparkles -> {
                line(0.30f, 0.12f, 0.30f, 0.44f)
                line(0.14f, 0.28f, 0.46f, 0.28f)
                line(0.20f, 0.18f, 0.40f, 0.38f)
                line(0.40f, 0.18f, 0.20f, 0.38f)
                line(0.70f, 0.48f, 0.70f, 0.84f)
                line(0.52f, 0.66f, 0.88f, 0.66f)
                line(0.58f, 0.54f, 0.82f, 0.78f)
                line(0.82f, 0.54f, 0.58f, 0.78f)
            }
            MixtapeEmbellishment.Smiley -> {
                drawCircle(color = color, radius = minOf(w, h) * 0.36f, center = point(0.50f, 0.50f), style = style)
                drawCircle(color = color, radius = minOf(w, h) * 0.035f, center = point(0.38f, 0.42f))
                drawCircle(color = color, radius = minOf(w, h) * 0.035f, center = point(0.62f, 0.42f))
                val smile = Path().apply {
                    moveTo(w * 0.34f, h * 0.58f)
                    quadraticTo(w * 0.50f, h * 0.76f, w * 0.68f, h * 0.58f)
                }
                drawPath(smile, color = color, style = style)
            }
            MixtapeEmbellishment.Flower -> {
                repeat(6) { petal ->
                    val angle = (petal * 60f) * PI.toFloat() / 180f
                    val center = point(0.50f + cos(angle) * 0.20f, 0.50f + sin(angle) * 0.20f)
                    drawCircle(color = color, radius = minOf(w, h) * 0.12f, center = center, style = style)
                }
                drawCircle(color = color, radius = minOf(w, h) * 0.08f, center = point(0.50f, 0.50f), style = style)
                line(0.50f, 0.64f, 0.45f, 0.90f)
                line(0.45f, 0.78f, 0.30f, 0.70f)
            }
            MixtapeEmbellishment.MusicNote -> {
                line(0.56f, 0.18f, 0.56f, 0.70f)
                line(0.56f, 0.18f, 0.80f, 0.26f)
                line(0.80f, 0.26f, 0.80f, 0.36f)
                filledNoteHead(0.42f, 0.72f, 0.14f)
            }
            MixtapeEmbellishment.Moon -> {
                val moon = Path().apply {
                    moveTo(w * 0.66f, h * 0.16f)
                    cubicTo(w * 0.30f, h * 0.20f, w * 0.20f, h * 0.70f, w * 0.60f, h * 0.86f)
                    cubicTo(w * 0.42f, h * 0.64f, w * 0.48f, h * 0.34f, w * 0.66f, h * 0.16f)
                }
                drawPath(moon, color = color, style = style)
            }
            MixtapeEmbellishment.Swirl -> {
                val swirl = Path().apply {
                    moveTo(w * 0.78f, h * 0.50f)
                    cubicTo(w * 0.72f, h * 0.20f, w * 0.28f, h * 0.24f, w * 0.28f, h * 0.54f)
                    cubicTo(w * 0.28f, h * 0.80f, w * 0.62f, h * 0.78f, w * 0.62f, h * 0.56f)
                    cubicTo(w * 0.62f, h * 0.42f, w * 0.42f, h * 0.42f, w * 0.44f, h * 0.56f)
                }
                drawPath(swirl, color = color, style = style)
            }
            MixtapeEmbellishment.Crown -> {
                path(0.16f to 0.72f, 0.22f to 0.32f, 0.42f to 0.58f, 0.52f to 0.22f, 0.66f to 0.58f, 0.84f to 0.34f, 0.78f to 0.72f, 0.16f to 0.72f)
                line(0.22f, 0.82f, 0.76f, 0.82f)
            }
            MixtapeEmbellishment.QuarterNote -> {
                line(0.62f, 0.16f, 0.62f, 0.70f)
                filledNoteHead(0.42f, 0.73f, 0.15f)
                line(0.52f, 0.65f, 0.62f, 0.70f)
            }
            MixtapeEmbellishment.EighthNote -> {
                line(0.56f, 0.15f, 0.56f, 0.70f)
                filledNoteHead(0.38f, 0.73f, 0.14f)
                val flag = Path().apply {
                    moveTo(w * 0.56f, h * 0.15f)
                    cubicTo(w * 0.82f, h * 0.20f, w * 0.78f, h * 0.42f, w * 0.60f, h * 0.46f)
                }
                drawPath(flag, color = color, style = style)
            }
            MixtapeEmbellishment.BeamedEighthNotes -> {
                line(0.32f, 0.22f, 0.32f, 0.72f)
                line(0.68f, 0.16f, 0.68f, 0.66f)
                path(0.32f to 0.22f, 0.68f to 0.16f, 0.68f to 0.28f, 0.32f to 0.34f, 0.32f to 0.22f)
                filledNoteHead(0.22f, 0.74f, 0.13f)
                filledNoteHead(0.58f, 0.68f, 0.13f)
            }
            MixtapeEmbellishment.SixteenthNote -> {
                line(0.50f, 0.14f, 0.50f, 0.72f)
                filledNoteHead(0.32f, 0.74f, 0.14f)
                val topFlag = Path().apply {
                    moveTo(w * 0.50f, h * 0.14f)
                    cubicTo(w * 0.78f, h * 0.19f, w * 0.78f, h * 0.34f, w * 0.54f, h * 0.38f)
                }
                val lowerFlag = Path().apply {
                    moveTo(w * 0.50f, h * 0.30f)
                    cubicTo(w * 0.74f, h * 0.36f, w * 0.72f, h * 0.50f, w * 0.54f, h * 0.54f)
                }
                drawPath(topFlag, color = color, style = style)
                drawPath(lowerFlag, color = color, style = style)
            }
            MixtapeEmbellishment.WholeNote -> {
                val whole = Path().apply {
                    moveTo(w * 0.24f, h * 0.54f)
                    cubicTo(w * 0.28f, h * 0.34f, w * 0.70f, h * 0.30f, w * 0.78f, h * 0.48f)
                    cubicTo(w * 0.86f, h * 0.68f, w * 0.46f, h * 0.76f, w * 0.28f, h * 0.62f)
                    cubicTo(w * 0.22f, h * 0.58f, w * 0.22f, h * 0.56f, w * 0.24f, h * 0.54f)
                }
                drawPath(whole, color = color, style = style)
                line(0.40f, 0.60f, 0.62f, 0.46f)
            }
            MixtapeEmbellishment.HalfNote -> {
                line(0.64f, 0.16f, 0.64f, 0.64f)
                val noteHead = Path().apply {
                    moveTo(w * 0.26f, h * 0.70f)
                    cubicTo(w * 0.32f, h * 0.52f, w * 0.62f, h * 0.52f, w * 0.66f, h * 0.66f)
                    cubicTo(w * 0.62f, h * 0.82f, w * 0.32f, h * 0.86f, w * 0.26f, h * 0.70f)
                }
                drawPath(noteHead, color = color, style = style)
                line(0.38f, 0.74f, 0.56f, 0.62f)
            }
            MixtapeEmbellishment.TrebleClef -> {
                val clef = Path().apply {
                    moveTo(w * 0.54f, h * 0.12f)
                    cubicTo(w * 0.38f, h * 0.26f, w * 0.36f, h * 0.48f, w * 0.56f, h * 0.56f)
                    cubicTo(w * 0.84f, h * 0.68f, w * 0.66f, h * 0.92f, w * 0.42f, h * 0.78f)
                    cubicTo(w * 0.20f, h * 0.66f, w * 0.36f, h * 0.44f, w * 0.58f, h * 0.46f)
                    cubicTo(w * 0.78f, h * 0.48f, w * 0.78f, h * 0.68f, w * 0.56f, h * 0.72f)
                }
                drawPath(clef, color = color, style = style)
                line(0.54f, 0.12f, 0.48f, 0.90f)
            }
            MixtapeEmbellishment.BassClef -> {
                val hook = Path().apply {
                    moveTo(w * 0.34f, h * 0.30f)
                    cubicTo(w * 0.66f, h * 0.16f, w * 0.86f, h * 0.54f, w * 0.42f, h * 0.76f)
                    cubicTo(w * 0.28f, h * 0.82f, w * 0.20f, h * 0.78f, w * 0.18f, h * 0.68f)
                }
                drawPath(hook, color = color, style = style)
                drawCircle(color = color, radius = minOf(w, h) * 0.04f, center = point(0.70f, 0.40f))
                drawCircle(color = color, radius = minOf(w, h) * 0.04f, center = point(0.74f, 0.58f))
            }
            MixtapeEmbellishment.SharpSign -> {
                line(0.36f, 0.18f, 0.30f, 0.84f)
                line(0.66f, 0.14f, 0.60f, 0.80f)
                line(0.22f, 0.38f, 0.78f, 0.30f)
                line(0.20f, 0.62f, 0.76f, 0.54f)
            }
            MixtapeEmbellishment.FlatSign -> {
                line(0.40f, 0.12f, 0.40f, 0.86f)
                val bowl = Path().apply {
                    moveTo(w * 0.40f, h * 0.48f)
                    cubicTo(w * 0.74f, h * 0.40f, w * 0.82f, h * 0.66f, w * 0.42f, h * 0.78f)
                }
                drawPath(bowl, color = color, style = style)
            }
        }
    }
}

private enum class SleevePaperPattern { Blank, Lined, Grid, Album }

private data class SleevePaperPalette(
    val pattern: SleevePaperPattern,
    val base: Color,
    val edge: Color,
    val line: Color,
    val margin: Color?,
    val ink: Color,
    val accent: Color,
    val speckle: Color?,
)

private fun SleeveTheme.paperPalette(): SleevePaperPalette = when (this) {
    SleeveTheme.BlankWhite -> SleevePaperPalette(SleevePaperPattern.Blank, Color(0xFFFBFAF5), Color(0xFFDCD8C8), Color.Transparent, null, Color(0xFF23272E), Color(0xFFB8422F), null)
    SleeveTheme.RuledNotebook -> SleevePaperPalette(SleevePaperPattern.Lined, Color(0xFFF8F6EC), Color(0xFFDDD8C4), Color(0xFFB9C8DD), Color(0xFFE59A9A), Color(0xFF202634), Color(0xFF3567C9), null)
    SleeveTheme.AlbumPrint -> SleevePaperPalette(SleevePaperPattern.Album, Color(0xFF101319), Color.Black, Color.Transparent, null, Color(0xFFF1EFE8), Color(0xFFD8A53F), null)
    SleeveTheme.KraftBrown -> SleevePaperPalette(SleevePaperPattern.Blank, Color(0xFFC9A876), Color(0xFFA5854F), Color.Transparent, null, Color(0xFF33261A), Color(0xFF7A2E1D), Color(0xFF8A6C42))
    SleeveTheme.MidnightGrid -> SleevePaperPalette(SleevePaperPattern.Grid, Color(0xFF182238), Color(0xFF0C1220), Color(0xFF405273), null, Color(0xFFEDF3FF), Color(0xFF5FD5D7), null)
    SleeveTheme.CoralAlbum -> SleevePaperPalette(SleevePaperPattern.Album, Color(0xFFF1D8CB), Color(0xFFBC8878), Color.Transparent, null, Color(0xFF38252A), Color(0xFFDF594D), null)
    SleeveTheme.ForestFleck -> SleevePaperPalette(SleevePaperPattern.Blank, Color(0xFFD3D9BD), Color(0xFF9BA77D), Color.Transparent, null, Color(0xFF263426), Color(0xFFA95138), Color(0xFF71805B))
    SleeveTheme.BlueprintGrid -> SleevePaperPalette(SleevePaperPattern.Grid, Color(0xFF155281), Color(0xFF0A3454), Color(0x55DEF2FF), null, Color(0xFFF2F8FC), Color(0xFFFFD05A), null)
    SleeveTheme.GraphPaper -> SleevePaperPalette(SleevePaperPattern.Grid, Color(0xFFF7F6EE), Color(0xFFD7D6CA), Color(0xFFB7C9D8), null, Color(0xFF27313B), Color(0xFF3D72A4), null)
}

private data class CasePlasticPalette(
    val tint: Color,
    val haze: Color,
    val edge: Color,
    val hinge: Color,
)

private fun CaseTheme.plasticPalette(): CasePlasticPalette = when (this) {
    CaseTheme.CrystalClear -> CasePlasticPalette(Color(0x1AEBF0FF), Color.Transparent, Color(0xA6FFFFFF), Color(0x4714141E))
    CaseTheme.CloudyClear -> CasePlasticPalette(Color(0x1FE0E8EE), Color(0x29F4F8FA), Color(0xB8FFFFFF), Color(0x4D2D343C))
    CaseTheme.SmokeTint -> CasePlasticPalette(Color(0x7312141A), Color.Transparent, Color(0x4DFFFFFF), Color(0x80000000))
    CaseTheme.AmberTint -> CasePlasticPalette(Color(0x42FF9628), Color.Transparent, Color(0x8CFFDCA0), Color(0x66783C00))
    CaseTheme.RubyClear -> CasePlasticPalette(Color(0x52EB1C2A), Color.Transparent, Color(0xC2FF808A), Color(0x75120812))
    CaseTheme.HotPinkClear -> CasePlasticPalette(Color(0x4DFF2296), Color.Transparent, Color(0xC7FF8ECC), Color(0x7019084E))
    CaseTheme.ElectricBlueClear -> CasePlasticPalette(Color(0x4A187EFF), Color.Transparent, Color(0xC787C4FF), Color(0x75053482))
    CaseTheme.AcidGreenClear -> CasePlasticPalette(Color(0x4A67F523), Color.Transparent, Color(0xC7BCFF8B), Color(0x70387805))
    CaseTheme.VioletClear -> CasePlasticPalette(Color(0x4A8943FF), Color.Transparent, Color(0xC7CA9EFF), Color(0x73411287))
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CassetteSpineRow(
    group: MixTapeGroup,
    sideNumber: Int,
    isCurrentMixtape: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val paper = group.visualProperties.sleeveTheme.paperPalette()
    val plastic = group.visualProperties.caseTheme.plasticPalette()
    val handwritingFont = group.handwritingFont
    val fontFamily = handwritingFont.cassetteHandwritingFontFamily()
    val jitterStartIndex = group.handwritingJitterStartIndex()

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrentMixtape) 4.dp else 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(CASSETTE_CASE_SPINE_ASPECT_RATIO)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .semantics(mergeDescendants = true) {
                contentDescription = if (isCurrentMixtape) "Current mixtape case spine ${group.name}" else "Case spine ${group.name}"
            },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val insetX = size.width * (8f / 280f)
                val insetY = size.height * (8f / 68f)
                val paperLeft = insetX
                val paperTop = insetY
                val paperRight = size.width - insetX
                val paperBottom = size.height - insetY
                drawRoundRect(Color(0xFF15191F), size = size, cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()))
                drawRect(paper.base, Offset(paperLeft, paperTop), Size(paperRight - paperLeft, paperBottom - paperTop))
                when (paper.pattern) {
                    SleevePaperPattern.Lined -> {
                        var y = paperTop + (18f / 52f) * (paperBottom - paperTop)
                        while (y < paperBottom) {
                            drawLine(paper.line, Offset(paperLeft + 4.dp.toPx(), y), Offset(paperRight - 4.dp.toPx(), y), 1.dp.toPx())
                            y += (18f / 52f) * (paperBottom - paperTop)
                        }
                    }
                    SleevePaperPattern.Grid -> {
                        val stepX = size.width * (18f / 280f)
                        val stepY = size.height * (18f / 68f)
                        var x = paperLeft + stepX
                        while (x < paperRight) { drawLine(paper.line, Offset(x, paperTop), Offset(x, paperBottom), 0.8.dp.toPx()); x += stepX }
                        var y = paperTop + stepY
                        while (y < paperBottom) { drawLine(paper.line, Offset(paperLeft, y), Offset(paperRight, y), 0.8.dp.toPx()); y += stepY }
                    }
                    SleevePaperPattern.Album -> drawRect(paper.accent, Offset(paperLeft + 10.dp.toPx(), paperTop + 5.dp.toPx()), Size(24.dp.toPx(), 6.dp.toPx()))
                    SleevePaperPattern.Blank -> Unit
                }
                paper.margin?.let { margin ->
                    drawLine(margin, Offset(paperLeft + 22.dp.toPx(), paperTop), Offset(paperLeft + 22.dp.toPx(), paperBottom), 1.dp.toPx())
                }
                paper.speckle?.let { fleck ->
                    repeat(18) { index ->
                        val x = paperLeft + ((index * 47) % 251) / 251f * (paperRight - paperLeft)
                        val y = paperTop + ((index * 29) % 47) / 47f * (paperBottom - paperTop)
                        drawCircle(fleck.copy(alpha = 0.35f), 0.8.dp.toPx(), Offset(x, y))
                    }
                }
                drawRect(paper.edge, Offset(paperLeft, paperTop), Size(paperRight - paperLeft, paperBottom - paperTop), style = Stroke(width = 1.dp.toPx()))
            }
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .border(BorderStroke(1.5.dp, paper.accent), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(group.visualProperties.decorativeId, color = paper.accent, fontSize = 16.5.sp, fontWeight = FontWeight.Bold)
                }
                JitteredHandwritingText(
                    text = group.name,
                    modifier = Modifier
                        .weight(1f)
                        .offset(y = handwritingFont.cassetteSpineVerticalOffset()),
                    startIndex = jitterStartIndex,
                    color = paper.ink,
                    fontFamily = fontFamily,
                    fontStyle = FontStyle.Italic,
                    fontWeight = handwritingFont.effectiveCassetteWeight(FontWeight.ExtraBold),
                    fontSize = handwritingFont.cassetteSpineFontSize(),
                    maxLines = 2,
                    overflow = TextOverflow.Clip,
                    tokenization = HandwritingJitterTokenization.Character,
                    strength = HandwritingJitterStrength(maxDxEm = 0.054f, maxDyEm = 0.105f, maxRotationDegrees = 3.0f, maxTrackingEm = 0.023f),
                )
            }
            Canvas(modifier = Modifier.matchParentSize()) {
                drawRoundRect(plastic.tint.copy(alpha = plastic.tint.alpha * CASE_PLASTIC_TINT_STRENGTH), size = size, cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()))
                if (plastic.haze != Color.Transparent) drawRoundRect(plastic.haze.copy(alpha = plastic.haze.alpha * CASE_PLASTIC_TINT_STRENGTH), size = size, cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()))
                drawRect(Color.White.copy(alpha = 0.20f), Offset(3.dp.toPx(), 3.dp.toPx()), Size(size.width - 6.dp.toPx(), 2.dp.toPx()))
                drawRect(Color.Black.copy(alpha = 0.28f), Offset(3.dp.toPx(), size.height - 5.dp.toPx()), Size(size.width - 6.dp.toPx(), 2.dp.toPx()))
                drawRoundRect(plastic.edge, topLeft = Offset(1.5.dp.toPx(), 1.5.dp.toPx()), size = Size(size.width - 3.dp.toPx(), size.height - 3.dp.toPx()), cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()), style = Stroke(width = 1.8.dp.toPx()))
                drawRoundRect(Color(0xDD06080C), topLeft = Offset(0.6.dp.toPx(), 0.6.dp.toPx()), size = Size(size.width - 1.2.dp.toPx(), size.height - 1.2.dp.toPx()), cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()), style = Stroke(width = 1.2.dp.toPx()))
                listOf(Offset(size.width * (14f / 280f), size.height / 2f), Offset(size.width * (266f / 280f), size.height / 2f)).forEach { hinge ->
                    drawCircle(plastic.hinge, 4.5.dp.toPx(), hinge)
                    drawCircle(plastic.edge.copy(alpha = 0.42f), 4.5.dp.toPx(), hinge, style = Stroke(width = 0.8.dp.toPx()))
                }
                val gloss = Path().apply {
                    moveTo(size.width * 0.25f, 0f); lineTo(size.width * 0.42f, 0f); lineTo(size.width * 0.34f, size.height); lineTo(size.width * 0.19f, size.height); close()
                }
                drawPath(gloss, Color.White.copy(alpha = 0.10f))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LegacyCassetteSpineRow(
    group: MixTapeGroup,
    sideNumber: Int,
    isCurrentMixtape: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val handwritingFont = group.handwritingFont
    val cassetteHandwritingFontFamily = handwritingFont.cassetteHandwritingFontFamily()
    val spinePalette = group.visualProperties.sleeveTheme.legacySpineSkin().palette()
    val paperColor = spinePalette.paper
    val labelStrip = spinePalette.strip
    val nameInk = group.visualProperties.nameColor.toComposeColor()
    val jitterStartIndex = group.handwritingJitterStartIndex()

    Card(
        colors = CardDefaults.cardColors(containerColor = paperColor),
        shape = RoundedCornerShape(6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrentMixtape) 4.dp else 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .semantics(mergeDescendants = true) {
                contentDescription = if (isCurrentMixtape) {
                    "Current mixtape cassette spine ${group.name}"
                } else {
                    "Cassette spine ${group.name}"
                }
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val endCapWidth = 28.dp.toPx()
                val notchWidth = 14.dp.toPx()
                // CassetteSpineTouchTargetRatioException: list rows keep a 64dp
                // tap target, but the painted case-spine band follows the
                // physical full-case spine ratio within that accessible row.
                val physicalCaseSpineHeight = size.width / CASSETTE_CASE_SPINE_ASPECT_RATIO
                val spineHeight = physicalCaseSpineHeight
                    .coerceAtLeast(56.dp.toPx())
                    .coerceAtMost(size.height)
                val spineTop = (size.height - spineHeight) / 2f
                val spineBottom = spineTop + spineHeight
                val spineSize = Size(size.width, spineHeight)
                val labelLeft = endCapWidth + 16.dp.toPx()
                val labelRight = size.width - endCapWidth - 14.dp.toPx()
                val availableLabelHeight = (spineHeight - 18.dp.toPx()).coerceAtLeast(0f)
                val physicalLabelHeight = ((labelRight - labelLeft) / CASSETTE_SPINE_LABEL_ASPECT_RATIO)
                    .coerceAtLeast(availableLabelHeight * 0.72f)
                val labelHeight = physicalLabelHeight.coerceAtMost(availableLabelHeight)
                val labelTop = spineTop + (spineHeight - labelHeight) / 2f
                drawRoundRect(
                    color = if (isCurrentMixtape) Color(0x88F7D98A) else spinePalette.accent.copy(alpha = 0.16f),
                    topLeft = Offset(0f, spineTop),
                    size = spineSize,
                    cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
                )
                drawRoundRect(
                    color = spinePalette.border.copy(alpha = 0.55f),
                    topLeft = Offset(3.dp.toPx(), spineTop + 3.dp.toPx()),
                    size = Size(size.width - 6.dp.toPx(), spineHeight - 6.dp.toPx()),
                    cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx()),
                )
                drawRoundRect(
                    color = paperColor,
                    topLeft = Offset(endCapWidth, spineTop + 6.dp.toPx()),
                    size = Size(size.width - endCapWidth * 2f, spineHeight - 12.dp.toPx()),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                )
                drawRoundRect(
                    color = spinePalette.label,
                    topLeft = Offset(labelLeft, labelTop),
                    size = Size(labelRight - labelLeft, labelHeight),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                )
                drawRoundRect(
                    color = labelStrip.copy(alpha = 0.92f),
                    topLeft = Offset(endCapWidth + 8.dp.toPx(), labelTop),
                    size = Size(6.dp.toPx(), labelHeight),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                )
                when (spinePalette.motif) {
                    CassetteSpineSkinMotif.Plain -> {
                        repeat(2) { line ->
                            val y = labelTop + labelHeight * (0.34f + line * 0.24f)
                            drawLine(
                                color = spinePalette.rule.copy(alpha = 0.48f),
                                start = Offset(labelLeft + 8.dp.toPx(), y),
                                end = Offset(labelRight - 8.dp.toPx(), y),
                                strokeWidth = 1.dp.toPx(),
                            )
                        }
                    }
                    CassetteSpineSkinMotif.DoubleRail -> {
                        drawRoundRect(
                            color = spinePalette.accent.copy(alpha = 0.82f),
                            topLeft = Offset(size.width - endCapWidth - 10.dp.toPx(), labelTop + 3.dp.toPx()),
                            size = Size(4.dp.toPx(), labelHeight - 6.dp.toPx()),
                            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                        )
                        repeat(2) { line ->
                            val y = labelTop + labelHeight * (0.28f + line * 0.44f)
                            drawLine(
                                color = spinePalette.rule.copy(alpha = 0.44f),
                                start = Offset(labelLeft + 10.dp.toPx(), y),
                                end = Offset(labelRight - 12.dp.toPx(), y),
                                strokeWidth = 1.dp.toPx(),
                            )
                        }
                    }
                    CassetteSpineSkinMotif.NotebookRules -> {
                        repeat(4) { line ->
                            val y = labelTop + labelHeight * (0.22f + line * 0.18f)
                            drawLine(
                                color = spinePalette.rule.copy(alpha = 0.42f),
                                start = Offset(labelLeft + 8.dp.toPx(), y),
                                end = Offset(labelRight - 10.dp.toPx(), y),
                                strokeWidth = 1.dp.toPx(),
                            )
                        }
                        drawLine(
                            color = spinePalette.strip.copy(alpha = 0.36f),
                            start = Offset(labelLeft + 18.dp.toPx(), labelTop + 4.dp.toPx()),
                            end = Offset(labelLeft + 18.dp.toPx(), labelTop + labelHeight - 4.dp.toPx()),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }
                    CassetteSpineSkinMotif.StickerTab -> {
                        drawRoundRect(
                            color = spinePalette.accent.copy(alpha = 0.74f),
                            topLeft = Offset(labelRight - 34.dp.toPx(), labelTop + 7.dp.toPx()),
                            size = Size(25.dp.toPx(), labelHeight - 14.dp.toPx()),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                        )
                        repeat(3) { tick ->
                            val y = labelTop + labelHeight * (0.28f + tick * 0.18f)
                            drawCircle(
                                color = spinePalette.rule.copy(alpha = 0.42f),
                                radius = 1.2.dp.toPx(),
                                center = Offset(labelLeft + 12.dp.toPx() + tick * 7.dp.toPx(), y),
                            )
                        }
                    }
                    CassetteSpineSkinMotif.CollageBlocks -> {
                        val blockTop = labelTop + 5.dp.toPx()
                        listOf(
                            Triple(labelLeft + 8.dp.toPx(), spinePalette.accent, 20.dp.toPx()),
                            Triple(labelLeft + 34.dp.toPx(), spinePalette.rule, 14.dp.toPx()),
                            Triple(labelRight - 48.dp.toPx(), spinePalette.strip, 18.dp.toPx()),
                            Triple(labelRight - 24.dp.toPx(), spinePalette.accent, 12.dp.toPx()),
                        ).forEachIndexed { index, (left, color, width) ->
                            drawRoundRect(
                                color = color.copy(alpha = 0.48f),
                                topLeft = Offset(left, blockTop + (index % 2) * 18.dp.toPx()),
                                size = Size(width, 10.dp.toPx()),
                                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                            )
                        }
                    }
                    CassetteSpineSkinMotif.DarkFrame -> {
                        drawRoundRect(
                            color = spinePalette.border.copy(alpha = 0.22f),
                            topLeft = Offset(labelLeft + 3.dp.toPx(), labelTop + 3.dp.toPx()),
                            size = Size(labelRight - labelLeft - 6.dp.toPx(), labelHeight - 6.dp.toPx()),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                            style = Stroke(width = 2.dp.toPx()),
                        )
                        drawCircle(
                            color = spinePalette.accent.copy(alpha = 0.72f),
                            radius = 2.2.dp.toPx(),
                            center = Offset(labelRight - 16.dp.toPx(), labelTop + 13.dp.toPx()),
                        )
                        drawCircle(
                            color = spinePalette.strip.copy(alpha = 0.72f),
                            radius = 2.2.dp.toPx(),
                            center = Offset(labelRight - 16.dp.toPx(), labelTop + labelHeight - 13.dp.toPx()),
                        )
                    }
                }
                listOf(0f, size.width - endCapWidth).forEach { endCapLeft ->
                    drawRoundRect(
                        color = spinePalette.endCap.copy(alpha = 0.92f),
                        topLeft = Offset(endCapLeft, spineTop),
                        size = Size(endCapWidth, spineHeight),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                    )
                    drawLine(
                        color = spinePalette.border.copy(alpha = 0.72f),
                        start = Offset(endCapLeft + endCapWidth * 0.55f, spineTop + 7.dp.toPx()),
                        end = Offset(endCapLeft + endCapWidth * 0.55f, spineBottom - 7.dp.toPx()),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
                listOf(
                    Offset(endCapWidth * 0.45f, spineTop + spineHeight * 0.24f),
                    Offset(size.width - endCapWidth * 0.45f, spineTop + spineHeight * 0.76f),
                ).forEach { screw ->
                    drawCircle(color = spinePalette.border.copy(alpha = 0.82f), radius = 3.4.dp.toPx(), center = screw)
                    drawCircle(color = spinePalette.label.copy(alpha = 0.84f), radius = 1.2.dp.toPx(), center = screw)
                }
                drawRoundRect(
                    color = spinePalette.accent.copy(alpha = 0.42f),
                    topLeft = Offset(size.width - endCapWidth - notchWidth, spineTop + spineHeight * 0.36f),
                    size = Size(notchWidth, spineHeight * 0.28f),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                )
                drawRoundRect(
                    color = group.visualProperties.caseTheme.tintColor(),
                    topLeft = Offset(0f, spineTop),
                    size = spineSize,
                    cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(horizontal = 40.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HandDrawnEmbellishment(
                    embellishment = group.visualProperties.embellishment,
                    modifier = Modifier.size(44.dp),
                    color = group.visualProperties.symbolColor.toComposeColor(),
                )
                Text(
                    text = group.visualProperties.decorativeId,
                    color = nameInk,
                    fontWeight = FontWeight.Bold,
                )
                JitteredHandwritingText(
                    text = group.name,
                    modifier = Modifier
                        .weight(1f)
                        .offset(y = handwritingFont.cassetteSpineVerticalOffset()),
                    startIndex = jitterStartIndex,
                    color = nameInk,
                    fontFamily = cassetteHandwritingFontFamily,
                    fontStyle = FontStyle.Italic,
                    fontWeight = handwritingFont.effectiveCassetteWeight(FontWeight.ExtraBold),
                    fontSize = 36.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    tokenization = HandwritingJitterTokenization.Character,
                    strength = HandwritingJitterStrength(maxDxEm = 0.054f, maxDyEm = 0.105f, maxRotationDegrees = 3.0f, maxTrackingEm = 0.023f),
                )
            }
        }
    }
}

@Composable
private fun NowPlayingModeToggleAndSpine(
    state: MixtapeUiState,
    bodyMode: NowPlayingBodyMode,
    onBodyModeChange: (NowPlayingBodyMode) -> Unit,
    onCustomize: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentGroup = state.mixTapeGroups.getOrNull(state.currentMixtapeIndex)
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val fullSpineWidth = maxWidth
        val regularMixtapeSpineWidth = (fullSpineWidth - 20.dp).coerceAtLeast(0.dp)
        val currentSpineWidth = (fullSpineWidth - 64.dp - 8.dp).coerceAtLeast(0.dp)
        val currentTrackPreviewScale = if (fullSpineWidth.value > 0f) {
            (currentSpineWidth.value / fullSpineWidth.value).coerceIn(0.5f, 1f)
        } else {
            1f
        }
        val currentMixtapeSpineScale = if (regularMixtapeSpineWidth.value > 0f) {
            (currentSpineWidth.value / regularMixtapeSpineWidth.value).coerceIn(0.5f, 1f)
        } else {
            1f
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (bodyMode) {
                NowPlayingBodyMode.Tracks -> {
                    if (currentGroup != null) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(CASSETTE_CASE_SPINE_ASPECT_RATIO)
                                .clipToBounds(),
                            contentAlignment = Alignment.TopStart,
                        ) {
                            CassetteSpineRow(
                                group = currentGroup,
                                sideNumber = state.currentMixtapeIndex + 1,
                                isCurrentMixtape = true,
                                onClick = {},
                                onLongClick = onCustomize,
                                modifier = Modifier
                                    .wrapContentSize(Alignment.TopStart, unbounded = true)
                                    .requiredWidth(regularMixtapeSpineWidth)
                                    .graphicsLayer {
                                        scaleX = currentMixtapeSpineScale
                                        scaleY = currentMixtapeSpineScale
                                        transformOrigin = TransformOrigin(0f, 0f)
                                    },
                            )
                        }
                    }
                }
                NowPlayingBodyMode.MixTapes -> CurrentTrackPreview(
                    currentTrack = state.currentTrack,
                    tracks = state.queueTracks,
                    currentIndex = state.currentIndex,
                    handwritingFont = state.currentMixtapeHandwritingFont,
                    mixtapeJitterStartIndex = nowPlayingJitterStartIndex(state),
                    sleeveTheme = state.currentMixtapeVisualProperties.sleeveTheme,
                    caseTheme = state.currentMixtapeVisualProperties.caseTheme,
                    sourceWidth = fullSpineWidth,
                    previewScale = currentTrackPreviewScale,
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(CASSETTE_CASE_SPINE_ASPECT_RATIO),
                )
            }
            DeckViewToggle(
                deckTheme = state.mixtapeThemeSettings.deckTheme,
                bodyMode = bodyMode,
                onClick = {
                    onBodyModeChange(
                        if (bodyMode == NowPlayingBodyMode.Tracks) NowPlayingBodyMode.MixTapes else NowPlayingBodyMode.Tracks,
                    )
                },
            )
        }
    }
}

@Composable
private fun DeckViewToggle(
    deckTheme: DeckTheme,
    bodyMode: NowPlayingBodyMode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = deckTheme.deckPalette()
    Canvas(
        modifier = modifier
            .size(64.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(palette.body)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = if (bodyMode == NowPlayingBodyMode.Tracks) {
                    "Show mix tape list"
                } else {
                    "Show current track list"
                }
            }
            .padding(10.dp),
    ) {
        val stroke = 4.dp.toPx()
        fun arrow(start: Offset, end: Offset, color: Color, reverseHead: Boolean) {
            drawLine(color, start, end, strokeWidth = stroke, cap = StrokeCap.Round)
            val head = if (reverseHead) start else end
            val direction = if (reverseHead) -1f else 1f
            drawLine(
                color,
                head,
                Offset(head.x - direction * size.width * 0.17f, head.y),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            drawLine(
                color,
                head,
                Offset(head.x, head.y + direction * size.height * 0.17f),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
        arrow(
            start = Offset(size.width * 0.14f, size.height * 0.76f),
            end = Offset(size.width * 0.76f, size.height * 0.14f),
            color = palette.accent,
            reverseHead = false,
        )
        arrow(
            start = Offset(size.width * 0.24f, size.height * 0.86f),
            end = Offset(size.width * 0.86f, size.height * 0.24f),
            color = palette.button,
            reverseHead = true,
        )
    }
}

@Composable
private fun CurrentTrackPreview(
    currentTrack: Track?,
    tracks: List<Track>,
    currentIndex: Int,
    handwritingFont: MixtapeHandwritingFont,
    mixtapeJitterStartIndex: Int,
    sleeveTheme: SleeveTheme,
    caseTheme: CaseTheme,
    sourceWidth: Dp,
    previewScale: Float,
    modifier: Modifier = Modifier,
) {
    val cassetteHandwritingFontFamily = handwritingFont.cassetteHandwritingFontFamily()
    val sleevePaper = sleeveTheme.paperPalette()
    val casePlastic = caseTheme.plasticPalette()
    val indexedTrack = tracks.getOrNull(currentIndex)
    val previewTrack = currentTrack ?: indexedTrack
    val previewIndex = when {
        indexedTrack != null && previewTrack?.id == indexedTrack.id -> currentIndex
        previewTrack != null -> tracks.indexOfFirst { it.id == previewTrack.id }
        else -> -1
    }

    fun trackRowText(index: Int, fallback: String? = null): String {
        val track = tracks.getOrNull(index) ?: return fallback.orEmpty()
        return "${track.title} — ${track.artist}  ${formatDuration(track.durationMs)}"
    }

    val previewRows = if (previewIndex >= 0) {
        listOf(
            previewIndex - 1 to trackRowText(previewIndex - 1),
            previewIndex to trackRowText(previewIndex),
            previewIndex + 1 to trackRowText(previewIndex + 1),
        )
    } else {
        listOf(
            -2 to "",
            -1 to if (tracks.isEmpty()) "No tracks queued" else "No current track",
            0 to "",
        )
    }

    Box(
        modifier = modifier
            .clipToBounds()
            .semantics { contentDescription = "Current track preview" },
        contentAlignment = Alignment.TopStart,
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = sleevePaper.base),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(3.dp, casePlastic.edge),
            modifier = Modifier
                .wrapContentSize(Alignment.TopStart, unbounded = true)
                .requiredWidth(sourceWidth)
                .aspectRatio(CASSETTE_CASE_SPINE_ASPECT_RATIO)
                .graphicsLayer {
                    scaleX = previewScale
                    scaleY = previewScale
                    transformOrigin = TransformOrigin(0f, 0f)
                },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        when (sleevePaper.pattern) {
                            SleevePaperPattern.Lined -> {
                                val lineSpacing = 49.dp.toPx()
                                var y = 8.dp.toPx()
                                while (y < size.height) {
                                    drawLine(sleevePaper.line, Offset(0f, y), Offset(size.width, y), 1.5f)
                                    y += lineSpacing
                                }
                            }
                            SleevePaperPattern.Grid -> {
                                val grid = 36.dp.toPx()
                                var x = grid
                                while (x < size.width) { drawLine(sleevePaper.line, Offset(x, 0f), Offset(x, size.height), 1f); x += grid }
                                var y = grid
                                while (y < size.height) { drawLine(sleevePaper.line, Offset(0f, y), Offset(size.width, y), 1f); y += grid }
                            }
                            SleevePaperPattern.Album, SleevePaperPattern.Blank -> Unit
                        }
                    }
                    .drawWithContent {
                        drawContent()
                        drawRoundRect(casePlastic.tint.copy(alpha = casePlastic.tint.alpha * CASE_PLASTIC_TINT_STRENGTH), size = size, cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()))
                        if (casePlastic.haze != Color.Transparent) drawRoundRect(casePlastic.haze.copy(alpha = casePlastic.haze.alpha * CASE_PLASTIC_TINT_STRENGTH), size = size, cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()))
                    },
            ) {
                previewRows.forEach { (index, rowText) ->
                    if (rowText.isNotEmpty()) {
                        val isCurrent = index == previewIndex
                        JitteredHandwritingText(
                            text = rowText,
                            startIndex = handwritingTrackStartIndex(mixtapeJitterStartIndex, index.coerceAtLeast(0)),
                            color = if (isCurrent) sleevePaper.accent else sleevePaper.ink,
                            fontFamily = cassetteHandwritingFontFamily,
                            fontSize = 40.sp,
                            fontWeight = handwritingFont.effectiveCassetteWeight(if (isCurrent) FontWeight.ExtraBold else FontWeight.Bold),
                            lineHeightScale = 0.8f,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            tokenization = HandwritingJitterTokenization.Character,
                            strength = HandwritingJitterStrength(maxDxEm = 0.036f, maxDyEm = 0.065f, maxRotationDegrees = 1.8f, maxTrackingEm = 0.008f),
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .offset(y = ((index - previewIndex) * 44).dp)
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StaticMixtapeStackPreview(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.semantics { contentDescription = "Mixtape stack preview" }) {
        // StaticMixtapeStackIconRatioException: the button is square, so draw
        // ratio-correct mini case spines inside the icon rather than stretching
        // one square cassette surface.
        val spineWidth = size.width * 0.84f
        val spineHeight = spineWidth / CASSETTE_CASE_SPINE_ASPECT_RATIO
        val stackGap = spineHeight * 0.22f
        val stackTop = (size.height - (spineHeight * 4f + stackGap * 3f)) / 2f
        repeat(4) { index ->
            val top = stackTop + index * (spineHeight + stackGap)
            val left = size.width * (0.08f + index * 0.03f)
            drawRoundRect(
                color = listOf(Color(0xFFF4E7C4), Color(0xFFDDEAF7), Color(0xFFF7D7D7), Color(0xFFE6D8F5))[index],
                topLeft = Offset(left, top),
                size = Size(spineWidth, spineHeight),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
            )
            drawLine(
                color = Color(0xFF263864),
                start = Offset(left + size.width * 0.18f, top + spineHeight * 0.5f),
                end = Offset(left + size.width * 0.76f, top + spineHeight * 0.5f),
                strokeWidth = 1.5.dp.toPx(),
            )
        }
    }
}

@Composable
private fun StaticTrackListPreview(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.semantics { contentDescription = "Track listing preview" }) {
        drawRoundRect(
            color = Color.White,
            size = size,
            cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx()),
        )
        repeat(3) { index ->
            val y = size.height * (0.25f + index * 0.22f)
            drawLine(
                color = Color(0xFFB7D7F3),
                start = Offset(size.width * 0.12f, y + size.height * 0.06f),
                end = Offset(size.width * 0.92f, y + size.height * 0.06f),
                strokeWidth = 1.dp.toPx(),
            )
            drawLine(
                color = if (index == 0) Color(0xFFD32F2F) else Color(0xFF283451),
                start = Offset(size.width * 0.18f, y),
                end = Offset(size.width * (0.78f - index * 0.08f), y),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
        drawLine(
            color = Color(0xFFE9A0A0),
            start = Offset(size.width * 0.14f, size.height * 0.08f),
            end = Offset(size.width * 0.14f, size.height * 0.92f),
            strokeWidth = 1.5.dp.toPx(),
        )
    }
}

@Composable
private fun NowPlaying(
    state: MixtapeUiState,
    mixTapeListState: LazyListState,
    mixTapeGridState: LazyGridState,
    onMixTapeGroupClick: (Int) -> Unit,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onStop: () -> Unit,
    onEject: () -> Unit,
    onDeleteTrackFromDevice: (Track) -> Unit,
    onRemoveTrackFromMixtape: (Track) -> Unit,
    onShowTrackInfo: (Track) -> Unit,
    onTrackDoubleClick: (Int, Track) -> Unit,
    onUpdateCurrentMixtapeCustomization: (MixtapeCustomization) -> Unit,
    modifier: Modifier = Modifier,
) {
    var bodyMode by rememberSaveable { mutableStateOf(NowPlayingBodyMode.Tracks) }
    var isCustomizerOpen by remember { mutableStateOf(false) }
    var lastCenteredMixtapeIndex by remember { mutableStateOf<Int?>(null) }
    val audioLevels by AudioLevelMonitor.levels.collectAsState()
    val tracksToggleLabel = "Tracks"
    val mixTapesToggleLabel = "Mix tapes"
    val showTracksContentDescription = "Show current track list"
    val showMixTapesContentDescription = "Show mix tape list"

    LaunchedEffect(bodyMode, state.currentMixtapeIndex) {
        val centeredMixtapeIndex = state.currentMixtapeIndex
        if (bodyMode == NowPlayingBodyMode.MixTapes && centeredMixtapeIndex >= 0 && lastCenteredMixtapeIndex != centeredMixtapeIndex) {
            mixTapeListState.scrollToItem((centeredMixtapeIndex - 2).coerceAtLeast(0))
            mixTapeGridState.scrollToItem((centeredMixtapeIndex - 4).coerceAtLeast(0))
            lastCenteredMixtapeIndex = centeredMixtapeIndex
        }
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
    ) {
        val isLandscape = maxWidth > maxHeight
        val nowPlayingJitterStartIndex = nowPlayingJitterStartIndex(state)
        val mixtapeProgress = mixtapePlaybackProgress(
            tracks = state.queueTracks,
            currentIndex = state.currentIndex,
            currentTrackPositionMs = state.positionMs,
        )

        if (isLandscape) {
            val leftPaneWidth = (maxWidth * 0.44f).coerceAtMost(420.dp)
            Row(
                modifier = Modifier
                    .fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(
                    modifier = Modifier
                        .width(leftPaneWidth)
                        .widthIn(max = 420.dp)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    DeckCassetteBay(
                        deckTheme = state.mixtapeThemeSettings.deckTheme,
                        decorativeId = state.currentMixtapeVisualProperties.decorativeId,
                        counterValue = mixtapeCounterValue(mixtapeProgress),
                        leftAudioLevel = audioLevels.left,
                        rightAudioLevel = audioLevels.right,
                        isPlaying = state.isPlaying,
                        hasTrack = state.currentTrack != null,
                        canGoPrevious = state.canGoPrevious,
                        canGoNext = state.canGoNext,
                        onRewind = onPrevious,
                        onPlayPause = onTogglePlayPause,
                        onFastForward = onNext,
                        onStop = onStop,
                        onEject = onEject,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        CassetteTape(
                        tapeName = tapeNameFor(state),
                        currentTrack = state.currentTrack,
                        isPlaying = state.isPlaying,
                        handwritingFont = state.currentMixtapeHandwritingFont,
                        mixtapeJitterStartIndex = nowPlayingJitterStartIndex,
                        embellishment = state.currentMixtapeVisualProperties.embellishment,
                        symbolColor = state.currentMixtapeVisualProperties.symbolColor,
                        nameColor = state.currentMixtapeVisualProperties.nameColor,
                        tapeSkin = state.currentMixtapeVisualProperties.tapeSkin,
                        decorativeId = state.currentMixtapeVisualProperties.decorativeId,
                        cassetteTheme = state.currentMixtapeVisualProperties.cassetteTheme,
                        screwTheme = state.currentMixtapeVisualProperties.screwTheme,
                        stickerTheme = state.currentMixtapeVisualProperties.stickerTheme,
                        tapeProgress = mixtapeProgress,
                        onCustomize = { isCustomizerOpen = true },
                        modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    NowPlayingModeToggleAndSpine(
                        state = state,
                        bodyMode = bodyMode,
                        onBodyModeChange = { bodyMode = it },
                        onCustomize = { isCustomizerOpen = true },
                    )
                }
                if (bodyMode == NowPlayingBodyMode.Tracks) {
                    CassetteCoverTrackList(
                        tracks = state.queueTracks,
                        currentIndex = state.currentIndex,
                        mixtapeName = tapeNameFor(state),
                        handwritingFont = state.currentMixtapeHandwritingFont,
                        mixtapeJitterStartIndex = nowPlayingJitterStartIndex,
                        sleeveTheme = state.currentMixtapeVisualProperties.sleeveTheme,
                        caseTheme = state.currentMixtapeVisualProperties.caseTheme,
                        onDeleteTrackFromDevice = onDeleteTrackFromDevice,
                        onRemoveTrackFromMixtape = onRemoveTrackFromMixtape,
                        onShowTrackInfo = onShowTrackInfo,
                        onTrackDoubleClick = onTrackDoubleClick,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        scrollContent = true,
                        showHeader = false,
                    )
                } else {
                    MixTapeBriefcaseList(
                        groups = state.mixTapeGroups,
                        mixTapeListState = mixTapeListState,
                        mixTapeGridState = mixTapeGridState,
                        onMixTapeGroupClick = onMixTapeGroupClick,
                        currentMixtapeIndex = state.currentMixtapeIndex,
                        forceSingleColumn = true,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                DeckCassetteBay(
                    deckTheme = state.mixtapeThemeSettings.deckTheme,
                    decorativeId = state.currentMixtapeVisualProperties.decorativeId,
                    counterValue = mixtapeCounterValue(mixtapeProgress),
                    leftAudioLevel = audioLevels.left,
                    rightAudioLevel = audioLevels.right,
                    isPlaying = state.isPlaying,
                    hasTrack = state.currentTrack != null,
                    canGoPrevious = state.canGoPrevious,
                    canGoNext = state.canGoNext,
                    onRewind = onPrevious,
                    onPlayPause = onTogglePlayPause,
                    onFastForward = onNext,
                    onStop = onStop,
                    onEject = onEject,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    CassetteTape(
                    tapeName = tapeNameFor(state),
                    currentTrack = state.currentTrack,
                    isPlaying = state.isPlaying,
                    handwritingFont = state.currentMixtapeHandwritingFont,
                    mixtapeJitterStartIndex = nowPlayingJitterStartIndex,
                    embellishment = state.currentMixtapeVisualProperties.embellishment,
                    symbolColor = state.currentMixtapeVisualProperties.symbolColor,
                    nameColor = state.currentMixtapeVisualProperties.nameColor,
                    tapeSkin = state.currentMixtapeVisualProperties.tapeSkin,
                    decorativeId = state.currentMixtapeVisualProperties.decorativeId,
                    cassetteTheme = state.currentMixtapeVisualProperties.cassetteTheme,
                    screwTheme = state.currentMixtapeVisualProperties.screwTheme,
                    stickerTheme = state.currentMixtapeVisualProperties.stickerTheme,
                    tapeProgress = mixtapeProgress,
                    onCustomize = { isCustomizerOpen = true },
                    modifier = Modifier.fillMaxWidth(),
                    )
                }
                NowPlayingModeToggleAndSpine(
                    state = state,
                    bodyMode = bodyMode,
                    onBodyModeChange = { bodyMode = it },
                    onCustomize = { isCustomizerOpen = true },
                )
                if (bodyMode == NowPlayingBodyMode.Tracks) {
                    CassetteCoverTrackList(
                        tracks = state.queueTracks,
                        currentIndex = state.currentIndex,
                        mixtapeName = tapeNameFor(state),
                        handwritingFont = state.currentMixtapeHandwritingFont,
                        mixtapeJitterStartIndex = nowPlayingJitterStartIndex,
                        sleeveTheme = state.currentMixtapeVisualProperties.sleeveTheme,
                        caseTheme = state.currentMixtapeVisualProperties.caseTheme,
                        onDeleteTrackFromDevice = onDeleteTrackFromDevice,
                        onRemoveTrackFromMixtape = onRemoveTrackFromMixtape,
                        onShowTrackInfo = onShowTrackInfo,
                        onTrackDoubleClick = onTrackDoubleClick,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        scrollContent = true,
                        showHeader = false,
                    )
                } else {
                    MixTapeBriefcaseList(
                        groups = state.mixTapeGroups,
                        mixTapeListState = mixTapeListState,
                        mixTapeGridState = mixTapeGridState,
                        onMixTapeGroupClick = onMixTapeGroupClick,
                        currentMixtapeIndex = state.currentMixtapeIndex,
                        forceSingleColumn = true,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    )
                }
            }
        }
    }

    if (isCustomizerOpen) {
        MixtapeCustomizationDialog(
            name = tapeNameFor(state),
            properties = state.currentMixtapeVisualProperties,
            settings = state.mixtapeThemeSettings,
            enabledEmbellishments = state.enabledMixtapeEmbellishments,
            enabledFonts = state.enabledMixtapeHandwritingFonts,
            onDismiss = { isCustomizerOpen = false },
            onSave = {
                onUpdateCurrentMixtapeCustomization(it)
                isCustomizerOpen = false
            },
        )
    }
}

@Composable
private fun MixtapeCustomizationDialog(
    name: String,
    properties: MixtapeVisualProperties,
    settings: MixtapeThemeSettings,
    enabledEmbellishments: Set<MixtapeEmbellishment>,
    enabledFonts: Set<MixtapeHandwritingFont>,
    onDismiss: () -> Unit,
    onSave: (MixtapeCustomization) -> Unit,
) {
    var editedName by remember(name) { mutableStateOf(name) }
    var decorativeId by remember(properties) { mutableStateOf(properties.decorativeId) }
    var font by remember(properties) { mutableStateOf(properties.handwritingFont) }
    var symbol by remember(properties) { mutableStateOf(properties.embellishment) }
    var symbolColor by remember(properties) { mutableStateOf(properties.symbolColor) }
    var nameColor by remember(properties) { mutableStateOf(properties.nameColor) }
    var cassette by remember(properties) { mutableStateOf(properties.cassetteTheme) }
    var screws by remember(properties) { mutableStateOf(properties.screwTheme) }
    var sticker by remember(properties) { mutableStateOf(properties.stickerTheme) }
    var caseTheme by remember(properties) { mutableStateOf(properties.caseTheme) }
    var sleeve by remember(properties) { mutableStateOf(properties.sleeveTheme) }

    val fonts = enabledFonts.ifEmpty { MixtapeHandwritingFont.entries.toSet() }.toList()
    val symbols = enabledEmbellishments.ifEmpty { MixtapeEmbellishment.entries.toSet() }.toList()
    fun <T> next(options: List<T>, current: T): T = options[(options.indexOf(current).takeIf { it >= 0 } ?: -1).plus(1) % options.size]

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Customize mixtape") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(value = editedName, onValueChange = { editedName = it }, label = { Text("Name") })
                OutlinedTextField(
                    value = decorativeId,
                    onValueChange = { decorativeId = it.take(2) },
                    label = { Text("Decorative ID") },
                    singleLine = true,
                )
                CustomizationChoice("Font", font.name) { font = next(fonts, font) }
                CustomizationChoice("Symbol", symbol.readableName()) { symbol = next(symbols, symbol) }
                CustomizationChoice("Symbol color", symbolColor.name) { symbolColor = next(MixtapeSymbolColor.entries, symbolColor) }
                CustomizationChoice("Name color", nameColor.name) { nameColor = next(MixtapeSymbolColor.entries, nameColor) }
                CustomizationChoice("Cassette", cassette.readableName()) { cassette = next(settings.enabledCassetteThemes.toList(), cassette) }
                CustomizationChoice("Screws", screws.readableName()) { screws = next(settings.enabledScrewThemes.toList(), screws) }
                CustomizationChoice("Sticker", sticker.readableName()) { sticker = next(settings.enabledStickerThemes.toList(), sticker) }
                CustomizationChoice("Case", caseTheme.readableName()) { caseTheme = next(settings.enabledCaseThemes.toList(), caseTheme) }
                CustomizationChoice("Sleeve", sleeve.readableName()) { sleeve = next(settings.enabledSleeveThemes.toList(), sleeve) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    MixtapeCustomization(
                        name = editedName,
                        decorativeId = decorativeId,
                        handwritingFont = font,
                        embellishment = symbol,
                        symbolColor = symbolColor,
                        nameColor = nameColor,
                        cassetteTheme = cassette,
                        screwTheme = screws,
                        stickerTheme = sticker,
                        caseTheme = caseTheme,
                        sleeveTheme = sleeve,
                    ),
                )
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun CustomizationChoice(label: String, value: String, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(label, modifier = Modifier.weight(1f))
        OutlinedButton(onClick = onNext) { Text(value) }
    }
}

@Composable
private fun DeckCassetteBay(
    deckTheme: DeckTheme,
    decorativeId: String,
    counterValue: Int,
    leftAudioLevel: Float,
    rightAudioLevel: Float,
    isPlaying: Boolean,
    hasTrack: Boolean,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    onRewind: () -> Unit,
    onPlayPause: () -> Unit,
    onFastForward: () -> Unit,
    onStop: () -> Unit,
    onEject: () -> Unit,
    modifier: Modifier = Modifier,
    cassette: @Composable () -> Unit,
) {
    val palette = deckTheme.deckPalette()
    Card(
        colors = CardDefaults.cardColors(containerColor = palette.body),
        border = BorderStroke(2.dp, palette.outline),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .widthIn(max = 560.dp)
            .aspectRatio(DEMO_DECK_PLAYER_ASPECT_RATIO),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val deckWidth = maxWidth
            val deckHeight = maxHeight
            Canvas(modifier = Modifier.matchParentSize()) {
                drawRoundRect(
                    color = palette.panel,
                    topLeft = Offset(size.width * (18f / 560f), size.height * (18f / 400f)),
                    size = Size(size.width * (524f / 560f), size.height * (364f / 400f)),
                    cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                )
                listOf(
                    Offset(size.width * (14f / 560f), size.height * (14f / 400f)),
                    Offset(size.width * (546f / 560f), size.height * (14f / 400f)),
                    Offset(size.width * (14f / 560f), size.height * (386f / 400f)),
                    Offset(size.width * (546f / 560f), size.height * (386f / 400f)),
                ).forEach { screw -> drawCircle(Color.Black.copy(alpha = 0.28f), 2.5.dp.toPx(), screw) }
            }
            Text(
                text = deckTheme.readableName(),
                color = palette.text,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.offset(x = deckWidth * (30f / 560f), y = deckHeight * (22f / 400f)),
            )
            Box(
                modifier = Modifier
                    .offset(x = deckWidth * (46f / 560f), y = deckHeight * (54f / 400f))
                    .size(width = deckWidth * (400f / 560f), height = deckHeight * (262f / 400f))
                    .background(palette.well, RoundedCornerShape(10.dp))
                    .border(BorderStroke(2.dp, palette.wellFrame), RoundedCornerShape(10.dp)),
            )
            Box(
                modifier = Modifier
                    .offset(x = deckWidth * (56f / 560f), y = deckHeight * (64.35f / 400f))
                    .size(width = deckWidth * (380f / 560f), height = deckHeight * (241.3f / 400f)),
                contentAlignment = Alignment.Center,
            ) { cassette() }
            Row(
                modifier = Modifier
                    .offset(x = deckWidth * (471f / 560f), y = deckHeight * (88f / 400f))
                    .size(width = deckWidth * (34f / 560f), height = deckHeight * (205f / 400f)),
                horizontalArrangement = Arrangement.spacedBy(deckWidth * (6f / 560f)),
            ) {
                DeckLevelMeter("L", leftAudioLevel, isPlaying, palette, Modifier.weight(1f).fillMaxHeight())
                DeckLevelMeter("R", rightAudioLevel, isPlaying, palette, Modifier.weight(1f).fillMaxHeight())
            }
            Row(
                modifier = Modifier
                    .offset(x = deckWidth * (64f / 560f), y = deckHeight * (331f / 400f))
                    .size(width = deckWidth * (364f / 560f), height = deckHeight * (36f / 400f)),
                horizontalArrangement = Arrangement.spacedBy(deckWidth * (8f / 560f)),
            ) {
                DemoDeckButton("◀◀", "Rewind", canGoPrevious, false, palette, onRewind, Modifier.weight(1f))
                DemoDeckButton("▶", "Play", hasTrack, isPlaying, palette, onPlayPause, Modifier.weight(1f))
                DemoDeckButton("▶▶", "Fast-forward", canGoNext, false, palette, onFastForward, Modifier.weight(1f))
                DemoDeckButton("■", "Stop", hasTrack, false, palette, onStop, Modifier.weight(1f))
                DemoDeckButton("Ⅱ", "Pause", hasTrack, !isPlaying, palette, onPlayPause, Modifier.weight(1f))
                DemoDeckButton("⏏", "Eject", hasTrack, false, palette, onEject, Modifier.weight(1f))
            }
            Box(
                modifier = Modifier
                    .offset(x = deckWidth * (446f / 560f), y = deckHeight * (333f / 400f))
                    .size(width = deckWidth * (84f / 560f), height = deckHeight * (30f / 400f))
                    .background(palette.counterFace, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = counterValue.coerceIn(0, 999).toString().padStart(3, '0'),
                    color = palette.counterInk,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    letterSpacing = 2.sp,
                )
            }
        }
    }
}

@Composable
private fun DeckLevelMeter(
    label: String,
    audioLevel: Float,
    isPlaying: Boolean,
    palette: DeckPalette,
    modifier: Modifier = Modifier,
) {
    val displayedLevel by animateFloatAsState(
        targetValue = if (isPlaying) audioLevel.coerceIn(0f, 1f) else 0f,
        animationSpec = tween(durationMillis = 70, easing = LinearEasing),
        label = "$label audio level meter",
    )
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.58f), RoundedCornerShape(3.dp))
                .semantics { contentDescription = "$label audio level ${(displayedLevel * 100f).roundToInt()} percent" },
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.58f)
                    .fillMaxHeight(displayedLevel)
                    .background(palette.accent, RoundedCornerShape(2.dp)),
            )
        }
        Text(label, color = palette.text, fontSize = 7.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DemoDeckButton(
    label: String,
    description: String,
    enabled: Boolean,
    active: Boolean,
    palette: DeckPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(
                if (active) palette.accent else palette.button.copy(alpha = if (enabled) 1f else 0.42f),
                RoundedCornerShape(4.dp),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = palette.buttonInk.copy(alpha = if (enabled) 1f else 0.45f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CassetteTape(
    tapeName: String,
    currentTrack: Track?,
    isPlaying: Boolean,
    handwritingFont: MixtapeHandwritingFont,
    mixtapeJitterStartIndex: Int,
    embellishment: MixtapeEmbellishment,
    symbolColor: MixtapeSymbolColor,
    nameColor: MixtapeSymbolColor,
    tapeSkin: MixtapeTapeSkin,
    decorativeId: String = "A",
    cassetteTheme: CassetteTheme = CassetteTheme.GhostClear,
    screwTheme: ScrewTheme = ScrewTheme.Light,
    stickerTheme: StickerTheme = StickerTheme.None,
    tapeProgress: Float = 0f,
    onCustomize: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val cassetteHandwritingFontFamily = handwritingFont.cassetteHandwritingFontFamily()
    val symbolInk = symbolColor.toComposeColor()
    val nameInk = nameColor.toComposeColor()
    val palette = cassetteTheme.palette()
    val screwColor = if (screwTheme == ScrewTheme.Light) Color(0xFFD7DCE2) else Color(0xFF24282F)
    val screwRimColor = if (screwTheme == ScrewTheme.Light) Color(0xFF59606A) else Color(0xFFAEB7C3)
    val screwSlotColor = if (screwTheme == ScrewTheme.Light) Color(0xFF343A43) else Color(0xFFC8D0DB)
    val stickerShape = stickerTheme.cassetteShape()
    val animatedTapeProgress by animateFloatAsState(
        targetValue = tapeProgress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 700, easing = LinearEasing),
        label = "cassette tape pack progress",
    )
    val reelRotation = if (isPlaying) {
        val transition = rememberInfiniteTransition(label = "cassette reel spin")
        val rotation by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 16_000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "slow cassette reel rotation",
        )
        rotation
    } else {
        0f
    }

    val leftReelRotation = -reelRotation
    val rightReelRotation = -reelRotation

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .aspectRatio(CASSETTE_SHELL_ASPECT_RATIO)
                .semantics { contentDescription = "Cassette tape" },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { contentDescription = if (isPlaying) "Cassette reels spinning" else "Cassette reels stopped" },
            ) {
                val shell = Size(size.width, size.height)
                val transparentShell = cassetteTheme.isTransparentShell()
                drawRoundRect(
                    color = if (transparentShell) Color(0x1808080C) else palette.shell,
                    size = shell,
                    cornerRadius = CornerRadius(28f, 28f),
                )
                val leftReelCenter = Offset(shell.width * 0.2875f, shell.height * 0.465f)
                val rightReelCenter = Offset(shell.width * 0.7125f, shell.height * 0.465f)
                val packRadii = drawTapeTransport(
                    shell = shell,
                    leftReelCenter = leftReelCenter,
                    rightReelCenter = rightReelCenter,
                    progress = animatedTapeProgress,
                    tapeColor = palette.tapePath,
                )
                if (transparentShell) {
                    drawRoundRect(
                        color = palette.shell.copy(alpha = 0.28f),
                        size = shell,
                        cornerRadius = CornerRadius(28f, 28f),
                    )
                }
                if (stickerTheme != StickerTheme.None) {
                    drawCassetteSticker(stickerTheme, palette, shell)
                }
                val reelWindowTopLeft = Offset(shell.width * 0.3625f, shell.height * 0.36f)
                val reelWindowSize = Size(shell.width * 0.275f, shell.height * 0.205f)
                drawRoundRect(
                    color = if (transparentShell) palette.reelWell.copy(alpha = 0.16f) else palette.reelWell,
                    topLeft = reelWindowTopLeft,
                    size = reelWindowSize,
                    cornerRadius = CornerRadius(10f, 10f),
                )
                drawRoundRect(
                    color = palette.accentSecondary.copy(alpha = if (transparentShell) 0.32f else 0.7f),
                    topLeft = reelWindowTopLeft,
                    size = reelWindowSize,
                    cornerRadius = CornerRadius(10f, 10f),
                    style = Stroke(width = 1.5.dp.toPx()),
                )
                drawReel(
                    center = leftReelCenter,
                    rotation = leftReelRotation * (packRadii.maxRadius / packRadii.leftRadius),
                    palette = palette,
                )
                drawReel(
                    center = rightReelCenter,
                    rotation = rightReelRotation * (packRadii.maxRadius / packRadii.rightRadius),
                    palette = palette,
                )
                drawRoundRect(
                    color = palette.accentSecondary.copy(alpha = 0.55f),
                    topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
                    size = Size(shell.width - 4.dp.toPx(), shell.height - 4.dp.toPx()),
                    cornerRadius = CornerRadius(24f, 24f),
                    style = Stroke(width = 2.dp.toPx()),
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = if (transparentShell) 0.12f else 0.2f),
                    topLeft = Offset(6.dp.toPx(), 6.dp.toPx()),
                    size = Size(shell.width - 12.dp.toPx(), shell.height - 12.dp.toPx()),
                    cornerRadius = CornerRadius(20f, 20f),
                    style = Stroke(width = 1.dp.toPx()),
                )
                val lowerTrapezoid = Path().apply {
                    moveTo(shell.width * (70f / 400f), shell.height * (250f / 254f))
                    lineTo(shell.width * (94f / 400f), shell.height * (214f / 254f))
                    lineTo(shell.width * (306f / 400f), shell.height * (214f / 254f))
                    lineTo(shell.width * (330f / 400f), shell.height * (250f / 254f))
                }
                drawPath(lowerTrapezoid, palette.tapePath.copy(alpha = 0.7f), style = Stroke(width = 1.5.dp.toPx()))
                drawRoundRect(
                    color = palette.lowerSlot,
                    topLeft = Offset(shell.width * (168f / 400f), shell.height * (222f / 254f)),
                    size = Size(shell.width * (64f / 400f), shell.height * (22f / 254f)),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                )
                drawRoundRect(
                    color = Color(0xFF3D332B),
                    topLeft = Offset(shell.width * (188f / 400f), shell.height * (224f / 254f)),
                    size = Size(shell.width * (24f / 400f), shell.height * (8f / 254f)),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                )
                listOf(
                    Offset(shell.width * (100f / 400f), shell.height * (236f / 254f)),
                    Offset(shell.width * (128f / 400f), shell.height * (232f / 254f)),
                    Offset(shell.width * (272f / 400f), shell.height * (232f / 254f)),
                    Offset(shell.width * (300f / 400f), shell.height * (236f / 254f)),
                ).forEach { opening -> drawCircle(Color(0xFF0E0E10), 3.dp.toPx(), opening) }
                listOf(
                    Offset(shell.width * (16f / 400f), shell.height * (16f / 254f)),
                    Offset(shell.width * (384f / 400f), shell.height * (16f / 254f)),
                    Offset(shell.width * (16f / 400f), shell.height * (238f / 254f)),
                    Offset(shell.width * (384f / 400f), shell.height * (238f / 254f)),
                ).forEach { screw ->
                    val screwRadius = shell.width * (5.2f / 400f)
                    drawCircle(color = screwRimColor, radius = screwRadius * 1.12f, center = screw)
                    drawCircle(color = screwColor, radius = screwRadius, center = screw)
                    drawLine(
                        color = screwSlotColor,
                        start = screw.copy(x = screw.x - screwRadius * 0.66f),
                        end = screw.copy(x = screw.x + screwRadius * 0.66f),
                        strokeWidth = 1.3f,
                    )
                    drawLine(
                        color = screwSlotColor,
                        start = screw.copy(y = screw.y - screwRadius * 0.66f),
                        end = screw.copy(y = screw.y + screwRadius * 0.66f),
                        strokeWidth = 1.3f,
                    )
                }
            }
            if (stickerTheme != StickerTheme.None) {
                Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(
                        top = if (stickerShape == CassetteStickerShape.Full) 32.dp else 10.dp,
                        start = 16.dp,
                        end = 18.dp,
                    ),
                horizontalAlignment = Alignment.Start,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .combinedClickable(
                                onClick = {},
                                onLongClick = onCustomize,
                            )
                            .semantics { contentDescription = "Current mixtape symbol" },
                        contentAlignment = Alignment.Center,
                    ) {
                        HandDrawnEmbellishment(
                            embellishment = embellishment,
                            modifier = Modifier.size(24.dp),
                            color = symbolColor.toComposeColor(),
                        )
                    }
                    Text(
                        text = decorativeId,
                        color = nameInk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                    JitteredHandwritingText(
                        text = tapeName,
                        startIndex = mixtapeJitterStartIndex,
                        color = nameInk,
                        fontFamily = cassetteHandwritingFontFamily,
                        fontStyle = FontStyle.Italic,
                        fontSize = 13.sp,
                        fontWeight = handwritingFont.effectiveCassetteWeight(FontWeight.ExtraBold),
                        textAlign = TextAlign.Start,
                        maxLines = 2,
                        overflow = TextOverflow.Clip,
                        tokenization = HandwritingJitterTokenization.Character,
                        strength = HandwritingJitterStrength(maxDxEm = 0.068f, maxDyEm = 0.12f, maxRotationDegrees = 3.75f, maxTrackingEm = 0.03f),
                        modifier = Modifier
                            .weight(1f)
                            .semantics { contentDescription = "Current mixtape name" },
                    )
                }
                }
            }
        }
    }
}

private enum class CassetteStickerShape { Strip, Full }

private fun StickerTheme.cassetteShape(): CassetteStickerShape = when (this) {
    StickerTheme.MinimalMono, StickerTheme.VintageSunset -> CassetteStickerShape.Strip
    else -> CassetteStickerShape.Full
}

private fun StickerTheme.accentColor(): Color = when (this) {
    StickerTheme.None -> Color.Transparent
    StickerTheme.StudioStock -> Color(0xFFBF4E3B)
    StickerTheme.MinimalMono -> Color(0xFF30343A)
    StickerTheme.VintageSunset -> Color(0xFFE46636)
    StickerTheme.AquaOrbit -> Color(0xFF159B9A)
    StickerTheme.LowerDeck -> Color(0xFF233B64)
    StickerTheme.HissTachiLoNoise -> Color(0xFF54718E)
    StickerTheme.PrismC60 -> Color(0xFF8B54C7)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCassetteSticker(
    stickerTheme: StickerTheme,
    palette: CassetteTapeSkinPalette,
    shell: Size,
) {
    val sx = shell.width / 400f
    val sy = shell.height / 254f
    val left = 24f * sx
    val top = 18f * sy
    val right = 376f * sx
    val shape = stickerTheme.cassetteShape()
    val bottom = (if (shape == CassetteStickerShape.Strip) 76f else 198f) * sy
    val stickerPath = Path().apply {
        fillType = PathFillType.EvenOdd
        addRoundRect(
            RoundRect(
                Rect(left, top, right, bottom),
                CornerRadius(10f * sx, 10f * sy),
            ),
        )
        if (shape == CassetteStickerShape.Full) {
            addRoundRect(
                RoundRect(
                    Rect(146f * sx, 88f * sy, 254f * sx, 148f * sy),
                    CornerRadius(10f * sx, 10f * sy),
                ),
            )
            addOval(Rect((115f - 31f) * sx, (118f - 31f) * sy, (115f + 31f) * sx, (118f + 31f) * sy))
            addOval(Rect((285f - 31f) * sx, (118f - 31f) * sy, (285f + 31f) * sx, (118f + 31f) * sy))
        }
    }
    drawPath(stickerPath, color = stickerTheme.labelColor(palette.label))
    drawPath(
        stickerPath,
        color = stickerTheme.accentColor().copy(alpha = 0.62f),
        style = Stroke(width = 1.2.dp.toPx()),
    )
    val accent = stickerTheme.accentColor()
    if (shape == CassetteStickerShape.Full) {
        drawRect(accent, Offset(left, top), Size(right - left, 24f * sy))
        drawLine(
            color = accent.copy(alpha = 0.42f),
            start = Offset(82f * sx, 60f * sy),
            end = Offset(362f * sx, 60f * sy),
            strokeWidth = 1.dp.toPx(),
        )
        drawLine(
            color = accent.copy(alpha = 0.32f),
            start = Offset(82f * sx, 80f * sy),
            end = Offset(362f * sx, 80f * sy),
            strokeWidth = 1.dp.toPx(),
        )
    } else {
        drawRoundRect(
            color = accent,
            topLeft = Offset(left, top),
            size = Size(12f * sx, bottom - top),
            cornerRadius = CornerRadius(5f * sx, 5f * sy),
        )
        drawLine(
            color = accent.copy(alpha = 0.4f),
            start = Offset(82f * sx, 60f * sy),
            end = Offset(360f * sx, 60f * sy),
            strokeWidth = 1.dp.toPx(),
        )
    }
}

private data class CassetteTapeSkinPalette(
    val shell: Color,
    val label: Color,
    val innerLabel: Color,
    val reelWell: Color,
    val lowerSlot: Color,
    val screw: Color,
    val tapePath: Color,
    val reel: Color,
    val reelWindow: Color,
    val titleInk: Color,
    val currentTrackInk: Color,
    val accent: Color,
    val accentSecondary: Color,
)

private fun CassetteTheme.legacyTapeSkin(): MixtapeTapeSkin = when (this) {
    CassetteTheme.StudioFerric -> MixtapeTapeSkin.ClassicCreamDots
    CassetteTheme.MidnightChrome -> MixtapeTapeSkin.BlackMagentaStripe
    CassetteTheme.GhostClear -> MixtapeTapeSkin.IvoryBlue120
    CassetteTheme.TranslucentCobalt -> MixtapeTapeSkin.TealMeterDeck
    CassetteTheme.TranslucentLime -> MixtapeTapeSkin.ChromeGreen
    CassetteTheme.TranslucentRuby -> MixtapeTapeSkin.RubyFerroGrid
    CassetteTheme.TranslucentSmoke -> MixtapeTapeSkin.SmokedGreenLowNoise
    CassetteTheme.TranslucentViolet -> MixtapeTapeSkin.TranslucentViolet
    CassetteTheme.BubblegumPop -> MixtapeTapeSkin.IvoryRedStripe
}

private fun CassetteTheme.isTransparentShell(): Boolean = when (this) {
    CassetteTheme.StudioFerric, CassetteTheme.MidnightChrome -> false
    else -> true
}

private fun CassetteTheme.palette(): CassetteTapeSkinPalette {
    val base = legacyTapeSkin().palette()
    return when (this) {
        CassetteTheme.StudioFerric -> base.copy(
            shell = Color(0xFF302824), reelWell = Color(0xFF171311), lowerSlot = Color(0xFF0D0A09),
            accent = Color(0xFFC86A35), accentSecondary = Color(0xFFE0B04F),
        )
        CassetteTheme.MidnightChrome -> base.copy(
            shell = Color(0xFF151821), reelWell = Color(0xFF080A10), lowerSlot = Color(0xFF030407),
            reel = Color(0xFFD6D8DE), accent = Color(0xFF55C7CF), accentSecondary = Color(0xFFD84DA6),
        )
        CassetteTheme.GhostClear -> base.copy(
            shell = Color(0x99E9F4F7), reelWell = Color(0x554A606B), lowerSlot = Color(0x88415561),
            reelWindow = Color(0x66758D98), accent = Color(0xFF7ACADB), accentSecondary = Color(0xFFD5F5FA),
        )
        CassetteTheme.BubblegumPop -> base.copy(
            shell = Color(0xB8F28AB8), reelWell = Color(0x77733A58), lowerSlot = Color(0xAA5A263F),
            reelWindow = Color(0x887E4560), accent = Color(0xFFFFD449), accentSecondary = Color(0xFF58D5D0),
        )
        CassetteTheme.TranslucentSmoke -> base.copy(
            shell = Color(0xA85A626A), reelWell = Color(0x7740474D), lowerSlot = Color(0xAA20252A),
            reelWindow = Color(0x88646C72), accent = Color(0xFF9EA8AF), accentSecondary = Color(0xFF667078),
        )
        CassetteTheme.TranslucentRuby -> base.copy(
            shell = Color(0xB8B91F3F), reelWell = Color(0x776F1228), lowerSlot = Color(0xAA4D0A1B),
            reelWindow = Color(0x888C2036), accent = Color(0xFFFF8A63), accentSecondary = Color(0xFFFFC1A8),
        )
        CassetteTheme.TranslucentCobalt -> base.copy(
            shell = Color(0xB82D5FB5), reelWell = Color(0x77213D75), lowerSlot = Color(0xAA14274F),
            reelWindow = Color(0x88456BA7), accent = Color(0xFF5DD6E8), accentSecondary = Color(0xFFB5ECF4),
        )
        CassetteTheme.TranslucentLime -> base.copy(
            shell = Color(0xB88BBB35), reelWell = Color(0x77516E20), lowerSlot = Color(0xAA354915),
            reelWindow = Color(0x886D9034), accent = Color(0xFFE5F65A), accentSecondary = Color(0xFF70D7B4),
        )
        CassetteTheme.TranslucentViolet -> base.copy(
            shell = Color(0xB87555B2), reelWell = Color(0x77462F70), lowerSlot = Color(0xAA30204F),
            reelWindow = Color(0x886F55A0), accent = Color(0xFFD99AE9), accentSecondary = Color(0xFF79C9EF),
        )
    }
}

private fun StickerTheme.legacyAccentSkin(): MixtapeTapeSkin = when (this) {
    StickerTheme.None, StickerTheme.StudioStock -> MixtapeTapeSkin.ClassicCreamDots
    StickerTheme.MinimalMono -> MixtapeTapeSkin.BlackMagentaStripe
    StickerTheme.VintageSunset -> MixtapeTapeSkin.IvoryRedStripe
    StickerTheme.AquaOrbit -> MixtapeTapeSkin.TealMeterDeck
    StickerTheme.LowerDeck -> MixtapeTapeSkin.CharcoalGold
    StickerTheme.HissTachiLoNoise -> MixtapeTapeSkin.SmokedGreenLowNoise
    StickerTheme.PrismC60 -> MixtapeTapeSkin.TranslucentViolet
}

private fun StickerTheme.labelColor(fallback: Color): Color = when (this) {
    StickerTheme.None -> Color.Transparent
    StickerTheme.StudioStock -> fallback
    StickerTheme.MinimalMono -> Color(0xFFF4F2EA)
    StickerTheme.VintageSunset -> Color(0xFFFFC36C)
    StickerTheme.AquaOrbit -> Color(0xFFA9F0EA)
    StickerTheme.LowerDeck -> Color(0xFFE7DCC3)
    StickerTheme.HissTachiLoNoise -> Color(0xFFCFD9E8)
    StickerTheme.PrismC60 -> Color(0xFFE4D8FF)
}

private fun StickerTheme.innerLabelColor(fallback: Color): Color = when (this) {
    StickerTheme.None -> Color.Transparent
    StickerTheme.StudioStock -> fallback
    StickerTheme.MinimalMono -> Color(0xFFE3E0D7)
    StickerTheme.VintageSunset -> Color(0xFFFFE09A)
    StickerTheme.AquaOrbit -> Color(0xFFD4FFF9)
    StickerTheme.LowerDeck -> Color(0xFFFAF2D9)
    StickerTheme.HissTachiLoNoise -> Color(0xFFE8EEF7)
    StickerTheme.PrismC60 -> Color(0xFFF4EFFF)
}

private fun SleeveTheme.trackListColor(): Color = when (this) {
    SleeveTheme.BlankWhite -> Color(0xFFFFFEFA)
    SleeveTheme.BlueprintGrid -> Color(0xFFDCEBFA)
    SleeveTheme.CoralAlbum -> Color(0xFFFFDDD6)
    SleeveTheme.ForestFleck -> Color(0xFFE2E7D2)
    SleeveTheme.GraphPaper -> Color(0xFFF3F7FB)
    SleeveTheme.KraftBrown -> Color(0xFFE6C99C)
    SleeveTheme.MidnightGrid -> Color(0xFF29344B)
    SleeveTheme.RuledNotebook -> Color(0xFFFFFBE9)
    SleeveTheme.AlbumPrint -> Color(0xFFFFE4A3)
}

private fun CaseTheme.borderColor(): Color = when (this) {
    CaseTheme.CrystalClear -> Color(0x557A8996)
    else -> tintColor().copy(alpha = 0.5f)
}

private fun CaseTheme.tintColor(): Color = when (this) {
    CaseTheme.CrystalClear -> Color.Transparent
    CaseTheme.CloudyClear -> Color(0x22FFFFFF)
    CaseTheme.SmokeTint -> Color(0x22304050)
    CaseTheme.AmberTint -> Color(0x22D88924)
    CaseTheme.AcidGreenClear -> Color(0x223CFF62)
    CaseTheme.ElectricBlueClear -> Color(0x223077FF)
    CaseTheme.HotPinkClear -> Color(0x22FF3999)
    CaseTheme.RubyClear -> Color(0x22D01435)
    CaseTheme.VioletClear -> Color(0x226C3FE8)
}

private fun MixtapeTapeSkin.palette(): CassetteTapeSkinPalette = when (this) {
    MixtapeTapeSkin.ClassicCreamDots -> CassetteTapeSkinPalette(
        shell = Color(0xFF24201E),
        label = Color(0xFFE8D9B7),
        innerLabel = Color(0xFFF6EACF),
        reelWell = Color(0xFF3A332F),
        lowerSlot = Color(0xFF171413),
        screw = Color(0xFF6B625C),
        tapePath = Color(0xFF151312),
        reel = Color(0xFFDED4C8),
        reelWindow = Color(0xFF7C6250),
        titleInk = Color(0xFF263864),
        currentTrackInk = Color(0xFF574739),
        accent = Color(0xFFE85F2A),
        accentSecondary = Color(0xFFF2C34D),
    )
    MixtapeTapeSkin.BlackMagentaStripe -> CassetteTapeSkinPalette(
        shell = Color(0xFF121214),
        label = Color(0xFFEFECE4),
        innerLabel = Color(0xFFFFFFFF),
        reelWell = Color(0xFF2B2D34),
        lowerSlot = Color(0xFF060608),
        screw = Color(0xFF8E8B85),
        tapePath = Color(0xFF070708),
        reel = Color(0xFFC9C4BA),
        reelWindow = Color(0xFF4B4149),
        titleInk = Color(0xFF1F3772),
        currentTrackInk = Color(0xFF52324A),
        accent = Color(0xFFD13F96),
        accentSecondary = Color(0xFF6D46A3),
    )
    MixtapeTapeSkin.ChromeGreen -> CassetteTapeSkinPalette(
        shell = Color(0xFFF5F5EF),
        label = Color(0xFFE8E8DE),
        innerLabel = Color(0xFFFFFFFF),
        reelWell = Color(0xFF1E211F),
        lowerSlot = Color(0xFF0E1110),
        screw = Color(0xFF606761),
        tapePath = Color(0xFF141615),
        reel = Color(0xFFD9D7CD),
        reelWindow = Color(0xFF6E746B),
        titleInk = Color(0xFF173A63),
        currentTrackInk = Color(0xFF25583B),
        accent = Color(0xFF3B9A62),
        accentSecondary = Color(0xFF99D05C),
    )
    MixtapeTapeSkin.CharcoalGold -> CassetteTapeSkinPalette(
        shell = Color(0xFF2C2B2A),
        label = Color(0xFFE4D7B5),
        innerLabel = Color(0xFFF7ECCF),
        reelWell = Color(0xFF1B1918),
        lowerSlot = Color(0xFF100F0E),
        screw = Color(0xFFB28C48),
        tapePath = Color(0xFF090807),
        reel = Color(0xFFC8B48B),
        reelWindow = Color(0xFF5D4B2A),
        titleInk = Color(0xFF263864),
        currentTrackInk = Color(0xFF5F401C),
        accent = Color(0xFFD3A13B),
        accentSecondary = Color(0xFF8A6124),
    )
    MixtapeTapeSkin.TranslucentViolet -> CassetteTapeSkinPalette(
        shell = Color(0xFFD9D7F0),
        label = Color(0xFFEDEAF9),
        innerLabel = Color(0xFFFFFFFF),
        reelWell = Color(0x806C66A8),
        lowerSlot = Color(0xFF4B4777),
        screw = Color(0xFF66609A),
        tapePath = Color(0xFF2F2B50),
        reel = Color(0xFFD4D1EA),
        reelWindow = Color(0xFF7A72B4),
        titleInk = Color(0xFF213265),
        currentTrackInk = Color(0xFF4F4179),
        accent = Color(0xFF6A7FD1),
        accentSecondary = Color(0xFFC381C8),
    )
    MixtapeTapeSkin.SmokedGreenLowNoise -> CassetteTapeSkinPalette(
        shell = Color(0xFF51483F),
        label = Color(0xFFEADCC1),
        innerLabel = Color(0xFFF9EBD0),
        reelWell = Color(0x805B5248),
        lowerSlot = Color(0xFF201B17),
        screw = Color(0xFF9B8D7E),
        tapePath = Color(0xFF17120F),
        reel = Color(0xFFD8C6AA),
        reelWindow = Color(0xFF6D5D4D),
        titleInk = Color(0xFF243A33),
        currentTrackInk = Color(0xFF3F372D),
        accent = Color(0xFF79B35A),
        accentSecondary = Color(0xFFB7C66C),
    )
    MixtapeTapeSkin.TealMeterDeck -> CassetteTapeSkinPalette(
        shell = Color(0xFF0E4E4D),
        label = Color(0xFFD9E3D5),
        innerLabel = Color(0xFFF1F4E4),
        reelWell = Color(0xFF0A2F33),
        lowerSlot = Color(0xFF082328),
        screw = Color(0xFF8AB3A6),
        tapePath = Color(0xFF04181B),
        reel = Color(0xFFC7D7C9),
        reelWindow = Color(0xFF225B5A),
        titleInk = Color(0xFF15335A),
        currentTrackInk = Color(0xFF173D35),
        accent = Color(0xFF16363D),
        accentSecondary = Color(0xFFE0B84C),
    )
    MixtapeTapeSkin.IvoryRedStripe -> CassetteTapeSkinPalette(
        shell = Color(0xFFF3E6C9),
        label = Color(0xFFE8D4AE),
        innerLabel = Color(0xFFFFF5D8),
        reelWell = Color(0xFF3A3028),
        lowerSlot = Color(0xFF211915),
        screw = Color(0xFF816B54),
        tapePath = Color(0xFF15100D),
        reel = Color(0xFFD8C6A6),
        reelWindow = Color(0xFF7B6652),
        titleInk = Color(0xFF23375E),
        currentTrackInk = Color(0xFF562A25),
        accent = Color(0xFFC53B32),
        accentSecondary = Color(0xFF1F1E1C),
    )
    MixtapeTapeSkin.IvoryBlue120 -> CassetteTapeSkinPalette(
        shell = Color(0xFFF0E9D3),
        label = Color(0xFFE3D7BC),
        innerLabel = Color(0xFFFFF8DF),
        reelWell = Color(0xFF28364D),
        lowerSlot = Color(0xFF152034),
        screw = Color(0xFF77715F),
        tapePath = Color(0xFF101723),
        reel = Color(0xFFD5CCB5),
        reelWindow = Color(0xFF5C6680),
        titleInk = Color(0xFF1E3B67),
        currentTrackInk = Color(0xFF38405C),
        accent = Color(0xFF225B95),
        accentSecondary = Color(0xFFE0B240),
    )
    MixtapeTapeSkin.RubyFerroGrid -> CassetteTapeSkinPalette(
        shell = Color(0xFFC83D39),
        label = Color(0xFFF0D9B5),
        innerLabel = Color(0xFFFFE7C4),
        reelWell = Color(0xFF7D2528),
        lowerSlot = Color(0xFF4A1318),
        screw = Color(0xFFFFC47B),
        tapePath = Color(0xFF2E080D),
        reel = Color(0xFFE3C0A3),
        reelWindow = Color(0xFF9B3435),
        titleInk = Color(0xFF273862),
        currentTrackInk = Color(0xFF5A241B),
        accent = Color(0xFFFF7B4A),
        accentSecondary = Color(0xFF7D171D),
    )
    MixtapeTapeSkin.MidnightCreamSuper -> CassetteTapeSkinPalette(
        shell = Color(0xFF101522),
        label = Color(0xFFE8D5A8),
        innerLabel = Color(0xFFF8E8BD),
        reelWell = Color(0xFF1F2634),
        lowerSlot = Color(0xFF05080F),
        screw = Color(0xFFA48758),
        tapePath = Color(0xFF05070C),
        reel = Color(0xFFD6C49D),
        reelWindow = Color(0xFF424550),
        titleInk = Color(0xFF22345F),
        currentTrackInk = Color(0xFF4E3A1F),
        accent = Color(0xFFD65C35),
        accentSecondary = Color(0xFF6D7382),
    )
    MixtapeTapeSkin.LimeNavyC30 -> CassetteTapeSkinPalette(
        shell = Color(0xFFB4CF43),
        label = Color(0xFF1C3557),
        innerLabel = Color(0xFFF8F2D7),
        reelWell = Color(0xFF233A50),
        lowerSlot = Color(0xFF15243A),
        screw = Color(0xFFEEF1A7),
        tapePath = Color(0xFF0D1826),
        reel = Color(0xFFDDE9A0),
        reelWindow = Color(0xFF57794B),
        titleInk = Color(0xFF102B52),
        currentTrackInk = Color(0xFF33451E),
        accent = Color(0xFF152D56),
        accentSecondary = Color(0xFFFFFFFF),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSkinAccents(
    tapeSkin: MixtapeTapeSkin,
    palette: CassetteTapeSkinPalette,
    shell: Size,
) {
    when (tapeSkin) {
        MixtapeTapeSkin.ClassicCreamDots -> {
            listOf(palette.accent, palette.accentSecondary, Color(0xFFD23B2D)).forEachIndexed { index, color ->
                drawCircle(
                    color = color,
                    radius = shell.width * 0.016f,
                    center = Offset(shell.width * (0.19f + index * 0.055f), shell.height * 0.43f),
                )
            }
        }
        MixtapeTapeSkin.BlackMagentaStripe -> {
            drawRoundRect(
                color = palette.accent,
                topLeft = Offset(shell.width * 0.09f, shell.height * 0.40f),
                size = Size(shell.width * 0.82f, shell.height * 0.055f),
                cornerRadius = CornerRadius(8f, 8f),
            )
            drawRoundRect(
                color = palette.accentSecondary,
                topLeft = Offset(shell.width * 0.09f, shell.height * 0.455f),
                size = Size(shell.width * 0.82f, shell.height * 0.028f),
                cornerRadius = CornerRadius(6f, 6f),
            )
        }
        MixtapeTapeSkin.ChromeGreen -> {
            drawRoundRect(
                color = palette.accent,
                topLeft = Offset(shell.width * 0.08f, shell.height * 0.405f),
                size = Size(shell.width * 0.84f, shell.height * 0.05f),
                cornerRadius = CornerRadius(5f, 5f),
            )
            repeat(4) { tick ->
                drawRoundRect(
                    color = palette.accentSecondary,
                    topLeft = Offset(shell.width * (0.68f + tick * 0.035f), shell.height * 0.18f),
                    size = Size(shell.width * 0.018f, shell.height * 0.12f),
                    cornerRadius = CornerRadius(3f, 3f),
                )
            }
        }
        MixtapeTapeSkin.CharcoalGold -> {
            drawRoundRect(
                color = palette.accent,
                topLeft = Offset(shell.width * 0.06f, shell.height * 0.39f),
                size = Size(shell.width * 0.88f, shell.height * 0.08f),
                cornerRadius = CornerRadius(10f, 10f),
            )
            drawLine(
                color = palette.accentSecondary,
                start = Offset(shell.width * 0.12f, shell.height * 0.525f),
                end = Offset(shell.width * 0.88f, shell.height * 0.525f),
                strokeWidth = 3f,
            )
        }
        MixtapeTapeSkin.TranslucentViolet -> {
            drawRoundRect(
                color = palette.accent.copy(alpha = 0.42f),
                topLeft = Offset(shell.width * 0.05f, shell.height * 0.58f),
                size = Size(shell.width * 0.9f, shell.height * 0.2f),
                cornerRadius = CornerRadius(18f, 18f),
            )
            drawRoundRect(
                color = palette.accentSecondary.copy(alpha = 0.55f),
                topLeft = Offset(shell.width * 0.78f, shell.height * 0.12f),
                size = Size(shell.width * 0.11f, shell.height * 0.28f),
                cornerRadius = CornerRadius(12f, 12f),
            )
        }
        MixtapeTapeSkin.SmokedGreenLowNoise -> {
            drawRoundRect(
                color = palette.accent.copy(alpha = 0.75f),
                topLeft = Offset(shell.width * 0.07f, shell.height * 0.405f),
                size = Size(shell.width * 0.86f, shell.height * 0.075f),
                cornerRadius = CornerRadius(9f, 9f),
            )
            drawRoundRect(
                color = palette.accentSecondary.copy(alpha = 0.48f),
                topLeft = Offset(shell.width * 0.06f, shell.height * 0.56f),
                size = Size(shell.width * 0.88f, shell.height * 0.21f),
                cornerRadius = CornerRadius(18f, 18f),
            )
        }
        MixtapeTapeSkin.TealMeterDeck -> {
            drawRoundRect(
                color = palette.accent,
                topLeft = Offset(shell.width * 0.34f, shell.height * 0.18f),
                size = Size(shell.width * 0.32f, shell.height * 0.13f),
                cornerRadius = CornerRadius(6f, 6f),
            )
            repeat(7) { tick ->
                drawRoundRect(
                    color = palette.accentSecondary,
                    topLeft = Offset(shell.width * (0.38f + tick * 0.04f), shell.height * (0.21f + (tick % 2) * 0.018f)),
                    size = Size(shell.width * 0.012f, shell.height * 0.06f),
                    cornerRadius = CornerRadius(2f, 2f),
                )
            }
        }
        MixtapeTapeSkin.IvoryRedStripe -> {
            drawRoundRect(
                color = palette.accentSecondary,
                topLeft = Offset(shell.width * 0.07f, shell.height * 0.398f),
                size = Size(shell.width * 0.86f, shell.height * 0.035f),
                cornerRadius = CornerRadius(5f, 5f),
            )
            drawRoundRect(
                color = palette.accent,
                topLeft = Offset(shell.width * 0.07f, shell.height * 0.435f),
                size = Size(shell.width * 0.86f, shell.height * 0.055f),
                cornerRadius = CornerRadius(5f, 5f),
            )
        }
        MixtapeTapeSkin.IvoryBlue120 -> {
            val lowerChevron = Path().apply {
                moveTo(shell.width * 0.08f, shell.height * 0.80f)
                lineTo(shell.width * 0.34f, shell.height * 0.67f)
                lineTo(shell.width * 0.66f, shell.height * 0.67f)
                lineTo(shell.width * 0.92f, shell.height * 0.80f)
                close()
            }
            drawPath(path = lowerChevron, color = palette.accent.copy(alpha = 0.82f))
            drawLine(
                color = palette.accentSecondary,
                start = Offset(shell.width * 0.14f, shell.height * 0.43f),
                end = Offset(shell.width * 0.86f, shell.height * 0.43f),
                strokeWidth = 5f,
            )
        }
        MixtapeTapeSkin.RubyFerroGrid -> {
            drawRoundRect(
                color = palette.accent.copy(alpha = 0.65f),
                topLeft = Offset(shell.width * 0.08f, shell.height * 0.39f),
                size = Size(shell.width * 0.84f, shell.height * 0.06f),
                cornerRadius = CornerRadius(8f, 8f),
            )
            repeat(6) { index ->
                drawLine(
                    color = palette.accentSecondary.copy(alpha = 0.44f),
                    start = Offset(shell.width * (0.2f + index * 0.12f), shell.height * 0.50f),
                    end = Offset(shell.width * (0.2f + index * 0.12f), shell.height * 0.77f),
                    strokeWidth = 3f,
                )
            }
        }
        MixtapeTapeSkin.MidnightCreamSuper -> {
            drawRoundRect(
                color = palette.accentSecondary.copy(alpha = 0.42f),
                topLeft = Offset(shell.width * 0.08f, shell.height * 0.40f),
                size = Size(shell.width * 0.84f, shell.height * 0.035f),
                cornerRadius = CornerRadius(4f, 4f),
            )
            drawCircle(
                color = palette.accent,
                radius = shell.width * 0.022f,
                center = Offset(shell.width * 0.84f, shell.height * 0.24f),
            )
            drawRoundRect(
                color = palette.accent.copy(alpha = 0.58f),
                topLeft = Offset(shell.width * 0.09f, shell.height * 0.79f),
                size = Size(shell.width * 0.82f, shell.height * 0.035f),
                cornerRadius = CornerRadius(5f, 5f),
            )
        }
        MixtapeTapeSkin.LimeNavyC30 -> {
            drawRoundRect(
                color = palette.accent,
                topLeft = Offset(shell.width * 0.09f, shell.height * 0.18f),
                size = Size(shell.width * 0.18f, shell.height * 0.15f),
                cornerRadius = CornerRadius(6f, 6f),
            )
            drawRoundRect(
                color = palette.accent,
                topLeft = Offset(shell.width * 0.73f, shell.height * 0.18f),
                size = Size(shell.width * 0.18f, shell.height * 0.15f),
                cornerRadius = CornerRadius(6f, 6f),
            )
            drawRoundRect(
                color = palette.accentSecondary.copy(alpha = 0.75f),
                topLeft = Offset(shell.width * 0.28f, shell.height * 0.405f),
                size = Size(shell.width * 0.44f, shell.height * 0.045f),
                cornerRadius = CornerRadius(6f, 6f),
            )
        }
    }
}

private data class TapePackRadii(
    val leftRadius: Float,
    val rightRadius: Float,
    val maxRadius: Float,
)

private data class CircleTangent(val first: Offset, val second: Offset)

private fun externalCircleTangent(
    firstCenter: Offset,
    firstRadius: Float,
    secondCenter: Offset,
    secondRadius: Float,
    side: Float,
): CircleTangent {
    val dx = secondCenter.x - firstCenter.x
    val dy = secondCenter.y - firstCenter.y
    val distance = sqrt(dx * dx + dy * dy).coerceAtLeast(0.001f)
    val vx = dx / distance
    val vy = dy / distance
    val radiusRatio = ((firstRadius - secondRadius) / distance).coerceIn(-1f, 1f)
    val tangentRatio = sqrt((1f - radiusRatio * radiusRatio).coerceAtLeast(0f))
    val normalX = vx * radiusRatio - side * tangentRatio * vy
    val normalY = vy * radiusRatio + side * tangentRatio * vx
    return CircleTangent(
        first = Offset(firstCenter.x + firstRadius * normalX, firstCenter.y + firstRadius * normalY),
        second = Offset(secondCenter.x + secondRadius * normalX, secondCenter.y + secondRadius * normalY),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTapeTransport(
    shell: Size,
    leftReelCenter: Offset,
    rightReelCenter: Offset,
    progress: Float,
    tapeColor: Color,
): TapePackRadii {
    val minRadius = shell.height * (30f / 254f)
    val maxRadius = shell.height * (57f / 254f)
    val tapeArea = maxRadius * maxRadius - minRadius * minRadius
    val clampedProgress = progress.coerceIn(0f, 1f)
    val leftRadius = sqrt(maxRadius * maxRadius - tapeArea * clampedProgress)
    val rightRadius = sqrt(minRadius * minRadius + tapeArea * clampedProgress)
    val guideRadius = shell.width * 0.0244f
    val leftGuide = Offset(shell.width * (52f / 400f), shell.height * (236f / 254f))
    val rightGuide = Offset(shell.width * (348f / 400f), shell.height * (236f / 254f))
    val leftTangent = externalCircleTangent(leftReelCenter, leftRadius, leftGuide, guideRadius, side = 1f)
    val rightTangent = externalCircleTangent(rightReelCenter, rightRadius, rightGuide, guideRadius, side = -1f)
    val leftGuideAngle = Math.toDegrees(
        atan2(leftTangent.second.y - leftGuide.y, leftTangent.second.x - leftGuide.x).toDouble(),
    ).toFloat()
    val rightGuideAngle = Math.toDegrees(
        atan2(rightTangent.second.y - rightGuide.y, rightTangent.second.x - rightGuide.x).toDouble(),
    ).toFloat()
    fun negativeSweep(start: Float, end: Float): Float {
        var sweep = end - start
        while (sweep > 0f) sweep -= 360f
        return sweep
    }

    val path = Path().apply {
        moveTo(leftTangent.first.x, leftTangent.first.y)
        lineTo(leftTangent.second.x, leftTangent.second.y)
        arcTo(
            Rect(
                leftGuide.x - guideRadius,
                leftGuide.y - guideRadius,
                leftGuide.x + guideRadius,
                leftGuide.y + guideRadius,
            ),
            leftGuideAngle,
            negativeSweep(leftGuideAngle, 90f),
            false,
        )
        lineTo(rightGuide.x, rightGuide.y + guideRadius)
        arcTo(
            Rect(
                rightGuide.x - guideRadius,
                rightGuide.y - guideRadius,
                rightGuide.x + guideRadius,
                rightGuide.y + guideRadius,
            ),
            90f,
            negativeSweep(90f, rightGuideAngle),
            false,
        )
        lineTo(rightTangent.first.x, rightTangent.first.y)
    }
    drawPath(path, color = tapeColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
    drawCircle(tapeColor, leftRadius, leftReelCenter)
    drawCircle(tapeColor, rightRadius, rightReelCenter)
    listOf(leftGuide, rightGuide).forEach { guide ->
        drawCircle(Color(0xFFD8D9D5), guideRadius, guide)
        drawCircle(Color(0xFF5A5D60), guideRadius * 0.42f, guide)
    }
    return TapePackRadii(leftRadius, rightRadius, maxRadius)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawReel(center: Offset, rotation: Float, palette: CassetteTapeSkinPalette) {
    val outerRadius = size.height * (26f / 254f)
    val reelWindowRadius = outerRadius * 0.78f
    val spindleRadius = outerRadius * 0.33f
    val spokeStartRadius = spindleRadius * 0.88f
    val spokeEndRadius = outerRadius * 0.72f
    val mainSpokeStrokeWidth = 9f
    val spokeColor = Color(0xFF11100F)
    val spokeShadowColor = Color(0xFF050403).copy(alpha = 0.55f)
    val spokeHighlightColor = Color(0xFFE8D9B7).copy(alpha = 0.24f)
    val spindleRimColor = Color(0xFFC79B61)
    val spindleBeigeColor = Color(0xFFEAD7B0)
    val spindleCreamColor = Color(0xFFF6EACF)
    val spindleGrooveColor = Color(0xFF8B633F)

    rotate(degrees = rotation, pivot = center) {
        drawCircle(color = palette.reel, radius = outerRadius, center = center)
        drawCircle(color = palette.reelWindow, radius = reelWindowRadius, center = center)
        repeat(6) { spoke ->
            val radians = ((spoke * 60f) * PI / 180f).toFloat()
            val start = Offset(
                x = center.x + cos(radians) * spokeStartRadius,
                y = center.y + sin(radians) * spokeStartRadius,
            )
            val end = Offset(
                x = center.x + cos(radians) * spokeEndRadius,
                y = center.y + sin(radians) * spokeEndRadius,
            )
            val shadowOffset = Offset(1.1f, 1.1f)
            val highlightOffset = Offset(-0.8f, -0.8f)

            drawLine(
                color = spokeShadowColor,
                start = start + shadowOffset,
                end = end + shadowOffset,
                strokeWidth = mainSpokeStrokeWidth + 2f,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = spokeColor,
                start = start,
                end = end,
                strokeWidth = mainSpokeStrokeWidth,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = spokeHighlightColor,
                start = start + highlightOffset,
                end = end + highlightOffset,
                strokeWidth = 2f,
                cap = StrokeCap.Round,
            )
        }

        drawCircle(color = spindleRimColor, radius = spindleRadius, center = center)
        drawCircle(color = spindleBeigeColor, radius = spindleRadius * 0.78f, center = center)
        drawCircle(color = spindleCreamColor, radius = spindleRadius * 0.46f, center = center)
        drawLine(
            color = spindleGrooveColor,
            start = center.copy(x = center.x - spindleRadius * 0.46f),
            end = center.copy(x = center.x + spindleRadius * 0.2f),
            strokeWidth = 3.5f,
            cap = StrokeCap.Round,
        )
        drawCircle(
            color = spindleGrooveColor,
            radius = spindleRadius * 0.11f,
            center = center.copy(x = center.x + spindleRadius * 0.42f, y = center.y - spindleRadius * 0.22f),
        )
    }
}

@Composable
private fun WalkmanTransportControls(
    deckTheme: DeckTheme,
    isPlaying: Boolean,
    hasTrack: Boolean,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    onRewind: () -> Unit,
    onPlayPause: () -> Unit,
    onFastForward: () -> Unit,
    onStop: () -> Unit,
    onEject: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = deckTheme.deckPalette()
    Card(
        colors = CardDefaults.cardColors(containerColor = palette.body),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WalkmanButton(label = "⏏", description = "Eject", enabled = hasTrack, palette = palette, onClick = onEject)
            TransportControlDivider(palette.accent)
            WalkmanButton(label = "⏪", description = "Rewind", enabled = canGoPrevious, palette = palette, onClick = onRewind)
            WalkmanButton(label = "■", description = "Stop", enabled = hasTrack, palette = palette, onClick = onStop)
            WalkmanButton(label = "▶", description = "Play", enabled = hasTrack, palette = palette, onClick = onPlayPause)
            WalkmanButton(label = "Ⅱ", description = "Pause", enabled = hasTrack, palette = palette, onClick = onPlayPause)
            WalkmanButton(label = "⏩", description = "Fast-forward", enabled = canGoNext, palette = palette, onClick = onFastForward)
        }
    }
}

@Composable
private fun TransportControlDivider(color: Color) {
    Box(
        modifier = Modifier
            .size(width = 1.dp, height = 24.dp)
            .background(color)
            .semantics { contentDescription = "Transport control divider" },
    )
}

@Composable
private fun WalkmanButton(
    label: String,
    description: String,
    enabled: Boolean,
    palette: DeckPalette,
    onClick: () -> Unit,
) {
    val buttonColor = if (enabled) palette.button else palette.button.copy(alpha = 0.42f)
    val textColor = if (enabled) palette.buttonInk else palette.buttonInk.copy(alpha = 0.5f)
    Box(
        modifier = Modifier
            .size(width = 34.dp, height = 30.dp)
            .drawBehind {
                drawRoundRect(
                    color = buttonColor,
                    size = size,
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                )
            }
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CassetteCoverTrackList(
    tracks: List<Track>,
    currentIndex: Int,
    mixtapeName: String,
    handwritingFont: MixtapeHandwritingFont,
    mixtapeJitterStartIndex: Int,
    sleeveTheme: SleeveTheme = SleeveTheme.BlankWhite,
    caseTheme: CaseTheme = CaseTheme.CrystalClear,
    onDeleteTrackFromDevice: (Track) -> Unit,
    onRemoveTrackFromMixtape: (Track) -> Unit,
    onShowTrackInfo: (Track) -> Unit,
    onTrackDoubleClick: (Int, Track) -> Unit,
    modifier: Modifier = Modifier,
    scrollContent: Boolean = false,
    showHeader: Boolean = true,
) {
    val cassetteHandwritingFontFamily = handwritingFont.cassetteHandwritingFontFamily()
    val sleevePaper = sleeveTheme.paperPalette()
    val casePlastic = caseTheme.plasticPalette()
    val headerText = mixtapeName
    val contentScrollState = rememberScrollState()
    val contentHorizontalScrollState = rememberScrollState()
    val trackListCoroutineScope = rememberCoroutineScope()
    var trackListFlingJob by remember { mutableStateOf<Job?>(null) }
    val trackListDragState = rememberDraggable2DState { dragDelta ->
        // Finger movement moves the paper; ScrollState values move its viewport
        // in the opposite direction. Dispatch each component independently so
        // reaching an edge on one axis never blocks the other.
        contentHorizontalScrollState.dispatchRawDelta(-dragDelta.x)
        contentScrollState.dispatchRawDelta(-dragDelta.y)
    }
    val textMeasurer = rememberTextMeasurer()
    val trackRowTexts = remember(tracks) {
        tracks.map { track ->
            "${track.title} — ${track.artist}  ${formatDuration(track.durationMs)}"
        }
    }
    val trackCenterOffsetsInViewport = remember { mutableStateMapOf<Long, Float>() }
    val trackTopOffsetsInViewport = remember { mutableStateMapOf<Long, Float>() }
    val trackBottomOffsetsInViewport = remember { mutableStateMapOf<Long, Float>() }
    var scrollViewportBounds by remember { mutableStateOf<Rect?>(null) }
    var expandedTrackId by remember { mutableStateOf<Long?>(null) }
    val currentTrackId = tracks.getOrNull(currentIndex)?.id
    val density = LocalDensity.current
    val estimatedTrackRowPitchPx = with(density) { 53.dp.toPx() }
    val estimatedHeaderPitchPx = with(density) { if (showHeader) 66.dp.toPx() else 0f }
    val contentVerticalPaddingPx = with(density) { 14.dp.toPx() }

    LaunchedEffect(tracks) {
        contentHorizontalScrollState.scrollTo(0)
        trackCenterOffsetsInViewport.clear()
        trackTopOffsetsInViewport.clear()
        trackBottomOffsetsInViewport.clear()
    }

    LaunchedEffect(currentTrackId, scrollContent) {
        if (!scrollContent || currentTrackId == null) return@LaunchedEffect

        trackListFlingJob?.cancel()
        var centerOffset = trackCenterOffsetsInViewport[currentTrackId]
        var currentRowTop = trackTopOffsetsInViewport[currentTrackId]
        var currentRowBottom = trackBottomOffsetsInViewport[currentTrackId]

        var measurementAttempts = 0
        while ((centerOffset == null || currentRowTop == null || currentRowBottom == null) && measurementAttempts < 3) {
            withFrameNanos { }
            centerOffset = trackCenterOffsetsInViewport[currentTrackId]
            currentRowTop = trackTopOffsetsInViewport[currentTrackId]
            currentRowBottom = trackBottomOffsetsInViewport[currentTrackId]
            measurementAttempts++
        }

        val viewportHeight = scrollViewportBounds?.height ?: 0f
        val estimatedCenterInViewport = contentVerticalPaddingPx +
            estimatedHeaderPitchPx +
            (currentIndex * estimatedTrackRowPitchPx) +
            (estimatedTrackRowPitchPx / 2f) -
            contentScrollState.value
        val isCurrentRowInViewBand = currentRowTop != null && currentRowBottom != null &&
            currentRowTop >= 0f && currentRowBottom <= viewportHeight
        val isCurrentRowCentered = centerOffset != null &&
            kotlin.math.abs(centerOffset - (viewportHeight / 2f)) <= (viewportHeight * 0.33f)
        val isEstimatedCurrentRowCentered = viewportHeight > 0f &&
            estimatedCenterInViewport >= 0f &&
            estimatedCenterInViewport <= viewportHeight &&
            kotlin.math.abs(estimatedCenterInViewport - (viewportHeight / 2f)) <= (viewportHeight * 0.33f)

        if (viewportHeight > 0f && (!isCurrentRowInViewBand || !isCurrentRowCentered || !isEstimatedCurrentRowCentered)) {
            val measuredTargetScroll = centerOffset?.let { centerOffset ->
                (contentScrollState.value + centerOffset - (viewportHeight / 2f)).roundToInt()
            }
            val estimatedTargetScroll = (contentVerticalPaddingPx +
                estimatedHeaderPitchPx +
                (currentIndex * estimatedTrackRowPitchPx) +
                (estimatedTrackRowPitchPx / 2f) -
                (viewportHeight / 2f)).roundToInt()
            val targetScroll = (if (isEstimatedCurrentRowCentered) measuredTargetScroll ?: estimatedTargetScroll else estimatedTargetScroll)
                .coerceIn(0, contentScrollState.maxValue)
            contentScrollState.animateScrollTo(targetScroll)
        }
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = sleevePaper.base),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(3.dp, casePlastic.edge),
        modifier = modifier
            // TrackListScrollablePaperRatioException: Now Playing uses this as
            // scrollable J-card back paper that fills the remaining pane; any
            // bounded/non-scrolling preview keeps the researched back-flap ratio.
            .then(if (scrollContent) Modifier else Modifier.aspectRatio(CASSETTE_J_CARD_BACK_ASPECT_RATIO))
            .then(
                if (scrollContent) {
                    Modifier.onGloballyPositioned { coordinates ->
                        scrollViewportBounds = coordinates.boundsInWindow()
                    }
                } else {
                    Modifier
                },
            )
            .semantics { contentDescription = "Cassette cover track list" },
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (scrollContent) Modifier.fillMaxSize() else Modifier)
                .then(
                    if (scrollContent) {
                        Modifier.draggable2D(
                            state = trackListDragState,
                            onDragStarted = { trackListFlingJob?.cancel() },
                            onDragStopped = { velocity ->
                                // Coast both axes together. Each ScrollState still
                                // clamps independently when its edge is reached.
                                trackListFlingJob = trackListCoroutineScope.launch {
                                    coroutineScope {
                                        launch {
                                            contentHorizontalScrollState.animateScrollBy(
                                                value = -velocity.x * 0.18f,
                                                animationSpec = tween(
                                                    durationMillis = 450,
                                                    easing = LinearOutSlowInEasing,
                                                ),
                                            )
                                        }
                                        launch {
                                            contentScrollState.animateScrollBy(
                                                value = -velocity.y * 0.18f,
                                                animationSpec = tween(
                                                    durationMillis = 450,
                                                    easing = LinearOutSlowInEasing,
                                                ),
                                            )
                                        }
                                    }
                                }
                            },
                        )
                    } else Modifier,
                ),
        ) {
            val horizontalPaperPadding = 18.dp
            val jitterSafetyPadding = 8.dp
            val rowTextStyle = TextStyle(
                fontFamily = cassetteHandwritingFontFamily,
                fontSize = 40.sp,
                fontWeight = handwritingFont.effectiveCassetteWeight(FontWeight.ExtraBold),
            )
            val headerTextStyle = TextStyle(
                fontFamily = cassetteHandwritingFontFamily,
                fontSize = 48.sp,
                fontWeight = handwritingFont.effectiveCassetteWeight(FontWeight.ExtraBold),
            )
            val widestRowWidthPx = remember(
                trackRowTexts,
                rowTextStyle,
                textMeasurer,
                density.density,
                density.fontScale,
            ) {
                trackRowTexts.maxOfOrNull { rowText ->
                    // The renderer measures tokens independently, so reserve one rounding pixel per token.
                    val tokenRoundingSafetyPx = Regex("\\S+|\\s+").findAll(rowText).count()
                    textMeasurer.measure(text = rowText, style = rowTextStyle, maxLines = 1).size.width +
                        tokenRoundingSafetyPx
                } ?: 0
            }
            val headerWidthPx = remember(
                showHeader,
                headerText,
                headerTextStyle,
                textMeasurer,
                density.density,
                density.fontScale,
            ) {
                if (showHeader) {
                    textMeasurer.measure(text = headerText, style = headerTextStyle, maxLines = 1).size.width
                } else {
                    0
                }
            }
            val measuredTextWidth = with(density) { maxOf(widestRowWidthPx, headerWidthPx).toDp() }
            val viewportTextWidth = (maxWidth - horizontalPaperPadding * 2).coerceAtLeast(1.dp)
            val trackListTextWidth = maxOf(
                viewportTextWidth,
                measuredTextWidth + jitterSafetyPadding * 2,
            )
            val trackListPaperWidth = trackListTextWidth + horizontalPaperPadding * 2

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (scrollContent) {
                            Modifier.horizontalScroll(
                                contentHorizontalScrollState,
                                enabled = false,
                            )
                        } else Modifier,
                    ),
            ) {
                Column(
                    modifier = Modifier
                        .requiredWidth(trackListPaperWidth)
                        .then(
                            if (scrollContent) {
                                Modifier
                                    .fillMaxHeight()
                                    .verticalScroll(
                                        contentScrollState,
                                        enabled = false,
                                    )
                            } else {
                                Modifier
                            },
                        )
                .drawBehind {
                    when (sleevePaper.pattern) {
                        SleevePaperPattern.Lined -> {
                            val lineSpacing = 49.dp.toPx()
                            var y = 98.dp.toPx()
                            while (y < size.height) {
                                drawLine(sleevePaper.line, Offset(0f, y), Offset(size.width, y), 1.5f)
                                y += lineSpacing
                            }
                        }
                        SleevePaperPattern.Grid -> {
                            val grid = 36.dp.toPx()
                            var x = grid
                            while (x < size.width) { drawLine(sleevePaper.line, Offset(x, 0f), Offset(x, size.height), 1f); x += grid }
                            var y = grid
                            while (y < size.height) { drawLine(sleevePaper.line, Offset(0f, y), Offset(size.width, y), 1f); y += grid }
                        }
                        SleevePaperPattern.Album, SleevePaperPattern.Blank -> Unit
                    }
                    sleevePaper.speckle?.let { fleck ->
                        repeat(80) { index ->
                            val x = ((index * 83) % 997) / 997f * size.width
                            val y = ((index * 47) % 991) / 991f * size.height
                            drawCircle(fleck.copy(alpha = 0.32f), 0.8.dp.toPx(), Offset(x, y))
                        }
                    }
                }
                .drawWithContent {
                    drawContent()
                    drawRoundRect(casePlastic.tint.copy(alpha = casePlastic.tint.alpha * CASE_PLASTIC_TINT_STRENGTH), size = size, cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()))
                    if (casePlastic.haze != Color.Transparent) drawRoundRect(casePlastic.haze.copy(alpha = casePlastic.haze.alpha * CASE_PLASTIC_TINT_STRENGTH), size = size, cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()))
                    drawRoundRect(casePlastic.edge.copy(alpha = 0.7f), size = size, cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()), style = Stroke(width = 1.5.dp.toPx()))
                }
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (showHeader) {
                JitteredHandwritingText(
                    text = headerText,
                    modifier = Modifier.width(trackListTextWidth),
                    startIndex = mixtapeJitterStartIndex,
                    color = sleevePaper.ink,
                    fontFamily = cassetteHandwritingFontFamily,
                    fontSize = 48.sp,
                    fontWeight = handwritingFont.effectiveCassetteWeight(FontWeight.ExtraBold),
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    tokenization = HandwritingJitterTokenization.Character,
                    strength = HandwritingJitterStrength(maxDxEm = 0.054f, maxDyEm = 0.09f, maxRotationDegrees = 3.0f, maxTrackingEm = 0.018f),
                )
            }
            tracks.forEachIndexed { index, track ->
                val selected = index == currentIndex
                val rowText = trackRowTexts[index]
                Box(
                    modifier = Modifier
                        .width(trackListTextWidth)
                        .onGloballyPositioned { coordinates ->
                            val viewportBounds = scrollViewportBounds
                            if (viewportBounds != null) {
                                val rowBounds = coordinates.boundsInWindow()
                                trackCenterOffsetsInViewport[track.id] = rowBounds.center.y - viewportBounds.top
                                trackTopOffsetsInViewport[track.id] = rowBounds.top - viewportBounds.top
                                trackBottomOffsetsInViewport[track.id] = rowBounds.bottom - viewportBounds.top
                            }
                        }
                        .combinedClickable(
                            onClick = {},
                            onDoubleClick = { onTrackDoubleClick(index, track) },
                            onLongClick = { expandedTrackId = track.id },
                        )
                        .semantics { contentDescription = "Track actions for ${track.title}" },
                ) {
                    JitteredHandwritingText(
                        text = rowText,
                        startIndex = handwritingTrackStartIndex(mixtapeJitterStartIndex, index),
                        color = if (selected) sleevePaper.accent else sleevePaper.ink,
                        fontFamily = cassetteHandwritingFontFamily,
                        fontSize = 40.sp,
                        fontWeight = handwritingFont.effectiveCassetteWeight(if (selected) FontWeight.ExtraBold else FontWeight.Bold),
                        lineHeightScale = 0.8f,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        tokenization = HandwritingJitterTokenization.Character,
                        strength = HandwritingJitterStrength(maxDxEm = 0.036f, maxDyEm = 0.065f, maxRotationDegrees = 1.8f, maxTrackingEm = 0.008f),
                        modifier = Modifier
                            .width(trackListTextWidth)
                            .semantics { contentDescription = rowText },
                    )
                    DropdownMenu(
                        expanded = expandedTrackId == track.id,
                        onDismissRequest = { expandedTrackId = null },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Delete from device") },
                            onClick = {
                                expandedTrackId = null
                                onDeleteTrackFromDevice(track)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Remove from mixtape") },
                            onClick = {
                                expandedTrackId = null
                                onRemoveTrackFromMixtape(track)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Track info") },
                            onClick = {
                                expandedTrackId = null
                                onShowTrackInfo(track)
                            },
                        )
                    }
                }
            }
                    if (tracks.isEmpty()) {
                        JitteredHandwritingText(
                            text = "No tracks queued",
                            startIndex = handwritingTrackStartIndex(mixtapeJitterStartIndex, 0),
                            color = sleevePaper.ink,
                            fontFamily = cassetteHandwritingFontFamily,
                            fontSize = 40.sp,
                            fontWeight = handwritingFont.effectiveCassetteWeight(FontWeight.Bold),
                            lineHeightScale = 0.8f,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            tokenization = HandwritingJitterTokenization.Character,
                            strength = HandwritingJitterStrength(maxDxEm = 0.036f, maxDyEm = 0.065f, maxRotationDegrees = 1.8f, maxTrackingEm = 0.008f),
                            modifier = Modifier.width(trackListTextWidth),
                        )
                    }
                }
            }
        }
    }
}


private fun tapeNameFor(state: MixtapeUiState): String = when {
    !state.currentMixtapeName.isNullOrBlank() -> state.currentMixtapeName
    !state.currentMixtapeStableKey.isNullOrBlank() -> state.mixTapeGroups
        .firstOrNull { it.tracks == state.queueTracks }
        ?.name
        ?: "Mix Tape"
    state.mixTapeGroups.size == 1 -> state.mixTapeGroups.first().name
    else -> "Mix Tape"
}

private fun nowPlayingJitterStartIndex(state: MixtapeUiState): Int {
    return state.currentMixtapeJitterStartIndex
}

private fun MixTapeGroup.handwritingJitterStartIndex(): Int = visualProperties.jitterStartIndex

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}

@Composable
private fun TrackInfoScreen(
    track: Track?,
    mixtapeName: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TextButton(onClick = onBack) { Text("Back to Now Playing") }
        Text("Track info", style = MaterialTheme.typography.titleLarge)
        if (track == null) {
            Text("No track selected.")
        } else {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TrackInfoRow("Title", track.title)
                    TrackInfoRow("Artist", track.artist)
                    TrackInfoRow("Duration", formatDuration(track.durationMs))
                    TrackInfoRow("MediaStore ID", track.id.toString())
                    TrackInfoRow("URI", track.uri)
                    TrackInfoRow("Display name", track.displayName ?: "Unknown")
                    TrackInfoRow("Album", track.album ?: "Unknown")
                    TrackInfoRow("MIME type", track.mimeType ?: "Unknown")
                    TrackInfoRow("Size", track.sizeBytes?.let { "$it bytes" } ?: "Unknown")
                    TrackInfoRow("Date added", track.dateAddedSeconds?.toString() ?: "Unknown")
                    TrackInfoRow("Date modified", track.dateModifiedSeconds?.toString() ?: "Unknown")
                    TrackInfoRow("Track number", track.trackNumber?.toString() ?: "Unknown")
                    TrackInfoRow("Mixtape", mixtapeName ?: "Unknown")
                }
            }
        }
    }
}

@Composable
private fun TrackInfoRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, fontWeight = FontWeight.Bold)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun Enum<*>.componentThemeName(): String = name
    .replace(Regex("([a-z0-9])([A-Z])"), "${'$'}1 ${'$'}2")
    .replace("Hi FI", "Hi-Fi")
    .replace("C 60", "C-60")

private data class DeckPalette(
    val body: Color,
    val panel: Color,
    val outline: Color,
    val text: Color,
    val well: Color,
    val wellFrame: Color,
    val button: Color,
    val buttonInk: Color,
    val accent: Color,
    val counterFace: Color,
    val counterInk: Color,
)

private fun DeckTheme.deckPalette(): DeckPalette = when (this) {
    DeckTheme.SilverfaceHiFi -> DeckPalette(
        Color(0xFFC7CAD1), Color(0xFFD8DBE0), Color(0xFF82868F), Color(0xFF3C4046),
        Color(0xFF33373F), Color(0xFF82868F), Color(0xFF35383F), Color(0xFFE8EAEF),
        Color(0xFFE2572F), Color(0xFF44464A), Color(0xFF9FE8B0),
    )
    DeckTheme.NavyMicro -> DeckPalette(
        Color(0xFF172B4B), Color(0xFFBFC4CA), Color(0xFF0B172A), Color(0xFF26364D),
        Color(0xFF101B2D), Color(0xFF6F7D90), Color(0xFFC8CCD1), Color(0xFF25344B),
        Color(0xFF3978C7), Color(0xFF202936), Color(0xFF83E2BD),
    )
    DeckTheme.CrimsonMetal -> DeckPalette(
        Color(0xFF741023), Color(0xFFA71932), Color(0xFF3D0710), Color(0xFFF5DFE3),
        Color(0xFF111216), Color(0xFF4B101B), Color(0xFF15171B), Color(0xFFE9EBEE),
        Color(0xFFEF3555), Color(0xFF17191C), Color(0xFFFF9BB0),
    )
    DeckTheme.SafetyYellow -> DeckPalette(
        Color(0xFFDCAE13), Color(0xFFEFC52B), Color(0xFF715806), Color(0xFF302A18),
        Color(0xFF17191A), Color(0xFF514713), Color(0xFF242628), Color(0xFFF6D64B),
        Color(0xFF32BBA5), Color(0xFF242628), Color(0xFFF5D64B),
    )
    DeckTheme.GraphiteSlim -> DeckPalette(
        Color(0xFF30343A), Color(0xFF555A61), Color(0xFF15181C), Color(0xFFEEF1F4),
        Color(0xFF111317), Color(0xFF777E88), Color(0xFFC5C9CE), Color(0xFF20242A),
        Color(0xFFE53E48), Color(0xFF171A1E), Color(0xFF71D8DB),
    )
    DeckTheme.SunsetBoombox -> DeckPalette(
        Color(0xFFE8DCC3), Color(0xFFF0E7D3), Color(0xFFA9946A), Color(0xFF4A3F33),
        Color(0xFF41301F), Color(0xFF8A744F), Color(0xFF4A3F33), Color(0xFFF4EAD6),
        Color(0xFFD3542E), Color(0xFF4A3F33), Color(0xFFF4EAD6),
    )
    DeckTheme.BlackoutPortable -> DeckPalette(
        Color(0xFF1D1F24), Color(0xFF25282E), Color(0xFF0B0C0F), Color(0xFFD7DAE0),
        Color(0xFF0C0D10), Color(0xFF34383F), Color(0xFFD7DAE0), Color(0xFF1D1F24),
        Color(0xFFFF5533), Color(0xFF17191C), Color(0xFFFFA287),
    )
}

private fun DeckTheme.backgroundColor(): Color = when (this) {
    DeckTheme.NavyMicro -> Color(0xFFE7EDF5)
    DeckTheme.CrimsonMetal -> Color(0xFFF3E5E5)
    DeckTheme.SafetyYellow -> Color(0xFFFFF4C4)
    DeckTheme.GraphiteSlim -> Color(0xFFE4E5E7)
    DeckTheme.SilverfaceHiFi -> Color(0xFFF0F1F2)
    DeckTheme.SunsetBoombox -> Color(0xFFFFE9DA)
    DeckTheme.BlackoutPortable -> Color(0xFFD7D9DD)
}

private fun DeckTheme.readableName(): String = componentThemeName()
private fun CassetteTheme.readableName(): String = componentThemeName()
private fun ScrewTheme.readableName(): String = componentThemeName()
private fun StickerTheme.readableName(): String = componentThemeName()
private fun CaseTheme.readableName(): String = componentThemeName()
private fun SleeveTheme.readableName(): String = componentThemeName()

@Preview(showBackground = true)
@Composable
private fun MixtapePreview() {
    AndroidMixtapeTheme {
        MixtapeApp(
            state = MixtapeUiState(
                status = LibraryStatus.Ready,
                tracks = listOf(Track(1, "Demo Song", "Unknown artist", 181_000, "content://demo/1")),
                queueTracks = listOf(Track(1, "Demo Song", "Unknown artist", 181_000, "content://demo/1")),
                currentTrack = Track(1, "Demo Song", "Unknown artist", 181_000, "content://demo/1"),
                currentIndex = 0,
                durationMs = 181_000,
                screen = MixtapeScreen.NowPlaying,
            ),
            onRequestPermission = {},
            onRefresh = {},
            onTogglePlayPause = {},
            onPrevious = {},
            onNext = {},
            onStop = {},
            onSeekTo = {},
        )
    }
}
