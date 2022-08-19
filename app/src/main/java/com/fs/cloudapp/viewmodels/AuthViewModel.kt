package com.fs.cloudapp.viewmodels

import android.app.Activity
import android.content.Intent
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fs.cloudapp.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.huawei.agconnect.auth.AGConnectAuth
import com.huawei.agconnect.auth.AGConnectAuthCredential
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


class AuthViewModel : ViewModel() {

    private val mState = MutableStateFlow(value = AuthState())
    val state: StateFlow<AuthState>
        get() = mState


    var authInstance: AGConnectAuth = AGConnectAuth.getInstance()
        private set

    init {
        val previousInstanceAlive = this.authInstance.currentUser != null
        updateState(state.value.copy(loggedIn = previousInstanceAlive, previousInstanceAlive = previousInstanceAlive))
    }

    private fun updateState(newState: AuthState) {
        viewModelScope.launch {
            mState.emit(value = newState)
        }
    }

    fun login(activity: Activity, credentialType: Int) {
        authInstance.signIn(activity, credentialType).addOnSuccessListener {
            // updateUI
            updateState(state.value.copy(loggedIn = true))
        }.addOnFailureListener {
            // onFailure
            updateState(state.value.copy(failureOutput = it))
        }

    }

    fun loginWithCredentials(credential: AGConnectAuthCredential) {
        this.authInstance.signIn(credential).addOnSuccessListener {
            // updateUI
            updateState(state.value.copy(loggedIn = true))
        }.addOnFailureListener {
            // onFailure
            updateState(state.value.copy(failureOutput = it))
        }
    }

    fun loginWithGoogle(activity: Activity) {
        val client = GoogleSignIn.getClient(
            activity,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(activity.getString(R.string.google_client_id))
                .requestProfile()
                .build()
        )

        val signInIntent: Intent = client.signInIntent
        startActivityForResult(activity, signInIntent, GOOGLE_SIGN_IN, null)
    }

    fun logout() {
        this.authInstance.signOut()
        updateState(state.value.copy(loggedIn = false, previousInstanceAlive = false))
    }

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