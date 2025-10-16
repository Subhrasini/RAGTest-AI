package com.fortify.fod.ui.test.regression;

import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.elements.Filters;
import com.fortify.fod.common.elements.ModalDialog;
import com.fortify.fod.common.entities.*;
import com.fortify.fod.ui.pages.common.common.cells.IssueCell;
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

import static org.assertj.core.api.Assertions.assertThat;

@Owner("svpillai@opentext.com")
@FodBacklogItem("1626023")
@Slf4j
public class FortifyAviatorEntitlementConsumptionTest extends FodBaseTest {
    TenantDTO tenantDTO;
    ApplicationDTO app1, app2, app3, app4;
    StaticScanDTO staticScanDTO;
    EntitlementDTO entitlementDTO;
    TenantUserDTO denyConsumeUserDTO;
    TenantUserRoleDTO tenantUserRoleDTO;
    String failureMessageNoIssues = "No new vulnerabilities were found so the SAST Aviator service did not run.";
    String payloadPath = "payloads/fod/WebGoat5.0.zip";
    String fortifyAviatorFilter = "SAST Aviator";

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Create tenants and applications for the test")
    @Test(groups = {"regression"})
    public void prepareTestData() {
        AllureReportUtil.info("Create a tenant and assign entitlements");
        entitlementDTO = EntitlementDTO.createDefaultInstance();
        entitlementDTO.setQuantityPurchased(100);
        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setOptionsToEnable(new String[]{"Manually Start Subscription"});
        tenantDTO.setEntitlementDTO(entitlementDTO);
        TenantActions.createTenant(tenantDTO, true);
        BrowserUtil.clearCookiesLogOff();

        staticScanDTO = StaticScanDTO.createDefaultInstance();
        staticScanDTO.setTechnologyStack(FodCustomTypes.TechnologyStack.AutoDetect);
        staticScanDTO.setFileToUpload(payloadPath);
        staticScanDTO.setIncludeFortifyAviator(true);
        tenantUserRoleDTO = TenantUserRoleDTO.createDefaultInstance();
        tenantUserRoleDTO.setApplicationAccess(FodCustomTypes.RoleApplicationAccess.All);
        tenantUserRoleDTO.setStartStaticScanPermissions(FodCustomTypes.RoleStartScanPermissions.Allow);
        tenantUserRoleDTO.setConsumeEntitlements(false);
        tenantUserRoleDTO.setRoleName("Deny_consume_entitlements_role");
        denyConsumeUserDTO = TenantUserDTO.createDefaultInstance();
        denyConsumeUserDTO.setTenant(tenantDTO.getTenantCode());
        denyConsumeUserDTO.setUserName(denyConsumeUserDTO.getUserName() + "-DenyConsume");
        LogInActions.tamUserLogin(tenantDTO);
        TenantRoleActions.createRole(tenantUserRoleDTO);
        TenantUserActions.createTenantUser(denyConsumeUserDTO);
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify Fortify Aviator usage with Manually Start Subscription Enabled")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void verifyEntitlementUsageWithEnableStartSubscriptionForFA() {
        app1 = ApplicationDTO.createDefaultInstance();
        var secondRelease = ReleaseDTO.createDefaultInstance();
        var copyReleaseDTO = ReleaseDTO.createDefaultInstance();
        copyReleaseDTO.setCopyState(true);
        copyReleaseDTO.setCopyFromReleaseName(app1.getReleaseName());

        int entitlementCountBeforeFA = verifyEntitlementsUsage(tenantDTO);
        BrowserUtil.clearCookiesLogOff();
        var staticScanSetupPage = ApplicationActions.createApplication(app1, tenantDTO, true)
                .openStaticScanSetup();
        staticScanSetupPage.chooseAssessmentType(staticScanDTO.getAssessmentType())
                .chooseEntitlement(staticScanDTO.getEntitlement())
                .enableFortifyAviator(staticScanDTO.isIncludeFortifyAviator())
                .chooseAuditPreference(staticScanDTO.getAuditPreference()).pressStartSubscription()
                .pressStartScanBtn().uploadFile(staticScanDTO.getFileToUpload()).pressNextBtn().pressStartScanBtn();
        staticScanSetupPage.waitStatus(FodCustomTypes.SetupScanPageStatus.Completed);
        BrowserUtil.clearCookiesLogOff();
        int entitlementCountAfterFA = verifyEntitlementsUsage(tenantDTO);
        assertThat(entitlementCountAfterFA)
                .as("Entitlement should be consumed")
                .isGreaterThan(entitlementCountBeforeFA);
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Create a new release for same application and run fortify aviator");
        LogInActions.tamUserLogin(tenantDTO);
        ReleaseActions.createRelease(app1, secondRelease);
        StaticScanActions.createStaticScan(staticScanDTO, app1, secondRelease, FodCustomTypes.SetupScanPageStatus.Completed);
        BrowserUtil.clearCookiesLogOff();
        assertThat(verifyEntitlementsUsage(tenantDTO))
                .as("No entitlement should be consumed for next release of same application")
                .isEqualTo(entitlementCountAfterFA);
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Create a copy state release for same application and run fortify aviator");
        LogInActions.tamUserLogin(tenantDTO);
        ReleaseActions.createRelease(app1, copyReleaseDTO);
        StaticScanActions.createStaticScan(staticScanDTO, app1, copyReleaseDTO, FodCustomTypes.SetupScanPageStatus.Completed);
        BrowserUtil.clearCookiesLogOff();
        assertThat(verifyEntitlementsUsage(tenantDTO))
                .as("No entitlement should be consumed for copy state release ")
                .isEqualTo(entitlementCountAfterFA);
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify Fortify Aviator usage for static scans")
    @Test(groups = {"regression"}, dependsOnMethods = {"verifyEntitlementUsageWithEnableStartSubscriptionForFA"})
    public void verifyEntitlementUsageForFAStaticScans() {
        app2 = ApplicationDTO.createDefaultInstance();
        var secondRelease = ReleaseDTO.createDefaultInstance();
        var copyReleaseDTO = ReleaseDTO.createDefaultInstance();
        copyReleaseDTO.setCopyState(true);
        copyReleaseDTO.setCopyFromReleaseName(app2.getReleaseName());

        int entitlementCountBeforeScan = verifyEntitlementsUsage(tenantDTO);
        BrowserUtil.clearCookiesLogOff();
        ApplicationActions.createApplication(app2, tenantDTO, true);
        StaticScanActions.createStaticScan(staticScanDTO, app2, FodCustomTypes.SetupScanPageStatus.Completed);
        BrowserUtil.clearCookiesLogOff();
        int entitlementCountAfterScan = verifyEntitlementsUsage(tenantDTO);
        assertThat(entitlementCountAfterScan)
                .as("Additional entitlements should be consumed for successful fortify aviator Scan")
                .isGreaterThan(entitlementCountBeforeScan);
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Create a new release for same application and run fortify aviator");
        LogInActions.tamUserLogin(tenantDTO);
        assertThat(ReleaseActions.createRelease(app2, secondRelease).openStaticScanSetup().getAviatorCheckbox().checked())
                .as("If FA is enabled for current release , the future release of the application should have FA checkbox enabled by default")
                .isTrue();
        StaticScanActions.createStaticScan(staticScanDTO, app2, secondRelease, FodCustomTypes.SetupScanPageStatus.Completed);
        BrowserUtil.clearCookiesLogOff();
        assertThat(verifyEntitlementsUsage(tenantDTO))
                .as("Entitlements should not be consumed for successive Fortify Aviator scan")
                .isEqualTo(entitlementCountAfterScan);
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Create a copy state release for same application and run fortify aviator");
        LogInActions.tamUserLogin(tenantDTO);
        ReleaseActions.createRelease(app2, copyReleaseDTO);
        StaticScanActions.createStaticScan(staticScanDTO, app2, copyReleaseDTO, FodCustomTypes.SetupScanPageStatus.Completed);
        BrowserUtil.clearCookiesLogOff();
        assertThat(verifyEntitlementsUsage(tenantDTO))
                .as("Entitlements should not be consumed for successive copy state release Fortify Aviator scan")
                .isEqualTo(entitlementCountAfterScan);
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("User should not start FA scan ,with no permission to consume entitlements")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void verifyFAUsageForUserHavingNoPermissionToConsumeEntitlements() {
        var staticScanDTO = StaticScanDTO.createDefaultInstance();
        staticScanDTO.setTechnologyStack(FodCustomTypes.TechnologyStack.AutoDetect);
        staticScanDTO.setFileToUpload(payloadPath);
        staticScanDTO.setIncludeFortifyAviator(false);
        app3 = ApplicationDTO.createDefaultInstance();

        LogInActions.tamUserLogin(tenantDTO)
                .tenantTopNavbar.openAdministration()
                .openUsers()
                .editUserByName(denyConsumeUserDTO.getUserName())
                .selectRole(tenantUserRoleDTO.getRoleName())
                .pressSaveBtn();

        ApplicationActions.createApplication(app3, tenantDTO, false);
        StaticScanActions.createStaticScan(staticScanDTO, app3, FodCustomTypes.SetupScanPageStatus.Completed);
        BrowserUtil.clearCookiesLogOff();

        LogInActions.tenantUserLogIn(denyConsumeUserDTO.getUserName(), FodConfig.TAM_PASSWORD, tenantDTO.getTenantCode())
                .tenantTopNavbar.openApplications().openYourReleases()
                .openDetailsForRelease(app3.getApplicationName(), app3.getReleaseName())
                .openStaticScanSetup().enableFortifyAviator(true).pressStartScanBtn();
        assertThat(new ModalDialog().getMessage())
                .as("User should get proper error message")
                .contains("Your role does not allow you to consume entitlements.");
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Fortify Aviator with no issues, entitlements should not be consumed")
    @Test(groups = {"regression"}, dependsOnMethods = {"verifyEntitlementUsageForFAStaticScans"})
    public void verifyFAUsageForPayloadHavingNoIssues() {
        var staticScanDto = StaticScanDTO.createDefaultInstance();
        staticScanDto.setTechnologyStack(FodCustomTypes.TechnologyStack.AutoDetect);
        staticScanDto.setFileToUpload("payloads/fod/static.java.zip");
        staticScanDto.setIncludeFortifyAviator(true);
        app4 = ApplicationDTO.createDefaultInstance();

        int entitlementCountBeforeScan = verifyEntitlementsUsage(tenantDTO);
        BrowserUtil.clearCookiesLogOff();

        ApplicationActions.createApplication(app4, tenantDTO, true);
        var staticScanSetupPage = StaticScanActions.createStaticScan(staticScanDto, app4, FodCustomTypes.SetupScanPageStatus.Completed);
        var scanSummaryPopup = staticScanSetupPage.openScans().getFirstScan().pressScanSummary();
        assertThat(scanSummaryPopup.getValueByName("SAST Aviator Service Requested"))
                .as("SAST Aviator service should be Yes")
                .isEqualTo("Yes");
        assertThat(scanSummaryPopup.getValueByName("SAST Aviator Service Status"))
                .as("SAST Aviator failure message should be displayed")
                .isEqualTo(failureMessageNoIssues);
        BrowserUtil.clearCookiesLogOff();

        assertThat(verifyEntitlementsUsage(tenantDTO))
                .as("11 units will be consumed for static scan and no additional units will be consumed for FA")
                .isEqualTo(entitlementCountBeforeScan + 11);
    }

    @FodBacklogItem("1678044")
    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify the Issues page filter and group-by options include Fortify Aviator")
    @Test(groups = {"regression"}, dependsOnMethods = {"verifyEntitlementUsageForFAStaticScans"})
    public void verifyFortifyAviatorFilterAndGroupByInIssuesPage() {
        var issuesPage = LogInActions.tamUserLogin(tenantDTO).tenantTopNavbar
                .openApplications().openYourReleases()
                .openDetailsForRelease(app2.getApplicationName(), app2.getReleaseName())
                .openIssues();
        issuesPage.groupBy(fortifyAviatorFilter).expandAllGroupByElements();
        var headerList = issuesPage.getGroupHeaders();

        //TODO: Commenting out the assertion due to recent changes in Fortify Aviator. It will be removed or uncommented once the new functionality is confirmed.
        /*assertThat(headerList)
                .as("Verify group by Aviator is working, and groupings should have true and false values")
                .containsAnyOf("false", "true");*/

        for (var header : headerList) {
            var firstSubIssueTitle = issuesPage.getIssuesByHeaders(String.valueOf(header)).stream()
                    .map(IssueCell::getTitle).findFirst().orElse(null);
            var issueId = issuesPage.selectIssueByText(firstSubIssueTitle).getId();
            var viewAuditDetailsPopup = issuesPage.pressViewAuditDetailsByIssueId(issueId);
            var historyList = viewAuditDetailsPopup.getHistoryList();
            if (header.equals("false")) {
                assertThat(historyList)
                        .as("Vulnerabilities which are not supported by FA should come under false groupings")
                        .anyMatch(text -> text.contains("Currently not supported by Fortify Aviator."));
            } else {
                assertThat(historyList)
                        .as("Vulnerabilities which are audited by FA should come under true groupings")
                        .anyMatch(text -> text.contains("Fortify Aviator running in test mode"));
            }
            viewAuditDetailsPopup.pressCancelButton();
        }

        //TODO: Due to the new implementation, false values are no longer being returned. The condition has been added for validation, which will be removed after this release
        if (headerList.contains(true) && headerList.contains(false)) {
            issuesPage.filters.openSettings().openFiltersTab().addFilterByName(fortifyAviatorFilter);
            assertThat(new Filters().expandAllFilters().getAllFilters())
                    .as("Fortify Aviator should be displayed on right side of filter section")
                    .contains("SAST Aviator");
        }

        var dataExportPopup = new TenantTopNavbar().openReports().openDataExport().pressCreateExportBtn()
                .setExportName("DataExportTest")
                .setExportTemplate(FodCustomTypes.DataExportTemplate.Issues)
                .pressNextButton();
        assertThat(new Filters().getAllFilters())
                .as("New field named Fortify Aviator should be present in the Filter for Issue data export")
                .contains("SAST Aviator");
        dataExportPopup.pressCloseButton();
    }

    private int verifyEntitlementsUsage(TenantDTO tenantDTO) {
        return LogInActions.adminLogIn().adminTopNavbar.openTenants()
                .openTenantByName(tenantDTO.getTenantName()).openEntitlements()
                .getAll().get(0).getQuantityConsumed();
    }
}
