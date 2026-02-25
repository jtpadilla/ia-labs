package io.github.jtpadilla.util;

import java.nio.charset.StandardCharsets;

public class BytesUtils {

    private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);

    public static String toSpacedHex(byte[] bytes) {

        if (bytes == null) {
            return "";
        } else {

            byte[] hexChars = new byte[bytes.length * 3 - 1];

            int idx = 0;
            for (int j = 0; j < bytes.length; j++) {

                int v = bytes[j] & 0xFF;

                if (j != 0) {
                    hexChars[idx++] = ' ';
                }
                hexChars[idx++] = HEX_ARRAY[v >>> 4];
                hexChars[idx++] = HEX_ARRAY[v & 0x0F];

            }
            return new String(hexChars, StandardCharsets.UTF_8);
        }
    }

    public static String toHex(byte[] bytes) {
        if (bytes == null) {
            return "";
        } else {
            byte[] hexChars = new byte[bytes.length * 2];
            int idx = 0;
            for (int j = 0; j < bytes.length; j++) {
                int v = bytes[j] & 0xFF;
                hexChars[idx++] = HEX_ARRAY[v >>> 4];
                hexChars[idx++] = HEX_ARRAY[v & 0x0F];
            }
            return new String(hexChars, StandardCharsets.UTF_8);
        }
    }

    static public byte[] toBytes(String s) {
        s = s.replaceAll("\\s", "");
        final int len = s.length();
        byte[] data = new byte[len / 2]; // Cada par de caracteres representa un byte, por lo que el tamaño es len/2
        for (int i = 0; i < len; i += 2) {
            // Convertimos cada par de caracteres hexadecimales en un byte
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }


}