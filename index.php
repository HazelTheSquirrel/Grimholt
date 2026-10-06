<?php
declare(strict_types=1);
$siteTitle = 'Grimholt';
$intro = 'Eine Chronik aus dunklem Holz, alten Geschichten und vergessenen Wegen.';
?>
<!doctype html>
<html lang="de">
<head>
<meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="description" content="Grimholt – eine schlichte Chronik im Geist einer alten Taverne.">
<title><?= htmlspecialchars($siteTitle, ENT_QUOTES, 'UTF-8') ?></title>
<link rel="preconnect" href="https://fonts.googleapis.com"><link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Cinzel:wght@500;600;700&family=Libre+Baskerville:wght@400;700&display=swap" rel="stylesheet">
<link rel="stylesheet" href="/assets/css/style.css">
</head>
<body>
<div class="entrance" id="entrance">
  <div class="entrance__panel">
    <p class="eyebrow">Die Chronik von Grimholt</p><h1>Betritt die Taverne</h1>
    <p>Hinter dieser Tür warten Geschichten, die besser im Kerzenschein erzählt werden.</p>
    <button class="button button--gold" id="enter-tavern" type="button">Taverne betreten</button>
  </div>
</div>
<div class="site-shell" id="site-shell" aria-hidden="true">
<header class="site-header">
  <a class="brand" href="/" aria-label="Grimholt Startseite"><span class="brand__mark">✦</span><span>Grimholt</span></a>
  <nav aria-label="Hauptnavigation"><a href="#chronik">Chronik</a><a href="#welt">Die Lande</a><a href="#taverne">Taverne</a></nav>
</header>
<main>
<section class="hero"><p class="eyebrow">Anno unbekannt</p><h1>Grimholt</h1><p class="hero__lead"><?= htmlspecialchars($intro, ENT_QUOTES, 'UTF-8') ?></p><a class="button" href="#chronik">Die Chronik öffnen</a></section>
<section class="section" id="chronik"><div class="section__heading"><p class="eyebrow">Aus den alten Seiten</p><h2>Die Chronik</h2></div>
<div class="parchment"><p>Man sagt, Grimholt sei älter als die Straßen, die zu ihm führen. Reisende finden hier Schutz vor dem Regen, ein Feuer für die Nacht und gelegentlich eine Geschichte, die sie lieber vergessen hätten.</p><p>Was wahr ist und was am Kamin erfunden wurde, mag jeder selbst entscheiden. Die Seiten dieser Chronik bewahren beides.</p></div></section>
<section class="section section--split" id="welt"><div><p class="eyebrow">Jenseits der Tür</p><h2>Die Lande</h2></div><div class="card"><p>Wälder ohne Ende. Alte Steine unter Moos. Nebel über stillen Mooren. Und Wege, deren Ziel kein Wirt kennt.</p><p class="muted">Weitere Orte erhalten ihre Seiten, wenn ihre Geschichten geschrieben sind.</p></div></section>
<section class="section section--tavern" id="taverne"><p class="eyebrow">Am Feuer</p><h2>Die Taverne</h2><p>Lehn dich zurück. Das Feuer knistert, der Krug steht bereit.</p><button class="audio-toggle" id="audio-toggle" type="button" aria-pressed="true">♫ Musik an</button><audio id="tavern-audio" loop preload="metadata"><source src="/assets/audio/tavern.mp3" type="audio/mpeg"></audio></section>
</main>
<footer class="site-footer"><span>Grimholt</span><span>Eine Chronik ohne Ende.</span></footer>
</div>
<script src="/assets/js/app.js" defer></script>
</body></html>