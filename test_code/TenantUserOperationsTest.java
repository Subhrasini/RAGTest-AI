package com.fortify.fod.ui.test.regression;


import com.codeborne.selenide.Selenide;
import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.UniqueRunTag;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.elements.DropdownButton;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.TenantDTO;
import com.fortify.fod.common.entities.TenantUserDTO;
import com.fortify.fod.common.entities.TenantUserRoleDTO;
import com.fortify.fod.common.utils.DateTimeUtil;
import com.fortify.fod.ui.pages.admin.administration.users.AdminUsersPage;
import com.fortify.fod.ui.pages.tenant.administration.user_management.groups.GroupsPage;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static com.codeborne.selenide.Selenide.page;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("oradchenko@opentext.com")
@Slf4j
public class TenantUserOperationsTest extends FodBaseTest {

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("TAM user should be able to create and delete user groups and roles")
    @Test(groups = "{regression}")
    public void createAndDeleteUserGroupAndRole() {

        var tenantName = defaultTenantDTO.getTenantName();
        var groupName = "Group-" + UniqueRunTag.generate();
        var tenantUser = TenantUserDTO.createDefaultInstance();
        tenantUser.setTenant(tenantName);

        var role = TenantUserRoleDTO.createDefaultInstance();
        role.setApplicationAccess(FodCustomTypes.RoleApplicationAccess.All);
        role.setApplicationsPermissions(FodCustomTypes.RoleApplicationsPermissions.Manage);
        role.setIssuePermissions(FodCustomTypes.RoleApplicationIssuePermissions.Edit);
        role.setReportsPermissions(FodCustomTypes.RoleApplicationReportsPermissions.Create);
        role.setAccessTraining(true);
        role.setConsumeEntitlements(true);
        role.setStartDynamicScanPermissions(FodCustomTypes.RoleStartScanPermissions.Configure);


        LogInActions.tamUserLogin(FodConfig.TAM_USER_NAME, FodConfig.TAM_PASSWORD, tenantName).tenantTopNavbar
                .openAdministration();
        var usersPage = page(AdminUsersPage.class);

        TenantUserActions.createTenantUser(tenantUser);
        Selenide.clearBrowserCookies();
        LogInActions.tamUserLogin(FodConfig.TAM_USER_NAME, FodConfig.TAM_PASSWORD, tenantName)
                .tenantTopNavbar.openAdministration();

        usersPage.openGroups();
        TenantGroupActions.createTenantGroup(groupName, "", true);

        var groupsPage = page(GroupsPage.class);

        var rolesPage = groupsPage.openRoles();
        TenantRoleActions.createRole(role);
        TenantRoleActions.deleteRole(role);

        rolesPage.openGroups();
        TenantGroupActions.deleteTenantGroup(groupName);

        groupsPage.openUsers();
        TenantUserActions.deleteTenantUser(tenantUser);
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("713009")
    @Owner("sbehera3@opentext.com")
    @Description("Check that Container scanning is removed")
    @Test(groups = {"regression"})
    public void verifyContainerScanTest() {
        var labelNames = LogInActions.adminLogIn().adminTopNavbar.openTenants()
                .openTenantByName(defaultTenantDTO.getTenantName()).openTabOptions()
                .getLabelNamesFromOptionsTab();
        assertThat(labelNames)
                .as("Verify that label names list is not empty")
                .isNotEmpty();
        assertThat(labelNames.contains("Enable Container Scans"))
                .as("Verify that Container related references are removed from UI Admin side")
                .isFalse();

        ApplicationDTO applicationDTO = ApplicationActions.createApplication(defaultTenantDTO, true);
        new TenantTopNavbar().openApplications().openYourReleases()
                .openDetailsForRelease(applicationDTO.getApplicationName(), applicationDTO.getReleaseName());
        var allOptions = new DropdownButton("Start Scan").getOptionsTextValues();
        assertThat(allOptions)
                .as("Verify that options list is not empty")
                .isNotEmpty();
        assertThat(allOptions)
                .as("Verify that Container related references are removed from UI Tenant side")
                .doesNotContain("Container");

    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("1652027")
    @Owner("pwardhan@opentext.com")
    @Description("Check that new user added from the tenant side has the Created Date set correctly")
    @Test(groups = {"regression"})
    public void verifyDateColumnForUserAdded() {
        AllureReportUtil.info("Navigating to Administrator->User to create a new user");
        LogInActions.tamUserLogin(defaultTenantDTO).tenantTopNavbar.openAdministration().openUsers();
        var tenantUser = TenantUserDTO.createDefaultInstance();
        tenantUser.setTenant(defaultTenantDTO.getTenantName());
        AllureReportUtil.info("Creating  a new User ");
        TenantUserActions.createTenantUser(tenantUser);
        BrowserUtil.clearCookiesLogOff();
        var userPage = LogInActions.tamUserLogin(defaultTenantDTO).tenantTopNavbar.openAdministration().openUsers();
        DateTimeUtil.getCurrentDateGMT();
        String ExpectedDate1= LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyy/MM/dd")).toString();
        String expectedDate2=LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd")).toString();
        AllureReportUtil.info("Checking the Created date of the new user is the current date.");
        assertThat(userPage.getCreatedDateByUserName(tenantUser.getUserName())).as("The Created date should be the current Date")
                .containsAnyOf(ExpectedDate1,expectedDate2);
    }
}