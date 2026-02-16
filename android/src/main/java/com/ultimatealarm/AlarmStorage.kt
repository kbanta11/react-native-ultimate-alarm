package com.ultimatealarm

import android.content.Context
import android.util.Log
import org.json.JSONObject
import org.json.JSONArray

class AlarmStorage(private val context: Context) {

    companion object {
        private const val TAG = "AlarmStorage"
        private const val PREFS_NAME = "UltimateAlarms"
        private const val ALARMS_KEY = "alarms"
        private const val LAUNCH_PAYLOAD_KEY = "launchPayload"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Save an alarm configuration
     */
    fun saveAlarm(alarmId: String, config: Map<String, Any?>) {
        try {
            val alarmsJson = getAllAlarmsJson()
            val alarmJson = mapToJson(config)
            alarmsJson.put(alarmId, alarmJson)

            prefs.edit()
                .putString(ALARMS_KEY, alarmsJson.toString())
                .apply()

            Log.d(TAG, "Saved alarm: $alarmId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save alarm: ${e.message}")
        }
    }

    /**
     * Get an alarm configuration by ID
     */
    fun getAlarm(alarmId: String): Map<String, Any?>? {
        try {
            val alarmsJson = getAllAlarmsJson()
            if (!alarmsJson.has(alarmId)) {
                return null
            }

            val alarmJson = alarmsJson.getJSONObject(alarmId)
            return jsonToMap(alarmJson)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get alarm: ${e.message}")
            return null
        }
    }

    /**
     * Delete an alarm by ID
     */
    fun deleteAlarm(alarmId: String) {
        try {
            val alarmsJson = getAllAlarmsJson()
            alarmsJson.remove(alarmId)

            prefs.edit()
                .putString(ALARMS_KEY, alarmsJson.toString())
                .apply()

            Log.d(TAG, "Deleted alarm: $alarmId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete alarm: ${e.message}")
        }
    }

    /**
     * Get all alarm IDs
     */
    fun getAllAlarms(): List<String> {
        try {
            val alarmsJson = getAllAlarmsJson()
            val alarmIds = mutableListOf<String>()
            val iterator = alarmsJson.keys()
            while (iterator.hasNext()) {
                alarmIds.add(iterator.next())
            }
            return alarmIds
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get all alarms: ${e.message}")
            return emptyList()
        }
    }

    /**
     * Get all alarm configurations
     */
    fun getAllAlarmConfigs(): List<Map<String, Any?>> {
        try {
            val alarmsJson = getAllAlarmsJson()
            val configs = mutableListOf<Map<String, Any?>>()
            val iterator = alarmsJson.keys()
            while (iterator.hasNext()) {
                val key = iterator.next()
                val alarmJson = alarmsJson.getJSONObject(key)
                configs.add(jsonToMap(alarmJson))
            }
            return configs
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get all alarm configs: ${e.message}")
            return emptyList()
        }
    }

    /**
     * Check if an alarm exists
     */
    fun hasAlarm(alarmId: String): Boolean {
        try {
            val alarmsJson = getAllAlarmsJson()
            return alarmsJson.has(alarmId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check alarm: ${e.message}")
            return false
        }
    }

    /**
     * Clear all alarms
     */
    fun clearAllAlarms() {
        try {
            prefs.edit()
                .remove(ALARMS_KEY)
                .apply()

            Log.d(TAG, "Cleared all alarms")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear alarms: ${e.message}")
        }
    }

    /**
     * Save launch payload (for app launch detection)
     */
    fun saveLaunchPayload(payload: Map<String, Any?>) {
        try {
            val payloadJson = mapToJson(payload)
            prefs.edit()
                .putString(LAUNCH_PAYLOAD_KEY, payloadJson.toString())
                .apply()

            Log.d(TAG, "Saved launch payload")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save launch payload: ${e.message}")
        }
    }

    /**
     * Get launch payload
     */
    fun getLaunchPayload(): Map<String, Any?>? {
        try {
            val payloadString = prefs.getString(LAUNCH_PAYLOAD_KEY, null) ?: return null
            val payloadJson = JSONObject(payloadString)
            return jsonToMap(payloadJson)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get launch payload: ${e.message}")
            return null
        }
    }

    /**
     * Clear launch payload
     */
    fun clearLaunchPayload() {
        try {
            prefs.edit()
                .remove(LAUNCH_PAYLOAD_KEY)
                .apply()

            Log.d(TAG, "Cleared launch payload")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear launch payload: ${e.message}")
        }
    }

    // Helper methods

    private fun getAllAlarmsJson(): JSONObject {
        val alarmsString = prefs.getString(ALARMS_KEY, null)
        return if (alarmsString != null) {
            JSONObject(alarmsString)
        } else {
            JSONObject()
        }
    }

    private fun mapToJson(map: Map<String, Any?>): JSONObject {
        val json = JSONObject()
        for ((key, value) in map) {
            when (value) {
                is Map<*, *> -> json.put(key, mapToJson(value as Map<String, Any?>))
                is List<*> -> json.put(key, listToJsonArray(value))
                else -> json.put(key, value)
            }
        }
        return json
    }

    private fun listToJsonArray(list: List<*>): JSONArray {
        val array = JSONArray()
        for (item in list) {
            when (item) {
                is Map<*, *> -> array.put(mapToJson(item as Map<String, Any?>))
                is List<*> -> array.put(listToJsonArray(item))
                else -> array.put(item)
            }
        }
        return array
    }

    private fun jsonToMap(json: JSONObject): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        val iterator = json.keys()
        while (iterator.hasNext()) {
            val key = iterator.next()
            val value = json.get(key)
            map[key] = when (value) {
                is JSONObject -> jsonToMap(value)
                is JSONArray -> jsonArrayToList(value)
                JSONObject.NULL -> null
                else -> value
            }
        }
        return map
    }

    private fun jsonArrayToList(array: JSONArray): List<Any?> {
        val list = mutableListOf<Any?>()
        for (i in 0 until array.length()) {
            val value = array.get(i)
            list.add(when (value) {
                is JSONObject -> jsonToMap(value)
                is JSONArray -> jsonArrayToList(value)
                JSONObject.NULL -> null
                else -> value
            })
        }
        return list
    }
}
