package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.UniqueRunTag;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.elements.Actions;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.ReleaseDTO;
import com.fortify.fod.common.entities.StaticScanDTO;
import com.fortify.fod.ui.pages.admin.AdminLoginPage;
import com.fortify.fod.ui.pages.admin.statics.scan_jobs.ScanJobsPage;
import com.fortify.fod.ui.pages.admin.statics.scanner_groups.StaticScannerGroupCell;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.ReleaseActions;
import com.fortify.fod.ui.test.actions.StaticScanActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.codeborne.selenide.Selenide.page;
import static com.codeborne.selenide.Selenide.refresh;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("sbehera3@opentext.com")
@Slf4j
public class ScanCentralTrendingArgsTest extends FodBaseTest {

    public ApplicationDTO applicationDTO;
    public ReleaseDTO releaseDTO;
    public StaticScanDTO staticScanDTO;
    Pattern p = Pattern.compile("\\d+");
    Matcher m = p.matcher(FodConfig.TEST_ENDPOINT_ADMIN);
    String scannerName = m.find() ? "FODQA" + m.group(0) + "-SCAN1" : "FODQA11-SCAN1";
    public String scannerGroupName;

    public String fileName = "payloads/fod/webgoat-sc.zip";
    public String rescanReason = "ForTesting";
    public String scanArgs = "if (string.IsNullOrEmpty(scanArgs) || string.IsNullOrEmpty(buildArgs))\n" +
            "{\n" +
            "//use defaults\n" +
            "var result = ScanArgs.GetDefaultArgs(technologyTypeId, technologyVersionTypeId);\n" +
            "scanArgs = isScanCentralPackage ? result.ScanCentralScanArgs : result.ScanArgs;\n" +
            "buildArgs = isScanCentralPackage ? result.ScanCentralTranslateArgs : result.TranslateArgs;\n" +
            "scannerGroupId = result.ScannerGroupId;\n" +
            "}";

    @BeforeClass
    public void createScannerGroup() {
        setupDriver("createScannerGroup");
        log.info(scannerName);
        scannerGroupName = "AutoGroup" + UniqueRunTag.generate();
        var scannerGroupsPage = AdminLoginPage.navigate()
                .login(FodConfig.ADMIN_USER_NAME, FodConfig.ADMIN_PASSWORD)
                .adminTopNavbar.openStatic().openScannerGroups().pressAddGroup()
                .setGroupName(scannerGroupName)
                .pressSave();
        var group = (StaticScannerGroupCell) scannerGroupsPage.getGroupByName(scannerGroupName);
        group.pressEditMembers().selectAvailableScanners(scannerName).pressAdd().pressSave();

        attachTestArtifacts();
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("533003")
    @FodBacklogItem("797023")
    @Description("Bug in logic causing Scan Central payloads to lose trending arguments")
    @Test(groups = {"hf", "regression"})
    public void validateExcludeStatementTest() {
        releaseDTO = ReleaseDTO.createDefaultInstance();
        staticScanDTO = StaticScanDTO.createDefaultInstance();
        applicationDTO = ApplicationActions.createApplication(defaultTenantDTO, true);
        ReleaseActions.createRelease(applicationDTO, releaseDTO);
        staticScanDTO.setAuditPreference(FodCustomTypes.AuditPreference.Manual);
        staticScanDTO.setFileToUpload(fileName);
        StaticScanActions.createStaticScan(staticScanDTO, applicationDTO, FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();

        AdminLoginPage.navigate().login(FodConfig.ADMIN_USER_NAME, FodConfig.ADMIN_PASSWORD)
                .adminTopNavbar.openStatic();
        var scanJobsPage = page(ScanJobsPage.class);
        var firstJob = scanJobsPage.findScanByAppName(applicationDTO.getApplicationName(), true).getJobs();
        assertThat(firstJob)
                .as("Validate only one scan is in progress under the release")
                .hasSize(1);
        firstJob.get(0).clickAction(Actions.JobAction.AbortJob)
                .clickClone()
                .setScanArgsElement(scanArgs)
                .setRescanReason(rescanReason)
                .chooseScannerGroup(scannerGroupName)
                .pressRescan();
        var allJobs = scanJobsPage.findScanByAppName(applicationDTO.getApplicationName(), true).getJobs();
        assertThat(allJobs)
                .as("Validate another scan has started under the same release with the same payload")
                .hasSize(2);
        allJobs.get(1).waitForJobStatus(FodCustomTypes.JobStatus.Scanning);
        var cloneJobPopup = allJobs.get(1).clickClone();
        assertThat(cloneJobPopup.getScanArgsElement())
                .as("Validate the exclude statement still be present in the scan/build args")
                .isEqualTo(scanArgs);

        cloneJobPopup.pressClose().getLatestScanByAppDto(applicationDTO).getLastJob()
                .clickAction(Actions.JobAction.AbortJob);
    }

    @AfterClass
    @Description("Delete created group")
    public void deleteCreatedGroup() {
        setupDriver("deleteGroupTest");
        var scannerGroupsPage = AdminLoginPage.navigate()
                .login(FodConfig.ADMIN_USER_NAME, FodConfig.ADMIN_PASSWORD)
                .adminTopNavbar.openStatic().openScannerGroups();

        var groupToDelete = scannerGroupsPage.getGroupByName(scannerGroupName);
        if (groupToDelete != null) {
            var editMembersPopup = scannerGroupsPage.getGroupByName(scannerGroupName)
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

            scannerGroupsPage.getGroupByName(scannerGroupName).pressDelete().pressYes();
            refresh();
            assertThat(scannerGroupsPage.getGroupByName(scannerGroupName))
                    .as(scannerGroupName + " group should be deleted")
                    .isNull();
        }
        attachTestArtifacts();
    }
}
