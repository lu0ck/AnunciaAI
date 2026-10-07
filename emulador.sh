#!/usr/bin/env bash
# ═════════════════════════════════════════════════════════════════
#  AnunciaAI — emulador.sh
#  Liga/desliga o emulador Android (AVD "anunciaai") no seu desktop.
#  Uso:  ./emulador.sh start     (abre janela no seu monitor)
#        ./emulador.sh stop      (desliga limpo)
#        ./emulador.sh status    (estado atual)
# ═════════════════════════════════════════════════════════════════
PROJ="/mnt/SSD_Games_2/Projetos/AnunciaAI"
EMU="$PROJ/android-sdk/emulator/emulator"
ADB="$PROJ/android-sdk/platform-tools/adb"
AVD="anunciaai"

case "$1" in
  start)
    if $ADB devices 2>/dev/null | grep -q "emulator-"; then
        echo "Emulador já está rodando."
        exit 0
    fi
    echo "Subindo emulador (AVD $AVD) — janela abre no seu monitor..."
    # -no-snapshot: boot limpo toda vez (evita corrupção de state no NTFS)
    nohup "$EMU" -avd "$AVD" -gpu auto -no-snapshot -no-boot-anim \
        > /tmp/emulador-anunciaai.log 2>&1 &
    echo "Aguardando boot..."
    $ADB wait-for-device 2>/dev/null
    for i in $(seq 1 90); do
        BOOT=$($ADB shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')
        [[ "$BOOT" == "1" ]] && { echo "✅ Emulador pronto (PID $(pgrep -f 'emulator.*anunciaai' | head -1))."; exit 0; }
        sleep 2
    done
    echo "⚠️ Boot demorou >180s — confira /tmp/emulador-anunciaai.log"
    ;;
  stop)
    $ADB -s emulator-5554 emu kill 2>/dev/null && echo "Emulador desligando..." || { pkill -f "emulator.*-avd anunciaai" 2>/dev/null; echo "Emulador morto (kill)."; }
    ;;
  status)
    if $ADB devices 2>/dev/null | grep -q "emulator-"; then
        echo "RODANDO — device: $($ADB devices | grep emulator- | awk '{print $1}')"
        $ADB shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' | grep -q 1 && echo "Boot: completo" || echo "Boot: em andamento"
    else
        echo "PARADO"
    fi
    ;;
  *)
    echo "Uso: ./emulador.sh {start|stop|status}"
    ;;
esac
