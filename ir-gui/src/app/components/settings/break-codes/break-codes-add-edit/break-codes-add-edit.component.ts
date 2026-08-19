import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { routes, CommonService } from 'src/app/core.index';
import { BreakCode, BreakCodeService } from '../break-codes.service';

@Component({
  selector: 'app-break-code-add-edit',
  templateUrl: './break-codes-add-edit.component.html',
  styleUrl: './break-codes-add-edit.component.scss',
  standalone: false,
})
export class BreakCodesAddEditComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  form: FormGroup;
  
  submitted = false;
  isCollapsed = false;
  selectedBreakCode: any = null;
  isLoading = false;

  public routes = routes;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private breakCodeService: BreakCodeService,
    private commonService: CommonService
  ) {
    this.form = this.fb.group({
      breakCode: ['', [Validators.required]],
      name: ['', [Validators.required]],
      duration: ['', [Validators.required, Validators.pattern(/^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$/)]],
      description: [''],
      status: ['1', Validators.required],
    });
  }

  ngOnInit(): void {
    const id = history.state?.id;
    if (id) {
      this.selectedBreakCode = { id: +id };
      this.loadBreakCodeFromApi(+id);
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadBreakCodeFromApi(id: number): void {
    this.isLoading = true;
    this.commonService.spinnerShow();

    this.breakCodeService.getById(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (breakCode: BreakCode) => {
          this.selectedBreakCode = breakCode;
          this.patchFormForEdit(breakCode);
          this.isLoading = false;
          this.commonService.spinnerHide();
        },
        error: (error) => {
          this.commonService.toastError('Failed to load break code');
          this.isLoading = false;
          this.commonService.spinnerHide();
          this.onCancel();
        }
      });
  }

  private patchFormForEdit(breakCode: BreakCode): void {
    this.form.patchValue({
      breakCode: breakCode.breakCode,
      name: breakCode.name,
      duration: breakCode.duration,
      description: breakCode.description || '',
      status: breakCode.status,
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

    if (this.selectedBreakCode?.id) {
      this.breakCodeService.update(this.selectedBreakCode.id, payload)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.commonService.toastSuccess('Break code updated successfully');
            this.commonService.spinnerHide();
            this.isLoading = false;
            setTimeout(() => this.onCancel(), 500);
          },
          error: (error) => {
            this.commonService.toastError('Failed to update break code');
            this.commonService.spinnerHide();
            this.isLoading = false;
          }
        });
    } else {
      this.breakCodeService.create(payload)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.commonService.toastSuccess('Break code created successfully');
            this.commonService.spinnerHide();
            this.isLoading = false;
            setTimeout(() => this.onCancel(), 500);
          },
          error: (error) => {
            this.commonService.toastError('Failed to create break code');
            this.commonService.spinnerHide();
            this.isLoading = false;
          }
        });
    }
  }

  clearForm(): void {
    if (this.isLoading) return;

    this.form.reset({ 
      breakCode: '', 
      name: '',
      duration: '',
      description: '',
      status: '1' 
    });
    
    this.submitted = false;
    this.form.markAsUntouched();
    this.form.markAsPristine();
    
    this.commonService.toastSuccess('Form cleared');
  }

  onCancel(): void {
    if (this.isLoading) return;
    this.router.navigate([this.routes.breakcodes], { replaceUrl: true });
  }

  toggleCollapse(): void {
    this.isCollapsed = !this.isCollapsed;
  }
}