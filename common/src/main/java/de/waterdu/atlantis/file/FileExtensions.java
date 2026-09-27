package de.waterdu.atlantis.file;

import de.waterdu.atlantis.util.java.ExceptionUtils;

public final class FileExtensions {

    public static final String JSON = ".json";
    public static final String AON = ".aon";
    public static final String TXT = ".txt";
    public static final String ZIP = ".zip";
    public static final String _7Z = ".7z";
    public static final String TAR = ".tar";
    public static final String JAR = ".jar";
    public static final String PNG = ".png";
    public static final String OGG = ".ogg";
    public static final String BIN = ".bin";
    public static final String DAT = ".dat";
    public static final String RTF = ".rtf";
    public static final String TOML = ".toml";
    public static final String XML = ".xml";
    public static final String YAML = ".yaml";
    public static final String HOCON = ".hocon";
    public static final String PDF = ".pdf";
    public static final String JAVA = ".java";
    public static final String CLASS = ".class";
    public static final String KT = ".kt";
    public static final String BAT = ".bat";
    public static final String PY = ".py";
    public static final String SH = ".sh";

    private FileExtensions() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }
}
