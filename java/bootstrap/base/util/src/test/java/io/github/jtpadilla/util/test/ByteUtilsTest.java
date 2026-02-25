package io.github.jtpadilla.util.test;

import io.github.jtpadilla.util.BytesUtils;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class ByteUtilsTest {

    @Test
    public void testToHex() {
        assertEquals("01 02 03", BytesUtils.toSpacedHex(new byte[] {1, 2, 3 } ));
    }

    @Test
    public void testToCompactHex() {
        assertEquals("010203", BytesUtils.toHex(new byte[] {1, 2, 3 } ));
    }

    @Test
    public void testToBytes() {
        byte[] expected = new byte[] {1, 2, 3};
        assertArrayEquals(expected, BytesUtils.toBytes("010203"));
        assertArrayEquals(expected, BytesUtils.toBytes("01 02 03"));
        assertArrayEquals(expected, BytesUtils.toBytes("01 02  03"));
    }

}
