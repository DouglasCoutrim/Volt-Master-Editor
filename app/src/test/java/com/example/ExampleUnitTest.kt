package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testLittleEndianUInt16ReadAndWrite() {
        val buffer = ByteArray(MIN_DUMP_SIZE)

        // Write 25 (0x0019) -> Low byte 0x19, High byte 0x00
        writeUInt16LE(buffer, OFFSET_PRIMARY_CYCLES, 25)
        assertEquals(0x19.toByte(), buffer[OFFSET_PRIMARY_CYCLES])
        assertEquals(0x00.toByte(), buffer[OFFSET_PRIMARY_CYCLES + 1])

        val readCycles = readUInt16LE(buffer, OFFSET_PRIMARY_CYCLES)
        assertEquals(25, readCycles)

        // Write reference cycle (25 + 1 = 26 -> 0x001A)
        writeUInt16LE(buffer, OFFSET_PRIMARY_REF, 26)
        assertEquals(0x1A.toByte(), buffer[OFFSET_PRIMARY_REF])
        assertEquals(0x00.toByte(), buffer[OFFSET_PRIMARY_REF + 1])
        assertEquals(26, readUInt16LE(buffer, OFFSET_PRIMARY_REF))

        // Check format string: "19 00"
        assertEquals("19 00", formatUInt16LEHex(25))
        assertEquals("1A 00", formatUInt16LEHex(26))
    }

    @Test
    fun testMirrorBlockCyclesAndRef() {
        val buffer = ByteArray(MIN_DUMP_SIZE)
        val cycles = 150
        val ref = cycles + 1

        writeUInt16LE(buffer, OFFSET_MIRROR_CYCLES, cycles)
        writeUInt16LE(buffer, OFFSET_MIRROR_REF, ref)

        assertEquals(cycles, readUInt16LE(buffer, OFFSET_MIRROR_CYCLES))
        assertEquals(ref, readUInt16LE(buffer, OFFSET_MIRROR_REF))
    }

    @Test
    fun testPrimaryAndSecondarySerialNumbers() {
        val buffer = ByteArray(MIN_DUMP_SIZE)

        val sn1 = "VM-BAT-99"
        writeAsciiString(buffer, OFFSET_SN1, LEN_SN1, sn1)
        val readSn1 = readAsciiString(buffer, OFFSET_SN1, LEN_SN1)
        assertEquals(sn1, readSn1)
        // Null padding check
        assertEquals(0.toByte(), buffer[OFFSET_SN1 + sn1.length])

        val sn2 = "VM-6S-PACK"
        writeAsciiString(buffer, OFFSET_SN2, LEN_SN2, sn2)
        val readSn2 = readAsciiString(buffer, OFFSET_SN2, LEN_SN2)
        assertEquals(sn2, readSn2)
        assertEquals(0.toByte(), buffer[OFFSET_SN2 + sn2.length])
    }

    @Test
    fun testHexTextAutoDetectionAndParsing() {
        // Build space-separated hex string
        val hexList = (0 until 600).map { String.format("%02X", it and 0xFF) }
        val hexString = hexList.joinToString(" ")

        val parsed = detectAndParseHexText(hexString.toByteArray(Charsets.UTF_8))
        assertNotNull(parsed)
        assertTrue(parsed!!.size >= MIN_DUMP_SIZE)
        assertEquals(0x00.toByte(), parsed[0])
        assertEquals(0x01.toByte(), parsed[1])
    }

    @Test
    fun testDemoDumpGeneration() {
        val demo = createDemoDump()
        assertTrue(demo.size >= MIN_DUMP_SIZE)

        val sn1 = readAsciiString(demo, OFFSET_SN1, LEN_SN1)
        val sn2 = readAsciiString(demo, OFFSET_SN2, LEN_SN2)
        val cycles = readUInt16LE(demo, OFFSET_PRIMARY_CYCLES)
        val mirrorCycles = readUInt16LE(demo, OFFSET_MIRROR_CYCLES)

        assertTrue(sn1.isNotEmpty())
        assertTrue(sn2.isNotEmpty())
        assertEquals(cycles, mirrorCycles)
    }
}
