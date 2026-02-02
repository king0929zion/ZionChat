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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.zionchat.app.LocalAppRepository
import com.zionchat.app.ui.components.FloatingTopBar
import com.zionchat.app.ui.components.TopFadeScrim
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

    val storedNickname by repository.nicknameFlow.collectAsState(initial = "")
    val storedInstructions by repository.customInstructionsFlow.collectAsState(initial = "")

    var initialized by rememberSaveable { mutableStateOf(false) }
    var nickname by rememberSaveable { mutableStateOf("") }
    var instructions by rememberSaveable { mutableStateOf("") }

    val latestStoredNickname by rememberUpdatedState(storedNickname)
    val latestStoredInstructions by rememberUpdatedState(storedInstructions)

    LaunchedEffect(storedNickname, storedInstructions, initialized) {
        if (!initialized && (storedNickname.isNotBlank() || storedInstructions.isNotBlank())) {
            nickname = storedNickname
            instructions = storedInstructions
            initialized = true
        }
        if (!initialized && storedNickname.isBlank() && storedInstructions.isBlank()) {
            initialized = true
        }
    }

    LaunchedEffect(initialized) {
        if (!initialized) return@LaunchedEffect

        snapshotFlow { nickname }
            .map { it.trimEnd() }
            .distinctUntilChanged()
            .filter { it != latestStoredNickname }
            .onEach { value -> repository.setNickname(value) }
            .collect { }
    }

    LaunchedEffect(initialized) {
        if (!initialized) return@LaunchedEffect

        snapshotFlow { instructions }
            .map { it.trimEnd() }
            .distinctUntilChanged()
            .filter { it != latestStoredInstructions }
            .onEach { value -> repository.setCustomInstructions(value) }
            .collect { }
    }

    val contentTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 86.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .padding(top = contentTopPadding)
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
                        modifier = Modifier.fillMaxWidth(),
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
                            .heightIn(min = 280.dp),
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
                        text = "12 memories",
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

        TopFadeScrim(
            color = Background,
            height = 64.dp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-20).dp)
        )

        FloatingTopBar(
            title = "Personalization",
            onBack = { navController.navigateUp() },
            modifier = Modifier.align(Alignment.TopCenter)
        )
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
