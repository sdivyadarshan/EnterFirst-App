package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PreferenceDao {
    @Query("SELECT * FROM preferences WHERE `key` = :key")
    fun getPreferenceFlow(key: String): Flow<Preference?>

    @Query("SELECT value FROM preferences WHERE `key` = :key")
    suspend fun getPreferenceValue(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreference(preference: Preference)

    @Query("DELETE FROM preferences")
    suspend fun deleteAllPreferences()
}
