// Build file a livello di root - dichiara i plugin ma non li applica direttamente.
//
// Versioni allineate ad Android Studio Narwhal+/AGP 9.x (stesso stack di MyVote_by_mcc):
//  - AGP 9.1.1
//  - Kotlin 2.2.10 (+ plugin Compose dedicato al posto di kotlinCompilerExtensionVersion)
//  - KSP 2.3.2 (deve corrispondere alla versione di Kotlin)
plugins {
    id("com.android.application") version "9.1.1" apply false
    id("org.jetbrains.kotlin.android") version "2.2.10" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
    id("com.google.devtools.ksp") version "2.3.2" apply false
}
