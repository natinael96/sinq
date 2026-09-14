/* Sinq site behaviour. No network, no tracking: the same principle as
   the app. */

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

  /* Third safeguard, and the one that actually caught this: the probe firing
     once set `alive` and disarmed the timeout below, so an observer that
     reported the probe and then went quiet — which is what happens when the
     page scrolls inside a container the observer is not watching — left
     everything after the fold hidden for good. A plain scroll listener does
     not depend on any of that. */
  function sweep() {
    if (!hidden.length) { return; }
    var still = [];
    hidden.forEach(function (el) {
      /* Any overlap with the viewport counts, not just a top edge that has
         crossed the 0.92 line: the feature groups are taller than a phone
         screen, so a group whose top had already passed above the fold sat
         invisible while the reader looked straight at where it should be. */
      var r = el.getBoundingClientRect();
      if (r.top < window.innerHeight && r.bottom > 0) {
        el.classList.add('in');
        io.unobserve(el);
      } else {
        still.push(el);
      }
    });
    hidden = still;
    if (!hidden.length) {
      window.removeEventListener('scroll', sweep);
      window.removeEventListener('resize', sweep);
    }
  }
  window.addEventListener('scroll', sweep, { passive: true });
  window.addEventListener('resize', sweep);

  window.setTimeout(function () { if (!alive) showAll(); }, 1200);
})();

/* The feedback form.

   The form works without any of this: it is a plain POST to /api/feedback and
   the endpoint redirects a non-script submission to /thanks.html. What this
   adds is staying on the page, so a message that fails to send is still
   sitting in the textarea rather than lost to a navigation. */
(function () {
  var form = document.getElementById('feedback-form');
  if (!form) return;
  var status = document.getElementById('form-status');
  var button = form.querySelector('button[type="submit"]');
  if (!status || !button) return;

  var label = button.textContent;

  function say(text, ok) {
    status.textContent = text;
    status.className = 'form-status ' + (ok ? 'ok' : 'bad');
  }

  form.addEventListener('submit', function (e) {
    if (!form.checkValidity()) return;   /* let the browser do its own telling */
    e.preventDefault();

    var data = {};
    new FormData(form).forEach(function (v, k) { data[k] = v; });

    button.disabled = true;
    button.textContent = 'Sending…';
    say('', true);
    status.textContent = '';

    fetch(form.action, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
      body: JSON.stringify(data)
    })
      .then(function (r) {
        return r.json().catch(function () { return { ok: r.ok }; });
      })
      .then(function (out) {
        if (!out.ok) throw new Error(out.error || 'It did not send.');
        form.reset();
        say('Thank you. Your message arrived.', true);
      })
      .catch(function (err) {
        /* The text stays in the box on purpose: nobody should have to write
           a correction twice because the network blinked. */
        say(err.message || 'It did not send. Please try again.', false);
      })
      .then(function () {
        button.disabled = false;
        button.textContent = label;
      });
  });
})();

/* The theme button.

   Three states, not two. A two-way toggle reads as complete until you notice
   it has no way back to "follow my system", which is where most readers
   start and where some want to stay — so this cycles system → light → dark.

   The choice is written to localStorage and applied before first paint by the
   small script in each page's head; this only handles the pressing. Nothing
   is sent anywhere: the preference never leaves the browser. */
(function () {
  var btn = document.getElementById('theme-btn');
  if (!btn) return;
  var root = document.documentElement;
  var lbl = btn.querySelector('.lbl');
  var ORDER = ['system', 'light', 'dark'];

  function current() { return root.getAttribute('data-theme') || 'system'; }

  function paint() {
    var m = current();
    if (lbl) lbl.textContent = m.charAt(0).toUpperCase() + m.slice(1);
    btn.setAttribute('aria-label',
      'Theme: ' + m + '. Activate to switch to ' + ORDER[(ORDER.indexOf(m) + 1) % 3] + '.');
  }

  btn.addEventListener('click', function () {
    var next = ORDER[(ORDER.indexOf(current()) + 1) % 3];
    try {
      if (next === 'system') { root.removeAttribute('data-theme');
                               localStorage.removeItem('sinq-theme'); }
      else { root.setAttribute('data-theme', next);
             localStorage.setItem('sinq-theme', next); }
    } catch (e) {
      /* Private browsing can refuse storage. The theme still changes for this
         page; it simply will not be remembered. */
      if (next === 'system') root.removeAttribute('data-theme');
      else root.setAttribute('data-theme', next);
    }
    paint();
  });

  paint();
})();

/* The dial: the day as a circle.

   The gold arc is the part of today that has already gone and the lit node is
   the hour being kept now, both read from the visitor's own clock. No request
   is made for any of it — the page still contacts nobody, which is the whole
   claim the site makes about itself.

   The list of hours underneath carries the same seven hours and the same
   "now", so this is an illustration of something already said in text rather
   than the only place it is said. */
(function () {
  var box = document.getElementById('dial');
  if (!box) return;

  var HOURS = [
    { g: '፩', am: 'ጸሎተ ነግህ',  en: 'Morning',  h: 6 },
    { g: '፪', am: 'ሠለስት',     en: 'Terce',    h: 9 },
    { g: '፫', am: 'ቀትር',      en: 'Sext',     h: 12 },
    { g: '፬', am: 'ተሰዓት',     en: 'None',     h: 15 },
    { g: '፭', am: 'ሰርክ',      en: 'Vespers',  h: 18 },
    { g: '፮', am: 'ንዋም',      en: 'Compline', h: 21 },
    { g: '፯', am: 'መንፈቀ ሌሊት', en: 'Midnight', h: 0 }
  ];
  var NS = 'http://www.w3.org/2000/svg';
  var R = 112, C = 2 * Math.PI * R;

  function pad(n) { return (n < 10 ? '0' : '') + n; }

  /* መንፈቀ ሌሊት is written last and kept first: it is midnight, so it comes
     before ነግህ on the clock even though it closes the list. Taking the
     latest hour whose time has already passed gets this right at every hour
     of the day, including the small ones before dawn. */
  function indexAt(mins) {
    var idx = 6, best = -1;
    HOURS.forEach(function (x, i) {
      var m = x.h * 60;
      if (m <= mins && m >= best) { best = m; idx = i; }
    });
    return idx;
  }

  var now = new Date();
  var idx = indexAt(now.getHours() * 60 + now.getMinutes());

  var svg = document.createElementNS(NS, 'svg');
  svg.setAttribute('viewBox', '0 0 300 300');
  svg.setAttribute('role', 'img');
  svg.setAttribute('aria-label',
    'The seven canonical hours around a day. It is now ' + HOURS[idx].en + '.');

  function add(tag, attrs) {
    var el = document.createElementNS(NS, tag);
    for (var k in attrs) el.setAttribute(k, attrs[k]);
    svg.appendChild(el);
    return el;
  }

  for (var k = 0; k < 8; k++) {
    var a = (k / 8) * Math.PI * 2 - Math.PI / 2;
    add('line', { 'class': 'spoke',
      x1: 150 + Math.cos(a) * 100, y1: 150 + Math.sin(a) * 100,
      x2: 150 + Math.cos(a) * R,   y2: 150 + Math.sin(a) * R });
  }

  add('circle', { 'class': 'track', cx: 150, cy: 150, r: R });
  var arc = add('circle', { 'class': 'elapsed', cx: 150, cy: 150, r: R,
    transform: 'rotate(-90 150 150)',
    'stroke-dasharray': C, 'stroke-dashoffset': C });
  var hand = add('line', { 'class': 'hand', x1: 150, y1: 150, x2: 150, y2: 52 });

  var readout = document.createElement('div');
  readout.className = 'dial-readout';
  readout.innerHTML = '<div class="k"></div><div class="n ethi"></div>' +
                      '<div class="e"></div><div class="t"></div>';
  var ro = {
    k: readout.querySelector('.k'), n: readout.querySelector('.n'),
    e: readout.querySelector('.e'), t: readout.querySelector('.t')
  };

  function show(x, isNow) {
    ro.k.textContent = isNow ? 'Now' : 'Hour';
    ro.n.textContent = x.am;
    ro.e.textContent = x.en;
    ro.t.textContent = pad(x.h) + ':00';
  }

  HOURS.forEach(function (x, i) {
    var a = (x.h / 24) * Math.PI * 2 - Math.PI / 2;
    var cx = 150 + Math.cos(a) * R, cy = 150 + Math.sin(a) * R;
    var g = document.createElementNS(NS, 'g');
    g.setAttribute('class', 'hr' + (i === idx ? ' now' : ''));
    g.setAttribute('tabindex', '0');
    g.setAttribute('role', 'button');
    g.setAttribute('aria-label', x.am + ' — ' + x.en + ', ' + pad(x.h) + ':00');

    var c = document.createElementNS(NS, 'circle');
    c.setAttribute('cx', cx); c.setAttribute('cy', cy); c.setAttribute('r', 18);
    var t = document.createElementNS(NS, 'text');
    t.setAttribute('x', cx); t.setAttribute('y', cy);
    t.textContent = x.g;
    g.appendChild(c); g.appendChild(t);
    svg.appendChild(g);

    function peek() { show(x, i === idx); }
    function rest() { show(HOURS[idx], true); }
    g.addEventListener('mouseenter', peek);
    g.addEventListener('mouseleave', rest);
    g.addEventListener('focus', peek);
    g.addEventListener('blur', rest);
    g.addEventListener('click', peek);
  });

  box.appendChild(svg);
  box.appendChild(readout);
  box.hidden = false;
  show(HOURS[idx], true);

  var clockWrap = document.getElementById('dial-clock-wrap');
  var clock = document.getElementById('dial-clock');
  if (clockWrap) clockWrap.hidden = false;

  var calm = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  function tick() {
    var d = new Date();
    var f = (d.getHours() * 60 + d.getMinutes()) / 1440;
    if (clock) clock.textContent = pad(d.getHours()) + ':' + pad(d.getMinutes());
    arc.setAttribute('stroke-dashoffset', C * (1 - f));
    var a = f * Math.PI * 2 - Math.PI / 2;
    hand.setAttribute('x2', 150 + Math.cos(a) * 98);
    hand.setAttribute('y2', 150 + Math.sin(a) * 98);
  }

  /* The arc sweeps out from midnight on the first frame after paint. With
     reduced motion it is simply drawn where it belongs, no sweep. */
  if (calm) { arc.style.transition = 'none'; tick(); }
  else { requestAnimationFrame(function () { requestAnimationFrame(tick); }); }
  window.setInterval(tick, 30000);

  /* The same "now" in the list, for anyone who never looks at the circle. */
  var li = document.querySelectorAll('#hours-list li[data-hour]');
  Array.prototype.forEach.call(li, function (el) {
    if (parseInt(el.getAttribute('data-hour'), 10) === HOURS[idx].h) {
      el.classList.add('is-now');
      var at = el.querySelector('.at');
      if (at) at.textContent = 'now';
    }
  });
})();
