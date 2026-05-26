package com.CWP.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import org.json.JSONArray

/**
 * 开机广播接收器：设备重启后重新注册所有闹钟。
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val prefs = context.getSharedPreferences("cwp_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("tasks_json", "[]") ?: return
        val arr = JSONArray(json)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val task = WallpaperTask(
                id = obj.getLong("id"),
                hour = obj.getInt("hour"),
                minute = obj.getInt("minute"),
                imageUri = obj.getString("uri"),
                displayName = obj.getString("name"),
                wallpaperFlag = obj.optInt("flag", android.app.WallpaperManager.FLAG_SYSTEM),
                dayType = DayType.fromKey(obj.optString("dayType", "EVERYDAY"))
            )

            // 仅一次类型不重新调度
            if (task.dayType == DayType.ONCE) continue

            val alarmIntent = Intent(context, WallpaperReceiver::class.java).apply {
                putExtra("task_id", task.id)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                task.id.toInt(),
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val triggerTime = MainActivity.getNextTriggerTime(task.hour, task.minute, task.dayType)
            val info = AlarmManager.AlarmClockInfo(triggerTime, null)
            alarmManager.setAlarmClock(info, pendingIntent)
        }
    }
}
