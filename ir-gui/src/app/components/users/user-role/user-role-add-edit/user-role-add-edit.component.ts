import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { routes, CommonService } from 'src/app/core.index';
import { UserRole, UserRoleService } from '../user-role.service';

@Component({
  selector: 'app-user-role-add-edit',
  templateUrl: './user-role-add-edit.component.html',
  styleUrl: './user-role-add-edit.component.scss',
  standalone: false,
})
export class UserRoleAddEditComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  form: FormGroup;
  
  submitted = false;
  isCollapsed = false;
  selectedUserRole: any = null;
  isLoading = false;

  public routes = routes;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private userRoleService: UserRoleService,
    private commonService: CommonService
  ) {
    this.form = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(3)]],
      status: [true, Validators.required],
      pbxMode: [false],
      cdrReport: [false],
      loginLogoutReport: [false],
      callCenterMode: [false],
      recording: [false],
      followUp: [false],
      stickyAgent: [false],
      numberMasking: [false],
      setting: [false],
      breakcode: [false],
      allowBlackList: [false],
      whatsapp: [false],
    });
  }

  ngOnInit(): void {
    const id = history.state?.id;
    if (id) {
      this.selectedUserRole = { id: +id };
      this.loadUserRoleFromApi(+id);
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadUserRoleFromApi(id: number): void {
    this.isLoading = true;
    this.commonService.spinnerShow();

    setTimeout(() => {
      const mockUserRole: UserRole = {
        id: id,
        name: 'test role for agent',
        status: true,
        pbxMode: false,
        cdrReport: false,
        loginLogoutReport: false,
        callCenterMode: false,
        recording: false,
        followUp: false,
        stickyAgent: false,
        numberMasking: false,
        setting: false,
        breakcode: false,
        allowBlackList: false,
        whatsapp: false,
      };

      this.selectedUserRole = mockUserRole;
      this.patchFormForEdit(mockUserRole);
      this.isLoading = false;
      this.commonService.spinnerHide();
    }, 500);
  }

  private patchFormForEdit(userRole: UserRole): void {
    this.form.patchValue({
      name: userRole.name,
      status: userRole.status,
      pbxMode: userRole.pbxMode,
      cdrReport: userRole.cdrReport,
      loginLogoutReport: userRole.loginLogoutReport,
      callCenterMode: userRole.callCenterMode,
      recording: userRole.recording,
      followUp: userRole.followUp,
      stickyAgent: userRole.stickyAgent,
      numberMasking: userRole.numberMasking,
      setting: userRole.setting,
      breakcode: userRole.breakcode,
      allowBlackList: userRole.allowBlackList,
      whatsapp: userRole.whatsapp,
    });
  }

  onSubmit(): void {
    this.submitted = true;

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.commonService.toastError('Please fill all required fields correctly');
      return;
    }

    const payload = { ...this.form.value };

    this.isLoading = true;
    this.commonService.spinnerShow();

    setTimeout(() => {
      if (this.selectedUserRole?.id) {
                this.commonService.toastSuccess('User Role updated successfully');
      } else {
                this.commonService.toastSuccess('User Role created successfully');
      }

      this.commonService.spinnerHide();
      this.isLoading = false;

      setTimeout(() => this.onCancel(), 500);
    }, 500);
  }

  clearForm(): void {
    if (this.isLoading) return;

    this.form.reset({ 
      name: '',
      status: true,
      pbxMode: false,
      cdrReport: false,
      loginLogoutReport: false,
      callCenterMode: false,
      recording: false,
      followUp: false,
      stickyAgent: false,
      numberMasking: false,
      setting: false,
      breakcode: false,
      allowBlackList: false,
      whatsapp: false,
    });
    
    this.submitted = false;
    this.form.markAsUntouched();
    this.form.markAsPristine();
    
    this.commonService.toastSuccess('Form cleared');
  }

  onCancel(): void {
    if (this.isLoading) return;
    this.router.navigate([this.routes.userrole], { replaceUrl: true });
  }

  toggleCollapse(): void {
    this.isCollapsed = !this.isCollapsed;
  }
}