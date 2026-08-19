import {
  Component,
  EventEmitter,
  Input,
  OnDestroy,
  OnInit,
  Output,
} from '@angular/core';
import { SystemConfigService } from '../system-config.service';
import { CommonService } from 'src/app/core/service/common.service';
import {
  UntypedFormControl,
  UntypedFormGroup,
  Validators,
} from '@angular/forms';
import { catchError, finalize, Subject, takeUntil } from 'rxjs';
import { WhiteeSpaceValidator } from 'src/app/core/shared/custom-validations/white-space.validator';
import { CustomElementModule } from 'src/app/core/shared/custom-elements/custom-elemets.module';
import { sharedModule } from '../../../../core/shared/shared.module';
import { CommonModule } from '@angular/common';

declare var bootstrap: any;

@Component({
  selector: 'app-system-config-add-edit',
  imports: [CustomElementModule, sharedModule, CommonModule],
  templateUrl: './system-config-add-edit.component.html',
  styleUrl: './system-config-add-edit.component.scss',
})
export class SystemConfigAddEditComponent implements OnInit, OnDestroy {
  @Output() close = new EventEmitter<boolean>();
  @Input() selectedSystemConfig: any = null;
  private destroy$ = new Subject<void>();
  systemConfigForm: UntypedFormGroup;

  constructor(
    private common: CommonService,
    private systemConfigService: SystemConfigService
  ) {
    this.systemConfigForm = new UntypedFormGroup({
      id: new UntypedFormControl(''),
      name: new UntypedFormControl('', [
        Validators.required,
        WhiteeSpaceValidator.cannotContainSpace,
      ]),
      value: new UntypedFormControl('', [Validators.required]),
    });
  }

  ngOnInit(): void {
    if (this.selectedSystemConfig?.id) {
      this.systemConfigForm
        .get('id')
        ?.patchValue(this.selectedSystemConfig?.id);
      this.systemConfigForm
        .get('name')
        ?.patchValue(this.selectedSystemConfig?.name);
      this.systemConfigForm
        .get('value')
        ?.patchValue(this.selectedSystemConfig?.value);
      this.systemConfigForm.get('name')?.disable();
    }
  }

  submit(): void {
    if (this.systemConfigForm.valid) {
      if (this.selectedSystemConfig?.id) {
        const url = '/system/configuration/' + this.selectedSystemConfig.id;
        const creatSystemConfigData = this.systemConfigForm.value;
        this.common.spinnerShow();
        this.systemConfigService
          .updateMethod(url, creatSystemConfigData)
          .pipe(
            takeUntil(this.destroy$),
            finalize(() => {
              this.common.spinnerHide();
            }),
            catchError((error) => {
              this.common.toastError(error?.error?.ERROR);
              return error;
            })
          )
          .subscribe((response: any) => {
            if (response?.status == 200) {
              this.systemConfigForm.reset();
              this.onClose(true);
              this.common.toastSuccess(response.msg);
            } else {
              this.common.toastInfo(response?.msg);
            }
          });
      } else {
        const url = '/system/configuration/';
        const creatSystemConfigData = this.systemConfigForm.value;

        this.common.spinnerShow();
        this.systemConfigService
          .postMethod(url, creatSystemConfigData)
          .pipe(
            takeUntil(this.destroy$),
            finalize(() => {
              this.common.spinnerHide();
            }),
            catchError((error) => {
              this.common.toastError(error?.error?.ERROR);
              return error;
            })
          )
          .subscribe((response: any) => {
            if (response?.status == 200) {
              this.systemConfigForm.reset();
              this.onClose(true);
              this.common.toastSuccess(response.msg);
            } else {
              this.common.toastInfo(response?.msg);
            }
          });
      }
    } else {
      this.systemConfigForm.markAllAsTouched();
    }
  }

  onClose(isReload: boolean = false) {
    this.systemConfigForm.reset();
    this.selectedSystemConfig = false;
    this.close.emit(isReload);

    const modalElement = document.getElementById('add-system-config');
    if (modalElement) {
      const modalInstance =
        bootstrap.Modal.getInstance(modalElement) ||
        new bootstrap.Modal(modalElement);
      modalInstance.hide();
    }
  }

  resetForm(): void {}

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
