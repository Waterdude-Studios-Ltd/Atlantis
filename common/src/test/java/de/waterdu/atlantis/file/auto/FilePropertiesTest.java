package de.waterdu.atlantis.file.auto;

import de.waterdu.atlantis.file.datatypes.Configuration;
import de.waterdu.atlantis.file.storage.Flatfile;
import de.waterdu.atlantis.file.storage.Volatile;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests how {@link FileProperties} is built from each file annotation.
 */
class FilePropertiesTest {

    /**
     * Checks that {@link BasicConfiguration} uses the lowercase class name as a JSON file name.
     */
    @Test
    void basicConfigurationUsesLowercaseClassNameAsJsonFile() {
        var properties = new FileProperties(SampleConfig.class.getAnnotation(BasicConfiguration.class), SampleConfig.class);

        assertThat(properties).isEqualTo(new FileProperties(
                "sample_mod", "sampleconfig.json", Flatfile.class, LoadOrder.NORMAL, false, false, NoConfiguration.class, "", false
        ));
    }

    /**
     * Checks that {@link BasicData} uses the lowercase class name as a forced directory.
     */
    @Test
    void basicDataUsesLowercaseClassNameAsForcedDirectory() {
        var properties = new FileProperties(SampleData.class.getAnnotation(BasicData.class), SampleData.class);

        assertThat(properties).isEqualTo(new FileProperties(
                "sample_mod", "sampledata", Flatfile.class, LoadOrder.NORMAL, true, false, NoConfiguration.class, "", false
        ));
    }

    /**
     * Checks that {@link I18nFile} uses {@code lang.json} and turns on i18n.
     */
    @Test
    void i18nFileUsesLangJson() {
        var properties = new FileProperties(SampleLang.class.getAnnotation(I18nFile.class));

        assertThat(properties).isEqualTo(new FileProperties(
                "sample_mod", "lang.json", Flatfile.class, LoadOrder.NORMAL, false, true, NoConfiguration.class, "", false
        ));
    }

    /**
     * Checks that {@link FileBackedObject} gives the default values when only the mod and path are set.
     */
    @Test
    void fileBackedObjectDefaults() {
        var properties = new FileProperties(DefaultBacked.class.getAnnotation(FileBackedObject.class));

        assertThat(properties).isEqualTo(new FileProperties(
                "sample_mod", "dir/file.json", Flatfile.class, LoadOrder.NORMAL, false, false, NoConfiguration.class, "", false
        ));
    }

    /**
     * Checks that every attribute of {@link FileBackedObject} is copied into the properties.
     */
    @Test
    void fileBackedObjectCopiesEveryAttribute() {
        var properties = new FileProperties(CustomBacked.class.getAnnotation(FileBackedObject.class));

        assertThat(properties).isEqualTo(new FileProperties(
                "other_mod", "custom.json", Volatile.class, LoadOrder.LATE, true, true, SampleConfig.class, "storage", true
        ));
    }

    /**
     * Checks that a path that ends in {@code .aon} turns on AON.
     */
    @Test
    void aonPathImpliesAon() {
        var properties = new FileProperties(AonBacked.class.getAnnotation(FileBackedObject.class));

        assertThat(properties.aon()).isTrue();
    }

    /**
     * A configuration with a {@link BasicConfiguration} annotation.
     */
    @BasicConfiguration(mod = "sample_mod")
    static final class SampleConfig implements Configuration {}

    /**
     * A configuration with a {@link BasicData} annotation.
     */
    @BasicData(mod = "sample_mod")
    static final class SampleData implements Configuration {}

    /**
     * A configuration with an {@link I18nFile} annotation.
     */
    @I18nFile(mod = "sample_mod")
    static final class SampleLang implements Configuration {}

    /**
     * A configuration with a {@link FileBackedObject} annotation that sets only the mod and path.
     */
    @FileBackedObject(mod = "sample_mod", path = "dir/file.json")
    static final class DefaultBacked implements Configuration {}

    /**
     * A configuration with a {@link FileBackedObject} annotation that sets every attribute.
     */
    @FileBackedObject(
            mod = "other_mod",
            path = "custom.json",
            storage = Volatile.class,
            order = LoadOrder.LATE,
            force = true,
            i18n = true,
            providerClass = SampleConfig.class,
            providerMethod = "storage",
            aon = true
    )
    static final class CustomBacked implements Configuration {}

    /**
     * A configuration with a {@link FileBackedObject} annotation whose path ends in {@code .aon}.
     */
    @FileBackedObject(mod = "sample_mod", path = "settings.aon")
    static final class AonBacked implements Configuration {}
}
