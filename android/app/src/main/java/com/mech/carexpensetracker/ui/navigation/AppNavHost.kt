package com.mech.carexpensetracker.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mech.carexpensetracker.R
import com.mech.carexpensetracker.domain.model.EventType
import com.mech.carexpensetracker.ui.charts.ChartsScreen
import com.mech.carexpensetracker.ui.components.AppIcons
import com.mech.carexpensetracker.ui.dashboard.DashboardScreen
import com.mech.carexpensetracker.ui.events.EventsScreen
import com.mech.carexpensetracker.ui.importexport.ImportScreen
import com.mech.carexpensetracker.ui.planning.PlanningScreen
import com.mech.carexpensetracker.ui.reminders.AddReminderScreen
import com.mech.carexpensetracker.ui.reminders.RemindersScreen
import com.mech.carexpensetracker.ui.settings.AddExpenseScreen
import com.mech.carexpensetracker.ui.settings.AddFuelScreen
import com.mech.carexpensetracker.ui.settings.CarFormScreen
import com.mech.carexpensetracker.ui.settings.MoreScreen
import com.mech.carexpensetracker.ui.welcome.WelcomeScreen
import kotlinx.coroutines.launch

data class MainTab(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost(
    hasCars: Boolean,
    selectedCarId: String?,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val showMessage: (String) -> Unit = { message ->
        scope.launch { snackbarHostState.showSnackbar(message) }
    }
    var importOpen by remember { mutableStateOf(false) }
    val showWelcome = !hasCars || importOpen

    if (showWelcome) {
        val navController = rememberNavController()
        Scaffold(
            modifier = modifier,
            contentWindowInsets = ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.ime),
            topBar = { AppTopBar(navController = navController, tabRoutes = emptySet()) },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Routes.Welcome,
                modifier = Modifier
                    .padding(padding)
                    .imePadding(),
            ) {
                composable(Routes.Welcome) {
                    WelcomeScreen(
                        onAddCar = { navController.navigate(Routes.AddCar) },
                        onImport = {
                            importOpen = true
                            navController.navigate(Routes.Import)
                        },
                    )
                }
                composable(Routes.AddCar) {
                    CarFormScreen(carId = null, onDone = { navController.popBackStack() })
                }
                composable(Routes.Import) {
                    ImportScreen(
                        onDone = {
                            importOpen = false
                            if (!hasCars) navController.popBackStack()
                        },
                        onMessage = showMessage,
                    )
                }
            }
        }
        return
    }

    val navController = rememberNavController()

    val tabs = listOf(
        MainTab(Routes.Dashboard, R.string.dashboard, Icons.Outlined.Dashboard, Icons.Default.Dashboard),
        MainTab(Routes.Events, R.string.events, AppIcons.eventsTab, AppIcons.eventsTabSelected),
        MainTab(Routes.Charts, R.string.charts, Icons.Outlined.BarChart, Icons.Default.BarChart),
        // ponytail: Planning tab hidden until the feature is ready; composable stays at Routes.Planning
        MainTab(Routes.More, R.string.more, Icons.Outlined.MoreHoriz, Icons.Default.MoreHoriz),
    )
    val tabRoutes = tabs.map { it.route }.toSet()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showEventFab = selectedCarId != null && currentRoute in setOf(Routes.Dashboard, Routes.Events)
    val showReminderFab = selectedCarId != null && currentRoute == Routes.Reminders

    BoxWithConstraints(modifier = modifier) {
        val useNavigationRail = maxWidth >= 600.dp
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.ime),
            topBar = { AppTopBar(navController = navController, tabRoutes = tabRoutes) },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                if (showEventFab) {
                    AddEventFab(
                        onAddFuel = { navController.navigate(Routes.AddFuel) },
                        onAddExpense = { navController.navigate(Routes.AddExpense) },
                    )
                } else if (showReminderFab) {
                    FloatingActionButton(onClick = { navController.navigate(Routes.AddReminder) }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.add_reminder),
                        )
                    }
                }
            },
            bottomBar = {
                if (!useNavigationRail && currentRoute in tabRoutes) {
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
                                icon = {
                                    Icon(
                                        imageVector = if (currentRoute == tab.route) tab.selectedIcon else tab.icon,
                                        contentDescription = stringResource(tab.labelRes),
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                            )
                        }
                    }
                }
            },
        ) { padding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding(),
            ) {
                if (useNavigationRail && currentRoute in tabRoutes) {
                    NavigationRail {
                        tabs.forEach { tab ->
                            NavigationRailItem(
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
                                icon = {
                                    Icon(
                                        imageVector = if (currentRoute == tab.route) tab.selectedIcon else tab.icon,
                                        contentDescription = stringResource(tab.labelRes),
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                            )
                        }
                    }
                }
                AppNavigationGraph(
                    navController = navController,
                    selectedCarId = selectedCarId,
                    showMessage = showMessage,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun AppNavigationGraph(
    navController: NavHostController,
    selectedCarId: String?,
    showMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Routes.Dashboard,
        modifier = modifier,
    ) {
            composable(Routes.Dashboard) {
                DashboardScreen(
                    onOpenEvents = {
                        navController.navigate(Routes.Events) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onReminderClick = { navController.navigate(Routes.editReminder(it)) },
                )
            }
            composable(Routes.Events) {
                EventsScreen(
                    onEventClick = { event ->
                        when (EventType.fromRaw(event.typeRaw)) {
                            EventType.Fuel -> navController.navigate(Routes.editFuel(event.externalId))
                            EventType.Repair, EventType.Papers, EventType.Care -> {
                                navController.navigate(Routes.editExpense(event.externalId))
                            }
                        }
                    },
                )
            }
            composable(Routes.Charts) { ChartsScreen(onMessage = showMessage) }
            composable(Routes.Planning) { PlanningScreen() }
            composable(Routes.More) {
                MoreScreen(
                    onCars = { navController.navigate(Routes.AddCar) },
                    onImport = { navController.navigate(Routes.Import) },
                    onReminders = { navController.navigate(Routes.Reminders) },
                    onMessage = showMessage,
                )
            }
            composable(Routes.AddCar) {
                CarFormScreen(
                    carId = null,
                    onDone = { navController.popBackStack() },
                    onEditCar = { id -> navController.navigate(Routes.editCar(id)) },
                )
            }
            composable(
                route = Routes.EditCar,
                arguments = listOf(navArgument("carId") { type = NavType.StringType }),
            ) { entry ->
                CarFormScreen(
                    carId = entry.arguments?.getString("carId"),
                    onDone = { navController.popBackStack() },
                    onEditCar = { id ->
                        navController.navigate(Routes.editCar(id)) { launchSingleTop = true }
                    },
                )
            }
            composable(Routes.Import) {
                ImportScreen(
                    onDone = { navController.popBackStack() },
                    onMessage = showMessage,
                )
            }
            composable(Routes.AddFuel) {
                selectedCarId?.let { id ->
                    AddFuelScreen(carExternalId = id, onDone = { navController.popBackStack() })
                }
            }
            composable(
                route = Routes.EditFuel,
                arguments = listOf(navArgument("eventId") { type = NavType.StringType }),
            ) { entry ->
                selectedCarId?.let { id ->
                    AddFuelScreen(
                        carExternalId = id,
                        eventId = entry.arguments?.getString("eventId"),
                        onDone = { navController.popBackStack() },
                    )
                }
            }
            composable(Routes.AddExpense) {
                selectedCarId?.let { id ->
                    AddExpenseScreen(carExternalId = id, onDone = { navController.popBackStack() })
                }
            }
            composable(
                route = Routes.EditExpense,
                arguments = listOf(navArgument("eventId") { type = NavType.StringType }),
            ) { entry ->
                selectedCarId?.let { id ->
                    AddExpenseScreen(
                        carExternalId = id,
                        eventId = entry.arguments?.getString("eventId"),
                        onDone = { navController.popBackStack() },
                    )
                }
            }
            composable(Routes.Reminders) {
                RemindersScreen(
                    onMessage = showMessage,
                    onReminderClick = { navController.navigate(Routes.editReminder(it)) },
                )
            }
            composable(Routes.AddReminder) {
                AddReminderScreen(onDone = { navController.popBackStack() })
            }
            composable(
                route = Routes.EditReminder,
                arguments = listOf(navArgument("reminderId") { type = NavType.StringType }),
            ) { entry ->
                AddReminderScreen(
                    reminderId = entry.arguments?.getString("reminderId"),
                    onDone = { navController.popBackStack() },
                )
            }
        }
}

@Composable
private fun AddEventFab(
    onAddFuel: () -> Unit,
    onAddExpense: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        FloatingActionButton(onClick = { expanded = true }) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_event))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.add_fuel)) },
                leadingIcon = { Icon(Icons.Default.LocalGasStation, contentDescription = null) },
                onClick = {
                    expanded = false
                    onAddFuel()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.add_expense)) },
                leadingIcon = { Icon(Icons.Default.Build, contentDescription = null) },
                onClick = {
                    expanded = false
                    onAddExpense()
                },
            )
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
    Routes.EditCar -> R.string.edit_car
    Routes.Import -> R.string.import_export
    Routes.AddFuel -> R.string.add_fuel
    Routes.AddExpense -> R.string.add_expense
    Routes.EditFuel -> R.string.edit_fuel
    Routes.EditExpense -> R.string.edit_expense
    Routes.Reminders -> R.string.reminders
    Routes.AddReminder -> R.string.add_reminder
    Routes.EditReminder -> R.string.edit_reminder
    else -> null
}
