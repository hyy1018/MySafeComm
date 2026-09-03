// ViewModel for hazard reports: Admin's full list, new-report submission, edit/delete.
package com.example.asgm.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.asgm.data.local.dao.ReportDao
import com.example.asgm.data.local.entity.ReportEntity
import com.example.asgm.data.local.entity.ReportStatus
import com.example.asgm.data.remote.isSupabaseConfigured
import com.example.asgm.data.remote.supabase
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReportViewModel(private val dao: ReportDao) : ViewModel() {

    val reports: StateFlow<List<ReportEntity>> = dao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // suspend so ReportHazardScreen can await the new id before clearing the form and navigating
    suspend fun submit(report: ReportEntity): Long {
        val newId = dao.insert(report)
        if (isSupabaseConfigured) {
            try {
                withContext(Dispatchers.IO) {
                    supabase.from("reports").insert(report.copy(reportId = newId))
                }
            } catch (e: Exception) {
                // Cloud copy failed -- local Room copy already saved.
            }
        }
        return newId
    }

    fun updateStatus(reportId: Long, status: ReportStatus) = viewModelScope.launch {
        dao.updateStatus(reportId, status)
        if (isSupabaseConfigured) {
            try {
                withContext(Dispatchers.IO) {
                    supabase.from("reports").update({
                        set("status", status.name)
                    }) {
                        filter { eq("reportId", reportId) }
                    }
                }
            } catch (e: Exception) {
                // Cloud copy failed -- local Room copy already saved.
            }
        }
    }
}

class ReportViewModelFactory(private val dao: ReportDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReportViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReportViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// Backs MyReportsScreen: one resident's own reports, live.
class MyReportsViewModel(dao: ReportDao, userId: String) : ViewModel() {
    val reports: StateFlow<List<ReportEntity>> = dao.getByUser(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

class MyReportsViewModelFactory(
    private val dao: ReportDao,
    private val userId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MyReportsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MyReportsViewModel(dao, userId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// Backs ReportDetailScreen: one report, view/edit/delete.
class ReportDetailViewModel(private val dao: ReportDao, reportId: Long) : ViewModel() {

    val report: StateFlow<ReportEntity?> = dao.getById(reportId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun update(report: ReportEntity) = viewModelScope.launch {
        dao.update(report)
        if (isSupabaseConfigured) {
            try {
                withContext(Dispatchers.IO) {
                    supabase.from("reports").update({
                        set("title", report.title)
                        set("location", report.location)
                        set("description", report.description)
                        set("photoUri", report.photoUri)
                        set("status", report.status.name)
                    }) {
                        filter { eq("reportId", report.reportId) }
                    }
                }
            } catch (e: Exception) {
                // Cloud copy failed -- local Room copy already saved.
            }
        }
    }

    fun delete(report: ReportEntity) = viewModelScope.launch {
        dao.delete(report)
        if (isSupabaseConfigured) {
            try {
                withContext(Dispatchers.IO) {
                    supabase.from("reports").delete {
                        filter { eq("reportId", report.reportId) }
                    }
                }
            } catch (e: Exception) {
                // Cloud copy failed -- local Room copy already deleted.
            }
        }
    }
}

class ReportDetailViewModelFactory(
    private val dao: ReportDao,
    private val reportId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReportDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReportDetailViewModel(dao, reportId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
