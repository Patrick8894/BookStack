// Custom error class for GraphQL errors
export class GraphQLError extends Error {
  constructor(message: string, public errors: any[]) {
    super(message);
    this.name = 'GraphQLError';
  }
}

// Helper function to check for GraphQL errors
export function checkGraphQLErrors(res: any) {
  if (res.data.errors && res.data.errors.length > 0) {
    const errorMessages = res.data.errors.map((err: any) => err.message).join(', ');
    throw new GraphQLError(errorMessages, res.data.errors);
  }
}

// Helper function to handle GraphQL response with error checking
export function handleGraphQLResponse(res: any, dataPath: string, notFoundMessage?: string) {
  // Check for GraphQL errors first
  checkGraphQLErrors(res);
  
  // Navigate to the data
  const data = res.data.data[dataPath];
  
  // Check if data exists (for cases where null indicates "not found")
  if (data === null && notFoundMessage) {
    throw new Error(notFoundMessage);
  }
  
  return data;
}