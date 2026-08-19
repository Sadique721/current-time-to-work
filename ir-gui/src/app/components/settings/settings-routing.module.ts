import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { RoleManagementComponent } from './role-management/role-management.component';
import { SettingsComponent } from './settings.component';
import { RoleManagementAddEditComponent } from './role-management/role-management-add-edit/role-management-add-edit.component';
import { AuthGuard, UnsavedChangesGuard } from 'src/app/core.index';
import { RoleAuthGuard } from 'src/app/core/guard/role-auth.guard';
import { SystemConfigComponent } from './system-config/system-config.component';
import { NotificationTemplateListComponent } from './notification-template-list/notification-template-list.component';
import { PaymentGatewayAddEditComponent } from './payment-gateway/payment-gateway-add-edit/payment-gateway-add-edit.component';
import { PaymentGatewayComponent } from './payment-gateway/payment-gateway.component';
import { StaffManagementComponent } from './staff-management/staff-management.component';
import { StaffAddEditComponent } from './staff-management/staff-add-edit/staff-add-edit.component';
import { SystemConfigResolver } from './staff-management/staff-resolvers/system-config-list.resolver';
import { AgentListResolver } from './staff-management/staff-resolvers/agent-list.resolver';
import { BusinessUnitListResolver } from './staff-management/staff-resolvers/business-unit-list.resolver';
import { ServiceAreaListResolver } from './staff-management/staff-resolvers/service-area-list.resolver';
import { TeamListResolver } from './staff-management/staff-resolvers/team-list.resolver';
import { RoleListForLoggedInUser } from './staff-management/staff-resolvers/role-list-for-logged-in-user.resolver';
import { StaffDetailsComponent } from './staff-management/staff-details/staff-details.component';
import { CurrencyConfigurationResolver } from './staff-management/staff-resolvers/currency-configuration.resolver';
import { StaffDetailsResolver } from './staff-management/staff-resolvers/staff-details.resolver';
import { BankListResolver } from './staff-management/staff-resolvers/bank-list.resolver';
import { MvnoManagementComponent } from './mvno-management/mvno-management.component';
import { MvnoAddEditComponent } from './mvno-management/mvno-add-edit/mvno-add-edit.component';
import { MvnoDocumentListComponent } from './mvno-management/mvno-document-list/mvno-document-list.component';
import { MvnoDocumentAddEditComponent } from './mvno-management/mvno-document-add-edit/mvno-document-add-edit.component';
import { MvnoDetailsComponent } from './mvno-management/mvno-details/mvno-details.component';
import { AudioPromptsComponent } from './audio-prompts/audio-prompts.component';
import { EmailSMTPComponent } from './email-smtp/email-smtp.component';
import { LeadStatusComponent } from './lead-status/lead-status.component';
import { TelecomCircleComponent } from './telecom-circle/telecom-circle.component';
import { BreakCodesComponent } from './break-codes/break-codes.component';
import { CallScriptComponent } from './call-script/call-script.component';
import { AudioPromptsAddEditComponent } from './audio-prompts/audio-prompts-add-edit/audio-prompts-add-edit.component';
import { LeadStatusAddEditComponent } from './lead-status/lead-status-add-edit/lead-status-add-edit.component';
import { TelecomCircleAddEditComponent } from './telecom-circle/telecom-circle-add-edit/telecom-circle-add-edit.component';
import { BreakCodesAddEditComponent } from './break-codes/break-codes-add-edit/break-codes-add-edit.component';
import { CallScriptAddEditComponent } from './call-script/call-script-add-edit/call-script-add-edit.component';

const routes: Routes = [
  {
    path: '',
    component: SettingsComponent,
    children: [
      {
        path: 'role-management',
        component: RoleManagementComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: 'role-management/add-edit',
        component: RoleManagementAddEditComponent,
        canDeactivate: [UnsavedChangesGuard],
      },
      {
        path: 'staff-management',
        component: StaffManagementComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: 'staff-management/add-edit',
        component: StaffAddEditComponent,
        canDeactivate: [UnsavedChangesGuard],
        resolve: {
          systemConfigs: SystemConfigResolver,
          roleListForLoggedInUser: RoleListForLoggedInUser,
          teamList: TeamListResolver,
          serviceAreaList: ServiceAreaListResolver,
          businessUnitList: BusinessUnitListResolver,
          agentList: AgentListResolver,
        },
      },
      {
        path: 'staff-management/mystaff/:id',
        component: StaffDetailsComponent,
        canActivate: [AuthGuard],
        resolve: {
          staffDetails: StaffDetailsResolver,
          currencyConfiguration: CurrencyConfigurationResolver,
          BankList: BankListResolver,
        },
      },
      {
        path: 'my-profile/:id',
        component: StaffDetailsComponent,
        canActivate: [AuthGuard],
        resolve: {
          staffDetails: StaffDetailsResolver,
          currencyConfiguration: CurrencyConfigurationResolver,
          bankList: BankListResolver,
        },
      },
      {
        path: 'system-config',
        component: SystemConfigComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: 'notification-template-list',
        component: NotificationTemplateListComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: 'payment-gateway',
        component: PaymentGatewayComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: 'payment-gateway/add-edit',
        component: PaymentGatewayAddEditComponent,
        canDeactivate: [UnsavedChangesGuard],
      },
      {
        path: 'mvno-management',
        component: MvnoManagementComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: 'mvno-management/details/:id',
        component: MvnoDetailsComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: 'mvno-management/add-edit',
        component: MvnoAddEditComponent,
        canDeactivate: [UnsavedChangesGuard],
      },
      {
        path: 'mvno-management/document/:id',
        component: MvnoDocumentListComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: 'mvno-management/document/:id/add-edit',
        component: MvnoDocumentAddEditComponent,
        canDeactivate: [UnsavedChangesGuard],
      },
      {
        path: 'audio-prompts',
        component: AudioPromptsComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: 'audio-prompts/add-edit',
        component: AudioPromptsAddEditComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: 'email-smtp',
        component: EmailSMTPComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: 'lead-status',
        component: LeadStatusComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: 'lead-status/add-edit',
        component: LeadStatusAddEditComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: 'telecom-circle',
        component: TelecomCircleComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: 'telecom-circle/add-edit',
        component: TelecomCircleAddEditComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: 'break-codes',
        component: BreakCodesComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: 'break-codes/add-edit',
        component: BreakCodesAddEditComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: 'call-script',
        component: CallScriptComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: 'call-script/add-edit',
        component: CallScriptAddEditComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
    ],
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class SettingsRoutingModule {}
