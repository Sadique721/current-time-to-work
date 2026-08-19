export interface IAclEntry {
  menuid?: number;
  code?: string;
}

export interface IAclMenu {
  id: number;
  name: string;
  code: string;
  isSelected: boolean;
}

export interface IRole {
  id?: number;
  rolename: string;
  status: string;
  product: string;
  aclMenu: IAclEntry[];
}

export interface IAclMenu {
  data: { name: string; code: string; id: number; isSelected: boolean };
  children?: { name: string; code: string; id: number; isSelected: boolean }[];
}
