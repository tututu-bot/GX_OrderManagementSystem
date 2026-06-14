const CACHE_NAME = 'gx-order-v4';
const urlsToCache = [
    '/',
    '/index.html',
    '/login.html',
    '/register.html',
    '/manifest.json'
];

self.addEventListener('install', event => {
    self.skipWaiting();
    event.waitUntil(
        caches.open(CACHE_NAME).then(cache => cache.addAll(urlsToCache))
    );
});

self.addEventListener('activate', event => {
    event.waitUntil(
        clients.claim().then(() => {
            return caches.keys().then(keys => {
                return Promise.all(
                    keys.filter(key => key !== CACHE_NAME).map(key => caches.delete(key))
                );
            });
        })
    );
});

self.addEventListener('fetch', event => {
    // HTML 文件走网络优先，确保总是拿到最新版本
    if (event.request.mode === 'navigate' || event.request.url.match(/\.html$/)) {
        event.respondWith(
            fetch(event.request).then(response => {
                return caches.open(CACHE_NAME).then(cache => {
                    cache.put(event.request, response.clone());
                    return response;
                });
            }).catch(() => {
                return caches.match(event.request);
            })
        );
    } else {
        event.respondWith(
            caches.match(event.request).then(response => response || fetch(event.request))
        );
    }
});
