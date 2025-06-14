package com.mehmettekin.altingunu.presentation.drawgroupscreen

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mehmettekin.altingunu.domain.repository.DrawGroupRepository
import com.mehmettekin.altingunu.utils.ResultState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DrawGroupsViewModel @Inject constructor(
    private val drawGroupRepository: DrawGroupRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(DrawGroupsState())
    val state: StateFlow<DrawGroupsState> = _state.asStateFlow()

    init {
        loadAllDrawGroups()
        migrateLegacyDataIfNeeded()
    }

    fun onEvent(event: DrawGroupsEvent) {
        when (event) {
            is DrawGroupsEvent.OnTabChanged -> {
                _state.update { currentState ->
                    currentState.copy(selectedTab = event.tab)
                }
            }

            is DrawGroupsEvent.OnGroupClick -> {
                // Navigation handled in UI layer
            }

            is DrawGroupsEvent.OnDeleteGroup -> {
                _state.update { currentState ->
                    currentState.copy(
                        showDeleteDialog = true,
                        groupToDelete = event.group
                    )
                }
            }

            is DrawGroupsEvent.OnConfirmDelete -> {
                val groupToDelete = _state.value.groupToDelete
                if (groupToDelete != null) {
                    deleteGroup(groupToDelete.id)
                }
                _state.update { currentState ->
                    currentState.copy(
                        showDeleteDialog = false,
                        groupToDelete = null
                    )
                }
            }

            is DrawGroupsEvent.OnCancelDelete -> {
                _state.update { currentState ->
                    currentState.copy(
                        showDeleteDialog = false,
                        groupToDelete = null
                    )
                }
            }

            is DrawGroupsEvent.OnErrorDismiss -> {
                _state.update { currentState ->
                    currentState.copy(error = null)
                }
            }

            is DrawGroupsEvent.OnRefresh -> {
                loadAllDrawGroups()
            }
        }
    }

    private fun loadAllDrawGroups() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            drawGroupRepository.getAllDrawGroups().collect { result ->
                when (result) {
                    is ResultState.Success -> {
                        val allGroups = result.data
                        val activeGroups = allGroups.filter { it.isActive && !it.isCompleted }
                        val completedGroups = allGroups.filter { it.isCompleted }

                        _state.update { currentState ->
                            currentState.copy(
                                allGroups = allGroups,
                                activeGroups = activeGroups,
                                completedGroups = completedGroups,
                                isLoading = false,
                                error = null
                            )
                        }
                    }

                    is ResultState.Error -> {
                        _state.update { currentState ->
                            currentState.copy(
                                isLoading = false,
                                error = result.message
                            )
                        }
                    }

                    is ResultState.Loading -> {
                        _state.update { it.copy(isLoading = true) }
                    }

                    is ResultState.Idle -> {
                        // No action needed
                    }
                }
            }
        }
    }

    private fun deleteGroup(groupId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            when (val result = drawGroupRepository.deleteDrawGroup(groupId)) {
                is ResultState.Success -> {
                    _state.update { currentState ->
                        currentState.copy(isLoading = false)
                    }
                    // Refresh data after deletion
                    loadAllDrawGroups()
                }

                is ResultState.Error -> {
                    _state.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }

                is ResultState.Loading -> {
                    _state.update { it.copy(isLoading = true) }
                }

                is ResultState.Idle -> {
                    // No action needed
                }
            }
        }
    }

    private fun migrateLegacyDataIfNeeded() {
        viewModelScope.launch {
            try {
                drawGroupRepository.migrateLegacyData()
            } catch (e: Exception) {
                // Silent migration failure - not critical for user experience
                android.util.Log.w("DrawGroupsViewModel", "Legacy data migration failed", e)
            }
        }
    }

    fun getFilteredGroups(): List<com.mehmettekin.altingunu.domain.model.DrawGroup> {
        return when (_state.value.selectedTab) {
            DrawGroupTab.ACTIVE -> _state.value.activeGroups
            DrawGroupTab.COMPLETED -> _state.value.completedGroups
            DrawGroupTab.ALL -> _state.value.allGroups
        }
    }

    fun getActiveGroupsCount(): Int = _state.value.activeGroups.size

    fun getCompletedGroupsCount(): Int = _state.value.completedGroups.size

    fun getAllGroupsCount(): Int = _state.value.allGroups.size

    fun hasGroups(): Boolean = _state.value.allGroups.isNotEmpty()

    fun isActiveTabSelected(): Boolean = _state.value.selectedTab == DrawGroupTab.ACTIVE

    fun isCompletedTabSelected(): Boolean = _state.value.selectedTab == DrawGroupTab.COMPLETED

    fun isAllTabSelected(): Boolean = _state.value.selectedTab == DrawGroupTab.ALL

    override fun onCleared() {
        super.onCleared()
        // Clean up any resources if needed
    }
}