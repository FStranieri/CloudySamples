package com.fs.cloudapp.composables

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
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
import com.fs.cloudapp.data.user_messages
import com.fs.cloudapp.viewmodels.CloudDBViewModel
import java.util.*

@Composable
fun BindChat(
    cloudDBViewModel: CloudDBViewModel
) {
    ConstraintLayout(Modifier.fillMaxSize()) {
        val (title,
            chatList,
            sendButton,
            inputMessage) = createRefs()
        val messagesValue by cloudDBViewModel.getChatMessages().observeAsState()
        val chatMessagesFailure by cloudDBViewModel.getFailureOutput().observeAsState()

        chatMessagesFailure?.let {
            Toast.makeText(LocalContext.current, it.message, Toast.LENGTH_LONG).show()
            cloudDBViewModel.resetFailureOutput()
        }

        val scrollState = rememberScrollState(0)
        val transScrollState = rememberScrollState(0)

        Text(
            modifier = Modifier
                .constrainAs(title) {
                    top.linkTo(parent.top)
                    linkTo(start = parent.start, end = parent.end)
                    width = Dimension.wrapContent
                    height = Dimension.wrapContent
                }
                .padding(16.dp),
            text = stringResource(R.string.chat),
            style = TextStyle(fontStyle = FontStyle.Italic),
            fontWeight = FontWeight.Bold
        )

        var messageText by remember { mutableStateOf("") }

        OutlinedTextField(
            modifier = Modifier.constrainAs(inputMessage) {
                bottom.linkTo(parent.bottom)
                start.linkTo(parent.start)
                end.linkTo(sendButton.start)
                width = Dimension.fillToConstraints
                height = Dimension.preferredWrapContent
            },
            value = messageText,
            onValueChange = { messageText = it },
            label = { Text("Label") }
        )

        Button(modifier = Modifier.constrainAs(sendButton) {
            bottom.linkTo(parent.bottom)
            end.linkTo(parent.end)
            width = Dimension.preferredWrapContent
            height = Dimension.preferredWrapContent
        }, onClick = {
            cloudDBViewModel.sendMessage(messageText)
        })
        {
            Text(
                text = stringResource(R.string.send_button)
            )
        }

        Box(
            modifier = Modifier
                .constrainAs(chatList) {
                    top.linkTo(title.bottom)
                    bottom.linkTo(sendButton.top)
                    end.linkTo(parent.end)
                    start.linkTo(parent.start)
                    width = Dimension.fillToConstraints
                    height = Dimension.fillToConstraints
                }
                .background(
                    colorResource(id = R.color.purple_500),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            LazyColumn(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                messagesValue?.let { list ->
                    items(list.toList(), key = {
                        it
                    }) { message ->
                        BuildChatCard(message = message)
                    }
                }
            }
        }

    }
}

@Composable
fun BuildChatCard(message: user_messages) {
    Card(
        backgroundColor = Color.White,
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