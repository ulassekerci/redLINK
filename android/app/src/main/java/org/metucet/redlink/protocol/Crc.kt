package org.metucet.redlink.protocol

// CRC-16/XMODEM: polynomial 0x1021, initial value 0, not reflected.
fun crc16(bytes: ByteArray): Int {
    var crc = 0
    for (index in bytes.indices) {
        crc = crc xor (bytes.unsigned(index) shl 8)
        repeat(8) {
            crc = if (crc and 0x8000 != 0) ((crc shl 1) xor 0x1021) and 0xffff else (crc shl 1) and 0xffff
        }
    }
    return crc
}
