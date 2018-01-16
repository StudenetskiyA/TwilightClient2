package com.shi.dayre.twilightclient2

import android.util.Log
import org.testng.annotations.Test

import org.testng.Assert.*

/**
 * Created by samsung on 16.01.2018.
 */
class CommonsKtTest {
    @Test
    fun testMetrToGradus() {
        assertEquals(metrToGradusToString("0.111197".toInt()),"0.1")
    }

}