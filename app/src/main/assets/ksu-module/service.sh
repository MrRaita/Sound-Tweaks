#!/system/bin/sh
# Açılışta ses deposunun sahipliğini/SELinux bağlamını düzelt
D=/data/system/soundmod
if [ -d "$D" ]; then
  chown -R system:system "$D"
  restorecon -R "$D"
fi
