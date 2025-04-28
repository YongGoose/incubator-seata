/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.compressor.gzip;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.apache.seata.common.util.IOUtil;

public final class GzipUtil {

    private GzipUtil() {}

    private static final int BUFFER_SIZE = 8192;

    private static final byte[] COMPRESSED_FLAG = {(byte) 0x02, (byte) 0x9D};
    private static final byte UNCOMPRESSED_FLAG = 0x00;

    public static byte[] compress(byte[] bytes) {
        if (bytes == null) {
            throw new NullPointerException("bytes is null");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            gzip.write(bytes);
            gzip.flush();
            gzip.finish();
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("gzip compress error", e);
        }
    }

    public static byte[] decompress(byte[] bytes) {
        if (bytes == null) {
            throw new NullPointerException("bytes is null");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPInputStream gunzip = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int n;
            while ((n = gunzip.read(buffer)) > -1) {
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("gzip decompress error", e);
        }
    }

    /**
     * Compress the data if it exceeds the threshold and add a flag byte to indicate compression status.
     * This method adds a single byte at the beginning of the data to indicate whether it is compressed.
     *
     * @param bytes the bytes to potentially compress
     * @param threshold the threshold in bytes. If the data size is below this, no compression is performed
     * @return byte array with compression flag + original/compressed data
     */
    public static byte[] compressWithFlag(byte[] bytes, int threshold) {
        if (bytes == null) {
            throw new NullPointerException("bytes is null");
        }

        // Don't compress small data
        if (bytes.length < threshold) {
            return addCompressionFlag(bytes, false);
        }

        byte[] compressed = compress(bytes);

        if (compressed.length < bytes.length) {
            return addCompressionFlag(compressed, true);
        } else {
            return addCompressionFlag(bytes, false);
        }
    }

    /**
     * Decompress the data if the flag indicates it is compressed.
     * This method expects the first and second byte to be a compression flag.
     *
     * @param bytes the bytes with compression flag
     * @return the decompressed data
     */
    public static byte[] decompressWithFlag(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new NullPointerException("bytes is null or empty");
        }

        if (bytes[0] == UNCOMPRESSED_FLAG) {
            byte[] data = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, data, 0, data.length);
            return data;
        }

        if (bytes.length < 2) {
            return bytes;
        }

        if (bytes[0] == COMPRESSED_FLAG[0] && bytes[1] == COMPRESSED_FLAG[1]) {
            byte[] data = new byte[bytes.length - 2];
            System.arraycopy(bytes, 2, data, 0, data.length);
            return decompress(data);
        }

        return bytes;
    }

    /**
     * Add a compression flag byte to the beginning of the data.
     *
     * @param bytes the original data
     * @param isCompressed whether the data is compressed
     * @return byte array with flag + data
     */
    private static byte[] addCompressionFlag(byte[] bytes, boolean isCompressed) {
        if (bytes == null) {
            throw new NullPointerException("bytes is null");
        }

        byte[] result;
        if (isCompressed) {
            result = new byte[bytes.length + COMPRESSED_FLAG.length];
            result[0] = COMPRESSED_FLAG[0];
            result[1] = COMPRESSED_FLAG[1];
            System.arraycopy(bytes, 0, result, COMPRESSED_FLAG.length, bytes.length);
        } else {
            result = new byte[bytes.length + 1];
            result[0] = UNCOMPRESSED_FLAG;
            System.arraycopy(bytes, 0, result, 1, bytes.length);
        }
        return result;
    }

    /**
     * Check if the data has the compression flag and is marked as compressed.
     * This is the preferred method to check compression status when using the new format.
     *
     * @param bytes the bytes to check
     * @return true if the first byte and second byte indicates the data is compressed
     */
    public static boolean hasCompressionFlag(byte[] bytes) {
        if (bytes == null || bytes.length < 2) {
            return false;
        }
        return bytes[0] == COMPRESSED_FLAG[0] && bytes[1] == COMPRESSED_FLAG[1];
    }

    /**
     * Is compress data boolean.
     *
     * @param bytes the bytes
     * @return the boolean
     */
    public static boolean isCompressData(byte[] bytes) {
        if (bytes != null && bytes.length > 2) {
            int header = ((bytes[0] & 0xff)) | (bytes[1] & 0xff) << 8;
            return GZIPInputStream.GZIP_MAGIC == header;
        }
        return false;
    }

    /**
     * Uncompress byte [ ].
     *
     * @param src the src
     * @return the byte [ ]
     * @throws IOException the io exception
     */
    public static byte[] uncompress(final byte[] src) throws IOException {
        byte[] result;
        byte[] uncompressData = new byte[src.length];
        ByteArrayInputStream bis = new ByteArrayInputStream(src);
        GZIPInputStream iis = new GZIPInputStream(bis);
        ByteArrayOutputStream bos = new ByteArrayOutputStream(src.length);

        try {
            while (true) {
                int len = iis.read(uncompressData, 0, uncompressData.length);
                if (len <= 0) {
                    break;
                }
                bos.write(uncompressData, 0, len);
            }
            bos.flush();
            result = bos.toByteArray();
        } finally {
            IOUtil.close(bis, iis, bos);
        }
        return result;
    }
}
