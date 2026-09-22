package com.example.androidmixtape

import android.app.Activity
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import com.example.androidmixtape.data.MediaStoreAudioRepository
import com.example.androidmixtape.diagnostics.AndroidAutoDiagnostics
import com.example.androidmixtape.name.AssetMixtapeNameSource
import com.example.androidmixtape.name.SharedPreferencesMixtapeNameStore
import com.example.androidmixtape.permissions.AudioPermissionPolicy
import com.example.androidmixtape.playback.AudioTrackTransportCuePlayer
import com.example.androidmixtape.playback.MediaControllerPlayerEngine
import com.example.androidmixtape.playback.MixtapeController
import com.example.androidmixtape.ui.AndroidMixtapeTheme
import com.example.androidmixtape.ui.MixtapeApp
import com.example.androidmixtape.viewmodel.MixtapeViewModel
import com.example.androidmixtape.viewmodel.SharedPreferencesMixtapeExclusionSettingsStore
import com.example.androidmixtape.viewmodel.SharedPreferencesMixtapeHandwritingFontSettingsStore
import com.example.androidmixtape.viewmodel.SharedPreferencesMixtapeSettingsStore
import com.example.androidmixtape.viewmodel.SharedPreferencesMixtapeSpineSkinSettingsStore
import com.example.androidmixtape.viewmodel.SharedPreferencesMixtapeSymbolSettingsStore
import com.example.androidmixtape.viewmodel.SharedPreferencesMixtapeTapeSkinSettingsStore
import com.example.androidmixtape.viewmodel.SharedPreferencesMixtapeThemeSettingsStore
import com.example.androidmixtape.viewmodel.SharedPreferencesMixtapeVisualPropertiesStore

class MainActivity : ComponentActivity() {
    private val viewModel: MixtapeViewModel by viewModels {
        MixtapeViewModel.Factory(
            repository = MediaStoreAudioRepository(applicationContext),
            controller = MixtapeController(MediaControllerPlayerEngine(applicationContext)),
            nameSource = AssetMixtapeNameSource(applicationContext),
            nameStore = SharedPreferencesMixtapeNameStore(applicationContext),
            visualPropertiesStore = SharedPreferencesMixtapeVisualPropertiesStore(applicationContext),
            settingsStore = SharedPreferencesMixtapeSettingsStore(applicationContext),
            symbolSettingsStore = SharedPreferencesMixtapeSymbolSettingsStore(applicationContext),
            tapeSkinSettingsStore = SharedPreferencesMixtapeTapeSkinSettingsStore(applicationContext),
            spineSkinSettingsStore = SharedPreferencesMixtapeSpineSkinSettingsStore(applicationContext),
            handwritingFontSettingsStore = SharedPreferencesMixtapeHandwritingFontSettingsStore(applicationContext),
            themeSettingsStore = SharedPreferencesMixtapeThemeSettingsStore(applicationContext),
            exclusionSettingsStore = SharedPreferencesMixtapeExclusionSettingsStore(applicationContext),
            transportCuePlayer = AudioTrackTransportCuePlayer(),
        )
    }

    private val deleteConfirmationCallback: (IntentSender) -> Unit = ::launchDeleteConfirmation

    private val requestPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.onPermissionResult(granted)
    }

    private val deleteTrackConfirmation = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.confirmTrackDeletedFromDevice()
        } else {
            viewModel.cancelPendingTrackDeleteFromDevice()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidAutoDiagnostics.logPhoneEnvironment(this, "main_activity_create")
        val permission = AudioPermissionPolicy.requiredRuntimePermission(Build.VERSION.SDK_INT)
        viewModel.onPermissionResult(
            permission == null || checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED,
        )
        viewModel.onDeleteTrackUserActionRequired = deleteConfirmationCallback

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            AndroidMixtapeTheme {
                MixtapeApp(
                    state = state,
                    onRequestPermission = { permission?.let(requestPermission::launch) ?: viewModel.onPermissionResult(true) },
                    onRefresh = viewModel::refresh,
                    onShowSettings = viewModel::showSettings,
                    onExitSettings = viewModel::exitSettings,
                    onShowHelp = viewModel::showHelp,
                    onShowMixtapeNames = viewModel::showMixtapeNames,
                    onShowMixtapeSymbolSettings = viewModel::showMixtapeSymbolSettings,
                    onShowTapeSkinSettings = viewModel::showTapeSkinSettings,
                    onShowSpineSkinSettings = viewModel::showSpineSkinSettings,
                    onShowHandwritingFontSettings = viewModel::showHandwritingFontSettings,
                    onShowDeckThemeSettings = viewModel::showDeckThemeSettings,
                    onShowCassetteThemeSettings = viewModel::showCassetteThemeSettings,
                    onShowScrewThemeSettings = viewModel::showScrewThemeSettings,
                    onShowStickerThemeSettings = viewModel::showStickerThemeSettings,
                    onShowCaseThemeSettings = viewModel::showCaseThemeSettings,
                    onShowSleeveThemeSettings = viewModel::showSleeveThemeSettings,
                    onShowExclusionSettings = viewModel::showExclusionSettings,
                    onMixTapeGroupClick = viewModel::selectMixTapeGroup,
                    onBackToMixTapes = viewModel::backToMixTapes,
                    onTogglePlayPause = viewModel::togglePlayPause,
                    onPrevious = viewModel::previous,
                    onNext = viewModel::next,
                    onStop = viewModel::stop,
                    onEject = viewModel::eject,
                    onSeekTo = viewModel::seekTo,
                    onDeleteTrackFromDevice = { track -> viewModel.deleteTrackFromDevice(track.id) },
                    onRemoveTrackFromMixtape = { track -> viewModel.removeTrackFromCurrentMixtape(track.id) },
                    onShowTrackInfo = { track -> viewModel.showTrackInfo(track.id) },
                    onTrackDoubleClick = viewModel::jumpToTrackWithCue,
                    onBackFromTrackInfo = viewModel::backFromTrackInfo,
                    onResetAllMixTapes = viewModel::resetAllMixTapes,
                    onSongsPerMixTapeChange = viewModel::updateSongsPerMixTape,
                    onArtistGroupingChange = viewModel::updateArtistGrouping,
                    onHandwritingMessinessChange = viewModel::updateHandwritingMessiness,
                    onEditMixtapeName = viewModel::editMixtapeName,
                    onRegenerateMixtapeName = viewModel::regenerateMixtapeName,
                    onAddFilenameExclusionPattern = viewModel::addFilenameExclusionPattern,
                    onRemoveFilenameExclusionPattern = viewModel::removeFilenameExclusionPattern,
                    onMixtapeEmbellishmentEnabledChange = viewModel::updateMixtapeEmbellishmentEnabled,
                    onMixtapeTapeSkinEnabledChange = viewModel::updateMixtapeTapeSkinEnabled,
                    onMixtapeSpineSkinEnabledChange = viewModel::updateMixtapeSpineSkinEnabled,
                    onMixtapeHandwritingFontEnabledChange = viewModel::updateMixtapeHandwritingFontEnabled,
                    onDeckThemeChange = viewModel::updateDeckTheme,
                    onCassetteThemeEnabledChange = viewModel::updateCassetteThemeEnabled,
                    onScrewThemeEnabledChange = viewModel::updateScrewThemeEnabled,
                    onStickerThemeEnabledChange = viewModel::updateStickerThemeEnabled,
                    onCaseThemeEnabledChange = viewModel::updateCaseThemeEnabled,
                    onSleeveThemeEnabledChange = viewModel::updateSleeveThemeEnabled,
                    onUpdateCurrentMixtapeCustomization = viewModel::updateCurrentMixtapeCustomization,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val permission = AudioPermissionPolicy.requiredRuntimePermission(Build.VERSION.SDK_INT)
        viewModel.onPermissionResult(
            permission == null || checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED,
        )
    }

    override fun onDestroy() {
        if (viewModel.onDeleteTrackUserActionRequired === deleteConfirmationCallback) {
            viewModel.onDeleteTrackUserActionRequired = null
        }
        super.onDestroy()
    }

    private fun launchDeleteConfirmation(intentSender: IntentSender) {
        try {
            deleteTrackConfirmation.launch(IntentSenderRequest.Builder(intentSender).build())
        } catch (error: RuntimeException) {
            viewModel.failPendingTrackDeleteFromDevice(error.message ?: "Could not launch Android delete confirmation")
        }
    }
}
