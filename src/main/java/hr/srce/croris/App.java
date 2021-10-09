package hr.srce.croris;

import java.io.FileNotFoundException;
import java.util.Arrays;

public class App {
    private static final String CMD_ARG_PATH_KEY = "-path=";
    private static final String CMD_ARG_CORE_PATH_KEY = "-core=";

    private static String getFolderPathFromCmdArgs(final String[] args, final String key) throws FileNotFoundException {
        return Arrays.stream(args)
                .filter(cmdArg -> cmdArg.startsWith(key))
                .findFirst()
                .orElseThrow(FileNotFoundException::new)
                .substring(key.length());
    }

    public static void main(final String[] args) throws Exception {
        final String folderPath = getFolderPathFromCmdArgs(args, CMD_ARG_PATH_KEY);
        final String coreMessagesFolderPath = getFolderPathFromCmdArgs(args, CMD_ARG_CORE_PATH_KEY);
        TranslationFolderWrapper translationFolderWrapper = new TranslationFolderWrapper(folderPath, coreMessagesFolderPath);
        translationFolderWrapper.displayDiffs();
        translationFolderWrapper.applyChangesOnDisk();
    }
}
