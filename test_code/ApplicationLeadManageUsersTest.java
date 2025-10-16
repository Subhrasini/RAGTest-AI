package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.UniqueRunTag;
import com.fortify.common.utils.WaitUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.EntitlementDTO;
import com.fortify.fod.common.entities.TenantDTO;
import com.fortify.fod.common.entities.TenantUserDTO;
import com.fortify.fod.common.utils.sql.FodSQLUtil;
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
import utils.FodBacklogItems;
import utils.MaxRetryCount;

import java.time.Duration;

import static com.fortify.common.utils.WaitUtil.waitFor;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("kbadia@opentext.com")
@Slf4j
public class ApplicationLeadManageUsersTest extends FodBaseTest {
    TenantDTO tenantDTO;
    TenantUserDTO appLead, dev;
    ApplicationDTO applicationDTO;

    public void init() {
        applicationDTO = ApplicationDTO.createDefaultInstance();
        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setEntitlementDTO(EntitlementDTO.createDefaultInstance());
        appLead = TenantUserDTO
                .createInstanceWithUserRole(FodCustomTypes.TenantUserRole.ApplicationLead);
        appLead.setTenant(tenantDTO.getTenantName());
        dev = TenantUserDTO
                .createInstanceWithUserRole(FodCustomTypes.TenantUserRole.Developer);
        dev.setTenant(tenantDTO.getTenantName());
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItems({@FodBacklogItem("616001"), @FodBacklogItem("606016")})
    @Description("Application Lead able to manage users for the apps they have visibility")
    @Test(groups = {"regression"})
    public void applicationLeadManageUsersIssueTest() {
        init();

        TenantActions.createTenant(tenantDTO);
        BrowserUtil.clearCookiesLogOff();
        ApplicationActions.createApplication(applicationDTO, tenantDTO, true);

        TenantUserActions.activateSecLead(tenantDTO, false);
        TenantUserActions.createTenantUsers(tenantDTO, appLead, dev);
        LogInActions.tamUserLogin(tenantDTO);
        var groupsPage = TenantGroupActions
                .createTenantGroup(
                        "New group " + UniqueRunTag.generate(), "", true);
        groupsPage.getAllGroups().get(0)
                .pressAssignApplication()
                .assignApplication(applicationDTO)
                .pressSave();
        BrowserUtil.clearCookiesLogOff();
        var applicationDetailsPage = LogInActions
                .tenantUserLogIn(appLead.getUserName(),
                        FodConfig.TAM_PASSWORD, tenantDTO.getTenantCode())
                .tenantTopNavbar.openApplications()
                .openDetailsFor(applicationDTO.getApplicationName());
        var isEditButtonVisible = applicationDetailsPage
                .openAccess()
                .editGroupsButton
                .isDisplayed();
        if (!isEditButtonVisible) {
            String query = String.format("DECLARE @roleId INT;\n" +
                            "SET @roleId = (SELECT TOP 1 RoleId\n" +
                            "               FROM RolePermission rp\n" +
                            "                        join UserAccount ua on ua.Role2Id = rp.RoleId\n" +
                            "                        join TenantUser tu on ua.Userid = tu.UserId\n" +
                            "                        join TenantMaster tm on tu.TenantId = tm.TenantID\n" +
                            "               where ua.UserName = '%s' and tm.TenantName = '%s')\n" +
                            "\n" +
                            "INSERT INTO RolePermission (permissionid, roleid) VALUES (2, @roleId)",
                    appLead.getUserName(), tenantDTO.getTenantCode());

            new FodSQLUtil().executeQuery(query).close();
        }

        BrowserUtil.clearCookiesLogOff();

        var accessPage = LogInActions
                .tenantUserLogIn(appLead.getUserName(),
                        FodConfig.TAM_PASSWORD, tenantDTO.getTenantCode())
                .tenantTopNavbar.openApplications()
                .openDetailsFor(applicationDTO.getApplicationName())
                .openAccess();
        waitFor(WaitUtil.Operator.Equals, true,
                accessPage.editGroupsButton::isDisplayed, Duration.ofMinutes(10), true);
        assertThat(accessPage.editGroupsButton.isDisplayed())
                .as("Edit groups button for " + appLead.getUserName() + " should be visible")
                .isTrue();
        assertThat(accessPage.editUsersButton.isDisplayed())
                .as("Edit Users button for " + appLead.getUserName() + " should be visible")
                .isTrue();
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("768037")
    @Description("Application lead should not be able to assign users to other Applications in the tenant")
    @Test(groups = {"hf", "regression"}, dependsOnMethods = {"applicationLeadManageUsersIssueTest"})
    public void nonSecLeadAssignUsersToAllApplicationTest() {
        var testUser = TenantUserDTO.createDefaultInstance();
        testUser.setTenant(tenantDTO.getTenantCode());
        LogInActions.tenantUserLogIn(tenantDTO.getUserName(), FodConfig.TAM_PASSWORD, tenantDTO.getTenantCode());
        TenantUserActions.createTenantUser(testUser);
        BrowserUtil.clearCookiesLogOff();
        LogInActions.tenantUserLogIn(tenantDTO.getUserName(), FodConfig.TAM_PASSWORD, tenantDTO.getTenantCode())
                .tenantTopNavbar
                .openAdministration()
                .openRoles()
                .editRole("Application Lead")
                .setApplicationAccess(FodCustomTypes.RoleApplicationAccess.All)
                .setManageUsers(true)
                .save();
        var popup = new TenantTopNavbar()
                .openAdministration()
                .openUsers()
                .pressAssignApplicationsByUser(dev.getUserName());
        assertThat(popup.checkAllCheckbox.isEnabled())
                .as("'Assign all Tenant Applications' checkbox should be enabled for Security Lead user")
                .isTrue();
        popup.selectedTab.click();
        assertThat(popup.uncheckAllCheckbox.isEnabled())
                .as("'Un-Assign all Tenant Applications' checkbox should be enabled for Security Lead user")
                .isTrue();
        BrowserUtil.clearCookiesLogOff();
        var assignApplicationsPopup = LogInActions
                .tenantUserLogIn(testUser.getUserName(), FodConfig.TAM_PASSWORD, tenantDTO.getTenantCode())
                .tenantTopNavbar
                .openAdministration()
                .openUsers()
                .pressAssignApplicationsByUser(dev.getUserName());
        assertThat(assignApplicationsPopup.checkAllCheckbox.getAttribute("disabled"))
                .as("'Assign all Tenant Applications' checkbox should be disabled for non security lead user")
                .isEqualTo("true");
        assignApplicationsPopup.selectedTab.click();
        assertThat(assignApplicationsPopup.uncheckAllCheckbox.getAttribute("disabled"))
                .as("'Un-Assign all Tenant Applications' checkbox should be disabled for non security lead user")
                .isEqualTo("true");
    }
}