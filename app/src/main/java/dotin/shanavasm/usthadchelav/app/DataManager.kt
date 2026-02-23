package dotin.shanavasm.usthadchelav.app

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
        context.getSharedPreferences("CleaningSchedulePrefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    // ── Serializable mutable state ────────────────────────────────────────────

    private var _members: MutableList<Member> = mutableListOf()
    private var _currentIndex: Int = 0
    private var _nextDate: String = todayString()

    val members: List<Member> get() = _members
    val currentIndex: Int get() = _currentIndex
    val nextDate: String get() = _nextDate

    init {
        load()
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private fun load() {
        val json = prefs.getString("employees", null)
        if (json != null) {
            val type = object : TypeToken<List<Member>>() {}.type
            _members = gson.fromJson<List<Member>>(json, type).toMutableList()
        } else {
            _members = mutableListOf(
                Member(name = "Alice Johnson",  phone = "+1 555-0101", whatsapp = "+1 555-0101", order = 0),
                Member(name = "Bob Smith",      phone = "+1 555-0102", whatsapp = "+1 555-0102", order = 1),
                Member(name = "Carol Davis",    phone = "+1 555-0103", whatsapp = "+1 555-0103", order = 2),
                Member(name = "David Lee",      phone = "+1 555-0104", whatsapp = "+1 555-0104", order = 3)
            )
        }
        _currentIndex = prefs.getInt("currentIndex", 0)
        _nextDate = prefs.getString("nextDate", todayString()) ?: todayString()
    }

    private fun save() {
        prefs.edit()
            .putString("employees", gson.toJson(_members))
            .putInt("currentIndex", _currentIndex)
            .putString("nextDate", _nextDate)
            .apply()
    }

    // ── Mutations (each returns a snapshot for ViewModel) ─────────────────────

    fun addEmployee(name: String, phone: String, whatsapp: String): List<Member> {
        _members.add(
            Member(name = name, phone = phone, whatsapp = whatsapp, order = _members.size)
        )
        save()
        return members.toList()
    }

    fun updateEmployee(id: String, name: String, phone: String, whatsapp: String): List<Member> {
        _members.replaceAll { e ->
            if (e.id == id) e.copy(name = name, phone = phone, whatsapp = whatsapp) else e
        }
        save()
        return members.toList()
    }

    fun deleteEmployee(id: String): List<Member> {
        _members.removeAll { it.id == id }
        _members.forEachIndexed { i, e -> _members[i] = e.copy(order = i) }
        if (_currentIndex >= _members.size) _currentIndex = 0
        save()
        return members.toList()
    }

    fun reorderEmployees(from: Int, to: Int): List<Member> {
        val item = _members.removeAt(from)
        _members.add(to, item)
        _members.forEachIndexed { i, e -> _members[i] = e.copy(order = i) }
        save()
        return members.toList()
    }

    fun toggleSkip(id: String): List<Member> {
        _members.replaceAll { e ->
            if (e.id == id) e.copy(skipIteration = !e.skipIteration) else e
        }
        save()
        return members.toList()
    }

    /** Assign the next non-skipped employee to nextDate and advance the pointer. */
    fun assignNext(): Pair<List<Member>, String> {
        if (_members.isEmpty()) return members.toList() to _nextDate

        var assignIdx = -1
        for (i in _members.indices) {
            val idx = (_currentIndex + i) % _members.size
            if (!_members[idx].skipIteration) { assignIdx = idx; break }
        }
        if (assignIdx == -1) return members.toList() to _nextDate  // all skipped

        // Clear any previous assignment to the same date
        _members.replaceAll { e ->
            if (e.assignedDate == _nextDate) e.copy(assignedDate = null) else e
        }
        _members[assignIdx] = _members[assignIdx].copy(assignedDate = _nextDate)
        _currentIndex = (assignIdx + 1) % _members.size
        _nextDate = nextDayString(_nextDate)
        save()
        return members.toList() to _nextDate
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

    private fun todayString(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    private fun nextDayString(dateStr: String): String = try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance().apply { time = sdf.parse(dateStr)!!; add(Calendar.DAY_OF_YEAR, 1) }
        sdf.format(cal.time)
    } catch (e: Exception) { todayString() }

    companion object {
        fun formatDisplayDate(isoDate: String?): String {
            if (isoDate.isNullOrEmpty()) return ""
            return try {
                val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val display = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
                display.format(parser.parse(isoDate)!!)
            } catch (e: Exception) { isoDate }
        }
    }
}
