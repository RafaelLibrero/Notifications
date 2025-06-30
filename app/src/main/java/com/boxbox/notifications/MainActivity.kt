package com.boxbox.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.boxbox.notifications.databinding.ActivityMainBinding
import java.util.Calendar
import androidx.core.net.toUri

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnStartService.setOnClickListener {
            val intent = Intent(this, MyForegroundService::class.java)
            startForegroundService(intent)
        }


        binding.btnStopService.setOnClickListener {
            val intent = Intent(this, MyForegroundService::class.java)
            stopService(intent)
        }


        createNotificationChannel()

        binding.btnSetReminder.setOnClickListener {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.YEAR, binding.datePicker.year)
                set(Calendar.MONTH, binding.datePicker.month)
                set(Calendar.DAY_OF_MONTH, binding.datePicker.dayOfMonth)
                set(Calendar.HOUR_OF_DAY, binding.timePicker.hour)
                set(Calendar.MINUTE, binding.timePicker.minute)
                set(Calendar.SECOND, 0)
            }

            val message = binding.etMessage.text.toString()
            val repeat = binding.cbRepeat.isChecked

            if (!checkExactAlarmPermission()) {
                requestExactAlarmPermission()
                return@setOnClickListener
            }

            val now = System.currentTimeMillis()
            if (calendar.timeInMillis <= now) {
                Toast.makeText(this, "Selecciona una fecha y hora futura", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            scheduleNotification(calendar.timeInMillis, message, repeat)
            Toast.makeText(this, "Recordatorio programado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun scheduleNotification(timeInMillis: Long, message: String, repeat: Boolean) {
        val intent = Intent(this, NotificationReceiver::class.java).apply {
            putExtra("message", message)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            timeInMillis.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager

        if (repeat) {
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                timeInMillis,
                pendingIntent
            )
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "reminder_channel",
            "Recordatorios",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Canal para recordatorios"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun checkExactAlarmPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !checkExactAlarmPermission()) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = "package:$packageName".toUri()
            }
            startActivity(intent)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1002)
            }
        }
    }
}