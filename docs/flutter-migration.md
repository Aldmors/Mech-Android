# Plan: migrate Car Expenses to Flutter

This plan treats “flatter” as **Flutter**: a new Dart app beside the existing Kotlin app, with the same Carnotes behavior. The Android app stays the shipping client until the Flutter app passes the same fixtures.

Data moves through a Carnotes ZIP, not by opening the Room database. Room is at schema version 5 with migrations in `AppDatabase.kt`. The ZIP export in `CarnotesExporter` is already the interchange format.

## What has to survive

| Area | Current code | Behavior to keep |
| --- | --- | --- |
| Cars | `CarEntity` | Name, plate, km/mi, buy date, icon, primary and optional second fuel |
| Events | `CarEventEntity` | Fuel, repair, papers, care. Dual fuel slot. Mileage, costs, photos |
| Money | `CurrencyFormatter`, `RecordCostService` | PLN display, comma/dot parsing, stored amounts as decimal strings (including `-1` as empty) |
| Consumption | `ConsumptionCalculator` | Full-tank-to-full-tank, partial fills included, L/100 km and mpg |
| Mileage | `MileageValidationService` | Reject a reading below the previous event |
| Charts | `ChartDataService` | Monthly spend, fuel consumption, category breakdown, cumulative cost. Presets: 3, 6, 12 months, all time |
| Planning | `PlannedExpenseEntity`, savings summary in `Services.kt` | Named future costs, horizon, savings target |
| Reminders | `CarReminderEntity`, reminder math in `Services.kt` | Due by date and/or mileage, intervals, obligatory seeds, repeat prompt |
| Notes and categories | `CarNoteEntity`, `ExpenseCategoryEntity` | Imported and exported with the ZIP. Notes also appear in the events filter |
| Photos | `EventPhotoStore` | Files on disk, paths in `event_photos`, included in the ZIP |
| Import / export | `CarnotesParser`, `CarnotesExporter`, `ImportCoordinator` | ZIP or loose JSON: garage, events, reminders, notes, categories, photos |
| Selected car | `SelectedCarStore` | Last selected car id survives restart |
| UI shell | `Routes`, `AppNavHost` | Welcome when there is no car. Tabs: dashboard, events, charts, planning, more. Wide layout uses a rail |
| Copy | `strings.xml` | Polish UI strings. Currency is PLN |

Platform pieces that are not pure logic:

- Local notifications and a daily check after reboot (`ReminderAlerts.kt`, `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`)
- All-day calendar events for reminders (`ReminderCalendarSync.kt`)
- Camera capture and gallery pick for event photos
- Share / save of the export ZIP and chart image

## Target layout

Keep `android/` untouched. Add `flutter/` as its own app.

```
flutter/
  lib/domain/     # enums, money, consumption, charts, reminders, mileage
  lib/carnotes/   # parse and write the ZIP
  lib/data/       # Drift tables, repositories, photo files, selected car
  lib/ui/         # Material 3 screens, one folder per current screen
  test/           # ports of the existing JVM tests, plus the sample ZIP
```

Do not introduce a use-case layer, Hilt equivalent, or a second design system. Screens call repositories. Domain functions stay plain Dart with no Flutter imports, same as today’s `domain/service` objects.

## Library map

| Kotlin | Flutter |
| --- | --- |
| Compose Material 3 | Flutter Material 3 (`useMaterial3: true`) |
| Navigation Compose | `go_router`, same route names as `Routes.kt` |
| Room | Drift. Tables match `Entities.kt` column names |
| DataStore | `shared_preferences` for the selected car id only |
| Hilt + ViewModel + Flow | Repository streams (`Stream`) read by the screen’s notifier |
| kotlinx.serialization | `dart:convert` for Carnotes JSON. It is a flat string map, not a typed schema |
| BigDecimal | `package:decimal`. Do not use `double` for money or fuel amounts |
| Vico | `fl_chart` |
| Coil | `Image.file` for local photos |
| Calendar provider | `device_calendar` |
| AlarmManager + notification channel | `flutter_local_notifications` |
| File picker / ZIP | `file_picker` + `archive` |

Add a package only when the row above needs it. Chart image export can stay a later step: `ChartImageSaver` writes a bitmap; `fl_chart` plus `RepaintBoundary.toImage` covers it.

## Data model

Drift tables copy the Room entities, including `externalId` as the stable key and cascade deletes from car → events / reminders / notes / categories / planned expenses, and from event → photos.

Stored money stays a string. `CurrencyFormatter.parseStored` treats blank and `-1` as null and rejects negatives. Port that before any screen formats a cost.

Flutter does not migrate Room files from schema 1 through 5. A user who already has data exports a ZIP from the Android app and imports it. New installs start empty and hit the welcome screen.

## Phases

Each phase ends when its check is green. Do not start UI while the domain tests are red.

### 1. Domain and Carnotes, no UI

Port, in this order:

1. `Enums.kt` (`EventType`, `FuelType`, `VehicleUnits`, `CarIcon`, horizons, chart presets)
2. `CurrencyFormatter` and `RecordCostService`
3. `ConsumptionCalculator` and `MileageValidationService`
4. Chart, reminder, and ownership math in `ChartDataService` and `Services.kt`
5. `CarnotesParser` and `CarnotesExporter`

Copy the existing fixtures into `flutter/test/fixtures/`:

- `android/app/src/main/assets/sampledata/`
- `android/app/src/test/resources/carnotes_zip_exported_1781131397635/`

Port these tests first. They already encode the rules:

- `ServiceTests.kt` (consumption, currency, mileage, reminders)
- `CarnotesParserTest.kt`
- `CarnotesExporterTest.kt`
- `CarIconTest.kt`
- `EventDateTest.kt`
- `ChartAxisLabelsTest.kt`

Exit check: `flutter test` passes on those fixtures, including a parse → export → parse round trip that keeps car, event, reminder, note, and photo ids.

### 2. Local database and import

Drift schema, repositories shaped like `Repositories.kt`, photo files under the app documents directory (`EventPhotoStore` relative paths), selected-car preference.

`ImportCoordinator` behavior to keep: preview counts, replace-per-car on import, infer fuel types, delete photo files when a car is replaced, export the full ZIP.

Exit check: widget-free test that imports the sample ZIP into an in-memory Drift database and exports a ZIP the parser accepts again.

### 3. Shell and daily entry

Material 3 theme from `Theme.kt` / `DesignTokens.kt`: light and dark `ColorScheme`, spacing xs–xl (4, 8, 16, 24, 32). Event colors stay the fuel / repair / papers tokens used by `EventTypeBadge`.

Screens: welcome, add/edit car, dashboard, events list, add/edit fuel, add/edit expense. Bottom bar below the wide breakpoint, rail above it, matching `AppNavHost`.

Polish strings move to `app_pl.arb`. English can wait; the current UI is Polish.

Exit check: manual pass on an Android emulator — empty welcome, add a car, log a full-tank fill-up, see it on the dashboard with PLN formatting.

### 4. The rest of the workflow

Charts (four kinds, period chips, save image), planning, reminders CRUD, more-tab import and export.

Exit check: the same sample ZIP imported from the Android app shows the same monthly spend, consumption, and reminder due state.

### 5. Device integrations

Only after phase 4. These need a device, not just unit tests.

- Notification permission, a daily reminder check, reschedule after reboot
- Calendar insert/delete using `syncedItemIdentifier`
- Camera and gallery photos on an event, restored from the ZIP

iOS is the same Dart UI once Android matches. Calendar, notifications, and photos get an iOS pass of their own. Do not block the Android Flutter build on iOS entitlements.

## Out of scope

- Rewriting `android/` in place, or generating Kotlin from Dart
- Reading `carexpensetracker.db` directly
- A shared monorepo package consumed by the Kotlin app
- New features (accounts, sync, extra currencies)
- Replacing the Carnotes JSON shape

## First change to make

Phase 1 only: `flutter create` under `flutter/`, add `decimal`, port the domain files and the tests listed above. No Drift, no screens, no plugins. That is the smallest slice that fails if money, consumption, or Carnotes parsing drifts.
