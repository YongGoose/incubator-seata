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

import java.util.zip.GZIPInputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class GzipUtilTest {

    @Test
    public void test_compress() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            GzipUtil.compress(null);
        });

        byte[] compress = GzipUtil.compress("aa".getBytes());
        int head = ((int) compress[0] & 0xff) | ((compress[1] << 8) & 0xff00);
        Assertions.assertEquals(GZIPInputStream.GZIP_MAGIC, head);
    }

    @Test
    public void test_decompress() {

        Assertions.assertThrows(NullPointerException.class, () -> {
            GzipUtil.decompress(null);
        });

        Assertions.assertThrows(RuntimeException.class, () -> {
            GzipUtil.decompress(new byte[0]);
        });

        Assertions.assertThrows(RuntimeException.class, () -> {
            byte[] bytes = {0x1, 0x2};
            GzipUtil.decompress(bytes);
        });
    }

    @Test
    public void test_compressEqualDecompress() {

        byte[] compress = GzipUtil.compress("aa".getBytes());

        byte[] decompress = GzipUtil.decompress(compress);

        Assertions.assertEquals("aa", new String(decompress));
    }

    @Test
    public void test_compressWithFlag_nullInput() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            GzipUtil.compressWithFlag(null, 1024);
        });
    }

    @Test
    public void test_compressWithFlag_smallInput() {
        String smallData = "small";
        byte[] result = GzipUtil.compressWithFlag(smallData.getBytes(), 100);

        Assertions.assertEquals(0, result[0]); // uncompressed flag

        byte[] originalData = new byte[result.length - 1];
        System.arraycopy(result, 1, originalData, 0, originalData.length);

        Assertions.assertEquals(smallData, new String(originalData));
    }

    @Test
    public void test_compressWithFlag_largeInput() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 2000; i++) {
            sb.append("a");
        }
        String largeData = sb.toString();
        byte[] result = GzipUtil.compressWithFlag(largeData.getBytes(), 1000);

        Assertions.assertEquals((byte) 0x02, result[0]);
        Assertions.assertEquals((byte) 0x9D, result[1]);
    }

    @Test
    public void test_decompressWithFlag_nullInput() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            GzipUtil.decompressWithFlag(null);
        });
    }

    @Test
    public void test_decompressWithFlag_emptyInput() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            GzipUtil.decompressWithFlag(new byte[0]);
        });
    }

    @Test
    public void test_decompressWithFlag_uncompressedData() {
        String original = "uncompressed data";
        byte[] data = original.getBytes();

        byte[] flaggedData = new byte[data.length + 1];
        flaggedData[0] = 0; // uncompressed flag

        System.arraycopy(data, 0, flaggedData, 1, data.length);
        byte[] result = GzipUtil.decompressWithFlag(flaggedData);
        Assertions.assertEquals(original, new String(result));
    }

    @Test
    public void test_decompressWithFlag_compressedData() {
        String original = "This is data that will be compressed with a flag byte added";
        byte[] compressed = GzipUtil.compress(original.getBytes());

        byte[] flaggedData = new byte[compressed.length + 2];
        flaggedData[0] = (byte) 0x02;
        flaggedData[1] = (byte) 0x9D;
        System.arraycopy(compressed, 0, flaggedData, 2, compressed.length);

        byte[] result = GzipUtil.decompressWithFlag(flaggedData);

        Assertions.assertEquals(original, new String(result));
    }

    @Test
    public void test_roundTrip_compressWithFlag() {
        String[] testData = {
            "small data",
            "medium sized data that might be close to the threshold",
            String.join("", java.util.Collections.nCopies(1000, "large data "))
        };

        for (String data : testData) {
            byte[] original = data.getBytes();
            byte[] withFlag = GzipUtil.compressWithFlag(original, 100);
            byte[] restored = GzipUtil.decompressWithFlag(withFlag);

            Assertions.assertEquals(data, new String(restored));
        }
    }

    @Test
    public void test_hasCompressionFlag() {
        Assertions.assertFalse(GzipUtil.hasCompressionFlag(null));
        Assertions.assertFalse(GzipUtil.hasCompressionFlag(new byte[0]));

        byte[] withCompressedFlag = new byte[] {(byte) 0x02, (byte) 0x9D, 0, 0};
        Assertions.assertTrue(GzipUtil.hasCompressionFlag(withCompressedFlag));

        byte[] withUncompressedFlag = new byte[] {0, 0, 0, 0};
        Assertions.assertFalse(GzipUtil.hasCompressionFlag(withUncompressedFlag));
    }

    @Test
    public void test_thresholdBehavior() {
        byte[] lessThreshold = new byte[99];
        byte[] result = GzipUtil.compressWithFlag(lessThreshold, 100);

        Assertions.assertEquals(0, result[0]);

        byte[] overThreshold = new byte[101];
        result = GzipUtil.compressWithFlag(overThreshold, 100);

        Assertions.assertEquals((byte) 0x02, result[0]);
        Assertions.assertEquals((byte) 0x9D, result[1]);
    }
}
