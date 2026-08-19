export interface Irate {
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
  priceRounding: string;
  noOfUnits: number;
  serviceType: string;
  type: string;
  rounding: string;
  ratePackageType: string;
  packageName: string;
}

export interface IRateManage {
  delete: boolean;
  isDelete: boolean;
  id: number;
  pulseName: string;
  noOfUnits: number;
  serviceType: string;
  unit: string;
}
