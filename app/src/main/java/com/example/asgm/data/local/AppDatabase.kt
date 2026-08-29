package com.example.asgm.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.asgm.data.local.dao.AlertAcknowledgementDao
import com.example.asgm.data.local.dao.AlertDao
import com.example.asgm.data.local.dao.CommentDao
import com.example.asgm.data.local.dao.EmergencyContactDao
import com.example.asgm.data.local.dao.LikeDao
import com.example.asgm.data.local.dao.PostDao
import com.example.asgm.data.local.dao.ReportDao
import com.example.asgm.data.local.dao.SafetyGuideDao
import com.example.asgm.data.local.dao.UserDao
import com.example.asgm.data.local.entity.AlertAcknowledgementEntity
import com.example.asgm.data.local.entity.AlertEntity
import com.example.asgm.data.local.entity.CommentEntity
import com.example.asgm.data.local.entity.EmergencyContactEntity
import com.example.asgm.data.local.entity.LikeEntity
import com.example.asgm.data.local.entity.PostEntity
import com.example.asgm.data.local.entity.ReportEntity
import com.example.asgm.data.local.entity.SafetyGuideEntity
import com.example.asgm.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        ReportEntity::class,
        AlertEntity::class,
        AlertAcknowledgementEntity::class,
        EmergencyContactEntity::class,
        SafetyGuideEntity::class,
        PostEntity::class,
        CommentEntity::class,
        LikeEntity::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun reportDao(): ReportDao
    abstract fun alertDao(): AlertDao
    abstract fun alertAcknowledgementDao(): AlertAcknowledgementDao
    abstract fun emergencyContactDao(): EmergencyContactDao
    abstract fun safetyGuideDao(): SafetyGuideDao
    abstract fun postDao(): PostDao
    abstract fun commentDao(): CommentDao
    abstract fun likeDao(): LikeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "my_safe_community.db"
                )
                    // Dev-time convenience: the schema is still changing often. Recreate the
                    // database instead of crashing when the version bumps with no Migration.
                    // Replace with real Migrations before submission if user data must survive an update.
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .addCallback(object : Callback() {
                    // Seeds run on every open (not just onCreate): fallbackToDestructiveMigration
                    // wipes tables without re-invoking onCreate, and each seed function below
                    // checks the table is empty first, so this stays idempotent and self-heals
                    // after a destructive migration instead of leaving the test accounts missing
                    // (which crashed Post/Comment/Like inserts with a FOREIGN KEY constraint error).
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        seedTestAccounts(db)
                        seedAlerts(db)
                        seedEmergencyContacts(db)
                        seedSafetyGuides(db)
                    }

                    private fun rowCount(db: SupportSQLiteDatabase, table: String): Int {
                        db.query("SELECT COUNT(*) FROM $table").use { cursor ->
                            cursor.moveToFirst()
                            return cursor.getInt(0)
                        }
                    }

                    // Only two test accounts to sign in with on the Login screen:
                    // test1/abc123456 (User tab) and admin1/abc123456 (Admin tab).
                    private fun seedTestAccounts(db: SupportSQLiteDatabase) {
                        // address is NOT NULL with no SQL-level default (Kotlin default values
                        // aren't reflected in the generated schema) -- omitting it here previously
                        // made INSERT OR IGNORE silently drop the row instead of throwing, since
                        // IGNORE suppresses NOT NULL violations too, not just PK/UNIQUE ones.
                        db.execSQL(
                            "INSERT OR IGNORE INTO users (id, password, name, role, phone, address, email) VALUES " +
                                "('test1', 'abc123456', 'Test User', 'RESIDENT', '0000000000', '', 'test1@example.com')"
                        )
                        db.execSQL(
                            "INSERT OR IGNORE INTO users (id, password, name, role, phone, address, email) VALUES " +
                                "('admin1', 'abc123456', 'Demo Admin', 'ADMIN', '0000000000', '', 'admin1@example.com')"
                        )
                    }

                    // Sample community notices so the Live Alerts feed isn't empty before an
                    // Admin "Add Alert" screen exists. Formatted as formal official notices:
                    // date (timestamp), location, and issuing office.
                    private fun seedAlerts(db: SupportSQLiteDatabase) {
                        if (rowCount(db, "alerts") > 0) return
                        val now = System.currentTimeMillis()
                        data class Notice(
                            val title: String,
                            val body: String,
                            val priority: String,
                            val location: String,
                            val issuedBy: String
                        )
                        val alerts = listOf(
                            Notice(
                                "Water Main Maintenance",
                                "Water supply will be interrupted from 14:00 to 18:00 today for scheduled infrastructure maintenance. Residents are advised to store water in advance.",
                                "URGENT",
                                "Oakwood Sector, Blocks A-G",
                                "Facilities Management Office"
                            ),
                            Notice(
                                "Community Town Hall Meeting",
                                "All residents are invited to attend the quarterly town hall meeting to discuss community matters and upcoming projects.",
                                "INFO",
                                "Community Hall, Ground Floor",
                                "Community Management Office"
                            ),
                            Notice(
                                "New Security Patrol Route",
                                "Enhanced surveillance and foot patrols have been implemented in Zone 4 following recent resident feedback.",
                                "INFO",
                                "Zone 4",
                                "Neighborhood Security Team"
                            ),
                            Notice(
                                "Park Cleaning Drive",
                                "Volunteers are needed for the community park cleaning drive this weekend. Gloves and equipment will be provided.",
                                "INFO",
                                "Central Community Park",
                                "Community Management Office"
                            )
                        )
                        alerts.forEachIndexed { index, notice ->
                            db.execSQL(
                                "INSERT INTO alerts (title, body, priority, location, issuedBy, timestamp) VALUES " +
                                    "('${notice.title}', '${notice.body}', '${notice.priority}', " +
                                    "'${notice.location}', '${notice.issuedBy}', ${now - index * 1000})"
                            )
                        }
                    }

                    // Sample directory so the Emergency Hub grid isn't empty before an Admin
                    // "Manage Contacts" screen exists.
                    private fun seedEmergencyContacts(db: SupportSQLiteDatabase) {
                        if (rowCount(db, "emergency_contacts") > 0) return
                        data class Contact(
                            val name: String,
                            val phone: String,
                            val description: String,
                            val specialized: Boolean
                        )
                        val contacts = listOf(
                            // The four 99x emergency numbers fill the grid's first 2 rows...
                            Contact("General Emergency", "999", "Police and ambulance", false),
                            Contact("Scam Response", "997", "Report suspected scams", false),
                            Contact("Civil Defence (APM)", "991", "Floods, trees, snakes", false),
                            Contact("Fire and Rescue", "994", "Fire emergencies", false),
                            // ...and Security, being the odd one out, gets centered on its own row.
                            Contact("Neighborhood Security", "012-345-6789", "Direct line to community patrol", false),
                            Contact("Private Surgery", "03-1234-5678", "On-call urgent care center", true),
                            Contact("Mental Health Hotline", "03-8765-4321", "24/7 crisis support", true)
                        )
                        contacts.forEach { c ->
                            db.execSQL(
                                "INSERT INTO emergency_contacts (name, phoneNo, categoryEmergency, isSpecialized) VALUES " +
                                    "('${c.name}', '${c.phone}', '${c.description}', ${if (c.specialized) 1 else 0})"
                            )
                        }
                    }

                    // Sample step-by-step guides so the Safety Guide screens aren't empty before
                    // an Admin "Manage Guides" screen exists.
                    private fun seedSafetyGuides(db: SupportSQLiteDatabase) {
                        if (rowCount(db, "safety_guides") > 0) return
                        val fireSteps = listOf(
                            "Assess the Situation||Immediately identify the source and size of the fire. If it is larger than a small trash can, do not attempt to fight it yourself.",
                            "Alert the Household||Shout FIRE loudly to ensure everyone is awake and aware. Use the persistent SOS button in the app to notify community responders immediately.",
                            "Evacuate Immediately||Leave the building using the nearest safe exit. Do not stop to collect personal belongings. Stay low to the floor if there is smoke.",
                            "Call Emergency Services||Once outside in a safe location, call 994 or your local emergency number. Provide clear details of your location and the fire status."
                        ).joinToString("\n")
                        val floodSteps = listOf(
                            "Move to High Ground||Relocate to the highest level of your home or building immediately.",
                            "Turn Off Utilities||Switch off electricity and gas at the mains if it is safe to do so.",
                            "Avoid Floodwater||Do not walk or drive through moving water; six inches can knock you down.",
                            "Call for Help||Use the SOS button or call Civil Defence (991) if water continues to rise."
                        ).joinToString("\n")
                        val powerOutageSteps = listOf(
                            "Use Torches, Not Candles||Avoid open flames; use flashlights or battery-powered lanterns.",
                            "Unplug Appliances||Prevent damage from power surges when electricity returns.",
                            "Check on Neighbours||Especially elderly residents or those relying on medical equipment.",
                            "Report the Outage||Use Hazard Reporting or call the Neighborhood Security line."
                        ).joinToString("\n")
                        val earthquakeSteps = listOf(
                            "Drop, Cover, and Hold On||Get under sturdy furniture and hold on until the shaking stops.",
                            "Stay Away from Windows||Move away from glass, mirrors, and heavy furniture that could fall.",
                            "Evacuate After Shaking Stops||Use stairs, not elevators, and head to an open area.",
                            "Check for Injuries and Hazards||Assist others if safe to do so, and report gas leaks or structural damage."
                        ).joinToString("\n")

                        val guides = listOf(
                            "Fire" to fireSteps,
                            "Flood" to floodSteps,
                            "Power Outage" to powerOutageSteps,
                            "Earthquake" to earthquakeSteps
                        )
                        guides.forEach { (category, steps) ->
                            db.execSQL(
                                "INSERT INTO safety_guides (categorySafety, steps) VALUES ('$category', '$steps')"
                            )
                        }
                    }
                }).build().also { INSTANCE = it }
            }
    }
}
