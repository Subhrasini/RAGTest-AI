package com.fortify.fod.ui.test.regression;

import com.codeborne.pdftest.PDF;
import com.codeborne.pdftest.assertj.Assertions;
import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.elements.Actions;
import com.fortify.fod.common.elements.Filters;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.DynamicScanDTO;
import com.fortify.fod.common.entities.ReportDTO;
import com.fortify.fod.ui.pages.admin.navigation.AdminTopNavbar;
import com.fortify.fod.ui.pages.tenant.TenantLoginPage;
import com.fortify.fod.ui.pages.tenant.applications.YourScansPage;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.DynamicScanActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import com.fortify.fod.ui.test.actions.ReportActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.io.File;
import java.io.IOException;
import java.time.Duration;

import static com.codeborne.selenide.Selenide.page;
import static com.codeborne.selenide.files.FileFilters.withExtension;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("sbehera3@opentext.com")
@Slf4j
public class DynamicScanExportTest extends FodBaseTest {

    public DynamicScanDTO dynamicScanDTO;
    public ApplicationDTO applicationDTO;

    public File expectedFile;
    public String settingName = "FeatureFlags";
    public String settingValue = "HackerLevelInsight=true";
    public String fileName = "payloads/fod/2107709.scan";
    public String expectedText = "<Name>HLI: Detected Libraries : Bootstrap v&gt;= 2.0.0 &amp; &lt;= 2.3.2</Name>";
    public String expectedHLI = "8\n" + " HLI: Detected Libraries";
    public String scanFileName = "payloads/fod/1990971.scan";
    public String groupName = "HLI: Detected Libraries";
    public String debrickedScanFileName = "payloads/fod/debricked_info_HLI_category.fpr";
    public String expectedDebrickedSection = "Debricked Open Source Health metrics for:";

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Set HackerLevelInsight=true flag in SiteSettingsPage")
    @Test(groups = {"hf", "regression"})
    public void setHackerLevelInsightFlagTest() {
        var settingsPage = LogInActions.adminLogIn().adminTopNavbar.openConfiguration();
        String currentValue = settingsPage.getSettingValueByName(settingName);
        if (!currentValue.contains(settingValue)) {
            var popup = settingsPage.openSettingByName(settingName);
            popup.settingValueElement.sendKeys("\n" + settingValue);
            popup.save();
            assertThat(settingsPage.getSettingValueByName(settingName))
                    .as("Verify value is updated in table").containsIgnoringCase(settingValue);
        }
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Create an Application in Tenant, login and start dynamic scan")
    @Test(groups = {"hf", "regression"}, dependsOnMethods = {"setHackerLevelInsightFlagTest"})
    public void createApplicationTest() {
        applicationDTO = ApplicationActions.createApplication(defaultTenantDTO, true);
        dynamicScanDTO = DynamicScanDTO.createDefaultInstance();
        DynamicScanActions.createDynamicScan(dynamicScanDTO, applicationDTO, FodCustomTypes.SetupScanPageStatus.Scheduled,
                FodCustomTypes.SetupScanPageStatus.InProgress);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Export .FPR file from a completed dynamic scan")
    @Test(groups = {"hf", "regression"}, dependsOnMethods = {"createApplicationTest"})
    public void exportFileTest() throws IOException {
        var dynamicScanOverviewPage = LogInActions.adminLogIn()
                .adminTopNavbar.openDynamic()
                .openDetailsFor(applicationDTO.getApplicationName());
        dynamicScanOverviewPage.importScan(fileName, true, true, true);
        dynamicScanOverviewPage.publishScan();
        BrowserUtil.clearCookiesLogOff();

        LogInActions
                .tamUserLogin(FodConfig.TAM_USER_NAME, FodConfig.TAM_PASSWORD, defaultTenantDTO.getTenantName())
                .openYourScans();
        expectedFile = page(YourScansPage.class)
                .getScanByType(applicationDTO, FodCustomTypes.ScanType.Dynamic)
                .waitStatus(FodCustomTypes.ScansPageStatus.Completed)
                .downloadResults();

        assertThat(expectedFile.getName()).as("Verify file is downloaded").contains("_scandata.fpr");
        assertThat(expectedFile).as("File size should be greater than 0").isNotEmpty();
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("518025")
    @Description("Hacker Level Insight Vulns in generated FPR files are presenting a GUID that is blank which breaks that FPR from being imported into SSC")
    @Test(groups = {"hf", "regression"}, dependsOnMethods = {"exportFileTest"})
    public void validateWebInspectFile() {
        TenantLoginPage.navigate();
        assertThat(DynamicScanActions.validateDynamicScanLogs(expectedFile, "/webinspect.xml", expectedText))
                .as("The expected text should present in the file").isTrue();
    }

    @SneakyThrows
    @Owner("kbadia@opentext.com")
    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("557007")
    @Description("Verify HLI findings are grouped under HLI:Detected Libraries instead of <Not Set>")
    @Test(groups = {"regression"},
            dependsOnMethods = {"setHackerLevelInsightFlagTest"})
    public void validateHLICategoryTest() {
        var application = ApplicationActions.createApplication(defaultTenantDTO, true);
        var scanDto = DynamicScanDTO.createDefaultInstance();
        DynamicScanActions.createDynamicScan(scanDto, application, FodCustomTypes.SetupScanPageStatus.Scheduled,
                FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();
        DynamicScanActions.importDynamicScanAdmin(application.getReleaseName(), fileName,
                true,
                true,
                true,
                false,
                true);
        DynamicScanActions.completeDynamicScanAdmin(application, false);
        BrowserUtil.clearCookiesLogOff();

        var applicationDetailsPage = LogInActions.tamUserLogin(defaultTenantDTO)
                .openYourApplications().openDetailsFor(application.getApplicationName());
        var allIssues = applicationDetailsPage.openIssues().groupBy("Category").getAllIssues();
        allIssues.forEach(i -> assertThat(
                i.getGroupHeaders()).as("verify that HLI findings are grouped under HLI:Detected Libraries")
                .containsAnyOf(expectedHLI));
        var reportDTO = ReportDTO.createDefaultInstance();
        reportDTO.setApplicationDTO(application);
        var reportsPage = ReportActions.createReport(reportDTO);
        var file = reportsPage.getReportByName(reportDTO.getReportName())
                .getActionButton(Actions.ReportAction.Download)
                .download(Duration.ofMinutes(3).toMillis(), withExtension("pdf"));
        PDF reportFile = new PDF(file);
        Assertions.assertThat(reportFile)
                .as("report should show HLI: Detected Libraries under Issues section")
                .containsExactText("HLI: Detected Libraries");
    }

    @Owner("sbehera3@opentext.com")
    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("731014")
    @Description("Hacker Level Insight (HLI: Detected Libraries) does not show categories group by and filter")
    @Test(groups = {"hf", "regression"}, dependsOnMethods = {"setHackerLevelInsightFlagTest"})
    public void validateHLICategoryFilterTest() {
        var applicationDTO = ApplicationActions.createApplication(defaultTenantDTO, true);
        var dynamicScanDTO = DynamicScanDTO.createDefaultInstance();
        DynamicScanActions.createDynamicScan(dynamicScanDTO, applicationDTO,
                FodCustomTypes.SetupScanPageStatus.Scheduled,
                FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();
        DynamicScanActions.importDynamicScanAdmin(applicationDTO.getReleaseName(), scanFileName,
                true,
                true,
                false,
                false,
                true);
        DynamicScanActions.completeDynamicScanAdmin(applicationDTO, false);
        assertThat(new AdminTopNavbar().openDynamic()
                .openDetailsFor(applicationDTO.getApplicationName())
                .openIssues()
                .clickAll()
                .groupBy("Category")
                .getGroupHeaders())
                .as("verify that HLI findings are grouped under HLI:Detected Libraries category group")
                .containsAnyOf(groupName);

        Filters filter = new Filters();
        filter.expandSideFiltersDrawer();
        assertThat(filter.getAllFilters())
                .as("There should be a filter with Category")
                .contains("Category");
        filter.setFilterByName("Category").expand();
        assertThat(filter.getAllOptions())
                .as("verify that HLI findings are grouped under HLI:Detected Libraries category filter")
                .containsAnyOf(groupName);
    }

    @Owner("svpillai@opentext.com")
    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("875020")
    @Description("Verify Debricked info in HLI WI findings")
    @Test(groups = {"regression"})
    public void validateDebrickedInfoInHLICategory() {
        var dynamicApp = ApplicationActions.createApplication(defaultTenantDTO, true);
        var dynamicScanDto = DynamicScanDTO.createDefaultInstance();
        DynamicScanActions.createDynamicScan(dynamicScanDto, dynamicApp, FodCustomTypes.SetupScanPageStatus.Scheduled,
                FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Import scan results to admin portal");
        DynamicScanActions.importDynamicScanAdmin(dynamicApp.getReleaseName(), debrickedScanFileName,
                true, true, false, false, true);
        DynamicScanActions.completeDynamicScanAdmin(dynamicApp, false);

        AllureReportUtil.info("Open any issue under the HLI: Detected Libraries category of a completed scan");
        assertThat(new AdminTopNavbar().openDynamic()
                .openDetailsFor(dynamicApp.getApplicationName())
                .openIssues()
                .clickAll()
                .groupBy("Category")
                .getIssuesByHeaders(groupName)
                .get(0)
                .openDetails()
                .openVulnerability()
                .getVisibleSection()
                .text())
                .as(String.format("Validate the issue vulnerability tab now contains '%s' part with values", expectedDebrickedSection))
                .contains(expectedDebrickedSection);
    }
}