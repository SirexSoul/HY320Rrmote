# HY320 Remote

Android remote for MAGCUBIC HY320 using classic ADB over Wi-Fi.

Restored from the existing HY320 Remote project. Supports YouTube links and Android sharing, volume, navigation, playback, and a sleep timer.

Phone and projector must share a network. Enable classic ADB TCP on the projector (usually port 5555) and approve its RSA prompt on first connection. Android 11 TLS pairing is not supported. Sleep sends Android KEYCODE_SLEEP; projector lamp shutdown depends on firmware.
