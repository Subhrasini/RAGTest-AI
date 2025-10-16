package com.fortify.fod.ui.test.regression;

import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.WaitUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.EntitlementDTO;
import com.fortify.fod.common.entities.TenantDTO;
import com.fortify.fod.ui.pages.common.common.cells.ReleaseScanCell;
import com.fortify.fod.ui.pages.tenant.applications.release.popups.ImportOpenSourceScanPopup;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import com.fortify.fod.ui.test.actions.TenantActions;
import com.fortify.fod.ui.test.actions.TenantUserActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static com.codeborne.selenide.Selenide.refresh;
import static org.assertj.core.api.Assertions.assertThat;

@FodBacklogItem("605007")
@Owner("vdubovyk@opentext.com")
@Slf4j
public class ImportCycloneDXDataTest extends FodBaseTest {
    TenantDTO tenantDTO;
    ApplicationDTO applicationDTO;
    List<String> fileNames = Arrays.asList("prodcomplocklicensesvulns.json", "emptyToolName.json",
            "absentToolName.json", "absentTools.json");

    private void init() {
        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setEntitlementDTO(EntitlementDTO.createDefaultInstance());
        //tenantDTO.setOptionsToEnable(new String[]{"Scan Third Party Libraries"});
    }


    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Test data preparation")
    @Test(groups = {"regression"})
    public void prepareTestData() {
        init();

        TenantActions.createTenant(tenantDTO, true);
        TenantUserActions.activateSecLead(tenantDTO, true);
        this.applicationDTO = ApplicationActions.createApplication();
        BrowserUtil.clearCookiesLogOff();
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Checks new CycloneDX endpoint for Swagger")
    @Test(groups = {"regression"}, dependsOnMethods = "prepareTestData")
    public void newCycloneEndpointTest() {
        var swaggerPage = LogInActions
                .tamUserLogin(tenantDTO)
                .tenantTopNavbar
                .openApiSwagger()
                .expandResource("Releases")
                .expandOperation("Releases_ReleasesV3_PutImportScan");

        assertThat(
                swaggerPage
                        .activeResource
                        .getText())
                .as("Resource should contain operation")
                .contains("PUT /api/v3/releases/{releaseId}/open-source-scans/import-cyclonedx-sbom");
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Importing CycloneDX SBOM data")
    @Test(groups = {"regression"}, dependsOnMethods = "prepareTestData")
    public void importCycloneDXDataTest() {
        var releaseScansPage = LogInActions
                .tenantUserLogIn(tenantDTO.getUserName(), FodConfig.ADMIN_PASSWORD, tenantDTO.getTenantCode())
                .tenantTopNavbar
                .openApplications()
                .openYourReleases()
                .openDetailsForRelease(applicationDTO)
                .openScans();

        var cycloneDX = "CycloneDX (Imported)";
        var debricked = "Debricked tool name (Imported)";
        var scanList = releaseScansPage.getAllScans(true);
        var countOfCycloneDXRows = scanList.stream()
                .filter(x -> x.getAssessmentType().equals(cycloneDX)).count();
        var countOfDebrikedRows = scanList.stream()
                .filter(x -> x.getAssessmentType().equals(debricked)).count();

        for (String fileName : fileNames) {
            releaseScansPage
                    .pressImportOpenSourceScan()
                    .uploadFile(String.format("payloads/fod/API/%s", fileName))
                    .pressImportButton();
            refresh();
        }

        WaitUtil.waitForTrue(() -> releaseScansPage.getAllScans(false)
                        .stream()
                        .filter(x -> x.getAssessmentType().equals(cycloneDX))
                        .count() == countOfCycloneDXRows + 3,
                Duration.ofMinutes(5), true);

        var importedScans = releaseScansPage.getAllScans(false);
        assertThat(importedScans
                .stream()
                .filter(x -> x.getAssessmentType().equals(debricked))
                .count())
                .as("Imported 1 Debricked record")
                .isEqualTo(countOfDebrikedRows + 1);

        assertThat(importedScans
                .stream()
                .filter(x -> x.getAssessmentType().equals(cycloneDX))
                .count())
                .as("Imported 3 CycloneDX record")
                .isEqualTo(countOfCycloneDXRows + 3);

        for (var importedScan : importedScans) {
            validate(importedScan);
        }
    }

    private void validate(ReleaseScanCell importedScan) {
        assertThat(importedScan.getScanType())
                .as("Scan Type should be 'Open Source'")
                .contains("Open Source");

        assertThat(importedScan.getTotalCount())
                .as("Total issues count should be more than 0")
                .isPositive();
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Several negative import test")
    @Test(groups = {"regression"}, dependsOnMethods = "prepareTestData")
    public void negativeImportTest() {
        var releaseScansPage = LogInActions
                .tenantUserLogIn(tenantDTO.getUserName(), FodConfig.ADMIN_PASSWORD, tenantDTO.getTenantCode())
                .tenantTopNavbar
                .openApplications()
                .openYourReleases()
                .openDetailsForRelease(applicationDTO)
                .openScans();

        var importOpenSourceScanPopup = new ImportOpenSourceScanPopup();

        var initialScansCount = releaseScansPage
                .getAllScans(true)
                .size();

        AllureReportUtil.info("Payload with more than one tool in the input file");
        releaseScansPage
                .pressImportOpenSourceScan()
                .uploadFile("payloads/fod/API/duplicatedTools.json")
                .pressImportButton();

        assertThat(
                importOpenSourceScanPopup
                        .getImportPopupErrorText())
                .as("Error message should appear")
                .contains("More than one tool was found in the input file. FOD only supports one tool per file.");
        importOpenSourceScanPopup.pressCancel();

        AllureReportUtil.info("Payload with dummy input file");
        releaseScansPage
                .pressImportOpenSourceScan()
                .uploadFile("payloads/fod/API/dummy.json")
                .pressImportButton();
        assertThat(
                importOpenSourceScanPopup
                        .getImportPopupErrorText())
                .as("Error message should appear")
                .contains("Failed to parse the input file. Please validate that it is a valid CycloneDX SBOM");
        importOpenSourceScanPopup.pressCancel();

        AllureReportUtil.info("Validate total count of scans");
        assertThat(
                releaseScansPage
                        .getAllScans(true))
                .as("Imported scans count should be the same as before unsuccessful import")
                .hasSize(initialScansCount);
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Issues Pages Categories Test")
    @Test(groups = {"regression"}, dependsOnMethods = "importCycloneDXDataTest")
    public void issuesPagesCategoriesTest() {
        var issuesMap =
                LogInActions
                        .tenantUserLogIn(tenantDTO.getUserName(), FodConfig.ADMIN_PASSWORD, tenantDTO.getTenantCode())
                        .tenantTopNavbar
                        .openApplications()
                        .openYourReleases()
                        .openDetailsForRelease(applicationDTO)
                        .openIssues()
                        .groupBy("Scan Tool")
                        .getIssueGroupsCounts();

        assertThat(
                issuesMap
                        .get("CycloneDX"))
                .as("Validate issue count")
                .isPositive();

        assertThat(
                issuesMap
                        .get("Debricked tool name"))
                .as("Validate issue count")
                .isPositive();
    }
}