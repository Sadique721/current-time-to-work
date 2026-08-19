import {
  Component,
  OnDestroy,
  OnInit,
} from "@angular/core";
import {
  UntypedFormControl,
  UntypedFormGroup,
  Validators,
} from "@angular/forms";
import { catchError, finalize, Subject, takeUntil, throwError } from "rxjs";
import { CommonService, routes, SidebarService } from "src/app/core.index";
import { TaxManageService } from "../tax-manage-rating.service";
import { Router } from "@angular/router";

@Component({
  selector: "app-tax-rating-add-edit",
  templateUrl: "./tax-rating-add-edit.component.html",
  standalone: false,
})
export class TaxRatingAddEditComponent implements OnInit, OnDestroy {
  taxRatingForm: UntypedFormGroup;
  private destroy$ = new Subject<void>();
  selectedTaxRating: any = null;
  isCollapsed = false;

  readonly taxTypeOptions = [
    { label: "GST", value: "GST" },
    { label: "VAT", value: "VAT" },
    { label: "Sales Tax", value: "SALES_TAX" },
    { label: "Cess", value: "CESS" },
    { label: "USF", value: "USF" },
  ];

  readonly applyOnOptions = [
    { label: "Base Amount", value: "BASE" },
    { label: "Cumulative (Tax on Tax)", value: "CUMULATIVE" },
  ];

  constructor(
    private commonservice: CommonService,
    private taxManageService: TaxManageService,
    private router: Router,
    private sidebar: SidebarService
  ) {
    this.taxRatingForm = new UntypedFormGroup({
      taxType: new UntypedFormControl("", [Validators.required, Validators.maxLength(20)]),
      taxName: new UntypedFormControl("", [Validators.maxLength(50)]),
      standardRate: new UntypedFormControl(null, [
        Validators.required,
        Validators.min(0.01),
        Validators.max(999.99),
      ]),
      allowsInputCredit: new UntypedFormControl(true),
      isActive: new UntypedFormControl(true),
      effectiveFrom: new UntypedFormControl(null, [Validators.required]),
      effectiveTo: new UntypedFormControl(null),
      applyOn: new UntypedFormControl("BASE"),
    });
  }

  ngOnInit(): void {
    const navState: any = this.router.getCurrentNavigation()?.extras?.state || window.history.state;
    if (navState && navState.taxConfigId) {
      this.selectedTaxRating = navState;
      this.patchForm(this.selectedTaxRating);
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get isEditMode(): boolean {
    return !!(this.selectedTaxRating && this.selectedTaxRating.taxConfigId);
  }

  private patchForm(src: any): void {
    this.taxRatingForm.patchValue({
      taxType: src.taxType || "",
      taxName: src.taxName || "",
      standardRate: src.standardRate || null,
      allowsInputCredit: src.allowsInputCredit ?? true,
      isActive: src.isActive ?? true,
      effectiveFrom: src.effectiveFrom ? new Date(src.effectiveFrom) : "",
      effectiveTo: src.effectiveTo ? new Date(src.effectiveTo) : null,
      applyOn: src.applyOn || "BASE",
    });
  }

  submit(): void {
    if (this.taxRatingForm.invalid) {
      this.taxRatingForm.markAllAsTouched();
      return;
    }

    const rawValue = this.taxRatingForm.getRawValue();
    const payload = {
      ...rawValue,
      effectiveFrom: this.formatDate(rawValue.effectiveFrom),
      effectiveTo: rawValue.effectiveTo ? this.formatDate(rawValue.effectiveTo) : null,
    };
    const isEdit = this.isEditMode;

    this.commonservice.spinnerShow();
    const request$ = isEdit
      ? this.taxManageService.putMethod(this.selectedTaxRating.taxConfigId, payload)
      : this.taxManageService.postMethod(payload);

    request$
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.commonservice.spinnerHide()),
        catchError((error) => {
          this.commonservice.toastError(
            error?.error?.errorMessage || "Something went wrong"
          );
          return throwError(() => error);
        })
      )
      .subscribe({
        next: () => {
          this.commonservice.toastSuccess(
            `Tax configuration ${isEdit ? "updated" : "created"} successfully`
          );
          this.onClose(true);
        },
      });
  }

  private formatDate(date: any): string | null {
    if (!date || !(date instanceof Date)) return null;
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
  }

  toggleCollapse(): void {
    this.sidebar.toggleCollapse();
    this.isCollapsed = !this.isCollapsed;
  }

  onClose(isReload: boolean = false): void {
    this.router.navigate([routes.ratingtax]);
  }
}
