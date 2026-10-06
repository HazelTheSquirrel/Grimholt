(() => {
  'use strict';
  const $ = (s) => document.querySelector(s);
  const entry=$('#entry'), enter=$('#enterTavern'), site=$('#site'), music=$('#tavernMusic'), toggle=$('#musicToggle');
  const dot=$('#serverDot'), state=$('#serverState'), name=$('#serverName'), players=$('#serverPlayers'), version=$('#serverVersion'), refresh=$('#refreshServer');
  const setMusicIcon=()=>{if(!toggle||!music)return;toggle.textContent=music.paused?'♫':'Ⅱ';toggle.setAttribute('aria-label',music.paused?'Musik abspielen':'Musik pausieren');toggle.classList.toggle('is-playing',!music.paused);};
  const loadServerStatus=async()=>{
    if(!state)return; state.textContent='Wird geprüft …'; dot?.classList.remove('is-online','is-offline');
    try{
      const r=await fetch(window.GRIMHOLT?.serverApi||'/api/server-status.php',{headers:{Accept:'application/json'},cache:'no-store'});
      if(!r.ok)throw new Error();
      const d=await r.json(); name.textContent=d.name||'Grimholt'; players.textContent=d.online?String(d.players?.online??0):'—'; version.textContent=d.version||'—';
      state.textContent=d.online?'Online':(d.enabled?'Offline':'Noch nicht verbunden'); dot?.classList.add(d.online?'is-online':'is-offline');
    }catch(_){state.textContent='Status nicht erreichbar';dot?.classList.add('is-offline');players.textContent='—';version.textContent='—';}
  };
  enter?.addEventListener('click',async()=>{if(music){music.volume=.18;try{await music.play();}catch(_){}}document.body.classList.add('entered');entry?.classList.add('is-hidden');site?.setAttribute('aria-hidden','false');setMusicIcon();loadServerStatus();});
  toggle?.addEventListener('click',async()=>{if(!music)return;if(music.paused){try{await music.play();}catch(_){}}else music.pause();setMusicIcon();});
  music?.addEventListener('play',setMusicIcon);music?.addEventListener('pause',setMusicIcon);refresh?.addEventListener('click',loadServerStatus);setMusicIcon();
})();
