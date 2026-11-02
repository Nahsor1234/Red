package com.example.jeecommandcenter.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.jeecommandcenter.data.CloudJeeRepository
import com.example.jeecommandcenter.ui.components.JeeCard
import com.example.jeecommandcenter.ui.components.JeePageHeader
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
    val focusManager = LocalFocusManager.current
    var mode by remember(initialMode) { mutableStateOf(initialMode.toInternal()) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var waitingForConfirmation by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    // Poll only while confirmation is pending; a confirmed session completes this flow automatically.
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
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Column(Modifier.fillMaxWidth().widthIn(max = 520.dp).align(Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                JeePageHeader(
                    title = if (mode == AuthMode.SIGN_IN) "Welcome back" else "Create your account",
                    subtitle = if (mode == AuthMode.SIGN_IN)
                        "Sign in to sync progress, question attempts and syllabus state across devices."
                    else "Create an account to keep your Sigma JE progress backed up in the cloud.",
                    eyebrow = "SIGMA JE · OPTIONAL CLOUD"
                )

                JeeCard(modifier = Modifier.animateContentSize()) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; error = null },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        enabled = !busy,
                        isError = error?.contains("email", ignoreCase = true) == true,
                        supportingText = {
                            error?.takeIf { it.contains("email", ignoreCase = true) }?.let { Text(it) }
                        }
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; error = null },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = if (mode == AuthMode.SIGN_UP) ImeAction.Next else ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) },
                            onDone = { focusManager.clearFocus(); submit() }
                        ),
                        enabled = !busy,
                        isError = error?.contains("password", ignoreCase = true) == true && error?.contains("match", ignoreCase = true) != true,
                        supportingText = {
                            error?.takeIf { it.contains("password", ignoreCase = true) && !it.contains("match", ignoreCase = true) }?.let { Text(it) }
                        }
                    )
                    if (mode == AuthMode.SIGN_UP) {
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it; error = null },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Confirm password") },
                            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); submit() }),
                            enabled = !busy,
                            isError = error?.contains("match", ignoreCase = true) == true,
                            supportingText = {
                                error?.takeIf { it.contains("match", ignoreCase = true) }?.let { Text(it) }
                            }
                        )
                    }

                    error?.takeUnless {
                        it.contains("email", ignoreCase = true) ||
                            it.contains("password", ignoreCase = true) ||
                            it.contains("match", ignoreCase = true)
                    }?.let {
                        Spacer(Modifier.height(12.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }
                    message?.let {
                        Spacer(Modifier.height(12.dp))
                        Text(it, color = Success, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(18.dp))
                    Button(onClick = { focusManager.clearFocus(); submit() }, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                        if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else {
                            Icon(if (mode == AuthMode.SIGN_UP) Icons.Filled.PersonAdd else Icons.Filled.Lock, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (mode == AuthMode.SIGN_IN) "Sign in" else "Create account")
                        }
                    }
                    if (waitingForConfirmation) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = ::resendConfirmation, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Icon(Icons.Filled.Email, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Resend confirmation email")
                        }
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
                    modifier = Modifier.align(Alignment.CenterHorizontally).heightIn(min = 48.dp)
                ) {
                    Text(if (mode == AuthMode.SIGN_IN) "New to Sigma JE? Create an account" else "Already have an account? Sign in")
                }

                Text(
                    "An account is optional. Keep using Sigma JE offline; cloud sync is available after authentication.",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private fun CloudAuthMode.toInternal(): AuthMode =
    if (this == CloudAuthMode.SIGN_UP) AuthMode.SIGN_UP else AuthMode.SIGN_IN
