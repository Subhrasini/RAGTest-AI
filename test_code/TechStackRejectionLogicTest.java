package com.fortify.fod.ui.test.regression;

import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.MailUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.StaticScanDTO;
import com.fortify.fod.common.entities.TenantUserDTO;
import com.fortify.fod.ui.pages.admin.navigation.AdminTopNavbar;
import com.fortify.fod.ui.pages.admin.statics.scan_args.popups.EditScanArgumentsPopup;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.LogInActions;
import com.fortify.fod.ui.test.actions.SiteSettingsActions;
import com.fortify.fod.ui.test.actions.StaticScanActions;
import com.fortify.fod.ui.test.actions.TenantUserActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.testng.annotations.AfterClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import static com.codeborne.selenide.Selenide.page;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("kbadia@opentext.com")
@FodBacklogItem("487003")
@Slf4j
public class TechStackRejectionLogicTest extends FodBaseTest {

    StaticScanDTO staticScanDotNetTestDTO;

    TenantUserDTO nonAllStateTenantSL;

    String allStateTenantCode = "AUTO-TENANT-ALLSTATE";
    String msgWithoutExeOrDll = "notes = \"Reason: Missing EXE/DLL files, Notes: .NET payloads" +
            " must contain at least one exe or dll file\"";
    String msgMicroservices = "notes = \"Reason: Missing EXE/DLL files, Notes: .NET payloads must" +
            " contain at least one exe or dll file; Only 1 .sln file is allowed " +
            "in a Microservice .NET payload.\"";
    String msgWithoutPdbOrDll = "notes = \"Reason: Missing PDB/DLL files, Notes: .NET payloads must" +
            " contain dlls and pdb files\"";
    String notificationForCompleted = "AUTO-STATIC scan has completed for %s (%s).";
    String notificationForWithoutDll = "The AUTO-STATIC scan has been cancelled for %s.Additional user" +
            " action may be needed..NET payloads must contain at least one exe or dll file.";
    String notificationForMultipleSlns = "The AUTO-STATIC_micro scan has been cancelled for %s.Additional" +
            " user action may be needed..NET payloads must contain at least one " +
            "exe or dll file; Only 1 .sln file is allowed in a Microservice .NET payload..";
    String notificationForWithoutPdb = "The AUTO-STATIC scan has been cancelled for %s.Additional user action" +
            " may be needed..NET payloads must contain dlls and pdb files.";
    String microserviceName;
    String runTag;
    String targetSubject = "Application Cancelled";

    @MaxRetryCount(3)
    @Description("Admin user should be able to create tenant on admin site")
    @Test(groups = {"regression"}, priority = 1)
    public void prepareTestData() {
        nonAllStateTenantSL = TenantUserDTO.createInstanceWithUserRole(FodCustomTypes.TenantUserRole.SecurityLead);
        nonAllStateTenantSL.setTenant(allStateTenantCode);
        AllureReportUtil.info("EnablePayloadValidation=true flag in site settings");
        SiteSettingsActions.setValueInSettings("EnablePayloadValidation", "true", true);
        new AdminTopNavbar().openStatic().openScanArgument()
                .editArgumentsByLanguageName(".Net Core")
                .inheritTranslateArguments(false)
                .setTranslateArguments("TRANSLATE= -Xss64M -Xmx14G -64 -libdirs-only")
                .pressSaveBtn();
        page(EditScanArgumentsPopup.class).waitUntilDisappear();
        BrowserUtil.clearCookiesLogOff();
        LogInActions.tamUserLogin(FodConfig.TAM_USER_NAME, FodConfig.TAM_PASSWORD, allStateTenantCode);
        TenantUserActions.createTenantUser(nonAllStateTenantSL);
    }

    @MaxRetryCount(5)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verifying Non-AllState Tenant with .NET Core tech stack for different payloads")
    @Parameters({"isAllState", "Payload Type", "Path", "Status", "Expected Notification", "Expected EvenLog"})
    @Test(groups = {"regression"}, dataProvider = "nonAllStateAndAllState",
            dependsOnMethods = "prepareTestData", dataProviderClass = TechStackRejectionLogicTest.class)
    public void nonAllStateTenantTest(Boolean isAllState, String payloadType, String filePath,
                                      FodCustomTypes.SetupScanPageStatus status,
                                      String expectedNotification, String expectedEventLog) {
        setTestCaseName(String.format("Validate Framework type: %s", payloadType));
        microserviceName = "microservice" + runTag;

        var applicationDotNetTestDTO = ApplicationDTO.createDefaultInstance();
        if (payloadType.equals("WithMultipleSlns")) {
            applicationDotNetTestDTO.setMicroserviceToChoose(microserviceName);
            applicationDotNetTestDTO.setMicroservicesEnabled(true);
            applicationDotNetTestDTO.setApplicationMicroservices(new String[]{microserviceName});
        }
        if (isAllState) {
            LogInActions
                    .tenantUserLogIn(nonAllStateTenantSL.getUserName(),
                            nonAllStateTenantSL.getPassword(), allStateTenantCode)
                    .tenantTopNavbar.openApplications()
                    .createApplication(applicationDotNetTestDTO);
        } else {
            LogInActions
                    .tenantUserLogIn(defaultTenantDTO.getUserName(),
                            FodConfig.TAM_PASSWORD, defaultTenantDTO.getTenantCode())
                    .tenantTopNavbar.openApplications().createApplication(applicationDotNetTestDTO);
        }
        staticScanDotNetTestDTO = StaticScanDTO.createDefaultInstance();
        staticScanDotNetTestDTO.setTechnologyStack(FodCustomTypes.TechnologyStack.NetCore);
        staticScanDotNetTestDTO.setLanguageLevel("2.0");
        staticScanDotNetTestDTO.setFileToUpload(filePath);
        if (status.equals(FodCustomTypes.SetupScanPageStatus.Completed)) {
            StaticScanActions.createStaticScan(staticScanDotNetTestDTO, applicationDotNetTestDTO,
                    FodCustomTypes.SetupScanPageStatus.InProgress);
            BrowserUtil.clearCookiesLogOff();
            StaticScanActions.completeStaticScan(applicationDotNetTestDTO, true);
        } else {
            StaticScanActions.createStaticScan(staticScanDotNetTestDTO, applicationDotNetTestDTO,
                    status);
        }
        BrowserUtil.clearCookiesLogOff();
        if (isAllState) {
            LogInActions.tenantUserLogIn(nonAllStateTenantSL.getUserName(),
                    nonAllStateTenantSL.getPassword(), allStateTenantCode);
        } else {
            LogInActions
                    .tenantUserLogIn(defaultTenantDTO.getUserName(),
                            FodConfig.TAM_PASSWORD, defaultTenantDTO.getTenantCode());
        }
        var notificationMessage = new TenantTopNavbar()
                .openNotifications()
                .findWithSearchBox(applicationDotNetTestDTO.getReleaseName())
                .waitForNonEmptyTable().getAllNotifications().get(0).getMessage();
        if (status.equals(FodCustomTypes.SetupScanPageStatus.Canceled)) {
            assertThat(notificationMessage)
                    .as("Verify notification message")
                    .isEqualTo(String.format(expectedNotification, applicationDotNetTestDTO.getReleaseName()));
            var mailBody = new MailUtil()
                    .findEmailByRecipientAndSubject(targetSubject, defaultTenantDTO.getUserEmail());
            Document html = Jsoup.parse(mailBody);
            var link = html.selectXpath("//*[contains(text(), 'Application Cancelled')]").attr("text");
            assertThat(link)
                    .as("Verify notification message")
                    .isNotNull();
        } else {
            assertThat(notificationMessage)
                    .as("Verify notification message")
                    .isEqualTo(String.format(expectedNotification,
                            applicationDotNetTestDTO.getApplicationName(), applicationDotNetTestDTO.getReleaseName()));
        }
        assertThat(new TenantTopNavbar()
                .openAdministration().openEventLog().getAllLogs().stream()
                .filter(x -> x.getApplication().text().trim().contains(applicationDotNetTestDTO.getApplicationName()))
                .filter(x -> x.getNotes().contains(expectedEventLog)).findFirst())
                .as("Log should be in log event")
                .isPresent();
    }

    @AfterClass
    public void setDefaultSetting() {
        setupDriver("setDefaultSetting");
        SiteSettingsActions.setValueInSettings("EnablePayloadValidation", "false", true);
        new AdminTopNavbar().openStatic().openScanArgument()
                .editArgumentsByLanguageName(".Net Core")
                .inheritTranslateArguments(true).pressSaveBtn();
        page(EditScanArgumentsPopup.class).waitUntilDisappear();
        attachTestArtifacts();
    }

    @DataProvider(name = "nonAllStateAndAllState", parallel = true)
    public Object[][] nonAllStateAndAllState() {
        return new Object[][]{
                {false, "WithPdbAndDll", "payloads/fod/BotSharp-DotnetcoreWithPdbAndDll.zip",
                        FodCustomTypes.SetupScanPageStatus.Completed, notificationForCompleted, "scanId"},
                {false, "WithoutPdbWithDll", "payloads/fod/BotSharp-DotnetcoreWithoutPdbWithDll.zip",
                        FodCustomTypes.SetupScanPageStatus.Completed, notificationForCompleted, "scanId"},
                {false, "WithPdbWithoutDll", "payloads/fod/BotSharp-DotnetcoreWithPdbWithoutDll.zip",
                        FodCustomTypes.SetupScanPageStatus.Canceled, notificationForWithoutDll, msgWithoutExeOrDll},
                {false, "WithMultipleSlns", "payloads/fod/BotSharp-DotnetcoreWithmultipleslns.zip",
                        FodCustomTypes.SetupScanPageStatus.Canceled, notificationForMultipleSlns, msgMicroservices},
                {true, "WithPdbAndDll", "payloads/fod/BotSharp-DotnetcoreWithPdbAndDll.zip",
                        FodCustomTypes.SetupScanPageStatus.Completed, notificationForCompleted, "scanId"},
                {true, "WithoutPdbWithDll", "payloads/fod/BotSharp-DotnetcoreWithoutPdbWithDll.zip",
                        FodCustomTypes.SetupScanPageStatus.Canceled, notificationForWithoutPdb, msgWithoutPdbOrDll},
                {true, "WithPdbWithoutDll", "payloads/fod/BotSharp-DotnetcoreWithPdbWithoutDll.zip",
                        FodCustomTypes.SetupScanPageStatus.Canceled, notificationForWithoutDll, msgWithoutExeOrDll},
                {true, "WithMultipleSlns", "payloads/fod/BotSharp-DotnetcoreWithmultipleslns.zip",
                        FodCustomTypes.SetupScanPageStatus.Canceled, notificationForMultipleSlns, msgMicroservices}
        };
    }
}
