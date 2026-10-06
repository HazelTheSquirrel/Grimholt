const audio = document.querySelector('#tavern-audio');
const toggle = document.querySelector('#audio-toggle');
const playerToggle = document.querySelector('#player-toggle');
const status = document.querySelector('#audio-status');
const playerLabel = document.querySelector('#player-label');
const volume = document.querySelector('#audio-volume');

const storageKey = 'grimholt-volume';

const setState = (on) => {
    status.textContent = on ? 'Klang an' : 'Klang aus';
    playerLabel.textContent = on ? 'Klang ausschalten' : 'Klang einschalten';
    toggle.classList.toggle('is-on', on);
    toggle.setAttribute('aria-pressed', String(on));
    playerToggle.setAttribute('aria-pressed', String(on));
};

const setVolume = (value) => {
    audio.volume = Number(value);
    volume.value = String(value);
    try { localStorage.setItem(storageKey, String(value)); } catch {}
};

const restoreVolume = () => {
    try {
        const value = Number(localStorage.getItem(storageKey));
        return Number.isFinite(value) ? Math.min(1, Math.max(0, value)) : 0.28;
    } catch {
        return 0.28;
    }
};

const toggleAudio = async () => {
    if (audio.paused) {
        try {
            await audio.play();
            setState(true);
        } catch {
            setState(false);
        }
        return;
    }
    audio.pause();
    setState(false);
};

setVolume(restoreVolume());
setState(false);

toggle.addEventListener('click', toggleAudio);
playerToggle.addEventListener('click', toggleAudio);
volume.addEventListener('input', (event) => {
    setVolume(event.target.value);
    if (Number(event.target.value) === 0 && !audio.paused) setState(false);
});

audio.addEventListener('play', () => setState(true));
audio.addEventListener('pause', () => setState(false));
audio.addEventListener('error', () => setState(false));
