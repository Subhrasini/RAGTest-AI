package com.fortify.fod.ui.test.regression;

import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.DynamicScanActions;
import com.fortify.fod.ui.test.actions.MobileScanActions;
import com.fortify.fod.ui.test.actions.StaticScanActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.MaxRetryCount;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("oradchenko@opentext.com")
@Slf4j
public class CancelRecentlyImportedFPRScanTest extends FodBaseTest {

    ApplicationDTO applicationForStaticScan;
    ApplicationDTO applicationForDynamicScan;
    ApplicationDTO applicationForMobileScan;

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Import static scan, verify status and issues count, cancel it, verify status and issues count again")
    @Test(groups = {"regression"})
    public void cancelRecentlyImportedStaticScan() {
        applicationForStaticScan = ApplicationDTO.createDefaultInstance();
        var releaseScansPage = ApplicationActions
                .createApplication(applicationForStaticScan, defaultTenantDTO, true)
                .tenantTopNavbar
                .openApplications().openYourReleases().openDetailsForRelease(applicationForStaticScan).openScans();

        var scansCount = releaseScansPage.getAllScans(true).size();
        var options = releaseScansPage.openImportDropdown().getImportOptions();
        assertThat(options).as("There should be 3 options")
                .hasSize(3)
                .as("Options should contains static and dynamic scans")
                .contains("static", "dynamic", "opensource");

        releaseScansPage.closeImportDropdown();

        StaticScanActions.importScanTenant(applicationForStaticScan, "payloads/fod/static.java.fpr");
        var scans = releaseScansPage.getAllScans(false);
        assertThat(scans).as("New scans count should be greater than previous").hasSizeGreaterThan(scansCount);

        var status = scans.get(0).getStatus();
        assertThat(status).as("Status should be completed").isEqualTo("Completed");

        var issuesPage = releaseScansPage.openIssues();
        assertThat(issuesPage.getAllCount()).as("There should be 4 issues in total").isEqualTo(4);

        releaseScansPage = issuesPage.openScans();
        releaseScansPage.getScanByType(FodCustomTypes.ScanType.Static).cancelScan();
        status = releaseScansPage.getAllScans(false).get(0).getStatus();
        assertThat(status).as("Status should be Cancelled by customer")
                .isIn("Cancelled by customer", "Canceled");
        issuesPage = releaseScansPage.openIssues();
        assertThat(issuesPage.getAllCount()).as("There should be 0 issues in total").isZero();
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Import dynamic scan, verify status and issues count, cancel it, verify status and issues count again")
    @Test(groups = {"regression"})
    public void cancelRecentlyImportedDynamicScan() {
        applicationForDynamicScan = ApplicationDTO.createDefaultInstance();
        var releaseScansPage = ApplicationActions
                .createApplication(applicationForDynamicScan, defaultTenantDTO, true)
                .tenantTopNavbar
                .openApplications().openYourReleases().openDetailsForRelease(applicationForDynamicScan).openScans();

        var scansCount = releaseScansPage.getAllScans(true).size();
        var options = releaseScansPage.openImportDropdown().getImportOptions();
        assertThat(options).as("There should be 3 options")
                .hasSize(3)
                .as("Options should contain static and dynamic scans")
                .contains("static", "dynamic", "opensource");

        releaseScansPage.closeImportDropdown();

        DynamicScanActions
                .importDynamicScanTenant(applicationForDynamicScan, "payloads/fod/dynamic.zero.fpr");
        var scans = releaseScansPage.getAllScans(false);
        assertThat(scans).as("New scans count should be greater than previous").hasSizeGreaterThan(scansCount);

        var status = scans.get(0).waitStatus(FodCustomTypes.ScansPageStatus.Completed).getStatus();
        assertThat(status).as("Status should be completed").isEqualTo("Completed");

        var issuesPage = releaseScansPage.openIssues();
        assertThat(issuesPage.getAllCount()).as("There should be 25 issues in total").isEqualTo(25);

        releaseScansPage = issuesPage.openScans();
        releaseScansPage.getScanByType(FodCustomTypes.ScanType.Dynamic).cancelScan();
        status = releaseScansPage.getAllScans(false).get(0).getStatus();
        assertThat(status).as("Status should be Cancelled by customer")
                .isIn("Cancelled by customer", "Canceled");
        issuesPage = releaseScansPage.openIssues();
        assertThat(issuesPage.getAllCount()).as("There should be 0 issues in total").isZero();
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Import mobile scan, verify status and issues count, cancel it, verify status and issues count again")
    @Test(groups = {"regression"})
    public void cancelRecentlyImportedMobileScan() {
        applicationForMobileScan = ApplicationDTO.createDefaultMobileInstance();
        var releaseScansPage = ApplicationActions
                .createApplication(applicationForMobileScan, defaultTenantDTO, true)
                .tenantTopNavbar
                .openApplications().openYourReleases().openDetailsForRelease(applicationForMobileScan).openScans();

        var scansCount = releaseScansPage.getAllScans(true).size();
        var options = releaseScansPage.openImportDropdown().getImportOptions();
        var disabled = releaseScansPage.isDynamicDisabled();
        assertThat(options).as("There should be 3 options")
                .hasSize(3)
                .as("Options should contains mobile and dynamic scans")
                .contains("static", "dynamic", "opensource");
        assertThat(disabled).as("Dynamic option should be disabled for Mobile scan").isTrue();
        releaseScansPage.closeImportDropdown();

        MobileScanActions.importScanTenant(applicationForMobileScan, "payloads/fod/static.java.fpr");
        var scans = releaseScansPage.getAllScans(false);
        assertThat(scans).as("New scans count should be greater than previous")
                .hasSizeGreaterThan(scansCount);

        var status = scans.get(0).getStatus();
        assertThat(status).as("Status should be completed").isEqualTo("Completed");

        var issuesPage = releaseScansPage.openIssues();
        assertThat(issuesPage.getAllCount()).as("There should be 4 issues in total").isEqualTo(4);

        releaseScansPage = issuesPage.openScans();
        releaseScansPage.getScanByType(FodCustomTypes.ScanType.Static).cancelScan();
        status = releaseScansPage.getAllScans(false).get(0).getStatus();
        assertThat(status).as("Status should be Cancelled by customer")
                .isIn("Cancelled by customer", "Canceled");
        issuesPage = releaseScansPage.openIssues();
        assertThat(issuesPage.getAllCount()).as("There should be 0 issues in total").isZero();
    }
}
