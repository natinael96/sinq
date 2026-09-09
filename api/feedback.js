// The site's only endpoint.
//
// The reader's browser posts here and nowhere else. Whatever this forwards the
// message to (a Google Sheet, an inbox, a chat room) is contacted server to
// server, so the page keeps its promise: no third party ever sees who read it.
//
// What is stored is what was typed. Not the IP, not the user agent, not a
// cookie, not a record that the page was opened. The site tells the reader
// exactly this on /feedback.html, so it has to stay true here.
//
// Destination is FEEDBACK_WEBHOOK (a URL that accepts a JSON POST) and, if the
// receiver checks one, FEEDBACK_SECRET. With neither set this refuses loudly
// rather than accepting messages and dropping them, because feedback silently
// swallowed is worse than a form that admits it is not connected.

const MAX = { message: 4000, where: 200, contact: 200, kind: 60, version: 20 };

// Strip C0 controls and DEL, keeping newlines and tabs, then cap the length.
const clean = (v, n) =>
  typeof v === 'string'
    ? v.replace(/[\u0000-\u0008\u000B\u000C\u000E-\u001F\u007F]/g, '').trim().slice(0, n)
    : '';

module.exports = async (req, res) => {
  if (req.method !== 'POST') {
    res.setHeader('Allow', 'POST');
    return res.status(405).json({ ok: false, error: 'Use POST.' });
  }

  const body = req.body && typeof req.body === 'object' ? req.body : {};
  // A fetch() sends JSON and wants JSON back. A plain form post, from a reader
  // with no JavaScript, wants to land on a page it can read.
  const wantsJson =
    (req.headers['content-type'] || '').includes('application/json') ||
    (req.headers.accept || '').includes('application/json');

  const done = (status, payload, redirect) => {
    if (wantsJson) return res.status(status).json(payload);
    if (redirect) {
      res.setHeader('Location', redirect);
      return res.status(303).end();
    }
    return res.status(status).send(payload.error || 'Error');
  };

  // The honeypot. A bot fills every field it can parse; a person never sees
  // this one. Answer as though it worked so the bot has nothing to learn.
  if (clean(body.website, 200)) return done(200, { ok: true }, '/thanks.html');

  const message = clean(body.message, MAX.message);
  if (!message) return done(400, { ok: false, error: 'A message is required.' });

  const entry = {
    kind: clean(body.kind, MAX.kind) || 'Something else',
    message,
    where: clean(body.where, MAX.where),
    contact: clean(body.contact, MAX.contact),
    version: clean(body.version, MAX.version),
    at: new Date().toISOString(),
  };

  const url = process.env.FEEDBACK_WEBHOOK;
  if (!url) {
    console.error('feedback received but FEEDBACK_WEBHOOK is not set:', entry.kind);
    return done(503, {
      ok: false,
      error: 'The form is not connected yet. Please open an issue on GitHub instead.',
    });
  }

  try {
    const upstream = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(
        process.env.FEEDBACK_SECRET
          ? Object.assign({}, entry, { secret: process.env.FEEDBACK_SECRET })
          : entry
      ),
      signal: AbortSignal.timeout(10000),
    });
    if (!upstream.ok) throw new Error('upstream ' + upstream.status);
  } catch (e) {
    // Never answer "sent" when it was not. Someone who took the time to write
    // a correction should be told to try elsewhere, not thanked for nothing.
    console.error('feedback forward failed:', e.message);
    return done(502, {
      ok: false,
      error: 'Could not send it just now. Please try again, or open an issue on GitHub.',
    });
  }

  return done(200, { ok: true }, '/thanks.html');
};
