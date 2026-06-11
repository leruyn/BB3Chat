package com.bb3.bb3chat.presentation.base

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface UiState
interface UiEvent
interface UiEffect

abstract class BaseViewModel<S : UiState, E : UiEvent, F : UiEffect>(
    initialState: S
) {
    protected val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()
    val currentState: S get() = _state.value

    private val _effect = Channel<F>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun handleEvent(event: E) {
        scope.launch { onEvent(event) }
    }

    protected abstract suspend fun onEvent(event: E)

    protected fun updateState(reducer: S.() -> S) = _state.update(reducer)

    protected fun emitEffect(effect: F) {
        scope.launch { _effect.send(effect) }
    }

    open fun onCleared() = scope.cancel()
}
