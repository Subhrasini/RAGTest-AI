package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.MailUtil;
import com.fortify.common.utils.WaitUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.*;
import com.fortify.fod.ui.pages.admin.navigation.AdminTopNavbar;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.time.Duration;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("kbadia@opentext.com")
@FodBacklogItem("294005")
@FodBacklogItem("298002")
@Slf4j
public class PausedOrCancelledReasonTest extends FodBaseTest {

    String staticEmailSubject = "[OpenText™ Core Application Security] Scan for %s has been paused for " +
            "Significant Payload Difference from Previous Release .";
    String mobileOrDynamicSubject = "[OpenText™ Core Application Security] Scan for %s has been paused for Credentials locked or invalid .";
    String cancelEmailSubject = "[OpenText™ Core Application Security] Scan for %s has been Other.";
    String mobileOrDynamicNote = "notes = \"The scan of your application has been paused because the credentials provided are not working. " +
            "Please verify the credentials and notify the Help Center with the updated information so we may complete your scan." +
            "To chat with an FoD representative or log a support ticket visit the FoD Help Center link on the FoD portal. " +
            "For immediate assistance use the Help Center Voice Channel (800)893-8141. Without your response your scan may be cancelled\"";
    String staticNote = "notes = \"The scan of your application has been paused because there is a significant " +
            "difference between the current submission and payload from the previous release. " +
            "Please verify the submitted payload is correct by contacting the Help Center in the FoD portal. " +
            "To chat with an FoD representative or log a support ticket visit the FoD Help Center link on the FoD portal. " +
            "For immediate assistance use the Help Center Voice Channel (800) 893-8141. Without your response your scan may be cancelled.\"";
    String staticMessage = "The %s scan has been paused for %s . Additional user action may be needed . " +
            "The scan of your application has been paused because there is a significant difference between the " +
            "current submission and payload from the previous release." +
            " Please verify the submitted payload is correct by contacting the Help Center in the FoD portal. " +
            "To chat with an FoD representative or log a support ticket visit the FoD Help Center link on the FoD portal. " +
            "For immediate assistance use the Help Center Voice Channel (800) 893-8141. Without your response your scan may be cancelled.";
    String mobileOrDynamicMessage = "The %s scan has been paused for %s . Additional user action may be needed . " +
            "The scan of your application has been paused because the credentials provided are not working. " +
            "Please verify the credentials and notify the Help Center with the updated information so we may complete your scan." +
            "To chat with an FoD representative or log a support ticket visit the FoD Help Center link on the FoD portal. " +
            "For immediate assistance use the Help Center Voice Channel (800)893-8141. Without your response your scan may be cancelled";
    String cancelMessage = "The %s scan has been cancelled for %s.Additional user action may be needed..";
    TenantDTO tenantEmailDTO;
    String targetToList;

    private void initEmail() {
        tenantEmailDTO = TenantDTO.createDefaultInstance();
        tenantEmailDTO.setOptionsToEnable(new String[]{"Allow scanning with no entitlements"});
        this.targetToList = tenantEmailDTO.getUserEmail();
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Test data preparation")
    @Test(groups = {"regression"}, priority = 1)
    public void prepareTestData() {
        initEmail();
        TenantActions.createTenant(tenantEmailDTO, true, false);
        TenantUserActions.activateSecLead(tenantEmailDTO, true);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify email notifications for scan pause and cancel scenario")
    @Parameters({"Scan", "ScanStatus", "Assessment", "ExpectedMessage", "TargetSubject", "Note"})
    @Test(groups = {"regression"},
            dataProvider = "verifyPausedOrCancelledReasonTest",
            dependsOnMethods = {"prepareTestData"})
    public void validatePausedOrCancelledReasonNotificationTest(String scan, String scanStatus,
                                                                String assessment, String expectedMessage, String targetSubject, String note) {
        setTestCaseName(String.format("Validate email Notification : %s with: %s", scan, scanStatus));
        ApplicationDTO applicationDTO;

        if (scan.equals("mobile")) {
            applicationDTO = ApplicationDTO.createDefaultMobileInstance();
        } else applicationDTO = ApplicationDTO.createDefaultInstance();

        ApplicationActions.createApplication(applicationDTO, tenantEmailDTO, true);
        switch (scan) {
            case "static":
                var staticScanDTO = StaticScanDTO.createDefaultInstance();
                staticScanDTO.setAuditPreference(FodCustomTypes.AuditPreference.Manual);
                StaticScanActions.createStaticScan(staticScanDTO, applicationDTO,
                        FodCustomTypes.SetupScanPageStatus.InProgress);
                break;
            case "dynamic":
                var dynamicScanDTO = DynamicScanDTO.createDefaultInstance();
                DynamicScanActions.createDynamicScan(dynamicScanDTO, applicationDTO,
                        FodCustomTypes.SetupScanPageStatus.InProgress);
                break;
            case "mobile":
                var mobileScanDTO = MobileScanDTO.createDefaultScanInstance();
                MobileScanActions.createMobileScan(mobileScanDTO, applicationDTO,
                        FodCustomTypes.SetupScanPageStatus.InProgress);
                break;
            default:
                throw new IllegalStateException("Unexpected scan: " + scan);
        }
        BrowserUtil.clearCookiesLogOff();
        LogInActions.adminLogIn();
        switch (scan) {
            case "static":
                var overviewPage = new AdminTopNavbar().openStatic()
                        .openDetailsByReleaseName(applicationDTO.getReleaseName());
                if (scanStatus.equals("paused")) {
                    overviewPage.pressPauseButton().pressOkButton();
                } else {
                    overviewPage.pressCancelBtn().setReason("Other").setRefund(false).pressOkBtn();
                }
                break;
            case "dynamic":
                var dynamicScanOverviewPage = new AdminTopNavbar().openDynamic()
                        .openDetailsFor(applicationDTO.getReleaseName());
                if (scanStatus.equals("paused")) {
                    dynamicScanOverviewPage.pressPauseButton().pressOkButton();
                } else {
                    dynamicScanOverviewPage.pressCancelBtn().setReason("Other").setRefund(false).pressOkBtn();
                }
                break;
            case "mobile":
                var mobileScanOverviewPage = new AdminTopNavbar().openMobile()
                        .openDetailsFor(applicationDTO.getApplicationName());
                if (scanStatus.equals("paused")) {
                    mobileScanOverviewPage.pressPauseButton().pressOkButton();
                } else {
                    mobileScanOverviewPage.pressCancelBtn().setReason("Other").setRefund(false).pressOkBtn();
                }
                break;
            default:
                throw new IllegalStateException("Unexpected scan: " + scan);
        }
        BrowserUtil.clearCookiesLogOff();
        assertThat(LogInActions.tamUserLogin(tenantEmailDTO)
                .tenantTopNavbar.openNotifications().findWithSearchBox(applicationDTO.getReleaseName())
                .waitForNonEmptyTable().getAllNotifications().get(0).getMessage())
                .as("Verify notification message")
                .isEqualTo(String.format(expectedMessage, assessment, applicationDTO.getReleaseName()));
        Supplier<String> sup = () -> {
            try {
                return new MailUtil().getAllEmailSubjects(targetToList).toString();
            } catch (Exception | Error e) {
                log.error(e.getMessage());
                return "";
            }
        };
        WaitUtil.waitFor(WaitUtil.Operator.Contains,
                String.format(targetSubject, applicationDTO.getApplicationName()),
                sup, Duration.ofMinutes(2), false);
        assertThat(new MailUtil().getAllEmailSubjects(targetToList))
                .as("Validate email subject after scan pause or cancel")
                .contains(String.format(targetSubject, applicationDTO.getApplicationName()));
        var mailBody = new MailUtil()
                .findEmailByRecipientAndSubject(String.format(targetSubject,
                        applicationDTO.getApplicationName()), targetToList);
        Document html = Jsoup.parse(mailBody);
        assertThat(html.selectXpath(String.format("//*[contains(text(), '%s')]",
                applicationDTO.getReleaseName())).text())
                .as("Verify email body after scan pause or cancel")
                .contains(String.format(expectedMessage, assessment, applicationDTO.getReleaseName()));
        assertThat(new TenantTopNavbar()
                .openAdministration().openEventLog().getAllLogs().stream()
                .filter(x -> x.getApplication().text().trim().contains(applicationDTO.getApplicationName()))
                .filter(x -> x.getNotes().contains(note)).findFirst())
                .as("Verify notes under log event")
                .isPresent();
    }

    @DataProvider(name = "verifyPausedOrCancelledReasonTest", parallel = true)
    public Object[][] verifyPausedOrCancelledReasonTest() {
        return new Object[][]{
                {"static", "paused",
                        "AUTO-STATIC", staticMessage, staticEmailSubject, staticNote},
                {"dynamic", "paused",
                        "AUTO-DYNAMIC", mobileOrDynamicMessage, mobileOrDynamicSubject, mobileOrDynamicNote},
                {"mobile", "paused",
                        "Mobile Express", mobileOrDynamicMessage, mobileOrDynamicSubject, mobileOrDynamicNote},
                {"static", "cancelled",
                        "AUTO-STATIC", cancelMessage, cancelEmailSubject, "notes = \"Reason: Other\""},
                {"dynamic", "cancelled",
                        "AUTO-DYNAMIC", cancelMessage, cancelEmailSubject, "notes = \"Reason: Other\""},
                {"mobile", "cancelled",
                        "Mobile Express", cancelMessage, cancelEmailSubject, "notes = \"Reason: Other\""}
        };
    }
}
