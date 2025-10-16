package com.fortify.fod.ui.test.regression;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.FileDownloadMode;
import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.*;
import com.fortify.fod.common.utils.CSVHelper;
import com.fortify.fod.ui.pages.tenant.reports.dataexport.DataExportPage;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


public class DataExportEntitlementConsumptionTest extends FodBaseTest {
    ApplicationDTO webApp;
    StaticScanDTO staticScanDTO;
    DataExportDTO dataExportDTO;
    DynamicScanDTO dastScanDTO;

    List<String> csvColumnsToValidate = new ArrayList<>() {{
        add("Scan ID");
        add("Release");
        add("Assessment Type");
        add("EntitlementUnitsConsumed");
    }};

    @FodBacklogItem("1723008")
    @Owner("agoyal3@opentext.com")
    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify data export entitlement consumption on static scan")
    @Test(groups = {"regression"})
    public void staticScanDataExportEntitlementConsumptionTest() {
        webApp = ApplicationDTO.createDefaultInstance();
        ApplicationActions.createApplication(webApp, defaultTenantDTO, true);

        staticScanDTO = StaticScanDTO.createDefaultInstance();
        staticScanDTO.setIncludeFortifyAviator(true);
        staticScanDTO.setTechnologyStack(FodCustomTypes.TechnologyStack.AutoDetect);
        staticScanDTO.setFileToUpload("payloads/fod/WebGoat5.0.zip");

        var scanSetupPage = StaticScanActions.createStaticScan(staticScanDTO, webApp, FodCustomTypes.SetupScanPageStatus.Completed);

        var scanSummaryPage = scanSetupPage.openScans().getScanByType(FodCustomTypes.ScanType.Static).pressScanSummary();
        SoftAssertions softAssertions = new SoftAssertions();

        softAssertions.assertThat(scanSummaryPage.getValueByName("SAST Aviator Service Requested")).as("Validate the value of Fortify Aviator field").isEqualTo("Yes");

        softAssertions.assertThat(scanSummaryPage.getValueByName("SAST Aviator Service Status")).as("Validate the value of Fortify Aviator Service Status field").isEqualTo("Fortify Aviator service has run successfully");

        scanSummaryPage.pressClose();

        AllureReportUtil.info("Verify the assessment type for first static scan with FA enabled in data exported csv report");
        dataExportDTO = DataExportDTO.createDefaultInstance();
        dataExportDTO.setTemplate(FodCustomTypes.DataExportTemplate.EntitlementConsumption);

        DataExportPage dataExportPage = DataExportActions.createExport(dataExportDTO);

        Configuration.fileDownload = FileDownloadMode.HTTPGET;
        File dataExportedFile = dataExportPage.getDataExportByName(dataExportDTO.getExportName()).download(0);

        String columnValue = webApp.getReleaseName();

        var dataFromCsv = new CSVHelper(dataExportedFile).getAllRows(csvColumnsToValidate);
        int itemCount = 0;

        List<List<Object>> matchingRows = new ArrayList<>();

        for (var row : dataFromCsv) {
            if (row[1].equals(columnValue)) {
                matchingRows.add(List.of(row));
                itemCount++;
            }
        }

        List<Object> assessmentTypeMatchingRow1 = matchingRows.get(0);
        List<Object> assessmentTypeMatchingRow2 = matchingRows.get(1);

        String assessmentType1 = (String) assessmentTypeMatchingRow1.get(2);
        String assessmentType2 = (String) assessmentTypeMatchingRow2.get(2);

        assertThat(assessmentType1).as("Verify the assessment type against static scan").isEqualTo("AUTO-STATIC");

        assertThat(assessmentType2).as("Verify the assessment type against static scan with fortify aviator enabled").isEqualTo("SAST Aviator");

        assertThat(itemCount).as("Verify the number of rows in data export report CSV file after first static scan").isEqualTo(2);


        AllureReportUtil.info("To verify the entitlement units consumed in second static scan with FA enabled in data exported csv report");
        StaticScanActions.createStaticScan(staticScanDTO, webApp, FodCustomTypes.SetupScanPageStatus.Completed);

        dataExportPage = DataExportActions.createExport(dataExportDTO);

        Configuration.fileDownload = FileDownloadMode.HTTPGET;
        dataExportedFile = dataExportPage.getDataExportByName(dataExportDTO.getExportName()).download(0);

        dataFromCsv = new CSVHelper(dataExportedFile).getAllRows(csvColumnsToValidate);
        itemCount = 0;

        matchingRows = new ArrayList<>();

        for (var row : dataFromCsv) {
            if (row[1].equals(columnValue)) {
                matchingRows.add(List.of(row));
                itemCount++;
            }
        }

        List<Object> entitlementMatchingRow1 = matchingRows.get(1);
        List<Object> entitlementMatchingRow2 = matchingRows.get(3);

        String entitlementUnitsConsumed1 = (String) entitlementMatchingRow1.get(3);
        String entitlementUnitsConsumed2 = (String) entitlementMatchingRow2.get(3);

        assertThat(entitlementUnitsConsumed1)
                .as("Verify the Entitlement Units Consumed for assessment type as Auto Static")
                .isEqualTo("0");

        assertThat(entitlementUnitsConsumed2)
                .as("Verify the Entitlement Units Consumed for assessment type as Fortify Aviator")
                .isEqualTo("0");

        assertThat(itemCount)
                .as("Verify the number of rows in data export report CSV file after second static scan")
                .isEqualTo(4);

    }

    @FodBacklogItem("1723008")
    @Owner("agoyal3@opentext.com")
    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify data export entitlement consumption on DAST Automated Scan")
    @Test(groups = {"regression"})
    public void dastAutomatedScanDataExportEntitlementConsumptionTest() {
        webApp = ApplicationDTO.createDefaultInstance();

        dastScanDTO = DynamicScanDTO.createDefaultDastAutomatedAPIsInstance();
        dastScanDTO.setIntrospectionType(FodCustomTypes.DynamicScanIntrospectionType.URL_OpenApi);
        dastScanDTO.setIntrospectionUrl("https://petstore3.swagger.io/api/v3/openapi.json");
        dastScanDTO.setWebServiceAPIKey("special-key");
        dastScanDTO.setRequestFalsePositiveRemoval(true);

        ApplicationActions.createApplication(webApp, defaultTenantDTO, true);

        DynamicScanActions.createDynamicScan(dastScanDTO, webApp, FodCustomTypes.SetupScanPageStatus.Scheduled);
        BrowserUtil.clearCookiesLogOff();

        LogInActions.adminLogIn().adminTopNavbar.openDastAutomated().openDetailsFor(webApp.getApplicationName()).waitScanStatus("In Progress - Scan Imported").publishScan();
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Verify the assessment type for first DAST Automated scan with Add-On Services in data exported csv report");
        LogInActions.tamUserLogin(defaultTenantDTO);
        dataExportDTO = DataExportDTO.createDefaultInstance();
        dataExportDTO.setTemplate(FodCustomTypes.DataExportTemplate.EntitlementConsumption);

        DataExportPage dataExportPage = DataExportActions.createExport(dataExportDTO);

        Configuration.fileDownload = FileDownloadMode.HTTPGET;
        File dataExportedFile = dataExportPage.getDataExportByName(dataExportDTO.getExportName()).download(0);

        String columnValue = webApp.getReleaseName();

        var dataFromCsv = new CSVHelper(dataExportedFile).getAllRows(csvColumnsToValidate);
        int itemCount = 0;

        List<List<Object>> matchingRows = new ArrayList<>();

        for (var row : dataFromCsv) {
            if (row[1].equals(columnValue)) {
                matchingRows.add(List.of(row));
                itemCount++;
            }
        }

        List<Object> assessmentTypeMatchingRow1 = matchingRows.get(0);
        List<Object> assessmentTypeMatchingRow2 = matchingRows.get(1);

        String assessmentType1 = (String) assessmentTypeMatchingRow1.get(2);
        String assessmentType2 = (String) assessmentTypeMatchingRow2.get(2);

        assertThat(assessmentType1).as("Verify the assessment type against DAST Automated scan").isEqualTo("DAST Automated");

        assertThat(assessmentType2).as("Verify the assessment type against DAST Automated scan with Add On Services").isEqualTo("DAST Automated Add On Services");

        assertThat(itemCount).as("Verify the number of rows in data export report CSV file after first DAST Automated scan").isEqualTo(2);


        AllureReportUtil.info("Verify the entitlement units consumed in second DAST Automated scan with Add-On Services in data exported csv report");

        DynamicScanActions.createDynamicScan(dastScanDTO, webApp, FodCustomTypes.SetupScanPageStatus.Scheduled);
        BrowserUtil.clearCookiesLogOff();

        LogInActions.adminLogIn().adminTopNavbar.openDastAutomated().openDetailsFor(webApp.getApplicationName()).waitScanStatus("Completed");
        BrowserUtil.clearCookiesLogOff();

        LogInActions.tamUserLogin(defaultTenantDTO);
        dataExportPage = DataExportActions.createExport(dataExportDTO);

        Configuration.fileDownload = FileDownloadMode.HTTPGET;
        dataExportedFile = dataExportPage.getDataExportByName(dataExportDTO.getExportName()).download(0);

        dataFromCsv = new CSVHelper(dataExportedFile).getAllRows(csvColumnsToValidate);
        itemCount = 0;

        matchingRows = new ArrayList<>();

        for (var row : dataFromCsv) {
            if (row[1].equals(columnValue)) {
                matchingRows.add(List.of(row));
                itemCount++;
            }
        }

        List<Object> entitlementMatchingRow1 = matchingRows.get(1);
        List<Object> entitlementMatchingRow2 = matchingRows.get(3);

        String entitlementUnitsConsumed1 = (String) entitlementMatchingRow1.get(3);
        String entitlementUnitsConsumed2 = (String) entitlementMatchingRow2.get(3);

        assertThat(entitlementUnitsConsumed1)
                .as("Verify the EntitlementUnitsConsumed for assessment type as DAST Automated")
                .isEqualTo("0");

        assertThat(entitlementUnitsConsumed2)
                .as("Verify the Entitlement Units Consumed for assessment type as DAST Automated Add On Services")
                .isEqualTo("0");

        assertThat(itemCount)
                .as("Verify the number of rows in data export report CSV file after second DAST Automated scan")
                .isEqualTo(4);
    }
}