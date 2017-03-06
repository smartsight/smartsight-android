<h1 align="center">
  <img src="https://github.com/smartsight/smartsight-art/raw/master/logo/variants/android/logo.png" alt="SmartSight Android app">
</h1>

> SmartSight Android app in Kotlin

[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](LICENSE.md)

## Development setup

You will need to define `SM_SERVER_URL` in the app [`build.gradle`](app/build.gradle). This constant is related to the address of the running [SmartSight API](https://github.com/smartsight/smartsight-api).

```groovy
android {
    buildTypes {
        debug {
            buildConfigField "String", "SM_SERVER_URL", "\"[address of the server]\""
        }
    }
}
```

## License

GPL © SmartSight
