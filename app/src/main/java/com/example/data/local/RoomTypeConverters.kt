package com.example.data.local

import androidx.room.TypeConverter
import com.example.cognitive.models.ReasoningStep
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class RoomTypeConverters {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        val adapter = moshi.adapter<List<String>>(Types.newParameterizedType(List::class.java, String::class.java))
        return adapter.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val adapter = moshi.adapter<List<String>>(Types.newParameterizedType(List::class.java, String::class.java))
        return adapter.fromJson(value) ?: emptyList()
    }

    @TypeConverter
    fun fromReasoningStepList(value: List<ReasoningStep>): String {
        val adapter = moshi.adapter<List<ReasoningStep>>(Types.newParameterizedType(List::class.java, ReasoningStep::class.java))
        return adapter.toJson(value)
    }

    @TypeConverter
    fun toReasoningStepList(value: String): List<ReasoningStep> {
        val adapter = moshi.adapter<List<ReasoningStep>>(Types.newParameterizedType(List::class.java, ReasoningStep::class.java))
        return adapter.fromJson(value) ?: emptyList()
    }
}
