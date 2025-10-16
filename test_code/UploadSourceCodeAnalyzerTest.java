package com.fortify.fod.ui.test.regression;

import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.ui.pages.admin.AdminLoginPage;
import com.fortify.fod.ui.pages.admin.administration.tenants.TenantsPage;
import com.fortify.fod.ui.pages.tenant.user_menu.ToolsPage;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.LogInActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import static com.codeborne.selenide.Selenide.page;
import static com.codeborne.selenide.Selenide.refresh;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("ysmal@opentext.com")
@Slf4j
public class UploadSourceCodeAnalyzerTest extends FodBaseTest {

    public String licenseFile = "payloads/fod/fortify.license";
    public String wrongFormatFile = "payloads/fod/rd.png";
    public String largeSizeFile = "payloads/fod/PerfStuffExtractor.zip";
    public String toolName = "Fortify Source Code Analyzer";
    public String largeFileMsg = "The file is too large. Please select a file less than 1MB.";
    public String invalidFormatMsg = "Invalid file format.";
    public String successMsg = "Saved Successfully.";
    public String fileNotFound = "File Not Found";
    public String deletedSuccess = "The license was deleted successfully.";

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Admin user should be able to enable license for created tenant")
    @Test(groups = {"regression"})
    public void uploadSourceCodeAnalyzerTest() {
        AdminLoginPage.navigate().login(FodConfig.ADMIN_USER_NAME, FodConfig.ADMIN_PASSWORD)
                .adminTopNavbar.openTenants();
        var tenantDetailsPage = page(TenantsPage.class).openTenantByName(defaultTenantDTO.getTenantName())
                .openTabLicenses();

        var modal = tenantDetailsPage.pressDeleteSCALicense();
        modal.clickButtonByText("Yes");
        assertThat(modal.getMessage()).as("Validate that alert message equal: " + fileNotFound)
                .isEqualTo(fileNotFound);

        modal.clickButtonByText("Close");
        tenantDetailsPage.setSCALicense(wrongFormatFile).pressSaveSCALicense();
        assertThat(tenantDetailsPage.getValidationMessage())
                .as("Validate that file not uploaded. Validation message equals: " +
                        invalidFormatMsg).isEqualTo(invalidFormatMsg);

        tenantDetailsPage.setSCALicense(largeSizeFile).pressSaveSCALicense();
        assertThat(tenantDetailsPage.getValidationMessage())
                .as("Validate that file not uploaded. Validation message equals: " +
                        largeFileMsg).isEqualTo(largeFileMsg);

        tenantDetailsPage.uploadSCALicense(licenseFile);
        assertThat(tenantDetailsPage.getValidationMessage())
                .as("Validate that file uploaded. Validation message equals: " +
                        successMsg).isEqualTo(successMsg);

        refresh();
        tenantDetailsPage.openTabLicenses();

        tenantDetailsPage.pressDeleteSCALicense().clickButtonByText("Yes");
        assertThat(tenantDetailsPage.getValidationMessage())
                .as("Validate that license deleted. Validation message equals: " +
                        deletedSuccess).isEqualTo(deletedSuccess);

        tenantDetailsPage.uploadSCALicense(licenseFile);
        assertThat(tenantDetailsPage.getValidationMessage())
                .as("Validate that file uploaded. Validation message equals: " +
                        successMsg).isEqualTo(successMsg);

        LogInActions.tamUserLogin(defaultTenantDTO)
                .tenantTopNavbar
                .openTools();

        var toolsPage = page(ToolsPage.class);
        var license = toolsPage.getTableByTitle("Licenses");
        assertThat(license).as("Validate that license section added").isNotNull();

        var rows = license.getAllDataRows();
        assertThat(rows.texts())
                .as(String.format("Validate that licenses (%s) accepted and available.", toolName))
                .contains(toolName);
    }
}
