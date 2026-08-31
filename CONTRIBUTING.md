# Contributing to Bird Fight 3

Thank you for your interest in contributing to Bird Fight 3.  
These guidelines explain how to propose changes, report issues, and participate in maintenance.

Bird Fight 3 is feature complete. Focus contributions on reproducible bugs,
compatibility, accessibility, documentation, and deliberately scoped improvements.

---

## How to Contribute

### 1. Fork the Repository
Create a personal fork of the project on GitHub.

### 2. Create a Feature Branch
Use clear, descriptive branch names such as:

- feature/new-bird-raven
- fix/audio-latency
- refactor/fight-setup-state

### 3. Make Your Changes
Keep commits focused and logically grouped.  
If your change affects gameplay, UI, or balance, include a brief explanation in the commit message.

### 4. Test Your Changes
Before opening a pull request:

- Run the game locally  
- Test menus, controller input, and fight setup  
- Ensure no new warnings or errors appear  
- Verify that existing characters still behave correctly  

### 5. Submit a Pull Request
Open a pull request to the `main` branch with:

- A clear title  
- A concise description of the changes  
- Screenshots or video if UI or gameplay changed  
- Any known limitations or follow-up tasks  

A maintainer will review your submission.

---

## Code Style and Standards

To maintain a consistent codebase:

- Follow existing formatting and structure  
- Use descriptive names for classes, methods, and variables  
- Avoid large, unrelated changes in a single PR  
- Keep logic modular; prefer helper classes over large monolithic methods  
- Document complex systems when appropriate (for example: fight setup state, CPU UI installers)

---

## Reporting Bugs

If you encounter a bug, open an Issue and include:

- Steps to reproduce  
- Expected behavior  
- Actual behavior  
- Screenshots or logs if applicable  
- Your operating system and Java version  

Clear reports help maintainers resolve issues faster.

---

## Suggesting Features

Feature requests should be submitted through GitHub Issues. The project does not
maintain an open-ended feature roadmap, so explain why the change is worth
reopening a completed area of the game.
Include:

- A clear description of the feature  
- Why it would benefit the project  
- Any relevant examples or references  

---

## Pull Request Expectations

Pull requests should:

- Be focused on a single change or topic  
- Pass all existing tests  
- Not introduce new warnings or errors  
- Include tests when appropriate  
- Avoid unnecessary refactoring unless directly related to the change  

---

## Code of Conduct

By participating in this project, you agree to follow the project's Code of Conduct.
