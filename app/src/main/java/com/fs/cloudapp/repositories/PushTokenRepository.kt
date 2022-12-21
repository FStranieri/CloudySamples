package com.fs.cloudapp.repositories

import android.app.Application
import com.fs.cloudapp.R
import com.huawei.hms.aaid.HmsInstanceId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PushTokenRepository(
    private val application: Application
) {

    suspend fun getPushToken(): String {
        return withContext(Dispatchers.IO) {
            // Obtain the app ID from the agconnect-services.json file.
            val appId = application.resources.getString(R.string.app_id)
            HmsInstanceId.getInstance(application).getToken(appId, "HCM").apply {
                if(this == null) {
                    throw IllegalStateException("Generated push token is null.")
                }
            }
        }
    }
}