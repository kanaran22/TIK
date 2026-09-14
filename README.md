# tik — Android To-Do Reminders (Time + Location, Alarm-Style)

A native Android to-do/reminder app, written in Kotlin with Jetpack Compose. Each
task can remind you:

- **at a specific time**, on a schedule that repeats like an alarm clock's —
  once, every day, chosen weekdays, a date range, or a hand-picked set of
  dates — with a louder alarm-stream notification sound + strong vibration, or
- **when you arrive at, or leave, a place** (a geofence — e.g. "remind me to buy
  milk when I get to the supermarket"), on the same repeat schedule, or
- **both at once** — the time picker becomes an arming *window* (e.g.
  8:00–10:30 AM): the geofence is only checked during that window on
  scheduled days. Miss the window with no location match and it just waits
  for the next scheduled day — no notification, no fallback fire.

Reminders show up as Android notifications with **"Mark as done"** and
**"Snooze 10 min"** actions, and keep working even if the app is closed or the
phone reboots. For a repeating task, marking it done completes just *that
day's* occurrence — the series stays armed for its next one. A one-off
(repeat = "Once") task completes for good.

Beyond the core reminder engine, the app also has:

- **Delete confirmation + Undo** — deleting a task asks first, then offers an
  Undo snackbar that restores it (and its completion history) exactly as it was.
- **Saved places** — star any picked location to reuse it on future tasks
  without re-searching or re-locating.
- **Search / sort / filter** on the task list (by kind — time/place/both — and
  by next-occurrence or title).
- **Streaks & stats** — a dedicated screen showing each repeating task's
  current streak and a 30-day completion rate, plus totals across all tasks.
- **A home screen widget** (Jetpack Glance) — shows your active reminders,
  lets you check them off or jump into a task, right from the launcher.

The UI is a deliberately raw **"Brutalist Chaos"** look: thick black borders,
hard (unblurred) drop shadows, one bright yellow accent, clashing bold-sans +
mono type, sharp corners throughout — in both light and dark mode.

## Project structure

```
app/src/main/java/com/kanaran/tik/
├── data/          Room entities (Task, SavedPlace, TaskCompletion), DAOs,
│                  database, repository, RepeatType, ScheduleUtil (repeat
│                  calendar math), StreakUtil (streak/completion-rate math) and
│                  TaskRules (the shared done / due / on-the-widget rules)
├── reminder/      AlarmManager scheduling, Geofencing, notifications,
│                  boot rebooting, "mark done"/"snooze" actions
├── ui/
│   ├── list/      Task list screen (search/sort/filter, swipe-delete + undo)
│   ├── editor/    Add/edit task screen, location picker (+ saved places),
│   │              repeat picker, time/window picker
│   ├── stats/     Streaks & stats screen
│   ├── components/  Brutalist design system — HardShadow, buttons, section
│   │              cards, confirm dialog, snackbar, date/time pickers
│   ├── navigation/  Compose Navigation graph
│   └── theme/     Brutalist Chaos Material 3 theme (light + dark)
├── util/          Permission + location/geocoding helpers
├── viewmodel/     TaskViewModel (StateFlow of tasks/completions/saved places)
├── widget/        Home screen widget (Jetpack Glance)
└── MainActivity.kt / TikApplication.kt
```

Architecture is a simple Repository + ViewModel (MVVM) setup with a hand-rolled
service locator (`TikApplication`) instead of a DI framework, to keep the
project easy to read end-to-end.

## How reminders work

- **Repeat schedule** (`RepeatType` + `ScheduleUtil`): every task carries a
  repeat pattern — Once (a single date), Daily, Weekly-custom (chosen
  weekdays), a date range, or a hand-picked set of custom dates. `ScheduleUtil`
  is pure calendar math (`java.time`, no Android dependencies) that answers
  "is `epochDay` a scheduled day for this task?" and "when's its next
  time-based fire?".
- **Time-only tasks** get a real `AlarmManager.setExactAndAllowWhileIdle(...)`
  alarm for their *next* occurrence — computed from the repeat schedule. When
  it fires, `AlarmReceiver` shows the notification and immediately schedules
  the *following* occurrence (if the schedule has one).
- **Location tasks** (alone or combined with time) use Google Play services'
  `GeofencingClient`, registered *continuously* while the task is active —
  there's no per-day add/remove. `GeofenceBroadcastReceiver` gates each
  transition at receive-time: is today a scheduled day, has it not already
  fired today, and (if combined with time) is the current time inside the
  task's window? Only then does it notify. This means a combined task needs
  **no alarm at all** — the window is enforced entirely in the geofence
  receiver.
- **Re-arming.** A reboot clears every alarm and geofence, and so does a
  force-stop — including the "clean up"/battery-boost buttons many Android skins
  ship. `TikApplication.onCreate` re-arms everything whenever the process starts,
  and `BootReceiver` also re-arms on reboot, app update, clock or time-zone
  change (fire times are absolute instants), and the exact-alarm permission being
  granted again. Re-arming is idempotent: every alarm and geofence is keyed per task.
- **Done means done.** One set of rules in `TaskRules` decides what's ticked, what's
  due, and what the widget shows. A task already marked done today is not reminded
  later that day — by its alarm, its geofence, or a pending snooze.
- Picking a place doesn't require the Google Maps SDK/API key — you either
  search an address (with a live suggestions dropdown, debounced via
  `Geocoder`) or tap "Use current location". Getting the current location
  layers three fallbacks (cached fused location → fresh fused fix with a
  timeout → raw `LocationManager` last-known location) since Play services'
  fused "current location" call can hang indefinitely with no callback on
  some devices/emulator images.

## Home screen widget

`TikWidget` (Jetpack Glance, `androidx.glance:glance-appwidget`) shows a
scrolling list of today's tasks — everything active, plus anything ticked today,
so a mistaken tick can be undone — with a checkbox, title, and time/place summary,
plus a "+ NEW" button. Tasks are ordered newest first and **don't move when
ticked**, so the same tap unticks them.

It collects the task table as a Flow *inside* its composition. That matters: while
a Glance session is alive, `updateAll()` only recomposes it and never calls
`provideGlance` again, so data read once up front goes stale — the widget used to
keep showing a task ticked after it had been unticked. The `widgetUpdater` hook in
`TaskRepository` now only has to start a session when none is running.

A checkbox tap runs `ToggleTaskAction`, which flips what the *database* says (via
`TaskRepository.setDoneToday`, the same call the list and the notification's
"Mark as done" use); tapping the row or "+ NEW" deep-links into `MainActivity` with
the same `EXTRA_TASK_ID` a notification tap uses. Card,
checkbox, and button borders are plain `<shape>` drawables (with `-night`
color variants) rather than a Glance border modifier, since RemoteViews-backed
widgets render those more reliably than any first-party alternative.

## Updating tik without losing data

Install a new APK **over** the old one — never uninstall first. Android then keeps
every task, streak and saved place. An update works only when all three hold:

1. **Same package** — `com.kanaran.tik`. Never change it.
2. **Same signing key** — `tik-upload.jks`. An APK signed with any other key is
   refused (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`; on the phone, "App not
   installed" / "package conflicts with an existing package"). The key and its
   `keystore.properties` are gitignored, so they exist only on the build machine:
   **back them up**. Lose the key and the installed app can never be updated again.
   To change its password without changing the key: `keytool -storepasswd -keystore tik-upload.jks`
   (then update `keystore.properties`).
3. **Higher version** — `versionCode` is the git commit count, so every committed
   build is newer. Installing an older one is refused (`INSTALL_FAILED_VERSION_DOWNGRADE`).
   The installed version is shown at the bottom of the Stats screen.

A debug APK is signed with a different (debug) key, so it can never update a
release install — keep them apart.

If an install is refused, **don't uninstall to get past it** — that deletes the
data. Fix the key or version instead.

When moving to Google Play: Play re-signs apps with its own key by default, which
would make the Play build unable to update a sideloaded install. Enrol in Play App
Signing with *"use my own key"* and upload `tik-upload.jks`, so both carry the same
signature.

As a safety net, Android Auto Backup is on and covers the database — including its
`-wal`/`-shm` files, since Room keeps recent writes there — so a reinstall on a
phone with Google backup enabled can restore tasks.

## Phones that stop background apps

Some Android skins — Vivo's FuntouchOS/OriginOS, Xiaomi, Oppo/Realme, Samsung and
others — pause background apps well beyond stock Android, which delays or drops
reminders; no code inside the app can override that. On those brands the task
list shows a **Keep reminders on time** card with the exact settings to change
(for Vivo: Autostart, background power consumption, notifications, and locking
tik in Recents) and buttons that open them. It can be hidden and brought back from
Stats › Battery tips. Stock phones never see it. Detection and wording live in
`util/BackgroundReliability.kt`; debug builds can fake a brand for testing:

```bash
adb shell am start -n com.kanaran.tik/.MainActivity --es debug_force_brand vivo
```

## Database migrations

The Room schema is at version 3, and each version's schema is exported as JSON to
`app/schemas/` (committed alongside the code) — those files are what migrations
get written and diffed against.

Versions 1 and 2 only ever existed on dev machines mid-build and their schemas
were never recorded, so those two are still allowed to reset via
`fallbackToDestructiveMigrationFrom(true, 1, 2)`. **From version 3 onward there is
no destructive fallback**: a schema change without a matching migration will fail
loudly at launch instead of silently wiping someone's tasks and streak history.

To change the schema:

1. Edit the entity and bump `version` in `TaskDatabase`.
2. Build once — Room writes `app/schemas/<new version>.json`. Diff it against the
   previous version to see exactly what SQL is needed.
3. Add a `Migration(from, to)` in `Migrations.kt` and list it in `ALL_MIGRATIONS`.

## Tests

`ScheduleUtil`, `StreakUtil` and `TaskRules` hold all the calendar math and the
shared "is it done / is it due / is it on the widget" rules, with no Android
dependencies, so they're covered by plain JVM unit tests:

```bash
./gradlew.bat :app:testDebugUnitTest   # Windows
./gradlew :app:testDebugUnitTest       # macOS/Linux
```

The suite pins down the fiddly edges: inclusive date-range bounds, the Monday=bit 0
weekday mask, a fire time landing exactly "now", unscheduled days not breaking a
streak, completion rate never looking back past a task's creation day, and
regressions for shipped bugs — a one-off task that vanished from the widget when
ticked, a ticked task that still fired its reminder, and ticked tasks jumping out
of the widget's visible rows.

## Permissions this app requests, and why

| Permission | Why |
|---|---|
| `POST_NOTIFICATIONS` | Required at runtime on Android 13+ to show any notification. |
| `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` | Needed to search/use your current location and to monitor geofences. |
| `ACCESS_BACKGROUND_LOCATION` | Needed so a location reminder still fires while the app is in the background (Android 10+ requires this as a separate grant). |
| `SCHEDULE_EXACT_ALARM` / the "Alarms & reminders" special access on Android 12+ | So time-only reminders fire at the exact minute instead of being batched. |
| `RECEIVE_BOOT_COMPLETED` | Re-arms reminders after a restart. |

The app never requests these all at once — the task list shows a small banner
for whichever permission is still missing, with a button that triggers just
that request.

## Building it

Open the project folder in **Android Studio** and click Run ▶ with a device or
emulator running Android 8.0 (API 26) or later — an emulator needs Google Play
services for geofencing/location, so use a **Play Store**-flavored system
image, not a plain Google APIs one. Or from a terminal:

```bash
./gradlew.bat assembleDebug   # Windows
./gradlew assembleDebug       # macOS/Linux
```

Minimum SDK is 26 (Android 8.0); target/compile SDK is 37. No API keys are
required to build or run. `gradle.properties` opts the project back to AGP's
classic DSL and standalone Kotlin plugin (`android.builtInKotlin=false`,
`android.newDsl=false`) rather than AGP 9's new built-in-Kotlin DSL — revisit
that before upgrading to AGP 10, which removes the opt-out.

## Trying it out

1. Run the app on a device or emulator.
2. Grant the permission banners at the top of the task list.
3. Tap **New task**, give it a title, and turn on **Remind me at a time**
   and/or **Remind me at a place**.
   - Time only: pick a single fire time. Both enabled: pick a **From/Until**
     window instead — the place is only checked for during that window.
   - For a location reminder, search an address (suggestions appear as you
     type) or tap "Use current location", set a radius, and choose
     Arrive/Leave/Both.
   - Set a **Repeat** pattern: Once, Daily, Weekdays (pick specific days),
     Date range, or Custom dates (pick individual dates one at a time).
4. Save. Time-only reminders fire via a real system alarm; location reminders
   fire when you cross the geofence during an armed window (on a real device,
   walk/drive out of and back into the radius; on an emulator use Extended
   Controls → Location to simulate movement, or `adb emu geo fix <lng> <lat>` —
   note some emulator images need `adb shell cmd location providers ...` or a
   few minutes' delay before a mock fix actually lands).
5. Long-press the home screen → **Widgets** → **tik** to add the widget.
   Star a picked location (the outline star next to it) to reuse it later from
   the **SAVED PLACES** chips. Open the bar-chart icon on the task list for
   **Streaks & stats**.

## Known limitations / good next steps

- The location picker is search + "current location" based, not a visual
  map. Adding `com.google.maps.android:maps-compose` +
  `com.google.android.gms:play-services-maps` (with a Google Maps API key)
  would let users drop a pin and see the radius circle visually.
- A combined time+location task that misses its window fires nothing that
  day (by design) — there's no "fires anyway at window end" fallback.
- No cloud sync/backup beyond Android's local Auto Backup — everything lives
  in a local Room database (`tik.db`). Schema changes currently use
  `fallbackToDestructiveMigration` (pre-release; revisit before a real
  release so upgrades don't wipe user data).
- No automated tests yet; `TaskRepository` and `ScheduleUtil` are written so
  they're easy to unit test (`ScheduleUtil` has zero Android dependencies).
- Notifications are a "louder" alarm-stream sound + strong vibration, not a
  full-screen takeover — they still respect silent mode/Do Not Disturb. A
  true full-screen alarm (`USE_FULL_SCREEN_INTENT` + an alarm Activity) would
  be the next step up if that's ever wanted.
