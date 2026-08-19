export interface Isourceconfig {
  sourceId: number;
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
  sourceName: string;
  topicName: string;
}

export interface ISourceCdrConfig {
  id: number;
  sourceId: number;
  fieldName: string;
  sequence: number;
}

export interface ISourceConfigManage {
  delete: boolean;
  isDelete: boolean;
  id: number;
  pulseName: string;
  noOfUnits: number;
  serviceType: string;
  unit: string;
}
