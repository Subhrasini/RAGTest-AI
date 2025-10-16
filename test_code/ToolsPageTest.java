package com.fortify.fod.ui.test.regression;

import com.codeborne.selenide.Condition;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.ui.pages.admin.AdminLoginPage;
import com.fortify.fod.ui.pages.admin.administration.tenants.TenantsPage;
import com.fortify.fod.ui.pages.admin.administration.tenants.tenant.TenantDetailsPage;
import com.fortify.fod.ui.pages.tenant.user_menu.ToolsPage;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.LogInActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.Duration;

import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.page;
import static com.codeborne.selenide.files.FileFilters.withExtension;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("ysmal@opentext.com")
@Slf4j
public class ToolsPageTest extends FodBaseTest {

    public String vStudio = "Fortify Security Assistant for Visual Studio";
    public String eclipse = "Fortify Security Assistant for Eclipse";
    public String licenseFile = "payloads/fod/fortify.license";
    private final int fodUploadLength = 3479926;
    private final String fodUploadUrl = "https://github.com/fod-dev/fod-uploader-java/releases/latest/download/otcoreapplicationsecurityuploader.jar";
    String macroRecorderUrl = "/Tools/MacroRecorder_v22.1.0.117.msi";
    String toolName = "Workflow Macro Recorder";
    final int msiFileSize = 295243776;

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Admin user should be able to enable license for created tenant")
    @Test(groups = {"regression"})
    public void securityAssistantTest() {
        AdminLoginPage.navigate()
                .login(FodConfig.ADMIN_USER_NAME, FodConfig.ADMIN_PASSWORD)
                .adminTopNavbar
                .openTenants();

        page(TenantsPage.class).openTenantByName(defaultTenantDTO.getTenantName());
        var tenantDetails = page(TenantDetailsPage.class);
        tenantDetails
                .openTabLicenses()
                .uploadSecurityAssistantLicense(licenseFile);

        BrowserUtil.clearCookiesLogOff();

        LogInActions.tamUserLogin(defaultTenantDTO)
                .tenantTopNavbar
                .openTools();

        var toolsPage = page(ToolsPage.class);
        var license = toolsPage.getTableByTitle("Licenses");
        assertThat(license).as("Validate that license section added").isNotNull();

        var rows = license.getAllDataRows();
        assertThat(rows.texts())
                .as(String.format("Validate that licenses (%s, %s) accepted and available.", vStudio, eclipse))
                .contains(vStudio, eclipse);
    }

    @Owner("tmagill@opentext.com")
    @MaxRetryCount(3)
    @Severity(SeverityLevel.CRITICAL)
    @FodBacklogItem("554005")
    @Description("AUTO-TAM should see expected link for fodupload.jar")
    @Test(groups = {"hf", "regression"})
    public void verifyFodUploadLinkTest() throws FileNotFoundException {

        var toolsPage = LogInActions.tamUserLogin(defaultTenantDTO)
                .tenantTopNavbar
                .openTools();

        var findTool = toolsPage.getTableByTitle("CI/CD PLUGINS").getRow(2);
        var verifyLink = findTool.hover().innerHtml();
        assertThat(verifyLink).containsSequence(fodUploadUrl);
//        File f = $x("//*[contains(text(), 'Fortify on Demand Uploader')]")
//                .download(Duration.ofMinutes(3).toMillis(), withExtension("jar"));
//        assertThat(f).hasSize(fodUploadLength);
    }

    @SneakyThrows
    @Owner("kbadia@opentext.com")
    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("645013")
    @FodBacklogItem("724013")
    @Description("Verify Workflow Macro Recorder download link to " +
            "be listed on Tools page under Utilities section.")
    @Test(groups = {"regression"}, enabled = false)
    public void verifyWorkflowMacroRecorderLinkTest() {
        var toolsPage = LogInActions.tamUserLogin(defaultTenantDTO)
                .tenantTopNavbar
                .openTools();
        var allDataRows = toolsPage.getTableByTitle("UTILITIES")
                .getAllDataRows();
        assertThat(allDataRows.texts())
                .as("Verify Workflow Macro Recorder tool " +
                        "is available under tools page")
                .contains(toolName);
        var verifyLink = allDataRows.findBy(Condition.text(toolName))
                .hover().innerHtml();
        assertThat(verifyLink)
                .as("Verify Workflow Macro Recorder is a" +
                        "link or not")
                .containsSequence(macroRecorderUrl);
        File file = $$("a").findBy(Condition.text(toolName))
                .download(Duration.ofMinutes(10).toMillis(), withExtension("msi"));
        assertThat(file)
                .as("Verify .msi file is downloading from " +
                        "Workflow Macro Recorder download link")
                .hasSize(msiFileSize);
    }
}
