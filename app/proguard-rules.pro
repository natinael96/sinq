# kotlinx.serialization — the whole content pipeline (assets/content/*.json)
# is @Serializable models in com.agpeya.app. The library ships consumer rules,
# but keep our serializers explicitly so a minified build can never lose them
# (a broken release build would crash on first launch while parsing assets).
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers @kotlinx.serialization.Serializable class com.agpeya.app.** {
    *** Companion;
}
-if @kotlinx.serialization.Serializable class com.agpeya.app.**
-keepclasseswithmembers class com.agpeya.app.<1>$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class com.agpeya.app.**$$serializer {
    *** INSTANCE;
}
