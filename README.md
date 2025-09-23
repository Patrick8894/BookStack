# 📚 BookStack - Library Management System

BookStack is a full-stack **Library Management System** for managing books, members, and lending operations.  
This project is in the **early design stage**. Notes here capture the core ideas and tech choices.

---

## ✨ Core Features (Planned)

### 🔐 User Authentication & Roles
- REST (Spring Security + JWT)
- Roles: **Admin**, **Librarian**, **Member**

### 📖 Book Management
- GraphQL queries for book search/filter
- GraphQL mutations for add/edit/delete books

### 👥 Member Management
- REST endpoints for CRUD
- (Optional) GraphQL if nested data is needed

### 🔄 Borrowing & Returning
- REST endpoints for borrow/return transactions

### 🔎 Search & Filter
- GraphQL queries (by title, author, category, availability)

---

## 🛠 Tech Stack

- **Frontend**: Vue 3 (composition API, Pinia or Vuex for state, Apollo for GraphQL)
- **Backend**: Spring Boot (REST + GraphQL)
- **Database**: PostgreSQL or MySQL
- **API Style**: Hybrid (REST for actions/auth, GraphQL for queries)

---

## 🚀 Project Status
- Currently in **planning stage**  
- Documenting **core functions** and **stack decisions**  
- Next step: define **ERD (books, members, borrow records)** + basic API design  

---
