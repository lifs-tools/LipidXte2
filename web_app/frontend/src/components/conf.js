// In development the Vite dev server runs on :9080 and the Node server on :8090 (it allows CORS);
// in the production bundle both are served from the same origin.
export const serverUrl = import.meta.env.DEV ? 'http://localhost:8090' : ''
