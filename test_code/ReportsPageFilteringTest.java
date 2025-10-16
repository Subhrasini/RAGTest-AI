package com.fortify.fod.ui.test.regression;

import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.elements.Filters;
import com.fortify.fod.common.entities.*;
import com.fortify.fod.ui.pages.common.tenant.cells.ReportCell;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;


@Owner("svpillai@opentext.com")
@FodBacklogItem("1702001")
@Slf4j
public class ReportsPageFilteringTest extends FodBaseTest {

    TenantDTO tenantDTO;
    ApplicationDTO dynamicApp, staticApp, mobileApp;
    ReleaseDTO qaTest;
    ReportDTO singleReportDTO, multiReportDTO, dynamicReportDTO, qaReleaseDTO;
    TenantUserDTO securityLead;
    MobileScanDTO mobileScanDTO;
    String staticFprFilePath = "payloads/fod/Compliance-category.fpr";
    LocalDate currentDate = LocalDate.now();
    LocalDate fromDate = currentDate.minusDays(5);
    String dateFormat = "yyyy/MM/dd";

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Create tenant , users , applications, reports for test")
    @Test(groups = {"regression"})
    public void prepareTestData() {
        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setEntitlementDTO(EntitlementDTO.createDefaultInstance());
        dynamicApp = ApplicationDTO.createDefaultInstance();
        staticApp = ApplicationDTO.createDefaultInstance();
        mobileApp = ApplicationDTO.createDefaultMobileInstance();
        mobileScanDTO = MobileScanDTO.createDefaultScanInstance();
        qaTest = ReleaseDTO.createDefaultInstance();
        qaTest.setSdlcStatus(FodCustomTypes.Sdlc.QaTest);
        securityLead = TenantUserDTO.createDefaultInstance();
        securityLead.setTenant(tenantDTO.getTenantCode());
        securityLead.setUserName(securityLead.getUserName() + "-SECLEAD");
        securityLead.setRole(FodCustomTypes.TenantUserRole.SecurityLead);

        AllureReportUtil.info("Create tenant, user , applications and reports for the test");
        TenantActions.createTenant(tenantDTO);
        BrowserUtil.clearCookiesLogOff();
        LogInActions.tamUserLogin(tenantDTO);
        TenantUserActions.createTenantUsers(tenantDTO, securityLead);
        BrowserUtil.clearCookiesLogOff();

        ApplicationActions.createApplication(staticApp, tenantDTO, true);
        ApplicationActions.createApplication(dynamicApp, tenantDTO, false);
        ApplicationActions.createApplication(mobileApp, tenantDTO, false);
        ReleaseActions.createReleases(staticApp, qaTest);
        StaticScanActions.importScanTenant(staticApp, staticFprFilePath);
        DynamicScanActions.importDynamicScanTenant(tenantDTO, dynamicApp, "payloads/fod/dynamic.zero.fpr",
                false);
        new TenantTopNavbar().openApplications().openYourReleases()
                .openDetailsForRelease(staticApp.getApplicationName(), qaTest.getReleaseName())
                .openScans().pressImportByScanType(FodCustomTypes.ScanType.Static)
                .uploadFile(staticFprFilePath).pressImportButton();

        MobileScanActions.createMobileScan(mobileScanDTO, mobileApp, FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();
        MobileScanActions.importMobileScanAdmin(mobileApp.getApplicationName(), FodCustomTypes.ImportFprScanType.Mobile,
                "payloads/fod/mobile_IOS.fpr", false, false, true);
        MobileScanActions.completeMobileScan(mobileApp, false);
        BrowserUtil.clearCookiesLogOff();

        ArrayList<String> releases = new ArrayList<>(Arrays.asList(staticApp.getReleaseName(), qaTest.getReleaseName()));
        singleReportDTO = ReportDTO.createDefaultInstance();
        singleReportDTO.setReportTemplate(FodCustomTypes.ReportTemplateType.Static_Issue_Detail.getTypeValue());
        singleReportDTO.setApplicationDTO(staticApp);
        multiReportDTO = ReportDTO.createDefaultInstance();
        multiReportDTO.setReportTemplate(FodCustomTypes.ReportTemplateType.Static_Summary.getTypeValue());
        multiReportDTO.setApplicationDTO(staticApp);
        multiReportDTO.setAdditionalReleases(releases);
        dynamicReportDTO = ReportDTO.createDefaultInstance();
        dynamicReportDTO.setReportTemplate(FodCustomTypes.ReportTemplateType.DynamicSummary.getTypeValue());
        dynamicReportDTO.setApplicationDTO(dynamicApp);
        qaReleaseDTO = ReportDTO.createDefaultInstance();
        qaReleaseDTO.setReportTemplate(FodCustomTypes.ReportTemplateType.Static_Comprehensive.getTypeValue());
        qaReleaseDTO.setApplicationDTO(staticApp);
        qaReleaseDTO.setAdditionalReleases(releases);

        LogInActions.tamUserLogin(tenantDTO);
        ReportActions.createReport(singleReportDTO);
        ReportActions.createReport(multiReportDTO);
        BrowserUtil.clearCookiesLogOff();

        LogInActions.tenantUserLogIn(securityLead.getUserName(), securityLead.getPassword(), tenantDTO.getTenantCode());
        ReportActions.createReport(qaReleaseDTO);
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify Release , Application name , Report Template , Status filters in Reports Page")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void verifyReportsPageWithApplicationReleaseTemplateStatusFilters() {

        LogInActions.tamUserLogin(tenantDTO);
        var reportsPage = new TenantTopNavbar().openReports();
        assertThat(applyFilterAndVerify("Release", staticApp.getReleaseName(), ReportCell::getReleaseName,
                true))
                .as("Verify Release filter is available and particular release should be visible on selecting it")
                .isTrue();
        reportsPage.appliedFilters.clearAll();

        assertThat(applyFilterAndVerify("Release", "(Multiple)", ReportCell::getReleaseName, true))
                .as("Verify reports created with multiple releases appear correctly when '(Multiple)' release filter is applied")
                .isTrue();
        reportsPage.appliedFilters.clearAll();

        assertThat(applyFilterAndVerify("Report Template",
                FodCustomTypes.ReportTemplateType.Static_Summary.getTypeValue()
                , ReportCell::getReportTemplate, false))
                .as("Verify Report template filter is available and reports with that template should be " +
                        "visible on selecting it")
                .isTrue();
        reportsPage.appliedFilters.clearAll();

        reportsPage.createReport(dynamicReportDTO);
        reportsPage.getReportByName(dynamicReportDTO.getReportName()).waitForReportStatus(FodCustomTypes.ReportStatus.Started);
        assertThat(applyFilterAndVerify("Status", FodCustomTypes.ReportStatus.Started.getTypeValue(),
                ReportCell::getStatus, true))
                .as("Verify Status filter is available and particular report with same status should be " +
                        "visible on selecting it")
                .isTrue();
        reportsPage.appliedFilters.clearAll();

        assertThat(applyFilterAndVerify("Application", staticApp.getApplicationName(),
                ReportCell::getApplicationName, true))
                .as("Verify Application filter is available and particular application should be visible on " +
                        "selecting it")
                .isTrue();
    }

    @FodBacklogItem("1680033")
    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify Created date and created by  filters in Reports Page and Application Reports Page")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void verifyReportsPageWithCreatedDateAndCreatedByFilters() {
        AllureReportUtil.info("Verify Created Date and Created By Filters in  Reports Page");
        LogInActions.tamUserLogin(tenantDTO);
        var reportsPage = new TenantTopNavbar().openReports();
        reportsPage.fromFilter.setValue(dateToFormat(fromDate, dateFormat));
        reportsPage.toFilter.setValue(dateToFormat(currentDate, dateFormat));
        new Filters().setFilterByName("Created Date Range").apply();
        assertThat(verifyCreatedDateInRange(reportsPage::getAllReports, currentDate.minusDays(5), currentDate, dateFormat))
                .as("Verify reports would be filtered based on selected Created Date Range in Reports page")
                .isTrue();
        reportsPage.appliedFilters.clearAll();

        assertThat(applyFilterAndVerify("Created By", securityLead.getUserName(), ReportCell::getCreatedBy,
                false))
                .as("Verify reports would be filtered based on selected Created By user in Reports page")
                .isTrue();

        AllureReportUtil.info("Verify Created Date and Created By Filters in  Application -> Reports Page");
        var appReportsPage = new TenantTopNavbar().openApplications()
                .openDetailsFor(staticApp.getApplicationName()).openReports();
        appReportsPage.fromFilter.setValue(dateToFormat(fromDate, dateFormat));
        appReportsPage.toFilter.setValue(dateToFormat(currentDate, dateFormat));
        new Filters().setFilterByName("Created Date Range").apply();
        assertThat(verifyCreatedDateInRange(reportsPage::getAllReports, currentDate.minusDays(5), currentDate, dateFormat))
                .as("Verify reports would be filtered based on selected Created Date Range in Application " +
                        "Reports page")
                .isTrue();
        appReportsPage.appliedFilters.clearAll();
        new Filters().setFilterByName("Created By").clickFilterOptionByName(securityLead.getUserName());
        assertThat(appReportsPage.getAllReports().stream()
                .map(ReportCell::getCreatedBy)
                .toList())
                .as("Verify reports would be filtered based on selected Created By user in Application-" +
                        " page")
                .contains(securityLead.getUserName());

    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify Static and Hybrid Report Templates")
    @Test(groups = {"regression"}, dataProvider = "staticAndHybridReportTemplateType", dependsOnMethods = {"prepareTestData"})
    public void verifyStaticAndHybridReportTemplate(String reportTemplate, FodCustomTypes.ReportFileType fileType) {
        AllureReportUtil.info("Verify report generation for static and Hybrid templates");
        var reportDTO = ReportDTO.createDefaultInstance();
        reportDTO.setReportTemplate(reportTemplate);
        reportDTO.setApplicationDTO(staticApp);
        reportDTO.setFileType(fileType);

        LogInActions.tamUserLogin(tenantDTO);
        ReportActions.createReport(reportDTO);
        assertThat(new ReportCell(reportDTO.getReportName()).getStatus())
                .as(String.format("Validate %s template report is created and report status is Completed", reportTemplate))
                .isEqualTo(FodCustomTypes.ReportStatus.Completed.getTypeValue());
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify Dynamic scan report templates")
    @Test(groups = {"regression"}, dataProvider = "dynamicReportTemplateType", dependsOnMethods = {"prepareTestData"})
    public void verifyDynamicReportTemplate(String reportTemplate, FodCustomTypes.ReportFileType fileType) {
        AllureReportUtil.info("Verify report generation for dynamic templates");
        var reportDTO = ReportDTO.createDefaultInstance();
        reportDTO.setReportTemplate(reportTemplate);
        reportDTO.setApplicationDTO(dynamicApp);
        reportDTO.setFileType(fileType);

        LogInActions.tamUserLogin(tenantDTO);
        ReportActions.createReport(reportDTO);
        assertThat(new ReportCell(reportDTO.getReportName()).getStatus())
                .as(String.format("Validate %s template report is created and report status is Completed", reportTemplate))
                .isEqualTo(FodCustomTypes.ReportStatus.Completed.getTypeValue());
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify Mobile Scans Report Templates")
    @Test(groups = {"regression"}, dataProvider = "mobileReportTemplateType", dependsOnMethods = {"prepareTestData"})
    public void verifyMobileReportTemplate(String reportTemplate, FodCustomTypes.ReportFileType fileType) {
        AllureReportUtil.info("Verify report generation for mobile templates");
        var reportDTO = ReportDTO.createDefaultInstance();
        reportDTO.setReportTemplate(reportTemplate);
        reportDTO.setApplicationDTO(mobileApp);
        reportDTO.setFileType(fileType);

        LogInActions.tamUserLogin(tenantDTO);
        ReportActions.createReport(reportDTO);
        assertThat(new ReportCell(reportDTO.getReportName()).getStatus())
                .as(String.format("Validate %s template report is created and report status is Completed", reportTemplate))
                .isEqualTo(FodCustomTypes.ReportStatus.Completed.getTypeValue());
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify report templates for different compliance categories")
    @Test(groups = {"regression"}, dataProvider = "complianceCategoryReportTemplateType", dependsOnMethods = {"prepareTestData"})
    public void verifyComplianceCategoryReportTemplate(String reportTemplate, FodCustomTypes.ReportFileType fileType) {
        AllureReportUtil.info("Verify report generation for  compliance category templates");
        var reportDTO = ReportDTO.createDefaultInstance();
        reportDTO.setReportTemplate(reportTemplate);
        reportDTO.setApplicationDTO(staticApp);
        reportDTO.setFileType(fileType);

        LogInActions.tamUserLogin(tenantDTO);
        ReportActions.createReport(reportDTO);
        assertThat(new ReportCell(reportDTO.getReportName()).getStatus())
                .as(String.format("Validate %s template report is created and report status is Completed", reportTemplate))
                .isEqualTo(FodCustomTypes.ReportStatus.Completed.getTypeValue());
    }

    @DataProvider(name = "staticAndHybridReportTemplateType")
    public static Object[][] staticAndHybridReportTemplateType() {
        return new Object[][]{
                {FodCustomTypes.ReportTemplateType.Static_Issue_Detail.getTypeValue(), FodCustomTypes.ReportFileType.PDF},
                {FodCustomTypes.ReportTemplateType.Static_Issue_Detail.getTypeValue(), FodCustomTypes.ReportFileType.HTML},
                {FodCustomTypes.ReportTemplateType.Static_Comprehensive.getTypeValue(), FodCustomTypes.ReportFileType.PDF},
                {FodCustomTypes.ReportTemplateType.Static_Comprehensive.getTypeValue(), FodCustomTypes.ReportFileType.HTML},
                {FodCustomTypes.ReportTemplateType.Static_Summary.getTypeValue(), FodCustomTypes.ReportFileType.PDF},
                {FodCustomTypes.ReportTemplateType.Static_Summary.getTypeValue(), FodCustomTypes.ReportFileType.HTML},
                {FodCustomTypes.ReportTemplateType.Static_Analysis_Trace.getTypeValue(), FodCustomTypes.ReportFileType.PDF},
                {FodCustomTypes.ReportTemplateType.Static_Analysis_Trace.getTypeValue(), FodCustomTypes.ReportFileType.HTML},
                {FodCustomTypes.ReportTemplateType.HybridComprehensive.getTypeValue(), FodCustomTypes.ReportFileType.PDF},
                {FodCustomTypes.ReportTemplateType.HybridComprehensive.getTypeValue(), FodCustomTypes.ReportFileType.HTML},
                {FodCustomTypes.ReportTemplateType.HybridIssueDetail.getTypeValue(), FodCustomTypes.ReportFileType.PDF},
                {FodCustomTypes.ReportTemplateType.HybridIssueDetail.getTypeValue(), FodCustomTypes.ReportFileType.HTML},
                {FodCustomTypes.ReportTemplateType.HybridSummary.getTypeValue(), FodCustomTypes.ReportFileType.PDF},
                {FodCustomTypes.ReportTemplateType.HybridSummary.getTypeValue(), FodCustomTypes.ReportFileType.HTML}
        };
    }

    @DataProvider(name = "dynamicReportTemplateType")
    public static Object[][] dynamicReportTemplateType() {
        return new Object[][]{
                {FodCustomTypes.ReportTemplateType.DynamicSummary.getTypeValue(), FodCustomTypes.ReportFileType.PDF},
                {FodCustomTypes.ReportTemplateType.DynamicSummary.getTypeValue(), FodCustomTypes.ReportFileType.HTML},
                {FodCustomTypes.ReportTemplateType.DynamicComprehensive.getTypeValue(), FodCustomTypes.ReportFileType.PDF},
                {FodCustomTypes.ReportTemplateType.DynamicComprehensive.getTypeValue(), FodCustomTypes.ReportFileType.HTML},
                {FodCustomTypes.ReportTemplateType.DynamicIssueDetail.getTypeValue(), FodCustomTypes.ReportFileType.PDF},
                {FodCustomTypes.ReportTemplateType.DynamicIssueDetail.getTypeValue(), FodCustomTypes.ReportFileType.HTML},
                {FodCustomTypes.ReportTemplateType.DynamicRequestResponse.getTypeValue(), FodCustomTypes.ReportFileType.PDF},
                {FodCustomTypes.ReportTemplateType.DynamicRequestResponse.getTypeValue(), FodCustomTypes.ReportFileType.HTML}
        };
    }

    @DataProvider(name = "mobileReportTemplateType")
    public static Object[][] mobileReportTemplateType() {
        return new Object[][]{
                {FodCustomTypes.ReportTemplateType.Mobile_Issue_Detail.getTypeValue(), FodCustomTypes.ReportFileType.PDF},
                {FodCustomTypes.ReportTemplateType.Mobile_Issue_Detail.getTypeValue(), FodCustomTypes.ReportFileType.HTML},
                {FodCustomTypes.ReportTemplateType.Mobile_Summary.getTypeValue(), FodCustomTypes.ReportFileType.PDF},
                {FodCustomTypes.ReportTemplateType.Mobile_Summary.getTypeValue(), FodCustomTypes.ReportFileType.HTML}
        };
    }

    @DataProvider(name = "complianceCategoryReportTemplateType")
    public static Object[][] complianceCategoryReportTemplateType() {
        return new Object[][]{
                {FodCustomTypes.ReportTemplateType.PCI_31_DSS_Compliance.getTypeValue(), FodCustomTypes.ReportFileType.PDF},
                {FodCustomTypes.ReportTemplateType.PCI_31_DSS_Compliance.getTypeValue(), FodCustomTypes.ReportFileType.HTML},
                {FodCustomTypes.ReportTemplateType.PCI_32_DSS_Compliance.getTypeValue(), FodCustomTypes.ReportFileType.PDF},
                {FodCustomTypes.ReportTemplateType.PCI_32_DSS_Compliance.getTypeValue(), FodCustomTypes.ReportFileType.HTML},
                {FodCustomTypes.ReportTemplateType.PCI_40_DSS_Compliance.getTypeValue(), FodCustomTypes.ReportFileType.PDF},
                {FodCustomTypes.ReportTemplateType.PCI_40_DSS_Compliance.getTypeValue(), FodCustomTypes.ReportFileType.HTML},
                {FodCustomTypes.ReportTemplateType.PCI_401_DSS_Compliance.getTypeValue(), FodCustomTypes.ReportFileType.PDF},
                {FodCustomTypes.ReportTemplateType.PCI_401_DSS_Compliance.getTypeValue(), FodCustomTypes.ReportFileType.HTML},
                {FodCustomTypes.ReportTemplateType.PCI_SSF_12_DSS_Compliance.getTypeValue(), FodCustomTypes.ReportFileType.PDF},
                {FodCustomTypes.ReportTemplateType.PCI_SSF_12_DSS_Compliance.getTypeValue(), FodCustomTypes.ReportFileType.HTML},
                {FodCustomTypes.ReportTemplateType.CWETop252023Compliance.getTypeValue(), FodCustomTypes.ReportFileType.PDF},
                {FodCustomTypes.ReportTemplateType.CWETop252023Compliance.getTypeValue(), FodCustomTypes.ReportFileType.HTML},
                {FodCustomTypes.ReportTemplateType.CWETop252024Compliance.getTypeValue(), FodCustomTypes.ReportFileType.PDF},
                {FodCustomTypes.ReportTemplateType.CWETop252024Compliance.getTypeValue(), FodCustomTypes.ReportFileType.HTML},
                {FodCustomTypes.ReportTemplateType.NIST_SP_800_53_Rev_5_Compliance.getTypeValue(), FodCustomTypes.ReportFileType.PDF},
                {FodCustomTypes.ReportTemplateType.NIST_SP_800_53_Rev_5_Compliance.getTypeValue(), FodCustomTypes.ReportFileType.HTML},
                {FodCustomTypes.ReportTemplateType.STIG_61_Compliance.getTypeValue(), FodCustomTypes.ReportFileType.PDF},
                {FodCustomTypes.ReportTemplateType.STIG_61_Compliance.getTypeValue(), FodCustomTypes.ReportFileType.HTML},
                {FodCustomTypes.ReportTemplateType.STIG_62_Compliance.getTypeValue(), FodCustomTypes.ReportFileType.PDF},
                {FodCustomTypes.ReportTemplateType.STIG_62_Compliance.getTypeValue(), FodCustomTypes.ReportFileType.HTML}
        };
    }

    private String dateToFormat(LocalDate date, String pattern) {
        return DateTimeFormatter.ofPattern(pattern).format(date);
    }

    private <T> boolean applyFilterAndVerify(String filterName, String filterValue, Function<ReportCell, T> extractor
            , boolean isCheckBox) {
        var reportsPage = new TenantTopNavbar().openReports();
        reportsPage.filters.expandAllFilters();
        Filters filters = new Filters();
        if (isCheckBox) {
            filters.setFilterByName(filterName).clickFilterCheckboxByName(filterValue).apply();
        } else {
            filters.setFilterByName(filterName).clickFilterOptionByName(filterValue);
        }
        return reportsPage.getAllReports().stream()
                .map(extractor)
                .toList()
                .contains(filterValue);
    }

    private boolean verifyCreatedDateInRange(
            Supplier<List<ReportCell>> reportSupplier,
            LocalDate fromDate,
            LocalDate toDate,
            String dateFormat
    ) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormat);

        return reportSupplier.get().stream()
                .map(ReportCell::getCreatedDate)
                .map(date -> date.split(" ")[0])
                .map(date -> LocalDate.parse(date, formatter))
                .allMatch(date -> !date.isBefore(fromDate) && !date.isAfter(toDate));
    }
}
