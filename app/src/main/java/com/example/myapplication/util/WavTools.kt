package com.example.myapplication.util

import android.media.AudioFormat
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WavTools() {
//    Taken from https://gist.github.com/kmark/d8b1b01fb0d2febf5770
    companion object {

        /**
         * Writes the proper 44-byte RIFF/WAVE header to/for the given stream
         * Two size fields are left empty/null since we do not yet know the final stream size
         *
         * @param out         The stream to write the header to
         * @param channelMask An AudioFormat.CHANNEL_* mask
         * @param sampleRate  The sample rate in hertz
         * @param encoding    An AudioFormat.ENCODING_PCM_* value
         * @throws IOException
         */
//        @Throws(IOException::class)
        fun writeWavHeader(
            out: FileOutputStream,
            channelMask: Int,
            sampleRate: Int,
            encoding: Int
        ) {
            val channels: Short
            channels = when (channelMask) {
                AudioFormat.CHANNEL_IN_MONO -> 1
                AudioFormat.CHANNEL_IN_STEREO -> 2
                else -> throw IllegalArgumentException("Unacceptable channel mask")
            }
            val bitDepth: Short
            bitDepth = when (encoding) {
                AudioFormat.ENCODING_PCM_8BIT -> 8
                AudioFormat.ENCODING_PCM_16BIT -> 16
                AudioFormat.ENCODING_PCM_FLOAT -> 32
                else -> throw IllegalArgumentException("Unacceptable encoding")
            }
            writeWavHeader(out, channels, sampleRate, bitDepth)
        }

        /**
         * Writes the proper 44-byte RIFF/WAVE header to/for the given stream
         * Two size fields are left empty/null since we do not yet know the final stream size
         *
         * @param out        The stream to write the header to
         * @param channels   The number of channels
         * @param sampleRate The sample rate in hertz
         * @param bitDepth   The bit depth
         * @throws IOException
         */
//        @Throws(IOException::class)
        fun writeWavHeader(
            out: OutputStream,
            channels: Short,
            sampleRate: Int,
            bitDepth: Short
        ) {
            // Convert the multi-byte integers to raw bytes in little endian format as required by the spec
            val littleBytes: ByteArray = ByteBuffer
                .allocate(14)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putShort(channels)
                .putInt(sampleRate)
                .putInt(sampleRate * channels * (bitDepth / 8))
                .putShort((channels * (bitDepth / 8)).toShort())
                .putShort(bitDepth)
                .array()

            // Not necessarily the best, but it's very easy to visualize this way
            out.write(
                byteArrayOf( // RIFF header
                    'R'.toByte(),
                    'I'.toByte(),
                    'F'.toByte(),
                    'F'.toByte(),  // ChunkID
                    0,
                    0,
                    0,
                    0,  // ChunkSize (must be updated later)
                    'W'.toByte(),
                    'A'.toByte(),
                    'V'.toByte(),
                    'E'.toByte(),  // Format
                    // fmt subchunk
                    'f'.toByte(),
                    'm'.toByte(),
                    't'.toByte(),
                    ' '.toByte(),  // Subchunk1ID
                    16,
                    0,
                    0,
                    0,  // Subchunk1Size
                    1,
                    0,  // AudioFormat
                    littleBytes[0],
                    littleBytes[1],  // NumChannels
                    littleBytes[2],
                    littleBytes[3],
                    littleBytes[4],
                    littleBytes[5],  // SampleRate
                    littleBytes[6],
                    littleBytes[7],
                    littleBytes[8],
                    littleBytes[9],  // ByteRate
                    littleBytes[10],
                    littleBytes[11],  // BlockAlign
                    littleBytes[12],
                    littleBytes[13],  // BitsPerSample
                    // data subchunk
                    'd'.toByte(),
                    'a'.toByte(),
                    't'.toByte(),
                    'a'.toByte(),  // Subchunk2ID
                    0,
                    0,
                    0,
                    0
                )
            )
        }

        /**
         * Updates the given wav file's header to include the final chunk sizes
         *
         * @param wav The wav file to update
         * @throws IOException
         */
//        @Throws(IOException::class)
        fun updateWavHeader(wav: File) {
            val sizes: ByteArray = ByteBuffer
                .allocate(8)
                .order(ByteOrder.LITTLE_ENDIAN) // There are probably a bunch of different/better ways to calculate
                // these two given your circumstances. Cast should be safe since if the WAV is
                // > 4 GB we've already made a terrible mistake.
                .putInt((wav.length() - 8).toInt() as Int) // ChunkSize
                .putInt((wav.length() - 44).toInt() as Int) // Subchunk2Size
                .array()
            var accessWave: RandomAccessFile? = null
            try {
                accessWave = RandomAccessFile(wav, "rw")
                // ChunkSize
                accessWave.seek(4)
                accessWave.write(sizes, 0, 4)

                // Subchunk2Size
                accessWave.seek(40)
                accessWave.write(sizes, 4, 4)
            } catch (ex: IOException) {
                // Rethrow but we still close accessWave in our finally
                throw ex
            } finally {
                if (accessWave != null) {
                    try {
                        accessWave.close()
                    } catch (ex: IOException) {
                        //
                    }
                }
            }
        }
    }

}
