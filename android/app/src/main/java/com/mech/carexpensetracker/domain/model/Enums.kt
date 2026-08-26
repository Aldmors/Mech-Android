package com.mech.carexpensetracker.domain.model

enum class EventType(val raw: String) {
    Fuel("fuel"),
    Repair("repair"),
    Papers("papers"),
    ;

    companion object {
        fun fromRaw(raw: String?): EventType = entries.find { it.raw == raw } ?: Repair
    }
}

enum class FuelType(val raw: String, val displayName: String) {
    Gasoline("gasoline", "Gasoline"),
    Diesel("diesel", "Diesel"),
    Lpg("lpg", "LPG"),
    Electric("electric", "Electric"),
    ;

    companion object {
        fun fromRaw(raw: String?): FuelType = entries.find { it.raw == raw } ?: Gasoline
    }
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

enum class ChartDatePreset(val label: String, val months: Int?) {
    ThreeMonths("3 months", 3),
    SixMonths("6 months", 6),
    TwelveMonths("12 months", 12),
    AllTime("All time", null),
}

enum class ChartKind(val label: String) {
    MonthlySpending("Monthly spending"),
    FuelConsumption("Fuel consumption"),
    CategoryBreakdown("Category breakdown"),
    CumulativeCost("Cumulative cost"),
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
