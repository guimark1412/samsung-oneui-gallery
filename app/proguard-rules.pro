# Regras R8 do projeto (Compose, Coil e AndroidX já trazem regras próprias)
# Mantém otimização e remoção de código morto, preservando nomes de classes para reduzir falsos positivos no Play Protect e MIUI Security.
-dontobfuscate
-keepattributes *Annotation*,SourceFile,LineNumberTable
