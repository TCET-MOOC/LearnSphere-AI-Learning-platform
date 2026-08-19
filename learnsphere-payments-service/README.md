# LearnSphere Payments Microservice

This microservice handles Razorpay payment integrations for one-time course purchases on the LearnSphere platform.
It acts as the single source of truth for payment status via Razorpay Webhooks.

## Architecture
- Exposes `POST /api/payments/orders` for the Angular frontend to initiate a payment.
- Exposes `POST /api/payments/webhook` for Razorpay to confirm payment status.
- Calls the main LearnSphere backend (`/internal/enroll`) via a secure internal REST call to finalize enrollment once a payment is confirmed.

## Environment Variables
The following environment variables should be set to run the service:

| Variable | Description | Default (Local) |
|----------|-------------|-----------------|
| `RAZORPAY_KEY_ID` | Public Key ID from Razorpay Dashboard | `rzp_test_yourkey` |
| `RAZORPAY_KEY_SECRET` | Secret Key from Razorpay Dashboard | `your_secret` |
| `RAZORPAY_WEBHOOK_SECRET` | Secret configured in the Razorpay Webhook settings | `your_webhook_secret` |
| `INTERNAL_API_KEY` | Shared secret key with the main backend | `my_secret_internal_key` |
| `MAIN_BACKEND_URL` | Base URL of the main LearnSphere backend | `http://localhost:8080` |

### Database Variables (Optional - PostgreSQL)
To use PostgreSQL instead of the default in-memory H2 database, set these variables and uncomment the properties in `application.properties`:
- `DB_URL`
- `DB_USER`
- `DB_PASSWORD`

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
To run the application locally:
```bash
mvn spring-boot:run
```
The application will start on port `8081`.
