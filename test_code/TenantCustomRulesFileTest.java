package com.fortify.fod.ui.test.regression;

import com.fortify.common.ui.config.AllureReportUtil;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.UniqueRunTag;
import com.fortify.fod.common.config.FodConfig;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.elements.ModalDialog;
import com.fortify.fod.common.elements.Paging;
import com.fortify.fod.common.entities.*;
import com.fortify.fod.data_providers.FodUiTestDataProviders;
import com.fortify.fod.ui.pages.common.common.cells.EventLogCellMain;
import com.fortify.fod.ui.pages.tenant.navigation.TenantSideNavTabs;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;
import utils.RetryAnalyzer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.codeborne.selenide.Selenide.sleep;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.linesOf;

@Owner("ysmal@opentext.com")
@Slf4j
public class TenantCustomRulesFileTest extends FodBaseTest {

    TenantUserDTO appLead, dev, executive, leadDev, reviewer, sl, tam;
    TenantDTO tenantDTO;
    ApplicationDTO applicationDTO;
    StaticScanDTO scanDTO;

    String newClassName = "New Class Name";
    String language = FodCustomTypes.TechnologyStack.JAVA.getTypeValue();
    String languageInRuleFile = "java";

    String msgCreated = "Dataflow Cleanse Rule Created";
    String msgEdited = "Dataflow Cleanse Rule Modified";
    String msgDeleted = "Dataflow Cleanse Rule Deleted";
    String msgAppCreated = "Application Dataflow Cleanse Rule Created";
    String msgAppEdited = "Application Dataflow Cleanse Rule Modified";
    String msgAppDeleted = "Application Dataflow Cleanse Rule Deleted";

    String category = "Privacy Violation";
    String dataValidation = "Object Function";
    String className = "AUTO-ClassName";
    String packageName = "AUTO-PackageName";
    String functionName = "AUTO-FunctionName";
    String ruleInFile = "+VALIDATED_PRIVACY_VIOLATION";

    List<String> langList = new ArrayList<>() {{
        add(".NET");
        add("ABAP");
        add("C/C++");
        add("COBOL");
        add("JAVA/J2EE/Kotlin");
        add("Objective C/C++");
        add("PHP");
        add("PL/SQL & T-SQL");
        add("PYTHON");
        add("Ruby");
        add("Scala");
        add("Swift");
    }};

    public void init() {
        scanDTO = StaticScanDTO.createDefaultInstance();
        scanDTO.setTechnologyStack(FodCustomTypes.TechnologyStack.JAVA);
        scanDTO.setAssessmentType("Static Premium");
        scanDTO.setEntitlement("Subscription");
        scanDTO.setFileToUpload("payloads/fod/static.java.zip");

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
        tam.setUserName("TAM");
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Prepare test data for test execution")
    @Test(groups = {"regression"})
    public void prepareTestData() {
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

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate custom rules creation functionality")
    @Test(groups = {"regression"},
            priority = 3, dependsOnMethods = {"prepareTestData"})
    public void customRulesFileTest() {
        var globalAuditPage = LogInActions.tamUserLogin(tenantDTO)
                .tenantTopNavbar.openAdministration()
                .openAuditTools().openCleanseRules();

        var addRulePopup = globalAuditPage.pressAddRule();
        var languages = addRulePopup.languageDropdown.$$("option").texts();
        assertThat(languages).containsAll(langList);

        var rules = addRulePopup.setLanguage(language)
                .setCategory(category)
                .setPackageName(packageName)
                .setDataValidation(dataValidation)
                .setFunctionName(functionName)
                .setClassName(className)
                .pressSave()
                .getAllRules();

        assertThat(rules).isNotEmpty();
        StaticScanActions.createStaticScan(scanDTO, applicationDTO, FodCustomTypes.SetupScanPageStatus.InProgress);

        BrowserUtil.clearCookiesLogOff();
        var scanDetails = LogInActions.adminLogIn().adminTopNavbar.openStatic()
                .getLatestScanByAppDto(applicationDTO)
                .openDetails();
        assertThat(scanDetails.generateCustomRulesBtn.exists()).isTrue();

        var customRulesFile = scanDetails.downloadCustomRulesFile();
        assertThat(Arrays.toString(linesOf(customRulesFile).toArray()))
                .contains(ruleInFile, className, functionName, packageName, languageInRuleFile);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate tenant event log after create/update/delete rule actions")
    @Test(groups = {"regression"},
            priority = 1, dependsOnMethods = {"prepareTestData"})
    public void tenantCustomRulesEventLogValidationTest() {
        var auditToolsPage = LogInActions.tamUserLogin(tenantDTO)
                .tenantTopNavbar.openAdministration()
                .openAuditTools().openCleanseRules();

        var addRulePopup = auditToolsPage.pressAddRule();
        var languages = addRulePopup.languageDropdown.$$("option").texts();
        assertThat(languages).containsAll(langList);

        var rules = addRulePopup.setLanguage(language)
                .setCategory(category)
                .setPackageName(packageName)
                .setDataValidation(dataValidation)
                .setFunctionName(functionName)
                .setClassName(className)
                .pressSave()
                .getAllRules();

        assertThat(rules).isNotEmpty();
        sleep(10000);

        var eventLog = auditToolsPage.openEventLog();
        var logs = eventLog.getAllLogs();
        var createdLog = logs.stream().filter(log -> log.getType().equals(msgCreated))
                .findFirst().orElse(null);
        assertThat(createdLog).isNotNull();
        assertThat(createdLog.getNotes()).contains(className, packageName, functionName);

        var updatedClassName = eventLog.openAuditTools().openCleanseRules()
                .getAllRules().get(0).pressEdit()
                .setClassName(newClassName).pressSave()
                .getAllRules().get(0).getClassName();

        assertThat(updatedClassName).isEqualTo(newClassName);
        sleep(10000);

        var updatedLog = auditToolsPage.openEventLog()
                .getAllLogs().stream().filter(log -> log.getType().equals(msgEdited))
                .findFirst().orElse(null);

        assertThat(updatedLog).isNotNull();
        assertThat(updatedLog.getNotes()).contains(newClassName, packageName, functionName);

        var rulesList = eventLog.openAuditTools().openCleanseRules().getAllRules();
        var rulesListCountBefore = rulesList.size();
        rulesList.get(0).pressDelete().pressYes();
        var rulesListCountAfter = auditToolsPage.getAllRules().size();
        assertThat(rulesListCountAfter).isLessThan(rulesListCountBefore);

        sleep(10000);
        var deletedLog = auditToolsPage.openEventLog().getAllLogs()
                .stream().filter(log -> log.getType().equals(msgDeleted))
                .findFirst().orElse(null);
        assertThat(deletedLog).isNotNull();
        assertThat(deletedLog.getNotes()).contains(newClassName, packageName, functionName);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate application event log after create/update/delete rule actions")
    @Test(groups = {"regression"},
            priority = 2, dependsOnMethods = {"prepareTestData"})
    public void applicationCustomRulesEventLogValidationTest() {
        var applicationAuditToolPage = LogInActions.tamUserLogin(tenantDTO)
                .tenantTopNavbar.openApplications()
                .openDetailsFor(applicationDTO.getApplicationName())
                .openAuditTools().openCleanseRules();

        var applicationAuditToolPopup = applicationAuditToolPage.pressAddRule();
        var appRules = applicationAuditToolPopup.setLanguage(language)
                .setCategory(category)
                .setPackageName(packageName)
                .setDataValidation(dataValidation)
                .setFunctionName(functionName)
                .setClassName(className)
                .pressSave()
                .getAllRules();

        assertThat(appRules).isNotEmpty();

        sleep(10000);
        var applicationEventLog = applicationAuditToolPage.openEventLog();
        var appRuleCreated = applicationEventLog.getAllLogs()
                .stream().filter(log -> log.getType().equals(msgAppCreated))
                .findFirst().orElse(null);
        assertThat(appRuleCreated).isNotNull();
        assertThat(appRuleCreated.getNotes()).contains(className, packageName, functionName);

        var updatedClassNameForAppRule = applicationEventLog.openAuditTools().openCleanseRules()
                .getAllRules().get(0)
                .pressEdit().setClassName(newClassName).pressSave()
                .getAllRules().get(0).getClassName();

        assertThat(updatedClassNameForAppRule).isEqualTo(newClassName);

        sleep(10000);
        var updatedAppRuleLog = applicationAuditToolPage.openEventLog()
                .getAllLogs().stream().filter(log -> log.getType().equals(msgAppEdited))
                .findFirst().orElse(null);
        assertThat(updatedAppRuleLog).isNotNull();
        assertThat(updatedAppRuleLog.getNotes()).contains(newClassName, packageName, functionName);

        var rulesList = applicationEventLog.openAuditTools().openCleanseRules().getAllRules();
        var rulesListCountBefore = rulesList.size();

        rulesList.get(0)
                .pressDelete()
                .pressYes();
        var rulesListCountAfter = applicationAuditToolPage.getAllRules().size();
        assertThat(rulesListCountAfter).isLessThan(rulesListCountBefore);

        sleep(10000);
        var appRuleDeletedLog = applicationAuditToolPage.openEventLog().getAllLogs()
                .stream().filter(log -> log.getType().equals(msgAppDeleted))
                .findFirst().orElse(null);
        assertThat(appRuleDeletedLog).isNotNull();
        assertThat(appRuleDeletedLog.getNotes()).contains(newClassName, packageName, functionName);
    }

    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Parameters("User Role")
    @Description("Validate users availability to audit tools functionality")
    @Test(groups = {"regression"},
            dataProvider = "tenantUserRolesStrings",
            dataProviderClass = FodUiTestDataProviders.class,
            priority = 1, dependsOnMethods = {"prepareTestData"})
    public void auditToolsSecurityMatrixTest(String role) {
        TenantUserDTO user;
        switch (role) {
            case "Application Lead":
                user = appLead;
                break;
            case "Developer":
                user = dev;
                break;
            case "Reviewer":
                user = reviewer;
                break;
            case "Security Lead":
                user = sl;
                break;
            case "Executive":
                user = executive;
                break;
            case "Lead Developer":
                user = leadDev;
                break;
            case "TAM":
                user = tam;
                break;
            default:
                throw new IllegalStateException("Unexpected user role: " + role);
        }

        var globalAuditTemplateTabName = "Global Audit Template";
        var appAuditTemplateTabName = "Application Audit Template";
        var dataflowCleanseRulesTabName = "Dataflow Cleanse Rules";
        var appDataflowCleanseRulesTabName = "Application Audit Template";

        var userRole = user.getUserName().contains("TAM")
                ? tenantDTO.getAssignedUser()
                : user.getRole().getTypeValue();

        var message = "Audit tools tab avail for: " + userRole;
        TenantTopNavbar topNavbar;

        var globalAuditToolAvailable = user.getUserName().contains("TAM")
                || user.getRole() == FodCustomTypes.TenantUserRole.SecurityLead;
        var applicationAuditToolAvailable = user.getUserName().contains("TAM")
                || user.getRole() == FodCustomTypes.TenantUserRole.SecurityLead
                || user.getRole() == FodCustomTypes.TenantUserRole.ApplicationLead;

        if (user.getUserName().contains("TAM"))
            topNavbar = LogInActions.tamUserLogin(tenantDTO).tenantTopNavbar;
        else
            topNavbar = LogInActions.tenantUserLogIn(user.getUserName(), user.getPassword(), tenantDTO.getTenantCode())
                    .tenantTopNavbar;

        if (globalAuditToolAvailable) {
            var configPage = topNavbar.openAdministration();
            assertThat(configPage.sideNavTabExists(TenantSideNavTabs.Administration.AuditTools))
                    .as(message)
                    .isTrue();
            var auditToolsPage = configPage.openAuditTools();
            assertThat(auditToolsPage.tabs.tabExists(globalAuditTemplateTabName)).isTrue();
            assertThat(auditToolsPage.tabs.tabExists(dataflowCleanseRulesTabName)).isTrue();
        } else {
            var accountSettings = topNavbar.openAccountSettings();
            assertThat(accountSettings.sideNavTabExists(TenantSideNavTabs.Administration.AuditTools))
                    .as(message)
                    .isFalse();
        }

        var appDetails = topNavbar.openApplications()
                .openDetailsFor(applicationDTO.getApplicationName());
        if (applicationAuditToolAvailable) {
            assertThat(appDetails.sideNavTabExists(TenantSideNavTabs.ApplicationDetails.AuditTools))
                    .as(message)
                    .isTrue();
            var auditToolsPage = appDetails.openAuditTools();
            assertThat(auditToolsPage.tabs.tabExists(appAuditTemplateTabName)).isTrue();
            assertThat(auditToolsPage.tabs.tabExists(appDataflowCleanseRulesTabName)).isTrue();
        } else {
            assertThat(appDetails.sideNavTabExists(TenantSideNavTabs.Administration.AuditTools))
                    .as(message)
                    .isFalse();
        }
    }

    @Owner("oradchenko@opentext.com")
    @FodBacklogItem("405003")
    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("User with audit permissions should be able to create cleanse rule")
    @Test(groups = {"hf", "regression"})
    public void createRuleAsNewRoleTest() {
        var tenant = defaultTenantDTO;
        var role = TenantUserRoleDTO.createDefaultInstance();
        role.setIssuePermissions(FodCustomTypes.RoleApplicationIssuePermissions.Audit);
        role.setApplicationAccess(FodCustomTypes.RoleApplicationAccess.All);
        var user = TenantUserDTO.createDefaultInstance();
        user.setTenant(tenant.getTenantCode());
        LogInActions.tamUserLogin(tenant);
        var app = ApplicationActions.createApplication();
        TenantRoleActions.createRole(role);
        TenantUserActions.createTenantUser(user);
        BrowserUtil.clearCookiesLogOff();
        LogInActions.tamUserLogin(tenant)
                .tenantTopNavbar.openAdministration().openUsers().editUserByName(user.getUserName())
                .selectRole(role.getRoleName()).pressSaveBtn();
        BrowserUtil.clearCookiesLogOff();

        var page = LogInActions.tenantUserLogIn(user.getUserName(), FodConfig.TAM_PASSWORD, tenant.getTenantCode())
                .tenantTopNavbar.openApplications().openDetailsFor(app.getApplicationName()).openAuditTools().openCleanseRules()
                .pressAddRule().setLanguage(language)
                .setCategory(category)
                .setPackageName(packageName)
                .setDataValidation(dataValidation)
                .setFunctionName(functionName)
                .setClassName(className)
                .pressSave();
        if (ModalDialog.isMessageDisplayed()) {
            var message = new ModalDialog().getMessage();
            assertThat(message).as("There shouldn't be any error messages")
                    .doesNotContainIgnoringCase("unexpected error");
        }
        var rules = page.getAllRules();
        assertThat(rules).isNotEmpty();
    }

    @Owner("kbadia@opentext.com")
    @MaxRetryCount(3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Validate Event Log page under Administration of tenant side")
    @Test(groups = {"regression"},

            dependsOnMethods = "auditToolsSecurityMatrixTest",
            alwaysRun = true)
    public void validateEventLogPageTest() {
        var eventLogPage = LogInActions.tamUserLogin(tenantDTO)
                .tenantTopNavbar.openAdministration().openEventLog();
        var expectedColumns = new ArrayList<String>() {{
            add("Event Date");
            add("Type");
            add("User");
            add("Application");
            add("Notes");
        }};
        AllureReportUtil.info("Validate Paging in tenant Event Log page");
        new PagingActions().validatePaging();
        AllureReportUtil.info("Validate Pagination in tenant Event Log page");
        new PagingActions().validatePagination();
        new Paging().setRecordsPerPage_100();
        assertThat(eventLogPage
                .searchTextField
                .isDisplayed())
                .as("Check search text field exists and visible")
                .isTrue();
        assertThat(eventLogPage
                .searchButton
                .isDisplayed())
                .as("Search button exists and visible")
                .isTrue();
        assertThat(eventLogPage.findWithSearchBox(tenantDTO.getAssignedUser()))
                .as("Verify searching of text is working fine")
                .isNotNull();
        var eventLogColumns = eventLogPage.getAllLogs().stream()
                .map(EventLogCellMain::getEventLogPageColumnHeaders)
                .collect(Collectors.toList());
        assertThat(eventLogColumns)
                .as("Verify the Event Log page is displaying the Event" +
                        " Date/Time, Event Type, User Name, Application, Event Notes")
                .contains(expectedColumns);
        assertThat(eventLogPage
                .getAllLogs())
                .as("Event log shouldn't be empty")
                .isNotEmpty();
    }
}