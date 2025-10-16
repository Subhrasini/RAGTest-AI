package com.fortify.fod.ui.test.regression;

import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.UniqueRunTag;
import com.fortify.common.utils.WaitUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.elements.ModalDialog;
import com.fortify.fod.common.elements.Table;
import com.fortify.fod.common.entities.*;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("vdubovyk@opentext.com")
@Slf4j
public class WebHooksTest extends FodBaseTest {
    ApplicationDTO webApplicationStaticDTO, webApplicationDynamicDTO, mobileApplicationDTO;
    WebhookDTO webhookForAssignment, webhookForStartComplete, webhookForPauseResume, webhookForCancel, webhookNegative;

    private void init() {
        mobileApplicationDTO = ApplicationDTO.createDefaultMobileInstance();
        webApplicationStaticDTO = ApplicationDTO.createDefaultInstance();
        webApplicationDynamicDTO = ApplicationDTO.createDefaultInstance();

        webhookForAssignment = WebhookDTO.createDefaultInstance();
        webhookForAssignment.setPayloadURL("https://webhook/for/assignment" + UniqueRunTag.generate());
        webhookForStartComplete = WebhookDTO.createDefaultInstance();
        webhookForStartComplete.setPayloadURL("https://webhook/for/start/complete" + UniqueRunTag.generate());
        webhookForPauseResume = WebhookDTO.createDefaultInstance();
        webhookForPauseResume.setPayloadURL("https://webhook/for/pause/resume" + UniqueRunTag.generate());
        webhookForCancel = WebhookDTO.createDefaultInstance();
        webhookForCancel.setPayloadURL("https://webhook/for/cancel" + UniqueRunTag.generate());
        webhookNegative = WebhookDTO.createDefaultInstance();
        webhookNegative.setPayloadURL("https://webhook/for/negative" + UniqueRunTag.generate());
    }

    @FodBacklogItem("419019")
    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Test data preparation")
    @Test(groups = {"regression"})
    public void prepareTestData() {
        init();
        LogInActions.tamUserLogin(defaultTenantDTO.getAssignedUser(),
                FodConfig.TAM_PASSWORD, defaultTenantDTO.getTenantCode());

        ApplicationActions.createApplications(webApplicationStaticDTO, webApplicationDynamicDTO, mobileApplicationDTO);

        WebhookActions.createWebhooks(webhookForAssignment, webhookForStartComplete, webhookForPauseResume,
                webhookForCancel, webhookNegative);
    }

    @FodBacklogItem("419019")
    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Assign Releases Test")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void assignReleasesTest() {
        var assignedWebhookReleases = LogInActions
                .tenantUserLogIn(defaultTenantDTO.getUserName(), FodConfig.TAM_PASSWORD, defaultTenantDTO.getTenantCode())
                .tenantTopNavbar
                .openAdministration()
                .openWebhooks()
                .findWebhook(webhookForAssignment.getPayloadURL())
                .getAllWebhooks()
                .get(0)
                .pressAssignReleases()
                .assignRelease(webApplicationStaticDTO.getReleaseName())
                .pressSave(true)
                .findWebhook(webhookForAssignment.getPayloadURL())
                .getAllWebhooks()
                .get(0)
                .pressAssignReleases()
                .getAllSelectedReleases();

        assertThat(assignedWebhookReleases
                .size())
                .as("There should be only 1 assigned release")
                .isEqualTo(1);

        assertThat(assignedWebhookReleases
                .get(0)
                .getReleaseName())
                .as("Validate assigned release")
                .isEqualTo(webApplicationStaticDTO.getReleaseName());
    }

    @FodBacklogItem("419019")
    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validation of Start/Complete scan trigger")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"},
            enabled = false)
    public void scanStartedCompletedTest() {
        LogInActions
                .tenantUserLogIn(defaultTenantDTO.getUserName(), FodConfig.TAM_PASSWORD, defaultTenantDTO.getTenantCode())
                .tenantTopNavbar
                .openAdministration()
                .openWebhooks()
                .findWebhook(webhookForStartComplete.getPayloadURL())
                .getAllWebhooks()
                .get(0)
                .pressEdit()
                .setScanStarted(true)
                .setScanCompleted(true)
                .clickSave()
                .closeAlert()
                .findWebhook(webhookForStartComplete.getPayloadURL())
                .getAllWebhooks()
                .get(0)
                .pressAssignReleases()
                .assignRelease(webApplicationStaticDTO.getReleaseName())
                .pressSave(true);

        StaticScanActions.createStaticScan(StaticScanDTO.createDefaultInstance(), webApplicationStaticDTO,
                FodCustomTypes.SetupScanPageStatus.Completed);

        assertThat(new TenantTopNavbar()
                .openAdministration()
                .openWebhooks()
                .openWebhooksDeliveries()
                .findWebhook(webhookForStartComplete.getPayloadURL())
                .getAllWebhooks()
                .size())
                .as("Webhook delivery actions should be equal: 2")
                .isEqualTo(2);
    }

    @FodBacklogItem("419019")
    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validation of Pause/Resume scan trigger")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"},
            enabled = false)
    public void scanPausedResumedTest() {
        LogInActions
                .tenantUserLogIn(defaultTenantDTO.getUserName(), FodConfig.TAM_PASSWORD, defaultTenantDTO.getTenantCode())
                .tenantTopNavbar
                .openAdministration()
                .openWebhooks()
                .findWebhook(webhookForPauseResume.getPayloadURL())
                .getAllWebhooks()
                .get(0)
                .pressEdit()
                .setScanPaused(true)
                .setScanResumed(true)
                .clickSave()
                .closeAlert()
                .findWebhook(webhookForPauseResume.getPayloadURL())
                .getAllWebhooks()
                .get(0)
                .pressAssignReleases()
                .assignRelease(webApplicationDynamicDTO.getReleaseName())
                .pressSave(true);

        var dynamicScanDTO = DynamicScanDTO.createDefaultInstance();
        DynamicScanActions.createDynamicScan(dynamicScanDTO, webApplicationDynamicDTO);
        BrowserUtil.clearCookiesLogOff();

        LogInActions
                .adminLogIn()
                .adminTopNavbar
                .openDynamic()
                .openDetailsFor(webApplicationDynamicDTO.getApplicationName())
                .pressPauseButton()
                .pressOkButton();
        BrowserUtil.clearCookiesLogOff();

        assertThat(LogInActions
                .tenantUserLogIn(defaultTenantDTO.getUserName(), FodConfig.TAM_PASSWORD, defaultTenantDTO.getTenantCode())
                .tenantTopNavbar
                .openAdministration()
                .openWebhooks()
                .openWebhooksDeliveries()
                .findWebhook(webhookForPauseResume.getPayloadURL())
                .getAllWebhooks()
                .size())
                .as("Webhook delivery actions should be equal: 1")
                .isEqualTo(1);
        BrowserUtil.clearCookiesLogOff();

        LogInActions
                .adminLogIn()
                .adminTopNavbar
                .openDynamic()
                .openDetailsFor(webApplicationDynamicDTO.getApplicationName())
                .pressResumeButton()
                .pressOkButton();

        assertThat(LogInActions
                .tenantUserLogIn(defaultTenantDTO.getUserName(), FodConfig.TAM_PASSWORD, defaultTenantDTO.getTenantCode())
                .tenantTopNavbar
                .openAdministration()
                .openWebhooks()
                .openWebhooksDeliveries()
                .findWebhook(webhookForPauseResume.getPayloadURL())
                .getAllWebhooks()
                .size())
                .as("Webhook delivery actions should be equal: 2")
                .isEqualTo(2);
    }

    @FodBacklogItem("419019")
    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validation of Cancel scan trigger")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"},
            enabled = false)
    public void scanCancelledTest() {
        LogInActions
                .tenantUserLogIn(defaultTenantDTO.getUserName(), FodConfig.TAM_PASSWORD, defaultTenantDTO.getTenantCode())
                .tenantTopNavbar
                .openAdministration()
                .openWebhooks()
                .findWebhook(webhookForCancel.getPayloadURL())
                .getAllWebhooks()
                .get(0)
                .pressEdit()
                .setScanCancelled(true)
                .clickSave()
                .closeAlert()
                .findWebhook(webhookForCancel.getPayloadURL())
                .getAllWebhooks()
                .get(0)
                .pressAssignReleases()
                .assignRelease(mobileApplicationDTO.getReleaseName())
                .pressSave(true);

        var mobileScanDTO = MobileScanDTO.createDefaultScanInstance();
        MobileScanActions.createMobileScan(mobileScanDTO, mobileApplicationDTO,
                FodCustomTypes.SetupScanPageStatus.Scheduled, FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();

        //todo:add reason for cancel mobile scan to FODCustomTypes
        LogInActions
                .adminLogIn()
                .adminTopNavbar
                .openMobile()
                .openDetailsFor(mobileApplicationDTO.getApplicationName())
                .pressCancelBtn()
                .setReason("Cancelled by customer")
                .pressOkBtn();
        var deliveryPage = LogInActions
                .tenantUserLogIn(defaultTenantDTO.getUserName(), FodConfig.TAM_PASSWORD, defaultTenantDTO.getTenantCode())
                .tenantTopNavbar
                .openAdministration()
                .openWebhooks()
                .openWebhooksDeliveries()
                .findWebhook(webhookForCancel.getPayloadURL());
        WaitUtil.waitFor(
                WaitUtil.Operator.Equals, false, Table::isEmpty, Duration.ofMinutes(2), true);
        assertThat(deliveryPage
                .getAllWebhooks())
                .as("Webhook delivery actions should be equal: 1")
                .hasSize(1);
    }

    @FodBacklogItem("488008")
    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Webhook Alert validation test")
    @Test(groups = {"regression"})
    public void alertValidationTest() {
        AllureReportUtil.info("Validate the Alert for a new webhook when 'Monitor All Application Releases' is off");
        var webhooksPage = LogInActions
                .tamUserLogin(defaultTenantDTO)
                .tenantTopNavbar
                .openAdministration()
                .openWebhooks();

        var fakeUrl = "https://some/fake/url" + UniqueRunTag.generate();
        webhooksPage
                .clickAddWebhook()
                .setPayloadURL(fakeUrl)
                .clickSave();

        var alertModal = new ModalDialog("Alert");

        assertThat(
                alertModal
                        .getMessage())
                .as("Message should be equal to expected")
                .contains("Please make sure to assign releases to the webhook");

        AllureReportUtil.info("Validate the Alert for an existing webhook when 'Monitor All Application Releases' " +
                "was changed from on to off");
        alertModal.pressClose();
        webhooksPage
                .getWebHookByAddress(fakeUrl)
                .pressEdit()
                .setMonitorAllApplicationReleases(true)
                .clickSave()
                .getWebHookByAddress(fakeUrl)
                .pressEdit()
                .setMonitorAllApplicationReleases(false)
                .clickSave();

        assertThat(
                alertModal
                        .getMessage())
                .as("Message should be equal to expected")
                .contains("Please make sure to assign releases to the webhook");

        AllureReportUtil.info("Validate the Alert for an existing webhook when 'Monitor All Application Releases' " +
                "is off and was off before the edit, and there are no releases assigned to the webhook");
        alertModal.pressClose(true);
        webhooksPage
                .getWebHookByAddress(fakeUrl)
                .pressEdit()
                .clickSave();

        assertThat(
                alertModal
                        .getMessage())
                .as("Message should be equal to expected")
                .contains("Please make sure to assign releases to the webhook");
    }

    @FodBacklogItem("419019")
    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Negative webhook tests")
    @Test(groups = {"regression"},
            dependsOnMethods = {"prepareTestData", "alertValidationTest"}, alwaysRun = true)
    public void negativeWebhookTests() {
        var softAssert = new SoftAssertions();
        var addEditWebhookPopup = LogInActions
                .tenantUserLogIn(defaultTenantDTO.getUserName(), FodConfig.TAM_PASSWORD, defaultTenantDTO.getTenantCode())
                .tenantTopNavbar
                .openAdministration()
                .openWebhooks()
                .findWebhook(webhookNegative.getPayloadURL())
                .getAllWebhooks()
                .get(0)
                .pressEdit();

        AllureReportUtil.info("Validate Webhook Alert for bad address schema");
        var webHookPopup = addEditWebhookPopup
                .setPayloadURL("http://949d7de304ed.ngrok.io/hook");
        webHookPopup.clickSave();
        softAssert.assertThat(webHookPopup
                        .editWebhookInfoBlock
                        .getText())
                .as("Validate Info Block error message")
                .contains("The address schema has to be https");

        AllureReportUtil.info("Validate Webhook Alert for bad response code");
        softAssert.assertThat(
                        addEditWebhookPopup
                                .setPayloadURL("https://949d7de304ed.ngrok.io/hook")
                                .clickTestWebhook()
                                .editWebhookInfoBlock
                                .getText())
                .as("Validate Info Block error message")
                .isNotEmpty();

        AllureReportUtil.info("Validate Webhook Alert for empty URL field");
        addEditWebhookPopup
                .payloadURLTextBox
                .clear();
        addEditWebhookPopup.clickSave();
        new ModalDialog().close();

        softAssert.assertThat(addEditWebhookPopup
                        .editWebhookInfoBlock
                        .getText())
                .as("Validate Info Block error message")
                .isNotEmpty();

        AllureReportUtil.info("Validate Webhook Alert for invalid URL field");
        addEditWebhookPopup
                .setPayloadURL("test")
                .clickSave();

        softAssert.assertThat(addEditWebhookPopup
                        .editWebhookInfoBlock
                        .getText())
                .as("Validate Info Block error message")
                .contains("Invalid Address");

        AllureReportUtil.info("Validate Webhook Alert for URL without response");
        addEditWebhookPopup
                .setPayloadURL("https://test")
                .clickTestWebhook();

        softAssert.assertThat(addEditWebhookPopup
                        .editWebhookInfoBlock
                        .getText())
                .as("Validate Info Block error message")
                .isNotEmpty();

        softAssert.assertAll();
    }
}