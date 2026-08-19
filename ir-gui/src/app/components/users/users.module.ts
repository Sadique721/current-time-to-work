import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { sharedModule } from 'src/app/core.index';

import { RouterModule } from '@angular/router';
import { UsersRoutingModule } from './users-routing.module';
import { UserComponent } from './user/user.component';
import { UserAddEditComponent } from './user/user-add-edit/user-add-edit.component';
import { UserGroupComponent } from './user-group/user-group.component';
import { UserRoleComponent } from './user-role/user-role.component';
import { UserGroupAddEditComponent } from './user-group/user-group-add-edit/user-group-add-edit.component';
import { UserRoleAddEditComponent } from './user-role/user-role-add-edit/user-role-add-edit.component';
import { UsersComponent } from './users.component';
import { CustomElementModule } from 'src/app/core/shared/custom-elements/custom-elemets.module';
import { SipDevicesComponent } from './sip-devices/sip-devices.component';
import { SipDevicesAddEditComponent } from './sip-devices/sip-devices-add-edit/sip-devices-add-edit.component';
import { SipDevicesOtherFeaturesComponent } from './sip-devices/sip-devices-other-features/sip-devices-other-features.component';


@NgModule({
  declarations: [
    UsersComponent,
    UserComponent,
    UserAddEditComponent,
    UserGroupComponent,
    UserRoleComponent,
    UserGroupAddEditComponent,
    UserRoleAddEditComponent,
    SipDevicesComponent,
    SipDevicesAddEditComponent,
    SipDevicesOtherFeaturesComponent
  ],
  imports: [
    CommonModule,
    UsersRoutingModule,
    sharedModule,
    RouterModule,
    CustomElementModule,
],
})
export class UsersModule {}
