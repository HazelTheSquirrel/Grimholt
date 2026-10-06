<?php
declare(strict_types=1);

$dataFile = __DIR__ . '/data/site.json';
$data = is_file($dataFile) ? json_decode((string) file_get_contents($dataFile), true) : [];
$data = is_array($data) ? $data : [];

$notice = null;
$noticeType = 'success';

if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['upload'])) {
    if (!isset($_FILES['grimholt_file']) || $_FILES['grimholt_file']['error'] !== UPLOAD_ERR_OK) {
        $notice = 'Upload fehlgeschlagen. Bitte wähle eine Datei aus.';
        $noticeType = 'error';
    } else {
        $file = $_FILES['grimholt_file'];
        $max = 64 * 1024 * 1024;
        $allowed = ['image/jpeg', 'image/png', 'image/webp', 'image/gif', 'application/pdf', 'text/plain'];
        $mime = (new finfo(FILEINFO_MIME_TYPE))->file($file['tmp_name']);

        if ($file['size'] > $max) {
            $notice = 'Die Datei ist größer als 64 MB.';
            $noticeType = 'error';
        } elseif (!in_array($mime, $allowed, true)) {
            $notice = 'Dieser Dateityp ist in der Demo nicht erlaubt.';
            $noticeType = 'error';
        } else {
            $dir = __DIR__ . '/storage/uploads';
            if (!is_dir($dir)) mkdir($dir, 0775, true);
            $safeName = bin2hex(random_bytes(8)) . '-' . preg_replace('/[^a-zA-Z0-9._-]/', '-', basename($file['name']));
            $target = $dir . '/' . $safeName;

            if (move_uploaded_file($file['tmp_name'], $target)) {
                $notice = 'Datei erfolgreich hochgeladen: ' . htmlspecialchars($safeName, ENT_QUOTES, 'UTF-8');
            } else {
                $notice = 'Die Datei konnte nicht gespeichert werden.';
                $noticeType = 'error';
            }
        }
    }
}

$stats = $data['stats'] ?? [
    ['value' => '03', 'label' => 'Districts'],
    ['value' => '12', 'label' => 'Lore entries'],
    ['value' => '64 MB', 'label' => 'Upload limit']
];
?>
<!doctype html>
<html lang="de">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="description" content="Grimholt — eine dunkle, immersive PHP/Nginx Demo.">
    <title><?= htmlspecialchars($data['title'] ?? 'Grimholt', ENT_QUOTES, 'UTF-8') ?></title>
    <link rel="stylesheet" href="/assets/css/style.css">
</head>
<body>
<div class="site-shell">
<header class="topbar">
    <a class="brand" href="#top" aria-label="Grimholt Startseite"><span>G</span> GRIMHOLT</a>
    <nav aria-label="Hauptnavigation">
        <a href="#world">Welt</a>
        <a href="#lore">Lore</a>
        <a href="#upload">Upload</a>
    </nav>
    <button class="theme-toggle" type="button" id="themeToggle" aria-label="Darstellung wechseln">◐</button>
</header>

<main id="top">
<section class="hero">
    <div class="hero__content">
        <p class="eyebrow">THE NORTHERN FRONTIER · DEMO 01</p>
        <h1><?= htmlspecialchars($data['hero']['headline'] ?? 'Where the fog remembers.', ENT_QUOTES, 'UTF-8') ?></h1>
        <p class="hero__lead"><?= htmlspecialchars($data['hero']['lead'] ?? 'Eine immersive Grimdark-Landingpage mit PHP, JSON-Daten und Nginx-Optimierung.', ENT_QUOTES, 'UTF-8') ?></p>
        <div class="hero__actions">
            <a class="button button--primary" href="#world">Welt betreten <span>↘</span></a>
            <a class="button button--ghost" href="#upload">Datei testen</a>
        </div>
    </div>
    <div class="hero__visual" aria-label="Illustration einer nebligen Festung">
        <div class="moon"></div>
        <div class="mountain mountain--back"></div>
        <div class="mountain mountain--front"></div>
        <div class="fort">
            <i></i><i></i><i></i>
        </div>
        <div class="fog"></div>
    </div>
</section>

<section class="stats" aria-label="Projektstatus">
<?php foreach ($stats as $stat): ?>
    <div><strong><?= htmlspecialchars((string)$stat['value'], ENT_QUOTES, 'UTF-8') ?></strong><span><?= htmlspecialchars((string)$stat['label'], ENT_QUOTES, 'UTF-8') ?></span></div>
<?php endforeach; ?>
</section>

<section class="intro" id="world">
    <div class="section-label">01 / THE WORLD</div>
    <div>
        <p class="eyebrow">GRIMHOLT · EST. UNKNOWN</p>
        <h2>Eine Stadt, die<br><em>nicht schläft.</em></h2>
        <p class="body-copy">Hinter den Mauern von Grimholt liegt ein Ort zwischen Ruine und Zukunft. Dieses Demo-Projekt zeigt, wie eine stark gestaltete Oberfläche trotzdem auf einer kleinen, verständlichen PHP-Struktur laufen kann.</p>
    </div>
</section>

<section class="cards" id="lore">
<?php foreach (($data['districts'] ?? []) as $index => $district): ?>
    <article class="card card--<?= ($index % 3) + 1 ?>">
        <div class="card__number">0<?= $index + 1 ?></div>
        <div class="card__art"></div>
        <p class="eyebrow"><?= htmlspecialchars((string)($district['tag'] ?? 'DISTRICT'), ENT_QUOTES, 'UTF-8') ?></p>
        <h3><?= htmlspecialchars((string)($district['name'] ?? 'Unknown'), ENT_QUOTES, 'UTF-8') ?></h3>
        <p><?= htmlspecialchars((string)($district['description'] ?? ''), ENT_QUOTES, 'UTF-8') ?></p>
    </article>
<?php endforeach; ?>
</section>

<section class="dark-section">
    <p class="eyebrow">02 / THE SIGNAL</p>
    <h2>Some stories<br><em>leave a trace.</em></h2>
    <p class="dark-copy">Die Demo besitzt bewusst keine Build-Pipeline. Dateien sind direkt auslieferbar, Assets werden von Nginx gecacht und PHP übernimmt nur dynamische Teile.</p>
</section>

<section class="upload-section" id="upload">
    <div>
        <p class="eyebrow">03 / FILE DROP</p>
        <h2>Bring something<br><em>from outside.</em></h2>
    </div>
    <div class="upload-panel">
        <?php if ($notice): ?>
            <div class="notice notice--<?= $noticeType ?>"><?= $notice ?></div>
        <?php endif; ?>
        <form method="post" enctype="multipart/form-data">
            <label for="grimholt_file">Datei auswählen</label>
            <input id="grimholt_file" name="grimholt_file" type="file" accept=".jpg,.jpeg,.png,.webp,.gif,.pdf,.txt" required>
            <button class="button button--primary" type="submit" name="upload">Upload starten <span>↑</span></button>
            <small>JPG, PNG, WEBP, GIF, PDF oder TXT · maximal 64 MB</small>
        </form>
    </div>
</section>
</main>

<footer class="footer">
    <span>GRIMHOLT / DEMO</span>
    <span>PHP + NGINX / <?= date('Y') ?></span>
</footer>
</div>
<script src="/assets/js/app.js" defer></script>
</body>
</html>
