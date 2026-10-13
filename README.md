# SoundMod

Sistem seslerini (klavye, dokunma, ekran görüntüsü, kilit, şarj…) istediğin sesle değiştiren
**LSPosed/Vector modülü + uygulama**. Material 3 Expressive arayüz, root ile çalışır.

Hedef: Nothing Phone (4a) · Nothing OS 4.1 · Android 16 (diğer AOSP tabanlı ROM'lar da denenebilir).

## Nasıl çalışır?

| Ses | Yöntem |
|---|---|
| Klavye tuşları, dokunma tıkı | `system_server` içinde `SoundEffectsHelper.getResourceFilePath` hook'u → `/data/system/soundmod/override/<StokAdı>.*` varsa onu döndürür |
| Ekran görüntüsü | SystemUI içinde `ScreenshotSoundProviderImpl.getScreenshotSound` hook'u |
| Kilitleme, kilit açma, güvenilir cihaz | Varsayılan: Android'in kendi mekanizması (`Settings.Global` `lock_sound` vb. yolu; sistem Sistem Arayüzü açılırken bir kez yükler). İsteğe bağlı "zorla çal" modunda SystemUI'da `KeyguardViewMediator.playSounds` / `playTrustedSound` yakalanır ve dosya SoundPool ile medya ses seviyesinden çalınır |
| Düşük pil, şarj, kablosuz şarj, dock, dock'tan çıkarma | Root ile `settings put global <anahtar> <dosya yolu>` (sistem bu yolu çalarken okur) |

* Uygulama `/product/media/audio` (ve diğer bilinen dizinleri) tarar, sesleri listeler, önizletir.
* Sesi **sistem seslerinden** ya da **kendi dosyandan** seçebilirsin. Kendi dosyaların WAV'a çevrilir,
  istenirse ortak seviyeye normalize edilir (ffmpeg gerekmez).
* Orijinal dosyalara dokunulmaz; "Varsayılana dön" ya da modülü kapatmak her şeyi eski haline getirir.

## Tasarım ilkeleri

1. Material Design, modern ve akıcı arayüz.
2. Sisteme olabildiğince az müdahale ederek olabildiğince çok ve dengeli değişiklik getirmek.
3. Arka planda gereksiz kaynak kullanımını olabildiğince az tutmak.

Bunun uygulamadaki karşılıkları:

* Uygulamada servis, açılış alıcısı ya da arka plan işi yok; kapalıyken hiçbir şey çalışmaz.
* Sistem ses dinleyicileri (zil modu, ses seviyesi, ayar gözlemcileri) yalnızca uygulama ekrandayken açıktır.
* Root sorguları açılışta tek `su` çağrısında toplanır; kütüphane taraması ilk gerektiğinde yapılır.
* Sistem süreçlerindeki hook'lar olay tabanlıdır (yoklama ya da sürekli iş parçacığı yok). Override dizini bellekte
  önbelleklenir; her olayda yalnızca dizinin değişim zamanına bakılır.
* Yalnızca ses akışındaki metotlar derlenmiş koddan çıkarılır (deopt); tüm sınıf değil.
* SystemUI olay kaydı varsayılan olarak kapalıdır (Ayarlar → Gelişmiş → Ayrıntılı olay kaydı).

## Kurulum

1. Repoyu GitHub'a it → **Actions → Build APK** çalışır → `SoundMod-apk` artifact'ındaki APK'yı kur.
2. Uygulamayı aç, root iznini ver.
3. **Vector**'da SoundMod'u etkinleştir, kapsam: **Sistem Çerçevesi** ve **Sistem Arayüzü**. Cihazı yeniden başlat.
4. Uygulamada Ayarlar sekmesi: "Vector modülü" kartı yeşil olmalı.
5. Sesler sekmesinden bir kalem seç → sistem sesi / kendi dosyan.

Not: Klavye/dokunma sesi duymak için *Ayarlar → Ses → Dokunma sesleri* açık olmalı.

### SELinux yardımcı modülü (isteğe bağlı)

Ses deposu (`/data/system/soundmod`) `system_file` SELinux bağlamıyla işaretlenir; böylece SystemUI ve
system_server dosyaları okuyabilir. Uygulama bunu kendisi yapar. Bağlam açılışta bozulursa Ayarlar sekmesindeki
**Kur** düğmesi küçük bir KernelSU/Magisk/APatch modülü kurar; modül her açılışta bağlamı yeniden uygular.

### Sesler çalmıyorsa

* Ayarlar → **Sistem ses anahtarları**: dokunma sesleri, kilit ekranı sesleri ve şarj sesleri açık olmalı; zil modu
  *Normal* ve sistem sesi kapalı olmamalı (titreşim/sessizde kilit ve dokunma sesleri çalmaz).
* Kilit sesleri varsayılanda stok mekanizmayla çalar: telefon sessiz/titreşimdeyken ya da "Kilit ekranı sesleri"
  kapalıyken duyulmaz. Ayarlar → Gelişmiş → **Kilit seslerini zorla çal** açılırsa SoundMod sesi medya ses
  seviyesiyle kendisi çalar (sessiz modda da duyulur); "Kilit ekranı sesleri" kapalıysa yine çalmaz.
* Sistem Arayüzü kendiliğinden yenilenmez; kilit sesi değişince ya da zorla çalma açılınca yenileme sana sorulur,
  ayrıca Ayarlar → Gelişmiş'ten onay vererek yenileyebilirsin.
* Ayarlar → Gelişmiş → **Tanı raporu**: dosya izinleri, ayar değerleri, hook durumu ve SELinux reddlerini gösterir.

## Derleme

* Yerelde: JDK 17+, `gradle wrapper --gradle-version 9.5.0` (bir kez), sonra `./gradlew assembleRelease`.
* CI: `.github/workflows/build.yml` wrapper'ı kendisi üretir. `gradle-wrapper.jar` ikili dosya olduğu için repoda yok.
* Sürüm: `versionName` `app/build.gradle.kts` içinde, `versionCode` CI çalışma numarasından gelir.
* Normalizasyon ölçütü ffmpeg `volumedetect` "mean_volume" ile aynıdır (varsayılan hedef −30 dBFS); tepeler
  yumuşak limiter ile sınırlanır, sonuç seviyesi bildirimde gösterilir.
* Sürüm APK'sı debug anahtarıyla imzalanır (doğrudan kurulabilir).

## Teknik notlar

* libxposed **API 101** (`minApiVersion=targetApiVersion=101`) → Vector 2.1 ve sonrası.
* Giriş sınıfı `dev.soundmod.nothing.hook.SoundModule`; meta veriler `src/main/resources/META-INF/xposed/`.
* system_server hook durumu `/data/system/soundmod_status` dosyasında, SystemUI hook durumu
  `Settings.Global` `soundmod_sysui_status` anahtarında, SystemUI'ın son olayı `soundmod_sysui_last` anahtarında tutulur.
* Paket adı geçici: `dev.soundmod.nothing` (değiştirirsen `java_init.list` ve `namespace` da güncellenmeli).
* Hook'lar `try/catch` içinde; hata olursa stok ses çalar. Durum dosyaları: `/data/system/soundmod/.status_*`.

## Bilinen belirsizlikler (cihazda doğrulanacak)

* `SoundEffectsHelper.Resource.mFileName` alanı ve `getResourceFilePath` imzası Nothing OS 4.1 A16 `services.jar`'da doğrulandı (metot var); çalışma anı davranışı cihazda test edilmeli.
* Ses efektlerinin yeniden yüklenmesi `AudioManager.unloadSoundEffects/loadSoundEffects` ile yansıma üzerinden yapılır; olmazsa yeniden başlat.
* SystemUI SELinux erişimi ROM'a göre değişebilir (yardımcı modül bunun için).
