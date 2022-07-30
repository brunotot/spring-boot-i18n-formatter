# spring-boot-i18n-formatter

## Build and run

### Summary
`build` folder contains shell scripts for building and running the application.
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