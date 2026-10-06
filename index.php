<?php
declare(strict_types=1);

$siteTitle = 'Grimholt';
$intro = 'Eine Stadt zwischen dem, was erzählt wird, und dem, was wirklich dort geschieht.';
$year = '2026';

function loadJson(string $path): array
{
    if (!is_file($path)) {
        return [];
    }

    $data = json_decode((string) file_get_contents($path), true);

    return is_array($data) ? $data : [];
}

$lore = loadJson(__DIR__ . '/data/lore.json');
$entries = $lore['entries'] ?? [];
$firstEntry = $entries[0] ?? [
    'title' => 'Grimholt',
    'text' => 'Die Stadt beginnt dort, wo der Weg aufhört.'
];
?>
<!doctype html>
<html lang="de">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="theme-color" content="#d9ddd5">
    <meta name="description" content="Grimholt — eine Stadt zwischen dem, was erzählt wird, und dem, was wirklich dort geschieht.">
    <title><?= htmlspecialchars($siteTitle, ENT_QUOTES, 'UTF-8') ?></title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=DM+Mono:wght@400;500&family=Manrope:wght@400;500;600;700&family=Instrument+Serif:ital@0;1&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="/assets/css/style.css">
</head>
<body>
<div class="page">

    <header class="topbar">
        <a class="logo" href="#top" aria-label="Grimholt Startseite">G.</a>
        <span class="topbar__place">GRIMHOLT / NACHTSTADT</span>
        <nav aria-label="Hauptnavigation">
            <a href="#stadt">Stadt</a>
            <a href="#nacht">Nacht</a>
            <a href="#klang">Klang</a>
        </nav>
        <button class="sound-button" id="audio-toggle" type="button" aria-pressed="false">
            <span class="sound-button__dot"></span>
            <span id="audio-status">Klang aus</span>
        </button>
    </header>

    <main id="top">
        <section class="hero">
            <div class="hero__intro">
                <p class="eyebrow">01 — Eingang</p>
                <h1>Grim<span>holt</span></h1>
                <p class="hero__claim"><?= htmlspecialchars($intro, ENT_QUOTES, 'UTF-8') ?></p>
                <a class="arrow-link" href="#stadt">Entdecken <span>↘</span></a>
            </div>

            <div class="hero-art" aria-hidden="true">
                <div class="hero-art__moon"></div>
                <div class="hero-art__glow"></div>
                <div class="hero-art__hill hero-art__hill--back"></div>
                <div class="hero-art__hill hero-art__hill--front"></div>
                <div class="tower tower--one"><i></i><b></b></div>
                <div class="tower tower--two"><i></i><b></b></div>
                <div class="tower tower--three"><i></i><b></b></div>
                <div class="hero-art__windows"></div>
                <div class="hero-art__mist"></div>
            </div>

            <div class="hero__meta">
                <span>52° 12' N</span>
                <span>10° 33' O</span>
                <span><?= $year ?></span>
            </div>
        </section>

        <section class="intro-band" id="stadt">
            <p class="eyebrow">02 — Die Stadt</p>
            <div class="intro-band__copy">
                <h2>Man kommt nicht<br><em>hierher.</em> Man bleibt.</h2>
                <p><?= htmlspecialchars((string) ($firstEntry['text'] ?? ''), ENT_QUOTES, 'UTF-8') ?></p>
            </div>
        </section>

        <section class="city-grid" aria-label="Impressionen aus Grimholt">
            <div class="city-grid__label">ORTE / MOMENTE</div>
            <article class="scene scene--street">
                <div class="scene__image" aria-hidden="true">
                    <span class="lamp lamp--one"></span>
                    <span class="lamp lamp--two"></span>
                    <span class="street__building street__building--one"></span>
                    <span class="street__building street__building--two"></span>
                </div>
                <div class="scene__caption"><strong>Die schmale Straße</strong><span>Wenn es regnet, glänzt sie wie schwarzes Glas.</span></div>
            </article>
            <article class="scene scene--tower">
                <div class="scene__image" aria-hidden="true"><span class="tower-card__moon"></span><span class="tower-card__spire"></span><span class="tower-card__wall"></span></div>
                <div class="scene__caption"><strong>Über den Dächern</strong><span>Kein Fenster ist zu hoch für ein Gerücht.</span></div>
            </article>
            <article class="scene scene--fire">
                <div class="scene__image" aria-hidden="true"><span class="fire"></span><span class="fire__smoke"></span></div>
                <div class="scene__caption"><strong>Licht im Süden</strong><span>Manchmal ist noch jemand wach.</span></div>
            </article>
        </section>

        <section class="night" id="nacht">
            <div class="night__number">03</div>
            <div class="night__copy">
                <p class="eyebrow">Nach Sonnenuntergang</p>
                <h2>Die Nacht<br><em>gehört dir.</em></h2>
                <p>Keine Karte. Keine Führung. Nur die Straßen, die Geräusche hinter den Fenstern und die Frage, warum du noch immer weitergehst.</p>
            </div>
            <div class="night__moon" aria-hidden="true"></div>
            <div class="night__line" aria-hidden="true"></div>
        </section>

        <section class="sound-section" id="klang">
            <div>
                <p class="eyebrow">04 — Atmosphäre</p>
                <h2>Mach es<br><em>laut.</em></h2>
            </div>
            <div class="sound-section__copy">
                <p>Ein wenig Regen. Holz, das arbeitet. Stimmen, die durch eine Wand kommen. Der Klang von Grimholt ist nicht Hintergrundmusik.</p>
                <div class="player">
                    <button class="player__button" id="player-toggle" type="button" aria-pressed="false">
                        <span id="player-label">Klang einschalten</span>
                        <span>→</span>
                    </button>
                    <label>
                        <span>Lautstärke</span>
                        <input id="audio-volume" type="range" min="0" max="1" step="0.01" value="0.28" aria-label="Lautstärke">
                    </label>
                </div>
            </div>
        </section>
    </main>

    <footer class="footer">
        <span>GRIMHOLT</span>
        <span>Eine Stadt. Mehr nicht.</span>
        <span><?= $year ?></span>
    </footer>
</div>

<audio id="tavern-audio" loop preload="metadata">
    <source src="/assets/audio/tavern.mp3" type="audio/mpeg">
</audio>
<script src="/assets/js/app.js" defer></script>
</body>
</html>
