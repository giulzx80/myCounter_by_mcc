# Add project specific ProGuard rules here.
# Conserva i nomi delle entity Room (necessario per il funzionamento del codegen)
-keep class com.mcc.mycounter.data.entities.** { *; }
-keep class com.mcc.mycounter.data.dao.** { *; }
