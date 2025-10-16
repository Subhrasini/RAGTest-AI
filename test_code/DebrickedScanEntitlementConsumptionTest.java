package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.EntitlementDTO;
import com.fortify.fod.common.entities.StaticScanDTO;
import com.fortify.fod.common.entities.TenantDTO;
import com.fortify.fod.ui.pages.tenant.applications.release.static_scan_setup.StaticScanSetupPage;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import org.testng.internal.collections.Pair;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import static com.codeborne.selenide.Selenide.page;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("tmagill@opentext.com")
@FodBacklogItem("950005")
@Slf4j
public class DebrickedScanEntitlementConsumptionTest extends FodBaseTest {

    TenantDTO tenantDTO;
    ApplicationDTO applicationDTO2;
    StaticScanDTO staticScanDTO2;

    @MaxRetryCount(1)
    @Description("Create Tenant, Debricked Entitlements, and Debricked Scan")
    @Test
    public void prepareTestData() {
        tenantDTO = TenantDTO.createDefaultInstance();
        var debrickedEntitlement = EntitlementDTO.createDefaultInstance();
        debrickedEntitlement.setQuantityPurchased(1);
        debrickedEntitlement.setEntitlementType(FodCustomTypes.EntitlementType.DebrickedEntitlement);
        tenantDTO.setEntitlementDTO(EntitlementDTO.createDefaultInstance());
        TenantActions.createTenant(tenantDTO, true);
        EntitlementsActions.createEntitlements(debrickedEntitlement);
        BrowserUtil.clearCookiesLogOff();

        addAppRunScan(0);
        var debrickedEntitlement2 = EntitlementDTO.createDefaultInstance();
        debrickedEntitlement2.setQuantityPurchased(1);
        debrickedEntitlement2.setEntitlementType(FodCustomTypes.EntitlementType.DebrickedEntitlement);
        tenantDTO.setEntitlementDTO(EntitlementDTO.createDefaultInstance());
        EntitlementsActions.createEntitlements(tenantDTO, true, debrickedEntitlement2);
        BrowserUtil.clearCookiesLogOff();
    }

    @Severity(SeverityLevel.CRITICAL)
    @MaxRetryCount(1)
    @Description("Scan should run with out interruption i.e. popup regarding Open Source entitlements")
    @Test(dependsOnMethods = {"prepareTestData"})
    public void checkStaticScanRerunTest() {
        Pair<ApplicationDTO, StaticScanDTO> pageObjects = addAppRunScan(1);
        applicationDTO2 = pageObjects.first();
        staticScanDTO2 = pageObjects.second();
        var applicationsPage = LogInActions.tamUserLogin(tenantDTO).tenantTopNavbar.openApplications();
        var secondScan = applicationsPage.openYourReleases()
                .openDetailsForRelease(applicationDTO2.getApplicationName(), applicationDTO2.getReleaseName())
                .pressStartStaticScan();
        secondScan.createStaticScan(staticScanDTO2).waitStatus(FodCustomTypes.SetupScanPageStatus.Completed);
        var staticScanSetupPage = page(StaticScanSetupPage.class);
        var getStatus = staticScanSetupPage
                .openScans()
                .getScanByType(FodCustomTypes.ScanType.OpenSource).waitStatus(FodCustomTypes.ScansPageStatus.Completed)
                .getStatus();
        assertThat(getStatus).isEqualTo("Completed");
    }

    @MaxRetryCount(1)
    @Owner("kbadia@opentext.com")
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("1403001")
    @Description("Verify that the full path displayed under dependency tab of release issues page")
    @Test(groups = {"regression"}, dependsOnMethods = {"checkStaticScanRerunTest"})
    public void verifyFullManifestFilePath() {
        var issuesPage = LogInActions
                .tamUserLogin(tenantDTO)
                .openYourReleases()
                .openDetailsForRelease(applicationDTO2.getApplicationName(), applicationDTO2.getReleaseName())
                .openIssues();
        issuesPage.clickAll();
        issuesPage.groupBy("Scan Type").expandAllGroupByElements();
        assertThat(issuesPage
                .getAllIssues()
                .get(0)
                .openDetails()
                .openDependencies()
                .dependencyPath
                .innerHtml())
                .as("Full path should be displayed under dependency tab of release issues page")
                .contains("NodeGoat-main\\package-lock.json");
    }

    public Pair<ApplicationDTO, StaticScanDTO> addAppRunScan(Integer index) {
        ApplicationDTO appDTO = ApplicationDTO.createDefaultInstance();
        ApplicationActions.createApplication(appDTO, tenantDTO, true);
        StaticScanDTO statScanDTO = StaticScanDTO.createDefaultInstance();
        statScanDTO.setLanguageLevel("10");
        statScanDTO.setOpenSourceComponent(true);
        statScanDTO.setFileToUpload("payloads/fod/NodeGoat-main.zip");
        StaticScanActions.createStaticScan(statScanDTO, appDTO, FodCustomTypes.SetupScanPageStatus.Completed);
        BrowserUtil.clearCookiesLogOff();

        var topTenantNavBar2 = LogInActions.tamUserLogin(tenantDTO).tenantTopNavbar;
        var openScansPage2 = topTenantNavBar2.openApplications()
                .openDetailsFor(appDTO.getApplicationName())
                .openScans();
        openScansPage2.getScanByType(FodCustomTypes.ScanType.OpenSource).waitStatus(FodCustomTypes.ScansPageStatus.Completed);
        var openSourceIssues2 = openScansPage2.getScanByType(FodCustomTypes.ScanType.OpenSource)
                .getTotalCount();
        assertThat(openSourceIssues2).isPositive();
        var staticScanIssue2 = openScansPage2.getScanByType(FodCustomTypes.ScanType.Static)
                .getTotalCount();
        assertThat(staticScanIssue2).isPositive();
        BrowserUtil.clearCookiesLogOff();
        checkingEntitlements(index);
        return Pair.of(appDTO, statScanDTO);
    }

    public void checkingEntitlements(Integer index) {
        var entitlementCell = LogInActions.adminLogIn().adminTopNavbar.openTenants()
                .openTenantByName(tenantDTO.getTenantName())
                .openEntitlements()
                .switchEntitlementsTab(FodCustomTypes.EntitlementType.DebrickedEntitlement)
                .getAll()
                .get(index);
        assertThat(entitlementCell.getQuantityPurchased()).isEqualTo(1);
        assertThat(entitlementCell.getQuantityConsumed()).isEqualTo(1);
        BrowserUtil.clearCookiesLogOff();
    }
}