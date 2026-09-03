// #member3
// ViewModel for the SOS emergency contacts list.
package com.example.asgm.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.asgm.data.local.dao.EmergencyContactDao
import com.example.asgm.data.local.entity.EmergencyContactEntity
import com.example.asgm.data.remote.isSupabaseConfigured
import com.example.asgm.data.remote.supabase
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EmergencyContactViewModel(private val dao: EmergencyContactDao) : ViewModel() {

    val contacts: StateFlow<List<EmergencyContactEntity>> = dao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addContact(contact: EmergencyContactEntity) = viewModelScope.launch {
        val newId = dao.insert(contact)
        // same id as the local row, so both stores stay in sync
        if (isSupabaseConfigured) {
            try {
                withContext(Dispatchers.IO) {
                    supabase.from("emergency_contacts").insert(contact.copy(serviceId = newId))
                }
            } catch (e: Exception) {
                // Cloud copy failed -- local copy already succeeded.
            }
        }
    }
}

class EmergencyContactViewModelFactory(private val dao: EmergencyContactDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EmergencyContactViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EmergencyContactViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
