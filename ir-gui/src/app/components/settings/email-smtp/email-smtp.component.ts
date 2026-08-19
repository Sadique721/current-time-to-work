import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';
import { CommonService, SidebarService } from 'src/app/core.index';
import { EmailSMTPConfig, EmailSMTPService } from './email-smtp.service';

@Component({
  selector: 'app-email-smtp',
  templateUrl: './email-smtp.component.html',
  styleUrl: './email-smtp.component.scss',
  standalone: false,
})
export class EmailSMTPComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  form: FormGroup;
  
  submitted = false;
  isLoading = false;
  isSaving = false;
  isCollapsed = false;
  showPassword = false;

  constructor(
    private fb: FormBuilder,
    private emailSMTPService: EmailSMTPService,
    private commonService: CommonService,
    private sidebar: SidebarService,
  ) {
    
    this.form = this.fb.group({
      host: ['', [Validators.required]],
      port: ['', [Validators.required, Validators.pattern(/^\d+$/)]],
      from: ['', [Validators.required, Validators.email]],
      username: ['', [Validators.required]],
      password: ['', [Validators.required]],
    });
  }

  ngOnInit(): void {
    this.loadSMTPConfig();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

    private loadSMTPConfig(): void {
    this.isLoading = true;
    this.commonService.spinnerShow();

    this.emailSMTPService.getConfig()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (config: EmailSMTPConfig) => {
          this.form.patchValue({
            host: config.host,
            port: config.port,
            from: config.from,
            username: config.username,
            password: config.password,
          });
          this.isLoading = false;
          this.commonService.spinnerHide();
        },
        error: (error) => {
                    this.commonService.toastError('Failed to load SMTP configuration');
          this.isLoading = false;
          this.commonService.spinnerHide();
        }
      });
  }

    togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

    onSave(): void {
    this.submitted = true;

    
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.commonService.toastError('Please fill all required fields correctly');
      return;
    }

    const config: EmailSMTPConfig = this.form.value;

    this.isSaving = true;
    this.commonService.spinnerShow();

    this.emailSMTPService.updateConfig(config)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.commonService.toastSuccess('SMTP configuration saved successfully');
          this.commonService.spinnerHide();
          this.isSaving = false;
          this.submitted = false;
        },
        error: (error) => {
                    this.commonService.toastError('Failed to save SMTP configuration');
          this.commonService.spinnerHide();
          this.isSaving = false;
        }
      });
  }


    resetForm(): void {
    this.form.reset({
      host: '',
      port: '',
      from: '',
      username: '',
      password: '',
    });
    this.submitted = false;
    this.commonService.toastSuccess('Form reset');
  }

   toggleCollapse(): void {
    this.sidebar.toggleCollapse();
    this.isCollapsed = !this.isCollapsed;
  }
}