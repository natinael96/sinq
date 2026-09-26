package com.agpeya.app.data

import android.app.Activity
import android.util.Log
import com.agpeya.app.BuildConfig
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.requestAppUpdateInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The Play half of "there is a newer ስንቅ".
 *
 * [UpdateRepository] is the other half and the two never both speak. A
 * hand-installed build has to announce its own updates because nothing else
 * will, and it must not be the one calling Play: In-App Updates only works for
 * an app Play installed, and asking from a sideloaded copy earns an error and
 * nothing else. A Play install is the reverse — it must never be pointed at a
 * page of APKs, and Play can update it properly. So the single
 * `BuildConfig.UPDATE_NOTICE` flag that darkens [UpdateRepository] is exactly
 * the flag that lights this, and neither call site has to know which build it
 * is in.
 *
 * **Flexible, never immediate.** Play offers a blocking full-screen flow that
 * will not let someone use the app until it updates. That is the wrong thing to
 * do to a person who has opened a prayer book: the hour is now, the whole app
 * already works offline, and an update can wait for as long as they like. So
 * the download runs in the background while they pray, and the only thing the
 * app ever asks for is the restart at the end — once, on a line they can
 * dismiss.
 *
 * Every failure is swallowed, for the reason [UpdateRepository] gives: a person
 * praying offline should never learn about it from ስንቅ.
 */
object PlayUpdateRepository {

    private const val TAG = "PlayUpdate"

    /** Where this build's updates come from at all. */
    val handlesUpdates: Boolean get() = !BuildConfig.UPDATE_NOTICE

    /** What the ቤት line needs, or null when there is nothing to say. */
    enum class Stage {
        /** Downloaded and waiting on a restart this app has to ask for. */
        READY_TO_INSTALL,
    }

    private val _stage = MutableStateFlow<Stage?>(null)
    val stage: StateFlow<Stage?> = _stage.asStateFlow()

    private var manager: AppUpdateManager? = null
    private var listener: InstallStateUpdatedListener? = null

    private fun manager(activity: Activity): AppUpdateManager =
        manager ?: AppUpdateManagerFactory.create(activity.applicationContext).also { manager = it }

    /**
     * Ask Play whether there is a newer build, and start the flexible flow if
     * there is.
     *
     * [launch] hands the consent dialog to the activity, which owns the result
     * contract. It is not launched when Play has nothing to offer, so the
     * common case — already up to date — shows the person nothing at all.
     */
    suspend fun check(activity: Activity, launch: (AppUpdateManager, com.google.android.play.core.appupdate.AppUpdateInfo) -> Unit) {
        if (!handlesUpdates) return
        runCatching {
            val am = manager(activity)
            val info = am.requestAppUpdateInfo()
            when {
                // A download that finished while the app was away: Play does not
                // re-announce it, so without this check the restart is never
                // offered and the update sits on the device forever.
                info.installStatus() == InstallStatus.DOWNLOADED -> {
                    _stage.value = Stage.READY_TO_INSTALL
                }
                info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                    info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> {
                    listen(am)
                    launch(am, info)
                }
                else -> Unit
            }
        }.onFailure {
            // Not installed by Play, no Play services, no network, Play saying
            // no — all the same outcome: the app says nothing.
            Log.d(TAG, "play update check skipped: ${it.message}")
        }
    }

    /** The options every launch uses; flexible for the reason in the kdoc. */
    fun flexibleOptions(): AppUpdateOptions =
        AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()

    /**
     * Watch a running download so the restart can be offered the moment it
     * lands, rather than only on the next launch.
     */
    private fun listen(am: AppUpdateManager) {
        if (listener != null) return
        val l = InstallStateUpdatedListener { state ->
            if (state.installStatus() == InstallStatus.DOWNLOADED) {
                _stage.value = Stage.READY_TO_INSTALL
            }
        }
        listener = l
        runCatching { am.registerListener(l) }
    }

    /**
     * Finish the update: Play restarts the app into the new build.
     *
     * The line stays put if this fails — better a restart offered twice than an
     * update that quietly never happened.
     */
    fun install(activity: Activity) {
        if (!handlesUpdates) return
        runCatching { manager(activity).completeUpdate() }
            .onFailure { Log.d(TAG, "completeUpdate failed: ${it.message}") }
    }

    /** Wave the restart away; the next launch will offer it again. */
    fun dismiss() {
        _stage.value = null
    }

    /** Drop the listener with the activity that registered it. */
    fun release() {
        val am = manager
        val l = listener
        if (am != null && l != null) runCatching { am.unregisterListener(l) }
        listener = null
    }
}
