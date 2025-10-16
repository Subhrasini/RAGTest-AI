package com.fortify.fod.ui.test.regression;

import com.codeborne.selenide.Condition;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.DynamicScanDTO;
import com.fortify.fod.common.entities.EntitlementDTO;
import com.fortify.fod.common.entities.TenantDTO;
import com.fortify.fod.ui.pages.common.admin.cells.FileCell;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.DynamicScanActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import com.fortify.fod.ui.test.actions.TenantActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("vdubovyk@opentext.com")
@Slf4j
public class DynamicScanOverviewTest extends FodBaseTest {

    private ApplicationDTO applicationDTO;
    private DynamicScanDTO dynamicScanDTO;
    private TenantDTO tenantDTO;

    String assessmentType = "AUTO-Dynamic Plus Web Services";
    String dynamicUrl = "https://not.a.valid.url.local/?SPHostUrl=https%3A%2F%2Fdigitalworkspaceuat%2Efnb%2Ecfbi%2Elocal%2Fcomm%2Fcredit%2Ftransm&source=https://digitalworkspaceuat.fnb.cfbi.local/comm/credit/transm/Lists/Transmittal/My%20Items.aspx&id=null";
    String excludeUrl = "google.com";
    String primaryUsername = "TestUserPrimaryUsername";
    String primaryPassword = "TestUserPrimaryPassword";
    String secondaryUsername = "TestUserSecondaryUsername";
    String secondaryPassword = "TestUserSecondaryPassword";
    String networkUsername = "TestUserNetworkUsername";
    String networkPassword = "TestUserNetworkPassword";
    String additionalAuthenticationInstructions = "Some Test Additional Authentication Instructions";
    String webServiceDocument = "payloads/fod/example-6.json";
    String webServiceAdditionalInstructions = "Some Test Web Service Additional Instructions";
    String webServiceUsername = "WSUser1";
    String webServicePassword = "123456789";
    String webServiceAPIKey = "TestApiKey";
    String webServiceAPIPassword = "987654321";
    String additionalNotes = "Some Test Additional Note";
    String additionalDocumentationFile = "payloads/fod/_test.txt";

    @MaxRetryCount(3)
    @FodBacklogItem("584008")
    @Severity(SeverityLevel.NORMAL)
    @Description("Create tenant / application / dynamic scan for test execution")
    @Test(groups = {"regression"})
    public void testDataPreparation() {

        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setEntitlementModel(FodCustomTypes.EntitlementModel.Scans);
        tenantDTO.setSubscriptionModel(FodCustomTypes.SubscriptionModel.Period);
        tenantDTO.setEntitlementDTO(EntitlementDTO.createScanInstance());
        tenantDTO.setOptionsToEnable(new String[]
                {"Allow scanning with no entitlements",
                        "Request WAF virtual patch for dynamic scans"});

        TenantActions.createTenant(tenantDTO);
        BrowserUtil.clearCookiesLogOff();

        applicationDTO = ApplicationActions.createApplication(tenantDTO);

        dynamicScanDTO = DynamicScanDTO.createDefaultInstance();
        dynamicScanDTO.setAssessmentType(assessmentType);
        dynamicScanDTO.setDynamicSiteUrl(dynamicUrl);
        dynamicScanDTO.setExcludeUrl(excludeUrl);

        dynamicScanDTO.setFormsAuthRequired(true);
        dynamicScanDTO.setPrimaryUsername(primaryUsername);
        dynamicScanDTO.setPrimaryPassword(primaryPassword);
        dynamicScanDTO.setSecondaryUsername(secondaryUsername);
        dynamicScanDTO.setSecondaryPassword(secondaryPassword);

        dynamicScanDTO.setNetworkAuthenticationRequired(true);
        dynamicScanDTO.setNetworkUsername(networkUsername);
        dynamicScanDTO.setNetworkPassword(networkPassword);

        dynamicScanDTO.setAdditionalAuthenticationInstructionsRequired(true);
        dynamicScanDTO.setAdditionalAuthenticationInstructions(additionalAuthenticationInstructions);

        dynamicScanDTO.setWebServicesRequired(true);
        dynamicScanDTO.setWebServiceType(FodCustomTypes.DynamicScanApiType.REST);
        dynamicScanDTO.setWebServiceDocument(webServiceDocument);
        dynamicScanDTO.setWebServiceAdditionalInstructions(webServiceAdditionalInstructions);
        dynamicScanDTO.setWebServiceUsername(webServiceUsername);
        dynamicScanDTO.setWebServicePassword(webServicePassword);
        dynamicScanDTO.setWebServiceAPIKey(webServiceAPIKey);
        dynamicScanDTO.setWebServiceAPIPassword(webServiceAPIPassword);

        dynamicScanDTO.setAdditionalNotes(additionalNotes);
        dynamicScanDTO.setAdditionalDocumentationFile(additionalDocumentationFile);
        dynamicScanDTO.setGenerateWAFVirtualPatch(true);

        dynamicScanDTO.setValidateRestrict("No");

        DynamicScanActions.createDynamicScan(dynamicScanDTO, applicationDTO);
    }

    @MaxRetryCount(3)
    @FodBacklogItem("584008")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify Dynamic Overview scan page")
    @Test(groups = {"regression"}, dependsOnMethods = {"testDataPreparation"})
    public void dynamicScanOverviewTest() {

        var overviewPage = LogInActions.adminLogIn().adminTopNavbar.openDynamic()
                .openDetailsFor(applicationDTO.getApplicationName()).openOverview();

        var softAssert = new SoftAssertions();
        assertThat(overviewPage.getAppName())
                .as("Correct application found")
                .isEqualTo(applicationDTO.getApplicationName());

        softAssert.assertThat(overviewPage.getOverviewValueByTitle("Dynamic Site URL")).isEqualTo(dynamicScanDTO.getDynamicSiteUrl());
        softAssert.assertThat(overviewPage.getOverviewValueByTitle("Customer Selected Time Zone")).isEqualTo(dynamicScanDTO.getTimezone());
        softAssert.assertThat(overviewPage.getOverviewValueByTitle("Environment Facing")).isEqualTo(dynamicScanDTO.getEnvironmentalFacing().getTypeValue());
        softAssert.assertThat(overviewPage.getOverviewValueByTitle("Additional Authentication Instructions")).isEqualTo(dynamicScanDTO.getAdditionalAuthenticationInstructions());
        softAssert.assertThat(overviewPage.getOverviewValueByTitle("Additional Instructions")).isEqualTo(dynamicScanDTO.getWebServiceAdditionalInstructions());
        softAssert.assertThat(overviewPage.getOverviewValueByTitle("API Type")).isEqualTo(dynamicScanDTO.getWebServiceType().getTypeValue());
        softAssert.assertThat(overviewPage.getOverviewValueByTitle("Additional Notes")).isEqualTo(dynamicScanDTO.getAdditionalNotes());

        softAssert.assertThat(overviewPage.getOverviewValueByTitle("Restrict to directory and subdirectories")).isEqualTo(dynamicScanDTO.getValidateRestrict());
        softAssert.assertThat(overviewPage.getOverviewValueByTitle("Allow HTTP (:80) and HTTPS (:443)")).isEqualTo("Yes");
        softAssert.assertThat(overviewPage.getOverviewValueByTitle("Allow form submissions during crawl")).isEqualTo("Yes");
        softAssert.assertThat(overviewPage.getOverviewValueByTitle("Use App Authentication")).isEqualTo("No");
        softAssert.assertThat(overviewPage.getOverviewValueByTitle("User Agent")).isEqualTo("Desktop browser");
        softAssert.assertThat(overviewPage.getOverviewValueByTitle("Concurrent")).isEqualTo("Standard");
        softAssert.assertThat(overviewPage.getOverviewValueByTitle("Pre-Assessment Call")).isEqualTo("No");

        softAssert.assertAll();

        overviewPage.getOverviewValueElementByTitle("Forms Authentication Required")
                .shouldHave(Condition.text(dynamicScanDTO.getPrimaryUsername()))
                .shouldHave(Condition.text(dynamicScanDTO.getPrimaryPassword()))
                .shouldHave(Condition.text(dynamicScanDTO.getSecondaryUsername()))
                .shouldHave(Condition.text(dynamicScanDTO.getSecondaryPassword()));

        overviewPage.getOverviewValueElementByTitle("Network Authentication Required")
                .shouldHave(Condition.text(dynamicScanDTO.getNetworkUsername()))
                .shouldHave(Condition.text(dynamicScanDTO.getNetworkPassword()));

        overviewPage.getOverviewValueElementByTitle("API Key")
                .shouldHave(Condition.text(dynamicScanDTO.getWebServiceAPIKey()))
                .shouldHave(Condition.text(dynamicScanDTO.getWebServiceAPIPassword()));

        overviewPage.getOverviewValueElementByTitle("API Unique Authentication")
                .shouldHave(Condition.text(dynamicScanDTO.getWebServiceUsername()))
                .shouldHave(Condition.text(dynamicScanDTO.getWebServicePassword()));

        var files = overviewPage.openFiles().getAllFiles().stream().map(FileCell::getName)
                .collect(Collectors.toList());
        assertThat(files)
                .contains(dynamicScanDTO.getWebServiceDocument().split("/")[2],
                        dynamicScanDTO.getAdditionalDocumentationFile().split("/")[2]);
    }
}