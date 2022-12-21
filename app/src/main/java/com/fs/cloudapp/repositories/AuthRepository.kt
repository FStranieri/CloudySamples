package com.fs.cloudapp.repositories

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import com.fs.cloudapp.R
import com.fs.cloudapp.TAG
import com.fs.cloudapp.await
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.huawei.agconnect.auth.AGConnectAuth
import com.huawei.agconnect.auth.AGConnectAuthCredential
import com.huawei.agconnect.auth.AGConnectUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Contains the login/logout logic.
 */
class AuthRepository(
    private val authInstance: AGConnectAuth
) {

    private val _state: MutableStateFlow<AuthState> = MutableStateFlow(
        if (authInstance.currentUser == null) AuthState.LoggedOut else AuthState.LoggedIn(
            authInstance.currentUser
        )
    )

    fun observeLoginState(): StateFlow<AuthState> = _state.asStateFlow()

    val currentUser: AGConnectUser?
        get() = authInstance.currentUser

    /**
     * METHOD 1 for login just passing activity context and the login provider we want to use.
     */
    suspend fun login(activity: Activity, credentialType: Int): AGConnectUser {
        Log.d(TAG, "Login with credentialType: $credentialType")
        return authInstance
            .signIn(activity, credentialType)
            .await()
            .user
            .apply { _state.update { AuthState.LoggedIn(this) } }
    }

    /**
     * METHOD 2 for login passing the already obtained login token.
     */
    suspend fun loginWithCredentials(credential: AGConnectAuthCredential): AGConnectUser {
        Log.d(TAG, "Login with credential: $credential")
        return authInstance
            .signIn(credential)
            .await()
            .user
            .apply { _state.update { AuthState.LoggedIn(this) } }
    }

    /**
     * Specific login for Google Login Provider following the METHOD 2, just as example.
     */
    fun initiateDirectLoginWithGoogle(
        activity: Activity,
        googleLoginIntentLauncher: ActivityResultLauncher<Intent>
    ) {
        Log.d(TAG, "Initiate direct login with Google")
        val client = GoogleSignIn.getClient(
            activity,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(activity.getString(R.string.google_client_id))
                .requestProfile()
                .build()
        )
        googleLoginIntentLauncher.launch(client.signInIntent)
    }

    fun logout() {
        authInstance.signOut()
        _state.update { AuthState.LoggedOut }
    }

    companion object {
        const val GOOGLE_SIGN_IN = 154
    }

    sealed class AuthState {
        object LoggedOut : AuthState()
        data class LoggedIn(val user: AGConnectUser) : AuthState()
    }
}