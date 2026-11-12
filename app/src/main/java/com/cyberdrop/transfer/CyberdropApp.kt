package com.cyberdrop.transfer

import android.app.Application
import com.cyberdrop.transfer.transfer.TransferEngine

class CyberdropApp : Application() {

    lateinit var engine: TransferEngine
        private set

    override fun onCreate() {
        super.onCreate()
        engine = TransferEngine(this)
    }
}
