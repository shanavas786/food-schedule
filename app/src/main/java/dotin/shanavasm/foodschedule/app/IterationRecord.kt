package dotin.shanavasm.foodschedule.app

/**
 * A single entry in the iteration history.
 * Each record represents one member's assignment within a completed iteration.
 */
data class IterationEntry(
    val memberId: String,
    val memberName: String,     // snapshot of name at time of assignment
    val assignedDate: String    // "yyyy-MM-dd"
)

/**
 * One full iteration — from when it started until all non-skipped members were assigned.
 */
data class IterationRecord(
    val iterationNumber: Int,
    val startedDate: String,    // "yyyy-MM-dd" of the first assignment in this iteration
    val completedDate: String?, // null if still in progress
    val entries: List<IterationEntry>
)
