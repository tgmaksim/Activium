package ru.tgmaksim.activium.ui.core

import androidx.lifecycle.ViewModel
import io.ktor.network.tls.TlsException
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.CancellationException

import ru.tgmaksim.activium.R
import ru.tgmaksim.activium.api.Request
import ru.tgmaksim.activium.api.ApiResponse
import ru.tgmaksim.activium.utilities.Utilities

open class UiViewModel : ViewModel() {
    protected suspend fun <R : ApiResponse, T> executeRequest(
        state: MutableStateFlow<LoadState<T>>,
        apiName: String,
        errorRes: Int,
        request: suspend () -> R,
        mapSuccess: (R) -> T?,
        onSuccess: suspend (T) -> Unit = {}
    ) {
        executeRequest(state, {}, apiName, errorRes, request, mapSuccess, onSuccess)
    }

    protected suspend fun <R : ApiResponse, T> executeRequest(
        state: MutableStateFlow<LoadState<T>>,
        onNewState: (LoadState<Nothing>) -> Unit,
        apiName: String,
        errorRes: Int,
        request: suspend () -> R,
        mapSuccess: (R) -> T?,
        onSuccess: suspend (T) -> Unit = {}
    ) {
        onNewState(state.setLoading())
        var loading = true

        try {
            val response = request()
            val answer = mapSuccess(response)

            if (!response.status || answer == null) {
                if (response.error != null)
                    Utilities.log("API error(${response.error?.type}) at $apiName: ${response.error?.errorMessage}")

                val unauthorized = response.error?.type == "UnauthorizedError"
                val message = response.error?.errorMessage?.let {
                    UiText.DynamicString(it)
                } ?: UiText.StringResource(errorRes)

                onNewState(state.setError(message, unauthorized))
                loading = false
            } else {
                state.setSuccess(answer)
                loading = false
                onSuccess(answer)
            }
        } catch (_: CancellationException) {
            onNewState(state.setEmpty())
            loading = false
        } catch (_: TlsException) {
            onNewState(state.setError(UiText.StringResource(R.string.error_tls)))
            loading = false
        } catch (e: Exception) {
            Utilities.log(e)
            val messageRes = if (!Request.checkInternet()) R.string.error_internet else errorRes
            onNewState(state.setError(UiText.StringResource(messageRes)))
            loading = false
        } finally {
            if (loading)
                onNewState(state.setError(UiText.StringResource(errorRes)))
        }
    }
}