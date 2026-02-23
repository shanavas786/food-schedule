package dotin.shanavasm.foodschedule.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ScheduleUiState(
    val members: List<Member> = emptyList(),
    val nextDate: String = "",
    val nextUpName: String = "",
    val currentIteration: Int = 1,
    val remainingThisIteration: Int = 0,
    val iterationHistory: List<IterationRecord> = emptyList(),
    val currentIterationEntries: List<IterationEntry> = emptyList(),
    // Transient — cleared after UI consumes it
    val justCompletedIteration: Int? = null
)

class ScheduleViewModel(application: Application) : AndroidViewModel(application) {

    private val dm = DataManager(application)

    private val _uiState = MutableStateFlow(snapshot())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    private fun snapshot(justCompleted: Int? = null) = ScheduleUiState(
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

    fun addMember(name: String, phone: String, whatsapp: String) {
        dm.addMember(name, phone, whatsapp); refresh()
    }

    fun updateMember(id: String, name: String, phone: String, whatsapp: String) {
        dm.updateMember(id, name, phone, whatsapp); refresh()
    }

    fun deleteMember(id: String) {
        dm.deleteMember(id); refresh()
    }

    fun toggleSkip(id: String) {
        dm.toggleSkip(id); refresh()
    }

    fun reorder(from: Int, to: Int) {
        dm.reorderMembers(from, to); refresh()
    }

    fun assignNext() {
        val result = dm.assignNext()
        refresh(justCompleted = if (result.iterationCompleted) result.completedIterationNumber else null)
    }

    /** Call after the UI has shown the "iteration complete" banner */
    fun clearIterationCompletedFlag() {
        _uiState.update { it.copy(justCompletedIteration = null) }
    }
}
