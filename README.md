# SoundMod

Sistem seslerini (klavye, dokunma, ekran görüntüsü, kilit, şarj…) istediğin sesle değiştiren
**LSPosed/Vector modülü + uygulama**. Material 3 Expressive arayüz, root ile çalışır.

Hedef: Nothing Phone (4a) · Nothing OS 4.1 · Android 16 (diğer AOSP tabanlı ROM'lar da denenebilir).

## Nasıl çalışır?

| Ses | Yöntem |
|---|---|
| Klavye tuşları, dokunma tıkı | `system_server` içinde `SoundEffectsHelper.getResourceFilePath` hook'u → `/data/system/soundmod/override/<StokAdı>.*` varsa onu döndürür |
| Ekran görüntüsü | SystemUI içinde `ScreenshotSoundProviderImpl.getScreenshotSound` hook'u |
| Kilit, kilit açma, güvenilir cihaz, düşük pil, şarj, kablosuz şarj, dock | Root ile `settings put global <anahtar> <dosya yolu>` (hook gerekmez) |

* Uygulama `/product/media/audio` (ve diğer bilinen dizinleri) tarar, sesleri listeler, önizletir.
* Sesi **sistem seslerinden** ya da **kendi dosyandan** seçebilirsin. Kendi dosyaların WAV'a çevrilir,
  istenirse ortak seviyeye normalize edilir (ffmpeg gerekmez).
* Orijinal dosyalara dokunulmaz; "Varsayılana dön" ya da modülü kapatmak her şeyi eski haline getirir.

## Kurulum

1. Repoyu GitHub'a it → **Actions → Build APK** çalışır → `SoundMod-apk` artifact'ındaki APK'yı kur.
2. Uygulamayı aç, root iznini ver.
3. **Vector**'da SoundMod'u etkinleştir, kapsam: **Sistem Çerçevesi** ve **Sistem Arayüzü**. Cihazı yeniden başlat.
4. Uygulamada Ayarlar sekmesi: "Vector modülü" kartı yeşil olmalı.
5. Sesler sekmesinden bir kalem seç → sistem sesi / kendi dosyan.

Not: Klavye/dokunma sesi duymak için *Ayarlar → Ses → Dokunma sesleri* açık olmalı.

### SELinux yardımcı modülü (isteğe bağlı)

SystemUI'ın `/data/system/soundmod` altındaki dosyaları okuyamadığı durumlarda (kilit sesi/ekran görüntüsü
özel dosyayla çalmıyorsa) Ayarlar sekmesindeki **Kur** düğmesi küçük bir KernelSU/Magisk/APatch modülü kurar
(`sepolicy.rule`). Kurduktan sonra cihazı yeniden başlat. Klavye sesleri (system_server) buna ihtiyaç duymaz.

## Derleme

* Yerelde: JDK 17+, `gradle wrapper --gradle-version 9.5.0` (bir kez), sonra `./gradlew assembleRelease`.
* CI: `.github/workflows/build.yml` wrapper'ı kendisi üretir. `gradle-wrapper.jar` ikili dosya olduğu için repoda yok.
* Sürüm: `versionName` `app/build.gradle.kts` içinde, `versionCode` CI çalışma numarasından gelir.
* Sürüm APK'sı debug anahtarıyla imzalanır (doğrudan kurulabilir).

## Teknik notlar

* libxposed **API 101** (`minApiVersion=targetApiVersion=101`) → Vector 2.1 ve sonrası.
* Giriş sınıfı `dev.soundmod.nothing.hook.SoundModule`; meta veriler `src/main/resources/META-INF/xposed/`.
* Paket adı geçici: `dev.soundmod.nothing` (değiştirirsen `java_init.list` ve `namespace` da güncellenmeli).
* Hook'lar `try/catch` içinde; hata olursa stok ses çalar. Durum dosyaları: `/data/system/soundmod/.status_*`.

## Bilinen belirsizlikler (cihazda doğrulanacak)

* `SoundEffectsHelper.Resource.mFileName` alanı ve `getResourceFilePath` imzası Nothing OS 4.1 A16 `services.jar`'da doğrulandı (metot var); çalışma anı davranışı cihazda test edilmeli.
* Ses efektlerinin yeniden yüklenmesi `AudioManager.unloadSoundEffects/loadSoundEffects` ile yansıma üzerinden yapılır; olmazsa yeniden başlat.
* SystemUI SELinux erişimi ROM'a göre değişebilir (yardımcı modül bunun için).
