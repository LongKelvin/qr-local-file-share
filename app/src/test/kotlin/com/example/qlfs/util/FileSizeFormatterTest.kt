package com.example.qlfs.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FileSizeFormatterTest {

    @Test
    fun testFormat() {
        assertEquals("0 B", FileSizeFormatter.format(0))
        assertEquals("1023 B", FileSizeFormatter.format(1023))
        assertEquals("1.0 KB", FileSizeFormatter.format(1024))
        assertEquals("1.0 MB", FileSizeFormatter.format(1_048_576))
        assertEquals("1.00 GB", FileSizeFormatter.format(1_073_741_824))
        assertEquals("5.00 GB", FileSizeFormatter.format(5_368_709_120))
    }
}
