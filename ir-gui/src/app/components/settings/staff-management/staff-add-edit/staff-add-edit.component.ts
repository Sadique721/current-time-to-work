import { CommonModule } from "@angular/common";
import { Component, HostListener, OnDestroy, OnInit } from "@angular/core";
import { FormBuilder, UntypedFormGroup, Validators } from "@angular/forms";
import { ActivatedRoute, Router, RouterModule } from "@angular/router";
import { routes } from "src/app/core/helpers/routes";
import { SidebarService } from "src/app/core/service/sidebar.service";
import { WhiteeSpaceValidator } from "src/app/core/shared/custom-validations/white-space.validator";
import { CustomElementModule } from "src/app/core/shared/custom-elements/custom-elemets.module";
import { sharedModule } from "../../../../core/shared/shared.module";
import { InputGroupModule } from "primeng/inputgroup";
import { InputGroupAddonModule } from "primeng/inputgroupaddon";
import { InputTextModule } from "primeng/inputtext";
import { CommonService } from "src/app/core/service/common.service";
import { StaffManagementService } from "../staff-management.service";
import { catchError, of, Subject, takeUntil } from "rxjs";
import { MultiSelectChangeEvent } from "primeng/multiselect";
import { SelectModule } from "primeng/select";
import { countryCodeList } from "src/app/core/models/country-code.constant";
import { ValidationPattern } from "src/app/core/models/validation";

@Component({
  selector: "app-staff-add-edit",
  imports: [
    CommonModule,
    RouterModule,
    CustomElementModule,
    sharedModule,
    InputGroupModule,
    InputGroupAddonModule,
    InputTextModule,
    SelectModule,
  ],
  templateUrl: "./staff-add-edit.component.html",
  styleUrl: "./staff-add-edit.component.scss",
})
export class StaffAddEditComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  public routes = routes;
  isCollapsed: boolean = false;
  staffGroupForm!: UntypedFormGroup;
  imageUrl: string | ArrayBuffer = "";
  fileToUpload: File | null = null;
  statusOption: any[] = [
    { value_field: "ACTIVE", display_field: "Active" },
    { value_field: "INACTIVE", display_field: "Inactive" },
    { value_field: "TERMINATED", display_field: "Terminated" },
  ];
  
  countryCodeList = countryCodeList;
  countryCodeFromSystemConfig!: string;
  mobileNoLengthFromSystemConfig!: number;
  systemConfigs: any[] = [];
  loggedInUserRoleList: any[] = [];
  teamList: any[] = [];
  serviceAreaList: any[] = [];
  businessUnitList: any[] = [];
  agentList = [1];
  parentStaffList: any[] = [];
  branchList: any[] = [];
  staffData: any = {};
  password: boolean[] = [false];

  constructor(
    private sidebar: SidebarService,
    private fb: FormBuilder,
    private commonService: CommonService,
    private staffManagementService: StaffManagementService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    const nav = this.router.getCurrentNavigation();
    this.staffData = nav ? nav?.extras?.state : null;
  }

  ngOnInit(): void {
    if (this.staffData == null || !this.staffData?.isAddEdit) {
      this.router.navigateByUrl(routes.staffManagement, {
        replaceUrl: true,
      });
    }

    
    const data = this.route.snapshot.data;
    this.systemConfigs = data["systemConfigs"] || [];
    this.loggedInUserRoleList = data["roleListForLoggedInUser"] || [];
    this.teamList = data["teamList"] || [];
    this.serviceAreaList = data["serviceAreaList"] || [];
    this.businessUnitList = data["businessUnitList"] || [];
    this.agentList = data["agentList"] || [];

    this.countryCodeFromSystemConfig =
      this.systemConfigs.find((item) => item.name === "COUNTRY_CODE")?.value ||
      "+91";
    this.mobileNoLengthFromSystemConfig = Number(
      this.systemConfigs.find((item) => item.name === "MOBILE_NUMBER")?.value
    );
    this.initializeForm();
    if (this.staffData && this.staffData.id) {
      this.setStaffFormValues(this.staffData);
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  @HostListener("window:beforeunload", ["$event"])
  handleBeforeUnload(event: BeforeUnloadEvent) {
    if (this.staffData && this.staffData?.isAddEdit) {
      event.preventDefault();
    } else {
      this.onClose();
    }
  }

  initializeForm(): void {
    this.staffGroupForm = this.fb.group({
      username: ["", Validators.required],
      password: [
        "",
        [
          Validators.required,
          WhiteeSpaceValidator.cannotContainSpace,
          Validators.pattern(ValidationPattern.password),
        ],
      ],
      email: ["", [Validators.required, Validators.email]],
      firstname: ["", Validators.required],
      lastname: ["", Validators.required],
      status: ["", Validators.required],
      phone: ["", [Validators.required, Validators.pattern("^[0-9]*$")]],
      roleIds: ["", Validators.required],
      teamIds: [[]],
      serviceAreaIdsList: [[]],
      businessUnitIdsList: [[]],
      agentid: [""],
      parentStaffId: [""],
      mvnoid: [""],
      countryCode: [this.countryCodeFromSystemConfig],
      branchId: [""],
      staffUserServiceMappingList: [],
      file: [""],
      hrmsId: [""],
      tacacsAccessLevelGroup: [""],
    });
    this.staffGroupForm.get("parentStaffId")?.disable();
  }

  setStaffFormValues(staffData: any): void {
    this.parentStaffList = [];
    this.imageUrl = "";
    this.staffGroupForm.get("username")?.disable();
    this.staffGroupForm.get("password")?.disable();
    this.setBranchInEditStaff(staffData.serviceAreasId, staffData);
    this.mapServiceAreaIds(staffData);
    this.updateProfileImage(staffData);
    this.staffGroupForm.patchValue(staffData);
    this.staffGroupForm.get("roleIds")?.patchValue(staffData.roleIds[0]);
    this.updateBusinessUnitList(staffData);
  }

  mapServiceAreaIds(staffData: any): void {
    if (!staffData.serviceAreaIdsList) {
      staffData.serviceAreaIdsList = staffData.serviceAreaNameList.map(
        (element: any) => element.id
      );
    }
    this.staffGroupForm.patchValue({
      serviceAreaIdsList: staffData.serviceAreaIdsList,
    });
  }

  updateProfileImage(staffData: any): void {
    if (staffData.profileImage) {
      const base64 = staffData.profileImage;
      let mimeType = "image/jpeg";

      if (base64.startsWith("iVBORw0KGgo")) {
        mimeType = "image/png";
      } else if (base64.startsWith("/9j/")) {
        mimeType = "image/jpeg";
      }

      this.imageUrl = `data:${mimeType};base64,${base64}`;

      
      const byteString = atob(base64);
      const arrayBuffer = new ArrayBuffer(byteString.length);
      const intArray = new Uint8Array(arrayBuffer);
      for (let i = 0; i < byteString.length; i++) {
        intArray[i] = byteString.charCodeAt(i);
      }
      const blob = new Blob([intArray], { type: mimeType });

      
      const file = new File([blob], "profileImage." + mimeType.split("/")[1], {
        type: mimeType,
      });

      
      const dataTransfer = new DataTransfer();
      dataTransfer.items.add(file);
      const fileList = dataTransfer.files;

      this.staffGroupForm.get("file")?.setValue(fileList);
    }
  }

  updateBusinessUnitList(staffData: any): void {
    if (!staffData.businessUnitIdsList) {
      staffData.businessUnitIdsList =
        staffData.businessUnitNameList?.map((element: any) => element.id) || [];
    }
    this.staffGroupForm.patchValue({
      businessUnitIdsList: staffData.businessUnitIdsList,
    });

    this.updateBusinessUnitFlags(staffData.businessUnitIdsList);
  }

  updateBusinessUnitFlags(businessUnitIdsList: any[]): void {
    this.businessUnitList.forEach((element) => {
      element.flag = businessUnitIdsList?.includes(element.id) ?? false;
    });
  }

  serviceAreaChange(event: MultiSelectChangeEvent): void {
    const serviceArea_ID = event.value;
    this.updateParentStaffList(serviceArea_ID);
    this.getbranchByServiceAreaID(serviceArea_ID);
  }

  staffAutofieldValue(data: any) {
    const serviceArea_ID = data;
    this.updateParentStaffList(serviceArea_ID, this.staffData.id);
  }

  updateParentStaffList(serviceArea_ID: any[], excludeId?: number) {
    this.parentStaffList = [];
    this.staffGroupForm.value.parentStaffId = "";

    if (serviceArea_ID.length > 0) {
      this.staffGroupForm.get("parentStaffId")?.enable();
    } else {
      this.staffGroupForm.get("parentStaffId")?.disable();
    }

    this.staffManagementService
      .getAllStaff()
      .pipe(
        takeUntil(this.destroy$),
        catchError((error) => {
          this.commonService.toastError(error?.error?.ERROR);
          return error;
        })
      )
      .subscribe((response: any) => {
        const allStaff = response.staffUserlist;
        const staffUsername = this.staffGroupForm.value.username;

        const uniqueStaffMap = new Map();

        for (const staff of allStaff) {
          if (
            staff.username !== staffUsername &&
            (!excludeId || staff.id !== excludeId)
          ) {
            const hasMatchingServiceArea = staff.serviceAreaIdsList.some(
              (staffServiceAreaId: any) =>
                serviceArea_ID.includes(staffServiceAreaId)
            );

            if (hasMatchingServiceArea) {
              uniqueStaffMap.set(staff.id, staff);
            }
          }
        }

        this.parentStaffList = Array.from(uniqueStaffMap.values());
      });
  }

  getbranchByServiceAreaID(ids: any[]) {
    let data = [];
    data = ids;
    this.staffManagementService
      .branchByServiceAreaID(data)
      .pipe(
        takeUntil(this.destroy$),
        catchError((error) => {
          this.commonService.toastError(error?.error?.ERROR);
          return error;
        })
      )
      .subscribe((response: any) => {
        this.branchList = response.dataList || [];
      });
  }

  setBranchInEditStaff(ids: any[], staffBranch: any) {
    let data = [];
    data = ids;
    this.staffManagementService
      .branchByServiceAreaID(data)
      .pipe(
        takeUntil(this.destroy$),
        catchError((error) => {
          this.commonService.toastError(error?.error?.ERROR);
          return error;
        })
      )
      .subscribe((response: any) => {
        this.branchList = response.dataList;
        this.branchList.forEach((item) => {
          if (staffBranch.branchName.length > 0) {
            if (staffBranch.branchName === item.name) {
              this.staffGroupForm.patchValue({
                branchId: item.id,
              });
            }
          }
        });
      });
  }

  togglePassword(index: number) {
    this.password[index] = !this.password[index];
  }

  toggleCollapse() {
    this.sidebar.toggleCollapse();
    this.isCollapsed = !this.isCollapsed;
  }

  canDeactivate(): boolean {
    if (this.staffData == null || !this.staffData?.isAddEdit) return true;

    const confirmLeave = window.confirm(
      "Changes will be lost. Are you sure you want leave this page?"
    );

    if (!confirmLeave) {
      this.commonService.spinnerHide();
    }

    return confirmLeave;
  }

  onClose() {
    this.staffData?.isAddEdit ? (this.staffData.isAddEdit = false) : null;

    this.staffGroupForm.reset();
    this.router.navigateByUrl(routes.staffManagement, {
      replaceUrl: true,
    });
  }

  onFileChangeUpload(event: Event): void {
    const input = event.target as HTMLInputElement;
    const files = input.files;
    const selectedFile = files?.item(0);

    if (!selectedFile) return;

    const allowedTypes = ["image/jpeg", "image/png"];
    const maxSize = 2 * 1024 * 1024;

    if (!allowedTypes.includes(selectedFile.type)) {
      this.commonService.toastError(
        "Only JPG and PNG files are allowed",
        "Invalid File Type"
      );
      return;
    }

    if (selectedFile.size > maxSize) {
      this.commonService.toastError(
        "File size cannot exceed 2 MB",
        "File Too Large"
      );
      return;
    }

    const reader = new FileReader();
    reader.onload = (e: any) => {
      this.imageUrl = e.target.result;
    };
    reader.readAsDataURL(selectedFile);

    this.staffGroupForm.patchValue({ file: files });
    this.fileToUpload = selectedFile;
  }

  uploadDocuments(uploadDocStaffId: any): void {
    const fileList: FileList = this.staffGroupForm.value.file;
    if (!fileList || fileList.length === 0) return this.onClose();

    const formData = new FormData();
    formData.append("file", fileList[0]);

    const url = `/staff/uploadProfileImage?staffId=${uploadDocStaffId}`;
    this.staffManagementService
      .uploadStaffProfileImage(url, formData)
      .pipe(
        takeUntil(this.destroy$),
        catchError((error) => {
          this.commonService.toastError(error?.error?.ERROR);
          return error;
        })
      )
      .subscribe((response: any) => {
        this.staffGroupForm.patchValue({ file: null });
        this.onClose();
      });
  }

  submit(): void {
    if (this.staffGroupForm.valid) {
      if (this.staffData.id) {
        this.updateStaff();
      } else {
        this.addNewStaff();
      }
    } else {
      this.staffGroupForm.markAllAsTouched();
    }
  }

  addNewStaff(): void {
    this.ensureCountryCodeIsSet();
    const formData = this.staffGroupForm.value;
    
    
    const payload = {
      username: formData.username,
      password: formData.password,
      firstname: formData.firstname,
      lastname: formData.lastname,
      email: formData.email,
      phone: formData.phone,
      countryCode: formData.countryCode || this.countryCodeFromSystemConfig,
      status: formData.status,
      roleIds: [formData.roleIds], 
      teamIds: formData.teamIds || [],
      serviceAreaIdsList: formData.serviceAreaIdsList || [],
      businessUnitIdsList: formData.businessUnitIdsList || [],
      agentid: formData.agentid || null,
      parentStaffId: formData.parentStaffId || null,
      branchId: formData.branchId || null,
      hrmsId: formData.hrmsId || null,
      isPasswordExpired: false,
      
      partnerid: 1,
      mvnoId: this.commonService.mvnoId || 1,
    };

    
    this.staffManagementService
      .addStaff(payload)
      .pipe(
        takeUntil(this.destroy$),
        catchError((error) => {
                    const errorMessage = error?.error?.ERROR || error?.error?.message || 'Failed to add staff';
          this.commonService.toastError(errorMessage);
          return of(null);
        })
      )
      .subscribe((response: any) => {
        if (response) {
                    const staffId = response?.staffuser?.id || response?.data?.id;
          
          if (staffId) {
            this.uploadDocuments(staffId);
          } else {
            this.onClose();
          }
          
          const successMessage = response?.message || 'Staff added successfully';
          this.commonService.toastSuccess(successMessage);
          this.resetForm();
        }
      });
  }

  updateStaff(): void {
    this.ensureCountryCodeIsSet();
    const formData = this.staffGroupForm.getRawValue();
    
    
    const payload: any = {
      staffId: this.staffData.id,
      username: formData.username,
      firstname: formData.firstname,
      lastname: formData.lastname,
      email: formData.email,
      phone: formData.phone,
      countryCode: formData.countryCode || this.countryCodeFromSystemConfig,
      status: formData.status,
      roleIds: [formData.roleIds], 
      teamIds: formData.teamIds || [],
      serviceAreaIdsList: formData.serviceAreaIdsList || [],
      businessUnitIdsList: formData.businessUnitIdsList || [],
      agentid: formData.agentid || null,
      parentStaffId: formData.parentStaffId || null,
      branchId: formData.branchId || null,
      hrmsId: formData.hrmsId || null,
      
      password: "",
      isPasswordExpired: false,
    };

    
    

    
    this.staffManagementService
      .updateStaff(payload, this.staffData.id)
      .pipe(
        takeUntil(this.destroy$),
        catchError((error) => {
                    const errorMessage = error?.error?.ERROR || error?.error?.message || 'Failed to update staff';
          this.commonService.toastError(errorMessage);
          return of(null);
        })
      )
      .subscribe((response: any) => {
        if (response) {
                    this.uploadDocuments(this.staffData.id);
          const successMessage = response?.message || 'Staff updated successfully';
          this.commonService.toastSuccess(successMessage);
          this.resetForm();
        }
      });
  }

  ensureCountryCodeIsSet(): void {
    if (!this.staffGroupForm.value.countryCode) {
      this.staffGroupForm
        .get("countryCode")
        ?.setValue(this.countryCodeFromSystemConfig);
    }
  }

  resetForm(): void {
    this.imageUrl = "";
    this.staffGroupForm.reset();
    this.parentStaffList = [];
  }

  deleteImage(): void {
    this.imageUrl = "";
    this.fileToUpload = null;
    this.staffGroupForm.patchValue({ file: null });
    
    
    const fileInput = document.getElementById('profileImageUpload') as HTMLInputElement;
    if (fileInput) {
      fileInput.value = '';
    }
  }
}