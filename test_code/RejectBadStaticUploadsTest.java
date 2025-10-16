package com.fortify.fod.ui.test.regression;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.WebDriverRunner;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.WaitUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.EntitlementDTO;
import com.fortify.fod.common.entities.TenantDTO;
import com.fortify.fod.data_providers.FodUiTestDataProviders;
import com.fortify.fod.ui.pages.admin.AdminLoginPage;
import com.fortify.fod.ui.pages.admin.statics.scan_jobs.StaticScanCell;
import com.fortify.fod.ui.pages.tenant.applications.release.popups.StartStaticScanPopup;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import com.fortify.fod.ui.test.actions.SiteSettingsActions;
import com.fortify.fod.ui.test.actions.TenantActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.time.Duration;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Supplier;

import static com.codeborne.selenide.Selenide.open;
import static com.codeborne.selenide.Selenide.page;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("ysmal@opentext.com")
@Slf4j
public class RejectBadStaticUploadsTest extends FodBaseTest {

    String siteSetting = "EnablePayloadValidation";
    String expectedValidationMessage = "Uploaded zip file is empty";
    TenantDTO tenantDTO;

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("User should be able to create tenant/application and static scan")
    @Test(groups = {"regression"})
    public void prepareTestData() {
        SiteSettingsActions.setValueInSettings(siteSetting, "true", true);
        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setEntitlementDTO(EntitlementDTO.createDefaultInstance());
        TenantActions.createTenant(tenantDTO, false);
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Bad static uploads should be rejected with right notifications on tenant site")
    @Parameters({"TechnologyStack", "Language Level", "Scan Payload", "Expected Log Message", "Expected Notification Message"})
    @Test(dataProvider = "rejectBadStaticUploadTestArguments", dataProviderClass = FodUiTestDataProviders.class,
            dependsOnMethods = {"prepareTestData"}, groups = {"regression"})
    public void rejectBadStaticUploadsTest(FodCustomTypes.TechnologyStack stack, String langLevel, String payload,
                                           String[] expectedLogMessage,
                                           String expectedNotificationMessage) {
        var applicationDTO = ApplicationActions.createApplication(tenantDTO);
        createStaticScan(stack, langLevel, payload, applicationDTO);

        if (payload.equals("payloads/fod/EmptyZip.zip")) {
            assertThat(page(StartStaticScanPopup.class).getErrorMessage())
                    .as("Scan shouldn't start with empty zip file")
                    .isEqualTo(expectedValidationMessage);
            return;
        }

        BrowserUtil.clearCookiesLogOff();
        validateScan(applicationDTO);
        BrowserUtil.clearCookiesLogOff();

        LogInActions.tamUserLogin(tenantDTO);
        validateLogs(expectedLogMessage);
        ValidateNotifications(expectedNotificationMessage, applicationDTO);
    }

    @AfterClass
    public void disablePayloadValidation() {
        setupDriver("disablePayloadValidation");
        SiteSettingsActions.setValueInSettings(siteSetting, "false", true);
        attachTestArtifacts();
    }


    private void createStaticScan(FodCustomTypes.TechnologyStack language, String languageLevel, String uploadFile,
                                  ApplicationDTO applicationDTO) {
        String entitlement = "Subscription";
        String assessmentTypeStatic = "AUTO-STATIC";

        var scanSetupPage = new TenantTopNavbar().openApplications().openYourReleases()
                .openDetailsForRelease(applicationDTO)
                .openStaticScanSetup();

        scanSetupPage.chooseAssessmentType(assessmentTypeStatic)
                .chooseEntitlement(entitlement)
                .getAdvancedSettingsPanel()
                .expand()
                .chooseTechnologyStack(language)
                .chooseLanguageLevel(languageLevel);

        var scanSetupPopup = scanSetupPage.chooseAuditPreference(FodCustomTypes.AuditPreference.Manual)
                .pressStartScanBtn()
                .uploadFile(uploadFile)
                .pressNextBtn();

        var url = WebDriverRunner.url();
        scanSetupPopup.pressStartScanBtn();

        if (uploadFile.equals("payloads/fod/EmptyZip.zip"))
            return;
        scanSetupPopup.popupElement.shouldNotBe(Condition.visible, Duration.ofMinutes(3));
        if (!WebDriverRunner.url().equals(url))
            open(url);
        scanSetupPage.waitStatus(FodCustomTypes.SetupScanPageStatus.InProgress, FodCustomTypes.SetupScanPageStatus.Canceled);
    }

    private void validateScan(ApplicationDTO applicationDTO) {
        var staticScanJobsPage =
                AdminLoginPage.navigate().login(FodConfig.ADMIN_USER_NAME, FodConfig.ADMIN_PASSWORD).adminTopNavbar.openStatic();

        staticScanJobsPage.appliedFilters.clearAll();
        staticScanJobsPage.findWithSearchBox(applicationDTO.getApplicationName());
        var scans = staticScanJobsPage.getAllScans();
        scans.sort(Comparator.comparing(StaticScanCell::getId).reversed());
        var scanStatus = scans.get(0).getLastJob().waitForJobStatus(FodCustomTypes.JobStatus.Failed).getStatus();

        assertThat(scanStatus)
                .as("Scan status should be equal to Failed")
                .isEqualTo(FodCustomTypes.JobStatus.Failed.getTypeValue());
    }

    private void validateLogs(String[] expectedLogMessage) {
        var logsPage = new TenantTopNavbar().openAdministration().openEventLog();
        var logs = logsPage.getAllLogs();
        boolean logIsPresent = false;
        for (var log : logs) {
            for (var expectedLog : expectedLogMessage) {
                if (log.getNotes().toLowerCase().contains(expectedLog.toLowerCase())) {
                    logIsPresent = true;
                    break;
                }
            }
        }
        assertThat(logIsPresent)
                .as("Log should be present: " + Arrays.toString(expectedLogMessage))
                .isTrue();
    }

    private void ValidateNotifications(String expectedNotificationMessage, ApplicationDTO applicationDTO) {
        expectedNotificationMessage = expectedNotificationMessage
                .replace("Release", applicationDTO.getReleaseName());

        var notificationsPage = new TenantTopNavbar().openNotifications();
        var notificationsList = notificationsPage.getAllNotifications();

        String finalExpectedNotificationMessage = expectedNotificationMessage;
        Supplier<Boolean> sup = () -> {
            boolean logIsPresent = false;
            for (var notificationCell : notificationsList) {
                if (notificationCell.getMessage().toLowerCase()
                        .contains(finalExpectedNotificationMessage.toLowerCase())) {
                    logIsPresent = true;
                }
            }
            return logIsPresent;
        };

        assertThat(WaitUtil.waitForTrue(sup, Duration.ofMinutes(5), true))
                .as("Notification should be present: " + expectedNotificationMessage)
                .isTrue();
    }
}