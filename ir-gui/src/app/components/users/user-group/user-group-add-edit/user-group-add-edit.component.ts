import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { routes, CommonService } from 'src/app/core.index';
import { UserGroup, UserGroupService } from '../user-group.service';

@Component({
  selector: 'app-user-group-add-edit',
  templateUrl: './user-group-add-edit.component.html',
  styleUrl: './user-group-add-edit.component.scss',
  standalone: false,
})
export class UserGroupAddEditComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  form: FormGroup;

  submitted = false;
  isCollapsed = false;
  selectedUserGroup: UserGroup | null = null;
  isLoading = false;

  public routes = routes;

  outgoingRuleOptions = [
    { label: 'XCESSRULE', value: 'XCESSRULE' },
    { label: 'DEFAULTRULE', value: 'DEFAULTRULE' },
    { label: 'CUSTOMRULE', value: 'CUSTOMRULE' },
  ];

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private userGroupService: UserGroupService,
    private commonService: CommonService
  ) {
    this.form = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(3)]],
      outgoingRule: ['', Validators.required],
      status: [true, Validators.required],
    });
  }

  ngOnInit(): void {
    const id = history.state?.id;
    if (id) {
      this.loadUserGroupFromApi(+id);
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  

  private loadUserGroupFromApi(id: number): void {
    this.isLoading = true;
    this.commonService.spinnerShow();

    this.userGroupService
      .getById(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (userGroup) => {
          this.selectedUserGroup = userGroup;
          this.patchFormForEdit(userGroup);
          this.isLoading = false;
          this.commonService.spinnerHide();
        },
        error: (err) => {
          console.error('Failed to load user group', err);
          this.commonService.toastError('Failed to load user group data');
          this.isLoading = false;
          this.commonService.spinnerHide();
        },
      });
  }

  private patchFormForEdit(userGroup: UserGroup): void {
    this.form.patchValue({
      name: userGroup.name,
      outgoingRule: userGroup.outgoingRule,
      status: userGroup.status,
    });
  }

  

  onSubmit(): void {
    this.submitted = true;

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.commonService.toastError('Please fill all required fields correctly');
      return;
    }

    const formValue = this.form.value;
    const payload: Partial<UserGroup> = {
      name: formValue.name,
      outgoingRule: formValue.outgoingRule,
      status: formValue.status,
    };

    this.isLoading = true;
    this.commonService.spinnerShow();

    if (this.selectedUserGroup?.id) {
      
      this.userGroupService
        .update(this.selectedUserGroup.id, payload)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.commonService.toastSuccess('User Group updated successfully');
            this.finishSubmit();
          },
          error: (err) => {
            console.error('Update failed', err);
            this.commonService.toastError('Failed to update user group');
            this.isLoading = false;
            this.commonService.spinnerHide();
          },
        });
    } else {
      
      this.userGroupService
        .create(payload)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.commonService.toastSuccess('User Group created successfully');
            this.finishSubmit();
          },
          error: (err) => {
            console.error('Create failed', err);
            this.commonService.toastError('Failed to create user group');
            this.isLoading = false;
            this.commonService.spinnerHide();
          },
        });
    }
  }

  onCancel(): void {
    if (this.isLoading) return;
    this.router.navigate([this.routes.usergroup], { replaceUrl: true });
  }

  toggleCollapse(): void {
    this.isCollapsed = !this.isCollapsed;
  }


  private finishSubmit(): void {
    this.commonService.spinnerHide();
    this.isLoading = false;
    setTimeout(() => this.onCancel(), 500);
  }
}