package com.meshlink.ui.auth

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

// FIX ERROR 2: Match app's established dark + neon green palette exactly
private val BgColor       = Color(0xFF121212)
private val CardColor     = Color(0xFF1E1E1E)
private val NeonGreen     = Color(0xFF00FF88)
private val NeonGreenDim  = Color(0xFF00CC6A)
private val BorderGreen   = Color(0xFF00FF88)
private val TextPrimary   = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFFAAAAAA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onNavigateToRegistration: () -> Unit,
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val authState by viewModel.authState.collectAsState()
    var phoneNumber by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }

    // FIX: Animation — button scale on press
    var buttonPressed by remember { mutableStateOf(false) }
    val buttonScale by animateFloatAsState(
        targetValue = if (buttonPressed) 0.96f else 1f,
        animationSpec = tween(durationMillis = 80),
        label = "buttonScale"
    )

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            viewModel.resetState()
            onLoginSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            // Mesh node logo mark
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(NeonGreen.copy(alpha = 0.3f), Color.Transparent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(NeonGreen)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Mesh Link",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Text(
                text = "Secure offline messaging",
                fontSize = 13.sp,
                color = NeonGreen.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Welcome back",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Login to connect securely",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(28.dp))

                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Phone Number", color = TextSecondary) },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "Phone icon", tint = NeonGreen) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = NeonGreen,
                            focusedBorderColor = BorderGreen,
                            unfocusedBorderColor = Color(0xFF333333),
                            focusedLabelColor = NeonGreen,
                            unfocusedLabelColor = TextSecondary
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 4) pin = it },
                        label = { Text("4-Digit PIN", color = TextSecondary) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Lock icon", tint = NeonGreen) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = NeonGreen,
                            focusedBorderColor = BorderGreen,
                            unfocusedBorderColor = Color(0xFF333333),
                            focusedLabelColor = NeonGreen,
                            unfocusedLabelColor = TextSecondary
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(28.dp))

                    // Animated Login Button
                    Button(
                        onClick = {
                            buttonPressed = true
                            viewModel.login(phoneNumber, pin)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .scale(buttonScale),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonGreen,
                            disabledContainerColor = NeonGreen.copy(alpha = 0.4f)
                        ),
                        enabled = authState !is AuthState.Loading
                    ) {
                        LaunchedEffect(buttonPressed) {
                            if (buttonPressed) {
                                kotlinx.coroutines.delay(100)
                                buttonPressed = false
                            }
                        }
                        if (authState is AuthState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.Black, strokeWidth = 2.dp)
                        } else {
                            Text("Login", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    TextButton(onClick = onNavigateToRegistration) {
                        Text("Don't have an account? ", color = TextSecondary, fontSize = 13.sp)
                        Text("Register", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    if (authState is AuthState.Error) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = (authState as AuthState.Error).message,
                            color = Color(0xFFFF5252),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
