import { Component, OnInit, OnDestroy } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { CommonService, routes } from "src/app/core.index";
import { ZoneManageService } from "../zone-manage-rating.service";
import { Subject, finalize, takeUntil } from "rxjs";

@Component({
  selector: "app-zone-rating-details",
  templateUrl: "./zone-rating-details.component.html",
  styleUrl: "./zone-rating-details.component.scss",
  standalone: false,
})
export class ZoneRatingDetailsComponent implements OnInit, OnDestroy {
  public routes = routes;
  selectedZoneRating: any = {};
  prefixDetails: any[] = [];
  loadingPrefixes = false;
  private destroy$ = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    private commonService: CommonService,
    private zoneManageService: ZoneManageService
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get("id");
    const id = idParam ? Number(idParam) : null;
    if (!id) {
      this.commonService.toastError("Invalid zone id");
      return;
    }
    this.fetchDetails(id);
  }

  private fetchDetails(id: number): void {
    this.commonService.spinnerShow();
    this.zoneManageService.getById(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res: any) => {
          this.commonService.spinnerHide();
          this.selectedZoneRating = res?.data || res?.content || res || {};
          const pattern = this.selectedZoneRating?.rawPrefixPattern || this.selectedZoneRating?.prefixPattern || "";
          if (pattern) {
            this.fetchPrefixDetails(pattern);
          }
        },
        error: (err: any) => {
          this.commonService.spinnerHide();
          this.commonService.toastError(
            err?.error?.msg || "Failed to load zone details"
          );
        },
    });
  }

  private fetchPrefixDetails(prefixPattern: string): void {
    this.loadingPrefixes = true;
    this.zoneManageService.getPrefixOptions("")
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.loadingPrefixes = false)
      )
      .subscribe({
        next: (response: any) => {
          const items = Array.isArray(response) ? response : response?.content || response?.data || [];
          const values = prefixPattern.split(",").map((v: string) => v.trim()).filter((v: string) => v.length > 0);
          
          const matchedItems = items.filter((opt: any) => {
            const val = opt.value || opt.prefix || opt.prefixValue || opt.countryCode || opt.displayValue || "";
            return values.includes(val);
          });
          
          this.prefixDetails = matchedItems.map((item: any) => ({
            label: item.label || item.name || item.prefixName || item.countryName || item.displayName || item.value || item.prefixValue || "",
            value: item.value || item.prefix || item.prefixValue || item.countryCode || item.displayValue || "",
            prefixType: item.prefixType || item.type || item.kind || "",
            sourceType: item.sourceType || item.source || ""
          }));
        },
        error: (err: any) => {
          console.error("Failed to load prefix details", err);
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
