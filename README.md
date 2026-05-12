# RemoteControlApp (Android 15)

Questa è un'applicazione Android progettata per funzionare come un telecomando universale tramite **Infrarossi (IR)**, senza necessità di connessione Wi-Fi.

## Caratteristiche
- **No Wi-Fi**: Utilizza il sensore IR del telefono.
- **Android 15 Ready**: Configurato per le ultime API.
- **Interfaccia Semplice**: Pulsanti Power e Volume.

## Come compilare l'APK
Poiché non è possibile compilare direttamente in questo ambiente senza l'Android SDK completo, segui questi passaggi:

1. **Installa Android Studio** (se non lo hai già).
2. **Apri il progetto**: Seleziona la cartella `RemoteControlApp`.
3. **Build**: Vai su `Build > Build Bundle(s) / APK(s) > Build APK(s)`.
4. **Installa**: Troverai l'APK nella cartella `app/build/outputs/apk/debug/app-debug.apk`.

## Nota Importante sull'Hardware
Perché l'app funzioni "realmente", il tuo smartphone **deve avere un sensore IR integrato** (comune su telefoni Xiaomi, vecchi Samsung/LG). Se il tuo telefono non ha il sensore IR, l'app mostrerà un avviso.
