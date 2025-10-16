package com.fortify.fod.ui.test.regression;

import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.MailUtil;
import com.fortify.common.utils.UniqueRunTag;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.elements.Table;
import com.fortify.fod.common.entities.*;
import com.fortify.fod.data_providers.FodUiTestDataProviders;
import com.fortify.fod.ui.pages.tenant.TenantLoginPage;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.pages.tenant.notifications.NotificationCell;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static com.codeborne.selenide.Selenide.refresh;
import static com.codeborne.selenide.Selenide.sleep;
import static org.assertj.core.api.Assertions.assertThat;

@Owner("oradchenko@opentext.com")
@Slf4j
public class NotificationsTest extends FodBaseTest {
    TenantDTO tenant, tenantDTO, tenantEmailDTO;
    TenantUserDTO appLead, dev, executive, leadDev, reviewer, sl, tam;
    ApplicationDTO applicationDTO;
    String targetSubject = "Application Created";
    String targetToList;
    List<String> recipients = new ArrayList<>() {{
        add("null");
        add("Everyone");
        add("Group");
        add("Role");
    }};

    public void init() {
        applicationDTO = ApplicationDTO.createDefaultInstance();
        tenantDTO = TenantDTO.createDefaultInstance();
        tenantDTO.setEntitlementDTO(EntitlementDTO.createDefaultInstance());
        sl = TenantUserDTO.createInstanceWithUserRole(FodCustomTypes.TenantUserRole.SecurityLead);
        sl.setUserName(tenantDTO.getUserName());
        sl.setTenant(tenantDTO.getTenantName());
        appLead = TenantUserDTO.createInstanceWithUserRole(FodCustomTypes.TenantUserRole.ApplicationLead);
        appLead.setTenant(tenantDTO.getTenantName());
        dev = TenantUserDTO.createInstanceWithUserRole(FodCustomTypes.TenantUserRole.Developer);
        dev.setTenant(tenantDTO.getTenantName());
        executive = TenantUserDTO.createInstanceWithUserRole(FodCustomTypes.TenantUserRole.Executive);
        executive.setTenant(tenantDTO.getTenantName());
        leadDev = TenantUserDTO.createInstanceWithUserRole(FodCustomTypes.TenantUserRole.LeadDeveloper);
        leadDev.setTenant(tenantDTO.getTenantName());
        reviewer = TenantUserDTO.createInstanceWithUserRole(FodCustomTypes.TenantUserRole.Reviewer);
        reviewer.setTenant(tenantDTO.getTenantName());
        tam = TenantUserDTO.createInstanceWithUserRole(FodCustomTypes.TenantUserRole.ApplicationLead);
        tam.setUserName("TAM" + UniqueRunTag.generate());
    }

    private void initEmail() {
        tenantEmailDTO = TenantDTO.createDefaultInstance();
        this.targetToList = tenantEmailDTO.getUserEmail();
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Admin user should be able to create tenant and entitlement on admin site.")
    @Test(groups = {"regression"})
    public void prepareTestData() {
        tenant = TenantDTO.createDefaultInstance();
        tenant.setEntitlementModel(FodCustomTypes.EntitlementModel.Units);
        tenant.setSubscriptionModel(FodCustomTypes.SubscriptionModel.Period);
        tenant.setEntitlementDTO(EntitlementDTO.createDefaultInstance());
        TenantActions.createTenant(tenant, true);
        BrowserUtil.clearCookiesLogOff();
        LogInActions.tamUserLogin(tenant);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Admin user should be able to create notification on tenant site.")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData"})
    public void createNotificationsTest() {
        var noteName = String.format("FLASH-NOTE-%s", tenant.getRunTag());

        var subsPage = LogInActions.tamUserLogin(tenant)
                .tenantTopNavbar.openNotifications().openMySubscriptions()
                .openMySubscriptions().openGlobalSubscriptions()
                .addSubscription(
                        "Application Created",
                        noteName,
                        null,
                        null);
        subsPage.findWithSearchBox(noteName);
        var table = subsPage.getActiveTable();
        var subsList = table
                .getAllColumnValues(table.getColumnIndex("Note"));
        assertThat(subsList).as("Table should have 1 record after the search").hasSize(1);
        assertThat(subsList.get(0))
                .as("Subscription should be created").isEqualTo(noteName);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("TAM user should be able to receive notification on tenant site.")
    @Test(groups = {"regression"}, dependsOnMethods = {"createNotificationsTest"})
    public void verifyNotificationsTest() {
        var applicationDTO = ApplicationDTO.createDefaultInstance();
        var myNotificationsPage = ApplicationActions
                .createApplication(applicationDTO, tenant, true)
                .tenantTopNavbar.openNotifications();
        myNotificationsPage.findWithSearchBox(applicationDTO.getApplicationName());
        var notifications = myNotificationsPage.waitForNonEmptyTable().getAllNotifications();
        assertThat(notifications).as("There should be 1 notification").hasSize(1);
        assertThat(notifications.get(0).getMessage())
                .as("Verify notification message")
                .isEqualTo(String.format("Application %s has been created.", applicationDTO.getApplicationName()));
    }

    @Owner("kbadia@opentext.com")
    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("notification would be triggered when an update was made to " +
            "the attribute value that was setup in the subscription")
    @Test(groups = {"regression"})
    public void verifyNotificationsAttributeTest() {
        var tenant = TenantDTO.createDefaultInstance();
        tenant.setEntitlementModel(FodCustomTypes.EntitlementModel.Units);
        tenant.setSubscriptionModel(FodCustomTypes.SubscriptionModel.Period);
        tenant.setEntitlementDTO(EntitlementDTO.createDefaultInstance());
        TenantActions.createTenant(tenant, true);
        BrowserUtil.clearCookiesLogOff();
        TenantUserActions.activateSecLead(tenant, true);
        BrowserUtil.clearCookiesLogOff();
        AttributeDTO picklist = AttributeDTO.createDefaultInstance();
        picklist.setAttributeDataType(FodCustomTypes.AttributeDataType.Picklist);
        picklist.setRequired(true);
        picklist.setPickListValues(new String[]{"XX", "QQ", "VV", "ZZ"});
        CustomAttributesActions.createCustomAttribute(picklist, tenant, true);
        var attributeAppDTO = ApplicationDTO.createDefaultInstance();
        HashMap<AttributeDTO, String> attributesMap = new HashMap<>() {{
            put(picklist, "QQ");
        }};
        attributeAppDTO.setAttributesMap(attributesMap);
        BrowserUtil.clearCookiesLogOff();
        LogInActions.tenantUserLogIn(tenant.getUserName(), FodConfig.TAM_PASSWORD, tenant.getTenantCode())
                .tenantTopNavbar.openNotifications().openMySubscriptions()
                .openMySubscriptions().openGlobalSubscriptions()
                .addSubscriptionWithScopeAndEmail(
                        "Application Created",
                        "noteName",
                        "Attribute Value",
                        null, "QQ", false);
        var myNotificationsPage =
                ApplicationActions.createApplication(attributeAppDTO, tenant, false)
                        .tenantTopNavbar.openNotifications().findWithSearchBox(attributeAppDTO.getApplicationName());
        var notifications = myNotificationsPage.waitForNonEmptyTable().getAllNotifications();
        assertThat(notifications).as("There should be 1 notification").hasSize(1);
        assertThat(notifications.get(0).getMessage())
                .as("Verify notification message")
                .isEqualTo(String.format("Application %s has been created.", attributeAppDTO.getApplicationName()));
    }

    @Owner("kbadia@opentext.com")
    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify Notifications for all available scope field with respective scope name")
    @Parameters({"Scope", "ScopeName"})
    @Test(groups = {"regression"}, dataProvider = "verifyNotificationsInputTest",
            dataProviderClass = NotificationsTest.class,
            dependsOnMethods = {"prepareTestData", "createNotificationsTest"})
    public void notificationsInputTest(String scope, String scopeName) {
        setTestCaseName(String.format("Validate Notification Scope: %s with: %s", scope, scopeName));
        var noteName = String.format("FLASH-NOTE-%s", tenant.getRunTag());

        var applicationDTO = ApplicationDTO.createDefaultInstance();
        if (scopeName.equals("ApplicationName")) {
            var appDTO = ApplicationDTO.createDefaultInstance();
            ApplicationActions.createApplication(appDTO, tenant, true);
            scopeName = appDTO.getApplicationName();
            BrowserUtil.clearCookiesLogOff();
        }
        if (scopeName.equals("Mobile")) {
            applicationDTO = ApplicationDTO.createDefaultMobileInstance();
        }

        LogInActions.tamUserLogin(tenant)
                .tenantTopNavbar.openNotifications().openMySubscriptions()
                .openMySubscriptions().openGlobalSubscriptions()
                .addSubscriptionWithScopeAndEmail(
                        "Application Created",
                        noteName,
                        scope,
                        null, scopeName, false);
        var myNotificationsPage =
                ApplicationActions.createApplication(applicationDTO, tenant, false)
                        .tenantTopNavbar.openNotifications().findWithSearchBox(applicationDTO.getApplicationName());
        var notifications = myNotificationsPage.waitForNonEmptyTable().getAllNotifications();
        assertThat(notifications).as("There should be 1 notification").hasSize(1);
        assertThat(notifications.get(0).getMessage())
                .as("Verify notification message")
                .isEqualTo(String.format("Application %s has been created.", applicationDTO.getApplicationName()));
    }

    @Owner("kbadia@opentext.com")
    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify Notifications for all available recipients")
    @Test(groups = {"regression"}, dependsOnMethods = {"prepareTestData", "createNotificationsTest"})
    public void validateNotificationsRecipientTest() {
        var noteName = String.format("FLASH-NOTE-%s", tenant.getRunTag());
        for (var recipientToValidate : recipients) {
            if (recipientToValidate.equals("Group")) {
                String userName = "TestUser" + UniqueRunTag.generate();
                var tenantUser = TenantUserDTO.createDefaultInstance();
                String groupName = "Group-" + UniqueRunTag.generate();

                LogInActions.tamUserLogin(tenant);
                var userManagementPage =
                        TenantLoginPage.navigate().tenantTopNavbar.openAdministration();
                var usersPage = userManagementPage.openUsers();

                for (int i = 0; i < 5; i++) {
                    tenantUser.setUserName(userName + i);
                    tenantUser.setUserEmail("testusermail" + UniqueRunTag.generate() + i + "@gmail.com");
                    tenantUser.setFirstName("FirstName" + i);
                    tenantUser.setLastName("LastName" + i);
                    tenantUser.setRole(FodCustomTypes.TenantUserRole.Developer);
                    usersPage.createUser(tenantUser);
                    Assertions.assertThat(usersPage.getUserNames())
                            .as("Verify that new user is present in table")
                            .contains(userName + i);
                }
                userManagementPage.openGroups();
                TenantGroupActions.createTenantGroup(groupName, "", true);
                BrowserUtil.clearCookiesLogOff();
            }
            if (recipientToValidate.equals("null")) {
                recipientToValidate = null;
            }
            LogInActions.tamUserLogin(tenant)
                    .tenantTopNavbar.openNotifications().openMySubscriptions()
                    .openMySubscriptions().openGlobalSubscriptions()
                    .addSubscription(
                            "Application Created",
                            noteName,
                            "All Applications",
                            recipientToValidate);
            var applicationDTO = ApplicationDTO.createDefaultInstance();
            var myNotificationsPage = ApplicationActions
                    .createApplication(applicationDTO, tenant, false)
                    .tenantTopNavbar.openNotifications();
            myNotificationsPage.findWithSearchBox(applicationDTO.getApplicationName());
            var notifications = myNotificationsPage.waitForNonEmptyTable().getAllNotifications();
            assertThat(notifications).as("There should be 1 notification").hasSize(1);
            assertThat(notifications.get(0).getMessage())
                    .as("Verify notification message")
                    .isEqualTo(String.format("Application %s has been created.", applicationDTO.getApplicationName()));
            BrowserUtil.clearCookiesLogOff();
        }
    }

    @Owner("kbadia@opentext.com")
    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Prepare test data for test execution")
    @Test(groups = {"regression"})
    public void prepareTestDataForNotification() {
        init();

        TenantActions.createTenant(tenantDTO);
        BrowserUtil.clearCookiesLogOff();
        ApplicationActions.createApplication(applicationDTO, tenantDTO, true);

        TenantUserActions.activateSecLead(tenantDTO, false);
        TenantUserActions.createTenantUsers(tenantDTO, appLead, dev, executive, leadDev, reviewer);
        LogInActions.tamUserLogin(tenantDTO);
        var groupsPage = TenantGroupActions
                .createTenantGroup("New group " + UniqueRunTag.generate(), "", true);
        groupsPage.getAllGroups().get(0).pressAssignApplication().assignApplication(applicationDTO)
                .pressSave();
    }

    @Owner("kbadia@opentext.com")
    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Parameters("User Role")
    @Description("Verify Visibility Of Notifications for different roles")
    @Test(groups = {"regression"},
            dataProvider = "tenantUserRolesStrings",
            dataProviderClass = FodUiTestDataProviders.class,
            dependsOnMethods = {"prepareTestDataForNotification"})
    public void validateVisibilityOfNotificationsTest(String role) {
        setTestCaseName("Validate  Visibility Of Notifications for: " + role);
        switch (role) {
            case "Application Lead":
                visibilityNotificationsTest(appLead.getUserName(), false, false);
                break;
            case "Developer":
                visibilityNotificationsTest(dev.getUserName(), false, false);
                break;
            case "Reviewer":
                visibilityNotificationsTest(reviewer.getUserName(), false, false);
                break;
            case "Security Lead":
                visibilityNotificationsTest(sl.getUserName(), false, true);
                break;
            case "Executive":
                visibilityNotificationsTest(executive.getUserName(), false, false);
                break;
            case "Lead Developer":
                visibilityNotificationsTest(leadDev.getUserName(), false, false);
                break;
            case "TAM":
                visibilityNotificationsTest(tenantDTO.getAssignedUser(), true, false);
                break;
            default:
                throw new IllegalStateException("Unexpected user role: " + role);
        }
    }

    @Owner("kbadia@opentext.com")
    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Test data preparation")
    @Test(groups = {"regression"})
    public void prepareTestDataEmail() {
        initEmail();
        TenantActions.createTenant(tenantEmailDTO, true, false);
        BrowserUtil.clearCookiesLogOff();

        TenantUserActions.activateSecLead(tenantEmailDTO, true);
    }

    @Owner("kbadia@opentext.com")
    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify email notifications for different recipients")
    @Test(groups = {"regression"}, dependsOnMethods = "prepareTestDataEmail")
    public void validateForEmailNotification() {
        LogInActions
                .tenantUserLogIn(tenantEmailDTO.getUserName(), FodConfig.TAM_PASSWORD, tenantEmailDTO.getTenantCode())
                .tenantTopNavbar.openNotifications().openMySubscriptions()
                .openMySubscriptions().openGlobalSubscriptions()
                .addSubscriptionWithEmail(
                        "Application Created",
                        "noteName",
                        "All Applications",
                        "Everyone");

        var applicationDTO1 = ApplicationDTO.createDefaultInstance();
        var myNotificationsPage = ApplicationActions
                .createApplication(applicationDTO1, tenantEmailDTO, false)
                .tenantTopNavbar.openNotifications();
        myNotificationsPage.findWithSearchBox(applicationDTO1.getApplicationName());
        var notifications = myNotificationsPage.waitForNonEmptyTable().getAllNotifications();
        assertThat(notifications).as("There should be 1 notification").hasSize(1);
        assertThat(notifications.get(0).getMessage())
                .as("Verify notification message")
                .isEqualTo(String.format("Application %s has been created.", applicationDTO1.getApplicationName()));
        var mailBody = new MailUtil().findEmailByRecipientAndSubject(targetSubject, targetToList);
        Document html = Jsoup.parse(mailBody);

        var link = html.selectXpath("//*[contains(text(), 'Application Created')]").attr("text");
        assertThat(link)
                .as("Verify notification message")
                .isNotNull();
        BrowserUtil.clearCookiesLogOff();
        String userName = "TestUser" + UniqueRunTag.generate();
        var tenantUser = TenantUserDTO.createDefaultInstance();
        String groupName = "Group-" + UniqueRunTag.generate();

        LogInActions.tamUserLogin(tenantEmailDTO);
        var userManagementPage = TenantLoginPage.navigate().tenantTopNavbar.openAdministration();
        var usersPage = userManagementPage.openUsers();

        for (int i = 0; i < 5; i++) {
            tenantUser.setUserName(userName + i);
            tenantUser.setUserEmail("testusermail" + UniqueRunTag.generate() + i + "@gmail.com");
            tenantUser.setFirstName("FirstName" + i);
            tenantUser.setLastName("LastName" + i);
            tenantUser.setRole(FodCustomTypes.TenantUserRole.Developer);
            usersPage.createUser(tenantUser);
            Assertions.assertThat(usersPage.getUserNames())
                    .as("Verify that new user is present in table")
                    .contains(userName + i);
        }
        userManagementPage.openGroups();
        TenantGroupActions.createTenantGroup(groupName, "", true);

        BrowserUtil.clearCookiesLogOff();

        LogInActions
                .tenantUserLogIn(tenantEmailDTO.getUserName(), FodConfig.TAM_PASSWORD, tenantEmailDTO.getTenantCode())
                .tenantTopNavbar.openNotifications().openMySubscriptions()
                .openMySubscriptions().openGlobalSubscriptions()
                .addSubscription(
                        "Application Created",
                        "noteName",
                        "All Applications",
                        "Group");

        var applicationDTO2 = ApplicationDTO.createDefaultInstance();
        var myNotificationsPage2 = ApplicationActions
                .createApplication(applicationDTO2, tenantEmailDTO, false)
                .tenantTopNavbar.openNotifications();
        myNotificationsPage2.findWithSearchBox(applicationDTO2.getApplicationName());
        var notifications2 = myNotificationsPage.waitForNonEmptyTable().getAllNotifications();
        assertThat(notifications2).as("There should be 1 notification").hasSize(1);
        assertThat(notifications2.get(0).getMessage())
                .as("Verify notification message")
                .isEqualTo(String.format("Application %s has been created.", applicationDTO2.getApplicationName()));
        var mailBody2 = new MailUtil().findEmailByRecipientAndSubject(targetSubject, targetToList);
        Document html2 = Jsoup.parse(mailBody2);

        var link2 = html2.selectXpath("//*[contains(text(), 'Application Created')]").attr("text");

        assertThat(link2)
                .as("Verify notification message")
                .isNotNull();
        BrowserUtil.clearCookiesLogOff();
        LogInActions
                .tenantUserLogIn(tenantEmailDTO.getUserName(), FodConfig.TAM_PASSWORD, tenantEmailDTO.getTenantCode())
                .tenantTopNavbar.openNotifications().openMySubscriptions()
                .openMySubscriptions().openGlobalSubscriptions()
                .addSubscription(
                        "Application Created",
                        "noteName",
                        "All Applications",
                        "Role");

        var applicationDTO3 = ApplicationDTO.createDefaultInstance();
        var myNotificationsPage3 = ApplicationActions
                .createApplication(applicationDTO3, tenantEmailDTO, false)
                .tenantTopNavbar.openNotifications();
        myNotificationsPage3.findWithSearchBox(applicationDTO3.getApplicationName());
        var notifications3 = myNotificationsPage.waitForNonEmptyTable().getAllNotifications();
        assertThat(notifications3).as("There should be 1 notification").hasSize(1);
        assertThat(notifications3.get(0).getMessage())
                .as("Verify notification message")
                .isEqualTo(String.format("Application %s has been created.", applicationDTO3.getApplicationName()));
        var mailBody3 = new MailUtil().findEmailByRecipientAndSubject(targetSubject, targetToList);
        Document html3 = Jsoup.parse(mailBody3);
        var link3 = html3.selectXpath("//*[contains(text(), 'Application Created')]").attr("text");

        assertThat(link3)
                .as("Verify notification message")
                .isNotNull();
    }


    @Owner("kbadia@opentext.com")
    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify my Subscriptions Without Email Notifications")
    @Parameters({"Scope", "ScopeName"})
    @Test(groups = {"regression"}, dataProvider = "verifyNotificationsInputTest",
            dependsOnMethods = {"prepareTestData", "createNotificationsTest"},
            dataProviderClass = NotificationsTest.class)
    public void mySubscriptionsWithEmailNotificationTest(String scope, String scopeName) {
        setTestCaseName(String
                .format("Validate mySubscriptions With Email Notification : %s with: %s", scope, scopeName));
        var noteName = String.format("FLASH-NOTE-%s", tenant.getRunTag());
        var applicationDTO = ApplicationDTO.createDefaultInstance();
        if (scopeName.equals("ApplicationName")) {
            var appDTO = ApplicationDTO.createDefaultInstance();
            ApplicationActions.createApplication(appDTO, tenant, true);
            scopeName = appDTO.getApplicationName();
            BrowserUtil.clearCookiesLogOff();
        }
        if (scopeName.equals("Mobile")) {
            applicationDTO = ApplicationDTO.createDefaultMobileInstance();
        }

        LogInActions.tamUserLogin(tenant)
                .tenantTopNavbar.openNotifications().openMySubscriptions()
                .openMySubscriptions().addSubscriptionWithScopeAndEmail(
                        "Application Created",
                        noteName,
                        scope,
                        null, scopeName, true);
        var myNotificationsPage =
                ApplicationActions.createApplication(applicationDTO, tenant, false)
                        .tenantTopNavbar.openNotifications().findWithSearchBox(applicationDTO.getApplicationName());
        var notifications = myNotificationsPage.waitForNonEmptyTable().getAllNotifications();
        assertThat(notifications).as("There should be 1 notification").hasSize(1);
        assertThat(notifications.get(0).getMessage())
                .as("Verify notification message")
                .isEqualTo(String.format("Application %s has been created.", applicationDTO.getApplicationName()));
        var mailBody = new MailUtil().findEmailByRecipientAndSubject(targetSubject, targetToList);
        Document html = Jsoup.parse(mailBody);

        var link = html.selectXpath("//*[contains(text(), 'Application Created')]").attr("text");
        assertThat(link)
                .as("Verify notification message")
                .isNotNull();
    }

    @Owner("kbadia@opentext.com")
    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify my Subscriptions Without Email Notifications")
    @Parameters({"Scope", "ScopeName"})
    @Test(groups = {"regression"}, dataProvider = "verifyNotificationsInputTest",
            dependsOnMethods = {"prepareTestData", "createNotificationsTest"},
            dataProviderClass = NotificationsTest.class)
    public void mySubscriptionsWithoutEmailNotificationTest(String scope, String scopeName) {
        setTestCaseName(String
                .format("Validate mySubscriptions Without EmailNotification: %s with: %s", scope, scopeName));
        var noteName = String.format("FLASH-NOTE-%s", tenant.getRunTag());
        var applicationDTO = ApplicationDTO.createDefaultInstance();
        if (scopeName.equals("ApplicationName")) {
            var appDTO = ApplicationDTO.createDefaultInstance();
            ApplicationActions.createApplication(appDTO, tenant, true);
            scopeName = appDTO.getApplicationName();
            BrowserUtil.clearCookiesLogOff();
        }
        if (scopeName.equals("Mobile")) {
            applicationDTO = ApplicationDTO.createDefaultMobileInstance();
        }

        LogInActions.tamUserLogin(tenant)
                .tenantTopNavbar.openNotifications().openMySubscriptions()
                .openMySubscriptions().addSubscriptionWithScopeAndEmail(
                        "Application Created",
                        noteName,
                        scope,
                        null, scopeName, false);
        var myNotificationsPage =
                ApplicationActions.createApplication(applicationDTO, tenant, false)
                        .tenantTopNavbar.openNotifications().findWithSearchBox(applicationDTO.getApplicationName());
        var notifications = myNotificationsPage.waitForNonEmptyTable().getAllNotifications();
        assertThat(notifications).as("There should be 1 notification").hasSize(1);
        assertThat(notifications.get(0).getMessage())
                .as("Verify notification message")
                .isEqualTo(String.format("Application %s has been created.", applicationDTO.getApplicationName()));
        var mailBody = new MailUtil().findEmailByRecipientAndSubject(targetSubject, targetToList);
        Document html = Jsoup.parse(mailBody);

        var link = html.selectXpath("//*[contains(text(), 'Application Created')]").attr("text");
        assertThat(link)
                .as("Verify notification message")
                .isNotNull();
    }

    @Owner("kbadia@opentext.com")
    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify that Read or Unread notifications are removed after 3 months")
    @Test(groups = {"regression"})
    public void deleteNotificationTest() {
        LogInActions.tamUserLogin(defaultTenantDTO)
                .tenantTopNavbar.openNotifications().openMySubscriptions()
                .openMySubscriptions().openGlobalSubscriptions()
                .addSubscription(
                        "Application Created",
                        "Creating Application.",
                        null,
                        null);
        var applicationDTO = ApplicationDTO.createDefaultInstance();
        String dateFormatPattern = "yyyy/MM/dd";
        DateFormat dateFormat = new SimpleDateFormat(dateFormatPattern);
        Calendar today = Calendar.getInstance();
        Calendar todayDate = Calendar.getInstance();
        todayDate.add(Calendar.MONTH, -3);
        Date lastDay = todayDate.getTime();
        String todayStr = dateFormat.format(today.getTime());
        String lastDayStr = dateFormat.format(lastDay.getTime());
        var notifications = ApplicationActions
                .createApplication(applicationDTO, defaultTenantDTO, false)
                .tenantTopNavbar.openNotifications().waitForNonEmptyTable().getAllNotifications();
        notifications.forEach(i -> Assertions.assertThat(
                        i.getDate())
                .as("Verify that Read/Unread notifications are removed after 3 months")
                .isBetween(lastDayStr, todayStr));
    }

    @Owner("kbadia@opentext.com")
    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify that start, pause, complete Notifications are triggering")
    @Test(groups = {"regression"})
    public void scanNotificationTest() {
        LogInActions
                .tenantUserLogIn(defaultTenantDTO.getUserName(),
                        FodConfig.TAM_PASSWORD, defaultTenantDTO.getTenantCode());
        var webApp1 = ApplicationActions.createApplication(defaultTenantDTO, false);
        var dynamicScanDTO = DynamicScanDTO.createDefaultInstance();
        DynamicScanActions.createDynamicScan(dynamicScanDTO, webApp1,
                FodCustomTypes.SetupScanPageStatus.InProgress);
        var myNotificationsPageMob = new TenantTopNavbar().openNotifications()
                .findWithSearchBox(webApp1.getApplicationName());
        var notificationsMob = myNotificationsPageMob.waitForNonEmptyTable().getAllNotifications();
        assertThat(notificationsMob).as("There should be 2 notification").hasSize(2);
        assertThat(notificationsMob.stream().map(NotificationCell::getMessage).collect(Collectors.toList()))
                .as("Verify notification message")
                .contains(String
                        .format("AUTO-DYNAMIC scan has started for %s (%s).",
                                webApp1.getApplicationName(), webApp1.getReleaseName()));
        BrowserUtil.clearCookiesLogOff();
        LogInActions.adminLogIn().adminTopNavbar.openDynamic()
                .openDetailsFor(webApp1.getApplicationName())
                .pressPauseButton().pressOkButton();
        BrowserUtil.clearCookiesLogOff();
        var myNotificationsPage = LogInActions
                .tenantUserLogIn(defaultTenantDTO.getUserName(),
                        FodConfig.TAM_PASSWORD, defaultTenantDTO.getTenantCode())
                .tenantTopNavbar.openNotifications()
                .findWithSearchBox(webApp1.getReleaseName());
        var notificationsList = myNotificationsPageMob.waitForNonEmptyTable().getAllNotifications();
        if (notificationsList.stream().noneMatch(x -> x.getMessage()
                .contains("The AUTO-DYNAMIC scan has been paused"))) {
            sleep(Duration.ofMinutes(2).toMillis());
            refresh();
            notificationsList = myNotificationsPage.getAllNotifications();
        }
        assertThat(notificationsMob).as("There should be 2 notification").hasSize(2);
        assertThat(notificationsList.stream().anyMatch(x -> x.getMessage()
                .contains("The AUTO-DYNAMIC scan has been paused")))
                .as("Notification should be present in Notifications Page ")
                .isTrue();
        BrowserUtil.clearCookiesLogOff();
        LogInActions.adminLogIn().adminTopNavbar.openDynamic()
                .openDetailsFor(webApp1.getApplicationName()).pressResumeButton().pressOkButton().publishScan();
        BrowserUtil.clearCookiesLogOff();
        var notificationsDynamic = LogInActions
                .tenantUserLogIn(defaultTenantDTO.getUserName(),
                        FodConfig.TAM_PASSWORD, defaultTenantDTO.getTenantCode())
                .tenantTopNavbar.openNotifications()
                .findWithSearchBox(webApp1.getReleaseName());

        var notifications = notificationsDynamic.waitForNonEmptyTable().getAllNotifications();
        assertThat(notifications.stream().map(NotificationCell::getMessage).collect(Collectors.toList()))
                .as("Verify notification message")
                .contains(String
                        .format("AUTO-DYNAMIC scan has completed for %s (%s).",
                                webApp1.getApplicationName(), webApp1.getReleaseName()));
    }

    @Owner("kbadia@opentext.com")
    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify that Cancel Scan Notification")
    @Test(groups = {"regression"})
    public void validateCancelScanNotificationTest() {
        var webApp1 = ApplicationDTO.createDefaultInstance();
        LogInActions.tamUserLogin(defaultTenantDTO);
        webApp1 = ApplicationActions.createApplication(defaultTenantDTO, false);
        var dynamicScanDTO = DynamicScanDTO.createDefaultInstance();
        DynamicScanActions.createDynamicScan(dynamicScanDTO, webApp1,
                FodCustomTypes.SetupScanPageStatus.Scheduled, FodCustomTypes.SetupScanPageStatus.InProgress);
        BrowserUtil.clearCookiesLogOff();
        DynamicScanActions.cancelDynamicScanAdmin(webApp1, true, false);
        BrowserUtil.clearCookiesLogOff();
        LogInActions.tamUserLogin(defaultTenantDTO)
                .tenantTopNavbar.openNotifications()
                .findWithSearchBox(webApp1.getReleaseName());
        var myNotificationsPageDynamic = new TenantTopNavbar().openNotifications()
                .findWithSearchBox(webApp1.getReleaseName());
        var notificationsList =
                myNotificationsPageDynamic.waitForNonEmptyTable().getAllNotifications();
        if (notificationsList.stream().noneMatch(x -> x.getMessage()
                .contains("The AUTO-DYNAMIC scan has been cancelled"))) {
            sleep(Duration.ofMinutes(2).toMillis());
            refresh();
            notificationsList = myNotificationsPageDynamic.getAllNotifications();
        }
        Assertions.assertThat(notificationsList.stream().anyMatch(x -> x.getMessage()
                        .contains("The AUTO-DYNAMIC scan has been cancelled")))
                .as("Notification should be present in Notifications Page ")
                .isTrue();
    }

    @Owner("kbadia@opentext.com")
    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify that FPC Scan Notification")
    @Test(groups = {"regression"})
    public void validateFPCScanNotificationTest() {
        LogInActions.adminLogIn().adminTopNavbar.openTenants()
                .openTenantByName(defaultTenantDTO.getTenantName())
                .openTabOptions().setFPCFlag(true);
        BrowserUtil.clearCookiesLogOff();
        LogInActions
                .tenantUserLogIn(defaultTenantDTO.getUserName(),
                        FodConfig.TAM_PASSWORD, defaultTenantDTO.getTenantCode())
                .tenantTopNavbar.openNotifications().openMySubscriptions()
                .openMySubscriptions().openGlobalSubscriptions()
                .addSubscriptionWithEmail(
                        "FPC Submitted",
                        "noteName",
                        "All Applications",
                        "Everyone");

        var applicationDTO = ApplicationDTO.createDefaultInstance();
        var firstDynamicScan = DynamicScanDTO.createDefaultInstance();
        ApplicationActions.createApplication(applicationDTO, defaultTenantDTO, false);
        DynamicScanActions.createDynamicScan(firstDynamicScan, applicationDTO);
        BrowserUtil.clearCookiesLogOff();
        DynamicScanActions.importDynamicScanAdmin(
                applicationDTO.getReleaseName(),
                "payloads/fod/DynSuper7.fpr",
                true,
                true,
                true,
                true);
        DynamicScanActions.completeDynamicScanAdmin(applicationDTO, false);
        BrowserUtil.clearCookiesLogOff();
        var page = LogInActions.tamUserLogin(defaultTenantDTO).openYourReleases()
                .openDetailsForRelease(applicationDTO).openIssues();
        page.getAllIssues().get(0).markAsFalsePositive(false);
        page.submitFalsePositiveChallenge();
        BrowserUtil.clearCookiesLogOff();
        LogInActions
                .tenantUserLogIn(defaultTenantDTO.getUserName(),
                        FodConfig.TAM_PASSWORD, defaultTenantDTO.getTenantCode())
                .tenantTopNavbar.openNotifications()
                .findWithSearchBox(applicationDTO.getReleaseName());
        var myNotificationsPageDynamic = new TenantTopNavbar().openNotifications()
                .findWithSearchBox(applicationDTO.getApplicationName());
        var notificationsDynamic =
                myNotificationsPageDynamic.waitForNonEmptyTable().getAllNotifications()
                        .stream().map(NotificationCell::getMessage).collect(Collectors.toList());
        assertThat(notificationsDynamic)
                .as("Verify notification message")
                .contains(String
                        .format("A False Positive Challenge has been submitted for %s (%s).",
                                applicationDTO.getApplicationName(), applicationDTO.getReleaseName()));
    }

    private void visibilityNotificationsTest(String username, boolean tamLogin, boolean sl) {
        if (tamLogin || sl) {
            if (sl) {
                var myNotificationsPage = LogInActions
                        .tenantUserLogIn(username, FodConfig.TAM_PASSWORD, tenantDTO.getTenantCode())
                        .tenantTopNavbar.openNotifications();
                var notifications = myNotificationsPage.waitForNonEmptyTable().getAllNotifications();
                assertThat(notifications).as("There should be 1 notification").hasSize(1);
                assertThat(notifications.get(0).getMessage())
                        .as("Verify notification message")
                        .isEqualTo(String
                                .format("Application %s has been created.", applicationDTO.getApplicationName()));
                var subsPage = myNotificationsPage.openMySubscriptions().openGlobalSubscriptions();
                assertThat(subsPage.checkAddNewSubscriptionButton())
                        .as("Verify add new subscription button visibility")
                        .isTrue();
                assertThat(subsPage.getAllSubscriptions().get(0).isEditButtonDisplayed())
                        .as("Verify edit button visibility")
                        .isTrue();
            } else {
                LogInActions.tamUserLogin(username, FodConfig.TAM_PASSWORD, tenantDTO.getTenantCode())
                        .tenantTopNavbar.openNotifications().openMySubscriptions()
                        .openMySubscriptions().openGlobalSubscriptions()
                        .addSubscription(
                                "Application Created",
                                "noteName",
                                "All Applications",
                                "Role");
                BrowserUtil.clearCookiesLogOff();
                LogInActions.tamUserLogin(username, FodConfig.TAM_PASSWORD, tenantDTO.getTenantCode())
                        .tenantTopNavbar.openNotifications();
                assertThat(Table.isEmpty())
                        .as("TAM User Shouldn't have visible notifications in global subscriptions")
                        .isTrue();
            }
        } else {
            var myNotificationsPage =
                    LogInActions.tenantUserLogIn(username, FodConfig.TAM_PASSWORD, tenantDTO.getTenantCode())
                            .tenantTopNavbar;
            var notificationsPage = myNotificationsPage.openNotifications();
            assertThat(Table.isEmpty())
                    .as("Verify notification message")
                    .isTrue();
            var subscriptionsPage = notificationsPage.openMySubscriptions().openGlobalSubscriptions();
            assertThat(subscriptionsPage.getAllSubscriptions().get(0).isEditButtonDisplayed())
                    .as("Table should have no edit button")
                    .isFalse();
        }
    }

    @DataProvider(name = "verifyNotificationsInputTest", parallel = true)
    public Object[][] verifyNotificationsInputTest() {
        return new Object[][]{
                {"Application", "ApplicationName"},
                {"Application Type", "Web / Thick-Client"},
                {"Application Type", "Mobile"},
                {"Business Criticality", "High"}
        };
    }
}