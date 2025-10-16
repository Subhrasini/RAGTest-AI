package com.fortify.fod.ui.test.regression;

import com.fortify.common.custom_types.IssuesCounters;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.elements.Paging;
import com.fortify.fod.common.entities.*;
import com.fortify.fod.ui.pages.admin.administration.tenants.tenant.TenantDetailsPage;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.time.LocalTime;
import java.util.List;

import static com.codeborne.selenide.Selenide.*;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("oradchenko@opentext.com")
@Slf4j
public class ReleaseOperationsTest extends FodBaseTest {

    TenantDTO tenantDTO;
    ApplicationDTO webApp;
    DynamicScanDTO dynamicScanDTO = DynamicScanDTO.createDefaultInstance();
    ReleaseDTO releaseToBeRetired;
    ReleaseDTO noCopyRelease;

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Admin user should be able to create tenant, entitlement, application on admin site.")
    @Test(groups = {"regression"})
    public void prepareTestData() {
        tenantDTO = TenantDTO.createDefaultInstance();
        webApp = ApplicationDTO.createDefaultInstance();
        releaseToBeRetired = ReleaseDTO.createDefaultInstance();
        noCopyRelease = ReleaseDTO.createDefaultInstance();

        tenantDTO.setEntitlementDTO(EntitlementDTO.createDefaultInstance());
        TenantActions.createTenant(tenantDTO, true);
        var entitlement = EntitlementDTO.createDefaultInstance();
        var tenantPage = page(TenantDetailsPage.class);
        tenantPage.adminTopNavbar.openTenants();
        EntitlementsActions.createEntitlements(tenantDTO, false, entitlement);

        BrowserUtil.clearCookiesLogOff();
        ApplicationActions.createApplication(webApp, tenantDTO, true);
        DynamicScanActions.createDynamicScan(dynamicScanDTO, webApp);
        BrowserUtil.clearCookiesLogOff();


        IssuesActions.importScanAndValidateDynamicIssuesAdmin(
                webApp.getApplicationName(),
                "payloads/fod/dynamic.zero.fpr",
                false,
                true,
                false,
                new IssuesCounters(1, 3, 4, 5, 25),
                true);
        DynamicScanActions.completeDynamicScanAdmin(webApp, false);
        BrowserUtil.clearCookiesLogOff();
        DynamicScanActions.validateDynamicScanCompletedTenant(tenantDTO, webApp);

        releaseToBeRetired.setCopyState(true);
        releaseToBeRetired.setCopyFromReleaseName(webApp.getReleaseName());
        var oldName = releaseToBeRetired.getReleaseName();
        releaseToBeRetired.setReleaseName(String.format("%s-retired", oldName));
        ReleaseActions.createRelease(webApp, releaseToBeRetired);
        ReleaseActions.createRelease(webApp, noCopyRelease);

    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Admin user should be able to see error message when creating concurrent dynamic scan, retire a release, and complete dynamic scan for retired release")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void concurrentScanTest() {

        LogInActions.tamUserLogin(tenantDTO.getAssignedUser(),
                FodConfig.TAM_PASSWORD, tenantDTO.getTenantCode());
        //create a dynamic scan for a to-be-retired release
        DynamicScanActions.createDynamicScan(dynamicScanDTO, webApp, releaseToBeRetired.getReleaseName(),
                FodCustomTypes.SetupScanPageStatus.InProgress);

        try {
            DynamicScanActions.createDynamicScan(dynamicScanDTO, webApp, noCopyRelease.getReleaseName(),
                    FodCustomTypes.SetupScanPageStatus.InProgress);
        } catch (Exception e) {
            assertThat(e.getMessage()).as("Scan should be rejected")
                    .containsIgnoringCase("A dynamic scan is currently in progress for this application.");
        }
    }

    @Severity(SeverityLevel.NORMAL)
    @Description("Admin user should be able to retire release, view its status in different places and will not be able to start scans")
    @Test(groups = {"regression"}, dependsOnMethods = {"concurrentScanTest"})
    public void retireReleaseTest() {

        String oldBadgeValue = "";

        var page = LogInActions.tamUserLogin(tenantDTO.getAssignedUser(),
                FodConfig.TAM_PASSWORD, tenantDTO.getTenantCode()).openYourReleases();
        oldBadgeValue = page.tabs.getBadgeValue(releaseToBeRetired.getSdlcStatus().getTypeValue());
        var releaseDetailsPage =
                page.openDetailsForRelease(webApp.getApplicationName(), releaseToBeRetired.getReleaseName())
                        .openReleaseSettings()
                        .setSDLCStatus(FodCustomTypes.Sdlc.Retired)
                        .pressSave()
                        .openOverview();

        assertThat(releaseDetailsPage.canStartScans()).as("Start Scans should be disabled").isFalse();

        var yourReleasesPage = releaseDetailsPage.tenantTopNavbar
                .openApplications()
                .openYourReleases()
                .openSDLCTab(releaseToBeRetired.getSdlcStatus().getTypeValue());

        yourReleasesPage.waitUntilSDLCStatusChanged(releaseToBeRetired.getSdlcStatus(), oldBadgeValue);
        var endTime = LocalTime.now().plusMinutes(5);
        List<String> notRetiredReleases;
        do {
            yourReleasesPage
                    .openYourReleases()
                    .openSDLCTab(releaseToBeRetired.getSdlcStatus().getTypeValue());
            notRetiredReleases = yourReleasesPage
                    .getReleasesBySDLC(releaseToBeRetired.getSdlcStatus());
            if (notRetiredReleases.size() == 1) break;
            sleep(5000);
            refresh();
        } while (LocalTime.now().isBefore(endTime));

        assertThat(notRetiredReleases)
                .as("Retired release should not be in other categories")
                .doesNotContain(releaseToBeRetired.getReleaseName());

        var retiredReleases = yourReleasesPage
                .getReleasesBySDLC(FodCustomTypes.Sdlc.Retired);


        assertThat(retiredReleases)
                .as("Retired release should be in Retired table")
                .contains(releaseToBeRetired.getReleaseName());

        boolean isStartEnabledRetired = yourReleasesPage.isStartScanDisabledForRelease(releaseToBeRetired.getReleaseName());
        assertThat(isStartEnabledRetired).as("Start button should be disabled for retired release").isTrue();

        yourReleasesPage.openSDLCTab("All");
        new Paging().setRecordsPerPage_100();
        boolean isStartEnabledAll = yourReleasesPage.isStartScanDisabledForRelease(releaseToBeRetired.getReleaseName());
        assertThat(isStartEnabledAll).as("Start button should be disabled for retired release").isTrue();
    }

    @Severity(SeverityLevel.NORMAL)
    @Description("Admin user should be able complete dynamic scan for retired release")
    @Test(groups = {"regression"}, dependsOnMethods = {"retireReleaseTest"})
    public void completeDynamicScanForRetiredRelease() {
        IssuesActions.importScanAndValidateDynamicIssuesAdmin(
                webApp.getApplicationName(),
                "payloads/fod/dynamic.zero.fpr",
                false,
                true,
                false,
                new IssuesCounters(1, 3, 4, 5, 25),
                true);
        DynamicScanActions.completeDynamicScanAdmin(webApp, false);
        BrowserUtil.clearCookiesLogOff();
        DynamicScanActions.validateDynamicScanCompletedTenant(tenantDTO, webApp);
    }
}
