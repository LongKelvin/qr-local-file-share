package com.example.qlfs.util

object FileSizeFormatter {
    fun format(bytes: Long): String = when {
        bytes < 1_024 -> "$bytes B"
        bytes < 1_048_576 -> "%.1f KB".format(bytes / 1_024.0)
        bytes < 1_073_741_824 -> "%.1f MB".format(bytes / 1_048_576.0)
        bytes < 1_099_511_627_776 -> "%.2f GB".format(bytes / 1_073_741_824.0)
        else -> "%.2f TB".format(bytes / 1_099_511_627_776.0)
    }
}
