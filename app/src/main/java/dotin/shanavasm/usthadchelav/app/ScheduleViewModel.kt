package dotin.shanavasm.usthadchelav.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ScheduleUiState(
    val members: List<Member> = emptyList(),
    val nextDate: String = "",
    val nextUpName: String = ""
)

class ScheduleViewModel(application: Application) : AndroidViewModel(application) {

    private val dataManager = DataManager(application)

    private val _uiState = MutableStateFlow(
        ScheduleUiState(
            members = dataManager.members.toList(),
            nextDate = dataManager.nextDate,
            nextUpName = dataManager.nextUpName()
        )
    )
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    private fun refresh(members: List<Member> = dataManager.members, nextDate: String = dataManager.nextDate) {
        _uiState.update { it.copy(members = members, nextDate = nextDate, nextUpName = dataManager.nextUpName()) }
    }

    fun addMember(name: String, phone: String, whatsapp: String) {
        refresh(dataManager.addEmployee(name, phone, whatsapp))
    }

    fun updateMember(id: String, name: String, phone: String, whatsapp: String) {
        refresh(dataManager.updateEmployee(id, name, phone, whatsapp))
    }

    fun deleteMember(id: String) {
        refresh(dataManager.deleteEmployee(id))
    }

    fun toggleSkip(id: String) {
        refresh(dataManager.toggleSkip(id))
    }

    fun reorder(from: Int, to: Int) {
        refresh(dataManager.reorderEmployees(from, to))
    }

    fun assignNext() {
        val (emps, date) = dataManager.assignNext()
        refresh(emps, date)
    }
}
