package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.UniqueRunTag;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.elements.Actions;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.StaticScanDTO;
import com.fortify.fod.common.utils.SshUtil;
import com.fortify.fod.ui.pages.admin.AdminLoginPage;
import com.fortify.fod.ui.pages.admin.statics.job.FileCell;
import com.fortify.fod.ui.pages.admin.statics.scanner_groups.StaticScannerGroupCell;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import com.fortify.fod.ui.test.actions.StaticScanActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import utils.MaxRetryCount;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

import static com.codeborne.selenide.Selenide.refresh;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("ysmal@opentext.com")
@Slf4j
public class HandleNestedStaticPayloadsTest extends FodBaseTest {

    //todo restrict this test only for QA11

    public ApplicationDTO applicationDTO;
    public StaticScanDTO staticScanDTO;
    String comment = "Auto-Comment";
    String scannerIp = "16.103.235.119";
    String selectedScannerName = "fodqa11-scanner5";
    String scannerUserName = "fod";
    String groupName = "Auto-Group" + UniqueRunTag.generate();

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("User should be able to view list of validating files on scanner machine")
    @Test(groups = {"regression"}, priority = 1)
    public void handleNestedStaticPayloadsTest() {
        applicationDTO = ApplicationDTO.createDefaultInstance();
        staticScanDTO = StaticScanDTO.createDefaultInstance();
        staticScanDTO.setFileToUpload("payloads/fod/WarFileNamedJar.zip");
        staticScanDTO.setLanguageLevel("1.8");
        staticScanDTO.setAuditPreference(FodCustomTypes.AuditPreference.Manual);

        ApplicationActions.createApplication(applicationDTO, defaultTenantDTO, true);
        StaticScanActions.createStaticScan(staticScanDTO, applicationDTO, FodCustomTypes.SetupScanPageStatus.Queued,
                FodCustomTypes.SetupScanPageStatus.InProgress);

        BrowserUtil.clearCookiesLogOff();
        String jobId;

        var scannerGroupsPage = LogInActions.adminLogIn()
                .adminTopNavbar.openStatic().openScannerGroups();

        if (scannerGroupsPage.getGroupByName(groupName) == null) {
            var addGroupPopup = scannerGroupsPage.pressAddGroup();
            assertThat(addGroupPopup.popupElement.isDisplayed())
                    .as("Popup should be opened")
                    .isTrue();

            var group = (StaticScannerGroupCell) addGroupPopup.setGroupName(groupName)
                    .setComments(comment)
                    .pressSave()
                    .getGroupByName(groupName);

            assertThat(group).as("Group should be created").isNotNull();
            assertThat(group.getScanners())
                    .as("Created group shouldn't have any assigned scanners")
                    .isEmpty();

            group.pressEditMembers().selectAvailableScanners(selectedScannerName).pressAdd().pressSave();

            var defaultGroup = scannerGroupsPage.getGroupByName("Default").getScanners();
            var createdGroup = scannerGroupsPage.getGroupByName(groupName).getScanners();

            assertThat(defaultGroup)
                    .as("Default group shouldn't contain scanner: " + selectedScannerName)
                    .doesNotContain(selectedScannerName);
            assertThat(createdGroup)
                    .as(createdGroup + " group should contain scanner: " + selectedScannerName)
                    .contains(selectedScannerName);
        }

        var job = scannerGroupsPage.adminTopNavbar.openStatic()
                .findScanByAppName(applicationDTO.getApplicationName())
                .getLastJob()
                .waitStatusChanged(FodCustomTypes.JobStatus.Pending);

        if (!job.getStatus().equals(FodCustomTypes.JobStatus.Success.getTypeValue())) {
            job.clickAction(Actions.JobAction.AbortJob);
        }

        job = job.clickClone().setRescanReason("No reason 1")
                .chooseScannerGroup(groupName)
                .pressRescan().findScanByAppName(applicationDTO.getApplicationName())
                .getLastJob()
                .waitForJobStatus(30, FodCustomTypes.JobStatus.Success);


        var filesPage = job.openDetails().openScannedFiles();
        filesPage.waitUntilBottomMessageDisappears(5);
        var scannedFilesList = filesPage.getAllFiles();
        var jsFiles = scannedFilesList.stream().map(FileCell::getName).filter(name -> name
                .endsWith(".js")).collect(Collectors.toList());

        log.info(Arrays.toString(jsFiles.toArray()));
        assertThat(jsFiles)
                .as("js files count should be equal to 13")
                .hasSize(13);

        job = filesPage.adminTopNavbar.openStatic().findScanByAppName(applicationDTO.getApplicationName())
                .getLastJob().clickClone().setRescanReason("No reason 1")
                .chooseScannerGroup(groupName)
                .pressRescan().findScanByAppName(applicationDTO.getApplicationName())
                .getLastJob()
                .waitStatusChanged(FodCustomTypes.JobStatus.Pending);

        jobId = job.getId();

        var listOfFolders = new ArrayList<>(Arrays.asList(jsFiles.get(jsFiles.size() - 1).split("/")));
        var index = listOfFolders.size() - 1;
        listOfFolders.remove(index);
        var indexOfFolder = listOfFolders.indexOf(listOfFolders.stream()
                .filter(x -> x.contains(defaultTenantDTO.getTenantCode())).findFirst().orElse(null));
        var replaced = listOfFolders.get(indexOfFolder).split("-");
        replaced[1] = jobId;
        var newJobIdForPath = String.join("-", replaced);
        listOfFolders.set(indexOfFolder, newJobIdForPath);
        var filePathNew = String.join("/", listOfFolders) + "/";

        var command = "cd " + filePathNew + " && ls -a";

        String result;
        var endTime = LocalTime.now().plusMinutes(2);
        do {
            result = SshUtil.executeCommand(scannerIp, scannerUserName, FodConfig.LINUX_SCANNER_PASSWORD, command);
            if (!result.isEmpty()) break;
        } while (LocalTime.now().isBefore(endTime));

        assertThat(result).isNotBlank();
    }

    @AfterClass
    @Description("Delete created group")
    public void deleteCreatedGroup() {
        setupDriver("deleteGroupTest");
        var scannerGroupsPage = AdminLoginPage.navigate()
                .login(FodConfig.ADMIN_USER_NAME, FodConfig.ADMIN_PASSWORD)
                .adminTopNavbar.openStatic().openScannerGroups();

        var groupToDelete = scannerGroupsPage.getGroupByName(groupName);
        if (groupToDelete != null) {
            var editMembersPopup = scannerGroupsPage.getGroupByName(groupName)
                    .pressEditMembers();
            var selectedScanners = editMembersPopup.getSelectedScanners();

            if (!selectedScanners.isEmpty()) {
                var selectedScannerName = selectedScanners.get(0);
                editMembersPopup.selectSelectedScanners(selectedScannerName)
                        .pressRemove();

                assertThat(editMembersPopup.getSelectedScanners())
                        .as("Selected scanners count should be 0")
                        .isEmpty();
                editMembersPopup.pressSave();
            } else editMembersPopup.pressClose();

            var defaultGroup = scannerGroupsPage.getGroupByName("Default").getScanners();
            var createdGroup = scannerGroupsPage.getGroupByName(groupName).getScanners();

            assertThat(defaultGroup)
                    .as("Default group should contain scanner: " + selectedScannerName)
                    .contains(selectedScannerName);
            assertThat(createdGroup)
                    .as(createdGroup + " group shouldn't contain scanner: " + selectedScannerName)
                    .doesNotContain(selectedScannerName);

            var modal = scannerGroupsPage.getGroupByName(groupName).pressDelete();
            modal.clickButtonByText("Yes");
            refresh();
            assertThat(scannerGroupsPage.getGroupByName(groupName))
                    .as(groupName + " group should be deleted")
                    .isNull();
        }
        attachTestArtifacts();
    }
}
