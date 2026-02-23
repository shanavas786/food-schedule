package dotin.shanavasm.foodschedule.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ScheduleUiState(
    val masterMembers: List<MasterMember> = emptyList(),
    val members: List<Member> = emptyList(),
    val nextDate: String = "",
    val nextUpName: String = "",
    val currentIteration: Int = 1,
    val remainingThisIteration: Int = 0,
    val iterationHistory: List<IterationRecord> = emptyList(),
    val currentIterationEntries: List<IterationEntry> = emptyList(),
    val justCompletedIteration: Int? = null
)

class ScheduleViewModel(application: Application) : AndroidViewModel(application) {

    private val dm = DataManager(application)

    private val _uiState = MutableStateFlow(snapshot())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    private fun snapshot(justCompleted: Int? = null) = ScheduleUiState(
        masterMembers            = dm.masterMembers.toList(),
        members                  = dm.members.toList(),
        nextDate                 = dm.nextDate,
        nextUpName               = dm.nextUpName(),
        currentIteration         = dm.currentIteration,
        remainingThisIteration   = dm.remainingThisIteration(),
        iterationHistory         = dm.iterationHistory.toList(),
        currentIterationEntries  = dm.currentIterationEntries.toList(),
        justCompletedIteration   = justCompleted
    )

    private fun refresh(justCompleted: Int? = null) {
        _uiState.update { snapshot(justCompleted) }
    }

    // Master list mutations
    fun addMember(name: String, phone: String, whatsapp: String) {
        dm.addMember(name, phone, whatsapp); refresh()
    }
    fun updateMember(id: String, name: String, phone: String, whatsapp: String) {
        dm.updateMember(id, name, phone, whatsapp); refresh()
    }
    fun deleteMember(id: String) {
        dm.deleteMember(id); refresh()
    }
    fun reorderMaster(from: Int, to: Int) {
        dm.reorderMaster(from, to); refresh()
    }
    fun toggleSkipByDefault(id: String) {
        dm.toggleSkipByDefault(id); refresh()
    }

    // Iteration-local mutations
    fun reorderIteration(from: Int, to: Int) {
        dm.reorderIterationMembers(from, to); refresh()
    }
    fun toggleSkipIteration(id: String) {
        dm.toggleSkipIteration(id); refresh()
    }

    // Schedule actions
    fun confirmSchedule(memberId: String) {
        dm.confirmSchedule(memberId); refresh()
    }
    fun removeSchedule(memberId: String) {
        dm.removeSchedule(memberId); refresh()
    }
    fun updateScheduledDate(memberId: String, newDate: String) {
        dm.updateScheduledDate(memberId, newDate); refresh()
    }

    fun setNextDate(dateStr: String) {
        dm.setNextDate(dateStr); refresh()
    }

    fun assignNext() {
        val result = dm.assignNext()
        refresh(justCompleted = if (result.iterationCompleted) result.completedIterationNumber else null)
    }

    fun clearIterationCompletedFlag() {
        _uiState.update { it.copy(justCompletedIteration = null) }
    }
}
