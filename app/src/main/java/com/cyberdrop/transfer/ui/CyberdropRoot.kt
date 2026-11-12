package com.cyberdrop.transfer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cyberdrop.transfer.transfer.Direction
import com.cyberdrop.transfer.transfer.Phase
import com.cyberdrop.transfer.transfer.TransferEngine
import com.cyberdrop.transfer.transfer.TransferUi
import com.cyberdrop.transfer.ui.components.CyberBackground
import com.cyberdrop.transfer.ui.screens.HomeScreen
import com.cyberdrop.transfer.ui.screens.ReceiveScreen
import com.cyberdrop.transfer.ui.screens.SendScreen
import com.cyberdrop.transfer.ui.screens.SettingsScreen
import com.cyberdrop.transfer.ui.screens.TransferBanner
import com.cyberdrop.transfer.ui.screens.TransferScreen

enum class Dest { HOME, SEND, RECEIVE, SETTINGS }

@Composable
fun CyberdropRoot(engine: TransferEngine) {
    val state by engine.state.collectAsStateWithLifecycle()
    var dest by remember { mutableStateOf(Dest.HOME) }
    var minimized by remember { mutableStateOf<String?>(null) }
    val view = LocalView.current

    // هر انتقالی (در جریان یا تازه تمام‌شده) صفحهٔ اختصاصی خودش را نشان می‌دهد.
    val requestFocus: TransferUi? = state.request?.let { r ->
        state.incoming ?: TransferUi(
            tid = r.tid,
            direction = Direction.RECEIVE,
            peerId = r.fromId,
            fileName = r.fileName,
            size = r.size,
            bytes = r.resumeOffset,
            phase = Phase.WAITING_ACCEPT,
        )
    }
    val focus: TransferUi? = requestFocus
        ?: listOfNotNull(state.outgoing, state.incoming).firstOrNull { it.active }
        ?: state.incoming
        ?: state.outgoing

    DisposableEffect(state.busy) {
        view.keepScreenOn = state.busy
        onDispose { view.keepScreenOn = false }
    }

    BackHandler(enabled = focus != null || dest != Dest.HOME) {
        when {
            focus == null -> dest = Dest.HOME
            focus.active && state.request == null -> minimized = focus.tid
            !focus.active -> engine.dismissFinished()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        CyberBackground {
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                val showing = focus?.takeIf { minimized != it.tid }
                if (showing != null) {
                    TransferScreen(
                        t = showing,
                        request = state.request,
                        onAccept = { engine.acceptIncoming() },
                        onReject = { engine.rejectIncoming() },
                        onCancel = { engine.cancel() },
                        onDismiss = { engine.dismissFinished() },
                        onMinimize = { minimized = showing.tid },
                    )
                } else {
                    if (focus != null) {
                        TransferBanner(focus) { minimized = null }
                    }
                    when (dest) {
                        Dest.HOME -> HomeScreen(
                            state = state,
                            myId = engine.myId,
                            onSend = { dest = Dest.SEND },
                            onReceive = { dest = Dest.RECEIVE },
                            onSettings = { dest = Dest.SETTINGS },
                        )

                        Dest.SEND -> SendScreen(
                            state = state,
                            myId = engine.myId,
                            maxBytes = engine.prefs.maxBytes,
                            describe = { uri -> engine.describe(uri) },
                            onSend = { uri, id -> engine.sendFile(uri, id) },
                            onResumeLast = { engine.resendLast() },
                            onDiscardResume = { engine.discardResume() },
                            onCancel = { engine.cancel() },
                            onDismiss = { engine.dismissFinished() },
                            onBack = { dest = Dest.HOME },
                        )

                        Dest.RECEIVE -> ReceiveScreen(
                            state = state,
                            myId = engine.myId,
                            onRetry = { engine.retryOnline() },
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
}
