export interface Iaccount {
  accountId: number;
  name: string;
  status: string;
  createdate: string;
  updatedate: string;
  displayName: string;
  lastModifiedByName: string | null;
  mvnoId: number | null;
  isDelete: boolean;
  delete: boolean;
  accountCode: string;
  productPlanName: string;
  accountType: string;
  partnerName: string;
}

export interface IAccountManage {
  delete: boolean;
  isDelete: boolean;
  id: number;
  accountCode: string;
  productPlanName: string;
  accountType: string;
  partnerName: string;
}
