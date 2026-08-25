package com.example.asgm.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.asgm.data.DemoSession
import com.example.asgm.data.local.dao.AlertDao
import com.example.asgm.data.local.dao.CommentDao
import com.example.asgm.data.local.dao.EmergencyContactDao
import com.example.asgm.data.local.dao.LikeDao
import com.example.asgm.data.local.dao.PostDao
import com.example.asgm.data.local.dao.ReportDao
import com.example.asgm.data.local.dao.SafetyGuideDao
import com.example.asgm.data.local.dao.UserDao
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
                    }
                }).build().also { INSTANCE = it }
            }
    }
}
