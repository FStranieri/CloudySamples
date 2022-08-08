package com.fs.cloudapp.composables

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Send
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.core.graphics.toColorInt
import com.fs.cloudapp.R
import com.fs.cloudapp.viewmodels.AuthViewModel
import com.fs.cloudapp.viewmodels.CloudDBViewModel
import com.fs.cloudapp.viewmodels.FullMessage
import com.skydoves.landscapist.glide.GlideImage

@Composable
fun BindChat(
    authViewModel: AuthViewModel,
    cloudDBViewModel: CloudDBViewModel
) {
    ConstraintLayout(Modifier.fillMaxSize()) {
        val (title,
            logoutButton,
            chatList,
            inputMessage,
            textToEdit) = createRefs()
        val messagesValue by cloudDBViewModel.getChatMessages().observeAsState()
        val lazyListState = rememberLazyListState()

        Text(
            modifier = Modifier
                .constrainAs(title) {
                    top.linkTo(parent.top)
                    linkTo(start = parent.start, end = parent.end)
                    width = Dimension.wrapContent
                    height = Dimension.wrapContent
                }
                .padding(8.dp),
            text = stringResource(R.string.chat),
            style = TextStyle(fontStyle = FontStyle.Italic),
            fontWeight = FontWeight.Bold
        )

        Button(
            modifier = Modifier.constrainAs(logoutButton) {
                top.linkTo(parent.top, 8.dp)
                end.linkTo(parent.end, 8.dp)
                width = Dimension.wrapContent
                height = Dimension.wrapContent
            }, // Occupy the max size in the Compose UI tree
            onClick = {
                authViewModel.logout()
            }) {
            Text(text = stringResource(R.string.logout_button_text), fontSize = 8.sp)
        }

        Box(
            modifier = Modifier
                .constrainAs(chatList) {
                    top.linkTo(title.bottom)
                    bottom.linkTo(inputMessage.top)
                    end.linkTo(parent.end)
                    start.linkTo(parent.start)
                    width = Dimension.fillToConstraints
                    height = Dimension.fillToConstraints
                }
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 70.dp),
                state = lazyListState
            ) {
                messagesValue?.let { list ->
                    items(list.toList(), key = {
                        it
                    }) { message ->
                        if (message.user_id == cloudDBViewModel.userID) {
                            BuildMyChatCard(message = message, cloudDBViewModel)
                        } else {
                            BuildUsersChatCard(message = message)
                        }
                    }
                }
            }
        }

        var messageText by remember { mutableStateOf("") }
        val messageToEdit by remember { cloudDBViewModel.messageToEdit }

        OutlinedTextField(
            modifier = Modifier
                .constrainAs(inputMessage) {
                    bottom.linkTo(parent.bottom, 8.dp)
                    start.linkTo(parent.start, 8.dp)
                    end.linkTo(parent.end, 8.dp)
                    width = Dimension.fillToConstraints
                    height = Dimension.preferredWrapContent
                }
                .then(Modifier.background(Color.White)),
            value = messageText,
            onValueChange = { messageText = it },
            label = { Text(stringResource(R.string.enter_text_label)) },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color.Blue
            ),
            trailingIcon = {
                if (messageText.isNotEmpty()) {
                    IconButton(onClick = {
                        if (messageToEdit != null) {
                            cloudDBViewModel.editMessage(messageText, messageToEdit!!)
                        } else {
                            cloudDBViewModel.sendMessage(messageText)
                        }
                        messageText = ""
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.Send,
                            contentDescription = null
                        )
                    }
                }
            }
        )

        AnimatedVisibility(
            visible = messageToEdit != null,
            modifier = Modifier
                .fillMaxWidth()
                .constrainAs(textToEdit) {
                    linkTo(start = parent.start, end = parent.end)
                    bottom.linkTo(inputMessage.top)
                    width = Dimension.wrapContent
                    height = Dimension.wrapContent
                },
            enter = fadeIn(
                // Overwrites the initial value of alpha to 0.4f for fade in, 0 by default
                initialAlpha = 1f
            ),
            exit = fadeOut(
                // Overwrites the default animation with tween
                animationSpec = tween(durationMillis = 250)
            )
        ) {
            Text(
                buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color.Gray)) {
                        append("Editing:")
                    }
                    append("\n${messageToEdit?.text ?: ""}")
                },
                modifier = Modifier
                    .background(Color.Yellow)
                    .padding(8.dp)
                    .clickable {
                        cloudDBViewModel.messageToEdit.value = null
                    },
                style = TextStyle(fontStyle = FontStyle.Italic),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BuildMyChatCard(message: FullMessage, cloudDBViewModel: CloudDBViewModel) {
    ConstraintLayout(Modifier.fillMaxWidth()) {
        val (card) = createRefs()
        var showOptions by remember { mutableStateOf(false) }

        Card(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .constrainAs(card) {
                    end.linkTo(parent.end)
                    width = Dimension.preferredWrapContent
                    height = Dimension.preferredWrapContent
                }
                .combinedClickable(
                    onClick = { },
                    onLongClick = { showOptions = true }
                ),
            backgroundColor = Color("#E8D100".toColorInt()),
            elevation = 4.dp,
            shape = RoundedCornerShape(8.dp),
        ) {
            ConstraintLayout(Modifier.wrapContentWidth()) {
                val (menu, text, date) = createRefs()

                DropdownMenu(
                    expanded = showOptions,
                    onDismissRequest = { showOptions = false },
                    modifier = Modifier
                        .constrainAs(menu) {}
                        .then(Modifier.background(color = Color("#E18701".toColorInt())))
                ) {
                    DropdownMenuItem(onClick = {
                        cloudDBViewModel.messageToEdit.value = message
                        showOptions = false
                    }) {
                        Text(stringResource(R.string.edit_message_option_text))
                    }
                    DropdownMenuItem(onClick = {
                        cloudDBViewModel.deleteMessage(message)
                        showOptions = false
                    }) {
                        Text(stringResource(R.string.delete_message_option_text))
                    }
                }

                Text(
                    text = message.formattedDate,
                    color = Color.DarkGray,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .constrainAs(date) {
                            end.linkTo(parent.end)
                            bottom.linkTo(parent.bottom)
                        }
                        .then(Modifier.padding(8.dp))
                )

                Text(
                    text = message.text,
                    color = Color.Black,
                    fontSize = 22.sp,
                    modifier = Modifier
                        .constrainAs(text) {
                            top.linkTo(parent.top)
                            bottom.linkTo(date.top, 4.dp)
                        }
                        .padding(8.dp)
                )
            }
        }
    }
}

@Composable
fun BuildUsersChatCard(message: FullMessage) {
    ConstraintLayout(Modifier.fillMaxWidth()) {
        val (card) = createRefs()
        Card(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .constrainAs(card) {
                    start.linkTo(parent.start)
                    width = Dimension.preferredWrapContent
                    height = Dimension.preferredWrapContent
                },
            backgroundColor = Color("#009BAF".toColorInt()),
            elevation = 4.dp,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(2.dp, Color(message.color.toColorInt()))
        ) {
            ConstraintLayout(Modifier.wrapContentWidth()) {
                val (name, text, pic, date) = createRefs()
                Text(
                    text = message.nickname,
                    color = Color(message.color.toColorInt()),
                    fontSize = 22.sp,
                    modifier = Modifier
                        .constrainAs(name) {
                            start.linkTo(parent.start)
                            top.linkTo(parent.top)
                        }
                        .then(Modifier.padding(8.dp))
                )

                GlideImage(
                    modifier = Modifier
                        .constrainAs(pic) {
                            start.linkTo(parent.start, 4.dp)
                            bottom.linkTo(parent.bottom, 4.dp)
                        }
                        .height(36.dp)
                        .width(36.dp)
                        .clip(CircleShape),
                    imageModel = message.picture_url,
                    // Crop, Fit, Inside, FillHeight, FillWidth, None
                    contentScale = ContentScale.Crop,
                    // shows a placeholder ImageBitmap when loading.
                    placeHolder = painterResource(R.drawable.ic_baseline_account_circle_24),
                    // shows an error ImageBitmap when the request failed.
                    error = painterResource(R.drawable.ic_baseline_account_circle_24)
                )

                Text(
                    text = message.formattedDate,
                    color = Color.DarkGray,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .constrainAs(date) {
                            end.linkTo(parent.end)
                            bottom.linkTo(parent.bottom)
                        }
                        .then(Modifier.padding(8.dp))
                )

                Text(
                    text = message.text,
                    color = Color.Black,
                    fontSize = 22.sp,
                    modifier = Modifier
                        .constrainAs(text) {
                            start.linkTo(pic.end, 4.dp)
                            top.linkTo(name.bottom)
                            bottom.linkTo(date.top, 4.dp)
                        }
                        .then(Modifier.padding(8.dp))
                )
            }
        }
    }
}