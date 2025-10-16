package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.UniqueRunTag;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.TenantDTO;
import com.fortify.fod.common.entities.TenantUserDTO;
import com.fortify.fod.ui.pages.tenant.TenantLoginPage;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.LogInActions;
import com.fortify.fod.ui.test.actions.TenantActions;
import com.fortify.fod.ui.test.actions.TenantGroupActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("sbehera3@opentext.com")
@Slf4j
public class AssignedUserGroupTest extends FodBaseTest {

    String groupName = null;
    String userName;
    TenantDTO tenantDTO;

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Create a couple of users and then assign them to a group")
    @Test(groups = {"hf", "regression"})
    public void addUserAndCreateGroupTest() {
        tenantDTO = TenantDTO.createDefaultInstance();
        TenantActions.createTenant(tenantDTO);
        BrowserUtil.clearCookiesLogOff();
        userName = "TestUser" + UniqueRunTag.generate();
        var tenantUser = TenantUserDTO.createDefaultInstance();
        groupName = "Group-" + UniqueRunTag.generate();

        LogInActions.tamUserLogin(tenantDTO);
        var userManagementPage = TenantLoginPage.navigate().tenantTopNavbar.openAdministration();
        var usersPage = userManagementPage.openUsers();

        for (int i = 0; i < 5; i++) {
            tenantUser.setUserName(userName + i);
            tenantUser.setUserEmail("testusermail" + UniqueRunTag.generate() + i + "@gmail.com");
            tenantUser.setFirstName("FirstName" + i);
            tenantUser.setLastName("LastName" + i);
            tenantUser.setRole(FodCustomTypes.TenantUserRole.Developer);
            usersPage.createUser(tenantUser);
            assertThat(usersPage.getUserNames())
                    .as("Verify that new user is present in table")
                    .contains(userName + i);
        }
        userManagementPage.openGroups();
        TenantGroupActions.createTenantGroup(groupName, "", true);
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("463007")
    @Description("The number of assigned users on a group includes the deleted users when adding a new user")
    @Test(dependsOnMethods = {"addUserAndCreateGroupTest"}, groups = {"hf", "regression"})
    public void validateDeletedUserTest() {
        LogInActions.tamUserLogin(tenantDTO);
        var userManagementPage = TenantLoginPage.navigate().tenantTopNavbar.openAdministration();

        var groupsPage = userManagementPage.openGroups();
        assertThat(groupsPage.getGroupByName(groupName).getAssignedUserCount())
                .as("Verify that new group is present in table")
                .isEqualTo(5);
        var usersPage = userManagementPage.openUsers();
        for (int i = 0; i < 3; i++) {
            usersPage.deleteUserByName(userName + i);
            assertThat(usersPage.getUserNames())
                    .as("Verify that new user is NOT present in table")
                    .doesNotContain(userName + i);
        }
        userManagementPage.openGroups();
        assertThat(groupsPage.getGroupByName(groupName).getAssignedUserCount())
                .as("Verify that new group is present in table")
                .isEqualTo(2);

        var addEditTenantUserPopup = userManagementPage.openUsers().pressAddUserBtn();
        assertThat(addEditTenantUserPopup.getAssignedUserCount(addEditTenantUserPopup.getGroupByName(groupName)))
                .as("Verify that new group is present in table")
                .isEqualTo(2);
    }
}