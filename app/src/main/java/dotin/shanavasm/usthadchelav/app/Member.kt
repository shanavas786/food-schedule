package dotin.shanavasm.usthadchelav.app

import java.util.UUID

data class Member(
    val id: String = UUID.randomUUID().toString().take(8),
    val name: String,
    val phone: String,
    val whatsapp: String,
    val assignedDate: String? = null,   // "yyyy-MM-dd"
    val skipIteration: Boolean = false,
    val order: Int = 0
)
