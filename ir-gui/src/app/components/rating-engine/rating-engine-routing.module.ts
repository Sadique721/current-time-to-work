import { NgModule } from "@angular/core";
import { RouterModule, Routes } from "@angular/router";
import { RatingEngineComponent } from "./rating-engine.component";
import { CountryManageComponent } from "./country-manage/country-manage.component";
import { AuthGuard } from "src/app/core.index";
import { RoleAuthGuard } from "src/app/core/guard/role-auth.guard";
import { PrefixManageRatingComponent } from "./prefix-manage-rating/prefix-manage-rating.component";
import { PulseManageRatingComponent } from "./pulse-manage-rating/pulse-manage-rating.component";
import { PartnerManageRatingComponent } from "./partner-manage-rating/partner-manage-rating.component";
import { PartnerDetailsComponent } from "./partner-manage-rating/partner-details/partner-details.component";
import { AccountManageRatingComponent } from "./account-manage-rating/account-manage-rating.component";
import { AccountRatingAddEditComponent } from "./account-manage-rating/account-rating-add-edit/account-rating-add-edit.component";
import { RatePackageManageRatingComponent } from "./rate-package-manage-rating/rate-package-manage-rating.component";
import { RatePackageAddEditComponent } from "./rate-package-manage-rating/rate-package-add-edit/rate-package-add-edit.component";
import { RatePackageDetailsComponent } from "./rate-package-manage-rating/rate-package-details/rate-package-details.component";
import { RatePackageGroupManageRatingComponent } from "./rate-package-group-manage-rating/rate-package-group-manage-rating.component";
import { RatePackageGroupDetailsComponent } from "./rate-package-group-manage-rating/rate-package-group-details/rate-package-group-details.component";
import { RatePackageGroupAddEditComponent } from "./rate-package-group-manage-rating/rate-package-group-add-edit/rate-package-group-add-edit.component";
import { ProductPlanManageRatingComponent } from "./product-plan-manage-rating/product-plan-manage-rating.component";
import { ProductPlanAddEditComponent } from "./product-plan-manage-rating/product-plan-add-edit/product-plan-add-edit.component";
import { ProductPlanDetailsComponent } from "./product-plan-manage-rating/product-plan-details/product-plan-details.component";
import { SourceConfigurationManageRatingComponent } from "./source-configuration-manage-rating/source-configuration-manage-rating.component";
import { SourceConfigurationDetailsComponent } from "./source-configuration-manage-rating/source-configuration-details/source-configuration-details.component";
import { AgreementManageRatingComponent } from "./agreement-manage-rating/agreement-manage-rating.component";
import { AgreementDetailsComponent } from "./agreement-manage-rating/agreement-details/agreement-details.component";
import { AgreementAddEditComponent } from "./agreement-manage-rating/agreement-add-edit/agreement-add-edit.component";
import { SchedulerConfigurationComponent } from "./scheduler-configuration/scheduler-configuration.component";
import { SchedulerAuditLogsComponent } from "./scheduler-audit-logs/scheduler-audit-logs.component";
import { DownloadCdrsComponent } from "./download-cdrs/download-cdrs.component";
import { InvoicesComponent } from "./invoices/invoices.component";
import { OrganizationManageComponent } from "./organization-manage/organization-manage.component";
import { InvoiceTemplateManageComponent } from "./invoice-template-manage/invoice-template-manage.component";
import { ClearingHouseManageRatingComponent } from "./clearing-house-manage-rating/clearing-house-manage-rating.component";
import { ClearingHouseDetailsComponent } from "./clearing-house-manage-rating/clearing-house-details/clearing-house-details.component";
import { ZoneManageRatingComponent } from "./zone-manage-rating/zone-manage-rating.component";
import { ZoneRatingAddEditComponent } from "./zone-manage-rating/zone-rating-add-edit/zone-rating-add-edit.component";
import { ZoneRatingDetailsComponent } from "./zone-manage-rating/zone-rating-details/zone-rating-details.component";
import { ExchangeRateListComponent } from "./exchange-rate-manage/exchange-rate-list/exchange-rate-list.component";
import { TapRecordsComponent } from "./tap-records/tap-records.component";
import { TaxManageRatingComponent } from "./tax-manage-rating/tax-manage-rating.component";
import { TaxDetailsComponent } from "./tax-manage-rating/tax-details/tax-details.component";
import { TaxRatingAddEditComponent } from "./tax-manage-rating/tax-rating-add-edit/tax-rating-add-edit.component";
import { TapSummaryComponent } from "./tap-summary/tap-summary.component";
import { TapSummaryDetailsComponent } from "./tap-summary/tap-summary-details/tap-summary-details.component";
import { TapConfigurationComponent } from "./tap-configuration/tap-configuration.component";
import { TapFieldDetailsComponent } from "./tap-configuration/tap-field-details/tap-field-details.component";
import { TapProfileDetailsComponent } from "./tap-configuration/tap-profile-details/tap-profile-details.component";
import { TapProfileGroupAddEditComponent } from "./tap-configuration/tap-profile-group-add-edit/tap-profile-group-add-edit.component";
import { TapProfileGroupDetailsComponent } from "./tap-configuration/tap-profile-group-details/tap-profile-group-details.component";
import { ErrorRateRequestManageComponent } from "./error-rate-request-manage/error-rate-request-manage.component";
import { ErrorRateRequestAddEditComponent } from "./error-rate-request-manage/error-rate-request-add-edit/error-rate-request-add-edit.component";
import { ReRateRequestManageComponent } from "./rerate-request-manage/rerate-request-manage.component";
import { ReRateRequestAddEditComponent } from "./rerate-request-manage/rerate-request-add-edit/rerate-request-add-edit.component";
import { ErrorAnalysisComponent } from "./error-analysis/error-analysis.component";
import { ErrorAnalysisDetailsComponent } from "./error-analysis/error-analysis-details/error-analysis-details.component";
import { ReRateRequestDetailsComponent } from "./rerate-request-manage/rerate-request-details/rerate-request-details.component";
const routes: Routes = [
  {
    path: "",
    component: RatingEngineComponent,
    children: [
      {
        path: "",
        redirectTo: "country",
        pathMatch: "full",
      },
      {
        path: "country-rating",
        component: CountryManageComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: "prefix-rating",
        component: PrefixManageRatingComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: "pulse-rating",
        component: PulseManageRatingComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: "partner-rating",
        component: PartnerManageRatingComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: "partner-rating/partner/:id",
        component: PartnerDetailsComponent,
        canActivate: [AuthGuard],
      },
      {
        path: "account-rating",
        component: AccountManageRatingComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: "account-rating/add-edit",
        component: AccountRatingAddEditComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: "rate-package-rating",
        component: RatePackageManageRatingComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: "rate-package-rating/add-edit",
        component: RatePackageAddEditComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: "rate-package-rating/rate-package/:id",
        component: RatePackageDetailsComponent,
        canActivate: [AuthGuard],
      },
      {
        path: "rate-package-group-rating",
        component: RatePackageGroupManageRatingComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: "rate-package-group-rating/add-edit",
        component: RatePackageGroupAddEditComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: "rate-package-group-rating/rate-package-group/:id",
        component: RatePackageGroupDetailsComponent,
        canActivate: [AuthGuard],
      },
      {
        path: "product-plan-rating",
        component: ProductPlanManageRatingComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: "product-plan-rating/add-edit",
        component: ProductPlanAddEditComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: "product-plan-rating/product-plan/:id",
        component: ProductPlanDetailsComponent,
        canActivate: [AuthGuard],
      },
      {
        path: "source-configuration-rating",
        component: SourceConfigurationManageRatingComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: "source-configuration-rating/source/:id",
        component: SourceConfigurationDetailsComponent,
        canActivate: [AuthGuard],
      },
      {
        path: "agreement-rating",
        component: AgreementManageRatingComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: "agreement-rating/add-edit",
        component: AgreementAddEditComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: "agreement-rating/agreement/:id",
        component: AgreementDetailsComponent,
        canActivate: [AuthGuard],
      },
      {
        path: "scheduler-rating",
        component: SchedulerConfigurationComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: "scheduler-audit-logs",
        component: SchedulerAuditLogsComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: "download-cdrs",
        component: DownloadCdrsComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: "invoices",
        component: InvoicesComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: "organization-manage",
        component: OrganizationManageComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: "invoice-template-manage",
        component: InvoiceTemplateManageComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: "clearing-house-rating",
        component: ClearingHouseManageRatingComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: "clearing-house-rating/details/:id",
        component: ClearingHouseDetailsComponent,
        canActivate: [AuthGuard],
      },
      {
        path: "zone-rating",
        component: ZoneManageRatingComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: "zone-rating/add-edit",
        component: ZoneRatingAddEditComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: "zone-rating/details/:id",
        component: ZoneRatingDetailsComponent,
        canActivate: [AuthGuard],
      },
      {
        path: "tax-rating",
        component: TaxManageRatingComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: "tax-rating/add-edit",
        component: TaxRatingAddEditComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: "tax-rating/details/:id",
        component: TaxDetailsComponent,
        canActivate: [AuthGuard],
      },
      {
        path: "tap-records",
        component: TapRecordsComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: "exchange-rates",
        component: ExchangeRateListComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: "tap-summary",
        component: TapSummaryComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: "tap-summary/details/:tapFileId",
        component: TapSummaryDetailsComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: "tap-configuration",
        component: TapConfigurationComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: "tap-configuration/details/field/:id",
        component: TapFieldDetailsComponent,
        canActivate: [AuthGuard],
      },
      {
        path: "tap-configuration/details/profile/:id",
        component: TapProfileDetailsComponent,
        canActivate: [AuthGuard],
      },
      {
        path: "tap-configuration/details/profile-group/:id",
        component: TapProfileGroupDetailsComponent,
        canActivate: [AuthGuard],
      },
      {
        path: "tap-configuration/profile-group/add-edit",
        component: TapProfileGroupAddEditComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: "tap-configuration/profile-group/add-edit/:id",
        component: TapProfileGroupAddEditComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: "error-rate-requests",
        component: ErrorRateRequestManageComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: "error-rate-requests/add-edit",
        component: ErrorRateRequestAddEditComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: "error-rate-requests/add-edit/:id",
        component: ErrorRateRequestAddEditComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: "rerate-requests",
        component: ReRateRequestManageComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: "rerate-requests/add-edit",
        component: ReRateRequestAddEditComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: "rerate-requests/add-edit/:id",
        component: ReRateRequestAddEditComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: "rerate-requests/details/:id",
        component: ReRateRequestDetailsComponent,
        canActivate: [AuthGuard],
      },
      {
        path: "error-analysis",
        component: ErrorAnalysisComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: "error-analysis/details/:id",
        component: ErrorAnalysisDetailsComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      }
    ],
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class RatingEngineRoutingModule { }
