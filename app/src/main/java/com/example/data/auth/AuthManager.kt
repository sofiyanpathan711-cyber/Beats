package com.example.data.auth

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class AuthState {
    object Unauthenticated : AuthState()
    object Loading : AuthState()
    data class Authenticated(val userId: String, val email: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

object AuthManager {
    private const val TAG = "AuthManager"
    private var firebaseAuth: FirebaseAuth? = null
    private var isFirebaseAvailable = false
    
    // In-memory persistent credentials fallback for demo simulation
    private val simulatedUsers = mutableMapOf<String, String>() // Email to Password
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun initialize(context: Context) {
        try {
            // Attempt to initialize Firebase securely 
            val app = if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            } else {
                FirebaseApp.getInstance()
            }
            if (app != null) {
                firebaseAuth = FirebaseAuth.getInstance()
                isFirebaseAvailable = true
                Log.d(TAG, "Firebase Authentication active successfully.")
                
                // Set initial status based on current user
                val currentUser = firebaseAuth?.currentUser
                if (currentUser != null) {
                    _authState.value = AuthState.Authenticated(
                        userId = currentUser.uid,
                        email = currentUser.email ?: "firebase_user@beats.com"
                    )
                } else {
                    _authState.value = AuthState.Unauthenticated
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firebase configuration ignored or not loaded: ${e.localizedMessage}. Running in offline fallback mode.")
            isFirebaseAvailable = false
            _authState.value = AuthState.Unauthenticated
        }
    }

    fun isFirebaseActive(): Boolean = isFirebaseAvailable

    fun signIn(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty() || password.isEmpty()) {
            _authState.value = AuthState.Error("Email and password cannot be empty")
            onResult(false, "Fields cannot be empty")
            return
        }

        _authState.value = AuthState.Loading

        if (isFirebaseAvailable && firebaseAuth != null) {
            firebaseAuth?.signInWithEmailAndPassword(trimmedEmail, password)
                ?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = firebaseAuth?.currentUser
                        if (user != null) {
                            _authState.value = AuthState.Authenticated(
                                userId = user.uid,
                                email = user.email ?: trimmedEmail
                            )
                            onResult(true, null)
                        } else {
                            _authState.value = AuthState.Error("Failed to fetch authenticated user credentials")
                            onResult(false, "User state invalid")
                        }
                    } else {
                        val errMsg = task.exception?.localizedMessage ?: "Invalid login credentials"
                        _authState.value = AuthState.Error(errMsg)
                        onResult(false, errMsg)
                    }
                }
        } else {
            // Fallback simulation: verify credentials offline for instant emulator preview
            val registeredPass = simulatedUsers[trimmedEmail]
            if (registeredPass == password) {
                val demoId = "simulated_user_" + trimmedEmail.hashCode().toString()
                _authState.value = AuthState.Authenticated(userId = demoId, email = trimmedEmail)
                onResult(true, null)
            } else if (registeredPass != null) {
                _authState.value = AuthState.Error("Incorrect password")
                onResult(false, "Incorrect password")
            } else {
                // To keep the developer & emulator experience frictionless, if they input a new user we automatically register them!
                simulatedUsers[trimmedEmail] = password
                val demoId = "simulated_user_" + trimmedEmail.hashCode().toString()
                _authState.value = AuthState.Authenticated(userId = demoId, email = trimmedEmail)
                onResult(true, null)
            }
        }
    }

    fun signUp(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty() || password.length < 6) {
            _authState.value = AuthState.Error("Password must be at least 6 characters")
            onResult(false, "Password must be at least 6 characters")
            return
        }

        _authState.value = AuthState.Loading

        if (isFirebaseAvailable && firebaseAuth != null) {
            firebaseAuth?.createUserWithEmailAndPassword(trimmedEmail, password)
                ?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = firebaseAuth?.currentUser
                        if (user != null) {
                            _authState.value = AuthState.Authenticated(
                                userId = user.uid,
                                email = user.email ?: trimmedEmail
                            )
                            onResult(true, null)
                        } else {
                            _authState.value = AuthState.Error("Authentication succeeded but user identity not found")
                            onResult(false, "Invalid register state")
                        }
                    } else {
                        val errMsg = task.exception?.localizedMessage ?: "Sign up failed"
                        _authState.value = AuthState.Error(errMsg)
                        onResult(false, errMsg)
                    }
                }
        } else {
            // Fallback: register offline
            simulatedUsers[trimmedEmail] = password
            val demoId = "simulated_user_" + trimmedEmail.hashCode().toString()
            _authState.value = AuthState.Authenticated(userId = demoId, email = trimmedEmail)
            onResult(true, null)
        }
    }

    fun signOut() {
        if (isFirebaseAvailable && firebaseAuth != null) {
            firebaseAuth?.signOut()
        }
        _authState.value = AuthState.Unauthenticated
    }

    fun getUserId(): String? {
        return when (val state = _authState.value) {
            is AuthState.Authenticated -> state.userId
            else -> null
        }
    }
}
