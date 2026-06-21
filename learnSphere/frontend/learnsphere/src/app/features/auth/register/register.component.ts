import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, RouterModule, ReactiveFormsModule],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss'] 
})
export class RegisterComponent {
  registerForm: FormGroup;
  selectedRole: 'STUDENT' | 'TEACHER' | 'ADMIN' = 'STUDENT'; // Default role

  roles = [
    { value: 'STUDENT', label: 'Student' },
    { value: 'TEACHER', label: 'Teacher' },
    { value: 'ADMIN', label: 'Admin' }
  ];

  constructor(private fb: FormBuilder, private router: Router) {
    this.registerForm = this.fb.group({
      fullName: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]]
    });
  }

  // Method to update the role when a user clicks a role button
  setRole(roleStr: string) {
    this.selectedRole = roleStr as 'STUDENT' | 'TEACHER' | 'ADMIN';
  }

  onSubmit() {
    if (this.registerForm.valid) {
      const payload = {
        role: this.selectedRole,
        ...this.registerForm.value
      };
      
      console.log('New User Registration:', payload);
      
      // After successful registration, kick them back to the login page
      this.router.navigate(['/login']);
    }
  }
}