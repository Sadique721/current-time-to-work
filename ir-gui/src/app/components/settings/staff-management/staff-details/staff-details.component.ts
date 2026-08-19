import { Component, OnInit } from '@angular/core';
import { ChangePasswordComponent } from '../change-password/change-password.component';
import { StaffManagementService } from '../staff-management.service';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { CommonService, routes } from 'src/app/core.index';
import { StaffReceiptComponent } from './staff-receipt/staff-receipt.component';
import { ChildMenuEnum, MenuEnum } from 'src/app/core/enums/sidebar-menu.enum';
import { CommonModule } from '@angular/common';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { StaffWalletLedgerComponent } from './staff-wallet-ledger/staff-wallet-ledger.component';

declare var bootstrap: any;

@Component({
  selector: 'app-staff-details',
  imports: [
    CommonModule,
    ChangePasswordComponent,
    RouterModule,
    StaffReceiptComponent,
    StaffWalletLedgerComponent,
  ],
  templateUrl: './staff-details.component.html',
  styleUrl: './staff-details.component.scss',
})
export class StaffDetailsComponent implements OnInit {
  routes = routes;
  staffData: any;
  viewReceiptAccess: boolean = false;
  profileWalletAccess: boolean = false;
  profileChangePassWordAccess: boolean = false;
  isStaffPersonalDataShow = true;
  isStaffReceiptDataShow = false;
  isStaffWalletShow = false;
  staffProfileImage!: SafeResourceUrl;
  bankList: any[] = [];
  currency!: string;

  constructor(
    private route: ActivatedRoute,
    private commonService: CommonService,
    private sanitizer: DomSanitizer,
  ) {}

  ngOnInit(): void {
    const data = this.route.snapshot.data;
    this.staffData = data['staffDetails'] || {};
    this.bankList = data['bankList'] || [];
    this.currency = data['currencyConfiguration'] || '';
    this.getPermission();
    this.getStaffProfileImage();
  }

  getStaffProfileImage(): void {
    const base64 = this.staffData.profileImage;
    let mimeType = 'image/jpeg';

    if (base64.startsWith('iVBORw0KGgo')) {
      mimeType = 'image/png';
    } else if (base64.startsWith('/9j/')) {
      mimeType = 'image/jpeg';
    }
    this.staffProfileImage = this.sanitizer.bypassSecurityTrustResourceUrl(
      `data:${mimeType};base64,${base64}`,
    );
  }

  getPermission(): void {
    const loggedInUserId = this.commonService.userId;
    if (loggedInUserId == this.staffData.id) {
      this.viewReceiptAccess = this.commonService.hasPermission(
        [MenuEnum.SETTING, ChildMenuEnum.MY_PROFILE, 'staff_receipt'],
        true,
      ).view;

      this.profileWalletAccess = this.commonService.hasPermission(
        [MenuEnum.SETTING, ChildMenuEnum.MY_PROFILE, 'my_profile_wallet'],
        true,
      ).view;

      this.profileChangePassWordAccess = this.commonService.hasPermission(
        [
          MenuEnum.SETTING,
          ChildMenuEnum.MY_PROFILE,
          'my_profile_change_password',
        ],
        true,
      ).view;
    } else {
      this.viewReceiptAccess = this.commonService.hasPermission(
        [
          MenuEnum.SETTING,
          ChildMenuEnum.STAFF,
          'staff_details',
          'staff_details_receipt',
        ],
        true,
      ).view;

      this.profileWalletAccess = this.commonService.hasPermission(
        [
          MenuEnum.SETTING,
          ChildMenuEnum.STAFF,
          'staff_details',
          'staff_details_wallet',
        ],
        true,
      ).view;

      this.profileChangePassWordAccess = this.commonService.hasPermission(
        [
          MenuEnum.SETTING,
          ChildMenuEnum.STAFF,
          'staff_details',
          'staff_change_password',
        ],
        true,
      ).view;
    }
  }

  staffView(): void {
    this.isStaffPersonalDataShow = true;
    this.isStaffReceiptDataShow = false;
    this.isStaffWalletShow = false;
  }
  receiptView(): void {
    this.isStaffPersonalDataShow = false;
    this.isStaffReceiptDataShow = true;
    this.isStaffWalletShow = false;
  }
  walletView(): void {
    this.isStaffWalletShow = true;
    this.isStaffPersonalDataShow = false;
    this.isStaffReceiptDataShow = false;
  }

  openChangePasswordDialog(): void {
    const modalElement = document.getElementById('change-password');
    if (modalElement) {
      const modalInstance =
        bootstrap.Modal.getInstance(modalElement) ||
        new bootstrap.Modal(modalElement);
      modalInstance.show();
    }
  }
}
