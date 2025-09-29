// Validation rules that match your backend UserInput constraints
export interface ValidationError {
  field: string;
  message: string;
}

export interface UserValidationData {
  username: string;
  password?: string;
  role: string;
}

export function validateUser(userData: UserValidationData, isUpdate = false): ValidationError[] {
  const errors: ValidationError[] = [];

  // Username validation
  if (!userData.username || userData.username.trim() === '') {
    errors.push({ field: 'username', message: 'Username is required' });
  } else if (userData.username.length < 3 || userData.username.length > 50) {
    errors.push({ field: 'username', message: 'Username must be between 3 and 50 characters' });
  }

  // Password validation (required for new users, optional for updates)
  if (!isUpdate) {
    // For new users, password is required
    if (!userData.password || userData.password.trim() === '') {
      errors.push({ field: 'password', message: 'Password is required' });
    } else if (userData.password.length < 6) {
      errors.push({ field: 'password', message: 'Password must be at least 6 characters' });
    }
  } else {
    // For updates, validate password only if provided
    if (userData.password && userData.password.trim() !== '' && userData.password.length < 6) {
      errors.push({ field: 'password', message: 'Password must be at least 6 characters' });
    }
  }

  // Role validation (optional but should be valid if provided)
  if (userData.role && !['ADMIN', 'LIBRARIAN', 'MEMBER'].includes(userData.role)) {
    errors.push({ field: 'role', message: 'Role must be ADMIN, LIBRARIAN, or MEMBER' });
  }

  return errors;
}