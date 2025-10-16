package com.fortify.fod.ui.test.regression;

import com.codeborne.selenide.Selenide;
import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.StaticScanDTO;
import com.fortify.fod.common.entities.TenantDTO;
import com.fortify.fod.common.entities.TenantUserDTO;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.MaxRetryCount;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("svpillai@opentext.com")
@Slf4j
@Test(enabled = false)
public class ThirdPartyAppStaticScanPermissionTest extends FodBaseTest {
    TenantDTO tenantDTO;
    TenantUserDTO leadDeveloper;
    ApplicationDTO webApp;
    String fileUpload = "payloads/fod/JavaTestPayload.zip";

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Create Tenant and activate Security Lead role")
    @Test(groups = {"regression"})
    public void prepareTestData() {
        AllureReportUtil.info("Create tenant with Scan third party Libraries option set to true");
        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setOptionsToEnable(new String[]{"Allow scanning with no entitlements"
                //"Scan Third Party Libraries"
        });
        TenantActions.createTenant(tenantDTO, true, false);
        BrowserUtil.clearCookiesLogOff();
        TenantUserActions.activateSecLead(tenantDTO, true);

        webApp = ApplicationDTO.createDefaultInstance();
        ApplicationActions.createApplication(webApp, tenantDTO, false);

        leadDeveloper = TenantUserDTO.createDefaultInstance();
        leadDeveloper.setTenant(tenantDTO.getTenantCode());
        leadDeveloper.setUserName(leadDeveloper.getUserName() + "-LeadDevCreateAccess");
        leadDeveloper.setRole(FodCustomTypes.TenantUserRole.LeadDeveloper);

        TenantUserActions.createTenantUsers(tenantDTO, leadDeveloper);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Enable View Third Party App Access for the role ")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void updateRoleAccess() {
        AllureReportUtil.info("Update Application Permission and view third party Apps options for a role");
        var securityLeadLogin = LogInActions
                .tenantUserLogIn(tenantDTO.getUserName(), FodConfig.TAM_PASSWORD, tenantDTO.getTenantCode())
                .tenantTopNavbar.openAdministration();
        securityLeadLogin.openRoles().editRole(String.valueOf(FodCustomTypes.TenantUserRole.LeadDeveloper.getTypeValue()))
                .setApplicationPermissions(FodCustomTypes.RoleApplicationsPermissions.Create)
                .setViewThirdPartyApps(true)
                .save();
        var usersList = securityLeadLogin.openUsers().getUserNames();
        assertThat(usersList.size()).as("2 Users should be created").isEqualTo(2);
        securityLeadLogin.openUsers().pressAssignApplicationsByUser(leadDeveloper.getUserName())
                .assignApplication(webApp)
                .pressSave();
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Start a static scan using scan with Third party libraries option enabled")
    @Test(groups = {"regression"}, dependsOnMethods = {"updateRoleAccess"})
    public void staticScanAccessUsingThirdPartyApps() {
        BrowserUtil.clearCookiesLogOff();
        AllureReportUtil.info("The role should have access to start a scan using third party libraries enabled");
        var staticScanSetupPage = LogInActions
                .tenantUserLogIn(leadDeveloper.getUserName(), FodConfig.TAM_PASSWORD, tenantDTO.getTenantCode())
                .tenantTopNavbar.openApplications().openYourReleases()
                .openDetailsForRelease(webApp)
                .openStaticScanSetup()
                .chooseAssessmentType("AUTO-STATIC")
                .chooseEntitlement("Subscription")
                .chooseAuditPreference(FodCustomTypes.AuditPreference.Automated);
        staticScanSetupPage.getAdvancedSettingsPanel().expand()
                .chooseTechnologyStack(FodCustomTypes.TechnologyStack.JAVA)
                .chooseLanguageLevel("1.9");
        staticScanSetupPage.pressSaveBtn();
        Selenide.refresh();

        var staticScan = StaticScanDTO.createDefaultInstance();
        staticScan.setFileToUpload(fileUpload);
        staticScan.setAuditPreference(FodCustomTypes.AuditPreference.Manual);
        StaticScanActions.createStaticScan(staticScan, webApp);
        staticScanSetupPage.waitStatus(FodCustomTypes.SetupScanPageStatus.Queued,
                FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();
        validateScanStatus(webApp);
    }

    public void validateScanStatus(ApplicationDTO webApp) {
        var staticScanJobsPage = LogInActions.adminLogIn().adminTopNavbar.openStatic();
        staticScanJobsPage.appliedFilters.clearAll();
        staticScanJobsPage.findWithSearchBox(webApp.getApplicationName());
        assertThat(staticScanJobsPage.getAllScans()
                .get(0)
                .getLastJob()
                .waitForJobStatus(FodCustomTypes.JobStatus.Success)
                .getStatus())
                .as("Scan status should be Success")
                .isEqualTo(FodCustomTypes.JobStatus.Success.getTypeValue());
    }
}