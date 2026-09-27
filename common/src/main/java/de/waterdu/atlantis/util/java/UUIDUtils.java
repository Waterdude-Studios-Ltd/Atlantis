package de.waterdu.atlantis.util.java;

import java.util.Random;
import java.util.UUID;

public final class UUIDUtils {

    public static final UUID ZEROED_UUID = new UUID(0L, 0L);

    private UUIDUtils() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    public static UUID fromString(String uuid) {
        if (uuid.length() == 32) {
            return UUID.fromString(
                    uuid.substring(0, 8) + '-' +
                    uuid.substring(8, 12) + '-' +
                    uuid.substring(12, 16) + '-' +
                    uuid.substring(16, 20) + '-' +
                    uuid.substring(20, 32)
            );
        } else {
            return UUID.fromString(uuid);
        }
    }

    public static UUID fromHashCode(Object object) {
        return UUIDUtils.fromSeed(object.hashCode());
    }

    public static UUID fromSeed(long seed) {
        var ng = new Random(seed);
        var randomBytes = new byte[16];
        ng.nextBytes(randomBytes);
        randomBytes[6] &= 0x0f;
        randomBytes[6] |= 0x40;
        randomBytes[8] &= 0x3f;
        randomBytes[8] |= (byte) 0x80;
        var msb = 0L;
        var lsb = 0L;
        for (var i = 0; i < 8; i++) {
            msb = (msb << 8) | (randomBytes[i] & 0xff);
        }
        for (var i = 8; i < 16; i++) {
            lsb = (lsb << 8) | (randomBytes[i] & 0xff);
        }
        return new UUID(msb, lsb);
    }
}
