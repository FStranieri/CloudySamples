package com.fs.cloudapp.composables

import android.app.Activity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.facebook.login.widget.LoginButton
import com.fs.cloudapp.MainActivity
import com.fs.cloudapp.R
import com.fs.cloudapp.viewmodels.AuthViewModel
import com.google.android.gms.common.SignInButton
import com.huawei.agconnect.auth.AGConnectAuthCredential
import com.huawei.hms.support.hwid.ui.HuaweiIdAuthButton

@Composable
fun BindAccounts(
    authViewModel: AuthViewModel
) {
    ConstraintLayout(Modifier.fillMaxSize()) {
        val (title,
            huaweiIdButton,
            googleButton,
            facebookButton) = createRefs()

        Text(
            modifier = Modifier
                .constrainAs(title) {
                    top.linkTo(parent.top)
                    linkTo(start = parent.start, end = parent.end)
                    width = Dimension.wrapContent
                    height = Dimension.wrapContent
                }
                .padding(8.dp),
            text = stringResource(R.string.login_page_title),
            style = TextStyle(fontStyle = FontStyle.Italic),
            fontWeight = FontWeight.Bold
        )

        // HUAWEI ID LOGIN
        AndroidView(
            modifier = Modifier.constrainAs(huaweiIdButton) {
                top.linkTo(title.bottom, 16.dp)
                linkTo(start = parent.start, end = parent.end)
                width = Dimension.wrapContent
                height = Dimension.wrapContent
            }, // Occupy the max size in the Compose UI tree
            factory = { context ->
                // Creates HUAWEI ID button
                HuaweiIdAuthButton(context).apply {
                    // Sets up listeners for View -> Compose communication
                    setOnClickListener {
                        authViewModel.login(
                            context as Activity,
                            AGConnectAuthCredential.HMS_Provider
                        )
                    }
                }
            })

        // GOOGLE LOGIN
        AndroidView(
            modifier = Modifier.constrainAs(googleButton) {
                top.linkTo(huaweiIdButton.bottom, 16.dp)
                linkTo(start = parent.start, end = parent.end)
                width = Dimension.wrapContent
                height = Dimension.wrapContent
            }, // Occupy the max size in the Compose UI tree
            factory = { context ->
                // Creates GOOGLE Login button
                SignInButton(context).apply {
                    // Sets up listeners for View -> Compose communication
                    setOnClickListener {
                        authViewModel.login(
                            context as Activity,
                            AGConnectAuthCredential.Google_Provider
                        )
                    }
                }
            })

        // FACEBOOK LOGIN
        AndroidView(
            modifier = Modifier.constrainAs(facebookButton) {
                top.linkTo(googleButton.bottom, 16.dp)
                linkTo(start = parent.start, end = parent.end)
                width = Dimension.wrapContent
                height = Dimension.wrapContent
            }, // Occupy the max size in the Compose UI tree
            factory = { context ->
                // Creates Facebook button
                LoginButton(context).apply {
                    // Sets up listeners for View -> Compose communication
                    setOnClickListener {
                        authViewModel.login(
                            context as Activity,
                            AGConnectAuthCredential.Facebook_Provider
                        )
                    }
                }
            })
    }
}