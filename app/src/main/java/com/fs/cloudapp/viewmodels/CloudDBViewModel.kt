package com.fs.cloudapp.viewmodels

import android.content.Context
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.fs.cloudapp.data.ObjectTypeInfoHelper
import com.fs.cloudapp.data.messages
import com.fs.cloudapp.data.user_push_tokens
import com.huawei.agconnect.AGCRoutePolicy
import com.huawei.agconnect.AGConnectInstance
import com.huawei.agconnect.AGConnectOptionsBuilder
import com.huawei.agconnect.auth.AGCAuthException
import com.huawei.agconnect.auth.AGConnectAuth
import com.huawei.agconnect.auth.SignInResult
import com.huawei.agconnect.cloud.database.*
import com.huawei.agconnect.cloud.database.exceptions.AGConnectCloudDBException
import java.lang.Exception

typealias Message = messages

class CloudDBViewModel : ViewModel() {

    private lateinit var DBInstance: AGConnectCloudDB
    private var DBZone: CloudDBZone? = null

    var userID: String = ""
        private set

    private var messageId: Long = 0L

    private var output: MutableLiveData<String> = MutableLiveData()
    private var messages: MutableLiveData<List<Message>> = MutableLiveData()
    private var failureOutput: MutableLiveData<Exception> = MutableLiveData()
    private var loadingProgress: MutableState<Boolean> = mutableStateOf(false)

    var canRegisterPushToken: MutableState<Boolean> = mutableStateOf(false)
        private set

    private val mSnapshotListener = OnSnapshotListener<Message> { cloudDBZoneSnapshot, e ->
        if (e != null) {
            Log.w(TAG, "onSnapshot: " + e.message)
            return@OnSnapshotListener
        }
        val snapshotObjects = cloudDBZoneSnapshot.snapshotObjects
        val messagesList: MutableList<Message> = ArrayList()
        try {
            if (snapshotObjects != null) {
                while (snapshotObjects.hasNext()) {
                    val message = snapshotObjects.next()

                    if (messageId <= message.id) {
                        messageId = message.id
                    }

                    messagesList.add(message)
                }
            }
            this.messages.postValue(messagesList)
        } catch (snapshotException: AGConnectCloudDBException) {
            Log.w(TAG, "onSnapshot:(getObject) " + snapshotException.message)
        } finally {
            cloudDBZoneSnapshot.release()
        }
    }

    fun initAGConnectCloudDB(
        context: Context,
        authInstance: AGConnectAuth,
        credentials: SignInResult
    ) {
        this.userID = credentials.user.displayName

        if (DBZone == null) {
            AGConnectCloudDB.initialize(context)
            val agcConnectOptions = AGConnectOptionsBuilder()
                .setRoutePolicy(AGCRoutePolicy.GERMANY)
                .build(context)
            val agConnectInstance = AGConnectInstance.buildInstance(agcConnectOptions)
            this.DBInstance = AGConnectCloudDB.getInstance(
                agConnectInstance,
                authInstance
            )
            this.DBInstance.createObjectType(ObjectTypeInfoHelper.getObjectTypeInfo())
            openCloudZone()
        }
    }

    private fun openCloudZone() {
        val mConfig = CloudDBZoneConfig(
            "ChatDemo",
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
        val upsertTask = this.DBZone!!.executeUpsert(pushToken)
        upsertTask.addOnSuccessListener { cloudDBZoneResult ->
            Log.i(TAG, "Upsert $cloudDBZoneResult records")
            userID = pushToken.user_id
        }.addOnFailureListener {
            val err = it as AGConnectCloudDBException
            Log.e(TAG, "${err.code}: ${err.message}")
        }
    }

    fun sendMessage(text: String) {
        messageId++

        val message = Message().apply {
            this.id = messageId
            this.text = text
            this.user_id = userID
            this.type = 0
        }

        val upsertTask = this.DBZone!!.executeUpsert(message)
        upsertTask.addOnSuccessListener { cloudDBZoneResult ->
            Log.i(TAG, "Upsert $cloudDBZoneResult records")
        }.addOnFailureListener {
            val err = it as AGConnectCloudDBException
            Log.e(TAG, "${err.code}: ${err.message}")
        }
    }

    fun getAllMessages() {
        val query = CloudDBZoneQuery.where(Message::class.java).equalTo("type", 0)
        val queryTask = this.DBZone!!.executeQuery(
            query,
            CloudDBZoneQuery.CloudDBZoneQueryPolicy.POLICY_QUERY_FROM_CLOUD_ONLY
        )
        queryTask.addOnSuccessListener { snapshot -> processQueryResult(snapshot) }
            .addOnFailureListener {
                failureOutput.value = it
            }

        this.DBZone!!.subscribeSnapshot(
            query,
            CloudDBZoneQuery.CloudDBZoneQueryPolicy.POLICY_QUERY_FROM_CLOUD_ONLY, mSnapshotListener
        )
    }

    private fun processQueryResult(snapshot: CloudDBZoneSnapshot<Message>) {
        val messagesCursor = snapshot.snapshotObjects
        val messagesList: MutableList<messages> = ArrayList()
        try {
            while (messagesCursor.hasNext()) {
                val message = messagesCursor.next()

                if (messageId <= message.id) {
                    messageId = message.id
                }

                messagesList.add(message)
            }
        } catch (e: AGConnectCloudDBException) {
            Log.w(TAG, "processQueryResult: " + e.message)
        } finally {
            snapshot.release()
        }

        messages.value = messagesList
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

    fun getChatMessages(): LiveData<List<Message>> {
        return this.messages
    }

    fun getLoadingProgress(): MutableState<Boolean> {
        return loadingProgress
    }

    fun closeDB() {
        if (this::DBInstance.isInitialized && this.DBZone != null) {
            this.DBInstance.closeCloudDBZone(this.DBZone)
        }
    }

    companion object {
        const val TAG = "CloudDBViewModel"
    }
}