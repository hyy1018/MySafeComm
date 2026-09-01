package com.example.asgm.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.asgm.data.local.dao.EmergencyContactDao
import com.example.asgm.data.local.entity.EmergencyContactEntity
import com.example.asgm.data.remote.isSupabaseConfigured
import com.example.asgm.data.remote.supabase
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ViewModel shouldn't directly deal with the database.
// UI -> ViewModel -> Dao (local Room) + Supabase (cloud) -> Database.
// Every write below goes to BOTH stores, per the "store data in 2 places" requirement.
class EmergencyContactViewModel(private val dao: EmergencyContactDao) : ViewModel() {

    // stateIn = convert Flow to StateFlow.
    // SharingStarted.WhileSubscribed(5000) = actively collect the database flow.
    // If nobody is subscribed, wait 5 seconds before stopping.
    val contacts: StateFlow<List<EmergencyContactEntity>> = dao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addContact(contact: EmergencyContactEntity) = viewModelScope.launch {
        val newId = dao.insert(contact)
        // Supabase's copy of this table is NOT auto-generated -- we write the same id Room just
        // generated so both stores agree on the same row. Guarded by isSupabaseConfigured and
        // wrapped in try/catch: if the project isn't set up yet, or the device is offline, the
        // local Room copy has already saved fine and the screen keeps working from that.
        if (isSupabaseConfigured) {
            try {
                supabase.from("emergency_contacts").insert(contact.copy(serviceId = newId))
            } catch (e: Exception) {
                // Cloud copy failed -- local copy already succeeded, nothing else to do here.
            }
        }
    }
}

// EmergencyContactViewModelFactory is responsible for creating EmergencyContactViewModel and
// passing the required EmergencyContactDao into it.
class EmergencyContactViewModelFactory(private val dao: EmergencyContactDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EmergencyContactViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EmergencyContactViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
