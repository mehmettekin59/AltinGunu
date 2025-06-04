package com.mehmettekin.altingunu.presentation.drawgroupscreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mehmettekin.altingunu.R
import com.mehmettekin.altingunu.domain.model.DrawGroup
import com.mehmettekin.altingunu.domain.repository.DrawGroupRepository
import com.mehmettekin.altingunu.utils.ResultState
import com.mehmettekin.altingunu.utils.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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

    private val _navigationEvent = MutableSharedFlow<DrawGroupsNavigation>()
    val navigationEvent: SharedFlow<DrawGroupsNavigation> = _navigationEvent.asSharedFlow()

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
                viewModelScope.launch {
                    // Gruba tıklandığında participants ekranına git
                    _navigationEvent.emit(DrawGroupsNavigation.ToParticipants(event.group.id))
                }
            }

            is DrawGroupsEvent.OnDeleteGroup -> {
                _state.update {
                    it.copy(
                        showDeleteDialog = true,
                        groupToDelete = event.group
                    )
                }
            }

            is DrawGroupsEvent.OnCreateNewGroup -> {
                createNewGroup(event.name, event.description)
            }

            is DrawGroupsEvent.OnGroupNameChanged -> {
                _state.update { it.copy(newGroupName = event.name) }
            }

            is DrawGroupsEvent.OnGroupDescriptionChanged -> {
                _state.update { it.copy(newGroupDescription = event.description) }
            }

            is DrawGroupsEvent.OnShowCreateDialog -> {
                _state.update {
                    it.copy(
                        showCreateGroupDialog = true,
                        newGroupName = "",
                        newGroupDescription = ""
                    )
                }
            }

            is DrawGroupsEvent.OnHideCreateDialog -> {
                _state.update {
                    it.copy(
                        showCreateGroupDialog = false,
                        newGroupName = "",
                        newGroupDescription = ""
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

    private fun createNewGroup(name: String, description: String) {
        if (name.isBlank()) {
            _state.update {
                it.copy(error = UiText.stringResource(R.string.error_empty_names))
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            // Yeni grup için participants ekranına git
            _state.update {
                it.copy(
                    isLoading = false,
                    showCreateGroupDialog = false,
                    newGroupName = "",
                    newGroupDescription = ""
                )
            }

            _navigationEvent.emit(DrawGroupsNavigation.ToCreateNewGroup(name, description))
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
                    // Silme başarılı olduğunda liste otomatik güncellenecek (Flow sayesinde)
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
                // Migration hatası loglanabilir ama kullanıcıya gösterilmez
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