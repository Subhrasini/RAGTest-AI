package com.fortify.fod.ui.test.regression;

import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.DataExportDTO;
import com.fortify.fod.common.entities.StaticScanDTO;
import com.fortify.fod.common.utils.CSVHelper;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.DataExportActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import com.fortify.fod.ui.test.actions.StaticScanActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("sbehera3@opentext.com")
@Slf4j
public class DataExportFixValidatedIssuesTest extends FodBaseTest {

    public DataExportDTO dataExportDTO, dataExportDTO_1;
    public ApplicationDTO applicationDTO, applicationDTO_1;
    public StaticScanDTO staticScanDTO, staticScanDTO_1;

    String firstPayload = "payloads/fod/static.java.zip";
    String secondPayload = "payloads/fod/Sonatype vulns with yellow banner_CUT.zip";
    File dataExportedFile = null;

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Start a static scan to get issue with status NEW,Existing,Fixed,Fix Validated")
    @Test(groups = {"hf", "regression"})
    public void testDataPreparation() {

        AllureReportUtil.info("<<< Start a static scan to get issue with status NEW >>>");
        staticScanDTO = StaticScanDTO.createDefaultInstance();
        applicationDTO = ApplicationDTO.createDefaultInstance();
        ApplicationActions.createApplication(applicationDTO, defaultTenantDTO, true);
        staticScanDTO.setFileToUpload(firstPayload);
        staticScanDTO.setAuditPreference(FodCustomTypes.AuditPreference.Manual);
        StaticScanActions.createStaticScan(staticScanDTO, applicationDTO, FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();
        StaticScanActions.completeStaticScan(applicationDTO, true);
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("<<< Start a static scan to get issue with status Existing >>>");
        LogInActions.tamUserLogin(defaultTenantDTO);
        StaticScanActions.createStaticScan(staticScanDTO, applicationDTO, FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();
        StaticScanActions.completeStaticScan(applicationDTO, true);
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("<<< Start a static scan to get issue with status Fixed >>>");
        staticScanDTO.setFileToUpload(secondPayload);
        staticScanDTO.setOpenSourceComponent(true);
        LogInActions.tamUserLogin(defaultTenantDTO);
        StaticScanActions.createStaticScan(staticScanDTO, applicationDTO, FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();
        StaticScanActions.completeStaticScan(applicationDTO, true);
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("<<< Start a static scan to get issue with status Fix Validated >>>");
        staticScanDTO.setFileToUpload(firstPayload);
        staticScanDTO.setOpenSourceComponent(false);
        LogInActions.tamUserLogin(defaultTenantDTO);
        StaticScanActions.createStaticScan(staticScanDTO, applicationDTO, FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();
        StaticScanActions.completeStaticScan(applicationDTO, true);
        BrowserUtil.clearCookiesLogOff();

    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Create a new Data Export of Type Issues")
    @Test(dependsOnMethods = {"testDataPreparation"}, groups = {"hf", "regression"})
    public void createExportTest() {

        LogInActions.tamUserLogin(defaultTenantDTO);
        dataExportDTO = DataExportDTO.createDTOWithTemplate(FodCustomTypes.DataExportTemplate.Issues);
        dataExportDTO.setSelectAllStatuses(true);

        var dataExportCell = DataExportActions.createExport(dataExportDTO)
                .getAllDataExports()
                .stream().filter(x -> x.getName().equals(dataExportDTO.getExportName()))
                .findFirst().orElse(null);

        assertThat(dataExportCell).as("Validate data export is created").isNotNull();
        dataExportedFile = dataExportCell.download(0);

    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("557012")
    @Description("Data Exports  - Scan Started and Completed Date missing for all fix validated Issues")
    @Test(dependsOnMethods = {"createExportTest"}, groups = {"hf", "regression"})
    public void validateDownloadedCSVTest() {

        List<String> csvColumnsToValidate = new ArrayList<>() {{
            add("Status");
            add("Scan started date");
            add("Scan completed date");
        }};

        List<String> statusToValidate = new ArrayList<>() {{
            add("Reopen");
            add("New");
            add("Existing");
            add("Fix Validated");
        }};

        var dataFromCsv = new CSVHelper(dataExportedFile).getAllRows(csvColumnsToValidate);

        assertThat(dataFromCsv.size())
                .as("Validate the downloaded csv file has atleast one row")
                .isPositive();

        for (var row : dataFromCsv) {
            for (int i = 0; i < row.length; i++) {
                if (statusToValidate.contains(row[0])) {
                    assertThat(row[1].trim())
                            .as("Validate scan start date is not blank")
                            .isNotBlank();
                    assertThat(row[2].trim())
                            .as("Validate scan completed date is not blank")
                            .isNotBlank();
                }
            }
        }
    }

    @Owner("svpillai@opentext.com")
    @FodBacklogItem("806012")
    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify cancelling of data export")
    @Test(groups = {"hf", "regression"},
            dependsOnMethods = {"testDataPreparation"})
    public void cancelDataExportTest() {
        LogInActions.tamUserLogin(defaultTenantDTO);
        var dataExportIssuesDto = DataExportDTO.createDefaultInstance();
        dataExportIssuesDto.setTemplate(FodCustomTypes.DataExportTemplate.Issues);
        dataExportIssuesDto.setSelectAllStatuses(true);
        dataExportIssuesDto.setRecurring(true);
        var dataExport = DataExportActions.createExport(dataExportIssuesDto)
                .getDataExportByName(dataExportIssuesDto.getExportName());
        assertThat(dataExport.cancelReport().isReportCancelled(0))
                .as("Data export should be cancelled successfully")
                .isTrue();
    }

    @MaxRetryCount(1)
    @FodBacklogItem("955008")
    @Severity(SeverityLevel.NORMAL)
    @Description("LOC Count, File Count and other Static Scan Summary metrics should be based on scan's FPR, " +
            "not just vulnerabilities")
    @Test(groups = {"hf", "regression"})
    public void validateStaticScanSummaryMetrics() {
        staticScanDTO_1 = StaticScanDTO.createDefaultInstance();
        staticScanDTO_1.setAuditPreference(FodCustomTypes.AuditPreference.Manual);
        applicationDTO_1 = ApplicationDTO.createDefaultInstance();
        ApplicationActions.createApplication(applicationDTO_1, defaultTenantDTO, true);

        StaticScanActions.createStaticScan(staticScanDTO_1, applicationDTO_1, FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();
        StaticScanActions.completeStaticScan(applicationDTO_1, true);
        BrowserUtil.clearCookiesLogOff();

        LogInActions.tamUserLogin(defaultTenantDTO);
        var releaseScanCell = new TenantTopNavbar().openApplications().openYourReleases()
                .openDetailsForRelease(applicationDTO_1.getApplicationName(),
                        applicationDTO_1.getReleaseName()).openScans()
                .getScanByType(FodCustomTypes.ScanType.Static);
        var scanSummaryPopup = releaseScanCell.pressScanSummary();
        var locCount = scanSummaryPopup.getValueByName("Total LOC");
        var fileCount = scanSummaryPopup.getValueByName("File Count");
        scanSummaryPopup.pressClose();

        dataExportDTO_1 = DataExportDTO.createDTOWithTemplate(FodCustomTypes.DataExportTemplate.Scans);
        dataExportDTO_1.setSelectAllStatuses(true);

        var dataExportCell = DataExportActions.createExport(dataExportDTO_1)
                .getAllDataExports()
                .stream().filter(x -> x.getName().equals(dataExportDTO_1.getExportName()))
                .findFirst().orElse(null);

        assertThat(dataExportCell).as("Validate data export is created").isNotNull();
        var exportedFile = dataExportCell.download(0);
        List<String> csvColumnsToValidate = new ArrayList<>() {{
            add("Application");
            add("FileCount");
            add("LOCCount");
        }};
        var dataFromCsv = new CSVHelper(exportedFile).getAllRows(csvColumnsToValidate);

        assertThat(dataFromCsv.size())
                .as("Validate the downloaded csv file has atleast one row")
                .isPositive();
        SoftAssertions softAssertions = new SoftAssertions();
        boolean isValidated = false;
        for (var row : dataFromCsv) {
            for (int i = 0; i < row.length; i++) {
                if (row[0].equals(applicationDTO_1.getApplicationName())) {
                    softAssertions.assertThat(row[1].trim())
                            .as("Validate file count should match both in scan summary and in data export")
                            .isEqualTo(fileCount);
                    softAssertions.assertThat(row[2].trim())
                            .as("Validate LOC count should match both in scan summary and in data export")
                            .isEqualTo(locCount);
                    softAssertions.assertAll();
                    isValidated = true;
                    break;
                }
            }
        }
        assertThat(isValidated)
                .as("Validated the downloaded csv file has the matching application")
                .isTrue();
    }
}
