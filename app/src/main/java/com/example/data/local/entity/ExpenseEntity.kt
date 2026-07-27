package com.example.data.local.entity

enum class ExpenseCategory(val label: String) {
    FOOD("Comida & Salgados"),
    DRINK("Bebidas"),
    DECORATION("Decoração"),
    VENUE("Aluguel do Local"),
    ENTERTAINMENT("Som & Animação"),
    OTHER("Outros")
}

data class ExpenseEntity(
    val id: String = "",
    val eventId: String = "",
    val title: String = "",
    val category: ExpenseCategory = ExpenseCategory.OTHER,
    val amount: Double = 0.0,
    val isPurchased: Boolean = false,
    val dateAddedMillis: Long = System.currentTimeMillis()
)
