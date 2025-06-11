package com.mehmettekin.altingunu.presentation.screens.participantmethodscreen


@HiltViewModel
class ParticipantMethodViewModel @Inject constructor(
    private val drawGroupRepository: DrawGroupRepository,
    private val fcmRepository: FcmRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ParticipantMethodState())
    val state: StateFlow<ParticipantMethodState> = _state.asStateFlow()


    fun initializeGroup(name: String, description: String) {
        _state.update {
            it.copy(
                groupName = name,
                groupDescription = description
            )
        }
    }

    fun selectMethod(method: ParticipantMethod) {
        _state.update { it.copy(selectedMethod = method) }
    }

    fun addManualParticipant(name: String) {
        if (name.isBlank()) return

        // Check for duplicate names
        val isDuplicate = _state.value.manualParticipants.any {
            it.name.lowercase() == name.trim().lowercase()
        }

        if (isDuplicate) {
            _state.update { it.copy(error = UiText.stringResource(R.string.error_duplicate_names)) }
            return
        }

        val newParticipant = Participant(
            id = UUID.randomUUID().toString(),
            name = name.trim()
        )

        _state.update { currentState ->
            currentState.copy(
                manualParticipants = currentState.manualParticipants + newParticipant
            )
        }
    }

    fun removeManualParticipant(participant: Participant) {
        _state.update { currentState ->
            currentState.copy(
                manualParticipants = currentState.manualParticipants.filter { it.id != participant.id }
            )
        }
    }

    fun continueWithManualParticipants() {
        viewModelScope.launch {
            val participants = _state.value.manualParticipants

            if (participants.size < 2) {
                _state.update { it.copy(error = UiText.stringResource(R.string.error_min_participants)) }
                return@launch
            }

            _state.update { it.copy(isLoading = true) }

            // Create group with manual participants
            val currentDate = Calendar.getInstance()
            val newGroup = DrawGroup(
                id = UUID.randomUUID().toString(),
                name = _state.value.groupName,
                description = _state.value.groupDescription,
                createdDate = System.currentTimeMillis(),
                lastModifiedDate = System.currentTimeMillis(),
                settings = ParticipantsScreenWholeInformation(
                    participantCount = participants.size,
                    participants = participants,
                    itemType = ItemType.TL,
                    specificItem = "",
                    monthlyAmount = 0.0,
                    durationMonths = 0,
                    startDay = currentDate.get(Calendar.DAY_OF_MONTH),
                    startMonth = currentDate.get(Calendar.MONTH) + 1,
                    startYear = currentDate.get(Calendar.YEAR)
                ),
                participants = participants,
                results = emptyList(),
                isCompleted = false,
                isActive = true,
                fcmTokens = emptyList(), // Manual participants don't have FCM tokens
                currentPaymentIndex = 0
            )

            when (val result = drawGroupRepository.createDrawGroup(newGroup)) {
                is ResultState.Success -> {
                    _state.update { it.copy(isLoading = false) }
                    // Navigate to participants screen to complete the setup
                    _navigationEvent.emit(
                        ParticipantMethodNavigation.ToParticipantsScreen(result.data)
                    )
                }
                is ResultState.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
                else -> {
                    _state.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun createInviteLink() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            // First create the group
            val currentDate = Calendar.getInstance()
            val newGroup = DrawGroup(
                id = UUID.randomUUID().toString(),
                name = _state.value.groupName,
                description = _state.value.groupDescription,
                createdDate = System.currentTimeMillis(),
                lastModifiedDate = System.currentTimeMillis(),
                settings = ParticipantsScreenWholeInformation(
                    participantCount = 0, // Will be updated as participants join
                    participants = emptyList(),
                    itemType = ItemType.TL,
                    specificItem = "",
                    monthlyAmount = 0.0,
                    durationMonths = 0,
                    startDay = currentDate.get(Calendar.DAY_OF_MONTH),
                    startMonth = currentDate.get(Calendar.MONTH) + 1,
                    startYear = currentDate.get(Calendar.YEAR)
                ),
                participants = emptyList(),
                results = emptyList(),
                isCompleted = false,
                isActive = true,
                fcmTokens = emptyList(),
                currentPaymentIndex = 0
            )

            when (val groupResult = drawGroupRepository.createDrawGroup(newGroup)) {
                is ResultState.Success -> {
                    val groupId = groupResult.data

                    // Create invitation
                    when (val inviteResult = fcmRepository.createInvitationOnServer(
                        drawGroupId = groupId,
                        drawGroupName = _state.value.groupName,
                        inviterName = "Grup Yöneticisi" // You can get actual user name if available
                    )) {
                        is ResultState.Success -> {
                            val inviteCode = inviteResult.data
                            val inviteUrl = "https://altingunu.app/invite/$inviteCode"

                            _state.update { it.copy(isLoading = false) }

                            // Share the invite link
                            _navigationEvent.emit(
                                ParticipantMethodNavigation.ShareInviteLink(inviteUrl)
                            )

                            // Then navigate to participants screen
                            _navigationEvent.emit(
                                ParticipantMethodNavigation.ToParticipantsScreen(groupId)
                            )
                        }
                        is ResultState.Error -> {
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    error = inviteResult.message
                                )
                            }
                        }
                        else -> {
                            _state.update { it.copy(isLoading = false) }
                        }
                    }
                }
                is ResultState.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = groupResult.message
                        )
                    }
                }
                else -> {
                    _state.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}