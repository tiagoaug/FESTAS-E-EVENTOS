package com.example.data.local.entity

enum class ParticipantType(val label: String) {
    ADULT("Adulto"),
    CHILD("Criança")
}

data class ParticipantEntity(
    val id: String = "",
    val eventId: String = "",
    val name: String = "",
    val phone: String = "",
    val type: ParticipantType = ParticipantType.ADULT,
    val familyGroup: String = "Sem Família",
    val paidAmount: Double = 0.0,
    val notes: String = "",
    /** Confirmação de presença no evento. */
    val confirmed: Boolean = false
)
