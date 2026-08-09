package jp.awt.clock.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class AlarmStore(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE alarms (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                hour INTEGER NOT NULL,
                minute INTEGER NOT NULL,
                label TEXT NOT NULL DEFAULT '',
                enabled INTEGER NOT NULL DEFAULT 1,
                repeat_mask INTEGER NOT NULL DEFAULT 0,
                gradual_volume INTEGER NOT NULL DEFAULT 1,
                vibrate INTEGER NOT NULL DEFAULT 1
            )
            """.trimIndent(),
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun all(): List<Alarm> = readableDatabase.query(
        "alarms",
        COLUMNS,
        null,
        null,
        null,
        null,
        "hour ASC, minute ASC, id ASC",
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                add(
                    Alarm(
                        id = cursor.getLong(0),
                        hour = cursor.getInt(1),
                        minute = cursor.getInt(2),
                        label = cursor.getString(3),
                        enabled = cursor.getInt(4) == 1,
                        repeatMask = cursor.getInt(5),
                        gradualVolume = cursor.getInt(6) == 1,
                        vibrate = cursor.getInt(7) == 1,
                    ),
                )
            }
        }
    }

    fun get(id: Long): Alarm? = readableDatabase.query(
        "alarms",
        COLUMNS,
        "id = ?",
        arrayOf(id.toString()),
        null,
        null,
        null,
        "1",
    ).use { cursor ->
        if (!cursor.moveToFirst()) return@use null
        Alarm(
            id = cursor.getLong(0),
            hour = cursor.getInt(1),
            minute = cursor.getInt(2),
            label = cursor.getString(3),
            enabled = cursor.getInt(4) == 1,
            repeatMask = cursor.getInt(5),
            gradualVolume = cursor.getInt(6) == 1,
            vibrate = cursor.getInt(7) == 1,
        )
    }

    fun save(alarm: Alarm): Alarm {
        val values = ContentValues().apply {
            put("hour", alarm.hour)
            put("minute", alarm.minute)
            put("label", alarm.label)
            put("enabled", if (alarm.enabled) 1 else 0)
            put("repeat_mask", alarm.repeatMask)
            put("gradual_volume", if (alarm.gradualVolume) 1 else 0)
            put("vibrate", if (alarm.vibrate) 1 else 0)
        }
        return if (alarm.id == 0L) {
            alarm.copy(id = writableDatabase.insertOrThrow("alarms", null, values))
        } else {
            writableDatabase.update("alarms", values, "id = ?", arrayOf(alarm.id.toString()))
            alarm
        }
    }

    fun setEnabled(id: Long, enabled: Boolean) {
        val values = ContentValues().apply { put("enabled", if (enabled) 1 else 0) }
        writableDatabase.update("alarms", values, "id = ?", arrayOf(id.toString()))
    }

    fun delete(id: Long) {
        writableDatabase.delete("alarms", "id = ?", arrayOf(id.toString()))
    }

    private companion object {
        const val DATABASE_NAME = "alarms.db"
        const val DATABASE_VERSION = 1
        val COLUMNS = arrayOf(
            "id",
            "hour",
            "minute",
            "label",
            "enabled",
            "repeat_mask",
            "gradual_volume",
            "vibrate",
        )
    }
}

