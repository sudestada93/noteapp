#!/usr/bin/env bash
# Prueba de humo en emulador: instala, recorre pantallas, saca capturas y falla si la app se cierra.
set -u
APK="$1"
OUT=shots
mkdir -p "$OUT"
PKG=com.libreta.app

shot() { sleep "${2:-2}"; adb exec-out screencap -p > "$OUT/$1.png"; echo "captura: $1"; }

# Toca el primer nodo cuyo text o content-desc contenga $1
tap_text() {
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb pull /sdcard/ui.xml ui.xml >/dev/null 2>&1
  local b
  b=$(python3 - "$1" <<'PY'
import sys, re
t = sys.argv[1].lower()
xml = open('ui.xml', encoding='utf-8').read()
for m in re.finditer(r'<node [^>]*>', xml):
    n = m.group(0)
    txt = (re.search(r' text="([^"]*)"', n) or [None, ''])[1].lower()
    desc = (re.search(r'content-desc="([^"]*)"', n) or [None, ''])[1].lower()
    if t in txt or t in desc:
        x1, y1, x2, y2 = map(int, re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', n).groups())
        print((x1 + x2) // 2, (y1 + y2) // 2)
        break
PY
)
  if [ -n "$b" ]; then adb shell input tap $b; echo "tap '$1' -> $b"; return 0; fi
  echo "no encontrado: '$1'"; return 1
}

crashed() { adb logcat -d | grep -E "FATAL EXCEPTION|AndroidRuntime: Process: $PKG" >/dev/null; }

adb install -r "$APK"
adb logcat -c
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell am start -n $PKG/.MainActivity
shot 01-bienvenida 8

tap_text "Tu nombre" && adb shell input text "Ivan" && adb shell input keyevent 4
tap_text "EMPEZAR"
sleep 3
tap_text "Permitir" || tap_text "Allow" || true
shot 02-inicio 4
adb shell input swipe 540 1700 540 600 400
shot 03-inicio-widgets 2
adb shell input swipe 540 1700 540 400 400
shot 04-inicio-libretas 2

tap_text "NOTAS"
shot 05-notas 3
tap_text "Empezá acá"
shot 06-editor 4
adb shell input swipe 540 1600 540 700 400
shot 07-editor-bloques 2
adb shell input keyevent 4; sleep 1; adb shell input keyevent 4
sleep 2
tap_text "TAREAS"
shot 08-tareas 3
tap_text "MÁS" || tap_text "Más"
shot 09-mas 3
tap_text "Calendario"
shot 10-calendario 3
adb shell input keyevent 4
tap_text "INICIO"
sleep 2
tap_text "Crear"
shot 11-crear 3
adb shell input keyevent 4
tap_text "Buscar en"
adb shell input text "libros"
shot 12-busqueda 3
adb shell input keyevent 4; adb shell input keyevent 4
adb shell cmd uimode night yes
sleep 3
shot 13-inicio-pizarra 3
tap_text "NOTAS"
shot 14-notas-pizarra 3
adb shell cmd uimode night no

adb logcat -d > "$OUT/logcat.txt"
if crashed; then
  echo "::error::La app se cerró inesperadamente"
  adb logcat -d | grep -A40 "FATAL EXCEPTION" | head -80
  exit 1
fi
echo "Sin cierres inesperados ✅"
