package com.fortify.fod.ui.test.regression;

import com.fortify.fod.ui.pages.tenant.TenantLoginPage;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.SiteSettingsActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("sbehera3@opentext.com")
@Slf4j
@FodBacklogItem("517003")
public class AddLinkToTenantPortalTest extends FodBaseTest {

    public String settingName = "CsaStarReferenceSnippet";
    public String settingValue = "<li><a href=\"https://cloudsecurityalliance.org/star/registry/services/fortify-on-demand\" target=\"_blank\">CSA STAR Level 1 Registry" + "</a></li>";
    public String linkText = "CSA STAR Level 1 Registry";
    public String link = "https://cloudsecurityalliance.org/star/registry/services/fortify-on-demand";
    public String linkText2 = "Micro Focus Security Fortify";

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Set CsaStarReferenceSnippet flag in admin SiteSettingsPage")
    @Test(groups = {"hf", "regression"})
    public void setCsaStarReferenceSnippetFlag() {
        SiteSettingsActions.setValueInSettings(settingName, settingValue, true);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify CSA STAR Self-Assessment Link on FOD Login Portal - Site Setting: ON ")
    @Test(dependsOnMethods = {"setCsaStarReferenceSnippetFlag"}, groups = {"hf", "regression"})
    public void verifyCSASTARSelfAssessmentLinkONTest() {
        // Wait for some time for the changes to reflect
        var csaSTARLink = TenantLoginPage.navigate().waitForLink(linkText, true).getContainerFluidLink(linkText);
        csaSTARLink.scrollIntoView(false);
        assertThat(csaSTARLink.isDisplayed())
                .as("Verify " + linkText + "is displayed at the bottom of the tenant portal").isTrue();
        assertThat(csaSTARLink.isEnabled())
                .as("Verify " + linkText + "is enabled at the bottom of the tenant portal").isTrue();
        assertThat(csaSTARLink.getAttribute("href"))
                .as("Verify the link").isEqualTo(link);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Remove the value in CsaStarReferenceSnippet flag in admin SiteSettingsPage")
    @Test(dependsOnMethods = {"verifyCSASTARSelfAssessmentLinkONTest"}, groups = {"hf", "regression"},
            alwaysRun = true)
    public void disableCsaStarReferenceSnippetFlag() {
        SiteSettingsActions.setValueInSettings(settingName, "", true);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify CSA STAR Self-Assessment Link on FOD Login Portal - Site Setting: OFF ")
    @Test(dependsOnMethods = {"disableCsaStarReferenceSnippetFlag"}, groups = {"hf", "regression"},
            enabled = false)
    public void verifyCSASTARSelfAssessmentLinkOFFTest() {
        // Wait for some time for the changes to reflect
        var tenantLogInPage = TenantLoginPage.navigate().waitForLink(linkText, false);
        tenantLogInPage.getContainerFluidLink(linkText2).scrollIntoView(false);
        assertThat(tenantLogInPage.getContainerFluidLink(linkText).exists())
                .as("Verify " + linkText + "doesn't exist at the bottom of the tenant portal").isFalse();
    }
}
