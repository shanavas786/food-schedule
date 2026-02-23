package dotin.shanavasm.usthadchelav.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dotin.shanavasm.usthadchelav.app.DataManager
import dotin.shanavasm.usthadchelav.app.Member
import dotin.shanavasm.usthadchelav.app.ScheduleViewModel
import org.burnoutcrew.reorderable.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleaningScheduleApp(vm: ScheduleViewModel = viewModel()) {
    val state by vm.uiState.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editTarget: Member? by remember { mutableStateOf(null) }
    var deleteTarget: Member? by remember { mutableStateOf(null) }
    var snackMessage by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(snackMessage) {
        snackMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackMessage = null
        }
    }

    // Reorderable list state
    val reorderState = rememberReorderableLazyListState(
        onMove = { from, to -> vm.reorder(from.index - 1, to.index - 1) }  // -1 offset for header
    )

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF1565C0),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Member")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        androidx.compose.foundation.lazy.LazyColumn(
            state = reorderState.listState,
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .padding(padding)
                .reorderable(reorderState)
                .detectReorderAfterLongPress(reorderState),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            // ── Header ──────────────────────────────────────────────────────
            item {
                HeaderCard(
                    nextDate = DataManager.formatDisplayDate(state.nextDate)
                        .ifEmpty { state.nextDate },
                    nextUpName = state.nextUpName,
                    onAssign = {
                        vm.assignNext()
                        snackMessage = "Assigned!"
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Members  (long-press ≡ to reorder)",
                    modifier = Modifier.padding(start = 16.dp, bottom = 4.dp),
                    fontSize = 12.sp,
                    color = Color(0xFF888888)
                )
            }

            // ── Employee rows ────────────────────────────────────────────────
            items(
                count = state.members.size,
                key = { state.members[it].id }
            ) { index ->
                val emp = state.members[index]
                ReorderableItem(reorderState, key = emp.id) { isDragging ->
                    MemberCard(
                        member = emp,
                        dragHandle = {
                            IconButton(
                                modifier = Modifier
                                    .size(32.dp)
                                    .detectReorder(reorderState),
                                onClick = {}
                            ) {
                                Icon(
                                    Icons.Default.DragHandle,
                                    contentDescription = "Drag to reorder",
                                    tint = Color(0xFFAAAAAA)
                                )
                            }
                        },
                        onEdit = { editTarget = emp },
                        onDelete = { deleteTarget = emp },
                        onToggleSkip = {
                            vm.toggleSkip(emp.id)
                            snackMessage = if (emp.skipIteration)
                                "${emp.name} included" else "${emp.name} skipped"
                        },
                        modifier = if (isDragging)
                            Modifier.background(Color(0xFFE3F2FD)) else Modifier
                    )
                }
            }
        }
    }

    // ── Dialogs ──────────────────────────────────────────────────────────────

    if (showAddDialog) {
        EmployeeDialog(
            existing = null,
            onDismiss = { showAddDialog = false },
            onSave = { name, phone, whatsapp ->
                vm.addMember(name, phone, whatsapp)
                showAddDialog = false
                snackMessage = "$name added"
            }
        )
    }

    editTarget?.let { emp ->
        EmployeeDialog(
            existing = emp,
            onDismiss = { editTarget = null },
            onSave = { name, phone, whatsapp ->
                vm.updateMember(emp.id, name, phone, whatsapp)
                editTarget = null
                snackMessage = "Updated"
            }
        )
    }

    deleteTarget?.let { emp ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete Employee") },
            text = { Text("Remove ${emp.name} from the schedule?") },
            confirmButton = {
                Button(
                    onClick = {
                        vm.deleteMember(emp.id)
                        deleteTarget = null
                        snackMessage = "${emp.name} deleted"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun HeaderCard(
    nextDate: String,
    nextUpName: String,
    onAssign: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1565C0))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "\uD83C\uDF72 Chelav Schedule",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text("Next date: $nextDate", color = Color(0xFFBBDEFB), fontSize = 14.sp)
            Text("Next up: $nextUpName", color = Color(0xFFE3F2FD), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onAssign,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1))
            ) {
                Text("✔  Assign Next Day", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
