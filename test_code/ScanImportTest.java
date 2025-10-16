package com.fortify.fod.ui.test.regression;

import com.fortify.common.custom_types.IssuesCounters;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.DynamicScanDTO;
import com.fortify.fod.common.entities.EntitlementDTO;
import com.fortify.fod.common.entities.TenantDTO;
import com.fortify.fod.ui.pages.common.common.cells.IssueCell;
import com.fortify.fod.ui.pages.tenant.applications.release.ReleaseDetailsPage;
import com.fortify.fod.ui.pages.tenant.applications.release.ReleaseScansPage;
import com.fortify.fod.ui.pages.tenant.applications.release.issues.ReleaseIssuesPage;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.util.stream.Collectors;

import static com.codeborne.selenide.Selenide.page;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("vdubovyk@opentext.com")
@Slf4j
public class ScanImportTest extends FodBaseTest {

    private ApplicationDTO applicationDTO;
    private DynamicScanDTO dynamicScanDTO;
    private TenantDTO tenantDTO;
    private String dynamicScanForImport;
    private String staticScanForImport;

    private void initTestData() {
        dynamicScanForImport = "payloads/fod/oxwall.scan";

        staticScanForImport = "payloads/fod/static.java.fpr";

        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setEntitlementDTO(EntitlementDTO.createDefaultInstance());

        applicationDTO = ApplicationDTO.createDefaultInstance();

        dynamicScanDTO = DynamicScanDTO.createDefaultInstance();
    }

    @MaxRetryCount(3)
    @Description("Preparing test data")
    @Test(groups = {"regression"})
    public void prepareTestData() {
        initTestData();

        TenantActions.createTenant(tenantDTO);
        BrowserUtil.clearCookiesLogOff();

        ApplicationActions.createApplication(applicationDTO, tenantDTO, true);

        DynamicScanActions.createDynamicScan(dynamicScanDTO, applicationDTO);
        BrowserUtil.clearCookiesLogOff();
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify import of dynamic and static scans")
    @Test(groups = {"regression"}, dependsOnMethods = "prepareTestData")
    public void importScanValidation() {
        log.info("Verify Dynamic Scan");
        IssuesActions.importScanAndValidateDynamicIssuesAdmin(applicationDTO.getReleaseName(), dynamicScanForImport,
                false, false, true,
                new IssuesCounters(0, 1, 92, 29, 505), true);

        DynamicScanActions.completeDynamicScanAdmin(applicationDTO, false);
        BrowserUtil.clearCookiesLogOff();

        DynamicScanActions.validateDynamicScanCompletedTenant(tenantDTO, applicationDTO);

        log.info("Verify Static Scan");
        StaticScanActions.importScanTenant(tenantDTO, applicationDTO, staticScanForImport, false);

        assertThat(page(ReleaseScansPage.class)
                .getAllScans(true)
                .get(0)
                .getAssessmentType())
                .as("Verify assessment type contains 'imported'")
                .containsIgnoringCase("imported");

        log.info("Verify Release Scans page");
        IssuesActions.validateScanIssuesTenant(new IssuesCounters(0, 4, 0, 0, 4));

        log.info("Verify Release Issue page");
        var tenantNavigation =
                new TenantTopNavbar()
                        .openApplications()
                        .openYourReleases()
                        .openDetailsForRelease(applicationDTO);

        tenantNavigation.openIssues();
        var issuesPage = page(ReleaseIssuesPage.class);

        assertThat(issuesPage.getAllCount())
                .as("Issues total count was updated")
                .isEqualTo(509);

        issuesPage.findWithSearchBox("ResourceLeakExample.java");
        assertThat(issuesPage.getIssues()
                .stream()
                .map(IssueCell::getTitle)
                .collect(Collectors.toList()))
                .as("Imported issue is displayed on Release Issues view")
                .contains("ResourceLeakExample.java : 60");

        log.info("Verify Release Details page");
        var releaseDetailsPage = page(ReleaseDetailsPage.class);
        tenantNavigation.openOverview();

        IssuesActions.validateOverviewIssuesTenant(new IssuesCounters(0, 5, 92, 29, 505));

        assertThat(releaseDetailsPage.getStaticScanStatus())
                .as("Status should be 'Completed'")
                .isEqualTo("Completed");

        assertThat(releaseDetailsPage.getDynamicScanStatus())
                .as("Status should be 'Completed'")
                .isEqualTo("Completed");

        log.info("Verify Release Scans and Release Details pages after removing imported static scan");
        StaticScanActions.cancelScanTenant(applicationDTO);

        tenantNavigation
                .tenantTopNavbar
                .openApplications()
                .openYourReleases()
                .openDetailsForRelease(applicationDTO)
                .openOverview();

        IssuesActions.validateOverviewIssuesTenant(new IssuesCounters(0, 1, 92, 29, 505));
    }

    @Owner("oradchenko@opentext.com")
    @FodBacklogItem("335024")
    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Code section should be populated after importing scan and navigating to Code tab in Release Issues")
    @Test(groups = {"hf", "regression"})
    public void releaseIssuesCodeTabTest() {
        var tenant = defaultTenantDTO;
        var app = ApplicationDTO.createDefaultInstance();
        LogInActions.tamUserLogin(tenant).openYourApplications();
        ApplicationActions.createApplication(app).openScans();
        var page = TenantScanActions.importScanTenant(app, "payloads/fod/646163_scandata.fpr", FodCustomTypes.ScanType.Static)
                .openIssues().waitTillAllCountGreaterThanOrEqual(200).clickCritical();
        var codeCell = page.getIssues().get(0).openDetails().openCode();
        assertThat(codeCell.isCodeSectionVisible()).as("Code section should be populated").isTrue();
        assertThat(page.getMainText()).as("Error message should not be displayed")
                .doesNotContain("(The source code associated with this issue was not included in the scan submission.)");
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate Dynamic scan import functionality on admin site")
    @Test(groups = {"regression"})
    public void importDynamicScanAdminTest() {
        var dynamicScanDTO = DynamicScanDTO.createDefaultInstance();
        dynamicScanDTO.setAssessmentType("AUTO-DYNAMIC");

        var webApplicationDTO = ApplicationDTO.createDefaultInstance();
        LogInActions.tamUserLogin(defaultTenantDTO);
        ApplicationActions.createApplication(webApplicationDTO, defaultTenantDTO, false);

        DynamicScanActions.createDynamicScan(dynamicScanDTO, webApplicationDTO);
        BrowserUtil.clearCookiesLogOff();

        var scan1FileName = "payloads/fod/dynamic.zero.fpr";
        var scan2FileName = "payloads/fod/oxwall.scan";
        var scan1CriticalCount = 1;
        var scan1HighCount = 3;
        var scan1MediumCount = 4;
        var scan1LowCount = 5;
        var scan1AllCount = 25;
        var scan2CriticalCount = 1;
        var scan2HighCount = 4;
        var scan2MediumCount = 96;
        var scan2LowCount = 34;
        var scan2AllCount = 530;

        IssuesActions.importScanAndValidateDynamicIssuesAdmin(
                webApplicationDTO.getApplicationName(),
                scan1FileName,
                false,
                true,
                false,
                new IssuesCounters(scan1CriticalCount, scan1HighCount, scan1MediumCount, scan1LowCount, scan1AllCount),
                true);

        IssuesActions.importScanAndValidateDynamicIssuesAdmin(
                webApplicationDTO.getApplicationName(),
                scan2FileName,
                true,
                true,
                true,
                new IssuesCounters(scan2CriticalCount, scan2HighCount, scan2MediumCount, scan2LowCount, scan2AllCount),
                false);

        DynamicScanActions.importDynamicScanAdmin(
                webApplicationDTO.getApplicationName(),
                scan1FileName,
                false,
                true,
                false,
                false,
                false,
                true);
        IssuesActions.validateIssuesAdmin(new IssuesCounters(scan2CriticalCount, scan2HighCount, scan2MediumCount, scan2LowCount, scan2AllCount));

        DynamicScanActions.completeDynamicScanAdmin(webApplicationDTO, false);
        BrowserUtil.clearCookiesLogOff();

        log.info("Check from tenant site");
        DynamicScanActions.validateDynamicScanCompletedTenant(defaultTenantDTO, webApplicationDTO);
    }
}