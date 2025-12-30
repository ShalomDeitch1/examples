---
applyTo: '**'
---
All code should be Clean Code. 
Respect KISS, DRY, YAGNI and SOLID.
Ensure that all code has clear Unit Tests, but that there are tests that check the full flow.
Each project must have a README.md file with a clear explanation of the contents of the project.
The flow and sequence are described using clear "mermaid" in the README.md file.

Each new concept, presentation is a sub-project with a name that summarizes what is exampled.

Toolstack
 Java 21
 Springboot 3.5.6
 TestContainers
 localstack:latest
 maven

The general idea is to show technologies, so keep things clear and simple.

If something is not clear, ask

# code style
  - use standard spring idioms
  - use annotations where possible
  - use JPA where relevant
  - prefer text blocks over escaping in strings
  - ensure that the code is easy to read and understand
  - use imports not explicit class names, unless there is a conflict in the class names
  - keep classes small but do not make too many tiny classes. Keep single responsibility principle in mind but also KISS.
  - methods should be small and do one thing
  - use constructor injection

  # tools
    - when you need a terminal use wsl
    - when you need to delete a file use the wsl command rm
    - if there are many files to delete make a script delete-files.sh and run it in wsl. This file should delete itself as the last step.
    - use maven for build and dependency management
  - use localstack for AWS emulation
  - use Testcontainers of H2 for databases
