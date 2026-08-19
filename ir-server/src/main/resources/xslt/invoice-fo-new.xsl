<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:fo="http://www.w3.org/1999/XSL/Format">

    <xsl:output method="xml" indent="yes"/>

    <!-- ================= DATE FORMAT ================= -->
    <xsl:template name="formatDate">
        <xsl:param name="date"/>
        <!-- expecting yyyy-MM-dd -->
        <xsl:value-of select="substring($date,9,2)"/>-<xsl:value-of select="substring($date,6,2)"/>-<xsl:value-of select="substring($date,1,4)"/>
    </xsl:template>

    <xsl:template match="/">

        <fo:root>

            <!-- ================= LAYOUT ================= -->
            <fo:layout-master-set>
                <fo:simple-page-master master-name="A4"
                                       page-height="297mm"
                                       page-width="210mm"
                                       margin-top="20mm"
                                       margin-bottom="25mm"
                                       margin-left="20mm"
                                       margin-right="20mm">
                    <fo:region-body margin-bottom="20mm"/>
                    <fo:region-after extent="18mm"/>
                </fo:simple-page-master>
            </fo:layout-master-set>

            <fo:page-sequence master-reference="A4">

                <!-- ================= FOOTER ================= -->
                <fo:static-content flow-name="xsl-region-after">
                    <fo:block border-top="0.5pt solid #e2e8f0"
                              padding-top="4mm"
                              font-size="8pt"
                              color="#64748b"
                              text-align="center">
                        <xsl:value-of select="Invoice/Footer/Text"/>
                        | Page <fo:page-number/>
                        of <fo:page-number-citation-last ref-id="last-page"/>
                    </fo:block>
                </fo:static-content>

                <!-- ================= BODY ================= -->
                <fo:flow flow-name="xsl-region-body"
                         font-family="Helvetica"
                         font-size="9pt"
                         color="#0f172a">

                    <!-- ================= HEADER ================= -->
                    <fo:block border-bottom="0.5pt solid #e2e8f0"
                              padding-bottom="6mm"
                              margin-bottom="8mm">

                        <fo:table width="100%" table-layout="fixed">
                            <fo:table-column column-width="50%"/>
                            <fo:table-column column-width="50%"/>

                            <fo:table-body>
                                <fo:table-row>

                                    <fo:table-cell>
                                        <fo:block font-size="18pt" font-weight="bold">
                                            <xsl:value-of select="Invoice/companyInfo/name"/>
                                            <fo:inline color="#137fec">
                                                <xsl:value-of select="Invoice/companyInfo/nameSuffix"/>
                                            </fo:inline>
                                        </fo:block>

                                        <fo:block margin-top="3mm" font-weight="bold">
                                            <xsl:value-of select="Invoice/companyInfo/legalName"/>
                                        </fo:block>

                                        <fo:block font-size="8pt" color="#64748b" margin-top="2mm">
                                            <xsl:value-of select="Invoice/companyInfo/address"/>
                                        </fo:block>
                                    </fo:table-cell>

                                    <fo:table-cell text-align="right">
                                        <fo:block font-size="22pt" font-weight="bold">
                                            INVOICE
                                        </fo:block>

                                        <fo:block margin-top="4mm">
                                            Invoice #: INV-<xsl:value-of select="Invoice/invoiceId"/>
                                        </fo:block>

                                        <fo:block>
                                            Invoice Date: <xsl:value-of select="Invoice/generatedDate"/>
                                        </fo:block>

                                        <fo:block>
                                            Billing Period:
                                            <xsl:value-of select="Invoice/agreement/billingCycleStart"/>
                                            to
                                            <xsl:value-of select="Invoice/agreement/billingCycleEnd"/>
                                        </fo:block>
                                    </fo:table-cell>

                                </fo:table-row>
                            </fo:table-body>
                        </fo:table>
                    </fo:block>

                    <!-- ================= AGREEMENT + BILL TO ================= -->
                    <fo:block margin-bottom="8mm">

                        <fo:table width="100%" table-layout="fixed">
                            <fo:table-column column-width="50%"/>
                            <fo:table-column column-width="50%"/>

                            <fo:table-body>
                                <fo:table-row>

                                    <fo:table-cell padding="4mm"
                                                   background-color="#f8fafc"
                                                   border="0.5pt solid #e2e8f0">

                                        <fo:block font-size="8pt"
                                                  font-weight="bold"
                                                  color="#64748b"
                                                  margin-bottom="2mm">
                                            AGREEMENT REFERENCE
                                        </fo:block>

                                        <fo:block>
                                            Agreement Code:
                                            <fo:inline font-weight="bold">
                                                <xsl:value-of select="Invoice/agreement/agreementCode"/>
                                            </fo:inline>
                                        </fo:block>

                                        <fo:block>
                                            Settlement Type:
                                            <fo:inline font-weight="bold">
                                                <xsl:value-of select="Invoice/agreement/settlementType"/>
                                            </fo:inline>
                                        </fo:block>

                                        <fo:block font-size="8pt" font-style="italic" color="#64748b">
                                            <xsl:value-of select="Invoice/agreement/description"/>
                                        </fo:block>
                                    </fo:table-cell>

                                    <fo:table-cell padding="4mm">
                                        <fo:block font-size="8pt"
                                                  font-weight="bold"
                                                  color="#64748b"
                                                  margin-bottom="2mm">
                                            BILL TO
                                        </fo:block>

                                        <fo:block font-weight="bold">
                                            <xsl:value-of select="Invoice/billTo/companyName"/>
                                        </fo:block>

                                        <fo:block font-size="8pt" color="#64748b">
                                            Account Code: <xsl:value-of select="Invoice/billTo/accountCode"/>
                                        </fo:block>
                                    </fo:table-cell>

                                </fo:table-row>
                            </fo:table-body>
                        </fo:table>
                    </fo:block>

                    <!-- ================= SERVICES BREAKDOWN ================= -->
                    <fo:block margin-bottom="10mm">

                        <fo:block font-size="10pt" font-weight="bold" margin-bottom="4mm">
                            SERVICES BREAKDOWN
                        </fo:block>

                        <fo:table width="100%" table-layout="fixed" border-collapse="collapse">

                            <fo:table-column column-width="30%"/>
                            <fo:table-column column-width="25%"/>
                            <fo:table-column column-width="20%"/>
                            <fo:table-column column-width="25%"/>

                            <fo:table-header>
                                <fo:table-row background-color="#f1f5f9">
                                    <fo:table-cell padding="3mm" border="0.5pt solid #e2e8f0">
                                        <fo:block font-weight="bold">Service Type</fo:block>
                                    </fo:table-cell>
                                    <fo:table-cell padding="3mm" border="0.5pt solid #e2e8f0">
                                        <fo:block font-weight="bold">Account Code</fo:block>
                                    </fo:table-cell>
                                    <fo:table-cell padding="3mm" border="0.5pt solid #e2e8f0">
                                        <fo:block font-weight="bold">Role</fo:block>
                                    </fo:table-cell>
                                    <fo:table-cell padding="3mm" border="0.5pt solid #e2e8f0" text-align="right">
                                        <fo:block font-weight="bold">Amount</fo:block>
                                    </fo:table-cell>
                                </fo:table-row>
                            </fo:table-header>

                            <fo:table-body>
                                <xsl:choose>
                                    <xsl:when test="count(Invoice/Accounts/Account) > 0">
                                        <xsl:for-each select="Invoice/Accounts/Account">
                                            <fo:table-row>
                                                <fo:table-cell padding="3mm" border="0.5pt solid #e2e8f0">
                                                    <fo:block><xsl:value-of select="serviceType"/></fo:block>
                                                </fo:table-cell>
                                                <fo:table-cell padding="3mm" border="0.5pt solid #e2e8f0">
                                                    <fo:block><xsl:value-of select="accountCode"/></fo:block>
                                                </fo:table-cell>
                                                <fo:table-cell padding="3mm" border="0.5pt solid #e2e8f0">
                                                    <fo:block><xsl:value-of select="accountType"/></fo:block>
                                                </fo:table-cell>
                                                <fo:table-cell padding="3mm" border="0.5pt solid #e2e8f0" text-align="right">
                                                    <fo:block font-weight="bold">
                                                        <xsl:value-of select="../../currency"/>
                                                        <xsl:value-of select="format-number(totalAmount,'#,## 0.0000')"/>
                                                    </fo:block>
                                                </fo:table-cell>
                                            </fo:table-row>
                                        </xsl:for-each>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <fo:table-row>
                                            <fo:table-cell number-columns-spanned="4" padding="3mm" border="0.5pt solid #e2e8f0">
                                                <fo:block text-align="center">No data available</fo:block>
                                            </fo:table-cell>
                                        </fo:table-row>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </fo:table-body>

                        </fo:table>
                    </fo:block>

                    <!-- ================= SETTLEMENT SUMMARY ================= -->
                    <fo:block margin-bottom="8mm">

                        <fo:block font-size="10pt" font-weight="bold" margin-bottom="4mm">
                            SETTLEMENT SUMMARY
                        </fo:block>

                        <fo:table width="100%" table-layout="fixed" border-collapse="collapse">

                            <fo:table-column column-width="25%"/>
                            <fo:table-column column-width="25%"/>
                            <fo:table-column column-width="30%"/>
                            <fo:table-column column-width="20%"/>

                            <fo:table-header>
                                <fo:table-row background-color="#f1f5f9">
                                    <fo:table-cell padding="3mm" border="0.5pt solid #e2e8f0">
                                        <fo:block font-weight="bold">Account Code</fo:block>
                                    </fo:table-cell>
                                    <fo:table-cell padding="3mm" border="0.5pt solid #e2e8f0">
                                        <fo:block font-weight="bold">Role</fo:block>
                                    </fo:table-cell>
                                    <fo:table-cell padding="3mm" border="0.5pt solid #e2e8f0">
                                        <fo:block font-weight="bold">Traffic Direction</fo:block>
                                    </fo:table-cell>
                                    <fo:table-cell padding="3mm" border="0.5pt solid #e2e8f0" text-align="right">
                                        <fo:block font-weight="bold">Total Amount</fo:block>
                                    </fo:table-cell>
                                </fo:table-row>
                            </fo:table-header>

                            <fo:table-body>
                                <xsl:choose>
                                    <xsl:when test="count(Invoice/Summary/SummaryAccount) > 0">
                                        <xsl:for-each select="Invoice/Summary/SummaryAccount">
                                            <fo:table-row>
                                                <fo:table-cell padding="3mm" border="0.5pt solid #e2e8f0">
                                                    <fo:block font-weight="bold">
                                                        <xsl:value-of select="accountCode"/>
                                                    </fo:block>
                                                </fo:table-cell>
                                                <fo:table-cell padding="3mm" border="0.5pt solid #e2e8f0">
                                                    <fo:block>
                                                        <xsl:value-of select="accountType"/>
                                                    </fo:block>
                                                </fo:table-cell>
                                                <fo:table-cell padding="3mm" border="0.5pt solid #e2e8f0">
                                                    <fo:block font-style="italic">
                                                        <xsl:value-of select="trafficDirection"/>
                                                    </fo:block>
                                                </fo:table-cell>
                                                <fo:table-cell padding="3mm" border="0.5pt solid #e2e8f0" text-align="right">
                                                    <fo:block font-weight="bold">
                                                        <xsl:value-of select="../../currency"/>
                                                        <xsl:value-of select="format-number(total,'#,## 0.0000')"/>
                                                    </fo:block>
                                                </fo:table-cell>
                                            </fo:table-row>
                                        </xsl:for-each>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <fo:table-row>
                                            <fo:table-cell number-columns-spanned="4" padding="3mm" border="0.5pt solid #e2e8f0">
                                                <fo:block text-align="center">No data available</fo:block>
                                            </fo:table-cell>
                                        </fo:table-row>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </fo:table-body>

                        </fo:table>
                    </fo:block>

                    <!-- ================= UNIFIED TOTALS & TAX BOX ================= -->
                    <fo:block margin-top="8mm">
                        <fo:table width="100%" table-layout="fixed">
                            <fo:table-column column-width="proportional-column-width(1)"/>
                            <fo:table-column column-width="95mm"/>

                            <fo:table-body>
                                <fo:table-row>
                                    <fo:table-cell>
                                        <fo:block/>
                                    </fo:table-cell>

                                    <fo:table-cell>
                                        <fo:table width="100%" table-layout="fixed" border="1.5pt solid #0f172a" border-collapse="collapse">
                                            <fo:table-column column-width="50mm"/>
                                            <fo:table-column column-width="45mm"/>

                                            <fo:table-body>
                                                <!-- Total Base Charge -->
                                                <fo:table-row>
                                                    <fo:table-cell padding="3mm" border="0.5pt solid #cbd5e1" background-color="#f8fafc">
                                                        <fo:block font-weight="bold" font-size="9pt" color="#334155">Total Base Charge</fo:block>
                                                    </fo:table-cell>
                                                    <fo:table-cell padding="3mm" border="0.5pt solid #cbd5e1" text-align="right">
                                                        <fo:block font-weight="bold" font-size="9pt" color="#0f172a">
                                                            <xsl:value-of select="Invoice/currency"/>
                                                            <xsl:value-of select="format-number(Invoice/netSettlement/netAmount,'#,## 0.0000')"/>
                                                        </fo:block>
                                                    </fo:table-cell>
                                                </fo:table-row>

                                                <!-- Dynamic Tax Lines -->
                                                <xsl:for-each select="Invoice/TaxLineItems/TaxLineItem">
                                                    <fo:table-row>
                                                        <fo:table-cell padding="3mm" border="0.5pt solid #cbd5e1" background-color="#f8fafc">
                                                            <fo:block font-size="9pt" color="#475569">
                                                                <xsl:value-of select="taxType"/>
                                                                <xsl:text> (</xsl:text>
                                                                <xsl:value-of select="format-number(taxRate,'#,##0.00')"/>
                                                                <xsl:text>%)</xsl:text>
                                                            </fo:block>
                                                        </fo:table-cell>
                                                        <fo:table-cell padding="3mm" border="0.5pt solid #cbd5e1" text-align="right">
                                                            <fo:block font-size="9pt" color="#0f172a">
                                                                <xsl:value-of select="../../currency"/>
                                                                <xsl:value-of select="format-number(taxAmount,'#,## 0.0000')"/>
                                                            </fo:block>
                                                        </fo:table-cell>
                                                    </fo:table-row>
                                                </xsl:for-each>

                                                <!-- Total Amount Due -->
                                                <fo:table-row background-color="#0f172a">
                                                    <fo:table-cell padding="4mm" border="0.5pt solid #0f172a">
                                                        <fo:block font-weight="bold" font-size="11pt" color="white">TOTAL AMOUNT DUE</fo:block>
                                                    </fo:table-cell>
                                                    <fo:table-cell padding="4mm" border="0.5pt solid #0f172a" text-align="right">
                                                        <fo:block font-weight="bold" font-size="11pt" color="white">
                                                            <xsl:value-of select="Invoice/currency"/>
                                                            <xsl:value-of select="format-number(Invoice/totalInvoiceAmount,'#,## 0.0000')"/>
                                                        </fo:block>
                                                    </fo:table-cell>
                                                </fo:table-row>

                                                <!-- Net Payable By -->
                                                <fo:table-row>
                                                    <fo:table-cell padding="3mm" border="0.5pt solid #cbd5e1" background-color="#f8fafc">
                                                        <fo:block font-weight="bold" font-size="9pt" color="#475569">Net Payable By</fo:block>
                                                    </fo:table-cell>
                                                    <fo:table-cell padding="3mm" border="0.5pt solid #cbd5e1" text-align="right">
                                                        <fo:block font-weight="bold" font-size="9pt" color="#137fec">
                                                            <xsl:value-of select="Invoice/netSettlement/netPayableBy"/>
                                                        </fo:block>
                                                    </fo:table-cell>
                                                </fo:table-row>
                                            </fo:table-body>
                                        </fo:table>
                                    </fo:table-cell>
                                </fo:table-row>
                            </fo:table-body>
                        </fo:table>
                    </fo:block>

                    <!-- page marker -->
                    <fo:block id="last-page"/>

                </fo:flow>

            </fo:page-sequence>

        </fo:root>

    </xsl:template>
</xsl:stylesheet>
