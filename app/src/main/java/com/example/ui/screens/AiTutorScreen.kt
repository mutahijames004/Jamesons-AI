package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.components.ResponsiveContainer
import com.example.ui.components.StatusBadge
import com.example.ui.components.SynapseCard
import com.example.ui.theme.AmoledSurface
import com.example.ui.theme.AmoledSurfaceBorder
import com.example.ui.theme.AmoledSurfaceVariant
import com.example.ui.theme.DangerCoral
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.SuccessMint
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.ChatMessage
import com.example.ui.viewmodel.SynapseViewModel

@Composable
fun AiTutorScreen(
    viewModel: SynapseViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val messages by viewModel.chatMessages.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val photoAnalysis by viewModel.photoAnalysisText.collectAsState()
    val isAnalyzingPhoto by viewModel.isAnalyzingPhoto.collectAsState()
    val decks by viewModel.decks.collectAsState()

    val availableSubjects by viewModel.availableSubjects.collectAsState()
    val selectedSubject by viewModel.selectedActiveSubject.collectAsState()

    val isVoiceModeActive by viewModel.isVoiceModeActive.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val audioRmsLevel by viewModel.audioRmsLevel.collectAsState()
    val liveTranscript by viewModel.liveTranscript.collectAsState()
    val speechStatus by viewModel.speechStatus.collectAsState()

    var userPrompt by remember { mutableStateOf("") }
    var showAddSubjectDialog by remember { mutableStateOf(false) }
    var showCameraPickerOptionDialog by remember { mutableStateOf(false) }
    var selectedImageBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Direct In-App Camera Launcher (takes photo with device camera)
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            selectedImageBitmap = bitmap
            viewModel.analyzePhoto(bitmap)
        }
    }

    // Camera Permission Launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(null)
        } else {
            viewModel.postMessage("Camera permission is required to take photos of study materials.")
        }
    }

    // Gallery Photo Picker Launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source)
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                selectedImageBitmap = bitmap
                viewModel.analyzePhoto(bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Direct In-App Audio Microphone Permission Launcher
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startListening()
        } else {
            viewModel.postMessage("Microphone permission required for in-app voice listening.")
        }
    }

    fun requestInAppVoiceListening() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            viewModel.startListening()
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun requestCameraCapture() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraLauncher.launch(null)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Mic pulsing animation when listening
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    ResponsiveContainer(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header Banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "JAMESON AI TUTOR",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    }
                    Text(
                        text = "Direct In-App Voice • Camera Scanner • Formatted Answers",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Voice Mode Toggle Button
                    IconButton(
                        onClick = {
                            viewModel.toggleVoiceMode()
                            if (!isVoiceModeActive) {
                                requestInAppVoiceListening()
                            }
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isVoiceModeActive) NeonViolet else AmoledSurfaceVariant)
                            .testTag("toggle_voice_mode_button")
                    ) {
                        Icon(
                            imageVector = if (isVoiceModeActive) Icons.Default.Headphones else Icons.Default.GraphicEq,
                            contentDescription = "Live Voice Mode",
                            tint = if (isVoiceModeActive) Color.White else ElectricCyan
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Camera Photo Capture Button
                    IconButton(
                        onClick = {
                            showCameraPickerOptionDialog = true
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(AmoledSurfaceVariant)
                            .testTag("scan_photo_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Camera Scanner",
                            tint = ElectricCyan
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Subject Filter Row with "+ Add Subject" button
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(availableSubjects) { subj ->
                    FilterChip(
                        selected = selectedSubject == subj,
                        onClick = { viewModel.selectSubject(subj) },
                        label = { Text(subj, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricCyan.copy(alpha = 0.2f),
                            selectedLabelColor = ElectricCyan,
                            containerColor = AmoledSurfaceVariant,
                            labelColor = TextSecondary
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (selectedSubject == subj) ElectricCyan else AmoledSurfaceBorder
                        ),
                        modifier = Modifier.testTag("subject_chip_$subj")
                    )
                }

                item {
                    Button(
                        onClick = { showAddSubjectDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = AmoledSurfaceVariant),
                        border = BorderStroke(1.dp, NeonViolet.copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("add_subject_chip_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = NeonViolet, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Subject", color = NeonViolet, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Direct In-App Live Voice Conversation Card (Zero Google Popups)
            AnimatedVisibility(visible = isVoiceModeActive) {
                SynapseCard(
                    borderColor = NeonViolet,
                    backgroundColor = AmoledSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusBadge(text = "IN-APP VOICE CONVERSATION", color = NeonViolet)
                            IconButton(
                                onClick = { viewModel.toggleVoiceMode() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close Voice", tint = TextMuted)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Real-time Audio Wave Visualizer
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.height(36.dp)
                        ) {
                            val waveBars = 9
                            for (i in 0 until waveBars) {
                                val barScale = if (isListening) {
                                    (audioRmsLevel * (0.4f + (i % 4) * 0.2f)).coerceIn(0.2f, 1f)
                                } else if (isSpeaking) {
                                    0.85f
                                } else {
                                    0.25f
                                }
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height((32 * barScale).dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(
                                            if (isListening) ElectricCyan else if (isSpeaking) SuccessMint else TextMuted
                                        )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Real-time In-App Live Transcript Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(AmoledSurface)
                                .border(BorderStroke(1.dp, if (isListening) ElectricCyan.copy(alpha = 0.6f) else AmoledSurfaceBorder), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = if (isListening) "🎙️ Listening by itself inside the app..." else "Status: $speechStatus",
                                    color = if (isListening) ElectricCyan else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (liveTranscript.isNotBlank()) "\"$liveTranscript\"" else "Speak your question clearly into your microphone...",
                                    color = if (liveTranscript.isNotBlank()) TextPrimary else TextMuted,
                                    fontSize = 13.sp,
                                    fontStyle = if (liveTranscript.isBlank()) FontStyle.Italic else FontStyle.Normal
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSpeaking) {
                                Button(
                                    onClick = { viewModel.stopSpeaking() },
                                    colors = ButtonDefaults.buttonColors(containerColor = DangerCoral),
                                    modifier = Modifier.testTag("stop_speaking_button")
                                ) {
                                    Icon(Icons.Default.Stop, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Stop Speaking", color = Color.White)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        if (isListening) {
                                            viewModel.stopListening()
                                        } else {
                                            requestInAppVoiceListening()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isListening) DangerCoral else ElectricCyan
                                    ),
                                    modifier = Modifier
                                        .testTag("voice_speak_button")
                                        .then(if (isListening) Modifier.scale(pulseScale) else Modifier)
                                ) {
                                    Icon(
                                        imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                                        contentDescription = null,
                                        tint = if (isListening) Color.White else AmoledSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isListening) "Finish & Ask" else "Speak Now",
                                        color = if (isListening) Color.White else AmoledSurface,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Quick In-Card Verbal Prompt Chips
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val voicePrompts = listOf(
                                "Explain in simple terms",
                                "Give the core formula",
                                "What are common exam traps?",
                                "Summarize key concept"
                            )
                            items(voicePrompts) { prompt ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AmoledSurface)
                                        .border(BorderStroke(1.dp, AmoledSurfaceBorder), RoundedCornerShape(8.dp))
                                        .clickable {
                                            viewModel.sendVoiceTutorMessage(prompt)
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(prompt, fontSize = 11.sp, color = ElectricCyan)
                                }
                            }
                        }
                    }
                }
            }

            // Multimodal Photo Analysis Card if camera photo taken
            AnimatedVisibility(visible = selectedImageBitmap != null || isAnalyzingPhoto || photoAnalysis != null) {
                SynapseCard(
                    borderColor = NeonViolet.copy(alpha = 0.5f),
                    backgroundColor = AmoledSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusBadge(text = "PHOTO NOTES ANALYZER", color = NeonViolet)
                            IconButton(
                                onClick = {
                                    selectedImageBitmap = null
                                    viewModel.clearPhotoAnalysis()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (selectedImageBitmap != null) {
                            Image(
                                bitmap = selectedImageBitmap!!.asImageBitmap(),
                                contentDescription = "Captured Photo",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        if (isAnalyzingPhoto) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = ElectricCyan,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Analyzing handwritten formulas and notes...", color = TextSecondary, fontSize = 12.sp)
                            }
                        } else if (photoAnalysis != null) {
                            FormattedExplanationContent(text = photoAnalysis!!)
                            Spacer(modifier = Modifier.height(10.dp))
                            val targetDeck = decks.firstOrNull()?.id ?: 1L
                            Button(
                                onClick = {
                                    viewModel.generateCardsFromNote(
                                        com.example.data.AcademicNoteEntity(
                                            title = "Camera Note",
                                            content = photoAnalysis!!,
                                            subject = selectedSubject,
                                            tags = "Camera, Notes"
                                        ),
                                        targetDeck
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                                modifier = Modifier.testTag("convert_photo_to_cards_button")
                            ) {
                                Icon(Icons.Default.AddCard, contentDescription = null, tint = AmoledSurface, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Convert to Flashcards", color = AmoledSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Quick Topic Suggestion Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                val suggestions = listOf(
                    "Explain core theorem",
                    "Step-by-step proof",
                    "Common exam traps",
                    "Give key formulas",
                    "Make 3 flashcards"
                )
                items(suggestions) { sugg ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(AmoledSurfaceVariant)
                            .border(BorderStroke(1.dp, AmoledSurfaceBorder), RoundedCornerShape(12.dp))
                            .clickable {
                                viewModel.sendTutorMessage(sugg, selectedSubject)
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(text = sugg, fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }

            // Chat Messages List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(messages) { msg ->
                    ChatBubble(
                        message = msg,
                        onSpeakAloud = {
                            viewModel.voiceManager.speak(msg.content)
                        }
                    )
                }

                if (isAiLoading && messages.lastOrNull()?.sender != "tutor_streaming") {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = ElectricCyan,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Jameson is formulating explanation...", color = TextMuted, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Chat Input Bar with Camera & Mic buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 90.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = userPrompt,
                    onValueChange = { userPrompt = it },
                    placeholder = { Text("Ask Jameson in $selectedSubject...", color = TextMuted, fontSize = 13.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_prompt_input"),
                    leadingIcon = {
                        IconButton(onClick = { showCameraPickerOptionDialog = true }) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Camera", tint = ElectricCyan, modifier = Modifier.size(20.dp))
                        }
                    },
                    trailingIcon = {
                        IconButton(onClick = {
                            if (!isVoiceModeActive) {
                                viewModel.toggleVoiceMode()
                            }
                            requestInAppVoiceListening()
                        }) {
                            Icon(Icons.Default.Mic, contentDescription = "Listen", tint = if (isListening) DangerCoral else ElectricCyan)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = AmoledSurfaceBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = AmoledSurfaceVariant,
                        unfocusedContainerColor = AmoledSurfaceVariant
                    ),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (userPrompt.isNotBlank()) {
                            val text = userPrompt
                            userPrompt = ""
                            viewModel.sendTutorMessage(text, selectedSubject)
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(ElectricCyan)
                        .testTag("send_ai_prompt_button")
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = AmoledSurface)
                }
            }
        }
    }

    // Camera vs Gallery Choice Dialog
    if (showCameraPickerOptionDialog) {
        AlertDialog(
            onDismissRequest = { showCameraPickerOptionDialog = false },
            containerColor = AmoledSurface,
            title = {
                Text("Scan Study Material", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Capture your handwritten formulas, whiteboard equations, or textbook notes:", color = TextSecondary, fontSize = 13.sp)

                    // Option 1: Take Photo with Camera
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AmoledSurfaceVariant)
                            .border(BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.4f)), RoundedCornerShape(12.dp))
                            .clickable {
                                showCameraPickerOptionDialog = false
                                requestCameraCapture()
                            }
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Camera, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Take Photo with Camera", color = TextPrimary, fontWeight = FontWeight.Bold)
                                Text("Open camera to photograph notes", color = TextMuted, fontSize = 11.sp)
                            }
                        }
                    }

                    // Option 2: Choose from Gallery
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AmoledSurfaceVariant)
                            .border(BorderStroke(1.dp, AmoledSurfaceBorder), RoundedCornerShape(12.dp))
                            .clickable {
                                showCameraPickerOptionDialog = false
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = NeonViolet, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Choose from Photos", color = TextPrimary, fontWeight = FontWeight.Bold)
                                Text("Select saved image or screenshot", color = TextMuted, fontSize = 11.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                OutlinedButton(onClick = { showCameraPickerOptionDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Add Custom Subject Dialog
    if (showAddSubjectDialog) {
        var newSubjectName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddSubjectDialog = false },
            containerColor = AmoledSurface,
            title = { Text("Add Subject to Learn", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter any subject or course syllabus (e.g. Neuroscience, Macroeconomics, Organic Chemistry):",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = newSubjectName,
                        onValueChange = { newSubjectName = it },
                        placeholder = { Text("Subject Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = AmoledSurfaceBorder
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("new_subject_name_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newSubjectName.isNotBlank()) {
                            viewModel.addCustomSubject(newSubjectName)
                            showAddSubjectDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                    modifier = Modifier.testTag("confirm_add_subject_button")
                ) {
                    Text("Add Subject", color = AmoledSurface, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddSubjectDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage,
    onSpeakAloud: () -> Unit = {}
) {
    val isUser = message.sender == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(if (isUser) 0.85f else 0.95f)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(if (isUser) ElectricCyan.copy(alpha = 0.2f) else AmoledSurfaceVariant)
                .border(
                    BorderStroke(
                        1.dp,
                        if (isUser) ElectricCyan.copy(alpha = 0.4f) else AmoledSurfaceBorder
                    ),
                    RoundedCornerShape(16.dp)
                )
                .padding(14.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUser) "You" else "Jameson AI Tutor",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isUser) ElectricCyan else NeonViolet
                    )
                    if (!isUser) {
                        IconButton(
                            onClick = onSpeakAloud,
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Read Aloud",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))

                if (isUser) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextPrimary,
                            lineHeight = 20.sp
                        )
                    )
                } else {
                    FormattedExplanationContent(text = message.content)
                }
            }
        }
    }
}

/**
 * Parses and renders Markdown-structured explanations with headers, bullet points,
 * formula blocks, and highlighted sections cleanly.
 */
@Composable
fun FormattedExplanationContent(text: String) {
    val lines = text.lines()

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                continue
            }

            when {
                // Section Header (### or ##)
                trimmed.startsWith("###") || trimmed.startsWith("##") -> {
                    val cleanHeader = trimmed.removePrefix("###").removePrefix("##").trim()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(3.dp, 14.dp)
                                .background(ElectricCyan, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = cleanHeader,
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyan,
                            fontSize = 15.sp
                        )
                    }
                }

                // Numbered Steps or Bold Points (e.g. **1. Definition:** or 1. Step)
                trimmed.startsWith("**") && trimmed.contains(":**") -> {
                    val label = trimmed.substringBefore(":**").removePrefix("**").trim()
                    val body = trimmed.substringAfter(":**").removeSuffix("**").trim()
                    Column(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text(
                            text = "$label:",
                            fontWeight = FontWeight.Bold,
                            color = NeonViolet,
                            fontSize = 13.sp
                        )
                        if (body.isNotBlank()) {
                            Text(
                                text = body,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }

                // Bullet points (• or - or *)
                trimmed.startsWith("•") || trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    val content = trimmed.removePrefix("•").removePrefix("- ").removePrefix("* ").trim()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 6.dp, top = 2.dp, bottom = 2.dp)
                    ) {
                        Text(
                            text = "•",
                            color = ElectricCyan,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = content.replace("**", ""),
                            color = TextPrimary,
                            fontSize = 13.sp,
                            lineHeight = 19.sp
                        )
                    }
                }

                // Formula / Callout block (*italic* or highlighted)
                trimmed.startsWith("*") && trimmed.endsWith("*") -> {
                    val callout = trimmed.removeSurrounding("*").trim()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AmoledSurface)
                            .border(BorderStroke(1.dp, NeonViolet.copy(alpha = 0.3f)), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = callout,
                            color = WarningAmber,
                            fontSize = 12.sp,
                            fontStyle = FontStyle.Italic,
                            lineHeight = 18.sp
                        )
                    }
                }

                // Standard paragraph text
                else -> {
                    Text(
                        text = trimmed.replace("**", ""),
                        color = TextPrimary,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}
