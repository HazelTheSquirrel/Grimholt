<?php
declare(strict_types=1);

$siteTitle = 'Grimholt';
$intro = 'Eine Stadt, die man nicht findet. Man landet darin.';
$yearMark = 'MMXXVI';

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
    'title' => 'Die erste Seite',
    'text' => 'Noch ist die Seite leer. Manche Geschichten beginnen erst, wenn jemand die Tür hinter sich schließt.'
];
?>
<!doctype html>
<html lang="de">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="theme-color" content="#11120f">
    <meta name="description" content="Grimholt — eine eigenwillige Fantasy-Welt zwischen alten Mauern, Gerüchten und nächtlichen Straßen.">
    <title><?= htmlspecialchars($siteTitle, ENT_QUOTES, 'UTF-8') ?></title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=DM+Mono:wght@400;500&family=Libre+Baskerville:wght@400;700&family=UnifrakturCook:wght@700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="/assets/css/style.css">
</head>
<body>
<div class="grain" aria-hidden="true"></div>

<div class="entrance" id="entrance">
    <div class="entrance__door" aria-hidden="true">
        <span class="entrance__light"></span>
        <span class="entrance__handle"></span>
    </div>
    <div class="entrance__content">
        <p class="micro">ARCHIVE / <?= $yearMark ?></p>
        <div class="entrance__sigil">G</div>
        <p class="entrance__statement">Du hast Grimholt gefunden.<br>Oder Grimholt dich.</p>
        <button class="enter-link" id="enter-tavern" type="button">
            <span>eintreten</span>
            <span>↳</span>
        </button>
        <p class="entrance__hint">Ton wird nach dem Eintreten aktiviert</p>
    </div>
</div>

<div class="site-shell" id="site-shell" aria-hidden="true">
    <header class="site-header">
        <a class="brand" href="#anfang" aria-label="Grimholt">
            <span class="brand__mark">G</span>
            <span>GRIMHOLT</span>
        </a>

        <div class="header-status">
            <span class="status-dot"></span>
            <span>nachts geöffnet</span>
        </div>

        <nav class="main-nav" aria-label="Hauptnavigation">
            <a href="#anfang">01</a>
            <a href="#chronik">02</a>
            <a href="#taverne">03</a>
        </nav>
    </header>

    <main>
        <section class="hero" id="anfang">
            <div class="hero__rail">
                <span>G</span>
                <span>R</span>
                <span>I</span>
                <span>M</span>
                <span>H</span>
                <span>O</span>
                <span>L</span>
                <span>T</span>
            </div>

            <div class="hero__copy">
                <p class="micro">Feldnotiz 01 / kein verlässlicher Weg</p>
                <h1>Grim<span>holt</span></h1>
                <p class="hero__lead"><?= htmlspecialchars($intro, ENT_QUOTES, 'UTF-8') ?></p>
                <a class="hero__link" href="#chronik">
                    <span>öffne die aufzeichnung</span>
                    <span class="hero__link-line"></span>
                    <span>↓</span>
                </a>
            </div>

            <div class="hero__coordinates" aria-hidden="true">
                <span>51° N</span>
                <span>17° O</span>
                <span>?</span>
            </div>

            <div class="hero__shape hero__shape--one" aria-hidden="true"></div>
            <div class="hero__shape hero__shape--two" aria-hidden="true"></div>
        </section>

        <section class="chronicle" id="chronik">
            <div class="chronicle__index">
                <span>02</span>
                <span>aufzeichnung</span>
            </div>

            <div class="chronicle__main">
                <div class="chronicle__heading">
                    <p class="micro">Was überliefert wurde</p>
                    <h2><?= htmlspecialchars((string) ($firstEntry['title'] ?? 'Die erste Seite'), ENT_QUOTES, 'UTF-8') ?></h2>
                </div>

                <div class="chronicle__body">
                    <p class="chronicle__dropcap"><?= htmlspecialchars(mb_substr((string) ($firstEntry['text'] ?? ''), 0, 1), ENT_QUOTES, 'UTF-8') ?></p>
                    <p><?= htmlspecialchars((string) ($firstEntry['text'] ?? ''), ENT_QUOTES, 'UTF-8') ?></p>
                    <p class="chronicle__aside">Man erzählt sich Dinge, wenn es draußen dunkel wird. Die meisten werden am Morgen anders erzählt.</p>
                </div>
            </div>

            <aside class="chronicle__stamp">
                <span>UNGEPRÜFT</span>
                <strong>NOCH NICHT<br>VERGESSEN</strong>
                <small>ARCHIV 001</small>
            </aside>
        </section>

        <section class="night" aria-labelledby="night-title">
            <div class="night__sky" aria-hidden="true">
                <span class="star star--a"></span>
                <span class="star star--b"></span>
                <span class="star star--c"></span>
                <span class="moon"></span>
                <span class="horizon"></span>
            </div>
            <div class="night__copy">
                <p class="micro">03 / nach mitternacht</p>
                <h2 id="night-title">Hier endet<br>die Karte.</h2>
                <p>Was dahinter liegt, gehört nicht auf eine Startseite.</p>
            </div>
        </section>

        <section class="tavern" id="taverne">
            <div class="tavern__index">03</div>
            <div class="tavern__copy">
                <p class="micro">soundtrack</p>
                <h2>Bleib noch.</h2>
                <p>Ein Feuer irgendwo hinter der Wand. Schritte im Flur. Der Rest ist deine Angelegenheit.</p>
            </div>

            <div class="audio-dock">
                <button class="audio-dock__toggle" id="audio-toggle" type="button" aria-pressed="true">
                    <span class="audio-dock__glyph">◉</span>
                    <span id="audio-status">ton an</span>
                </button>
                <label class="audio-dock__volume" for="audio-volume">
                    <span>volume</span>
                    <input id="audio-volume" type="range" min="0" max="1" step="0.01" value="0.28" aria-label="Musiklautstärke">
                </label>
            </div>
        </section>
    </main>

    <footer class="site-footer">
        <span>GRIMHOLT / <?= $yearMark ?></span>
        <span>keine legende. nur spuren.</span>
    </footer>
</div>

<audio id="tavern-audio" loop preload="metadata">
    <source src="/assets/audio/tavern.mp3" type="audio/mpeg">
</audio>

<script src="/assets/js/app.js" defer></script>
</body>
</html>
