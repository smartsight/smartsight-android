<h1 align="center">
  <img src="https://github.com/smartsight/smartsight-art/raw/master/logo/variants/android/logo.png" alt="SmartSight Android app">
</h1>

> SmartSight Android app in Kotlin

[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](LICENSE)

## Development setup

You will need to define the server URL in the project [`build.gradle`](build.gradle) file. This constant corresponds to the address of the running [SmartSight API](https://github.com/smartsight/smartsight-api) ([see the default setup](https://github.com/smartsight/smartsight-api#development-setup)).

```groovy
buildscript {
    ext.serverUrl = [
        debug: '[address of the server in debug mode]',
        release: '[address of the server in release mode]'
    ]

    // ...
}
```

Sync your project and you're ready to go.

## License

GPL © SmartSight
