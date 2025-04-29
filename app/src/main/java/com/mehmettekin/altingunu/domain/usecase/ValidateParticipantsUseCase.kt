package com.mehmettekin.altingunu.domain.usecase

import com.mehmettekin.altingunu.R
import com.mehmettekin.altingunu.domain.model.Participant
import com.mehmettekin.altingunu.utils.ResultState
import com.mehmettekin.altingunu.utils.UiText
import javax.inject.Inject

class ValidateParticipantsUseCase @Inject constructor() {
    operator fun invoke(participants: List<Participant>): ResultState<List<Participant>> {
        // Check if there are any participants
        // Check if there are enough participants (at least 2)
        if (participants.isEmpty() || participants.size < 2) {
            return ResultState.Error(UiText.stringResource(R.string.error_min_participants))
        }

        // Check for duplicate names
        val nameSet = mutableSetOf<String>()
        for (participant in participants) {
            if (!nameSet.add(participant.name.lowercase())) {
                return ResultState.Error(UiText.stringResource(R.string.error_duplicate_names))
            }
        }

        // Check for empty names
        if (participants.any { it.name.isBlank() }) {
            return ResultState.Error(UiText.stringResource(R.string.error_empty_names))
        }

        // All validations passed
        return ResultState.Success(participants)
    }
}
