package konarparti.messenger.ViewModel

import androidx.lifecycle.ViewModel
import konarparti.messenger.States.BaseState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

abstract class BaseViewModel<T : BaseState>(initial: T) : ViewModel() {
    private val _state = MutableStateFlow(initial)
    val state: StateFlow<T>
        get() = _state

    fun setState(newState: T) = _state.tryEmit(newState)
}