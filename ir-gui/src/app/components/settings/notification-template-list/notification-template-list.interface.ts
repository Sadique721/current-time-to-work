export interface INotificationTemplate {
  templateId: number;
  eventName: string;
  buName: string;
  appendUrl: string;
  smsEventConfigured: boolean;
  emailEventConfigured: boolean;
  smsTemplateData: string;
  emailTemplateData: string;
}
