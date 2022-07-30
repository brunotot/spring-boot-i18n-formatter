package hr.srce.croris;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

public class App {
    private static final String TRUE = Boolean.TRUE.toString();
    private static final String FALSE = Boolean.FALSE.toString();
    private static final String CMD_ARG_PATH_KEY = "-path=";
    private static final String CMD_ARG_CORE_PATH_KEY = "-core=";
    private static final String CMD_ARG_REMOVE_IF_KEY_EXISTS_IN_CORE_KEY = "-removeIfKeyExistsInCore=";
    private static final String CMD_ARG_REMOVE_IF_KEY_NOT_IN_USE_KEY = "-removeIfKeyNotInUse=";
    private static final String CMD_ARG_APPLY_CHANGES_ON_DISK_KEY = "-applyChangesOnDisk=";

    private static Optional<String> getCmdArg(
            final String[] args,
            final String key
    ) {
        return Arrays.stream(args)
                .filter(cmdArg -> cmdArg.startsWith(key))
                .map(str -> str.substring(key.length()))
                .findFirst();
    }

    private static String getCmdArg(
            final String[] args,
            final String key,
            final String defaultValue
    ) {
        return getCmdArg(args, key).orElse(defaultValue);
    }

    public static void main(final String[] args) throws IOException {
        final boolean applyChangesOnDisk = getCmdArg(args, CMD_ARG_APPLY_CHANGES_ON_DISK_KEY, TRUE).equals(TRUE);
        final boolean removeIfKeyExistsInCore = getCmdArg(args, CMD_ARG_REMOVE_IF_KEY_EXISTS_IN_CORE_KEY, TRUE).equals(TRUE);
        final boolean removeIfKeyNotInUse = getCmdArg(args, CMD_ARG_REMOVE_IF_KEY_NOT_IN_USE_KEY, FALSE).equals(TRUE);
        final String folderPath = getCmdArg(args, CMD_ARG_PATH_KEY).orElseThrow(FileNotFoundException::new);
        final String coreMessagesFolderPath = getCmdArg(args, CMD_ARG_CORE_PATH_KEY).orElseThrow(FileNotFoundException::new);
        TranslationFolderWrapper translationFolderWrapper = new TranslationFolderWrapper(
                folderPath,
                coreMessagesFolderPath,
                removeIfKeyExistsInCore,
                removeIfKeyNotInUse
        );
        translationFolderWrapper.displayDiffs();
        if (applyChangesOnDisk) {
            translationFolderWrapper.applyChangesOnDisk();
        }
    }
}
