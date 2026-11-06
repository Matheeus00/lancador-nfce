package com.fritz.lancador.services;

import com.fritz.lancador.enums.Item;
import com.fritz.lancador.models.DataForm;
import javafx.print.PageLayout;
import javafx.print.PrinterJob;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Window;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class ThermalPrinterService {
    private static final DateTimeFormatter DATE_FORMAT =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public PrintResult printDraft(DataForm dataForm, String customer, Window owner) {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            return PrintResult.NO_PRINTER;
        }

        if (!job.showPrintDialog(owner)) {
            job.cancelJob();
            return PrintResult.CANCELLED;
        }

        PageLayout pageLayout = job.getPrinter().getDefaultPageLayout();
        Text receipt = new Text(formatDraft(dataForm, customer));
        receipt.setFont(Font.font("Monospaced", 9));
        receipt.setWrappingWidth(pageLayout.getPrintableWidth());

        if (!job.printPage(pageLayout, receipt)) {
            job.cancelJob();
            return PrintResult.FAILED;
        }

        return job.endJob() ? PrintResult.PRINTED : PrintResult.FAILED;
    }

    private String formatDraft(DataForm dataForm, String customer) {
        Item item = dataForm.itemProperty().get();
        NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        String customerName = customer == null || customer.trim().isEmpty()
            ? "Consumidor final"
            : customer.trim();

        return "PRÉVIA - NÃO FISCAL\n"
            + "NÃO É DOCUMENTO FISCAL\n\n"
            + "Data: " + LocalDateTime.now().format(DATE_FORMAT) + "\n"
            + "Item: " + (item == null ? "Não informado" : item) + "\n"
            + "Cliente: " + customerName + "\n"
            + "Total: " + currency.format(dataForm.valueProperty().get()) + "\n\n";
    }

    public enum PrintResult {
        PRINTED,
        CANCELLED,
        NO_PRINTER,
        FAILED
    }
}
