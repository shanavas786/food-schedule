package dotin.shanavasm.foodschedule.app

import java.util.UUID

/**
 * Master record – lives forever, drives the master list.
 * [skipByDefault] means this member is skipped in every new iteration unless manually re-included.
 */
data class MasterMember(
    val id: String = UUID.randomUUID().toString().take(8),
    val name: String,
    val phone: String,
    val whatsapp: String,
    val masterOrder: Int = 0,     // order in the master list
    val skipByDefault: Boolean = false
)

/**
 * Per-iteration copy of a member.
 * [skipIteration] can differ from [MasterMember.skipByDefault] – user may toggle within an iteration.
 * [order] is the iteration-local display order (drag-reorder only affects this).
 * [assignedDate] is set when the member is scheduled; [confirmed] tracks acceptance.
 */
data class Member(
    val id: String,               // matches MasterMember.id
    val name: String,
    val phone: String,
    val whatsapp: String,
    val order: Int = 0,
    val skipIteration: Boolean = false,
    val assignedDate: String? = null,   // "yyyy-MM-dd"
    val confirmed: Boolean? = null      // null = pending, true = confirmed, false = removed
)
