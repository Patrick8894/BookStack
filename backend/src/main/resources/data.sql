-- Insert Users (passwords are 'password123' encoded with BCrypt)
INSERT INTO users (username, password, role, deleted_at) VALUES 
('admin', '$2a$10$zBsYTPETAHefSCqBVUNaeep/6es9B177MpyBlX0H6WFi9EKjo.Wt.', 'ADMIN', NULL),
('librarian1', '$2a$10$zBsYTPETAHefSCqBVUNaeep/6es9B177MpyBlX0H6WFi9EKjo.Wt.', 'LIBRARIAN', NULL),
('librarian2', '$2a$10$zBsYTPETAHefSCqBVUNaeep/6es9B177MpyBlX0H6WFi9EKjo.Wt.', 'LIBRARIAN', NULL),
('member1', '$2a$10$zBsYTPETAHefSCqBVUNaeep/6es9B177MpyBlX0H6WFi9EKjo.Wt.', 'MEMBER', NULL),
('member2', '$2a$10$zBsYTPETAHefSCqBVUNaeep/6es9B177MpyBlX0H6WFi9EKjo.Wt.', 'MEMBER', NULL),
('johndoe', '$2a$10$zBsYTPETAHefSCqBVUNaeep/6es9B177MpyBlX0H6WFi9EKjo.Wt.', 'MEMBER', NULL),
('janedoe', '$2a$10$zBsYTPETAHefSCqBVUNaeep/6es9B177MpyBlX0H6WFi9EKjo.Wt.', 'MEMBER', NULL);

-- Insert Books
INSERT INTO books (title, author, isbn, category, language, total_copies, available_copies, deleted_at) VALUES 
('The Great Gatsby', 'F. Scott Fitzgerald', '978-0-7432-7356-5', 'Fiction', 'English', 5, 3, NULL),
('To Kill a Mockingbird', 'Harper Lee', '978-0-06-112008-4', 'Fiction', 'English', 4, 2, NULL),
('1984', 'George Orwell', '978-0-452-28423-4', 'Dystopian Fiction', 'English', 6, 4, NULL),
('Pride and Prejudice', 'Jane Austen', '978-0-14-143951-8', 'Romance', 'English', 3, 1, NULL),
('The Catcher in the Rye', 'J.D. Salinger', '978-0-316-76948-0', 'Fiction', 'English', 4, 4, NULL),
('Lord of the Flies', 'William Golding', '978-0-571-05686-2', 'Fiction', 'English', 5, 3, NULL),
('Brave New World', 'Aldous Huxley', '978-0-06-085052-4', 'Science Fiction', 'English', 3, 2, NULL),
('The Hobbit', 'J.R.R. Tolkien', '978-0-547-92822-7', 'Fantasy', 'English', 7, 5, NULL),
('Fahrenheit 451', 'Ray Bradbury', '978-1-4516-7331-9', 'Dystopian Fiction', 'English', 4, 3, NULL),
('Animal Farm', 'George Orwell', '978-0-452-28424-1', 'Political Satire', 'English', 5, 4, NULL),
('One Hundred Years of Solitude', 'Gabriel García Márquez', '978-0-06-088328-8', 'Magical Realism', 'Spanish', 3, 2, NULL),
('The Alchemist', 'Paulo Coelho', '978-0-06-231500-7', 'Adventure', 'Portuguese', 6, 4, NULL),
('Don Quixote', 'Miguel de Cervantes', '978-0-06-093434-4', 'Adventure', 'Spanish', 2, 1, NULL),
('Crime and Punishment', 'Fyodor Dostoevsky', '978-0-14-044913-6', 'Psychological Fiction', 'Russian', 3, 2, NULL),
('War and Peace', 'Leo Tolstoy', '978-0-14-044793-4', 'Historical Fiction', 'Russian', 2, 1, NULL);

-- Insert Borrows (some active, some returned, some overdue)
INSERT INTO borrows (user_id, book_id, status, borrow_date, due_date, return_date, notes) VALUES 
-- Active borrows
(4, 1, 'ACTIVE', '2024-01-15 10:00:00', '2024-01-29 10:00:00', NULL, 'Great condition'),
(5, 2, 'ACTIVE', '2024-01-20 14:30:00', '2024-02-03 14:30:00', NULL, 'First time borrowing'),
(6, 4, 'ACTIVE', '2024-01-25 09:15:00', '2024-02-08 09:15:00', NULL, 'Excited to read this classic'),
(7, 8, 'ACTIVE', '2024-02-01 16:45:00', '2024-02-15 16:45:00', NULL, 'Love fantasy books'),

-- Overdue borrows (due date in the past, still ACTIVE)
(4, 3, 'OVERDUE', '2023-12-01 10:00:00', '2023-12-15 10:00:00', NULL, 'Should have returned by now'),
(5, 6, 'OVERDUE', '2023-12-10 11:30:00', '2023-12-24 11:30:00', NULL, 'Lost track of time'),

-- Returned borrows
(6, 5, 'RETURNED', '2023-11-01 09:00:00', '2023-11-15 09:00:00', '2023-11-12 15:30:00', 'Returned early, excellent book'),
(7, 7, 'RETURNED', '2023-11-10 14:00:00', '2023-11-24 14:00:00', '2023-11-23 10:15:00', 'Great read'),
(4, 9, 'RETURNED', '2023-12-05 16:20:00', '2023-12-19 16:20:00', '2023-12-18 13:45:00', 'Thought-provoking'),
(5, 10, 'RETURNED', '2023-12-15 12:00:00', '2023-12-29 12:00:00', '2023-12-28 17:30:00', 'Loved it!');