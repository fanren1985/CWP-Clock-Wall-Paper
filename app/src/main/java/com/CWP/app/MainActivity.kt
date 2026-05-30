package com.CWP.app

import android.app.ActivityManager
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.Dialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

// ── 日子类型枚举 ────────────────────────────────────────────

enum class DayType(val key: String, val labelResId: Int) {
    ONCE("ONCE", R.string.day_once),
    EVERYDAY("EVERYDAY", R.string.day_everyday),
    WEEKDAY("WEEKDAY", R.string.day_weekday),
    WEEKEND("WEEKEND", R.string.day_weekend),
    WORKDAY("WORKDAY", R.string.day_workday),
    HOLIDAY("HOLIDAY", R.string.day_holiday);

    companion object {
        fun fromKey(key: String): DayType =
            entries.firstOrNull { it.key == key } ?: EVERYDAY
    }
}

// ── 标准9色主题色 ─────────────────────────────────────────

val THEME_COLORS = arrayOf(
    "#E53935" to "红",   // 0
    "#FB8C00" to "橙",   // 1
    "#FDD835" to "黄",   // 2
    "#43A047" to "绿",   // 3
    "#00ACC1" to "青",   // 4
    "#1E88E5" to "蓝",   // 5
    "#8E24AA" to "紫",   // 6
    "#424242" to "黑",   // 7
    "#BDBDBD" to "白"    // 8
)

// ── 法定节假日（硬编码，覆盖 2024-2026）─────────────────────

private val CHINESE_HOLIDAYS: Set<String> = buildSet {
    // 元旦
    add("01-01"); add("01-02"); add("01-03")
    // 春节 2024: 2/10-2/17
    add("02-10"); add("02-11"); add("02-12"); add("02-13"); add("02-14"); add("02-15"); add("02-16"); add("02-17")
    // 清明节
    add("04-04"); add("04-05"); add("04-06")
    // 劳动节
    add("05-01"); add("05-02"); add("05-03"); add("05-04"); add("05-05")
    // 端午节
    add("06-10"); add("06-11")
    // 中秋节
    add("09-17")
    // 国庆节
    add("10-01"); add("10-02"); add("10-03"); add("10-04"); add("10-05"); add("10-06"); add("10-07")
}

// ── 数据模型 ──────────────────────────────────────────────

data class WallpaperTask(
    val id: Long,
    val hour: Int,
    val minute: Int,
    val imageUri: String,
    val displayName: String,
    /** WallpaperManager.FLAG_SYSTEM (1), FLAG_LOCK (2), 或两者 or (3) */
    val wallpaperFlag: Int,
    /** 日子类型，默认每天 */
    val dayType: DayType = DayType.EVERYDAY
)

// ── 列表项封装（任务卡片 + 分组标题）─────────────────────

sealed class TaskListItem {
    /** 分组标题：同一壁纸类型下的第一个任务前显示 */
    data class Header(val wallpaperFlag: Int) : TaskListItem()
    /** 普通任务卡片 */
    data class Task(val task: WallpaperTask) : TaskListItem()
}

// ── MainActivity ─────────────────────────────────────────

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: TaskAdapter
    private val tasks = mutableListOf<WallpaperTask>()

    // 添加任务时暂存的数据
    private var tempDayType = DayType.EVERYDAY
    private var tempHour = 8
    private var tempMinute = 0
    private var tempImageUri: Uri? = null
    private var tempDisplayName = ""
    private var tempWallpaperFlag = WallpaperManager.FLAG_SYSTEM

    // 主题色索引
    private var currentThemeColorIndex = 0

    // 图片选择器
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleImagePicked(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 启用沉浸式 edge-to-edge 显示
        WindowCompat.setDecorFitsSystemWindows(window, false)

        prefs = getSharedPreferences("cwp_prefs", MODE_PRIVATE)

        // 应用夜间模式
        applyDarkMode()
        // 应用隐藏最近任务设置
        applyHideFromRecents(prefs.getBoolean("hide_from_recents", false))

        initViews()
        loadTasks()
        scheduleAllAlarms()
    }

    override fun onResume() {
        super.onResume()
        loadTasks()
        applyThemeColor()
    }

    // ── 视图初始化 ────────────────────────────────────────

    private fun initViews() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // 沉浸式顶栏：给 toolbar 留出状态栏高度，并相应增加高度
        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { v, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(v.paddingLeft, statusBarHeight, v.paddingRight, v.paddingBottom)
            val lp = v.layoutParams
            val actionBarSize = v.context.theme.obtainStyledAttributes(
                intArrayOf(android.R.attr.actionBarSize)
            ).use { it.getDimensionPixelSize(0, 0) }
            lp.height = actionBarSize + statusBarHeight
            v.layoutParams = lp
            insets
        }

        tvEmpty = findViewById(R.id.tv_empty)
        recyclerView = findViewById(R.id.recycler_tasks)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = TaskAdapter(
            onDelete = { task, anchor -> confirmDeleteTask(task, anchor) }
        )
        recyclerView.adapter = adapter

        findViewById<FloatingActionButton>(R.id.btn_add)
            .setOnClickListener { startAddTaskFlow() }

        findViewById<View>(R.id.btn_settings)
            .setOnClickListener { showSettingsDialog() }
    }

    // ── 任务增删改查 ──────────────────────────────────────

    private fun loadTasks() {
        tasks.clear()
        val json = prefs.getString("tasks_json", "[]") ?: "[]"
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            tasks.add(
                WallpaperTask(
                    id = obj.getLong("id"),
                    hour = obj.getInt("hour"),
                    minute = obj.getInt("minute"),
                    imageUri = obj.getString("uri"),
                    displayName = obj.getString("name"),
                    wallpaperFlag = obj.optInt("flag", WallpaperManager.FLAG_SYSTEM),
                    dayType = DayType.fromKey(obj.optString("dayType", "EVERYDAY"))
                )
            )
        }
        // 先按壁纸类型分类，各分类下再按时间排序
        tasks.sortWith(compareBy<WallpaperTask> { it.wallpaperFlag }
            .thenBy { it.hour * 60 + it.minute })
        refreshUI()
    }

    private fun saveTasks() {
        val arr = JSONArray()
        for (t in tasks) {
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("hour", t.hour)
            obj.put("minute", t.minute)
            obj.put("uri", t.imageUri)
            obj.put("name", t.displayName)
            obj.put("flag", t.wallpaperFlag)
            obj.put("dayType", t.dayType.key)
            arr.put(obj)
        }
        prefs.edit().putString("tasks_json", arr.toString()).apply()
    }

    private fun addTask(task: WallpaperTask) {
        tasks.add(task)
        // 先按壁纸类型分类，各分类下再按时间排序
        tasks.sortWith(compareBy<WallpaperTask> { it.wallpaperFlag }
            .thenBy { it.hour * 60 + it.minute })
        saveTasks()
        refreshUI()
        // 修复：仅一次任务也需要调度闹钟
        scheduleAlarm(task)
    }

    /** 二次确认后删除任务 — 使用 PopupWindow 悬浮在垃圾桶左侧 */
    private fun confirmDeleteTask(task: WallpaperTask, anchor: View) {
        val popupView = layoutInflater.inflate(R.layout.popup_delete_confirm, null)

        // 根据当前系统深浅模式设置颜色
        val nightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val isDark = nightMode == Configuration.UI_MODE_NIGHT_YES
        val textColor = if (isDark) Color.WHITE else Color.DKGRAY
        val bgColor = if (isDark) Color.DKGRAY else Color.WHITE

        popupView.background = GradientDrawable().apply {
            setColor(bgColor)
            cornerRadius = (6f * resources.displayMetrics.density)
        }
        popupView.clipToOutline = true
        popupView.findViewById<TextView>(R.id.tv_popup_msg).setTextColor(textColor)
        popupView.alpha = 0.7f

        val popup = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true // focusable — 点击外部关闭
        ).apply {
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
            setOnDismissListener {
                adapter.clearHighlight()
            }
        }

        // 删除按钮：确认删除
        popupView.findViewById<MaterialButton>(R.id.btn_popup_delete).apply {
            backgroundTintList = ColorStateList.valueOf(
                desaturateColor(Color.parseColor("#F44336"))
            )
            setOnClickListener {
                popup.dismiss()
                deleteTask(task)
            }
        }

        // 在 anchor（垃圾桶按钮）左侧展示，垂直居中对齐
        anchor.post {
            val anchorLoc = IntArray(2)
            anchor.getLocationOnScreen(anchorLoc)
            popupView.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            val popupW = popupView.measuredWidth
            val popupH = popupView.measuredHeight
            val margin = (8 * resources.displayMetrics.density).toInt()
            val xOff = anchorLoc[0] - popupW - margin
            val yOff = anchorLoc[1] + anchor.height / 2 - popupH / 2
            popup.showAtLocation(
                anchor.rootView,
                Gravity.NO_GRAVITY,
                xOff.coerceAtLeast(0),
                yOff.coerceAtLeast(0)
            )
        }
    }

    private fun deleteTask(task: WallpaperTask) {
        cancelAlarm(task)
        tasks.remove(task)
        saveTasks()
        refreshUI()
        Toast.makeText(this, R.string.task_deleted, Toast.LENGTH_SHORT).show()
    }

    // ── 添加任务流程 (日子 → 时间 → 图片) ─────────────────

    private fun startAddTaskFlow() {
        tempDayType = DayType.EVERYDAY
        tempHour = 8
        tempMinute = 0
        tempImageUri = null
        tempDisplayName = ""
        tempWallpaperFlag = WallpaperManager.FLAG_SYSTEM
        showDayTypePicker()
    }

    private fun showDayTypePicker() {
        val dayTypes = DayType.entries.toTypedArray()
        val labels = dayTypes.map { type ->
            val base = getString(type.labelResId)
            if (type == DayType.ONCE) "$base（执行完毕自动删除）" else base
        }.toTypedArray()
        var selected = dayTypes.indexOf(tempDayType).coerceAtLeast(0)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.select_day_type)
            .setSingleChoiceItems(labels, selected) { _, which ->
                selected = which
            }
            .setPositiveButton(R.string.confirm) { _, _ ->
                tempDayType = dayTypes[selected]
                showTimePicker()
            }
            .show()
            .also { styleDialogButtons(it) }
    }

    private fun showTimePicker() {
        val dialog = TimePickerDialog(
            this,
            { _, hour, minute ->
                tempHour = hour
                tempMinute = minute
                showImagePicker()
            },
            tempHour, tempMinute, true
        )
        dialog.setButton(AlertDialog.BUTTON_NEUTRAL, "上一步") { _, _ ->
            showDayTypePicker()
        }
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.visibility = View.GONE
        // "上一步"与"确定"之间拉开间距
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.let { btn ->
            (btn.layoutParams as? ViewGroup.MarginLayoutParams)?.marginEnd =
                (48 * resources.displayMetrics.density).toInt()
        }
        styleDialogButtons(dialog)
    }

    private fun showImagePicker() {
        pickImageLauncher.launch("image/*")
    }

    private fun handleImagePicked(uri: Uri) {
        // 获取持久化读取权限
        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) { }

        tempImageUri = uri
        tempDisplayName = getDisplayName(uri)
        showWallpaperTypePicker()
    }

    /** 从 URI 提取显示文件名，多途径尝试确保正确 */
    private fun getDisplayName(uri: Uri): String {
        // 方式1：通过 ContentResolver 查询 DISPLAY_NAME（最可靠）
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) {
                    val displayName = cursor.getString(idx)
                    if (!displayName.isNullOrBlank()) return displayName.trim()
                }
            }
        }

        // 方式2：从 URI 路径最后一段提取（后备，URL 解码以处理特殊字符）
        uri.lastPathSegment?.let { seg ->
            val decoded = Uri.decode(seg)       // 解码 %20 等编码字符
            val cleaned = decoded.substringAfterLast('/').substringBefore('?').trim()
            // 过滤纯数字或空字符串（不太可能是有意义的文件名）
            if (cleaned.isNotBlank() && !cleaned.all { it.isDigit() }) {
                return cleaned
            }
        }

        return "unknown"
    }

    private fun showWallpaperTypePicker() {
        val types = arrayOf(
            getString(R.string.wallpaper_system),
            getString(R.string.wallpaper_lock),
            getString(R.string.wallpaper_both)
        )
        val flags = intArrayOf(
            WallpaperManager.FLAG_SYSTEM,
            WallpaperManager.FLAG_LOCK,
            WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
        )
        var selected = 0

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.wallpaper_type)
            .setSingleChoiceItems(types, selected) { _, which ->
                selected = which
            }
            .setPositiveButton(R.string.confirm) { _, _ ->
                tempWallpaperFlag = flags[selected]
                confirmAddTask()
            }
            .setNeutralButton("上一步") { _, _ ->
                pickImageLauncher.launch("image/*")
            }
            .show()
            .also { styleDialogButtons(it) }
    }

    private fun confirmAddTask() {
        val task = WallpaperTask(
            id = System.currentTimeMillis(),
            hour = tempHour,
            minute = tempMinute,
            imageUri = tempImageUri.toString(),
            displayName = tempDisplayName,
            wallpaperFlag = tempWallpaperFlag,
            dayType = tempDayType
        )
        addTask(task)
    }

    // ── UI 刷新 ───────────────────────────────────────────

    private fun refreshUI() {
        adapter.submitList(tasks)
        if (tasks.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            recyclerView.visibility = View.INVISIBLE
        } else {
            tvEmpty.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    // ── 设置对话框 ────────────────────────────────────────

    private fun showSettingsDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_settings, null)

        // ── 暂存变量：所有设置仅在对话框中预览，点确定后才生效 ──
        var tempDarkOn = prefs.getBoolean("dark_on", false)
        var tempDarkFollow = prefs.getBoolean("dark_follow_system", true)
        var tempThemeIndex = prefs.getInt("theme_color_index", 0)
        var tempHideRecents = prefs.getBoolean("hide_from_recents", false)

        // 夜间模式 —— 两个拨动式 Switch（互斥，仅更新暂存变量）
        val switchDarkOn = view.findViewById<Switch>(R.id.switch_dark_on)
        val switchDarkFollow = view.findViewById<Switch>(R.id.switch_dark_follow)
        switchDarkOn.isChecked = tempDarkOn
        switchDarkFollow.isChecked = tempDarkFollow

        // Switch 开关状态色：打开绿色(50%饱和)，关闭灰色
        val checkedGreen = desaturateColor(Color.parseColor("#4CAF50"))
        val uncheckedGray = desaturateColor(Color.parseColor("#BDBDBD"))
        val switchColorStateList = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf()
            ),
            intArrayOf(checkedGreen, uncheckedGray)
        )
        switchDarkOn.thumbTintList = switchColorStateList
        switchDarkOn.trackTintList = switchColorStateList
        switchDarkFollow.thumbTintList = switchColorStateList
        switchDarkFollow.trackTintList = switchColorStateList

        switchDarkOn.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                switchDarkFollow.isChecked = false
                tempDarkOn = true
                tempDarkFollow = false
            } else if (!switchDarkFollow.isChecked) {
                switchDarkFollow.isChecked = true
                tempDarkOn = false
                tempDarkFollow = true
            }
        }
        switchDarkFollow.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                switchDarkOn.isChecked = false
                tempDarkOn = false
                tempDarkFollow = true
            } else if (!switchDarkOn.isChecked) {
                switchDarkOn.isChecked = true
                tempDarkOn = true
                tempDarkFollow = false
            }
        }

        // 主题色：圆形按钮（仅预览）
        val containerThemeColors = view.findViewById<LinearLayout>(R.id.container_theme_colors)
        populateThemeButtons(containerThemeColors, tempThemeIndex) { idx ->
            tempThemeIndex = idx
        }

        // 隐藏最近任务（仅更新暂存变量）
        val switchHide = view.findViewById<Switch>(R.id.switch_hide_recents)
        switchHide.isChecked = tempHideRecents
        switchHide.thumbTintList = switchColorStateList
        switchHide.trackTintList = switchColorStateList
        switchHide.setOnCheckedChangeListener { _, isChecked ->
            tempHideRecents = isChecked
        }

        // 版本号和作者
        try {
            val pkgInfo = packageManager.getPackageInfo(packageName, 0)
            val verName = pkgInfo.versionName
            view.findViewById<TextView>(R.id.tv_version_info).text =
                getString(R.string.version_author_template, verName)
        } catch (_: Exception) {
            view.findViewById<TextView>(R.id.tv_version_info).text =
                getString(R.string.version_author_template, "?.?")
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(view)
            .show()

        // 确定按钮：与对话框 positive 按钮保持一致的绿色
        view.findViewById<MaterialButton>(R.id.btn_confirm).apply {
            backgroundTintList = ColorStateList.valueOf(
                desaturateColor(Color.parseColor("#4CAF50"))
            )
        }

        // 确定按钮：统一保存并应用所有设置
        view.findViewById<View>(R.id.btn_confirm).setOnClickListener {
            prefs.edit()
                .putBoolean("dark_on", tempDarkOn)
                .putBoolean("dark_follow_system", tempDarkFollow)
                .putInt("theme_color_index", tempThemeIndex)
                .putBoolean("hide_from_recents", tempHideRecents)
                .apply()

            currentThemeColorIndex = tempThemeIndex
            applyDarkMode()
            applyThemeColor()
            applyHideFromRecents(tempHideRecents)
            dialog.dismiss()
        }
    }

    /** 在容器中动态创建 9 个圆形主题色按钮（仅预览，不写 prefs） */
    private fun populateThemeButtons(
        container: LinearLayout,
        selectedIndex: Int,
        onSelected: (Int) -> Unit
    ) {
        container.removeAllViews()
        val density = resources.displayMetrics.density
        val btnSize = (44 * density).toInt()       // 圆形按钮大小 44dp
        val marginEnd = (10 * density).toInt()      // 按钮间距 10dp

        for (i in THEME_COLORS.indices) {
            val (colorStr, _) = THEME_COLORS[i]
            val color = desaturateColor(Color.parseColor(colorStr))
            val isSelected = (i == selectedIndex)

            val btn = MaterialButton(container.context).apply {
                layoutParams = LinearLayout.LayoutParams(btnSize, btnSize).apply {
                    this.marginEnd = marginEnd
                }
                text = ""                               // 无文字
                tag = i
                // 圆形样式
                cornerRadius = btnSize / 2
                strokeWidth = 0
                insetTop = 0
                insetBottom = 0
                iconSize = (20 * density).toInt()
                iconPadding = 0
                // 确保图标在无文字按钮中完全居中
                iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START

                if (isSelected) {
                    // 选中：颜色变暗 50%，中央白色对钩
                    setBackgroundColor(darkenColor(color, 0.5f))
                    icon = resources.getDrawable(R.drawable.ic_check, null)
                    iconTint = ColorStateList.valueOf(Color.WHITE)
                } else {
                    // 未选中：完整主题色，无图标
                    setBackgroundColor(color)
                    icon = null
                }

                setOnClickListener {
                    onSelected(i)
                    refreshThemeButtons(container, i)
                }
            }
            container.addView(btn)
        }
    }

    /** 刷新所有圆形按钮的选中状态 */
    private fun refreshThemeButtons(container: LinearLayout, selectedIndex: Int) {
        for (j in 0 until container.childCount) {
            val btn = container.getChildAt(j) as MaterialButton
            val idx = btn.tag as Int
            val colorStr = THEME_COLORS[idx].first
            val color = desaturateColor(Color.parseColor(colorStr))
            if (idx == selectedIndex) {
                btn.setBackgroundColor(darkenColor(color, 0.5f))
                btn.icon = resources.getDrawable(R.drawable.ic_check, null)
                btn.iconTint = ColorStateList.valueOf(Color.WHITE)
                btn.iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
            } else {
                btn.setBackgroundColor(color)
                btn.icon = null
            }
        }
    }

    // ── 夜间模式 ──────────────────────────────────────────

    private fun applyDarkMode() {
        val darkOn = prefs.getBoolean("dark_on", false)
        val darkFollow = prefs.getBoolean("dark_follow_system", true)
        val mode = when {
            darkFollow || (!darkOn && !darkFollow) -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            darkOn -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    // ── 主题色应用 ────────────────────────────────────────

    private fun applyThemeColor() {
        val index = prefs.getInt("theme_color_index", 0)
        currentThemeColorIndex = index
        val color = desaturateColor(Color.parseColor(THEME_COLORS[index].first))

        // Toolbar
        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)?.setBackgroundColor(color)

        // FAB：背景跟随主题色，图标始终白色
        val fab = findViewById<FloatingActionButton>(R.id.btn_add)
        fab?.backgroundTintList = ColorStateList.valueOf(color)
        fab?.imageTintList = ColorStateList.valueOf(Color.WHITE)

        // 状态栏：与顶栏颜色保持一致（沉浸式）
        window.statusBarColor = color
    }

    // ── 隐藏最近任务 ──────────────────────────────────────

    private fun applyHideFromRecents(hide: Boolean) {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val appTasks = am.appTasks
        for (at in appTasks) {
            at.setExcludeFromRecents(hide)
        }
    }

    // ── 对话框按钮样式 ──────────────────────────────────

    /** 统一风格对话框按钮：Neutral=上一步(橙) | Negative=取消(红) | Positive=确定(绿)，均为标准色 50% 饱和度。
     *  兼容 MaterialButton（MaterialAlertDialogBuilder）和普通 Button（TimePickerDialog）。 */
    private fun styleDialogButtons(dialog: Dialog) {
        val density = resources.displayMetrics.density
        val padH = (16 * density).toInt()
        val padV = (10 * density).toInt()
        val cornerRadiusPx = (24 * density).toInt()

        styleDialogButton(dialog.findViewById<Button>(android.R.id.button1),
            desaturateColor(Color.parseColor("#4CAF50")), Color.WHITE, cornerRadiusPx, padH, padV)
        styleDialogButton(dialog.findViewById<Button>(android.R.id.button2),
            desaturateColor(Color.parseColor("#F44336")), Color.WHITE, cornerRadiusPx, padH, padV)
        styleDialogButton(dialog.findViewById<Button>(android.R.id.button3),
            desaturateColor(Color.parseColor("#FF9800")), Color.DKGRAY, cornerRadiusPx, padH, padV)
    }

    /** 对单个按钮应用药丸样式，自动适配 MaterialButton 与普通 Button */
    private fun styleDialogButton(
        btn: Button?, bgColor: Int, textColor: Int,
        cornerRadiusPx: Int, padH: Int, padV: Int
    ) {
        btn ?: return
        if (btn is MaterialButton) {
            // MaterialButton：必须通过 backgroundTintList 着色，直接替换 background 会导致按钮消失
            btn.backgroundTintList = ColorStateList.valueOf(bgColor)
            btn.strokeWidth = 0
            btn.cornerRadius = cornerRadiusPx
        } else {
            // 普通 Button（如 TimePickerDialog）：使用 GradientDrawable
            val drawable = GradientDrawable().apply {
                setColor(bgColor)
                cornerRadius = cornerRadiusPx.toFloat()
            }
            btn.background = drawable
        }
        btn.setTextColor(textColor)
        btn.setPadding(padH, padV, padH, padV)
    }

    // ── 闹钟管理 ──────────────────────────────────────────

    private fun scheduleAllAlarms() {
        for (task in tasks) {
            if (task.dayType != DayType.ONCE) {
                scheduleAlarm(task)
            }
        }
    }

    private fun scheduleAlarm(task: WallpaperTask) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, WallpaperReceiver::class.java).apply {
            putExtra("task_id", task.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = getNextTriggerTime(task.hour, task.minute, task.dayType)
        val info = AlarmManager.AlarmClockInfo(triggerTime, null)
        alarmManager.setAlarmClock(info, pendingIntent)
    }

    private fun cancelAlarm(task: WallpaperTask) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, WallpaperReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    // ── 辅助方法 ──────────────────────────────────────────

    /** 计算距离 (hour:minute) 最近的下一次触发时间（毫秒），考虑 dayType */
    companion object {
        fun getNextTriggerTime(hour: Int, minute: Int, dayType: DayType = DayType.EVERYDAY): Long {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)

            // 如果今天已过，先推进到明天
            if (cal.timeInMillis <= System.currentTimeMillis()) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }

            when (dayType) {
                DayType.ONCE, DayType.EVERYDAY -> {
                    // 无需特殊处理
                }
                DayType.WEEKDAY -> {
                    while (!isWeekday(cal)) cal.add(Calendar.DAY_OF_YEAR, 1)
                }
                DayType.WEEKEND -> {
                    while (!isWeekend(cal)) cal.add(Calendar.DAY_OF_YEAR, 1)
                }
                DayType.WORKDAY -> {
                    while (!isWorkday(cal)) cal.add(Calendar.DAY_OF_YEAR, 1)
                }
                DayType.HOLIDAY -> {
                    while (!isHoliday(cal)) cal.add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            return cal.timeInMillis
        }

        fun isWeekday(cal: Calendar): Boolean {
            val dow = cal.get(Calendar.DAY_OF_WEEK)
            return dow in Calendar.MONDAY..Calendar.FRIDAY
        }

        fun isWeekend(cal: Calendar): Boolean {
            val dow = cal.get(Calendar.DAY_OF_WEEK)
            return dow == Calendar.SATURDAY || dow == Calendar.SUNDAY
        }

        fun isWorkday(cal: Calendar): Boolean {
            return isWeekday(cal) && !isHoliday(cal)
        }

        fun isHoliday(cal: Calendar): Boolean {
            val mmdd = String.format("%02d-%02d",
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH))
            return CHINESE_HOLIDAYS.contains(mmdd)
        }

        /** 使颜色变暗 */
        fun darkenColor(color: Int, factor: Float): Int {
            val a = Color.alpha(color)
            val r = (Color.red(color) * factor).toInt().coerceIn(0, 255)
            val g = (Color.green(color) * factor).toInt().coerceIn(0, 255)
            val b = (Color.blue(color) * factor).toInt().coerceIn(0, 255)
            return Color.argb(a, r, g, b)
        }

        /** 将颜色饱和度降低为原来的 50%，保持色相和明度不变 */
        fun desaturateColor(color: Int): Int {
            val hsv = FloatArray(3)
            Color.colorToHSV(color, hsv)
            hsv[1] *= 0.5f
            return Color.HSVToColor(Color.alpha(color), hsv)
        }
    }

    // ── 提取图片格式 (e.g. JPG / PNG / BMP / GIF) ───────────

    private fun getImageFormat(displayName: String): String {
        val dotIndex = displayName.lastIndexOf('.')
        if (dotIndex >= 0 && dotIndex < displayName.length - 1) {
            val ext = displayName.substring(dotIndex + 1)
            if (ext.length in 2..5 && ext.all { it.isLetterOrDigit() }) {
                return ext.uppercase()
            }
        }
        return "IMG"
    }

    // ── RecyclerView Adapter ──────────────────────────────

    inner class TaskAdapter(
        private val onDelete: (WallpaperTask, View) -> Unit
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private var displayItems: List<TaskListItem> = emptyList()
        private var highlightedHolder: TaskViewHolder? = null

        fun submitList(tasks: List<WallpaperTask>) {
            highlightedHolder?.setHighlighted(false)
            highlightedHolder = null
            displayItems = buildDisplayList(tasks)
            notifyDataSetChanged()
        }

        fun clearHighlight() {
            highlightedHolder?.setHighlighted(false)
            highlightedHolder = null
        }

        private fun buildDisplayList(tasks: List<WallpaperTask>): List<TaskListItem> {
            val result = mutableListOf<TaskListItem>()
            var lastFlag = -1
            for (task in tasks) {
                if (task.wallpaperFlag != lastFlag) {
                    result.add(TaskListItem.Header(task.wallpaperFlag))
                    lastFlag = task.wallpaperFlag
                }
                result.add(TaskListItem.Task(task))
            }
            return result
        }

        override fun getItemViewType(position: Int): Int {
            return when (displayItems[position]) {
                is TaskListItem.Header -> 0
                is TaskListItem.Task -> 1
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return when (viewType) {
                0 -> {
                    val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_section_header, parent, false)
                    HeaderViewHolder(view)
                }
                else -> {
                    val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_wallpaper_task, parent, false)
                    TaskViewHolder(view)
                }
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (val item = displayItems[position]) {
                is TaskListItem.Header -> (holder as HeaderViewHolder).bind(item.wallpaperFlag)
                is TaskListItem.Task -> (holder as TaskViewHolder).bind(item.task)
            }
        }

        override fun getItemCount() = displayItems.size

        // ── Header ViewHolder ──────────────────────────────

        inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val label: TextView = itemView.findViewById(R.id.tv_section_label)
            private val divider: View = itemView.findViewById(R.id.divider)

            fun bind(wallpaperFlag: Int) {
                // 与时间文字蓝色保持一致，不降低饱和度
                val outValue = android.util.TypedValue()
                itemView.context.theme.resolveAttribute(
                    android.R.attr.colorPrimary, outValue, true
                )
                val sectionColor = outValue.data
                label.text = itemView.context.getString(
                    when (wallpaperFlag) {
                        WallpaperManager.FLAG_SYSTEM -> R.string.wallpaper_system
                        WallpaperManager.FLAG_LOCK -> R.string.wallpaper_lock
                        else -> R.string.wallpaper_both
                    }
                )
                label.setTextColor(sectionColor)
                divider.setBackgroundColor(sectionColor)
            }
        }

        // ── Task ViewHolder ────────────────────────────────

        inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvTime: TextView = itemView.findViewById(R.id.tv_time)
            private val tvDayType: TextView = itemView.findViewById(R.id.tv_day_type)
            private val tvImageName: TextView = itemView.findViewById(R.id.tv_image_name)
            private val chipType: Chip = itemView.findViewById(R.id.chip_type)
            private val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete)
            private val ivThumbnail: ImageView = itemView.findViewById(R.id.iv_thumbnail)
            private val density = itemView.context.resources.displayMetrics.density
            private val trashColor = desaturateColor(Color.parseColor("#F44336"))

            // 预创建背景，避免每次高亮切换时分配对象
            private val ringBg: GradientDrawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setStroke((2f * density).toInt(), trashColor)
                setColor(Color.TRANSPARENT)
            }

            private val defaultBg: android.graphics.drawable.Drawable = run {
                val outValue = android.util.TypedValue()
                itemView.context.theme.resolveAttribute(
                    android.R.attr.selectableItemBackgroundBorderless, outValue, true
                )
                itemView.context.resources.getDrawable(outValue.resourceId, itemView.context.theme)
            }

            fun bind(task: WallpaperTask) {
                tvTime.text = String.format("%02d:%02d", task.hour, task.minute)
                tvDayType.text = itemView.context.getString(task.dayType.labelResId)
                tvImageName.text = getImageFormat(task.displayName)

                chipType.text = when {
                    task.wallpaperFlag == WallpaperManager.FLAG_SYSTEM -> itemView.context.getString(R.string.wallpaper_system)
                    task.wallpaperFlag == WallpaperManager.FLAG_LOCK -> itemView.context.getString(R.string.wallpaper_lock)
                    else -> itemView.context.getString(R.string.wallpaper_both)
                }

                // 加载壁纸缩略图
                try {
                    val uri = Uri.parse(task.imageUri)
                    val inputStream = itemView.context.contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        val opts = BitmapFactory.Options().apply {
                            inSampleSize = 4  // 缩小4倍作为缩略图
                        }
                        val bitmap = BitmapFactory.decodeStream(inputStream, null, opts)
                        inputStream.close()
                        ivThumbnail.setImageBitmap(bitmap)
                    }
                } catch (_: Exception) {
                    ivThumbnail.setImageResource(android.R.drawable.ic_menu_gallery)
                }

                btnDelete.imageTintList = ColorStateList.valueOf(trashColor)
                btnDelete.background = defaultBg

                btnDelete.setOnClickListener {
                    highlightedHolder?.setHighlighted(false)
                    highlightedHolder = this
                    setHighlighted(true)
                    onDelete(task, btnDelete)
                }
            }

            /** 直接交换预创建的背景，零分配、零延迟 */
            fun setHighlighted(highlighted: Boolean) {
                btnDelete.background = if (highlighted) ringBg else defaultBg
            }
        }

    }
}
