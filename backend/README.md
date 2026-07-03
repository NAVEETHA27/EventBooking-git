# Event Booking Backend

Spring Boot backend for the Event Booking System.

## Structure

- `config` - application, web, security, and seed configuration
- `controller` - REST API controllers
- `service` - application services
- `repository` - Spring Data repositories
- `entity` - JPA and MongoDB persistence entities
- `dto` - request and response DTOs
- `security` - JWT, principals, authentication filters, and token stores
- `exception` - API exception types and global exception handling
- `ai` - AI provider integrations
- `payment` - payment and refund workflow services
- `notification` - notification workflow services
- `scheduler` - reserved for dedicated scheduled jobs

## Validation

Compile with:

```bash
mvn -q -DskipTests compile
```
