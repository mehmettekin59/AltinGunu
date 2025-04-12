package com.mehmettekin.altingunu.domain.usecase

import javax.inject.Inject

class ValidateDrawSettingsUseCase @Inject constructor() {
    operator fun invoke(settings: ParticipantsScreenWholeInformation): ResultState<ParticipantsScreenWholeInformation> {
        // Check if participant count is valid
        if (settings.participantCount <= 0) {
            return ResultState.Error(UiText.stringResource(R.string.error_invalid_participant_count))
        }

        // Check if actual participants count matches expected count
        if (settings.participants.size != settings.participantCount) {
            return ResultState.Error(UiText.stringResource(R.string.error_participant_count_mismatch))
        }

        // Check if monthly amount is valid
        if (settings.monthlyAmount <= 0) {
            return ResultState.Error(UiText.stringResource(R.string.error_invalid_monthly_amount))
        }

        // Check if duration is valid
        if (settings.durationMonths <= 0) {
            return ResultState.Error(UiText.stringResource(R.string.error_invalid_duration))
        }

        // For currency and gold, check if specific item is selected
        if ((settings.itemType == ItemType.CURRENCY || settings.itemType == ItemType.GOLD)
            && settings.specificItem.isBlank()) {
            return ResultState.Error(UiText.stringResource(R.string.error_specific_item_not_selected))
        }

        // All validations passed
        return ResultState.Success(settings)
    }
}