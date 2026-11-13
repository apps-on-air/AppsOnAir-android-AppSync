package com.appsonair.appsync.services

import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadImageTaskTest {

    @Test
    fun smallImageIsNotDownsampled() {
        assertEquals(1, DownloadImageTask.calculateInSampleSize(200, 200, 280, 280))
    }

    @Test
    fun largeImageIsDownsampledToAtLeastTargetSize() {
        val sample = DownloadImageTask.calculateInSampleSize(3000, 3000, 280, 280)
        assertEquals(8, sample)
        // Decoded size must still cover the target so the icon isn't blurry.
        assert(3000 / sample >= 280)
    }

    @Test
    fun nonSquareImageUsesSmallerDimension() {
        assertEquals(4, DownloadImageTask.calculateInSampleSize(4000, 1200, 280, 280))
    }
}
