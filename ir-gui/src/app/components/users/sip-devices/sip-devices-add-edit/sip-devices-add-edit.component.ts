import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { routes, CommonService } from 'src/app/core.index';
import { SipDevice, SipDevicesService } from '../sip-devices.service';

@Component({
  selector: 'app-sip-devices-add-edit',
  templateUrl: './sip-devices-add-edit.component.html',
  styleUrl: './sip-devices-add-edit.component.scss',
  standalone: false,
})
export class SipDevicesAddEditComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  form: FormGroup;
  
  submitted = false;
  isCollapsed = false;
  selectedDevice: any = null;
  isLoading = false;
  passwordVisible: boolean = false;

  public routes = routes;

  userOptions: Array<{ label: string; value: number }> = [];
  callerIdNumberOptions: Array<{ label: string; value: string }> = [];

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private sipDevicesService: SipDevicesService,
    private commonService: CommonService
  ) {
    this.form = this.fb.group({
      deviceName: ['', [Validators.required]],
      username: ['', Validators.required],
      password: ['', Validators.required],
      callerIdName: [''],
      callerIdNumber: [''],
      userId: ['', Validators.required],
      status: ['1', Validators.required],
      extensionStatus: [1],
      recording: [0],
      dnd: [0],
      isAssigned: [0],
      mailTo: ['']
    });
  }

  ngOnInit(): void {
    this.loadUsers();
    this.loadCallerIdNumbers();
    const id = history.state?.id;
    if (id) {
      this.selectedDevice = { id: +id };
      this.loadDeviceFromApi(+id);
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadUsers(): void {
    this.sipDevicesService.getUsers()
      .pipe(takeUntil(this.destroy$))
      .subscribe((users) => {
        this.userOptions = users;
      });
  }

  private loadCallerIdNumbers(): void {
    this.sipDevicesService.getCallerIdNumbers()
      .pipe(takeUntil(this.destroy$))
      .subscribe((numbers) => {
        this.callerIdNumberOptions = numbers;
      });
  }

  private loadDeviceFromApi(id: number): void {
    this.isLoading = true;
    this.commonService.spinnerShow();

    this.sipDevicesService.getById(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (device) => {
          this.selectedDevice = device;
          this.patchFormForEdit(device);
          this.isLoading = false;
          this.commonService.spinnerHide();
        },
        error: (error) => {
                    this.isLoading = false;
          this.commonService.spinnerHide();
          this.commonService.toastError('Failed to load device data');
          this.onCancel();
        }
      });
  }

  private patchFormForEdit(device: SipDevice): void {
    this.form.patchValue({
      deviceName: device.deviceName,
      username: device.username,
      password: device.password,
      callerIdName: device.callerIdName,
      callerIdNumber: device.callerIdNumber,
      userId: device.userId,
      status: device.status,
      extensionStatus: device.extensionStatus,
      recording: device.recording,
      dnd: device.dnd,
      isAssigned: device.isAssigned,
      mailTo: device.mailTo
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

    const operation = this.selectedDevice?.id
      ? this.sipDevicesService.update(this.selectedDevice.id, payload)
      : this.sipDevicesService.create(payload);

    operation
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          if (this.selectedDevice?.id) {
                        this.commonService.toastSuccess('Device updated successfully');
          } else {
                        this.commonService.toastSuccess('Device created successfully');
          }

          this.commonService.spinnerHide();
          this.isLoading = false;

          setTimeout(() => this.onCancel(), 500);
        },
        error: (error) => {
                    this.commonService.spinnerHide();
          this.isLoading = false;
          this.commonService.toastError('Failed to save device');
        }
      });
  }

  clearForm(): void {
    if (this.isLoading) return;

    this.form.reset({ 
      deviceName: '', 
      username: '',
      password: '',
      callerIdName: '',
      callerIdNumber: '',
      userId: '',
      status: '1',
      extensionStatus: 1,
      recording: 0,
      dnd: 0,
      isAssigned: 0,
      mailTo: ''
    });
    
    this.submitted = false;
    this.form.markAsUntouched();
    this.form.markAsPristine();
    
    this.commonService.toastSuccess('Form cleared');
  }

  onCancel(): void {
    if (this.isLoading) return;
    this.router.navigate([this.routes.sipdevices], { replaceUrl: true });
  }

  toggleCollapse(): void {
    this.isCollapsed = !this.isCollapsed;
  }

  togglePasswordVisibility(): void {
    this.passwordVisible = !this.passwordVisible;
  }

  generatePassword(): void {
    const password = this.sipDevicesService.generatePassword();
    this.form.patchValue({ password });
    this.commonService.toastSuccess('Password generated');
  }

  openOtherFeatures(): void {
    if (!this.selectedDevice?.id) return;
    
    this.router.navigate([this.routes.sipdevices + '/other-features'], {
      state: { id: this.selectedDevice.id },
    });
  }

    onRecordingChange(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.form.patchValue({ recording: target.checked ? 1 : 0 });
  }

    onDndChange(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.form.patchValue({ dnd: target.checked ? 1 : 0 });
  }

    onIsAssignedChange(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.form.patchValue({ isAssigned: target.checked ? 1 : 0 });
  }
}