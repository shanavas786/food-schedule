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
    val membershipNo: String = "",    // optional membership / ID number
    val masterOrder: Int = 0,
    val skipByDefault: Boolean = false
)

/**
 * Per-iteration copy of a member.
 */
data class Member(
    val id: String,
    val name: String,
    val phone: String,
    val whatsapp: String,
    val membershipNo: String = "",
    val order: Int = 0,
    val skipIteration: Boolean = false,
    val assignedDate: String? = null,
    val confirmed: Boolean? = null
)
