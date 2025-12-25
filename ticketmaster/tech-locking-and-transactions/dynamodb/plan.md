# DynamoDB locking — plan (minimal)

Goal: runnable, testable examples of **conditional writes** and **transactions**.

## TODO

- [ ] Create Maven module (Java 21 + Spring Boot 3.5.9)
- [ ] Implement conditional seat reserve (`UpdateItem` + `ConditionExpression`)
- [ ] Implement transact reserve + order create (`TransactWriteItems`)
- [ ] Integration tests with LocalStack Testcontainers

## Acceptance criteria

- `mvn test` passes (with Docker running)
- Conditional write: 2 racers → exactly 1 wins
- Transaction write: seat reserved and order created atomically
