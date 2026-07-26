package com.example.agent.services

import android.content.Context
import android.provider.CallLog
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidCallLogService @Inject constructor(
    @ApplicationContext private val context: Context
) : CallLogService {
    override fun getCallLogs(): List<CallLogEntry> {
        val logs = mutableListOf<CallLogEntry>()
        val cursor = context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME, CallLog.Calls.TYPE, CallLog.Calls.DATE, CallLog.Calls.DURATION),
            null, null, "${CallLog.Calls.DATE} DESC LIMIT 20"
        )

        cursor?.use {
            val numIdx = it.getColumnIndex(CallLog.Calls.NUMBER)
            val nameIdx = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
            val typeIdx = it.getColumnIndex(CallLog.Calls.TYPE)
            val dateIdx = it.getColumnIndex(CallLog.Calls.DATE)
            val durIdx = it.getColumnIndex(CallLog.Calls.DURATION)

            while (it.moveToNext()) {
                logs.add(CallLogEntry(
                    number = it.getString(numIdx) ?: "unknown",
                    name = it.getString(nameIdx),
                    type = it.getInt(typeIdx),
                    date = it.getLong(dateIdx),
                    duration = it.getLong(durIdx)
                ))
            }
        }
        return logs
    }
}
