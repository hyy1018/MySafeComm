// ViewModel for community alerts: resident's Live Alerts list, Admin add/edit/delete.
package com.example.asgm.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.asgm.data.local.dao.AlertAcknowledgementDao
import com.example.asgm.data.local.dao.AlertDao
import com.example.asgm.data.local.entity.AlertAcknowledgementEntity
import com.example.asgm.data.local.entity.AlertEntity
import com.example.asgm.data.remote.isSupabaseConfigured
import com.example.asgm.data.remote.supabase
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlertViewModel(private val dao: AlertDao) : ViewModel() {

    val alerts: StateFlow<List<AlertEntity>> = dao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addAlert(alert: AlertEntity) = viewModelScope.launch {
        val newId = dao.insert(alert)
        if (isSupabaseConfigured) {
            try {
                withContext(Dispatchers.IO) {
                    supabase.from("alerts").insert(alert.copy(alertId = newId))
                }
            } catch (e: Exception) {
                // Cloud copy failed -- local Room copy already saved.
            }
        }
    }

    fun updateAlert(alert: AlertEntity) = viewModelScope.launch {
        dao.update(alert)
        if (isSupabaseConfigured) {
            try {
                withContext(Dispatchers.IO) {
                    supabase.from("alerts").update({
                        set("title", alert.title)
                        set("body", alert.body)
                        set("priority", alert.priority.name)
                        set("location", alert.location)
                        set("issuedBy", alert.issuedBy)
                    }) {
                        filter { eq("alertId", alert.alertId) }
                    }
                }
            } catch (e: Exception) {
                // Cloud copy failed -- local Room copy already saved.
            }
        }
    }

    fun deleteAlert(alert: AlertEntity) = viewModelScope.launch {
        dao.delete(alert)
        if (isSupabaseConfigured) {
            try {
                withContext(Dispatchers.IO) {
                    supabase.from("alerts").delete { filter { eq("alertId", alert.alertId) } }
                }
            } catch (e: Exception) {
                // Cloud copy failed -- local Room copy already deleted.
            }
        }
    }
}

class AlertViewModelFactory(private val dao: AlertDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AlertViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AlertViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// Backs AdminAlertFormScreen's edit case: one live alert by id.
class AlertDetailViewModel(dao: AlertDao, alertId: Long) : ViewModel() {
    val alert: StateFlow<AlertEntity?> = dao.getById(alertId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}

class AlertDetailViewModelFactory(
    private val dao: AlertDao,
    private val alertId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AlertDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AlertDetailViewModel(dao, alertId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// Backs one AlertCard row's acknowledgement state and Confirm button.
class AlertAckViewModel(
    private val dao: AlertAcknowledgementDao,
    private val alertId: Long,
    private val userId: String
) : ViewModel() {

    val acknowledged: StateFlow<Boolean> = dao.isAcknowledgedByUser(alertId, userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun acknowledge() = viewModelScope.launch {
        val ack = AlertAcknowledgementEntity(alertId = alertId, userId = userId)
        dao.acknowledge(ack)
        if (isSupabaseConfigured) {
            try {
                withContext(Dispatchers.IO) {
                    supabase.from("alert_acknowledgements").insert(ack)
                }
            } catch (e: Exception) {
                // Cloud copy failed -- local Room copy already saved.
            }
        }
    }
}

class AlertAckViewModelFactory(
    private val dao: AlertAcknowledgementDao,
    private val alertId: Long,
    private val userId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AlertAckViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AlertAckViewModel(dao, alertId, userId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
