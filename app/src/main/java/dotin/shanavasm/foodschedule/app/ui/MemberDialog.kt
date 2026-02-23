package dotin.shanavasm.foodschedule.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dotin.shanavasm.foodschedule.app.MasterMember

@Composable
fun MemberDialog(
    existing: MasterMember?,
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, whatsapp: String) -> Unit
) {
    var name      by remember { mutableStateOf(existing?.name ?: "") }
    var phone     by remember { mutableStateOf(existing?.phone ?: "") }
    var whatsapp  by remember { mutableStateOf(existing?.whatsapp ?: "") }
    var nameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existing == null) "Add Member" else "Edit Member",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = false },
                    label = { Text("Full Name *") },
                    isError = nameError,
                    supportingText = if (nameError) ({ Text("Name is required") }) else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    placeholder = { Text("+1 555-0100") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = whatsapp,
                    onValueChange = { whatsapp = it },
                    label = { Text("WhatsApp Number") },
                    placeholder = { Text("+1 555-0100") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isBlank()) { nameError = true; return@Button }
                onSave(name.trim(), phone.trim(), whatsapp.trim())
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/** Simple date picker dialog using text input (yyyy-MM-dd) */
@Composable
fun EditDateDialog(
    currentDate: String?,
    memberName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var dateText by remember { mutableStateOf(currentDate ?: "") }
    var error    by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Schedule Date", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Set the scheduled date for ${memberName}:", fontSize = 13.sp, color = Color(0xFF666666))
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it; error = false },
                    label = { Text("Date (yyyy-MM-dd)") },
                    placeholder = { Text("e.g. 2025-01-15") },
                    isError = error,
                    supportingText = if (error) ({ Text("Enter a valid date (yyyy-MM-dd)") }) else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val trimmed = dateText.trim()
                if (!trimmed.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                    error = true; return@Button
                }
                onSave(trimmed)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
