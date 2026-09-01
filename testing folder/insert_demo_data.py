import urllib.request
import json
import sys

# Ensure UTF-8 console output
if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')

BASE_URL = "http://localhost:8060/rating-engine/v1/api"

def api_get(endpoint):
    url = f"{BASE_URL}/{endpoint}"
    req = urllib.request.Request(url, headers={"Accept": "application/json"})
    try:
        with urllib.request.urlopen(req) as resp:
            return resp.status, json.loads(resp.read().decode())
    except urllib.error.HTTPError as e:
        body = e.read().decode()
        return e.code, body
    except Exception as e:
        return 500, str(e)

def api_post(endpoint, payload):
    url = f"{BASE_URL}/{endpoint}"
    data = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(url, data=data, headers={"Content-Type": "application/json", "Accept": "application/json"})
    try:
        with urllib.request.urlopen(req) as resp:
            return resp.status, json.loads(resp.read().decode())
    except urllib.error.HTTPError as e:
        body = e.read().decode()
        return e.code, body
    except Exception as e:
        return 500, str(e)

print("Starting API-driven Demo Data insertion...")

# -------------------------------------------------------------
# 1. TAX CONFIGS
# -------------------------------------------------------------
print("\n--- 1. Tax Configs ---")
status, existing_taxes = api_get("tax-configs")
tax_map = {}
if status == 200 and isinstance(existing_taxes, list):
    for t in existing_taxes:
        tax_map[t.get("taxType")] = t.get("taxConfigId")
        print(f"Found existing tax: {t.get('taxType')} (ID: {t.get('taxConfigId')})")

new_taxes = [
    {
        "taxType": "VAT",
        "taxName": "Standard EU VAT 20%",
        "standardRate": 20.00,
        "applyOn": "BASE",
        "effectiveFrom": "2024-01-01",
        "allowsInputCredit": True,
        "isActive": True
    },
    {
        "taxType": "WHT",
        "taxName": "Withholding Tax 5%",
        "standardRate": 5.00,
        "applyOn": "BASE",
        "effectiveFrom": "2024-01-01",
        "allowsInputCredit": False,
        "isActive": True
    }
]

for tax in new_taxes:
    ttype = tax["taxType"]
    if ttype not in tax_map:
        st, res = api_post("tax-configs", tax)
        if st in [200, 201]:
            tax_map[ttype] = res.get("taxConfigId")
            print(f"[SUCCESS] Created Tax {ttype} -> ID: {res.get('taxConfigId')}")
        else:
            print(f"[ERROR] Failed to create Tax {ttype}: {res}")
    else:
        print(f"[SKIP] Tax {ttype} already exists (ID: {tax_map[ttype]})")

# -------------------------------------------------------------
# 2. PULSES
# -------------------------------------------------------------
print("\n--- 2. Pulses ---")
status, pulse_page = api_post("pulse/search", {"page": 0, "size": 100})
pulse_map = {}
if status == 200 and isinstance(pulse_page, dict):
    for p in pulse_page.get("content", []):
        pulse_map[p.get("pulseName")] = p.get("pulseId")
        print(f"Found existing pulse: {p.get('pulseName')} (ID: {p.get('pulseId')})")

new_pulses = [
    {
        "pulseName": "Voice 1s Pulse",
        "serviceType": "VOICE",
        "unit": "SECOND",
        "noOfUnits": 1
    },
    {
        "pulseName": "Data 10KB Pulse",
        "serviceType": "USAGE",
        "unit": "KB",
        "noOfUnits": 10
    }
]

for p in new_pulses:
    pname = p["pulseName"]
    if pname not in pulse_map:
        st, res = api_post("pulse", p)
        if st in [200, 201]:
            pulse_map[pname] = res.get("pulseId")
            print(f"[SUCCESS] Created Pulse {pname} -> ID: {res.get('pulseId')}")
        else:
            print(f"[ERROR] Failed to create Pulse {pname}: {res}")
    else:
        print(f"[SKIP] Pulse {pname} already exists (ID: {pulse_map[pname]})")

# -------------------------------------------------------------
# 3. PREFIXES
# -------------------------------------------------------------
print("\n--- 3. Prefixes ---")
status, prefix_page = api_post("prefixes/search", {"page": 0, "size": 100})
prefix_map = {}
if status == 200 and isinstance(prefix_page, dict):
    for pr in prefix_page.get("content", []):
        prefix_map[pr.get("prefix")] = pr.get("prefixId")
        print(f"Found existing prefix: {pr.get('prefix')} - {pr.get('prefixName')}")

new_prefixes = [
    {
        "countryName": "United Kingdom",
        "prefix": "79",
        "prefixName": "UK Mobile Networks",
        "prefixType": "INTERCONNECT"
    },
    {
        "countryName": "United Kingdom",
        "prefix": "23415",
        "prefixName": "Vodafone UK Network",
        "prefixType": "ROAMING"
    },
    {
        "countryName": "United States",
        "prefix": "310410",
        "prefixName": "ATT Mobility USA",
        "prefixType": "ROAMING"
    },
    {
        "countryName": "United Arab Emirates",
        "prefix": "9715",
        "prefixName": "UAE Mobile Networks",
        "prefixType": "INTERCONNECT"
    }
]

for pr in new_prefixes:
    pnum = pr["prefix"]
    if pnum not in prefix_map:
        st, res = api_post("prefixes", pr)
        if st in [200, 201]:
            prefix_map[pnum] = res.get("prefixId")
            print(f"[SUCCESS] Created Prefix {pnum} ({pr['prefixName']}) -> ID: {res.get('prefixId')}")
        else:
            print(f"[ERROR] Failed to create Prefix {pnum}: {res}")
    else:
        print(f"[SKIP] Prefix {pnum} already exists (ID: {prefix_map[pnum]})")

# -------------------------------------------------------------
# 4. ZONES
# -------------------------------------------------------------
print("\n--- 4. Zones ---")
status, zone_page = api_post("zones/search", {"page": 0, "size": 100})
zone_map = {}
if status == 200 and isinstance(zone_page, dict):
    for z in zone_page.get("content", []):
        zone_map[z.get("zoneName")] = z.get("zoneId")
        print(f"Found existing zone: {z.get('zoneName')} (ID: {z.get('zoneId')})")

new_zones = [
    {
        "zoneName": "ZONE_DOMESTIC_IN",
        "rawPrefixPattern": "40401,40410,40420,40445,405800",
        "description": "Domestic Indian Mobile Networks",
        "priority": 1,
        "prefixInputMode": "MANUAL"
    },
    {
        "zoneName": "ZONE_NORTH_AMERICA",
        "rawPrefixPattern": "310410,310260,302720,1201,1212",
        "description": "USA and Canada Networks",
        "priority": 10,
        "prefixInputMode": "MANUAL"
    },
    {
        "zoneName": "ZONE_EUROPE_TIER1",
        "rawPrefixPattern": "23415,23410,26201,26202,20801",
        "description": "UK Germany France Major Operators",
        "priority": 20,
        "prefixInputMode": "MANUAL"
    },
    {
        "zoneName": "ZONE_GCC_ARAB",
        "rawPrefixPattern": "42402,42403,42001,42003",
        "description": "UAE and Saudi Arabia",
        "priority": 30,
        "prefixInputMode": "MANUAL"
    }
]

for z in new_zones:
    zname = z["zoneName"]
    if zname not in zone_map:
        st, res = api_post("zones", z)
        if st in [200, 201]:
            zone_map[zname] = res.get("zoneId")
            print(f"[SUCCESS] Created Zone {zname} -> ID: {res.get('zoneId')}")
        else:
            print(f"[ERROR] Failed to create Zone {zname}: {res}")
    else:
        print(f"[SKIP] Zone {zname} already exists (ID: {zone_map[zname]})")

# -------------------------------------------------------------
# 5. PARTNERS
# -------------------------------------------------------------
print("\n--- 5. Partners ---")
status, partner_page = api_post("partners/search", {"page": 0, "size": 100})
partner_map = {}
if status == 200 and isinstance(partner_page, dict):
    for part in partner_page.get("content", []):
        partner_map[part.get("partnerCode")] = part.get("partnerId")
        print(f"Found existing partner: {part.get('partnerName')} ({part.get('partnerCode')}) (ID: {part.get('partnerId')})")

new_partners = [
    {
        "partnerName": "Bharti Airtel Interconnect",
        "partnerCode": "AIRTEL_IN_01",
        "partnerType": "BOTH",
        "status": "ACTIVE",
        "organizationId": 4, # Keyanna Telecom Global
        "lineOfBusiness": "INTERCONNECT",
        "interconnectType": "IP",
        "ipAddress": "192.168.25.101",
        "pointCode": "4-123-1",
        "routingPrefix": "91",
        "country": "India",
        "city": "New Delhi",
        "postalCode": "110001",
        "addressLine1": "Airtel Center, Plot 16, Udyog Vihar",
        "contactPersonName": "Rajesh Sharma",
        "email": "interconnect@airtel.com",
        "phoneNumber": "+919810012345",
        "billingCurrency": "USD",
        "billingCycle": "MONTHLY",
        "paymentTerms": "Net 30 Days",
        "taxNumber": "07AAACB1234F1Z8",
        "bankAccountNumber": "50100234567890",
        "swiftCode": "HDFCINBB"
    },
    {
        "partnerName": "Vodafone UK Roaming",
        "partnerCode": "VODAFONE_UK_01",
        "partnerType": "BOTH",
        "status": "ACTIVE",
        "organizationId": 4, # Keyanna Telecom Global
        "lineOfBusiness": "ROAMING",
        "tadigCode": "GBRVF",
        "hplmn": "23415",
        "country": "United Kingdom",
        "city": "London",
        "postalCode": "W1A1AA",
        "addressLine1": "Vodafone House, The Connection",
        "contactPersonName": "Emily Watson",
        "email": "roaming.billing@vodafone.co.uk",
        "phoneNumber": "+447911123456",
        "billingCurrency": "EUR",
        "billingCycle": "MONTHLY",
        "paymentTerms": "Net 45 Days",
        "taxNumber": "GB123456789",
        "bankAccountNumber": "801234567",
        "swiftCode": "BARCGB22"
    }
]

for part in new_partners:
    pcode = part["partnerCode"]
    if pcode not in partner_map:
        st, res = api_post("partners", part)
        if st in [200, 201]:
            partner_map[pcode] = res.get("partnerId")
            print(f"[SUCCESS] Created Partner {part['partnerName']} ({pcode}) -> ID: {res.get('partnerId')}")
        else:
            print(f"[ERROR] Failed to create Partner {pcode}: {res}")
    else:
        print(f"[SKIP] Partner {pcode} already exists (ID: {partner_map[pcode]})")

# -------------------------------------------------------------
# 6. RATE PACKAGES
# -------------------------------------------------------------
print("\n--- 6. Rate Packages ---")
status, rp_page = api_post("rate-packages/search", {"page": 0, "size": 100})
rp_map = {}
if status == 200 and isinstance(rp_page, dict):
    for pkg in rp_page.get("content", []):
        rp_map[pkg.get("packageName")] = pkg.get("ratePackageId")
        print(f"Found existing rate package: {pkg.get('packageName')} (ID: {pkg.get('ratePackageId')})")

voice_1s_id = pulse_map.get("Voice 1s Pulse", 1)
voice_60s_id = pulse_map.get("Voice 60s Pulse (Minute)", 5)
data_10k_id = pulse_map.get("Data 10KB Pulse", pulse_map.get("Data 1MB Pulse", 7))

new_rate_packages = [
    {
        "packageName": "RP_INTR_VOICE_OUTBOUND_STANDARD",
        "packageDesc": "Standard Outbound Buying Tariff for Airtel Interconnect",
        "type": "BUYING",
        "serviceType": "VOICE",
        "ratePackageType": "SOURCE_DESTINATION_BASED",
        "pulseId": voice_1s_id,
        "rounding": "UPPER",
        "priceRounding": "DEFAULT",
        "currency": "USD",
        "rate_details": [
            {
                "sourcePrefix": "91",
                "sourcePrefixName": "India",
                "destinationPrefix": "1",
                "destinationPrefixName": "USA Nationwide Direct",
                "rate": 0.0120,
                "startTime": "2025-01-01 00:00:00",
                "endTime": "2099-12-31 23:59:59"
            },
            {
                "sourcePrefix": "91",
                "sourcePrefixName": "India",
                "destinationPrefix": "447",
                "destinationPrefixName": "UK Mobile EE Vodafone",
                "rate": 0.0350,
                "startTime": "2025-01-01 00:00:00",
                "endTime": "2099-12-31 23:59:59"
            },
            {
                "sourcePrefix": "91",
                "sourcePrefixName": "India",
                "destinationPrefix": "9715",
                "destinationPrefixName": "UAE Mobile Etisalat",
                "rate": 0.1450,
                "startTime": "2025-01-01 00:00:00",
                "endTime": "2099-12-31 23:59:59"
            }
        ]
    },
    {
        "packageName": "RP_INTR_VOICE_INBOUND_STANDARD",
        "packageDesc": "Standard Inbound Selling Tariff for Airtel Termination",
        "type": "SELLING",
        "serviceType": "VOICE",
        "ratePackageType": "DESTINATION_BASED",
        "pulseId": voice_1s_id,
        "rounding": "UPPER",
        "priceRounding": "DEFAULT",
        "currency": "USD",
        "rate_details": [
            {
                "destinationPrefix": "9198",
                "destinationPrefixName": "India Airtel Metro",
                "rate": 0.0085,
                "startTime": "2025-01-01 00:00:00",
                "endTime": "2099-12-31 23:59:59"
            },
            {
                "destinationPrefix": "9199",
                "destinationPrefixName": "India Vodafone Circle",
                "rate": 0.0090,
                "startTime": "2025-01-01 00:00:00",
                "endTime": "2099-12-31 23:59:59"
            }
        ]
    },
    {
        "packageName": "RP_ROAM_IOT_VOICE_GBR",
        "packageDesc": "Vodafone UK Roaming IOT Voice Tariff",
        "type": "BUYING",
        "serviceType": "VOICE",
        "ratePackageType": "ZONE_DESTINATION_BASED",
        "pulseId": voice_60s_id,
        "rounding": "UPPER",
        "priceRounding": "DEFAULT",
        "currency": "EUR",
        "rate_details": [
            {
                "zoneName": "ZONE_EUROPE_TIER1",
                "rate": 0.0450,
                "startTime": "2025-01-01 00:00:00",
                "endTime": "2099-12-31 23:59:59"
            },
            {
                "zoneName": "ZONE_NORTH_AMERICA",
                "rate": 0.0850,
                "startTime": "2025-01-01 00:00:00",
                "endTime": "2099-12-31 23:59:59"
            }
        ]
    },
    {
        "packageName": "RP_ROAM_DATA_GPRS_GBR",
        "packageDesc": "Vodafone UK Roaming Data GPRS Tariff",
        "type": "BUYING",
        "serviceType": "USAGE",
        "ratePackageType": None,
        "pulseId": data_10k_id,
        "rounding": "UPPER",
        "priceRounding": "DEFAULT",
        "currency": "EUR",
        "startTime": "2025-01-01 00:00:00",
        "endTime": "2099-12-31 23:59:59",
        "rate": 0.0025,
        "rate_details": []
    }
]

for pkg in new_rate_packages:
    pname = pkg["packageName"]
    if pname not in rp_map:
        st, res = api_post("rate-packages", pkg)
        if st in [200, 201]:
            rp_map[pname] = res.get("ratePackageId")
            print(f"[SUCCESS] Created Rate Package {pname} -> ID: {res.get('ratePackageId')}")
        else:
            print(f"[ERROR] Failed to create Rate Package {pname}: {res}")
    else:
        print(f"[SKIP] Rate Package {pname} already exists (ID: {rp_map[pname]})")

# -------------------------------------------------------------
# 7. RATE PACKAGE GROUPS
# -------------------------------------------------------------
print("\n--- 7. Rate Package Groups ---")
status, rpg_page = api_post("rate-package-groups/search", {"page": 0, "size": 100})
rpg_map = {}
if status == 200 and isinstance(rpg_page, dict):
    for g in rpg_page.get("content", []):
        rpg_map[g.get("ratePackageGroupName")] = g.get("ratePackageGroupId")
        print(f"Found existing RPG: {g.get('ratePackageGroupName')} (ID: {g.get('ratePackageGroupId')})")

new_rpgs = [
    {
        "ratePackageGroupName": "RPG_INTR_VOICE_BUY",
        "description": "Wholesale Outbound Voice Tier Group",
        "packageType": "BUYING",
        "serviceType": "VOICE",
        "ratePackageSelectionType": "PRIORITY",
        "ratePackages": [
            {
                "ratePackage": rp_map.get("RP_INTR_VOICE_OUTBOUND_STANDARD"),
                "startTime": "2025-01-01 00:00:00",
                "endTime": "2099-12-31 23:59:59",
                "priority": 1
            }
        ]
    },
    {
        "ratePackageGroupName": "RPG_INTR_VOICE_SELL",
        "description": "Termination Inbound Voice Tier Group",
        "packageType": "SELLING",
        "serviceType": "VOICE",
        "ratePackageSelectionType": "PRIORITY",
        "ratePackages": [
            {
                "ratePackage": rp_map.get("RP_INTR_VOICE_INBOUND_STANDARD"),
                "startTime": "2025-01-01 00:00:00",
                "endTime": "2099-12-31 23:59:59",
                "priority": 1
            }
        ]
    },
    {
        "ratePackageGroupName": "RPG_ROAM_VODAFONE_BUY",
        "description": "Roaming IOT Vodafone UK Buying Group",
        "packageType": "BUYING",
        "serviceType": "VOICE",
        "ratePackageSelectionType": "CALL_TYPE",
        "ratePackages": [
            {
                "ratePackage": rp_map.get("RP_ROAM_IOT_VOICE_GBR"),
                "startTime": "2025-01-01 00:00:00",
                "endTime": "2099-12-31 23:59:59",
                "callType": "MO_VOICE"
            },
            {
                "ratePackage": rp_map.get("RP_ROAM_IOT_VOICE_GBR"),
                "startTime": "2025-01-01 00:00:00",
                "endTime": "2099-12-31 23:59:59",
                "callType": "MT_VOICE"
            }
        ]
    }
]

for rpg in new_rpgs:
    gname = rpg["ratePackageGroupName"]
    if gname not in rpg_map:
        st, res = api_post("rate-package-groups", rpg)
        if st in [200, 201]:
            rpg_map[gname] = res.get("ratePackageGroupId")
            print(f"[SUCCESS] Created RPG {gname} -> ID: {res.get('ratePackageGroupId')}")
        else:
            print(f"[ERROR] Failed to create RPG {gname}: {res}")
    else:
        print(f"[SKIP] RPG {gname} already exists (ID: {rpg_map[gname]})")

# -------------------------------------------------------------
# 8. PRODUCT PLANS
# -------------------------------------------------------------
print("\n--- 8. Product Plans ---")
status, pp_page = api_post("product-plans/search", {"page": 0, "size": 100})
pp_map = {}
if status == 200 and isinstance(pp_page, dict):
    for plan in pp_page.get("content", []):
        pp_map[plan.get("name")] = plan.get("productPlanId")
        print(f"Found existing Product Plan: {plan.get('name')} (ID: {plan.get('productPlanId')})")

new_product_plans = [
    {
        "name": "PLAN_AIRTEL_GLOBAL_BUY",
        "description": "Airtel Interconnect Buying Wholesale Plan",
        "packageType": "BUYING",
        "ratePackageGroups": [
            {
                "ratePackageGroupId": rpg_map.get("RPG_INTR_VOICE_BUY"),
                "serviceType": "VOICE"
            }
        ]
    },
    {
        "name": "PLAN_AIRTEL_GLOBAL_SELL",
        "description": "Airtel Interconnect Selling Termination Plan",
        "packageType": "SELLING",
        "ratePackageGroups": [
            {
                "ratePackageGroupId": rpg_map.get("RPG_INTR_VOICE_SELL"),
                "serviceType": "VOICE"
            }
        ]
    },
    {
        "name": "PLAN_VODAFONE_ROAM_BUY",
        "description": "Vodafone Roaming Buying Plan",
        "packageType": "BUYING",
        "ratePackageGroups": [
            {
                "ratePackageGroupId": rpg_map.get("RPG_ROAM_VODAFONE_BUY"),
                "serviceType": "VOICE"
            }
        ]
    }
]

for plan in new_product_plans:
    pname = plan["name"]
    if pname not in pp_map:
        st, res = api_post("product-plans", plan)
        if st in [200, 201]:
            pp_map[pname] = res.get("productPlanId")
            print(f"[SUCCESS] Created Product Plan {pname} -> ID: {res.get('productPlanId')}")
        else:
            print(f"[ERROR] Failed to create Product Plan {pname}: {res}")
    else:
        print(f"[SKIP] Product Plan {pname} already exists (ID: {pp_map[pname]})")

# -------------------------------------------------------------
# 9. ACCOUNTS
# -------------------------------------------------------------
print("\n--- 9. Accounts ---")
status, acc_page = api_post("accounts/search", {"page": 0, "size": 100})
acc_map = {}
if status == 200 and isinstance(acc_page, dict):
    for a in acc_page.get("content", []):
        key = f"{a.get('accountCode')}_{a.get('accountType')}"
        acc_map[key] = a.get("accountId")
        print(f"Found existing Account: {a.get('accountCode')} ({a.get('accountType')}) (ID: {a.get('accountId')})")

airtel_partner_id = partner_map.get("AIRTEL_IN_01")
vodafone_partner_id = partner_map.get("VODAFONE_UK_01")

new_accounts = [
    {
        "accountCode": "ACC_AIRTEL_IN_CUST",
        "partnerId": airtel_partner_id,
        "accountType": "CUSTOMER",
        "productPlanId": pp_map.get("PLAN_AIRTEL_GLOBAL_SELL")
    },
    {
        "accountCode": "ACC_AIRTEL_IN_VEND",
        "partnerId": airtel_partner_id,
        "accountType": "VENDOR",
        "productPlanId": pp_map.get("PLAN_AIRTEL_GLOBAL_BUY")
    },
    {
        "accountCode": "23415",
        "partnerId": vodafone_partner_id,
        "accountType": "VENDOR",
        "productPlanId": pp_map.get("PLAN_VODAFONE_ROAM_BUY")
    }
]

for acc in new_accounts:
    key = f"{acc['accountCode']}_{acc['accountType']}"
    if key not in acc_map and acc["partnerId"] and acc["productPlanId"]:
        st, res = api_post("accounts", acc)
        if st in [200, 201]:
            acc_map[key] = res.get("accountId")
            print(f"[SUCCESS] Created Account {acc['accountCode']} ({acc['accountType']}) -> ID: {res.get('accountId')}")
        else:
            print(f"[ERROR] Failed to create Account {acc['accountCode']}: {res}")
    else:
        print(f"[SKIP] Account {acc['accountCode']} already exists or missing partner/plan (ID: {acc_map.get(key)})")

# -------------------------------------------------------------
# 10. AGREEMENTS
# -------------------------------------------------------------
print("\n--- 10. Agreements ---")
status, ag_page = api_post("agreements/search", {"page": 0, "size": 100})
ag_map = {}
if status == 200 and isinstance(ag_page, dict):
    for ag in ag_page.get("content", []):
        ag_map[ag.get("agreementCode")] = ag.get("agreementId")
        print(f"Found existing Agreement: {ag.get('agreementCode')} (ID: {ag.get('agreementId')})")

gst_tax_id = tax_map.get("GST", 23)
cess_tax_id = tax_map.get("CESS", 24)
vat_tax_id = tax_map.get("VAT", 25)

airtel_cust_acc_id = acc_map.get("ACC_AIRTEL_IN_CUST_CUSTOMER")
airtel_vend_acc_id = acc_map.get("ACC_AIRTEL_IN_VEND_VENDOR")
vodafone_roam_acc_id = acc_map.get("23415_VENDOR")

new_agreements = [
    {
        "agreementCode": "AGR-AIRTEL-INTR-2026",
        "billingCycleStartDate": "2026-08-01",
        "billingType": "MONTHLY",
        "isIncomingSettlement": True,
        "isOutgoingSettlement": True,
        "isNetSettlement": True,
        "incomingSettlementTemplateId": 12,
        "outgoingSettlementTemplateId": 12,
        "netSettlementTemplateId": 12,
        "lineOfBusiness": "INTERCONNECT",
        "isTaxExempt": False,
        "accountAgreements": [
            {
                "accountId": airtel_cust_acc_id,
                "accountCode": "ACC_AIRTEL_IN_CUST",
                "accountType": "CUSTOMER",
                "invoiceFormat": "PDF"
            },
            {
                "accountId": airtel_vend_acc_id,
                "accountCode": "ACC_AIRTEL_IN_VEND",
                "accountType": "VENDOR",
                "invoiceFormat": "PDF"
            }
        ] if airtel_cust_acc_id and airtel_vend_acc_id else [],
        "taxConfigs": [
            {
                "taxConfigId": gst_tax_id,
                "applyOrder": 1
            },
            {
                "taxConfigId": cess_tax_id,
                "applyOrder": 2,
                "accumulateFromOrders": "1"
            }
        ]
    },
    {
        "agreementCode": "AGR-VODAFONE-ROAM-2026",
        "billingCycleStartDate": "2026-08-01",
        "billingType": "MONTHLY",
        "isIncomingSettlement": False,
        "isOutgoingSettlement": True,
        "isNetSettlement": False,
        "outgoingSettlementTemplateId": 14,
        "lineOfBusiness": "ROAMING",
        "tapDirection": "TAP_IN",
        "isTaxExempt": False,
        "accountAgreements": [
            {
                "accountId": vodafone_roam_acc_id,
                "accountCode": "23415",
                "accountType": "VENDOR",
                "invoiceFormat": "PDF"
            }
        ] if vodafone_roam_acc_id else [],
        "taxConfigs": [
            {
                "taxConfigId": vat_tax_id,
                "applyOrder": 1
            }
        ] if vat_tax_id else []
    }
]

for ag in new_agreements:
    acode = ag["agreementCode"]
    if acode not in ag_map:
        st, res = api_post("agreements", ag)
        if st in [200, 201]:
            ag_map[acode] = res.get("agreementId")
            print(f"[SUCCESS] Created Agreement {acode} -> ID: {res.get('agreementId')}")
        else:
            print(f"[ERROR] Failed to create Agreement {acode}: {res}")
    else:
        print(f"[SKIP] Agreement {acode} already exists (ID: {ag_map[acode]})")

print("\nAll Demo Data processing completed successfully!")
