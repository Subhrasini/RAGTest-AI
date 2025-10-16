package com.fortify.fod.ui.test.regression;

import com.codeborne.pdftest.PDF;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.WebDriverRunner;
import com.fortify.common.ui.config.AllureAttachmentsUtil;
import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.*;
import com.fortify.fod.ui.pages.admin.administration.tenants.tenant.TenantDetailsPage;
import com.fortify.fod.ui.pages.tenant.applications.release.popups.StartStaticScanPopup;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;

import static com.codeborne.pdftest.assertj.Assertions.assertThat;
import static com.codeborne.selenide.Selenide.*;

@Owner("vdubovyk@opentext.com")
@Slf4j
public class MicroServicesScanTest extends FodBaseTest {
    TenantDTO tenantDTO;
    ApplicationDTO appDTO, secondAppDTO;
    String releaseName;
    String microServiceName;

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Create Tenant and Applications for the test")
    @Test(groups = {"regression"})
    public void testDataPreparation() {
        AllureReportUtil.info("Creating Tenant and Entitlement with enabled 'Microservices option for web applications'");
        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setOptionsToEnable(new String[]{"Enable Microservices option for web applications"});
        tenantDTO.setEntitlementDTO(EntitlementDTO.createDefaultInstance());
        TenantActions.createTenant(tenantDTO);
        page(TenantDetailsPage.class).setMicroserviceMaxPayloadSize("10").pressSave();
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Creating Application with enabled microservices option");
        appDTO = ApplicationDTO.createDefaultInstance();
        appDTO.setBusinessCriticality(FodCustomTypes.BusinessCriticality.Medium);
        appDTO.setMicroservicesEnabled(true);
        appDTO.setApplicationMicroservices(new String[]{"Microservice 0", "Microservice 1", "Microservice 2",
                "Microservice 3", "Microservice 4", "Microservice 5", "Microservice 6", "Microservice 7",
                "Microservice 8", "Microservice 9"});
        appDTO.setMicroserviceToChoose("Microservice 3");

        secondAppDTO = ApplicationDTO.createDefaultInstance();
        secondAppDTO.setBusinessCriticality(FodCustomTypes.BusinessCriticality.Medium);
        secondAppDTO.setMicroservicesEnabled(true);
        secondAppDTO.setApplicationMicroservices(new String[]{"Microservice 0", "Microservice 1", "Microservice 2",
                "Microservice 3", "Microservice 4", "Microservice 5"});
        secondAppDTO.setMicroserviceToChoose("Microservice 4");

        ApplicationActions.createApplication(appDTO, tenantDTO, true);
        ApplicationActions.createApplication(secondAppDTO, tenantDTO, false);
        StaticScanActions.importScanTenant(secondAppDTO, "payloads/fod/static.java.fpr");
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verification of Static Scans for Microservices")
    @Test(groups = {"regression"}, dependsOnMethods = {"testDataPreparation"})
    public void microServicesScanTest() {
        AllureReportUtil.info("Verify Static Scan Setup Page for Microservices");
        var staticScanSetupPage = LogInActions.tamUserLogin(tenantDTO)
                .openYourReleases()
                .openDetailsForRelease(appDTO)
                .openStaticScanSetup();
        var startStaticScanPopup = page(StartStaticScanPopup.class);
        staticScanSetupPage.chooseAssessmentType("Static Assessment");
        assertThat(staticScanSetupPage.getAdvancedSettingsPanel().expand()
                .getTechnologyStackDropdown().getOptionsTextValues())
                .as("Technology Stack dropdown should contain predefined values")
                .contains(FodCustomTypes.TechnologyStack.JAVA.getTypeValue(),
                        FodCustomTypes.TechnologyStack.DotNet.getTypeValue(),
                        FodCustomTypes.TechnologyStack.NetCore.getTypeValue(),
                        FodCustomTypes.TechnologyStack.Go.getTypeValue(),
                        FodCustomTypes.TechnologyStack.JS.getTypeValue(),
                        FodCustomTypes.TechnologyStack.Ruby.getTypeValue(),
                        FodCustomTypes.TechnologyStack.PHP.getTypeValue(),
                        FodCustomTypes.TechnologyStack.PYTHON.getTypeValue());

        AllureReportUtil.info("Verify possibility of big payload for microservices");
        staticScanSetupPage.getAdvancedSettingsPanel().expand()
                .chooseTechnologyStack(FodCustomTypes.TechnologyStack.JAVA)
                .chooseLanguageLevel("1.9");

        var popup = staticScanSetupPage.pressStartScanBtn()
                .uploadFile("payloads/fod/apache-jmeter-5_src.zip")
                .pressNextBtn();

        var url = WebDriverRunner.url();
        popup.pressStartScanBtn();
        startStaticScanPopup.progressBar.shouldNotBe(Condition.visible, Duration.ofMinutes(5));

        if (!WebDriverRunner.url().equals(url))
            open(url);
        assertThat(startStaticScanPopup.getErrorMessage())
                .as("Validation message should be visible and equal: 'The source file is too large.' ")
                .isEqualTo("The source file is too large.");
        refresh();
        assertThat(staticScanSetupPage.getScanStatusFromIcon())
                .as("Scan shouldn't start")
                .containsIgnoringCase(FodCustomTypes.SetupScanPageStatus.NotStarted.getTypeValue());

        AllureReportUtil.info("Verify Static Scan for Microservices");
        var pop = staticScanSetupPage
                .pressStartScanBtn()
                .uploadFile("payloads/fod/JavaTestPayload.zip")
                .pressNextBtn();
        url = WebDriverRunner.url();
        pop.pressStartScanBtn();
        startStaticScanPopup.progressBar.shouldNotBe(Condition.visible, Duration.ofMinutes(5));
        if (!WebDriverRunner.url().equals(url))
            open(url);
        staticScanSetupPage.waitStatus(FodCustomTypes.SetupScanPageStatus.InProgress);

        AllureReportUtil.info("Create a new Release with Microservices");
        var secondReleaseDTO = ReleaseDTO.createDefaultInstance();
        secondReleaseDTO.setMicroservice("Microservice 5");
        ReleaseActions.createRelease(appDTO, secondReleaseDTO);

        AllureReportUtil.info("Create a Static Scan with Microservices in the new Release");
        var secondApp = ApplicationDTO.createDefaultInstance();
        secondApp.setReleaseName(secondReleaseDTO.getReleaseName());
        secondApp.setApplicationName(appDTO.getApplicationName());
        StaticScanActions.importScanTenant(secondApp, "payloads/fod/static.java.fpr");
    }

    @MaxRetryCount(3)
    @FodBacklogItem("597009")
    @Owner("svpillai@opentext.com")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify microservice name is present in All types of reports")
    @Parameters({"Report type", "extension"})
    @Test(groups = {"regression"},
            dataProvider = "microServiceReportTypes",
            dataProviderClass = MicroServicesScanTest.class,
            dependsOnMethods = {"testDataPreparation"})
    public void verifyMicroServiceNameInAllReports(FodCustomTypes.ReportFileType reportType, String extension) throws
            IOException, ZipException {
        setTestCaseName(String.format("Validate Report type: %s with: %s", reportType, extension));
        var releases = LogInActions.tamUserLogin(tenantDTO).openYourReleases().
                findWithSearchBox(secondAppDTO.getApplicationName()).getAllReleases();
        releaseName = releases.get(0).getFullReleaseName();
        microServiceName = releases.get(0).getMicroserviceName();
        secondAppDTO.setReleaseName(releaseName);
        var report = ReportDTO.createDefaultInstance();
        report.setFileType(reportType);
        report.setReportTemplate("Static Summary");
        report.setApplicationDTO(secondAppDTO);
        var reportsPage = ReportActions.createReport(report);
        var file = reportsPage.getReportByName(report.getReportName()).downloadReport(extension);
        if (file.getName().contains("pdf")) {
            PDF reportFile = new PDF(file);
            AllureAttachmentsUtil.attachFile(file, report.getReportName(), "pdf", "application/pdf");
            assertThat(reportFile)
                    .as("PDF Report File should contains microservice name")
                    .containsExactText(microServiceName);
        } else {
            var textExist = verifyMicroserviceNameInZipFile(file, microServiceName);
            assertThat(textExist)
                    .as("Microservice name is present in HTML reports")
                    .isTrue();
        }
    }

    boolean verifyMicroserviceNameInZipFile(File zipFile, String microServiceName) throws ZipException, IOException {
        AllureReportUtil.info("Validate zip file");
        String zipFilePath = zipFile.getPath();
        String unzippedFilePath = zipFile.getParent();
        new ZipFile(zipFilePath).extractAll(unzippedFilePath);
        var unzippedFile = Arrays.stream(Objects.requireNonNull(new File(unzippedFilePath).listFiles()))
                .filter(file -> file.getName().contains("html"))
                .findFirst().orElse(null);
        var reportLines = Files.readAllLines(Objects.requireNonNull(unzippedFile).toPath());
        for (var line : reportLines) {
            if (line.contains(microServiceName)) {
                return true;
            }
        }
        return false;
    }

    @DataProvider(name = "microServiceReportTypes", parallel = true)
    public Object[][] microServiceReportTypes() {
        return new Object[][]{
                {FodCustomTypes.ReportFileType.PDF, "pdf"},
                {FodCustomTypes.ReportFileType.HTML, "zip"}
        };
    }
}