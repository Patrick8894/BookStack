package com.bookstack.bookstack.book.controller;

import com.bookstack.bookstack.book.model.Book;
import com.bookstack.bookstack.book.service.BookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@GraphQlTest(controllers = BookGraphQLController.class)
@TestPropertySource(properties = "bookstack.auth.enabled=false") // Disable auth aspect
class BookGraphQLControllerTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private BookService bookService;

    private Book sampleBook;
    private List<Book> sampleBooks;

    @BeforeEach
    void setUp() {
        sampleBook = new Book();
        sampleBook.setId(1L);
        sampleBook.setTitle("The Great Gatsby");
        sampleBook.setAuthor("F. Scott Fitzgerald");
        sampleBook.setIsbn("978-0-7432-7356-5");
        sampleBook.setCategory("Fiction");
        sampleBook.setLanguage("English");
        sampleBook.setTotalCopies(5);
        sampleBook.setAvailableCopies(3);

        sampleBooks = Arrays.asList(sampleBook);
    }

    @Test
    void allBooks_ShouldReturnExpectedShape() {
        // Given
        when(bookService.getAllBooks()).thenReturn(sampleBooks);

        // When & Then - Verify field names and structure
        graphQlTester.document("""
            query {
                allBooks {
                    id
                    title
                    author
                    isbn
                    category
                    language
                    totalCopies
                    availableCopies
                }
            }
            """)
            .execute()
            .path("allBooks[0].id").entity(Long.class).isEqualTo(1L)
            .path("allBooks[0].title").entity(String.class).isEqualTo("The Great Gatsby")
            .path("allBooks[0].author").entity(String.class).isEqualTo("F. Scott Fitzgerald")
            .path("allBooks[0].isbn").entity(String.class).isEqualTo("978-0-7432-7356-5")
            .path("allBooks[0].category").entity(String.class).isEqualTo("Fiction")
            .path("allBooks[0].language").entity(String.class).isEqualTo("English")
            .path("allBooks[0].totalCopies").entity(Integer.class).isEqualTo(5)
            .path("allBooks[0].availableCopies").entity(Integer.class).isEqualTo(3);
    }

    @Test
    void bookById_WithNullResult_ShouldReturnNull() {
        // Given
        when(bookService.getBookById(999L)).thenReturn(null);

        // When & Then - Verify null handling
        graphQlTester.document("""
            query {
                bookById(id: 999) {
                    id
                    title
                }
            }
            """)
            .execute()
            .path("bookById").valueIsNull();
    }

    @Test
void addBook_WithValidInput_ShouldReturnBook() {
    // Given
    Book newBook = new Book();
    newBook.setId(2L);
    newBook.setTitle("New Book");
    newBook.setAuthor("Valid Author");
    newBook.setIsbn("978-1-234-56789-0");
    newBook.setCategory("Science");
    newBook.setLanguage("English");
    newBook.setTotalCopies(2);
    newBook.setAvailableCopies(2);

    when(bookService.addBook(any(Book.class))).thenReturn(newBook);

    // When & Then - Test successful mutation
    graphQlTester.document("""
        mutation {
            addBook(input: {
                title: "New Book",
                author: "Valid Author",
                isbn: "978-1-234-56789-0",
                category: "Science",
                language: "English",
                totalCopies: 2,
                availableCopies: 2
            }) {
                id
                title
                author
                isbn
                category
                language
                totalCopies
                availableCopies
            }
        }
        """)
        .execute()
        .path("addBook.id").entity(Long.class).isEqualTo(2L)
        .path("addBook.title").entity(String.class).isEqualTo("New Book")
        .path("addBook.author").entity(String.class).isEqualTo("Valid Author")
        .path("addBook.isbn").entity(String.class).isEqualTo("978-1-234-56789-0")
        .path("addBook.category").entity(String.class).isEqualTo("Science")
        .path("addBook.language").entity(String.class).isEqualTo("English")
        .path("addBook.totalCopies").entity(Integer.class).isEqualTo(2)
        .path("addBook.availableCopies").entity(Integer.class).isEqualTo(2);
}

    @Test
    void searchBooks_ShouldReturnExpectedShape() {
        // Given
        when(bookService.searchBooks("Gatsby", null, "Fiction", null)).thenReturn(sampleBooks);

        // When & Then - Verify search query works with optional parameters
        graphQlTester.document("""
            query {
                searchBooks(title: "Gatsby", category: "Fiction") {
                    id
                    title
                    category
                }
            }
            """)
            .execute()
            .path("searchBooks").entityList(Book.class).hasSize(1)
            .path("searchBooks[0].id").entity(Long.class).isEqualTo(1L)
            .path("searchBooks[0].title").entity(String.class).isEqualTo("The Great Gatsby")
            .path("searchBooks[0].category").entity(String.class).isEqualTo("Fiction");
    }
}