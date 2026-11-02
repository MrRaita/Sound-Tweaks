# Sound Tweaks

🇬🇧 [English](README.md) · 🇹🇷 Türkçe

> Bu belge İngilizce README'nin Türkçe çevirisidir; farklılık varsa İngilizce olan geçerlidir.

Sound Tweaks, Android sistem seslerini (klavye, dokunma, ekran görüntüsü, kilit ekranı, şarj, dock) değiştirmeye yarayan bir LSPosed modülüdür.

Sistem dosyaları değiştirilmez. Seçilen sesler ayrı bir depoda tutulur ve ilgili sistem süreçlerinde yalnızca ses yolu yönlendirilir; modül devre dışı bırakıldığında ya da sesler sıfırlandığında sistem kendi varsayılanlarına döner.

## Özellikler

- Sesleri dosya yolundan değil, uygulama içinden seçme: sistem seslerinden, kendi dosyanızdan ya da susturarak.
- Sistem ses dizinlerini tarayıp listeleyen, dinlenebilir kütüphane.
- Her ses için ayrı normalize ayarı (genel, kapalı ya da özel hedef seviye); tepeler yumuşak limiter ile sınırlanır.
- Dokunma, kilit ekranı ve şarj sesleri için sistem anahtarlarının durumunu gösterme ve değiştirme (zil modu ile ses seviyeleri dahil).
- İsteğe bağlı "zorla çal" modu: kilit sesleri, sessiz/titreşim modunda da duyulacak şekilde medya ses akışından çalınır.
- Kulaklıkta çıkan yüksek ses (kulak koruma) uyarısını kapatma seçeneği; sisteme hook eklenmez, açılışta çalışan küçük bir betik kullanılır.
- Tanılama raporu (dosya izinleri, ayarlar, hook durumu, SELinux reddleri).

## Desteklenen sesler

| Grup | Sesler | Yöntem |
|---|---|---|
| Klavye | Tuş, boşluk, silme, enter, geçersiz tuş | `system_server` hook'u |
| Dokunma ve ekran | Dokunma tıkı, ekran görüntüsü | `system_server` hook'u, SystemUI hook'u |
| Kilit ekranı | Kilitleme, kilit açma, güvenilir cihaz | `Settings.Global` yolu (varsayılan), isteğe bağlı SystemUI hook'u |
| Şarj ve pil | Düşük pil, şarj, kablosuz şarj | `Settings.Global` yolu |
| Dock | Masa/araç dock'u, dock'tan çıkarma | `Settings.Global` yolu |

Kamera, video kaydı, NFC ve ters şarj sesleri `system_server` ya da SystemUI dışındaki süreçlerde çalındığı için kapsam dışındadır (bkz. [Bilinen sınırlamalar](#bilinen-sınırlamalar)).

## Gereksinimler

- Android 9 (API 28) veya üzeri
- Root (KernelSU, Magisk ya da APatch); ek bir modül gerekmez. Kulaklık uyarısı seçeneği için kök yöneticisinin `/data/adb/post-fs-data.d` dizinindeki betikleri çalıştırması gerekir (üçü de çalıştırır)
- libxposed API 101'i destekleyen bir LSPosed çerçevesi (örneğin Vector)

Geliştirme ve test ortamı: Nothing Phone (4a), Nothing OS 4.1, Android 16, KernelSU, Vector 2.2.

## Kurulum

1. APK'yı deponun **Releases** sayfasından indirip kurun.
2. Uygulamayı açın ve kök yöneticinizden Sound Tweaks'e süper kullanıcı yetkisi verin.
3. Vector (LSPosed) yöneticisinden Sound Tweaks'i etkinleştirin ve **System Framework** ile **System UI**'yi kapsama ekleyin.
4. Cihazı yeniden başlatın.
5. Uygulamada **Ayarlar** sekmesindeki Vector kartı, hook'ların aktif olduğunu göstermelidir.

## Kullanım

- **Sesler:** Bir sese dokunduğunuzda sistem seslerinden seçme, kendi dosyanızı seçme, susturma ya da varsayılana dönme seçenekleri açılır. Dosyalar çözülüp WAV olarak depoya yazılır.
- **Kütüphane:** Bulunan sistem sesleri filtrelenebilir, arama simgesiyle aranabilir ve dinlenebilir.
- **Gelişmiş:** Sistem ses anahtarları, ses işleme (genel ve ses başına normalize), kilit seslerini zorla çalma ve kulaklık uyarısını kapatma.
- **Ayarlar:** Durum kartları (Root ve Vector modülü), tema ve renkler, alt çubuk düzeni, araçlar (Sistem Arayüzünü yenileme, tanı raporu).

## Nasıl çalışır

### Ses deposu

Seçilen sesler root ile `/data/system/soundtweaks` altına yazılır ve herkesin okuyabildiği `system_file` SELinux bağlamıyla işaretlenir.

- `override/<StokAdı>.<uzantı>`: hook'ların yönlendirdiği dosyalar
- `files/<ayarAnahtarı>.<uzantı>`: `Settings.Global` yollarının gösterdiği dosyalar

Bağlam, uygulama her açıldığında ve her ses yazıldığında yeniden uygulanır. Bir sistem güncellemesi dosya bağlamlarını yeniden yazarsa uygulamayı bir kez açmanız yeterlidir.

### Hook'lar

libxposed API 101 ile yazılmıştır (`minApiVersion = targetApiVersion = 101`). Giriş sınıfı `com.mrraita.soundtwks.hook.SoundTweaksModule`, meta veriler `app/src/main/resources/META-INF/xposed/` altındadır.

| Süreç | Sınıf / metot | Amaç |
|---|---|---|
| `system_server` | `SoundEffectsHelper.getResourceFilePath` | Klavye ve dokunma sesi yollarını yönlendirir |
| SystemUI | `ScreenshotSoundProviderImpl.getScreenshotSound` | Ekran görüntüsü sesini değiştirir |
| SystemUI | `KeyguardViewMediator.playSounds`, `playTrustedSound` | Yalnızca "zorla çal" modunda kilit seslerini çalar |

Vector kartındaki "Çerçeve" satırı Vector'ün kendi API sürümünü, "Modül API" satırı ise Sound Tweaks'in derlendiği API sürümünü (101) gösterir; ikisinin farklı olması normaldir. Her hook ayrı bir `try/catch` içindedir; hata durumunda stok davranış sürer.

Kilit sesleri varsayılan olarak stok mekanizmayla çalar: sistem, `lock_sound`, `unlock_sound` ve `trusted_sound` ayarlarındaki yolu Sistem Arayüzü açılırken bir kez yükler. "Zorla çal" modu kapalıyken SystemUI'ye hook eklenmez.

### Kulaklık ses uyarısı

Android, yeniden başlatmadan sonra kulaklıkta ses belirli bir seviyenin üzerine çıkarılırken bir uyarı gösterir. Bu uyarıyı, açılışta okunan üç sistem özelliği denetler. Seçenek açıldığında Sound Tweaks şunları yapar:

- `/data/adb/soundtweaks/hp_warning.sh` betiğini yazar; betik `audio.safemedia.bypass=true`, `audio.safemedia.force=false` ve `audio.safemedia.csd.force=false` değerlerini ayarlar (`resetprop`, yoksa `setprop` ile).
- Sistem başlamadan önce çalışması için `/data/adb/post-fs-data.d/soundtweaks_hp_warning.sh` dosyasına yalnızca betiği çağıran tek bir satır koyar.
- Özellikleri o anda da bir kez ayarlar; böylece yeniden başlatmayı beklemek gerekmez.

Seçenek kapatıldığında iki dosya da silinir ve uyarı sonraki açılıştan itibaren geri gelir. Hiçbir sistem sürecine hook eklenmez. Uygulamayı kaldırmadan önce seçeneği kapatmalısınız; aksi halde betik `/data/adb` altında kalır. Elle temizlemek için:

```sh
su -c "rm -rf /data/adb/soundtweaks /data/adb/post-fs-data.d/soundtweaks_hp_warning.sh"
```

### Ses işleme

Dosyalar `MediaExtractor` ve `MediaCodec` ile çözülür ve 16 bit PCM WAV olarak yazılır. Normalize ölçütü, tüm dosyanın ortalama (RMS) seviyesidir (ffmpeg `volumedetect` "mean_volume" değeriyle aynı); hedefe ulaşmak için gereken kazanç tepeleri taşırıyorsa yumuşak limiter uygulanır.

## Kaynak kullanımı

- Uygulamada servis, açılış alıcısı ya da arka plan işi yoktur.
- Zil modu ve ses seviyesi dinleyicileri yalnızca uygulama ekrandayken açıktır.
- Root sorguları açılışta tek bir `su` çağrısında toplanır; kütüphane ilk gerektiğinde taranır.
- Hook'lar olay tabanlıdır; yoklama ya da sürekli çalışan iş parçacığı yoktur. Override dizini bellekte tutulur ve yalnızca dizinin değişim zamanı sorgulanır.
- Yalnızca ilgili sınıfların (`SoundEffectsHelper` ve `KeyguardViewMediator`) metotları derlenmiş koddan çıkarılır (deopt).
- Kulaklık uyarısı betiği açılışta yalnızca bir kez çalışır, üç özelliği ayarlar ve çıkar.
- Ayrıntılı olay kaydı yalnızca ön sürümlerde ve debug sürümlerinde bulunur, varsayılan olarak kapalıdır; release sürümlerde bu kod hiç çalışmaz.

## Sorun giderme

- **Sesler çalmıyor:** Gelişmiş sekmesindeki "Sistem ses anahtarları" kartında dokunma, kilit ekranı ve şarj sesleri açık olmalı, zil modu da normal olmalıdır. Telefon sessiz ya da titreşimdeyken kilit ve dokunma sesleri çalmaz; kilit sesleri için "zorla çal" modu bunu aşar.
- **Kilit sesi değişmedi:** Kilit sesleri Sistem Arayüzü açılırken yüklendiği için, değişiklikten sonra Sistem Arayüzünün yenilenmesi gerekir (uygulama bunu sorar).
- **Vector kartı kırmızı:** Modül etkin değil ya da kapsam eksik; kapsamı kontrol edip cihazı yeniden başlatın.
- **Tanılama:** Ayarlar → Araçlar → Tanı raporu oluştur. Ön sürümlerde ve debug sürümlerinde "Ayrıntılı olay kaydı" açıkken son SystemUI olayı da rapora yazılır.
- **Kulaklık uyarısı hâlâ çıkıyor:** Kök yöneticiniz `post-fs-data.d` betiklerini çalıştırmıyor ya da uyarıyı açılışta okuyan özellikler üretici tarafından ezilmiş olabilir; seçeneği açıp cihazı yeniden başlatın ve tanı raporuna bakın.

## Derleme

Gereksinim: JDK 17 veya üzeri (CI, JDK 21 kullanır). Gradle sarmalayıcı jar dosyası ikili olduğu için depoda yer almaz; sürüm ve SHA-256 sağlama toplamı `gradle/wrapper/gradle-wrapper.properties` dosyasından okunur ve CI'da dağıtım bu toplamla doğrulanır.

```sh
gradle wrapper --gradle-version <gradle-wrapper.properties içindeki sürüm>   # bir kez
./gradlew assembleRelease
```

`versionName` `app/build.gradle.kts` içindedir; `versionCode` CI çalışma numarasından gelir. Yerel derlemede imza anahtarı verilmezse release APK'sı debug anahtarıyla imzalanır. Ayrıntılı olay kaydı yalnızca CI ön derlemesinde (`SOUNDTWEAKS_VERBOSE=true`) ve debug derlemelerinde APK'ya girer; release derlemelerinde yoktur.

## Proje yapısı

```
app/src/main/
├── kotlin/com/mrraita/soundtwks/
│   ├── MainActivity.kt
│   ├── core/    # katalog, tarayıcı, dönüştürücü, root, depo
│   ├── hook/    # libxposed giriş sınıfı ve hook'lar
│   └── ui/      # Compose ekranları, tema, paletler, ViewModel
├── resources/META-INF/xposed/   # module.prop, scope.list, java_init.list
└── res/
```

## Bilinen sınırlamalar

- Kamera, video kaydı, NFC, ters şarj ve arama bildirim sesleri, Sound Tweaks'in kapsamındaki süreçlerde çalınmaz. Bunları değiştirmek için ilgili uygulamaların Vector kapsamına eklenmesi ve ses dosyalarının bu uygulama süreçlerine aktarılması (Vector uzak dosya desteği) gerekir; bu henüz uygulanmamıştır.
- Eski kamera API'sinin `cameraserver` içinde yerel kodla çaldığı deklanşör sesi hook'lanamaz.
- Kulaklık uyarısı seçeneği, kök yöneticisinin `/data/adb/post-fs-data.d` betiklerini çalıştırmasına bağlıdır.
- Sistem ses kütüphanesi, kökteki tüm bölümlerde `/<bölüm>/media/audio` dizinlerini arar; üreticiye özel ses dizinleri bu kalıbın dışındaysa listelenmez. Tanı raporundaki "sound directories" bölümü, cihazda nelerin bulunduğunu gösterir.
- Hook noktaları AOSP sınıf adlarına dayanır; üreticiye özel arayüzlerde sınıf adları farklıysa ilgili hook çalışmaz ve durum kartında hata olarak görünür.

## Lisans

Sound Tweaks, [GNU Genel Kamu Lisansı sürüm 3](LICENSE) (GPL-3.0-or-later) ile lisanslanmıştır. Programı, Özgür Yazılım Vakfı'nın yayımladığı GPL'in 3. sürümünün ya da (isteğinize bağlı olarak) daha yeni bir sürümünün koşulları altında yeniden dağıtabilir ve değiştirebilirsiniz. Program yararlı olması umuduyla dağıtılır, ancak herhangi bir garanti vermez.
