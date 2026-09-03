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

    // community contacts only -- residents' private contacts (ownerId set) are not the admin's to manage
    val contacts: StateFlow<List<EmergencyContactEntity>> = dao.getCommunity()
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

    fun updateContact(contact: EmergencyContactEntity) = viewModelScope.launch {
        dao.update(contact)
        if (isSupabaseConfigured) {
            try {
                withContext(Dispatchers.IO) {
                    supabase.from("emergency_contacts").update({
                        set("name", contact.name)
                        set("phoneNo", contact.phoneNo)
                        set("categoryEmergency", contact.categoryEmergency)
                    }) {
                        filter { eq("serviceId", contact.serviceId) }
                    }
                }
            } catch (e: Exception) {
                // Cloud copy failed -- local Room copy already saved.
            }
        }
    }

    fun deleteContact(contact: EmergencyContactEntity) = viewModelScope.launch {
        dao.delete(contact)
        if (isSupabaseConfigured) {
            try {
                withContext(Dispatchers.IO) {
                    supabase.from("emergency_contacts").delete { filter { eq("serviceId", contact.serviceId) } }
                }
            } catch (e: Exception) {
                // Cloud copy failed -- local Room copy already deleted.
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

// Backs AdminAddContactScreen's edit case: one contact by id.
class EmergencyContactDetailViewModel(dao: EmergencyContactDao, serviceId: Long) : ViewModel() {
    val contact: StateFlow<EmergencyContactEntity?> = dao.getById(serviceId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}

class EmergencyContactDetailViewModelFactory(
    private val dao: EmergencyContactDao,
    private val serviceId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EmergencyContactDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EmergencyContactDetailViewModel(dao, serviceId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// Backs the resident's SOS screen: the community list plus their own private contacts, and
// add/edit/delete for the private ones. A private contact carries ownerId = this user's id.
class PersonalContactViewModel(
    private val dao: EmergencyContactDao,
    private val userId: String
) : ViewModel() {

    val personal: StateFlow<List<EmergencyContactEntity>> = dao.getByOwner(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val community: StateFlow<List<EmergencyContactEntity>> = dao.getCommunity()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun add(name: String, phone: String) = viewModelScope.launch {
        val contact = EmergencyContactEntity(
            name = name,
            phoneNo = phone,
            categoryEmergency = "", // residents only enter a name + number
            ownerId = userId
        )
        val newId = dao.insert(contact)
        if (isSupabaseConfigured) {
            try {
                withContext(Dispatchers.IO) {
                    supabase.from("emergency_contacts").insert(contact.copy(serviceId = newId))
                }
            } catch (e: Exception) {
                // Cloud copy failed -- local Room copy already saved.
            }
        }
    }

    fun update(contact: EmergencyContactEntity) = viewModelScope.launch {
        dao.update(contact)
        if (isSupabaseConfigured) {
            try {
                withContext(Dispatchers.IO) {
                    supabase.from("emergency_contacts").update({
                        set("name", contact.name)
                        set("phoneNo", contact.phoneNo)
                    }) {
                        filter { eq("serviceId", contact.serviceId) }
                    }
                }
            } catch (e: Exception) {
                // Cloud copy failed -- local Room copy already saved.
            }
        }
    }

    fun delete(contact: EmergencyContactEntity) = viewModelScope.launch {
        dao.delete(contact)
        if (isSupabaseConfigured) {
            try {
                withContext(Dispatchers.IO) {
                    supabase.from("emergency_contacts").delete { filter { eq("serviceId", contact.serviceId) } }
                }
            } catch (e: Exception) {
                // Cloud copy failed -- local Room copy already deleted.
            }
        }
    }
}

class PersonalContactViewModelFactory(
    private val dao: EmergencyContactDao,
    private val userId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PersonalContactViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PersonalContactViewModel(dao, userId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
