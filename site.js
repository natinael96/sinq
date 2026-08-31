/* Language toggle. The page ships both languages; this only decides which one
   the document shows, and remembers the choice. No network, no tracking:
   the same principle as the app. The pre-paint half of this lives inline in each
   page's <head> so a returning Amharic reader never sees English flash first. */
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
