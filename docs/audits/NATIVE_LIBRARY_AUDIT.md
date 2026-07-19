# Native Library Audit

| Native Library | Originating Gradle Dependency | Current Version | Latest Stable Version | 16 KB Compatible? |
| :--- | :--- | :--- | :--- | :--- |
| `libsqlcipher.so` | `net.zetetic:sqlcipher-android` | 4.6.1 | 4.9.0 | YES (updated) |
| `libdatastore_shared_counter.so`| `androidx.datastore:datastore-preferences` | 1.1.1 | 1.1.7 | YES (updated) |
| `libimage_processing_util_jni.so`| `androidx.camera:camera-core` | 1.3.3 | 1.4.0 | YES (updated) |

*Note: The native libraries above were initially non-compliant in their older versions. They have been updated to the latest stable versions which include 16 KB page size alignment.*
