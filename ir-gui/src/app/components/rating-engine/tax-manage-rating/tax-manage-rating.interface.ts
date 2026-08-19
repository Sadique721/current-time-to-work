export interface ITaxConfig {
  taxConfigId?: number;
  taxType: string;
  taxName?: string;
  standardRate: number;
  allowsInputCredit: boolean;
  isActive: boolean;
  effectiveFrom: string;
  effectiveTo?: string;
  applyOn: string;
}
