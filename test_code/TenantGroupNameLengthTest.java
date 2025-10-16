package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.ui.pages.tenant.administration.user_management.groups.GroupsPage;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.LogInActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("tmagill@opentext.com")
@FodBacklogItem("797043")
@Slf4j
public class TenantGroupNameLengthTest extends FodBaseTest {
    String longGroupName;
    String choppedGroupName;
    String appendedGroupName;

    @MaxRetryCount(1)
    @Test(groups = {"regression"})
    @Description("Tests the Name field of Group Edit Popup when using names with length of 256 chars")
    public void createLongGroupNameTest() {
        longGroupName = "GroupNameWith256Characters-".concat("x".repeat(229));
        var groupsPage = getGroupsPage();
        groupsPage.pressAddGroup().setName(longGroupName).pressSave();
        assertThat(longGroupName)
                .as("Checking for length of group name - should be 256")
                .hasSize(256);
        assertThat(groupsPage.getGroupByName(longGroupName).getName())
                .as("Ensuring name is valid and can be retrieved")
                .isNotEmpty();
    }

    @MaxRetryCount(1)
    @Test(groups = {"regression"}, dependsOnMethods = {"createLongGroupNameTest"})
    @Description("Tests the Name field of Group Edit Popup using names edited to length of 255")
    public void chopEditGroupStringTest() {
        choppedGroupName = StringUtils.chop(longGroupName);
        var groupsPage = editGroup(longGroupName, false);
        assertThat(choppedGroupName)
                .as("After input field is edited, length of name should be 255")
                .hasSize(255);
        assertThat(groupsPage.getGroupByName(choppedGroupName).getName())
                .as("Comparing " + choppedGroupName + "to " + longGroupName + "; they should be the same")
                .isEqualTo(StringUtils.chop(longGroupName));
    }

    @MaxRetryCount(1)
    @Test(groups = {"regression"}, dependsOnMethods = {"chopEditGroupStringTest"})
    @Description("Tests the Name field of Group Edit Popup when appending Group Name to 256 chars in length")
    public void appendEditGroupStringTest() {
        appendedGroupName = choppedGroupName.concat("x");
        var groupsPage = editGroup(choppedGroupName, true);
        assertThat(appendedGroupName).
                as("Group name " + appendedGroupName + " should be 256 characters in length").
                hasSize(256);
        assertThat(groupsPage.getGroupByName(appendedGroupName).getName())
                .as(" Comparing " + appendedGroupName + " to " + choppedGroupName + "x; They should be the same")
                .isEqualTo(choppedGroupName + "x");
    }

    @MaxRetryCount(1)
    @Test(groups = {"regression"}, dependsOnMethods = {"appendEditGroupStringTest"})
    @Description("Tests removal of Group with name 256 chars in length")
    public void removeLongGroupNameTest() {
        var groupsPage = getGroupsPage();
        assertThat(longGroupName)
                .as("Length of " + longGroupName + " should be 256")
                .hasSize(256);
        groupsPage.getGroupByName(longGroupName)
                .pressDelete()
                .pressYes();
        if (groupsPage.getTableRowsFound().equals("0 found")) {
            assertThat(groupsPage.getTableRowsFound())
                    .as("Validates if group was only group on tenant, it is no longer there")
                    .isEqualTo("0 found");
        } else {
            assertThat(groupsPage.getGroupByName(longGroupName))
                    .as("Group " + longGroupName + " should no longer exist")
                    .isNull();
        }
    }

    GroupsPage getGroupsPage() {
        return LogInActions.tamUserLogin(defaultTenantDTO).tenantTopNavbar.openAdministration().openGroups();
    }

    GroupsPage editGroup(String groupName, Boolean isAppend) {
        var groupsPage = getGroupsPage();
        var editGroupPopup = groupsPage
                .getGroupByName(groupName)
                .pressEdit();
        if (isAppend)
            editGroupPopup.nameInput.sendKeys("x");
        else editGroupPopup.nameInput.sendKeys("\b");
        editGroupPopup.pressSave();
        return groupsPage;

    }

    @AfterClass()
    public void removeGroups() {
        setupDriver("removeGroups");
        var groups = new ArrayList<String>() {{
            add(longGroupName);
            add(choppedGroupName);
        }};
        var groupsPage = LogInActions.tamUserLogin(defaultTenantDTO).tenantTopNavbar.openAdministration().openGroups();
        groups.forEach(group -> {
            try {
                groupsPage.getGroupByName(group).pressDelete().pressYes();
            } catch (Exception | Error e) {
                log.error("Group does not exist");
            }
        });
        BrowserUtil.clearCookiesLogOff();
        attachTestArtifacts();
    }
}
