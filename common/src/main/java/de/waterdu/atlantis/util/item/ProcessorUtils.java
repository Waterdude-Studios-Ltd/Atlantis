package de.waterdu.atlantis.util.item;

import de.waterdu.atlantis.AtlantisLogger;
import de.waterdu.atlantis.util.java.ExceptionUtils;
import net.minecraft.world.item.ItemStack;

import java.awt.Color;

public final class ProcessorUtils {

    public static final String INVALID_PROCESSOR_ERROR = "Invalid argument for ConfigurableItemStack processor {}: {}";

    private ProcessorUtils() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    public static void shortValue(String[] kv, ItemStack s, String tag, String processor) {
        try {
            ItemUtils.tag(s, c -> c.putShort(tag, Short.parseShort(kv[1])));
        } catch (NumberFormatException e) {
            AtlantisLogger.warn(INVALID_PROCESSOR_ERROR, processor, kv[1]);
        }
    }

    public static void intValue(String[] kv, ItemStack s, String tag, String processor) {
        try {
            ItemUtils.tag(s, c -> c.putInt(tag, Integer.parseInt(kv[1])));
        } catch (NumberFormatException e) {
            AtlantisLogger.warn(INVALID_PROCESSOR_ERROR, processor, kv[1]);
        }
    }

    public static void floatValue(String[] kv, ItemStack s, String tag, String processor) {
        try {
            ItemUtils.tag(s, c -> c.putFloat(tag, Float.parseFloat(kv[1])));
        } catch (NumberFormatException e) {
            AtlantisLogger.warn(INVALID_PROCESSOR_ERROR, processor, kv[1]);
        }
    }

    public static void dualIntValue(String[] kv, ItemStack s, String tag1, String tag2, String processor) {
        var splitTemp = kv[1].split(",");
        String[] split;
        if (splitTemp.length == 1) {
            split = new String[]{splitTemp[0], splitTemp[0]};
        } else {
            split = splitTemp;
        }
        try {
            ItemUtils.tag(s, c -> c.putInt(tag1, Short.parseShort(split[0])));
            ItemUtils.tag(s, c -> c.putInt(tag2, Short.parseShort(split[0])));
        } catch (NumberFormatException e) {
            AtlantisLogger.warn(INVALID_PROCESSOR_ERROR, processor, kv[1]);
        }
    }

    public static void dualFloatValue(String[] kv, ItemStack s, String tag1, String tag2, String processor) {
        var splitTemp = kv[1].split(",");
        String[] split;
        if (splitTemp.length == 1) {
            split = new String[]{splitTemp[0], splitTemp[0]};
        } else {
            split = splitTemp;
        }
        try {
            ItemUtils.tag(s, c -> c.putFloat(tag1, Float.parseFloat(split[0])));
            ItemUtils.tag(s, c -> c.putFloat(tag2, Float.parseFloat(split[0])));
        } catch (NumberFormatException e) {
            AtlantisLogger.warn(INVALID_PROCESSOR_ERROR, processor, kv[1]);
        }
    }

    public static void singleColorValue(String[] kv, ItemStack s, String tag, String processor) {
        try {
            float value;
            if (kv[1].contains(".")) {
                value = Float.parseFloat(kv[1]);
            } else {
                value = Integer.parseInt(kv[1]) / 255F;
            }
            ItemUtils.tag(s, c -> c.putFloat(tag, value));
        } catch (NumberFormatException e) {
            AtlantisLogger.warn(INVALID_PROCESSOR_ERROR, processor, kv[1]);
        }
    }

    public static void colorValue(String[] kv, ItemStack s, String r, String g, String b, String a, String processor) {
        try {
            if (!kv[1].startsWith("#")) {
                kv[1] = "#" + kv[1];
            }
            var color = Color.decode(kv[1]).getColorComponents(null);
            ItemUtils.tag(s, c -> c.putFloat(r, color[0]));
            ItemUtils.tag(s, c -> c.putFloat(g, color[1]));
            ItemUtils.tag(s, c -> c.putFloat(b, color[2]));
            ItemUtils.tag(s, c -> c.putFloat(a, color.length > 3 ? color[3] : 1F));
        } catch (NumberFormatException | IndexOutOfBoundsException | NullPointerException e) {
            AtlantisLogger.warn(INVALID_PROCESSOR_ERROR, processor, kv[1]);
        }
    }
}
