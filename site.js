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
