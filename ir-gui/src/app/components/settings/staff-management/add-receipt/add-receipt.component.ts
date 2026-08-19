import { Component, EventEmitter, Input, Output } from '@angular/core';
import {
  ReactiveFormsModule,
  UntypedFormControl,
  UntypedFormGroup,
  Validators,
} from '@angular/forms';
import { CustomElementModule } from 'src/app/core/shared/custom-elements/custom-elemets.module';
import { sharedModule } from '../../../../core/shared/shared.module';
import { StaffManagementService } from '../staff-management.service';
import { catchError, Subject, takeUntil } from 'rxjs';
import { CommonService } from 'src/app/core/service/common.service';

declare var bootstrap: any;

@Component({
  selector: 'app-add-receipt',
  imports: [ReactiveFormsModule, CustomElementModule, sharedModule],
  templateUrl: './add-receipt.component.html',
  styleUrl: './add-receipt.component.scss',
})
export class AddReceiptComponent {
  @Output() close = new EventEmitter<boolean>();
  @Input() selectedStaff: any = null;
  private destroy$ = new Subject<void>();
  paymentReciptForm: UntypedFormGroup;

  constructor(
    private staffManagementService: StaffManagementService,
    private common: CommonService,
  ) {
    this.paymentReciptForm = new UntypedFormGroup({
      prefix: new UntypedFormControl('', [Validators.required]),
      receiptFrom: new UntypedFormControl('', [Validators.required]),
      receiptTo: new UntypedFormControl('', [Validators.required]),
    });
  }

  addNewReceipt(): void {
    if (this.paymentReciptForm.valid) {
      const staffUserServiceMappingList = {
        fromreceiptnumber: this.paymentReciptForm.get('receiptFrom')?.value,
        id: '',
        identityKey: '',
        isActive: false,
        isDeleted: false,
        mvnoId: '',
        prefix: this.paymentReciptForm.get('prefix')?.value,
        stfmappingId: this.selectedStaff?.id,
        toreceiptnumber: this.paymentReciptForm.get('receiptTo')?.value,
      };
      this.staffManagementService
        .addNewReceipt(staffUserServiceMappingList)
        .pipe(
          takeUntil(this.destroy$),
          catchError((error) => {
            this.common.toastError(error?.error?.msg);
            return error;
          }),
        )
        .subscribe((response: any) => {
          if (response?.responseCode == 417) {
            this.common.toastInfo(response.responseMessage);
          } else if (response?.responseCode == 200) {
            this.common.toastSuccess(response.responseMessage);
            this.onClose(true);
          }
        });
    } else {
      this.paymentReciptForm.markAllAsTouched();
    }
  }

  onClose(isReload: boolean = false): void {
    this.paymentReciptForm.reset();
    this.selectedStaff = false;
    this.close.emit(isReload);

    const modalElement = document.getElementById('add-receipt');
    if (modalElement) {
      const modalInstance =
        bootstrap.Modal.getInstance(modalElement) ||
        new bootstrap.Modal(modalElement);
      modalInstance.hide();
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
