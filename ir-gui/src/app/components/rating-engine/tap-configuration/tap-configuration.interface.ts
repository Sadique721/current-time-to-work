export interface ITapFieldMapping {
  id?: number;
  callType: string;
  fieldName: string;
  asnPath: string;
  dataType: string;
  outSourceColumn: string;
  inTargetColumn: string;
  defaultValue: string | null;
  isMandatory: boolean;
}

export interface IProfileFieldOverride {
  id?: number;
  tapFieldMappingId: number;
  tapFieldMapping?: ITapFieldMapping;
  customDefaultValue: string | null;
  isMandatoryOverride: boolean | null;
}

export interface ITapProfile {
  id?: number;
  profileName: string;
  description: string;
  isActive: boolean;
  fieldMappings: IProfileFieldOverride[];
}
