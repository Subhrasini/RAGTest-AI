package com.fortify.fod.ui.test.regression;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.elements.ModalDialog;
import com.fortify.fod.common.elements.RadioButton;
import com.fortify.fod.common.entities.*;
import com.fortify.fod.ui.pages.admin.navigation.AdminTopNavbar;
import com.fortify.fod.ui.pages.common.common.pages.IssuesPage;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.IterableUtils;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;

import java.util.stream.Collectors;

import static com.codeborne.selenide.Selenide.refresh;
import static com.codeborne.selenide.WebDriverRunner.url;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class ReleaseIssuesPageManagedTest extends FodBaseTest {

    AdminUserDTO dynamicTester, mobileOperator;
    ApplicationDTO dynamicApp, staticApp, mobileApp, additionalStaticApp, additionalMobileApp;
    DynamicScanDTO dynamicScanDTO;
    MobileScanDTO mobileScanDTO;
    StaticScanDTO staticScanDTO;
    RadioButton suppressButton = new RadioButton("Suppress");
    RadioButton setSeverityButton = new RadioButton("Set Severity");

    public void init() {
        dynamicTester = AdminUserDTO.createInstanceWithUserRole(FodCustomTypes.AdminUserRole.DynamicTester);
        dynamicTester.setTenant(defaultTenantDTO.getTenantCode());
        mobileOperator = AdminUserDTO.createDefaultInstance();
        mobileOperator.setRole(FodCustomTypes.AdminUserRole.MobileTester);
        mobileOperator.setTenant(defaultTenantDTO.getTenantCode());

        dynamicApp = ApplicationDTO.createDefaultInstance();
        staticApp = ApplicationDTO.createDefaultInstance();
        additionalStaticApp = ApplicationDTO.createDefaultInstance();
        mobileApp = ApplicationDTO.createDefaultMobileInstance();
        additionalMobileApp = ApplicationDTO.createDefaultMobileInstance();

        dynamicScanDTO = DynamicScanDTO.createDefaultInstance();

        mobileScanDTO = MobileScanDTO.createDefaultScanInstance();
        mobileScanDTO.setAssessmentType("AUTO-MOBILE");

        staticScanDTO = StaticScanDTO.createDefaultInstance();
        staticScanDTO.setAuditPreference(FodCustomTypes.AuditPreference.Manual);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Prepare tenant/applications/completed scans for test execution")
    @Test(groups = {"regression"})
    public void testDataPreparation() {
        init();

        LogInActions.adminLogIn();
        AdminUserActions.createAdminUsers(dynamicTester, mobileOperator);
        BrowserUtil.clearCookiesLogOff();

        ApplicationActions.createApplication(dynamicApp, defaultTenantDTO, true);
        ApplicationActions.createApplication(mobileApp, defaultTenantDTO, false);
        ApplicationActions.createApplication(additionalMobileApp, defaultTenantDTO, false);
        ApplicationActions.createApplication(staticApp, defaultTenantDTO, false);
        ApplicationActions.createApplication(additionalStaticApp, defaultTenantDTO, false);

        DynamicScanActions.createDynamicScan(dynamicScanDTO, dynamicApp, FodCustomTypes.SetupScanPageStatus.Queued,
                FodCustomTypes.SetupScanPageStatus.InProgress, FodCustomTypes.SetupScanPageStatus.Scheduled);

        MobileScanActions.createMobileScan(mobileScanDTO, mobileApp, FodCustomTypes.SetupScanPageStatus.Queued,
                FodCustomTypes.SetupScanPageStatus.Scheduled, FodCustomTypes.SetupScanPageStatus.InProgress);

        MobileScanActions.createMobileScan(mobileScanDTO, additionalMobileApp, FodCustomTypes.SetupScanPageStatus.Queued,
                FodCustomTypes.SetupScanPageStatus.Scheduled, FodCustomTypes.SetupScanPageStatus.InProgress);

        staticScanDTO.setFileToUpload("payloads/fod/WebGoat5.0.zip");
        StaticScanActions.createStaticScan(staticScanDTO, staticApp, FodCustomTypes.SetupScanPageStatus.Queued,
                FodCustomTypes.SetupScanPageStatus.Scheduled, FodCustomTypes.SetupScanPageStatus.InProgress);

        StaticScanActions.createStaticScan(staticScanDTO, additionalStaticApp, FodCustomTypes.SetupScanPageStatus.Queued,
                FodCustomTypes.SetupScanPageStatus.Scheduled, FodCustomTypes.SetupScanPageStatus.InProgress);

        BrowserUtil.clearCookiesLogOff();

        DynamicScanActions.importDynamicScanAdmin(dynamicApp.getReleaseName(), "payloads/fod/dynamic.zero.fpr",
                false, false, false, true);
        DynamicScanActions.completeDynamicScanAdmin(dynamicApp, false);

        MobileScanActions.importMobileScanAdmin(mobileApp.getApplicationName(),
                FodCustomTypes.ImportFprScanType.Mobile, "payloads/fod/mobile_IOS.fpr",
                false, false, false);
        MobileScanActions.completeMobileScan(mobileApp, false);

        MobileScanActions.importMobileScanAdmin(additionalMobileApp.getApplicationName(),
                FodCustomTypes.ImportFprScanType.Mobile, "payloads/fod/dynamic.zero.fpr", false,
                false, false);
        MobileScanActions.completeMobileScan(additionalMobileApp, false);

        StaticScanActions.completeStaticScan(staticApp, false);

        StaticScanActions.importScanAdmin(additionalStaticApp, "payloads/fod/static.java.fpr", false);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate Dynamic Scan Audit Template")
    @Test(groups = {"regression"}, dependsOnMethods = {"testDataPreparation"})
    public void dynamicScanAuditTemplateTest() {
        var releaseIssuesPage = LogInActions.tamUserLogin(defaultTenantDTO)
                .openYourReleases().openDetailsForRelease(dynamicApp).openIssues().clickAll();
        releaseIssuesPage.findWithSearchBox(4722);
        var auditTemplatePopup = releaseIssuesPage.openIssueByIndex(0)
                .pressAddAuditFilter();

        assertThat(auditTemplatePopup.getCheckboxesTexts())
                .as("Mobile Scan. All checkBoxes are checked and present.")
                .contains("Category Equals Insecure Transport", "Rule ID Equals 4722",
                        "Severity Equals High", "Kingdom Equals Security Features",
                        "URL Equals http://zero.webappsecurity.com:80/banklogin.asp?serviceName=FreebankCaastAccess&" +
                                "templateName=prod_sel.forte&source=Freebank&AD_REFERRING_URL=http://www.Freebank.com");
        assertThat(suppressButton.isChecked()).as("Dynamic Scan. " +
                "Radio button 'Suppress' is present and checked by default.").isTrue();
        assertThat(setSeverityButton.isChecked()).as("Dynamic Scan. " +
                "Radio button 'Set Severity' is present and unchecked by default.").isFalse();

        auditTemplatePopup.pressCreateFilter().waitForModalVisible().pressClose();
        var auditToolsPage = new TenantTopNavbar().openApplications().openYourApplications()
                .openDetailsFor(dynamicApp.getApplicationName())
                .openAuditTools().openAuditTemplate().setScanType(FodCustomTypes.ScanType.Dynamic);
        assertThat(auditToolsPage.getFiltersCount()).as("Only one filter is present").isEqualTo(1);

        var filterRows = auditToolsPage.getAllFilters().get(0).conditionRows;

        assertThat(filterRows).as("All conditions is present").hasSize(5);

        var listOfFilters = IterableUtils.toList(filterRows.asDynamicIterable())
                .stream().map(row -> IterableUtils.toList(row.$$("select").asDynamicIterable())
                        .stream()
                        .map(SelenideElement::text).collect(Collectors.joining(" "))).collect(Collectors.toList());
        assertThat(listOfFilters.toString()).as("Check filters")
                .contains("Category Equals Insecure Transport", "Rule ID Equals", "Severity Equals High",
                        "Kingdom Equals Security Features", "URL Equals");
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate Static Scan Audit Template")
    @Test(groups = {"regression"}, dependsOnMethods = {"testDataPreparation"})
    public void staticScanAuditTemplateTest() {
        var releaseIssuesPage = LogInActions.tamUserLogin(defaultTenantDTO)
                .openYourReleases().openDetailsForRelease(additionalStaticApp).openIssues().clickAll();
        releaseIssuesPage.findWithSearchBox("0344E9F9-4113-4228-9BA4-1696CB402251");
        var auditTemplatePopup = releaseIssuesPage.openIssueByIndex(0)
                .pressAddAuditFilter();

        assertThat(auditTemplatePopup.getCheckboxesTexts())
                .as("Static Scan. All checkBoxes are checked and present.")
                .contains("Category Equals Poor Error Handling: Return Inside Finally",
                        "Rule ID Equals 0344E9F9-4113-4228-9BA4-1696CB402251",
                        "Severity Equals High", "Kingdom Equals Errors",
                        "File Equals src/org/checkers/pkg001/dEfault/ResourceLeakExample.java",
                        "Sink Equals ReturnStatement", "Package Equals Java Structural");
        assertThat(suppressButton.isChecked()).as("Dynamic Scan. " +
                "Radio button 'Suppress' is present and checked by default.").isTrue();
        assertThat(setSeverityButton.isChecked()).as("Dynamic Scan. " +
                "Radio button 'Set Severity' is present and unchecked by default.").isFalse();

        auditTemplatePopup.setSeverity(FodCustomTypes.Severity.Low).pressCreateFilter().waitForModalVisible().pressClose();
        refresh();
        var auditToolsPage = new TenantTopNavbar().openApplications()
                .openDetailsFor(additionalStaticApp.getApplicationName())
                .openAuditTools().openAuditTemplate().setScanType(FodCustomTypes.ScanType.Static);
        assertThat(auditToolsPage.getFiltersCount()).as("Only one filter is present").isEqualTo(1);

        var filterRows = auditToolsPage.getAllFilters().get(0).conditionRows;

        assertThat(filterRows).as("All conditions is present").hasSize(7);

        var listOfFilters = IterableUtils.toList(filterRows.asDynamicIterable())
                .stream().map(row -> IterableUtils.toList(row.$$("select").asDynamicIterable())
                        .stream()
                        .map(SelenideElement::text).collect(Collectors.joining(" "))).collect(Collectors.toList());
        assertThat(listOfFilters.toString()).as("Check filter values")
                .contains("Category Equals Poor Error Handling: Return Inside Finally", "And Rule ID Equals",
                        "And Severity Equals High", "And Kingdom Equals Errors", "And File Equals", "And Sink Equals",
                        "And Package Equals");
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate Mobile Scan Audit Template")
    @Test(groups = {"regression"}, dependsOnMethods = {"testDataPreparation"})
    public void mobileScanAuditTemplateTest() {
        var releaseIssuesPage = LogInActions.tamUserLogin(defaultTenantDTO)
                .openYourReleases().openDetailsForRelease(additionalMobileApp).openIssues().clickAll();
        releaseIssuesPage.findWithSearchBox(4722);
        var auditTemplatePopup = releaseIssuesPage.openIssueByIndex(0)
                .pressAddAuditFilter();

        assertThat(auditTemplatePopup.getCheckboxesTexts())
                .as("Dynamic Scan. All checkBoxes are checked and present.")
                .contains("Category Equals Insecure Transport", "Rule ID Equals 4722",
                        "Severity Equals High", "Kingdom Equals Security Features",
                        "URL Equals http://zero.webappsecurity.com:80/banklogin.asp?serviceName=FreebankCaastAccess&" +
                                "templateName=prod_sel.forte&source=Freebank&AD_REFERRING_URL=http://www.Freebank.com");
        assertThat(suppressButton.isChecked()).as("Dynamic Scan. " +
                "Radio button 'Suppress' is present and checked by default.").isTrue();
        assertThat(setSeverityButton.isChecked()).as("Dynamic Scan. " +
                "Radio button 'Set Severity' is present and unchecked by default.").isFalse();

        auditTemplatePopup.setSeverity(FodCustomTypes.Severity.Critical).pressCreateFilter().waitForModalVisible().pressClose();
        var auditToolsPage = new TenantTopNavbar().openApplications().openYourApplications()
                .openDetailsFor(additionalMobileApp.getApplicationName()).openAuditTools().openAuditTemplate()
                .setScanType(FodCustomTypes.ScanType.Mobile);

        assertThat(auditToolsPage.getFiltersCount()).as("Only one filter is present").isEqualTo(1);
        var filterRows = auditToolsPage.getAllFilters().get(0).conditionRows;
        assertThat(filterRows.size()).as("All conditions is present").isEqualTo(5);

        var listOfFilters = IterableUtils.toList(filterRows.asDynamicIterable())
                .stream().map(row -> IterableUtils.toList(row.$$("select").asDynamicIterable())
                        .stream()
                        .map(SelenideElement::text).collect(Collectors.joining(" "))).collect(Collectors.toList());
        assertThat(listOfFilters.toString()).as("Check filter values")
                .contains("Category Equals Insecure Transport", "Rule ID Equals", "Severity Equals High",
                        "Kingdom Equals Security Features", "URL Equals");
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate release issue page functionality")
    @Test(groups = {"regression"}, dependsOnMethods = {"testDataPreparation"})
    public void releaseIssueTest() {
        var issuesPage = LogInActions.tamUserLogin(defaultTenantDTO).openYourReleases()
                .openDetailsForRelease(dynamicApp).openIssues();

        issuesPage.appliedFilters.clearAll();
        issuesPage.clickAll();
        issuesPage.groupBy("Scan Type");
        assertThat(issuesPage.getIssueGroupsCounts()).hasSize(1).containsKey("Dynamic");

        issuesPage.groupBy("<None: no issue grouping>");

        assertThat(issuesPage.getIssueGroupsCounts()).hasSize(1).containsKey("All Issues");

        issuesPage.setShowSuppressed(true);
        int issuesMedium = issuesPage.getMediumCount();
        int totalCount = issuesPage.getAllCount();

        issuesPage.openIssueByIndex(0).setAuditorStatus("Risk Accepted");
        issuesPage.openIssueByIndex(1).setAuditorStatus("Not an Issue");
        issuesPage.setShowSuppressed(false);

        assertThat(totalCount - 2)
                .as("Issues count should be less on 2 issues after suppressing")
                .isEqualTo(issuesPage.getAllCount());

        var issue = issuesPage.openIssueByIndex(0);
        var issueId = issue.getId();
        issue.openInNewWindow();

        assertThat(BrowserUtil.getTabsCount()).isEqualTo(2);
        Selenide.switchTo().window(1);
        assertThat(url()).contains(issueId);

        Selenide.switchTo().window(0);
        issuesPage.openSettings().addFilterByName("Severity");
        issuesPage.filters.setFilterByName("Severity")
                .expand()
                .clickFilterOptionByName("Medium");
        assertThat(issuesPage.getAllCount()).isEqualTo(issuesPage.getMediumCount()).isEqualTo(issuesMedium);
        issuesPage.appliedFilters.clearAll();
        issuesPage.clickAll();
        issuesPage.groupBy("Category");
        issuesPage.filters.setFilterByName("Category").expand()
                .clickFilterOptionByName("Insecure Transport");

        assertThat(issuesPage.getIssueGroupsCounts()).hasSize(1).containsKey("Insecure Transport");
        assertThat(issuesPage.getAllCount()).isEqualTo(1);

        issuesPage.appliedFilters.clearAll();
        var exportModal = issuesPage.pressExportButton();
        assertThat(exportModal.getMessage())
                .isEqualTo("Data exports run in the background and may take up to several hours to process based " +
                        "on the size of the export and system load. Once complete, you will receive an email to " +
                        "download the export. Do you want to run the export?");
        exportModal.pressYes();

        var secondModal = new ModalDialog();
        assertThat(secondModal.getMessage()).isEqualTo("The export has been queued. " +
                "You will receive an email when it is ready to download.");

        secondModal.pressClose();

        issuesPage.findWithSearchBox("http://zero.webappsecurity.com:80/banklogin");
        assertThat(issuesPage.getAllCount()).isEqualTo(8);

        issuesPage.appliedFilters.clearAll();

        for (var app : new ApplicationDTO[]{mobileApp, dynamicApp, staticApp}) {
            var issuesP = new TenantTopNavbar().openApplications()
                    .openDetailsFor(app.getApplicationName()).openIssues();

            assertThat(issuesP.getTabByName("Recommendations")).isNotNull();
            assertThat(issuesP.getTabByName("More Evidence")).isNotNull();
            assertThat(issuesP.getTabByName("Notes")).isNotNull();
            assertThat(issuesP.getTabByName("Vulnerability")).isNotNull();
            assertThat(issuesP.getTabByName("Screenshots")).isNotNull();
            assertThat(issuesP.getTabByName("History")).isNotNull();

            if (app == dynamicApp) {
                assertThat(issuesP.getTabByName("Headers")).isNotNull();
                assertThat(issuesP.getTabByName("HTTP")).isNotNull();
                assertThat(issuesP.getTabByName("Parameters")).isNotNull();
            }

            if (app == staticApp) {
                var issuesList = issuesP.getIssues();
                assertThat(issuesList).isNotEmpty();
                var smartFix = issuesList.get(0).openSmartFix();
                assertThat(smartFix.getGetSmartFixWindowElement()).isNotNull();
            }
        }
    }

    @MaxRetryCount(3)
    @FodBacklogItem("417021")
    @Owner("svpillai@opentext.com")
    @Severity(SeverityLevel.NORMAL)
    @Description("Suppressed option is enabled by default for existing and new users")
    @Test(groups = {"regression"}, dependsOnMethods = {"testDataPreparation"})
    public void suppressedOptionEnabledTest() {
        AllureReportUtil.info("Verify Suppressed option is enabled by default for existing user");
        LogInActions.adminLogIn();
        verifySuppressedOption(dynamicApp, FodCustomTypes.ScanType.Dynamic);
        verifySuppressedOption(mobileApp, FodCustomTypes.ScanType.Mobile);
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Verify Suppressed option is enabled by default for dynamic scans -new user");
        LogInActions.adminUserLogIn(dynamicTester);
        verifySuppressedOption(dynamicApp, FodCustomTypes.ScanType.Dynamic);
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Verify Suppressed option is enabled by default for mobile scans -new user");
        LogInActions.adminUserLogIn(mobileOperator);
        verifySuppressedOption(mobileApp, FodCustomTypes.ScanType.Mobile);
    }

    public void verifySuppressedOption(ApplicationDTO applicationDTO, FodCustomTypes.ScanType scanType) {
        IssuesPage issuesPage;
        if (scanType.getTypeValue().equals("Dynamic")) {
            issuesPage = new AdminTopNavbar().openDynamic().openDetailsFor(applicationDTO.getApplicationName()).openIssues();
        } else {
            issuesPage = new AdminTopNavbar().openMobile().findScanByAppDto(applicationDTO)
                    .openDetailsFor(applicationDTO.getApplicationName()).openIssues();
        }
        assertThat(issuesPage.getShowFixedSuppressedDropdown().isShowSuppressedChecked())
                .as("Suppressed option should be enabled by default in Issues Page")
                .isTrue();
    }
}