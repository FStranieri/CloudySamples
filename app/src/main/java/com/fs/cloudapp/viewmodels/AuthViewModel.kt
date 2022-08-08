package com.fs.cloudapp.viewmodels

import android.app.Activity
import android.content.Intent
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.lifecycle.ViewModel
import com.fs.cloudapp.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.huawei.agconnect.auth.AGConnectAuth
import com.huawei.agconnect.auth.AGConnectAuthCredential


class AuthViewModel : ViewModel() {

    var loggedIn: MutableState<Boolean> = mutableStateOf(false)
        private set

    var failureOutput: MutableState<Exception?> = mutableStateOf(null)
        private set

    var authInstance: AGConnectAuth = AGConnectAuth.getInstance()
        private set

    init {
        this.loggedIn.value = authInstance.currentUser != null
    }

    fun login(activity: Activity, credentialType: Int) {
        this.authInstance.signIn(activity, credentialType).addOnSuccessListener {
            // updateUI
            loggedIn.value = true
        }.addOnFailureListener {
            // onFailure
            failureOutput.value = it
        }
    }

    fun loginWithCredentials(credential: AGConnectAuthCredential) {
        this.authInstance.signIn(credential).addOnSuccessListener {
            // updateUI
            loggedIn.value = true
        }.addOnFailureListener {
            // onFailure
            failureOutput.value = it
        }
    }

    fun loginWithGoogle(activity: Activity) {
        val client = GoogleSignIn.getClient(activity,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(activity.getString(R.string.google_client_id))
                .requestProfile()
                .build())

        val signInIntent: Intent = client.signInIntent
        startActivityForResult(activity, signInIntent, GOOGLE_SIGN_IN, null)
    }

    fun logout() {
        this.authInstance.signOut()
        this.loggedIn.value = false
    }

    fun resetFailureOutput() {
        this.failureOutput.value = null
    }

    companion object {
        const val TAG = "AuthViewModel"
        const val GOOGLE_SIGN_IN = 154
    }
}