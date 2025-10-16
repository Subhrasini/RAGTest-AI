package com.fortify.fod.ui.test.regression;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.FileDownloadMode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.*;
import com.fortify.fod.common.utils.CSVHelper;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.pages.tenant.reports.dataexport.DataExportPage;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
@Owner("yliben@opentext.com")
@FodBacklogItem("1727004")
@Slf4j
public class ApplicationAuditFilterTest extends FodBaseTest {
    ApplicationDTO dynamicApp, openSourceApp, staticApp;
    File dataExportedFile;
    String auditFilterColumnValue;
    DynamicScanDTO dynamicScanDTO;
    StaticScanDTO staticScanDTO;
    TenantDTO tenantDTO;
    DataExportDTO dataExportDTO;
    String openSourceScanPayload = "payloads/fod/10JavaDefects_Small(OS).zip";
    String staticFpr = "payloads/fod/10JavaDefects_ORIGINAL.fpr";
    String dynamicScanFpr = "payloads/fod/dynamic.zero.fpr";
    List<String> csvColumnsToValidate = new ArrayList<>() {{
        add("Application ID");
        add("Application");
        add("Audit Filter");
    }};
    @MaxRetryCount(1)
    @Severity(SeverityLevel.BLOCKER)
    @Description("Create tenant, applications, scans and global audit setting")
    @Test(groups = {"regression"})
    public void prepareTestData() {
        var sonatypeEntitlement = EntitlementDTO.createDefaultInstance();
        sonatypeEntitlement.setEntitlementType(FodCustomTypes.EntitlementType.SonatypeEntitlement);
        final String ruleId = "6DEAABAF-72E9-4AD6-8903-0EB8E858CB89";
        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setOptionsToEnable(new String[]
                {
                        "Allow scanning with no entitlements",
                        "Enable Source Download",
                        "Enable advanced Audit Template options"
                });

        tenantDTO.setEntitlementDTO(EntitlementDTO.createDefaultInstance());
        TenantActions.createTenant(tenantDTO);
        EntitlementsActions.createEntitlements(sonatypeEntitlement);
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Add global audit filter");
        LogInActions.tamUserLogin(tenantDTO);
        var auditToolsPage = new TenantTopNavbar()
                .openAdministration().openAuditTools();

        var filter = auditToolsPage.setScanType(FodCustomTypes.ScanType.Dynamic).pressAddFilter();
        filter.setCondition(
                FodCustomTypes.FilterOptions.Severity.getTypeValue(),
                FodCustomTypes.FilterOperators.Equals.getTypeValue(),
                FodCustomTypes.Severity.High.getTypeValue());
        filter.setSuppress();
        auditToolsPage.pressSave().pressClose();

        filter = auditToolsPage.setScanType(FodCustomTypes.ScanType.Static).pressAddFilter();
        filter.setCondition(
                "Rule ID",
                FodCustomTypes.FilterOperators.Contains.getTypeValue(),
                ruleId);
        filter.setSuppress();
        auditToolsPage.pressSave().pressClose();

        filter = auditToolsPage.setScanType(FodCustomTypes.ScanType.OpenSource).pressAddFilter();
        filter.setCondition(
                FodCustomTypes.FilterOptions.Severity.getTypeValue(),
                FodCustomTypes.FilterOperators.Equals.getTypeValue(),
                FodCustomTypes.Severity.Critical.getTypeValue());
        filter.setSuppress();
        auditToolsPage.pressSave().pressClose();

        AllureReportUtil.info("create static, dynamic and open source scans");
        dynamicApp = ApplicationDTO.createDefaultInstance();
        dynamicScanDTO = DynamicScanDTO.createDefaultInstance();
        ApplicationActions.createApplication(dynamicApp, tenantDTO, false);
        DynamicScanActions.importDynamicScanTenant(dynamicApp, dynamicScanFpr);

        staticApp = ApplicationDTO.createDefaultInstance();
        ApplicationActions.createApplication(staticApp).tenantTopNavbar
                .openApplications().openDetailsFor(staticApp.getApplicationName());
        StaticScanActions
                .importScanTenant(staticApp, staticFpr)
                .openIssues();

        openSourceApp = ApplicationDTO.createDefaultInstance();
        staticScanDTO = StaticScanDTO.createDefaultInstance();
        staticScanDTO.setAssessmentType("Static Standard");
        staticScanDTO.setLanguageLevel("1.8");
        staticScanDTO.setFileToUpload(openSourceScanPayload);
        staticScanDTO.setOpenSourceComponent(true);
        ApplicationActions.createApplication(openSourceApp, tenantDTO, false);
        StaticScanActions.createStaticScan(staticScanDTO, openSourceApp);
        BrowserUtil.clearCookiesLogOff();
        StaticScanActions.importScanAdmin(openSourceApp, staticFpr, true);
        BrowserUtil.clearCookiesLogOff();
        LogInActions.tamUserLogin(tenantDTO).openYourReleases()
                .openDetailsForRelease(openSourceApp)
                .openScans().getScanByType(FodCustomTypes.ScanType.OpenSource)
                .waitStatus(FodCustomTypes.ScansPageStatus.Completed);
    }
    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify application audit filter column exists, not empty and it is in Json format for dynamic scan")
    @Test(groups = {"regression"},dependsOnMethods = {"prepareTestData"})
    public void validateAuditFilterDynamicScan() {
        var auditToolsPage = LogInActions.tamUserLogin(tenantDTO).openYourApplications()
                .openDetailsFor(dynamicApp.getApplicationName()).openAuditTools().openAuditTemplate()
                .setScanType(FodCustomTypes.ScanType.Dynamic);

        AllureReportUtil.info("Add local audit filter");
        var addAuditFilter = auditToolsPage.pressAddFilter().setCondition(
                FodCustomTypes.FilterOptions.Severity.getTypeValue(),
                FodCustomTypes.FilterOperators.Equals.getTypeValue(),
                FodCustomTypes.Severity.Low.getTypeValue());
        addAuditFilter.setSuppress();
        auditToolsPage.pressSave().pressClose();

        AllureReportUtil.info("verify audit filter column after creating data report");
        verifyAuditFilterColumn(dynamicApp.getApplicationName());
    }
        @MaxRetryCount(1)
        @Severity(SeverityLevel.NORMAL)
        @Description("Verify application audit filter column exists, not empty and it is in Json format for static scan")
        @Test(groups = {"regression"},dependsOnMethods = {"validateAuditFilterDynamicScan"})
        public void validateAuditFilterStaticScan() {
            var auditToolsPage = LogInActions.tamUserLogin(tenantDTO).openYourApplications()
                    .openDetailsFor(staticApp.getApplicationName()).openAuditTools().openAuditTemplate()
                    .setScanType(FodCustomTypes.ScanType.Static);

            AllureReportUtil.info("Add local audit filter");
            var addAuditFilter = auditToolsPage.pressAddFilter().setCondition(
                    FodCustomTypes.FilterOptions.Severity.getTypeValue(),
                    FodCustomTypes.FilterOperators.Equals.getTypeValue(),
                    FodCustomTypes.Severity.High.getTypeValue());
            addAuditFilter.setSuppress();
            auditToolsPage.pressSave().pressClose();

            AllureReportUtil.info("verify audit filter column after creating data report");
            verifyAuditFilterColumn(staticApp.getApplicationName());
    }
    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify application audit filter column exists, not empty and it is in Json format for open source scan")
    @Test(groups = {"regression"},dependsOnMethods = {"validateAuditFilterStaticScan"})
    public void validateAuditFilterOpenSourceScan() {
        var auditToolsPage = LogInActions.tamUserLogin(tenantDTO).openYourApplications()
                .openDetailsFor(openSourceApp.getApplicationName()).openAuditTools().openAuditTemplate()
                .setScanType(FodCustomTypes.ScanType.OpenSource);

        AllureReportUtil.info("Add local audit filter");
        var addAuditFilter = auditToolsPage.pressAddFilter().setCondition(
                FodCustomTypes.FilterOptions.Severity.getTypeValue(),
                FodCustomTypes.FilterOperators.Equals.getTypeValue(),
                FodCustomTypes.Severity.Low.getTypeValue());
        addAuditFilter.setSuppress();
        auditToolsPage.pressSave().pressClose();

        AllureReportUtil.info("verify audit filter column after creating data report");
        verifyAuditFilterColumn(openSourceApp.getApplicationName());
    }
    private void verifyAuditFilterColumn(String columnValue) {
        dataExportDTO = DataExportDTO.createDefaultInstance();
        dataExportDTO.setTemplate(FodCustomTypes.DataExportTemplate.Applications);
        DataExportPage dataExportPage = DataExportActions.createExport(dataExportDTO);
        Configuration.fileDownload = FileDownloadMode.HTTPGET;
        dataExportedFile = dataExportPage.getDataExportByName(dataExportDTO.getExportName()).download(0);

        var dataFromCsvHeader = new CSVHelper(dataExportedFile).getColumnHeaders();
        assertThat(dataFromCsvHeader)
                .as("Validate Audit Filter column exists")
                .contains("Audit Filter");

        var dataFromCsv = new CSVHelper(dataExportedFile).getAllRows(csvColumnsToValidate);
        int itemCount = 0;

        List<List<Object>> matchingRows = new ArrayList<>();

        for (var row : dataFromCsv) {
            if (row[1].equals(columnValue)) {
                matchingRows.add(List.of(row));
                itemCount++;
            }
        }
        AllureReportUtil.info("verify audit filter column after creating data report - json format and not empty");
        List<Object> AuditFilterMatchingRow = matchingRows.get(0);
        auditFilterColumnValue = (String) AuditFilterMatchingRow.get(2);
        assertThat(auditFilterColumnValue)
                .as("audit filter column is not empty").isNotEmpty();
        Assert.assertTrue(isValidJson(auditFilterColumnValue));
    }
    private boolean isValidJson(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.readTree(json);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
