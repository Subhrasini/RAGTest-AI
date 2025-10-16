package com.fortify.fod.ui.test.regression;

import com.codeborne.selenide.Condition;
import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.elements.ModalDialog;
import com.fortify.fod.common.entities.*;
import com.fortify.fod.exceptions.FodElementNotCreatedException;
import com.fortify.fod.exceptions.FodUnExpectedScanStatusException;
import com.fortify.fod.ui.pages.admin.administration.tenants.tenant.entitlements.EntitlementCell;
import com.fortify.fod.ui.pages.admin.navigation.AdminTopNavbar;
import com.fortify.fod.ui.pages.tenant.applications.release.popups.StartOpenSourceScanPopup;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("oradchenko@opentext.com")
@Slf4j
public class EntitlementsTest extends FodBaseTest {

    @MaxRetryCount(1)
    @FodBacklogItem("688003")
    @Severity(SeverityLevel.NORMAL)
    @Description("Create tenant, entitlement, create and complete static, dynamic and mobile scans, verify entitlement usage, repeat with remediation scans, entitlement usage should not change, disable entitlements and verify")
    @Test(groups = {"regression"}, priority = 1)
    public void entitlementsTest1() {
        TenantDTO tenantDTO;
        EntitlementDTO entitlementDTO;
        ApplicationDTO applicationDTO;
        ApplicationDTO mobileApplicationDTO;
        DynamicScanDTO dynamicScan;
        StaticScanDTO staticScanDTO;
        MobileScanDTO mobileScanDTO;

        entitlementDTO = EntitlementDTO.createDefaultInstance();
        entitlementDTO.setQuantityPurchased(200);

        var sonatypeEntitlement = EntitlementDTO.createDefaultInstance();
        sonatypeEntitlement.setEntitlementType(FodCustomTypes.EntitlementType.SonatypeEntitlement);
        sonatypeEntitlement.setQuantityPurchased(100);

        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setEntitlementDTO(entitlementDTO);
        tenantDTO.setEntitlementModel(FodCustomTypes.EntitlementModel.Units);
        tenantDTO.setSubscriptionModel(FodCustomTypes.SubscriptionModel.Period);
        tenantDTO.setOptionsToEnable(new String[]{"Allow scanning with no entitlements"});
        applicationDTO = ApplicationDTO.createDefaultInstance();
        mobileApplicationDTO = ApplicationDTO.createDefaultMobileInstance();
        staticScanDTO = StaticScanDTO.createDefaultInstance();
        staticScanDTO.setAuditPreference(FodCustomTypes.AuditPreference.Manual);
        staticScanDTO.setTechnologyStack(FodCustomTypes.TechnologyStack.DotNet);
        staticScanDTO.setLanguageLevel("3.5");
        staticScanDTO.setIncludeThirdParty(false);
        staticScanDTO.setOpenSourceComponent(true);
        staticScanDTO.setFileToUpload("payloads/fod/PerfStuffExtractor.zip");
        staticScanDTO.setEntitlement("Single Scan");

        dynamicScan = DynamicScanDTO.createDefaultInstance();
        dynamicScan.setStartInFuture(true);
        dynamicScan.setEntitlement("Single Scan");

        mobileScanDTO = MobileScanDTO.createDefaultScanInstance();
        mobileScanDTO.setEntitlement("Single Scan");
        mobileScanDTO.setAssessmentType("AUTO-MOBILE");

        TenantActions.createTenant(tenantDTO);

        EntitlementsActions.createEntitlements(tenantDTO, false, sonatypeEntitlement);
        BrowserUtil.clearCookiesLogOff();
        LogInActions.tamUserLogin(tenantDTO);
        ApplicationActions.createApplication(applicationDTO);
        ApplicationActions.createApplication(mobileApplicationDTO);
        BrowserUtil.clearCookiesLogOff();
        verifySonatypeEntitlementUsageAdmin(tenantDTO, 100, 0, true);
        BrowserUtil.clearCookiesLogOff();
        DashboardActions.verifyEntitlementUsageTenant(tenantDTO, 200, 0, true);

        AllureReportUtil.info("Create and complete scans, verify entitlement usage");
        StaticScanActions.createStaticScan(staticScanDTO, applicationDTO);
        DynamicScanActions.createDynamicScan(dynamicScan, applicationDTO, FodCustomTypes.SetupScanPageStatus.Scheduled);
        MobileScanActions.createMobileScan(mobileScanDTO, mobileApplicationDTO);

        BrowserUtil.clearCookiesLogOff();
        StaticScanActions.completeStaticScan(applicationDTO, true);
        DynamicScanActions.completeDynamicScanAdmin(applicationDTO, false);
        MobileScanActions.completeMobileScan(mobileApplicationDTO, false);

        verifySonatypeEntitlementUsageAdmin(tenantDTO, 100, 1, false);

        BrowserUtil.clearCookiesLogOff();
        DashboardActions.verifyEntitlementUsageTenant(tenantDTO, 200, 60, true);

        // remediation part

        AllureReportUtil.info("Create and complete remediation scans, should be free of charge");
        staticScanDTO.setAssessmentType(staticScanDTO.getAssessmentType() + " - Remediation");
        staticScanDTO.setEntitlement("Remediation");

        dynamicScan.setAssessmentType(dynamicScan.getAssessmentType() + " - Remediation");
        dynamicScan.setEntitlement("Remediation");
        dynamicScan.setRemediation(true);

        mobileScanDTO.setAssessmentType(mobileScanDTO.getAssessmentType() + " - Remediation");
        mobileScanDTO.setEntitlement("Remediation");

        StaticScanActions.createStaticScan(staticScanDTO, applicationDTO);
        DynamicScanActions.createDynamicScan(dynamicScan, applicationDTO, FodCustomTypes.SetupScanPageStatus.Scheduled);
        MobileScanActions.createMobileScan(mobileScanDTO, mobileApplicationDTO);

        BrowserUtil.clearCookiesLogOff();

        try {
            StaticScanActions.completeStaticScan(applicationDTO, true);
        } catch (FodUnExpectedScanStatusException exception) {
            log.info(exception.getMessage());
        }

        DynamicScanActions.completeDynamicScanAdmin(applicationDTO, false);
        try {
            MobileScanActions.completeMobileScan(mobileApplicationDTO, true, false);
        } catch (FodUnExpectedScanStatusException e) {
            assertThat(e.getMessage()).as("Scan should be completed").containsIgnoringCase("completed");
        }

        BrowserUtil.clearCookiesLogOff();
        DashboardActions.verifyEntitlementUsageTenant(tenantDTO, 200, 60, true);


        // Entitlements III Test merged part
        AllureReportUtil.info("Create dynamic scan (single), cancel without refund, entitlements should be charged");
        dynamicScan = DynamicScanDTO.createDefaultInstance();
        dynamicScan.setStartInFuture(true);
        dynamicScan.setEntitlement("Single Scan");

        mobileScanDTO = MobileScanDTO.createDefaultScanInstance();
        mobileScanDTO.setEntitlement("Single Scan");
        mobileScanDTO.setAssessmentType("AUTO-MOBILE");

        DynamicScanActions.createDynamicScan(dynamicScan, applicationDTO, FodCustomTypes.SetupScanPageStatus.Scheduled);
        BrowserUtil.clearCookiesLogOff();
        DynamicScanActions.cancelDynamicScanAdmin(applicationDTO, true, false);
        BrowserUtil.clearCookiesLogOff();
        DashboardActions.verifyEntitlementUsageTenant(tenantDTO, 200, 90, true);

        AllureReportUtil.info("Create dynamic scan (subscription), cancel with refund, entitlements should be refunded");
        dynamicScan.setEntitlement("Subscription");
        DynamicScanActions.createDynamicScan(dynamicScan, applicationDTO, FodCustomTypes.SetupScanPageStatus.Scheduled);
        DashboardActions.verifyEntitlementUsageTenant(tenantDTO, 200, 121, false);
        BrowserUtil.clearCookiesLogOff();
        DynamicScanActions.cancelDynamicScanAdmin(applicationDTO, true, true);
        BrowserUtil.clearCookiesLogOff();
        DashboardActions.verifyEntitlementUsageTenant(tenantDTO, 200, 90, true);

        AllureReportUtil.info("Create dynamic and mobile scans (subscription),entitlements should be charged");
        DynamicScanActions.createDynamicScan(dynamicScan, applicationDTO, FodCustomTypes.SetupScanPageStatus.Scheduled);
        mobileScanDTO.setEntitlement("Subscription");
        MobileScanActions.createMobileScan(mobileScanDTO, mobileApplicationDTO);
        BrowserUtil.clearCookiesLogOff();
        DynamicScanActions.completeDynamicScanAdmin(applicationDTO, true);
        try {
            MobileScanActions.completeMobileScan(mobileApplicationDTO, true, false);
        } catch (FodUnExpectedScanStatusException e) {
            assertThat(e.getMessage()).as("Scan should be completed").containsIgnoringCase("completed");
        }
        BrowserUtil.clearCookiesLogOff();
        DashboardActions.verifyEntitlementUsageTenant(tenantDTO, 200, 142, true);
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Create one more entitlement");
        var secondEntitlementDTO = EntitlementDTO.createDefaultInstance();
        secondEntitlementDTO.setQuantityPurchased(100);
        EntitlementsActions.createEntitlements(tenantDTO, true, secondEntitlementDTO);
        BrowserUtil.clearCookiesLogOff();

        DashboardActions.verifyEntitlementUsageTenant(tenantDTO, 300, 142, true);

        AllureReportUtil.info("Create dynamic and mobile scans, cancel dynamic with refund, complete dynamic - No charge and no refund for 2nd Sub Scans");
        DynamicScanActions.createDynamicScan(dynamicScan, applicationDTO, FodCustomTypes.SetupScanPageStatus.Scheduled);
        MobileScanActions.createMobileScan(mobileScanDTO, mobileApplicationDTO);
        BrowserUtil.clearCookiesLogOff();
        DynamicScanActions.cancelDynamicScanAdmin(applicationDTO, true, true);
        try {
            MobileScanActions.completeMobileScan(mobileApplicationDTO, true, false);
        } catch (FodUnExpectedScanStatusException e) {
            assertThat(e.getMessage()).as("Scan should be completed").containsIgnoringCase("completed");
        }
        BrowserUtil.clearCookiesLogOff();
        DashboardActions.verifyEntitlementUsageTenant(tenantDTO, 300, 142, true);
        BrowserUtil.clearCookiesLogOff();
        AllureReportUtil.info("Disable all entitlements and verify");
        EntitlementsActions.disableAllEntitlements(tenantDTO, true);
        BrowserUtil.clearCookiesLogOff();
        DashboardActions.verifyEntitlementsDisabled(tenantDTO, true);
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify entitlement model 'Scans', with 1 purchased, first scan should be charged, the second one should be rejected")
    @Test(groups = {"regression"})
    public void entitlementsTest2() {
        var tenant = TenantDTO.createDefaultInstance();
        tenant.setEntitlementModel(FodCustomTypes.EntitlementModel.Scans);
        tenant.setSubscriptionModel(FodCustomTypes.SubscriptionModel.StartOnFirstScan);
        tenant.setPaymentModel(FodCustomTypes.PaymentModel.Other);
        var entitlement = EntitlementDTO.createDefaultInstance();
        entitlement.setQuantityPurchased(1);
        entitlement.setAnalysisType(FodCustomTypes.AnalysisType.Dynamic);
        entitlement.setAssessmentType("AUTO-DYNAMIC");
        entitlement.setFrequency(FodCustomTypes.ScanFrequency.SingleScan);
        entitlement.setScans(true);
        tenant.setEntitlementDTO(entitlement);
        TenantActions.createTenant(tenant);
        BrowserUtil.clearCookiesLogOff();
        var application = ApplicationDTO.createDefaultInstance();
        LogInActions.tamUserLogin(tenant);
        ApplicationActions.createApplication(application);
        var dynamicScan = DynamicScanDTO.createDefaultInstance();
        dynamicScan.setStartInFuture(true);
        dynamicScan.setEntitlement("Single Scan");
        DynamicScanActions.createDynamicScan(dynamicScan, application, FodCustomTypes.SetupScanPageStatus.Scheduled);
        BrowserUtil.clearCookiesLogOff();
        DynamicScanActions.completeDynamicScanAdmin(application, true);
        BrowserUtil.clearCookiesLogOff();
        DashboardActions.verifyEntitlementUsageTenant(tenant, 1, 1, true);
        try {
            DynamicScanActions.createDynamicScan(dynamicScan, application, FodCustomTypes.SetupScanPageStatus.Scheduled);
        } catch (FodElementNotCreatedException e) {
            log.info("Expecting alert about absence of entitlements here, so it's fine");
            assertThat(e.getMessage()).contains("No active entitlement");
        }
        BrowserUtil.clearCookiesLogOff();
        EntitlementsActions.disableAllEntitlements(tenant, true);
        BrowserUtil.clearCookiesLogOff();
        DashboardActions.verifyEntitlementsDisabled(tenant, true);
    }

    @Owner("tmagill@opentext.com")
    @FodBacklogItem("319002")
    @FodBacklogItem("720007")
    @MaxRetryCount(2)
    @Description("Test will validate the switching between Sonatype and Debricked entitlements Admin - Tenants - Entitlements page")
    @Test(groups = {"regression"})
    public void enableDebrickedEntitlementsTest() {
        TenantDTO tenantDTO;
        EntitlementDTO entitlementDTO;

        entitlementDTO = EntitlementDTO.createDefaultInstance();
        entitlementDTO.setQuantityPurchased(200);

        var sonatypeEntitlement = EntitlementDTO.createDefaultInstance();
        sonatypeEntitlement.setEntitlementType(FodCustomTypes.EntitlementType.SonatypeEntitlement);
        sonatypeEntitlement.setQuantityPurchased(100);

        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setEntitlementDTO(entitlementDTO);
        tenantDTO.setEntitlementModel(FodCustomTypes.EntitlementModel.Units);
        tenantDTO.setSubscriptionModel(FodCustomTypes.SubscriptionModel.Period);

        TenantActions.createTenant(tenantDTO);
        EntitlementsActions.createEntitlements(tenantDTO, false, sonatypeEntitlement);

        var debrickedEntitlement = EntitlementDTO.createDefaultInstance();
        debrickedEntitlement.setEntitlementType(FodCustomTypes.EntitlementType.DebrickedEntitlement);
        debrickedEntitlement.setQuantityPurchased(100);

        BrowserUtil.clearCookiesLogOff();

        var openEntitlementsPage = LogInActions.adminLogIn()
                .adminTopNavbar.openTenants()
                .findTenant(tenantDTO.getTenantName()).openTenantByName(tenantDTO.getTenantName())
                .openEntitlements();

        openEntitlementsPage.switchEntitlementsTab(FodCustomTypes.EntitlementType.SonatypeEntitlement)
                .getAll().get(0).pressEdit()
                .disableEntitlement();
        assertThat(new EntitlementCell().getEnabled())
                .as("Enabled column for Sonatype Entitlement should be 'No'")
                .isEqualTo("No");

        EntitlementsActions.createEntitlements(tenantDTO, false, debrickedEntitlement);

        var getEntitlementCount = openEntitlementsPage
                .switchEntitlementsTab(FodCustomTypes.EntitlementType.DebrickedEntitlement).getAll().get(0);
        assertThat(getEntitlementCount)
                .as("There should be an entry for the Debricked Entitlement").isNotNull();
        assertThat(new EntitlementCell().getEnabled())
                .as("'Enabled' column for Debricked Entitlement should be 'Yes'")
                .isEqualTo("Yes");

        openEntitlementsPage.switchEntitlementsTab(FodCustomTypes.EntitlementType.DebrickedEntitlement)
                .getAll().get(0).pressEdit()
                .disableEntitlement();
        assertThat(new EntitlementCell().getEnabled())
                .as("'Enabled' column for Debricked Entitlement should be 'No'")
                .isEqualTo("No");

    }

    public void verifySonatypeEntitlementUsageAdmin(TenantDTO tenantDTO,
                                                    int expectedPurchased,
                                                    int expectedConsumed,
                                                    boolean needLogin) {
        SoftAssertions softAssertions = new SoftAssertions();
        AllureReportUtil.info("Verify Sonatype entitlements usage");
        if (needLogin) {
            LogInActions.adminLogIn();
        }

        var entitlementsPage = new AdminTopNavbar().openTenants()
                .openTenantByName(tenantDTO.getTenantName()).openEntitlements()
                .switchEntitlementsTab(FodCustomTypes.EntitlementType.SonatypeEntitlement);
        var entitlement = entitlementsPage.getAll().get(0);
        softAssertions.assertThat(entitlement.getQuantityConsumed())
                .as("Sonatype entitlements: consumed count should match expected")
                .isEqualTo(expectedConsumed);
        softAssertions.assertThat(entitlement.getQuantityPurchased())
                .as("Sonatype entitlements: purchased count should match expected")
                .isEqualTo(expectedPurchased);
        softAssertions.assertAll();
    }

    @Owner("vdubovyk@opentext.com")
    @FodBacklogItem("1568001")
    @MaxRetryCount(2)
    @Description("User that is not allowed to consume entitlements should not start the open source scan that " +
            "consumes open source entitlements")
    @Test(groups = {"regression"})
    public void entitlementConsumptionNotAllowedTest() {
        log.info("Test data preparation");
        var tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setOptionsToEnable(new String[]{"Allow Binary Scanning"});
        TenantActions.createTenant(tenantDTO);

        var entitlementsPage = EntitlementsActions.createEntitlements(tenantDTO, false,
                EntitlementDTO.createDefaultInstance(),
                EntitlementDTO.createDefaultInstance(FodCustomTypes.EntitlementType.DebrickedEntitlement));

        var initialDebrickedEntitlementConsumed = entitlementsPage.getAll().get(0).getQuantityConsumed();
        var initialFortifyEntitlementConsumed = entitlementsPage
                .switchEntitlementsTab(FodCustomTypes.EntitlementType.FortifyEntitlement)
                .getAll().get(0).getQuantityConsumed();

        var tenantUserRoleDTO = TenantUserRoleDTO.createDefaultInstance();
        tenantUserRoleDTO.setApplicationAccess(FodCustomTypes.RoleApplicationAccess.All);
        tenantUserRoleDTO.setStartStaticScanPermissions(FodCustomTypes.RoleStartScanPermissions.Allow);
        tenantUserRoleDTO.setConsumeEntitlements(false);
        tenantUserRoleDTO.setRoleName("deny_consuming");
        BrowserUtil.clearCookiesLogOff();

        LogInActions.tamUserLogin(tenantDTO);
        TenantRoleActions.createRole(tenantUserRoleDTO);

        var tenantUserDTO = TenantUserDTO.createDefaultInstance();
        tenantUserDTO.setTenant(tenantDTO.getTenantName());
        TenantUserActions.createTenantUser(tenantUserDTO);
        BrowserUtil.clearCookiesLogOff();

        LogInActions.tamUserLogin(tenantDTO)
                .tenantTopNavbar.openAdministration()
                .openUsers()
                .editUserByName(tenantUserDTO.getUserName())
                .selectRole(tenantUserRoleDTO.getRoleName())
                .pressSaveBtn();

        var applicationDTO = ApplicationActions.createApplication();
        BrowserUtil.clearCookiesLogOff();

        log.info("Trying to start the static scan in existing release and application");
        var staticScanDTO = StaticScanDTO.createDefaultInstance();
        staticScanDTO.setLanguageLevel("1.8");

        var staticScanSetupPage = LogInActions
                .tenantUserLogIn(tenantUserDTO.getUserName(), tenantUserDTO.getPassword(), tenantDTO.getTenantCode())
                .tenantTopNavbar
                .openApplications()
                .openYourReleases()
                .openDetailsForRelease(applicationDTO.getApplicationName(), applicationDTO.getReleaseName())
                .pressStartStaticScan()
                .chooseAssessmentType(staticScanDTO.getAssessmentType());
        staticScanSetupPage.getAdvancedSettingsPanel().expand()
                .chooseTechnologyStack(staticScanDTO.getTechnologyStack())
                .chooseLanguageLevel(staticScanDTO.getLanguageLevel());
        staticScanSetupPage.chooseAuditPreference(staticScanDTO.getAuditPreference())
                .pressStartScanBtn();

        assertThat(new ModalDialog().getMessage())
                .as("User is denied to start static scan.")
                .isEqualTo("Your role does not allow you to consume entitlements.");
        BrowserUtil.clearCookiesLogOff();

        log.info("Trying to enable the Open Source (Debricked) scans in existing static scan");
        LogInActions.tamUserLogin(tenantDTO);
        StaticScanActions.createStaticScan(staticScanDTO, applicationDTO)
                .waitStatus(FodCustomTypes.SetupScanPageStatus.Completed);
        BrowserUtil.clearCookiesLogOff();

        LogInActions
                .tenantUserLogIn(tenantUserDTO.getUserName(), tenantUserDTO.getPassword(), tenantDTO.getTenantCode())
                .tenantTopNavbar.openApplications()
                .openYourReleases()
                .openDetailsForRelease(applicationDTO.getApplicationName(), applicationDTO.getReleaseName())
                .pressStartStaticScan()
                .enableOpenSourceScans(true)
                .pressStartScanBtn();

        assertThat(new ModalDialog().getMessage())
                .as("User is denied to start open source scan.")
                .isEqualTo("Your role does not allow you to consume entitlements to perform " +
                        "an open source component analysis. To start a scan you need to uncheck the " +
                        "'Software Composition Analysis' option.");
        BrowserUtil.clearCookiesLogOff();

        log.info("Trying to start the Open Source (Debricked) scan in new release and application");
        applicationDTO = ApplicationActions.createApplication(tenantDTO, true);
        BrowserUtil.clearCookiesLogOff();

        LogInActions
                .tenantUserLogIn(tenantUserDTO.getUserName(), tenantUserDTO.getPassword(), tenantDTO.getTenantCode())
                .tenantTopNavbar.openApplications()
                .openYourReleases()
                .openDetailsForRelease(applicationDTO.getApplicationName(), applicationDTO.getReleaseName())
                .pressStartOpenSourceScan()
                .uploadFile("payloads/fod/NodeGoat-main-GOOD_AND_2_BAD-LOCKFILE.zip")
                .uploadButton.shouldBe(Condition.visible, Condition.enabled).click();

        var startOpenSourceScanPopup = new StartOpenSourceScanPopup();
        assertThat(startOpenSourceScanPopup.getErrorMessage())
                .as("User is denied to start open source scan.")
                .isEqualTo("Your role does not allow you to consume entitlements.");
        BrowserUtil.clearCookiesLogOff();

        assertThat(LogInActions.tamUserLogin(tenantDTO)
                .tenantTopNavbar.openAdministration()
                .openEventLog()
                .getAllLogs()
                .stream()
                .filter(log -> "Tenant Entitlement Consumed".equals(log.getType()))
                .toList())
                .as("'Tenant Entitlement Consumed' event should occur only once!.")
                .hasSize(1);

        entitlementsPage = LogInActions.adminLogIn()
                .adminTopNavbar.openTenants()
                .openTenantByName(tenantDTO.getTenantName())
                .openEntitlements();

        assertThat(entitlementsPage.getAll().get(0).getQuantityConsumed())
                .as("Fortify entitlements should be consumed.")
                .isGreaterThan(initialFortifyEntitlementConsumed);
        assertThat(entitlementsPage.switchEntitlementsTab(FodCustomTypes.EntitlementType.DebrickedEntitlement)
                .getAll().get(0).getQuantityConsumed())
                .as("Debricked entitlements should not be consumed.")
                .isEqualTo(initialDebrickedEntitlementConsumed);
    }
}