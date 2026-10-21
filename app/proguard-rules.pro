# kotlinx.serialization — the whole content pipeline (assets/content/*.json)
# is @Serializable models in com.sinq.app. The library ships consumer rules,
# but keep our serializers explicitly so a minified build can never lose them
# (a broken release build would crash on first launch while parsing assets).
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers @kotlinx.serialization.Serializable class com.sinq.app.** {
    *** Companion;
}
-if @kotlinx.serialization.Serializable class com.sinq.app.**
-keepclasseswithmembers class com.sinq.app.<1>$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class com.sinq.app.**$$serializer {
    *** INSTANCE;
}

# DataStore Preferences stores string-sets (bookmarks, streak records, scheduled
# ids…) through a bundled protobuf-lite. Its generated messages resolve fields
# reflectively by their original name (e.g. StringSet.strings_); R8 field
# renaming makes that lookup fail at runtime ("Field strings_ ... not found").
# Keep the datastore tree — including the repackaged protobuf — intact.
-keep class androidx.datastore.** { *; }
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

# Room Database, DAOs, and Entities
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class com.sinq.app.data.JournalDatabase_Impl { *; }
-keep class com.sinq.app.data.CorpusDatabase_Impl { *; }

# Room Entities & SQLite FTS Models
-keep class com.sinq.app.data.CorpusFtsEntity { *; }
-keep class com.sinq.app.data.CorpusVerseEntity { *; }
-keep class com.sinq.app.data.HighlightEntity { *; }
-keep class com.sinq.app.data.HabitLogEntity { *; }
-keep class com.sinq.app.data.JournalEntry { *; }
-keep class com.sinq.app.data.BackupRepository$* { *; }

