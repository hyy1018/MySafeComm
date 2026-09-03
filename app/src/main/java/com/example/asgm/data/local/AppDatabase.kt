// #member1
// The Room database: table list, DAOs, and seed data for a fresh install.
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
import com.example.asgm.data.local.dao.MessageDao
import com.example.asgm.data.local.dao.PostDao
import com.example.asgm.data.local.dao.ReportDao
import com.example.asgm.data.local.dao.SafetyGuideDao
import com.example.asgm.data.local.dao.UserDao
import com.example.asgm.data.local.entity.AlertAcknowledgementEntity
import com.example.asgm.data.local.entity.AlertEntity
import com.example.asgm.data.local.entity.CommentEntity
import com.example.asgm.data.local.entity.EmergencyContactEntity
import com.example.asgm.data.local.entity.LikeEntity
import com.example.asgm.data.local.entity.MessageEntity
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
        LikeEntity::class,
        MessageEntity::class
    ],
    version = 15,
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
    abstract fun messageDao(): MessageDao

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
                    // dev-time only: wipes and rebuilds on every schema bump instead of crashing.
                    // swap for real Migrations before submission if user data must survive an update.
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .addCallback(object : Callback() {
                    // runs on every open, not just onCreate, since the wipe above skips onCreate;
                    // each seed function checks the table is empty first so this stays idempotent
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

                    // test1/test2 (resident) and admin1/admin2 (admin) to sign in with -- all four abc123456.
                    // IMPORTANT: every column here is NOT NULL -- INSERT OR IGNORE silently drops
                    // the whole row if one's missing (no crash, account just never gets created).
                    // Adding a NOT NULL column to UserEntity? Add it here too.
                    private fun seedUser(
                        db: SupportSQLiteDatabase,
                        id: String,
                        password: String,
                        name: String,
                        role: String,
                        phone: String = "",
                        email: String = ""
                    ) {
                        db.execSQL(
                            "INSERT OR IGNORE INTO users " +
                                "(id, password, name, role, phone, address, email, lastSeenActivityAt, lastSeenMessagesAt) VALUES " +
                                "('$id', '$password', '$name', '$role', '$phone', '', '$email', 0, 0)"
                        )
                    }

                    private fun seedTestAccounts(db: SupportSQLiteDatabase) {
                        seedUser(db, "test1", "abc123456", "Test User", "RESIDENT", "0000000000", "test1@example.com")
                        seedUser(db, "test2", "abc123456", "Test User 2", "RESIDENT", "0000000000", "test2@example.com")
                        seedUser(db, "admin1", "abc123456", "Demo Admin", "ADMIN", "0000000000", "admin1@example.com")
                        seedUser(db, "admin2", "abc123456", "Demo Admin 2", "ADMIN", "0000000000", "admin2@example.com")
                    }

                    // sample notices so Live Alerts isn't empty on first launch
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

                    // sample contacts so SOS isn't empty on first launch
                    private fun seedEmergencyContacts(db: SupportSQLiteDatabase) {
                        if (rowCount(db, "emergency_contacts") > 0) return
                        data class Contact(val name: String, val phone: String, val description: String)
                        val contacts = listOf(
                            Contact("General Emergency", "999", "Police and ambulance"),
                            Contact("Scam Response", "997", "Report suspected scams"),
                            Contact("Civil Defence (APM)", "991", "Floods, trees, snakes"),
                            Contact("Fire and Rescue", "994", "Fire emergencies"),
                            Contact("Neighborhood Security", "012-345-6789", "Direct line to community patrol")
                        )
                        contacts.forEach { c ->
                            db.execSQL(
                                "INSERT INTO emergency_contacts (name, phoneNo, categoryEmergency) VALUES " +
                                    "('${c.name}', '${c.phone}', '${c.description}')"
                            )
                        }
                    }

                    // sample guides so Safety Guides isn't empty on first launch
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
