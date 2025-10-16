package com.fortify.fod.ui.test.regression;

import com.codeborne.pdftest.PDF;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.FileDownloadMode;
import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.UniqueRunTag;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.elements.Actions;
import com.fortify.fod.common.elements.Filters;
import com.fortify.fod.common.entities.*;
import com.fortify.fod.common.utils.CSVHelper;
import com.fortify.fod.common.utils.htmlValidator;
import com.fortify.fod.ui.pages.tenant.TenantLoginPage;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.pages.tenant.reports.dataexport.DataExportPage;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.FodBacklogItems;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;
import com.codeborne.pdftest.PDF;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import net.lingala.zip4j.core.ZipFile;

import static com.codeborne.selenide.Selenide.refresh;
import static com.fortify.fod.api.utils.AccessScopeRestrictionsFodApi.API_TENANT;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.assertj.core.api.Assertions.allOf;
import static org.assertj.core.api.Assertions.assertThat;
import static com.codeborne.pdftest.assertj.Assertions.assertThat;

@Owner("tmagill@opentext.com")
@FodBacklogItem("728008")
@Slf4j
public class ReportGenerateHTMLandPDFMultiReleases extends FodBaseTest {

    TenantDTO tenantDTO;
    ApplicationDTO applicationDTO;
    ReleaseDTO releaseDTO2;
    StaticScanDTO staticScanDTO;
    String fileName = "payloads/fod/10JavaDefects_Small(OS).zip";
    ReportDTO reportDTO;
    DataExportDTO dataExportDTO;

    @MaxRetryCount(1)
    @Description("Create tenant, application, and required 2 releases")
    @Test(groups = {"regression"})
    public void prepareTestData() {
        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setEntitlementDTO(EntitlementDTO.createDefaultInstance());

        TenantActions.createTenant(tenantDTO, true, false);
        BrowserUtil.clearCookiesLogOff();

        applicationDTO = ApplicationActions.createApplication(tenantDTO, true);

        staticScanDTO = StaticScanDTO.createDefaultInstance();
        staticScanDTO.setFileToUpload(fileName);
        StaticScanActions.createStaticScan(staticScanDTO, applicationDTO, FodCustomTypes.SetupScanPageStatus.Completed);

        releaseDTO2 = ReleaseDTO.createDefaultInstance();
        ReleaseActions.createRelease(applicationDTO, releaseDTO2);
        StaticScanActions.createStaticScan(staticScanDTO, applicationDTO, releaseDTO2, FodCustomTypes.SetupScanPageStatus.Completed);
    }

    @MaxRetryCount(1)
    @Description("Check reports for multiple releases for html and pdf")
    @Test(groups = {"regression"}, dataProvider = "reportFileType", dependsOnMethods = {"reportsPageSingleReleaseTest"})
    public void reportsPageMultipleReleasesTest(FodCustomTypes.ReportFileType fileType) throws ZipException {
        var reportName = "ReportName-" + UniqueRunTag.generate();
        var reportsPage = LogInActions.tamUserLogin(tenantDTO).tenantTopNavbar.openReports();
        var reportsWizard = reportsPage.pressNewReportBtn();
        reportsWizard.chooseApplication(applicationDTO.getApplicationName());
        AllureReportUtil.info("Check for existence of radio button on select Application page");
        assertThat(reportsWizard.radioButtonVisible.size()).isGreaterThanOrEqualTo(1);

        reportsWizard.pressNextBtn();
        AllureReportUtil.info("Check for checkboxes on select Release page");
        assertThat(reportsWizard.checkBoxVisible.size()).isGreaterThanOrEqualTo(2);
        reportsWizard.chooseRelease(applicationDTO.getReleaseName());
        reportsWizard.chooseRelease(releaseDTO2.getReleaseName());
        var summaryPage = reportsWizard.pressNextBtn()
                .setReportName(reportName)
                .chooseFileType(fileType)
                .pressNextBtn()
                .selectReportTemplate("Static Summary")
                .pressNextBtn();
        assertThat(summaryPage.overviewRelease.getText())
                .as("Summary Page should have (Multiple) next to Releases:")
                .contains("Multiple");
        AllureReportUtil.info("Report is generated and completed");
        summaryPage.pressGenerateBtn();
        reportsPage.getReportByName(reportName)
                .waitForReportStatus(FodCustomTypes.ReportStatus.Completed, 20);
        var theReport = reportsPage.getReportByName(reportName).downloadReport("zip");
        AllureReportUtil.info("Report is downloaded");
        assertThat(theReport)
                .as("Zip file should be downloaded")
                .isNotEmpty()
                .hasExtension("zip");

        String zipFilePath = theReport.getPath();
        String unzippedFilePath = theReport.getParent();

        AllureReportUtil.info("Zip file is unzipped");
        new ZipFile(zipFilePath).extractAll(unzippedFilePath);

        AllureReportUtil.info("Check number of files in zip");
        var unzippedFiles = Arrays.stream(new File(unzippedFilePath).listFiles())
                .filter(file -> !file.getName().contains("zip")).collect(Collectors.toList());
        assertThat(unzippedFiles.size()).isGreaterThanOrEqualTo(2);
        AllureReportUtil.info("Verifying each file...");
        for (var file : unzippedFiles) {
            assertThat(file.getName()).satisfiesAnyOf(
                    param -> assertThat(param).isEqualTo(reportName + "_" + applicationDTO.getReleaseName() + "." + String.valueOf(fileType).toLowerCase()),
                    param -> assertThat(param).isEqualTo(reportName + "_" + releaseDTO2.getReleaseName() + "." + String.valueOf(fileType).toLowerCase())
            );
        }

        assertThat(reportsPage.getReportByName(reportName).getActionButtonAvailability(Actions.ReportAction.Publish)
                .isEnabled())
                .as("Publish button should not be available for report")
                .isFalse();
        assertThat(reportsPage.getReportByName(reportName).getReleaseName())
                .as("Report should display (Multiple) in Releases column")
                .contains("Multiple");
    }

    @MaxRetryCount(1)
    @Description("Verify publish button availability for single releases")
    @Test(groups = {"regression"}, dataProvider = "reportFileType", dependsOnMethods = {"reportsPageLongReportNameTest"})
    public void reportsPageSingleReleaseTest(FodCustomTypes.ReportFileType fileType) throws ZipException {
        reportDTO = ReportDTO.createDefaultInstance();
        reportDTO.setFileType(fileType);
        reportDTO.setReportTemplate("Static Summary");
        reportDTO.setApplicationDTO(applicationDTO);
        var reportsPage = LogInActions.tamUserLogin(tenantDTO).tenantTopNavbar.openReports();
        reportsPage.createReport(reportDTO);
        reportsPage.getReportByName(reportDTO.getReportName())
                .waitForReportStatus(FodCustomTypes.ReportStatus.Completed, 20);

        AllureReportUtil.info("Check for availability of Publish button");
        assertThat(reportsPage.getReportByName(reportDTO.getReportName())
                .getActionButtonAvailability(Actions.ReportAction.Publish)
                .isEnabled())
                .as("Publish button should be available for release")
                .isTrue();
    }

    @MaxRetryCount(1)
    @Description("Check long report name given matches name of downloaded file")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void reportsPageLongReportNameTest() throws ZipException {
        String reportName = generateLongName();
        reportDTO = ReportDTO.createDefaultInstance();
        reportDTO.setReportTemplate("Static Summary");
        reportDTO.setApplicationDTO(applicationDTO);
        reportDTO.setReportName(reportName);
        var reportsPage = LogInActions.tamUserLogin(tenantDTO).tenantTopNavbar.openReports();
        reportsPage.createReport(reportDTO);


        var getReportCellName = reportsPage.findWithSearchBox(reportName);
        var getThisReportName = getReportCellName.getAllReports().get(0).getReportNameRaw();
        reportsPage.getAllReports().get(0)
                .waitForReportStatus(FodCustomTypes.ReportStatus.Completed, 20);

        var theReport = reportsPage.getAllReports().get(0).downloadReport("pdf");
        AllureReportUtil.info("Report is downloaded");
        assertThat(theReport)
                .as("PDF file should be downloaded")
                .isNotEmpty()
                .hasExtension("pdf");

        AllureReportUtil.info("Check that report name given matches downloaded file name");
        assertThat(theReport.getName())
                .as("Report name given should match name of downloaded file")
                .isEqualTo(getThisReportName + ".pdf");
    }

    @Owner("yliben@opentext.com")
    @MaxRetryCount(1)
    @FodBacklogItems({@FodBacklogItem("1735010"), @FodBacklogItem("1723002"),@FodBacklogItem("1739003")})
    @Description("Verify report template, report created with STIG 6.1,CWE Top 25 2023 Compliance and NIST SP 800-53 Rev. 5 Compliance PDF and Html")
    @Test(groups = {"regression"},dataProvider = "reportType", dependsOnMethods = {"prepareTestData"})
    public void validateReportTemplateComplianceTest(FodCustomTypes.ReportFileType fileType,String reportTemplateOptions,String htmlContentVerify) throws IOException, ZipException {
        var reportName = "ReportName-" + UniqueRunTag.generate();
        var reportsPage = LogInActions.tamUserLogin(tenantDTO).tenantTopNavbar.openReports();
        var reportsWizard = reportsPage.pressNewReportBtn();
        reportsWizard.chooseApplication(applicationDTO.getApplicationName());
        reportsWizard.pressNextBtn();

        reportsWizard.chooseRelease(applicationDTO.getReleaseName());
        reportsWizard.pressNextBtn();
        reportsWizard.setReportName(reportName);
        reportsWizard.chooseFileType(fileType);
        reportsWizard.pressNextBtn();
        assertThat(reportsWizard.getReportTemplateOptions())
                .as("Template option Page should have" + reportTemplateOptions )
                .contains(reportTemplateOptions);
        AllureReportUtil.info("Report is generated and completed " + reportTemplateOptions);
        reportsWizard.selectReportTemplate(reportTemplateOptions);
        reportsWizard.pressNextBtn();

        assertThat(reportsWizard.overviewReportTemplate.getText())
                .as("Summary Page should have " + reportTemplateOptions)
                .contains(reportTemplateOptions);
        AllureReportUtil.info("Report is generated and completed"+ reportTemplateOptions);
        reportsWizard.pressGenerateBtn();
        reportsPage.getReportByName(reportName)
                .waitForReportStatus(FodCustomTypes.ReportStatus.Completed, 20);

        if(fileType.equals(FodCustomTypes.ReportFileType.PDF)){
            AllureReportUtil.info("verify report is generated and completed with PDF"+ reportTemplateOptions);
            PDF file = new PDF(reportsPage.getReportByName(reportName).downloadReport("pdf"));
            assertThat(file).as("PDF report should have "+ reportTemplateOptions)
                    .containsExactText(reportTemplateOptions);;
        }
        else {
            AllureReportUtil.info("verify report is generated and completed with Html" + reportTemplateOptions);
            Configuration.fileDownload = FileDownloadMode.HTTPGET;
            AllureReportUtil.info("Report is downloaded");
            File reportDownLoadFile = reportsPage.getReportByName(reportName).downloadReport("zip");
            assertThat(reportDownLoadFile)

                    .as("Zip file should be downloaded")
                    .isNotEmpty()
                    .hasExtension("zip");

            String unzippedFilePath = reportDownLoadFile.getParent();
            new ZipFile(reportDownLoadFile.getPath()).extractAll(unzippedFilePath);
            var unzippedFiles = Arrays.stream(new File(unzippedFilePath).listFiles())
                    .filter(file -> !file.getName().contains("zip")).collect(Collectors.toList());

            String htmlReportFilePath = unzippedFiles.get(0).getCanonicalPath();
            String htmlContent = new String(Files.readAllBytes(Paths.get(htmlReportFilePath)));
            Document doc = Jsoup.parse(htmlContent);
            Elements headings = doc.select("h1, h2, h3, h4, h5, h6");

            AllureReportUtil.info("Verify HTML report contains report" + reportTemplateOptions);
            Set<String> headingLabels = new HashSet<>();
            for (Element heading : headings) {
                headingLabels.add(heading.text());
            }
            AllureReportUtil.info(headingLabels.toString());
            assertThat(headingLabels.contains(htmlContentVerify))
                    .as("Verify all the required headings present for " + reportTemplateOptions + "report ")
                    .isTrue();
        }
    }

    @Owner("yliben@opentext.com")
    @MaxRetryCount(1)
    @FodBacklogItems({@FodBacklogItem("1735010"), @FodBacklogItem("1723002"),@FodBacklogItem("1739003")})
    @Description("Verify data export for STIG 6.1, CWE Top 25 2023  Compliance and NIST SP 800-53 Rev. 5 Compliance")
    @Test(groups = {"regression"},dataProvider = "reportTemplate",dependsOnMethods = {"prepareTestData","validateReportTemplateComplianceTest"})
    public void verifyDataExportsComplianceTest(String reportTemplate,String reportFilter){
        LogInActions.tamUserLogin(tenantDTO);
        var reportsPages = new TenantTopNavbar().openReports();

        AllureReportUtil.info("Verify new template STIG 6.1 Compliance is displayed as part of template");
        assertThat(reportTemplate).as("Verify " + reportTemplate +" is as part of template list")
                .isIn(reportsPages.openTemplates().getAllTemplateNames());

        AllureReportUtil.info("verify filter should displays" + reportFilter);
        var dataExportWizard = reportsPages.openDataExport().pressCreateExportBtn()
                .setExportTemplate(FodCustomTypes.DataExportTemplate.Issues)
                .pressNextButton();
        assertThat(new Filters().expandAllFilters().getAllFilters().contains(reportFilter))
                .as("filter displays " + reportFilter + " in data export wizard").isTrue();
        dataExportWizard.pressCloseButton();

        var dataExportWizard1 = reportsPages.openDataExport().pressCreateExportBtn()
                .setExportTemplate(FodCustomTypes.DataExportTemplate.Issues)
                .pressNextButton()
                .setDateFrom(1)
                .setDateTo(30)
                .pressNextButton();
        assertThat(
                dataExportWizard1
                        .dataExportsColumns
                        .getText())
                .as("Data exports columns should contain" + reportFilter)
                .contains(reportFilter);
        dataExportWizard1.pressCloseButton();
        refresh();

        AllureReportUtil.info("validate data export created successfully and validate report contain " + reportFilter);
        Configuration.fileDownload = FileDownloadMode.HTTPGET;
        dataExportDTO = DataExportDTO.createDTOWithTemplate(FodCustomTypes.DataExportTemplate.Issues);
        var generatedFromReportsPage = DataExportActions.createDataExportAndDownload(dataExportDTO);
        var dataFromCsv = new CSVHelper(generatedFromReportsPage).getColumnHeaders();
        assertThat(dataFromCsv)
                .as("Validate " + reportFilter + " column exists in the csv")
                .contains(reportFilter);
    }

    @Owner("yliben@opentext.com")
    @MaxRetryCount(1)
    @FodBacklogItems({@FodBacklogItem("1735010"), @FodBacklogItem("1723002"),@FodBacklogItem("1739003")})
    @Description("Verify issues page group by and setting options for STIG 6.1, " +
            "CWE Top 25 2023 Compliance and NIST SP 800-53 Rev. 5 Compliance")
    @Test(dependsOnMethods = {"prepareTestData","verifyDataExportsComplianceTest"},dataProvider = "reportCompliance")
    public void issuesPageTestForComplianceTest(String reportCompliance,String complianceDropDown) {
        LogInActions.tamUserLogin(tenantDTO);
        var issuesPage = new TenantTopNavbar().openApplications()
                .openDetailsFor(applicationDTO.getApplicationName())
                .openIssues();

        AllureReportUtil.info("Verify that the settings popup includes options " + reportCompliance);
       var settingPage =  issuesPage.openIssues().filters.openSettings();
        assertThat(
                settingPage
                        .getVisibleOptions())
                .as("Filtering options should contain option" + reportCompliance)
                .contains(reportCompliance);

        settingPage.openGroupsTab();
        AllureReportUtil.info("Verify Grouping options should contain options" + reportCompliance);
        assertThat(
                settingPage
                        .getVisibleOptions())
                .as("Grouping options should contain option" + reportCompliance)
                .contains(reportCompliance);
        settingPage.pressClose();
        refresh();

        AllureReportUtil.info("Verify Compliance Requirement dropdown should contain the new category " + complianceDropDown);
        assertThat( new TenantTopNavbar().openAdministration()
                .openPolicyManagement()
                .openPolicies()
                .pressAddPolicy()
                .getComplianceRequirementDropdown()
                .getOptionsTextValues())
                .as("Compliance Requirement dropdown should contain" + complianceDropDown)
                .contains(complianceDropDown);
    }

    private String generateLongName() {
        String baseString = "ReportName-";
        for (var x = 0; x < 88; x++) {
            baseString += "x";
        }
        return baseString;
    }

    @DataProvider(name = "reportFileType", parallel = true)
    private static Object[][] reportFileType() {
        return new Object[][]{
                {FodCustomTypes.ReportFileType.PDF},
                {FodCustomTypes.ReportFileType.HTML}
        };
    }
    @DataProvider(name = "reportCompliance",parallel = true)
    public Object[][] reportCompliance() {
        return new Object[][]{
                {"CWE Top 25 2023","CWE Top 25 2023"},
                {"DISA STIG 6.1","STIG 6.1"},
                {"NIST SP 800-53 Rev. 5","NIST SP 800-53 Rev.5"}
        };
    }
    @DataProvider(name = "reportType",parallel = true)
    public Object[][] reportType() {
        return new Object[][]{
                {FodCustomTypes.ReportFileType.PDF,"CWE Top 25 2023 Compliance", "CWE Top 25 2023 Issue Breakdown"},
                {FodCustomTypes.ReportFileType.HTML,"CWE Top 25 2023 Compliance", "CWE Top 25 2023 Issue Breakdown"},
                {FodCustomTypes.ReportFileType.PDF,"STIG 6.1 Compliance", "STIG 6.1 Issue Breakdown"},
                {FodCustomTypes.ReportFileType.HTML,"STIG 6.1 Compliance", "STIG 6.1 Issue Breakdown"},
                {FodCustomTypes.ReportFileType.PDF,"NIST SP 800-53 Rev. 5 Compliance", "NIST SP 800-53 Rev. 5 Issue Breakdown"},
                {FodCustomTypes.ReportFileType.HTML,"NIST SP 800-53 Rev. 5 Compliance", "NIST SP 800-53 Rev. 5 Issue Breakdown"}
        };
    }
    @DataProvider(name = "reportTemplate",parallel = true)
    public Object[][] reportTemplate() {
        return new Object[][]{
                {"CWE Top 25 2023 Compliance","CWE Top 25 2023",},
                {"STIG 6.1 Compliance","STIG 6.1"},
                {"NIST SP 800-53 Rev. 5 Compliance","NIST SP 800-53 Rev. 5"}
        };
    }
}
