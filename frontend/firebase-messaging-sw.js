// Service worker for Firebase Cloud Messaging compat
importScripts('https://www.gstatic.com/firebasejs/10.8.0/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/10.8.0/firebase-messaging-compat.js');

// Initialize Firebase App in service worker context
firebase.initializeApp({
  messagingSenderId: "195352003055",
  projectId: "safewatch-4ff20",
  apiKey: "mock-api-key",
  appId: "mock-app-id"
});

const messaging = firebase.messaging();

// Background message handler
messaging.onBackgroundMessage((payload) => {
  console.log('[firebase-messaging-sw.js] Background message payload:', payload);
  
  const notificationTitle = payload.notification?.title || 'SafeWatch Alert';
  const notificationOptions = {
    body: payload.notification?.body || 'New alert from SafeWatch.',
    icon: '/static/icon.png',
    badge: '/static/icon.png',
    data: payload.data
  };

  self.registration.showNotification(notificationTitle, notificationOptions);
});
