# SoundMod

🇬🇧 [English](README.md) · 🇹🇷 Türkçe

> Uygulama arayüzü şu an İngilizcedir (sekmeler: **Sounds**, **Library**, **Advanced**, **Settings**). Bu belge İngilizce README'nin Türkçe çevirisidir; farklılık varsa İngilizce olan geçerlidir.

Android sistem seslerini (klavye, dokunma, ekran görüntüsü, kilit ekranı, şarj, dock) değiştirmeye yarayan,
LSPosed/Vector modülü ve yönetim uygulaması. Arayüz Material 3 Expressive ile yazılmıştır.

Sistem dosyaları değiştirilmez. Seçilen sesler ayrı bir depoda tutulur ve ilgili sistem süreçlerinde yalnızca
ses yolu yönlendirilir; modül devre dışı bırakıldığında ya da sesler sıfırlandığında sistem kendi varsayılanlarına döner.

## Özellikler

- Sesleri dosya yolundan değil, uygulama içinden seçme: sistem seslerinden, kullanıcının kendi dosyasından ya da
  susturarak.
- Sistem ses dizinlerinin taranıp listelendiği, dinlenebilen kütüphane (çal/duraklat, süreye bağlı dalgalı ilerleme çubuğu).
- Her ses için ayrı normalize ayarı (genel, kapalı ya da özel hedef seviye); tepeler yumuşak limiter ile sınırlanır.
- Dokunma, kilit ekranı ve şarj sesleri için sistem anahtarlarının durumunu gösterme ve değiştirme (zil modu ile ses
  seviyeleri dahil).
- İsteğe bağlı "zorla çal" modu: kilit sesleri sessiz/titreşim modunda da duyulacak şekilde medya ses akışından çalınır.
- Sistem Arayüzünü yeniden başlatma düğmesi (ilk kullanımda uyarı gösterir, uyarı kapatılabilir).
- Tema: sistem, açık, koyu; ayrı bir OLED (saf siyah) anahtarı, koyu temanın (sistem koyuya geçince de) siyah
  olmasını sağlar. Material You renkleri ya da 12 hazır palet.
- Yüzen alt gezinti çubuğu, iki genişlikte (geniş: seçili sekme simge ve yazıyla genişleyen hap; kompakt: yalnızca
  simgeler ve kayan vurgu), sekme sırasını sürükle-bırak ile değiştirme ve sekmeleri gizleme.
- Üst başlık aşağı kaydırınca daralır, uygulama adının yerine sekme adı geçer; başlık listenin üstüne biner, köşelerin
  ardında liste görünür. Kütüphane sekmesinde arama satırı ve filtre çipleri başlığa bağlıdır, başlıkla birlikte yukarı kayar.
- Kulaklıkta çıkan yüksek ses (kulak koruma) uyarısını kapatma seçeneği; sisteme kanca eklenmez, açılışta çalışan
  küçük bir betik kullanılır.
- Tanılama raporu (dosya izinleri, ayarlar, hook durumu, SELinux reddleri).

## Desteklenen sesler

| Grup | Sesler | Yöntem |
|---|---|---|
| Klavye | Tuş, boşluk, silme, enter, geçersiz tuş | `system_server` hook'u |
| Dokunma ve ekran | Dokunma tıkı, ekran görüntüsü | `system_server` hook'u, SystemUI hook'u |
| Kilit ekranı | Kilitleme, kilit açma, güvenilir cihaz | `Settings.Global` yolu (varsayılan), isteğe bağlı SystemUI hook'u |
| Şarj ve pil | Düşük pil, şarj, kablosuz şarj | `Settings.Global` yolu |
| Dock | Masa/araç dock'u, dock'tan çıkarma | `Settings.Global` yolu |

Kamera, video kaydı, NFC ve ters şarj sesleri `system_server` ya da SystemUI dışındaki süreçlerde çalındığı için
kapsam dışındadır (bkz. [Bilinen sınırlamalar](#bilinen-sınırlamalar)).

## Gereksinimler

- Android 9 (API 28) veya üzeri
- Root (KernelSU, Magisk ya da APatch); ek bir modül gerekmez. Kulaklık uyarısı seçeneği için kök yöneticisinin
  `/data/adb/post-fs-data.d` dizinini çalıştırması gerekir (üçü de çalıştırır)
- Vector (LSPosed) 2.1 veya üzeri, libxposed API 101 desteğiyle

Geliştirme ve test ortamı: Nothing Phone (4a), Nothing OS 4.1, Android 16, KernelSU, Vector 2.2.

## Kurulum

1. APK, deponun **Releases** sayfasından (ya da **Actions** çıktısından) indirilip kurulur.
2. Uygulama açılır ve root izni verilir.
3. Vector yöneticisinde SoundMod etkinleştirilir. Kapsamda **System Framework** ve **System UI** seçili olmalıdır.
4. Cihaz yeniden başlatılır.
5. Uygulamada **Settings** sekmesindeki Vector kartı kancaların aktif olduğunu göstermelidir.

## Kullanım

- **Sesler (Sounds):** Bir sese dokunulduğunda sistem seslerinden seçme, kendi dosyasını seçme, susturma ya da varsayılana
  dönme seçenekleri açılır. Dosyalar çözülüp WAV olarak depoya yazılır.
- **Kütüphane (Library):** Bulunan sistem sesleri filtrelenir, arama simgesiyle aranır ve dinlenir.
- **Gelişmiş (Advanced):** Sistem ses anahtarları, ses işleme (genel ve ses başına normalize), kilit seslerini zorla çal,
  kulaklık uyarısını kapat.
- **Ayarlar (Settings):** Durum kartları (Root ve Vector modülü), tema ve renkler, alt çubuk düzeni, araçlar (Sistem Arayüzünü
  yenileme, tanı raporu).

## Nasıl çalışır

### Ses deposu

Seçilen sesler root ile `/data/system/soundmod` altına yazılır ve herkesin okuyabildiği `system_file` SELinux
bağlamıyla işaretlenir.

- `override/<StokAdı>.<uzantı>`: hook'ların yönlendirdiği dosyalar
- `files/<ayarAnahtarı>.<uzantı>`: `Settings.Global` yollarının gösterdiği dosyalar

Bağlam, uygulama her açıldığında ve her ses yazıldığında yeniden uygulanır. Sistem güncellemesi dosya bağlamlarını
yeniden yazarsa uygulamanın bir kez açılması yeterlidir.

### Arayüz

Üst başlık iki durumludur (geniş/dar); kaydırma eşiği aşılınca yaylı bir geçişle daralır, arka planı yükseklikle
birlikte küçülür ve listenin üstüne biner. Liste satırlarına kaydırma sırasında efekt uygulanmaz; yalnızca liste ilk
açılırken satırlar sırayla belirir. "Animasyonları azalt" açıkken geçişler sadeleşir.

### Hook'lar

libxposed API 101 ile yazılmıştır (`minApiVersion = targetApiVersion = 101`). Giriş sınıfı
`dev.soundmod.nothing.hook.SoundModule`, meta veriler `app/src/main/resources/META-INF/xposed/` altındadır.

| Süreç | Sınıf / metot | Amaç |
|---|---|---|
| `system_server` | `SoundEffectsHelper.getResourceFilePath` | Klavye ve dokunma sesi yollarını yönlendirir |
| SystemUI | `ScreenshotSoundProviderImpl.getScreenshotSound` | Ekran görüntüsü sesini değiştirir |
| SystemUI | `KeyguardViewMediator.playSounds`, `playTrustedSound` | Yalnızca "zorla çal" modunda kilit seslerini çalar |

Vector kartında "Framework" satırı Vector'ün kendi API sürümünü, "Module API" satırı SoundMod'un derlendiği API'yi
(101) gösterir; ikisinin farklı olması normaldir. Her hook ayrı bir `try/catch` içindedir; hata durumunda stok
davranış sürer. Kilit sesleri varsayılanda stok
mekanizmayla çalar: sistem `lock_sound`, `unlock_sound` ve `trusted_sound` ayarlarındaki yolu Sistem Arayüzü açılırken
bir kez yükler. "Zorla çal" modu kapalıyken SystemUI'a hook eklenmez.

### Kulaklık ses uyarısı

Android, yeniden başlatmadan sonra kulaklıkta ses belirli bir seviyenin üzerine çıkarılırken bir uyarı gösterir. Uyarıyı
açılışta okunan üç sistem özelliği denetler. Seçenek açıldığında SoundMod şunları yapar:

- `/data/adb/soundmod/hp_warning.sh` betiğini yazar; betik `audio.safemedia.bypass=true`,
  `audio.safemedia.force=false` ve `audio.safemedia.csd.force=false` değerlerini ayarlar (`resetprop`, yoksa `setprop`).
- Sistem başlamadan önce çalışsın diye `/data/adb/post-fs-data.d/soundmod_hp_warning.sh` içine yalnızca betiği çağıran
  tek satır koyar.
- Özellikleri o anda da bir kez ayarlar, böylece yeniden başlatma beklemek gerekmez.

Seçenek kapatılınca iki dosya da silinir ve uyarı sonraki açılıştan itibaren geri gelir. Hiçbir sistem sürecine hook
eklenmez. Uygulamayı kaldırmadan önce seçeneği kapatmak gerekir; aksi halde betik `/data/adb` altında kalır. Elle temizlemek için:

```sh
su -c "rm -rf /data/adb/soundmod /data/adb/post-fs-data.d/soundmod_hp_warning.sh"
```

### Ses işleme

Dosyalar `MediaExtractor` ve `MediaCodec` ile çözülür ve 16 bit PCM WAV olarak yazılır. Normalize ölçütü tüm dosyanın
ortalama (RMS) seviyesidir (ffmpeg `volumedetect` "mean_volume" ile aynı); hedefe ulaşmak için gereken kazanç tepeleri
taşırıyorsa yumuşak limiter uygulanır.

## Kaynak kullanımı

- Uygulamada servis, açılış alıcısı ya da arka plan işi yoktur.
- Zil modu ve ses seviyesi dinleyicileri yalnızca uygulama ekrandayken açıktır.
- Root sorguları açılışta tek `su` çağrısında toplanır; kütüphane ilk gerektiğinde taranır.
- Hook'lar olay tabanlıdır; yoklama ya da sürekli çalışan iş parçacığı yoktur. Override dizini bellekte tutulur ve
  yalnızca dizinin değişim zamanı sorgulanır.
- Yalnızca ses akışındaki metotlar derlenmiş koddan çıkarılır (deopt).
- Kulaklık uyarısı betiği yalnızca açılışta bir kez çalışır ve üç özellik ayarlayıp çıkar.
- Ayrıntılı olay kaydı yalnızca ön sürümlerde bulunur ve varsayılan olarak kapalıdır; kararlı sürümde bu kod hiç
  çalışmaz.

## Sorun giderme

- **Sesler çalmıyor:** Advanced sekmesindeki "System sound switches" kartında dokunma, kilit ekranı ve şarj sesleri
  açık olmalı, zil modu normal olmalıdır. Telefon sessiz ya da titreşimdeyken kilit ve dokunma sesleri çalmaz;
  kilit sesleri için "zorla çal" modu bunu aşar.
- **Kilit sesi değişmedi:** Kilit sesleri Sistem Arayüzü açılırken yüklendiği için değişiklikten sonra Sistem Arayüzünün
  yenilenmesi gerekir (uygulama bunu sorar).
- **Vector kartı kırmızı:** Modül etkin değil ya da kapsam eksik; kapsam ve yeniden başlatma kontrol edilmelidir.
- **Tanılama:** Settings → Tools → Generate diagnostic report. Ön sürümlerde "Verbose event log" açıkken son SystemUI
  olayı da rapora yazılır.
- **Kulaklık uyarısı hâlâ çıkıyor:** Kök yöneticisi `post-fs-data.d` betiklerini çalıştırmıyor olabilir ya da uyarıyı
  açılışta okuyan özellikler üretici tarafından ezilmiş olabilir; seçeneği açıp cihazı yeniden başlat ve tanı raporuna bak.

## Derleme

Gereksinimler: JDK 17 veya üzeri (CI JDK 21 kullanır). Gradle sarmalayıcı jar dosyası ikili olduğu için depoda yer
almaz; sürüm ve SHA-256 sağlama toplamı `gradle/wrapper/gradle-wrapper.properties` dosyasından okunur ve CI'da dağıtım bu toplamla doğrulanır.

```sh
gradle wrapper --gradle-version <gradle-wrapper.properties içindeki sürüm>   # bir kez
./gradlew assembleRelease
```

`versionName` `app/build.gradle.kts` içindedir; `versionCode` CI çalışma numarasından gelir. Yerel derlemede imza
anahtarı verilmezse sürüm APK'sı debug anahtarıyla imzalanır. Ayrıntılı olay kaydı yalnızca CI ön derlemesinde
(`SOUNDMOD_VERBOSE=true`) APK'ya girer; yerel ve kararlı derlemelerde yoktur.

## CI/CD

| Akış | Tetikleyici | Çıktı |
|---|---|---|
| `prebuild.yml` (Pre-build) | Her push, PR ve elle | İmzalı APK (artifact, ayrıntılı olay kaydı dahil) ve varsayılan dalda `pre-<çalışma no>` etiketli ön sürüm (son 10 saklanır) |
| `release.yml` (Stable release) | Yalnızca elle | `v<versionName>` etiketli kararlı sürüm (ayrıntılı olay kaydı yok); imza anahtarı yoksa başarısız olur, aynı sürüm numarası varsa durur |
| `build-apk.yml` | Yukarıdakiler çağırır | Ortak derleme ve imzalama adımları |
| `security.yml` (Security and quality scan) | Push, PR, haftalık, elle | Android Lint, Semgrep, CodeQL (Java/Kotlin ve GitHub Actions, `security-and-quality`), Gitleaks (git geçmişinde gizli anahtar ve imza anahtarı dosyası), zizmor (workflow güvenliği), PR'larda bağımlılık incelemesi |
| `dependabot.yml` | Haftalık | Gradle ve GitHub Actions sürüm güncellemeleri (gruplu); libxposed API'sinin büyük sürüm atlaması yok sayılır |

Android Lint hata bulursa, Semgrep ERROR düzeyinde ve zizmor yüksek/kritik düzeyde bulgu verirse iş başarısız olur; daha düşük
önemdeki bulgular yalnızca raporlanır; sonuçlar iş özetinde ve artifact olarak (`lint-raporu`,
`semgrep-raporu`, `zizmor-raporu`) bulunur. Gitleaks, CodeQL ve bağımlılık incelemesi bulgu varsa işi başarısız yapar. Ön derleme PR'larda imza sırlarını görmez
(PR derlemeleri debug anahtarıyla imzalanır) ve derleme akışına yalnızca dört imza sırrı aktarılır.
Sürüm sayfaları GitHub CLI (`gh release create`) ile oluşturulur, üçüncü taraf yayın eklentisi kullanılmaz.
`gradle/actions/setup-gradle` tam commit SHA'sına sabitlenmiştir; sürüm yorumunu Dependabot güncel tutar.

### İmza anahtarı

Ön sürümler ve kararlı sürümler aynı anahtarla imzalanır; bu sayede biri diğerinin üzerine kurulabilir. Anahtar depoya
konmaz, **Settings → Secrets and variables → Actions** altına dört gizli değişken olarak eklenir:

| Gizli değişken | İçerik |
|---|---|
| `SIGNING_KEYSTORE_BASE64` | Keystore dosyasının base64 hâli |
| `SIGNING_KEYSTORE_PASSWORD` | Keystore parolası |
| `SIGNING_KEY_ALIAS` | Anahtar takma adı |
| `SIGNING_KEY_PASSWORD` | Anahtar parolası |

Anahtar üretme ve base64'e çevirme:

```sh
keytool -genkeypair -v -keystore soundmod.jks -alias soundmod \
  -keyalg RSA -keysize 4096 -validity 10000
base64 -w0 soundmod.jks    # çıktı SIGNING_KEYSTORE_BASE64 olarak kaydedilir
```

Keystore dosyası ve parolalar yedeklenmelidir; kaybedilirse mevcut kurulumların üzerine güncelleme yapılamaz.
`SIGNING_KEYSTORE_BASE64` tanımlı değilse ön derleme debug anahtarıyla imzalanır (her derlemede farklı imza) ve
uyarı verir. Derleme sonunda `apksigner` sertifika özeti günlüğe yazılır; iki sürümün aynı anahtarla imzalandığı bu
özetle doğrulanabilir.

### Kararlı sürüm çıkarma

1. `app/build.gradle.kts` içinde `versionName` artırılır ve push edilir.
2. **Actions → Stable release → Run workflow** çalıştırılır (isteğe bağlı sürüm notu girilebilir).
3. APK, `v<versionName>` etiketiyle **Releases** sayfasında kararlı sürüm olarak yayımlanır.

## Proje yapısı

```
app/src/main/
├── kotlin/dev/soundmod/nothing/
│   ├── MainActivity.kt
│   ├── core/    # katalog, tarayıcı, dönüştürücü, root, depo
│   ├── hook/    # libxposed giriş sınıfı ve hook'lar
│   └── ui/      # Compose ekranları, tema, paletler, ViewModel
├── resources/META-INF/xposed/   # module.prop, scope.list, java_init.list
└── res/
```

## Bilinen sınırlamalar

- Kamera, video kaydı, NFC, ters şarj ve arama bildirim sesleri SoundMod'un kapsamındaki süreçlerde çalınmaz. Bu
  sesler için ilgili uygulamaların Vector kapsamına alınması ve ses dosyalarının uygulama süreçlerine aktarılması
  (Vector uzak dosya desteği) gerekir; henüz uygulanmamıştır.
- Eski kamera API'sinin `cameraserver` içinde yerel kodla çaldığı deklanşör sesi hook'lanamaz.
- Kulaklık uyarısı seçeneği kök yöneticisinin `/data/adb/post-fs-data.d` betiklerini çalıştırmasına bağlıdır.
- Sistem ses kütüphanesi `/<bölüm>/media/audio` dizinlerini kökteki tüm bölümlerde arar; üreticiye özel ses
  dizinleri bu kalıbın dışındaysa listelenmez. Tanı raporundaki "sound directories" bölümü cihazda nelerin bulunduğunu gösterir.
- Hook noktaları AOSP sınıf adlarına dayanır; üreticiye özel arayüzlerde sınıf adları farklıysa ilgili hook çalışmaz ve
  durum kartında hata olarak görünür.

## Lisans

SoundMod, [GNU Genel Kamu Lisansı sürüm 3](LICENSE) (GPL-3.0-or-later) ile lisanslanmıştır. Programı, Özgür Yazılım
Vakfı'nın yayımladığı GPL'in 3. sürümü ya da (isteğinize bağlı olarak) daha yeni bir sürümü koşullarında yeniden
dağıtabilir ve değiştirebilirsiniz. Program yararlı olması umuduyla dağıtılır, ancak herhangi bir garanti vermez.
