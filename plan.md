# Car Expense Tracker — Android Agent Plan

Plan for an AI coding agent to build an **Android port** of the existing iOS app in this repo. Target **feature parity** with the SwiftUI/SwiftData implementation, reusing the same **Carnotes JSON** format and sample fixtures.

**Reference iOS sources:** `CarExpenseTracker/CarExpenseTracker/` (models, services, import, views).  
**Sample data:** `carnotes_zip_exported_1781131397635/` and `Resources/SampleData/`.

---

## 1. Goal

Ship a lightweight Android app where users can:

- Manage one or more cars (km/mi, fuel types, icon color)
- Record **fuel**, **repair/service**, and **documents/fees** events
- Attach **photos** to events
- View a **dashboard** (monthly spend, mileage, consumption, ownership stats)
- Browse/filter/search **events** and **notes**
- View **charts** (monthly spend, fuel consumption, category breakdown, cumulative cost) and **share/save as PNG**
- **Import** and **export** Carnotes JSON (merge by `_id`, optional replace)
- Manage **reminders** (time + mileage), with local notifications
- Plan future expenses with **savings targets**
- Use custom **expense categories** per car

**Out of scope for v1 Android** (match iOS gaps): iCloud sync, home-screen widgets, Siri shortcuts.

---

## 2. iOS → Android Stack Mapping

| iOS | Android | Notes |
|-----|---------|-------|
| SwiftUI | **Jetpack Compose** | Material 3 |
| SwiftData | **Room** + optional **DataStore** | `externalId` as stable PK for import |
| `@Query` | **Flow** from DAO + `collectAsStateWithLifecycle` | |
| `@AppStorage` | **DataStore Preferences** | `selected_car_external_id` |
| Swift Charts | **Vico** (Compose) or **MPAndroidChart** via `AndroidView` | Prefer Vico for Compose-native charts |
| `ImageRenderer` | **Compose `GraphicsLayer` / `drawToBitmap`** | Fixed-width export composable |
| `ShareLink` / `UIActivityViewController` | **`Intent.ACTION_SEND`** | Share PNG + JSON files |
| `UIDocumentPicker` | **`ActivityResultContracts.OpenDocument`** / `OpenMultipleDocuments` | MIME `application/json` |
| `UNUserNotificationCenter` | **NotificationManager** + **AlarmManager** or **WorkManager** | Exact alarms need `SCHEDULE_EXACT_ALARM` on API 31+ |
| EventKit | **CalendarContract** (calendar events) | No direct “Reminders” API; calendar-only sync is acceptable v1 |
| `UIImagePickerController` | **Activity Result** camera + `PickVisualMedia` | Coil for display |
| XCTest | **JUnit 5** + **Robolectric** (optional) + **Compose UI tests** (smoke only) | Prioritize pure Kotlin unit tests |

### Recommended versions

- **minSdk 26**, **targetSdk 35**
- **Kotlin 2.x**, **Compose BOM** (latest stable)
- **Room 2.6+**, **Hilt** or manual DI (agent: pick **Hilt** unless repo already has Koin)
- **kotlinx.serialization** for Carnotes JSON
- **Coil 3** for images

---

## 3. Repository Layout (new Gradle module)

Create a sibling project or `android/` folder at repo root:

```
android/
├── app/
│   └── src/
│       ├── main/
│       │   ├── java/.../carexpensetracker/
│       │   │   ├── CarExpenseTrackerApp.kt
│       │   │   ├── MainActivity.kt
│       │   │   ├── data/
│       │   │   │   ├── db/          # Room entities, DAOs, Database
│       │   │   │   ├── prefs/       # DataStore
│       │   │   │   └── repository/  # CarRepository, EventRepository, …
│       │   │   ├── domain/
│       │   │   │   ├── model/       # EventType, FuelType, FuelLeg, …
│       │   │   │   └── service/     # Pure logic (port from iOS Services/)
│       │   │   ├── import_/         # Carnotes DTOs, parser, exporter, coordinator
│       │   │   ├── ui/
│       │   │   │   ├── theme/       # DesignTokens equivalent
│       │   │   │   ├── components/  # AppCard, MetricCard, CarHeader, …
│       │   │   │   ├── navigation/  # NavHost, routes
│       │   │   │   ├── welcome/
│       │   │   │   ├── dashboard/
│       │   │   │   ├── events/
│       │   │   │   ├── charts/
│       │   │   │   ├── planning/
│       │   │   │   ├── settings/
│       │   │   │   ├── reminders/
│       │   │   │   ├── categories/
│       │   │   │   └── importexport/
│       │   │   └── util/            # CurrencyFormatter, ShareHelper, BitmapExport
│       │   ├── res/
│       │   └── assets/sampledata/   # Copy iOS SampleData JSON
│       └── test/                    # Unit tests mirroring iOS test names
│       └── androidTest/             # Minimal Compose smoke tests
├── build.gradle.kts
└── settings.gradle.kts
```

**Agent rule:** One feature per PR-sized phase; each phase ends with a **runnable app** and **at least one unit test** for new pure logic.

---

## 4. Data Model (Room)

Mirror iOS `@Model` types. Use `externalId: String` as business key; Room `@PrimaryKey(autoGenerate = true) val id: Long` optional.

### Entities & relationships

```
Car (1) ──< CarEvent (cascade delete)
Car (1) ──< CarReminder
Car (1) ──< CarNote
Car (1) ──< ExpenseCategory
Car (1) ──< PlannedExpense
CarEvent (1) ──< EventPhoto (image BLOB or file path; prefer file in app files dir + path in DB)
```

### Field parity checklist

| Entity | Must-have fields (match iOS) |
|--------|------------------------------|
| `Car` | `externalId`, `name`, `plateNumber`, `vehicleUnits`, `buyDate`, `iconColorName`, `primaryFuelTypeRaw`, `alternativeFuelTypeRaw` |
| `CarEvent` | All fuel primary + **secondary fuel leg** fields, `typeRaw`, costs, `comment`, mileage |
| `CarReminder` | date + mileage due fields, `isCompleted`, `syncedItemIdentifier` (calendar event id) |
| `CarNote` | `title`, `details`, `priorityRaw`, `isResolved`, `createdAt`, `resolvedAt` |
| `PlannedExpense` | `name`, `cost`, `location`, `months`, `targetDate`, `horizonRaw`, `createdAt` |
| `ExpenseCategory` | `name`, `createdAt` |
| `EventPhoto` | `imagePath` or `imageBytes`, `createdAt` |

### Enums (domain package)

Port from iOS: `EventType` (fuel, repair, papers), `FuelType`, `CarIconColor`, `NotePriority`, `ChartDatePreset`, `ChartKind`.

### `FuelLeg` value type

Port `CarEvent.fuelLegs` logic from iOS — required for dual-fuel charts and event rows.

---

## 5. Carnotes Import/Export (critical path)

**Must be byte-compatible with iOS** for the tables this app uses.

### Tables

| File | Entity |
|------|--------|
| `garage_table.json` | `Car` |
| `car_events_table.json` | `CarEvent` |
| `car_reminders_table.json` | `CarReminder` |
| `notes_table.json` | `CarNote` |

### Port these iOS files 1:1 (logic, not syntax)

- `Import/CarnotesDTOs.swift` → `CarnotesDtos.kt`
- `Import/CarnotesValueParsers.swift` → `CarnotesValueParsers.kt`
- `Import/CarnotesParser.swift` → `CarnotesParser.kt`
- `Import/CarnotesExporter.swift` → `CarnotesExporter.kt`
- `Import/ImportCoordinator.swift` → `ImportCoordinator.kt`

### Normalization rules (do not change)

- String numbers: `"5.71"` → `BigDecimal`
- Sentinel `"-1"` → `null` for mileage/costs
- Dates: Unix **milliseconds** as string → `Instant` / `LocalDateTime`
- `fuel_full_tank`: `"1"` / `"0"` → boolean
- Upsert by `_id` / `externalId`; support **merge** (default) and **replace per car**

### Tests (copy iOS fixtures)

Use `carnotes_zip_exported_1781131397635/` in `app/src/test/resources/`:

- Expect ≥1 car ("DS", km)
- Expect ≥50 events
- Spot-check event `_id: "21"` if present in fixture

**Acceptance:** Android import of sample folder produces same car/event counts as iOS unit tests.

---

## 6. Services to Port (pure Kotlin)

Copy behavior from iOS `Services/` — no Android APIs inside these files.

| Service | iOS source | Android test file |
|---------|------------|-------------------|
| `ConsumptionCalculator` | `ConsumptionCalculator.swift` | `ConsumptionCalculatorTest.kt` |
| `MileageValidationService` | `MileageValidationService.swift` | `MileageValidationServiceTest.kt` |
| `EventSummaryService` | `EventSummaryService.swift` | `EventSummaryServiceTest.kt` |
| `ChartDataService` | `ChartDataService.swift` | `ChartDataServiceTest.kt` |
| `OwnershipAnalyticsService` | `OwnershipAnalyticsService.swift` | `OwnershipAnalyticsServiceTest.kt` |
| `PlanningSavingsService` | `PlanningSavingsService.swift` | `PlanningSavingsServiceTest.kt` |
| `RecordCostService` | `RecordCostService.swift` | (inline in form VM tests) |
| `ExpenseCategoryService` | `ExpenseCategoryService.swift` | optional |
| `NoteSortingService` | `NoteSortingService.swift` | `NoteSortingServiceTest.kt` |
| `ObligatoryReminderService` | `ObligatoryReminderService.swift` | `ObligatoryReminderServiceTest.kt` |
| `ReminderAlertService` | `ReminderAlertService.swift` | `ReminderAlertServiceTest.kt` |
| `CurrencyFormatter` | `CurrencyFormatter.swift` | `CurrencyFormatterTest.kt` |

`ChartImageExporter` → `ChartBitmapExporter.kt` (uses Compose; keep bitmap logic thin).

---

## 7. UI / Navigation

### Root flow (match `ContentView.swift`)

```
if (cars.isEmpty) WelcomeScreen
else MainScaffold with BottomNavigation:
  - Dashboard
  - Events
  - Charts
  - Planning
  - More (Settings)
```

Persist selected car in DataStore (`selected_car_external_id`).

### Screens

| Screen | iOS reference | Key behaviors |
|--------|---------------|---------------|
| Welcome | `WelcomeView` | Import JSON, Add car |
| Dashboard | `DashboardView` | Metric cards, ownership section, reminder advert, recent 5 events |
| Events | `EventListView` | Filter chips: All / Fuel / Service / Documents / **Notes**; search; sort sheet |
| Add fuel | `AddFuelView` | Dual fuel, consumption preview, photos, mileage validation |
| Add expense | `AddExpenseView` | Category picker, parts/labour, photos |
| Charts | `ChartsTabView` | 4 chart kinds, date preset, share PNG |
| Planning | `PlanningView` | Planned expenses list, savings summary cards |
| More | `SettingsView` | Cars, Reminders, Categories, Import, Export, version |
| Reminders | `RemindersView` | CRUD, complete, sync to calendar |
| Categories | `ExpenseCategoriesView` | Per-car categories |
| Import | `ImportView` | Multi-file picker, preview counts, car detail overrides |
| Car form | `CarFormView` | Add/edit/delete car |

### Design system (match `DesignSystem/`)

- `DesignTokens`: colors, spacing, typography
- Components: `AppScreen`, `AppCard`, `MetricCard`, `PrimaryButton`, `SecondaryButton`, `SectionHeader`, `CarHeader`, `EventTypeBadge`, `EmptyStateCard`, `PhotoAttachmentsSection`

Use **Material 3** with a custom accent color aligned with iOS `DesignTokens.Palette.accent`.

---

## 8. Charts

### Chart kinds (parity with `ChartKind`)

1. **Monthly spending** — stacked bars by fuel / repair / papers
2. **Fuel consumption** — line + points; respect km vs mi
3. **Category breakdown** — donut or horizontal bars
4. **Cumulative cost** — ownership line chart

### Date presets

Port `ChartDatePreset`: 3 / 6 / 12 months, all time (and custom only if iOS has it).

### PNG export

1. Dedicated **export Composable** (fixed width ~600dp, **light background** always)
2. Title, subtitle (date range), chart, legend, 1–2 summary stats, car name footer
3. `ChartBitmapExporter.capture(composable)` → PNG `ByteArray`
4. Share via `FileProvider` + `ACTION_SEND`
5. Optional: save to Pictures via MediaStore

**Empty state:** disable share when no data in range.

---

## 9. Reminders & Notifications

### Local notifications

Port `ReminderNotificationService`:

- Request `POST_NOTIFICATIONS` (API 33+)
- Schedule date-based reminders
- On fuel/expense save with mileage, run `ReminderAlertService` for mileage thresholds

### Calendar sync (EventKit substitute)

Port `ReminderSyncService` **calendar path only**:

- `READ_CALENDAR` / `WRITE_CALENDAR`
- Create/update `CalendarContract.Events` with `syncedItemIdentifier` stored on `CarReminder`
- Skip “Apple Reminders” target — document as Android limitation

### Obligatory reminders

On app start / car count change, seed templates via `ObligatoryReminderService` (insurance, inspection, etc.) — same templates as iOS.

---

## 10. Permissions (`AndroidManifest.xml`)

| Permission | Use |
|------------|-----|
| `POST_NOTIFICATIONS` | Reminder alerts |
| `READ_CALENDAR` / `WRITE_CALENDAR` | Optional calendar sync |
| `CAMERA` | Event photos |
| `READ_MEDIA_IMAGES` (API 33+) or `READ_EXTERNAL_STORAGE` (legacy) | Photo picker |
| `SCHEDULE_EXACT_ALARM` | Exact reminder times (if used) |

Use **Photo Picker** API where possible to avoid broad storage permission.

---

## 11. Implementation Phases (agent execution order)

Each phase: **implement → unit test → manual smoke → commit**.

### Phase 0 — Project bootstrap
- [ ] Create Gradle project, Compose, Room, Hilt, serialization
- [ ] `MainActivity`, empty theme, app icon placeholder
- [ ] Copy sample JSON to `assets/` and `test/resources/`
- **Done when:** app launches to blank screen

### Phase 1 — Database & car CRUD
- [ ] Room entities: `Car` (+ DAO)
- [ ] `CarRepository`, `CarFormScreen`, DataStore selected car
- [ ] `WelcomeScreen` + empty garage routing
- **Done when:** add/edit/delete car persists across restart

### Phase 2 — Events CRUD
- [ ] `CarEvent` entity + DAO, cascade from car
- [ ] `EventListScreen`, `AddFuelScreen`, `AddExpenseScreen`
- [ ] Port `ConsumptionCalculator`, `MileageValidationService`, `RecordCostService`, `CurrencyFormatter` + tests
- **Done when:** fuel + expense entries show in list

### Phase 3 — Design system & main navigation
- [ ] Bottom nav: Dashboard, Events, Charts (stub), Planning (stub), More
- [ ] `CarHeader`, cards, buttons, tokens
- [ ] Event filters, search, sort
- **Done when:** navigation matches iOS tab structure

### Phase 4 — Dashboard
- [ ] Port `EventSummaryService`, `OwnershipAnalyticsService` + tests
- [ ] `DashboardScreen` metric grid, recent events, quick add
- **Done when:** monthly total and mileage match manual calculation

### Phase 5 — Carnotes import
- [ ] DTOs, parsers, parser tests (iOS fixture)
- [ ] `ImportCoordinator` preview + merge
- [ ] `ImportScreen` + document picker
- **Done when:** sample export imports; event count matches iOS test expectations

### Phase 6 — Charts
- [ ] Port `ChartDataService` + tests
- [ ] Four chart screens/composables
- [ ] `ChartsScreen` with kind + preset selectors
- **Done when:** charts render with imported data

### Phase 7 — Chart PNG export
- [ ] Export composable + `ChartBitmapExporter`
- [ ] Share intent
- **Done when:** PNG shares via Gmail/Files

### Phase 8 — Extended models
- [ ] `CarReminder`, `CarNote`, `ExpenseCategory`, `PlannedExpense`, `EventPhoto`
- [ ] Import/export reminders + notes tables
- [ ] Dual-fuel fields + `FuelLeg`
- [ ] Photos on add/edit event (Coil, file storage)
- **Done when:** full sample import including reminders; dual-fuel event displays two legs

### Phase 9 — Reminders & notifications
- [ ] `RemindersScreen`, notification scheduler, obligatory seed
- [ ] `ReminderAlertService` on mileage update
- [ ] Calendar sync (optional toggle in settings)
- **Done when:** date reminder fires notification; mileage reminder fires after fuel entry

### Phase 10 — Planning, notes, categories
- [ ] `PlanningScreen` + `PlanningSavingsService` test
- [ ] Notes mode in Events list + note form
- [ ] `CategoriesScreen`
- **Done when:** planning savings math matches iOS test vectors

### Phase 11 — Export & polish
- [ ] `CarnotesExporter` + share JSON files
- [ ] More screen: import/export, version, car list
- [ ] String resources (`strings.xml`), empty states, error snackbars
- [ ] Dashboard reminder advert cards
- **Done when:** export → re-import merge yields no duplicates

### Phase 12 — Verification
- [ ] Full unit test suite green
- [ ] One Compose smoke test: welcome → import sample → dashboard visible
- [ ] Parity checklist (section 12) signed off

---

## 12. Feature Parity Checklist

Use this as the agent’s final gate:

- [ ] Multi-car garage with persisted selection
- [ ] Fuel / repair / papers events with edit & delete
- [ ] Dual-fuel single stop (primary + secondary leg)
- [ ] Photo attachments on events
- [ ] Dashboard: month spend, mileage, consumption, ownership, recent events
- [ ] Events: filter, search, sort; Notes filter with priorities
- [ ] Charts: 4 kinds, date presets, empty states
- [ ] Chart PNG share (light background export view)
- [ ] Carnotes import: merge + replace, preview, car detail overrides
- [ ] Carnotes export: all four tables
- [ ] Reminders: CRUD, complete, notifications, obligatory templates
- [ ] Calendar sync OR documented deferral with in-app-only reminders
- [ ] Planning: planned expenses + savings summary
- [ ] Custom expense categories per car
- [ ] Unit tests for all ported services + import/export

---

## 13. Agent Operating Rules

1. **Read iOS file before porting** — behavior must match, not reinvent.
2. **Smallest diff** — no extra libraries beyond stack in §2 without justification.
3. **Business logic in `domain/service/`** — not in Composables.
4. **One runnable checkpoint per phase** — never leave the app broken between commits.
5. **Test pure logic** — every ported `*Service` gets a JUnit test copied from iOS expectations.
6. **Import is the contract** — Carnotes compatibility beats cosmetic UI differences.
7. **Mark Android-only gaps** with `// android-port:` comment (e.g. no Reminders app sync).
8. **Do not block on pixel-perfect UI** — match information architecture and data, then polish.

---

## 14. Open Decisions (agent defaults)

| Question | Default for agent |
|----------|-------------------|
| DI framework | Hilt |
| Chart library | Vico (Compose) |
| Image storage | App-private files + path in Room |
| Calendar vs Reminders sync | Calendar only |
| minSdk | 26 |
| Currency | `NumberFormat.getCurrencyInstance()` with device locale; no multi-currency editor v1 |
| App name | "Car Expense Tracker" |

---

## 15. Success Criteria

The Android app is **done** when:

1. A user can import `carnotes_zip_exported_1781131397635/` and see the same cars/events as on iOS.
2. Dashboard and chart totals align with iOS for the same imported data (± rounding).
3. Export produces JSON files importable by **both** Android and iOS apps without data loss on `_id`.
4. All service-layer unit tests pass; import parser tests match iOS fixture assertions.

---

## Summary

Build **Kotlin + Compose + Room** app mirroring iOS layer boundaries. Port **Carnotes import/export** and **Services/** first-class with shared test fixtures. UI follows the same five-tab shell and screen set. Defer iCloud/widgets; substitute EventKit with **CalendarContract** for reminder sync. Execute in **12 phases**, each leaving a runnable build and tested pure logic.
