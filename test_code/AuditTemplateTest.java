package com.fortify.fod.ui.test.regression;

import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.EntitlementDTO;
import com.fortify.fod.common.entities.MobileScanDTO;
import com.fortify.fod.common.entities.TenantDTO;
import com.fortify.fod.ui.pages.common.common.cells.IssueCell;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("oradchenko@opentext.com")
@Slf4j
public class AuditTemplateTest extends FodBaseTest {
    ApplicationDTO staticApplicationDTO, mobileApplicationDTO, dynamicApplicationDTO, dynamicApplicationDTO1;
    MobileScanDTO mobileScanDTO;
    TenantDTO tenantDTO;
    List<String> listOfFilterOptions = List.of("(Choose One)", "Severity", "Rule ID", "Kingdom", "Category", "URL",
            "Body", "Headers", "Parameters", "Response", "drc_weakciphers");

    @Description("Create tenant and applications")
    @Test(groups = {"regression"})
    public void prepareTestData() {
        final String ruleId = "D25057C1-E690-4679-93AF-0BE4EDD646B5";

        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setOptionsToEnable(new String[]
                {
                        "Enable False Positive Challenge flag and submission",
                        "Allow scanning with no entitlements",
                        //"Scan Third Party Libraries",
                        "Enable Microservices option for web applications",
                        "Enable Source Download",
                        "Enable advanced Audit Template options"
                });

        tenantDTO.setEntitlementDTO(EntitlementDTO.createDefaultInstance());
        TenantActions.createTenant(tenantDTO);
        BrowserUtil.clearCookiesLogOff();

        LogInActions.tamUserLogin(tenantDTO);
        var auditToolsPage = new TenantTopNavbar()
                .openAdministration().openAuditTools();

        AllureReportUtil.info("Create template for Mobile Scan");
        var filter = auditToolsPage.setScanType(FodCustomTypes.ScanType.Mobile).pressAddFilter();
        filter.setCondition("Category", FodCustomTypes.FilterOperators.Contains.getTypeValue(), "Unhandled Exception");
        filter.setSuppress();
        auditToolsPage.pressSave().close();

        var filters = auditToolsPage.getAllFilters();
        assertThat(filters).as("There should be mobile filter").hasSize(1);

        AllureReportUtil.info("Create template for Dynamic Scan");
        filter = auditToolsPage.setScanType(FodCustomTypes.ScanType.Dynamic).pressAddFilter();
        filter.setCondition(
                FodCustomTypes.FilterOptions.Severity.getTypeValue(),
                FodCustomTypes.FilterOperators.Equals.getTypeValue(),
                FodCustomTypes.Severity.Low.getTypeValue()).addConditionRow();
        filter.setCondition(
                FodCustomTypes.FilterOptions.Severity.getTypeValue(),
                FodCustomTypes.FilterOperators.Equals.getTypeValue(),
                FodCustomTypes.Severity.Medium.getTypeValue()).addConditionRow();
        filter.setCondition(
                FodCustomTypes.FilterOptions.Severity.getTypeValue(),
                FodCustomTypes.FilterOperators.Equals.getTypeValue(),
                FodCustomTypes.Severity.High.getTypeValue());
        filter.setSeverity(FodCustomTypes.Severity.Critical);
        auditToolsPage.pressSave().pressClose();

        filters = auditToolsPage.getAllFilters();
        assertThat(filters).as("There should be dynamic filter").hasSize(1);

        AllureReportUtil.info("Create template for Static Scan");

        filter = auditToolsPage.setScanType(FodCustomTypes.ScanType.Static).pressAddFilter();
        filter.setCondition(
                "Rule ID",
                FodCustomTypes.FilterOperators.Contains.getTypeValue(),
                ruleId);
        filter.setSuppress();
        auditToolsPage.pressSave().pressClose();

        filters = auditToolsPage.getAllFilters();
        assertThat(filters).as("There should be static filter").hasSize(1);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Created filters should affect scan results for Mobile scans")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void mobileAuditTemplateTest() {
        final String[] suppressedIssueTitles = {"/cfmerror.html", "/cfmerror.html", "/pindex.asp"};

        mobileApplicationDTO = ApplicationDTO.createDefaultMobileInstance();

        mobileScanDTO = MobileScanDTO.createDefaultScanInstance();
        mobileScanDTO.setFileToUpload("payloads/fod/flickrj.apk");
        mobileScanDTO.setAssessmentType("AUTO-MOBILE");

        LogInActions.tamUserLogin(tenantDTO);
        ApplicationActions.createApplication(mobileApplicationDTO);

        new TenantTopNavbar().openApplications()
                .openDetailsFor(mobileApplicationDTO.getApplicationName());
        MobileScanActions.createMobileScan(mobileScanDTO, mobileApplicationDTO, FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();
        MobileScanActions.importMobileScanAdmin(mobileApplicationDTO.getApplicationName(),
                FodCustomTypes.ImportFprScanType.Mobile,
                "payloads/fod/dynamic.zero.fpr",
                false,
                false,
                true
        );
        MobileScanActions.completeMobileScan(mobileApplicationDTO, false);
        BrowserUtil.clearCookiesLogOff();
        var issuesPage = LogInActions.tamUserLogin(tenantDTO).tenantTopNavbar.openApplications()
                .openDetailsFor(mobileApplicationDTO.getApplicationName()).openIssues();
        issuesPage.getShowFixedSuppressedDropdown()
                .setShowSuppressed(true);
        var actualSuppressedIssues = issuesPage.clickAll().getAllIssues().stream()
                .filter(i -> i.isSuppressed).collect(Collectors.toList());

        assertThat(actualSuppressedIssues).as("There should be 3 suppressed issues")
                .hasSize(suppressedIssueTitles.length);

        assertThat(actualSuppressedIssues.stream().map(IssueCell::getTitle).collect(Collectors.toList()))
                .as("Issue titles should match expected").containsExactlyInAnyOrder(suppressedIssueTitles);
    }


    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Created filters should affect scan results for Static scans")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void staticAuditTemplateTest() {
        final String issueTitle = "Logger.cs : 21";

        staticApplicationDTO = ApplicationDTO.createDefaultInstance();

        LogInActions.tamUserLogin(tenantDTO);
        ApplicationActions.createApplication(staticApplicationDTO).tenantTopNavbar
                .openApplications().openDetailsFor(staticApplicationDTO.getApplicationName());
        var issuesPage = StaticScanActions
                .importScanTenant(staticApplicationDTO, "payloads/fod/LoggerCS.fpr")
                .openIssues();
        var issues = issuesPage.clickAll().getAllIssues();
        issues.forEach(i -> assertThat(
                i.getTitle()).as(String.format("There should not be an issue with title %s", issueTitle))
                .isNotEqualTo(issueTitle));
        issuesPage.setShowSuppressed(true);
        issues = issuesPage.clickAll().getAllIssues();
        assertThat(issues).as("Issues list should not be empty").isNotEmpty();
        assertThat(issues.stream().filter(i -> i.isSuppressed).findFirst().get().getTitle())
                .as(String.format("Suppressed issue should have title %s", issueTitle)).isEqualTo(issueTitle);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Created filters should affect scan results for Dynamic scans")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void dynamicAuditTemplateTest() {
        dynamicApplicationDTO = ApplicationDTO.createDefaultInstance();

        LogInActions.tamUserLogin(tenantDTO);
        ApplicationActions.createApplication(dynamicApplicationDTO);

        new TenantTopNavbar().openApplications()
                .openDetailsFor(dynamicApplicationDTO.getApplicationName());

        DynamicScanActions.importDynamicScanTenant(dynamicApplicationDTO, "payloads/fod/dynamic.zero.fpr");
        BrowserUtil.clearCookiesLogOff();
        var issues = LogInActions.tamUserLogin(tenantDTO).tenantTopNavbar.openApplications()
                .openDetailsFor(dynamicApplicationDTO.getApplicationName()).openIssues().getIssuesCounters();

        assertThat(issues.getHigh()).as("There should be no High issues").isZero();
        assertThat(issues.getMedium()).as("There should be no Medium issues").isZero();
        assertThat(issues.getLow()).as("There should be no Low issues").isZero();
    }

    @FodBacklogItem("797044")
    @Owner("svpillai@opentext.com")
    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify global audit template filters for dynamic scans ")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void verifyGlobalAuditTemplateFiltersForDynamicScanType() {
        dynamicApplicationDTO1 = ApplicationDTO.createDefaultInstance();
        LogInActions.tamUserLogin(tenantDTO);
        ApplicationActions.createApplication(dynamicApplicationDTO1);
        DynamicScanActions.importDynamicScanTenant(dynamicApplicationDTO1, "payloads/fod/dynamic.zero.fpr");

        AllureReportUtil.info("Verify application audit template filter for a completed dynamic scan");
        assertThat(new TenantTopNavbar().openApplications()
                .openDetailsFor(dynamicApplicationDTO1.getApplicationName())
                .openAuditTools()
                .setScanType(FodCustomTypes.ScanType.Dynamic)
                .pressAddFilter()
                .getVulnerabilityAttributes())
                .as("Filter options should be displayed with expected option list")
                .containsExactlyInAnyOrderElementsOf(listOfFilterOptions);

        AllureReportUtil.info("Verify global audit template filter options for dynamic scan type");
        assertThat(new TenantTopNavbar().openAdministration().openAuditTools()
                .setScanType(FodCustomTypes.ScanType.Dynamic)
                .pressAddFilter()
                .getVulnerabilityAttributes())
                .as("Vulnerability Attributes should contain identical filter options as App level audit templates")
                .containsExactlyInAnyOrderElementsOf(listOfFilterOptions);
    }
}