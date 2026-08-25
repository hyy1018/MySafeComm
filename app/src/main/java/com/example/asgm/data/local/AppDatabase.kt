package com.example.asgm.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.asgm.data.DemoSession
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
    version = 1,
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
                ).addCallback(object : Callback() {
                    // Seeds the demo resident account used by DemoSession until Login exists,
                    // so screens that write to Reports/Posts/etc. have a valid userId to point at.
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        db.execSQL(
                            "INSERT INTO users (id, password, name, role, contact) VALUES " +
                                "('${DemoSession.CURRENT_USER_ID}', 'demo1234', 'Demo Resident', 'RESIDENT', '0000000000')"
                        )
                        seedAlerts(db)
                        seedEmergencyContacts(db)
                        seedSafetyGuides(db)
                    }

                    // Sample community notices so the Live Alerts feed isn't empty before an
                    // Admin "Add Alert" screen exists.
                    private fun seedAlerts(db: SupportSQLiteDatabase) {
                        val now = System.currentTimeMillis()
                        val alerts = listOf(
                            Triple(
                                "Water Main Maintenance",
                                "Oakwood Sector - Blocks A-G, 14:00 - 18:00 (Today). Category: Infrastructure Maintenance.",
                                "URGENT"
                            ),
                            Triple(
                                "Community Town Hall Meeting",
                                "Scheduled for Friday at 6:00 PM.",
                                "INFO"
                            ),
                            Triple(
                                "New Security Patrol Route",
                                "Enhanced surveillance in Zone 4.",
                                "INFO"
                            ),
                            Triple(
                                "Park Cleaning Drive",
                                "Volunteers needed this weekend.",
                                "INFO"
                            )
                        )
                        alerts.forEachIndexed { index, (title, body, priority) ->
                            db.execSQL(
                                "INSERT INTO alerts (title, body, priority, timestamp) VALUES " +
                                    "('$title', '$body', '$priority', ${now - index * 1000})"
                            )
                        }
                    }

                    // Sample directory so the Emergency Hub grid isn't empty before an Admin
                    // "Manage Contacts" screen exists.
                    private fun seedEmergencyContacts(db: SupportSQLiteDatabase) {
                        data class Contact(
                            val name: String,
                            val phone: String,
                            val description: String,
                            val specialized: Boolean
                        )
                        val contacts = listOf(
                            Contact("Neighborhood Security", "012-345-6789", "Direct line to community patrol", false),
                            Contact("General Emergency", "999", "Police and ambulance", false),
                            Contact("Scam Response", "997", "Report suspected scams", false),
                            Contact("Civil Defence (APM)", "991", "Floods, trees, snakes", false),
                            Contact("Fire and Rescue", "994", "Fire emergencies", false),
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
