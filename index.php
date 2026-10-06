<?php
declare(strict_types=1);

function loadJson(string $path, array $fallback = []): array {
    if (!is_file($path)) return $fallback;
    try {
        $data = json_decode((string) file_get_contents($path), true, 512, JSON_THROW_ON_ERROR);
        return is_array($data) ? $data : $fallback;
    } catch (Throwable) {
        return $fallback;
    }
}
$projects = loadJson(__DIR__ . '/data/projects.json');
$lore = loadJson(__DIR__ . '/data/lore.json');
$server = loadJson(__DIR__ . '/data/server.json', ['name'=>'Grimholt','host'=>'127.0.0.1','port'=>25565,'enabled'=>false]);
?><!doctype html>
<html lang="de">
<head>
<meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="theme-color" content="#0d0907">
<meta name="description" content="Grimholt – eine lebendige Chronik für Rollenspiel, LARP und Minecraft.">
<title>Grimholt — Chronik der alten Lande</title>
<link rel="preconnect" href="https://fonts.googleapis.com"><link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Cinzel:wght@500;600;700;800&family=Crimson+Text:wght@400;600&display=swap" rel="stylesheet">
<link rel="stylesheet" href="/assets/css/style.css">
</head>
<body>
<div class="grain" aria-hidden="true"></div>
<div class="entry" id="entry"><div class="entry__panel">
<div class="entry__seal" aria-hidden="true">✦</div><p class="eyebrow">Eine Chronik aus den alten Landen</p>
<h1>Grimholt</h1><p class="entry__text">Draußen ist die Nacht kalt. Drinnen brennt noch Licht.</p>
<button class="button button--gold" id="enterTavern" type="button"><span aria-hidden="true">⚔</span> Taverne betreten</button>
<p class="entry__hint">Mit dem Eintritt darf die Taverne ihre Musik erklingen lassen.</p>
</div></div>
<main class="site-shell" id="site" aria-hidden="true">
<header class="masthead">
<a class="brand" href="/" aria-label="Grimholt Startseite"><span class="brand__mark">✦</span><span>Grimholt</span></a>
<nav class="nav" aria-label="Hauptnavigation"><a href="#chronik">Chronik</a><a href="#abenteuer">Abenteuer</a><a href="#minecraft">Minecraft</a><a href="#werkstatt">Werkstatt</a></nav>
<button class="music-toggle" id="musicToggle" type="button" aria-label="Musik abspielen">♫</button>
</header>
<section class="hero"><p class="eyebrow">Willkommen, Reisender</p><h1>Wo Geschichten<br><em>noch Gewicht haben.</em></h1>
<p class="hero__lead">Grimholt ist eine kleine Ecke zwischen Wirklichkeit und Legende — für Rollenspiel, LARP, Minecraft und jene, die lieber selbst am Feuer sitzen als nur davon zu lesen.</p>
<div class="hero__actions"><a class="button button--gold" href="#chronik">Die Chronik öffnen</a><a class="text-link" href="#minecraft">Zum Server <span>→</span></a></div></section>
<section class="section" id="chronik"><div class="section__heading"><p class="eyebrow">Aus den Annalen</p><h2>Die Chronik</h2></div>
<div class="lore-grid"><?php foreach ($lore as $entry): ?><article class="card card--lore">
<span class="card__sigil"><?= htmlspecialchars((string)($entry['sigil'] ?? '✦'), ENT_QUOTES, 'UTF-8') ?></span>
<p class="card__meta"><?= htmlspecialchars((string)($entry['era'] ?? ''), ENT_QUOTES, 'UTF-8') ?></p><h3><?= htmlspecialchars((string)($entry['title'] ?? ''), ENT_QUOTES, 'UTF-8') ?></h3>
<p><?= htmlspecialchars((string)($entry['text'] ?? ''), ENT_QUOTES, 'UTF-8') ?></p></article><?php endforeach; ?></div></section>
<section class="section section--dark" id="abenteuer"><div class="section__heading"><p class="eyebrow">Was vor dem Feuer liegt</p><h2>Abenteuer</h2></div>
<div class="project-list"><?php foreach ($projects as $project): ?><article class="project"><div class="project__icon" aria-hidden="true"><?= htmlspecialchars((string)($project['icon'] ?? '◆'), ENT_QUOTES, 'UTF-8') ?></div>
<div><p class="card__meta"><?= htmlspecialchars((string)($project['type'] ?? ''), ENT_QUOTES, 'UTF-8') ?></p><h3><?= htmlspecialchars((string)($project['title'] ?? ''), ENT_QUOTES, 'UTF-8') ?></h3><p><?= htmlspecialchars((string)($project['text'] ?? ''), ENT_QUOTES, 'UTF-8') ?></p></div>
<span class="project__status"><?= htmlspecialchars((string)($project['status'] ?? ''), ENT_QUOTES, 'UTF-8') ?></span></article><?php endforeach; ?></div></section>
<section class="section minecraft" id="minecraft"><div class="minecraft__grid"><div>
<p class="eyebrow">Die Tore nach Grimholt</p><h2>Der Minecraft-Server</h2>
<p class="minecraft__lead">Diese Seite ist bereits auf die spätere Verbindung mit deinem Minecraft-Server vorbereitet. Sobald der Server aktiviert ist, kann der Status hier ausgelesen werden — ohne Datenbank und ohne Framework.</p>
<div class="server-address"><span class="server-address__label">Serveradresse</span><code><?= htmlspecialchars((string)$server['host'], ENT_QUOTES, 'UTF-8') ?>:<?= (int)$server['port'] ?></code></div>
</div><aside class="server-card" aria-live="polite"><div class="server-card__top"><span class="server-dot" id="serverDot"></span><span id="serverState">Wird geprüft …</span></div>
<div class="server-card__name" id="serverName"><?= htmlspecialchars((string)$server['name'], ENT_QUOTES, 'UTF-8') ?></div>
<div class="server-card__stats"><div><strong id="serverPlayers">—</strong><span>Spieler</span></div><div><strong id="serverVersion">—</strong><span>Version</span></div></div>
<button class="button button--outline button--small" id="refreshServer" type="button">Status erneuern</button></aside></div></section>
<section class="section workshop" id="werkstatt"><div class="workshop__inner"><p class="eyebrow">Die Werkstatt</p><h2>Welten entstehen nicht von selbst.</h2>
<p>Hier wachsen Karten, Geschichten, Minecraft-Bauten und kleine Ideen, die eines Tages groß genug sind, um eine eigene Legende zu tragen.</p>
<a class="button button--outline" href="mailto:grimholt@example.local">Eine Nachricht hinterlassen</a></div></section>
<footer class="footer"><span>Grimholt · Chronik der alten Lande</span><span>✦</span><span>Erbaut für Geschichten, die bleiben.</span></footer>
</main>
<audio id="tavernMusic" loop preload="metadata"><source src="/assets/audio/tavern.mp3" type="audio/mpeg"></audio>
<script>window.GRIMHOLT={serverApi:'/api/server-status.php'};</script><script src="/assets/js/tavern.js" defer></script>
</body></html>
