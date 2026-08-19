import { Component, OnDestroy } from '@angular/core';
import {
  UntypedFormControl,
  UntypedFormGroup,
  Validators,
} from '@angular/forms';
import { Router } from '@angular/router';
import {
  catchError,
  EMPTY,
  finalize,
  of,
  Subscription,
  switchMap,
  tap,
} from 'rxjs';
import { routes } from 'src/app/core/helpers/routes';
import { CommonService } from 'src/app/core/service/common.service';
import { SigninService } from './signin.service';

@Component({
  selector: 'app-signin',
  templateUrl: './signin.component.html',
  styleUrl: './signin.component.scss',
  standalone: false,
})
export class SigninComponent implements OnDestroy {
  loginForm: UntypedFormGroup;
  password: boolean[] = [false];
  subs: Subscription;

  constructor(
    private router: Router,
    private signinService: SigninService,
    private commonService: CommonService
  ) {
    this.loginForm = new UntypedFormGroup({
      username: new UntypedFormControl('', [Validators.required]),
      password: new UntypedFormControl('', [Validators.required]),
    });

    this.subs = new Subscription();
  }

  ngOnDestroy(): void {
    this.subs.unsubscribe();
  }

  public togglePassword(index: number) {
    this.password[index] = !this.password[index];
  }

  checkLogin() {
  if (this.loginForm.valid) {
    this.commonService.spinnerShow();
    this.subs.add(
      this.signinService
        .generateOtp(
          this.loginForm.value.username,
          this.loginForm.value.password
        )
        .subscribe(
          (response: any) => {
            if (response.status === 200) {
              if (response.IsOTPRequired) {
                this.commonService.toastError('OTP is required');
                this.commonService.spinnerHide();
              } else {
                this.login();
              }
            } else {
              this.commonService.spinnerHide();
              const errorMessage = response.message || response.error || 'Failed to generate OTP. Please try again.';
              this.commonService.toastError(errorMessage);
            }
          },
          (error: any) => {
            this.commonService.spinnerHide();
            const errorMessage = error?.error?.ERROR 
              || error?.error?.message 
              || error?.error 
              || error?.message 
              || 'An unexpected error occurred. Please try again.';
            this.commonService.toastError(errorMessage);
          }
        )
      );
  } else {
    this.loginForm.markAllAsTouched();
  }
}

login() {
  if (this.loginForm.valid) {
    this.commonService.spinnerShow();
    this.commonService.clearUserData();

    this.subs.add(
      this.signinService
        .generateToken(this.loginForm.value)
        .pipe(
          finalize(() => this.commonService.spinnerHide()),
          catchError((error: any) => {
            const errorMessage = error?.error?.ERROR 
              || error?.error?.message 
              || error?.error 
              || error?.message 
              || 'Login failed, try again.';
            this.commonService.toastError(errorMessage);
            this.commonService.clearUserData();
            return EMPTY;
          })
        )
        .subscribe((response: any) => {
          if (response.status == 200) {
            const userData = {
              fullName: response.fullName,
              userId: response.userId,
              agentId: response.agentId,
              mvnoId: response.mvnoId || '1',
              userRoles: response.userRoles,
              serviceArea: response.serviceAreaIdList,
              token: response.accessToken,
              mvnoName: response.mvnoName,
              loginUserName: response.userName,
              loginProfile: response.profileImage,
            };

            localStorage.setItem(
              'loggedInUserData',
              JSON.stringify(userData)
            );
            if (response.aclMenus) {
              this.commonService.setRolePermission(response.aclMenus);
            }
            this.router.navigate([routes.index]);
            this.commonService.toastSuccess(
              response.message || 'Login Success'
            );
          } else {
            const errorMessage = response.message || response.error || 'Something went wrong!';
            this.commonService.toastError(errorMessage);
          }
        })
    );
  } else {
    this.loginForm.markAllAsTouched();
  }
}
}
