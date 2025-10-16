package com.fortify.fod.ui.test.regression;

import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.WaitUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.elements.Switcher;
import com.fortify.fod.common.elements.Table;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.StaticScanDTO;
import com.fortify.fod.common.entities.TenantDTO;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
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

import static com.codeborne.selenide.Selenide.refresh;
import static com.codeborne.selenide.Selenide.sleep;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("svpillai@opentext.com")
@FodBacklogItem("510008")
@Slf4j
public class StaticScanPayloadValidationTest extends FodBaseTest {
    TenantDTO tenantDTO;
    StaticScanDTO staticScanDTO;
    String staticScanFileExt = "StaticSCAApprovedFileExtensions";
    String staticScanExecutableFileExt = "StaticApprovedExecutableFileExtensions";
    String staticLabelName = "Reject Payloads with No Executable Files";
    String validPayload = "payloads/fod/static.java.zip";
    String invalidPayload = "payloads/fod/NoJavaFiles.zip";
    String xmlOnlyPayload = "payloads/fod/Onlyxml.zip";
    String docxOnlyPayload = "payloads/fod/Onlydocx.zip";
    ApplicationDTO webApp1, webApp2, webApp3, webApp4, webApp5, webApp6, webApp7;
    String staticFileExt = ".abap,.appxmanifest,.as,.asax,.ascx,.ashx,.asmx,.asp,.aspx,.baml,.bas,.bsp,.BSP,.cbl,.cfc," +
            ".cfm,.cfml,.cls,.cob,.conf,.config,.cpx,.cs,.cscfg,.csdef,.cshtml,.ctl,.ctp,.dll,.dockerfile,.erb,.exe,.frm," +
            ".go,.htm,.html,.inc,.ini,.java,.js,.jsff,.json,.jsp,.jspf,.jspx,.jsx,.kt,.kts,.master,.mxml,.page,.php," +
            ".phtml,.pkb,.pkh,.pks,.plist,.properties,.py,.razor,.rb,.scala,.settings,.sql,.swift,.tag,.tagx,.tld," +
            ".trigger,.ts,.tsx,.vb,.vbs,.vbscript,.vbhtml,.wadcfg,.wadcfgx,.winmd,.wsdd,.wsdl,.xaml,.xcfg,.xhtml,.xmi," +
            ".xml,.xsd,.yaml,.yml,.class,.jar,.mbs,.dart,.bicep,.sol";
    String staticExecutableExt = ".abap,.as,.asax,.ashx,.asmx,.asp,.aspx,.bas,.bsp,.cbl,.cfc,.cfm,.cfml,.cls,.cob,.cs," +
            ".cshtml,.ctl,.dll,.erb,.exe,.frm,.go,.htm,.html,.java,.js,.jsp,.jspf,.jspx,.jsx,.kt,.kts,.page,.php,.phtml," +
            ".py,.rb,.scala,.swift,.trigger,.ts,.tsx,.vb,.vbhtml,.vbs,.vbscript,.dart,.bicep,.sol";
    String expectedFailedNotification = "The AUTO-STATIC scan has been cancelled for Release.Additional user action may " +
            "be needed.Payload contains no scannable file extensions. Contact support for further clarification";
    String expectedNotification = "The AUTO-STATIC scan has been cancelled for Release.Additional user action may be" +
            " needed.Payload contains no file extensions that could contain an executable line of code. Contact" +
            " support for further clarification..";

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("User should be able to create tenant")
    @Test(groups = {"regression"}, priority = 1)
    public void prepareTestData() {
        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setOptionsToEnable(new String[]{"Allow scanning with no entitlements"});
        TenantActions.createTenant(tenantDTO, true);
        staticScanDTO = StaticScanDTO.createDefaultInstance();
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Admin user should check for 2 new entries for Static Scan and validate the default values of file " +
            "extensions")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void validateStaticScanFileExtensionsInAdmin() {
        AllureReportUtil.info("Validate Options and default values in Site Settings Page");
        var settingsPage = LogInActions.adminLogIn().adminTopNavbar.openConfiguration();
        settingsPage.getSettingValueByName(staticScanFileExt);
        assertThat(settingsPage.getSettingByName(staticScanFileExt))
                .as("Validate StaticSCAApprovedFileExtensions entry is present in Site Settings Page ")
                .isNotNull();
        assertThat(settingsPage.getSettingByName(staticScanExecutableFileExt))
                .as("Validate StaticApprovedExecutableFileExtensions entry is present in Site Settings Page ")
                .isNotNull();
        var popupFileExt = settingsPage.openSettingByName(staticScanFileExt);
        assertThat(popupFileExt.getSettingValue().split(","))
                .as("Validate the default file extensions of StaticSCAApprovedFileExtensions")
                .containsExactlyInAnyOrder(staticFileExt.split(","));
        popupFileExt.save();
        var popupExecutableExt = settingsPage.openSettingByName(staticScanExecutableFileExt);
        assertThat(popupExecutableExt.getSettingValue().split(","))
                .as("Validate default executable file extensions of StaticApprovedExecutableFileExtensions")
                .containsExactlyInAnyOrder(staticExecutableExt.split(","));
        popupExecutableExt.save();
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate Static Scan should be cancelled for invalid payload when Reject Payload option is OFF ")
    @Test(groups = {"regression"},
            dependsOnMethods = {"prepareTestData", "validateStaticScanFileExtensionsInAdmin"})
    public void staticScanFileExtensionWithInvalidPayload() {
        AllureReportUtil.info("Static Scan should be cancelled for invalid payload, Reject Payload option is OFF");
        var tenantDetailsPage = LogInActions.adminLogIn().adminTopNavbar.openTenants()
                .openTenantByName(tenantDTO.getTenantName()).openTabOptions();
        var labelList = tenantDetailsPage.getLabelNamesFromOptionsTab();
        assertThat(labelList).contains(staticLabelName);
        new Switcher("Reject Payloads with No Executable Files").setValue(false);
        BrowserUtil.clearCookiesLogOff();

        webApp1 = ApplicationActions.createApplication(tenantDTO, true);
        staticScanDTO.setFileToUpload(invalidPayload);
        StaticScanActions.createStaticScan(staticScanDTO, webApp1, FodCustomTypes.SetupScanPageStatus.InProgress,
                FodCustomTypes.SetupScanPageStatus.Canceled);
        BrowserUtil.clearCookiesLogOff();

        validateFailedScan(webApp1);
        BrowserUtil.clearCookiesLogOff();

        LogInActions.tamUserLogin(tenantDTO);
        verifyNotifications(expectedFailedNotification, webApp1);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate Static Scan should be success for Valid payload when Reject Payload option is OFF ")
    @Test(groups = {"regression"},
            dependsOnMethods = {"staticScanFileExtensionWithInvalidPayload"})
    public void staticScanFileExtensionWithValidPayload() {
        AllureReportUtil.info("Static Scan should be Success for valid payload, Reject Payload option is OFF");
        LogInActions.adminLogIn().adminTopNavbar.openTenants().openTenantByName(tenantDTO.getTenantName())
                .openTabOptions();
        new Switcher("Reject Payloads with No Executable Files").setValue(false);
        BrowserUtil.clearCookiesLogOff();

        webApp2 = ApplicationActions.createApplication(tenantDTO, true);
        staticScanDTO.setFileToUpload(validPayload);
        staticScanDTO.setAuditPreference(FodCustomTypes.AuditPreference.Manual);
        StaticScanActions.createStaticScan(staticScanDTO, webApp2, FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();
        validateSuccessScan(webApp2);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate Static Scan should be cancelled for invalid payload when Reject Payload option is ON ")
    @Test(groups = {"regression"},
            dependsOnMethods = {"staticScanFileExtensionWithValidPayload"})
    public void validateStaticScanWithRejectPayloadOptionOn() {
        AllureReportUtil.info("Static Scan should be cancelled for invalid payload, Reject Payload option is ON");
        LogInActions.adminLogIn().adminTopNavbar.openTenants().openTenantByName(tenantDTO.getTenantName()).openTabOptions();
        new Switcher("Reject Payloads with No Executable Files").setValue(true);
        BrowserUtil.clearCookiesLogOff();

        webApp3 = ApplicationActions.createApplication(tenantDTO, true);
        staticScanDTO.setFileToUpload(invalidPayload);
        StaticScanActions.createStaticScan(staticScanDTO, webApp3, FodCustomTypes.SetupScanPageStatus.InProgress,
                FodCustomTypes.SetupScanPageStatus.Canceled);
        BrowserUtil.clearCookiesLogOff();

        validateFailedScan(webApp3);
        BrowserUtil.clearCookiesLogOff();

        LogInActions.tamUserLogin(tenantDTO);
        verifyNotifications(expectedFailedNotification, webApp3);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("597008")
    @Description("Verify two techstacks are excluded from 'StaticApprovedExecutableFileExtensions' Validation")
    @Test(groups = {"regression"},
            dependsOnMethods = {"validateStaticScanWithRejectPayloadOptionOn"})
    public void excludeTechStackTestWithXmlAndDocxPayload() {
        var staticScanDTO = StaticScanDTO.createDefaultInstance();
        LogInActions.adminLogIn().adminTopNavbar.openTenants().openTenantByName(tenantDTO.getTenantName())
                .openTabOptions();
        new Switcher("Reject Payloads with No Executable Files").setValue(true);
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Configure scan for JS TechStack.Upload a dummy payload with .xml files only.Scan not " +
                "rejected and successfully completed");
        webApp5 = ApplicationActions.createApplication(tenantDTO, true);
        staticScanDTO.setTechnologyStack(FodCustomTypes.TechnologyStack.JS);
        staticScanDTO.setFileToUpload(xmlOnlyPayload);
        StaticScanActions.createStaticScan(staticScanDTO, webApp5, FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();
        StaticScanActions.completeStaticScan(webApp5, true);
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Upload a dummy payload with .docx files only.Scan rejected and canceled due to missing " +
                "scannable files");
        LogInActions.tamUserLogin(tenantDTO);
        staticScanDTO.setFileToUpload(docxOnlyPayload);
        StaticScanActions.createStaticScan(staticScanDTO, webApp5, FodCustomTypes.SetupScanPageStatus.InProgress,
                FodCustomTypes.SetupScanPageStatus.Canceled);
        verifyNotifications(expectedFailedNotification, webApp5);
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Configure scan for Infrastructure TechStack.Upload a dummy payload with .xml files only." +
                "Scan not rejected and successfully completed");
        webApp6 = ApplicationActions.createApplication(tenantDTO, true);
        staticScanDTO.setTechnologyStack(FodCustomTypes.TechnologyStack.Infrastructure);
        staticScanDTO.setFileToUpload(xmlOnlyPayload);
        StaticScanActions.createStaticScan(staticScanDTO, webApp6, FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();
        StaticScanActions.completeStaticScan(webApp6, true);
        BrowserUtil.clearCookiesLogOff();
        AllureReportUtil.info("Upload a dummy payload with .docx files only.Scan rejected and canceled due to missing " +
                "scannable files");
        LogInActions.tamUserLogin(tenantDTO);
        staticScanDTO.setFileToUpload(docxOnlyPayload);
        StaticScanActions.createStaticScan(staticScanDTO, webApp6, FodCustomTypes.SetupScanPageStatus.InProgress,
                FodCustomTypes.SetupScanPageStatus.Canceled);
        verifyNotifications(expectedFailedNotification, webApp6);
        BrowserUtil.clearCookiesLogOff();

        AllureReportUtil.info("Configure scan with any techstack. Scan rejected and canceled due to missing executable " +
                "files");
        webApp7 = ApplicationActions.createApplication(tenantDTO, true);
        staticScanDTO.setTechnologyStack(FodCustomTypes.TechnologyStack.JAVA);
        staticScanDTO.setLanguageLevel("1.8");
        staticScanDTO.setFileToUpload(xmlOnlyPayload);
        StaticScanActions.createStaticScan(staticScanDTO, webApp7, FodCustomTypes.SetupScanPageStatus.InProgress,
                FodCustomTypes.SetupScanPageStatus.Canceled);
        verifyNotifications(expectedNotification, webApp7);
        BrowserUtil.clearCookiesLogOff();
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Update any file extension of Static Scan option,scan should be cancelled")
    @Test(groups = {"regression"},
            dependsOnMethods = {"excludeTechStackTestWithXmlAndDocxPayload"})
    public void validateStaticScanWithValidPayloadAndInvalidFileExt() {
        AllureReportUtil.info("Static Scan should be cancelled for Valid payload and file extension not updates in setting");
        var editSetting = staticExecutableExt.replace(".java,", "");
        var settingsPage = LogInActions.adminLogIn().adminTopNavbar.openConfiguration();
        var editPopup = settingsPage.openSettingByName(staticScanExecutableFileExt);
        editPopup.setValue(editSetting).save();
        BrowserUtil.clearCookiesLogOff();

        webApp4 = ApplicationActions.createApplication(tenantDTO, true);
        staticScanDTO.setFileToUpload(validPayload);
        StaticScanActions.createStaticScan(staticScanDTO, webApp4, FodCustomTypes.SetupScanPageStatus.InProgress,
                FodCustomTypes.SetupScanPageStatus.Canceled);
        BrowserUtil.clearCookiesLogOff();

        validateFailedScan(webApp4);
        BrowserUtil.clearCookiesLogOff();

        LogInActions.tamUserLogin(tenantDTO);
        verifyNotifications(expectedNotification, webApp4);
        BrowserUtil.clearCookiesLogOff();
    }

    public void validateFailedScan(ApplicationDTO applicationDTO) {
        var staticScanJobsPage = LogInActions.adminLogIn().adminTopNavbar.openStatic();
        staticScanJobsPage.appliedFilters.clearAll();
        staticScanJobsPage.findWithSearchBox(applicationDTO.getApplicationName());
        var scanlist = staticScanJobsPage.getAllScans();
        var scanFailStatus = scanlist.get(0).getLastJob().waitForJobStatus(FodCustomTypes.JobStatus.Failed).getStatus();
        assertThat(scanFailStatus)
                .as("Scan status should be Failed")
                .isEqualTo(FodCustomTypes.JobStatus.Failed.getTypeValue());
    }

    public void validateSuccessScan(ApplicationDTO applicationDTO) {
        var staticScanJobsPage = LogInActions.adminLogIn().adminTopNavbar.openStatic();
        staticScanJobsPage.appliedFilters.clearAll();
        staticScanJobsPage.findWithSearchBox(applicationDTO.getApplicationName());
        var scanList = staticScanJobsPage.getAllScans();
        var scanSuccessStatus = scanList.get(0).getLastJob()
                .waitForJobStatus(FodCustomTypes.JobStatus.Success).getStatus();
        assertThat(scanSuccessStatus)
                .as("Scan status should be Success")
                .isEqualTo(FodCustomTypes.JobStatus.Success.getTypeValue());
    }

    public void verifyNotifications(String expectedFailedNotification, ApplicationDTO applicationDTO) {
        expectedFailedNotification = expectedFailedNotification.replace("Release", applicationDTO.getReleaseName());
        var notificationPage = new TenantTopNavbar().openNotifications();
        WaitUtil.waitForTrue(() -> !Table.isEmpty(), Duration.ofMinutes(3), true);
        var notificationsList = notificationPage.getAllNotifications();
        String finalExpectedFailedNotification = expectedFailedNotification;

        if (notificationsList.stream().noneMatch(x -> x.getMessage().toLowerCase()
                .contains(finalExpectedFailedNotification.toLowerCase()))) {
            sleep(Duration.ofMinutes(2).toMillis());
            refresh();
            notificationsList = notificationPage.getAllNotifications();
        }

        assertThat(notificationsList.stream().anyMatch(x -> x.getMessage().toLowerCase()
                .contains(finalExpectedFailedNotification.toLowerCase())))
                .as("Notification should be present in Notifications Page: " + expectedFailedNotification)
                .isTrue();
    }

    @AfterClass
    public void setStaticScanExecutableFileExtToDefaultValue() {
        setupDriver("setStaticScanExecutableFileExtToDefaultValue");
        SiteSettingsActions.setValueInSettings(staticScanExecutableFileExt, staticExecutableExt, true);
        attachTestArtifacts();
    }
}
