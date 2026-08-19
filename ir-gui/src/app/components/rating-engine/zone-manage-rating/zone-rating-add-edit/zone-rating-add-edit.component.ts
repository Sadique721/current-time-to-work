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
import {
  catchError,
  debounceTime,
  distinctUntilChanged,
  finalize,
  Subject,
  takeUntil,
  throwError,
} from "rxjs";
import { CommonService } from "src/app/core.index";
import { ZoneManageService } from "../zone-manage-rating.service";
import { ZoneRating } from "../zone-manage-rating.interface";
import { Router } from "@angular/router";
import { routes } from "src/app/core/helpers/routes";
interface PrefixOption {
  id: number;
  label: string;
  value: string;
  sourceType: string;
  prefixType: string;
  itemType: "PREFIX" | "COUNTRY";
  countryId?: number;
}

@Component({
  selector: "app-zone-rating-add-edit",
  templateUrl: "./zone-rating-add-edit.component.html",
  styleUrl: "./zone-rating-add-edit.component.scss",
  standalone: false,
})
export class ZoneRatingAddEditComponent implements OnInit, OnDestroy {
  zoneForm: UntypedFormGroup;
  private destroy$ = new Subject<void>();
  private prefixSearch$ = new Subject<string>();
  prefixInputMode: "MANUAL" | "DROPDOWN" = "MANUAL";
  prefixSearch = "";
  private initialDropdownLoaded = false;
  prefixTypeFilter = "ALL";
  prefixOptions: PrefixOption[] = [];
  selectedPrefixItems: PrefixOption[] = [];
  prefixOptionsLoading = false;
  manualValidationErrors: string[] = [];
  manualValidationSuccess = "";
  lastClearedItems: PrefixOption[] = [];
  private undoTimeout: any;


  selectedZoneRating: any = null;

  constructor(
    private commonService: CommonService,
    private zoneManageService: ZoneManageService,
    private router: Router
  ) {
    const state = history.state;
    if (state?.id) {
      this.selectedZoneRating = { zoneId: state.id };
    }

    this.zoneForm = new UntypedFormGroup({
      zoneName: new UntypedFormControl("", [
        Validators.required,
        Validators.minLength(3),
        Validators.maxLength(100),
      ]),
      prefixPattern: new UntypedFormControl("", []),
      description: new UntypedFormControl("", [Validators.maxLength(255)]),
      priority: new UntypedFormControl(1, [Validators.required, Validators.min(1)]),
      prefixInputMode: new UntypedFormControl("MANUAL", [Validators.required]),
      selectedPrefixIds: new UntypedFormControl([]),
      selectedCountryIds: new UntypedFormControl([]),
    });
  }

  ngOnInit(): void {
    this.prefixSearch$.pipe(debounceTime(300), distinctUntilChanged(), takeUntil(this.destroy$)).subscribe((search) => {
      this.loadPrefixOptions(search);
    });

    this.zoneForm.get("prefixPattern")?.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(() => {
      if (this.prefixInputMode === "MANUAL") {
        this.validateManualPrefixes();
      }
    });

    if (this.selectedZoneRating?.zoneId) {
      this.commonService.spinnerShow();
      this.zoneManageService.getById(this.selectedZoneRating.zoneId).pipe(
        takeUntil(this.destroy$),
        finalize(() => this.commonService.spinnerHide())
      ).subscribe((res: any) => {
        this.selectedZoneRating = res?.data || res?.content || res;
        this.hydrateForEdit();
      });
    } else {
      this.loadPrefixOptions();
    }
  }

  get filteredPrefixOptions(): PrefixOption[] {
    const search = this.prefixSearch.trim().toLowerCase();

    return this.prefixOptions.filter((option) => {
      const matchesType =
        this.prefixTypeFilter === "ALL" ||
        option.prefixType === this.prefixTypeFilter ||
        option.sourceType === this.prefixTypeFilter;
      const matchesSearch =
        !search ||
        option.label.toLowerCase().includes(search) ||
        option.value.toLowerCase().includes(search);

      return matchesType && matchesSearch;
    });
  }

  private hydrateForEdit(): void {
    const mode = this.selectedZoneRating?.prefixInputMode === "DROPDOWN" ? "DROPDOWN" : "MANUAL";
    const prefixPattern = this.selectedZoneRating?.rawPrefixPattern || this.selectedZoneRating?.prefixPattern || "";

    this.prefixInputMode = mode;
    this.zoneForm.patchValue({
      zoneName: this.selectedZoneRating.zoneName,
      prefixPattern: mode === "MANUAL" ? prefixPattern : "",
      description: this.selectedZoneRating.description,
      priority: this.selectedZoneRating.priority ?? 1,
      prefixInputMode: mode,
      selectedPrefixIds: [],
      selectedCountryIds: [],
    });

    this.selectedPrefixItems = [];
    this.manualValidationErrors = [];
    this.manualValidationSuccess = "";

    if (mode === "MANUAL") {
      this.validateManualPrefixes();
    } else {
      this.loadPrefixOptions();
    }
  }

  onPrefixModeChange(mode: "MANUAL" | "DROPDOWN"): void {
    this.prefixInputMode = mode;
    this.zoneForm.patchValue({
      prefixInputMode: mode,
    });

    if (mode === "DROPDOWN" && this.prefixOptions.length === 0) {
      this.loadPrefixOptions();
    }
  }

  onPrefixSearchInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.prefixSearch = input?.value || "";
    this.prefixSearch$.next(this.prefixSearch);
  }

  onPrefixTypeFilterChange(type: string): void {
    this.prefixTypeFilter = type;
  }

  togglePrefixSelection(option: PrefixOption): void {
    const existing = this.selectedPrefixItems.find((item) => item.id === option.id && item.itemType === option.itemType);

    if (existing) {
      this.selectedPrefixItems = this.selectedPrefixItems.filter(
        (item) => !(item.id === option.id && item.itemType === option.itemType)
      );
    } else {
      this.selectedPrefixItems = [...this.selectedPrefixItems, option];
    }
    
    this.lastClearedItems = []; // clear undo state on manual change
    this.syncSelectedFormValues();
  }

  isOptionSelected(option: PrefixOption): boolean {
    return this.selectedPrefixItems.some(
      (item) => item.id === option.id && item.itemType === option.itemType
    );
  }

  removeSelectedItem(option: PrefixOption): void {
    this.selectedPrefixItems = this.selectedPrefixItems.filter(
      (item) => !(item.id === option.id && item.itemType === option.itemType)
    );
    this.lastClearedItems = []; // clear undo state on manual change
    this.syncSelectedFormValues();
  }

  clearAllPrefixes(): void {
    if (this.selectedPrefixItems.length > 0) {
      this.lastClearedItems = [...this.selectedPrefixItems];
      this.selectedPrefixItems = [];
      this.syncSelectedFormValues();
      
      if (this.undoTimeout) clearTimeout(this.undoTimeout);
      this.undoTimeout = setTimeout(() => {
        this.lastClearedItems = [];
      }, 10000);
    }
  }

  undoClearAll(): void {
    if (this.lastClearedItems.length > 0) {
      this.selectedPrefixItems = [...this.lastClearedItems];
      this.lastClearedItems = [];
      this.syncSelectedFormValues();
      if (this.undoTimeout) clearTimeout(this.undoTimeout);
    }
  }

  validateManualPrefixes(): void {
    const rawValue = (this.zoneForm.get("prefixPattern")?.value || "").toString();
    const prefixes = this.normalizeManualPrefixes(rawValue);

    this.manualValidationErrors = [];
    this.manualValidationSuccess = "";

    if (!prefixes.length) {
      this.manualValidationErrors = ["Please enter at least one prefix"];
      return;
    }

    const seen = new Set<string>();
    const duplicates = new Set<string>();

    prefixes.forEach((prefix) => {
      if (!/^\d+$/.test(prefix)) {
        this.manualValidationErrors.push(`Prefix '${prefix}' contains non-digit characters`);
        return;
      }

      if (prefix.length > 15) {
        this.manualValidationErrors.push(`Prefix '${prefix}' exceeds maximum length of 15 digits`);
        return;
      }

      if (seen.has(prefix)) {
        duplicates.add(prefix);
        return;
      }

      seen.add(prefix);
    });

    duplicates.forEach((prefix) => {
      this.manualValidationErrors.push(`Duplicate prefix found: '${prefix}'`);
    });

    if (prefixes.length > 2000) {
      this.manualValidationErrors.push("Maximum 2000 prefixes allowed");
    }

    if (!this.manualValidationErrors.length) {
      this.manualValidationSuccess = `✓ ${seen.size} valid prefixes`;
    }
  }

  canSubmit(): boolean {
    const baseValid = this.zoneForm.get("zoneName")?.valid && this.zoneForm.get("priority")?.valid;
    if (!baseValid) {
      return false;
    }

    if (this.prefixInputMode === "MANUAL") {
      const normalizedPrefixes = this.normalizeManualPrefixes(this.zoneForm.get("prefixPattern")?.value || "");
      return !this.manualValidationErrors.length && normalizedPrefixes.length > 0;
    }

    return this.selectedPrefixItems.length > 0;
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  submit(): void {
    if (!this.canSubmit()) {
      this.zoneForm.markAllAsTouched();
      if (this.prefixInputMode === "MANUAL") {
        this.validateManualPrefixes();
      }
      return;
    }

    const formValue = this.zoneForm.getRawValue();
    const payload: ZoneRating = {
      zoneName: formValue.zoneName,
      description: formValue.description,
      priority: Number(formValue.priority),
      prefixInputMode: this.prefixInputMode,
    };

    if (this.prefixInputMode === "MANUAL") {
      const normalizedPrefixes = this.normalizeManualPrefixes(formValue.prefixPattern || "");
      payload.prefixPattern = normalizedPrefixes.join(",");
      payload.rawPrefixPattern = normalizedPrefixes.join(",");
    } else {
      payload.selectedPrefixIds = this.selectedPrefixItems
        .filter((item) => item.itemType === "PREFIX")
        .map((item) => item.id);
      payload.selectedCountryIds = this.selectedPrefixItems
        .filter((item) => item.itemType === "COUNTRY")
        .map((item) => item.countryId || item.id);
    }

    const isEdit = !!this.selectedZoneRating?.zoneId;

    this.commonService.spinnerShow();
    const request$ = isEdit
      ? this.zoneManageService.putMethod(this.selectedZoneRating.zoneId, payload)
      : this.zoneManageService.postMethod(payload);

    request$
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.commonService.spinnerHide()),
        catchError((error) => {
          const message = error?.error?.errorMessage || error?.error?.message || "Something went wrong";
          this.commonService.toastError(message);
          return throwError(() => error);
        })
      )
      .subscribe(() => {
        this.commonService.toastSuccess(`Zone ${isEdit ? "updated" : "created"} successfully`);
        this.onClose(true);
      });
  }

  onClose(isReload: boolean = false) {
    this.router.navigate([routes.ratingzone]);
  }

  private normalizeManualPrefixes(value: string): string[] {
    return (value || "")
      .split(",")
      .map((part) => part.trim())
      .filter((part) => part.length > 0);
  }

  private loadPrefixOptions(search: string = ""): void {
    this.prefixOptionsLoading = true;
    this.zoneManageService
      .getPrefixOptions(search)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => (this.prefixOptionsLoading = false)),
        catchError((error) => {
          this.prefixOptions = [];
          return throwError(() => error);
        })
      )
      .subscribe((response: any) => {
        const items = Array.isArray(response) ? response : response?.content || response?.data || [];
        this.prefixOptions = this.normalizePrefixOptions(items);
        
        if (this.prefixInputMode === "DROPDOWN" && this.selectedZoneRating?.zoneId && !this.initialDropdownLoaded) {
          this.initialDropdownLoaded = true;
          const prefixPattern = this.selectedZoneRating?.rawPrefixPattern || this.selectedZoneRating?.prefixPattern || "";
          if (prefixPattern) {
            const values = prefixPattern.split(",").map((v: string) => v.trim()).filter((v: string) => v.length > 0);
            this.selectedPrefixItems = this.prefixOptions.filter(opt => values.includes(opt.value));
          }
        }

        this.syncSelectedFormValues();
      });
  }

  private normalizePrefixOptions(items: any[]): PrefixOption[] {
    return (items || []).map((item: any) => ({
      id: item.id ?? item.prefixId ?? item.countryId ?? 0,
      label:
        item.label ||
        item.name ||
        item.prefixName ||
        item.countryName ||
        item.displayName ||
        item.value ||
        item.prefixValue ||
        "",
      value:
        item.value ||
        item.prefix ||
        item.prefixValue ||
        item.countryCode ||
        item.displayValue ||
        "",
      sourceType: item.sourceType || item.source || "",
      prefixType: item.prefixType || item.type || item.kind || "",
      itemType:
        item.itemType ||
        ((item.sourceType || "").toUpperCase() === "COUNTRY" || item.isCountry
          ? "COUNTRY"
          : "PREFIX"),
      countryId: item.countryId,
    }));
  }

  syncSelectedFormValues(): void {
    this.zoneForm.get("selectedPrefixIds")?.setValue(
      this.selectedPrefixItems.filter((item) => item.itemType === "PREFIX").map((item) => item.id)
    );
    this.zoneForm.get("selectedCountryIds")?.setValue(
      this.selectedPrefixItems.filter((item) => item.itemType === "COUNTRY").map((item) => item.countryId || item.id)
    );
  }
}
