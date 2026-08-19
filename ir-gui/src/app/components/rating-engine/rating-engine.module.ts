import { NgModule } from "@angular/core";
import { CommonModule } from "@angular/common";
import { RatingEngineComponent } from "./rating-engine.component";
import { CountryManageComponent } from "./country-manage/country-manage.component";
import { RatingEngineRoutingModule } from "./rating-engine-routing.module";
import { sharedModule } from "src/app/core.index";
import { CustomElementModule } from "src/app/core/shared/custom-elements/custom-elemets.module";
import { RouterModule } from "@angular/router";
import { DatePickerModule } from "primeng/datepicker";
import { TableModule } from "primeng/table";
import { SelectModule } from "primeng/select";
import { CountryRatingAddEditComponent } from "./country-manage/country-rating-add-edit/country-rating-add-edit.component";
import { PrefixManageRatingComponent } from "./prefix-manage-rating/prefix-manage-rating.component";
import { PrefixRatingAddEditComponent } from "./prefix-manage-rating/prefix-rating-add-edit/prefix-rating-add-edit.component";
import { PulseManageRatingComponent } from "./pulse-manage-rating/pulse-manage-rating.component";
import { PulseRatingAddEditComponent } from "./pulse-manage-rating/pulse-rating-add-edit/pulse-rating-add-edit.component";
import { PartnerManageRatingComponent } from "./partner-manage-rating/partner-manage-rating.component";
import { PartnerRatingAddEditComponent } from "./partner-manage-rating/partner-rating-add-edit/partner-rating-add-edit.component";
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
import { SourceConfigurationAddEditComponent } from "./source-configuration-manage-rating/source-configuration-add-edit/source-configuration-add-edit.component";
import { SourceConfigurationDetailsComponent } from "./source-configuration-manage-rating/source-configuration-details/source-configuration-details.component";
import { AgreementManageRatingComponent } from "./agreement-manage-rating/agreement-manage-rating.component";
import { AgreementAddEditComponent } from "./agreement-manage-rating/agreement-add-edit/agreement-add-edit.component";
import { AgreementDetailsComponent } from "./agreement-manage-rating/agreement-details/agreement-details.component";
import { SchedulerConfigurationComponent } from "./scheduler-configuration/scheduler-configuration.component";
import { SchedulerAuditLogsComponent } from "./scheduler-audit-logs/scheduler-audit-logs.component";
import { DownloadCdrsComponent } from "./download-cdrs/download-cdrs.component";
import { InvoicesComponent } from "./invoices/invoices.component";
import { OrganizationManageComponent } from "./organization-manage/organization-manage.component";
import { OrganizationAddEditComponent } from "./organization-manage/organization-add-edit/organization-add-edit.component";
import { InvoiceTemplateManageComponent } from "./invoice-template-manage/invoice-template-manage.component";
import { ClearingHouseManageRatingComponent } from "./clearing-house-manage-rating/clearing-house-manage-rating.component";
import { ClearingHouseAddEditComponent } from "./clearing-house-manage-rating/clearing-house-add-edit/clearing-house-add-edit.component";
import { ClearingHouseDetailsComponent } from "./clearing-house-manage-rating/clearing-house-details/clearing-house-details.component";
import { ZoneManageRatingComponent } from "./zone-manage-rating/zone-manage-rating.component";
import { ZoneRatingAddEditComponent } from "./zone-manage-rating/zone-rating-add-edit/zone-rating-add-edit.component";
import { ZoneRatingDetailsComponent } from "./zone-manage-rating/zone-rating-details/zone-rating-details.component";
import { ExchangeRateListComponent } from "./exchange-rate-manage/exchange-rate-list/exchange-rate-list.component";
import { TapRecordsComponent } from "./tap-records/tap-records.component";
import { TaxManageRatingComponent } from "./tax-manage-rating/tax-manage-rating.component";
import { TaxRatingAddEditComponent } from "./tax-manage-rating/tax-rating-add-edit/tax-rating-add-edit.component";
import { TaxDetailsComponent } from "./tax-manage-rating/tax-details/tax-details.component";
import { TapSummaryComponent } from "./tap-summary/tap-summary.component";
import { TapSummaryDetailsComponent } from "./tap-summary/tap-summary-details/tap-summary-details.component";
import { TapConfigurationComponent } from "./tap-configuration/tap-configuration.component";
import { TapFieldAddEditComponent } from "./tap-configuration/tap-field-add-edit/tap-field-add-edit.component";
import { TapProfileAddEditComponent } from "./tap-configuration/tap-profile-add-edit/tap-profile-add-edit.component";
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
import { ReRateRequestDetailsComponent } from './rerate-request-manage/rerate-request-details/rerate-request-details.component';
@NgModule({
  declarations: [
    RatingEngineComponent,
    CountryManageComponent,
    CountryRatingAddEditComponent,
    PrefixManageRatingComponent,
    PrefixRatingAddEditComponent,
    PulseManageRatingComponent,
    PulseRatingAddEditComponent,
    PartnerManageRatingComponent,
    PartnerRatingAddEditComponent,
    PartnerDetailsComponent,
    AccountManageRatingComponent,
    AccountRatingAddEditComponent,
    RatePackageManageRatingComponent,
    RatePackageAddEditComponent,
    RatePackageDetailsComponent,
    RatePackageGroupManageRatingComponent,
    RatePackageGroupAddEditComponent,
    RatePackageGroupDetailsComponent,
    ProductPlanManageRatingComponent,
    ProductPlanAddEditComponent,
    ProductPlanDetailsComponent,
    SourceConfigurationManageRatingComponent,
    SourceConfigurationAddEditComponent,
    SourceConfigurationDetailsComponent,
    AgreementManageRatingComponent,
    AgreementAddEditComponent,
    AgreementDetailsComponent,
    SchedulerConfigurationComponent,
    SchedulerAuditLogsComponent,
    DownloadCdrsComponent,
    InvoicesComponent,
    OrganizationManageComponent,
    OrganizationAddEditComponent,
    InvoiceTemplateManageComponent,
    ClearingHouseManageRatingComponent,
    ClearingHouseAddEditComponent,
    ClearingHouseDetailsComponent,
    ZoneManageRatingComponent,
    ZoneRatingAddEditComponent,
    ZoneRatingDetailsComponent,
    TapRecordsComponent,
    ExchangeRateListComponent,
    TaxManageRatingComponent,
    TaxRatingAddEditComponent,
    TaxDetailsComponent,
    TapSummaryComponent,
    TapSummaryDetailsComponent,
    TapConfigurationComponent,
    TapFieldAddEditComponent,
    TapProfileAddEditComponent,
    TapFieldDetailsComponent,
    TapProfileDetailsComponent,
    TapProfileGroupAddEditComponent,
    TapProfileGroupDetailsComponent,
    ErrorRateRequestManageComponent,
    ErrorRateRequestAddEditComponent,
    ReRateRequestManageComponent,
    ReRateRequestAddEditComponent,
    ErrorAnalysisComponent,
    ErrorAnalysisDetailsComponent,
    ReRateRequestDetailsComponent
  ],
  imports: [
    CommonModule,
    RatingEngineRoutingModule,
    sharedModule,
    CustomElementModule,
    RouterModule,
    DatePickerModule,
    TableModule,
    SelectModule,
  ],
})
export class RatingEngineModule { }
