import sys
import os

content = """package com.example.brain.memory

import androidx.room.TypeConverter
import org.json.JSONArray
import org.json.JSONObject
import com.example.cognitive.models.ReasoningStep

class MemoryTypeConverters {
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        if (value == null) return null
        val array = JSONArray()
        for (item in value) {
            array.put(item)
        }
        return array.toString()
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        if (value == null) return null
        val list = mutableListOf<String>()
        val array = JSONArray(value)
        for (i in 0 until array.length()) {
            list.add(array.getString(i))
        }
        return list
    }

    @TypeConverter
    fun fromReasoningStepList(value: List<ReasoningStep>?): String? {
        if (value == null) return null
        val array = JSONArray()
        for (item in value) {
            val obj = JSONObject()
            obj.put("iteration", item.iteration)
            obj.put("thought", item.thought)
            obj.put("toolName", item.toolName)
            obj.put("toolArgs", item.toolArgs)
            obj.put("observation", item.observation)
            array.put(obj)
        }
        return array.toString()
    }

    @TypeConverter
    fun toReasoningStepList(value: String?): List<ReasoningStep>? {
        if (value == null) return null
        val list = mutableListOf<ReasoningStep>()
        val array = JSONArray(value)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(ReasoningStep(
                iteration = obj.getInt("iteration"),
                thought = obj.getString("thought"),
                toolName = if (obj.has("toolName") && !obj.isNull("toolName")) obj.getString("toolName") else null,
                toolArgs = if (obj.has("toolArgs") && !obj.isNull("toolArgs")) obj.getString("toolArgs") else null,
                observation = if (obj.has("observation") && !obj.isNull("observation")) obj.getString("observation") else null
            ))
        }
        return list
    }
}"""
open("app/src/main/java/com/example/brain/memory/MemoryTypeConverters.kt", "w").write(content)

db_path = "app/src/main/java/com/example/data/local/NousDatabase.kt"
db_content = open(db_path).read()
if "@TypeConverters" not in db_content:
    db_content = db_content.replace("@Database(", "@androidx.room.TypeConverters(com.example.brain.memory.MemoryTypeConverters::class)\n@Database(")
    open(db_path, "w").write(db_content)
    print("Database patched")
