package ro.banking40.team01.onboarding

import org.junit.Assert.assertEquals
import org.junit.Test

class AccountTest {

    @Test
    fun initialsUseFirstAndLastName() {
        assertEquals("AP", initials("ANA-MARIA ELENA  POPESCU"))
        assertEquals("A", initials("Ana"))
    }

    @Test
    fun noNameNoInitials() {
        assertEquals("", initials(null))
        assertEquals("", initials("  "))
    }
}
