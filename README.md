# Grimholt

Eine eigenständige, atmosphärische Fantasy-Website – bewusst ohne Datenbank und ohne Framework-Overhead.

## Technik

- Nginx auf Port 80
- PHP 8.1+
- Native CSS
- Vanilla JavaScript (ES6+)
- JSON als Datenbasis
- HTML5 Audio

## Aktueller Aufbau

Die Startseite ist als immersive Eingangsszene gestaltet. Der Besucher öffnet bewusst die Tür, anschließend erscheint die eigentliche Website und die optionale Tavernenmusik startet.

Die Musik bietet:
- An/Aus
- Lautstärkeregler
- gespeicherte Lautstärke per localStorage
- keinen automatischen Start vor der Benutzerinteraktion

## Audio

Lege die gewünschte Musik als:

`assets/audio/tavern.mp3`

ab.

Fehlt die Datei, bleibt die Website trotzdem vollständig benutzbar; der Audioplayer fällt still auf "Musik aus" zurück.

## Lokale Installation

Das Projekt erwartet aktuell:

`/var/www/Grimholt`

als Nginx-Webroot.

Die mitgelieferte Konfiguration liegt unter:

`nginx/grimholt.conf`

Nach Änderungen:

```bash
sudo nginx -t
sudo systemctl reload nginx
```

## Architektur

Die Website bleibt zunächst bewusst schlank. Inhalte können über JSON erweitert werden, ohne eine Datenbank einzuführen. Eine spätere Chat-/NSC-Funktion kann als eigener PHP-Endpunkt ergänzt werden, ohne das visuelle Fundament neu bauen zu müssen.
