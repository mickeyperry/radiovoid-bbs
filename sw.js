// RadioVoid BBS service worker — fast loads + offline shell.
// Caches the app shell + static CDN libs. Never caches Supabase (API or audio).
const CACHE = 'rv-shell-v4';
const SHELL = ['./', './index.html', './icon.svg', './manifest.webmanifest'];
const CDN_HOSTS = ['fonts.googleapis.com', 'fonts.gstatic.com', 'cdn.jsdelivr.net'];

self.addEventListener('install', (e) => {
  e.waitUntil(
    caches.open(CACHE).then((c) => c.addAll(SHELL)).then(() => self.skipWaiting())
  );
});

self.addEventListener('activate', (e) => {
  e.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(keys.filter((k) => k !== CACHE).map((k) => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', (e) => {
  const req = e.request;
  if (req.method !== 'GET') return;
  const url = new URL(req.url);

  // Never touch Supabase — API responses and audio/mod files must always be live.
  if (url.hostname.endsWith('supabase.co')) return;

  // App shell (same origin): stale-while-revalidate — instant load, updates in background.
  if (url.origin === location.origin) {
    e.respondWith(staleWhileRevalidate(req));
    return;
  }

  // Known static CDNs (fonts, libs): cache-first.
  if (CDN_HOSTS.some((h) => url.hostname.endsWith(h))) {
    e.respondWith(cacheFirst(req));
  }
});

function cacheable(res) {
  // ok responses, or opaque cross-origin responses (status 0) from no-cors script/font loads.
  return res && (res.ok || res.status === 0 || res.type === 'opaque');
}

async function staleWhileRevalidate(req) {
  const cache = await caches.open(CACHE);
  const cached = await cache.match(req);
  const network = fetch(req)
    .then((res) => { if (cacheable(res)) cache.put(req, res.clone()); return res; })
    .catch(() => cached);
  return cached || network;
}

async function cacheFirst(req) {
  const cache = await caches.open(CACHE);
  const cached = await cache.match(req);
  if (cached) return cached;
  const res = await fetch(req);
  if (cacheable(res)) cache.put(req, res.clone());
  return res;
}
