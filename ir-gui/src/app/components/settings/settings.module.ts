import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SettingsRoutingModule } from './settings-routing.module';
import { sharedModule } from 'src/app/core.index';
import { SettingsComponent } from './settings.component';
import { RouterModule } from '@angular/router';
import { RoleManagementComponent } from './role-management/role-management.component';
import { NotificationTemplateListComponent } from './notification-template-list/notification-template-list.component';
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
import { CustomElementModule } from "src/app/core/shared/custom-elements/custom-elemets.module";

@NgModule({
  declarations: [SettingsComponent, AudioPromptsComponent, EmailSMTPComponent, LeadStatusComponent, TelecomCircleComponent, BreakCodesComponent, CallScriptComponent, AudioPromptsAddEditComponent, LeadStatusAddEditComponent, TelecomCircleAddEditComponent, BreakCodesAddEditComponent, CallScriptAddEditComponent],
  imports: [
    CommonModule,
    SettingsRoutingModule,
    sharedModule,
    RouterModule,
    RoleManagementComponent,
    NotificationTemplateListComponent,
    CustomElementModule
],
})
export class SettingsModule {}
