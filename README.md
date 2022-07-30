# spring-boot-i18n-formatter

## Build and run

### Summary
`build` folder contains shell scripts for building and running the application with `args.properties` configuration file.
The application first has to be built to be able to run.

### Terminal commands
```shell
sh build.sh
sh run.sh
```

### Command line params list
| Key                     | Type      | Default | Example                                                                  | Required           |
|-------------------------|-----------|---------|--------------------------------------------------------------------------|--------------------|
| core                    | `String`  | `null`  | /home/crorisvanjski4/Desktop/dev<br/>/core/src/main/resources/i18n-core  | :heavy_check_mark: |
| path                    | `String`  | `null`  | /home/crorisvanjski4/Desktop/dev<br/>/croris-ppg/src/main/resources/i18n | :heavy_check_mark: |
| removeIfKeyNotInUse     | `boolean` | `false` | true                                                                     | :x:                |
| removeIfKeyExistsInCore | `boolean` | `true`  | true                                                                     | :x:                |

## Output

Output can contain 6 types of data for each of the existing languages.

1. `KEY_IN_CORE` - Key already exists in Core app.
2. `KEY_NOT_IN_USE` - Key is not in use.
3. `VALUE_EMPTY` - Translation is empty.
4. `WRONG_LANGUAGE_TRANSLATION` - Entry language is not the same as the entry file's language.
5. `KEY_MISSING` - Translation key is missing in some of the messages_*.properties files.
6. `DUPLICATE_VALUES` - Translation is duplicating on multiple keys.
