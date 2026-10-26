import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export const PASSWORD_REGEX = /^(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?`~]).{8,16}$/;

export function passwordValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = control.value;
    if (!value) {
      return null;
    }

    const errors: ValidationErrors = {};

    if (value.length < 8 || value.length > 16) {
      errors['passwordLength'] = true;
    }
    if (!/[A-Z]/.test(value)) {
      errors['passwordUppercase'] = true;
    }
    if (!/\d/.test(value)) {
      errors['passwordNumeric'] = true;
    }
    if (!/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?`~]/.test(value)) {
      errors['passwordSpecial'] = true;
    }

    if (Object.keys(errors).length > 0) {
      return {
        invalidPassword: true,
        ...errors
      };
    }

    return null;
  };
}
