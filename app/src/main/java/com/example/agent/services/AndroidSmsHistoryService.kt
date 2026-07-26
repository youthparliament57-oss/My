package com.example.agent.services

import android.content.Context
import android.provider.Telephony
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidSmsHistoryService @Inject constructor(
    @ApplicationContext private val context: Context
) : SmsHistoryService {
    override fun getSmsHistory(): List<SmsMessage> {
        val messages = mutableListOf<SmsMessage>()
        val cursor = context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.TYPE),
            null, null, "${Telephony.Sms.DATE} DESC LIMIT 50"
        )
        
        cursor?.use {
            val addrIdx = it.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIdx = it.getColumnIndex(Telephony.Sms.BODY)
            val dateIdx = it.getColumnIndex(Telephony.Sms.DATE)
            val typeIdx = it.getColumnIndex(Telephony.Sms.TYPE)
            
            while (it.moveToNext()) {
                messages.add(SmsMessage(
                    address = it.getString(addrIdx) ?: "unknown",
                    body = it.getString(bodyIdx) ?: "",
                    date = it.getLong(dateIdx),
                    type = it.getInt(typeIdx)
                ))
            }
        }
        return messages
    }
}
