(() => {
  const toggle = document.querySelector('#themeToggle');
  const root = document.documentElement;
  const body = document.body;
  const saved = localStorage.getItem('grimholt-theme');
  if (saved === 'dark') body.classList.add('dark');

  toggle?.addEventListener('click', () => {
    body.classList.toggle('dark');
    localStorage.setItem('grimholt-theme', body.classList.contains('dark') ? 'dark' : 'light');
  });

  const input = document.querySelector('#grimholt_file');
  input?.addEventListener('change', () => {
    const file = input.files?.[0];
    if (!file) return;
    const mb = file.size / 1024 / 1024;
    if (mb > 64) {
      alert('Diese Datei ist größer als 64 MB.');
      input.value = '';
    }
  });
})();
