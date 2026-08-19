export interface IrateGroup {
  ratePackageGroupId: number;
  ratePackageId: number;
  name: string;
  status: string;
  createdate: string;
  updatedate: string;
  displayName: string;
  lastModifiedByName: string | null;
  mvnoId: number | null;
  isDelete: boolean;
  delete: boolean;
  description: string;
  packageType: string;
  ratePackageType: string;
  ratePackageGroupName: string;
}

export interface IRateGroupManage {
  delete: boolean;
  isDelete: boolean;
  id: number;
  pulseName: string;
  noOfUnits: number;
  serviceType: string;
  unit: string;
}
