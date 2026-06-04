package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.auth.AuthManager
import com.example.data.auth.AuthState
import com.example.ui.theme.GlowMagenta
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TropicalTeal

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val authState by AuthManager.authState.collectAsState()
    
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUpMode by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()

    // Sync state for error messages
    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onAuthSuccess()
        } else if (authState is AuthState.Error) {
            errorMessage = (authState as AuthState.Error).message
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F1B)) // Deep cosmic canvas gradient background
            .testTag("auth_screen_container")
    ) {
        // Futuristic background glowing orb aesthetics
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            NeonViolet.copy(alpha = 0.2f),
                            Color.Transparent
                        ),
                        radius = 1200f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
                .statusBarsPadding()
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(30.dp))

            // Unified App Logo Emblem Card
            Box(
                modifier = Modifier
                    .size(85.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(NeonViolet, GlowMagenta, TropicalTeal)
                        )
                    )
                    .border(2.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                    .testTag("auth_logo_box"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(45.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "BEATS STUDIO",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Secure Your Personalized Playlists & Settings",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.618f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Main Credential Panel Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                NeonViolet.copy(alpha = 0.5f),
                                TropicalTeal.copy(alpha = 0.3f)
                            )
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .testTag("auth_panel_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF16162B).copy(alpha = 0.85f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isSignUpMode) "Create Account" else "Sign In",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .testTag("auth_panel_title"),
                        textAlign = TextAlign.Start
                    )

                    // Email input with proper testTag and icon
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            errorMessage = null
                        },
                        label = { Text("Email Address", color = Color.White.copy(alpha = 0.6f)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = TropicalTeal
                            )
                        },
                        textStyle = LocalTextStyle.current.copy(color = Color.White),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .testTag("email_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TropicalTeal,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            cursorColor = TropicalTeal
                        )
                    )

                    // Password input with visibility toggle
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMessage = null
                        },
                        label = { Text("Password", color = Color.White.copy(alpha = 0.6f)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = TropicalTeal
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle password visibility",
                                    tint = Color.White.copy(alpha = 0.5f)
                                )
                            }
                        },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordTransformationWrapper(),
                        textStyle = LocalTextStyle.current.copy(color = Color.White),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .testTag("password_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TropicalTeal,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            cursorColor = TropicalTeal
                        )
                    )

                    // Error presentation label with fade animation
                    AnimatedVisibility(
                        visible = errorMessage != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .testTag("auth_error_label"),
                            textAlign = TextAlign.Center
                        )
                    }

                    // Primary submit button
                    Button(
                        onClick = {
                            errorMessage = null
                            if (isSignUpMode) {
                                AuthManager.signUp(email, password) { success, err ->
                                    if (!success) errorMessage = err
                                }
                            } else {
                                AuthManager.signIn(email, password) { success, err ->
                                    if (!success) errorMessage = err
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("auth_submit_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonViolet
                        ),
                        enabled = authState !is AuthState.Loading
                    ) {
                        if (authState is AuthState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (isSignUpMode) "Register Account" else "Access Beats",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Redirect links to switch signup/signin
                    TextButton(
                        onClick = {
                            isSignUpMode = !isSignUpMode
                            errorMessage = null
                        },
                        modifier = Modifier.testTag("auth_toggle_mode_button")
                    ) {
                        Text(
                            text = if (isSignUpMode)
                                "Already have an account? Sign In"
                            else
                                "First time here? Sign Up",
                            color = TropicalTeal,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Frictionless Local Demonstration Access
            if (!AuthManager.isFirebaseActive()) {
                OutlinedButton(
                    onClick = {
                        // Effortless guest access gets simulated instantly
                        AuthManager.signIn("demo_guest@beats.com", "beats123") { _, _ -> }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("offline_sandbox_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, TropicalTeal.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TropicalTeal
                    )
                ) {
                    Text(
                        text = "Enter Offline Sandbox Mode",
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                }

                Text(
                    text = "No Firebase config detected. Running in secure offline-first local simulation for development.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }
    }
}

// Ensure clean custom presentation of password
@Composable
private fun PasswordTransformationWrapper(): VisualTransformation {
    return remember { PasswordVisualTransformation() }
}
