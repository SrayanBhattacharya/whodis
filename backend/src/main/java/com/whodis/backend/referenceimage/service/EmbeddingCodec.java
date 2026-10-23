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
}
