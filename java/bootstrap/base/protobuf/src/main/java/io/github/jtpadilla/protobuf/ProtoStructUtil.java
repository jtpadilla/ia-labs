package io.github.jtpadilla.protobuf;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;

public class ProtoStructUtil {

    static public Struct jsonToStruct(String json) {
        final Struct.Builder structBuilder = Struct.newBuilder();
        try {
            JsonFormat.parser().merge(json, structBuilder);
        } catch (InvalidProtocolBufferException e) {
            structBuilder.putFields("invalid-json", Value.newBuilder().setStringValue(json).build());
        }
        return structBuilder.build();
    }

    public static Struct messageToStruct(Message message) throws IllegalArgumentException {
        return jsonToStruct(ProtoJsonUtil.toJsonOneLine(message));
    }

}
