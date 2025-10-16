package com.fortify.fod.ui.test.regression;

import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.ui.pages.admin.navigation.AdminSideNavTabs;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import com.fortify.fod.ui.test.actions.StaticScanActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;

import java.util.ArrayList;
import java.util.List;

import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("kbadia@opentext.com")
@FodBacklogItem("595019")
@Slf4j
public class AppDefenderRemoveCheckTest extends FodBaseTest {

    ApplicationDTO applicationDTO;

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify that Remove App Defender screen from Administration -> Settings")
    @Test(groups = {"regression"})
    public void verifyAppDefenderTabInSettingsPage() {
        AllureReportUtil.info("Check App Defender screen removed from Administration -> Settings");
        assertThat(LogInActions
                .tamUserLogin(defaultTenantDTO)
                .tenantTopNavbar
                .openAdministration()
                .openSettings()
                .getTabByName("Application Defender")
                .exists())
                .as("Verify App Defender tab is not exists under settings page")
                .isFalse();
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify that App Defender mapping Removed from admin portal")
    @Test(groups = {"regression"})
    public void verifyAppDefenderMapping() {
        AllureReportUtil.info("App Defender mapping removed from admin portal");
        assertThat(LogInActions
                .adminLogIn()
                .adminTopNavbar
                .openConfiguration()
                .sideNavTabExists(AdminSideNavTabs.Configuration.AppDefenderMapping))
                .as("Verify that App Defender mapping removed from admin portal")
                .isFalse();
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify that AppDefenderIntegrationKey, AppDefenderProxy," +
            " AppDefenderRootUrl are removed from  site settings")
    @Test(groups = {"regression"})
    public void verifyAppDefenderKeys() {
        AllureReportUtil.info("Check AppDefenderIntegrationKey, AppDefenderProxy, " +
                "AppDefenderRootUrl are removed  from  site settings");
        List<String> keysToValidate = new ArrayList<>() {{
            add("AppDefenderIntegrationKey");
            add("AppDefenderProxy");
            add("AppDefenderRootUrl");
        }};
        assertThat(LogInActions
                .adminLogIn()
                .adminTopNavbar
                .openConfiguration()
                .openSiteSettings()
                .settingsTable
                .getAllDataRows()
                .texts()
                .toString())
                .as("Verify AppDefenderIntegrationKey, AppDefenderProxy, " +
                        "AppDefenderRootUrl are removed from  site settings")
                .doesNotContain(keysToValidate);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify Protect with app defender button is not exist")
    @Test(groups = {"regression"})
    public void verifyAppDefenderInIssuesPage() {
        AllureReportUtil.info("Prepare Application and static scan!");
        applicationDTO = ApplicationDTO.createDefaultInstance();
        ApplicationActions.createApplication(applicationDTO, defaultTenantDTO, true);
        StaticScanActions.importScanTenant(applicationDTO, "payloads/fod/static.java.fpr");
        new TenantTopNavbar().openApplications().openYourReleases()
                .openDetailsForRelease(applicationDTO)
                .openIssues().openIssueByIndex(0);
        assertThat($(byText("Protect With App Defender"))
                .exists())
                .as("Verify Protect with app defender button " +
                        "is not exists under issues page")
                .isFalse();
    }
}
