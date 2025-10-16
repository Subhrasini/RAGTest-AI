package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.UniqueRunTag;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.elements.Actions;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.StaticScanDTO;
import com.fortify.fod.ui.pages.admin.AdminLoginPage;
import com.fortify.fod.ui.pages.admin.statics.scan_jobs.ScanJobsPage;
import com.fortify.fod.ui.pages.admin.statics.scan_jobs.StaticJobCell;
import com.fortify.fod.ui.pages.admin.statics.scan_jobs.popups.CloneJobPopup;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.StaticScanActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import utils.MaxRetryCount;

import static com.codeborne.selenide.Selenide.*;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("ysmal@opentext.com")
@Slf4j
public class AssignClonedStaticJobToScannerGroupTest extends FodBaseTest {

    ApplicationDTO applicationDTO;
    String groupName = "Auto-Group" + UniqueRunTag.generate();
    String confirmMessage = "Are you sure you want to delete the scanner group?";
    String warningMessage = "The selected scanner group currently has no scanners.";
    String reasonToRescan = "Auto-Reason";
    String comment = "Auto-Comment";

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Admin user should be able to create scanner group and assign to static job")
    @Test(groups = {"regression"})
    public void assignClonedStaticJobToScannerGroupTest() {
        applicationDTO = ApplicationActions.createApplication(defaultTenantDTO);
        StaticScanDTO scanDTO = StaticScanDTO.createDefaultInstance();
        scanDTO.setFileToUpload("payloads/fod/10JavaDefects_Small(OS).zip");

        StaticScanActions.createStaticScan(scanDTO, applicationDTO,
                FodCustomTypes.SetupScanPageStatus.InProgress);

        BrowserUtil.clearCookiesLogOff();

        AdminLoginPage.navigate().login(FodConfig.ADMIN_USER_NAME, FodConfig.ADMIN_PASSWORD)
                .adminTopNavbar.openStatic();

        var jobsPage = page(ScanJobsPage.class);
        var scan = jobsPage.findScanByAppName(applicationDTO.getApplicationName());
        var scanId = scan.getId();
        var defJob = scan.getLastJob();

        if (defJob.getStatus().equals(FodCustomTypes.JobStatus.Scanning.getTypeValue())
                || defJob.getStatus().equals(FodCustomTypes.JobStatus.Pending.getTypeValue())) {
            defJob.clickAction(Actions.JobAction.AbortJob);
        }

        defJob.clickClone().setRescanReason(reasonToRescan).pressRescan();
        var job = new StaticJobCell(jobsPage.findScanByScanId(scanId)
                .getLastJob().getId())
                .clickAction(Actions.JobAction.AbortJob)
                .waitForJobStatus(FodCustomTypes.JobStatus.Aborted, FodCustomTypes.JobStatus.Aborting);

        var scannerGroupsPage = jobsPage.openScannerGroups();

        if (scannerGroupsPage.getGroupByName(groupName) == null) {
            var addGroupPopup = scannerGroupsPage.pressAddGroup();

            assertThat(addGroupPopup.popupElement.isDisplayed())
                    .as("Popup should be opened")
                    .isTrue();

            var group = addGroupPopup.setGroupName(groupName)
                    .setComments(comment)
                    .pressSave()
                    .getGroupByName(groupName);

            assertThat(group).as("Group should be created").isNotNull();
            assertThat(group.getScanners())
                    .as("Created group shouldn't have any assigned scanners")
                    .isEmpty();
        }

        scannerGroupsPage.openScanJobs();
        new StaticJobCell(jobsPage.findScanByScanId(scanId).getLastJob().getId()).clickClone();

        var cloneJobPopup = page(CloneJobPopup.class);

        assertThat(cloneJobPopup.popupElement.isDisplayed())
                .as("Popup should be opened")
                .isTrue();

        cloneJobPopup.chooseScannerGroup(groupName)
                .setRescanReason(reasonToRescan);

        assertThat(cloneJobPopup.emptyGroupWarning.isDisplayed())
                .as("Warning message should be displayed when group has no one assigned scanner")
                .isTrue();
        assertThat(cloneJobPopup.emptyGroupWarning.text())
                .as("Warning message should be equal to: " + warningMessage)
                .isEqualTo(warningMessage);

        cloneJobPopup.pressRescan();

        job = new StaticJobCell(jobsPage.findScanByScanId(scanId).getLastJob().getId());

        String status = job.getStatus();
        String agent = job.getAgent();
        String owner = job.getOwner();
        String scanGroup = job.getScannerGroup();
        String scanGroupColor = job.getScannerGroupElement().getCssValue("background-color");

        assertThat(status).isEqualTo("Pending");
        assertThat(agent).isEmpty();
        assertThat(owner).isEqualTo("NA");
        assertThat(scanGroup).isEqualTo(groupName);
        assertThat(scanGroupColor).isEqualTo("rgba(249, 156, 28, 1)");

        // From Test case
        // Set timeout 2 minutes. -> "Validate: Status of job is still 'Pending'".
        sleep(120000);

        job = new StaticJobCell(jobsPage.findScanByScanId(scanId).getLastJob().getId());
        assertThat(job.getStatus())
                .as("Status should be 'Pending' after 2 minutes")
                .isEqualTo("Pending");

        jobsPage.openScannerGroups();
        scannerGroupsPage.waitForAvailableScanner();
        var editMembersPopup = scannerGroupsPage.getGroupByName(groupName)
                .pressEditMembers();

        assertThat(editMembersPopup.popupElement.isDisplayed())
                .as("Edit members popup should be opened")
                .isTrue();

        var availScanner = editMembersPopup.getAvailableScanners().get(0);
        editMembersPopup.selectAvailableScanners(availScanner).pressAdd();
        assertThat(editMembersPopup.getSelectedScanners()).isNotNull();
        var selectedScannerName = editMembersPopup.getSelectedScanners().get(0);
        editMembersPopup.pressSave();

        var defaultGroup = scannerGroupsPage.getGroupByName("Default").getScanners();
        var createdGroup = scannerGroupsPage.getGroupByName(groupName).getScanners();

        assertThat(defaultGroup)
                .as("Default group shouldn't contain scanner: " + selectedScannerName)
                .doesNotContain(selectedScannerName);
        assertThat(createdGroup)
                .as(createdGroup + " group should contain scanner: " + selectedScannerName)
                .contains(selectedScannerName);

        scannerGroupsPage.openScanJobs();

        job = new StaticJobCell(jobsPage.findScanByScanId(scanId).getLastJob().getId())
                .waitStatusChanged(FodCustomTypes.JobStatus.Pending);

        assertThat(job.getStatus())
                .as("Status should be changed")
                .isNotEqualTo(FodCustomTypes.JobStatus.Pending.getTypeValue());
    }

    @Description("Delete created group")
    @AfterClass
    public void deleteGroupTest() {
        setupDriver("deleteGroupTest");
        var scannerGroupsPage = AdminLoginPage.navigate().login(FodConfig.ADMIN_USER_NAME, FodConfig.ADMIN_PASSWORD)
                .adminTopNavbar.openStatic().openScannerGroups();

        var groupToDelete = scannerGroupsPage.getGroupByName(groupName);
        if (groupToDelete != null) {
            var editMembersPopup = scannerGroupsPage.getGroupByName(groupName)
                    .pressEditMembers();
            var selectedScannerName = editMembersPopup.getSelectedScanners().get(0);
            editMembersPopup.selectSelectedScanners(selectedScannerName)
                    .pressRemove();

            assertThat(editMembersPopup.getSelectedScanners().size())
                    .as("Selected scanners count should be 0")
                    .isZero();
            editMembersPopup.pressSave();

            var defaultGroup = scannerGroupsPage.getGroupByName("Default").getScanners();
            var createdGroup = scannerGroupsPage.getGroupByName(groupName).getScanners();

            assertThat(defaultGroup)
                    .as("Default group should contain scanner: " + selectedScannerName)
                    .contains(selectedScannerName);
            assertThat(createdGroup)
                    .as(createdGroup + " group shouldn't contain scanner: " + selectedScannerName)
                    .doesNotContain(selectedScannerName);

            var modal = scannerGroupsPage.getGroupByName(groupName).pressDelete();
            assertThat(modal.getMessage()).isEqualTo(confirmMessage);
            modal.clickButtonByText("Yes");
            refresh();
            assertThat(scannerGroupsPage.getGroupByName(groupName))
                    .as(groupName + " group should be deleted")
                    .isNull();
        }
        attachTestArtifacts();
    }
}
