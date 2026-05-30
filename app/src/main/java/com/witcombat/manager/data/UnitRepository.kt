package com.witcombat.manager.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.witcombat.manager.domain.GameUnit
import java.io.File

class UnitRepository(private val context: Context) {

    private val gson = Gson()
    private val dataFile: File
        get() = File(context.filesDir, "units.json")

    fun loadUnits(): List<GameUnit> {
        if (!dataFile.exists()) return emptyList()
        return try {
            val json = dataFile.readText()
            val type = object : TypeToken<List<Map<String, Any?>>>() {}.type
            val list: List<Map<String, Any?>> = gson.fromJson(json, type)
            list.map { GameUnit.fromMap(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveUnits(units: List<GameUnit>) {
        try {
            val list = units.map { it.toMap() }
            dataFile.writeText(gson.toJson(list))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
