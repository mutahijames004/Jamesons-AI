package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.configureAppCheck
import com.example.ui.screens.AiTutorScreen
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.RevisionScreen
import com.example.ui.screens.StudyRoomDialog
import com.example.ui.screens.VaultNotesScreen
import com.example.ui.theme.AmoledSurface
import com.example.ui.theme.AmoledSurfaceBorder
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.SynapseTheme
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.SynapseViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: SynapseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Mandatory App Check initialization for Firebase AI Logic
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            SynapseTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SynapseApp(viewModel = viewModel)
                }
            }
        }
    }
}

enum class SynapseTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val testTag: String) {
    DASHBOARD("Home", Icons.Default.Dashboard, "tab_dashboard"),
    REVISION("Revision", Icons.Default.Repeat, "tab_revision"),
    AI_TUTOR("AI Tutor", Icons.Default.AutoAwesome, "tab_ai_tutor"),
    ANALYTICS("Analytics", Icons.Default.Insights, "tab_analytics"),
    VAULT("Vault", Icons.Default.FolderSpecial, "tab_vault")
}

@Composable
fun SynapseApp(viewModel: SynapseViewModel) {
    var selectedTab by remember { mutableStateOf(SynapseTab.DASHBOARD) }
    var selectedDeckForRevision by remember { mutableStateOf<Long?>(null) }
    var showStudyRoomDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val userMessage by viewModel.userMessage.collectAsState()
    val activeDeckId by viewModel.activeDeckId.collectAsState()

    // Handle Back Press navigation
    BackHandler(enabled = selectedTab != SynapseTab.DASHBOARD || activeDeckId != null) {
        if (activeDeckId != null) {
            viewModel.startDeckReview(null)
        } else {
            selectedTab = SynapseTab.DASHBOARD
        }
    }

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = AmoledSurface,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("synapse_bottom_navigation")
            ) {
                SynapseTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            selectedTab = tab
                            if (tab != SynapseTab.REVISION) {
                                viewModel.startDeckReview(null)
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                tint = if (isSelected) ElectricCyan else TextMuted
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 11.sp,
                                color = if (isSelected) ElectricCyan else TextMuted
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = ElectricCyan.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag(tab.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                SynapseTab.DASHBOARD -> DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToRevision = { deckId ->
                        selectedDeckForRevision = deckId
                        viewModel.startDeckReview(deckId)
                        selectedTab = SynapseTab.REVISION
                    },
                    onNavigateToAiTutor = { selectedTab = SynapseTab.AI_TUTOR },
                    onOpenStudyRoom = { showStudyRoomDialog = true }
                )

                SynapseTab.REVISION -> RevisionScreen(
                    viewModel = viewModel,
                    selectedDeckId = selectedDeckForRevision
                )

                SynapseTab.AI_TUTOR -> AiTutorScreen(viewModel = viewModel)

                SynapseTab.ANALYTICS -> AnalyticsScreen(viewModel = viewModel)

                SynapseTab.VAULT -> VaultNotesScreen(viewModel = viewModel)
            }
        }
    }

    if (showStudyRoomDialog) {
        StudyRoomDialog(
            viewModel = viewModel,
            onDismiss = { showStudyRoomDialog = false }
        )
    }
}
