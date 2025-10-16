package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.*;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;

import static com.codeborne.selenide.Selenide.refresh;
import static com.codeborne.selenide.Selenide.sleep;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("ysmal@opentext.com")
@Slf4j
public class PolicyManagementTest extends FodBaseTest {

    TenantDTO tenantDTO;
    ApplicationDTO applicationA, applicationB, applicationC;
    DynamicScanDTO dynamicScan1, dynamicScan2;
    MobileScanDTO mobileScanDTO;
    AttributeDTO attributeDTO;
    HashMap<AttributeDTO, String> attributesMapA, attributesMapB, attributesMapC;
    PolicyManagementDTO policy1Star, policy5Star;

    public void init() {
        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setEntitlementDTO(EntitlementDTO.createDefaultInstance());

        attributeDTO = AttributeDTO.createDefaultInstance();
        attributeDTO.setAttributeDataType(FodCustomTypes.AttributeDataType.Picklist);
        attributeDTO.setPickListValues(new String[]{"A", "B", "C"});

        attributesMapA = new HashMap<>() {{
            put(attributeDTO, "A");
        }};

        attributesMapB = new HashMap<>() {{
            put(attributeDTO, "B");
        }};

        attributesMapC = new HashMap<>() {{
            put(attributeDTO, "C");
        }};

        applicationA = ApplicationDTO.createDefaultInstance();
        applicationA.setSdlcStatus(FodCustomTypes.Sdlc.Production);
        applicationA.setAttributesMap(attributesMapA);
        applicationB = ApplicationDTO.createDefaultMobileInstance();
        applicationB.setSdlcStatus(FodCustomTypes.Sdlc.Production);
        applicationB.setAttributesMap(attributesMapB);
        applicationC = ApplicationDTO.createDefaultInstance();
        applicationC.setSdlcStatus(FodCustomTypes.Sdlc.Production);
        applicationC.setAttributesMap(attributesMapC);

        dynamicScan1 = DynamicScanDTO.createDefaultInstance();
        dynamicScan1.setStartInFuture(true);
        dynamicScan1.setEntitlement("Single Scan");

        dynamicScan2 = DynamicScanDTO.createDefaultInstance();
        dynamicScan2.setStartInFuture(true);
        dynamicScan2.setEntitlement("Single Scan");

        mobileScanDTO = MobileScanDTO.createDefaultScanInstance();
        mobileScanDTO.setAssessmentType("AUTO-MOBILE");
        mobileScanDTO.setEntitlement("Single Scan");

        policy1Star = PolicyManagementDTO.createDefaultInstance();
        policy1Star.setStarsRating(1);
        policy1Star.setAssessmentToChoose(new ArrayList<>() {{
            add("AUTO-MOBILE - Single Scan");
        }});

        policy5Star = PolicyManagementDTO.createDefaultInstance();
        policy5Star.setStarsRating(5);
        policy5Star.setAssessmentToChoose(new ArrayList<>() {{
            add("AUTO-DYNAMIC - Single Scan");
        }});
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Create tenant / applications / custom attribute for test execution")
    @Test(groups = {"regression"})
    public void testDataPreparation() {
        init();

        TenantActions.createTenant(tenantDTO);
        BrowserUtil.clearCookiesLogOff();
        CustomAttributesActions.createCustomAttribute(attributeDTO, tenantDTO, true);

        ApplicationActions.createApplication(applicationA);
        ApplicationActions.createApplication(applicationB);
        ApplicationActions.createApplication(applicationC);

        DynamicScanActions.createDynamicScan(dynamicScan1, applicationA,
                FodCustomTypes.SetupScanPageStatus.Scheduled,
                FodCustomTypes.SetupScanPageStatus.InProgress,
                FodCustomTypes.SetupScanPageStatus.Queued);
        DynamicScanActions.createDynamicScan(dynamicScan2, applicationC,
                FodCustomTypes.SetupScanPageStatus.Scheduled,
                FodCustomTypes.SetupScanPageStatus.InProgress,
                FodCustomTypes.SetupScanPageStatus.Queued);
        MobileScanActions.createMobileScan(mobileScanDTO, applicationB);

        BrowserUtil.clearCookiesLogOff();
        DynamicScanActions.importDynamicScanAdmin(applicationA.getReleaseName(),
                "payloads/fod/oxwall.scan",
                true, true, true,
                false, true);
        DynamicScanActions.completeDynamicScanAdmin(applicationA, false);

        DynamicScanActions.importDynamicScanAdmin(applicationC.getReleaseName(),
                "payloads/fod/dynamic.zero.fpr",
                true, true, false,
                false, false);
        DynamicScanActions.completeDynamicScanAdmin(applicationC, false);
        MobileScanActions.publishMobileScanWithoutImportFpr(applicationB, false);
        BrowserUtil.clearCookiesLogOff();

        LogInActions.tamUserLogin(tenantDTO);
        PolicyManagementActions.createPolicyManagement(policy1Star);
        PolicyManagementActions.createPolicyManagement(policy5Star);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate Business Criticality Policy Scope")
    @Test(groups = {"regression"}, dependsOnMethods = {"testDataPreparation"},
            priority = 1)
    public void businessCriticalityPolicyScopeTest() {
        var policyPage = LogInActions.tamUserLogin(tenantDTO)
                .tenantTopNavbar.openAdministration()
                .openPolicyManagement();
        policyPage.pressEditScope().setAssignmentType(FodCustomTypes.PolicyScope.BusinessCriticality).pressSave();
        policyPage.getPolicyAssignmentDropDownByName("Low").selectOptionContainingText(policy1Star.getPolicyName());
        policyPage.pressSave();
        var securityPopup = policyPage.viewSecurityPolicyByPolicyAssignmentName("Low");
        assertThat(securityPopup.getPolicyName())
                .isEqualTo(policy1Star.getPolicyName());
        securityPopup.pressClose();

        var dashboard = policyPage.tenantTopNavbar.openDashboard();
        dashboard.spinner.waitTillLoading(1, true);
        if (dashboard.getPolicyCompliancePassCount() != 1) {
            sleep(Duration.ofSeconds(30).toMillis());
            refresh();
            dashboard.spinner.waitTillLoading(1, true);
        }
        assertThat(dashboard.getPolicyCompliancePassCount())
                .as("Pass count should be equal 1")
                .isEqualTo(1);
        assertThat(dashboard.getPolicyComplianceFailCount())
                .as("Fail count should be equal 2")
                .isEqualTo(2);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate Application Type Policy Scope")
    @Test(groups = {"regression"}, dependsOnMethods = {"testDataPreparation"},
            priority = 2)
    public void applicationTypePolicyScopeTest() {
        var policyPage = LogInActions.tamUserLogin(tenantDTO)
                .tenantTopNavbar.openAdministration()
                .openPolicyManagement();
        policyPage.pressEditScope().setAssignmentType(FodCustomTypes.PolicyScope.ApplicationType).pressSave();
        policyPage.getPolicyAssignmentDropDownByName("Web / Thick-Client")
                .selectOptionContainingText(policy1Star.getPolicyName());
        policyPage.pressSave();
        var securityPopup = policyPage.viewSecurityPolicyByPolicyAssignmentName("Web / Thick-Client");
        assertThat(securityPopup.getPolicyName())
                .isEqualTo(policy1Star.getPolicyName());
        securityPopup.pressClose();

        policyPage.getPolicyAssignmentDropDownByName("Mobile")
                .selectOptionContainingText(policy5Star.getPolicyName());
        policyPage.pressSave();
        securityPopup = policyPage.viewSecurityPolicyByPolicyAssignmentName("Mobile");
        assertThat(securityPopup.getPolicyName())
                .isEqualTo(policy5Star.getPolicyName());
        securityPopup.pressClose();

        var dashboard = policyPage.tenantTopNavbar.openDashboard();
        dashboard.spinner.waitTillLoading(1, true);
        if (dashboard.getPolicyCompliancePassCount() != 2) {
            sleep(Duration.ofSeconds(30).toMillis());
            refresh();
            dashboard.spinner.waitTillLoading(1, true);
        }
        assertThat(dashboard.getPolicyCompliancePassCount())
                .as("Pass count should be equal 2")
                .isEqualTo(2);
        assertThat(dashboard.getPolicyComplianceFailCount())
                .as("Fail count should be equal 1")
                .isEqualTo(1);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate Application Attribute Policy Scope")
    @Test(groups = {"regression"}, dependsOnMethods = {"testDataPreparation"},
            priority = 3)
    public void applicationAttributePolicyScopeTest() {
        var policyPage = LogInActions.tamUserLogin(tenantDTO)
                .tenantTopNavbar.openAdministration()
                .openPolicyManagement();
        policyPage.pressEditScope().setAssignmentType(FodCustomTypes.PolicyScope.ApplicationAttribute)
                .setAttribute(attributeDTO.getAttributeName())
                .pressSave();
        policyPage.getPolicyAssignmentDropDownByName("A")
                .selectOptionContainingText(policy5Star.getPolicyName());
        policyPage.pressSave();
        var securityPopup = policyPage.viewSecurityPolicyByPolicyAssignmentName("A");
        assertThat(securityPopup.getPolicyName())
                .isEqualTo(policy5Star.getPolicyName());
        securityPopup.pressClose();

        policyPage.getPolicyAssignmentDropDownByName("C")
                .selectOptionContainingText(policy1Star.getPolicyName());
        policyPage.pressSave();
        securityPopup = policyPage.viewSecurityPolicyByPolicyAssignmentName("C");
        assertThat(securityPopup.getPolicyName())
                .isEqualTo(policy1Star.getPolicyName());
        securityPopup.pressClose();

        var dashboard = policyPage.tenantTopNavbar.openDashboard();
        dashboard.spinner.waitTillLoading(1, true);
        if (dashboard.getPolicyCompliancePassCount() != 1) {
            sleep(Duration.ofSeconds(30).toMillis());
            refresh();
            dashboard.spinner.waitTillLoading(1, true);
        }
        assertThat(dashboard.getPolicyCompliancePassCount())
                .as("Pass count should be equal 1")
                .isEqualTo(1);
        assertThat(dashboard.getPolicyComplianceFailCount())
                .as("Fail count should be equal 2")
                .isEqualTo(2);
    }
}