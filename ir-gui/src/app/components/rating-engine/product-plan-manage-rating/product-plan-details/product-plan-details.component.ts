import { Component, OnDestroy, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { Subject, takeUntil, finalize } from "rxjs";
import { routes, CommonService } from "src/app/core.index";
import { ProductPlanManageService } from "../product-plan-manage-rating.service";

@Component({
  selector: "app-product-plan-details",
  templateUrl: "./product-plan-details.component.html",
  styleUrl: "./product-plan-details.component.scss",
  standalone: false,
})
export class ProductPlanDetailsComponent implements OnInit, OnDestroy {
  public routes = routes;
  private destroy$ = new Subject<void>();

  selectedProductPlan: any = {};

  // Reorder state
  groupRows: { ratePackageGroupId: number; groupName: string; serviceType: string }[] = [];
  isSavingOrder = false;
  hasOrderChanged = false;

  constructor(
    private route: ActivatedRoute,
    private commonService: CommonService,
    private productPlanService: ProductPlanManageService
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get("id");
    const id = idParam ? Number(idParam) : null;
    if (!id) {
      this.commonService.toastError("Invalid product plan id");
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
    this.productPlanService.getById(id)
      .pipe(takeUntil(this.destroy$), finalize(() => this.commonService.spinnerHide()))
      .subscribe({
        next: (res: any) => {
          this.selectedProductPlan = res || {};
          this.initGroupRows(res?.ratePackageGroups || []);
        },
        error: (err: any) => {
          this.commonService.toastError(err?.error?.msg || "Failed to load product plan details");
        },
      });
  }

  private initGroupRows(groups: any[]): void {
    this.groupRows = groups.map((g) => ({
      ratePackageGroupId: g.ratePackageGroupId,
      groupName: g.ratePackageGroupName ?? `Group #${g.ratePackageGroupId}`,
      serviceType: g.serviceType ?? "-",
    }));
    this.hasOrderChanged = false;
  }


}
