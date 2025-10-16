package com.fortify.fod.ui.test.regression;

import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.TenantDTO;
import com.fortify.fod.ui.pages.admin.AdminLoginPage;
import com.fortify.fod.ui.pages.admin.administration.tenants.TenantsPage;
import com.fortify.fod.ui.pages.admin.administration.tenants.tenant.TenantDetailsPage;
import com.fortify.fod.ui.pages.admin.administration.tenants.tenant.entitlements.EntitlementCell;
import com.fortify.fod.ui.pages.admin.administration.tenants.tenant.entitlements.EntitlementsPage;
import com.fortify.fod.ui.pages.admin.administration.tenants.tenant.popups.AddEditEntitlementPopup;
import com.fortify.fod.ui.pages.admin.user_menu.MyAccountAdminPage;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.TenantActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;

import static com.codeborne.selenide.Selenide.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Owner("ysmal@opentext.com")
@Slf4j
public class ChangingEntitlementModelsTest extends FodBaseTest {

    String expectedQuantityPurchasedError = "The Quantity Purchased field is required.";
    String expectedAnalysisTypeError = "The Analysis Type field is required.";
    String expectedAssessmentTypeError = "The Assessment Type field is required.";
    String expectedFrequencyError = "The Frequency field is required.";
    String expectedStartDateError = "The Start Date field is required.";
    String expectedEndDateError = "The End Date field is required.";
    String dateFormatPattern = "MM/dd/yyyy";
    DateFormat dateFormat = new SimpleDateFormat(dateFormatPattern);
    Date today = Calendar.getInstance().getTime();
    Date afterYear = DateUtils.addYears(today, 1);
    String todayStr = dateFormat.format(today);
    String afterYearStr = dateFormat.format(afterYear);
    TenantDTO tenantDTO;

    @MaxRetryCount(3)
    @FodBacklogItem("705002")
    @Severity(SeverityLevel.NORMAL)
    @Description("Admin user should be able to create entitlements and change entitlement model")
    @Test(groups = {"regression"})
    public void changingEntitlementModelsTest() {
        tenantDTO = TenantActions.createTenant();
        BrowserUtil.clearCookiesLogOff();

        AdminLoginPage.navigate()
                .login(FodConfig.ADMIN_USER_NAME, FodConfig.ADMIN_PASSWORD)
                .adminTopNavbar
                .openMyAccount();

        var myAccount = page(MyAccountAdminPage.class);
        myAccount.setDateFormat(dateFormatPattern);
        myAccount.pressSave();

        assertThat(myAccount.getDateFormatValue())
                .as("Date format should be equal: " + dateFormatPattern)
                .isEqualTo(dateFormatPattern);

        myAccount.adminTopNavbar.openTenants();

        page(TenantsPage.class)
                .openTenantByName(tenantDTO.getTenantName())
                .openEntitlements();

        var tenantDetailsPage = page(TenantDetailsPage.class);
        var entitlementsPage = page(EntitlementsPage.class);
        var entitlementsPopup = entitlementsPage.pressAddEntitlement();

        AllureReportUtil.info("Checking popup fields");

        SoftAssertions softAssertions = new SoftAssertions();
        softAssertions.assertThat(entitlementsPopup.enabledSwitch.checked())
                .as("Enabled switcher should be 'Yes'")
                .isTrue();
        softAssertions.assertThat(entitlementsPopup.purchasedUnitsForm.isDisplayed())
                .as("Quantity Purchased field should be visible")
                .isTrue();
        softAssertions.assertThat(entitlementsPopup.commentsForm.isDisplayed())
                .as("Comments field should be visible")
                .isTrue();

        softAssertions.assertAll();
        validateDateFields();

        AllureReportUtil.info("Negative test (validate error message for Assessment Units Popup)");

        entitlementsPopup.startDateForm.clear();
        entitlementsPopup.endDateForm.clear();
        entitlementsPopup.pressSaveButton();

        var errorMessages = entitlementsPopup.getErrorMessages();
        softAssertions.assertThat(entitlementsPopup.errorBlockElement.isDisplayed())
                .as("Validate that message displayed").isTrue();
        softAssertions.assertThat(errorMessages.get(0))
                .as("Validate that message equal to: " + expectedQuantityPurchasedError)
                .isEqualTo(expectedQuantityPurchasedError);
        softAssertions.assertThat(errorMessages.get(1))
                .as("Validate that message equal to: " + expectedStartDateError)
                .isEqualTo(expectedStartDateError);
        softAssertions.assertThat(errorMessages.get(2))
                .as("Validate that message equal to: " + expectedEndDateError)
                .isEqualTo(expectedEndDateError);

        AllureReportUtil.info("Creating entitlements");

        entitlementsPopup.setPurchasedQuantity(1000);
        entitlementsPopup.setStartDate(todayStr);
        entitlementsPopup.setEndDate(afterYearStr);
        entitlementsPopup.pressSaveButton();
        refresh();

        AllureReportUtil.info("Check created entitlements. Remember ID of entitlement");

        assertThat(entitlementsPage.entitlementsTable.isEmpty())
                .as("Entitlements table shouldn't be empty")
                .isFalse();
        var defaultEntitlement = new EntitlementCell();
        var assessmentEntId = defaultEntitlement.getId();

        AllureReportUtil.info("Changing of Entitlement model on Scans");

        entitlementsPage.openTenantDetails();
        tenantDetailsPage.setEntitlementModel(FodCustomTypes.EntitlementModel.Scans).pressSave();
        tenantDetailsPage.openEntitlements();

        assertThat(entitlementsPage.entitlementsTable.isEmpty())
                .as("Entitlements page should have no one created entitlement after model changing")
                .isTrue();

        AllureReportUtil.info("Check popup fields for Scans model Entitlements");

        entitlementsPage.pressAddEntitlement();

        softAssertions.assertThat(entitlementsPopup.enabledSwitch.checked())
                .as("Enabled switcher should be 'Yes'")
                .isTrue();
        softAssertions.assertThat(entitlementsPopup.purchasedUnitsForm.isDisplayed())
                .as("Quantity Purchased field should be visible")
                .isTrue();
        softAssertions.assertThat(entitlementsPopup.commentsForm.isDisplayed())
                .as("Comments field should be visible")
                .isTrue();
        softAssertions.assertThat(entitlementsPopup.analysisTypeDropdown.isEnabled())
                .as("Comments field should be visible")
                .isTrue();
        softAssertions.assertThat(entitlementsPopup.assessmentTypeDropDown.isEnabled())
                .as("Comments field should be visible")
                .isTrue();
        softAssertions.assertThat(entitlementsPopup.frequencyTypeDropdown.isEnabled())
                .as("Comments field should be visible")
                .isTrue();

        softAssertions.assertAll();
        validateDateFields();

        AllureReportUtil.info("Changing of Entitlement model on Scans");

        entitlementsPopup.startDateForm.clear();
        entitlementsPopup.endDateForm.clear();
        entitlementsPopup.pressSaveButton();
        errorMessages = entitlementsPopup.getErrorMessages();

        softAssertions.assertThat(entitlementsPopup.errorBlockElement.isDisplayed())
                .as("Validate that message displayed").isTrue();
        softAssertions.assertThat(errorMessages.get(0))
                .as("Validate that message equal to: " + expectedAnalysisTypeError)
                .isEqualTo(expectedAnalysisTypeError);
        softAssertions.assertThat(errorMessages.get(1))
                .as("Validate that message equal to: " + expectedAssessmentTypeError)
                .isEqualTo(expectedAssessmentTypeError);
        softAssertions.assertThat(errorMessages.get(2))
                .as("Validate that message equal to: " + expectedFrequencyError)
                .isEqualTo(expectedFrequencyError);
        softAssertions.assertThat(errorMessages.get(3))
                .as("Validate that message equal to: " + expectedQuantityPurchasedError)
                .isEqualTo(expectedQuantityPurchasedError);
        softAssertions.assertThat(errorMessages.get(4))
                .as("Validate that message equal to: " + expectedStartDateError)
                .isEqualTo(expectedStartDateError);
        softAssertions.assertThat(errorMessages.get(5))
                .as("Validate that message equal to: " + expectedEndDateError)
                .isEqualTo(expectedEndDateError);
        softAssertions.assertAll();

        AllureReportUtil.info("Creating Scans Entitlements");

        entitlementsPopup.setAnalysisType(FodCustomTypes.AnalysisType.Static);
        entitlementsPopup.setAssessmentType("Static Premium");
        entitlementsPopup.setFrequency(FodCustomTypes.ScanFrequency.Subscription);
        entitlementsPopup.setPurchasedQuantity(1000);
        entitlementsPopup.setStartDate(todayStr);
        entitlementsPopup.setEndDate(afterYearStr);
        entitlementsPopup.pressSaveButton();

        assertThat(entitlementsPage.entitlementsTable.isEmpty())
                .as("Entitlements table shouldn't be empty")
                .isFalse();

        var scanEntitlement = new EntitlementCell();
        var scanEntId = scanEntitlement.getId();

        AllureReportUtil.info("Creating Scans Entitlements");

        entitlementsPage.openTenantDetails();
        tenantDetailsPage.setEntitlementModel(FodCustomTypes.EntitlementModel.Units).pressSave();
        tenantDetailsPage.openEntitlements();

        AllureReportUtil.info("Checking that Assessment Units displaying on table again");

        var entitlement = new EntitlementCell();
        boolean assessmentEntPresent = entitlement.getId() == assessmentEntId && entitlement.getId() != scanEntId;

        assertThat(assessmentEntPresent)
                .as("Validate that created entitlements has different ids")
                .isTrue();

        AllureReportUtil.info("Delete created entitlement");

        entitlementsPage.deleteEntitlementById(assessmentEntId);
        var endTime = LocalTime.now().plusMinutes(2);
        do {
            if (entitlementsPage.entitlementsTable.isEmpty()) break;
            sleep(1000);
            refresh();
        }
        while (LocalTime.now().isBefore(endTime));
        assertThat(entitlementsPage.entitlementsTable.isEmpty())
                .as("Entitlements table should be empty after deleting")
                .isTrue();
    }

    private void validateDateFields() {
        var entitlementsPopup = page(AddEditEntitlementPopup.class);
        var startDate = entitlementsPopup.getStartDate();
        var endDate = entitlementsPopup.getEndDate();
        var dateAfterYearPlusDay = dateFormat.format(DateUtils.addDays(afterYear, 1));
        var dateAfterYearMinusDay = dateFormat.format(DateUtils.addDays(afterYear, -1));
        var datePlusDay = dateFormat.format(DateUtils.addDays(today, 1));
        var dateMinusDay = dateFormat.format(DateUtils.addDays(today, -1));
        boolean startDateEnabled = startDate.equals(todayStr) || startDate.equals(datePlusDay) ||
                startDate.equals(dateMinusDay);
        boolean endDateEnabled = endDate.equals(afterYearStr) || endDate.equals(dateAfterYearPlusDay) ||
                endDate.equals(dateAfterYearMinusDay);

        assertThat(startDateEnabled)
                .as("Start date should be equal today's date").isTrue();
        assertThat(endDateEnabled)
                .as("End date should be equal today's date plus year").isTrue();
    }
}
