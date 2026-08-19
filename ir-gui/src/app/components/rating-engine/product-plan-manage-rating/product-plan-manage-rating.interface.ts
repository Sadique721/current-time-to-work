export interface Iproductplan {
  productPlanId: number;
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
  description: string;
  packageType: string;
  packageName: string;
}

export interface IProductPlanManage {
  delete: boolean;
  isDelete: boolean;
  id: number;
  pulseName: string;
  noOfUnits: number;
  serviceType: string;
  unit: string;
}
