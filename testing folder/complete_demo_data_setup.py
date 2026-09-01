import urllib.request
import json
import sys

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

# 1. Fetch all existing entities
print("Fetching current system state...")
_, taxes = api_get("tax-configs")
_, pulses = api_get("pulse")
_, prefixes = api_get("prefixes")
_, zones = api_get("zones")
_, partners = api_get("partners")
_, rate_packages = api_get("rate-packages")
_, rpgs = api_get("rate-package-groups")
_, product_plans = api_get("product-plans")
_, accounts = api_get("accounts")
_, agreements = api_get("agreements")

print(f"Taxes: {len(taxes) if isinstance(taxes, list) else 'ERR'}")
print(f"Pulses: {len(pulses) if isinstance(pulses, list) else 'ERR'}")
print(f"Prefixes: {len(prefixes) if isinstance(prefixes, list) else 'ERR'}")
print(f"Zones: {len(zones) if isinstance(zones, list) else 'ERR'}")
print(f"Partners: {len(partners) if isinstance(partners, list) else 'ERR'}")
print(f"Rate Packages: {len(rate_packages) if isinstance(rate_packages, list) else 'ERR'}")
print(f"RPGs: {len(rpgs) if isinstance(rpgs, list) else 'ERR'}")
print(f"Product Plans: {len(product_plans) if isinstance(product_plans, list) else 'ERR'}")
print(f"Accounts: {len(accounts) if isinstance(accounts, list) else 'ERR'}")
print(f"Agreements: {len(agreements) if isinstance(agreements, list) else 'ERR'}")

# Build lookup maps
tax_map = {t["taxType"]: t["taxConfigId"] for t in taxes if isinstance(t, dict)}
pulse_map = {p["pulseName"]: p["pulseId"] for p in pulses if isinstance(p, dict)}
partner_map = {p["partnerCode"]: p["partnerId"] for p in partners if isinstance(p, dict)}
rp_map = {r["packageName"]: r["ratePackageId"] for r in rate_packages if isinstance(r, dict)}
rpg_map = {g["ratePackageGroupName"]: g["ratePackageGroupId"] for g in rpgs if isinstance(g, dict)}
pp_map = {p["name"]: p["productPlanId"] for p in product_plans if isinstance(p, dict)}
acc_map = {f"{a['accountCode']}_{a['accountType']}": a["accountId"] for a in accounts if isinstance(a, dict)}
ag_map = {a["agreementCode"]: a["agreementId"] for a in agreements if isinstance(a, dict)}

print("\n--- Rate Packages in DB ---")
for k, v in rp_map.items():
    print(f"  {k}: {v}")

# 2. Create missing Rate Package Groups
print("\n--- Creating Missing RPGs ---")
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
            }
        ]
    }
]

for rpg in new_rpgs:
    gname = rpg["ratePackageGroupName"]
    if gname not in rpg_map:
        st, res = api_post("rate-package-groups", rpg)
        if st in [200, 201]:
            print(f"[SUCCESS] Created RPG {gname}")
        else:
            print(f"[ERROR] Failed to create RPG {gname}: {res}")
    else:
        print(f"[SKIP] RPG {gname} already exists (ID: {rpg_map[gname]})")

# Refresh RPG map
_, rpgs = api_get("rate-package-groups")
rpg_map = {g["ratePackageGroupName"]: g["ratePackageGroupId"] for g in rpgs if isinstance(g, dict)}

# 3. Create missing Product Plans
print("\n--- Creating Missing Product Plans ---")
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
            print(f"[SUCCESS] Created Product Plan {pname}")
        else:
            print(f"[ERROR] Failed to create Product Plan {pname}: {res}")
    else:
        print(f"[SKIP] Product Plan {pname} already exists (ID: {pp_map[pname]})")

# Refresh Product Plan map
_, product_plans = api_get("product-plans")
pp_map = {p["name"]: p["productPlanId"] for p in product_plans if isinstance(p, dict)}

# 4. Create missing Accounts
print("\n--- Creating Missing Accounts ---")
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
            print(f"[SUCCESS] Created Account {acc['accountCode']} ({acc['accountType']})")
        else:
            print(f"[ERROR] Failed to create Account {acc['accountCode']}: {res}")
    else:
        print(f"[SKIP] Account {acc['accountCode']} already exists (ID: {acc_map.get(key)})")

# Refresh Account map
_, accounts = api_get("accounts")
acc_map = {f"{a['accountCode']}_{a['accountType']}": a["accountId"] for a in accounts if isinstance(a, dict)}

# 5. Link Accounts to Agreements
print("\n--- Updating Agreements with Linked Accounts ---")
_, agreements = api_get("agreements")
ag_map = {a["agreementCode"]: a["agreementId"] for a in agreements if isinstance(a, dict)}

airtel_cust_acc_id = acc_map.get("ACC_AIRTEL_IN_CUST_CUSTOMER")
airtel_vend_acc_id = acc_map.get("ACC_AIRTEL_IN_VEND_VENDOR")
vodafone_roam_acc_id = acc_map.get("23415_VENDOR")

gst_tax_id = tax_map.get("GST", 23)
cess_tax_id = tax_map.get("CESS", 24)
vat_tax_id = tax_map.get("VAT", 25)

# Update AGR-AIRTEL-INTR-2026
airtel_ag_id = ag_map.get("AGR-AIRTEL-INTR-2026")
if airtel_ag_id and airtel_cust_acc_id and airtel_vend_acc_id:
    payload = {
        "agreementId": airtel_ag_id,
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
        ],
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
    }
    url = f"{BASE_URL}/agreements/{airtel_ag_id}"
    data = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(url, data=data, headers={"Content-Type": "application/json", "Accept": "application/json"}, method="PUT")
    try:
        with urllib.request.urlopen(req) as resp:
            print(f"[SUCCESS] Updated Agreement AGR-AIRTEL-INTR-2026 with accounts and tax mappings")
    except Exception as e:
        print(f"[ERROR] Failed to update Agreement AGR-AIRTEL-INTR-2026: {e}")

# Update AGR-VODAFONE-ROAM-2026
voda_ag_id = ag_map.get("AGR-VODAFONE-ROAM-2026")
if voda_ag_id and vodafone_roam_acc_id:
    payload = {
        "agreementId": voda_ag_id,
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
        ],
        "taxConfigs": [
            {
                "taxConfigId": vat_tax_id,
                "applyOrder": 1
            }
        ]
    }
    url = f"{BASE_URL}/agreements/{voda_ag_id}"
    data = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(url, data=data, headers={"Content-Type": "application/json", "Accept": "application/json"}, method="PUT")
    try:
        with urllib.request.urlopen(req) as resp:
            print(f"[SUCCESS] Updated Agreement AGR-VODAFONE-ROAM-2026 with account and tax mappings")
    except Exception as e:
        print(f"[ERROR] Failed to update Agreement AGR-VODAFONE-ROAM-2026: {e}")

print("\n--- Final Verification Summary ---")
_, agreements_final = api_get("agreements")
for ag in agreements_final:
    if ag["agreementCode"] in ["AGR-AIRTEL-INTR-2026", "AGR-VODAFONE-ROAM-2026"]:
        print(f"Agreement: {ag['agreementCode']} | LOB: {ag['lineOfBusiness']} | Accounts: {len(ag.get('accountAgreements', []))} | Taxes: {len(ag.get('taxConfigs', []))}")
