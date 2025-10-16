package com.fortify.fod.ui.test.regression;

import com.codeborne.selenide.Condition;
import com.fortify.common.utils.UniqueRunTag;
import com.fortify.fod.common.elements.Checkbox;
import com.fortify.fod.ui.pages.tenant.administration.policy_management.AddEditPolicyPopup;
import com.fortify.fod.ui.pages.tenant.administration.policy_management.PolicyManagementPage;
import com.fortify.fod.ui.pages.tenant.administration.user_management.UserManagementPage;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.LogInActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.Test;
import utils.MaxRetryCount;

import java.time.Duration;
import java.util.ArrayList;

import static com.codeborne.selenide.Selenide.page;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("ysmal@opentext.com")
@Slf4j
public class CreatePolicyManagementSetupWizardTest extends FodBaseTest {

    private static int testNumber = 0;
    private static ArrayList<String> developmentReleases = new ArrayList<>();
    private static ArrayList<String> qaTestReleases = new ArrayList<>();
    private static ArrayList<String> productionReleases = new ArrayList<>();

    private String policyName = "New Policy - " + UniqueRunTag.generate();
    private int starRating = 3;
    private String applicationMonitoring = "Not required";
    private String complianceRequirement = "All vulnerability categories";

    private ArrayList<String> expectedStepsList = new ArrayList<>() {{
        add("Policy Details");
        add("Assessment Types");
        add("Add-on Services");
        add("Development Releases");
        add("QA/Test Releases");
        add("Production Releases");
        add("Summary");
    }};

    private ArrayList<String> expectedPolicyDetailsFields = new ArrayList<>() {{
        add("Policy Name");
        add("Star Rating");
        add("Application Monitoring");
        add("Compliance Requirement");
    }};

    private ArrayList<String> expectedReleasesFields = new ArrayList<>() {{
        add("Critical");
        add("High");
        add("Static");
        add("Dynamic");
        add("Mobile");
    }};

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Tam user should be able to create policy")
    @Test(groups = {"regression"})
    public void createPolicyManagementSetupWizardTest() {

        SoftAssertions softAssertions = new SoftAssertions();
        LogInActions.tamUserLogin(defaultTenantDTO)
                .tenantTopNavbar.openAdministration();
        page(UserManagementPage.class)
                .openPolicyManagement();

        var policyPage = page(PolicyManagementPage.class);
        var addPolicyPopup = policyPage.openPolicies().pressAddPolicy();

        assertThat(addPolicyPopup.addEditPolicyPopup.shouldBe(Condition.visible, Duration.ofMinutes(1)).isDisplayed())
                .as("Popup should be opened").isTrue();

        assertThat(addPolicyPopup.getAllWizardSteps())
                .as("Wizard steps should be equal to expected.").isEqualTo(expectedStepsList);

        assertThat(addPolicyPopup.getCurrentStep())
                .as("Current step should be: " + expectedStepsList.get(0))
                .isEqualTo(expectedStepsList.get(0));

        for (var field : expectedPolicyDetailsFields) {
            var fieldElem = addPolicyPopup.findFieldByTitle(field);
            softAssertions.assertThat(fieldElem != null && fieldElem.isDisplayed())
                    .as("Validate that field present: " + field)
                    .isTrue();
        }
        softAssertions.assertAll();

        addPolicyPopup.setPolicyName(policyName);
        addPolicyPopup.setStarRating(starRating);
        addPolicyPopup.setApplicationMonitoring(applicationMonitoring);
        addPolicyPopup.setComplianceRequirement(complianceRequirement);
        addPolicyPopup.pressNext();

        assertThat(addPolicyPopup.getCurrentStep())
                .as("Current step should be: " + expectedStepsList.get(1))
                .isEqualTo(expectedStepsList.get(1));

        assertThat(new Checkbox(addPolicyPopup.allowAllAssessmentTypesTestCheckbox).checked())
                .as("Validate that 'Allow All Assessment Types' checkbox is checked by default")
                .isTrue();

        addPolicyPopup.pressNext();
        addPolicyPopup.pressNext();

        for (var expectedStepName : new String[]{
                "Development Releases", "QA/Test Releases", "Production Releases"}) {
            ValidateReleasesTabs(expectedStepName, expectedReleasesFields);
            FillAllReleasesFields(expectedReleasesFields);
            addPolicyPopup.pressNext();
        }

        assertThat(addPolicyPopup.getCurrentStep())
                .as("Current step should be: " + expectedStepsList.get(6))
                .isEqualTo(expectedStepsList.get(6));

        var expectedSectionsList = expectedStepsList;
        expectedSectionsList.remove(expectedStepsList.size() - 1);

        assertThat(expectedSectionsList)
                .as("Validate that sections present in expected list")
                .isEqualTo(addPolicyPopup.getSummarySections().texts());

        //Validate Policy Details
        ValidateSummary("Policy Details", "Policy Name", policyName);
        ValidateSummary("Policy Details", "Star Rating", String.format("At least %d stars", starRating));
        ValidateSummary("Policy Details", "Compliance Requirement", complianceRequirement);
        ValidateSummary("Policy Details", "Application Monitoring", applicationMonitoring);

        //Validate Assessment Types
        ValidateSummary("Assessment Types", "", "Allow All Assessment Types");

        //Validate Releases sections
        ValidateSummary("Development Releases", developmentReleases);
        ValidateSummary("QA/Test Releases", qaTestReleases);
        ValidateSummary("Production Releases", productionReleases);

        var policiesTable = addPolicyPopup.pressSave().openPolicies().getPoliciesTable();

        assertThat(policiesTable.getAllColumnValues(policiesTable.getColumnIndex("Policy Name")))
                .as("Validate that policy created")
                .contains(policyName);
    }

    private void ValidateReleasesTabs(String expectedStepName, ArrayList<String> fieldsToValidate) {
        var policyPopup = page(AddEditPolicyPopup.class);
        var currentStep = policyPopup.getCurrentStep();
        SoftAssertions softAssertions = new SoftAssertions();
        softAssertions.assertThat(currentStep).as("Validate that current step is equal to: " + expectedStepName)
                .isEqualTo(expectedStepName);

        var actualSections = policyPopup.getReleasesPageTitles();
        var expectedSections = new String[]{
                "Remediation Grace Period ( 0 - 365 Days)",
                "Required Scan Frequency ( 0 - 365 Days)"
        };

        softAssertions.assertThat(actualSections.get(0).text()).isEqualTo((expectedSections[0]));
        softAssertions.assertThat(actualSections.get(1).text()).isEqualTo((expectedSections[1]));

        for (var field : fieldsToValidate) {
            var fieldElem = policyPopup.findFieldByTitle(field);
            softAssertions.assertThat(fieldElem != null && fieldElem.isDisplayed())
                    .as("Validate that field present: " + field)
                    .isTrue();
        }
        softAssertions.assertAll();
    }

    private static void FillAllReleasesFields(ArrayList<String> fields) {
        var policyPopup = page(AddEditPolicyPopup.class);
        var currentStep = policyPopup.getCurrentStep();
        for (var field : fields) {
            policyPopup.findFieldByTitle(field).setValue(Integer.toString(testNumber));
            switch (currentStep) {
                case "Development Releases":
                    developmentReleases.add(String.format("%s: %d days", field, testNumber));
                    break;
                case "QA/Test Releases":
                    qaTestReleases.add(String.format("%s: %d days", field, testNumber));
                    break;
                case "Production Releases":
                    productionReleases.add(String.format("%s: %d days", field, testNumber));
                    break;
            }
            testNumber++;
        }
    }

    private void ValidateSummary(String section, ArrayList<String> expectedValues) {
        var policyPopup = page(AddEditPolicyPopup.class);
        var actualList = policyPopup.getSectionValues(section, "");
        assertThat(actualList.size()).isEqualTo(expectedValues.size());
        for (var expectedValue : expectedValues) {
            ValidateSummary(section, "", expectedValue);
        }
    }

    private void ValidateSummary(String section, String title, String expectedValue) {
        var policyPopup = page(AddEditPolicyPopup.class);
        var actualValues = policyPopup.getSectionValues(section, title);
        var description = !title.isEmpty()
                ? String.format("'%s' is valid for '%s' section.", title, section)
                : String.format("'%s' section is valid.", section);
        assertThat(actualValues).as(description).contains(expectedValue);
    }
}
