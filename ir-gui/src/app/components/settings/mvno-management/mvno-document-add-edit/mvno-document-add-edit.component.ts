import { CommonModule } from '@angular/common';
import { Component, HostListener, OnDestroy, OnInit } from '@angular/core';
import { UntypedFormGroup, FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import {
  Subject,
  takeUntil,
  startWith,
  finalize,
  catchError,
  of,
  iif,
  EMPTY,
  switchMap,
} from 'rxjs';
import {
  CommonService,
  routes,
  sharedModule,
  status,
} from 'src/app/core.index';
import { CustomElementModule } from 'src/app/core/shared/custom-elements/custom-elemets.module';
import { RoleManagementService } from '../../role-management/role-management.service';
import { MvnoManagementService } from '../mvno-management.service';
import customDatetime from 'src/app/core/shared/custom-elements/custom-datetime.pipe';

@Component({
  selector: 'app-mvno-document-add-edit',
  imports: [sharedModule, CustomElementModule, CommonModule],
  templateUrl: './mvno-document-add-edit.component.html',
  styleUrl: './mvno-document-add-edit.component.scss',
})
export class MvnoDocumentAddEditComponent implements OnInit, OnDestroy {
  mvnoDocumnetFG: UntypedFormGroup;
  private destroy$ = new Subject<void>();
  statusOptions = status;
  data: any = {};
  isLoading: boolean = true;
  VerificationModeValue: any = [];
  docTypeList: any[] = [];
  docSubTypeList: any[] = [];
  documentStatusList: any = [];
  mvnoId: number = 0;
  isEditMode: boolean = false;

  isControlRequired = Validators.required;

  constructor(
    private common: CommonService,
    private mvnoService: MvnoManagementService,
    private router: Router,
    private fb: FormBuilder,
    private roleMangService: RoleManagementService,
  ) {
    const nav = this.router.getCurrentNavigation();
    this.data = nav ? nav?.extras?.state : null;
    this.mvnoId = this.data?.mvnoId;

    this.mvnoDocumnetFG = this.fb.group({
      mode: ['', Validators.required],
      docType: ['', Validators.required],
      docSubType: ['', Validators.required],
      filename: ['', [Validators.required]],
      file: [''],
      docStatus: ['pending', [Validators.required]],
      startDate: [null, [Validators.required]],
      endDate: [null, [Validators.required]],
      remark: ['', []],
    });
  }

  ngOnInit(): void {
    if (this.data == null || !this.data?.isAddEdit) {
      this.router.navigateByUrl(routes.mvnoManagement, {
        replaceUrl: true,
      });
    }

    this.isEditMode = !!(this.data && this.data?.docId);
    this.verificationMode();
    this.documentType();
    this.getDocStatusList();
    
    if (this.data && this.data?.docId) {
      this.mvnoDocumnetFG.patchValue({
        mode: this.data.mode,
        docType: this.data.docType,
        docSubType: this.data.docSubType,
        filename: this.data.filename,
        docStatus: this.data.docStatus,
        startDate: new Date(this.data.startDate),
        endDate: new Date(this.data.endDate),
        remark: this.data.remark || '',
      });
      this.mvnoDocumnetFG.get('mode')?.disable();
      this.mvnoDocumnetFG.get('docType')?.disable();
      this.documentSubType();
    }

    this.mvnoDocumnetFG
      .get('docType')
      ?.valueChanges.pipe(
        startWith(this.mvnoDocumnetFG.get('docType')?.value),
        takeUntil(this.destroy$),
      )
      .subscribe((res) => {
        if (!this.data?.docId || res !== this.data.docType) {
          this.documentSubType();
        }
      });

    this.mvnoDocumnetFG
      .get('mode')
      ?.valueChanges.pipe(
        startWith(this.mvnoDocumnetFG.get('mode')?.value),
        takeUntil(this.destroy$),
      )
      .subscribe((res) => {
        if (!this.isEditMode) {
          if (!!res) {
            this.mvnoDocumnetFG.get('docType')?.enable();
          } else {
            this.mvnoDocumnetFG.get('docType')?.disable();
          }
        }
      });

    if (this.data && this.data?.id) {
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
            this.mvnoDocumnetFG.patchValue({ ...res?.data });
          } else {
            this.common.toastError(res?.responseMessage);
          }
        });
    }
  }

  private getDocStatusList(): void {
    this.mvnoService
      .getDocStatusList()
      .pipe(
        takeUntil(this.destroy$),
        catchError((error) => {
          this.common.toastError(
            error?.error?.ERROR || error?.error?.msg || error?.error?.error,
          );
          return of({});
        }),
      )
      .subscribe((res: any) => {
        if (res?.responseCode == 200) {
          this.documentStatusList = res?.dataList || [];
        } else {
          this.common.toastError(res?.responseMessage);
        }
      });
  }

  private documentSubType(): void {
    this.docSubTypeList = [];
    const control = this.mvnoDocumnetFG.get('docSubType');
    const currentValue = control?.value;
    control?.enable();
    control?.setValue('');
    control?.clearValidators();
    control?.updateValueAndValidity();
    control?.disable();

    if (
      this.mvnoDocumnetFG?.get('mode')?.value &&
      this.mvnoDocumnetFG?.get('docType')?.value
    ) {
      this.mvnoService
        .getDocumentSubTypeList(
          this.mvnoDocumnetFG?.get('docType')?.value,
          this.mvnoDocumnetFG?.get('mode')?.value,
        )
        .pipe(
          takeUntil(this.destroy$),
          catchError((error) => {
            this.common.toastError(
              error?.error?.ERROR || error?.error?.msg || error?.error?.error,
            );
            return of({});
          }),
        )
        .subscribe((res: any) => {
          if (res?.responseCode == 200) {
            this.docSubTypeList = res?.dataList || [];
            const control = this.mvnoDocumnetFG.get('docSubType');

            control?.enable();
            control?.setValidators([Validators.required]);
            if (this.isEditMode && currentValue) {
              control?.setValue(currentValue);
            }
            control?.updateValueAndValidity();
          } else {
            this.common.toastError(res?.responseMessage);
          }
        });
    }
  }

  private documentType(): any {
    this.docTypeList = [];

    this.mvnoService
      .getDocumentTypeList()
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {}),
        catchError((error) => {
          this.common.toastError(
            error?.error?.ERROR ||
              error?.error?.msg ||
              error?.error?.error ||
              'Something went wrong!',
          );
          return of({});
        }),
      )
      .subscribe((response: any) => {
        this.docTypeList = response?.dataList || [];
      });
  }

  private verificationMode(): void {
    this.VerificationModeValue = [];

    this.mvnoService
      .getDocumentVerificationModeList()
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {}),
        catchError((error) => {
          this.common.toastError(
            error?.error?.ERROR ||
              error?.error?.msg ||
              error?.error?.error ||
              'Something went wrong!',
          );
          return of({});
        }),
      )
      .subscribe((response: any) => {
        this.VerificationModeValue = response?.dataList || [];
      });
  }

  onFileChangeUpload(event: Event): void {
    const files: FileList = (event as any).target.files;
    this.mvnoDocumnetFG.get('file')?.setValue(null);

    const selectedFile = files.item(0);
    if (!selectedFile) {
      return;
    }
    const allowedTypes = ['image/jpeg', 'image/png'];
    if (!allowedTypes.includes(selectedFile.type)) {
      this.mvnoDocumnetFG.get('filename')?.setValue(null);
      this.common.toastError('Only JPEG and PNG files are allowed.');
      return;
    }
    const maxSize = 2097152;
    if (selectedFile.size > maxSize) {
      this.mvnoDocumnetFG.get('filename')?.setValue(null);
      this.common.toastError('File size cannot exceed 2MB.');
      return;
    }

    this.mvnoDocumnetFG.get('file')?.setValue(selectedFile);
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
    this.router.navigate([routes.mvnoManagement + '/document', this.mvnoId], {
      replaceUrl: true,
    });
  }

  submit(): void {
    if (!this.mvnoDocumnetFG.valid) {
      this.mvnoDocumnetFG.markAllAsTouched();
      return;
    }

    const { file, ...values } = this.mvnoDocumnetFG.getRawValue();

    const filename = values?.filename?.split('\\').pop();
    const startDate = customDatetime(values.startDate?.toISOString(), {
      format: 'YYYY-MM-DD',
    });
    const endDate = customDatetime(values.endDate?.toISOString(), {
      format: 'YYYY-MM-DD',
    });

    const payload = [
      {
        ...values,
        filename,
        endDate,
        startDate,
        mvnoId: this.mvnoId,
      },
    ];

    let formData: any = new FormData();
    formData.append('file', file);
    formData.append('docDetailsList', JSON.stringify(payload));

    this.common.spinnerShow();
    this.mvnoService
      .saveMvnoDocument(this.mvnoId, formData)
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
