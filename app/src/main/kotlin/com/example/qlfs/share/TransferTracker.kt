package com.example.qlfs.share

sealed class TransferEvent {
    object Started : TransferEvent()
    object Completed : TransferEvent()
    data class Failed(val error: Throwable) : TransferEvent()
}
