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

/* 3D Celestial Horizon Sun (Three.js WebGL).
   A celestial sun cresting over the planetary horizon in the background
   behind the canonical hours clock. 100% offline, zero network, zero tracking. */
function initHeroCelestialSun(calm) {
  var container = document.getElementById('hero-bg-canvas');
  var heroWrap = document.querySelector('.hero');
  var dialEl = document.getElementById('dial');
  if (!container || !heroWrap || typeof THREE === 'undefined') return null;

  var w = heroWrap.clientWidth || window.innerWidth;
  var h = heroWrap.clientHeight || 640;

  var renderer;
  try {
    renderer = new THREE.WebGLRenderer({ alpha: true, antialias: true, powerPreference: 'low-power' });
  } catch (err) {
    return null;
  }
  renderer.setSize(w, h);
  renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));
  container.appendChild(renderer.domElement);

  var scene = new THREE.Scene();
  var camera = new THREE.PerspectiveCamera(40, w / h, 0.1, 100);
  camera.position.set(0, 0, 8.5);

  var sunSystem = new THREE.Group();
  scene.add(sunSystem);

  function getDialWorldPos(targetZ) {
    if (!dialEl || dialEl.hidden) {
      return { x: (w >= 820 ? 2.2 : 0), y: -0.1 };
    }
    var rect = dialEl.getBoundingClientRect();
    var heroRect = heroWrap.getBoundingClientRect();
    var cx = rect.left + rect.width / 2 - heroRect.left;
    var cy = rect.top + rect.height / 2 - heroRect.top;
    var ndcX = (cx / heroRect.width) * 2 - 1;
    var ndcY = -(cy / heroRect.height) * 2 + 1;
    var vec = new THREE.Vector3(ndcX, ndcY, 0.5);
    vec.unproject(camera);
    var dir = vec.sub(camera.position).normalize();
    var dist = (targetZ - camera.position.z) / dir.z;
    return camera.position.clone().add(dir.multiplyScalar(dist));
  }

  var sunRadius = 2.85;
  var horizonRadius = 24.0;
  var targetCamX = 0, targetCamY = 0;
  var targetFlare = 0.0, curFlare = 0.0;

  // 1. Sun Sphere with dynamic convective plasma shader
  var sunVertex = [
    'varying vec3 vNormal;',
    'varying vec3 vPosition;',
    'varying vec2 vUv;',
    'void main() {',
    '  vUv = uv;',
    '  vNormal = normalize(normalMatrix * normal);',
    '  vPosition = (modelViewMatrix * vec4(position, 1.0)).xyz;',
    '  gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);',
    '}'
  ].join('\n');

  var sunFragment = [
    'uniform float u_time;',
    'uniform float u_flare;',
    'varying vec3 vNormal;',
    'varying vec3 vPosition;',
    'varying vec2 vUv;',
    '',
    'float hash(vec2 p) { return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453); }',
    'float noise(vec2 p) {',
    '  vec2 i = floor(p); vec2 f = fract(p);',
    '  f = f * f * (3.0 - 2.0 * f);',
    '  return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x), mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x), f.y);',
    '}',
    'float fbm(vec2 p) {',
    '  float v = 0.0; float a = 0.5;',
    '  for (int i = 0; i < 4; i++) { v += a * noise(p); p = p * 2.04 + vec2(31.4); a *= 0.5; }',
    '  return v;',
    '}',
    '',
    'void main() {',
    '  vec3 viewDir = normalize(-vPosition);',
    '  float fresnel = pow(1.0 - max(0.0, dot(vNormal, viewDir)), 2.6);',
    '',
    '  vec2 uv = vUv * 5.0;',
    '  float n1 = fbm(uv + vec2(u_time * 0.03, u_time * 0.015));',
    '  float n2 = fbm(uv * 1.6 - vec2(u_time * 0.025, -u_time * 0.035));',
    '  float plasma = n1 * 0.6 + n2 * 0.4;',
    '',
    '  vec3 moltenAmber = vec3(0.42, 0.18, 0.03);',
    '  vec3 liturgicalGold = vec3(0.93, 0.76, 0.36);',
    '  vec3 solarWhite = vec3(1.0, 0.98, 0.88);',
    '',
    '  vec3 color = mix(moltenAmber, liturgicalGold, plasma);',
    '  color = mix(color, solarWhite, fresnel * 0.88 + pow(plasma, 2.2) * 0.45 + u_flare * 0.25);',
    '',
    '  gl_FragColor = vec4(color, 0.98);',
    '}'
  ].join('\n');

  var sunMat = new THREE.ShaderMaterial({
    vertexShader: sunVertex,
    fragmentShader: sunFragment,
    uniforms: {
      u_time: { value: 0 },
      u_flare: { value: 0 },
    },
  });

  var sunMesh = new THREE.Mesh(new THREE.SphereGeometry(sunRadius, 64, 64), sunMat);
  sunSystem.add(sunMesh);

  // 2. Atmospheric Corona halo around the solar crest
  var coronaVertex = [
    'varying vec3 vNormal;',
    'varying vec3 vPosition;',
    'void main() {',
    '  vNormal = normalize(normalMatrix * normal);',
    '  vPosition = (modelViewMatrix * vec4(position, 1.0)).xyz;',
    '  gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);',
    '}'
  ].join('\n');

  var coronaFragment = [
    'uniform float u_flare;',
    'varying vec3 vNormal;',
    'varying vec3 vPosition;',
    'void main() {',
    '  vec3 viewDir = normalize(-vPosition);',
    '  float f = pow(1.0 - max(0.0, dot(vNormal, viewDir)), 2.8);',
    '  vec3 gold = vec3(0.93, 0.76, 0.36);',
    '  vec3 white = vec3(1.0, 0.98, 0.88);',
    '  gl_FragColor = vec4(mix(gold, white, f * 0.5), f * (0.85 + u_flare * 0.2));',
    '}'
  ].join('\n');

  var coronaMat = new THREE.ShaderMaterial({
    vertexShader: coronaVertex,
    fragmentShader: coronaFragment,
    uniforms: { u_flare: { value: 0 } },
    transparent: true,
    blending: THREE.AdditiveBlending,
    side: THREE.BackSide,
    depthWrite: false,
  });
  var coronaMesh = new THREE.Mesh(
    new THREE.SphereGeometry(sunRadius * 1.25, 48, 48),
    coronaMat
  );
  sunSystem.add(coronaMesh);

  // 3. Planetary Horizon Sphere
  var horizonVertex = [
    'varying vec3 vNormal;',
    'varying vec3 vPosition;',
    'void main() {',
    '  vNormal = normalize(normalMatrix * normal);',
    '  vPosition = (modelViewMatrix * vec4(position, 1.0)).xyz;',
    '  gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);',
    '}'
  ].join('\n');

  var horizonFragment = [
    'varying vec3 vNormal;',
    'varying vec3 vPosition;',
    'void main() {',
    '  vec3 viewDir = normalize(-vPosition);',
    '  float rim = pow(1.0 - max(0.0, dot(vNormal, viewDir)), 5.0);',
    '  vec3 ground = vec3(0.025, 0.11, 0.09);',
    '  vec3 atmosGold = vec3(0.85, 0.68, 0.32);',
    '  vec3 color = mix(ground, atmosGold, rim * 0.65);',
    '  gl_FragColor = vec4(color, 1.0);',
    '}'
  ].join('\n');

  var horizonMesh = new THREE.Mesh(
    new THREE.SphereGeometry(horizonRadius, 80, 80),
    new THREE.ShaderMaterial({
      vertexShader: horizonVertex,
      fragmentShader: horizonFragment,
    })
  );
  scene.add(horizonMesh);

  // 4. Volumetric Sky Light Rays
  var rayVertex = [
    'varying vec2 vUv;',
    'void main() {',
    '  vUv = uv;',
    '  gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);',
    '}'
  ].join('\n');

  var rayFragment = [
    'uniform float u_time;',
    'uniform vec2 u_sun_uv;',
    'uniform float u_flare;',
    'varying vec2 vUv;',
    'void main() {',
    '  vec2 p = vUv - u_sun_uv;',
    '  p.x *= (22.0 / 14.0);',
    '  float angle = atan(p.y, p.x);',
    '  float dist = length(p);',
    '  float r1 = pow(max(0.0, cos(7.0 * angle + u_time * 0.03)), 4.0);',
    '  float r2 = pow(max(0.0, cos(14.0 * angle - u_time * 0.02)), 6.0) * 0.5;',
    '  float rays = (r1 + r2) * smoothstep(1.8, 0.1, dist) * smoothstep(0.0, 0.2, p.y);',
    '  vec3 gold = vec3(0.93, 0.76, 0.36);',
    '  gl_FragColor = vec4(gold, rays * (0.35 + u_flare * 0.15));',
    '}'
  ].join('\n');

  var raySunUv = new THREE.Vector2(0.65, 0.45);
  var rayMat = new THREE.ShaderMaterial({
    vertexShader: rayVertex,
    fragmentShader: rayFragment,
    uniforms: {
      u_time: { value: 0 },
      u_sun_uv: { value: raySunUv },
      u_flare: { value: 0 },
    },
    transparent: true,
    blending: THREE.AdditiveBlending,
    depthWrite: false,
  });
  var rayPlane = new THREE.Mesh(new THREE.PlaneGeometry(22, 14), rayMat);
  rayPlane.position.set(0, 0, -1.8);
  scene.add(rayPlane);

  // 5. Celestial Embers
  var emberCount = 85;
  var emberGeo = new THREE.BufferGeometry();
  var emberPositions = new Float32Array(emberCount * 3);
  for (var i = 0; i < emberCount; i++) {
    emberPositions[i * 3 + 0] = (Math.random() - 0.5) * 14;
    emberPositions[i * 3 + 1] = -1.5 + Math.random() * 5.0;
    emberPositions[i * 3 + 2] = (Math.random() - 0.5) * 4;
  }
  emberGeo.setAttribute('position', new THREE.BufferAttribute(emberPositions, 3));
  var embers = new THREE.Points(
    emberGeo,
    new THREE.PointsMaterial({
      color: 0xE8C46B,
      size: 0.04,
      transparent: true,
      opacity: 0.65,
      blending: THREE.AdditiveBlending,
    })
  );
  scene.add(embers);

  var horizonY = -0.3;

  function updateLayout() {
    var heroW = heroWrap.clientWidth || window.innerWidth;
    var heroH = heroWrap.clientHeight || 640;
    camera.aspect = heroW / heroH;
    camera.updateProjectionMatrix();
    renderer.setSize(heroW, heroH);

    var isMobile = heroW < 820;
    var currentSunRadius = isMobile ? Math.min(2.0, (heroW / 400) * 1.8) : 2.85;
    sunMesh.scale.setScalar(currentSunRadius / 2.85);
    coronaMesh.scale.setScalar(currentSunRadius / 2.85);

    var p = getDialWorldPos(-1.2);
    var sunX = p.x;
    horizonY = p.y - (isMobile ? 0.35 : 0.2);
    var sunY = horizonY - (isMobile ? 0.45 : 0.7);

    sunMesh.position.set(sunX, sunY, -1.2);
    coronaMesh.position.set(sunX, sunY, -1.2);
    horizonMesh.position.set(isMobile ? sunX : 0.5, horizonY - horizonRadius, 0.2);
    rayMat.uniforms.u_sun_uv.value.set((sunX + 11.0) / 22.0, (horizonY + 7.0) / 14.0);
  }

  updateLayout();
  window.setTimeout(updateLayout, 80);
  window.setTimeout(updateLayout, 300);
  window.addEventListener('resize', updateLayout);

  var animId = null;
  var isVisible = true;
  var start = performance.now();

  function renderFrame(now) {
    var t = (now - start) * 0.001;
    sunMat.uniforms.u_time.value = t;
    rayMat.uniforms.u_time.value = t;

    curFlare += (targetFlare - curFlare) * 0.08;
    sunMat.uniforms.u_flare.value = curFlare;
    coronaMat.uniforms.u_flare.value = curFlare;
    rayMat.uniforms.u_flare.value = curFlare;

    sunMesh.rotation.y = t * 0.06;
    sunMesh.rotation.x = Math.sin(t * 0.04) * 0.04;

    camera.position.x += (targetCamX - camera.position.x) * 0.05;
    camera.position.y += (targetCamY - camera.position.y) * 0.05;

    var pos = emberGeo.attributes.position.array;
    for (var i = 0; i < emberCount; i++) {
      pos[i * 3 + 1] += 0.004;
      if (pos[i * 3 + 1] > horizonY + 5.0) {
        pos[i * 3 + 1] = horizonY + 0.1;
      }
    }
    emberGeo.attributes.position.needsUpdate = true;

    renderer.render(scene, camera);
  }

  function loop(now) {
    if (!isVisible) { animId = null; return; }
    renderFrame(now);
    animId = requestAnimationFrame(loop);
  }

  renderFrame(performance.now());

  if (!calm) {
    animId = requestAnimationFrame(loop);

    if ('IntersectionObserver' in window) {
      var io = new IntersectionObserver(function (entries) {
        entries.forEach(function (e) {
          isVisible = e.isIntersecting && !document.hidden;
          if (isVisible && !animId) animId = requestAnimationFrame(loop);
        });
      }, { threshold: 0.05 });
      io.observe(heroWrap);
    }

    document.addEventListener('visibilitychange', function () {
      if (document.hidden) {
        isVisible = false;
      } else {
        isVisible = true;
        if (!animId) animId = requestAnimationFrame(loop);
      }
    });
  }

  return {
    focusAngle: function (angle, weight) {
      targetFlare = weight || 0;
    },
    setParallax: function (x, y) {
      targetCamX = x * 0.5;
      targetCamY = y * 0.35;
    }
  };
}

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
  var activeAngle = (HOURS[idx].h / 24) * Math.PI * 2 - Math.PI / 2;

  var calm = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  var sun = initHeroCelestialSun(calm);

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

    function peek() {
      show(x, i === idx);
      if (sun) sun.focusAngle(a, 0.95);
    }
    function rest() {
      show(HOURS[idx], true);
      if (sun) sun.focusAngle(activeAngle, 0.4);
    }
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

  var heroWrapEl = document.querySelector('.hero');
  if (heroWrapEl && sun && !calm) {
    heroWrapEl.addEventListener('mousemove', function (e) {
      var r = heroWrapEl.getBoundingClientRect();
      var nx = (e.clientX - (r.left + r.width / 2)) / (r.width / 2);
      var ny = (e.clientY - (r.top + r.height / 2)) / (r.height / 2);
      sun.setParallax(Math.max(-1, Math.min(1, nx)), Math.max(-1, Math.min(1, -ny)));
    });
    heroWrapEl.addEventListener('mouseleave', function () {
      sun.setParallax(0, 0);
    });
  }

  var clockWrap = document.getElementById('dial-clock-wrap');
  var clock = document.getElementById('dial-clock');
  if (clockWrap) clockWrap.hidden = false;

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
