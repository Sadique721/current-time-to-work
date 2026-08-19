import {
  Component,
  EventEmitter,
  Input,
  OnDestroy,
  OnInit,
  Output,
} from "@angular/core";
import {
  UntypedFormControl,
  UntypedFormGroup,
  Validators,
} from "@angular/forms";
import { catchError, finalize, Subject, takeUntil, throwError } from "rxjs";
import { CommonService } from "src/app/core.index";
import { ClearingHouseManageService } from "../clearing-house-manage-rating.service";
declare var bootstrap: any;

@Component({
  selector: "app-clearing-house-add-edit",
  templateUrl: "./clearing-house-add-edit.component.html",
  standalone: false,
})
export class ClearingHouseAddEditComponent implements OnInit, OnDestroy {
  clearingHouseForm: UntypedFormGroup;
  private destroy$ = new Subject<void>();
  @Output() close = new EventEmitter<boolean>();
  @Input() selectedItem: any = null;

  readonly typeOptions = [
    { label: "DCH – Data Clearing House", value: "DCH" },
    { label: "FCH – Financial Clearing House", value: "FCH" },
    { label: "BOTH", value: "BOTH" },
  ];

  readonly statusOptions = [
    { label: "ACTIVE", value: "ACTIVE" },
    { label: "INACTIVE", value: "INACTIVE" },
  ];

  selectedProtocols: string[] = ["SFTP"];
  currencyList: any[] = [];


  constructor(
    private commonService: CommonService,
    private clearingHouseService: ClearingHouseManageService
  ) {
    this.clearingHouseForm = new UntypedFormGroup({
      name: new UntypedFormControl("", [Validators.required]),
      type: new UntypedFormControl("", [Validators.required]),
      status: new UntypedFormControl("ACTIVE", [Validators.required]),
      defaultCurrency: new UntypedFormControl("", [
        Validators.required,
        Validators.minLength(3),
        Validators.maxLength(3),
      ]),
      timezone: new UntypedFormControl("UTC", [Validators.required]),

    });
  }

  ngOnInit(): void {
    this.fetchCurrencyCodes();
    if (this.selectedItem?.id) {
      const {
        name,
        type,
        status,
        defaultCurrency,
        timezone,
        protocols,
        sftpHost,
        sftpPort,
        sftpUsername,
        sftpPassword,
        sftpRemotePath,
        sftpInboxPath,
      } = this.selectedItem;
      this.clearingHouseForm.patchValue({
        name,
        type,
        status,
        defaultCurrency,
        timezone,
      });
      this.selectedProtocols = ["SFTP"];
    }

    this.selectedProtocols = ["SFTP"];
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private fetchCurrencyCodes(): void {
    this.clearingHouseService.getCurrencyCodes().subscribe({
      next: (res: any) => {
        this.currencyList = Array.isArray(res) ? res : [];
      },
      error: (err: any) => console.error("Failed to load currency codes", err),
    });
  }



  submit(): void {
    if (this.clearingHouseForm.invalid) {
      this.clearingHouseForm.markAllAsTouched();
      return;
    }
    if (this.selectedProtocols.length === 0) {
      this.commonService.toastError("Please select at least one protocol");
      return;
    }

    const payload = {
      ...this.clearingHouseForm.getRawValue(),
      protocols: [...this.selectedProtocols],
    };

    const isEdit = !!this.selectedItem?.id;
    const request$ = isEdit
      ? this.clearingHouseService.putMethod(this.selectedItem.id, payload)
      : this.clearingHouseService.postMethod(payload);

    this.commonService.spinnerShow();
    request$
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.commonService.spinnerHide()),
        catchError((error) => {
          const message =
            error?.error?.errorMessage ||
            error?.message ||
            "Something went wrong";
          this.commonService.toastError(message);
          return throwError(() => error);
        })
      )
      .subscribe({
        next: () => {
          this.clearingHouseForm.reset();
          this.selectedProtocols = [];
          this.onClose(true);
          this.commonService.toastSuccess(
            isEdit
              ? "Clearing house updated successfully"
              : "Clearing house created successfully"
          );
        },
        error: () => {},
      });
  }

  onClose(isReload: boolean = false): void {
    this.clearingHouseForm.reset();
    this.selectedProtocols = [];
    this.selectedItem = null;
    this.close.emit(isReload);

    const modalElement = document.getElementById("add-clearing-house");
    if (modalElement) {
      const modalInstance =
        bootstrap.Modal.getInstance(modalElement) ||
        new bootstrap.Modal(modalElement);
      modalInstance.hide();
    }
  }
}
