import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { routes, CommonService } from 'src/app/core.index';
import { LeadStatus, LeadStatusService } from '../lead-status.service';

@Component({
  selector: 'app-lead-status-add-edit',
  templateUrl: './lead-status-add-edit.component.html',
  styleUrl: './lead-status-add-edit.component.scss',
  standalone: false,
})
export class LeadStatusAddEditComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  form: FormGroup;
  
  submitted = false;
  isCollapsed = false;
  selectedLeadStatus: any = null;
  isLoading = false;

  public routes = routes;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private leadStatusService: LeadStatusService,
    private commonService: CommonService
  ) {
    
    this.form = this.fb.group({
      statusName: ['', [Validators.required]],
      code: ['', [Validators.required]],
      status: [true, Validators.required],
    });
  }

  ngOnInit(): void {
    
    const id = history.state?.id;
    if (id) {
      this.selectedLeadStatus = { id: +id };
      this.loadLeadStatusFromApi(+id);
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

    private loadLeadStatusFromApi(id: number): void {
    this.isLoading = true;
    this.commonService.spinnerShow();

    this.leadStatusService.getById(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (leadStatus: LeadStatus) => {
          this.selectedLeadStatus = leadStatus;
          this.patchFormForEdit(leadStatus);
          this.isLoading = false;
          this.commonService.spinnerHide();
        },
        error: (error) => {
                    this.commonService.toastError('Failed to load lead status');
          this.isLoading = false;
          this.commonService.spinnerHide();
          this.onCancel();
        }
      });
  }

  private patchFormForEdit(leadStatus: LeadStatus): void {
    this.form.patchValue({
      statusName: leadStatus.statusName,
      code: leadStatus.code,
      status: leadStatus.status,
    });
  }

    onSubmit(): void {
    this.submitted = true;

    
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.commonService.toastError('Please fill all required fields correctly');
      return;
    }

    const payload = this.form.value;

    this.isLoading = true;
    this.commonService.spinnerShow();

    if (this.selectedLeadStatus?.id) {
      
      this.leadStatusService.update(this.selectedLeadStatus.id, payload)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.commonService.toastSuccess('Lead status updated successfully');
            this.commonService.spinnerHide();
            this.isLoading = false;
            setTimeout(() => this.onCancel(), 500);
          },
          error: (error) => {
                        this.commonService.toastError('Failed to update lead status');
            this.commonService.spinnerHide();
            this.isLoading = false;
          }
        });
    } else {
      
      this.leadStatusService.create(payload)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.commonService.toastSuccess('Lead status created successfully');
            this.commonService.spinnerHide();
            this.isLoading = false;
            setTimeout(() => this.onCancel(), 500);
          },
          error: (error) => {
                        this.commonService.toastError('Failed to create lead status');
            this.commonService.spinnerHide();
            this.isLoading = false;
          }
        });
    }
  }

  clearForm(): void {
    if (this.isLoading) return;

    this.form.reset({ 
      statusName: '', 
      code: '',
      status: true 
    });
    
    this.submitted = false;
    this.form.markAsUntouched();
    this.form.markAsPristine();
    
    this.commonService.toastSuccess('Form cleared');
  }

  onCancel(): void {
    if (this.isLoading) return;
    this.router.navigate([this.routes.leadstatus], { replaceUrl: true });
  }

  toggleCollapse(): void {
    this.isCollapsed = !this.isCollapsed;
  }
}