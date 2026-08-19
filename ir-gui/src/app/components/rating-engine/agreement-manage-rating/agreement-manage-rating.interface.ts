export interface IAccountAgreement {
  accountAgreementId?: number;
  accountId: number;
  accountCode: string;
  accountType: string;
  invoiceFormat: string;
}

export interface IAgreementTaxConfig {
  taxConfigId: number;
  applyOrder: number;
  accumulateFromOrders: string | null;
}

export interface IAgreement {
  agreementId: number;
  agreementCode: string;
  billingCycleStartDate: string;
  billingCyclePeriod: number;
  isIncomingSettlement: boolean;
  isOutgoingSettlement: boolean;
  isNetSettlement: boolean;
  incomingSettlementTemplateId: number | null;
  outgoingSettlementTemplateId: number | null;
  netSettlementTemplateId: number | null;
  incomingSettlementTemplateName: string | null;
  outgoingSettlementTemplateName: string | null;
  netSettlementTemplateName: string | null;
  partnerName: string;
  accountAgreements: IAccountAgreement[];
  lineOfBusiness: string | null;
  homePlmn: string | null;
  visitorPlmn: string | null;
  tapDirection: string | null;
  isTaxExempt: boolean;
  taxConfigs: IAgreementTaxConfig[];
}
