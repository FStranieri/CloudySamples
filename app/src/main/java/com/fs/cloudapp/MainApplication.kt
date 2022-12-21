package com.fs.cloudapp

import android.app.Application
import com.fs.cloudapp.repositories.AuthRepository
import com.fs.cloudapp.repositories.CloudDBRepository
import com.fs.cloudapp.repositories.PushTokenRepository
import com.huawei.agconnect.auth.AGConnectAuth

class MainApplication : Application() {

    private lateinit var authInstance: AGConnectAuth
    lateinit var authRepository: AuthRepository
    lateinit var cloudDBRepository: CloudDBRepository
    lateinit var pushTokenRepository: PushTokenRepository

    override fun onCreate() {
        super.onCreate()
        initSingletons()
    }

    /**
     * This is ripe for D.I.
     */
    private fun initSingletons() {
        authInstance = AGConnectAuth.getInstance()
        authRepository = AuthRepository(authInstance)
        cloudDBRepository = CloudDBRepository(
            this,
            authInstance
        )
        pushTokenRepository = PushTokenRepository(this)
    }

}