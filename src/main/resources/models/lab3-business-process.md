# Lab 3 Business Process Model

## Async Video Processing

1. Author uploads metadata through `POST /api/videos`.
2. The HTTP transaction creates `videos` in the main PostgreSQL database and writes `VIDEO_PROCESSING_REQUESTED` to the outbox PostgreSQL database in one Atomikos/JTA 2PC transaction.
3. `KafkaOutboxPublisher` publishes the committed outbox event to `video.processing.requested`.
4. One of two application nodes in the shared Kafka consumer group validates the video and runs automatic copyright processing.
5. The worker updates the main database and writes `VIDEO_PROCESSING_COMPLETED` to outbox in one JTA transaction.

## Moderation Events

1. Moderator submits `POST /api/moderation/videos/{videoId}/decision`.
2. Video, moderation request, notification and audit rows are committed with the `MODERATION_DECISION` outbox event through JTA 2PC.
3. The publisher sends the committed event to Kafka topic `moderation.decision`.

## Scheduled Monthly Payouts

1. Both app nodes run `MonthlyPayoutScheduler`.
2. Only the node that acquires `scheduler_locks.monthly-payout-scheduler` queues `MONTHLY_PAYOUT_REQUESTED`.
3. A Kafka consumer calculates revenue and creates `PENDING` payouts.
4. Each payout creates `PAYOUT_REGISTRATION_REQUESTED`.

## Payment EIS Through JCA

1. `PayoutRegistrationConsumer` receives `payout.registration.requested`.
2. `PaymentRegistrationService` obtains a connection from the Jakarta Connector `PaymentEisManagedConnectionFactory`.
3. Payment EIS registration uses `payout-{payoutId}` as idempotency key.
4. Success stores `external_payment_id`, marks payout `PROCESSED`, and emits `PAYOUT_REGISTRATION_COMPLETED`.
5. Failure increments `attempts`, stores `last_error`, marks payout `FAILED`, and the retry scheduler requeues it.

## Consistency Rules

- PostgreSQL business data and outbox data are the XA resources in the strict distributed transaction.
- Kafka and Payment EIS are post-commit integrations with at-least-once delivery.
- Consumers are idempotent by business keys: `videoId`, payout period, `payoutId`, and EIS idempotency key.
