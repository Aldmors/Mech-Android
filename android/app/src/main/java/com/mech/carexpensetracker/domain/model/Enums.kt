package com.mech.carexpensetracker.domain.model

enum class EventType(val raw: String) {
    Fuel("fuel"),
    Repair("repair"),
    Papers("papers"),
    Care("care"),
    ;

    companion object {
        fun fromRaw(raw: String?): EventType =
            entries.find { it.raw.equals(raw, ignoreCase = true) } ?: Repair
    }
}

enum class FuelType(val raw: String, val displayName: String) {
    Gasoline("gasoline", "Gasoline"),
    Diesel("diesel", "Diesel"),
    Lpg("lpg", "LPG"),
    Electric("electric", "Electric"),
    ;

    companion object {
        fun fromRaw(raw: String?): FuelType =
            entries.find { it.raw.equals(raw, ignoreCase = true) } ?: Gasoline
    }
}

enum class FuelSlot {
    Primary,
    Secondary,
}

enum class CarIconColor(val raw: String) {
    Blue("blue"),
    Red("red"),
    Green("green"),
    Orange("orange"),
    Purple("purple"),
    Gray("gray"),
    ;

    companion object {
        fun fromRaw(raw: String?): CarIconColor = entries.find { it.raw == raw } ?: Blue
    }
}

object CarIcon {
    const val DEFAULT = "DirectionsCar"

    val names = listOf(
        DEFAULT,
        "ElectricCar",
        "CarRental",
        "CarRepair",
        "LocalTaxi",
        "LocalShipping",
        "AirportShuttle",
        "TwoWheeler",
        "PedalBike",
        "Moped",
        "ElectricMoped",
        "DirectionsBus",
        "DirectionsBoat",
        "Agriculture",
        "RvHookup",
        "Commute",
        "Garage",
        "TimeToLeave",
        "LocalCarWash",
        "LocalGasStation",
        "EvStation",
        "Speed",
        "Traffic",
    )

    private val lookup = names.associateBy { it.lowercase() }

    fun resolve(name: String?): String = name?.lowercase()?.let { lookup[it] } ?: DEFAULT
}

enum class VehicleUnits(val raw: String) {
    Km("km"),
    Mi("mi"),
    ;

    companion object {
        fun fromRaw(raw: String?): VehicleUnits = entries.find { it.raw == raw } ?: Km
    }
}

enum class NotePriority(val raw: String) {
    Low("low"),
    Normal("normal"),
    High("high"),
    ;

    companion object {
        fun fromRaw(raw: String?): NotePriority = entries.find { it.raw == raw } ?: Normal
    }
}

enum class ChartDatePreset(val months: Int?) {
    ThreeMonths(3),
    SixMonths(6),
    TwelveMonths(12),
    AllTime(null),
}

enum class ChartKind {
    MonthlySpending,
    FuelConsumption,
    CategoryBreakdown,
    CumulativeCost,
}

enum class PlanningHorizon(val raw: String) {
    Short("short"),
    Medium("medium"),
    Long("long"),
    ;

    companion object {
        fun fromRaw(raw: String?): PlanningHorizon = entries.find { it.raw == raw } ?: Medium
    }
}
