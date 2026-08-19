import { Injectable } from "@angular/core";
import jsPDF from "jspdf";

export interface InvoiceCustomer {
  firstName: string;
  lastName: string;
  mobile: string;
  email?: string;
  address?: string;
  province?: string;
}

export interface InvoicePlan {
  planName: string;
  smsLimit: number;
  emailLimit: number;
  whatsappLimit: number;
  totalPrice: number;
}

export interface InvoiceMeta {
  invoiceNo: string;
  invoiceDate: string;
  type: "Plan Purchase" | "Plan Renewal";
}

export interface CompanyInfo {
  name: string;
  address: string;
  email: string;
  phone: string;
  gstin?: string;
}

const DEFAULT_COMPANY: CompanyInfo = {
  name: "TeleConnect Pvt. Ltd.",
  address: "Plot 42, Tech Park, Sector 18, Gurugram, Haryana - 122015",
  email: "support@teleconnect.in",
  phone: "+91 98765 43210",
  gstin: "09AAACU0481H1ZK",
};

@Injectable({ providedIn: "root" })
export class InvoiceService {
  private readonly PRIMARY = [79, 70, 229] as const;
  private readonly PRIMARY_L = [238, 242, 255] as const;
  private readonly DARK = [30, 41, 59] as const;
  private readonly MID = [71, 85, 105] as const;
  private readonly LIGHT = [248, 250, 255] as const;
  private readonly BORDER = [226, 232, 240] as const;
  private readonly SUCCESS = [22, 163, 74] as const;
  private readonly WHITE = [255, 255, 255] as const;

  downloadPlanPurchaseInvoice(
    customer: InvoiceCustomer,
    plan: InvoicePlan,
    meta?: Partial<InvoiceMeta>,
    company?: Partial<CompanyInfo>,
  ): void {
    const resolvedMeta: InvoiceMeta = {
      invoiceNo: meta?.invoiceNo ?? this.generateInvoiceNo(),
      invoiceDate: meta?.invoiceDate ?? this.today(),
      type: "Plan Purchase",
    };
    this.generate(customer, plan, resolvedMeta, {
      ...DEFAULT_COMPANY,
      ...company,
    });
  }

  downloadRenewalInvoice(
    customer: InvoiceCustomer,
    plan: InvoicePlan,
    meta?: Partial<InvoiceMeta>,
    company?: Partial<CompanyInfo>,
  ): void {
    const resolvedMeta: InvoiceMeta = {
      invoiceNo: meta?.invoiceNo ?? this.generateInvoiceNo(),
      invoiceDate: meta?.invoiceDate ?? this.today(),
      type: "Plan Renewal",
    };
    this.generate(customer, plan, resolvedMeta, {
      ...DEFAULT_COMPANY,
      ...company,
    });
  }

  private generate(
    customer: InvoiceCustomer,
    plan: InvoicePlan,
    meta: InvoiceMeta,
    company: CompanyInfo,
  ): void {
    // ── Use npm import directly — no window/CDN check needed ──
    const doc = new jsPDF({
      unit: "mm",
      format: "a4",
      orientation: "portrait",
    });

    const PW = 210;
    const ML = 15,
      MR = 15,
      CW = PW - ML - MR;

    const total = plan.totalPrice;
    const baseExTax = parseFloat((total / 1.18).toFixed(2));
    const taxAmt = parseFloat((total - baseExTax).toFixed(2));

    let y = 0;

    const setFill = (c: readonly number[]) =>
      doc.setFillColor(c[0], c[1], c[2]);
    const setStroke = (c: readonly number[]) =>
      doc.setDrawColor(c[0], c[1], c[2]);
    const setTxt = (c: readonly number[]) => doc.setTextColor(c[0], c[1], c[2]);
    const fmt = (n: number) =>
      "Rs. " + n.toLocaleString("en-IN", { minimumFractionDigits: 2 });
    const fmtNum = (n: number) => n.toLocaleString("en-IN");

    // ── Header ───────────────────────────────────────────────
    const headerH = 38;
    setFill(this.PRIMARY);
    doc.roundedRect(ML, 10, CW, headerH, 4, 4, "F");

    doc.setFont("helvetica", "bold");
    doc.setFontSize(18);
    setTxt(this.WHITE);
    doc.text(company.name, ML + 6, 24);

    doc.setFont("helvetica", "normal");
    doc.setFontSize(7.5);
    setTxt([199, 210, 254]);
    doc.text(company.address, ML + 6, 30);
    doc.text(`${company.email}  |  ${company.phone}`, ML + 6, 35);

    const badge = meta.type === "Plan Purchase" ? "INVOICE" : "RENEWAL INVOICE";
    doc.setFont("helvetica", "bold");
    doc.setFontSize(9);
    setTxt(this.WHITE);
    doc.text(badge, PW - MR - 6, 22, { align: "right" });

    doc.setFont("helvetica", "normal");
    doc.setFontSize(7.5);
    doc.text(`Invoice No: ${meta.invoiceNo}`, PW - MR - 6, 28, {
      align: "right",
    });
    doc.text(`Date: ${meta.invoiceDate}`, PW - MR - 6, 33, {
      align: "right",
    });
    doc.text(`Type: ${meta.type}`, PW - MR - 6, 38, { align: "right" });

    y = 10 + headerH + 7;

    // ── Info Boxes ───────────────────────────────────────────
    const colW = (CW - 6) / 2;

    const drawInfoBox = (
      x: number,
      bY: number,
      w: number,
      heading: string,
      rows: [string, string][],
    ): number => {
      let boxY = bY;
      const rowH = 5.5;
      const boxH = 10 + rows.length * rowH * 2;

      setFill(this.LIGHT);
      setStroke(this.BORDER);
      doc.setLineWidth(0.3);
      doc.roundedRect(x, boxY, w, boxH, 3, 3, "FD");

      boxY += 6;
      doc.setFont("helvetica", "bold");
      doc.setFontSize(7.5);
      setTxt(this.PRIMARY);
      doc.text(heading, x + 5, boxY);

      boxY += 4;
      for (const [lbl, val] of rows) {
        doc.setFont("helvetica", "bold");
        doc.setFontSize(6.5);
        setTxt(this.MID);
        doc.text(lbl.toUpperCase(), x + 5, boxY);
        boxY += 3.5;
        doc.setFont("helvetica", "normal");
        doc.setFontSize(8);
        setTxt(this.DARK);
        const wrapped = doc.splitTextToSize(val || "—", w - 10);
        doc.text(wrapped, x + 5, boxY);
        boxY += wrapped.length * 4 + 1.5;
      }
      return 10 + rows.length * rowH * 2;
    };

    const custRows: [string, string][] = [
      ["Customer Name", `${customer.firstName} ${customer.lastName}`],
      ["Mobile", customer.mobile || ""],
      ["Email", customer.email || ""],
      ["Address", customer.address || ""],
      ["Province", customer.province || ""],
    ];

    const compRows: [string, string][] = [
      ["GSTIN / Tax ID", company.gstin || ""],
      ["Email", company.email],
      ["Phone", company.phone],
      ["Address", company.address],
    ];

    drawInfoBox(ML, y, colW, "BILL TO", custRows);
    drawInfoBox(ML + colW + 6, y, colW, "COMPANY DETAILS", compRows);

    y += 10 + custRows.length * 5.5 * 2 + 8;

    // ── Plan Details Table ───────────────────────────────────
    doc.setFont("helvetica", "bold");
    doc.setFontSize(7.5);
    setTxt(this.PRIMARY);
    doc.text("PLAN DETAILS", ML, y);
    y += 4;

    const cols = [
      { label: "Description", w: 60, align: "left" },
      { label: "SMS Limit", w: 28, align: "center" },
      { label: "Email Limit", w: 28, align: "center" },
      { label: "WA Limit", w: 28, align: "center" },
      { label: "Amount (₹)", w: 28, align: "right" },
    ];

    const tblW = cols.reduce((s, c) => s + c.w, 0);
    const rowH = 9;

    setFill(this.PRIMARY);
    doc.roundedRect(ML, y, tblW, rowH, 2, 2, "F");

    let cx = ML;
    doc.setFont("helvetica", "bold");
    doc.setFontSize(8);
    setTxt(this.WHITE);
    for (const col of cols) {
      const tx =
        col.align === "right"
          ? cx + col.w - 3
          : col.align === "center"
            ? cx + col.w / 2
            : cx + 3;
      doc.text(col.label, tx, y + 6, { align: col.align as any });
      cx += col.w;
    }
    y += rowH;

    const dataRow = [
      plan.planName,
      fmtNum(plan.smsLimit),
      fmtNum(plan.emailLimit),
      fmtNum(plan.whatsappLimit),
      `Rs. ${baseExTax.toLocaleString("en-IN", { minimumFractionDigits: 2 })}`,
    ];

    setFill(this.LIGHT);
    setStroke(this.BORDER);
    doc.setLineWidth(0.3);
    doc.roundedRect(ML, y, tblW, rowH, 2, 2, "FD");

    cx = ML;
    doc.setFont("helvetica", "normal");
    doc.setFontSize(8.5);
    setTxt(this.DARK);
    for (let i = 0; i < cols.length; i++) {
      const col = cols[i];
      const tx =
        col.align === "right"
          ? cx + col.w - 3
          : col.align === "center"
            ? cx + col.w / 2
            : cx + 3;
      doc.text(dataRow[i], tx, y + 6, { align: col.align as any });
      cx += col.w;
    }
    y += rowH + 5;

    // ── Totals ───────────────────────────────────────────────
    const totalsX = ML + 70;
    const totalsW = tblW - 70;

    const drawTotalRow = (
      label: string,
      value: string,
      bold = false,
      highlight = false,
    ) => {
      if (highlight) {
        setFill(this.PRIMARY_L);
        doc.roundedRect(totalsX, y - 1, totalsW, 8, 2, 2, "F");
      }
      doc.setFont("helvetica", bold ? "bold" : "normal");
      doc.setFontSize(bold ? 10 : 8.5);
      setTxt(bold ? this.PRIMARY : this.DARK);
      doc.text(label, totalsX + totalsW - 35, y + 5, { align: "right" });
      doc.text(value, totalsX + totalsW - 2, y + 5, { align: "right" });
      y += 8;
    };

    setStroke(this.BORDER);
    doc.setLineWidth(0.3);
    doc.line(totalsX, y, totalsX + totalsW, y);
    y += 3;

    drawTotalRow("Subtotal", fmt(baseExTax));
    drawTotalRow("GST (18%)", fmt(taxAmt));

    setStroke(this.PRIMARY);
    doc.setLineWidth(0.5);
    doc.line(totalsX, y - 1, totalsX + totalsW, y - 1);
    drawTotalRow("Total (Inc. Tax)", fmt(total), true, true);
    y += 4;

    // ── Paid stamp ───────────────────────────────────────────
    setFill([240, 253, 244]);
    setStroke(this.SUCCESS);
    doc.setLineWidth(0.8);
    doc.roundedRect(ML, y, tblW, 10, 3, 3, "FD");

    doc.setFont("helvetica", "bold");
    doc.setFontSize(10);
    setTxt(this.SUCCESS);
    doc.text("PAID", ML + tblW / 2, y + 7, { align: "center" });
    y += 18;

    // ── Footer ───────────────────────────────────────────────
    setStroke(this.BORDER);
    doc.setLineWidth(0.3);
    doc.line(ML, y, ML + CW, y);
    y += 5;

    doc.setFont("helvetica", "normal");
    doc.setFontSize(7.5);
    setTxt(this.MID);
    const noteText = `This is a system-generated invoice. For queries contact ${company.email}.`;
    doc.text(doc.splitTextToSize(noteText, CW), ML, y);
    y += 9;

    doc.setFont("helvetica", "bold");
    doc.setFontSize(9);
    setTxt(this.PRIMARY);
    doc.text(`Thank you for choosing ${company.name}!`, ML + CW / 2, y, {
      align: "center",
    });
    y += 7;

    doc.setFont("helvetica", "normal");
    doc.setFontSize(7);
    setTxt(this.MID);
    const year = new Date().getFullYear();
    doc.text(
      `© ${year} ${company.name} · All rights reserved`,
      ML + CW / 2,
      y,
      { align: "center" },
    );

    // ── Save ─────────────────────────────────────────────────
    const prefix =
      meta.type === "Plan Purchase" ? "Invoice" : "Renewal_Invoice";
    doc.save(`${prefix}_${meta.invoiceNo}.pdf`);
  }

  private generateInvoiceNo(): string {
    const year = new Date().getFullYear();
    const seq = Math.floor(Math.random() * 90000) + 10000;
    return `INV-${year}-${seq}`;
  }

  private today(): string {
    return new Date().toLocaleDateString("en-IN", {
      day: "2-digit",
      month: "short",
      year: "numeric",
    });
  }
}
