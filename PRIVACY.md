# Privacy Policy for Sinq (ስንቅ)

**Effective Date:** 25 September 2026  
**Application:** Sinq (ስንቅ), package `com.sinq.app`  
**Developer:** Natinael M. (`natinael.96@gmail.com`)  
**Web Policy:** [https://sinq.natinael96.tech/privacy-policy.html](https://sinq.natinael96.tech/privacy-policy.html)

---

## 1. Summary

**Sinq collects no personal data. None.**

There are no user accounts, no analytics tracking, no advertising networks, and no third-party data collection SDKs of any kind. Everything you read, write, mark, or calculate remains strictly on your local device.

---

## 2. On-Device Storage

All personal records and user configurations are stored locally on your device in local SQLite databases / Android DataStore preferences:
- **Settings & Preferences:** Language, dark/light theme, font size, Ethiopic typeface, reminder and alert preferences.
- **Prayer & Habit Logs (ጉዞ):** Recorded hourly prayers, prostrations, fast tracking, and daily completions.
- **Bookmarks & Highlights:** Saved citations, notes, custom prayer hours, and personal prayer lists.
- **Confession & Spiritual Notes (ማስታወሻ / ንስሐ):** Private spiritual notes and confession preparation lists.
- **Tithes & Vows (አስራት / ስዕለት):** Offline record ledgers.

Android's automatic cloud backup (`android:allowBackup="false"`) is explicitly disabled in the application manifest, ensuring that your spiritual notes and private prayer records are never automatically synchronized to third-party cloud servers.

---

## 3. Network Access & Use

The application requests `android.permission.INTERNET` solely for:
1. **Application Updates:** In self-updating builds, checking for newer versions once daily. No personal identifiers or analytics are sent. In Google Play builds, updates are handled natively through the Google Play Store In-App Update API.
2. **External Commentary:** If you explicitly choose to view Catena patristic commentary on a specific biblical verse, the app opens the verse on `catenabible.com` in your device's external web browser.

No user data, device metrics, or telemetry are ever uploaded.

---

## 4. Permissions Requested

- `android.permission.POST_NOTIFICATIONS`: To show prayer hour reminders and notifications configured by you.
- `android.permission.USE_EXACT_ALARM` & `SCHEDULE_EXACT_ALARM`: Required to sound canonical prayer hour alarms at the precise scheduled times according to the Orthodox Agpeya hours.
- `android.permission.RECEIVE_BOOT_COMPLETED`: To restore and reschedule your prayer alarms after the device reboots.
- `android.permission.VIBRATE`: For the tactile vibration alert when prayer alarms fire.
- `android.permission.INTERNET`: Used exclusively as described in Section 3 above.

The application **never** requests access to your location, camera, microphone, contacts, biometric data, phone state, or external storage scanning.

---

## 5. Data Deletion & Backup

You retain full control over your data:
- **Export / Backup:** You may manually export your settings and records to a local JSON file at any time from *Settings → Records*.
- **Data Deletion:** Uninstalling the app completely purges all local databases and preferences from your device.

---

## 6. Contact

If you have questions or concerns regarding this Privacy Policy:
- Email: [natinael.96@gmail.com](mailto:natinael.96@gmail.com)
- Telegram: [@natinael96](https://t.me/natinael96)
- Website: [https://sinq.natinael96.tech](https://sinq.natinael96.tech)
