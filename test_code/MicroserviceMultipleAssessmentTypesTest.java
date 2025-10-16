package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.UniqueRunTag;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.elements.Paging;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("ysmal@opentext.com")
@Slf4j
public class MicroserviceMultipleAssessmentTypesTest extends FodBaseTest {

    ApplicationDTO applicationDTO;
    String runTag;
    String assessmentTypeName;
    String microserviceName;

    int entitlementsCount;

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate that MicroserviceStaticAssessmentTypeName deleted from site settings")
    @Test(groups = {"regression"})
    public void validateThatOldOptionDeleted() {
        var settingsPage = LogInActions.adminLogIn().adminTopNavbar.openConfiguration();
        assertThat(settingsPage.isSettingExists("MicroserviceStaticAssessmentTypeName"))
                .as("Setting 'MicroserviceStaticAssessmentTypeName' should be removed")
                .isFalse();
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Create assessment type with microservices")
    @Test(groups = {"regression"})
    public void createAssessmentType() {
        runTag = UniqueRunTag.generate();
        assessmentTypeName = "Assessment" + runTag;
        var assessmentPage = LogInActions.adminLogIn().adminTopNavbar.openAssessmentTypes();
        var paging = new Paging();
        paging.setRecordsPerPage(paging.getTotalRecords());
        var allow = Objects.requireNonNull(assessmentPage.getAllAssessmentTypes().stream()
                .filter(x -> x.getName().equals("Static Assessment")).findFirst()
                .orElse(null)).getAllowMicroservice();

        assertThat(allow)
                .as("Allow Microservices set on Yes for Static Assessment")
                .isEqualTo("Yes");

        var addAssessmentPopup = assessmentPage.pressAddAssessmentTypeBtn();
        for (var option : addAssessmentPopup.assessmentCategoryDropdown.$$("option").texts()) {
            boolean shouldBeEnabled = option.equals("Static");

            addAssessmentPopup.setAssessmentCategory(option);
            BrowserUtil.waitAjaxLoaded();

            var validation = addAssessmentPopup.allowMicroservicesSwitcher.parent().isDisplayed() == shouldBeEnabled;
            var expectedResult = shouldBeEnabled ? "enabled" : "disabled";
            assertThat(validation)
                    .as(String.format("Allow Microservices for %s assessment category should be %s",
                            option, expectedResult))
                    .isTrue();
        }

        addAssessmentPopup.setAssessmentTypeName(assessmentTypeName)
                .setAnalysisType(FodCustomTypes.AnalysisType.Static)
                .setAssessmentCategory("Static")
                .setWorkflow("Static")
                .setSlaDays("2")
                .setRemediationWorkflow("Static")
                .setSingleScanUnits("1")
                .setSubscriptionScanUnits("1")
                .setAllowMicroservices(true)
                .openTenantsTab()
                .allowScansForTenant(defaultTenantDTO.getTenantName(), true, true)
                .pressSaveButton()
                .spinner.waitTillLoading();

        assertThat(assessmentPage.getAssessmentTypeByName(assessmentTypeName)).isNotNull();

        entitlementsCount = (int) assessmentPage
                .adminTopNavbar.openTenants()
                .openTenantByName(defaultTenantDTO.getTenantCode())
                .openEntitlements()
                .getAll()
                .stream().filter(x -> validateData(x.getEndDate())).count();
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Create application with microservices")
    @Test(dependsOnMethods = {"createAssessmentType"}, groups = {"regression"})
    public void createApplicationAndValidateAssessmentType() {
        microserviceName = "microservice" + runTag;
        applicationDTO = ApplicationDTO.createDefaultInstance();
        applicationDTO.setMicroserviceToChoose(microserviceName);
        applicationDTO.setMicroservicesEnabled(true);
        applicationDTO.setApplicationMicroservices(new String[]{microserviceName});

        var staticScanSetupPage = ApplicationActions
                .createApplication(applicationDTO, defaultTenantDTO, true)
                .openStaticScanSetup();

        String[] assessments = new String[]{assessmentTypeName, "Static Assessment"};
        for (var assessment : assessments) {
            staticScanSetupPage.chooseAssessmentType(assessment);
            var listEntitlements = staticScanSetupPage.entitlementDropdown
                    .$$("option").texts();
            var listAuditPreference = staticScanSetupPage.auditPreferenceDropdown
                    .$$("option").texts();

            assertThat(listEntitlements).hasSize(entitlementsCount + 1);
            assertThat(listEntitlements.stream().filter(x -> x.contains("Single")).findFirst().orElse(null))
                    .as("Entitlements for %s contains only subscription", assessment)
                    .isNull();

            assertThat(listAuditPreference).hasSize(1);
            assertThat(listAuditPreference.stream().filter(x -> x.contains("Automated")).findFirst())
                    .as("Audit Preference for for %s contains only Automated", assessment)
                    .isPresent();
        }
    }

    private boolean validateData(String data) {
        String dateFormatPattern = "mm/dd/yyyy";
        DateFormat dateFormat = new SimpleDateFormat(dateFormatPattern);
        Date today = Calendar.getInstance().getTime();
        try {
            return today.before(dateFormat.parse(data));
        } catch (ParseException ignore) {
            return false;
        }
    }
}
