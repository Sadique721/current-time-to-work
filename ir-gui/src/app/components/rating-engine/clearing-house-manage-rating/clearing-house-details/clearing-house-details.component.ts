import { Component, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { routes, CommonService } from "src/app/core.index";
import { ClearingHouseManageService } from "../clearing-house-manage-rating.service";

@Component({
  selector: "app-clearing-house-details",
  templateUrl: "./clearing-house-details.component.html",
  standalone: false,
})
export class ClearingHouseDetailsComponent implements OnInit {
  public routes = routes;
  item: any = {};

  get isSFTPEnabled(): boolean {
    return Array.isArray(this.item?.protocols) && this.item.protocols.includes("SFTP");
  }

  constructor(
    private route: ActivatedRoute,
    private commonService: CommonService,
    private clearingHouseService: ClearingHouseManageService
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get("id");
    const id = idParam ? Number(idParam) : null;
    if (!id) {
      this.commonService.toastError("Invalid clearing house ID");
      return;
    }
    this.fetchDetails(id);
  }

  private fetchDetails(id: number): void {
    this.commonService.spinnerShow();
    this.clearingHouseService.getById(id).subscribe({
      next: (res: any) => {
        this.commonService.spinnerHide();
        this.item = res || {};
      },
      error: (err: any) => {
        this.commonService.spinnerHide();
        this.commonService.toastError(
          err?.error?.errorMessage || "Failed to load clearing house details"
        );
      },
    });
  }
}
