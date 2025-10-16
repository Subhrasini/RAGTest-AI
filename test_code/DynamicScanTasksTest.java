package com.fortify.fod.ui.test.regression;

import com.codeborne.selenide.Condition;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.DynamicScanDTO;
import com.fortify.fod.common.entities.TenantDTO;
import com.fortify.fod.ui.pages.admin.AdminLoginPage;
import com.fortify.fod.ui.pages.admin.dynamic.DynamicQueuePage;
import com.fortify.fod.ui.pages.admin.dynamic.dynamic_scan_details.tasks.DynamicScanTasksPage;
import com.fortify.fod.ui.pages.tenant.TenantLoginPage;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.DynamicScanActions;
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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static com.codeborne.selenide.Selenide.page;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("sbehera3@opentext.com")
@Slf4j
public class DynamicScanTasksTest extends FodBaseTest {

    public String dynamicSiteURL = "http://stg.motortrend.com/";
    public String assessmentType = "Dynamic Website Assessment";
    public String subdomainURL = "https://dev-auth.motortrend.com/create-account";
    public String dynamicPlusWebServices = "Dynamic Plus APIs";
    public String dynamicPlusWebsite = "Dynamic+ Website Assessment";

    List<String> assessmentTypeToValidate = new ArrayList<>() {{
        add("Dynamic Plus APIs");
        add("Dynamic Standard");
        add("Dynamic+ Website Assessment");
    }};

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("518024")
    @Description("N/A Field in Task Steps Won't Stay Selected")
    @Test(groups = {"hf", "regression"})
    public void verifyNAField() {
        DynamicScanDTO dynamicScanDTO = DynamicScanDTO.createDefaultInstance();
        var applicationDTO = ApplicationActions.createApplication(defaultTenantDTO, true);
        TenantLoginPage.navigate().tenantTopNavbar.openApplications().openYourReleases();
        DynamicScanActions.createDynamicScan(dynamicScanDTO,
                applicationDTO, FodCustomTypes.SetupScanPageStatus.InProgress);

        BrowserUtil.clearCookiesLogOff();

        AdminLoginPage.navigate().login(FodConfig.ADMIN_USER_NAME, FodConfig.ADMIN_PASSWORD)
                .adminTopNavbar.openDynamic();
        var dynamicScanOverviewPage = page(DynamicQueuePage.class)
                .openDetailsFor(applicationDTO.getApplicationName());
        dynamicScanOverviewPage.openTasks();
        var taskPage = page(DynamicScanTasksPage.class);
        taskPage.takeTaskButton.click();
        taskPage.takeTaskButton.shouldNotBe(Condition.visible, Duration.ofMinutes(1));
        taskPage.getFirstTask().setNA(true);
        assertThat(taskPage.getFirstTask().isNASelected()).as("Verify N/A checkbox is selected").isTrue();
    }

    @Owner("kbadia@opentext.com")
    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("595008")
    @Description("validate dynamic scan setup page status by adding subdomain URL")
    @Test(groups = {"hf", "regression"})
    public void validateDynamicScanSetupStatus() {
        var applicationDTO = ApplicationActions.createApplication(defaultTenantDTO, true);
        var dynamicScanDTO = DynamicScanDTO.createDefaultInstance();
        dynamicScanDTO.setAssessmentType(assessmentType);
        dynamicScanDTO.setDynamicSiteUrl(dynamicSiteURL);
        dynamicScanDTO.setExcludeUrl(subdomainURL);
        var status = DynamicScanActions.createDynamicScan(dynamicScanDTO, applicationDTO,
                FodCustomTypes.SetupScanPageStatus.Scheduled).getSetupMessages();
        assertThat(status).isEqualTo("Valid");
    }

    @Owner("kbadia@opentext.com")
    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("452004")
    @Description("validate dynamic scan by selecting Dynamic+ Web Services/Website under assessment type" +
            " and included subdomain URLs")
    @Test(groups = {"regression"})
    public void validateDynamicScanWithSubdomainUrls() {
        verifyDynamicScan(subdomainURL);
    }

    @Owner("kbadia@opentext.com")
    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @FodBacklogItem("452004")
    @Description("validate dynamic scan by selecting Dynamic Standard, " +
            "Dynamic+ Web Services and Dynamic APIs under " +
            "assessment type and not included subdomain URLs")
    @Test(groups = {"regression"},
            dependsOnMethods = {"validateDynamicScanWithSubdomainUrls"})
    public void validateDynamicScanWithoutSubdomainUrls() {
        verifyDynamicScan("");
    }

    public void verifyDynamicScan(String includeUrl) {
        var tenantDTO = TenantDTO.createDefaultInstance();
        DynamicScanDTO dynamicScanDTO = DynamicScanDTO.createDefaultInstance();

        tenantDTO.setEntitlementModel(FodCustomTypes.EntitlementModel.Scans);
        tenantDTO.setSubscriptionModel(FodCustomTypes.SubscriptionModel.Period);
        tenantDTO.setOptionsToEnable(new String[]{"Allow scanning with no entitlements"});
        TenantActions.createTenant(tenantDTO, true, false)
                .openAssessmentTypes()
                .selectAllowSingleScan(dynamicPlusWebServices)
                .selectAllowSubscription(dynamicPlusWebServices)
                .selectAllowSingleScan(dynamicPlusWebsite)
                .selectAllowSubscription(dynamicPlusWebsite);

        BrowserUtil.clearCookiesLogOff();

        for (var dynamicPlus : assessmentTypeToValidate) {
            var applicationDTO = ApplicationDTO.createDefaultInstance();
            ApplicationActions
                    .createApplication(applicationDTO, tenantDTO,
                            !dynamicPlus.equals("Dynamic+ Website Assessment")
                                    && !dynamicPlus.equals("Dynamic Standard"));
            dynamicScanDTO.setAssessmentType(dynamicPlus);
            dynamicScanDTO.setDynamicSiteUrl(dynamicSiteURL);
            if (dynamicPlus.equals(dynamicPlusWebsite)) {
                dynamicScanDTO.setIncludeUrl(includeUrl);
            }
            dynamicScanDTO.setExcludeUrl(includeUrl);
            var setupPage = DynamicScanActions.createDynamicScan(dynamicScanDTO, applicationDTO,
                    FodCustomTypes.SetupScanPageStatus.Scheduled, FodCustomTypes.SetupScanPageStatus.InProgress);
            if (!includeUrl.isEmpty()) {
                setupPage.getScopePanel().expand();
                String isSubDomainUrlAdded = setupPage.getAddedSubdomainUrl();
                assertThat(isSubDomainUrlAdded)
                        .as("Verifying if correct subdomain URL added")
                        .isEqualTo(subdomainURL);
            }
            String setupStatus = setupPage.getSetupScanStatus();
            assertThat(setupStatus)
                    .as("Setup status should be Valid")
                    .isEqualToIgnoringCase("Setup Status: Valid");
        }
    }
}