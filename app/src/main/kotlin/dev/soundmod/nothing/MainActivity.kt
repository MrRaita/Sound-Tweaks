package dev.soundmod.nothing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.soundmod.nothing.core.SoundSlot
import dev.soundmod.nothing.ui.MainViewModel
import dev.soundmod.nothing.ui.SoundModScreen
import dev.soundmod.nothing.ui.SoundModTheme
import dev.soundmod.nothing.ui.UiActions

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onStart() {
        super.onStart()
        vm.startSysWatch()
    }

    override fun onStop() {
        vm.stopSysWatch()
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by vm.state.collectAsStateWithLifecycle()
            val player = vm.player.collectAsStateWithLifecycle()
            val snackbar = remember { SnackbarHostState() }
            var pendingSlot by remember { mutableStateOf<SoundSlot?>(null) }

            val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                val slot = pendingSlot
                pendingSlot = null
                if (uri != null && slot != null) vm.applyCustom(slot, uri)
            }

            LaunchedEffect(Unit) {
                vm.messages.collect { snackbar.showSnackbar(it) }
            }

            val actions = remember {
                UiActions(
                    refresh = vm::refreshAll,
                    refreshSys = vm::refreshSys,
                    reloadLibrary = vm::loadLibrary,
                    ensureLibrary = vm::ensureLibrary,
                    playLibrary = vm::playLibrary,
                    playSlot = vm::playSlot,
                    testFx = vm::testEffect,
                    pickSystem = vm::applySystem,
                    pickFile = { slot ->
                        pendingSlot = slot
                        picker.launch(arrayOf("audio/*"))
                    },
                    reset = vm::reset,
                    setReduceMotion = vm::setReduceMotion,
                    setNormalize = vm::setNormalize,
                    setTargetDb = vm::setTargetDb,
                    reapplyAll = vm::reapplyAll,
                    setThemeMode = vm::setThemeMode,
                    setDynamic = vm::setDynamic,
                    setPalette = vm::setPalette,
                    restartSysUiNow = vm::restartSysUiNow,
                    setSystemToggle = vm::setSystemToggle,
                    installModule = vm::installModule,
                    reboot = vm::reboot,
                    buildReport = vm::buildReport,
                    clearReport = vm::clearReport,
                    dismissIntro = vm::dismissIntro,
                    showIntro = vm::showIntroAgain,
                    setDebugLog = vm::setDebugLog,
                    setForceLock = vm::setForceLock,
                    confirmRestartPrompt = vm::confirmRestartPrompt,
                    dismissRestartPrompt = vm::dismissRestartPrompt
                )
            }

            SoundModTheme(
                themeMode = state.themeMode,
                dynamic = state.dynamicColor,
                palette = state.palette,
                reduceMotion = state.reduceMotion
            ) {
                SoundModScreen(state = state, player = player, snackbar = snackbar, a = actions)
            }
        }
    }
}
