package com.fortify.fod.ui.test.regression;

import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.*;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import com.fortify.fod.ui.test.actions.StaticScanActions;
import com.fortify.fod.ui.test.actions.TenantActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("tmagill@opentext.com")
@Slf4j
public class RetiredReleaseIssueDetailsTest extends FodBaseTest {

    ApplicationDTO applicationDTO;
    TenantDTO tenantDTO;

    @MaxRetryCount(3)
    @Description("TAM should be able to import scan")
    @Test(groups = {"regression", "hf"})
    public void prepareTestData() {
        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setEntitlementDTO(EntitlementDTO.createDefaultInstance());
        TenantActions.createTenant(tenantDTO, true, false);
        applicationDTO = ApplicationDTO.createDefaultInstance();
        ApplicationActions.createApplication(applicationDTO, tenantDTO, true);
        StaticScanActions.importScanTenant(applicationDTO, "payloads/fod/chat_application_via_lan.fpr");
    }

    @MaxRetryCount(3)
    @FodBacklogItem("946001")
    @Severity(SeverityLevel.NORMAL)
    @Description("After release is retired, issued details (tabs) should still appear")
    @Test(groups = {"regression", "hf"}, dependsOnMethods = {"prepareTestData"})
    public void validateIssueDetails() {
        var tenantBar = LogInActions.tamUserLogin(tenantDTO).tenantTopNavbar;
        validateIssues(tenantBar);
        new TenantTopNavbar().openApplications()
                .openYourReleases()
                .openDetailsForRelease(applicationDTO.getApplicationName(), applicationDTO.getReleaseName())
                .openReleaseSettings()
                .setSDLCStatus(FodCustomTypes.Sdlc.Retired)
                .pressSave();
        validateIssues(tenantBar);
    }

    public void validateIssues(TenantTopNavbar tenantTopNavbar) {
        var releaseIssues = tenantTopNavbar.openApplications().openYourReleases()
                .openDetailsForRelease(applicationDTO.getApplicationName(), applicationDTO.getReleaseName())
                .openIssues()
                .getAllIssues();
        releaseIssues
                .forEach(issue -> assertThat(issue.openDetails().getAllTabsNames())
                        .as("Tabs should be visible in issue detail")
                        .containsAnyOf("Vulnerability", "Recommendations", "Code", "Diagram", "More Evidence", "History"));
    }
}
