import { Component, OnDestroy, OnInit } from "@angular/core";
import { UntypedFormControl, UntypedFormGroup, Validators } from "@angular/forms";
import { Router } from "@angular/router";
import { catchError, finalize, Subject, takeUntil, throwError } from "rxjs";
import { CommonService, routes, SidebarService } from "src/app/core.index";
import { RatePackageGroupManageService } from "../rate-package-group-manage-rating.service";
import { FormControl } from "@angular/forms";

@Component({
  selector: "app-rate-package-group-add-edit",
  templateUrl: "./rate-package-group-add-edit.component.html",
  styleUrl: "./rate-package-group-add-edit.component.scss",
  standalone: false,
})
export class RatePackageGroupAddEditComponent implements OnInit, OnDestroy {
  public routes = routes;
  private destroy$ = new Subject<void>();

  isCollapsed = false;
  selectedRateGroupPackage: any = null;

  // ─── Wizard state ───────────────────────────────────────────────────────────
  currentStep = 1;

  // ─── Step 1 ─────────────────────────────────────────────────────────────────
  basicForm: UntypedFormGroup;

  typeOptions = [
    { label: "BUYING", value: "BUYING" },
    { label: "SELLING", value: "SELLING" },
  ];



  callTypeOptions = [
    { label: "GPRS", value: "GPRS" },
    { label: "MO_VOICE", value: "MO_VOICE" },
    { label: "MT_VOICE", value: "MT_VOICE" },
    { label: "MO_SMS", value: "MO_SMS" },
    { label: "MT_SMS", value: "MT_SMS" },
  ];

  selectionTypeOptions = [
    { label: "Priority", value: "PRIORITY" },
    { label: "Call Type", value: "CALL_TYPE" },
    { label: "Expression", value: "EXPRESSION" },
  ];

  serviceTypeOptions = [
    { label: "VOICE", value: "VOICE" },
    { label: "SMS", value: "SMS" },
    { label: "USAGE", value: "USAGE" },
  ];

  // ─── Step 2 ─────────────────────────────────────────────────────────────────
  availablePackages: { packageName: string; ratePackageId: number }[] = [];
  selectedPackages: Set<number> = new Set();
  apiPackageTimes: Map<number, { startTime: Date | null; endTime: Date | null }> = new Map();
  apiPackageMeta: Map<number, { isFallback: boolean; priority: number; callType: string | null; expression: string | null; selectionType: string | null }> = new Map();
  isLoadingPackages = false;

  // ─── Shared Start/End Time (Step 2 defaults) ─────────────────────────────
  defaultStartTime = new UntypedFormControl(null);
  defaultEndTime = new UntypedFormControl(null);

  // ─── Change tracking ────────────────────────────────────────────────────────
  lastCoreConfig: string = "";

  // ─── Step 3 ─────────────────────────────────────────────────────────────────
  packageRows: { 
    packageName: string; 
    ratePackageId: number; 
    startTimeControl: UntypedFormControl; 
    endTimeControl: UntypedFormControl;
    callTypeControl: UntypedFormControl;
    expressionControl: UntypedFormControl;
    selectionTypeControl: UntypedFormControl;
    priority: number;
    isFallback: boolean;
  }[] = [];

  fallbackPackageId: number | null = null;  // tracks which package is the fallback
  draggedIndex: number | null = null;

  constructor(
    private router: Router,
    private sidebar: SidebarService,
    private commonservice: CommonService,
    private ratePackageGroupService: RatePackageGroupManageService
  ) {
    this.basicForm = new UntypedFormGroup({
      ratePackageGroupName: new UntypedFormControl("", [
        Validators.required,
        Validators.minLength(6),
        Validators.maxLength(100),
      ]),
      description: new UntypedFormControl("", [
        Validators.required,
        Validators.minLength(6),
        Validators.maxLength(100),
      ]),
      packageType: new UntypedFormControl("", [Validators.required]),
      serviceType: new UntypedFormControl(""),
      selectionType: new UntypedFormControl("PRIORITY", [Validators.required]),
    });
  }

  ngOnInit(): void {
    const navState: any =
      this.router.getCurrentNavigation()?.extras?.state || window.history.state;
    if (navState?.id) {
      this.fetchGroupDetails(navState.id);
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get isEditMode(): boolean {
    return !!(this.selectedRateGroupPackage && this.selectedRateGroupPackage.ratePackageGroupId);
  }

  // ─── Fetch Details (Edit Mode) ──────────────────────────────────────────────────
  private fetchGroupDetails(id: number): void {
    this.commonservice.spinnerShow();
    this.ratePackageGroupService
      .getById(id)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.commonservice.spinnerHide())
      )
      .subscribe({
        next: (data: any) => {
          this.selectedRateGroupPackage = data;
          this.basicForm.patchValue({
            ratePackageGroupName: data.ratePackageGroupName,
            description: data.description,
            packageType: data.packageType,
            serviceType: data.serviceType,
            selectionType: data.ratePackageSelectionType || "PRIORITY",
          });

          if (data.ratePackages && Array.isArray(data.ratePackages)) {
            // Sort by priority so selectedPackages Set preserves correct order
            const sorted = [...data.ratePackages].sort((a: any, b: any) => (a.priority ?? 0) - (b.priority ?? 0));
            sorted.forEach((pkg: any) => {
              this.selectedPackages.add(pkg.ratePackage);
              this.apiPackageTimes.set(pkg.ratePackage, {
                startTime: pkg.startTime ? new Date(pkg.startTime) : null,
                endTime: pkg.endTime ? new Date(pkg.endTime) : null,
              });
              this.apiPackageMeta.set(pkg.ratePackage, {
                isFallback: pkg.isFallback ?? false,
                priority: pkg.priority ?? 0,
                callType: pkg.callType || null,
                expression: pkg.expression || null,
                selectionType: pkg.selectionType || null,
              });
            });
          }
        },
        error: (err) => {
          this.commonservice.toastError(
            err?.error?.msg || "Failed to load rate package group"
          );
        },
      });
  }

  // ─── Navigation ─────────────────────────────────────────────────────────────
  goToStep2(): void {
    if (this.basicForm.invalid) {
      this.basicForm.markAllAsTouched();
      return;
    }

    const packageType = this.basicForm.get("packageType")?.value;
    const serviceType = this.basicForm.get("serviceType")?.value;
    
    const currentConfig = `${packageType}|${serviceType}`;
    
    // If core config changed, reset all selections and cached data
    if (this.lastCoreConfig && this.lastCoreConfig !== currentConfig) {
      this.selectedPackages.clear();
      this.packageRows = [];
      this.apiPackageTimes.clear();
      this.apiPackageMeta.clear();
      this.defaultStartTime.setValue(null);
      this.defaultEndTime.setValue(null);
      this.availablePackages = [];
    }
    
    // Only re-fetch if type or serviceType changed or empty
    if (this.lastCoreConfig !== currentConfig || !this.availablePackages.length) {
      this.fetchPackagesByType(packageType, serviceType);
    }
    
    this.lastCoreConfig = currentConfig;
    this.currentStep = 2;
  }

  goToStep3(): void {
    if (this.selectedPackages.size === 0) {
      this.commonservice.toastError("Please select at least one package.");
      return;
    }

    const sharedStart = this.defaultStartTime.value;
    const sharedEnd = this.defaultEndTime.value;

    this.packageRows = Array.from(this.selectedPackages).map((id, idx) => {
      const existing = this.availablePackages.find((p) => p.ratePackageId === id);
      const existingRow = this.packageRows.find((r) => r.ratePackageId === id);
      const apiTimes = this.apiPackageTimes.get(id);
      
      const startVal = existingRow 
          ? existingRow.startTimeControl.value 
          : (apiTimes?.startTime ?? sharedStart);
          
      const endVal = existingRow 
          ? existingRow.endTimeControl.value 
          : (apiTimes?.endTime ?? sharedEnd);

      if (!existingRow) {
          this.apiPackageTimes.delete(id);
      }

      const apiMeta = this.apiPackageMeta.get(id);

      return {
        ratePackageId: id,
        packageName: existing?.packageName || "Package ID: " + id,
        startTimeControl: new UntypedFormControl(this.formatForInput(startVal)),
        endTimeControl: new UntypedFormControl(this.formatForInput(endVal)),
        callTypeControl: new UntypedFormControl(existingRow?.callTypeControl.value ?? apiMeta?.callType ?? ""),
        expressionControl: new UntypedFormControl(existingRow?.expressionControl.value ?? apiMeta?.expression ?? ""),
        selectionTypeControl: new UntypedFormControl(existingRow?.selectionTypeControl.value ?? apiMeta?.selectionType ?? "PRIORITY"),
        priority: existingRow?.priority ?? apiMeta?.priority ?? (idx + 1),
        isFallback: existingRow?.isFallback ?? apiMeta?.isFallback ?? false,
      };
    });

    // Re-assign sequential priorities based on current order
    this.packageRows.forEach((row, idx) => row.priority = idx + 1);

    // Sync fallbackPackageId so the radio pre-selects correctly
    const fallbackRow = this.packageRows.find(r => r.isFallback);
    this.fallbackPackageId = fallbackRow?.ratePackageId ?? null;

    this.currentStep = 3;
  }

  goBack(): void {
    if (this.currentStep > 1) {
      this.currentStep--;
    }
  }

  // ─── Package fetch (Step 2) ──────────────────────────────────────────────────
  private fetchPackagesByType(packageType: string, serviceType: string): void {
    this.isLoadingPackages = true;
    this.availablePackages = [];

    this.ratePackageGroupService
      .getRatePackagesNamesByType(packageType, "", serviceType)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => (this.isLoadingPackages = false))
      )
      .subscribe({
        next: (res: any) => {
          this.availablePackages = Array.isArray(res) ? res : [];
          // In edit mode, pre-select packages that came from the API response
          if (this.isEditMode && this.selectedPackages.size === 0) {
            this.apiPackageMeta.forEach((_, id) => this.selectedPackages.add(id));
          }
        },
        error: (err: any) => {
          console.error("Error fetching packages", err);
          this.commonservice.toastError("Failed to load packages for type");
        },
      });
  }

  // ─── Step 2 helpers ──────────────────────────────────────────────────────────
  togglePackage(id: number): void {
    if (this.selectedPackages.has(id)) {
      this.selectedPackages.delete(id);
    } else {
      this.selectedPackages.add(id);
    }
  }

  isPackageSelected(id: number): boolean {
    return this.selectedPackages.has(id);
  }

  // ─── Step 3 helpers ──────────────────────────────────────────────────────────
  removePackageRow(index: number): void {
    const row = this.packageRows[index];
    if (this.fallbackPackageId === row.ratePackageId) {
      this.fallbackPackageId = null;
    }
    this.packageRows.splice(index, 1);
    this.selectedPackages.delete(row.ratePackageId);
    // Re-assign priorities
    this.packageRows.forEach((r, idx) => r.priority = idx + 1);
    if (this.packageRows.length === 0) {
      this.currentStep = 2;
    }
  }

  toggleFallback(ratePackageId: number): void {
    if (this.fallbackPackageId === ratePackageId) {
      // Deselect — clicking the same one again clears it
      this.fallbackPackageId = null;
      this.packageRows.forEach(r => r.isFallback = false);
    } else {
      this.fallbackPackageId = ratePackageId;
      this.packageRows.forEach(r => r.isFallback = r.ratePackageId === ratePackageId);
    }
  }

  moveRowUp(index: number, btn?: HTMLElement): void {
    if (index === 0) return;
    [this.packageRows[index - 1], this.packageRows[index]] =
      [this.packageRows[index], this.packageRows[index - 1]];
    this.packageRows.forEach((r, idx) => r.priority = idx + 1);
    btn?.blur();
  }

  moveRowDown(index: number, btn?: HTMLElement): void {
    if (index === this.packageRows.length - 1) return;
    [this.packageRows[index], this.packageRows[index + 1]] =
      [this.packageRows[index + 1], this.packageRows[index]];
    this.packageRows.forEach((r, idx) => r.priority = idx + 1);
    btn?.blur();
  }

  onDragStart(index: number): void {
    this.draggedIndex = index;
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
  }

  onDrop(targetIndex: number): void {
    if (this.draggedIndex === null || this.draggedIndex === targetIndex) return;
    const dragged = this.packageRows.splice(this.draggedIndex, 1)[0];
    this.packageRows.splice(targetIndex, 0, dragged);
    this.packageRows.forEach((r, idx) => r.priority = idx + 1);
    this.draggedIndex = null;
  }

  // ─── Date formatting helpers ───────────────────────────────────────────────
  formatDateTimeForAPI(datetime: Date | string | null): string | null {
    if (!datetime) return null;
    const date = datetime instanceof Date ? datetime : new Date(datetime);
    const yyyy = date.getFullYear();
    const MM = this.padZero(date.getMonth() + 1);
    const dd = this.padZero(date.getDate());
    const hh = this.padZero(date.getHours());
    const mm = this.padZero(date.getMinutes());
    const ss = this.padZero(date.getSeconds());
    return `${yyyy}-${MM}-${dd} ${hh}:${mm}:${ss}`;
  }

  private padZero(n: number): string {
    return n < 10 ? "0" + n : n.toString();
  }

  private formatForInput(date: any): string {
    if (!date) return "";
    const d = new Date(date);
    if (isNaN(d.getTime())) return "";
    const yyyy = d.getFullYear();
    const MM = this.padZero(d.getMonth() + 1);
    const dd = this.padZero(d.getDate());
    const hh = this.padZero(d.getHours());
    const mm = this.padZero(d.getMinutes());
    return `${yyyy}-${MM}-${dd}T${hh}:${mm}`;
  }

  // ─── Submission ──────────────────────────────────────────────────────────────
  createRatePackageGroup(): void {
    if (this.packageRows.length === 0) {
      this.commonservice.toastError("No packages configured.");
      return;
    }

    const formValue = this.basicForm.getRawValue();
    const payload = {
      ratePackageGroupName: formValue.ratePackageGroupName,
      description: formValue.description,
      packageType: formValue.packageType,
      serviceType: formValue.serviceType || null,
      lineOfBusiness: null,
      ratePackageSelectionType: formValue.selectionType || "PRIORITY",
      ratePackages: this.packageRows.map((row) => ({
        ratePackage: row.ratePackageId,
        startTime: this.formatDateTimeForAPI(row.startTimeControl.value),
        endTime: this.formatDateTimeForAPI(row.endTimeControl.value),
        callType: formValue.selectionType === 'CALL_TYPE' ? (row.callTypeControl.value || null) : null,
        expression: formValue.selectionType === 'EXPRESSION' ? (row.expressionControl.value || null) : null,
        selectionType: formValue.selectionType || "PRIORITY",
        priority: formValue.selectionType === 'PRIORITY' ? row.priority : null,
      })),
    };

    if (this.isEditMode) {
      this.commonservice.spinnerShow();
      this.ratePackageGroupService
        .updateRatePackages(
          `/rate-package-groups/${this.selectedRateGroupPackage.ratePackageGroupId}`,
          payload
        )
        .pipe(
          takeUntil(this.destroy$),
          finalize(() => this.commonservice.spinnerHide()),
          catchError((error) => throwError(() => error))
        )
        .subscribe({
          next: () => {
            this.commonservice.toastSuccess("Rate Package Group updated successfully");
            this.onClose(true);
          },
          error: (err) => {
            this.commonservice.toastError(
              err?.error?.errorMessage || "Failed to update rate package group"
            );
          },
        });
    } else {
      this.commonservice.spinnerShow();
      this.ratePackageGroupService
        .addRatePackagesGroup(`/rate-package-groups`, payload)
        .pipe(
          takeUntil(this.destroy$),
          finalize(() => this.commonservice.spinnerHide()),
          catchError((error) => {
            this.commonservice.toastError(
              error?.error?.errorMessage || "At least one rate package is required"
            );
            throw error;
          })
        )
        .subscribe({
          next: () => {
            this.commonservice.toastSuccess("Rate Package Group created successfully");
            this.onClose(true);
          },
          error: () => {},
        });
    }
  }

  onClose(success?: boolean): void {
    this.router.navigateByUrl(routes.ratingratepackagegroup, {
      replaceUrl: true,
    });
  }

  toggleCollapse(): void {
    this.sidebar.toggleCollapse();
    this.isCollapsed = !this.isCollapsed;
  }
}
