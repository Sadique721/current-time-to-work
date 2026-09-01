import urllib.request
import json
import sys

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')

BASE_URL = "http://localhost:8060/rating-engine/v1/api"

def api_call(endpoint, method="GET", payload=None):
    url = f"{BASE_URL}/{endpoint}"
    data = json.dumps(payload).encode("utf-8") if payload else None
    req = urllib.request.Request(url, data=data, headers={"Content-Type": "application/json", "Accept": "application/json"}, method=method)
    try:
        with urllib.request.urlopen(req) as resp:
            content = resp.read().decode()
            return resp.status, json.loads(content) if content else {}
    except urllib.error.HTTPError as e:
        body = e.read().decode()
        return e.code, body
    except Exception as e:
        return 500, str(e)

# Fetch current agreements, accounts, taxes
_, agreements = api_call("agreements")
_, accounts = api_call("accounts")
_, taxes = api_call("tax-configs")

tax_map = {t["taxType"]: t["taxConfigId"] for t in taxes if isinstance(t, dict)}
acc_map = {f"{a['accountCode']}_{a['accountType']}": a["accountId"] for a in accounts if isinstance(a, dict)}

print("Account Map:", acc_map)
print("Tax Map:", tax_map)

# Delete existing demo agreements to recreate cleanly
for ag in agreements:
    if ag["agreementCode"] in ["AGR-AIRTEL-INTR-2026", "AGR-VODAFONE-ROAM-2026"]:
        st, res = api_call(f"agreements/{ag['agreementId']}", method="DELETE")
        print(f"Deleted old agreement {ag['agreementCode']} (ID: {ag['agreementId']}) -> Status: {st}")

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
        ],
        "taxConfigs": [
            {
                "taxConfigId": vat_tax_id,
                "applyOrder": 1
            }
        ]
    }
]

for ag in new_agreements:
    st, res = api_call("agreements", method="POST", payload=ag)
    if st in [200, 201]:
        print(f"[SUCCESS] Re-created Agreement {ag['agreementCode']} -> ID: {res.get('agreementId')}")
    else:
        print(f"[ERROR] Failed to create Agreement {ag['agreementCode']}: {res}")

# Verify
print("\n--- Final Verification from API ---")
_, agreements_final = api_call("agreements")
for ag in agreements_final:
    if ag["agreementCode"] in ["AGR-AIRTEL-INTR-2026", "AGR-VODAFONE-ROAM-2026"]:
        st, full_ag = api_call(f"agreements/{ag['agreementId']}")
        print(f"Agreement: {full_ag.get('agreementCode')}")
        print(f"  LOB: {full_ag.get('lineOfBusiness')}, Billing: {full_ag.get('billingType')}")
        print(f"  Accounts ({len(full_ag.get('accountAgreements', []))}): {[a.get('accountCode') for a in full_ag.get('accountAgreements', [])]}")
        print(f"  Taxes ({len(full_ag.get('taxConfigs', []))}): {[t.get('taxType') for t in full_ag.get('taxConfigs', [])]}")
