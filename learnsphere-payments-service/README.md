# LearnSphere Payments Microservice

This microservice handles Razorpay payment integrations for one-time course purchases on the LearnSphere platform.
It acts as the single source of truth for payment status via Razorpay Webhooks.

## Architecture
- Exposes `POST /api/payments/orders` for the Angular frontend to initiate a payment.
- Exposes `POST /api/payments/webhook` for Razorpay to confirm payment status.
- Calls the main LearnSphere backend (`/internal/enroll`) via a secure internal REST call to finalize enrollment once a payment is confirmed.

## Environment Variables
Every value below is required. There are no secret fallbacks in the source or in
`docker-compose.yml`; the service fails fast when one is missing. Use the repository-level
`.env.example` for Docker Compose, or export the variables for a bare-metal run.

| Variable | Description |
|----------|-------------|
| `RAZORPAY_KEY_ID` | Public test Key ID from the Razorpay Dashboard |
| `RAZORPAY_KEY_SECRET` | Test Secret Key from the Razorpay Dashboard |
| `RAZORPAY_WEBHOOK_SECRET` | Secret configured in the Razorpay Webhook settings |
| `INTERNAL_API_KEY` | Shared secret key with the main backend |
| `MAIN_BACKEND_URL` | Base URL of the main LearnSphere backend (`http://backend:8080` in Compose) |

### Database Variables
The service uses PostgreSQL in every profile. The `local` profile reads `DB_URL`,
`DB_USER` and `DB_PASSWORD`; the `prod` profile requires all three with no defaults.
The `test` profile gets its disposable PostgreSQL from Testcontainers.

## Local Testing with Ngrok
To test webhooks locally, Razorpay needs to be able to reach your locally running instance.

1. Install ngrok.
2. Run ngrok to tunnel port 8081:
   ```bash
   ngrok http 8081
   ```
3. Ngrok will give you a public URL (e.g., `https://1234abcd.ngrok-free.app`).

## Registering Webhook in Razorpay Dashboard
1. Go to the Razorpay Dashboard -> Settings -> Webhooks.
2. Click "Add New Webhook".
3. Set the Webhook URL to your ngrok URL + the endpoint path.
   Example: `https://1234abcd.ngrok-free.app/api/payments/webhook`
4. Set the Secret. This must match the `RAZORPAY_WEBHOOK_SECRET` environment variable.
5. Under "Active Events", select:
   - `payment.captured`
   - `payment.failed`
6. Save the webhook.

## Running the Application
From this directory, after exporting the variables above:

```bash
./mvnw spring-boot:run
```

On Windows use `.\mvnw.cmd spring-boot:run`. The service starts on port `8081`.
