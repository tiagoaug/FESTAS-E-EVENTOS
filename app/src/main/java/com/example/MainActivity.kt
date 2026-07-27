package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.PartyViewModel

enum class NavigationItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    DASHBOARD("dashboard", "Início", Icons.Default.Dashboard, Icons.Outlined.Dashboard),
    PARTICIPANTS("participants", "Convidados", Icons.Default.People, Icons.Outlined.People),
    EXPENSES("expenses", "Gastos", Icons.Default.ShoppingCart, Icons.Outlined.ShoppingCart),
    PAYMENTS("payments", "Receber", Icons.Default.Payments, Icons.Outlined.Payments),
    INVITATIONS("invitations", "Convites", Icons.Default.Chat, Icons.Outlined.Chat)
}

class MainActivity : ComponentActivity() {

    private val viewModel: PartyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val currentUser by viewModel.authState.collectAsStateWithLifecycle()

                if (currentUser == null) {
                    val webClientId = stringResource(R.string.web_client_id)
                    LoginScreen(onSignInClick = { viewModel.signInWithGoogle(webClientId) })
                } else {
                val navController = rememberNavController()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val showBottomBar = currentRoute in NavigationItem.entries.map { it.route }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar {
                                NavigationItem.entries.forEach { item ->
                                    val isSelected = currentRoute == item.route
                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick = {
                                            if (currentRoute != item.route) {
                                                navController.navigate(item.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = item.title
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = item.title,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = NavigationItem.DASHBOARD.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(NavigationItem.DASHBOARD.route) {
                            DashboardScreen(
                                uiState = uiState,
                                viewModel = viewModel,
                                onNavigateToSetup = { navController.navigate("event_setup") },
                                onNavigateToParticipants = { navController.navigate(NavigationItem.PARTICIPANTS.route) },
                                onNavigateToExpenses = { navController.navigate(NavigationItem.EXPENSES.route) },
                                onNavigateToPayments = { navController.navigate(NavigationItem.PAYMENTS.route) },
                                onNavigateToInvitations = { navController.navigate(NavigationItem.INVITATIONS.route) }
                            )
                        }

                        composable(NavigationItem.PARTICIPANTS.route) {
                            ParticipantsScreen(
                                uiState = uiState,
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable(NavigationItem.EXPENSES.route) {
                            ExpensesScreen(
                                uiState = uiState,
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable(NavigationItem.PAYMENTS.route) {
                            PaymentsScreen(
                                uiState = uiState,
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable(NavigationItem.INVITATIONS.route) {
                            InvitationsScreen(
                                uiState = uiState,
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("event_setup") {
                            EventSetupScreen(
                                uiState = uiState,
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
                }
            }
        }
    }
}
