package com.fortify.fod.ui.test.regression;

import com.codeborne.selenide.Selenide;
import com.fortify.common.custom_types.IssuesCounters;
import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.*;
import com.fortify.fod.ui.pages.tenant.applications.application.ApplicationDetailsPage;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.util.List;

import static com.fortify.fod.ui.pages.tenant.navigation.TenantSideNavTabs.ApplicationDetails.AuditTools;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("oradchenko@opentext.com")
@Slf4j
public class IssueFiltersAppLevelTest extends FodBaseTest {

    String tamUserName;
    TenantDTO tenantDto;
    ApplicationDTO applicationDTO;
    TenantUserDTO secLead;
    TenantUserDTO developer;
    TenantUserDTO leadDeveloper;
    TenantUserDTO reviewer;
    TenantUserDTO executive;
    TenantUserDTO appLead;
    TenantUserDTO[] users;

    @MaxRetryCount(3)
    @Description("Create tenant, app, and all necessary users")
    @Test(groups = {"regression"})
    public void prepareTestData() {

        tenantDto = TenantDTO.createDefaultInstance();
        tamUserName = tenantDto.getAssignedUser();
        tenantDto.setEntitlementDTO(EntitlementDTO.createDefaultInstance());

        applicationDTO = ApplicationDTO.createDefaultInstance();

        secLead = TenantUserDTO.createDefaultInstance();
        secLead.setUserName(tenantDto.getUserName());

        developer = TenantUserDTO.createDefaultInstance();
        developer.setRole(FodCustomTypes.TenantUserRole.Developer);
        developer.setUserName(developer.getUserName() + "-DEVELOPER");
        developer.setTenant(tenantDto.getTenantCode());

        leadDeveloper = TenantUserDTO.createDefaultInstance();
        leadDeveloper.setRole(FodCustomTypes.TenantUserRole.LeadDeveloper);
        leadDeveloper.setUserName(leadDeveloper.getUserName() + "-LEADDEV");
        leadDeveloper.setTenant(tenantDto.getTenantCode());

        reviewer = TenantUserDTO.createDefaultInstance();
        reviewer.setRole(FodCustomTypes.TenantUserRole.Reviewer);
        reviewer.setUserName(reviewer.getUserName() + "-REVIEWER");
        reviewer.setTenant(tenantDto.getTenantCode());

        executive = TenantUserDTO.createDefaultInstance();
        executive.setRole(FodCustomTypes.TenantUserRole.Executive);
        executive.setUserName(executive.getUserName() + "-EXEC");
        executive.setTenant(tenantDto.getTenantCode());

        appLead = TenantUserDTO.createDefaultInstance();
        appLead.setRole(FodCustomTypes.TenantUserRole.ApplicationLead);
        appLead.setUserName(appLead.getUserName() + "-APPLEAD");
        appLead.setTenant(tenantDto.getTenantCode());

        users = new TenantUserDTO[]{developer, leadDeveloper, reviewer, executive, appLead};
        TenantActions.createTenant(tenantDto, true);
        TenantUserActions.createTenantUsers(tenantDto, users);
        ApplicationActions.createApplication(applicationDTO, tenantDto, true);
        BrowserUtil.clearCookiesLogOff();
        TenantUserActions.activateSecLead(tenantDto, true).tenantTopNavbar.openApplications()
                .openDetailsFor(applicationDTO.getApplicationName()).openAccess().pressEditUsersButton()
                .openAvailableTab().setAssignAll(true).pressSave();
    }

    @MaxRetryCount(3)
    @Description("Verify if audit tools is present / not present for every role")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void auditToolsSecurityTest() {
        AllureReportUtil.info("TAM user");
        LogInActions.tamUserLogin(tenantDto).tenantTopNavbar.openApplications()
                .openDetailsFor(applicationDTO.getApplicationName());
        verifyAuditToolsPresent(true);
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Application Lead user");
        LogInActions.tenantUserLogIn(appLead.getUserName(), FodConfig.TAM_PASSWORD, tenantDto.getTenantCode())
                .tenantTopNavbar.openApplications()
                .openDetailsFor(applicationDTO.getApplicationName());
        verifyAuditToolsPresent(true);
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Security Lead user");
        LogInActions.tenantUserLogIn(secLead.getUserName(), FodConfig.TAM_PASSWORD, tenantDto.getTenantCode())
                .tenantTopNavbar.openApplications()
                .openDetailsFor(applicationDTO.getApplicationName());
        verifyAuditToolsPresent(true);
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Developer user");
        LogInActions.tenantUserLogIn(developer.getUserName(), FodConfig.TAM_PASSWORD, tenantDto.getTenantCode())
                .tenantTopNavbar.openApplications()
                .openDetailsFor(applicationDTO.getApplicationName());
        verifyAuditToolsPresent(false);
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Lead Developer user");
        LogInActions.tenantUserLogIn(leadDeveloper.getUserName(), FodConfig.TAM_PASSWORD, tenantDto.getTenantCode())
                .tenantTopNavbar.openApplications()
                .openDetailsFor(applicationDTO.getApplicationName());
        verifyAuditToolsPresent(false);
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Executive user");
        LogInActions.tenantUserLogIn(executive.getUserName(), FodConfig.TAM_PASSWORD, tenantDto.getTenantCode())
                .tenantTopNavbar.openApplications()
                .openDetailsFor(applicationDTO.getApplicationName());
        verifyAuditToolsPresent(false);
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Reviewer user");
        LogInActions.tenantUserLogIn(reviewer.getUserName(), FodConfig.TAM_PASSWORD, tenantDto.getTenantCode())
                .tenantTopNavbar.openApplications()
                .openDetailsFor(applicationDTO.getApplicationName());
        verifyAuditToolsPresent(false);
        BrowserUtil.clearCookiesLogOff();
    }

    @MaxRetryCount(3)
    @Description("Verify all options in filters UI, create filter, static scan, complete and validate suppressed issue")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void auditToolsSetupAndResultsTest() {
        var applicationDTO = ApplicationDTO.createDefaultInstance();
        var appAuditToolsPage = ApplicationActions
                .createApplication(applicationDTO, defaultTenantDTO, true)
                .tenantTopNavbar
                .openApplications().openDetailsFor(applicationDTO.getApplicationName()).openAuditTools();

        var filter = appAuditToolsPage
                .setScanType(FodCustomTypes.ScanType.OpenSource)
                .setScanType(FodCustomTypes.ScanType.Mobile)
                .setScanType(FodCustomTypes.ScanType.Dynamic)
                .setScanType(FodCustomTypes.ScanType.Static)
                .pressAddFilter();

        filter.addConditionRow();
        int rowsCount = filter.conditionRows.size();
        filter.removeConditionRow();
        int rowsCountAfter = filter.conditionRows.size();
        assertThat(rowsCountAfter).as("Verify row is removed").isLessThan(rowsCount);
        var options = List.of(new String[]{
                "Severity", "Rule ID", "Category", "Kingdom", "File", "Package", "Source", "Sink"
        });

        for (var option : options) {
            filter.addConditionRow();
            filter.setCondition(option, FodCustomTypes.FilterOperators.Contains.getTypeValue(), "...");
            filter.removeConditionRow();
        }
        filter.addConditionRow();
        filter.setCondition(
                FodCustomTypes.FilterOptions.Severity.getTypeValue(),
                FodCustomTypes.FilterOperators.Equals.getTypeValue(),
                FodCustomTypes.Severity.Critical.getTypeValue(),
                FodCustomTypes.FilterLogicOperators.And);

        appAuditToolsPage.deleteAllFilters();
        appAuditToolsPage.pressAddFilter().setCondition(
                FodCustomTypes.FilterOptions.Severity.getTypeValue(),
                FodCustomTypes.FilterOperators.DoesNotEqual.getTypeValue(),
                FodCustomTypes.Severity.Low.getTypeValue()
        ).setSuppress();
        appAuditToolsPage.pressSave().pressClose();

        var staticScanDTO = StaticScanDTO.createDefaultInstance();
        staticScanDTO = StaticScanDTO.createDefaultInstance();
        staticScanDTO.setAuditPreference(FodCustomTypes.AuditPreference.Manual);
        staticScanDTO.setTechnologyStack(FodCustomTypes.TechnologyStack.JAVA);
        staticScanDTO.setLanguageLevel("17");
        staticScanDTO.setIncludeThirdParty(false);
        staticScanDTO.setFileToUpload("payloads/fod/demo.zip");
        staticScanDTO.setEntitlement("Single Scan");

        StaticScanActions.createStaticScan(staticScanDTO, applicationDTO);
        BrowserUtil.clearCookiesLogOff();
        StaticScanActions.completeStaticScan(applicationDTO, true);
        BrowserUtil.clearCookiesLogOff();
        var page = LogInActions.tamUserLogin(defaultTenantDTO).openYourReleases()
                .openDetailsForRelease(applicationDTO);
        page.showFixedSuppressedDropdown.setShowSuppressed(false);
        IssuesActions.validateOverviewIssuesTenant(new IssuesCounters(0, 0, 0, 0));
        page.showFixedSuppressedDropdown.setShowSuppressed(true);
        var events = IssuesActions
                .validateOverviewIssuesTenant(new IssuesCounters(0, 1, 0, 0))
                .openIssues().getAllIssues().get(0).openDetails().openHistory().getAllEvents();

        assertThat(events).as("History should have at least 2 records")
                .hasSizeGreaterThanOrEqualTo(2);
        assertThat(events.stream().anyMatch(x -> x.contains("Application Audit Template")))
                .as("There should be event for Application Audit")
                .isTrue();
        assertThat(events.stream().anyMatch(x -> x.contains("Changed Issue from 'Unsuppressed to 'Suppressed'")))
                .as("There should be event about suppression")
                .isTrue();
    }

    private void verifyAuditToolsPresent(boolean shouldBePresent) {
        var page = Selenide.page(ApplicationDetailsPage.class);
        if (shouldBePresent) {
            assertThat(page.sideNavTabExists(AuditTools)).as("Audit tools should be present").isTrue();
            var auditToolsPage = page.openAuditTools();
            assertThat(auditToolsPage.getMainText()).as("Audit Tools Page should be opened")
                    .containsIgnoringCase("Advanced Audit Tools");
        } else {
            assertThat(page.sideNavTabExists(AuditTools)).as("Audit tools should NOT be present").isFalse();
        }
    }
}
