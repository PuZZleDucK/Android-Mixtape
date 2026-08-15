package com.example.androidmixtape.car

import android.content.Intent
import androidx.car.app.CarAppService
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.ScreenManager
import androidx.car.app.Session
import androidx.car.app.SessionInfo
import androidx.car.app.media.model.MediaPlaybackTemplate
import androidx.car.app.model.Action
import androidx.car.app.model.GridItem
import androidx.car.app.model.GridSection
import androidx.car.app.model.Header
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.RowSection
import androidx.car.app.model.SectionedItemTemplate
import androidx.car.app.model.Template
import androidx.car.app.validation.HostValidator
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.androidmixtape.diagnostics.AndroidAutoDiagnostics

class MixtapeCarAppService : CarAppService() {
    override fun onCreate() {
        super.onCreate()
        AndroidAutoDiagnostics.log(this, "car_app_service_onCreate")
        AndroidAutoDiagnostics.logPhoneEnvironment(this, "car_app_service_create")
    }

    override fun createHostValidator(): HostValidator {
        AndroidAutoDiagnostics.log(this, "car_app_service_createHostValidator hostInfo=$hostInfo")
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onCreateSession(): Session {
        AndroidAutoDiagnostics.log(this, "car_app_service_onCreateSession legacy hostInfo=$hostInfo")
        return MixtapeCarSession(null)
    }

    override fun onCreateSession(sessionInfo: SessionInfo): Session {
        AndroidAutoDiagnostics.log(this, "car_app_service_onCreateSession sessionInfo=$sessionInfo hostInfo=$hostInfo")
        return MixtapeCarSession(sessionInfo)
    }

    override fun onDestroy() {
        AndroidAutoDiagnostics.log(this, "car_app_service_onDestroy hostInfo=$hostInfo")
        super.onDestroy()
    }
}

class MixtapeCarSession(
    private val initialSessionInfo: SessionInfo?,
) : Session() {
    private var carController: MixtapeCarMediaController? = null

    init {
        lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                AndroidAutoDiagnostics.log(carContext, "car_session_lifecycle event=$event sessionInfo=$initialSessionInfo")
                if (event == Lifecycle.Event.ON_DESTROY) {
                    carController?.release()
                    carController = null
                }
            },
        )
    }

    override fun onCreateScreen(intent: Intent): Screen {
        AndroidAutoDiagnostics.logCarHost(carContext, initialSessionInfo, intent)
        val catalogState = CarMixtapeCatalog(carContext).load()
        AndroidAutoDiagnostics.log(
            carContext,
            "car_session_catalog mixtapes=${catalogState.mixtapes.size} message=${catalogState.message}",
        )
        val controller = controller()
        if (intent.action == SHOW_MEDIA_PLAYBACK) {
            return MixtapePlaybackScreen(carContext, controller)
        }
        return MixtapeListScreen(carContext, CarMixtapeCatalog(carContext), controller)
    }

    override fun onNewIntent(intent: Intent) {
        AndroidAutoDiagnostics.log(
            carContext,
            "car_session_onNewIntent action=${intent.action} flags=0x${intent.flags.toString(16)} extras=${intent.extras?.keySet()?.sorted()}",
        )
        super.onNewIntent(intent)
        if (intent.action == SHOW_MEDIA_PLAYBACK) {
            val screenManager: ScreenManager = carContext.getCarService(ScreenManager::class.java)
            if (screenManager.top !is MixtapePlaybackScreen) {
                screenManager.push(MixtapePlaybackScreen(carContext, controller()))
            }
        }
    }

    private fun controller(): MixtapeCarMediaController = carController ?: MixtapeCarMediaController(carContext).also { controller ->
        carController = controller
        controller.registerMediaPlaybackToken(carContext)
    }

    companion object {
        const val SHOW_MEDIA_PLAYBACK = "androidx.car.app.media.action.SHOW_MEDIA_PLAYBACK"
    }
}

class MixtapeListScreen(
    carContext: CarContext,
    private val catalog: CarMixtapeCatalog,
    private val carController: MixtapeCarMediaController,
) : Screen(carContext) {
    override fun onGetTemplate(): Template {
        val state = catalog.load()
        AndroidAutoDiagnostics.log(
            carContext,
            "car_mixtape_list_template mixtapes=${state.mixtapes.size} message=${state.message}",
        )
        if (state.mixtapes.isEmpty()) {
            return MessageTemplate.Builder(state.message ?: "No mixtapes are ready yet.")
                .setHeader(header("Mixtapes"))
                .addAction(Action.MEDIA_PLAYBACK)
                .build()
        }

        val mixtapes = state.mixtapes
        val cassetteBriefcase = GridSection.Builder()
            .setTitle("${mixtapes.size} mix tapes filed spine-out in a cassette briefcase")
            .setItemSize(GridSection.ITEM_SIZE_LARGE)
            .setItemImageShape(GridSection.ITEM_IMAGE_SHAPE_UNSET)
        mixtapes.forEachIndexed { index, mixtape: CarMixtape ->
            cassetteBriefcase.addItem(
                GridItem.Builder()
                    .setTitle(mixtape.displayName)
                    .setText("${mixtape.trackCount} tracks • Side ${index + 1}")
                    .setImage(
                        MixtapeCarArtworkRenderer.icon(mixtape, sideNumber = index + 1),
                        GridItem.IMAGE_TYPE_LARGE,
                    )
                    .setOnClickListener {
                        screenManager.push(MixtapeSideScreen(carContext, mixtape, carController))
                    }
                    .build(),
            )
        }

        return SectionedItemTemplate.Builder()
            .setHeader(header("Mixtape briefcase"))
            .addAction(Action.MEDIA_PLAYBACK)
            .addSection(cassetteBriefcase.build())
            .build()
    }
}

class MixtapeSideScreen(
    carContext: CarContext,
    private val mixtape: CarMixtape,
    private val carController: MixtapeCarMediaController,
) : Screen(carContext) {
    override fun onGetTemplate(): Template {
        AndroidAutoDiagnostics.log(
            carContext,
            "car_mixtape_side_template name=${mixtape.displayName} tracks=${mixtape.tracks.size} startIndex=${mixtape.startIndex}",
        )
        if (mixtape.tracks.isEmpty()) {
            return MessageTemplate.Builder("This side of tape has no playable tracks.")
                .setHeader(header(mixtape.displayName, Action.BACK))
                .addAction(Action.MEDIA_PLAYBACK)
                .build()
        }

        val rows = RowSection.Builder()
            .setTitle("Side ${mixtape.startIndex + 1}")
            .addItem(
                Row.Builder()
                    .setTitle("Play ${mixtape.displayName}")
                    .addText("Start this side from the first track")
                    .setOnClickListener { playSide(startIndex = 0) }
                    .build(),
            )
        mixtape.tracks.forEachIndexed { trackIndex, track ->
            rows.addItem(
                Row.Builder()
                    .setTitle(track.title)
                    .addText(track.subtitle)
                    .setOnClickListener { playSide(startIndex = trackIndex) }
                    .build(),
            )
        }

        return SectionedItemTemplate.Builder()
            .setHeader(header(mixtape.displayName, Action.BACK))
            .addAction(Action.MEDIA_PLAYBACK)
            .addSection(rows.build())
            .build()
    }

    private fun playSide(startIndex: Int) {
        AndroidAutoDiagnostics.log(
            carContext,
            "car_mixtape_play_side name=${mixtape.displayName} tracks=${mixtape.tracks.size} startIndex=$startIndex",
        )
        carController.playMixtape(mixtape, startIndex)
        screenManager.push(MixtapePlaybackScreen(carContext, carController))
    }
}

class MixtapePlaybackScreen(
    carContext: CarContext,
    private val carController: MixtapeCarMediaController,
) : Screen(carContext) {
    override fun onGetTemplate(): Template {
        AndroidAutoDiagnostics.log(carContext, "car_playback_template_requested")
        carController.registerMediaPlaybackToken(carContext)
        return MediaPlaybackTemplate.Builder()
            .setHeader(header("Mixtape playback", Action.BACK))
            .build()
    }
}

private fun header(title: String, startAction: Action = Action.APP_ICON): Header = Header.Builder()
    .setStartHeaderAction(startAction)
    .setTitle(title)
    .build()
