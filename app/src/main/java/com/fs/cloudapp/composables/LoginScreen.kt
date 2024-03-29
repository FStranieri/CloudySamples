package com.fs.cloudapp.composables

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.core.graphics.toColorInt
import com.fs.cloudapp.R
import com.fs.cloudapp.viewmodels.AuthViewModel
import com.huawei.agconnect.auth.AGConnectAuthCredential

/**
 * Login screen, using Auth Service it's possible to login with 3rd party providers,
 * your own server or anonymously.
 * The credentials are already stored into Auth Service but we are saving them on Cloud DB too
 * in order to manipulate the info for the chat.
 */

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    googleLoginIntentLauncher: ActivityResultLauncher<Intent>
) {
    val currentActivity = LocalContext.current as Activity

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.login_screen_bg),
            contentDescription = "login bg",
            Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                Modifier.wrapContentSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.login_screen_phone),
                    contentDescription = "login pic",
                    Modifier
                        .fillMaxWidth(0.8f)
                        .fillMaxHeight(0.3f)
                )

                Spacer(modifier = Modifier.height(30.dp))

                Text(
                    buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                color = Color("#f982f6".toColorInt()),
                                fontSize = 20.sp
                            )
                        ) {
                            append(stringResource(R.string.login_page_title1))
                        }

                        append(" ")

                        withStyle(
                            style = SpanStyle(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 20.sp
                            )
                        ) {
                            append(stringResource(R.string.login_page_title2))
                        }
                    },
                    modifier = Modifier
                        .wrapContentSize()
                        .padding(8.dp),
                    style = TextStyle(fontStyle = FontStyle.Italic),
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(24.dp))

                // HUAWEI ID LOGIN
                HuaweiLoginIdButton {
                    authViewModel.login(
                        activity = currentActivity,
                        credentialType = AGConnectAuthCredential.HMS_Provider
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                //GOOGLE LOGIN
                GoogleLoginButton {
                    //METHOD 1
                    /*authViewModel.login(
                    activity = currentActivity,
                    credentialType = AGConnectAuthCredential.Google_Provider
                )*/

                    //METHOD 2
                    authViewModel.loginWithGoogle(
                        activity = currentActivity,
                        googleLoginIntentLauncher
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

}

@Composable
private fun GoogleLoginButton(
    onClickLogin: () -> Unit
) {
    Button(
        modifier = Modifier
            .fillMaxWidth(0.7f),
        colors = ButtonDefaults.buttonColors(backgroundColor = Color("#FFFFFF".toColorInt())),
        onClick = onClickLogin
    ) {
        ConstraintLayout(Modifier.fillMaxWidth()) {
            val (icon, text) = createRefs()

            Icon(
                painter = painterResource(id = R.drawable.g_logo),
                modifier = Modifier
                    .constrainAs(icon) {
                        start.linkTo(parent.start)
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                    }
                    .size(24.dp),
                contentDescription = "g logo",
                tint = Color.Unspecified
            )
            Text(
                text = stringResource(R.string.g_login_text),
                color = Color("#000000".toColorInt()),
                textAlign = TextAlign.Center,
                modifier = Modifier.constrainAs(text) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                },
                fontFamily = FontFamily(
                    Font(
                        R.font.roboto_medium
                    )
                )
            )
        }
    }

    //in case you want the official button
    /*AndroidView(
        modifier = Modifier.fillMaxWidth(0.7f).height(50.dp).border(2.dp, color = Color.Black, shape = RoundedCornerShape(2.dp)),
        factory = { context ->
            // Creates Google login button
            SignInButton(context).apply {
                setSize(SignInButton.SIZE_STANDARD)
                // Sets up listeners for View -> Compose communication
                setOnClickListener { onClickLogin() }
            }
        })*/
}

@Composable
private fun FacebookLoginButton(
    onClickLogin: () -> Unit
) {
    Button(
        modifier = Modifier
            .fillMaxWidth(0.7f),
        colors = ButtonDefaults.buttonColors(backgroundColor = Color("#1877F2".toColorInt())),
        onClick = onClickLogin
    ) {
        ConstraintLayout(Modifier.fillMaxWidth()) {
            val (icon, text) = createRefs()

            Icon(
                painter = painterResource(id = R.drawable.f_logo_58),
                modifier = Modifier
                    .constrainAs(icon) {
                        start.linkTo(parent.start)
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                    }
                    .size(24.dp),
                contentDescription = "fb logo",
                tint = Color.Unspecified
            )
            Text(
                text = stringResource(R.string.fb_login_text),
                color = Color("#FFFFFF".toColorInt()),
                textAlign = TextAlign.Start,
                modifier = Modifier.constrainAs(text) {
                    start.linkTo(icon.end, 8.dp)
                    end.linkTo(parent.end)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                },
                fontFamily = FontFamily(
                    Font(
                        R.font.roboto_medium
                    )
                )
            )
        }
    }

    //in case you want the official button
    /*AndroidView(
        modifier = Modifier
            .fillMaxWidth(0.7f)
            .height(80.dp),
        factory = { context ->
            // Creates Facebook button
            LoginButton(context).apply {
                layoutParams = ViewGroup.LayoutParams(300, 50)
                // Sets up listeners for View -> Compose communication
                setOnClickListener { onClickLogin() }
            }
        })*/
}

@Composable
private fun HuaweiLoginIdButton(
    onClickLogin: () -> Unit
) {
    Button(
        modifier = Modifier
            .fillMaxWidth(0.7f),
        colors = ButtonDefaults.buttonColors(backgroundColor = Color("#CE0E2D".toColorInt())),
        onClick = onClickLogin
    ) {
        ConstraintLayout(Modifier.fillMaxWidth()) {
            val (icon, text) = createRefs()

            Icon(
                painter = painterResource(id = R.drawable.hw_40x40_logo_white),
                modifier = Modifier
                    .constrainAs(icon) {
                        start.linkTo(parent.start)
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                    }
                    .size(24.dp),
                contentDescription = "huawei logo",
                tint = Color.Unspecified
            )
            Text(
                text = stringResource(R.string.huawei_login_text),
                color = Color("#FFFFFF".toColorInt()),
                textAlign = TextAlign.Start,
                modifier = Modifier.constrainAs(text) {
                    start.linkTo(icon.end, 8.dp)
                    end.linkTo(parent.end)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                },
                fontFamily = FontFamily(
                    Font(
                        R.font.roboto_medium
                    )
                )
            )
        }
    }

    //in case you want the official button
    /*AndroidView(
        modifier = Modifier
            .fillMaxWidth(0.7f)
            .height(50.dp)
            .border(2.dp, color = Color.Black, shape = RoundedCornerShape(2.dp)),
        factory = { context ->
            // Creates HUAWEI ID button
            HuaweiIdAuthButton(context).apply {
                // Sets up listeners for View -> Compose communication
                setOnClickListener { onClickLogin() }
            }
        })*/
}