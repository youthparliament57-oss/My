import sys

content = """package com.example.agent.services

import android.content.Context
import android.provider.ContactsContract
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidContactService @Inject constructor(
    @ApplicationContext private val context: Context
) : ContactService {

    override fun findContactByName(name: String): List<ContactInfo> {
        return getAllContacts().filter { it.name.contains(name, ignoreCase = true) }
    }

    override fun getAllContacts(): List<ContactInfo> {
        val contacts = mutableListOf<ContactInfo>()
        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME
        )
        val selection = "${ContactsContract.Contacts.HAS_PHONE_NUMBER} = 1"
        
        val cursor = context.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            projection, selection, null, null
        )
        
        cursor?.use {
            val idIdx = it.getColumnIndex(ContactsContract.Contacts._ID)
            val nameIdx = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            
            while (it.moveToNext()) {
                val id = it.getString(idIdx)
                val name = it.getString(nameIdx) ?: "Unknown"
                
                // Fetch phone numbers for this contact
                val phoneNumbers = mutableListOf<String>()
                val phoneCursor = context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                    arrayOf(id),
                    null
                )
                
                phoneCursor?.use { pc ->
                    val numIdx = pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    while (pc.moveToNext()) {
                        phoneNumbers.add(pc.getString(numIdx))
                    }
                }
                
                contacts.add(ContactInfo(id, name, phoneNumbers))
            }
        }
        return contacts
    }
}
"""
open("app/src/main/java/com/example/agent/services/AndroidContactService.kt", "w").write(content)
print("Patched AndroidContactService.kt")
