package com.cyberdrop.transfer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cyberdrop.transfer.transfer.TransferEngine
import com.cyberdrop.transfer.ui.components.CyberBackground
import com.cyberdrop.transfer.ui.screens.HomeScreen
import com.cyberdrop.transfer.ui.screens.ReceiveScreen
import com.cyberdrop.transfer.ui.screens.SendScreen
import com.cyberdrop.transfer.ui.screens.SettingsScreen

enum class Dest { HOME, SEND, RECEIVE, SETTINGS }

@Composable
fun CyberdropRoot(engine: TransferEngine) {
    val state by engine.state.collectAsStateWithLifecycle()
    var dest by remember { mutableStateOf(Dest.HOME) }
    val view = LocalView.current

    DisposableEffect(state.busy) {
        view.keepScreenOn = state.busy
        onDispose { view.keepScreenOn = false }
    }

    BackHandler(enabled = dest != Dest.HOME) { dest = Dest.HOME }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        CyberBackground {
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                when (dest) {
                    Dest.HOME -> HomeScreen(
                        state = state,
                        myId = engine.myId,
                        onSend = { dest = Dest.SEND },
                        onReceive = { dest = Dest.RECEIVE },
                        onAcceptRequest = {
                            engine.acceptIncoming()
                            dest = Dest.RECEIVE
                        },
                        onRejectRequest = { engine.rejectIncoming() },
                        onSettings = { dest = Dest.SETTINGS },
                        onCancel = { engine.cancel() },
                        onDismiss = { engine.dismissFinished() },
                    )

                    Dest.SEND -> SendScreen(
                        state = state,
                        myId = engine.myId,
                        maxBytes = engine.prefs.maxBytes,
                        describe = { uri -> engine.describe(uri) },
                        onSend = { uri, id -> engine.sendFile(uri, id) },
                        onCancel = { engine.cancel() },
                        onDismiss = { engine.dismissFinished() },
                        onBack = { dest = Dest.HOME },
                    )

                    Dest.RECEIVE -> ReceiveScreen(
                        state = state,
                        myId = engine.myId,
                        onRetry = { engine.retryOnline() },
                        onAccept = { engine.acceptIncoming() },
                        onReject = { engine.rejectIncoming() },
                        onCancel = { engine.cancel() },
                        onDismiss = { engine.dismissFinished() },
                        onBack = { dest = Dest.HOME },
                    )

                    Dest.SETTINGS -> SettingsScreen(
                        engine = engine,
                        myId = engine.myId,
                        onBack = { dest = Dest.HOME },
                    )
                }
            }
        }
    }
}
