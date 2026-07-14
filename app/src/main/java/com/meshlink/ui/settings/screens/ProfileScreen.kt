package com.meshlink.ui.settings.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.meshlink.ui.components.settings.SettingsItemRow
import com.meshlink.ui.designsystem.theme.MeshTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userName: String,
    meshId: String,
    onBack: () -> Unit
) {
    var editName by remember { mutableStateOf(userName) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Profile") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = MeshTheme.spacing.mediumLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(MeshTheme.spacing.large))
            
            // Large Avatar
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userName.take(1).uppercase(), 
                    color = MaterialTheme.colorScheme.onPrimaryContainer, 
                    style = MaterialTheme.typography.displayMedium, 
                    fontWeight = FontWeight.Bold
                )
                
                // Verified Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 8.dp, end = 8.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle, 
                        contentDescription = "Verified",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(MeshTheme.spacing.giant))
            
            // Edit Name Field
            OutlinedTextField(
                value = editName,
                onValueChange = { editName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Display Name") },
                trailingIcon = { Icon(Icons.Default.Edit, contentDescription = "Edit") },
                shape = MeshTheme.shapes.medium
            )
            
            Spacer(modifier = Modifier.height(MeshTheme.spacing.mediumLarge))
            
            // Mesh ID Read-only
            OutlinedTextField(
                value = meshId.ifBlank { "Unassigned" },
                onValueChange = { },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Mesh ID") },
                trailingIcon = { Icon(Icons.Default.QrCode, contentDescription = "QR") },
                readOnly = true,
                shape = MeshTheme.shapes.medium
            )
            
            Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
            Text(
                text = "Your Mesh ID is your unique identifier on the decentralized network. It cannot be changed.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = MeshTheme.spacing.medium)
            )

            Spacer(modifier = Modifier.height(MeshTheme.spacing.giant))
            
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = MeshTheme.shapes.large
            ) {
                Column {
                    SettingsItemRow(
                        title = "Trust Level",
                        subtitle = "High (Self-Signed Identity)",
                        trailingContent = {
                            Text("Verified", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    )
                }
            }
        }
    }
}
