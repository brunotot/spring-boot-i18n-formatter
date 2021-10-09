package hr.srce.croris;

import java.io.File;
import java.util.Map;

public class TranslationFileWrapper {
    private final File file;
    private String writeableString;
    private Map<String, String> translations;

    public TranslationFileWrapper(final File file) {
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    public String getWriteableString() {
        return writeableString;
    }

    public void setWriteableString(String writeableString) {
        this.writeableString = writeableString;
    }

    public Map<String, String> getTranslations() {
        return translations;
    }

    public void setTranslations(Map<String, String> translations) {
        this.translations = translations;
    }
}
