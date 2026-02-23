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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dotin.shanavasm.foodschedule.app.*
import org.burnoutcrew.reorderable.*

enum class AppTab { SCHEDULE, MASTER, HISTORY }

@Composable
fun FoodScheduleApp(vm: ScheduleViewModel = viewModel()) {
    val state by vm.uiState.collectAsState()
    var activeTab by remember { mutableStateOf(AppTab.SCHEDULE) }

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
                Text(
                    "All members have been assigned in iteration $num. " +
                    "A new iteration (#${num + 1}) has started, with members reset from the master list."
                )
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
                    icon  = { Icon(Icons.Default.CalendarToday, null) },
                    label = { Text("Schedule") }
                )
                NavigationBarItem(
                    selected = activeTab == AppTab.MASTER,
                    onClick  = { activeTab = AppTab.MASTER },
                    icon  = { Icon(Icons.Default.Group, null) },
                    label = { Text("Master List") }
                )
                NavigationBarItem(
                    selected = activeTab == AppTab.HISTORY,
                    onClick  = { activeTab = AppTab.HISTORY },
                    icon  = { Icon(Icons.Default.History, null) },
                    label = { Text("History") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (activeTab) {
                AppTab.SCHEDULE -> ScheduleTab(vm, state)
                AppTab.MASTER   -> MasterListTab(vm, state)
                AppTab.HISTORY  -> HistoryTab(state)
            }
        }
    }
}

// ── Schedule tab (current iteration) ─────────────────────────────────────────

@Composable
fun ScheduleTab(vm: ScheduleViewModel, state: ScheduleUiState) {
    var snackMessage         by remember { mutableStateOf<String?>(null) }
    var editDateTarget: Member? by remember { mutableStateOf(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(snackMessage) {
        snackMessage?.let { snackbarHostState.showSnackbar(it); snackMessage = null }
    }

    val reorderState = rememberReorderableLazyListState(
        onMove = { from, to -> vm.reorderIteration(from.index - 1, to.index - 1) }
    )

    Scaffold(
        snackbarHost    = { SnackbarHost(snackbarHostState) },
        containerColor  = Color(0xFFF5F5F5)
    ) { padding ->

        LazyColumn(
            state    = reorderState.listState,
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
                    nextDateIso      = state.nextDate,
                    nextUpName       = state.nextUpName,
                    currentIteration = state.currentIteration,
                    remaining        = state.remainingThisIteration,
                    totalEligible    = state.members.count { !it.skipIteration },
                    onAssign         = { vm.assignNext(); snackMessage = "Assigned!" },
                    onNextDateChanged = { newDate -> vm.setNextDate(newDate) }
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "This Iteration  (long-press ≡ to reorder)",
                        fontSize = 12.sp,
                        color    = Color(0xFF888888),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "Reorder here won't affect Master List",
                        fontSize = 10.sp,
                        color    = Color(0xFFBBBBBB)
                    )
                }
            }

            items(
                count = state.members.size,
                key   = { state.members[it].id }
            ) { index ->
                val member = state.members[index]
                ReorderableItem(reorderState, key = member.id) { isDragging ->
                    MemberCard(
                        member   = member,
                        dragHandle = {
                            IconButton(
                                modifier = Modifier.size(32.dp).detectReorder(reorderState),
                                onClick  = {}
                            ) {
                                Icon(Icons.Default.DragHandle, "Drag",
                                    tint = Color(0xFFAAAAAA))
                            }
                        },
                        onEdit          = { /* editing handled from Master tab */ },
                        onDelete        = { /* deletion from Master tab */ },
                        onToggleSkip    = {
                            vm.toggleSkipIteration(member.id)
                            snackMessage = if (member.skipIteration)
                                "${member.name} included this iteration"
                            else "${member.name} skipped this iteration"
                        },
                        onConfirm       = {
                            vm.confirmSchedule(member.id)
                            snackMessage = "${member.name}'s schedule confirmed"
                        },
                        onRemoveSchedule = {
                            vm.removeSchedule(member.id)
                            snackMessage = "${member.name}'s schedule removed"
                        },
                        onEditDate = { editDateTarget = member },
                        modifier = if (isDragging) Modifier.background(Color(0xFFE3F2FD)) else Modifier
                    )
                }
            }
        }
    }

    // Edit date dialog
    editDateTarget?.let { m ->
        EditDateDialog(
            currentDate = m.assignedDate,
            memberName  = m.name,
            onDismiss   = { editDateTarget = null },
            onSave      = { newDate ->
                vm.updateScheduledDate(m.id, newDate)
                editDateTarget = null
                snackMessage   = "Date updated"
            }
        )
    }
}

// ── Master List tab ───────────────────────────────────────────────────────────

@Composable
fun MasterListTab(vm: ScheduleViewModel, state: ScheduleUiState) {
    var showAddDialog        by remember { mutableStateOf(false) }
    var editTarget: MasterMember? by remember { mutableStateOf(null) }
    var deleteTarget: MasterMember? by remember { mutableStateOf(null) }
    var snackMessage         by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(snackMessage) {
        snackMessage?.let { snackbarHostState.showSnackbar(it); snackMessage = null }
    }

    val reorderState = rememberReorderableLazyListState(
        onMove = { from, to -> vm.reorderMaster(from.index - 1, to.index - 1) }
    )

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick        = { showAddDialog = true },
                containerColor = Color(0xFF1565C0),
                contentColor   = Color.White
            ) { Icon(Icons.Default.Add, "Add Member") }
        },
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF5F5F5)
    ) { padding ->

        LazyColumn(
            state    = reorderState.listState,
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .padding(padding)
                .reorderable(reorderState)
                .detectReorderAfterLongPress(reorderState),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            item {
                Card(
                    modifier  = Modifier.fillMaxWidth().padding(12.dp),
                    shape     = RoundedCornerShape(12.dp),
                    colors    = CardDefaults.cardColors(containerColor = Color(0xFF0D47A1)),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Group, null,
                                tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Master Member List", fontWeight = FontWeight.Bold,
                                color = Color.White, fontSize = 16.sp)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Reordering here sets the default order for new iterations. " +
                            "\"Skip by default\" marks members as skipped at the start of every new iteration.",
                            color    = Color(0xFFBBDEFB),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
                Text(
                    "Members  (long-press ≡ to reorder master list)",
                    modifier = Modifier.padding(start = 16.dp, bottom = 4.dp),
                    fontSize = 12.sp, color = Color(0xFF888888)
                )
            }

            items(
                count = state.masterMembers.size,
                key   = { state.masterMembers[it].id }
            ) { index ->
                val master = state.masterMembers[index]
                ReorderableItem(reorderState, key = master.id) { isDragging ->
                    MasterMemberCard(
                        master   = master,
                        dragHandle = {
                            IconButton(
                                modifier = Modifier.size(32.dp).detectReorder(reorderState),
                                onClick  = {}
                            ) {
                                Icon(Icons.Default.DragHandle, "Drag",
                                    tint = Color(0xFFAAAAAA))
                            }
                        },
                        onEdit           = { editTarget = master },
                        onDelete         = { deleteTarget = master },
                        onToggleSkipDefault = {
                            vm.toggleSkipByDefault(master.id)
                            snackMessage = if (master.skipByDefault)
                                "${master.name} will be included by default"
                            else "${master.name} will be skipped by default"
                        },
                        modifier = if (isDragging) Modifier.background(Color(0xFFE3F2FD)) else Modifier
                    )
                }
            }
        }
    }

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
            text  = { Text("Remove ${m.name} from the master list and all future iterations?") },
            confirmButton = {
                Button(
                    onClick = {
                        vm.deleteMember(m.id)
                        deleteTarget = null
                        snackMessage = "${m.name} deleted"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } }
        )
    }
}

// ── Master member card ────────────────────────────────────────────────────────

@Composable
fun MasterMemberCard(
    master: MasterMember,
    dragHandle: @Composable () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleSkipDefault: () -> Unit,
    modifier: Modifier = Modifier
) {
    val alpha = if (master.skipByDefault) 0.6f else 1f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (master.skipByDefault) Color(0xFFFFF8E1) else Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            dragHandle()
            Spacer(Modifier.width(6.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = master.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF1A1A1A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (master.skipByDefault) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFFE0B2)
                        ) {
                            Text(
                                "Skip by default",
                                fontSize = 10.sp,
                                color    = Color(0xFFE65100),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(master.phone, fontSize = 13.sp, color = Color(0xFF666666))

                Spacer(Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Edit
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, "Edit",
                            tint = Color(0xFF555555), modifier = Modifier.size(18.dp))
                    }
                    // Delete
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, "Delete",
                            tint = Color(0xFFE53935), modifier = Modifier.size(18.dp))
                    }
                    // Toggle skip by default
                    IconButton(onClick = onToggleSkipDefault, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (master.skipByDefault)
                                Icons.Default.PlayCircle else Icons.Default.DoNotDisturb,
                            contentDescription = if (master.skipByDefault)
                                "Include by default" else "Skip by default",
                            tint = if (master.skipByDefault) Color(0xFF1565C0) else Color(0xFFE65100),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = if (master.skipByDefault) "Include by default" else "Skip by default",
                        fontSize = 11.sp,
                        color    = if (master.skipByDefault) Color(0xFF1565C0) else Color(0xFFE65100)
                    )
                }
            }

            // Order badge
            Box(
                modifier = Modifier
                    .background(Color(0xFFE8EAF6), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "#${master.masterOrder + 1}",
                    color      = Color(0xFF3949AB),
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ── Header card ───────────────────────────────────────────────────────────────

@Composable
private fun HeaderCard(
    nextDateIso: String,
    nextUpName: String,
    currentIteration: Int,
    remaining: Int,
    totalEligible: Int,
    onAssign: () -> Unit,
    onNextDateChanged: (String) -> Unit
) {
    val assigned = totalEligible - remaining
    val progress = if (totalEligible > 0) assigned.toFloat() / totalEligible else 0f

    // Inline date editing state
    var editing   by remember { mutableStateOf(false) }
    var dateInput by remember(nextDateIso) { mutableStateOf(nextDateIso) }
    var dateError by remember { mutableStateOf(false) }

    // Commit helper
    fun commitDate() {
        val trimmed = dateInput.trim()
        if (trimmed.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
            onNextDateChanged(trimmed)
            editing   = false
            dateError = false
        } else {
            dateError = true
        }
    }

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
                Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFF0D47A1)) {
                    Text(
                        "Iteration $currentIteration",
                        color    = Color(0xFFBBDEFB),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                "$assigned / $totalEligible members assigned this iteration",
                color = Color(0xFFBBDEFB), fontSize = 13.sp
            )
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress   = { progress },
                modifier   = Modifier.fillMaxWidth().height(6.dp),
                color      = Color(0xFF42A5F5),
                trackColor = Color(0xFF0D47A1)
            )

            Spacer(Modifier.height(10.dp))

            // ── Next date row ──────────────────────────────────────────────
            if (editing) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value         = dateInput,
                        onValueChange = { dateInput = it; dateError = false },
                        label         = { Text("Next date", color = Color(0xFFBBDEFB)) },
                        placeholder   = { Text("yyyy-MM-dd", color = Color(0x88FFFFFF)) },
                        isError       = dateError,
                        singleLine    = true,
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedTextColor       = Color.White,
                            unfocusedTextColor     = Color.White,
                            focusedBorderColor     = Color(0xFF90CAF9),
                            unfocusedBorderColor   = Color(0xFF64B5F6),
                            errorBorderColor       = Color(0xFFEF9A9A),
                            cursorColor            = Color.White,
                            focusedLabelColor      = Color(0xFFBBDEFB),
                            unfocusedLabelColor    = Color(0xFFBBDEFB)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(6.dp))
                    // Confirm
                    IconButton(onClick = { commitDate() }) {
                        Icon(Icons.Default.Check, "Confirm date",
                            tint = Color(0xFF81C784), modifier = Modifier.size(22.dp))
                    }
                    // Cancel
                    IconButton(onClick = { editing = false; dateInput = nextDateIso; dateError = false }) {
                        Icon(Icons.Default.Close, "Cancel",
                            tint = Color(0xFFEF9A9A), modifier = Modifier.size(22.dp))
                    }
                }
                if (dateError) {
                    Text("Enter a valid date  (yyyy-MM-dd)",
                        color = Color(0xFFEF9A9A), fontSize = 11.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp))
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Next date: ",
                        color    = Color(0xFFBBDEFB),
                        fontSize = 14.sp
                    )
                    Text(
                        DataManager.formatDisplayDate(nextDateIso).ifEmpty { nextDateIso },
                        color      = Color.White,
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.width(6.dp))
                    IconButton(
                        onClick  = { editing = true; dateInput = nextDateIso },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.EditCalendar, "Edit next date",
                            tint     = Color(0xFF90CAF9),
                            modifier = Modifier.size(16.dp))
                    }
                }
            }

            Text("Next up: $nextUpName",    color = Color(0xFFE3F2FD), fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold)
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
                        color      = Color(0xFF1565C0),
                        fontSize   = 15.sp
                    )
                }
                Spacer(Modifier.height(8.dp))
                if (state.currentIterationEntries.isEmpty()) {
                    Text("No assignments yet.", color = Color(0xFF666666), fontSize = 13.sp)
                } else {
                    state.currentIterationEntries
                        .sortedBy { it.assignedDate }
                        .forEach { entry -> IterationEntryRow(entry) }
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
            entry.confirmed?.let {
                Spacer(Modifier.width(6.dp))
                Text(
                    if (it) "✔" else "✖",
                    fontSize = 11.sp,
                    color    = if (it) Color(0xFF388E3C) else Color(0xFFE64A19)
                )
            }
        }
        Text(
            DataManager.formatDisplayDate(entry.assignedDate),
            fontSize = 12.sp, color = Color(0xFF1565C0), fontWeight = FontWeight.Medium
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    FoodScheduleApp()
}