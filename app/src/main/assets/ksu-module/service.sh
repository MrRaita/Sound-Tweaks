#!/system/bin/sh
# Açılışta SoundMod ses deposunun sahipliğini ve SELinux bağlamını (system_file) yeniden uygula.
D=/data/system/soundmod
until [ "$(getprop sys.boot_completed)" = "1" ]; do sleep 2; done
if [ -d "$D" ]; then
  chown -R system:system "$D"
  chmod -R u+rwX,go+rX "$D"
  chcon -R u:object_r:system_file:s0 "$D"
fi
