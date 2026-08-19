import { Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { Subject, throwError } from 'rxjs';
import { takeUntil, finalize, catchError } from 'rxjs/operators';
import { CommonService } from 'src/app/core.index';
import { TapConfigurationService } from '../tap-configuration.service';

declare var bootstrap: any;

@Component({
  selector: 'app-tap-profile-add-edit',
  templateUrl: './tap-profile-add-edit.component.html',
  standalone: false
})
export class TapProfileAddEditComponent implements OnInit, OnDestroy {
  @Input() selectedProfile: any = null;
  @Output() close = new EventEmitter<boolean>();

  profileForm: UntypedFormGroup;
  private destroy$ = new Subject<void>();
  
  allFields: any[] = [];
  profileMappings: any[] = []; // Stores the selected fields and their overrides

  readonly serviceTypeOptions = [
    { label: 'VOICE', value: 'VOICE' },
    { label: 'SMS', value: 'SMS' },
    { label: 'USAGE', value: 'USAGE' }
  ];

  constructor(
    private commonService: CommonService,
    private tapService: TapConfigurationService
  ) {
    this.profileForm = new UntypedFormGroup({
      profileName: new UntypedFormControl('', [Validators.required]),
      description: new UntypedFormControl(''),
      serviceType: new UntypedFormControl(null, [Validators.required]),
      isActive: new UntypedFormControl(true)
    });
  }

  ngOnInit(): void {
    this.loadFields();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadFields(): void {
    this.commonService.spinnerShow();
    this.tapService.getTapFieldsDropdown()
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.commonService.spinnerHide())
      )
      .subscribe({
        next: (res: any) => {
          this.allFields = res || [];
          if (this.selectedProfile && this.selectedProfile.id) {
            this.patchForm(this.selectedProfile);
          } else {
            this.initializeBlankProfileMappings();
          }
        },
        error: (err: any) => {
          this.commonService.toastError(
            err?.error?.errorMessage || err?.error?.msg || 'Failed to fetch field mappings'
          );
        }
      });
  }

  private initializeBlankProfileMappings(): void {
    // Start with empty profile mappings
    this.profileMappings = [];
  }

  private patchForm(src: any): void {
    this.profileForm.patchValue({
      profileName: src.profileName || '',
      description: src.description || '',
      serviceType: src.serviceType || null,
      isActive: src.active ?? src.isActive ?? true
    });

    // Populate overrides mapping table from response
    if (src.fieldMappings && Array.isArray(src.fieldMappings)) {
      this.profileMappings = src.fieldMappings.map((m: any) => ({
        tapFieldMappingId: m.tapFieldMapping?.id || m.tapFieldMappingId,
        customDefaultValue: m.customDefaultValue || null,
        isMandatoryOverride: m.isMandatoryOverride === null ? 'null' : String(m.isMandatoryOverride),
        // Keep a reference to details for rendering
        fieldName: m.tapFieldMapping?.fieldName,
        asnPath: m.tapFieldMapping?.asnPath,
        callType: m.tapFieldMapping?.callType,
        dataType: m.tapFieldMapping?.dataType
      }));
    }
  }

  addFieldRow(): void {
    // Add a blank row with first unselected field or default null
    const selectedIds = this.profileMappings.map(m => m.tapFieldMappingId);
    const availableField = this.allFields.find(f => !selectedIds.includes(f.id));

    if (!availableField) {
      this.commonService.toastError('All available fields have already been added.');
      return;
    }

    this.profileMappings.push({
      tapFieldMappingId: availableField.id,
      customDefaultValue: null,
      isMandatoryOverride: 'null',
      fieldName: availableField.fieldName,
      asnPath: availableField.asnPath,
      callType: availableField.callType,
      dataType: availableField.dataType
    });
  }

  removeFieldRow(index: number): void {
    this.profileMappings.splice(index, 1);
  }

  onFieldChange(index: number, newFieldId: any): void {
    const selectedField = this.allFields.find(f => f.id === Number(newFieldId));
    if (selectedField) {
      this.profileMappings[index] = {
        ...this.profileMappings[index],
        tapFieldMappingId: selectedField.id,
        fieldName: selectedField.fieldName,
        asnPath: selectedField.asnPath,
        callType: selectedField.callType,
        dataType: selectedField.dataType
      };
    }
  }

  getAvailableFieldsForSelect(currentIndex: number): any[] {
    const currentSelectedId = this.profileMappings[currentIndex]?.tapFieldMappingId;
    const selectedIds = this.profileMappings
      .filter((_, i) => i !== currentIndex)
      .map(m => m.tapFieldMappingId);
    return this.allFields.filter(f => !selectedIds.includes(f.id));
  }

  submit(): void {
    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      return;
    }

    if (this.profileMappings.length === 0) {
      this.commonService.toastError('At least one field mapping override is required in the profile.');
      return;
    }

    const rawForm = this.profileForm.value;
    const fieldMappingsPayload = this.profileMappings.map(m => {
      let isMandOverride: boolean | null = null;
      if (m.isMandatoryOverride === 'true') isMandOverride = true;
      if (m.isMandatoryOverride === 'false') isMandOverride = false;

      return {
        tapFieldMappingId: m.tapFieldMappingId,
        customDefaultValue: m.customDefaultValue || null,
        isMandatoryOverride: isMandOverride
      };
    });

    const payload = {
      ...rawForm,
      active: rawForm.isActive,
      isActive: rawForm.isActive,
      fieldMappings: fieldMappingsPayload
    };

    const isEdit = !!this.selectedProfile?.id;
    this.commonService.spinnerShow();
    const request$ = isEdit
      ? this.tapService.updateTapProfile(this.selectedProfile.id, payload)
      : this.tapService.createTapProfile(payload);

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
            `Profile ${isEdit ? 'updated' : 'created'} successfully`
          );
          this.onClose(true);
        }
      });
  }

  onClose(isReload: boolean = false): void {
    this.profileForm.reset({
      isActive: true,
      serviceType: null
    });
    this.profileMappings = [];
    this.selectedProfile = null;
    this.close.emit(isReload);

    const modalEl = document.getElementById('add-tap-profile');
    if (modalEl) {
      const inst = bootstrap.Modal.getInstance(modalEl) || new bootstrap.Modal(modalEl);
      inst.hide();
    }
  }
}
