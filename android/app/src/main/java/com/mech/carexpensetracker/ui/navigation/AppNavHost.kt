package com.mech.carexpensetracker.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mech.carexpensetracker.R
import com.mech.carexpensetracker.ui.charts.ChartsScreen
import com.mech.carexpensetracker.ui.components.AppIcons
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost(
    hasCars: Boolean,
    selectedCarId: String?,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()

    if (!hasCars) {
        Scaffold(
            modifier = modifier,
            topBar = { AppTopBar(navController = navController, tabRoutes = emptySet()) },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Routes.Welcome,
                modifier = Modifier.padding(padding),
            ) {
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
        }
        return
    }

    val tabs = listOf(
        MainTab(Routes.Dashboard, R.string.dashboard, Icons.Default.Dashboard),
        MainTab(Routes.Events, R.string.events, AppIcons.eventsTab),
        MainTab(Routes.Charts, R.string.charts, Icons.Default.BarChart),
        // ponytail: Planning tab hidden until the feature is ready; composable stays at Routes.Planning
        MainTab(Routes.More, R.string.more, Icons.Default.MoreHoriz),
    )
    val tabRoutes = tabs.map { it.route }.toSet()

    Scaffold(
        modifier = modifier,
        topBar = { AppTopBar(navController = navController, tabRoutes = tabRoutes) },
        bottomBar = {
            val backStack by navController.currentBackStackEntryAsState()
            val currentRoute = backStack?.destination?.route
            if (currentRoute in tabRoutes) {
                NavigationBar(windowInsets = NavigationBarDefaults.windowInsets) {
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
                            icon = { Icon(tab.icon, contentDescription = stringResource(tab.labelRes)) },
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
                    onOpenEvents = {
                        navController.navigate(Routes.Events) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(
    navController: NavHostController,
    tabRoutes: Set<String>,
) {
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val titleRes = titleResFor(route) ?: return
    val showBack = route != null && route !in tabRoutes && route != Routes.Welcome
    TopAppBar(
        title = { Text(stringResource(titleRes)) },
        navigationIcon = {
            if (showBack) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.navigate_up),
                    )
                }
            }
        },
    )
}

private fun titleResFor(route: String?): Int? = when (route) {
    Routes.Welcome -> R.string.welcome_title
    Routes.Dashboard -> R.string.dashboard
    Routes.Events -> R.string.events
    Routes.Charts -> R.string.charts
    Routes.Planning -> R.string.planning
    Routes.More -> R.string.more
    Routes.AddCar -> R.string.add_car
    Routes.Import -> R.string.import_label
    Routes.AddFuel -> R.string.add_fuel
    Routes.AddExpense -> R.string.add_expense
    Routes.Reminders -> R.string.reminders
    Routes.Categories -> R.string.categories
    else -> null
}
