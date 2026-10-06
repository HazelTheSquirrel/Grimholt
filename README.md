# Grimholt

Atmosphärische Webanwendung für Rollenspiel, LARP und Minecraft.

## Stack
- Nginx auf Port 80
- PHP 8.1+
- Native CSS
- Vanilla JavaScript (ES6+)
- JSON statt Datenbank

## Funktionen
- Taverne-Betreten-Overlay als sauberer Autoplay-Gate für Hintergrundmusik
- Responsive Chronik, Abenteuer und Werkstatt
- Minecraft-Serverbereich mit JSON-Konfiguration
- API-Endpunkt `/api/server-status.php` für die spätere Serveranbindung
- Kein Framework und keine Datenbank

## Minecraft-Anbindung

Die Konfiguration liegt in `data/server.json`. Mit `enabled: true` prüft der Status-Endpunkt zunächst, ob der konfigurierte TCP-Port erreichbar ist.

Wichtig: Das ist bewusst nur ein Connectivity-Check. Für echten Minecraft-Status (MOTD, Version, Spielerzahl) kann später das Java-Server-Status-Protokoll oder eine eigene Bridge ergänzt werden. So wird heute nichts vorgetäuscht, was der Server noch nicht liefert.

## Musik

Lege eine rechtlich passende Datei als `assets/audio/tavern.mp3` ab. Die Wiedergabe beginnt erst nach „Taverne betreten“.

## Lokaler Betrieb

Nginx-Root auf `/var/www/grimholt` setzen und `nginx/grimholt.conf` als Serverblock verwenden. Danach PHP-FPM 8.1 aktivieren und Nginx neu laden.

Beispiel:
```bash
sudo ln -s /var/www/grimholt/nginx/grimholt.conf /etc/nginx/sites-enabled/grimholt
sudo nginx -t
sudo systemctl reload nginx
```

Für den lokalen Namen kann `grimholt.local` in `/etc/hosts` auf `127.0.0.1` zeigen.
