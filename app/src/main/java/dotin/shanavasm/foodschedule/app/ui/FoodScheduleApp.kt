package dotin.shanavasm.foodschedule.app.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dotin.shanavasm.foodschedule.app.*
import org.burnoutcrew.reorderable.*

// ── Tabs ─────────────────────────────────────────────────────────────────────

enum class AppTab { SCHEDULE, HISTORY }

// ── Root composable ───────────────────────────────────────────────────────────

@Composable
fun FoodScheduleApp(vm: ScheduleViewModel = viewModel()) {
    val state by vm.uiState.collectAsState()
    var activeTab by remember { mutableStateOf(AppTab.SCHEDULE) }

    // Show a one-shot dialog when an iteration completes
    state.justCompletedIteration?.let { num ->
        AlertDialog(
            onDismissRequest = { vm.clearIterationCompletedFlag() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EmojiEvents, null,
                        tint = Color(0xFFFFA000), modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Iteration $num Complete!", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text("All members have been assigned in iteration $num. " +
                        "A new iteration (#${num + 1}) has started automatically.")
            },
            confirmButton = {
                Button(onClick = { vm.clearIterationCompletedFlag() }) { Text("Great!") }
            }
        )
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = activeTab == AppTab.SCHEDULE,
                    onClick  = { activeTab = AppTab.SCHEDULE },
                    icon = { Icon(Icons.Default.CalendarToday, null) },
                    label = { Text("Schedule") }
                )
                NavigationBarItem(
                    selected = activeTab == AppTab.HISTORY,
                    onClick  = { activeTab = AppTab.HISTORY },
                    icon = { Icon(Icons.Default.History, null) },
                    label = { Text("History") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (activeTab) {
                AppTab.SCHEDULE -> ScheduleTab(vm, state)
                AppTab.HISTORY  -> HistoryTab(state)
            }
        }
    }
}

// ── Schedule tab ──────────────────────────────────────────────────────────────

@Composable
fun ScheduleTab(vm: ScheduleViewModel, state: ScheduleUiState) {
    var showAddDialog  by remember { mutableStateOf(false) }
    var editTarget: Member?   by remember { mutableStateOf(null) }
    var deleteTarget: Member? by remember { mutableStateOf(null) }
    var snackMessage   by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(snackMessage) {
        snackMessage?.let { snackbarHostState.showSnackbar(it); snackMessage = null }
    }

    val reorderState = rememberReorderableLazyListState(
        onMove = { from, to -> vm.reorder(from.index - 1, to.index - 1) }
    )

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF1565C0),
                contentColor   = Color.White
            ) { Icon(Icons.Default.Add, "Add Member") }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF5F5F5)
    ) { padding ->

        LazyColumn(
            state = reorderState.listState,
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .padding(padding)
                .reorderable(reorderState)
                .detectReorderAfterLongPress(reorderState),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            item {
                HeaderCard(
                    nextDate          = DataManager.formatDisplayDate(state.nextDate).ifEmpty { state.nextDate },
                    nextUpName        = state.nextUpName,
                    currentIteration  = state.currentIteration,
                    remaining         = state.remainingThisIteration,
                    totalEligible     = state.members.count { !it.skipIteration },
                    onAssign          = { vm.assignNext(); snackMessage = "Assigned!" }
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Members  (long-press ≡ to reorder)",
                    modifier = Modifier.padding(start = 16.dp, bottom = 4.dp),
                    fontSize = 12.sp,
                    color    = Color(0xFF888888)
                )
            }

            items(
                count = state.members.size,
                key   = { state.members[it].id }
            ) { index ->
                val member = state.members[index]
                ReorderableItem(reorderState, key = member.id) { isDragging ->
                    MemberCard(
                        member       = member,
                        dragHandle   = {
                            IconButton(
                                modifier = Modifier.size(32.dp).detectReorder(reorderState),
                                onClick  = {}
                            ) {
                                Icon(Icons.Default.DragHandle, "Drag",
                                    tint = Color(0xFFAAAAAA))
                            }
                        },
                        onEdit       = { editTarget = member },
                        onDelete     = { deleteTarget = member },
                        onToggleSkip = {
                            vm.toggleSkip(member.id)
                            snackMessage = if (member.skipIteration)
                                "${member.name} included" else "${member.name} skipped"
                        },
                        modifier = if (isDragging) Modifier.background(Color(0xFFE3F2FD)) else Modifier
                    )
                }
            }
        }
    }

    // Dialogs
    if (showAddDialog) {
        MemberDialog(
            existing  = null,
            onDismiss = { showAddDialog = false },
            onSave    = { name, phone, whatsapp ->
                vm.addMember(name, phone, whatsapp)
                showAddDialog = false
                snackMessage  = "$name added"
            }
        )
    }

    editTarget?.let { m ->
        MemberDialog(
            existing  = m,
            onDismiss = { editTarget = null },
            onSave    = { name, phone, whatsapp ->
                vm.updateMember(m.id, name, phone, whatsapp)
                editTarget   = null
                snackMessage = "Updated"
            }
        )
    }

    deleteTarget?.let { m ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete Member") },
            text  = { Text("Remove ${m.name} from the schedule?") },
            confirmButton = {
                Button(
                    onClick = { vm.deleteMember(m.id); deleteTarget = null; snackMessage = "${m.name} deleted" },
                    colors  = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } }
        )
    }
}

// ── Header card ───────────────────────────────────────────────────────────────

@Composable
private fun HeaderCard(
    nextDate: String,
    nextUpName: String,
    currentIteration: Int,
    remaining: Int,
    totalEligible: Int,
    onAssign: () -> Unit
) {
    val assigned = totalEligible - remaining
    val progress = if (totalEligible > 0) assigned.toFloat() / totalEligible else 0f

    Card(
        modifier  = Modifier.fillMaxWidth().padding(12.dp),
        shape     = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(6.dp),
        colors    = CardDefaults.cardColors(containerColor = Color(0xFF1565C0))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🍽️ Food Schedule",
                    color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Surface(
                    shape  = RoundedCornerShape(20.dp),
                    color  = Color(0xFF0D47A1)
                ) {
                    Text(
                        "Iteration $currentIteration",
                        color    = Color(0xFFBBDEFB),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Progress bar
            Text(
                "$assigned / $totalEligible members assigned this iteration",
                color = Color(0xFFBBDEFB), fontSize = 13.sp
            )
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color    = Color(0xFF42A5F5),
                trackColor = Color(0xFF0D47A1)
            )

            Spacer(Modifier.height(8.dp))
            Text("Next date: $nextDate",     color = Color(0xFFBBDEFB), fontSize = 14.sp)
            Text("Next up: $nextUpName",     color = Color(0xFFE3F2FD), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            Button(
                onClick  = onAssign,
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1))
            ) {
                Text("✔  Assign Next Day", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── History tab ───────────────────────────────────────────────────────────────

@Composable
fun HistoryTab(state: ScheduleUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Current iteration in progress
        Card(
            modifier  = Modifier.fillMaxWidth().padding(12.dp),
            shape     = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            colors    = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Autorenew, null,
                        tint = Color(0xFF1565C0), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Iteration ${state.currentIteration} — In Progress",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1565C0),
                        fontSize = 15.sp
                    )
                }
                Spacer(Modifier.height(8.dp))
                if (state.currentIterationEntries.isEmpty()) {
                    Text("No assignments yet.", color = Color(0xFF666666), fontSize = 13.sp)
                } else {
                    state.currentIterationEntries
                        .sortedBy { it.assignedDate }
                        .forEach { entry ->
                            IterationEntryRow(entry)
                        }
                }
            }
        }

        if (state.iterationHistory.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.History, null,
                        tint = Color(0xFFCCCCCC), modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("No completed iterations yet.",
                        color = Color(0xFF999999), fontSize = 14.sp)
                }
            }
            return
        }

        Text(
            "Completed Iterations",
            modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
            fontSize = 13.sp, color = Color(0xFF888888)
        )

        LazyColumn(contentPadding = PaddingValues(bottom = 80.dp)) {
            items(state.iterationHistory, key = { it.iterationNumber }) { record ->
                IterationHistoryCard(record)
            }
        }
    }
}

@Composable
private fun IterationHistoryCard(record: IterationRecord) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        shape     = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(3.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier  = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null,
                        tint = Color(0xFF43A047), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Column {
                        Text("Iteration ${record.iterationNumber}",
                            fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            "${DataManager.formatFullDate(record.startedDate)} → " +
                                    "${DataManager.formatFullDate(record.completedDate)}",
                            fontSize = 12.sp, color = Color(0xFF888888)
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${record.entries.size} members",
                        fontSize = 12.sp, color = Color(0xFF666666))
                    IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            null, tint = Color(0xFF888888)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    HorizontalDivider(color = Color(0xFFEEEEEE))
                    Spacer(Modifier.height(6.dp))
                    record.entries.sortedBy { it.assignedDate }.forEach { entry ->
                        IterationEntryRow(entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun IterationEntryRow(entry: IterationEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Person, null,
                tint = Color(0xFF1565C0), modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(entry.memberName, fontSize = 13.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(
            DataManager.formatDisplayDate(entry.assignedDate),
            fontSize = 12.sp, color = Color(0xFF1565C0), fontWeight = FontWeight.Medium
        )
    }
}
