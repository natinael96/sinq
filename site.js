/* Language toggle. The page ships both languages; this only decides which one
   the document shows, and remembers the choice. No network, no tracking:
   the same principle as the app. The pre-paint half of this lives inline in
   each page's <head> so a returning Amharic reader never sees English flash
   first. */
(function () {
  var root = document.documentElement;
  var btn = document.getElementById('lang');
  if (!btn) return;

  function apply(lang) {
    root.setAttribute('data-lang', lang);
    root.setAttribute('lang', lang);
    btn.setAttribute('aria-label', lang === 'am' ? 'Switch to English' : 'ወደ አማርኛ ቀይር');
    try { localStorage.setItem('sinq-lang', lang); } catch (e) { /* private mode */ }
  }

  apply(root.getAttribute('data-lang') === 'am' ? 'am' : 'en');

  btn.addEventListener('click', function () {
    apply(root.getAttribute('data-lang') === 'am' ? 'en' : 'am');
  });
})();

/* Reveal on scroll.

   Two safeguards, because the failure mode here is content that never
   becomes visible at all:

   1. The hidden state is applied by this script, never by the stylesheet,
      so a reader with no JavaScript gets the whole page.
   2. An IntersectionObserver that never fires would leave everything
      hidden forever. So we observe one element that is already on screen
      as a liveness probe; if it does not report back promptly, the
      observer is not working in this context and every hidden element is
      restored immediately. */
(function () {
  if (!('IntersectionObserver' in window)) return;
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;

  var targets = document.querySelectorAll(
    'main > section, .band-in, .group, .release, .card, .steps > li'
  );
  if (!targets.length) return;

  var hidden = [];
  var alive = false;

  function showAll() {
    hidden.forEach(function (el) {
      el.classList.remove('reveal');
      el.style.transitionDelay = '';
    });
    hidden = [];
  }

  var io = new IntersectionObserver(function (entries) {
    alive = true;
    entries.forEach(function (e) {
      if (!e.isIntersecting) return;
      e.target.classList.add('in');
      io.unobserve(e.target);
    });
  }, { rootMargin: '0px 0px -8% 0px', threshold: 0.04 });

  /* The probe: something guaranteed to be in view at load. */
  var probe = document.querySelector('.hero, .page-head, main');
  if (probe) io.observe(probe);

  targets.forEach(function (el, i) {
    /* Anything already on screen at load stays put; only what the reader
       scrolls to gets the entrance. */
    if (el.getBoundingClientRect().top < window.innerHeight * 0.92) return;
    el.classList.add('reveal');
    el.style.transitionDelay = (Math.min(i % 4, 3) * 55) + 'ms';
    hidden.push(el);
    io.observe(el);
  });

  if (probe) io.unobserve(probe);

  window.setTimeout(function () { if (!alive) showAll(); }, 1200);
})();
