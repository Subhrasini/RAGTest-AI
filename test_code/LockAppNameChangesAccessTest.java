package com.fortify.fod.ui.test.regression;

import com.codeborne.selenide.Selenide;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.UniqueRunTag;
import com.fortify.common.utils.WaitUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.TenantDTO;
import com.fortify.fod.common.entities.TenantUserDTO;
import com.fortify.fod.ui.pages.tenant.administration.user_management.groups.GroupsPage;
import com.fortify.fod.ui.pages.tenant.navigation.TenantSideNavTabs;
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

import java.time.Duration;

import static com.codeborne.selenide.Selenide.*;
import static com.codeborne.selenide.WebDriverRunner.url;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("svpillai@opentext.com")
@Slf4j
@FodBacklogItem("353003")
public class LockAppNameChangesAccessTest extends FodBaseTest {
    TenantDTO tenantDTO;
    ApplicationDTO webApp, mobileApp;
    TenantUserDTO applicationLead, executive, leadDeveloper;
    String appTestGroup;

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Create Tenant and activate Security Lead role . Create required User Roles and applications")
    @Test(groups = {"regression"})
    public void prepareTestData() {
        tenantDTO = TenantDTO.createDefaultInstance();
        TenantActions.createTenant(tenantDTO, true, false);
        BrowserUtil.clearCookiesLogOff();
        TenantUserActions.activateSecLead(tenantDTO, true);
        BrowserUtil.clearCookiesLogOff();

        webApp = ApplicationDTO.createDefaultInstance();
        mobileApp = ApplicationDTO.createDefaultMobileInstance();
        ApplicationActions.createApplication(webApp, tenantDTO, true);
        ApplicationActions.createApplication(mobileApp);

        applicationLead = TenantUserDTO.createDefaultInstance();
        applicationLead.setTenant(tenantDTO.getTenantCode());
        applicationLead.setUserName(applicationLead.getUserName() + "-AppLeadManage");
        applicationLead.setRole(FodCustomTypes.TenantUserRole.ApplicationLead);

        executive = TenantUserDTO.createDefaultInstance();
        executive.setTenant(tenantDTO.getTenantCode());
        executive.setUserName(executive.getUserName() + "-ExecutiveView");
        executive.setRole(FodCustomTypes.TenantUserRole.Executive);

        leadDeveloper = TenantUserDTO.createDefaultInstance();
        leadDeveloper.setTenant(tenantDTO.getTenantCode());
        leadDeveloper.setUserName(leadDeveloper.getUserName() + "-LeadDevCreate");
        leadDeveloper.setRole(FodCustomTypes.TenantUserRole.LeadDeveloper);

        TenantUserActions.createTenantUsers(tenantDTO, applicationLead, executive, leadDeveloper);
        BrowserUtil.clearCookiesLogOff();
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Assign Application Access as ALL and validate app name changes with users having view,create and manage access")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void verifyAppNameChangesWithAppAccessAsAll() {
        var securityLeadLogin = LogInActions
                .tenantUserLogIn(tenantDTO.getUserName(), FodConfig.TAM_PASSWORD, tenantDTO.getTenantCode())
                .tenantTopNavbar.openAdministration();
        securityLeadLogin.openRoles().editRole(String.valueOf(FodCustomTypes.TenantUserRole.ApplicationLead.getTypeValue()))
                .setApplicationAccess(FodCustomTypes.RoleApplicationAccess.All)
                .setApplicationPermissions(FodCustomTypes.RoleApplicationsPermissions.Manage)
                .save();
        securityLeadLogin.openRoles().editRole(String.valueOf(FodCustomTypes.TenantUserRole.Executive.getTypeValue()))
                .setApplicationAccess(FodCustomTypes.RoleApplicationAccess.All)
                .setApplicationPermissions(FodCustomTypes.RoleApplicationsPermissions.View)
                .save();
        securityLeadLogin.openRoles().editRole(String.valueOf(FodCustomTypes.TenantUserRole.LeadDeveloper.getTypeValue()))
                .setApplicationAccess(FodCustomTypes.RoleApplicationAccess.All)
                .setApplicationPermissions(FodCustomTypes.RoleApplicationsPermissions.Create)
                .save();

        var usersPageList = securityLeadLogin.openUsers().getUserNames();
        assertThat(usersPageList.size()).as("4 Users should be created").isEqualTo(4);
        BrowserUtil.clearCookiesLogOff();

        appNameChangeWithViewAccess();
        BrowserUtil.clearCookiesLogOff();
        appNameChangeWithManageAccess();
        BrowserUtil.clearCookiesLogOff();
        appNameChangeWithCreateAccess();
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Assign Application Access as Manual and validate app name changes with users having view,create and manage access")
    @Test(groups = {"regression"}, dependsOnMethods = {"verifyAppNameChangesWithAppAccessAsAll"})
    public void verifyAppNameChangesWithAppAccessAsManual() {
        var securityLeadLogin = LogInActions
                .tenantUserLogIn(tenantDTO.getUserName(), FodConfig.TAM_PASSWORD, tenantDTO.getTenantCode())
                .tenantTopNavbar.openAdministration();
        securityLeadLogin.openRoles().editRole(String.valueOf(FodCustomTypes.TenantUserRole.ApplicationLead.getTypeValue()))
                .setApplicationAccess(FodCustomTypes.RoleApplicationAccess.Manual)
                .save();
        securityLeadLogin.openRoles().editRole(String.valueOf(FodCustomTypes.TenantUserRole.Executive.getTypeValue()))
                .setApplicationAccess(FodCustomTypes.RoleApplicationAccess.Manual)
                .save();
        securityLeadLogin.openRoles().editRole(String.valueOf(FodCustomTypes.TenantUserRole.LeadDeveloper.getTypeValue()))
                .setApplicationAccess(FodCustomTypes.RoleApplicationAccess.Manual)
                .save();

        var usersPageList = securityLeadLogin.openUsers().getUserNames();
        assertThat(usersPageList.size()).as("4 Users should be present on Users Page").isEqualTo(4);

        securityLeadLogin.openUsers().pressAssignApplicationsByUser(applicationLead.getUserName())
                .assignApplication(webApp).assignApplication(mobileApp)
                .pressSave();
        securityLeadLogin.openUsers().pressAssignApplicationsByUser(executive.getUserName())
                .assignApplication(webApp).assignApplication(mobileApp)
                .pressSave();
        securityLeadLogin.openUsers().pressAssignApplicationsByUser(leadDeveloper.getUserName())
                .assignApplication(webApp).assignApplication(mobileApp)
                .pressSave();
        BrowserUtil.clearCookiesLogOff();

        appNameChangeWithViewAccess();
        BrowserUtil.clearCookiesLogOff();
        appNameChangeWithManageAccess();
        BrowserUtil.clearCookiesLogOff();
        appNameChangeWithCreateAccess();
    }

    @MaxRetryCount(3)
    @Description("Create a group and add users , applications.Validate app name changes with users having view,create and manage access ")
    @Severity(SeverityLevel.NORMAL)
    @Test(groups = {"regression"}, dependsOnMethods = {"verifyAppNameChangesWithAppAccessAsManual"})
    public void verifyAppNameChangesForGroups() {
        appTestGroup = "Auto-App-Test-Group" + UniqueRunTag.generate();
        LogInActions.tenantUserLogIn(tenantDTO.getUserName(), FodConfig.TAM_PASSWORD, tenantDTO.getTenantCode());
        TenantGroupActions.createTenantGroup(appTestGroup, applicationLead, executive, leadDeveloper);
        var groupsPage = page(GroupsPage.class);
        assertThat(groupsPage.getGroupByName(appTestGroup))
                .as("New Group should be created for the test: " + appTestGroup)
                .isNotNull();
        var applicationCount = groupsPage.openGroups().getGroupByName(appTestGroup)
                .pressAssignApplication()
                .assignApplication(webApp)
                .assignApplication(mobileApp)
                .pressSave()
                .getGroupByName(appTestGroup)
                .getAssignedApplicationsCount();
        assertThat(applicationCount)
                .as("Web and Mobile Application should be assigned")
                .isEqualTo(2);
        BrowserUtil.clearCookiesLogOff();

        appNameChangeWithViewAccess();
        BrowserUtil.clearCookiesLogOff();
        appNameChangeWithManageAccess();
        BrowserUtil.clearCookiesLogOff();
        appNameChangeWithCreateAccess();
    }

    public void appNameChangeWithViewAccess() {
        var yourApplicationsPage = LogInActions
                .tenantUserLogIn(executive.getUserName(), FodConfig.TAM_PASSWORD, tenantDTO.getTenantCode())
                .tenantTopNavbar.openApplications()
                .findWithSearchBox(webApp.getApplicationName());

        WaitUtil.waitForTrue(() -> yourApplicationsPage
                .getAppByName(webApp.getApplicationName()) != null, Duration.ofMinutes(2), true);

        var viewWebAppSettingsTab = yourApplicationsPage
                .openDetailsFor(webApp.getApplicationName())
                .openOverview();
        assertThat(viewWebAppSettingsTab.sideNavTabExists(TenantSideNavTabs.ApplicationDetails.Settings))
                .as("Validate settings tab is not present in side navigation tab for Web Application with user having View Access")
                .isFalse();
        open(url() + "/Settings");
        assertThat($("h2").text().trim())
                .as("Validate Access Denied Page should be opened")
                .isEqualTo("You are not allowed to access this function.");
        BrowserUtil.clearCookiesLogOff();

        var viewMobAppSettingsTab = LogInActions
                .tenantUserLogIn(executive.getUserName(), FodConfig.TAM_PASSWORD, tenantDTO.getTenantCode())
                .tenantTopNavbar.openApplications()
                .openDetailsFor(mobileApp.getApplicationName())
                .openOverview();
        assertThat(viewMobAppSettingsTab.sideNavTabExists(TenantSideNavTabs.ApplicationDetails.Settings))
                .as("Validate settings tab is not present in side navigation tab for Mobile Application with user having View Access")
                .isFalse();
        open(url() + "/Settings");
        assertThat($("h2").text().trim())
                .as("Validate the Access Denied Page should be opened")
                .isEqualTo("You are not allowed to access this function.");
    }

    public void appNameChangeWithManageAccess() {
        var manageWebAppSettingsPage = LogInActions
                .tenantUserLogIn(applicationLead.getUserName(), FodConfig.TAM_PASSWORD, tenantDTO.getTenantCode())
                .tenantTopNavbar.openApplications().openDetailsFor(webApp.getApplicationName())
                .openSettings()
                .openAppSummaryTab().setApplicationName("Updated Web App")
                .pressSave();
        Selenide.refresh();
        assertThat(manageWebAppSettingsPage.openAppSummaryTab().appNameField.getValue())
                .as("Validate Web App Name should not be  changed with user having Manage Access")
                .isEqualTo(webApp.getApplicationName());
        BrowserUtil.clearCookiesLogOff();

        var manageMobileAppSettingsPage = LogInActions
                .tenantUserLogIn(applicationLead.getUserName(), FodConfig.TAM_PASSWORD, tenantDTO.getTenantCode())
                .tenantTopNavbar.openApplications().openDetailsFor(mobileApp.getApplicationName())
                .openSettings()
                .openAppSummaryTab().setApplicationName("Updated Mobile App")
                .pressSave();
        Selenide.refresh();
        assertThat(manageMobileAppSettingsPage.openAppSummaryTab().appNameField.getValue())
                .as("Validate Mobile App Name should not be changed with user having Manage Access")
                .isEqualTo(mobileApp.getApplicationName());
    }

    public void appNameChangeWithCreateAccess() {
        var updateWebAppName = LogInActions
                .tenantUserLogIn(leadDeveloper.getUserName(), FodConfig.TAM_PASSWORD, tenantDTO.getTenantCode())
                .tenantTopNavbar.openApplications().openDetailsFor(webApp.getApplicationName())
                .openSettings()
                .openAppSummaryTab().setApplicationName("Updated Web App Name" + UniqueRunTag.generate())
                .pressSave();
        Selenide.refresh();
        var newWebAppName = updateWebAppName.openAppSummaryTab().appNameField.getValue();
        assertThat(updateWebAppName.openAppSummaryTab().appNameField.getValue())
                .as("Web Application name should be updated with user having Create access")
                .isEqualTo(newWebAppName);
        BrowserUtil.clearCookiesLogOff();

        var updateMobileAppName = LogInActions
                .tenantUserLogIn(leadDeveloper.getUserName(), FodConfig.TAM_PASSWORD, tenantDTO.getTenantCode())
                .tenantTopNavbar.openApplications().openDetailsFor(mobileApp.getApplicationName())
                .openSettings()
                .openAppSummaryTab().setApplicationName("Updated Mobile App Name" + UniqueRunTag.generate())
                .pressSave();
        Selenide.refresh();
        var newMobileAppName = updateMobileAppName.openAppSummaryTab().appNameField.getValue();
        assertThat(updateMobileAppName.openAppSummaryTab().appNameField.getValue())
                .as("Mobile Application name should be updated with user having Create access")
                .isEqualTo(newMobileAppName);

        webApp.setApplicationName(newWebAppName);
        mobileApp.setApplicationName(newMobileAppName);
    }
}
