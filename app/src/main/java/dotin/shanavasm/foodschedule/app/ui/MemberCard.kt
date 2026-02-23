package dotin.shanavasm.foodschedule.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dotin.shanavasm.foodschedule.app.DataManager
import dotin.shanavasm.foodschedule.app.Member

@Composable
fun MemberCard(
    member: Member,
    dragHandle: @Composable () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleSkip: () -> Unit,
    onConfirm: () -> Unit,
    onRemoveSchedule: () -> Unit,
    onEditDate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val alpha = if (member.skipIteration) 0.55f else 1f
    var showMessageDialog by remember { mutableStateOf(false) }

    if (showMessageDialog) {
        WhatsAppMessageDialog(
            memberName = member.name,
            onDismiss  = { showMessageDialog = false },
            onSend     = { message ->
                showMessageDialog = false
                val number  = member.whatsapp.replace(Regex("[^0-9]"), "")
                val encoded = java.net.URLEncoder.encode(message, "UTF-8")
                val intent  = Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://wa.me/$number?text=$encoded"))
                    .apply { setPackage("com.whatsapp") }
                try { context.startActivity(intent) }
                catch (e: Exception) { context.startActivity(intent.apply { setPackage(null) }) }
            }
        )
    }

    // Determine card accent based on confirmation state
    val cardBg = when {
        member.skipIteration -> Color.White
        member.confirmed == true -> Color(0xFFF1F8E9)   // light green
        member.confirmed == false -> Color(0xFFFFF3E0)  // light amber (removed)
        member.assignedDate != null -> Color(0xFFE3F2FD) // light blue – pending
        else -> Color.White
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .alpha(alpha),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)) {

            // ── Top row: drag + name + date badge ────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                dragHandle()
                Spacer(Modifier.width(6.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = member.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF1A1A1A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(text = member.phone, fontSize = 13.sp, color = Color(0xFF666666))
                    if (member.membershipNo.isNotBlank()) {
                        Text(
                            "No: ${member.membershipNo}",
                            fontSize = 12.sp,
                            color    = Color(0xFF1565C0),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Date badge + confirmation chip
                val dateLabel = DataManager.formatDisplayDate(member.assignedDate)
                if (dateLabel.isNotEmpty()) {
                    Column(horizontalAlignment = Alignment.End) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF1565C0).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(dateLabel, color = Color(0xFF1565C0), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(2.dp))
                        when (member.confirmed) {
                            true -> Text("✔ Confirmed", color = Color(0xFF388E3C), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            false -> Text("✖ Removed", color = Color(0xFFE64A19), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            null -> Text("Pending", color = Color(0xFF757575), fontSize = 10.sp)
                        }
                    }
                }
            }

            // ── Action row ───────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Call
                IconButton(
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${member.phone}")))
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Call, "Call",
                        tint = Color(0xFF1565C0), modifier = Modifier.size(18.dp))
                }

                // Open WhatsApp chat
                IconButton(
                    onClick = {
                        val number = member.whatsapp.replace(Regex("[^0-9]"), "")
                        val message = "${member.assignedDate} ന് ഉസ്താദിന്റെ ചെലവ് "
                        val encoded = java.net.URLEncoder.encode(message, "UTF-8")
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$number?text=$encoded"))
                            .apply { setPackage("com.whatsapp") }
                        try { context.startActivity(intent) }
                        catch (e: Exception) { context.startActivity(intent.apply { setPackage(null) }) }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Send, "WhatsApp",
                        tint = Color(0xFF25D366), modifier = Modifier.size(18.dp))
                }

//                // Send WhatsApp message
//                IconButton(onClick = { showMessageDialog = true }, modifier = Modifier.size(32.dp)) {
//                    Icon(Icons.Default.Message, "Send Message",
//                        tint = Color(0xFF25D366), modifier = Modifier.size(18.dp))
//                }

                // Skip this iteration
                IconButton(onClick = onToggleSkip, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = if (member.skipIteration) Icons.Default.PlayArrow else Icons.Default.Block,
                        contentDescription = if (member.skipIteration) "Include" else "Skip this iteration",
                        tint = if (member.skipIteration) Color(0xFF1565C0) else Color(0xFFFF8F00),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Only show confirm/remove/edit-date if there is an assigned date
                if (member.assignedDate != null && !member.skipIteration) {
                    Spacer(Modifier.width(2.dp))

                    // Edit date
                    IconButton(onClick = onEditDate, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.EditCalendar, "Edit Date",
                            tint = Color(0xFF1565C0), modifier = Modifier.size(18.dp))
                    }

                    // Confirm schedule
                    if (member.confirmed != true) {
                        IconButton(onClick = onConfirm, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.CheckCircle, "Confirm",
                                tint = Color(0xFF388E3C), modifier = Modifier.size(18.dp))
                        }
                    }

                    // Remove schedule
                    if (member.confirmed != false) {
                        IconButton(onClick = onRemoveSchedule, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.RemoveCircle, "Remove Schedule",
                                tint = Color(0xFFE53935), modifier = Modifier.size(18.dp))
                        }
                    }
                }

                if (member.skipIteration) {
                    Text("Skipped", color = Color(0xFFFF8F00), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun WhatsAppMessageDialog(
    memberName: String,
    onDismiss: () -> Unit,
    onSend: (String) -> Unit
) {
    var message      by remember { mutableStateOf("") }
    var messageError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Send, null,
                    tint = Color(0xFF25D366), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Message $memberName", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                Text("Type a message to send via WhatsApp:",
                    fontSize = 13.sp, color = Color(0xFF666666))
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it; messageError = false },
                    placeholder = { Text("e.g. Hi! It's your turn to bring food tomorrow.") },
                    isError = messageError,
                    supportingText = if (messageError) ({ Text("Message cannot be empty") }) else null,
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                Text("Quick messages:", fontSize = 12.sp, color = Color(0xFF888888))
                Spacer(Modifier.height(4.dp))
                listOf(
                    "Hi! It's your turn to bring food tomorrow 🍽️",
                    "Reminder: It's your food duty today!",
                    "Please don't forget your food schedule shift."
                ).forEach { quick ->
                    SuggestionChip(
                        onClick = { message = quick; messageError = false },
                        label = {
                            Text(quick, fontSize = 11.sp, maxLines = 1,
                                overflow = TextOverflow.Ellipsis)
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (message.isBlank()) { messageError = true; return@Button }
                    onSend(message.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
            ) {
                Icon(Icons.Default.Send, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Send")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
