## Plan: Implement Ticketmaster Notification Examples

I will implement three simple Spring Boot examples demonstrating server-to-client notifications: Polling, Server-Sent Events (SSE), and Webhooks.

### Steps
1. **Update Main Docs**: Refine `ticketmaster/tech-notifications/plan.md` and `README.md` to align with the specific implementation details.
2. **Implement Polling (`app-notification`)**:
    - Create `README.md` with a sequence diagram for client polling.
    - Scaffold Spring Boot app (Java 21, Spring Boot 3.5.9).
    - Implement a simple REST endpoint for the client to poll for status.
3. **Implement SSE (`sse`)**:
    - Create `README.md` with a sequence diagram for SSE subscription and events.
    - Scaffold Spring Boot app.
    - Implement an endpoint returning `SseEmitter` and a mechanism to push events.
4. **Implement Webhook (`webhook`)**:
    - Create `README.md` with a sequence diagram for the callback flow.
    - Scaffold Spring Boot app.
    - Implement a logic to "send" a POST request to a registered URL (simulating the callback).

### Further Considerations
1. **NO Common Event Model**
2. **Testing**
  -  uses curl in the README.md file. If parameters are required should give a set of commands that can be just copied
  -  unit tests for the core logic
3. ** implementation details**
  - since all the sub-projects are independent, they can all be run in parallel using agents

refer to skills, copilot-instructions, ticketmaster-tech-notifications.prompt.md and all the README.md files in the tech-notifications sub-projects for more details. Remember first step can update those README.md files as needed.  
