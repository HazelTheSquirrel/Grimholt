<?php
declare(strict_types=1);

$projects = json_decode((string) file_get_contents(__DIR__ . '/data/projects.json'), true, 512, JSON_THROW_ON_ERROR);
$lore = json_decode((string) file_get_contents(__DIR__ . '/data/lore.json'), true, 512, JSON_THROW_ON_ERROR);
?><!doctype html>
<html lang="de">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <meta name="description" content="Grimholt – Chroniken, Rollenspiel, LARP und Minecraft-Projekte.">
  <title>Grimholt — Chronik der alten Lande</title>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Cinzel:wght@500;600;700;800&family=Crimson+Text:wght@400;600&display=swap" rel="stylesheet">
  <link rel="stylesheet" href="/assets/css/style.css">
</head>
<body>
  <div class="grain" aria-hidden="true"></div>

  <div class="entry" id="entry">
    <div class="entry__seal">✦</div>
    <p class="eyebrow">Eine Chronik aus den alten Landen</p>
    <h1>Grimholt</h1>
    <p class="entry__text">Draußen ist die Nacht kalt. Drinnen brennt noch Licht.</p>
    <button class="button button--gold" id="enterTavern" type="button">
      <span aria-hidden="true">⚔</span> Taverne betreten
    </button>
    <p class="entry__hint">Mit dem Eintritt wird die Taverne von Musik erfüllt.</p>
  </div>

  <main class="site-shell" id="site" aria-hidden="true">
    <header class="masthead">
      <a class="brand" href="/" aria-label="Grimholt Startseite">
        <span class="brand__mark">✦</span>
        <span>Grimholt</span>
      </a>
      <nav class="nav" aria-label="Hauptnavigation">
        <a href="#chronik">Chronik</a>
        <a href="#abenteuer">Abenteuer</a>
        <a href="#werkstatt">Werkstatt</a>
      </nav>
      <button class="music-toggle" id="musicToggle" type="button" aria-label="Musik pausieren">♫</button>
    </header>

    <section class="hero">
      <p class="eyebrow">Willkommen, Reisender</p>
      <h1>Wo Geschichten<br><em>noch Gewicht haben.</em></h1>
      <p class="hero__lead">Grimholt ist eine kleine Ecke zwischen Wirklichkeit und Legende — für Rollenspiel, LARP, Minecraft und jene, die lieber selbst am Feuer sitzen als nur davon zu lesen.</p>
      <a class="scroll-cue" href="#chronik">Die Chronik öffnen <span>↓</span></a>
    </section>

    <section class="section" id="chronik">
      <div class="section__heading">
        <p class="eyebrow">Aus den Annalen</p>
        <h2>Die Chronik</h2>
      </div>
      <div class="lore-grid">
        <?php foreach ($lore as $entry): ?>
          <article class="card card--lore">
            <span class="card__sigil"><?= htmlspecialchars($entry['sigil'], ENT_QUOTES, 'UTF-8') ?></span>
            <p class="card__meta"><?= htmlspecialchars($entry['era'], ENT_QUOTES, 'UTF-8') ?></p>
            <h3><?= htmlspecialchars($entry['title'], ENT_QUOTES, 'UTF-8') ?></h3>
            <p><?= htmlspecialchars($entry['text'], ENT_QUOTES, 'UTF-8') ?></p>
          </article>
        <?php endforeach; ?>
      </div>
    </section>

    <section class="section section--dark" id="abenteuer">
      <div class="section__heading">
        <p class="eyebrow">Was vor dem Feuer liegt</p>
        <h2>Abenteuer</h2>
      </div>
      <div class="project-list">
        <?php foreach ($projects as $project): ?>
          <article class="project">
            <div class="project__icon"><?= htmlspecialchars($project['icon'], ENT_QUOTES, 'UTF-8') ?></div>
            <div>
              <p class="card__meta"><?= htmlspecialchars($project['type'], ENT_QUOTES, 'UTF-8') ?></p>
              <h3><?= htmlspecialchars($project['title'], ENT_QUOTES, 'UTF-8') ?></h3>
              <p><?= htmlspecialchars($project['text'], ENT_QUOTES, 'UTF-8') ?></p>
            </div>
            <span class="project__status"><?= htmlspecialchars($project['status'], ENT_QUOTES, 'UTF-8') ?></span>
          </article>
        <?php endforeach; ?>
      </div>
    </section>

    <section class="section workshop" id="werkstatt">
      <div class="workshop__inner">
        <p class="eyebrow">Die Werkstatt</p>
        <h2>Welten entstehen nicht von selbst.</h2>
        <p>Hier wachsen Karten, Geschichten, Minecraft-Bauten und kleine Ideen, die eines Tages groß genug sind, um eine eigene Legende zu tragen.</p>
        <a class="button button--outline" href="mailto:grimholt@example.local">Eine Nachricht hinterlassen</a>
      </div>
    </section>

    <footer class="footer">
      <span>Grimholt · Chronik der alten Lande</span>
      <span>✦</span>
      <span>Erbaut für Geschichten, die bleiben.</span>
    </footer>
  </main>

  <audio id="tavernMusic" loop preload="metadata">
    <source src="/assets/audio/tavern.mp3" type="audio/mpeg">
  </audio>
  <script src="/assets/js/tavern.js" defer></script>
</body>
</html>
