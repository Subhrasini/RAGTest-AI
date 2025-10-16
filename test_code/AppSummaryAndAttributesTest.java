package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.BrowserUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.elements.ModalDialog;
import com.fortify.fod.common.entities.ApplicationDTO;
import com.fortify.fod.common.entities.AttributeDTO;
import com.fortify.fod.common.entities.TenantDTO;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.CustomAttributesActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import com.fortify.fod.ui.test.actions.TenantActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.MaxRetryCount;

import java.time.Duration;
import java.util.HashMap;

import static com.codeborne.selenide.Selenide.refresh;
import static com.codeborne.selenide.Selenide.sleep;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("ysmal@opentext.com")
@Slf4j
public class AppSummaryAndAttributesTest extends FodBaseTest {

    AttributeDTO text, bool, date, user, picklist;
    HashMap<AttributeDTO, String> attributesMap;
    TenantDTO tenantDTO;
    ApplicationDTO applicationDTO;

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Create tenant/custom attributes/application for test execution")
    @Test(groups = {"regression"})
    public void testDataPreparation() {
        tenantDTO = TenantDTO.createDefaultInstance();

        TenantActions.createTenant(tenantDTO);
        BrowserUtil.clearCookiesLogOff();

        text = AttributeDTO.createDefaultInstance();
        bool = AttributeDTO.createDefaultInstance();
        date = AttributeDTO.createDefaultInstance();
        user = AttributeDTO.createDefaultInstance();
        picklist = AttributeDTO.createDefaultInstance();

        text.setAttributeDataType(FodCustomTypes.AttributeDataType.Text);
        bool.setAttributeDataType(FodCustomTypes.AttributeDataType.Boolean);
        date.setAttributeDataType(FodCustomTypes.AttributeDataType.Date);
        user.setAttributeDataType(FodCustomTypes.AttributeDataType.User);
        picklist.setAttributeDataType(FodCustomTypes.AttributeDataType.Picklist);
        picklist.setRequired(true);
        picklist.setPickListValues(new String[]{"XX", "QQ", "VV", "ZZ"});

        CustomAttributesActions.createCustomAttribute(text, tenantDTO, true);
        CustomAttributesActions.createCustomAttribute(bool);
        CustomAttributesActions.createCustomAttribute(date);
        CustomAttributesActions.createCustomAttribute(user);
        CustomAttributesActions.createCustomAttribute(picklist);

        attributesMap = new HashMap<>() {{
            put(text, "Test Text");
            put(bool, "True");
            put(user, tenantDTO.getUserName());
            put(picklist, "QQ");
            put(date, "25");
        }};
        applicationDTO = ApplicationDTO.createDefaultInstance();
        applicationDTO.setAttributesMap(attributesMap);

        ApplicationActions.createApplication(applicationDTO, tenantDTO, false);
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate application attributes page")
    @Test(groups = {"regression"}, dependsOnMethods = {"testDataPreparation"}, priority = 1)
    public void appAttributesTest() {
        var attributesPage = LogInActions.tamUserLogin(tenantDTO)
                .openDetailsFor(applicationDTO.getApplicationName())
                .openSettings().openAppAttributesTab();
        assertThat(attributesPage.tabs.getActiveTab()).isEqualTo("Application Attributes");

        for (var attr : attributesMap.entrySet()) {
            if (attr.getKey().getAttributeDataType().equals(FodCustomTypes.AttributeDataType.Date))
                assertThat(attributesPage.getAttributeValue(attr.getKey()))
                        .contains(attr.getValue());
            else
                assertThat(attributesPage.getAttributeValue(attr.getKey()))
                        .isEqualTo(attr.getValue());
        }
    }

    @MaxRetryCount(1)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate application summary page")
    @Test(groups = {"regression"}, dependsOnMethods = {"testDataPreparation"}, priority = 2)
    public void appSummaryTest() {
        var newAppName = "NewAppName";
        var appSummaryPage = LogInActions.tamUserLogin(tenantDTO)
                .openDetailsFor(applicationDTO.getApplicationName())
                .openSettings().openAppSummaryTab();
        assertThat(appSummaryPage.tabs.getActiveTab()).isEqualTo("Application Summary");

        applicationDTO.setApplicationName(newAppName);
        appSummaryPage.setApplicationName(applicationDTO.getApplicationName());
        appSummaryPage.setDescription("Some Description");
        appSummaryPage.setBusinessCriticality(FodCustomTypes.BusinessCriticality.Medium);
        appSummaryPage.setEmail("a@b.cc");

        var validationMessage = appSummaryPage.pressSave().getValidationMessage();
        assertThat(validationMessage).isEqualTo("Settings saved successfully.");

        appSummaryPage.setEmail("Email@").pressSave();
        var modal = new ModalDialog();
        assertThat(modal.getMessage())
                .isEqualTo("One or more Additional Emails are invalid. " +
                        "Multiple email addresses must be separated by a semicolon or comma.");
        modal.pressClose();

        var apps = appSummaryPage.tenantTopNavbar.openApplications().getAllApps().get(0);
        if (!apps.getName().equals(newAppName)) {
            sleep(Duration.ofMinutes(1).toMillis());
            refresh();
            apps = appSummaryPage.tenantTopNavbar.openApplications().getAllApps().get(0);
        }

        assertThat(apps.getName()).isEqualTo(applicationDTO.getApplicationName());
    }
}