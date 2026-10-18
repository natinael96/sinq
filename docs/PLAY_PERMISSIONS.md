# Google Play Permission Declarations & Justifications

This document provides the compliance justifications and implementation details for sensitive and special permissions requested by **Sinq (ስንቅ)** in accordance with Google Play Developer Program Policies.

---

## 1. `SCHEDULE_EXACT_ALARM`

### Permission Identifier
`android.permission.SCHEDULE_EXACT_ALARM`

### App Category & Core Functionality
- **Category**: Books & Reference / Lifestyle (Devotional & Liturgical Companion)
- **Core Functionality**: Ethiopian Orthodox Tewahedo Church Horologium (መጽሐፈ ሰዓታት) canonical prayer cycle.

### Play Console Permission Declaration Justification
> **Core Feature Description**:
> Sinq provides timely reminders for the Seven Canonical Hours of the Ethiopian Orthodox Horologium (ጸሎተ ነግህ / Prime at 06:00, ሠለስት / Terce at 09:00, ስድስት / Sext at 12:00, ተስዐት / None at 15:00, ሠርክ / Vespers at 17:00, ንዋም / Compline at 21:00, and መንፈቀ ሌሊት / Midnight at 00:00).
>
> **Why Exact Timing is Essential**:
> In the liturgical tradition of the Ethiopian Orthodox Tewahedo Church, canonical prayers must be recited at their designated liturgical hour. Deferring or bundling these alarms in Doze maintenance windows by 30 to 60 minutes causes users to miss the canonical hour entirely, undermining the app's core devotional purpose.
>
> **Graceful Fallback Implementation**:
> 1. On Android 12+ (API 31+), the app explicitly audits permission status via `AlarmManager.canScheduleExactAlarms()`.
> 2. If the user revokes or has not granted exact alarm scheduling, the app never crashes; it gracefully degrades to inexact alarms using `AlarmManager.setAndAllowWhileIdle(RTC_WAKEUP, ...)`.
> 3. The user is provided with an informative in-app guidance card in **Settings > Reminders not arriving?** explaining why exact alarms are needed and providing a direct system link (`Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM`) to grant permission if desired.
> 4. All reminder types (canonical hours, nightly prayer nudge, daily lectionary readings) can be individually configured or completely disabled by the user at any time.

---

## 2. `RECEIVE_BOOT_COMPLETED`

### Permission Identifier
`android.permission.RECEIVE_BOOT_COMPLETED`

### Justification
AlarmManager alarms are cleared by the Android system upon device restart. `RECEIVE_BOOT_COMPLETED` is used exclusively by `SystemEventsReceiver` to re-arm the user's active prayer hour schedule and reminder preferences after a device reboot. No background services or long-running workers are spawned.

---

## 3. `POST_NOTIFICATIONS`

### Permission Identifier
`android.permission.POST_NOTIFICATIONS`

### Justification
Required on Android 13+ (API 33+) to display prayer hour alarm ringing heads-up notifications, daily Scripture / Synaxarium lectionary nudges, and evening prayer invitations. Granted via standard runtime permission request flow.

---

## 4. Zero Network & Privacy Guarantee

- The application declares **zero analytics SDKs**, **zero crash-reporting SDKs**, and **zero tracking libraries**.
- All prayer logs, habits, bookmarks, highlights, and journal entries are stored strictly on-device in local DataStore and Room SQLite databases.
- The app operates 100% offline.
