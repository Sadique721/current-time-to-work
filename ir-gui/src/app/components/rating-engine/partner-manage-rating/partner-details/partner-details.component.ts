import { Component, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { routes, CommonService } from "src/app/core.index";
import { PartnerManageService } from "../partner-manage-rating.service";

@Component({
  selector: "app-partner-details",
  templateUrl: "./partner-details.component.html",
  styleUrl: "./partner-details.component.scss",
  standalone: false,
})
export class PartnerDetailsComponent implements OnInit {
  public routes = routes;
  selectedPartner: any = {};

  constructor(
    private route: ActivatedRoute,
    private commonService: CommonService,
    private partnerManageService: PartnerManageService
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get("id");
    const id = idParam ? Number(idParam) : null;
    if (!id) {
      this.commonService.toastError("Invalid partner id");
      return;
    }
    this.fetchDetails(id);
  }

  private fetchDetails(id: number): void {
    this.commonService.spinnerShow();
    this.partnerManageService.getById(id).subscribe({
      next: (res: any) => {
        this.commonService.spinnerHide();
        this.selectedPartner = res || {};
      },
      error: (err: any) => {
        this.commonService.spinnerHide();
        this.commonService.toastError(
          err?.error?.msg || "Failed to load partner details"
        );
      },
    });
  }
}
