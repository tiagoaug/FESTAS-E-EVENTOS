package com.example.data.local.entity

enum class CostShareMode {
    EQUAL,            // Divisão igualitária entre todos
    FIXED_TYPE,       // Valores fixos (Adulto / Criança)
    ORGANIZER_ONLY,   // Organizador assume custo total sozinho
    DEFINE_LATER      // Calculado depois, com base na lista de participantes
}

data class EventEntity(
    val id: String = "",
    val title: String = "",
    val eventType: String = "Aniversário",
    val eventDateMillis: Long = 0L,
    val location: String = "",
    val budget: Double = 0.0,
    val costShareMode: CostShareMode = CostShareMode.EQUAL,
    val fixedAdultPrice: Double = 0.0,
    val fixedChildPrice: Double = 0.0,
    val invitationTemplate: String = "Olá {nome}! Você está convidado(a) para {evento} no dia {data} no local {local}. Sua contribuição: {valor}. Confirme sua presença!"
)
