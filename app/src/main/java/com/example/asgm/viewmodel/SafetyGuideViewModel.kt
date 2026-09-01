package com.example.asgm.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.asgm.data.local.dao.SafetyGuideDao
import com.example.asgm.data.local.entity.SafetyGuideEntity
import com.example.asgm.data.remote.isSupabaseConfigured
import com.example.asgm.data.remote.supabase
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// UI -> ViewModel -> Dao (local Room) + Supabase (cloud) -> Database.
// Backs the SOS screen's list of safety guide categories.
class SafetyGuideViewModel(private val dao: SafetyGuideDao) : ViewModel() {

    val guides: StateFlow<List<SafetyGuideEntity>> = dao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addGuide(guide: SafetyGuideEntity) = viewModelScope.launch {
        val newId = dao.insert(guide)
        // Cloud copy uses the same id Room just generated so both stores agree on the same row.
        if (isSupabaseConfigured) {
            try {
                supabase.from("safety_guides").insert(guide.copy(guideId = newId))
            } catch (e: Exception) {
                // Cloud copy failed (offline / credentials not set yet) -- local Room copy already saved.
            }
        }
    }
}

class SafetyGuideViewModelFactory(private val dao: SafetyGuideDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SafetyGuideViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SafetyGuideViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// Backs the single-guide detail screen. Takes guideId up front (via its Factory) so the StateFlow
// for that one guide is set up once in the ViewModel's body, the same way the list ViewModel above
// sets up its StateFlow once -- not re-created on every recomposition.
class SafetyGuideDetailViewModel(dao: SafetyGuideDao, guideId: Long) : ViewModel() {

    val guide: StateFlow<SafetyGuideEntity?> = dao.getById(guideId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}

class SafetyGuideDetailViewModelFactory(
    private val dao: SafetyGuideDao,
    private val guideId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SafetyGuideDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SafetyGuideDetailViewModel(dao, guideId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
