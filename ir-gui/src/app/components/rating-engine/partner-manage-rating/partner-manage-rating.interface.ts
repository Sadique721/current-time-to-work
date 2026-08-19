export interface Ipartner {
  partnerId: number;
  partnerName: string;
  partnerCode: string;
  partnerType: string;
  lineOfBusiness?: string;
  status: string;
  country: string;
  contactPersonName: string;
  email: string;
  phoneNumber: string;
  addressLine1: string;
  city: string;
  postalCode: string;
  createdate: string;
  updatedate: string;
  displayName: string;
  lastModifiedByName: string | null;
  mvnoId: number | null;
  isDelete: boolean;
  delete: boolean;
}

export interface IPartnerManage {
  delete: boolean;
  isDelete: boolean;
  id: number;
  partnerName: string;
  partnerCode: string;
  partnerType: string;
  lineOfBusiness?: string;
  status: string;
  country: string;
  contactPersonName: string;
  email: string;
  phoneNumber: string;
  addressLine1: string;
  city: string;
  postalCode: string;
}
