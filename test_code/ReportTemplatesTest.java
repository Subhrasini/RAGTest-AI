package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.UniqueRunTag;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.entities.ReportTemplateDTO;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.LogInActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("dgochev@opentext.com")
public class ReportTemplatesTest extends FodBaseTest {

    ReportTemplateDTO template = ReportTemplateDTO.createDefaultInstance();
    String previousName;
    String systemTemplateName = "Continuous Application Monitoring";

    @BeforeMethod
    public void login() {
        LogInActions.tamUserLogin(FodConfig.TAM_USER_NAME, FodConfig.TAM_PASSWORD, "AUTO-TENANT");
    }

    @Severity(SeverityLevel.NORMAL)
    @MaxRetryCount(3)
    @Description("Create new template")
    @Test(groups = {"regression"})
    public void createTemplateTest() {
        template.setName("FLASH-Template-" + UniqueRunTag.generate());
        var templatesPage = new TenantTopNavbar()
                .openReports().openTemplates().createTemplate(template);
        previousName = template.getName(); //for retries of editTemplateTest

        assertThat(template.getName())
                .as("Verify that template is in template list").isIn(templatesPage.getAllTemplateNames());
    }

    @Severity(SeverityLevel.NORMAL)
    @MaxRetryCount(3)
    @Description("Edit template")
    @Test(dependsOnMethods = "createTemplateTest", groups = {"regression"})
    public void editTemplateTest() {
        template.setName("FLASH-Template-Edited" + UniqueRunTag.generate());
        var templatesPage = new TenantTopNavbar()
                .openReports().openTemplates().editTemplateByName(previousName, template);

        assertThat(template.getName()).as("Verify that edited template is in template list")
                .isIn(templatesPage.getAllTemplateNames());
        assertThat(previousName).as("Verify that previous template is not in template list")
                .isNotIn(templatesPage.getAllTemplateNames());
    }

    @Severity(SeverityLevel.NORMAL)
    @MaxRetryCount(3)
    @Description("View template")
    @Test(dependsOnMethods = "editTemplateTest", groups = {"regression"})
    public void viewTemplateTest() {
        var templatePopup = new TenantTopNavbar()
                .openReports().openTemplates().viewTemplateByName(template.getName());

        assertThat(template.getName())
                .as("Compare template name").isEqualTo(templatePopup.getName());
        assertThat(template.getScanTypes())
                .as("Compare template scan types").isEqualTo(templatePopup.getScanTypes());
        assertThat(template.getSeverity())
                .as("Compare template severities").isEqualTo(templatePopup.getSeverities());
        assertThat(template.getModules())
                .as("Compare template modules").isEqualTo(templatePopup.getModules());
    }

    @Severity(SeverityLevel.NORMAL)
    @MaxRetryCount(3)
    @Description("Copy template")
    @Test(dependsOnMethods = "viewTemplateTest", groups = {"regression"})
    public void copyTemplateTest() {

        var templatesPage = new TenantTopNavbar().openReports().openTemplates();

        if (templatesPage.getAllTemplateNames().contains(systemTemplateName.concat(" (copy)")))
            templatesPage.deleteTemplateByName(systemTemplateName.concat(" (copy)"));

        templatesPage.copyTemplateByName(systemTemplateName);

        assertThat(systemTemplateName.concat(" (copy)")).as("Verify that template is in template list")
                .isIn(templatesPage.getAllTemplateNames());
    }

    @Severity(SeverityLevel.NORMAL)
    @MaxRetryCount(3)
    @Description("Delete template")
    @Test(dependsOnMethods = "copyTemplateTest", groups = {"regression"})
    public void deleteTemplateTest() {
        var templatesPage = new TenantTopNavbar()
                .openReports().openTemplates().deleteTemplateByName(template.getName())
                .deleteTemplateByName(systemTemplateName.concat(" (copy)"));

        assertThat(template.getName()).as("Verify that template is not in template list")
                .isNotIn(templatesPage.getAllTemplateNames());
    }

    @Severity(SeverityLevel.NORMAL)
    @MaxRetryCount(3)
    @Description("Suppress template")
    @Test(groups = {"regression"})
    public void suppressTemplateTest() {
        var templatesPage = new TenantTopNavbar()
                .openReports().openTemplates().deleteTemplateByName(systemTemplateName);

        assertThat(templatesPage.isRestoreButtonVisible(systemTemplateName))
                .as("Verify that template is suppressed").isTrue();
    }

    @Severity(SeverityLevel.NORMAL)
    @MaxRetryCount(3)
    @Description("Restore template")
    @Test(dependsOnMethods = "suppressTemplateTest", groups = {"regression"})
    public void restoreTemplateTest() {
        var templatesPage = new TenantTopNavbar()
                .openReports().openTemplates().restoreTemplateByName(systemTemplateName);

        assertThat(templatesPage.isRestoreButtonVisible(systemTemplateName))
                .as("Verify that template is restored").isFalse();
    }
}
