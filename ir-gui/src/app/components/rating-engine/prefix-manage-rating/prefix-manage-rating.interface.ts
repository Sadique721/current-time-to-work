export interface Iprefix {
  prefixId: number;
  name: string;
  status: string;
  createdate: string;
  updatedate: string;
  displayName: string;
  lastModifiedByName: string | null;
  mvnoId: number | null;
  isDelete: boolean;
  delete: boolean;
  prefixName: string;
  prefix: number;
  countryName: string;
  prefixType?: string;
}

export interface IPrefixManage {
  delete: boolean;
  isDelete: boolean;
  prefixId: number;
  id: number;
  prefixName: string;
  prefix: number;
  countryName: string;
  prefixType?: string;
}
