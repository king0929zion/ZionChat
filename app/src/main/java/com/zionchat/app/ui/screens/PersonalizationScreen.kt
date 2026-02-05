package com.zionchat.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.zionchat.app.LocalAppRepository
import com.zionchat.app.ui.components.PageTopBar
import com.zionchat.app.ui.icons.AppIcons
import com.zionchat.app.ui.theme.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

@Composable
fun PersonalizationScreen(navController: NavController) {
    val repository = LocalAppRepository.current

    val storedNickname by repository.personalNicknameFlow.collectAsState(initial = "")
    val storedInstructions by repository.customInstructionsFlow.collectAsState(initial = "")
    val memories by repository.memoriesFlow.collectAsState(initial = emptyList())

    var nickname by rememberSaveable { mutableStateOf("") }
    var instructions by rememberSaveable { mutableStateOf("") }
    var nicknameFocused by remember { mutableStateOf(false) }
    var instructionsFocused by remember { mutableStateOf(false) }

    val latestStoredNickname by rememberUpdatedState(storedNickname)
    val latestStoredInstructions by rememberUpdatedState(storedInstructions)

    LaunchedEffect(storedNickname, nicknameFocused) {
        if (!nicknameFocused && nickname != storedNickname) {
            nickname = storedNickname
        }
    }

    LaunchedEffect(storedInstructions, instructionsFocused) {
        if (!instructionsFocused && instructions != storedInstructions) {
            instructions = storedInstructions
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { nickname }
            .map { it.trimEnd() }
            .distinctUntilChanged()
            .filter { it != latestStoredNickname }
            .onEach { value -> repository.setPersonalNickname(value) }
            .collect()
    }

    LaunchedEffect(Unit) {
        snapshotFlow { instructions }
            .map { it.trimEnd() }
            .distinctUntilChanged()
            .filter { it != latestStoredInstructions }
            .onEach { value -> repository.setCustomInstructions(value) }
            .collect()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        PageTopBar(
            title = "Personalization",
            onBack = { navController.navigateUp() }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            SectionTitle(title = "Nickname")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Surface)
            ) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    BasicTextField(
                        value = nickname,
                        onValueChange = { nickname = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { nicknameFocused = it.isFocused },
                        textStyle = TextStyle(fontSize = 16.sp, color = TextPrimary),
                        singleLine = true,
                        cursorBrush = SolidColor(TextPrimary),
                        decorationBox = { innerTextField ->
                            Box(modifier = Modifier.fillMaxWidth()) {
                                if (nickname.isBlank()) {
                                    Text(
                                        text = "Enter your nickname",
                                        fontSize = 16.sp,
                                        color = Color(0xFFC7C7CC)
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            SectionTitle(title = "Custom Instructions")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 280.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Surface)
            ) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    BasicTextField(
                        value = instructions,
                        onValueChange = { instructions = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 280.dp)
                            .onFocusChanged { instructionsFocused = it.isFocused },
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            color = TextPrimary,
                            lineHeight = 22.sp
                        ),
                        cursorBrush = SolidColor(TextPrimary),
                        decorationBox = { innerTextField ->
                            Box(modifier = Modifier.fillMaxSize()) {
                                if (instructions.isBlank()) {
                                    Text(
                                        text = "What would you like ChatGPT to know about you to provide better responses?\n\nExample: I work as a software engineer, prefer concise answers, and am interested in AI technology.",
                                        fontSize = 16.sp,
                                        color = Color(0xFFC7C7CC),
                                        lineHeight = 22.sp
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            SectionTitle(title = "Memory")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { navController.navigate("memories") }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = AppIcons.Memory,
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Memories",
                        fontSize = 16.sp,
                        fontFamily = SourceSans3,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${memories.size} memories",
                        fontSize = 15.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    androidx.compose.material3.Icon(
                        imageVector = AppIcons.ChevronRight,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        fontFamily = SourceSans3,
        color = TextSecondary,
        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
    )
}
