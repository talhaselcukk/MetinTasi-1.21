# Metin Taşı (MetinTasi)

> [!WARNING]
> **Bu proje hâlen test/beta aşamasındadır.** Canlı sunucuya kurmadan önce yedek alın ve ayrı bir test sunucusunda deneyin. Karşılaştığınız hataları; Paper sürümü, hata kaydı ve tekrar etme adımlarıyla GitHub Issues bölümüne bildirin.

**Test edilen hedef:** Paper 1.21.x · Java 21
**Güncel beta:** `1.1.0-beta.1`

**Metin Taşı**, Minecraft sunucuları için geliştirilmiş bir **Metin taşı** deneyimidir. Oyuncuların kırabileceği özel bloklar, can çubuğu, hologram, **eşya ve para ödülleri** (genel havuz veya taşa özel), sıralama hologramı, kırıldıktan sonra **yenilenme süreleri** ve isteğe bağlı **Discord webhook** desteği sunar. Yöneticiler GUI ve komutlarla taşları ve ödülleri yönetebilir.

## Özellikler

- **Metin taşı yerleştirme:** Bloka sağ tıklayıp sohbetten blok türü ve görünen isim girerek taş oluşturun; can, hologram ve `stones/<uuid>.yml` kaydı otomatik oluşur.
- **Ödül sistemi:** `rewards.yml` genel ödül havuzu ile her taş için ayrı `stone-rewards` listesi; para ve eşya ödülleri, şans yüzdeleriyle çalışır.
- **Para ödülleri (GUI):** `/metintasi admin` panelinden ödülü **tüm taşlara** (genel) veya **seçtiğiniz tek taşa** ekleyebilir veya listeleyebilirsiniz.
- **Eşya ödülleri (komut):** `/metintasi reward additem` ile elinizdeki eşyayı (NBT dahil) ödül yapın; hedefi komutta belirtin veya **sohbette taş adı / genel** olarak sorulmasını bekleyin.
- **Yenilenme süreleri:** `config.yml` içinde genel süre (`reactivation.interval-hours` veya `interval-minutes`), yeni taşlar için `metin-stone.default-reactivation-hours`; taş başına `stones/*.yml` içinde ayrı süre tanımlanabilir.
- **Yenilenme duyurusu:** Taş yeniden aktif olduğunda sohbette **turuncu kalın başlık** ve `reactivation-broadcast` ile özelleştirdiğiniz duyuru metni (`<isim>`, `<dunya>`, `<x>`, `<y>`, `<z>`, `<konum>` yer tutucuları).
- **Koruma:** Patlama, piston ve benzeri olaylarda metin bloğunun bozulmasını engelleme (ayar üzerinden).
- **Sıralama hologramı:** Taş başına kırılış sayıları; `/metintasi hologram here` ile yüzen lider tablosu.
- **Discord webhook:** Taş oluşturma, kırılma ve yeniden açılış için embed bildirimleri (`config.yml` → `webhook`).
- **Mesajlar:** Oyuncu mesajları `messages.yml` üzerinden düzenlenebilir.

## Komutlar

* `/metintasi help` — Yardım menüsünü gösterir.
* `/metintasi admin` — Yönetim panelini açar (taş listeleri, para ödülü ekleme/listeleme, hedef seçimi).
* `/metintasi spawn [blok] [isim]` — Metin taşı oluşturma akışını başlatır.
* `/metintasi remove` — Baktığınız bloktaki metin taşını kaldırır.
* `/metintasi list` — Kayıtlı tüm metin taşlarını listeler.
* `/metintasi reload` — `config.yml`, `messages.yml`, taşlar ve ödülleri yeniden yükler.
* `/metintasi reward additem <şans> [taş|genel]` — Elindeki eşyayı ödül yapar; `[taş|genel]` yoksa sohbette hedef sorulur (`genel`, `global`, `*`, `tum` vb.).
* `/metintasi reward move <id> up|down` — Genel havuzdaki ödül sırasını değiştirir.
* `/metintasi hologram here <taş adı>` — Sıralama hologramını bulunduğunuz konuma yerleştirir.
* `/metintasi hologram reset <taş adı>` — O taş için sıralama hologramını kaldırır.
* `/metintasi stone health <taş adı> <maxCan> [şimdikiCan]` — Taş canını ayarlar.
* `/metintasi reset` — Baktığınız taşı tam canla yeniden açar.
* `/metintasi reset <taş adı>` — İsmi verilen taşı tam canla yeniden açar.
* `/metintasi reset all` — Kayıtlı tüm taşları tam canla yeniden açar.
**Komut takma adları:** `mt`, `metintaşı`

Metin taşları yalnızca herhangi bir kazmayla normal kırılma süreci tamamlandığında hasar alır. Kazma türü ve büyüleri fark etmeksizin her tam blok kırma işlemi `1` can düşürür; tek tıklama, yumruk ve diğer eşyalar hasar vermez.

## Hızlı Test Akışı

```text
/mt spawn STONE TestMetini
/mt stone health TestMetini 5 5
/mt list
```

Yumruk ve diğer aletler can düşürmemelidir. Kazmayla blok kırma animasyonu tamamen tamamlandığında can yalnızca `1` azalmalıdır. Beş tam kırmanın ardından taş kırık duruma geçmelidir.

```text
/mt reset TestMetini
/mt reward additem 100 TestMetini
/mt hologram here TestMetini
```

Ödül testi sırasında ödül yapılacak eşya ana elde tutulmalıdır.

## Beta Sürümünde Bilinen Sınırlar

- Gerçek sunucu yükü altında geniş oyuncu kitlesiyle henüz doğrulanmadı.
- Vault para ödülleri için ayrıca Vault ve uyumlu bir ekonomi eklentisi gerekir.
- PlaceholderAPI ve DecentHolograms entegrasyonları isteğe bağlıdır ve kurulu sürüme göre ayrıca test edilmelidir.
- `/reload` yerine tam sunucu yeniden başlatması önerilir.

## Hata Bildirimi

Yeni bir issue açarken aşağıdakileri ekleyin:

1. Paper'ın tam sürümü (`/version` çıktısı)
2. Java sürümü (`java -version`)
3. Hatanın tekrar etme adımları
4. Konsoldaki ilgili hata kaydı
5. Kullanılan Vault, ekonomi, PlaceholderAPI ve hologram eklentilerinin sürümleri

## İzinler (Permissions)

- `metintasi.use` — Metin taşlarıyla etkileşim ve kırma (varsayılan: herkes).
- `metintasi.admin` — Tüm yönetim komutları ve admin paneli (varsayılan: op).

## Kurulum

1. En son sürümü indirin veya projeyi aşağıdaki gibi Maven ile kendiniz derleyin.
2. Oluşan **`MetinTasi.jar`** dosyasını sunucunuzun `plugins` klasörüne kopyalayın.
3. Sunucuyu yeniden başlatın (tam restart önerilir; `/reload` kullanmayın).
4. `plugins/MetinTasi/` altında oluşan **`config.yml`**, **`messages.yml`** ve **`rewards.yml`** dosyalarını ihtiyacınıza göre yapılandırın.

> Eski kurulumlarda yalnızca `settings.yml` varsa, eklenti bunu ilk çalışmada **`config.yml`** olarak kopyalayabilir; ayarları kalıcı olarak `config.yml` üzerinden yönetin.

### Yapılandırma dosyaları (kısa)

- **`config.yml`** — Webhook, hologram satırları, yenilenme modu/süreleri, duyuru metni, spawn zamanları vb.
- **`messages.yml`** — Oyuncu ve admin mesajları.
- **`rewards.yml`** — Genel ödül listesi.
- **`stones/<uuid>.yml`** — Her taşın konumu, canı, özel ödülleri ve isteğe bağlı yenilenme süresi.

## Kaynaktan Derleme (Building from Source)

Bu proje **Maven** kullanır. Projeyi bilgisayarınızda derlemek için:

```bash
mvn clean package
```

Windows’ta Maven Wrapper ile:

```bash
.\mvnw.cmd clean package -DskipTests
```

Çıktı JAR genelde `target/MetinTasi.jar` yolundadır (`pom.xml` içindeki `finalName` ile aynı isim).

## Gereksinimler

- **Java 21**
- **Paper 1.21.x**
- **Vault** — isteğe bağlı; para ödülleri için ekonomi bağlantısı
- **PlaceholderAPI** — isteğe bağlı
- **DecentHolograms** — isteğe bağlı (softdepend; hologramlar için)

---
