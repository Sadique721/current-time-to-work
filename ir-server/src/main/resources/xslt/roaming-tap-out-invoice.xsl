<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:fo="http://www.w3.org/1999/XSL/Format">

    <xsl:output method="xml" indent="yes"/>

    <!-- =====================================================================
         ROAMING TAP OUT INVOICE — XSL-FO Template
         Root element : <Invoice>  (InvoiceXmlDTO)
         Produced by  : InvoiceService  via  RoamingTapOutXmlConverter
         Rendered by  : PdfGenerationService (Apache FOP)

         XML structure:
           <Invoice>
             <invoiceId/>            — e.g. RMG-OUT-202507-001
             <generatedDate/>        — invoice date
             <currency/>             — e.g. EUR
             <taxCalculationDate/>
             <totalInvoiceAmount/>   — base + tax
             <companyInfo>  name / nameSuffix / legalName / address  </companyInfo>
             <agreement>    agreementCode / description / billingCycleStart /
                            billingCycleEnd / settlementType             </agreement>
             <billTo>       companyName / accountCode                   </billTo>
             <netSettlement>customerTotal / vendorTotal /
                            netAmount / netPayableBy                    </netSettlement>
             <TapFiles>
               <TapFile>
                 <fileName/> <sequenceNo/> <senderTadig/> <recipientTadig/>
                 <generatedAt/> <totalCdrs/> <tapCharge/> <totalOurCharge/>
                 <CdrLines>
                   <CdrLine>
                     <serviceType/>   VOICE | SMS | USAGE
                     <callingNumber/> <calledNumber/> <startTime/>
                     <durationSec/>   (VOICE)
                     <smsCount/>      (SMS)
                     <totalUsage/>    (USAGE)
                     <measurementUnit/> (USAGE)
                     <appliedRate/>
                     <ourCharge/> <currency/>
                   </CdrLine>
                 </CdrLines>
               </TapFile>
             </TapFiles>
             <TaxLineItems>
               <TaxLineItem>
                 <applyOrder/> <taxType/> <taxRate/>
                 <taxableAmount/> <taxAmount/> <applyOn/>
               </TaxLineItem>
             </TaxLineItems>
           </Invoice>
    ====================================================================== -->

    <!-- ── Colour palette ────────────────────────────────────────────────── -->
    <!-- Primary dark navy  : #0f172a   -->
    <!-- Accent blue        : #137fec   -->
    <!-- Mid blue-grey      : #334155   -->
    <!-- Light slate        : #64748b   -->
    <!-- Surface light      : #f8fafc   -->
    <!-- Border             : #e2e8f0   -->
    <!-- Table header bg    : #f1f5f9   -->
    <!-- TAP-OUT badge bg   : #dbeafe   (light blue)  -->
    <!-- TAP-OUT badge fg   : #1e40af   (dark blue)   -->

    <!-- ── Utility: format yyyy-MM-dd as dd-Mon-yyyy ─────────────────────── -->
    <xsl:template name="formatDate">
        <xsl:param name="date"/>
        <xsl:variable name="day"   select="substring($date,9,2)"/>
        <xsl:variable name="mon"   select="substring($date,6,2)"/>
        <xsl:variable name="year"  select="substring($date,1,4)"/>
        <xsl:choose>
            <xsl:when test="$mon='01'"><xsl:value-of select="$day"/>-Jan-<xsl:value-of select="$year"/></xsl:when>
            <xsl:when test="$mon='02'"><xsl:value-of select="$day"/>-Feb-<xsl:value-of select="$year"/></xsl:when>
            <xsl:when test="$mon='03'"><xsl:value-of select="$day"/>-Mar-<xsl:value-of select="$year"/></xsl:when>
            <xsl:when test="$mon='04'"><xsl:value-of select="$day"/>-Apr-<xsl:value-of select="$year"/></xsl:when>
            <xsl:when test="$mon='05'"><xsl:value-of select="$day"/>-May-<xsl:value-of select="$year"/></xsl:when>
            <xsl:when test="$mon='06'"><xsl:value-of select="$day"/>-Jun-<xsl:value-of select="$year"/></xsl:when>
            <xsl:when test="$mon='07'"><xsl:value-of select="$day"/>-Jul-<xsl:value-of select="$year"/></xsl:when>
            <xsl:when test="$mon='08'"><xsl:value-of select="$day"/>-Aug-<xsl:value-of select="$year"/></xsl:when>
            <xsl:when test="$mon='09'"><xsl:value-of select="$day"/>-Sep-<xsl:value-of select="$year"/></xsl:when>
            <xsl:when test="$mon='10'"><xsl:value-of select="$day"/>-Oct-<xsl:value-of select="$year"/></xsl:when>
            <xsl:when test="$mon='11'"><xsl:value-of select="$day"/>-Nov-<xsl:value-of select="$year"/></xsl:when>
            <xsl:when test="$mon='12'"><xsl:value-of select="$day"/>-Dec-<xsl:value-of select="$year"/></xsl:when>
            <xsl:otherwise><xsl:value-of select="$date"/></xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- ── Utility: format datetime yyyy-MM-ddTHH:mm:ss (keep date portion) -->
    <xsl:template name="formatDateTime">
        <xsl:param name="dt"/>
        <xsl:variable name="datePart" select="substring-before($dt,'T')"/>
        <xsl:variable name="timePart" select="substring-before(substring-after($dt,'T'),'+')"/>
        <xsl:choose>
            <xsl:when test="$datePart != ''">
                <xsl:call-template name="formatDate"><xsl:with-param name="date" select="$datePart"/></xsl:call-template>
                <xsl:if test="$timePart != ''">
                    <xsl:text> </xsl:text><xsl:value-of select="$timePart"/>
                </xsl:if>
            </xsl:when>
            <xsl:otherwise><xsl:value-of select="$dt"/></xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- ================================================== -->
    <!--  ROOT TEMPLATE                                     -->
    <!-- ================================================== -->
    <xsl:template match="/">

        <fo:root>

            <!-- ── PAGE LAYOUT ──────────────────────────────────────────── -->
            <fo:layout-master-set>
                <fo:simple-page-master master-name="A4"
                                       page-height="297mm"
                                       page-width="210mm"
                                       margin-top="18mm"
                                       margin-bottom="22mm"
                                       margin-left="18mm"
                                       margin-right="18mm">
                    <fo:region-body   margin-bottom="18mm"/>
                    <fo:region-after  extent="16mm"/>
                </fo:simple-page-master>
            </fo:layout-master-set>

            <!-- ── PAGE SEQUENCE ─────────────────────────────────────────── -->
            <fo:page-sequence master-reference="A4">

                <!-- ── FOOTER ───────────────────────────────────────────── -->
                <fo:static-content flow-name="xsl-region-after">
                    <fo:block border-top="0.5pt solid #e2e8f0"
                              padding-top="3mm"
                              font-family="Helvetica"
                              font-size="8pt"
                              color="#64748b"
                              text-align="center">
                        ROAMING TAP OUT SETTLEMENT INVOICE ·
                        <xsl:value-of select="RoamingTapOutInvoice/invoiceId"/>
                        · Settlement Type: ROAMING_TAP_OUT
                        <fo:leader leader-pattern="space"/>
                        Page <fo:page-number/> of <fo:page-number-citation-last ref-id="tap-out-last-page"/>
                    </fo:block>
                </fo:static-content>

                <!-- ── BODY ─────────────────────────────────────────────── -->
                <fo:flow flow-name="xsl-region-body"
                         font-family="Helvetica"
                         font-size="9pt"
                         color="#0f172a">

                    <!-- ================================================== -->
                    <!--  SECTION 1 — HEADER BANNER                          -->
                    <!-- ================================================== -->
                    <fo:block border-bottom="0.5pt solid #e2e8f0"
                              padding-bottom="6mm"
                              margin-bottom="7mm">

                        <fo:table width="100%" table-layout="fixed">
                            <fo:table-column column-width="55%"/>
                            <fo:table-column column-width="45%"/>
                            <fo:table-body>
                                <fo:table-row>

                                    <!-- Company name / legal name / address -->
                                    <fo:table-cell>
                                        <fo:block font-size="20pt"
                                                  font-weight="bold"
                                                  color="#0f172a">
                                            <xsl:value-of select="RoamingTapOutInvoice/companyInfo/name"/>
                                            <fo:inline color="#137fec">
                                                <xsl:value-of select="RoamingTapOutInvoice/companyInfo/nameSuffix"/>
                                            </fo:inline>
                                        </fo:block>

                                        <fo:block margin-top="2mm"
                                                  font-size="9pt"
                                                  font-weight="bold"
                                                  color="#334155">
                                            <xsl:value-of select="RoamingTapOutInvoice/companyInfo/legalName"/>
                                        </fo:block>

                                        <fo:block font-size="8pt"
                                                  color="#64748b"
                                                  margin-top="1.5mm">
                                            <xsl:value-of select="RoamingTapOutInvoice/companyInfo/address"/>
                                        </fo:block>
                                    </fo:table-cell>

                                    <!-- Invoice title & meta -->
                                    <fo:table-cell text-align="right">
                                        <fo:block font-size="22pt"
                                                  font-weight="bold"
                                                  color="#0f172a">
                                            ROAMING INVOICE
                                        </fo:block>

                                        <!-- TAP OUT badge -->
                                        <fo:block margin-top="2mm">
                                            <fo:inline background-color="#dbeafe"
                                                       color="#1e40af"
                                                       font-size="8pt"
                                                       font-weight="bold"
                                                       padding-left="4pt"
                                                       padding-right="4pt"
                                                       padding-top="2pt"
                                                       padding-bottom="2pt">
                                                TAP OUT — We Bill Partner
                                            </fo:inline>
                                        </fo:block>

                                        <fo:block margin-top="3mm" font-size="9pt">
                                            <fo:inline font-weight="bold">Invoice #: </fo:inline>
                                            <xsl:value-of select="RoamingTapOutInvoice/invoiceId"/>
                                        </fo:block>

                                        <fo:block font-size="9pt">
                                            <fo:inline font-weight="bold">Invoice Date: </fo:inline>
                                            <xsl:call-template name="formatDate">
                                                <xsl:with-param name="date" select="RoamingTapOutInvoice/generatedDate"/>
                                            </xsl:call-template>
                                        </fo:block>

                                        <fo:block font-size="9pt">
                                            <fo:inline font-weight="bold">Currency: </fo:inline>
                                            <xsl:value-of select="RoamingTapOutInvoice/currency"/>
                                        </fo:block>

                                        <fo:block font-size="9pt">
                                            <fo:inline font-weight="bold">Billing Period: </fo:inline>
                                            <xsl:call-template name="formatDate">
                                                <xsl:with-param name="date" select="RoamingTapOutInvoice/agreement/billingCycleStart"/>
                                            </xsl:call-template>
                                            <xsl:text> to </xsl:text>
                                            <xsl:call-template name="formatDate">
                                                <xsl:with-param name="date" select="RoamingTapOutInvoice/agreement/billingCycleEnd"/>
                                            </xsl:call-template>
                                        </fo:block>
                                    </fo:table-cell>

                                </fo:table-row>
                            </fo:table-body>
                        </fo:table>
                    </fo:block>

                    <!-- ================================================== -->
                    <!--  SECTION 2 — AGREEMENT INFO + BILL TO               -->
                    <!-- ================================================== -->
                    <fo:block margin-bottom="7mm">
                        <fo:table width="100%" table-layout="fixed">
                            <fo:table-column column-width="50%"/>
                            <fo:table-column column-width="50%"/>
                            <fo:table-body>
                                <fo:table-row>

                                    <!-- Agreement reference -->
                                    <fo:table-cell padding="4mm"
                                                   background-color="#f8fafc"
                                                   border="0.5pt solid #e2e8f0"
                                                   margin-right="3mm">
                                        <fo:block font-size="8pt"
                                                  font-weight="bold"
                                                  color="#64748b"
                                                  margin-bottom="2mm">
                                            AGREEMENT REFERENCE
                                        </fo:block>

                                        <fo:block>
                                            Agreement Code:
                                            <fo:inline font-weight="bold">
                                                <xsl:value-of select="RoamingTapOutInvoice/agreement/agreementCode"/>
                                            </fo:inline>
                                        </fo:block>

                                        <fo:block>
                                            Settlement Type:
                                            <fo:inline font-weight="bold" color="#137fec">
                                                <xsl:value-of select="RoamingTapOutInvoice/agreement/settlementType"/>
                                            </fo:inline>
                                        </fo:block>

                                        <fo:block font-size="8pt"
                                                  font-style="italic"
                                                  color="#64748b"
                                                  margin-top="1.5mm">
                                            <xsl:value-of select="RoamingTapOutInvoice/agreement/description"/>
                                        </fo:block>
                                    </fo:table-cell>

                                    <!-- Bill To -->
                                    <fo:table-cell padding="4mm"
                                                   border="0.5pt solid #e2e8f0">
                                        <fo:block font-size="8pt"
                                                  font-weight="bold"
                                                  color="#64748b"
                                                  margin-bottom="2mm">
                                            BILL TO (PARTNER / VISITED NETWORK)
                                        </fo:block>

                                        <fo:block font-weight="bold" font-size="10pt">
                                            <xsl:value-of select="RoamingTapOutInvoice/billTo/companyName"/>
                                        </fo:block>

                                        <fo:block font-size="8pt"
                                                  color="#64748b"
                                                  margin-top="1mm">
                                            Partner Code:
                                            <xsl:value-of select="RoamingTapOutInvoice/billTo/accountCode"/>
                                        </fo:block>
                                    </fo:table-cell>

                                </fo:table-row>
                            </fo:table-body>
                        </fo:table>
                    </fo:block>

                    <!-- ================================================== -->
                    <!--  SECTION 3 — TAP FILES (one block per file)          -->
                    <!-- ================================================== -->
                    <fo:block font-size="11pt"
                              font-weight="bold"
                              color="#0f172a"
                              margin-bottom="4mm"
                              border-bottom="1pt solid #137fec"
                              padding-bottom="2mm">
                        TAP OUT FILES — CDR DETAIL
                    </fo:block>

                    <xsl:for-each select="RoamingTapOutInvoice/TapFiles/TapFile">

                        <!-- ── File header card ─────────────────────────── -->
                        <fo:block background-color="#f1f5f9"
                                  border="0.5pt solid #e2e8f0"
                                  padding="4mm"
                                  margin-bottom="3mm"
                                  keep-with-next.within-page="always">

                            <fo:table width="100%" table-layout="fixed">
                                <fo:table-column column-width="25%"/>
                                <fo:table-column column-width="25%"/>
                                <fo:table-column column-width="25%"/>
                                <fo:table-column column-width="25%"/>
                                <fo:table-body>
                                    <fo:table-row>

                                        <fo:table-cell>
                                            <fo:block font-size="8pt" color="#64748b">TAP FILE NAME</fo:block>
                                            <fo:block font-weight="bold" font-size="9pt">
                                                <xsl:value-of select="fileName"/>
                                            </fo:block>
                                        </fo:table-cell>

                                        <fo:table-cell>
                                            <fo:block font-size="8pt" color="#64748b">SENDER / RECIPIENT TADIG</fo:block>
                                            <fo:block font-weight="bold" font-size="9pt">
                                                <xsl:value-of select="senderTadig"/>
                                                <xsl:text> → </xsl:text>
                                                <xsl:value-of select="recipientTadig"/>
                                            </fo:block>
                                        </fo:table-cell>

                                        <fo:table-cell>
                                            <fo:block font-size="8pt" color="#64748b">SEQ NO / TOTAL CDRs</fo:block>
                                            <fo:block font-weight="bold" font-size="9pt">
                                                #<xsl:value-of select="sequenceNo"/>
                                                <xsl:text>  ·  </xsl:text>
                                                <xsl:value-of select="totalCdrs"/> CDRs
                                            </fo:block>
                                        </fo:table-cell>

                                        <fo:table-cell text-align="right">
                                            <fo:block font-size="8pt" color="#64748b">GENERATED AT</fo:block>
                                            <fo:block font-weight="bold" font-size="9pt">
                                                <xsl:call-template name="formatDateTime">
                                                    <xsl:with-param name="dt" select="generatedAt"/>
                                                </xsl:call-template>
                                            </fo:block>
                                        </fo:table-cell>

                                    </fo:table-row>
                                </fo:table-body>
                            </fo:table>

                            <!-- File charge summary row -->
                            <fo:block margin-top="3mm">
                                <fo:table width="100%" table-layout="fixed">
                                    <fo:table-column column-width="50%"/>
                                    <fo:table-column column-width="50%"/>
                                    <fo:table-body>
                                        <fo:table-row>
                                            <fo:table-cell>
                                                <fo:block font-size="8pt" color="#64748b">
                                                    RAW TAP CHARGE (from ASN.1 file)
                                                </fo:block>
                                                <fo:block font-size="9pt">
                                                    <xsl:value-of select="../../currency"/>
                                                    <xsl:text> </xsl:text>
                                                    <xsl:value-of select="format-number(tapCharge,'#,##0.0000')"/>
                                                </fo:block>
                                            </fo:table-cell>
                                            <fo:table-cell text-align="right">
                                                <fo:block font-size="8pt" color="#64748b">
                                                    TOTAL OUR CHARGE (this file)
                                                </fo:block>
                                                <fo:block font-weight="bold"
                                                          font-size="10pt"
                                                          color="#137fec">
                                                    <xsl:value-of select="../../currency"/>
                                                    <xsl:text> </xsl:text>
                                                    <xsl:value-of select="format-number(totalOurCharge,'#,##0.0000')"/>
                                                </fo:block>
                                            </fo:table-cell>
                                        </fo:table-row>
                                    </fo:table-body>
                                </fo:table>
                            </fo:block>
                        </fo:block>

                        <!-- ── CDR Detail Table ──────────────────────────── -->
                        <fo:block margin-bottom="8mm">
                            <fo:table width="100%"
                                      table-layout="fixed"
                                      border-collapse="collapse"
                                      font-size="8pt">

                                <!-- Column widths — total 174mm body width -->
                                <fo:table-column column-width="12%"/>  <!-- Service -->
                                <fo:table-column column-width="18%"/>  <!-- Calling -->
                                <fo:table-column column-width="18%"/>  <!-- Called -->
                                <fo:table-column column-width="15%"/>  <!-- Start Time -->
                                <fo:table-column column-width="13%"/>  <!-- Dur/SMS/Usage -->
                                <fo:table-column column-width="12%"/>  <!-- Applied Rate -->
                                <fo:table-column column-width="12%"/>  <!-- Our Charge -->

                                <!-- Table header -->
                                <fo:table-header>
                                    <fo:table-row background-color="#0f172a">
                                        <fo:table-cell padding="2.5mm"
                                                       border="0.5pt solid #334155">
                                            <fo:block font-weight="bold"
                                                      color="white">Service</fo:block>
                                        </fo:table-cell>
                                        <fo:table-cell padding="2.5mm"
                                                       border="0.5pt solid #334155">
                                            <fo:block font-weight="bold"
                                                      color="white">Calling No.</fo:block>
                                        </fo:table-cell>
                                        <fo:table-cell padding="2.5mm"
                                                       border="0.5pt solid #334155">
                                            <fo:block font-weight="bold"
                                                      color="white">Called No.</fo:block>
                                        </fo:table-cell>
                                        <fo:table-cell padding="2.5mm"
                                                       border="0.5pt solid #334155">
                                            <fo:block font-weight="bold"
                                                      color="white">Start Time</fo:block>
                                        </fo:table-cell>
                                        <fo:table-cell padding="2.5mm"
                                                       border="0.5pt solid #334155">
                                            <fo:block font-weight="bold"
                                                      color="white">Dur/Qty</fo:block>
                                        </fo:table-cell>
                                        <fo:table-cell padding="2.5mm"
                                                       border="0.5pt solid #334155"
                                                       text-align="right">
                                            <fo:block font-weight="bold"
                                                      color="white">Rate</fo:block>
                                        </fo:table-cell>
                                        <fo:table-cell padding="2.5mm"
                                                       border="0.5pt solid #334155"
                                                       text-align="right">
                                            <fo:block font-weight="bold"
                                                      color="white">Our Charge</fo:block>
                                        </fo:table-cell>
                                    </fo:table-row>
                                </fo:table-header>

                                <!-- CDR rows -->
                                <fo:table-body>
                                    <xsl:for-each select="CdrLines/CdrLine">
                                        <xsl:variable name="rowBg">
                                            <xsl:choose>
                                                <xsl:when test="position() mod 2 = 0">#f8fafc</xsl:when>
                                                <xsl:otherwise>white</xsl:otherwise>
                                            </xsl:choose>
                                        </xsl:variable>

                                        <fo:table-row background-color="{$rowBg}">

                                            <!-- Service Type with colour badge -->
                                            <fo:table-cell padding="2.5mm"
                                                           border="0.5pt solid #e2e8f0">
                                                <xsl:choose>
                                                    <xsl:when test="serviceType='VOICE'">
                                                        <fo:block color="#15803d" font-weight="bold">
                                                            <xsl:value-of select="serviceType"/>
                                                        </fo:block>
                                                    </xsl:when>
                                                    <xsl:when test="serviceType='SMS'">
                                                        <fo:block color="#7c3aed" font-weight="bold">
                                                            <xsl:value-of select="serviceType"/>
                                                        </fo:block>
                                                    </xsl:when>
                                                    <xsl:when test="serviceType='USAGE'">
                                                        <fo:block color="#b45309" font-weight="bold">
                                                            <xsl:value-of select="serviceType"/>
                                                        </fo:block>
                                                    </xsl:when>
                                                    <xsl:otherwise>
                                                        <fo:block>
                                                            <xsl:value-of select="serviceType"/>
                                                        </fo:block>
                                                    </xsl:otherwise>
                                                </xsl:choose>
                                            </fo:table-cell>

                                            <!-- Calling Number -->
                                            <fo:table-cell padding="2.5mm"
                                                           border="0.5pt solid #e2e8f0">
                                                <fo:block>
                                                    <xsl:value-of select="callingNumber"/>
                                                </fo:block>
                                            </fo:table-cell>

                                            <!-- Called Number -->
                                            <fo:table-cell padding="2.5mm"
                                                           border="0.5pt solid #e2e8f0">
                                                <fo:block>
                                                    <xsl:value-of select="calledNumber"/>
                                                </fo:block>
                                            </fo:table-cell>

                                            <!-- Start Time -->
                                            <fo:table-cell padding="2.5mm"
                                                           border="0.5pt solid #e2e8f0">
                                                <fo:block>
                                                    <xsl:call-template name="formatDateTime">
                                                        <xsl:with-param name="dt" select="startTime"/>
                                                    </xsl:call-template>
                                                </fo:block>
                                            </fo:table-cell>

                                            <!-- Duration / SMS count / Data usage — service-type conditional -->
                                            <fo:table-cell padding="2.5mm"
                                                           border="0.5pt solid #e2e8f0">
                                                <xsl:choose>
                                                    <xsl:when test="serviceType='VOICE'">
                                                        <fo:block>
                                                            <xsl:value-of select="durationSec"/>s
                                                        </fo:block>
                                                    </xsl:when>
                                                    <xsl:when test="serviceType='SMS'">
                                                        <fo:block>
                                                            <xsl:value-of select="smsCount"/> msg
                                                        </fo:block>
                                                    </xsl:when>
                                                    <xsl:when test="serviceType='USAGE'">
                                                        <fo:block>
                                                            <xsl:value-of select="format-number(totalUsage,'#,##0.000')"/>
                                                            <xsl:text> </xsl:text>
                                                            <xsl:value-of select="measurementUnit"/>
                                                        </fo:block>
                                                    </xsl:when>
                                                    <xsl:otherwise>
                                                        <fo:block>—</fo:block>
                                                    </xsl:otherwise>
                                                </xsl:choose>
                                            </fo:table-cell>

                                            <!-- Applied Rate -->
                                            <fo:table-cell padding="2.5mm"
                                                           border="0.5pt solid #e2e8f0"
                                                           text-align="right">
                                                <fo:block>
                                                    <xsl:choose>
                                                        <xsl:when test="string(appliedRate)">
                                                            <xsl:value-of select="format-number(appliedRate,'#,##0.0000')"/>
                                                        </xsl:when>
                                                        <xsl:otherwise>-</xsl:otherwise>
                                                    </xsl:choose>
                                                </fo:block>
                                            </fo:table-cell>

                                            <!-- Our Charge -->
                                            <fo:table-cell padding="2.5mm"
                                                           border="0.5pt solid #e2e8f0"
                                                           text-align="right">
                                                <fo:block font-weight="bold" color="#0f172a">
                                                    <xsl:value-of select="currency"/>
                                                    <xsl:text> </xsl:text>
                                                    <xsl:value-of select="format-number(ourCharge,'#,##0.0000')"/>
                                                </fo:block>
                                            </fo:table-cell>

                                        </fo:table-row>
                                    </xsl:for-each>
                                </fo:table-body>
                            </fo:table>
                        </fo:block>

                    </xsl:for-each>
                    <!-- end TapFiles loop -->

                    <!-- ================================================== -->
                    <!--  SECTION 4 — OVERALL CHARGE TOTALS                  -->
                    <!-- ================================================== -->
                    <fo:block font-size="11pt"
                              font-weight="bold"
                              color="#0f172a"
                              margin-top="4mm"
                              margin-bottom="4mm"
                              border-bottom="1pt solid #137fec"
                              padding-bottom="2mm">
                        OVERALL CHARGE SUMMARY
                    </fo:block>

                    <fo:block margin-bottom="8mm">
                        <fo:table width="100%" table-layout="fixed"
                                  border-collapse="collapse">
                            <fo:table-column column-width="25%"/>
                            <fo:table-column column-width="25%"/>
                            <fo:table-column column-width="25%"/>
                            <fo:table-column column-width="25%"/>
                            <fo:table-header>
                                <fo:table-row background-color="#f1f5f9">
                                    <fo:table-cell padding="3mm"
                                                   border="0.5pt solid #e2e8f0">
                                        <fo:block font-weight="bold">Description</fo:block>
                                    </fo:table-cell>
                                    <fo:table-cell padding="3mm"
                                                   border="0.5pt solid #e2e8f0">
                                        <fo:block font-weight="bold">Settlement Type</fo:block>
                                    </fo:table-cell>
                                    <fo:table-cell padding="3mm"
                                                   border="0.5pt solid #e2e8f0">
                                        <fo:block font-weight="bold">Payable By</fo:block>
                                    </fo:table-cell>
                                    <fo:table-cell padding="3mm"
                                                   border="0.5pt solid #e2e8f0"
                                                   text-align="right">
                                        <fo:block font-weight="bold">Total Our Charge</fo:block>
                                    </fo:table-cell>
                                </fo:table-row>
                            </fo:table-header>
                            <fo:table-body>
                                <fo:table-row>
                                    <fo:table-cell padding="3mm"
                                                   border="0.5pt solid #e2e8f0">
                                        <fo:block>Outgoing Roaming Charges</fo:block>
                                    </fo:table-cell>
                                    <fo:table-cell padding="3mm"
                                                   border="0.5pt solid #e2e8f0">
                                        <fo:block>
                                            <xsl:value-of select="RoamingTapOutInvoice/agreement/settlementType"/>
                                        </fo:block>
                                    </fo:table-cell>
                                    <fo:table-cell padding="3mm"
                                                   border="0.5pt solid #e2e8f0">
                                        <fo:block font-weight="bold" color="#137fec">
                                            <xsl:value-of select="RoamingTapOutInvoice/netSettlement/netPayableBy"/>
                                        </fo:block>
                                    </fo:table-cell>
                                    <fo:table-cell padding="3mm"
                                                   border="0.5pt solid #e2e8f0"
                                                   text-align="right">
                                        <fo:block font-weight="bold">
                                            <xsl:value-of select="RoamingTapOutInvoice/currency"/>
                                            <xsl:text> </xsl:text>
                                            <xsl:value-of select="format-number(RoamingTapOutInvoice/netSettlement/netAmount,'#,##0.0000')"/>
                                        </fo:block>
                                    </fo:table-cell>
                                </fo:table-row>
                            </fo:table-body>
                        </fo:table>
                    </fo:block>

                    <!-- ================================================== -->
                    <!--  SECTION 5 — TAX DETAIL                             -->
                    <!-- ================================================== -->
                    <xsl:if test="RoamingTapOutInvoice/TaxLineItems/TaxLineItem">
                        <fo:block font-size="11pt"
                                  font-weight="bold"
                                  color="#0f172a"
                                  margin-bottom="4mm"
                                  border-bottom="1pt solid #137fec"
                                  padding-bottom="2mm">
                            TAX DETAIL
                        </fo:block>

                        <fo:block margin-bottom="8mm">
                            <fo:table width="100%" table-layout="fixed"
                                      border-collapse="collapse" font-size="9pt">
                                <fo:table-column column-width="10%"/>  <!-- Order -->
                                <fo:table-column column-width="20%"/>  <!-- Tax Type -->
                                <fo:table-column column-width="12%"/>  <!-- Rate -->
                                <fo:table-column column-width="18%"/>  <!-- Apply On -->
                                <fo:table-column column-width="20%"/>  <!-- Taxable Base -->
                                <fo:table-column column-width="20%"/>  <!-- Tax Amount -->

                                <fo:table-header>
                                    <fo:table-row background-color="#f1f5f9">
                                        <fo:table-cell padding="3mm" border="0.5pt solid #e2e8f0">
                                            <fo:block font-weight="bold">Order</fo:block>
                                        </fo:table-cell>
                                        <fo:table-cell padding="3mm" border="0.5pt solid #e2e8f0">
                                            <fo:block font-weight="bold">Tax Type</fo:block>
                                        </fo:table-cell>
                                        <fo:table-cell padding="3mm" border="0.5pt solid #e2e8f0">
                                            <fo:block font-weight="bold">Rate (%)</fo:block>
                                        </fo:table-cell>
                                        <fo:table-cell padding="3mm" border="0.5pt solid #e2e8f0">
                                            <fo:block font-weight="bold">Applied On</fo:block>
                                        </fo:table-cell>
                                        <fo:table-cell padding="3mm" border="0.5pt solid #e2e8f0"
                                                       text-align="right">
                                            <fo:block font-weight="bold">Taxable Amount</fo:block>
                                        </fo:table-cell>
                                        <fo:table-cell padding="3mm" border="0.5pt solid #e2e8f0"
                                                       text-align="right">
                                            <fo:block font-weight="bold">Tax Amount</fo:block>
                                        </fo:table-cell>
                                    </fo:table-row>
                                </fo:table-header>

                                <fo:table-body>
                                    <xsl:for-each select="RoamingTapOutInvoice/TaxLineItems/TaxLineItem">
                                        <xsl:variable name="rowBg2">
                                            <xsl:choose>
                                                <xsl:when test="position() mod 2 = 0">#f8fafc</xsl:when>
                                                <xsl:otherwise>white</xsl:otherwise>
                                            </xsl:choose>
                                        </xsl:variable>
                                        <fo:table-row background-color="{$rowBg2}">
                                            <fo:table-cell padding="3mm" border="0.5pt solid #e2e8f0">
                                                <fo:block>
                                                    <xsl:value-of select="applyOrder"/>
                                                </fo:block>
                                            </fo:table-cell>
                                            <fo:table-cell padding="3mm" border="0.5pt solid #e2e8f0">
                                                <fo:block font-weight="bold">
                                                    <xsl:value-of select="taxType"/>
                                                </fo:block>
                                            </fo:table-cell>
                                            <fo:table-cell padding="3mm" border="0.5pt solid #e2e8f0">
                                                <fo:block>
                                                    <xsl:value-of select="format-number(taxRate,'#,##0.00')"/>%
                                                </fo:block>
                                            </fo:table-cell>
                                            <fo:table-cell padding="3mm" border="0.5pt solid #e2e8f0">
                                                <fo:block>
                                                    <xsl:value-of select="applyOn"/>
                                                </fo:block>
                                            </fo:table-cell>
                                            <fo:table-cell padding="3mm" border="0.5pt solid #e2e8f0"
                                                           text-align="right">
                                                <fo:block>
                                                    <xsl:value-of select="../../currency"/>
                                                    <xsl:text> </xsl:text>
                                                    <xsl:value-of select="format-number(taxableAmount,'#,##0.0000')"/>
                                                </fo:block>
                                            </fo:table-cell>
                                            <fo:table-cell padding="3mm" border="0.5pt solid #e2e8f0"
                                                           text-align="right">
                                                <fo:block font-weight="bold" color="#0f172a">
                                                    <xsl:value-of select="../../currency"/>
                                                    <xsl:text> </xsl:text>
                                                    <xsl:value-of select="format-number(taxAmount,'#,##0.0000')"/>
                                                </fo:block>
                                            </fo:table-cell>
                                        </fo:table-row>
                                    </xsl:for-each>
                                </fo:table-body>
                            </fo:table>
                        </fo:block>
                    </xsl:if>

                    <!-- ================================================== -->
                    <!--  SECTION 6 — TOTALS BOX (right-aligned)             -->
                    <!-- ================================================== -->
                    <fo:block margin-top="6mm">
                        <fo:table width="100%" table-layout="fixed">
                            <fo:table-column column-width="proportional-column-width(1)"/>
                            <fo:table-column column-width="95mm"/>
                            <fo:table-body>
                                <fo:table-row>
                                    <fo:table-cell><fo:block/></fo:table-cell>

                                    <fo:table-cell>
                                        <fo:table width="100%" table-layout="fixed"
                                                  border="1.5pt solid #0f172a"
                                                  border-collapse="collapse">
                                            <fo:table-column column-width="55mm"/>
                                            <fo:table-column column-width="40mm"/>
                                            <fo:table-body>

                                                <!-- Total Our Charge (base) -->
                                                <fo:table-row>
                                                    <fo:table-cell padding="3mm"
                                                                   border="0.5pt solid #cbd5e1"
                                                                   background-color="#f8fafc">
                                                        <fo:block font-weight="bold"
                                                                  font-size="9pt"
                                                                  color="#334155">
                                                            Total Our Charge
                                                        </fo:block>
                                                    </fo:table-cell>
                                                    <fo:table-cell padding="3mm"
                                                                   border="0.5pt solid #cbd5e1"
                                                                   text-align="right">
                                                        <fo:block font-size="9pt" color="#0f172a">
                                                            <xsl:value-of select="RoamingTapOutInvoice/currency"/>
                                                            <xsl:text> </xsl:text>
                                                            <xsl:value-of select="format-number(RoamingTapOutInvoice/netSettlement/netAmount,'#,##0.0000')"/>
                                                        </fo:block>
                                                    </fo:table-cell>
                                                </fo:table-row>

                                                <!-- Dynamic tax rows -->
                                                <xsl:for-each select="RoamingTapOutInvoice/TaxLineItems/TaxLineItem">
                                                    <fo:table-row>
                                                        <fo:table-cell padding="3mm"
                                                                       border="0.5pt solid #cbd5e1"
                                                                       background-color="#f8fafc">
                                                            <fo:block font-size="9pt" color="#475569">
                                                                <xsl:value-of select="taxType"/>
                                                                <xsl:text> (</xsl:text>
                                                                <xsl:value-of select="format-number(taxRate,'#,##0.00')"/>
                                                                <xsl:text>%)</xsl:text>
                                                            </fo:block>
                                                        </fo:table-cell>
                                                        <fo:table-cell padding="3mm"
                                                                       border="0.5pt solid #cbd5e1"
                                                                       text-align="right">
                                                            <fo:block font-size="9pt" color="#0f172a">
                                                                <xsl:value-of select="../../currency"/>
                                                                <xsl:text> </xsl:text>
                                                                <xsl:value-of select="format-number(taxAmount,'#,##0.0000')"/>
                                                            </fo:block>
                                                        </fo:table-cell>
                                                    </fo:table-row>
                                                </xsl:for-each>

                                                <!-- TOTAL AMOUNT DUE -->
                                                <fo:table-row background-color="#0f172a">
                                                    <fo:table-cell padding="4mm"
                                                                   border="0.5pt solid #0f172a">
                                                        <fo:block font-weight="bold"
                                                                  font-size="11pt"
                                                                  color="white">
                                                            TOTAL AMOUNT DUE
                                                        </fo:block>
                                                    </fo:table-cell>
                                                    <fo:table-cell padding="4mm"
                                                                   border="0.5pt solid #0f172a"
                                                                   text-align="right">
                                                        <fo:block font-weight="bold"
                                                                  font-size="11pt"
                                                                  color="white">
                                                            <xsl:value-of select="RoamingTapOutInvoice/currency"/>
                                                            <xsl:text> </xsl:text>
                                                            <xsl:value-of select="format-number(RoamingTapOutInvoice/totalInvoiceAmount,'#,##0.0000')"/>
                                                        </fo:block>
                                                    </fo:table-cell>
                                                </fo:table-row>

                                                <!-- Net Payable By -->
                                                <fo:table-row>
                                                    <fo:table-cell padding="3mm"
                                                                   border="0.5pt solid #cbd5e1"
                                                                   background-color="#f8fafc">
                                                        <fo:block font-weight="bold"
                                                                  font-size="9pt"
                                                                  color="#475569">
                                                            Net Payable By
                                                        </fo:block>
                                                    </fo:table-cell>
                                                    <fo:table-cell padding="3mm"
                                                                   border="0.5pt solid #cbd5e1"
                                                                   text-align="right">
                                                        <fo:block font-weight="bold"
                                                                  font-size="9pt"
                                                                  color="#137fec">
                                                            <xsl:value-of select="RoamingTapOutInvoice/netSettlement/netPayableBy"/>
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

                    <!-- ── Page-end marker ─────────────────────────────── -->
                    <fo:block id="tap-out-last-page"/>

                </fo:flow>
            </fo:page-sequence>
        </fo:root>

    </xsl:template>

</xsl:stylesheet>
