package io.github.jtpadilla.protobuf;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;

public class ProtoJsonUtil {

    static public String toJson(Message message) {
        JsonFormat.Printer printer = JsonFormat
                .printer()
//                .preservingProtoFieldNames()
                .alwaysPrintFieldsWithNoPresence();
        try {
            return printer.print(message);
        } catch (InvalidProtocolBufferException e) {
            return String.format("{%n \"error\": \"Can't convert ProtoMessage to JSON\", %n\"exception\": \"%s\"%n}", e.getMessage());
        }
    }

    static public String toJsonOneLine(Message message) {
        JsonFormat.Printer printer = JsonFormat
                .printer()
//                .preservingProtoFieldNames()
                .alwaysPrintFieldsWithNoPresence()
                .omittingInsignificantWhitespace();
        try {
            return printer.print(message);
        } catch (InvalidProtocolBufferException e) {
            return String.format("{ \"error\": \"Can't convert ProtoMessage to JSON\", \"exception\": \"%s\"}", e.getMessage());
        }
    }
}
