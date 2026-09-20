# Payment-Processing-System

- Built a microservices-based payment platform using Spring Boot 3, Java 17, PostgreSQL, Redis, and Apache Kafka for processing deposits, withdrawals, transfers, and reversals
- Implemented JWT-based authentication with access/refresh token rotation, Redis-backed token blacklisting, and Spring Security
- Designed RESTful APIs for account management and transaction processing with bean validation and global exception handling
- Applied the Transactional Outbox Pattern to guarantee reliable event publishing to Kafka, ensuring data consistency between services without distributed transactions
- Built an event-driven Notification Service that consumes Kafka events and sends email alerts via SMTP
- Implemented idempotency handling using Redis caching to prevent duplicate transaction processing
- Used optimistic locking with retry logic (Spring Retry) to handle concurrent account balance updates safely
- Containerized infrastructure with Docker Compose (PostgreSQL, Redis, Kafka in KRaft mode) with health checks
