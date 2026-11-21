# ALzikr

**تنزيل مباشر (أندرويد):** https://github.com/KL4PINGUINSKL/ALzikr/releases/latest/download/ALzikr.apk
(الرابط يعطي دائمًا أحدث إصدار. عند التثبيت اسمح لمتصفحك بتثبيت التطبيقات.)

A light Android app (Compose, no other libraries) from the AL family:

- **Prayer times** computed on the phone (PrayTimes.org equations, 10 calculation methods, Hanafi Asr, per-prayer minute offsets), adhan alert with a chosen sound, heads-up before each prayer, qibla direction. The method follows the country until the user picks one.
- **Adhkar and daily awrad**: tap counter with vibration, custom adhkar with daily targets, streak, morning/evening reminders after Fajr/Asr.
- **Tasks and notes**: tasks can be tied to a prayer ("after Asr") and are listed in that prayer's notification.
- **Focus sessions**: 15–60 min with a countdown in the notification shade (no service), break, daily stats, warns when the adhan falls inside the session.

Build: `android\gradlew.bat assembleDebug` with JAVA_HOME = JDK 17. Releases also go to KL4PINGUINSKL/ALapps as `ALzikr-<ver>-Android.apk` (ALhub and the app's own update check read them).

- **Explained hadith**: HadeethEnc.com (Encyclopedia of Translated Prophetic Hadiths) — explanation, benefits, grade, references; cached on the phone once opened.
- **Islamic books** (Ahl as-Sunnah wal-Jama'ah): IslamHouse.com public API — category tree, PDF download, in-app reader (PdfRenderer) that remembers the page.
- **Tools**: zakat calculator, Hijri/Gregorian converter, daily khatma reminder; update and share the app from «المزيد».
