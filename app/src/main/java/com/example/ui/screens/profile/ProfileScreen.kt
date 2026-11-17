package com.example.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.credentials.CredentialManager
import com.example.data.model.GoalItem
import com.example.data.model.UserProfile
import com.example.data.repository.NexoraRepository
import android.content.Context
import android.widget.Toast
import com.example.ui.components.GlassCard
import com.example.ui.screens.auth.onGoogleSignInClicked
import com.example.ui.screens.auth.performSignOut
import com.example.ui.theme.NexoraCardBackground
import com.example.ui.theme.NexoraErrorRed
import com.example.ui.theme.NexoraGlassCardBorder
import com.example.ui.theme.NexoraMainBackground
import com.example.ui.theme.NexoraNeonPurpleGlow
import com.example.ui.theme.NexoraPurpleHighlight
import com.example.ui.theme.NexoraPurplePrimary
import com.example.ui.theme.NexoraSuccessGreen
import com.example.ui.theme.NexoraTextMuted
import com.example.ui.theme.NexoraTextSecondary
import com.example.ui.theme.NexoraTextWhite
import com.example.ui.theme.NexoraWarningAmber
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun ProfileScreen(
    profile: UserProfile?,
    goals: List<GoalItem>,
    repository: NexoraRepository,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var showCreateGoalDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showReauthDeleteDialog by remember { mutableStateOf(false) }
    var isDeletingAccount by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val currentUser = Firebase.auth.currentUser

    fun executeCompleteDeletion() {
        val user = Firebase.auth.currentUser
        isDeletingAccount = true
        scope.launch {
            try {
                // 1. Delete all user documents in Cloud Firestore subcollections + user doc
                repository.deleteAllUserCloudData()

                // 2. Delete Firebase Auth account
                user?.delete()?.await()

                // 3. Clear local app SharedPreferences (step counter & alarm settings)
                context.getSharedPreferences("nexora_steps", Context.MODE_PRIVATE).edit().clear().apply()
                context.getSharedPreferences("nexora_alarms", Context.MODE_PRIVATE).edit().clear().apply()

                isDeletingAccount = false
                Toast.makeText(context, "Your NEXORA account and cloud data have been deleted.", Toast.LENGTH_LONG).show()
                performSignOut(context, credentialManager, onSignOut, scope)
            } catch (_: FirebaseAuthRecentLoginRequiredException) {
                isDeletingAccount = false
                showReauthDeleteDialog = true
            } catch (e: Exception) {
                isDeletingAccount = false
                Toast.makeText(
                    context,
                    e.localizedMessage ?: "Failed to delete account. Please try again.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NexoraMainBackground)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "USER PROFILE",
                color = NexoraPurpleHighlight,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = "Account & Objectives",
                color = NexoraTextWhite,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Profile Identity Card
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_identity_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(NexoraNeonPurpleGlow.copy(alpha = 0.2f))
                            .border(2.dp, NexoraPurplePrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (profile?.displayName?.take(1) ?: "A").uppercase(),
                            color = NexoraTextWhite,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = profile?.displayName?.ifBlank { "NEXORA Achiever" } ?: "NEXORA Achiever",
                            color = NexoraTextWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = currentUser?.email ?: (profile?.email ?: ""),
                            color = NexoraTextSecondary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(NexoraPurplePrimary.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = profile?.level ?: "Beginner",
                                    color = NexoraPurpleHighlight,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${profile?.xp ?: 0} XP",
                                color = NexoraTextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Goals Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PERSONAL GOALS",
                    color = NexoraTextWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                IconButton(
                    onClick = { showCreateGoalDialog = true },
                    modifier = Modifier.testTag("add_goal_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Goal",
                        tint = NexoraPurpleHighlight
                    )
                }
            }
        }

        if (goals.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No active goals yet",
                            color = NexoraTextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Define milestones for career, fitness, coding, or learning.",
                            color = NexoraTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            items(goals, key = { it.id }) { goal ->
                GoalItemCard(
                    goal = goal,
                    onToggleMilestone = { milestone ->
                        scope.launch {
                            repository.toggleGoalMilestone(goal.id, milestone)
                        }
                    },
                    onUpdateProgress = { newProgress, completed ->
                        scope.launch {
                            repository.updateGoalProgress(goal.id, newProgress, completed)
                        }
                    },
                    onDelete = {
                        scope.launch {
                            repository.deleteGoal(goal.id)
                        }
                    }
                )
            }
        }

        // Account Actions
        item {
            Text(
                text = "ACCOUNT ACTIONS",
                color = NexoraTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(top = 10.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_logout_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = NexoraCardBackground),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = null,
                            tint = NexoraPurpleHighlight
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Log Out", color = NexoraTextWhite)
                    }

                    Button(
                        onClick = { showDeleteAccountDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_delete_account_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = NexoraErrorRed.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = NexoraErrorRed
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete Account & Data", color = NexoraErrorRed)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (showCreateGoalDialog) {
        CreateGoalDialog(
            onDismiss = { showCreateGoalDialog = false },
            onConfirm = { title, desc, targetDate, category, milestones ->
                scope.launch {
                    repository.createGoal(title, desc, targetDate, category, milestones)
                    showCreateGoalDialog = false
                }
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log Out from NEXORA?", color = NexoraTextWhite) },
            text = { Text("Your progress, habits, and tasks will remain safely synchronized in Cloud Firestore.", color = NexoraTextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        performSignOut(context, credentialManager, onSignOut, scope)
                    }
                ) {
                    Text("Log Out", color = NexoraPurpleHighlight)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = NexoraTextSecondary)
                }
            },
            containerColor = NexoraCardBackground
        )
    }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { if (!isDeletingAccount) showDeleteAccountDialog = false },
            title = { Text("Permanently Delete Account?", color = NexoraErrorRed) },
            text = {
                Text(
                    "This permanently deletes your Firebase login, Cloud Firestore profile, tasks, habits, goals, routines, focus sessions, RICKY history, and local device records.\n\nWeb deletion policy: ab-dev-studio-prov4.vercel.app (or email abishekoffical3@gmail.com).",
                    color = NexoraTextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !isDeletingAccount,
                    onClick = {
                        showDeleteAccountDialog = false
                        executeCompleteDeletion()
                    }
                ) {
                    Text(
                        if (isDeletingAccount) "Deleting..." else "Delete Everything",
                        color = NexoraErrorRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isDeletingAccount,
                    onClick = { showDeleteAccountDialog = false }
                ) {
                    Text("Cancel", color = NexoraTextSecondary)
                }
            },
            containerColor = NexoraCardBackground
        )
    }

    if (showReauthDeleteDialog) {
        AlertDialog(
            onDismissRequest = { if (!isDeletingAccount) showReauthDeleteDialog = false },
            title = { Text("Confirm Google Identity", color = NexoraTextWhite) },
            text = {
                Text(
                    "For security, Google requires a fresh sign-in before permanently deleting your account. Tap Verify & Delete to confirm.",
                    color = NexoraTextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !isDeletingAccount,
                    onClick = {
                        showReauthDeleteDialog = false
                        onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = { executeCompleteDeletion() },
                            onAuthError = { msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            },
                            scope = scope
                        )
                    }
                ) {
                    Text("Verify & Delete", color = NexoraErrorRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReauthDeleteDialog = false }) {
                    Text("Cancel", color = NexoraTextSecondary)
                }
            },
            containerColor = NexoraCardBackground
        )
    }
}

@Composable
fun GoalItemCard(
    goal: GoalItem,
    onToggleMilestone: (String) -> Unit,
    onUpdateProgress: (Int, Boolean) -> Unit,
    onDelete: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.title,
                        color = NexoraTextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${goal.category} • Target: ${goal.targetDate ?: "Flexible"}",
                        color = NexoraPurpleHighlight,
                        fontSize = 12.sp
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Goal",
                        tint = NexoraTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { (goal.progress / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (goal.completed) NexoraSuccessGreen else NexoraPurplePrimary,
                trackColor = NexoraCardBackground
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${goal.progress}% Completed",
                    color = NexoraTextSecondary,
                    fontSize = 12.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (goal.progress < 100) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NexoraCardBackground)
                                .clickable {
                                    val next = (goal.progress + 25).coerceAtMost(100)
                                    onUpdateProgress(next, next == 100)
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("+25%", color = NexoraPurpleHighlight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (!goal.completed) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NexoraSuccessGreen.copy(alpha = 0.2f))
                                .clickable { onUpdateProgress(100, true) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Complete", color = NexoraSuccessGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Interactive Milestones
            if (goal.milestones.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Milestones (${goal.completedMilestones.size}/${goal.milestones.size})",
                    color = NexoraPurpleHighlight,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                goal.milestones.forEach { milestone ->
                    val isChecked = goal.completedMilestones.contains(milestone)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleMilestone(milestone) }
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { onToggleMilestone(milestone) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = NexoraPurplePrimary,
                                uncheckedColor = NexoraTextSecondary,
                                checkmarkColor = NexoraTextWhite
                            ),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = milestone,
                            color = if (isChecked) NexoraTextMuted else NexoraTextWhite,
                            fontSize = 13.sp,
                            textDecoration = if (isChecked) androidx.compose.ui.text.style.TextDecoration.LineThrough else androidx.compose.ui.text.style.TextDecoration.None
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CreateGoalDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String?, String?, String, List<String>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var targetDate by remember { mutableStateOf("") }
    var milestonesText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Career") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = NexoraMainBackground,
            border = androidx.compose.foundation.BorderStroke(1.dp, NexoraGlassCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "New Goal",
                    color = NexoraTextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Goal title (e.g. Master Jetpack Compose)", color = NexoraTextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NexoraPurplePrimary,
                        unfocusedBorderColor = NexoraGlassCardBorder,
                        focusedTextColor = NexoraTextWhite,
                        unfocusedTextColor = NexoraTextWhite,
                        focusedContainerColor = NexoraCardBackground,
                        unfocusedContainerColor = NexoraCardBackground
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = targetDate,
                    onValueChange = { targetDate = it },
                    placeholder = { Text("Target date (e.g. 2026-12-31)", color = NexoraTextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NexoraPurplePrimary,
                        unfocusedBorderColor = NexoraGlassCardBorder,
                        focusedTextColor = NexoraTextWhite,
                        unfocusedTextColor = NexoraTextWhite,
                        focusedContainerColor = NexoraCardBackground,
                        unfocusedContainerColor = NexoraCardBackground
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = milestonesText,
                    onValueChange = { milestonesText = it },
                    placeholder = { Text("Milestones (e.g. Read docs, Build UI, Add Tests)", color = NexoraTextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NexoraPurplePrimary,
                        unfocusedBorderColor = NexoraGlassCardBorder,
                        focusedTextColor = NexoraTextWhite,
                        unfocusedTextColor = NexoraTextWhite,
                        focusedContainerColor = NexoraCardBackground,
                        unfocusedContainerColor = NexoraCardBackground
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = NexoraCardBackground)
                    ) {
                        Text("Cancel", color = NexoraTextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                val parsedMilestones = milestonesText.split("\n", ",")
                                    .map { it.trim() }
                                    .filter { it.isNotBlank() }
                                onConfirm(title, description.ifBlank { null }, targetDate.ifBlank { null }, category, parsedMilestones)
                            }
                        },
                        enabled = title.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = NexoraPurplePrimary)
                    ) {
                        Text("Create Goal", color = NexoraTextWhite)
                    }
                }
            }
        }
    }
}
