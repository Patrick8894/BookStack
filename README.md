# 📚 BookStack - Library Management System

BookStack is a full-stack **Library Management System** for managing books, users, and lending operations.  
The system supports **role-based access** with separate functionality for Admins, Librarians, and Members.  
Both frontend and backend are containerized using **Docker Compose** for easy deployment.

---

## ✨ Core Features

### 🔐 User Authentication & Roles
- REST (Spring Security + JWT)
- Roles:
  - **Admin** – manage users, books, borrow records
  - **Librarian** – manage books, handle borrow/return
  - **Member** – view/search books, check personal borrow history

### 📖 Book Management
- GraphQL queries for book search/filter
- GraphQL mutations for add/edit/delete books (Admin/Librarian only)

### 🔄 Borrowing & Returning
- REST endpoints for borrow/return transactions (Admin/Librarian only)
- Members can only view their own borrow/return records

### 🔎 Search & Filter
- GraphQL queries by title, author, category, or availability

---

## 🛠 Tech Stack

- **Frontend**: Nuxt 3 (Vue framework) + Vuetify + Apollo Client (GraphQL)
- **Backend**: Spring Boot (REST + GraphQL, Spring Security + JWT)
- **Database**: PostgreSQL
- **Containerization**: Docker Compose (frontend + backend + database)
- **API Style**: Hybrid (REST for actions/auth, GraphQL for queries)

---

## 🚀 Project Status
- ✅ Backend server finished (Spring Boot + JWT + GraphQL)  
- ✅ Docker Compose set up for backend, frontend, and database  
- 🚧 Frontend development ongoing (Nuxt 3 + Vuetify)  
- 📌 Next step: refine UI pages (Login/Register, Dashboard, Book List, Borrow Management, User Management)

---

## 🗂 Project Structure (High-Level)
```
bookstack/
├── backend/ # Spring Boot backend
├── frontend/ # Nuxt 3 + Vuetify frontend
├── docker-compose.yml
└── README.md
```

---

## 🚀 Planned Extensions (Future Work)
- Reservation / Hold requests  
- Overdue fine calculation  
- Email/SMS notifications  
- Reports & analytics  
- QR/Barcode support  

---