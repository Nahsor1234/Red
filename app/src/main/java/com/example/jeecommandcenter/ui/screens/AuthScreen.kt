package com.example.jeecommandcenter.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeecommandcenter.data.CloudJeeRepository
import com.example.jeecommandcenter.ui.components.JeeCard
import com.example.jeecommandcenter.ui.components.JeeTopBar
import com.example.jeecommandcenter.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private enum class AuthMode { SIGN_IN, SIGN_UP }

enum class CloudAuthMode { SIGN_IN, SIGN_UP }

@Composable
fun AuthScreen(
    initialMode: CloudAuthMode = CloudAuthMode.SIGN_IN,
    onBack: () -> Unit,
    onAuthenticated: () -> Unit
) {
    val cloud = remember { CloudJeeRepository() }
    val scope = rememberCoroutineScope()
    var mode by remember(initialMode) { mutableStateOf(initialMode.toInternal()) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var waitingForConfirmation by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    // When the user confirms the email in a browser, Supabase redirects back
    // into Sigma JE. This also covers the case where the Auth screen is still
    // visible while the confirmation happens on another app/browser.
    LaunchedEffect(waitingForConfirmation) {
        if (!waitingForConfirmation) return@LaunchedEffect
        val deadline = System.currentTimeMillis() + 120_000L
        while (isActive && System.currentTimeMillis() < deadline) {
            if (runCatching { cloud.currentUser() }.getOrNull() != null) {
                waitingForConfirmation = false
                onAuthenticated()
                break
            }
            delay(1_000L)
        }
    }

    fun submit() {
        val normalizedEmail = email.trim()
        error = when {
            normalizedEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches() ->
                "Enter a valid email address."
            password.length < 6 -> "Password must be at least 6 characters."
            mode == AuthMode.SIGN_UP && password != confirmPassword -> "Passwords do not match."
            else -> null
        }
        if (error != null) return

        scope.launch {
            busy = true
            message = null
            error = null
            waitingForConfirmation = false
            runCatching {
                if (mode == AuthMode.SIGN_IN) {
                    cloud.signIn(normalizedEmail, password)
                    if (cloud.currentUser() == null) error("Sign in did not create an active session.")
                } else {
                    cloud.signUp(normalizedEmail, password)
                }
            }.onSuccess {
                val user = cloud.currentUser()
                if (user != null) {
                    message = "Signed in as ${user.email ?: normalizedEmail}."
                    onAuthenticated()
                } else {
                    mode = AuthMode.SIGN_IN
                    password = ""
                    confirmPassword = ""
                    waitingForConfirmation = true
                    message = "Confirmation email sent. Open it on this device and Sigma JE will finish the sign-in automatically. If you already confirmed this account, use Sign in instead."
                }
            }.onFailure {
                error = it.message?.takeIf(String::isNotBlank) ?: "Authentication failed. Please try again."
            }
            busy = false
        }
    }

    fun resendConfirmation() {
        val normalizedEmail = email.trim()
        if (normalizedEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches()) {
            error = "Enter the email address you used to create the account."
            return
        }
        scope.launch {
            busy = true
            error = null
            runCatching { cloud.resendConfirmation(normalizedEmail) }
                .onSuccess {
                    waitingForConfirmation = true
                    message = "Confirmation email sent again. Check your inbox and spam folder."
                }
                .onFailure {
                    error = "Could not resend confirmation. If the account is already confirmed, switch to Sign in."
                }
            busy = false
        }
    }

    Scaffold(
        containerColor = BgApp,
        topBar = { JeeTopBar(title = if (mode == AuthMode.SIGN_IN) "Sign in" else "Create account", onBack = onBack) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            JeeCard(featured = true) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Sigma JE account", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        if (mode == AuthMode.SIGN_IN)
                            "Sign in to sync your progress, question attempts and syllabus state across devices."
                        else
                            "Create an account to keep your Sigma JE progress backed up in the cloud.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it; error = null },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Filled.Email, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                enabled = !busy
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; error = null },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Filled.Lock, null) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                enabled = !busy
            )

            if (mode == AuthMode.SIGN_UP) {
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; error = null },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Confirm password") },
                    leadingIcon = { Icon(Icons.Filled.Lock, null) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    enabled = !busy
                )
            }

            error?.let { Text(it, color = Danger, fontSize = 12.sp) }
            message?.let { Text(it, color = AccentGreen, fontSize = 12.sp) }

            Button(
                onClick = ::submit,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else {
                    Icon(if (mode == AuthMode.SIGN_UP) Icons.Filled.PersonAdd else Icons.Filled.Lock, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (mode == AuthMode.SIGN_IN) "Sign in" else "Create account")
                }
            }

            if (waitingForConfirmation) {
                OutlinedButton(
                    onClick = ::resendConfirmation,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Email, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Resend confirmation email")
                }
            }

            TextButton(
                onClick = {
                    mode = if (mode == AuthMode.SIGN_IN) AuthMode.SIGN_UP else AuthMode.SIGN_IN
                    waitingForConfirmation = false
                    message = null
                    error = null
                },
                enabled = !busy,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(if (mode == AuthMode.SIGN_IN) "New to Sigma JE? Create an account" else "Already have an account? Sign in")
            }

            Text(
                "You can keep using Sigma JE offline without an account. Cloud sync is available after authentication.",
                color = TextMuted,
                fontSize = 10.sp
            )
        }
    }
}

private fun CloudAuthMode.toInternal(): AuthMode =
    if (this == CloudAuthMode.SIGN_UP) AuthMode.SIGN_UP else AuthMode.SIGN_IN
