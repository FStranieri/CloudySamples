package com.fs.cloudapp.viewmodels

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fs.cloudapp.MainActivity
import com.fs.cloudapp.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.huawei.agconnect.api.AGConnectApi
import com.huawei.agconnect.auth.AGConnectAuth
import com.huawei.agconnect.auth.AGConnectAuthCredential
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * This ViewModel includes all the logic about the Authentication Flow
 */
class AuthViewModel : ViewModel() {

    //Auth Service main object instance
    var authInstance: AGConnectAuth = AGConnectAuth.getInstance()
        private set

    //The MutableStateFlow that we need to manage the UI changes with Compose
    private val mState = MutableStateFlow(value = AuthState())
    val state: StateFlow<AuthState>
        get() = mState

    init {
        //if we are already logged in
        val previousInstanceAlive = this.authInstance.currentUser != null
        updateState(
            state.value.copy(
                loggedIn = previousInstanceAlive,
                previousInstanceAlive = previousInstanceAlive
            )
        )
    }

    //Update the MutableStateFlow data for UI changes based on events
    private fun updateState(newState: AuthState) {
        viewModelScope.launch {
            mState.emit(value = newState)
        }
    }

    //METHOD 1 for login just passing activity context and the login provider we want to use
    fun login(activity: Activity, credentialType: Int) {
        authInstance.signIn(activity, credentialType).addOnSuccessListener {
            // updateUI
            updateState(state.value.copy(loggedIn = true))
        }.addOnFailureListener {
            // onFailure
            updateState(state.value.copy(failureOutput = it))
        }
    }

    //METHOD 2 for login passing the already obtained login token
    fun loginWithCredentials(credential: AGConnectAuthCredential) {
        this.authInstance.signIn(credential).addOnSuccessListener {
            // updateUI
            updateState(state.value.copy(loggedIn = true))
        }.addOnFailureListener {
            // onFailure
            updateState(state.value.copy(failureOutput = it))
        }
    }

    //specific login for Google Login Provider following the METHOD 2, just as example
    fun loginWithGoogle(activity: Activity, googleLoginIntentLauncher: ActivityResultLauncher<Intent>) {
        val client = GoogleSignIn.getClient(
            activity,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(activity.getString(R.string.google_client_id))
                .requestProfile()
                .build()
        )

        googleLoginIntentLauncher.launch(client.signInIntent)
    }

    //In order to execute a logout
    fun logout() {
        this.authInstance.signOut()
        updateState(state.value.copy(loggedIn = false, previousInstanceAlive = false))
    }

    //In order to reset the failure data status after a successful retry
    fun resetFailureOutput() {
        updateState(state.value.copy(failureOutput = null))
    }

    companion object {
        const val TAG = "AuthViewModel"
        const val GOOGLE_SIGN_IN = 154
    }

    data class AuthState(
        val loggedIn: Boolean = false,
        val failureOutput: Exception? = null,
        val previousInstanceAlive: Boolean = false
    )
}