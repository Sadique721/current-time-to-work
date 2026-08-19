import { Component, OnDestroy, OnInit } from "@angular/core";
import { UntypedFormControl, UntypedFormGroup, Validators, FormControl } from "@angular/forms";
import { Router } from "@angular/router";
import { Subject, takeUntil, throwError } from "rxjs";
import { catchError, finalize } from "rxjs/operators";
import { routes, CommonService, SidebarService } from "src/app/core.index";
import { ProductPlanManageService } from "../product-plan-manage-rating.service";

@Component({
  selector: "app-product-plan-add-edit",
  templateUrl: "./product-plan-add-edit.component.html",
  styleUrl: "./product-plan-add-edit.component.scss",
  standalone: false,
})
export class ProductPlanAddEditComponent implements OnInit, OnDestroy {
  routes = routes;
  destroy$ = new Subject();
  isCollapsed = false;
  selectedProductPlan: any = null;
  currentStep = 1;

  basicForm = new UntypedFormGroup({
    name: new UntypedFormControl("", [
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
  });

  typeOptions = [
    { label: "BUYING", value: "BUYING" },
    { label: "SELLING", value: "SELLING" },
  ];

  availableGroups: { voice: any[], sms: any[], usage: any[] } = { voice: [], sms: [], usage: [] };
  selectedGroups = new Set<any>();
  apiGroupTimes: Map<any, { startTime: Date | null; endTime: Date | null }> = new Map();
  apiGroupMeta: Map<any, { isFallback: boolean; priority: number; serviceType: string | null }> = new Map();
  isLoadingGroups = false;
  groupRows: any[] = [];
  fallbackGroupId: any = null;
  draggedIndex: number | null = null;

  selectedVoiceGroupId: any = null;
  selectedSmsGroupId: any = null;
  selectedUsageGroupId: any = null;



  constructor(
    private router: Router,
    private sidebar: SidebarService,
    private commonService: CommonService,
    private productPlanService: ProductPlanManageService
  ) {}

  ngOnInit() {
    const navState: any =
      this.router.getCurrentNavigation()?.extras?.state || window.history.state;
    if (navState?.id) {
      this.fetchPlanDetails(navState.id);
    } else if (navState?.productPlanId) {
      this.fetchPlanDetails(navState.productPlanId);
    }

    // Clear selections when Package Type changes
    this.basicForm.get('packageType')?.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(() => {
      this.selectedGroups.clear();
      this.groupRows = [];
      this.apiGroupTimes.clear();
      this.apiGroupMeta.clear();
      this.fallbackGroupId = null;
      this.availableGroups = { voice: [], sms: [], usage: [] };
      this.selectedVoiceGroupId = null;
      this.selectedSmsGroupId = null;
      this.selectedUsageGroupId = null;
    });
  }

  ngOnDestroy() {
    this.destroy$.next(null);
    this.destroy$.complete();
  }

  get isEditMode() {
    return !!(this.selectedProductPlan && this.selectedProductPlan.productPlanId);
  }

  fetchPlanDetails(id: any) {
    this.commonService.spinnerShow();
    this.productPlanService
      .getById(id)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.commonService.spinnerHide())
      )
      .subscribe({
        next: (data: any) => {
          this.selectedProductPlan = data;
          this.basicForm.patchValue({
            name: data.name,
            description: data.description,
            packageType: data.packageType,
          }, { emitEvent: false });

          if (data.ratePackageGroups && Array.isArray(data.ratePackageGroups)) {
            // Sort by priority so the Set preserves correct order
            const sorted = [...data.ratePackageGroups].sort((a: any, b: any) => (a.priority ?? 0) - (b.priority ?? 0));
            sorted.forEach((group: any) => {
              const gid = group.ratePackageGroupId || group.ratePackageGroup;
              this.selectedGroups.add(gid);
              this.apiGroupTimes.set(gid, {
                startTime: group.startTime ? new Date(group.startTime) : null,
                endTime: group.endTime ? new Date(group.endTime) : null,
              });
              this.apiGroupMeta.set(gid, {
                isFallback: group.isFallback ?? false,
                priority: group.priority ?? 0,
                serviceType: group.serviceType || null
              });
            });
          }
        },
        error: (err: any) => {
          this.commonService.toastError(
            err?.error?.msg || "Failed to load product plan"
          );
        },
      });
  }

  goToStep2() {
    if (this.basicForm.invalid) {
      this.basicForm.markAllAsTouched();
      return;
    }
    const packageType = this.basicForm.get("packageType")?.value;
    this.currentStep = 2;
    this.fetchGroupsByType(packageType);
  }

  goToStep3() {
    if (this.selectedGroups.size === 0) {
      this.commonService.toastError("Please select at least one group.");
      return;
    }

    this.groupRows = Array.from(this.selectedGroups).map((id, idx) => {
      const allGroups = [...this.availableGroups.voice, ...this.availableGroups.sms, ...this.availableGroups.usage];
      const existing = allGroups.find(
        (g: any) => g.ratePackageGroupId === id
      );

      const existingRow = this.groupRows.find((r) => r.ratePackageGroupId === id);
      const apiMeta = this.apiGroupMeta.get(id) as any;

      return {
        ratePackageGroupId: id,
        groupName: existing?.ratePackageGroupName || existing?.groupName || "Group ID: " + id,
        serviceType: existingRow?.serviceType ?? apiMeta?.serviceType ?? existing?.serviceType ?? ""
      };
    });

    this.currentStep = 3;
  }

  goBack() {
    if (this.currentStep > 1) {
      this.currentStep--;
    }
  }

  fetchGroupsByType(packageType: string) {
    this.isLoadingGroups = true;
    this.availableGroups = { voice: [], sms: [], usage: [] };

    this.productPlanService
      .getRatePackageGroupsNamesByType(packageType)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => (this.isLoadingGroups = false))
      )
      .subscribe({
        next: (res: any) => {
          // Normalize packageId to ratePackageGroupId if needed
          const normalize = (list: any[]) => (list || []).map(g => ({
            ...g,
            ratePackageGroupId: g.ratePackageGroupId || g.packageId,
            ratePackageGroupName: g.ratePackageGroupName || g.packageName
          }));

          this.availableGroups = {
            voice: normalize(res?.voice),
            sms: normalize(res?.sms),
            usage: normalize(res?.usage)
          };

          // Pre-populate selected dropdown IDs from selectedGroups set
          this.availableGroups.voice.forEach(g => {
            if (this.selectedGroups.has(g.ratePackageGroupId)) {
              this.selectedVoiceGroupId = g.ratePackageGroupId;
            }
          });
          this.availableGroups.sms.forEach(g => {
            if (this.selectedGroups.has(g.ratePackageGroupId)) {
              this.selectedSmsGroupId = g.ratePackageGroupId;
            }
          });
          this.availableGroups.usage.forEach(g => {
            if (this.selectedGroups.has(g.ratePackageGroupId)) {
              this.selectedUsageGroupId = g.ratePackageGroupId;
            }
          });
        },
        error: (err: any) => {
          console.error("Error fetching groups", err);
          this.commonService.toastError("Failed to load groups for type");
        },
      });
  }

  onDropdownSelect(section: string, id: any): void {
    const sectionGroups = this.availableGroups[section as keyof typeof this.availableGroups] || [];
    sectionGroups.forEach((g: any) => this.selectedGroups.delete(g.ratePackageGroupId));
    if (id !== null && id !== undefined) {
      this.selectedGroups.add(id);
    }
  }

  toggleGroup(id: any, section: string) {
    const sectionGroups = this.availableGroups[section as keyof typeof this.availableGroups] || [];
    const sectionIds = sectionGroups.map(g => g.ratePackageGroupId);

    if (this.selectedGroups.has(id)) {
      this.selectedGroups.delete(id);
    } else {
      // Deselect any other group in the same section
      sectionIds.forEach(sid => this.selectedGroups.delete(sid));
      this.selectedGroups.add(id);
    }
  }

  isGroupSelected(id: any) {
    return this.selectedGroups.has(id);
  }



  toggleAllGroups(service?: string) {
    let currentFiltered: any[] = [];
    if (service) {
      currentFiltered = this.availableGroups[service as keyof typeof this.availableGroups] || [];
    } else {
      currentFiltered = [...this.availableGroups.voice, ...this.availableGroups.sms, ...this.availableGroups.usage];
    }
    
    const allSelected = currentFiltered.every(g => this.selectedGroups.has(g.ratePackageGroupId));
    
    if (allSelected) {
      currentFiltered.forEach(g => this.selectedGroups.delete(g.ratePackageGroupId));
    } else {
      currentFiltered.forEach(g => this.selectedGroups.add(g.ratePackageGroupId));
    }
  }
removeGroupRow(index: number) {
    const row = this.groupRows[index];
    if (this.fallbackGroupId === row.ratePackageGroupId) this.fallbackGroupId = null;
    this.groupRows.splice(index, 1);
    this.selectedGroups.delete(row.ratePackageGroupId);
    this.groupRows.forEach((r, idx) => r.priority = idx + 1);
    if (this.groupRows.length === 0) this.currentStep = 2;
  }

  toggleFallback(id: any): void {
    if (this.fallbackGroupId === id) {
      this.fallbackGroupId = null;
      this.groupRows.forEach(r => r.isFallback = false);
    } else {
      this.fallbackGroupId = id;
      this.groupRows.forEach(r => r.isFallback = r.ratePackageGroupId === id);
    }
  }

  moveRowUp(index: number, btn?: HTMLElement): void {
    if (index === 0) return;
    [this.groupRows[index - 1], this.groupRows[index]] = [this.groupRows[index], this.groupRows[index - 1]];
    this.groupRows.forEach((r, idx) => r.priority = idx + 1);
    btn?.blur();
  }

  moveRowDown(index: number, btn?: HTMLElement): void {
    if (index === this.groupRows.length - 1) return;
    [this.groupRows[index], this.groupRows[index + 1]] = [this.groupRows[index + 1], this.groupRows[index]];
    this.groupRows.forEach((r, idx) => r.priority = idx + 1);
    btn?.blur();
  }

  onDragStart(index: number): void { this.draggedIndex = index; }
  onDragOver(event: DragEvent): void { event.preventDefault(); }
  onDrop(targetIndex: number): void {
    if (this.draggedIndex === null || this.draggedIndex === targetIndex) return;
    const dragged = this.groupRows.splice(this.draggedIndex, 1)[0];
    this.groupRows.splice(targetIndex, 0, dragged);
    this.groupRows.forEach((r, idx) => r.priority = idx + 1);
    this.draggedIndex = null;
  }

  createProductPlan() {
    if (this.groupRows.length === 0) {
      this.commonService.toastError("No groups configured.");
      return;
    }

    const formValue = this.basicForm.getRawValue();
    const payload = {
      name: formValue.name,
      description: formValue.description,
      packageType: formValue.packageType,
      ratePackageGroups: this.groupRows.map((row) => ({
        ratePackageGroupId: row.ratePackageGroupId,
        serviceType: row.serviceType
      })),
    };

    this.commonService.spinnerShow();
    if (this.isEditMode) {
      this.productPlanService
        .updateMethod(this.selectedProductPlan.productPlanId, payload)
        .pipe(
          takeUntil(this.destroy$),
          finalize(() => this.commonService.spinnerHide()),
          catchError((error) => throwError(() => error))
        )
        .subscribe({
          next: () => {
            this.commonService.toastSuccess("Product Plan updated successfully");
            this.onClose();
          },
          error: (err: any) => {
            this.commonService.toastError(
              err?.error?.msg ||
                err?.error?.errorMessage ||
                "Failed to update product plan"
            );
          },
        });
    } else {
      this.productPlanService
        .postMethod(payload)
        .pipe(
          takeUntil(this.destroy$),
          finalize(() => this.commonService.spinnerHide()),
          catchError((error) => throwError(() => error))
        )
        .subscribe({
          next: () => {
            this.commonService.toastSuccess("Product Plan created successfully");
            this.onClose();
          },
          error: (err: any) => {
            this.commonService.toastError(
              err?.error?.msg ||
                err?.error?.errorMessage ||
                "At least one group is required"
            );
          },
        });
    }
  }

  onClose() {
    this.router.navigateByUrl(this.routes.ratingproductplan, {
      replaceUrl: true,
    });
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

  toggleCollapse() {
    this.sidebar.toggleCollapse();
    this.isCollapsed = !this.isCollapsed;
  }
}
