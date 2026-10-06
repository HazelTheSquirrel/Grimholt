const entrance = document.querySelector('#entrance');
const shell = document.querySelector('#site-shell');
const enterButton = document.querySelector('#enter-tavern');
const audio = document.querySelector('#tavern-audio');
const audioToggle = document.querySelector('#audio-toggle');
const audioStatus = document.querySelector('#audio-status');
const volume = document.querySelector('#audio-volume');

const storageKey = 'grimholt-audio-volume';

const updateAudioButton = (playing) => {
    audioStatus.textContent = playing ? 'Musik an' : 'Musik aus';
    audioToggle.setAttribute('aria-pressed', String(playing));
};

const setVolume = (value) => {
    audio.volume = Number(value);
    volume.value = String(value);

    try {
        localStorage.setItem(storageKey, String(value));
    } catch {
        // Local storage can be disabled; the player still works normally.
    }
};

const restoreVolume = () => {
    try {
        const saved = localStorage.getItem(storageKey);
        if (saved !== null && Number.isFinite(Number(saved))) {
            return Math.min(1, Math.max(0, Number(saved)));
        }
    } catch {
        // Ignore storage errors.
    }

    return 0.28;
};

const playAudio = async () => {
    try {
        await audio.play();
        updateAudioButton(true);
    } catch {
        updateAudioButton(false);
    }
};

setVolume(restoreVolume());

enterButton.addEventListener('click', async () => {
    entrance.classList.add('is-hidden');
    shell.classList.add('is-visible');
    shell.setAttribute('aria-hidden', 'false');
    await playAudio();
});

audioToggle.addEventListener('click', async () => {
    if (audio.paused) {
        await playAudio();
        return;
    }

    audio.pause();
    updateAudioButton(false);
});

volume.addEventListener('input', (event) => {
    setVolume(event.target.value);

    if (Number(event.target.value) === 0) {
        updateAudioButton(false);
    } else if (!audio.paused) {
        updateAudioButton(true);
    }
});

audio.addEventListener('play', () => updateAudioButton(true));
audio.addEventListener('pause', () => updateAudioButton(false));
audio.addEventListener('error', () => updateAudioButton(false));
