package com.meshlink.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meshlink.ui.designsystem.theme.MeshTheme
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    viewModel: ProfileSetupViewModel = hiltViewModel(),
    onSetupSuccess: () -> Unit
) {
    var displayName by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is ProfileSetupEvent.SetupSuccess -> {
                    onSetupSuccess()
                }
                is ProfileSetupEvent.Error -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Mesh Link") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(MeshTheme.spacing.large),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome to Mesh Link",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(MeshTheme.spacing.medium))
            
            Text(
                text = "Enter a display name to get started.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(MeshTheme.spacing.large))

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Display Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = displayName.isNotEmpty() && (displayName.length < 2 || displayName.length > 30),
                supportingText = {
                    if (displayName.isNotEmpty() && (displayName.length < 2 || displayName.length > 30)) {
                        Text("Name must be between 2 and 30 characters")
                    }
                }
            )

            Spacer(modifier = Modifier.height(MeshTheme.spacing.large))

            Button(
                onClick = { viewModel.createProfile(displayName) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MeshTheme.spacing.extraGiant),
                shape = MeshTheme.shapes.medium,
                enabled = uiState !is ProfileSetupUiState.Loading && displayName.length in 2..30
            ) {
                if (uiState is ProfileSetupUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = MeshTheme.spacing.small)
                            .size(MeshTheme.spacing.mediumLarge),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = MeshTheme.spacing.extraSmall
                    )
                }
                Text("Continue")
            }
        }
    }
}
