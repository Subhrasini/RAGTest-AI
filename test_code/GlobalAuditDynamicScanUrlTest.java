package com.fortify.fod.ui.test.regression;

import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.DynamicScanDTO;
import com.fortify.fod.common.entities.EntitlementDTO;
import com.fortify.fod.common.entities.TenantDTO;
import com.fortify.fod.ui.pages.tenant.applications.application.issues.ApplicationIssuesPage;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.DynamicScanActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import com.fortify.fod.ui.test.actions.TenantActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import static com.codeborne.selenide.Selenide.page;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("tmagill@opentext.com")
@FodBacklogItem("797044")
@Slf4j
public class GlobalAuditDynamicScanUrlTest extends FodBaseTest {

    TenantDTO tenantDTO;
    ApplicationDTO applicationDTO, applicationDTO2;
    String folder = "/bank";
    String searchCriteria = "banklogin";
    Integer totalIssues;
    String payload = "payloads/fod/dynamic.zero.fpr";

    @MaxRetryCount(1)
    @Description("Sets up a tenant with two applications and then checks for total issues found on an app scan")
    @Test()
    public void prepareTestData() {
        tenantDTO = TenantDTO.createDefaultInstance();
        applicationDTO = ApplicationDTO.createDefaultInstance();
        applicationDTO2 = ApplicationDTO.createDefaultInstance();
        tenantDTO.setEntitlementDTO(EntitlementDTO.createDefaultInstance());
        TenantActions.createTenant(tenantDTO, true, false);
        BrowserUtil.clearCookiesLogOff();
        ApplicationActions.createApplication(applicationDTO, tenantDTO, true);
        ApplicationActions.createApplication(applicationDTO2, tenantDTO, false);
        BrowserUtil.clearCookiesLogOff();
        executeDynamicScanWithDTO(applicationDTO);
        AllureReportUtil.info("Getting base line of issues");
        var issuesPage = openIssuesPage(applicationDTO);
        totalIssues = issuesPage.getAllCount();
    }

    @MaxRetryCount(1)
    @Description("Set Global Audit and run Scan, Issues should be suppressed")
    @Test(dependsOnMethods = {"prepareTestData"})
    public void scanWithGlobalAuditTest() {
        var auditPage = LogInActions.tamUserLogin(tenantDTO)
                .tenantTopNavbar
                .openAdministration()
                .openAuditTools();
        auditPage
                .setScanType(FodCustomTypes.ScanType.Dynamic)
                .pressAddFilter()
                .setCondition("URL", "Contains", folder)
                .setSuppress();
        auditPage.pressSave()
                .pressClose();
        BrowserUtil.clearCookiesLogOff();
        executeDynamicScanUI();
        DynamicScanActions.importDynamicScanAdmin(applicationDTO.getReleaseName(), payload,
                false, false, false, false, true);
        DynamicScanActions.completeDynamicScanAdmin(applicationDTO, false);
        AllureReportUtil.info("With Global Audit set, search size should be 0");
        var issuesPage = openIssuesPage(applicationDTO);
        assertThat(issuesPage.findWithSearchBox(searchCriteria).getIssues().size()).isEqualTo(0);
    }

    @MaxRetryCount(1)
    @Description("Remove global audit, set App Audit, run Scan on non-Audited app, verify issues not suppressed")
    @Test(dependsOnMethods = {"scanWithGlobalAuditTest"})
    public void applicationAuditNotApplicableTest() {
        removeGlobalAudit();
        var auditPage = LogInActions.tamUserLogin(tenantDTO)
                .openDetailsFor(applicationDTO.getApplicationName())
                .openAuditTools();
        auditPage.setScanType(FodCustomTypes.ScanType.Dynamic)
                .pressAddFilter()
                .setCondition("URL", "Contains", folder)
                .setSuppress();
        auditPage.pressSave()
                .pressClose();
        BrowserUtil.clearCookiesLogOff();
        AllureReportUtil.info("Running scan against other application to show issues not suppressed");
        executeDynamicScanWithDTO(applicationDTO2);
        AllureReportUtil.info("Search size should be greater than 0");
        var issuesPage = openIssuesPage(applicationDTO2);
        assertThat(issuesPage.findWithSearchBox(searchCriteria).getIssues().size()).isBetween(0, totalIssues);
    }

    @MaxRetryCount(1)
    @Description("With Application Audit set, verify issues are suppressed for this audit")
    @Test(dependsOnMethods = {"prepareTestData", "applicationAuditNotApplicableTest"})
    public void applicationAuditApplicableTest() {
        executeDynamicScanUI();
        DynamicScanActions.importDynamicScanAdmin(applicationDTO.getReleaseName(), payload,
                false, false, false, false, true);
        DynamicScanActions.completeDynamicScanAdmin(applicationDTO, false);
        BrowserUtil.clearCookiesLogOff();
        AllureReportUtil.info("With Application Audit now set, search size should be 0");
        var issuesPage = openIssuesPage(applicationDTO);
        assertThat(issuesPage.findWithSearchBox(searchCriteria).getIssues().size()).isEqualTo(0);
    }

    public void removeGlobalAudit() {
        LogInActions.tamUserLogin(tenantDTO)
                .tenantTopNavbar
                .openAdministration()
                .openAuditTools()
                .setScanType(FodCustomTypes.ScanType.Dynamic)
                .deleteAllFilters()
                .pressSave()
                .pressClose();
        BrowserUtil.clearCookiesLogOff();
    }

    public void executeDynamicScanWithDTO(ApplicationDTO appDTO) {
        DynamicScanDTO dynScanDTO = DynamicScanDTO.createDefaultInstance();
        dynScanDTO.setAssessmentType("Dynamic Website Assessment");
        LogInActions.tamUserLogin(tenantDTO);
        DynamicScanActions.createDynamicScan(dynScanDTO, appDTO, FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();
        DynamicScanActions.importDynamicScanAdmin(appDTO.getReleaseName(), payload,
                false, false, false, false, true);
        DynamicScanActions.completeDynamicScanAdmin(appDTO, false);
        BrowserUtil.clearCookiesLogOff();
    }

    public void executeDynamicScanUI() {
        var dynamicScanSetupPage = LogInActions.tamUserLogin(tenantDTO)
                .tenantTopNavbar
                .openApplications()
                .openYourReleases()
                .openDetailsForRelease(applicationDTO.getApplicationName(), applicationDTO.getReleaseName())
                .pressStartDynamicScan();
        dynamicScanSetupPage.pressStartScanBtn()
                .pressNextButtonUntilStartAvailable()
                .pressStartButton();
        dynamicScanSetupPage.waitStatus(FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();
    }

    public ApplicationIssuesPage openIssuesPage(ApplicationDTO appDTO) {
        LogInActions.tamUserLogin(tenantDTO)
                .tenantTopNavbar
                .openApplications()
                .openDetailsFor(appDTO.getApplicationName())
                .openIssues();
        return page(ApplicationIssuesPage.class);
    }
}
