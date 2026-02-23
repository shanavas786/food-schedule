package dotin.shanavasm.foodschedule.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import dotin.shanavasm.foodschedule.app.MasterMember

@Composable
fun MemberDialog(
    existing: MasterMember?,
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, whatsapp: String, membershipNo: String) -> Unit
) {
    val context = LocalContext.current

    var name         by remember { mutableStateOf(existing?.name ?: "") }
    var phone        by remember { mutableStateOf(existing?.phone ?: "") }
    var whatsapp     by remember { mutableStateOf(existing?.whatsapp ?: "") }
    var membershipNo by remember { mutableStateOf(existing?.membershipNo ?: "") }
    var nameError    by remember { mutableStateOf(false) }

    // ── Contact picker launcher ───────────────────────────────────────────────
    val contactPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickContact()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        // Resolve display name
        val nameCursor = context.contentResolver.query(
            uri,
            arrayOf(ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.Contacts._ID),
            null, null, null
        )
        var contactId: String? = null
        nameCursor?.use { c ->
            if (c.moveToFirst()) {
                name = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)) ?: name
                contactId = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
                nameError = false
            }
        }
        // Resolve phone numbers
        if (contactId != null) {
            val phoneCursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER,
                        ContactsContract.CommonDataKinds.Phone.TYPE),
                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                arrayOf(contactId), null
            )
            phoneCursor?.use { c ->
                // Prefer MOBILE type, fall back to first entry
                val numbers = mutableListOf<Pair<Int, String>>()
                while (c.moveToNext()) {
                    val type = c.getInt(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.TYPE))
                    val num  = c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)) ?: continue
                    numbers.add(type to num)
                }
                val primary = numbers.firstOrNull {
                    it.first == ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                }?.second ?: numbers.firstOrNull()?.second ?: ""
                if (phone.isBlank())    phone    = primary
                if (whatsapp.isBlank()) whatsapp = primary
            }
        }
    }

    // ── Contacts permission launcher ─────────────────────────────────────────
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) contactPickerLauncher.launch(null)
    }

    fun pickContact() {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) contactPickerLauncher.launch(null)
        else permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
    }

    // ─────────────────────────────────────────────────────────────────────────

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (existing == null) "Add Member" else "Edit Member",
                    fontWeight = FontWeight.Bold
                )
                if (existing == null) {
                    // "Pick from contacts" button — only shown when adding
                    TextButton(
                        onClick = { pickContact() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Contacts, null,
                            modifier = Modifier.size(18.dp),
                            tint = Color(0xFF1565C0)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Pick Contact", fontSize = 13.sp, color = Color(0xFF1565C0))
                    }
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // Show "Pick Contact" as a full-width button inside the form when editing too
                if (existing != null) {
                    OutlinedButton(
                        onClick = { pickContact() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Contacts, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Pick from Contacts")
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = false },
                    label = { Text("Full Name *") },
                    isError = nameError,
                    supportingText = if (nameError) ({ Text("Name is required") }) else null,
                    leadingIcon = { Icon(Icons.Default.Person, null, modifier = Modifier.size(20.dp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = membershipNo,
                    onValueChange = { membershipNo = it },
                    label = { Text("Membership No.") },
                    placeholder = { Text("e.g. MBR-001") },
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
                onSave(name.trim(), phone.trim(), whatsapp.trim(), membershipNo.trim())
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
                Text("Set the scheduled date for $memberName:", fontSize = 13.sp, color = Color(0xFF666666))
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
