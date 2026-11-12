package com.cyberdrop.transfer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.cyberdrop.transfer.ui.CyberdropRoot
import com.cyberdrop.transfer.transfer.TransferEngine

class MainActivity : ComponentActivity() {

    private val engine: TransferEngine by lazy { (application as CyberdropApp).engine }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        engine.start()
        setContent { CyberdropRoot(engine) }
    }
}
