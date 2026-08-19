import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output,
} from '@angular/core';
import {
  UntypedFormGroup,
  UntypedFormControl,
  Validators,
  ReactiveFormsModule,
} from '@angular/forms';
import { CustomElementModule } from 'src/app/core/shared/custom-elements/custom-elemets.module';
import { sharedModule } from 'src/app/core/shared/shared.module';
import { StaffManagementService } from '../staff-management.service';
import { catchError, finalize, Subject, takeUntil } from 'rxjs';
import { CommonService } from 'src/app/core/service/common.service';
import { CommonModule } from '@angular/common';

declare var bootstrap: any;

@Component({
  selector: 'app-change-password',
  imports: [ReactiveFormsModule, CustomElementModule, sharedModule , CommonModule],
  templateUrl: './change-password.component.html',
  styleUrl: './change-password.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ChangePasswordComponent implements OnInit {
  @Output() close = new EventEmitter<boolean>();
  @Input() selectedStaff: any = null;
  private destroy$ = new Subject<void>();
  changePasswordForm: UntypedFormGroup;
  ifgenerateOtpField = true;
  isOtpGenerated = false;

  constructor(
    private staffManagementService: StaffManagementService,
    private common: CommonService,
    private cdr: ChangeDetectorRef,
  ) {
    this.changePasswordForm = new UntypedFormGroup({
      userName: new UntypedFormControl({ value: '', disabled: true }, [
        Validators.required,
      ]),
      otp: new UntypedFormControl('', [Validators.required]),
      newPassword: new UntypedFormControl('', [Validators.required]),
      confirmPassword: new UntypedFormControl('', [Validators.required]),
    });
  }

  ngOnInit(): void {
    this.changePasswordForm
      .get('userName')
      ?.patchValue(this.selectedStaff.username);
  }

  generateOtp(): void {
    const username = this.selectedStaff.username;
    if (!username) {
      this.common.toastError('Username is required');
      return;
    }

    this.common.spinnerShow();
    this.staffManagementService
      .generateOtp(username)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.common.spinnerHide();
        }),
        catchError((error) => {
          this.common.spinnerHide();
          this.common.toastError(
            error?.error?.message || error?.message || 'Failed to generate OTP',
          );
          return [];
        }),
      )
      .subscribe({
        next: (response: any) => {
          
          const message = typeof response === 'string' ? response : (response?.message || 'OTP generated successfully');
          this.isOtpGenerated = true;
          this.common.toastSuccess(message);
          this.cdr.markForCheck();
        },
        error: (error) => {
                  }
      });
  }

  validateOtp(): void {
    const otpValue = this.changePasswordForm.get('otp')?.value;
    const username = this.selectedStaff.username;

    if (!otpValue) {
      this.common.toastError('Please enter OTP');
      return;
    }

    if (!username) {
      this.common.toastError('Username is required');
      return;
    }

    const data = {
      username: username,
      otp: otpValue,
    };

    this.common.spinnerShow();
    this.staffManagementService
      .validateOtp(data)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.common.spinnerHide();
        }),
        catchError((error) => {
          this.common.spinnerHide();
          this.common.toastError(
            error?.error?.message || error?.message || 'Invalid OTP. Please try again.',
          );
          return [];
        }),
      )
      .subscribe({
        next: (response: any) => {
          
          const message = typeof response === 'string' ? response : (response?.message || 'OTP validated successfully');
          this.ifgenerateOtpField = false;
          this.common.toastSuccess(message);
          this.cdr.markForCheck();
        },
        error: (error) => {
                  }
      });
  }

  changePassword(): void {
    const newPassword = this.changePasswordForm.get('newPassword')?.value;
    const confirmPassword = this.changePasswordForm.get('confirmPassword')?.value;
    const username = this.selectedStaff.username;

    
    if (!newPassword || !confirmPassword) {
      this.common.toastError('Please fill in all password fields');
      return;
    }

    if (newPassword !== confirmPassword) {
      this.common.toastError('Passwords do not match');
      return;
    }

    
    if (newPassword.length < 8) {
      this.common.toastError('Password must be at least 8 characters long');
      return;
    }

    const data = {
      username: username,
      newPassword: newPassword,
      confirmPassword: confirmPassword,
    };

    this.common.spinnerShow();
    this.staffManagementService
      .resetPassword(data)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.common.spinnerHide();
        }),
        catchError((error) => {
          this.common.spinnerHide();
          this.common.toastError(
            error?.error?.message || error?.message || 'Failed to reset password',
          );
          return [];
        }),
      )
      .subscribe({
        next: (response: any) => {
          
          const message = typeof response === 'string' ? response : (response?.message || 'Password reset successfully');
          this.common.toastSuccess(message);
          this.onClose(true);
        },
        error: (error) => {
                  }
      });
  }

  onClose(isReload: boolean = false): void {
    this.changePasswordForm.reset();
    this.ifgenerateOtpField = true;
    this.isOtpGenerated = false;
    this.selectedStaff = null;
    this.close.emit(isReload);

    const modalElement = document.getElementById('change-password');
    if (modalElement) {
      const modalInstance =
        bootstrap.Modal.getInstance(modalElement) ||
        new bootstrap.Modal(modalElement);
      modalInstance.hide();
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}