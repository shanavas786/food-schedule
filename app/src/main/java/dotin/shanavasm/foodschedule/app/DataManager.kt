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

    /** Permanent master list – order/skipByDefault live here */
    private var _masterMembers: MutableList<MasterMember> = mutableListOf()

    /** Current-iteration members (copied from master at iteration start) */
    private var _members: MutableList<Member> = mutableListOf()

    private var _currentIndex: Int = 0
    private var _nextDate: String = todayString()
    private var _currentIteration: Int = 1
    private var _iterationHistory: MutableList<IterationRecord> = mutableListOf()
    private var _currentIterationEntries: MutableList<IterationEntry> = mutableListOf()

    val masterMembers: List<MasterMember> get() = _masterMembers
    val members: List<Member> get() = _members
    val currentIndex: Int get() = _currentIndex
    val nextDate: String get() = _nextDate
    val currentIteration: Int get() = _currentIteration
    val iterationHistory: List<IterationRecord> get() = _iterationHistory
    val currentIterationEntries: List<IterationEntry> get() = _currentIterationEntries

    init { load() }

    // ── Persistence ───────────────────────────────────────────────────────────

    private fun load() {
        // Master members
        val masterJson = prefs.getString("masterMembers", null)
        _masterMembers = if (masterJson != null) {
            gson.fromJson<List<MasterMember>>(masterJson,
                object : TypeToken<List<MasterMember>>() {}.type).toMutableList()
        } else {
            // Migrate legacy "members" if present
            val legacyJson = prefs.getString("members", null)
            if (legacyJson != null) {
                val legacy = gson.fromJson<List<Member>>(legacyJson,
                    object : TypeToken<List<Member>>() {}.type)
                legacy.mapIndexed { i, m ->
                    MasterMember(id = m.id, name = m.name, phone = m.phone,
                        whatsapp = m.whatsapp, membershipNo = m.membershipNo, masterOrder = i)
                }.toMutableList()
            } else {
                mutableListOf(
                    MasterMember(name = "Alice Johnson", phone = "+1 555-0101", whatsapp = "+1 555-0101", masterOrder = 0),
                    MasterMember(name = "Bob Smith",     phone = "+1 555-0102", whatsapp = "+1 555-0102", masterOrder = 1),
                    MasterMember(name = "Carol Davis",   phone = "+1 555-0103", whatsapp = "+1 555-0103", masterOrder = 2),
                    MasterMember(name = "David Lee",     phone = "+1 555-0104", whatsapp = "+1 555-0104", masterOrder = 3)
                )
            }
        }

        // Current iteration members (iteration-local copy)
        val membersJson = prefs.getString("iterationMembers", null)
        _members = if (membersJson != null) {
            gson.fromJson<List<Member>>(membersJson,
                object : TypeToken<List<Member>>() {}.type).toMutableList()
        } else {
            // Bootstrap first iteration from master
            buildIterationMembers()
        }

        _currentIndex     = prefs.getInt("currentIndex", 0)
        _nextDate         = prefs.getString("nextDate", todayString()) ?: todayString()
        _currentIteration = prefs.getInt("currentIteration", 1)

        val historyJson = prefs.getString("iterationHistory", null)
        _iterationHistory = if (historyJson != null) {
            gson.fromJson<List<IterationRecord>>(historyJson,
                object : TypeToken<List<IterationRecord>>() {}.type).toMutableList()
        } else mutableListOf()

        val entriesJson = prefs.getString("currentIterationEntries", null)
        _currentIterationEntries = if (entriesJson != null) {
            gson.fromJson<List<IterationEntry>>(entriesJson,
                object : TypeToken<List<IterationEntry>>() {}.type).toMutableList()
        } else mutableListOf()
    }

    private fun save() {
        prefs.edit()
            .putString("masterMembers", gson.toJson(_masterMembers))
            .putString("iterationMembers", gson.toJson(_members))
            .putInt("currentIndex", _currentIndex)
            .putString("nextDate", _nextDate)
            .putInt("currentIteration", _currentIteration)
            .putString("iterationHistory", gson.toJson(_iterationHistory))
            .putString("currentIterationEntries", gson.toJson(_currentIterationEntries))
            .apply()
    }

    /** Build a fresh iteration member list from master (respects skipByDefault) */
    private fun buildIterationMembers(): MutableList<Member> =
        _masterMembers.sortedBy { it.masterOrder }.mapIndexed { i, m ->
            Member(
                id            = m.id,
                name          = m.name,
                phone         = m.phone,
                whatsapp      = m.whatsapp,
                membershipNo  = m.membershipNo,
                order         = i,
                skipIteration = m.skipByDefault
            )
        }.toMutableList()

    // ── Master member mutations ───────────────────────────────────────────────

    fun addMember(name: String, phone: String, whatsapp: String, membershipNo: String = ""): List<MasterMember> {
        val newMaster = MasterMember(name = name, phone = phone, whatsapp = whatsapp,
            membershipNo = membershipNo, masterOrder = _masterMembers.size)
        _masterMembers.add(newMaster)
        _members.add(Member(id = newMaster.id, name = name, phone = phone,
            whatsapp = whatsapp, membershipNo = membershipNo, order = _members.size))
        save()
        return _masterMembers.toList()
    }

    fun updateMember(id: String, name: String, phone: String, whatsapp: String, membershipNo: String = ""): List<MasterMember> {
        _masterMembers.replaceAll { m ->
            if (m.id == id) m.copy(name = name, phone = phone, whatsapp = whatsapp, membershipNo = membershipNo) else m
        }
        _members.replaceAll { m ->
            if (m.id == id) m.copy(name = name, phone = phone, whatsapp = whatsapp, membershipNo = membershipNo) else m
        }
        save()
        return _masterMembers.toList()
    }

    fun deleteMember(id: String): List<MasterMember> {
        _masterMembers.removeAll { it.id == id }
        _masterMembers.forEachIndexed { i, m -> _masterMembers[i] = m.copy(masterOrder = i) }
        _members.removeAll { it.id == id }
        _members.forEachIndexed { i, m -> _members[i] = m.copy(order = i) }
        if (_currentIndex >= _members.size) _currentIndex = 0
        _currentIterationEntries.removeAll { it.memberId == id }
        save()
        return _masterMembers.toList()
    }

    /** Reorder master list */
    fun reorderMaster(from: Int, to: Int): List<MasterMember> {
        val item = _masterMembers.removeAt(from)
        _masterMembers.add(to, item)
        _masterMembers.forEachIndexed { i, m -> _masterMembers[i] = m.copy(masterOrder = i) }
        save()
        return _masterMembers.toList()
    }

    /** Reorder within the current iteration only */
    fun reorderIterationMembers(from: Int, to: Int): List<Member> {
        val item = _members.removeAt(from)
        _members.add(to, item)
        _members.forEachIndexed { i, m -> _members[i] = m.copy(order = i) }
        save()
        return _members.toList()
    }

    /** Toggle skipByDefault on master; also updates current iteration member */
    fun toggleSkipByDefault(id: String): List<MasterMember> {
        _masterMembers.replaceAll { m ->
            if (m.id == id) m.copy(skipByDefault = !m.skipByDefault) else m
        }
        save()
        return _masterMembers.toList()
    }

    /** Toggle skip for this iteration only */
    fun toggleSkipIteration(id: String): List<Member> {
        _members.replaceAll { m ->
            if (m.id == id) m.copy(skipIteration = !m.skipIteration) else m
        }
        save()
        return _members.toList()
    }

    // ── Schedule confirmation ─────────────────────────────────────────────────

    /** Confirm a member's scheduled date (mark as accepted) */
    fun confirmSchedule(memberId: String): List<Member> {
        _members.replaceAll { m ->
            if (m.id == memberId) m.copy(confirmed = true) else m
        }
        // Also update the entry in currentIterationEntries
        _currentIterationEntries.replaceAll { e ->
            if (e.memberId == memberId) e.copy(confirmed = true) else e
        }
        save()
        return _members.toList()
    }

    /** Remove/unschedule a member's assignment */
    fun removeSchedule(memberId: String): List<Member> {
        _members.replaceAll { m ->
            if (m.id == memberId) m.copy(assignedDate = null, confirmed = null) else m
        }
        _currentIterationEntries.removeAll { it.memberId == memberId }
        // If removed member was before currentIndex, adjust index
        val removedOrderIdx = _members.indexOfFirst { it.id == memberId }
        // Move currentIndex back so this member gets re-assigned
        val assignedCountBefore = _currentIterationEntries.size
        // Recalculate currentIndex: find first non-assigned non-skipped member
        recalcCurrentIndex()
        save()
        return _members.toList()
    }

    private fun recalcCurrentIndex() {
        val assignedIds = _currentIterationEntries.map { it.memberId }.toSet()
        for (i in _members.indices) {
            val m = _members[i]
            if (!m.skipIteration && m.id !in assignedIds) {
                _currentIndex = i
                return
            }
        }
        _currentIndex = 0
    }

    /** Manually set the scheduled date for a member */
    fun updateScheduledDate(memberId: String, newDate: String): List<Member> {
        _members.replaceAll { m ->
            if (m.id == memberId) m.copy(assignedDate = newDate, confirmed = null) else m
        }
        _currentIterationEntries.replaceAll { e ->
            if (e.memberId == memberId) e.copy(assignedDate = newDate) else e
        }
        save()
        return _members.toList()
    }

    // ── Assignment logic ──────────────────────────────────────────────────────

    data class AssignResult(
        val members: List<Member>,
        val masterMembers: List<MasterMember>,
        val nextDate: String,
        val iterationCompleted: Boolean,
        val completedIterationNumber: Int = 0
    )

    fun assignNext(): AssignResult {
        if (_members.isEmpty()) return AssignResult(_members.toList(), _masterMembers.toList(), _nextDate, false)

        var assignIdx = -1
        for (i in _members.indices) {
            val idx = (_currentIndex + i) % _members.size
            if (!_members[idx].skipIteration) { assignIdx = idx; break }
        }
        if (assignIdx == -1) return AssignResult(_members.toList(), _masterMembers.toList(), _nextDate, false)

        // Clear any previous assignment on this date
        _members.replaceAll { m ->
            if (m.assignedDate == _nextDate) m.copy(assignedDate = null, confirmed = null) else m
        }

        val assigned = _members[assignIdx]
        _members[assignIdx] = assigned.copy(assignedDate = _nextDate, confirmed = null)

        val entry = IterationEntry(
            memberId     = assigned.id,
            memberName   = assigned.name,
            assignedDate = _nextDate
        )
        _currentIterationEntries.removeAll { it.memberId == assigned.id }
        _currentIterationEntries.add(entry)

        val assignedDate = _nextDate
        _currentIndex = (assignIdx + 1) % _members.size
        _nextDate = nextDayString(_nextDate)

        val eligibleIds = _members.filter { !it.skipIteration }.map { it.id }.toSet()
        val assignedIds = _currentIterationEntries.map { it.memberId }.toSet()
        val iterationCompleted = eligibleIds.isNotEmpty() && eligibleIds == assignedIds

        var completedNumber = 0
        if (iterationCompleted) {
            completedNumber = _currentIteration
            val record = IterationRecord(
                iterationNumber = _currentIteration,
                startedDate     = _currentIterationEntries.minByOrNull { it.assignedDate }?.assignedDate ?: assignedDate,
                completedDate   = assignedDate,
                entries         = _currentIterationEntries.toList()
            )
            _iterationHistory.add(0, record)

            _currentIteration++
            _currentIterationEntries.clear()
            // Start new iteration from master list
            _members = buildIterationMembers()
            _currentIndex = 0
        }

        save()
        return AssignResult(_members.toList(), _masterMembers.toList(), _nextDate, iterationCompleted, completedNumber)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Manually override the date that will be used for the next assignment */
    fun setNextDate(dateStr: String) {
        _nextDate = dateStr
        save()
    }

    fun nextUpName(): String {
        if (_members.isEmpty()) return "—"
        for (i in _members.indices) {
            val idx = (_currentIndex + i) % _members.size
            if (!_members[idx].skipIteration) return _members[idx].name
        }
        return "All skipped"
    }

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

    // ── Export / Import ───────────────────────────────────────────────────────

    /**
     * Serialise the entire database to a pretty-printed JSON string.
     * The envelope contains a version field so future migrations can be handled gracefully.
     */
    fun exportJson(): String {
        val envelope = mapOf(
            "version"                  to 1,
            "exportedAt"               to todayString(),
            "masterMembers"            to _masterMembers,
            "iterationMembers"         to _members,
            "currentIndex"             to _currentIndex,
            "nextDate"                 to _nextDate,
            "currentIteration"         to _currentIteration,
            "iterationHistory"         to _iterationHistory,
            "currentIterationEntries"  to _currentIterationEntries
        )
        return gson.toJson(envelope)
    }

    /**
     * Replace the entire in-memory state (and persist) from a JSON string previously
     * produced by [exportJson].  Returns an error message on failure, null on success.
     */
    fun importJson(json: String): String? {
        return try {
            val mapType = object : TypeToken<Map<String, Any>>() {}.type
            val map: Map<String, Any> = gson.fromJson(json, mapType)

            fun <T> decode(key: String, type: TypeToken<T>): T? {
                val raw = map[key] ?: return null
                return gson.fromJson(gson.toJson(raw), type.type)
            }

            val masterMembers = decode("masterMembers",
                object : TypeToken<List<MasterMember>>() {}) ?: return "Missing masterMembers"
            val members = decode("iterationMembers",
                object : TypeToken<List<Member>>() {}) ?: return "Missing iterationMembers"
            val history = decode("iterationHistory",
                object : TypeToken<List<IterationRecord>>() {}) ?: emptyList<IterationRecord>()
            val entries = decode("currentIterationEntries",
                object : TypeToken<List<IterationEntry>>() {}) ?: emptyList<IterationEntry>()

            val currentIndex     = (map["currentIndex"] as? Double)?.toInt() ?: 0
            val currentIteration = (map["currentIteration"] as? Double)?.toInt() ?: 1
            val nextDate         = map["nextDate"] as? String ?: todayString()

            _masterMembers           = masterMembers.toMutableList()
            _members                 = members.toMutableList()
            _currentIndex            = currentIndex
            _nextDate                = nextDate
            _currentIteration        = currentIteration
            _iterationHistory        = history.toMutableList()
            _currentIterationEntries = entries.toMutableList()

            save()
            null  // success
        } catch (e: Exception) {
            "Import failed: ${e.message}"
        }
    }

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
                display.format(parser.parse(isoDate)!!).toString()
            } catch (e: Exception) { isoDate ?: "" }
        }
    }
}
