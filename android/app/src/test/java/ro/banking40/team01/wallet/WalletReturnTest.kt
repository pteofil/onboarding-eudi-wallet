package ro.banking40.team01.wallet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WalletReturnTest {

    @Test
    fun readsCodeFromFragment() {
        assertEquals("abc123", WalletReturn.responseCode("ro.banking40.team01://same-device#response_code=abc123"))
    }

    @Test
    fun readsCodeAmongOtherFragmentParams() {
        assertEquals("abc", WalletReturn.responseCode("ro.banking40.team01://same-device#state=x&response_code=abc"))
    }

    @Test
    fun fallsBackToQuery() {
        assertEquals("q1", WalletReturn.responseCode("ro.banking40.team01://same-device?response_code=q1"))
    }

    @Test
    fun fragmentWinsOverQuery() {
        assertEquals("f", WalletReturn.responseCode("ro.banking40.team01://same-device?response_code=q#response_code=f"))
    }

    @Test
    fun percentDecodesButKeepsPlus() {
        assertEquals("a/b+c=", WalletReturn.responseCode("ro.banking40.team01://same-device#response_code=a%2Fb+c%3D"))
    }

    @Test
    fun missingOrEmptyCodeIsNull() {
        assertNull(WalletReturn.responseCode("ro.banking40.team01://same-device"))
        assertNull(WalletReturn.responseCode("ro.banking40.team01://same-device#response_code="))
        assertNull(WalletReturn.responseCode("ro.banking40.team01://same-device#error=access_denied"))
    }

    @Test
    fun recognisesOnlyOurCallback() {
        assertTrue(WalletReturn.isWalletReturn("ro.banking40.team01://same-device#response_code=x"))
        assertTrue(WalletReturn.isWalletReturn("ro.banking40.team01://same-device"))
        assertFalse(WalletReturn.isWalletReturn("ro.banking40.team01://same-device-evil#response_code=x"))
        assertFalse(WalletReturn.isWalletReturn("https://example.com/#response_code=x"))
        assertFalse(WalletReturn.isWalletReturn(null))
    }
}
