package com.example.data.local.entity

data class ExpenseEntity(
    val id: String = "",
    val eventId: String = "",
    val title: String = "",
    /** Id de um documento em users/{uid}/categories. */
    val category: String = "",
    val amount: Double = 0.0,
    /** Status de compra: o item já foi comprado? */
    val isPurchased: Boolean = false,
    /** Status de pagamento: o item já foi pago ao fornecedor? Independente de isPurchased. */
    val isPaid: Boolean = false,
    val dateAddedMillis: Long = System.currentTimeMillis(),
    val notes: String = ""
)
