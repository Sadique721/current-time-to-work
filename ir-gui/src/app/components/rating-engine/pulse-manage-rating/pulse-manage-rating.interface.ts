export interface Ipulse {
  pulseId: number;
  name: string;
  status: string;
  createdate: string;
  updatedate: string;
  displayName: string;
  lastModifiedByName: string | null;
  mvnoId: number | null;
  isDelete: boolean;
  delete: boolean;
  pulseName: string;
  noOfUnits: number;
  serviceType: string;
  unit: string;
}

export interface IPulseManage {
  delete: boolean;
  isDelete: boolean;
  id: number;
  pulseName: string;
  noOfUnits: number;
  serviceType: string;
  unit: string;
}
