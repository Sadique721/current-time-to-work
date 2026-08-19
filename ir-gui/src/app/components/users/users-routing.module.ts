import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { UsersComponent } from './users.component';
import { AuthGuard, UnsavedChangesGuard } from 'src/app/core.index';
import { RoleAuthGuard } from 'src/app/core/guard/role-auth.guard';
import { UserComponent } from './user/user.component';
import { UserAddEditComponent } from './user/user-add-edit/user-add-edit.component';
import { UserGroupComponent } from './user-group/user-group.component';
import { UserGroupAddEditComponent } from './user-group/user-group-add-edit/user-group-add-edit.component';
import { UserRoleComponent } from './user-role/user-role.component';
import { UserRoleAddEditComponent } from './user-role/user-role-add-edit/user-role-add-edit.component';
import { SipDevicesAddEditComponent } from './sip-devices/sip-devices-add-edit/sip-devices-add-edit.component';
import { SipDevicesComponent } from './sip-devices/sip-devices.component';
import { SipDevicesOtherFeaturesComponent } from './sip-devices/sip-devices-other-features/sip-devices-other-features.component';


const routes: Routes = [
  {
    path: '',
    component: UsersComponent,
    children: [
      {
        path: 'user',
        component: UserComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: 'user/add-edit',
        component: UserAddEditComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: 'user-group',
        component: UserGroupComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: 'user-group/add-edit',
        component: UserGroupAddEditComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: 'user-role',
        component: UserRoleComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: 'user-role/add-edit',
        component: UserRoleAddEditComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: 'sip-devices',
        component: SipDevicesComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: 'sip-devices/add-edit',
        component: SipDevicesAddEditComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: 'sip-devices/other-features',
        component: SipDevicesOtherFeaturesComponent,
        canActivate: [AuthGuard, RoleAuthGuard],
      },
     
    ],
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class UsersRoutingModule {}
