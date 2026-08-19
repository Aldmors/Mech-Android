package com.mech.carexpensetracker.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mech.carexpensetracker.R
import com.mech.carexpensetracker.ui.charts.ChartsScreen
import com.mech.carexpensetracker.ui.dashboard.DashboardScreen
import com.mech.carexpensetracker.ui.events.EventsScreen
import com.mech.carexpensetracker.ui.importexport.ImportScreen
import com.mech.carexpensetracker.ui.planning.PlanningScreen
import com.mech.carexpensetracker.ui.settings.AddExpenseScreen
import com.mech.carexpensetracker.ui.settings.AddFuelScreen
import com.mech.carexpensetracker.ui.settings.CarFormScreen
import com.mech.carexpensetracker.ui.settings.MoreScreen
import com.mech.carexpensetracker.ui.welcome.WelcomeScreen

data class MainTab(val route: String, val labelRes: Int, val icon: ImageVector)

@Composable
fun AppNavHost(
    hasCars: Boolean,
    selectedCarId: String?,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()

    if (!hasCars) {
        NavHost(navController = navController, startDestination = Routes.Welcome, modifier = modifier) {
            composable(Routes.Welcome) {
                WelcomeScreen(
                    onAddCar = { navController.navigate(Routes.AddCar) },
                    onImport = { navController.navigate(Routes.Import) },
                )
            }
            composable(Routes.AddCar) {
                CarFormScreen(carId = null, onDone = { navController.popBackStack() })
            }
            composable(Routes.Import) {
                ImportScreen(onDone = { navController.popBackStack() })
            }
        }
        return
    }

    val tabs = listOf(
        MainTab(Routes.Dashboard, R.string.dashboard, Icons.Default.Dashboard),
        MainTab(Routes.Events, R.string.events, Icons.Default.List),
        MainTab(Routes.Charts, R.string.charts, Icons.Default.BarChart),
        MainTab(Routes.Planning, R.string.planning, Icons.Default.CalendarMonth),
        MainTab(Routes.More, R.string.more, Icons.Default.MoreHoriz),
    )

    Scaffold(
        modifier = modifier,
        bottomBar = {
            val backStack by navController.currentBackStackEntryAsState()
            val currentRoute = backStack?.destination?.route
            if (currentRoute in tabs.map { it.route }) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.Dashboard,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.Dashboard) {
                DashboardScreen(
                    onAddFuel = { selectedCarId?.let { navController.navigate(Routes.AddFuel) } },
                    onAddExpense = { selectedCarId?.let { navController.navigate(Routes.AddExpense) } },
                )
            }
            composable(Routes.Events) { EventsScreen() }
            composable(Routes.Charts) { ChartsScreen() }
            composable(Routes.Planning) { PlanningScreen() }
            composable(Routes.More) {
                MoreScreen(
                    onCars = { navController.navigate(Routes.AddCar) },
                    onImport = { navController.navigate(Routes.Import) },
                    onReminders = { navController.navigate(Routes.Reminders) },
                    onCategories = { navController.navigate(Routes.Categories) },
                )
            }
            composable(Routes.AddCar) {
                CarFormScreen(carId = null, onDone = { navController.popBackStack() })
            }
            composable(Routes.Import) {
                ImportScreen(onDone = { navController.popBackStack() })
            }
            composable(Routes.AddFuel) {
                selectedCarId?.let { id ->
                    AddFuelScreen(carExternalId = id, onDone = { navController.popBackStack() })
                }
            }
            composable(Routes.AddExpense) {
                selectedCarId?.let { id ->
                    AddExpenseScreen(carExternalId = id, onDone = { navController.popBackStack() })
                }
            }
            composable(Routes.Reminders) {
                Text("Reminders") // android-port: full RemindersScreen in follow-up
            }
            composable(Routes.Categories) {
                Text("Categories") // android-port: full CategoriesScreen in follow-up
            }
        }
    }
}
