package com.example.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * System announcements and updates broadcasted by the admin
 * to all logged-in users of the application.
 *
 * Supports optional scheduled appearance and disappearance/expiration timestamps.
 */
@Entity(tableName = "app_announcements")
data class AppAnnouncement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val message: String,
    val category: String = "UPDATE", // UPDATE, SYSTEM, PAYMENT, FEATURE, NOTICE
    val senderEmail: String = "Admin",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val priority: String = "NORMAL", // NORMAL, HIGH, URGENT
    val apkDownloadUrl: String? = null,
    val versionCode: String? = null,
    val scheduledAppearTime: Long? = null, // Optional: timestamp when notification starts appearing
    val scheduledDisappearTime: Long? = null // Optional: timestamp when notification auto-disappears/expires
) {
    /**
     * Determines whether the announcement is currently active given the current timestamp.
     */
    fun isCurrentlyActive(now: Long = System.currentTimeMillis()): Boolean {
        val appearValid = scheduledAppearTime == null || scheduledAppearTime <= 0 || now >= scheduledAppearTime
        val disappearValid = scheduledDisappearTime == null || scheduledDisappearTime <= 0 || now <= scheduledDisappearTime
        return appearValid && disappearValid
    }
}

/**
 * Tracks users who have logged into the app to enable targeted communications
 * and user roster visibility for the admin.
 */
@Entity(tableName = "app_users")
data class AppUser(
    @PrimaryKey val email: String,
    val displayName: String = "",
    val photoUrl: String? = null,
    val lastLoginTime: Long = System.currentTimeMillis(),
    val isAdmin: Boolean = false
)

@Dao
interface AnnouncementDao {
    @Query("SELECT * FROM app_announcements ORDER BY timestamp DESC")
    fun getAllAnnouncements(): Flow<List<AppAnnouncement>>

    @Query("SELECT * FROM app_announcements ORDER BY timestamp DESC")
    suspend fun getAllAnnouncementsList(): List<AppAnnouncement>

    @Query("SELECT * FROM app_announcements WHERE (:now >= COALESCE(scheduledAppearTime, 0)) AND (scheduledDisappearTime IS NULL OR scheduledDisappearTime <= 0 OR :now <= scheduledDisappearTime) ORDER BY timestamp DESC")
    fun getActiveAnnouncements(now: Long): Flow<List<AppAnnouncement>>

    @Query("SELECT COUNT(*) FROM app_announcements WHERE isRead = 0")
    fun getUnreadCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnouncement(announcement: AppAnnouncement): Long

    @Query("UPDATE app_announcements SET isRead = 1 WHERE id = :id OR timestamp = :id")
    suspend fun markAsRead(id: Long)

    @Query("UPDATE app_announcements SET isRead = 1")
    suspend fun markAllAsRead()

    @Query("DELETE FROM app_announcements WHERE id = :id OR timestamp = :id")
    suspend fun deleteAnnouncement(id: Long)

    @Query("DELETE FROM app_announcements WHERE title != 'Welcome to EnterFirst Finance & UPI'")
    suspend fun clearAllExceptWelcome()

    @Query("DELETE FROM app_announcements")
    suspend fun clearAllAnnouncements()

    @Query("SELECT COUNT(*) FROM app_announcements")
    suspend fun getAnnouncementCount(): Int

    // User management
    @Query("SELECT * FROM app_users ORDER BY lastLoginTime DESC")
    fun getAllUsers(): Flow<List<AppUser>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUser(user: AppUser)

    @Query("SELECT * FROM app_users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): AppUser?
}
