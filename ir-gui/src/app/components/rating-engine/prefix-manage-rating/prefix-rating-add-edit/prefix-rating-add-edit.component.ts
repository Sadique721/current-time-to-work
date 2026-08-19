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
import { PrefixManageService } from "../prefix-manage-rating.service";
import { IPrefixManage } from "../prefix-manage-rating.interface";
declare var bootstrap: any;

@Component({
  selector: "app-prefix-rating-add-edit",
  templateUrl: "./prefix-rating-add-edit.component.html",
  styleUrl: "./prefix-rating-add-edit.component.scss",
  standalone: false,
})
export class PrefixRatingAddEditComponent implements OnInit, OnDestroy {
  prefixRatingForm: UntypedFormGroup;
  private destroy$ = new Subject<void>();
  @Output() close = new EventEmitter<boolean>();
  @Input() selectedPrefixRating: any = null;

  countryList: any[] = [];
  prefixTypeOptions = [
    { label: "Interconnect", value: "INTERCONNECT" },
    { label: "Roaming", value: "ROAMING" },
  ];

  /** Tracks the country code for the currently selected country */
  selectedCountryCode: string = "";

  /** Separate control for the suffix portion the user types */
  prefixSuffixControl = new UntypedFormControl("", [Validators.required]);

  constructor(
    private commonservice: CommonService,
    private prefixManageService: PrefixManageService
  ) {
    this.prefixRatingForm = new UntypedFormGroup({
      countryName: new UntypedFormControl("", [
        WhiteeSpaceValidator.cannotContainSpace,
      ]),
      prefixName: new UntypedFormControl("", [Validators.required]),
      prefix: new UntypedFormControl("", [Validators.required]),
      prefixType: new UntypedFormControl("", [Validators.required]),
    });
  }

  ngOnInit(): void {
    this.fetchContries();

    // React to country selection → look up country code and re-sync prefix
    this.prefixRatingForm
      .get('countryName')
      ?.valueChanges.pipe(takeUntil(this.destroy$))
      .subscribe((selectedName: string) => {
        if (selectedName) {
          const country = this.countryList.find((c) => c.name === selectedName);
          this.selectedCountryCode = country?.countryCode ?? '';
        } else {
          this.selectedCountryCode = '';
        }
        this.syncPrefixValue(this.prefixSuffixControl.value);
      });

    // Re-sync prefix whenever the user types more suffix digits
    this.prefixSuffixControl.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe((suffix: string) => {
        this.syncPrefixValue(suffix);
      });

    this.prefixRatingForm
      .get("prefixType")
      ?.valueChanges.pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.updatePrefixTypeBehavior();
      });

    if (this.selectedPrefixRating?.prefixId) {
      this.hydrateForEdit();
    }
  }


  get isInterconnectType(): boolean {
    return this.getCurrentPrefixType() === "INTERCONNECT";
  }

  private getCurrentPrefixType(): string {
    return (this.prefixRatingForm.get("prefixType")?.value || "").toString().toUpperCase();
  }

  /** Combines countryCode + suffix → writes to the hidden `prefix` control */
  private syncPrefixValue(suffix: string): void {
    if (this.isInterconnectType) {
      const combined = (this.selectedCountryCode || "") + (suffix || "");
      this.prefixRatingForm.get("prefix")?.setValue(combined, { emitEvent: false });
    } else {
      this.prefixRatingForm.get("prefix")?.setValue(suffix || "", { emitEvent: false });
    }
  }

  private updatePrefixTypeBehavior(): void {
    const countryControl = this.prefixRatingForm.get("countryName");
    const type = this.getCurrentPrefixType();

    if (type === "INTERCONNECT") {
      countryControl?.setValidators([
        Validators.required,
        WhiteeSpaceValidator.cannotContainSpace,
      ]);
      countryControl?.updateValueAndValidity({ emitEvent: false });
    } else {
      countryControl?.clearValidators();
      countryControl?.setValue("");
      countryControl?.updateValueAndValidity({ emitEvent: false });
      this.selectedCountryCode = "";
    }

    this.syncPrefixValue(this.prefixSuffixControl.value);
  }

  private fetchContries(): void {
    this.prefixManageService.getContries().subscribe({
      next: (res: any) => {
        this.countryList = Array.isArray(res) ? res : [];

        // If in edit mode and countries are now loaded, set the country code
        if (this.selectedPrefixRating?.prefixId && this.countryList.length) {
          this.setCountryCodeFromName(this.selectedPrefixRating.countryName);
          this.extractSuffixForEdit(this.selectedPrefixRating.prefix);
        }
      },
      error: (err: any) => {
        console.error("Failed to load countries", err);
      },
    });
  }

  /** Pre-fill form controls when editing an existing prefix */
  private hydrateForEdit(): void {
    const { countryName, prefixName, prefix, prefixType } = this.selectedPrefixRating;

    this.prefixRatingForm.patchValue({
      countryName,
      prefixName,
      prefix,
      prefixType: prefixType || "",
    });
    this.updatePrefixTypeBehavior();

    // Country code and suffix will be set once countryList loads (see fetchContries)
  }

  /** Finds the country code for the given country name */
  private setCountryCodeFromName(countryName: string): void {
    const country = this.countryList.find((c) => c.name === countryName);
    this.selectedCountryCode = country?.countryCode ?? "";
  }

  /**
   * In edit mode, the stored prefix is the full value (e.g. "919900").
   * We strip the leading country code to get just the suffix for the user input.
   */
  private extractSuffixForEdit(fullPrefix: string): void {
    const prefixStr = String(fullPrefix || "");
    if (this.isInterconnectType && this.selectedCountryCode && prefixStr.startsWith(this.selectedCountryCode)) {
      this.prefixSuffixControl.setValue(
        prefixStr.substring(this.selectedCountryCode.length)
      );
    } else {
      // Fallback – show the full prefix as suffix if country code doesn't match
      this.prefixSuffixControl.setValue(prefixStr);
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  submit(): void {
    // Mark suffix as touched so validation shows
    this.prefixSuffixControl.markAsTouched();

    if (this.prefixRatingForm.valid && this.prefixSuffixControl.valid) {
      // Ensure the combined prefix is up-to-date
      this.syncPrefixValue(this.prefixSuffixControl.value);

      if (this.selectedPrefixRating.prefixId) {
        const id = this.selectedPrefixRating.prefixId;
        const prefixData: IPrefixManage = {
          ...this.prefixRatingForm.value,
          prefix: this.prefixRatingForm.get("prefix")?.value || "",
        };
        this.commonservice.spinnerShow();
        this.prefixManageService
          .putMethod(id, prefixData)
          .pipe(
            takeUntil(this.destroy$),
            finalize(() => {
              this.commonservice.spinnerHide();
            }),
            catchError((error) => {
              const response = error?.error;
              const message = response?.errorMessage || response?.message;
              const details = response?.errorCode ? `${message}` : message;
              this.commonservice.toastError(details);
              return throwError(() => error);
            })
          )
          .subscribe((response: any) => {
            if (response?.statusCode == 200) {
              this.prefixRatingForm.reset();
              this.prefixSuffixControl.reset();
              this.selectedCountryCode = "";
              this.onClose(true);
              this.commonservice.toastSuccess(response.statusMsg);
            } else {
              this.commonservice.toastError(response?.errorMessage);
            }
          });
      } else {
        const prefixData: IPrefixManage = {
          ...this.prefixRatingForm.value,
          prefix: this.prefixRatingForm.get("prefix")?.value || "",
        };
        this.commonservice.spinnerShow();
        this.prefixManageService
          .postMethod(prefixData)
          .pipe(
            takeUntil(this.destroy$),
            finalize(() => {
              this.commonservice.spinnerHide();
            }),
            catchError((error) => {
              const response = error?.error;
              const message = response?.errorMessage || response?.message;
              const details = response?.errorCode ? `${message}` : message;
              this.commonservice.toastError(details);
              return throwError(() => error);
            })
          )
          .subscribe((response: any) => {
            if (response?.statusCode == 200) {
              this.prefixRatingForm.reset();
              this.prefixSuffixControl.reset();
              this.selectedCountryCode = "";
              this.onClose(true);
              this.commonservice.toastSuccess(response.statusMsg);
            } else {
              this.commonservice.toastError(response.errorMessage);
            }
          });
      }
    } else {
      this.prefixRatingForm.markAllAsTouched();
      this.prefixSuffixControl.markAsTouched();
    }
  }

  onClose(isReload: boolean = false) {
    this.prefixRatingForm.reset({
      countryName: "",
      prefixName: "",
      prefix: "",
      prefixType: "",
    });
    this.prefixSuffixControl.reset();
    this.selectedCountryCode = "";
    this.selectedPrefixRating = false;
    this.close.emit(isReload);

    const modalElement = document.getElementById("add-prefix-rating");
    if (modalElement) {
      const modalInstance =
        bootstrap.Modal.getInstance(modalElement) ||
        new bootstrap.Modal(modalElement);
      modalInstance.hide();
    }
  }
}
