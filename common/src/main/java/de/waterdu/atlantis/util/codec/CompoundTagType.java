package de.waterdu.atlantis.util.codec;

import com.google.common.collect.Maps;
import com.google.gson.reflect.TypeToken;
import de.waterdu.atlantis.util.java.interfaces.TriConsumer;
import net.minecraft.nbt.CompoundTag;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

@SuppressWarnings("unchecked")
public enum CompoundTagType {

    INT_PRIMITIVE(int.class.getTypeName(), CompoundTag::getInt, (tag, key, value) -> tag.putInt(key, (int) value)),
    LONG_PRIMITIVE(long.class.getTypeName(), CompoundTag::getLong, (tag, key, value) -> tag.putLong(key, (long) value)),
    BOOLEAN_PRIMITIVE(boolean.class.getTypeName(), CompoundTag::getBoolean, (tag, key, value) -> tag.putBoolean(key, (boolean) value)),
    BYTE_PRIMITIVE(byte.class.getTypeName(), CompoundTag::getByte, (tag, key, value) -> tag.putByte(key, (byte) value)),
    DOUBLE_PRIMITIVE(double.class.getTypeName(), CompoundTag::getDouble, (tag, key, value) -> tag.putDouble(key, (double) value)),
    FLOAT_PRIMITIVE(float.class.getTypeName(), CompoundTag::getFloat, (tag, key, value) -> tag.putFloat(key, (float) value)),
    SHORT_PRIMITIVE(short.class.getTypeName(), CompoundTag::getShort, (tag, key, value) -> tag.putShort(key, (short) value)),
    INTEGER_OBJECT(Integer.class.getTypeName(), CompoundTag::getInt, (tag, key, value) -> tag.putInt(key, (Integer) value)),
    LONG_OBJECT(Long.class.getTypeName(), CompoundTag::getLong, (tag, key, value) -> tag.putLong(key, (Long) value)),
    BOOLEAN_OBJECT(Boolean.class.getTypeName(), CompoundTag::getBoolean, (tag, key, value) -> tag.putBoolean(key, (Boolean) value)),
    BYTE_OBJECT(Byte.class.getTypeName(), CompoundTag::getByte, (tag, key, value) -> tag.putByte(key, (Byte) value)),
    DOUBLE_OBJECT(Double.class.getTypeName(), CompoundTag::getDouble, (tag, key, value) -> tag.putDouble(key, (Double) value)),
    FLOAT_OBJECT(Float.class.getTypeName(), CompoundTag::getFloat, (tag, key, value) -> tag.putFloat(key, (Float) value)),
    SHORT_OBJECT(Short.class.getTypeName(), CompoundTag::getShort, (tag, key, value) -> tag.putShort(key, (Short) value)),
    STRING(String.class.getTypeName(), CompoundTag::getString, (tag, key, value) -> tag.putString(key, (String) value)),
    UUID(java.util.UUID.class.getTypeName(), CompoundTag::getUUID, (tag, key, value) -> tag.putUUID(key, (java.util.UUID) value)),
    INT_ARRAY(int[].class.getTypeName(), CompoundTag::getIntArray, (tag, key, value) -> tag.putIntArray(key, (int[]) value)),
    LONG_ARRAY(long[].class.getTypeName(), CompoundTag::getLongArray, (tag, key, value) -> tag.putLongArray(key, (long[]) value)),
    BYTE_ARRAY(byte[].class.getTypeName(), CompoundTag::getByteArray, (tag, key, value) -> tag.putByteArray(key, (byte[]) value)),
    INTEGER_LIST(TypeToken.getParameterized(List.class, Integer.class).getType().getTypeName(), CompoundTag::getIntArray, (tag, key, value) -> tag.putIntArray(key, (List<Integer>) value)),
    LONG_LIST(TypeToken.getParameterized(List.class, Long.class).getType().getTypeName(), CompoundTag::getLongArray, (tag, key, value) -> tag.putLongArray(key, (List<Long>) value)),
    BYTE_LIST(TypeToken.getParameterized(List.class, Byte.class).getType().getTypeName(), CompoundTag::getByteArray, (tag, key, value) -> tag.putByteArray(key, (List<Byte>) value)),
    ;

    private static final Map<String, Optional<CompoundTagType>> MAPPED_TYPES = Maps.newHashMap();
    static {
        for(CompoundTagType type : CompoundTagType.values()) {
            MAPPED_TYPES.put(type.typeName, Optional.of(type));
        }
    }

    private final String typeName;
    private final BiFunction<CompoundTag, String, Object> read;
    private final TriConsumer<CompoundTag, String, Object> write;

    CompoundTagType(String typeName, BiFunction<CompoundTag, String, Object> read, TriConsumer<CompoundTag, String, Object> write) {
        this.typeName = typeName;
        this.read = read;
        this.write = write;
    }

    public static Optional<CompoundTagType> get(Type type) {
        return CompoundTagType.get(type.getTypeName());
    }

    public static Optional<CompoundTagType> get(String typeName) {
        return MAPPED_TYPES.getOrDefault(typeName, Optional.empty());
    }

    public <T> T read(CompoundTag tag, String key) {
        return (T) this.read.apply(tag, key);
    }

    public <T> void write(CompoundTag tag, String key, T value) {
        this.write.accept(tag, key, value);
    }
}
