import { Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil, finalize, catchError } from 'rxjs/operators';
import { throwError } from 'rxjs';
import { CommonService } from 'src/app/core.index';
import { TapConfigurationService } from '../tap-configuration.service';

declare var bootstrap: any;

@Component({
  selector: 'app-tap-field-add-edit',
  templateUrl: './tap-field-add-edit.component.html',
  standalone: false
})
export class TapFieldAddEditComponent implements OnInit, OnDestroy {
  @Input() selectedField: any = null;
  @Output() close = new EventEmitter<boolean>();
  
  fieldForm: UntypedFormGroup;
  private destroy$ = new Subject<void>();

  readonly callTypeOptions = [
    { label: 'Global (All Call Types)', value: null },
    { label: 'GPRS (General Packet Radio Service)', value: 'GPRS' },
    { label: 'MO_VOICE', value: 'MO_VOICE' },
    { label: 'MT_VOICE', value: 'MT_VOICE' },
    { label: 'MO_SMS', value: 'MO_SMS' },
    { label: 'MT_SMS', value: 'MT_SMS' }
  ];

  readonly dataTypeOptions = [
    { label: 'BCD String (Binary Coded Decimal)', value: 'BCD_STRING' },
    { label: 'ASCII String', value: 'ASCII_STRING' },
    { label: 'Integer', value: 'INTEGER' },
    { label: 'Decimal', value: 'DECIMAL' },
    { label: 'Date Time', value: 'DATE_TIME' }
  ];

  constructor(
    private commonService: CommonService,
    private tapService: TapConfigurationService
  ) {
    this.fieldForm = new UntypedFormGroup({
      callType: new UntypedFormControl(null),
      fieldName: new UntypedFormControl('', [Validators.required]),
      asnPath: new UntypedFormControl('', [Validators.required]),
      dataType: new UntypedFormControl('BCD_STRING', [Validators.required]),
      outSourceColumn: new UntypedFormControl(''),
      inTargetColumn: new UntypedFormControl(''),
      defaultValue: new UntypedFormControl(null),
      isMandatory: new UntypedFormControl(false)
    });
  }

  ngOnInit(): void {
    if (this.selectedField && this.selectedField.id) {
      this.patchForm(this.selectedField);
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private patchForm(src: any): void {
    this.fieldForm.patchValue({
      callType: src.callType || null,
      fieldName: src.fieldName || '',
      asnPath: src.asnPath || '',
      dataType: src.dataType || 'BCD_STRING',
      outSourceColumn: src.outSourceColumn || '',
      inTargetColumn: src.inTargetColumn || '',
      defaultValue: src.defaultValue || null,
      isMandatory: src.mandatory ?? src.isMandatory ?? false
    });
  }

  submit(): void {
    if (this.fieldForm.invalid) {
      this.fieldForm.markAllAsTouched();
      return;
    }

    const formVal = this.fieldForm.value;
    const payload = {
      ...formVal,
      mandatory: formVal.isMandatory,
      isMandatory: formVal.isMandatory
    };
    const isEdit = !!this.selectedField?.id;

    this.commonService.spinnerShow();
    const request$ = isEdit
      ? this.tapService.updateTapField(this.selectedField.id, payload)
      : this.tapService.createTapField(payload);

    request$
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.commonService.spinnerHide()),
        catchError((error) => {
          this.commonService.toastError(
            error?.error?.errorMessage || error?.error?.msg || 'Something went wrong'
          );
          return throwError(() => error);
        })
      )
      .subscribe({
        next: () => {
          this.commonService.toastSuccess(
            `Field mapping ${isEdit ? 'updated' : 'created'} successfully`
          );
          this.onClose(true);
        }
      });
  }

  onClose(isReload: boolean = false): void {
    this.fieldForm.reset({
      dataType: 'BCD_STRING',
      isMandatory: false
    });
    this.selectedField = null;
    this.close.emit(isReload);

    const modalEl = document.getElementById('add-tap-field');
    if (modalEl) {
      const inst = bootstrap.Modal.getInstance(modalEl) || new bootstrap.Modal(modalEl);
      inst.hide();
    }
  }
}
