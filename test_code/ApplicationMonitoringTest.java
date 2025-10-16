package com.fortify.fod.ui.test.regression;

import com.codeborne.selenide.Condition;
import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.UniqueRunTag;
import com.fortify.common.utils.WaitUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.TenantDTO;
import com.fortify.fod.ui.pages.admin.navigation.AdminTopNavbar;
import com.fortify.fod.ui.pages.tenant.applications.application.monitoring.popups.SaveAppMonitoringConfigPopup;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import com.fortify.fod.ui.test.actions.TenantActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;

import java.time.Duration;
import java.util.function.Supplier;

import static com.codeborne.selenide.Selenide.page;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("sbehera3@opentext.com")
@Slf4j
public class ApplicationMonitoringTest extends FodBaseTest {

    ApplicationDTO applicationDTO, applicationDTO2;
    String url = "127.0.0.1";
    String expectedErrMessage = "The following errors were encountered:\nInvalid URL.";
    String maxEnrolledApps = "1";
    String appAndReleaseName = StringUtils.repeat("B", 250);
    String exceededLengthName = StringUtils.repeat("C", 270);
    TenantDTO tenantDTO;

    private void init() {
        tenantDTO = TenantDTO.createDefaultInstance();
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Update maximum enrolled applications value for tenant in admin login")
    @Test(groups = {"hf", "regression"})
    public void updateMaxEnrolledApplicationsTest() {
        LogInActions.adminLogIn();
        var additionalServicesPage = new AdminTopNavbar().openTenants()
                .openTenantByName(defaultTenantDTO.getTenantCode()).openAdditionalServices();
        additionalServicesPage.updateMaxEnrolledApplications(maxEnrolledApps);
        additionalServicesPage.pressSave();
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @Description("Create an Application in Tenant")
    @Test(groups = {"hf", "regression"}, dependsOnMethods = {"updateMaxEnrolledApplicationsTest"})
    public void createApplicationTest() {
        applicationDTO = ApplicationDTO.createDefaultInstance();
        ApplicationActions.createApplication(applicationDTO, defaultTenantDTO, true);
    }

    @MaxRetryCount(2)
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("533002")
    @Description("Missed GUI response to not allowed URLs in Monitoring scan configuration")
    @Test(groups = {"hf", "regression"}, dependsOnMethods = {"createApplicationTest"})
    public void verifyErrorMessageInAppMonitoringConfigTest() {
        LogInActions.tamUserLogin(defaultTenantDTO).tenantTopNavbar.openApplications()
                .openDetailsFor(applicationDTO.getApplicationName())
                .openMonitoring()
                .openConfigurationTab()
                .enableAppMonitoring(true)
                .setURL(url)
                .pressSave();

        var saveAppMonitoringConfigPopup = page(SaveAppMonitoringConfigPopup.class);
        saveAppMonitoringConfigPopup.messageContainer.shouldHave(Condition.text(expectedErrMessage), Duration.ofSeconds(50));
        assertThat(saveAppMonitoringConfigPopup.getErrorMessage())
                .as("Verify error message in save application monitoring configuration Popup ")
                .isEqualTo(expectedErrMessage);
        saveAppMonitoringConfigPopup.pressClose();
    }

    @MaxRetryCount(3)
    @Owner("svpillai@opentext.com")
    @FodBacklogItem("420028")
    @Severity(SeverityLevel.NORMAL)
    @Description("Tenant user should add application name and release name, expected maximum length is 250 characters")
    @Test(groups = {"regression"})
    public void applicationAndReleaseNameCharacterLimitTest() {
        var runTag = UniqueRunTag.generate();
        var tempAppName = StringUtils.repeat("Flash-App--" + runTag, 10);
        AllureReportUtil.info("Verify that only 250 characters are allowed for Application and Release Name");
        LogInActions.tamUserLogin(defaultTenantDTO);
        var createApplicationPopup = new TenantTopNavbar().openApplications().pressAddNewApplicationBtn();
        createApplicationPopup.setApplicationName(appAndReleaseName);
        assertThat(createApplicationPopup.getApplicationName())
                .as("Check if App Name length with 250 characters is  allowed")
                .hasSize(appAndReleaseName.length());
        createApplicationPopup.setApplicationName(exceededLengthName);
        assertThat(createApplicationPopup.getApplicationName())
                .as("Check if App Name length is more than 250 characters, Value taken should be limited to 250")
                .hasSize(250);
        createApplicationPopup.chooseBusinessCriticality(FodCustomTypes.BusinessCriticality.High)
                .chooseApplicationType(FodCustomTypes.AppType.Web).pressNextButton();

        createApplicationPopup.setReleaseName(appAndReleaseName);
        assertThat(createApplicationPopup.getReleaseName())
                .as("Check if Release Name length with 250 characters is  allowed")
                .hasSize(appAndReleaseName.length());
        createApplicationPopup.setReleaseName(exceededLengthName);
        assertThat(createApplicationPopup.getReleaseName())
                .as("Check if Release Name length is more than 250 characters,Value taken should be limited to 250")
                .hasSize(250);
        createApplicationPopup.close();

        AllureReportUtil.info("Verify that application and release name fields would have only 250 characters and the " +
                "Application/Release would be created");
        applicationDTO2 = ApplicationDTO.createDefaultInstance();
        applicationDTO2.setApplicationName(tempAppName);
        applicationDTO2.setReleaseName(tempAppName);
        var releaseDetailsPage = ApplicationActions
                .createApplication(applicationDTO2, defaultTenantDTO, false);
        assertThat(releaseDetailsPage.tenantTopNavbar
                .openApplications().findWithSearchBox(runTag)
                .getAppByName(applicationDTO2.getApplicationName(), false).getName())
                .as("Application created successfully with name having 250 characters")
                .hasSize(250);
    }

    @Owner("vdubovyk@opentext.com")
    @MaxRetryCount(1)
    @Description("Monitoring Scan Test")
    @Test(groups = {"regression"}, enabled = false)
    public void monitoringScanTest() {
        init();
        TenantActions.createTenant(tenantDTO, true)
                .openAdditionalServices()
                .updateMaxEnrolledApplications("3")
                .updateOverrideScanFrequency("1")
                .pressSave();

        var applicationDTO = ApplicationActions.createApplication(tenantDTO, true);

        var saveAppMonitoringConfigPopup = new TenantTopNavbar()
                .openApplications()
                .openYourApplications()
                .openDetailsFor(applicationDTO.getApplicationName())
                .openMonitoring()
                .openConfigurationTab()
                .setURL("http://zero.webappsecurity.com")
                .enableAppMonitoring(true)
                .pressSave();

        Supplier<Boolean> sup = () -> saveAppMonitoringConfigPopup.getErrorMessage()
                .equals("Application Monitoring Configuration saved.");
        assertThat(WaitUtil.waitForTrue(sup, Duration.ofSeconds(10), false))
                .as("Check the 'Application Monitoring Configuration Popup' message")
                .isEqualTo(true);

        assertThat(saveAppMonitoringConfigPopup.pressClose()
                .getAlertMessage())
                .as("Check the Application Monitoring Configuration Popup message")
                .isEqualTo("Application Monitoring Configuration saved.");
    }
}