# Omninet Chat API and WebSocket Guide (Text, Voice, Video)

This document describes all chat-related REST APIs and WebSocket endpoints, including request/response shapes and typical flows for text chat, read receipts/typing, and voice/video calls.

Last updated: 2025-09-29

---

## Authentication

- All REST endpoints require a valid JWT access token in the HTTP Authorization header: `Authorization: Bearer <token>`.
- WebSocket/STOMP must include the same header in the CONNECT frame as a native header: `Authorization: Bearer <token>`.
- WebSocket endpoint: `/ws` (SockJS enabled). Application destination prefix is `/app`. Broker prefixes: `/queue`, `/topic`.
- The server uses both direct destinations (e.g., `/queue/messages-<email>`) and user destinations (e.g., `/user/queue/call-status`). See "Subscriptions" below for what to subscribe to.

---

## Data Models (DTOs)

- SendMessageDTO
  - receiverEmail: string (email, required)
  - content: string (1..4000, required; HTML-escaped by backend)

- MessageView
  - id: string
  - senderEmail: string
  - receiverEmail: string
  - content: string
  - timestamp: ISO datetime
  - status: string (PENDING|DELIVERED|READ)

- TypingEvent
  - fromEmail: string (email; server sets this to current user)
  - toEmail: string (email, required)
  - typing: boolean

- MarkReadRequest
  - myEmail: string (email)
  - otherEmail: string (email)
  Note: For REST and WS, server takes the authenticated user as `myEmail` and uses the provided `otherEmail`.

- ContactItem
  - email: string
  - name: string
  - avatarUrl: string
  - lastMessagePreview: string | null
  - lastMessageTime: ISO datetime | null
  - unreadCount: number
  - online: boolean

- AddContactRequest
  - meEmail: string (email)
  - contactEmail: string (email)

- CallOfferDTO
  - receiverEmail: string (email, required)
  - callType: enum VOICE|VIDEO (required)
  - sdpOffer: string (required)
  - callId: string | null (server fills a backend UUID when initiating)

- CallResponseDTO
  - callId: string (required)
  - responseType: enum ACCEPT|REJECT|BUSY (required)
  - sdpAnswer: string | null (required when ACCEPT)
  - reason: string | null (used for REJECT/BUSY)

- CallStatusDTO
  - callId: string
  - callerEmail: string
  - calleeEmail: string
  - callType: VOICE|VIDEO
  - state: INITIATING|RINGING|CONNECTING|CONNECTED|ENDED|FAILED
  - startTime: ISO datetime
  - endTime: ISO datetime | null
  - errorMessage: string | null

- CallEndDTO
  - callId: string (required)
  - reason: enum USER_HANGUP|USER_ENDED|CALL_REJECTED|CONNECTION_LOST|ERROR|TIMEOUT | null

- IceCandidateDTO
  - callId: string (required)
  - candidate: string (required)
  - sdpMid: string (required)
  - sdpMLineIndex: number

---

## REST APIs

Base paths come from the following controllers: `MessageController`, `ContactController`, `CallController`.

### Messages

1) GET `/messages/history`
- Query params:
  - otherEmail: string (required)
  - page: int (default 0)
  - size: int (default 20, max 100)
- Response 200: HistoryPage
  - items: MessageView[]
  - page: number
  - size: number
  - hasMore: boolean

2) POST `/messages/mark-read`
- Body: MarkReadRequest
  - otherEmail is used; myEmail is ignored (derived from auth)
- Response: 200 empty

### Contacts

1) POST `/contacts/add`
- Body: AddContactRequest (meEmail, contactEmail)
- Response: 200 empty

2) GET `/contacts/list`
- Response 200: ContactItem[]

### Calls

Base path: `/api/chat/calls`

1) GET `/api/chat/calls/active`
- Response 200: Optional<string> (the active callId if any)

2) GET `/api/chat/calls/status/{callId}`
- Response 200: "Call is active" if the authenticated user is currently in this call
- Response 403: Not authorized to view this call

3) GET `/api/chat/calls/history?days=7`
- Query param: days (1..30, default 7)
- Response 200: CallStatusDTO[] (recent calls involving the user)

4) POST `/api/chat/calls/cleanup`
- Response 200: "Stale calls cleaned up successfully"
- Note: intended for admins; currently not gated in controller code

---

## WebSocket/STOMP Endpoints

- Connect to `/ws` with SockJS. Set native header `Authorization: Bearer <token>` in CONNECT.
- Application destination prefix: `/app` (clients SEND here).
- Broker prefixes: `/queue`, `/topic` (clients SUBSCRIBE here). Some messages also use user destinations under `/user`.

### Subscriptions to register (per authenticated user)

Subscribe to the following destinations on connect (replace `<me>` with your email):

- Text chat
  - `/queue/messages-<me>`: incoming MessageView
  - `/queue/read-<me>`: MarkReadRequest when the other user has read your messages
  - `/queue/typing-<me>`: TypingEvent when the other user is typing

- Calls (voice/video)
  - `/queue/call-offer-<me>`: CallOfferDTO for incoming call (includes backend `callId` and `sdpOffer`)
  - `/queue/call-response-<me>`: CallResponseDTO updates sent to the caller
  - `/queue/ice-candidate-<me>`: IceCandidateDTO from the other party
  - `/queue/call-status-<me>`: CallStatusDTO after connection confirmation
  - `/queue/call-end-<me>`: CallEndDTO when a call ends

- User destinations (server also emits to these for some events)
  - `/user/queue/call-status`: CallStatusDTO (e.g., after sending an offer)
  - `/user/queue/errors`: error messages for generic WS errors
  - `/user/queue/call-errors`: error messages related to calls

Note: The server uses both direct queues and user destinations; subscribing to all above ensures you receive every event.

### Client SEND destinations (under `/app`)

- `/app/chat.send`
  - Payload: SendMessageDTO
  - Effects:
    - If receiver is online: MessageView delivered to `/queue/messages-<receiver>` immediately
    - If offline: queued in RabbitMQ; delivered when they reconnect
    - Sender also receives the MessageView at `/queue/messages-<sender>`

- `/app/chat.read`
  - Payload: MarkReadRequest (server uses auth for `myEmail` and `otherEmail` from payload)
  - Effects: backend sets relevant messages to READ and notifies other party via `/queue/read-<other>`

- `/app/chat.typing`
  - Payload: TypingEvent with `toEmail` set; server fills `fromEmail` from auth
  - Effects: forwarded to `/queue/typing-<toEmail>`

- `/app/call.offer`
  - Payload: CallOfferDTO (`receiverEmail`, `callType`, `sdpOffer`)
  - Effects:
    - Backend creates a CallSession with backend UUID `callId`
    - Sends CallOfferDTO to callee at `/queue/call-offer-<callee>` (includes `callId`)
    - Sends CallStatusDTO to caller at `/user/queue/call-status` (state: RINGING)

- `/app/call.response`
  - Payload: CallResponseDTO (`callId`, `responseType`, and `sdpAnswer` when ACCEPT)
  - Effects:
    - For ACCEPT: forwarded to caller at `/queue/call-response-<caller>`; call moves to CONNECTING
    - For REJECT/BUSY: forwarded to caller, session ends and is cleaned up

- `/app/call.ice-candidate`
  - Payload: IceCandidateDTO (candidate info)
  - Effects: forwarded to the other party at `/queue/ice-candidate-<other>`

- `/app/call.connected`
  - Payload: string (the `callId`)
  - Effects: moves state to CONNECTED; notifies both parties via `/queue/call-status-<caller>` and `/queue/call-status-<callee>`

- `/app/call.end`
  - Payload: CallEndDTO (`callId`, optional `reason`)
  - Effects: ends the session, removes active tracking, and notifies both parties via `/queue/call-end-<caller>` and `/queue/call-end-<callee>`

---

## Text Chat Flow

1) Sender sends `/app/chat.send` with SendMessageDTO.
2) If receiver online (tracked by PresenceRegistry):
   - MessageView delivered immediately to `/queue/messages-<receiver>` and to `/queue/messages-<sender>`.
3) If receiver offline:
   - Message is marked PENDING and published to RabbitMQ (exchange `chat.direct`, routing key = receiver email, durable queue `chat.user.<sanitized-email>`).
   - On the receiver’s WebSocket subscribe/connect event, the server drains queued messages and updates their status to DELIVERED, pushing them to `/queue/messages-<receiver>`.

Read receipts
- The receiver sends `/app/chat.read` with `otherEmail` set to the sender email (or uses REST `/messages/mark-read`).
- Backend marks relevant messages READ and notifies the original sender at `/queue/read-<sender>`.

Typing indicator
- As user types, send `/app/chat.typing` with `toEmail` and `typing=true/false`.
- Backend forwards TypingEvent to `/queue/typing-<toEmail>`.

---

## Voice/Video Call Flow (WebRTC Signaling)

Preconditions: both parties are contacts; both must not be in active calls; callee must be online.

1) Offer
- Caller sends `/app/call.offer` with `receiverEmail`, `callType`, and `sdpOffer`.
- Server creates CallSession with backend `callId`, sets state RINGING.
- Server sends the CallOfferDTO to callee at `/queue/call-offer-<callee>` (includes `callId`).
- Server also sends CallStatusDTO to caller at `/user/queue/call-status` (state RINGING).

2) Response
- Callee sends `/app/call.response` with the `callId` and either ACCEPT (`sdpAnswer`) or REJECT/BUSY (`reason`).
- Server forwards CallResponseDTO to the caller at `/queue/call-response-<caller>`.
- On ACCEPT, state becomes CONNECTING.

3) ICE Candidates
- Either party sends `/app/call.ice-candidate` with IceCandidateDTO.
- Server forwards to the other participant at `/queue/ice-candidate-<other>`.

4) Connected Confirmation
- After peer connection is established, either party calls `/app/call.connected` with `callId`.
- Server sets state CONNECTED and notifies both parties via `/queue/call-status-<caller>` and `/queue/call-status-<callee>`.

5) End Call
- Either party sends `/app/call.end` with `callId` (and optional `reason`).
- Server sets state ENDED, cleans up active call tracking, and notifies both parties at `/queue/call-end-<caller>` and `/queue/call-end-<callee>`.

Timeouts and cleanup
- Stale RINGING calls older than 5 minutes are marked FAILED with reason TIMEOUT, both parties notified via `/queue/call-end-<email>`.

---

## Subtle Notes and Gotchas

- Destinations: The code uses both direct broker destinations (`/queue/...-<email>`) and user destinations (`/user/queue/...`). Subscribe to all relevant ones to be safe.
- Authorization: WebSocket authentication occurs during STOMP CONNECT via the `Authorization` header. If missing or invalid, the server logs a warning and may allow the session to proceed; your client should always send the header.
- Offline messages: Delivery depends on RabbitMQ being configured (`chat.rabbitmq.exchange`, `chat.rabbitmq.queue.prefix`). See `application.properties` for settings.
- Contacts: Some endpoints accept `meEmail` in the payload; however, the server typically derives the authenticated user from the session. Clients should not attempt to impersonate another user.
- Duplicates: ICE candidates and call responses are de-duplicated; repeated submissions may be ignored.

---

## Quick Client Sketch

- On connect, subscribe to:
  - `/queue/messages-<me>`, `/queue/read-<me>`, `/queue/typing-<me>`
  - `/queue/call-offer-<me>`, `/queue/call-response-<me>`, `/queue/ice-candidate-<me>`, `/queue/call-status-<me>`, `/queue/call-end-<me>`
  - `/user/queue/call-status`, `/user/queue/errors`, `/user/queue/call-errors`
- Send messages via `/app/chat.send`.
- For WebRTC, orchestrate offer/answer/ICE through `/app/call.*` as above.

---

## Related Classes

- Controllers: `MessageController`, `ContactController`, `ChatWsController`, `CallController`
- Services: `MessageService`, `ContactService`, `CallService`, `PresenceRegistry`
- Messaging: `RabbitConfig`, `RabbitQueueManager`, `MessageQueueService`
- WebSocket config/interceptor: `WebSocketConfig`, `AuthChannelInterceptor`


