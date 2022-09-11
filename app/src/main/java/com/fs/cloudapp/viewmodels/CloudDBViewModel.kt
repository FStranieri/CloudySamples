package com.fs.cloudapp.viewmodels

import android.content.Context
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fs.cloudapp.data.*
import com.huawei.agconnect.AGCRoutePolicy
import com.huawei.agconnect.AGConnectInstance
import com.huawei.agconnect.AGConnectOptionsBuilder
import com.huawei.agconnect.auth.AGConnectAuth
import com.huawei.agconnect.auth.AGConnectUser
import com.huawei.agconnect.cloud.database.*
import com.huawei.agconnect.cloud.database.exceptions.AGConnectCloudDBException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.Exception

typealias Message = input_messages
typealias User = users
typealias FullMessage = full_messages
typealias PollLunchChoices = poll_lunch_choices
typealias PollLunch = poll_lunch

class CloudDBViewModel : ViewModel() {

    private lateinit var DBInstance: AGConnectCloudDB
    private var DBZone: CloudDBZone? = null

    private val mState = MutableStateFlow(CloudState())
    val state: StateFlow<CloudState>
        get() = mState

    var userID: String = ""
        private set

    var messages: MutableLiveData<List<FullMessage>> = MutableLiveData()
    private set

    var lunchChoices: MutableLiveData<List<PollLunchChoices>> = MutableLiveData()
    private set

    private var loadingProgress: MutableState<Boolean> = mutableStateOf(false)

    private val mSnapshotListener = OnSnapshotListener<FullMessage> { cloudDBZoneSnapshot, err ->
        err?.let {
            Log.w(TAG, "onSnapshot: " + err.message)
        } ?: processQueryResult(cloudDBZoneSnapshot)
    }

    var messageToEdit: MutableState<FullMessage?> = mutableStateOf(null)

    fun initAGConnectCloudDB(
        context: Context,
        authInstance: AGConnectAuth
    ) {
        this.userID = authInstance.currentUser.uid

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
            updateState(mState.value.copy(dbReady = true))
        }.addOnFailureListener {
            Log.e(TAG, "${it.message}")
        }
    }

    fun saveUser(credentials: AGConnectUser) {
        val user = User().apply {
            id = credentials.uid
            nickname = credentials.displayName
            email = credentials.email
            phone_number = credentials.phone
            picture_url = credentials.photoUrl
            provider_id = credentials.providerId
        }

        val upsertTask = this.DBZone!!.executeUpsert(user)
        upsertTask.addOnSuccessListener { cloudDBZoneResult ->
            Log.i(TAG, "Upsert users $cloudDBZoneResult records")
        }.addOnFailureListener {
            val err = it as AGConnectCloudDBException
            Log.e(TAG, "${err.code}: ${err.message}")
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
        val message = Message().apply {
            this.id = ""
            this.text = text
            this.user_id = userID
            this.type = ObjectTypeInfoHelper.MESSAGE_TYPE_STANDARD
        }

        sendMessageOnCloud(message)
    }

    fun editMessage(text: String, fullMessage: FullMessage) {
        val message = Message().apply {
            this.id = fullMessage.id
            this.text = text
            this.user_id = fullMessage.user_id
            this.type = fullMessage.type
        }

        this.messageToEdit.value = null

        sendMessageOnCloud(message)
    }

    private fun sendMessageOnCloud(message: Message) {
        val upsertTask = this.DBZone!!.executeUpsert(message)
        upsertTask.addOnSuccessListener { cloudDBZoneResult ->
            Log.i(TAG, "Upsert $cloudDBZoneResult records")
        }.addOnFailureListener {
            val err = it as AGConnectCloudDBException
            Log.e(TAG, "${err.code}: ${err.message}")
        }
    }

    fun getAllMessages() {
        val query = CloudDBZoneQuery.where(FullMessage::class.java)
            .equalTo("type", ObjectTypeInfoHelper.MESSAGE_TYPE_STANDARD)
            .or()
            .equalTo("type", ObjectTypeInfoHelper.MESSAGE_TYPE_POLL)
        //not supported by the subscription
        //.orderByDesc("date_ins")

        val queryTask = this.DBZone!!.executeQuery(
            query,
            CloudDBZoneQuery.CloudDBZoneQueryPolicy.POLICY_QUERY_DEFAULT
        )
        queryTask.addOnSuccessListener { snapshot -> processQueryResult(snapshot) }
            .addOnFailureListener {
                updateState(state.value.copy(failureOutput = it))
            }

        this.DBZone!!.subscribeSnapshot(
            query,
            CloudDBZoneQuery.CloudDBZoneQueryPolicy.POLICY_QUERY_FROM_CLOUD_ONLY, mSnapshotListener
        )
    }

    private fun processQueryResult(snapshot: CloudDBZoneSnapshot<FullMessage>) {
        val messagesList = snapshot.toList()
        messages.postValue(messagesList.sortedBy { it.date_ins })
    }

    fun deleteMessage(message: FullMessage) {
        val deleteTask = this.DBZone!!.executeDelete(message)
        deleteTask.addOnSuccessListener {
            Log.i(TAG, "Delete message ${message.id} succeed!")
        }.addOnFailureListener {
            Log.e(TAG, "Delete message error: ", it)
        }
    }

    fun deleteUser(id: String) {
        val userToDelete = User().apply {
            this.id = id
        }

        val deleteTask = this.DBZone!!.executeDelete(userToDelete)
        deleteTask.addOnSuccessListener {
            Log.i(TAG, "Delete user ${userToDelete.id} succeed!")
        }.addOnFailureListener {
            Log.e(TAG, "Delete user error: ", it)
        }
    }

    fun resetFailureOutput() {
        updateState(state.value.copy(failureOutput = null))
    }

    private fun getPollLunchChoices() {
        val query = CloudDBZoneQuery.where(PollLunchChoices::class.java)

        val queryTask = this.DBZone!!.executeQuery(
            query,
            CloudDBZoneQuery.CloudDBZoneQueryPolicy.POLICY_QUERY_DEFAULT
        )

        queryTask.addOnSuccessListener { snapshot ->
            lunchChoices.postValue(snapshot.toList())
        }.addOnFailureListener {
            updateState(state.value.copy(failureOutput = it))
        }
    }

    fun sendPollLunchChoice(pollLunchChoice: PollLunchChoices) {
        val upsertTask = this.DBZone!!.executeUpsert(PollLunch().apply {
            user_id = userID
            choice = pollLunchChoice.name
        })
        upsertTask.addOnSuccessListener { cloudDBZoneResult ->
            Log.i(TAG, "Upsert $cloudDBZoneResult records")
        }.addOnFailureListener {
            val err = it as AGConnectCloudDBException
            Log.e(TAG, "${err.code}: ${err.message}")
        }
    }

    fun setLunchChoicesVisibility(visible: Boolean) {
        if (visible) {
            getPollLunchChoices()
        }

        updateState(mState.value.copy(showLunchChoices = visible))
    }

    private fun <T : CloudDBZoneObject> CloudDBZoneSnapshot<T>.toList() = run {
        val cursor = this.snapshotObjects
        val list = mutableListOf<T>()

        try {
            while (cursor.hasNext()) {
                list.add(cursor.next())
            }
        } catch (e: AGConnectCloudDBException) {
            Log.w(TAG, "processQueryResult: " + e.message)
        } finally {
            this.release()
        }

        list
    }

    fun getLoadingProgress(): MutableState<Boolean> {
        return loadingProgress
    }

    fun closeDB() {
        if (this::DBInstance.isInitialized && this.DBZone != null) {
            this.DBInstance.closeCloudDBZone(this.DBZone)
        }
    }

    private fun updateState(newState: CloudState) {
        viewModelScope.launch {
            mState.emit(value = newState)
        }
    }

    companion object {
        const val TAG = "CloudDBViewModel"
    }

    data class CloudState(
        val dbReady: Boolean = false,
        val showLunchChoices: Boolean = false,
        val failureOutput: Exception? = null
    )
}

