import { useNuxtApp } from '#app';
import { GraphQLError, handleGraphQLResponse } from '@/utils/graphql-errors';

export async function getAllBooks() {
  const query = `
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
  `;
  
  try {
    const { $api } = useNuxtApp();
    const res = await $api.post('/graphql', { query });
    
    return handleGraphQLResponse(res, 'allBooks');
  } catch (error) {
    console.error('Failed to get all books:', error);
    throw error;
  }
}

export async function searchBooks(filters: { title?: string; author?: string; category?: string }) {
  const query = `
    query($title: String, $author: String, $category: String) {
      searchBooks(title: $title, author: $author, category: $category) {
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
  `;
  
  try {
    const { $api } = useNuxtApp();
    const res = await $api.post('/graphql', { query, variables: filters });
    
    return handleGraphQLResponse(res, 'searchBooks');
  } catch (error) {
    console.error('Failed to search books:', error);
    throw error;
  }
}

export async function getBookById(id: number) {
  const query = `
    query($id: ID!) {
      bookById(id: $id) {
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
  `;
  
  try {
    const { $api } = useNuxtApp();
    const res = await $api.post('/graphql', { query, variables: { id } });
    
    return handleGraphQLResponse(res, 'bookById', `Book with ID ${id} not found`);
  } catch (error) {
    console.error(`Failed to get book by ID ${id}:`, error);
    throw error;
  }
}

export async function addBook(input: any) {
  const mutation = `
    mutation($input: BookInput!) {
      addBook(input: $input) {
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
  `;
  
  try {
    const { $api } = useNuxtApp();
    const res = await $api.post('/graphql', { query: mutation, variables: { input } });
    
    const result = handleGraphQLResponse(res, 'addBook');
    
    if (!result) {
      throw new Error('Failed to create book');
    }
    
    return result;
  } catch (error) {
    console.error('Failed to create book:', error);
    
    // Re-throw GraphQL errors with more context
    if (error instanceof GraphQLError) {
      throw new Error(`Create failed: ${error.message}`);
    }
    
    throw error;
  }
}

export async function updateBook(id: number, input: any) {
  const mutation = `
    mutation($id: ID!, $input: BookInput!) {
      updateBook(id: $id, input: $input) {
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
  `;
  
  try {
    const { $api } = useNuxtApp();
    const res = await $api.post('/graphql', { query: mutation, variables: { id, input } });
    
    const result = handleGraphQLResponse(res, 'updateBook');
    
    if (!result) {
      throw new Error(`Failed to update book with ID ${id}`);
    }
    
    return result;
  } catch (error) {
    console.error(`Failed to update book with ID ${id}:`, error);
    
    // Re-throw GraphQL errors with more context
    if (error instanceof GraphQLError) {
      throw new Error(`Update failed: ${error.message}`);
    }
    
    throw error;
  }
}

export async function deleteBook(id: number) {
  const mutation = `
    mutation($id: ID!) {
      deleteBook(id: $id)
    }
  `;
  
  try {
    const { $api } = useNuxtApp();
    const res = await $api.post('/graphql', { query: mutation, variables: { id } });
    
    const result = handleGraphQLResponse(res, 'deleteBook');
    
    if (result === false) {
      throw new Error(`Failed to delete book with ID ${id}`);
    }
    
    return result;
  } catch (error) {
    console.error(`Failed to delete book with ID ${id}:`, error);
    
    // Re-throw GraphQL errors with more context
    if (error instanceof GraphQLError) {
      throw new Error(`Delete failed: ${error.message}`);
    }
    
    throw error;
  }
}