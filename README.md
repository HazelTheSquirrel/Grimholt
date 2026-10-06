# Grimholt Demo

A complete mobile-first PHP/Nginx demo for Grimholt.

## Stack
- PHP 8.x
- Nginx
- Vanilla CSS + JavaScript
- JSON-backed demo data
- Demo upload endpoint with 64 MB client limit

## Deploy
1. Copy the repository to `/var/www/Grimholt`.
2. Copy `nginx/grimholt.conf` to your Nginx site configuration.
3. Make `storage/uploads` writable by the PHP-FPM user.
4. Test Nginx with `nginx -t` and reload it.
5. Open the server IP/domain.

> The provided Nginx configuration intentionally uses `/run/php/php-fpm.sock`. Change that socket only if your PHP-FPM installation uses a versioned socket.
