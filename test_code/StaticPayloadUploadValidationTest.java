package com.fortify.fod.ui.test.regression;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.WebDriverRunner;
import com.fortify.common.utils.UniqueRunTag;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.StaticScanDTO;
import com.fortify.fod.ui.pages.common.common.cells.EventLogCellMain;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.SiteSettingsActions;
import com.fortify.fod.ui.test.actions.StaticScanActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.codeborne.selenide.Selenide.*;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("ysmal@opentext.com")
@Slf4j
public class StaticPayloadUploadValidationTest extends FodBaseTest {

    String settingName = "MicroserviceMaxPayloadSizeMB";
    String fileToUpload = "payloads/fod/10JavaDefects_Small(OS).zip";
    String messageRejected = "Static Scan Rejected";
    String messageCorrupt = "Corrupt ZIP file";
    String messageNotFoundDirectory = "End of central directory not found";
    String validationMessage = "The source file is too large.";
    ApplicationDTO webApp;
    ApplicationDTO mobileApp;
    ApplicationDTO applicationDTO;
    StaticScanDTO staticScanDTO;

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Update max microservice payload size")
    @Test(groups = {"regression"}, priority = 1)
    public void prepareTestData() {
        SiteSettingsActions.setValueInSettings(settingName, "6", true);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate uploading time and absence of unexpected logs")
    @Test(groups = {"regression"}, priority = 2)
    public void staticPayloadUploadValidationMobileApp() {
        mobileApp = ApplicationDTO.createDefaultMobileInstance();
        var topNavBar = ApplicationActions
                .createApplication(mobileApp, defaultTenantDTO, true)
                .tenantTopNavbar;

        var staticScanSetupPage = topNavBar.openApplications()
                .openYourReleases().openDetailsForRelease(mobileApp)
                .openStaticScanSetup();
        staticScanSetupPage.chooseAssessmentType("Static Basic")
                .chooseEntitlement("Subscription")
                .getAdvancedSettingsPanel().expand()
                .chooseTechnologyStack(FodCustomTypes.TechnologyStack.JAVA)
                .chooseLanguageLevel("1.8");
        var startStaticScanPopup = staticScanSetupPage
                .chooseAuditPreference(FodCustomTypes.AuditPreference.Manual)
                .pressStartScanBtn();

        var url = WebDriverRunner.url();
        var scanSetupPage = startStaticScanPopup.uploadFile(fileToUpload)
                .pressNextBtn().pressStartScanBtn();

        scanSetupPage.spinner.waitTillLoading(1, true);
        var started = LocalTime.now();
        var endTime = started.plusSeconds(100);
        var finishTime = 0;
        int iteration = 0;
        var progressBarElement = "#staticScanUploadProgress .progress-bar";
        float progress = -1;
        do {
            try {
                progress = Float.parseFloat(Objects.requireNonNull($(progressBarElement)
                        .getAttribute("aria-valuenow")));
            } catch (Exception | Error e) {
                log.info(e.getMessage());
                break;
            }

            if (progress == 0 && iteration > 10 && !startStaticScanPopup.startScanButton.isDisplayed()) {
                finishTime = LocalTime.now().minusNanos(started.toNanoOfDay()).toSecondOfDay();
                break;
            }

            iteration++;
        } while (LocalTime.now().isBefore(endTime));

        log.info("Finished in: {} seconds", finishTime);
        if (!WebDriverRunner.url().equals(url))
            open(url);
        scanSetupPage.waitStatus(FodCustomTypes.SetupScanPageStatus.InProgress);

        /*Cancel scanning to avoid overloading scanners*/
        scanSetupPage.openScans().getAllScans(true).get(0).cancelScan();

        assertThat(finishTime)
                .as("File should be uploaded faster then 100 seconds. AR: " + finishTime)
                .isLessThan(100);

        assertThat(topNavBar.openApplications().openDetailsFor(mobileApp.getApplicationName()).openEventLog()
                .getAllLogs().stream().map(EventLogCellMain::getNotes).collect(Collectors.toList()))
                .doesNotContain(messageRejected, messageCorrupt, messageNotFoundDirectory);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate uploading with file size larger than allowed")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"},
            priority = 3)
    public void staticPayloadUploadValidationMicroserviceApp() {
        String microserviceName = "microservice" + UniqueRunTag.generate();
        webApp = ApplicationDTO.createDefaultInstance();
        webApp.setMicroservicesEnabled(true);
        webApp.setMicroserviceToChoose(microserviceName);
        webApp.setApplicationMicroservices(new String[]{microserviceName});
        var topNavBar = ApplicationActions
                .createApplication(webApp, defaultTenantDTO, true)
                .tenantTopNavbar;

        var staticScanSetupPage = topNavBar.openApplications()
                .openYourReleases().openDetailsForRelease(webApp)
                .openStaticScanSetup();
        staticScanSetupPage.chooseAssessmentType("Static Assessment")
                .chooseEntitlement("Subscription")
                .getAdvancedSettingsPanel().expand()
                .chooseTechnologyStack(FodCustomTypes.TechnologyStack.JAVA)
                .chooseLanguageLevel("1.8");
        var startStaticScanPopup = staticScanSetupPage
                .pressStartScanBtn();

        var scanSetupPage = startStaticScanPopup.uploadFile(fileToUpload)
                .pressNextBtn().pressStartScanBtn();

        if (startStaticScanPopup.progressBar.isDisplayed()) {
            startStaticScanPopup.progressBar
                    .shouldNotBe(Condition.visible, Duration.ofMinutes(2));
        }

        var endTime = LocalTime.now().plusMinutes(7);
        do {
            if (!startStaticScanPopup.scanError.isDisplayed()) {
                scanSetupPage.waitStatus(FodCustomTypes.SetupScanPageStatus.InProgress);
                var scansPage = scanSetupPage.openScans();
                scansPage.getAllScans(true).get(0).cancelScan();
                sleep(Duration.ofMinutes(2).toMillis());
                scansPage.openStaticScanSetup().pressStartScanBtn().uploadFile(fileToUpload)
                        .pressNextBtn().pressStartScanBtn();
                startStaticScanPopup.progressBar
                        .shouldBe(Condition.visible, Duration.ofSeconds(15))
                        .shouldNotBe(Condition.visible, Duration.ofMinutes(2));
            } else break;
        } while (LocalTime.now().isBefore(endTime));

        assertThat(startStaticScanPopup.getErrorMessage())
                .as("Validation message should be visible and equal: " + validationMessage)
                .isEqualTo(validationMessage);
        refresh();
        assertThat(scanSetupPage.getScanStatusFromIcon())
                .as("Scan shouldn't start")
                .containsIgnoringCase(FodCustomTypes.SetupScanPageStatus.NotStarted.getTypeValue());
    }

    @AfterClass
    public void setDefaultPayloadSize() {
        setupDriver("setDefaultPayloadSize");
        SiteSettingsActions.setValueInSettings(settingName, "100", true);
        attachTestArtifacts();
    }

    public void init() {

        applicationDTO = ApplicationDTO.createDefaultInstance();

        staticScanDTO = StaticScanDTO.createDefaultInstance();
        staticScanDTO.setAuditPreference(FodCustomTypes.AuditPreference.Manual);
        staticScanDTO.setFileToUpload("payloads/fod/webgoat-sc.zip");
    }

    @Owner("vdubovyk@opentext.com")
    @FodBacklogItem("523012")
    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate Defect 523012. Create and complete Scan Central Static Scan")
    @Test(groups = {"regression"})
    public void testDataPreparation() {
        init();

        ApplicationActions.createApplication(applicationDTO, defaultTenantDTO, true);

        StaticScanActions.createStaticScan(staticScanDTO, applicationDTO, FodCustomTypes.SetupScanPageStatus.Queued,
                FodCustomTypes.SetupScanPageStatus.InProgress);

        StaticScanActions.completeStaticScan(applicationDTO, true);

    }
}