package hr.srce.croris;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class App {
    private static final String CMD_ARG_PATH_KEY = "-path=";
    private static final String TRANSLATION_COMMENT_KEY = "#";
    private static final String LINE_BREAK_KEY = "\\";
    private static final String KEY_VALUE_SPLIT_REGEX = "=";
    private static final String KEY_LEVEL_INDENTATION_KEY = "\\.";
    private static final Integer MAX_CHARACTERS_IN_LINE_COUNT = 120;
    private static final String TAB = "  ";

    private static String currentKey = "";
    private static String currentValue = "";
    private static boolean isValueBreaking = false;

    private static Path getFilePathFromCmdArgs(final String[] args) throws FileNotFoundException {
        final String filePath = Arrays.stream(args)
                .filter(cmdArg -> cmdArg.startsWith(CMD_ARG_PATH_KEY))
                .findFirst()
                .orElseThrow(FileNotFoundException::new)
                .substring(CMD_ARG_PATH_KEY.length());
        File file = new File(filePath);
        if (!file.exists()){
            throw new FileNotFoundException();
        }
        return Paths.get(file.toURI());
    }

    private static Map<String, String> normalizeTranslationStrings(final Path filePath) throws IOException {
        List<String> normalizedTranslationStrings = Files.readAllLines(filePath).stream()
                .filter(line -> !line.isBlank() && !line.startsWith(TRANSLATION_COMMENT_KEY))
                .map(String::trim)
                .collect(Collectors.toList());

        Map<String, String> translationsMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
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
                translationsMap.put(currentKey, currentValue);
            }
        });
        return translationsMap;
    }

    private static String normalizeKey(final String key) {
        String[] keyLevelIndentationStrings = key.split(KEY_LEVEL_INDENTATION_KEY);
        String currentFirstLevelKey = keyLevelIndentationStrings[0];
        String otherLevelKeys = String.join(".", Arrays.asList(keyLevelIndentationStrings).subList(1, keyLevelIndentationStrings.length));
        return currentFirstLevelKey + "." + otherLevelKeys;
    }

    private static int getIndexOfLastSpace(final String string, final int indexLimit) {
        int stringLength = string.length();
        if (stringLength <= indexLimit) {
            return stringLength - 1;
        }
        return string
                .substring(0, indexLimit)
                .lastIndexOf(' ');
    }

    private static String getWriteableStringFromTranslationMap(final Map<String, String> map) {
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
            } else {
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
        }
        return writeableString.toString();
    }

    private static void writeNormalizedTranslationStringsToFile(final Path filePath, final String writeableString) {
        try (FileWriter fileWriter = new FileWriter(String.valueOf(filePath), false)) {
            fileWriter.write(writeableString);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(final String[] args) throws Exception {
        final Path filePath = getFilePathFromCmdArgs(args);
        final Map<String, String> normalizedTranslationStrings = normalizeTranslationStrings(filePath);
        final String writeableString = getWriteableStringFromTranslationMap(normalizedTranslationStrings);
        writeNormalizedTranslationStringsToFile(filePath, writeableString);
    }
}
