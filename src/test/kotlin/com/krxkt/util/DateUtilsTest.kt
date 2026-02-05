package com.krxkt.util

import com.krxkt.error.KrxError
import kotlin.test.Test
import kotlin.test.assertFailsWith

class DateUtilsTest {

    @Test
    fun `validateDate should accept valid date format`() {
        // Should not throw
        DateUtils.validateDate("20210122")
        DateUtils.validateDate("20210101")
        DateUtils.validateDate("20211231")
    }

    @Test
    fun `validateDate should reject invalid format`() {
        assertFailsWith<KrxError.InvalidDateError> {
            DateUtils.validateDate("2021-01-22")
        }
        assertFailsWith<KrxError.InvalidDateError> {
            DateUtils.validateDate("2021/01/22")
        }
        assertFailsWith<KrxError.InvalidDateError> {
            DateUtils.validateDate("21012")
        }
        assertFailsWith<KrxError.InvalidDateError> {
            DateUtils.validateDate("")
        }
    }

    @Test
    fun `validateDate should reject invalid month`() {
        assertFailsWith<KrxError.InvalidDateError> {
            DateUtils.validateDate("20211301")
        }
        assertFailsWith<KrxError.InvalidDateError> {
            DateUtils.validateDate("20210001")
        }
    }

    @Test
    fun `validateDate should reject invalid day`() {
        assertFailsWith<KrxError.InvalidDateError> {
            DateUtils.validateDate("20210100")
        }
        assertFailsWith<KrxError.InvalidDateError> {
            DateUtils.validateDate("20210132")
        }
    }

    @Test
    fun `validateDateRange should accept valid range`() {
        // Should not throw
        DateUtils.validateDateRange("20210101", "20210131")
        DateUtils.validateDateRange("20210122", "20210122") // Same date
    }

    @Test
    fun `validateDateRange should reject start after end`() {
        assertFailsWith<KrxError.InvalidDateError> {
            DateUtils.validateDateRange("20210131", "20210101")
        }
    }
}
