package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.WaitUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.ReleaseDTO;
import com.fortify.fod.common.entities.StaticScanDTO;
import com.fortify.fod.ui.pages.admin.AdminLoginPage;
import com.fortify.fod.ui.pages.admin.statics.scan_jobs.ScanJobsPage;
import com.fortify.fod.ui.pages.admin.statics.scan_jobs.StaticScanCell;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.ReleaseActions;
import com.fortify.fod.ui.test.actions.StaticScanActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;

import java.util.Comparator;

import static com.codeborne.selenide.Selenide.page;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("ysmal@opentext.com")
@Slf4j
public class QueueStaticScansTest extends FodBaseTest {

    public ApplicationDTO applicationDTO, app2;
    public ReleaseDTO firstRelease;
    public ReleaseDTO secondRelease;
    public ReleaseDTO thirdRelease;
    public StaticScanDTO staticScanDTO;

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Admin user should be able validate static scans statuses")
    @Test(groups = {"regression"})
    public void queueStaticScansTest() {
        applicationDTO = ApplicationActions.createApplication(defaultTenantDTO);
        firstRelease = ReleaseDTO.createDefaultInstance();
        secondRelease = ReleaseDTO.createDefaultInstance();
        thirdRelease = ReleaseDTO.createDefaultInstance();
        ReleaseActions.createReleases(applicationDTO, firstRelease, secondRelease, thirdRelease);
        staticScanDTO = StaticScanDTO.createDefaultInstance();
        staticScanDTO.setAuditPreference(FodCustomTypes.AuditPreference.Manual);

        StaticScanActions.createStaticScan(staticScanDTO, applicationDTO,
                firstRelease, FodCustomTypes.SetupScanPageStatus.InProgress);
        StaticScanActions.createStaticScan(staticScanDTO, applicationDTO,
                secondRelease, FodCustomTypes.SetupScanPageStatus.Queued);
        StaticScanActions.createStaticScan(staticScanDTO, applicationDTO,
                thirdRelease, FodCustomTypes.SetupScanPageStatus.Queued);

        BrowserUtil.clearCookiesLogOff();

        AdminLoginPage.navigate().login(FodConfig.ADMIN_USER_NAME, FodConfig.ADMIN_PASSWORD)
                .adminTopNavbar.openStatic();

        var staticScanJobsPage = page(ScanJobsPage.class);
        staticScanJobsPage.appliedFilters.clearAll();
        staticScanJobsPage.findWithSearchBox(applicationDTO.getApplicationName());
        var scans = staticScanJobsPage.getAllScans();
        scans.sort(Comparator.comparing(StaticScanCell::getId));

        assertThat(scans).as("There should be 3 scans found").hasSize(3);
        var firstJobStatus = scans.get(0).getLastJob().getStatus();

        if (firstJobStatus.equals(FodCustomTypes.JobStatus.Pending.getTypeValue())) {
            scans.get(0).getLastJob().waitStatusChanged(FodCustomTypes.JobStatus.Pending, 30);
            staticScanJobsPage.findWithSearchBox(applicationDTO.getApplicationName());
            scans = staticScanJobsPage.getAllScans();
            scans.sort(Comparator.comparing(StaticScanCell::getId));
            firstJobStatus = scans.get(0).getLastJob().getStatus();
        }

        boolean statusValid = firstJobStatus.equals(FodCustomTypes.JobStatus.Success.getTypeValue())
                || firstJobStatus.equals(FodCustomTypes.JobStatus.Scanning.getTypeValue())
                || firstJobStatus.equals(FodCustomTypes.JobStatus.Building.getTypeValue());

        assertThat(statusValid).as("Status should be: Scanning || Building || Success. AR: "
                + firstJobStatus).isTrue();

        var secondJobStatus = scans.get(1).getLastJob().getStatus();
        assertThat(secondJobStatus)
                .as("Status of second job should be pending")
                .isEqualTo(FodCustomTypes.JobStatus.Pending.getTypeValue());

        var thirdJobStatus = scans.get(2).getLastJob().getStatus();
        assertThat(thirdJobStatus)
                .as("Status of third job should be pending")
                .isEqualTo(FodCustomTypes.JobStatus.Pending.getTypeValue());
    }

    @FodBacklogItem("1731001")
    @Owner("svpillai@opentext.com")
    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify more than one scan should not be in progress for same release")
    @Test(groups = {"regression","hf"})
    public void verifyScanStatusOfMultipleScansOnSameRelease() {
        app2 = ApplicationActions.createApplication(defaultTenantDTO);
        staticScanDTO = StaticScanDTO.createDefaultInstance();
        staticScanDTO.setAuditPreference(FodCustomTypes.AuditPreference.Manual);
        assertThat(StaticScanActions.createStaticScan(staticScanDTO, app2, FodCustomTypes.SetupScanPageStatus.InProgress)
                .openScans().getLastScanStatus())
                .as("First scan should be In Progress")
                .isEqualTo("In Progress");

        var secondScanOnSameRelease = StaticScanActions.createStaticScan(staticScanDTO, app2);
        WaitUtil.randomSleep(60, 120, true);
        assertThat(secondScanOnSameRelease.openScans().getLastScanStatus())
                        .as("Second scan should not change to In-Progress, it should remain in Queued status")
                        .isEqualTo("Queued");
    }
}
