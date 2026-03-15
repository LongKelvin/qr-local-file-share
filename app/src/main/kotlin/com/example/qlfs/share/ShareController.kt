package com.example.qlfs.share

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShareController @Inject constructor() {
    
    private val _downloadCount = MutableStateFlow(0)
    val downloadCount: StateFlow<Int> = _downloadCount.asStateFlow()

    private val _serverError = MutableStateFlow<String?>(null)
    val serverError: StateFlow<String?> = _serverError.asStateFlow()

    private val _serviceRunning = MutableStateFlow(false)
    val serviceRunning: StateFlow<Boolean> = _serviceRunning.asStateFlow()

    var activeSession: SessionManager? = null

    fun onTransferEvent(event: TransferEvent) {
        when (event) {
            is TransferEvent.Completed -> {
                _downloadCount.value += 1
            }
            is TransferEvent.Failed -> {}
            is TransferEvent.Started -> {}
        }
    }

    fun reportServerError(message: String) {
        _serverError.value = message
    }

    fun setServiceRunning(isRunning: Boolean) {
        _serviceRunning.value = isRunning
    }

    fun reset() {
        _downloadCount.value = 0
        _serverError.value = null
        activeSession = null
        _serviceRunning.value = false
    }
}
