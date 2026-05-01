export const environment = {
  production: true,
  // Use relative path — Vercel rewrites /api/v1/* → Render backend (server-to-server).
  // This means the browser never makes a cross-origin request, so CORS is not triggered.
  // This is always more reliable than calling Render directly from the browser.
  apiUrl: '/api/v1'
};
