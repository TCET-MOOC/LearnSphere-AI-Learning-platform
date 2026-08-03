import { Component, OnInit, TemplateRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { AdminService } from '../services/admin.service';
import { NotificationService } from '@core/services/notification.service';
import { College, AffiliationRequest } from '@core/models/user.model';
import { StatCardComponent } from '@shared/components/stat-cards/stat-card.component';
import { StatusPillComponent } from '@shared/components/status-pills/status-pill.component';
import { ConfirmDialogComponent } from '@shared/components/confirm-dialog/confirm-dialog.component';

/**
 * CollegesComponent enables platform-wide college database management.
 * Admin can verify/reject colleges, approve/reject teacher affiliation requests,
 * and manually register new institutions using an overlay form modal.
 */
@Component({
  selector: 'app-colleges',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    StatCardComponent,
    StatusPillComponent
  ],
  templateUrl: './colleges.component.html',
  styleUrls: ['./colleges.component.scss']
})
export class CollegesComponent implements OnInit {
  /**
   * Reference to the inline template used for the manual college-addition modal.
   */
  @ViewChild('addCollegeTpl') addCollegeTpl!: TemplateRef<any>;

  /**
   * Master lists for holding colleges and teacher affiliation data fetched from service.
   */
  colleges: College[] = [];
  affiliationRequests: AffiliationRequest[] = [];

  // KPI display variables
  totalCollegesCount = 0;
  pendingCollegesCount = 0;
  totalStudents = 0;
  totalTeachers = 0;

  /**
   * Form group tracking manual college inputs: college name and city.
   */
  collegeForm!: FormGroup;

  /**
   * Keep a reference to the active dialog to close it programmatically.
   */
  private activeDialogRef?: MatDialogRef<any>;

  constructor(
    private adminService: AdminService,
    private notificationService: NotificationService,
    private dialog: MatDialog,
    private fb: FormBuilder
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.loadData();
  }

  /**
   * Initialises the reactive form with validator controls.
   */
  initForm(): void {
    this.collegeForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(3)]],
      city: ['', [Validators.required, Validators.minLength(2)]]
    });
  }

  /**
   * Loads the colleges and teacher affiliations sequentially.
   */
  loadData(): void {
    this.loadColleges();
    this.loadAffiliationRequests();
  }

  /**
   * Fetches the registered colleges list from AdminService.
   */
  loadColleges(): void {
    this.adminService.getColleges().subscribe({
      next: (data: any) => {
        this.colleges = data;
        this.calculateStats();
      },
      error: (err: any) => console.error('Failed to load colleges', err)
    });
  }

  /**
   * Fetches the teacher affiliation requests list from AdminService.
   */
  loadAffiliationRequests(): void {
    this.adminService.getAffiliationRequests().subscribe({
      next: (data: any) => {
        this.affiliationRequests = data;
      },
      error: (err: any) => console.error('Failed to load affiliation requests', err)
    });
  }

  /**
   * Computes KPI stat metrics dynamically based on current colleges data.
   */
  calculateStats(): void {
    this.totalCollegesCount = this.colleges.length;
    this.pendingCollegesCount = this.colleges.filter(c => c.verificationStatus === 'PENDING').length;
    
    // Aggregates counts across verified institutions
    this.totalStudents = this.colleges.reduce((acc, curr) => acc + curr.studentCount, 0);
    this.totalTeachers = this.colleges.reduce((acc, curr) => acc + curr.teacherCount, 0);
  }

  /**
   * Opens the inline TemplateRef as a Material overlay dialog.
   */
  openAddCollegeDialog(): void {
    this.collegeForm.reset();
    this.activeDialogRef = this.dialog.open(this.addCollegeTpl, {
      width: '420px',
      disableClose: false
    });
  }

  /**
   * Handles form submission to manually register a new college.
   */
  onSubmitCollege(): void {
    if (this.collegeForm.invalid) {
      return;
    }

    const payload = this.collegeForm.value;
    this.adminService.createCollege(payload).subscribe({
      next: (newColl: any) => {
        this.notificationService.success(`Successfully registered "${newColl.name}"!`);
        this.activeDialogRef?.close();
        this.loadColleges(); // reload college lists and recalculate KPI stats
      },
      error: (err: any) => {
        this.notificationService.error('Failed to register college. Please try again.');
        console.error(err);
      }
    });
  }

  /**
   * Prompts user with a confirmation overlay, then marks a college as VERIFIED.
   */
  verifyCollege(college: College): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Verify College',
        message: `Are you sure you want to verify and whitelist "${college.name}"? This allows their students and teachers to access dashboards.`,
        confirmLabel: 'Verify',
        cancelLabel: 'Cancel'
      }
    });

    dialogRef.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.adminService.verifyCollege(college.id).subscribe({
          next: () => {
            this.notificationService.success(`College "${college.name}" has been verified!`);
            this.loadColleges();
          },
          error: (err: any) => {
            this.notificationService.error('Verification failed.');
            console.error(err);
          }
        });
      }
    });
  }

  /**
   * Prompts user with a confirmation overlay, then rejects/removes a pending college.
   */
  rejectCollege(college: College): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Reject College Request',
        message: `Are you sure you want to reject and decline the application for "${college.name}"? This decision will delete the application.`,
        confirmLabel: 'Reject',
        cancelLabel: 'Cancel'
      }
    });

    dialogRef.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.adminService.rejectCollege(college.id).subscribe({
          next: () => {
            this.notificationService.success(`Decline application for "${college.name}".`);
            this.loadColleges();
          },
          error: (err: any) => {
            this.notificationService.error('Decline request failed.');
            console.error(err);
          }
        });
      }
    });
  }

  /**
   * Utility to open the teacher ID verification document in a new tab.
   */
  reviewDocument(docUrl: string): void {
    if (docUrl) {
      window.open(docUrl, '_blank');
    } else {
      this.notificationService.error('No document URL provided for this request.');
    }
  }

  /**
   * Approves a teacher's affiliation request.
   */
  approveRequest(req: AffiliationRequest): void {
    this.adminService.approveAffiliation(req.id).subscribe({
      next: () => {
        this.notificationService.success(`Approved affiliation request for ${req.teacherName}.`);
        this.loadAffiliationRequests(); // refresh lists
        // Simulate updating college teacher numbers for verified college
        this.loadColleges();
      },
      error: (err: any) => {
        this.notificationService.error('Failed to approve request.');
        console.error(err);
      }
    });
  }

  /**
   * Rejects a teacher's affiliation request.
   */
  rejectRequest(req: AffiliationRequest): void {
    this.adminService.rejectAffiliation(req.id).subscribe({
      next: () => {
        this.notificationService.success(`Rejected affiliation request for ${req.teacherName}.`);
        this.loadAffiliationRequests();
      },
      error: (err: any) => {
        this.notificationService.error('Failed to reject request.');
        console.error(err);
      }
    });
  }

  /**
   * Helper placeholder method for managing a college.
   */
  manageCollege(college: College): void {
    // Sourced from requirements: "Verified rows show a Manage button (logs a // TODO since no detail page exists)"
    console.log(`// TODO: Manage college details for ID: ${college.id}`);
    this.notificationService.info(`Management dashboard for "${college.name}" is coming soon.`);
  }
}
