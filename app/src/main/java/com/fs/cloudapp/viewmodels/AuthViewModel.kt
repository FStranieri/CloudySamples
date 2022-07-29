package com.fs.cloudapp.viewmodels

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.huawei.agconnect.auth.AGConnectAuth
import com.huawei.agconnect.auth.AGConnectAuthCredential
import com.huawei.agconnect.auth.SignInResult
import kotlinx.coroutines.withContext

class AuthViewModel : ViewModel() {

    var loggedIn: MutableState<Boolean> = mutableStateOf(false)
        private set

    private var output: MutableLiveData<SignInResult> = MutableLiveData()
    private var failureOutput: MutableLiveData<Exception> = MutableLiveData()

    var authInstance = AGConnectAuth.getInstance()
        private set

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
    }
}