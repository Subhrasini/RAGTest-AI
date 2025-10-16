package com.fortify.fod.ui.test.regression;

import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.EntitlementDTO;
import com.fortify.fod.common.entities.TenantDTO;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.EntitlementsActions;
import com.fortify.fod.ui.test.actions.TenantActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("svpillai@opentext.com")
@FodBacklogItem("1343003")
@Slf4j
public class PartiallyCompletedDebrickedScanTest extends FodBaseTest {
    ApplicationDTO app;
    TenantDTO tenantDTO;
    String debrickedFilePath = "payloads/fod/NodeGoat-main-GOOD_AND_2_BAD-LOCKFILE.zip";
    String partialCompleteMessage = "Open Source Scan Partially Completed";
    String partialCompleteStatus = "Partial Complete. See Scan Summary for details.";
    String orangeIconColor = "rgba(255, 128, 0, 1)";

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify partially completed debricked scans from tenant site")
    @Test(groups = {"regression"})
    public void verifyPartiallyCompletedDebrickedScan() {
        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setEntitlementDTO(EntitlementDTO.createDefaultInstance());

        var debrickedEntitlement = EntitlementDTO.createDefaultInstance();
        debrickedEntitlement.setQuantityPurchased(100);
        debrickedEntitlement.setEntitlementType(FodCustomTypes.EntitlementType.DebrickedEntitlement);
        TenantActions.createTenant(tenantDTO, true);
        EntitlementsActions.createEntitlements(debrickedEntitlement);
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Verify release scans page,debricked completed scan status should display with Orange checkmark");
        app = ApplicationDTO.createDefaultInstance();
        var scanCell = ApplicationActions.createApplication(app, tenantDTO, true)
                .pressStartOpenSourceScan()
                .uploadFile(debrickedFilePath)
                .pressStartScan()
                .getScanByType(FodCustomTypes.ScanType.OpenSource)
                .waitStatus(FodCustomTypes.ScansPageStatus.PartialComplete);
        assertThat(scanCell.getStatus())
                .as("Debricked completed scan should show Partial complete on the hover text")
                .isEqualTo(partialCompleteStatus);
        assertThat(scanCell.getStatusIconColor())
                .as("Debricked completed scan should show orange checkmark")
                .isEqualTo(orangeIconColor);
        var scanSummaryPopup = scanCell.pressScanSummary();
        assertThat(scanSummaryPopup.getValueByName("Errors"))
                .as("Scan summary should have details for the errors")
                .contains("Failed to import 2 of 3 dependency files.");
        scanSummaryPopup.pressClose();

        AllureReportUtil.info("Verify release overview page , Scan status as orange to indicate partial completion");
        var releaseDetailsPage = new TenantTopNavbar().openApplications().openYourReleases()
                .openDetailsForRelease(app.getApplicationName(), app.getReleaseName());
        assertThat(releaseDetailsPage.getOpenSourceScanStatus())
                .as("Scan Status should be completed")
                .isEqualTo("Completed");
        assertThat(releaseDetailsPage.getOpenSourceScanStatusIconColor())
                .as("Icon color should be orange to indicate partial complete")
                .isEqualTo(orangeIconColor);
        assertThat(releaseDetailsPage.getOpenSourceScanStatusTitle())
                .as("Completed scan should show partial complete on the hover message")
                .contains(partialCompleteStatus);

        AllureReportUtil.info("Verify event log page,record should present for partially completed scans");
        assertThat(new TenantTopNavbar().openApplications().openDetailsFor(app.getApplicationName())
                .openEventLog().getAllLogs().stream()
                .anyMatch(x -> x.getType().equals(partialCompleteMessage)))
                .as("Event log should have record stating that debricked scan was partially completed")
                .isTrue();
    }
}
