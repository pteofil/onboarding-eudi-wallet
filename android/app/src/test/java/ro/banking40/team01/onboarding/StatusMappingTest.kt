package ro.banking40.team01.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class StatusMappingTest {

    @Test
    fun pendingKeepsPolling() {
        assertEquals(StatusOutcome.KeepPolling, outcomeOf("PENDING"))
    }

    @Test
    fun successStatuses() {
        assertEquals(StatusOutcome.Verified, outcomeOf("VERIFIED"))
        assertEquals(StatusOutcome.Confirmed, outcomeOf("CONFIRMED"))
    }

    @Test
    fun everyBackendFailureHasItsErrorScreen() {
        mapOf(
            "DECLINED" to ErrorKind.DECLINED,
            "EXPIRED" to ErrorKind.EXPIRED,
            "WALLET_ERROR" to ErrorKind.WALLET_ERROR,
            "NOT_VERIFIED" to ErrorKind.NOT_VERIFIED,
            "UNDER_18" to ErrorKind.UNDER_18,
            "MISSING_IDENTITY" to ErrorKind.MISSING_IDENTITY,
        ).forEach { (status, kind) -> assertEquals(status, StatusOutcome.Failed(kind), outcomeOf(status)) }
    }

    @Test
    fun unknownStatusIsNeverSuccess() {
        assertEquals(StatusOutcome.Failed(ErrorKind.NOT_VERIFIED), outcomeOf("SOMETHING_NEW"))
        assertEquals(StatusOutcome.Failed(ErrorKind.NOT_VERIFIED), outcomeOf(""))
    }

    @Test
    fun under18OffersNoRetry() {
        assertFalse(ErrorKind.UNDER_18.canRetry)
    }
}
