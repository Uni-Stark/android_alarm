package kr.uni.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import java.util.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initOnOffButton()
        initChangeAlarmTimeButton()

        //Step 1 Data 가져오기
        val model = fetchDataFromSharedPreferences()
        renderView(model)

    }

    private fun initOnOffButton() {
        val onOffButton = findViewById<Button>(R.id.onOffButton)
        onOffButton.setOnClickListener {
            // 데이터를 확인을 한다.
            val model = it.tag as? AlarmDisplayModel ?: return@setOnClickListener
            val newModel =
                saveAlarmModel(hour = model.hour, minute = model.minute, onOff = model.onOff.not())
            renderView(newModel)

            if (newModel.onOff) {
                //켜진거임 ->알람 등록

                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, newModel.hour)
                    set(Calendar.MINUTE, newModel.minute)

                    if (before(Calendar.getInstance())) {
                        add(Calendar.DATE, 1)
                    }
                }

                val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(this, AlarmReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    this, BROADCAST_ALARM_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT
                )

                alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
            } else {
                cancelAlarm()
            }


            // 데이터를 저장한다.

        }
    }

    private fun initChangeAlarmTimeButton() {
        val changeAlarmButton = findViewById<Button>(R.id.changeAlarmTimeButton)
        changeAlarmButton.setOnClickListener {

            val calendar = Calendar.getInstance()
            TimePickerDialog(
                this@MainActivity, { picker, hour, minute ->

                    val model = saveAlarmModel(hour, minute, false)
                    renderView(model)
                    cancelAlarm()


                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false
            ).show()

        }
    }

    private fun saveAlarmModel(hour: Int, minute: Int, onOff: Boolean): AlarmDisplayModel {
        val model = AlarmDisplayModel(hour = hour, minute = minute, onOff = onOff)

        val sharedPreference = getSharedPreferences(PREF_TIME, Context.MODE_PRIVATE)
        with(sharedPreference.edit()) {
            putString(PREF_ALARM, model.makeDataForDB())
            putBoolean(PREF_ONOFF, model.onOff)
            commit()
        }

        return model
    }

    private fun fetchDataFromSharedPreferences(): AlarmDisplayModel {
        val sharedPreference = getSharedPreferences(PREF_TIME, Context.MODE_PRIVATE)
        val timeDBValue = sharedPreference.getString(PREF_ALARM, "9:30") ?: "9:30"
        val onOffDBValue = sharedPreference.getBoolean(PREF_ONOFF, false)
        val alarmData = timeDBValue.split(":")

        val alarmModel = AlarmDisplayModel(
            hour = alarmData[0].toInt(),
            minute = alarmData[1].toInt(),
            onOff = onOffDBValue
        )

        //예외 처리
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            BROADCAST_ALARM_REQUEST_CODE,
            Intent(this, AlarmReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE // NO CREATE는 있으면 있는거고 없으면 안만든다라는 기능
        )

        if ((pendingIntent == null) and alarmModel.onOff) {
            //알람은 꺼져이쓴데, 데이터는 켜져있는 경우
            alarmModel.onOff = false
        } else if ((pendingIntent != null) and alarmModel.onOff.not()) {
            pendingIntent.cancel() // 알람은 켜져 있는데 데이터는 꺼져있는 경우 알람 취소
        }

        return alarmModel
    }

    private fun renderView(model: AlarmDisplayModel) {
        findViewById<TextView>(R.id.ampmTextView).apply {
            text = model.ampmText
        }
        findViewById<TextView>(R.id.timeTextView).apply {
            text = model.timeText
        }
        findViewById<Button>(R.id.onOffButton).apply {
            text = model.onOffText
            tag = model
        }
    }

    private fun cancelAlarm() {
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            BROADCAST_ALARM_REQUEST_CODE,
            Intent(this, AlarmReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE // NO CREATE는 있으면 있는거고 없으면 안만든다라는 기능
        )
        pendingIntent?.cancel()
    }

    companion object {
        const val PREF_ALARM = "alarm"
        const val PREF_ONOFF = "onOff"
        const val PREF_TIME = "time"
        const val BROADCAST_ALARM_REQUEST_CODE = 1000
    }
}