package hr.srce.croris;

import com.github.pemistahl.lingua.api.Language;
import com.github.pemistahl.lingua.api.LanguageDetector;
import com.github.pemistahl.lingua.api.LanguageDetectorBuilder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class TranslationFolderWrapper {
    private static final String SRC_FOLDER_SUBSTRING = "/src";
    private static final Double MINIMUM_LANGUAGE_PROBABILITY = 0.75;
    private static final String TRANSLATION_FILE_NAME_STARTING_SUBSTRING = "messages_";
    private static final String TRANSLATION_FILE_NAME_ENDING_SUBSTRING = ".properties";
    private static final String TRANSLATION_COMMENT_KEY = "#";
    private static final String LINE_BREAK_KEY = "\\";
    private static final String KEY_VALUE_SPLIT_REGEX = "=";
    private static final String KEY_LEVEL_INDENTATION_KEY = "\\.";
    private static final Integer MAX_CHARACTERS_IN_LINE_COUNT = 120;
    private static final String TAB = "  ";
    private static final String KEY_IN_CORE = "KEY_IN_CORE";
    private static final String KEY_NOT_IN_USE = "KEY_NOT_IN_USE";
    private static final String VALUE_EMPTY = "VALUE_EMPTY";
    private static final String WRONG_LANGUAGE_TRANSLATION = "WRONG_LANGUAGE_TRANSLATION";
    private static final String KEY_MISSING = "KEY_MISSING";
    private static final String DUPLICATE_VALUES = "DUPLICATE_VALUES";
    private static final List<String> IGNORABLE_ENDING_DIFF_FILE_SUBSTRINGS_TRANSLATION_KEYS = List.of(
            "js",
            "html",
            "java"
    );
    private static final List<String> IGNORABLE_ENDING_SUBSTRINGS_TRANSLATION_KEYS = List.of(
            "create.success",
            "create.failure",
            "update.success",
            "update.failure",
            "delete.success",
            "delete.failure"
    );
    private static final List<String> IGNORABLE_STARTING_SUBSTRINGS_TRANSLATION_KEYS = List.of(
            "excel.",
            "ustanova.export."
    );
    private static final Map<String, String> DIFFERENCES_MAP = Map.ofEntries(
            new AbstractMap.SimpleEntry<>(KEY_IN_CORE, "Ključ već postoji u Core modulu."),
            new AbstractMap.SimpleEntry<>(KEY_NOT_IN_USE, "Ključ se ne koristi u projektu."),
            new AbstractMap.SimpleEntry<>(VALUE_EMPTY, "Vrijednost prijevoda je prazna."),
            new AbstractMap.SimpleEntry<>(WRONG_LANGUAGE_TRANSLATION, "Jezik prijevoda se ne poklapa sa jezikom datoteke."),
            new AbstractMap.SimpleEntry<>(KEY_MISSING, "Ključ prijevoda nedostaje u nekoj od messages_*.properties datoteka."),
            new AbstractMap.SimpleEntry<>(DUPLICATE_VALUES, "Prijevod se duplicira na više ključeva.")
    );

    private static final Map<String, Language> LANGUAGES_MAP = Map.ofEntries(
            new AbstractMap.SimpleEntry<>("en", Language.ENGLISH),
            new AbstractMap.SimpleEntry<>("hr", Language.CROATIAN)
    );

    private Map<String, Map<String, Set<String>>> differencesMap = new HashMap<>();
    private Map<String, TranslationFileWrapper> translationsMap = new HashMap<>();
    private Set<String> keysCheckedForExistence = new TreeSet<>();
    private TranslationFolderWrapper coreWrapper;
    private boolean isValueBreaking;
    private String currentValue;
    private String currentKey;
    private File folder;
    private File srcFolder;
    private boolean removeIfKeyExistsInCore;
    private boolean removeIfKeyNotInUse;

    public TranslationFolderWrapper(
            final String folderPath,
            final boolean removeIfKeyExistsInCore,
            final boolean removeIfKeyNotInUse
    ) throws IOException {
        this.removeIfKeyExistsInCore = removeIfKeyExistsInCore;
        this.removeIfKeyNotInUse = removeIfKeyNotInUse;
        this.setFolder(folderPath);
    }

    public TranslationFolderWrapper(
            final String folderPath,
            final String coreFolderPath,
            final boolean removeIfKeyExistsInCore,
            final boolean removeIfKeyNotInUse
    ) throws IOException {
        this.removeIfKeyExistsInCore = removeIfKeyExistsInCore;
        this.removeIfKeyNotInUse = removeIfKeyNotInUse;
        this.coreWrapper = new TranslationFolderWrapper(coreFolderPath, removeIfKeyExistsInCore, removeIfKeyNotInUse);
        this.setFolder(folderPath);
    }

    private void resetValues() {
        this.isValueBreaking = false;
        this.currentValue = "";
        this.currentKey = "";
        this.keysCheckedForExistence = new TreeSet<>();
        this.differencesMap = new HashMap<>();
        this.translationsMap = new HashMap<>();
    }

    private void writeNormalizedTranslationStringsToFile(final File file, final String writeableString) throws IOException {
        try (final FileWriter fileWriter = new FileWriter(file, false)) {
            fileWriter.write(writeableString);
        }
    }

    private int getIndexOfLastSpace(final String string, final int indexLimit) {
        int stringLength = string.length();
        if (stringLength <= indexLimit) {
            return stringLength - 1;
        }
        return string.substring(0, indexLimit).lastIndexOf(' ');
    }

    private String getWriteableStringFromTranslationMap(final Map<String, String> map) {
        StringBuilder writeableString = new StringBuilder();
        String firstLevelKey = "";
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String key = normalizeKey(entry.getKey());
            String value = entry.getValue();
            String[] keyLevelIndentationStrings = key.split(KEY_LEVEL_INDENTATION_KEY);
            String currentFirstLevelKey = keyLevelIndentationStrings[0];

            if (!firstLevelKey.equalsIgnoreCase(currentFirstLevelKey)) {
                if (!"".equals(firstLevelKey)) {
                    writeableString.append("\n");
                }
                firstLevelKey = currentFirstLevelKey;
            }

            writeableString.append(key);
            writeableString.append(KEY_VALUE_SPLIT_REGEX);
            int keyWithSplitRegexLength = key.length() + KEY_VALUE_SPLIT_REGEX.length();
            int valueLength = value.length();

            if (keyWithSplitRegexLength + valueLength <= MAX_CHARACTERS_IN_LINE_COUNT) {
                writeableString.append(value);
                writeableString.append("\n");
                continue;
            }
            int charCountSpaceAvailable = MAX_CHARACTERS_IN_LINE_COUNT - keyWithSplitRegexLength;
            int indexOfLastSpace = getIndexOfLastSpace(value, charCountSpaceAvailable - 1);
            writeableString.append(value, 0, indexOfLastSpace);
            writeableString.append(" ");
            writeableString.append(LINE_BREAK_KEY);
            writeableString.append("\n");

            String remainingValue = value.substring(indexOfLastSpace + 1);
            while (true) {
                indexOfLastSpace = getIndexOfLastSpace(remainingValue, MAX_CHARACTERS_IN_LINE_COUNT - TAB.length() - 1);
                writeableString.append(TAB);
                String normalizedStringToWrite = remainingValue.substring(0, indexOfLastSpace + 1).trim();
                writeableString.append(normalizedStringToWrite);
                remainingValue = remainingValue.substring(indexOfLastSpace + 1);
                if ("".equals(remainingValue)) {
                    writeableString.append("\n");
                    break;
                }
                writeableString.append(" ");
                writeableString.append(LINE_BREAK_KEY);
                writeableString.append("\n");
            }
        }
        return writeableString.toString();
    }

    private void adjustTranslationFiles() {
        this.translationsMap = new HashMap<>();
        File[] folderFiles = this.folder.listFiles();
        if (folderFiles != null) {
            for (final File file : folderFiles) {
                final String fileName = file.getName();
                if (!file.isDirectory() && fileName.startsWith(TRANSLATION_FILE_NAME_STARTING_SUBSTRING)
                  && fileName.endsWith(TRANSLATION_FILE_NAME_ENDING_SUBSTRING)) {
                    final String langCode = fileName.substring(TRANSLATION_FILE_NAME_STARTING_SUBSTRING.length(),
                            fileName.indexOf(TRANSLATION_FILE_NAME_ENDING_SUBSTRING));
                    final TranslationFileWrapper translationFileWrapper = new TranslationFileWrapper(file);
                    this.translationsMap.put(langCode, translationFileWrapper);
                }
            }
        }
    }

    public void setFolder(final String folderPath) throws IOException {
        this.resetValues();
        File newFolder = new File(folderPath);
        if (!newFolder.exists()){
            throw new FileNotFoundException();
        }
        this.folder = newFolder;
        final String srcPath = folderPath.substring(0, folderPath.indexOf(SRC_FOLDER_SUBSTRING) + SRC_FOLDER_SUBSTRING.length());
        this.srcFolder = new File(srcPath);

        this.adjustTranslationFiles();
        this.applyChangesInternally();
        this.configureDiffsInternally();
    }

    private void applyChangesInternally() throws IOException {
        for (final Map.Entry<String, TranslationFileWrapper> translationFileWrapperEntry : this.translationsMap.entrySet()) {
            TranslationFileWrapper translationFileWrapper = translationFileWrapperEntry.getValue();
            final String langCode = translationFileWrapperEntry.getKey();
            final Map<String, String> normalizedTranslationStrings = this.normalizeTranslationStrings(langCode);
            final String writeableString = getWriteableStringFromTranslationMap(normalizedTranslationStrings);
            translationFileWrapper.setTranslations(normalizedTranslationStrings);
            translationFileWrapper.setWriteableString(writeableString);
            translationFileWrapperEntry.setValue(translationFileWrapper);
        }
    }

    public void applyChangesOnDisk() throws IOException {
        for (final Map.Entry<String, TranslationFileWrapper> translationFileWrapperEntry : this.translationsMap.entrySet()) {
            final TranslationFileWrapper translationFileWrapper = translationFileWrapperEntry.getValue();
            final String writeableString = translationFileWrapper.getWriteableString();
            final File translationFile = translationFileWrapper.getFile();
            writeNormalizedTranslationStringsToFile(translationFile, writeableString);
        }
    }

    private String normalizeKey(final String key) {
        String[] keyLevelIndentationStrings = key.split(KEY_LEVEL_INDENTATION_KEY);
        String currentFirstLevelKey = keyLevelIndentationStrings[0];
        String otherLevelKeys = String.join(".", Arrays.asList(keyLevelIndentationStrings).subList(1, keyLevelIndentationStrings.length));
        return currentFirstLevelKey + "." + otherLevelKeys;
    }

    private Map<String, String> normalizeTranslationStrings(final String langCode) throws IOException {
        final TranslationFileWrapper translationFileWrapper = this.translationsMap.get(langCode);
        final File file = translationFileWrapper.getFile();
        final Path path = Paths.get(file.toURI());
        List<String> normalizedTranslationStrings = Files.readAllLines(path).stream()
                .filter(line -> !line.isBlank() && !line.startsWith(TRANSLATION_COMMENT_KEY))
                .map(String::trim)
                .collect(Collectors.toList());

        Map<String, String> translationsMapInner = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        normalizedTranslationStrings.forEach(line -> {
            if (isValueBreaking) {
                currentValue = currentValue.substring(0, currentValue.length() - 1);
                currentValue = currentValue.trim();
                currentValue += " " + line;
            } else {
                String[] paramSplit = line.split(KEY_VALUE_SPLIT_REGEX);
                currentKey = normalizeKey(paramSplit[0]);
                currentValue = String.join("", Arrays.asList(paramSplit).subList(1, paramSplit.length));
            }

            isValueBreaking = currentValue.endsWith(LINE_BREAK_KEY);
            if (!isValueBreaking) {
                translationsMapInner.put(currentKey, currentValue);
            }
        });
        return translationsMapInner;
    }

    private static boolean isStringUpperCase(final String str){
        char[] charArray = str.toCharArray();
        for (char ch : charArray) {
            if (!Character.isWhitespace(ch) && !Character.isUpperCase(ch)) {
                return false;
            }
        }
        return true;
    }

    private List<File> listFilesInternal(final File folder) {
        if (folder == null) return Collections.emptyList();
        File[] files = folder.listFiles();
        if (files == null) return Collections.emptyList();
        return Arrays.stream(files).collect(Collectors.toList());
    }

    private boolean stringExistsInProject(final String str, final File folder) throws IOException {
        if (IGNORABLE_ENDING_SUBSTRINGS_TRANSLATION_KEYS.stream().anyMatch(str::endsWith)) return true;
        if (IGNORABLE_STARTING_SUBSTRINGS_TRANSLATION_KEYS.stream().anyMatch(str::startsWith)) return true;
        List<File> files = listFilesInternal(folder);

        for (final File fileEntry : files) {
            if (fileEntry.isDirectory()) {
                if (stringExistsInProject(str, fileEntry)) {
                    return true;
                }
            } else {
                final String fileName = fileEntry.getName();
                if (IGNORABLE_ENDING_DIFF_FILE_SUBSTRINGS_TRANSLATION_KEYS.stream().anyMatch(fileName::endsWith)) {
                    final String fileContent = new String(Files.readAllBytes(Paths.get(fileEntry.toURI())));
                    if (fileContent.contains(str)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private void execAppropriateChangeForDiff(final String diffKey, final String translationKey) {
        if ((this.removeIfKeyExistsInCore && KEY_IN_CORE.equals(diffKey))
                || (this.removeIfKeyNotInUse && KEY_NOT_IN_USE.equals(diffKey))) {
            for (Map.Entry<String, TranslationFileWrapper> entry : translationsMap.entrySet()) {
                TranslationFileWrapper translationFileWrapper = entry.getValue();
                translationFileWrapper.getTranslations().remove(translationKey);
                final String writeableString = getWriteableStringFromTranslationMap(translationFileWrapper.getTranslations());
                translationFileWrapper.setWriteableString(writeableString);
                entry.setValue(translationFileWrapper);
            }
        }
    }

    private void putToMapWhereValuesAreLists(final Map<String, Set<String>> map, final String key, final String newValue) {
        Set<String> listValue = map.get(key);
        if (listValue == null) {
            listValue = new TreeSet<>();
        }
        listValue.add(newValue);
        map.put(key, listValue);
    }

    private void configureDiffsInternally() throws IOException {
        Language[] languageList = new ArrayList<>(LANGUAGES_MAP.values()).toArray(new Language[0]);
        final LanguageDetector languageDetector = LanguageDetectorBuilder.fromLanguages(languageList).build();
        for (final Map.Entry<String, TranslationFileWrapper> translationFileWrapperEntry : this.translationsMap.entrySet()) {
            final String langCode = translationFileWrapperEntry.getKey();
            Map<String, Set<String>> differencesByTypeOfDiff = new HashMap<>();

            final TranslationFileWrapper translationFileWrapper = translationFileWrapperEntry.getValue();
            final Map<String, String> translations = translationFileWrapper.getTranslations();

            for (final Map.Entry<String, String> translationsEntry : translations.entrySet()) {
                final String translationKey = translationsEntry.getKey();
                if (!this.keysCheckedForExistence.contains(translationKey)) {
                    boolean stringExistsInProject = this.stringExistsInProject(translationKey, this.srcFolder);
                    boolean stringExistsInCore = false;
                    if (this.coreWrapper != null) {
                        TranslationFileWrapper coreTranslationFileWrapper = this.coreWrapper.translationsMap.get(langCode);
                        final String coreTranslationForGivenKey = coreTranslationFileWrapper.getTranslations().get(translationKey);
                        stringExistsInCore = coreTranslationForGivenKey != null;
                    }
                    if (stringExistsInCore) {
                        this.putToMapWhereValuesAreLists(differencesByTypeOfDiff, KEY_IN_CORE, translationKey);
                    } else if (!stringExistsInProject) {
                        this.putToMapWhereValuesAreLists(differencesByTypeOfDiff, KEY_NOT_IN_USE, translationKey);
                    }
                }
                this.keysCheckedForExistence.add(translationKey);
                final String translationValue = translationsEntry.getValue().trim();

                handleDuplicateValues(differencesByTypeOfDiff, translations, translationKey, translationValue);

                if ("".equals(translationValue)) {
                    this.putToMapWhereValuesAreLists(differencesByTypeOfDiff, VALUE_EMPTY, translationKey);
                } else {
                    final String detectedLangCode = languageDetector.detectLanguageOf(translationValue).getIsoCode639_1().toString();
                    if (!langCode.equals(detectedLangCode)) {
                        final Double languageProbability = languageDetector.computeLanguageConfidenceValues(translationValue).get(LANGUAGES_MAP.get(langCode));
                        if (!isStringUpperCase(translationValue) && (languageProbability == null || languageProbability < MINIMUM_LANGUAGE_PROBABILITY)) {
                            this.putToMapWhereValuesAreLists(differencesByTypeOfDiff, WRONG_LANGUAGE_TRANSLATION, translationKey);
                        }
                    }
                }

                for (final Map.Entry<String, TranslationFileWrapper> translationFileWrapperEntryInner : this.translationsMap.entrySet()) {
                    final String langCodeInner = translationFileWrapperEntryInner.getKey();
                    final TranslationFileWrapper translationFileWrapperInner = translationFileWrapperEntryInner.getValue();
                    final Map<String, String> translationsInner = translationFileWrapperInner.getTranslations();
                    if (!langCode.equals(langCodeInner)) {
                        final String translationValueInner = translationsInner.get(translationKey);
                        if (translationValueInner == null) {
                            this.putToMapWhereValuesAreLists(differencesByTypeOfDiff, KEY_MISSING, translationKey);
                        }
                    }
                }
            }

            this.differencesMap.put(langCode, differencesByTypeOfDiff);
        }

        this.differencesMap.forEach((langCode, diffSetMap) ->
                diffSetMap.forEach((diffKey, translationKeySet) ->
                        translationKeySet.forEach(translationKey -> this.execAppropriateChangeForDiff(diffKey, translationKey))
                )
        );
    }

    public void displayDiffs() {
        System.out.println("Displaying diffs...");
        System.out.println("***********************************");
        differencesMap.forEach((langCode, diffsMap) -> {
            System.out.println(LANGUAGES_MAP.get(langCode).toString().toUpperCase(Locale.ROOT));
            diffsMap.forEach((diffKey, translationKeySet) -> {
                System.out.println("  - " + DIFFERENCES_MAP.get(diffKey));
                translationKeySet.forEach(translationKey -> System.out.println("       " + translationKey));
            });
        });
        System.out.println("***********************************");
    }

    private void handleDuplicateValues(
            final  Map<String, Set<String>> differencesByTypeOfDiff,
            final Map<String, String> translations,
            final String translationKey,
            final String translationValue
    ) {
        var isDuplicateEntry = translations.entrySet()
                .stream()
                .anyMatch(entry -> {
                    var key = entry.getKey();
                    var value = entry.getValue();
                    return !key.equals(translationKey) && translationValue.equals(value);
                });
        if (isDuplicateEntry) {
            this.putToMapWhereValuesAreLists(differencesByTypeOfDiff, DUPLICATE_VALUES, translationValue);
        }
    }
}
