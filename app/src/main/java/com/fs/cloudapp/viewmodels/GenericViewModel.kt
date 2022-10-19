package com.fs.cloudapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

open class GenericViewModel<T: GenericViewModel.GenericState>: ViewModel() {

    protected lateinit var mState: MutableStateFlow<T>
    val state: StateFlow<T>
        get() = mState

    protected fun updateState(newState: T) {
        viewModelScope.launch {
            mState.emit(value = newState)
        }
    }

    interface GenericState {
        val failureOutput: Exception?
    }
}