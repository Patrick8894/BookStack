// Validation rules that match your backend BookInput constraints
export interface BookValidationError {
  field: string;
  message: string;
}

export interface BookValidationData {
  title: string;
  author: string;
  isbn: string;
  category: string;
  language: string;
  totalCopies: number | string;
  availableCopies: number | string;
}

export function validateBook(bookData: BookValidationData, isUpdate = false): BookValidationError[] {
  const errors: BookValidationError[] = [];

  // Title validation
  if (!bookData.title || bookData.title.trim() === '') {
    errors.push({ field: 'title', message: 'Title is required' });
  } else if (bookData.title.length < 1 || bookData.title.length > 200) {
    errors.push({ field: 'title', message: 'Title must be between 1 and 200 characters' });
  }

  // Author validation
  if (!bookData.author || bookData.author.trim() === '') {
    errors.push({ field: 'author', message: 'Author is required' });
  } else if (bookData.author.length < 1 || bookData.author.length > 100) {
    errors.push({ field: 'author', message: 'Author must be between 1 and 100 characters' });
  }

  // ISBN validation
  if (!bookData.isbn || bookData.isbn.trim() === '') {
    errors.push({ field: 'isbn', message: 'ISBN is required' });
  } else {
    // Basic ISBN format validation (10 or 13 digits, with optional hyphens)
    const isbnClean = bookData.isbn.replace(/[-\s]/g, '');
    if (!/^\d{10}(\d{3})?$/.test(isbnClean)) {
      errors.push({ field: 'isbn', message: 'ISBN must be 10 or 13 digits' });
    }
  }

  // Category validation
  if (!bookData.category || bookData.category.trim() === '') {
    errors.push({ field: 'category', message: 'Category is required' });
  } else if (bookData.category.length < 1 || bookData.category.length > 50) {
    errors.push({ field: 'category', message: 'Category must be between 1 and 50 characters' });
  }

  // Language validation
  if (!bookData.language || bookData.language.trim() === '') {
    errors.push({ field: 'language', message: 'Language is required' });
  } else if (bookData.language.length < 1 || bookData.language.length > 30) {
    errors.push({ field: 'language', message: 'Language must be between 1 and 30 characters' });
  }

  // Total copies validation
  const totalCopies = typeof bookData.totalCopies === 'string' 
    ? parseInt(bookData.totalCopies, 10) 
    : bookData.totalCopies;
    
  if (bookData.totalCopies === undefined || bookData.totalCopies === null || bookData.totalCopies === '') {
    errors.push({ field: 'totalCopies', message: 'Total copies is required' });
  } else if (isNaN(totalCopies) || totalCopies < 0 || !Number.isInteger(totalCopies)) {
    errors.push({ field: 'totalCopies', message: 'Total copies must be a non-negative integer' });
  }

  // Available copies validation
  const availableCopies = typeof bookData.availableCopies === 'string' 
    ? parseInt(bookData.availableCopies, 10) 
    : bookData.availableCopies;
    
  if (bookData.availableCopies === undefined || bookData.availableCopies === null || bookData.availableCopies === '') {
    errors.push({ field: 'availableCopies', message: 'Available copies is required' });
  } else if (isNaN(availableCopies) || availableCopies < 0 || !Number.isInteger(availableCopies)) {
    errors.push({ field: 'availableCopies', message: 'Available copies must be a non-negative integer' });
  } else if (!isNaN(totalCopies) && availableCopies > totalCopies) {
    errors.push({ field: 'availableCopies', message: 'Available copies cannot exceed total copies' });
  }

  return errors;
}