# Grimholt

Eine schlichte, atmosphärische Fantasy-Website im Stil einer alten Chronik und Taverne.

## Stack

- Nginx auf Port 80
- PHP 8.1+
- Native CSS
- Vanilla JavaScript (ES6+)
- JSON statt Datenbank

## Nginx

`nginx/grimholt.conf` nach `/etc/nginx/sites-available/grimholt` kopieren, aktivieren, `nginx -t` prüfen und Nginx neu laden. Den `root`-Pfad an den lokalen Checkout anpassen.

## Audio

`assets/audio/tavern.mp3` hinzufügen. Der Browser startet die Musik erst nach dem bewussten Eintritt in die Taverne.