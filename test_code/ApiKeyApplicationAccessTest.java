package com.fortify.fod.ui.test.regression;

import com.fortify.common.api.providers.BaseRestClient;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.UniqueRunTag;
import com.fortify.common.utils.WaitUtil;
import com.fortify.fod.api.providers.ApplicationsApiProvider;
import com.fortify.fod.api.utils.AccessScopeRestrictionsFodApi;
import com.fortify.fod.api.utils.FodRestClientInitializer;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.ApplicationActions;
import com.fortify.fod.ui.test.actions.LogInActions;
import groovy.util.logging.Slf4j;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

@Owner("dgochev@opentext.com")
@FodBacklogItem("603028")
@Slf4j
public class ApiKeyApplicationAccessTest extends FodBaseTest {

    private String readOnly = "readOnlyUser".concat(UniqueRunTag.generate());
    private String startScans = "startScansUser".concat(UniqueRunTag.generate());
    private String manageApp = "manageAppUser".concat(UniqueRunTag.generate());
    private String secLead = "secLeadUser".concat(UniqueRunTag.generate());
    private String manageApp2 = "manageAppUser2".concat(UniqueRunTag.generate());

    private Map<String, String> readOnlyUserCredentials;
    private Map<String, String> startScansUserCredentials;
    private Map<String, String> manageAppUserCredentials;
    private Map<String, String> secLeadUserCredentials;
    private Map<String, String> manageAppUserSecondCredentials;

    private BaseRestClient fodRestClient;

    @MaxRetryCount(3)
    @Description("Create 4 users with different roles and get it`s secrets")
    @Test(groups = {"regression"})
    public void createApiUsers() {
        ApplicationActions.createApplication(defaultTenantDTO, true);
        BrowserUtil.clearCookiesLogOff();

        var apiSettingsPage = LogInActions.tamUserLogin(defaultTenantDTO).tenantTopNavbar
                .openAdministration().openSettings().openApiTab();

        String secret = apiSettingsPage.addKeyAndGetSecret(readOnly, "Read Only", true);
        readOnlyUserCredentials = apiSettingsPage.getUserRowByName(readOnly).toMap();
        readOnlyUserCredentials.put("Secret", secret);

        secret = apiSettingsPage.addKeyAndGetSecret(startScans, "Start Scans", true);
        startScansUserCredentials = apiSettingsPage.getUserRowByName(startScans).toMap();
        startScansUserCredentials.put("Secret", secret);

        secret = apiSettingsPage.addKeyAndGetSecret(manageApp, "Manage Applications", true);
        manageAppUserCredentials = apiSettingsPage.getUserRowByName(manageApp).toMap();
        manageAppUserCredentials.put("Secret", secret);

        secret = apiSettingsPage.addKeyAndGetSecret(secLead, "Security Lead", true);
        secLeadUserCredentials = apiSettingsPage.getUserRowByName(secLead).toMap();
        secLeadUserCredentials.put("Secret", secret);

        secret = apiSettingsPage.addKeyAndGetSecret(manageApp2, "Manage Applications", true);
        manageAppUserSecondCredentials = apiSettingsPage.getUserRowByName(manageApp2).toMap();
        manageAppUserSecondCredentials.put("Secret", secret);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Test if all users have access to all applications")
    @Test(groups = {"regression"}, dependsOnMethods = {"createApiUsers"})
    public void accessToAllApplicationsTest() {

        AtomicInteger countSecLeadUser = new AtomicInteger();
        AtomicInteger readOnly = new AtomicInteger();
        AtomicInteger startScans = new AtomicInteger();
        AtomicInteger manageApp = new AtomicInteger();

        Supplier<Boolean> sup = () -> {
            countSecLeadUser.set(getApplicationAccessCount(secLeadUserCredentials));
            readOnly.set(getApplicationAccessCount(readOnlyUserCredentials));
            startScans.set(getApplicationAccessCount(startScansUserCredentials));
            manageApp.set(getApplicationAccessCount(manageAppUserCredentials));
            return countSecLeadUser.get() == readOnly.get()
                    && countSecLeadUser.get() == startScans.get()
                    && countSecLeadUser.get() == manageApp.get();
        };

        WaitUtil.waitForTrue(sup, Duration.ofMinutes(3), false);

        assertThat(countSecLeadUser.get())
                .as("Read Only user does not have access to all applications")
                .isEqualTo(readOnly.get())
                .as("Start Scans user does not have access to all applications")
                .isEqualTo(startScans.get())
                .as("Manage Application user does not have access to all applications")
                .isEqualTo(manageApp.get());
    }

    @MaxRetryCount(3)
    @Description("Assign only one application to all api users")
    @Test(groups = {"regression"}, dependsOnMethods = {"accessToAllApplicationsTest"})
    public void assign1AppToAllUsers() {
        String application = new ApplicationsApiProvider(fodRestClient).getApplications().jsonPath()
                .getString("items.applicationName[1]");

        LogInActions.tamUserLogin(defaultTenantDTO).tenantTopNavbar.openAdministration()
                .openSettings().openApiTab()
                .assignApplication(readOnlyUserCredentials.get("Name"), application)
                .assignApplication(startScansUserCredentials.get("Name"), application)
                .assignApplication(manageAppUserCredentials.get("Name"), application);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Test if all users have access only to one application")
    @Test(groups = {"regression"}, dependsOnMethods = {"assign1AppToAllUsers"})
    public void accessToOneApplicationTest() {

        assertThat(getApplicationAccessCount(readOnlyUserCredentials))
                .as("Read Only user has access to more than one applications").isEqualTo(1);
        assertThat(getApplicationAccessCount(startScansUserCredentials))
                .as("Start Scans user has access to more than one applications").isEqualTo(1);
        assertThat(getApplicationAccessCount(manageAppUserCredentials))
                .as("Manage Application user has access to more than one applications").isEqualTo(1);
        //unassigned user should have access to all apprs
        assertThat(getApplicationAccessCount(manageAppUserSecondCredentials))
                .as("Manage Application manageAppUser2 user has access to all applications").isPositive();
    }

    private int getApplicationAccessCount(Map<String, String> userCredentials) {
        fodRestClient = FodRestClientInitializer.getFodRestClient(userCredentials.get("ApiKey"), userCredentials.get("Secret"),
                AccessScopeRestrictionsFodApi.API_TENANT);

        return new ApplicationsApiProvider(fodRestClient)
                .getApplications()
                .jsonPath()
                .getInt("totalCount");
    }
}
