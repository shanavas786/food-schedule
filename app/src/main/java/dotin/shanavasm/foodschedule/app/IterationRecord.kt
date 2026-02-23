package dotin.shanavasm.foodschedule.app

data class IterationEntry(
    val memberId: String,
    val memberName: String,
    val assignedDate: String,
    val confirmed: Boolean? = null   // null=pending, true=confirmed, false=removed
)

data class IterationRecord(
    val iterationNumber: Int,
    val startedDate: String,
    val completedDate: String?,
    val entries: List<IterationEntry>
)
