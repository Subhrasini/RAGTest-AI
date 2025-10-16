package com.fortify.fod.ui.test.regression;

import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.entities.EntitlementDTO;
import com.fortify.fod.common.entities.TenantDTO;
import com.fortify.fod.common.utils.DateTimeUtil;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.EntitlementsActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import com.fortify.fod.ui.test.actions.TenantActions;
import io.qameta.allure.Description;
import org.testng.Assert;
import org.testng.annotations.Test;
import utils.MaxRetryCount;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


public class EntitlementReminderTests extends FodBaseTest {

    TenantDTO tenantDTO;
    String firstReminderDays = "30";
    String editFirstReminderDays = "15";
    String secondReminderDays = "7";
    String fortifyEndDate;
    String sonatypeEndDate;
    String debrickedEndDate;
    String fortifyReminderDate;
    String sonaTypeReminderDate;
    String debrickedReminderDate;
    String inValidEmail = "InvalidEmail";
    String invalidEmailMessage = "Invalid email address: InvalidEmail";
    String setReminder = "Set Reminders";
    String editReminder = "Edit Reminders";
    String reminderMessage = "Reminder will apply to all active entitlements.";
    String deleteMessage = "Are you sure you want to delete the reminder? Reminder will be deleted for all active entitlements.";

    String FirstReminderRequiredMessage = "First Reminder is required";

    String emailToRequired = "Email To is required";

    @Test(groups = {"regression"})
    public void prepareTestData() {
        String startDate = DateTimeUtil.getCurrentDateGMT();
        tenantDTO = TenantDTO.createDefaultInstance();
        TenantActions.createTenant(tenantDTO, true, false);
        EntitlementDTO entitlementDTO;
        entitlementDTO = EntitlementDTO.createDefaultInstance();
        entitlementDTO.setEntitlementType(FodCustomTypes.EntitlementType.FortifyEntitlement);
        entitlementDTO.setStartDate(startDate);
        fortifyEndDate = DateTimeUtil.addDaysToDate(startDate, 60, "MM/dd/yyyy", "MM/dd/yyyy");
        entitlementDTO.setEndDate(fortifyEndDate);
        EntitlementsActions.createEntitlements(entitlementDTO);
        entitlementDTO.setEntitlementType(FodCustomTypes.EntitlementType.SonatypeEntitlement);
        sonatypeEndDate = DateTimeUtil.addDaysToDate(startDate, 90, "MM/dd/yyyy", "MM/dd/yyyy");
        entitlementDTO.setEndDate(sonatypeEndDate);
        EntitlementsActions.createEntitlements(entitlementDTO);
        entitlementDTO.setEntitlementType(FodCustomTypes.EntitlementType.DebrickedEntitlement);
        debrickedEndDate = DateTimeUtil.addDaysToDate(startDate, 120, "MM/dd/yyyy", "MM/dd/yyyy");
        entitlementDTO.setEndDate(debrickedEndDate);
        EntitlementsActions.createEntitlements(entitlementDTO);

        sonaTypeReminderDate = DateTimeUtil.addDaysToDate(sonatypeEndDate, -30, "MM/dd/yyyy", "yyyy/MM/dd");
        debrickedReminderDate = DateTimeUtil.addDaysToDate(debrickedEndDate, -30, "MM/dd/yyyy", "yyyy/MM/dd");
        fortifyReminderDate = DateTimeUtil.addDaysToDate(fortifyEndDate, -30, "MM/dd/yyyy", "yyyy/MM/dd");
    }

    @MaxRetryCount(2)
    @Description("Checks the create, update  and delete of the Reminder ")
    @Test(groups = {"regression"}, dependsOnMethods = "prepareTestData")
    public void entitlementReminderTest() {
        AllureReportUtil.info("Login in the tenant and Navigating to the Administration-> Entitlement");

        var entitlementPage = LogInActions.tamUserLogin(tenantDTO)
                .tenantTopNavbar.openAdministration().openEntitlements();

        AllureReportUtil.info("Checking the first Reminder field is mandatory.");
        assertThat(entitlementPage.checkFields("", tenantDTO.getUserEmail())
        ).as("The First reminder text box is a mandatory parameter").isEqualTo(FirstReminderRequiredMessage);

        AllureReportUtil.info("Checking the email field is mandatory.");
        assertThat(entitlementPage.checkFields("7", "")
        ).as("The email box is a mandatory parameter").isEqualTo(emailToRequired);

        AllureReportUtil.info("Checking the email field format check.");
        assertThat(entitlementPage.checkFields("7", inValidEmail)
        ).as("The email should have the correct format").isEqualTo(invalidEmailMessage);

        AllureReportUtil.info("Checking the SET REMINDERS button on all types of entitlement");
        assertThat(entitlementPage
                .switchEntitlementsTab(FodCustomTypes.EntitlementType.DebrickedEntitlement)
                .getReminderButtonText())
                .as("The debricked entitlement page should show the SET REMINDERS button")
                .isEqualTo(setReminder);
        assertThat(entitlementPage
                .switchEntitlementsTab(FodCustomTypes.EntitlementType.SonatypeEntitlement)
                .getReminderButtonText())
                .as("The sonatype entitlement page should show the SET REMINDERS button")
                .isEqualTo(setReminder);
        assertThat(entitlementPage
                .switchEntitlementsTab(FodCustomTypes.EntitlementType.FortifyEntitlement)
                .getReminderButtonText())
                .as("The fortify entitlement page should show SET REMINDERS button")
                .isEqualTo(setReminder);

        AllureReportUtil.info("Creating an entitlement reminder reminder");
        assertThat(entitlementPage
                .createReminder(firstReminderDays, tenantDTO.getUserEmail()))
                .as("Checking the message after creation of the entitlement reminder")
                .isEqualTo(reminderMessage);

        AllureReportUtil.info("Checking that reminder date column is getting the correct value for each entitlement type");
        assertThat(entitlementPage
                .switchEntitlementsTab(FodCustomTypes.EntitlementType.DebrickedEntitlement)
                .getReminderDate())
                .as("The debricked entitlement reminder date should be correctly set")
                .isEqualTo(debrickedReminderDate);
        assertThat(entitlementPage
                .switchEntitlementsTab(FodCustomTypes.EntitlementType.SonatypeEntitlement)
                .getReminderDate())
                .as("The sonatype entitlement reminder date should be correctly set")
                .isEqualTo(sonaTypeReminderDate);
        assertThat(entitlementPage
                .switchEntitlementsTab(FodCustomTypes.EntitlementType.FortifyEntitlement)
                .getReminderDate())
                .as("The fortify entitlement reminder date should be correctly set")
                .isEqualTo(fortifyReminderDate);


        AllureReportUtil.info("Checking that entitlement pages ,the SET REMINDER BUTTON is changes to EDIT REMINDER");
        assertThat(entitlementPage
                .switchEntitlementsTab(FodCustomTypes.EntitlementType.DebrickedEntitlement)
                .getReminderButtonText())
                .as("The debricked entitlement page should show the EDIT REMINDERS button")
                .isEqualTo(editReminder);
        assertThat(entitlementPage
                .switchEntitlementsTab(FodCustomTypes.EntitlementType.SonatypeEntitlement)
                .getReminderButtonText())
                .as("The sonatype entitlement page should show the EDIT REMINDERS button")
                .isEqualTo(editReminder);
        assertThat(entitlementPage
                .switchEntitlementsTab(FodCustomTypes.EntitlementType.FortifyEntitlement)
                .getReminderButtonText())
                .as("The fortify entitlement page should show EDIT REMINDERS button")
                .isEqualTo(editReminder);


        AllureReportUtil.info("Editing the Reminder");
        assertThat(entitlementPage.editReminder(editFirstReminderDays, secondReminderDays))
                .as("After editing correct message should be displayed")
                .isEqualTo(reminderMessage);

        fortifyReminderDate = DateTimeUtil.addDaysToDate(fortifyEndDate, -Integer.parseInt(editFirstReminderDays), "MM/dd/yyyy", "yyyy/MM/dd");
        sonaTypeReminderDate = DateTimeUtil.addDaysToDate(sonatypeEndDate, -Integer.parseInt(editFirstReminderDays), "MM/dd/yyyy", "yyyy/MM/dd");
        debrickedReminderDate = DateTimeUtil.addDaysToDate(debrickedEndDate, -Integer.parseInt(editFirstReminderDays), "MM/dd/yyyy", "yyyy/MM/dd");

        AllureReportUtil.info("The reminder date should change after the edit");
        assertThat(entitlementPage
                .switchEntitlementsTab(FodCustomTypes.EntitlementType.DebrickedEntitlement)
                .getReminderDate())
                .as("The reminder date for debricked entitlement should get changed as per the modification.")
                .isEqualTo(debrickedReminderDate);
        assertThat(entitlementPage
                .switchEntitlementsTab(FodCustomTypes.EntitlementType.SonatypeEntitlement)
                .getReminderDate())
                .as("The reminder date for sonatype entitlement should get changed as per the modification.")
                .isEqualTo(sonaTypeReminderDate);
        assertThat(entitlementPage
                .switchEntitlementsTab(FodCustomTypes.EntitlementType.FortifyEntitlement)
                .getReminderDate())
                .as("The reminder date for fortify entitlement should get changed as per the modification.")
                .isEqualTo(fortifyReminderDate);

        AllureReportUtil.info("Deleting the reminder");
        assertThat(entitlementPage.deleteReminder())
                .as("There should a message on deleting the reminder")
                .isEqualTo(deleteMessage);

        AllureReportUtil.info("Check after deleting the reminder the reminder column is now empty.");
        assertThat(entitlementPage.switchEntitlementsTab(FodCustomTypes.EntitlementType.DebrickedEntitlement, true).getReminderDate())
                .as("The debricked page should have the Reminder column empty")
                .isEmpty();
        assertThat(entitlementPage.switchEntitlementsTab(FodCustomTypes.EntitlementType.SonatypeEntitlement).getReminderDate())
                .as("The debricked page should have the Reminder column empty")
                .isEmpty();
        assertThat(entitlementPage.switchEntitlementsTab(FodCustomTypes.EntitlementType.FortifyEntitlement).getReminderDate())
                .as("The debricked page should have the Reminder column empty")
                .isEmpty();
    }
}