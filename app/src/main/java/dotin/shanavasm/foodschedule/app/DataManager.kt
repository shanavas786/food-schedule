package dotin.shanavasm.foodschedule.app

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DataManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("FoodSchedulePrefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    // ── In-memory state ───────────────────────────────────────────────────────

    private var _members: MutableList<Member> = mutableListOf()
    private var _currentIndex: Int = 0          // next member to assign
    private var _nextDate: String = todayString()
    private var _currentIteration: Int = 1
    private var _iterationHistory: MutableList<IterationRecord> = mutableListOf()
    // Entries accumulated for the iteration currently in progress
    private var _currentIterationEntries: MutableList<IterationEntry> = mutableListOf()

    val members: List<Member> get() = _members
    val currentIndex: Int get() = _currentIndex
    val nextDate: String get() = _nextDate
    val currentIteration: Int get() = _currentIteration
    val iterationHistory: List<IterationRecord> get() = _iterationHistory
    val currentIterationEntries: List<IterationEntry> get() = _currentIterationEntries

    init { load() }

    // ── Persistence ───────────────────────────────────────────────────────────

    private fun load() {
        // Members
        val membersJson = prefs.getString("members", null)
        _members = if (membersJson != null) {
            gson.fromJson<List<Member>>(membersJson, object : TypeToken<List<Member>>() {}.type)
                .toMutableList()
        } else {
            mutableListOf(
                Member(name = "Alice Johnson", phone = "+1 555-0101", whatsapp = "+1 555-0101", order = 0),
                Member(name = "Bob Smith",     phone = "+1 555-0102", whatsapp = "+1 555-0102", order = 1),
                Member(name = "Carol Davis",   phone = "+1 555-0103", whatsapp = "+1 555-0103", order = 2),
                Member(name = "David Lee",     phone = "+1 555-0104", whatsapp = "+1 555-0104", order = 3)
            )
        }

        _currentIndex     = prefs.getInt("currentIndex", 0)
        _nextDate         = prefs.getString("nextDate", todayString()) ?: todayString()
        _currentIteration = prefs.getInt("currentIteration", 1)

        // History
        val historyJson = prefs.getString("iterationHistory", null)
        _iterationHistory = if (historyJson != null) {
            gson.fromJson<List<IterationRecord>>(historyJson,
                object : TypeToken<List<IterationRecord>>() {}.type).toMutableList()
        } else mutableListOf()

        // Current in-progress entries
        val entriesJson = prefs.getString("currentIterationEntries", null)
        _currentIterationEntries = if (entriesJson != null) {
            gson.fromJson<List<IterationEntry>>(entriesJson,
                object : TypeToken<List<IterationEntry>>() {}.type).toMutableList()
        } else mutableListOf()
    }

    private fun save() {
        prefs.edit()
            .putString("members", gson.toJson(_members))
            .putInt("currentIndex", _currentIndex)
            .putString("nextDate", _nextDate)
            .putInt("currentIteration", _currentIteration)
            .putString("iterationHistory", gson.toJson(_iterationHistory))
            .putString("currentIterationEntries", gson.toJson(_currentIterationEntries))
            .apply()
    }

    // ── Member mutations ──────────────────────────────────────────────────────

    fun addMember(name: String, phone: String, whatsapp: String): List<Member> {
        _members.add(Member(name = name, phone = phone, whatsapp = whatsapp, order = _members.size))
        save()
        return members.toList()
    }

    fun updateMember(id: String, name: String, phone: String, whatsapp: String): List<Member> {
        _members.replaceAll { m ->
            if (m.id == id) m.copy(name = name, phone = phone, whatsapp = whatsapp) else m
        }
        save()
        return members.toList()
    }

    fun deleteMember(id: String): List<Member> {
        _members.removeAll { it.id == id }
        _members.forEachIndexed { i, m -> _members[i] = m.copy(order = i) }
        if (_currentIndex >= _members.size) _currentIndex = 0
        // Remove any in-progress history entry for this member
        _currentIterationEntries.removeAll { it.memberId == id }
        save()
        return members.toList()
    }

    fun reorderMembers(from: Int, to: Int): List<Member> {
        val item = _members.removeAt(from)
        _members.add(to, item)
        _members.forEachIndexed { i, m -> _members[i] = m.copy(order = i) }
        save()
        return members.toList()
    }

    fun toggleSkip(id: String): List<Member> {
        _members.replaceAll { m ->
            if (m.id == id) m.copy(skipIteration = !m.skipIteration) else m
        }
        save()
        return members.toList()
    }

    // ── Assignment logic ──────────────────────────────────────────────────────

    data class AssignResult(
        val members: List<Member>,
        val nextDate: String,
        val iterationCompleted: Boolean,
        val completedIterationNumber: Int = 0
    )

    /**
     * Assign the next non-skipped member to [_nextDate].
     * When all eligible members in this iteration have been assigned,
     * the iteration is finalized, saved to history, and a new one begins.
     */
    fun assignNext(): AssignResult {
        if (_members.isEmpty()) return AssignResult(members.toList(), _nextDate, false)

        // Find next non-skipped member
        var assignIdx = -1
        for (i in _members.indices) {
            val idx = (_currentIndex + i) % _members.size
            if (!_members[idx].skipIteration) { assignIdx = idx; break }
        }
        if (assignIdx == -1) return AssignResult(members.toList(), _nextDate, false) // all skipped

        // Clear any previous assignment on this date
        _members.replaceAll { m ->
            if (m.assignedDate == _nextDate) m.copy(assignedDate = null) else m
        }

        val assigned = _members[assignIdx]
        _members[assignIdx] = assigned.copy(assignedDate = _nextDate)

        // Record this assignment in the current iteration entries
        val entry = IterationEntry(
            memberId    = assigned.id,
            memberName  = assigned.name,
            assignedDate = _nextDate
        )
        // Replace if this member was already recorded (e.g. re-assign edge case)
        _currentIterationEntries.removeAll { it.memberId == assigned.id }
        _currentIterationEntries.add(entry)

        val assignedDate = _nextDate
        _currentIndex = (assignIdx + 1) % _members.size
        _nextDate = nextDayString(_nextDate)

        // Check if this iteration is complete:
        // All non-skipped members have an entry in currentIterationEntries
        val eligibleIds = _members.filter { !it.skipIteration }.map { it.id }.toSet()
        val assignedIds = _currentIterationEntries.map { it.memberId }.toSet()
        val iterationCompleted = eligibleIds.isNotEmpty() && eligibleIds == assignedIds

        var completedNumber = 0
        if (iterationCompleted) {
            completedNumber = _currentIteration
            // Save completed iteration to history
            val record = IterationRecord(
                iterationNumber = _currentIteration,
                startedDate     = _currentIterationEntries.minByOrNull { it.assignedDate }?.assignedDate ?: assignedDate,
                completedDate   = assignedDate,
                entries         = _currentIterationEntries.toList()
            )
            _iterationHistory.add(0, record) // newest first

            // Start new iteration
            _currentIteration++
            _currentIterationEntries.clear()
            // Clear assigned dates for all members so next iteration starts fresh
            _members.replaceAll { m -> m.copy(assignedDate = null) }
        }

        save()
        return AssignResult(members.toList(), _nextDate, iterationCompleted, completedNumber)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    fun nextUpName(): String {
        if (_members.isEmpty()) return "—"
        for (i in _members.indices) {
            val idx = (_currentIndex + i) % _members.size
            if (!_members[idx].skipIteration) return _members[idx].name
        }
        return "All skipped"
    }

    /** How many non-skipped members still need to be assigned this iteration */
    fun remainingThisIteration(): Int {
        val eligibleIds = _members.filter { !it.skipIteration }.map { it.id }.toSet()
        val assignedIds = _currentIterationEntries.map { it.memberId }.toSet()
        return (eligibleIds - assignedIds).size
    }

    private fun todayString(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    private fun nextDayString(dateStr: String): String = try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance().apply {
            time = sdf.parse(dateStr)!!
            add(Calendar.DAY_OF_YEAR, 1)
        }
        sdf.format(cal.time)
    } catch (e: Exception) { todayString() }

    companion object {
        fun formatDisplayDate(isoDate: String?): String {
            if (isoDate.isNullOrEmpty()) return ""
            return try {
                val parser  = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val display = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
                display.format(parser.parse(isoDate)!!)
            } catch (e: Exception) { isoDate }
        }

        fun formatFullDate(isoDate: String?): String {
            if (isoDate.isNullOrEmpty()) return ""
            return try {
                val parser  = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val display = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                display.format(parser.parse(isoDate)!!)
            } catch (e: Exception) { isoDate ?: "" }
        }
    }
}
