package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
            val appTheme by viewModel.appTheme.collectAsStateWithLifecycle()
            val appFont by viewModel.appFont.collectAsStateWithLifecycle()
            MyApplicationTheme(themeId = appTheme, fontId = appFont) {
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
                            GlassBottomNav(
                                currentRoute = currentRoute,
                                onNavigate = { route ->
                                    if (currentRoute != route) {
                                        navController.navigate(route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            )
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
                                onNavigateToInvitations = { navController.navigate(NavigationItem.INVITATIONS.route) },
                                onNavigateToSettings = { navController.navigate("settings") }
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
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToSettings = { navController.navigate("settings") }
                            )
                        }

                        composable("event_setup") {
                            EventSetupScreen(
                                uiState = uiState,
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("settings") {
                            SettingsScreen(
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

/**
 * Barra de navegação flutuante em "pill", com fundo translúcido e o item ativo
 * destacado em degradê, mirando o estilo glass usado na versão web.
 */
@Composable
private fun GlassBottomNav(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 14.dp, shape = RoundedCornerShape(28.dp), clip = false)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                .padding(6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavigationItem.entries.forEach { item ->
                val isSelected = currentRoute == item.route
                val backgroundModifier = if (isSelected) {
                    Modifier.background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primaryContainer
                            )
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                } else {
                    Modifier
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .then(backgroundModifier)
                        .clickable { onNavigate(item.route) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.title,
                        tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
