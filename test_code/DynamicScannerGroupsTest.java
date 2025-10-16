package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.UniqueRunTag;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.ui.pages.admin.AdminLoginPage;
import com.fortify.fod.ui.pages.admin.dynamic.DynamicQueuePage;
import com.fortify.fod.ui.pages.admin.dynamic.scanner_groups.DynamicScannerGroupCell;
import com.fortify.fod.ui.pages.admin.dynamic.scanner_groups.DynamicScannerGroupsPage;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.Ordering;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import static com.codeborne.selenide.Selenide.page;
import static com.codeborne.selenide.Selenide.refresh;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("ysmal@opentext.com")
@Slf4j
public class DynamicScannerGroupsTest extends FodBaseTest {

    String groupName;

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Admin user should be able to create dynamic scanner group and assign scanner")
    @Test(groups = {"regression"}, priority = 1)
    public void dynamicScannerGroupsTest() {
        String tag = UniqueRunTag.generate();
        groupName = "Auto-Group-" + tag;
        String comment = "Auto-Comment-" + tag;
        String[] groupsForOrdering =
                new String[]{"AGroup-" + tag, "BGroup-" + tag, "CGroup-" + tag, "1Group-" + tag, "2Group-" + tag};
        AdminLoginPage.navigate().login(FodConfig.ADMIN_USER_NAME, FodConfig.ADMIN_PASSWORD)
                .adminTopNavbar.openDynamic();
        page(DynamicQueuePage.class).openScannerGroups();
        var dynamicScannerGroupsPage = page(DynamicScannerGroupsPage.class);

        var addEditDynamicScannerGroupPopup = dynamicScannerGroupsPage.pressAddGroup();

        assertThat(addEditDynamicScannerGroupPopup.popupElement.isDisplayed())
                .as("Popup should be visible")
                .isTrue();

        addEditDynamicScannerGroupPopup
                .setRestricted(true)
                .setGroupName(groupName)
                .setComments(comment);

        addEditDynamicScannerGroupPopup.pressSave();
        var createdGroup = (DynamicScannerGroupCell) dynamicScannerGroupsPage.getGroupByName(groupName);
        assertThat(dynamicScannerGroupsPage.getGroupByName(groupName))
                .as("Group should be created with name: " + groupName)
                .isNotNull();
        createdGroup.pressEdit();
        assertThat(addEditDynamicScannerGroupPopup.getComment())
                .as("Comment should be saved and equal: " + comment)
                .isEqualTo(comment);

        addEditDynamicScannerGroupPopup.pressClose();
        assertThat(createdGroup.getRestricted()).isEqualTo("Yes");

        dynamicScannerGroupsPage.waitForAvailableScanner();
        var editMembersPopup = dynamicScannerGroupsPage
                .getGroupByName(groupName)
                .pressEditMembers();
        var availScanner = editMembersPopup.getAvailableScanners().get(0);
        editMembersPopup
                .selectAvailableScanners(availScanner)
                .pressAdd()
                .pressSave();

        refresh();
        assertThat(dynamicScannerGroupsPage.getGroupByName(groupName).getScanners())
                .as("Scanner should be added")
                .contains(availScanner);

        dynamicScannerGroupsPage.getGroupByName(groupName)
                .pressEditMembers()
                .selectSelectedScanners(availScanner)
                .pressRemove()
                .pressSave();
        assertThat(dynamicScannerGroupsPage.getGroupByName(groupName).getScanners())
                .as("Scanner should be added")
                .doesNotContain(availScanner);

        for (var group : groupsForOrdering) {
            dynamicScannerGroupsPage.pressAddGroup()
                    .setRestricted(true)
                    .setGroupName(group)
                    .setComments(comment)
                    .pressSave();

            refresh();
            assertThat(dynamicScannerGroupsPage.getGroupByName(group))
                    .as("Group should be created with name: " + group)
                    .isNotNull();
        }

        var ordering = new Ordering(dynamicScannerGroupsPage.getTable());

        ordering.verifyOrderForColumn("Group Name");
        ordering.verifyOrderForColumn("Modified Date");

        for (var group : groupsForOrdering) {
            dynamicScannerGroupsPage
                    .getGroupByName(group)
                    .pressDelete().clickButtonByText("Yes");
            refresh();
            assertThat(dynamicScannerGroupsPage.getGroupByName(group))
                    .as("Group should be deleted: " + group)
                    .isNull();
        }
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Admin user should be able to delete dynamic scanner group and unassign scanner")
    @Test(groups = {"regression"}, priority = 2)
    public void deleteScannerGroupTest() {
        AdminLoginPage.navigate().login(FodConfig.ADMIN_USER_NAME, FodConfig.ADMIN_PASSWORD)
                .adminTopNavbar.openDynamic();
        page(DynamicQueuePage.class).openScannerGroups();

        var scannerGroupsPage = page(DynamicScannerGroupsPage.class);
        var createdGroup = scannerGroupsPage.getGroupByName(groupName);

        if (!createdGroup.getScanners().isEmpty()) {
            var editMembers = createdGroup.pressEditMembers();
            editMembers
                    .selectSelectedScanners(editMembers.getSelectedScanners().get(0))
                    .pressRemove()
                    .pressSave();
        }

        createdGroup = scannerGroupsPage.getGroupByName(groupName);
        createdGroup.pressDelete().clickButtonByText("Yes");
        refresh();
        assertThat(scannerGroupsPage.getGroupByName(groupName))
                .as("Group should be deleted: " + groupName)
                .isNull();
    }
}
