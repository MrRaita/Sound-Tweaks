# SoundMod

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
- Tema: sistem, açık, koyu, OLED (saf siyah); Material You renkleri ya da 12 hazır palet.
- Yüzen alt gezinti çubuğu, iki genişlikte (geniş: seçili sekme simge ve yazıyla genişleyen hap; kompakt: yalnızca
  simgeler ve kayan vurgu), sekme sırasını sürükle-bırak ile değiştirme ve sekmeleri gizleme.
- Kaydırma hızına bağlı çekme-itme efekti liste satırlarında; üst başlık aşağı kaydırınca daralır ve uygulama
  adının yerine sekme adı geçer.
- Kulaklıkta ses yükseltilirken gösterilen güvenli medya sesi (kulak koruma) uyarısını kapatma seçeneği.
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
- Root (KernelSU, Magisk ya da APatch); ek bir modül gerekmez
- Vector (LSPosed) 2.1 veya üzeri, libxposed API 101 desteğiyle

Geliştirme ve test ortamı: Nothing Phone (4a), Nothing OS 4.1, Android 16, KernelSU, Vector 2.2.

## Kurulum

1. APK, deponun **Releases** sayfasından (ya da **Actions** çıktısından) indirilip kurulur.
2. Uygulama açılır ve root izni verilir.
3. Vector yöneticisinde SoundMod etkinleştirilir. Kapsamda **Sistem Çerçevesi** ve **Sistem Arayüzü** seçili olmalıdır.
4. Cihaz yeniden başlatılır.
5. Uygulamada **Ayarlar** sekmesindeki Vector kartı kancaların aktif olduğunu göstermelidir.

## Kullanım

- **Sesler:** Bir sese dokunulduğunda sistem seslerinden seçme, kendi dosyasını seçme, susturma ya da varsayılana
  dönme seçenekleri açılır. Dosyalar çözülüp WAV olarak depoya yazılır.
- **Kütüphane:** Bulunan sistem sesleri filtrelenir, arama simgesiyle aranır ve dinlenir.
- **Gelişmiş:** Sistem ses anahtarları, ses işleme (genel ve ses başına normalize), kilit seslerini zorla çal.
- **Ayarlar:** Durum kartları, tema ve renkler, alt çubuk düzeni, Sistem Arayüzü yenileme ve tanılama.

## Nasıl çalışır

### Ses deposu

Seçilen sesler root ile `/data/system/soundmod` altına yazılır ve herkesin okuyabildiği `system_file` SELinux
bağlamıyla işaretlenir.

- `override/<StokAdı>.<uzantı>`: hook'ların yönlendirdiği dosyalar
- `files/<ayarAnahtarı>.<uzantı>`: `Settings.Global` yollarının gösterdiği dosyalar

Bağlam, uygulama her açıldığında ve her ses yazıldığında yeniden uygulanır. Sistem güncellemesi dosya bağlamlarını
yeniden yazarsa uygulamanın bir kez açılması yeterlidir.

### Kaydırma efektleri

Kaydırma hareketi bir `NestedScrollConnection` ile izlenir ve −1..1 aralığında bir "çekme" değerine dönüştürülür;
değer bırakılınca az sönümlü bir yayla sıfıra döner. Liste satırları bu değeri yalnızca çizim aşamasında okur
(yeniden oluşturma tetiklenmez). Üst başlık iki durumludur (geniş/dar); kaydırma eşiği aşılınca yaylı bir geçişle
daralır. "Animasyonları azalt" açıkken efektler kapanır.

### Hook'lar

libxposed API 101 ile yazılmıştır (`minApiVersion = targetApiVersion = 101`). Giriş sınıfı
`dev.soundmod.nothing.hook.SoundModule`, meta veriler `app/src/main/resources/META-INF/xposed/` altındadır.

| Süreç | Sınıf / metot | Amaç |
|---|---|---|
| `system_server` | `SoundEffectsHelper.getResourceFilePath` | Klavye ve dokunma sesi yollarını yönlendirir |
| `system_server` | `SoundDoseHelper.checkSafeMediaVolume`, `checkSafeMediaVolume_l`, `raiseVolumeDisplaySafeMediaVolume` | Bayrak varken kulaklık ses uyarısını atlar |
| SystemUI | `ScreenshotSoundProviderImpl.getScreenshotSound` | Ekran görüntüsü sesini değiştirir |
| SystemUI | `KeyguardViewMediator.playSounds`, `playTrustedSound` | Yalnızca "zorla çal" modunda kilit seslerini çalar |

Her hook ayrı bir `try/catch` içindedir; hata durumunda stok davranış sürer. Kilit sesleri varsayılanda stok
mekanizmayla çalar: sistem `lock_sound`, `unlock_sound` ve `trusted_sound` ayarlarındaki yolu Sistem Arayüzü açılırken
bir kez yükler. "Zorla çal" modu kapalıyken SystemUI'a hook eklenmez.

### Kulaklık ses uyarısı

Android, yeniden başlatmadan sonra kulaklıkta ses belirli bir seviyenin üzerine çıkarılırken bir uyarı gösterir
(`AudioService.adjustStreamVolume` → `SoundDoseHelper.raiseVolumeDisplaySafeMediaVolume` → `checkSafeMediaVolume`).
Seçenek açıldığında `/data/system/soundmod/.no_hp_warning` bayrağı oluşturulur; ilgili üç yöntem bayrak varken
"uyarı gerekmiyor" (`false`) sonucunu döndürür ve uyarı hiç gösterilmez. Bu sürümde dönüş değerleri bayt koduyla
doğrulanmıştır: `true`, sesin güvenli seviyeye kırpılması anlamına gelir. Hook her çağrıda yalnızca bayrağın varlığına bakar, bu
yüzden ayar anında geçerlidir ve ayrı bir `system.prop` modülü gerekmez. Ses dozu (CSD) bildirimleri değiştirilmez.

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
- Ayrıntılı olay kaydı varsayılan olarak kapalıdır.

## Sorun giderme

- **Sesler çalmıyor:** Gelişmiş sekmesindeki "Sistem ses anahtarları" kartında dokunma, kilit ekranı ve şarj sesleri
  açık olmalı, zil modu normal olmalıdır. Telefon sessiz ya da titreşimdeyken kilit ve dokunma sesleri çalmaz;
  kilit sesleri için "zorla çal" modu bunu aşar.
- **Kilit sesi değişmedi:** Kilit sesleri Sistem Arayüzü açılırken yüklendiği için değişiklikten sonra Sistem Arayüzünün
  yenilenmesi gerekir (uygulama bunu sorar).
- **Vector kartı kırmızı:** Modül etkin değil ya da kapsam eksik; kapsam ve yeniden başlatma kontrol edilmelidir.
- **Tanılama:** Ayarlar → Araçlar → Tanı raporu. "Ayrıntılı olay kaydı" açıkken son SystemUI olayı da rapora yazılır.

## Derleme

Gereksinimler: JDK 17 veya üzeri. Gradle sarmalayıcı jar dosyası ikili olduğu için depoda yer almaz.

```sh
gradle wrapper --gradle-version 9.5.0   # bir kez
./gradlew assembleRelease
```

`versionName` `app/build.gradle.kts` içindedir; `versionCode` CI çalışma numarasından gelir. Yerel derlemede imza
anahtarı verilmezse sürüm APK'sı debug anahtarıyla imzalanır.

## CI/CD

| Akış | Tetikleyici | Çıktı |
|---|---|---|
| `prebuild.yml` (Ön derleme) | Her push, PR ve elle | İmzalı APK (artifact) ve varsayılan dalda `pre-<çalışma no>` etiketli ön sürüm (son 10 saklanır) |
| `release.yml` (Kararlı sürüm) | Yalnızca elle | `v<versionName>` etiketli kararlı sürüm; imza anahtarı yoksa başarısız olur, aynı sürüm numarası varsa durur |
| `build-apk.yml` | Yukarıdakiler çağırır | Ortak derleme ve imzalama adımları |
| `codeql.yml` | Push, PR, haftalık | Java/Kotlin ve GitHub Actions için CodeQL analizi (`security-and-quality`) |
| `dependabot.yml` | Haftalık | Gradle ve GitHub Actions sürüm güncellemeleri |

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
2. **Actions → Kararlı sürüm → Run workflow** çalıştırılır (isteğe bağlı sürüm notu girilebilir).
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
- Hook noktaları AOSP sınıf adlarına dayanır; üreticiye özel arayüzlerde sınıf adları farklıysa ilgili hook çalışmaz ve
  durum kartında hata olarak görünür.

## Lisans

Henüz bir lisans dosyası eklenmemiştir.
