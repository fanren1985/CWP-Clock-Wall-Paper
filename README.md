引子：

故事，还得从小米HyperOS上线前说起。

那时我在用小米MIX4手机，搭载的应该是MIUI末期的某个版本，非常喜欢它有一个明暗壁纸定时切换的功能。

一开始，我以为是MIUI系统级别的黑科技，对同一张图片进行了像素明暗重构处理，来实现白天壁纸图片是亮光景色，晚上壁纸图片是暗光景色。

后来找工具提取喜欢的系统壁纸时才发现，实际是MIUI在用户设定的时点，用不同亮度下（但景物主体一模一样）拍摄的两张图片来回切换。

再后来，小米HyperOS正式上线，这个最喜欢的壁纸功能就无影无踪了。我在互联网海量资源里苦苦寻找，愣是一直没找到类似的代替方案。

借助AI编程的迅猛发展，作为计算机外行的我终于下决心，尝试从头手搓这个功能应用。

所以，就有了眼下这个小工具：CWP - Clock Wall Paper (Android phone xiaomi 14 ONLY) | 定时壁纸（仅安卓手机xiaomi 14）。

++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

以下是值得提醒的一些Tips，重要信息前置：

一、测试机型：小米14安卓手机
（抱歉，目前我仅有此机型。其他的机型请大家自行下载、测试随缘，我不会去额外测试）。

二、APP稳定运行所需权限：
- 在最近任务列表中将APP锁定
- 省电策略-无限制
- 自启动-开启
- 权限管理-“照片和视频”（始终允许），其他权限可以全部拒绝
- 通知管理-不允许

++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

CWP能做什么？很简单，就是实现我上面所说的定时切换不同壁纸。具体来说：

### 1 CWP（定时壁纸）是一款 Android 手机应用，允许用户按预设计划自动更换设备桌面壁纸和锁屏壁纸。

### 2.1 创建定时任务

| 任务列表 | 按已设定计划任务的时间升序排列，显示所选壁纸缩略图、日子类型、时间、文件名、壁纸类型 |
| 创建任务 | 点击右下角+号 → 选择日子类型 → 设定时间 → 选择图片 → 选择壁纸类型，完成创建计划任务 |
| 删除任务 | 点击任务卡片右侧的垃圾桶图标，确认后删除该计划任务 |

### 2.2 日子类型

| 仅一次 | 执行完毕后自动删除任务 |
| 每天 | 每日触发执行 |
| 周一至周五 | 自然日历的周一至周五 |
| 周六+周日 | 自然日历的周末 |
| 法定工作日 | 工作日且非法定节假日 |
| 法定节假日 | 仅法定节假日（内置 2024–2026 中国法定节假日） |

### 2.3 时间类型

- 没啥好说的，拨动小时、分钟指针完成设置

### 2.3 壁纸类型

| 桌面壁纸 | 仅修改桌面壁纸 |
| 锁屏壁纸 | 仅修改锁屏壁纸 |
| 桌面+锁屏 | 同时修改桌面和锁屏壁纸 |

### 2.4 设置

| 夜间模式 | 开启 / 跟随系统（互斥 Switch） |
| 主题色 | 低饱和度 9 色可选（红、橙、黄、绿、青、蓝、紫、黑、白），全局应用至工具栏、新建+号按钮（FAB）、状态栏 |
| 从最近任务中隐藏 | 从系统最近任务列表中隐藏应用，眼不见心不烦 |

## 3. 技术架构

### 3.1 文件结构

```
CWP/
├── app/
│   ├── build.gradle.kts          # 自动版本号递增
│   ├── version.properties        # 当前版本计数器
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/CWP/app/
│       │   ├── MainActivity.kt    # 主界面 + 全部业务逻辑
│       │   ├── WallpaperReceiver.kt  # 闹钟广播接收
│       │   └── BootReceiver.kt    # 开机广播接收
│       └── res/
│           ├── layout/
│           │   ├── activity_main.xml
│           │   ├── item_wallpaper_task.xml
│           │   └── dialog_settings.xml
│           ├── drawable/          # 药丸背景、图标等
│           └── values/            # 字符串、主题、颜色
```

### 3.2 数据模型

```kotlin
data class WallpaperTask(
    id: Long,           // System.currentTimeMillis()
    hour: Int,          // 0–23
    minute: Int,        // 0–59
    imageUri: String,   // content:// URI
    displayName: String,
    wallpaperFlag: Int, // FLAG_SYSTEM / FLAG_LOCK / 两者 or
    dayType: DayType
)
```

### 3.3 闹钟调度

- 首次调度：`MainActivity.scheduleAlarm()`
- 重复调度：`WallpaperReceiver.rescheduleAlarm()`
- 开机恢复：`BootReceiver.onReceive()`
- 仅一次类型：执行后 `WallpaperReceiver` 自动从存储中删除
- 闹钟类型：`AlarmManager.setAlarmClock()`（精确闹钟，系统可显示）

### 4.4 关键依赖

| 依赖 | 用途 |
|------|------|
| Material Components | MaterialCardView, MaterialButton, Chip, FAB, MaterialAlertDialogBuilder |
| AppCompat | AppCompatActivity, 夜间模式, 沉浸式 |
| RecyclerView | 任务卡片列表 |
| ConstraintLayout | 主界面布局 |
