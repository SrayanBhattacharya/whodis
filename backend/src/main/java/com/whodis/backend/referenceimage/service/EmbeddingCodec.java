package com.whodis.backend.referenceimage.service;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

public final class EmbeddingCodec {
    private EmbeddingCodec() {
    }

    public static byte[] encode(List<Float> embedding) {
        ByteBuffer buffer =
                ByteBuffer.allocate(embedding.size() * Float.BYTES)
                        .order(ByteOrder.LITTLE_ENDIAN);

        for (Float value : embedding) {
            buffer.putFloat(value);
        }

        return buffer.array();
    }

    public static float[] decode(byte[] embedding) {
        if (embedding == null || embedding.length % Float.BYTES != 0) {
            throw new IllegalArgumentException("Invalid embedding byte array");
        }

        ByteBuffer buffer = ByteBuffer
                .wrap(embedding)
                .order(ByteOrder.LITTLE_ENDIAN);

        float[] result = new float[embedding.length / Float.BYTES];

        for (int i = 0; i < result.length; i++) {
            result[i] = buffer.getFloat();
        }

        return result;
    }
}
