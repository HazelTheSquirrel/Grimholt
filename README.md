# Grimholt

Eine atmosphärische Webpräsenz für Rollenspiel, LARP und Minecraft-Projekte.

## Stack

- Nginx auf Port 80
- PHP 8.1+
- Native CSS
- Vanilla JavaScript (ES6+)
- JSON statt Datenbank

## Struktur

- `index.php` – Startseite
- `assets/css/style.css` – Gestaltung
- `assets/js/tavern.js` – Eintritt & Musiksteuerung
- `assets/audio/tavern.mp3` – lokale Hintergrundmusik (nicht enthalten)
- `data/lore.json` – Chronik
- `data/projects.json` – Projekte

## Lokaler Betrieb

Nginx sollte den Projektordner als Document Root verwenden und PHP über PHP-FPM ausführen.

Die Audiodatei muss aus Lizenzgründen separat unter `assets/audio/tavern.mp3` abgelegt werden.

## Nginx

Beispiel:

```nginx
server {
    listen 80;
    server_name grimholt.local;

    root /var/www/grimholt;
    index index.php;

    location / {
        try_files $uri $uri/ /index.php?$query_string;
    }

    location ~ \.php$ {
        include snippets/fastcgi-php.conf;
        fastcgi_pass unix:/run/php/php8.1-fpm.sock;
    }

    location ~ /\. {
        deny all;
    }
}
```
