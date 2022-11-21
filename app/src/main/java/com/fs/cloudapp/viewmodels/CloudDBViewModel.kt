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

//renamed all the object types with more human names
typealias Message = input_messages
typealias User = users
typealias FullMessage = full_messages
typealias PollLunchChoices = poll_lunch_choices
typealias PollLunch = poll_lunch
typealias PushTokens = user_push_tokens

/**
 * This ViewModel includes all the functions to store and retrieve data from Cloud DB
 */
class CloudDBViewModel : ViewModel() {

    //main object instance for Cloud DB
    private lateinit var DBInstance: AGConnectCloudDB

    //DBZone object where we are storing and retrieving data
    private var DBZone: CloudDBZone? = null

    //The MutableStateFlow that we need to manage the UI changes with Compose
    private val mState = MutableStateFlow(CloudState())
    val state: StateFlow<CloudState>
        get() = mState

    //The userID of the user logged in the chat
    var userID: String = ""
        private set

    //list of chat messages to show
    var messages: MutableLiveData<List<FullMessage>> = MutableLiveData()
        private set

    //list of restaurants to choose in order to organize a lunch
    var lunchChoices: MutableLiveData<List<PollLunchChoices>> = MutableLiveData()
        private set

    //this listener alerts the app everytime there's a change in the full_messages Object Type
    private val mSnapshotListener = OnSnapshotListener<FullMessage> { cloudDBZoneSnapshot, err ->
        err?.let {
            Log.w(TAG, "onSnapshot: ${err.message}")
        } ?: processQueryResult(cloudDBZoneSnapshot)
    }

    //temporary data about the message we are going to modify
    var messageToEdit: MutableState<FullMessage?> = mutableStateOf(null)

    //create the Cloud DB instance and establish the connection with it
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

    //create an instance of the Cloud DB Zone we are going to use to store and retrieve data
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

    /*
    in order to save data into 'users' Object Type but It's not used since when the user login
    is triggered, the cloud function stores the data in DB
     */
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

    //in order to save the push token into 'user_push_tokens' Object Type
    fun savePushToken(pushToken: PushTokens) {
        val upsertTask = this.DBZone!!.executeUpsert(pushToken)
        upsertTask.addOnSuccessListener { cloudDBZoneResult ->
            Log.i(TAG, "Upsert $cloudDBZoneResult records")
            userID = pushToken.user_id
        }.addOnFailureListener {
            val err = it as AGConnectCloudDBException
            Log.e(TAG, "${err.code}: ${err.message}")
        }
    }

    /*
    in order to send message to the Cloud DB input_messages Object Type, it will start the
    chat flow
     */
    fun sendMessage(text: String) {
        val message = Message().apply {
            this.id = ""
            this.text = text
            this.user_id = userID
            this.type = ObjectTypeInfoHelper.MESSAGE_TYPE_STANDARD
        }

        sendMessageOnCloud(message)
    }

    //in order to edit a message we will use the same id but with updated data
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

    //this function execute an upsert on the input_messages ObjectType
    private fun sendMessageOnCloud(message: Message) {
        val upsertTask = this.DBZone!!.executeUpsert(message)
        upsertTask.addOnSuccessListener { cloudDBZoneResult ->
            Log.i(TAG, "Upsert $cloudDBZoneResult records")
        }.addOnFailureListener {
            val err = it as AGConnectCloudDBException
            Log.e(TAG, "${err.code}: ${err.message}")
        }
    }

    /*
    this function execute a query to retrieve all the messages and then subscribe a listener
    in order to get all the data changes about the same Object Type. In this way everytime we
    have a new message and/or a deleted messages and/or an updated one, we get automatically
    new data shown in the app
     */
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
        queryTask.addOnSuccessListener { snapshot ->
            processQueryResult(snapshot)

            DBZone!!.subscribeSnapshot(
                query,
                CloudDBZoneQuery.CloudDBZoneQueryPolicy.POLICY_QUERY_FROM_CLOUD_ONLY,
                mSnapshotListener
            )
        }
            .addOnFailureListener {
                updateState(state.value.copy(failureOutput = it))
            }
    }

    //to convert the result into a list to set in the LazyColumn
    private fun processQueryResult(snapshot: CloudDBZoneSnapshot<FullMessage>) {
        val messagesList = snapshot.toList()
        messages.postValue(messagesList.sortedBy { it.date_ins })
    }

    //task to delete a message
    fun deleteMessage(message: FullMessage) {
        val deleteTask = this.DBZone!!.executeDelete(message)
        deleteTask.addOnSuccessListener {
            Log.i(TAG, "Delete message ${message.id} succeed!")
        }.addOnFailureListener {
            Log.e(TAG, "Delete message error: ", it)
        }
    }

    //not used since when the user logout is triggered, the cloud function deletes the data in DB
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

    //this function will check if there's the user data into the related Object Type
    fun getUserDataAvailability() {
        val query = CloudDBZoneQuery.where(User::class.java)
            .equalTo("id", this.userID)

        val queryTask = this.DBZone!!.executeQuery(
            query,
            CloudDBZoneQuery.CloudDBZoneQueryPolicy.POLICY_QUERY_FROM_CLOUD_ONLY
        )

        queryTask.addOnSuccessListener { snapshot ->
            updateState(
                mState.value.copy(
                    userDataAvailable =
                    (snapshot.snapshotObjects?.size() ?: 0) > 0
                )
            )
        }.addOnFailureListener {
            updateState(state.value.copy(failureOutput = it))
        }
    }

    //In order to reset the failure data status after a successful retry
    fun resetFailureOutput() {
        updateState(state.value.copy(failureOutput = null))
    }

    //query to retrieve all the available restaurants in the related Object Type
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

    //store the restaurant choice by the user into the related Object Type
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

    //with Compose we are setting the visibility of the restaurants list
    fun setLunchChoicesVisibility(visible: Boolean) {
        if (visible) {
            getPollLunchChoices()
        }

        updateState(mState.value.copy(showLunchChoices = visible))
    }

    //extended function to simplify the snapshot parsing to a mutablelist ready for the LazyColumn
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

    //actually not used in order to keep it alive
    fun closeDB() {
        if (this::DBInstance.isInitialized && this.DBZone != null) {
            this.DBInstance.closeCloudDBZone(this.DBZone)
        }
    }

    //Update the MutableStateFlow data for UI changes based on events
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
        val userDataAvailable: Boolean = false,
        val showLunchChoices: Boolean = false,
        val failureOutput: Exception? = null
    )
}

