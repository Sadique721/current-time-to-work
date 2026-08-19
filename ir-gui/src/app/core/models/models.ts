import { ChildMenuEnum, MenuEnum } from '../enums/sidebar-menu.enum';
export interface apiResultFormat {
  data: [];
  totalData: number;
}
export interface url {
  url: string;
}

export interface pageSelection {
  skip: number;
  limit: number;
}
export interface tablePageSize {
  skip: number;
  limit: number;
  pageSize: number;
}
export interface pageSizeCal {
  totalData: number;
  pageSize: number;
  tableData: Array<any>;
  tableData2?: Array<any>;
  tableDataCopy?: Array<any>;
  serialNumberArray: Array<number>;
  currentPage?: number;
  skip?: number; 
}
export interface pageSize {
  pageSize: number;
}

export const status = [
  { label: 'Active', value: 'Y', val: 'ACTIVE' },
  { label: 'Inactive', value: 'N', val: 'INACTIVE' },
];
export interface IMenuItem {
  name: string;
  showSubRoute: boolean;
  hasSubRoute: boolean;
  code: MenuEnum;
  icon?: string;
  icon2?: string;
  route: string;
  routeKey?: string;
  subMenus: IChildMenu[];
}
export interface IChildMenu {
  name: string;
  code: ChildMenuEnum;
  icon?: string;
  icon2?: string;
  route: string;
  routeKey?: string;
  showSubRoute: boolean;
  hasSubRoute: boolean;
  subMenus: ISubMenu[];
}
export interface ISubMenu {
  name: string;
  code: ChildMenuEnum;
  route: string;
  routeKey?: string;
}
export interface IPermission {
  create: boolean;
  edit: boolean;
  delete: boolean;
  view: boolean;
}
