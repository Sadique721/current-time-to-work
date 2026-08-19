import { Component, OnDestroy, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { Subject, takeUntil, finalize } from "rxjs";
import { routes, CommonService } from "src/app/core.index";
import { RatePackageGroupManageService } from "../rate-package-group-manage-rating.service";

@Component({
  selector: "app-rate-package-group-details",
  templateUrl: "./rate-package-group-details.component.html",
  styleUrl: "./rate-package-group-details.component.scss",
  standalone: false,
})
export class RatePackageGroupDetailsComponent implements OnInit, OnDestroy {
  public routes = routes;
  private destroy$ = new Subject<void>();

  selectedRateGroupPackage: any = {};

  // Reorder state
  packageRows: { ratePackageId: number; packageName: string; startTime: string; endTime: string; priority: number; isFallback: boolean; callType: string; expression: string }[] = [];
  fallbackPackageId: number | null = null;
  draggedIndex: number | null = null;
  isSavingOrder = false;
  hasOrderChanged = false;

  get isPriority(): boolean {
    return this.selectedRateGroupPackage?.ratePackageSelectionType === 'PRIORITY';
  }

  get isCallType(): boolean {
    return this.selectedRateGroupPackage?.ratePackageSelectionType === 'CALL_TYPE';
  }

  get isExpression(): boolean {
    return this.selectedRateGroupPackage?.ratePackageSelectionType === 'EXPRESSION';
  }

  constructor(
    private route: ActivatedRoute,
    private commonService: CommonService,
    private ratePackageGroupManageService: RatePackageGroupManageService
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get("id");
    const id = idParam ? Number(idParam) : null;
    if (!id) {
      this.commonService.toastError("Invalid rate package group id");
      return;
    }
    this.fetchDetails(id);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private fetchDetails(id: number): void {
    this.commonService.spinnerShow();
    this.ratePackageGroupManageService.getById(id)
      .pipe(takeUntil(this.destroy$), finalize(() => this.commonService.spinnerHide()))
      .subscribe({
        next: (res: any) => {
          this.selectedRateGroupPackage = res || {};
          this.initPackageRows(res?.ratePackages || []);
        },
        error: (err: any) => {
          this.commonService.toastError(err?.error?.msg || "Failed to load rate package group details");
        },
      });
  }

  private initPackageRows(packages: any[]): void {
    // Sort by existing priority if available
    const sorted = [...packages].sort((a, b) => (a.priority ?? 0) - (b.priority ?? 0));
    this.packageRows = sorted.map((pkg, idx) => ({
      ratePackageId: pkg.ratePackage ?? pkg.ratePackageId,
      packageName: pkg.packageName ?? `Package #${pkg.ratePackage ?? pkg.ratePackageId}`,
      startTime: pkg.startTime ?? '-',
      endTime: pkg.endTime ?? '-',
      priority: pkg.priority ?? (idx + 1),
      isFallback: pkg.isFallback ?? false,
      callType: pkg.callType ?? '-',
      expression: pkg.expression ?? '-',
    }));
    this.fallbackPackageId = this.packageRows.find(r => r.isFallback)?.ratePackageId ?? null;
    this.hasOrderChanged = false;
  }

  // ─── Fallback toggle (0 or 1 fallback allowed) ───────────────────────────────
  toggleFallback(ratePackageId: number): void {
    if (this.fallbackPackageId === ratePackageId) {
      this.fallbackPackageId = null;
      this.packageRows.forEach(r => r.isFallback = false);
    } else {
      this.fallbackPackageId = ratePackageId;
      this.packageRows.forEach(r => r.isFallback = r.ratePackageId === ratePackageId);
    }
    this.hasOrderChanged = true;
  }

  // ─── Move buttons ─────────────────────────────────────────────────────────────
  moveRowUp(index: number, btn?: HTMLElement): void {
    if (index === 0) return;
    [this.packageRows[index - 1], this.packageRows[index]] = [this.packageRows[index], this.packageRows[index - 1]];
    this.recalcPriorities();
    btn?.blur();
  }

  moveRowDown(index: number, btn?: HTMLElement): void {
    if (index === this.packageRows.length - 1) return;
    [this.packageRows[index], this.packageRows[index + 1]] = [this.packageRows[index + 1], this.packageRows[index]];
    this.recalcPriorities();
    btn?.blur();
  }

  // ─── Drag and drop ────────────────────────────────────────────────────────────
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
    this.recalcPriorities();
    this.draggedIndex = null;
  }

  private recalcPriorities(): void {
    this.packageRows.forEach((r, idx) => r.priority = idx + 1);
    this.hasOrderChanged = true;
  }

  // ─── Save re-ordered priorities ───────────────────────────────────────────────
  saveOrder(): void {
    const groupId = this.selectedRateGroupPackage?.ratePackageGroupId;
    if (!groupId) return;

    const packages = this.packageRows.map(r => ({
      ratePackageId: r.ratePackageId,
      priority: r.priority,
      isFallback: r.isFallback,
    }));

    this.isSavingOrder = true;
    this.ratePackageGroupManageService.updatePackagePriorities(groupId, packages)
      .pipe(takeUntil(this.destroy$), finalize(() => this.isSavingOrder = false))
      .subscribe({
        next: () => {
          this.commonService.toastSuccess("Package order updated successfully");
          this.hasOrderChanged = false;
        },
        error: (err: any) => {
          this.commonService.toastError(err?.error?.msg || "Failed to update package order");
        },
      });
  }

  resetOrder(): void {
    this.initPackageRows(this.selectedRateGroupPackage?.ratePackages || []);
  }

  ratePackageGroupView(): void {
    // placeholder - kept for template compatibility
  }
}
