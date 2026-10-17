# R8 shrinks and optimises the release build but keeps every name, so stack
# traces stay readable and the build stays reproducible (IzzyOnDroid's
# "optimize, but do not obfuscate").
-dontobfuscate

# Reached from the native side (AIScatcherNDK.cpp) by name: the static
# callbacks onMessage/onStatus/onUpdate/onError and the Statistics fields.
-keep class com.jvdegithub.aiscatcher.AisCatcherJava { *; }
-keep class com.jvdegithub.aiscatcher.AisCatcherJava$* { *; }
