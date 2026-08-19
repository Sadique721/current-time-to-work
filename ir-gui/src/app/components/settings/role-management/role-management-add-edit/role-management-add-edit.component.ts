import { Component, HostListener, OnDestroy, OnInit } from '@angular/core';
import {
  UntypedFormGroup,
  UntypedFormControl,
  Validators,
} from '@angular/forms';
import { finalize, catchError, Subject, takeUntil, EMPTY, iif } from 'rxjs';
import {
  CommonService,
  routes,
  sharedModule,
  status,
} from 'src/app/core.index';
import { WhiteeSpaceValidator } from 'src/app/core/shared/custom-validations/white-space.validator';
import { RoleManagementService } from '../role-management.service';
import { CustomElementModule } from 'src/app/core/shared/custom-elements/custom-elemets.module';
import { CommonModule } from '@angular/common';
import { TreeTableModule } from 'primeng/treetable';
import { TreeNode } from 'primeng/api';
import { Router } from '@angular/router';
import { IAclEntry, IRole } from '../role-management-interface';

@Component({
  selector: 'app-role-management-add-edit',
  imports: [sharedModule, CustomElementModule, CommonModule, TreeTableModule],
  templateUrl: './role-management-add-edit.component.html',
  styleUrl: './role-management-add-edit.component.scss',
})
export class RoleManagementAddEditComponent implements OnInit, OnDestroy {
  roleForm: UntypedFormGroup;
  private destroy$ = new Subject<void>();
  statusOptions = status;
  tableData: TreeNode[] = [];
  selectAllFC: UntypedFormControl;
  data: any = {};
  permissionFG: UntypedFormGroup;
  isLoading: boolean = true;
  rolePermissionList: IAclEntry[] = [];
  isDisable: boolean = false;

  constructor(
    private common: CommonService,
    private roleManagementService: RoleManagementService,
    private router: Router
  ) {
    const nav = this.router.getCurrentNavigation();
    this.data = nav ? nav?.extras?.state : null;

    this.selectAllFC = new UntypedFormControl('');
    this.permissionFG = new UntypedFormGroup({});

    this.roleForm = new UntypedFormGroup({
      rolename: new UntypedFormControl('', [
        Validators.required,
        WhiteeSpaceValidator.cannotContainSpace,
      ]),
      status: new UntypedFormControl('', [Validators.required]),
    });
  }

  ngOnInit(): void {
    if (this.data == null || !this.data?.isAddEdit) {
      this.router.navigateByUrl(routes.roleManagement, {
        replaceUrl: true,
      });
    }

    if (this.data && this.data?.id) {
      const { status, rolename, ...rest } = this.data;
      this.roleForm.patchValue({
        status,
        rolename,
      });

      this.common.spinnerShow();
      this.roleManagementService
        .getRoleById(this.data.id)
        .pipe(
          takeUntil(this.destroy$),
          finalize(() => {
            this.common.spinnerHide();
          }),
          catchError((error) => {
            this.common.toastError(error?.error?.ERROR);
            return EMPTY;
          })
        )
        .subscribe((response: any) => {
          this.tableData = response?.data?.aclMenus || [];
          this.createControls();
        });
    } else {
      this.common.spinnerShow();
      this.roleManagementService
        .getAllACLMenu()
        .pipe(
          takeUntil(this.destroy$),
          finalize(() => {
            this.common.spinnerHide();
          }),
          catchError((error) => {
            this.common.toastError(error?.error?.ERROR);
            return EMPTY;
          })
        )
        .subscribe((response: any) => {
          this.tableData = response?.datalist || [];
          this.createControls();
        });
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  @HostListener('window:beforeunload', ['$event'])
  handleBeforeUnload(event: BeforeUnloadEvent) {
    if (this.data && this.data?.isAddEdit) {
      event.preventDefault();
    } else {
      this.onClose();
    }
  }

  canDeactivate(): boolean {
    if (this.data == null || !this.data?.isAddEdit) return true;

    const confirmLeave = window.confirm(
      'Changes will be lost. Do you want to go back?'
    );

    if (!confirmLeave) {
      this.common.spinnerHide();
    }

    return confirmLeave;
  }

  onClose() {
    this.data?.isAddEdit ? (this.data.isAddEdit = false) : null;

    this.roleForm.reset();
    this.permissionFG.reset();
    this.router.navigateByUrl(routes.roleManagement, {
      replaceUrl: true,
    });
  }

  getPermissionItem(name: string): UntypedFormControl {
    return this.permissionFG.get(name) as UntypedFormControl;
  }

  setPermissionItem(name: string, value: boolean): void {
    this.permissionFG.addControl(name, new UntypedFormControl(value, []));
  }

  createControls(): void {
    this.tableData.forEach((i) => {
      this.setDynamicControl(i);
    });

    this.isLoading = false;
    this.selectAllFC.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe((res) => {
        const updateSelection = (nodes: any[]) => {
          nodes.forEach((node) => {
            node.data.isSelected = res;
            if (node.children?.length) {
              updateSelection(node.children);
            }
          });
        };

        updateSelection(this.tableData);

        
        let i = 0;
        for (const control of Object.values(this.permissionFG.controls)) {
          control.setValue(res, { emitEvent: false });
          i++;
        }
      });
    this.updateGlobalSelection();
  }

  private setChildrenSelection(children: any[], value: boolean): void {
    children?.forEach((child) => {
      const childData = child.data;
      childData.isSelected = value;

      const childControl = this.getPermissionItem(childData.tempControlName);
      if (childControl) {
        childControl.setValue(value, { emitEvent: false });
      }

      if (child.children?.length) {
        this.setChildrenSelection(child.children, value);
      }
    });
  }

  setDynamicControl(node: any): void {
    const item = node.data;
    const controlName = `${item.code}${item.parenid}${item.id}`;
    item.tempControlName = controlName;

    this.setPermissionItem(controlName, !!item.isSelected);

    const control = this.getPermissionItem(controlName);
    if (!control) return;

    control.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe((newValue: boolean) => {
        item.isSelected = newValue;
        this.setChildrenSelection(node.children, newValue);
        this.updateParentSelection(node);
        
      });

    
    node.children?.forEach(this.setDynamicControl.bind(this));
  }

  private updateGlobalSelection(): void {
    const isAllSelected = this.tableData.every((i) => i.data.isSelected);
    this.selectAllFC.setValue(isAllSelected, { emitEvent: false });
  }

  private updateParentSelection(node: any): void {
  const parent = node.parent;
  if (!parent || !Array.isArray(parent.children)) return;

  const anySelected = parent.children.some(
    (child: any) => !!child.data?.isSelected
  );

  
  parent.data.isSelected = anySelected;

  const parentControl = this.getPermissionItem(parent.data.tempControlName);
  if (parentControl) {
    parentControl.setValue(anySelected, { emitEvent: false });
  }

  this.updateParentSelection(parent); 
}

  
  private saveRolePermissions() {
    this.rolePermissionList = [];
    this.tableData.forEach((permission: any) => {
      
      this.createRolePermissionList(permission);
      if (permission.children?.length! > 0) {
        this.saveRolePermissionsChildren(permission.children);
      }
    });
  }

  
  saveRolePermissionsChildren(childItem: any) {
    childItem.map((permission: any) => {
      this.createRolePermissionList(permission);
      if (permission.children?.length! > 0) {
        this.saveRolePermissionsChildren(permission.children);
      }
    });
  }

  
  createRolePermissionList(object: any) {
    if (object.data.isSelected) {
      const aclEntry: IAclEntry = {
        code: object.data.code,
        menuid: object.data.id,
      };
      this.rolePermissionList.push(aclEntry);
    }
  }

  submit(): void {
    if (this.roleForm.valid) {
      this.saveRolePermissions();
      if (this.rolePermissionList.length == 0) {
        this.common.toastError(
          'Please select atleast one operation permission.'
        );
        return;
      }

      const formData = this.roleForm.value;

      const role: IRole = {
        rolename: formData.rolename,
        status: formData.status,
        product: 'Bss',
        aclMenu: this.rolePermissionList,
      };

      const id = this.data.id;
      if (id) {
        role.id = id;
      }

      this.common.spinnerShow();

      iif(
        () => !!id,
        this.roleManagementService.updateRole(role),
        this.roleManagementService.addRole(role)
      )
        .pipe(
          takeUntil(this.destroy$),
          finalize(() => {
            this.common.spinnerHide();
          }),
          catchError((error) => {
            this.common.toastError(error?.error?.ERROR);
            return EMPTY;
          })
        )
        .subscribe((response: any) => {
          if (response.responseCode == 200) {
            this.data.isAddEdit = false;
            this.common.toastSuccess(response.responseMessage || '');
            this.router.navigateByUrl(routes.roleManagement, {
              state: { refreshList: response.state == 200 },
              replaceUrl: true,
            });
          } else {
            this.common.toastError(response?.responseMessage || '');
          }
        });
    } else {
      this.roleForm.markAllAsTouched();
    }
  }
}
