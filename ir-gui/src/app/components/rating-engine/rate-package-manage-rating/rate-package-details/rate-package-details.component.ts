import { Component, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { CommonService, routes } from "src/app/core.index";
import { RatePackageManageService } from "../rate-package-manage-rating.service";

@Component({
  selector: "app-rate-package-details",
  templateUrl: "./rate-package-details.component.html",
  styleUrl: "./rate-package-details.component.scss",
  standalone: false,
})
export class RatePackageDetailsComponent implements OnInit {
  public routes = routes;
  selectedRatePackage: any = {};
  isStaffPersonalDataShow = true;

  constructor(
    private route: ActivatedRoute,
    private commonService: CommonService,
    private ratePackageService: RatePackageManageService
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get("id");
    const id = idParam ? Number(idParam) : null;
    if (!id) {
      this.commonService.toastError("Invalid rate package id");
      return;
    }
    this.fetchDetails(id);
  }

  private fetchDetails(id: number): void {
    this.commonService.spinnerShow();
    this.ratePackageService.getById(id).subscribe({
      next: (res: any) => {
        this.commonService.spinnerHide();
        this.selectedRatePackage = res || {};
      },
      error: (err: any) => {
        this.commonService.spinnerHide();
        this.commonService.toastError(
          err?.error?.msg || "Failed to load rate package details"
        );
      },
    });
  }

  staffView(): void {
    this.isStaffPersonalDataShow = true;
  }
}
