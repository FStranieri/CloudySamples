package com.fs.cloudapp.viewmodels

import android.content.Context
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.fs.cloudapp.data.ObjectTypeInfoHelper
import com.fs.cloudapp.data.user_push_tokens
import com.huawei.agconnect.AGCRoutePolicy
import com.huawei.agconnect.AGConnectInstance
import com.huawei.agconnect.AGConnectOptionsBuilder
import com.huawei.agconnect.auth.AGCAuthException
import com.huawei.agconnect.auth.AGConnectAuth
import com.huawei.agconnect.cloud.database.AGConnectCloudDB
import com.huawei.agconnect.cloud.database.CloudDBZone
import com.huawei.agconnect.cloud.database.CloudDBZoneConfig
import com.huawei.agconnect.cloud.database.exceptions.AGConnectCloudDBException
import java.lang.Exception

class CloudDBViewModel: ViewModel() {

    lateinit var DBInstance: AGConnectCloudDB
    lateinit var DBZone: CloudDBZone

    private var output: MutableLiveData<String> = MutableLiveData()
    private var failureOutput: MutableLiveData<Exception> = MutableLiveData()

    var canRegisterPushToken: MutableState<Boolean> = mutableStateOf(false)
        private set

    fun initAGConnectCloudDB(context: Context) {
        AGConnectAuth.getInstance().signInAnonymously().addOnSuccessListener {
            // onSuccess
            val user = it.user
            AGConnectCloudDB.initialize(context)
            val agcConnectOptions = AGConnectOptionsBuilder()
                .setRoutePolicy(AGCRoutePolicy.GERMANY)
                .build(context)
            val agConnectInstance = AGConnectInstance.buildInstance(agcConnectOptions)
            this.DBInstance = AGConnectCloudDB.getInstance(
                agConnectInstance,
                AGConnectAuth.getInstance(agConnectInstance)
            )

            this.DBInstance.createObjectType(ObjectTypeInfoHelper.getObjectTypeInfo())
            openCloudZone()
        }.addOnFailureListener {
            val err = it as AGCAuthException
            Log.e(TAG, "${err.code}: ${err.message}")
        }
    }

    private fun openCloudZone() {
        val mConfig = CloudDBZoneConfig(
            "DBZone0",
            CloudDBZoneConfig.CloudDBZoneSyncProperty.CLOUDDBZONE_CLOUD_CACHE,
            CloudDBZoneConfig.CloudDBZoneAccessProperty.CLOUDDBZONE_PUBLIC
        ).apply {
            persistenceEnabled = true
        }

        this.DBInstance.openCloudDBZone2(mConfig, true).addOnSuccessListener {
            DBZone = it
            canRegisterPushToken.value = true
        }.addOnFailureListener {
            Log.e(TAG, "${it.message}")
        }
    }

    fun savePushToken(pushToken: user_push_tokens) {
        val upsertTask = this.DBZone.executeUpsert(pushToken)
        upsertTask.addOnSuccessListener { cloudDBZoneResult ->
            Log.i(TAG, "Upsert $cloudDBZoneResult records")
        }.addOnFailureListener {
            val err = it as AGConnectCloudDBException
            Log.e(TAG, "${err.code}: ${err.message}")
        }
    }

    fun getOutput(): LiveData<String> {
        return output
    }

    fun getFailureOutput(): LiveData<Exception> {
        return failureOutput
    }

    fun resetFailureOutput() {
        this.failureOutput.value = null
    }

    companion object {
        const val TAG = "CloudDBViewModel"
    }
}