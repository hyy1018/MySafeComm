// #member1
// Pulls the cloud (Supabase) copy of every table down into Room. Room stays the source of
// truth for the UI -- this fills it in / refreshes it from the remote database, typically
// right after login. It is the "retrieve from the remote database" half of the app's
// local + remote data storage: writes push to Supabase as they happen, and this reads back.
package com.example.asgm.data.remote

import android.content.Context
import com.example.asgm.data.local.AppDatabase
import com.example.asgm.data.local.entity.AlertAcknowledgementEntity
import com.example.asgm.data.local.entity.AlertEntity
import com.example.asgm.data.local.entity.CommentEntity
import com.example.asgm.data.local.entity.ConversationReadEntity
import com.example.asgm.data.local.entity.EmergencyContactEntity
import com.example.asgm.data.local.entity.LikeEntity
import com.example.asgm.data.local.entity.MessageEntity
import com.example.asgm.data.local.entity.PostEntity
import com.example.asgm.data.local.entity.ReportEntity
import com.example.asgm.data.local.entity.SafetyGuideEntity
import com.example.asgm.data.local.entity.UserEntity
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object CloudSync {
    // app-scoped, so the pull keeps running even after the login screen is gone
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun pull(context: Context) {
        val app = context.applicationContext
        scope.launch { pullAll(app) }
    }

    private suspend fun pullAll(context: Context) {
        if (!isSupabaseConfigured) return
        val db = AppDatabase.getInstance(context)
        // Parent tables before their children so foreign keys resolve. Each table has its own
        // try/catch so one empty / not-yet-created table can't stop the rest.
        try { db.userDao().upsertAll(supabase.from("users").select().decodeList<UserEntity>()) } catch (e: Exception) {}
        try { db.alertDao().upsertAll(supabase.from("alerts").select().decodeList<AlertEntity>()) } catch (e: Exception) {}
        try { db.safetyGuideDao().upsertAll(supabase.from("safety_guides").select().decodeList<SafetyGuideEntity>()) } catch (e: Exception) {}
        try { db.emergencyContactDao().upsertAll(supabase.from("emergency_contacts").select().decodeList<EmergencyContactEntity>()) } catch (e: Exception) {}
        try { db.postDao().upsertAll(supabase.from("posts").select().decodeList<PostEntity>()) } catch (e: Exception) {}
        try { db.reportDao().upsertAll(supabase.from("reports").select().decodeList<ReportEntity>()) } catch (e: Exception) {}
        try { db.commentDao().upsertAll(supabase.from("comments").select().decodeList<CommentEntity>()) } catch (e: Exception) {}
        try { db.likeDao().upsertAll(supabase.from("likes").select().decodeList<LikeEntity>()) } catch (e: Exception) {}
        try { db.alertAcknowledgementDao().upsertAll(supabase.from("alert_acknowledgements").select().decodeList<AlertAcknowledgementEntity>()) } catch (e: Exception) {}
        try { db.messageDao().upsertAll(supabase.from("messages").select().decodeList<MessageEntity>()) } catch (e: Exception) {}
        try { db.conversationReadDao().upsertAll(supabase.from("conversation_reads").select().decodeList<ConversationReadEntity>()) } catch (e: Exception) {}
    }
}
