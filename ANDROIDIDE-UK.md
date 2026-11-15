# Збірка HY320 Remote на телефоні

1. Встанови AndroidIDE з офіційного джерела: https://github.com/AndroidIDEOfficial/AndroidIDE/releases (перевір актуальність проєкту).
2. Під час першого запуску налаштуй JDK, Android SDK та build tools відповідно до підказок AndroidIDE. Потрібно кілька ГБ вільного місця.
3. Розпакуй цей ZIP і відкрий папку `HY320Remote` в AndroidIDE.
4. Проєкт має Gradle build scripts, але **не має Gradle Wrapper JAR**. Якщо AndroidIDE вимагає `gradlew`/wrapper, треба додати wrapper з Gradle 8.9; цей ZIP не є гарантовано сумісним з AndroidIDE без доопрацювання.
5. Спробуй Sync/Build APK. Для AGP 8.7.3 потрібні JDK 17, Gradle 8.9, Android SDK 35.
6. APK після успішної збірки: `app/build/outputs/apk/debug/app-debug.apk`.

Альтернатива: GitHub Actions workflow `.github/workflows/build-apk.yml` автоматично збирає APK у GitHub; завантажуй артефакт у вкладці Actions.

Важливо: ADB на проекторі має реально слухати TCP порт 5555. Увімкнення USB debugging не гарантує цього. Застосунок не підтримує Android 11+ TLS pairing. Таймер не гарантує виконання після закриття застосунку. Наразі AI/DeepSeek не інтегровано.
