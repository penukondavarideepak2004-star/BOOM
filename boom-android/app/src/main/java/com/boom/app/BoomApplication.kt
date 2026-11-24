package com.boom.app

import android.app.Application
import com.boom.core.database.AndroidSecureSessionStorage
import com.boom.core.database.SessionStorage
import com.boom.core.security.BoomGuardian

class BoomApplication : Application() {

    lateinit var sessionStorage: SessionStorage
        private set

    lateinit var guardian: BoomGuardian
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        sessionStorage = AndroidSecureSessionStorage(this)
        guardian = BoomGuardian()
    }

    companion object {
        lateinit var instance: BoomApplication
            private set
    }
}
