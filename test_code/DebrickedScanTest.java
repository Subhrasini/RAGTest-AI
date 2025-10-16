package com.fortify.fod.ui.test.regression;

import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.elements.ModalDialog;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.EntitlementDTO;
import com.fortify.fod.common.entities.StaticScanDTO;
import com.fortify.fod.common.entities.TenantDTO;
import com.fortify.fod.ui.pages.common.common.cells.ReleaseScanCell;
import com.fortify.fod.ui.pages.tenant.applications.YourScansPage;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;

import java.io.File;

import static com.codeborne.selenide.Selenide.page;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("svpillai@opentext.com")
@FodBacklogItem("602036")
@Slf4j
public class DebrickedScanTest extends FodBaseTest {
    TenantDTO tenantDTO;
    ApplicationDTO sonatypeApp1, sonatypeApp2, debrickedApp1, debrickedApp2, webApp1, webApp2;
    StaticScanDTO sonatypeStaticScanDto1, sonatypeStaticScanDto2, debrickedStaticScanDto1, debrickedStaticScanDto2;
    EntitlementDTO sonatypeEntitlement, debrickedEntitlement;
    String sonatypeFilePath = "payloads/fod/10JavaDefects_Small(OS).zip";
    String debrickedFilePath = "payloads/fod/vuln_python_with_pipfilelock.zip";
    File expectedFile;

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Create tenant , applications and entitlements for the test")
    @Test(groups = {"regression"})
    public void prepareTestData() {
        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setEntitlementDTO(EntitlementDTO.createDefaultInstance());
        TenantActions.createTenant(tenantDTO, true);
        BrowserUtil.clearCookiesLogOff();

        sonatypeStaticScanDto1 = StaticScanDTO.createDefaultInstance();
        sonatypeStaticScanDto1.setLanguageLevel("11");
        sonatypeStaticScanDto1.setFileToUpload(sonatypeFilePath);
        sonatypeStaticScanDto1.setOpenSourceComponent(true);
        sonatypeStaticScanDto2 = StaticScanDTO.createDefaultInstance();
        sonatypeStaticScanDto2.setLanguageLevel("11");
        sonatypeStaticScanDto2.setFileToUpload(sonatypeFilePath);
        sonatypeStaticScanDto2.setOpenSourceComponent(false);

        debrickedStaticScanDto1 = StaticScanDTO.createDefaultInstance();
        debrickedStaticScanDto1.setFileToUpload(debrickedFilePath);
        debrickedStaticScanDto1.setTechnologyStack(FodCustomTypes.TechnologyStack.PYTHON);
        debrickedStaticScanDto1.setLanguageLevel("3");
        debrickedStaticScanDto1.setOpenSourceComponent(true);
        debrickedStaticScanDto2 = StaticScanDTO.createDefaultInstance();
        debrickedStaticScanDto2.setFileToUpload(debrickedFilePath);
        debrickedStaticScanDto2.setTechnologyStack(FodCustomTypes.TechnologyStack.PYTHON);
        debrickedStaticScanDto2.setLanguageLevel("3");
        debrickedStaticScanDto2.setOpenSourceComponent(false);

        sonatypeApp1 = ApplicationDTO.createDefaultInstance();
        sonatypeApp2 = ApplicationDTO.createDefaultInstance();
        debrickedApp1 = ApplicationDTO.createDefaultInstance();
        debrickedApp2 = ApplicationDTO.createDefaultInstance();
        webApp2 = ApplicationDTO.createDefaultInstance();

        ApplicationActions.createApplication(sonatypeApp1, tenantDTO, true);
        ApplicationActions.createApplication(sonatypeApp2, tenantDTO, false);
        ApplicationActions.createApplication(debrickedApp1, tenantDTO, false);
        ApplicationActions.createApplication(debrickedApp2, tenantDTO, false);
        ApplicationActions.createApplication(webApp2, tenantDTO, false);
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify sonatype scan with open source checkbox enabled")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void sonatypeScanWithOpenSourceCheckboxEnabled() {
        sonatypeEntitlement = EntitlementDTO.createDefaultInstance();
        sonatypeEntitlement.setEntitlementType(FodCustomTypes.EntitlementType.SonatypeEntitlement);
        var entitlementPage = EntitlementsActions.createEntitlements(tenantDTO, true, sonatypeEntitlement);

        AllureReportUtil.info("Entitlements page should contain 3 types of entitlements");
        assertThat(entitlementPage.tabs.tabExists("Fortify Entitlements"))
                .as("Entitlements Page should contain Fortify Entitlements")
                .isTrue();
        assertThat(entitlementPage.tabs.tabExists("Sonatype Entitlements"))
                .as("Entitlements Page should contain Sonatype Entitlements")
                .isTrue();
        assertThat(entitlementPage.tabs.tabExists("Debricked Entitlements"))
                .as("Entitlements Page should contain Debricked Entitlements")
                .isTrue();
        BrowserUtil.clearCookiesLogOff();

        LogInActions.tamUserLogin(tenantDTO);
        StaticScanActions
                .createStaticScan(sonatypeStaticScanDto1, sonatypeApp1, FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();
        StaticScanActions.completeStaticScan(sonatypeApp1, true, true);
        BrowserUtil.clearCookiesLogOff();

        var scansPage = LogInActions.tamUserLogin(tenantDTO).openYourReleases()
                .openDetailsForRelease(sonatypeApp1).openScans();
        scansPage.getScanByType(FodCustomTypes.ScanType.OpenSource)
                .waitStatus(FodCustomTypes.ScansPageStatus.Completed);
        var staticScanSetupPage = scansPage.openStaticScanSetup();
        assertThat(
                staticScanSetupPage.getOpenSourceCheckboxName())
                .as("The 'Open Source Component Analysis' checkbox renamed to" +
                        " 'Software Composition Analysis'")
                .isEqualTo("Software Composition Analysis");
        verifyApplicationAndReleaseScansPage(sonatypeApp1, "Sonatype");
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify sonatype scan with open source checkbox disabled")
    @Test(groups = {"regression"},
            dependsOnMethods = {"sonatypeScanWithOpenSourceCheckboxEnabled"})
    public void sonatypeScanWithOpenSourceCheckboxDisabled() {
        LogInActions.tamUserLogin(tenantDTO);
        StaticScanActions
                .createStaticScan(sonatypeStaticScanDto2, sonatypeApp2, FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();
        StaticScanActions.completeStaticScan(sonatypeApp2, true);
        BrowserUtil.clearCookiesLogOff();

        assertThat(LogInActions.tamUserLogin(tenantDTO).openYourReleases().openDetailsForRelease(sonatypeApp2)
                .openScans().getScanTypes())
                .as("Open source scan should not present")
                .containsOnly("Static");
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify debricked scan with open source checkbox enabled")
    @Test(groups = {"regression"},
            dependsOnMethods = {"prepareTestData", "sonatypeScanWithOpenSourceCheckboxDisabled"})
    public void debrickedScanWithOpenSourceCheckboxEnabled() {
        EntitlementsActions.disableEntitlementsByEntitlementType(tenantDTO, FodCustomTypes.EntitlementType.SonatypeEntitlement, true);
        debrickedEntitlement = EntitlementDTO.createDefaultInstance();
        debrickedEntitlement.setEntitlementType(FodCustomTypes.EntitlementType.DebrickedEntitlement);
        EntitlementsActions.createEntitlements(tenantDTO, false, debrickedEntitlement);
        BrowserUtil.clearCookiesLogOff();

        LogInActions.tamUserLogin(tenantDTO);
        StaticScanActions.createStaticScan(debrickedStaticScanDto1, debrickedApp1, FodCustomTypes.SetupScanPageStatus.Completed);
        verifyApplicationAndReleaseScansPage(debrickedApp1, "Debricked");
    }

    @MaxRetryCount(0)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify debricked scan with open source checkbox disabled")
    @Test(groups = {"regression"},
            dependsOnMethods = {"debrickedScanWithOpenSourceCheckboxEnabled"})
    public void debrickedScanWithOpenSourceCheckboxDisabled() {
        LogInActions.tamUserLogin(tenantDTO);
        assertThat(StaticScanActions.createStaticScan(debrickedStaticScanDto2, debrickedApp2, FodCustomTypes.SetupScanPageStatus.Completed)
                .openScans().getScanTypes())
                .as("Open source scan should not present")
                .containsOnly("Static");
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify scan with open source entitlements disabled and open source checkbcx enabled")
    @Test(groups = {"regression"},
            dependsOnMethods = {"debrickedScanWithOpenSourceCheckboxDisabled"})
    public void scanWithOssEntitlementsDisabledAndOssCheckboxEnabled() {
        EntitlementsActions.disableEntitlementsByEntitlementType(tenantDTO, FodCustomTypes.EntitlementType.DebrickedEntitlement, true);
        BrowserUtil.clearCookiesLogOff();
        webApp1 = ApplicationDTO.createDefaultInstance();
        var staticScanSetupPage = ApplicationActions.createApplication(webApp1, tenantDTO, true)
                .openStaticScanSetup();
        staticScanSetupPage.chooseAssessmentType("AUTO-STATIC");
        staticScanSetupPage.chooseEntitlement("Subscription");
        staticScanSetupPage.getAdvancedSettingsPanel().expand()
                .chooseTechnologyStack(FodCustomTypes.TechnologyStack.JAVA)
                .chooseLanguageLevel("11");
        staticScanSetupPage.chooseAuditPreference(FodCustomTypes.AuditPreference.Automated);
        staticScanSetupPage.enableOpenSourceScans(true);
        var staticScanPopup = staticScanSetupPage.pressStartScanBtn();
        var scanModal = new ModalDialog();
        assertThat(scanModal.getMessage())
                .as("Warning that opensource entitlements are absent should be appeared")
                .contains("No OpenSource Entitlements are available.");
        scanModal.pressYes();
        staticScanPopup.uploadFile(debrickedFilePath).pressNextBtn().pressStartScanBtn();
        var scansPage = staticScanSetupPage.openScans();
        scansPage.waitForScan();
        scansPage
                .getScanByType(FodCustomTypes.ScanType.Static)
                .waitStatus(FodCustomTypes.ScansPageStatus.Completed);
        var scans = scansPage.getAllScans(false).stream()
                .map(ReleaseScanCell::getScanType).toList();
        assertThat(scans)
                .as("Open source scan should not present")
                .containsOnly("Static");
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify scan with open source entitlements disabled and open source checkbcx disabled")
    @Test(groups = {"regression"},
            dependsOnMethods = {"scanWithOssEntitlementsDisabledAndOssCheckboxEnabled"})
    public void scanWithOssEntitlementsDisabledAndOssCheckboxDisabled() {
        LogInActions.tamUserLogin(tenantDTO);
        assertThat(StaticScanActions.createStaticScan(debrickedStaticScanDto2, webApp2, FodCustomTypes.SetupScanPageStatus.Completed)
                .openScans().getScanTypes())
                .as("Open source scan should not present")
                .containsOnly("Static");
    }

    @SneakyThrows
    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify debricked scan result can be downloaded")
    @Test(groups = {"regression"},
            dependsOnMethods = {"debrickedScanWithOpenSourceCheckboxEnabled"})
    public void downloadSbomFileTest() {
        LogInActions.tamUserLogin(tenantDTO).openYourScans();
        expectedFile = page(YourScansPage.class)
                .getScanByType(debrickedApp1, FodCustomTypes.ScanType.OpenSource)
                .downloadSBOM();
        assertThat(expectedFile.getName())
                .as("Verify file is downloaded")
                .contains("_cyclonedx.json");
        assertThat(expectedFile.length())
                .as("File size should be greater than 0")
                .isPositive();
    }

    public void verifyApplicationAndReleaseScansPage(ApplicationDTO applicationDto, String openSourceType) {
        var topNavBar = new TenantTopNavbar();
        var releaseScansPage = topNavBar.openApplications().openYourReleases()
                .openDetailsForRelease(applicationDto)
                .openScans();
        assertThat(releaseScansPage.getScanByType(FodCustomTypes.ScanType.Static)
                .waitStatus(FodCustomTypes.ScansPageStatus.Completed).getStatus())
                .as("Static scans should be  completed successfully")
                .isEqualTo("Completed");
        assertThat(releaseScansPage.getScanByType(FodCustomTypes.ScanType.OpenSource)
                .waitStatus(FodCustomTypes.ScansPageStatus.Completed).getStatus())
                .as("Open source scans should be  completed successfully")
                .isEqualTo("Completed");

        AllureReportUtil.info("Verify issues pages ,open source issues can be found under open source scan tool name");
        var releaseIssuesPage = releaseScansPage.openIssues();
        assertThat(releaseIssuesPage.getAllCount())
                .as("Scans should contain issues")
                .isPositive();
        releaseIssuesPage.filters.expandAllFilters();
        releaseIssuesPage.filters.setFilterByName("Category").clickFilterOptionByName("Open Source");
        releaseIssuesPage.groupBy("Scan Tool");
        assertThat(releaseIssuesPage.getGroupHeaders())
                .as(String.format("Open source issues should be  = %s", openSourceType))
                .contains(openSourceType);
        assertThat(releaseIssuesPage.issues.size())
                .as("Open source issues should be present")
                .isPositive();

        var applicationScansPage = topNavBar.openApplications()
                .openDetailsFor(applicationDto.getApplicationName())
                .openScans();
        assertThat(applicationScansPage.getScanTypes())
                .as("Application scans page should contain static and opensource scans")
                .containsExactlyInAnyOrder("Open Source", "Static");
        assertThat(applicationScansPage.getAssessmentTypes())
                .as(String.format("Application scans page should contain open source type  = %s", openSourceType))
                .contains(openSourceType);
    }
}