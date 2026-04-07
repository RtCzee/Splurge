# 🤝 Contributing Guide Splurge

This document outlines how all team members should contribute to the Splurge project. Follow these rules to ensure smooth collaboration and maintain code quality.

---

## 🚀 Getting Started

1. Clone the repository:

```bash
git clone <repo-url>
```

2. Open the project in Android Studio

3. Make sure you are on the `dev` branch:

```bash
git checkout dev
```

4. Pull the latest changes:

```bash
git pull origin dev
```

---

## 🌿 Branching Strategy

We use the following branches:

* `main` → Stable version (protected)
* `dev` → Active development branch
* `feature/*` → Individual features

### Example:

```bash
feature/dashboard-ui
feature/auth-system
feature/transactions
```

---

## 🧠 Workflow

Follow this process for every task:

1. Pull latest code:

```bash
git checkout dev
git pull
```

2. Create a new feature branch:

```bash
git checkout -b feature/your-feature-name
```

3. Work on your feature

4. Commit your changes:

```bash
git add .
git commit -m "feat: short description of feature"
```

5. Push your branch:

```bash
git push origin feature/your-feature-name
```

6. Create a Pull Request on GitHub → target `dev`

---

## 🔁 Pull Request (PR) Rules

* All work must be submitted via Pull Request
* PR must include:

  * Description of what was done
  * Screenshots (for UI changes)
* Ensure the app builds and runs before submitting
* Do NOT merge your own PR
* Wait for approval before merging

---

## 🧾 Commit Message Rules

Use clear and consistent commit messages:

* `feat: add dashboard UI`
* `fix: resolve login bug`
* `refactor: improve transaction logic`

---

## 🧩 Issue Assignment Rules

* One issue = one person
* Do not start work without assigning yourself
* Do not work on the same issue as someone else
* Update issue status (In Progress / Done)

---

## ✅ Definition of Done

A task is complete only when:

* Feature works correctly
* No crashes or major bugs
* Code is clean and readable
* Changes are pushed to GitHub
* Pull Request is approved

---

## 🧱 Coding Guidelines

* Write clean, readable code
* Use meaningful variable names
* Comment important logic
* Follow existing project structure
* Do not break existing features

---

## ⚠️ Error Handling Rules

* Do not allow the app to crash
* Handle invalid inputs (empty fields, wrong formats)
* Show user-friendly error messages
* Log errors for debugging

---

## 🔒 Important Rules

❌ Do NOT push directly to `main`
❌ Do NOT merge your own PR
❌ Do NOT break the app

✅ Always test before pushing
✅ Always pull before starting work
✅ Keep commits clean

---

## 📢 Communication

* Inform the team when starting a task
* Ask before making major changes
* Avoid duplicate work
* Share progress regularly

---



                                                    _ Didintle Mokgoro