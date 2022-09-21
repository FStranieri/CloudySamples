package com.fs.cloudapp.composables

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.facebook.login.widget.LoginButton
import com.fs.cloudapp.R
import com.fs.cloudapp.viewmodels.AuthViewModel
import com.google.android.gms.common.SignInButton
import com.huawei.agconnect.auth.AGConnectAuthCredential
import com.huawei.hms.support.hwid.ui.HuaweiIdAuthButton

/**
 * Login screen, using Auth Service it's possible to login with 3rd party providers,
 * your own server or anonymously.
 * The credentials are already stored into Auth Service but we are saving them on Cloud DB too
 * in order to manipulate the info for the chat.
 */

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel
) {
    val currentActivity = LocalContext.current as Activity

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(Modifier.wrapContentSize(),
            horizontalAlignment = Alignment.CenterHorizontally) {

            Text(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(8.dp),
                text = stringResource(R.string.login_page_title),
                style = TextStyle(fontStyle = FontStyle.Italic),
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // HUAWEI ID LOGIN
            HuaweiLoginIdButton {
                authViewModel.login(
                    activity = currentActivity,
                    credentialType = AGConnectAuthCredential.HMS_Provider
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // GOOGLE LOGIN with custom management (method 2 according to the documentation)
            /*Button(
                modifier = Modifier.constrainAs(googleButton) {
                    top.linkTo(huaweiIdButton.bottom, 16.dp)
                    linkTo(start = parent.start, end = parent.end)
                    width = Dimension.wrapContent
                    height = Dimension.wrapContent
                }, // Occupy the max size in the Compose UI tree
                onClick = {

                    authViewModel.loginWithGoogle(activity)
                }) {
                Text(text = "GOOGLE LOGIN")
            }*/

            //GOOGLE LOGIN
            GoogleLoginButton {
                authViewModel.login(
                    activity = currentActivity,
                    credentialType = AGConnectAuthCredential.Google_Provider
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // FACEBOOK LOGIN
            FacebookLoginButton {
                authViewModel.login(
                    activity = currentActivity,
                    credentialType = AGConnectAuthCredential.Facebook_Provider
                )
            }
        }
    }

}

@Composable
private fun GoogleLoginButton(
    onClickLogin: () -> Unit
) {
    AndroidView(
        modifier = Modifier.wrapContentSize(),
        factory = { context ->
            // Creates Google login button
            SignInButton(context).apply {
                // Sets up listeners for View -> Compose communication
                setOnClickListener { onClickLogin() }
            }
        })
}

@Composable
private fun FacebookLoginButton(
    onClickLogin: () -> Unit
) {
    AndroidView(
        modifier = Modifier.wrapContentSize(),
        factory = { context ->
            // Creates Facebook button
            LoginButton(context).apply {
                // Sets up listeners for View -> Compose communication
                setOnClickListener { onClickLogin() }
            }
        })
}

@Composable
private fun HuaweiLoginIdButton(
    onClickLogin: () -> Unit
) {
    AndroidView(
        modifier = Modifier.wrapContentSize(),
        factory = { context ->
            // Creates HUAWEI ID button
            HuaweiIdAuthButton(context).apply {
                // Sets up listeners for View -> Compose communication
                setOnClickListener { onClickLogin() }
            }
        })
}