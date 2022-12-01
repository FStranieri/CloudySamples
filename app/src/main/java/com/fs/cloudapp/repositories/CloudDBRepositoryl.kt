package com.fs.cloudapp.repositories

import android.app.Application
import android.util.Log
import com.fs.cloudapp.TAG
import com.fs.cloudapp.await
import com.fs.cloudapp.data.*
import com.huawei.agconnect.AGCRoutePolicy
import com.huawei.agconnect.AGConnectInstance
import com.huawei.agconnect.AGConnectOptionsBuilder
import com.huawei.agconnect.auth.AGConnectAuth
import com.huawei.agconnect.auth.AGConnectUser
import com.huawei.agconnect.cloud.database.*
import com.huawei.agconnect.cloud.database.exceptions.AGConnectCloudDBException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.CancellationException

//renamed all the object types with more human names
typealias Message = input_messages
typealias User = users
typealias FullMessage = full_messages
typealias PollLunchChoice = poll_lunch_choices
typealias PollLunch = poll_lunch
typealias PushTokens = user_push_tokens

/**
 * Contains all the functions to store and retrieve data from Cloud DB.
 * The constructor will initiate the connection to the Cloud Zone and users can listen to the connection status
 * via observeConnectionState().
 */
class CloudDBRepository(
    application: Application,
    private val authInstance: AGConnectAuth
) {

    private val dbInstance: AGConnectCloudDB
    private lateinit var dbZone: CloudDBZone
    private lateinit var allMessagesListener: ListenerHandler
    private val initJob: Job

    private val _allMessages: MutableStateFlow<List<FullMessage>> = MutableStateFlow(emptyList())
    fun observeAllMessages(): StateFlow<List<FullMessage>> = _allMessages.asStateFlow()

    private val _connectionState: MutableStateFlow<CloudState> =
        MutableStateFlow(CloudState.Disconnected)

    fun observeConnectionState(): StateFlow<CloudState> = _connectionState.asStateFlow()

    init {
        AGConnectCloudDB.initialize(application)
        val agcConnectOptions = AGConnectOptionsBuilder()
            .setRoutePolicy(AGCRoutePolicy.GERMANY)
            .build(application)
        val agConnectInstance = AGConnectInstance.buildInstance(agcConnectOptions)
        dbInstance = AGConnectCloudDB.getInstance(
            agConnectInstance,
            authInstance
        )
        dbInstance.createObjectType(ObjectTypeInfoHelper.getObjectTypeInfo())

        val cloudInitExceptionHandler =
            CoroutineExceptionHandler { _, throwable ->
                Log.e(this.TAG, "Failed to init cloud repo with: $throwable")
                _connectionState.update { CloudState.ConnectionFailed(throwable) }
            }

        initJob = MainScope().launch(cloudInitExceptionHandler) {
            _connectionState.emit(CloudState.Connecting)
            openCloudZone()
            _connectionState.emit(CloudState.Connected)
            listenForAllMessages()
        }
    }

    private suspend fun openCloudZone() {
        dbZone = dbInstance.openCloudDBZone2(buildCloudConfig(), true).await()
    }

    private fun buildCloudConfig(): CloudDBZoneConfig {
        val config = CloudDBZoneConfig(
            "ChatDemo",
            CloudDBZoneConfig.CloudDBZoneSyncProperty.CLOUDDBZONE_CLOUD_CACHE,
            CloudDBZoneConfig.CloudDBZoneAccessProperty.CLOUDDBZONE_PUBLIC
        )
        config.persistenceEnabled = true
        return config
    }

    fun closeCloudZone() {
        if (initJob.isActive) {
            initJob.cancel(CancellationException("Canceled by user"))
        }

        _connectionState.update {
            if (it is CloudState.Connected) {
                allMessagesListener.remove()
                dbInstance.closeCloudDBZone(dbZone)
            }
            CloudState.Disconnected
        }
    }

    /*
    in order to save data into 'users' Object Type but It's not used since when the user login
    is triggered, the cloud function stores the data in DB
     */
    suspend fun saveUser(credentials: AGConnectUser) {
        val user = User().apply {
            id = credentials.uid
            nickname = credentials.displayName
            email = credentials.email
            phone_number = credentials.phone
            picture_url = credentials.photoUrl
            provider_id = credentials.providerId
        }
        Log.d(
            TAG,
            "Saving user ${user.nickname} (${user.id})"
        )
        dbZone.executeUpsert(user).await()
    }

    //in order to save the push token into 'user_push_tokens' Object Type
    suspend fun savePushToken(pushToken: String) {
        val userPushToken = PushTokens().apply {
            token = pushToken
            user_id = checkNotNull(authInstance.currentUser).uid
            platform = 0
        }
        Log.d(
            TAG,
            "Saving push token $pushToken${userPushToken.token} for user ${userPushToken.user_id} "
        )
        dbZone.executeUpsert(userPushToken).await()
    }

    /*
    in order to send message to the Cloud DB input_messages Object Type, it will start the
    chat flow
     */
    suspend fun sendMessage(text: String) {
        val message = Message().apply {
            this.id = ""
            this.text = text
            this.user_id = checkNotNull(authInstance.currentUser).uid
            this.type = ObjectTypeInfoHelper.MESSAGE_TYPE_STANDARD
        }
        Log.d(
            TAG,
            "Sending message with length ${message.text.length}"
        )
        sendMessageOnCloud(message)
    }

    //in order to edit a message we will use the same id but with updated data
    suspend fun editMessage(text: String, fullMessage: FullMessage) {
        val message = Message().apply {
            this.id = fullMessage.id
            this.text = text
            this.user_id = fullMessage.user_id
            this.type = fullMessage.type
        }
        Log.d(
            TAG,
            "Editing message with id ${message.id}"
        )
        sendMessageOnCloud(message)
    }

    //this function execute an upsert on the input_messages ObjectType
    private suspend fun sendMessageOnCloud(message: Message) {
        dbZone.executeUpsert(message).await()
    }

    /*
    this function execute a query to retrieve all the messages and then subscribe a listener
    in order to get all the data changes about the same Object Type. In this way everytime we
    have a new message and/or a deleted messages and/or an updated one, we get automatically
    new data shown in the app
     */
    private suspend fun listenForAllMessages() {

        fun processQueryResult(snapshot: CloudDBZoneSnapshot<FullMessage>) {
            val messagesList = snapshot.toList()
            _allMessages.update { messagesList.sortedBy { it.date_ins } }
            Log.d(TAG, "Retrieving ${messagesList.size} messages")
        }

        val query = CloudDBZoneQuery.where(FullMessage::class.java)
            .equalTo("type", ObjectTypeInfoHelper.MESSAGE_TYPE_STANDARD)
            .or()
            .equalTo("type", ObjectTypeInfoHelper.MESSAGE_TYPE_POLL)
        //not supported by the subscription
        //.orderByDesc("date_ins")

        allMessagesListener = dbZone.subscribeSnapshot(
            query,
            CloudDBZoneQuery.CloudDBZoneQueryPolicy.POLICY_QUERY_FROM_CLOUD_ONLY
        ) { cloudDBZoneSnapshot, err ->
            err?.let {
                Log.w(TAG, "Snapshot failed with: $err")
            } ?: processQueryResult(cloudDBZoneSnapshot)
        }

        processQueryResult(
            dbZone.executeQuery(
                query,
                CloudDBZoneQuery.CloudDBZoneQueryPolicy.POLICY_QUERY_DEFAULT
            ).await()
        )
    }

    suspend fun deleteMessage(message: FullMessage) {
        Log.d(TAG, "Deleting message ${message.id}")
        dbZone.executeDelete(message).await()
    }

    //not used since when the user logout is triggered, the cloud function deletes the data in DB
    suspend fun deleteUser(id: String) {
        val userToDelete = User().apply {
            this.id = id
        }
        Log.d(TAG, "Deleting user ${userToDelete.id} ")
        dbZone.executeDelete(userToDelete).await()
    }

    //this function will check if there's the user data into the related Object Type
    suspend fun getUserDataAvailability(): Boolean {
        val query = CloudDBZoneQuery.where(User::class.java)
            .equalTo("id", checkNotNull(authInstance.currentUser).uid)

        val snapshot = dbZone.executeQuery(
            query,
            CloudDBZoneQuery.CloudDBZoneQueryPolicy.POLICY_QUERY_FROM_CLOUD_ONLY
        ).await()

        return (snapshot.snapshotObjects?.size() ?: 0) > 0
    }

    //query to retrieve all the available restaurants in the related Object Type
    suspend fun getPollLunchChoices(): List<PollLunchChoice> {
        val query = CloudDBZoneQuery.where(PollLunchChoice::class.java)

        val snapshot = dbZone.executeQuery(
            query,
            CloudDBZoneQuery.CloudDBZoneQueryPolicy.POLICY_QUERY_DEFAULT
        ).await()

        val listOfChoices = snapshot.toList()
        Log.d(TAG, "Getting poll choices, count: ${listOfChoices.size} ")
        return listOfChoices
    }

    //store the restaurant choice by the user into the related Object Type
    suspend fun sendPollLunchChoice(pollLunchChoice: PollLunchChoice) {
        Log.d(TAG, "Sending poll choice: ${pollLunchChoice.name} ")
        dbZone.executeUpsert(PollLunch().apply {
            user_id = checkNotNull(authInstance.currentUser).uid
            choice = pollLunchChoice.name
        }).await()
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

    sealed class CloudState {
        object Disconnected : CloudState()
        data class ConnectionFailed(val throwable: Throwable) : CloudState()
        object Connecting : CloudState()
        object Connected : CloudState()
    }
}

