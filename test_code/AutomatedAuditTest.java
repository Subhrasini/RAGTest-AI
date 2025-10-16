package com.fortify.fod.ui.test.regression;

import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.AssessmentTypesDTO;
import com.fortify.fod.common.entities.StaticScanDTO;
import com.fortify.fod.ui.pages.tenant.applications.YourScansPage;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.AssessmentTypesActions;
import com.fortify.fod.ui.test.actions.StaticScanActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.MaxRetryCount;

import static com.codeborne.selenide.Selenide.page;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Owner("ysmal@opentext.com")
@Slf4j
public class AutomatedAuditTest extends FodBaseTest {

    AssessmentTypesDTO staticSingle;
    AssessmentTypesDTO staticPlusSubscription;
    StaticScanDTO staticSingleScan;
    StaticScanDTO staticPlusSubscriptionScan;
    ApplicationDTO applicationOne;
    ApplicationDTO applicationTwo;
    ApplicationDTO applicationThree;

    String fileToUpload = "payloads/fod/WebGoat5.0.zip";

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Admin user should be able to create assessment types on admin site.")
    @Test(groups = {"regression"})
    public void createAssessmentTypesTest() {
        staticSingle = AssessmentTypesDTO.createDefaultInstance();
        staticSingle.setTenantToAssign(defaultTenantDTO.getTenantCode());
        staticSingle.setAllowSingleScanForTenant(true);
        staticSingle.setAllowSubscriptionForTenant(true);
        staticPlusSubscription = AssessmentTypesDTO.createDefaultInstance();
        staticPlusSubscription.setTenantToAssign(defaultTenantDTO.getTenantCode());
        staticPlusSubscription.setAllowSingleScanForTenant(true);
        staticPlusSubscription.setAllowSubscriptionForTenant(true);
        staticPlusSubscription.setAssessmentCategory("Static Plus");

        AssessmentTypesActions.createAssessmentType(staticSingle, true);
        AssessmentTypesActions.createAssessmentType(staticPlusSubscription);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Tam user should be able to create static scan on tenant site.")
    @Test(groups = {"regression"}, dependsOnMethods = {"createAssessmentTypesTest"})
    public void createFirstScan() {
        applicationOne = ApplicationDTO.createDefaultInstance();
        ApplicationActions.createApplication(applicationOne, defaultTenantDTO, true);

        staticSingleScan = StaticScanDTO.createDefaultInstance();
        staticSingleScan.setAssessmentType(staticSingle.getAssessmentTypeName());
        staticSingleScan.setFileToUpload(fileToUpload);

        StaticScanActions.createStaticScan(staticSingleScan, applicationOne,
                FodCustomTypes.SetupScanPageStatus.Completed);

        var yourScansPage = page(YourScansPage.class);
        yourScansPage.tenantTopNavbar.openApplications().openYourScans();
        var scan = yourScansPage.getScanByType(applicationOne, FodCustomTypes.ScanType.Static)
                .waitStatus(FodCustomTypes.ScansPageStatus.Completed);
        assertThat(scan.getTotalCount()).as("Validate that scan has issues").isPositive();
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Tam user should be able to create static scan on tenant site.")
    @Test(groups = {"regression"}, dependsOnMethods = {"createAssessmentTypesTest"})
    public void createSecondScan() {
        applicationTwo = ApplicationDTO.createDefaultInstance();
        ApplicationActions.createApplication(applicationTwo, defaultTenantDTO, true);

        staticPlusSubscriptionScan = StaticScanDTO.createDefaultInstance();
        staticPlusSubscriptionScan.setAssessmentType(staticPlusSubscription.getAssessmentTypeName());
        staticPlusSubscriptionScan.setFileToUpload(fileToUpload);

        StaticScanActions.createStaticScan(staticPlusSubscriptionScan, applicationTwo,
                FodCustomTypes.SetupScanPageStatus.Completed);

        var yourScansPage = page(YourScansPage.class);
        yourScansPage.tenantTopNavbar.openApplications().openYourScans();
        var scan = yourScansPage.getScanByType(applicationTwo, FodCustomTypes.ScanType.Static)
                .waitStatus(FodCustomTypes.ScansPageStatus.Completed);
        assertThat(scan.getTotalCount()).as("Validate that scan has issues").isPositive();
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Tam user should be able to cancel static scan on tenant site.")
    @Test(groups = {"regression"}, dependsOnMethods = {"createAssessmentTypesTest"})
    public void createScanAndCancel() {
        applicationThree = ApplicationDTO.createDefaultInstance();
        ApplicationActions.createApplication(applicationThree, defaultTenantDTO, true);

        staticPlusSubscriptionScan = StaticScanDTO.createDefaultInstance();
        staticPlusSubscriptionScan.setAuditPreference(FodCustomTypes.AuditPreference.Manual);
        staticSingleScan = StaticScanDTO.createDefaultInstance();
        staticSingleScan.setFileToUpload(fileToUpload);

        StaticScanActions.createStaticScan(staticPlusSubscriptionScan, applicationThree,
                FodCustomTypes.SetupScanPageStatus.InProgress);
        StaticScanActions.cancelScanTenant(applicationThree);
    }
}
