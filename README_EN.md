<p align="center"> <img width="2410" height="2410" alt="egg_foreground" src="https://github.com/user-attachments/assets/93fc3866-2c46-4e60-8ad8-c6109c17d04b" style="width:30%;"/> </p> <br><br>

<div align="center">

English | [中文](./README.md)

</div>
<br>
This story dates back to before the launch of Xiaomi HyperOS.

Back then I was using the Xiaomi MIX4 running one of the final MIUI versions. I was really fond of its **timed light/dark wallpaper switching** feature.

At first, I thought it was a sophisticated system-level tech that reconstructed the pixels of a single image to create bright scenery for daytime and dim scenery for nighttime.

Later, when I tried to extract the built-in wallpapers with tools, I found out how it actually works: MIUI simply swaps between two photos taken at different brightness levels (with identical subjects) at the scheduled time.

After Xiaomi HyperOS officially rolled out, this favorite wallpaper feature disappeared completely. I searched far and wide online but never found a proper alternative.

Thanks to the rapid advancement of AI programming, I, with no professional computer background, finally decided to build this tool from scratch on my own.

That’s how this little utility came into being:
CWP - Clock Wall Paper (Android phone xiaomi 14 ONLY) | Timed Wallpaper (Xiaomi 14 Android Phone Only).

---
<div align="center">
    
Download APK [from HERE](https://github.com/fanren1985/CWP/releases)

</div>
<br><br>
### Important Tips (Key information first)
1. **Tested Device**: Xiaomi 14 (Android)
Sorry, this is the only device I own. Feel free to download and test it on other models at your own discretion; I will not run extra tests for other devices.

2. **Required Permissions for Stable Operation**
- Lock the app in the recent apps list
<img width="360" height="801" alt="Screenshot_2026-05-26-17-22-33-534_com miui home" src="https://github.com/user-attachments/assets/0111ea76-ede9-4b16-b91f-647c88e0af2f" style="width:30%;"/>

- Battery saver: Set to **No restrictions**
<img width="360" height="801" alt="Screenshot_2026-05-26-16-23-40-060_com miui securitycenter" src="https://github.com/user-attachments/assets/a043ef7d-c6c0-4436-94bc-3903743f95ec" style="width:30%;"/>

- Enable **Auto-start**
<img width="360" height="801" alt="Screenshot_2026-05-26-16-23-45-340_com miui securitycenter" src="https://github.com/user-attachments/assets/91c2db3b-cf07-41d1-9274-55e87a70a937" style="width:30%;"/>

- Permission management: Grant **Photos and videos (Always allow)**. All other permissions can be denied.
<img width="360" height="801" alt="Screenshot_2026-05-26-16-25-18-292_com miui securitycenter" src="https://github.com/user-attachments/assets/08c60255-e8da-4c03-ae7e-480040728b63" style="width:30%;"/>
<img width="360" height="801" alt="Screenshot_2026-05-26-16-25-20-940_com miui securitycenter" src="https://github.com/user-attachments/assets/a2cbdaa9-4e8a-448f-8cc6-48ed1dfff455" style="width:30%;"/>

- Notification management: **Block notifications**

---

### What CWP Does
It simply enables timed wallpaper switching as mentioned above.

#### 1.1 Introduction
CWP (Clock Wallpaper) is an Android app that automatically changes your home screen and lock screen wallpapers according to preset schedules.

#### 2.1 Create Timed Tasks
- **Task List**: Sorts scheduled tasks in ascending order by time, showing wallpaper thumbnails, schedule type, time, file name and wallpaper type.
<img width="360" height="801" alt="Screenshot_2026-05-26-16-16-21-006_com CWP app" src="https://github.com/user-attachments/assets/3f6b5609-4bd1-468a-97f6-a5ad5d99d658" style="width:30%;"/>

- **Create a Task**: Tap the plus icon at the bottom right → Select schedule type → Set time → Choose an image → Select wallpaper type to finish creating the task.
<img width="360" height="801" alt="Screenshot_2026-05-26-16-16-28-600_com CWP app" src="https://github.com/user-attachments/assets/0fe4ed32-62cf-4ca5-9aa8-363bd8062031" style="width:30%;"/>
<img width="360" height="801" alt="Screenshot_2026-05-26-16-16-32-239_com CWP app" src="https://github.com/user-attachments/assets/64bf3e44-0676-4547-b9b6-5c2a0c6ff686" style="width:30%;"/>
<img width="360" height="801" alt="Screenshot_2026-05-26-16-16-43-550_com android photopicker" src="https://github.com/user-attachments/assets/ce880ada-bd62-4e88-b7b8-908c6ee7589b" style="width:30%;"/>
<img width="360" height="801" alt="Screenshot_2026-05-26-16-16-57-746_com CWP app" src="https://github.com/user-attachments/assets/50065936-4cef-4121-ad79-763ad17d8892" style="width:30%;"/>

- **Delete a Task**: Tap the trash icon on the task card and confirm to delete the scheduled task.

#### 2.2 Schedule Types
- **Once**: The task will be automatically deleted after execution.
- **Daily**: Triggers every day.
- **Monday to Friday**: Runs on workdays from Monday to Friday.
- **Saturday & Sunday**: Runs on weekends.
- **Legal Workdays**: Activates on official workdays excluding statutory holidays.
- **Statutory Holidays**: Activates only on Chinese statutory holidays (2024–2026 holidays pre-configured).

#### 2.3 Time Setting
Adjust the hour and minute dials to set the desired time.

#### 2.4 Wallpaper Types
- **Home Screen Only**: Changes only the home screen wallpaper.
- **Lock Screen Only**: Changes only the lock screen wallpaper.
- **Home & Lock Screen**: Changes both wallpapers simultaneously.

#### 2.5 Settings
<img width="360" height="801" alt="Screenshot_2026-05-26-16-16-24-798_com CWP app" src="https://github.com/user-attachments/assets/303bdc3a-5fc8-4915-b9ea-9a9be6de61bd" style="width:30%;"/>

- **Dark Mode**: On / Follow System (mutually exclusive toggle)
- **Theme Color**: 9 low-saturation colors available (Red, Orange, Yellow, Green, Cyan, Blue, Purple, Black, White), applied globally to toolbars, floating action button (FAB) and status bar.
- **Hide from Recent Apps**: Remove the app from the system’s recent apps list.

---

## 3. Technical Architecture (Content extracted from AI coding tool)
### 3.1 File Structure
```
CWP/
├── app/
│   ├── build.gradle.kts          # Auto-increment version code
│   ├── version.properties        # Current version counter
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/CWP/app/
│       │   ├── MainActivity.kt    # Main interface & all business logic
│       │   ├── WallpaperReceiver.kt  # Alarm broadcast receiver
│       │   └── BootReceiver.kt    # Boot completion broadcast receiver
│       └── res/
│           ├── layout/
│           │   ├── activity_main.xml
│           │   ├── item_wallpaper_task.xml
│           │   └── dialog_settings.xml
│           ├── drawable/          # Pill-shaped backgrounds, icons, etc.
│           └── values/            # Strings, themes and colors
```

### 3.2 Data Model
```kotlin
data class WallpaperTask(
    id: Long,           // System.currentTimeMillis()
    hour: Int,          // 0–23
    minute: Int,        // 0–59
    imageUri: String,   // content:// URI
    displayName: String,
    wallpaperFlag: Int, // FLAG_SYSTEM / FLAG_LOCK / Both
    dayType: DayType
)
```

### 3.3 Alarm Scheduling
- Initial scheduling: `MainActivity.scheduleAlarm()`
- Rescheduling: `WallpaperReceiver.rescheduleAlarm()`
- Restore after device reboot: `BootReceiver.onReceive()`
- One-time tasks: Automatically deleted from storage by `WallpaperReceiver` after execution
- Alarm type: `AlarmManager.setAlarmClock()` (Precise alarm, visible in system)

### 3.4 Core Dependencies
| Dependency | Usage |
|------|------|
| Material Components | MaterialCardView, MaterialButton, Chip, FAB, MaterialAlertDialogBuilder |
| AppCompat | AppCompatActivity, dark mode, immersive display |
| RecyclerView | Task card list |
| ConstraintLayout | Main page layout |
