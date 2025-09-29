import { useNuxtApp } from '#app';
import { GraphQLError, handleGraphQLResponse } from '@/utils/graphql-errors';

export async function getAllUsers() {
  const query = `
    query {
      allUsers {
        id
        username
        role
      }
    }
  `;
  
  try {
    const { $api } = useNuxtApp();
    const res = await $api.post('/graphql', { query });
    
    return handleGraphQLResponse(res, 'allUsers');
  } catch (error) {
    console.error('Failed to get all users:', error);
    throw error;
  }
}

export async function searchUsersByUsername(username: string) {
  const query = `
    query($username: String!) {
      searchUsersByUsername(username: $username) {
        id
        username
        role
      }
    }
  `;
  
  try {
    const { $api } = useNuxtApp();
    const res = await $api.post('/graphql', { query, variables: { username } });
    
    return handleGraphQLResponse(res, 'searchUsersByUsername');
  } catch (error) {
    console.error(`Failed to search users by username "${username}":`, error);
    throw error;
  }
}

export async function getUserById(id: number) {
  const query = `
    query($id: ID!) {
      userById(id: $id) {
        id
        username
        role
      }
    }
  `;
  
  try {
    const { $api } = useNuxtApp();
    const res = await $api.post('/graphql', { query, variables: { id } });
    
    return handleGraphQLResponse(res, 'userById', `User with ID ${id} not found`);
  } catch (error) {
    console.error(`Failed to get user by ID ${id}:`, error);
    throw error;
  }
}

export async function updateUser(id: number, input: { username: string; password?: string; role: string }) {
  const mutation = `
    mutation($id: ID!, $input: UserInput!) {
      updateUser(id: $id, input: $input) {
        id
        username
        role
      }
    }
  `;
  
  try {
    const { $api } = useNuxtApp();
    const res = await $api.post('/graphql', { query: mutation, variables: { id, input } });
    
    const result = handleGraphQLResponse(res, 'updateUser');
    
    if (!result) {
      throw new Error(`Failed to update user with ID ${id}`);
    }
    
    return result;
  } catch (error) {
    console.error(`Failed to update user with ID ${id}:`, error);
    
    // Re-throw GraphQL errors with more context
    if (error instanceof GraphQLError) {
      throw new Error(`Update failed: ${error.message}`);
    }
    
    throw error;
  }
}

export async function deleteUser(id: number) {
  const mutation = `
    mutation($id: ID!) {
      deleteUser(id: $id)
    }
  `;
  
  try {
    const { $api } = useNuxtApp();
    const res = await $api.post('/graphql', { query: mutation, variables: { id } });
    
    const result = handleGraphQLResponse(res, 'deleteUser');
    
    if (result === false) {
      throw new Error(`Failed to delete user with ID ${id}`);
    }
    
    return result;
  } catch (error) {
    console.error(`Failed to delete user with ID ${id}:`, error);
    
    // Re-throw GraphQL errors with more context
    if (error instanceof GraphQLError) {
      throw new Error(`Delete failed: ${error.message}`);
    }
    
    throw error;
  }
}

export async function createUser(input: { username: string; password: string; role: string }) {
  const mutation = `
    mutation($input: UserInput!) {
      addUser(input: $input) {
        id
        username
        role
      }
    }
  `;
  
  try {
    const { $api } = useNuxtApp();
    const res = await $api.post('/graphql', { query: mutation, variables: { input } });
    
    const result = handleGraphQLResponse(res, 'addUser');
    
    if (!result) {
      throw new Error('Failed to create user');
    }
    
    return result;
  } catch (error) {
    console.error('Failed to create user:', error);
    
    // Re-throw GraphQL errors with more context
    if (error instanceof GraphQLError) {
      throw new Error(`Create failed: ${error.message}`);
    }
    
    throw error;
  }
}