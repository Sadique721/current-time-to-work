export class routes {
  private static base = "";
  static paymentreport: string;

  public static get baseUrl(): string {
    return this.base;
  }

  public static get index(): string {
    return this.dashboard;
  }

  public static get signIn(): string {
    return this.baseUrl + "/signin";
  }
  public static get errorPages(): string {
    return this.baseUrl + "/error-pages";
  }
  public static get error404(): string {
    return this.errorPages + "/error-404";
  }
  public static get error500(): string {
    return this.errorPages + "/error-500";
  }
  
  public static get master(): string {
    return this.baseUrl + "/master-management";
  }
  public static get country(): string {
    return this.master + "/country";
  }
  public static get state(): string {
    return this.master + "/state";
  }
  public static get city(): string {
    return this.master + "/city";
  }
  public static get pincode(): string {
    return this.master + "/pincode";
  }
  public static get area(): string {
    return this.master + "/area";
  }
  public static get department(): string {
    return this.master + "/department";
  }
  public static get investmentCode(): string {
    return this.master + "/investment-code";
  }
  public static get bank(): string {
    return this.master + "/bank";
  }
  public static get businessUnit(): string {
    return this.master + "/business-unit";
  }
  public static get subBusinessUnit(): string {
    return this.master + "/sub-business-unit";
  }
  public static get branch(): string {
    return this.master + "/branch";
  }
  public static get region(): string {
    return this.master + "/region";
  }
  public static get businessVertical(): string {
    return this.master + "/business-vertical";
  }
  public static get subBusinessVertical(): string {
    return this.master + "/sub-business-vertical";
  }
  public static get serviceArea(): string {
    return this.master + "/service-area";
  }

  public static get location(): string {
    return this.master + "/location";
  }

  

  public static get rating(): string {
    return this.baseUrl + "/rating-engine";
  }
  public static get ratingcountry(): string {
    return this.rating + "/country-rating";
  }
  public static get ratingprefix(): string {
    return this.rating + "/prefix-rating";
  }
  public static get ratingpulse(): string {
    return this.rating + "/pulse-rating";
  }
  public static get ratingpartner(): string {
    return this.rating + "/partner-rating";
  }
  public static get ratingaccount(): string {
    return this.rating + "/account-rating";
  }
  public static get ratingratepackage(): string {
    return this.rating + "/rate-package-rating";
  }
  public static get ratingratepackagegroup(): string {
    return this.rating + "/rate-package-group-rating";
  }
  public static get ratingproductplan(): string {
    return this.rating + "/product-plan-rating";
  }
  public static get ratingsourceconfiguration(): string {
    return this.rating + "/source-configuration-rating";
  }
  public static get ratingagreement(): string {
    return this.rating + "/agreement-rating";
  }
  public static get ratingscheduler(): string {
    return this.rating + "/scheduler-rating";
  }
  public static get ratingscheduleraudit(): string {
    return this.rating + "/scheduler-audit-logs";
  }
  public static get ratingdownloadcdrs(): string {
    return this.rating + "/download-cdrs";
  }
  public static get ratinginvoices(): string {
    return this.rating + "/invoices";
  }
  public static get ratingorganization(): string {
    return this.rating + "/organization-manage";
  }
  public static get ratinginvoicetemplatemanage(): string {
    return this.rating + "/invoice-template-manage";
  }
  public static get ratingclearinghouse(): string {
    return this.rating + "/clearing-house-rating";
  }
  public static get ratingzone(): string {
    return this.rating + "/zone-rating";
  }
  public static get ratingexchangerate(): string {
    return this.rating + "/exchange-rates";
  }
  public static get errorRateRequests(): string {
    return this.rating + "/error-rate-requests";
  }
  public static get rerateRequests(): string {
    return this.rating + "/rerate-requests";
  }
  public static get errorAnalysis(): string {
    return this.rating + "/error-analysis";
  }
  public static get ratingtaprecords(): string {
    return this.rating + "/tap-records";
  }
  public static get ratingtapsummary(): string {
    return this.rating + "/tap-summary";
  }
  public static get ratingtapconfiguration(): string {
    return this.rating + "/tap-configuration";
  }
  public static get ratingtax(): string {
    return this.rating + "/tax-rating";
  }



  


  public static get managers(): string {
    return this.baseUrl + "/managers";
  }
  public static get manager(): string {
    return this.managers + "/manager";
  }
  public static get managerrole(): string {
    return this.managers + "/manager-role";
  }


  

  public static get users(): string {
    return this.baseUrl + "/users";
  }
  public static get user(): string {
    return this.users + "/user";
  }
  public static get userrole(): string {
    return this.users + "/user-role";
  }
  public static get usergroup(): string {
    return this.users + "/user-group";
  }
  public static get sipdevices(): string {
    return this.users + "/sip-devices";
  }


  

  public static get broadcasting(): string {
    return this.baseUrl + "/broadcasting";
  }
  public static get voicebroadcasting(): string {
    return this.broadcasting + "/voice-broadcasting";
  }

  

  public static get integration(): string {
    return this.baseUrl + "/integration";
  }
  public static get webhooks(): string {
    return this.integration + "/webhooks";
  }





  

  public static get product(): string {
    return this.baseUrl + "/product-management";
  }
  public static get servicemanagement(): string {
    return this.product + "/service-management";
  }
  public static get taxmanagement(): string {
    return this.product + "/tax-management";
  }
  public static get chargemanagement(): string {
    return this.product + "/charge-management";
  }
  public static get qospolicymanagement(): string {
    return this.product + "/qos-policy-management";
  }
  public static get timebasepolicymanagement(): string {
    return this.product + "/time-base-policy-management";
  }
  public static get planmanagement(): string {
    return this.product + "/plan-management";
  }
  public static get planbundle(): string {
    return this.product + "/plan-bundle";
  }
  public static get discountmanagement(): string {
    return this.product + "/discount-management";
  }

  public static get specialplanmappingmanagement(): string {
    return this.product + "/special-plan-mapping-management";
  }
  public static get vouchermanagement(): string {
    return this.product + "/voucher-management";
  }
  public static get vouchermanagementprofile(): string {
    return this.product + "/voucher-management/profile";
  }
  public static get vouchermanagementbatch(): string {
    return this.product + "/voucher-management/batch";
  }

  
  public static get salescrm(): string {
    return this.baseUrl + "/sales-crm";
  }
  public static get leadSourceMaster(): string {
    return this.salescrm + "/lead-source-master";
  }

  

  public static get customer(): string {
    return this.baseUrl + "/customer-management";
  }
  public static get customerlist(): string {
    return this.customer + "/customer";
  }

  

  public static get campaign(): string {
    return this.baseUrl + "/campaign-management";
  }
  public static get smscampaignlist(): string {
    return this.campaign + "/sms-campaign";
  }

  public static get ivrcampaignlist(): string {
    return this.campaign + "/ivr-campaign";
  }

  public static get campaignresponse(): string {
    return this.campaign + "/campaign-response";
  }



    

  public static get contactcenter(): string {
    return this.baseUrl + "/contact-center";
  }
  public static get inboundcampaign(): string {
    return this.contactcenter + "/inbound-campaign";
  }

  public static get outboundcampaign(): string {
    return this.contactcenter + "/outbound-campaign";
  }

  public static get blendedcampaign(): string {
    return this.contactcenter + "/blended-campaign";
  }

  public static get dispositions(): string {
    return this.contactcenter + "/dispositions";
  }

  public static get dnc(): string {
    return this.contactcenter + "/dnc";
  }

  public static get webform(): string {
    return this.contactcenter + "/webform";
  }


  

  public static get leadhub(): string {
    return this.baseUrl + "/lead-hub";
  }
  public static get leadsources(): string {
    return this.leadhub + "/lead-sources";
  }

  public static get rejectionreasons(): string {
    return this.leadhub + "/rejection-reasons";
  }

  public static get leadmanagement(): string {
    return this.leadhub + "/lead-management";
  }

   public static get leads(): string {
    return this.leadhub + "/leads";
  }

   public static get leadgroup(): string {
    return this.leadhub + "/lead-group";
  }

   public static get followup(): string {
    return this.leadhub + "/follow-up";
  }

   public static get customfields(): string {
    return this.leadhub + "/custom-fields";
  }

  
  public static get notifications(): string {
    return this.baseUrl + "/notifications";
  }
  public static get smsConfigurations(): string {
    return this.notifications + "/sms-configurations";
  }
  public static get sms(): string {
    return this.notifications + "/sms";
  }
  public static get whatsappConfigurations(): string {
    return this.notifications + "/whatsapp-configurations";
  }

  public static get whatsappchatConfigurations(): string {
    return this.notifications + "/whatsapp-chat-configurations";
  }

   public static get ticketchatconfiguration(): string {
    return this.notifications + "/ticket-chat-configuration";
  }

  public static get otp(): string {
    return this.notifications + "/otp";
  }

  public static get emailconfiguration(): string {
    return this.notifications + "/email-configuration";
  }

  public static get email(): string {
    return this.notifications + "/email";
  }

  public static get whatsapp(): string {
    return this.notifications + "/whatsapp";
  }

  public static get whatsapptemplate(): string {
    return this.notifications + "/whatsapp-template";
  }

  

  public static get ruleEngine(): string {
    return this.baseUrl + "/ruleEngine";
  }
  public static get smsRuleEngine(): string {
    return this.ruleEngine + "/sms-rules";
  }

  

  public static get ivrConfiguration(): string {
    return this.baseUrl + "/ivr-configuration";
  }
  public static get siptrunk(): string {
    return this.ivrConfiguration + "/sip-trunk";
  }
  public static get outgoingrules(): string {
    return this.ivrConfiguration + "/outgoing-rules";
  }
  public static get dids(): string {
    return this.ivrConfiguration + "/did";
  }

  

  public static get incomingrules(): string {
    return this.baseUrl + "/incoming-rules";
  }

  public static get recordings(): string {
    return this.incomingrules + "/recordings";
  }

  public static get callerId(): string {
    return this.incomingrules + "/caller-id";
  }
  public static get ipMapping(): string {
    return this.incomingrules + "/ip-mapping";
  }
  public static get clickToCall(): string {
    return this.incomingrules + "/click-to-call";
  }
  public static get ringGroup(): string {
    return this.incomingrules + "/ring-group";
  }
  public static get ivr(): string {
    return this.incomingrules + "/ivr";
  }
  public static get timeCondition(): string {
    return this.incomingrules + "/time-condition";
  }
  public static get conference(): string {
    return this.incomingrules + "/conference";
  }
  public static get callQueue(): string {
    return this.incomingrules + "/call-queue";
  }
  public static get blockInbound(): string {
    return this.incomingrules + "/block-inbound";
  }
  public static get featureCodes(): string {
    return this.incomingrules + "/feature-codes";
  }

  

  public static get support(): string {
    return this.baseUrl + "/support";
  }
  public static get tatTracking(): string {
    return this.support + "/tat-tracking";
  }
  public static get problemdomains(): string {
    return this.support + "/problem-domains";
  }
  public static get subproblemdomains(): string {
    return this.support + "/sub-problem-domains";
  }
  public static get rootcausemaster(): string {
    return this.support + "/root-cause-master";
  }
  public static get ticketManagement(): string {
    return this.support + "/ticket-management";
  }
  public static get openOpportunities(): string {
    return this.support + "/open-opportunities";
  }
  public static get tatTrackingTasks(): string {
    return this.support + "/tat-tracking-tasks";
  }
  public static get taskCategories(): string {
    return this.support + "/task-categories";
  }
  public static get taskSubCategories(): string {
    return this.support + "/task-subcategories";
  }
  public static get taskManagement(): string {
    return this.support + "/task-management";
  }
  public static get rootcausemastertasks(): string {
    return this.support + "/root-cause-master-tasks";
  }

  
  public static get settings(): string {
    return this.baseUrl + "/settings";
  }
  public static get roleManagement(): string {
    return this.settings + "/role-management";
  }
  public static get staffManagement(): string {
    return this.settings + "/staff-management";
  }
  public static get myProfile(): string {
    return this.settings + "/my-profile";
  }
  public static get systemConfig(): string {
    return this.settings + "/system-config";
  }
  public static get notificationTemplateList(): string {
    return this.settings + "/notification-template-list";
  }
  public static get paymentGateway(): string {
    return this.settings + "/payment-gateway";
  }
  public static get mvnoManagement(): string {
    return this.settings + "/mvno-management";
  }

   public static get audioprompts(): string {
    return this.settings + "/audio-prompts";
  }

   public static get emailsmtp(): string {
    return this.settings + "/email-smtp";
  }

   public static get leadstatus(): string {
    return this.settings + "/lead-status";
  }

   public static get telecomcircle(): string {
    return this.settings + "/telecom-circle";
  }

   public static get breakcodes(): string {
    return this.settings + "/break-codes";
  }

   public static get callscript(): string {
    return this.settings + "/call-script";
  }

  
  public static get audit(): string {
    return this.baseUrl + "/audit";
  }
  public static get auditLog(): string {
    return this.audit + "/audit-log";
  }
  public static get reportedProblem(): string {
    return this.audit + "/reported-problem";
  }

  public static get dashboard(): string {
    return this.baseUrl + "/dashboard";
  }


  public static get reports(): string {
    return this.baseUrl + "/reports";
  }
  public static get cdrs(): string {
    return this.reports + "/cdrs";
  }

  public static get livecalls(): string {
    return this.reports + "/live-calls";
  }

  public static get missedcall(): string {
    return this.reports + "/missed-call";
  }

  public static get realtimereport(): string {
    return this.reports + "/realtime-report";
  }

  public static get ivrreport(): string {
    return this.reports + "/ivr-report";
  }

  public static get inboundoutboundreport(): string {
    return this.reports + "/inbound-outbound-report";
  }
  public static get calldropreport(): string {
    return this.reports + "/call-drop-report";
  }
  public static get dispositionreport(): string {
    return this.reports + "/disposition-report";
  }
  public static get leadreport(): string {
    return this.reports + "/lead-report";
  }
  public static get campaignreport(): string {
    return this.reports + "/campaign-report";
  }

  public static get voicebroadcastreport(): string {
    return this.reports + "/voice-broadcast-report";
  }

  public static get userperformance(): string {
    return this.reports + "/user-performance";
  }

  public static get loginlogoutreport(): string {
    return this.reports + "/login-logout-report";
  }

   public static get voicemail(): string {
    return this.reports + "/voicemail";
  }

   public static get stickyagentreport(): string {
    return this.reports + "/sticky-agent-report";
  }




  }
