# Short URL Service - Simplest Version

This project demonstrates the simplest possible implementation of a URL Shortener service using a **Monolithic Architecture** with **In-Memory Storage**.

## Architecture

- **Pattern**: Monolith (Single Module)
- **Data Storage**: In-Memory (`ConcurrentHashMap`)
- **Framework**: Spring Boot

## Features

- **Shorten URL**: Accepts a long URL and returns a short ID.
- **Resolve URL**: Redirects a short ID to the original long URL.
- **Speed**: Operations are O(1) due to hash map usage.

## Limitations (By Design)

1.  **Data Loss**: All data is lost when the application restarts. This is verified by the `verifyDataLossAfterRestart` test.
2.  **Scalability**: Cannot scale horizontally without a shared database (requests would go to different instances with different maps).
3.  **Memory**: Limited by the JVM heap size.

## specific Pattern Implementation

This example highlights the **"Big Ball of Mud"** or **Simple Monolith** starting point:
- No database dependencies.
- Service and persistence logic mixed (UrlService holds the map).
- Extremely fast for prototyping but not production-ready.

## How to Run

```bash
mvn spring-boot:run
```

## Endpoints

### Shorten URL
`POST /api/shorten`
```json
{
  "url": "https://example.com"
}
```

### Resolve URL
`GET /{shortId}`
Redirects to `https://example.com`
