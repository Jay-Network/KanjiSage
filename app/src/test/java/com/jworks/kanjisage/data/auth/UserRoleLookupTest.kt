package com.jworks.kanjisage.data.auth

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class UserRoleLookupTest {
    @Test
    fun adminRoleEnablesPrivilegedUi() = runTest {
        assertTrue(hasPrivilegedRole { """[{"role":"admin"}]""" })
    }

    @Test
    fun developerRoleEnablesPrivilegedUi() = runTest {
        assertTrue(hasPrivilegedRole { """[{"role":"developer"}]""" })
    }

    @Test
    fun ordinaryRoleRemainsNonAdmin() = runTest {
        assertFalse(hasPrivilegedRole { """[{"role":"member"}]""" })
    }

    @Test
    fun emptyTableRemainsNonAdmin() = runTest {
        assertFalse(hasPrivilegedRole { "[]" })
    }

    @Test
    fun missingOrUnavailableTableRemainsNonAdmin() = runTest {
        assertFalse(hasPrivilegedRole { throw IllegalStateException("Role query unavailable") })
    }

    @Test
    fun malformedResponseRemainsNonAdmin() = runTest {
        assertFalse(hasPrivilegedRole { "not json" })
        assertFalse(hasPrivilegedRole { "{}" })
        assertFalse(hasPrivilegedRole { "[null]" })
    }

    @Test
    fun missingOrNullRoleRemainsNonAdmin() = runTest {
        assertFalse(hasPrivilegedRole { "[{}]" })
        assertFalse(hasPrivilegedRole { """[{"role":null}]""" })
    }

    @Test
    fun cancellationIsNotConvertedIntoACompletedLookup() = runTest {
        val cancelled = CancellationException("Lookup cancelled")
        try {
            hasPrivilegedRole { throw cancelled }
            fail("Cancellation must propagate")
        } catch (actual: CancellationException) {
            assertSame(cancelled, actual)
        }
    }
}
