package ru.tgmaksim.activium.pages.settings

import ru.tgmaksim.activium.pages.UiText
import ru.tgmaksim.activium.api.MyReviewResult

sealed class ReviewState {
    data object Null : ReviewState()
    data object Loading : ReviewState()
    data class Error(val message: UiText, val unauthorized: Boolean = false) : ReviewState()
    data class Success(val data: MyReviewResult) : ReviewState()
}