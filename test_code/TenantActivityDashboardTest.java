package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.WaitUtil;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.elements.Table;
import com.fortify.fod.common.entities.AdminUserDTO;
import com.fortify.fod.common.entities.TenantDTO;
import com.fortify.fod.ui.pages.tenant.applications.your_applications.YourApplicationCell;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.AdminUserActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import com.fortify.fod.ui.test.actions.Ordering;
import com.fortify.fod.ui.test.actions.TenantActions;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.fortify.common.utils.WaitUtil.waitFor;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("oradchenko@opentext.com")
@Slf4j
public class TenantActivityDashboardTest extends FodBaseTest {

    AdminUserDTO tamUser;
    TenantDTO tenantRankA;
    TenantDTO tenantRankB;
    TenantDTO tenantRankC;
    TenantDTO tenantWithDemoStatus;
    TenantDTO tenantWithProofOfValueStatus;
    TenantDTO tenantWithInactiveStatus;

    @MaxRetryCount(3)
    @Description("Create TAM and necessary tenants")
    @Test(groups = {"regression"})
    public void prepareTestData() {
        tamUser = AdminUserDTO.createInstanceWithUserRole(FodCustomTypes.AdminUserRole.TAM);
        tenantRankA = TenantDTO.createDefaultInstance();
        tenantRankA.setCustomerRank(FodCustomTypes.CustomerRank.A);
        tenantRankA.setOpportunityClass(FodCustomTypes.OpportunityClass.Gold);
        tenantRankA.setAssignedUser(tamUser.getUserName());

        tenantRankB = TenantDTO.createDefaultInstance();
        tenantRankB.setCustomerRank(FodCustomTypes.CustomerRank.B);
        tenantRankB.setOpportunityClass(FodCustomTypes.OpportunityClass.Silver);

        tenantRankC = TenantDTO.createDefaultInstance();
        tenantRankC.setCustomerRank(FodCustomTypes.CustomerRank.C);
        tenantRankC.setOpportunityClass(FodCustomTypes.OpportunityClass.Bronze);
        tenantRankC.setAssignedUser(tamUser.getUserName());

        tenantWithDemoStatus = TenantDTO.createDefaultInstance();
        tenantWithDemoStatus.setDemoTenant(true);
        tenantWithDemoStatus.setAssignedUser(tamUser.getUserName());

        tenantWithProofOfValueStatus = TenantDTO.createDefaultInstance();
        tenantWithProofOfValueStatus.setProofOfValue(true);
        tenantWithProofOfValueStatus.setAssignedUser(tamUser.getUserName());

        tenantWithInactiveStatus = TenantDTO.createDefaultInstance();
        tenantWithInactiveStatus.setInactive(true);


        AdminUserActions.createAdminUser(tamUser, true);
        TenantActions.createTenants(
                false,
                tenantRankA,
                tenantRankB,
                tenantRankC,
                tenantWithDemoStatus,
                tenantWithProofOfValueStatus,
                tenantWithInactiveStatus);
    }

    @MaxRetryCount(3)
    @Description("Only tenants TAM user is assigned to should be visible for TAM")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void tenantActivityDashboardTestWithTamUser() {
        var page = LogInActions.adminUserLogIn(tamUser)
                .adminTopNavbar.openTenants().openTenantActivity();
        page.getTable().waitForEntryAppear(tenantRankA.getTenantName(), 0, 15);
        page.getTable().waitForEntryAppear(tenantRankC.getTenantName(), 0, 15);
        var tenants = page.getAllTenants()
                .stream().map(tenant -> tenant.getName()).collect(Collectors.toList());

        assertThat(tenants).isNotEmpty();

        assertThat(tenants).as("Tenants should not be in Tenant Activity").doesNotContain(
                        tenantRankB.getTenantCode(),
                        tenantWithDemoStatus.getTenantCode(),
                        tenantWithInactiveStatus.getTenantCode(),
                        tenantWithProofOfValueStatus.getTenantCode()).as("Tenants should be in Tenant Activity").
                contains(tenantRankA.getTenantCode(), tenantRankC.getTenantCode());
    }

    @MaxRetryCount(3)
    @Description("All tenants should be visible for admin user, all features should match expected")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void tenantActivityDashboardTestWithAdminUser() {
        var expectedColumns = List.of(new String[]{
                "TENANT NAME",
                "STATIC",
                "DYNAMIC",
                "MOBILE",
                "LAST STARTED",
                "LAST LOGIN",
                "RECENT CHALLENGES",
                "ENTITLEMENT CONSUMPTION",
                "EXPIRING ENTITLEMENTS",
                "CUSTOMER RANK",
                "OPPORTUNITY CLASS"
        });

        var page = LogInActions.adminLogIn()
                .adminTopNavbar.openTenants().openTenantActivity();
        page.getPaging().setRecordsPerPage(1000);

        page.findWithSearchBox(tenantRankA.getTenantName())
                .getTable().waitForEntryAppear(tenantRankA.getTenantName(), 0, 15);
        page.findWithSearchBox(tenantRankB.getTenantName())
                .getTable().waitForEntryAppear(tenantRankB.getTenantName(), 0, 15);
        page.findWithSearchBox(tenantRankC.getTenantName())
                .getTable().waitForEntryAppear(tenantRankC.getTenantName(), 0, 15);

        page.clearSearchBox();

        var tenant = page.getCellByTenantName(tenantRankA.getTenantName()).getName();
        assertThat(tenant).as("Check if tenant with Rank A was found").isEqualTo(tenantRankA.getTenantName());

        page.findWithSearchBox(tenantRankB.getTenantName());
        tenant = page.getCellByTenantName(tenantRankB.getTenantName()).getName();
        assertThat(tenant).as("Check if tenant with Rank B was found").isEqualTo(tenantRankB.getTenantName());

        page.findWithSearchBox(tenantRankC.getTenantName());
        tenant = page.getCellByTenantName(tenantRankC.getTenantName()).getName();
        assertThat(tenant).as("Check if tenant with Rank C was found").isEqualTo(tenantRankC.getTenantName());

        page.findWithSearchBox(tenantWithDemoStatus.getTenantName());
        assertThat(Table.isEmpty())
                .as("Tenant with Demo Status should not be found").isTrue();

        page.findWithSearchBox(tenantWithProofOfValueStatus.getTenantName());
        assertThat(Table.isEmpty())
                .as("Tenant with Proof of Value Status should not be found").isTrue();

        page.findWithSearchBox(tenantWithInactiveStatus.getTenantName());
        assertThat(Table.isEmpty())
                .as("Tenant with Inactive Status should not be found").isTrue();

        page.clearSearchBox();

        var actualColumns = page.getTable().columns.texts();
        assertThat(actualColumns.size()).isEqualTo(expectedColumns.size());
        for (int i = 0; i < expectedColumns.size(); i++) {
            assertThat(actualColumns.get(i)).as("Verify columns order").isEqualToIgnoringCase(actualColumns.get(i));
        }

        // Check filters

        var tenantNames = new ArrayList<String>() {
            {
                add(tenantRankB.getTenantName());
                add(tenantRankC.getTenantName());
                add(tenantWithDemoStatus.getTenantName());
                add(tenantWithProofOfValueStatus.getTenantName());
                add(tenantWithInactiveStatus.getTenantName());
            }
        };

        // Rank A
        page.appliedFilters.clearAll();
        page.filters.expandAllFilters().setFilterByName("Customer Rank").clickFilterCheckboxByName("A").apply();
        page.findWithSearchBox(tenantRankA.getTenantName());
        tenant = page.getCellByTenantName(tenantRankA.getTenantName()).getName();
        assertThat(tenant).as("Check if tenant with Rank A was visible for filter Rank A")
                .isEqualTo(tenantRankA.getTenantName());

        for (var notPresentTenant : tenantNames) {
            page.findWithSearchBox(notPresentTenant);
            assertThat(Table.isEmpty())
                    .as("Tenant with should not visible for Rank A").isTrue();
        }

        tenantNames.remove(tenantRankB.getTenantName());
        tenantNames.add(tenantRankA.getTenantName());
        ;
        page.appliedFilters.clearAll();
        page.filters.expandAllFilters().setFilterByName("Customer Rank").clickFilterCheckboxByName("B").apply();
        page.findWithSearchBox(tenantRankB.getTenantName());
        tenant = page.getCellByTenantName(tenantRankB.getTenantName()).getName();
        assertThat(tenant).as("Check if tenant with Rank B was visible for filter Rank B")
                .isEqualTo(tenantRankB.getTenantName());

        for (var notPresentTenant : tenantNames) {
            page.findWithSearchBox(notPresentTenant);
            assertThat(Table.isEmpty())
                    .as("Tenant with should not visible for Rank B").isTrue();
        }

        // Rank C
        tenantNames.remove(tenantRankC.getTenantName());
        tenantNames.add(tenantRankB.getTenantName());

        page.appliedFilters.clearAll();
        page.filters.expandAllFilters().setFilterByName("Customer Rank").clickFilterCheckboxByName("C").apply();
        page.findWithSearchBox(tenantRankC.getTenantName());
        tenant = page.getCellByTenantName(tenantRankC.getTenantName()).getName();
        assertThat(tenant).as("Check if tenant with Rank C was visible for filter Rank C").isEqualTo(tenantRankC.getTenantName());

        for (var notPresentTenant : tenantNames) {
            page.findWithSearchBox(notPresentTenant);
            assertThat(Table.isEmpty())
                    .as("Tenant with should not visible for Rank C").isTrue();
        }

        // Opportunity class Gold
        tenantNames.remove(tenantRankA.getTenantName());
        tenantNames.add(tenantRankC.getTenantName());

        page.appliedFilters.clearAll();
        page.filters.expandAllFilters().setFilterByName("Opportunity Class").clickFilterCheckboxByName("Gold").apply();
        page.findWithSearchBox(tenantRankA.getTenantName());
        tenant = page.getCellByTenantName(tenantRankA.getTenantName()).getName();
        assertThat(tenant).as("Check if tenant with Rank A (Gold) was visible for filter Opportunity class Gold")
                .isEqualTo(tenantRankA.getTenantName());

        for (var notPresentTenant : tenantNames) {
            page.findWithSearchBox(notPresentTenant);
            assertThat(Table.isEmpty())
                    .as("Tenant with should not visible for Opportunity class Gold").isTrue();
        }

        // Opportunity class Silver
        tenantNames.remove(tenantRankB.getTenantName());
        tenantNames.add(tenantRankA.getTenantName());

        page.appliedFilters.clearAll();
        page.filters.expandAllFilters().setFilterByName("Opportunity Class").clickFilterCheckboxByName("Silver").apply();
        page.findWithSearchBox(tenantRankB.getTenantName());
        tenant = page.getCellByTenantName(tenantRankB.getTenantName()).getName();
        assertThat(tenant).as("Check if tenant with Rank B (Silver) was visible for filter Opportunity class Silver")
                .isEqualTo(tenantRankB.getTenantName());

        for (var notPresentTenant : tenantNames) {
            page.findWithSearchBox(notPresentTenant);
            assertThat(Table.isEmpty())
                    .as("Tenant with should not visible for Opportunity class Silver").isTrue();
        }

        // Opportunity class Bronze
        tenantNames.remove(tenantRankC.getTenantName());
        tenantNames.add(tenantRankB.getTenantName());

        page.appliedFilters.clearAll();
        page.filters.expandAllFilters().setFilterByName("Opportunity Class").clickFilterCheckboxByName("Bronze").apply();
        page.findWithSearchBox(tenantRankC.getTenantName());
        tenant = page.getCellByTenantName(tenantRankC.getTenantName()).getName();
        assertThat(tenant).as("Check if tenant with Rank B (Silver) was visible for filter Opportunity class Silver")
                .isEqualTo(tenantRankC.getTenantName());

        for (var notPresentTenant : tenantNames) {
            page.findWithSearchBox(notPresentTenant);
            assertThat(Table.isEmpty())
                    .as("Tenant with should not visible for Opportunity class Bronze").isTrue();
        }

        page.appliedFilters.clearAll();

        var ordering = new Ordering(page.getTable());
        ordering.verifyOrderForColumn(expectedColumns.get(0));
        for (int i = 4; i < expectedColumns.size(); i++) {
            ordering.verifyOrderForColumn(expectedColumns.get(i));
        }
    }

    @Owner("kbadia@opentext.com")
    @MaxRetryCount(3)
    @FodBacklogItem("777072")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify auto-populate sample scans for POV tenants")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void validatePOVTenantsTest() {
        var yourApplicationsPage = LogInActions.tamUserLogin(
                        tenantWithProofOfValueStatus.getAssignedUser(), FodConfig.TAM_PASSWORD,
                        tenantWithProofOfValueStatus.getTenantCode()).tenantTopNavbar
                .openApplications();
        waitFor(WaitUtil.Operator.Equals, false, Table::isEmpty, Duration.ofMinutes(5), true);
        waitFor(WaitUtil.Operator.Equals, 8, yourApplicationsPage.getTable()::getRowsCount,
                Duration.ofMinutes(5), true);
        var apps = yourApplicationsPage.getAllApps();
        assertThat(apps)
                .as("Your Applications Page should contain few dummy apps and scans")
                .isNotEmpty();
        yourApplicationsPage.filters.setFilterByName("Application type").expand().clickFilterOptionByName("Web");
        assertThat(yourApplicationsPage.getAllApps().stream().map(YourApplicationCell::getName)
                .collect(Collectors.toList()))
                .as("Your Applications Page should contain some dummy web apps")
                .isNotEmpty();
        yourApplicationsPage.appliedFilters.clearAll();
        yourApplicationsPage.filters.setFilterByName("Application type").expand().clickFilterOptionByName("Mobile");
        assertThat(yourApplicationsPage.getAllApps().stream().map(YourApplicationCell::getName)
                .collect(Collectors.toList()))
                .as("Your Applications Page should contain some dummy mobile apps")
                .isNotEmpty();
        yourApplicationsPage.appliedFilters.clearAll();
        yourApplicationsPage.filters.setFilterByName("Scan Type").expand().clickFilterOptionByName("Dynamic");
        assertThat(yourApplicationsPage.getAllApps().stream().map(YourApplicationCell::getName)
                .collect(Collectors.toList()))
                .as("Your Applications Page should contain some dummy Dynamic app scans")
                .isNotEmpty();

        yourApplicationsPage.appliedFilters.clearAll();
        yourApplicationsPage.filters.setFilterByName("Scan Type").expand().clickFilterOptionByName("Static");
        assertThat(yourApplicationsPage.getAllApps().stream().map(YourApplicationCell::getName)
                .collect(Collectors.toList()))
                .as("Your Applications Page should contain some dummy static app scans")
                .isNotEmpty();
    }
}
