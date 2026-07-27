package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.CostShareMode
import com.example.data.local.entity.EventEntity
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.ParticipantEntity
import com.example.data.local.entity.ParticipantType
import com.example.data.repository.AuthRepository
import com.example.data.repository.PartyRepository
import com.example.ui.theme.AppFontId
import com.example.ui.theme.AppThemeId
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val SETTINGS_PREFS_NAME = "festas_eventos_settings"
private const val KEY_USE_WHATSAPP = "use_whatsapp"
private const val KEY_APP_THEME = "app_theme"
private const val KEY_APP_FONT = "app_font"

/** Crianças pagam esta fração do valor do adulto no modo CostShareMode.DEFINE_LATER. */
const val DEFINE_LATER_CHILD_WEIGHT = 0.5

data class FinancialSummary(
    val budget: Double = 0.0,
    val totalSpent: Double = 0.0,
    val totalPurchased: Double = 0.0,
    val budgetBalance: Double = 0.0,
    val totalExpectedCollection: Double = 0.0,
    val totalCollected: Double = 0.0,
    val missingCollection: Double = 0.0,
    val netBalance: Double = 0.0,
    val adultCount: Int = 0,
    val childCount: Int = 0,
    val totalParticipants: Int = 0
)

data class PartyUiState(
    val events: List<EventEntity> = emptyList(),
    val activeEvent: EventEntity? = null,
    val participants: List<ParticipantEntity> = emptyList(),
    val expenses: List<ExpenseEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val financialSummary: FinancialSummary = FinancialSummary(),
    val isLoading: Boolean = true
)

class PartyViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository(application)

    private val settingsPrefs = application.getSharedPreferences(SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)

    private val _useWhatsApp = MutableStateFlow(settingsPrefs.getBoolean(KEY_USE_WHATSAPP, true))
    val useWhatsApp: StateFlow<Boolean> = _useWhatsApp.asStateFlow()

    fun setUseWhatsApp(enabled: Boolean) {
        _useWhatsApp.value = enabled
        settingsPrefs.edit().putBoolean(KEY_USE_WHATSAPP, enabled).apply()
    }

    private val _appTheme = MutableStateFlow(
        runCatching { AppThemeId.valueOf(settingsPrefs.getString(KEY_APP_THEME, null) ?: "") }
            .getOrDefault(AppThemeId.VIOLETA)
    )
    val appTheme: StateFlow<AppThemeId> = _appTheme.asStateFlow()

    fun setAppTheme(theme: AppThemeId) {
        _appTheme.value = theme
        settingsPrefs.edit().putString(KEY_APP_THEME, theme.name).apply()
    }

    private val _appFont = MutableStateFlow(
        runCatching { AppFontId.valueOf(settingsPrefs.getString(KEY_APP_FONT, null) ?: "") }
            .getOrDefault(AppFontId.SYSTEM)
    )
    val appFont: StateFlow<AppFontId> = _appFont.asStateFlow()

    fun setAppFont(font: AppFontId) {
        _appFont.value = font
        settingsPrefs.edit().putString(KEY_APP_FONT, font.name).apply()
    }

    /** Repository for the currently signed-in user, or null when signed out. */
    private val repository: PartyRepository?
        get() = authRepository.currentUser?.uid?.let { PartyRepository(it) }

    val currentUser: FirebaseUser?
        get() = authRepository.currentUser

    val authState: StateFlow<FirebaseUser?> = authRepository.authState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = authRepository.currentUser,
    )

    val uiState: StateFlow<PartyUiState> = authRepository.authState
        .flatMapLatest { user ->
            if (user == null) {
                flowOf(PartyUiState(isLoading = false))
            } else {
                val userRepository = PartyRepository(user.uid)
                viewModelScope.launch { userRepository.createInitialSampleEventIfEmpty() }
                viewModelScope.launch { userRepository.seedDefaultCategoriesIfEmpty() }

                combine(
                    userRepository.allEvents,
                    userRepository.activeEventId,
                    userRepository.allCategories
                ) { events, activeId, categories ->
                    Triple(events, events.find { it.id == activeId }, categories)
                }.flatMapLatest { (events, active, categories) ->
                    if (active == null) {
                        flowOf(PartyUiState(events = events, categories = categories, isLoading = false))
                    } else {
                        combine(
                            userRepository.getParticipants(active.id),
                            userRepository.getExpenses(active.id)
                        ) { participants, expenses ->
                            val summary = calculateFinancialSummary(active, participants, expenses)
                            PartyUiState(
                                events = events,
                                activeEvent = active,
                                participants = participants,
                                expenses = expenses,
                                categories = categories,
                                financialSummary = summary,
                                isLoading = false
                            )
                        }
                    }
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PartyUiState()
        )

    fun signInWithGoogle(webClientId: String) = viewModelScope.launch {
        authRepository.signInWithGoogle(webClientId)
    }

    fun signOut() {
        authRepository.signOut()
    }

    fun calculateParticipantTarget(
        participant: ParticipantEntity,
        event: EventEntity,
        allParticipants: List<ParticipantEntity>
    ): Double {
        return when (event.costShareMode) {
            CostShareMode.ORGANIZER_ONLY -> 0.0
            CostShareMode.FIXED_TYPE -> {
                if (participant.type == ParticipantType.ADULT) event.fixedAdultPrice else event.fixedChildPrice
            }
            CostShareMode.EQUAL -> {
                if (allParticipants.isNotEmpty()) event.budget / allParticipants.size else 0.0
            }
            CostShareMode.DEFINE_LATER -> {
                val adultCount = allParticipants.count { it.type == ParticipantType.ADULT }
                val childCount = allParticipants.count { it.type == ParticipantType.CHILD }
                val totalWeight = adultCount + childCount * DEFINE_LATER_CHILD_WEIGHT
                if (totalWeight <= 0.0) return 0.0
                val unit = event.budget / totalWeight
                if (participant.type == ParticipantType.ADULT) unit else unit * DEFINE_LATER_CHILD_WEIGHT
            }
        }
    }

    fun calculateDefineLaterSuggestion(event: EventEntity, allParticipants: List<ParticipantEntity>): Pair<Double, Double> {
        val adultCount = allParticipants.count { it.type == ParticipantType.ADULT }
        val childCount = allParticipants.count { it.type == ParticipantType.CHILD }
        val totalWeight = adultCount + childCount * DEFINE_LATER_CHILD_WEIGHT
        if (totalWeight <= 0.0) return 0.0 to 0.0
        val unit = event.budget / totalWeight
        return unit to (unit * DEFINE_LATER_CHILD_WEIGHT)
    }

    private fun calculateFinancialSummary(
        event: EventEntity,
        participants: List<ParticipantEntity>,
        expenses: List<ExpenseEntity>
    ): FinancialSummary {
        val totalSpent = expenses.sumOf { it.amount }
        val totalPurchased = expenses.filter { it.isPurchased }.sumOf { it.amount }
        val budgetBalance = event.budget - totalSpent

        val adultCount = participants.count { it.type == ParticipantType.ADULT }
        val childCount = participants.count { it.type == ParticipantType.CHILD }
        val totalCount = participants.size

        val totalExpected = participants.sumOf { p ->
            calculateParticipantTarget(p, event, participants)
        }
        val totalCollected = participants.sumOf { it.paidAmount }
        val missing = maxOf(0.0, totalExpected - totalCollected)
        val netBalance = totalCollected - totalSpent

        return FinancialSummary(
            budget = event.budget,
            totalSpent = totalSpent,
            totalPurchased = totalPurchased,
            budgetBalance = budgetBalance,
            totalExpectedCollection = totalExpected,
            totalCollected = totalCollected,
            missingCollection = missing,
            netBalance = netBalance,
            adultCount = adultCount,
            childCount = childCount,
            totalParticipants = totalCount
        )
    }

    // Event Actions
    fun addEvent(event: EventEntity) = viewModelScope.launch {
        repository?.insertEvent(event)
    }

    fun updateEvent(event: EventEntity) = viewModelScope.launch {
        repository?.updateEvent(event)
    }

    fun selectActiveEvent(eventId: String) = viewModelScope.launch {
        repository?.selectActiveEvent(eventId)
    }

    fun deleteEvent(event: EventEntity) = viewModelScope.launch {
        repository?.deleteEvent(event)
    }

    // Participant Actions
    fun addParticipant(participant: ParticipantEntity) = viewModelScope.launch {
        repository?.insertParticipant(participant)
    }

    fun updateParticipant(participant: ParticipantEntity) = viewModelScope.launch {
        repository?.updateParticipant(participant)
    }

    fun updateParticipantPayment(participantId: String, paidAmount: Double) = viewModelScope.launch {
        repository?.updatePayment(participantId, paidAmount)
    }

    fun toggleParticipantConfirmed(participantId: String, confirmed: Boolean) = viewModelScope.launch {
        repository?.updateParticipantConfirmed(participantId, confirmed)
    }

    fun deleteParticipant(participant: ParticipantEntity) = viewModelScope.launch {
        repository?.deleteParticipant(participant)
    }

    // Expense Actions
    fun addExpense(expense: ExpenseEntity) = viewModelScope.launch {
        repository?.insertExpense(expense)
    }

    fun updateExpense(expense: ExpenseEntity) = viewModelScope.launch {
        repository?.updateExpense(expense)
    }

    fun toggleExpensePurchased(expenseId: String, isPurchased: Boolean) = viewModelScope.launch {
        repository?.updateExpensePurchased(expenseId, isPurchased)
    }

    fun toggleExpensePaid(expenseId: String, isPaid: Boolean) = viewModelScope.launch {
        repository?.updateExpensePaid(expenseId, isPaid)
    }

    fun deleteExpense(expense: ExpenseEntity) = viewModelScope.launch {
        repository?.deleteExpense(expense)
    }

    // Category Actions
    fun addCategory(category: CategoryEntity) = viewModelScope.launch {
        repository?.insertCategory(category)
    }

    fun updateCategory(category: CategoryEntity) = viewModelScope.launch {
        repository?.updateCategory(category)
    }

    fun deleteCategory(category: CategoryEntity) = viewModelScope.launch {
        repository?.deleteCategory(category)
    }
}
