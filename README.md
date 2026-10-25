# AJT Backend

Spring Boot service for the CodeCollab platform.

## Prerequisites

- Java 17+
- PostgreSQL database
- SMTP-capable email provider for verification emails
- Google OAuth credentials

## Local setup

1. Copy `.env.example` to `.env` and populate the values for your local environment.
2. Start the application with:
   `./mvnw spring-boot:run`
   If the Maven wrapper is unavailable, run:
   `mvn spring-boot:run`
3. The application listens on the port defined in `PORT` (default `8080`).

## Security notes

- Never commit `.env` files or other secrets.
- Rotate any credentials that were previously exposed in the repository history.
- Keep `.env.example` as a template only; do not store real secrets in it.
