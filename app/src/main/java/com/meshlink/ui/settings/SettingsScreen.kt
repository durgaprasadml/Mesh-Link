package com.meshlink.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

// --- Theme Colors ---
private val DarkBackground = Color(0xFF121212)
private val SurfaceDark = Color(0xFF1E1E1E)
private val PrimaryNeonGreen = Color(0xFF00FF88)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFFAAAAAA)
private val ErrorRed = Color(0xFFFF5252)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    var userName by remember { mutableStateOf("User") }
    LaunchedEffect(uiState.user) { uiState.user?.name?.let { userName = it } }
    
    val meshModes = listOf("Auto", "Performance", "Battery Saver")

    LaunchedEffect(viewModel.uiEvent) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is SettingsEvent.LogoutSuccess -> onLoggedOut()
                else -> {}
            }
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                title = { Text("Settings", fontWeight = FontWeight.Bold, color = TextPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            
            // --- Profile Section ---
            item {
                SettingsSectionTitle("Profile")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(SurfaceDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(userName.take(1).uppercase(), color = PrimaryNeonGreen, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    OutlinedTextField(
                        value = userName,
                        onValueChange = { userName = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryNeonGreen,
                            unfocusedBorderColor = SurfaceDark,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        trailingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Name", tint = TextSecondary, modifier = Modifier.size(18.dp))
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // --- Privacy Section ---
            item {
                SettingsSectionTitle("Privacy")
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Encryption", color = TextPrimary, fontSize = 16.sp)
                            Switch(
                                checked = uiState.isEncryptionEnabled,
                                onCheckedChange = { viewModel.setEncryptionEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = PrimaryNeonGreen
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Online visibility", color = TextPrimary, fontSize = 16.sp)
                            Switch(
                                checked = uiState.isOnlineVisible,
                                onCheckedChange = { viewModel.setOnlineVisible(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = PrimaryNeonGreen
                                )
                            )
                        }
                    }
                }
            }

            // --- Mesh Settings Section ---
            item {
                SettingsSectionTitle("Mesh Settings")
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    meshModes.forEachIndexed { index, label ->
                        SegmentedButton(
                            selected = label == uiState.meshMode,
                            onClick = { viewModel.setMeshMode(label) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = meshModes.size),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = PrimaryNeonGreen,
                                activeContentColor = Color.Black,
                                inactiveContainerColor = DarkBackground,
                                inactiveContentColor = TextPrimary,
                                inactiveBorderColor = SurfaceDark
                            )
                        ) {
                            Text(label, fontSize = 12.sp, fontWeight = if (label == uiState.meshMode) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }

            // --- Storage & Account Section ---
            item {
                SettingsSectionTitle("Storage & Account")
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        SettingsClickableRow("Clear chats", textColor = ErrorRed)
                        HorizontalDivider(color = DarkBackground, thickness = 1.dp)
                        SettingsClickableRow("Clear cache")
                        HorizontalDivider(color = DarkBackground, thickness = 1.dp)
                        SettingsClickableRow("Logout", textColor = ErrorRed) {
                            viewModel.logout()
                        }
                    }
                }
            }

            // --- About Section ---
            item {
                SettingsSectionTitle("About")
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("App version", color = TextPrimary, fontSize = 16.sp)
                        Text("v1.5.0", color = TextSecondary, fontSize = 14.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        color = TextPrimary,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun SettingsClickableRow(title: String, textColor: Color = TextPrimary, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = textColor, fontSize = 16.sp)
        Icon(Icons.Default.ChevronRight, contentDescription = "Go to details", tint = TextSecondary)
    }
}
