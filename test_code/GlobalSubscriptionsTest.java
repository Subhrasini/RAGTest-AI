package com.fortify.fod.ui.test.regression;

import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.MailUtil;
import com.fortify.common.utils.WaitUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.*;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.pages.tenant.notifications.subscriptions.MySubscriptionsPage;
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

import java.time.Duration;
import java.util.function.Supplier;

import static com.codeborne.selenide.Selenide.refresh;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("sbehera3@opentext.com")
@FodBacklogItem("736006")
@Slf4j
public class GlobalSubscriptionsTest extends FodBaseTest {

    TenantDTO tenantDTO;
    ApplicationDTO applicationDTO, applicationDTO_1, applicationDTO_2,
            applicationDTO_3, applicationDTO_4, applicationDTO_5, applicationDTO_6;
    TenantUserDTO appLead;
    MySubscriptionsPage mySubscriptionsPage;
    StaticScanDTO staticScanDTO;
    String noteName, targetToList_TAM, targetToList_SecLead,
            targetToList_OtherUser, triggerName, triggerName_User, subjectLine;
    MailUtil mailUtil = new MailUtil();
    TenantTopNavbar tenantTopNavbar = new TenantTopNavbar();

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Admin user should be able to create tenant and entitlement on admin site.")
    @Test(groups = {"regression"})
    public void prepareTestData() {

        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setEntitlementDTO(EntitlementDTO.createDefaultInstance());
        TenantActions.createTenant(tenantDTO, true);
        BrowserUtil.clearCookiesLogOff();
        TenantUserActions.activateSecLead(tenantDTO, true);
        appLead = TenantUserDTO.createInstanceWithUserRole(FodCustomTypes.TenantUserRole.ApplicationLead);
        appLead.setTenant(tenantDTO.getTenantName());
        TenantUserActions.createTenantUser(appLead);
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate send email in TAM User,Sec Lead and other user in Global Subscriptions and My Subscription")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void validateSendEmailSubscriptionTest() {

        noteName = String.format("FLASH-NOTE-%s", tenantDTO.getRunTag());
        AllureReportUtil.info("- ***TAM User, Global Subscriptions***");
        mySubscriptionsPage = LogInActions.tamUserLogin(tenantDTO)
                .tenantTopNavbar.openNotifications().openMySubscriptions()
                .openGlobalSubscriptions();
        var allSubscriptions = mySubscriptionsPage.getAllSubscriptions();
        for (var ele : allSubscriptions) {
            assertThat(ele.getSendEmailsCheckbox().isDisabled())
                    .as("Send Emails rows are disabled").isTrue();
        }
        editSubscriptionToYes("Scan Started");
        editSubscriptionToNo("Scan Started");
        verifyDefaultSendEmailsYes("Scan Paused");
        verifyDefaultSendEmailsYes("Scan Canceled");

        AllureReportUtil.info(" ***TAM User, My Subscriptions***");
        String trigger = "Application Created";
        mySubscriptionsPage.tenantTopNavbar.openNotifications().openMySubscriptions();
        mySubscriptionsPage.addSubscription(trigger, noteName, null, null);
        editSubscriptionToYes(trigger);
        editSubscriptionToNo(trigger);
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info(" ***Security Lead, Global Subscriptions***");
        mySubscriptionsPage = LogInActions
                .tenantUserLogIn(tenantDTO.getUserName(), FodConfig.TAM_PASSWORD,
                        tenantDTO.getTenantCode())
                .tenantTopNavbar.openNotifications().openMySubscriptions().openGlobalSubscriptions();
        editSubscriptionToYes("Payload Has No Source Files");
        editSubscriptionToNo("Payload Has No Source Files");
        verifyDefaultSendEmailsYes("Scan Paused");
        verifyDefaultSendEmailsYes("Scan Canceled");

        AllureReportUtil.info(" ***Security Lead, My Subscriptions***");
        String trigger_secLead = "Application Deleted";
        mySubscriptionsPage.tenantTopNavbar.openNotifications().openMySubscriptions();
        mySubscriptionsPage.addSubscription(trigger_secLead, noteName, null, null);
        editSubscriptionToYes(trigger_secLead);
        editSubscriptionToNo(trigger_secLead);
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info(" ***Other Users, Global Subscriptions***");
        mySubscriptionsPage = LogInActions
                .tenantUserLogIn(appLead.getUserName(), FodConfig.TAM_PASSWORD,
                        tenantDTO.getTenantCode())
                .tenantTopNavbar.openNotifications().openMySubscriptions().openGlobalSubscriptions();

        verifyViewPopup("Payload Has No Source Files", "No");
        verifyViewPopup("Scan Paused", "Yes");
        verifyViewPopup("Scan Canceled", "Yes");

        AllureReportUtil.info(" ***Other Users, My Subscriptions***");
        String trigger_Other = "Release Created";
        mySubscriptionsPage.tenantTopNavbar.openNotifications().openMySubscriptions();
        mySubscriptionsPage.addSubscription(trigger_Other, noteName, null, null);
        editSubscriptionToYes(trigger_Other);
        editSubscriptionToNo(trigger_Other);

    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate send email in TAM User,Sec Lead and other user in Global Subscriptions and My Subscription")
    @Test(groups = {"regression"}, dependsOnMethods = {"validateSendEmailSubscriptionTest"})
    public void validateEmailNotificationTest() {

        targetToList_SecLead = tenantDTO.getUserEmail();
        staticScanDTO = StaticScanDTO.createDefaultInstance();
        staticScanDTO.setAuditPreference(FodCustomTypes.AuditPreference.Manual);
        applicationDTO = ApplicationDTO.createDefaultInstance();
        applicationDTO_1 = ApplicationDTO.createDefaultInstance();
        applicationDTO_2 = ApplicationDTO.createDefaultInstance();
        applicationDTO_3 = ApplicationDTO.createDefaultInstance();
        applicationDTO_4 = ApplicationDTO.createDefaultInstance();
        applicationDTO_5 = ApplicationDTO.createDefaultInstance();
        applicationDTO_6 = ApplicationDTO.createDefaultInstance();
        targetToList_TAM = "tam@fod.auto";
        targetToList_OtherUser = appLead.getUserEmail();
        triggerName = "Application Business Criticality Updated";
        triggerName_User = "Release SDLC Status Updated";

        AllureReportUtil.info("Check that TAM, SecLead and Other User got the email about the action you've triggered");
        mySubscriptionsPage = LogInActions.tenantUserLogIn(tenantDTO.getUserName(),
                        FodConfig.TAM_PASSWORD, tenantDTO.getTenantCode())
                .tenantTopNavbar.openNotifications().openMySubscriptions()
                .openGlobalSubscriptions();
        editSubscriptionToYes(triggerName);
        ApplicationActions.createApplications(applicationDTO, applicationDTO_1, applicationDTO_2,
                applicationDTO_3, applicationDTO_4, applicationDTO_5, applicationDTO_6);
        tenantTopNavbar.openAdministration().openUsers()
                .pressAssignApplicationsByUser(appLead.getUserName())
                .openAvailableTab()
                .selectAssignAllCheckbox().pressSave();
        tenantTopNavbar.openApplications().openDetailsFor(applicationDTO.getApplicationName())
                .openSettings().openAppSummaryTab()
                .setBusinessCriticality(FodCustomTypes.BusinessCriticality.Medium)
                .pressSave();
        mySubscriptionsPage.tenantTopNavbar.openNotifications().openMySubscriptions().openGlobalSubscriptions();
        verifyEmail("[OpenText™ Core Application Security] Business Criticality Updated - " + applicationDTO.getApplicationName());

        AllureReportUtil.info("Check that all three users got only the notification but not email" +
                " about the action you've triggered");
        editSubscriptionToNo(triggerName);
        tenantTopNavbar.openApplications().openDetailsFor(applicationDTO_1.getApplicationName())
                .openSettings().openAppSummaryTab()
                .setBusinessCriticality(FodCustomTypes.BusinessCriticality.Low)
                .pressSave();
        verifyNotification(String.format("Business criticality has been updated for " +
                applicationDTO_1.getApplicationName()));
        verifyNoOneReceivedEmail("[OpenText™ Core Application Security] Business Criticality Updated - " +
                applicationDTO_1.getApplicationName());

        AllureReportUtil.info("Check that that TAM, SecLead and Other User got the email about the scan paused " +
                "and scan canceled action");
        StaticScanActions.createStaticScan(staticScanDTO, applicationDTO,
                FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();
        var staticScanOverviewPage = LogInActions.adminLogIn().adminTopNavbar.openStatic()
                .findScanByAppName(applicationDTO.getApplicationName())
                .openDetails().pressPauseButton().pressOkButton();
        refresh();
        assertThat(staticScanOverviewPage.getStatus())
                .as("Static Scan status should be changed to Paused ")
                .isEqualTo(FodCustomTypes.ScansDetailsPageStatus.WaitingCustomer.getTypeValue());
        verifyEmail("[OpenText™ Core Application Security] Scan for " + applicationDTO.getApplicationName() +
                " has been paused for Significant Payload Difference from Previous Release .");

        staticScanOverviewPage.pressCancelBtn().setReason("Cancelled by customer").setRefund(false).pressOkBtn();
        refresh();
        assertThat(staticScanOverviewPage.getStatus())
                .as("Static Scan status should be changed to Canceled ")
                .isEqualTo(FodCustomTypes.ScansDetailsPageStatus.Canceled.getTypeValue());
        verifyEmail("[OpenText™ Core Application Security] Scan for " + applicationDTO.getApplicationName() +
                " has been Cancelled by customer.");
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("My Subscriptions - Emails-TAM");
        LogInActions.tamUserLogin(tenantDTO)
                .tenantTopNavbar.openNotifications().openMySubscriptions()
                .addSubscriptionWithEmail("Release SDLC Status Updated", "For Testing TAM",
                        null, null);
        tenantTopNavbar.openApplications().openYourReleases()
                .openDetailsForRelease(applicationDTO).openReleaseSettings()
                .setSDLCStatus(FodCustomTypes.Sdlc.QaTest)
                .pressSave();
        mySubscriptionsPage.tenantTopNavbar.openNotifications().openMySubscriptions();
        verifyEmailUser("[OpenText™ Core Application Security] Release SDLC Status Updated - " + applicationDTO.getApplicationName(),
                targetToList_TAM, targetToList_SecLead, targetToList_OtherUser);
        editSubscriptionToNo("Release SDLC Status Updated");
        tenantTopNavbar.openApplications().openYourReleases()
                .openDetailsForRelease(applicationDTO_2).openReleaseSettings()
                .setSDLCStatus(FodCustomTypes.Sdlc.Production)
                .pressSave();
        verifyNotification("Release " + applicationDTO_2.getReleaseName() + " has been moved to Production for "
                + applicationDTO_2.getApplicationName());
        verifyNoOneReceivedEmail("[OpenText™ Core Application Security] Release SDLC Status Updated - " +
                applicationDTO_2.getApplicationName());
        deleteSubscription(triggerName_User);
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("My Subscriptions - Emails-SecLead");
        LogInActions.tenantUserLogIn(tenantDTO.getUserName(), FodConfig.TAM_PASSWORD,
                        tenantDTO.getTenantCode())
                .tenantTopNavbar.openNotifications().openMySubscriptions()
                .addSubscriptionWithEmail(triggerName_User, "For Testing SecLead",
                        null, null);
        tenantTopNavbar.openApplications().openYourReleases()
                .openDetailsForRelease(applicationDTO_3).openReleaseSettings()
                .setSDLCStatus(FodCustomTypes.Sdlc.QaTest)
                .pressSave();
        mySubscriptionsPage.tenantTopNavbar.openNotifications().openMySubscriptions();
        verifyEmailUser("[OpenText™ Core Application Security] Release SDLC Status Updated - " + applicationDTO_3.getApplicationName(),
                targetToList_SecLead, targetToList_TAM, targetToList_OtherUser);
        editSubscriptionToNo(triggerName_User);
        tenantTopNavbar.openApplications().openYourReleases()
                .openDetailsForRelease(applicationDTO_4).openReleaseSettings()
                .setSDLCStatus(FodCustomTypes.Sdlc.Production)
                .pressSave();
        verifyNotification("Release " + applicationDTO_4.getReleaseName() +
                " has been moved to Production for " + applicationDTO_4.getApplicationName());
        verifyNoOneReceivedEmail("[OpenText™ Core Application Security] Release SDLC Status Updated - " +
                applicationDTO_4.getApplicationName());
        deleteSubscription(triggerName_User);
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("My Subscriptions - Emails-Other");
        LogInActions.tenantUserLogIn(appLead.getUserName(), FodConfig.TAM_PASSWORD,
                        tenantDTO.getTenantCode())
                .tenantTopNavbar.openNotifications().openMySubscriptions()
                .addSubscriptionWithEmail(triggerName_User, "For Testing other user",
                        null, null);
        tenantTopNavbar.openApplications().openYourReleases()
                .openDetailsForRelease(applicationDTO_5).openReleaseSettings()
                .setSDLCStatus(FodCustomTypes.Sdlc.QaTest)
                .pressSave();
        mySubscriptionsPage.tenantTopNavbar.openNotifications().openMySubscriptions();
        verifyEmailUser("[OpenText™ Core Application Security] Release SDLC Status Updated - " + applicationDTO_5.getApplicationName(),
                targetToList_OtherUser, targetToList_TAM, targetToList_SecLead);
        editSubscriptionToNo(triggerName_User);
        tenantTopNavbar.openApplications().openYourReleases()
                .openDetailsForRelease(applicationDTO_6).openReleaseSettings()
                .setSDLCStatus(FodCustomTypes.Sdlc.Production)
                .pressSave();
        verifyNotification("Release " + applicationDTO_6.getReleaseName() + " has been moved to Production for "
                + applicationDTO_6.getApplicationName());
        verifyNoOneReceivedEmail("[OpenText™ Core Application Security] Release SDLC Status Updated - " +
                applicationDTO_6.getApplicationName());
        deleteSubscription(triggerName_User);

    }

    private void editSubscriptionToYes(String triggerName) {

        var mySubCell = mySubscriptionsPage.getSubscriptionCell(triggerName);
        assertThat(mySubCell.getSendEmailsCheckbox().isDisabled())
                .as("Verify that A new column Send Emails with unactive" +
                        " checked and unchecked checkboxes appeared").isTrue();
        if (!mySubCell.getSendEmailsCheckbox().checked()) {
            mySubCell.pressEditButton().setSendEmailToggle(true).pressNext().pressSave();
        }
        mySubCell = mySubscriptionsPage.getSubscriptionCell(triggerName);
        assertThat(mySubCell.getSendEmailsCheckbox().checked())
                .as("Verify that send emails checkbox is now checked").isTrue();
        var detailsPopup = mySubCell.pressViewButton();
        assertThat(detailsPopup.getContentValue("Send Emails"))
                .as("Verify Send emails option set to Yes")
                .isEqualTo("Yes");
        detailsPopup.pressClose();
    }

    private void editSubscriptionToNo(String triggerName) {

        var mySubCell = mySubscriptionsPage.getSubscriptionCell(triggerName);
        mySubCell.pressEditButton().setSendEmailToggle(false).pressNext().pressSave();
        mySubCell = mySubscriptionsPage.getSubscriptionCell(triggerName);
        assertThat(mySubCell.getSendEmailsCheckbox().checked())
                .as("Verify that send emails checkbox is now un checked").isFalse();
        var detailsPopup = mySubCell.pressViewButton();
        assertThat(detailsPopup.getContentValue("Send Emails"))
                .as("Verify Send emails option set to No")
                .isEqualTo("No");
        detailsPopup.pressClose();
    }

    private void verifyDefaultSendEmailsYes(String triggerName) {

        SoftAssertions softAssertions = new SoftAssertions();
        var scanPausedCell = mySubscriptionsPage.getSubscriptionCell(triggerName);
        softAssertions.assertThat(scanPausedCell.getSendEmailsCheckbox().checked())
                .as("Verify that send emails checkbox is checked by default for " + triggerName).isTrue();
        var detailsPopup = scanPausedCell.pressViewButton();
        softAssertions.assertThat(detailsPopup.getContentValue("Send Emails"))
                .as("Verify Send emails value for " + triggerName)
                .isEqualTo("Yes");
        detailsPopup.pressClose();
        var editSubscPopup = scanPausedCell.pressEditButton();
        softAssertions.assertThat(editSubscPopup.sendEmailCheckbox.checked())
                .as("Verify Send emails option set to Yes by default for " + triggerName)
                .isTrue();
        softAssertions.assertThat(editSubscPopup.sendEmailCheckbox.isDisabled())
                .as("Verify Send emails option is disabled for " + triggerName)
                .isTrue();
        editSubscPopup.pressNext().pressSave();
        softAssertions.assertAll();

    }

    private void verifyViewPopup(String triggerName, String expectedValue) {

        SoftAssertions softAssertions = new SoftAssertions();
        var scanPausedCell = mySubscriptionsPage.getSubscriptionCell(triggerName);
        if (expectedValue.equals("Yes")) {
            softAssertions.assertThat(scanPausedCell.getSendEmailsCheckbox().checked())
                    .as("Verify that send emails checkbox is checked by default for " + triggerName).isTrue();
        } else {
            softAssertions.assertThat(scanPausedCell.getSendEmailsCheckbox().checked())
                    .as("Verify that send emails checkbox is checked by default for " + triggerName).isFalse();
        }
        var detailsPopup = scanPausedCell.pressViewButton();
        softAssertions.assertThat(detailsPopup.getContentValue("Send Emails"))
                .as("Verify Send emails value for " + triggerName)
                .isEqualTo(expectedValue);
        detailsPopup.pressClose();
        softAssertions.assertAll();
    }

    private void verifyEmail(String subject) {

        SoftAssertions softAssertions = new SoftAssertions();
        softAssertions.assertThat(mailUtil.findEmailByRecipientAndSubject(subject,
                        targetToList_TAM))
                .as("Verify TAM should not get the mail for the action")
                .isEmpty();
        softAssertions.assertThat(getEmail(subject, targetToList_SecLead))
                .as("Verify Security Lead got the mail for the action")
                .isNotEmpty();
        softAssertions.assertThat(getEmail(subject, targetToList_OtherUser))
                .as("Verify Other User got the mail for the action")
                .isNotEmpty();
        softAssertions.assertAll();
    }

    private void verifyEmailUser(String subject, String recipient, String... userDidnotGetMail) {

        SoftAssertions softAssertions = new SoftAssertions();
        for (String user : userDidnotGetMail) {
            softAssertions.assertThat(
                            mailUtil.findEmailByRecipientAndSubject(subject, user))
                    .as("Verify " + user + " should not get the email ")
                    .isEmpty();
        }
        softAssertions.assertThat(getEmail(subject, recipient))
                .as("Verify " + recipient + " got the mail for the action")
                .isNotEmpty();
        softAssertions.assertAll();
    }

    private void verifyNoOneReceivedEmail(String subject) {

        SoftAssertions softAssertions = new SoftAssertions();
        softAssertions.assertThat(
                        mailUtil.findEmailByRecipientAndSubject(subject, targetToList_TAM))
                .as("Verify TAM did not get the mail for the action")
                .isEmpty();
        softAssertions.assertThat(
                        mailUtil.findEmailByRecipientAndSubject(subject, targetToList_SecLead))
                .as("Verify Security Lead did not get the mail for the action")
                .isEmpty();
        softAssertions.assertThat(
                        mailUtil.findEmailByRecipientAndSubject(subject, targetToList_OtherUser))
                .as("Verify Other User did not get the mail for the action")
                .isEmpty();
        softAssertions.assertAll();
    }

    private void deleteSubscription(String triggerName) {

        mySubscriptionsPage.tenantTopNavbar.openNotifications()
                .openMySubscriptions().getSubscriptionCell(triggerName).pressDeleteButton();
    }

    private void verifyNotification(String expectedString) {
        Supplier<String> sup = () -> tenantTopNavbar.openNotifications().waitForNonEmptyTable()
                .getAllNotifications().get(0).getMessage();
        WaitUtil.waitFor(WaitUtil.Operator.Contains, expectedString,
                sup, Duration.ofMinutes(2), true);
    }

    private String getEmail(String targetSubject, String targetToList) {
        var mail = new MailUtil();
        Supplier<String> sup = () -> {
            try {
                return mail.findEmailByRecipientAndSubject(targetSubject,
                        targetToList);
            } catch (Exception | Error e) {
                log.error(e.getMessage());
                return "";
            }
        };
        WaitUtil.waitFor(WaitUtil.Operator.DoesNotEqual, "",
                sup, Duration.ofMinutes(8), false);
        return sup.get();
    }

}
