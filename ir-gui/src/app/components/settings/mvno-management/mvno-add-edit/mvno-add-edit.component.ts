import { CommonModule } from '@angular/common';
import { Component, HostListener, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import {
  catchError,
  debounceTime,
  EMPTY,
  finalize,
  iif,
  of,
  startWith,
  Subject,
  takeUntil,
  tap,
} from 'rxjs';
import {
  CommonService,
  routes,
  sharedModule,
  status,
} from 'src/app/core.index';
import { CustomElementModule } from 'src/app/core/shared/custom-elements/custom-elemets.module';
import { IMvnoManagement } from '../mvno-management.interface';
import { MvnoManagementService } from '../mvno-management.service';
import { Router } from '@angular/router';
import { RoleManagementService } from '../../role-management/role-management.service';
import { ValidationPattern } from 'src/app/core/models/validation';

@Component({
  selector: 'app-mvno-add-edit',
  imports: [sharedModule, CustomElementModule, CommonModule],
  templateUrl: './mvno-add-edit.component.html',
  styleUrl: './mvno-add-edit.component.scss',
})
export class MvnoAddEditComponent implements OnInit, OnDestroy {
  mvnoFG: UntypedFormGroup;
  private destroy$ = new Subject<void>();
  statusOptions = status;
  data: IMvnoManagement | any = {};
  isLoading: boolean = true;
  twoFactorEnabled = [
    { label: 'true', value: 'true' },
    { label: 'false', value: 'false' },
  ];
  roleList: { id: number; rolename: string }[] = [];
  profileImage: any;
  showPassword: boolean = false;

  twofaType: {
    id: number;
    text: string;
    value: string;
    displayName: string;
  }[] = [];
  days: { label: number }[] = [];

  constructor(
    private common: CommonService,
    private mvnoService: MvnoManagementService,
    private router: Router,
    private fb: FormBuilder,
    private roleMangService: RoleManagementService,
  ) {
    const nav = this.router.getCurrentNavigation();
    this.data = nav ? nav?.extras?.state : null;

    this.mvnoFG = this.fb.group({
      fullName: ['', Validators.required],
      name: ['', Validators.required],
      username: ['', Validators.required],
      password: [
        '',
        [Validators.required, Validators.pattern(ValidationPattern.password)],
      ],
      phone: [null, [Validators.required, Validators.pattern('^[0-9]{10}$')]],
      email: ['', [Validators.required, Validators.email]],
      suffix: [''],
      logfile: [''],
      mvnoHeader: [''],
      mvnoFooter: [''],
      isTwoFactorEnabled: [false],
      authEventName: [''],
      status: ['', Validators.required],
      roleId: ['', Validators.required],
      mvnoPaymentDueDays: [null],
      address: [''],
      ispCommissionPercentage: [null, Validators.required],
      ispBillDay: ['', Validators.required],
      clientId: [''],
      description: ['', Validators.required],
      profileImage: [''],
      logo_file_name: [''],
    });
  }

  ngOnInit(): void {
    if (this.data == null || !this.data?.isAddEdit) {
      this.router.navigateByUrl(routes.mvnoManagement, {
        replaceUrl: true,
      });
    }

    this.getAutType();
    this.getAllRole();
    this.daySequence();

    this.mvnoFG.get('username')?.disable();

    this.mvnoFG
      .get('name')
      ?.valueChanges.pipe(
        takeUntil(this.destroy$),
        tap((res) => {
          if (!this.data?.id && res?.trim()) {
            this.mvnoFG.patchValue({
              username: 'admin@' + res.trim(),
            });
          }
        }),
      )
      .subscribe();

    this.mvnoFG
      .get('isTwoFactorEnabled')
      ?.valueChanges.pipe(
        startWith(this.mvnoFG.get('isTwoFactorEnabled')?.value),
        takeUntil(this.destroy$),
      )
      .subscribe((res) => {
        const control = this.mvnoFG.get('authEventName');
        if (res) {
          control?.enable();
          control?.setValidators([Validators.required]);
          control?.updateValueAndValidity();
        } else {
          control?.setValue('');
          control?.clearValidators();
          control?.updateValueAndValidity();
          control?.disable();
        }
      });

    if (this.data && this.data?.id) {
      this.mvnoFG.get('password')?.disable();

      this.common.spinnerShow();
      this.mvnoService
        .getMvnoById(this.data.id)
        .pipe(
          finalize(() => this.common.spinnerHide()),
          takeUntil(this.destroy$),
          catchError((error) => {
            this.common.toastError(
              error?.error?.ERROR || error?.error?.msg || error?.error?.error,
            );
            return of({});
          }),
        )
        .subscribe((res: any) => {
          if (res.responseCode == 200) {
            res.data?.profileImage
              ? (this.profileImage = `data:image/jpeg;base64,${res.data.profileImage}`)
              : null;

            this.mvnoFG.patchValue({ ...res?.data });
          } else {
            this.common.toastError(res?.responseMessage);
          }
        });
    }
  }

  private getAutType(): void {
    this.mvnoService
      .getAutType()
      .pipe(takeUntil(this.destroy$))
      .subscribe((response: any) => {
        this.twofaType = response?.dataList || [];
      });
  }

  onFileChangeUpload(event: Event): void {
    const files: FileList = (event as any).target.files;

    let fileArray: FileList;
    const formData = new FormData();

    const selectedFile = files.item(0);
    if (!selectedFile) {
      return;
    }
    const allowedTypes = ['image/jpeg', 'image/png'];
    if (!allowedTypes.includes(selectedFile.type)) {
      alert('Only JPEG and PNG files are allowed.');
      return;
    }
    const maxSize = 2097152;
    if (selectedFile.size > maxSize) {
      this.common.toastError('File size cannot exceed 2MB.');
      return;
    }

    fileArray = files;
    formData.append('file', fileArray[0]);

    this.mvnoFG.patchValue({
      logo_file_name: selectedFile.name,
    });

    const reader = new FileReader();
    reader.onload = (e: any) => {
      const base64String = reader.result as string; 
      const base64Data = base64String.split(',')[1];

      this.profileImage = e.target.result;
      this.mvnoFG.patchValue({
        profileImage: base64Data ?? null,
      });
    };
    reader.readAsDataURL(selectedFile);
  }

  removeImage(): void {
    this.mvnoFG.get('logo_file_name')?.setValue(null);
    this.mvnoFG.get('profileImage')?.setValue(null);
    this.profileImage = null;
  }

  private getAllRole(): void {
    this.roleMangService
      .getAllRole()
      .pipe(
        takeUntil(this.destroy$),
        catchError((error) => {
          this.common.toastError(
            error?.error?.ERROR || error?.error?.msg || error?.error?.error,
          );
          return [];
        }),
      )
      .subscribe((response: any) => {
        this.roleList =
          response?.dataList?.filter((role: any) => role.product === 'BSS') ||
          [];
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  @HostListener('window:beforeunload', ['$event'])
  handleBeforeUnload(event: BeforeUnloadEvent) {
    if (this.data && this.data?.isAddEdit) {
      event.preventDefault();
    } else {
      this.onClose();
    }
  }

  canDeactivate(): boolean {
    if (this.data == null || !this.data?.isAddEdit) return true;

    const confirmLeave = window.confirm(
      'Changes will be lost. Do you want to go back?',
    );

    if (!confirmLeave) {
      this.common.spinnerHide();
    }

    return confirmLeave;
  }

  onClose() {
    this.data?.isAddEdit ? (this.data.isAddEdit = false) : null;
    this.router.navigateByUrl(routes.mvnoManagement, {
      replaceUrl: true,
    });
  }

  daySequence(): void {
    for (let i = 0; i < 31; i++) {
      this.days.push({ label: i + 1 });
    }
  }

  submit(): void {
    if (!this.mvnoFG.valid) {
      this.mvnoFG.markAllAsTouched();
      return;
    }

    const mvnoFGValue = this.mvnoFG.getRawValue();

    const payload = { ...mvnoFGValue };

    if (this.data?.id) {
      payload.id = this.data.id;
    } else {
      payload.passwordPolicyId = 1;
    }

    this.common.spinnerShow();
    iif(
      () => this.data.id,
      this.mvnoService.updateMvno(payload),
      this.mvnoService.createMvno(payload),
    )
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.common.spinnerHide();
        }),
        catchError((error) => {
          this.common.toastError(
            error?.error?.ERROR ||
              error?.error?.msg ||
              error?.error?.error ||
              'Something went wrong!',
          );
          return EMPTY;
        }),
      )
      .subscribe((res: any) => {
        if (res.responseCode == 200) {
          this.common.toastSuccess(res?.responseMessage);
          this.onClose();
        } else {
          this.common.toastError(res?.responseMessage);
        }
      });
  }
}
