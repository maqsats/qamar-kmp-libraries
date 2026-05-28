package com.qamar.quran.translations.database

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.usePinned
import platform.zlib.MAX_WBITS
import platform.zlib.Z_FINISH
import platform.zlib.Z_OK
import platform.zlib.Z_STREAM_END
import platform.zlib.inflate
import platform.zlib.inflateEnd
import platform.zlib.inflateInit2_
import platform.zlib.z_stream_s

/**
 * Extracts the first file entry from a ZIP archive using zlib raw-deflate.
 * ZIP compression method 8 = DEFLATE without zlib header (negative windowBits).
 * Method 0 = stored (no compression).
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal object IosZipExtractor {

    // ZIP local file header field readers (little-endian)
    private fun ByteArray.u16(offset: Int): Int =
        (this[offset].toInt() and 0xFF) or ((this[offset + 1].toInt() and 0xFF) shl 8)

    private fun ByteArray.u32(offset: Int): Long =
        (this[offset].toLong() and 0xFF) or
            ((this[offset + 1].toLong() and 0xFF) shl 8) or
            ((this[offset + 2].toLong() and 0xFF) shl 16) or
            ((this[offset + 3].toLong() and 0xFF) shl 24)

    fun extractFirstEntry(bytes: ByteArray): ByteArray? {
        // Local file header: signature PK\x03\x04 = 0x04034b50
        if (bytes.size < 30 || bytes.u32(0) != 0x04034b50L) return null

        val compressionMethod = bytes.u16(8)
        val compressedSize    = bytes.u32(18)
        val uncompressedSize  = bytes.u32(22)
        val fileNameLen       = bytes.u16(26)
        val extraLen          = bytes.u16(28)
        val dataOffset        = 30 + fileNameLen + extraLen

        if (dataOffset >= bytes.size) return null

        return when (compressionMethod) {
            0 -> {
                // Stored – copy raw bytes
                val size = if (compressedSize > 0L) compressedSize.toInt()
                           else bytes.size - dataOffset
                bytes.copyOfRange(dataOffset, (dataOffset + size).coerceAtMost(bytes.size))
            }
            8 -> inflateRaw(bytes, dataOffset, uncompressedSize.toInt())
            else -> null
        }
    }

    /**
     * Inflates raw DEFLATE-compressed data (no zlib header, windowBits = -MAX_WBITS).
     * Retries with a doubling buffer up to 4 times in case uncompressedSize was
     * unavailable in the header (data-descriptor ZIP variant).
     */
    private fun inflateRaw(bytes: ByteArray, dataOffset: Int, uncompressedSize: Int): ByteArray? {
        val compressedLength = bytes.size - dataOffset
        if (compressedLength <= 0) return null

        var outputSize = if (uncompressedSize > 0) uncompressedSize
                         else maxOf(compressedLength * 4, 4 * 1024 * 1024)

        repeat(4) {
            val output = ByteArray(outputSize)
            var zlibResult = -999
            var produced = 0

            bytes.usePinned { inPin ->
                output.usePinned { outPin ->
                    memScoped {
                        // alloc<z_stream_s>() zero-initialises the struct:
                        // zalloc / zfree / opaque = null → zlib uses its default allocator
                        val stream = alloc<z_stream_s>()
                        stream.avail_in  = compressedLength.toUInt()
                        stream.next_in   = inPin.addressOf(dataOffset).reinterpret()
                        stream.avail_out = outputSize.toUInt()
                        stream.next_out  = outPin.addressOf(0).reinterpret()

                        // -MAX_WBITS → raw deflate (no zlib wrapper header)
                        // inflateInit2_ takes version: String? — Kotlin Native converts it
                        val initRes = inflateInit2_(
                            stream.ptr,
                            -MAX_WBITS,
                            "1.2.11",
                            sizeOf<z_stream_s>().toInt(),
                        )
                        if (initRes == Z_OK) {
                            zlibResult = inflate(stream.ptr, Z_FINISH)
                            produced   = outputSize - stream.avail_out.toInt()
                            inflateEnd(stream.ptr)
                        }
                    }
                }
            }

            if (zlibResult == Z_STREAM_END && produced > 0) {
                return output.copyOf(produced)
            }
            outputSize *= 2 // buffer was too small — retry with double the space
        }
        return null
    }
}
