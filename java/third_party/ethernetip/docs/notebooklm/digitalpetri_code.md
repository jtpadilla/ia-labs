# Compilación de Código: digitalpetri

Este documento contiene todo el código fuente `.java` encontrado en el directorio.

---

### `ethernet-ip/logix-services/src/main/java/com/digitalpetri/enip/logix/services/ReadTagService.java`

```java
package com.digitalpetri.enip.logix.services;

import com.digitalpetri.enip.cip.CipResponseException;
import com.digitalpetri.enip.cip.epath.EPath.PaddedEPath;
import com.digitalpetri.enip.cip.services.CipService;
import com.digitalpetri.enip.cip.structs.MessageRouterRequest;
import com.digitalpetri.enip.cip.structs.MessageRouterResponse;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;

import java.util.function.Consumer;

public class ReadTagService implements CipService<ByteBuf> {

  public static final int SERVICE_CODE = 0x4C;

  private final Consumer<ByteBuf> dataEncoder = this::encode;

  private final PaddedEPath requestPath;
  private final int elementCount;

  /**
   * Create a ReadTagService requesting 1 element at {@code requestPath}.
   *
   * @param requestPath the path to the tag to read.
   */
  public ReadTagService(PaddedEPath requestPath) {
    this(requestPath, 1);
  }

  /**
   * Create a ReadTagService requesting {@code elementCount} elements at {@code requestPath}.
   *
   * @param requestPath the path to the tag to read.
   * @param elementCount the number of elements to request.
   */
  public ReadTagService(PaddedEPath requestPath, int elementCount) {
    this.requestPath = requestPath;
    this.elementCount = elementCount;
  }

  @Override
  public void encodeRequest(ByteBuf buffer) {
    MessageRouterRequest request = new MessageRouterRequest(SERVICE_CODE, requestPath, dataEncoder);

    MessageRouterRequest.encode(request, buffer);
  }

  @Override
  public ByteBuf decodeResponse(ByteBuf buffer)
      throws PartialResponseException, CipResponseException {
    MessageRouterResponse response = MessageRouterResponse.decode(buffer);

    int generalStatus = response.getGeneralStatus();

    try {
      if (generalStatus == 0x00) {
        return decode(response);
      } else {
        throw new CipResponseException(generalStatus, response.getAdditionalStatus());
      }
    } finally {
      ReferenceCountUtil.release(response.getData());
    }
  }

  private void encode(ByteBuf buffer) {
    buffer.writeShort(elementCount);
  }

  private ByteBuf decode(MessageRouterResponse response) {
    return response.getData().retain();
  }
}

```

---

### `ethernet-ip/logix-services/src/main/java/com/digitalpetri/enip/logix/services/ReadTagFragmentedService.java`

```java
package com.digitalpetri.enip.logix.services;

import com.digitalpetri.enip.cip.CipResponseException;
import com.digitalpetri.enip.cip.epath.EPath.PaddedEPath;
import com.digitalpetri.enip.cip.services.CipService;
import com.digitalpetri.enip.cip.structs.MessageRouterRequest;
import com.digitalpetri.enip.cip.structs.MessageRouterResponse;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class ReadTagFragmentedService implements CipService<ByteBuf> {

  public static final int SERVICE_CODE = 0x52;

  private final Consumer<ByteBuf> dataEncoder = this::encode;

  private final List<ByteBuf> buffers = Collections.synchronizedList(new ArrayList<>());
  private volatile int offset = 0;

  private final PaddedEPath requestPath;
  private final int elementCount;

  public ReadTagFragmentedService(PaddedEPath requestPath) {
    this(requestPath, 1);
  }

  public ReadTagFragmentedService(PaddedEPath requestPath, int elementCount) {
    this.requestPath = requestPath;
    this.elementCount = elementCount;
  }

  @Override
  public void encodeRequest(ByteBuf buffer) {
    MessageRouterRequest request = new MessageRouterRequest(SERVICE_CODE, requestPath, dataEncoder);

    MessageRouterRequest.encode(request, buffer);
  }

  private void encode(ByteBuf buffer) {
    buffer.writeShort(elementCount);
    buffer.writeInt(offset);
  }

  @Override
  public ByteBuf decodeResponse(ByteBuf buffer)
      throws CipResponseException, PartialResponseException {
    MessageRouterResponse response = MessageRouterResponse.decode(buffer);

    int status = response.getGeneralStatus();
    ByteBuf data = response.getData();

    try {
      if (status == 0x00 || status == 0x06) {
        if (status == 0x06 && data.readableBytes() == 0) {
          throw PartialResponseException.INSTANCE;
        }

        boolean structured = data.getShort(data.readerIndex()) == 0x02A0;
        ByteBuf header = structured ? data.readSlice(4) : data.readSlice(2);
        ByteBuf fragment = data.slice().retain();

        buffers.add(fragment);
        offset += fragment.readableBytes();

        if (status == 0x00) {
          synchronized (buffers) {
            ByteBuf composite =
                Unpooled.compositeBuffer()
                    .addComponent(header.retain())
                    .addComponents(buffers)
                    .writerIndex(header.readableBytes() + offset)
                    .order(ByteOrder.LITTLE_ENDIAN);

            // Clean up so this service can be re-used...
            buffers.clear();
            offset = 0;

            return composite;
          }
        } else {
          throw PartialResponseException.INSTANCE;
        }
      } else {
        throw new CipResponseException(status, response.getAdditionalStatus());
      }
    } finally {
      ReferenceCountUtil.release(data);
    }
  }
}

```

---

### `ethernet-ip/logix-services/src/main/java/com/digitalpetri/enip/logix/services/GetSymbolInstanceAttributeListService.java`

```java
package com.digitalpetri.enip.logix.services;

import com.digitalpetri.enip.logix.structs.SymbolInstance;
import io.netty.buffer.ByteBuf;
import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;

public class GetSymbolInstanceAttributeListService
    extends GetInstanceAttributeListService<SymbolInstance> {

  private static final int SYMBOL_CLASS_ID = 0x6B;

  private static final int[] REQUESTED_ATTRIBUTES = {
    1, // symbol name
    2, // symbol type
    8 // dimensions
  };

  public GetSymbolInstanceAttributeListService(@Nullable String program) {
    super(program, SYMBOL_CLASS_ID, REQUESTED_ATTRIBUTES, new SymbolAttributesDecoder(program));
  }

  private static class SymbolAttributesDecoder implements AttributesDecoder<SymbolInstance> {

    private final String program;

    private SymbolAttributesDecoder(String program) {
      this.program = program;
    }

    @Override
    public SymbolInstance decode(int instanceId, ByteBuf buffer) {
      // attribute 1 - symbol name
      int nameLength = buffer.readUnsignedShort();
      String name = buffer.toString(buffer.readerIndex(), nameLength, StandardCharsets.US_ASCII);
      buffer.skipBytes(nameLength);

      // attribute 2 - symbol type
      int type = buffer.readUnsignedShort();

      // attribute 8 - dimensions
      int d1Size = buffer.readInt();
      int d2Size = buffer.readInt();
      int d3Size = buffer.readInt();

      return new SymbolInstance(program, name, instanceId, type, d1Size, d2Size, d3Size);
    }
  }
}

```

---

### `ethernet-ip/logix-services/src/main/java/com/digitalpetri/enip/logix/services/ReadTemplateService.java`

```java
package com.digitalpetri.enip.logix.services;

import com.digitalpetri.enip.cip.CipResponseException;
import com.digitalpetri.enip.cip.epath.EPath.PaddedEPath;
import com.digitalpetri.enip.cip.services.CipService;
import com.digitalpetri.enip.cip.structs.MessageRouterRequest;
import com.digitalpetri.enip.cip.structs.MessageRouterResponse;
import com.digitalpetri.enip.logix.structs.TemplateAttributes;
import com.digitalpetri.enip.logix.structs.TemplateInstance;
import com.digitalpetri.enip.logix.structs.TemplateMember;
import com.digitalpetri.enip.util.IntUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.util.ReferenceCountUtil;

import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

public class ReadTemplateService implements CipService<TemplateInstance> {

  public static final int SERVICE_CODE = 0x4C;

  private final List<ByteBuf> buffers = new CopyOnWriteArrayList<>();
  private volatile int totalBytesRead = 0;

  private final PaddedEPath requestPath;
  private final TemplateAttributes attributes;
  private final int symbolType;

  public ReadTemplateService(
      PaddedEPath requestPath, TemplateAttributes attributes, int symbolType) {
    this.requestPath = requestPath;
    this.attributes = attributes;
    this.symbolType = symbolType;
  }

  @Override
  public void encodeRequest(ByteBuf buffer) {
    MessageRouterRequest request =
        new MessageRouterRequest(SERVICE_CODE, requestPath, this::encode);

    MessageRouterRequest.encode(request, buffer);
  }

  @Override
  public TemplateInstance decodeResponse(ByteBuf buffer)
      throws CipResponseException, PartialResponseException {
    MessageRouterResponse response = MessageRouterResponse.decode(buffer);

    int status = response.getGeneralStatus();

    try {
      if (status == 0x00 || status == 0x06) {
        buffers.add(response.getData().retain());

        totalBytesRead += response.getData().readableBytes();

        if (status == 0x00) {
          ByteBuf composite =
              PooledByteBufAllocator.DEFAULT
                  .compositeBuffer(buffers.size())
                  .addComponents(buffers)
                  .writerIndex(totalBytesRead)
                  .order(ByteOrder.LITTLE_ENDIAN);

          TemplateInstance instance = decode(composite, symbolType);

          ReferenceCountUtil.release(composite);

          return instance;
        } else {
          throw PartialResponseException.INSTANCE;
        }
      } else {
        throw new CipResponseException(status, response.getAdditionalStatus());
      }
    } finally {
      ReferenceCountUtil.release(response.getData());
    }
  }

  private void encode(ByteBuf buffer) {
    int bytesToRead = (attributes.getObjectDefinitionSize() * 4) - 23;
    bytesToRead = roundUp(bytesToRead, 4) + 4;
    bytesToRead -= totalBytesRead;

    buffer.writeInt(totalBytesRead);
    buffer.writeShort(bytesToRead);
  }

  private TemplateInstance decode(ByteBuf buffer, int symbolType) {
    int memberCount = attributes.getMemberCount();

    List<Function<String, TemplateMember>> functions = new ArrayList<>(memberCount);

    for (int i = 0; i < memberCount; i++) {
      int infoWord = buffer.readShort();
      int memberType = buffer.readUnsignedShort();
      int offset = IntUtil.saturatedCast(buffer.readUnsignedInt());

      functions.add((name) -> new TemplateMember(name, infoWord, memberType, offset));
    }

    String templateName = readNullTerminatedString(buffer);

    if (templateName.contains(";n")) {
      templateName = templateName.substring(0, templateName.indexOf(";n"));
    }

    List<TemplateMember> members = new ArrayList<>(memberCount);

    for (int i = 0; i < memberCount; i++) {
      String memberName = readNullTerminatedString(buffer);
      if (memberName.isEmpty()) memberName = "__UnnamedMember" + i;

      TemplateMember member = functions.get(i).apply(memberName);

      members.add(member);
    }

    return new TemplateInstance(templateName, symbolType, attributes, members);
  }

  private static final Charset ASCII = Charset.forName("US-ASCII");

  private String readNullTerminatedString(ByteBuf buffer) {
    int length = buffer.bytesBefore((byte) 0x00);

    if (length == -1) {
      return "";
    } else {
      String s = buffer.toString(buffer.readerIndex(), length, ASCII);
      buffer.skipBytes(length + 1);
      return s;
    }
  }

  /**
   * Round {@code n} up to the nearest multiple {@code m}.
   *
   * @param n the number to round.
   * @param m the multiple to up to.
   * @return {@code n} rounded up to the nearest multiple {@code m}.
   */
  private int roundUp(int n, int m) {
    return ((n + m - 1) / m) * m;
  }
}

```

---

### `ethernet-ip/logix-services/src/main/java/com/digitalpetri/enip/logix/services/ReadModifyWriteTagService.java`

```java
package com.digitalpetri.enip.logix.services;

import com.digitalpetri.enip.cip.CipResponseException;
import com.digitalpetri.enip.cip.epath.EPath.PaddedEPath;
import com.digitalpetri.enip.cip.services.CipService;
import com.digitalpetri.enip.cip.structs.MessageRouterRequest;
import com.digitalpetri.enip.cip.structs.MessageRouterResponse;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;

public class ReadModifyWriteTagService implements CipService<Void> {

  public static final int SERVICE_CODE = 0x4E;

  private final PaddedEPath requestPath;
  private final MaskSize maskSize;
  private final long orMask;
  private final long andMask;

  public ReadModifyWriteTagService(
      PaddedEPath requestPath, MaskSize maskSize, long orMask, long andMask) {
    this.requestPath = requestPath;
    this.maskSize = maskSize;
    this.orMask = orMask;
    this.andMask = andMask;
  }

  @Override
  public void encodeRequest(ByteBuf buffer) {
    MessageRouterRequest request =
        new MessageRouterRequest(SERVICE_CODE, requestPath, this::encode);

    MessageRouterRequest.encode(request, buffer);
  }

  @Override
  public Void decodeResponse(ByteBuf buffer) throws CipResponseException, PartialResponseException {
    MessageRouterResponse response = MessageRouterResponse.decode(buffer);

    int generalStatus = response.getGeneralStatus();

    try {
      if (generalStatus == 0x00) {
        return null;
      } else {
        throw new CipResponseException(generalStatus, response.getAdditionalStatus());
      }
    } finally {
      ReferenceCountUtil.release(response.getData());
    }
  }

  private void encode(ByteBuf buffer) {
    switch (maskSize) {
      case ONE_BYTE:
        buffer.writeShort(1);
        buffer.writeByte((int) orMask);
        buffer.writeByte((int) andMask);
        break;
      case TWO_BYTE:
        buffer.writeShort(2);
        buffer.writeShort((int) orMask);
        buffer.writeShort((int) andMask);
        break;
      case FOUR_BYTE:
        buffer.writeShort(4);
        buffer.writeInt((int) orMask);
        buffer.writeInt((int) andMask);
        break;
      case EIGHT_BYTE:
        buffer.writeShort(8);
        buffer.writeLong(orMask);
        buffer.writeLong(andMask);
        break;
    }
  }

  public enum MaskSize {
    ONE_BYTE,
    TWO_BYTE,
    FOUR_BYTE,
    EIGHT_BYTE
  }
}

```

---

### `ethernet-ip/logix-services/src/main/java/com/digitalpetri/enip/logix/services/WriteTagService.java`

```java
package com.digitalpetri.enip.logix.services;

import com.digitalpetri.enip.cip.CipResponseException;
import com.digitalpetri.enip.cip.epath.EPath.PaddedEPath;
import com.digitalpetri.enip.cip.services.CipService;
import com.digitalpetri.enip.cip.structs.MessageRouterRequest;
import com.digitalpetri.enip.cip.structs.MessageRouterResponse;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;

public class WriteTagService implements CipService<Void> {

  public static final int SERVICE_CODE = 0x4D;

  private final PaddedEPath requestPath;
  private final boolean structured;
  private final int tagType;
  private final int elementCount;
  private final ByteBuf data;

  public WriteTagService(PaddedEPath requestPath, boolean structured, int tagType, ByteBuf data) {
    this(requestPath, structured, tagType, 1, data);
  }

  public WriteTagService(
      PaddedEPath requestPath, boolean structured, int tagType, int elementCount, ByteBuf data) {

    this.requestPath = requestPath;
    this.structured = structured;
    this.tagType = tagType;
    this.elementCount = elementCount;
    this.data = data;
  }

  @Override
  public void encodeRequest(ByteBuf buffer) {
    MessageRouterRequest request =
        new MessageRouterRequest(SERVICE_CODE, requestPath, this::encode);

    MessageRouterRequest.encode(request, buffer);
  }

  @Override
  public Void decodeResponse(ByteBuf buffer) throws CipResponseException, PartialResponseException {
    MessageRouterResponse response = MessageRouterResponse.decode(buffer);

    int generalStatus = response.getGeneralStatus();

    try {
      if (generalStatus == 0x00) {
        return null;
      } else {
        throw new CipResponseException(generalStatus, response.getAdditionalStatus());
      }
    } finally {
      ReferenceCountUtil.release(response.getData());
    }
  }

  private void encode(ByteBuf buffer) {
    if (structured) {
      buffer.writeByte(0xA0).writeByte(0x02);
    }

    buffer.writeShort(tagType);
    buffer.writeShort(elementCount);
    buffer.writeBytes(data);
  }
}

```

---

### `ethernet-ip/logix-services/src/main/java/com/digitalpetri/enip/logix/services/WriteTagFragmentedService.java`

```java
package com.digitalpetri.enip.logix.services;

import com.digitalpetri.enip.cip.CipResponseException;
import com.digitalpetri.enip.cip.epath.EPath.PaddedEPath;
import com.digitalpetri.enip.cip.services.CipService;
import com.digitalpetri.enip.cip.structs.MessageRouterRequest;
import com.digitalpetri.enip.cip.structs.MessageRouterResponse;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;

import java.util.function.Consumer;

public class WriteTagFragmentedService implements CipService<ByteBuf> {

  public static final int SERVICE_CODE = 0x53;

  private final Consumer<ByteBuf> dataEncoder = this::encode;

  private final PaddedEPath requestPath;
  private final int elementCount;
  private final boolean structured;
  private final int tagType;
  private final ByteBuf data;
  private final int offset;

  /**
   * @param requestPath {@link PaddedEPath Path} of tag
   * @param structured True if tag is structured
   * @param tagType Type of tag
   * @param elementCount Total number of elements being sent. Usually the number of bytes, but can
   *     vary based on data type.
   * @param offset Total number of bytes of data transferred in previous requests
   * @param data {@link ByteBuf Data} to be sent in request. Data should be a slice of original
   *     data, starting at offset and ending at an appropriate length for the CIP connection size.
   */
  public WriteTagFragmentedService(
      PaddedEPath requestPath,
      boolean structured,
      int tagType,
      int elementCount,
      int offset,
      ByteBuf data) {

    this.requestPath = requestPath;
    this.structured = structured;
    this.tagType = tagType;
    this.elementCount = elementCount;
    this.offset = offset;
    this.data = data;
  }

  @Override
  public void encodeRequest(ByteBuf buffer) {
    MessageRouterRequest request = new MessageRouterRequest(SERVICE_CODE, requestPath, dataEncoder);

    MessageRouterRequest.encode(request, buffer);
  }

  private void encode(ByteBuf buffer) {
    if (structured) {
      buffer.writeByte(0xA0).writeByte(0x02);
    }

    buffer.writeShort(tagType);
    buffer.writeShort(elementCount);
    buffer.writeInt(offset);
    buffer.writeBytes(data);
  }

  @Override
  public ByteBuf decodeResponse(ByteBuf buffer) throws CipResponseException {
    MessageRouterResponse response = MessageRouterResponse.decode(buffer);

    int generalStatus = response.getGeneralStatus();

    try {
      if (generalStatus == 0x00) {
        return Unpooled.EMPTY_BUFFER;
      } else {
        throw new CipResponseException(generalStatus, response.getAdditionalStatus());
      }
    } finally {
      ReferenceCountUtil.release(response.getData());
    }
  }
}

```

---

### `ethernet-ip/logix-services/src/main/java/com/digitalpetri/enip/logix/services/GetInstanceAttributeListService.java`

```java
package com.digitalpetri.enip.logix.services;

import com.digitalpetri.enip.cip.CipResponseException;
import com.digitalpetri.enip.cip.epath.DataSegment;
import com.digitalpetri.enip.cip.epath.EPath;
import com.digitalpetri.enip.cip.epath.LogicalSegment;
import com.digitalpetri.enip.cip.services.CipService;
import com.digitalpetri.enip.cip.structs.MessageRouterRequest;
import com.digitalpetri.enip.cip.structs.MessageRouterResponse;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public class GetInstanceAttributeListService<T> implements CipService<List<T>> {

  public static final int SERVICE_CODE = 0x55;

  private final List<T> instances = new CopyOnWriteArrayList<>();

  private volatile int instanceId = 0;
  private volatile int lastInstanceId = 0;

  private final String program;
  private final int classId;
  private final int[] attributes;
  private final AttributesDecoder<T> attributesDecoder;

  public GetInstanceAttributeListService(
      @Nullable String program,
      int classId,
      int @NonNull [] attributes,
      AttributesDecoder<T> attributesDecoder) {

    this.program = program;
    this.classId = classId;
    this.attributes = attributes;
    this.attributesDecoder = attributesDecoder;
  }

  @Override
  public void encodeRequest(ByteBuf buffer) {
    EPath.PaddedEPath requestPath =
        Optional.ofNullable(program)
            .map(
                p ->
                    new EPath.PaddedEPath(
                        new DataSegment.AnsiDataSegment(p),
                        new LogicalSegment.ClassId(classId),
                        new LogicalSegment.InstanceId(instanceId)))
            .orElse(
                new EPath.PaddedEPath(
                    new LogicalSegment.ClassId(classId),
                    new LogicalSegment.InstanceId(instanceId)));

    MessageRouterRequest request =
        new MessageRouterRequest(
            SERVICE_CODE,
            requestPath,
            b -> {
              b.writeShort(attributes.length);
              for (int attr : attributes) {
                b.writeShort(attr);
              }
            });

    MessageRouterRequest.encode(request, buffer);
  }

  @Override
  public List<T> decodeResponse(ByteBuf buffer)
      throws CipResponseException, PartialResponseException {
    MessageRouterResponse response = MessageRouterResponse.decode(buffer);

    int status = response.getGeneralStatus();
    ByteBuf data = response.getData();

    try {
      if (status == 0x00 || status == 0x06) {
        instances.addAll(decode(data));

        if (status == 0x00) {
          return new ArrayList<>(instances);
        } else {
          instanceId = lastInstanceId + 1;

          throw PartialResponseException.INSTANCE;
        }
      } else {
        throw new CipResponseException(status, response.getAdditionalStatus());
      }
    } finally {
      ReferenceCountUtil.release(data);
    }
  }

  private List<T> decode(ByteBuf buffer) {
    List<T> list = new ArrayList<>();

    while (buffer.isReadable()) {
      // reply data includes instanceId + requested attributes
      lastInstanceId = buffer.readInt();

      list.add(attributesDecoder.decode(lastInstanceId, buffer));
    }

    return list;
  }

  @FunctionalInterface
  interface AttributesDecoder<T> {

    /**
     * Decode the requested attributes from {@code buffer}.
     *
     * <p>The instance id has already been decoded and provided.
     *
     * @param instanceId the instanceId.
     * @param buffer the buffer containing the requested attributes.
     * @return the decoded instance and attributes.
     */
    T decode(int instanceId, ByteBuf buffer);
  }
}

```

---

### `ethernet-ip/logix-services/src/main/java/com/digitalpetri/enip/logix/structs/SymbolInstance.java`

```java
package com.digitalpetri.enip.logix.structs;

import java.util.Optional;

public final class SymbolInstance {

  private final String program;
  private final String name;
  private final int instanceId;
  private final int type;
  private final int d1Size;
  private final int d2Size;
  private final int d3Size;

  public SymbolInstance(
      String program, String name, int instanceId, int type, int d1Size, int d2Size, int d3Size) {

    this.program = program;
    this.name = name;
    this.instanceId = instanceId;
    this.type = type;
    this.d1Size = d1Size;
    this.d2Size = d2Size;
    this.d3Size = d3Size;
  }

  public Optional<String> getProgram() {
    return Optional.ofNullable(program);
  }

  public String getName() {
    return name;
  }

  public int getInstanceId() {
    return instanceId;
  }

  public int getType() {
    return type;
  }

  public int getD1Size() {
    return d1Size;
  }

  public int getD2Size() {
    return d2Size;
  }

  public int getD3Size() {
    return d3Size;
  }

  @Override
  public String toString() {
    return "SymbolInstance{"
        + "program="
        + program
        + ", name='"
        + name
        + '\''
        + ", instanceId="
        + instanceId
        + ", type="
        + type
        + '}';
  }
}

```

---

### `ethernet-ip/logix-services/src/main/java/com/digitalpetri/enip/logix/structs/TemplateInstance.java`

```java
package com.digitalpetri.enip.logix.structs;

import java.io.Serializable;
import java.util.List;

public final class TemplateInstance implements Serializable {

  private final String name;
  private final int symbolType;
  private final TemplateAttributes attributes;
  private final List<TemplateMember> members;

  public TemplateInstance(
      String name, int symbolType, TemplateAttributes attributes, List<TemplateMember> members) {

    this.name = name;
    this.symbolType = symbolType;
    this.attributes = attributes;
    this.members = members;
  }

  public String getName() {
    return name;
  }

  public int getSymbolType() {
    return symbolType;
  }

  public TemplateAttributes getAttributes() {
    return attributes;
  }

  public List<TemplateMember> getMembers() {
    return members;
  }

  public int getInstanceId() {
    return symbolType & 0x0FFF;
  }

  @Override
  public String toString() {
    return "TemplateInstance{"
        + "name='"
        + name
        + '\''
        + ", attributes="
        + attributes
        + ", members="
        + members
        + '}';
  }
}

```

---

### `ethernet-ip/logix-services/src/main/java/com/digitalpetri/enip/logix/structs/TemplateAttributes.java`

```java
package com.digitalpetri.enip.logix.structs;

import java.io.Serializable;

public final class TemplateAttributes implements Serializable {

  private final int handle;
  private final int memberCount;
  private final int objectDefinitionSize;
  private final int structureSize;

  public TemplateAttributes(
      int handle, int memberCount, int objectDefinitionSize, int structureSize) {
    this.handle = handle;
    this.memberCount = memberCount;
    this.objectDefinitionSize = objectDefinitionSize;
    this.structureSize = structureSize;
  }

  public int getHandle() {
    return handle;
  }

  public int getMemberCount() {
    return memberCount;
  }

  public int getObjectDefinitionSize() {
    return objectDefinitionSize;
  }

  public int getStructureSize() {
    return structureSize;
  }

  @Override
  public String toString() {
    return "TemplateAttributes{"
        + "handle="
        + handle
        + ", memberCount="
        + memberCount
        + ", objectDefinitionSize="
        + objectDefinitionSize
        + ", structureSize="
        + structureSize
        + '}';
  }
}

```

---

### `ethernet-ip/logix-services/src/main/java/com/digitalpetri/enip/logix/structs/TemplateMember.java`

```java
package com.digitalpetri.enip.logix.structs;

import java.io.Serializable;

public final class TemplateMember implements Serializable {

  private final String name;
  private final int infoWord;
  private final int symbolType;
  private final int offset;

  public TemplateMember(String name, int infoWord, int symbolType, int offset) {
    this.name = name;
    this.infoWord = infoWord;
    this.symbolType = symbolType;
    this.offset = offset;
  }

  public String getName() {
    return name;
  }

  public int getInfoWord() {
    return infoWord;
  }

  public int getSymbolType() {
    return symbolType;
  }

  public int getOffset() {
    return offset;
  }

  @Override
  public String toString() {
    return "TemplateMember{"
        + "name='"
        + name
        + '\''
        + ", infoWord="
        + infoWord
        + ", symbolType="
        + symbolType
        + ", offset="
        + offset
        + '}';
  }
}

```

---

### `ethernet-ip/cip-client/src/main/java/com/digitalpetri/enip/cip/CipConnectionPool.java`

```java
/*
 * Copyright 2014
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.digitalpetri.enip.cip;

import com.digitalpetri.enip.cip.epath.EPath.PaddedEPath;
import com.digitalpetri.enip.cip.epath.LogicalSegment.ClassId;
import com.digitalpetri.enip.cip.epath.LogicalSegment.InstanceId;
import com.digitalpetri.enip.cip.services.CipService.PartialResponseException;
import com.digitalpetri.enip.cip.services.ForwardCloseService;
import com.digitalpetri.enip.cip.services.ForwardOpenService;
import com.digitalpetri.enip.cip.services.LargeForwardOpenService;
import com.digitalpetri.enip.cip.structs.*;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.Duration;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class CipConnectionPool {

  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final Map<String, String> loggingContext;

  private final Queue<CipConnection> queue = new LinkedList<>();

  private final Queue<CompletableFuture<CipConnection>> waitQueue = new LinkedList<>();
  private final AtomicInteger count = new AtomicInteger(0);

  private final int connectionLimit;
  private final CipConnectionFactory connectionFactory;

  public CipConnectionPool(
      int connectionLimit, CipClient client, PaddedEPath connectionPath, int connectionSize) {
    this(
        connectionLimit,
        new DefaultConnectionFactory(client, connectionPath, connectionSize),
        client.getConfig().getLoggingContext());
  }

  public CipConnectionPool(
      int connectionLimit,
      CipConnectionFactory connectionFactory,
      Map<String, String> loggingContext) {

    this.connectionLimit = connectionLimit;
    this.connectionFactory = connectionFactory;
    this.loggingContext = loggingContext;
  }

  public synchronized CompletableFuture<CipConnection> acquire() {
    CompletableFuture<CipConnection> future = new CompletableFuture<>();

    acquire0()
        .whenComplete(
            (c, ex) -> {
              if (c != null) {
                if (c.isExpired()) {
                  remove(c);

                  acquire()
                      .whenComplete(
                          (c2, ex2) -> {
                            if (c2 != null) future.complete(c2);
                            else future.completeExceptionally(ex2);
                          });
                } else {
                  future.complete(c);
                }
              } else {
                future.completeExceptionally(ex);
              }
            });

    return future;
  }

  private synchronized CompletableFuture<CipConnection> acquire0() {
    CompletableFuture<CipConnection> future = new CompletableFuture<>();

    if (!queue.isEmpty()) {
      future.complete(queue.poll());
    } else {
      waitQueue.add(future);

      if (count.incrementAndGet() <= connectionLimit) {
        CompletableFuture<CipConnection> f = connectionFactory.open();

        f.whenComplete(
            (c, ex) -> {
              CompletableFuture<CipConnection> waiter;
              synchronized (CipConnectionPool.this) {
                waiter = waitQueue.poll();
              }

              if (c != null) {
                if (waiter != null) {
                  waiter.complete(c);
                } else {
                  queue.add(c);
                }
                loggingContext.forEach(MDC::put);
                try {
                  logger.debug("Forward open succeeded: {}", c);
                } finally {
                  loggingContext.keySet().forEach(MDC::remove);
                }
              } else {
                count.decrementAndGet();
                if (waiter != null) waiter.completeExceptionally(ex);

                loggingContext.forEach(MDC::put);
                try {
                  logger.debug("Forward open failed: {}", ex.getMessage(), ex);
                } finally {
                  loggingContext.keySet().forEach(MDC::remove);
                }
              }
            });
      } else {
        count.decrementAndGet();
      }
    }

    return future;
  }

  public synchronized void release(CipConnection connection) {
    connection.updateLastUse();

    if (!waitQueue.isEmpty()) {
      waitQueue.poll().complete(connection);
    } else {
      queue.add(connection);
    }
  }

  public synchronized void remove(CipConnection connection) {
    connectionFactory
        .close(connection)
        .thenRun(
            () -> {
              loggingContext.forEach(MDC::put);
              try {
                logger.debug("Connection closed: {}", connection);
              } finally {
                loggingContext.keySet().forEach(MDC::remove);
              }
            });

    queue.remove(connection);
    count.decrementAndGet();

    if (!waitQueue.isEmpty()) {
      CompletableFuture<CipConnection> next = waitQueue.poll();

      acquire()
          .whenComplete(
              (c, ex) -> {
                if (c != null) next.complete(c);
                else next.completeExceptionally(ex);
              });
    }
  }

  public interface CipConnectionFactory {

    CompletableFuture<CipConnection> open();

    CompletableFuture<ForwardCloseResponse> close(CipConnection connection);
  }

  public static class DefaultConnectionFactory implements CipConnectionFactory {

    private static final Duration DEFAULT_RPI = Duration.ofSeconds(2);
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(15);

    private static final PaddedEPath MESSAGE_ROUTER_CP_PATH =
        new PaddedEPath(new ClassId(0x02), new InstanceId(0x01));

    private static final AtomicInteger T2O_CONNECTION_ID = new AtomicInteger(0);

    private final CipClient client;
    private final PaddedEPath connectionPath;
    private final int connectionSize;

    public DefaultConnectionFactory(
        CipClient client, PaddedEPath connectionPath, int connectionSize) {
      this.client = client;
      this.connectionPath = connectionPath;
      this.connectionSize = connectionSize;
    }

    @Override
    public CompletableFuture<CipConnection> open() {
      return connectionSize <= 500 ? forwardOpen() : largeForwardOpen();
    }

    private CompletableFuture<CipConnection> forwardOpen() {
      CompletableFuture<CipConnection> future = new CompletableFuture<>();

      NetworkConnectionParameters parameters = getNetworkConnectionParameters();

      ForwardOpenRequest request =
          new ForwardOpenRequest(
              DEFAULT_TIMEOUT,
              0,
              T2O_CONNECTION_ID.incrementAndGet(),
              new Random().nextInt(),
              client.getConfig().getVendorId(),
              client.getConfig().getSerialNumber(),
              1, // 0 = x4, 1 = x8, 2 = x16, 3 = x32, 4 = x128, 5 = x256, 6 = x512
              connectionPath.append(MESSAGE_ROUTER_CP_PATH),
              DEFAULT_RPI,
              parameters,
              DEFAULT_RPI,
              parameters,
              0xA3);

      ForwardOpenService service = new ForwardOpenService(request);

      client
          .sendUnconnectedData(service::encodeRequest)
          .whenComplete(
              (b, ex) -> {
                if (b != null) {
                  try {
                    ForwardOpenResponse response = service.decodeResponse(b);

                    CipConnection connection =
                        new CipConnection(
                            DEFAULT_TIMEOUT.toNanos(),
                            response.getO2tConnectionId(),
                            response.getT2oConnectionId(),
                            response.getConnectionSerialNumber(),
                            response.getOriginatorVendorId(),
                            response.getOriginatorSerialNumber());

                    ReferenceCountUtil.release(response.getApplicationReply());

                    future.complete(connection);
                  } catch (CipResponseException | PartialResponseException e) {
                    future.completeExceptionally(e);
                  } finally {
                    ReferenceCountUtil.release(b);
                  }
                } else {
                  future.completeExceptionally(ex);
                }
              });

      return future;
    }

    protected NetworkConnectionParameters getNetworkConnectionParameters() {
      return new NetworkConnectionParameters(
          connectionSize,
          NetworkConnectionParameters.SizeType.Variable,
          NetworkConnectionParameters.Priority.Low,
          NetworkConnectionParameters.ConnectionType.PointToPoint,
          false);
    }

    private CompletableFuture<CipConnection> largeForwardOpen() {
      CompletableFuture<CipConnection> future = new CompletableFuture<>();

      NetworkConnectionParameters parameters = getNetworkConnectionParameters();

      LargeForwardOpenRequest request =
          new LargeForwardOpenRequest(
              DEFAULT_TIMEOUT, // timeout
              0, // o2tConnectionId
              T2O_CONNECTION_ID.incrementAndGet(), // t2oConnectionId
              new Random().nextInt(), // connectionSerialNumber
              client.getConfig().getVendorId(), // vendorId
              client.getConfig().getSerialNumber(), // vendorSerialNumber
              1, // connectionTimeoutMultiplier
              connectionPath.append(MESSAGE_ROUTER_CP_PATH), // connectionPath
              DEFAULT_RPI, // o2tRpi
              parameters, // o2tParameters
              DEFAULT_RPI, // t2oRpi
              parameters, // t2oParameters
              0xA3); // transportClassAndTrigger

      LargeForwardOpenService service = new LargeForwardOpenService(request);

      client
          .sendUnconnectedData(service::encodeRequest)
          .whenComplete(
              (b, ex) -> {
                if (b != null) {
                  try {
                    LargeForwardOpenResponse response = service.decodeResponse(b);

                    CipConnection connection =
                        new CipConnection(
                            DEFAULT_TIMEOUT.toNanos(),
                            response.getO2tConnectionId(),
                            response.getT2oConnectionId(),
                            response.getConnectionSerialNumber(),
                            response.getOriginatorVendorId(),
                            response.getOriginatorSerialNumber());

                    ReferenceCountUtil.release(response.getApplicationReply());

                    future.complete(connection);
                  } catch (CipResponseException | PartialResponseException e) {
                    future.completeExceptionally(e);
                  } finally {
                    ReferenceCountUtil.release(b);
                  }
                } else {
                  future.completeExceptionally(ex);
                }
              });

      return future;
    }

    @Override
    public CompletableFuture<ForwardCloseResponse> close(CipConnection connection) {
      CompletableFuture<ForwardCloseResponse> future = new CompletableFuture<>();

      ForwardCloseRequest request =
          new ForwardCloseRequest(
              Duration.ofNanos(connection.getTimeoutNanos()),
              connection.getSerialNumber(),
              connection.getOriginatorVendorId(),
              connection.getOriginatorSerialNumber(),
              connectionPath.append(MESSAGE_ROUTER_CP_PATH));

      ForwardCloseService service = new ForwardCloseService(request);

      client
          .sendUnconnectedData(service::encodeRequest)
          .whenComplete(
              (b, ex) -> {
                if (b != null) {
                  try {
                    ForwardCloseResponse response = service.decodeResponse(b);

                    future.complete(response);
                  } catch (CipResponseException | PartialResponseException e) {
                    future.completeExceptionally(e);
                  } finally {
                    ReferenceCountUtil.release(b);
                  }
                } else {
                  future.completeExceptionally(ex);
                }
              });

      return future;
    }
  }

  public static class CipConnection {

    private volatile long lastUse = System.nanoTime();

    private final long timeoutNanos;
    private final int o2tConnectionId;
    private final int t2oConnectionId;
    private final int serialNumber;
    private final int originatorVendorId;
    private final long originatorSerialNumber;

    public CipConnection(
        long timeoutNanos,
        int o2tConnectionId,
        int t2oConnectionId,
        int serialNumber,
        int originatorVendorId,
        long originatorSerialNumber) {

      this.timeoutNanos = timeoutNanos;
      this.o2tConnectionId = o2tConnectionId;
      this.t2oConnectionId = t2oConnectionId;
      this.serialNumber = serialNumber;
      this.originatorVendorId = originatorVendorId;
      this.originatorSerialNumber = originatorSerialNumber;
    }

    public long getTimeoutNanos() {
      return timeoutNanos;
    }

    public int getO2tConnectionId() {
      return o2tConnectionId;
    }

    public int getT2oConnectionId() {
      return t2oConnectionId;
    }

    public int getSerialNumber() {
      return serialNumber;
    }

    public int getOriginatorVendorId() {
      return originatorVendorId;
    }

    public long getOriginatorSerialNumber() {
      return originatorSerialNumber;
    }

    void updateLastUse() {
      lastUse = System.nanoTime();
    }

    boolean isExpired() {
      return (System.nanoTime() - lastUse) > timeoutNanos;
    }

    @Override
    public String toString() {
      return "CipConnection{"
          + "o2tConnectionId="
          + o2tConnectionId
          + ", t2oConnectionId="
          + t2oConnectionId
          + ", serialNumber="
          + serialNumber
          + '}';
    }
  }
}

```

---

### `ethernet-ip/cip-client/src/main/java/com/digitalpetri/enip/cip/CipClient.java`

```java
package com.digitalpetri.enip.cip;

import com.digitalpetri.enip.EtherNetIpClient;
import com.digitalpetri.enip.EtherNetIpClientConfig;
import com.digitalpetri.enip.cip.epath.EPath;
import com.digitalpetri.enip.cip.services.CipService;
import com.digitalpetri.enip.cip.services.CipServiceInvoker;
import com.digitalpetri.enip.cip.services.UnconnectedSendService;
import com.digitalpetri.enip.commands.SendRRData;
import com.digitalpetri.enip.commands.SendUnitData;
import com.digitalpetri.enip.cpf.*;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class CipClient extends EtherNetIpClient implements CipServiceInvoker {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final ConnectedDataHandler connectedDataHandler = new ConnectedDataHandler();
  private final List<CpfItemHandler> additionalHandlers = new CopyOnWriteArrayList<>();

  private final Map<Integer, CompletableFuture<ByteBuf>> pending = new ConcurrentHashMap<>();
  private final Map<Integer, Timeout> timeouts = new ConcurrentHashMap<>();

  private final AtomicInteger sequenceNumber = new AtomicInteger(0);

  private final EPath.PaddedEPath connectionPath;

  public CipClient(EtherNetIpClientConfig config, EPath.PaddedEPath connectionPath) {
    super(config);

    this.connectionPath = connectionPath;
  }

  @Override
  public <T> CompletableFuture<T> invokeConnected(int connectionId, CipService<T> service) {
    CompletableFuture<T> future = new CompletableFuture<>();

    return invokeConnected(connectionId, service, future);
  }

  private <T> CompletableFuture<T> invokeConnected(
      int connectionId, CipService<T> service, CompletableFuture<T> future) {

    sendConnectedData(service::encodeRequest, connectionId)
        .whenComplete(
            (buffer, ex) -> {
              if (buffer != null) {
                try {
                  T response = service.decodeResponse(buffer);

                  future.complete(response);
                } catch (CipService.PartialResponseException e) {
                  invokeConnected(connectionId, service, future);
                } catch (CipResponseException e) {
                  future.completeExceptionally(e);
                } catch (Throwable t) {
                  logger.error("Uncaught Throwable by CipService::decodeResponse", t);

                  future.completeExceptionally(t);
                } finally {
                  ReferenceCountUtil.release(buffer);
                }
              } else {
                future.completeExceptionally(ex);
              }
            });

    return future;
  }

  @Override
  public <T> CompletableFuture<T> invokeUnconnected(CipService<T> service) {
    return invokeUnconnected(service, 0);
  }

  @Override
  public <T> CompletableFuture<T> invokeUnconnected(CipService<T> service, int maxRetries) {
    UnconnectedSendService<T> uss =
        new UnconnectedSendService<T>(service, connectionPath, getConfig().getTimeout());

    return invoke(uss, maxRetries);
  }

  @Override
  public <T> CompletableFuture<T> invoke(CipService<T> service) {
    return invoke(service, 0);
  }

  @Override
  public <T> CompletableFuture<T> invoke(CipService<T> service, int maxRetries) {
    CompletableFuture<T> future = new CompletableFuture<>();

    return invoke(service, future, 0, maxRetries);
  }

  private <T> CompletableFuture<T> invoke(
      CipService<T> service, CompletableFuture<T> future, int count, int maxRetries) {

    sendUnconnectedData(service::encodeRequest)
        .whenComplete(
            (buffer, ex) -> {
              if (buffer != null) {
                try {
                  T response = service.decodeResponse(buffer);

                  future.complete(response);
                } catch (CipService.PartialResponseException e) {
                  invoke(service, future, count, maxRetries);
                } catch (CipResponseException e) {
                  if (e.getGeneralStatus() == 0x01) {
                    boolean requestTimedOut =
                        Arrays.stream(e.getAdditionalStatus()).anyMatch(i -> i == 0x0204);

                    if (requestTimedOut && count < maxRetries) {
                      getConfig().getLoggingContext().forEach(MDC::put);
                      try {
                        logger.debug(
                            "Unconnected request timed out; " + "retrying, count={}, max={}",
                            count,
                            maxRetries);
                      } finally {
                        getConfig().getLoggingContext().keySet().forEach(MDC::remove);
                      }

                      invoke(service, future, count + 1, maxRetries);
                    } else {
                      future.completeExceptionally(e);
                    }
                  } else {
                    future.completeExceptionally(e);
                  }
                } finally {
                  ReferenceCountUtil.release(buffer);
                }
              } else {
                future.completeExceptionally(ex);
              }
            });

    return future;
  }

  public CompletableFuture<ByteBuf> sendConnectedData(ByteBuf data, int connectionId) {
    return sendConnectedData((buffer) -> buffer.writeBytes(data), connectionId);
  }

  public CompletableFuture<ByteBuf> sendConnectedData(
      Consumer<ByteBuf> dataEncoder, int connectionId) {
    CompletableFuture<ByteBuf> future = new CompletableFuture<>();

    ConnectedAddressItem addressItem = new ConnectedAddressItem(connectionId);

    int sequenceNumber = nextSequenceNumber();

    ConnectedDataItemRequest dataItem =
        new ConnectedDataItemRequest(
            (b) -> {
              b.writeShort(sequenceNumber);
              dataEncoder.accept(b);
            });

    CpfPacket packet = new CpfPacket(addressItem, dataItem);
    SendUnitData command = new SendUnitData(packet);

    Timeout timeout =
        getConfig()
            .getWheelTimer()
            .newTimeout(
                tt -> {
                  if (tt.isCancelled()) return;
                  CompletableFuture<ByteBuf> f = pending.remove(sequenceNumber);
                  if (f != null) {
                    String message =
                        String.format(
                            "sequenceNumber=%s timed out waiting %sms for response",
                            sequenceNumber, getConfig().getTimeout().toMillis());
                    f.completeExceptionally(new TimeoutException(message));
                  }
                },
                getConfig().getTimeout().toMillis(),
                TimeUnit.MILLISECONDS);

    pending.put(sequenceNumber, future);
    timeouts.put(sequenceNumber, timeout);

    sendUnitData(command)
        .whenComplete(
            (v, ex) -> {
              // sendUnitData() fails fast if the channel isn't available
              if (ex != null) future.completeExceptionally(ex);
            });

    return future;
  }

  public CompletableFuture<ByteBuf> sendUnconnectedData(ByteBuf data) {
    return sendUnconnectedData((buffer) -> buffer.writeBytes(data));
  }

  public CompletableFuture<ByteBuf> sendUnconnectedData(Consumer<ByteBuf> dataEncoder) {
    CompletableFuture<ByteBuf> future = new CompletableFuture<>();

    UnconnectedDataItemRequest dataItem = new UnconnectedDataItemRequest(dataEncoder);
    CpfPacket packet = new CpfPacket(new NullAddressItem(), dataItem);

    sendRRData(new SendRRData(packet))
        .whenComplete(
            (command, ex) -> {
              if (command != null) {
                CpfItem[] items = command.getPacket().getItems();

                if (items.length >= 2
                    && items[0].getTypeId() == NullAddressItem.TYPE_ID
                    && items[1].getTypeId() == UnconnectedDataItemResponse.TYPE_ID) {

                  ByteBuf data = ((UnconnectedDataItemResponse) items[1]).getData();

                  future.complete(data);
                } else {
                  future.completeExceptionally(new Exception("received unexpected items"));
                }
              } else {
                future.completeExceptionally(ex);
              }
            });

    return future;
  }

  @Override
  protected void onUnitDataReceived(SendUnitData command) {
    CpfItem[] items = command.getPacket().getItems();

    if (connectedDataHandler.itemsMatch(items)) {
      connectedDataHandler.itemsReceived(items);
    } else {
      for (CpfItemHandler handler : additionalHandlers) {
        if (handler.itemsMatch(items)) {
          handler.itemsReceived(items);
          break;
        }
      }
    }
  }

  private short nextSequenceNumber() {
    return (short) sequenceNumber.incrementAndGet();
  }

  private class ConnectedDataHandler implements CpfItemHandler {

    @Override
    public void itemsReceived(CpfItem[] items) {
      int connectionId = ((ConnectedAddressItem) items[0]).getConnectionId();
      ByteBuf buffer = ((ConnectedDataItemResponse) items[1]).getData();

      int sequenceNumber = buffer.readShort();
      ByteBuf data = buffer.readSlice(buffer.readableBytes()).retain();

      Timeout timeout = timeouts.remove(sequenceNumber);
      if (timeout != null) timeout.cancel();

      CompletableFuture<ByteBuf> future = pending.remove(sequenceNumber);

      if (future != null) {
        future.complete(data);
      } else {
        ReferenceCountUtil.release(data);
      }

      ReferenceCountUtil.release(buffer);
    }

    @Override
    public boolean itemsMatch(CpfItem[] items) {
      return items.length == 2
          && items[0].getTypeId() == ConnectedAddressItem.TYPE_ID
          && items[1].getTypeId() == ConnectedDataItemResponse.TYPE_ID;
    }
  }

  public static interface CpfItemHandler {
    boolean itemsMatch(CpfItem[] items);

    void itemsReceived(CpfItem[] items);
  }
}

```

---

### `ethernet-ip/enip-core/src/main/java/com/digitalpetri/enip/commands/ListServices.java`

```java
package com.digitalpetri.enip.commands;

import io.netty.buffer.ByteBuf;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Objects;

/**
 * The optional List Interfaces command shall be used by a connection originator to identify non-CIP
 * communication interfaces associated with the target. A session need not be established to send
 * this command.
 */
public final class ListServices extends Command {

  private final ServiceInformation[] services;

  public ListServices(ServiceInformation... services) {
    super(CommandCode.ListServices);

    this.services = services;
  }

  public ServiceInformation[] getServices() {
    return services;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ListServices that = (ListServices) o;

    return Arrays.equals(services, that.services);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(services);
  }

  public static ByteBuf encode(ListServices command, ByteBuf buffer) {
    if (command.getServices().length != 0) {
      buffer.writeShort(command.getServices().length);

      for (ServiceInformation serviceInformation : command.getServices()) {
        ServiceInformation.encode(serviceInformation, buffer);
      }
    }

    return buffer;
  }

  public static ListServices decode(ByteBuf buffer) {
    int itemCount = buffer.readableBytes() >= 2 ? buffer.readUnsignedShort() : 0;

    ServiceInformation[] services = new ServiceInformation[itemCount];

    for (int i = 0; i < itemCount; i++) {
      services[i] = ServiceInformation.decode(buffer);
    }

    return new ListServices(services);
  }

  public static class ServiceInformation {

    private final int typeCode;
    private final int version;
    private final int capabilityFlags;
    private final String name;

    public ServiceInformation(int typeCode, int version, int capabilityFlags, String name) {
      this.typeCode = typeCode;
      this.version = version;
      this.capabilityFlags = capabilityFlags;
      this.name = name;
    }

    public int getTypeCode() {
      return typeCode;
    }

    public int getVersion() {
      return version;
    }

    public int getCapabilityFlags() {
      return capabilityFlags;
    }

    public String getName() {
      return name;
    }

    public static ByteBuf encode(ServiceInformation serviceInformation, ByteBuf buffer) {
      buffer.writeShort(serviceInformation.getTypeCode());

      // The 16 bytes of the name plus two shorts.
      buffer.writeShort(20);

      // Encode the item...
      buffer.writeShort(serviceInformation.getVersion());
      buffer.writeShort(serviceInformation.getCapabilityFlags());
      writeString(serviceInformation.getName(), buffer);

      return buffer;
    }

    public static ServiceInformation decode(ByteBuf buffer) {
      int typeCode = buffer.readUnsignedShort();
      int itemLength = buffer.readShort();
      int version = buffer.readShort();
      int capabilityFlags = buffer.readShort();
      String name = readString(buffer, itemLength - 4).trim();
      return new ServiceInformation(typeCode, version, capabilityFlags, name);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      ServiceInformation that = (ServiceInformation) o;

      return (typeCode == that.typeCode)
          && (version == that.version)
          && (capabilityFlags == that.capabilityFlags)
          && name.equals(that.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(getTypeCode(), getVersion(), getCapabilityFlags(), getName());
    }

    private static String readString(ByteBuf buffer, int length) {
      length = Math.min(Math.min(length, 255), buffer.readableBytes());
      byte[] bs = new byte[length];
      buffer.readBytes(bs);
      return new String(bs, Charset.forName("US-ASCII"));
    }

    private static void writeString(String s, ByteBuf buffer) {
      byte[] fullBytes = new byte[16];
      byte[] bytes = s.getBytes(Charset.forName("US-ASCII"));
      System.arraycopy(bytes, 0, fullBytes, 0, Math.min(bytes.length, 16));
      buffer.writeBytes(fullBytes);
    }
  }
}

```

---

### `ethernet-ip/enip-core/src/main/java/com/digitalpetri/enip/commands/CommandCode.java`

```java
package com.digitalpetri.enip.commands;

import io.netty.buffer.ByteBuf;

public enum CommandCode {
  Nop(0x00),
  ListServices(0x04),
  ListIdentity(0x63),
  ListInterfaces(0x64),
  RegisterSession(0x65),
  UnRegisterSession(0x66),
  SendRRData(0x6F),
  SendUnitData(0x70);

  private final int code;

  CommandCode(int code) {
    this.code = code;
  }

  public int getCode() {
    return code;
  }

  public static ByteBuf encode(CommandCode commandCode, ByteBuf buffer) {
    buffer.writeShort(commandCode.getCode());

    return buffer;
  }

  public static CommandCode decode(ByteBuf buffer) {
    int code = buffer.readUnsignedShort();

    switch (code) {
      case 0x00:
        return Nop;
      case 0x04:
        return ListServices;
      case 0x63:
        return ListIdentity;
      case 0x64:
        return ListInterfaces;
      case 0x65:
        return RegisterSession;
      case 0x66:
        return UnRegisterSession;
      case 0x6F:
        return SendRRData;
      case 0x70:
        return SendUnitData;
      default:
        throw new RuntimeException(String.format("unrecognized command code: 0x%02X", code));
    }
  }
}

```

---

### `ethernet-ip/enip-core/src/main/java/com/digitalpetri/enip/commands/SendUnitData.java`

```java
package com.digitalpetri.enip.commands;

import com.digitalpetri.enip.cpf.CpfPacket;
import io.netty.buffer.ByteBuf;

public final class SendUnitData extends Command {

  public static final long DEFAULT_INTERFACE_HANDLE = 0;
  public static final int DEFAULT_TIMEOUT = 0;

  private final long interfaceHandle;
  private final int timeout;
  private final CpfPacket packet;

  public SendUnitData(CpfPacket packet) {
    this(DEFAULT_INTERFACE_HANDLE, DEFAULT_TIMEOUT, packet);
  }

  public SendUnitData(long interfaceHandle, int timeout, CpfPacket packet) {
    super(CommandCode.SendUnitData);

    this.interfaceHandle = interfaceHandle;
    this.timeout = timeout;
    this.packet = packet;
  }

  public long getInterfaceHandle() {
    return interfaceHandle;
  }

  public int getTimeout() {
    return timeout;
  }

  public CpfPacket getPacket() {
    return packet;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    SendUnitData that = (SendUnitData) o;

    return interfaceHandle == that.interfaceHandle
        && timeout == that.timeout
        && packet.equals(that.packet);
  }

  @Override
  public int hashCode() {
    int result = (int) (interfaceHandle ^ (interfaceHandle >>> 32));
    result = 31 * result + timeout;
    result = 31 * result + packet.hashCode();
    return result;
  }

  public static ByteBuf encode(SendUnitData command, ByteBuf buffer) {
    buffer.writeInt((int) command.getInterfaceHandle());
    buffer.writeShort(command.getTimeout());

    CpfPacket.encode(command.getPacket(), buffer);

    return buffer;
  }

  public static SendUnitData decode(ByteBuf buffer) {
    return new SendUnitData(
        buffer.readUnsignedInt(), buffer.readUnsignedShort(), CpfPacket.decode(buffer));
  }
}

```

---

### `ethernet-ip/enip-core/src/main/java/com/digitalpetri/enip/commands/SendRRData.java`

```java
package com.digitalpetri.enip.commands;

import com.digitalpetri.enip.cpf.CpfPacket;
import io.netty.buffer.ByteBuf;

public final class SendRRData extends Command {

  public static final long DEFAULT_INTERFACE_HANDLE = 0;
  public static final int DEFAULT_TIMEOUT = 0;

  private final long interfaceHandle;
  private final int timeout;
  private final CpfPacket packet;

  public SendRRData(CpfPacket packet) {
    this(DEFAULT_INTERFACE_HANDLE, DEFAULT_TIMEOUT, packet);
  }

  public SendRRData(long interfaceHandle, int timeout, CpfPacket packet) {
    super(CommandCode.SendRRData);

    this.interfaceHandle = interfaceHandle;
    this.timeout = timeout;
    this.packet = packet;
  }

  public long getInterfaceHandle() {
    return interfaceHandle;
  }

  public int getTimeout() {
    return timeout;
  }

  public CpfPacket getPacket() {
    return packet;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    SendRRData that = (SendRRData) o;

    return interfaceHandle == that.interfaceHandle
        && timeout == that.timeout
        && packet.equals(that.packet);
  }

  @Override
  public int hashCode() {
    int result = (int) (interfaceHandle ^ (interfaceHandle >>> 32));
    result = 31 * result + timeout;
    result = 31 * result + packet.hashCode();
    return result;
  }

  public static ByteBuf encode(SendRRData command, ByteBuf buffer) {
    buffer.writeInt((int) command.getInterfaceHandle());
    buffer.writeShort(command.getTimeout());

    CpfPacket.encode(command.getPacket(), buffer);

    return buffer;
  }

  public static SendRRData decode(ByteBuf buffer) {
    return new SendRRData(
        buffer.readUnsignedInt(), buffer.readUnsignedShort(), CpfPacket.decode(buffer));
  }
}

```

---

### `ethernet-ip/enip-core/src/main/java/com/digitalpetri/enip/commands/Nop.java`

```java
package com.digitalpetri.enip.commands;

import io.netty.buffer.ByteBuf;

import java.util.Arrays;

public final class Nop extends Command {

  private final byte[] data;

  public Nop() {
    this(new byte[0]);
  }

  public Nop(byte[] data) {
    super(CommandCode.Nop);

    this.data = data;
  }

  public byte[] getData() {
    return data;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Nop nop = (Nop) o;

    return Arrays.equals(data, nop.data);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(data);
  }

  public static ByteBuf encode(Nop command, ByteBuf buffer) {
    buffer.writeBytes(command.getData());

    return buffer;
  }

  public static Nop decode(ByteBuf buffer) {
    int size = Math.min(buffer.readableBytes(), 65511);
    byte[] data = new byte[size];
    buffer.readBytes(data);

    return new Nop(data);
  }
}

```

---

### `ethernet-ip/enip-core/src/main/java/com/digitalpetri/enip/commands/ListIdentity.java`

```java
package com.digitalpetri.enip.commands;

import com.digitalpetri.enip.cpf.CipIdentityItem;
import com.digitalpetri.enip.cpf.CipSecurityItem;
import com.digitalpetri.enip.cpf.CpfItem;
import com.digitalpetri.enip.cpf.CpfPacket;
import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ListIdentity extends Command {

  public static final CommandCode COMMAND_CODE = CommandCode.ListIdentity;

  private final CipIdentityItem identityItem;
  private final CipSecurityItem securityItem;

  public ListIdentity() {
    this(null);
  }

  public ListIdentity(CipIdentityItem identityItem) {
    this(identityItem, null);
  }

  public ListIdentity(CipIdentityItem identityItem, CipSecurityItem securityItem) {
    super(COMMAND_CODE);

    this.identityItem = identityItem;
    this.securityItem = securityItem;
  }

  /**
   * @deprecated use {@link #getIdentityItem()}
   */
  @Deprecated
  public Optional<CipIdentityItem> getIdentity() {
    return Optional.ofNullable(identityItem);
  }

  /**
   * @return an {@link Optional} containing the {@link CipIdentityItem}, if present.
   */
  public Optional<CipIdentityItem> getIdentityItem() {
    return Optional.ofNullable(identityItem);
  }

  /**
   * @return an {@link Optional} containing the {@link CipSecurityItem}, if present.
   */
  public Optional<CipSecurityItem> getSecurityItem() {
    return Optional.ofNullable(securityItem);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ListIdentity that = (ListIdentity) o;
    return Objects.equals(identityItem, that.identityItem)
        && Objects.equals(securityItem, that.securityItem);
  }

  @Override
  public int hashCode() {
    return Objects.hash(identityItem, securityItem);
  }

  public static ByteBuf encode(ListIdentity command, ByteBuf buffer) {
    List<CpfItem> items = new ArrayList<>();
    command.getIdentityItem().ifPresent(items::add);
    command.getSecurityItem().ifPresent(items::add);

    if (items.size() > 0) {
      CpfItem[] itemArray = items.toArray(new CpfItem[0]);
      CpfPacket.encode(new CpfPacket(itemArray), buffer);
    }

    return buffer;
  }

  public static ListIdentity decode(ByteBuf buffer) {
    if (buffer.readableBytes() > 0) {
      CpfPacket packet = CpfPacket.decode(buffer);
      CpfItem[] items = packet.getItems();

      CipIdentityItem identityItem;
      CipSecurityItem securityItem = null;

      if (items.length >= 1) {
        assert items[0] instanceof CipIdentityItem;
        identityItem = (CipIdentityItem) items[0];

        if (items.length >= 2 && items[1] instanceof CipSecurityItem) {
          securityItem = (CipSecurityItem) items[1];
        }

        return new ListIdentity(identityItem, securityItem);
      } else {
        return new ListIdentity();
      }
    }

    return new ListIdentity();
  }
}

```

---

### `ethernet-ip/enip-core/src/main/java/com/digitalpetri/enip/commands/RegisterSession.java`

```java
package com.digitalpetri.enip.commands;

import io.netty.buffer.ByteBuf;

public final class RegisterSession extends Command {

  public static final int DEFAULT_PROTOCOL_VERSION = 1;
  public static final int DEFAULT_OPTION_FLAGS = 0;

  private final int protocolVersion;
  private final int optionFlags;

  public RegisterSession() {
    this(DEFAULT_PROTOCOL_VERSION, DEFAULT_OPTION_FLAGS);
  }

  public RegisterSession(int protocolVersion, int optionFlags) {
    super(CommandCode.RegisterSession);

    this.protocolVersion = protocolVersion;
    this.optionFlags = optionFlags;
  }

  public int getProtocolVersion() {
    return protocolVersion;
  }

  public int getOptionFlags() {
    return optionFlags;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RegisterSession that = (RegisterSession) o;

    return optionFlags == that.optionFlags && protocolVersion == that.protocolVersion;
  }

  @Override
  public int hashCode() {
    int result = protocolVersion;
    result = 31 * result + optionFlags;
    return result;
  }

  public static ByteBuf encode(RegisterSession command, ByteBuf buffer) {
    buffer.writeShort(command.getProtocolVersion());
    buffer.writeShort(command.getOptionFlags());

    return buffer;
  }

  public static RegisterSession decode(ByteBuf buffer) {
    return new RegisterSession(buffer.readUnsignedShort(), buffer.readUnsignedShort());
  }
}

```

---

### `ethernet-ip/enip-core/src/main/java/com/digitalpetri/enip/commands/Command.java`

```java
package com.digitalpetri.enip.commands;

public abstract class Command {

  private final CommandCode commandCode;

  protected Command(CommandCode commandCode) {
    this.commandCode = commandCode;
  }

  public CommandCode getCommandCode() {
    return commandCode;
  }
}

```

---

### `ethernet-ip/enip-core/src/main/java/com/digitalpetri/enip/commands/UnRegisterSession.java`

```java
package com.digitalpetri.enip.commands;

import io.netty.buffer.ByteBuf;

public final class UnRegisterSession extends Command {

  public UnRegisterSession() {
    super(CommandCode.UnRegisterSession);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    return !(o == null || getClass() != o.getClass());
  }

  @Override
  public int hashCode() {
    return 0;
  }

  public static ByteBuf encode(UnRegisterSession command, ByteBuf buffer) {
    return buffer;
  }

  public static UnRegisterSession decode(ByteBuf buffer) {
    return new UnRegisterSession();
  }
}

```

---

### `ethernet-ip/enip-core/src/main/java/com/digitalpetri/enip/commands/ListInterfaces.java`

```java
package com.digitalpetri.enip.commands;

import io.netty.buffer.ByteBuf;

import java.util.Arrays;

/**
 * The optional List Interfaces command shall be used by a connection originator to identify non-CIP
 * communication interfaces associated with the target. A session need not be established to send
 * this command.
 */
public final class ListInterfaces extends Command {

  private final InterfaceInformation[] interfaces;

  public ListInterfaces(InterfaceInformation... interfaces) {
    super(CommandCode.ListInterfaces);

    this.interfaces = interfaces;
  }

  public InterfaceInformation[] getInterfaces() {
    return interfaces;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ListInterfaces that = (ListInterfaces) o;

    return Arrays.equals(interfaces, that.interfaces);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(interfaces);
  }

  public static ByteBuf encode(ListInterfaces command, ByteBuf buffer) {
    if (command.getInterfaces().length != 0) {
      buffer.writeShort(command.getInterfaces().length);

      for (InterfaceInformation interfaceInformation : command.getInterfaces()) {
        InterfaceInformation.encode(interfaceInformation, buffer);
      }
    }

    return buffer;
  }

  public static ListInterfaces decode(ByteBuf buffer) {
    int itemCount = buffer.readableBytes() >= 2 ? buffer.readUnsignedShort() : 0;

    InterfaceInformation[] interfaces = new InterfaceInformation[itemCount];

    for (int i = 0; i < itemCount; i++) {
      interfaces[i] = InterfaceInformation.decode(buffer);
    }

    return new ListInterfaces(interfaces);
  }

  public static class InterfaceInformation {

    private final int itemId;
    private final byte[] data;

    public InterfaceInformation(int itemId, byte[] data) {
      this.itemId = itemId;
      this.data = data;
    }

    public int getItemId() {
      return itemId;
    }

    public byte[] getData() {
      return data;
    }

    public static ByteBuf encode(InterfaceInformation interfaceInformation, ByteBuf buffer) {
      buffer.writeShort(interfaceInformation.getItemId());
      buffer.writeShort(interfaceInformation.getData().length);
      buffer.writeBytes(interfaceInformation.getData());

      return buffer;
    }

    public static InterfaceInformation decode(ByteBuf buffer) {
      int itemId = buffer.readUnsignedShort();
      int dataLength = buffer.readUnsignedShort();
      byte[] data = new byte[dataLength];
      buffer.readBytes(data);

      return new InterfaceInformation(itemId, data);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      InterfaceInformation that = (InterfaceInformation) o;

      return itemId == that.itemId && Arrays.equals(data, that.data);
    }

    @Override
    public int hashCode() {
      int result = itemId;
      result = 31 * result + Arrays.hashCode(data);
      return result;
    }
  }
}

```

---

### `ethernet-ip/enip-core/src/main/java/com/digitalpetri/enip/util/IntUtil.java`

```java
package com.digitalpetri.enip.util;

public class IntUtil {

  /**
   * Returns the {@code int} nearest in value to {@code value}.
   *
   * @param value any {@code long} value
   * @return the same value cast to {@code int} if it is in the range of the {@code int} type,
   *     {@link Integer#MAX_VALUE} if it is too large, or {@link Integer#MIN_VALUE} if it is too
   *     small.
   */
  public static int saturatedCast(long value) {
    if (value > Integer.MAX_VALUE) {
      return Integer.MAX_VALUE;
    }
    if (value < Integer.MIN_VALUE) {
      return Integer.MIN_VALUE;
    }
    return (int) value;
  }
}

```

---

### `ethernet-ip/enip-core/src/main/java/com/digitalpetri/enip/cpf/ConnectedDataItemResponse.java`

```java
package com.digitalpetri.enip.cpf;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

public final class ConnectedDataItemResponse extends CpfItem {

  public static final int TYPE_ID = 0xB1;

  private final ByteBuf data;

  public ConnectedDataItemResponse(ByteBuf data) {
    super(TYPE_ID);

    this.data = data;
  }

  public ByteBuf getData() {
    return data;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ConnectedDataItemResponse that = (ConnectedDataItemResponse) o;

    return ByteBufUtil.equals(data, that.data);
  }

  @Override
  public int hashCode() {
    return ByteBufUtil.hashCode(data);
  }

  public static ByteBuf encode(ConnectedDataItemResponse item, ByteBuf buffer) {
    buffer.writeShort(item.getTypeId());

    // Length placeholder...
    int lengthStartIndex = buffer.writerIndex();
    buffer.writeShort(0);

    // Encode the encapsulated data...
    int dataStartIndex = buffer.writerIndex();
    buffer.writeBytes(item.getData());
    item.getData().release();

    // Go back and update the length.
    int bytesWritten = buffer.writerIndex() - dataStartIndex;
    buffer.markWriterIndex();
    buffer.writerIndex(lengthStartIndex);
    buffer.writeShort(bytesWritten);
    buffer.resetWriterIndex();

    return buffer;
  }

  public static ConnectedDataItemResponse decode(ByteBuf buffer) {
    int typeId = buffer.readUnsignedShort();
    int length = buffer.readUnsignedShort();

    assert (typeId == TYPE_ID);

    ByteBuf data = buffer.readSlice(length).retain();

    return new ConnectedDataItemResponse(data);
  }
}

```

---

### `ethernet-ip/enip-core/src/main/java/com/digitalpetri/enip/cpf/SockAddr.java`

```java
package com.digitalpetri.enip.cpf;

import io.netty.buffer.ByteBuf;

import java.nio.ByteOrder;
import java.util.Arrays;

public final class SockAddr {

  private final int sinFamily;
  private final int sinPort;
  private final byte[] sinAddr;
  private final long sinZero;

  public SockAddr(int sinFamily, int sinPort, byte[] sinAddr, long sinZero) {
    this.sinFamily = sinFamily;
    this.sinPort = sinPort;
    this.sinAddr = sinAddr;
    this.sinZero = sinZero;
  }

  public int getSinFamily() {
    return sinFamily;
  }

  public int getSinPort() {
    return sinPort;
  }

  public byte[] getSinAddr() {
    return sinAddr;
  }

  public long getSinZero() {
    return sinZero;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    SockAddr sockAddr = (SockAddr) o;

    return sinFamily == sockAddr.sinFamily
        && sinPort == sockAddr.sinPort
        && sinZero == sockAddr.sinZero
        && Arrays.equals(sinAddr, sockAddr.sinAddr);
  }

  @Override
  public int hashCode() {
    int result = sinFamily;
    result = 31 * result + sinPort;
    result = 31 * result + Arrays.hashCode(sinAddr);
    result = 31 * result + (int) (sinZero ^ (sinZero >>> 32));
    return result;
  }

  public static ByteBuf encode(SockAddr sockAddr, ByteBuf buffer) {
    buffer.order(ByteOrder.BIG_ENDIAN).writeShort(sockAddr.getSinFamily());
    buffer.order(ByteOrder.BIG_ENDIAN).writeShort(sockAddr.getSinPort());
    buffer.order(ByteOrder.BIG_ENDIAN).writeBytes(sockAddr.getSinAddr());
    buffer.order(ByteOrder.BIG_ENDIAN).writeLong(sockAddr.getSinZero());

    return buffer;
  }

  public static SockAddr decode(ByteBuf buffer) {
    int sinFamily = buffer.order(ByteOrder.BIG_ENDIAN).readUnsignedShort();
    int sinPort = buffer.order(ByteOrder.BIG_ENDIAN).readUnsignedShort();
    byte[] sinAddr = new byte[4];
    buffer.order(ByteOrder.BIG_ENDIAN).readBytes(sinAddr);
    long sinZero = buffer.order(ByteOrder.BIG_ENDIAN).readLong();

    return new SockAddr(sinFamily, sinPort, sinAddr, sinZero);
  }
}

```

---

### `ethernet-ip/enip-core/src/main/java/com/digitalpetri/enip/cpf/CipIdentityItem.java`

```java
package com.digitalpetri.enip.cpf;

import io.netty.buffer.ByteBuf;

import java.nio.charset.Charset;

public final class CipIdentityItem extends CpfItem {

  public static final int TYPE_ID = 0x0C;

  private final int protocolVersion;
  private final SockAddr socketAddress;
  private final int vendorId;
  private final int deviceType;
  private final int productCode;
  private final short revisionMajor;
  private final short revisionMinor;
  private final short status;
  private final long serialNumber;
  private final String productName;
  private final short state;

  /**
   * @param protocolVersion encapsulation protocol version supported (also returned with {@link
   *     com.digitalpetri.enip.commands.RegisterSession} reply).
   * @param socketAddress {@link SockAddr} structure.
   * @param vendorId device manufacturers vendor ID.
   * @param deviceType device type of product.
   * @param productCode product code assigned with respect to device type.
   * @param revisionMajor device major revision.
   * @param revisionMinor device minor revision.
   * @param status current status of device.
   * @param serialNumber serial number of device.
   * @param productName human readable description of device.
   * @param state current state of device.
   */
  public CipIdentityItem(
      int protocolVersion,
      SockAddr socketAddress,
      int vendorId,
      int deviceType,
      int productCode,
      short revisionMajor,
      short revisionMinor,
      short status,
      long serialNumber,
      String productName,
      short state) {

    super(TYPE_ID);

    this.protocolVersion = protocolVersion;
    this.socketAddress = socketAddress;
    this.vendorId = vendorId;
    this.deviceType = deviceType;
    this.productCode = productCode;
    this.revisionMajor = revisionMajor;
    this.revisionMinor = revisionMinor;
    this.status = status;
    this.serialNumber = serialNumber;
    this.productName = productName;
    this.state = state;
  }

  public int getProtocolVersion() {
    return protocolVersion;
  }

  public SockAddr getSocketAddress() {
    return socketAddress;
  }

  public int getVendorId() {
    return vendorId;
  }

  public int getDeviceType() {
    return deviceType;
  }

  public int getProductCode() {
    return productCode;
  }

  public short getRevisionMajor() {
    return revisionMajor;
  }

  public short getRevisionMinor() {
    return revisionMinor;
  }

  public short getStatus() {
    return status;
  }

  public long getSerialNumber() {
    return serialNumber;
  }

  public String getProductName() {
    return productName;
  }

  public short getState() {
    return state;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    CipIdentityItem that = (CipIdentityItem) o;

    return deviceType == that.deviceType
        && productCode == that.productCode
        && protocolVersion == that.protocolVersion
        && revisionMajor == that.revisionMajor
        && revisionMinor == that.revisionMinor
        && serialNumber == that.serialNumber
        && state == that.state
        && status == that.status
        && vendorId == that.vendorId
        && productName.equals(that.productName)
        && socketAddress.equals(that.socketAddress);
  }

  @Override
  public int hashCode() {
    int result = protocolVersion;
    result = 31 * result + socketAddress.hashCode();
    result = 31 * result + vendorId;
    result = 31 * result + deviceType;
    result = 31 * result + productCode;
    result = 31 * result + (int) revisionMajor;
    result = 31 * result + (int) revisionMinor;
    result = 31 * result + (int) status;
    result = 31 * result + (int) (serialNumber ^ (serialNumber >>> 32));
    result = 31 * result + productName.hashCode();
    result = 31 * result + (int) state;
    return result;
  }

  public static ByteBuf encode(CipIdentityItem item, ByteBuf buffer) {
    buffer.writeShort(TYPE_ID);

    // Length placeholder...
    int lengthStartIndex = buffer.writerIndex();
    buffer.writeShort(0);

    // Encode the item...
    int itemStartIndex = buffer.writerIndex();
    buffer.writeShort(item.getProtocolVersion());
    SockAddr.encode(item.getSocketAddress(), buffer);
    buffer.writeShort(item.getVendorId());
    buffer.writeShort(item.getDeviceType());
    buffer.writeShort(item.getProductCode());
    buffer.writeByte(item.getRevisionMajor());
    buffer.writeByte(item.getRevisionMinor());
    buffer.writeShort(item.getStatus());
    buffer.writeInt((int) item.getSerialNumber());
    writeString(item.getProductName(), buffer);
    buffer.writeByte(item.getState());

    // Go back and update the length.
    int bytesWritten = buffer.writerIndex() - itemStartIndex;
    buffer.markWriterIndex();
    buffer.writerIndex(lengthStartIndex);
    buffer.writeShort(bytesWritten);
    buffer.resetWriterIndex();

    return buffer;
  }

  public static CipIdentityItem decode(ByteBuf buffer) {
    int typeId = buffer.readUnsignedShort();
    buffer.skipBytes(2); // length

    assert (typeId == TYPE_ID);

    return new CipIdentityItem(
        buffer.readUnsignedShort(),
        SockAddr.decode(buffer),
        buffer.readUnsignedShort(),
        buffer.readUnsignedShort(),
        buffer.readUnsignedShort(),
        buffer.readUnsignedByte(),
        buffer.readUnsignedByte(),
        buffer.readShort(),
        buffer.readUnsignedInt(),
        readString(buffer),
        buffer.readUnsignedByte());
  }

  private static String readString(ByteBuf buffer) {
    int length = Math.min(buffer.readUnsignedByte(), 255);
    byte[] bs = new byte[length];
    buffer.readBytes(bs);

    return new String(bs, Charset.forName("US-ASCII"));
  }

  private static void writeString(String s, ByteBuf buffer) {
    int length = Math.min(s.length(), 255);
    buffer.writeByte(length);
    buffer.writeBytes(s.getBytes(Charset.forName("US-ASCII")), 0, length);
  }
}

```

---

### `ethernet-ip/enip-core/src/main/java/com/digitalpetri/enip/cpf/UnconnectedDataItemRequest.java`

```java
package com.digitalpetri.enip.cpf;

import io.netty.buffer.ByteBuf;

import java.util.function.Consumer;

public class UnconnectedDataItemRequest extends CpfItem {

  public static final int TYPE_ID = 0xB2;

  private final Consumer<ByteBuf> encoder;

  public UnconnectedDataItemRequest(Consumer<ByteBuf> encoder) {
    super(TYPE_ID);

    this.encoder = encoder;
  }

  public Consumer<ByteBuf> getEncoder() {
    return encoder;
  }

  public static ByteBuf encode(UnconnectedDataItemRequest item, ByteBuf buffer) {
    buffer.writeShort(item.getTypeId());

    // Length placeholder...
    int lengthStartIndex = buffer.writerIndex();
    buffer.writeShort(0);

    // Encode the encapsulated data...
    int dataStartIndex = buffer.writerIndex();
    item.getEncoder().accept(buffer);

    // Go back and update the length.
    int bytesWritten = buffer.writerIndex() - dataStartIndex;
    buffer.markWriterIndex();
    buffer.writerIndex(lengthStartIndex);
    buffer.writeShort(bytesWritten);
    buffer.resetWriterIndex();

    return buffer;
  }
}

```

---

### `ethernet-ip/enip-core/src/main/java/com/digitalpetri/enip/cpf/CipSecurityItem.java`

```java
package com.digitalpetri.enip.cpf;

import io.netty.buffer.ByteBuf;

import java.util.Objects;

public class CipSecurityItem extends CpfItem {

  public static final int TYPE_ID = 0x86;

  private final int securityProfiles;
  private final int cipSecurityState;
  private final int enipSecurityState;
  private final int ianaPortState;

  public CipSecurityItem(
      int securityProfiles, int cipSecurityState, int enipSecurityState, int ianaPortState) {
    super(TYPE_ID);

    this.securityProfiles = securityProfiles;
    this.cipSecurityState = cipSecurityState;
    this.enipSecurityState = enipSecurityState;
    this.ianaPortState = ianaPortState;
  }

  /**
   * Get the CIP Security Profiles supported by the device.
   *
   * <pre>
   *  Bit 0       EtherNet/IP Integrity Profile
   *  Bit 1       EtherNet/IP Confidentiality Profile
   *  Bit 2       CIP Authorization Profile
   *  Bit 3       CIP Integrity Profile
   *  Bit 4-15    Reserved
   * </pre>
   *
   * @return the CIP Security Profiles supported by the device.
   */
  public int getSecurityProfiles() {
    return securityProfiles;
  }

  /**
   * Get the current state of the CIP Security Object.
   *
   * <pre>
   *  0   Factory Default Configuration
   *  1   Initial Commissioning In Progress
   *  2   Configured
   *  3   Incomplete Configuration
   * </pre>
   *
   * @return the current state of the CIP Security Object.
   */
  public int getCipSecurityState() {
    return cipSecurityState;
  }

  /**
   * Get the current state of the EtherNet/IP Security Object associated with the IP address where
   * the request was received.
   *
   * <pre>
   *  0   Factory Default Configuration
   *  1   Configuration In Progress
   *  2   Configured
   * </pre>
   *
   * @return the current state of the EtherNet/IP Security Object associated with the IP address
   *     where the request was received.
   */
  public int getEnipSecurityState() {
    return enipSecurityState;
  }

  /**
   * Get the current state, open or closed, for all EtherNet/IP related IANA ports Object associated
   * with the IP address where the request was received.
   *
   * <p>1 (TRUE) shall indicate that the corresponding port is open. If the bit is 0 (FALSE) the
   * port is closed. Reserved bits shall be 0.
   *
   * <pre>
   *  Bit 0    44818/tcp
   *  Bit 1    44818/udp
   *  Bit 2    2222/udp
   *  Bit 3    2221/tcp
   *  Bit 4    2221/udp
   *  Bit 5-7  Reserved
   * </pre>
   *
   * @return the current state, open or closed, for all EtherNet/IP related IANA ports Object
   *     associated with the IP address where the request was received.
   */
  public int getIanaPortState() {
    return ianaPortState;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CipSecurityItem that = (CipSecurityItem) o;
    return securityProfiles == that.securityProfiles
        && cipSecurityState == that.cipSecurityState
        && enipSecurityState == that.enipSecurityState
        && ianaPortState == that.ianaPortState;
  }

  @Override
  public int hashCode() {
    return Objects.hash(securityProfiles, cipSecurityState, enipSecurityState, ianaPortState);
  }

  public static ByteBuf encode(CipSecurityItem item, ByteBuf buffer) {
    buffer.writeShort(TYPE_ID);

    // Length placeholder...
    int lengthStartIndex = buffer.writerIndex();
    buffer.writeShort(0);

    // Encode the item...
    int itemStartIndex = buffer.writerIndex();
    buffer.writeShort(item.getSecurityProfiles());
    buffer.writeByte(item.getCipSecurityState() & 0xFF);
    buffer.writeByte(item.getEnipSecurityState() & 0xFF);
    buffer.writeByte(item.getIanaPortState() & 0xFF);

    // Go back and update the length.
    int bytesWritten = buffer.writerIndex() - itemStartIndex;
    buffer.markWriterIndex();
    buffer.writerIndex(lengthStartIndex);
    buffer.writeShort(bytesWritten);
    buffer.resetWriterIndex();

    return buffer;
  }

  public static CipSecurityItem decode(ByteBuf buffer) {
    int typeId = buffer.readUnsignedShort();
    assert typeId == TYPE_ID;

    buffer.skipBytes(2); // length

    return new CipSecurityItem(
        buffer.readShort(),
        buffer.readUnsignedByte(),
        buffer.readUnsignedByte(),
        buffer.readUnsignedByte());
  }
}

```

---

### `ethernet-ip/enip-core/src/main/java/com/digitalpetri/enip/cpf/SequencedAddressItem.java`

```java
package com.digitalpetri.enip.cpf;

import io.netty.buffer.ByteBuf;

/**
 * This address item shall be used for CIP transport class 0 and class 1 connected data. The data
 * shall contain a connection identifier and a sequence number.
 */
public final class SequencedAddressItem extends CpfItem {

  public static final int TYPE_ID = 0x8002;

  private final long connectionId;
  private final long sequenceNumber;

  /**
   * @param connectionId connection identifier.
   * @param sequenceNumber sequence number.
   */
  public SequencedAddressItem(long connectionId, long sequenceNumber) {
    super(TYPE_ID);

    this.connectionId = connectionId;
    this.sequenceNumber = sequenceNumber;
  }

  public long getConnectionId() {
    return connectionId;
  }

  public long getSequenceNumber() {
    return sequenceNumber;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    SequencedAddressItem that = (SequencedAddressItem) o;

    return connectionId == that.connectionId && sequenceNumber == that.sequenceNumber;
  }

  @Override
  public int hashCode() {
    int result = (int) (connectionId ^ (connectionId >>> 32));
    result = 31 * result + (int) (sequenceNumber ^ (sequenceNumber >>> 32));
    return result;
  }

  private static final int ITEM_LENGTH = 8;

  public static ByteBuf encode(SequencedAddressItem item, ByteBuf buffer) {
    buffer.writeShort(TYPE_ID);
    buffer.writeShort(ITEM_LENGTH);
    buffer.writeInt((int) item.getConnectionId());
    buffer.writeInt((int) item.getSequenceNumber());

    return buffer;
  }

  public static SequencedAddressItem decode(ByteBuf buffer) {
    int typeId = buffer.readUnsignedShort();
    int length = buffer.readUnsignedShort();
    long connectionId = buffer.readUnsignedInt();
    long sequenceNumber = buffer.readUnsignedInt();

    assert (typeId == TYPE_ID);
    assert (length == ITEM_LENGTH);

    return new SequencedAddressItem(connectionId, sequenceNumber);
  }
}

```

---

### `ethernet-ip/enip-core/src/main/java/com/digitalpetri/enip/cpf/CpfPacket.java`

```java
package com.digitalpetri.enip.cpf;

import io.netty.buffer.ByteBuf;

import java.util.Arrays;

public final class CpfPacket {

  private final CpfItem[] items;

  public CpfPacket(CpfItem... items) {
    this.items = items;
  }

  public CpfItem[] getItems() {
    return items;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    CpfPacket cpfPacket = (CpfPacket) o;

    return Arrays.equals(items, cpfPacket.items);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(items);
  }

  public static ByteBuf encode(CpfPacket packet, ByteBuf buffer) {
    buffer.writeShort(packet.getItems().length);

    for (CpfItem item : packet.getItems()) {
      CpfItem.encode(item, buffer);
    }

    return buffer;
  }

  public static CpfPacket decode(ByteBuf buffer) {
    int itemCount = buffer.readUnsignedShort();
    CpfItem[] items = new CpfItem[itemCount];

    for (int i = 0; i < itemCount; i++) {
      items[i] = CpfItem.decode(buffer);
    }

    return new CpfPacket(items);
  }
}

```

---

### `ethernet-ip/enip-core/src/main/java/com/digitalpetri/enip/cpf/CpfItem.java`

```java
package com.digitalpetri.enip.cpf;

import io.netty.buffer.ByteBuf;

public abstract class CpfItem {

  private final int typeId;

  protected CpfItem(int typeId) {
    this.typeId = typeId;
  }

  public int getTypeId() {
    return typeId;
  }

  public static ByteBuf encode(CpfItem item, ByteBuf buffer) {
    switch (item.getTypeId()) {
      case CipIdentityItem.TYPE_ID:
        return CipIdentityItem.encode((CipIdentityItem) item, buffer);

      case ConnectedAddressItem.TYPE_ID:
        return ConnectedAddressItem.encode((ConnectedAddressItem) item, buffer);

      case ConnectedDataItemRequest.TYPE_ID:
        return ConnectedDataItemRequest.encode((ConnectedDataItemRequest) item, buffer);

      case NullAddressItem.TYPE_ID:
        return NullAddressItem.encode((NullAddressItem) item, buffer);

      case SequencedAddressItem.TYPE_ID:
        return SequencedAddressItem.encode((SequencedAddressItem) item, buffer);

      case SockAddrItemO2t.TYPE_ID:
        return SockAddrItemO2t.encode((SockAddrItemO2t) item, buffer);

      case SockAddrItemT2o.TYPE_ID:
        return SockAddrItemT2o.encode((SockAddrItemT2o) item, buffer);

      case UnconnectedDataItemRequest.TYPE_ID:
        return UnconnectedDataItemRequest.encode((UnconnectedDataItemRequest) item, buffer);

      case CipSecurityItem.TYPE_ID:
        return CipSecurityItem.encode((CipSecurityItem) item, buffer);

      default:
        throw new RuntimeException(String.format("unhandled item type: 0x%02X", item.getTypeId()));
    }
  }

  public static CpfItem decode(ByteBuf buffer) {
    int typeId = buffer.getUnsignedShort(buffer.readerIndex());

    switch (typeId) {
      case CipIdentityItem.TYPE_ID:
        return CipIdentityItem.decode(buffer);

      case ConnectedAddressItem.TYPE_ID:
        return ConnectedAddressItem.decode(buffer);

      case ConnectedDataItemResponse.TYPE_ID:
        return ConnectedDataItemResponse.decode(buffer);

      case NullAddressItem.TYPE_ID:
        return NullAddressItem.decode(buffer);

      case SequencedAddressItem.TYPE_ID:
        return SequencedAddressItem.decode(buffer);

      case SockAddrItemO2t.TYPE_ID:
        return SockAddrItemO2t.decode(buffer);

      case SockAddrItemT2o.TYPE_ID:
        return SockAddrItemT2o.decode(buffer);

      case UnconnectedDataItemResponse.TYPE_ID:
        return UnconnectedDataItemResponse.decode(buffer);

      case CipSecurityItem.TYPE_ID:
        return CipSecurityItem.decode(buffer);

      default:
        throw new RuntimeException(String.format("unhandled item type: 0x%02X", typeId));
    }
  }
}

```

---

### `ethernet-ip/enip-core/src/main/java/com/digitalpetri/enip/cpf/SockAddrItemT2o.java`

```java
package com.digitalpetri.enip.cpf;

import io.netty.buffer.ByteBuf;

public final class SockAddrItemT2o extends CpfItem {

  public static final int TYPE_ID = 0x8001;
  private final SockAddr sockAddr;

  public SockAddrItemT2o(SockAddr sockAddr) {
    super(TYPE_ID);

    this.sockAddr = sockAddr;
  }

  public SockAddr getSockAddr() {
    return sockAddr;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    SockAddrItemT2o that = (SockAddrItemT2o) o;

    if (!sockAddr.equals(that.sockAddr)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return sockAddr.hashCode();
  }

  private static final int ITEM_LENGTH = 16;

  public static ByteBuf encode(SockAddrItemT2o item, ByteBuf buffer) {
    buffer.writeShort(TYPE_ID);
    buffer.writeShort(ITEM_LENGTH);

    return SockAddr.encode(item.getSockAddr(), buffer);
  }

  public static SockAddrItemT2o decode(ByteBuf buffer) {
    int typeId = buffer.readUnsignedShort();
    int length = buffer.readUnsignedShort();

    assert (typeId == TYPE_ID);
    assert (length == ITEM_LENGTH);

    SockAddr sockAddr = SockAddr.decode(buffer);

    return new SockAddrItemT2o(sockAddr);
  }
}

```

---

### `ethernet-ip/enip-core/src/main/java/com/digitalpetri/enip/cpf/ConnectedDataItemRequest.java`

```java
package com.digitalpetri.enip.cpf;

import io.netty.buffer.ByteBuf;

import java.util.function.Consumer;

public final class ConnectedDataItemRequest extends CpfItem {

  public static final int TYPE_ID = 0xB1;

  private final Consumer<ByteBuf> encoder;

  public ConnectedDataItemRequest(Consumer<ByteBuf> encoder) {
    super(TYPE_ID);

    this.encoder = encoder;
  }

  public Consumer<ByteBuf> getEncoder() {
    return encoder;
  }

  public static ByteBuf encode(ConnectedDataItemRequest item, ByteBuf buffer) {
    buffer.writeShort(item.getTypeId());

    // Length placeholder...
    int lengthStartIndex = buffer.writerIndex();
    buffer.writeShort(0);

    // Encode the encapsulated data...
    int dataStartIndex = buffer.writerIndex();
    item.getEncoder().accept(buffer);

    // Go back and update the length.
    int bytesWritten = buffer.writerIndex() - dataStartIndex;
    buffer.markWriterIndex();
    buffer.writerIndex(lengthStartIndex);
    buffer.writeShort(bytesWritten);
    buffer.resetWriterIndex();

    return buffer;
  }
}

```

---

### `ethernet-ip/enip-core/src/main/java/com/digitalpetri/enip/cpf/ConnectedAddressItem.java`

```java
package com.digitalpetri.enip.cpf;

import io.netty.buffer.ByteBuf;

/**
 * This address item shall be used when the encapsulated protocol is connection-oriented. The data
 * shall contain a connection identifier.
 */
public final class ConnectedAddressItem extends CpfItem {

  public static final int TYPE_ID = 0xA1;

  private final int connectionId;

  /**
   * @param connectionId the connection identifier, exchanged in the Forward Open service of the
   *     Connection Manager.
   */
  public ConnectedAddressItem(int connectionId) {
    super(TYPE_ID);

    this.connectionId = connectionId;
  }

  public int getConnectionId() {
    return connectionId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ConnectedAddressItem that = (ConnectedAddressItem) o;

    return connectionId == that.connectionId;
  }

  @Override
  public int hashCode() {
    return connectionId;
  }

  private static final int ITEM_LENGTH = 4;

  public static ByteBuf encode(ConnectedAddressItem item, ByteBuf buffer) {
    buffer.writeShort(item.getTypeId());
    buffer.writeShort(ITEM_LENGTH);
    buffer.writeInt(item.getConnectionId());

    return buffer;
  }

  public static ConnectedAddressItem decode(ByteBuf buffer) {
    int typeId = buffer.readUnsignedShort();
    int length = buffer.readUnsignedShort();
    int connectionId = buffer.readInt();

    assert (typeId == TYPE_ID);
    assert (length == ITEM_LENGTH);

    return new ConnectedAddressItem(connectionId);
  }
}

```

---

### `ethernet-ip/enip-core/src/main/java/com/digitalpetri/enip/cpf/NullAddressItem.java`

```java
package com.digitalpetri.enip.cpf;

import io.netty.buffer.ByteBuf;

/**
 * The null address item shall contain only the type id and the length. The length shall be zero. No
 * data shall follow the length. Since the null address item contains no routing information, it
 * shall be used when the protocol packet itself contains any necessary routing information. The
 * null address item shall be used for Unconnected Messages.
 */
public final class NullAddressItem extends CpfItem {

  public static final int TYPE_ID = 0x00;

  public NullAddressItem() {
    super(TYPE_ID);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    return !(o == null || getClass() != o.getClass());
  }

  @Override
  public int hashCode() {
    return 0;
  }

  private static final int ITEM_LENGTH = 0;

  public static ByteBuf encode(NullAddressItem item, ByteBuf buffer) {
    buffer.writeShort(TYPE_ID);
    buffer.writeShort(ITEM_LENGTH);

    return buffer;
  }

  public static NullAddressItem decode(ByteBuf buffer) {
    int typeId = buffer.readUnsignedShort();
    int length = buffer.readUnsignedShort();

    assert (typeId == TYPE_ID);
    assert (length == ITEM_LENGTH);

    return new NullAddressItem();
  }
}

```

---

### `ethernet-ip/enip-core/src/main/java/com/digitalpetri/enip/cpf/UnconnectedDataItemResponse.java`

```java
package com.digitalpetri.enip.cpf;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

public final class UnconnectedDataItemResponse extends CpfItem {

  public static final int TYPE_ID = 0xB2;

  private final ByteBuf data;

  public UnconnectedDataItemResponse(ByteBuf data) {
    super(TYPE_ID);

    this.data = data;
  }

  public ByteBuf getData() {
    return data;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    UnconnectedDataItemResponse that = (UnconnectedDataItemResponse) o;

    return ByteBufUtil.equals(data, that.data);
  }

  @Override
  public int hashCode() {
    return ByteBufUtil.hashCode(data);
  }

  public static ByteBuf encode(UnconnectedDataItemResponse item, ByteBuf buffer) {
    buffer.writeShort(item.getTypeId());

    // Length placeholder...
    int lengthStartIndex = buffer.writerIndex();
    buffer.writeShort(0);

    // Encode the encapsulated data...
    int dataStartIndex = buffer.writerIndex();
    buffer.writeBytes(item.getData());
    item.getData().release();

    // Go back and update the length.
    int bytesWritten = buffer.writerIndex() - dataStartIndex;
    buffer.markWriterIndex();
    buffer.writerIndex(lengthStartIndex);
    buffer.writeShort(bytesWritten);
    buffer.resetWriterIndex();

    return buffer;
  }

  public static UnconnectedDataItemResponse decode(ByteBuf buffer) {
    int typeId = buffer.readUnsignedShort();
    int length = buffer.readUnsignedShort();

    assert (typeId == TYPE_ID);

    ByteBuf data = buffer.readSlice(length).retain();

    return new UnconnectedDataItemResponse(data);
  }
}

```

---

### `ethernet-ip/enip-core/src/main/java/com/digitalpetri/enip/cpf/SockAddrItemO2t.java`

```java
package com.digitalpetri.enip.cpf;

import io.netty.buffer.ByteBuf;

public final class SockAddrItemO2t extends CpfItem {

  public static final int TYPE_ID = 0x8000;

  private final SockAddr sockAddr;

  public SockAddrItemO2t(SockAddr sockAddr) {
    super(TYPE_ID);

    this.sockAddr = sockAddr;
  }

  public SockAddr getSockAddr() {
    return sockAddr;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    SockAddrItemO2t that = (SockAddrItemO2t) o;

    return sockAddr.equals(that.sockAddr);
  }

  @Override
  public int hashCode() {
    return sockAddr.hashCode();
  }

  private static final int ITEM_LENGTH = 16;

  public static ByteBuf encode(SockAddrItemO2t item, ByteBuf buffer) {
    buffer.writeShort(TYPE_ID);
    buffer.writeShort(ITEM_LENGTH);

    return SockAddr.encode(item.getSockAddr(), buffer);
  }

  public static SockAddrItemO2t decode(ByteBuf buffer) {
    int typeId = buffer.readUnsignedShort();
    int length = buffer.readUnsignedShort();

    assert (typeId == TYPE_ID);
    assert (length == ITEM_LENGTH);

    SockAddr sockAddr = SockAddr.decode(buffer);

    return new SockAddrItemO2t(sockAddr);
  }
}

```

---

### `ethernet-ip/enip-core/src/main/java/com/digitalpetri/enip/EnipPacket.java`

```java
package com.digitalpetri.enip;

import com.digitalpetri.enip.commands.*;
import io.netty.buffer.ByteBuf;
import org.jspecify.annotations.Nullable;

/**
 * All encapsulation messages, sent via TCP or sent to UDP port 0xAF12, shall be composed of a
 * fixed-length header of 24 bytes followed by an optional data portion. The total encapsulation
 * message length (including header) shall be limited to 65535 bytes.
 */
public final class EnipPacket {

  private final CommandCode commandCode;
  private final long sessionHandle;
  private final EnipStatus status;
  private final long senderContext;

  private final @Nullable Command command;

  public EnipPacket(
      CommandCode commandCode,
      long sessionHandle,
      EnipStatus status,
      long senderContext,
      @Nullable Command command) {

    this.commandCode = commandCode;
    this.sessionHandle = sessionHandle;
    this.status = status;
    this.senderContext = senderContext;
    this.command = command;
  }

  public CommandCode getCommandCode() {
    return commandCode;
  }

  public long getSessionHandle() {
    return sessionHandle;
  }

  public EnipStatus getStatus() {
    return status;
  }

  public long getSenderContext() {
    return senderContext;
  }

  @Nullable
  public Command getCommand() {
    return command;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    EnipPacket that = (EnipPacket) o;

    return senderContext == that.senderContext
        && sessionHandle == that.sessionHandle
        && !(command != null ? !command.equals(that.command) : that.command != null)
        && commandCode == that.commandCode
        && status == that.status;
  }

  @Override
  public int hashCode() {
    int result = commandCode.hashCode();
    result = 31 * result + (int) (sessionHandle ^ (sessionHandle >>> 32));
    result = 31 * result + status.hashCode();
    result = 31 * result + (int) (senderContext ^ (senderContext >>> 32));
    result = 31 * result + (command != null ? command.hashCode() : 0);
    return result;
  }

  public static ByteBuf encode(EnipPacket packet, ByteBuf buffer) {
    buffer.writeShort(packet.getCommandCode().getCode());

    // Length placeholder...
    int lengthStartIndex = buffer.writerIndex();
    buffer.writeShort(0);

    buffer.writeInt((int) packet.getSessionHandle());
    buffer.writeInt(packet.getStatus().getStatus());
    buffer.writeLong(packet.getSenderContext());
    buffer.writeInt(0);

    int dataStartIndex = buffer.writerIndex();

    if (packet.getCommand() != null) {
      encodeCommand(packet.getCommand(), buffer);
    }

    // Go back and update the length.
    int bytesWritten = buffer.writerIndex() - dataStartIndex;
    buffer.markWriterIndex();
    buffer.writerIndex(lengthStartIndex);
    buffer.writeShort(bytesWritten);
    buffer.resetWriterIndex();

    return buffer;
  }

  public static EnipPacket decode(ByteBuf buffer) {
    CommandCode commandCode = CommandCode.decode(buffer);
    buffer.skipBytes(2); // length
    long sessionHandle = buffer.readUnsignedInt();
    EnipStatus status = EnipStatus.decode(buffer);
    long senderContext = buffer.readLong();
    buffer.skipBytes(4); // options

    Command command =
        (status == EnipStatus.EIP_SUCCESS) ? decodeCommand(commandCode, buffer) : null;

    return new EnipPacket(commandCode, sessionHandle, status, senderContext, command);
  }

  private static ByteBuf encodeCommand(Command command, ByteBuf buffer) {
    switch (command.getCommandCode()) {
      case ListIdentity:
        return ListIdentity.encode((ListIdentity) command, buffer);

      case ListInterfaces:
        return ListInterfaces.encode((ListInterfaces) command, buffer);

      case ListServices:
        return ListServices.encode((ListServices) command, buffer);

      case Nop:
        return Nop.encode((Nop) command, buffer);

      case RegisterSession:
        return RegisterSession.encode((RegisterSession) command, buffer);

      case SendRRData:
        return SendRRData.encode((SendRRData) command, buffer);

      case SendUnitData:
        return SendUnitData.encode((SendUnitData) command, buffer);

      case UnRegisterSession:
        return UnRegisterSession.encode((UnRegisterSession) command, buffer);

      default:
        throw new RuntimeException(
            String.format("unhandled command: %s", command.getCommandCode()));
    }
  }

  private static Command decodeCommand(CommandCode commandCode, ByteBuf buffer) {
    switch (commandCode) {
      case ListIdentity:
        return ListIdentity.decode(buffer);

      case ListInterfaces:
        return ListInterfaces.decode(buffer);

      case ListServices:
        return ListServices.decode(buffer);

      case Nop:
        return Nop.decode(buffer);

      case RegisterSession:
        return RegisterSession.decode(buffer);

      case SendRRData:
        return SendRRData.decode(buffer);

      case SendUnitData:
        return SendUnitData.decode(buffer);

      case UnRegisterSession:
        return UnRegisterSession.decode(buffer);

      default:
        throw new RuntimeException(String.format("unhandled command: %s", commandCode));
    }
  }
}

```

---

### `ethernet-ip/enip-core/src/main/java/com/digitalpetri/enip/EnipStatus.java`

```java
package com.digitalpetri.enip;

import io.netty.buffer.ByteBuf;

public enum EnipStatus {
  EIP_SUCCESS(0x00),
  INVALID_UNSUPPORTED(0x01),
  INSUFFICIENT_MEMORY(0x02),
  MALFORMED_DATA(0x03),
  INVALID_SESSION_HANDLE(0x64),
  INVALID_LENGTH(0x65),
  UNSUPPORTED_PROTOCOL_VERSION(0x69);

  private final int status;

  EnipStatus(int status) {
    this.status = status;
  }

  public int getStatus() {
    return status;
  }

  public static ByteBuf encode(EnipStatus status, ByteBuf buffer) {
    buffer.writeInt(status.getStatus());

    return buffer;
  }

  public static EnipStatus decode(ByteBuf buffer) {
    int status = buffer.readInt();

    switch (status) {
      case 0x00:
        return EIP_SUCCESS;
      case 0x01:
        return INVALID_UNSUPPORTED;
      case 0x02:
        return INSUFFICIENT_MEMORY;
      case 0x03:
        return MALFORMED_DATA;
      case 0x64:
        return INVALID_SESSION_HANDLE;
      case 0x65:
        return INVALID_LENGTH;
      case 0x69:
        return UNSUPPORTED_PROTOCOL_VERSION;
      default:
        throw new RuntimeException(String.format("unrecognized status: 0x%02X", status));
    }
  }
}

```

---

### `ethernet-ip/enip-core/src/test/java/com/digitalpetri/enip/commands/NopTest.java`

```java
package com.digitalpetri.enip.commands;

import static org.testng.Assert.assertEquals;

import com.digitalpetri.enip.SerializationTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class NopTest extends SerializationTest<Nop> {

  @Test(dataProvider = "getData")
  public void testSerialization(byte[] data) {
    Nop command = new Nop(data);
    Nop decoded = encodeDecode(command, Nop::encode, Nop::decode);

    assertEquals(command, decoded);
  }

  @DataProvider
  private static Object[][] getData() {
    return new Object[][] {{new byte[0]}, {new byte[] {1, 2, 3, 4}}};
  }
}

```

---

### `ethernet-ip/enip-core/src/test/java/com/digitalpetri/enip/commands/ListServicesTest.java`

```java
package com.digitalpetri.enip.commands;

import static org.testng.Assert.assertEquals;

import com.digitalpetri.enip.SerializationTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class ListServicesTest extends SerializationTest<ListServices> {

  @Test(dataProvider = "getServices")
  public void testSerialization(ListServices.ServiceInformation[] services) {
    ListServices command = new ListServices(services);
    ListServices decoded = encodeDecode(command, ListServices::encode, ListServices::decode);

    assertEquals(command, decoded);
  }

  @DataProvider
  private static Object[][] getServices() {
    ListServices.ServiceInformation ii1 =
        new ListServices.ServiceInformation(1, 1, 2, "SomeService");
    ListServices.ServiceInformation ii2 =
        new ListServices.ServiceInformation(2, 1, 3, "OtherServicxe");

    return new Object[][] {
      {new ListServices.ServiceInformation[0]},
      {new ListServices.ServiceInformation[] {ii1}},
      {new ListServices.ServiceInformation[] {ii1, ii2}}
    };
  }
}

```

---

### `ethernet-ip/enip-core/src/test/java/com/digitalpetri/enip/commands/UnRegisterSessionTest.java`

```java
package com.digitalpetri.enip.commands;

import static org.testng.Assert.assertEquals;

import com.digitalpetri.enip.SerializationTest;
import org.testng.annotations.Test;

public class UnRegisterSessionTest extends SerializationTest<UnRegisterSession> {

  @Test
  public void testSerialization() {
    UnRegisterSession command = new UnRegisterSession();
    UnRegisterSession decoded =
        encodeDecode(command, UnRegisterSession::encode, UnRegisterSession::decode);

    assertEquals(command, decoded);
  }
}

```

---

### `ethernet-ip/enip-core/src/test/java/com/digitalpetri/enip/commands/ListInterfacesTest.java`

```java
package com.digitalpetri.enip.commands;

import static org.testng.Assert.assertEquals;

import com.digitalpetri.enip.SerializationTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class ListInterfacesTest extends SerializationTest<ListInterfaces> {

  @Test(dataProvider = "getInterfaces")
  public void testSerialization(ListInterfaces.InterfaceInformation[] interfaces) {
    ListInterfaces command = new ListInterfaces(interfaces);
    ListInterfaces decoded = encodeDecode(command, ListInterfaces::encode, ListInterfaces::decode);

    assertEquals(command, decoded);
  }

  @DataProvider
  private static Object[][] getInterfaces() {
    ListInterfaces.InterfaceInformation ii1 =
        new ListInterfaces.InterfaceInformation(1, new byte[] {1, 2, 3, 4});
    ListInterfaces.InterfaceInformation ii2 =
        new ListInterfaces.InterfaceInformation(2, new byte[] {4, 3, 2, 1});

    return new Object[][] {
      {new ListInterfaces.InterfaceInformation[0]},
      {new ListInterfaces.InterfaceInformation[] {ii1}},
      {new ListInterfaces.InterfaceInformation[] {ii1, ii2}}
    };
  }
}

```

---

### `ethernet-ip/enip-core/src/test/java/com/digitalpetri/enip/commands/RegisterSessionTest.java`

```java
package com.digitalpetri.enip.commands;

import static org.testng.Assert.assertEquals;

import com.digitalpetri.enip.SerializationTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class RegisterSessionTest extends SerializationTest<RegisterSession> {

  @Test
  public void testDefaultParameters() {
    RegisterSession command = new RegisterSession();

    assertEquals(command.getProtocolVersion(), RegisterSession.DEFAULT_PROTOCOL_VERSION);
    assertEquals(command.getOptionFlags(), RegisterSession.DEFAULT_OPTION_FLAGS);
  }

  @Test(dataProvider = "getParameters")
  public void testSerialization(int protocolVersion, int optionFlags) {
    RegisterSession command = new RegisterSession(protocolVersion, optionFlags);

    RegisterSession decoded =
        encodeDecode(command, RegisterSession::encode, RegisterSession::decode);

    assertEquals(command.getProtocolVersion(), decoded.getProtocolVersion());
    assertEquals(command.getOptionFlags(), decoded.getOptionFlags());
  }

  @DataProvider
  private static Object[][] getParameters() {
    return new Object[][] {
      {RegisterSession.DEFAULT_PROTOCOL_VERSION, RegisterSession.DEFAULT_OPTION_FLAGS},
      {RegisterSession.DEFAULT_PROTOCOL_VERSION + 1, RegisterSession.DEFAULT_OPTION_FLAGS + 1}
    };
  }
}

```

---

### `ethernet-ip/enip-core/src/test/java/com/digitalpetri/enip/commands/CommandCodeTest.java`

```java
package com.digitalpetri.enip.commands;

import static org.testng.Assert.assertEquals;

import com.digitalpetri.enip.SerializationTest;
import org.testng.annotations.Test;

public class CommandCodeTest extends SerializationTest<CommandCode> {

  @Test
  public void testSerialization() {
    for (CommandCode commandCode : CommandCode.values()) {
      CommandCode decoded = encodeDecode(commandCode, CommandCode::encode, CommandCode::decode);

      assertEquals(commandCode, decoded);
    }
  }
}

```

---

### `ethernet-ip/enip-core/src/test/java/com/digitalpetri/enip/commands/ListIdentityTest.java`

```java
package com.digitalpetri.enip.commands;

import static org.testng.Assert.assertEquals;

import com.digitalpetri.enip.SerializationTest;
import com.digitalpetri.enip.cpf.CipIdentityItem;
import com.digitalpetri.enip.cpf.CipSecurityItem;
import com.digitalpetri.enip.cpf.SockAddr;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class ListIdentityTest extends SerializationTest<ListIdentity> {

  @Test(dataProvider = "getIdentityItem")
  public void testSerialization(CipIdentityItem identityItem, CipSecurityItem securityItem) {
    ListIdentity identity = new ListIdentity(identityItem, securityItem);
    ListIdentity decoded = encodeDecode(identity, ListIdentity::encode, ListIdentity::decode);

    assertEquals(identity, decoded);
  }

  @DataProvider
  private static Object[][] getIdentityItem() {
    CipIdentityItem identityItem =
        new CipIdentityItem(
            0,
            new SockAddr(1, 2, new byte[4], 0),
            1,
            2,
            3,
            (short) 4,
            (short) 5,
            (short) 6,
            1234L,
            "test",
            (short) 0);

    CipSecurityItem securityItem = new CipSecurityItem(1, 2, 3, 4);

    return new Object[][] {
      {null, null},
      {identityItem, null},
      {identityItem, securityItem}
    };
  }
}

```

---

### `ethernet-ip/enip-core/src/test/java/com/digitalpetri/enip/SerializationTest.java`

```java
package com.digitalpetri.enip;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

public abstract class SerializationTest<T> {

  protected ByteBuf buffer;

  @BeforeMethod
  public void setUp() {
    buffer = Unpooled.buffer();
  }

  @AfterMethod
  public void tearDown() {
    ReferenceCountUtil.release(buffer);
  }

  protected T encodeDecode(
      T toEncode, BiConsumer<T, ByteBuf> encoder, Function<ByteBuf, T> decoder) {
    encoder.accept(toEncode, buffer);
    return decoder.apply(buffer);
  }
}

```

---

### `ethernet-ip/enip-core/src/test/java/com/digitalpetri/enip/EnipPacketTest.java`

```java
package com.digitalpetri.enip;

import static org.testng.Assert.assertEquals;

import com.digitalpetri.enip.commands.Command;
import com.digitalpetri.enip.commands.ListIdentity;
import com.digitalpetri.enip.commands.ListInterfaces;
import com.digitalpetri.enip.commands.ListServices;
import com.digitalpetri.enip.commands.Nop;
import com.digitalpetri.enip.commands.RegisterSession;
import com.digitalpetri.enip.commands.SendRRData;
import com.digitalpetri.enip.commands.SendUnitData;
import com.digitalpetri.enip.commands.UnRegisterSession;
import com.digitalpetri.enip.cpf.CpfPacket;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class EnipPacketTest extends SerializationTest<EnipPacket> {

  @Test(dataProvider = "getCommand")
  public void testSerialization(Command command) {
    EnipPacket packet =
        new EnipPacket(command.getCommandCode(), 1L, EnipStatus.EIP_SUCCESS, 2L, command);

    EnipPacket decoded = encodeDecode(packet, EnipPacket::encode, EnipPacket::decode);

    assertEquals(packet, decoded);
  }

  @DataProvider
  private static Object[][] getCommand() {
    return new Object[][] {
      {new ListIdentity()},
      {new ListInterfaces()},
      {new ListServices()},
      {new Nop()},
      {new RegisterSession()},
      {new SendRRData(new CpfPacket())},
      {new SendUnitData(new CpfPacket())},
      {new UnRegisterSession()}
    };
  }
}

```

---

### `ethernet-ip/enip-core/src/test/java/com/digitalpetri/enip/cpf/SequencedAddressItemTest.java`

```java
package com.digitalpetri.enip.cpf;

import static org.testng.Assert.assertEquals;

import com.digitalpetri.enip.SerializationTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class SequencedAddressItemTest extends SerializationTest<SequencedAddressItem> {

  @Test(dataProvider = "getParameters")
  public void testSerialization(long connectionId, long sequenceNumber) {
    SequencedAddressItem item = new SequencedAddressItem(connectionId, sequenceNumber);
    SequencedAddressItem decoded =
        encodeDecode(item, SequencedAddressItem::encode, SequencedAddressItem::decode);

    assertEquals(item, decoded);
  }

  @DataProvider
  private static Object[][] getParameters() {
    return new Object[][] {
      {0L, 0L},
      {(long) Short.MAX_VALUE, (long) Short.MAX_VALUE},
      {(long) Integer.MAX_VALUE, (long) Integer.MAX_VALUE},
      {(long) Integer.MAX_VALUE + 1, (long) Integer.MAX_VALUE + 1}
    };
  }
}

```

---

### `ethernet-ip/enip-core/src/test/java/com/digitalpetri/enip/cpf/UnconnectedDataItemResponseTest.java`

```java
package com.digitalpetri.enip.cpf;

import static org.testng.Assert.assertEquals;

import com.digitalpetri.enip.SerializationTest;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class UnconnectedDataItemResponseTest
    extends SerializationTest<UnconnectedDataItemResponse> {

  @Test(dataProvider = "getData")
  public void testSerialization(ByteBuf data) {
    data.retain();
    data.markReaderIndex();

    UnconnectedDataItemResponse item = new UnconnectedDataItemResponse(data);
    UnconnectedDataItemResponse decoded =
        encodeDecode(
            item, UnconnectedDataItemResponse::encode, UnconnectedDataItemResponse::decode);

    data.resetReaderIndex();
    assertEquals(item, decoded);

    data.release();
    decoded.getData().release();
  }

  @DataProvider
  private static Object[][] getData() {
    return new Object[][] {{Unpooled.EMPTY_BUFFER}, {Unpooled.buffer().writeByte(1).writeByte(2)}};
  }
}

```

---

### `ethernet-ip/enip-core/src/test/java/com/digitalpetri/enip/cpf/CipIdentityItemTest.java`

```java
package com.digitalpetri.enip.cpf;

import static org.testng.Assert.assertEquals;

import com.digitalpetri.enip.SerializationTest;
import org.testng.annotations.Test;

public class CipIdentityItemTest extends SerializationTest<CipIdentityItem> {

  @Test
  public void testSerialization() {
    CipIdentityItem item =
        new CipIdentityItem(
            0,
            new SockAddr(1, 2, new byte[4], 0),
            1,
            2,
            3,
            (short) 4,
            (short) 5,
            (short) 6,
            1234L,
            "test",
            (short) 0);

    CipIdentityItem decoded = encodeDecode(item, CipIdentityItem::encode, CipIdentityItem::decode);

    assertEquals(item, decoded);
  }
}

```

---

### `ethernet-ip/enip-core/src/test/java/com/digitalpetri/enip/cpf/NullAddressItemTest.java`

```java
package com.digitalpetri.enip.cpf;

import static org.testng.Assert.assertEquals;

import com.digitalpetri.enip.SerializationTest;
import org.testng.annotations.Test;

public class NullAddressItemTest extends SerializationTest<NullAddressItem> {

  @Test
  public void testSerialization() {
    NullAddressItem item = new NullAddressItem();
    NullAddressItem decoded = encodeDecode(item, NullAddressItem::encode, NullAddressItem::decode);

    assertEquals(item, decoded);
  }
}

```

---

### `ethernet-ip/enip-core/src/test/java/com/digitalpetri/enip/cpf/ConnectedAddressItemTest.java`

```java
package com.digitalpetri.enip.cpf;

import static org.testng.Assert.assertEquals;

import com.digitalpetri.enip.SerializationTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class ConnectedAddressItemTest extends SerializationTest<ConnectedAddressItem> {

  @Test(dataProvider = "getConnectionId")
  public void testSerialization(int connectionId) {
    ConnectedAddressItem item = new ConnectedAddressItem(connectionId);

    ConnectedAddressItem decoded =
        encodeDecode(item, ConnectedAddressItem::encode, ConnectedAddressItem::decode);

    assertEquals(item, decoded);
  }

  @DataProvider
  private static Object[][] getConnectionId() {
    return new Object[][] {{0}, {1}, {Integer.MAX_VALUE}};
  }
}

```

---

### `ethernet-ip/enip-core/src/test/java/com/digitalpetri/enip/cpf/SockAddrItemT2oTest.java`

```java
package com.digitalpetri.enip.cpf;

import static org.testng.Assert.assertEquals;

import com.digitalpetri.enip.SerializationTest;
import org.testng.annotations.Test;

public class SockAddrItemT2oTest extends SerializationTest<SockAddrItemT2o> {

  @Test
  public void testSerialization() {
    SockAddrItemT2o item = new SockAddrItemT2o(new SockAddr(1, 2, new byte[] {1, 2, 3, 4}, 0L));
    SockAddrItemT2o decoded = encodeDecode(item, SockAddrItemT2o::encode, SockAddrItemT2o::decode);

    assertEquals(item, decoded);
  }
}

```

---

### `ethernet-ip/enip-core/src/test/java/com/digitalpetri/enip/cpf/SockAddrItemO2tTest.java`

```java
package com.digitalpetri.enip.cpf;

import static org.testng.Assert.assertEquals;

import com.digitalpetri.enip.SerializationTest;
import org.testng.annotations.Test;

public class SockAddrItemO2tTest extends SerializationTest<SockAddrItemO2t> {

  @Test
  public void testSerialization() {
    SockAddrItemO2t item = new SockAddrItemO2t(new SockAddr(1, 2, new byte[] {1, 2, 3, 4}, 0L));
    SockAddrItemO2t decoded = encodeDecode(item, SockAddrItemO2t::encode, SockAddrItemO2t::decode);

    assertEquals(item, decoded);
  }
}

```

---

### `ethernet-ip/enip-core/src/test/java/com/digitalpetri/enip/cpf/ConnectedDataItemResponseTest.java`

```java
package com.digitalpetri.enip.cpf;

import static org.testng.Assert.assertEquals;

import com.digitalpetri.enip.SerializationTest;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class ConnectedDataItemResponseTest extends SerializationTest<ConnectedDataItemResponse> {

  @Test(dataProvider = "getData")
  public void testSerialization(ByteBuf data) {
    data.retain();
    data.markReaderIndex();

    ConnectedDataItemResponse item = new ConnectedDataItemResponse(data);
    ConnectedDataItemResponse decoded =
        encodeDecode(item, ConnectedDataItemResponse::encode, ConnectedDataItemResponse::decode);

    data.resetReaderIndex();
    assertEquals(item, decoded);

    data.release();
    decoded.getData().release();
  }

  @DataProvider
  private static Object[][] getData() {
    return new Object[][] {{Unpooled.EMPTY_BUFFER}, {Unpooled.buffer().writeByte(1).writeByte(2)}};
  }
}

```

---

### `ethernet-ip/enip-core/src/test/java/com/digitalpetri/enip/cpf/CpfItemTest.java`

```java
package com.digitalpetri.enip.cpf;

import static org.testng.Assert.assertEquals;

import com.digitalpetri.enip.SerializationTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class CpfItemTest extends SerializationTest<CpfItem> {

  @Test(dataProvider = "getItems")
  public void testSerialization(CpfItem item) {
    CpfItem decoded = encodeDecode(item, CpfItem::encode, CpfItem::decode);

    assertEquals(item, decoded);
  }

  @DataProvider
  private static Object[][] getItems() {
    return new Object[][] {
      {
        new CipIdentityItem(
            0,
            new SockAddr(1, 2, new byte[4], 0),
            1,
            2,
            3,
            (short) 4,
            (short) 5,
            (short) 6,
            1234L,
            "test",
            (short) 0)
      },
      {new ConnectedAddressItem(1)},
      {new NullAddressItem()},
      {new SequencedAddressItem(1L, 2L)},
      {new SockAddrItemO2t(new SockAddr(1, 2, new byte[] {1, 2, 3, 4}, 0L))},
      {new SockAddrItemT2o(new SockAddr(1, 2, new byte[] {1, 2, 3, 4}, 0L))},
      {new CipSecurityItem(1, 2, 3, 4)}
    };
  }
}

```

---

### `ethernet-ip/enip-core/src/test/java/com/digitalpetri/enip/EnipStatusTest.java`

```java
package com.digitalpetri.enip;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

public class EnipStatusTest extends SerializationTest<EnipStatus> {

  @Test
  public void testSerialization() {
    for (EnipStatus status : EnipStatus.values()) {
      EnipStatus decoded = encodeDecode(status, EnipStatus::encode, EnipStatus::decode);

      assertEquals(status, decoded);
    }
  }
}

```

---

### `ethernet-ip/cip-core/src/main/java/com/digitalpetri/enip/cip/epath/DataSegment.java`

```java
package com.digitalpetri.enip.cip.epath;

import io.netty.buffer.ByteBuf;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public abstract class DataSegment<T> extends EPathSegment {

  public static final int SEGMENT_TYPE = 0x80;

  protected abstract ByteBuf encode(ByteBuf buffer);

  public static ByteBuf encode(DataSegment<?> segment, boolean padded, ByteBuf buffer) {
    return segment.encode(buffer);
  }

  public static final class AnsiDataSegment extends DataSegment<String> {

    public static final int SUBTYPE = 0x11;

    private final String data;
    private final Charset charset;

    public AnsiDataSegment(String data) {
      this(data, StandardCharsets.US_ASCII);
    }

    public AnsiDataSegment(String data, Charset charset) {
      this.data = data;
      this.charset = charset;
    }

    /**
     * Get the data string.
     *
     * @return the data string.
     */
    public String getData() {
      return data;
    }

    /**
     * Get the charset used to encode the data.
     *
     * @return the charset.
     */
    public Charset getCharset() {
      return charset;
    }

    @Override
    protected ByteBuf encode(ByteBuf buffer) {
      byte[] dataBytes = data.getBytes(charset);
      int dataLength = Math.min(dataBytes.length, 255);

      buffer.writeByte(SEGMENT_TYPE | SUBTYPE);
      buffer.writeByte(dataLength);
      buffer.writeBytes(dataBytes, 0, dataLength);
      if (dataLength % 2 != 0) buffer.writeByte(0);

      return buffer;
    }
  }

  public static final class SimpleDataSegment extends DataSegment<short[]> {

    private final short[] data;

    public SimpleDataSegment(short[] data) {
      this.data = data;
    }

    /**
     * Get the data array.
     *
     * @return the data array.
     */
    public short[] getData() {
      return data;
    }

    @Override
    protected ByteBuf encode(ByteBuf buffer) {
      buffer.writeByte(SEGMENT_TYPE);
      buffer.writeByte(data.length);

      for (short d : data) {
        buffer.writeShort(d);
      }

      return buffer;
    }
  }
}

```

---

### `ethernet-ip/cip-core/src/main/java/com/digitalpetri/enip/cip/epath/EPathSegment.java`

```java
package com.digitalpetri.enip.cip.epath;

public abstract class EPathSegment {}

```

---

### `ethernet-ip/cip-core/src/main/java/com/digitalpetri/enip/cip/epath/PortSegment.java`

```java
package com.digitalpetri.enip.cip.epath;

import io.netty.buffer.ByteBuf;

public final class PortSegment extends EPathSegment {

  private final int portId;
  private final byte[] linkAddress;

  public PortSegment(int portId, byte[] linkAddress) {
    this.portId = portId;
    this.linkAddress = linkAddress;
  }

  public int getPortId() {
    return portId;
  }

  public byte[] getLinkAddress() {
    return linkAddress;
  }

  private static final int EXTENDED_LINKED_ADDRESS_SIZE = 1 << 4;

  public static ByteBuf encode(PortSegment segment, boolean padded, ByteBuf buffer) {
    int writerIndex = buffer.writerIndex();
    int linkAddressLength = segment.getLinkAddress().length;
    boolean needLinkAddressSize = linkAddressLength > 1;
    boolean needExtendedPort = segment.portId > 14;

    int segmentByte = needExtendedPort ? 0x0F : segment.getPortId();
    if (needLinkAddressSize) segmentByte |= EXTENDED_LINKED_ADDRESS_SIZE;
    buffer.writeByte(segmentByte);

    if (needLinkAddressSize) buffer.writeByte(linkAddressLength);
    if (needExtendedPort) buffer.writeShort(segment.getPortId());
    buffer.writeBytes(segment.getLinkAddress());

    int bytesWritten = buffer.writerIndex() - writerIndex;
    if (bytesWritten % 2 != 0) buffer.writeByte(0);

    return buffer;
  }
}

```

---

### `ethernet-ip/cip-core/src/main/java/com/digitalpetri/enip/cip/epath/EPath.java`

```java
package com.digitalpetri.enip.cip.epath;

import io.netty.buffer.ByteBuf;

import java.util.List;

public abstract class EPath {

  private final EPathSegment[] segments;

  protected EPath(EPathSegment[] segments) {
    this.segments = segments;
  }

  public EPathSegment[] getSegments() {
    return segments;
  }

  public abstract boolean isPadded();

  public static ByteBuf encode(EPath path, ByteBuf buffer) {
    // length placeholder...
    int lengthStartIndex = buffer.writerIndex();
    buffer.writeByte(0);

    // encode the path segments...
    int dataStartIndex = buffer.writerIndex();

    for (EPathSegment segment : path.getSegments()) {
      if (segment instanceof LogicalSegment) {
        LogicalSegment.encode((LogicalSegment) segment, path.isPadded(), buffer);
      } else if (segment instanceof PortSegment) {
        PortSegment.encode((PortSegment) segment, path.isPadded(), buffer);
      } else if (segment instanceof DataSegment) {
        DataSegment.encode((DataSegment) segment, path.isPadded(), buffer);
      } else {
        throw new RuntimeException("no encoder for " + segment.getClass().getSimpleName());
      }
    }

    // go back and update the length
    int bytesWritten = buffer.writerIndex() - dataStartIndex;
    int wordsWritten = bytesWritten / 2;
    buffer.markWriterIndex();
    buffer.writerIndex(lengthStartIndex);
    buffer.writeByte(wordsWritten);
    buffer.resetWriterIndex();

    return buffer;
  }

  public static final class PaddedEPath extends EPath {

    public PaddedEPath(List<EPathSegment> segments) {
      this(segments.toArray(new EPathSegment[segments.size()]));
    }

    public PaddedEPath(EPathSegment... segments) {
      super(segments);
    }

    @Override
    public boolean isPadded() {
      return true;
    }

    public PaddedEPath append(PaddedEPath other) {
      int aLen = getSegments().length;
      int bLen = other.getSegments().length;

      EPathSegment[] newSegments = new EPathSegment[aLen + bLen];

      System.arraycopy(getSegments(), 0, newSegments, 0, aLen);
      System.arraycopy(other.getSegments(), 0, newSegments, aLen, bLen);

      return new PaddedEPath(newSegments);
    }
  }
}

```

---

### `ethernet-ip/cip-core/src/main/java/com/digitalpetri/enip/cip/epath/LogicalSegment.java`

```java
package com.digitalpetri.enip.cip.epath;

import com.digitalpetri.enip.cip.structs.ElectronicKey;
import io.netty.buffer.ByteBuf;

public abstract class LogicalSegment<T> extends EPathSegment {

  public static final int SEGMENT_TYPE = 0x01;

  private final T value;
  private final LogicalType type;
  private final LogicalFormat format;

  protected LogicalSegment(T value, LogicalType type, LogicalFormat format) {
    this.value = value;
    this.type = type;
    this.format = format;
  }

  public T getValue() {
    return value;
  }

  public LogicalType getType() {
    return type;
  }

  public LogicalFormat getFormat() {
    return format;
  }

  protected abstract ByteBuf encodeValue(ByteBuf buffer);

  protected static LogicalFormat smallestFormat(int value) {
    if ((value & 0xFF) == value) {
      return LogicalFormat.Bits_8;
    } else if ((value & 0xFFFF) == value) {
      return LogicalFormat.Bits_16;
    } else {
      return LogicalFormat.Bits_32;
    }
  }

  public static ByteBuf encode(LogicalSegment<?> segment, boolean padded, ByteBuf buffer) {
    int segmentByte = 0;

    segmentByte |= (SEGMENT_TYPE << 5);
    segmentByte |= (segment.getType().getType() << 2);
    segmentByte |= segment.getFormat().getType();

    buffer.writeByte(segmentByte);

    if (padded
        && (segment.getFormat() == LogicalFormat.Bits_16
            || segment.getFormat() == LogicalFormat.Bits_32)) {
      buffer.writeByte(0x00);
    }

    segment.encodeValue(buffer);

    return buffer;
  }

  private static ByteBuf encodeIntValue(LogicalFormat format, int value, ByteBuf buffer) {
    switch (format) {
      case Bits_8:
        return buffer.writeByte(value);
      case Bits_16:
        return buffer.writeShort(value);
      case Bits_32:
        return buffer.writeInt(value);
      case Reserved:
      default:
        throw new IllegalStateException("Reserved segment type not supported");
    }
  }

  private static ByteBuf encodeKeyValue(LogicalFormat format, ElectronicKey value, ByteBuf buffer) {
    return ElectronicKey.encode(value, buffer);
  }

  public static final class ClassId extends LogicalSegment<Integer> {

    public ClassId(Integer value) {
      this(value, smallestFormat(value));
    }

    public ClassId(int value, LogicalFormat format) {
      super(value, LogicalType.ClassId, format);
    }

    @Override
    protected ByteBuf encodeValue(ByteBuf buffer) {
      return encodeIntValue(getFormat(), getValue(), buffer);
    }
  }

  public static final class InstanceId extends LogicalSegment<Integer> {

    public InstanceId(int value) {
      this(value, smallestFormat(value));
    }

    public InstanceId(int value, LogicalFormat format) {
      super(value, LogicalType.InstanceId, format);
    }

    @Override
    protected ByteBuf encodeValue(ByteBuf buffer) {
      return encodeIntValue(getFormat(), getValue(), buffer);
    }
  }

  public static final class MemberId extends LogicalSegment<Integer> {

    public MemberId(Integer value) {
      this(value, smallestFormat(value));
    }

    public MemberId(Integer value, LogicalFormat format) {
      super(value, LogicalType.MemberId, format);
    }

    @Override
    protected ByteBuf encodeValue(ByteBuf buffer) {
      return encodeIntValue(getFormat(), getValue(), buffer);
    }
  }

  public static final class ConnectionPoint extends LogicalSegment<Integer> {

    public ConnectionPoint(Integer value) {
      this(value, smallestFormat(value));
    }

    public ConnectionPoint(Integer value, LogicalFormat format) {
      super(value, LogicalType.ConnectionPoint, format);
    }

    @Override
    protected ByteBuf encodeValue(ByteBuf buffer) {
      return encodeIntValue(getFormat(), getValue(), buffer);
    }
  }

  public static final class AttributeId extends LogicalSegment<Integer> {

    public AttributeId(Integer value) {
      super(value, LogicalType.AttributeId, smallestFormat(value));
    }

    public AttributeId(Integer value, LogicalFormat format) {
      super(value, LogicalType.AttributeId, format);
    }

    @Override
    protected ByteBuf encodeValue(ByteBuf buffer) {
      return encodeIntValue(getFormat(), getValue(), buffer);
    }
  }

  public static final class ServiceId extends LogicalSegment<Integer> {

    public ServiceId(Integer value, LogicalFormat format) {
      super(value, LogicalType.ServiceId, format);
    }

    @Override
    protected ByteBuf encodeValue(ByteBuf buffer) {
      return encodeIntValue(getFormat(), getValue(), buffer);
    }
  }

  public static final class KeySegment extends LogicalSegment<ElectronicKey> {

    public KeySegment(ElectronicKey value, LogicalFormat format) {
      super(value, LogicalType.Special, format);
    }

    @Override
    protected ByteBuf encodeValue(ByteBuf buffer) {
      return encodeKeyValue(getFormat(), getValue(), buffer);
    }
  }

  public static enum LogicalType {
    ClassId(0x0),
    InstanceId(0x1),
    MemberId(0x2),
    ConnectionPoint(0x3),
    AttributeId(0x4),
    Special(0x5),
    ServiceId(0x6),
    Reserved(0x7);

    private final int type;

    LogicalType(int type) {
      this.type = type;
    }

    public int getType() {
      return type;
    }
  }

  public static enum LogicalFormat {
    Bits_8(0x0),
    Bits_16(0x1),
    Bits_32(0x2),
    Reserved(0x3);

    private final int type;

    LogicalFormat(int type) {
      this.type = type;
    }

    public int getType() {
      return type;
    }
  }
}

```

---

### `ethernet-ip/cip-core/src/main/java/com/digitalpetri/enip/cip/services/MultipleServicePacketService.java`

```java
package com.digitalpetri.enip.cip.services;

import com.digitalpetri.enip.cip.CipResponseException;
import com.digitalpetri.enip.cip.epath.EPath.PaddedEPath;
import com.digitalpetri.enip.cip.epath.LogicalSegment.ClassId;
import com.digitalpetri.enip.cip.epath.LogicalSegment.InstanceId;
import com.digitalpetri.enip.cip.structs.MessageRouterRequest;
import com.digitalpetri.enip.cip.structs.MessageRouterResponse;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import static java.util.Collections.synchronizedList;

public class MultipleServicePacketService implements CipService<Void> {

  public static final int SERVICE_CODE = 0x0A;

  private static final PaddedEPath MESSAGE_ROUTER_PATH =
      new PaddedEPath(new ClassId(0x02), new InstanceId(0x01));

  private final List<CipService<?>> services;
  private final List<BiConsumer<?, Throwable>> consumers;

  private final List<CipService<?>> currentServices;
  private final List<BiConsumer<?, Throwable>> currentConsumers;

  public MultipleServicePacketService(
      List<CipService<?>> services, List<BiConsumer<?, Throwable>> consumers) {
    assert (services.size() == consumers.size());

    this.services = synchronizedList(new ArrayList<>(services));
    this.consumers = synchronizedList(new ArrayList<>(consumers));

    this.currentServices = synchronizedList(new ArrayList<>(services));
    this.currentConsumers = synchronizedList(new ArrayList<>(consumers));
  }

  @Override
  public void encodeRequest(ByteBuf buffer) {
    MessageRouterRequest request =
        new MessageRouterRequest(SERVICE_CODE, MESSAGE_ROUTER_PATH, this::encode);

    MessageRouterRequest.encode(request, buffer);
  }

  @Override
  public Void decodeResponse(ByteBuf buffer) throws CipResponseException, PartialResponseException {
    MessageRouterResponse response = MessageRouterResponse.decode(buffer);

    try {
      if (response.getGeneralStatus() == 0x00 || response.getGeneralStatus() == 0x1E) {
        List<Object[]> partials = new ArrayList<>();

        ByteBuf[] serviceData = decode(response.getData());

        for (int i = 0; i < serviceData.length; i++) {
          CipService<?> service = currentServices.get(i);

          @SuppressWarnings("unchecked")
          BiConsumer<Object, Throwable> consumer =
              (BiConsumer<Object, Throwable>) currentConsumers.get(i);

          try {
            consumer.accept(service.decodeResponse(serviceData[i]), null);
          } catch (PartialResponseException prx) {
            partials.add(new Object[] {service, consumer});
          } catch (Throwable t) {
            consumer.accept(null, t);
          } finally {
            ReferenceCountUtil.release(serviceData[i]);
          }
        }

        if (partials.isEmpty()) {
          // Reset to the original state.
          currentServices.clear();
          currentServices.addAll(services);

          currentConsumers.clear();
          currentConsumers.addAll(consumers);
        } else {
          // Keep sending only services that aren't done yet.
          currentServices.clear();
          currentConsumers.clear();

          for (Object[] oa : partials) {
            CipService<?> service = (CipService<?>) oa[0];

            @SuppressWarnings("unchecked")
            BiConsumer<Object, Throwable> consumer = (BiConsumer<Object, Throwable>) oa[1];

            currentServices.add(service);
            currentConsumers.add(consumer);
          }

          throw PartialResponseException.INSTANCE;
        }

        return null;

      } else {
        throw new CipResponseException(response.getGeneralStatus(), response.getAdditionalStatus());
      }
    } finally {
      ReferenceCountUtil.release(response.getData());
    }
  }

  private void encode(ByteBuf buffer) {
    int serviceCount = currentServices.size();

    buffer.writeShort(serviceCount);

    int[] offsets = new int[serviceCount];
    int offsetsStartIndex = buffer.writerIndex();
    buffer.writeZero(serviceCount * 2);

    for (int i = 0; i < serviceCount; i++) {
      offsets[i] = buffer.writerIndex() - offsetsStartIndex + 2;
      currentServices.get(i).encodeRequest(buffer);
    }

    buffer.markWriterIndex();
    buffer.writerIndex(offsetsStartIndex);
    for (int offset : offsets) {
      buffer.writeShort(offset);
    }
    buffer.resetWriterIndex();
  }

  private ByteBuf[] decode(ByteBuf buffer) {
    int dataStartIndex = buffer.readerIndex();
    int serviceCount = buffer.readUnsignedShort();

    int[] offsets = new int[serviceCount];
    for (int i = 0; i < serviceCount; i++) {
      offsets[i] = buffer.readUnsignedShort();
    }

    ByteBuf[] serviceData = new ByteBuf[serviceCount];
    for (int i = 0; i < serviceCount; i++) {
      int offset = offsets[i];

      int length = (i + 1 < serviceCount) ? offsets[i + 1] - offset : buffer.readableBytes();

      serviceData[i] = buffer.slice(dataStartIndex + offsets[i], length).retain();

      buffer.skipBytes(length);
    }

    return serviceData;
  }
}

```

---

### `ethernet-ip/cip-core/src/main/java/com/digitalpetri/enip/cip/services/GetAttributesAllService.java`

```java
package com.digitalpetri.enip.cip.services;

import com.digitalpetri.enip.cip.CipResponseException;
import com.digitalpetri.enip.cip.epath.EPath.PaddedEPath;
import com.digitalpetri.enip.cip.structs.MessageRouterRequest;
import com.digitalpetri.enip.cip.structs.MessageRouterResponse;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;

public class GetAttributesAllService implements CipService<ByteBuf> {

  public static final int SERVICE_CODE = 0x01;

  private final PaddedEPath requestPath;

  public GetAttributesAllService(PaddedEPath requestPath) {
    this.requestPath = requestPath;
  }

  @Override
  public void encodeRequest(ByteBuf buffer) {
    MessageRouterRequest request =
        new MessageRouterRequest(SERVICE_CODE, requestPath, byteBuf -> {});

    MessageRouterRequest.encode(request, buffer);
  }

  @Override
  public ByteBuf decodeResponse(ByteBuf buffer) throws CipResponseException {
    MessageRouterResponse response = MessageRouterResponse.decode(buffer);

    if (response.getGeneralStatus() == 0x00) {
      return response.getData();
    } else {
      ReferenceCountUtil.release(response.getData());

      throw new CipResponseException(response.getGeneralStatus(), response.getAdditionalStatus());
    }
  }
}

```

---

### `ethernet-ip/cip-core/src/main/java/com/digitalpetri/enip/cip/services/GetAttributeSingleService.java`

```java
package com.digitalpetri.enip.cip.services;

import com.digitalpetri.enip.cip.CipResponseException;
import com.digitalpetri.enip.cip.epath.EPath.PaddedEPath;
import com.digitalpetri.enip.cip.structs.MessageRouterRequest;
import com.digitalpetri.enip.cip.structs.MessageRouterResponse;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;

public class GetAttributeSingleService implements CipService<ByteBuf> {

  public static final int SERVICE_CODE = 0x0E;

  private final PaddedEPath requestPath;

  public GetAttributeSingleService(PaddedEPath requestPath) {
    this.requestPath = requestPath;
  }

  @Override
  public void encodeRequest(ByteBuf buffer) {
    MessageRouterRequest request =
        new MessageRouterRequest(SERVICE_CODE, requestPath, this::encode);

    MessageRouterRequest.encode(request, buffer);
  }

  @Override
  public ByteBuf decodeResponse(ByteBuf buffer)
      throws CipResponseException, PartialResponseException {
    MessageRouterResponse response = MessageRouterResponse.decode(buffer);

    try {
      if (response.getGeneralStatus() == 0x00) {
        return decode(response.getData());
      } else {
        throw new CipResponseException(response.getGeneralStatus(), response.getAdditionalStatus());
      }
    } finally {
      ReferenceCountUtil.release(response.getData());
    }
  }

  private void encode(ByteBuf buffer) {}

  private ByteBuf decode(ByteBuf buffer) {
    return buffer.readSlice(buffer.readableBytes()).retain();
  }
}

```

---

### `ethernet-ip/cip-core/src/main/java/com/digitalpetri/enip/cip/services/CipService.java`

```java
package com.digitalpetri.enip.cip.services;

import com.digitalpetri.enip.cip.CipResponseException;
import io.netty.buffer.ByteBuf;

public interface CipService<T> {

  void encodeRequest(ByteBuf buffer);

  T decodeResponse(ByteBuf buffer) throws CipResponseException, PartialResponseException;

  public static final class PartialResponseException extends Exception {
    public static final PartialResponseException INSTANCE = new PartialResponseException();
  }
}

```

---

### `ethernet-ip/cip-core/src/main/java/com/digitalpetri/enip/cip/services/ForwardCloseService.java`

```java
package com.digitalpetri.enip.cip.services;

import com.digitalpetri.enip.cip.CipResponseException;
import com.digitalpetri.enip.cip.epath.EPath.PaddedEPath;
import com.digitalpetri.enip.cip.epath.LogicalSegment.ClassId;
import com.digitalpetri.enip.cip.epath.LogicalSegment.InstanceId;
import com.digitalpetri.enip.cip.structs.ForwardCloseRequest;
import com.digitalpetri.enip.cip.structs.ForwardCloseResponse;
import com.digitalpetri.enip.cip.structs.MessageRouterRequest;
import com.digitalpetri.enip.cip.structs.MessageRouterResponse;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;

public class ForwardCloseService implements CipService<ForwardCloseResponse> {

  public static final int SERVICE_CODE = 0x4E;

  private static final PaddedEPath CONNECTION_MANAGER_PATH =
      new PaddedEPath(new ClassId(0x06), new InstanceId(0x01));

  private final ForwardCloseRequest request;

  public ForwardCloseService(ForwardCloseRequest request) {
    this.request = request;
  }

  @Override
  public void encodeRequest(ByteBuf buffer) {
    MessageRouterRequest mrr =
        new MessageRouterRequest(
            SERVICE_CODE, CONNECTION_MANAGER_PATH, b -> ForwardCloseRequest.encode(request, b));

    MessageRouterRequest.encode(mrr, buffer);
  }

  @Override
  public ForwardCloseResponse decodeResponse(ByteBuf buffer)
      throws CipResponseException, PartialResponseException {
    MessageRouterResponse mResponse = MessageRouterResponse.decode(buffer);

    int generalStatus = mResponse.getGeneralStatus();

    try {
      if (generalStatus == 0x00) {
        return ForwardCloseResponse.decode(mResponse.getData());
      } else {
        throw new CipResponseException(generalStatus, mResponse.getAdditionalStatus());
      }
    } finally {
      ReferenceCountUtil.release(mResponse.getData());
    }
  }
}

```

---

### `ethernet-ip/cip-core/src/main/java/com/digitalpetri/enip/cip/services/ForwardOpenService.java`

```java
package com.digitalpetri.enip.cip.services;

import com.digitalpetri.enip.cip.CipResponseException;
import com.digitalpetri.enip.cip.epath.EPath.PaddedEPath;
import com.digitalpetri.enip.cip.epath.LogicalSegment.ClassId;
import com.digitalpetri.enip.cip.epath.LogicalSegment.InstanceId;
import com.digitalpetri.enip.cip.structs.ForwardOpenRequest;
import com.digitalpetri.enip.cip.structs.ForwardOpenResponse;
import com.digitalpetri.enip.cip.structs.MessageRouterRequest;
import com.digitalpetri.enip.cip.structs.MessageRouterResponse;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;

public class ForwardOpenService implements CipService<ForwardOpenResponse> {

  public static final int SERVICE_CODE = 0x54;

  private static final PaddedEPath CONNECTION_MANAGER_PATH =
      new PaddedEPath(new ClassId(0x06), new InstanceId(0x01));

  private final ForwardOpenRequest request;

  public ForwardOpenService(ForwardOpenRequest request) {
    this.request = request;
  }

  @Override
  public void encodeRequest(ByteBuf buffer) {
    MessageRouterRequest mrr =
        new MessageRouterRequest(SERVICE_CODE, CONNECTION_MANAGER_PATH, this::encode);

    MessageRouterRequest.encode(mrr, buffer);
  }

  @Override
  public ForwardOpenResponse decodeResponse(ByteBuf buffer)
      throws CipResponseException, PartialResponseException {
    MessageRouterResponse mResponse = MessageRouterResponse.decode(buffer);

    int generalStatus = mResponse.getGeneralStatus();

    try {
      if (generalStatus == 0x00) {
        return ForwardOpenResponse.decode(mResponse.getData());
      } else {
        throw new CipResponseException(generalStatus, mResponse.getAdditionalStatus());
      }
    } finally {
      ReferenceCountUtil.release(mResponse.getData());
    }
  }

  private void encode(ByteBuf buffer) {
    ForwardOpenRequest.encode(request, buffer);
  }
}

```

---

### `ethernet-ip/cip-core/src/main/java/com/digitalpetri/enip/cip/services/UnconnectedSendService.java`

```java
package com.digitalpetri.enip.cip.services;

import com.digitalpetri.enip.cip.CipResponseException;
import com.digitalpetri.enip.cip.epath.DataSegment;
import com.digitalpetri.enip.cip.epath.EPath.PaddedEPath;
import com.digitalpetri.enip.cip.epath.EPathSegment;
import com.digitalpetri.enip.cip.epath.LogicalSegment;
import com.digitalpetri.enip.cip.epath.LogicalSegment.ClassId;
import com.digitalpetri.enip.cip.epath.LogicalSegment.InstanceId;
import com.digitalpetri.enip.cip.epath.PortSegment;
import com.digitalpetri.enip.cip.structs.MessageRouterRequest;
import com.digitalpetri.enip.util.TimeoutCalculator;
import io.netty.buffer.ByteBuf;

import java.time.Duration;

public class UnconnectedSendService<T> implements CipService<T> {

  public static final int SERVICE_CODE = 0x52;

  private static final PaddedEPath CONNECTION_MANAGER_PATH =
      new PaddedEPath(new ClassId(0x06), new InstanceId(0x01));

  private final CipService<T> service;
  private final PaddedEPath connectionPath;
  private final Duration timeout;

  public UnconnectedSendService(
      CipService<T> service, PaddedEPath connectionPath, Duration timeout) {

    this.service = service;
    this.connectionPath = connectionPath;
    this.timeout = timeout;
  }

  @Override
  public void encodeRequest(ByteBuf buffer) {
    MessageRouterRequest request =
        new MessageRouterRequest(SERVICE_CODE, CONNECTION_MANAGER_PATH, this::encode);

    MessageRouterRequest.encode(request, buffer);
  }

  private ByteBuf encode(ByteBuf buffer) {
    int priorityAndTimeoutBytes = TimeoutCalculator.calculateTimeoutBytes(timeout);

    // priority/timeTick & timeoutTicks
    buffer.writeByte(priorityAndTimeoutBytes >> 8 & 0xFF);
    buffer.writeByte(priorityAndTimeoutBytes & 0xFF);

    // message length + message
    int bytesWritten = encodeEmbeddedService(buffer);

    // pad byte if length was odd
    if (bytesWritten % 2 != 0) buffer.writeByte(0x00);

    // path length + reserved + path
    encodeConnectionPath(buffer);

    return buffer;
  }

  private int encodeEmbeddedService(ByteBuf buffer) {
    // length of embedded message
    int lengthStartIndex = buffer.writerIndex();
    buffer.writeShort(0);

    // embedded message
    int messageStartIndex = buffer.writerIndex();
    service.encodeRequest(buffer);

    // go back and update length
    int bytesWritten = buffer.writerIndex() - messageStartIndex;
    buffer.markWriterIndex();
    buffer.writerIndex(lengthStartIndex);
    buffer.writeShort(bytesWritten);
    buffer.resetWriterIndex();

    return bytesWritten;
  }

  private void encodeConnectionPath(ByteBuf buffer) {
    // connectionPath length
    int pathLengthStartIndex = buffer.writerIndex();
    buffer.writeByte(0);

    // reserved byte
    buffer.writeByte(0x00);

    // encode the path segments...
    int pathDataStartIndex = buffer.writerIndex();

    for (EPathSegment segment : connectionPath.getSegments()) {
      if (segment instanceof LogicalSegment) {
        LogicalSegment.encode((LogicalSegment) segment, connectionPath.isPadded(), buffer);
      } else if (segment instanceof PortSegment) {
        PortSegment.encode((PortSegment) segment, connectionPath.isPadded(), buffer);
      } else if (segment instanceof DataSegment) {
        DataSegment.encode((DataSegment) segment, connectionPath.isPadded(), buffer);
      } else {
        throw new RuntimeException("no encoder for " + segment.getClass().getSimpleName());
      }
    }

    // go back and update the length.
    int pathBytesWritten = buffer.writerIndex() - pathDataStartIndex;
    int wordsWritten = pathBytesWritten / 2;
    buffer.markWriterIndex();
    buffer.writerIndex(pathLengthStartIndex);
    buffer.writeByte(wordsWritten);
    buffer.resetWriterIndex();
  }

  @Override
  public T decodeResponse(ByteBuf buffer) throws CipResponseException, PartialResponseException {
    return service.decodeResponse(buffer);
  }
}

```

---

### `ethernet-ip/cip-core/src/main/java/com/digitalpetri/enip/cip/services/CipServiceInvoker.java`

```java
package com.digitalpetri.enip.cip.services;

import java.util.concurrent.CompletableFuture;

public interface CipServiceInvoker {

  /**
   * Invoke a service request using CIP unconnected messaging.
   *
   * <p>The service is invoked directly, without being passed through the Unconnected_Send service
   * of the Connection Manager object.
   *
   * @param service the service to invoke.
   * @return a {@link CompletableFuture} containing the eventual service response or failure.
   */
  <T> CompletableFuture<T> invoke(CipService<T> service);

  /**
   * Invoke a service request using CIP unconnected messaging, allowing for a number of retries if
   * the destination node returns an error status indicating it is currently busy.
   *
   * <p>The service is invoked directly, without being passed through the Unconnected_Send service
   * of the Connection Manager object.
   *
   * @param service the service to invoke.
   * @param maxRetries the maximum number of retries to attempt.
   * @return a {@link CompletableFuture} containing the eventual service response or failure.
   */
  <T> CompletableFuture<T> invoke(CipService<T> service, int maxRetries);

  /**
   * Invoke a service request using CIP connected messaging on the provided connection id.
   *
   * @param connectionId the id of the connection to use.
   * @param service the service to invoke.
   * @return a {@link CompletableFuture} containing the eventual service response or failure.
   */
  <T> CompletableFuture<T> invokeConnected(int connectionId, CipService<T> service);

  /**
   * Invoke a service request using CIP unconnected messaging using the Unconnected_Send Service
   * (0x52) of the Connection Manager object.
   *
   * @param service the service to invoke.
   * @return a {@link CompletableFuture} containing the eventual service response or failure.
   */
  <T> CompletableFuture<T> invokeUnconnected(CipService<T> service);

  /**
   * Invoke a service request using CIP unconnected messaging using the Unconnected_Send Service
   * (0x52) of the Connection Manager object, allowing for a number of retries if the destination
   * node returns an error status indicating it is currently busy.
   *
   * @param service the service to invoke.
   * @param maxRetries the maximum number of retries to attempt.
   * @return a {@link CompletableFuture} containing the eventual service response or failure.
   */
  <T> CompletableFuture<T> invokeUnconnected(CipService<T> service, int maxRetries);
}

```

---

### `ethernet-ip/cip-core/src/main/java/com/digitalpetri/enip/cip/services/GetAttributeListService.java`

```java
package com.digitalpetri.enip.cip.services;

import com.digitalpetri.enip.cip.CipResponseException;
import com.digitalpetri.enip.cip.epath.EPath.PaddedEPath;
import com.digitalpetri.enip.cip.structs.AttributeResponse;
import com.digitalpetri.enip.cip.structs.MessageRouterRequest;
import com.digitalpetri.enip.cip.structs.MessageRouterResponse;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;

public class GetAttributeListService implements CipService<AttributeResponse[]> {

  public static final int SERVICE_CODE = 0x03;

  private final PaddedEPath requestPath;
  private final int[] attributeIds;
  private final int[] attributeSizes;

  public GetAttributeListService(
      PaddedEPath requestPath, int[] attributeIds, int[] attributeSizes) {
    this.attributeIds = attributeIds;
    this.attributeSizes = attributeSizes;
    this.requestPath = requestPath;
  }

  @Override
  public void encodeRequest(ByteBuf buffer) {
    MessageRouterRequest request =
        new MessageRouterRequest(SERVICE_CODE, requestPath, this::encode);

    MessageRouterRequest.encode(request, buffer);
  }

  @Override
  public AttributeResponse[] decodeResponse(ByteBuf buffer)
      throws CipResponseException, PartialResponseException {
    MessageRouterResponse response = MessageRouterResponse.decode(buffer);

    try {
      if (response.getGeneralStatus() == 0x00) {
        return decode(response.getData());
      } else {
        throw new CipResponseException(response.getGeneralStatus(), response.getAdditionalStatus());
      }
    } finally {
      ReferenceCountUtil.release(response.getData());
    }
  }

  private void encode(ByteBuf buffer) {
    buffer.writeShort(attributeIds.length);

    for (int id : attributeIds) {
      buffer.writeShort(id);
    }
  }

  private AttributeResponse[] decode(ByteBuf buffer) {
    int count = buffer.readUnsignedShort();

    AttributeResponse[] attributeResponses = new AttributeResponse[count];

    for (int i = 0; i < count; i++) {
      int id = buffer.readUnsignedShort();
      int status = buffer.readUnsignedShort();
      ByteBuf data =
          status == 0x00 ? buffer.readSlice(attributeSizes[i]).copy() : Unpooled.EMPTY_BUFFER;

      attributeResponses[i] = new AttributeResponse(id, status, data);
    }

    return attributeResponses;
  }
}

```

---

### `ethernet-ip/cip-core/src/main/java/com/digitalpetri/enip/cip/services/SetAttributesAllService.java`

```java
package com.digitalpetri.enip.cip.services;

import com.digitalpetri.enip.cip.CipResponseException;
import com.digitalpetri.enip.cip.epath.EPath.PaddedEPath;
import com.digitalpetri.enip.cip.structs.MessageRouterRequest;
import com.digitalpetri.enip.cip.structs.MessageRouterResponse;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;

import java.util.function.Consumer;

public class SetAttributesAllService implements CipService<Void> {

  public static final int SERVICE_CODE = 0x02;

  private final PaddedEPath requestPath;
  private final Consumer<ByteBuf> attributeEncoder;

  public SetAttributesAllService(PaddedEPath requestPath, Consumer<ByteBuf> attributeEncoder) {
    this.requestPath = requestPath;
    this.attributeEncoder = attributeEncoder;
  }

  @Override
  public void encodeRequest(ByteBuf buffer) {
    MessageRouterRequest request =
        new MessageRouterRequest(SERVICE_CODE, requestPath, attributeEncoder);

    MessageRouterRequest.encode(request, buffer);
  }

  @Override
  public Void decodeResponse(ByteBuf buffer) throws CipResponseException, PartialResponseException {
    MessageRouterResponse response = MessageRouterResponse.decode(buffer);

    try {
      if (response.getGeneralStatus() == 0x00) {
        return null;
      } else {
        throw new CipResponseException(response.getGeneralStatus(), response.getAdditionalStatus());
      }
    } finally {
      ReferenceCountUtil.release(response.getData());
    }
  }
}

```

---

### `ethernet-ip/cip-core/src/main/java/com/digitalpetri/enip/cip/services/LargeForwardOpenService.java`

```java
package com.digitalpetri.enip.cip.services;

import com.digitalpetri.enip.cip.CipResponseException;
import com.digitalpetri.enip.cip.epath.EPath.PaddedEPath;
import com.digitalpetri.enip.cip.epath.LogicalSegment.ClassId;
import com.digitalpetri.enip.cip.epath.LogicalSegment.InstanceId;
import com.digitalpetri.enip.cip.structs.LargeForwardOpenRequest;
import com.digitalpetri.enip.cip.structs.LargeForwardOpenResponse;
import com.digitalpetri.enip.cip.structs.MessageRouterRequest;
import com.digitalpetri.enip.cip.structs.MessageRouterResponse;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;

public class LargeForwardOpenService implements CipService<LargeForwardOpenResponse> {

  public static final int SERVICE_CODE = 0x5B;

  private static final PaddedEPath CONNECTION_MANAGER_PATH =
      new PaddedEPath(new ClassId(0x06), new InstanceId(0x01));

  private final LargeForwardOpenRequest request;

  public LargeForwardOpenService(LargeForwardOpenRequest request) {
    this.request = request;
  }

  @Override
  public void encodeRequest(ByteBuf buffer) {
    MessageRouterRequest mrr =
        new MessageRouterRequest(SERVICE_CODE, CONNECTION_MANAGER_PATH, this::encode);

    MessageRouterRequest.encode(mrr, buffer);
  }

  @Override
  public LargeForwardOpenResponse decodeResponse(ByteBuf buffer)
      throws CipResponseException, PartialResponseException {
    MessageRouterResponse mResponse = MessageRouterResponse.decode(buffer);

    int generalStatus = mResponse.getGeneralStatus();

    try {
      if (generalStatus == 0x00) {
        return LargeForwardOpenResponse.decode(mResponse.getData());
      } else {
        throw new CipResponseException(generalStatus, mResponse.getAdditionalStatus());
      }
    } finally {
      ReferenceCountUtil.release(mResponse.getData());
    }
  }

  private void encode(ByteBuf buffer) {
    LargeForwardOpenRequest.encode(request, buffer);
  }
}

```

---

### `ethernet-ip/cip-core/src/main/java/com/digitalpetri/enip/cip/services/SetAttributeListService.java`

```java
package com.digitalpetri.enip.cip.services;

import com.digitalpetri.enip.cip.CipResponseException;
import com.digitalpetri.enip.cip.epath.EPath.PaddedEPath;
import com.digitalpetri.enip.cip.structs.AttributeRequest;
import com.digitalpetri.enip.cip.structs.AttributeResponse;
import com.digitalpetri.enip.cip.structs.MessageRouterRequest;
import com.digitalpetri.enip.cip.structs.MessageRouterResponse;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;

import java.util.function.Function;

public class SetAttributeListService implements CipService<AttributeResponse[]> {

  public static final int SERVICE_CODE = 0x04;

  private final PaddedEPath requestPath;
  private final AttributeRequest[] attributeRequests;
  private final Function<Integer, ByteBuf> attributeDataDecoder;

  public SetAttributeListService(
      PaddedEPath requestPath,
      AttributeRequest[] attributeRequests,
      Function<Integer, ByteBuf> attributeDataDecoder) {

    this.requestPath = requestPath;
    this.attributeRequests = attributeRequests;
    this.attributeDataDecoder = attributeDataDecoder;
  }

  @Override
  public void encodeRequest(ByteBuf buffer) {
    MessageRouterRequest request =
        new MessageRouterRequest(SERVICE_CODE, requestPath, this::encode);

    MessageRouterRequest.encode(request, buffer);
  }

  @Override
  public AttributeResponse[] decodeResponse(ByteBuf buffer)
      throws CipResponseException, PartialResponseException {
    MessageRouterResponse response = MessageRouterResponse.decode(buffer);

    try {
      if (response.getGeneralStatus() == 0x00) {
        return decode(buffer);
      } else {
        throw new CipResponseException(response.getGeneralStatus(), response.getAdditionalStatus());
      }
    } finally {
      ReferenceCountUtil.release(buffer);
    }
  }

  private void encode(ByteBuf buffer) {
    buffer.writeShort(attributeRequests.length);

    for (AttributeRequest request : attributeRequests) {
      buffer.writeShort(request.getId());
      buffer.writeBytes(request.getData());
    }
  }

  private AttributeResponse[] decode(ByteBuf buffer) {
    int count = buffer.readUnsignedShort();

    AttributeResponse[] responses = new AttributeResponse[count];

    for (int i = 0; i < count; i++) {
      int id = buffer.readShort();
      int status = buffer.readShort();

      ByteBuf data = (status == 0x00) ? attributeDataDecoder.apply(id) : Unpooled.EMPTY_BUFFER;

      responses[i] = new AttributeResponse(id, status, data);
    }

    return responses;
  }
}

```

---

### `ethernet-ip/cip-core/src/main/java/com/digitalpetri/enip/cip/CipResponseException.java`

```java
package com.digitalpetri.enip.cip;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class CipResponseException extends Exception {

  private final int generalStatus;
  private final int[] additionalStatus;

  public CipResponseException(int generalStatus, int[] additionalStatus) {
    this.generalStatus = generalStatus;
    this.additionalStatus = additionalStatus;
  }

  public int getGeneralStatus() {
    return generalStatus;
  }

  public int[] getAdditionalStatus() {
    return additionalStatus;
  }

  @Override
  public String getMessage() {
    StringBuilder sb = new StringBuilder();

    sb.append(String.format("status=0x%02X", generalStatus));

    CipStatusCodes.getName(generalStatus)
        .ifPresent(name -> sb.append(" [").append(name).append("] "));

    List<String> as =
        Arrays.stream(additionalStatus)
            .mapToObj(a -> String.format("0x%04X", a))
            .collect(Collectors.toList());

    String additional = "[" + String.join(",", as) + "]";

    sb.append(", additional=").append(additional);

    return sb.toString();
  }

  /**
   * If {@code ex} is a {@link CipResponseException}, or if a {@link CipResponseException} can be
   * found by walking the exception cause chain, return it.
   *
   * @param ex the {@link Throwable} to extract from.
   * @return a {@link CipResponseException} if one was present in the exception chain.
   */
  public static Optional<CipResponseException> extract(Throwable ex) {
    if (ex instanceof CipResponseException) {
      return Optional.of((CipResponseException) ex);
    } else {
      Throwable cause = ex.getCause();
      return cause != null ? extract(cause) : Optional.empty();
    }
  }
}

```

---

### `ethernet-ip/cip-core/src/main/java/com/digitalpetri/enip/cip/CipStatusCodes.java`

```java
package com.digitalpetri.enip.cip;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public abstract class CipStatusCodes {

  private CipStatusCodes() {}

  private static final Map<Integer, NameAndDescription> STATUS_CODES;

  static {
    STATUS_CODES = new HashMap<>();
    STATUS_CODES.put(
        0x00, v("success", "Service was successfully performed by the object specified."));
    STATUS_CODES.put(
        0x01,
        v("connection failure", "A connection related service failed along the connection path."));
    STATUS_CODES.put(
        0x02,
        v(
            "resource unavailable",
            "Resources needed for the object to perform the requested service were unavailable."));
    STATUS_CODES.put(
        0x03,
        v(
            "invalid parameter value",
            "A parameter associated with the request was invalid. This code is used when a"
                + " parameter does not meet the requirements of this specification and/or the"
                + " requirements defined in an Application Object Specification."));
    STATUS_CODES.put(
        0x04,
        v(
            "path segment error",
            "The path segment identifier or the segment syntax was not understood by the processing"
                + " node. Path processing shall stop when a path segment error is encountered."));
    STATUS_CODES.put(
        0x05,
        v(
            "path destination unknown",
            "The path is referencing an object class, instance or structure element that is not"
                + " known or is not contained in the processing node. Path processing shall stop"
                + " when a path destination unknown error is encountered."));
    STATUS_CODES.put(
        0x06, v("partial transfer", "Only part of the expected data was transferred."));
    STATUS_CODES.put(0x07, v("connection lost", "The messaging connection was lost."));
    STATUS_CODES.put(
        0x08,
        v(
            "service not supported",
            "The requested service was not implemented or was not defined for this Object"
                + " Class/Instance."));
    STATUS_CODES.put(0x09, v("invalid attribute value", "Invalid attribute data detected."));
    STATUS_CODES.put(
        0x0A,
        v(
            "attribute list error",
            "An attribute in the Get_Attribute_List or Set_Attribute_List response has a non-zero"
                + " status."));
    STATUS_CODES.put(
        0x0B,
        v(
            "already in requested mode/state",
            "The object is already in the mode/state being requested by the service."));
    STATUS_CODES.put(
        0x0C,
        v(
            "object state conflict",
            "The object cannot perform the requested service in its current mode/state."));
    STATUS_CODES.put(
        0x0D,
        v(
            "object already exists",
            "The requested instance of object to be created already exists."));
    STATUS_CODES.put(
        0x0E,
        v(
            "attribute not settable",
            "A request to modify a non-modifiable attribute was received."));
    STATUS_CODES.put(0x0F, v("privilege violation", "A permission/privilege check failed."));
    STATUS_CODES.put(
        0x10,
        v(
            "device state conflict",
            "The device’s current mode/state prohibits the execution of the requested service."));
    STATUS_CODES.put(
        0x11,
        v(
            "reply data too large",
            "The data to be transmitted in the response buffer is larger than the allocated"
                + " response buffer."));
    STATUS_CODES.put(
        0x12,
        v(
            "fragmentation of a primitive value",
            "The service specified an operation that is going to fragment a primitive data value,"
                + " i.e. half a REAL data type."));
    STATUS_CODES.put(
        0x13,
        v(
            "not enough data",
            "The service did not supply enough data to perform the specified operation."));
    STATUS_CODES.put(
        0x14,
        v("attribute not supported", "The attribute specified in the request is not supported."));
    STATUS_CODES.put(0x15, v("too much data", "The service supplied more data than was expected."));
    STATUS_CODES.put(
        0x16, v("object does not exist", "The object specified does not exist in the device."));
    STATUS_CODES.put(
        0x17,
        v(
            "service fragmentation sequence not in progress",
            "The fragmentation sequence for this service is not currently active for this data."));
    STATUS_CODES.put(
        0x18,
        v(
            "no stored attribute data",
            "The attribute data of this object was not saved prior to the requested service."));
    STATUS_CODES.put(
        0x19,
        v(
            "store operation failure",
            "The attribute data of this object was not saved due to a failure during the"
                + " attempt."));
    STATUS_CODES.put(
        0x1A,
        v(
            "routing failure, request packet too large",
            "The service request packet was too large for transmission on a network in the path to"
                + " the destination. The routing device was forced to abort the service."));
    STATUS_CODES.put(
        0x1B,
        v(
            "routing failure, response packet too large",
            "The service response packet was too large for transmission on a network in the path"
                + " from the destination. The routing device was forced to abort the service."));
    STATUS_CODES.put(
        0x1C,
        v(
            "missing attribute list entry data",
            "The service did not supply an attribute in a list of attributes that was needed by the"
                + " service to perform the requested behavior."));
    STATUS_CODES.put(
        0x1D,
        v(
            "invalid attribute value list",
            "The service is returning the list of attributes supplied with status information for"
                + " those attributes that were invalid."));
    STATUS_CODES.put(
        0x1E, v("embedded service error", "An embedded service resulted in an error."));
    STATUS_CODES.put(
        0x1F, v("vendor specific error", "A vendor specific error has been encountered."));
    STATUS_CODES.put(
        0x20,
        v(
            "invalid parameter",
            "A parameter associated with the request was invalid. This code is used when a"
                + " parameter does not meet the requirements of this specification and/or the"
                + " requirements defined in an Application Object Specification."));
    STATUS_CODES.put(
        0x21,
        v(
            "write-once value or medium already written",
            "An attempt was made to write to a write-once medium (e.g. WORM drive, PROM) that has"
                + " already been written, or to modify a value that cannot be changed once"
                + " established."));
    STATUS_CODES.put(
        0x22,
        v(
            "invalid reply received",
            "An invalid reply is received (e.g. reply service code does not match the request"
                + " service code, or reply message is shorter than the minimum expected reply"
                + " size). This status code can serve for other causes of invalid replies."));
    STATUS_CODES.put(
        0x23,
        v(
            "buffer overflow",
            "The message received is larger than the receiving buffer can handle. The entire"
                + " message was discarded."));
    STATUS_CODES.put(
        0x24,
        v(
            "message format error",
            "The format of the received message is not supported by the server."));
    STATUS_CODES.put(
        0x25,
        v(
            "key failure in path",
            "The Key Segment that was included as the first segment in the path does not match the"
                + " destination module. The object specific status shall indicate which part of the"
                + " key check failed."));
    STATUS_CODES.put(
        0x26,
        v(
            "path size invalid",
            "The size of the path which was sent with the Service Request is either not large"
                + " enough to allow the Request to be routed to an object or too much routing data"
                + " was included."));
    STATUS_CODES.put(
        0x27,
        v(
            "unexpected attribute in list",
            "An attempt was made to set an attribute that is not able to be set at this time."));
    STATUS_CODES.put(
        0x28,
        v(
            "invalid member id",
            "The Member ID specified in the request does not exist in the specified"
                + " Class/Instance/Attribute."));
    STATUS_CODES.put(
        0x29,
        v("member not settable", "A request to modify a non-modifiable member was received."));
    STATUS_CODES.put(
        0x2A,
        v(
            "group 2 only server general failure",
            "This error code may only be reported by DeviceNet Group 2 Only servers with 4K or less"
                + " code space and only in place of Service not supported, Attribute not supported"
                + " and Attribute not settable."));
    STATUS_CODES.put(
        0x2B,
        v(
            "unknown modbus error",
            "A CIP to Modbus translator received an unknown Modbus Exception Code."));
    STATUS_CODES.put(
        0x2C,
        v("attribute not gettable", "A request to read a non-readable attribute was received."));
    STATUS_CODES.put(
        0x2D, v("instance not deletable", "The requested object instance cannot be deleted."));
  }

  public static Optional<String> getName(int statusCode) {
    NameAndDescription nameAndDescription =
        STATUS_CODES.getOrDefault(statusCode, NameAndDescription.NULL);

    return Optional.ofNullable(nameAndDescription.name);
  }

  public static Optional<String> getDescription(int statusCode) {
    NameAndDescription nameAndDescription =
        STATUS_CODES.getOrDefault(statusCode, NameAndDescription.NULL);

    return Optional.ofNullable(nameAndDescription.description);
  }

  private static NameAndDescription v(String name, String description) {
    return new NameAndDescription(name, description);
  }

  private static class NameAndDescription {
    static final NameAndDescription NULL = new NameAndDescription(null, null);

    final String name;
    final String description;

    public NameAndDescription(String name, String description) {
      this.name = name;
      this.description = description;
    }
  }
}

```

---

### `ethernet-ip/cip-core/src/main/java/com/digitalpetri/enip/cip/structs/AttributeRequest.java`

```java
package com.digitalpetri.enip.cip.structs;

import io.netty.buffer.ByteBuf;

public class AttributeRequest {

  private final int id;
  private final ByteBuf data;

  public AttributeRequest(int id, ByteBuf data) {
    this.id = id;
    this.data = data;
  }

  public int getId() {
    return id;
  }

  public ByteBuf getData() {
    return data;
  }
}

```

---

### `ethernet-ip/cip-core/src/main/java/com/digitalpetri/enip/cip/structs/ForwardOpenRequest.java`

```java
package com.digitalpetri.enip.cip.structs;

import com.digitalpetri.enip.cip.epath.EPath;
import com.digitalpetri.enip.util.TimeoutCalculator;
import io.netty.buffer.ByteBuf;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public final class ForwardOpenRequest {

  private final Duration timeout;
  private final int o2tConnectionId;
  private final int t2oConnectionId;
  private final int connectionSerialNumber;
  private final int vendorId;
  private final long vendorSerialNumber;
  private final int connectionTimeoutMultiplier;
  private final EPath.PaddedEPath connectionPath;
  private final Duration o2tRpi;
  private final NetworkConnectionParameters o2tParameters;
  private final Duration t2oRpi;
  private final NetworkConnectionParameters t2oParameters;
  private final int transportClassAndTrigger;

  public ForwardOpenRequest(
      Duration timeout,
      int o2tConnectionId,
      int t2oConnectionId,
      int connectionSerialNumber,
      int vendorId,
      long vendorSerialNumber,
      int connectionTimeoutMultiplier,
      EPath.PaddedEPath connectionPath,
      Duration o2tRpi,
      NetworkConnectionParameters o2tParameters,
      Duration t2oRpi,
      NetworkConnectionParameters t2oParameters,
      int transportClassAndTrigger) {

    this.timeout = timeout;
    this.o2tConnectionId = o2tConnectionId;
    this.t2oConnectionId = t2oConnectionId;
    this.connectionSerialNumber = connectionSerialNumber;
    this.vendorId = vendorId;
    this.vendorSerialNumber = vendorSerialNumber;
    this.connectionTimeoutMultiplier = connectionTimeoutMultiplier;
    this.connectionPath = connectionPath;
    this.o2tRpi = o2tRpi;
    this.o2tParameters = o2tParameters;
    this.t2oRpi = t2oRpi;
    this.t2oParameters = t2oParameters;
    this.transportClassAndTrigger = transportClassAndTrigger;
  }

  public Duration getTimeout() {
    return timeout;
  }

  public int getO2tConnectionId() {
    return o2tConnectionId;
  }

  public int getT2oConnectionId() {
    return t2oConnectionId;
  }

  public int getConnectionSerialNumber() {
    return connectionSerialNumber;
  }

  public int getVendorId() {
    return vendorId;
  }

  public long getVendorSerialNumber() {
    return vendorSerialNumber;
  }

  public int getConnectionTimeoutMultiplier() {
    return connectionTimeoutMultiplier;
  }

  public EPath.PaddedEPath getConnectionPath() {
    return connectionPath;
  }

  public Duration getO2tRpi() {
    return o2tRpi;
  }

  public NetworkConnectionParameters getO2tParameters() {
    return o2tParameters;
  }

  public Duration getT2oRpi() {
    return t2oRpi;
  }

  public NetworkConnectionParameters getT2oParameters() {
    return t2oParameters;
  }

  public int getTransportClassAndTrigger() {
    return transportClassAndTrigger;
  }

  public static ByteBuf encode(ForwardOpenRequest request, ByteBuf buffer) {
    int priorityAndTimeoutBytes = TimeoutCalculator.calculateTimeoutBytes(request.timeout);
    buffer.writeByte(priorityAndTimeoutBytes >> 8 & 0xFF);
    buffer.writeByte(priorityAndTimeoutBytes & 0xFF);

    buffer.writeInt(0); // o2tConnectionId chosen by remote and indicated in response
    buffer.writeInt(request.t2oConnectionId);
    buffer.writeShort(request.connectionSerialNumber);

    buffer.writeShort(request.vendorId);
    buffer.writeInt((int) request.vendorSerialNumber);

    buffer.writeByte(request.connectionTimeoutMultiplier);
    buffer.writeZero(3); // 3 reserved bytes

    buffer.writeInt(
        (int) TimeUnit.MICROSECONDS.convert(request.o2tRpi.toMillis(), TimeUnit.MILLISECONDS));
    buffer.writeShort(parametersToInt(request.o2tParameters));

    buffer.writeInt(
        (int) TimeUnit.MICROSECONDS.convert(request.t2oRpi.toMillis(), TimeUnit.MILLISECONDS));
    buffer.writeShort(parametersToInt(request.t2oParameters));

    buffer.writeByte(request.transportClassAndTrigger);

    EPath.PaddedEPath.encode(request.connectionPath, buffer);

    return buffer;
  }

  private static int parametersToInt(NetworkConnectionParameters parameters) {
    int parametersInt = parameters.getConnectionSize() & 0x1FF;

    parametersInt |= (parameters.getSizeType().ordinal() << 9);
    parametersInt |= (parameters.getPriority().ordinal() << 10);
    parametersInt |= (parameters.getConnectionType().ordinal() << 13);
    if (parameters.isRedundantOwner()) parametersInt |= (1 << 15);

    return parametersInt;
  }
}

```

---

### `ethernet-ip/cip-core/src/main/java/com/digitalpetri/enip/cip/structs/MessageRouterRequest.java`

```java
package com.digitalpetri.enip.cip.structs;

import com.digitalpetri.enip.cip.epath.EPath;
import io.netty.buffer.ByteBuf;

import java.util.function.Consumer;

public class MessageRouterRequest {

  private final int serviceCode;
  private final EPath.PaddedEPath requestPath;
  private final Consumer<ByteBuf> dataEncoder;

  public MessageRouterRequest(int serviceCode, EPath.PaddedEPath requestPath, ByteBuf requestData) {
    this.serviceCode = serviceCode;
    this.requestPath = requestPath;
    this.dataEncoder = (buffer) -> buffer.writeBytes(requestData);
  }

  public MessageRouterRequest(
      int serviceCode, EPath.PaddedEPath requestPath, Consumer<ByteBuf> dataEncoder) {
    this.serviceCode = serviceCode;
    this.requestPath = requestPath;
    this.dataEncoder = dataEncoder;
  }

  public static void encode(MessageRouterRequest request, ByteBuf buffer) {
    buffer.writeByte(request.serviceCode);
    EPath.encode(request.requestPath, buffer);
    request.dataEncoder.accept(buffer);
  }
}

```

---

### `ethernet-ip/cip-core/src/main/java/com/digitalpetri/enip/cip/structs/NetworkConnectionParameters.java`

```java
package com.digitalpetri.enip.cip.structs;

public class NetworkConnectionParameters {

  private final int connectionSize;
  private final SizeType sizeType;
  private final Priority priority;
  private final ConnectionType connectionType;
  private final boolean redundantOwner;

  public NetworkConnectionParameters(
      int connectionSize,
      SizeType sizeType,
      Priority priority,
      ConnectionType connectionType,
      boolean redundantOwner) {

    this.connectionSize = connectionSize;
    this.sizeType = sizeType;
    this.priority = priority;
    this.connectionType = connectionType;
    this.redundantOwner = redundantOwner;
  }

  public int getConnectionSize() {
    return connectionSize;
  }

  public SizeType getSizeType() {
    return sizeType;
  }

  public Priority getPriority() {
    return priority;
  }

  public ConnectionType getConnectionType() {
    return connectionType;
  }

  public boolean isRedundantOwner() {
    return redundantOwner;
  }

  public static enum SizeType {
    Fixed,
    Variable
  }

  public static enum Priority {
    Low,
    High,
    Scheduled,
    Urgent
  }

  public static enum ConnectionType {
    Null,
    Multicast,
    PointToPoint,
    Reserved
  }
}

```

---

### `ethernet-ip/cip-core/src/main/java/com/digitalpetri/enip/cip/structs/ForwardCloseRequest.java`

```java
package com.digitalpetri.enip.cip.structs;

import com.digitalpetri.enip.cip.epath.*;
import com.digitalpetri.enip.cip.epath.EPath.PaddedEPath;
import com.digitalpetri.enip.util.TimeoutCalculator;
import io.netty.buffer.ByteBuf;

import java.time.Duration;

public class ForwardCloseRequest {

  private final Duration connectionTimeout;
  private final int connectionSerialNumber;
  private final int originatorVendorId;
  private final long originatorSerialNumber;
  private final PaddedEPath connectionPath;

  public ForwardCloseRequest(
      Duration connectionTimeout,
      int connectionSerialNumber,
      int originatorVendorId,
      long originatorSerialNumber,
      PaddedEPath connectionPath) {

    this.connectionTimeout = connectionTimeout;
    this.connectionSerialNumber = connectionSerialNumber;
    this.originatorVendorId = originatorVendorId;
    this.originatorSerialNumber = originatorSerialNumber;
    this.connectionPath = connectionPath;
  }

  public Duration getConnectionTimeout() {
    return connectionTimeout;
  }

  public int getConnectionSerialNumber() {
    return connectionSerialNumber;
  }

  public int getOriginatorVendorId() {
    return originatorVendorId;
  }

  public long getOriginatorSerialNumber() {
    return originatorSerialNumber;
  }

  public PaddedEPath getConnectionPath() {
    return connectionPath;
  }

  public static ByteBuf encode(ForwardCloseRequest request, ByteBuf buffer) {
    int priorityAndTimeoutBytes =
        TimeoutCalculator.calculateTimeoutBytes(request.getConnectionTimeout());
    buffer.writeByte(priorityAndTimeoutBytes >> 8 & 0xFF);
    buffer.writeByte(priorityAndTimeoutBytes & 0xFF);

    buffer.writeShort(request.getConnectionSerialNumber());

    buffer.writeShort(request.getOriginatorVendorId());
    buffer.writeInt((int) request.getOriginatorSerialNumber());

    encodeConnectionPath(request.getConnectionPath(), buffer);

    return buffer;
  }

  /**
   * Encode the connection path.
   *
   * <p>{@link PaddedEPath#encode(EPath, ByteBuf)} can't be used here because the {@link
   * ForwardCloseRequest} has an extra reserved byte after the connection path size for some reason.
   *
   * @param path the {@link PaddedEPath} to encode.
   * @param buffer the {@link ByteBuf} to encode into.
   */
  private static void encodeConnectionPath(PaddedEPath path, ByteBuf buffer) {
    // length placeholder...
    int lengthStartIndex = buffer.writerIndex();
    buffer.writeByte(0);

    // reserved
    buffer.writeZero(1);

    // encode the path segments...
    int dataStartIndex = buffer.writerIndex();

    for (EPathSegment segment : path.getSegments()) {
      if (segment instanceof LogicalSegment) {
        LogicalSegment.encode((LogicalSegment) segment, path.isPadded(), buffer);
      } else if (segment instanceof PortSegment) {
        PortSegment.encode((PortSegment) segment, path.isPadded(), buffer);
      } else if (segment instanceof DataSegment) {
        DataSegment.encode((DataSegment) segment, path.isPadded(), buffer);
      } else {
        throw new RuntimeException("no encoder for " + segment.getClass().getSimpleName());
      }
    }

    // go back and update the length
    int bytesWritten = buffer.writerIndex() - dataStartIndex;
    int wordsWritten = bytesWritten / 2;
    buffer.markWriterIndex();
    buffer.writerIndex(lengthStartIndex);
    buffer.writeByte(wordsWritten);
    buffer.resetWriterIndex();
  }
}

```

---

### `ethernet-ip/cip-core/src/main/java/com/digitalpetri/enip/cip/structs/AttributeResponse.java`

```java
package com.digitalpetri.enip.cip.structs;

import io.netty.buffer.ByteBuf;

public final class AttributeResponse {

  private final int id;
  private final int status;
  private final ByteBuf data;

  public AttributeResponse(int id, int status, ByteBuf data) {
    this.id = id;
    this.status = status;
    this.data = data;
  }

  public int getId() {
    return id;
  }

  public int getStatus() {
    return status;
  }

  public ByteBuf getData() {
    return data;
  }
}

```

---

### `ethernet-ip/cip-core/src/main/java/com/digitalpetri/enip/cip/structs/LargeForwardOpenRequest.java`

```java
package com.digitalpetri.enip.cip.structs;

import com.digitalpetri.enip.cip.epath.EPath;
import com.digitalpetri.enip.cip.epath.EPath.PaddedEPath;
import com.digitalpetri.enip.util.TimeoutCalculator;
import io.netty.buffer.ByteBuf;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class LargeForwardOpenRequest {

  private final Duration timeout;
  private final int o2tConnectionId;
  private final int t2oConnectionId;
  private final int connectionSerialNumber;
  private final int vendorId;
  private final long vendorSerialNumber;
  private final int connectionTimeoutMultiplier;
  private final EPath.PaddedEPath connectionPath;
  private final Duration o2tRpi;
  private final NetworkConnectionParameters o2tParameters;
  private final Duration t2oRpi;
  private final NetworkConnectionParameters t2oParameters;
  private final int transportClassAndTrigger;

  public LargeForwardOpenRequest(
      Duration timeout,
      int o2tConnectionId,
      int t2oConnectionId,
      int connectionSerialNumber,
      int vendorId,
      long vendorSerialNumber,
      int connectionTimeoutMultiplier,
      PaddedEPath connectionPath,
      Duration o2tRpi,
      NetworkConnectionParameters o2tParameters,
      Duration t2oRpi,
      NetworkConnectionParameters t2oParameters,
      int transportClassAndTrigger) {

    this.timeout = timeout;
    this.o2tConnectionId = o2tConnectionId;
    this.t2oConnectionId = t2oConnectionId;
    this.connectionSerialNumber = connectionSerialNumber;
    this.vendorId = vendorId;
    this.vendorSerialNumber = vendorSerialNumber;
    this.connectionTimeoutMultiplier = connectionTimeoutMultiplier;
    this.connectionPath = connectionPath;
    this.o2tRpi = o2tRpi;
    this.o2tParameters = o2tParameters;
    this.t2oRpi = t2oRpi;
    this.t2oParameters = t2oParameters;
    this.transportClassAndTrigger = transportClassAndTrigger;
  }

  public Duration getTimeout() {
    return timeout;
  }

  public int getO2tConnectionId() {
    return o2tConnectionId;
  }

  public int getT2oConnectionId() {
    return t2oConnectionId;
  }

  public int getConnectionSerialNumber() {
    return connectionSerialNumber;
  }

  public int getVendorId() {
    return vendorId;
  }

  public long getVendorSerialNumber() {
    return vendorSerialNumber;
  }

  public int getConnectionTimeoutMultiplier() {
    return connectionTimeoutMultiplier;
  }

  public PaddedEPath getConnectionPath() {
    return connectionPath;
  }

  public Duration getO2tRpi() {
    return o2tRpi;
  }

  public NetworkConnectionParameters getO2tParameters() {
    return o2tParameters;
  }

  public Duration getT2oRpi() {
    return t2oRpi;
  }

  public NetworkConnectionParameters getT2oParameters() {
    return t2oParameters;
  }

  public int getTransportClassAndTrigger() {
    return transportClassAndTrigger;
  }

  public static ByteBuf encode(LargeForwardOpenRequest request, ByteBuf buffer) {
    int priorityAndTimeoutBytes = TimeoutCalculator.calculateTimeoutBytes(request.timeout);
    buffer.writeByte(priorityAndTimeoutBytes >> 8 & 0xFF);
    buffer.writeByte(priorityAndTimeoutBytes & 0xFF);

    buffer.writeInt(0); // o2tConnectionId chosen by remote and indicated in response
    buffer.writeInt(request.t2oConnectionId);
    buffer.writeShort(request.connectionSerialNumber);

    buffer.writeShort(request.vendorId);
    buffer.writeInt((int) request.vendorSerialNumber);

    buffer.writeByte(request.connectionTimeoutMultiplier);
    buffer.writeZero(3); // 3 reserved bytes

    buffer.writeInt(
        (int) TimeUnit.MICROSECONDS.convert(request.o2tRpi.toMillis(), TimeUnit.MILLISECONDS));
    buffer.writeInt(parametersToInt(request.o2tParameters));

    buffer.writeInt(
        (int) TimeUnit.MICROSECONDS.convert(request.t2oRpi.toMillis(), TimeUnit.MILLISECONDS));
    buffer.writeInt(parametersToInt(request.t2oParameters));

    buffer.writeByte(request.transportClassAndTrigger);

    EPath.PaddedEPath.encode(request.connectionPath, buffer);

    return buffer;
  }

  private static int parametersToInt(NetworkConnectionParameters parameters) {
    int parametersInt = parameters.getConnectionSize() & 0xFFFF;

    parametersInt |= (parameters.getSizeType().ordinal() << 25);
    parametersInt |= (parameters.getPriority().ordinal() << 26);
    parametersInt |= (parameters.getConnectionType().ordinal() << 29);
    if (parameters.isRedundantOwner()) parametersInt |= (1 << 31);

    return parametersInt;
  }
}

```

---

### `ethernet-ip/cip-core/src/main/java/com/digitalpetri/enip/cip/structs/ForwardCloseResponse.java`

```java
package com.digitalpetri.enip.cip.structs;

import io.netty.buffer.ByteBuf;

public class ForwardCloseResponse {

  private final int connectionSerialNumber;
  private final int originatorVendorId;
  private final long originatorSerialNumber;

  public ForwardCloseResponse(
      int connectionSerialNumber, int originatorVendorId, long originatorSerialNumber) {
    this.connectionSerialNumber = connectionSerialNumber;
    this.originatorVendorId = originatorVendorId;
    this.originatorSerialNumber = originatorSerialNumber;
  }

  public int getConnectionSerialNumber() {
    return connectionSerialNumber;
  }

  public int getOriginatorVendorId() {
    return originatorVendorId;
  }

  public long getOriginatorSerialNumber() {
    return originatorSerialNumber;
  }

  public static ForwardCloseResponse decode(ByteBuf buffer) {
    int connectionSerialNumber = buffer.readUnsignedShort();
    int originatorVendorId = buffer.readUnsignedShort();
    long originatorSerialNumber = buffer.readUnsignedInt();

    return new ForwardCloseResponse(
        connectionSerialNumber, originatorVendorId, originatorSerialNumber);
  }
}

```

---

### `ethernet-ip/cip-core/src/main/java/com/digitalpetri/enip/cip/structs/LargeForwardOpenResponse.java`

```java
package com.digitalpetri.enip.cip.structs;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class LargeForwardOpenResponse {

  private final int o2tConnectionId;
  private final int t2oConnectionId;
  private final int connectionSerialNumber;
  private final int originatorVendorId;
  private final long originatorSerialNumber;
  private final Duration o2tApi;
  private final Duration t2oApi;
  private final int applicationReplySize;
  private final ByteBuf applicationReply;

  public LargeForwardOpenResponse(
      int o2tConnectionId,
      int t2oConnectionId,
      int connectionSerialNumber,
      int originatorVendorId,
      long originatorSerialNumber,
      Duration o2tApi,
      Duration t2oApi,
      int applicationReplySize,
      ByteBuf applicationReply) {

    this.o2tConnectionId = o2tConnectionId;
    this.t2oConnectionId = t2oConnectionId;
    this.connectionSerialNumber = connectionSerialNumber;
    this.originatorVendorId = originatorVendorId;
    this.originatorSerialNumber = originatorSerialNumber;
    this.o2tApi = o2tApi;
    this.t2oApi = t2oApi;
    this.applicationReplySize = applicationReplySize;
    this.applicationReply = applicationReply;
  }

  public int getO2tConnectionId() {
    return o2tConnectionId;
  }

  public int getT2oConnectionId() {
    return t2oConnectionId;
  }

  public int getConnectionSerialNumber() {
    return connectionSerialNumber;
  }

  public int getOriginatorVendorId() {
    return originatorVendorId;
  }

  public long getOriginatorSerialNumber() {
    return originatorSerialNumber;
  }

  public Duration getO2tApi() {
    return o2tApi;
  }

  public Duration getT2oApi() {
    return t2oApi;
  }

  public int getApplicationReplySize() {
    return applicationReplySize;
  }

  public ByteBuf getApplicationReply() {
    return applicationReply;
  }

  public static LargeForwardOpenResponse decode(ByteBuf buffer) {
    int o2tConnectionId = buffer.readInt();
    int t2oConnectionId = buffer.readInt();
    int connectionSerialNumber = buffer.readUnsignedShort();
    int originatorVendorId = buffer.readUnsignedShort();
    long originatorSerialNumber = buffer.readUnsignedInt();
    long o2tActualPacketInterval =
        TimeUnit.MICROSECONDS.convert(buffer.readUnsignedInt(), TimeUnit.MILLISECONDS);
    long t2oActualPacketInterval =
        TimeUnit.MICROSECONDS.convert(buffer.readUnsignedInt(), TimeUnit.MILLISECONDS);
    int applicationReplySize = buffer.readUnsignedByte();
    buffer.skipBytes(1); // reserved

    ByteBuf applicationReply =
        applicationReplySize > 0
            ? buffer.readSlice(applicationReplySize).copy()
            : Unpooled.EMPTY_BUFFER;

    return new LargeForwardOpenResponse(
        o2tConnectionId,
        t2oConnectionId,
        connectionSerialNumber,
        originatorVendorId,
        originatorSerialNumber,
        Duration.ofMillis(o2tActualPacketInterval),
        Duration.ofMillis(t2oActualPacketInterval),
        applicationReplySize,
        applicationReply);
  }
}

```

---

### `ethernet-ip/cip-core/src/main/java/com/digitalpetri/enip/cip/structs/MessageRouterResponse.java`

```java
package com.digitalpetri.enip.cip.structs;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jspecify.annotations.NonNull;

public class MessageRouterResponse {

  private final int serviceCode;
  private final int generalStatus;
  private final int[] additionalStatus;

  private final ByteBuf data;

  public MessageRouterResponse(
      int serviceCode, int generalStatus, int[] additionalStatus, @NonNull ByteBuf data) {

    this.serviceCode = serviceCode;
    this.generalStatus = generalStatus;
    this.additionalStatus = additionalStatus;
    this.data = data;
  }

  public int getServiceCode() {
    return serviceCode;
  }

  public int getGeneralStatus() {
    return generalStatus;
  }

  public int[] getAdditionalStatus() {
    return additionalStatus;
  }

  public @NonNull ByteBuf getData() {
    return data;
  }

  public static MessageRouterResponse decode(ByteBuf buffer) {
    int serviceCode = buffer.readUnsignedByte();
    buffer.skipBytes(1); // reserved
    int generalStatus = buffer.readUnsignedByte();

    int count = buffer.readUnsignedByte();
    int[] additionalStatus = new int[count];
    for (int i = 0; i < count; i++) {
      additionalStatus[i] = buffer.readShort();
    }

    ByteBuf data =
        buffer.isReadable()
            ? buffer.readSlice(buffer.readableBytes()).retain()
            : Unpooled.EMPTY_BUFFER;

    return new MessageRouterResponse(serviceCode, generalStatus, additionalStatus, data);
  }
}

```

---

### `ethernet-ip/cip-core/src/main/java/com/digitalpetri/enip/cip/structs/ForwardOpenResponse.java`

```java
package com.digitalpetri.enip.cip.structs;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public final class ForwardOpenResponse {

  private final int o2tConnectionId;
  private final int t2oConnectionId;
  private final int connectionSerialNumber;
  private final int originatorVendorId;
  private final long originatorSerialNumber;
  private final Duration o2tApi;
  private final Duration t2oApi;
  private final int applicationReplySize;
  private final ByteBuf applicationReply;

  public ForwardOpenResponse(
      int o2tConnectionId,
      int t2oConnectionId,
      int connectionSerialNumber,
      int originatorVendorId,
      long originatorSerialNumber,
      Duration o2tApi,
      Duration t2oApi,
      int applicationReplySize,
      ByteBuf applicationReply) {

    this.o2tConnectionId = o2tConnectionId;
    this.t2oConnectionId = t2oConnectionId;
    this.connectionSerialNumber = connectionSerialNumber;
    this.originatorVendorId = originatorVendorId;
    this.originatorSerialNumber = originatorSerialNumber;
    this.o2tApi = o2tApi;
    this.t2oApi = t2oApi;
    this.applicationReplySize = applicationReplySize;
    this.applicationReply = applicationReply;
  }

  public int getO2tConnectionId() {
    return o2tConnectionId;
  }

  public int getT2oConnectionId() {
    return t2oConnectionId;
  }

  public int getConnectionSerialNumber() {
    return connectionSerialNumber;
  }

  public int getOriginatorVendorId() {
    return originatorVendorId;
  }

  public long getOriginatorSerialNumber() {
    return originatorSerialNumber;
  }

  public Duration getO2tApi() {
    return o2tApi;
  }

  public Duration getT2oApi() {
    return t2oApi;
  }

  public int getApplicationReplySize() {
    return applicationReplySize;
  }

  public ByteBuf getApplicationReply() {
    return applicationReply;
  }

  public static ForwardOpenResponse decode(ByteBuf buffer) {
    int o2tConnectionId = buffer.readInt();
    int t2oConnectionId = buffer.readInt();
    int connectionSerialNumber = buffer.readUnsignedShort();
    int originatorVendorId = buffer.readUnsignedShort();
    long originatorSerialNumber = buffer.readUnsignedInt();
    long o2tActualPacketInterval =
        TimeUnit.MICROSECONDS.convert(buffer.readUnsignedInt(), TimeUnit.MILLISECONDS);
    long t2oActualPacketInterval =
        TimeUnit.MICROSECONDS.convert(buffer.readUnsignedInt(), TimeUnit.MILLISECONDS);
    int applicationReplySize = buffer.readUnsignedByte();
    buffer.skipBytes(1); // reserved

    ByteBuf applicationReply =
        applicationReplySize > 0
            ? buffer.readSlice(applicationReplySize).copy()
            : Unpooled.EMPTY_BUFFER;

    return new ForwardOpenResponse(
        o2tConnectionId,
        t2oConnectionId,
        connectionSerialNumber,
        originatorVendorId,
        originatorSerialNumber,
        Duration.ofMillis(o2tActualPacketInterval),
        Duration.ofMillis(t2oActualPacketInterval),
        applicationReplySize,
        applicationReply);
  }
}

```

---

### `ethernet-ip/cip-core/src/main/java/com/digitalpetri/enip/cip/structs/ElectronicKey.java`

```java
package com.digitalpetri.enip.cip.structs;

import io.netty.buffer.ByteBuf;

public final class ElectronicKey {

  private final int vendorId;
  private final int deviceType;
  private final int productCode;
  private final boolean compatibilitySet;
  private final short majorRevision;
  private final short minorRevision;

  public ElectronicKey(
      int vendorId,
      int deviceType,
      int productCode,
      boolean compatibilitySet,
      short majorRevision,
      short minorRevision) {

    this.vendorId = vendorId;
    this.deviceType = deviceType;
    this.productCode = productCode;
    this.compatibilitySet = compatibilitySet;
    this.majorRevision = majorRevision;
    this.minorRevision = minorRevision;
  }

  public int getVendorId() {
    return vendorId;
  }

  public int getDeviceType() {
    return deviceType;
  }

  public int getProductCode() {
    return productCode;
  }

  public boolean isCompatibilitySet() {
    return compatibilitySet;
  }

  public short getMajorRevision() {
    return majorRevision;
  }

  public short getMinorRevision() {
    return minorRevision;
  }

  public static ByteBuf encode(ElectronicKey key, ByteBuf buffer) {
    buffer.writeShort(key.getVendorId());
    buffer.writeShort(key.getDeviceType());
    buffer.writeShort(key.getProductCode());

    int majorRevisionAndCompatibility = key.isCompatibilitySet() ? 0x80 : 0x00;
    majorRevisionAndCompatibility |= (key.getMajorRevision() & 0x7F);

    buffer.writeByte(majorRevisionAndCompatibility);
    buffer.writeByte(key.getMinorRevision());

    return buffer;
  }
}

```

---

### `ethernet-ip/cip-core/src/main/java/com/digitalpetri/enip/cip/CipDataType.java`

```java
package com.digitalpetri.enip.cip;

import java.util.Optional;

/** CIP elementary data types and their identification codes, as defined by Volume 1, Appendix C. */
public enum CipDataType {

  /** Logical Boolean with values TRUE and FALSE. */
  BOOL(0xC1),

  /** Signed 8–bit integer value. */
  SINT(0xC2),

  /** Signed 16–bit integer value. */
  INT(0xC3),

  /** Signed 32–bit integer value. */
  DINT(0xC4),

  /** Signed 64–bit integer value. */
  LINT(0xC5),

  /** Unsigned 8–bit integer value. */
  USINT(0xC6),

  /** Unsigned 16–bit integer value. */
  UINT(0xC7),

  /** Unsigned 32–bit integer value. */
  UDINT(0xC8),

  /** Unsigned 64–bit integer value. */
  ULINT(0xC9),

  /** 32-bit floating point value. */
  REAL(0xCA),

  /** 64-bit floating point value. */
  LREAL(0xCB),

  /** Synchronous time information. */
  STIME(0xCC),

  /** Date information. */
  DATE(0xCD),

  /** Time of day. */
  TIME_OF_DAY(0xCE),

  /** Date and time of day. */
  DATE_AND_TIME(0xCF),

  /** Character string (1 byte per character). */
  STRING(0xD0),

  /** Bit string, 8-bit. */
  BYTE(0xD1),

  /** Bit string, 16-bit. */
  WORD(0xD2),

  /** Bit string, 32-bit. */
  DWORD(0xD3),

  /** Bit string, 64-bit. */
  LWORD(0xD4),

  /** Character string (2 bytes per character). */
  STRING2(0xD5),

  /** Duration (high resolution). */
  FTIME(0xD6),

  /** Duration (long). */
  LTIME(0xD7),

  /** Duration (short). */
  ITIME(0xD8),

  /** Character string (N bytes per character). */
  STRINGN(0xD9),

  /** Character string (1 byte per character, 1 byte length indicator). */
  SHORT_STRING(0xDA),

  /** Duration (milliseconds). */
  TIME(0xDB),

  /** CIP path segments. */
  EPATH(0xDC),

  /** Engineering Units. */
  ENGUNIT(0xDD),

  /** International Character String. */
  STRINGI(0xDE);

  private final int code;

  CipDataType(int code) {
    this.code = code;
  }

  public final int getCode() {
    return code;
  }

  /**
   * Look up the elementary {@link CipDataType} for a given identification code.
   *
   * @param code the code to look up.
   * @return an {@link Optional} containing the {@link CipDataType}, if one exists.
   */
  public static Optional<CipDataType> fromCode(int code) {
    for (CipDataType dataType : values()) {
      if (dataType.getCode() == code) {
        return Optional.of(dataType);
      }
    }
    return Optional.empty();
  }
}

```

---

### `ethernet-ip/cip-core/src/main/java/com/digitalpetri/enip/util/TimeoutCalculator.java`

```java
package com.digitalpetri.enip.util;

import java.time.Duration;

public class TimeoutCalculator {

  private static final int MIN_TIMEOUT = 1;
  private static final int MAX_TIMEOUT = 8355840;

  public static int calculateTimeoutBytes(Duration timeout) {
    int desiredTimeout = (int) timeout.toMillis();

    if (desiredTimeout < MIN_TIMEOUT) desiredTimeout = MIN_TIMEOUT;
    if (desiredTimeout > MAX_TIMEOUT) desiredTimeout = MAX_TIMEOUT;

    boolean precisionLost = false;
    int shifts = 0;
    int multiplier = desiredTimeout;

    while (multiplier > 255) {
      precisionLost |= (multiplier & 1) == 1;
      multiplier >>= 1;
      shifts += 1;
    }

    if (precisionLost) {
      multiplier += 1;
      if (multiplier > 255) {
        multiplier >>= 1;
        shifts += 1;
      }
    }

    assert (shifts <= 15);

    int tick = (int) Math.pow(2, shifts);

    assert (tick >= 1 && tick <= 32768);
    assert (multiplier >= 1 && multiplier <= 255);

    return shifts << 8 | multiplier;
  }
}

```

---

### `ethernet-ip/enip-client/src/main/java/com/digitalpetri/enip/EtherNetIpClient.java`

```java
package com.digitalpetri.enip;

import com.digitalpetri.enip.codec.EnipCodec;
import com.digitalpetri.enip.commands.*;
import com.digitalpetri.enip.cpf.ConnectedDataItemResponse;
import com.digitalpetri.enip.cpf.CpfPacket;
import com.digitalpetri.enip.cpf.UnconnectedDataItemResponse;
import com.digitalpetri.enip.util.IntUtil;
import com.digitalpetri.fsm.FsmContext;
import com.digitalpetri.netty.fsm.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static com.digitalpetri.enip.util.FutureUtils.complete;

public class EtherNetIpClient {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final ExecutorService executor;

  private final Map<Long, PendingRequest<? extends Command>> pendingRequests =
      new ConcurrentHashMap<>();
  private final AtomicLong senderContext = new AtomicLong(0L);

  private volatile long sessionHandle;

  private final List<ChannelStateListener> channelStateListeners = new CopyOnWriteArrayList<>();

  private final ChannelFsm channelFsm;
  private final EtherNetIpClientConfig config;

  public EtherNetIpClient(EtherNetIpClientConfig config) {
    this.config = config;

    executor = config.getExecutor();

    ChannelFsmConfig fsmConfig =
        ChannelFsmConfig.newBuilder()
            .setLazy(config.isLazy())
            .setPersistent(config.isPersistent())
            .setMaxIdleSeconds(IntUtil.saturatedCast(config.getMaxIdle().getSeconds()))
            .setMaxReconnectDelaySeconds(config.getMaxReconnectDelaySeconds())
            .setChannelActions(new EnipChannelActions())
            .setExecutor(config.getExecutor())
            .setScheduler(config.getScheduledExecutor())
            .setLoggerName("com.digitalpetri.enip.ChannelFsm")
            .setLoggingContext(config.getLoggingContext())
            .build();

    channelFsm = ChannelFsmFactory.newChannelFsm(fsmConfig);

    channelFsm.addTransitionListener(
        (from, to, via) -> channelStateListeners.forEach(l -> l.onChannelStateChanged(from, to)));
  }

  public CompletableFuture<EtherNetIpClient> connect() {
    return complete(new CompletableFuture<EtherNetIpClient>())
        .with(channelFsm.connect().thenApply(c -> EtherNetIpClient.this));
  }

  public CompletableFuture<EtherNetIpClient> disconnect() {
    return complete(new CompletableFuture<EtherNetIpClient>())
        .with(channelFsm.disconnect().thenApply(c -> EtherNetIpClient.this));
  }

  public String getState() {
    return channelFsm.getState().toString();
  }

  /**
   * Add a {@link ChannelStateListener} that will get notified when the underlying channel {@link
   * State} changes.
   *
   * @param listener the {@link ChannelStateListener} to add.
   */
  public void addChannelStateListener(ChannelStateListener listener) {
    channelStateListeners.add(listener);
  }

  /**
   * Remove a previously-registered {@link ChannelStateListener}.
   *
   * @param listener the {@link ChannelStateListener} to remove.
   */
  public void removeChannelStateListener(ChannelStateListener listener) {
    channelStateListeners.remove(listener);
  }

  public CompletableFuture<ListIdentity> listIdentity() {
    return sendCommand(new ListIdentity());
  }

  public CompletableFuture<SendRRData> sendRRData(SendRRData command) {
    return sendCommand(command);
  }

  public CompletableFuture<Void> sendUnitData(SendUnitData command) {
    CompletableFuture<Void> future = new CompletableFuture<>();

    channelFsm
        .getChannel(config.getWaitForReconnect())
        .whenComplete(
            (ch, ex) -> {
              if (ch != null) {
                EnipPacket packet =
                    new EnipPacket(
                        command.getCommandCode(),
                        sessionHandle,
                        EnipStatus.EIP_SUCCESS,
                        0L,
                        command);

                ch.writeAndFlush(packet)
                    .addListener(
                        f -> {
                          if (f.isSuccess()) future.complete(null);
                          else future.completeExceptionally(f.cause());
                        });
              } else {
                future.completeExceptionally(ex);
              }
            });

    return future;
  }

  public EtherNetIpClientConfig getConfig() {
    return config;
  }

  public ExecutorService getExecutor() {
    return executor;
  }

  public <T extends Command> CompletableFuture<T> sendCommand(Command command) {
    CompletableFuture<T> future = new CompletableFuture<>();

    channelFsm
        .getChannel(config.getWaitForReconnect())
        .whenComplete(
            (ch, ex) -> {
              if (ch != null) writeCommand(ch, command, future);
              else future.completeExceptionally(ex);
            });

    return future;
  }

  public <T extends Command> void writeCommand(
      Channel channel, Command command, CompletableFuture<T> future) {

    EnipPacket packet =
        new EnipPacket(
            command.getCommandCode(),
            sessionHandle,
            EnipStatus.EIP_SUCCESS,
            senderContext.getAndIncrement(),
            command);

    Timeout timeout =
        config
            .getWheelTimer()
            .newTimeout(
                tt -> {
                  if (tt.isCancelled()) return;
                  PendingRequest<?> p = pendingRequests.remove(packet.getSenderContext());
                  if (p != null) {
                    String message =
                        String.format(
                            "senderContext=%s timed out waiting %sms for response",
                            packet.getSenderContext(), config.getTimeout().toMillis());
                    p.promise.completeExceptionally(new TimeoutException(message));
                  }
                },
                config.getTimeout().toMillis(),
                TimeUnit.MILLISECONDS);

    pendingRequests.put(packet.getSenderContext(), new PendingRequest<>(future, timeout));

    channel
        .writeAndFlush(packet)
        .addListener(
            f -> {
              if (!f.isSuccess()) {
                PendingRequest pending = pendingRequests.remove(packet.getSenderContext());
                if (pending != null) {
                  pending.timeout.cancel();
                  pending.promise.completeExceptionally(f.cause());
                }
              }
            });
  }

  private void onChannelRead(EnipPacket packet) {
    CommandCode commandCode = packet.getCommandCode();
    EnipStatus status = packet.getStatus();

    if (commandCode == CommandCode.SendUnitData) {
      if (status == EnipStatus.EIP_SUCCESS) {
        onUnitDataReceived((SendUnitData) packet.getCommand());
      } else {
        config.getLoggingContext().forEach(MDC::put);
        try {
          logger.warn("Received SendUnitData command with status: {}", status);
        } finally {
          config.getLoggingContext().keySet().forEach(MDC::remove);
        }
      }
    } else {
      if (commandCode == CommandCode.RegisterSession) {
        if (status == EnipStatus.EIP_SUCCESS) {
          sessionHandle = packet.getSessionHandle();
        } else {
          sessionHandle = 0L;
        }
      }

      PendingRequest<?> pending = pendingRequests.remove(packet.getSenderContext());

      if (pending != null) {
        pending.timeout.cancel();

        if (status == EnipStatus.EIP_SUCCESS) {
          pending.promise.complete(packet.getCommand());
        } else {
          pending.promise.completeExceptionally(new Exception("EtherNet/IP status: " + status));
        }
      } else {
        config.getLoggingContext().forEach(MDC::put);
        try {
          logger.debug("Received response for unknown context: {}", packet.getSenderContext());
        } finally {
          config.getLoggingContext().keySet().forEach(MDC::remove);
        }

        if (packet.getCommand() instanceof SendRRData) {
          CpfPacket cpfPacket = ((SendRRData) packet.getCommand()).getPacket();

          Arrays.stream(cpfPacket.getItems())
              .forEach(
                  item -> {
                    if (item instanceof ConnectedDataItemResponse) {
                      ReferenceCountUtil.safeRelease(((ConnectedDataItemResponse) item).getData());
                    } else if (item instanceof UnconnectedDataItemResponse) {
                      ReferenceCountUtil.safeRelease(
                          ((UnconnectedDataItemResponse) item).getData());
                    }
                  });
        }
      }
    }
  }

  private void onChannelInactive(ChannelHandlerContext ctx) {
    config.getLoggingContext().forEach(MDC::put);
    try {
      logger.debug(
          "onChannelInactive() {} <-> {}",
          ctx.channel().localAddress(),
          ctx.channel().remoteAddress());
    } finally {
      config.getLoggingContext().keySet().forEach(MDC::remove);
    }
  }

  private void onExceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    config.getLoggingContext().forEach(MDC::put);
    try {
      logger.debug(
          "onExceptionCaught() {} <-> {}",
          ctx.channel().localAddress(),
          ctx.channel().remoteAddress(),
          cause);
    } finally {
      config.getLoggingContext().keySet().forEach(MDC::remove);
    }

    ctx.channel().close();
  }

  /**
   * Subclasses can override this to handle incoming {@link
   * com.digitalpetri.enip.commands.SendUnitData} commands.
   *
   * @param command the {@link com.digitalpetri.enip.commands.SendUnitData} command received.
   */
  protected void onUnitDataReceived(SendUnitData command) {}

  private final class EnipChannelActions implements ChannelActions {

    @Override
    public CompletableFuture<Channel> connect(FsmContext<State, Event> ctx) {
      return bootstrap(EtherNetIpClient.this)
          .thenCompose(
              channel -> {
                CompletableFuture<RegisterSession> future = new CompletableFuture<>();

                writeCommand(channel, new RegisterSession(), future);

                return future.thenApply(rs -> channel);
              });
    }

    @Override
    public CompletableFuture<Void> disconnect(FsmContext<State, Event> ctx, Channel channel) {
      CompletableFuture<Void> disconnectFuture = new CompletableFuture<>();

      // When the remote receives UnRegisterSession it's likely to just close the connection.
      channel
          .pipeline()
          .addFirst(
              new ChannelInboundHandlerAdapter() {
                @Override
                public void channelInactive(ChannelHandlerContext ctx) {
                  disconnectFuture.complete(null);
                }

                @Override
                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                  disconnectFuture.complete(null);
                }
              });

      CompletableFuture<UnRegisterSession> future = new CompletableFuture<>();
      writeCommand(channel, new UnRegisterSession(), future);

      future.whenComplete(
          (cmd, ex2) -> {
            channel.close();
            disconnectFuture.complete(null);
          });

      return disconnectFuture;
    }

    @Override
    public CompletableFuture<Void> keepAlive(FsmContext<State, Event> ctx, Channel channel) {
      return listIdentity()
          .whenComplete(
              (li, ex) -> {
                if (ex != null) {
                  config.getLoggingContext().forEach(MDC::put);
                  try {
                    logger.debug("Keep alive failed: {}", ex.getMessage(), ex);
                  } finally {
                    config.getLoggingContext().keySet().forEach(MDC::remove);
                  }
                }
              })
          .thenApply(li -> null);
    }
  }

  private static final class EtherNetIpClientHandler
      extends SimpleChannelInboundHandler<EnipPacket> {

    private final ExecutorService executor;

    private final EtherNetIpClient client;

    private EtherNetIpClientHandler(EtherNetIpClient client) {
      this.client = client;

      executor = client.getExecutor();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, EnipPacket packet) {
      executor.execute(() -> client.onChannelRead(packet));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
      client.onChannelInactive(ctx);

      super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
      client.onExceptionCaught(ctx, cause);

      super.exceptionCaught(ctx, cause);
    }
  }

  private static CompletableFuture<Channel> bootstrap(EtherNetIpClient client) {
    CompletableFuture<Channel> future = new CompletableFuture<>();
    EtherNetIpClientConfig config = client.getConfig();

    Bootstrap bootstrap = new Bootstrap();

    bootstrap
        .group(config.getEventLoop())
        .channel(NioSocketChannel.class)
        .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) config.getTimeout().toMillis())
        .option(ChannelOption.TCP_NODELAY, true)
        .handler(
            new ChannelInitializer<SocketChannel>() {
              @Override
              protected void initChannel(SocketChannel ch) {
                ch.pipeline().addLast(new EnipCodec());
                ch.pipeline().addLast(new EtherNetIpClientHandler(client));
              }
            });

    config.getBootstrapConsumer().accept(bootstrap);

    bootstrap
        .connect(config.getHostname(), config.getPort())
        .addListener(
            (ChannelFuture f) -> {
              if (f.isSuccess()) {
                future.complete(f.channel());
              } else {
                future.completeExceptionally(f.cause());
              }
            });

    return future;
  }

  private static final class PendingRequest<T> {

    private final CompletableFuture<Command> promise = new CompletableFuture<>();

    private final Timeout timeout;

    @SuppressWarnings("unchecked")
    private PendingRequest(CompletableFuture<T> future, Timeout timeout) {
      this.timeout = timeout;

      promise.whenComplete(
          (r, ex) -> {
            if (r != null) {
              try {
                future.complete((T) r);
              } catch (ClassCastException e) {
                future.completeExceptionally(e);
              }
            } else {
              future.completeExceptionally(ex);
            }
          });
    }
  }

  public interface ChannelStateListener {

    /**
     * The underlying channel state has changed from {@code previous} to {@code current}.
     *
     * @param previous the previous channel {@link State}.
     * @param current the current channel {@link State}.
     */
    void onChannelStateChanged(State previous, State current);
  }
}

```

---

### `ethernet-ip/enip-client/src/main/java/com/digitalpetri/enip/EtherNetIpClientConfig.java`

```java
package com.digitalpetri.enip;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.util.HashedWheelTimer;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

public class EtherNetIpClientConfig {

  private final String hostname;
  private final int port;
  private final int vendorId;
  private final int serialNumber;
  private final Duration timeout;
  private final Duration maxIdle;
  private final int maxReconnectDelaySeconds;
  private final boolean lazy;
  private final boolean persistent;
  private final boolean waitForReconnect;
  private final ExecutorService executor;
  private final ScheduledExecutorService scheduledExecutor;
  private final EventLoopGroup eventLoop;
  private final HashedWheelTimer wheelTimer;
  private final Consumer<Bootstrap> bootstrapConsumer;
  private final Map<String, String> loggingContext;

  public EtherNetIpClientConfig(
      String hostname,
      int port,
      int vendorId,
      int serialNumber,
      Duration timeout,
      Duration maxIdle,
      int maxReconnectDelaySeconds,
      boolean lazy,
      boolean persistent,
      boolean waitForReconnect,
      ExecutorService executor,
      ScheduledExecutorService scheduledExecutor,
      EventLoopGroup eventLoop,
      HashedWheelTimer wheelTimer,
      Consumer<Bootstrap> bootstrapConsumer,
      Map<String, String> loggingContext) {

    this.hostname = hostname;
    this.port = port;
    this.vendorId = vendorId;
    this.serialNumber = serialNumber;
    this.timeout = timeout;
    this.maxIdle = maxIdle;
    this.maxReconnectDelaySeconds = maxReconnectDelaySeconds;
    this.lazy = lazy;
    this.persistent = persistent;
    this.waitForReconnect = waitForReconnect;
    this.executor = executor;
    this.scheduledExecutor = scheduledExecutor;
    this.eventLoop = eventLoop;
    this.wheelTimer = wheelTimer;
    this.bootstrapConsumer = bootstrapConsumer;
    this.loggingContext = loggingContext;
  }

  public String getHostname() {
    return hostname;
  }

  public int getPort() {
    return port;
  }

  public int getVendorId() {
    return vendorId;
  }

  public int getSerialNumber() {
    return serialNumber;
  }

  public Duration getTimeout() {
    return timeout;
  }

  /**
   * @return the max amount of time that can elapse without reading any data from the remote before
   *     a keep alive ListIdentity request is sent. If this ListIdentity request fails for any
   *     reason the channel is closed.
   */
  public Duration getMaxIdle() {
    return maxIdle;
  }

  /**
   * Get the maximum amount of time to delay between reconnect attempts, in seconds.
   *
   * <p>Delays between reconnect attempts are exponentially backed off starting from 1 until {@code
   * maxReconnectDelay} is reached.
   *
   * <p>Must be a power of 2 or else it will be rounded up to the nearest.
   *
   * @return the maximum amount of time to delay, in seconds, between reconnect attempts.
   */
  public int getMaxReconnectDelaySeconds() {
    return maxReconnectDelaySeconds;
  }

  /**
   * @return {@code true} if the channel state machine is lazy in its reconnection attempts, i.e.
   *     after a break in the connection occurs it moves to the Idle state, reconnecting on demand
   *     the next time the connect() or getChannel() is called. If {@code false} the state machine
   *     eagerly attempts to reconnect and move back into Connected state.
   */
  public boolean isLazy() {
    return lazy;
  }

  /**
   * @return {@code true} if the channel state machine is persistent in its connection attempts,
   *     i.e. after a single call to connect() it strives to stay in a Connected state (respecting
   *     laziness) regardless of the result of the initial connect(). If {@code false}, the state
   *     machine won't attempt to remain connected until it has successfully moved into the
   *     Connected state.
   */
  public boolean isPersistent() {
    return persistent;
  }

  /**
   * @return {@code true} if, when the channel state machine is between reconnect attempts, channel
   *     operations wait for the result of the subsequent reconnect attempt, or {@code false} if
   *     they fail immediately.
   */
  public boolean getWaitForReconnect() {
    return waitForReconnect;
  }

  public ExecutorService getExecutor() {
    return executor;
  }

  public ScheduledExecutorService getScheduledExecutor() {
    return scheduledExecutor;
  }

  public EventLoopGroup getEventLoop() {
    return eventLoop;
  }

  public HashedWheelTimer getWheelTimer() {
    return wheelTimer;
  }

  public Consumer<Bootstrap> getBootstrapConsumer() {
    return bootstrapConsumer;
  }

  /**
   * Get the logging context Map.
   *
   * <p>Keys and values in the Map will be set on the SLF4J {@link org.slf4j.MDC} when logging.
   *
   * @return the logging context Map an {@link EtherNetIpClient} instance will use when logging.
   */
  public Map<String, String> getLoggingContext() {
    return loggingContext;
  }

  public static Builder builder(String hostname) {
    return new Builder().setHostname(hostname);
  }

  public static class Builder {

    private String hostname;
    private int port = 44818;
    private int vendorId = 0;
    private int serialNumber = 0;
    private Duration timeout = Duration.ofSeconds(5);
    private Duration maxIdle = Duration.ofSeconds(15);
    private int maxReconnectDelaySeconds = 16;
    private boolean lazy = true;
    private boolean persistent = true;
    private boolean waitForReconnect = true;
    private ExecutorService executor;
    private ScheduledExecutorService scheduledExecutor;
    private EventLoopGroup eventLoop;
    private HashedWheelTimer wheelTimer;
    private Consumer<Bootstrap> bootstrapConsumer = (b) -> {};
    private Map<String, String> loggingContext = new ConcurrentHashMap<>();

    public Builder setHostname(String hostname) {
      this.hostname = hostname;
      return this;
    }

    public Builder setPort(int port) {
      this.port = port;
      return this;
    }

    public Builder setVendorId(int vendorId) {
      this.vendorId = vendorId;
      return this;
    }

    public Builder setSerialNumber(int serialNumber) {
      this.serialNumber = serialNumber;
      return this;
    }

    public Builder setTimeout(Duration timeout) {
      this.timeout = timeout;
      return this;
    }

    /**
     * @see EtherNetIpClientConfig#getMaxIdle()
     */
    public Builder setMaxIdle(Duration maxIdle) {
      this.maxIdle = maxIdle;
      return this;
    }

    /**
     * @see EtherNetIpClientConfig#getMaxReconnectDelaySeconds()
     */
    public Builder setMaxReconnectDelaySeconds(int maxReconnectDelaySeconds) {
      this.maxReconnectDelaySeconds = maxReconnectDelaySeconds;
      return this;
    }

    /**
     * @see EtherNetIpClientConfig#isLazy()
     */
    public Builder setLazy(boolean lazy) {
      this.lazy = lazy;
      return this;
    }

    /**
     * @see EtherNetIpClientConfig#isPersistent()
     */
    public Builder setPersistent(boolean persistent) {
      this.persistent = persistent;
      return this;
    }

    /**
     * @see EtherNetIpClientConfig#getWaitForReconnect()
     */
    public Builder setWaitForReconnect(boolean waitForReconnect) {
      this.waitForReconnect = waitForReconnect;
      return this;
    }

    public Builder setExecutor(ExecutorService executor) {
      this.executor = executor;
      return this;
    }

    public Builder setScheduledExecutor(ScheduledExecutorService scheduledExecutor) {
      this.scheduledExecutor = scheduledExecutor;
      return this;
    }

    public Builder setEventLoop(EventLoopGroup eventLoop) {
      this.eventLoop = eventLoop;
      return this;
    }

    public Builder setWheelTimer(HashedWheelTimer wheelTimer) {
      this.wheelTimer = wheelTimer;
      return this;
    }

    public Builder setBootstrapConsumer(Consumer<Bootstrap> bootstrapConsumer) {
      this.bootstrapConsumer = bootstrapConsumer;
      return this;
    }

    /**
     * Set the logging context Map an {@link EtherNetIpClient} instance will use.
     *
     * <p>Keys and values in the Map will be set on the SLF4J {@link org.slf4j.MDC} when logging.
     *
     * <p>This method makes a defensive copy of {@code loggingContext}.
     *
     * @param loggingContext the logging context for this {@link EtherNetIpClient} instance.
     * @return this {@link Builder} instance.
     */
    public Builder setLoggingContext(Map<String, String> loggingContext) {
      this.loggingContext = new ConcurrentHashMap<>(loggingContext);
      return this;
    }

    public EtherNetIpClientConfig build() {
      if (executor == null) {
        executor = EtherNetIpShared.sharedExecutorService();
      }
      if (scheduledExecutor == null) {
        scheduledExecutor = EtherNetIpShared.sharedScheduledExecutor();
      }
      if (eventLoop == null) {
        eventLoop = EtherNetIpShared.sharedEventLoop();
      }
      if (wheelTimer == null) {
        wheelTimer = EtherNetIpShared.sharedWheelTimer();
      }

      return new EtherNetIpClientConfig(
          hostname,
          port,
          vendorId,
          serialNumber,
          timeout,
          maxIdle,
          maxReconnectDelaySeconds,
          lazy,
          persistent,
          waitForReconnect,
          executor,
          scheduledExecutor,
          eventLoop,
          wheelTimer,
          bootstrapConsumer,
          loggingContext);
    }
  }
}

```

---

### `ethernet-ip/enip-client/src/main/java/com/digitalpetri/enip/util/FutureUtils.java`

```java
package com.digitalpetri.enip.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class FutureUtils {

  /**
   * Complete {@code future} with the result of the {@link CompletableFuture} that is provided to
   * the returned {@link CompletionBuilder}.
   *
   * @param future the future to complete.
   * @return a {@link CompletionBuilder}.
   */
  public static <T> CompletionBuilder<T> complete(CompletableFuture<T> future) {
    return new CompletionBuilder<>(future);
  }

  public static class CompletionBuilder<T> {

    final CompletableFuture<T> toComplete;

    private CompletionBuilder(CompletableFuture<T> toComplete) {
      this.toComplete = toComplete;
    }

    public CompletionBuilder<T> async(Executor executor) {
      return new AsyncCompletionBuilder<>(toComplete, executor);
    }

    /**
     * Complete the contained to-be-completed {@link CompletableFuture} using the result of {@code
     * future}.
     *
     * @param future the {@link CompletableFuture} to use as the result for the contained future.
     * @return the original, to-be-completed future provided to this {@link CompletionBuilder}.
     */
    public CompletableFuture<T> with(CompletableFuture<T> future) {
      future.whenComplete(
          (v, ex) -> {
            if (ex != null) toComplete.completeExceptionally(ex);
            else toComplete.complete(v);
          });

      return toComplete;
    }
  }

  private static final class AsyncCompletionBuilder<T> extends CompletionBuilder<T> {

    private final Executor executor;

    AsyncCompletionBuilder(CompletableFuture<T> toComplete, Executor executor) {
      super(toComplete);

      this.executor = executor;
    }

    @Override
    public CompletableFuture<T> with(CompletableFuture<T> future) {
      future.whenCompleteAsync(
          (v, ex) -> {
            if (ex != null) toComplete.completeExceptionally(ex);
            else toComplete.complete(v);
          },
          executor);

      return toComplete;
    }
  }
}

```

---

### `ethernet-ip/enip-client/src/main/java/com/digitalpetri/enip/EtherNetIpShared.java`

```java
package com.digitalpetri.enip;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.HashedWheelTimer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class EtherNetIpShared {

  private static EventLoopGroup SHARED_EVENT_LOOP;
  private static HashedWheelTimer SHARED_WHEEL_TIMER;
  private static ExecutorService SHARED_EXECUTOR;
  private static ScheduledExecutorService SHARED_SCHEDULED_EXECUTOR;

  /**
   * @return a shared {@link io.netty.channel.EventLoopGroup}.
   */
  public static synchronized EventLoopGroup sharedEventLoop() {
    if (SHARED_EVENT_LOOP == null) {
      SHARED_EVENT_LOOP = new NioEventLoopGroup();
    }
    return SHARED_EVENT_LOOP;
  }

  /**
   * @return a shared {@link io.netty.util.HashedWheelTimer}.
   */
  public static synchronized HashedWheelTimer sharedWheelTimer() {
    if (SHARED_WHEEL_TIMER == null) {
      SHARED_WHEEL_TIMER = new HashedWheelTimer();
    }
    return SHARED_WHEEL_TIMER;
  }

  /**
   * @return a shared {@link java.util.concurrent.ExecutorService}.
   */
  public static synchronized ExecutorService sharedExecutorService() {
    if (SHARED_EXECUTOR == null) {
      SHARED_EXECUTOR = Executors.newWorkStealingPool();
    }
    return SHARED_EXECUTOR;
  }

  /**
   * @return a shared {@link ScheduledExecutorService}.
   */
  public static synchronized ScheduledExecutorService sharedScheduledExecutor() {
    if (SHARED_SCHEDULED_EXECUTOR == null) {
      SHARED_SCHEDULED_EXECUTOR = Executors.newSingleThreadScheduledExecutor();
    }
    return SHARED_SCHEDULED_EXECUTOR;
  }

  /** Release/shutdown/cleanup any shared resources that were created. */
  public static synchronized void releaseSharedResources() {
    if (SHARED_EVENT_LOOP != null) {
      SHARED_EVENT_LOOP.shutdownGracefully();
      SHARED_EVENT_LOOP = null;
    }
    if (SHARED_WHEEL_TIMER != null) {
      SHARED_WHEEL_TIMER.stop();
      SHARED_WHEEL_TIMER = null;
    }
    if (SHARED_EXECUTOR != null) {
      SHARED_EXECUTOR.shutdown();
      SHARED_EXECUTOR = null;
    }
    if (SHARED_SCHEDULED_EXECUTOR != null) {
      SHARED_SCHEDULED_EXECUTOR.shutdown();
      SHARED_SCHEDULED_EXECUTOR = null;
    }
  }
}

```

---

### `ethernet-ip/enip-client/src/main/java/com/digitalpetri/enip/codec/EnipCodec.java`

```java
package com.digitalpetri.enip.codec;

import com.digitalpetri.enip.EnipPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;

import java.nio.ByteOrder;
import java.util.List;

public class EnipCodec extends ByteToMessageCodec<EnipPacket> {

  private static final int HEADER_SIZE = 24;
  private static final int LENGTH_OFFSET = 2;

  @Override
  protected void encode(ChannelHandlerContext ctx, EnipPacket packet, ByteBuf out)
      throws Exception {
    EnipPacket.encode(packet, out.order(ByteOrder.LITTLE_ENDIAN));
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    ByteBuf buffer = in.order(ByteOrder.LITTLE_ENDIAN);

    int startIndex = buffer.readerIndex();

    while (buffer.readableBytes() >= HEADER_SIZE
        && buffer.readableBytes() >= HEADER_SIZE + getLength(buffer, startIndex)) {

      out.add(EnipPacket.decode(buffer));

      startIndex = buffer.readerIndex();
    }
  }

  private int getLength(ByteBuf buffer, int startIndex) {
    return buffer.getUnsignedShort(startIndex + LENGTH_OFFSET);
  }
}

```

---

### `modbus/serial/src/main/java/com/digitalpetri/modbus/serial/server/SerialPortServerTransport.java`

```java
package com.digitalpetri.modbus.serial.server;

import com.digitalpetri.modbus.ModbusRtuFrame;
import com.digitalpetri.modbus.ModbusRtuRequestFrameParser;
import com.digitalpetri.modbus.ModbusRtuRequestFrameParser.Accumulated;
import com.digitalpetri.modbus.ModbusRtuRequestFrameParser.ParserState;
import com.digitalpetri.modbus.exceptions.UnknownUnitIdException;
import com.digitalpetri.modbus.internal.util.ExecutionQueue;
import com.digitalpetri.modbus.serial.SerialPortTransportConfig;
import com.digitalpetri.modbus.serial.SerialPortTransportConfig.Builder;
import com.digitalpetri.modbus.server.ModbusRequestContext.ModbusRtuRequestContext;
import com.digitalpetri.modbus.server.ModbusRtuServerTransport;
import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Modbus RTU/Serial server transport; a {@link ModbusRtuServerTransport} that sends and receives
 * {@link ModbusRtuFrame}s over a serial port.
 */
public class SerialPortServerTransport implements ModbusRtuServerTransport {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final ModbusRtuRequestFrameParser frameParser = new ModbusRtuRequestFrameParser();
  private final AtomicReference<FrameReceiver<ModbusRtuRequestContext, ModbusRtuFrame>>
      frameReceiver = new AtomicReference<>();

  private final ExecutionQueue executionQueue;

  private final SerialPort serialPort;

  private final SerialPortTransportConfig config;

  public SerialPortServerTransport(SerialPortTransportConfig config) {
    this.config = config;

    serialPort = SerialPort.getCommPort(config.serialPort());

    serialPort.setComPortParameters(
        config.baudRate(),
        config.dataBits(),
        config.stopBits(),
        config.parity(),
        config.rs485Mode());

    executionQueue = new ExecutionQueue(config.executor());
  }

  @Override
  public CompletionStage<Void> bind() {
    if (serialPort.isOpen()) {
      return CompletableFuture.completedFuture(null);
    } else {
      if (serialPort.openPort()) {
        frameParser.reset();

        serialPort.addDataListener(new ModbusRtuDataListener());

        return CompletableFuture.completedFuture(null);
      } else {
        return CompletableFuture.failedFuture(
            new Exception(
                "failed to open port '%s', lastErrorCode=%d"
                    .formatted(config.serialPort(), serialPort.getLastErrorCode())));
      }
    }
  }

  @Override
  public CompletionStage<Void> unbind() {
    if (serialPort.isOpen()) {
      if (serialPort.closePort()) {
        frameParser.reset();

        return CompletableFuture.completedFuture(null);
      } else {
        return CompletableFuture.failedFuture(
            new Exception(
                "failed to close port '%s', lastErrorCode=%d"
                    .formatted(config.serialPort(), serialPort.getLastErrorCode())));
      }
    } else {
      return CompletableFuture.completedFuture(null);
    }
  }

  @Override
  public void receive(FrameReceiver<ModbusRtuRequestContext, ModbusRtuFrame> frameReceiver) {
    this.frameReceiver.set(frameReceiver);
  }

  private class ModbusRtuDataListener implements SerialPortDataListener {

    /** Bit mask indicating what events we're interested in. */
    private static final int LISTENING_EVENTS = SerialPort.LISTENING_EVENT_DATA_RECEIVED;

    @Override
    public int getListeningEvents() {
      return LISTENING_EVENTS;
    }

    @Override
    public void serialEvent(SerialPortEvent event) {
      if ((event.getEventType() & LISTENING_EVENTS) == LISTENING_EVENTS) {
        onDataReceived(event);
      }
    }

    private void onDataReceived(SerialPortEvent event) {
      byte[] receivedData = event.getReceivedData();

      ParserState state = frameParser.parse(receivedData);

      if (state instanceof Accumulated a) {
        try {
          onFrameReceived(a.frame());
        } finally {
          frameParser.reset();
        }
      }
    }

    private void onFrameReceived(ModbusRtuFrame requestFrame) {
      FrameReceiver<ModbusRtuRequestContext, ModbusRtuFrame> frameReceiver =
          SerialPortServerTransport.this.frameReceiver.get();

      if (frameReceiver != null) {
        executionQueue.submit(
            () -> {
              try {
                ModbusRtuFrame responseFrame =
                    frameReceiver.receive(new ModbusRtuRequestContext() {}, requestFrame);

                int unitId = responseFrame.unitId();
                ByteBuffer pdu = responseFrame.pdu();
                ByteBuffer crc = responseFrame.crc();

                byte[] data = new byte[1 + pdu.remaining() + crc.remaining()];
                data[0] = (byte) unitId;
                pdu.get(data, 1, pdu.remaining());
                crc.get(data, data.length - 2, crc.remaining());

                int totalWritten = 0;
                while (totalWritten < data.length) {
                  int written =
                      serialPort.writeBytes(data, data.length - totalWritten, totalWritten);
                  if (written == -1) {
                    logger.error("Error writing frame to serial port");

                    return;
                  }
                  totalWritten += written;
                }
              } catch (UnknownUnitIdException e) {
                logger.debug("Ignoring request for unknown unit id: {}", requestFrame.unitId());
              } catch (Exception e) {
                logger.error("Error handling frame: {}", e.getMessage(), e);
              }
            });
      }
    }
  }

  /**
   * Create a new {@link SerialPortServerTransport} with a callback that allows customizing the
   * configuration.
   *
   * @param configure a {@link Consumer} that accepts a {@link SerialPortTransportConfig.Builder}
   *     instance to configure.
   * @return a new {@link SerialPortServerTransport}.
   */
  public static SerialPortServerTransport create(Consumer<Builder> configure) {

    var builder = new SerialPortTransportConfig.Builder();
    configure.accept(builder);
    return new SerialPortServerTransport(builder.build());
  }
}

```

---

### `modbus/serial/src/main/java/com/digitalpetri/modbus/serial/SerialPortTransportConfig.java`

```java
package com.digitalpetri.modbus.serial;

import com.digitalpetri.modbus.Modbus;
import com.digitalpetri.modbus.serial.client.SerialPortClientTransport;
import com.fazecast.jSerialComm.SerialPort;

import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

/**
 * Configuration for a {@link SerialPortClientTransport}.
 *
 * @param serialPort the OS/system-dependent serial port descriptor.
 * @param baudRate the desired baud rate to operate at.
 * @param dataBits the number of data bits per word to use.
 * @param stopBits the number of stop bits to use.
 * @param parity the type of parity error-checking to use.
 * @param rs485Mode enable RS-485 mode, i.e. transmit/receive mode signaling using the RTS pin.
 * @param executor the {@link ExecutorService} to use when delivering frame received callbacks.
 * @see SerialPortTransportConfig#create(Consumer)
 */
public record SerialPortTransportConfig(
    String serialPort,
    int baudRate,
    int dataBits,
    int stopBits,
    int parity,
    boolean rs485Mode,
    ExecutorService executor) {

  /**
   * Create a new {@link SerialPortTransportConfig}, using the callback to configure the builder as
   * required.
   *
   * @param configure a {@link Consumer} that accepts a {@link Builder} instance to configure.
   * @return a new {@link SerialPortTransportConfig}.
   */
  public static SerialPortTransportConfig create(Consumer<Builder> configure) {
    var builder = new Builder();
    configure.accept(builder);
    return builder.build();
  }

  public static class Builder {

    /**
     * The OS/system-dependent serial port descriptor.
     *
     * <p>On Windows this may look something like {@code COM1}. On Linux this may look something
     * like {@code /dev/ttyUSB0}.
     */
    public String serialPort;

    /** The desired baud rate to operate at. */
    public int baudRate = 9600;

    /** The number of data bits per word to use. */
    public int dataBits = 8;

    /**
     * The number of stop bits to use.
     *
     * @see SerialPort#ONE_STOP_BIT
     * @see SerialPort#ONE_POINT_FIVE_STOP_BITS
     * @see SerialPort#TWO_STOP_BITS
     */
    public int stopBits = SerialPort.ONE_STOP_BIT;

    /**
     * The type of parity error-checking to use.
     *
     * @see SerialPort#NO_PARITY
     * @see SerialPort#ODD_PARITY
     * @see SerialPort#EVEN_PARITY
     * @see SerialPort#MARK_PARITY
     * @see SerialPort#SPACE_PARITY
     */
    public int parity = SerialPort.NO_PARITY;

    /**
     * Enable RS-485 mode, i.e. transmit/receive mode signaling using the RTS pin.
     *
     * <p>This requires support from the underlying driver and may not work with all RS-485 devices.
     */
    public boolean rs485Mode = false;

    /**
     * The {@link ExecutorService} to use when delivering frame received callbacks.
     *
     * <p>Defaults to {@link Modbus#sharedExecutor()} if not set explicitly.
     */
    public ExecutorService executor;

    /**
     * Set the OS/system-dependent serial port descriptor.
     *
     * @param serialPort the serial port descriptor.
     * @return this {@link Builder}.
     */
    public Builder setSerialPort(String serialPort) {
      this.serialPort = serialPort;
      return this;
    }

    /**
     * Set the desired baud rate to operate at.
     *
     * @param baudRate the baud rate.
     * @return this {@link Builder}.
     */
    public Builder setBaudRate(int baudRate) {
      this.baudRate = baudRate;
      return this;
    }

    /**
     * Set the number of data bits per word to use.
     *
     * @param dataBits the number of data bits.
     * @return this {@link Builder}.
     */
    public Builder setDataBits(int dataBits) {
      this.dataBits = dataBits;
      return this;
    }

    /**
     * Set the number of stop bits to use.
     *
     * @param stopBits the number of stop bits.
     * @return this {@link Builder}.
     * @see SerialPort#ONE_STOP_BIT
     * @see SerialPort#ONE_POINT_FIVE_STOP_BITS
     * @see SerialPort#TWO_STOP_BITS
     */
    public Builder setStopBits(int stopBits) {
      this.stopBits = stopBits;
      return this;
    }

    /**
     * Set the type of parity error-checking to use.
     *
     * @param parity the type of parity.
     * @return this {@link Builder}.
     * @see SerialPort#NO_PARITY
     * @see SerialPort#ODD_PARITY
     * @see SerialPort#EVEN_PARITY
     * @see SerialPort#MARK_PARITY
     * @see SerialPort#SPACE_PARITY
     */
    public Builder setParity(int parity) {
      this.parity = parity;
      return this;
    }

    /**
     * Enable or disable RS-485 mode, i.e. transmit/receive mode signaling using the RTS pin.
     *
     * @param rs485Mode true to enable RS-485 mode, false to disable.
     * @return this {@link Builder}.
     */
    public Builder setRs485Mode(boolean rs485Mode) {
      this.rs485Mode = rs485Mode;
      return this;
    }

    /**
     * Set the {@link ExecutorService} to use when delivering frame received callbacks.
     *
     * @param executor the executor service.
     * @return this {@link Builder}.
     */
    public Builder setExecutor(ExecutorService executor) {
      this.executor = executor;
      return this;
    }

    /**
     * Build a new {@link SerialPortTransportConfig} from the current state of this builder.
     *
     * @return a new {@link SerialPortTransportConfig}.
     */
    public SerialPortTransportConfig build() {
      if (serialPort == null) {
        throw new NullPointerException("serialPort must not be null");
      }
      if (executor == null) {
        executor = Modbus.sharedExecutor();
      }

      return new SerialPortTransportConfig(
          serialPort, baudRate, dataBits, stopBits, parity, rs485Mode, executor);
    }
  }
}

```

---

### `modbus/serial/src/main/java/com/digitalpetri/modbus/serial/client/SerialPortClientTransport.java`

```java
package com.digitalpetri.modbus.serial.client;

import com.digitalpetri.modbus.ModbusRtuFrame;
import com.digitalpetri.modbus.ModbusRtuResponseFrameParser;
import com.digitalpetri.modbus.ModbusRtuResponseFrameParser.Accumulated;
import com.digitalpetri.modbus.ModbusRtuResponseFrameParser.ParserState;
import com.digitalpetri.modbus.client.ModbusRtuClientTransport;
import com.digitalpetri.modbus.internal.util.ExecutionQueue;
import com.digitalpetri.modbus.serial.SerialPortTransportConfig;
import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Modbus RTU/Serial client transport; a {@link ModbusRtuClientTransport} that sends and receives
 * {@link ModbusRtuFrame}s over a serial port.
 */
public class SerialPortClientTransport implements ModbusRtuClientTransport {

  private final ModbusRtuResponseFrameParser frameParser = new ModbusRtuResponseFrameParser();
  private final AtomicReference<Consumer<ModbusRtuFrame>> frameReceiver = new AtomicReference<>();

  private final ExecutionQueue executionQueue;

  private final SerialPort serialPort;

  private final SerialPortTransportConfig config;

  public SerialPortClientTransport(SerialPortTransportConfig config) {
    this.config = config;

    serialPort = SerialPort.getCommPort(config.serialPort());

    serialPort.setComPortParameters(
        config.baudRate(),
        config.dataBits(),
        config.stopBits(),
        config.parity(),
        config.rs485Mode());

    executionQueue = new ExecutionQueue(config.executor());
  }

  @Override
  public synchronized CompletableFuture<Void> connect() {
    if (serialPort.isOpen()) {
      return CompletableFuture.completedFuture(null);
    } else {
      if (serialPort.openPort()) {
        frameParser.reset();

        // note: no-op if already added from previous connect()
        serialPort.addDataListener(new ModbusRtuDataListener());

        return CompletableFuture.completedFuture(null);
      } else {
        return CompletableFuture.failedFuture(
            new Exception(
                "failed to open port '%s', lastErrorCode=%d"
                    .formatted(config.serialPort(), serialPort.getLastErrorCode())));
      }
    }
  }

  @Override
  public synchronized CompletableFuture<Void> disconnect() {
    if (serialPort.isOpen()) {
      if (serialPort.closePort()) {
        frameParser.reset();

        return CompletableFuture.completedFuture(null);
      } else {
        return CompletableFuture.failedFuture(
            new Exception(
                "failed to close port '%s', lastErrorCode=%d"
                    .formatted(config.serialPort(), serialPort.getLastErrorCode())));
      }
    } else {
      return CompletableFuture.completedFuture(null);
    }
  }

  @Override
  public boolean isConnected() {
    return serialPort.isOpen();
  }

  @Override
  public CompletionStage<Void> send(ModbusRtuFrame frame) {
    ByteBuffer buffer = ByteBuffer.allocate(256);

    try {
      buffer.put((byte) frame.unitId());
      buffer.put(frame.pdu());
      buffer.put(frame.crc());

      byte[] data = new byte[buffer.position()];
      buffer.flip();
      buffer.get(data);

      int totalWritten = 0;
      while (totalWritten < data.length) {
        int written = serialPort.writeBytes(data, data.length - totalWritten, totalWritten);
        if (written == -1) {
          int errorCode = serialPort.getLastErrorCode();
          throw new Exception(
              "failed to write to port '%s', lastErrorCode=%d"
                  .formatted(config.serialPort(), errorCode));
        }
        totalWritten += written;
      }

      return CompletableFuture.completedFuture(null);
    } catch (Exception e) {
      return CompletableFuture.failedFuture(e);
    }
  }

  @Override
  public void receive(Consumer<ModbusRtuFrame> frameReceiver) {
    this.frameReceiver.set(frameReceiver);
  }

  @Override
  public void resetFrameParser() {
    frameParser.reset();
  }

  private class ModbusRtuDataListener implements SerialPortDataListener {

    /** Bit mask indicating what events we're interested in. */
    private static final int LISTENING_EVENTS = SerialPort.LISTENING_EVENT_DATA_RECEIVED;

    @Override
    public int getListeningEvents() {
      return LISTENING_EVENTS;
    }

    @Override
    public void serialEvent(SerialPortEvent event) {
      if ((event.getEventType() & LISTENING_EVENTS) == LISTENING_EVENTS) {
        onDataReceived(event);
      }
    }

    private void onDataReceived(SerialPortEvent event) {
      byte[] receivedData = event.getReceivedData();

      ParserState state = frameParser.parse(receivedData);

      if (state instanceof Accumulated a) {
        try {
          onFrameReceived(a.frame());
        } finally {
          frameParser.reset();
        }
      }
    }

    private void onFrameReceived(ModbusRtuFrame frame) {
      Consumer<ModbusRtuFrame> frameReceiver = SerialPortClientTransport.this.frameReceiver.get();
      if (frameReceiver != null) {
        executionQueue.submit(() -> frameReceiver.accept(frame));
      }
    }
  }

  /**
   * Create a new {@link SerialPortClientTransport} with a callback that allows customizing the
   * configuration.
   *
   * @param configure a {@link Consumer} that accepts a {@link SerialPortTransportConfig.Builder}
   *     instance to configure.
   * @return a new {@link SerialPortClientTransport}.
   */
  public static SerialPortClientTransport create(
      Consumer<SerialPortTransportConfig.Builder> configure) {

    var builder = new SerialPortTransportConfig.Builder();
    configure.accept(builder);
    return new SerialPortClientTransport(builder.build());
  }
}

```

---

### `modbus/tcp/src/main/java/com/digitalpetri/modbus/tcp/server/NettyServerTransportConfig.java`

```java
package com.digitalpetri.modbus.tcp.server;

import com.digitalpetri.modbus.Modbus;
import com.digitalpetri.modbus.tcp.Netty;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

/**
 * Configuration for a {@link NettyRtuServerTransport}.
 *
 * @param bindAddress the address to bind to.
 * @param port the port to bind to.
 * @param eventLoopGroup the {@link EventLoopGroup} to use.
 * @param executor the {@link ExecutorService} to use.
 * @param bootstrapCustomizer a {@link Consumer} that can be used to customize the Netty {@link
 *     ServerBootstrap}.
 * @param pipelineCustomizer a {@link Consumer} that can be used to customize the Netty {@link
 *     ChannelPipeline}.
 */
public record NettyServerTransportConfig(
    String bindAddress,
    int port,
    EventLoopGroup eventLoopGroup,
    ExecutorService executor,
    Consumer<ServerBootstrap> bootstrapCustomizer,
    Consumer<ChannelPipeline> pipelineCustomizer,
    boolean tlsEnabled,
    Optional<KeyManagerFactory> keyManagerFactory,
    Optional<TrustManagerFactory> trustManagerFactory) {

  /**
   * Create a new {@link NettyServerTransportConfig} with a callback that allows customizing the
   * configuration.
   *
   * @param configure a {@link Consumer} that accepts a {@link Builder} instance to configure.
   * @return a new {@link NettyServerTransportConfig}.
   */
  public static NettyServerTransportConfig create(Consumer<Builder> configure) {
    var builder = new Builder();
    configure.accept(builder);
    return builder.build();
  }

  public static class Builder {

    /** The address to bind to. */
    public String bindAddress = "0.0.0.0";

    /** The port to bind to. */
    public int port = -1;

    /** The {@link EventLoopGroup} to use. */
    public EventLoopGroup eventLoopGroup;

    /** The {@link ExecutorService} to use. */
    public ExecutorService executor;

    /** A {@link Consumer} that can be used to customize the Netty {@link ServerBootstrap}. */
    public Consumer<ServerBootstrap> bootstrapCustomizer = b -> {};

    /** A {@link Consumer} that can be used to customize the Netty {@link ChannelPipeline}. */
    public Consumer<ChannelPipeline> pipelineCustomizer = p -> {};

    public boolean tlsEnabled = false;
    public KeyManagerFactory keyManagerFactory = null;
    public TrustManagerFactory trustManagerFactory = null;

    /**
     * Set the address to bind to.
     *
     * @param bindAddress the address to bind to.
     * @return this {@link Builder}.
     */
    public Builder setBindAddress(String bindAddress) {
      this.bindAddress = bindAddress;
      return this;
    }

    /**
     * Set the port to bind to.
     *
     * @param port the port to bind to.
     * @return this {@link Builder}.
     */
    public Builder setPort(int port) {
      this.port = port;
      return this;
    }

    /**
     * Set the {@link EventLoopGroup} to use.
     *
     * @param eventLoopGroup the {@link EventLoopGroup} to use.
     * @return this {@link Builder}.
     */
    public Builder setEventLoopGroup(EventLoopGroup eventLoopGroup) {
      this.eventLoopGroup = eventLoopGroup;
      return this;
    }

    /**
     * Set the {@link ExecutorService} to use.
     *
     * @param executor the {@link ExecutorService} to use.
     * @return this {@link Builder}.
     */
    public Builder setExecutor(ExecutorService executor) {
      this.executor = executor;
      return this;
    }

    /**
     * Set the {@link Consumer} that can be used to customize the Netty {@link ServerBootstrap}.
     *
     * @param bootstrapCustomizer the {@link Consumer} to use.
     * @return this {@link Builder}.
     */
    public Builder setBootstrapCustomizer(Consumer<ServerBootstrap> bootstrapCustomizer) {
      this.bootstrapCustomizer = bootstrapCustomizer;
      return this;
    }

    /**
     * Set the {@link Consumer} that can be used to customize the Netty {@link ChannelPipeline}.
     *
     * @param pipelineCustomizer the {@link Consumer} to use.
     * @return this {@link Builder}.
     */
    public Builder setPipelineCustomizer(Consumer<ChannelPipeline> pipelineCustomizer) {
      this.pipelineCustomizer = pipelineCustomizer;
      return this;
    }

    /**
     * Set whether TLS is enabled.
     *
     * @param tlsEnabled whether TLS is enabled.
     * @return this {@link Builder}.
     */
    public Builder setTlsEnabled(boolean tlsEnabled) {
      this.tlsEnabled = tlsEnabled;
      return this;
    }

    /**
     * Set the {@link KeyManagerFactory} to use.
     *
     * @param keyManagerFactory the {@link KeyManagerFactory} to use.
     * @return this {@link Builder}.
     */
    public Builder setKeyManagerFactory(KeyManagerFactory keyManagerFactory) {
      this.keyManagerFactory = keyManagerFactory;
      return this;
    }

    /**
     * Set the {@link TrustManagerFactory} to use.
     *
     * @param trustManagerFactory the {@link TrustManagerFactory} to use.
     * @return this {@link Builder}.
     */
    public Builder setTrustManagerFactory(TrustManagerFactory trustManagerFactory) {
      this.trustManagerFactory = trustManagerFactory;
      return this;
    }

    public NettyServerTransportConfig build() {
      if (port == -1) {
        port = tlsEnabled ? 802 : 502;
      }
      if (eventLoopGroup == null) {
        eventLoopGroup = Netty.sharedEventLoop();
      }
      if (executor == null) {
        executor = Modbus.sharedExecutor();
      }

      return new NettyServerTransportConfig(
          bindAddress,
          port,
          eventLoopGroup,
          executor,
          bootstrapCustomizer,
          pipelineCustomizer,
          tlsEnabled,
          Optional.ofNullable(keyManagerFactory),
          Optional.ofNullable(trustManagerFactory));
    }
  }
}

```

---

### `modbus/tcp/src/main/java/com/digitalpetri/modbus/tcp/server/NettyTcpServerTransport.java`

```java
package com.digitalpetri.modbus.tcp.server;

import com.digitalpetri.modbus.ModbusTcpFrame;
import com.digitalpetri.modbus.exceptions.UnknownUnitIdException;
import com.digitalpetri.modbus.internal.util.ExecutionQueue;
import com.digitalpetri.modbus.server.ModbusRequestContext.ModbusTcpRequestContext;
import com.digitalpetri.modbus.server.ModbusTcpServerTransport;
import com.digitalpetri.modbus.tcp.ModbusTcpCodec;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProtocols;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Modbus/TCP transport; a {@link ModbusTcpServerTransport} that sends and receives {@link
 * ModbusTcpFrame}s over TCP.
 */
public class NettyTcpServerTransport implements ModbusTcpServerTransport {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final AtomicReference<FrameReceiver<ModbusTcpRequestContext, ModbusTcpFrame>>
      frameReceiver = new AtomicReference<>();

  private final AtomicReference<ServerSocketChannel> serverChannel = new AtomicReference<>();
  private final List<Channel> clientChannels = new CopyOnWriteArrayList<>();

  private final ExecutionQueue executionQueue;
  private final NettyServerTransportConfig config;

  public NettyTcpServerTransport(NettyServerTransportConfig config) {
    this.config = config;

    executionQueue = new ExecutionQueue(config.executor(), 1);
  }

  @Override
  public void receive(FrameReceiver<ModbusTcpRequestContext, ModbusTcpFrame> frameReceiver) {
    this.frameReceiver.set(frameReceiver);
  }

  @Override
  public CompletableFuture<Void> bind() {
    final var future = new CompletableFuture<Void>();

    var bootstrap = new ServerBootstrap();

    bootstrap
        .channel(NioServerSocketChannel.class)
        .childHandler(
            new ChannelInitializer<SocketChannel>() {
              @Override
              protected void initChannel(SocketChannel channel) throws Exception {
                clientChannels.add(channel);

                if (config.tlsEnabled()) {
                  SslContext sslContext =
                      SslContextBuilder.forServer(config.keyManagerFactory().orElseThrow())
                          .clientAuth(ClientAuth.REQUIRE)
                          .trustManager(config.trustManagerFactory().orElseThrow())
                          .protocols(SslProtocols.TLS_v1_2, SslProtocols.TLS_v1_3)
                          .build();

                  channel.pipeline().addLast(sslContext.newHandler(channel.alloc()));
                }

                channel
                    .pipeline()
                    .addLast(
                        new ChannelInboundHandlerAdapter() {
                          @Override
                          public void channelInactive(ChannelHandlerContext ctx) {
                            clientChannels.remove(ctx.channel());
                          }
                        })
                    .addLast(new ModbusTcpCodec())
                    .addLast(new ModbusTcpFrameHandler());

                config.pipelineCustomizer().accept(channel.pipeline());
              }
            });

    bootstrap.group(config.eventLoopGroup());
    bootstrap.option(ChannelOption.SO_REUSEADDR, Boolean.TRUE);
    bootstrap.childOption(ChannelOption.TCP_NODELAY, Boolean.TRUE);

    config.bootstrapCustomizer().accept(bootstrap);

    bootstrap
        .bind(config.bindAddress(), config.port())
        .addListener(
            (ChannelFutureListener)
                channelFuture -> {
                  if (channelFuture.isSuccess()) {
                    serverChannel.set((ServerSocketChannel) channelFuture.channel());

                    future.complete(null);
                  } else {
                    future.completeExceptionally(channelFuture.cause());
                  }
                });

    return future;
  }

  @Override
  public CompletableFuture<Void> unbind() {
    ServerSocketChannel channel = serverChannel.getAndSet(null);

    if (channel != null) {
      var future = new CompletableFuture<Void>();
      channel
          .close()
          .addListener(
              (ChannelFutureListener)
                  cf -> {
                    clientChannels.forEach(Channel::close);
                    clientChannels.clear();

                    if (cf.isSuccess()) {
                      future.complete(null);
                    } else {
                      future.completeExceptionally(cf.cause());
                    }
                  });
      return future;
    } else {
      return CompletableFuture.completedFuture(null);
    }
  }

  private class ModbusTcpFrameHandler extends SimpleChannelInboundHandler<ModbusTcpFrame> {

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
      logger.error("Exception caught", cause);
      ctx.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ModbusTcpFrame requestFrame) {
      FrameReceiver<ModbusTcpRequestContext, ModbusTcpFrame> frameReceiver =
          NettyTcpServerTransport.this.frameReceiver.get();

      if (frameReceiver != null) {
        executionQueue.submit(
            () -> {
              try {
                ModbusTcpFrame responseFrame =
                    frameReceiver.receive(new NettyRequestContext(ctx), requestFrame);

                ctx.channel().writeAndFlush(responseFrame);
              } catch (UnknownUnitIdException e) {
                logger.debug(
                    "Ignoring request for unknown unit id: {}", requestFrame.header().unitId());
              } catch (Exception e) {
                logger.error("Error handling frame: {}", e.getMessage(), e);

                ctx.close();
              }
            });
      }
    }
  }

  /**
   * Create a new {@link NettyTcpServerTransport} with a callback that allows customizing the
   * configuration.
   *
   * @param configure a {@link Consumer} that accepts a {@link NettyServerTransportConfig.Builder}
   *     instance to configure.
   * @return a new {@link NettyTcpServerTransport}.
   */
  public static NettyTcpServerTransport create(
      Consumer<NettyServerTransportConfig.Builder> configure) {

    var builder = new NettyServerTransportConfig.Builder();
    configure.accept(builder);
    return new NettyTcpServerTransport(builder.build());
  }
}

```

---

### `modbus/tcp/src/main/java/com/digitalpetri/modbus/tcp/server/NettyRequestContext.java`

```java
package com.digitalpetri.modbus.tcp.server;

import com.digitalpetri.modbus.server.ModbusRequestContext.ModbusRtuTlsRequestContext;
import com.digitalpetri.modbus.server.ModbusRequestContext.ModbusTcpTlsRequestContext;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

import javax.net.ssl.SSLPeerUnverifiedException;
import java.net.SocketAddress;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Optional;

/**
 * Combined {@link ModbusTcpTlsRequestContext} and {@link ModbusRtuTlsRequestContext} implementation
 * for Netty-based transports.
 */
class NettyRequestContext implements ModbusTcpTlsRequestContext, ModbusRtuTlsRequestContext {

  private static final AttributeKey<String> CLIENT_ROLE = AttributeKey.valueOf("clientRole");

  private static final AttributeKey<X509Certificate[]> CLIENT_CERTIFICATE_CHAIN =
      AttributeKey.valueOf("clientCertificateChain");

  private final ChannelHandlerContext ctx;

  NettyRequestContext(ChannelHandlerContext ctx) {
    this.ctx = ctx;
  }

  @Override
  public SocketAddress localAddress() {
    return ctx.channel().localAddress();
  }

  @Override
  public SocketAddress remoteAddress() {
    return ctx.channel().remoteAddress();
  }

  @Override
  public Optional<String> clientRole() {
    Attribute<String> attr = ctx.channel().attr(CLIENT_ROLE);

    String clientRole = attr.get();

    if (clientRole == null) {
      X509Certificate x509Certificate = clientCertificateChain()[0];

      byte[] bs = x509Certificate.getExtensionValue("1.3.6.1.4.1.50316.802.1");

      if (bs != null && bs.length >= 4) {
        // Strip the leading tag and length bytes.
        clientRole = new String(bs, 4, bs.length - 4);
      } else {
        clientRole = "";
      }

      attr.set(clientRole);
    }

    if (clientRole.isEmpty()) {
      return Optional.empty();
    } else {
      return Optional.of(clientRole);
    }
  }

  @Override
  public X509Certificate[] clientCertificateChain() {
    Attribute<X509Certificate[]> attr = ctx.channel().attr(CLIENT_CERTIFICATE_CHAIN);

    X509Certificate[] clientCertificateChain = attr.get();

    if (clientCertificateChain == null) {
      try {
        SslHandler handler = ctx.channel().pipeline().get(SslHandler.class);
        Certificate[] peerCertificates = handler.engine().getSession().getPeerCertificates();

        clientCertificateChain =
            Arrays.stream(peerCertificates)
                .map(cert -> (X509Certificate) cert)
                .toArray(X509Certificate[]::new);

        attr.set(clientCertificateChain);
      } catch (SSLPeerUnverifiedException e) {
        throw new RuntimeException(e);
      }
    }

    return clientCertificateChain;
  }
}

```

---

### `modbus/tcp/src/main/java/com/digitalpetri/modbus/tcp/server/NettyRtuServerTransport.java`

```java
package com.digitalpetri.modbus.tcp.server;

import com.digitalpetri.modbus.ModbusRtuFrame;
import com.digitalpetri.modbus.ModbusRtuRequestFrameParser;
import com.digitalpetri.modbus.ModbusRtuRequestFrameParser.Accumulated;
import com.digitalpetri.modbus.ModbusRtuRequestFrameParser.ParseError;
import com.digitalpetri.modbus.ModbusRtuRequestFrameParser.ParserState;
import com.digitalpetri.modbus.exceptions.UnknownUnitIdException;
import com.digitalpetri.modbus.internal.util.ExecutionQueue;
import com.digitalpetri.modbus.server.ModbusRequestContext.ModbusRtuRequestContext;
import com.digitalpetri.modbus.server.ModbusRtuServerTransport;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProtocols;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Modbus RTU/TCP server transport; a {@link ModbusRtuServerTransport} that sends and receives
 * {@link ModbusRtuFrame}s over TCP.
 */
public class NettyRtuServerTransport implements ModbusRtuServerTransport {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final AtomicReference<FrameReceiver<ModbusRtuRequestContext, ModbusRtuFrame>>
      frameReceiver = new AtomicReference<>();
  private final ModbusRtuRequestFrameParser frameParser = new ModbusRtuRequestFrameParser();

  private final AtomicReference<ServerSocketChannel> serverChannel = new AtomicReference<>();

  private final AtomicReference<Channel> clientChannel = new AtomicReference<>();

  private final ExecutionQueue executionQueue;
  private final NettyServerTransportConfig config;

  public NettyRtuServerTransport(NettyServerTransportConfig config) {
    this.config = config;

    executionQueue = new ExecutionQueue(config.executor(), 1);
  }

  @Override
  public CompletionStage<Void> bind() {
    final var future = new CompletableFuture<Void>();

    var bootstrap = new ServerBootstrap();

    bootstrap
        .channel(NioServerSocketChannel.class)
        .childHandler(
            new ChannelInitializer<SocketChannel>() {
              @Override
              protected void initChannel(SocketChannel channel) throws Exception {
                if (clientChannel.compareAndSet(null, channel)) {
                  if (config.tlsEnabled()) {
                    SslContext sslContext =
                        SslContextBuilder.forServer(config.keyManagerFactory().orElseThrow())
                            .clientAuth(ClientAuth.REQUIRE)
                            .trustManager(config.trustManagerFactory().orElseThrow())
                            .protocols(SslProtocols.TLS_v1_2, SslProtocols.TLS_v1_3)
                            .build();

                    channel.pipeline().addLast(sslContext.newHandler(channel.alloc()));
                  }

                  channel
                      .pipeline()
                      .addLast(
                          new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelInactive(ChannelHandlerContext ctx) {
                              clientChannel.set(null);
                            }
                          })
                      .addLast(new ModbusRtuServerFrameReceiver());

                  config.pipelineCustomizer().accept(channel.pipeline());
                } else {
                  channel.close();
                }
              }
            });

    bootstrap.group(config.eventLoopGroup());
    bootstrap.option(ChannelOption.SO_REUSEADDR, Boolean.TRUE);
    bootstrap.childOption(ChannelOption.TCP_NODELAY, Boolean.TRUE);

    config.bootstrapCustomizer().accept(bootstrap);

    bootstrap
        .bind(config.bindAddress(), config.port())
        .addListener(
            (ChannelFutureListener)
                channelFuture -> {
                  if (channelFuture.isSuccess()) {
                    serverChannel.set((ServerSocketChannel) channelFuture.channel());

                    future.complete(null);
                  } else {
                    future.completeExceptionally(channelFuture.cause());
                  }
                });

    return future;
  }

  @Override
  public CompletionStage<Void> unbind() {
    ServerSocketChannel channel = serverChannel.getAndSet(null);

    if (channel != null) {
      var future = new CompletableFuture<Void>();
      channel
          .close()
          .addListener(
              (ChannelFutureListener)
                  cf -> {
                    Channel ch = clientChannel.getAndSet(null);
                    if (ch != null) {
                      ch.close();
                    }

                    if (cf.isSuccess()) {
                      future.complete(null);
                    } else {
                      future.completeExceptionally(cf.cause());
                    }
                  });
      return future;
    } else {
      return CompletableFuture.completedFuture(null);
    }
  }

  @Override
  public void receive(FrameReceiver<ModbusRtuRequestContext, ModbusRtuFrame> frameReceiver) {
    this.frameReceiver.set(frameReceiver);
  }

  private class ModbusRtuServerFrameReceiver extends SimpleChannelInboundHandler<ByteBuf> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf buffer) {
      byte[] data = new byte[buffer.readableBytes()];
      buffer.readBytes(data);

      ParserState state = frameParser.parse(data);

      if (state instanceof Accumulated a) {
        try {
          onFrameReceived(ctx, a.frame());
        } finally {
          frameParser.reset();
        }
      } else if (state instanceof ParseError e) {
        logger.error("Error parsing frame: {}", e.message());

        frameParser.reset();
        ctx.close();
      }
    }

    private void onFrameReceived(ChannelHandlerContext ctx, ModbusRtuFrame requestFrame) {
      FrameReceiver<ModbusRtuRequestContext, ModbusRtuFrame> frameReceiver =
          NettyRtuServerTransport.this.frameReceiver.get();

      if (frameReceiver != null) {
        executionQueue.submit(
            () -> {
              try {
                ModbusRtuFrame responseFrame =
                    frameReceiver.receive(new NettyRequestContext(ctx), requestFrame);

                ByteBuf buffer = Unpooled.buffer();
                buffer.writeByte(responseFrame.unitId());
                buffer.writeBytes(responseFrame.pdu());
                buffer.writeBytes(responseFrame.crc());

                ctx.channel().writeAndFlush(buffer);
              } catch (UnknownUnitIdException e) {
                logger.debug("Ignoring request for unknown unit id: {}", requestFrame.unitId());
              } catch (Exception e) {
                logger.error("Error handling frame: {}", e.getMessage(), e);

                ctx.close();
              }
            });
      }
    }
  }

  /**
   * Create a new {@link NettyRtuServerTransport} with a callback that allows customizing the
   * configuration.
   *
   * @param configure a {@link Consumer} that accepts a {@link NettyServerTransportConfig.Builder}
   *     instance to configure.
   * @return a new {@link NettyRtuServerTransport}.
   */
  public static NettyRtuServerTransport create(
      Consumer<NettyServerTransportConfig.Builder> configure) {

    var builder = new NettyServerTransportConfig.Builder();
    configure.accept(builder);
    return new NettyRtuServerTransport(builder.build());
  }
}

```

---

### `modbus/tcp/src/main/java/com/digitalpetri/modbus/tcp/ModbusTcpCodec.java`

```java
package com.digitalpetri.modbus.tcp;

import com.digitalpetri.modbus.MbapHeader;
import com.digitalpetri.modbus.ModbusTcpFrame;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;

import java.nio.ByteBuffer;
import java.util.List;

public class ModbusTcpCodec extends ByteToMessageCodec<ModbusTcpFrame> {

  public static final int MBAP_TOTAL_LENGTH = 7;
  public static final int MBAP_LENGTH_FIELD_OFFSET = 4;

  @Override
  protected void encode(ChannelHandlerContext ctx, ModbusTcpFrame msg, ByteBuf out) {
    var buffer = ByteBuffer.allocate(MBAP_TOTAL_LENGTH + msg.pdu().limit() - msg.pdu().position());
    MbapHeader.Serializer.encode(msg.header(), buffer);
    buffer.put(msg.pdu());

    buffer.flip();
    out.writeBytes(buffer);
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
    if (in.readableBytes() >= MBAP_TOTAL_LENGTH) {
      int frameLength = in.getUnsignedShort(in.readerIndex() + MBAP_LENGTH_FIELD_OFFSET) + 6;

      if (in.readableBytes() >= frameLength) {
        ByteBuffer buffer = ByteBuffer.allocate(frameLength);
        in.readBytes(buffer);
        buffer.flip();

        MbapHeader header = MbapHeader.Serializer.decode(buffer);
        ByteBuffer pdu = buffer.slice();

        out.add(new ModbusTcpFrame(header, pdu));
      }
    }
  }
}

```

---

### `modbus/tcp/src/main/java/com/digitalpetri/modbus/tcp/Netty.java`

```java
package com.digitalpetri.modbus.tcp;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public final class Netty {

  private static NioEventLoopGroup EVENT_LOOP;

  private static HashedWheelTimer WHEEL_TIMER;

  /**
   * @return a shared {@link NioEventLoopGroup}.
   */
  public static synchronized NioEventLoopGroup sharedEventLoop() {
    if (EVENT_LOOP == null) {
      ThreadFactory threadFactory =
          new ThreadFactory() {
            private final AtomicLong threadNumber = new AtomicLong(0L);

            @Override
            public Thread newThread(Runnable r) {
              Thread thread =
                  new Thread(r, "modbus-netty-event-loop-" + threadNumber.getAndIncrement());
              thread.setDaemon(true);
              return thread;
            }
          };

      EVENT_LOOP = new NioEventLoopGroup(1, threadFactory);
    }

    return EVENT_LOOP;
  }

  /**
   * @return a shared {@link HashedWheelTimer}.
   */
  public static synchronized HashedWheelTimer sharedWheelTimer() {
    if (WHEEL_TIMER == null) {
      ThreadFactory threadFactory =
          r -> {
            Thread thread = new Thread(r, "modbus-netty-wheel-timer");
            thread.setDaemon(true);
            return thread;
          };

      WHEEL_TIMER = new HashedWheelTimer(threadFactory);
    }

    return WHEEL_TIMER;
  }

  /**
   * Release shared resources, waiting at most 5 seconds for each of the shared resources to shut
   * down gracefully.
   *
   * @see #releaseSharedResources(long, TimeUnit)
   */
  public static synchronized void releaseSharedResources() {
    releaseSharedResources(5, TimeUnit.SECONDS);
  }

  /**
   * Release shared resources, waiting at most the specified timeout for each of the shared
   * resources to shut down gracefully.
   *
   * @param timeout the duration of the timeout.
   * @param unit the unit of the timeout duration.
   */
  public static synchronized void releaseSharedResources(long timeout, TimeUnit unit) {
    if (EVENT_LOOP != null) {
      try {
        if (!EVENT_LOOP.shutdownGracefully().await(timeout, unit)) {
          LoggerFactory.getLogger(Netty.class)
              .warn("Event loop not shut down after {} {}.", timeout, unit);
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        LoggerFactory.getLogger(Netty.class).warn("Interrupted awaiting event loop shutdown", e);
      }
      EVENT_LOOP = null;
    }

    if (WHEEL_TIMER != null) {
      WHEEL_TIMER.stop().forEach(Timeout::cancel);
      WHEEL_TIMER = null;
    }
  }
}

```

---

### `modbus/tcp/src/main/java/com/digitalpetri/modbus/tcp/security/SecurityUtil.java`

```java
package com.digitalpetri.modbus.tcp.security;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public class SecurityUtil {

  /**
   * Create a {@link KeyManagerFactory} from a private key and certificates.
   *
   * @param privateKey the private key.
   * @param certificates the certificates.
   * @return a {@link KeyManagerFactory}.
   * @throws GeneralSecurityException if an error occurs.
   * @throws IOException if an error occurs.
   */
  public static KeyManagerFactory createKeyManagerFactory(
      PrivateKey privateKey, X509Certificate... certificates)
      throws GeneralSecurityException, IOException {

    KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
    keyStore.load(null, null);

    keyStore.setKeyEntry("key", privateKey, new char[0], certificates);

    return createKeyManagerFactory(keyStore, new char[0]);
  }

  /**
   * Create a {@link KeyManagerFactory} from a {@link KeyStore}.
   *
   * @param keyStore the {@link KeyStore}.
   * @param keyStorePassword the password for the {@link KeyStore}.
   * @return a {@link KeyManagerFactory}.
   * @throws GeneralSecurityException if an error occurs.
   */
  public static KeyManagerFactory createKeyManagerFactory(
      KeyStore keyStore, char[] keyStorePassword) throws GeneralSecurityException {

    KeyManagerFactory keyManagerFactory =
        KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());

    keyManagerFactory.init(keyStore, keyStorePassword);

    return keyManagerFactory;
  }

  /**
   * Create a {@link TrustManagerFactory} from certificates.
   *
   * @param certificates the certificates.
   * @return a {@link TrustManagerFactory}.
   * @throws GeneralSecurityException if an error occurs.
   * @throws IOException if an error occurs.
   */
  public static TrustManagerFactory createTrustManagerFactory(X509Certificate... certificates)
      throws GeneralSecurityException, IOException {

    KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
    keyStore.load(null, null);

    for (int i = 0; i < certificates.length; i++) {
      keyStore.setCertificateEntry("cert" + i, certificates[i]);
    }

    return createTrustManagerFactory(keyStore);
  }

  /**
   * Create a {@link TrustManagerFactory} from a {@link KeyStore}.
   *
   * @param keyStore the {@link KeyStore}.
   * @return a {@link TrustManagerFactory}.
   * @throws GeneralSecurityException if an error occurs.
   */
  public static TrustManagerFactory createTrustManagerFactory(KeyStore keyStore)
      throws GeneralSecurityException {

    TrustManagerFactory trustManagerFactory =
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

    trustManagerFactory.init(keyStore);

    return trustManagerFactory;
  }
}

```

---

### `modbus/tcp/src/main/java/com/digitalpetri/modbus/tcp/client/NettyTcpClientTransport.java`

```java
package com.digitalpetri.modbus.tcp.client;

import com.digitalpetri.fsm.FsmContext;
import com.digitalpetri.modbus.ModbusTcpFrame;
import com.digitalpetri.modbus.client.ModbusTcpClientTransport;
import com.digitalpetri.modbus.internal.util.ExecutionQueue;
import com.digitalpetri.modbus.tcp.ModbusTcpCodec;
import com.digitalpetri.netty.fsm.*;
import com.digitalpetri.netty.fsm.ChannelFsm.TransitionListener;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Modbus/TCP client transport; a {@link ModbusTcpClientTransport} that sends and receives {@link
 * ModbusTcpFrame}s over TCP.
 */
public class NettyTcpClientTransport implements ModbusTcpClientTransport {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final AtomicReference<Consumer<ModbusTcpFrame>> frameReceiver = new AtomicReference<>();

  private final List<ConnectionListener> connectionListeners = new CopyOnWriteArrayList<>();

  private final ChannelFsm channelFsm;
  private final ExecutionQueue executionQueue;

  private final NettyClientTransportConfig config;

  public NettyTcpClientTransport(NettyClientTransportConfig config) {
    this.config = config;

    ChannelFsmConfigBuilder channelFsmConfigBuilder =
        ChannelFsmConfig.newBuilder()
            .setExecutor(config.executor())
            .setLazy(config.reconnectLazy())
            .setPersistent(config.connectPersistent())
            .setChannelActions(new ModbusTcpChannelActions())
            .setLoggerName("com.digitalpetri.modbus.client.ChannelFsm");

    config.channelFsmCustomizer().accept(channelFsmConfigBuilder);

    channelFsm = ChannelFsmFactory.newChannelFsm(channelFsmConfigBuilder.build());

    executionQueue = new ExecutionQueue(config.executor());

    channelFsm.addTransitionListener(
        (from, to, via) -> {
          logger.debug("onStateTransition: {} -> {} via {}", from, to, via);

          maybeNotifyConnectionListeners(from, to);
        });
  }

  @SuppressWarnings("DuplicatedCode")
  private void maybeNotifyConnectionListeners(State from, State to) {
    if (connectionListeners.isEmpty()) {
      return;
    }

    if (from != State.Connected && to == State.Connected) {
      executionQueue.submit(() -> connectionListeners.forEach(ConnectionListener::onConnection));
    } else if (from == State.Connected && to != State.Connected) {
      executionQueue.submit(
          () -> connectionListeners.forEach(ConnectionListener::onConnectionLost));
    }
  }

  @Override
  public CompletableFuture<Void> connect() {
    return channelFsm.connect().thenApply(c -> null);
  }

  @Override
  public CompletableFuture<Void> disconnect() {
    return channelFsm.disconnect();
  }

  @Override
  public CompletionStage<Void> send(ModbusTcpFrame frame) {
    return channelFsm
        .getChannel()
        .thenCompose(
            channel -> {
              var future = new CompletableFuture<Void>();

              channel
                  .writeAndFlush(frame)
                  .addListener(
                      (ChannelFutureListener)
                          channelFuture -> {
                            if (channelFuture.isSuccess()) {
                              future.complete(null);
                            } else {
                              future.completeExceptionally(channelFuture.cause());
                            }
                          });

              return future;
            });
  }

  @Override
  public void receive(Consumer<ModbusTcpFrame> frameReceiver) {
    this.frameReceiver.set(frameReceiver);
  }

  @Override
  public boolean isConnected() {
    return channelFsm.getState() == State.Connected;
  }

  /**
   * Get the {@link ChannelFsm} used by this transport.
   *
   * <p>This should not generally be used by client code except perhaps to add a {@link
   * TransitionListener} to receive more detailed callbacks about the connection status.
   *
   * @return the {@link ChannelFsm} used by this transport.
   */
  public ChannelFsm getChannelFsm() {
    return channelFsm;
  }

  /**
   * Add a {@link ConnectionListener} to this transport.
   *
   * @param listener the listener to add.
   */
  public void addConnectionListener(ConnectionListener listener) {
    connectionListeners.add(listener);
  }

  /**
   * Remove a {@link ConnectionListener} from this transport.
   *
   * @param listener the listener to remove.
   */
  public void removeConnectionListener(ConnectionListener listener) {
    connectionListeners.remove(listener);
  }

  private class ModbusTcpFrameHandler extends SimpleChannelInboundHandler<ModbusTcpFrame> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ModbusTcpFrame frame) {
      Consumer<ModbusTcpFrame> frameReceiver = NettyTcpClientTransport.this.frameReceiver.get();
      if (frameReceiver != null) {
        executionQueue.submit(() -> frameReceiver.accept(frame));
      }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
      logger.error("Exception caught", cause);
      ctx.close();
    }
  }

  private class ModbusTcpChannelActions implements ChannelActions {

    @Override
    public CompletableFuture<Channel> connect(FsmContext<State, Event> fsmContext) {
      var bootstrap =
          new Bootstrap()
              .channel(NioSocketChannel.class)
              .group(config.eventLoopGroup())
              .option(
                  ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) config.connectTimeout().toMillis())
              .option(ChannelOption.TCP_NODELAY, Boolean.TRUE)
              .handler(newChannelInitializer());

      config.bootstrapCustomizer().accept(bootstrap);

      var future = new CompletableFuture<Channel>();

      bootstrap
          .connect(config.hostname(), config.port())
          .addListener(
              (ChannelFutureListener)
                  channelFuture -> {
                    if (channelFuture.isSuccess()) {
                      Channel channel = channelFuture.channel();

                      if (config.tlsEnabled()) {
                        channel
                            .pipeline()
                            .get(SslHandler.class)
                            .handshakeFuture()
                            .addListener(
                                handshakeFuture -> {
                                  if (handshakeFuture.isSuccess()) {
                                    future.complete(channel);
                                  } else {
                                    future.completeExceptionally(handshakeFuture.cause());
                                  }
                                });
                      } else {
                        future.complete(channel);
                      }
                    } else {
                      future.completeExceptionally(channelFuture.cause());
                    }
                  });

      return future;
    }

    private ChannelInitializer<SocketChannel> newChannelInitializer() {
      return new ChannelInitializer<>() {
        @Override
        protected void initChannel(SocketChannel channel) throws Exception {
          if (config.tlsEnabled()) {
            SslContext sslContext =
                SslContextBuilder.forClient()
                    .clientAuth(ClientAuth.REQUIRE)
                    .keyManager(config.keyManagerFactory().orElseThrow())
                    .trustManager(config.trustManagerFactory().orElseThrow())
                    .protocols(SslProtocols.TLS_v1_2, SslProtocols.TLS_v1_3)
                    .build();

            channel
                .pipeline()
                .addLast(sslContext.newHandler(channel.alloc(), config.hostname(), config.port()));
          }

          channel.pipeline().addLast(new ModbusTcpCodec());
          channel.pipeline().addLast(new ModbusTcpFrameHandler());

          config.pipelineCustomizer().accept(channel.pipeline());
        }
      };
    }

    @Override
    public CompletableFuture<Void> disconnect(
        FsmContext<State, Event> fsmContext, Channel channel) {

      var future = new CompletableFuture<Void>();

      channel.close().addListener((ChannelFutureListener) channelFuture -> future.complete(null));

      return future;
    }
  }

  /**
   * Create a new {@link NettyTcpClientTransport} with a callback that allows customizing the
   * configuration.
   *
   * @param configure a {@link Consumer} that accepts a {@link NettyClientTransportConfig.Builder}
   *     instance to configure.
   * @return a new {@link NettyTcpClientTransport}.
   */
  public static NettyTcpClientTransport create(
      Consumer<NettyClientTransportConfig.Builder> configure) {

    var config = NettyClientTransportConfig.create(configure);

    return new NettyTcpClientTransport(config);
  }

  public interface ConnectionListener {

    /** Callback invoked when the transport has connected. */
    void onConnection();

    /**
     * Callback invoked when the transport has disconnected.
     *
     * <p>Note that implementations do not need to initiate a reconnect, as this is handled
     * automatically by {@link NettyTcpClientTransport}.
     */
    void onConnectionLost();
  }
}

```

---

### `modbus/tcp/src/main/java/com/digitalpetri/modbus/tcp/client/NettyClientTransportConfig.java`

```java
package com.digitalpetri.modbus.tcp.client;

import com.digitalpetri.modbus.Modbus;
import com.digitalpetri.modbus.tcp.Netty;
import com.digitalpetri.netty.fsm.ChannelFsmConfigBuilder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

/**
 * Configuration for a {@link NettyTcpClientTransport}.
 *
 * @param hostname the hostname or IP address to connect to.
 * @param port the port to connect to.
 * @param connectTimeout the connect timeout.
 * @param connectPersistent whether to connect persistently.
 * @param reconnectLazy whether to reconnect lazily.
 * @param eventLoopGroup the {@link EventLoopGroup} to use.
 * @param executor the {@link ExecutorService} to use.
 * @param bootstrapCustomizer a {@link Consumer} that can be used to customize the Netty {@link
 *     Bootstrap}.
 * @param pipelineCustomizer a {@link Consumer} that can be used to customize the Netty {@link
 *     ChannelPipeline}.
 * @param channelFsmCustomizer a {@link Consumer} that can be used to customize the {@link
 *     ChannelFsmConfigBuilder}.
 * @param tlsEnabled whether to enable TLS (Modbus/TCP Security).
 * @param keyManagerFactory the {@link KeyManagerFactory} to use if TLS is enabled.
 * @param trustManagerFactory the {@link TrustManagerFactory} to use if TLS is enabled.
 */
public record NettyClientTransportConfig(
    String hostname,
    int port,
    Duration connectTimeout,
    boolean connectPersistent,
    boolean reconnectLazy,
    EventLoopGroup eventLoopGroup,
    ExecutorService executor,
    Consumer<Bootstrap> bootstrapCustomizer,
    Consumer<ChannelPipeline> pipelineCustomizer,
    Consumer<ChannelFsmConfigBuilder> channelFsmCustomizer,
    boolean tlsEnabled,
    Optional<KeyManagerFactory> keyManagerFactory,
    Optional<TrustManagerFactory> trustManagerFactory) {

  /**
   * Create a new {@link NettyClientTransportConfig} with a callback that allows customizing the
   * configuration.
   *
   * @param configure a {@link Consumer} that accepts a {@link Builder} instance to configure.
   * @return a new {@link NettyClientTransportConfig}.
   */
  public static NettyClientTransportConfig create(Consumer<Builder> configure) {
    var builder = new Builder();
    configure.accept(builder);
    return builder.build();
  }

  public static class Builder {

    /** The hostname or IP address to connect to. */
    public String hostname;

    /** The port to connect to. */
    public int port = -1;

    /** The connect timeout. */
    public Duration connectTimeout = Duration.ofSeconds(5);

    /** Whether to connect persistently. */
    public boolean connectPersistent = true;

    /** Whether to reconnect lazily. */
    public boolean reconnectLazy = false;

    /**
     * The {@link EventLoopGroup} to use.
     *
     * @see Netty#sharedEventLoop()
     */
    public EventLoopGroup eventLoopGroup;

    /**
     * The {@link ExecutorService} to use.
     *
     * @see Modbus#sharedExecutor()
     */
    public ExecutorService executor;

    /** A {@link Consumer} that can be used to customize the Netty {@link Bootstrap}. */
    public Consumer<Bootstrap> bootstrapCustomizer = b -> {};

    /** A {@link Consumer} that can be used to customize the Netty {@link ChannelPipeline}. */
    public Consumer<ChannelPipeline> pipelineCustomizer = p -> {};

    /** A {@link Consumer} that can be used to customize the {@link ChannelFsmConfigBuilder}. */
    public Consumer<ChannelFsmConfigBuilder> channelFsmCustomizer = c -> {};

    /** Whether to enable TLS (Modbus/TCP Security). */
    public boolean tlsEnabled = false;

    /** The {@link KeyManagerFactory} to use if TLS is enabled. */
    public KeyManagerFactory keyManagerFactory = null;

    /** The {@link TrustManagerFactory} to use if TLS is enabled. */
    public TrustManagerFactory trustManagerFactory = null;

    /**
     * Set the hostname or IP address to connect to.
     *
     * @param hostname the hostname or IP address to connect to.
     * @return this Builder.
     */
    public Builder setHostname(String hostname) {
      this.hostname = hostname;
      return this;
    }

    /**
     * Set the port to connect to.
     *
     * @param port the port to connect to.
     * @return this Builder.
     */
    public Builder setPort(int port) {
      this.port = port;
      return this;
    }

    /**
     * Set the connect timeout.
     *
     * @param connectTimeout the connect timeout.
     * @return this Builder.
     */
    public Builder setConnectTimeout(Duration connectTimeout) {
      this.connectTimeout = connectTimeout;
      return this;
    }

    /**
     * Set whether to connect persistently.
     *
     * @param connectPersistent whether to connect persistently.
     * @return this Builder.
     */
    public Builder setConnectPersistent(boolean connectPersistent) {
      this.connectPersistent = connectPersistent;
      return this;
    }

    /**
     * Set whether to reconnect lazily.
     *
     * @param reconnectLazy whether to reconnect lazily.
     * @return this Builder.
     */
    public Builder setReconnectLazy(boolean reconnectLazy) {
      this.reconnectLazy = reconnectLazy;
      return this;
    }

    /**
     * Set the {@link EventLoopGroup} to use.
     *
     * @param eventLoopGroup the {@link EventLoopGroup} to use.
     * @return this Builder.
     * @see Netty#sharedEventLoop()
     */
    public Builder setEventLoopGroup(EventLoopGroup eventLoopGroup) {
      this.eventLoopGroup = eventLoopGroup;
      return this;
    }

    /**
     * Set the {@link ExecutorService} to use.
     *
     * @param executor the {@link ExecutorService} to use.
     * @return this Builder.
     * @see Modbus#sharedExecutor()
     */
    public Builder setExecutor(ExecutorService executor) {
      this.executor = executor;
      return this;
    }

    /**
     * Set the {@link Consumer} that can be used to customize the Netty {@link Bootstrap}.
     *
     * @param bootstrapCustomizer the {@link Consumer} that can be used to customize the Netty
     *     {@link Bootstrap}.
     * @return this Builder.
     */
    public Builder setBootstrapCustomizer(Consumer<Bootstrap> bootstrapCustomizer) {
      this.bootstrapCustomizer = bootstrapCustomizer;
      return this;
    }

    /**
     * Set the {@link Consumer} that can be used to customize the Netty {@link ChannelPipeline}.
     *
     * @param pipelineCustomizer the {@link Consumer} that can be used to customize the Netty {@link
     *     ChannelPipeline}.
     * @return this Builder.
     */
    public Builder setPipelineCustomizer(Consumer<ChannelPipeline> pipelineCustomizer) {
      this.pipelineCustomizer = pipelineCustomizer;
      return this;
    }

    /**
     * Set the {@link Consumer} that can be used to customize the {@link ChannelFsmConfigBuilder}.
     *
     * @param channelFsmCustomizer the {@link Consumer} that can be used to customize the {@link
     *     ChannelFsmConfigBuilder}.
     * @return this Builder.
     */
    public Builder setChannelFsmCustomizer(Consumer<ChannelFsmConfigBuilder> channelFsmCustomizer) {
      this.channelFsmCustomizer = channelFsmCustomizer;
      return this;
    }

    /**
     * Set whether to enable TLS (Modbus/TCP Security).
     *
     * @param tlsEnabled whether to enable TLS (Modbus/TCP Security).
     * @return this Builder.
     */
    public Builder setTlsEnabled(boolean tlsEnabled) {
      this.tlsEnabled = tlsEnabled;
      return this;
    }

    /**
     * Set the {@link KeyManagerFactory} to use if TLS is enabled.
     *
     * @param keyManagerFactory the {@link KeyManagerFactory} to use if TLS is enabled.
     * @return this Builder.
     */
    public Builder setKeyManagerFactory(KeyManagerFactory keyManagerFactory) {
      this.keyManagerFactory = keyManagerFactory;
      return this;
    }

    /**
     * Set the {@link TrustManagerFactory} to use if TLS is enabled.
     *
     * @param trustManagerFactory the {@link TrustManagerFactory} to use if TLS is enabled.
     * @return this Builder.
     */
    public Builder setTrustManagerFactory(TrustManagerFactory trustManagerFactory) {
      this.trustManagerFactory = trustManagerFactory;
      return this;
    }

    public NettyClientTransportConfig build() {
      if (hostname == null) {
        throw new NullPointerException("hostname must not be null");
      }
      if (port == -1) {
        port = tlsEnabled ? 802 : 502;
      }
      if (eventLoopGroup == null) {
        eventLoopGroup = Netty.sharedEventLoop();
      }
      if (executor == null) {
        executor = Modbus.sharedExecutor();
      }
      if (tlsEnabled) {
        if (keyManagerFactory == null) {
          throw new NullPointerException("keyManagerFactory must not be null");
        }
        if (trustManagerFactory == null) {
          throw new NullPointerException("trustManagerFactory must not be null");
        }
      }

      return new NettyClientTransportConfig(
          hostname,
          port,
          connectTimeout,
          connectPersistent,
          reconnectLazy,
          eventLoopGroup,
          executor,
          bootstrapCustomizer,
          pipelineCustomizer,
          channelFsmCustomizer,
          tlsEnabled,
          Optional.ofNullable(keyManagerFactory),
          Optional.ofNullable(trustManagerFactory));
    }
  }
}

```

---

### `modbus/tcp/src/main/java/com/digitalpetri/modbus/tcp/client/NettyRtuClientTransport.java`

```java
package com.digitalpetri.modbus.tcp.client;

import com.digitalpetri.fsm.FsmContext;
import com.digitalpetri.modbus.ModbusRtuFrame;
import com.digitalpetri.modbus.ModbusRtuResponseFrameParser;
import com.digitalpetri.modbus.ModbusRtuResponseFrameParser.Accumulated;
import com.digitalpetri.modbus.ModbusRtuResponseFrameParser.ParseError;
import com.digitalpetri.modbus.ModbusRtuResponseFrameParser.ParserState;
import com.digitalpetri.modbus.client.ModbusRtuClientTransport;
import com.digitalpetri.modbus.internal.util.ExecutionQueue;
import com.digitalpetri.modbus.tcp.client.NettyTcpClientTransport.ConnectionListener;
import com.digitalpetri.netty.fsm.*;
import com.digitalpetri.netty.fsm.ChannelFsm.TransitionListener;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProtocols;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Modbus RTU/TCP client transport; a {@link ModbusRtuClientTransport} that sends and receives
 * {@link ModbusRtuFrame}s over TCP.
 */
public class NettyRtuClientTransport implements ModbusRtuClientTransport {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final ModbusRtuResponseFrameParser frameParser = new ModbusRtuResponseFrameParser();
  private final AtomicReference<Consumer<ModbusRtuFrame>> frameReceiver = new AtomicReference<>();

  private final List<ConnectionListener> connectionListeners = new CopyOnWriteArrayList<>();

  private final ChannelFsm channelFsm;
  private final ExecutionQueue executionQueue;

  private final NettyClientTransportConfig config;

  public NettyRtuClientTransport(NettyClientTransportConfig config) {
    this.config = config;

    ChannelFsmConfigBuilder channelFsmConfigBuilder =
        ChannelFsmConfig.newBuilder()
            .setExecutor(config.executor())
            .setLazy(config.reconnectLazy())
            .setPersistent(config.connectPersistent())
            .setChannelActions(new ModbusRtuChannelActions());

    config.channelFsmCustomizer().accept(channelFsmConfigBuilder);

    channelFsm = ChannelFsmFactory.newChannelFsm(channelFsmConfigBuilder.build());

    channelFsm.addTransitionListener(
        (from, to, via) -> {
          logger.debug("onStateTransition: {} -> {} via {}", from, to, via);

          maybeNotifyConnectionListeners(from, to);
        });

    executionQueue = new ExecutionQueue(config.executor());
  }

  @SuppressWarnings("DuplicatedCode")
  private void maybeNotifyConnectionListeners(State from, State to) {
    if (connectionListeners.isEmpty()) {
      return;
    }

    if (from != State.Connected && to == State.Connected) {
      executionQueue.submit(() -> connectionListeners.forEach(ConnectionListener::onConnection));
    } else if (from == State.Connected && to != State.Connected) {
      executionQueue.submit(
          () -> connectionListeners.forEach(ConnectionListener::onConnectionLost));
    }
  }

  @Override
  public CompletableFuture<Void> connect() {
    return channelFsm.connect().thenApply(c -> null);
  }

  @Override
  public CompletableFuture<Void> disconnect() {
    return channelFsm.disconnect();
  }

  @Override
  public boolean isConnected() {
    return channelFsm.getState() == State.Connected;
  }

  /**
   * Get the {@link ChannelFsm} used by this transport.
   *
   * <p>This should not generally be used by client code except perhaps to add a {@link
   * TransitionListener} to receive more detailed callbacks about the connection status.
   *
   * @return the {@link ChannelFsm} used by this transport.
   */
  public ChannelFsm getChannelFsm() {
    return channelFsm;
  }

  /**
   * Add a {@link ConnectionListener} to this transport.
   *
   * @param listener the listener to add.
   */
  public void addConnectionListener(ConnectionListener listener) {
    connectionListeners.add(listener);
  }

  /**
   * Remove a {@link ConnectionListener} from this transport.
   *
   * @param listener the listener to remove.
   */
  public void removeConnectionListener(ConnectionListener listener) {
    connectionListeners.remove(listener);
  }

  @Override
  public CompletionStage<Void> send(ModbusRtuFrame frame) {
    return channelFsm
        .getChannel()
        .thenCompose(
            channel -> {
              ByteBuf buffer = Unpooled.buffer();
              buffer.writeByte(frame.unitId());
              buffer.writeBytes(frame.pdu());
              buffer.writeBytes(frame.crc());

              var future = new CompletableFuture<Void>();

              channel
                  .writeAndFlush(buffer)
                  .addListener(
                      (ChannelFutureListener)
                          channelFuture -> {
                            if (channelFuture.isSuccess()) {
                              future.complete(null);
                            } else {
                              future.completeExceptionally(channelFuture.cause());
                            }
                          });

              return future;
            });
  }

  @Override
  public void receive(Consumer<ModbusRtuFrame> frameReceiver) {
    this.frameReceiver.set(frameReceiver);
  }

  @Override
  public void resetFrameParser() {
    frameParser.reset();
  }

  private class ModbusRtuClientFrameReceiver extends SimpleChannelInboundHandler<ByteBuf> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf buffer) {
      byte[] data = new byte[buffer.readableBytes()];
      buffer.readBytes(data);

      ParserState state = frameParser.parse(data);

      if (state instanceof Accumulated a) {
        try {
          onFrameReceived(a.frame());
        } finally {
          frameParser.reset();
        }
      } else if (state instanceof ParseError e) {
        logger.error("Error parsing frame: {}", e.error());

        frameParser.reset();
        ctx.close();
      }
    }

    private void onFrameReceived(ModbusRtuFrame frame) {
      Consumer<ModbusRtuFrame> frameReceiver = NettyRtuClientTransport.this.frameReceiver.get();
      if (frameReceiver != null) {
        executionQueue.submit(() -> frameReceiver.accept(frame));
      }
    }
  }

  private class ModbusRtuChannelActions implements ChannelActions {

    @Override
    public CompletableFuture<Channel> connect(FsmContext<State, Event> fsmContext) {
      var bootstrap =
          new Bootstrap()
              .channel(NioSocketChannel.class)
              .group(config.eventLoopGroup())
              .option(
                  ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) config.connectTimeout().toMillis())
              .option(ChannelOption.TCP_NODELAY, Boolean.TRUE)
              .handler(
                  new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel channel) throws Exception {
                      if (config.tlsEnabled()) {
                        SslContext sslContext =
                            SslContextBuilder.forClient()
                                .clientAuth(ClientAuth.REQUIRE)
                                .keyManager(config.keyManagerFactory().orElseThrow())
                                .trustManager(config.trustManagerFactory().orElseThrow())
                                .protocols(SslProtocols.TLS_v1_2, SslProtocols.TLS_v1_3)
                                .build();

                        channel
                            .pipeline()
                            .addLast(
                                sslContext.newHandler(
                                    channel.alloc(), config.hostname(), config.port()));
                      }

                      channel.pipeline().addLast(new ModbusRtuClientFrameReceiver());

                      config.pipelineCustomizer().accept(channel.pipeline());
                    }
                  });

      config.bootstrapCustomizer().accept(bootstrap);

      var future = new CompletableFuture<Channel>();

      bootstrap
          .connect(config.hostname(), config.port())
          .addListener(
              (ChannelFutureListener)
                  channelFuture -> {
                    if (channelFuture.isSuccess()) {
                      future.complete(channelFuture.channel());
                    } else {
                      future.completeExceptionally(channelFuture.cause());
                    }
                  });

      return future;
    }

    @Override
    public CompletableFuture<Void> disconnect(
        FsmContext<State, Event> fsmContext, Channel channel) {

      var future = new CompletableFuture<Void>();

      channel.close().addListener((ChannelFutureListener) channelFuture -> future.complete(null));

      return future;
    }
  }

  /**
   * Create a new {@link NettyRtuClientTransport} with a callback that allows customizing the
   * configuration.
   *
   * @param configure a {@link Consumer} that accepts a {@link NettyClientTransportConfig.Builder}
   *     instance to configure.
   * @return a new {@link NettyRtuClientTransport}.
   */
  public static NettyRtuClientTransport create(
      Consumer<NettyClientTransportConfig.Builder> configure) {

    var config = NettyClientTransportConfig.create(configure);

    return new NettyRtuClientTransport(config);
  }
}

```

---

### `modbus/tcp/src/main/java/com/digitalpetri/modbus/tcp/client/NettyTimeoutScheduler.java`

```java
package com.digitalpetri.modbus.tcp.client;

import com.digitalpetri.modbus.TimeoutScheduler;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class NettyTimeoutScheduler implements TimeoutScheduler {

  private final HashedWheelTimer wheelTimer;

  public NettyTimeoutScheduler(HashedWheelTimer wheelTimer) {
    this.wheelTimer = wheelTimer;
  }

  @Override
  public TimeoutHandle newTimeout(Task task, long delay, TimeUnit unit) {
    final var ref = new AtomicReference<Timeout>();

    var handle =
        new TimeoutHandle() {
          @Override
          public void cancel() {
            synchronized (ref) {
              ref.get().cancel();
            }
          }

          @Override
          public boolean isCancelled() {
            synchronized (ref) {
              return ref.get().isCancelled();
            }
          }
        };

    synchronized (ref) {
      ref.set(wheelTimer.newTimeout(timeout -> task.run(handle), delay, unit));
    }

    return handle;
  }
}

```

---

### `modbus/tcp/src/test/java/com/digitalpetri/modbus/tcp/ModbusTcpCodecTest.java`

```java
package com.digitalpetri.modbus.tcp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.digitalpetri.modbus.MbapHeader;
import com.digitalpetri.modbus.ModbusTcpFrame;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

class ModbusTcpCodecTest {

  @Test
  void encodeDecodeFrame() {
    var channel = new EmbeddedChannel(new ModbusTcpCodec());

    var frame =
        new ModbusTcpFrame(
            new MbapHeader(0, 0, 5, 0), ByteBuffer.wrap(new byte[] {0x01, 0x02, 0x03, 0x04}));

    channel.writeOutbound(frame);
    ByteBuf encoded = channel.readOutbound();
    System.out.println(ByteBufUtil.hexDump(encoded));

    channel.writeInbound(encoded);
    ModbusTcpFrame decoded = channel.readInbound();

    frame.pdu().flip();

    System.out.println(frame);
    System.out.println(decoded);

    assertEquals(frame, decoded);
  }

  @Test
  void emptyPdu() {
    var rx = Unpooled.copiedBuffer(ByteBufUtil.decodeHexDump("5FFD0000000101"));
    var channel = new EmbeddedChannel(new ModbusTcpCodec());

    channel.writeInbound(rx);
    ModbusTcpFrame frame = channel.readInbound();

    System.out.println(frame);
    assertEquals(0, frame.pdu().remaining());
  }
}

```

---

### `modbus/tests/src/test/java/com/digitalpetri/modbus/test/ModbusRtuClientServerIT.java`

```java
package com.digitalpetri.modbus.test;

import com.digitalpetri.modbus.client.ModbusClient;
import com.digitalpetri.modbus.client.ModbusRtuClient;
import com.digitalpetri.modbus.serial.client.SerialPortClientTransport;
import com.digitalpetri.modbus.serial.server.SerialPortServerTransport;
import com.digitalpetri.modbus.server.ModbusRtuServer;
import com.digitalpetri.modbus.server.ModbusServer;
import com.digitalpetri.modbus.server.ProcessImage;
import com.digitalpetri.modbus.server.ReadWriteModbusServices;
import com.fazecast.jSerialComm.SerialPort;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIf;

@EnabledIf("serialPortsConfigured")
public class ModbusRtuClientServerIT extends ClientServerIT {

  ModbusRtuClient client;
  ModbusRtuServer server;

  @BeforeEach
  void setup() throws Exception {
    var processImage = new ProcessImage();
    var modbusServices =
        new ReadWriteModbusServices() {
          @Override
          protected Optional<ProcessImage> getProcessImage(int unitId) {
            return Optional.of(processImage);
          }
        };

    server =
        ModbusRtuServer.create(
            SerialPortServerTransport.create(
                cfg -> {
                  cfg.serialPort = System.getProperty("modbus.serverSerialPort");
                  cfg.baudRate = 115200;
                  cfg.dataBits = 8;
                  cfg.parity = SerialPort.NO_PARITY;
                  cfg.stopBits = 1;
                }),
            modbusServices);
    server.start();

    client =
        ModbusRtuClient.create(
            SerialPortClientTransport.create(
                cfg -> {
                  cfg.serialPort = System.getProperty("modbus.clientSerialPort");
                  cfg.baudRate = 115200;
                  cfg.dataBits = 8;
                  cfg.parity = SerialPort.NO_PARITY;
                  cfg.stopBits = 1;
                }));
    client.connect();
  }

  @AfterEach
  void teardown() throws Exception {
    if (client != null) {
      client.disconnect();
    }
    if (server != null) {
      server.stop();
    }
  }

  @Override
  ModbusClient getClient() {
    return client;
  }

  @Override
  ModbusServer getServer() {
    return server;
  }

  static boolean serialPortsConfigured() {
    return System.getProperty("modbus.clientSerialPort") != null
        && System.getProperty("modbus.serverSerialPort") != null;
  }
}

```

---

### `modbus/tests/src/test/java/com/digitalpetri/modbus/test/ModbusTcpTlsClientServerIT.java`

```java
package com.digitalpetri.modbus.test;

import com.digitalpetri.modbus.client.ModbusClient;
import com.digitalpetri.modbus.client.ModbusTcpClient;
import com.digitalpetri.modbus.server.ModbusServer;
import com.digitalpetri.modbus.server.ModbusTcpServer;
import com.digitalpetri.modbus.server.ProcessImage;
import com.digitalpetri.modbus.server.ReadWriteModbusServices;
import com.digitalpetri.modbus.tcp.client.NettyTcpClientTransport;
import com.digitalpetri.modbus.tcp.security.SecurityUtil;
import com.digitalpetri.modbus.tcp.server.NettyTcpServerTransport;
import com.digitalpetri.modbus.test.CertificateUtil.KeyPairCert;
import com.digitalpetri.modbus.test.CertificateUtil.Role;
import java.util.Optional;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import org.junit.jupiter.api.BeforeEach;

public class ModbusTcpTlsClientServerIT extends ClientServerIT {

  ModbusTcpClient client;
  ModbusTcpServer server;

  KeyPairCert authorityKeyPairCert = CertificateUtil.generateCaCertificate();

  KeyPairCert clientKeyPairCert =
      CertificateUtil.generateCaSignedCertificate(Role.CLIENT, authorityKeyPairCert);

  KeyPairCert serverKeyPairCert =
      CertificateUtil.generateCaSignedCertificate(Role.SERVER, authorityKeyPairCert);

  NettyTcpClientTransport clientTransport;

  @BeforeEach
  void setup() throws Exception {
    var processImage = new ProcessImage();
    var modbusServices =
        new ReadWriteModbusServices() {
          @Override
          protected Optional<ProcessImage> getProcessImage(int unitId) {
            return Optional.of(processImage);
          }
        };

    KeyManagerFactory serverKeyManagerFactory =
        SecurityUtil.createKeyManagerFactory(
            serverKeyPairCert.keyPair().getPrivate(), serverKeyPairCert.certificate());
    TrustManagerFactory serverTrustManagerFactory =
        SecurityUtil.createTrustManagerFactory(authorityKeyPairCert.certificate());

    int serverPort = -1;

    for (int i = 50200; i < 65536; i++) {
      try {
        final var port = i;
        var serverTransport =
            NettyTcpServerTransport.create(
                cfg -> {
                  cfg.bindAddress = "localhost";
                  cfg.port = port;

                  cfg.tlsEnabled = true;
                  cfg.keyManagerFactory = serverKeyManagerFactory;
                  cfg.trustManagerFactory = serverTrustManagerFactory;
                });

        System.out.println("trying port " + port);
        server = ModbusTcpServer.create(serverTransport, modbusServices);
        server.start();
        serverPort = port;
        break;
      } catch (Exception e) {
        server = null;
      }
    }

    KeyManagerFactory clientKeyManagerFactory =
        SecurityUtil.createKeyManagerFactory(
            clientKeyPairCert.keyPair().getPrivate(), clientKeyPairCert.certificate());
    TrustManagerFactory clientTrustManagerFactory =
        SecurityUtil.createTrustManagerFactory(authorityKeyPairCert.certificate());

    final var port = serverPort;
    clientTransport =
        NettyTcpClientTransport.create(
            cfg -> {
              cfg.hostname = "localhost";
              cfg.port = port;
              cfg.connectPersistent = false;

              cfg.tlsEnabled = true;
              cfg.keyManagerFactory = clientKeyManagerFactory;
              cfg.trustManagerFactory = clientTrustManagerFactory;
            });

    client = ModbusTcpClient.create(clientTransport);
    client.connect();
  }

  @Override
  ModbusClient getClient() {
    return client;
  }

  @Override
  ModbusServer getServer() {
    return server;
  }
}

```

---

### `modbus/tests/src/test/java/com/digitalpetri/modbus/test/ModbusTcpClientServerIT.java`

```java
package com.digitalpetri.modbus.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.digitalpetri.modbus.ModbusPduSerializer.DefaultRequestSerializer;
import com.digitalpetri.modbus.client.ModbusClient;
import com.digitalpetri.modbus.client.ModbusTcpClient;
import com.digitalpetri.modbus.internal.util.Hex;
import com.digitalpetri.modbus.pdu.ReadHoldingRegistersRequest;
import com.digitalpetri.modbus.server.ModbusServer;
import com.digitalpetri.modbus.server.ModbusTcpServer;
import com.digitalpetri.modbus.server.ProcessImage;
import com.digitalpetri.modbus.server.ReadWriteModbusServices;
import com.digitalpetri.modbus.tcp.Netty;
import com.digitalpetri.modbus.tcp.client.NettyTcpClientTransport;
import com.digitalpetri.modbus.tcp.client.NettyTcpClientTransport.ConnectionListener;
import com.digitalpetri.modbus.tcp.client.NettyTimeoutScheduler;
import com.digitalpetri.modbus.tcp.server.NettyTcpServerTransport;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ModbusTcpClientServerIT extends ClientServerIT {

  ModbusTcpClient client;
  ModbusTcpServer server;

  NettyTcpClientTransport clientTransport;

  @BeforeEach
  void setup() throws Exception {
    var processImage = new ProcessImage();
    var modbusServices =
        new ReadWriteModbusServices() {
          @Override
          protected Optional<ProcessImage> getProcessImage(int unitId) {
            return Optional.of(processImage);
          }
        };

    int serverPort = -1;

    for (int i = 50200; i < 65536; i++) {
      try {
        final var port = i;
        var serverTransport =
            NettyTcpServerTransport.create(
                cfg -> {
                  cfg.bindAddress = "localhost";
                  cfg.port = port;
                });

        System.out.println("trying port " + port);
        server = ModbusTcpServer.create(serverTransport, modbusServices);
        server.start();
        serverPort = port;
        break;
      } catch (Exception e) {
        server = null;
      }
    }

    if (server == null) {
      throw new Exception("Failed to start server");
    }

    final var port = serverPort;
    clientTransport =
        NettyTcpClientTransport.create(
            cfg -> {
              cfg.hostname = "localhost";
              cfg.port = port;
              cfg.connectPersistent = false;
            });

    client =
        ModbusTcpClient.create(
            clientTransport,
            cfg -> cfg.timeoutScheduler = new NettyTimeoutScheduler(Netty.sharedWheelTimer()));
    client.connect();
  }

  @AfterEach
  void teardown() throws Exception {
    if (client != null) {
      client.disconnect();
    }
    if (server != null) {
      server.stop();
    }
  }

  @Override
  ModbusClient getClient() {
    return client;
  }

  @Override
  ModbusServer getServer() {
    return server;
  }

  @Test
  void sendRaw() throws Exception {
    var request = new ReadHoldingRegistersRequest(0, 10);
    ByteBuffer buffer = ByteBuffer.allocate(256);
    DefaultRequestSerializer.INSTANCE.encode(request, buffer);

    byte[] requestedPduBytes = new byte[buffer.position()];
    buffer.flip();
    buffer.get(requestedPduBytes);

    System.out.println("requestedPduBytes: " + Hex.format(requestedPduBytes));

    byte[] responsePduBytes = client.sendRaw(0, requestedPduBytes);

    System.out.println("responsePduBytes: " + Hex.format(responsePduBytes));
  }

  @Test
  void connectionListener() throws Exception {
    var onConnection = new CountDownLatch(1);
    var onConnectionLost = new CountDownLatch(1);

    clientTransport.addConnectionListener(
        new ConnectionListener() {
          @Override
          public void onConnection() {
            onConnection.countDown();
          }

          @Override
          public void onConnectionLost() {
            onConnectionLost.countDown();
          }
        });

    assertTrue(client.isConnected());

    client.disconnect();
    assertTrue(onConnectionLost.await(1, TimeUnit.SECONDS));

    client.connect();
    assertTrue(onConnection.await(1, TimeUnit.SECONDS));
  }
}

```

---

### `modbus/tests/src/test/java/com/digitalpetri/modbus/test/CertificateUtil.java`

```java
package com.digitalpetri.modbus.test;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Locale;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

public class CertificateUtil {

  private CertificateUtil() {}

  public static KeyPairCert generateSelfSignedCertificate(Role role) {
    try {
      KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(2048);
      KeyPair keyPair = generator.generateKeyPair();

      var nameBuilder = new X500NameBuilder();
      if (role == Role.CLIENT) {
        nameBuilder.addRDN(BCStyle.CN, "Modbus Client");
      } else {
        nameBuilder.addRDN(BCStyle.CN, "Modbus Server");
      }
      X500Name name = nameBuilder.build();

      var certSerialNumber = new BigInteger(Long.toString(System.currentTimeMillis()));

      SubjectPublicKeyInfo subjectPublicKeyInfo =
          SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());

      var certificateBuilder =
          new X509v3CertificateBuilder(
              name,
              certSerialNumber,
              new Date(),
              new Date(System.currentTimeMillis() + 365 * 24 * 60 * 60 * 1000L),
              name,
              subjectPublicKeyInfo);

      var basicConstraints = new BasicConstraints(false);
      certificateBuilder.addExtension(Extension.basicConstraints, false, basicConstraints);

      // Key Usage
      certificateBuilder.addExtension(
          Extension.keyUsage,
          false,
          new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));

      // Extended Key Usage
      certificateBuilder.addExtension(
          Extension.extendedKeyUsage,
          false,
          new ExtendedKeyUsage(
              role == Role.CLIENT ? KeyPurposeId.id_kp_clientAuth : KeyPurposeId.id_kp_serverAuth));

      // Authority Key Identifier
      certificateBuilder.addExtension(
          Extension.authorityKeyIdentifier,
          false,
          new JcaX509ExtensionUtils().createAuthorityKeyIdentifier(keyPair.getPublic()));

      // Subject Key Identifier
      certificateBuilder.addExtension(
          Extension.subjectKeyIdentifier,
          false,
          new JcaX509ExtensionUtils().createSubjectKeyIdentifier(keyPair.getPublic()));

      // Subject Alternative Name
      if (role == Role.SERVER) {
        certificateBuilder.addExtension(
            Extension.subjectAlternativeName,
            false,
            new GeneralNames(
                new GeneralName[] {new GeneralName(GeneralName.dNSName, "localhost")}));
      }

      // Modbus Security Role OID
      if (role == Role.CLIENT) {
        certificateBuilder.addExtension(
            new ASN1ObjectIdentifier("1.3.6.1.4.1.50316.802.1"),
            false,
            new DEROctetString("Operator".getBytes()));
      }

      var contentSigner =
          new JcaContentSignerBuilder("SHA256withRSA")
              .setProvider(new BouncyCastleProvider())
              .build(keyPair.getPrivate());

      X509CertificateHolder certificateHolder = certificateBuilder.build(contentSigner);

      X509Certificate certificate =
          new JcaX509CertificateConverter().getCertificate(certificateHolder);

      return new KeyPairCert(keyPair, certificate);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static KeyPairCert generateCaSignedCertificate(Role role, KeyPairCert caKeyPairCert) {
    try {
      KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(2048);
      KeyPair keyPair = generator.generateKeyPair();

      var nameBuilder = new X500NameBuilder();
      if (role == Role.CLIENT) {
        nameBuilder.addRDN(BCStyle.CN, "Modbus Client");
      } else {
        nameBuilder.addRDN(BCStyle.CN, "Modbus Server");
      }
      X500Name name = nameBuilder.build();

      var certSerialNumber = new BigInteger(Long.toString(System.currentTimeMillis()));

      SubjectPublicKeyInfo subjectPublicKeyInfo =
          SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());

      var certificateBuilder =
          new X509v3CertificateBuilder(
              new X500Name(caKeyPairCert.certificate().getSubjectX500Principal().getName()),
              certSerialNumber,
              new Date(),
              new Date(System.currentTimeMillis() + 365 * 24 * 60 * 60 * 1000L),
              name,
              subjectPublicKeyInfo);

      var basicConstraints = new BasicConstraints(false);
      certificateBuilder.addExtension(Extension.basicConstraints, false, basicConstraints);

      // Key Usage
      certificateBuilder.addExtension(
          Extension.keyUsage,
          false,
          new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));

      // Extended Key Usage
      certificateBuilder.addExtension(
          Extension.extendedKeyUsage,
          false,
          new ExtendedKeyUsage(
              role == Role.CLIENT ? KeyPurposeId.id_kp_clientAuth : KeyPurposeId.id_kp_serverAuth));

      // Authority Key Identifier
      certificateBuilder.addExtension(
          Extension.authorityKeyIdentifier,
          false,
          new JcaX509ExtensionUtils().createAuthorityKeyIdentifier(caKeyPairCert.certificate()));

      // Subject Key Identifier
      certificateBuilder.addExtension(
          Extension.subjectKeyIdentifier,
          false,
          new JcaX509ExtensionUtils().createSubjectKeyIdentifier(keyPair.getPublic()));

      // Subject Alternative Name
      if (role == Role.SERVER) {
        certificateBuilder.addExtension(
            Extension.subjectAlternativeName,
            false,
            new GeneralNames(
                new GeneralName[] {new GeneralName(GeneralName.dNSName, "localhost")}));
      }

      // Modbus Security Role OID
      if (role == Role.CLIENT) {
        certificateBuilder.addExtension(
            new ASN1ObjectIdentifier("1.3.6.1.4.1.50316.802.1"),
            false,
            new DEROctetString("Operator".getBytes()));
      }

      var contentSigner =
          new JcaContentSignerBuilder("SHA256withRSA")
              .setProvider(new BouncyCastleProvider())
              .build(caKeyPairCert.keyPair().getPrivate());

      X509CertificateHolder certificateHolder = certificateBuilder.build(contentSigner);

      X509Certificate certificate =
          new JcaX509CertificateConverter().getCertificate(certificateHolder);

      return new KeyPairCert(keyPair, certificate);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static KeyPairCert generateCaCertificate() {
    try {
      KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(2048);
      KeyPair keyPair = generator.generateKeyPair();

      var nameBuilder = new X500NameBuilder(BCStyle.INSTANCE);
      nameBuilder.addRDN(BCStyle.CN, "Modbus CA");
      X500Name name = nameBuilder.build();

      var certSerialNumber = new BigInteger(Long.toString(System.currentTimeMillis()));

      SubjectPublicKeyInfo subjectPublicKeyInfo =
          SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());

      var certificateBuilder =
          new X509v3CertificateBuilder(
              name,
              certSerialNumber,
              new Date(),
              new Date(System.currentTimeMillis() + 365 * 24 * 60 * 60 * 1000L),
              Locale.ENGLISH,
              name,
              subjectPublicKeyInfo);

      var basicConstraints = new BasicConstraints(true);
      certificateBuilder.addExtension(Extension.basicConstraints, true, basicConstraints);

      // Key Usage
      certificateBuilder.addExtension(
          Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));

      // Authority Key Identifier
      certificateBuilder.addExtension(
          Extension.authorityKeyIdentifier,
          false,
          new JcaX509ExtensionUtils().createAuthorityKeyIdentifier(keyPair.getPublic()));

      // Subject Key Identifier
      certificateBuilder.addExtension(
          Extension.subjectKeyIdentifier,
          false,
          new JcaX509ExtensionUtils().createSubjectKeyIdentifier(keyPair.getPublic()));

      var contentSigner =
          new JcaContentSignerBuilder("SHA256withRSA")
              .setProvider(new BouncyCastleProvider())
              .build(keyPair.getPrivate());

      X509CertificateHolder certificateHolder = certificateBuilder.build(contentSigner);

      X509Certificate certificate =
          new JcaX509CertificateConverter().getCertificate(certificateHolder);

      return new KeyPairCert(keyPair, certificate);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public enum Role {
    CLIENT,
    SERVER
  }

  public record KeyPairCert(KeyPair keyPair, X509Certificate certificate) {}
}

```

---

### `modbus/tests/src/test/java/com/digitalpetri/modbus/test/ModbusRtuTcpClientServerIT.java`

```java
package com.digitalpetri.modbus.test;

import com.digitalpetri.modbus.client.ModbusClient;
import com.digitalpetri.modbus.client.ModbusRtuClient;
import com.digitalpetri.modbus.server.ModbusRtuServer;
import com.digitalpetri.modbus.server.ModbusServer;
import com.digitalpetri.modbus.server.ProcessImage;
import com.digitalpetri.modbus.server.ReadWriteModbusServices;
import com.digitalpetri.modbus.tcp.client.NettyRtuClientTransport;
import com.digitalpetri.modbus.tcp.server.NettyRtuServerTransport;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public class ModbusRtuTcpClientServerIT extends ClientServerIT {

  ModbusRtuClient client;
  ModbusRtuServer server;

  @BeforeEach
  void setup() throws Exception {
    var processImage = new ProcessImage();
    var modbusServices =
        new ReadWriteModbusServices() {
          @Override
          protected Optional<ProcessImage> getProcessImage(int unitId) {
            return Optional.of(processImage);
          }
        };

    int serverPort = -1;

    for (int i = 50200; i < 65536; i++) {
      try {
        final var port = i;
        var serverTransport =
            NettyRtuServerTransport.create(
                cfg -> {
                  cfg.bindAddress = "localhost";
                  cfg.port = port;
                });

        System.out.println("trying port " + port);
        server = ModbusRtuServer.create(serverTransport, modbusServices);
        server.start();
        serverPort = port;
        break;
      } catch (Exception e) {
        server = null;
      }
    }

    if (server == null) {
      throw new Exception("Failed to start server");
    }

    final var port = serverPort;

    client =
        ModbusRtuClient.create(
            NettyRtuClientTransport.create(
                cfg -> {
                  cfg.hostname = "localhost";
                  cfg.port = port;
                  cfg.connectPersistent = false;
                }));
    client.connect();
  }

  @AfterEach
  void teardown() throws Exception {
    if (client != null) {
      client.disconnect();
    }
    if (server != null) {
      server.stop();
    }
  }

  @Override
  ModbusClient getClient() {
    return client;
  }

  @Override
  ModbusServer getServer() {
    return server;
  }
}

```

---

### `modbus/tests/src/test/java/com/digitalpetri/modbus/test/ClientServerIT.java`

```java
package com.digitalpetri.modbus.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.digitalpetri.modbus.client.ModbusClient;
import com.digitalpetri.modbus.exceptions.ModbusExecutionException;
import com.digitalpetri.modbus.pdu.MaskWriteRegisterRequest;
import com.digitalpetri.modbus.pdu.MaskWriteRegisterResponse;
import com.digitalpetri.modbus.pdu.ReadCoilsRequest;
import com.digitalpetri.modbus.pdu.ReadCoilsResponse;
import com.digitalpetri.modbus.pdu.ReadDiscreteInputsRequest;
import com.digitalpetri.modbus.pdu.ReadDiscreteInputsResponse;
import com.digitalpetri.modbus.pdu.ReadHoldingRegistersRequest;
import com.digitalpetri.modbus.pdu.ReadHoldingRegistersResponse;
import com.digitalpetri.modbus.pdu.ReadInputRegistersRequest;
import com.digitalpetri.modbus.pdu.ReadInputRegistersResponse;
import com.digitalpetri.modbus.pdu.ReadWriteMultipleRegistersRequest;
import com.digitalpetri.modbus.pdu.ReadWriteMultipleRegistersResponse;
import com.digitalpetri.modbus.pdu.WriteMultipleCoilsRequest;
import com.digitalpetri.modbus.pdu.WriteMultipleCoilsResponse;
import com.digitalpetri.modbus.pdu.WriteMultipleRegistersRequest;
import com.digitalpetri.modbus.pdu.WriteMultipleRegistersResponse;
import com.digitalpetri.modbus.pdu.WriteSingleCoilRequest;
import com.digitalpetri.modbus.pdu.WriteSingleCoilResponse;
import com.digitalpetri.modbus.pdu.WriteSingleRegisterRequest;
import com.digitalpetri.modbus.pdu.WriteSingleRegisterResponse;
import com.digitalpetri.modbus.server.ModbusServer;
import org.junit.jupiter.api.Test;

public abstract class ClientServerIT {

  abstract ModbusClient getClient();

  abstract ModbusServer getServer();

  @Test
  void readCoils() throws Exception {
    ReadCoilsResponse response = getClient().readCoils(1, new ReadCoilsRequest(0, 2000));

    assertEquals(250, response.coils().length);
  }

  @Test
  void readDiscreteInputs() throws Exception {
    ReadDiscreteInputsResponse response =
        getClient().readDiscreteInputs(1, new ReadDiscreteInputsRequest(0, 2000));

    assertEquals(250, response.inputs().length);
  }

  @Test
  void readHoldingRegisters() throws Exception {
    ReadHoldingRegistersResponse response =
        getClient().readHoldingRegisters(1, new ReadHoldingRegistersRequest(0, 125));

    assertEquals(250, response.registers().length);
  }

  @Test
  void readInputRegisters() throws Exception {
    ReadInputRegistersResponse response =
        getClient().readInputRegisters(1, new ReadInputRegistersRequest(0, 125));

    assertEquals(250, response.registers().length);
  }

  @Test
  void writeSingleCoil() throws Exception {
    WriteSingleCoilResponse response =
        getClient().writeSingleCoil(1, new WriteSingleCoilRequest(0, true));

    assertEquals(0, response.address());
    assertEquals(0xFF00, response.value());
  }

  @Test
  void writeSingleRegister() throws Exception {
    WriteSingleRegisterResponse response =
        getClient().writeSingleRegister(1, new WriteSingleRegisterRequest(0, 0x1234));

    assertEquals(0, response.address());
    assertEquals(0x1234, response.value());
  }

  @Test
  void writeMultipleCoils() throws Exception {
    WriteMultipleCoilsResponse response =
        getClient()
            .writeMultipleCoils(1, new WriteMultipleCoilsRequest(0, 8, new byte[] {(byte) 0xFF}));

    assertEquals(0, response.address());
    assertEquals(8, response.quantity());
  }

  @Test
  void writeMultipleRegisters() throws Exception {
    WriteMultipleRegistersResponse response =
        getClient()
            .writeMultipleRegisters(
                1, new WriteMultipleRegistersRequest(0, 1, new byte[] {0x12, 0x34}));

    assertEquals(0, response.address());
    assertEquals(1, response.quantity());
  }

  @Test
  void maskWriteRegister() throws Exception {
    MaskWriteRegisterResponse response =
        getClient().maskWriteRegister(1, new MaskWriteRegisterRequest(0, 0xFF00, 0x00FF));

    assertEquals(0, response.address());
    assertEquals(0xFF00, response.andMask());
    assertEquals(0x00FF, response.orMask());
  }

  @Test
  void readWriteMultipleRegisters() throws Exception {
    ReadWriteMultipleRegistersResponse response =
        getClient()
            .readWriteMultipleRegisters(
                1, new ReadWriteMultipleRegistersRequest(0, 1, 0, 1, new byte[] {0x12, 0x34}));

    byte[] registers = response.registers();
    assertEquals(0x12, registers[0]);
    assertEquals(0x34, registers[1]);
  }

  @Test
  void isConnected() throws ModbusExecutionException {
    assertTrue(getClient().isConnected());

    getClient().disconnect();

    assertFalse(getClient().isConnected());
  }
}

```

---

### `modbus/tests/src/test/java/com/digitalpetri/modbus/test/ModbusSecurityIT.java`

```java
package com.digitalpetri.modbus.test;

import com.digitalpetri.modbus.client.ModbusClient;
import com.digitalpetri.modbus.client.ModbusTcpClient;
import com.digitalpetri.modbus.pdu.ReadCoilsRequest;
import com.digitalpetri.modbus.server.ModbusServer;
import com.digitalpetri.modbus.server.ModbusTcpServer;
import com.digitalpetri.modbus.server.ProcessImage;
import com.digitalpetri.modbus.server.ReadWriteModbusServices;
import com.digitalpetri.modbus.tcp.client.NettyTcpClientTransport;
import com.digitalpetri.modbus.tcp.security.SecurityUtil;
import com.digitalpetri.modbus.tcp.server.NettyTcpServerTransport;
import com.digitalpetri.modbus.test.CertificateUtil.KeyPairCert;
import com.digitalpetri.modbus.test.CertificateUtil.Role;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Optional;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ModbusSecurityIT {

  KeyPairCert authority1Keys = CertificateUtil.generateCaCertificate();
  KeyPairCert authority2Keys = CertificateUtil.generateCaCertificate();

  KeyPairCert client1Keys =
      CertificateUtil.generateCaSignedCertificate(Role.CLIENT, authority1Keys);
  KeyPairCert client2Keys =
      CertificateUtil.generateCaSignedCertificate(Role.CLIENT, authority2Keys);

  KeyPairCert server1Keys =
      CertificateUtil.generateCaSignedCertificate(Role.SERVER, authority1Keys);
  KeyPairCert server2Keys =
      CertificateUtil.generateCaSignedCertificate(Role.SERVER, authority2Keys);

  KeyPairCert selfSignedClientKeys = CertificateUtil.generateSelfSignedCertificate(Role.CLIENT);
  KeyPairCert selfSignedServerKeys = CertificateUtil.generateSelfSignedCertificate(Role.SERVER);

  @Test
  void clientServerMutualTrust() throws Exception {
    ModbusClient client = setupClientWithKeys(client1Keys, authority1Keys);
    ModbusServer server = setupServerWithKeys(server1Keys, authority1Keys);

    server.start();

    Assertions.assertDoesNotThrow(
        () -> {
          client.connect();
          client.readCoils(1, new ReadCoilsRequest(0, 1));
        });

    server.stop();
  }

  @Test
  void clientServerMutualTrust2() throws Exception {
    ModbusClient client = setupClientWithKeys(client2Keys, authority2Keys);
    ModbusServer server = setupServerWithKeys(server2Keys, authority2Keys);

    server.start();

    Assertions.assertDoesNotThrow(
        () -> {
          try {
            client.connect();
            client.readCoils(1, new ReadCoilsRequest(0, 1));
          } finally {
            client.disconnect();
          }
        });

    server.stop();
  }

  @Test
  void clientRejectsUntrustedServerCertificate() throws Exception {
    ModbusClient client = setupClientWithKeys(client1Keys, authority1Keys);
    ModbusServer server = setupServerWithKeys(server2Keys, authority1Keys, authority2Keys);

    server.start();

    Assertions.assertThrows(
        Exception.class,
        () -> {
          try {
            client.connect();
            client.readCoils(1, new ReadCoilsRequest(0, 1));
          } finally {
            client.disconnect();
          }
        });

    server.stop();
  }

  @Test
  void serverRejectsUntrustedClientCertificate() throws Exception {
    ModbusClient client = setupClientWithKeys(client1Keys, authority1Keys, authority2Keys);
    ModbusServer server = setupServerWithKeys(server2Keys, authority2Keys);

    server.start();

    Assertions.assertThrows(
        Exception.class,
        () -> {
          try {
            client.connect();
            client.readCoils(1, new ReadCoilsRequest(0, 1));
          } finally {
            client.disconnect();
          }
        });

    server.stop();
  }

  @Test
  void selfSignedClientAndServer() throws Exception {
    ModbusClient client = setupClientWithKeys(selfSignedClientKeys, selfSignedServerKeys);
    ModbusServer server = setupServerWithKeys(selfSignedServerKeys, selfSignedClientKeys);

    server.start();

    Assertions.assertDoesNotThrow(
        () -> {
          client.connect();
          client.readCoils(1, new ReadCoilsRequest(0, 1));
        });

    server.stop();
  }

  ModbusClient setupClientWithKeys(KeyPairCert clientKeys, KeyPairCert... authorityKeys)
      throws Exception {

    KeyManagerFactory keyManagerFactory =
        SecurityUtil.createKeyManagerFactory(
            clientKeys.keyPair().getPrivate(), clientKeys.certificate());

    TrustManagerFactory trustManagerFactory =
        SecurityUtil.createTrustManagerFactory(
            Arrays.stream(authorityKeys)
                .map(KeyPairCert::certificate)
                .toArray(X509Certificate[]::new));

    var transport =
        NettyTcpClientTransport.create(
            cfg -> {
              cfg.hostname = "localhost";
              cfg.port = 50200;
              cfg.connectPersistent = false;

              cfg.tlsEnabled = true;
              cfg.keyManagerFactory = keyManagerFactory;
              cfg.trustManagerFactory = trustManagerFactory;
            });

    return ModbusTcpClient.create(transport);
  }

  ModbusServer setupServerWithKeys(KeyPairCert serverKeys, KeyPairCert... authorityKeys)
      throws Exception {

    KeyManagerFactory keyManagerFactory =
        SecurityUtil.createKeyManagerFactory(
            serverKeys.keyPair().getPrivate(), serverKeys.certificate());

    TrustManagerFactory trustManagerFactory =
        SecurityUtil.createTrustManagerFactory(
            Arrays.stream(authorityKeys)
                .map(KeyPairCert::certificate)
                .toArray(X509Certificate[]::new));

    var serverTransport =
        NettyTcpServerTransport.create(
            cfg -> {
              cfg.bindAddress = "localhost";
              cfg.port = 50200;

              cfg.tlsEnabled = true;
              cfg.keyManagerFactory = keyManagerFactory;
              cfg.trustManagerFactory = trustManagerFactory;
            });

    var processImage = new ProcessImage();
    var modbusServices =
        new ReadWriteModbusServices() {
          @Override
          protected Optional<ProcessImage> getProcessImage(int unitId) {
            return Optional.of(processImage);
          }
        };

    return ModbusTcpServer.create(serverTransport, modbusServices);
  }
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/ExceptionCode.java`

```java
package com.digitalpetri.modbus;

import java.util.Optional;

public enum ExceptionCode {

  /**
   * Exception Code 0x01 - Illegal Function.
   *
   * <p>The function code received in the query is not an allowable action for the server.
   */
  ILLEGAL_FUNCTION(0x01),

  /**
   * Exception Code 0x02 - Illegal Data Address.
   *
   * <p>The data address received in the query is not an allowable address for the server. More
   * specifically, the combination of reference number and transfer length is invalid.
   */
  ILLEGAL_DATA_ADDRESS(0x02),

  /**
   * Exception Code 0x03 - Illegal Data Value.
   *
   * <p>A value contained in the query data field is not an allowable value for server.
   *
   * <p>This indicates a fault in the structure of the remainder of a complex request, such as that
   * the implied length is incorrect. It specifically does NOT mean that a data item submitted for
   * storage in a register has a value outside the expectation of the application program.
   */
  ILLEGAL_DATA_VALUE(0x03),

  /**
   * Exception Code 0x04 - Slave Device Failure.
   *
   * <p>An unrecoverable error occurred while the server was attempting to perform the requested
   * action.
   */
  SLAVE_DEVICE_FAILURE(0x04),

  /**
   * Exception Code 0x05 - Acknowledge.
   *
   * <p>Specialized use in conjunction with programming commands.
   *
   * <p>The server has accepted the request and is processing it, but a long duration of time will
   * be required to do so. This response is returned to prevent a timeout error from occurring in
   * the client.
   */
  ACKNOWLEDGE(0x05),

  /**
   * Exception Code 0x06 - Slave Device Busy.
   *
   * <p>Specialized use in conjunction with programming commands.
   *
   * <p>The server is engaged in processing a long–duration program command. The client should
   * retransmit the message later when the server is free.
   */
  SLAVE_DEVICE_BUSY(0x06),

  /**
   * Exception Code 0x08 - Memory Parity Error.
   *
   * <p>Specialized use in conjunction with function codes 20 and 21 and reference type 6, to
   * indicate that the extended file area failed to pass a consistency check.
   */
  MEMORY_PARITY_ERROR(0x08),

  /**
   * Exception Code 0x0A - Gateway Path Unavailable.
   *
   * <p>Specialized use in conjunction with gateways, indicates that the gateway was unable to
   * allocate an internal communication path from the input port to the output port for processing
   * the request. Usually means that the gateway is misconfigured or overloaded.
   */
  GATEWAY_PATH_UNAVAILABLE(0x0A),

  /**
   * Exception Code 0x0B - Gateway Target Device Failed to Respond.
   *
   * <p>Specialized use in conjunction with gateways, indicates that no response was obtained from
   * the target device. Usually means that the device is not present on the network.
   */
  GATEWAY_TARGET_DEVICE_FAILED_TO_RESPONSE(0x0B);

  private final int code;

  ExceptionCode(int code) {
    this.code = code;
  }

  public int getCode() {
    return code;
  }

  /**
   * Look up the corresponding {@link ExceptionCode} for {@code code}.
   *
   * @param code the exception code to look up.
   * @return the corresponding {@link ExceptionCode} for {@code code}.
   */
  public static Optional<ExceptionCode> from(int code) {
    ExceptionCode ec =
        switch (code) {
          case 0x01 -> ILLEGAL_FUNCTION;
          case 0x02 -> ILLEGAL_DATA_ADDRESS;
          case 0x03 -> ILLEGAL_DATA_VALUE;
          case 0x04 -> SLAVE_DEVICE_FAILURE;
          case 0x05 -> ACKNOWLEDGE;
          case 0x06 -> SLAVE_DEVICE_BUSY;
          case 0x08 -> MEMORY_PARITY_ERROR;
          case 0x0A -> GATEWAY_PATH_UNAVAILABLE;
          case 0x0B -> GATEWAY_TARGET_DEVICE_FAILED_TO_RESPONSE;
          default -> null;
        };

    return Optional.ofNullable(ec);
  }
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/Crc16.java`

```java
package com.digitalpetri.modbus;

import java.nio.ByteBuffer;

public class Crc16 {

  /** CRC-16/Modbus lookup table. */
  private static final int[] TABLE = {
    0x0000, 0xC0C1, 0xC181, 0x0140, 0xC301, 0x03C0, 0x0280, 0xC241,
    0xC601, 0x06C0, 0x0780, 0xC741, 0x0500, 0xC5C1, 0xC481, 0x0440,
    0xCC01, 0x0CC0, 0x0D80, 0xCD41, 0x0F00, 0xCFC1, 0xCE81, 0x0E40,
    0x0A00, 0xCAC1, 0xCB81, 0x0B40, 0xC901, 0x09C0, 0x0880, 0xC841,
    0xD801, 0x18C0, 0x1980, 0xD941, 0x1B00, 0xDBC1, 0xDA81, 0x1A40,
    0x1E00, 0xDEC1, 0xDF81, 0x1F40, 0xDD01, 0x1DC0, 0x1C80, 0xDC41,
    0x1400, 0xD4C1, 0xD581, 0x1540, 0xD701, 0x17C0, 0x1680, 0xD641,
    0xD201, 0x12C0, 0x1380, 0xD341, 0x1100, 0xD1C1, 0xD081, 0x1040,
    0xF001, 0x30C0, 0x3180, 0xF141, 0x3300, 0xF3C1, 0xF281, 0x3240,
    0x3600, 0xF6C1, 0xF781, 0x3740, 0xF501, 0x35C0, 0x3480, 0xF441,
    0x3C00, 0xFCC1, 0xFD81, 0x3D40, 0xFF01, 0x3FC0, 0x3E80, 0xFE41,
    0xFA01, 0x3AC0, 0x3B80, 0xFB41, 0x3900, 0xF9C1, 0xF881, 0x3840,
    0x2800, 0xE8C1, 0xE981, 0x2940, 0xEB01, 0x2BC0, 0x2A80, 0xEA41,
    0xEE01, 0x2EC0, 0x2F80, 0xEF41, 0x2D00, 0xEDC1, 0xEC81, 0x2C40,
    0xE401, 0x24C0, 0x2580, 0xE541, 0x2700, 0xE7C1, 0xE681, 0x2640,
    0x2200, 0xE2C1, 0xE381, 0x2340, 0xE101, 0x21C0, 0x2080, 0xE041,
    0xA001, 0x60C0, 0x6180, 0xA141, 0x6300, 0xA3C1, 0xA281, 0x6240,
    0x6600, 0xA6C1, 0xA781, 0x6740, 0xA501, 0x65C0, 0x6480, 0xA441,
    0x6C00, 0xACC1, 0xAD81, 0x6D40, 0xAF01, 0x6FC0, 0x6E80, 0xAE41,
    0xAA01, 0x6AC0, 0x6B80, 0xAB41, 0x6900, 0xA9C1, 0xA881, 0x6840,
    0x7800, 0xB8C1, 0xB981, 0x7940, 0xBB01, 0x7BC0, 0x7A80, 0xBA41,
    0xBE01, 0x7EC0, 0x7F80, 0xBF41, 0x7D00, 0xBDC1, 0xBC81, 0x7C40,
    0xB401, 0x74C0, 0x7580, 0xB541, 0x7700, 0xB7C1, 0xB681, 0x7640,
    0x7200, 0xB2C1, 0xB381, 0x7340, 0xB101, 0x71C0, 0x7080, 0xB041,
    0x5000, 0x90C1, 0x9181, 0x5140, 0x9301, 0x53C0, 0x5280, 0x9241,
    0x9601, 0x56C0, 0x5780, 0x9741, 0x5500, 0x95C1, 0x9481, 0x5440,
    0x9C01, 0x5CC0, 0x5D80, 0x9D41, 0x5F00, 0x9FC1, 0x9E81, 0x5E40,
    0x5A00, 0x9AC1, 0x9B81, 0x5B40, 0x9901, 0x59C0, 0x5880, 0x9841,
    0x8801, 0x48C0, 0x4980, 0x8941, 0x4B00, 0x8BC1, 0x8A81, 0x4A40,
    0x4E00, 0x8EC1, 0x8F81, 0x4F40, 0x8D01, 0x4DC0, 0x4C80, 0x8C41,
    0x4400, 0x84C1, 0x8581, 0x4540, 0x8701, 0x47C0, 0x4680, 0x8641,
    0x8201, 0x42C0, 0x4380, 0x8341, 0x4100, 0x81C1, 0x8081, 0x4040
  };

  private int sum = 0xFFFF;

  /**
   * @return the CRC-16 value.
   */
  public int getValue() {
    return sum;
  }

  /** Resets the CRC-16 to the initial value. */
  public void reset() {
    sum = 0xFFFF;
  }

  /**
   * Updates the CRC-16 with the given byte.
   *
   * @param b the byte to update the CRC-16 with.
   */
  public void update(int b) {
    sum = (sum >> 8) ^ TABLE[((sum) ^ (b & 0xFF)) & 0xFF];
  }

  /**
   * Updates the CRC-16 with the given {@link ByteBuffer}.
   *
   * @param buffer the {@link ByteBuffer} to update the CRC-16 with.
   */
  public void update(ByteBuffer buffer) {
    int offset = buffer.position();
    for (int i = offset; i < buffer.limit(); i++) {
      update(buffer.get(offset + i));
    }
  }
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/server/ModbusServer.java`

```java
package com.digitalpetri.modbus.server;

public interface ModbusServer {

  /**
   * Start the server.
   *
   * @throws Exception if the server could not be started.
   */
  void start() throws Exception;

  /**
   * Stop the server.
   *
   * @throws Exception if the server could not be stopped.
   */
  void stop() throws Exception;

  /**
   * Set the {@link ModbusServices} that will be used to handle requests.
   *
   * @param services the {@link ModbusServices} to use.
   */
  void setModbusServices(ModbusServices services);
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/server/ModbusTcpServer.java`

```java
package com.digitalpetri.modbus.server;

import com.digitalpetri.modbus.ExceptionCode;
import com.digitalpetri.modbus.FunctionCode;
import com.digitalpetri.modbus.MbapHeader;
import com.digitalpetri.modbus.ModbusTcpFrame;
import com.digitalpetri.modbus.exceptions.ModbusResponseException;
import com.digitalpetri.modbus.pdu.*;
import com.digitalpetri.modbus.server.ModbusRequestContext.ModbusTcpRequestContext;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class ModbusTcpServer implements ModbusServer {

  private final ModbusServerConfig config;
  private final ModbusTcpServerTransport transport;
  private final AtomicReference<ModbusServices> services =
      new AtomicReference<>(new ModbusServices() {});

  public ModbusTcpServer(
      ModbusServerConfig config, ModbusTcpServerTransport transport, ModbusServices services) {

    this.config = config;
    this.transport = transport;

    this.services.set(services);
  }

  /**
   * Get the {@link ModbusServerConfig} used by this server.
   *
   * @return the {@link ModbusServerConfig} used by this server.
   */
  public ModbusServerConfig getConfig() {
    return config;
  }

  /**
   * Get the {@link ModbusTcpServerTransport} used by this server.
   *
   * @return the {@link ModbusTcpServerTransport} used by this server.
   */
  public ModbusTcpServerTransport getTransport() {
    return transport;
  }

  @Override
  public void start() throws ExecutionException, InterruptedException {
    transport.receive((context, frame) -> handleModbusTcpFrame(frame, context));

    transport.bind().toCompletableFuture().get();
  }

  @Override
  public void stop() throws ExecutionException, InterruptedException {
    transport.unbind().toCompletableFuture().get();
  }

  @Override
  public void setModbusServices(ModbusServices services) {
    if (services == null) {
      throw new NullPointerException("services");
    }
    this.services.set(services);
  }

  protected ModbusTcpFrame handleModbusTcpFrame(
      ModbusTcpFrame frame, ModbusTcpRequestContext context) throws Exception {

    MbapHeader header = frame.header();
    ByteBuffer pdu = frame.pdu();
    int functionCode = pdu.get(pdu.position()) & 0xFF;
    ModbusRequestPdu requestPdu =
        (ModbusRequestPdu) config.requestSerializer().decode(functionCode, pdu);

    return handleModbusRequestPdu(context, header.transactionId(), header.unitId(), requestPdu);
  }

  protected ModbusTcpFrame handleModbusRequestPdu(
      ModbusTcpRequestContext context, int transactionId, int unitId, ModbusRequestPdu requestPdu)
      throws Exception {

    try {
      int fcb = requestPdu.getFunctionCode();
      FunctionCode functionCode = FunctionCode.from(fcb).orElse(null);

      if (functionCode == null) {
        throw new ModbusResponseException(
            requestPdu.getFunctionCode(), ExceptionCode.ILLEGAL_FUNCTION.getCode());
      }

      ModbusServices services = this.services.get();
      assert services != null;

      ModbusResponsePdu response =
          switch (functionCode) {
            case READ_COILS -> {
              if (requestPdu instanceof ReadCoilsRequest request) {
                yield services.readCoils(context, unitId, request);
              } else {
                throw new IllegalArgumentException("expected ReadCoilsRequest");
              }
            }
            case READ_DISCRETE_INPUTS -> {
              if (requestPdu instanceof ReadDiscreteInputsRequest request) {
                yield services.readDiscreteInputs(context, unitId, request);
              } else {
                throw new IllegalArgumentException("expected ReadDiscreteInputsRequest");
              }
            }
            case READ_HOLDING_REGISTERS -> {
              if (requestPdu instanceof ReadHoldingRegistersRequest request) {
                yield services.readHoldingRegisters(context, unitId, request);
              } else {
                throw new IllegalArgumentException("expected ReadHoldingRegistersRequest");
              }
            }
            case READ_INPUT_REGISTERS -> {
              if (requestPdu instanceof ReadInputRegistersRequest request) {
                yield services.readInputRegisters(context, unitId, request);
              } else {
                throw new IllegalArgumentException("expected ReadInputRegistersRequest");
              }
            }
            case WRITE_SINGLE_COIL -> {
              if (requestPdu instanceof WriteSingleCoilRequest request) {
                yield services.writeSingleCoil(context, unitId, request);
              } else {
                throw new IllegalArgumentException("expected WriteSingleCoilRequest");
              }
            }
            case WRITE_SINGLE_REGISTER -> {
              if (requestPdu instanceof WriteSingleRegisterRequest request) {
                yield services.writeSingleRegister(context, unitId, request);
              } else {
                throw new IllegalArgumentException("expected WriteSingleRegisterRequest");
              }
            }
            case WRITE_MULTIPLE_COILS -> {
              if (requestPdu instanceof WriteMultipleCoilsRequest request) {
                yield services.writeMultipleCoils(context, unitId, request);
              } else {
                throw new IllegalArgumentException("expected WriteMultipleCoilsRequest");
              }
            }
            case WRITE_MULTIPLE_REGISTERS -> {
              if (requestPdu instanceof WriteMultipleRegistersRequest request) {
                yield services.writeMultipleRegisters(context, unitId, request);
              } else {
                throw new IllegalArgumentException("expected WriteMultipleRegistersRequest");
              }
            }
            case MASK_WRITE_REGISTER -> {
              if (requestPdu instanceof MaskWriteRegisterRequest request) {
                yield services.maskWriteRegister(context, unitId, request);
              } else {
                throw new IllegalArgumentException("expected MaskWriteRegisterRequest");
              }
            }
            case READ_WRITE_MULTIPLE_REGISTERS -> {
              if (requestPdu instanceof ReadWriteMultipleRegistersRequest request) {
                yield services.readWriteMultipleRegisters(context, unitId, request);
              } else {
                throw new IllegalArgumentException("expected ReadWriteMultipleRegistersRequest");
              }
            }
            default ->
                throw new ModbusResponseException(
                    requestPdu.getFunctionCode(), ExceptionCode.ILLEGAL_FUNCTION.getCode());
          };

      ByteBuffer pdu = ByteBuffer.allocate(256);

      config.responseSerializer().encode(response, pdu);

      var header = new MbapHeader(transactionId, 0, pdu.position() + 1, unitId);

      return new ModbusTcpFrame(header, pdu.flip());
    } catch (ModbusResponseException e) {
      var header = new MbapHeader(transactionId, 0, 3, unitId);
      int fc = e.getFunctionCode() + 0x80;
      int ec = e.getExceptionCode();
      ByteBuffer pdu = ByteBuffer.allocate(2).put((byte) fc).put((byte) ec).flip();

      return new ModbusTcpFrame(header, pdu);
    }
  }

  /**
   * Create a new {@link ModbusTcpServer} with the given {@link ModbusTcpServerTransport} and {@link
   * ModbusServices}.
   *
   * @param transport the {@link ModbusTcpServerTransport} to use.
   * @param modbusServices the {@link ModbusServices} to use.
   * @return a new {@link ModbusTcpServer}.
   */
  public static ModbusTcpServer create(
      ModbusTcpServerTransport transport, ModbusServices modbusServices) {

    return create(transport, modbusServices, b -> {});
  }

  /**
   * Create a new {@link ModbusTcpServer} with the given {@link ModbusTcpServerTransport}, {@link
   * ModbusServices}, and a callback that can be used to configure a {@link
   * ModbusServerConfig.Builder}.
   *
   * @param transport the {@link ModbusTcpServerTransport} to use.
   * @param modbusServices the {@link ModbusServices} to use.
   * @param configure a callback that can be used to configure a {@link ModbusServerConfig.Builder}.
   * @return a new {@link ModbusTcpServer}.
   */
  public static ModbusTcpServer create(
      ModbusTcpServerTransport transport,
      ModbusServices modbusServices,
      Consumer<ModbusServerConfig.Builder> configure) {

    ModbusServerConfig.Builder builder = new ModbusServerConfig.Builder();
    configure.accept(builder);
    return new ModbusTcpServer(builder.build(), transport, modbusServices);
  }
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/server/ModbusRtuServerTransport.java`

```java
package com.digitalpetri.modbus.server;

import com.digitalpetri.modbus.ModbusRtuFrame;
import com.digitalpetri.modbus.server.ModbusRequestContext.ModbusRtuRequestContext;

/**
 * Modbus/RTU server transport; a {@link ModbusServerTransport} that sends and receives {@link
 * ModbusRtuFrame}s.
 */
public interface ModbusRtuServerTransport
    extends ModbusServerTransport<ModbusRtuRequestContext, ModbusRtuFrame> {}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/server/ProcessImage.java`

```java
package com.digitalpetri.modbus.server;

import com.digitalpetri.modbus.internal.util.Hex;
import com.digitalpetri.modbus.server.ProcessImage.Modification.CoilModification;
import com.digitalpetri.modbus.server.ProcessImage.Modification.DiscreteInputModification;
import com.digitalpetri.modbus.server.ProcessImage.Modification.HoldingRegisterModification;
import com.digitalpetri.modbus.server.ProcessImage.Modification.InputRegisterModification;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Function;

public class ProcessImage {

  private static final ThreadLocal<Object> IN_TRANSACTION = new ThreadLocal<>();

  private final ReadWriteLock exclusiveLock = new ReentrantReadWriteLock();

  private final ReadWriteLock coilLock = new ReentrantReadWriteLock();
  private final Map<Integer, Boolean> coilMap = new HashMap<>();

  private final ReadWriteLock discreteInputLock = new ReentrantReadWriteLock();
  private final Map<Integer, Boolean> discreteInputMap = new HashMap<>();

  private final ReadWriteLock holdingRegisterLock = new ReentrantReadWriteLock();
  private final Map<Integer, byte[]> holdingRegisterMap = new HashMap<>();

  private final ReadWriteLock inputRegisterLock = new ReentrantReadWriteLock();
  private final Map<Integer, byte[]> inputRegisterMap = new HashMap<>();

  private final List<ModificationListener> modificationListeners = new CopyOnWriteArrayList<>();

  /**
   * Perform an action using a {@link Transaction} that does not return value.
   *
   * <p>Transactions are only valid during the scope of the provided action.
   *
   * @param action an action to perform using the provided {@link Transaction}. The Transaction is
   *     only valid during the scope of this action.
   */
  public void with(Consumer<Transaction> action) {
    with(false, action);
  }

  /**
   * Perform an action using a {@link Transaction} that does not return value.
   *
   * <p>Transactions are only valid during the scope of the provided action.
   *
   * @param exclusive whether this Transaction should be exclusive, i.e. it is guaranteed to be the
   *     only Transaction running against the ProcessImage.
   * @param action an action to perform using the provided {@link Transaction}. The Transaction is
   *     only valid during the scope of this action.
   */
  public void with(boolean exclusive, Consumer<Transaction> action) {
    get(
        exclusive,
        tx -> {
          action.accept(tx);
          return null;
        });
  }

  /**
   * Perform an action using a {@link Transaction} that returns value.
   *
   * <p>Transactions are only valid during the scope of the provided action.
   *
   * @param action the action to perform using the provided {@link Transaction}. The Transaction is
   *     only valid during the scope of this action.
   * @param <T> the return type of the action.
   * @return the return value.
   */
  public <T> T get(Function<Transaction, T> action) {
    return get(false, action);
  }

  /**
   * Perform an action using a {@link Transaction} that returns value.
   *
   * <p>Transactions are only valid during the scope of the provided action.
   *
   * @param exclusive whether this Transaction should be exclusive, i.e. it is guaranteed to be the
   *     only Transaction running against the ProcessImage.
   * @param action the action to perform using the provided {@link Transaction}. The Transaction is
   *     only valid during the scope of this action.
   * @param <T> the return type of the action.
   * @return the return value.
   */
  public <T> T get(boolean exclusive, Function<Transaction, T> action) {
    if (IN_TRANSACTION.get() != null) {
      throw new IllegalStateException("nested transaction");
    } else {
      IN_TRANSACTION.set(new Object());
    }

    if (exclusive) {
      exclusiveLock.writeLock().lock();
    } else {
      exclusiveLock.readLock().lock();
    }
    try {
      try (var tx = new Transaction()) {
        return action.apply(tx);
      }
    } finally {
      if (exclusive) {
        exclusiveLock.writeLock().unlock();
      } else {
        exclusiveLock.readLock().unlock();
      }
      IN_TRANSACTION.remove();
    }
  }

  /**
   * Add a {@link ModificationListener} to be notified when the ProcessImage is modified.
   *
   * @param listener the listener to add.
   */
  public void addModificationListener(ModificationListener listener) {
    modificationListeners.add(listener);
  }

  /**
   * Remove a {@link ModificationListener}.
   *
   * @param listener the listener to remove.
   */
  public void removeModificationListener(ModificationListener listener) {
    modificationListeners.remove(listener);
  }

  public class Transaction implements AutoCloseable {

    private enum State {
      OPEN,
      CLOSED
    }

    private final AtomicReference<State> state = new AtomicReference<>(State.OPEN);

    /**
     * Provide a function that reads from an unmodifiable view of the Coils in the ProcessImage.
     *
     * @param read the function that reads the Coils.
     * @param <T> the return type of the function.
     * @return the result of the read function.
     */
    public <T> T readCoils(Function<Map<Integer, Boolean>, T> read) {
      if (state.get() != State.OPEN) {
        throw new IllegalStateException("transaction closed");
      }

      coilLock.readLock().lock();
      try {
        return read.apply(Collections.unmodifiableMap(coilMap));
      } finally {
        coilLock.readLock().unlock();
      }
    }

    /**
     * Provide a function that reads from an unmodifiable view of the Discrete Inputs in the
     * ProcessImage.
     *
     * @param read the function that reads the Discrete Inputs.
     * @param <T> the return type of the function.
     * @return the result of the read function.
     */
    public <T> T readDiscreteInputs(Function<Map<Integer, Boolean>, T> read) {
      if (state.get() != State.OPEN) {
        throw new IllegalStateException("transaction closed");
      }

      discreteInputLock.readLock().lock();
      try {
        return read.apply(Collections.unmodifiableMap(discreteInputMap));
      } finally {
        discreteInputLock.readLock().unlock();
      }
    }

    /**
     * Provide a function that reads from an unmodifiable view of the Holding Registers in the
     * ProcessImage.
     *
     * @param read the function that reads the Holding Registers.
     * @param <T> the return type of the function.
     * @return the result of the read function.
     */
    public <T> T readHoldingRegisters(Function<Map<Integer, byte[]>, T> read) {
      if (state.get() != State.OPEN) {
        throw new IllegalStateException("transaction closed");
      }

      holdingRegisterLock.readLock().lock();
      try {
        return read.apply(Collections.unmodifiableMap(holdingRegisterMap));
      } finally {
        holdingRegisterLock.readLock().unlock();
      }
    }

    /**
     * Provide a function that reads from an unmodifiable view of the Input Registers in the
     * ProcessImage.
     *
     * @param read the function that reads the Input Registers.
     * @param <T> the return type of the function.
     * @return the result of the read function.
     */
    public <T> T readInputRegisters(Function<Map<Integer, byte[]>, T> read) {
      if (state.get() != State.OPEN) {
        throw new IllegalStateException("transaction closed");
      }

      inputRegisterLock.readLock().lock();
      try {
        return read.apply(Collections.unmodifiableMap(inputRegisterMap));
      } finally {
        inputRegisterLock.readLock().unlock();
      }
    }

    /**
     * Provide a callback that can write to mutable view of the Coils in the ProcessImage.
     *
     * @param write the callback that can write to the Coils.
     */
    public void writeCoils(Consumer<Map<Integer, Boolean>> write) {
      if (state.get() != State.OPEN) {
        throw new IllegalStateException("transaction closed");
      }

      var modifications = new ArrayList<CoilModification>();

      coilLock.writeLock().lock();
      try {
        write.accept(
            new TransactionScopedMap<>(coilMap, state) {
              @Override
              protected void recordPut(Integer key, Boolean value) {
                modifications.add(new CoilModification(key, value));
              }

              @Override
              protected void recordRemove(Object key) {
                if (key instanceof Integer k) {
                  modifications.add(new CoilModification(k, false));
                }
              }
            });

        notifyCoilsModified(modifications);
      } finally {
        coilLock.writeLock().unlock();
      }
    }

    /**
     * Provide a callback that can write to mutable view of the Discrete Inputs in the ProcessImage.
     *
     * @param write the callback that can write to the Discrete Inputs.
     */
    public void writeDiscreteInputs(Consumer<Map<Integer, Boolean>> write) {
      if (state.get() != State.OPEN) {
        throw new IllegalStateException("transaction closed");
      }

      var modifications = new ArrayList<DiscreteInputModification>();

      discreteInputLock.writeLock().lock();
      try {
        write.accept(
            new TransactionScopedMap<>(discreteInputMap, state) {
              @Override
              protected void recordPut(Integer key, Boolean value) {
                modifications.add(new DiscreteInputModification(key, value));
              }

              @Override
              protected void recordRemove(Object key) {
                if (key instanceof Integer k) {
                  modifications.add(new DiscreteInputModification(k, false));
                }
              }
            });

        notifyDiscreteInputsModified(modifications);
      } finally {
        discreteInputLock.writeLock().unlock();
      }
    }

    /**
     * Provide a callback that can write to mutable view of the Holding Registers in the
     * ProcessImage.
     *
     * @param write the callback that can write to the Holding Registers.
     */
    public void writeHoldingRegisters(Consumer<Map<Integer, byte[]>> write) {
      if (state.get() != State.OPEN) {
        throw new IllegalStateException("transaction closed");
      }

      var modifications = new ArrayList<HoldingRegisterModification>();

      holdingRegisterLock.writeLock().lock();
      try {
        write.accept(
            new TransactionScopedMap<>(holdingRegisterMap, state) {
              @Override
              protected void recordPut(Integer key, byte[] value) {
                modifications.add(new HoldingRegisterModification(key, value));
              }

              @Override
              protected void recordRemove(Object key) {
                if (key instanceof Integer k) {
                  modifications.add(new HoldingRegisterModification(k, new byte[2]));
                }
              }
            });

        notifyHoldingRegistersModified(modifications);
      } finally {
        holdingRegisterLock.writeLock().unlock();
      }
    }

    /**
     * Provide a callback that can write to mutable view of the Input Registers in the ProcessImage.
     *
     * @param write the callback that can write to the Input Registers.
     */
    public void writeInputRegisters(Consumer<Map<Integer, byte[]>> write) {
      if (state.get() != State.OPEN) {
        throw new IllegalStateException("transaction closed");
      }

      var modifications = new ArrayList<InputRegisterModification>();

      inputRegisterLock.writeLock().lock();
      try {
        write.accept(
            new TransactionScopedMap<>(inputRegisterMap, state) {
              @Override
              protected void recordPut(Integer key, byte[] value) {
                modifications.add(new InputRegisterModification(key, value));
              }

              @Override
              protected void recordRemove(Object key) {
                if (key instanceof Integer k) {
                  modifications.add(new InputRegisterModification(k, new byte[2]));
                }
              }
            });

        notifyInputRegistersModified(modifications);
      } finally {
        inputRegisterLock.writeLock().unlock();
      }
    }

    @Override
    public void close() {
      state.set(State.CLOSED);
    }

    private void notifyCoilsModified(List<CoilModification> modifications) {
      if (!modifications.isEmpty()) {
        modificationListeners.forEach(listener -> listener.onCoilsModified(modifications));
      }
    }

    private void notifyDiscreteInputsModified(List<DiscreteInputModification> modifications) {
      if (!modifications.isEmpty()) {
        modificationListeners.forEach(listener -> listener.onDiscreteInputsModified(modifications));
      }
    }

    private void notifyHoldingRegistersModified(List<HoldingRegisterModification> modifications) {
      if (!modifications.isEmpty()) {
        modificationListeners.forEach(
            listener -> listener.onHoldingRegistersModified(modifications));
      }
    }

    private void notifyInputRegistersModified(List<InputRegisterModification> modifications) {
      if (!modifications.isEmpty()) {
        modificationListeners.forEach(listener -> listener.onInputRegistersModified(modifications));
      }
    }

    private abstract static class TransactionScopedMap<K, V> extends AbstractMap<K, V> {

      private final Map<K, V> delegate;
      private final AtomicReference<State> state;

      public TransactionScopedMap(Map<K, V> delegate, AtomicReference<State> state) {
        this.delegate = delegate;
        this.state = state;
      }

      @Override
      public Set<Entry<K, V>> entrySet() {
        if (state.get() != State.OPEN) {
          throw new IllegalStateException("transaction closed");
        }
        return delegate.entrySet();
      }

      @Override
      public V put(K key, V value) {
        if (state.get() != State.OPEN) {
          throw new IllegalStateException("transaction closed");
        }
        try {
          return delegate.put(key, value);
        } finally {
          recordPut(key, value);
        }
      }

      @Override
      public V remove(Object key) {
        if (state.get() != State.OPEN) {
          throw new IllegalStateException("transaction closed");
        }
        try {
          return super.remove(key);
        } finally {
          recordRemove(key);
        }
      }

      protected abstract void recordPut(K key, V value);

      protected abstract void recordRemove(Object key);
    }
  }

  public sealed interface Modification
      permits CoilModification,
          DiscreteInputModification,
          HoldingRegisterModification,
          InputRegisterModification {

    record CoilModification(int address, boolean value) implements Modification {}

    record DiscreteInputModification(int address, boolean value) implements Modification {}

    record HoldingRegisterModification(int address, byte[] value) implements Modification {

      @Override
      public String toString() {
        return new StringJoiner(", ", HoldingRegisterModification.class.getSimpleName() + "[", "]")
            .add("address=" + address)
            .add("value=0x" + Hex.format(value))
            .toString();
      }
    }

    record InputRegisterModification(int address, byte[] value) implements Modification {

      @Override
      public String toString() {
        return new StringJoiner(", ", InputRegisterModification.class.getSimpleName() + "[", "]")
            .add("address=" + address)
            .add("value=0x" + Hex.format(value))
            .toString();
      }
    }
  }

  public interface ModificationListener {

    /**
     * Coils in the ProcessImage have been modified.
     *
     * <p>This callback is made while holding the write lock for the modified area. Consider
     * queueing and processing asynchronously if the listener needs to block.
     *
     * @param modifications the list of {@link CoilModification}s that were applied.
     */
    void onCoilsModified(List<CoilModification> modifications);

    /**
     * Discrete Inputs in the ProcessImage have been modified.
     *
     * <p>This callback is made while holding the write lock for the modified area. Consider
     * queueing and processing asynchronously if the listener needs to block.
     *
     * @param modifications the list of {@link DiscreteInputModification}s that were applied.
     */
    void onDiscreteInputsModified(List<DiscreteInputModification> modifications);

    /**
     * Holding Registers in the ProcessImage have been modified.
     *
     * <p>This callback is made while holding the write lock for the modified area. Consider
     * queueing and processing asynchronously if the listener needs to block.
     *
     * @param modifications the list of {@link HoldingRegisterModification}s that were applied.
     */
    void onHoldingRegistersModified(List<HoldingRegisterModification> modifications);

    /**
     * Input Registers in the ProcessImage have been modified.
     *
     * <p>This callback is made while holding the write lock for the modified area. Consider
     * queueing and processing asynchronously if the listener needs to block.
     *
     * @param modifications the list of {@link InputRegisterModification}s that were applied.
     */
    void onInputRegistersModified(List<InputRegisterModification> modifications);
  }
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/server/ModbusServerConfig.java`

```java
package com.digitalpetri.modbus.server;

import com.digitalpetri.modbus.ModbusPduSerializer;

import java.util.function.Consumer;

import static com.digitalpetri.modbus.ModbusPduSerializer.DefaultRequestSerializer;
import static com.digitalpetri.modbus.ModbusPduSerializer.DefaultResponseSerializer;

/**
 * Configuration for a {@link ModbusTcpServer}.
 *
 * @param requestSerializer the {@link ModbusPduSerializer} used to decode incoming requests.
 * @param responseSerializer the {@link ModbusPduSerializer} used to encode outgoing responses.
 */
public record ModbusServerConfig(
    ModbusPduSerializer requestSerializer, ModbusPduSerializer responseSerializer) {

  /**
   * Create a new {@link ModbusServerConfig} instance.
   *
   * @param configure a callback that accepts a {@link Builder} used to configure the new instance.
   * @return a new {@link ModbusServerConfig} instance.
   */
  public static ModbusServerConfig create(Consumer<Builder> configure) {
    var builder = new Builder();
    configure.accept(builder);
    return builder.build();
  }

  public static class Builder {

    /** The {@link ModbusPduSerializer} used to decode incoming requests. */
    public ModbusPduSerializer requestSerializer = DefaultRequestSerializer.INSTANCE;

    /** The {@link ModbusPduSerializer} used to encode outgoing responses. */
    public ModbusPduSerializer responseSerializer = DefaultResponseSerializer.INSTANCE;

    /**
     * Set the {@link ModbusPduSerializer} used to decode incoming requests.
     *
     * @param requestSerializer the request serializer.
     * @return this {@link Builder}.
     */
    public Builder setRequestSerializer(ModbusPduSerializer requestSerializer) {
      this.requestSerializer = requestSerializer;
      return this;
    }

    /**
     * Set the {@link ModbusPduSerializer} used to encode outgoing responses.
     *
     * @param responseSerializer the response serializer.
     * @return this {@link Builder}.
     */
    public Builder setResponseSerializer(ModbusPduSerializer responseSerializer) {
      this.responseSerializer = responseSerializer;
      return this;
    }

    /**
     * @return a new {@link ModbusServerConfig} instance.
     */
    public ModbusServerConfig build() {
      return new ModbusServerConfig(requestSerializer, responseSerializer);
    }
  }
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/server/ReadWriteModbusServices.java`

```java
package com.digitalpetri.modbus.server;

import com.digitalpetri.modbus.exceptions.UnknownUnitIdException;
import com.digitalpetri.modbus.pdu.*;

public abstract class ReadWriteModbusServices extends ReadOnlyModbusServices
    implements ModbusServices {

  @Override
  public WriteSingleCoilResponse writeSingleCoil(
      ModbusRequestContext context, int unitId, WriteSingleCoilRequest request)
      throws UnknownUnitIdException {

    ProcessImage processImage =
        getProcessImage(unitId).orElseThrow(() -> new UnknownUnitIdException(unitId));

    final int address = request.address();
    final int value = request.value();

    processImage.with(
        tx ->
            tx.writeCoils(
                coilMap -> {
                  if (value == 0) {
                    coilMap.remove(address);
                  } else {
                    coilMap.put(address, true);
                  }
                }));

    return new WriteSingleCoilResponse(address, value);
  }

  @Override
  public WriteMultipleCoilsResponse writeMultipleCoils(
      ModbusRequestContext context, int unitId, WriteMultipleCoilsRequest request)
      throws UnknownUnitIdException {

    ProcessImage processImage =
        getProcessImage(unitId).orElseThrow(() -> new UnknownUnitIdException(unitId));

    final int address = request.address();
    final int quantity = request.quantity();
    final byte[] values = request.values();

    processImage.with(
        tx ->
            tx.writeCoils(
                coilMap -> {
                  for (int i = 0; i < quantity; i++) {
                    int vi = i / 8;
                    int bi = i % 8;
                    boolean value = (values[vi] & (1 << bi)) != 0;
                    if (!value) {
                      coilMap.remove(address + i);
                    } else {
                      coilMap.put(address + i, true);
                    }
                  }
                }));

    return new WriteMultipleCoilsResponse(address, quantity);
  }

  @Override
  public WriteSingleRegisterResponse writeSingleRegister(
      ModbusRequestContext context, int unitId, WriteSingleRegisterRequest request)
      throws UnknownUnitIdException {

    ProcessImage processImage =
        getProcessImage(unitId).orElseThrow(() -> new UnknownUnitIdException(unitId));

    final int address = request.address();
    final int value = request.value();

    processImage.with(
        tx ->
            tx.writeHoldingRegisters(
                registerMap -> {
                  if (value == 0) {
                    registerMap.remove(address);
                  } else {
                    byte high = (byte) ((value >> 8) & 0xFF);
                    byte low = (byte) (value & 0xFF);
                    byte[] bs = new byte[] {high, low};
                    registerMap.put(address, bs);
                  }
                }));

    return new WriteSingleRegisterResponse(address, value);
  }

  @Override
  public WriteMultipleRegistersResponse writeMultipleRegisters(
      ModbusRequestContext context, int unitId, WriteMultipleRegistersRequest request)
      throws UnknownUnitIdException {

    ProcessImage processImage =
        getProcessImage(unitId).orElseThrow(() -> new UnknownUnitIdException(unitId));

    final int address = request.address();
    final int quantity = request.quantity();
    final byte[] values = request.values();

    processImage.with(
        tx ->
            tx.writeHoldingRegisters(
                registerMap -> {
                  for (int i = 0; i < quantity; i++) {
                    byte high = values[i * 2];
                    byte low = values[i * 2 + 1];

                    if (high == 0 && low == 0) {
                      registerMap.remove(address + i);
                    } else {
                      byte[] value = new byte[] {high, low};
                      registerMap.put(address + i, value);
                    }
                  }
                }));

    return new WriteMultipleRegistersResponse(address, quantity);
  }

  @Override
  public MaskWriteRegisterResponse maskWriteRegister(
      ModbusRequestContext context, int unitId, MaskWriteRegisterRequest request)
      throws UnknownUnitIdException {

    ProcessImage processImage =
        getProcessImage(unitId).orElseThrow(() -> new UnknownUnitIdException(unitId));

    // Result = (Current Contents AND And_Mask) OR (Or_Mask AND (NOT And_Mask))
    final int address = request.address();
    final int andMask = request.andMask();
    final int orMask = request.orMask();

    processImage.with(
        tx ->
            tx.writeHoldingRegisters(
                registerMap -> {
                  byte[] value = registerMap.getOrDefault(address, new byte[2]);
                  int currentValue = (value[0] << 8) | (value[1] & 0xFF);
                  int result = (currentValue & andMask) | (orMask & ~andMask);

                  if (result == 0) {
                    registerMap.remove(address);
                  } else {
                    byte high = (byte) ((result >> 8) & 0xFF);
                    byte low = (byte) (result & 0xFF);
                    byte[] bs = new byte[] {high, low};
                    registerMap.put(address, bs);
                  }
                }));

    return new MaskWriteRegisterResponse(address, andMask, orMask);
  }

  @Override
  public ReadWriteMultipleRegistersResponse readWriteMultipleRegisters(
      ModbusRequestContext context, int unitId, ReadWriteMultipleRegistersRequest request)
      throws UnknownUnitIdException {

    ProcessImage processImage =
        getProcessImage(unitId).orElseThrow(() -> new UnknownUnitIdException(unitId));

    final int readAddress = request.readAddress();
    final int readQuantity = request.readQuantity();
    final int writeAddress = request.writeAddress();
    final int writeQuantity = request.writeQuantity();
    final byte[] values = request.values();

    byte[] registers =
        processImage.get(
            tx -> {
              tx.writeHoldingRegisters(
                  registerMap -> {
                    for (int i = 0; i < writeQuantity; i++) {
                      byte high = values[i * 2];
                      byte low = values[i * 2 + 1];

                      if (high == 0 && low == 0) {
                        registerMap.remove(writeAddress + i);
                      } else {
                        byte[] value = new byte[] {high, low};
                        registerMap.put(writeAddress + i, value);
                      }
                    }
                  });

              return tx.readHoldingRegisters(readRegisters(readAddress, readQuantity));
            });

    return new ReadWriteMultipleRegistersResponse(registers);
  }
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/server/authz/AuthzHandler.java`

```java
package com.digitalpetri.modbus.server.authz;

import com.digitalpetri.modbus.pdu.*;

/** Callback interface that handles authorization of Modbus operations. */
public interface AuthzHandler {

  /**
   * Authorizes the reading of coils.
   *
   * @param authzContext the {@link AuthzContext}.
   * @param unitId the unit identifier of the Modbus device.
   * @param request the {@link ReadCoilsRequest} being authorized.
   * @return the result of the authorization check.
   */
  AuthzResult authorizeReadCoils(AuthzContext authzContext, int unitId, ReadCoilsRequest request);

  /**
   * Authorizes the reading of discrete inputs.
   *
   * @param authzContext the {@link AuthzContext}.
   * @param unitId the unit identifier of the Modbus device.
   * @param request the {@link ReadDiscreteInputsRequest} being authorized.
   * @return the result of the authorization check.
   */
  AuthzResult authorizeReadDiscreteInputs(
      AuthzContext authzContext, int unitId, ReadDiscreteInputsRequest request);

  /**
   * Authorizes the reading of holding registers.
   *
   * @param authzContext the {@link AuthzContext}.
   * @param unitId the unit identifier of the Modbus device.
   * @param request the {@link ReadHoldingRegistersRequest} being authorized.
   * @return the result of the authorization check.
   */
  AuthzResult authorizeReadHoldingRegisters(
      AuthzContext authzContext, int unitId, ReadHoldingRegistersRequest request);

  /**
   * Authorizes the reading of input registers.
   *
   * @param authzContext the {@link AuthzContext}.
   * @param unitId the unit identifier of the Modbus device.
   * @param request the {@link ReadInputRegistersRequest} being authorized.
   * @return the result of the authorization check.
   */
  AuthzResult authorizeReadInputRegisters(
      AuthzContext authzContext, int unitId, ReadInputRegistersRequest request);

  /**
   * Authorizes the writing of a single coil.
   *
   * @param authzContext the {@link AuthzContext}.
   * @param unitId the unit identifier of the Modbus device.
   * @param request the {@link WriteSingleCoilRequest} being authorized.
   * @return the result of the authorization check.
   */
  AuthzResult authorizeWriteSingleCoil(
      AuthzContext authzContext, int unitId, WriteSingleCoilRequest request);

  /**
   * Authorizes the writing of a single register.
   *
   * @param authzContext the {@link AuthzContext}.
   * @param unitId the unit identifier of the Modbus device.
   * @param request the {@link WriteSingleRegisterRequest} being authorized.
   * @return the result of the authorization check.
   */
  AuthzResult authorizeWriteSingleRegister(
      AuthzContext authzContext, int unitId, WriteSingleRegisterRequest request);

  /**
   * Authorizes the writing of multiple coils.
   *
   * @param authzContext the {@link AuthzContext}.
   * @param unitId the unit identifier of the Modbus device.
   * @param request the {@link WriteMultipleCoilsRequest} being authorized.
   * @return the result of the authorization check.
   */
  AuthzResult authorizeWriteMultipleCoils(
      AuthzContext authzContext, int unitId, WriteMultipleCoilsRequest request);

  /**
   * Authorizes the writing of multiple registers.
   *
   * @param authzContext the {@link AuthzContext}.
   * @param unitId the unit identifier of the Modbus device.
   * @param request the {@link WriteMultipleRegistersRequest} being authorized.
   * @return the result of the authorization check.
   */
  AuthzResult authorizeWriteMultipleRegisters(
      AuthzContext authzContext, int unitId, WriteMultipleRegistersRequest request);

  /**
   * Authorizes the mask write register operation.
   *
   * @param authzContext the {@link AuthzContext}.
   * @param unitId the unit identifier of the Modbus device.
   * @param request the {@link MaskWriteRegisterRequest} being authorized.
   * @return the result of the authorization check.
   */
  AuthzResult authorizeMaskWriteRegister(
      AuthzContext authzContext, int unitId, MaskWriteRegisterRequest request);

  /**
   * Authorizes the read-write-multiple registers operation.
   *
   * @param authzContext the {@link AuthzContext}.
   * @param unitId the unit identifier of the Modbus device.
   * @param request the {@link ReadWriteMultipleRegistersRequest} being authorized.
   * @return the result of the authorization check.
   */
  AuthzResult authorizeReadWriteMultipleRegisters(
      AuthzContext authzContext, int unitId, ReadWriteMultipleRegistersRequest request);

  /** Enumeration representing the result of an authorization check. */
  enum AuthzResult {

    /** Indicates that the operation is authorized. */
    AUTHORIZED,

    /** Indicates that the operation is not authorized. */
    NOT_AUTHORIZED
  }
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/server/authz/AuthzModbusServices.java`

```java
package com.digitalpetri.modbus.server.authz;

import com.digitalpetri.modbus.ExceptionCode;
import com.digitalpetri.modbus.FunctionCode;
import com.digitalpetri.modbus.exceptions.ModbusResponseException;
import com.digitalpetri.modbus.exceptions.UnknownUnitIdException;
import com.digitalpetri.modbus.pdu.*;
import com.digitalpetri.modbus.server.ModbusRequestContext;
import com.digitalpetri.modbus.server.ModbusServices;
import com.digitalpetri.modbus.server.authz.AuthzHandler.AuthzResult;

/**
 * A {@link ModbusServices} implementation that delegates to another {@link ModbusServices} instance
 * after performing authorization checks.
 */
public class AuthzModbusServices implements ModbusServices {

  private final AuthzHandler authzHandler;
  private final ModbusServices modbusServices;

  /**
   * Create a new {@link AuthzModbusServices} instance.
   *
   * @param authzHandler the {@link AuthzHandler} to use for authorization checks.
   * @param modbusServices the {@link ModbusServices} to delegate to.
   */
  public AuthzModbusServices(AuthzHandler authzHandler, ModbusServices modbusServices) {
    this.authzHandler = authzHandler;
    this.modbusServices = modbusServices;
  }

  @Override
  public ReadCoilsResponse readCoils(
      ModbusRequestContext context, int unitId, ReadCoilsRequest request)
      throws ModbusResponseException, UnknownUnitIdException {

    if (context instanceof AuthzContext ctx) {
      AuthzResult result = authzHandler.authorizeReadCoils(ctx, unitId, request);

      if (result != AuthzResult.AUTHORIZED) {
        throw new ModbusResponseException(FunctionCode.READ_COILS, ExceptionCode.ILLEGAL_FUNCTION);
      }
    }

    return modbusServices.readCoils(context, unitId, request);
  }

  @Override
  public ReadDiscreteInputsResponse readDiscreteInputs(
      ModbusRequestContext context, int unitId, ReadDiscreteInputsRequest request)
      throws ModbusResponseException, UnknownUnitIdException {

    if (context instanceof AuthzContext ctx) {
      AuthzResult result = authzHandler.authorizeReadDiscreteInputs(ctx, unitId, request);

      if (result != AuthzResult.AUTHORIZED) {
        throw new ModbusResponseException(
            FunctionCode.READ_DISCRETE_INPUTS, ExceptionCode.ILLEGAL_FUNCTION);
      }
    }

    return modbusServices.readDiscreteInputs(context, unitId, request);
  }

  @Override
  public ReadHoldingRegistersResponse readHoldingRegisters(
      ModbusRequestContext context, int unitId, ReadHoldingRegistersRequest request)
      throws ModbusResponseException, UnknownUnitIdException {

    if (context instanceof AuthzContext ctx) {
      AuthzResult result = authzHandler.authorizeReadHoldingRegisters(ctx, unitId, request);

      if (result != AuthzResult.AUTHORIZED) {
        throw new ModbusResponseException(
            FunctionCode.READ_HOLDING_REGISTERS, ExceptionCode.ILLEGAL_FUNCTION);
      }
    }

    return modbusServices.readHoldingRegisters(context, unitId, request);
  }

  @Override
  public ReadInputRegistersResponse readInputRegisters(
      ModbusRequestContext context, int unitId, ReadInputRegistersRequest request)
      throws ModbusResponseException, UnknownUnitIdException {

    if (context instanceof AuthzContext ctx) {
      AuthzResult result = authzHandler.authorizeReadInputRegisters(ctx, unitId, request);

      if (result != AuthzResult.AUTHORIZED) {
        throw new ModbusResponseException(
            FunctionCode.READ_INPUT_REGISTERS, ExceptionCode.ILLEGAL_FUNCTION);
      }
    }

    return modbusServices.readInputRegisters(context, unitId, request);
  }

  @Override
  public WriteSingleCoilResponse writeSingleCoil(
      ModbusRequestContext context, int unitId, WriteSingleCoilRequest request)
      throws ModbusResponseException, UnknownUnitIdException {

    if (context instanceof AuthzContext ctx) {
      AuthzResult result = authzHandler.authorizeWriteSingleCoil(ctx, unitId, request);

      if (result != AuthzResult.AUTHORIZED) {
        throw new ModbusResponseException(
            FunctionCode.WRITE_SINGLE_COIL, ExceptionCode.ILLEGAL_FUNCTION);
      }
    }

    return modbusServices.writeSingleCoil(context, unitId, request);
  }

  @Override
  public WriteSingleRegisterResponse writeSingleRegister(
      ModbusRequestContext context, int unitId, WriteSingleRegisterRequest request)
      throws ModbusResponseException, UnknownUnitIdException {

    if (context instanceof AuthzContext ctx) {
      AuthzResult result = authzHandler.authorizeWriteSingleRegister(ctx, unitId, request);

      if (result != AuthzResult.AUTHORIZED) {
        throw new ModbusResponseException(
            FunctionCode.WRITE_SINGLE_REGISTER, ExceptionCode.ILLEGAL_FUNCTION);
      }
    }

    return modbusServices.writeSingleRegister(context, unitId, request);
  }

  @Override
  public WriteMultipleCoilsResponse writeMultipleCoils(
      ModbusRequestContext context, int unitId, WriteMultipleCoilsRequest request)
      throws ModbusResponseException, UnknownUnitIdException {

    if (context instanceof AuthzContext ctx) {
      AuthzResult result = authzHandler.authorizeWriteMultipleCoils(ctx, unitId, request);

      if (result != AuthzResult.AUTHORIZED) {
        throw new ModbusResponseException(
            FunctionCode.WRITE_MULTIPLE_COILS, ExceptionCode.ILLEGAL_FUNCTION);
      }
    }

    return modbusServices.writeMultipleCoils(context, unitId, request);
  }

  @Override
  public WriteMultipleRegistersResponse writeMultipleRegisters(
      ModbusRequestContext context, int unitId, WriteMultipleRegistersRequest request)
      throws ModbusResponseException, UnknownUnitIdException {

    if (context instanceof AuthzContext ctx) {
      AuthzResult result = authzHandler.authorizeWriteMultipleRegisters(ctx, unitId, request);

      if (result != AuthzResult.AUTHORIZED) {
        throw new ModbusResponseException(
            FunctionCode.WRITE_MULTIPLE_REGISTERS, ExceptionCode.ILLEGAL_FUNCTION);
      }
    }

    return modbusServices.writeMultipleRegisters(context, unitId, request);
  }

  @Override
  public MaskWriteRegisterResponse maskWriteRegister(
      ModbusRequestContext context, int unitId, MaskWriteRegisterRequest request)
      throws ModbusResponseException, UnknownUnitIdException {

    if (context instanceof AuthzContext ctx) {
      AuthzResult result = authzHandler.authorizeMaskWriteRegister(ctx, unitId, request);

      if (result != AuthzResult.AUTHORIZED) {
        throw new ModbusResponseException(
            FunctionCode.MASK_WRITE_REGISTER, ExceptionCode.ILLEGAL_FUNCTION);
      }
    }

    return modbusServices.maskWriteRegister(context, unitId, request);
  }

  @Override
  public ReadWriteMultipleRegistersResponse readWriteMultipleRegisters(
      ModbusRequestContext context, int unitId, ReadWriteMultipleRegistersRequest request)
      throws ModbusResponseException, UnknownUnitIdException {

    if (context instanceof AuthzContext ctx) {
      AuthzResult result = authzHandler.authorizeReadWriteMultipleRegisters(ctx, unitId, request);

      if (result != AuthzResult.AUTHORIZED) {
        throw new ModbusResponseException(
            FunctionCode.READ_WRITE_MULTIPLE_REGISTERS, ExceptionCode.ILLEGAL_FUNCTION);
      }
    }

    return modbusServices.readWriteMultipleRegisters(context, unitId, request);
  }
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/server/authz/AuthzContext.java`

```java
package com.digitalpetri.modbus.server.authz;

import java.security.cert.X509Certificate;
import java.util.Optional;

public interface AuthzContext {

  /**
   * Get the role of the client attempting the operation, if available.
   *
   * @return the role of the client attempting the operation, if available.
   */
  Optional<String> clientRole();

  /**
   * Get the client certificate chain.
   *
   * @return the client certificate chain.
   */
  X509Certificate[] clientCertificateChain();
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/server/authz/ReadWriteAuthzHandler.java`

```java
package com.digitalpetri.modbus.server.authz;

import com.digitalpetri.modbus.pdu.*;

/**
 * A simplified {@link AuthzHandler} that determines authorization based on whether the client has
 * read or write access to a given unit id.
 *
 * <p>Subclasses must implement {@link #authorizeRead(int, AuthzContext)} and {@link
 * #authorizeWrite(int, AuthzContext)}. The default implementations of the read and write methods
 * will call these methods to determine authorization.
 *
 * <p>Operations that require read authorization:
 *
 * <ul>
 *   <li>ReadCoils
 *   <li>ReadDiscreteInputs
 *   <li>ReadHoldingRegisters
 *   <li>ReadInputRegisters
 * </ul>
 *
 * <p>Operations that require write authorization:
 *
 * <ul>
 *   <li>WriteSingleCoil
 *   <li>WriteSingleRegister
 *   <li>WriteMultipleCoils
 *   <li>WriteMultipleRegisters
 *   <li>MaskWriteRegister
 * </ul>
 *
 * <p>Operations that require both read and write authorization:
 *
 * <ul>
 *   <li>ReadWriteMultipleRegisters
 * </ul>
 */
public abstract class ReadWriteAuthzHandler implements AuthzHandler {

  @Override
  public AuthzResult authorizeReadCoils(
      AuthzContext authzContext, int unitId, ReadCoilsRequest request) {

    return authorizeRead(unitId, authzContext);
  }

  @Override
  public AuthzResult authorizeReadDiscreteInputs(
      AuthzContext authzContext, int unitId, ReadDiscreteInputsRequest request) {

    return authorizeRead(unitId, authzContext);
  }

  @Override
  public AuthzResult authorizeReadHoldingRegisters(
      AuthzContext authzContext, int unitId, ReadHoldingRegistersRequest request) {

    return authorizeRead(unitId, authzContext);
  }

  @Override
  public AuthzResult authorizeReadInputRegisters(
      AuthzContext authzContext, int unitId, ReadInputRegistersRequest request) {

    return authorizeRead(unitId, authzContext);
  }

  @Override
  public AuthzResult authorizeWriteSingleCoil(
      AuthzContext authzContext, int unitId, WriteSingleCoilRequest request) {

    return authorizeWrite(unitId, authzContext);
  }

  @Override
  public AuthzResult authorizeWriteSingleRegister(
      AuthzContext authzContext, int unitId, WriteSingleRegisterRequest request) {

    return authorizeWrite(unitId, authzContext);
  }

  @Override
  public AuthzResult authorizeWriteMultipleCoils(
      AuthzContext authzContext, int unitId, WriteMultipleCoilsRequest request) {

    return authorizeWrite(unitId, authzContext);
  }

  @Override
  public AuthzResult authorizeWriteMultipleRegisters(
      AuthzContext authzContext, int unitId, WriteMultipleRegistersRequest request) {

    return authorizeWrite(unitId, authzContext);
  }

  @Override
  public AuthzResult authorizeMaskWriteRegister(
      AuthzContext authzContext, int unitId, MaskWriteRegisterRequest request) {

    return authorizeWrite(unitId, authzContext);
  }

  @Override
  public AuthzResult authorizeReadWriteMultipleRegisters(
      AuthzContext authzContext, int unitId, ReadWriteMultipleRegistersRequest request) {

    AuthzResult readResult = authorizeRead(unitId, authzContext);
    AuthzResult writeResult = authorizeWrite(unitId, authzContext);

    if (readResult == AuthzResult.AUTHORIZED && writeResult == AuthzResult.AUTHORIZED) {
      return AuthzResult.AUTHORIZED;
    } else {
      return AuthzResult.NOT_AUTHORIZED;
    }
  }

  /**
   * Authorize a read operation against the given unit id.
   *
   * @param unitId the unit id to authorize against.
   * @param authzContext the {@link AuthzContext}.
   * @return the result of the authorization check.
   */
  protected abstract AuthzResult authorizeRead(int unitId, AuthzContext authzContext);

  /**
   * Authorize a write operation against the given unit id.
   *
   * @param unitId the unit id to authorize against.
   * @param authzContext the {@link AuthzContext}.
   * @return the result of the authorization check.
   */
  protected abstract AuthzResult authorizeWrite(int unitId, AuthzContext authzContext);
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/server/ReadOnlyModbusServices.java`

```java
package com.digitalpetri.modbus.server;

import com.digitalpetri.modbus.exceptions.UnknownUnitIdException;
import com.digitalpetri.modbus.pdu.*;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public abstract class ReadOnlyModbusServices implements ModbusServices {

  protected abstract Optional<ProcessImage> getProcessImage(int unitId);

  @Override
  public ReadCoilsResponse readCoils(
      ModbusRequestContext context, int unitId, ReadCoilsRequest request)
      throws UnknownUnitIdException {

    ProcessImage processImage =
        getProcessImage(unitId).orElseThrow(() -> new UnknownUnitIdException(unitId));

    final int address = request.address();
    final int quantity = request.quantity();

    byte[] coils = processImage.get(tx -> tx.readCoils(readBits(address, quantity)));

    return new ReadCoilsResponse(coils);
  }

  @Override
  public ReadDiscreteInputsResponse readDiscreteInputs(
      ModbusRequestContext context, int unitId, ReadDiscreteInputsRequest request)
      throws UnknownUnitIdException {

    ProcessImage processImage =
        getProcessImage(unitId).orElseThrow(() -> new UnknownUnitIdException(unitId));

    final int address = request.address();
    final int quantity = request.quantity();

    byte[] inputs = processImage.get(tx -> tx.readDiscreteInputs(readBits(address, quantity)));

    return new ReadDiscreteInputsResponse(inputs);
  }

  @Override
  public ReadHoldingRegistersResponse readHoldingRegisters(
      ModbusRequestContext context, int unitId, ReadHoldingRegistersRequest request)
      throws UnknownUnitIdException {

    ProcessImage processImage =
        getProcessImage(unitId).orElseThrow(() -> new UnknownUnitIdException(unitId));

    final int address = request.address();
    final int quantity = request.quantity();

    byte[] registers =
        processImage.get(tx -> tx.readHoldingRegisters(readRegisters(address, quantity)));

    return new ReadHoldingRegistersResponse(registers);
  }

  @Override
  public ReadInputRegistersResponse readInputRegisters(
      ModbusRequestContext context, int unitId, ReadInputRegistersRequest request)
      throws UnknownUnitIdException {

    ProcessImage processImage =
        getProcessImage(unitId).orElseThrow(() -> new UnknownUnitIdException(unitId));

    final int address = request.address();
    final int quantity = request.quantity();

    byte[] registers =
        processImage.get(tx -> tx.readInputRegisters(readRegisters(address, quantity)));

    return new ReadInputRegistersResponse(registers);
  }

  private static Function<Map<Integer, Boolean>, byte[]> readBits(int address, int quantity) {
    final var bytes = new byte[(quantity + 7) / 8];

    return bitMap -> {
      for (int i = 0; i < quantity; i++) {
        int bitIndex = i % 8;
        int byteIndex = i / 8;

        boolean value = bitMap.getOrDefault(address + i, false);

        int b = bytes[byteIndex];
        if (value) {
          b |= (1 << bitIndex);
        } else {
          b &= ~(1 << bitIndex);
        }
        bytes[byteIndex] = (byte) (b & 0xFF);
      }

      return bytes;
    };
  }

  protected static Function<Map<Integer, byte[]>, byte[]> readRegisters(int address, int quantity) {
    final var registers = new byte[quantity * 2];

    return registerMap -> {
      for (int i = 0; i < quantity; i++) {
        byte[] value = registerMap.getOrDefault(address + i, new byte[2]);

        registers[i * 2] = value[0];
        registers[i * 2 + 1] = value[1];
      }

      return registers;
    };
  }
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/server/ModbusServerTransport.java`

```java
package com.digitalpetri.modbus.server;

import java.util.concurrent.CompletionStage;

public interface ModbusServerTransport<C extends ModbusRequestContext, T> {

  /**
   * Bind the transport to its configured local address.
   *
   * @return a {@link CompletionStage} that completes when the transport is bound.
   */
  CompletionStage<Void> bind();

  /**
   * Unbind the transport from its configured local address.
   *
   * @return a {@link CompletionStage} that completes when the transport is unbound.
   */
  CompletionStage<Void> unbind();

  /**
   * Configure a callback to receive request frames from the transport.
   *
   * @param frameReceiver the callback to receive request frames.
   */
  void receive(FrameReceiver<C, T> frameReceiver);

  interface FrameReceiver<C, T> {

    /**
     * Receive a request frame from the transport and respond to it.
     *
     * @param frame the request frame.
     * @return a corresponding response frame.
     * @throws Exception if there is an unrecoverable error and the channel should be closed.
     */
    T receive(C context, T frame) throws Exception;
  }
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/server/ModbusServices.java`

```java
package com.digitalpetri.modbus.server;

import com.digitalpetri.modbus.ExceptionCode;
import com.digitalpetri.modbus.FunctionCode;
import com.digitalpetri.modbus.exceptions.ModbusResponseException;
import com.digitalpetri.modbus.exceptions.UnknownUnitIdException;
import com.digitalpetri.modbus.pdu.*;

public interface ModbusServices {

  /**
   * Handle an incoming {@link ReadCoilsRequest} targeting {@code unitId}.
   *
   * @param context the {@link ModbusRequestContext} for the request.
   * @param unitId the unit id being targeted.
   * @param request the {@link ReadCoilsRequest} to handle.
   * @return a {@link ReadCoilsResponse}.
   * @throws ModbusResponseException if there is an error handling the request that can be reported
   *     by an {@link ExceptionCode}.
   * @throws UnknownUnitIdException if the unit id is not known to this server.
   */
  default ReadCoilsResponse readCoils(
      ModbusRequestContext context, int unitId, ReadCoilsRequest request)
      throws ModbusResponseException, UnknownUnitIdException {

    throw new ModbusResponseException(FunctionCode.READ_COILS, ExceptionCode.ILLEGAL_FUNCTION);
  }

  /**
   * Handle an incoming {@link ReadDiscreteInputsRequest} targeting {@code unitId}.
   *
   * @param context the {@link ModbusRequestContext} for the request.
   * @param unitId the unit id being targeted.
   * @param request the {@link ReadDiscreteInputsRequest} to handle.
   * @return a {@link ReadDiscreteInputsResponse}.
   * @throws ModbusResponseException if there is an error handling the request that can be reported
   *     by an {@link ExceptionCode}.
   * @throws UnknownUnitIdException if the unit id is not known to this server.
   */
  default ReadDiscreteInputsResponse readDiscreteInputs(
      ModbusRequestContext context, int unitId, ReadDiscreteInputsRequest request)
      throws ModbusResponseException, UnknownUnitIdException {

    throw new ModbusResponseException(
        FunctionCode.READ_DISCRETE_INPUTS, ExceptionCode.ILLEGAL_FUNCTION);
  }

  /**
   * Handle an incoming {@link ReadHoldingRegistersRequest} targeting {@code unitId}.
   *
   * @param context the {@link ModbusRequestContext} for the request.
   * @param unitId the unit id being targeted.
   * @param request the {@link ReadHoldingRegistersRequest} to handle.
   * @return a {@link ReadHoldingRegistersResponse}.
   * @throws ModbusResponseException if there is an error handling the request that can be reported
   *     by an {@link ExceptionCode}.
   * @throws UnknownUnitIdException if the unit id is not known to this server.
   */
  default ReadHoldingRegistersResponse readHoldingRegisters(
      ModbusRequestContext context, int unitId, ReadHoldingRegistersRequest request)
      throws ModbusResponseException, UnknownUnitIdException {

    throw new ModbusResponseException(
        FunctionCode.READ_HOLDING_REGISTERS, ExceptionCode.ILLEGAL_FUNCTION);
  }

  /**
   * Handle an incoming {@link ReadInputRegistersRequest} targeting {@code unitId}.
   *
   * @param context the {@link ModbusRequestContext} for the request.
   * @param unitId the unit id being targeted.
   * @param request the {@link ReadInputRegistersRequest} to handle.
   * @return a {@link ReadInputRegistersResponse}.
   * @throws ModbusResponseException if there is an error handling the request that can be reported
   *     by an {@link ExceptionCode}.
   * @throws UnknownUnitIdException if the unit id is not known to this server.
   */
  default ReadInputRegistersResponse readInputRegisters(
      ModbusRequestContext context, int unitId, ReadInputRegistersRequest request)
      throws ModbusResponseException, UnknownUnitIdException {

    throw new ModbusResponseException(
        FunctionCode.READ_INPUT_REGISTERS, ExceptionCode.ILLEGAL_FUNCTION);
  }

  /**
   * Handle an incoming {@link WriteSingleCoilRequest} targeting {@code unitId}.
   *
   * @param context the {@link ModbusRequestContext} for the request.
   * @param unitId the unit id being targeted.
   * @param request the {@link WriteSingleCoilRequest} to handle.
   * @return a {@link WriteSingleCoilResponse}.
   * @throws ModbusResponseException if there is an error handling the request that can be reported
   *     by an {@link ExceptionCode}.
   * @throws UnknownUnitIdException if the unit id is not known to this server.
   */
  default WriteSingleCoilResponse writeSingleCoil(
      ModbusRequestContext context, int unitId, WriteSingleCoilRequest request)
      throws ModbusResponseException, UnknownUnitIdException {

    throw new ModbusResponseException(
        FunctionCode.WRITE_SINGLE_COIL, ExceptionCode.ILLEGAL_FUNCTION);
  }

  /**
   * Handle an incoming {@link WriteSingleRegisterRequest} targeting {@code unitId}.
   *
   * @param context the {@link ModbusRequestContext} for the request.
   * @param unitId the unit id being targeted.
   * @param request the {@link WriteSingleRegisterRequest} to handle.
   * @return a {@link WriteSingleRegisterResponse}.
   * @throws ModbusResponseException if there is an error handling the request that can be reported
   *     by an {@link ExceptionCode}.
   * @throws UnknownUnitIdException if the unit id is not known to this server.
   */
  default WriteSingleRegisterResponse writeSingleRegister(
      ModbusRequestContext context, int unitId, WriteSingleRegisterRequest request)
      throws ModbusResponseException, UnknownUnitIdException {

    throw new ModbusResponseException(
        FunctionCode.WRITE_SINGLE_REGISTER, ExceptionCode.ILLEGAL_FUNCTION);
  }

  /**
   * Handle an incoming {@link WriteMultipleCoilsRequest} targeting {@code unitId}.
   *
   * @param context the {@link ModbusRequestContext} for the request.
   * @param unitId the unit id being targeted.
   * @param request the {@link WriteMultipleCoilsRequest} to handle.
   * @return a {@link WriteMultipleCoilsResponse}.
   * @throws ModbusResponseException if there is an error handling the request that can be reported
   *     by an {@link ExceptionCode}.
   * @throws UnknownUnitIdException if the unit id is not known to this server.
   */
  default WriteMultipleCoilsResponse writeMultipleCoils(
      ModbusRequestContext context, int unitId, WriteMultipleCoilsRequest request)
      throws ModbusResponseException, UnknownUnitIdException {

    throw new ModbusResponseException(
        FunctionCode.WRITE_MULTIPLE_COILS, ExceptionCode.ILLEGAL_FUNCTION);
  }

  /**
   * Handle an incoming {@link WriteMultipleRegistersRequest} targeting {@code unitId}.
   *
   * @param context the {@link ModbusRequestContext} for the request.
   * @param unitId the unit id being targeted.
   * @param request the {@link WriteMultipleRegistersRequest} to handle.
   * @return a {@link WriteMultipleRegistersResponse}.
   * @throws ModbusResponseException if there is an error handling the request that can be reported
   *     by an {@link ExceptionCode}.
   * @throws UnknownUnitIdException if the unit id is not known to this server.
   */
  default WriteMultipleRegistersResponse writeMultipleRegisters(
      ModbusRequestContext context, int unitId, WriteMultipleRegistersRequest request)
      throws ModbusResponseException, UnknownUnitIdException {

    throw new ModbusResponseException(
        FunctionCode.WRITE_MULTIPLE_REGISTERS, ExceptionCode.ILLEGAL_FUNCTION);
  }

  /**
   * Handle an incoming {@link MaskWriteRegisterRequest} targeting {@code unitId}.
   *
   * @param context the {@link ModbusRequestContext} for the request.
   * @param unitId the unit id being targeted.
   * @param request the {@link MaskWriteRegisterRequest} to handle.
   * @return a {@link MaskWriteRegisterResponse}.
   * @throws ModbusResponseException if there is an error handling the request that can be reported
   *     by an {@link ExceptionCode}.
   * @throws UnknownUnitIdException if the unit id is not known to this server.
   */
  default MaskWriteRegisterResponse maskWriteRegister(
      ModbusRequestContext context, int unitId, MaskWriteRegisterRequest request)
      throws ModbusResponseException, UnknownUnitIdException {

    throw new ModbusResponseException(
        FunctionCode.MASK_WRITE_REGISTER, ExceptionCode.ILLEGAL_FUNCTION);
  }

  /**
   * Handle an incoming {@link ReadWriteMultipleRegistersRequest} targeting {@code unitId}.
   *
   * @param context the {@link ModbusRequestContext} for the request.
   * @param unitId the unit id being targeted.
   * @param request the {@link ReadWriteMultipleRegistersRequest} to handle.
   * @return a {@link ReadWriteMultipleRegistersResponse}.
   * @throws ModbusResponseException if there is an error handling the request that can be reported
   *     by an {@link ExceptionCode}.
   * @throws UnknownUnitIdException if the unit id is not known to this server.
   */
  default ReadWriteMultipleRegistersResponse readWriteMultipleRegisters(
      ModbusRequestContext context, int unitId, ReadWriteMultipleRegistersRequest request)
      throws ModbusResponseException, UnknownUnitIdException {

    throw new ModbusResponseException(
        FunctionCode.READ_WRITE_MULTIPLE_REGISTERS, ExceptionCode.ILLEGAL_FUNCTION);
  }

  /**
   * Check that the combination of address and quantity are valid.
   *
   * @param functionCode the function code of the request.
   * @param address the starting address.
   * @param quantity the quantity of registers.
   * @throws ModbusResponseException if the combination of address and quantity are invalid.
   */
  static void checkRegisterRange(int functionCode, int address, int quantity)
      throws ModbusResponseException {

    if (address < 0 || address > 0xFFFF) {
      throw new ModbusResponseException(functionCode, ExceptionCode.ILLEGAL_DATA_ADDRESS.getCode());
    }
    if (quantity < 1 || quantity > 0x7D) {
      throw new ModbusResponseException(functionCode, ExceptionCode.ILLEGAL_DATA_VALUE.getCode());
    }
    if (address + quantity > 65536) {
      throw new ModbusResponseException(functionCode, ExceptionCode.ILLEGAL_DATA_ADDRESS.getCode());
    }
  }

  /**
   * Check that the combination of address and quantity are valid.
   *
   * @param functionCode the function code of the request.
   * @param address the starting address.
   * @param quantity the quantity of bits.
   * @throws ModbusResponseException if the combination of address and quantity are invalid.
   */
  static void checkBitRange(int functionCode, int address, int quantity)
      throws ModbusResponseException {

    if (address < 0 || address > 0xFFFF) {
      throw new ModbusResponseException(functionCode, ExceptionCode.ILLEGAL_DATA_ADDRESS.getCode());
    }
    if (quantity < 1 || quantity > 0x7D0) {
      throw new ModbusResponseException(functionCode, ExceptionCode.ILLEGAL_DATA_VALUE.getCode());
    }
    if (address + quantity > 65536) {
      throw new ModbusResponseException(functionCode, ExceptionCode.ILLEGAL_DATA_ADDRESS.getCode());
    }
  }
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/server/ModbusTcpServerTransport.java`

```java
package com.digitalpetri.modbus.server;

import com.digitalpetri.modbus.ModbusTcpFrame;
import com.digitalpetri.modbus.server.ModbusRequestContext.ModbusTcpRequestContext;

/**
 * Modbus/TCP server transport; a {@link ModbusServerTransport} that sends and receives {@link
 * ModbusTcpFrame}s.
 */
public interface ModbusTcpServerTransport
    extends ModbusServerTransport<ModbusTcpRequestContext, ModbusTcpFrame> {}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/server/ModbusRequestContext.java`

```java
package com.digitalpetri.modbus.server;

import com.digitalpetri.modbus.server.ModbusRequestContext.ModbusRtuRequestContext;
import com.digitalpetri.modbus.server.ModbusRequestContext.ModbusTcpRequestContext;
import com.digitalpetri.modbus.server.authz.AuthzContext;

import java.net.SocketAddress;

/**
 * A transport-agnostic super-interface that transport implementations can subclass to smuggle
 * transport-specific context information to the application layer.
 */
public sealed interface ModbusRequestContext
    permits ModbusRtuRequestContext, ModbusTcpRequestContext {

  non-sealed interface ModbusTcpRequestContext extends ModbusRequestContext {

    /**
     * Get the local address that received the request.
     *
     * @return the local address that received the request.
     */
    SocketAddress localAddress();

    /**
     * Get the remote address of the client that sent the request.
     *
     * @return the remote address of the client that sent the request.
     */
    SocketAddress remoteAddress();
  }

  interface ModbusTcpTlsRequestContext extends ModbusTcpRequestContext, AuthzContext {}

  non-sealed interface ModbusRtuRequestContext extends ModbusRequestContext {}

  interface ModbusRtuTcpRequestContext extends ModbusRtuRequestContext, ModbusTcpRequestContext {}

  interface ModbusRtuTlsRequestContext
      extends ModbusRtuRequestContext, ModbusTcpTlsRequestContext {}
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/server/ModbusRtuServer.java`

```java
package com.digitalpetri.modbus.server;

import com.digitalpetri.modbus.Crc16;
import com.digitalpetri.modbus.ExceptionCode;
import com.digitalpetri.modbus.FunctionCode;
import com.digitalpetri.modbus.ModbusRtuFrame;
import com.digitalpetri.modbus.exceptions.ModbusResponseException;
import com.digitalpetri.modbus.pdu.*;
import com.digitalpetri.modbus.server.ModbusRequestContext.ModbusRtuRequestContext;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class ModbusRtuServer implements ModbusServer {

  private final ModbusServerConfig config;
  private final ModbusRtuServerTransport transport;
  private final AtomicReference<ModbusServices> services =
      new AtomicReference<>(new ModbusServices() {});

  public ModbusRtuServer(
      ModbusServerConfig config, ModbusRtuServerTransport transport, ModbusServices services) {

    this.config = config;
    this.transport = transport;

    this.services.set(services);
  }

  /**
   * Get the {@link ModbusServerConfig} used by this server.
   *
   * @return the {@link ModbusServerConfig} used by this server.
   */
  public ModbusServerConfig getConfig() {
    return config;
  }

  /**
   * Get the {@link ModbusRtuServerTransport} used by this server.
   *
   * @return the {@link ModbusRtuServerTransport} used by this server.
   */
  public ModbusRtuServerTransport getTransport() {
    return transport;
  }

  @Override
  public void start() throws ExecutionException, InterruptedException {
    transport.receive(
        (context, frame) -> {
          int unitId = frame.unitId();
          ByteBuffer pdu = frame.pdu();
          int fcb = pdu.get(pdu.position()) & 0xFF;
          ModbusRequestPdu requestPdu =
              (ModbusRequestPdu) config.requestSerializer().decode(fcb, pdu);

          return handleModbusRtuFrame(context, unitId, fcb, requestPdu);
        });

    transport.bind().toCompletableFuture().get();
  }

  @Override
  public void stop() throws ExecutionException, InterruptedException {
    transport.unbind().toCompletableFuture().get();
  }

  @Override
  public void setModbusServices(ModbusServices services) {
    if (services == null) {
      throw new NullPointerException("services");
    }
    this.services.set(services);
  }

  protected ModbusRtuFrame handleModbusRtuFrame(
      ModbusRtuRequestContext context, int unitId, int fcb, ModbusRequestPdu requestPdu)
      throws Exception {

    try {
      FunctionCode functionCode = FunctionCode.from(fcb).orElse(null);

      if (functionCode == null) {
        throw new ModbusResponseException(fcb, ExceptionCode.ILLEGAL_FUNCTION.getCode());
      }

      ModbusServices services = this.services.get();
      assert services != null;

      ModbusResponsePdu response =
          switch (functionCode) {
            case READ_COILS -> {
              if (requestPdu instanceof ReadCoilsRequest request) {
                yield services.readCoils(context, unitId, request);
              } else {
                throw new IllegalArgumentException("expected ReadCoilsRequest");
              }
            }
            case READ_DISCRETE_INPUTS -> {
              if (requestPdu instanceof ReadDiscreteInputsRequest request) {
                yield services.readDiscreteInputs(context, unitId, request);
              } else {
                throw new IllegalArgumentException("expected ReadDiscreteInputsRequest");
              }
            }
            case READ_HOLDING_REGISTERS -> {
              if (requestPdu instanceof ReadHoldingRegistersRequest request) {
                yield services.readHoldingRegisters(context, unitId, request);
              } else {
                throw new IllegalArgumentException("expected ReadHoldingRegistersRequest");
              }
            }
            case READ_INPUT_REGISTERS -> {
              if (requestPdu instanceof ReadInputRegistersRequest request) {
                yield services.readInputRegisters(context, unitId, request);
              } else {
                throw new IllegalArgumentException("expected ReadInputRegistersRequest");
              }
            }
            case WRITE_SINGLE_COIL -> {
              if (requestPdu instanceof WriteSingleCoilRequest request) {
                yield services.writeSingleCoil(context, unitId, request);
              } else {
                throw new IllegalArgumentException("expected WriteSingleCoilRequest");
              }
            }
            case WRITE_SINGLE_REGISTER -> {
              if (requestPdu instanceof WriteSingleRegisterRequest request) {
                yield services.writeSingleRegister(context, unitId, request);
              } else {
                throw new IllegalArgumentException("expected WriteSingleRegisterRequest");
              }
            }
            case WRITE_MULTIPLE_COILS -> {
              if (requestPdu instanceof WriteMultipleCoilsRequest request) {
                yield services.writeMultipleCoils(context, unitId, request);
              } else {
                throw new IllegalArgumentException("expected WriteMultipleCoilsRequest");
              }
            }
            case WRITE_MULTIPLE_REGISTERS -> {
              if (requestPdu instanceof WriteMultipleRegistersRequest request) {
                yield services.writeMultipleRegisters(context, unitId, request);
              } else {
                throw new IllegalArgumentException("expected WriteMultipleRegistersRequest");
              }
            }
            case MASK_WRITE_REGISTER -> {
              if (requestPdu instanceof MaskWriteRegisterRequest request) {
                yield services.maskWriteRegister(context, unitId, request);
              } else {
                throw new IllegalArgumentException("expected MaskWriteRegisterRequest");
              }
            }
            case READ_WRITE_MULTIPLE_REGISTERS -> {
              if (requestPdu instanceof ReadWriteMultipleRegistersRequest request) {
                yield services.readWriteMultipleRegisters(context, unitId, request);
              } else {
                throw new IllegalArgumentException("expected ReadWriteMultipleRegistersRequest");
              }
            }
            default ->
                throw new ModbusResponseException(
                    requestPdu.getFunctionCode(), ExceptionCode.ILLEGAL_FUNCTION.getCode());
          };

      ByteBuffer pdu = ByteBuffer.allocate(256);
      config.responseSerializer().encode(response, pdu);
      pdu.flip();

      ByteBuffer crc = calculateCrc16(unitId, pdu);
      crc.flip();

      return new ModbusRtuFrame(unitId, pdu, crc);
    } catch (ModbusResponseException e) {
      int fc = fcb + 0x80;
      int ec = e.getExceptionCode();

      ByteBuffer pdu = ByteBuffer.allocate(2).put((byte) fc).put((byte) ec);

      ByteBuffer crc = calculateCrc16(unitId, pdu);

      return new ModbusRtuFrame(unitId, pdu.flip(), crc.flip());
    }
  }

  private ByteBuffer calculateCrc16(int unitId, ByteBuffer pdu) {
    var crc16 = new Crc16();
    crc16.update(unitId);
    crc16.update(pdu);

    ByteBuffer crc = ByteBuffer.allocate(2);
    // write crc in little-endian order
    crc.put((byte) (crc16.getValue() & 0xFF));
    crc.put((byte) ((crc16.getValue() >> 8) & 0xFF));

    return crc;
  }

  /**
   * Create a new {@link ModbusRtuServer} with the given {@link ModbusRtuServerTransport} and {@link
   * ModbusServices}.
   *
   * @param transport the {@link ModbusRtuServerTransport} to use.
   * @param modbusServices the {@link ModbusServices} to use.
   * @return a new {@link ModbusRtuServer}.
   */
  public static ModbusRtuServer create(
      ModbusRtuServerTransport transport, ModbusServices modbusServices) {

    return create(transport, modbusServices, b -> {});
  }

  /**
   * Create a new {@link ModbusRtuServer} with the given {@link ModbusRtuServerTransport}, {@link
   * ModbusServices}, and a callback that can be used to configure a {@link
   * ModbusServerConfig.Builder}.
   *
   * @param transport the {@link ModbusRtuServerTransport} to use.
   * @param modbusServices the {@link ModbusServices} to use.
   * @param configure a callback that can be used to configure a {@link ModbusServerConfig.Builder}.
   * @return a new {@link ModbusRtuServer}.
   */
  public static ModbusRtuServer create(
      ModbusRtuServerTransport transport,
      ModbusServices modbusServices,
      Consumer<ModbusServerConfig.Builder> configure) {

    ModbusServerConfig.Builder builder = new ModbusServerConfig.Builder();
    configure.accept(builder);
    return new ModbusRtuServer(builder.build(), transport, modbusServices);
  }
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/pdu/ReadInputRegistersRequest.java`

```java
package com.digitalpetri.modbus.pdu;

import com.digitalpetri.modbus.FunctionCode;

import java.nio.ByteBuffer;

/**
 * A {@link FunctionCode#READ_INPUT_REGISTERS} request PDU.
 *
 * <p>Requests specify the starting register address and the number of register to read. In the PDU,
 * addresses are addressed starting at 0.
 *
 * @param address the starting address. 2 bytes, range [0x0000, 0xFFFF].
 * @param quantity the quantity of registers to read. 2 bytes, range [0x01, 0x7D].
 */
public record ReadInputRegistersRequest(int address, int quantity) implements ModbusRequestPdu {

  @Override
  public int getFunctionCode() {
    return FunctionCode.READ_INPUT_REGISTERS.getCode();
  }

  /** Utility functions for encoding and decoding {@link ReadInputRegistersRequest}. */
  public static final class Serializer {

    private Serializer() {}

    /**
     * Encode a {@link ReadInputRegistersRequest} into a {@link ByteBuffer}.
     *
     * @param request the request to encode.
     * @param buffer the buffer to encode into.
     */
    public static void encode(ReadInputRegistersRequest request, ByteBuffer buffer) {
      buffer.put((byte) request.getFunctionCode());
      buffer.putShort((short) request.address);
      buffer.putShort((short) request.quantity);
    }

    /**
     * Decode a {@link ReadInputRegistersRequest} from a {@link ByteBuffer}.
     *
     * @param buffer the buffer to decode from.
     * @return the decoded request.
     */
    public static ReadInputRegistersRequest decode(ByteBuffer buffer) {
      int functionCode = buffer.get() & 0xFF;
      assert functionCode == FunctionCode.READ_INPUT_REGISTERS.getCode();

      int address = buffer.getShort() & 0xFFFF;
      int quantity = buffer.getShort() & 0xFFFF;

      return new ReadInputRegistersRequest(address, quantity);
    }
  }
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/pdu/ModbusRequestPdu.java`

```java
package com.digitalpetri.modbus.pdu;

/** Super-interface for Modbus request PDUs. */
public interface ModbusRequestPdu extends ModbusPdu {}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/pdu/MaskWriteRegisterRequest.java`

```java
package com.digitalpetri.modbus.pdu;

import com.digitalpetri.modbus.FunctionCode;

import java.nio.ByteBuffer;

/**
 * A {@link FunctionCode#MASK_WRITE_REGISTER} request PDU.
 *
 * @param address the address, 2 bytes, range [0x0000, 0xFFFF].
 * @param andMask the AND mask, 2 bytes, range [0x0000, 0xFFFF].
 * @param orMask the OR mask, 2 bytes, range [0x0000, 0xFFFF].
 */
public record MaskWriteRegisterRequest(int address, int andMask, int orMask)
    implements ModbusRequestPdu {

  @Override
  public int getFunctionCode() {
    return FunctionCode.MASK_WRITE_REGISTER.getCode();
  }

  /** Utility functions for encoding and decoding {@link MaskWriteRegisterRequest}. */
  public static class Serializer {

    private Serializer() {}

    /**
     * Encode a {@link MaskWriteRegisterRequest} into a {@link ByteBuffer}.
     *
     * @param request the request to encode.
     * @param buffer the buffer to encode into.
     */
    public static void encode(MaskWriteRegisterRequest request, ByteBuffer buffer) {
      buffer.put((byte) request.getFunctionCode());
      buffer.putShort((short) request.address);
      buffer.putShort((short) request.andMask);
      buffer.putShort((short) request.orMask);
    }

    /**
     * Decode a {@link MaskWriteRegisterRequest} from a {@link ByteBuffer}.
     *
     * @param buffer the buffer to decode from.
     * @return the decoded request.
     */
    public static MaskWriteRegisterRequest decode(ByteBuffer buffer) {
      int functionCode = buffer.get() & 0xFF;
      assert functionCode == FunctionCode.MASK_WRITE_REGISTER.getCode();

      int address = buffer.getShort() & 0xFFFF;
      int andMask = buffer.getShort() & 0xFFFF;
      int orMask = buffer.getShort() & 0xFFFF;

      return new MaskWriteRegisterRequest(address, andMask, orMask);
    }
  }
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/pdu/ModbusPdu.java`

```java
package com.digitalpetri.modbus.pdu;

/** Super-interface for objects that can be encoded as a Modbus PDU. */
public interface ModbusPdu {

  /**
   * Get the function code identifying this PDU.
   *
   * @return the function code identifying this PDU.
   */
  int getFunctionCode();
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/pdu/ReadDiscreteInputsRequest.java`

```java
package com.digitalpetri.modbus.pdu;

import com.digitalpetri.modbus.FunctionCode;

import java.nio.ByteBuffer;

/**
 * A {@link FunctionCode#READ_DISCRETE_INPUTS} request PDU.
 *
 * <p>Requests specify the starting address, i.e. the address of the first input specified, and the
 * number of inputs. In the PDU inputs are addressed starting at 0.
 *
 * @param address the starting address. 2 bytes, range [0x0000, 0xFFFF].
 * @param quantity the quantity of inputs to read. 2 bytes, range [0x0001, 0x07D0].
 */
public record ReadDiscreteInputsRequest(int address, int quantity) implements ModbusRequestPdu {

  @Override
  public int getFunctionCode() {
    return FunctionCode.READ_DISCRETE_INPUTS.getCode();
  }

  public static final class Serializer {

    private Serializer() {}

    /**
     * Encode a {@link ReadDiscreteInputsRequest} into a {@link ByteBuffer}.
     *
     * @param request the request to encode.
     * @param buffer the buffer to encode into.
     */
    public static void encode(ReadDiscreteInputsRequest request, ByteBuffer buffer) {
      buffer.put((byte) request.getFunctionCode());
      buffer.putShort((short) request.address);
      buffer.putShort((short) request.quantity);
    }

    /**
     * Decode a {@link ReadDiscreteInputsRequest} from a {@link ByteBuffer}.
     *
     * @param buffer the buffer to decode from.
     * @return the decoded request.
     */
    public static ReadDiscreteInputsRequest decode(ByteBuffer buffer) {
      int functionCode = buffer.get() & 0xFF;
      assert functionCode == FunctionCode.READ_DISCRETE_INPUTS.getCode();

      int address = buffer.getShort() & 0xFFFF;
      int quantity = buffer.getShort() & 0xFFFF;

      return new ReadDiscreteInputsRequest(address, quantity);
    }
  }
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/pdu/WriteMultipleCoilsRequest.java`

```java
package com.digitalpetri.modbus.pdu;

import com.digitalpetri.modbus.FunctionCode;
import com.digitalpetri.modbus.internal.util.Hex;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * A {@link FunctionCode#WRITE_MULTIPLE_COILS} request PDU.
 *
 * @param address the starting address. 2 bytes, range [0x0000, 0xFFFF].
 * @param quantity the quantity of coils to write. 2 bytes, range [0x0001, 0x7B0].
 * @param values a buffer of at least N bytes, where N = (quantity + 7) / 8.
 */
public record WriteMultipleCoilsRequest(int address, int quantity, byte[] values)
    implements ModbusRequestPdu {

  @Override
  public int getFunctionCode() {
    return FunctionCode.WRITE_MULTIPLE_COILS.getCode();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WriteMultipleCoilsRequest that = (WriteMultipleCoilsRequest) o;
    return Objects.equals(address, that.address)
        && Objects.equals(quantity, that.quantity)
        && Arrays.equals(values, that.values);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(address, quantity);
    result = 31 * result + Arrays.hashCode(values);
    return result;
  }

  @Override
  public String toString() {
    // note: overridden to give preferred representation of `values` bytes
    return new StringJoiner(", ", WriteMultipleCoilsRequest.class.getSimpleName() + "[", "]")
        .add("address=" + address)
        .add("quantity=" + quantity)
        .add("values=" + Hex.format(values))
        .toString();
  }

  /** Utility functions for encoding and decoding {@link WriteMultipleCoilsRequest}. */
  public static final class Serializer {

    private Serializer() {}

    /**
     * Encode a {@link WriteMultipleCoilsRequest} into a {@link ByteBuffer}.
     *
     * @param request the request to encode.
     * @param buffer the buffer to encode into.
     */
    public static void encode(WriteMultipleCoilsRequest request, ByteBuffer buffer) {
      buffer.put((byte) request.getFunctionCode());
      buffer.putShort((short) request.address);
      buffer.putShort((short) request.quantity);

      int byteCount = (request.quantity + 7) / 8;
      buffer.put((byte) byteCount);
      buffer.put(request.values);
    }

    /**
     * Decode a {@link WriteMultipleCoilsRequest} from a {@link ByteBuffer}.
     *
     * @param buffer the buffer to decode from.
     * @return the decoded request.
     */
    public static WriteMultipleCoilsRequest decode(ByteBuffer buffer) {
      int functionCode = buffer.get() & 0xFF;
      assert functionCode == FunctionCode.WRITE_MULTIPLE_COILS.getCode();

      int address = buffer.getShort() & 0xFFFF;
      int quantity = buffer.getShort() & 0xFFFF;

      int byteCount = buffer.get() & 0xFF;
      var values = new byte[byteCount];
      buffer.get(values);

      return new WriteMultipleCoilsRequest(address, quantity, values);
    }
  }
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/pdu/ReadDiscreteInputsResponse.java`

```java
package com.digitalpetri.modbus.pdu;

import com.digitalpetri.modbus.FunctionCode;
import com.digitalpetri.modbus.internal.util.Hex;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.StringJoiner;

/** A {@link FunctionCode#READ_DISCRETE_INPUTS} response PDU. */
public record ReadDiscreteInputsResponse(byte[] inputs) implements ModbusResponsePdu {

  @Override
  public int getFunctionCode() {
    return FunctionCode.READ_DISCRETE_INPUTS.getCode();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ReadDiscreteInputsResponse that = (ReadDiscreteInputsResponse) o;
    return Arrays.equals(inputs, that.inputs);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(inputs);
  }

  @Override
  public String toString() {
    // note: overridden to give preferred representation of `inputs` bytes
    return new StringJoiner(", ", ReadDiscreteInputsResponse.class.getSimpleName() + "[", "]")
        .add("inputs=" + Hex.format(inputs))
        .toString();
  }

  /** Utility functions for encoding and decoding {@link ReadDiscreteInputsResponse}. */
  public static final class Serializer {

    private Serializer() {}

    /**
     * Encode a {@link ReadDiscreteInputsResponse} into a {@link ByteBuffer}.
     *
     * @param response the response to encode.
     * @param buffer the buffer to encode into.
     */
    public static void encode(ReadDiscreteInputsResponse response, ByteBuffer buffer) {
      buffer.put((byte) response.getFunctionCode());
      buffer.put((byte) response.inputs.length);
      buffer.put(response.inputs);
    }

    /**
     * Decode a {@link ReadDiscreteInputsResponse} from a {@link ByteBuffer}.
     *
     * @param buffer the buffer to decode from.
     * @return the decoded response.
     */
    public static ReadDiscreteInputsResponse decode(ByteBuffer buffer) {
      int functionCode = buffer.get() & 0xFF;
      assert functionCode == FunctionCode.READ_DISCRETE_INPUTS.getCode();

      int byteCount = buffer.get() & 0xFF;
      var inputs = new byte[byteCount];
      buffer.get(inputs);

      return new ReadDiscreteInputsResponse(inputs);
    }
  }
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/pdu/WriteSingleCoilResponse.java`

```java
package com.digitalpetri.modbus.pdu;

import com.digitalpetri.modbus.FunctionCode;

import java.nio.ByteBuffer;

/**
 * A {@link FunctionCode#WRITE_SINGLE_COIL} response PDU.
 *
 * <p>The normal response is an echo of the request PDU, returned after the coil state has been
 * written.
 */
public record WriteSingleCoilResponse(int address, int value) implements ModbusResponsePdu {

  /**
   * @see #WriteSingleCoilResponse(int, int)
   */
  public WriteSingleCoilResponse(int address, boolean value) {
    this(address, value ? 0xFF00 : 0x0000);
  }

  @Override
  public int getFunctionCode() {
    return FunctionCode.WRITE_SINGLE_COIL.getCode();
  }

  /** Utility functions for encoding and decoding {@link WriteSingleCoilResponse}. */
  public static final class Serializer {

    private Serializer() {}

    /**
     * Encode a {@link WriteSingleCoilResponse} into a {@link ByteBuffer}.
     *
     * @param response the response to encode.
     * @param buffer the buffer to encode into.
     */
    public static void encode(WriteSingleCoilResponse response, ByteBuffer buffer) {
      buffer.put((byte) response.getFunctionCode());
      buffer.putShort((short) response.address);
      buffer.putShort((short) response.value);
    }

    /**
     * Decode a {@link WriteSingleCoilResponse} from a {@link ByteBuffer}.
     *
     * @param buffer the buffer to decode from.
     * @return the decoded response.
     */
    public static WriteSingleCoilResponse decode(ByteBuffer buffer) {
      int functionCode = buffer.get() & 0xFF;
      assert functionCode == FunctionCode.WRITE_SINGLE_COIL.getCode();

      int address = buffer.getShort() & 0xFFFF;
      int value = buffer.getShort() & 0xFFFF;

      return new WriteSingleCoilResponse(address, value);
    }
  }
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/pdu/WriteSingleCoilRequest.java`

```java
package com.digitalpetri.modbus.pdu;

import com.digitalpetri.modbus.FunctionCode;

import java.nio.ByteBuffer;

/**
 * A {@link FunctionCode#WRITE_SINGLE_COIL} request PDU.
 *
 * <p>Requests specify the address of the coil to be forced. In the PDU, coils are addressed
 * starting at 0.
 *
 * @param address the address of the coil to force. 2 bytes, range [0x0000, 0xFFFF].
 * @param value the value to force.
 */
public record WriteSingleCoilRequest(int address, int value) implements ModbusRequestPdu {

  /**
   * @see #WriteSingleCoilRequest(int, int)
   */
  public WriteSingleCoilRequest(int address, boolean value) {
    this(address, value ? 0xFF00 : 0x0000);
  }

  @Override
  public int getFunctionCode() {
    return FunctionCode.WRITE_SINGLE_COIL.getCode();
  }

  /** Utility functions for encoding and decoding {@link WriteSingleCoilRequest}. */
  public static final class Serializer {

    private Serializer() {}

    /**
     * Encode a {@link WriteSingleCoilRequest} into a {@link ByteBuffer}.
     *
     * @param request the request to encode.
     * @param buffer the buffer to encode into.
     */
    public static void encode(WriteSingleCoilRequest request, ByteBuffer buffer) {
      buffer.put((byte) request.getFunctionCode());
      buffer.putShort((short) request.address);
      buffer.putShort((short) request.value);
    }

    /**
     * Decode a {@link WriteSingleCoilRequest} from a {@link ByteBuffer}.
     *
     * @param buffer the buffer to decode from.
     * @return the decoded request.
     */
    public static WriteSingleCoilRequest decode(ByteBuffer buffer) {
      int functionCode = buffer.get() & 0xFF;
      assert functionCode == FunctionCode.WRITE_SINGLE_COIL.getCode();

      int address = buffer.getShort() & 0xFFFF;
      int value = buffer.getShort() & 0xFFFF;

      return new WriteSingleCoilRequest(address, value);
    }
  }
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/pdu/ReadCoilsRequest.java`

```java
package com.digitalpetri.modbus.pdu;

import com.digitalpetri.modbus.FunctionCode;

import java.nio.ByteBuffer;

/**
 * A {@link FunctionCode#READ_COILS} request PDU.
 *
 * <p>Requests specify the starting address, i.e. the address of the first coil specified, and the
 * number of coils. In the PDU Coils are addressed starting at 0.
 *
 * @param address the starting address. 2 bytes, range [0x0000, 0xFFFF].
 * @param quantity the quantity of coils to read. 2 bytes, range [0x0001, 0x07D0].
 */
public record ReadCoilsRequest(int address, int quantity) implements ModbusRequestPdu {

  @Override
  public int getFunctionCode() {
    return FunctionCode.READ_COILS.getCode();
  }

  /** Utility functions for encoding and decoding {@link ReadCoilsRequest}. */
  public static final class Serializer {

    private Serializer() {}

    /**
     * Encode a {@link ReadCoilsRequest} into a {@link ByteBuffer}.
     *
     * @param request the request to encode.
     * @param buffer the buffer to encode into.
     */
    public static void encode(ReadCoilsRequest request, ByteBuffer buffer) {
      buffer.put((byte) request.getFunctionCode());
      buffer.putShort((short) request.address);
      buffer.putShort((short) request.quantity);
    }

    /**
     * Decode a {@link ReadCoilsRequest} from a {@link ByteBuffer}.
     *
     * @param buffer the buffer to decode from.
     * @return the decoded request.
     */
    public static ReadCoilsRequest decode(ByteBuffer buffer) {
      int functionCode = buffer.get() & 0xFF;
      assert functionCode == FunctionCode.READ_COILS.getCode();

      int address = buffer.getShort() & 0xFFFF;
      int quantity = buffer.getShort() & 0xFFFF;

      return new ReadCoilsRequest(address, quantity);
    }
  }
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/pdu/WriteMultipleRegistersRequest.java`

```java
package com.digitalpetri.modbus.pdu;

import com.digitalpetri.modbus.FunctionCode;
import com.digitalpetri.modbus.internal.util.Hex;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * A {@link FunctionCode#WRITE_MULTIPLE_REGISTERS} request PDU.
 *
 * @param address the starting address to write to. 2 bytes, range [0x0000, 0xFFFF].
 * @param quantity the number of registers to write. 2 bytes, range [0x0001, 0x007B].
 * @param values the values to write. Must be at least {@code 2 * quantity} bytes.
 */
public record WriteMultipleRegistersRequest(int address, int quantity, byte[] values)
    implements ModbusRequestPdu {

  @Override
  public int getFunctionCode() {
    return FunctionCode.WRITE_MULTIPLE_REGISTERS.getCode();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WriteMultipleRegistersRequest that = (WriteMultipleRegistersRequest) o;
    return Objects.equals(address, that.address)
        && Objects.equals(quantity, that.quantity)
        && Arrays.equals(values, that.values);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(address, quantity);
    result = 31 * result + Arrays.hashCode(values);
    return result;
  }

  @Override
  public String toString() {
    // note: overridden to give preferred representation of `values` bytes
    return new StringJoiner(", ", WriteMultipleRegistersRequest.class.getSimpleName() + "[", "]")
        .add("address=" + address)
        .add("quantity=" + quantity)
        .add("values=" + Hex.format(values))
        .toString();
  }

  /** Utility functions for encoding and decoding {@link WriteMultipleRegistersRequest}. */
  public static final class Serializer {

    private Serializer() {}

    /**
     * Encode a {@link WriteMultipleRegistersRequest} into a {@link ByteBuffer}.
     *
     * @param request the request to encode.
     * @param buffer the buffer to encode into.
     */
    public static void encode(WriteMultipleRegistersRequest request, ByteBuffer buffer) {
      buffer.put((byte) request.getFunctionCode());
      buffer.putShort((short) request.address);
      buffer.putShort((short) request.quantity);

      buffer.put((byte) request.values.length);
      buffer.put(request.values);
    }

    /**
     * Decode a {@link WriteMultipleRegistersRequest} from a {@link ByteBuffer}.
     *
     * @param buffer the buffer to decode from.
     * @return the decoded request.
     */
    public static WriteMultipleRegistersRequest decode(ByteBuffer buffer) {
      int functionCode = buffer.get() & 0xFF;
      assert functionCode == FunctionCode.WRITE_MULTIPLE_REGISTERS.getCode();

      int address = buffer.getShort() & 0xFFFF;
      int quantity = buffer.getShort() & 0xFFFF;
      int byteCount = buffer.get() & 0xFF;

      var values = new byte[byteCount];
      buffer.get(values);

      return new WriteMultipleRegistersRequest(address, quantity, values);
    }
  }
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/pdu/ReadCoilsResponse.java`

```java
package com.digitalpetri.modbus.pdu;

import com.digitalpetri.modbus.FunctionCode;
import com.digitalpetri.modbus.internal.util.Hex;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.StringJoiner;

/**
 * A {@link FunctionCode#READ_COILS} response PDU.
 *
 * <p>The coils in the response message are packed as one coil per-bit. Status is indicated as 1=ON
 * and 0=OFF.
 *
 * <p>The LSB of the first data byte contains the output addressed in the query. The other coils
 * follow toward the high order end of this byte, and from low order to high order in subsequent
 * bytes.
 *
 * <p>If the returned output quantity is not a multiple of eight, the remaining bits in the last
 * byte will be padded with zeros (toward the high order end of the byte).
 *
 * @param coils the {@code byte[]} containing coil status, 8 coils per-byte.
 */
public record ReadCoilsResponse(byte[] coils) implements ModbusResponsePdu {

  @Override
  public int getFunctionCode() {
    return FunctionCode.READ_COILS.getCode();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ReadCoilsResponse that = (ReadCoilsResponse) o;
    return Arrays.equals(coils, that.coils);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(coils);
  }

  @Override
  public String toString() {
    // note: overridden to give preferred representation of `coils` bytes
    return new StringJoiner(", ", ReadCoilsResponse.class.getSimpleName() + "[", "]")
        .add("coils=" + Hex.format(coils))
        .toString();
  }

  /** Utility functions for encoding and decoding {@link ReadCoilsResponse}. */
  public static final class Serializer {

    private Serializer() {}

    /**
     * Encode a {@link ReadCoilsResponse} into a {@link ByteBuffer}.
     *
     * @param response the response to encode.
     * @param buffer the buffer to encode into.
     */
    public static void encode(ReadCoilsResponse response, ByteBuffer buffer) {
      buffer.put((byte) response.getFunctionCode());
      buffer.put((byte) response.coils.length);
      buffer.put(response.coils);
    }

    /**
     * Decode a {@link ReadCoilsResponse} from a {@link ByteBuffer}.
     *
     * @param buffer the buffer to decode from.
     * @return the decoded response.
     */
    public static ReadCoilsResponse decode(ByteBuffer buffer) {
      int functionCode = buffer.get() & 0xFF;
      assert functionCode == FunctionCode.READ_COILS.getCode();

      int byteCount = buffer.get() & 0xFF;
      var coils = new byte[byteCount];
      buffer.get(coils);

      return new ReadCoilsResponse(coils);
    }
  }
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/pdu/WriteMultipleCoilsResponse.java`

```java
package com.digitalpetri.modbus.pdu;

import com.digitalpetri.modbus.FunctionCode;

import java.nio.ByteBuffer;

/**
 * A {@link FunctionCode#WRITE_MULTIPLE_COILS} response PDU.
 *
 * <p>The normal response returns the function code, starting address, and quantity of coils forced.
 *
 * @param address the starting address. 2 bytes, range [0x0000, 0xFFFF].
 * @param quantity the quantity of coils to write. 2 bytes, range [0x0001, 0x7B0].
 */
public record WriteMultipleCoilsResponse(int address, int quantity) implements ModbusResponsePdu {

  @Override
  public int getFunctionCode() {
    return FunctionCode.WRITE_MULTIPLE_COILS.getCode();
  }

  /** Utility functions for encoding and decoding {@link WriteMultipleCoilsResponse}. */
  public static final class Serializer {

    private Serializer() {}

    /**
     * Encode a {@link WriteMultipleCoilsResponse} into a {@link ByteBuffer}.
     *
     * @param response the response to encode.
     * @param buffer the buffer to encode into.
     */
    public static void encode(WriteMultipleCoilsResponse response, ByteBuffer buffer) {
      buffer.put((byte) response.getFunctionCode());
      buffer.putShort((short) response.address);
      buffer.putShort((short) response.quantity);
    }

    /**
     * Decode a {@link WriteMultipleCoilsResponse} from a {@link ByteBuffer}.
     *
     * @param buffer the buffer to decode from.
     * @return the decoded response.
     */
    public static WriteMultipleCoilsResponse decode(ByteBuffer buffer) {
      int functionCode = buffer.get() & 0xFF;
      assert functionCode == FunctionCode.WRITE_MULTIPLE_COILS.getCode();

      int address = buffer.getShort() & 0xFFFF;
      int quantity = buffer.getShort() & 0xFFFF;

      return new WriteMultipleCoilsResponse(address, quantity);
    }
  }
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/pdu/WriteSingleRegisterResponse.java`

```java
package com.digitalpetri.modbus.pdu;

import com.digitalpetri.modbus.FunctionCode;

import java.nio.ByteBuffer;

/**
 * A {@link FunctionCode#WRITE_SINGLE_REGISTER} response PDU.
 *
 * @param address the address of the register written to. 2 bytes, range [0x0000, 0xFFFF].
 * @param value the value written. 2 bytes, range [0x0000, 0xFFFF].
 */
public record WriteSingleRegisterResponse(int address, int value) implements ModbusResponsePdu {

  @Override
  public int getFunctionCode() {
    return FunctionCode.WRITE_SINGLE_REGISTER.getCode();
  }

  /** Utility functions for encoding and decoding {@link WriteSingleRegisterResponse}. */
  public static final class Serializer {

    private Serializer() {}

    /**
     * Encode a {@link WriteSingleRegisterResponse} into a {@link ByteBuffer}.
     *
     * @param response the response to encode.
     * @param buffer the buffer to encode into.
     */
    public static void encode(WriteSingleRegisterResponse response, ByteBuffer buffer) {
      buffer.put((byte) response.getFunctionCode());
      buffer.putShort((short) response.address);
      buffer.putShort((short) response.value);
    }

    /**
     * Decode a {@link WriteSingleRegisterResponse} from a {@link ByteBuffer}.
     *
     * @param buffer the buffer to decode from.
     * @return the decoded response.
     */
    public static WriteSingleRegisterResponse decode(ByteBuffer buffer) {
      int functionCode = buffer.get();
      assert functionCode == FunctionCode.WRITE_SINGLE_REGISTER.getCode();

      int address = buffer.getShort() & 0xFFFF;
      int value = buffer.getShort() & 0xFFFF;

      return new WriteSingleRegisterResponse(address, value);
    }
  }
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/pdu/WriteMultipleRegistersResponse.java`

```java
package com.digitalpetri.modbus.pdu;

import com.digitalpetri.modbus.FunctionCode;

import java.nio.ByteBuffer;

/**
 * A {@link FunctionCode#WRITE_MULTIPLE_REGISTERS} response PDU.
 *
 * @param address the starting address. 2 bytes, range [0x0000, 0xFFFF].
 * @param quantity the quantity of registers written to. 2 bytes, range [0x0001, 0x007B].
 */
public record WriteMultipleRegistersResponse(int address, int quantity)
    implements ModbusResponsePdu {

  @Override
  public int getFunctionCode() {
    return FunctionCode.WRITE_MULTIPLE_REGISTERS.getCode();
  }

  /** Utility functions for encoding and decoding {@link WriteMultipleRegistersResponse}. */
  public static class Serializer {

    private Serializer() {}

    /**
     * Encode a {@link WriteMultipleRegistersResponse} into a {@link ByteBuffer}.
     *
     * @param response the response to encode.
     * @param buffer the buffer to encode into.
     */
    public static void encode(WriteMultipleRegistersResponse response, ByteBuffer buffer) {
      buffer.put((byte) response.getFunctionCode());
      buffer.putShort((short) response.address);
      buffer.putShort((short) response.quantity);
    }

    /**
     * Decode a {@link WriteMultipleRegistersResponse} from a {@link ByteBuffer}.
     *
     * @param buffer the buffer to decode from.
     * @return the decoded response.
     */
    public static WriteMultipleRegistersResponse decode(ByteBuffer buffer) {
      int functionCode = buffer.get() & 0xFF;
      assert functionCode == FunctionCode.WRITE_MULTIPLE_REGISTERS.getCode();

      int address = buffer.getShort() & 0xFFFF;
      int quantity = buffer.getShort() & 0xFFFF;

      return new WriteMultipleRegistersResponse(address, quantity);
    }
  }
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/pdu/ReadHoldingRegistersRequest.java`

```java
package com.digitalpetri.modbus.pdu;

import com.digitalpetri.modbus.FunctionCode;

import java.nio.ByteBuffer;

/**
 * A {@link FunctionCode#READ_HOLDING_REGISTERS} request PDU.
 *
 * <p>Requests specify the starting register address and the number of register to read. In the PDU,
 * registers are addressed starting at 0.
 *
 * @param address the starting address. 2 bytes, range [0x0000, 0xFFFF].
 * @param quantity the quantity of registers to read. 2 bytes, range [0x01, 0x7D].
 */
public record ReadHoldingRegistersRequest(int address, int quantity) implements ModbusRequestPdu {

  @Override
  public int getFunctionCode() {
    return FunctionCode.READ_HOLDING_REGISTERS.getCode();
  }

  /** Utility functions for encoding and decoding {@link ReadHoldingRegistersRequest}. */
  public static final class Serializer {

    private Serializer() {}

    /**
     * Encode a {@link ReadHoldingRegistersRequest} into a {@link ByteBuffer}.
     *
     * @param request the request to encode.
     * @param buffer the buffer to encode into.
     */
    public static void encode(ReadHoldingRegistersRequest request, ByteBuffer buffer) {
      buffer.put((byte) request.getFunctionCode());
      buffer.putShort((short) request.address);
      buffer.putShort((short) request.quantity);
    }

    /**
     * Decode a {@link ReadHoldingRegistersRequest} from a {@link ByteBuffer}.
     *
     * @param buffer the buffer to decode from.
     * @return the decoded request.
     */
    public static ReadHoldingRegistersRequest decode(ByteBuffer buffer) {
      int functionCode = buffer.get() & 0xFF;
      assert functionCode == FunctionCode.READ_HOLDING_REGISTERS.getCode();

      int address = buffer.getShort() & 0xFFFF;
      int quantity = buffer.getShort() & 0xFFFF;

      return new ReadHoldingRegistersRequest(address, quantity);
    }
  }
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/pdu/ReadWriteMultipleRegistersResponse.java`

```java
package com.digitalpetri.modbus.pdu;

import com.digitalpetri.modbus.FunctionCode;
import com.digitalpetri.modbus.internal.util.Hex;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.StringJoiner;

/**
 * A {@link FunctionCode#READ_WRITE_MULTIPLE_REGISTERS} response PDU.
 *
 * @param registers the register data, 2 bytes per register requested.
 */
public record ReadWriteMultipleRegistersResponse(byte[] registers) implements ModbusResponsePdu {

  @Override
  public int getFunctionCode() {
    return FunctionCode.READ_WRITE_MULTIPLE_REGISTERS.getCode();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ReadWriteMultipleRegistersResponse response = (ReadWriteMultipleRegistersResponse) o;
    return Arrays.equals(registers, response.registers);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(registers);
  }

  @Override
  public String toString() {
    // note: overridden to give preferred representation of `registers` bytes
    return new StringJoiner(
            ", ", ReadWriteMultipleRegistersResponse.class.getSimpleName() + "[", "]")
        .add("registers=" + Hex.format(registers))
        .toString();
  }

  /** Utility functions for encoding and decoding {@link ReadWriteMultipleRegistersResponse}. */
  public static final class Serializer {

    private Serializer() {}

    /**
     * Encode a {@link ReadWriteMultipleRegistersResponse} into a {@link ByteBuffer}.
     *
     * @param response the response to encode.
     * @param buffer the buffer to encode into.
     */
    public static void encode(ReadWriteMultipleRegistersResponse response, ByteBuffer buffer) {
      buffer.put((byte) response.getFunctionCode());
      buffer.put((byte) response.registers.length);
      buffer.put(response.registers);
    }

    /**
     * Decode a {@link ReadWriteMultipleRegistersResponse} from a {@link ByteBuffer}.
     *
     * @param buffer the buffer to decode from.
     * @return the decoded response.
     */
    public static ReadWriteMultipleRegistersResponse decode(ByteBuffer buffer) {
      int functionCode = buffer.get() & 0xFF;
      assert functionCode == FunctionCode.READ_WRITE_MULTIPLE_REGISTERS.getCode();

      int byteCount = buffer.get() & 0xFF;
      var registers = new byte[byteCount];
      buffer.get(registers);

      return new ReadWriteMultipleRegistersResponse(registers);
    }
  }
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/pdu/ReadWriteMultipleRegistersRequest.java`

```java
package com.digitalpetri.modbus.pdu;

import com.digitalpetri.modbus.FunctionCode;
import com.digitalpetri.modbus.internal.util.Hex;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * A {@link FunctionCode#READ_WRITE_MULTIPLE_REGISTERS} request PDU.
 *
 * @param readAddress the starting address to read from. 2 bytes, range [0x0000, 0xFFFF].
 * @param readQuantity the quantity of registers to read. 2 bytes, range [0x01, 0x7D].
 * @param writeAddress the starting address to write to. 2 bytes, range [0x0000, 0xFFFF].
 * @param writeQuantity the quantity of registers to write. 2 bytes, range [0x01, 0x79].
 * @param values the register values to write. 2 bytes per register.
 */
public record ReadWriteMultipleRegistersRequest(
    int readAddress, int readQuantity, int writeAddress, int writeQuantity, byte[] values)
    implements ModbusRequestPdu {

  @Override
  public int getFunctionCode() {
    return FunctionCode.READ_WRITE_MULTIPLE_REGISTERS.getCode();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ReadWriteMultipleRegistersRequest that = (ReadWriteMultipleRegistersRequest) o;
    return readAddress == that.readAddress
        && readQuantity == that.readQuantity
        && writeAddress == that.writeAddress
        && writeQuantity == that.writeQuantity
        && Objects.deepEquals(values, that.values);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        readAddress, readQuantity, writeAddress, writeQuantity, Arrays.hashCode(values));
  }

  @Override
  public String toString() {
    // note: overridden to give preferred representation of `values` bytes
    return new StringJoiner(
            ", ", ReadWriteMultipleRegistersRequest.class.getSimpleName() + "[", "]")
        .add("readAddress=" + readAddress)
        .add("readQuantity=" + readQuantity)
        .add("writeAddress=" + writeAddress)
        .add("writeQuantity=" + writeQuantity)
        .add("values=" + Hex.format(values))
        .toString();
  }

  /** Utility functions for encoding and decoding {@link ReadWriteMultipleRegistersRequest}. */
  public static final class Serializer {

    private Serializer() {}

    /**
     * Encode a {@link ReadWriteMultipleRegistersRequest} into a {@link ByteBuffer}.
     *
     * @param request the request to encode.
     * @param buffer the buffer to encode into.
     */
    public static void encode(ReadWriteMultipleRegistersRequest request, ByteBuffer buffer) {
      buffer.put((byte) request.getFunctionCode());
      buffer.putShort((short) request.readAddress);
      buffer.putShort((short) request.readQuantity);
      buffer.putShort((short) request.writeAddress);
      buffer.putShort((short) request.writeQuantity);
      buffer.put((byte) (2 * request.writeQuantity));
      buffer.put(request.values);
    }

    /**
     * Decode a {@link ReadWriteMultipleRegistersRequest} from a {@link ByteBuffer}.
     *
     * @param buffer the buffer to decode from.
     * @return the decoded request.
     */
    public static ReadWriteMultipleRegistersRequest decode(ByteBuffer buffer) {
      int functionCode = buffer.get() & 0xFF;
      assert functionCode == FunctionCode.READ_WRITE_MULTIPLE_REGISTERS.getCode();

      int readAddress = buffer.getShort() & 0xFFFF;
      int readQuantity = buffer.getShort() & 0xFFFF;
      int writeAddress = buffer.getShort() & 0xFFFF;
      int writeQuantity = buffer.getShort() & 0xFFFF;
      int byteCount = buffer.get() & 0xFF;
      byte[] values = new byte[byteCount];
      buffer.get(values);

      return new ReadWriteMultipleRegistersRequest(
          readAddress, readQuantity, writeAddress, writeQuantity, values);
    }
  }
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/pdu/WriteSingleRegisterRequest.java`

```java
package com.digitalpetri.modbus.pdu;

import com.digitalpetri.modbus.FunctionCode;

import java.nio.ByteBuffer;

/**
 * A {@link FunctionCode#WRITE_SINGLE_REGISTER} request PDU.
 *
 * @param address the address of the register to write. 2 bytes, range [0x0000, 0xFFFF].
 * @param value the value to write. 2 bytes, range [0x0000, 0xFFFF].
 */
public record WriteSingleRegisterRequest(int address, int value) implements ModbusRequestPdu {

  @Override
  public int getFunctionCode() {
    return FunctionCode.WRITE_SINGLE_REGISTER.getCode();
  }

  /** Utility functions for encoding and decoding {@link WriteSingleRegisterRequest}. */
  public static final class Serializer {

    private Serializer() {}

    /**
     * Encode a {@link WriteSingleRegisterRequest} into a {@link ByteBuffer}.
     *
     * @param request the request to encode.
     * @param buffer the buffer to encode into.
     */
    public static void encode(WriteSingleRegisterRequest request, ByteBuffer buffer) {
      buffer.put((byte) request.getFunctionCode());
      buffer.putShort((short) request.address);
      buffer.putShort((short) request.value);
    }

    /**
     * Decode a {@link WriteSingleRegisterRequest} from a {@link ByteBuffer}.
     *
     * @param buffer the buffer to decode from.
     * @return the decoded request.
     */
    public static WriteSingleRegisterRequest decode(ByteBuffer buffer) {
      int functionCode = buffer.get() & 0xFF;
      assert functionCode == FunctionCode.WRITE_SINGLE_REGISTER.getCode();

      int address = buffer.getShort() & 0xFFFF;
      int value = buffer.getShort() & 0xFFFF;

      return new WriteSingleRegisterRequest(address, value);
    }
  }
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/pdu/ReadHoldingRegistersResponse.java`

```java
package com.digitalpetri.modbus.pdu;

import com.digitalpetri.modbus.FunctionCode;
import com.digitalpetri.modbus.internal.util.Hex;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.StringJoiner;

/**
 * A {@link FunctionCode#READ_HOLDING_REGISTERS} response PDU.
 *
 * <p>The register data in the response is packed as 2 bytes per register.
 *
 * @param registers the register data, 2 bytes per register requested.
 */
public record ReadHoldingRegistersResponse(byte[] registers) implements ModbusResponsePdu {

  @Override
  public int getFunctionCode() {
    return FunctionCode.READ_HOLDING_REGISTERS.getCode();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ReadHoldingRegistersResponse response = (ReadHoldingRegistersResponse) o;
    return Arrays.equals(registers, response.registers);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(registers);
  }

  @Override
  public String toString() {
    // note: overridden to give preferred representation of `registers` bytes
    return new StringJoiner(", ", ReadHoldingRegistersResponse.class.getSimpleName() + "[", "]")
        .add("registers=" + Hex.format(registers))
        .toString();
  }

  /** Utility functions for encoding and decoding {@link ReadHoldingRegistersResponse}. */
  public static final class Serializer {

    private Serializer() {}

    /**
     * Encode a {@link ReadHoldingRegistersResponse} into a {@link ByteBuffer}.
     *
     * @param response the response to encode.
     * @param buffer the buffer to encode into.
     */
    public static void encode(ReadHoldingRegistersResponse response, ByteBuffer buffer) {
      buffer.put((byte) response.getFunctionCode());
      buffer.put((byte) response.registers.length);
      buffer.put(response.registers);
    }

    /**
     * Decode a {@link ReadHoldingRegistersResponse} from a {@link ByteBuffer}.
     *
     * @param buffer the buffer to decode from.
     * @return the decoded response.
     */
    public static ReadHoldingRegistersResponse decode(ByteBuffer buffer) {
      int functionCode = buffer.get() & 0xFF;
      assert functionCode == FunctionCode.READ_HOLDING_REGISTERS.getCode();

      int byteCount = buffer.get() & 0xFF;
      var registers = new byte[byteCount];
      buffer.get(registers);

      return new ReadHoldingRegistersResponse(registers);
    }
  }
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/pdu/ReadInputRegistersResponse.java`

```java
package com.digitalpetri.modbus.pdu;

import com.digitalpetri.modbus.FunctionCode;
import com.digitalpetri.modbus.internal.util.Hex;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.StringJoiner;

/**
 * A {@link FunctionCode#READ_INPUT_REGISTERS} response PDU.
 *
 * <p>The register data in the response is packed as 2 bytes per register.
 *
 * @param registers the register data, 2 bytes per register requested.
 */
public record ReadInputRegistersResponse(byte[] registers) implements ModbusResponsePdu {

  @Override
  public int getFunctionCode() {
    return FunctionCode.READ_INPUT_REGISTERS.getCode();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ReadInputRegistersResponse that = (ReadInputRegistersResponse) o;
    return Arrays.equals(registers, that.registers);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(registers);
  }

  @Override
  public String toString() {
    // note: overridden to give preferred representation of `registers` bytes
    return new StringJoiner(", ", ReadInputRegistersResponse.class.getSimpleName() + "[", "]")
        .add("registers=" + Hex.format(registers))
        .toString();
  }

  /** Utility functions for encoding and decoding {@link ReadInputRegistersResponse}. */
  public static final class Serializer {

    private Serializer() {}

    /**
     * Encode a {@link ReadInputRegistersResponse} into a {@link ByteBuffer}.
     *
     * @param response the response to encode.
     * @param buffer the buffer to encode into.
     */
    public static void encode(ReadInputRegistersResponse response, ByteBuffer buffer) {
      buffer.put((byte) response.getFunctionCode());
      buffer.put((byte) response.registers.length);
      buffer.put(response.registers);
    }

    /**
     * Decode a {@link ReadInputRegistersResponse} from a {@link ByteBuffer}.
     *
     * @param buffer the buffer to decode from.
     * @return the decoded response.
     */
    public static ReadInputRegistersResponse decode(ByteBuffer buffer) {
      int functionCode = buffer.get() & 0xFF;
      assert functionCode == FunctionCode.READ_INPUT_REGISTERS.getCode();

      int byteCount = buffer.get() & 0xFF;
      var registers = new byte[byteCount];
      buffer.get(registers);

      return new ReadInputRegistersResponse(registers);
    }
  }
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/pdu/MaskWriteRegisterResponse.java`

```java
package com.digitalpetri.modbus.pdu;

import com.digitalpetri.modbus.FunctionCode;

import java.nio.ByteBuffer;

/**
 * A {@link FunctionCode#MASK_WRITE_REGISTER} response PDU.
 *
 * @param address the address, 2 bytes, range [0x0000, 0xFFFF].
 * @param andMask the AND mask, 2 bytes, range [0x0000, 0xFFFF].
 * @param orMask the OR mask, 2 bytes, range [0x0000, 0xFFFF].
 */
public record MaskWriteRegisterResponse(int address, int andMask, int orMask)
    implements ModbusResponsePdu {

  @Override
  public int getFunctionCode() {
    return FunctionCode.MASK_WRITE_REGISTER.getCode();
  }

  /** Utility functions for encoding and decoding {@link MaskWriteRegisterResponse}. */
  public static final class Serializer {

    private Serializer() {}

    /**
     * Encode a {@link MaskWriteRegisterResponse} into a {@link ByteBuffer}.
     *
     * @param response the response to encode.
     * @param buffer the buffer to encode into.
     */
    public static void encode(MaskWriteRegisterResponse response, ByteBuffer buffer) {
      buffer.put((byte) response.getFunctionCode());
      buffer.putShort((short) response.address);
      buffer.putShort((short) response.andMask);
      buffer.putShort((short) response.orMask);
    }

    /**
     * Decode a {@link MaskWriteRegisterResponse} from a {@link ByteBuffer}.
     *
     * @param buffer the buffer to decode from.
     * @return the decoded response.
     */
    public static MaskWriteRegisterResponse decode(ByteBuffer buffer) {
      int functionCode = buffer.get() & 0xFF;
      assert functionCode == FunctionCode.MASK_WRITE_REGISTER.getCode();

      int address = buffer.getShort() & 0xFFFF;
      int andMask = buffer.getShort() & 0xFFFF;
      int orMask = buffer.getShort() & 0xFFFF;

      return new MaskWriteRegisterResponse(address, andMask, orMask);
    }
  }
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/pdu/ModbusResponsePdu.java`

```java
package com.digitalpetri.modbus.pdu;

/** Super-interface for Modbus response PDUs. */
public interface ModbusResponsePdu extends ModbusPdu {}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/ModbusPduSerializer.java`

```java
package com.digitalpetri.modbus;

import com.digitalpetri.modbus.exceptions.ModbusException;
import com.digitalpetri.modbus.pdu.*;

import java.nio.ByteBuffer;

public interface ModbusPduSerializer {

  /**
   * Encode a Modbus PDU into a {@link ByteBuffer}.
   *
   * @param pdu the PDU object.
   * @param buffer the buffer to encode into.
   * @throws Exception if an error occurs during encoding.
   */
  void encode(ModbusPdu pdu, ByteBuffer buffer) throws Exception;

  /**
   * Decode a Modbus PDU from a {@link ByteBuffer}.
   *
   * @param functionCode the function code of the PDU to decode.
   * @param buffer the buffer to decode from.
   * @return the decoded PDU object.
   * @throws Exception if error occurs during decoding.
   */
  ModbusPdu decode(int functionCode, ByteBuffer buffer) throws Exception;

  class DefaultRequestSerializer implements ModbusPduSerializer {

    /**
     * A shared instance of {@link DefaultRequestSerializer}.
     *
     * <p>This instance is stateless and therefore safe for concurrent use by any number of threads.
     */
    public static final DefaultRequestSerializer INSTANCE = new DefaultRequestSerializer();

    @Override
    public void encode(ModbusPdu pdu, ByteBuffer buffer) throws ModbusException {
      switch (pdu.getFunctionCode()) {
        case 0x01 -> {
          if (pdu instanceof ReadCoilsRequest request) {
            ReadCoilsRequest.Serializer.encode(request, buffer);
          } else {
            throw new IllegalArgumentException("expected ReadCoilsRequest");
          }
        }
        case 0x02 -> {
          if (pdu instanceof ReadDiscreteInputsRequest request) {
            ReadDiscreteInputsRequest.Serializer.encode(request, buffer);
          } else {
            throw new IllegalArgumentException("expected ReadDiscreteInputsRequest");
          }
        }
        case 0x03 -> {
          if (pdu instanceof ReadHoldingRegistersRequest request) {
            ReadHoldingRegistersRequest.Serializer.encode(request, buffer);
          } else {
            throw new IllegalArgumentException("expected ReadHoldingRegistersRequest");
          }
        }
        case 0x04 -> {
          if (pdu instanceof ReadInputRegistersRequest request) {
            ReadInputRegistersRequest.Serializer.encode(request, buffer);
          } else {
            throw new IllegalArgumentException("expected ReadInputRegistersRequest");
          }
        }
        case 0x05 -> {
          if (pdu instanceof WriteSingleCoilRequest request) {
            WriteSingleCoilRequest.Serializer.encode(request, buffer);
          } else {
            throw new IllegalArgumentException("expected WriteSingleCoilRequest");
          }
        }
        case 0x06 -> {
          if (pdu instanceof WriteSingleRegisterRequest request) {
            WriteSingleRegisterRequest.Serializer.encode(request, buffer);
          } else {
            throw new IllegalArgumentException("expected WriteSingleRegisterRequest");
          }
        }
        case 0x0F -> {
          if (pdu instanceof WriteMultipleCoilsRequest request) {
            WriteMultipleCoilsRequest.Serializer.encode(request, buffer);
          } else {
            throw new IllegalArgumentException("expected WriteMultipleCoilsRequest");
          }
        }
        case 0x10 -> {
          if (pdu instanceof WriteMultipleRegistersRequest request) {
            WriteMultipleRegistersRequest.Serializer.encode(request, buffer);
          } else {
            throw new IllegalArgumentException("expected WriteMultipleRegistersRequest");
          }
        }
        case 0x16 -> {
          if (pdu instanceof MaskWriteRegisterRequest request) {
            MaskWriteRegisterRequest.Serializer.encode(request, buffer);
          } else {
            throw new IllegalArgumentException("expected MaskWriteRegisterRequest");
          }
        }
        case 0x17 -> {
          if (pdu instanceof ReadWriteMultipleRegistersRequest request) {
            ReadWriteMultipleRegistersRequest.Serializer.encode(request, buffer);
          } else {
            throw new IllegalArgumentException("expected ReadWriteMultipleRegistersRequest");
          }
        }
        default ->
            throw new ModbusException(
                "no serializer for functionCode=0x%02X".formatted(pdu.getFunctionCode()));
      }
    }

    @Override
    public ModbusPdu decode(int functionCode, ByteBuffer buffer) throws ModbusException {
      return switch (functionCode) {
        case 0x01 -> ReadCoilsRequest.Serializer.decode(buffer);
        case 0x02 -> ReadDiscreteInputsRequest.Serializer.decode(buffer);
        case 0x03 -> ReadHoldingRegistersRequest.Serializer.decode(buffer);
        case 0x04 -> ReadInputRegistersRequest.Serializer.decode(buffer);
        case 0x05 -> WriteSingleCoilRequest.Serializer.decode(buffer);
        case 0x06 -> WriteSingleRegisterRequest.Serializer.decode(buffer);
        case 0x0F -> WriteMultipleCoilsRequest.Serializer.decode(buffer);
        case 0x10 -> WriteMultipleRegistersRequest.Serializer.decode(buffer);
        case 0x16 -> MaskWriteRegisterRequest.Serializer.decode(buffer);
        case 0x17 -> ReadWriteMultipleRegistersRequest.Serializer.decode(buffer);
        default ->
            throw new ModbusException(
                "no serializer for functionCode=0x%02X".formatted(functionCode));
      };
    }
  }

  class DefaultResponseSerializer implements ModbusPduSerializer {

    /**
     * A shared instance of {@link DefaultResponseSerializer}.
     *
     * <p>This instance is stateless and therefore safe for concurrent use by any number of threads.
     */
    public static final DefaultResponseSerializer INSTANCE = new DefaultResponseSerializer();

    @Override
    public void encode(ModbusPdu pdu, ByteBuffer buffer) throws ModbusException {
      switch (pdu.getFunctionCode()) {
        case 0x01 -> {
          if (pdu instanceof ReadCoilsResponse response) {
            ReadCoilsResponse.Serializer.encode(response, buffer);
          } else {
            throw new IllegalArgumentException("expected ReadCoilsResponse");
          }
        }
        case 0x02 -> {
          if (pdu instanceof ReadDiscreteInputsResponse response) {
            ReadDiscreteInputsResponse.Serializer.encode(response, buffer);
          } else {
            throw new IllegalArgumentException("expected ReadDiscreteInputsResponse");
          }
        }
        case 0x03 -> {
          if (pdu instanceof ReadHoldingRegistersResponse response) {
            ReadHoldingRegistersResponse.Serializer.encode(response, buffer);
          } else {
            throw new IllegalArgumentException("expected ReadHoldingRegistersResponse");
          }
        }
        case 0x04 -> {
          if (pdu instanceof ReadInputRegistersResponse response) {
            ReadInputRegistersResponse.Serializer.encode(response, buffer);
          } else {
            throw new IllegalArgumentException("expected ReadInputRegistersResponse");
          }
        }
        case 0x05 -> {
          if (pdu instanceof WriteSingleCoilResponse response) {
            WriteSingleCoilResponse.Serializer.encode(response, buffer);
          } else {
            throw new IllegalArgumentException("expected WriteSingleCoilResponse");
          }
        }
        case 0x06 -> {
          if (pdu instanceof WriteSingleRegisterResponse response) {
            WriteSingleRegisterResponse.Serializer.encode(response, buffer);
          } else {
            throw new IllegalArgumentException("expected WriteSingleRegisterResponse");
          }
        }
        case 0x0F -> {
          if (pdu instanceof WriteMultipleCoilsResponse response) {
            WriteMultipleCoilsResponse.Serializer.encode(response, buffer);
          } else {
            throw new IllegalArgumentException("expected WriteMultipleCoilsResponse");
          }
        }
        case 0x10 -> {
          if (pdu instanceof WriteMultipleRegistersResponse response) {
            WriteMultipleRegistersResponse.Serializer.encode(response, buffer);
          } else {
            throw new IllegalArgumentException("expected WriteMultipleRegistersResponse");
          }
        }
        case 0x16 -> {
          if (pdu instanceof MaskWriteRegisterResponse response) {
            MaskWriteRegisterResponse.Serializer.encode(response, buffer);
          } else {
            throw new IllegalArgumentException("expected MaskWriteRegisterResponse");
          }
        }
        case 0x17 -> {
          if (pdu instanceof ReadWriteMultipleRegistersResponse response) {
            ReadWriteMultipleRegistersResponse.Serializer.encode(response, buffer);
          } else {
            throw new IllegalArgumentException("expected ReadWriteMultipleRegistersResponse");
          }
        }
        default ->
            throw new ModbusException(
                "no serializer for functionCode=0x%02X".formatted(pdu.getFunctionCode()));
      }
    }

    @Override
    public ModbusPdu decode(int functionCode, ByteBuffer buffer) throws ModbusException {
      return switch (functionCode) {
        case 0x01 -> ReadCoilsResponse.Serializer.decode(buffer);
        case 0x02 -> ReadDiscreteInputsResponse.Serializer.decode(buffer);
        case 0x03 -> ReadHoldingRegistersResponse.Serializer.decode(buffer);
        case 0x04 -> ReadInputRegistersResponse.Serializer.decode(buffer);
        case 0x05 -> WriteSingleCoilResponse.Serializer.decode(buffer);
        case 0x06 -> WriteSingleRegisterResponse.Serializer.decode(buffer);
        case 0x0F -> WriteMultipleCoilsResponse.Serializer.decode(buffer);
        case 0x10 -> WriteMultipleRegistersResponse.Serializer.decode(buffer);
        case 0x16 -> MaskWriteRegisterResponse.Serializer.decode(buffer);
        case 0x17 -> ReadWriteMultipleRegistersResponse.Serializer.decode(buffer);
        default ->
            throw new ModbusException(
                "no serializer for functionCode=0x%02X".formatted(functionCode));
      };
    }
  }
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/ModbusRtuRequestFrameParser.java`

```java
package com.digitalpetri.modbus;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;

public class ModbusRtuRequestFrameParser {

  private final AtomicReference<ParserState> state = new AtomicReference<>(new Idle());

  /**
   * Parse incoming data and return the updated {@link ParserState}.
   *
   * @param data the incoming data to parse.
   * @return the updated {@link ParserState}.
   */
  public ParserState parse(byte[] data) {
    return state.updateAndGet(s -> s.parse(data));
  }

  /**
   * Get the current {@link ParserState}.
   *
   * @return the current {@link ParserState}.
   */
  public ParserState getState() {
    return state.get();
  }

  /** Reset this parser to the {@link Idle} state. */
  public ParserState reset() {
    return state.getAndSet(new Idle());
  }

  public sealed interface ParserState permits Idle, Accumulating, Accumulated, ParseError {

    ParserState parse(byte[] data);
  }

  /** Waiting to receive initial data. */
  public record Idle() implements ParserState {

    @Override
    public ParserState parse(byte[] data) {
      var accumulating = new Accumulating(ByteBuffer.allocate(256), -1);

      return accumulating.parse(data);
    }
  }

  public record Accumulating(ByteBuffer buffer, int expectedLength) implements ParserState {

    @Override
    public ParserState parse(byte[] data) {
      buffer.put(data);

      int readableBytes = buffer.position();

      if (readableBytes < 3 || readableBytes < expectedLength) {
        return this;
      }

      byte fcb = buffer.get(1);

      switch (fcb & 0xFF) {
        case 0x01, 0x02, 0x03, 0x04, 0x05, 0x06 -> {
          int fixedLength = 1 + (1 + 2 + 2) + 2;
          if (readableBytes >= fixedLength) {
            ModbusRtuFrame frame = readFrame(buffer.flip(), fixedLength);
            return new Accumulated(frame);
          } else {
            return new Accumulating(buffer, fixedLength);
          }
        }

        case 0x0F, 0x10 -> {
          int minimum = 1 + (1 + 2 + 2 + 1) + 2;
          if (readableBytes >= minimum) {
            int byteCount = buffer.get(6);
            if (readableBytes >= minimum + byteCount) {
              ModbusRtuFrame frame = readFrame(buffer.flip(), minimum + byteCount);
              return new Accumulated(frame);
            } else {
              return new Accumulating(buffer, minimum + byteCount);
            }
          } else {
            return new Accumulating(buffer, minimum);
          }
        }

        case 0x16 -> {
          int fixedLength = 1 + (1 + 2 + 2 + 2) + 2;
          if (readableBytes >= fixedLength) {
            ModbusRtuFrame frame = readFrame(buffer.flip(), fixedLength);
            return new Accumulated(frame);
          } else {
            return new Accumulating(buffer, fixedLength);
          }
        }

        case 0x17 -> {
          int minimum = 1 + (1 + 2 + 2 + 2 + 2 + 1) + 2;
          if (readableBytes >= minimum) {
            int byteCount = buffer.get(10);
            if (readableBytes >= minimum + byteCount) {
              ModbusRtuFrame frame = readFrame(buffer.flip(), minimum + byteCount);
              return new Accumulated(frame);
            } else {
              return new Accumulating(buffer, minimum + byteCount);
            }
          } else {
            return new Accumulating(buffer, minimum);
          }
        }

        default -> {
          return new ParseError(buffer, "unsupported function code: 0x%02X".formatted(fcb));
        }
      }
    }

    private static ModbusRtuFrame readFrame(ByteBuffer buffer, int length) {
      int slaveId = buffer.get() & 0xFF;

      ByteBuffer payload = buffer.slice(buffer.position(), length - 3);
      ByteBuffer crc = buffer.slice(buffer.position() + length - 3, 2);

      return new ModbusRtuFrame(slaveId, payload, crc);
    }
  }

  public record Accumulated(ModbusRtuFrame frame) implements ParserState {

    @Override
    public ParserState parse(byte[] data) {
      return this;
    }
  }

  public record ParseError(ByteBuffer buffer, String message) implements ParserState {

    @Override
    public ParserState parse(byte[] data) {
      buffer.put(data);
      return this;
    }
  }
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/MbapHeader.java`

```java
package com.digitalpetri.modbus;

import java.nio.ByteBuffer;

/**
 * Modbus Application Protocol header for frames that encapsulates Modbus request and response PDUs
 * on TCP/IP.
 *
 * @param transactionId transaction identifier. 2 bytes, identifies a request/response transaction.
 * @param protocolId protocol identifier. 2 bytes, always 0 for Modbus protocol.
 * @param length number of bytes that follow, including 1 for the unit id. 2 bytes.
 * @param unitId identifier of a remote slave connected on a physical or logical other bus. 1 byte.
 */
public record MbapHeader(int transactionId, int protocolId, int length, int unitId) {

  /** Utility functions for encoding and decoding {@link MbapHeader}. */
  public static class Serializer {

    private Serializer() {}

    /**
     * Encode a {@link MbapHeader} into a {@link ByteBuffer}.
     *
     * @param header the header to encode.
     * @param buffer the buffer to encode into.
     */
    public static void encode(MbapHeader header, ByteBuffer buffer) {
      buffer.putShort((short) header.transactionId);
      buffer.putShort((short) header.protocolId);
      buffer.putShort((short) header.length);
      buffer.put((byte) header.unitId);
    }

    /**
     * Decode a {@link MbapHeader} from a {@link ByteBuffer}.
     *
     * @param buffer the buffer to decode from.
     * @return the decoded header.
     */
    public static MbapHeader decode(ByteBuffer buffer) {
      int transactionId = buffer.getShort() & 0xFFFF;
      int protocolId = buffer.getShort() & 0xFFFF;
      int length = buffer.getShort() & 0xFFFF;
      int unitId = buffer.get() & 0xFF;

      return new MbapHeader(transactionId, protocolId, length, unitId);
    }
  }
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/TimeoutScheduler.java`

```java
package com.digitalpetri.modbus;

import com.digitalpetri.modbus.internal.util.ExecutionQueue;

import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public interface TimeoutScheduler {

  TimeoutHandle newTimeout(Task task, long delay, TimeUnit unit);

  interface Task {

    void run(TimeoutHandle handle);
  }

  interface TimeoutHandle {

    void cancel();

    boolean isCancelled();
  }

  static TimeoutScheduler create(Executor executor, ScheduledExecutorService scheduledExecutor) {
    return (task, delay, unit) -> {
      final var ref = new AtomicReference<ScheduledFuture<?>>();
      final ExecutionQueue queue = new ExecutionQueue(executor);

      var handle =
          new TimeoutHandle() {
            @Override
            public void cancel() {
              synchronized (ref) {
                ref.get().cancel(false);
              }
            }

            @Override
            public boolean isCancelled() {
              synchronized (ref) {
                return ref.get().isCancelled();
              }
            }
          };

      synchronized (ref) {
        ScheduledFuture<?> future =
            scheduledExecutor.schedule(() -> queue.submit(() -> task.run(handle)), delay, unit);
        ref.set(future);
      }

      return handle;
    };
  }
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/exceptions/UnknownUnitIdException.java`

```java
package com.digitalpetri.modbus.exceptions;

import java.io.Serial;

public class UnknownUnitIdException extends ModbusException {

  @Serial private static final long serialVersionUID = 58792353863854093L;

  public UnknownUnitIdException(int unitId) {
    super("unknown unit id: " + unitId);
  }

  public UnknownUnitIdException(int unitId, Throwable cause) {
    super("unknown unit id: " + unitId, cause);
  }
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/exceptions/ModbusTimeoutException.java`

```java
package com.digitalpetri.modbus.exceptions;

import java.io.Serial;

public class ModbusTimeoutException extends ModbusException {

  @Serial private static final long serialVersionUID = -8643809775979891078L;

  public ModbusTimeoutException(String message) {
    super(message);
  }

  public ModbusTimeoutException(Throwable cause) {
    super(cause);
  }

  public ModbusTimeoutException(String message, Throwable cause) {
    super(message, cause);
  }
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/exceptions/ModbusResponseException.java`

```java
package com.digitalpetri.modbus.exceptions;

import com.digitalpetri.modbus.ExceptionCode;
import com.digitalpetri.modbus.FunctionCode;

import java.io.Serial;

public class ModbusResponseException extends ModbusException {

  @Serial private static final long serialVersionUID = -4058366691447836220L;

  private final int functionCode;
  private final int exceptionCode;

  public ModbusResponseException(int functionCode, int exceptionCode) {
    super(createMessage(functionCode, exceptionCode));

    this.functionCode = functionCode;
    this.exceptionCode = exceptionCode;
  }

  public ModbusResponseException(FunctionCode functionCode, ExceptionCode exceptionCode) {
    this(functionCode.getCode(), exceptionCode.getCode());
  }

  /**
   * @return the function code that generated the exception response.
   */
  public int getFunctionCode() {
    return functionCode;
  }

  /**
   * @return the exception code indicated in the exception response.
   */
  public int getExceptionCode() {
    return exceptionCode;
  }

  private static String createMessage(int functionCode, int exceptionCode) {
    String fcs = FunctionCode.from(functionCode).map(Enum::toString).orElse("UNKNOWN");
    String ecs = ExceptionCode.from(exceptionCode).map(Enum::toString).orElse("UNKNOWN");

    return "0x%02X [%s] generated exception response 0x%02X [%s]"
        .formatted(functionCode, fcs, exceptionCode, ecs);
  }
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/exceptions/ModbusExecutionException.java`

```java
package com.digitalpetri.modbus.exceptions;

import java.io.Serial;

public class ModbusExecutionException extends ModbusException {

  @Serial private static final long serialVersionUID = 8407528717209895345L;

  public ModbusExecutionException(String message) {
    super(message);
  }

  public ModbusExecutionException(Throwable cause) {
    super(cause);
  }

  public ModbusExecutionException(String message, Throwable cause) {
    super(message, cause);
  }
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/exceptions/ModbusConnectException.java`

```java
package com.digitalpetri.modbus.exceptions;

import java.io.Serial;

public class ModbusConnectException extends ModbusException {

  @Serial private static final long serialVersionUID = -5350159787088895451L;

  public ModbusConnectException(String message) {
    super(message);
  }

  public ModbusConnectException(Throwable cause) {
    super(cause);
  }

  public ModbusConnectException(String message, Throwable cause) {
    super(message, cause);
  }
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/exceptions/ModbusException.java`

```java
package com.digitalpetri.modbus.exceptions;

import java.io.Serial;

public class ModbusException extends Exception {

  @Serial private static final long serialVersionUID = 5355236996267676988L;

  public ModbusException(String message) {
    super(message);
  }

  public ModbusException(Throwable cause) {
    super(cause);
  }

  public ModbusException(String message, Throwable cause) {
    super(message, cause);
  }
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/exceptions/ModbusCrcException.java`

```java
package com.digitalpetri.modbus.exceptions;

import com.digitalpetri.modbus.ModbusRtuFrame;

import java.io.Serial;

public class ModbusCrcException extends ModbusException {

  @Serial private static final long serialVersionUID = -5350159787088895451L;

  private final ModbusRtuFrame frame;

  public ModbusCrcException(ModbusRtuFrame frame) {
    super("CRC mismatch");

    this.frame = frame;
  }

  /**
   * Get the frame that caused the exception.
   *
   * @return the frame that caused the exception.
   */
  public ModbusRtuFrame getFrame() {
    return frame;
  }
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/internal/util/package-info.java`

```java
/**
 * Internal classes used by the library.
 *
 * <p>These classes are not part of the public API and should not be used by clients.
 */
package com.digitalpetri.modbus.internal.util;

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/internal/util/BufferPool.java`

```java
package com.digitalpetri.modbus.internal.util;

import java.nio.ByteBuffer;
import java.util.Deque;
import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicLong;

public abstract class BufferPool implements AutoCloseable {

  private static final int QUEUE_SIZE = 3;

  private final Map<Integer, AtomicLong> allocationCounts = new ConcurrentHashMap<>();
  private final Map<Integer, AtomicLong> rejectionCounts = new ConcurrentHashMap<>();

  private final NavigableMap<Integer, Deque<ByteBuffer>> buffers = new ConcurrentSkipListMap<>();

  public void give(ByteBuffer buffer) {
    Deque<ByteBuffer> queue =
        buffers.computeIfAbsent(buffer.capacity(), k -> new LinkedBlockingDeque<>(QUEUE_SIZE));
    if (!queue.offer(buffer)) {
      rejectionCounts.computeIfAbsent(buffer.capacity(), k -> new AtomicLong()).incrementAndGet();
    }
  }

  public ByteBuffer take(int capacity) {
    var entry = buffers.ceilingEntry(capacity);

    if (entry != null) {
      Deque<ByteBuffer> queue = entry.getValue();
      ByteBuffer buffer = queue.poll();

      if (buffer != null) {
        return buffer.clear().limit(capacity);
      } else {
        return allocate(capacity);
      }
    } else {
      return allocate(capacity);
    }
  }

  @Override
  public void close() {
    buffers.clear();
    allocationCounts.clear();
    rejectionCounts.clear();
  }

  public Map<Integer, AtomicLong> getAllocationCounts() {
    return allocationCounts;
  }

  public Map<Integer, AtomicLong> getRejectionCounts() {
    return rejectionCounts;
  }

  protected final ByteBuffer allocate(int capacity) {
    allocationCounts.computeIfAbsent(capacity, k -> new AtomicLong()).incrementAndGet();
    return create(capacity);
  }

  protected abstract ByteBuffer create(int capacity);

  public static class HeapBufferPool extends BufferPool {

    @Override
    protected ByteBuffer create(int capacity) {
      return ByteBuffer.allocate(capacity);
    }

    @Override
    public void give(ByteBuffer buffer) {
      assert !buffer.isDirect();
      super.give(buffer);
    }
  }

  public static class DirectBufferPool extends BufferPool {

    @Override
    protected ByteBuffer create(int capacity) {
      return ByteBuffer.allocateDirect(capacity);
    }

    @Override
    public void give(ByteBuffer buffer) {
      assert buffer.isDirect();
      super.give(buffer);
    }
  }

  public static class NoOpBufferPool extends BufferPool {

    @Override
    protected ByteBuffer create(int capacity) {
      return ByteBuffer.allocate(capacity);
    }

    @Override
    public void give(ByteBuffer buffer) {}

    @Override
    public ByteBuffer take(int capacity) {
      return allocate(capacity);
    }
  }
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/internal/util/Hex.java`

```java
package com.digitalpetri.modbus.internal.util;

import java.nio.ByteBuffer;
import java.util.HexFormat;

public class Hex {

  private Hex() {}

  /**
   * Format a {@link ByteBuffer} as a hex string.
   *
   * <p>Only the bytes between {@link ByteBuffer#position()} and {@link ByteBuffer#limit()} are
   * considered.
   *
   * @param buffer the buffer to format.
   * @return the formatted hex string.
   */
  public static String format(ByteBuffer buffer) {
    StringBuilder sb = new StringBuilder();
    for (int i = buffer.position(); i < buffer.limit(); i++) {
      sb.append(String.format("%02x", buffer.get(i)));
    }
    return sb.toString();
  }

  /**
   * Format a byte array as a hex string.
   *
   * @param bytes the bytes to format.
   * @return the formatted hex string.
   */
  public static String format(byte[] bytes) {
    return HexFormat.of().formatHex(bytes);
  }
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/internal/util/ExecutionQueue.java`

```java
package com.digitalpetri.modbus.internal.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.concurrent.Executor;

/**
 * Queues up submitted {@link Runnable}s and executes on an {@link Executor}, with optional
 * concurrency.
 *
 * <p>When {@code concurrency = 1} (the default) submitted tasks are guaranteed to run serially and
 * in the order submitted.
 *
 * <p>When {@code concurrency > 1} there are no guarantees beyond the fact that tasks are still
 * pulled from a queue to be executed.
 */
public class ExecutionQueue {

  private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionQueue.class);

  private final Object queueLock = new Object();
  private final ArrayDeque<Runnable> queue = new ArrayDeque<>();

  private int pending = 0;
  private boolean paused = false;

  private final Executor executor;
  private final int concurrencyLimit;

  public ExecutionQueue(Executor executor) {
    this(executor, 1);
  }

  public ExecutionQueue(Executor executor, int concurrencyLimit) {
    this.executor = executor;
    this.concurrencyLimit = concurrencyLimit;
  }

  /**
   * Submit a {@link Runnable} to be executed.
   *
   * @param runnable the {@link Runnable} to be executed.
   */
  public void submit(Runnable runnable) {
    synchronized (queueLock) {
      queue.add(runnable);

      maybePollAndExecute();
    }
  }

  /**
   * Submit a {@link Runnable} to be executed at the head of the queue.
   *
   * @param runnable the {@link Runnable} to be executed.
   */
  public void submitToHead(Runnable runnable) {
    synchronized (queueLock) {
      queue.addFirst(runnable);

      maybePollAndExecute();
    }
  }

  /** Pause execution of queued {@link Runnable}s. */
  public void pause() {
    synchronized (queueLock) {
      paused = true;
    }
  }

  /** Resume execution of queued {@link Runnable}s. */
  public void resume() {
    synchronized (queueLock) {
      paused = false;

      maybePollAndExecute();
    }
  }

  private void maybePollAndExecute() {
    synchronized (queueLock) {
      if (pending < concurrencyLimit && !paused && !queue.isEmpty()) {
        executor.execute(new Task(queue.poll()));
        pending++;
      }
    }
  }

  private class Task implements Runnable {

    private final Runnable runnable;

    Task(Runnable runnable) {
      if (runnable == null) {
        throw new NullPointerException("runnable");
      }

      this.runnable = runnable;
    }

    @Override
    public void run() {
      try {
        runnable.run();
      } catch (Throwable throwable) {
        LOGGER.warn("Uncaught Throwable during execution", throwable);
      }

      InlineTask inlineTask = null;

      synchronized (queueLock) {
        if (queue.isEmpty() || paused) {
          pending--;
        } else {
          // pending count remains the same
          inlineTask = new InlineTask(queue.poll());
        }
      }

      if (inlineTask != null) {
        inlineTask.run();
      }
    }
  }

  private class InlineTask implements Runnable {

    private final Runnable runnable;

    InlineTask(Runnable runnable) {
      if (runnable == null) {
        throw new NullPointerException("runnable");
      }

      this.runnable = runnable;
    }

    @Override
    public void run() {
      try {
        runnable.run();
      } catch (Throwable throwable) {
        LOGGER.warn("Uncaught Throwable during execution", throwable);
      }

      synchronized (queueLock) {
        if (queue.isEmpty() || paused) {
          pending--;
        } else {
          // pending count remains the same
          executor.execute(new Task(queue.poll()));
        }
      }
    }
  }
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/Modbus.java`

```java
/*
 * Copyright 2016 Kevin Herron
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.digitalpetri.modbus;

import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Shared resources that, if not otherwise provided, can be used as defaults.
 *
 * <p>These resources should be released when the JVM is shutting down or the ClassLoader that
 * loaded this library is unloaded.
 *
 * <p>See {@link #releaseSharedResources()}.
 */
public final class Modbus {

  private Modbus() {}

  private static ExecutorService EXECUTOR_SERVICE;
  private static ScheduledExecutorService SCHEDULED_EXECUTOR_SERVICE;

  /**
   * @return a shared {@link ExecutorService}.
   */
  public static synchronized ExecutorService sharedExecutor() {
    if (EXECUTOR_SERVICE == null) {
      ThreadFactory threadFactory =
          new ThreadFactory() {
            private final AtomicLong threadNumber = new AtomicLong(0L);

            @Override
            public Thread newThread(Runnable r) {
              Thread thread =
                  new Thread(r, "modbus-shared-thread-pool-" + threadNumber.getAndIncrement());
              thread.setDaemon(true);
              thread.setUncaughtExceptionHandler(
                  (t, e) ->
                      LoggerFactory.getLogger(Modbus.class)
                          .warn("Uncaught Exception on shared stack ExecutorService thread", e));
              return thread;
            }
          };

      EXECUTOR_SERVICE = Executors.newCachedThreadPool(threadFactory);
    }

    return EXECUTOR_SERVICE;
  }

  /**
   * @return a shared {@link ScheduledExecutorService}.
   */
  public static synchronized ScheduledExecutorService sharedScheduledExecutor() {
    if (SCHEDULED_EXECUTOR_SERVICE == null) {
      ThreadFactory threadFactory =
          new ThreadFactory() {
            private final AtomicLong threadNumber = new AtomicLong(0L);

            @Override
            public Thread newThread(Runnable r) {
              Thread thread =
                  new Thread(
                      r, "modbus-shared-scheduled-executor-" + threadNumber.getAndIncrement());
              thread.setDaemon(true);
              thread.setUncaughtExceptionHandler(
                  (t, e) ->
                      LoggerFactory.getLogger(Modbus.class)
                          .warn(
                              "Uncaught Exception on shared stack ScheduledExecutorService thread",
                              e));
              return thread;
            }
          };

      var executor = new ScheduledThreadPoolExecutor(1, threadFactory);

      executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);

      SCHEDULED_EXECUTOR_SERVICE = executor;
    }

    return SCHEDULED_EXECUTOR_SERVICE;
  }

  /**
   * Release shared resources, waiting at most 5 seconds for each of the shared resources to shut
   * down gracefully.
   *
   * @see #releaseSharedResources(long, TimeUnit)
   */
  public static synchronized void releaseSharedResources() {
    releaseSharedResources(5, TimeUnit.SECONDS);
  }

  /**
   * Release shared resources, waiting at most the specified timeout for each of the shared
   * resources to shut down gracefully.
   *
   * @param timeout the duration of the timeout.
   * @param unit the unit of the timeout duration.
   */
  public static synchronized void releaseSharedResources(long timeout, TimeUnit unit) {
    if (EXECUTOR_SERVICE != null) {
      EXECUTOR_SERVICE.shutdown();
    }

    if (SCHEDULED_EXECUTOR_SERVICE != null) {
      SCHEDULED_EXECUTOR_SERVICE.shutdown();
    }

    if (EXECUTOR_SERVICE != null) {
      try {
        if (!EXECUTOR_SERVICE.awaitTermination(timeout, unit)) {
          LoggerFactory.getLogger(Modbus.class)
              .warn("ExecutorService not shut down after {} {}.", timeout, unit);
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        LoggerFactory.getLogger(Modbus.class)
            .warn("Interrupted awaiting executor service shutdown", e);
      }
      EXECUTOR_SERVICE = null;
    }

    if (SCHEDULED_EXECUTOR_SERVICE != null) {
      try {
        if (!SCHEDULED_EXECUTOR_SERVICE.awaitTermination(timeout, unit)) {
          LoggerFactory.getLogger(Modbus.class)
              .warn("ScheduledExecutorService not shut down after {} {}.", timeout, unit);
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        LoggerFactory.getLogger(Modbus.class)
            .warn("Interrupted awaiting scheduled executor service shutdown", e);
      }
      SCHEDULED_EXECUTOR_SERVICE = null;
    }
  }
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/ModbusTcpFrame.java`

```java
package com.digitalpetri.modbus;

import com.digitalpetri.modbus.internal.util.Hex;

import java.nio.ByteBuffer;
import java.util.StringJoiner;

/**
 * Modbus/TCP frame data, an {@link MbapHeader} and encoded PDU.
 *
 * @param header the {@link MbapHeader} for this frame.
 * @param pdu the encoded Modbus PDU data.
 */
public record ModbusTcpFrame(MbapHeader header, ByteBuffer pdu) {

  @Override
  public String toString() {
    // note: overridden to give preferred representation of `pdu` bytes
    return new StringJoiner(", ", ModbusTcpFrame.class.getSimpleName() + "[", "]")
        .add("header=" + header)
        .add("pdu=" + Hex.format(pdu))
        .toString();
  }
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/ModbusRtuFrame.java`

```java
package com.digitalpetri.modbus;

import com.digitalpetri.modbus.internal.util.Hex;

import java.nio.ByteBuffer;
import java.util.StringJoiner;

/**
 * Modbus/RTU frame data, a unit id and encoded PDU.
 *
 * @param unitId the identifier of a remote slave connected on a physical or logical other bus.
 * @param pdu the encoded Modbus PDU data.
 * @param crc the CRC bytes.
 */
public record ModbusRtuFrame(int unitId, ByteBuffer pdu, ByteBuffer crc) {

  @Override
  public String toString() {
    // note: overridden to give preferred representation of `pdu` and `crc` bytes
    return new StringJoiner(", ", ModbusRtuFrame.class.getSimpleName() + "[", "]")
        .add("unitId=" + unitId)
        .add("pdu=" + Hex.format(pdu))
        .add("crc=" + Hex.format(crc))
        .toString();
  }
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/ModbusRtuResponseFrameParser.java`

```java
package com.digitalpetri.modbus;

import com.digitalpetri.modbus.internal.util.Hex;

import java.nio.ByteBuffer;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicReference;

public class ModbusRtuResponseFrameParser {

  private final AtomicReference<ParserState> state = new AtomicReference<>(new Idle());

  /**
   * Parse incoming data and return the updated {@link ParserState}.
   *
   * @param data the incoming data to parse.
   * @return the updated {@link ParserState}.
   */
  public ParserState parse(byte[] data) {
    return state.updateAndGet(s -> s.parse(data));
  }

  /**
   * Get the current {@link ParserState}.
   *
   * @return the current {@link ParserState}.
   */
  public ParserState getState() {
    return state.get();
  }

  /** Reset this parser to the {@link Idle} state. */
  public ParserState reset() {
    return state.getAndSet(new Idle());
  }

  public sealed interface ParserState permits Idle, Accumulating, Accumulated, ParseError {

    ParserState parse(byte[] data);
  }

  /** Waiting to receive initial data. */
  public record Idle() implements ParserState {

    @Override
    public ParserState parse(byte[] data) {
      var accumulating = new Accumulating(ByteBuffer.allocate(256), -1);

      return accumulating.parse(data);
    }
  }

  /**
   * Holds a {@link ByteBuffer} that accumulates data and, if known, the expected total length.
   *
   * @param buffer the buffer to accumulate data in.
   * @param expectedLength the expected total length; -1 if not yet known.
   */
  public record Accumulating(ByteBuffer buffer, int expectedLength) implements ParserState {

    @Override
    public ParserState parse(byte[] data) {
      buffer.put(data);

      int readableBytes = buffer.position();

      if (readableBytes < 3 || readableBytes < expectedLength) {
        return this;
      }

      byte fcb = buffer.get(1);

      switch (fcb & 0xFF) {
        case 0x01, 0x02, 0x03, 0x04, 0x17 -> {
          int count = buffer.get(2) & 0xFF;
          int calculatedLength = count + 5;

          if (readableBytes >= calculatedLength) {
            ModbusRtuFrame frame = readFrame(buffer.flip(), calculatedLength);
            return new Accumulated(frame);
          } else {
            return new Accumulating(buffer, calculatedLength);
          }
        }
        case 0x05, 0x06, 0x0F, 0x10 -> {
          // the body of each of these is 4 bytes, so we know the total length
          int fixedLength = 8;
          if (readableBytes >= fixedLength) {
            ModbusRtuFrame frame = readFrame(buffer.flip(), fixedLength);
            return new Accumulated(frame);
          } else {
            return new Accumulating(buffer, fixedLength);
          }
        }
        case 0x16 -> {
          int fixedLength = 10;
          if (readableBytes >= fixedLength) {
            ModbusRtuFrame frame = readFrame(buffer.flip(), fixedLength);
            return new Accumulated(frame);
          } else {
            return new Accumulating(buffer, fixedLength);
          }
        }
        case 0x81, 0x82, 0x83, 0x84, 0x85, 0x86, 0x8F, 0x90, 0x96 -> {
          // error response for one of the supported function codes
          int fixedLength = 5;
          if (readableBytes >= fixedLength) {
            ModbusRtuFrame frame = readFrame(buffer.flip(), fixedLength);
            return new Accumulated(frame);
          } else {
            return new Accumulating(buffer, fixedLength);
          }
        }
        default -> {
          return new ParseError(buffer, "unsupported function code: 0x%02X".formatted(fcb));
        }
      }
    }

    private static ModbusRtuFrame readFrame(ByteBuffer buffer, int length) {
      int slaveId = buffer.get() & 0xFF;

      ByteBuffer payload = buffer.slice(buffer.position(), length - 3);
      ByteBuffer crc = buffer.slice(buffer.position() + length - 3, 2);

      return new ModbusRtuFrame(slaveId, payload, crc);
    }

    @Override
    public String toString() {
      int limit = buffer.limit();
      int position = buffer.position();
      buffer.flip();
      try {
        return new StringJoiner(", ", Accumulating.class.getSimpleName() + "[", "]")
            .add("buffer=" + Hex.format(buffer))
            .add("expectedLength=" + expectedLength)
            .toString();
      } finally {
        buffer.limit(limit).position(position);
      }
    }
  }

  /**
   * Contains an accumulated {@link ModbusRtuFrame}, with a PDU of some function code we understand
   * enough to parse. The CRC has not been validated.
   *
   * @param frame the accumulated {@link ModbusRtuFrame}.
   */
  public record Accumulated(ModbusRtuFrame frame) implements ParserState {

    @Override
    public ParserState parse(byte[] data) {
      return this;
    }
  }

  /**
   * Parser received a function code it doesn't recognize.
   *
   * @param buffer the accumulated data buffer.
   * @param error a message describing the error.
   */
  public record ParseError(ByteBuffer buffer, String error) implements ParserState {

    @Override
    public ParserState parse(byte[] data) {
      buffer.put(data);
      return this;
    }
  }
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/FunctionCode.java`

```java
package com.digitalpetri.modbus;

import java.util.Optional;

public enum FunctionCode {

  /** Function Code 0x01 - Read Coils. */
  READ_COILS(0x01),

  /** Function Code 0x02 - Read Discrete Inputs. */
  READ_DISCRETE_INPUTS(0x02),

  /** Function Code 0x03 - Read Holding Registers. */
  READ_HOLDING_REGISTERS(0x03),

  /** Function Code 0x04 - Read Input Registers. */
  READ_INPUT_REGISTERS(0x04),

  /** Function Code 0x05 - Write Single Coil. */
  WRITE_SINGLE_COIL(0x05),

  /** Function Code 0x06 - Write Single Register. */
  WRITE_SINGLE_REGISTER(0x06),

  /** Function Code 0x07 - Read Exception Status. */
  READ_EXCEPTION_STATUS(0x07),

  /** Function Code 0x08 - Diagnostics. */
  DIAGNOSTICS(0x08),

  /** Function Code 0x0B - Get Comm Event Counter. */
  GET_COMM_EVENT_COUNTER(0x0B),

  /** Function Code 0x0C - Get Comm Event Log. */
  GET_COMM_EVENT_LOG(0x0C),

  /** Function Code 0x0F - Write Multiple Coils. */
  WRITE_MULTIPLE_COILS(0x0F),

  /** Function Code 0x10 - Write Multiple Registers. */
  WRITE_MULTIPLE_REGISTERS(0x10),

  /** Function Code 0x11 - Report Slave Id. */
  REPORT_SLAVE_ID(0x11),

  /** Function Code 0x14 - Read File Record. */
  READ_FILE_RECORD(0x14),

  /** Function Code 0x15 - Write File Record. */
  WRITE_FILE_RECORD(0x15),

  /** Function Code 0x16 - Mask Write Register. */
  MASK_WRITE_REGISTER(0x16),

  /** Function Code 0x17 - Read/Write Multiple Registers. */
  READ_WRITE_MULTIPLE_REGISTERS(0x17),

  /** Function Code 0x18 - Read FIFO Queue. */
  READ_FIFO_QUEUE(0x18),

  /** Function Code 0x2B - Encapsulated Interface Transport. */
  ENCAPSULATED_INTERFACE_TRANSPORT(0x2B);

  FunctionCode(int code) {
    this.code = code;
  }

  private final int code;

  public int getCode() {
    return code;
  }

  /**
   * Look up the corresponding {@link FunctionCode} for {@code code}.
   *
   * @param code the function code to look up.
   * @return the corresponding {@link FunctionCode} for {@code code}.
   */
  public static Optional<FunctionCode> from(int code) {
    FunctionCode fc =
        switch (code) {
          case 0x01 -> READ_COILS;
          case 0x02 -> READ_DISCRETE_INPUTS;
          case 0x03 -> READ_HOLDING_REGISTERS;
          case 0x04 -> READ_INPUT_REGISTERS;
          case 0x05 -> WRITE_SINGLE_COIL;
          case 0x06 -> WRITE_SINGLE_REGISTER;
          case 0x07 -> READ_EXCEPTION_STATUS;
          case 0x08 -> DIAGNOSTICS;
          case 0x0B -> GET_COMM_EVENT_COUNTER;
          case 0x0C -> GET_COMM_EVENT_LOG;
          case 0x0F -> WRITE_MULTIPLE_COILS;
          case 0x10 -> WRITE_MULTIPLE_REGISTERS;
          case 0x11 -> REPORT_SLAVE_ID;
          case 0x14 -> READ_FILE_RECORD;
          case 0x15 -> WRITE_FILE_RECORD;
          case 0x16 -> MASK_WRITE_REGISTER;
          case 0x17 -> READ_WRITE_MULTIPLE_REGISTERS;
          case 0x18 -> READ_FIFO_QUEUE;
          case 0x2B -> ENCAPSULATED_INTERFACE_TRANSPORT;
          default -> null;
        };

    return Optional.ofNullable(fc);
  }
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/ExceptionResponse.java`

```java
package com.digitalpetri.modbus;

/**
 * Modbus Exception Response.
 *
 * @param functionCode the {@link FunctionCode} that elicited this response.
 * @param exceptionCode the {@link ExceptionCode} indicated by the outstation.
 */
public record ExceptionResponse(FunctionCode functionCode, ExceptionCode exceptionCode) {}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/client/ModbusClientTransport.java`

```java
package com.digitalpetri.modbus.client;

import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

public interface ModbusClientTransport<T> {

  /**
   * Connect this transport.
   *
   * @return a {@link CompletionStage} that completes when the transport has been connected.
   */
  CompletionStage<Void> connect();

  /**
   * Disconnect this transport.
   *
   * @return a {@link CompletionStage} that completes when the transport has been disconnected.
   */
  CompletionStage<Void> disconnect();

  /**
   * Check if the transport is connected.
   *
   * @return {@code true} if the transport is connected.
   */
  boolean isConnected();

  /**
   * Send a request frame to the transport.
   *
   * @param frame the request frame to send.
   * @return a {@link CompletionStage} that completes when the frame has been sent.
   */
  CompletionStage<Void> send(T frame);

  /**
   * Configure a callback to receive response frames from the transport.
   *
   * @param frameReceiver the callback to response receive frames.
   */
  void receive(Consumer<T> frameReceiver);
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/client/ModbusClientConfig.java`

```java
package com.digitalpetri.modbus.client;

import com.digitalpetri.modbus.Modbus;
import com.digitalpetri.modbus.ModbusPduSerializer;
import com.digitalpetri.modbus.TimeoutScheduler;

import java.time.Duration;
import java.util.function.Consumer;

import static com.digitalpetri.modbus.ModbusPduSerializer.DefaultRequestSerializer;
import static com.digitalpetri.modbus.ModbusPduSerializer.DefaultResponseSerializer;

/**
 * Configuration for a {@link ModbusClient}.
 *
 * @param requestTimeout the timeout duration for requests.
 * @param timeoutScheduler the {@link TimeoutScheduler} used to schedule request timeouts.
 * @param requestSerializer the {@link ModbusPduSerializer} used to encode requests.
 * @param responseSerializer the {@link ModbusPduSerializer} used to decode responses.
 */
public record ModbusClientConfig(
    Duration requestTimeout,
    TimeoutScheduler timeoutScheduler,
    ModbusPduSerializer requestSerializer,
    ModbusPduSerializer responseSerializer) {

  /**
   * Create a new {@link ModbusClientConfig} instance.
   *
   * @param configure a callback that accepts a {@link Builder} used to configure the new instance.
   * @return a new {@link ModbusClientConfig} instance.
   */
  public static ModbusClientConfig create(Consumer<Builder> configure) {
    var builder = new Builder();
    configure.accept(builder);
    return builder.build();
  }

  public static class Builder {

    /** The timeout duration for requests. */
    public Duration requestTimeout = Duration.ofSeconds(5);

    /** The {@link TimeoutScheduler} used to schedule request timeouts. */
    public TimeoutScheduler timeoutScheduler;

    /** The {@link ModbusPduSerializer} used to encode outgoing requests. */
    public ModbusPduSerializer requestSerializer = DefaultRequestSerializer.INSTANCE;

    /** The {@link ModbusPduSerializer} used to decode incoming responses. */
    public ModbusPduSerializer responseSerializer = DefaultResponseSerializer.INSTANCE;

    /**
     * Set the timeout duration for requests.
     *
     * @param requestTimeout the request timeout.
     * @return this {@link Builder}.
     */
    public Builder setRequestTimeout(Duration requestTimeout) {
      this.requestTimeout = requestTimeout;
      return this;
    }

    /**
     * Set the {@link TimeoutScheduler} used to schedule request timeouts.
     *
     * @param timeoutScheduler the timeout scheduler.
     * @return this {@link Builder}.
     */
    public Builder setTimeoutScheduler(TimeoutScheduler timeoutScheduler) {
      this.timeoutScheduler = timeoutScheduler;
      return this;
    }

    /**
     * Set the {@link ModbusPduSerializer} used to encode outgoing requests.
     *
     * @param requestSerializer the request serializer.
     * @return this {@link Builder}.
     */
    public Builder setRequestSerializer(ModbusPduSerializer requestSerializer) {
      this.requestSerializer = requestSerializer;
      return this;
    }

    /**
     * Set the {@link ModbusPduSerializer} used to decode incoming responses.
     *
     * @param responseSerializer the response serializer.
     * @return this {@link Builder}.
     */
    public Builder setResponseSerializer(ModbusPduSerializer responseSerializer) {
      this.responseSerializer = responseSerializer;
      return this;
    }

    /**
     * @return a new {@link ModbusClientConfig} instance.
     */
    public ModbusClientConfig build() {
      if (timeoutScheduler == null) {
        timeoutScheduler =
            TimeoutScheduler.create(Modbus.sharedExecutor(), Modbus.sharedScheduledExecutor());
      }

      return new ModbusClientConfig(
          requestTimeout, timeoutScheduler, requestSerializer, responseSerializer);
    }
  }
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/client/ModbusClient.java`

```java
package com.digitalpetri.modbus.client;

import com.digitalpetri.modbus.exceptions.ModbusExecutionException;
import com.digitalpetri.modbus.exceptions.ModbusResponseException;
import com.digitalpetri.modbus.exceptions.ModbusTimeoutException;
import com.digitalpetri.modbus.pdu.*;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public abstract class ModbusClient {

  private final ModbusClientTransport<?> transport;

  ModbusClient(ModbusClientTransport<?> transport) {
    this.transport = transport;
  }

  /**
   * Get the {@link ModbusClientTransport} used by this client.
   *
   * @return the {@link ModbusClientTransport} used by this client.
   */
  public ModbusClientTransport<?> getTransport() {
    return transport;
  }

  /**
   * Connect the underlying transport.
   *
   * @throws ModbusExecutionException if the connection fails.
   */
  public void connect() throws ModbusExecutionException {
    try {
      transport.connect().toCompletableFuture().get();
    } catch (ExecutionException e) {
      throw new ModbusExecutionException(e.getCause());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ModbusExecutionException(e);
    }
  }

  /**
   * Connect the underlying transport asynchronously.
   *
   * @return a {@link CompletionStage} that completes successfully when the connection is
   *     established, or completes exceptionally if the connection fails.
   */
  public CompletionStage<Void> connectAsync() {
    return transport.connect();
  }

  /**
   * Alias for {@link #connect()} that returns {@link ModbusClientAutoCloseable} for use in
   * try-with-resources blocks.
   *
   * @return a {@link ModbusClientAutoCloseable} instance that disconnects this client when closed.
   * @throws ModbusExecutionException if the disconnection fails.
   */
  public ModbusClientAutoCloseable open() throws ModbusExecutionException {
    connect();
    return this::disconnect;
  }

  /**
   * Disconnect the underlying transport.
   *
   * @throws ModbusExecutionException if the disconnection fails.
   */
  public void disconnect() throws ModbusExecutionException {
    try {
      transport.disconnect().toCompletableFuture().get();
    } catch (ExecutionException e) {
      throw new ModbusExecutionException(e.getCause());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ModbusExecutionException(e);
    }
  }

  /**
   * Disconnect the underlying transport asynchronously.
   *
   * @return a {@link CompletionStage} that completes successfully when the connection is closed, or
   *     completes exceptionally if the disconnection fails.
   */
  public CompletionStage<Void> disconnectAsync() {
    return transport.disconnect();
  }

  /**
   * Check if this client is connected i.e. the underlying transport is connected.
   *
   * @return {@code true} if this client is connected.
   */
  public boolean isConnected() {
    return transport.isConnected();
  }

  /**
   * Send a {@link ModbusRequestPdu} PDU to the remote device identified by {@code unitId}.
   *
   * @param unitId the remote device unit id.
   * @param request the request PDU.
   * @return the {@link ModbusResponsePdu} PDU.
   * @throws ModbusExecutionException if any unexpected execution error occurs.
   * @throws ModbusResponseException if the remote device responds with an error.
   * @throws ModbusTimeoutException if the request times out.
   */
  public ModbusResponsePdu send(int unitId, ModbusRequestPdu request)
      throws ModbusExecutionException, ModbusResponseException, ModbusTimeoutException {

    try {
      return sendAsync(unitId, request).toCompletableFuture().get();
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof TimeoutException ex) {
        throw new ModbusTimeoutException(ex);
      } else if (cause instanceof ModbusResponseException ex) {
        throw ex;
      } else {
        throw new ModbusExecutionException(cause);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ModbusExecutionException(e);
    }
  }

  /**
   * Send a {@link ModbusRequestPdu} PDU to the remote device identified by {@code unitId}.
   *
   * @param unitId the remote device unit id.
   * @param request the request PDU.
   * @return a {@link CompletionStage} that completes successfully with the {@link
   *     ModbusResponsePdu} PDU, or completes exceptionally if an error occurs.
   */
  public abstract CompletionStage<ModbusResponsePdu> sendAsync(
      int unitId, ModbusRequestPdu request);

  // region Read Coils (function code 0x01)

  /**
   * Send a {@link ReadCoilsRequest} (FC 0x01) to the remote device identified by {@code unitId}.
   *
   * @param unitId the remote device unit id.
   * @param request the {@link ReadCoilsRequest}.
   * @return the {@link ReadCoilsResponse}.
   * @throws ModbusExecutionException if any unexpected execution error occurs.
   * @throws ModbusResponseException if the remote device responds with an error.
   * @throws ModbusTimeoutException if the request times out.
   */
  public ReadCoilsResponse readCoils(int unitId, ReadCoilsRequest request)
      throws ModbusExecutionException, ModbusResponseException, ModbusTimeoutException {

    ModbusResponsePdu response = send(unitId, request);

    if (response instanceof ReadCoilsResponse r) {
      return r;
    } else {
      throw new ModbusExecutionException(
          "unexpected response: 0x%02X".formatted(response.getFunctionCode()));
    }
  }

  /**
   * Send a {@link ReadCoilsRequest} (FC 0x01) to the remote device identified by {@code unitId}.
   *
   * @param unitId the remote device unit id.
   * @param request the {@link ReadCoilsRequest}.
   * @return a {@link CompletionStage} that completes successfully with the {@link
   *     ReadCoilsResponse}, or completes exceptionally if an error occurs.
   */
  public CompletionStage<ReadCoilsResponse> readCoilsAsync(int unitId, ReadCoilsRequest request) {

    return sendAsync(unitId, request).thenApply(ReadCoilsResponse.class::cast);
  }

  // endregion

  // region Read Discrete Inputs (function code 0x02)

  /**
   * Send a {@link ReadDiscreteInputsRequest} (FC 0x02) to the remote device identified by {@code
   * unitId}.
   *
   * @param unitId the remote device unit id.
   * @param request the {@link ReadDiscreteInputsRequest}.
   * @return the {@link ReadDiscreteInputsResponse}.
   * @throws ModbusExecutionException if any unexpected execution error occurs.
   * @throws ModbusResponseException if the remote device responds with an error.
   * @throws ModbusTimeoutException if the request times out.
   */
  public ReadDiscreteInputsResponse readDiscreteInputs(
      int unitId, ReadDiscreteInputsRequest request)
      throws ModbusExecutionException, ModbusResponseException, ModbusTimeoutException {

    ModbusResponsePdu response = send(unitId, request);

    if (response instanceof ReadDiscreteInputsResponse r) {
      return r;
    } else {
      throw new ModbusExecutionException(
          "unexpected response: 0x%02X".formatted(response.getFunctionCode()));
    }
  }

  /**
   * Send a {@link ReadDiscreteInputsRequest} (FC 0x02) to the remote device identified by {@code
   * unitId}.
   *
   * @param unitId the remote device unit id.
   * @param request the {@link ReadDiscreteInputsRequest}.
   * @return a {@link CompletionStage} that completes successfully with the {@link
   *     ReadDiscreteInputsResponse}, or completes exceptionally if an error occurs.
   */
  public CompletionStage<ReadDiscreteInputsResponse> readDiscreteInputsAsync(
      int unitId, ReadDiscreteInputsRequest request) {

    return sendAsync(unitId, request).thenApply(ReadDiscreteInputsResponse.class::cast);
  }

  // endregion

  // region Read Holding Registers (function code 0x03)

  /**
   * Send a {@link ReadHoldingRegistersRequest} (FC 0x03) to the remote device identified by {@code
   * unitId}.
   *
   * @param unitId the remote device unit id.
   * @param request the {@link ReadHoldingRegistersRequest}.
   * @return the {@link ReadHoldingRegistersResponse}.
   * @throws ModbusExecutionException if any unexpected execution error occurs.
   * @throws ModbusResponseException if the remote device responds with an error.
   * @throws ModbusTimeoutException if the request times out.
   */
  public ReadHoldingRegistersResponse readHoldingRegisters(
      int unitId, ReadHoldingRegistersRequest request)
      throws ModbusExecutionException, ModbusResponseException, ModbusTimeoutException {

    ModbusResponsePdu response = send(unitId, request);

    if (response instanceof ReadHoldingRegistersResponse r) {
      return r;
    } else {
      throw new ModbusExecutionException(
          "unexpected response: 0x%02X".formatted(response.getFunctionCode()));
    }
  }

  /**
   * Send a {@link ReadHoldingRegistersRequest} (FC 0x03) to the remote device identified by {@code
   * unitId}.
   *
   * @param unitId the remote device unit id.
   * @param request the {@link ReadHoldingRegistersRequest}.
   * @return a {@link CompletionStage} that completes successfully with the {@link
   *     ReadHoldingRegistersResponse}, or completes exceptionally if an error occurs.
   */
  public CompletionStage<ReadHoldingRegistersResponse> readHoldingRegistersAsync(
      int unitId, ReadHoldingRegistersRequest request) {

    return sendAsync(unitId, request).thenApply(ReadHoldingRegistersResponse.class::cast);
  }

  // endregion

  // region Read Input Registers (function code 0x04)

  /**
   * Send a {@link ReadInputRegistersRequest} (FC 0x04) to the remote device identified by {@code
   * unitId}.
   *
   * @param unitId the remote device unit id.
   * @param request the {@link ReadInputRegistersRequest}.
   * @return the {@link ReadInputRegistersResponse}.
   * @throws ModbusExecutionException if any unexpected execution error occurs.
   * @throws ModbusResponseException if the remote device responds with an error.
   * @throws ModbusTimeoutException if the request times out.
   */
  public ReadInputRegistersResponse readInputRegisters(
      int unitId, ReadInputRegistersRequest request)
      throws ModbusExecutionException, ModbusResponseException, ModbusTimeoutException {

    ModbusResponsePdu response = send(unitId, request);

    if (response instanceof ReadInputRegistersResponse r) {
      return r;
    } else {
      throw new ModbusExecutionException(
          "unexpected response: 0x%02X".formatted(response.getFunctionCode()));
    }
  }

  /**
   * Send a {@link ReadInputRegistersRequest} (FC 0x04) to the remote device identified by {@code
   * unitId}.
   *
   * @param unitId the remote device unit id.
   * @param request the {@link ReadInputRegistersRequest}.
   * @return a {@link CompletionStage} that completes successfully with the {@link
   *     ReadInputRegistersResponse}, or completes exceptionally if an error occurs.
   */
  public CompletionStage<ReadInputRegistersResponse> readInputRegistersAsync(
      int unitId, ReadInputRegistersRequest request) {

    return sendAsync(unitId, request).thenApply(ReadInputRegistersResponse.class::cast);
  }

  // endregion

  // region Write Single Coil (0x05)

  /**
   * Send a {@link WriteSingleCoilRequest} (FC 0x05) to the remote device identified by {@code
   * unitId}.
   *
   * @param unitId the remote device unit id.
   * @param request the {@link WriteSingleCoilRequest}.
   * @return the {@link WriteSingleCoilResponse}.
   * @throws ModbusExecutionException if any unexpected execution error occurs.
   * @throws ModbusResponseException if the remote device responds with an error.
   * @throws ModbusTimeoutException if the request times out.
   */
  public WriteSingleCoilResponse writeSingleCoil(int unitId, WriteSingleCoilRequest request)
      throws ModbusExecutionException, ModbusResponseException, ModbusTimeoutException {

    ModbusResponsePdu response = send(unitId, request);

    if (response instanceof WriteSingleCoilResponse r) {
      return r;
    } else {
      throw new ModbusExecutionException(
          "unexpected response: 0x%02X".formatted(response.getFunctionCode()));
    }
  }

  /**
   * Send a {@link WriteSingleCoilRequest} (FC 0x05) to the remote device identified by {@code
   * unitId}.
   *
   * @param unitId the remote device unit id.
   * @param request the {@link WriteSingleCoilRequest}.
   * @return a {@link CompletionStage} that completes successfully with the {@link
   *     WriteSingleCoilResponse}, or completes exceptionally if an error occurs.
   */
  public CompletionStage<WriteSingleCoilResponse> writeSingleCoilAsync(
      int unitId, WriteSingleCoilRequest request) {

    return sendAsync(unitId, request).thenApply(WriteSingleCoilResponse.class::cast);
  }

  // endregion

  // region Write Single Register (0x06)

  /**
   * Send a {@link WriteSingleRegisterRequest} (FC 0x06) to the remote device identified by {@code
   * unitId}.
   *
   * @param unitId the remote device unit id.
   * @param request the {@link WriteSingleRegisterRequest}.
   * @return the {@link WriteSingleRegisterResponse}.
   * @throws ModbusExecutionException if any unexpected execution error occurs.
   * @throws ModbusResponseException if the remote device responds with an error.
   * @throws ModbusTimeoutException if the request times out.
   */
  public WriteSingleRegisterResponse writeSingleRegister(
      int unitId, WriteSingleRegisterRequest request)
      throws ModbusExecutionException, ModbusResponseException, ModbusTimeoutException {

    ModbusResponsePdu response = send(unitId, request);

    if (response instanceof WriteSingleRegisterResponse r) {
      return r;
    } else {
      throw new ModbusExecutionException(
          "unexpected response: 0x%02X".formatted(response.getFunctionCode()));
    }
  }

  /**
   * Send a {@link WriteSingleRegisterRequest} (FC 0x06) to the remote device identified by {@code
   * unitId}.
   *
   * @param unitId the remote device unit id.
   * @param request the {@link WriteSingleRegisterRequest}.
   * @return a {@link CompletionStage} that completes successfully with the {@link
   *     WriteSingleRegisterResponse}, or completes exceptionally if an error occurs.
   */
  public CompletionStage<WriteSingleRegisterResponse> writeSingleRegisterAsync(
      int unitId, WriteSingleRegisterRequest request) {

    return sendAsync(unitId, request).thenApply(WriteSingleRegisterResponse.class::cast);
  }

  // endregion

  // region Write Multiple Coils (0x0F)

  /**
   * Send a {@link WriteMultipleCoilsRequest} (FC 0x0F) to the remote device identified by {@code
   * unitId}.
   *
   * @param unitId the remote device unit id.
   * @param request the {@link WriteMultipleCoilsRequest}.
   * @return the {@link WriteMultipleCoilsResponse}.
   * @throws ModbusExecutionException if any unexpected execution error occurs.
   * @throws ModbusResponseException if the remote device responds with an error.
   * @throws ModbusTimeoutException if the request times out.
   */
  public WriteMultipleCoilsResponse writeMultipleCoils(
      int unitId, WriteMultipleCoilsRequest request)
      throws ModbusExecutionException, ModbusResponseException, ModbusTimeoutException {

    ModbusResponsePdu response = send(unitId, request);

    if (response instanceof WriteMultipleCoilsResponse r) {
      return r;
    } else {
      throw new ModbusExecutionException(
          "unexpected response: 0x%02X".formatted(response.getFunctionCode()));
    }
  }

  /**
   * Send a {@link WriteMultipleCoilsRequest} (FC 0x0F) to the remote device identified by {@code
   * unitId}.
   *
   * @param unitId the remote device unit id.
   * @param request the {@link WriteMultipleCoilsRequest}.
   * @return a {@link CompletionStage} that completes successfully with the {@link
   *     WriteMultipleCoilsResponse}, or completes exceptionally if an error occurs.
   */
  public CompletionStage<WriteMultipleCoilsResponse> writeMultipleCoilsAsync(
      int unitId, WriteMultipleCoilsRequest request) {

    return sendAsync(unitId, request).thenApply(WriteMultipleCoilsResponse.class::cast);
  }

  // endregion

  // region Write Multiple Registers (0x10)

  /**
   * Send a {@link WriteMultipleRegistersRequest} (FC 0x10) to the remote device identified by
   * {@code unitId}.
   *
   * @param unitId the remote device unit id.
   * @param request the {@link WriteMultipleRegistersRequest}.
   * @return the {@link WriteMultipleRegistersResponse}.
   * @throws ModbusExecutionException if any unexpected execution error occurs.
   * @throws ModbusResponseException if the remote device responds with an error.
   * @throws ModbusTimeoutException if the request times out.
   */
  public WriteMultipleRegistersResponse writeMultipleRegisters(
      int unitId, WriteMultipleRegistersRequest request)
      throws ModbusExecutionException, ModbusResponseException, ModbusTimeoutException {

    ModbusResponsePdu response = send(unitId, request);

    if (response instanceof WriteMultipleRegistersResponse r) {
      return r;
    } else {
      throw new ModbusExecutionException(
          "unexpected response: 0x%02X".formatted(response.getFunctionCode()));
    }
  }

  /**
   * Send a {@link WriteMultipleRegistersRequest} (FC 0x10) to the remote device identified by
   * {@code unitId}.
   *
   * @param unitId the remote device unit id.
   * @param request the {@link WriteMultipleRegistersRequest}.
   * @return a {@link CompletionStage} that completes successfully with the {@link
   *     WriteMultipleRegistersResponse}, or completes exceptionally if an error occurs.
   */
  public CompletionStage<WriteMultipleRegistersResponse> writeMultipleRegistersAsync(
      int unitId, WriteMultipleRegistersRequest request) {

    return sendAsync(unitId, request).thenApply(WriteMultipleRegistersResponse.class::cast);
  }

  // endregion

  // region Mask Write Register (0x16)

  /**
   * Send a {@link MaskWriteRegisterRequest} (FC 0x16) to the remote device identified by {@code
   * unitId}.
   *
   * @param unitId the remote device unit id.
   * @param request the {@link MaskWriteRegisterRequest}.
   * @return the {@link MaskWriteRegisterResponse}.
   * @throws ModbusExecutionException if any unexpected execution error occurs.
   * @throws ModbusResponseException if the remote device responds with an error.
   * @throws ModbusTimeoutException if the request times out.
   */
  public MaskWriteRegisterResponse maskWriteRegister(int unitId, MaskWriteRegisterRequest request)
      throws ModbusExecutionException, ModbusResponseException, ModbusTimeoutException {

    ModbusResponsePdu response = send(unitId, request);

    if (response instanceof MaskWriteRegisterResponse r) {
      return r;
    } else {
      throw new ModbusExecutionException(
          "unexpected response: 0x%02X".formatted(response.getFunctionCode()));
    }
  }

  /**
   * Send a {@link MaskWriteRegisterRequest} (FC 0x16) to the remote device identified by {@code
   * unitId}.
   *
   * @param unitId the remote device unit id.
   * @param request the {@link MaskWriteRegisterRequest}.
   * @return a {@link CompletionStage} that completes successfully with the {@link
   *     MaskWriteRegisterResponse}, or completes exceptionally if an error occurs.
   */
  public CompletionStage<MaskWriteRegisterResponse> maskWriteRegisterAsync(
      int unitId, MaskWriteRegisterRequest request) {

    return sendAsync(unitId, request).thenApply(MaskWriteRegisterResponse.class::cast);
  }

  // endregion

  // region ReadWriteMultipleRegisters (0x17)

  /**
   * Send a {@link ReadWriteMultipleRegistersRequest} (FC 0x17) to the remote device identified by
   * {@code unitId}.
   *
   * @param unitId the remote device unit id.
   * @param request the {@link ReadWriteMultipleRegistersRequest}.
   * @return the {@link ReadWriteMultipleRegistersResponse}.
   * @throws ModbusExecutionException if any unexpected execution error occurs.
   * @throws ModbusResponseException if the remote device responds with an error.
   * @throws ModbusTimeoutException if the request times out.
   */
  public ReadWriteMultipleRegistersResponse readWriteMultipleRegisters(
      int unitId, ReadWriteMultipleRegistersRequest request)
      throws ModbusExecutionException, ModbusResponseException, ModbusTimeoutException {

    ModbusResponsePdu response = send(unitId, request);

    if (response instanceof ReadWriteMultipleRegistersResponse r) {
      return r;
    } else {
      throw new ModbusExecutionException(
          "unexpected response: 0x%02X".formatted(response.getFunctionCode()));
    }
  }

  /**
   * Send a {@link ReadWriteMultipleRegistersRequest} (FC 0x17) to the remote device identified by
   * {@code unitId}.
   *
   * @param unitId the remote device unit id.
   * @param request the {@link ReadWriteMultipleRegistersRequest}.
   * @return a {@link CompletionStage} that completes successfully with the {@link
   *     ReadWriteMultipleRegistersResponse}, or completes exceptionally if an error occurs.
   */
  public CompletionStage<ReadWriteMultipleRegistersResponse> readWriteMultipleRegistersAsync(
      int unitId, ReadWriteMultipleRegistersRequest request) {

    return sendAsync(unitId, request).thenApply(ReadWriteMultipleRegistersResponse.class::cast);
  }

  // endregion

  public interface ModbusClientAutoCloseable extends AutoCloseable {

    @Override
    void close() throws ModbusExecutionException;
  }
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/client/ModbusRtuClientTransport.java`

```java
package com.digitalpetri.modbus.client;

import com.digitalpetri.modbus.ModbusRtuFrame;

public interface ModbusRtuClientTransport extends ModbusClientTransport<ModbusRtuFrame> {

  /**
   * Reset the frame parser.
   *
   * <p>This method should be called after a timeout or CRC error to reset the parser state.
   */
  void resetFrameParser();
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/client/ModbusTcpClientTransport.java`

```java
package com.digitalpetri.modbus.client;

import com.digitalpetri.modbus.ModbusTcpFrame;

public interface ModbusTcpClientTransport extends ModbusClientTransport<ModbusTcpFrame> {}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/client/ModbusRtuClient.java`

```java
package com.digitalpetri.modbus.client;

import com.digitalpetri.modbus.Crc16;
import com.digitalpetri.modbus.ModbusRtuFrame;
import com.digitalpetri.modbus.TimeoutScheduler.TimeoutHandle;
import com.digitalpetri.modbus.exceptions.ModbusCrcException;
import com.digitalpetri.modbus.exceptions.ModbusException;
import com.digitalpetri.modbus.exceptions.ModbusExecutionException;
import com.digitalpetri.modbus.exceptions.ModbusResponseException;
import com.digitalpetri.modbus.pdu.ModbusPdu;
import com.digitalpetri.modbus.pdu.ModbusRequestPdu;
import com.digitalpetri.modbus.pdu.ModbusResponsePdu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class ModbusRtuClient extends ModbusClient {

  /** The unit/slave ID used when sending broadcast messages. */
  private static final int BROADCAST_ID = 0;

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final ArrayDeque<ResponsePromise> promises = new ArrayDeque<>();

  // package visibility for testing
  final Map<ResponsePromise, TimeoutHandle> timeouts = new ConcurrentHashMap<>();

  private final ModbusClientConfig config;
  private final ModbusRtuClientTransport transport;

  public ModbusRtuClient(ModbusClientConfig config, ModbusRtuClientTransport transport) {
    super(transport);

    this.config = config;
    this.transport = transport;

    transport.receive(this::onFrameReceived);
  }

  /**
   * Get the {@link ModbusClientConfig} used by this client.
   *
   * @return the {@link ModbusClientConfig} used by this client.
   */
  public ModbusClientConfig getConfig() {
    return config;
  }

  /**
   * Get the {@link ModbusRtuClientTransport} used by this client.
   *
   * @return the {@link ModbusRtuClientTransport} used by this client.
   */
  @Override
  public ModbusRtuClientTransport getTransport() {
    return transport;
  }

  @Override
  public CompletionStage<ModbusResponsePdu> sendAsync(int unitId, ModbusRequestPdu request) {
    ByteBuffer pdu = ByteBuffer.allocate(256);

    try {
      config.requestSerializer().encode(request, pdu);
      pdu.flip();
    } catch (Exception e) {
      return CompletableFuture.failedFuture(e);
    }

    ByteBuffer crc = calculateCrc16(unitId, pdu);

    var promise = new ResponsePromise(unitId, request.getFunctionCode(), new CompletableFuture<>());

    synchronized (promises) {
      promises.push(promise);
    }

    long timeoutMillis = config.requestTimeout().toMillis();
    TimeoutHandle timeout =
        config
            .timeoutScheduler()
            .newTimeout(
                t -> {
                  boolean removed;
                  synchronized (promises) {
                    removed = promises.remove(promise);
                  }

                  timeouts.remove(promise);

                  if (removed) {
                    // The frame parser needs to be reset!
                    // It could be "stuck" in Accumulating or ParseError states if the timeout was
                    // caused by an incomplete or invalid response rather than no response.
                    resetFrameParser();

                    promise.future.completeExceptionally(
                        new TimeoutException(
                            "request timed out after %sms".formatted(timeoutMillis)));
                  }
                },
                timeoutMillis,
                TimeUnit.MILLISECONDS);

    timeouts.put(promise, timeout);

    transport
        .send(new ModbusRtuFrame(unitId, pdu, crc))
        .whenComplete(
            (v, ex) -> {
              if (ex != null) {
                boolean removed;
                synchronized (promises) {
                  removed = promises.remove(promise);
                }
                if (removed) {
                  promise.future.completeExceptionally(ex);
                }
                TimeoutHandle t = timeouts.remove(promise);
                if (t != null) {
                  t.cancel();
                }
              }
            });

    return promise.future;
  }

  /**
   * Send a broadcast request to all connected slaves. No response is returned to broadcast requests
   * sent by the master.
   *
   * <p>Broadcast requests are necessarily write commands.
   *
   * @param request the request to broadcast. Must be a write command.
   * @throws ModbusExecutionException if an error occurs while sending the request.
   */
  public void broadcast(ModbusRequestPdu request) throws ModbusExecutionException {
    try {
      broadcastAsync(request).toCompletableFuture().get();
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      throw new ModbusExecutionException(cause);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ModbusExecutionException(e);
    }
  }

  /**
   * Send a broadcast request to all connected slaves. No response is returned to broadcast requests
   * sent by the master.
   *
   * <p>Broadcast requests are necessarily write commands.
   *
   * @param request the request to broadcast. Must be a write command.
   * @return a {@link CompletionStage} that completes when the request has been sent.
   */
  public CompletionStage<Void> broadcastAsync(ModbusRequestPdu request) {
    ByteBuffer pdu = ByteBuffer.allocate(256);

    try {
      config.requestSerializer().encode(request, pdu);
      pdu.flip();
    } catch (Exception e) {
      return CompletableFuture.failedFuture(e);
    }

    ByteBuffer crc = calculateCrc16(BROADCAST_ID, pdu);

    return transport.send(new ModbusRtuFrame(BROADCAST_ID, pdu, crc));
  }

  private void onFrameReceived(ModbusRtuFrame frame) {
    ResponsePromise promise;
    synchronized (promises) {
      promise = promises.poll();
    }

    if (promise != null) {
      TimeoutHandle t = timeouts.remove(promise);
      if (t != null) {
        t.cancel();
      }

      if (!verifyCrc16(frame)) {
        resetFrameParser();

        promise.future.completeExceptionally(new ModbusCrcException(frame));
        return;
      }

      int slaveId = frame.unitId();

      if (promise.slaveId != slaveId) {
        promise.future.completeExceptionally(
            new ModbusException("slave id mismatch: %s != %s".formatted(promise.slaveId, slaveId)));
        return;
      }

      ByteBuffer buffer = frame.pdu();
      int functionCode = buffer.get(buffer.position()) & 0xFF;

      if (functionCode < 0x80) {
        if (functionCode != promise.functionCode) {
          // Response might be out of sync, e.g. the timeout elapsed in request A,
          // we sent request B, and now we're receiving response A.

          promise.future.completeExceptionally(
              new ModbusException(
                  "function code mismatch: %s != %s"
                      .formatted(promise.functionCode, functionCode)));

          // Clear out any pending promises.
          var pending = new ArrayList<ResponsePromise>();
          synchronized (promises) {
            while (!promises.isEmpty()) {
              pending.add(promises.poll());
            }
          }
          pending.forEach(
              p -> p.future.completeExceptionally(new ModbusException("synchronization error")));
        } else {
          try {
            ModbusPdu modbusPdu = config.responseSerializer().decode(functionCode, buffer);
            promise.future.complete((ModbusResponsePdu) modbusPdu);
          } catch (Exception e) {
            promise.future.completeExceptionally(e);
          }
        }
      } else {
        int exceptionCode = buffer.get();

        promise.future.completeExceptionally(
            new ModbusResponseException(promise.functionCode, exceptionCode));
      }
    } else {
      logger.warn("No pending request for response frame: {}", frame);
    }
  }

  /** Reset the transport's frame parser. */
  protected void resetFrameParser() {
    transport.resetFrameParser();
  }

  /**
   * Calculate the CRC-16 for the given frame (unit ID and PDU).
   *
   * @param unitId the unit ID.
   * @param pdu the PDU.
   * @return a {@link ByteBuffer} containing the calculated CRC-16.
   */
  protected ByteBuffer calculateCrc16(int unitId, ByteBuffer pdu) {
    var crc16 = new Crc16();
    crc16.update(unitId);
    crc16.update(pdu);

    ByteBuffer crc = ByteBuffer.allocate(2);
    // write crc in little-endian order
    crc.put((byte) (crc16.getValue() & 0xFF));
    crc.put((byte) ((crc16.getValue() >> 8) & 0xFF));

    return crc.flip();
  }

  /**
   * Verify the reported CRC-16 matches the calculated CRC-16.
   *
   * @param frame the frame to verify.
   * @return {@code true} if the CRC-16 matches, {@code false} otherwise.
   */
  protected boolean verifyCrc16(ModbusRtuFrame frame) {
    var crc16 = new Crc16();
    crc16.update(frame.unitId());
    crc16.update(frame.pdu());
    int expected = crc16.getValue();

    int offset = frame.crc().position();
    int low = frame.crc().get(offset) & 0xFF;
    int high = frame.crc().get(offset + 1) & 0xFF;
    int reported = (high << 8) | low;

    return expected == reported;
  }

  /**
   * Create a new {@link ModbusRtuClient} using the given {@link ModbusRtuClientTransport} and a
   * {@link ModbusClientConfig} with the default values.
   *
   * @param transport the {@link ModbusRtuClientTransport} to use.
   * @return a new {@link ModbusRtuClient}.
   */
  public static ModbusRtuClient create(ModbusRtuClientTransport transport) {
    return create(transport, cfg -> {});
  }

  /**
   * Create a new {@link ModbusRtuClient} using the given {@link ModbusRtuClientTransport} and a
   * callback for building a {@link ModbusClientConfig}.
   *
   * @param transport the {@link ModbusRtuClientTransport} to use.
   * @param configure a callback used to build a {@link ModbusClientConfig}.
   * @return a new {@link ModbusRtuClient}.
   */
  public static ModbusRtuClient create(
      ModbusRtuClientTransport transport, Consumer<ModbusClientConfig.Builder> configure) {

    var builder = new ModbusClientConfig.Builder();
    configure.accept(builder);
    return new ModbusRtuClient(builder.build(), transport);
  }

  private record ResponsePromise(
      int slaveId, int functionCode, CompletableFuture<ModbusResponsePdu> future) {}
}

```

---

### `modbus/protocol/src/main/java/com/digitalpetri/modbus/client/ModbusTcpClient.java`

```java
package com.digitalpetri.modbus.client;

import com.digitalpetri.modbus.MbapHeader;
import com.digitalpetri.modbus.ModbusTcpFrame;
import com.digitalpetri.modbus.TimeoutScheduler.TimeoutHandle;
import com.digitalpetri.modbus.exceptions.ModbusException;
import com.digitalpetri.modbus.exceptions.ModbusExecutionException;
import com.digitalpetri.modbus.exceptions.ModbusResponseException;
import com.digitalpetri.modbus.exceptions.ModbusTimeoutException;
import com.digitalpetri.modbus.internal.util.Hex;
import com.digitalpetri.modbus.pdu.ModbusPdu;
import com.digitalpetri.modbus.pdu.ModbusRequestPdu;
import com.digitalpetri.modbus.pdu.ModbusResponsePdu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class ModbusTcpClient extends ModbusClient {

  /** Fixed protocol ID identifying the protocol as Modbus in {@link MbapHeader}. */
  private static final int MODBUS_PROTOCOL_ID = 0;

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final Map<Integer, ResponsePromise> promises = new ConcurrentHashMap<>();

  private final AtomicReference<TransactionSequence> transactionSequence = new AtomicReference<>();

  private final ModbusClientConfig config;
  private final ModbusTcpClientTransport transport;

  public ModbusTcpClient(ModbusClientConfig config, ModbusTcpClientTransport transport) {
    super(transport);

    this.config = config;
    this.transport = transport;

    transport.receive(this::onFrameReceived);
  }

  /**
   * Get the {@link ModbusClientConfig} used by this client.
   *
   * @return the {@link ModbusClientConfig} used by this client.
   */
  public ModbusClientConfig getConfig() {
    return config;
  }

  /**
   * Get the {@link ModbusTcpClientTransport} used by this client.
   *
   * @return the {@link ModbusTcpClientTransport} used by this client.
   */
  @Override
  public ModbusTcpClientTransport getTransport() {
    return transport;
  }

  public byte[] sendRaw(int unitId, byte[] pduBytes)
      throws ModbusExecutionException, ModbusResponseException, ModbusTimeoutException {

    try {
      return sendRawAsync(unitId, pduBytes).toCompletableFuture().get();
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof TimeoutException ex) {
        throw new ModbusTimeoutException(ex);
      } else if (cause instanceof ModbusResponseException ex) {
        throw ex;
      } else {
        throw new ModbusExecutionException(cause);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ModbusExecutionException(e);
    }
  }

  public CompletionStage<byte[]> sendRawAsync(int unitId, byte[] pduBytes) {
    CompletionStage<ByteBuffer> cs = sendBufferAsync(unitId, ByteBuffer.wrap(pduBytes));

    return cs.thenApply(
        buffer -> {
          var bytes = new byte[buffer.remaining()];
          buffer.get(bytes);
          return bytes;
        });
  }

  @Override
  public CompletionStage<ModbusResponsePdu> sendAsync(int unitId, ModbusRequestPdu request) {
    ByteBuffer pduBytes = ByteBuffer.allocate(256);

    try {
      config.requestSerializer().encode(request, pduBytes);
    } catch (Exception e) {
      return CompletableFuture.failedFuture(e);
    }

    CompletionStage<ByteBuffer> cs = sendBufferAsync(unitId, pduBytes.flip());

    return cs.thenApply(
        buffer -> {
          try {
            ModbusPdu decoded =
                config.responseSerializer().decode(request.getFunctionCode(), buffer);
            return (ModbusResponsePdu) decoded;
          } catch (Exception e) {
            throw new CompletionException(e);
          }
        });
  }

  private CompletionStage<ByteBuffer> sendBufferAsync(int unitId, ByteBuffer buffer) {
    TransactionSequence sequence =
        transactionSequence.updateAndGet(ts -> ts != null ? ts : createTransactionSequence());
    int transactionId = sequence.next();

    var header = new MbapHeader(transactionId, MODBUS_PROTOCOL_ID, 1 + buffer.remaining(), unitId);

    long timeoutMillis = config.requestTimeout().toMillis();
    TimeoutHandle timeout =
        config
            .timeoutScheduler()
            .newTimeout(
                t -> {
                  ResponsePromise promise = promises.remove(header.transactionId());
                  if (promise != null) {
                    promise.future.completeExceptionally(
                        new TimeoutException(
                            "request timed out after %sms".formatted(timeoutMillis)));
                  }
                },
                timeoutMillis,
                TimeUnit.MILLISECONDS);

    var pending = new ResponsePromise(buffer.get(0) & 0xFF, new CompletableFuture<>(), timeout);

    promises.put(header.transactionId(), pending);

    transport
        .send(new ModbusTcpFrame(header, buffer))
        .whenComplete(
            (v, ex) -> {
              if (ex != null) {
                ResponsePromise promise = promises.remove(header.transactionId());
                if (promise != null) {
                  promise.timeout.cancel();
                  promise.future.completeExceptionally(ex);
                }
              }
            });

    return pending.future;
  }

  private void onFrameReceived(ModbusTcpFrame frame) {
    MbapHeader header = frame.header();
    ResponsePromise promise = promises.remove(header.transactionId());

    if (promise != null) {
      promise.timeout.cancel();

      ByteBuffer buffer = frame.pdu();

      if (buffer.remaining() == 0) {
        promise.future.completeExceptionally(new ModbusException("empty response PDU"));
        return;
      }

      int functionCode = buffer.get(buffer.position()) & 0xFF;

      if (functionCode == promise.functionCode) {
        promise.future.complete(buffer);
      } else if (functionCode == promise.functionCode + 0x80) {
        if (buffer.remaining() >= 2) {
          buffer.get(); // skip FC byte
          int exceptionCode = buffer.get() & 0xFF;

          promise.future.completeExceptionally(
              new ModbusResponseException(promise.functionCode, exceptionCode));
        } else {
          promise.future.completeExceptionally(
              new ModbusException(
                  "malformed exception response PDU: %s".formatted(Hex.format(buffer))));
        }
      } else {
        promise.future.completeExceptionally(
            new ModbusException("unexpected function code: 0x%02X".formatted(functionCode)));
      }
    } else {
      logger.warn("No pending request for response frame: {}", frame);
    }
  }

  /**
   * Create and return the {@link TransactionSequence} that will be used to generate transaction
   * ids.
   *
   * @return the {@link TransactionSequence} that will be used to generate transaction ids.
   */
  protected TransactionSequence createTransactionSequence() {
    return new DefaultTransactionSequence();
  }

  /**
   * Create a new {@link ModbusTcpClient} using the given {@link ModbusTcpClientTransport} and a
   * {@link ModbusClientConfig} with the default values.
   *
   * @param transport the {@link ModbusTcpClientTransport} to use.
   * @return a new {@link ModbusTcpClient}.
   */
  public static ModbusTcpClient create(ModbusTcpClientTransport transport) {
    return create(transport, cfg -> {});
  }

  /**
   * Create a new {@link ModbusTcpClient} using the given {@link ModbusTcpClientTransport} and a
   * callback for building a {@link ModbusClientConfig}.
   *
   * @param transport the {@link ModbusTcpClientTransport} to use.
   * @param configure a callback used to build a {@link ModbusClientConfig}.
   * @return a new {@link ModbusTcpClient}.
   */
  public static ModbusTcpClient create(
      ModbusTcpClientTransport transport, Consumer<ModbusClientConfig.Builder> configure) {

    var config = ModbusClientConfig.create(configure);

    return new ModbusTcpClient(config, transport);
  }

  /**
   * The promise of some future response PDU bytes and the function code of the originating request.
   *
   * @param functionCode the function code of the originating request.
   * @param future a {@link CompletableFuture} that completes successfully with the response PDU
   *     bytes, or completes exceptionally if no response is received.
   * @param timeout a {@link TimeoutHandle} handle to be cancelled when the response is received.
   */
  private record ResponsePromise(
      int functionCode, CompletableFuture<ByteBuffer> future, TimeoutHandle timeout) {}

  public interface TransactionSequence {

    /**
     * Return the next 2-byte transaction identifier. Range is [0, 65535] by default.
     *
     * <p>Implementations must be safe for use by multiple threads.
     *
     * @return the next 2-byte transaction identifier.
     */
    int next();
  }

  public static class DefaultTransactionSequence implements TransactionSequence {

    private final int low;
    private final int high;

    private final AtomicReference<Integer> transactionId = new AtomicReference<>(0);

    public DefaultTransactionSequence() {
      this(0, 65535);
    }

    public DefaultTransactionSequence(int low, int high) {
      this.low = low;
      this.high = high;

      transactionId.set(low);
    }

    @Override
    public int next() {
      while (true) {
        Integer id = transactionId.get();
        Integer nextId = id >= high ? low : id + 1;

        if (transactionId.compareAndSet(id, nextId)) {
          return id;
        }
      }
    }
  }
}

```

---

### `modbus/protocol/src/test/java/com/digitalpetri/modbus/MbapHeaderTest.java`

```java
package com.digitalpetri.modbus;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

class MbapHeaderTest {

  @Test
  void serializer() {
    for (int i = 0; i < 65536; i++) {
      ByteBuffer buffer = ByteBuffer.allocate(7);

      var header = new MbapHeader(i, i, i, i % 256);
      MbapHeader.Serializer.encode(header, buffer);

      buffer.flip();
      MbapHeader decoded = MbapHeader.Serializer.decode(buffer);

      assertEquals(header, decoded);
    }
  }
}

```

---

### `modbus/protocol/src/test/java/com/digitalpetri/modbus/ModbusRtuResponseFrameParserTest.java`

```java
package com.digitalpetri.modbus;

import static com.digitalpetri.modbus.Util.partitions;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.fail;

import com.digitalpetri.modbus.ModbusRtuResponseFrameParser.Accumulated;
import com.digitalpetri.modbus.ModbusRtuResponseFrameParser.Accumulating;
import com.digitalpetri.modbus.ModbusRtuResponseFrameParser.ParserState;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ModbusRtuResponseFrameParserTest {

  private static final byte[] READ_COILS =
      new byte[] {0x01, 0x01, 0x02, 0x01, 0x02, (byte) 0xCA, (byte) 0xFE};

  private static final byte[] READ_HOLDING_REGISTERS =
      new byte[] {0x01, 0x03, 0x04, 0x01, 0x02, 0x03, 0x04, (byte) 0xCA, (byte) 0xFE};

  @Test
  void readCoils() {
    parseValidResponse(READ_COILS);
  }

  @Test
  void readHoldingRegisters() {
    parseValidResponse(READ_HOLDING_REGISTERS);
  }

  @Test
  void readCoils_InvalidLength() {
    byte[] invalidLengthResponse = Arrays.copyOf(READ_COILS, READ_COILS.length);

    invalidLengthResponse[2] = (byte) (invalidLengthResponse[2] * 2);

    parseInvalidLengthResponse(invalidLengthResponse);
  }

  @Test
  void readHoldingRegisters_InvalidLength() {
    byte[] invalidLengthResponse =
        Arrays.copyOf(READ_HOLDING_REGISTERS, READ_HOLDING_REGISTERS.length);

    invalidLengthResponse[2] = (byte) (invalidLengthResponse[2] * 2);

    parseInvalidLengthResponse(invalidLengthResponse);
  }

  private void parseValidResponse(byte[] validResponseData) {
    var parser = new ModbusRtuResponseFrameParser();

    for (int i = 1; i <= validResponseData.length; i++) {
      parser.reset();

      partitions(validResponseData, i)
          .forEach(
              data -> {
                ParserState s = parser.parse(data);
                System.out.println(s);
              });
      System.out.println("--");

      ParserState state = parser.getState();
      if (state instanceof Accumulated a) {
        int expectedUnitId = validResponseData[0] & 0xFF;
        ByteBuffer expectedPdu =
            ByteBuffer.wrap(validResponseData, 1, validResponseData.length - 3);
        ByteBuffer expectedCrc =
            ByteBuffer.wrap(validResponseData, validResponseData.length - 2, 2);
        assertEquals(expectedUnitId, a.frame().unitId());
        assertEquals(expectedPdu, a.frame().pdu());
        assertEquals(expectedCrc, a.frame().crc());
      } else {
        fail("unexpected state: " + state);
      }
    }
  }

  private void parseInvalidLengthResponse(byte[] invalidLengthResponse) {
    var parser = new ModbusRtuResponseFrameParser();

    for (int i = 1; i <= invalidLengthResponse.length; i++) {
      parser.reset();

      Stream<byte[]> chunks = partitions(invalidLengthResponse, i);
      chunks.forEach(
          data -> {
            ParserState s = parser.parse(data);
            System.out.println(s);
          });
      System.out.println("--");

      ParserState state = parser.getState();

      assertInstanceOf(Accumulating.class, state);
    }
  }
}

```

---

### `modbus/protocol/src/test/java/com/digitalpetri/modbus/server/ReadOnlyModbusServicesTest.java`

```java
package com.digitalpetri.modbus.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.digitalpetri.modbus.pdu.ReadCoilsRequest;
import com.digitalpetri.modbus.pdu.ReadCoilsResponse;
import com.digitalpetri.modbus.pdu.ReadDiscreteInputsRequest;
import com.digitalpetri.modbus.pdu.ReadDiscreteInputsResponse;
import com.digitalpetri.modbus.pdu.ReadHoldingRegistersRequest;
import com.digitalpetri.modbus.pdu.ReadHoldingRegistersResponse;
import com.digitalpetri.modbus.pdu.ReadInputRegistersRequest;
import com.digitalpetri.modbus.pdu.ReadInputRegistersResponse;
import com.digitalpetri.modbus.server.ModbusRequestContext.ModbusTcpRequestContext;
import java.net.SocketAddress;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.Test;

public class ReadOnlyModbusServicesTest {

  private final Random random = new Random();

  private final ProcessImage processImage = new ProcessImage();

  private final ReadOnlyModbusServices services =
      new ReadOnlyModbusServices() {
        @Override
        protected Optional<ProcessImage> getProcessImage(int unitId) {
          return Optional.of(processImage);
        }
      };

  @Test
  void readCoils() throws Exception {
    var randomBooleans = new boolean[65536];
    for (int i = 0; i < 65536; i++) {
      randomBooleans[i] = random.nextBoolean();
    }

    processImage.with(
        tx ->
            tx.writeCoils(
                coilMap -> {
                  for (int i = 0; i < 65536; i++) {
                    coilMap.put(i, randomBooleans[i]);
                  }
                }));

    int address = 0;
    int remaining = 65536;
    int quantity = Math.min(remaining - address, random.nextInt(2000) + 1);

    while (remaining > 0) {
      ReadCoilsResponse response =
          services.readCoils(
              new TestModbusRequestContext(), 0, new ReadCoilsRequest(address, quantity));

      byte[] inputs = response.coils();

      for (int i = 0; i < quantity; i++) {
        byte b = inputs[i / 8];
        int bit = i % 8;
        int value = (b >> bit) & 0x01;
        assertEquals(randomBooleans[address + i] ? 1 : 0, value);
      }

      address += quantity;
      remaining -= quantity;
      quantity = Math.min(remaining, random.nextInt(2000) + 1);
    }
  }

  @Test
  void readDiscreteInputs() throws Exception {
    var randomBooleans = new boolean[65536];
    for (int i = 0; i < 65536; i++) {
      randomBooleans[i] = random.nextBoolean();
    }

    processImage.with(
        tx ->
            tx.writeDiscreteInputs(
                discreteInputMap -> {
                  for (int i = 0; i < 65536; i++) {
                    discreteInputMap.put(i, randomBooleans[i]);
                  }
                }));

    int address = 0;
    int remaining = 65536;
    int quantity = Math.min(remaining - address, random.nextInt(2000) + 1);

    while (remaining > 0) {
      ReadDiscreteInputsResponse response =
          services.readDiscreteInputs(
              new TestModbusRequestContext(), 0, new ReadDiscreteInputsRequest(address, quantity));

      byte[] inputs = response.inputs();

      for (int i = 0; i < quantity; i++) {
        byte b = inputs[i / 8];
        int bit = i % 8;
        int value = (b >> bit) & 0x01;
        assertEquals(randomBooleans[address + i] ? 1 : 0, value);
      }

      address += quantity;
      remaining -= quantity;
      quantity = Math.min(remaining, random.nextInt(2000) + 1);
    }
  }

  @Test
  void readHoldingRegisters() throws Exception {
    var random = new Random();
    var randomBytes = new byte[65536 * 2];
    random.nextBytes(randomBytes);

    // Fill the Holding Registers with random data
    processImage.with(
        tx ->
            tx.writeHoldingRegisters(
                holdingRegisterMap -> {
                  for (int i = 0; i < 65536; i++) {
                    var bs = new byte[2];
                    bs[0] = randomBytes[i * 2];
                    bs[1] = randomBytes[i * 2 + 1];
                    holdingRegisterMap.put(i, bs);
                  }
                }));

    // Read random lengths until all registers are read and verified
    int address = 0;
    int remaining = 65536;
    int quantity = Math.min(remaining - address, random.nextInt(125) + 1);

    while (remaining > 0) {
      ReadHoldingRegistersResponse response =
          services.readHoldingRegisters(
              new TestModbusRequestContext(),
              0,
              new ReadHoldingRegistersRequest(address, quantity));

      byte[] registers = response.registers();

      int baseOffsetIntoRandom = address * 2;
      for (int i = 0; i < quantity; i++) {
        byte b0 = registers[i * 2];
        byte b1 = registers[i * 2 + 1];
        byte r0 = randomBytes[baseOffsetIntoRandom + i * 2];
        byte r1 = randomBytes[baseOffsetIntoRandom + i * 2 + 1];
        assertEquals(r0, b0);
        assertEquals(r1, b1);
      }

      address += quantity;
      remaining -= quantity;
      quantity = Math.min(remaining, random.nextInt(125) + 1);
    }
  }

  @Test
  void readInputRegisters() throws Exception {
    var random = new Random();
    var randomBytes = new byte[65536 * 2];
    random.nextBytes(randomBytes);

    // Fill the Holding Registers with random data
    processImage.with(
        tx ->
            tx.writeInputRegisters(
                inputRegisterMap -> {
                  for (int i = 0; i < 65536; i++) {
                    var bs = new byte[2];
                    bs[0] = randomBytes[i * 2];
                    bs[1] = randomBytes[i * 2 + 1];
                    inputRegisterMap.put(i, bs);
                  }
                }));

    // Read random lengths until all registers are read and verified
    int address = 0;
    int remaining = 65536;
    int quantity = Math.min(remaining - address, random.nextInt(125) + 1);

    while (remaining > 0) {
      ReadInputRegistersResponse response =
          services.readInputRegisters(
              new TestModbusRequestContext(), 0, new ReadInputRegistersRequest(address, quantity));

      byte[] registers = response.registers();

      int baseOffsetIntoRandom = address * 2;
      for (int i = 0; i < quantity; i++) {
        byte b0 = registers[i * 2];
        byte b1 = registers[i * 2 + 1];
        byte r0 = randomBytes[baseOffsetIntoRandom + i * 2];
        byte r1 = randomBytes[baseOffsetIntoRandom + i * 2 + 1];
        assertEquals(r0, b0);
        assertEquals(r1, b1);
      }

      address += quantity;
      remaining -= quantity;
      quantity = Math.min(remaining, random.nextInt(125) + 1);
    }
  }

  static class TestModbusRequestContext implements ModbusTcpRequestContext {

    @Override
    public SocketAddress localAddress() {
      return null;
    }

    @Override
    public SocketAddress remoteAddress() {
      return null;
    }
  }
}

```

---

### `modbus/protocol/src/test/java/com/digitalpetri/modbus/server/ProcessImageTest.java`

```java
package com.digitalpetri.modbus.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class ProcessImageTest {

  @Test
  void coils() {
    var processImage = new ProcessImage();

    processImage.with(
        tx -> {
          tx.readCoils(
              coils -> {
                for (int i = 0; i < 65536; i++) {
                  assertFalse(coils.containsKey(i));
                }
                return null;
              });
          tx.writeCoils(
              coils -> {
                for (int i = 0; i < 65536; i++) {
                  coils.put(i, i % 2 == 0);
                }
              });
          tx.readCoils(
              coils -> {
                for (int i = 0; i < 65536; i++) {
                  assertEquals(i % 2 == 0, coils.get(i));
                }
                return null;
              });
          tx.writeCoils(
              coils -> {
                for (int i = 0; i < 65536; i++) {
                  coils.remove(i);
                }
              });
          tx.readCoils(
              coils -> {
                for (int i = 0; i < 65536; i++) {
                  assertFalse(coils.containsKey(i));
                }
                return null;
              });
        });
  }

  @Test
  void discreteInputs() {
    var processImage = new ProcessImage();

    processImage.with(
        tx -> {
          tx.readDiscreteInputs(
              inputs -> {
                for (int i = 0; i < 65536; i++) {
                  assertFalse(inputs.containsKey(i));
                }
                return null;
              });
          tx.writeDiscreteInputs(
              inputs -> {
                for (int i = 0; i < 65536; i++) {
                  inputs.put(i, i % 2 == 0);
                }
              });
          tx.readDiscreteInputs(
              inputs -> {
                for (int i = 0; i < 65536; i++) {
                  assertEquals(i % 2 == 0, inputs.get(i));
                }
                return null;
              });
          tx.writeDiscreteInputs(
              inputs -> {
                for (int i = 0; i < 65536; i++) {
                  inputs.remove(i);
                }
              });
          tx.readDiscreteInputs(
              inputs -> {
                for (int i = 0; i < 65536; i++) {
                  assertFalse(inputs.containsKey(i));
                }
                return null;
              });
        });
  }

  @Test
  void holdingRegisters() {
    var processImage = new ProcessImage();

    processImage.with(
        tx -> {
          tx.readHoldingRegisters(
              registers -> {
                for (int i = 0; i < 65536; i++) {
                  assertFalse(registers.containsKey(i));
                }
                return null;
              });
          tx.writeHoldingRegisters(
              registers -> {
                for (int i = 0; i < 65536; i++) {
                  var bs = new byte[2];
                  bs[0] = (byte) ((i >> 8) & 0xFF);
                  bs[1] = (byte) (i & 0xFF);
                  registers.put(i, bs);
                }
              });
          tx.readHoldingRegisters(
              registers -> {
                for (int i = 0; i < 65536; i++) {
                  byte[] bs = registers.get(i);
                  int value = (bs[0] & 0xFF) << 8 | bs[1] & 0xFF;
                  assertEquals(i, value);
                }
                return null;
              });
          tx.writeHoldingRegisters(
              registers -> {
                for (int i = 0; i < 65536; i++) {
                  registers.remove(i);
                }
              });
          tx.readHoldingRegisters(
              registers -> {
                for (int i = 0; i < 65536; i++) {
                  assertFalse(registers.containsKey(i));
                }
                return null;
              });
        });
  }

  @Test
  void inputRegisters() {
    var processImage = new ProcessImage();

    processImage.with(
        tx -> {
          tx.readInputRegisters(
              registers -> {
                for (int i = 0; i < 65536; i++) {
                  assertFalse(registers.containsKey(i));
                }
                return null;
              });
          tx.writeInputRegisters(
              registers -> {
                for (int i = 0; i < 65536; i++) {
                  var bs = new byte[2];
                  bs[0] = (byte) ((i >> 8) & 0xFF);
                  bs[1] = (byte) (i & 0xFF);
                  registers.put(i, bs);
                }
              });
          tx.readInputRegisters(
              registers -> {
                for (int i = 0; i < 65536; i++) {
                  byte[] bs = registers.get(i);
                  int value = (bs[0] & 0xFF) << 8 | bs[1] & 0xFF;
                  assertEquals(i, value);
                }
                return null;
              });
          tx.writeInputRegisters(
              registers -> {
                for (int i = 0; i < 65536; i++) {
                  registers.remove(i);
                }
              });
          tx.readInputRegisters(
              registers -> {
                for (int i = 0; i < 65536; i++) {
                  assertFalse(registers.containsKey(i));
                }
                return null;
              });
        });
  }
}

```

---

### `modbus/protocol/src/test/java/com/digitalpetri/modbus/server/ReadWriteModbusServicesTest.java`

```java
package com.digitalpetri.modbus.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.digitalpetri.modbus.pdu.MaskWriteRegisterRequest;
import com.digitalpetri.modbus.pdu.ReadWriteMultipleRegistersRequest;
import com.digitalpetri.modbus.pdu.ReadWriteMultipleRegistersResponse;
import com.digitalpetri.modbus.pdu.WriteMultipleCoilsRequest;
import com.digitalpetri.modbus.pdu.WriteMultipleRegistersRequest;
import com.digitalpetri.modbus.pdu.WriteSingleCoilRequest;
import com.digitalpetri.modbus.pdu.WriteSingleRegisterRequest;
import com.digitalpetri.modbus.server.ReadOnlyModbusServicesTest.TestModbusRequestContext;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.Test;

public class ReadWriteModbusServicesTest {

  private final Random random = new Random();

  private final ProcessImage processImage = new ProcessImage();

  private final ReadWriteModbusServices services =
      new ReadWriteModbusServices() {
        @Override
        protected Optional<ProcessImage> getProcessImage(int unitId) {
          return Optional.of(processImage);
        }
      };

  @Test
  void writeSingleCoil() throws Exception {
    boolean[] values = new boolean[65536];
    for (int i = 0; i < 65536; i++) {
      values[i] = random.nextBoolean();
      services.writeSingleCoil(
          new TestModbusRequestContext(), 0, new WriteSingleCoilRequest(i, values[i]));
    }
    processImage.with(
        tx ->
            tx.readCoils(
                coilMap -> {
                  for (int i = 0; i < 65536; i++) {
                    assertEquals(values[i], coilMap.getOrDefault(i, false));
                  }
                  return null;
                }));
  }

  @Test
  void writeMultipleCoils() throws Exception {
    boolean[] values = new boolean[65536];
    for (int i = 0; i < 65536; i++) {
      values[i] = random.nextBoolean();
    }

    int address = 0;
    int remaining = 65536;
    int quantity = Math.min(remaining - address, random.nextInt(0x7B0) + 1);

    while (remaining > 0) {
      var coils = new byte[(quantity + 7) / 8];
      for (int i = 0; i < quantity; i++) {
        int ci = i / 8;
        int bi = i % 8;
        if (values[address + i]) {
          coils[ci] |= (byte) (1 << bi);
        }
      }

      services.writeMultipleCoils(
          new TestModbusRequestContext(),
          0,
          new WriteMultipleCoilsRequest(address, quantity, coils));

      address += quantity;
      remaining -= quantity;
      quantity = Math.min(remaining, random.nextInt(0x7B0) + 1);
    }

    processImage.with(
        tx ->
            tx.readCoils(
                coilMap -> {
                  for (int i = 0; i < 65536; i++) {
                    assertEquals(values[i], coilMap.getOrDefault(i, false));
                  }
                  return null;
                }));
  }

  @Test
  void writeSingleRegister() throws Exception {
    var randomBytes = new byte[65536 * 2];
    random.nextBytes(randomBytes);

    for (int i = 0; i < 65536; i++) {
      int value = (randomBytes[i * 2] & 0xFF) << 8 | (randomBytes[i * 2 + 1] & 0xFF);
      services.writeSingleRegister(
          new TestModbusRequestContext(), 0, new WriteSingleRegisterRequest(i, value));
    }

    processImage.with(
        tx ->
            tx.readHoldingRegisters(
                holdingRegisterMap -> {
                  for (int i = 0; i < 65536; i++) {
                    byte[] registerBytes = holdingRegisterMap.getOrDefault(i, new byte[2]);
                    int registerValue = (registerBytes[0] & 0xFF) << 8 | (registerBytes[1] & 0xFF);
                    int expectedValue =
                        (randomBytes[i * 2] & 0xFF) << 8 | (randomBytes[i * 2 + 1] & 0xFF);
                    assertEquals(expectedValue, registerValue);
                  }
                  return null;
                }));
  }

  @Test
  void writeMultipleRegisters() throws Exception {
    var randomBytes = new byte[65536 * 2];
    random.nextBytes(randomBytes);

    int address = 0;
    int remaining = 65536;
    int quantity = Math.min(remaining - address, random.nextInt(123) + 1);

    while (remaining > 0) {
      var registers = new byte[quantity * 2];

      int baseOffsetIntoRandom = address * 2;
      for (int i = 0; i < quantity; i++) {
        registers[i * 2] = randomBytes[baseOffsetIntoRandom + i * 2];
        registers[i * 2 + 1] = randomBytes[baseOffsetIntoRandom + i * 2 + 1];
      }

      services.writeMultipleRegisters(
          new TestModbusRequestContext(),
          0,
          new WriteMultipleRegistersRequest(address, quantity, registers));

      address += quantity;
      remaining -= quantity;
      quantity = Math.min(remaining, random.nextInt(123) + 1);
    }

    processImage.with(
        tx ->
            tx.readHoldingRegisters(
                holdingRegisterMap -> {
                  for (int i = 0; i < 65536; i++) {
                    byte[] registerBytes = holdingRegisterMap.getOrDefault(i, new byte[2]);
                    byte b0 = registerBytes[0];
                    byte b1 = registerBytes[1];
                    byte r0 = randomBytes[i * 2];
                    byte r1 = randomBytes[i * 2 + 1];
                    assertEquals(r0, b0);
                    assertEquals(r1, b1);
                  }
                  return null;
                }));
  }

  @Test
  void maskWriteRegister() throws Exception {
    var randomBytes = new byte[65536 * 2];
    random.nextBytes(randomBytes);

    processImage.with(
        tx ->
            tx.writeHoldingRegisters(
                registerMap -> {
                  for (int i = 0; i < 65536; i++) {
                    var bs = new byte[2];
                    bs[0] = randomBytes[i * 2];
                    bs[1] = randomBytes[i * 2 + 1];
                    registerMap.put(i, bs);
                  }
                }));

    short[] expectedValues = new short[65536];

    for (int i = 0; i < 65536; i++) {
      int andMask = random.nextInt(0xFFFF + 1);
      int orMask = random.nextInt(0xFFFF + 1);
      services.maskWriteRegister(
          new TestModbusRequestContext(), 0, new MaskWriteRegisterRequest(i, andMask, orMask));
      int current = (randomBytes[i * 2] & 0xFF) << 8 | randomBytes[i * 2 + 1] & 0xFF;
      int expected = (current & andMask) | (orMask & ~andMask);
      expectedValues[i] = (short) expected;
    }

    processImage.with(
        tx ->
            tx.readHoldingRegisters(
                registerMap -> {
                  for (int i = 0; i < 65536; i++) {
                    byte[] registerBytes = registerMap.getOrDefault(i, new byte[2]);
                    short registerValue =
                        (short) ((registerBytes[0] & 0xFF) << 8 | (registerBytes[1] & 0xFF));
                    assertEquals(expectedValues[i], registerValue);
                  }
                  return null;
                }));
  }

  @Test
  void readWriteMultipleRegisters() throws Exception {
    var randomBytes = new byte[65536 * 2];
    random.nextBytes(randomBytes);

    int address = 0;
    int remaining = 65536;
    int quantity = Math.min(remaining - address, random.nextInt(0x79) + 1);

    while (remaining > 0) {
      var values = new byte[quantity * 2];

      ReadWriteMultipleRegistersResponse response =
          services.readWriteMultipleRegisters(
              new TestModbusRequestContext(),
              0,
              new ReadWriteMultipleRegistersRequest(address, quantity, address, quantity, values));

      byte[] registers = response.registers();
      for (byte register : registers) {
        assertEquals(0, register);
      }

      address += quantity;
      remaining -= quantity;
      quantity = Math.min(remaining, random.nextInt(123) + 1);
    }

    // the above wrote zero to all the randomly initialized registers
    processImage.with(
        tx ->
            tx.readHoldingRegisters(
                holdingRegisterMap -> {
                  for (int i = 0; i < 65536; i++) {
                    byte[] registerBytes = holdingRegisterMap.getOrDefault(i, new byte[2]);
                    byte b0 = registerBytes[0];
                    byte b1 = registerBytes[1];

                    assertEquals(0, b0);
                    assertEquals(0, b1);
                  }
                  return null;
                }));
  }
}

```

---

### `modbus/protocol/src/test/java/com/digitalpetri/modbus/server/authz/AuthzModbusServicesTest.java`

```java
package com.digitalpetri.modbus.server.authz;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.digitalpetri.modbus.exceptions.ModbusException;
import com.digitalpetri.modbus.pdu.MaskWriteRegisterRequest;
import com.digitalpetri.modbus.pdu.ReadCoilsRequest;
import com.digitalpetri.modbus.pdu.ReadDiscreteInputsRequest;
import com.digitalpetri.modbus.pdu.ReadHoldingRegistersRequest;
import com.digitalpetri.modbus.pdu.ReadInputRegistersRequest;
import com.digitalpetri.modbus.pdu.ReadWriteMultipleRegistersRequest;
import com.digitalpetri.modbus.pdu.WriteMultipleCoilsRequest;
import com.digitalpetri.modbus.pdu.WriteMultipleRegistersRequest;
import com.digitalpetri.modbus.pdu.WriteSingleCoilRequest;
import com.digitalpetri.modbus.pdu.WriteSingleRegisterRequest;
import com.digitalpetri.modbus.server.ModbusRequestContext.ModbusTcpTlsRequestContext;
import com.digitalpetri.modbus.server.ProcessImage;
import com.digitalpetri.modbus.server.ReadWriteModbusServices;
import java.net.SocketAddress;
import java.security.cert.X509Certificate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AuthzModbusServicesTest {

  private final TestModbusContext readOnlyContext = new TestModbusContext("ReadOnly");
  private final TestModbusContext writeOnlyContext = new TestModbusContext("WriteOnly");
  private final TestModbusContext readWriteContext = new TestModbusContext("ReadWrite");

  private final AuthzHandler readAuthzHandler =
      new ReadWriteAuthzHandler() {
        @Override
        protected AuthzResult authorizeRead(int unitId, AuthzContext authzContext) {
          String clientRole = authzContext.clientRole().orElseThrow();
          return clientRole.equals("ReadOnly") || clientRole.equals("ReadWrite")
              ? AuthzResult.AUTHORIZED
              : AuthzResult.NOT_AUTHORIZED;
        }

        @Override
        protected AuthzResult authorizeWrite(int unitId, AuthzContext authzContext) {
          String clientRole = authzContext.clientRole().orElseThrow();
          return clientRole.equals("WriteOnly") || clientRole.equals("ReadWrite")
              ? AuthzResult.AUTHORIZED
              : AuthzResult.NOT_AUTHORIZED;
        }
      };

  private final AuthzModbusServices services =
      new AuthzModbusServices(
          readAuthzHandler,
          new ReadWriteModbusServices() {
            private final ProcessImage processImage = new ProcessImage();

            @Override
            protected Optional<ProcessImage> getProcessImage(int unitId) {
              return Optional.of(processImage);
            }
          });

  @Test
  void testReadCoils() {
    assertDoesNotThrow(
        () -> {
          services.readCoils(readOnlyContext, 1, new ReadCoilsRequest(0, 1));
        });
    assertThrows(
        ModbusException.class,
        () -> {
          services.readCoils(writeOnlyContext, 1, new ReadCoilsRequest(0, 1));
        });
    assertDoesNotThrow(
        () -> {
          services.readCoils(readWriteContext, 1, new ReadCoilsRequest(0, 1));
        });
  }

  @Test
  void testReadDiscreteInputs() {
    assertDoesNotThrow(
        () -> {
          services.readDiscreteInputs(readOnlyContext, 1, new ReadDiscreteInputsRequest(0, 1));
        });
    assertThrows(
        ModbusException.class,
        () -> {
          services.readDiscreteInputs(writeOnlyContext, 1, new ReadDiscreteInputsRequest(0, 1));
        });
    assertDoesNotThrow(
        () -> {
          services.readDiscreteInputs(readWriteContext, 1, new ReadDiscreteInputsRequest(0, 1));
        });
  }

  @Test
  void testReadHoldingRegisters() {
    assertDoesNotThrow(
        () -> {
          services.readHoldingRegisters(readOnlyContext, 1, new ReadHoldingRegistersRequest(0, 1));
        });
    assertThrows(
        ModbusException.class,
        () -> {
          services.readHoldingRegisters(writeOnlyContext, 1, new ReadHoldingRegistersRequest(0, 1));
        });
    assertDoesNotThrow(
        () -> {
          services.readHoldingRegisters(readWriteContext, 1, new ReadHoldingRegistersRequest(0, 1));
        });
  }

  @Test
  void testReadInputRegisters() {
    assertDoesNotThrow(
        () -> {
          services.readInputRegisters(readOnlyContext, 1, new ReadInputRegistersRequest(0, 1));
        });
    assertThrows(
        ModbusException.class,
        () -> {
          services.readInputRegisters(writeOnlyContext, 1, new ReadInputRegistersRequest(0, 1));
        });
    assertDoesNotThrow(
        () -> {
          services.readInputRegisters(readWriteContext, 1, new ReadInputRegistersRequest(0, 1));
        });
  }

  @Test
  void testWriteSingleCoil() {
    assertThrows(
        ModbusException.class,
        () -> {
          services.writeSingleCoil(readOnlyContext, 1, new WriteSingleCoilRequest(0, true));
        });
    assertDoesNotThrow(
        () -> {
          services.writeSingleCoil(writeOnlyContext, 1, new WriteSingleCoilRequest(0, true));
        });
    assertDoesNotThrow(
        () -> {
          services.writeSingleCoil(readWriteContext, 1, new WriteSingleCoilRequest(0, true));
        });
  }

  @Test
  void testWriteSingleRegister() {
    assertThrows(
        ModbusException.class,
        () -> {
          services.writeSingleRegister(readOnlyContext, 1, new WriteSingleRegisterRequest(0, 1234));
        });
    assertDoesNotThrow(
        () -> {
          services.writeSingleRegister(
              writeOnlyContext, 1, new WriteSingleRegisterRequest(0, 1234));
        });
    assertDoesNotThrow(
        () -> {
          services.writeSingleRegister(
              readWriteContext, 1, new WriteSingleRegisterRequest(0, 1234));
        });
  }

  @Test
  void testWriteMultipleCoils() {
    assertThrows(
        ModbusException.class,
        () -> {
          services.writeMultipleCoils(
              readOnlyContext, 1, new WriteMultipleCoilsRequest(0, 1, new byte[] {0}));
        });
    assertDoesNotThrow(
        () -> {
          services.writeMultipleCoils(
              writeOnlyContext, 1, new WriteMultipleCoilsRequest(0, 1, new byte[] {0}));
        });
    assertDoesNotThrow(
        () -> {
          services.writeMultipleCoils(
              readWriteContext, 1, new WriteMultipleCoilsRequest(0, 1, new byte[] {0}));
        });
  }

  @Test
  void testWriteMultipleRegisters() {
    assertThrows(
        ModbusException.class,
        () -> {
          services.writeMultipleRegisters(
              readOnlyContext, 1, new WriteMultipleRegistersRequest(0, 1, new byte[] {0, 0}));
        });
    assertDoesNotThrow(
        () -> {
          services.writeMultipleRegisters(
              writeOnlyContext, 1, new WriteMultipleRegistersRequest(0, 1, new byte[] {0, 0}));
        });
    assertDoesNotThrow(
        () -> {
          services.writeMultipleRegisters(
              readWriteContext, 1, new WriteMultipleRegistersRequest(0, 1, new byte[] {0, 0}));
        });
  }

  @Test
  void testMaskWriteRegister() {
    assertThrows(
        ModbusException.class,
        () -> {
          services.maskWriteRegister(
              readOnlyContext, 1, new MaskWriteRegisterRequest(0, 0xFFFF, 0x0000));
        });
    assertDoesNotThrow(
        () -> {
          services.maskWriteRegister(
              writeOnlyContext, 1, new MaskWriteRegisterRequest(0, 0xFFFF, 0x0000));
        });
    assertDoesNotThrow(
        () -> {
          services.maskWriteRegister(
              readWriteContext, 1, new MaskWriteRegisterRequest(0, 0xFFFF, 0x0000));
        });
  }

  @Test
  void testReadWriteMultipleRegisters() {
    assertThrows(
        ModbusException.class,
        () -> {
          services.readWriteMultipleRegisters(
              readOnlyContext,
              1,
              new ReadWriteMultipleRegistersRequest(0, 1, 0, 1, new byte[] {0, 0}));
        });
    assertThrows(
        ModbusException.class,
        () -> {
          services.readWriteMultipleRegisters(
              writeOnlyContext,
              1,
              new ReadWriteMultipleRegistersRequest(0, 1, 0, 1, new byte[] {0, 0}));
        });
    assertDoesNotThrow(
        () -> {
          services.readWriteMultipleRegisters(
              readWriteContext,
              1,
              new ReadWriteMultipleRegistersRequest(0, 1, 0, 1, new byte[] {0, 0}));
        });
  }

  private static class TestModbusContext implements AuthzContext, ModbusTcpTlsRequestContext {

    private final String clientRole;

    private TestModbusContext(String clientRole) {
      this.clientRole = clientRole;
    }

    @Override
    public Optional<String> clientRole() {
      return Optional.of(clientRole);
    }

    @Override
    public X509Certificate[] clientCertificateChain() {
      return new X509Certificate[0];
    }

    @Override
    public SocketAddress localAddress() {
      return null;
    }

    @Override
    public SocketAddress remoteAddress() {
      return null;
    }
  }
}

```

---

### `modbus/protocol/src/test/java/com/digitalpetri/modbus/server/authz/ReadWriteAuthzHandlerTest.java`

```java
package com.digitalpetri.modbus.server.authz;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.digitalpetri.modbus.pdu.MaskWriteRegisterRequest;
import com.digitalpetri.modbus.pdu.ReadCoilsRequest;
import com.digitalpetri.modbus.pdu.ReadDiscreteInputsRequest;
import com.digitalpetri.modbus.pdu.ReadHoldingRegistersRequest;
import com.digitalpetri.modbus.pdu.ReadInputRegistersRequest;
import com.digitalpetri.modbus.pdu.ReadWriteMultipleRegistersRequest;
import com.digitalpetri.modbus.pdu.WriteMultipleCoilsRequest;
import com.digitalpetri.modbus.pdu.WriteMultipleRegistersRequest;
import com.digitalpetri.modbus.pdu.WriteSingleCoilRequest;
import com.digitalpetri.modbus.pdu.WriteSingleRegisterRequest;
import com.digitalpetri.modbus.server.authz.AuthzHandler.AuthzResult;
import java.security.cert.X509Certificate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReadWriteAuthzHandlerTest {

  private ReadWriteAuthzHandler authzHandler;
  private AuthzContext readOnlyAuthzContext;
  private AuthzContext writeOnlyAuthzContext;
  private AuthzContext readWriteAuthzContext;

  @BeforeEach
  void setUp() {
    authzHandler =
        new ReadWriteAuthzHandler() {
          @Override
          protected AuthzResult authorizeRead(int unitId, AuthzContext authzContext) {
            String role = authzContext.clientRole().orElseThrow();
            return role.equals("ReadOnly") || role.equals("ReadWrite")
                ? AuthzResult.AUTHORIZED
                : AuthzResult.NOT_AUTHORIZED;
          }

          @Override
          protected AuthzResult authorizeWrite(int unitId, AuthzContext authzContext) {
            String role = authzContext.clientRole().orElseThrow();
            return role.equals("WriteOnly") || role.equals("ReadWrite")
                ? AuthzResult.AUTHORIZED
                : AuthzResult.NOT_AUTHORIZED;
          }
        };

    readOnlyAuthzContext = new TestAuthzContext("ReadOnly");
    writeOnlyAuthzContext = new TestAuthzContext("WriteOnly");
    readWriteAuthzContext = new TestAuthzContext("ReadWrite");
  }

  @Test
  void testAuthorizeReadCoils() {
    AuthzResult result =
        authzHandler.authorizeReadCoils(readOnlyAuthzContext, 1, new ReadCoilsRequest(0, 1));
    assertEquals(AuthzResult.AUTHORIZED, result);

    result = authzHandler.authorizeReadCoils(writeOnlyAuthzContext, 1, new ReadCoilsRequest(0, 1));
    assertEquals(AuthzResult.NOT_AUTHORIZED, result);

    result = authzHandler.authorizeReadCoils(readWriteAuthzContext, 1, new ReadCoilsRequest(0, 1));
    assertEquals(AuthzResult.AUTHORIZED, result);
  }

  @Test
  void testAuthorizeReadDiscreteInputs() {
    AuthzResult result =
        authzHandler.authorizeReadDiscreteInputs(
            readOnlyAuthzContext, 1, new ReadDiscreteInputsRequest(0, 1));
    assertEquals(AuthzResult.AUTHORIZED, result);

    result =
        authzHandler.authorizeReadDiscreteInputs(
            writeOnlyAuthzContext, 1, new ReadDiscreteInputsRequest(0, 1));
    assertEquals(AuthzResult.NOT_AUTHORIZED, result);

    result =
        authzHandler.authorizeReadDiscreteInputs(
            readWriteAuthzContext, 1, new ReadDiscreteInputsRequest(0, 1));
    assertEquals(AuthzResult.AUTHORIZED, result);
  }

  @Test
  void testAuthorizeReadHoldingRegisters() {
    AuthzResult result =
        authzHandler.authorizeReadHoldingRegisters(
            readOnlyAuthzContext, 1, new ReadHoldingRegistersRequest(0, 1));
    assertEquals(AuthzResult.AUTHORIZED, result);

    result =
        authzHandler.authorizeReadHoldingRegisters(
            writeOnlyAuthzContext, 1, new ReadHoldingRegistersRequest(0, 1));
    assertEquals(AuthzResult.NOT_AUTHORIZED, result);

    result =
        authzHandler.authorizeReadHoldingRegisters(
            readWriteAuthzContext, 1, new ReadHoldingRegistersRequest(0, 1));
    assertEquals(AuthzResult.AUTHORIZED, result);
  }

  @Test
  void testAuthorizeReadInputRegisters() {
    AuthzResult result =
        authzHandler.authorizeReadInputRegisters(
            readOnlyAuthzContext, 1, new ReadInputRegistersRequest(0, 1));
    assertEquals(AuthzResult.AUTHORIZED, result);

    result =
        authzHandler.authorizeReadInputRegisters(
            writeOnlyAuthzContext, 1, new ReadInputRegistersRequest(0, 1));
    assertEquals(AuthzResult.NOT_AUTHORIZED, result);

    result =
        authzHandler.authorizeReadInputRegisters(
            readWriteAuthzContext, 1, new ReadInputRegistersRequest(0, 1));
    assertEquals(AuthzResult.AUTHORIZED, result);
  }

  @Test
  void testAuthorizeWriteSingleCoil() {
    AuthzResult result =
        authzHandler.authorizeWriteSingleCoil(
            readOnlyAuthzContext, 1, new WriteSingleCoilRequest(0, true));
    assertEquals(AuthzResult.NOT_AUTHORIZED, result);

    result =
        authzHandler.authorizeWriteSingleCoil(
            writeOnlyAuthzContext, 1, new WriteSingleCoilRequest(0, true));
    assertEquals(AuthzResult.AUTHORIZED, result);

    result =
        authzHandler.authorizeWriteSingleCoil(
            readWriteAuthzContext, 1, new WriteSingleCoilRequest(0, true));
    assertEquals(AuthzResult.AUTHORIZED, result);
  }

  @Test
  void testAuthorizeWriteSingleRegister() {
    AuthzResult result =
        authzHandler.authorizeWriteSingleRegister(
            readOnlyAuthzContext, 1, new WriteSingleRegisterRequest(0, (short) 1));
    assertEquals(AuthzResult.NOT_AUTHORIZED, result);

    result =
        authzHandler.authorizeWriteSingleRegister(
            writeOnlyAuthzContext, 1, new WriteSingleRegisterRequest(0, (short) 1));
    assertEquals(AuthzResult.AUTHORIZED, result);

    result =
        authzHandler.authorizeWriteSingleRegister(
            readWriteAuthzContext, 1, new WriteSingleRegisterRequest(0, (short) 1));
    assertEquals(AuthzResult.AUTHORIZED, result);
  }

  @Test
  void testAuthorizeWriteMultipleCoils() {
    AuthzResult result =
        authzHandler.authorizeWriteMultipleCoils(
            readOnlyAuthzContext, 1, new WriteMultipleCoilsRequest(0, 1, new byte[] {1}));
    assertEquals(AuthzResult.NOT_AUTHORIZED, result);

    result =
        authzHandler.authorizeWriteMultipleCoils(
            writeOnlyAuthzContext, 1, new WriteMultipleCoilsRequest(0, 1, new byte[] {1}));
    assertEquals(AuthzResult.AUTHORIZED, result);

    result =
        authzHandler.authorizeWriteMultipleCoils(
            readWriteAuthzContext, 1, new WriteMultipleCoilsRequest(0, 1, new byte[] {1}));
    assertEquals(AuthzResult.AUTHORIZED, result);
  }

  @Test
  void testAuthorizeWriteMultipleRegisters() {
    AuthzResult result =
        authzHandler.authorizeWriteMultipleRegisters(
            readOnlyAuthzContext, 1, new WriteMultipleRegistersRequest(0, 1, new byte[] {1, 2}));
    assertEquals(AuthzResult.NOT_AUTHORIZED, result);

    result =
        authzHandler.authorizeWriteMultipleRegisters(
            writeOnlyAuthzContext, 1, new WriteMultipleRegistersRequest(0, 1, new byte[] {1, 2}));
    assertEquals(AuthzResult.AUTHORIZED, result);

    result =
        authzHandler.authorizeWriteMultipleRegisters(
            readWriteAuthzContext, 1, new WriteMultipleRegistersRequest(0, 1, new byte[] {1, 2}));
    assertEquals(AuthzResult.AUTHORIZED, result);
  }

  @Test
  void testAuthorizeMaskWriteRegister() {
    AuthzResult result =
        authzHandler.authorizeMaskWriteRegister(
            readOnlyAuthzContext, 1, new MaskWriteRegisterRequest(0, (short) 1, (short) 1));
    assertEquals(AuthzResult.NOT_AUTHORIZED, result);

    result =
        authzHandler.authorizeMaskWriteRegister(
            writeOnlyAuthzContext, 1, new MaskWriteRegisterRequest(0, (short) 1, (short) 1));
    assertEquals(AuthzResult.AUTHORIZED, result);

    result =
        authzHandler.authorizeMaskWriteRegister(
            readWriteAuthzContext, 1, new MaskWriteRegisterRequest(0, (short) 1, (short) 1));
    assertEquals(AuthzResult.AUTHORIZED, result);
  }

  @Test
  void testAuthorizeReadWriteMultipleRegisters() {
    AuthzResult result =
        authzHandler.authorizeReadWriteMultipleRegisters(
            readOnlyAuthzContext,
            1,
            new ReadWriteMultipleRegistersRequest(0, 1, 0, 1, new byte[] {1, 2}));
    assertEquals(AuthzResult.NOT_AUTHORIZED, result);

    result =
        authzHandler.authorizeReadWriteMultipleRegisters(
            writeOnlyAuthzContext,
            1,
            new ReadWriteMultipleRegistersRequest(0, 1, 0, 1, new byte[] {1, 2}));
    assertEquals(AuthzResult.NOT_AUTHORIZED, result);

    result =
        authzHandler.authorizeReadWriteMultipleRegisters(
            readWriteAuthzContext,
            1,
            new ReadWriteMultipleRegistersRequest(0, 1, 0, 1, new byte[] {1, 2}));
    assertEquals(AuthzResult.AUTHORIZED, result);
  }

  record TestAuthzContext(Optional<String> clientRole, X509Certificate[] clientCertificateChain)
      implements AuthzContext {
    TestAuthzContext(String clientRole) {
      this(Optional.of(clientRole), new X509Certificate[0]);
    }
  }
}

```

---

### `modbus/protocol/src/test/java/com/digitalpetri/modbus/Crc16Test.java`

```java
package com.digitalpetri.modbus;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

class Crc16Test {

  @Test
  void crc16() {
    // https://crccalc.com/?crc=123456789&method=crc16&datatype=hex&outtype=0
    Crc16 crc = new Crc16();
    crc.update(ByteBuffer.wrap(new byte[] {0x12, 0x34, 0x56, 0x78, 0x09}));
    int value = crc.getValue();

    assertEquals(0x2590, value);
  }

  @Test
  void reset() {
    Crc16 crc = new Crc16();
    crc.update(ByteBuffer.wrap(new byte[] {0x12, 0x34, 0x56, 0x78, 0x09}));
    crc.reset();

    assertEquals(0xFFFF, crc.getValue());
  }
}

```

---

### `modbus/protocol/src/test/java/com/digitalpetri/modbus/pdu/WriteSingleCoilResponseTest.java`

```java
package com.digitalpetri.modbus.pdu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

class WriteSingleCoilResponseTest {

  @Test
  void serializer() {
    for (int address = 0; address < 0xFFFF; address++) {
      for (boolean value : new boolean[] {true, false}) {
        var response = new WriteSingleCoilResponse(address, value);
        ByteBuffer buffer = ByteBuffer.allocate(256);

        WriteSingleCoilResponse.Serializer.encode(response, buffer);

        buffer.flip();

        WriteSingleCoilResponse decoded = WriteSingleCoilResponse.Serializer.decode(buffer);

        assertEquals(response, decoded);
      }
    }
  }
}

```

---

### `modbus/protocol/src/test/java/com/digitalpetri/modbus/pdu/WriteMultipleCoilsRequestTest.java`

```java
package com.digitalpetri.modbus.pdu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class WriteMultipleCoilsRequestTest {

  @Test
  void serializer() {
    for (int address = 0; address < 0xFFFF; address++) {
      for (int quantity = 0; quantity < 0x07B0; quantity++) {
        ByteBuffer buffer = ByteBuffer.allocate(256);

        byte[] values = new byte[(quantity + 7) / 8];
        Arrays.fill(values, (byte) 0xFF);

        var request = new WriteMultipleCoilsRequest(address, quantity, values);
        WriteMultipleCoilsRequest.Serializer.encode(request, buffer);

        buffer.flip();

        WriteMultipleCoilsRequest decoded = WriteMultipleCoilsRequest.Serializer.decode(buffer);

        assertEquals(request, decoded);
      }
    }
  }
}

```

---

### `modbus/protocol/src/test/java/com/digitalpetri/modbus/pdu/WriteSingleRegisterResponseTest.java`

```java
package com.digitalpetri.modbus.pdu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

class WriteSingleRegisterResponseTest {

  @Test
  void serializer() {
    for (int address = 0; address < 0xFFFF; address++) {
      for (int value : new int[] {0, 1, 0xFFFF}) {
        ByteBuffer buffer = ByteBuffer.allocate(256);

        var response = new WriteSingleRegisterResponse(address, value);

        WriteSingleRegisterResponse.Serializer.encode(response, buffer);

        buffer.flip();

        WriteSingleRegisterResponse decoded = WriteSingleRegisterResponse.Serializer.decode(buffer);

        assertEquals(response, decoded);
      }
    }
  }
}

```

---

### `modbus/protocol/src/test/java/com/digitalpetri/modbus/pdu/ReadInputRegistersResponseTest.java`

```java
package com.digitalpetri.modbus.pdu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

public class ReadInputRegistersResponseTest {

  @Test
  public void serializer() {
    byte[] registers = new byte[] {0x01, 0x02, 0x03, 0x04};
    var response = new ReadInputRegistersResponse(registers);

    ByteBuffer buffer = ByteBuffer.allocate(256);
    ReadInputRegistersResponse.Serializer.encode(response, buffer);

    buffer.flip();

    ReadInputRegistersResponse decoded = ReadInputRegistersResponse.Serializer.decode(buffer);

    assertEquals(response, decoded);
  }
}

```

---

### `modbus/protocol/src/test/java/com/digitalpetri/modbus/pdu/WriteSingleCoilRequestTest.java`

```java
package com.digitalpetri.modbus.pdu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

class WriteSingleCoilRequestTest {

  @Test
  void serializer() {
    for (int address = 0; address < 0xFFFF; address++) {
      for (boolean value : new boolean[] {true, false}) {
        ByteBuffer buffer = ByteBuffer.allocate(256);

        var request = new WriteSingleCoilRequest(address, value);
        WriteSingleCoilRequest.Serializer.encode(request, buffer);

        buffer.flip();

        WriteSingleCoilRequest decoded = WriteSingleCoilRequest.Serializer.decode(buffer);

        assertEquals(request, decoded);
      }
    }
  }
}

```

---

### `modbus/protocol/src/test/java/com/digitalpetri/modbus/pdu/WriteMultipleRegistersRequestTest.java`

```java
package com.digitalpetri.modbus.pdu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class WriteMultipleRegistersRequestTest {

  @Test
  void serializer() {
    for (int address = 0; address < 0xFFFF; address++) {
      for (int quantity = 1; quantity < 0x007B; quantity++) {
        ByteBuffer buffer = ByteBuffer.allocate(256);

        byte[] values = new byte[quantity * 2];
        Arrays.fill(values, (byte) 0xFF);

        var request = new WriteMultipleRegistersRequest(address, quantity, values);

        WriteMultipleRegistersRequest.Serializer.encode(request, buffer);

        buffer.flip();

        WriteMultipleRegistersRequest decoded =
            WriteMultipleRegistersRequest.Serializer.decode(buffer);

        assertEquals(request, decoded);
      }
    }
  }
}

```

---

### `modbus/protocol/src/test/java/com/digitalpetri/modbus/pdu/ReadDiscreteInputsRequestTest.java`

```java
package com.digitalpetri.modbus.pdu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

class ReadDiscreteInputsRequestTest {

  @Test
  public void serializer() {
    for (int address = 0; address < 0xFFFF; address++) {
      for (short quantity = 1; quantity <= 2000; quantity++) {
        ByteBuffer buffer = ByteBuffer.allocate(256);

        var request = new ReadDiscreteInputsRequest(address, quantity);
        ReadDiscreteInputsRequest.Serializer.encode(request, buffer);

        buffer.flip();

        ReadDiscreteInputsRequest decoded = ReadDiscreteInputsRequest.Serializer.decode(buffer);

        assertEquals(request, decoded);
      }
    }
  }
}

```

---

### `modbus/protocol/src/test/java/com/digitalpetri/modbus/pdu/ReadWriteMultipleRegistersResponseTest.java`

```java
package com.digitalpetri.modbus.pdu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

class ReadWriteMultipleRegistersResponseTest {

  @Test
  void serialize() {
    byte[] registers = new byte[] {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};
    var response = new ReadWriteMultipleRegistersResponse(registers);

    ByteBuffer buffer = ByteBuffer.allocate(256);
    ReadWriteMultipleRegistersResponse.Serializer.encode(response, buffer);

    buffer.flip();

    ReadWriteMultipleRegistersResponse decoded =
        ReadWriteMultipleRegistersResponse.Serializer.decode(buffer);

    assertEquals(response, decoded);
  }
}

```

---

### `modbus/protocol/src/test/java/com/digitalpetri/modbus/pdu/ReadCoilsRequestTest.java`

```java
package com.digitalpetri.modbus.pdu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

class ReadCoilsRequestTest {

  @Test
  void serializer() {
    for (int address = 0; address < 0xFFFF; address++) {
      for (short quantity = 1; quantity <= 2000; quantity++) {
        ByteBuffer buffer = ByteBuffer.allocate(256);

        var request = new ReadCoilsRequest(address, quantity);
        ReadCoilsRequest.Serializer.encode(request, buffer);

        buffer.flip();

        ReadCoilsRequest decoded = ReadCoilsRequest.Serializer.decode(buffer);

        assertEquals(request, decoded);
      }
    }
  }
}

```

---

### `modbus/protocol/src/test/java/com/digitalpetri/modbus/pdu/WriteMultipleCoilsResponseTest.java`

```java
package com.digitalpetri.modbus.pdu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

class WriteMultipleCoilsResponseTest {

  @Test
  void serializer() {
    for (int address = 0; address < 0xFFFF; address++) {
      for (int quantity = 1; quantity < 0x07B0; quantity++) {
        ByteBuffer buffer = ByteBuffer.allocate(256);

        var response = new WriteMultipleCoilsResponse(address, quantity);
        WriteMultipleCoilsResponse.Serializer.encode(response, buffer);

        buffer.flip();

        WriteMultipleCoilsResponse decoded = WriteMultipleCoilsResponse.Serializer.decode(buffer);

        assertEquals(response, decoded);
      }
    }
  }
}

```

---

### `modbus/protocol/src/test/java/com/digitalpetri/modbus/pdu/ReadWriteMultipleRegistersRequestTest.java`

```java
package com.digitalpetri.modbus.pdu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.util.Random;
import org.junit.jupiter.api.Test;

class ReadWriteMultipleRegistersRequestTest {

  @Test
  void serializer() {
    var random = new Random();
    int address = random.nextInt(0xFFFF + 1);
    int quantity = random.nextInt(125) + 1;
    int writeAddress = random.nextInt(0xFFFF + 1);
    int writeQuantity = random.nextInt(125) + 1;
    byte[] writeValues = new byte[writeQuantity * 2];
    random.nextBytes(writeValues);

    ByteBuffer buffer = ByteBuffer.allocate(256);

    var request =
        new ReadWriteMultipleRegistersRequest(
            address, quantity, writeAddress, writeQuantity, writeValues);

    ReadWriteMultipleRegistersRequest.Serializer.encode(request, buffer);

    buffer.flip();

    ReadWriteMultipleRegistersRequest decoded =
        ReadWriteMultipleRegistersRequest.Serializer.decode(buffer);

    assertEquals(request, decoded);
  }
}

```

---

### `modbus/protocol/src/test/java/com/digitalpetri/modbus/pdu/WriteSingleRegisterRequestTest.java`

```java
package com.digitalpetri.modbus.pdu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

class WriteSingleRegisterRequestTest {

  @Test
  void serializer() {
    for (int address = 0; address < 0xFFFF; address++) {
      for (int value : new int[] {0, 1, 0xFFFF}) {
        ByteBuffer buffer = ByteBuffer.allocate(256);

        var request = new WriteSingleRegisterRequest(address, value);
        WriteSingleRegisterRequest.Serializer.encode(request, buffer);

        buffer.flip();

        WriteSingleRegisterRequest decoded = WriteSingleRegisterRequest.Serializer.decode(buffer);

        assertEquals(request, decoded);
      }
    }
  }
}

```

---

### `modbus/protocol/src/test/java/com/digitalpetri/modbus/pdu/ReadHoldingRegistersResponseTest.java`

```java
package com.digitalpetri.modbus.pdu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

class ReadHoldingRegistersResponseTest {

  @Test
  void serializer() {
    byte[] registers = new byte[] {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};
    var response = new ReadHoldingRegistersResponse(registers);

    ByteBuffer buffer = ByteBuffer.allocate(256);
    ReadHoldingRegistersResponse.Serializer.encode(response, buffer);

    buffer.flip();

    ReadHoldingRegistersResponse decoded = ReadHoldingRegistersResponse.Serializer.decode(buffer);

    assertEquals(response, decoded);
  }
}

```

---

### `modbus/protocol/src/test/java/com/digitalpetri/modbus/pdu/WriteMultipleRegistersResponseTest.java`

```java
package com.digitalpetri.modbus.pdu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

class WriteMultipleRegistersResponseTest {

  @Test
  void serializer() {
    for (int address = 0; address < 0xFFFF; address++) {
      for (int quantity = 1; quantity < 0x007B; quantity++) {
        ByteBuffer buffer = ByteBuffer.allocate(256);

        var response = new WriteMultipleRegistersResponse(address, quantity);
        WriteMultipleRegistersResponse.Serializer.encode(response, buffer);

        buffer.flip();

        WriteMultipleRegistersResponse decoded =
            WriteMultipleRegistersResponse.Serializer.decode(buffer);

        assertEquals(response, decoded);
      }
    }
  }
}

```

---

### `modbus/protocol/src/test/java/com/digitalpetri/modbus/pdu/ReadDiscreteInputsResponseTest.java`

```java
package com.digitalpetri.modbus.pdu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.util.Random;
import org.junit.jupiter.api.Test;

public class ReadDiscreteInputsResponseTest {

  @Test
  public void serializer() {
    ByteBuffer buffer = ByteBuffer.allocate(256);

    byte[] bs = new byte[10];
    new Random().nextBytes(bs);

    var response = new ReadDiscreteInputsResponse(bs);
    ReadDiscreteInputsResponse.Serializer.encode(response, buffer);

    buffer.flip();

    ReadDiscreteInputsResponse decoded = ReadDiscreteInputsResponse.Serializer.decode(buffer);

    assertEquals(response, decoded);
  }
}

```

---

### `modbus/protocol/src/test/java/com/digitalpetri/modbus/pdu/MaskWriteRegisterRequestTest.java`

```java
package com.digitalpetri.modbus.pdu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

class MaskWriteRegisterRequestTest {

  @Test
  void serializer() {
    for (int address = 0; address < 0xFFFF; address += 256) {
      for (int andMask = 0; andMask <= 0xFFFF; andMask += 256) {
        for (int orMask = 0; orMask <= 0xFFFF; orMask += 256) {
          ByteBuffer buffer = ByteBuffer.allocate(256);

          var request = new MaskWriteRegisterRequest(address, andMask, orMask);
          MaskWriteRegisterRequest.Serializer.encode(request, buffer);

          buffer.flip();

          MaskWriteRegisterRequest decoded = MaskWriteRegisterRequest.Serializer.decode(buffer);

          assertEquals(request, decoded);
        }
      }
    }
  }
}

```

---

### `modbus/protocol/src/test/java/com/digitalpetri/modbus/pdu/ReadCoilsResponseTest.java`

```java
package com.digitalpetri.modbus.pdu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.util.Random;
import org.junit.jupiter.api.Test;

class ReadCoilsResponseTest {

  @Test
  void serializer() {
    var buffer = ByteBuffer.allocate(256);

    byte[] bs = new byte[10];
    new Random().nextBytes(bs);

    var response = new ReadCoilsResponse(bs);
    ReadCoilsResponse.Serializer.encode(response, buffer);

    buffer.flip();

    ReadCoilsResponse decoded = ReadCoilsResponse.Serializer.decode(buffer);

    assertEquals(response, decoded);
  }
}

```

---

### `modbus/protocol/src/test/java/com/digitalpetri/modbus/pdu/ReadInputRegistersRequestTest.java`

```java
package com.digitalpetri.modbus.pdu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

class ReadInputRegistersRequestTest {

  @Test
  public void serializer() {
    for (int address = 0; address < 0xFFFF; address++) {
      for (short quantity = 1; quantity <= 125; quantity++) {
        ByteBuffer buffer = ByteBuffer.allocate(256);

        var request = new ReadInputRegistersRequest(address, quantity);
        ReadInputRegistersRequest.Serializer.encode(request, buffer);

        buffer.flip();

        ReadInputRegistersRequest decoded = ReadInputRegistersRequest.Serializer.decode(buffer);

        assertEquals(request, decoded);
      }
    }
  }
}

```

---

### `modbus/protocol/src/test/java/com/digitalpetri/modbus/pdu/ReadHoldingRegistersRequestTest.java`

```java
package com.digitalpetri.modbus.pdu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

class ReadHoldingRegistersRequestTest {

  @Test
  void serializer() {
    for (int address = 0; address < 0xFFFF; address++) {
      for (short quantity = 1; quantity <= 125; quantity++) {
        ByteBuffer buffer = ByteBuffer.allocate(256);

        var request = new ReadHoldingRegistersRequest(address, quantity);
        ReadHoldingRegistersRequest.Serializer.encode(request, buffer);

        buffer.flip();

        ReadHoldingRegistersRequest decoded = ReadHoldingRegistersRequest.Serializer.decode(buffer);

        assertEquals(request, decoded);
      }
    }
  }
}

```

---

### `modbus/protocol/src/test/java/com/digitalpetri/modbus/pdu/MaskWriteRegisterResponseTest.java`

```java
package com.digitalpetri.modbus.pdu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

class MaskWriteRegisterResponseTest {

  @Test
  void serializer() {
    for (int address = 0; address < 0xFFFF; address += 256) {
      for (int andMask = 0; andMask <= 0xFFFF; andMask += 256) {
        for (int orMask = 0; orMask <= 0xFFFF; orMask += 256) {
          ByteBuffer buffer = ByteBuffer.allocate(256);

          var response = new MaskWriteRegisterResponse(address, andMask, orMask);
          MaskWriteRegisterResponse.Serializer.encode(response, buffer);

          buffer.flip();

          MaskWriteRegisterResponse decoded = MaskWriteRegisterResponse.Serializer.decode(buffer);

          assertEquals(response, decoded);
        }
      }
    }
  }
}

```

---

### `modbus/protocol/src/test/java/com/digitalpetri/modbus/ModbusRtuRequestFrameParserTest.java`

```java
package com.digitalpetri.modbus;

import static com.digitalpetri.modbus.Util.partitions;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.digitalpetri.modbus.ModbusRtuRequestFrameParser.Accumulated;
import com.digitalpetri.modbus.ModbusRtuRequestFrameParser.ParserState;
import java.nio.ByteBuffer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ModbusRtuRequestFrameParserTest {

  private static final byte[] READ_COILS =
      new byte[] {0x01, 0x01, 0x00, 0x00, 0x00, 0x08, (byte) 0xCA, (byte) 0xFE};

  private static final byte[] WRITE_MULTIPLE_REGISTERS =
      new byte[] {
        0x01, 0x10, 0x00, 0x00, 0x00, 0x02, 0x04, 0x00, 0x01, 0x00, 0x02, (byte) 0x8B, (byte) 0x3A
      };

  @Test
  void readCoils() {
    parseValidRequest(READ_COILS);
  }

  @Test
  void writeMultipleRegisters() {
    parseValidRequest(WRITE_MULTIPLE_REGISTERS);
  }

  private void parseValidRequest(byte[] validRequestData) {
    var parser = new ModbusRtuRequestFrameParser();

    for (int i = 1; i <= validRequestData.length; i++) {
      parser.reset();

      Stream<byte[]> chunks = partitions(validRequestData, i);

      chunks.forEach(
          chunk -> {
            ParserState state = parser.parse(chunk);
            System.out.println(state);
          });
      System.out.println("--");

      ParserState state = parser.getState();
      if (state instanceof Accumulated a) {
        int expectedUnitId = validRequestData[0] & 0xFF;
        ByteBuffer expectedPdu = ByteBuffer.wrap(validRequestData, 1, validRequestData.length - 3);
        ByteBuffer expectedCrc = ByteBuffer.wrap(validRequestData, validRequestData.length - 2, 2);
        assertEquals(expectedUnitId, a.frame().unitId());
        assertEquals(expectedPdu, a.frame().pdu());
        assertEquals(expectedCrc, a.frame().crc());
      }
    }
  }
}

```

---

### `modbus/protocol/src/test/java/com/digitalpetri/modbus/client/ModbusRtuClientTest.java`

```java
package com.digitalpetri.modbus.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.digitalpetri.modbus.ModbusRtuFrame;
import com.digitalpetri.modbus.exceptions.ModbusExecutionException;
import com.digitalpetri.modbus.exceptions.ModbusTimeoutException;
import com.digitalpetri.modbus.pdu.ReadHoldingRegistersRequest;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

public class ModbusRtuClientTest {

  @Test
  void timeoutHandleIsRemoved() throws ModbusExecutionException {
    var transport = new TimeoutRtuTransport();
    var client =
        ModbusRtuClient.create(transport, cfg -> cfg.requestTimeout = Duration.ofMillis(100));

    client.connect();

    assertThrows(
        ModbusTimeoutException.class,
        () -> client.readHoldingRegisters(1, new ReadHoldingRegistersRequest(0, 10)));

    assertEquals(0, client.timeouts.size());
  }

  private static class TimeoutRtuTransport implements ModbusRtuClientTransport {
    boolean connected = false;

    @Override
    public void resetFrameParser() {}

    @Override
    public CompletionStage<Void> connect() {
      connected = true;
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<Void> disconnect() {
      connected = false;
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public boolean isConnected() {
      return connected;
    }

    @Override
    public CompletionStage<Void> send(ModbusRtuFrame frame) {
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public void receive(Consumer<ModbusRtuFrame> frameReceiver) {}
  }
}

```

---

### `modbus/protocol/src/test/java/com/digitalpetri/modbus/client/DefaultTransactionSequenceTest.java`

```java
package com.digitalpetri.modbus.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.digitalpetri.modbus.client.ModbusTcpClient.DefaultTransactionSequence;
import org.junit.jupiter.api.Test;

class DefaultTransactionSequenceTest {

  @Test
  void rollover() {
    DefaultTransactionSequence sequence = new DefaultTransactionSequence();

    // Assert that transactions are generated in the range [0, 65535]
    // and that they roll over back to 0.
    for (int i = 0; i < 2; i++) {
      for (int id = 0; id < 65536; id++) {
        assertEquals(id, sequence.next());
      }
    }
  }
}

```

---

### `modbus/protocol/src/test/java/com/digitalpetri/modbus/client/ModbusTcpClientTest.java`

```java
package com.digitalpetri.modbus.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.digitalpetri.modbus.MbapHeader;
import com.digitalpetri.modbus.ModbusTcpFrame;
import com.digitalpetri.modbus.exceptions.ModbusException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

public class ModbusTcpClientTest {

  /**
   * Tests handling of an erroneous empty response PDU.
   *
   * @see <a
   *     href="https://github.com/digitalpetri/modbus/issues/121">https://github.com/digitalpetri/modbus/issues/121</a>
   */
  @Test
  void emptyResponsePdu() {
    var transport = new TestTransport();
    var client = ModbusTcpClient.create(transport);

    CompletionStage<byte[]> cs = client.sendRawAsync(1, new byte[] {0x04, 0x03, 0x00, 0x00, 0x01});

    transport.frameReceiver.accept(
        new ModbusTcpFrame(new MbapHeader(0, 1, 1, 1), ByteBuffer.allocate(0)));

    ExecutionException ex =
        assertThrows(ExecutionException.class, () -> cs.toCompletableFuture().get());

    ModbusException cause = (ModbusException) ex.getCause();
    assertEquals("empty response PDU", cause.getMessage());
  }

  /**
   * Tests handling of a malformed exception response PDU containing only the function code | 0x80
   * and missing the required exception code byte.
   */
  @Test
  void malformedExceptionResponsePdu() {
    var transport = new TestTransport();
    var client = ModbusTcpClient.create(transport);

    // Send a request with function code 0x04
    CompletionStage<byte[]> cs = client.sendRawAsync(1, new byte[] {0x04, 0x03, 0x00, 0x00, 0x01});

    // Receive a malformed exception response: only 1 byte (0x84), no exception code
    transport.frameReceiver.accept(
        new ModbusTcpFrame(new MbapHeader(0, 1, 1, 1), ByteBuffer.wrap(new byte[] {(byte) 0x84})));

    ExecutionException ex =
        assertThrows(ExecutionException.class, () -> cs.toCompletableFuture().get());

    ModbusException cause = (ModbusException) ex.getCause();
    assertEquals("malformed exception response PDU: 84", cause.getMessage());
  }

  private static class TestTransport implements ModbusTcpClientTransport {

    boolean connected = false;
    ModbusTcpFrame lastFrameSent;
    Consumer<ModbusTcpFrame> frameReceiver;

    @Override
    public CompletionStage<Void> connect() {
      connected = true;
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<Void> disconnect() {
      connected = false;
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public boolean isConnected() {
      return connected;
    }

    @Override
    public CompletionStage<Void> send(ModbusTcpFrame frame) {
      lastFrameSent = frame;
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public void receive(Consumer<ModbusTcpFrame> frameReceiver) {
      this.frameReceiver = frameReceiver;
    }
  }
}

```

---

### `modbus/protocol/src/test/java/com/digitalpetri/modbus/Util.java`

```java
package com.digitalpetri.modbus;

import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Util {

  static Stream<byte[]> partitions(byte[] source, int partitionSize) {
    int size = source.length;

    if (size == 0) {
      return Stream.empty();
    }

    int fullChunks = (size - 1) / partitionSize;

    return IntStream.range(0, fullChunks + 1)
        .mapToObj(
            n -> {
              int fromIndex = n * partitionSize;
              int toIndex = n == fullChunks ? size : (n + 1) * partitionSize;

              return Arrays.copyOfRange(source, fromIndex, toIndex);
            });
  }
}

```

---

### `strict-machine/src/main/java/com/digitalpetri/fsm/StrictMachine.java`

```java
/*
 * Copyright (c) 2024 Kevin Herron
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.digitalpetri.fsm;

import com.digitalpetri.fsm.dsl.ActionContext;
import com.digitalpetri.fsm.dsl.ActionProxy;
import com.digitalpetri.fsm.dsl.Transition;
import com.digitalpetri.fsm.dsl.TransitionAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Function;

public class StrictMachine<S, E> implements Fsm<S, E> {

  private volatile boolean pollExecuted = false;
  private final Object queueLock = new Object();
  private final ArrayDeque<PendingEvent> eventQueue = new ArrayDeque<>();
  private final ArrayDeque<PendingEvent> eventShelf = new ArrayDeque<>();

  private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);
  private final Map<FsmContext.Key<?>, Object> contextValues = new ConcurrentHashMap<>();
  private final AtomicReference<S> state = new AtomicReference<>();

  private final Logger logger;
  private final Map<String, String> mdc;
  private final Executor executor;
  private final Object userContext;
  private final ActionProxy<S, E> actionProxy;
  private final List<Transition<S, E>> transitions;
  private final List<TransitionAction<S, E>> transitionActions;

  public StrictMachine(
      String loggerName,
      Map<String, String> mdc,
      Executor executor,
      Object userContext,
      ActionProxy<S, E> actionProxy,
      S initialState,
      List<Transition<S, E>> transitions,
      List<TransitionAction<S, E>> transitionActions
  ) {

    this.logger = LoggerFactory.getLogger(loggerName);
    this.mdc = mdc;
    this.executor = executor;
    this.userContext = userContext;
    this.actionProxy = actionProxy;
    this.transitions = transitions;
    this.transitionActions = transitionActions;

    state.set(initialState);
  }

  @Override
  public S getState() {
    try {
      readWriteLock.readLock().lock();

      return state.get();
    } finally {
      readWriteLock.readLock().unlock();
    }
  }

  @Override
  public void fireEvent(E event) {
    fireEvent(event, null);
  }

  @Override
  public void fireEvent(E event, Consumer<S> stateConsumer) {
    synchronized (queueLock) {
      eventQueue.add(new PendingEvent(event, stateConsumer));

      maybeExecutePoll();
    }
  }

  @Override
  public S fireEventBlocking(E event) throws InterruptedException {
    var transferQueue = new LinkedTransferQueue<S>();

    fireEvent(event, transferQueue::put);

    return transferQueue.take();
  }

  @Override
  public <T> T getFromContext(Function<FsmContext<S, E>, T> get) {
    try {
      readWriteLock.writeLock().lock();

      return get.apply(new FsmContextImpl());
    } finally {
      readWriteLock.writeLock().unlock();
    }
  }

  @Override
  public void withContext(Consumer<FsmContext<S, E>> contextConsumer) {
    try {
      readWriteLock.writeLock().lock();

      contextConsumer.accept(new FsmContextImpl());
    } finally {
      readWriteLock.writeLock().unlock();
    }
  }

  private void maybeExecutePoll() {
    synchronized (queueLock) {
      if (!pollExecuted && !eventQueue.isEmpty()) {
        executor.execute(new PollAndEvaluate());
        pollExecuted = true;
      }
    }
  }

  private class PendingEvent {

    final E event;
    final Consumer<S> stateConsumer;

    PendingEvent(E event, Consumer<S> stateConsumer) {
      this.event = event;
      this.stateConsumer = stateConsumer;
    }
  }

  private class PollAndEvaluate implements Runnable {

    @Override
    public void run() {
      PendingEvent pending;

      synchronized (queueLock) {
        pending = eventQueue.poll();

        if (pending == null) {
          return;
        }
      }

      E event = pending.event;

      try {
        readWriteLock.writeLock().lock();

        S currState = state.get();
        S nextState = currState;

        var ctx = new FsmContextImpl();

        for (Transition<S, E> transition : transitions) {
          if (transition.matches(ctx, currState, event)) {
            nextState = transition.target();
            break;
          }
        }

        state.set(nextState);

        if (logger.isDebugEnabled()) {
          mdc.forEach(MDC::put);
          try {
            logger.debug(
                "{} x {} = {}",
                padRight(String.format("S(%s)", currState)),
                padRight(String.format("E(%s)", event)),
                padRight(String.format("S'(%s)", nextState))
            );
          } finally {
            mdc.keySet().forEach(MDC::remove);
          }
        }

        var actionContext = new ActionContextImpl(
            currState,
            nextState,
            event
        );

        var matchingActions = new ArrayList<TransitionAction<S, E>>();

        for (TransitionAction<S, E> transitionAction : transitionActions) {
          if (transitionAction.matches(currState, nextState, event)) {
            matchingActions.add(transitionAction);
          }
        }

        if (logger.isTraceEnabled()) {
          mdc.forEach(MDC::put);
          try {
            logger.trace("found {} matching TransitionActions", matchingActions.size());
          } finally {
            mdc.keySet().forEach(MDC::remove);
          }
        }

        matchingActions.forEach(transitionAction -> {
          try {
            if (actionProxy == null) {
              if (logger.isTraceEnabled()) {
                mdc.forEach(MDC::put);
                try {
                  logger.trace("executing TransitionAction: {}", transitionAction);
                } finally {
                  mdc.keySet().forEach(MDC::remove);
                }
              }

              transitionAction.execute(actionContext);
            } else {
              if (logger.isTraceEnabled()) {
                mdc.forEach(MDC::put);
                try {
                  logger.trace("executing (via proxy) TransitionAction: {}", transitionAction);
                } finally {
                  mdc.keySet().forEach(MDC::remove);
                }
              }

              actionProxy.execute(actionContext, transitionAction::execute);
            }
          } catch (Throwable ex) {

            mdc.forEach(MDC::put);
            try {
              logger.warn("uncaught Throwable executing TransitionAction: {}",
                  transitionAction, ex);
            } finally {
              mdc.keySet().forEach(MDC::remove);
            }
          }
        });

      } finally {
        readWriteLock.writeLock().unlock();
      }

      if (pending.stateConsumer != null) {
        pending.stateConsumer.accept(state.get());
      }

      synchronized (queueLock) {
        if (eventQueue.isEmpty()) {
          pollExecuted = false;
        } else {
          // pollExecuted remains true
          executor.execute(new PollAndEvaluate());
        }
      }
    }
  }

  private static final int PADDING = 24;

  private static String padRight(String s) {
    return String.format("%1$-" + PADDING + "s", s);
  }

  private class FsmContextImpl implements FsmContext<S, E> {

    @Override
    public S currentState() {
      return getState();
    }

    @Override
    public void fireEvent(E event) {
      StrictMachine.this.fireEvent(event);
    }

    @Override
    public void shelveEvent(E event) {
      try {
        readWriteLock.writeLock().lock();

        eventShelf.add(new PendingEvent(event, s -> {}));
      } finally {
        readWriteLock.writeLock().unlock();
      }
    }

    @Override
    public void processShelvedEvents() {
      try {
        readWriteLock.writeLock().lock();

        synchronized (queueLock) {
          while (!eventShelf.isEmpty()) {
            eventQueue.addFirst(eventShelf.removeLast());
          }

          maybeExecutePoll();
        }
      } finally {
        readWriteLock.writeLock().unlock();
      }
    }

    @Override
    public Object get(Key<?> key) {
      try {
        readWriteLock.readLock().lock();

        return contextValues.get(key);
      } finally {
        readWriteLock.readLock().unlock();
      }
    }

    @Override
    public Object remove(Key<?> key) {
      try {
        readWriteLock.writeLock().lock();

        return contextValues.remove(key);
      } finally {
        readWriteLock.writeLock().unlock();
      }
    }

    @Override
    public void set(Key<?> key, Object value) {
      try {
        readWriteLock.writeLock().lock();

        contextValues.put(key, value);
      } finally {
        readWriteLock.writeLock().unlock();
      }
    }

    @Override
    public Object getUserContext() {
      return userContext;
    }

  }

  private class ActionContextImpl extends FsmContextImpl implements ActionContext<S, E> {

    private final S from;
    private final S to;
    private final E event;

    ActionContextImpl(S from, S to, E event) {
      this.from = from;
      this.to = to;
      this.event = event;
    }

    @Override
    public S from() {
      return from;
    }

    @Override
    public S to() {
      return to;
    }

    @Override
    public E event() {
      return event;
    }

  }

}

```

---

### `strict-machine/src/main/java/com/digitalpetri/fsm/dsl/ActionToBuilder.java`

```java
/*
 * Copyright (c) 2024 Kevin Herron
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.digitalpetri.fsm.dsl;

import java.util.LinkedList;
import java.util.Objects;
import java.util.function.Predicate;

public class ActionToBuilder<S extends Enum<S>, E> {

  private final Predicate<S> toFilter;
  private final LinkedList<TransitionAction<S, E>> transitionActions;

  ActionToBuilder(Predicate<S> toFilter, LinkedList<TransitionAction<S, E>> transitionActions) {
    this.toFilter = toFilter;
    this.transitionActions = transitionActions;
  }

  public ViaBuilder<S, E> from(S from) {
    return from(s -> Objects.equals(s, from));
  }

  public ViaBuilder<S, E> from(Predicate<S> fromFilter) {
    return new ViaBuilder<>(
        fromFilter,
        toFilter,
        transitionActions
    );
  }

  public ViaBuilder<S, E> fromAny() {
    return from(s -> true);
  }

}

```

---

### `strict-machine/src/main/java/com/digitalpetri/fsm/dsl/GuardBuilder.java`

```java
/*
 * Copyright (c) 2024 Kevin Herron
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.digitalpetri.fsm.dsl;

import com.digitalpetri.fsm.FsmContext;

import java.util.LinkedList;
import java.util.Objects;
import java.util.function.Predicate;

public class GuardBuilder<S, E> extends ActionBuilder<S, E> {

  private final PredicatedTransition<S, E> transition;

  GuardBuilder(
      PredicatedTransition<S, E> transition,
      LinkedList<TransitionAction<S, E>> transitionActions
  ) {

    super(
        transition.getFrom(),
        s -> Objects.equals(s, transition.getTarget()),
        transition.getVia(),
        transitionActions
    );

    this.transition = transition;
  }

  public ActionBuilder<S, E> guardedBy(Predicate<FsmContext<S, E>> guard) {
    transition.setGuard(guard);

    return this;
  }

}

```

---

### `strict-machine/src/main/java/com/digitalpetri/fsm/dsl/ActionProxy.java`

```java
/*
 * Copyright (c) 2024 Kevin Herron
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.digitalpetri.fsm.dsl;

@FunctionalInterface
public interface ActionProxy<S, E> {

  /**
   * Execute this action.
   *
   * @param context the {@link ActionContext}.
   * @param action the {@link Action} to execute.
   */
  void execute(ActionContext<S, E> context, Action<S, E> action);

}

```

---

### `strict-machine/src/main/java/com/digitalpetri/fsm/dsl/TransitionBuilder.java`

```java
/*
 * Copyright (c) 2024 Kevin Herron
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.digitalpetri.fsm.dsl;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

public class TransitionBuilder<S extends Enum<S>, E> {

  private final S from;
  private final List<Transition<S, E>> transitions;
  private final LinkedList<TransitionAction<S, E>> transitionActions;

  TransitionBuilder(
      S from,
      List<Transition<S, E>> transitions,
      LinkedList<TransitionAction<S, E>> transitionActions
  ) {

    this.from = from;
    this.transitions = transitions;
    this.transitionActions = transitionActions;
  }

  /**
   * Continue defining a {@link Transition} that is triggered by {@code event}.
   *
   * @param event the event that triggers this transition.
   * @return a {@link TransitionTo}.
   */
  public TransitionTo<S, E> on(E event) {
    return to -> {
      PredicatedTransition<S, E> transition =
          Transitions.fromInstanceViaInstance(from, event, to);

      transitions.add(transition);

      return new GuardBuilder<>(transition, transitionActions);
    };
  }

  /**
   * Continue defining a {@link Transition} that is triggered by an event of type
   * {@code eventClass}.
   *
   * @param eventClass the †ype of event that triggers this transition.
   * @return a {@link TransitionTo}.
   */
  public TransitionTo<S, E> on(Class<? extends E> eventClass) {
    return to -> {
      PredicatedTransition<S, E> transition =
          Transitions.fromInstanceViaClass(from, eventClass, to);

      transitions.add(transition);

      return new GuardBuilder<>(transition, transitionActions);
    };
  }

  /**
   * Continue defining a {@link Transition} that is triggered by any event that passes
   * {@code eventFilter}.
   *
   * @param eventFilter the filter for events that trigger this transition.
   * @return a {@link TransitionTo}.
   */
  public TransitionTo<S, E> on(Predicate<E> eventFilter) {
    return to -> {
      PredicatedTransition<S, E> transition =
          Transitions.fromInstanceViaDynamic(from, eventFilter, to);

      transitions.add(transition);

      return new GuardBuilder<>(transition, transitionActions);
    };
  }

  /**
   * Continue defining a {@link Transition} that is triggered by any event.
   *
   * @return a {@link TransitionTo}.
   */
  public TransitionTo<S, E> onAny() {
    return on(e -> true);
  }

  public interface TransitionTo<S, E> {

    GuardBuilder<S, E> transitionTo(S state);

  }

}

```

---

### `strict-machine/src/main/java/com/digitalpetri/fsm/dsl/Action.java`

```java
/*
 * Copyright (c) 2024 Kevin Herron
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.digitalpetri.fsm.dsl;

@FunctionalInterface
public interface Action<S, E> {

  /**
   * Execute this action.
   *
   * @param context the {@link ActionContext}.
   */
  void execute(ActionContext<S, E> context);

}

```

---

### `strict-machine/src/main/java/com/digitalpetri/fsm/dsl/Transitions.java`

```java
/*
 * Copyright (c) 2024 Kevin Herron
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.digitalpetri.fsm.dsl;

import java.util.Objects;
import java.util.function.Predicate;

class Transitions {

  private Transitions() {}

  static <S, E> PredicatedTransition<S, E> fromInstanceViaClass(
      S state,
      Class<? extends E> event,
      S target
  ) {

    return new PredicatedTransition<>(
        s -> Objects.equals(s, state),
        e -> Objects.equals(e.getClass(), event),
        target
    );
  }

  static <S, E> PredicatedTransition<S, E> fromInstanceViaDynamic(
      S state,
      Predicate<E> via,
      S target
  ) {

    return new PredicatedTransition<>(
        s -> Objects.equals(s, state),
        via,
        target
    );
  }

  static <S, E> PredicatedTransition<S, E> fromInstanceViaInstance(
      S state,
      E event,
      S target
  ) {

    return new PredicatedTransition<>(
        s -> Objects.equals(s, state),
        e -> Objects.equals(e, event),
        target
    );
  }

}

```

---

### `strict-machine/src/main/java/com/digitalpetri/fsm/dsl/Transition.java`

```java
/*
 * Copyright (c) 2024 Kevin Herron
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.digitalpetri.fsm.dsl;

import com.digitalpetri.fsm.FsmContext;

public interface Transition<S, E> {

  /**
   * Get the target state of this transition.
   *
   * @return the target state of this transition.
   */
  S target();

  /**
   * Test whether this Transition is applicable for the current {@code state} and {@code event}.
   *
   * @param ctx the {@link FsmContext}.
   * @param state the current FSM state.
   * @param event the event being evaluated.
   * @return {@code true} if this transition is applicable for {@code state} and {@code event}.
   */
  boolean matches(FsmContext<S, E> ctx, S state, E event);


}

```

---

### `strict-machine/src/main/java/com/digitalpetri/fsm/dsl/ViaBuilder.java`

```java
/*
 * Copyright (c) 2024 Kevin Herron
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.digitalpetri.fsm.dsl;

import java.util.LinkedList;
import java.util.Objects;
import java.util.function.Predicate;

public class ViaBuilder<S, E> {

  private final Predicate<S> fromFilter;
  private final Predicate<S> toFilter;
  private final LinkedList<TransitionAction<S, E>> transitionActions;

  ViaBuilder(
      Predicate<S> fromFilter,
      Predicate<S> toFilter,
      LinkedList<TransitionAction<S, E>> transitionActions
  ) {

    this.fromFilter = fromFilter;
    this.toFilter = toFilter;
    this.transitionActions = transitionActions;
  }

  public ActionBuilder<S, E> via(E event) {
    return new ActionBuilder<>(
        fromFilter,
        toFilter,
        e -> Objects.equals(e, event),
        transitionActions
    );
  }

  public ActionBuilder<S, E> via(Class<? extends E> eventClass) {
    return new ActionBuilder<>(
        fromFilter,
        toFilter,
        e -> Objects.equals(e.getClass(), eventClass),
        transitionActions
    );
  }

  public ActionBuilder<S, E> via(Predicate<E> eventFilter) {
    return new ActionBuilder<>(
        fromFilter,
        toFilter,
        eventFilter,
        transitionActions
    );
  }

  public ActionBuilder<S, E> viaAny() {
    return new ActionBuilder<>(
        fromFilter,
        toFilter,
        e -> true,
        transitionActions
    );
  }

}

```

---

### `strict-machine/src/main/java/com/digitalpetri/fsm/dsl/TransitionAction.java`

```java
/*
 * Copyright (c) 2024 Kevin Herron
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.digitalpetri.fsm.dsl;

public interface TransitionAction<S, E> {

  /**
   * Execute the {@link Action}s backing this TransitionAction.
   *
   * @param context the {@link ActionContext}.
   */
  void execute(ActionContext<S, E> context);

  /**
   * Test whether this TransitionAction is applicable to the transition criteria.
   *
   * @param from the state transitioned from.
   * @param to the state transitioned to.
   * @param event the event that caused the transition.
   * @return {@code true} if this transition action is applicable to the transition criteria.
   */
  boolean matches(S from, S to, E event);

}

```

---

### `strict-machine/src/main/java/com/digitalpetri/fsm/dsl/ActionBuilder.java`

```java
/*
 * Copyright (c) 2024 Kevin Herron
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.digitalpetri.fsm.dsl;

import java.util.LinkedList;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ActionBuilder<S, E> {

  private Predicate<S> from;
  private Predicate<S> to;
  private Predicate<E> via;
  private final LinkedList<TransitionAction<S, E>> transitionActions;

  ActionBuilder(
      Predicate<S> from,
      Predicate<S> to,
      Predicate<E> via,
      LinkedList<TransitionAction<S, E>> transitionActions
  ) {

    this.from = from;
    this.to = to;
    this.via = via;
    this.transitionActions = transitionActions;
  }

  /**
   * Add {@code action} to the list of {@link TransitionAction}s to be executed.
   *
   * <p>Actions are executed in the order they appear in the list.
   *
   * @param action the action to execute.
   * @return this {@link ActionBuilder}.
   */
  public ActionBuilder<S, E> execute(Action<S, E> action) {
    return executeLast(action);
  }

  /**
   * Add {@code action} to the end of the list of {@link TransitionAction}s to be executed.
   *
   * <p>Actions are executed in the order they appear in the list.
   *
   * @param action the action to execute.
   * @return this {@link ActionBuilder}.
   */
  public ActionBuilder<S, E> executeLast(Action<S, E> action) {
    transitionActions.addLast(
        new PredicatedTransitionAction<>(
            from,
            to,
            via,
            action::execute
        )
    );

    return this;
  }

  /**
   * Add {@code action} to the beginning of the list of {@link TransitionAction}s to be executed.
   *
   * <p>Actions are executed in the order they appear in the list.
   *
   * @param action the action to execute.
   * @return this {@link ActionBuilder}.
   */
  public ActionBuilder<S, E> executeFirst(Action<S, E> action) {
    transitionActions.addFirst(
        new PredicatedTransitionAction<>(
            from,
            to,
            via,
            action::execute
        )
    );

    return this;
  }

  private static class PredicatedTransitionAction<S, E> implements TransitionAction<S, E> {

    private final Predicate<S> from;
    private final Predicate<S> to;
    private final Predicate<E> via;
    private final Consumer<ActionContext<S, E>> action;

    PredicatedTransitionAction(
        Predicate<S> from,
        Predicate<S> to,
        Predicate<E> via,
        Consumer<ActionContext<S, E>> action
    ) {

      this.from = from;
      this.to = to;
      this.via = via;
      this.action = action;
    }

    @Override
    public void execute(ActionContext<S, E> context) {
      action.accept(context);
    }

    @Override
    public boolean matches(S from, S to, E event) {
      return this.from.test(from) && this.to.test(to) && this.via.test(event);
    }

  }

}

```

---

### `strict-machine/src/main/java/com/digitalpetri/fsm/dsl/PredicatedTransition.java`

```java
/*
 * Copyright (c) 2024 Kevin Herron
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.digitalpetri.fsm.dsl;

import com.digitalpetri.fsm.FsmContext;

import java.util.function.Predicate;

class PredicatedTransition<S, E> implements Transition<S, E> {

  private volatile Predicate<FsmContext<S, E>> guard = ctx -> true;

  private final Predicate<S> from;
  private final Predicate<E> via;
  private final S target;

  PredicatedTransition(Predicate<S> from, Predicate<E> via, S target) {
    this.from = from;
    this.via = via;
    this.target = target;
  }

  @Override
  public S target() {
    return target;
  }

  @Override
  public boolean matches(FsmContext<S, E> ctx, S state, E event) {
    return from.test(state) && via.test(event) && guard.test(ctx);
  }

  Predicate<FsmContext<S, E>> getGuard() {
    return guard;
  }

  Predicate<S> getFrom() {
    return from;
  }

  Predicate<E> getVia() {
    return via;
  }

  S getTarget() {
    return target;
  }

  void setGuard(Predicate<FsmContext<S, E>> guard) {
    this.guard = guard;
  }

}

```

---

### `strict-machine/src/main/java/com/digitalpetri/fsm/dsl/ActionContext.java`

```java
/*
 * Copyright (c) 2024 Kevin Herron
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.digitalpetri.fsm.dsl;

import com.digitalpetri.fsm.FsmContext;

/**
 * The context in which a {@link Action} is being executed.
 *
 * <p>Provides access to the transition criteria: the from state, to state, and event that triggered
 * the transition.
 *
 * @param <S> state type
 * @param <E> event type
 */
public interface ActionContext<S, E> extends FsmContext<S, E> {

  /**
   * Get the state being transitioned from.
   *
   * @return the state being transitioned from.
   */
  S from();

  /**
   * Get the state transitioned to.
   *
   * @return the state transitioned to.
   */
  S to();

  /**
   * Get the event that caused the transition.
   *
   * @return the event that caused the transition.
   */
  E event();

}

```

---

### `strict-machine/src/main/java/com/digitalpetri/fsm/dsl/ActionFromBuilder.java`

```java
/*
 * Copyright (c) 2024 Kevin Herron
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.digitalpetri.fsm.dsl;

import java.util.LinkedList;
import java.util.Objects;
import java.util.function.Predicate;

public class ActionFromBuilder<S extends Enum<S>, E> {

  private final Predicate<S> fromFilter;
  private final LinkedList<TransitionAction<S, E>> transitionActions;

  ActionFromBuilder(Predicate<S> fromFilter, LinkedList<TransitionAction<S, E>> transitionActions) {
    this.fromFilter = fromFilter;
    this.transitionActions = transitionActions;
  }

  public ViaBuilder<S, E> to(S state) {
    return to(s -> Objects.equals(s, state));
  }

  public ViaBuilder<S, E> to(Predicate<S> toFilter) {
    return new ViaBuilder<>(
        fromFilter,
        toFilter,
        transitionActions
    );
  }

  public ViaBuilder<S, E> toAny() {
    return to(s -> true);
  }

}

```

---

### `strict-machine/src/main/java/com/digitalpetri/fsm/dsl/FsmBuilder.java`

```java
/*
 * Copyright (c) 2024 Kevin Herron
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.digitalpetri.fsm.dsl;

import com.digitalpetri.fsm.Fsm;
import com.digitalpetri.fsm.StrictMachine;

import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

public class FsmBuilder<S extends Enum<S>, E> {

  private static final AtomicLong INSTANCE_ID = new AtomicLong(0);

  private static final ExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadExecutor();

  private final List<Transition<S, E>> transitions = new ArrayList<>();

  private final LinkedList<TransitionAction<S, E>> transitionActions = new LinkedList<>();

  private ActionProxy<S, E> actionProxy = null;

  private final String loggerName;
  private final Map<String, String> mdc;
  private final Executor executor;
  private final Object userContext;

  public FsmBuilder() {
    this(
        StrictMachine.class.getName(),
        Map.of(),
        EXECUTOR_SERVICE,
        null
    );
  }

  public FsmBuilder(
      String loggerName,
      Map<String, String> mdc,
      Executor executor,
      Object userContext
  ) {

    this.loggerName = loggerName;
    this.mdc = mdc;
    this.executor = executor;
    this.userContext = userContext;
  }


  /**
   * Start defining a {@link Transition} from state {@code state}.
   *
   * @param state the state the transition begins in.
   * @return a {@link TransitionBuilder}.
   */
  public TransitionBuilder<S, E> when(S state) {
    return new TransitionBuilder<>(state, transitions, transitionActions);
  }

  /**
   * Start defining an {@link Action} that will be executed after an internal transition from/to
   * {@code state}.
   *
   * <p>The criteria for the event that causes this transition is defined on the returned
   * {@link ViaBuilder}.
   *
   * @param state the state experiencing an internal transition.
   * @return a {@link ViaBuilder}.
   */
  public ViaBuilder<S, E> onInternalTransition(S state) {
    return onTransitionFrom(state).to(state);
  }

  /**
   * Start defining an {@link Action} that will be executed after a transition to {@code state}.
   *
   * <p>Further criteria for execution will be defined on the returned {@link ActionToBuilder}.
   *
   * @param state the state being transitioned to.
   * @return an {@link ActionToBuilder}.
   */
  public ActionToBuilder<S, E> onTransitionTo(S state) {
    return onTransitionTo(s -> Objects.equals(s, state));
  }

  /**
   * Start defining an {@link Action} that will execute after a transition to any state that passes
   * {@code filter}.
   *
   * <p>Further criteria for execution will be defined on the returned {@link ActionToBuilder}.
   *
   * @param filter the filter for states being transitioned to.
   * @return an {@link ActionToBuilder}.
   */
  public ActionToBuilder<S, E> onTransitionTo(Predicate<S> filter) {
    return new ActionToBuilder<>(filter, transitionActions);
  }

  /**
   * Start defining an {@link Action} that will execute after a transition from {@code state}.
   *
   * @param state the state being transitioned from.
   * @return an {@link ActionFromBuilder}.
   */
  public ActionFromBuilder<S, E> onTransitionFrom(S state) {
    return onTransitionFrom(s -> Objects.equals(s, state));
  }

  /**
   * Start defining an {@link Action} that will execute after a transition from any state that
   * passes {@code filter}.
   *
   * @param filter the filter for states being transitioned from.
   * @return an {@link ActionFromBuilder}.
   */
  public ActionFromBuilder<S, E> onTransitionFrom(Predicate<S> filter) {
    return new ActionFromBuilder<>(filter, transitionActions);
  }

  /**
   * Add a manually defined {@link Transition}.
   *
   * @param transition the {@link Transition} to add.
   */
  public void addTransition(Transition<S, E> transition) {
    transitions.add(transition);
  }

  /**
   * Add a manually defined {@link TransitionAction}.
   *
   * @param transitionAction the {@link TransitionAction} to add.
   */
  public void addTransitionAction(TransitionAction<S, E> transitionAction) {
    transitionActions.add(transitionAction);
  }

  /**
   * Configure an {@link ActionProxy} for the {@link Fsm} instance being built.
   *
   * @param actionProxy an {@link ActionProxy} for the {@link Fsm} instance being built.
   */
  public void setActionProxy(ActionProxy<S, E> actionProxy) {
    this.actionProxy = actionProxy;
  }

  public Fsm<S, E> build(S initialState) {
    return new StrictMachine<>(
        loggerName,
        mdc,
        executor,
        userContext,
        actionProxy,
        initialState,
        new ArrayList<>(transitions),
        new ArrayList<>(transitionActions)
    );
  }

}

```

---

### `strict-machine/src/main/java/com/digitalpetri/fsm/FsmContext.java`

```java
/*
 * Copyright (c) 2024 Kevin Herron
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.digitalpetri.fsm;

import java.util.Objects;
import java.util.StringJoiner;

public interface FsmContext<S, E> {

  /**
   * Get the current state of the FSM.
   *
   * @return the current state of the FSM.
   */
  S currentState();

  /**
   * Fire an event to be evaluated against the current state of the {@link Fsm}.
   *
   * @param event the event to be evaluated.
   */
  void fireEvent(E event);

  /**
   * Shelve an event to be evaluated at some later time.
   *
   * <p>This is useful e.g. when an event can't be handled in the current state but shouldn't be
   * discarded or ignored with an action-less internal transition.
   *
   * @param event the event to be queued.
   * @see #processShelvedEvents()
   */
  void shelveEvent(E event);

  /**
   * Drain the event shelf of any queued events and fire them for evaluation.
   */
  void processShelvedEvents();

  /**
   * Get the value identified by {@code key} from the context, or {@code null} if it does not
   * exist.
   *
   * @param key the {@link Key}.
   * @return the value identified by {@code key}, or {@code null} if it does not exist.
   */
  Object get(FsmContext.Key<?> key);

  /**
   * Get and remove the value identified by {@code key} from the context, or {@code null} if it does
   * not exist.
   *
   * @param key the {@link Key}.
   * @return the value identified by {@code key}, or {@code null} if it did not exist.
   */
  Object remove(FsmContext.Key<?> key);

  /**
   * Set a value identified by {@code key} on the context.
   *
   * @param key the {@link Key}.
   * @param value the value.
   */
  void set(FsmContext.Key<?> key, Object value);

  /**
   * Get the user-configurable context associated with this FSM instance.
   *
   * @return the user-configurable context associated with this FSM instance.
   */
  Object getUserContext();

  final class Key<T> {

    private final String name;
    private final Class<T> type;

    public Key(String name, Class<T> type) {
      this.name = name;
      this.type = type;
    }

    public String name() {
      return name;
    }

    public Class<T> type() {
      return type;
    }

    public T get(FsmContext<?, ?> context) {
      Object value = context.get(this);

      return value != null ? type.cast(value) : null;
    }

    public T remove(FsmContext<?, ?> context) {
      Object value = context.remove(this);

      return value != null ? type.cast(value) : null;
    }

    public void set(FsmContext<?, ?> context, T value) {
      context.set(this, value);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Key<?> key = (Key<?>) o;
      return Objects.equals(name, key.name)
          && Objects.equals(type, key.type);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, type);
    }

    @Override
    public String toString() {
      return new StringJoiner(", ", Key.class.getSimpleName() + "[", "]")
          .add("name='" + name + "'")
          .add("type=" + type)
          .toString();
    }

  }

}

```

---

### `strict-machine/src/main/java/com/digitalpetri/fsm/Fsm.java`

```java
/*
 * Copyright (c) 2024 Kevin Herron
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.digitalpetri.fsm;

import java.util.function.Consumer;
import java.util.function.Function;

public interface Fsm<S, E> {

  /**
   * Get the current state of the FSM.
   *
   * @return the current state of the FSM.
   */
  S getState();

  /**
   * Fire an event for the FSM to evaluate.
   *
   * <p>The subsequent state transition may occur asynchronously. There is no guarantee that a
   * subsequent call to {@link #getState()} reflects a state arrived at via evaluation of this
   * event.
   *
   * @param event the event to evaluate.
   * @see #fireEvent(Object, Consumer)
   */
  void fireEvent(E event);

  /**
   * Fire an event for the FSM to evaluate, providing a callback that will be invoked when the event
   * is evaluated.
   *
   * <p>This callback may occur asynchronously.
   *
   * @param event the event to evaluate.
   * @param stateConsumer the callback that will receive the state transitioned to via
   *     evaluation of {@code event}.
   */
  void fireEvent(E event, Consumer<S> stateConsumer);

  /**
   * Fire an event for the FSM to evaluate and block waiting until the state transitioned to as a
   * result of evaluating {@code event} is available.
   *
   * @param event the event to evaluate.
   * @return the state transitioned to as a result of evaluating {@code event}.
   * @throws InterruptedException if interrupted while blocking.
   */
  S fireEventBlocking(E event) throws InterruptedException;

  /**
   * Provides safe access to the {@link FsmContext} in order to retrieve a value from it.
   *
   * @param get the Function provided access to the context.
   * @param <T> the type of the value being retrieved.
   * @return a value from {@code context}.
   */
  <T> T getFromContext(Function<FsmContext<S, E>, T> get);

  /**
   * Provides safe access to the {@link FsmContext}.
   *
   * @param contextConsumer the callback that will receive the {@link FsmContext}.
   */
  void withContext(Consumer<FsmContext<S, E>> contextConsumer);

}

```

---

### `strict-machine/src/test/java/com/digitalpetri/fsm/dsl/ActionFromBuilderTest.java`

```java
/*
 * Copyright (c) 2024 Kevin Herron
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.digitalpetri.fsm.dsl;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class ActionFromBuilderTest {

  @Test
  void actionFromBuilder_toInstance() throws InterruptedException {
    assertActionExecuted(afb -> afb.to(State.S2));
  }

  @Test
  void actionFromBuilder_toPredicate() throws InterruptedException {
    assertActionExecuted(afb -> afb.to(s -> s == State.S2));
  }

  @Test
  void actionFromBuilder_toAny() throws InterruptedException {
    assertActionExecuted(ActionFromBuilder::toAny);
  }

  private void assertActionExecuted(
      Function<ActionFromBuilder<State, Event>, ViaBuilder<State, Event>> f
  ) throws InterruptedException {
    var fb = new FsmBuilder<State, Event>();

    fb.when(State.S1)
        .on(Event.E1.class)
        .transitionTo(State.S2);

    var executed = new AtomicBoolean(false);

    ActionFromBuilder<State, Event> afb = fb.onTransitionFrom(State.S1);
    ViaBuilder<State, Event> viaBuilder = f.apply(afb);

    viaBuilder.via(Event.E1.class)
        .execute(ctx -> executed.set(true));

    fb.build(State.S1).fireEventBlocking(new Event.E1());

    assertTrue(executed.get());
  }

}

```

---

### `strict-machine/src/test/java/com/digitalpetri/fsm/dsl/State.java`

```java
/*
 * Copyright (c) 2024 Kevin Herron
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.digitalpetri.fsm.dsl;

enum State {
  S1, S2, S3, S4
}

```

---

### `strict-machine/src/test/java/com/digitalpetri/fsm/dsl/ViaBuilderTest.java`

```java
/*
 * Copyright (c) 2024 Kevin Herron
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.digitalpetri.fsm.dsl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

public class ViaBuilderTest {

  @Test
  void actionBuiltWithViaInstance() throws InterruptedException {
    var fb = new FsmBuilder<State, Event>();

    fb.when(State.S1)
        .on(Event.E1.INSTANCE)
        .transitionTo(State.S2);

    var executed = new AtomicBoolean(false);

    fb.onTransitionFrom(State.S1)
        .to(State.S2)
        .via(Event.E1.INSTANCE)
        .execute(ctx -> executed.set(true));

    assertEquals(State.S2, fb.build(State.S1).fireEventBlocking(Event.E1.INSTANCE));

    assertTrue(executed.get());
  }

  @Test
  void actionBuiltWithViaClass() throws InterruptedException {
    var fb = new FsmBuilder<State, Event>();

    fb.when(State.S1)
        .on(Event.E1.INSTANCE)
        .transitionTo(State.S2);

    var executed = new AtomicBoolean(false);

    fb.onTransitionFrom(State.S1)
        .to(State.S2)
        .via(Event.E1.class)
        .execute(ctx -> executed.set(true));

    assertEquals(State.S2, fb.build(State.S1).fireEventBlocking(Event.E1.INSTANCE));

    assertTrue(executed.get());
  }

  @Test
  void actionBuiltWithViaPredicate() throws InterruptedException {
    var fb = new FsmBuilder<State, Event>();

    fb.when(State.S1)
        .on(Event.E1.INSTANCE)
        .transitionTo(State.S2);

    var executed = new AtomicBoolean(false);

    fb.onTransitionFrom(State.S1)
        .to(State.S2)
        .via(e -> e instanceof Event.E1)
        .execute(ctx -> executed.set(true));

    assertEquals(State.S2, fb.build(State.S1).fireEventBlocking(Event.E1.INSTANCE));

    assertTrue(executed.get());
  }

  @Test
  void actionBuiltWithViaAny() throws InterruptedException {
    var fb = new FsmBuilder<State, Event>();

    fb.when(State.S1)
        .onAny()
        .transitionTo(State.S2);

    var executed = new AtomicBoolean(false);

    fb.onTransitionFrom(State.S1)
        .to(State.S2)
        .viaAny()
        .execute(ctx -> executed.set(true));

    for (Event event : List.of(new Event.E1(), new Event.E2(), new Event.E3())) {
      executed.set(false);
      assertEquals(State.S2, fb.build(State.S1).fireEventBlocking(event));
      assertTrue(executed.get());
    }
  }
  
}

```

---

### `strict-machine/src/test/java/com/digitalpetri/fsm/dsl/TransitionBuilderTest.java`

```java
/*
 * Copyright (c) 2024 Kevin Herron
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.digitalpetri.fsm.dsl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class TransitionBuilderTest {

  @Test
  void transitionFromEventInstance() throws InterruptedException {
    var fb = new FsmBuilder<State, Event>();

    fb.when(State.S1)
        .on(Event.E1.INSTANCE)
        .transitionTo(State.S2);

    assertEquals(State.S2, fb.build(State.S1).fireEventBlocking(Event.E1.INSTANCE));

    // internal transitions
    assertEquals(State.S1, fb.build(State.S1).fireEventBlocking(Event.E2.INSTANCE));
    assertEquals(State.S1, fb.build(State.S1).fireEventBlocking(Event.E3.INSTANCE));
  }

  @Test
  void transitionFromEventClass() throws InterruptedException {
    var fb = new FsmBuilder<State, Event>();

    fb.when(State.S1)
        .on(Event.E1.class)
        .transitionTo(State.S2);

    assertEquals(State.S2, fb.build(State.S1).fireEventBlocking(new Event.E1()));

    // internal transitions
    assertEquals(State.S1, fb.build(State.S1).fireEventBlocking(new Event.E2()));
    assertEquals(State.S1, fb.build(State.S1).fireEventBlocking(new Event.E3()));
  }

  @Test
  void transitionFromPredicate() throws InterruptedException {
    var fb = new FsmBuilder<State, Event>();

    fb.when(State.S1)
        .on(e -> e instanceof Event.E1 || e instanceof Event.E2)
        .transitionTo(State.S2);

    assertEquals(State.S2, fb.build(State.S1).fireEventBlocking(new Event.E1()));
    assertEquals(State.S2, fb.build(State.S1).fireEventBlocking(new Event.E2()));

    // internal transitions
    assertEquals(State.S1, fb.build(State.S1).fireEventBlocking(new Event.E3()));
  }

  @Test
  void transitionFromOnAny() throws InterruptedException {
    var fb = new FsmBuilder<State, Event>();

    fb.when(State.S1)
        .onAny()
        .transitionTo(State.S2);

    assertEquals(State.S2, fb.build(State.S1).fireEventBlocking(new Event.E1()));
    assertEquals(State.S2, fb.build(State.S1).fireEventBlocking(new Event.E2()));
    assertEquals(State.S2, fb.build(State.S1).fireEventBlocking(new Event.E3()));
    assertEquals(State.S2, fb.build(State.S1).fireEventBlocking(new Event.E4()));
  }

  @Test
  void firstOfMultipleWins() throws InterruptedException {
    var fb = new FsmBuilder<State, Event>();

    fb.when(State.S1)
        .on(Event.E1.class)
        .transitionTo(State.S3);

    // first definition "wins", this should not result in S2
    fb.when(State.S1)
        .on(Event.E1.class)
        .transitionTo(State.S2);

    assertEquals(State.S3, fb.build(State.S1).fireEventBlocking(new Event.E1()));
  }

}

```

---

### `strict-machine/src/test/java/com/digitalpetri/fsm/dsl/ActionProxyTest.java`

```java
/*
 * Copyright (c) 2024 Kevin Herron
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.digitalpetri.fsm.dsl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class ActionProxyTest {

  @Test
  void actionProxyGetsCalled() throws InterruptedException {
    var fb = new FsmBuilder<State, Event>();

    var state3Latch = new CountDownLatch(1);

    fb.when(State.S1)
        .on(Event.E1.class)
        .transitionTo(State.S2)
        .execute(ctx -> {
          ctx.fireEvent(new Event.E2());
          ctx.fireEvent(new Event.E3());
        });

    fb.when(State.S2)
        .on(Event.E2.class)
        .transitionTo(State.S3);

    fb.when(State.S3)
        .on(Event.E3.class)
        .transitionTo(State.S1)
        .execute(ctx -> state3Latch.countDown());

    var actionProxyLatch = new CountDownLatch(2);

    fb.setActionProxy((ctx, action) -> {
      actionProxyLatch.countDown();
      action.execute(ctx);
    });

    var fsm = fb.build(State.S1);

    fsm.fireEvent(new Event.E1());

    assertTrue(state3Latch.await(5, TimeUnit.SECONDS));
    assertTrue(actionProxyLatch.await(5, TimeUnit.SECONDS));
    assertEquals(State.S1, fsm.getState());
  }

}

```

---

### `strict-machine/src/test/java/com/digitalpetri/fsm/dsl/GuardBuilderTest.java`

```java
/*
 * Copyright (c) 2024 Kevin Herron
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.digitalpetri.fsm.dsl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.digitalpetri.fsm.FsmContext;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

class GuardBuilderTest {

  @Test
  void guardBuilder() {
    var transition = new PredicatedTransition<State, Event>(
        s -> s == State.S1,
        e -> e instanceof Event.E1,
        State.S2
    );

    var actions = new LinkedList<TransitionAction<State, Event>>();
    var guardBuilder = new GuardBuilder<>(transition, actions);
    Predicate<FsmContext<State, Event>> guard = ctx -> true;

    guardBuilder.guardedBy(guard);

    assertEquals(guard, transition.getGuard());
  }

  @Test
  void guardedTransition() throws InterruptedException {
    var fb = new FsmBuilder<State, Event>();

    var guardCondition = new AtomicBoolean(false);

    fb.when(State.S1)
        .on(Event.E1.class)
        .transitionTo(State.S2)
        .guardedBy(ctx -> guardCondition.get());

    var fsm = fb.build(State.S1);

    assertEquals(State.S1, fsm.fireEventBlocking(new Event.E1()));
    guardCondition.set(true);
    assertEquals(State.S2, fsm.fireEventBlocking(new Event.E1()));
  }

}

```

---

### `strict-machine/src/test/java/com/digitalpetri/fsm/dsl/Event.java`

```java
/*
 * Copyright (c) 2024 Kevin Herron
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.digitalpetri.fsm.dsl;

abstract class Event {

  static class E1 extends Event {

    static E1 INSTANCE = new E1();

    @Override
    public String toString() {
      return "E1";
    }
  }

  static class E2 extends Event {

    static E2 INSTANCE = new E2();

    @Override
    public String toString() {
      return "E2";
    }
  }

  static class E3 extends Event {

    static E3 INSTANCE = new E3();

    @Override
    public String toString() {
      return "E3";
    }
  }

  static class E4 extends Event {

    static E4 INSTANCE = new E4();

    @Override
    public String toString() {
      return "E4";
    }
  }

}

```

---

### `strict-machine/src/test/java/com/digitalpetri/fsm/dsl/atm/AtmFsm.java`

```java
/*
 * Copyright (c) 2024 Kevin Herron
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.digitalpetri.fsm.dsl.atm;

import com.digitalpetri.fsm.Fsm;
import com.digitalpetri.fsm.dsl.FsmBuilder;
import java.util.function.Consumer;
import java.util.function.Function;

public class AtmFsm {

  private final Fsm<State, Event> fsm;

  private AtmFsm(Fsm<State, Event> fsm) {
    this.fsm = fsm;
  }

  void fireEvent(Event event, Consumer<State> stateConsumer) {
    fsm.fireEvent(event, stateConsumer);
  }

  State fireEventBlocking(Event event) throws InterruptedException {
    return fsm.fireEventBlocking(event);
  }

  enum State {
    Idle,
    Loading,
    OutOfService,
    InService,
    Disconnected
  }

  enum Event {
    Connected,
    ConnectionClosed,
    ConnectionLost,
    ConnectionRestored,
    LoadFail,
    LoadSuccess,
    Shutdown,
    Startup
  }

  /**
   * Create a new {@link AtmFsm} in {@link State#Idle}.
   *
   * @return a new {@link AtmFsm} in {@link State#Idle}.
   */
  public static AtmFsm newAtmFsm() {
    return buildFsm(fb -> State.Idle);
  }

  /**
   * Build an {@link AtmFsm}.
   *
   * <p>{@code builderStateFunction} may make modifications to the FSM before it's built via the
   * builder and returns the desired initial state.
   *
   * @param builderStateFunction invoked after the builder has set up all state transitions.
   *     Returns the desired initial state of the FSM.
   * @return an {@link AtmFsm}.
   */
  static AtmFsm buildFsm(Function<FsmBuilder<State, Event>, State> builderStateFunction) {
    FsmBuilder<State, Event> fb = new FsmBuilder<>();

    /* Idle */
    fb.when(State.Idle)
        .on(Event.Connected)
        .transitionTo(State.Loading);

    /* Loading */
    fb.when(State.Loading)
        .on(Event.LoadSuccess)
        .transitionTo(State.InService);

    fb.when(State.Loading)
        .on(Event.LoadFail)
        .transitionTo(State.OutOfService);

    fb.when(State.Loading)
        .on(Event.ConnectionClosed)
        .transitionTo(State.Disconnected);

    /* OutOfService */
    fb.when(State.OutOfService)
        .on(Event.Startup)
        .transitionTo(State.InService);

    fb.when(State.OutOfService)
        .on(Event.ConnectionLost)
        .transitionTo(State.Disconnected);

    /* InService */
    fb.when(State.InService)
        .on(Event.ConnectionLost)
        .transitionTo(State.Disconnected);

    fb.when(State.InService)
        .on(Event.Shutdown)
        .transitionTo(State.OutOfService);

    /* Disconnected */
    fb.when(State.Disconnected)
        .on(Event.ConnectionRestored)
        .transitionTo(State.InService);

    State initialState = builderStateFunction.apply(fb);

    return new AtmFsm(fb.build(initialState));
  }

}

```

---

### `strict-machine/src/test/java/com/digitalpetri/fsm/dsl/atm/AtmTest.java`

```java
/*
 * Copyright (c) 2024 Kevin Herron
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.digitalpetri.fsm.dsl.atm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.digitalpetri.fsm.dsl.atm.AtmFsm.Event;
import com.digitalpetri.fsm.dsl.atm.AtmFsm.State;
import org.junit.jupiter.api.Test;

@SuppressWarnings("MethodName")
class AtmTest {

  @Test
  void Idle_Connected_Loading() throws InterruptedException {
    var fsm = AtmFsm.buildFsm(fb -> State.Idle);

    assertEquals(State.Loading, fsm.fireEventBlocking(Event.Connected));
  }

  @Test
  void Loading_LoadSuccess_InService() throws InterruptedException {
    var fsm = AtmFsm.buildFsm(fb -> State.Loading);

    assertEquals(State.InService, fsm.fireEventBlocking(Event.LoadSuccess));
  }

  @Test
  void Loading_LoadFail_OutOfService() throws InterruptedException {
    var fsm = AtmFsm.buildFsm(fb -> State.Loading);

    assertEquals(State.OutOfService, fsm.fireEventBlocking(Event.LoadFail));
  }

  @Test
  void Loading_ConnectionClosed_Disconnected() throws InterruptedException {
    var fsm = AtmFsm.buildFsm(fb -> State.Loading);

    assertEquals(State.Disconnected, fsm.fireEventBlocking(Event.ConnectionClosed));
  }

  @Test
  void OutOfService_Startup_InService() throws InterruptedException {
    var fsm = AtmFsm.buildFsm(fb -> State.OutOfService);

    assertEquals(State.InService, fsm.fireEventBlocking(Event.Startup));
  }

  @Test
  void OutOfService_ConnectionLost_Disconnected() throws InterruptedException {
    var fsm = AtmFsm.buildFsm(fb -> State.OutOfService);

    assertEquals(State.Disconnected, fsm.fireEventBlocking(Event.ConnectionLost));
  }

  @Test
  void InService_ConnectionLost_Disconnected() throws InterruptedException {
    var fsm = AtmFsm.buildFsm(fb -> State.InService);

    assertEquals(State.Disconnected, fsm.fireEventBlocking(Event.ConnectionLost));
  }

  @Test
  void InService_Shutdown_OutOfService() throws InterruptedException {
    var fsm = AtmFsm.buildFsm(fb -> State.InService);

    assertEquals(State.OutOfService, fsm.fireEventBlocking(Event.Shutdown));
  }

  @Test
  void Disconnected_ConnectionRestored_InService() throws InterruptedException {
    var fsm = AtmFsm.buildFsm(fb -> State.Disconnected);

    assertEquals(State.InService, fsm.fireEventBlocking(Event.ConnectionRestored));
  }

}

```

---

### `strict-machine/src/test/java/com/digitalpetri/fsm/dsl/ActionBuilderTest.java`

```java
/*
 * Copyright (c) 2024 Kevin Herron
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.digitalpetri.fsm.dsl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;

class ActionBuilderTest {

  @Test
  void actionsAreExecutedInOrder() throws InterruptedException {
    var fb = new FsmBuilder<State, Event>();

    var executed = new ArrayList<Integer>();

    fb.when(State.S1)
        .on(Event.E1.class)
        .transitionTo(State.S2)
        .execute(ctx -> executed.add(0))
        .executeLast(ctx -> executed.add(1))
        .executeFirst(ctx -> executed.add(2));

    fb.build(State.S1).fireEventBlocking(new Event.E1());

    assertEquals(2, (int) executed.get(0));
    assertEquals(0, (int) executed.get(1));
    assertEquals(1, (int) executed.get(2));
  }

}

```

---

### `strict-machine/src/test/java/com/digitalpetri/fsm/dsl/ActionToBuilderTest.java`

```java
/*
 * Copyright (c) 2024 Kevin Herron
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.digitalpetri.fsm.dsl;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

@SuppressWarnings("MethodName")
class ActionToBuilderTest {

  @Test
  void actionToBuilder_fromInstance() throws InterruptedException {
    assertActionExecuted(atb -> atb.from(State.S1));
  }

  @Test
  void actionToBuilder_fromPredicate() throws InterruptedException {
    assertActionExecuted(atb -> atb.from(s -> s == State.S1));
  }

  @Test
  void actionToBuilder_fromAny() throws InterruptedException {
    assertActionExecuted(ActionToBuilder::fromAny);
  }

  private void assertActionExecuted(
      Function<ActionToBuilder<State, Event>, ViaBuilder<State, Event>> f
  ) throws InterruptedException {
    var fb = new FsmBuilder<State, Event>();

    fb.when(State.S1)
        .on(Event.E1.class)
        .transitionTo(State.S2);

    var executed = new AtomicBoolean(false);

    ActionToBuilder<State, Event> atb = fb.onTransitionTo(State.S2);
    ViaBuilder<State, Event> viaBuilder = f.apply(atb);

    viaBuilder.via(Event.E1.class)
        .execute(ctx -> executed.set(true));

    fb.build(State.S1).fireEventBlocking(new Event.E1());

    assertTrue(executed.get());
  }

}

```

---

### `strict-machine/src/test/java/com/digitalpetri/fsm/dsl/StrictMachineTest.java`

```java
/*
 * Copyright (c) 2024 Kevin Herron
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.digitalpetri.fsm.dsl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.digitalpetri.fsm.FsmContext;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class StrictMachineTest {

  @Test
  void eventsFiredInExecuteCallbacks() throws InterruptedException {
    var fb = new FsmBuilder<State, Event>();

    var latch = new CountDownLatch(1);

    fb.when(State.S1)
        .on(Event.E1.class)
        .transitionTo(State.S2)
        .execute(ctx -> {
          ctx.fireEvent(new Event.E2());
          ctx.fireEvent(new Event.E3());
        });

    fb.when(State.S2)
        .on(Event.E2.class)
        .transitionTo(State.S3);

    fb.when(State.S3)
        .on(Event.E3.class)
        .transitionTo(State.S1)
        .execute(ctx -> latch.countDown());

    var fsm = fb.build(State.S1);
    fsm.fireEvent(new Event.E1());

    assertTrue(latch.await(5, TimeUnit.SECONDS));
    assertEquals(State.S1, fsm.getState());
  }

  @Test
  void eventShelving() throws InterruptedException {
    var fb = new FsmBuilder<State, Event>();

    fb.when(State.S1)
        .on(Event.E1.class)
        .transitionTo(State.S2);

    fb.onInternalTransition(State.S1)
        .via(Event.E2.class)
        .execute(ctx -> ctx.shelveEvent(ctx.event()));

    fb.onTransitionFrom(State.S1)
        .to(s -> s != State.S1)
        .viaAny()
        .execute(FsmContext::processShelvedEvents);

    fb.when(State.S2)
        .on(Event.E2.class)
        .transitionTo(State.S3);

    fb.when(State.S3)
        .on(Event.E3.class)
        .transitionTo(State.S4);

    var fsm = fb.build(State.S1);

    // fire an E2 that gets shelved
    fsm.fireEventBlocking(new Event.E2());

    // fire E1 to trigger S1 -> S2
    fsm.fireEventBlocking(new Event.E1());

    // fsm should have processed event shelf and landed in S3.
    // now move to S4 via E3 and check the result.
    assertEquals(State.S4, fsm.fireEventBlocking(new Event.E3()));
  }

}

```

---

### `netty-channel-fsm/src/main/java/com/digitalpetri/netty/fsm/ChannelActions.java`

```java
/*
 * Copyright (c) 2024 Kevin Herron
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.digitalpetri.netty.fsm;

import com.digitalpetri.fsm.FsmContext;
import io.netty.channel.Channel;

import java.util.concurrent.CompletableFuture;

public interface ChannelActions {

  /**
   * Bootstrap a new {@link Channel} and return a {@link CompletableFuture} that completes
   * successfully when the Channel is ready to use or completes exceptionally if the Channel could
   * not be created or made ready to use for any reason.
   *
   * @param ctx the {@link FsmContext}.
   * @return a {@link CompletableFuture} that completes successfully when the Channel is ready to
   *     use or completes exceptionally if the Channel could not be created or made ready to use for
   *     any reason.
   */
  CompletableFuture<Channel> connect(FsmContext<State, Event> ctx);

  /**
   * Perform any disconnect actions and then close {@code channel}, returning a
   * {@link CompletableFuture} that completes successfully when the Channel has disconnected or
   * completes exceptionally if the channel could not be disconnected for any reason.
   *
   * <p>The state machine advances the same way regardless of how the future is completed.
   *
   * @param ctx the {@link FsmContext}.
   * @param channel the {@link Channel} to disconnect.
   * @return a {@link CompletableFuture} that completes successfully when the Channel * has
   *     disconnected or completes exceptionally if the channel could not be disconnected for any
   *     reason.
   */
  CompletableFuture<Void> disconnect(FsmContext<State, Event> ctx, Channel channel);

  /**
   * Perform a keep-alive action because the Channel has been idle for longer than
   * {@code maxIdleSeconds}.
   *
   * <p>Although the keep-alive action is implementation dependent the intended usage would be to
   * do something send a request that tests the Channel to make sure it's still valid.
   *
   * @param ctx the {@link FsmContext}
   * @param channel the {@link Channel} to send the keep-alive on.
   * @return a {@link CompletableFuture} that completes successfully if the channel is still valid
   *     and completes exceptionally otherwise.
   */
  default CompletableFuture<Void> keepAlive(FsmContext<State, Event> ctx, Channel channel) {
    return CompletableFuture.completedFuture(null);
  }

}

```

---

### `netty-channel-fsm/src/main/java/com/digitalpetri/netty/fsm/CompletionBuilders.java`

```java
/*
 * Copyright (c) 2024 Kevin Herron
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.digitalpetri.netty.fsm;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

class CompletionBuilders {

  private CompletionBuilders() {}

  /**
   * Complete {@code future} with the result of the {@link CompletableFuture} that is provided to
   * the returned {@link CompletionBuilder}.
   *
   * @param future the future to complete.
   * @param <T> the type returned by {@code future}.
   * @return a {@link CompletionBuilder}.
   */
  public static <T> CompletionBuilder<T> complete(CompletableFuture<T> future) {
    return new CompletionBuilder<>(future);
  }

  /**
   * Complete {@code future} asynchronously with the result of the {@link CompletableFuture} that is
   * provided to the returned {@link CompletionBuilder}.
   *
   * @param future the future to complete.
   * @param executor the {@link Executor} to use.
   * @param <T> the type returned by {@code future}.
   * @return a {@link CompletionBuilder}.
   */
  public static <T> CompletionBuilder<T> completeAsync(CompletableFuture<T> future,
      Executor executor) {
    return new AsyncCompletionBuilder<>(future, executor);
  }

  public static class CompletionBuilder<T> {

    final CompletableFuture<T> toComplete;

    private CompletionBuilder(CompletableFuture<T> toComplete) {
      this.toComplete = toComplete;
    }

    /**
     * Turn this {@link CompletionBuilder} into an {@link AsyncCompletionBuilder}.
     *
     * @param executor the {@link Executor} to use for the async completion.
     * @return an {@link AsyncCompletionBuilder}.
     */
    public CompletionBuilder<T> async(Executor executor) {
      return new AsyncCompletionBuilder<>(toComplete, executor);
    }

    /**
     * Complete the contained to-be-completed {@link CompletableFuture} using the result of
     * {@code future}.
     *
     * @param future the {@link CompletableFuture} to use as the result for the contained
     *     future.
     * @return the original, to-be-completed future provided to this {@link CompletionBuilder}.
     */
    public CompletableFuture<T> with(CompletableFuture<T> future) {
      future.whenComplete((v, ex) -> {
        if (ex != null) {
          toComplete.completeExceptionally(ex);
        } else {
          toComplete.complete(v);
        }
      });

      return toComplete;
    }

  }

  private static final class AsyncCompletionBuilder<T> extends CompletionBuilder<T> {

    private final Executor executor;

    AsyncCompletionBuilder(CompletableFuture<T> toComplete, Executor executor) {
      super(toComplete);

      this.executor = executor;
    }

    @Override
    public CompletableFuture<T> with(CompletableFuture<T> future) {
      future.whenCompleteAsync((v, ex) -> {
        if (ex != null) {
          toComplete.completeExceptionally(ex);
        } else {
          toComplete.complete(v);
        }
      }, executor);

      return toComplete;
    }

  }

}

```

---

### `netty-channel-fsm/src/main/java/com/digitalpetri/netty/fsm/ChannelFsmConfigBuilder.java`

```java
/*
 * Copyright (c) 2024 Kevin Herron
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.digitalpetri.netty.fsm;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.*;

public class ChannelFsmConfigBuilder {

  static int DEFAULT_MAX_RECONNECT_DELAY_SECONDS = 32;

  private boolean lazy = false;
  private boolean persistent = true;
  private int maxIdleSeconds = 15;
  private int maxReconnectDelaySeconds = DEFAULT_MAX_RECONNECT_DELAY_SECONDS;
  private ChannelActions channelActions;
  private Executor executor;
  private Scheduler scheduler;
  private String loggerName;
  private Map<String, String> loggingContext = Collections.emptyMap();
  private Object userContext;

  /**
   * @param lazy {@code true} if the ChannelFsm should be lazy,
   * @return this {@link ChannelFsmConfigBuilder}.
   * @see ChannelFsmConfig#isLazy()
   */
  public ChannelFsmConfigBuilder setLazy(boolean lazy) {
    this.lazy = lazy;
    return this;
  }

  /**
   * @param persistent {@code true} if the ChannelFsm should be persistent in its initial
   *     connect attempt.
   * @return this {@link ChannelFsmConfigBuilder}.
   * @see ChannelFsmConfig#isPersistent()
   */
  public ChannelFsmConfigBuilder setPersistent(boolean persistent) {
    this.persistent = persistent;
    return this;
  }

  /**
   * @param maxIdleSeconds the maximum amount of time, in seconds, before a keep alive occurs on
   *     an idle channel.
   * @return this {@link ChannelFsmConfigBuilder}.
   * @see ChannelFsmConfig#getMaxIdleSeconds()
   */
  public ChannelFsmConfigBuilder setMaxIdleSeconds(int maxIdleSeconds) {
    this.maxIdleSeconds = maxIdleSeconds;
    return this;
  }

  /**
   * @param maxReconnectDelaySeconds the maximum delay to occur between reconnect attempts.
   * @return this {@link ChannelFsmConfigBuilder}.
   * @see ChannelFsmConfig#getMaxReconnectDelaySeconds()
   */
  public ChannelFsmConfigBuilder setMaxReconnectDelaySeconds(int maxReconnectDelaySeconds) {
    this.maxReconnectDelaySeconds = maxReconnectDelaySeconds;
    return this;
  }

  /**
   * @param channelActions the {@link ChannelActions} delegate.
   * @return this {@link ChannelFsmConfigBuilder}.
   * @see ChannelFsmConfig#getChannelActions()
   */
  public ChannelFsmConfigBuilder setChannelActions(ChannelActions channelActions) {
    this.channelActions = channelActions;
    return this;
  }

  /**
   * @param executor the {@link Executor} to use.
   * @return this {@link ChannelFsmConfigBuilder}.
   * @see ChannelFsmConfig#getExecutor()
   */
  public ChannelFsmConfigBuilder setExecutor(Executor executor) {
    this.executor = executor;
    return this;
  }

  /**
   * @param scheduler the {@link Scheduler} to use.
   * @return this {@link ChannelFsmConfigBuilder}.
   * @see ChannelFsmConfig#getScheduler()
   */
  public ChannelFsmConfigBuilder setScheduler(Scheduler scheduler) {
    this.scheduler = scheduler;
    return this;
  }

  /**
   * @param scheduledExecutor the {@link ScheduledExecutorService} to use.
   * @return this {@link ChannelFsmConfigBuilder}.
   * @see ChannelFsmConfig#getScheduler()
   */
  public ChannelFsmConfigBuilder setScheduler(ScheduledExecutorService scheduledExecutor) {
    this.scheduler = Scheduler.fromScheduledExecutor(scheduledExecutor);
    return this;
  }

  /**
   * @param loggerName the logger name the FSM should use.
   * @return this {@link ChannelFsmConfigBuilder}.
   * @see ChannelFsmConfig#getLoggerName()
   */
  public ChannelFsmConfigBuilder setLoggerName(String loggerName) {
    this.loggerName = loggerName;
    return this;
  }

  /**
   * Set the logging context Map a {@link ChannelFsm} instance will use.
   *
   * <p>Keys and values in the Map will be set on the SLF4J {@link org.slf4j.MDC} when logging.
   *
   * <p>This method makes a defensive copy of {@code loggingContext}.
   *
   * @param loggingContext the logging context Map a {@link ChannelFsm} instance will use.
   * @return this {@link ChannelFsmConfigBuilder}
   * @see ChannelFsmConfig#getLoggingContext()
   */
  public ChannelFsmConfigBuilder setLoggingContext(Map<String, String> loggingContext) {
    this.loggingContext = new ConcurrentHashMap<>(loggingContext);
    return this;
  }

  /**
   * @param userContext the user-configurable context associated with this ChannelFsm.
   * @return this {@link ChannelFsmConfigBuilder}.
   * @see ChannelFsmConfig#getUserContext()
   */
  public ChannelFsmConfigBuilder setUserContext(Object userContext) {
    this.userContext = userContext;
    return this;
  }

  public ChannelFsmConfig build() {
    if (channelActions == null) {
      throw new IllegalArgumentException("channelActions must be non-null");
    }
    if (maxReconnectDelaySeconds < 1) {
      maxReconnectDelaySeconds = DEFAULT_MAX_RECONNECT_DELAY_SECONDS;
    }
    if (executor == null) {
      executor = SharedExecutor.INSTANCE;
    }
    if (scheduler == null) {
      scheduler = SharedScheduler.INSTANCE;
    }
    if (loggerName == null) {
      loggerName = ChannelFsm.class.getName();
    }

    return new ChannelFsmConfigImpl(
        lazy,
        persistent,
        maxIdleSeconds,
        maxReconnectDelaySeconds,
        channelActions,
        executor,
        scheduler,
        loggerName,
        loggingContext,
        userContext
    );
  }

  private static class SharedExecutor {

    private static final ExecutorService INSTANCE =
        Executors.newSingleThreadExecutor(
            r -> {
              Thread t = Executors.defaultThreadFactory().newThread(r);
              t.setName("channel-fsm-shared-executor");
              t.setDaemon(true);
              return t;
            });
  }

  private static class SharedScheduler {

    private static final Scheduler INSTANCE =
        Scheduler.fromScheduledExecutor(
            Executors.newSingleThreadScheduledExecutor(
                r -> {
                  Thread t = Executors.defaultThreadFactory().newThread(r);
                  t.setName("channel-fsm-shared-scheduler");
                  t.setDaemon(true);
                  return t;
                }));
  }

  private static class ChannelFsmConfigImpl implements ChannelFsmConfig {

    private final boolean lazy;
    private final boolean persistent;
    private final int maxIdleSeconds;
    private final int maxReconnectDelaySeconds;
    private final ChannelActions channelActions;
    private final Executor executor;
    private final Scheduler scheduler;
    private final String loggerName;
    private final Map<String, String> loggingContext;
    private final Object userContext;

    ChannelFsmConfigImpl(
        boolean lazy,
        boolean persistent,
        int maxIdleSeconds,
        int maxReconnectDelaySeconds,
        ChannelActions channelActions,
        Executor executor,
        Scheduler scheduler,
        String loggerName,
        Map<String, String> loggingContext,
        Object userContext
    ) {

      this.lazy = lazy;
      this.persistent = persistent;
      this.maxIdleSeconds = maxIdleSeconds;
      this.maxReconnectDelaySeconds = maxReconnectDelaySeconds;
      this.channelActions = channelActions;
      this.executor = executor;
      this.scheduler = scheduler;
      this.loggerName = loggerName;
      this.loggingContext = loggingContext;
      this.userContext = userContext;
    }

    @Override
    public boolean isLazy() {
      return lazy;
    }

    @Override
    public boolean isPersistent() {
      return persistent;
    }

    @Override
    public int getMaxIdleSeconds() {
      return maxIdleSeconds;
    }

    @Override
    public int getMaxReconnectDelaySeconds() {
      return maxReconnectDelaySeconds;
    }

    @Override
    public ChannelActions getChannelActions() {
      return channelActions;
    }

    @Override
    public Executor getExecutor() {
      return executor;
    }

    @Override
    public Scheduler getScheduler() {
      return scheduler;
    }

    @Override
    public String getLoggerName() {
      return loggerName;
    }

    @Override
    public Map<String, String> getLoggingContext() {
      return loggingContext;
    }

    @Override
    public Object getUserContext() {
      return userContext;
    }

  }

}

```

---

### `netty-channel-fsm/src/main/java/com/digitalpetri/netty/fsm/State.java`

```java
/*
 * Copyright (c) 2024 Kevin Herron
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.digitalpetri.netty.fsm;

public enum State {
  Connecting,
  Connected,
  Disconnecting,
  Idle,
  NotConnected,
  ReconnectWait,
  Reconnecting
}

```

---

### `netty-channel-fsm/src/main/java/com/digitalpetri/netty/fsm/Event.java`

```java
/*
 * Copyright (c) 2024 Kevin Herron
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.digitalpetri.netty.fsm;

import io.netty.channel.Channel;

import java.util.concurrent.CompletableFuture;

public interface Event {

  class ChannelIdle implements Event {

    @Override
    public String toString() {
      return getClass().getSimpleName();
    }
  }

  class ChannelInactive implements Event {

    @Override
    public String toString() {
      return getClass().getSimpleName();
    }
  }

  class Connect implements Event {

    public final CompletableFuture<Channel> channelFuture = new CompletableFuture<>();

    @Override
    public String toString() {
      return getClass().getSimpleName();
    }
  }

  class ConnectSuccess implements Event {

    public final Channel channel;

    public ConnectSuccess(Channel channel) {
      this.channel = channel;
    }

    @Override
    public String toString() {
      return getClass().getSimpleName();
    }
  }

  class ConnectFailure implements Event {

    public final Throwable failure;

    public ConnectFailure(Throwable failure) {
      this.failure = failure;
    }

    @Override
    public String toString() {
      return getClass().getSimpleName();
    }
  }

  class Disconnect implements Event {

    public final CompletableFuture<Void> disconnectFuture = new CompletableFuture<>();

    @Override
    public String toString() {
      return getClass().getSimpleName();
    }
  }

  class DisconnectSuccess implements Event {

    @Override
    public String toString() {
      return getClass().getSimpleName();
    }
  }

  class GetChannel implements Event {

    public final CompletableFuture<Channel> channelFuture = new CompletableFuture<>();

    public final boolean waitForReconnect;

    GetChannel() {
      this(true);
    }

    GetChannel(boolean waitForReconnect) {
      this.waitForReconnect = waitForReconnect;
    }

    @Override
    public String toString() {
      return getClass().getSimpleName();
    }
  }

  class KeepAliveFailure implements Event {

    public final Throwable failure;

    KeepAliveFailure(Throwable failure) {
      this.failure = failure;
    }

    @Override
    public String toString() {
      return getClass().getSimpleName();
    }
  }

  class ReconnectDelayElapsed implements Event {

    @Override
    public String toString() {
      return getClass().getSimpleName();
    }
  }

}

```

---

### `netty-channel-fsm/src/main/java/com/digitalpetri/netty/fsm/Scheduler.java`

```java
/*
 * Copyright (c) 2024 Kevin Herron
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.digitalpetri.netty.fsm;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public interface Scheduler {

  /**
   * Schedule a command to run after a {@code delay}.
   *
   * @param command the commadn to run.
   * @param delay the time to delay.
   * @param unit the time unit of the delay.
   * @return a {@link Cancellable} that can be used to attempt cancellation if needed.
   */
  Cancellable schedule(Runnable command, long delay, TimeUnit unit);

  interface Cancellable {

    /**
     * Attempt to cancel a scheduled command.
     *
     * @return {@code true} if the command was canceled.
     */
    boolean cancel();

  }

  /**
   * Create a {@link Scheduler} from the provided {@link ScheduledExecutorService}.
   *
   * @param scheduledExecutor a {@link ScheduledExecutorService}.
   * @return a {@link Scheduler}.
   */
  static Scheduler fromScheduledExecutor(ScheduledExecutorService scheduledExecutor) {
    return (command, delay, unit) -> {
      ScheduledFuture<?> future = scheduledExecutor.schedule(command, delay, unit);

      return () -> future.cancel(false);
    };
  }

}

```

---

### `netty-channel-fsm/src/main/java/com/digitalpetri/netty/fsm/ChannelFsm.java`

```java
/*
 * Copyright (c) 2024 Kevin Herron
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.digitalpetri.netty.fsm;

import com.digitalpetri.fsm.Fsm;
import com.digitalpetri.fsm.FsmContext;
import com.digitalpetri.fsm.dsl.ActionContext;
import com.digitalpetri.fsm.dsl.FsmBuilder;
import com.digitalpetri.fsm.dsl.TransitionAction;
import com.digitalpetri.netty.fsm.Event.Connect;
import com.digitalpetri.netty.fsm.Event.Disconnect;
import com.digitalpetri.netty.fsm.Event.GetChannel;
import com.digitalpetri.netty.fsm.Scheduler.Cancellable;
import io.netty.channel.Channel;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChannelFsm {

  private final List<TransitionListener> transitionListeners = new CopyOnWriteArrayList<>();

  private final Fsm<State, Event> fsm;

  ChannelFsm(FsmBuilder<State, Event> builder, State initialState) {
    builder.addTransitionAction(new TransitionAction<State, Event>() {
      @Override
      public void execute(ActionContext<State, Event> context) {
        transitionListeners.forEach(
            listener ->
                listener.onStateTransition(context.from(), context.to(), context.event())
        );
      }

      @Override
      public boolean matches(State from, State to, Event event) {
        return true;
      }
    });

    this.fsm = builder.build(initialState);
  }

  public Fsm<State, Event> getFsm() {
    return fsm;
  }

  /**
   * Fire a {@link Connect} event and return a {@link CompletableFuture} that completes successfully
   * with the {@link Channel} if a successful connection is made, or already exists, and completes
   * exceptionally otherwise.
   *
   * @return a {@link CompletableFuture} that completes successfully with the {@link Channel} if a
   *     successful connection was made, or already exists, and completes exceptionally otherwise.
   */
  public CompletableFuture<Channel> connect() {
    Connect connect = new Connect();

    fsm.fireEvent(connect);

    return connect.channelFuture;
  }

  /**
   * Fire a {@link Disconnect} event and return a {@link CompletableFuture} that completes
   * successfully when the {@link Channel} has been closed.
   *
   * @return a {@link CompletableFuture} that completes successfully when the {@link Channel} has
   *     been closed.
   */
  public CompletableFuture<Void> disconnect() {
    Disconnect disconnect = new Disconnect();

    fsm.fireEvent(disconnect);

    return disconnect.disconnectFuture;
  }

  /**
   * Fire a {@link GetChannel} event and return a {@link CompletableFuture} that completes
   * successfully when the {@link Channel} is available and completes exceptionally if the FSM is
   * not currently connected or the connection attempt failed.
   *
   * <p>{@link #connect()} must have been called at least once before attempting to get a Channel.
   * Whether further calls are necessary depends on whether the FSM is configured to be persistent
   * in its connection attempts or not.
   *
   * <p>The returned CompletableFuture always fails exceptionally if the FSM is not connected.
   *
   * <p>This method is equivalent to {@code getChannel(true)} - if the state machine is
   * reconnecting it will wait for the result.
   *
   * @return a {@link CompletableFuture} that completes successfully when the {@link Channel} is
   *     available and completes exceptionally if the FSM is not currently connected or the
   *     connection attempt failed.
   */
  public CompletableFuture<Channel> getChannel() {
    return getChannel(true);
  }

  /**
   * Fire a {@link GetChannel} event and return a {@link CompletableFuture} that completes
   * successfully when the {@link Channel} is available and completes exceptionally if the FSM is
   * not currently connected or the connection attempt failed.
   *
   * <p>{@link #connect()} must have been called at least once before attempting to get a Channel.
   * Whether further calls are necessary depends on whether the FSM is configured to be persistent
   * in its connection attempts or not.
   *
   * <p>The returned CompletableFuture always fails exceptionally if the FSM is not connected.
   *
   * @param waitForReconnect when {@code true} and the state machine is in
   *     {@link State#ReconnectWait} the future will not be completed until the result of the
   *     subsequent reconnect attempt has been obtained. When {@code false} and the state machine is
   *     in {@link State#ReconnectWait} the future is failed immediately. This parameter has no
   *     effect in other states.
   * @return a {@link CompletableFuture} that completes successfully when the {@link Channel} is
   *     available and *completes exceptionally if the FSM is not currently connected or the
   *     connection attempt failed.
   */
  public CompletableFuture<Channel> getChannel(boolean waitForReconnect) {
    CompletableFuture<Channel> future = fsm.getFromContext(ctx -> {
      State state = ctx.currentState();

      if (state == State.Connected) {
        ConnectFuture cf = KEY_CF.get(ctx);

        assert cf != null;

        return cf.future;
      } else {
        return null;
      }
    });

    if (future != null) {
      return future;
    } else {
      // "Slow" path... not connected yet.
      GetChannel getChannel = new GetChannel(waitForReconnect);

      fsm.fireEvent(getChannel);

      return getChannel.channelFuture;
    }
  }

  /**
   * Get the current {@link State} of the {@link ChannelFsm}.
   *
   * @return the current {@link State} of the {@link ChannelFsm}.
   */
  public State getState() {
    return fsm.getFromContext(FsmContext::currentState);
  }

  /**
   * Add a {@link TransitionListener}.
   *
   * @param transitionListener the {@link TransitionListener}.
   */
  public void addTransitionListener(TransitionListener transitionListener) {
    transitionListeners.add(transitionListener);
  }

  /**
   * Remove a previously registered {@link TransitionListener}.
   *
   * @param transitionListener the {@link TransitionListener}.
   */
  public void removeTransitionListener(TransitionListener transitionListener) {
    transitionListeners.remove(transitionListener);
  }

  static final FsmContext.Key<ConnectFuture> KEY_CF =
      new FsmContext.Key<>("connectFuture", ConnectFuture.class);

  static final FsmContext.Key<DisconnectFuture> KEY_DF =
      new FsmContext.Key<>("disconnectFuture", DisconnectFuture.class);

  static final FsmContext.Key<Long> KEY_RD =
      new FsmContext.Key<>("reconnectDelay", Long.class);

  static final FsmContext.Key<Cancellable> KEY_RDF =
      new FsmContext.Key<>("reconnectDelayCancellable", Cancellable.class);

  static class ConnectFuture {

    final CompletableFuture<Channel> future = new CompletableFuture<>();
  }

  static class DisconnectFuture {

    final CompletableFuture<Void> future = new CompletableFuture<>();
  }

  public interface TransitionListener {

    /**
     * A state transition has occurred.
     *
     * <p>Transitions may be internal, i.e. the {@code from} and {@code to} state are the same.
     *
     * <p>Listener notification is implemented as a {@link TransitionAction}, so take care not to
     * block in this callback as it will block the state machine evaluation as well.
     *
     * @param from the {@link State} transitioned from.
     * @param to the {@link State} transitioned to.
     * @param via the {@link Event} that caused the transition.
     */
    void onStateTransition(State from, State to, Event via);

  }

}

```

---

### `netty-channel-fsm/src/main/java/com/digitalpetri/netty/fsm/ChannelFsmConfig.java`

```java
/*
 * Copyright (c) 2024 Kevin Herron
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.digitalpetri.netty.fsm;

import java.util.Map;
import java.util.concurrent.Executor;

public interface ChannelFsmConfig {

  /**
   * {@code true} if the ChannelFsm should be lazy, i.e. after an unintentional channel disconnect
   * it waits in an Idle state until the Channel is requested via {@link ChannelFsm#connect()} or
   * {@link ChannelFsm#getChannel()}.
   *
   * @return {@code true} if the ChannelFsm should be lazy,
   */
  boolean isLazy();

  /**
   * {@code true} if the ChannelFsm should be persistent in its initial connect attempt, i.e. if the
   * initial attempt to connect initiated by {@link ChannelFsm#connect()}} fails, it will
   * immediately move into a reconnecting state and continue to try and establish a connection.
   *
   * <p>Each time a connection attempt fails, including the first, the outstanding
   * {@link java.util.concurrent.CompletableFuture}s will be completed exceptionally.
   *
   * @return {@code true} if the ChannelFsm should be persistent in its initial connect attempt.
   */
  boolean isPersistent();

  /**
   * Get the maximum amount of time, in seconds, before a keep alive occurs on an idle channel.
   *
   * <p>An idle channel is one that that hasn't read any bytes within the time defined by this
   * value.
   *
   * <p>Return 0 to disable keep alives.
   *
   * @return the maximum amount of time, in seconds, before a keep alive occurs on an idle channel.
   */
  int getMaxIdleSeconds();

  /**
   * Get the maximum delay to occur between reconnect attempts. Will be rounded up to the nearest
   * power of 2.
   *
   * <p>The delay is increased exponentially starting at 1 second until the maximum delay, e.g.
   * (1, 2, 4, 8, 16, 32, 32, 32, 32...).
   *
   * @return the maximum delay to occur between reconnect attempts.
   */
  int getMaxReconnectDelaySeconds();

  /**
   * Get the {@link ChannelActions} delegate.
   *
   * @return the {@link ChannelActions} delegate.
   */
  ChannelActions getChannelActions();

  /**
   * Get the {@link Executor} to use.
   *
   * @return the {@link Executor} to use.
   */
  Executor getExecutor();

  /**
   * Get the {@link Scheduler} to use.
   *
   * @return the {@link Scheduler} to use.
   */
  Scheduler getScheduler();

  /**
   * Get the logger name the FSM should use.
   *
   * @return the logger name the FSM should use.
   */
  String getLoggerName();

  /**
   * Get the logging context Map a {@link ChannelFsm} instance will use.
   *
   * <p>Keys and values in the Map will be set on the SLF4J {@link org.slf4j.MDC} when logging.
   *
   * @return the logging context Map a {@link ChannelFsm} instance will use.
   */
  Map<String, String> getLoggingContext();

  /**
   * Get the user-configurable context associated with this ChannelFsm.
   *
   * @return the user-configurable context associated with this ChannelFsm.
   */
  Object getUserContext();

  /**
   * Create a new {@link ChannelFsmConfigBuilder}.
   *
   * @return a new {@link ChannelFsmConfigBuilder}.
   */
  static ChannelFsmConfigBuilder newBuilder() {
    return new ChannelFsmConfigBuilder();
  }

}

```

---

### `netty-channel-fsm/src/main/java/com/digitalpetri/netty/fsm/ChannelFsmFactory.java`

```java
/*
 * Copyright (c) 2024 Kevin Herron
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.digitalpetri.netty.fsm;

import com.digitalpetri.fsm.FsmContext;
import com.digitalpetri.fsm.dsl.ActionContext;
import com.digitalpetri.fsm.dsl.FsmBuilder;
import com.digitalpetri.netty.fsm.Scheduler.Cancellable;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.digitalpetri.netty.fsm.ChannelFsm.*;
import static com.digitalpetri.netty.fsm.CompletionBuilders.completeAsync;

public class ChannelFsmFactory {

  private final ChannelFsmConfig config;

  public ChannelFsmFactory(ChannelFsmConfig config) {
    this.config = config;
  }

  /**
   * Create a new {@link ChannelFsm} instance.
   *
   * @return a new {@link ChannelFsm} instance.
   */
  public ChannelFsm newChannelFsm() {
    return newChannelFsm(State.NotConnected);
  }

  ChannelFsm newChannelFsm(State initialState) {
    var builder = new FsmBuilder<State, Event>(
        config.getLoggerName(),
        config.getLoggingContext(),
        config.getExecutor(),
        config.getUserContext()
    );

    configureChannelFsm(builder, config);

    return new ChannelFsm(builder, initialState);
  }

  /**
   * Create a new {@link ChannelFsm} instance from {@code config}.
   *
   * @param config a {@link ChannelFsmConfig}.
   * @return a new {@link ChannelFsm} from {@code config}.
   */
  public static ChannelFsm newChannelFsm(ChannelFsmConfig config) {
    return new ChannelFsmFactory(config).newChannelFsm();
  }

  private static void configureChannelFsm(FsmBuilder<State, Event> fb, ChannelFsmConfig config) {
    configureNotConnectedState(fb, config);
    configureIdleState(fb, config);
    configureConnectingState(fb, config);
    configureConnectedState(fb, config);
    configureDisconnectingState(fb, config);
    configureReconnectWaitState(fb, config);
    configureReconnectingState(fb, config);
  }

  private static void configureNotConnectedState(
      FsmBuilder<State, Event> fb,
      ChannelFsmConfig config
  ) {

    fb.when(State.NotConnected)
        .on(Event.Connect.class)
        .transitionTo(State.Connecting);

    fb.onInternalTransition(State.NotConnected)
        .via(Event.Disconnect.class)
        .execute(ctx -> {
          Event.Disconnect disconnectEvent = (Event.Disconnect) ctx.event();

          config.getExecutor().execute(() ->
              disconnectEvent.disconnectFuture.complete(null)
          );
        });

    fb.onInternalTransition(State.NotConnected)
        .via(Event.GetChannel.class)
        .execute(ctx -> {
          Event.GetChannel getChannelEvent = (Event.GetChannel) ctx.event();

          config.getExecutor().execute(() ->
              getChannelEvent.channelFuture
                  .completeExceptionally(new Exception("not connected"))
          );
        });
  }

  private static void configureIdleState(FsmBuilder<State, Event> fb, ChannelFsmConfig config) {
    fb.when(State.Idle)
        .on(Event.Connect.class)
        .transitionTo(State.Reconnecting);

    fb.when(State.Idle)
        .on(Event.GetChannel.class)
        .transitionTo(State.Reconnecting);

    fb.when(State.Idle)
        .on(Event.Disconnect.class)
        .transitionTo(State.NotConnected);

    fb.onTransitionFrom(State.Idle)
        .to(State.NotConnected)
        .via(Event.Disconnect.class)
        .execute(ctx -> {
          Event.Disconnect disconnect = (Event.Disconnect) ctx.event();
          config.getExecutor().execute(() ->
              disconnect.disconnectFuture.complete(null)
          );
        });
  }

  private static void configureConnectingState(
      FsmBuilder<State, Event> fb,
      ChannelFsmConfig config
  ) {

    if (config.isPersistent()) {
      if (config.isLazy()) {
        fb.when(State.Connecting)
            .on(Event.ConnectFailure.class)
            .transitionTo(State.Idle);
      } else {
        fb.when(State.Connecting)
            .on(Event.ConnectFailure.class)
            .transitionTo(State.ReconnectWait);
      }
    } else {
      fb.when(State.Connecting)
          .on(Event.ConnectFailure.class)
          .transitionTo(State.NotConnected);
    }

    fb.when(State.Connecting)
        .on(Event.ConnectSuccess.class)
        .transitionTo(State.Connected);

    fb.onTransitionTo(State.Connecting)
        .from(s -> s != State.Connecting)
        .via(e -> e.getClass() == Event.Connect.class)
        .execute(ctx -> {
          ConnectFuture cf = new ConnectFuture();
          KEY_CF.set(ctx, cf);

          handleConnectEvent(ctx, config);

          connect(ctx, config);
        });

    fb.onInternalTransition(State.Connecting)
        .via(Event.Connect.class)
        .execute(ctx -> handleConnectEvent(ctx, config));

    fb.onInternalTransition(State.Connecting)
        .via(Event.GetChannel.class)
        .execute(ctx -> handleGetChannelEvent(ctx, config));

    fb.onInternalTransition(State.Connecting)
        .via(Event.Disconnect.class)
        .execute(ctx -> ctx.shelveEvent(ctx.event()));

    fb.onTransitionFrom(State.Connecting)
        .to(s -> s != State.Connecting)
        .viaAny()
        .execute(FsmContext::processShelvedEvents);

    fb.onTransitionFrom(State.Connecting)
        .to(s -> s != State.Connecting)
        .via(Event.ConnectFailure.class)
        .execute(ctx -> handleConnectFailureEvent(ctx, config));
  }

  private static void configureConnectedState(
      FsmBuilder<State, Event> fb,
      ChannelFsmConfig config
  ) {

    Logger logger = LoggerFactory.getLogger(config.getLoggerName());

    fb.when(State.Connected)
        .on(Event.Disconnect.class)
        .transitionTo(State.Disconnecting);

    if (config.isLazy()) {
      fb.when(State.Connected)
          .on(e ->
              e.getClass() == Event.ChannelInactive.class
                  || e.getClass() == Event.KeepAliveFailure.class)
          .transitionTo(State.Idle);
    } else {
      fb.when(State.Connected)
          .on(e ->
              e.getClass() == Event.ChannelInactive.class
                  || e.getClass() == Event.KeepAliveFailure.class)
          .transitionTo(State.ReconnectWait);
    }

    fb.onTransitionTo(State.Connected)
        .from(s -> s != State.Connected)
        .via(Event.ConnectSuccess.class)
        .execute(ctx -> {
          Event.ConnectSuccess event = (Event.ConnectSuccess) ctx.event();
          Channel channel = event.channel;

          if (config.getMaxIdleSeconds() > 0) {
            channel.pipeline()
                .addFirst(new IdleStateHandler(config.getMaxIdleSeconds(), 0, 0));
          }

          channel.pipeline().addLast(new ChannelInboundHandlerAdapter() {
            @Override
            public void channelInactive(
                ChannelHandlerContext channelContext
            ) throws Exception {

              config.getLoggingContext().forEach(MDC::put);
              try {
                logger.debug(
                    "channelInactive() local={}, remote={}",
                    channelContext.channel().localAddress(),
                    channelContext.channel().remoteAddress()
                );
              } finally {
                config.getLoggingContext().keySet().forEach(MDC::remove);
              }

              if (ctx.currentState() == State.Connected) {
                ctx.fireEvent(new Event.ChannelInactive());
              }

              super.channelInactive(channelContext);
            }

            @Override
            public void exceptionCaught(
                ChannelHandlerContext channelContext,
                Throwable cause
            ) {

              config.getLoggingContext().forEach(MDC::put);
              try {
                logger.debug(
                    "exceptionCaught() local={}, remote={}",
                    channelContext.channel().localAddress(),
                    channelContext.channel().remoteAddress(),
                    cause
                );
              } finally {
                config.getLoggingContext().keySet().forEach(MDC::remove);
              }

              if (ctx.currentState() == State.Connected) {
                channelContext.close();
              }
            }

            @Override
            public void userEventTriggered(
                ChannelHandlerContext channelContext,
                Object evt
            ) throws Exception {

              if (evt instanceof IdleStateEvent) {
                IdleState idleState = ((IdleStateEvent) evt).state();

                if (idleState == IdleState.READER_IDLE) {
                  config.getLoggingContext().forEach(MDC::put);
                  try {
                    logger.debug("channel idle, maxIdleSeconds={}", config.getMaxIdleSeconds());
                  } finally {
                    config.getLoggingContext().keySet().forEach(MDC::remove);
                  }

                  ctx.fireEvent(new Event.ChannelIdle());
                }
              }

              super.userEventTriggered(channelContext, evt);
            }
          });

          ConnectFuture cf = KEY_CF.get(ctx);
          if (cf != null) {
            config.getExecutor().execute(() -> cf.future.complete(channel));
          }
        });

    fb.onInternalTransition(State.Connected)
        .via(Event.Connect.class)
        .execute(ctx -> handleConnectEvent(ctx, config));

    fb.onInternalTransition(State.Connected)
        .via(Event.GetChannel.class)
        .execute(ctx -> handleGetChannelEvent(ctx, config));

    fb.onInternalTransition(State.Connected)
        .via(Event.ChannelIdle.class)
        .execute(ctx -> {
          ConnectFuture cf = KEY_CF.get(ctx);

          cf.future.thenAcceptAsync(ch -> {
            CompletableFuture<Void> keepAliveFuture =
                config.getChannelActions().keepAlive(ctx, ch);

            keepAliveFuture.whenComplete((v, ex) -> {
              if (ex != null) {
                ctx.fireEvent(new Event.KeepAliveFailure(ex));
              }
            });
          }, config.getExecutor());
        });

    fb.onTransitionFrom(State.Connected)
        .to(s -> s == State.Idle || s == State.ReconnectWait)
        .via(Event.KeepAliveFailure.class)
        .execute(ctx -> {
          ConnectFuture cf = KEY_CF.get(ctx);

          cf.future.thenAccept(Channel::close);
        });
  }

  private static void configureDisconnectingState(
      FsmBuilder<State, Event> fb,
      ChannelFsmConfig config
  ) {

    fb.when(State.Disconnecting)
        .on(Event.DisconnectSuccess.class)
        .transitionTo(State.NotConnected);

    fb.onTransitionTo(State.Disconnecting)
        .from(State.Connected)
        .via(Event.Disconnect.class)
        .execute(ctx -> {
          DisconnectFuture df = new DisconnectFuture();
          KEY_DF.set(ctx, df);

          Event.Disconnect event = (Event.Disconnect) ctx.event();

          completeAsync(event.disconnectFuture, config.getExecutor()).with(df.future);

          disconnect(ctx, config);
        });

    fb.onInternalTransition(State.Disconnecting)
        .via(e -> e.getClass() == Event.Connect.class || e.getClass() == Event.GetChannel.class)
        .execute(ctx -> ctx.shelveEvent(ctx.event()));

    fb.onInternalTransition(State.Disconnecting)
        .via(Event.Disconnect.class)
        .execute(ctx -> {
          DisconnectFuture df = KEY_DF.get(ctx);

          if (df != null) {
            Event.Disconnect event = (Event.Disconnect) ctx.event();

            completeAsync(event.disconnectFuture, config.getExecutor()).with(df.future);
          }
        });

    fb.onTransitionFrom(State.Disconnecting)
        .to(s -> s != State.Disconnecting)
        .via(Event.DisconnectSuccess.class)
        .execute(ctx -> {
          DisconnectFuture df = KEY_DF.remove(ctx);

          if (df != null) {
            config.getExecutor().execute(() -> df.future.complete(null));
          }
        });

    fb.onTransitionFrom(State.Disconnecting)
        .to(s -> s != State.Disconnecting)
        .viaAny()
        .execute(FsmContext::processShelvedEvents);
  }

  private static void configureReconnectWaitState(
      FsmBuilder<State, Event> fb,
      ChannelFsmConfig config
  ) {

    fb.when(State.ReconnectWait)
        .on(Event.ReconnectDelayElapsed.class)
        .transitionTo(State.Reconnecting);

    fb.when(State.ReconnectWait)
        .on(Event.Disconnect.class)
        .transitionTo(State.NotConnected);

    // This needs to be defined before the action after it so the previous
    // ConnectFuture can be notified before a new ConnectFuture is set.
    fb.onTransitionTo(State.ReconnectWait)
        .from(State.Reconnecting)
        .via(Event.ConnectFailure.class)
        .execute(ctx -> handleConnectFailureEvent(ctx, config));

    fb.onTransitionTo(State.ReconnectWait)
        .from(s -> s != State.ReconnectWait)
        .viaAny()
        .execute(ctx -> {
          KEY_CF.set(ctx, new ConnectFuture());

          Long delay = KEY_RD.get(ctx);
          if (delay == null) {
            delay = 1L;
          } else {
            delay = Math.min(getMaxReconnectDelay(config), delay << 1);
          }
          KEY_RD.set(ctx, delay);

          Cancellable reconnectDelayFuture = config.getScheduler().schedule(
              () ->
                  ctx.fireEvent(new Event.ReconnectDelayElapsed()),
              delay,
              TimeUnit.SECONDS
          );

          KEY_RDF.set(ctx, reconnectDelayFuture);
        });

    fb.onInternalTransition(State.ReconnectWait)
        .via(Event.Connect.class)
        .execute(ctx -> handleConnectEvent(ctx, config));

    fb.onInternalTransition(State.ReconnectWait)
        .via(Event.GetChannel.class)
        .execute(ctx -> {
          Event.GetChannel event = (Event.GetChannel) ctx.event();

          if (event.waitForReconnect) {
            handleGetChannelEvent(ctx, config);
          } else {
            config.getExecutor().execute(() ->
                event.channelFuture
                    .completeExceptionally(new Exception("not reconnected"))
            );
          }
        });

    fb.onTransitionFrom(State.ReconnectWait)
        .to(State.NotConnected)
        .via(Event.Disconnect.class)
        .execute(ctx -> {
          ConnectFuture connectFuture = KEY_CF.remove(ctx);
          if (connectFuture != null) {
            config.getExecutor().execute(() ->
                connectFuture.future
                    .completeExceptionally(new Exception("client disconnected"))
            );
          }

          KEY_RD.remove(ctx);

          Cancellable reconnectDelayCancellable = KEY_RDF.remove(ctx);
          if (reconnectDelayCancellable != null) {
            reconnectDelayCancellable.cancel();
          }

          Event.Disconnect disconnect = (Event.Disconnect) ctx.event();
          config.getExecutor().execute(() ->
              disconnect.disconnectFuture.complete(null)
          );
        });
  }

  private static void configureReconnectingState(
      FsmBuilder<State, Event> fb,
      ChannelFsmConfig config
  ) {

    fb.when(State.Reconnecting)
        .on(Event.ConnectFailure.class)
        .transitionTo(State.ReconnectWait);

    fb.when(State.Reconnecting)
        .on(Event.ConnectSuccess.class)
        .transitionTo(State.Connected);

    fb.onTransitionTo(State.Reconnecting)
        .from(State.ReconnectWait)
        .via(Event.ReconnectDelayElapsed.class)
        .execute(ctx -> connect(ctx, config));

    fb.onTransitionTo(State.Reconnecting)
        .from(State.Idle)
        .via(e -> e.getClass() == Event.Connect.class || e.getClass() == Event.GetChannel.class)
        .execute(ctx -> {
          ConnectFuture cf = new ConnectFuture();
          KEY_CF.set(ctx, cf);

          Event event = ctx.event();

          if (event instanceof Event.Connect) {
            handleConnectEvent(ctx, config);
          } else if (event instanceof Event.GetChannel) {
            handleGetChannelEvent(ctx, config);
          }

          connect(ctx, config);
        });

    fb.onInternalTransition(State.Reconnecting)
        .via(Event.Connect.class)
        .execute(ctx -> handleConnectEvent(ctx, config));

    fb.onInternalTransition(State.Reconnecting)
        .via(Event.GetChannel.class)
        .execute(ctx -> handleGetChannelEvent(ctx, config));

    fb.onInternalTransition(State.Reconnecting)
        .via(Event.Disconnect.class)
        .execute(ctx -> ctx.shelveEvent(ctx.event()));

    fb.onTransitionFrom(State.Reconnecting)
        .to(s -> s != State.Reconnecting)
        .viaAny()
        .execute(FsmContext::processShelvedEvents);

    fb.onTransitionFrom(State.Reconnecting)
        .to(State.Connected)
        .via(Event.ConnectSuccess.class)
        .execute(ctx -> {
          KEY_RD.remove(ctx);
          KEY_RDF.remove(ctx);
        });
  }

  private static void connect(
      ActionContext<State, Event> ctx,
      ChannelFsmConfig config
  ) {

    config.getExecutor().execute(() ->
        config.getChannelActions().connect(ctx).whenComplete((channel, ex) -> {
          if (channel != null) {
            ctx.fireEvent(new Event.ConnectSuccess(channel));
          } else {
            ctx.fireEvent(new Event.ConnectFailure(ex));
          }
        })
    );
  }

  private static void disconnect(
      ActionContext<State, Event> ctx,
      ChannelFsmConfig config
  ) {

    ConnectFuture connectFuture = KEY_CF.get(ctx);

    if (connectFuture != null && connectFuture.future.isDone()) {
      config.getExecutor().execute(() -> {
        CompletableFuture<Void> disconnectFuture = config.getChannelActions().disconnect(
            ctx,
            connectFuture.future.getNow(null)
        );

        disconnectFuture.whenComplete(
            (v, ex) -> ctx.fireEvent(new Event.DisconnectSuccess()));
      });
    } else {
      ctx.fireEvent(new Event.DisconnectSuccess());
    }
  }

  private static void handleConnectEvent(
      ActionContext<State, Event> ctx,
      ChannelFsmConfig config
  ) {

    CompletableFuture<Channel> channelFuture = KEY_CF.get(ctx).future;

    Event.Connect connectEvent = (Event.Connect) ctx.event();
    completeAsync(connectEvent.channelFuture, config.getExecutor()).with(channelFuture);
  }

  private static void handleGetChannelEvent(
      ActionContext<State, Event> ctx,
      ChannelFsmConfig config
  ) {

    CompletableFuture<Channel> channelFuture = KEY_CF.get(ctx).future;

    Event.GetChannel getChannelEvent = (Event.GetChannel) ctx.event();
    completeAsync(getChannelEvent.channelFuture, config.getExecutor()).with(channelFuture);
  }

  private static void handleConnectFailureEvent(
      ActionContext<State, Event> ctx,
      ChannelFsmConfig config
  ) {

    ConnectFuture cf = KEY_CF.remove(ctx);

    if (cf != null) {
      Event.ConnectFailure connectFailureEvent = (Event.ConnectFailure) ctx.event();

      config.getExecutor().execute(() ->
          cf.future.completeExceptionally(connectFailureEvent.failure)
      );
    }
  }

  private static int getMaxReconnectDelay(ChannelFsmConfig config) {
    int maxReconnectDelay = config.getMaxReconnectDelaySeconds();

    if (maxReconnectDelay < 1) {
      maxReconnectDelay = ChannelFsmConfigBuilder.DEFAULT_MAX_RECONNECT_DELAY_SECONDS;
    }

    int highestOneBit = Integer.highestOneBit(maxReconnectDelay);

    if (maxReconnectDelay == highestOneBit) {
      return maxReconnectDelay;
    } else {
      return highestOneBit << 1;
    }
  }

}

```

