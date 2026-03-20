FPP frontend — Vercel-ready API

This `frontend` directory now contains Vercel serverless API endpoints under `api/`:

- `GET /api/status` — returns plugin info. If the repo files are present in the deployment it will read them locally; otherwise it will proxy to a configured plugin API.
- `GET /api/check-update` — proxies to the plugin API's update endpoint.

Deployment notes

- Set the environment variable `PLUGIN_API_URL` in your Vercel project settings to point to your hosted plugin API (for example, `https://your-plugin-server.example.com`). When `PLUGIN_API_URL` is present the serverless functions will proxy to that API to fetch live data.

Run locally (PowerShell):

```powershell
cd "C:\Users\Name\IdeaProjects\fake player plugin\frontend"
npm install
# Run the local Express server (not required for Vercel deployment)
npm start
```

Vercel configuration

- `vercel.json` is included and routes all requests to `/api/status.js` by default so visiting the root shows the JSON.

Security note: The serverless functions may proxy to your plugin API — ensure you secure that API (auth or network restrictions) if it exposes sensitive data.
