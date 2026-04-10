# Refactor & Simplification Plan

## 1. Centralize runtime permission and battery optimization handling
- **Observation:** `MainActivity.kt:35-86` duplicates notification permission logic already exposed by `PermissionManager.kt:66-71`, and battery optimization handling lives only in the activity.
- **Plan:** Move all permission/battery optimization workflows into `PermissionManager` (or a renamed helper), expose a single `ensurePrerequisites { onReady() }` style entry point, and delete the unused `MainActivity.requestNotificationPermission` along with direct permission branching in the activity.
- **Expected impact:** Shrinks `MainActivity`, keeps permission UX in one place, and makes reuse/testing easier if more entry points are added.

## 2. Collapse duplicated step-sensor bootstrapping
- **Observation:** `DataSenderWorker.kt:36-103` and `TrackerDataCollector.kt:17-30` spin up separate `StepSensorManager` instances and duplicate warm-up and persistence concerns.
- **Plan:** Let `TrackerDataCollector.collectData()` own the sensor warm-up/tear-down lifecycle (possibly via a `suspend fun StepSensorManager.readDailySteps()` helper), and inject the collector into the worker so the worker no longer manages a second sensor manager or its own handler thread.
- **Expected impact:** Removes the worker-side `collectSensorData()` block entirely, reduces race conditions between parallel listeners, and keeps sensor logic in one class.

## 3. Trim unused reactive scaffolding in `StepSensorManager`
- **Observation:** `StepSensorManager.kt:33-34` exposes a `Flow`, and imports `Channel`/`receiveAsFlow`, none of which are consumed; `SAMPLING_PERIOD_MICROS` is also unused.
- **Plan:** Drop the unused reactive API, keep only synchronous getters backed by `StepDataStorage`, and document the baseline adjustment once. If observers are needed later, introduce them behind a smaller interface.
- **Expected impact:** Cuts several properties/imports, clarifies the class as a simple data source, and reduces the risk of partial reactive migrations.

## 4. Inline the custom `Location` wrapper
- **Observation:** `TrackerDataCollector.kt:38-50` wraps latitude/longitude in `models/Location.kt`, only to unpack it immediately when creating `TrackerData`.
- **Plan:** Return a lightweight `Pair<Double, Double>` (or populate `TrackerData` directly) and delete `models/Location.kt`; update callers accordingly.
- **Expected impact:** Removes an entire model file, simplifies the collector, and avoids extra object churn.

