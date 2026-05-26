package com.CWP.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.net.Uri
import org.json.JSONArray

/**
 * 接收 AlarmManager 闹钟广播，执行壁纸更换。
 * 更换完成后调度下一次闹钟（仅一次类型不再调度，并自动删除）。
 */
class WallpaperReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra("task_id", -1)
        if (taskId == -1L) return

        val prefs = context.getSharedPreferences("cwp_prefs", Context.MODE_PRIVATE)
        val task = findTask(prefs, taskId) ?: return

        // 更换壁纸
        try {
            setWallpaper(context, task.imageUri, task.wallpaperFlag)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 仅一次类型：运行后从存储中自动删除
        if (task.dayType == DayType.ONCE) {
            removeTaskFromPrefs(prefs, taskId)
        } else {
            rescheduleAlarm(context, task)
        }
    }

    private fun findTask(prefs: SharedPreferences, taskId: Long): WallpaperTask? {
        val json = prefs.getString("tasks_json", "[]") ?: return null
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            if (obj.getLong("id") == taskId) {
                return WallpaperTask(
                    id = obj.getLong("id"),
                    hour = obj.getInt("hour"),
                    minute = obj.getInt("minute"),
                    imageUri = obj.getString("uri"),
                    displayName = obj.getString("name"),
                    wallpaperFlag = obj.optInt("flag", WallpaperManager.FLAG_SYSTEM),
                    dayType = DayType.fromKey(obj.optString("dayType", "EVERYDAY"))
                )
            }
        }
        return null
    }

    /** 从 SharedPreferences 中删除指定任务 */
    private fun removeTaskFromPrefs(prefs: SharedPreferences, taskId: Long) {
        val json = prefs.getString("tasks_json", "[]") ?: "[]"
        val arr = JSONArray(json)
        val newArr = JSONArray()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            if (obj.getLong("id") != taskId) {
                newArr.put(obj)
            }
        }
        prefs.edit().putString("tasks_json", newArr.toString()).apply()
    }

    private fun setWallpaper(context: Context, uriString: String, flag: Int) {
        val uri = Uri.parse(uriString)
        val inputStream = context.contentResolver.openInputStream(uri) ?: return
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        val wm = WallpaperManager.getInstance(context)

        // 桌面壁纸
        if (flag and WallpaperManager.FLAG_SYSTEM != 0) {
            wm.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
        }

        // 锁屏壁纸（需要重新解码，因为 setBitmap 可能回收 bitmap）
        if (flag and WallpaperManager.FLAG_LOCK != 0) {
            val inputStream2 = context.contentResolver.openInputStream(uri) ?: return
            val bitmap2 = BitmapFactory.decodeStream(inputStream2)
            inputStream2.close()
            wm.setBitmap(bitmap2, null, true, WallpaperManager.FLAG_LOCK)
        }
    }

    private fun rescheduleAlarm(context: Context, task: WallpaperTask) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WallpaperReceiver::class.java).apply {
            putExtra("task_id", task.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = MainActivity.getNextTriggerTime(task.hour, task.minute, task.dayType)
        val info = AlarmManager.AlarmClockInfo(triggerTime, null)
        alarmManager.setAlarmClock(info, pendingIntent)
    }
}
