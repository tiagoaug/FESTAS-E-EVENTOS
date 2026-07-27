package com.example.data.repository

import com.example.data.local.entity.CostShareMode
import com.example.data.local.entity.EventEntity
import com.example.data.local.entity.ExpenseCategory
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.ParticipantEntity
import com.example.data.local.entity.ParticipantType
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

private fun Query.snapshots(): Flow<List<DocumentSnapshot>> = callbackFlow {
    val registration = addSnapshotListener { snapshot, error ->
        if (error != null) {
            close(error)
            return@addSnapshotListener
        }
        trySend(snapshot?.documents ?: emptyList())
    }
    awaitClose { registration.remove() }
}

private fun documentToEvent(doc: DocumentSnapshot): EventEntity = EventEntity(
    id = doc.id,
    title = doc.getString("title") ?: "",
    eventType = doc.getString("eventType") ?: "Aniversário",
    eventDateMillis = doc.getLong("eventDateMillis") ?: 0L,
    location = doc.getString("location") ?: "",
    budget = doc.getDouble("budget") ?: 0.0,
    costShareMode = runCatching { CostShareMode.valueOf(doc.getString("costShareMode") ?: "EQUAL") }
        .getOrDefault(CostShareMode.EQUAL),
    fixedAdultPrice = doc.getDouble("fixedAdultPrice") ?: 0.0,
    fixedChildPrice = doc.getDouble("fixedChildPrice") ?: 0.0,
    invitationTemplate = doc.getString("invitationTemplate") ?: "",
)

private fun EventEntity.toMap(): Map<String, Any?> = mapOf(
    "title" to title,
    "eventType" to eventType,
    "eventDateMillis" to eventDateMillis,
    "location" to location,
    "budget" to budget,
    "costShareMode" to costShareMode.name,
    "fixedAdultPrice" to fixedAdultPrice,
    "fixedChildPrice" to fixedChildPrice,
    "invitationTemplate" to invitationTemplate,
)

private fun documentToParticipant(doc: DocumentSnapshot): ParticipantEntity = ParticipantEntity(
    id = doc.id,
    eventId = doc.getString("eventId") ?: "",
    name = doc.getString("name") ?: "",
    phone = doc.getString("phone") ?: "",
    type = runCatching { ParticipantType.valueOf(doc.getString("type") ?: "ADULT") }
        .getOrDefault(ParticipantType.ADULT),
    familyGroup = doc.getString("familyGroup") ?: "Sem Família",
    paidAmount = doc.getDouble("paidAmount") ?: 0.0,
    notes = doc.getString("notes") ?: "",
)

private fun ParticipantEntity.toMap(): Map<String, Any?> = mapOf(
    "eventId" to eventId,
    "name" to name,
    "phone" to phone,
    "type" to type.name,
    "familyGroup" to familyGroup,
    "paidAmount" to paidAmount,
    "notes" to notes,
)

private fun documentToExpense(doc: DocumentSnapshot): ExpenseEntity = ExpenseEntity(
    id = doc.id,
    eventId = doc.getString("eventId") ?: "",
    title = doc.getString("title") ?: "",
    category = runCatching { ExpenseCategory.valueOf(doc.getString("category") ?: "OTHER") }
        .getOrDefault(ExpenseCategory.OTHER),
    amount = doc.getDouble("amount") ?: 0.0,
    isPurchased = doc.getBoolean("isPurchased") ?: false,
    dateAddedMillis = doc.getLong("dateAddedMillis") ?: System.currentTimeMillis(),
)

private fun ExpenseEntity.toMap(): Map<String, Any?> = mapOf(
    "eventId" to eventId,
    "title" to title,
    "category" to category.name,
    "amount" to amount,
    "isPurchased" to isPurchased,
    "dateAddedMillis" to dateAddedMillis,
)

/**
 * Firestore-backed repository, scoped to a single signed-in user (users/{uid}/...).
 */
class PartyRepository(uid: String) {
    private val db = FirebaseFirestore.getInstance()
    private val userDoc = db.collection("users").document(uid)
    private val eventsCollection = userDoc.collection("events")
    private val participantsCollection = userDoc.collection("participants")
    private val expensesCollection = userDoc.collection("expenses")

    val allEvents: Flow<List<EventEntity>> = eventsCollection.snapshots()
        .map { docs -> docs.map(::documentToEvent).sortedBy { it.eventDateMillis } }

    val activeEventId: Flow<String?> = callbackFlow {
        val registration = userDoc.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.getString("activeEventId"))
        }
        awaitClose { registration.remove() }
    }

    fun getParticipants(eventId: String): Flow<List<ParticipantEntity>> =
        participantsCollection.whereEqualTo("eventId", eventId).snapshots()
            .map { docs -> docs.map(::documentToParticipant).sortedWith(compareBy({ it.familyGroup }, { it.name })) }

    fun getExpenses(eventId: String): Flow<List<ExpenseEntity>> =
        expensesCollection.whereEqualTo("eventId", eventId).snapshots()
            .map { docs -> docs.map(::documentToExpense).sortedByDescending { it.dateAddedMillis } }

    suspend fun createInitialSampleEventIfEmpty() {
        val existing = allEvents.firstOrNull()
        if (existing.isNullOrEmpty()) {
            val futureTime = System.currentTimeMillis() + (14 * 24 * 60 * 60 * 1000L) // 14 dias
            val sampleEvent = EventEntity(
                title = "Aniversário de 30 Anos",
                eventDateMillis = futureTime,
                location = "Espaço Festas & Cia, Av. Paulista 1000",
                budget = 2500.0,
                costShareMode = CostShareMode.FIXED_TYPE,
                fixedAdultPrice = 50.0,
                fixedChildPrice = 20.0,
            )
            val eventRef = eventsCollection.add(sampleEvent.toMap()).await()
            userDoc.set(mapOf("activeEventId" to eventRef.id), SetOptions.merge()).await()

            val participants = listOf(
                ParticipantEntity(eventId = eventRef.id, name = "Lucas Silva", phone = "5511999990001", type = ParticipantType.ADULT, familyGroup = "Família Silva", paidAmount = 50.0),
                ParticipantEntity(eventId = eventRef.id, name = "Mariane Silva", phone = "5511999990002", type = ParticipantType.ADULT, familyGroup = "Família Silva", paidAmount = 50.0),
                ParticipantEntity(eventId = eventRef.id, name = "Pedro Silva", phone = "5511999990003", type = ParticipantType.CHILD, familyGroup = "Família Silva", paidAmount = 20.0),
                ParticipantEntity(eventId = eventRef.id, name = "Carlos Oliveira", phone = "5511999990004", type = ParticipantType.ADULT, familyGroup = "Família Oliveira", paidAmount = 50.0),
                ParticipantEntity(eventId = eventRef.id, name = "Ana Oliveira", phone = "5511999990005", type = ParticipantType.ADULT, familyGroup = "Família Oliveira", paidAmount = 0.0),
                ParticipantEntity(eventId = eventRef.id, name = "Sofia Oliveira", phone = "5511999990006", type = ParticipantType.CHILD, familyGroup = "Família Oliveira", paidAmount = 0.0),
                ParticipantEntity(eventId = eventRef.id, name = "Roberto Souza", phone = "5511999990007", type = ParticipantType.ADULT, familyGroup = "Família Souza", paidAmount = 25.0),
            )
            participants.forEach { participantsCollection.add(it.toMap()).await() }

            val expenses = listOf(
                ExpenseEntity(eventId = eventRef.id, title = "Aluguel do Espaço", category = ExpenseCategory.VENUE, amount = 1000.0, isPurchased = true),
                ExpenseEntity(eventId = eventRef.id, title = "Salgados & Doces", category = ExpenseCategory.FOOD, amount = 450.0, isPurchased = true),
                ExpenseEntity(eventId = eventRef.id, title = "Carne & Churrasco", category = ExpenseCategory.FOOD, amount = 600.0, isPurchased = false),
                ExpenseEntity(eventId = eventRef.id, title = "Refrigerantes & Sucos", category = ExpenseCategory.DRINK, amount = 200.0, isPurchased = false),
                ExpenseEntity(eventId = eventRef.id, title = "Decoração com Balões", category = ExpenseCategory.DECORATION, amount = 250.0, isPurchased = true),
            )
            expenses.forEach { expensesCollection.add(it.toMap()).await() }
        }
    }

    suspend fun insertEvent(event: EventEntity): String {
        val ref = eventsCollection.add(event.toMap()).await()
        userDoc.set(mapOf("activeEventId" to ref.id), SetOptions.merge()).await()
        return ref.id
    }

    suspend fun updateEvent(event: EventEntity) {
        eventsCollection.document(event.id).set(event.toMap()).await()
    }

    suspend fun selectActiveEvent(eventId: String) {
        userDoc.set(mapOf("activeEventId" to eventId), SetOptions.merge()).await()
    }

    suspend fun deleteEvent(event: EventEntity) {
        val participantsSnap = participantsCollection.whereEqualTo("eventId", event.id).get().await()
        participantsSnap.documents.forEach { it.reference.delete().await() }

        val expensesSnap = expensesCollection.whereEqualTo("eventId", event.id).get().await()
        expensesSnap.documents.forEach { it.reference.delete().await() }

        eventsCollection.document(event.id).delete().await()

        val currentActiveId = userDoc.get().await().getString("activeEventId")
        if (currentActiveId == event.id) {
            val remaining = allEvents.firstOrNull()?.filterNot { it.id == event.id }
            userDoc.set(mapOf("activeEventId" to remaining?.firstOrNull()?.id), SetOptions.merge()).await()
        }
    }

    suspend fun insertParticipant(participant: ParticipantEntity) {
        participantsCollection.add(participant.toMap()).await()
    }

    suspend fun updateParticipant(participant: ParticipantEntity) {
        participantsCollection.document(participant.id).set(participant.toMap()).await()
    }

    suspend fun updatePayment(participantId: String, paidAmount: Double) {
        participantsCollection.document(participantId).update("paidAmount", paidAmount).await()
    }

    suspend fun deleteParticipant(participant: ParticipantEntity) {
        participantsCollection.document(participant.id).delete().await()
    }

    suspend fun insertExpense(expense: ExpenseEntity) {
        expensesCollection.add(expense.toMap()).await()
    }

    suspend fun updateExpense(expense: ExpenseEntity) {
        expensesCollection.document(expense.id).set(expense.toMap()).await()
    }

    suspend fun updateExpensePurchased(expenseId: String, isPurchased: Boolean) {
        expensesCollection.document(expenseId).update("isPurchased", isPurchased).await()
    }

    suspend fun deleteExpense(expense: ExpenseEntity) {
        expensesCollection.document(expense.id).delete().await()
    }
}
