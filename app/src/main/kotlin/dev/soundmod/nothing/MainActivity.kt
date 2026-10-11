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

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by vm.state.collectAsStateWithLifecycle()
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

            SoundModTheme(reduceMotion = state.reduceMotion) {
                SoundModScreen(
                    state = state,
                    snackbar = snackbar,
                    onRefresh = vm::refreshAll,
                    onReloadLibrary = vm::loadLibrary,
                    onPreviewPath = vm::preview,
                    onTestFx = vm::testEffect,
                    onPickSystem = vm::applySystem,
                    onPickFile = { slot ->
                        pendingSlot = slot
                        picker.launch(arrayOf("audio/*"))
                    },
                    onReset = vm::reset,
                    onReduceMotion = vm::setReduceMotion,
                    onNormalize = vm::setNormalize,
                    onTargetDb = vm::setTargetDb,
                    onInstallModule = vm::installModule,
                    onReboot = vm::reboot
                )
            }
        }
    }
}
