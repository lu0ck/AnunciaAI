#!/usr/bin/env bash
# ═════════════════════════════════════════════════════════════════
#  AnunciaAI — testar.sh
#  Um comando: builda o APK de debug, instala no emulador e abre o app.
#  Uso:  ./testar.sh            (build + instala + abre)
#        ./testar.sh --no-build (só reinstala e abre — pula o gradle)
#  Requisitos: emulador AVD "anunciaai" rodando (./emulador.sh start).
# ═════════════════════════════════════════════════════════════════
set -e
PROJ="/mnt/SSD_Games_2/Projetos/AnunciaAI"
ADB="$PROJ/android-sdk/platform-tools/adb"
APK_DEBUG="$PROJ/app/build/outputs/apk/debug/app-debug.apk"
ATIVIDADE="br.com.anunciaai/.MainActivity"

cd "$PROJ"

# 1. build (pulável com --no-build)
if [[ "$1" != "--no-build" ]]; then
    echo "── [1/3] Buildando APK debug..."
    GRADLE_USER_HOME=$PROJ/.gradle-home ANDROID_HOME=$PROJ/android-sdk \
        JAVA_HOME=$PROJ/jdk-17.0.20.1+1 \
        ./gradle-8.10.2/bin/gradle assembleDebug --console=plain -q
else
    echo "── [1/3] Build pulado (--no-build)"
fi

# 2. espera o emulador responder
echo "── [2/3] Conectando ao emulador..."
$ADB wait-for-device 2>/dev/null || { echo "ERRO: emulador não responde — rode ./emulador.sh start"; exit 1; }
# espera o boot completar (prop sys.boot_completed=1)
for i in $(seq 1 60); do
    BOOT=$($ADB shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')
    [[ "$BOOT" == "1" ]] && break
    sleep 2
done
[[ "$BOOT" == "1" ]] || { echo "ERRO: emulador não terminou o boot (120s)"; exit 1; }

# 3. instala e abre
echo "── [3/3] Instalando e abrindo o app..."
$ADB install -r "$APK_DEBUG" || { echo "ERRO no adb install"; exit 1; }
$ADB shell am start -n "$ATIVIDADE"
echo "✅ App aberto no emulador — testável na janela."
