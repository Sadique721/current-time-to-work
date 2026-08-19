import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { routes, CommonService } from 'src/app/core.index';
import { TelecomCircle, TelecomCircleService } from '../telecom-circle.service';

@Component({
  selector: 'app-telecom-circle-add-edit',
  templateUrl: './telecom-circle-add-edit.component.html',
  styleUrl: './telecom-circle-add-edit.component.scss',
  standalone: false,
})
export class TelecomCircleAddEditComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  form: FormGroup;
  
  submitted = false;
  isCollapsed = false;
  selectedTelecomCircle: any = null;
  isLoading = false;

  public routes = routes;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private telecomCircleService: TelecomCircleService,
    private commonService: CommonService
  ) {
    this.form = this.fb.group({
      name: ['', [Validators.required]],
      prefix: ['', [Validators.required]],
      status: ['1', Validators.required],
    });
  }

  ngOnInit(): void {
    const id = history.state?.id;
    if (id) {
      this.selectedTelecomCircle = { id: +id };
      this.loadTelecomCircleFromApi(+id);
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadTelecomCircleFromApi(id: number): void {
    this.isLoading = true;
    this.commonService.spinnerShow();

    this.telecomCircleService.getById(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (circle: TelecomCircle) => {
          this.selectedTelecomCircle = circle;
          this.patchFormForEdit(circle);
          this.isLoading = false;
          this.commonService.spinnerHide();
        },
        error: (error) => {
          this.commonService.toastError('Failed to load telecom circle');
          this.isLoading = false;
          this.commonService.spinnerHide();
          this.onCancel();
        }
      });
  }

  private patchFormForEdit(circle: TelecomCircle): void {
    this.form.patchValue({
      name: circle.name,
      prefix: circle.prefix,
      status: circle.status,
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

    if (this.selectedTelecomCircle?.id) {
      this.telecomCircleService.update(this.selectedTelecomCircle.id, payload)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.commonService.toastSuccess('Telecom circle updated successfully');
            this.commonService.spinnerHide();
            this.isLoading = false;
            setTimeout(() => this.onCancel(), 500);
          },
          error: (error) => {
            this.commonService.toastError('Failed to update telecom circle');
            this.commonService.spinnerHide();
            this.isLoading = false;
          }
        });
    } else {
      this.telecomCircleService.create(payload)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.commonService.toastSuccess('Telecom circle created successfully');
            this.commonService.spinnerHide();
            this.isLoading = false;
            setTimeout(() => this.onCancel(), 500);
          },
          error: (error) => {
            this.commonService.toastError('Failed to create telecom circle');
            this.commonService.spinnerHide();
            this.isLoading = false;
          }
        });
    }
  }

  clearForm(): void {
    if (this.isLoading) return;

    this.form.reset({ 
      name: '', 
      prefix: '',
      status: '1' 
    });
    
    this.submitted = false;
    this.form.markAsUntouched();
    this.form.markAsPristine();
    
    this.commonService.toastSuccess('Form cleared');
  }

  onCancel(): void {
    if (this.isLoading) return;
    this.router.navigate([this.routes.telecomcircle], { replaceUrl: true });
  }

  toggleCollapse(): void {
    this.isCollapsed = !this.isCollapsed;
  }
}