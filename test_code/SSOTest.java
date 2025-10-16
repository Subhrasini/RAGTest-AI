package com.fortify.fod.ui.test.regression;

import com.codeborne.selenide.Condition;
import com.fortify.common.ui.pages.simplesaml.SSOLoginPage;
import com.fortify.common.utils.BrowserUtil;
import com.fortify.common.utils.UniqueRunTag;
import com.fortify.common.utils.WaitUtil;
import com.fortify.fod.common.custom_types.FodCustomTypes;
import com.fortify.fod.common.elements.ModalDialog;
import com.fortify.fod.common.entities.SSOUserDTO;
import com.fortify.fod.common.entities.TenantDTO;
import com.fortify.fod.common.entities.TenantUserDTO;
import com.fortify.fod.common.utils.sql.FodSQLUtil;
import com.fortify.fod.data_providers.FodUiTestDataProviders;
import com.fortify.fod.ui.pages.tenant.administration.user_management.groups.popups.GroupUserCell;
import com.fortify.fod.ui.pages.tenant.navigation.TenantTopNavbar;
import com.fortify.fod.ui.test.FodBaseTest;
import com.fortify.fod.ui.test.actions.*;
import io.qameta.allure.*;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.Test;
import utils.FodBacklogItem;
import utils.MaxRetryCount;

import java.time.Duration;
import java.util.List;

import static com.codeborne.selenide.files.FileFilters.withExtension;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@FodBacklogItem("60311")
@Owner("oradchenko@opentext.com")
public class SSOTest extends FodBaseTest {

    final String idpMetadataFileName = "payloads/fod/idp_metadata.xml";
    final String uid = "uid";
    final String givenName = "givenname";
    final String surname = "surname";
    final String email = "email";
    final String groupName = "groupName";
    final String role = "role";
    final String singleIdpName = "https://16.103.235.59:9443/simplesaml/saml2/idp/metadata.php";
    final String expectedCertThumbprint = "781f2e061f47cc630af4e6b5d483d16268658b07";
    final String multiTenantSsoGuid1 = "25b932cb-edfe-4d34-813d-fee0ec503d67";
    final String multiTenantSsoGuid2 = "c268899b-c175-455e-a5d2-c90e9b63a5b1";
    String ssoLoginURL;
    String tenant1LoginUrl;
    String tenant2loginUrl;

    @Test()
    @Description("Admin site settings should contain valid 'SAMLServiceProviderCertificateThumbprint' entry")
    public void ssoAdminSettingsTest() {
        var actualCertThumbprint = LogInActions.adminLogIn()
                .adminTopNavbar.openConfiguration().getSettingValueByName("SAMLServiceProviderCertificateThumbprint");
        assertThat(actualCertThumbprint).as("Certificate thumbprint should match expected value")
                .isEqualToIgnoringCase(expectedCertThumbprint);

    }

    @FodBacklogItem("417023")
    @MaxRetryCount(1)
    @Description("Tenant user should be able to configure SSO with IdP metadata, download FoD SP metadata," +
            " and get an SSO login URL")
    @Severity(SeverityLevel.NORMAL)
    @Test(dependsOnMethods = {"ssoAdminSettingsTest"})
    public void tenantSSOSetupTest() {
        ssoLoginURL = setupTenantSSO(defaultTenantDTO, null);
    }


    @FodBacklogItem("676002")
    @MaxRetryCount(1)
    @Description("With configured multi-tenant SSO, it should be possible to use single IdP with multiple tenants")
    @Severity(SeverityLevel.NORMAL)
    @Test(dependsOnMethods = {"ssoAdminSettingsTest"})
    public void multiTenantSSOTest() {
        eraseIdpConfig();
        var tenant1 = TenantDTO.createDefaultInstance();
        var tenant2 = TenantDTO.createDefaultInstance();

        TenantActions.createTenants(true, tenant1, tenant2);

        BrowserUtil.clearCookiesLogOff();
        tenant1LoginUrl = setupTenantSSO(tenant1, multiTenantSsoGuid1);
        BrowserUtil.clearCookiesLogOff();
        tenant2loginUrl = setupTenantSSO(tenant2, multiTenantSsoGuid2);
        BrowserUtil.clearCookiesLogOff();

        createSsoUserAndCheckLogin(tenant1, tenant1LoginUrl, multiTenantSsoGuid1);
        closeDriverAttachVideo();
        createSsoUserAndCheckLogin(tenant2, tenant2loginUrl, multiTenantSsoGuid2);
    }

    @MaxRetryCount(1)
    @Description("User should be created in FoD on successful SSO login")
    @Severity(SeverityLevel.NORMAL)
    @Test(dependsOnMethods = {"tenantSSOSetupTest"}, alwaysRun = true)
    public void jitUserProvisioningTest() {
        var softAssertions = new SoftAssertions();
        var ssoUser = SSOUserDTO.createDefaultInstance();

        var ssoPage = LogInActions.tamUserLogin(defaultTenantDTO).tenantTopNavbar.openAdministration().openSSO();

        ssoPage.enableJITProvisioning.setValue(true);
        ssoPage.enableJITGroupAssignment.setValue(false);
        ssoPage.enableJITGroupProvisioning.setValue(false);
        ssoPage.pressSave();

        closeDriverAttachVideo();

        SSOActions.createSSOUser(ssoUser);
        var ssoLoginPage = SSOLoginPage.navigate(ssoLoginURL);
        ssoLoginPage.logIn(ssoUser.getUserName(), ssoUser.getPassword());

        var name = new TenantTopNavbar().openAccountSettings().getUsername();

        softAssertions.assertThat(name).as("Username should equal username from IdP")
                .isEqualToIgnoringCase(ssoUser.getUserName());

        softAssertions.assertAll();
    }


    @MaxRetryCount(1)
    @Description("User should be assigned to FoD group on successful SSO login")
    @Severity(SeverityLevel.NORMAL)
    @Test(dependsOnMethods = {"jitUserProvisioningTest"}, alwaysRun = true)
    public void jitGroupAssignmentTest() {
        var softAssertions = new SoftAssertions();
        var groupName = String.format("group-%s", UniqueRunTag.generate());
        var ssoUser = SSOUserDTO.createDefaultInstance();

        var page = LogInActions.tamUserLogin(defaultTenantDTO).tenantTopNavbar.openAdministration().openGroups();
        TenantGroupActions.createTenantGroup(groupName);
        var ssoPage = page.tenantTopNavbar.openAdministration().openSSO();

        ssoPage.enableJITProvisioning.setValue(true);
        ssoPage.enableJITGroupAssignment.setValue(true);
        ssoPage.enableJITGroupProvisioning.setValue(false);
        ssoPage.pressSave();

        closeDriverAttachVideo();

        ssoUser.setGroups(new String[]{groupName});
        SSOActions.createSSOUser(ssoUser);
        var ssoLoginPage = SSOLoginPage.navigate(ssoLoginURL);
        ssoLoginPage.logIn(ssoUser.getUserName(), ssoUser.getPassword());

        var name = new TenantTopNavbar().openAccountSettings().getUsername();

        softAssertions.assertThat(name).as("Username should equal username from IdP")
                .isEqualToIgnoringCase(ssoUser.getUserName());

        BrowserUtil.clearCookiesLogOff();
        List<GroupUserCell> selectedGroupUsers =
                LogInActions.tamUserLogin(defaultTenantDTO).tenantTopNavbar.openAdministration().openGroups()
                        .findGroup(groupName).getGroupByName(groupName).pressEdit().getAllSelectedUsers();

        var userNames = selectedGroupUsers.stream().map(u -> u.getLastName());

        softAssertions.assertThat(userNames)
                .as(String.format("User %s should be assigned to group %s", ssoUser.getLastName(), groupName))
                .contains(ssoUser.getLastName());
        softAssertions.assertAll();
    }

    @MaxRetryCount(1)
    @Description("Group should be created and user should be assigned to the group on successful SSO login")
    @Severity(SeverityLevel.NORMAL)
    @Test(dependsOnMethods = {"jitGroupAssignmentTest"}, alwaysRun = true)
    public void jitGroupCreationTest() {
        var softAssertions = new SoftAssertions();
        var groupName = String.format("sso-group-%s", UniqueRunTag.generate());
        var ssoUser = SSOUserDTO.createDefaultInstance();

        var ssoPage = LogInActions.tamUserLogin(defaultTenantDTO).tenantTopNavbar.openAdministration().openSSO();

        ssoPage.enableJITProvisioning.setValue(true);
        ssoPage.enableJITGroupAssignment.setValue(true);
        ssoPage.enableJITGroupProvisioning.setValue(true);
        ssoPage.pressSave();

        closeDriverAttachVideo();

        ssoUser.setGroups(new String[]{groupName});
        SSOActions.createSSOUser(ssoUser);
        var ssoLoginPage = SSOLoginPage.navigate(ssoLoginURL);
        ssoLoginPage.logIn(ssoUser.getUserName(), ssoUser.getPassword());

        var name = new TenantTopNavbar().openAccountSettings().getUsername();

        softAssertions.assertThat(name).as("Username should equal username from IdP")
                .isEqualToIgnoringCase(ssoUser.getUserName());

        BrowserUtil.clearCookiesLogOff();
        var groupsPage = LogInActions.tamUserLogin(defaultTenantDTO).tenantTopNavbar
                .openAdministration().openGroups();

        var groups = groupsPage.findGroup(groupName).getAllGroups().stream().map(group -> group.getName());
        softAssertions.assertThat(groups).as("Group Created with SSO should be in the list").isNotEmpty()
                .contains(groupName);

        List<GroupUserCell> selectedGroupUsers = groupsPage.getGroupByName(groupName).pressEdit().getAllSelectedUsers();
        var userNames = selectedGroupUsers.stream().map(u -> u.getLastName());

        softAssertions.assertThat(userNames)
                .as(String.format("User %s should be assigned to group %s", ssoUser.getLastName(), groupName))
                .contains(ssoUser.getLastName()).as("There should be only 1 user in the group").hasSize(1);
        softAssertions.assertAll();
    }

    @MaxRetryCount(1)
    @Description("Pre-existing user should be assigned to FoD group on successful SSO login")
    @Severity(SeverityLevel.NORMAL)
    @Test(dependsOnMethods = {"jitGroupCreationTest"}, alwaysRun = true)
    public void preExistingUserJITGroupAssignmentTest() {
        var softAssertions = new SoftAssertions();
        var groupName = String.format("group-%s", UniqueRunTag.generate());
        var ssoUser = SSOUserDTO.createDefaultInstance();
        var fodUser = TenantUserDTO.createInstanceWithUserRole(FodCustomTypes.TenantUserRole.Developer);
        fodUser.setTenant(defaultTenantDTO.getTenantCode());
        ssoUser.setUserName(fodUser.getUserName());
        ssoUser.setFirstName(fodUser.getFirstName());
        ssoUser.setLastName(fodUser.getLastName());
        ssoUser.setGroups(new String[]{groupName});

        LogInActions.tamUserLogin(defaultTenantDTO).tenantTopNavbar.openAdministration().openGroups();
        TenantGroupActions.createTenantGroup(groupName);
        TenantUserActions.createTenantUser(fodUser);
        BrowserUtil.clearCookiesLogOff();

        var ssoPage = LogInActions.tamUserLogin(defaultTenantDTO).tenantTopNavbar.openAdministration().openSSO();

        ssoPage.enableJITProvisioning.setValue(true);
        ssoPage.enableJITGroupAssignment.setValue(true);
        ssoPage.enableJITGroupProvisioning.setValue(false);
        ssoPage.pressSave();

        closeDriverAttachVideo();

        SSOActions.createSSOUser(ssoUser);
        var ssoLoginPage = SSOLoginPage.navigate(ssoLoginURL);
        ssoLoginPage.logIn(ssoUser.getUserName(), ssoUser.getPassword());

        var name = new TenantTopNavbar().openAccountSettings().getUsername();

        softAssertions.assertThat(name).as("Username should equal username from IdP")
                .isEqualToIgnoringCase(ssoUser.getUserName());
        BrowserUtil.clearCookiesLogOff();

        var groupsPage = LogInActions.tamUserLogin(defaultTenantDTO).tenantTopNavbar
                .openAdministration().openGroups();

        var groups = groupsPage.findGroup(groupName).getAllGroups().stream().map(group -> group.getName());
        softAssertions.assertThat(groups).as("Group Created with SSO should be in the list").isNotEmpty()
                .contains(groupName);

        List<GroupUserCell> selectedGroupUsers = groupsPage.findGroup(groupName).getGroupByName(groupName).pressEdit()
                .getAllSelectedUsers();

        var userNames = selectedGroupUsers.stream().map(u -> u.getLastName());

        softAssertions.assertThat(userNames)
                .as(String.format("User %s should be assigned to group %s", ssoUser.getLastName(), groupName))
                .contains(ssoUser.getLastName()).as("There should be only 1 user in the group").hasSize(1);
        softAssertions.assertAll();
    }

    @MaxRetryCount(1)
    @Description("Pre-existing user should be assigned to the JIT-created group on successful SSO login")
    @Severity(SeverityLevel.NORMAL)
    @Test(dependsOnMethods = {"preExistingUserJITGroupAssignmentTest"}, alwaysRun = true)
    public void preExistingUserJITGroupCreationTest() {
        var softAssertions = new SoftAssertions();
        var groupName = String.format("sso-group-%s", UniqueRunTag.generate());
        var ssoUser = SSOUserDTO.createDefaultInstance();
        var fodUser = TenantUserDTO.createInstanceWithUserRole(FodCustomTypes.TenantUserRole.Developer);
        fodUser.setTenant(defaultTenantDTO.getTenantCode());
        ssoUser.setUserName(fodUser.getUserName());
        ssoUser.setFirstName(fodUser.getFirstName());
        ssoUser.setLastName(fodUser.getLastName());
        ssoUser.setGroups(new String[]{groupName});

        LogInActions.tamUserLogin(defaultTenantDTO).tenantTopNavbar.openAdministration();
        TenantUserActions.createTenantUser(fodUser);
        BrowserUtil.clearCookiesLogOff();

        var ssoPage = LogInActions.tamUserLogin(defaultTenantDTO).tenantTopNavbar.openAdministration().openSSO();

        ssoPage.enableJITProvisioning.setValue(true);
        ssoPage.enableJITGroupAssignment.setValue(true);
        ssoPage.enableJITGroupProvisioning.setValue(true);
        ssoPage.pressSave();

        closeDriverAttachVideo();

        SSOActions.createSSOUser(ssoUser);
        var ssoLoginPage = SSOLoginPage.navigate(ssoLoginURL);
        ssoLoginPage.logIn(ssoUser.getUserName(), ssoUser.getPassword());

        var name = new TenantTopNavbar().openAccountSettings().getUsername();

        softAssertions.assertThat(name).as("Username should equal username from IdP")
                .isEqualToIgnoringCase(ssoUser.getUserName());

        BrowserUtil.clearCookiesLogOff();
        List<GroupUserCell> selectedGroupUsers =
                LogInActions.tamUserLogin(defaultTenantDTO).tenantTopNavbar.openAdministration().openGroups()
                        .findGroup(groupName).getGroupByName(groupName).pressEdit().getAllSelectedUsers();

        var userNames = selectedGroupUsers.stream().map(u -> u.getLastName());

        softAssertions.assertThat(userNames)
                .as(String.format("User %s should be assigned to group %s", ssoUser.getLastName(), groupName))
                .contains(ssoUser.getLastName());
        softAssertions.assertAll();
    }

    @MaxRetryCount(1)
    @Description("Pre-existing user should be unassigned from the pre-existing group " +
            "and assigned to the JIT-created group on successful SSO login")
    @Severity(SeverityLevel.NORMAL)
    @Test(dependsOnMethods = {"preExistingUserJITGroupCreationTest"}, alwaysRun = true)
    public void changingUserGroupDesignationTest() {
        var softAssertions = new SoftAssertions();
        var fodGroupName = String.format("group-%s", UniqueRunTag.generate());
        var ssoGroupName = String.format("sso-group-%s", UniqueRunTag.generate());
        var ssoUser = SSOUserDTO.createDefaultInstance();
        var fodUser = TenantUserDTO.createInstanceWithUserRole(FodCustomTypes.TenantUserRole.Developer);
        fodUser.setTenant(defaultTenantDTO.getTenantCode());
        ssoUser.setUserName(fodUser.getUserName());
        ssoUser.setFirstName(fodUser.getFirstName());
        ssoUser.setLastName(fodUser.getLastName());
        ssoUser.setGroups(new String[]{ssoGroupName});

        LogInActions.tamUserLogin(defaultTenantDTO).tenantTopNavbar.openAdministration();
        TenantUserActions.createTenantUser(fodUser);
        BrowserUtil.clearCookiesLogOff();

        var groupsPage = LogInActions.tamUserLogin(defaultTenantDTO).tenantTopNavbar.openAdministration().openGroups();
        TenantGroupActions.createTenantGroup(fodGroupName);
        var groupCell = groupsPage.findGroup(fodGroupName).getGroupByName(fodGroupName);
        groupCell.pressEdit().assignUser(fodUser).pressSave();

        BrowserUtil.clearCookiesLogOff();

        var ssoPage = LogInActions.tamUserLogin(defaultTenantDTO).tenantTopNavbar.openAdministration().openSSO();

        ssoPage.enableJITProvisioning.setValue(true);
        ssoPage.enableJITGroupAssignment.setValue(true);
        ssoPage.enableJITGroupProvisioning.setValue(true);
        ssoPage.pressSave();

        closeDriverAttachVideo();


        SSOActions.createSSOUser(ssoUser);
        var ssoLoginPage = SSOLoginPage.navigate(ssoLoginURL);
        ssoLoginPage.logIn(ssoUser.getUserName(), ssoUser.getPassword());

        var name = new TenantTopNavbar().openAccountSettings().getUsername();

        softAssertions.assertThat(name).as("Username should equal username from IdP")
                .isEqualToIgnoringCase(ssoUser.getUserName());

        BrowserUtil.clearCookiesLogOff();
        groupsPage = LogInActions.tamUserLogin(defaultTenantDTO).tenantTopNavbar
                .openAdministration().openGroups();

        var popup = groupsPage.findGroup(fodGroupName).getGroupByName(fodGroupName).pressEdit().openSelectedTab();

        softAssertions.assertThat(popup.isSelectedTableEmpty())
                .as("List should be empty, user should be unassigned from the group").isTrue();
        popup.pressSave();

        var groups = groupsPage.findGroup(ssoGroupName).getAllGroups().stream().map(group -> group.getName());
        softAssertions.assertThat(groups).as("Group Created with SSO should be in the list").isNotEmpty()
                .contains(ssoGroupName);

        List<GroupUserCell> selectedGroupUsers = groupsPage.findGroup(ssoGroupName).getGroupByName(ssoGroupName).pressEdit()
                .getAllSelectedUsers();

        var userNames = selectedGroupUsers.stream().map(u -> u.getLastName());

        softAssertions.assertThat(userNames)
                .as(String.format("User %s should be assigned to group %s", ssoUser.getLastName(), ssoGroupName))
                .contains(ssoUser.getLastName()).as("There should be only 1 user in the group").hasSize(1);
        softAssertions.assertAll();
    }

    @MaxRetryCount(1)
    @Description("Users with different roles should be able to log in via SSO")
    @Severity(SeverityLevel.NORMAL)
    @Test(dataProviderClass = FodUiTestDataProviders.class, dataProvider = "tenantSSOUsersRoles",
            dependsOnMethods = {"changingUserGroupDesignationTest"},
            alwaysRun = true)
    public void ssoRolesTest(FodCustomTypes.TenantUserRole role) {
        var softAssertions = new SoftAssertions();
        var ssoPage = LogInActions.tamUserLogin(defaultTenantDTO).tenantTopNavbar.openAdministration().openSSO();
        ssoPage.enableJITProvisioning.setValue(false);
        ssoPage.enableJITGroupAssignment.setValue(false);
        ssoPage.enableJITGroupProvisioning.setValue(false);
        ssoPage.pressSave();

        closeDriverAttachVideo();

        var ssoUser = SSOUserDTO.createInstanceWithRole(role);
        var fodUser = TenantUserDTO.createInstanceWithUserRole(role);
        fodUser.setTenant(defaultTenantDTO.getTenantCode());
        ssoUser.setUserName(fodUser.getUserName());
        ssoUser.setFirstName(fodUser.getFirstName());
        ssoUser.setLastName(fodUser.getLastName());

        LogInActions.tamUserLogin(defaultTenantDTO).tenantTopNavbar.openAdministration();
        TenantUserActions.createTenantUser(fodUser);
        SSOActions.createSSOUser(ssoUser);
        closeDriverAttachVideo();

        var ssoLoginPage = SSOLoginPage.navigate(ssoLoginURL);
        ssoLoginPage.logIn(ssoUser.getUserName(), ssoUser.getPassword());

        var name = new TenantTopNavbar().openAccountSettings().getUsername();

        softAssertions.assertThat(name).as("Username should equal username from IdP")
                .isEqualToIgnoringCase(ssoUser.getUserName());

        softAssertions.assertAll();
    }

    /**
     * @param tenantDTO
     * @param tenantGuid use for multi tenant config, otherwise pass null
     */
    String setupTenantSSO(TenantDTO tenantDTO, String tenantGuid) {
        var softAssertions = new SoftAssertions();
        var ssoPage = LogInActions.tamUserLogin(tenantDTO).tenantTopNavbar.openAdministration().openSSO();
        ssoPage.ssoEnabled.click();

        if (tenantGuid == null) {
            eraseIdpConfig();
        } else {
            ssoPage.enableMultiSSO.setValue(true);
        }

        try {
            var fodSPMetadataFile = ssoPage.downloadMetadataButton.download(Duration.ofMinutes(3).toMillis(),
                    withExtension("xml"));
            softAssertions.assertThat(fodSPMetadataFile).as("XML metadata file should not be empty").isNotEmpty();
        } catch (Exception e) {
            softAssertions.fail(String.format("Download XML failure: ", e.getMessage()));
        }

        ssoPage.expandIDPSection.click();
        ssoPage.uploadMetadata(idpMetadataFileName).pressImport();
        var modal = new ModalDialog("Alert");
        if (WaitUtil.waitFor(WaitUtil.Operator.Equals,
                true,
                () -> modal.isVisible(),
                Duration.ofSeconds(10),
                false)) {
            softAssertions.fail(String.format("Import metadata failed because of: %s", modal.getMessage()));
            softAssertions.assertAll();
        }
        softAssertions.assertThat(ssoPage.idpName.input.shouldNotBe(Condition.empty, Duration.ofMinutes(1)).getValue())
                .as("IDP Name should not be empty after importing metadata").isNotBlank();

        softAssertions.assertThat(ssoPage.idpURL.input.getValue())
                .as("IDP URL should have valid value").containsIgnoringCase("http");

        ssoPage.expandAttributesSection.click();
        ssoPage.userName.setValue(uid);
        ssoPage.firstName.setValue(givenName);
        ssoPage.lastName.setValue(surname);
        ssoPage.email.setValue(email);
        ssoPage.groups.setValue(groupName);
        ssoPage.role.setValue(role);


        ssoPage.pressSave();
        softAssertions.assertThat(ssoPage.getSSOLoginURL()).as("SSO login URL should not be empty").isNotBlank();
        if (tenantGuid != null) {
            var db = new FodSQLUtil();
            var tenantId = db.getTenantIdByName(tenantDTO.getTenantName());

            db.executeQuery(
                    String.format("update SamlIdentityProviderConfig set RedirectToken='%s', IdProviderName='%s' where TenantId=%d",
                            tenantGuid, String.format("%s?t=%s", singleIdpName, tenantGuid), Integer.parseInt(tenantId)));

            db.close();
            WaitUtil.waitForTrue(() -> ssoPage.idpName.getValue().contains(tenantGuid), Duration.ofSeconds(60), true);

            softAssertions.assertThat(ssoPage.idpName.getValue())
                    .as("IDP Name should contain tenant GUID").contains(tenantGuid);
        }
        softAssertions.assertAll();
        return ssoPage.getSSOLoginURL();
    }

    void createSsoUserAndCheckLogin(TenantDTO tenantDTO, String loginUrl, String guid) {
        var ssoPage = LogInActions.tamUserLogin(tenantDTO).tenantTopNavbar.openAdministration().openSSO();
        ssoPage.enableJITProvisioning.setValue(true);
        ssoPage.enableJITGroupAssignment.setValue(false);
        ssoPage.enableJITGroupProvisioning.setValue(false);
        ssoPage.pressSave();

        closeDriverAttachVideo();

        var ssoUser = SSOUserDTO.createDefaultInstance();
        ssoUser.setMtt(guid);
        SSOActions.createSSOUser(ssoUser);

        var ssoLoginPage = SSOLoginPage.navigate(loginUrl);
        ssoLoginPage.logIn(ssoUser.getUserName(), ssoUser.getPassword());

        var name = new TenantTopNavbar().openAccountSettings().getUsername();

        assertThat(name).as("Username should equal username from IdP")
                .isEqualToIgnoringCase(ssoUser.getUserName());
    }

    @Step("Erase IdP Config in DB")
    void eraseIdpConfig() {
        var db = new FodSQLUtil();
        db.executeQuery("truncate table SamlIdentityProviderConfig").close();
    }
}
