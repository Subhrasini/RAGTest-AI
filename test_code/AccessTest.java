package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.UniqueRunTag;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.elements.Table;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.TenantDTO;
import com.fortify.fod.common.entities.TenantUserDTO;
import com.fortify.fod.ui.pages.tenant.administration.user_management.groups.GroupsPage;
import com.fortify.fod.ui.pages.tenant.administration.user_management.groups.popups.GroupUserCell;
import com.fortify.fod.ui.pages.tenant.applications.application.access.AccessPage;
import com.fortify.fod.ui.pages.tenant.applications.application.access.UserApplicationAccessCell;
import com.fortify.fod.ui.pages.tenant.applications.application.access.popups.AssignedGroupCell;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.MaxRetryCount;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("ysmal@opentext.com")
@Slf4j
public class AccessTest extends FodBaseTest {

    TenantUserDTO developer;
    TenantUserDTO executive;
    TenantUserDTO applicationLead;
    TenantUserDTO leadDeveloper;
    TenantUserDTO reviewer;
    TenantUserDTO securityLead;
    TenantDTO tenantDTO;
    ApplicationDTO applicationDTO;
    String groupName;

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Create tenant/application/tenant and all types of tenant users")
    @Test(groups = {"regression"})
    public void prepareTestData() {
        tenantDTO = TenantDTO.createDefaultInstance();
        securityLead = TenantUserDTO.createDefaultInstance();
        securityLead.setUserName(tenantDTO.getUserName());
        securityLead.setFirstName(tenantDTO.getFirstName());
        securityLead.setLastName(tenantDTO.getLastName());
        securityLead.setRole(FodCustomTypes.TenantUserRole.SecurityLead);
        securityLead.setTenant(tenantDTO.getTenantName());
        developer = TenantUserDTO.createDefaultInstance();
        developer.setTenant(tenantDTO.getTenantName());
        developer.setRole(FodCustomTypes.TenantUserRole.Developer);
        executive = TenantUserDTO.createDefaultInstance();
        executive.setTenant(tenantDTO.getTenantName());
        executive.setRole(FodCustomTypes.TenantUserRole.Executive);
        applicationLead = TenantUserDTO.createDefaultInstance();
        applicationLead.setTenant(tenantDTO.getTenantName());
        applicationLead.setRole(FodCustomTypes.TenantUserRole.ApplicationLead);
        leadDeveloper = TenantUserDTO.createDefaultInstance();
        leadDeveloper.setTenant(tenantDTO.getTenantName());
        leadDeveloper.setRole(FodCustomTypes.TenantUserRole.LeadDeveloper);
        reviewer = TenantUserDTO.createDefaultInstance();
        reviewer.setTenant(tenantDTO.getTenantName());
        reviewer.setRole(FodCustomTypes.TenantUserRole.Reviewer);

        TenantActions.createTenant(tenantDTO);
        BrowserUtil.clearCookiesLogOff();
        applicationDTO = ApplicationActions.createApplication(tenantDTO);
        TenantUserActions.createTenantUsers(tenantDTO, developer, executive,
                reviewer, applicationLead, leadDeveloper);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate Access page functionality for all types of users")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void accessTest() {
        groupName = "Group-" + UniqueRunTag.generate();
        var groupsPage = (GroupsPage) LogInActions.tamUserLogin(tenantDTO)
                .tenantTopNavbar.openAdministration().openGroups().pressAddGroup()
                .setName(groupName)
                .assignUser(developer)
                .assignUser(executive)
                .pressSave();

        assertThat(groupsPage.getGroupByName(groupName))
                .as("Group should be created with name: " + groupName)
                .isNotNull();

        var assignedCount = groupsPage.getGroupByName(groupName)
                .pressAssignApplication()
                .assignApplication(applicationDTO)
                .pressSave()
                .getGroupByName(groupName)
                .getAssignedApplicationsCount();

        assertThat(assignedCount)
                .as("Application should be assigned")
                .isEqualTo(1);

        var accessPage = groupsPage.tenantTopNavbar.openApplications().openDetailsFor(applicationDTO.getApplicationName())
                .openAccess();

        assertThat(accessPage.getUserByDto(securityLead)).isNotNull();
        assertThat(accessPage.getUserByDto(reviewer)).isNotNull();

        assertThat(new Table(accessPage.tableElement).getColumnHeaders())
                .contains("Last Name", "First Name", "Email", "Role Name", "Access Method");

        var editUsersPopup = accessPage.pressEditUsersButton();

        List<GroupUserCell> list = editUsersPopup.getAllAvailableUsers();

        assertThat(list.stream()
                .map(GroupUserCell::getFirstName).collect(Collectors.toList()))
                .as("Assign Users pop-up should be displayed with 4 available to assign users")
                .hasSize(4)
                .contains(applicationLead.getFirstName(),
                        developer.getFirstName(),
                        executive.getFirstName(),
                        leadDeveloper.getFirstName());

        accessPage = (AccessPage) editUsersPopup.setAssignAll(true).pressSave();
        assertThat(accessPage.getAllUsers())
                .as("6 users should be displayed in access table")
                .hasSize(6);

        accessPage.findWithSearchBox("Lead");

        assertThat(accessPage.getAllUsers().stream()
                .map(UserApplicationAccessCell::getFirstName).collect(Collectors.toList()))
                .as("Search by 'Lead' key. Should be only 3 users displayed in the table with Lead prefix")
                .hasSize(3)
                .contains(applicationLead.getFirstName(),
                        leadDeveloper.getFirstName(),
                        securityLead.getFirstName());

        accessPage.clearSearchBox();
        accessPage = (AccessPage) accessPage.pressEditUsersButton().setUnAssignAll(true).pressSave();
        accessPage = accessPage.pressEditGroupsButton().setUnAssignAll(true).pressSave();

        assertThat(accessPage.getAllUsers())
                .as("2 users should be displayed in access table")
                .hasSize(2);
        var editGroupPopup = accessPage.pressEditGroupsButton();
        assertThat(editGroupPopup.popup.isDisplayed()).as("Popup should be displayed")
                .isTrue();

        assertThat(editGroupPopup.getAllAvailableGroups().stream()
                .map(AssignedGroupCell::getGroupName).collect(Collectors.toList()))
                .as("Only created group should be present in the table")
                .hasSize(1)
                .contains(groupName);

        accessPage = editGroupPopup.setAssignAll(true).pressSave();
        var usersList = accessPage.getAllUsers();
        assertThat(usersList)
                .as("4 users should be present in table")
                .hasSize(4);

        assertThat(accessPage.getUserByDto(developer).getAccessMethod())
                .as("Developer access method should be: Group(s): " + groupName)
                .isEqualTo("Group(s): " + groupName);
        assertThat(accessPage.getUserByDto(executive).getAccessMethod())
                .as("Executive access method should be: Group(s): " + groupName)
                .isEqualTo("Group(s): " + groupName);
        assertThat(accessPage.getUserByDto(reviewer).getAccessMethod())
                .as("Reviewer access method should be: Role")
                .isEqualTo("Role");
        assertThat(accessPage.getUserByDto(securityLead).getAccessMethod())
                .as("Security Lead access method should be: Role")
                .isEqualTo("Role");

        new PagingActions().validatePaging();
    }
}
