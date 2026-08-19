import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { routes, CommonService } from 'src/app/core.index';
import { User, UserService } from '../user.service';

@Component({
  selector: 'app-user-add-edit',
  templateUrl: './user-add-edit.component.html',
  styleUrl: './user-add-edit.component.scss',
  standalone: false,
})
export class UserAddEditComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  form: FormGroup;
  
  submitted = false;
  isCollapsed = false;
  selectedUser: any = null;
  isLoading = false;
  showPassword = false;

  public routes = routes;

  userRoleOptions = [
    { label: 'Admin', value: 'Admin' },
    { label: 'Agent', value: 'Agent' },
    { label: 'Supervisor', value: 'Supervisor' },
    { label: 'Manager', value: 'Manager' },
  ];

  userGroupOptions = [
    { label: 'auto outbounded', value: 'auto outbounded' },
    { label: 'Default', value: 'Default' },
    { label: 'Sales Team', value: 'Sales Team' },
    { label: 'Support Team', value: 'Support Team' },
  ];

  whatsappChannelOptions = [
    { label: 'Channel 1', value: 'channel1' },
    { label: 'Channel 2', value: 'channel2' },
  ];

  smsChannelOptions = [
    { label: 'SMS Channel 1', value: 'sms1' },
    { label: 'SMS Channel 2', value: 'sms2' },
  ];

  campaignOptions = [
    { label: 'auto lead test', value: 'auto lead test' },
    { label: 'Sale_IC', value: 'Sale_IC' },
    { label: 'Kenya_all_queue_IC', value: 'Kenya_all_queue_IC' },
  ];

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private userService: UserService,
    private commonService: CommonService
  ) {
    this.form = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3)]],
      email: ['', [Validators.required, Validators.email]],
      password: [''],
      defaultTimeout: [30, [Validators.required, Validators.min(10), Validators.max(300)]],
      userRole: ['', Validators.required],
      userGroup: ['', Validators.required],
      whatsappMessagingChannel: [null],
      smsMessagingChannel: [null],
      zohoUserId: [''],
      campaignOptions: [null],
      callRecording: [true, Validators.required],
      status: [true, Validators.required],
    });
  }

  ngOnInit(): void {
    const id = history.state?.id;
    if (id) {
      this.selectedUser = { id: +id };
      this.loadUserFromApi(+id);
    } else {
      this.form.get('password')?.setValidators([
        Validators.required,
        Validators.minLength(6)
      ]);
      this.form.get('password')?.updateValueAndValidity();
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadUserFromApi(id: number): void {
    this.isLoading = true;
    this.commonService.spinnerShow();

    setTimeout(() => {
      const mockUser: User = {
        id: id,
        username: 'bharat',
        email: 'Bharat.singh@unifyxcess.ai',
        defaultTimeout: 30,
        userRole: 'Agent',
        userGroup: 'auto outbounded',
        whatsappMessagingChannel: 'channel1',
        smsMessagingChannel: 'sms1',
        zohoUserId: 'zoho123',
        campaignOptions: 'auto lead test',
        callRecording: true,
        status: true,
      };

      this.selectedUser = mockUser;
      this.patchFormForEdit(mockUser);
      this.isLoading = false;
      this.commonService.spinnerHide();
    }, 500);
  }

  private patchFormForEdit(user: User): void {
    this.form.patchValue({
      username: user.username,
      email: user.email,
      defaultTimeout: user.defaultTimeout,
      userRole: user.userRole,
      userGroup: user.userGroup,
      whatsappMessagingChannel: user.whatsappMessagingChannel,
      smsMessagingChannel: user.smsMessagingChannel,
      zohoUserId: user.zohoUserId,
      campaignOptions: user.campaignOptions,
      callRecording: user.callRecording,
      status: user.status,
    });

    this.form.get('password')?.clearValidators();
    this.form.get('password')?.updateValueAndValidity();
  }

  onSubmit(): void {
    this.submitted = true;

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.commonService.toastError('Please fill all required fields correctly');
      return;
    }

    const payload = { ...this.form.value };
    
    if (!payload.password) {
      delete payload.password;
    }

    this.isLoading = true;
    this.commonService.spinnerShow();

    setTimeout(() => {
      if (this.selectedUser?.id) {
        this.commonService.toastSuccess('User updated successfully');
      } else {
        this.commonService.toastSuccess('User created successfully');
      }

      this.commonService.spinnerHide();
      this.isLoading = false;

      setTimeout(() => this.onCancel(), 500);
    }, 500);
  }

  clearForm(): void {
    if (this.isLoading) return;

    this.form.reset({ 
      username: '',
      email: '',
      password: '',
      defaultTimeout: 30,
      userRole: '',
      userGroup: '',
      whatsappMessagingChannel: null,
      smsMessagingChannel: null,
      zohoUserId: '',
      campaignOptions: null,
      callRecording: true,
      status: true
    });
    
    this.submitted = false;
    this.form.markAsUntouched();
    this.form.markAsPristine();
    
    this.commonService.toastSuccess('Form cleared');
  }

  onCancel(): void {
    if (this.isLoading) return;
    this.router.navigate([this.routes.user], { replaceUrl: true });
  }

  toggleCollapse(): void {
    this.isCollapsed = !this.isCollapsed;
  }

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  incrementTimeout(): void {
    const currentValue = this.form.get('defaultTimeout')?.value || 30;
    const newValue = Math.min(currentValue + 5, 300);
    this.form.patchValue({ defaultTimeout: newValue });
  }

  decrementTimeout(): void {
    const currentValue = this.form.get('defaultTimeout')?.value || 30;
    const newValue = Math.max(currentValue - 5, 10);
    this.form.patchValue({ defaultTimeout: newValue });
  }
}