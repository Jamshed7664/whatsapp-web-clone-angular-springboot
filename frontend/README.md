# 🎨 ChatWave Frontend — Angular 20

> Responsive WhatsApp Web–inspired messaging UI. See the [main README](../README.md) for full-stack setup and limitations.

## Requirements

| Tool | Version |
| --- | --- |
| Node.js | 20.19+ (or another Angular 20–supported version) |
| npm | 10+ |
| Angular CLI | `~20.3.0` (installed locally) |
| Angular | `~20.3.0` |
| TypeScript | `~5.9.0` |
| Zone.js | `~0.15.1` |
| RxJS | `~7.8.2` |
| STOMP.js | `^7.1.1` |
| lucide-angular | `^0.468.0` |

## Run

```bash
cd frontend
npm install
npm start
```

Open **http://localhost:4200**. Start Spring Boot on **http://localhost:8080** first.

## Commands

| Command | Action |
| --- | --- |
| `npm start` | Development server |
| `npm run typecheck` | TypeScript type checking |
| `npm run build` | Production build |

## Source files

- `src/app/app.component.ts/html/css` — UI, dialogs, chat state, theme, socket connection
- `src/app/api.service.ts` — REST requests to `http://localhost:8080/api`
- `src/app/auth.interceptor.ts` — JWT authorization header
- `src/app/models.ts` — Shared frontend types
- `src/main.ts` — Angular bootstrap
- `angular.json` — Includes `zone.js` in `polyfills`

## Backend communication

- REST API: `http://localhost:8080/api`
- WebSocket: `ws://localhost:8080/ws`
- STOMP subscription: `/user/queue/events`, with JWT on CONNECT

Change both the API base and the WebSocket URL when deploying; these are currently hard-coded local-development endpoints.

## Test chatting

Register two accounts in different browser sessions, click **New Chat**, find the other account, start a direct conversation, and send a message. There are no default demo credentials.

## Troubleshooting

- **NG0908**: Run `npm install` and ensure `"polyfills": ["zone.js"]` in `angular.json`.
- **CORS**: Configure Spring Security to allow `http://localhost:4200`, preflight `OPTIONS`, and `/api/auth/**`.
- **Cannot connect**: Start backend port 8080 first.
- **No live refresh**: Inspect browser Network → WebSocket and backend logs.

## Scope

Text messaging, group chat creation, authentication, themes, search, and unread counts are implemented. Media uploads, calls, statuses, real presence, and end-to-end encryption are not included. This is not an official WhatsApp client.
