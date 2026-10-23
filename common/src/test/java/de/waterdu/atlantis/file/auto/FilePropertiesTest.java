package de.waterdu.atlantis.file.auto;

import de.waterdu.atlantis.file.datatypes.Configuration;
import de.waterdu.atlantis.file.storage.Flatfile;
import de.waterdu.atlantis.file.storage.Volatile;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FilePropertiesTest {

    @Test
    void basicConfigurationUsesLowercaseClassNameAsJsonFile() {
        var properties = new FileProperties(SampleConfig.class.getAnnotation(BasicConfiguration.class), SampleConfig.class);

        assertThat(properties).isEqualTo(new FileProperties(
                "sample_mod", "sampleconfig.json", Flatfile.class, LoadOrder.NORMAL, false, false, NoConfiguration.class, "", false
        ));
    }

    @Test
    void basicDataUsesLowercaseClassNameAsForcedDirectory() {
        var properties = new FileProperties(SampleData.class.getAnnotation(BasicData.class), SampleData.class);

        assertThat(properties).isEqualTo(new FileProperties(
                "sample_mod", "sampledata", Flatfile.class, LoadOrder.NORMAL, true, false, NoConfiguration.class, "", false
        ));
    }

    @Test
    void i18nFileUsesLangJson() {
        var properties = new FileProperties(SampleLang.class.getAnnotation(I18nFile.class));

        assertThat(properties).isEqualTo(new FileProperties(
                "sample_mod", "lang.json", Flatfile.class, LoadOrder.NORMAL, false, true, NoConfiguration.class, "", false
        ));
    }

    @Test
    void fileBackedObjectDefaults() {
        var properties = new FileProperties(DefaultBacked.class.getAnnotation(FileBackedObject.class));

        assertThat(properties).isEqualTo(new FileProperties(
                "sample_mod", "dir/file.json", Flatfile.class, LoadOrder.NORMAL, false, false, NoConfiguration.class, "", false
        ));
    }

    @Test
    void fileBackedObjectCopiesEveryAttribute() {
        var properties = new FileProperties(CustomBacked.class.getAnnotation(FileBackedObject.class));

        assertThat(properties).isEqualTo(new FileProperties(
                "other_mod", "custom.json", Volatile.class, LoadOrder.LATE, true, true, SampleConfig.class, "storage", true
        ));
    }

    @Test
    void aonPathImpliesAon() {
        var properties = new FileProperties(AonBacked.class.getAnnotation(FileBackedObject.class));

        assertThat(properties.aon()).isTrue();
    }

    @BasicConfiguration(mod = "sample_mod")
    static final class SampleConfig implements Configuration {}

    @BasicData(mod = "sample_mod")
    static final class SampleData implements Configuration {}

    @I18nFile(mod = "sample_mod")
    static final class SampleLang implements Configuration {}

    @FileBackedObject(mod = "sample_mod", path = "dir/file.json")
    static final class DefaultBacked implements Configuration {}

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

    @FileBackedObject(mod = "sample_mod", path = "settings.aon")
    static final class AonBacked implements Configuration {}
}
