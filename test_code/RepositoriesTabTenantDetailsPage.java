package com.fortify.fod.ui.test.regression;

import com.fortify.fod.common.elements.ModalDialog;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.LogInActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.util.ArrayList;

import static com.codeborne.selenide.WebDriverRunner.source;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("svpillai@opentext.com")
@Slf4j
public class RepositoriesTabTenantDetailsPage extends FodBaseTest {

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Repositories tab contains Repository Type, Repository ID and Record Count columns. Validate Health check ,Rebuild , delete buttons are enabled . Validate Repository column should contain all defined types")
    @Test(groups = "{regression}")

    public void validateRepositoryTab() {

        var repositoriesPage = LogInActions.adminLogIn().adminTopNavbar.openTenants().openTenantByName(defaultTenantDTO.getTenantName()).openRepositories();

        var expectedRepositoryColumns = new ArrayList<String>() {{
            add("Repository Type");
            add("Repository ID");
            add("Record Count");
            add("");
        }};

        var expectedRepositoryTypes = new ArrayList<String>() {{
            add("Application");
            add("Release");
            add("Scan");
            add("Vulnerability");
            add("Vulnerabilty Pre-Publish");
        }};

        assertThat(repositoriesPage.table.getColumnHeaders()).as("Repositories tab should contain Repository Type, Repository ID and Record Count columns").hasSameElementsAs(expectedRepositoryColumns);

        assertThat(repositoriesPage.deleteBtn.isEnabled()).as("Validate that delete button is enabled").isTrue();
        assertThat(repositoriesPage.healthCheckBtn.isEnabled()).as("Validate that Health check  button is enabled").isTrue();
        assertThat(repositoriesPage.rebuildBtn.isEnabled()).as("Validate that Rebuild button is enabled").isTrue();

        assertThat(repositoriesPage.getRepositoryTypes()).as("Repository Type column should contain all defined types").hasSameElementsAs(expectedRepositoryTypes);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Owner("sbehera3@opentext.com")
    @FodBacklogItem("762037")
    @Description("Validate all references to ElasticReadStore are removed")
    @Test(groups = "{regression}")

    public void validateElasticReadStoreTest() {

        var repositoriesPage = LogInActions.adminLogIn().adminTopNavbar.openTenants()
                .openTenantByName(defaultTenantDTO.getTenantName()).openRepositories();
        assertThat(repositoriesPage.getTitle())
                .as("Validate Repositories page is showing correctly")
                .isEqualTo("Repositories");
        assertThat(source())
                .as("Validate that UI is not displaying any references to Elastic ReadStore")
                .doesNotContain("Elastic");
        var modal = new ModalDialog("Alert");
        repositoriesPage.getAllRepositories()
                .forEach(x -> {
                    x.clickRebuild().pressYes();
                    modal.waitForModalVisible();
                    assertThat(modal.isVisible())
                            .as("Validate that all repositories can be rebuild without any errors").isFalse();
                });
    }

}
