# MediSmart
Full-stack telehealth app: Spring Boot API + Next.js frontend with JWT auth, appointments, doctor recommendations, medical records, password reset/OTP email, and live consultations (Agora video + STOMP chat).

## Live links
 https://medi-smart-eta.vercel.app

## Repository layout
- MediSmart-Backend/   - Spring Boot 3.5 service (MongoDB, Security/JWT, WebSocket/STOMP, Agora token generation).
- MediSmart-frontend/  - Next.js 14 client (React 18, Tailwind, Radix UI) consuming the backend APIs.

## Tech stack
- Backend: Spring Boot, Spring Security + JWT, Spring Data MongoDB, Spring Mail, WebSocket/STOMP, Agora Authentication SDK, Lombok.
- Frontend: Next.js 14 (App Router), React 18, Tailwind CSS, Radix UI, zod + react-hook-form, Axios, Agora Web SDK.

## Prerequisites
- Java 21+ and Maven (wrappers `mvnw` / `mvnw.cmd` included)
- Node.js 18+ and npm
- MongoDB connection string
- SMTP credentials (use an app password for Gmail)
- Agora project App ID and App Certificate

## Environment configuration
Backend config file: `MediSmart-Backend/src/main/resources/application.properties`  
Use environment variables (recommended) instead of hard-coded secrets:
```
spring.application.name=MediSmart
spring.data.mongodb.uri=${MONGODB_URI}
spring.data.mongodb.auto-index-creation=true

jwt.secret-key=${JWT_SECRET_KEY}

spring.mail.port=${SPRING_MAIL_PORT}
spring.mail.host=${SPRING_MAIL_HOST}
spring.mail.username=${SPRING_MAIL_USERNAME}
spring.mail.password=${SPRING_MAIL_PASSWORD}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

agora.app-id=${AGORA_APP_ID}
agora.app-certificate=${AGORA_APP_CERTIFICATE}
```
Frontend API base URL: set `NEXT_PUBLIC_API_BASE_URL` in `MediSmart-frontend/.env.local` (defaults to `https://medismart.onrender.com`). Example for local dev: `http://localhost:8080`.

## Running locally
Backend:
```
cd MediSmart-Backend
./mvnw spring-boot:run      # or mvnw.cmd spring-boot:run on Windows
```
Runs on http://localhost:8080.

Frontend:
```
cd MediSmart-frontend
npm install
npm run dev                 # serves on http://localhost:3000
```
If the backend host or port differs, update `config/api.ts` or set `NEXT_PUBLIC_API_BASE_URL`.

## Feature map (where to look)
- Auth & JWT: `AuthController`, `JwtAuthenticationFilter`, `JwtUtil`
- Appointments: `AppointmentController`, `AppointmentService`
- Doctor recommendations: `DoctorRecommendationController`
- Medical records: `MedicalRecordController`
- Consultations: `ConsultationController` (token issuance), `ChatController` (chat), `components/consultation/consultation-room.tsx` (Agora Web + STOMP)
- Password reset / OTP: `PasswordResetController`, mail settings above

## Scripts and builds
- Backend tests: `./mvnw test`
- Backend package: `./mvnw package` (artifact in `MediSmart-Backend/target/`)
- Frontend lint: `npm run lint`
- Frontend production build: `npm run build` (output in `MediSmart-frontend/.next/`)

## Deployment notes
- Keep secrets in environment variables or a secrets manager; do not commit real values.
- Use separate MongoDB databases per environment.
- Serve over HTTPS; ensure CORS and WebSocket origins include the frontend host.
- For static hosting of the frontend, run `npm run build` then `next start`, or deploy via Vercel and set `API_BASE_URL`.

## Troubleshooting
- MongoDB connection failures: verify `MONGODB_URI` and IP allowlist.
- Email not sending: double-check SMTP host/port (587) and app password.
- Agora join errors: confirm `AGORA_APP_ID` / `AGORA_APP_CERTIFICATE`; ensure the backend issues a token for the channel the frontend joins.
- WebSocket chat drops: make sure `/ws` endpoint is reachable and CORS/WS origins match the frontend.

## Security checklist
- Rotate any credentials already committed and replace with env vars.
- Prefer unique JWT secrets per environment; keep token TTLs reasonable.
- Use HTTPS in production; enable HTTP-only/Secure flags if you add cookies later.
