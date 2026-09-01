package com.vcapp.voicechanger

import android.app.Application
import com.vcapp.voicechanger.service.EngineController

class VcApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        EngineController.init(this)
    }
}
