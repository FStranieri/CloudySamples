package com.fs.cloudapp.viewmodels

import android.app.Activity
import android.content.Intent
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.fs.cloudapp.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.huawei.agconnect.auth.AGConnectAuth
import com.huawei.agconnect.auth.AGConnectAuthCredential
import com.huawei.agconnect.auth.SignInResult
import com.huawei.hms.support.api.entity.common.CommonConstant.ReqAccessTokenParam.CLIENT_ID


class AuthViewModel : ViewModel() {

    var loggedIn: MutableState<Boolean> = mutableStateOf(false)
        private set

    private var output: MutableLiveData<SignInResult> = MutableLiveData()
    private var failureOutput: MutableLiveData<Exception> = MutableLiveData()

    var authInstance: AGConnectAuth = AGConnectAuth.getInstance()
        private set

    init {
        authInstance.signOut()
    }

    fun login(activity: Activity, credentialType: Int) {
        this.authInstance.signIn(activity, credentialType).addOnSuccessListener {
            // updateUI
            output.value = it
            loggedIn.value = true
        }.addOnFailureListener {
            // onFailure
            failureOutput.value = it
        }
    }

    fun loginWithCredentials(credential: AGConnectAuthCredential) {
        this.authInstance.signIn(credential).addOnSuccessListener {
            // updateUI
            output.value = it
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

    fun getOutput(): LiveData<SignInResult> {
        return output
    }

    fun getFailureOutput(): LiveData<Exception> {
        return failureOutput
    }

    fun resetFailureOutput() {
        this.failureOutput.value = null
    }

    companion object {
        const val TAG = "AuthViewModel"
        const val GOOGLE_SIGN_IN = 154
    }
}