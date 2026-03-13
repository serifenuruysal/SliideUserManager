package com.sliide.usermanager.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddUserSheet(
    sheetState: SheetState,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (name: String, email: String, gender: String) -> Unit
) {
    var name   by remember { mutableStateOf("") }
    var email  by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("male") }

    val nameError  = name.isNotEmpty()  && name.trim().length < 2
    val emailError = email.isNotEmpty() && !email.contains("@")
    val canSubmit  = name.trim().length >= 2 && email.contains("@") && !isLoading

    val focusManager = LocalFocusManager.current

    // Explicit Posh Palette - Reverted to darker, high-contrast values
    val poshSheetBg = Color(0xFF1E293B) // Elevated background
    val poshInputSurface = Color(0xFF0F172A) // Darker inner surface for fields
    val poshBorder = Color(0xFF475569) // Clearly visible border
    val poshIndigo = Color(0xFF6366F1)
    val poshSilver = Color(0xFFCBD5E1) // High-contrast secondary text

    ModalBottomSheet(
        onDismissRequest = { if (!isLoading) onDismiss() },
        sheetState = sheetState,
        containerColor = poshSheetBg,
        contentColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle(color = poshBorder) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp)
                .imePadding()
        ) {
            Text(
                text  = "Create New Profile",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            if (isLoading) {
                Spacer(Modifier.height(32.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = poshIndigo,
                    trackColor = poshInputSurface
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Provisioning new user...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = poshSilver
                )
                return@Column
            }

            Spacer(Modifier.height(32.dp))

            OutlinedTextField(
                value          = name,
                onValueChange  = { name = it },
                label          = { Text("Full Name") },
                placeholder    = { Text("e.g. John Doe", color = poshBorder) },
                leadingIcon    = { Icon(Icons.Default.Person, contentDescription = null, tint = poshIndigo) },
                isError        = nameError,
                supportingText = if (nameError) {
                    { Text("Requires at least 2 characters") }
                } else null,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = poshIndigo,
                    unfocusedBorderColor = poshBorder,
                    focusedLabelColor = poshIndigo,
                    unfocusedLabelColor = poshSilver,
                    cursorColor = poshIndigo,
                    focusedContainerColor = poshInputSurface,
                    unfocusedContainerColor = poshInputSurface,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction      = ImeAction.Next
                ),
                singleLine = true,
                modifier   = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value         = email,
                onValueChange = { email = it.trim() },
                label         = { Text("Email Address") },
                placeholder   = { Text("e.g. john@example.com", color = poshBorder) },
                leadingIcon   = { Icon(Icons.Default.Email, contentDescription = null, tint = poshIndigo) },
                isError       = emailError,
                supportingText = if (emailError) {
                    { Text("Invalid email format") }
                } else null,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = poshIndigo,
                    unfocusedBorderColor = poshBorder,
                    focusedLabelColor = poshIndigo,
                    unfocusedLabelColor = poshSilver,
                    cursorColor = poshIndigo,
                    focusedContainerColor = poshInputSurface,
                    unfocusedContainerColor = poshInputSurface,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction    = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    if (canSubmit) onSubmit(name.trim(), email, gender)
                }),
                singleLine = true,
                modifier   = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            Text("Classification", style = MaterialTheme.typography.labelLarge, color = poshSilver)
            Row(modifier = Modifier.padding(top = 12.dp)) {
                listOf("male", "female").forEach { option ->
                    FilterChip(
                        selected = gender == option,
                        onClick  = { gender = option },
                        label    = { Text(option.replaceFirstChar { it.uppercase() }) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = poshIndigo,
                            selectedLabelColor = Color.White,
                            labelColor = poshSilver
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = poshBorder,
                            selectedBorderColor = poshIndigo,
                            borderWidth = 1.dp,
                            enabled = true,
                            selected = gender == option
                        ),
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }
            }

            Spacer(Modifier.height(40.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = onDismiss, 
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColors(contentColor = poshSilver)
                ) {
                    Text("Discard")
                }
                Button(
                    onClick  = { onSubmit(name.trim(), email, gender) },
                    enabled  = canSubmit,
                    modifier = Modifier.weight(1.4f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = poshIndigo,
                        disabledContainerColor = poshBorder.copy(alpha = 0.3f)
                    )
                ) {
                    Text("Create Profile", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
