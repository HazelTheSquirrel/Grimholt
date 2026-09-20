(() => {
  const entry = document.querySelector('#entry');
  const enter = document.querySelector('#enterTavern');
  const site = document.querySelector('#site');
  const music = document.querySelector('#tavernMusic');
  const toggle = document.querySelector('#musicToggle');

  const setMusicIcon = () => {
    toggle.textContent = music.paused ? '♫' : 'Ⅱ';
    toggle.setAttribute('aria-label', music.paused ? 'Musik abspielen' : 'Musik pausieren');
  };

  enter.addEventListener('click', async () => {
    music.volume = 0.22;
    try { await music.play(); } catch (_) {}
    document.body.classList.add('entered');
    entry.classList.add('is-hidden');
    site.setAttribute('aria-hidden', 'false');
    setMusicIcon();
  });

  toggle.addEventListener('click', async () => {
    if (music.paused) {
      try { await music.play(); } catch (_) {}
    } else {
      music.pause();
    }
    setMusicIcon();
  });

  music.addEventListener('play', setMusicIcon);
  music.addEventListener('pause', setMusicIcon);
})();
