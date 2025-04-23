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

public class GzipUtil {

    private GzipUtil() {}

    private static final int BUFFER_SIZE = 8192;

    private static final byte COMPRESSED_FLAG = 1;
    private static final byte UNCOMPRESSED_FLAG = 0;

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
     * This method expects the first byte to be a compression flag.
     *
     * @param bytes the bytes with compression flag
     * @return the decompressed data
     */
    public static byte[] decompressWithFlag(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new NullPointerException("bytes is null or empty");
        }

        boolean isCompressed = bytes[0] == COMPRESSED_FLAG;
        byte[] data = new byte[bytes.length - 1];
        System.arraycopy(bytes, 1, data, 0, data.length);

        return isCompressed ? decompress(data) : data;
    }

    /**
     * Add a compression flag byte to the beginning of the data.
     *
     * @param bytes the original data
     * @param isCompressed whether the data is compressed
     * @return byte array with flag + data
     */
    private static byte[] addCompressionFlag(byte[] bytes, boolean isCompressed) {
        byte[] result = new byte[bytes.length + 1];
        result[0] = isCompressed ? COMPRESSED_FLAG : UNCOMPRESSED_FLAG;
        System.arraycopy(bytes, 0, result, 1, bytes.length);
        return result;
    }

    /**
     * Check if the data has the compression flag and is marked as compressed.
     * This is the preferred method to check compression status when using the new format.
     *
     * @param bytes the bytes to check
     * @return true if the first byte indicates the data is compressed
     */
    public static boolean hasCompressionFlag(byte[] bytes) {
        return bytes != null && bytes.length > 0 && bytes[0] == COMPRESSED_FLAG;
    }
}
