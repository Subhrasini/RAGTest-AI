package com.fortify.fod.ui.test.regression;

import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.*;
import com.fortify.fod.common.utils.CSVHelper;
import com.fortify.fod.ui.pages.tenant.applications.OpenSourceComponentCell;
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
import utils.FodBacklogItems;
import utils.MaxRetryCount;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("kbadia@opentext.com")
@Slf4j
public class AddPackageURLToOSSIssueExportTest extends FodBaseTest {
    ApplicationDTO application;
    StaticScanDTO staticScanDTO;
    TenantDTO tenantDTO;
    String staticPayload = "payloads/fod/10JavaDefects_Small(OS).zip";
    String staticFpr = "payloads/fod/10JavaDefects_ORIGINAL.fpr";
    DataExportDTO issuesDto;

    @MaxRetryCount(3)
    @Description("Admin user should be able to create tenant on " +
            "admin site and AUTO-TAM should create and complete static scan")
    @Test(groups = {"regression"})
    public void prepareTestData() {
        AllureReportUtil.info("Creating tenant with sonatype entitlement");
        var sonatypeEntitlement = EntitlementDTO.createDefaultInstance();
        sonatypeEntitlement.setEntitlementType(FodCustomTypes.EntitlementType.SonatypeEntitlement);
        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setEntitlementDTO(EntitlementDTO.createDefaultInstance());
        TenantActions.createTenant(tenantDTO, true);
        EntitlementsActions.createEntitlements(sonatypeEntitlement);
        BrowserUtil.clearCookiesLogOff();
        AllureReportUtil.info("Starting static scan with selecting the " +
                "check box of Open Source Component");
        application = ApplicationDTO.createDefaultInstance();
        staticScanDTO = StaticScanDTO.createDefaultInstance();
        staticScanDTO.setAssessmentType("Static Standard");
        staticScanDTO.setLanguageLevel("1.8");
        staticScanDTO.setFileToUpload(staticPayload);
        staticScanDTO.setOpenSourceComponent(true);
        ApplicationActions.createApplication(application, tenantDTO, true);
        StaticScanActions.createStaticScan(staticScanDTO, application);
        BrowserUtil.clearCookiesLogOff();
        StaticScanActions.importScanAdmin(application, staticFpr, true);
        BrowserUtil.clearCookiesLogOff();
        AllureReportUtil.info("Completing static scan with open source scan");
        LogInActions.tamUserLogin(tenantDTO).openYourReleases()
                .openDetailsForRelease(application)
                .openScans().getScanByType(FodCustomTypes.ScanType.OpenSource)
                .waitStatus(FodCustomTypes.ScansPageStatus.Completed);
    }

    @SneakyThrows
    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItems({@FodBacklogItem("443026"), @FodBacklogItem("1131001")})
    @Description("The URL on the Data Export would match the " +
            "'Package URL' on the Open Source Components page")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void validateOSSPackageURLTest() {
        AllureReportUtil.info("Creating Data Export with " +
                "Issues Data Export Template");
        issuesDto = DataExportDTO
                .createDTOWithTemplate(FodCustomTypes
                        .DataExportTemplate.Issues);
        List<String> csvColumnsToValidate = new ArrayList<>() {{
            add("URL");
        }};
        var yourReportsPage = LogInActions
                .tamUserLogin(tenantDTO).tenantTopNavbar.openReports();
        var generatedFromReportsPage = DataExportActions
                .createDataExportAndDownload(issuesDto);
        var dataFromCsv = new CSVHelper(generatedFromReportsPage)
                .getAllRows(csvColumnsToValidate);
        var checkOpenSourcePage = yourReportsPage.tenantTopNavbar
                .openApplications()
                .openYourApplications()
                .openOpenSourceComponents();
        var staticIssuesRow = checkOpenSourcePage.getTable()
                .getRowByColumnText("source.zip", 0)
                .getText();
        assertThat(staticIssuesRow)
                .as("Validate Package URL column cell would be blank " +
                        "for static issues row")
                .contains("");
        AllureReportUtil.info("Extracting all URLs available under Open Source " +
                "Components page in Component Details section");
        var openSourceComponentCells = checkOpenSourcePage
                .openOpenSourceComponents()
                .getAllComponentsOnPage();
        List<String> packageUrl = new ArrayList<>();
        for (var component : openSourceComponentCells) {
            var componentDetailsPopup = component
                    .pressDetailsButtonByComponent(component.getCellText(OpenSourceComponentCell.Column.COMPONENT));
            packageUrl.add(componentDetailsPopup.packageUrl.getOwnText());
            componentDetailsPopup.closeBtn.click();
        }
        for (var element : dataFromCsv) {
            String expectedURL = Arrays.asList(element).toString()
                    .replaceAll("[\\[\\]]", "");
            assertThat(packageUrl)
                    .as("Validate URL on the Data Export would match the 'Package URL' " +
                            "on the Open Source Components page in Component Details section")
                    .contains(expectedURL);
        }
    }

    @SneakyThrows
    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("740006")
    @Description("Verify that Never audited issues doesn't " +
            "have the time of creation as 'Audited timestamp'")
    @Test(groups = {"regression"},
            dependsOnMethods = {"prepareTestData"})
    public void validateNeverAuditedIssueTest() {
        AllureReportUtil.info("Create the new Issue Data Export. " +
                "Set 'Schedule' to 'Queue now'");
        var applicationDTO = ApplicationDTO
                .createDefaultInstance();
        applicationDTO = ApplicationActions
                .createApplication(tenantDTO, true);
        var scanDTO = StaticScanDTO
                .createDefaultInstance();
        StaticScanActions.createStaticScan(scanDTO, applicationDTO);
        BrowserUtil.clearCookiesLogOff();
        StaticScanActions.importScanAdmin(applicationDTO, staticFpr, true);
        StaticScanActions.completeStaticScan(applicationDTO, false);
        var dataExportDTO = DataExportDTO
                .createDTOWithTemplate(FodCustomTypes.DataExportTemplate.Issues);
        LogInActions.tamUserLogin(tenantDTO);
        var generatedFromReportsPage = DataExportActions
                .createDataExportAndDownload(dataExportDTO);
        var dataFromCsv = new CSVHelper(generatedFromReportsPage)
                .getAllRows(Collections.singletonList("Audited Timestamp"));
        for (var data : dataFromCsv) {
            assertThat(data)
                    .as("Verify blank field/empty string for " +
                            "'Audited timestamp' of all issues")
                    .contains("");
        }
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("725031")
    @Description("Separate source tools on Open Source Components page")
    @Test(groups = {"regression"}, dependsOnMethods = {"validateNeverAuditedIssueTest", "validateOSSPackageURLTest"})
    public void openSourceToolsForDebrickedScanTest() {
        LogInActions.adminLogIn().adminTopNavbar.openTenants().findTenant(tenantDTO.getTenantName())
                .openTenantByName(tenantDTO.getTenantName())
                .openEntitlements().switchEntitlementsTab(FodCustomTypes.EntitlementType.SonatypeEntitlement)
                .getAll().get(0)
                .pressEdit()
                .disableEntitlement();
        BrowserUtil.clearCookiesLogOff();
        var debrickedEntitlement = EntitlementDTO.createDefaultInstance();
        LogInActions.adminLogIn().adminTopNavbar.openTenants().openTenantByName(tenantDTO.getTenantName())
                .openEntitlements();
        debrickedEntitlement.setEntitlementType(FodCustomTypes.EntitlementType.DebrickedEntitlement);
        EntitlementsActions.createEntitlements(debrickedEntitlement);
        BrowserUtil.clearCookiesLogOff();

        var applicationDTO = ApplicationDTO.createDefaultInstance();
        var staticScanOpenSourceDTO = StaticScanDTO.createDefaultInstance();
        staticScanOpenSourceDTO.setTechnologyStack(FodCustomTypes.TechnologyStack.JS);
        staticScanOpenSourceDTO.setOpenSourceComponent(true);
        staticScanOpenSourceDTO.setFileToUpload("payloads/fod/JS_Payload.zip");
        ApplicationActions.createApplication(applicationDTO, tenantDTO, true);
        StaticScanActions.createStaticScan(staticScanOpenSourceDTO, applicationDTO);
        BrowserUtil.clearCookiesLogOff();
        StaticScanActions.importScanAdmin(applicationDTO, staticFpr, true);
        BrowserUtil.clearCookiesLogOff();
        AllureReportUtil.info("Completing static scan with open source scan");
        LogInActions.tamUserLogin(tenantDTO).openYourReleases()
                .openDetailsForRelease(applicationDTO)
                .openScans().getScanByType(FodCustomTypes.ScanType.OpenSource)
                .waitStatus(FodCustomTypes.ScansPageStatus.Completed);
        BrowserUtil.clearCookiesLogOff();
        var openSourceComponentsPage = LogInActions
                .tamUserLogin(tenantDTO).openYourReleases()
                .openDetailsForRelease(applicationDTO)
                .openScans()
                .pressImportOpenSourceScan()
                .uploadFile("payloads/fod/21210_51134_cyclonedx.json")
                .pressImportButton()
                .openOpenSourceComponents();
        var table = openSourceComponentsPage.getTable();
        assertThat(table.getAllColumnValues(table.getColumnIndex("Scan Tool")))
                .as("Scan Tool Column should not be " +
                        "empty and should contain scan types Debricked")
                .contains("Debricked");
        assertThat(table
                .getAllColumnValues(table
                        .getColumnIndex("Known Public Vulnerabilities")))
                .as("Vulnerabilities column should not be empty")
                .isNotEmpty();
        assertThat(openSourceComponentsPage.getNumberComponentsIdentifiedCount())
                .as("Components Identified section " +
                        "should show the components count")
                .isEqualTo(21);
        assertThat(openSourceComponentsPage.getNumberSecurityIssuesCount())
                .as("Security Issues section " +
                        "should show the Security issues count")
                .isEqualTo(26);
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @SneakyThrows
    @FodBacklogItem("762026")
    @Description("OSS Components page : Components page is showing " +
            "incorrect tool name for copy state release-Debrick scan")
    @Test(groups = {"regression"},
            dependsOnMethods = {"openSourceToolsForDebrickedScanTest"})
    public void copyStateReleaseDebrickedScanTest() {
        var applicationDTO = ApplicationDTO.createDefaultInstance();
        var staticScanOpenSourceDTO = StaticScanDTO.createDefaultInstance();
        staticScanOpenSourceDTO.setTechnologyStack(FodCustomTypes.TechnologyStack.JS);
        staticScanOpenSourceDTO.setOpenSourceComponent(true);
        staticScanOpenSourceDTO.setFileToUpload("payloads/fod/JS_Payload.zip");
        ApplicationActions.createApplication(applicationDTO, tenantDTO, true);
        StaticScanActions.createStaticScan(staticScanOpenSourceDTO, applicationDTO);
        BrowserUtil.clearCookiesLogOff();
        StaticScanActions.importScanAdmin(applicationDTO, staticFpr, true);
        BrowserUtil.clearCookiesLogOff();
        AllureReportUtil.info("Completing static scan with Debricked open source scan");
        LogInActions.tamUserLogin(tenantDTO).openYourReleases()
                .openDetailsForRelease(applicationDTO)
                .openScans().getScanByType(FodCustomTypes.ScanType.OpenSource)
                .waitStatus(FodCustomTypes.ScansPageStatus.Completed);
        BrowserUtil.clearCookiesLogOff();
        var releaseDetailsPage = LogInActions
                .tamUserLogin(tenantDTO).openYourReleases()
                .openDetailsForRelease(applicationDTO);
        AllureReportUtil.info("Download SBOM file from completed scan");
        File file = releaseDetailsPage.openScans().getAllScans(true).get(0).downloadSBOM();
        AllureReportUtil.info("Import SBOM file to the same release");
        releaseDetailsPage.openScans().pressImportOpenSourceScan()
                .uploadFile(file.getAbsolutePath())
                .pressImportButton().waitLastScanComplete()
                .openOpenSourceComponents();
        AllureReportUtil.info("Create a copy state release from release 1");
        var copyReleaseDTO = ReleaseDTO.createDefaultInstance();
        copyReleaseDTO.setCopyState(true);
        copyReleaseDTO.setSdlcStatus(FodCustomTypes.Sdlc.Development);
        copyReleaseDTO.setCopyFromReleaseName(applicationDTO.getReleaseName());
        new TenantTopNavbar().openApplications();
        ReleaseActions.createRelease(applicationDTO, copyReleaseDTO);
        BrowserUtil.clearCookiesLogOff();

    }
}
