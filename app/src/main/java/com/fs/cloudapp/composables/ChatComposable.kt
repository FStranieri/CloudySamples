package com.fs.cloudapp.composables

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Send
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.fs.cloudapp.R
import com.fs.cloudapp.data.messages
import com.fs.cloudapp.viewmodels.CloudDBViewModel

typealias Message = messages

@Composable
fun BindChat(
    cloudDBViewModel: CloudDBViewModel
) {
    ConstraintLayout(Modifier.fillMaxSize()) {
        val (title,
            chatList,
            inputMessage) = createRefs()
        val messagesValue by cloudDBViewModel.getChatMessages().observeAsState()
        val chatMessagesFailure by cloudDBViewModel.getFailureOutput().observeAsState()

        chatMessagesFailure?.let {
            Toast.makeText(LocalContext.current, it.message, Toast.LENGTH_LONG).show()
            cloudDBViewModel.resetFailureOutput()
        }

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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                messagesValue?.let { list ->
                    items(list.toList(), key = {
                        it
                    }) { message ->
                        if (message.user_id == cloudDBViewModel.userID) {
                            //BuildMyChatCard(message = message)
                            BuildUsersChatCard(message = message)
                        } else {
                            BuildUsersChatCard(message = message)
                        }
                    }
                }
            }
        }

        var messageText by remember { mutableStateOf("") }

        OutlinedTextField(
            modifier = Modifier
                .constrainAs(inputMessage) {
                    bottom.linkTo(parent.bottom, 8.dp)
                    start.linkTo(parent.start, 8.dp)
                    end.linkTo(parent.end, 8.dp)
                    width = Dimension.fillToConstraints
                    height = Dimension.preferredWrapContent
                }.then(Modifier.background(Color.White)),
            value = messageText,
            onValueChange = { messageText = it },
            label = { Text(stringResource(R.string.enter_text_label))},
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color.Blue
            ),
            trailingIcon = {
                if (messageText.isNotEmpty()) {
                    IconButton(onClick = {
                        cloudDBViewModel.sendMessage(messageText)
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
    }
}

@Composable
fun BuildMyChatCard(message: Message) {
    ConstraintLayout(Modifier.fillMaxWidth()) {
        val (card) = createRefs()
        Card(
            modifier = Modifier.constrainAs(card) {
                end.linkTo(parent.end)
                width = Dimension.fillToConstraints
                height = Dimension.preferredWrapContent
            },
            backgroundColor = Color.Yellow,
            elevation = 4.dp,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = message.text,
                color = Color.Black,
                fontSize = 22.sp,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
fun BuildUsersChatCard(message: Message) {
    ConstraintLayout(Modifier.fillMaxWidth()) {
        val (card) = createRefs()
        Card(
            modifier = Modifier.constrainAs(card) {
                start.linkTo(parent.start)
                width = Dimension.fillToConstraints
                height = Dimension.preferredWrapContent
            },
            backgroundColor = Color.Cyan,
            elevation = 4.dp,
            shape = RoundedCornerShape(16.dp)
        ) {
            ConstraintLayout(Modifier.wrapContentWidth()) {
                val (name, text) = createRefs()
                Text(
                    text = message.user_id,
                    color = Color.Blue,
                    fontSize = 22.sp,
                    modifier = Modifier
                        .constrainAs(name) {
                            start.linkTo(parent.start)
                            top.linkTo(parent.top)
                        }
                        .then(Modifier.padding(8.dp))
                )
                Text(
                    text = message.text,
                    color = Color.Black,
                    fontSize = 22.sp,
                    modifier = Modifier
                        .constrainAs(text) {
                            start.linkTo(parent.start)
                            top.linkTo(name.bottom)
                        }
                        .then(Modifier.padding(8.dp))
                )
            }
        }
    }
}