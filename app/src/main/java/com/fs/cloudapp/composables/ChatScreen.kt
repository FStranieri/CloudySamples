package com.fs.cloudapp.composables

import android.util.Log
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
import androidx.compose.material.icons.outlined.Fastfood
import androidx.compose.material.icons.outlined.Send
import androidx.compose.runtime.*
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fs.cloudapp.R
import com.fs.cloudapp.TAG
import com.fs.cloudapp.repositories.AuthRepository
import com.fs.cloudapp.repositories.CloudDBRepository
import com.fs.cloudapp.repositories.FullMessage
import com.fs.cloudapp.repositories.PollLunchChoice
import com.sebaslogen.resaca.viewModelScoped
import com.skydoves.landscapist.glide.GlideImage
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Chat screen using Cloud DB to store and read messages.
 */
@Composable
fun ChatScreen(
    cloudRepository: CloudDBRepository,
    authRepository: AuthRepository,
    viewModel: ChatViewModel = viewModelScoped {

        ChatViewModel(cloudRepository, authRepository)
    }
) {
    ConstraintLayout(Modifier.fillMaxSize()) {
        val (title, logoutButton, chatList, lunchChoicesBox, inputMessage, textToEdit) = createRefs()

//        val lunchChoices by viewModel.lunchChoices.collectAsState()
        val state by viewModel.state.collectAsState()
        val cloudState by viewModel.cloudState.collectAsState()

        val lazyListState = rememberLazyListState()

        Text(modifier = Modifier
            .constrainAs(title) {
                top.linkTo(parent.top)
                linkTo(start = parent.start, end = parent.end)
                width = Dimension.wrapContent
                height = Dimension.wrapContent
            }
            .padding(8.dp),
            text = stringResource(R.string.chat),
            style = TextStyle(fontStyle = FontStyle.Italic),
            fontWeight = FontWeight.Bold)

        Button(modifier = Modifier.constrainAs(logoutButton) {
            top.linkTo(parent.top, 8.dp)
            end.linkTo(parent.end, 8.dp)
            width = Dimension.wrapContent
            height = Dimension.wrapContent
        }, // Occupy the max size in the Compose UI tree
            onClick = {
                viewModel.logout()
            }) {
            Text(text = stringResource(R.string.logout_button_text), fontSize = 8.sp)
        }

        when (val cloudStateValue = cloudState) {
            CloudDBRepository.CloudState.Connected -> {
                val messagesValue by viewModel.messages.collectAsState()

                Box(modifier = Modifier
                    .constrainAs(chatList) {
                        top.linkTo(logoutButton.bottom, 16.dp)
                        bottom.linkTo(inputMessage.top)
                        end.linkTo(parent.end)
                        start.linkTo(parent.start)
                        width = Dimension.fillToConstraints
                        height = Dimension.fillToConstraints
                    }
                    .padding(16.dp)
                    .background(Color("#D5D5D5".toColorInt())),
                contentAlignment = Alignment.Center
            ) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                        items(messagesValue.toList(), key = {
                            it
                        }) { message ->
                            if (message.type == 0) {
                                if (message.user_id == viewModel.getUserId()) {
                                    BuildMyChatCard(message = message,
                                        onEdit = { viewModel.setEditMessageIntention(message) },
                                        onDelete = { viewModel.deleteMessage(message) })
                                } else {
                                    BuildUsersChatCard(message = message)
                                }
                            } else if (message.type == 1) {
                                BuildLunchPollChatCard(message = message)
                            }
                        }
                    }
                }

                if (state.pollChoices != null) {
                    Box(modifier = Modifier
                        .constrainAs(lunchChoicesBox) {
                            top.linkTo(logoutButton.bottom, 16.dp)
                            bottom.linkTo(inputMessage.top, 16.dp)
                            end.linkTo(parent.end, 16.dp)
                            start.linkTo(parent.start, 16.dp)
                            width = Dimension.fillToConstraints
                            height = Dimension.fillToConstraints
                        }
                        .padding(16.dp)
                        .background(Color.Blue), contentAlignment = Alignment.Center) {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            state.pollChoices?.let { list ->
                                items(list.toList(), key = {
                                    it
                                }) { lunchChoice ->
                                    BuildLunchPollChoice(
                                        choice = lunchChoice,
                                        onClick = {
                                            viewModel.updatePollChoices(false)
                                            viewModel.sendPollChoice(lunchChoice)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

            }
            CloudDBRepository.CloudState.Connecting -> {
                Snackbar {
                    Text(text = "Fetching conversations.")
                }
            }
            is CloudDBRepository.CloudState.ConnectionFailed -> {
                Snackbar {
                    Text(text = "Connection failed with reason: ${cloudStateValue.throwable}")
                }
            }
            CloudDBRepository.CloudState.Disconnected -> TODO()
        }

        var messageText by remember { mutableStateOf("") }
        val messageToEdit = state.messageToBeEdited

        OutlinedTextField(modifier = Modifier
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
                            viewModel.editMessage(messageText, messageToEdit)
                        } else {
                            viewModel.sendMessage(messageText)
                        }
                        messageText = ""
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.Send, contentDescription = null
                        )
                    }
                }
            },
            leadingIcon = {
                IconButton(onClick = {
                    viewModel.updatePollChoices(true)
                }) {
                    Icon(
                        imageVector = Icons.Outlined.Fastfood, contentDescription = null
                    )
                }
            })

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
                }, modifier = Modifier
                    .background(Color.Yellow)
                    .padding(8.dp)
                    .clickable {
                        viewModel.setEditMessageIntention(null)
                    }, style = TextStyle(fontStyle = FontStyle.Italic), fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BuildMyChatCard(
    message: FullMessage, onEdit: () -> Unit, onDelete: () -> Unit
) {
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
            backgroundColor = Color("#C8BFE7".toColorInt()),
            elevation = 4.dp,
            shape = RoundedCornerShape(8.dp),
        ) {
            ConstraintLayout(Modifier.wrapContentWidth()) {
                val (menu, text, date) = createRefs()

                DropdownMenu(expanded = showOptions,
                    onDismissRequest = { showOptions = false },
                    modifier = Modifier
                        .constrainAs(menu) {}
                        .then(Modifier.background(color = Color("#A596D8".toColorInt())))
                ) {
                    DropdownMenuItem(onClick = {
                        onEdit.invoke()
                        showOptions = false
                    }) {
                        Text(stringResource(R.string.edit_message_option_text))
                    }
                    DropdownMenuItem(onClick = {
                        onDelete.invoke()
                        showOptions = false
                    }) {
                        Text(stringResource(R.string.delete_message_option_text))
                    }
                }

                Text(text = message.formattedDate,
                    color = Color.DarkGray,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .constrainAs(date) {
                            end.linkTo(parent.end)
                            bottom.linkTo(parent.bottom)
                        }
                        .then(Modifier.padding(8.dp)))

                Text(text = message.text,
                    color = Color.Black,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .constrainAs(text) {
                            top.linkTo(parent.top)
                            bottom.linkTo(date.top, 4.dp)
                        }
                        .padding(8.dp))
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
            backgroundColor = Color("#A596D8".toColorInt()),
            elevation = 4.dp,
            shape = RoundedCornerShape(16.dp)
        ) {
            ConstraintLayout(
                Modifier
                    .wrapContentWidth()
                    .padding(8.dp)
            ) {
                val (name, text, pic, date) = createRefs()

                GlideImage(modifier = Modifier
                    .constrainAs(pic) {
                        start.linkTo(parent.start, 4.dp)
                        top.linkTo(parent.top, 4.dp)
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
                    error = painterResource(R.drawable.ic_baseline_account_circle_24))

                Text(text = message.nickname,
                    color = Color(message.color.toColorInt()),
                    fontSize = 16.sp,
                    modifier = Modifier
                        .constrainAs(name) {
                            start.linkTo(pic.end)
                            top.linkTo(pic.top)
                            bottom.linkTo(pic.bottom)
                        }
                        .then(Modifier.padding(4.dp)))

                Text(text = message.formattedDate,
                    color = Color.DarkGray,
                    fontSize = 8.sp,
                    modifier = Modifier
                        .constrainAs(date) {
                            end.linkTo(parent.end)
                            bottom.linkTo(parent.bottom)
                        }
                        .then(Modifier.padding(8.dp)))

                Text(text = message.text,
                    color = Color.Black,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .padding(8.dp)
                        .constrainAs(text) {
                            end.linkTo(date.end)
                            top.linkTo(pic.bottom)
                            bottom.linkTo(date.top, 4.dp)
                            width = Dimension.preferredWrapContent
                        })
            }
        }
    }
}

@Composable
fun BuildLunchPollChatCard(message: FullMessage) {
    ConstraintLayout(
        Modifier
            .wrapContentHeight()
            .fillMaxWidth()
    ) {
        val (card) = createRefs()
        Card(
            modifier = Modifier
                .wrapContentSize()
                .constrainAs(card) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                },
            backgroundColor = Color("#D5D5D5".toColorInt()),
            elevation = 4.dp,
            shape = RoundedCornerShape(16.dp)
        ) {
            ConstraintLayout(
                Modifier
                    .wrapContentWidth()
                    .padding(8.dp)
            ) {
                val (name, polls, pic, date) = createRefs()

                GlideImage(modifier = Modifier
                    .constrainAs(pic) {
                        start.linkTo(parent.start, 4.dp)
                        top.linkTo(parent.top, 4.dp)
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
                    error = painterResource(R.drawable.ic_baseline_account_circle_24))

                Text(text = message.nickname,
                    color = Color(message.color.toColorInt()),
                    fontSize = 16.sp,
                    modifier = Modifier
                        .constrainAs(name) {
                            start.linkTo(pic.end)
                            top.linkTo(pic.top)
                            bottom.linkTo(pic.bottom)
                        }
                        .then(Modifier.padding(4.dp)))
                Text(
                    text = message.formattedDate,
                    color = Color.Black,
                    fontSize = 8.sp,
                    modifier = Modifier
                        .constrainAs(date) {
                            end.linkTo(parent.end)
                            bottom.linkTo(parent.bottom)
                        }
                        .then(Modifier.padding(8.dp)))

                val json = JSONObject(message.text)
                val maxValue = json["max"] as Int
                val pollsList = json["polls"] as JSONObject

                LazyColumn(
                    modifier = Modifier
                        .height(120.dp)
                        .padding(8.dp)
                        .constrainAs(polls) {
                            end.linkTo(date.end)
                            top.linkTo(pic.bottom)
                            bottom.linkTo(date.top, 4.dp)
                            width = Dimension.preferredWrapContent
                        }, verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(pollsList.keys().toList(), key = {
                        it
                    }) { key ->
                        BuildLunchPoll(
                            name = key, value = pollsList[key] as Int, maxValue = maxValue
                        )
                    }
                }
            }
        }
    }
}

fun Iterator<String>.toList() = run {
    val list = mutableListOf<String>()
    while (this.hasNext()) {
        list.add(this.next())
    }
    list
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BuildLunchPollChoice(choice: PollLunchChoice, onClick: () -> Unit) {
    ConstraintLayout(Modifier.fillMaxWidth()) {
        val (card) = createRefs()

        Card(
            modifier = Modifier
                .wrapContentWidth()
                .constrainAs(card) {
                    end.linkTo(parent.end)
                    start.linkTo(parent.start)
                    width = Dimension.preferredWrapContent
                    height = Dimension.preferredWrapContent
                }
                .combinedClickable(
                    onClick = {
                        cloudDBViewModel.setLunchChoicesVisibility(false)
                        cloudDBViewModel.sendPollLunchChoice(choice)
                    },
                    onLongClick = {}
                ),
            backgroundColor = Color("#C8BFE7".toColorInt()),
            elevation = 4.dp,
            shape = RoundedCornerShape(8.dp),
        ) {
            ConstraintLayout(Modifier.wrapContentWidth()) {
                val (text) = createRefs()

                Text(text = choice.name,
                    color = Color.DarkGray,
                    fontSize = 30.sp,
                    modifier = Modifier
                        .constrainAs(text) {
                            end.linkTo(parent.end)
                            start.linkTo(parent.start)
                            bottom.linkTo(parent.bottom)
                            top.linkTo(parent.top)
                        }
                        .then(Modifier.padding(8.dp)))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BuildLunchPoll(name: String, value: Int, maxValue: Int) {
    ConstraintLayout(Modifier.wrapContentSize()) {
        val (choice, progress) = createRefs()
        Text(
            text = "$name ($value)",
            color = Color.Black,
            fontSize = 16.sp,
            modifier = Modifier.constrainAs(choice) {
                start.linkTo(parent.start)
                top.linkTo(parent.top)
            })

        LinearProgressIndicator(backgroundColor = Color.Blue,
            progress = value.toFloat() / maxValue.toFloat(),
            color = Color("#E8D100".toColorInt()),
            modifier = Modifier.constrainAs(progress) {
                start.linkTo(choice.start)
                top.linkTo(choice.bottom, 4.dp)
            })
    }
}

class ChatViewModel(
    private val cloudRepository: CloudDBRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    val cloudState = cloudRepository.observeConnectionState()

    val messages: StateFlow<List<FullMessage>> = cloudRepository.observeAllMessages()

    private val genericExceptionHandler =
        CoroutineExceptionHandler { _, e -> Log.e(this.TAG, e.toString()) }

    fun getUserId(): String {
        return checkNotNull(authRepository.currentUser).uid
    }

    fun setEditMessageIntention(message: FullMessage?) {
        _state.update { it.copy(messageToBeEdited = message) }
    }

    fun editMessage(messageText: String, message: FullMessage) {
        _state.update { it.copy(messageToBeEdited = null) }
        viewModelScope.launch(genericExceptionHandler) {
            cloudRepository.editMessage(messageText, message)
        }
    }

    fun deleteMessage(message: FullMessage) {
        viewModelScope.launch(genericExceptionHandler) {
            cloudRepository.deleteMessage(message)
        }
    }

    fun sendMessage(text: String) {
        viewModelScope.launch(genericExceptionHandler) {
            cloudRepository.sendMessage(text)
        }
    }

    fun updatePollChoices(hasToShowPollChoices: Boolean) {
        viewModelScope.launch(genericExceptionHandler) {
            val pollChoices = if (hasToShowPollChoices) {
                cloudRepository.getPollLunchChoices()
            } else {
                null
            }
            _state.update { it.copy(pollChoices = pollChoices) }
        }
    }

    fun sendPollChoice(pollLunchChoice: PollLunchChoice) {
        viewModelScope.launch(genericExceptionHandler) {
            cloudRepository.sendPollLunchChoice(pollLunchChoice)
        }
    }

    fun logout() {
        authRepository.logout()
    }

    data class ChatState(
        val messageToBeEdited: FullMessage? = null,
        val pollChoices: List<PollLunchChoice>? = null
    )
}