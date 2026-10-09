package com.example.nmf.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nmf.data.AppDatabase
import com.example.nmf.data.BlockedSite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FocusFilterViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).blockedSiteDao()

    val blockedSites: StateFlow<List<BlockedSite>> = dao.getAllSites().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
    )

    fun addSite(domainInput: String, onComplete: (() -> Unit)? = null) {
        val cleanDomain = domainInput.trim()
            .lowercase()
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
            .substringBefore("/")
            .trim()

        if (cleanDomain.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            dao.insertSite(BlockedSite(domain = cleanDomain))
            onComplete?.invoke()
        }
    }

    fun deleteSite(site: BlockedSite) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteSite(site)
        }
    }
}