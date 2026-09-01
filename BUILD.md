# Build VcApp local (fără CI)

Nu există workflow de build pe GitHub. APK-ul se face local, pe calculatorul tău.

## De ce ai nevoie

- **Android Studio** (Koala sau mai nou) — vine cu JDK 17 inclus
- **Android SDK Platform 34** + **Build-Tools 34** (Android Studio le descarcă singur la primul sync)
- ~4 GB liberi pe disc

## Pași (Android Studio)

1. `git clone https://github.com/redglitchx001-dev/VcApp.git`
   apoi `git checkout arena/01a05dc3-vcapp`
2. **Open** folderul `VcApp` în Android Studio (nu „Import project", doar Open).
3. Când te întreabă de Gradle wrapper, lasă-l să-l genereze / să folosească Gradle 8.7.
   (Wrapper JAR-ul nu e commituit în repo.)
4. Așteaptă **Gradle Sync** să termine (prima dată durează, descarcă dependențele).
5. Conectează telefonul cu **USB debugging** pornit și apasă **Run ▶**,
   sau **Build → Build Bundle(s)/APK(s) → Build APK(s)**.

APK-ul apare la:
```
app/build/outputs/apk/debug/app-debug.apk
```

## Pași (linie de comandă)

```bash
# o singură dată, dacă ai gradle instalat local:
gradle wrapper --gradle-version 8.7

./gradlew assembleDebug     # APK debug
./gradlew installDebug      # instalează direct pe telefonul conectat
```

Dacă Gradle nu găsește SDK-ul, creează `local.properties` în rădăcina proiectului:

```properties
sdk.dir=/home/utilizator/Android/Sdk        # Linux
# sdk.dir=C:\\Users\\Nume\\AppData\\Local\\Android\\Sdk   # Windows
```

## APK de release (semnat)

```bash
keytool -genkey -v -keystore vcapp.jks -keyalg RSA -keysize 2048 -validity 10000 -alias vcapp
```

Apoi în Android Studio: **Build → Generate Signed Bundle / APK → APK → release**.

## După instalare, pe telefon

1. Dă permisiune la **Microfon** și **Notificări** la prima pornire.
2. Pentru bula flotantă: **Settings → Apps → VcApp → Display over other apps → Allow**
   (butonul din tab-ul *Live* te duce direct acolo).
3. Scoate VcApp din optimizarea bateriei, ca serviciul să nu fie oprit în timpul apelurilor.
