package com.mech.carexpensetracker.ui.navigation

object Routes {
    const val Welcome = "welcome"
    const val Main = "main"
    const val Dashboard = "dashboard"
    const val Events = "events"
    const val Charts = "charts"
    const val Planning = "planning"
    const val More = "more"
    const val AddCar = "add_car"
    const val EditCar = "edit_car/{carId}"
    const val AddFuel = "add_fuel"
    const val AddExpense = "add_expense"
    const val Import = "import"
    const val Reminders = "reminders"
    const val Categories = "categories"

    fun editCar(carId: String) = "edit_car/$carId"
}
