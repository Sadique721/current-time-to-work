import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { AuditRoutingModule } from './audit-routing.module';
import { sharedModule } from 'src/app/core/shared/shared.module';
import { RouterModule } from '@angular/router';
import { RoleManagementComponent } from '../settings/role-management/role-management.component';
import { AuditComponent } from './audit.component';

@NgModule({
  declarations: [AuditComponent],
  imports: [
    CommonModule,
    AuditRoutingModule,
    sharedModule,
    RouterModule,
    RoleManagementComponent,
  ],
})
export class AuditModule {}
