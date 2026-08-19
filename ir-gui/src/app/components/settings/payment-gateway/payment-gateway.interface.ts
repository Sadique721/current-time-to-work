export interface IPaymentGateway {
  createDate: string;
  isActive: boolean;
  mvnoId: number;
  paymentConfigId: number;
  paymentConfigMappingList: {
    paymentConfigMappingId: number;
    parameterDisplayName: string;
    paymentParameterName: string;
    paymentParameterValue: string;
  }[];
  paymentConfigName: string;
  paymentGatewayInfo: string;
}

export interface IPaymentGatewayType {
  id: number;
  text: string;
  value: string;
  type: string;
  status: string;
  displayId: number;
  displayName: string;
  mvnoId: number;
}

export interface IpaymentConfigMapping {
  paymentConfigMappingId: number;
  paymentConfigId: number;
  paymentParameterName: string;
  paymentParameterValue: string | null;
  paymentParameterDescription: string;
  parameterDisplayName: string;
  paymentParameterFor: string;
}
