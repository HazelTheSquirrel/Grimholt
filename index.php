<?php
declare(strict_types=1);

$siteTitle = 'Grimholt';
$intro = 'Wo das Feuer länger brennt als die Nacht und alte Geschichten noch Namen tragen.';

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
?>
<!doctype html>
<html lang="de">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="theme-color" content="#0d0a08">
    <meta name="description" content="Grimholt – eine dunkle, atmosphärische Chronik zwischen Feuer, Holz und alten Geschichten.">
    <title><?= htmlspecialchars($siteTitle, ENT_QUOTES, 'UTF-8') ?></title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Cinzel:wght@500;600;700&family=Libre+Baskerville:wght@400;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="/assets/css/style.css">
</head>
<body>
<div class="page-noise" aria-hidden="true"></div>

<div class="entrance" id="entrance">
    <div class="entrance__glow" aria-hidden="true"></div>
    <div class="entrance__panel">
        <div class="sigil" aria-hidden="true">✦</div>
        <p class="eyebrow">Eine Tür im Dunkeln</p>
        <h1>Grimholt</h1>
        <p class="entrance__copy">Tritt ein. Draußen wartet die Nacht. Drinnen wartet eine Geschichte.</p>
        <button class="button button--gold" id="enter-tavern" type="button">
            <span>Die Tür öffnen</span>
            <span class="button__arrow" aria-hidden="true">→</span>
        </button>
    </div>
    <p class="entrance__hint">Am besten mit Kopfhörern</p>
</div>

<div class="site-shell" id="site-shell" aria-hidden="true">
    <header class="site-header">
        <a class="brand" href="/" aria-label="Grimholt Startseite">
            <span class="brand__crest" aria-hidden="true">✦</span>
            <span class="brand__name">Grimholt</span>
        </a>

        <nav class="main-nav" aria-label="Hauptnavigation">
            <a href="#anfang">Anfang</a>
            <a href="#chronik">Chronik</a>
            <a href="#taverne">Taverne</a>
        </nav>

        <div class="header-rule" aria-hidden="true"></div>
    </header>

    <main>
        <section class="hero" id="anfang">
            <div class="hero__ornament" aria-hidden="true">
                <span></span><i>✦</i><span></span>
            </div>
            <p class="eyebrow">Anno unbekannt · irgendwo jenseits der alten Wege</p>
            <h1>Grimholt</h1>
            <p class="hero__lead"><?= htmlspecialchars($intro, ENT_QUOTES, 'UTF-8') ?></p>
            <a class="button button--outline" href="#chronik">
                <span>Die erste Seite</span>
                <span class="button__arrow" aria-hidden="true">↓</span>
            </a>
            <div class="hero__scroll" aria-hidden="true"><span></span></div>
        </section>

        <section class="section section--chronicle" id="chronik">
            <div class="section-heading">
                <p class="eyebrow">Aus den alten Seiten</p>
                <h2>Was man sich erzählt</h2>
            </div>

            <div class="chronicle-layout">
                <article class="parchment">
                    <div class="parchment__corner parchment__corner--tl" aria-hidden="true"></div>
                    <div class="parchment__corner parchment__corner--br" aria-hidden="true"></div>
                    <?php if ($entries !== []): ?>
                        <?php foreach ($entries as $entry): ?>
                            <p class="entry-kicker"><?= htmlspecialchars((string) ($entry['title'] ?? 'Eintrag'), ENT_QUOTES, 'UTF-8') ?></p>
                            <p class="entry-text"><?= htmlspecialchars((string) ($entry['text'] ?? ''), ENT_QUOTES, 'UTF-8') ?></p>
                        <?php endforeach; ?>
                    <?php else: ?>
                        <p class="entry-kicker">Die erste Seite</p>
                        <p class="entry-text">Noch ist die Seite leer. Manche Geschichten brauchen Zeit, bevor sie geschrieben werden.</p>
                    <?php endif; ?>
                </article>

                <aside class="chronicle-note">
                    <span class="note-mark" aria-hidden="true">“</span>
                    <p>Nicht alles, was wahr ist, wurde aufgeschrieben. Und nicht alles, was aufgeschrieben wurde, ist wahr.</p>
                    <span class="note-signature">— aus einem unbekannten Wirtshaus</span>
                </aside>
            </div>
        </section>

        <section class="section section--atmosphere" aria-labelledby="atmosphere-title">
            <div class="atmosphere-card">
                <div class="atmosphere-card__visual" aria-hidden="true">
                    <div class="moon"></div>
                    <div class="ridge ridge--one"></div>
                    <div class="ridge ridge--two"></div>
                    <div class="mist mist--one"></div>
                    <div class="mist mist--two"></div>
                </div>
                <div class="atmosphere-card__copy">
                    <p class="eyebrow">Jenseits des Feuers</p>
                    <h2 id="atmosphere-title">Die Nacht ist groß.</h2>
                    <p>Hinter den Fenstern endet das Licht. Dahinter beginnen Wege, von denen man am Kamin nur leise spricht.</p>
                    <span class="small-rule" aria-hidden="true"></span>
                </div>
            </div>
        </section>

        <section class="section section--tavern" id="taverne">
            <div class="tavern-frame">
                <div class="tavern-frame__inner">
                    <p class="eyebrow">Noch ist Platz am Feuer</p>
                    <h2>Die Taverne</h2>
                    <p>Bleib eine Weile. Der Abend hat gerade erst begonnen.</p>

                    <div class="audio-control" aria-label="Hintergrundmusik">
                        <button class="audio-control__button" id="audio-toggle" type="button" aria-pressed="true">
                            <span class="audio-icon" aria-hidden="true">♫</span>
                            <span id="audio-status">Musik an</span>
                        </button>
                        <label class="volume-control" for="audio-volume">
                            <span class="volume-control__label">Lautstärke</span>
                            <input id="audio-volume" type="range" min="0" max="1" step="0.01" value="0.28" aria-label="Musiklautstärke">
                        </label>
                    </div>
                </div>
            </div>
            <audio id="tavern-audio" loop preload="metadata">
                <source src="/assets/audio/tavern.mp3" type="audio/mpeg">
            </audio>
        </section>
    </main>

    <footer class="site-footer">
        <div>
            <span class="footer-brand">Grimholt</span>
            <span class="footer-subtitle">Eine Chronik ohne Ende.</span>
        </div>
        <span class="footer-mark" aria-hidden="true">✦</span>
        <span class="footer-meta">Gebaut für die Nacht.</span>
    </footer>
</div>

<script src="/assets/js/app.js" defer></script>
</body>
</html>
