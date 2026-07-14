package com.meshlink.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meshlink.ui.components.AnimatedErrorDialog
import com.meshlink.ui.components.PinInput
import com.meshlink.ui.designsystem.theme.MeshTheme

enum class LoginStep {
    PHONE, PIN
}

@Composable
fun LoginScreen(
    onNavigateToRegistration: () -> Unit,
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var step by remember { mutableStateOf(LoginStep.PHONE) }
    var phoneNumber by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel.uiEvent) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is AuthEvent.LoginSuccess -> onLoginSuccess()
                is AuthEvent.Error -> {
                    errorMessage = event.message
                    showErrorDialog = true
                    if (step == LoginStep.PIN) pin = "" // clear PIN on error
                }
                else -> {}
            }
        }
    }

    AnimatedErrorDialog(
        visible = showErrorDialog,
        title = "Authentication Failed",
        message = errorMessage,
        onDismiss = { showErrorDialog = false }
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(MeshTheme.spacing.extraLarge)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(MeshTheme.spacing.giant))
            
            // Header
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = "Login",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(MeshTheme.spacing.large))
            
            Text(
                text = "Welcome back",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Text(
                text = "Login to connect securely",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(MeshTheme.spacing.giant))

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "LoginStep"
            ) { currentStep ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    when (currentStep) {
                        LoginStep.PHONE -> {
                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = { phoneNumber = it },
                                label = { Text("Phone Number") },
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "Phone icon") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                shape = MeshTheme.shapes.large,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Spacer(modifier = Modifier.height(MeshTheme.spacing.giant))
                            
                            Button(
                                onClick = { if (phoneNumber.isNotBlank()) step = LoginStep.PIN },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = MeshTheme.shapes.large,
                                enabled = phoneNumber.isNotBlank()
                            ) {
                                Text("Continue", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                        LoginStep.PIN -> {
                            Text(
                                text = "Enter your PIN",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            PinInput(
                                pin = pin,
                                onPinChange = { pin = it },
                                pinLength = 4,
                                isError = showErrorDialog,
                                onMaxPinEntered = { completedPin ->
                                    viewModel.login(phoneNumber, completedPin)
                                }
                            )

                            Spacer(modifier = Modifier.height(MeshTheme.spacing.large))

                            Crossfade(targetState = uiState is AuthUiState.Loading, label = "LoadingBtn") { isLoading ->
                                if (isLoading) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().height(56.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            strokeWidth = 2.dp
                                        )
                                    }
                                } else {
                                    TextButton(onClick = { step = LoginStep.PHONE }) {
                                        Text("Change Phone Number", color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(MeshTheme.spacing.large))

            if (step == LoginStep.PHONE) {
                TextButton(onClick = onNavigateToRegistration) {
                    Text("Don't have an account? ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Register", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            } else {
                TextButton(onClick = {
                    errorMessage = "PIN recovery via SMS is not configured yet."
                    showErrorDialog = true
                }) {
                    Text("Forgot PIN?", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
