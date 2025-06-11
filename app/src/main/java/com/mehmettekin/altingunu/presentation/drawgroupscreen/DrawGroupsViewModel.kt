package com.mehmettekin.altingunu.presentation.drawgroupscreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mehmettekin.altingunu.domain.model.DrawGroup
import com.mehmettekin.altingunu.domain.repository.DrawGroupRepository
import com.mehmettekin.altingunu.utils.ResultState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DrawGroupsViewModel @Inject constructor(
    private val drawGroupRepository: DrawGroupRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DrawGroupsState())
    val state: StateFlow<DrawGroupsState> = _state.asStateFlow()


    init {
        loadDrawGroups()
        migrateLegacyData()
    }

    fun onEvent(event: DrawGroupsEvent) {
        when (event) {
            is DrawGroupsEvent.OnTabChanged -> {
                _state.update { it.copy(selectedTab = event.tab) }
            }

            is DrawGroupsEvent.OnGroupClick -> {
                // Navigation is handled in the Screen composable directly
            }

            is DrawGroupsEvent.OnDeleteGroup -> {
                _state.update {
                    it.copy(
                        showDeleteDialog = true,
                        groupToDelete = event.group
                    )
                }
            }

            is DrawGroupsEvent.OnConfirmDelete -> {
                deleteGroup()
            }

            is DrawGroupsEvent.OnCancelDelete -> {
                _state.update {
                    it.copy(
                        showDeleteDialog = false,
                        groupToDelete = null
                    )
                }
            }

            is DrawGroupsEvent.OnErrorDismiss -> {
                _state.update { it.copy(error = null) }
            }

            is DrawGroupsEvent.OnRefresh -> {
                loadDrawGroups()
            }
        }
    }

    private fun loadDrawGroups() {
        viewModelScope.launch {
            drawGroupRepository.getAllDrawGroups().collectLatest { result ->
                when (result) {
                    is ResultState.Loading -> {
                        _state.update { it.copy(isLoading = true) }
                    }

                    is ResultState.Success -> {
                        val allGroups = result.data
                        val activeGroups = allGroups.filter { it.isActive && !it.isCompleted }
                        val completedGroups = allGroups.filter { it.isCompleted }

                        _state.update {
                            it.copy(
                                isLoading = false,
                                allGroups = allGroups,
                                activeGroups = activeGroups,
                                completedGroups = completedGroups,
                                error = null
                            )
                        }
                    }

                    is ResultState.Error -> {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = result.message
                            )
                        }
                    }

                    is ResultState.Idle -> {
                        // Do nothing
                    }
                }
            }
        }
    }

    private fun deleteGroup() {
        val groupToDelete = _state.value.groupToDelete ?: return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            when (val result = drawGroupRepository.deleteDrawGroup(groupToDelete.id)) {
                is ResultState.Success -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            showDeleteDialog = false,
                            groupToDelete = null
                        )
                    }
                    // Reload groups after successful deletion
                    loadDrawGroups()
                }

                is ResultState.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = result.message,
                            showDeleteDialog = false,
                            groupToDelete = null
                        )
                    }
                }

                else -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            showDeleteDialog = false,
                            groupToDelete = null
                        )
                    }
                }
            }
        }
    }

    private fun migrateLegacyData() {
        viewModelScope.launch {
            try {
                drawGroupRepository.migrateLegacyData()
            } catch (e: Exception) {
                // Migration error can be logged but not shown to user
                android.util.Log.e("DrawGroupsViewModel", "Migration error", e)
            }
        }
    }

    fun getFilteredGroups(): List<DrawGroup> {
        return when (_state.value.selectedTab) {
            DrawGroupTab.ACTIVE -> _state.value.activeGroups
            DrawGroupTab.COMPLETED -> _state.value.completedGroups
            DrawGroupTab.ALL -> _state.value.allGroups
        }
    }
}