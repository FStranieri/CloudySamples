package com.fs.cloudapp

import android.os.Bundle
import android.util.Log
import com.huawei.hms.push.HmsMessageService

class HUAWEIPushService: HmsMessageService() {

    override fun onNewToken(token: String?, bundle: Bundle?) {
        // Obtain a push token.
        Log.i(TAG, "have received refresh token:$token")

        // Check whether the token is null.
        if (token?.isNotEmpty() == true) {
            refreshedTokenToServer(token)
        }
    }

    private fun refreshedTokenToServer(token: String) {
        Log.i(TAG, "sending token to server. token:$token")
    }
}