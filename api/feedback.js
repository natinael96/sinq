// The site's only endpoint.
//
// The reader's browser posts here and nowhere else. The destination, a
// Telegram chat, is contacted server to server, so the page keeps its promise:
// no third party ever sees who read it, and Telegram learns nothing about the
// person who wrote in beyond what they typed.
//
// What is sent on is what was typed. Not the IP, not the user agent, not a
// cookie, not a record that the page was opened. The site tells the reader
// exactly this on /feedback.html, so it has to stay true here.
//
// Destination, in order of preference:
//   TELEGRAM_BOT_TOKEN + TELEGRAM_CHAT_ID   a message from a bot
//   FEEDBACK_WEBHOOK (+ FEEDBACK_SECRET)    any URL taking a JSON POST
// With neither set this refuses loudly rather than accepting messages and
// dropping them, because feedback silently swallowed is worse than a form that
// admits it is not connected.

const MAX = { message: 4000, where: 200, contact: 200, kind: 60, version: 20 };

// Telegram caps a message at 4096 characters. Reserve room for the fields
// under the message so that a very long report never costs the reply address.
const TG_LIMIT = 4096;
const TG_RESERVE = 320;

// Strip C0 controls and DEL, keeping newlines and tabs, then cap the length.
const clean = (v, n) =>
  typeof v === 'string'
    ? v.replace(/[\u0000-\u0008\u000B\u000C\u000E-\u001F\u007F]/g, '').trim().slice(0, n)
    : '';

function telegramText(e) {
  const body = e.message.length > MAX.message - TG_RESERVE
    ? e.message.slice(0, MAX.message - TG_RESERVE - 3) + '...'
    : e.message;
  const lines = ['Sinq feedback · ' + e.kind, '', body, ''];
  if (e.where) lines.push('Where: ' + e.where);
  lines.push('Reply to: ' + (e.contact || 'no address given'));
  if (e.version) lines.push('App version: ' + e.version);
  return lines.join('\n').slice(0, TG_LIMIT);
}

async function sendTelegram(entry, token, chat) {
  const r = await fetch('https://api.telegram.org/bot' + token + '/sendMessage', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      chat_id: chat,
      text: telegramText(entry),
      disable_web_page_preview: true,
    }),
    signal: AbortSignal.timeout(10000),
  });
  // Telegram answers 200 with {"ok":false} for several real failures, so the
  // status code alone is not enough to call this sent.
  const out = await r.json().catch(() => ({}));
  if (!r.ok || out.ok === false) {
    throw new Error('telegram: ' + (out.description || 'HTTP ' + r.status));
  }
}

async function sendWebhook(entry, url, secret) {
  const r = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(secret ? Object.assign({}, entry, { secret: secret }) : entry),
    signal: AbortSignal.timeout(10000),
  });
  if (!r.ok) throw new Error('webhook: HTTP ' + r.status);
}

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

  const token = process.env.TELEGRAM_BOT_TOKEN;
  const chat = process.env.TELEGRAM_CHAT_ID;
  const hook = process.env.FEEDBACK_WEBHOOK;

  if (!(token && chat) && !hook) {
    console.error('feedback received but no destination is configured:', entry.kind);
    return done(503, {
      ok: false,
      error: 'The form is not connected yet. Please open an issue on GitHub instead.',
    });
  }

  try {
    if (token && chat) await sendTelegram(entry, token, chat);
    else await sendWebhook(entry, hook, process.env.FEEDBACK_SECRET);
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
