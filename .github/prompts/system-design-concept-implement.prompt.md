---
agent: agent
---
# aim
  - The aim of this project is to assist learning and understanding system design.
  - To acheive this there are lots of examples of implementations of system design constructs.

# the folder to implement is the name of the folder given as a parameter relative to the root of the repository
 - e.g. if the parameter is "ticketmaster/tech-locking-and-transactions" then implement for all the subprojects in that folder.

# implementation
 - These examples should be as simple as possible to present the ideas.
 - All examples should be in their own subproject. At this stage, these folders of these projects should be defined.
   If the subproject folder is missing or has the wrong name - ASK (provide any information you can to assist the user.)
 - Each subproject should contain a README.md file and a plan.md file. If these look to be incorrect, alert the user and explain what should be chnaged.
 - each project should be a Springboot 3.5.9 maven project using java 21 
 - be as spring idiomatic as possible
 - strongly prefer using Spring Annotations   
 - ensure that all logic has clear unit tests to prove that the logic is correct
 - ensure that there is a test, maybe an integration test, that proves that this subproject works and shows how to use this technology
 - ensure that there is a mermaid flow graph + sequence diagram in the README.md file and that the README.md file is a proper explanation for what was implemented
 - each implementation should be complete in itself, and not with any //TODO 
 - the README.md file of each subproject should contain instructions on how to run the tests
 - the top level README.md file in the folder should contain an overview of all the subprojects and a comparison table of the options considered with links to each subproject

# skills and instruction files
 - use relevenat skills and instruction files from the .github/prompts/skills and .github/prompts/instructions folders to assist you in implementing this prompt