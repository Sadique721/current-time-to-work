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
import { CountryManageService } from "../country-manage.service";
import { WhiteeSpaceValidator } from "src/app/core/shared/custom-validations/white-space.validator";
import { ICountryManage } from "../country-manage.interface";
declare var bootstrap: any;

@Component({
  selector: "app-country-rating-add-edit",
  templateUrl: "./country-rating-add-edit.component.html",
  styleUrl: "./country-rating-add-edit.component.scss",
  standalone: false,
})
export class CountryRatingAddEditComponent implements OnInit, OnDestroy {
  countryRatingForm: UntypedFormGroup;
  private destroy$ = new Subject<void>();
  @Output() close = new EventEmitter<boolean>();
  @Input() selectedCountryRating: any = null;

  constructor(
    private commonservice: CommonService,
    private countryManageService: CountryManageService
  ) {
    this.countryRatingForm = new UntypedFormGroup({
      name: new UntypedFormControl("", [
        Validators.required,
        WhiteeSpaceValidator.cannotContainSpace,
      ]),
      countryCode: new UntypedFormControl("", [Validators.required]),
      currencyCode: new UntypedFormControl("", [Validators.required]),
      currencySymbol: new UntypedFormControl("", [Validators.required]),
      isoCode: new UntypedFormControl("", [
        Validators.required,
        Validators.minLength(2),
        Validators.maxLength(2),
      ]),
    });
  }

  ngOnInit(): void {
    if (this.selectedCountryRating?.countryId) {
      const { name, countryCode, currencyCode, currencySymbol, isoCode } =
        this.selectedCountryRating;
      this.countryRatingForm.patchValue({
        name,
        countryCode,
        currencyCode,
        currencySymbol,
        isoCode,
      });
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  submit(): void {
    if (this.countryRatingForm.valid) {
      const { name, countryCode, currencyCode, currencySymbol, isoCode } =
        this.countryRatingForm.value;
      const countryData: ICountryManage = {
        name,
        countryCode,
        currencyCode,
        currencySymbol,
        isoCode,
      };

      if (this.selectedCountryRating.countryId) {
        const id = this.selectedCountryRating.countryId;
        this.commonservice.spinnerShow();
        this.countryManageService
          .putMethod(id, countryData)
          .pipe(
            takeUntil(this.destroy$),
            finalize(() => {
              this.commonservice.spinnerHide();
            }),
            catchError((error) => {
              this.commonservice.toastError(this.extractErrorMessage(error?.error));
              return throwError(() => error);
            })
          )
          .subscribe((response: any) => {
            if (response?.statusCode == 200) {
              this.countryRatingForm.reset();
              this.onClose(true);
              this.commonservice.toastSuccess(response.statusMsg);
            } else {
              this.commonservice.toastError(response?.errorMessage);
            }
          });
      } else {
        this.commonservice.spinnerShow();
        this.countryManageService
          .postMethod(countryData)
          .pipe(
            takeUntil(this.destroy$),
            finalize(() => {
              this.commonservice.spinnerHide();
            }),
            catchError((error) => {
              this.commonservice.toastError(this.extractErrorMessage(error?.error));
              return throwError(() => error);
            })
          )
          .subscribe((response: any) => {
            if (response?.statusCode == 200) {
              this.countryRatingForm.reset();
              this.onClose(true);
              this.commonservice.toastSuccess(response.statusMsg);
            } else {
              this.commonservice.toastError(response.errorMessage);
            }
          });
      }
    } else {
      this.countryRatingForm.markAllAsTouched();
    }
  }

  private extractErrorMessage(response: any): string {
    if (!response) return 'An unexpected error occurred.';
    // Standard error shape
    if (response.errorMessage || response.message) {
      return response.errorMessage || response.message;
    }
    // Field-keyed validation errors shape: { name: '...', currencyCode: '...' }
    const fieldErrors = Object.values(response).filter(
      (v) => typeof v === 'string'
    );
    if (fieldErrors.length) {
      return fieldErrors.join('\n');
    }
    return 'An unexpected error occurred.';
  }

  onClose(isReload: boolean = false) {
    this.countryRatingForm.reset();
    this.selectedCountryRating = false;
    this.close.emit(isReload);

    const modalElement = document.getElementById("add-country-rating");
    if (modalElement) {
      const modalInstance =
        bootstrap.Modal.getInstance(modalElement) ||
        new bootstrap.Modal(modalElement);
      modalInstance.hide();
    }
  }
}
