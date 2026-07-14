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
import androidx.compose.material.icons.filled.Person
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

enum class RegisterStep {
    PROFILE, CREATE_PIN, CONFIRM_PIN
}

@Composable
fun RegistrationScreen(
    onNavigateToLogin: () -> Unit,
    onRegistrationSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var step by remember { mutableStateOf(RegisterStep.PROFILE) }
    var name by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }

    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel.uiEvent) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is AuthEvent.RegistrationSuccess -> onRegistrationSuccess()
                is AuthEvent.Error -> {
                    errorMessage = event.message
                    showErrorDialog = true
                    if (step == RegisterStep.CREATE_PIN || step == RegisterStep.CONFIRM_PIN) {
                        pinError = true
                        pin = ""
                        confirmPin = ""
                        step = RegisterStep.CREATE_PIN
                    }
                }
                else -> {}
            }
        }
    }

    AnimatedErrorDialog(
        visible = showErrorDialog,
        title = "Registration Failed",
        message = errorMessage,
        onDismiss = { 
            showErrorDialog = false 
            pinError = false
        }
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = MeshTheme.spacing.extraLarge)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(MeshTheme.spacing.giant))
            
            // Progress Indicator Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(RegisterStep.PROFILE, RegisterStep.CREATE_PIN, RegisterStep.CONFIRM_PIN).forEachIndexed { index, s ->
                    val isActive = step.ordinal >= index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActive) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                    if (index < 2) {
                        Spacer(modifier = Modifier.width(MeshTheme.spacing.small))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(MeshTheme.spacing.giant))
            
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "RegisterStep"
            ) { currentStep ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    when (currentStep) {
                        RegisterStep.PROFILE -> {
                            Text(
                                text = "Create Profile",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
                            Text(
                                text = "Join the secure mesh network",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Spacer(modifier = Modifier.height(MeshTheme.spacing.giant))

                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Display Name") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Person") },
                                singleLine = true,
                                shape = MeshTheme.shapes.large,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Spacer(modifier = Modifier.height(MeshTheme.spacing.large))
                            
                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = { phoneNumber = it },
                                label = { Text("Phone Number") },
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "Phone") },
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
                            
                            val isProfileValid = name.isNotBlank() && phoneNumber.isNotBlank()
                            Button(
                                onClick = { if (isProfileValid) step = RegisterStep.CREATE_PIN },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = MeshTheme.shapes.large,
                                enabled = isProfileValid
                            ) {
                                Text("Next", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            
                            Spacer(modifier = Modifier.height(MeshTheme.spacing.large))
                            TextButton(onClick = onNavigateToLogin) {
                                Text("Already have an account? ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Login", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        RegisterStep.CREATE_PIN -> {
                            Text(
                                text = "Create PIN",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
                            Text(
                                text = "Set a 4-digit PIN for security",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Spacer(modifier = Modifier.height(MeshTheme.spacing.extraLarge))
                            
                            PinInput(
                                pin = pin,
                                onPinChange = { pin = it },
                                pinLength = 4,
                                isError = pinError,
                                onMaxPinEntered = {
                                    step = RegisterStep.CONFIRM_PIN
                                }
                            )

                            Spacer(modifier = Modifier.height(MeshTheme.spacing.large))
                            TextButton(onClick = { step = RegisterStep.PROFILE }) {
                                Text("Back", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        
                        RegisterStep.CONFIRM_PIN -> {
                            Text(
                                text = "Confirm PIN",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
                            Text(
                                text = "Re-enter your PIN to verify",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Spacer(modifier = Modifier.height(MeshTheme.spacing.extraLarge))
                            
                            PinInput(
                                pin = confirmPin,
                                onPinChange = { confirmPin = it },
                                pinLength = 4,
                                isError = pinError,
                                onMaxPinEntered = { completedConfirm ->
                                    if (completedConfirm == pin) {
                                        viewModel.register(name, phoneNumber, pin)
                                    } else {
                                        errorMessage = "PINs do not match"
                                        showErrorDialog = true
                                        pinError = true
                                        confirmPin = ""
                                    }
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
                                    TextButton(onClick = { 
                                        confirmPin = ""
                                        pinError = false
                                        step = RegisterStep.CREATE_PIN 
                                    }) {
                                        Text("Back to Create PIN", color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
