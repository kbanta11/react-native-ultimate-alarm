package com.ultimatealarm

import android.content.Context
import android.util.Log
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableMap
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
    fun saveAlarm(alarmId: String, config: ReadableMap) {
        try {
            val alarmsJson = getAllAlarmsJson()
            val alarmJson = readableMapToJson(config)
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
    fun getAlarm(alarmId: String): ReadableMap? {
        try {
            val alarmsJson = getAllAlarmsJson()
            if (!alarmsJson.has(alarmId)) {
                return null
            }

            val alarmJson = alarmsJson.getJSONObject(alarmId)
            return jsonToWritableMap(alarmJson)
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
            if (alarmsJson.has(alarmId)) {
                alarmsJson.remove(alarmId)
                prefs.edit()
                    .putString(ALARMS_KEY, alarmsJson.toString())
                    .apply()
                Log.d(TAG, "Deleted alarm: $alarmId")
            }
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
            val ids = mutableListOf<String>()
            val keys = alarmsJson.keys()
            while (keys.hasNext()) {
                ids.add(keys.next())
            }
            return ids
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get all alarms: ${e.message}")
            return emptyList()
        }
    }

    /**
     * Get all alarm configurations
     */
    fun getAllAlarmConfigs(): List<ReadableMap> {
        try {
            val alarmsJson = getAllAlarmsJson()
            val configs = mutableListOf<ReadableMap>()
            val keys = alarmsJson.keys()

            while (keys.hasNext()) {
                val key = keys.next()
                val alarmJson = alarmsJson.getJSONObject(key)
                configs.add(jsonToWritableMap(alarmJson))
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
            Log.e(TAG, "Failed to clear all alarms: ${e.message}")
        }
    }

    /**
     * Save launch payload (for app launch detection)
     */
    fun saveLaunchPayload(payload: Map<String, Any>) {
        try {
            val json = JSONObject(payload)
            prefs.edit()
                .putString(LAUNCH_PAYLOAD_KEY, json.toString())
                .apply()
            Log.d(TAG, "Saved launch payload")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save launch payload: ${e.message}")
        }
    }

    /**
     * Get launch payload
     */
    fun getLaunchPayload(): ReadableMap? {
        try {
            val jsonString = prefs.getString(LAUNCH_PAYLOAD_KEY, null) ?: return null
            val json = JSONObject(jsonString)
            return jsonToWritableMap(json)
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

    // Helper methods for JSON conversion

    private fun getAllAlarmsJson(): JSONObject {
        val jsonString = prefs.getString(ALARMS_KEY, null)
        return if (jsonString != null) {
            try {
                JSONObject(jsonString)
            } catch (e: Exception) {
                JSONObject()
            }
        } else {
            JSONObject()
        }
    }

    private fun readableMapToJson(readableMap: ReadableMap): JSONObject {
        val json = JSONObject()
        val iterator = readableMap.keySetIterator()

        while (iterator.hasNextKey()) {
            val key = iterator.nextKey()
            when (readableMap.getType(key)) {
                com.facebook.react.bridge.ReadableType.Null -> json.put(key, null)
                com.facebook.react.bridge.ReadableType.Boolean -> json.put(key, readableMap.getBoolean(key))
                com.facebook.react.bridge.ReadableType.Number -> json.put(key, readableMap.getDouble(key))
                com.facebook.react.bridge.ReadableType.String -> json.put(key, readableMap.getString(key))
                com.facebook.react.bridge.ReadableType.Map -> {
                    val nestedMap = readableMap.getMap(key)
                    if (nestedMap != null) {
                        json.put(key, readableMapToJson(nestedMap))
                    }
                }
                com.facebook.react.bridge.ReadableType.Array -> {
                    val array = readableMap.getArray(key)
                    if (array != null) {
                        json.put(key, readableArrayToJson(array))
                    }
                }
            }
        }

        return json
    }

    private fun readableArrayToJson(readableArray: com.facebook.react.bridge.ReadableArray): JSONArray {
        val json = JSONArray()

        for (i in 0 until readableArray.size()) {
            when (readableArray.getType(i)) {
                com.facebook.react.bridge.ReadableType.Null -> json.put(null)
                com.facebook.react.bridge.ReadableType.Boolean -> json.put(readableArray.getBoolean(i))
                com.facebook.react.bridge.ReadableType.Number -> json.put(readableArray.getDouble(i))
                com.facebook.react.bridge.ReadableType.String -> json.put(readableArray.getString(i))
                com.facebook.react.bridge.ReadableType.Map -> {
                    val map = readableArray.getMap(i)
                    if (map != null) {
                        json.put(readableMapToJson(map))
                    }
                }
                com.facebook.react.bridge.ReadableType.Array -> {
                    val array = readableArray.getArray(i)
                    if (array != null) {
                        json.put(readableArrayToJson(array))
                    }
                }
            }
        }

        return json
    }

    private fun jsonToWritableMap(json: JSONObject): WritableMap {
        val map = Arguments.createMap()
        val keys = json.keys()

        while (keys.hasNext()) {
            val key = keys.next()
            val value = json.get(key)

            when (value) {
                is JSONObject -> map.putMap(key, jsonToWritableMap(value))
                is JSONArray -> map.putArray(key, jsonToWritableArray(value))
                is Boolean -> map.putBoolean(key, value)
                is Int -> map.putInt(key, value)
                is Double -> map.putDouble(key, value)
                is String -> map.putString(key, value)
                is Long -> map.putDouble(key, value.toDouble())
                JSONObject.NULL -> map.putNull(key)
                else -> map.putString(key, value.toString())
            }
        }

        return map
    }

    private fun jsonToWritableArray(json: JSONArray): com.facebook.react.bridge.WritableArray {
        val array = Arguments.createArray()

        for (i in 0 until json.length()) {
            val value = json.get(i)

            when (value) {
                is JSONObject -> array.pushMap(jsonToWritableMap(value))
                is JSONArray -> array.pushArray(jsonToWritableArray(value))
                is Boolean -> array.pushBoolean(value)
                is Int -> array.pushInt(value)
                is Double -> array.pushDouble(value)
                is String -> array.pushString(value)
                is Long -> array.pushDouble(value.toDouble())
                JSONObject.NULL -> array.pushNull()
                else -> array.pushString(value.toString())
            }
        }

        return array
    }
}
