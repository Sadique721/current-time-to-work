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
import { WhiteeSpaceValidator } from "src/app/core/shared/custom-validations/white-space.validator";
import { PulseManageService } from "../pulse-manage-rating.service";
import { IPulseManage } from "../pulse-manage-rating.interface";
declare var bootstrap: any;

@Component({
  selector: "app-pulse-rating-add-edit",
  templateUrl: "./pulse-rating-add-edit.component.html",
  styleUrl: "./pulse-rating-add-edit.component.scss",
  standalone: false,
})
export class PulseRatingAddEditComponent implements OnInit, OnDestroy {
  pulseRatingForm: UntypedFormGroup;
  private destroy$ = new Subject<void>();
  @Output() close = new EventEmitter<boolean>();
  @Input() selectedPulseRating: any = null;
  serviceTypeOptions = [
    { label: "VOICE", value: "VOICE" },
    { label: "SMS", value: "SMS" },
    { label: "USAGE", value: "USAGE" },
  ];
  unitOptions = [
    { label: "SECOND", value: "SECOND" },
    { label: "MINUTE", value: "MINUTE" },
    { label: "EVENT", value: "EVENT" },
    { label: "KB", value: "KB" },
    { label: "MB", value: "MB" },
    { label: "GB", value: "GB" },
    { label: "BYTE", value: "BYTE" },
  ];

  filteredUnitOptions = this.unitOptions;

  constructor(
    private commonservice: CommonService,
    private pulseManageService: PulseManageService
  ) {
    this.pulseRatingForm = new UntypedFormGroup({
      pulseName: new UntypedFormControl("", [
        Validators.required,
        WhiteeSpaceValidator.cannotContainSpace,
      ]),
      serviceType: new UntypedFormControl("", [Validators.required]),
      unit: new UntypedFormControl("", [Validators.required]),
      noOfUnits: new UntypedFormControl("", [
        Validators.required,
        Validators.min(1),
      ]),
    });
  }

  ngOnInit(): void {
    this.pulseRatingForm.get("serviceType")?.valueChanges.subscribe((value) => {
      this.updateUnitOptions(value);
      this.pulseRatingForm.get("unit")?.setValue("");
    });
    if (this.selectedPulseRating?.pulseId) {
      this.pulseRatingForm.patchValue({
        pulseName: this.selectedPulseRating.pulseName,
        serviceType: this.selectedPulseRating.serviceType,
        unit: this.selectedPulseRating.unit,
        noOfUnits: this.selectedPulseRating.noOfUnits,
      });
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  submit(): void {
    if (this.pulseRatingForm.valid) {
      if (this.selectedPulseRating.pulseId) {
        const id = this.selectedPulseRating.pulseId;
        const formValue = this.pulseRatingForm.value;
        const pulseData: IPulseManage = {
          ...formValue,
          noOfUnits: Number(formValue?.noOfUnits),
        };
        this.commonservice.spinnerShow();
        this.pulseManageService
          .putMethod(id, pulseData)
          .pipe(
            takeUntil(this.destroy$),
            finalize(() => {
              this.commonservice.spinnerHide();
            }),
            catchError((error) => {
              const message =
                error?.error?.errorMessage || error?.message || "Something went wrong";
              this.commonservice.toastError(message);
              return throwError(() => error);
            })
          )
          .subscribe({
            next: (response: any) => {
              this.pulseRatingForm.reset();
              this.onClose(true);
              this.commonservice.toastSuccess("Pulse rating updated successfully");
            },
            error: () => {}
          });
      } else {
        const formValue = this.pulseRatingForm.value;
        const pulseData: IPulseManage = {
          ...formValue,
          noOfUnits: Number(formValue?.noOfUnits),
        };
        this.commonservice.spinnerShow();
        this.pulseManageService
          .postMethod(pulseData)
          .pipe(
            takeUntil(this.destroy$),
            finalize(() => {
              this.commonservice.spinnerHide();
            }),
            catchError((error) => {
              const message =
                error?.error?.errorMessage || error?.message || "Something went wrong";
              this.commonservice.toastError(message);
              return throwError(() => error);
            })
          )
          .subscribe({
            next: (response: any) => {
              this.pulseRatingForm.reset();
              this.onClose(true);
              this.commonservice.toastSuccess("Pulse rating created successfully");
            },
            error: () => {}
          });
      }
    } else {
      this.pulseRatingForm.markAllAsTouched();
    }
  }

  onClose(isReload: boolean = false) {
    this.pulseRatingForm.reset();
    this.selectedPulseRating = false;
    this.close.emit(isReload);

    const modalElement = document.getElementById("add-pulse-rating");
    if (modalElement) {
      const modalInstance =
        bootstrap.Modal.getInstance(modalElement) ||
        new bootstrap.Modal(modalElement);
      modalInstance.hide();
    }
  }

  updateUnitOptions(serviceType: string) {
    if (serviceType === "VOICE") {
      this.filteredUnitOptions = this.unitOptions.filter(
        (opt) => opt.value === "SECOND" || opt.value === "MINUTE"
      );
    } else if (serviceType === "SMS") {
      this.filteredUnitOptions = this.unitOptions.filter(
        (opt) => opt.value === "EVENT"
      );
    } else if (serviceType === "USAGE") {
      this.filteredUnitOptions = this.unitOptions.filter(
        (opt) =>
          opt.value === "KB" ||
          opt.value === "MB" ||
          opt.value === "GB" ||
          opt.value === "BYTE"
      );
    } else {
      this.filteredUnitOptions = this.unitOptions;
    }
  }
}
