package dev.drperky.utils;

import java.io.IOException;
import java.nio.ByteBuffer;

public class ByteBufferExtras {
    public static String readUtf(ByteBuffer buffer, int maxLength) {
        short stringLength = buffer.getShort();
        if (stringLength > maxLength || stringLength <= 0) {
            return "";
        }

        StringBuilder builder = new StringBuilder(stringLength);
        for (int i = 0; i < stringLength; i++) {
            char c = (char)buffer.getShort();
            builder.append(c);
        }

        return builder.toString();
    }

    public static void writeUtf(ByteBuffer buffer, String value, int maxLength) throws IOException {
        if (value == null) value = "";

        int length = value.length();
        if (length > maxLength) {
            throw new IOException("String length " + length + " exceeds max of " + maxLength);
        }

        buffer.putShort((short) length);
        for (int i = 0; i < length; i++) {
            buffer.putChar(value.charAt(i));
        }
    }
}
